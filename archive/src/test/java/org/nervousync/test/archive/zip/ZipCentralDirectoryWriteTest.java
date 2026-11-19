/*
 * Licensed to the Nervousync Studio (NSYC) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nervousync.test.archive.zip;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.zip.ZipOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

final class ZipCentralDirectoryWriteTest {

	@TempDir
	Path directory;

	private static Stream<Arguments> protectedLayouts() {
		return Stream.of(Layout.values()).flatMap(layout -> Stream.of(Protection.values())
				.map(protection -> Arguments.of(layout, protection)));
	}

	@ParameterizedTest(name = "{0}, central directory {1}")
	@MethodSource("protectedLayouts")
	void readsCentralDirectoryWrittenWithSelectedProtection(final Layout layout,
	                                                       final Protection protection) throws Exception {
		Path target = this.directory.resolve("protected.zip");
		ZipOptions options = protectedOptions(layout, Zip64Mode.AUTO, protection);
		byte[] bytes = payload(120017);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				archiveOptions(), options)) {
			writeEntry(archive, "payload.bin", bytes);
		}
		assertContent(readablePath(target, layout), options, Map.of("payload.bin", bytes));
	}

	private static Stream<Arguments> largeDirectories() {
		return Stream.of(Zip64Mode.AUTO, Zip64Mode.ALWAYS).flatMap(mode ->
				Stream.of("plain", "compress", "encrypt", "all").map(protection -> Arguments.of(mode, protection)));
	}

	@ParameterizedTest(name = "large native central directory {0}, {1}")
	@MethodSource("largeDirectories")
	void readsLargeCentralDirectoryAcrossNativeVolumes(final Zip64Mode mode, final String protection) throws Exception {
		Path target = this.directory.resolve("large-directory.zip");
		ZipOptions.Builder builder = ZipOptions.newBuilder().zip64Mode(mode)
				.splitArchive(true, 65536).splitType(ZipOptions.SplitType.NATIVE);
		if ("compress".equals(protection) || "all".equals(protection)) {
			builder.enableCentralDirectoryCompress();
		}
		if ("encrypt".equals(protection) || "all".equals(protection)) {
			builder.centralDirectoryCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256);
		}
		if ("all".equals(protection)) {
			builder.centralDirectoryHash(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
		}
		ZipOptions options = builder.build();
		// Keep the directory larger than one volume even after DEFLATE compression.
		String comment = Base64.getEncoder().encodeToString(payload(48000));
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				archiveOptions(), options)) {
			writeEntry(archive, "first", new byte[]{1}, comment);
			writeEntry(archive, "second", new byte[]{2}, comment);
		}
		assertTrue(Files.exists(this.directory.resolve("large-directory.z01")));
		assertContent(target, options, Map.of("first", new byte[]{1}, "second", new byte[]{2}));
	}

	@ParameterizedTest(name = "central directory hash {0}")
	@ValueSource(ints = {0x0001, 0x8003, 0x8004, 0x8007, 0x800C, 0x800D, 0x800E})
	void readsHashOnlyCentralDirectoryWithoutSpecialEntryCountMarkers(final int hashAlgorithm) throws Exception {
		Path target = this.directory.resolve("hash-only.zip");
		ZipOptions options = ZipOptions.newBuilder().centralDirectoryHash(hashAlgorithm).build();
		byte[] bytes = {3, 1, 4, 1, 5};
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"), options)) {
			writeEntry(archive, "entry", bytes);
		}
		assertContent(target, options, Map.of("entry", bytes));
	}
}
