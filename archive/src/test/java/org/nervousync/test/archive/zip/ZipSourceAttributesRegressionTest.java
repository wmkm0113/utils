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
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.zip.ZipOptions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Source metadata must come from the source Path rather than the compressed staging file. */
public class ZipSourceAttributesRegressionTest {
	@TempDir Path directory;

	private void create(Path source, Path target) throws Exception {
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"), ZipOptions.newBuilder().build());
			 ResourceWriter writer = archive.openWriter("data.bin", "").orElseThrow()) {
			writer.write(source);
			writer.finish();
		}
	}

	@Test
	void preservesSubSecondModifiedTimeInListingAndJdkReader() throws Exception {
		Path source = Files.writeString(directory.resolve("source.bin"), "metadata payload");
		Files.setLastModifiedTime(source, FileTime.from(Instant.parse("2024-03-05T07:09:11.123456Z")));
		Instant expected = Files.getLastModifiedTime(source).toInstant();
		Path target = directory.resolve("times.zip");
		create(source, target);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "r"), ZipOptions.newBuilder().build());
			 ZipFile jdk = new ZipFile(target.toFile())) {
			assertAll(
					() -> assertEquals(expected, archive.entry("data.bin").orElseThrow().getLastModified()),
					() -> assertEquals(expected, jdk.getEntry("data.bin").getLastModifiedTime().toInstant()));
		}
	}

	@Test
	void preservesDosReadOnlyAndHiddenFlags() throws Exception {
		Path source = Files.writeString(directory.resolve("source.bin"), "metadata payload");
		DosFileAttributeView view = Files.getFileAttributeView(source, DosFileAttributeView.class);
		assumeTrue(view != null, "Source file system has no DOS attributes");
		try {
			view.setHidden(true);
			view.setReadOnly(true);
			Path target = directory.resolve("dos.zip");
			create(source, target);
			byte[] bytes = Files.readAllBytes(target);
			ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			int end = bytes.length - 22;
			assertEquals(0x06054b50, data.getInt(end));
			int central = data.getInt(end + 16);
			assertEquals(0x02014b50, data.getInt(central));
			assertEquals(0x03, data.getInt(central + 38) & 0x03);
		} finally {
			view.setReadOnly(false);
			view.setHidden(false);
		}
	}
}
