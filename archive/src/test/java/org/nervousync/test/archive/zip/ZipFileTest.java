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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.zip.AesVendor;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.commons.Globals;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.commons.FileUtils;
import org.nervousync.utils.commons.IOUtils;
import org.nervousync.utils.commons.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ZipFileTest extends BaseTest {

	public enum Layout { SINGLE, NATIVE, NUMBERED }

	@TempDir
	static Path tempDirectory;

	@BeforeAll
	public static void createTmpFiles() throws IOException {
		String compressFolder = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip";
		Files.createDirectories(Path.of(compressFolder));

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
				randFilePath = compressFolder + Globals.DEFAULT_PAGE_SEPARATOR + "中文路径";
			} else {
				randFilePath = compressFolder + Globals.DEFAULT_PAGE_SEPARATOR + StringUtils.randomString(8);
			}
			FileUtils.saveFile(byteArrayOutputStream.toByteArray(), randFilePath);
		}
	}

	private static Stream<Arguments> strongAlgorithms() {
		return Stream.of(
				Arguments.of("DES-64", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES, 64),
				Arguments.of("3DES-112", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112, 112),
				Arguments.of("3DES-168", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168, 168),
				Arguments.of("AES-128", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128, 128),
				Arguments.of("AES-192", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192, 192),
				Arguments.of("AES-256", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256),
				Arguments.of("RC2-40", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW, 40),
				Arguments.of("RC2-64", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW, 64),
				Arguments.of("RC2-128", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW, 128),
				Arguments.of("RC4-128", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4, 128),
				Arguments.of("Blowfish-128", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH, 128),
				Arguments.of("Twofish-256", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH, 256));
	}

	private static Stream<Arguments> archiveCases() {
		return strongAlgorithms().flatMap(arguments -> {
			Object[] algorithm = arguments.get();
			return Stream.of(Layout.values())
					.map(layout -> Arguments.of(algorithm[0], algorithm[1], algorithm[2], layout));
		});
	}

	@Order(0)
	@ParameterizedTest(name = "Strong {0}, {3}")
	@MethodSource("archiveCases")
	public void createStrongFileFromListAndReadBack(final String name, final int algorithm,
	                                                final int keyBitLength, final Layout layout) throws Exception {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "strong-" + name + "-" + layout + ".zip";
		ArchiveOptions archiveOptions = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("Nervousync".toCharArray()))
				.build();
		ZipOptions.Builder optionsBuilder = ZipOptions.newBuilder()
				.enableCentralDirectoryCompress()
				.strongCrypto(algorithm, keyBitLength);
		switch (layout) {
			case NATIVE:
				optionsBuilder.splitArchive(true, 1024 * 1024L).splitType(ZipOptions.SplitType.NATIVE);
				break;
			case NUMBERED:
				optionsBuilder.splitArchive(true, 1024 * 1024L).splitType(ZipOptions.SplitType.NUMBERED);
				break;
			case SINGLE:
				optionsBuilder.splitArchive(false, Globals.DEFAULT_VALUE_LONG);
				optionsBuilder.splitType(ZipOptions.SplitType.DETECT);
				break;
		}
		ZipOptions zipOptions = optionsBuilder.build();
		this.createFile(filePath, archiveOptions, zipOptions);
		if (layout == Layout.NUMBERED) {
			this.validateFile(filePath + ".001", archiveOptions, zipOptions);
		} else {
			this.validateFile(filePath, archiveOptions, zipOptions);
		}
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(0)
	public void createZipFromList() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoFile.zip";
		ZipOptions zipOptions = ZipOptions.newBuilder().build();
		this.createFile(filePath, ArchiveOptions.newBuilder().build(), zipOptions);
		this.validateFile(filePath, ArchiveOptions.newBuilder().build(), zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(5)
	public void createEncFile() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoEnc.zip";
		ArchiveOptions archiveOptions = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("Nervousync".toCharArray()))
				.build();
		ZipOptions zipOptions = ZipOptions.newBuilder().standardCrypto().build();
		this.createFile(filePath, archiveOptions, zipOptions);
		this.validateFile(filePath, archiveOptions, zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(10)
	public void createAes128File() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes128.zip";
		ArchiveOptions archiveOptions = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("Nervousync".toCharArray()))
				.build();
		ZipOptions zipOptions = ZipOptions.newBuilder().aesCrypto(128).build();
		this.createFile(filePath, archiveOptions, zipOptions);
		this.validateFile(filePath, archiveOptions, zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(15)
	public void createAes192File() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes192.zip";
		ArchiveOptions archiveOptions = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("Nervousync".toCharArray()))
				.build();
		ZipOptions zipOptions = ZipOptions.newBuilder().aesCrypto(192).build();
		this.createFile(filePath, archiveOptions, zipOptions);
		this.validateFile(filePath, archiveOptions, zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(20)
	public void createAes256File() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes256.zip";
		ArchiveOptions archiveOptions = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("Nervousync".toCharArray()))
				.build();
		ZipOptions zipOptions = ZipOptions.newBuilder().aesCrypto(256).build();
		this.createFile(filePath, archiveOptions, zipOptions);
		this.validateFile(filePath, archiveOptions, zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(25)
	public void createAes1_128File() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes1_128.zip";
		ArchiveOptions archiveOptions = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("Nervousync".toCharArray()))
				.build();
		ZipOptions zipOptions = ZipOptions.newBuilder().aesCrypto(128, AesVendor.AE_1).build();
		this.createFile(filePath, archiveOptions, zipOptions);
		this.validateFile(filePath, archiveOptions, zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(30)
	public void createAes1_192File() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes1_192.zip";
		ArchiveOptions archiveOptions = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("Nervousync".toCharArray()))
				.build();
		ZipOptions zipOptions = ZipOptions.newBuilder().aesCrypto(192, AesVendor.AE_1).build();
		this.createFile(filePath, archiveOptions, zipOptions);
		this.validateFile(filePath, archiveOptions, zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(35)
	public void createAes1_256File() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoAes1_256.zip";
		ArchiveOptions archiveOptions = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("Nervousync".toCharArray()))
				.build();
		ZipOptions zipOptions = ZipOptions.newBuilder().aesCrypto(256, AesVendor.AE_1).build();
		this.createFile(filePath, archiveOptions, zipOptions);
		this.validateFile(filePath, archiveOptions, zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(40)
	public void createNativeSplitFile() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demoNativeSplit.zip";
		ZipOptions zipOptions = ZipOptions.newBuilder().splitArchive(true, 1024 * 1024L).splitType(ZipOptions.SplitType.NATIVE).build();
		this.createFile(filePath, ArchiveOptions.newBuilder().build(), zipOptions);
		this.validateFile(filePath, ArchiveOptions.newBuilder().build(), zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	@Test
	@Order(45)
	public void create7zSplitFile() throws IOException {
		String filePath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "demo7zSplit.zip";
		ZipOptions zipOptions = ZipOptions.newBuilder().splitArchive(true, 1024 * 1024L).splitType(ZipOptions.SplitType.NUMBERED).build();
		this.createFile(filePath, ArchiveOptions.newBuilder().build(), zipOptions);
		this.validateFile(filePath + ".001", ArchiveOptions.newBuilder().build(), zipOptions);
		this.logger.info("Zip_Create_Success");
	}

	private void createFile(final String filePath, final ArchiveOptions archiveOptions, final ZipOptions zipOptions)
			throws IOException {
		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(filePath, "rw"), archiveOptions, zipOptions)) {
			archiveFile.addDirectory("", tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip")
					.addDirectory("TestZip", tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip");
		} catch (Exception e) {
			throw (e instanceof IOException) ? (IOException) e : new IOException(e);
		}
	}

	private void validateFile(final String filePath, final ArchiveOptions archiveOptions, final ZipOptions zipOptions)
			throws IOException {
		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(filePath, "r"), archiveOptions, zipOptions)) {
			String folderPath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip";
			for (String itemPath : FileUtils.listFiles(folderPath)) {
				String entryPath =
						StringUtils.replace(itemPath.substring(folderPath.length() + 1),
								Globals.DEFAULT_PAGE_SEPARATOR, ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR);
				ArchiveEntry archiveEntry = archiveFile.entry(entryPath).orElseThrow();
				try (ResourceReader reader = archiveFile.openReader(archiveEntry).orElseThrow();
				     InputStream entryStream = reader.openStream();
					 InputStream originalStream = FileUtils.loadFile(itemPath)) {
					byte[] readBytes = IOUtils.readBytes(entryStream), originalBytes = IOUtils.readBytes(originalStream);
					assertArrayEquals(readBytes, originalBytes);
				}
				ArchiveEntry folderEntry = archiveFile.entry("TestZip" + ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR + entryPath).orElseThrow();
				try (ResourceReader reader = archiveFile.openReader(folderEntry).orElseThrow();
				     InputStream entryStream = reader.openStream();
				     InputStream originalStream = FileUtils.loadFile(itemPath)) {
					byte[] readBytes = IOUtils.readBytes(entryStream), originalBytes = IOUtils.readBytes(originalStream);
					assertArrayEquals(readBytes, originalBytes);
				}
			}
		} catch (Exception e) {
			throw (e instanceof IOException) ? (IOException) e : new IOException(e);
		}
		if (zipOptions.getEncryptionMethod() != ZipGlobals.ENC_NO_ENCRYPTION
				|| zipOptions.getCentralDirectoryEncryptionAlgorithm() != ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE) {
			assertThrows(IOException.class, () -> {
				ArchiveOptions incorrectOptions = ArchiveOptions.newBuilder()
						.passwordProvider(PasswordProvider.fixed("Incorrect-password".toCharArray()))
						.build();
				try (ArchiveFile archiveFile =
						     ArchiveFile.zipFile(new LocalDataFile(filePath, "r"), incorrectOptions, zipOptions)) {
					String folderPath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip";
					for (String itemPath : FileUtils.listFiles(folderPath)) {
						String entryPath =
								StringUtils.replace(itemPath.substring(folderPath.length() + 1),
										Globals.DEFAULT_PAGE_SEPARATOR, ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR);
						ArchiveEntry archiveEntry = archiveFile.entry(entryPath).orElseThrow();
						try (ResourceReader reader = archiveFile.openReader(archiveEntry).orElseThrow();
						     InputStream entryStream = reader.openStream()) {
							IOUtils.readBytes(entryStream);
						}
					}
				}
			});
		}
	}

}
