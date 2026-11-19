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
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.zip.AesVendor;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.ExtraDataRecord;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.models.header.GeneralFileHeader;
import org.nervousync.archive.zip.models.header.LocalFileHeader;
import org.nervousync.archive.zip.operator.ZipFileOperator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class LocalFileHeaderTest {

	@TempDir
	Path tempDirectory;
	private static final long OUT_OF_LIMIT_ERROR_CODE = 0x0000001A003EL;

	@Test
	void rejectsLocalFileHeaderLargerThanHeaderLimit() {
		String entryName = "header-limit.txt";
		byte[] localHeader =
				localHeader(20, 0x0800, ZipGlobals.COMP_STORE, 0, 0,
				0L, 0L, 0L, entryName, new byte[0]);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 20, 0x0800, ZipGlobals.COMP_STORE, 0,
						0, 0L, 0L, 0L);

		long maxHeaderBytes = localHeader.length - 1L;
		ArchiveException exception =
				assertThrows(ArchiveException.class,
						() -> readLocalHeader(localHeader, generalHeader, maxHeaderBytes));

		assertEquals(OUT_OF_LIMIT_ERROR_CODE, exception.getErrorCode());
	}

	@Test
	void acceptsLocalFileHeaderAtHeaderLimit() throws Exception {
		String entryName = "header-boundary.txt";
		byte[] localHeader =
				localHeader(20, 0x0800, ZipGlobals.COMP_STORE, 0, 0,
				0L, 0L, 0L, entryName, new byte[0]);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 20, 0x0800, ZipGlobals.COMP_STORE, 0,
				0, 0L, 0L, 0L);

		LocalFileHeader result = readLocalHeader(localHeader, generalHeader, localHeader.length);

		assertEquals(entryName, result.getEntryPath());
		assertEquals(localHeader.length, result.getOffsetStartOfData());
	}

	@Test
	void countsExtraFieldInLocalHeaderLimit() {
		String entryName = "extra-limit.txt";
		byte[] extraField = ByteBuffer
				.allocate(Short.BYTES * 2 + 32)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) 0xCAFE)
				.putShort((short) 32)
				.put(new byte[32])
				.array();

		byte[] localHeader =
				localHeader(20, 0x0800, ZipGlobals.COMP_STORE, 0, 0, 0L,
				0L, 0L, entryName, extraField);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 20, 0x0800, ZipGlobals.COMP_STORE, 0,
						0, 0L, 0L, 0L);

		long sizeWithoutExtraField = 30L + entryName.getBytes(StandardCharsets.UTF_8).length;
		ArchiveException exception =
				assertThrows(ArchiveException.class,
						() -> readLocalHeader(localHeader, generalHeader, sizeWithoutExtraField));

		assertEquals(OUT_OF_LIMIT_ERROR_CODE, exception.getErrorCode());
	}

	@Test
	void readsOrdinaryLocalFileHeader() throws Exception {
		String entryName = "directory/entry.txt";
		int flags = 0x0800;
		int modifiedTime = 0x4A21;
		int modifiedDate = 0x579A;
		long crc32 = 0x89ABCDEFL;
		long size = 3L;

		byte[] localHeader =
				localHeader(20, flags, ZipGlobals.COMP_STORE,
						modifiedTime, modifiedDate, crc32, size, size, entryName, new byte[0]);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 20, flags, ZipGlobals.COMP_STORE,
						modifiedTime, modifiedDate, crc32, size, size);

		LocalFileHeader result = readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE);

		assertEquals(ZipGlobals.LOCAL_FILE_HEADER_SIGNATURE, Integer.toUnsignedLong(result.getSignature()));
		assertEquals(entryName, result.getEntryPath());
		assertEquals(ZipGlobals.COMP_STORE, result.getCompressionMethod());
		assertEquals(crc32, result.getCrc32());
		assertEquals(size, result.getCompressedSize());
		assertEquals(size, result.getUncompressedSize());

		assertEquals(0L, result.getDiskNumberStart());
		assertEquals(0L, result.getOffsetLocalHeader());
		assertEquals(localHeader.length, result.getOffsetStartOfData());

		assertNotNull(result.getExtraDataRecords());
		assertTrue(result.getExtraDataRecords().isEmpty());
	}

	@Test
	void readsLargeDataDescriptorEntryWithoutLocalZip64Extra() throws Exception {
		String entryName = "large-data.bin";
		int flags = 0x0800 | 0x0008;
		long compressedSize = ZipGlobals.ZIP_64_LIMIT + 1L;
		long uncompressedSize = ZipGlobals.ZIP_64_LIMIT + 1024L;
		long crc32 = 0x12345678L;

		byte[] localHeader =
				localHeader(20, flags, ZipGlobals.COMP_DEFLATE,
						0, 0, 0L, 0L, 0L, entryName, new byte[0]);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, ZipGlobals.ZIP_64_VERSION, flags, ZipGlobals.COMP_DEFLATE,
						0, 0, crc32, compressedSize, uncompressedSize);

		// 中央目录使用 ZIP64，但 Local Header 可以不包含 ZIP64 Extra。
		generalHeader.setExtraDataRecords(Collections.singletonList(
				new ExtraDataRecord(ZipGlobals.ZIP64_EXTRA_HEADER_ID, zip64Data(uncompressedSize, compressedSize))));

		LocalFileHeader result = readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE);

		assertTrue(result.hasDataDescriptor());
		assertEquals(crc32, result.getCrc32());
		assertEquals(compressedSize, result.getCompressedSize());
		assertEquals(uncompressedSize, result.getUncompressedSize());
		assertEquals(localHeader.length, result.getOffsetStartOfData());
	}

	@Test
	void readsLocalZip64ExtendedSizes() throws Exception {
		String entryName = "zip64-entry.bin";
		int flags = 0x0800;
		long compressedSize = ZipGlobals.ZIP_64_LIMIT + 100L;
		long uncompressedSize = ZipGlobals.ZIP_64_LIMIT + 200L;
		long crc32 = 0x10203040L;

		byte[] zip64Extra = extraRecord(ZipGlobals.ZIP64_EXTRA_HEADER_ID, zip64Data(uncompressedSize, compressedSize));

		byte[] localHeader =
				localHeader(ZipGlobals.ZIP_64_VERSION, flags, ZipGlobals.COMP_STORE,
						0, 0, crc32, ZipGlobals.ZIP_64_LIMIT, ZipGlobals.ZIP_64_LIMIT, entryName, zip64Extra);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, ZipGlobals.ZIP_64_VERSION, flags, ZipGlobals.COMP_STORE,
						0, 0, crc32, compressedSize, uncompressedSize);

		LocalFileHeader result = readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE);

		assertEquals(compressedSize, result.getCompressedSize());
		assertEquals(uncompressedSize, result.getUncompressedSize());
		assertEquals(1, result.getExtraDataRecords().size());
		assertEquals(ZipGlobals.ZIP64_EXTRA_HEADER_ID, result.getExtraDataRecords().get(0).headerId());
		assertEquals(localHeader.length, result.getOffsetStartOfData());
	}

	@Test
	void rejectsMissingLocalZip64Extra() {
		String entryName = "missing-zip64.bin";

		byte[] localHeader =
				localHeader(ZipGlobals.ZIP_64_VERSION, 0x0800, ZipGlobals.COMP_STORE,
						0, 0, 0L, ZipGlobals.ZIP_64_LIMIT, ZipGlobals.ZIP_64_LIMIT, entryName, new byte[0]);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, ZipGlobals.ZIP_64_VERSION, 0x0800, ZipGlobals.COMP_STORE,
						0, 0, 0L, ZipGlobals.ZIP_64_LIMIT + 1L,
						ZipGlobals.ZIP_64_LIMIT + 1L);

		assertThrows(ArchiveException.class, () -> readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE));
	}

	@Test
	void readsAesLocalFileHeader() throws Exception {
		String entryName = "encrypted.bin";
		int flags = 0x0800 | 0x0001;
		long compressedSize = 128L;
		long uncompressedSize = 256L;

		byte[] aesExtra =
				extraRecord(ZipGlobals.AES_ENC_HEADER_ID, aesData(2, 3, ZipGlobals.COMP_DEFLATE));

		byte[] localHeader =
				localHeader(51, flags, ZipGlobals.COMP_AES,
						0, 0, 0L, compressedSize, uncompressedSize, entryName, aesExtra);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 51, flags, ZipGlobals.COMP_DEFLATE,
						0, 0, 0L, compressedSize, uncompressedSize);

		generalHeader.setEncryptionInfo(new AesEncryptionInfo(AesVendor.AE_2, 3));

		LocalFileHeader result = readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE);

		assertTrue(result.encrypted());
		assertEquals(ZipGlobals.COMP_DEFLATE, result.getCompressionMethod());
		assertNotNull(result.getEncryptionInfo());
		assertEquals(AesVendor.AE_2, ((AesEncryptionInfo) result.getEncryptionInfo()).getVendor());
		assertEquals(3, ((AesEncryptionInfo) result.getEncryptionInfo()).getStrength());
	}

	@Test
	void rejectsAesExtraWhenDeclaredMethodIsNotAes() {
		String entryName = "invalid-aes-method.bin";
		int flags = 0x0800 | 0x0001;

		byte[] aesExtra =
				extraRecord(ZipGlobals.AES_ENC_HEADER_ID, aesData(2, 3, ZipGlobals.COMP_DEFLATE));

		byte[] localHeader =
				localHeader(51, flags, ZipGlobals.COMP_DEFLATE,
						0, 0, 0L, 10L, 10L, entryName, aesExtra);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 51, flags, ZipGlobals.COMP_DEFLATE,
						0, 0, 0L, 10L, 10L);

		assertThrows(ArchiveException.class, () -> readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE));
	}

	@Test
	void rejectsInvalidAesStrengthZero() {
		String entryName = "invalid-aes-strength.bin";
		int flags = 0x0800 | 0x0001;

		byte[] aesExtra =
				extraRecord(ZipGlobals.AES_ENC_HEADER_ID, aesData(2, 0, ZipGlobals.COMP_DEFLATE));

		byte[] localHeader =
				localHeader(51, flags, ZipGlobals.COMP_AES, 0, 0,
						0L, 10L, 10L, entryName, aesExtra);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 51, flags, ZipGlobals.COMP_DEFLATE,
						0, 0, 0L, 10L, 10L);

		assertThrows(ArchiveException.class, () -> readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE));
	}

	@Test
	void readsAe1StoredLocalFileHeader() throws Exception {
		String entryName = "aes-ae1-stored.bin";

		int flags = 0x0800 | 0x0001;
		int aesStrength = ZipGlobals.AES_STRENGTH_128;

		long uncompressedSize = 64L;
		long compressedSize = uncompressedSize + 8L + 2L + ZipGlobals.AES_AUTH_LENGTH;

		long crc32 = 0x12345678L;

		byte[] aesExtra =
				extraRecord(ZipGlobals.AES_ENC_HEADER_ID, aesData(1, aesStrength, ZipGlobals.COMP_STORE));

		byte[] localHeader = localHeader(51, flags, ZipGlobals.COMP_AES, 0, 0,
						crc32, compressedSize, uncompressedSize, entryName, aesExtra);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 51, flags, ZipGlobals.COMP_STORE,
						0, 0, crc32, compressedSize, uncompressedSize);

		generalHeader.setEncryptionInfo(new AesEncryptionInfo(AesVendor.AE_1, aesStrength));

		LocalFileHeader result = readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE);

		assertTrue(result.encrypted());
		assertFalse(result.hasDataDescriptor());

		assertEquals(ZipGlobals.COMP_STORE, result.getCompressionMethod());

		assertNotNull(result.getEncryptionInfo());
		assertEquals(AesVendor.AE_1, ((AesEncryptionInfo) result.getEncryptionInfo()).getVendor());
		assertEquals(aesStrength, ((AesEncryptionInfo) result.getEncryptionInfo()).getStrength());

		assertEquals(crc32, result.getCrc32());
		assertEquals(compressedSize, result.getCompressedSize());
		assertEquals(uncompressedSize, result.getUncompressedSize());

		assertEquals(localHeader.length, result.getOffsetStartOfData());
	}

	@Test
	void rejectsNullAesVendor() {
		assertThrows(NullPointerException.class, () -> new AesEncryptionInfo(null, 3));
	}

	@Test
	void rejectsTruncatedLocalFileHeader() {
		String entryName = "truncated.txt";
		byte[] completeHeader =
				localHeader(20, 0x0800, ZipGlobals.COMP_STORE,
						0, 0, 0L, 0L, 0L, entryName, new byte[0]);

		byte[] truncatedHeader = Arrays.copyOf(completeHeader, completeHeader.length - 1);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 20, 0x0800, ZipGlobals.COMP_STORE,
						0, 0, 0L, 0L, 0L);

		assertThrows(ArchiveException.class, () -> readLocalHeader(truncatedHeader, generalHeader, Long.MAX_VALUE));
	}

	@Test
	void rejectsIncorrectLocalHeaderSignature() {
		String entryName = "bad-signature.txt";

		byte[] localHeader =
				localHeader(20, 0x0800, ZipGlobals.COMP_STORE,
						0, 0, 0L, 0L, 0L, entryName, new byte[0]);

		ByteBuffer.wrap(localHeader).order(ByteOrder.LITTLE_ENDIAN).putInt(0, 0x12345678);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 20, 0x0800, ZipGlobals.COMP_STORE,
						0, 0, 0L, 0L, 0L);

		assertThrows(ArchiveException.class, () -> readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE));
	}

	@Test
	void rejectsLocalHeaderCrossingSplitBoundary() throws Exception {
		String entryName = "split-boundary.txt";

		byte[] localHeader =
				localHeader(20, 0x0800, ZipGlobals.COMP_STORE, 0,
						0, 0L, 0L, 0L, entryName, new byte[0]);

		Path firstPart = this.tempDirectory.resolve("split-test.zip.001");
		Path finalPart = this.tempDirectory.resolve("split-test.zip");

		// 固定 30 字节位于第一卷，但文件名跨越卷边界。
		int splitPosition = 31;
		Files.write(firstPart, Arrays.copyOfRange(localHeader, 0, splitPosition));
		Files.write(finalPart, Arrays.copyOfRange(localHeader, splitPosition, localHeader.length));

		GeneralFileHeader generalHeader =
				generalHeader(entryName, 20, 0x0800, ZipGlobals.COMP_STORE,
						0, 0, 0L, 0L, 0L);

		try (ZipFileOperator operator =
				     new ZipFileOperator(new LocalDataFile(finalPart.toString(), "r"), ArchiveOptions.newBuilder().strict(true).build(),
						     ZipOptions.newBuilder().splitArchive(true, 64L * 1024L).build())) {
			assertThrows(ArchiveException.class,
					() -> operator.readLocalFileHeader(generalHeader, StandardCharsets.UTF_8, Long.MAX_VALUE));
		}
	}

	/**
	 * 回归测试：包含 ":/" 的合法条目名称不得被截断。
	 * 旧实现会把 "urn:/data.txt" 错误解析为 "data.txt"。
	 */
	@Test
	void preservesEntryPathContainingColonSlash() throws Exception {
		String entryName = "urn:/data.txt";
		int flags = 0x0800;
		byte[] localHeader =
				localHeader(20, flags, ZipGlobals.COMP_STORE, 0, 0, 0L,
						0L, 0L, entryName, new byte[0]);
		GeneralFileHeader generalHeader =
				generalHeader(entryName, 20, flags, ZipGlobals.COMP_STORE, 0, 0,
						0L, 0L, 0L);
		LocalFileHeader result = readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE);
		assertEquals(entryName, result.getEntryPath());
	}

	@Test
	void equalAesEncryptionInfoHaveSameHashCode() throws Exception {
		AesEncryptionInfo first = new AesEncryptionInfo(AesVendor.AE_2, 3);
		AesEncryptionInfo second = new AesEncryptionInfo(AesVendor.AE_2, 3);

		Set<AesEncryptionInfo> values = new HashSet<>();
		values.add(first);

		assertTrue(values.contains(second));
	}

	@Test
	void rejectsLocalZip64ExtraWithoutSizePlaceholders() {
		String entryName = "redundant-zip64-extra.bin";
		int flags = 0x0800;
		long compressedSize = 128L;
		long uncompressedSize = 256L;
		long crc32 = 0x12345678L;
		byte[] zip64Extra = extraRecord(ZipGlobals.ZIP64_EXTRA_HEADER_ID, zip64Data(uncompressedSize, compressedSize));
		byte[] localHeader =
				localHeader(ZipGlobals.ZIP_64_VERSION, flags, ZipGlobals.COMP_STORE, 0, 0,
						crc32, compressedSize, uncompressedSize, entryName, zip64Extra);
		GeneralFileHeader generalHeader =
				generalHeader(entryName, ZipGlobals.ZIP_64_VERSION, flags, ZipGlobals.COMP_STORE,
						0, 0, crc32, compressedSize, uncompressedSize);
		assertThrows(ArchiveException.class, () -> readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE));
	}

	@Test
	void rejectsLocalZip64ExtraWithOnlyOneSizePlaceholder() {
		String entryName = "partial-zip64-placeholder.bin";
		int flags = 0x0800;

		long compressedSize = ZipGlobals.ZIP_64_LIMIT + 1L;
		long uncompressedSize = 256L;
		long crc32 = 0x87654321L;

		byte[] zip64Extra = extraRecord(ZipGlobals.ZIP64_EXTRA_HEADER_ID, zip64Data(uncompressedSize, compressedSize));

		byte[] localHeader =
				localHeader(ZipGlobals.ZIP_64_VERSION, flags, ZipGlobals.COMP_STORE, 0, 0,
						crc32, ZipGlobals.ZIP_64_LIMIT, uncompressedSize, entryName, zip64Extra);

		GeneralFileHeader generalHeader =
				generalHeader(entryName, ZipGlobals.ZIP_64_VERSION, flags, ZipGlobals.COMP_STORE,
						0, 0, crc32, compressedSize, uncompressedSize);

		assertThrows(ArchiveException.class, () -> readLocalHeader(localHeader, generalHeader, Long.MAX_VALUE));
	}

	private LocalFileHeader readLocalHeader(final byte[] localHeaderData, final GeneralFileHeader generalFileHeader,
	                                        final long maxHeaderBytes)
			throws Exception {
		Path archivePath = Files.createTempFile(this.tempDirectory, "local-header-", ".zip");
		Files.write(archivePath, localHeaderData);
		try (ZipFileOperator operator =
				     new ZipFileOperator(new LocalDataFile(archivePath.toString(), "r"), ArchiveOptions.newBuilder().build(),
						     ZipOptions.newBuilder().build())) {
			return operator.readLocalFileHeader(generalFileHeader, StandardCharsets.UTF_8, maxHeaderBytes);
		}
	}

	private static GeneralFileHeader generalHeader(final String entryName, final int extractNeeded, final int flags,
	                                               final int compressionMethod, final int modifiedTime,
	                                               final int modifiedDate, final long crc32,
	                                               final long compressedSize, final long uncompressedSize) {
		GeneralFileHeader fileHeader = new GeneralFileHeader();

		fileHeader.setExtractNeeded(extractNeeded);
		fileHeader.setGeneralPurpose(flags);
		fileHeader.setCompressionMethod(compressionMethod);
		fileHeader.setLastModifiedTime(modifiedTime);
		fileHeader.setLastModifiedDate(modifiedDate);
		fileHeader.setCrc32(crc32);
		fileHeader.setCompressedSize(compressedSize);
		fileHeader.setUncompressedSize(uncompressedSize);
		fileHeader.setEntryPath(entryName);
		fileHeader.setNameLength(entryName.getBytes(StandardCharsets.UTF_8).length);
		fileHeader.setExtraFieldLength(0);
		fileHeader.setDiskNumberStart(0L);
		fileHeader.setOffsetLocalHeader(0L);
		fileHeader.setExtraDataRecords(Collections.emptyList());

		return fileHeader;
	}

	private static byte[] localHeader(final int extractNeeded, final int flags, final int compressionMethod,
	                                  final int modifiedTime, final int modifiedDate, final long crc32,
	                                  final long compressedSize, final long uncompressedSize,
	                                  final String entryName, final byte[] extraField) {
		byte[] nameBytes = entryName.getBytes(StandardCharsets.UTF_8);

		ByteBuffer buffer =
				ByteBuffer.allocate(30 + nameBytes.length + extraField.length).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt((int) ZipGlobals.LOCAL_FILE_HEADER_SIGNATURE);
		buffer.putShort((short) extractNeeded);
		buffer.putShort((short) flags);
		buffer.putShort((short) compressionMethod);
		buffer.putShort((short) modifiedTime);
		buffer.putShort((short) modifiedDate);
		buffer.putInt((int) crc32);
		buffer.putInt((int) compressedSize);
		buffer.putInt((int) uncompressedSize);
		buffer.putShort((short) nameBytes.length);
		buffer.putShort((short) extraField.length);
		buffer.put(nameBytes);
		buffer.put(extraField);

		return buffer.array();
	}

	private static byte[] extraRecord(final int headerId, final byte[] data) {
		return ByteBuffer.allocate(Short.BYTES * 2 + data.length)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) headerId)
				.putShort((short) data.length)
				.put(data)
				.array();
	}

	private static byte[] zip64Data(final long uncompressedSize, final long compressedSize) {
		return ByteBuffer.allocate(Long.BYTES * 2)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putLong(uncompressedSize)
				.putLong(compressedSize)
				.array();
	}

	private static byte[] aesData(final int version, final int strength, final int actualCompressionMethod) {
		return ByteBuffer.allocate(7)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) version)
				.put((byte) 'A')
				.put((byte) 'E')
				.put((byte) strength)
				.putShort((short) actualCompressionMethod)
				.array();
	}
}