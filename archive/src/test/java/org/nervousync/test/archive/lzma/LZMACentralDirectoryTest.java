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

package org.nervousync.test.archive.lzma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.operator.ZipFileOperator;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/** LZMA1 central-directory metadata, encryption boundaries and independent fixtures. */
final class LZMACentralDirectoryTest {
	@TempDir Path directory;

	private static ZipOptions optionsFor(Layout layout, Zip64Mode mode, int encryption, int hash) throws Exception {
		ZipOptions.Builder builder = ZipOptions.newBuilder(options(CompressionAlgorithm.LZMA1, mode, layout, Encryption.NONE))
				.enableCentralDirectoryCompress();
		if (encryption != 0) builder.centralDirectoryCrypto(encryption,
				encryption == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 ? 256 : 168);
		return builder.centralDirectoryHash(hash).build();
	}

	private static Stream<Arguments> configurations() {
		return Stream.of(Layout.values()).flatMap(layout -> Stream.of(Zip64Mode.AUTO, Zip64Mode.ALWAYS)
				.flatMap(mode -> Stream.of(0, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256,
								ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168)
						.flatMap(encryption -> Stream.of(0, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32,
										ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256)
								.map(hash -> Arguments.of(layout, mode, encryption, hash)))));
	}

	@Test
	void writesCompressedDirectoryWithValidSizes() throws Exception {
		Path path = directory.resolve("sizes.zip");
		ZipOptions options = ZipOptions.newBuilder(optionsFor(Layout.SINGLE, Zip64Mode.AUTO, 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32)).compressionLevel(0).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "value", new byte[] {1, 2, 3});
		}
		try (ZipFileOperator operator = operator(path, options)) {
			var end = operator.readEndCentralDirectoryRecord();
			var zip64 = operator.readZip64EndCentralDirectory(end);
			var protection = zip64.getProtection();
			assertEquals(14, protection.getCompressionMethod());
			assertTrue(zip64.getRecord().getExtractNeeded() >= 62);
			assertEquals(protection.getCompressedSize(), zip64.getRecord().getCentralDirectorySize());
			byte[] file = Files.readAllBytes(path);
			int offset = Math.toIntExact(zip64.getRecord().getCentralDirectoryOffset());
			byte[] compressed = Arrays.copyOfRange(file, offset, offset + Math.toIntExact(protection.getCompressedSize()));
			assertArrayEquals(new byte[] {9, 4, 5, 0}, Arrays.copyOf(compressed, 4));
			try (InputStream input = independentDecoder(compressed)) {
				byte[] central = input.readAllBytes();
				assertEquals(protection.getUncompressedSize(), central.length);
				assertEquals(0x02014B50, ByteBuffer.wrap(central).order(ByteOrder.LITTLE_ENDIAN).getInt());
				CRC32 crc = new CRC32(); crc.update(central);
				assertArrayEquals(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
						.putInt((int) crc.getValue()).array(), protection.getHashData());
			}
			assertNotNull(operator.readCentralDirectory(end, zip64).header("value"));
		}
	}

	@ParameterizedTest(name = "{0} {1}, crypto={2}, hash={3}")
	@MethodSource("configurations")
	void roundTripsProtectionCombinations(Layout layout, Zip64Mode mode, int encryption, int hash) throws Exception {
		Path path = directory.resolve("protected.zip");
		ZipOptions options = optionsFor(layout, mode, encryption, hash);
		byte[] payload = payload(120017);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "payload", payload);
			writeEntry(archive, "empty", new byte[0]);
		}
		Path readable = readablePath(path, layout);
		assertContent(readable, options, Map.of("payload", payload, "empty", new byte[0]));
		var protection = protection(readable, options);
		assertEquals(14, protection.getCompressionMethod());
		assertEquals(encryption, protection.getAlgorithm());
		assertEquals(hash, protection.getHashAlgorithm());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256})
	void supportsMultiblockCentralDirectoryAcrossNativeVolumes(int encryption) throws Exception {
		Path path = directory.resolve("large.zip");
		ZipOptions options = ZipOptions.newBuilder(optionsFor(Layout.NATIVE, Zip64Mode.AUTO, encryption,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256)).compressionLevel(1).build();
		String comment = Base64.getEncoder().encodeToString(payload(48000));
        byte[] other = new byte[48000];
        new java.util.Random(43).nextBytes(other);
        String secondComment = Base64.getEncoder().encodeToString(other);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "first", new byte[] {1}, comment);
			writeEntry(archive, "second", new byte[] {2}, secondComment);
		}
		assertTrue(Files.exists(directory.resolve("large.z01")));
		assertTrue(protection(path, options).getCompressedSize() > 65536);
		assertContent(path, options, Map.of("first", new byte[] {1}, "second", new byte[] {2}));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256})
	void supportsEmptyCentralDirectory(int encryption) throws Exception {
		Path path = directory.resolve("empty.zip");
		ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO, encryption,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32);
		try (ArchiveFile ignored = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) { }
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"), archiveOptions(), options)) {
			assertTrue(archive.entry("missing").isEmpty());
		}
		var protection = protection(path, options);
		assertEquals(0, protection.getUncompressedSize());
		assertTrue(protection.getCompressedSize() >= 19);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void mergePreservesCompressionWithoutSourceWriteOptions(boolean encrypted) throws Exception {
		ZipOptions options = optionsFor(Layout.NATIVE, Zip64Mode.AUTO,
				encrypted ? ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 : 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
		Path source = directory.resolve("split.zip"), merged = directory.resolve("merged.zip");
		byte[] payload = payload(120017);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(source.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "payload", payload);
		}
		ZipOptions reading = ZipOptions.newBuilder().splitArchive(true, 65536).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(source.toString(), "r"), archiveOptions(), reading)) {
			archive.merge(merged);
		}
		assertContent(merged, ZipOptions.newBuilder().build(), Map.of("payload", payload));
		var actual = protection(merged, ZipOptions.newBuilder().build());
		assertEquals(14, actual.getCompressionMethod());
		assertEquals(encrypted ? ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 : 0, actual.getAlgorithm());
		assertEquals(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256, actual.getHashAlgorithm());
	}

	@Test
	void readsIndependentCompressedCentralDirectory() throws Exception {
		byte[] plain = plainCentral();
		Path path = directory.resolve("reference.zip");
		Files.write(path, fixture(compress(plain), plain.length));
		assertNotNull(readFixture(path, ArchiveOptions.newBuilder().build()).header("entry.txt"));
	}

	@Test
	void rewritesLzmaDirectoryAfterEditingEntries() throws Exception {
		Path path = directory.resolve("edit.zip");
		ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO, 0, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
		byte[] kept = payload(1001), added = payload(2017);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "keep", kept);
			writeEntry(archive, "remove", new byte[] {7});
		}
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			archive.remove(archive.entry("remove").orElseThrow());
			writeEntry(archive, "add", added);
		}
		assertContent(path, options, Map.of("keep", kept, "add", added));
		assertEquals(14, protection(path, options).getCompressionMethod());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 8193})
	void rejectsBufferedAndUnbufferedTrailingBytes(int suffixLength) throws Exception {
		byte[] plain = plainCentral(), compressed = compress(plain);
		byte[] extra = Arrays.copyOf(compressed, compressed.length + suffixLength);
		extra[extra.length - 1] = 42;
		Path path = directory.resolve("trailing.zip");
		Files.write(path, fixture(extra, plain.length));
		assertThrows(IOException.class, () -> readFixture(path, ArchiveOptions.newBuilder().build()));
	}

	@Test
	void rejectsSecondMemberCorruptionTruncationAndWrongSizes() throws Exception {
		byte[] plain = plainCentral(), compressed = compress(plain), empty = compress(new byte[0]);
		byte[] concatenated = Arrays.copyOf(compressed, compressed.length + empty.length);
		System.arraycopy(empty, 0, concatenated, compressed.length, empty.length);
		byte[] badBlock = compressed.clone(); badBlock[10] ^= 1;
		byte[] badCombined = compressed.clone(); badCombined[badCombined.length - 2] ^= 0x10;
		for (byte[] bad : new byte[][] {concatenated, badBlock, badCombined, Arrays.copyOf(compressed, compressed.length - 1)}) {
			Path path = directory.resolve("malformed.zip");
			Files.write(path, fixture(bad, plain.length));
			assertThrows(IOException.class, () -> readFixture(path, ArchiveOptions.newBuilder().build()));
		}
		Path path = directory.resolve("wrong-size.zip");
		Files.write(path, fixture(compressed, plain.length + 1));
		assertThrows(IOException.class, () -> readFixture(path, ArchiveOptions.newBuilder().build()));
		Files.write(path, fixture(compressed, plain.length));
		ArchiveOptions limited = ArchiveOptions.newBuilder().limitBuilder().maxHeaderBytes(plain.length - 1).confirm().build();
		assertThrows(IOException.class, () -> readFixture(path, limited));
	}

	/** 哈希覆盖解压后的中央目录，篡改哈希必须被拒绝。 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void rejectsCorruptedCentralDirectoryHash(boolean encrypted) throws Exception {
		ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO,
				encrypted ? ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 : 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
		Path path = directory.resolve("bad-hash.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "entry", new byte[] {1, 2, 3});
		}
		byte[] bytes = Files.readAllBytes(path);
		ByteBuffer raw = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		int locator = bytes.length - 22 - 20;
		assertEquals(0x07064B50, raw.getInt(locator));
		int zip64 = Math.toIntExact(raw.getLong(locator + 8));
		assertEquals(0x06064B50, raw.getInt(zip64));
		assertEquals(32, Short.toUnsignedInt(raw.getShort(zip64 + 82)));
		bytes[zip64 + 84] ^= 1;
		Files.write(path, bytes);
		assertThrows(IOException.class, () -> readFixture(path, archiveOptions()));
	}

	/** 压缩尺寸必须准确限定 LZMA1 成员边界，少一字节或多一字节都应拒绝。 */
	@ParameterizedTest
	@ValueSource(ints = {-1, 1})
	void rejectsIncorrectCompressedSize(int delta) throws Exception {
		byte[] plain = plainCentral(), compressed = compress(plain);
		byte[] bytes = fixture(compressed, plain.length);
		ByteBuffer raw = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		raw.putLong(compressed.length + 58, compressed.length + delta);
		Path path = directory.resolve("bad-compressed-size.zip");
		Files.write(path, bytes);
		assertThrows(IOException.class, () -> readFixture(path, ArchiveOptions.newBuilder().build()));
	}

	@Test
	void rejectsWrongPasswordForLzmaCentralDirectory() throws Exception {
		ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO,
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
		Path path = directory.resolve("wrong-password.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "entry", new byte[] {1, 2, 3});
		}
		ArchiveOptions wrong = ArchiveOptions.newBuilder().passwordProvider(
				org.nervousync.archive.security.password.PasswordProvider.fixed("incorrect".toCharArray())).build();
		assertThrows(IOException.class, () -> readFixture(path, wrong));
	}

	private static ZipFileOperator operator(Path path, ZipOptions options) throws Exception {
		return new ZipFileOperator(new LocalDataFile(path.toString(), "r"), archiveOptions(), options);
	}

	private static Zip64EndCentralDirectory.Protection protection(Path path, ZipOptions options) throws Exception {
		try (ZipFileOperator operator = operator(path, options)) {
			var end = operator.readEndCentralDirectoryRecord();
			return operator.readZip64EndCentralDirectory(end).getProtection();
		}
	}

	private static org.nervousync.archive.zip.models.central.CentralDirectory readFixture(Path path, ArchiveOptions options) throws Exception {
		try (ZipFileOperator operator = new ZipFileOperator(new LocalDataFile(path.toString(), "r"), options, ZipOptions.newBuilder().build())) {
			var end = operator.readEndCentralDirectoryRecord();
			return operator.readCentralDirectory(end, operator.readZip64EndCentralDirectory(end));
		}
	}

	private static byte[] compress(byte[] plain) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		org.tukaani.xz.LZMA2Options options = new org.tukaani.xz.LZMA2Options(1);
        options.setDictSize(65536);
        bytes.write(new byte[]{9, 4, 5, 0, 93, 0, 0, 1, 0});
        try (OutputStream output = new org.tukaani.xz.LZMAOutputStream(bytes, options, true)) { output.write(plain); }
		return bytes.toByteArray();
	}

    private static InputStream independentDecoder(byte[] bytes) throws Exception {
        int dictionary = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(5);
        return new org.tukaani.xz.LZMAInputStream(new ByteArrayInputStream(bytes, 9, bytes.length - 9), -1, bytes[4], dictionary);
    }

	private static byte[] plainCentral() {
		byte[] name = "entry.txt".getBytes(StandardCharsets.UTF_8);
		return ByteBuffer.allocate(46 + name.length + 256).order(ByteOrder.LITTLE_ENDIAN)
				.putInt(0x02014B50).putShort((short) 20).putShort((short) 20).putShort((short) 0)
				.putShort((short) 0).putShort((short) 0).putShort((short) 0).putInt(0).putInt(0).putInt(0)
				.putShort((short) name.length).putShort((short) 0).putShort((short) 256).putShort((short) 0)
				.putShort((short) 0).putInt(0).putInt(0).put(name).put(new byte[256]).array();
	}

	/** ZIP64 V2 envelope built directly, without the production ZIP writer. */
	private static byte[] fixture(byte[] compressed, int originalLength) {
		ByteBuffer bytes = ByteBuffer.allocate(compressed.length + 84 + 20 + 22).order(ByteOrder.LITTLE_ENDIAN);
		bytes.put(compressed).putInt(0x06064B50).putLong(72).putShort((short) 63).putShort((short) 63)
				.putInt(0).putInt(0).putLong(0).putLong(1).putLong(compressed.length).putLong(0)
				.putShort((short) 14).putLong(compressed.length).putLong(originalLength)
				.putShort((short) 0).putShort((short) 0).putShort((short) 0).putShort((short) 0).putShort((short) 0)
				.putInt(0x07064B50).putInt(0).putLong(compressed.length).putInt(1)
				.putInt(0x06054B50).putShort((short) 0).putShort((short) 0).putShort((short) 0)
				.putShort((short) 0xFFFF).putInt(0xFFFFFFFF).putInt(0xFFFFFFFF).putShort((short) 0);
		return bytes.array();
	}
}
