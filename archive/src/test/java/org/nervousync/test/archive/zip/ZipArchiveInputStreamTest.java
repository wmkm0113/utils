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
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.io.ZipArchiveInputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ZipArchiveInputStreamTest {

	@TempDir
	Path tempDirectory;

	@Test
	void readsOrdinaryArchiveFromBeginning() throws Exception {
		Path archive = this.tempDirectory.resolve("ordinary.zip");
		byte[] expected = {0x11, 0x22, 0x33, 0x44};
		Files.write(archive, expected);

		try (ZipArchiveInputStream inputStream =
				     new ZipArchiveInputStream(new LocalDataFile(archive.toString(), "r"),
						     ZipOptions.newBuilder().splitArchive(false, -1L).build())) {

			assertArrayEquals(expected, inputStream.readNBytes(expected.length));
			assertEquals(-1, inputStream.read());
		}
	}

	@Test
	void readsAcrossSplitFileBoundary() throws Exception {
		Path firstPart = this.tempDirectory.resolve("sample.z01");
		Path finalPart = this.tempDirectory.resolve("sample.zip");

		Files.write(firstPart, new byte[]{0x01, 0x02});
		Files.write(finalPart, new byte[]{0x03, 0x04});

		try (ZipArchiveInputStream inputStream =
				     new ZipArchiveInputStream(new LocalDataFile(finalPart.toString(), "r"),
						     ZipOptions.newBuilder().splitArchive(true, 2L).build())) {

			assertEquals(4L, inputStream.length());
			assertEquals(1L, inputStream.lastDiskNumber());

			assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, inputStream.readNBytes(4));

			assertEquals(-1, inputStream.read());
		}
	}

	@Test
	void seeksToSpecificSplitPart() throws Exception {
		Path firstPart = this.tempDirectory.resolve("seek.z01");
		Path finalPart = this.tempDirectory.resolve("seek.zip");

		Files.write(firstPart, new byte[]{0x10, 0x11});
		Files.write(finalPart, new byte[]{0x20, 0x21});

		try (ZipArchiveInputStream inputStream =
				     new ZipArchiveInputStream(new LocalDataFile(finalPart.toString(), "r"),
						     ZipOptions.newBuilder().splitArchive(true, 2L).build())) {

			inputStream.seek(1L, 1L);
			assertEquals(0x21, inputStream.read());

			inputStream.seek(0L, 0L);
			assertEquals(0x10, inputStream.read());
		}
	}

	@Test
	void operationsAfterCloseThrowIOException() throws Exception {
		Path archive = this.tempDirectory.resolve("closed.zip");
		Files.write(archive, new byte[]{0x01, 0x02});

		ZipArchiveInputStream inputStream = new ZipArchiveInputStream(new LocalDataFile(archive.toString(), "r"),
				ZipOptions.newBuilder().splitArchive(false, -1L).build());

		inputStream.close();

		assertThrows(IOException.class, inputStream::read);
		assertThrows(IOException.class, () -> inputStream.read(new byte[1]));
		assertThrows(IOException.class, () -> inputStream.skip(1L));
		assertThrows(IOException.class, () -> inputStream.seek(0L, 0L));
		assertThrows(IOException.class, () -> inputStream.readLastBytes(1));

		// 重复关闭必须保持幂等。
		assertDoesNotThrow(inputStream::close);
	}
}