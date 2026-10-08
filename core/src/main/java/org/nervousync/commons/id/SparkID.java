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

import java.io.Serializable;

/**
 * <h2 class="en-US">Spark ID Sortable Identifier</h2>
 * <h2 class="zh-CN">Spark ID 标识符</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 08, 2026 10:43:08 $
 */
public final class SparkID implements Serializable, Comparable<SparkID> {

	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = 611553377686584606L;

	/**
	 * <span class="en-US">Alphabet used (Base58)</span>
	 * <span class="zh-CN">使用的字母表（Base58）</span>
	 */
	private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
	private static final char[] ALPHABET_ARRAY = ALPHABET.toCharArray();

	/**
	 * <span class="en-US">Timestamp</span>
	 * <span class="zh-CN">时间戳</span>
	 */
	private final long timestamp;
	/**
	 * <span class="en-US">Sequence counter</span>
	 * <span class="zh-CN">计数器</span>
	 */
	private final long sequence;
	/**
	 * <span class="en-US">Worker id</span>
	 * <span class="zh-CN">机器码</span>
	 */
	private final long workerId;
	/**
	 * <span class="en-US">Random number</span>
	 * <span class="zh-CN">随机数</span>
	 */
	private final long random;

	/**
	 * <h3 class="en-US">Private constructor method for the Spark ID Sortable Identifier</h3>
	 * <h3 class="zh-CN">Spark ID 标识符的私有构造方法</h3>
	 *
	 * @param timestamp <span class="en-US">Timestamp</span>
	 *                  <span class="zh-CN">时间戳</span>
	 * @param sequence  <span class="en-US">Sequence counter</span>
	 *                  <span class="zh-CN">计数器</span>
	 * @param workerId  <span class="en-US">Worker id</span>
	 *                  <span class="zh-CN">机器码</span>
	 * @param random    <span class="en-US">Random number</span>
	 *                  <span class="zh-CN">随机数</span>
	 */
	public SparkID(final long timestamp, final long sequence, final long workerId, final long random) {
		this.timestamp = timestamp;
		this.sequence = sequence;
		this.workerId = workerId;
		this.random = random;
	}

	/**
	 * <h3 class="en-US">Static method for parse the Spark ID string</h3>
	 * <h3 class="zh-CN">静态方法用于解析 SparkID 字符串</h3>
	 *
	 * @param sparkId <span class="en-US">Spark ID string</span>
	 *                <span class="zh-CN">SparkID 字符串</span>
	 */
	public static SparkID fromString(final String sparkId) {
		if (sparkId == null || sparkId.length() != 21) {
			throw new IllegalArgumentException("Invalid spark ID: " + sparkId);
		}
		return new SparkID(decode(sparkId.substring(0, 8)), decode(sparkId.substring(8, 14)),
				decode(sparkId.substring(14, 16)), decode(sparkId.substring(16, 21)));
	}

	@Override
	public String toString() {
		return encode(this.timestamp, 8) + encode(this.sequence, 6)
				+ encode(this.workerId, 2) + encode(this.random, 5);
	}

	@Override
	public int compareTo(@Nonnull final SparkID o) {
		if (this.timestamp != o.timestamp) {
			return Long.compare(this.timestamp, o.timestamp);
		}
		if (this.sequence != o.sequence) {
			return Long.compare(this.sequence, o.sequence);
		}
		return Long.compare(this.random, o.random);
	}

	/**
	 * <h3 class="en-US">Encoding value using base58</h3>
	 * <h3 class="zh-CN">Base58 编码数据</h3>
	 *
	 * @param value  <span class="en-US">Encoding value</span>
	 *               <span class="zh-CN">要编码的数据</span>
	 * @param length <span class="en-US">Output string length</span>
	 *               <span class="zh-CN">输出字符串长度</span>
	 * @return <span class="en-US">Encoded string</span>
	 * <span class="zh-CN">编码后的字符串</span>
	 */
	private static String encode(final long value, final int length) {
		StringBuilder stringBuilder = new StringBuilder();
		long result = value;
		while (result > 0) {
			stringBuilder.append(ALPHABET_ARRAY[(int) (result % ALPHABET_ARRAY.length)]);
			result = result / ALPHABET_ARRAY.length;
		}
		while (stringBuilder.length() < length) {
			stringBuilder.append(ALPHABET_ARRAY[0]);
		}
		return stringBuilder.reverse().toString();
	}

	/**
	 * <h3 class="en-US">Decoding value using base58</h3>
	 * <h3 class="zh-CN">解码 Base58 数据</h3>
	 *
	 * @param value <span class="en-US">Decoding value</span>
	 *              <span class="zh-CN">要解码的数据</span>
	 * @return <span class="en-US">Decoded value</span>
	 * <span class="zh-CN">编码后的数据</span>
	 */
	private static long decode(final String value) {
		long result = 0L;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			int index = ALPHABET.indexOf(c);
			if (index >= 0) {
				result = result * 58 + index;
			}
		}
		return result;
	}
}