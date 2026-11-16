/*
 * Licensed to the Nervousync Studio (NSYC) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nervousync.test.archive.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.StrongAccessMode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.security.StrongCertificateTestSupport.*;

/** Public archive API coverage for protected metadata, split output, merge and edit. */
final class StrongCertificateArchiveTest {

	@TempDir
	Path directory;

	@BeforeAll
	static void generateCertificates() throws Exception {
		fixtures();
	}

	private static Stream<Arguments> archiveCases() {
		// The public ZipOptions builder normalizes DES to 64 bits; DES-56 is tested at stream level.
		return algorithms().filter(value -> !"DES-56".equals(value.get()[0])).flatMap(value -> {
			Object[] algorithm = value.get();
			return Stream.of(Layout.values()).flatMap(layout -> Stream.of(StrongAccessMode.values())
					.map(mode -> Arguments.of(algorithm[0], algorithm[1], algorithm[2], layout, mode)));
		});
	}

	@ParameterizedTest(name = "{0}, {3}, {4}")
	@MethodSource("archiveCases")
	void createsAndReadsProtectedArchive(final String name, final int algorithm, final int bits,
	                                     final Layout layout, final StrongAccessMode mode) throws Exception {
		Path target = this.directory.resolve("archive.zip");
		ZipOptions zip = zipBuilder(algorithm, bits, mode, layout).build();
		createArchive(target, zip, mode);
		Path readable = readable(target, layout);
		if (layout == Layout.NATIVE) {
			assertTrue(Files.isRegularFile(this.directory.resolve("archive.z01")));
		} else if (layout == Layout.NUMBERED) {
			assertTrue(Files.isRegularFile(readable));
			assertTrue(Files.isRegularFile(Path.of(target + ".002")));
		}
		if (mode.useCertificate()) {
			assertArrayEquals(PAYLOAD, readEntry(readable, zip, certificateOptions(), ENTRY_PATH));
		}
		if (mode.usePassword()) {
			assertArrayEquals(PAYLOAD, readEntry(readable, zip, passwordOptions(), ENTRY_PATH));
		}
	}

	@ParameterizedTest
	@EnumSource(StrongAccessMode.class)
	void supportsStoredEntries(final StrongAccessMode mode) throws Exception {
		Path target = this.directory.resolve("stored.zip");
		ZipOptions zip = zipBuilder(AES256, 256, mode, Layout.SINGLE)
				.compressionAlgorithm(CompressionAlgorithm.STORE).build();
		createArchive(target, zip, mode);
		assertArrayEquals(PAYLOAD, readEntry(target, zip, writeOptions(mode), ENTRY_PATH));
	}

	@ParameterizedTest
	@EnumSource(value = StrongAccessMode.class, names = {"CERTIFICATE", "BOTH"})
	void supportsCertificateEntriesWithPlainCentralDirectory(final StrongAccessMode mode) throws Exception {
		Path target = this.directory.resolve("plain-central.zip");
		ZipOptions zip = ZipOptions.newBuilder()
				.strongCrypto(AES256, 256, mode, fixtures().recipients()).build();
		createArchive(target, zip, mode);
		assertArrayEquals(PAYLOAD, readEntry(target, zip, certificateOptions(), ENTRY_PATH));
		if (mode.usePassword()) {
			assertArrayEquals(PAYLOAD, readEntry(target, zip, passwordOptions(), ENTRY_PATH));
		}
	}

	private static Stream<Arguments> mergeCases() {
		return Stream.of(Layout.NATIVE, Layout.NUMBERED).flatMap(layout ->
				Stream.of(StrongAccessMode.CERTIFICATE, StrongAccessMode.BOTH)
						.map(mode -> Arguments.of(layout, mode)));
	}

