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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.AesVendor;
import org.nervousync.archive.enumerations.PasswordPurpose;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.operator.ZipFileOperator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipMergeTestSupport.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/** Regression tests for merging split ZIP archives through the public archive API. */
final class ZipArchiveMergeTest {

	private enum CryptoMode { NONE, STANDARD, AE_1, AE_2, STRONG }

	@TempDir
	Path directory;

	@ParameterizedTest(name = "READ merge, {0}")
	@EnumSource(value = Layout.class, names = {"NATIVE", "NUMBERED"})
	void mergesReadOnlySourceIntoReadableSingleZip(final Layout layout) throws Exception {
		ZipOptions options = splitOptions(layout);
		Path source = createSource(this.directory, layout, options);
		Map<Path, byte[]> before = volumeHashes(source, options);
		Path target = this.directory.resolve("merged.zip");
		try (ArchiveFile archive = open(source, "r", options)) {
			archive.merge(target);
			assertContent(source, options, contents());
		}
		assertSingleZip(target);
		assertVolumeHashes(before);
	}

	@ParameterizedTest(name = "EDIT publication, {0}")
	@EnumSource(value = Layout.class, names = {"NATIVE", "NUMBERED"})
	void publishesCompleteOutputBeforeReturningAndReleasesTarget(final Layout layout) throws Exception {
		ZipOptions options = splitOptions(layout);
		Path source = createSource(this.directory, layout, options);
		Map<Path, byte[]> before = volumeHashes(source, options);
		Path target = this.directory.resolve("merged.zip");
		try (ArchiveFile archive = open(source, "rw", options)) {
			archive.merge(target);
			assertSingleZip(target);
		}
		assertSingleZip(target);
		assertVolumeHashes(before);
		Files.delete(target);
		assertFalse(Files.exists(target), "The completed output must not retain an open file handle");
	}

	private static Stream<Arguments> encryptedLayouts() {
		return Stream.of(Layout.NATIVE, Layout.NUMBERED).flatMap(layout ->
				Stream.of(CryptoMode.values()).map(crypto -> Arguments.of(layout, crypto)));
	}

