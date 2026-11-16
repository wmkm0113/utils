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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.utils.commons.RawUtils;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

final class StrongEncryptionProviderMatrixTest {

	private static Stream<Arguments> passwordAlgorithms() {
		return Stream.of(
				Arguments.of("DES-56", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES, 56, 8),
				Arguments.of("3DES-112", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112, 112, 24),
				Arguments.of("3DES-168", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168, 168, 24),
				Arguments.of("AES-128", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128, 128, 16),
				Arguments.of("AES-192", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192, 192, 24),
				Arguments.of("AES-256", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256, 32),
				Arguments.of("RC2-40", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW, 40, 5),
				Arguments.of("RC2-128", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW, 128, 16),
				Arguments.of("RC4-128", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4, 128, 16),
				Arguments.of("Blowfish-128", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH, 128, 16),
				Arguments.of("Twofish-256", ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH, 256, 32)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("passwordAlgorithms")
	void derivesKeyAndRoundTripsRandomData(final String name, final int algorithm, final int keyBitLength,
	                                       final int expectedKeyBytes) throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(algorithm);

		byte[] material = ("password-" + name).getBytes(StandardCharsets.UTF_8);

		byte[] iv = new byte[cipher.blockSize()];
		byte[] randomData = new byte[Math.max(cipher.blockSize(), 16)];

		for (int i = 0; i < iv.length; i++) {
			iv[i] = (byte) (0x20 + i);
		}
		for (int i = 0; i < randomData.length; i++) {
			randomData[i] = (byte) (0x50 + i);
		}

		byte[] key = null;
		byte[] encrypted = null;
		byte[] decrypted = null;

		try {
			assertDoesNotThrow(() ->
					cipher.validateHeader(algorithm, keyBitLength, ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD, iv));

			key = cipher.deriveKey(material, keyBitLength);
			assertEquals(expectedKeyBytes, key.length);

			encrypted = cipher.encryptRandomData(randomData, key, iv);
			decrypted = cipher.decryptRandomData(encrypted, key, iv);

			assertArrayEquals(randomData, decrypted);
		} finally {
			RawUtils.clear(material);
			RawUtils.clear(iv);
			RawUtils.clear(randomData);
			RawUtils.clear(key);
			RawUtils.clear(encrypted);
			RawUtils.clear(decrypted);
		}
	}

	@Test
	void resolvesLegacyRC2Algorithm() {
		assertThrows(ArchiveException.class, () -> EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_OLD));
	}

	@Test
	void supportsMaximumBlowfishKeyLength() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH);

		byte[] material = "blowfish-448".getBytes(StandardCharsets.UTF_8);
		try {
			assertThrows(CryptoException.class, () -> cipher.deriveKey(material, 448));
		} finally {
			RawUtils.clear(material);
		}
	}

	@ParameterizedTest(name = "{0} with ErdData 3DES flag")
	@MethodSource("passwordAlgorithms")
	void acceptsErdDataTripleDESFlag(final String name, final int algorithm, final int keyBitLength) throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(algorithm);
		int flags = ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD | ZipGlobals.ZIP64_HEADER_ENC_FLAGS_ERD_3DES;
		assertDoesNotThrow(() -> cipher.validateHeader(algorithm, keyBitLength, flags, new byte[cipher.blockSize()]));
	}

	@Test
	void initializesTwofishUsingArchiveRuntime() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH);
		byte[] key = cipher.deriveKey("twofish-runtime".getBytes(StandardCharsets.UTF_8), 256);
		assertDoesNotThrow(() -> cipher.encryptor(key, new byte[16]));
	}
}