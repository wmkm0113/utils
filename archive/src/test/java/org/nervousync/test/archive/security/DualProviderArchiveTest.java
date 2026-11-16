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
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.StrongAccessMode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.certificate.CertificateProvider;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.nervousync.test.archive.security.StrongCertificateTestSupport.*;

/** Both providers must coexist and be selected independently for each encrypted region. */
final class DualProviderArchiveTest {
	@TempDir Path directory;

	static Stream<Arguments> modes() {
		return Stream.of(StrongAccessMode.values()).flatMap(entry ->
				Stream.of(StrongAccessMode.values()).flatMap(central ->
						Stream.of(Layout.values()).map(layout -> Arguments.of(entry, central, layout))));
	}

	private ArchiveOptions credentials() throws Exception {
		return ArchiveOptions.newBuilder().passwordProvider(PasswordProvider.fixed(PASSWORD.toCharArray()))
				.certificateProvider(fixtures().secondProvider()).build();
	}

	@ParameterizedTest(name = "entry={0}, central={1}, layout={2}")
	@MethodSource("modes")
	void roundTripsAllEntryAndCentralAccessPairs(final StrongAccessMode entry,
	                                            final StrongAccessMode central, final Layout layout) throws Exception {
		ZipOptions options = zipBuilder(AES256, 256, entry, layout)
				.centralDirectoryCrypto(AES256, 256, central,
						central.useCertificate() ? fixtures().recipients() : List.of()).build();
		Path target = directory.resolve("mixed.zip");
		ArchiveOptions both = credentials();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "rw"), both, options)) {
			writeEntry(archive, ENTRY_PATH, PAYLOAD);
		}
		assertArrayEquals(PAYLOAD, readEntry(readable(target, layout), options, both, ENTRY_PATH));
	}

	@Test
	void passwordOnlyIgnoresUnusedCertificateProvider() throws Exception {
		Stored stored = encrypt(AES256, 256, StrongAccessMode.PASSWORD, payload(65));
		CertificateProvider unused = request -> { throw new AssertionError("Password-only stream must not request a certificate"); };
		assertArrayEquals(payload(65), decrypt(stored, PasswordProvider.fixed(PASSWORD.toCharArray()), unused));
	}

	@Test
	void bothUsesPasswordWhenCertificateUnavailable() throws Exception {
		Stored stored = encrypt(AES256, 256, StrongAccessMode.BOTH, payload(65));
		assertArrayEquals(payload(65), decrypt(stored, PasswordProvider.fixed(PASSWORD.toCharArray()), CertificateProvider.none()));
	}

	@Test
	void bothPrefersCertificateWhenBothAreAvailable() throws Exception {
		Stored stored = encrypt(AES256, 256, StrongAccessMode.BOTH, payload(65));
		PasswordProvider unused = request -> { throw new AssertionError("Valid certificate must take priority"); };
		assertArrayEquals(payload(65), decrypt(stored, unused, fixtures().secondProvider()));
	}

	@Test
	void certificateOnlyIgnoresUnusedPasswordProvider() throws Exception {
		Stored stored = encrypt(AES256, 256, StrongAccessMode.CERTIFICATE, payload(65));
		PasswordProvider unused = request -> { throw new AssertionError("Certificate-only stream must not request a password"); };
		assertArrayEquals(payload(65), decrypt(stored, unused, fixtures().secondProvider()));
	}

	@Test
	void optionsCopyRetainsBothProviders() throws Exception {
		PasswordProvider password = PasswordProvider.fixed(PASSWORD.toCharArray());
		CertificateProvider certificate = fixtures().secondProvider();
		for (ArchiveOptions options : List.of(
				ArchiveOptions.newBuilder().passwordProvider(password).certificateProvider(certificate).build(),
				ArchiveOptions.newBuilder().certificateProvider(certificate).passwordProvider(password).build())) {
			ArchiveOptions copy = ArchiveOptions.newBuilder(options).build();
			assertSame(password, copy.getPasswordProvider());
			assertSame(certificate, copy.getCertificateProvider());
		}
	}
}
