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

package org.nervousync.commons.id;

import jakarta.annotation.Nonnull;
import org.nervousync.utils.core.RawUtils;
import org.nervousync.utils.core.StringUtils;

import java.io.Serializable;
import java.util.UUID;

/**
 * <h2 class="en-US">Universally Unique Lexicographically Sortable Identifier</h2>
 * <h2 class="zh-CN">通用唯一字典排序标识符</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: May 21, 2025 14:27:18 $
 */
@SuppressWarnings("unused")
public final class ULID implements Serializable, Comparable<ULID> {

	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = 4996816510419541554L;

	/**
	 * <span class="en-US">Alphabet used (Base32 Crockford)</span>
	 * <span class="zh-CN">使用的字母表（Base32 Crockford）</span>
	 */
	private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

	/**
	 * <span class="en-US">The most significant 64 bits of this ULID.</span>
	 * <span class="zh-CN">ULID的128位值中的高有效64位</span>
	 */
	private final long mostSigBits;

	/*
	 * <span class="en-US">The least significant 64 bits of this ULID.</span>
	 * <span class="zh-CN">ULID的128位值中的低有效64位</span>
	 */
	private final long leastSigBits;

	/**
	 * <h3 class="en-US">Private constructor method for universally Unique Lexicographically Sortable Identifier</h3>
	 * <h3 class="zh-CN">通用唯一字典排序标识符的私有构造方法</h3>
	 *
	 * @param dataBytes <span class="en-US">Binary data bytes</span>
	 *                  <span class="zh-CN">二进制数据</span>
	 */
	private ULID(final byte[] dataBytes) {
		long mostSigBits = 0;
		long leastSigBits = 0;
		assert dataBytes.length == 16 : "data must be 16 bytes in length";
		for (int i = 0; i < 8; i++) {
			mostSigBits = (mostSigBits << 8) | (dataBytes[i] & 0xFFL);
		}
		for (int i = 8; i < 16; i++) {
			leastSigBits = (leastSigBits << 8) | (dataBytes[i] & 0xFFL);
		}
		this.mostSigBits = mostSigBits;
		this.leastSigBits = leastSigBits;
	}

	/**
	 * <h3 class="en-US">Constructor method for universally Unique Lexicographically Sortable Identifier</h3>
	 * <h3 class="zh-CN">通用唯一字典排序标识符的构造方法</h3>
	 *
	 * @param mostSigBits  <span class="en-US">The most significant 64 bits of this ULID.</span>
	 *                     <span class="zh-CN">ULID的128位值中的高有效64位</span>
	 * @param leastSigBits <span class="en-US">The least significant 64 bits of this ULID.</span>
	 *                     <span class="zh-CN">ULID的128位值中的低有效64位</span>
	 */
	public ULID(final long mostSigBits, final long leastSigBits) {
		this.mostSigBits = mostSigBits;
		this.leastSigBits = leastSigBits;
	}

	/**
	 * <h3 class="en-US">Static method used to generate a ULID instance object from a ULID binary data bytes</h3>
	 * <h3 class="zh-CN">静态方法用于通过ULID二进制数据生成ULID实例对象</h3>
	 *
	 * @param dataBytes <span class="en-US">Binary data bytes</span>
	 *                  <span class="zh-CN">二进制数据</span>
	 * @return <span class="en-US">Parsed ULID instance object</span>
	 * <span class="zh-CN">生成的ULID实例对象</span>
	 */
	public static ULID fromBytes(final byte[] dataBytes) {
		return new ULID(dataBytes);
	}

	/**
	 * <h3 class="en-US">Static method used to generate a ULID instance object from a ULID string</h3>
	 * <h3 class="zh-CN">静态方法用于通过ULID字符串生成ULID实例对象</h3>
	 *
	 * @param string  <span class="en-US">The ULID string</span>
	 *                     <span class="zh-CN">ULID字符串</span>
	 * @return <span class="en-US">Parsed ULID instance object</span>
	 * <span class="zh-CN">生成的ULID实例对象</span>
	 */
	public static ULID fromString(final String string) {
		return new ULID(StringUtils.base32Decode(string, ALPHABET));
	}

	/**
	 * <h3 class="en-US">Getter method for the most significant 64 bits of this ULID.</h3>
	 * <h3 class="zh-CN">ULID的128位值中的高有效64位的Getter方法</h3>
	 *
	 * @return <span class="en-US">The most significant 64 bits of this ULID.</span>
	 * <span class="zh-CN">ULID的128位值中的高有效64位</span>
	 */
	public long getMostSigBits() {
		return this.mostSigBits;
	}

	/**
	 * <h3 class="en-US">Getter method for the most significant 64 bits of this ULID.</h3>
	 * <h3 class="zh-CN">ULID的128位值中的低有效64位的Getter方法</h3>
	 *
	 * @return <span class="en-US">The least significant 64 bits of this ULID.</span>
	 * <span class="zh-CN">ULID的128位值中的低有效64位</span>
	 */
	public long getLeastSigBits() {
		return this.leastSigBits;
	}

	/**
	 * <h3 class="en-US">Convert to RFC-4122 compliant ULID</h3>
	 * <h3 class="zh-CN">转换为符合RFC-4122标准的ULID</h3>
	 *
	 * @return <span class="en-US">Converted ULID instance object</span>
	 * <span class="zh-CN">转换后的ULID实例对象</span>
	 */
	public ULID toRfc4122() {
		final long mostSigBits = (this.mostSigBits & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000004000L;
		final long leastSIgBits = (this.leastSigBits & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
		return new ULID(mostSigBits, leastSIgBits);
	}

	/**
	 * <h3 class="en-US">Convert to UUID</h3>
	 * <h3 class="zh-CN">转换为UUID</h3>
	 *
	 * @return <span class="en-US">Converted UUID instance object</span>
	 * <span class="zh-CN">转换后的UUID实例对象</span>
	 */
	public UUID toUUID() {
		return new UUID(this.mostSigBits, this.leastSigBits);
	}

	@Override
	public String toString() {
		byte[] dataBytes = new byte[16];
		RawUtils.writeLong(dataBytes, 0, this.mostSigBits);
		RawUtils.writeLong(dataBytes, 8, this.leastSigBits);
		return StringUtils.base32Encode(dataBytes, ALPHABET);
	}

	@Override
	public int hashCode() {
		long hilo = this.mostSigBits ^ this.leastSigBits;
		return ((int) (hilo >> 32)) ^ (int) hilo;
	}

	@Override
	public boolean equals(final Object obj) {
		if ((null == obj) || (obj.getClass() != ULID.class)) {
			return false;
		}
		ULID id = (ULID) obj;
		return (this.mostSigBits == id.mostSigBits &&
				this.leastSigBits == id.leastSigBits);
	}

	@Override
	public int compareTo(@Nonnull final ULID val) {
		if (this.mostSigBits < val.mostSigBits) {
			return -1;
		}
		if (this.mostSigBits > val.mostSigBits) {
			return 1;
		}
		return Long.compare(this.leastSigBits, val.leastSigBits);
	}
}
