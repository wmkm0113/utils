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

package org.nervousync.utils.password;

import org.nervousync.security.password.PasswordEncoder;
import org.nervousync.security.password.impl.Argon2idEncoderImpl;
import org.nervousync.security.password.impl.BCryptEncoderImpl;
import org.nervousync.security.password.impl.PBKDF2EncoderImpl;

import java.util.Optional;

/**
 * <h2 class="en-US">Password-Authenticated Key Exchange Utilities</h2>
 * <h2 class="zh-CN">密码验证工具</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Sep 30, 2026 10:47:28 $
 */
public final class PasswordUtils {

	/**
	 * <span class="en-US">Password encoder singleton instance object</span>
	 * <span class="zh-CN">密码验证工具单例实例对象</span>
	 */
	private static PasswordUtils INSTANCE = null;

	/**
	 * <span class="en-US">Password encoder instance object</span>
	 * <span class="zh-CN">密码编码器实例对象</span>
	 */
	private final PasswordEncoder encoder;

	/**
	 * <h3 class="en-US">Private constructor method for the Password-Authenticated Key Exchange Utilities</h3>
	 * <h3 class="zh-CN">密码验证工具的私有构造方法</h3>
	 *
	 * @param cost <span class="en-US">The cost factor</span>
	 *             <span class="zh-CN">成本因子</span>
	 */
	private PasswordUtils(final int cost) {
		this.encoder = new BCryptEncoderImpl(cost);
	}

	/**
	 * <h3 class="en-US">Private constructor method for the Password-Authenticated Key Exchange Utilities</h3>
	 * <h3 class="zh-CN">密码验证工具的私有构造方法</h3>
	 *
	 * @param memory      <span class="en-US">Usage memory size</span>
	 *                    <span class="zh-CN">使用内存大小</span>
	 * @param iterations  <span class="en-US">Iteration count</span>
	 *                    <span class="zh-CN">迭代次数</span>
	 * @param parallelism <span class="en-US">Parallelism lanes</span>
	 *                    <span class="zh-CN">并行通道数量</span>
	 */
	private PasswordUtils(final int memory, final int iterations, final int parallelism) {
		this.encoder = new Argon2idEncoderImpl(memory, iterations, parallelism);
	}

	/**
	 * <h3 class="en-US">Private constructor method for the Password-Authenticated Key Exchange Utilities</h3>
	 * <h3 class="zh-CN">密码验证工具的私有构造方法</h3>
	 *
	 * @param algorithm  <span class="en-US">Algorithm name</span>
	 *                   <span class="zh-CN">算法名称</span>
	 * @param iterations <span class="en-US">Algorithm name</span>
	 *                   <span class="zh-CN">算法名称</span>
	 */
	private PasswordUtils(final PBKDF2EncoderImpl.Algorithm algorithm, final int iterations) {
		this.encoder = new PBKDF2EncoderImpl(algorithm, iterations);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the Password-Authenticated Key Exchange Utilities</h3>
	 * <h3 class="zh-CN">初始化密码验证工具的静态方法</h3>
	 *
	 * @param cost <span class="en-US">The cost factor</span>
	 *             <span class="zh-CN">成本因子</span>
	 */
	public static void initialize(final int cost) {
		if (INSTANCE == null) {
			INSTANCE = new PasswordUtils(cost);
		} else {
			throw new IllegalStateException("PasswordUtils already initialized");
		}
	}

	/**
	 * <h3 class="en-US">Static method for initialize the Password-Authenticated Key Exchange Utilities</h3>
	 * <h3 class="zh-CN">初始化密码验证工具的静态方法</h3>
	 *
	 * @param memory      <span class="en-US">Usage memory size</span>
	 *                    <span class="zh-CN">使用内存大小</span>
	 * @param iterations  <span class="en-US">Iteration count</span>
	 *                    <span class="zh-CN">迭代次数</span>
	 * @param parallelism <span class="en-US">Parallelism lanes</span>
	 *                    <span class="zh-CN">并行通道数量</span>
	 */
	public static void initialize(final int memory, final int iterations, final int parallelism) {
		if (INSTANCE == null) {
			INSTANCE = new PasswordUtils(memory, iterations, parallelism);
		} else {
			throw new IllegalStateException("PasswordUtils already initialized");
		}
	}

	/**
	 * <h3 class="en-US">Static method for initialize the Password-Authenticated Key Exchange Utilities</h3>
	 * <h3 class="zh-CN">初始化密码验证工具的静态方法</h3>
	 *
	 * @param algorithm  <span class="en-US">Algorithm name</span>
	 *                   <span class="zh-CN">算法名称</span>
	 * @param iterations <span class="en-US">Algorithm name</span>
	 *                   <span class="zh-CN">算法名称</span>
	 */
	public static void initialize(final PBKDF2EncoderImpl.Algorithm algorithm, final int iterations) {
		if (INSTANCE == null) {
			INSTANCE = new PasswordUtils(algorithm, iterations);
		} else {
			throw new IllegalStateException("PasswordUtils already initialized");
		}
	}

	/**
	 * <h3 class="en-US">Static method for destroy the Password-Authenticated Key Exchange Utilities</h3>
	 * <h3 class="zh-CN">销毁当前密码验证工具的静态方法</h3>
	 */
	public static void destroy() {
		if (INSTANCE != null) {
			INSTANCE = null;
		}
	}

	/**
	 * <h3 class="en-US">Encode the given password</h3>
	 * <h3 class="zh-CN">对给定的密码进行编码</h3>
	 *
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @return <span class="en-US">Encoded string</span>
	 * <span class="zh-CN">编码后的字符串</span>
	 */
	public static String encode(final String password) throws IllegalArgumentException {
		return Optional.ofNullable(INSTANCE)
				.map(instance -> instance.encoder.encode(password.toCharArray()))
				.orElseThrow(() -> new IllegalArgumentException("Password utility not initialized"));
	}

	/**
	 * <h3 class="en-US">Verify the given password is match the given encoded password</h3>
	 * <h3 class="zh-CN">验证给定的密码是否与给定的编码一致</h3>
	 *
	 * @param password        <span class="en-US">Password</span>
	 *                        <span class="zh-CN">密码</span>
	 * @param encodedPassword <span class="en-US">Encoded string</span>
	 *                        <span class="zh-CN">编码后的字符串</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	public static boolean verify(final String password, final String encodedPassword) {
		return Optional.ofNullable(INSTANCE)
				.map(instance -> instance.encoder.verify(password.toCharArray(), encodedPassword))
				.orElseThrow(() -> new IllegalArgumentException("Password utility not initialized"));
	}

	/**
	 * <h3 class="en-US">Check the given encoded string needs upgrade</h3>
	 * <h3 class="zh-CN">检查给定的编码是否需要升级</h3>
	 *
	 * @param encodedPassword <span class="en-US">Encoded string</span>
	 *                        <span class="zh-CN">编码后的字符串</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean needsUpgrade(final String encodedPassword) {
		return Optional.ofNullable(INSTANCE)
				.map(instance -> instance.encoder.needsUpgrade(encodedPassword))
				.orElseThrow(() -> new IllegalArgumentException("Password utility not initialized"));
	}
}