	@ParameterizedTest(name = "merge {0}, {1}")
	@MethodSource("mergeCases")
	void mergedArchivePreservesCertificateRecipients(final Layout layout,
	                                                final StrongAccessMode mode) throws Exception {
		Path source = this.directory.resolve("source.zip");
		Path merged = this.directory.resolve("merged.zip");
		ZipOptions split = zipBuilder(AES256, 256, mode, layout).build();
		createArchive(source, split, mode);
		Path readable = readable(source, layout);
		byte[] firstVolume = Files.readAllBytes(layout == Layout.NATIVE
				? this.directory.resolve("source.z01") : readable);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(readable.toString(), "r"),
				writeOptions(mode), split)) {
			archive.merge(merged);
		}
		assertArrayEquals(firstVolume, Files.readAllBytes(layout == Layout.NATIVE
				? this.directory.resolve("source.z01") : readable));
		ZipOptions single = zipBuilder(AES256, 256, mode, Layout.SINGLE).build();
		assertArrayEquals(PAYLOAD, readEntry(merged, single, certificateOptions(), ENTRY_PATH));
		if (mode.usePassword()) {
			assertArrayEquals(PAYLOAD, readEntry(merged, single, passwordOptions(), ENTRY_PATH));
		}
	}

	@ParameterizedTest
	@EnumSource(value = StrongAccessMode.class, names = {"CERTIFICATE", "BOTH"})
	void editingPreservesRetainedEntryAndEncryptsNewEntry(final StrongAccessMode mode) throws Exception {
		Path target = this.directory.resolve("edit.zip");
		ZipOptions zip = zipBuilder(AES256, 256, mode, Layout.SINGLE).build();
		ArchiveOptions credentials = writeOptions(mode);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"), credentials, zip)) {
			writeEntry(archive, ENTRY_PATH, PAYLOAD);
			writeEntry(archive, "delete.bin", new byte[]{1});
		}
		byte[] added = payload(73);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"), credentials, zip)) {
			archive.remove(archive.entry("delete.bin").orElseThrow());
			writeEntry(archive, "added.bin", added);
		}
		assertArrayEquals(PAYLOAD, readEntry(target, zip, certificateOptions(), ENTRY_PATH));
		assertArrayEquals(added, readEntry(target, zip, certificateOptions(), "added.bin"));
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "r"), certificateOptions(), zip)) {
			assertTrue(archive.entry("delete.bin").isEmpty());
		}
	}

	@Test
	void rejectsMalformedCentralCertificateRecipientCount() throws Exception {
		Path target = this.directory.resolve("invalid-recipients.zip");
		ZipOptions zip = ZipOptions.newBuilder().strongCrypto(
				AES256, 256, StrongAccessMode.CERTIFICATE, fixtures().recipients()).build();
		createArchive(target, zip, StrongAccessMode.CERTIFICATE);
		byte[] bytes = Files.readAllBytes(target);
		ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		int end = bytes.length - 22;
		assertEquals(0x06054B50, data.getInt(end));
		int central = data.getInt(end + 16);
		assertEquals(0x02014B50, data.getInt(central));
		int extra = central + 46 + Short.toUnsignedInt(data.getShort(central + 28));
		int limit = extra + Short.toUnsignedInt(data.getShort(central + 30));
		boolean altered = false;
		while (extra < limit) {
			int tag = Short.toUnsignedInt(data.getShort(extra));
			int length = Short.toUnsignedInt(data.getShort(extra + 2));
			if (tag == ZipGlobals.STRONG_ENC_HEADER_ID) {
				data.putInt(extra + 4 + 8, 0);
				altered = true;
				break;
			}
			extra += 4 + length;
		}
		assertTrue(altered, "The strong certificate extra field must be present");
		Files.write(target, bytes);
		assertThrows(IOException.class, () -> {
			try (LocalDataFile file = new LocalDataFile(target.toString(), "r");
			     ArchiveFile ignored = ArchiveFile.zipFile(file, certificateOptions(), zip)) {
				fail("Malformed recipient count was accepted");
			}
		});
	}
}
