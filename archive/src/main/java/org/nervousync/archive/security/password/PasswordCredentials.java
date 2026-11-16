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

import org.nervousync.archive.security.SecurityCredentials;

import javax.security.auth.Destroyable;
import java.util.Arrays;
import java.util.Objects;

/**
 * <h2 class="en-US">Password credentials</h2>
 * <h2 class="zh-CN">密码凭据</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class PasswordCredentials extends SecurityCredentials implements Destroyable, AutoCloseable {

	/**
	 * <span class="en-US">Password</span>
	 * <span class="zh-CN">密码</span>
	 */
	private char[] password;
	/**
	 * <span class="en-US">Using UTF-8 encoding flag</span>
	 * <span class="zh-CN">使用 UTF-8 编码标记</span>
	 */
	private final boolean utf8Encoding;
	/**
	 * <span class="en-US">Destroyed flag</span>
	 * <span class="zh-CN">销毁标记</span>
	 */
	private volatile boolean destroyed;

	/**
	 * <h3 class="en-US">Constructor method for the password credentials</h3>
	 * <h3 class="zh-CN">密码凭据的构造方法</h3>
	 *
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @param utf8Encoding <span class="en-US">Using UTF-8 encoding flag</span>
	 *                     <span class="zh-CN">使用 UTF-8 编码标记</span>
	 */
	private PasswordCredentials(final char[] password, final boolean utf8Encoding) {
		Objects.requireNonNull(password, "password");
		this.password = Arrays.copyOf(password, password.length);
		this.utf8Encoding = utf8Encoding;
	}

	/**
	 * <h3 class="en-US">Static method for initialize password credentials</h3>
	 * <h3 class="zh-CN">静态方法用于初始化密码凭据</h3>
	 *
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @return <span class="en-US">Password credentials instance object</span>
	 * <span class="zh-CN">密码凭据实例对象</span>
	 */
	public static PasswordCredentials of(final char[] password) {
		return new PasswordCredentials(password, false);
	}

	/**
	 * <h3 class="en-US">Static method for initialize password credentials</h3>
	 * <h3 class="zh-CN">静态方法用于初始化密码凭据</h3>
	 *
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @param utf8Encoding <span class="en-US">Using UTF-8 encoding flag</span>
	 *                     <span class="zh-CN">使用 UTF-8 编码标记</span>
	 * @return <span class="en-US">Password credentials instance object</span>
	 * <span class="zh-CN">密码凭据实例对象</span>
	 */
	public static PasswordCredentials of(final char[] password, final boolean utf8Encoding) {
		return new PasswordCredentials(password, utf8Encoding);
	}

	/**
	 * <h3 class="en-US">Getter method for the using UTF-8 encoding flag</h3>
	 * <h3 class="zh-CN">使用 UTF-8 编码标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Using UTF-8 encoding flag</span>
	 * <span class="zh-CN">使用 UTF-8 编码标记</span>
	 */
	public boolean isUtf8Encoding() {
		return this.utf8Encoding;
	}

	/**
	 * <h3 class="en-US">Copy password</h3>
	 * <h3 class="zh-CN">复制密码</h3>
	 *
	 * @return <span class="en-US">Password</span>
	 * <span class="zh-CN">密码</span>
	 */
	public synchronized char[] copyPassword() {
		this.ensureAvailable();
		return Arrays.copyOf(this.password, this.password.length);
	}

	/**
	 * <h3 class="en-US">Password length</h3>
	 * <h3 class="zh-CN">密码长度</h3>
	 *
	 * @return <span class="en-US">Password length</span>
	 * <span class="zh-CN">密码长度</span>
	 */
	public synchronized int length() {
		this.ensureAvailable();
		return this.password.length;
	}

	@Override
	public void close() {
		this.destroy();
	}

	@Override
	public String toString() {
		return "PasswordCredentials{destroyed=" + this.destroyed + ", password=<redacted>}";
	}

	@Override
	public boolean isDestroyed() {
		return this.destroyed;
	}

	@Override
	public void destroy() {
		if (this.destroyed) {
			return;
		}
		Arrays.fill(this.password, '\0');
		this.password = new char[0];
		this.destroyed = Boolean.TRUE;
	}

	private void ensureAvailable() {
		if (this.destroyed) {
			throw new IllegalStateException("Password credentials has been destroyed");
		}
	}
}
