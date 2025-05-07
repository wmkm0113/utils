package org.nervousync.test.zip;

import org.junit.jupiter.api.*;
import org.nervousync.commons.Globals;
import org.nervousync.zip.options.ZipOptions;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.FileUtils;
import org.nervousync.utils.StringUtils;
import org.nervousync.zip.ZipFile;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Random;

public final class ZipTest extends BaseTest {

	private static final String BASE_PATH;
	private static String COMPRESS_FOLDER;
	private static String EXTRACT_FOLDER;
	private static String ZIP_FILE_PATH;
	private static final String ZIP_PASSWORD = "Nervousync";

	static {
		String tmpDir = System.getProperty("java.io.tmpdir");
		BASE_PATH = tmpDir.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)
				? tmpDir.substring(0, tmpDir.length() - 1)
				: tmpDir;
	}

	@BeforeAll
	public static void createTmpFiles() {
		COMPRESS_FOLDER = BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip";
		FileUtils.makeDir(COMPRESS_FOLDER);
		EXTRACT_FOLDER = BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestUnzip";
		FileUtils.makeDir(EXTRACT_FOLDER);
		ZIP_FILE_PATH = BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "ZipFiles";
		FileUtils.makeDir(ZIP_FILE_PATH);

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

		boolean copyResult = FileUtils.copy("src/test/resources/TestZip.zip.001",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.001");
		copyResult &= FileUtils.copy("src/test/resources/TestZip.zip.002",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.002");
		copyResult &= FileUtils.copy("src/test/resources/TestZip.zip.003",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.003");
		copyResult &= FileUtils.copy("src/test/resources/TestZip.zip.004",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.004");
		copyResult &= FileUtils.copy("src/test/resources/TestZip.zip.005",
				ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.005");
		System.out.println("Copy test split zip file result: " + copyResult);
	}

	@AfterAll
	public static void clearTmpFiles() {
		boolean removeResult = FileUtils.removeDir(COMPRESS_FOLDER);
		removeResult &= FileUtils.removeDir(EXTRACT_FOLDER);
		removeResult &= FileUtils.removeDir(ZIP_FILE_PATH);
		System.out.println("Remove temp file result: " + removeResult);
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
		ZipOptions zipOptions = ZipOptions.newOptions();
		//  Setting character encoding "GBK" if comment content contains CJK character for compatible the compress software (ex: 7-Zip)
		zipOptions.setCharsetEncoding("GBK");
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
		String filePath = ZIP_FILE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.001";
		this.logger.info("Zip_Read_Path", filePath);
		if (FileUtils.isExists(filePath)) {
			ZipFile zipFile = ZipFile.openZipFile(filePath);
			Assertions.assertNotNull(zipFile);
			zipFile.extractAll(EXTRACT_FOLDER + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip");
		}
	}
}
