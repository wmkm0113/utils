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

package org.nervousync.test.utils;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.CertificateUtils;
import org.nervousync.utils.RawUtils;
import org.nervousync.utils.SecurityUtils;
import org.nervousync.utils.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public final class StringTest extends BaseTest {

	private static final byte[] BYTE_ARRAY = new byte[8];

	static {
		RawUtils.writeLong(BYTE_ARRAY, 1303315200000L);
	}

	@Test
	public void sha() {
		byte[] result = SecurityUtils.SHA256("");
		StringBuilder builder = new StringBuilder();
		for (byte b : result) {
			builder.append(", ").append(b);
		}
		System.out.println(builder.substring(1));
		System.out.println("[-29, -80, -60, 66, -104, -4, 28, 20, -102, -5, -12, -56, -103, 111, -71, 36, 39, -82, 65, -28, 100, -101, -109, 76, -92, -107, -103, 27, 120, 82, -72, 85]");
	}

	@Test
	public void bytes() {
		PrivateKey privateKey = CertificateUtils.privateKey("RSA", StringUtils.base64Decode("MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJsniijupzAbLFianCHsskjRWEJ9uQMb0An8vt06vgK14OJCS2DZRQKRqzBV/hjg6UYCIy2cFCX5ybll/p6m64tj5jzmoEv6zgsXrk5aMfjtHYkT0FCCvdF8Nk+bxh7EmFw59ANnUcW+oEZV3Wov7qIzcH774K1FHg/usjhdu/w7AgMBAAECgYASbZfTVMU2yYNy4qo4vUxPqc252ATpgZwsE4D39coruD6FoSoizquLqpGSpCQSAGS0k/xppmgeOpTjGGItpdXEASgA15SUbdATbteS8KllBGwQvYcc5cQ7SfGCS9nY+ZMeCm/kGG+b88wyceXGvz9v99/ZYHpAbok8LmTIjogxwQJBANJi6yUMDNC7TdmejlxyzM2SaCeeXshyDKoOg8u3H1ilQ7feKBc2EmZoGp/F7r+vDISbhMFd/O68yn3NN7dGyA8CQQC8yxS60rvL/zqD8r/7wzH7ucqSe0U7+BEuWNtsNLnZh3+5qxW2EhWiJYxVQyLez8qW5T+tXhZlUnK1Abcinj0VAkA9THME9VDalGhnaspB53UCxJCyUnN9ZbWI6ve9qFpqwqRLc2As+yU7T0PKn6ojkYZNMN7qVE845Cr/ooaEUQxDAkBdWjXX41gFGSFAGYqg3PYcONRX9ihX7OPh/QIS0UeMrpmTn2tO6kZUNSjdCCN39VuBnb4M5ddBJrGdm0mJDqG5AkBSBnm1qtTXs5c96yHNatXC1a7Ora87yBoXig5fIZacbqgmxL7sBBnJkgul0IPo6MfOeGpNRar1v0BbJQTCHiHW"));
		PublicKey publicKey = CertificateUtils.publicKey("RSA", StringUtils.base64Decode("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCbJ4oo7qcwGyxYmpwh7LJI0VhCfbkDG9AJ/L7dOr4CteDiQktg2UUCkaswVf4Y4OlGAiMtnBQl+cm5Zf6epuuLY+Y85qBL+s4LF65OWjH47R2JE9BQgr3RfDZPm8YexJhcOfQDZ1HFvqBGVd1qL+6iM3B+++CtRR4P7rI4Xbv8OwIDAQAB"));
		System.out.println(((RSAPublicKey) publicKey).getPublicExponent().toString(16));
		KeyPair keyPair = SecurityUtils.RSAKeyPair(1024);
		System.out.println(StringUtils.base64Encode(keyPair.getPublic().getEncoded()));
		System.out.println(StringUtils.base64Encode(keyPair.getPrivate().getEncoded()));
		System.out.println(((RSAPrivateKey)keyPair.getPrivate()).getPrivateExponent().toString(16));
		System.out.println(((RSAPrivateKey)keyPair.getPrivate()).getModulus().toString(16));
//		PublicKey publicKey = CertificateUtils.publicKey("RSA", StringUtils.base64Decode("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDQXWw50+7YgosqR3EhuYwwpJeMnLGNjHdJanXBqTenP0QshBPrqFFOerIO+vHxtrtn+8zu7ja4dpIQXtv+uFjJfjunSfLYIE5AkfXoI5D4pAptpV0Y4/6afZshgI+8uNNT3De2vLbrqjbqMPDbAowPkBHDEVRTE8GcWbmYYb4jYQIDAQAB"));
		byte[] result = SecurityUtils.RSADecryptor("OAEPWithSHA-256AndMGF1Padding", publicKey).finish(StringUtils.base64Decode("G7n6AaA/6HpDz22+Y+Fv0SxcGmXCEC61I/3MdSIBz6CNwXEceH4Oi4P0mLcIERlrxCwcrBofcvczWDcJ4Bf/2PGDY8SxDo4f0JQcP8mfHDKwZJMrfOfnqICqb63KmQ0buPw/9/gJ1MBQoYznIeufDglSw6+edtt6nXDo2kwaBJaZcye36wNOwXz9gElJPzzkYR3IRfGxplNEbjx6ZmOiEcvFjj9gxVS1D1H7VzNKWXrAghxkdmq+rGoji0J5aKplD2UhVMW3lY/6+3xeyxVP7OfpY4ArIFI4tzQcUm8bxV/3N4pdcl1NbdQDH4Tw659ocnjPSMMSPXGRSKUpcJd+NJoT0yJ4Xxlhf0qOU6gyISkgEuDWJZEhFeRjimUXmgmo9p+XE7HZQMeFcQuKqDyxSTRzuu7geDtikFl7Bgnx1AuAxyFGKiS/ZN+Z0EYMWNHGsgTWBVMcTkKjl2zrfG4uj8DW+RxmYKrA7nj8rdylfjL2g9vuASoklDp1Xqa+VvED"));
		byte[] original = "Test测试TestTest测试TestTestTest测试TestTestTestTest测试TestTestTestTestTest测试TestTestTestTestTestTest测试TestTestTestTestTestTestTest测试TestTestTestTestTest测试".getBytes(StandardCharsets.UTF_8);
//		byte[] result = SecurityUtils.RSAEncryptor("PKCS1Padding", publicKey).finish(original);
		System.out.println(StringUtils.base64Encode(result));
		byte[] dec = SecurityUtils.RSADecryptor("PKCS1Padding", privateKey).finish(result);
		System.out.println(new String(dec));
		System.out.println("-----BEGIN PRIVATE KEY-----");
		System.out.println("MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAJsniijupzAbLFianCHsskjRWEJ9uQMb0An8vt06vgK14OJCS2DZRQKRqzBV/hjg6UYCIy2cFCX5ybll/p6m64tj5jzmoEv6zgsXrk5aMfjtHYkT0FCCvdF8Nk+bxh7EmFw59ANnUcW+oEZV3Wov7qIzcH774K1FHg/usjhdu/w7AgMBAAECgYASbZfTVMU2yYNy4qo4vUxPqc252ATpgZwsE4D39coruD6FoSoizquLqpGSpCQSAGS0k/xppmgeOpTjGGItpdXEASgA15SUbdATbteS8KllBGwQvYcc5cQ7SfGCS9nY+ZMeCm/kGG+b88wyceXGvz9v99/ZYHpAbok8LmTIjogxwQJBANJi6yUMDNC7TdmejlxyzM2SaCeeXshyDKoOg8u3H1ilQ7feKBc2EmZoGp/F7r+vDISbhMFd/O68yn3NN7dGyA8CQQC8yxS60rvL/zqD8r/7wzH7ucqSe0U7+BEuWNtsNLnZh3+5qxW2EhWiJYxVQyLez8qW5T+tXhZlUnK1Abcinj0VAkA9THME9VDalGhnaspB53UCxJCyUnN9ZbWI6ve9qFpqwqRLc2As+yU7T0PKn6ojkYZNMN7qVE845Cr/ooaEUQxDAkBdWjXX41gFGSFAGYqg3PYcONRX9ihX7OPh/QIS0UeMrpmTn2tO6kZUNSjdCCN39VuBnb4M5ddBJrGdm0mJDqG5AkBSBnm1qtTXs5c96yHNatXC1a7Ora87yBoXig5fIZacbqgmxL7sBBnJkgul0IPo6MfOeGpNRar1v0BbJQTCHiHW");
		System.out.println("-----END PRIVATE KEY-----");
	}

	private static void print(final byte[] dataBytes) {
		System.out.println(dataBytes.length);
		StringBuilder stringBuilder = new StringBuilder();
		for (byte b : dataBytes) {
			String hex = Integer.toString(b < 0 ? b + 256 : b, 16);
			if (hex.length() == 1) {
				stringBuilder.append('0');
			}
			stringBuilder.append(hex);
		}
		System.out.println(stringBuilder);
	}

	@Test
	@Order(10)
	public void base32() {
		String string = StringUtils.base32Encode(BYTE_ARRAY);
		this.logger.info("String_Encode", "Base32", string);
		byte[] decodeBytes = StringUtils.base32Decode(string);
		this.logger.info("String_Decode", "Base32", RawUtils.readLong(decodeBytes) == 1303315200000L);
	}

	@Test
	@Order(20)
	public void base64() {
		String string = StringUtils.base64Encode(BYTE_ARRAY);
		this.logger.info("String_Encode", "Base64", string);
		byte[] decodeBytes = StringUtils.base64Decode(string);
		this.logger.info("String_Decode", "Base64", RawUtils.readLong(decodeBytes) == 1303315200000L);
	}
}
