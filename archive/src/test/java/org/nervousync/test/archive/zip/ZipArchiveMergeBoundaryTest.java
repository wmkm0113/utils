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
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.PasswordPurpose;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipMergeTestSupport.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/** Regression tests for target ownership, cross-directory cleanup, and source-session reuse. */
final class ZipArchiveMergeBoundaryTest {

	@TempDir
	Path directory;

	@Test
	void failedMergePreservesPreExistingTargetBytes() throws Exception {
		ZipOptions options = maskedOptions();
		Path source = createSource(this.directory, Layout.NATIVE, options);
		Map<Path, byte[]> before = volumeHashes(source, options);
		Path target = this.directory.resolve("merged.zip");
		byte[] original = {9, 8, 7, 6};
		Files.write(target, original);

		try (ArchiveFile archive = openWithoutHeaderWriteCredentials(source, options)) {
			assertThrows(ArchiveException.class, () -> archive.merge(target));
		}

		assertTrue(Files.isRegularFile(target), "A failed merge must preserve a pre-existing target");
		assertArrayEquals(original, Files.readAllBytes(target));
		assertVolumeHashes(before);
	}

	@Test
	void mergesIntoAnotherDirectoryAndRemovesItsWorkspace() throws Exception {
		Path sourceDirectory = Files.createDirectory(this.directory.resolve("source"));
		Path outputDirectory = Files.createDirectory(this.directory.resolve("output"));
		ZipOptions options = maskedOptions();
		Path source = createSource(sourceDirectory, Layout.NATIVE, options);
		Map<Path, byte[]> before = volumeHashes(source, options);
		Path target = outputDirectory.resolve("merged.zip");

		try (ArchiveFile archive = open(source, "r", options)) {
			archive.merge(target);
		}

		assertContent(target, ZipOptions.newBuilder().build(), contents());
		assertEquals(Set.of("merged.zip"), childNames(outputDirectory),
				"Only the published ZIP should remain in the output directory");
		assertVolumeHashes(before);
	}

	@Test
	void crossDirectoryFailurePreservesWriteErrorAndLeavesNoTargetOrWorkspace() throws Exception {
		Path sourceDirectory = Files.createDirectory(this.directory.resolve("source"));
		Path outputDirectory = Files.createDirectory(this.directory.resolve("output"));
		ZipOptions options = maskedOptions();
		Path source = createSource(sourceDirectory, Layout.NATIVE, options);
		Map<Path, byte[]> before = volumeHashes(source, options);
		Path target = outputDirectory.resolve("merged.zip");

		try (ArchiveFile archive = openWithoutHeaderWriteCredentials(source, options)) {
			// The missing-write-credential ArchiveException must survive cleanup.
			assertThrows(ArchiveException.class, () -> archive.merge(target));
		}

		assertFalse(Files.exists(target), "A failed merge must not retain its placeholder target");
		assertTrue(childNames(outputDirectory).isEmpty(), "A failed write must remove its workspace");
		assertVolumeHashes(before);
	}

	@Test
	void sourceEditSessionCanWriteAndCommitAfterMerge() throws Exception {
		ZipOptions options = splitOptions(Layout.NATIVE);
		Path source = createSource(this.directory, Layout.NATIVE, options);
		Path target = this.directory.resolve("merged.zip");
		byte[] added = {1, 2, 3};

		try (ArchiveFile archive = open(source, "rw", options)) {
			archive.merge(target);
			writeEntry(archive, "added.bin", added);
		}

		assertContent(source, options, Map.of(
				"data.bin", DATA, "empty.bin", new byte[0], "added.bin", added));
		ZipOptions single = ZipOptions.newBuilder().build();
		assertContent(target, single, contents());
		try (ArchiveFile merged = open(target, "r", single)) {
			assertTrue(merged.entry("added.bin").isEmpty(),
					"Editing the source after merge must not change the previously merged output");
		}
	}

	@Test
	void rejectedDirectoryTargetRemainsPresentAndEmpty() throws Exception {
		ZipOptions options = maskedOptions();
		Path source = createSource(this.directory, Layout.NATIVE, options);
		Map<Path, byte[]> before = volumeHashes(source, options);
		Path target = Files.createDirectory(this.directory.resolve("merged.zip"));

		try (ArchiveFile archive = open(source, "r", options)) {
			assertThrows(IOException.class, () -> archive.merge(target));
		}

		assertTrue(Files.isDirectory(target), "A failed merge must not delete an existing directory");
		assertTrue(childNames(target).isEmpty());
		assertVolumeHashes(before);
	}

	private static ArchiveFile openWithoutHeaderWriteCredentials(final Path source, final ZipOptions options)
			throws Exception {
		PasswordProvider fixed = PasswordProvider.fixed(PASSWORD.toCharArray());
		ArchiveOptions credentials = ArchiveOptions.newBuilder().passwordProvider(request ->
				request.getPurpose() == PasswordPurpose.WRITE_HEADER
						? Optional.empty() : fixed.credentials(request)).build();
		return ArchiveFile.zipFile(new LocalDataFile(source.toString(), "r"), credentials, options);
	}
}
