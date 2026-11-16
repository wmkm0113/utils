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
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.enumerations.AesVendor;
import org.nervousync.archive.options.ArchiveLimit;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.crypto.aes.AesInputStream;
import org.nervousync.archive.zip.crypto.aes.AesOutputStream;
import org.nervousync.archive.zip.crypto.standard.StandardInputStream;
import org.nervousync.archive.zip.crypto.standard.StandardOutputStream;
import org.nervousync.archive.zip.crypto.strong.StrongInputStream;
import org.nervousync.archive.zip.crypto.strong.StrongOutputStream;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.archive.zip.models.header.StrongDecryptionHeader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

final class CryptoOutputStreamWriteTest {

	private static final String PASSWORD = "crypto-output-test-password";
	private static final byte[] PASSWORD_BYTES = PASSWORD.getBytes(StandardCharsets.UTF_8);
	private static final int[] LENGTHS = {0, 1, 15, 16, 17, 64};

	@ParameterizedTest(name = "ZipCrypto payload length {0}")
	@ValueSource(ints = {0, 1, 15, 16, 17, 64})
	void standardOutputIncludesHeaderAndRoundTrips(final int length) throws Exception {
		byte[] plain = payload(length);
		ByteArrayOutputStream stored = new ByteArrayOutputStream();
		try (StandardOutputStream output = new StandardOutputStream(stored, PASSWORD_BYTES, 0x12345678)) {
			writeMixed(output, plain);
		}
		byte[] encrypted = stored.toByteArray();
		assertEquals(12 + length, encrypted.length);
		ByteArrayInputStream source = new ByteArrayInputStream(encrypted);
		byte[] header = source.readNBytes(12);
		try (StandardInputStream input = new StandardInputStream(source, length, PASSWORD_BYTES, header, 0x12)) {
			assertArrayEquals(plain, input.readAllBytes());
		}
	}

	private static Stream<Arguments> aesCases() {
		return Stream.of(1, 2, 3).flatMap(strength -> Arrays.stream(LENGTHS)
				.mapToObj(length -> Arguments.of(strength, length)));
	}

	@ParameterizedTest(name = "AES strength {0}, payload length {1}")
	@MethodSource("aesCases")
	void aesOutputIncludesEnvelopeAndAuthentication(final int strength, final int length) throws Exception {
		byte[] plain = payload(length);
		byte[] encrypted = encryptAes(plain, strength);
		int saltLength = strength == 1 ? 8 : strength == 2 ? 12 : 16;
		assertEquals(saltLength + 2 + length + 10, encrypted.length);
		try (AesInputStream input = new AesInputStream(new ByteArrayInputStream(encrypted),
				new AesEncryptionInfo(AesVendor.AE_2, strength), encrypted.length, PASSWORD_BYTES)) {
			assertArrayEquals(plain, input.readAllBytes());
		}
	}

	@ParameterizedTest(name = "AES strength {0} uses fresh salt")
	@ValueSource(ints = {1, 2, 3})
	void aesUsesDifferentSaltForRepeatedEncryption(final int strength) throws Exception {
		byte[] first = encryptAes(payload(32), strength);
		byte[] second = encryptAes(payload(32), strength);
		int saltLength = strength == 1 ? 8 : strength == 2 ? 12 : 16;
		assertFalse(Arrays.equals(Arrays.copyOf(first, saltLength), Arrays.copyOf(second, saltLength)),
				"Repeated encryption with the same password must not reuse a fixed salt");
	}

	private static Stream<Arguments> strongCases() {
		int[][] choices = {
				{0x6601, 56}, {0x6601, 64}, {0x6609, 112}, {0x6603, 168},
				{0x660E, 128}, {0x660F, 192}, {0x6610, 256},
				{0x6702, 40}, {0x6702, 64}, {0x6702, 128},
				{0x6801, 40}, {0x6801, 64}, {0x6801, 128},
				{0x6720, 32}, {0x6720, 128}, {0x6720, 320},
				{0x6721, 128}, {0x6721, 192}, {0x6721, 256}
		};
		return Arrays.stream(choices).flatMap(choice -> Arrays.stream(LENGTHS)
				.mapToObj(length -> Arguments.of(String.format("0x%04X", choice[0]), choice[0], choice[1], length)));
	}

	@ParameterizedTest(name = "Strong {0}, {2} bits, payload length {3}")
	@MethodSource("strongCases")
	void strongOutputContinuesCipherStateAfterValidationBlock(final String name, final int algorithm,
	                                                        final int keyBits, final int length) throws Exception {
		byte[] plain = payload(length);
		ByteArrayOutputStream stored = new ByteArrayOutputStream();
		try (StrongOutputStream output = new StrongOutputStream(stored, algorithm, keyBits, PASSWORD_BYTES)) {
			writeMixed(output, plain);
		}
		byte[] encrypted = stored.toByteArray();
		ByteArrayInputStream source = new ByteArrayInputStream(encrypted);
		StrongDecryptionHeader header = ZipCrypto.readStrongDecryptionHeader(new DataInputStream(source), ArchiveLimit.DEFAULT_LIMIT.getMaxHeaderBytes());
		try (StrongInputStream input = StrongInputStream.entry("archive.zip", "entry", source, encrypted.length,
				PasswordProvider.fixed(PASSWORD.toCharArray()), null,
				new StrongEncryptionInfo(algorithm, keyBits, 1, new byte[0]), header)) {
			assertArrayEquals(plain, input.readAllBytes(), name);
		}
	}

	@Test
	void finishIsIdempotentAndLeavesSinkOpenForFollowingZipRecords() throws Exception {
		TrackingSink stored = new TrackingSink();
		AesOutputStream output = new AesOutputStream(stored, 3, PASSWORD_BYTES);
		try (output) {
			output.write(payload(17));
			output.finish();
			int completedLength = stored.size();
			output.finish();
			assertEquals(completedLength, stored.size());
			assertFalse(stored.closed);
			assertThrows(IOException.class, () -> output.write(1));
		}
		assertTrue(stored.closed);
		assertDoesNotThrow(output::close);
	}

	private static byte[] encryptAes(final byte[] plain, final int strength) throws Exception {
		ByteArrayOutputStream stored = new ByteArrayOutputStream();
		try (AesOutputStream output = new AesOutputStream(stored, strength, PASSWORD_BYTES)) {
			writeMixed(output, plain);
		}
		return stored.toByteArray();
	}

	private static void writeMixed(final OutputStream output, final byte[] bytes) throws IOException {
		output.write(new byte[0]);
		if (bytes.length > 0) {
			output.write(bytes[0] & 0xFF);
			int firstChunk = (bytes.length - 1) / 2;
			output.write(bytes, 1, firstChunk);
			output.write(bytes, 1 + firstChunk, bytes.length - 1 - firstChunk);
		}
	}

	private static byte[] payload(final int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = (byte) (i + 1);
		}
		return bytes;
	}

	private static final class TrackingSink extends ByteArrayOutputStream {
		private boolean closed;

		@Override
		public void close() throws IOException {
			this.closed = true;
			super.close();
		}
	}
}
