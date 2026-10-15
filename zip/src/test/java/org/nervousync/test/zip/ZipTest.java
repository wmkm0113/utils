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

package org.nervousync.test.zip;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.zip.CompressionLevel;
import org.nervousync.enumerations.zip.CompressionType;
import org.nervousync.enumerations.zip.Zip64Mode;
import org.nervousync.utils.core.SystemUtils;
import org.nervousync.zip.models.header.GeneralFileHeader;
import org.nervousync.zip.options.ZipOptions;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.zip.ZipFile;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public final class ZipTest extends BaseTest {

	private static final long LARGE_FILE_SIZE = (1L << 32) + (64L * 1024L);

	private static final byte[] HEAD_MARKER = {0x4E, 0x45, 0x52, 0x56, 0x4F, 0x55, 0x53};

	private static final byte[] TAIL_MARKER = {0x5A, 0x49, 0x50, 0x36, 0x34, 0x45, 0x4E, 0x44};

	private static final String BASE_PATH;
	private static String COMPRESS_FOLDER;
	private static String EXTRACT_FOLDER;
	private static String ZIP_FILE_PATH;
	private static String LARGE_FILE_PATH;
	private static final String ZIP_PASSWORD = "Nervousync";
	private static boolean COPY_RESULT = Boolean.FALSE;

	static {
		String tmpDir = SystemUtils.JAVA_TMP_DIR;
		BASE_PATH = tmpDir.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)
				? tmpDir.substring(0, tmpDir.length() - 1)
				: tmpDir;
		//  Uncomment for testing large zip file
//		System.setProperty("runLargeZipTest", "true");
	}

	@BeforeAll
	public static void createTmpFiles() {
		COMPRESS_FOLDER = BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip";
		FileUtils.makeDir(COMPRESS_FOLDER);
		EXTRACT_FOLDER = BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestUnzip";
		FileUtils.makeDir(EXTRACT_FOLDER);
		ZIP_FILE_PATH = BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "ZipFiles";
		FileUtils.makeDir(ZIP_FILE_PATH);
		LARGE_FILE_PATH = BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "LargeFiles";
		FileUtils.makeDir(LARGE_FILE_PATH);

		Random random = new Random();
		for (int i = 0 ; i < 5 ; i++) {
			int fileSize = random.nextInt(1048576);
			if (i % 2 == 1) {
				fileSize += 1048576;
			}

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			for (int j = 0 ; j < fileSize ; j++) {
				byteArrayOutputStream.write(random.nextInt(256));
			}
			String randFilePath;
			if (i == 0) {
				randFilePath = COMPRESS_FOLDER + Globals.DEFAULT_PAGE_SEPARATOR + "中文路径";
			} else {
				randFilePath = COMPRESS_FOLDER + Globals.DEFAULT_PAGE_SEPARATOR + StringUtils.randomString(8);
			}
			FileUtils.saveFile(byteArrayOutputStream.toByteArray(), randFilePath);
		}

		COPY_RESULT = FileUtils.copy("src/test/resources/TestZip.zip.001",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.001");
		COPY_RESULT &= FileUtils.copy("src/test/resources/TestZip.zip.002",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.002");
		COPY_RESULT &= FileUtils.copy("src/test/resources/TestZip.zip.003",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.003");
		COPY_RESULT &= FileUtils.copy("src/test/resources/TestZip.zip.004",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.004");
		COPY_RESULT &= FileUtils.copy("src/test/resources/TestZip.zip.005",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.005");
	}

	@AfterAll
	public static void clearTmpFiles() {
		FileUtils.removeDir(COMPRESS_FOLDER);
		FileUtils.removeDir(EXTRACT_FOLDER);
		FileUtils.removeDir(ZIP_FILE_PATH);
		FileUtils.removeFile(LARGE_FILE_PATH);
	}

	@Test
	@Order(0)
	public void createZipFromList() throws ZipException, FileNotFoundException {
		Assertions.assertNotNull(ZipFile.createZipFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoFile.zip",
				ZipOptions.newOptions(), FileUtils.listFiles(COMPRESS_FOLDER).toArray(new String[0])));
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(5)
	public void createZipFromFolder() throws ZipException {
		Assertions.assertNotNull(ZipFile.createZipFileFromFolder(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoFolder.zip",
				ZipOptions.newOptions(), COMPRESS_FOLDER));
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(10)
	public void createEncFile() throws ZipException {
		Assertions.assertNotNull(ZipFile.createZipFileFromFolder(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoEnc.zip",
				ZipOptions.standardEncryptOptions(ZIP_PASSWORD), COMPRESS_FOLDER));
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(20)
	public void createAes256File() throws ZipException {
		Assertions.assertNotNull(ZipFile.createZipFileFromFolder(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes256.zip",
				ZipOptions.aesEncryptOptions(ZIP_PASSWORD), COMPRESS_FOLDER));
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(30)
	public void createSplitFile() throws ZipException {
		Assertions.assertNotNull(ZipFile.createZipFileFromFolder(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoSplit.zip",
				ZipOptions.newOptions(), Boolean.TRUE, 1024 * 1024L, COMPRESS_FOLDER));
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(40)
	public void createCommentFile() throws ZipException {
		//  Setting character encoding "GBK" if comment content contains CJK character for compatible the compress software (ex: 7-Zip)
		ZipOptions zipOptions = ZipOptions.newBuilder().characterEncoding("GBK").build();
		ZipFile zipFile = ZipFile.createZipFileFromFolder(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoComment.zip",
				zipOptions, Boolean.FALSE, Globals.DEFAULT_VALUE_LONG, COMPRESS_FOLDER);
		Assertions.assertNotNull(zipFile);
		zipFile.setComment("Comment 中文测试");
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(45)
	public void zipEntryTest() throws ZipException {
		ZipFile zipFile = ZipFile.openZipFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoFile.zip");
		Assertions.assertNotNull(zipFile);
		List<String> entryList = zipFile.entryList();
		for (int i = 0 ; i < entryList.size() ; i++) {
			String entryPath = entryList.get(i);
			this.logger.info("Zip_Entry_Path", entryPath);
			if (i % 2 == 0) {
				byte[] dataBytes = zipFile.readEntry(entryPath);
				this.logger.info("Zip_Entry_Size", dataBytes.length);
			}
		}
	}

	@Test
	@Order(50)
	public void extractFile() throws ZipException {
		ZipFile zipFile = ZipFile.openZipFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoFolder.zip");
		Assertions.assertNotNull(zipFile);
		zipFile.extractAll(EXTRACT_FOLDER + Globals.DEFAULT_PAGE_SEPARATOR + "demo");
	}

	@Test
	@Order(60)
	public void extractEncFile() throws ZipException {
		ZipFile zipFile = ZipFile.openZipFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoEnc.zip");
		Assertions.assertNotNull(zipFile);
		zipFile.setPassword(ZIP_PASSWORD);
		zipFile.extractAll(EXTRACT_FOLDER + Globals.DEFAULT_PAGE_SEPARATOR + "demoEnc");
	}

	@Test
	@Order(70)
	public void extractAes256File() throws ZipException {
		ZipFile zipFile = ZipFile.openZipFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes256.zip");
		Assertions.assertNotNull(zipFile);
		zipFile.setPassword(ZIP_PASSWORD);
		zipFile.extractAll(EXTRACT_FOLDER + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes256");
	}

	@Test
	@Order(80)
	public void extractSplitFile() throws ZipException {
		ZipFile zipFile = ZipFile.openZipFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoSplit.zip");
		Assertions.assertNotNull(zipFile);
		zipFile.extractAll(EXTRACT_FOLDER + Globals.DEFAULT_PAGE_SEPARATOR + "demoSplit");
	}

	@Test
	@Order(90)
	public void mergeSplitFile() throws ZipException {
		ZipFile zipFile = ZipFile.openZipFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoSplit.zip");
		Assertions.assertNotNull(zipFile);
		zipFile.mergeSplitFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoMerge.zip");
	}

	@Test
	@Order(100)
	public void readComment() throws ZipException {
		//  Using GBK Encoding if comment content contains CJK character
		ZipFile zipFile = ZipFile.openZipFile(ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "demoComment.zip", "GBK");
		Assertions.assertNotNull(zipFile);
		//  Or using GBK encoding to read comment content
		this.logger.info("Zip_Read_Comment", zipFile.getComment("GBK"));
	}

	/**
	 * Test for support the split zip file which created by 7-zip
	 */
	@Test
	@Order(110)
	public void extractSoftwareZippedFile() throws ZipException {
		if (COPY_RESULT) {
			String filePath = ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.001";
			this.logger.info("Zip_Read_Path", filePath);
			if (FileUtils.isExists(filePath)) {
				ZipFile zipFile = ZipFile.openZipFile(filePath);
				Assertions.assertNotNull(zipFile);
				zipFile.extractAll(EXTRACT_FOLDER + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip");
			}
		}
	}

	@Test
	@Order(120)
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	@EnabledIfSystemProperty(named = "runLargeZipTest", matches = "true")
	public void largeFile() throws Exception {
		String sourcePath = LARGE_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "large-file.bin";
		String zipPath = LARGE_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "large-file.zip";

		try (RandomAccessFile raf = new RandomAccessFile(sourcePath, "rw")) {
			raf.setLength(LARGE_FILE_SIZE);

			raf.seek(0L);
			raf.write(HEAD_MARKER);

			raf.seek(LARGE_FILE_SIZE - TAIL_MARKER.length);
			raf.write(TAIL_MARKER);
		}

		Assertions.assertEquals(LARGE_FILE_SIZE, FileUtils.fileSize(sourcePath));

		ZipOptions zipOptions =
				ZipOptions.newBuilder()
						.compressionType(CompressionType.DEFLATE)
						.compressionLevel(CompressionLevel.FAST)
						.readHiddenFiles(Boolean.TRUE)
						.includeRootFolder(Boolean.TRUE)
						.rootFolderInZip(Globals.DEFAULT_VALUE_STRING)
						.timezone(TimeZone.getDefault())
						.fileNameInZip(Globals.DEFAULT_VALUE_STRING)
						.zip64Mode(Zip64Mode.ALWAYS)
						.build();

		Assertions.assertNotNull(ZipFile.createZipFile(zipPath, zipOptions, sourcePath));

		ZipFile zipFile = ZipFile.openZipFile(zipPath);
		Assertions.assertNotNull(zipFile.getCentralDirectory());

		List<GeneralFileHeader> fileHeaders = zipFile.getCentralDirectory().getFileHeaders();
		Assertions.assertNotNull(fileHeaders);
		Assertions.assertEquals(1, fileHeaders.size());

		GeneralFileHeader fileHeader = fileHeaders.get(0);

		try (InputStream inputStream = zipFile.entryInputStream(fileHeader.getEntryPath())) {
			byte[] actualHead = inputStream.readNBytes(HEAD_MARKER.length);
			Assertions.assertArrayEquals(HEAD_MARKER, actualHead);

			byte[] buffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
			long length = LARGE_FILE_SIZE - HEAD_MARKER.length - TAIL_MARKER.length;
			while (length > 0L) {
				int expectedLength = (int) Math.min(buffer.length, length);
				int readLength = inputStream.read(buffer, 0, expectedLength);
				if (readLength < 0) {
					throw new EOFException("Unexpected end of ZIP entry, remaining: " + length);
				}
				length -= readLength;
			}

			byte[] actualTail = inputStream.readNBytes(TAIL_MARKER.length);

			Assertions.assertArrayEquals(TAIL_MARKER, actualTail);
			Assertions.assertEquals(-1, inputStream.read());
		}
	}
}
