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

package org.nervousync.test.archive.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.EntryType;
import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.enumerations.StrongAccessMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.security.certificate.CertificateCredentials;
import org.nervousync.archive.security.certificate.CertificateProvider;
import org.nervousync.archive.security.certificate.PKCS12CertificateProvider;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.security.StrongCertificateTestSupport.*;

/** Additional credential rejection and extraction publication acceptance tests. */
public class ZipCredentialAcceptanceTest {

	@TempDir
	Path directory;

	private CertificateProvider mismatchedPrivateKey() throws Exception {
		return CertificateProvider.fixed(new CertificateCredentials(
				fixtures().second, fixtures().firstKey.getPrivate(), null));
	}

	@ParameterizedTest
	@EnumSource(value = StrongAccessMode.class, names = {"CERTIFICATE", "BOTH"})
	void matchingCertificateWithWrongPrivateKeyIsRejected(final StrongAccessMode mode) throws Exception {
		Stored stored = encrypt(AES256, 256, mode, payload(65));
		CertificateProvider wrong = mismatchedPrivateKey();
		assertThrows(IOException.class, () -> decrypt(stored, null, wrong));
	}

	@ParameterizedTest
	@EnumSource(value = StrongAccessMode.class, names = {"CERTIFICATE", "BOTH"})
	void protectedCentralDirectoryRejectsWrongPrivateKey(final StrongAccessMode mode) throws Exception {
		Path zip = directory.resolve("wrong-private-key.zip");
		ZipOptions options = zipBuilder(AES256, 256, mode, Layout.SINGLE).build();
		createArchive(zip, options, mode);
		ArchiveOptions wrong = ArchiveOptions.newBuilder()
				.certificateProvider(mismatchedPrivateKey()).build();
		assertThrows(IOException.class, () -> readEntry(zip, options, wrong, ENTRY_PATH));
	}

	static Stream<Arguments> extractionFailures() {
		return Stream.of(StrongAccessMode.PASSWORD, StrongAccessMode.BOTH)
				.flatMap(mode -> Stream.of(false, true).map(existing -> Arguments.of(mode, existing)));
	}

	@ParameterizedTest(name = "{0}, existingTarget={1}")
	@MethodSource("extractionFailures")
	void wrongPasswordDoesNotPublishPartialEntry(final StrongAccessMode mode,
	                                             final boolean existingTarget) throws Exception {
		Path zip = directory.resolve("wrong-password.zip");
		ZipOptions options = ZipOptions.newBuilder().strongCrypto(AES256, 256, mode,
				mode.useCertificate() ? fixtures().recipients() : List.of()).build();
		ArchiveEntry metadata = ArchiveEntry.newBuilder().entryPath("data.bin")
				.entryType(EntryType.FILE).lastModified(Instant.parse("2024-01-02T03:04:05Z")).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"), passwordOptions(), options);
		     ResourceWriter writer = archive.openWriter("data.bin", "").orElseThrow()) {
			try (OutputStream output = writer.openStream(metadata)) { output.write(PAYLOAD); }
			writer.finish();
		}
		Path parent = Files.createDirectory(directory.resolve("extracted"));
		Path target = parent.resolve("data.bin");
		byte[] original = payload(27);
		if (existingTarget) Files.write(target, original);
		ArchiveOptions wrong = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("incorrect".toCharArray())).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "r"), wrong, options);
		     ResourceReader reader = archive.openReader(archive.entry("data.bin").orElseThrow()).orElseThrow()) {
			ArchiveException failure = assertThrows(ArchiveException.class, () -> reader.extract(target,
					ExtractOptions.newBuilder().overrideMode(OverrideMode.ALWAYS).build()));
			assertEquals(0x0000001B000DL, failure.getErrorCode());
		}
		if (existingTarget) assertArrayEquals(original, Files.readAllBytes(target));
		else assertFalse(Files.exists(target));
		try (Stream<Path> files = Files.list(parent)) {
			assertEquals(existingTarget ? 1L : 0L, files.count(), "No staging files may remain");
		}
	}

	@Test
	void certificateEncryptionDoesNotRequireWriterPrivateKey() throws Exception {
		Path zip = directory.resolve("public-certificates-only.zip");
		ZipOptions options = zipBuilder(AES256, 256, StrongAccessMode.CERTIFICATE, Layout.SINGLE).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"),
				ArchiveOptions.newBuilder().build(), options)) {
			writeEntry(archive, ENTRY_PATH, PAYLOAD);
		}
		assertArrayEquals(PAYLOAD, readEntry(zip, options, certificateOptions(), ENTRY_PATH));
	}

	@Test
	void mixedExclusiveModesRequireBothProviders() throws Exception {
		Path zip = directory.resolve("mixed-credentials.zip");
		ZipOptions options = ZipOptions.newBuilder()
				.strongCrypto(AES256, 256, StrongAccessMode.CERTIFICATE, fixtures().recipients())
				.centralDirectoryCrypto(AES256, 256, StrongAccessMode.PASSWORD, List.of()).build();
		createArchive(zip, options, StrongAccessMode.PASSWORD);
		assertThrows(IOException.class, () -> readEntry(zip, options, passwordOptions(), ENTRY_PATH));
		assertThrows(IOException.class, () -> readEntry(zip, options, certificateOptions(), ENTRY_PATH));
		ArchiveOptions both = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed(PASSWORD.toCharArray()))
				.certificateProvider(fixtures().secondProvider()).build();
		assertArrayEquals(PAYLOAD, readEntry(zip, options, both, ENTRY_PATH));
	}

	@Test
	void pkcs12RejectsWrongPrivateKeyPassword() throws Exception {
		char[] storePassword = "store-secret".toCharArray();
		char[] keyPassword = "key-secret".toCharArray();
		char[] wrongPassword = "incorrect".toCharArray();
		try {
			Path p12 = directory.resolve("recipient.p12");
			KeyStore store = KeyStore.getInstance("PKCS12");
			store.load(null, storePassword);
			store.setKeyEntry("recipient", fixtures().secondKey.getPrivate(), keyPassword,
					new Certificate[]{fixtures().second});
			try (OutputStream output = Files.newOutputStream(p12)) { store.store(output, storePassword); }
			assertThrows(UnrecoverableKeyException.class,
					() -> PKCS12CertificateProvider.load(p12, storePassword, wrongPassword));
		} finally {
			Arrays.fill(storePassword, '\0');
			Arrays.fill(keyPassword, '\0');
			Arrays.fill(wrongPassword, '\0');
		}
	}
}
