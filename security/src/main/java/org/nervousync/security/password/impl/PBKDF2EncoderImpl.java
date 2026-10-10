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

import jakarta.annotation.Nonnull;
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
	private static final int ITERATION_MIN_LIMIT_SHA1 = 1300000;
	private static final int ITERATION_MAX_LIMIT_SHA1 = 3000000;
	private static final int ITERATION_MIN_LIMIT_SHA256 = 600000;
	private static final int ITERATION_MAX_LIMIT_SHA256 = 2000000;
	private static final int ITERATION_MIN_LIMIT_SHA512 = 210000;
	private static final int ITERATION_MAX_LIMIT_SHA512 = 1000000;

	/**
	 * <span class="en-US">Algorithm name</span>
	 * <span class="zh-CN">算法名称</span>
	 */
	private final Algorithm algorithm;
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
		switch (algorithm) {
			case SHA1:
				this.iterations = Math.min(Math.max(iterations, ITERATION_MIN_LIMIT_SHA1), ITERATION_MAX_LIMIT_SHA1);
				break;
			case SHA256:
				this.iterations = Math.min(Math.max(iterations, ITERATION_MIN_LIMIT_SHA256), ITERATION_MAX_LIMIT_SHA256);
				break;
			case SHA512:
				this.iterations = Math.min(Math.max(iterations, ITERATION_MIN_LIMIT_SHA512), ITERATION_MAX_LIMIT_SHA512);
				break;
			default:
				throw new IllegalArgumentException("Algorithm not supported: " + algorithm);
		}
		Config config = new Config(algorithm, iterations, keyLength(algorithm), new byte[0], new byte[0]);
		this.algorithm = config.algorithm;
		this.keyLength = config.keyLength;
	}

	@Override
	public String encode(final char[] password) {
		byte[] salt = new byte[SALT_LENGTH];
		Globals.randomBytes(salt);
		Config config = new Config(this.algorithm, this.iterations, this.keyLength, salt, new byte[0]);
		byte[] hash = this.derive(password, config);
		return config + "$" + StringUtils.base64Encode(hash, Boolean.FALSE);
	}

	@Override
	public boolean verify(final char[] password, final String encodedPassword) {
		Config config = Config.parse(encodedPassword);
		byte[] hash = this.derive(password, config);
		try {
			return MessageDigest.isEqual(config.hash, hash);
		} finally {
			Arrays.fill(hash, (byte) 0);
		}
	}

	@Override
	public boolean needsUpgrade(final String encodedPassword) {
		Config config = Config.parse(encodedPassword);
		return !this.algorithm.equals(config.algorithm) || config.iterations != this.iterations;
	}

	private byte[] derive(final char[] password, final Config config) {
		PBEKeySpec spec = new PBEKeySpec(password, config.salt, config.iterations, config.keyLength * 8);
		try {
			return SecretKeyFactory.getInstance(config.algorithmName()).generateSecret(spec).getEncoded();
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException("PBKDF2 is not available", e);
		} finally {
			spec.clearPassword();
		}
	}

	private static int keyLength(final Algorithm algorithm) {
		return switch (algorithm) {
			case SHA1 -> 16;
			case SHA256 -> 32;
			case SHA512 -> 64;
		};
	}

	public enum Algorithm {
		SHA1, SHA256, SHA512
	}

	private record Config(Algorithm algorithm, int iterations, int keyLength, byte[] salt, byte[] hash) {

		public String algorithmName() {
			return switch (algorithm) {
				case SHA1 -> "PBKDF2WithHmacSHA1";
				case SHA256 -> "PBKDF2WithHmacSHA256";
				case SHA512 -> "PBKDF2WithHmacSHA512";
			};
		}

		@Override
		@Nonnull
		public String toString() {
			String identify = switch (this.algorithm) {
				case SHA1 -> "sha1";
				case SHA256 -> "sha256";
				case SHA512 -> "sha512";
			};
			return "$pbkdf2-" + identify + "$i=" + this.iterations
					+ "$" + StringUtils.base64Encode(this.salt, Boolean.FALSE);
		}

		static Config parse(final String encodedPassword) {
			String[] parts = StringUtils.tokenizeToStringArray(encodedPassword, "$");
			if (parts.length != 4 || !StringUtils.startsWithIgnoreCase(parts[0], "pbkdf2-")
					|| !StringUtils.startsWithIgnoreCase(parts[1], "i=")) {
				throw new IllegalArgumentException("Invalid PBKDF2 hash! " + encodedPassword);
			}
			String string = parts[0].substring("pbkdf2-".length()).toUpperCase();
			Algorithm algorithm = switch (string) {
				case "SHA1" -> Algorithm.SHA1;
				case "SHA256" -> Algorithm.SHA256;
				case "SHA512" -> Algorithm.SHA512;
				default -> throw new IllegalArgumentException("Algorithm not supported: " + string);
			};
			int iterations = Integer.parseInt(parts[1].substring(2));
			return new Config(algorithm, iterations, PBKDF2EncoderImpl.keyLength(algorithm),
					StringUtils.base64Decode(parts[2]), StringUtils.base64Decode(parts[3]));
		}
	}
}
