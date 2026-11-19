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

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveLimit;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.crypto.strong.StrongInputStream;
import org.nervousync.archive.zip.crypto.strong.StrongOutputStream;
import org.nervousync.archive.zip.io.BoundedInputStream;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.operator.ZipFileOperator;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/** Small deterministic archive fixtures; no private state or reflection is used. */
final class ZipMergeTestSupport {

	static final byte[] DATA = payload(120017);

	private ZipMergeTestSupport() {
	}

	static Map<String, byte[]> contents() {
		return Map.of("data.bin", DATA, "empty.bin", new byte[0]);
	}

	static ZipOptions splitOptions(final Layout layout) throws Exception {
		return options(CompressionAlgorithm.DEFLATE, Zip64Mode.AUTO, layout, Encryption.NONE);
	}

	static ArchiveFile open(final Path path, final String mode, final ZipOptions options) throws Exception {
		return ArchiveFile.zipFile(new LocalDataFile(path.toString(), mode), archiveOptions(), options);
	}

	static ZipFileOperator operator(final Path path, final ZipOptions options) throws Exception {
		return new ZipFileOperator(new LocalDataFile(path.toString(), "r"), archiveOptions(), options);
	}

	static Path createSource(final Path directory, final Layout layout, final ZipOptions options) throws Exception {
		Path target = directory.resolve("source.zip");
		try (ArchiveFile archive = open(target, "rw", options)) {
			writeEntry(archive, "data.bin", DATA);
			writeEntry(archive, "empty.bin", new byte[0]);
		}
		Path source = readablePath(target, layout);
		try (ZipFileOperator operator = operator(source, options)) {
			assertTrue(operator.splitArchive(), "The fixture must have multiple physical volumes");
		}
		return source;
	}

	static Map<Path, byte[]> volumeHashes(final Path source, final ZipOptions options) throws Exception {
		Map<Path, byte[]> hashes = new LinkedHashMap<>();
		try (ZipFileOperator operator = operator(source, options)) {
			for (Path volume : operator.existVolumes()) {
				hashes.put(volume, hash(volume));
			}
		}
		return hashes;
	}

	static void assertVolumeHashes(final Map<Path, byte[]> expected) throws Exception {
		for (Map.Entry<Path, byte[]> volume : expected.entrySet()) {
			assertArrayEquals(volume.getValue(), hash(volume.getKey()),
					"Merging must not modify source volume " + volume.getKey());
		}
	}

