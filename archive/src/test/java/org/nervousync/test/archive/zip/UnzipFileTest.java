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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.FileUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnzipFileTest {

	@TempDir
	static Path tempDirectory;

	@BeforeAll
	public static void createTmpFiles() throws IOException {
		Files.createDirectories(Path.of(tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip"));
		Files.createDirectories(Path.of(tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "TestUnzip"));
		Files.createDirectories(Path.of(tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "ZipFiles"));
		Files.createDirectories(Path.of(tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "LargeFiles"));

		FileUtils.copy("src/test/resources/TestZip.zip.001",
				tempDirectory.toString() + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.001");
		FileUtils.copy("src/test/resources/TestZip.zip.002",
				tempDirectory.toString() + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.002");
		FileUtils.copy("src/test/resources/TestZip.zip.003",
				tempDirectory.toString() + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.003");
		FileUtils.copy("src/test/resources/TestZip.zip.004",
				tempDirectory.toString() + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.004");
		FileUtils.copy("src/test/resources/TestZip.zip.005",
				tempDirectory.toString() + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.005");
	}

	@Test
	void unzip7zSplitFiles() {
		String folderPath = tempDirectory + Globals.DEFAULT_PAGE_SEPARATOR + "TestUnzip";
		String filePath = tempDirectory.toString() + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip.zip.001";
		try (ArchiveFile archiveFile = ArchiveFile.zipFile(new LocalDataFile(filePath, "r"),
				ArchiveOptions.newBuilder().charset(Charset.forName("GBK")).build(),
				ZipOptions.newBuilder().splitArchive(true, 1048576L).build())) {
			archiveFile.extract(Path.of(folderPath), ExtractOptions.newBuilder().build());
		} catch (Exception ignore) {
		}
		assertEquals(1965961L,
				FileUtils.fileSize(folderPath + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip" + Globals.DEFAULT_PAGE_SEPARATOR + "4YYW288A"));
		assertEquals(684330L,
				FileUtils.fileSize(folderPath + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip" + Globals.DEFAULT_PAGE_SEPARATOR + "7H68LKCC"));
		assertEquals(1394244L,
				FileUtils.fileSize(folderPath + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip" + Globals.DEFAULT_PAGE_SEPARATOR + "724MVWNH"));
		assertEquals(250896L,
				FileUtils.fileSize(folderPath + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip" + Globals.DEFAULT_PAGE_SEPARATOR + "JEP9VSQR"));
		assertEquals(346538L,
				FileUtils.fileSize(folderPath + Globals.DEFAULT_PAGE_SEPARATOR + "TestZip" + Globals.DEFAULT_PAGE_SEPARATOR + "中文路径"));
	}
}
