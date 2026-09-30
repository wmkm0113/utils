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

package org.nervousync.security.password.impl;

import org.nervousync.commons.Globals;
import org.nervousync.security.password.PasswordEncoder;
import org.nervousync.utils.core.StringUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * <h2 class="en-US">PBKDF2 password encoder implement class</h2>
 * <h2 class="zh-CN">PBKDF2密码编码器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2026 11:23:13 $
 */
public final class PBKDF2EncoderImpl implements PasswordEncoder {

	private static final int SALT_LENGTH = 16;  //  128 bit

	/**
	 * <span class="en-US">Algorithm name</span>
	 * <span class="zh-CN">算法名称</span>
	 */
	private final String algorithm;
	/**
	 * <span class="en-US">Hash name</span>
	 * <span class="zh-CN">哈希名称</span>
	 */
	private final String identify;
	/**
	 * <span class="en-US">Iteration count</span>
	 * <span class="zh-CN">迭代次数</span>
	 */
	private final int iterations;
	/**
	 * <span class="en-US">Derived key length</span>
	 * <span class="zh-CN">派生密钥长度</span>
	 */
	private final int keyLength;

	/**
	 * <h3 class="en-US">Constructor method for the PBKDF2 password encoder implement class</h3>
	 * <h3 class="zh-CN">PBKDF2密码编码器实现类的构造方法</h3>
	 *
	 * @param algorithm  <span class="en-US">Algorithm name</span>
	 *                   <span class="zh-CN">算法名称</span>
	 * @param iterations <span class="en-US">Iteration count</span>
	 *                   <span class="zh-CN">迭代次数</span>
	 */
	public PBKDF2EncoderImpl(final Algorithm algorithm, final int iterations) {
		if (iterations <= 0) {
			throw new IllegalArgumentException("Iterations must be positive");
		}
		this.iterations = iterations;
		switch (algorithm) {
			case SHA1:
				this.algorithm = "PBKDF2WithHmacSHA1";
				this.identify = "sha1";
				this.keyLength = 16;
				break;
			case SHA256:
				this.algorithm = "PBKDF2WithHmacSHA256";
				this.identify = "sha256";
				this.keyLength = 32;
				break;
			case SHA512:
				this.algorithm = "PBKDF2WithHmacSHA512";
				this.identify = "sha512";
				this.keyLength = 64;
				break;
			default:
				throw new IllegalArgumentException("Algorithm not supported: " + algorithm);
		}
	}

	@Override
	public String encode(final char[] password) {
		byte[] salt = new byte[SALT_LENGTH];
		Globals.randomBytes(salt);
		byte[] hash = this.derive(password, salt, this.algorithm, this.keyLength, this.iterations);
		return "$pbkdf2-" + this.identify + "$i=" + this.iterations
				+ "$" + StringUtils.base64Encode(salt, Boolean.FALSE)
				+ "$" + StringUtils.base64Encode(hash, Boolean.FALSE);
	}

	@Override
	public boolean verify(final char[] password, final String encodedPassword) {
		Config config = Config.parse(encodedPassword);
		byte[] hash = this.derive(password, config.salt, config.algorithm, config.keyLength, config.iterations);
		try {
			return MessageDigest.isEqual(config.hash, hash);
		} finally {
			Arrays.fill(hash, (byte) 0);
		}
	}

	@Override
	public boolean needsUpgrade(final String encodedPassword) {
		Config config = Config.parse(encodedPassword);
		return !this.algorithm.equalsIgnoreCase(config.algorithm) || config.iterations != this.iterations;
	}

	private byte[] derive(final char[] password, final byte[] salt, final String algorithm,
	                      final int keyLength, final int iterations) {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength * 8);
		try {
			return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException("PBKDF2 is not available", e);
		} finally {
			spec.clearPassword();
		}
	}

	public enum Algorithm {
		SHA1, SHA256, SHA512
	}

	private static final class Config {

		private final String algorithm;
		private final int iterations;
		private final int keyLength;
		private final byte[] salt;
		private final byte[] hash;

		Config(final String algorithm, final int iterations, final int keyLength, final byte[] salt, final byte[] hash) {
			this.algorithm = algorithm;
			this.iterations = iterations;
			this.keyLength = keyLength;
			this.salt = salt;
			this.hash = hash;
		}

		static Config parse(final String encodedPassword) {
			String[] parts = StringUtils.tokenizeToStringArray(encodedPassword, "$");
			if (parts.length != 4 || !StringUtils.startsWithIgnoreCase(parts[0], "pbkdf2-")
					|| !StringUtils.startsWithIgnoreCase(parts[1], "i=")) {
				throw new IllegalArgumentException("Invalid PBKDF2 hash! " + encodedPassword);
			}
			String algorithm = parts[0].substring("pbkdf2-".length()).toUpperCase();
			int keyLength;
			switch (algorithm) {
				case "SHA1":
					keyLength = 16;
					break;
				case "SHA256":
					keyLength = 32;
					break;
				case "SHA512":
					keyLength = 64;
					break;
				default:
					throw new IllegalArgumentException("Algorithm not supported: " + algorithm);
			}
			return new Config("PBKDF2WithHmac" + algorithm,
					Integer.parseInt(parts[1].substring(2)), keyLength,
					StringUtils.base64Decode(parts[2]), StringUtils.base64Decode(parts[3]));
		}
	}
}