	@ParameterizedTest(name = "workspace cleanup, {0}")
	@EnumSource(value = Layout.class, names = {"NATIVE", "NUMBERED"})
	void successfulMergeRemovesItsTemporaryWorkspace(final Layout layout) throws Exception {
		ZipOptions options = splitOptions(layout);
		Path source = createSource(this.directory, layout, options);
		try (ArchiveFile archive = open(source, "r", options)) {
			archive.merge(this.directory.resolve("merged.zip"));
		}
		try (Stream<Path> files = Files.list(this.directory)) {
			assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".nervousync-")),
					"Successful merging must remove its temporary workspace");
		}
	}

	@ParameterizedTest(name = "entry encryption, {0}, {1}")
	@MethodSource("encryptedLayouts")
	void preservesEncryptedEntryData(final Layout layout, final CryptoMode crypto) throws Exception {
		ZipOptions.Builder builder = ZipOptions.newBuilder(splitOptions(layout));
		switch (crypto) {
			case STANDARD:
				builder.standardCrypto();
				break;
			case AE_1:
				builder.aesCrypto(256, AesVendor.AE_1);
				break;
			case AE_2:
				builder.aesCrypto(256, AesVendor.AE_2);
				break;
			case STRONG:
				builder.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256);
				break;
			default:
				break;
		}
		ZipOptions options = builder.build();
		Path source = createSource(this.directory, layout, options);
		Path target = this.directory.resolve("merged.zip");
		try (ArchiveFile archive = open(source, "r", options)) {
			archive.merge(target);
		}
		assertContent(target, ZipOptions.newBuilder().build(), contents());
		try (ZipFileOperator operator = operator(target, ZipOptions.newBuilder().build())) {
			var end = operator.readEndCentralDirectoryRecord();
			var header = operator.readCentralDirectory(end, operator.readZip64EndCentralDirectory(end))
					.header("data.bin");
			assertNotNull(header);
			assertEquals(crypto != CryptoMode.NONE, header.encrypted());
			if (crypto == CryptoMode.AE_1 || crypto == CryptoMode.AE_2) {
				assertInstanceOf(AesEncryptionInfo.class, header.getEncryptionInfo());
				assertEquals(crypto == CryptoMode.AE_1 ? AesVendor.AE_1 : AesVendor.AE_2,
						((AesEncryptionInfo) header.getEncryptionInfo()).getVendor());
			}
		}
	}

	@ParameterizedTest(name = "central directory Protection, {0}")
	@EnumSource(Protection.class)
	void preservesProtectionWithoutRepeatingSourceWriteOptions(final Protection protection) throws Exception {
		ZipOptions sourceOptions = protectedOptions(Layout.NATIVE, Zip64Mode.AUTO, protection);
		Path source = createSource(this.directory, Layout.NATIVE, sourceOptions);
		Zip64EndCentralDirectory.Protection expected = readProtection(source, sourceOptions);
		assertNotNull(expected, "The source fixture must actually carry Protection");
		Path target = this.directory.resolve("merged.zip");
		// Reading requires split-layout settings and credentials, not the source writer's settings.
		try (ArchiveFile archive = open(source, "r", splitOptions(Layout.NATIVE))) {
			archive.merge(target);
		}
		ZipOptions single = ZipOptions.newBuilder().build();
		assertContent(target, single, contents());
		Zip64EndCentralDirectory.Protection actual = readProtection(target, single);
		assertNotNull(actual, "Merging must not discard the source central-directory Protection");
		assertAll(() -> assertEquals(expected.getCompressionMethod(), actual.getCompressionMethod()),
				() -> assertEquals(expected.getAlgorithm(), actual.getAlgorithm()),
				() -> assertEquals(expected.getKeyBitLength(), actual.getKeyBitLength()),
				() -> assertEquals(expected.getHashAlgorithm(), actual.getHashAlgorithm()));
	}

	@ParameterizedTest(name = "repeat merge and source preservation, {0}")
	@EnumSource(value = Layout.class, names = {"NATIVE", "NUMBERED"})
	void allowsRepeatedMergesWithoutChangingSourceVolumes(final Layout layout) throws Exception {
		ZipOptions options = splitOptions(layout);
		Path source = createSource(this.directory, layout, options);
		Map<Path, byte[]> before = volumeHashes(source, options);
		Path first = this.directory.resolve("first.zip");
		Path second = this.directory.resolve("second.zip");
		try (ArchiveFile archive = open(source, "r", options)) {
			archive.merge(first);
			assertContent(source, options, contents());
			archive.merge(second);
			assertContent(source, options, contents());
		}
		assertSingleZip(first);
		assertSingleZip(second);
		assertVolumeHashes(before);
	}

	@Test
	void preservesBothPhysicalEntriesWithTheSameName() throws Exception {
		Path source = createDuplicateNumberedSource(this.directory);
		ZipOptions options = splitOptions(Layout.NUMBERED);
		try (ZipFileOperator operator = operator(source, options)) {
			var end = operator.readEndCentralDirectoryRecord();
			assertEquals(2, operator.readCentralDirectory(end, operator.readZip64EndCentralDirectory(end))
					.getFileHeaders().size(), "The fixture must contain two physical entries");
		}
		Path target = this.directory.resolve("merged.zip");
		try (ArchiveFile archive = open(source, "r", options)) {
			archive.merge(target);
		}
		try (ZipFile zip = new ZipFile(target.toFile())) {
			assertEquals(2, zip.size());
			assertEquals(2L, zip.stream().filter(entry -> entry.getName().equals("same.bin")).count());
		}
	}

	@Test
	void maskedHeaderFixtureIsReadableBeforeMerge() throws Exception {
		ZipOptions options = maskedOptions();
		Path source = createMaskedSource(this.directory, options);
		assertContent(source, options, contents());
		assertMaskedFields(source, options);
	}

	@Test
	void keepsMaskedLocalHeaderFieldsZeroAfterMerge() throws Exception {
		ZipOptions options = maskedOptions();
		Path source = createMaskedSource(this.directory, options);
		assertContent(source, options, contents());
		assertMaskedFields(source, options);
		Path target = this.directory.resolve("merged.zip");
		try (ArchiveFile archive = open(source, "r", options)) {
			archive.merge(target);
		}
		ZipOptions single = ZipOptions.newBuilder().build();
		assertContent(target, single, contents());
		assertMaskedFields(target, single);
	}

	@Test
	void failedProtectedMergeDoesNotLeaveAnEmptyTarget() throws Exception {
		ZipOptions options = maskedOptions();
		Path source = createSource(this.directory, Layout.NATIVE, options);
		Map<Path, byte[]> before = volumeHashes(source, options);
		PasswordProvider fixed = PasswordProvider.fixed(PASSWORD.toCharArray());
		ArchiveOptions readOnlyCredentials = ArchiveOptions.newBuilder().passwordProvider(request ->
				request.getPurpose() == PasswordPurpose.WRITE_HEADER ? Optional.empty() : fixed.credentials(request))
				.build();
		Path target = this.directory.resolve("merged.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(source.toString(), "r"),
				readOnlyCredentials, options)) {
			assertThrows(IOException.class, () -> archive.merge(target));
		}
		assertFalse(Files.exists(target), "An unsuccessful merge must not publish a placeholder target");
		assertVolumeHashes(before);
	}

	private static void assertSingleZip(final Path target) throws Exception {
		assertTrue(Files.isRegularFile(target), "merge must publish the requested target");
		assertTrue(Files.size(target) >= 22L, "A complete ZIP needs its ending record");
		assertContent(target, ZipOptions.newBuilder().build(), contents());
		try (ZipFile zip = new ZipFile(target.toFile())) {
			assertEquals(2, zip.size());
			assertArrayEquals(DATA, zip.getInputStream(zip.getEntry("data.bin")).readAllBytes());
			assertEquals(0L, zip.getEntry("empty.bin").getSize());
		}
	}
}
