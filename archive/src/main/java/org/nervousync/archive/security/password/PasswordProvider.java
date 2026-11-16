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

package org.nervousync.archive.security.password;

import java.util.Objects;
import java.util.Optional;

/**
 * <h2 class="en-US">Password provider</h2>
 * <h2 class="zh-CN">密码提供适配器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
@FunctionalInterface
public interface PasswordProvider {

	/**
	 * <h3 class="en-US">Initialize a password credential instance object via a password request.</h3>
	 * <h3 class="zh-CN">通过密码请求初始化密码凭证实例对象</h3>
	 *
	 * @param request <span class="en-US">Password request</span>
	 *                <span class="zh-CN">密码请求</span>
	 * @return <span class="en-US">Password provider instance object</span>
	 * <span class="zh-CN">密码提供适配器实例对象</span>
	 */
	Optional<PasswordCredentials> credentials(final PasswordRequest request);

	/**
	 * <h3 class="en-US">Static method for initialize the non-password provision adapter</h3>
	 * <h3 class="zh-CN">静态方法用于初始化无密码提供适配器</h3>
	 *
	 * @return <span class="en-US">Password provider instance object</span>
	 * <span class="zh-CN">密码提供适配器实例对象</span>
	 */
	static PasswordProvider none() {
		return request -> Optional.empty();
	}

	/**
	 * <h3 class="en-US">Static method for initialize the non-interactive password provision adapter</h3>
	 * <h3 class="zh-CN">静态方法用于初始化非交互式的密码提供适配器</h3>
	 *
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @return <span class="en-US">Password provider instance object</span>
	 * <span class="zh-CN">密码提供适配器实例对象</span>
	 */
	static PasswordProvider fixed(final char[] password) {
		return fixed(password, false);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the non-interactive password provision adapter</h3>
	 * <h3 class="zh-CN">静态方法用于初始化非交互式的密码提供适配器</h3>
	 *
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @param utf8Encoding <span class="en-US">Using UTF-8 encoding flag</span>
	 *                     <span class="zh-CN">使用 UTF-8 编码标记</span>
	 * @return <span class="en-US">Password provider instance object</span>
	 * <span class="zh-CN">密码提供适配器实例对象</span>
	 */
	static PasswordProvider fixed(final char[] password, final boolean utf8Encoding) {
		Objects.requireNonNull(password, "Password");

		final char[] storedPassword = password.clone();
		return request -> Optional.of(PasswordCredentials.of(storedPassword, utf8Encoding));
	}
}
