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

import org.nervousync.archive.enumerations.PasswordPurpose;
import org.nervousync.archive.security.SecurityRequest;

/**
 * <h2 class="en-US">Password request</h2>
 * <h2 class="zh-CN">密码请求</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class PasswordRequest extends SecurityRequest {

	/**
	 * <span class="en-US">Password purpose</span>
	 * <span class="zh-CN">密码用途</span>
	 */
	private final PasswordPurpose purpose;
	/**
	 * <span class="en-US"></span>
	 * <span class="zh-CN"></span>
	 */
	private final int attempt;

	/**
	 * <h3 class="en-US">Constructor method for the password request</h3>
	 * <h3 class="zh-CN">密码请求的构造方法</h3>
	 *
	 * @param purpose     <span class="en-US">Password purpose</span>
	 *                    <span class="zh-CN">密码用途</span>
	 * @param archivePath <span class="en-US">Archive file path</span>
	 *                    <span class="zh-CN">归档文件地址</span>
	 * @param entryPath   <span class="en-US">Entry path</span>
	 *                    <span class="zh-CN">条目地址</span>
	 * @param attempt     <span class="en-US"></span>
	 *                    <span class="zh-CN"></span>
	 */
	public PasswordRequest(final PasswordPurpose purpose, final String archivePath,
	                       final String entryPath, final int attempt) {
		super(archivePath, entryPath);
		this.purpose = purpose;
		this.attempt = attempt;
	}

	/**
	 * <h3 class="en-US">Getter method for the password purpose</h3>
	 * <h3 class="zh-CN">密码用途的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Password purpose</span>
	 * <span class="zh-CN">密码用途</span>
	 */
	public PasswordPurpose getPurpose() {
		return this.purpose;
	}

	/**
	 * <h3 class="en-US">Getter method for </h3>
	 * <h3 class="zh-CN">的 Getter 方法</h3>
	 *
	 * @return <span class="en-US"></span>
	 * <span class="zh-CN"></span>
	 */
	public int getAttempt() {
		return this.attempt;
	}
}
