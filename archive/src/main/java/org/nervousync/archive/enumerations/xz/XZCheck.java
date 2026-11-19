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

package org.nervousync.archive.enumerations.xz;

import java.io.IOException;

/**
 * <h2 class="en-US">Integrity checks defined by the XZ Stream Flags.</h2>
 * <h2 class="zh-CN">XZ 流标志中定义的完整性校验类型。</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public enum XZCheck {
	/**
	 * <span class="en-US">No data Check; structural CRC32 fields remain mandatory.</span>
	 * <span class="zh-CN">无数据校验，结构 CRC32 仍必需。</span>
	 */
	NONE(0, 0),
	/**
	 * <span class="en-US">IEEE CRC32, little endian.</span>
	 * <span class="zh-CN">小端表示的 IEEE CRC32。</span>
	 */
	CRC32(1, 4),
	/**
	 * <span class="en-US">XZ CRC64 with reflected ECMA polynomial.</span>
	 * <span class="zh-CN">使用反射 ECMA 多项式的 XZ CRC64。</span>
	 */
	CRC64(4, 8),
	/**
	 * <span class="en-US">SHA-256 digest in its normal byte order.</span>
	 * <span class="zh-CN">使用摘要原始字节顺序的 SHA-256。</span>
	 */
	SHA256(10, 32);

	/**
	 * <span class="en-US">Wire identifier.</span>
	 * <span class="zh-CN">编码标识。</span>
	 */
	private final int code;
	/**
	 * <span class="en-US">Check length in bytes.</span>
	 * <span class="zh-CN">校验字节数。</span>
	 */
	private final int size;

	/**
	 * <h3 class="en-US">Constructor method for the integrity checks defined by the XZ Stream Flags.</h3>
	 * <h3 class="zh-CN">XZ 流标志中定义的完整性校验类型的构造方法</h3>
	 *
	 * @param code <span class="en-US">Wire identifier.</span>
	 *             <span class="zh-CN">编码标识。</span>
	 * @param size <span class="en-US">Check length in bytes.</span>
	 *             <span class="zh-CN">校验字节数。</span>
	 */
	XZCheck(final int code, final int size) {
		this.code = code;
		this.size = size;
	}

	/**
	 * <h3 class="en-US">Getter method for the wire identifier</h3>
	 * <h3 class="zh-CN">编码标识的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Wire identifier.</span>
	 * <span class="zh-CN">编码标识。</span>
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * <h3 class="en-US">Getter method for the check length in bytes</h3>
	 * <h3 class="zh-CN">校验字节数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Check length in bytes.</span>
	 * <span class="zh-CN">校验字节数。</span>
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * <h3 class="en-US">Resolve a supported wire ID; reserved IDs are errors.</h3>
	 * <h3 class="zh-CN">解析支持的标识，拒绝保留值。</h3>
	 *
	 * @param code <span class="en-US">Wire identifier.</span>
	 *             <span class="zh-CN">编码标识。</span>
	 * @return <span class="en-US">Matched XZCheck instance object.</span>
	 * <span class="zh-CN">匹配的校验类型。</span>
	 * @throws IOException <span class="en-US">Not found the wire identifier</span>
	 *                     <span class="zh-CN">编码标识未找到</span>
	 */
	public static XZCheck from(final int code) throws IOException {
		for (XZCheck check : values()) {
			if (check.code == code) {
				return check;
			}
		}
		throw new IOException("Unsupported XZ check code: " + code);
	}
}