	private static byte[] hash(final Path file) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (InputStream input = Files.newInputStream(file)) {
			byte[] buffer = new byte[8192];
			for (int length; (length = input.read(buffer)) != -1; ) {
				digest.update(buffer, 0, length);
			}
		}
		return digest.digest();
	}

	static Zip64EndCentralDirectory.Protection readProtection(final Path path, final ZipOptions options)
			throws Exception {
		try (ZipFileOperator operator = operator(path, options)) {
			var end = operator.readEndCentralDirectoryRecord();
			var zip64 = operator.readZip64EndCentralDirectory(end);
			return zip64 == null ? null : zip64.getProtection();
		}
	}

	static Path createDuplicateNumberedSource(final Path directory) throws Exception {
		Path single = directory.resolve("duplicate-original.zip");
		try (var output = new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(single)) {
			// Keep this small fixture ZIP32 so the test isolates duplicate-entry preservation.
			output.setUseZip64(org.apache.commons.compress.archivers.zip.Zip64Mode.Never);
			byte[] second = DATA.clone();
			second[0] ^= 0x55;
			for (byte[] bytes : new byte[][]{DATA, second}) {
				output.putArchiveEntry(new ZipArchiveEntry("same.bin"));
				output.write(bytes);
				output.closeArchiveEntry();
			}
		}
		try (InputStream input = Files.newInputStream(single)) {
			int index = 1;
			byte[] chunk;
			while ((chunk = input.readNBytes(65536)).length != 0) {
				Files.write(directory.resolve(String.format(java.util.Locale.ROOT,
						"duplicate.zip.%03d", index++)), chunk);
			}
		}
		return directory.resolve("duplicate.zip.001");
	}

	static ZipOptions maskedOptions() throws Exception {
		return ZipOptions.newBuilder(splitOptions(Layout.NATIVE)).compressionAlgorithm(CompressionAlgorithm.STORE)
				.centralDirectoryCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256).build();
	}

	static Path createMaskedSource(final Path directory, final ZipOptions options) throws Exception {
		Path source = createSource(directory, Layout.NATIVE, options);
		byte[] centralBytes;
		long centralOffset;
		int originalEncryptedLength;
		try (ZipFileOperator operator = operator(source, options)) {
			var end = operator.readEndCentralDirectoryRecord();
			var zip64 = operator.readZip64EndCentralDirectory(end);
			assertNotNull(zip64);
			var central = operator.readCentralDirectory(end, zip64);
			centralOffset = zip64.getRecord().getCentralDirectoryOffset();
			assertEquals(operator.existVolumes().size() - 1L, zip64.getRecord().getCentralDirectoryStartDisk(),
					"This fixture keeps its encrypted central directory on the last volume");
			for (var header : central.getFileHeaders()) {
				Path volume = operator.existVolumes().get(Math.toIntExact(header.getDiskNumberStart()));
				try (RandomAccessFile file = new RandomAccessFile(volume.toFile(), "rw")) {
					file.seek(header.getOffsetLocalHeader());
					byte[] fixed = new byte[30];
					file.readFully(fixed);
					ByteBuffer wire = ByteBuffer.wrap(fixed).order(ByteOrder.LITTLE_ENDIAN);
					wire.putShort(6, (short) (header.getGeneralPurpose() | 0x2000));
					Arrays.fill(fixed, 10, 26, (byte) 0);
					file.seek(header.getOffsetLocalHeader());
					file.write(fixed);
					byte[] maskedName = new byte[Short.toUnsignedInt(wire.getShort(26))];
					Arrays.fill(maskedName, (byte) '0');
					file.write(maskedName);
				}
			}
			originalEncryptedLength = Math.toIntExact(zip64.getLocator().getZip64EndRecordOffset() - centralOffset);
			try (var input = operator.inputStream()) {
				input.seek(zip64.getRecord().getCentralDirectoryStartDisk(), centralOffset);
				var bounded = new BoundedInputStream(input, originalEncryptedLength);
				var decryptionHeader = ZipCrypto.readStrongDecryptionHeader(new DataInputStream(bounded), ArchiveLimit.DEFAULT_LIMIT.getMaxHeaderBytes());
				try (var decrypted = StrongInputStream.centralDirectory(source.toString(), bounded,
						zip64.getProtection(), archiveOptions().getPasswordProvider(), archiveOptions().getCertificateProvider(),
						decryptionHeader)) {
					centralBytes = decrypted.readAllBytes();
				}
			}
		}
		ByteBuffer central = ByteBuffer.wrap(centralBytes).order(ByteOrder.LITTLE_ENDIAN);
		for (int offset = 0; offset < centralBytes.length; ) {
			assertEquals(0x02014B50, central.getInt(offset));
			central.putShort(offset + 8, (short) (Short.toUnsignedInt(central.getShort(offset + 8)) | 0x2000));
			offset += 46 + Short.toUnsignedInt(central.getShort(offset + 28))
					+ Short.toUnsignedInt(central.getShort(offset + 30))
					+ Short.toUnsignedInt(central.getShort(offset + 32));
		}
		ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
		try (var output = new StrongOutputStream(encrypted, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256,
				256, PASSWORD.getBytes(StandardCharsets.UTF_8))) {
			output.write(centralBytes);
		}
		assertEquals(originalEncryptedLength, encrypted.size(), "Patching flags must not move the ending records");
		try (RandomAccessFile file = new RandomAccessFile(source.toFile(), "rw")) {
			file.seek(centralOffset);
			file.write(encrypted.toByteArray());
		}
		return source;
	}

	static void assertMaskedFields(final Path source, final ZipOptions options) throws Exception {
		try (ZipFileOperator operator = operator(source, options)) {
			var end = operator.readEndCentralDirectoryRecord();
			var central = operator.readCentralDirectory(end, operator.readZip64EndCentralDirectory(end));
			var header = central.header("data.bin");
			assertNotNull(header);
			try (var input = operator.inputStream()) {
				input.seek(header.getDiskNumberStart(), header.getOffsetLocalHeader());
				byte[] fixed = new byte[30];
				new DataInputStream(input).readFully(fixed);
				ByteBuffer wire = ByteBuffer.wrap(fixed).order(ByteOrder.LITTLE_ENDIAN);
				assertAll(() -> assertNotEquals(0, Short.toUnsignedInt(wire.getShort(6)) & 0x2000),
						() -> assertEquals(0, wire.getInt(14), "Masked CRC must stay zero"),
						() -> assertEquals(0, wire.getInt(18), "Masked compressed size must stay zero"),
						() -> assertEquals(0, wire.getInt(22), "Masked original size must stay zero"));
			}
		}
	}
}
