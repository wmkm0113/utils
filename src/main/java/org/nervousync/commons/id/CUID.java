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
import org.nervousync.generator.cuid.impl.CUIDv1Generator;
import org.nervousync.utils.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * <h2 class="en-US">Secure, collision-resistant ids optimized for horizontal scaling and performance. Next generation UUIDs.</h2>
 * <h2 class="zh-CN">高性能、可预测且适用于分布式环境的唯一标识符</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: May 22, 2025 09:08:15 $
 */
public final class CUID implements Serializable, Comparable<CUID> {

	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = 9198313467092132650L;

	/**
	 * <span class="en-US">Unique identifier value</span>
	 * <span class="zh-CN">唯一标识符值</span>
	 */
	private final String value;

	/**
	 * <h3 class="en-US">Private constructor method for Secure, collision-resistant ids optimized for horizontal scaling and performance. Next generation UUIDs.</h3>
	 * <h3 class="zh-CN">高性能、可预测且适用于分布式环境的唯一标识符的私有构造方法</h3>
	 *
	 * @param value <span class="en-US">The CUID string</span>
	 *              <span class="zh-CN">CUID字符串</span>
	 */
	private CUID(final String value) {
		assert validate(value) : "Invalid CUID value: " + value;
		this.value = value;
	}

	/**
	 * <h3 class="en-US">Static method used to generate a CUID instance object from a CUID string</h3>
	 * <h3 class="zh-CN">静态方法用于通过CUID字符串生成CUID实例对象</h3>
	 *
	 * @param value <span class="en-US">The CUID string</span>
	 *              <span class="zh-CN">CUID字符串</span>
	 * @return <span class="en-US">Parsed CUID instance object</span>
	 * <span class="zh-CN">生成的CUID实例对象</span>
	 */
	public static CUID fromString(final String value) {
		return new CUID(value);
	}

	/**
	 * <h3 class="en-US">Static method used to validate a CUID string is valid</h3>
	 * <h3 class="zh-CN">静态方法用于验证CUID字符串是否有效</h3>
	 *
	 * @param string <span class="en-US">The CUID string</span>
	 *               <span class="zh-CN">CUID字符串</span>
	 * @return <span class="en-US">Validate result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	public static boolean validate(final String string) {
		if (StringUtils.isEmpty(string)) {
			return Boolean.FALSE;
		}

		if ((string.length() == CUIDv1Generator.VALUE_LENGTH)
				&& string.startsWith(Character.toString(CUIDv1Generator.START_CHAR))) {
			return Boolean.TRUE;
		}
		return string.chars()
				.allMatch(c -> ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')));
	}

	@Override
	public String toString() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.value);
	}

	@Override
	public boolean equals(final Object obj) {
		if ((null == obj) || (obj.getClass() != CUID.class)) {
			return false;
		}
		CUID id = (CUID) obj;
		return ObjectUtils.nullSafeEquals(this.value, id.value);
	}

	@Override
	public int compareTo(@Nonnull final CUID val) {
		return this.value.compareTo(val.value);
	}
}
