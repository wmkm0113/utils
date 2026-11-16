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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.zip.ZipOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.assertContent;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.writeEntry;

final class ZipWriterValidationTest {

	@TempDir
	Path directory;

	@ParameterizedTest(name = "reject path [{0}]")
	@ValueSource(strings = {"../outside.txt", "folder/../../outside.txt", "/absolute.txt", "C:/absolute.txt",
			"C:relative.txt", "\\\\server\\share\\file.txt", "nul\0name", "", " "})
	void rejectsInvalidEntryPathBeforeRegisteringWriter(final String name) throws Exception {
		Path target = this.directory.resolve("invalid-name.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			try {
				assertThrows(IOException.class, () -> archive.openWriter(name, ""));
			} finally {
				archive.rollback();
			}
		}
	}

	@Test
	void normalizesWindowsEntrySeparatorsBeforeWriting() throws Exception {
		Path target = this.directory.resolve("normalized.zip");
		byte[] bytes = {1, 2, 3};
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			writeEntry(archive, "folder\\entry.txt", bytes);
		}
		assertContent(target, ZipOptions.newBuilder().build(), Map.of("folder/entry.txt", bytes));
	}

	@Test
	void closedArchiveRejectsWriters() throws Exception {
		Path target = this.directory.resolve("closed.zip");
		ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build());
		archive.close();
		assertThrows(IOException.class, () -> archive.openWriter("lost.txt", ""));
		assertDoesNotThrow(archive::close);
	}

	@Test
	void rolledBackArchiveRejectsWriters() throws Exception {
		Path target = this.directory.resolve("rolled-back.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			archive.rollback();
			assertThrows(IOException.class, () -> archive.openWriter("lost.txt", ""));
		}
	}
}
