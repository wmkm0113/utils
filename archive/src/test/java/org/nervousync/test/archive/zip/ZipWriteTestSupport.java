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

import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public final class ZipWriteTestSupport {

	static final String PASSWORD = "archive-write-test-password";

	public enum Layout { SINGLE, NATIVE, NUMBERED }
	public enum Encryption { NONE, STANDARD, AES, STRONG }
	enum Protection { HASH, COMPRESS, ENCRYPT, ALL }

	private ZipWriteTestSupport() {
	}

	public static ArchiveOptions archiveOptions() {
		return ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed(PASSWORD.toCharArray())).build();
	}

	public static ZipOptions options(final CompressionAlgorithm algorithm, final Zip64Mode mode,
	                          final Layout layout, final Encryption encryption) throws Exception {
		ZipOptions.Builder builder = ZipOptions.newBuilder().compressionAlgorithm(algorithm).zip64Mode(mode);
		if (layout != Layout.SINGLE) {
			builder.splitArchive(true, 65536)
					.splitType(layout == Layout.NATIVE ? ZipOptions.SplitType.NATIVE : ZipOptions.SplitType.NUMBERED);
		}
		switch (encryption) {
			case STANDARD:
				return builder.standardCrypto().build();
			case AES:
				return builder.aesCrypto(256).build();
			case STRONG:
				return builder.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256).build();
			default:
				return builder.build();
		}
	}

	static ZipOptions protectedOptions(final Layout layout, final Zip64Mode mode,
	                                   final Protection protection) throws Exception {
		ZipOptions.Builder builder = ZipOptions.newBuilder(options(
				CompressionAlgorithm.DEFLATE, mode, layout, Encryption.NONE));
		if (protection == Protection.COMPRESS || protection == Protection.ALL) {
			builder.enableCentralDirectoryCompress();
		}
		if (protection == Protection.ENCRYPT || protection == Protection.ALL) {
			builder.centralDirectoryCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256);
		}
		if (protection == Protection.HASH || protection == Protection.ALL) {
			builder.centralDirectoryHash(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
		}
		return builder.build();
	}

	public static byte[] payload(final int length) {
		byte[] bytes = new byte[length];
		new Random(42L).nextBytes(bytes);
		return bytes;
	}

	public static void writeEntry(final ArchiveFile archive, final String name, final byte[] bytes) throws Exception {
		writeEntry(archive, name, bytes, "");
	}

	public static void writeEntry(final ArchiveFile archive, final String name, final byte[] bytes,
	                       final String comment) throws Exception {
		try (ResourceWriter writer = archive.openWriter(name, comment).orElseThrow()) {
			try (OutputStream output = writer.openStream(0L)) {
				output.write(new byte[0]);
				if (bytes.length > 0) {
					output.write(bytes[0] & 0xFF);
					output.write(bytes, 1, bytes.length - 1);
				}
			}
			writer.finish();
		}
	}

	public static void assertContent(final Path archivePath, final ZipOptions options,
	                          final Map<String, byte[]> entries) throws Exception {
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(archivePath.toString(), "r"),
				archiveOptions(), options)) {
			for (Map.Entry<String, byte[]> expected : entries.entrySet()) {
				try (ResourceReader reader = archive.openReader(
						archive.entry(expected.getKey()).orElseThrow()).orElseThrow();
				     InputStream input = reader.openStream()) {
					assertArrayEquals(expected.getValue(), input.readAllBytes(), expected.getKey());
				}
			}
		}
	}

	public static Path readablePath(final Path target, final Layout layout) {
		return layout == Layout.NUMBERED
				? target.resolveSibling(target.getFileName() + ".001") : target;
	}

	static void createJdkArchive(final Path target, final Map<String, byte[]> entries) throws Exception {
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(target))) {
			for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
				ZipEntry header = new ZipEntry(entry.getKey());
				header.setTime(1700000000000L);
				output.putNextEntry(header);
				output.write(entry.getValue());
				output.closeEntry();
			}
		}
	}

	static Set<String> childNames(final Path directory) throws Exception {
		try (Stream<Path> children = Files.list(directory)) {
			return children.map(path -> path.getFileName().toString()).collect(Collectors.toSet());
		}
	}
}
