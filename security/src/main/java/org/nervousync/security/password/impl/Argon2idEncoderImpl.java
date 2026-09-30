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

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.nervousync.commons.Globals;
import org.nervousync.security.password.PasswordEncoder;
import org.nervousync.utils.core.StringUtils;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * <h2 class="en-US">Argon2id password encoder implement class</h2>
 * <h2 class="zh-CN">Argon2id 密码编码器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2026 11:23:13 $
 */
public final class Argon2idEncoderImpl implements PasswordEncoder {

	private static final int SALT_LENGTH = 16;
	private static final int HASH_LENGTH = 32;

	/**
	 * <span class="en-US">Usage memory size</span>
	 * <span class="zh-CN">使用内存大小</span>
	 */
	private final int memory;
	/**
	 * <span class="en-US">Iteration count</span>
	 * <span class="zh-CN">迭代次数</span>
	 */
	private final int iterations;
	/**
	 * <span class="en-US">Parallelism lanes</span>
	 * <span class="zh-CN">并行通道数量</span>
	 */
	private final int parallelism;

	/**
	 * <h3 class="en-US">Constructor method for the Argon2id password encoder implement class</h3>
	 * <h3 class="zh-CN">Argon2id 密码编码器实现类的构造方法</h3>
	 *
	 * @param memory      <span class="en-US">Usage memory size</span>
	 *                    <span class="zh-CN">使用内存大小</span>
	 * @param iterations  <span class="en-US">Iteration count</span>
	 *                    <span class="zh-CN">迭代次数</span>
	 * @param parallelism <span class="en-US">Parallelism lanes</span>
	 *                    <span class="zh-CN">并行通道数量</span>
	 */
	public Argon2idEncoderImpl(final int memory, final int iterations, final int parallelism) {
		this.memory = memory;
		this.iterations = iterations;
		this.parallelism = parallelism;
	}

	@Override
	public String encode(final char[] password) {
		byte[] salt = new byte[SALT_LENGTH];
		Globals.randomBytes(salt);
		byte[] hash = derive(password, salt, this.memory, this.iterations, this.parallelism);
		return "$argon2id$v=19$m=" + (this.memory * 1024) + ",t=" + this.iterations + ",p=" + this.parallelism
				+ "$" + StringUtils.base64Encode(salt, Boolean.FALSE)
				+ "$" + StringUtils.base64Encode(hash, Boolean.FALSE);
	}

	@Override
	public boolean verify(final char[] password, final String encodedPassword) {
		Config config = Config.parse(encodedPassword);
		byte[] hash = derive(password, config.salt, config.memory, config.iterations, config.parallelism);
		try {
			return MessageDigest.isEqual(config.hash, hash);
		} finally {
			Arrays.fill(hash, (byte) 0);
		}
	}

	@Override
	public boolean needsUpgrade(final String encodedPassword) {
		Config config = Config.parse(encodedPassword);
		return this.memory != config.memory
				|| this.iterations != config.iterations
				|| this.parallelism != config.parallelism;
	}

	private static byte[] derive(final char[] password, final byte[] salt,
	                             final int memory, final int iterations, final int parallelism) {
		Argon2Parameters parameters =
				new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
						.withVersion(Argon2Parameters.ARGON2_VERSION_13)
						.withMemoryAsKB(memory * 1024)
						.withIterations(iterations)
						.withParallelism(parallelism)
						.withSalt(salt)
						.build();
		Argon2BytesGenerator generator = new Argon2BytesGenerator();
		generator.init(parameters);

		byte[] result = new byte[HASH_LENGTH];
		generator.generateBytes(password, result);
		return result;
	}

	private record Config(int memory, int iterations, int parallelism, byte[] salt, byte[] hash) {

		static Config parse(final String encodedPassword) {
			String[] parts = StringUtils.tokenizeToStringArray(encodedPassword, "$");
			if (parts.length != 5 || !"argon2id".equalsIgnoreCase(parts[0])
					|| !"v=19".equalsIgnoreCase(parts[1])) {
				throw new IllegalArgumentException("Invalid Argon2id hash! " + encodedPassword);
			}
			String[] confParts = StringUtils.tokenizeToStringArray(parts[2], ",");
			if (confParts.length != 3) {
				throw new IllegalArgumentException("Invalid Argon2id hash! " + encodedPassword);
			}
			int memorySize = Integer.parseInt(confParts[0].substring(2));
			if (memorySize % 1024 != 0) {
				throw new IllegalArgumentException("Invalid Argon2id hash! " + encodedPassword);
			}
			return new Config(memorySize / 1024,
					Integer.parseInt(confParts[1].substring(2)), Integer.parseInt(confParts[2].substring(2)),
					StringUtils.base64Decode(parts[3]), StringUtils.base64Decode(parts[4]));
		}

	}
}
