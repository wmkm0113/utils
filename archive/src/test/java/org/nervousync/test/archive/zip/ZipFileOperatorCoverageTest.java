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
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.central.CentralDirectory;
import org.nervousync.archive.zip.models.central.EndCentralDirectoryRecord;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.operator.ZipFileOperator;
import org.nervousync.utils.commons.RawUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ZipFileOperatorCoverageTest {

	private static final long OUT_OF_LIMIT_ERROR_CODE = 0x0000001A003EL;

	@TempDir
	Path tempDirectory;

	/**
	 * 中央目录实际大小超过 maxHeaderBytes 时必须在解析前拒绝。
	 */
	@Test
	void rejectsPlainCentralDirectoryLargerThanHeaderLimit() {
		byte[] centralDirectory = centralDirectoryEntry("entry.txt");
		long maxHeaderBytes = centralDirectory.length - 1L;
		ArchiveOptions options = optionsWithMaxHeaderBytes(maxHeaderBytes);
		byte[] archive = plainArchive(centralDirectory, 1, centralDirectory.length);
		ArchiveException exception = assertThrows(ArchiveException.class, () -> readArchive(archive, options));
		assertEquals(OUT_OF_LIMIT_ERROR_CODE, exception.getErrorCode());
	}

	/**
	 * maxHeaderBytes 等于中央目录实际大小时应该允许读取。
	 * 该测试确保生产代码使用 >，而不是 >=。
	 */
	@Test
	void acceptsPlainCentralDirectoryAtHeaderLimit() throws Exception {
		byte[] centralDirectory = centralDirectoryEntry("entry.txt");
		ArchiveOptions options = optionsWithMaxHeaderBytes(centralDirectory.length);
		byte[] archive = plainArchive(centralDirectory, 1, centralDirectory.length);
		CentralDirectory result = readArchive(archive, options);
		assertNotNull(result);
		assertEquals(1, result.getFileHeaders().size());
		assertEquals("entry.txt", result.getFileHeaders().get(0).getEntryPath());
	}

	/**
	 * Archive Extra Data 和 Digital Signature 也属于中央目录大小限制。
	 */
	@Test
	void countsOptionalCentralDirectoryStructuresInHeaderLimit() {
		byte[] archiveExtraData = {0x11, 0x22, 0x33, 0x44};
		byte[] signatureData = {0x55, 0x66, 0x77};

		byte[] entry = centralDirectoryEntry("entry.txt");
		byte[] extraRecord = archiveExtraRecord(archiveExtraData);
		byte[] signature = digitalSignature(signatureData);

		byte[] centralDirectory = RawUtils.join(RawUtils.join(extraRecord, entry), signature);

		/*
		 * 限制只够容纳 entry，不够容纳完整中央目录。
		 * 如果实现错误地只累计 File Header，这个测试会意外通过。
		 */
		ArchiveOptions options = optionsWithMaxHeaderBytes(entry.length);
		byte[] archive = plainArchive(centralDirectory, 1, centralDirectory.length);
		ArchiveException exception = assertThrows(ArchiveException.class, () -> readArchive(archive, options));
		assertEquals(OUT_OF_LIMIT_ERROR_CODE, exception.getErrorCode());
	}

	@Test
	void readsArchiveExtraDataRecord() throws Exception {
		byte[] archiveExtraData = {0x11, 0x22, 0x33, 0x44};

		byte[] centralDirectory =
				RawUtils.join(archiveExtraRecord(archiveExtraData), centralDirectoryEntry("entry.txt"));

		CentralDirectory result = readArchive(plainArchive(centralDirectory, 1, centralDirectory.length));

		assertArrayEquals(archiveExtraData, result.getArchiveExtraData());
		assertEquals("entry.txt", result.getFileHeaders().get(0).getEntryPath());
	}

	@Test
	void readsUtf8EntryNameIndependentlyOfConfiguredCharset()
			throws Exception {
		String entryName = "目录/测试文件.txt";
		byte[] centralDirectory = centralDirectoryEntry(entryName);

		ArchiveOptions options = ArchiveOptions.newBuilder().charset(StandardCharsets.ISO_8859_1).build();

		CentralDirectory result =
				readArchive(plainArchive(centralDirectory, 1, centralDirectory.length), options);

		assertEquals(entryName, result.getFileHeaders().get(0).getEntryPath());
	}

	@Test
	void readsInlineDigitalSignatureFromPlainCentralDirectory() throws Exception {
		byte[] signatureData = {0x01, 0x23, 0x45, 0x67};

		byte[] centralDirectory =
				RawUtils.join(centralDirectoryEntry("entry.txt"), digitalSignature(signatureData));

		CentralDirectory result = readArchive(plainArchive(centralDirectory, 1, centralDirectory.length));

		assertArrayEquals(signatureData, result.getSignatureData());
	}

	@Test
	void rejectsInlineDigitalSignatureWithIncorrectLength() {
		byte[] signatureData = {0x01, 0x23, 0x45, 0x67};

		byte[] digitalSignature = digitalSignature(signatureData);

		// 声明长度比实际数据多一个字节。
		ByteBuffer.wrap(digitalSignature)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort(Integer.BYTES, (short) (signatureData.length + 1));

		byte[] centralDirectory = RawUtils.join(centralDirectoryEntry("entry.txt"), digitalSignature);

		byte[] archive = plainArchive(centralDirectory, 1, centralDirectory.length);

		assertThrows(ArchiveException.class, () -> readArchive(archive));
	}

	@Test
	void rejectsCentralDirectorySizeLargerThanAvailableData() {
		byte[] centralDirectory = centralDirectoryEntry("entry.txt");
		// EOCD 声明中央目录比实际数据多一个字节。
		byte[] archive = plainArchive(centralDirectory, 1, centralDirectory.length + 1);
		assertThrows(ArchiveException.class, () -> readArchive(archive));
	}

	@Test
	void returnsEmptyOptionalFieldsForOrdinaryZip() throws Exception {
		byte[] centralDirectory = centralDirectoryEntry("entry.txt");

		CentralDirectory result = readArchive(plainArchive(centralDirectory, 1, centralDirectory.length));

		assertArrayEquals(new byte[0], result.getArchiveExtraData());
		assertArrayEquals(new byte[0], result.getSignatureData());
	}

	private CentralDirectory readArchive(final byte[] archiveData) throws Exception {
		return readArchive(archiveData, ArchiveOptions.newBuilder().build());
	}

	private CentralDirectory readArchive(final byte[] archiveData, final ArchiveOptions options) throws Exception {

		Path archivePath = Files.createTempFile(this.tempDirectory, "plain-central-directory-", ".zip");
		Files.write(archivePath, archiveData);

		try (ZipFileOperator operator =
				     new ZipFileOperator(new LocalDataFile(archivePath.toString(), "r"), ArchiveOptions.newBuilder(options).strict(true).build(),
						     ZipOptions.newBuilder().build())) {
			EndCentralDirectoryRecord endRecord = operator.readEndCentralDirectoryRecord();
			Zip64EndCentralDirectory zip64Record = operator.readZip64EndCentralDirectory(endRecord);
			assertNull(zip64Record);
			return operator.readCentralDirectory(endRecord, null);
		}
	}

	private static byte[] plainArchive(final byte[] centralDirectory, final int entryCount,
	                                   final int declaredCentralDirectorySize) {
		ByteBuffer buffer = ByteBuffer.allocate(centralDirectory.length + 22).order(ByteOrder.LITTLE_ENDIAN);

		buffer.put(centralDirectory);

		buffer.putInt((int) ZipGlobals.END_OF_CENTRAL_DIRECTORY_SIGNATURE);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putShort((short) entryCount);
		buffer.putShort((short) entryCount);
		buffer.putInt(declaredCentralDirectorySize);
		buffer.putInt(0);
		buffer.putShort((short) 0);

		return buffer.array();
	}

	private static byte[] centralDirectoryEntry(
			final String entryName) {

		byte[] nameBytes =
				entryName.getBytes(StandardCharsets.UTF_8);

		ByteBuffer buffer = ByteBuffer
				.allocate(46 + nameBytes.length)
				.order(ByteOrder.LITTLE_ENDIAN);

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
		buffer.putShort((short) nameBytes.length);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.put(nameBytes);

		return buffer.array();
	}

	private static byte[] archiveExtraRecord(final byte[] data) {
		ByteBuffer buffer = ByteBuffer
				.allocate(8 + data.length)
				.order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt((int) ZipGlobals.EXTRA_DATA_RECORD_SIGNATURE);
		buffer.putInt(data.length);
		buffer.put(data);
		return buffer.array();
	}

	private static byte[] digitalSignature(final byte[] data) {
		ByteBuffer buffer = ByteBuffer
				.allocate(6 + data.length)
				.order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt((int) ZipGlobals.DIGITAL_SIGNATURE);
		buffer.putShort((short) data.length);
		buffer.put(data);
		return buffer.array();
	}

	private static ArchiveOptions optionsWithMaxHeaderBytes(final long maxHeaderBytes) {
		return ArchiveOptions.newBuilder()
				.limitBuilder()
				.maxHeaderBytes(maxHeaderBytes)
				.confirm()
				.build();
	}
}