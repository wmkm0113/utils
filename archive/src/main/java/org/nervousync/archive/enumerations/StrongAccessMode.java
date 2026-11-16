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

package org.nervousync.archive.enumerations;

import org.nervousync.archive.commons.ZipGlobals;

/**
 * <h2 class="en-US">Strong encryption access mode</h2>
 * <h2 class="zh-CN">强加密访问模式</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public enum StrongAccessMode {

	PASSWORD(1),
	CERTIFICATE(2),
	BOTH(3);

	/**
	 * <span class="en-US">Flags</span>
	 * <span class="zh-CN">标记</span>
	 */
	private final int flags;

	/**
	 * <h3 class="en-US">Constructor method for the strong encryption access mode</h3>
	 * <h3 class="zh-CN">强加密访问模式的构造方法</h3>
	 *
	 * @param flags <span class="en-US">Flags</span>
	 *              <span class="zh-CN">标记</span>
	 */
	StrongAccessMode(final int flags) {
		this.flags = flags;
	}

	/**
	 * <h3 class="en-US">Getter method for the flags</h3>
	 * <h3 class="zh-CN">标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Flags</span>
	 * <span class="zh-CN">标记</span>
	 */
	public int flags() {
		return this.flags;
	}

	/**
	 * <h3 class="en-US">Check use password</h3>
	 * <h3 class="zh-CN">检查使用密码</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean usePassword() {
		return (this.flags & ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD) != 0;
	}

	/**
	 * <h3 class="en-US">Check use certificate</h3>
	 * <h3 class="zh-CN">检查使用证书</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean useCertificate() {
		return (this.flags & ZipGlobals.ZIP64_HEADER_ENC_FLAGS_CERTIFICATE) != 0;
	}

	/**
	 * <h3 class="en-US">Get strong access mode from given flags</h3>
	 * <h3 class="zh-CN">从标记获得强加密访问模式</h3>
	 *
	 * @param flags <span class="en-US">Flags</span>
	 *              <span class="zh-CN">标记</span>
	 * @return <span class="en-US">Strong access mode</span>
	 * <span class="zh-CN">强加密访问模式</span>
	 */
	public static StrongAccessMode from(final int flags) {
		return switch (flags & 3) {
			case 1 -> PASSWORD;
			case 2 -> CERTIFICATE;
			case 3 -> BOTH;
			default -> throw new IllegalArgumentException("Invalid flags");
		};
	}
}
