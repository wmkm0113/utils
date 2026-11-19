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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipAdaptor;
import org.nervousync.archive.zip.ZipOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

final class ZipArchiveEditTest {

	@TempDir
	Path directory;

	private static Stream<Arguments> retainedEntries() {
		return Stream.of(CompressionAlgorithm.STORE, CompressionAlgorithm.DEFLATE).flatMap(algorithm ->
				Stream.of(Encryption.values()).map(encryption -> Arguments.of(algorithm, encryption)));
	}

	@ParameterizedTest(name = "retain {0}, {1}")
	@MethodSource("retainedEntries")
	void preservesExistingEncryptedEntryWhileDeletingAnother(final CompressionAlgorithm algorithm,
	                                                       final Encryption encryption) throws Exception {
		Path target = this.directory.resolve("edit.zip");
		byte[] bytes = payload(73);
		ZipOptions options = options(algorithm, Zip64Mode.AUTO, Layout.SINGLE, encryption);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				archiveOptions(), options)) {
			writeEntry(archive, "keep.bin", bytes);
			writeEntry(archive, "delete.bin", new byte[]{1, 2, 3});
		}
		assertContent(target, options, Map.of("keep.bin", bytes));
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				archiveOptions(), options)) {
			archive.remove(archive.entry("delete.bin").orElseThrow());
		}
		assertContent(target, options, Map.of("keep.bin", bytes));
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "r"),
				archiveOptions(), options)) {
			assertTrue(archive.entry("delete.bin").isEmpty());
		}
	}

	@Test
	void replacementAppearsExactlyOnceAndOtherEntrySurvives() throws Exception {
		Path target = this.directory.resolve("replace.zip");
		Map<String, byte[]> original = new LinkedHashMap<>();
		original.put("first.txt", new byte[]{1});
		original.put("second.txt", new byte[]{2});
		createJdkArchive(target, original);
		byte[] replacement = {3, 4, 5};
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			writeEntry(archive, "first.txt", replacement);
		}
		try (ZipFile archive = new ZipFile(target.toFile())) {
			List<String> names = Collections.list(archive.entries()).stream()
					.map(ZipEntry::getName).collect(Collectors.toList());
			assertEquals(2, names.size());
			assertEquals(1, Collections.frequency(names, "first.txt"));
			assertArrayEquals(replacement, archive.getInputStream(archive.getEntry("first.txt")).readAllBytes());
			assertArrayEquals(new byte[]{2}, archive.getInputStream(archive.getEntry("second.txt")).readAllBytes());
		}
	}

	@Test
	void rollbackPreservesOriginalBytesAndRemovesStagedChanges() throws Exception {
		Path target = this.directory.resolve("rollback.zip");
		createJdkArchive(target, Map.of("original.txt", new byte[]{1, 2, 3}));
		byte[] original = Files.readAllBytes(target);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			writeEntry(archive, "new.txt", payload(4096));
			archive.remove(archive.entry("original.txt").orElseThrow());
			archive.rollback();
		}
		assertArrayEquals(original, Files.readAllBytes(target));
		assertEquals(Set.of("rollback.zip"), childNames(this.directory));
	}

	@Test
	void rollbackOfNewArchiveRemovesTargetPlaceholder() throws Exception {
		Path target = this.directory.resolve("cancelled.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			writeEntry(archive, "cancelled.txt", new byte[]{1});
			archive.rollback();
		}
		assertFalse(Files.exists(target));
		assertTrue(childNames(this.directory).isEmpty());
	}

	@Test
	void successfulEditLeavesOnlyPublishedArchive() throws Exception {
		Path target = this.directory.resolve("published.zip");
		createJdkArchive(target, Map.of("old.txt", new byte[]{1}));
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			writeEntry(archive, "new.txt", new byte[]{2});
		}
		assertEquals(Set.of("published.zip"), childNames(this.directory));
		assertContent(target, ZipOptions.newBuilder().build(), Map.of("old.txt", new byte[]{1}, "new.txt", new byte[]{2}));
	}

	@Test
	void unchangedEditDoesNotLeaveWorkspace() throws Exception {
		Path target = this.directory.resolve("unchanged.zip");
		createJdkArchive(target, Map.of("old.txt", new byte[]{1}));
		byte[] original = Files.readAllBytes(target);
		try (ArchiveFile ignored = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"),
				ZipOptions.newBuilder().build())) {
			// A no-op edit must not rebuild or leave temporary files.
		}
		assertArrayEquals(original, Files.readAllBytes(target));
		assertEquals(Set.of("unchanged.zip"), childNames(this.directory));
	}

	@Test
	void committedAdaptorRejectsAnotherWriter() throws Exception {
		Path target = this.directory.resolve("committed.zip");
		createJdkArchive(target, Map.of());
		try (ZipAdaptor adaptor = new ZipAdaptor(new LocalDataFile(target.toString(), "rw"),
				archiveOptions(), ZipOptions.newBuilder().build())) {
			adaptor.commit();
			assertThrows(IOException.class, () -> adaptor.openWriter("lost.txt", ""));
		}
	}

	@Test
	void passwordProviderReceivesArchivePathAndEntryPath() throws Exception {
		Path target = this.directory.resolve("password.zip");
		PasswordProvider fixed = PasswordProvider.fixed(PASSWORD.toCharArray());
		ArchiveOptions options = ArchiveOptions.newBuilder().passwordProvider(request -> {
			assertEquals(target.toString(), request.getArchivePath());
			assertEquals("entry.bin", request.getEntryPath());
			return request.getArchivePath().equals(target.toString())
					? fixed.credentials(request) : Optional.empty();
		}).build();
		ZipOptions zipOptions = ZipOptions.newBuilder().aesCrypto(256).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"), options, zipOptions)) {
			writeEntry(archive, "entry.bin", new byte[]{1, 2, 3});
		}
		assertContent(target, zipOptions, Map.of("entry.bin", new byte[]{1, 2, 3}));
	}
}
