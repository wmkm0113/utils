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
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.enumerations.zip.StrongAccessMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.security.StrongKey;
import org.nervousync.archive.security.certificate.CertificateCredentials;
import org.nervousync.archive.security.certificate.CertificateProvider;
import org.nervousync.archive.security.certificate.PKCS12CertificateProvider;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.crypto.strong.StrongOutputStream;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.security.StrongCertificateTestSupport.*;

/** Regression coverage for password/certificate selection, metadata and key transport. */
final class StrongCertificateStreamTest {


	private static byte[] decrypt(final Stored stored, final PasswordProvider provider) throws Exception {
		return StrongCertificateTestSupport.decrypt(stored, provider, null);
	}

	private static byte[] decrypt(final Stored stored, final CertificateProvider provider) throws Exception {
		return StrongCertificateTestSupport.decrypt(stored, null, provider);
	}

	@TempDir
	Path directory;

	@BeforeAll
	static void generateCertificates() throws Exception {
		fixtures();
	}

	private static Stream<Arguments> streamCases() {
		return algorithms().flatMap(value -> {
			Object[] algorithm = value.get();
			return Stream.of(
					Arguments.of(algorithm[0], algorithm[1], algorithm[2], StrongAccessMode.PASSWORD, false),
					Arguments.of(algorithm[0], algorithm[1], algorithm[2], StrongAccessMode.CERTIFICATE, true),
					Arguments.of(algorithm[0], algorithm[1], algorithm[2], StrongAccessMode.BOTH, false),
					Arguments.of(algorithm[0], algorithm[1], algorithm[2], StrongAccessMode.BOTH, true));
		});
	}

