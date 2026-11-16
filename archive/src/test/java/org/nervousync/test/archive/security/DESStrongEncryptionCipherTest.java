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
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.crypto.strong.StrongCryptoEngine;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.utils.commons.RawUtils;

import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class DESStrongEncryptionCipherTest {

	private static final int DES_BLOCK_SIZE = 8;

	private static final byte[] IV = {
			(byte) 0x10, (byte) 0x21,
			(byte) 0x32, (byte) 0x43,
			(byte) 0x54, (byte) 0x65,
			(byte) 0x76, (byte) 0x87
	};

	private static final byte[] EXPECTED_DES_56_KEY =
			hex("DA70F2FEAE020B92");

	@Test
	void derivesExpectedTripleDESKeys() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112);

		byte[] material = "3des-test-password".getBytes(StandardCharsets.UTF_8);

		byte[] key112 = null;
		byte[] key168 = null;

		try {
			key112 = cipher.deriveKey(material, 112);
			key168 = cipher.deriveKey(material, 168);

			assertArrayEquals(hex("F7DC04E913CD94D0" + "3EAED367B3256D1C" + "F7DC04E913CD94D0"), key112);
			assertArrayEquals(hex("F7DC04E913CD94D0" + "3EAED367B3256D1C" + "4997F2B60D109D91"), key168);

			assertTrue(DESedeKeySpec.isParityAdjusted(key112, 0));
			assertTrue(DESedeKeySpec.isParityAdjusted(key168, 0));

			assertArrayEquals(Arrays.copyOfRange(key112, 0, 8), Arrays.copyOfRange(key112, 16, 24));
		} finally {
			RawUtils.clear(material);
			RawUtils.clear(key112);
			RawUtils.clear(key168);
		}
	}

	@Test
	void resolvesDESStrongEncryptionCipher() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES);

		assertNotNull(cipher);
		assertEquals(DES_BLOCK_SIZE, cipher.blockSize());
		assertTrue(cipher.usesPadding());
	}

	@Test
	void validatesDESHeaderParameters() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES);

		assertDoesNotThrow(() ->
				cipher.validateHeader(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES,
						56, ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD, IV));

		assertThrows(ArchiveException.class, () ->
				cipher.validateHeader(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128, 56,
						ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD, IV));

		assertThrows(ArchiveException.class, () ->
				cipher.validateHeader(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES, 128,
						ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD, IV));

		assertThrows(ArchiveException.class, () ->
				cipher.validateHeader(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES, 56,
						ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD, new byte[7]));

		assertThrows(ArchiveException.class, () ->
				cipher.validateHeader(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES, 56, 0, IV));
	}

	@Test
	void derivesExpectedDES56Key() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES);

		byte[] material = "des-test-password".getBytes(StandardCharsets.UTF_8);
		byte[] derivedKey = null;

		try {
			derivedKey = cipher.deriveKey(material, 56);

			assertEquals(DES_BLOCK_SIZE, derivedKey.length);
			assertArrayEquals(EXPECTED_DES_56_KEY, derivedKey);

			assertTrue(DESKeySpec.isParityAdjusted(derivedKey, 0));
		} finally {
			RawUtils.clear(material);
			RawUtils.clear(derivedKey);
		}
	}

	@Test
	void derivesEightByteKeyForDES64Representation()
			throws Exception {

		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES);
		byte[] material = "des-test-password".getBytes(StandardCharsets.UTF_8);
		byte[] derivedKey = null;

		try {
			derivedKey = cipher.deriveKey(material, 64);
			assertEquals(DES_BLOCK_SIZE, derivedKey.length);
			assertTrue(DESKeySpec.isParityAdjusted(derivedKey, 0));
		} finally {
			RawUtils.clear(material);
			RawUtils.clear(derivedKey);
		}
	}

	@Test
	void encryptsAndDecryptsRandomData() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES);

		byte[] material = "des-test-password".getBytes(StandardCharsets.UTF_8);

		byte[] masterSessionKey = null;
		byte[] randomData = {
				(byte) 0x31, (byte) 0x42,
				(byte) 0x53, (byte) 0x64,
				(byte) 0x75, (byte) 0x86,
				(byte) 0x97, (byte) 0xA8
		};

		byte[] encrypted = null;
		byte[] decrypted = null;

		try {
			masterSessionKey = cipher.deriveKey(material, 56);
			encrypted = cipher.encryptRandomData(randomData, masterSessionKey, IV);

			assertEquals(16, encrypted.length);
			assertFalse(Arrays.equals(randomData, encrypted));

			decrypted = cipher.decryptRandomData(encrypted, masterSessionKey, IV);

			assertArrayEquals(randomData, decrypted);
		} finally {
			RawUtils.clear(material);
			RawUtils.clear(masterSessionKey);
			RawUtils.clear(randomData);
			RawUtils.clear(encrypted);
			RawUtils.clear(decrypted);
		}
	}

	@Test
	void rejectsDamagedRandomDataPadding() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES);

		byte[] material = "des-test-password".getBytes(StandardCharsets.UTF_8);

		byte[] masterSessionKey = null;
		byte[] randomData = {
				0x01, 0x12, 0x23, 0x34,
				0x45, 0x56, 0x67, 0x78
		};

		byte[] encrypted = null;
		byte[] damaged = null;

		try {
			masterSessionKey = cipher.deriveKey(material, 56);
			encrypted = cipher.encryptRandomData(randomData, masterSessionKey, IV);
			damaged = encrypted.clone();

			damaged[damaged.length - DES_BLOCK_SIZE - 1] ^= 0x01;

			byte[] finalMasterSessionKey = masterSessionKey;
			byte[] finalDamaged = damaged;

			assertThrows(ArchiveException.class, () -> cipher.decryptRandomData(finalDamaged, finalMasterSessionKey, IV));
		} finally {
			RawUtils.clear(material);
			RawUtils.clear(masterSessionKey);
			RawUtils.clear(randomData);
			RawUtils.clear(encrypted);
			RawUtils.clear(damaged);
		}
	}

	@Test
	void strongCryptoEngineSupportsChunkedDESRoundTrip() throws Exception {
		EncryptionProvider cipher = EncryptionProvider.resolve(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES);

		byte[] material = "des-test-password".getBytes(StandardCharsets.UTF_8);

		byte[] sessionKey = null;
		byte[] plaintext = "DES strong encryption chunked input test".getBytes(StandardCharsets.UTF_8);

		byte[] encrypted = null;
		byte[] decrypted = null;

		try {
			sessionKey = cipher.deriveKey(material, 56);

			try (StrongCryptoEngine encryptEngine =
					     new StrongCryptoEngine(CryptoMode.ENCRYPT, cipher, cipher.encryptor(sessionKey, IV),
							     sessionKey, plaintext.length)) {
				encrypted = RawUtils.join(
						encryptEngine.update(plaintext, 0, 5),
						encryptEngine.update(plaintext, 5, 11),
						encryptEngine.update(plaintext, 16, plaintext.length - 16),
						encryptEngine.finish());
			}

			assertEquals(cipher.encryptedSize(plaintext.length), encrypted.length);

			try (StrongCryptoEngine decryptEngine =
					     new StrongCryptoEngine(CryptoMode.DECRYPT, cipher, cipher.decryptor(sessionKey, IV),
							     sessionKey, plaintext.length)) {
				decrypted = RawUtils.join(
						decryptEngine.update(encrypted, 0, 3),
						decryptEngine.update(encrypted, 3, 13),
						decryptEngine.update(encrypted, 16, encrypted.length - 16),
						decryptEngine.finish());
			}

			assertArrayEquals(plaintext, decrypted);
		} finally {
			RawUtils.clear(material);
			RawUtils.clear(sessionKey);
			RawUtils.clear(plaintext);
			RawUtils.clear(encrypted);
			RawUtils.clear(decrypted);
		}
	}

	private static byte[] hex(final String value) {
		if ((value.length() & 1) != 0) {
			throw new IllegalArgumentException("Hex value must contain an even number of characters");
		}
		byte[] result = new byte[value.length() / 2];
		for (int i = 0; i < result.length; i++) {
			int high = Character.digit(value.charAt(i * 2), 16);
			int low = Character.digit(value.charAt(i * 2 + 1), 16);
			if (high < 0 || low < 0) {
				throw new IllegalArgumentException("Invalid hexadecimal value");
			}
			result[i] = (byte) ((high << 4) | low);
		}
		return result;
	}
}