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

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.nervousync.commons.Globals;
import org.nervousync.security.password.PasswordEncoder;
import org.nervousync.utils.core.StringUtils;

import java.util.Arrays;

/**
 * <h2 class="en-US">BCrypt password encoder implement class</h2>
 * <h2 class="zh-CN">BCrypt 密码编码器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2026 11:23:13 $
 */
public final class BCryptEncoderImpl implements PasswordEncoder {

	private static final int SALT_LENGTH = 16;
	private static final int DEFAULT_COST = 12;

	/**
	 * <span class="en-US">The cost factor</span>
	 * <span class="zh-CN">成本因子</span>
	 */
	private final int cost;

	/**
	 * <h3 class="en-US">Constructor method for the BCrypt password encoder implement class</h3>
	 * <h3 class="zh-CN">BCrypt 密码编码器实现类的构造方法</h3>
	 *
	 * @param cost <span class="en-US">The cost factor</span>
	 *             <span class="zh-CN">成本因子</span>
	 */
	public BCryptEncoderImpl(final int cost) {
		this.cost = (cost <= 0) ? DEFAULT_COST : cost;
	}

	@Override
	public String encode(final char[] password) {
		byte[] salt = new byte[SALT_LENGTH];
		Globals.randomBytes(salt);

		try {
			return OpenBSDBCrypt.generate("2b", password, salt, this.cost);
		} finally {
			Arrays.fill(salt, (byte) 0);
		}
	}

	@Override
	public boolean verify(final char[] password, final String encodedPassword) {
		return OpenBSDBCrypt.checkPassword(encodedPassword, password);
	}

	@Override
	public boolean needsUpgrade(final String encodedPassword) {
		if (!StringUtils.startsWithIgnoreCase(encodedPassword, "$2b")) {
			return Boolean.TRUE;
		}
		if (!StringUtils.startsWithIgnoreCase(encodedPassword, ("$2b$" + this.cost))) {
			return Boolean.TRUE;
		}
		return false;
	}
}