	@ParameterizedTest(name = "{0}, {3}, certificate={4}")
	@MethodSource("streamCases")
	void roundTripsUsingExactlyOneProvider(final String name, final int algorithm, final int bits,
	                                      final StrongAccessMode mode, final boolean certificate) throws Exception {
		byte[] data = payload(65);
		Stored stored = encrypt(algorithm, bits, mode, data);
		assertArrayEquals(data, certificate
				? decrypt(stored, fixtures().secondProvider())
				: decrypt(stored, PasswordProvider.fixed(PASSWORD.toCharArray())));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 7, 8, 15, 16, 17, 1025})
	void handlesEmptyAndPartialBlocks(final int size) throws Exception {
		byte[] data = payload(size);
		Stored stored = encrypt(AES256, 256, StrongAccessMode.BOTH, data);
		assertArrayEquals(data, decrypt(stored, fixtures().secondProvider()));
		assertArrayEquals(data, decrypt(stored, PasswordProvider.fixed(PASSWORD.toCharArray())));
	}

	@ParameterizedTest
	@EnumSource(value = StrongAccessMode.class, names = {"PASSWORD", "BOTH"})
	void preservesWrongPasswordExceptionAndErrorCode(final StrongAccessMode mode) throws Exception {
		Stored stored = encrypt(AES256, 256, mode, payload(65));
		ArchiveException failure = assertThrows(ArchiveException.class,
				() -> decrypt(stored, PasswordProvider.fixed("incorrect".toCharArray())));
		assertEquals(0x0000001B000DL, failure.getErrorCode());
	}

	@ParameterizedTest
	@EnumSource(StrongAccessMode.class)
	void rejectsMissingProviderWithoutClassCastException(final StrongAccessMode mode) throws Exception {
		Stored stored = encrypt(AES256, 256, mode, payload(65));
		assertThrows(IOException.class, () -> StrongCertificateTestSupport.decrypt(stored, PasswordProvider.none(), CertificateProvider.none()));
	}

	@Test
	void rejectsProviderTypeDisallowedByArchive() throws Exception {
		Stored password = encrypt(AES256, 256, StrongAccessMode.PASSWORD, payload(65));
		Stored certificate = encrypt(AES256, 256, StrongAccessMode.CERTIFICATE, payload(65));
		assertThrows(IOException.class, () -> decrypt(password, fixtures().secondProvider()));
		assertThrows(IOException.class,
				() -> decrypt(certificate, PasswordProvider.fixed(PASSWORD.toCharArray())));
	}

	@Test
	void certificateSelectionDoesNotRequestPasswordOrFallBack() throws Exception {
		Stored stored = encrypt(AES256, 256, StrongAccessMode.BOTH, payload(65));
		CertificateProvider unavailable = request -> { throw new IOException("Test private key unavailable"); };
		IOException failure = assertThrows(IOException.class, () -> decrypt(stored, unavailable));
		assertEquals("Test private key unavailable", failure.getMessage());
	}

	@Test
	void rejectsCertificateProviderWithoutMatchingRecipient() throws Exception {
		Stored stored = encrypt(AES256, 256, StrongAccessMode.BOTH, payload(65));
		CertificateProvider empty = request -> java.util.Optional.empty();
		assertThrows(IOException.class, () -> decrypt(stored, empty));
	}

	@Test
	void checksCentralRecipientHashesBeforeRequestingPrivateKey() throws Exception {
		Stored original = encrypt(AES256, 256, StrongAccessMode.CERTIFICATE, payload(65));
		byte[] data = original.info.getCertificateData().clone();
		data[data.length - 1] ^= 1;
		Stored altered = new Stored(original.bytes, new StrongEncryptionInfo(AES256, 256,
				original.info.getFlags(), data));
		CertificateProvider mustNotBeCalled = request -> {
			throw new AssertionError("Recipient mismatch must fail before requesting credentials");
		};
		assertThrows(IOException.class, () -> decrypt(altered, mustNotBeCalled));
	}

	@Test
	void bothWritingRequiresPasswordAndRecipientCertificates() throws Exception {
		assertThrows(IOException.class, () -> StrongOutputStream.create(new ByteArrayOutputStream(),
				AES256, 256, StrongAccessMode.BOTH, null, fixtures().recipients()));
		assertThrows(IOException.class, () -> StrongOutputStream.create(new ByteArrayOutputStream(),
				AES256, 256, StrongAccessMode.BOTH, PASSWORD.getBytes(java.nio.charset.StandardCharsets.UTF_8), List.of()));
	}

	@ParameterizedTest
	@ValueSource(ints = {40, 64, 128})
	void rc2SimpleBlobUsesCryptoApiIdentifierAndPreservesKey(final int bits) throws Exception {
		byte[] key = payload(bits / 8);
		byte[] blob = StrongKey.wrap(fixtures().second, RC2, bits, key);
		assertEquals(0x00006602, ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN).getInt(4));
		CertificateCredentials credentials = new CertificateCredentials(
				fixtures().second, fixtures().secondKey.getPrivate(), null);
		assertArrayEquals(key, StrongKey.unwrap(credentials, RC2, bits,
				ZipGlobals.ZIP64_HEADER_ENC_FLAGS_NON_OAEP, blob));
	}

	@Test
	void rc2BlobMappingDoesNotEnableLegacyZipRc2() {
		assertThrows(ArchiveException.class,
				() -> StrongKey.blobAlgorithm(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_OLD));
	}

	@Test
	void defaultOptionsAndPartialCertificateOptionsHaveValidDefaults() throws Exception {
		ZipOptions plain = ZipOptions.newBuilder().build();
		assertEquals(StrongAccessMode.PASSWORD, plain.getAccessMode());
		assertEquals(StrongAccessMode.PASSWORD, plain.getCentralDirectoryAccessMode());
		assertTrue(plain.getEntryRecipients().isEmpty());
		assertTrue(plain.getCentralDirectoryRecipients().isEmpty());
		ZipOptions entryOnly = ZipOptions.newBuilder()
				.strongCrypto(AES256, 256, StrongAccessMode.CERTIFICATE, fixtures().recipients()).build();
		assertTrue(entryOnly.getCentralDirectoryRecipients().isEmpty());
		ZipOptions centralOnly = ZipOptions.newBuilder()
				.centralDirectoryCrypto(AES256, 256, StrongAccessMode.CERTIFICATE, fixtures().recipients()).build();
		assertTrue(centralOnly.getEntryRecipients().isEmpty());
	}

	@Test
	void loadsMultiplePrivateKeysFromPkcs12AndDecrypts() throws Exception {
		char[] storePassword = "test-store-secret".toCharArray();
		char[] keyPassword = "test-key-secret".toCharArray();
		Path path = this.directory.resolve("recipients.p12");
		KeyStore store = KeyStore.getInstance("PKCS12");
		store.load(null, storePassword);
		store.setKeyEntry("first", fixtures().firstKey.getPrivate(), keyPassword,
				new Certificate[]{fixtures().first});
		store.setKeyEntry("second", fixtures().secondKey.getPrivate(), keyPassword,
				new Certificate[]{fixtures().second});
		try (OutputStream output = Files.newOutputStream(path)) {
			store.store(output, storePassword);
		}
		try {
			PKCS12CertificateProvider provider = PKCS12CertificateProvider.load(path, storePassword, keyPassword);
			byte[] data = payload(65);
			assertArrayEquals(data, decrypt(encrypt(AES256, 256, StrongAccessMode.CERTIFICATE, data), provider));
			assertThrows(IOException.class,
					() -> PKCS12CertificateProvider.load(path, "incorrect".toCharArray(), keyPassword));
		} finally {
			Arrays.fill(storePassword, '\0');
			Arrays.fill(keyPassword, '\0');
		}
	}
}
