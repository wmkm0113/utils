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
package org.nervousync.test.archive.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.models.central.CentralDirectory;
import org.nervousync.archive.zip.models.central.EndCentralDirectoryRecord;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.operator.ZipFileOperator;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.security.CryptoAdaptor;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;

final class Zip64ProtectedCentralDirectoryTest {

	@TempDir
	Path tempDirectory;
	private static final char[] PASSWORD = "central-directory-password".toCharArray();
	private static final long OUT_OF_LIMIT_ERROR_CODE = 0x0000001A003EL;

	@Test
	void rejectsDeflatedCentralDirectoryByUncompressedHeaderSize() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] compressedDirectory = deflate(centralDirectory);

		assertTrue(compressedDirectory.length < centralDirectory.length,
				"Test data must be smaller after deflate");

		byte[] archive = archiveData(centralDirectory, ZipGlobals.COMP_DEFLATE,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256, digest("SHA-256", centralDirectory),
				new byte[0], new byte[0]);

		ArchiveOptions options = optionsWithMaxHeaderBytes(centralDirectory.length - 1L);
		ArchiveException exception = assertThrows(ArchiveException.class, () -> readArchive(archive, options));
		assertEquals(OUT_OF_LIMIT_ERROR_CODE, exception.getErrorCode());
	}

	@Test
	void acceptsDeflatedCentralDirectoryAtUncompressedHeaderLimit() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] archive = archiveData(centralDirectory, ZipGlobals.COMP_DEFLATE,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256, digest("SHA-256", centralDirectory),
				new byte[0], new byte[0]);

		ArchiveOptions options = optionsWithMaxHeaderBytes(centralDirectory.length);
		CentralDirectory result = readArchive(archive, options);
		assertCentralDirectory(result);
	}

	@Test
	void rejectsStoredProtectedCentralDirectoryLargerThanHeaderLimit() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] archive = archiveData(centralDirectory, ZipGlobals.COMP_STORE,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256, digest("SHA-256", centralDirectory),
				new byte[0], new byte[0]
		);
		ArchiveOptions options = optionsWithMaxHeaderBytes(centralDirectory.length - 1L);
		ArchiveException exception = assertThrows(ArchiveException.class, () -> readArchive(archive, options));
		assertEquals(OUT_OF_LIMIT_ERROR_CODE, exception.getErrorCode());
	}

	@Test
	void acceptsAes256EncryptedStoredCentralDirectory() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] archive = encryptedArchiveData(centralDirectory, ZipGlobals.COMP_STORE);

		ArchiveOptions options = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed(PASSWORD))
				.build();

		CentralDirectory result = readArchive(archive, options);
		assertCentralDirectory(result);
	}

	@Test
	void acceptsAes256EncryptedAndDeflatedCentralDirectory() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] archive = encryptedArchiveData(centralDirectory, ZipGlobals.COMP_DEFLATE);
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed(PASSWORD))
				.build();
		CentralDirectory result = readArchive(archive, options);
		assertCentralDirectory(result);
	}

	@Test
	void rejectsIncorrectCentralDirectoryPassword() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] archive = encryptedArchiveData(centralDirectory, ZipGlobals.COMP_STORE);
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("incorrect".toCharArray()))
				.build();
		assertThrows(ArchiveException.class, () -> readArchive(archive, options));
	}

	@Test
	void rejectsStoredCentralDirectorySizeMismatch() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] archive =
				archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256,
						digest("SHA-256", centralDirectory), new byte[0], new byte[0]);
		ByteBuffer.wrap(archive)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putLong(centralDirectory.length + 66, centralDirectory.length + 1L);
		assertThrows(ArchiveException.class, () -> readArchive(archive));
	}

	@Test
	void acceptsSha256Hash() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = digest("SHA-256", centralDirectory);
		CentralDirectory result = readArchive(
				archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256,
						hash, new byte[0], new byte[0]));
		assertCentralDirectory(result);
	}

	@Test
	void rejectsIncorrectSha256Hash() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = digest("SHA-256", centralDirectory);
		hash[0] ^= 0x01;
		byte[] archive = archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256,
				hash, new byte[0], new byte[0]);
		assertThrows(ArchiveException.class, () -> readArchive(archive));
	}

	@Test
	void acceptsSha384Hash() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = digest("SHA-384", centralDirectory);
		CentralDirectory result = readArchive(
				archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA384,
						hash, new byte[0], new byte[0]));
		assertCentralDirectory(result);
	}

	@Test
	void rejectsIncorrectSha384Hash() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = digest("SHA-384", centralDirectory);
		hash[0] ^= 0x01;
		byte[] archive = archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA384,
				hash, new byte[0], new byte[0]);
		assertThrows(ArchiveException.class, () -> readArchive(archive));
	}

	@Test
	void acceptsSha512Hash() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = digest("SHA-512", centralDirectory);
		CentralDirectory result = readArchive(
				archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA512,
						hash, new byte[0], new byte[0]));
		assertCentralDirectory(result);
	}

	@Test
	void rejectsIncorrectSha512Hash() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = digest("SHA-512", centralDirectory);
		hash[0] ^= 0x01;
		byte[] archive = archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA512,
				hash, new byte[0], new byte[0]);
		assertThrows(ArchiveException.class, () -> readArchive(archive));
	}

	@Test
	void acceptsCrc32InLittleEndianOrder() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		CentralDirectory result = readArchive(
				archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32,
						crc32(centralDirectory), new byte[0], new byte[0]));
		assertCentralDirectory(result);
	}

	@Test
	void acceptsRipemd160Hash() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = MessageDigest.getInstance(
				"RIPEMD160", new BouncyCastleProvider()
		).digest(centralDirectory);

		CentralDirectory result = readArchive(
				archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_RIPEMD160,
						hash, new byte[0], new byte[0]));
		assertCentralDirectory(result);
	}

	@Test
	void acceptsDeflatedCentralDirectory() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = digest("SHA-256", centralDirectory);

		CentralDirectory result = readArchive(
				archiveData(centralDirectory, ZipGlobals.COMP_DEFLATE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256,
						hash, new byte[0], new byte[0]));
		assertCentralDirectory(result);
	}

	@Test
	void rejectsTrailingBytesAfterDeflateStream() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] hash = digest("SHA-256", centralDirectory);

		byte[] archive = archiveData(centralDirectory, ZipGlobals.COMP_DEFLATE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256,
				hash, new byte[]{0x5A}, new byte[0]);

		assertThrows(ArchiveException.class, () -> readArchive(archive));
	}

	@Test
	void readsExternalDigitalSignature() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] signatureData = {0x11, 0x22, 0x33, 0x44};

		CentralDirectory result = readArchive(
				archiveData(centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256,
						digest("SHA-256", centralDirectory), new byte[0], signatureData));

		assertArrayEquals(signatureData, result.getSignatureData());
	}

	@Test
	void rejectsExternalDigitalSignatureCrossingZip64EndRecordBoundary() throws Exception {
		byte[] centralDirectory = centralDirectoryData();
		byte[] signatureData = {0x11, 0x22, 0x33, 0x44};
		byte[] archive = archiveData(
				centralDirectory, ZipGlobals.COMP_STORE, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256,
				digest("SHA-256", centralDirectory), new byte[0], signatureData);

		ByteBuffer.wrap(archive, centralDirectory.length + Integer.BYTES, Short.BYTES)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) (signatureData.length + Integer.BYTES));

		assertThrows(ArchiveException.class, () -> readArchive(archive));
	}

	@Test
	void readsEncryptedCentralDirectoryWithArchiveExtraData() throws Exception {
		byte[] extraData = {0x11, 0x22, 0x33, 0x44};
		byte[] protectedDirectory = join(archiveExtraDataRecord(extraData), centralDirectoryData());
		byte[] archive = encryptedArchiveData(protectedDirectory, ZipGlobals.COMP_STORE);
		ArchiveOptions options = ArchiveOptions.newBuilder().passwordProvider(PasswordProvider.fixed(PASSWORD)).build();
		CentralDirectory result = readArchive(archive, options);
		assertArrayEquals(extraData, result.getArchiveExtraData());
		assertCentralDirectory(result);
	}

	@Test
	void readsEncryptedCentralDirectoryAcrossSplitBoundary() throws Exception {
		byte[] archive = encryptedArchiveData(centralDirectoryData(), ZipGlobals.COMP_STORE);
		int locatorOffset = archive.length - 22 - 20;
		long zip64RecordOffset = ByteBuffer.wrap(archive).order(ByteOrder.LITTLE_ENDIAN).getLong(locatorOffset + 8);
		int splitOffset = Math.toIntExact(zip64RecordOffset / 2L);
		ArchiveOptions options = ArchiveOptions.newBuilder().passwordProvider(PasswordProvider.fixed(PASSWORD)).build();
		CentralDirectory result = readSplitArchive(archive, splitOffset, options);
		assertCentralDirectory(result);
	}

	private CentralDirectory readArchive(final byte[] archiveData) throws Exception {
		return readArchive(archiveData, ArchiveOptions.newBuilder().build());
	}

	private CentralDirectory readArchive(final byte[] archiveData, final ArchiveOptions options) throws Exception {
		Path archivePath = Files.createTempFile(this.tempDirectory, "zip64-protected-", ".zip");
		Files.write(archivePath, archiveData);
		try (ZipFileOperator operator =
				     new ZipFileOperator(new LocalDataFile(archivePath.toString(), "r"), ArchiveOptions.newBuilder(options).strict(true).build(),
						     ZipOptions.newBuilder().build())) {
			EndCentralDirectoryRecord endRecord = operator.readEndCentralDirectoryRecord();
			Zip64EndCentralDirectory zip64Record = operator.readZip64EndCentralDirectory(endRecord);
			assertNotNull(zip64Record);
			assertNotNull(zip64Record.getProtection());
			return operator.readCentralDirectory(endRecord, zip64Record);
		}
	}

	private CentralDirectory readSplitArchive(final byte[] archiveData, final int splitOffset,
	                                          final ArchiveOptions options) throws Exception {
		byte[] patchedArchive = Arrays.copyOf(archiveData, archiveData.length);
		ByteBuffer buffer = ByteBuffer.wrap(patchedArchive).order(ByteOrder.LITTLE_ENDIAN);
		int eocdOffset = patchedArchive.length - 22;
		int locatorOffset = eocdOffset - 20;
		long absoluteZip64Offset = buffer.getLong(locatorOffset + 8);
		if (splitOffset <= 0 || splitOffset >= absoluteZip64Offset) {
			throw new IllegalArgumentException("Split offset must be inside protected directory");
		}
		int zip64Offset = Math.toIntExact(absoluteZip64Offset);

		buffer.putInt(zip64Offset + 16, 1);
		buffer.putInt(zip64Offset + 20, 0);
		buffer.putInt(locatorOffset + 4, 1);
		buffer.putLong(locatorOffset + 8, absoluteZip64Offset - splitOffset);
		buffer.putInt(locatorOffset + 16, 2);
		buffer.putShort(eocdOffset + 4, (short) 1);
		buffer.putShort(eocdOffset + 6, (short) 0);

		Path splitDirectory = Files.createDirectory(this.tempDirectory.resolve("protected-split"));

		Path firstPart = splitDirectory.resolve("protected.z01");
		Path finalPart = splitDirectory.resolve("protected.zip");

		Files.write(firstPart, Arrays.copyOfRange(patchedArchive, 0, splitOffset));
		Files.write(finalPart, Arrays.copyOfRange(patchedArchive, splitOffset, patchedArchive.length));

		long splitLimit = Math.max(splitOffset, patchedArchive.length - splitOffset);
		try (ZipFileOperator operator =
				     new ZipFileOperator(new LocalDataFile(finalPart.toString(), "r"), ArchiveOptions.newBuilder(options).strict(true).build(),
						     ZipOptions.newBuilder().splitArchive(true, splitLimit).build())) {
			EndCentralDirectoryRecord endRecord = operator.readEndCentralDirectoryRecord();
			Zip64EndCentralDirectory zip64Record = operator.readZip64EndCentralDirectory(endRecord);
			assertNotNull(zip64Record);
			return operator.readCentralDirectory(endRecord, zip64Record);
		}
	}

	private static byte[] archiveExtraDataRecord(final byte[] extraData) {
		return ByteBuffer
				.allocate(8 + extraData.length)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putInt((int) ZipGlobals.EXTRA_DATA_RECORD_SIGNATURE)
				.putInt(extraData.length)
				.put(extraData)
				.array();
	}

	private static void assertCentralDirectory(final CentralDirectory centralDirectory) {
		assertNotNull(centralDirectory);
		assertEquals(1, centralDirectory.getFileHeaders().size());
		assertEquals("entry.txt", centralDirectory.getFileHeaders().get(0).getEntryPath());
	}

	private static byte[] archiveData(final byte[] centralDirectory, final int compressionMethod,
			final int hashAlgorithm, final byte[] hashData, final byte[] compressedSuffix, final byte[] signatureData) {

		byte[] compressedData =
				compressionMethod == ZipGlobals.COMP_DEFLATE
						? deflate(centralDirectory)
						: Arrays.copyOf(centralDirectory, centralDirectory.length);

		byte[] protectedData = join(compressedData, compressedSuffix);
		byte[] digitalSignature = digitalSignature(signatureData);

		int v2Length = 28 + hashData.length;
		int zip64RecordLength = 56 + v2Length;

		long zip64RecordOffset = (long) protectedData.length + digitalSignature.length;

		int totalLength = Math.toIntExact(zip64RecordOffset + zip64RecordLength + 20L + 22L);

		ByteBuffer buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);

		// Protected central-directory data.
		buffer.put(protectedData);

		// Digital Signature is not compressed or encrypted.
		buffer.put(digitalSignature);

		// ZIP64 End of Central Directory Record.
		buffer.putInt((int) ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE);
		buffer.putLong(44L + v2Length);
		buffer.putShort((short) 63);
		buffer.putShort((short) 62);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putLong(0L);
		buffer.putLong(1L);
		buffer.putLong(centralDirectory.length);
		buffer.putLong(0L);

		// ZIP64 V2 protection fields.
		buffer.putShort((short) compressionMethod);
		buffer.putLong(protectedData.length);
		buffer.putLong(centralDirectory.length);
		buffer.putShort((short)
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putShort((short) hashAlgorithm);
		buffer.putShort((short) hashData.length);
		buffer.put(hashData);

		// ZIP64 End of Central Directory Locator.
		buffer.putInt((int) ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE);
		buffer.putInt(0);
		buffer.putLong(zip64RecordOffset);
		buffer.putInt(1);

		// End of Central Directory Record.
		buffer.putInt((int) ZipGlobals.END_OF_CENTRAL_DIRECTORY_SIGNATURE);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0xFFFF);
		buffer.putInt(0xFFFFFFFF);
		buffer.putInt(0xFFFFFFFF);
		buffer.putShort((short) 0);

		return buffer.array();
	}

	private static byte[] centralDirectoryData() {
		byte[] entryName = "entry.txt".getBytes(StandardCharsets.UTF_8);
		byte[] comment = new byte[64];

		ByteBuffer buffer =
				ByteBuffer.allocate(46 + entryName.length + comment.length).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt((int) ZipGlobals.GENERAL_FILE_HEADER_SIGNATURE);
		buffer.putShort((short) 20);
		buffer.putShort((short) 20);
		buffer.putShort((short) 0x0800);
		buffer.putShort((short) ZipGlobals.COMP_STORE);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putShort((short) entryName.length);
		buffer.putShort((short) 0);
		buffer.putShort((short) comment.length); // comment length
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.put(entryName);
		buffer.put(comment);

		return buffer.array();
	}

	private static byte[] digitalSignature(final byte[] signatureData) {
		if (signatureData.length == 0) {
			return new byte[0];
		}

		ByteBuffer buffer = ByteBuffer.allocate(6 + signatureData.length).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt((int) ZipGlobals.DIGITAL_SIGNATURE);
		buffer.putShort((short) signatureData.length);
		buffer.put(signatureData);
		return buffer.array();
	}

	private static byte[] digest(final String algorithm, final byte[] data) throws Exception {
		return MessageDigest.getInstance(algorithm, new BouncyCastleProvider()).digest(data);
	}

	private static byte[] crc32(final byte[] data) {
		CRC32 crc32 = new CRC32();
		crc32.update(data);

		return ByteBuffer.allocate(Integer.BYTES)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putInt((int) crc32.getValue())
				.array();
	}

	private static byte[] deflate(final byte[] data) {
		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

		try {
			deflater.setInput(data);
			deflater.finish();

			ByteArrayOutputStream output = new ByteArrayOutputStream();

			byte[] buffer = new byte[256];
			while (!deflater.finished()) {
				int length = deflater.deflate(buffer);
				if (length == 0 && deflater.needsInput()) {
					throw new IllegalStateException("Deflater unexpectedly needs more input");
				}
				output.write(buffer, 0, length);
			}
			return output.toByteArray();
		} finally {
			deflater.end();
		}
	}

	private static byte[] join(final byte[] first, final byte[] second) {
		byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	private static byte[] encryptedArchiveData(final byte[] centralDirectory, final int compressionMethod)
			throws Exception {

		int algorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256;
		int keyBitLength = 256;
		int flags = ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD;

		EncryptionProvider cipher = EncryptionProvider.resolve(algorithm);

		byte[] compressedData =
				(compressionMethod == ZipGlobals.COMP_DEFLATE)
						? deflate(centralDirectory)
						: Arrays.copyOf(centralDirectory, centralDirectory.length);

		byte[] ivData = new byte[cipher.blockSize()];
		byte[] randomData = new byte[cipher.blockSize()];

		for (int i = 0; i < cipher.blockSize(); i++) {
			ivData[i] = (byte) (0x10 + i);
			randomData[i] = (byte) (0x40 + i);
		}

		byte[] masterSessionKey = cipher.deriveKey(ZipCrypto.encodePassword(PASSWORD, true), keyBitLength);

		byte[] encryptedRandomData = cipher.encryptRandomData(randomData, masterSessionKey, ivData);

		byte[] sessionMaterial = join(ivData, randomData);
		byte[] fileSessionKey = cipher.deriveKey(sessionMaterial, keyBitLength);

		/*
		 * 验证数据使用 12 字节内容和 4 字节 little-endian CRC32，
		 * 总长度保持为一个 AES block。
		 */
		byte[] validationData = new byte[16];
		for (int i = 0; i < 12; i++) {
			validationData[i] = (byte) (0x70 + i);
		}

		CRC32 validationCrc = new CRC32();
		validationCrc.update(validationData, 0, 12);

		ByteBuffer.wrap(validationData)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putInt(12, (int) validationCrc.getValue());

		int paddingSize = cipher.paddingSize(compressedData.length);
		byte[] paddedCentralDirectory = Arrays.copyOf(compressedData, compressedData.length + paddingSize);

		Arrays.fill(paddedCentralDirectory, compressedData.length, paddedCentralDirectory.length, (byte) paddingSize);

		/*
		 * 验证数据和中央目录必须使用同一个 CBC 加密器连续加密，
		 * 否则解密端的 CBC chaining state 无法对应。
		 */
		CryptoAdaptor encryptor = cipher.encryptor(fileSessionKey, ivData);

		byte[] encryptedPayload = encryptor.finish(join(validationData, paddedCentralDirectory));

		byte[] encryptedValidationData = Arrays.copyOfRange(encryptedPayload, 0, validationData.length);

		byte[] encryptedCentralDirectory =
				Arrays.copyOfRange(encryptedPayload, validationData.length, encryptedPayload.length);

		int decryptionHeaderRemaining =
				10 + encryptedRandomData.length + Integer.BYTES + Short.BYTES + encryptedValidationData.length;

		ByteBuffer decryptionHeader = ByteBuffer
				.allocate(Short.BYTES + ivData.length + 14 + encryptedRandomData.length + Integer.BYTES + Short.BYTES + encryptedValidationData.length)
				.order(ByteOrder.LITTLE_ENDIAN);

		decryptionHeader.putShort((short) ivData.length);
		decryptionHeader.put(ivData);
		decryptionHeader.putInt(decryptionHeaderRemaining);
		decryptionHeader.putShort((short) 3);
		decryptionHeader.putShort((short) algorithm);
		decryptionHeader.putShort((short) keyBitLength);
		decryptionHeader.putShort((short) flags);
		decryptionHeader.putShort((short) encryptedRandomData.length);
		decryptionHeader.put(encryptedRandomData);
		decryptionHeader.putInt(0); // recipient count
		decryptionHeader.putShort((short) encryptedValidationData.length);
		decryptionHeader.put(encryptedValidationData);

		byte[] protectedData = join(decryptionHeader.array(), encryptedCentralDirectory);

		byte[] hashData = digest("SHA-256", centralDirectory);

		int v2Length = 28 + hashData.length;
		int zip64RecordLength = 56 + v2Length;
		long zip64RecordOffset = protectedData.length;

		int totalLength = Math.toIntExact(zip64RecordOffset + zip64RecordLength + 20L + 22L);

		ByteBuffer archive = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);

		archive.put(protectedData);

		// ZIP64 End of Central Directory Record
		archive.putInt((int) ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE);
		archive.putLong(44L + v2Length);
		archive.putShort((short) 63);
		archive.putShort((short) 62);
		archive.putInt(0);
		archive.putInt(0);
		archive.putLong(0L);
		archive.putLong(1L);
		archive.putLong(centralDirectory.length);
		archive.putLong(0L);

		// ZIP64 V2 protection fields
		archive.putShort((short) compressionMethod);
		archive.putLong(compressedData.length);
		archive.putLong(centralDirectory.length);
		archive.putShort((short) algorithm);
		archive.putShort((short) keyBitLength);
		archive.putShort((short) flags);
		archive.putShort((short)
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
		archive.putShort((short) hashData.length);
		archive.put(hashData);

		// ZIP64 locator
		archive.putInt((int) ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE);
		archive.putInt(0);
		archive.putLong(zip64RecordOffset);
		archive.putInt(1);

		// EOCD
		archive.putInt((int) ZipGlobals.END_OF_CENTRAL_DIRECTORY_SIGNATURE);
		archive.putShort((short) 0);
		archive.putShort((short) 0);
		archive.putShort((short) 0);
		archive.putShort((short) 0xFFFF);
		archive.putInt(0xFFFFFFFF);
		archive.putInt(0xFFFFFFFF);
		archive.putShort((short) 0);

		return archive.array();
	}

	private static ArchiveOptions optionsWithMaxHeaderBytes(final long maxHeaderBytes) {
		return ArchiveOptions.newBuilder()
				.limitBuilder()
				.maxHeaderBytes(maxHeaderBytes)
				.confirm()
				.build();
	}
}
