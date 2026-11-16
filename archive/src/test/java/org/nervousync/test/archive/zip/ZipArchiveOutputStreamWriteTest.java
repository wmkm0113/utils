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
import org.nervousync.archive.enumerations.SplitType;
import org.nervousync.archive.io.zip.ZipArchiveOutputStream;
import org.nervousync.archive.zip.ZipOptions;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class ZipArchiveOutputStreamWriteTest {

	@TempDir
	Path directory;

	@Test
	void acceptsEmptySingleByteAndShortWrites() throws Exception {
		Path target = this.directory.resolve("short.zip");
		Path staging = Files.createDirectory(this.directory.resolve("staging"));
		try (LocalDataFile source = new LocalDataFile(target.toString(), "rw");
		     ZipArchiveOutputStream output = new ZipArchiveOutputStream(staging, source, ZipOptions.newBuilder().build())) {
			output.write(new byte[0]);
			output.write(1);
			output.write(new byte[]{2, 3, 4});
			output.write(new byte[]{9, 5, 6, 9}, 1, 2);
			output.finish();
			assertEquals(1, output.publishFiles().size());
			Path stored = output.publishFiles().keySet().iterator().next();
			assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6}, Files.readAllBytes(stored));
		}
	}

	@Test
	void splitsLargeNativePayloadAndKeepsFirstMarkerFourBytesLong() throws Exception {
		Path target = this.directory.resolve("native.zip");
		Path staging = Files.createDirectory(this.directory.resolve("staging"));
		byte[] payload = ZipWriteTestSupport.payload(130001);
		ZipOptions options = ZipOptions.newBuilder().splitArchive(true, 65536).splitType(SplitType.NATIVE).build();
		try (LocalDataFile source = new LocalDataFile(target.toString(), "rw");
		     ZipArchiveOutputStream output = new ZipArchiveOutputStream(staging, source, options)) {
			output.write(payload);
			assertEquals(2L, output.diskCount());
			output.finish();
			Map<Path, Path> files = output.publishFiles();
			assertEquals(2, files.size());
			Path first = sourceFor(files, target.resolveSibling("native.z01"));
			Path last = sourceFor(files, target);
			byte[] firstBytes = Files.readAllBytes(first);
			assertEquals(65536, firstBytes.length);
			assertTrue(Files.size(last) <= 65536L);
			assertArrayEquals(new byte[]{0x50, 0x4B, 0x07, 0x08}, Arrays.copyOf(firstBytes, 4));
			ByteArrayOutputStream combined = new ByteArrayOutputStream();
			combined.write(firstBytes, 4, firstBytes.length - 4);
			combined.write(Files.readAllBytes(last));
			assertArrayEquals(payload, combined.toByteArray());
		}
	}

	@Test
	void numberedSlicesUseLogicalOffsetsAndDoNotInsertNativeMarker() throws Exception {
		Path target = this.directory.resolve("numbered.zip");
		Path staging = Files.createDirectory(this.directory.resolve("staging"));
		byte[] payload = new byte[20];
		for (int i = 0; i < payload.length; i++) {
			payload[i] = (byte) i;
		}
		ZipOptions options = ZipOptions.newBuilder().splitArchive(true, 7).splitType(SplitType.NUMBERED).build();
		try (LocalDataFile source = new LocalDataFile(target.toString(), "rw");
		     ZipArchiveOutputStream output = new ZipArchiveOutputStream(staging, source, options)) {
			output.write(payload);
			assertEquals(20L, output.offset());
			assertEquals(0L, output.diskNumber());
			assertEquals(1L, output.diskCount());
			output.finish();
			List<Path> parts = output.publishFiles().keySet().stream()
					.sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
			assertEquals(3, parts.size());
			assertEquals(7L, Files.size(parts.get(0)));
			assertEquals(7L, Files.size(parts.get(1)));
			assertEquals(6L, Files.size(parts.get(2)));
			ByteArrayOutputStream combined = new ByteArrayOutputStream();
			for (Path part : parts) {
				combined.write(Files.readAllBytes(part));
			}
			assertArrayEquals(payload, combined.toByteArray());
		}
	}

	private static Path sourceFor(final Map<Path, Path> files, final Path target) {
		return files.entrySet().stream().filter(entry -> entry.getValue().equals(target))
				.map(Map.Entry::getKey).findFirst().orElseThrow();
	}
}
