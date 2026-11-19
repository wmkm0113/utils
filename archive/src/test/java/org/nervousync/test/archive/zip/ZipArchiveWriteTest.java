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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.zip.ZipOptions;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

final class ZipArchiveWriteTest {

	@TempDir
	Path directory;

	private static Stream<Arguments> entryConfigurations() {
		return Stream.of(CompressionAlgorithm.STORE, CompressionAlgorithm.DEFLATE).flatMap(algorithm ->
				Stream.of(Zip64Mode.AUTO, Zip64Mode.ALWAYS).flatMap(mode ->
						Stream.of(Layout.values()).flatMap(layout -> Stream.of(Encryption.values())
								.map(encryption -> Arguments.of(algorithm, mode, layout, encryption)))));
	}

	@ParameterizedTest(name = "{0}, {1}, {2}, {3}")
	@MethodSource("entryConfigurations")
	void createsArchiveAndReadsPayloadAndEmptyEntry(final CompressionAlgorithm algorithm, final Zip64Mode mode,
	                                               final Layout layout, final Encryption encryption) throws Exception {
		Path target = this.directory.resolve("created.zip");
		byte[] bytes = payload(120017);
		ZipOptions options = options(algorithm, mode, layout, encryption);
		assertFalse(Files.exists(target));
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				archiveOptions(), options)) {
			writeEntry(archive, "folder/payload.bin", bytes);
			writeEntry(archive, "empty.bin", new byte[0]);
		}
		assertContent(readablePath(target, layout), options,
				Map.of("folder/payload.bin", bytes, "empty.bin", new byte[0]));
	}

	private static Stream<Arguments> emptyConfigurations() {
		return Stream.of(Zip64Mode.AUTO, Zip64Mode.ALWAYS).flatMap(mode ->
				Stream.of(Layout.values()).map(layout -> Arguments.of(mode, layout)));
	}

	@ParameterizedTest(name = "empty {0}, {1}")
	@MethodSource("emptyConfigurations")
	void createsEmptyArchive(final Zip64Mode mode, final Layout layout) throws Exception {
		Path target = this.directory.resolve("empty.zip");
		ZipOptions options = options(CompressionAlgorithm.STORE, mode, layout, Encryption.NONE);
		try (ArchiveFile ignored = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"), options)) {
			// Closing CREATE writes the archive footer even without entries.
		}
		try (ArchiveFile archive = ArchiveFile.zipFile(
				new LocalDataFile(readablePath(target, layout).toString(), "r"), options)) {
			assertTrue(archive.entry("missing").isEmpty());
		}
		if (layout == Layout.SINGLE) {
			assertEquals(mode == Zip64Mode.ALWAYS ? 98L : 22L, Files.size(target));
			try (ZipFile archive = new ZipFile(target.toFile())) {
				assertEquals(0, archive.size());
			}
		}
	}

	@ParameterizedTest(name = ".001 slice size {0}")
	@ValueSource(ints = {1, 7, 31})
	void readsZip64AesArchiveWhoseHeadersCrossNumberedSlices(final int sliceSize) throws Exception {
		Path target = this.directory.resolve("tiny.zip");
		byte[] bytes = {1, 2, 3, 4, 5};
		ZipOptions options = ZipOptions.newBuilder().splitArchive(true, sliceSize)
				.splitType(ZipOptions.SplitType.NUMBERED).zip64Mode(Zip64Mode.ALWAYS).aesCrypto(128).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				archiveOptions(), options)) {
			writeEntry(archive, "tiny.txt", bytes);
		}
		assertContent(readablePath(target, Layout.NUMBERED), options, Map.of("tiny.txt", bytes));
	}

	@Test
	void writtenUtf8ArchiveIsReadableByJdk() throws Exception {
		Path target = this.directory.resolve("jdk-readable.zip");
		String name = "目录/条目.txt";
		byte[] bytes = "UTF-8 ZIP compatibility".getBytes(StandardCharsets.UTF_8);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			writeEntry(archive, name, bytes);
		}
		try (ZipFile archive = new ZipFile(target.toFile());
		     InputStream input = archive.getInputStream(archive.getEntry(name))) {
			assertArrayEquals(bytes, input.readAllBytes());
		}
	}
}
