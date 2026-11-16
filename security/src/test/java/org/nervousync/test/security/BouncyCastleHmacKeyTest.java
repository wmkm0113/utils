package org.nervousync.test.security;

import org.junit.jupiter.api.Test;
import org.nervousync.utils.security.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class BouncyCastleHmacKeyTest {

	@Test
	void hmacSha1MatchesRfc2202Vector() throws Exception {
		byte[] key = new byte[20];
		Arrays.fill(key, (byte) 0x0b);
		byte[] message = "Hi There".getBytes(StandardCharsets.US_ASCII);
		byte[] expected = {
				(byte) 0xb6, 0x17, 0x31, (byte) 0x86, 0x55,
				0x05, 0x72, 0x64, (byte) 0xe2, (byte) 0x8b,
				(byte) 0xc0, (byte) 0xb6, (byte) 0xfb, 0x37, (byte) 0x8c,
				(byte) 0x8e, (byte) 0xf1, 0x46, (byte) 0xbe, 0x00
		};

		assertArrayEquals(expected, DigestUtils.HmacSHA1(key).finish(message));
	}

	@Test
	void hmacSha1ResultDependsOnKey() throws Exception {
		byte[] message = "same-message".getBytes(StandardCharsets.US_ASCII);
		byte[] first = DigestUtils.HmacSHA1("first-key".getBytes(StandardCharsets.US_ASCII)).finish(message);
		byte[] second = DigestUtils.HmacSHA1("second-key".getBytes(StandardCharsets.US_ASCII)).finish(message);

		assertFalse(Arrays.equals(first, second), "HMAC output must change when the key changes");
	}
}
