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

package org.nervousync.generator.cuid.impl;

import org.nervousync.annotations.provider.Provider;
import org.nervousync.commons.Globals;
import org.nervousync.commons.id.CUID;
import org.nervousync.generator.cuid.CUIDGenerator;
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.id.IDUtils;
import org.nervousync.utils.core.RawUtils;
import org.nervousync.utils.security.SecurityUtils;

import java.nio.charset.StandardCharsets;

/**
 * <h2 class="en-US">CUID version 2 generator</h2>
 * <h2 class="zh-CN">CUID版本2生成器抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: May 22, 2025 13:05:28 $
 */
@Provider(name = IDUtils.CUIDv2, titleKey = "version2.cuid.id.generator.name")
public final class CUIDv2Generator extends CUIDGenerator {

	/**
	 * <span class="en-US">Valid string value length</span>
	 * <span class="zh-CN">合法值的字符串长度</span>
	 */
	public static final int VALUE_LENGTH = 24;
	private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
	/**
	 * <span class="en-US">Initial letter safety table</span>
	 * <span class="zh-CN">首字母安全表</span>
	 */
	private static final String STARTING_LETTERS = "abcdefghijklmnopqrstuvwxy";
	/**
	 * <span class="en-US">Valid string value length</span>
	 * <span class="zh-CN">合法值的字符串长度</span>
	 */
	private static final int[] PRIME_NUMBER_ARRAY =
			new int[]{109717, 109721, 109741, 109751, 109789, 109793, 109807, 109819, 109829, 109831};
	/**
	 * <span class="en-US">Counter</span>
	 * <span class="zh-CN">计数器</span>
	 */
	private int counter = Integer.MAX_VALUE;

	@Override
	public synchronized CUID generate() {
		byte[] dataBytes = new byte[4];
		RawUtils.writeInt(dataBytes, VALUE_LENGTH);
		return this.generate(dataBytes);
	}

	@Override
	public synchronized CUID generate(final byte[] dataBytes) {
		int length = RawUtils.readInt(dataBytes);
		if (length <= Globals.INITIALIZE_INT_VALUE) {
			length = VALUE_LENGTH;
		}
		if (this.counter == Integer.MAX_VALUE) {
			this.counter = safeAbs(Globals.random());
		} else {
			this.counter++;
		}
		final char firstChar = STARTING_LETTERS.charAt(Globals.random(STARTING_LETTERS.length()));
		final String timestamp = Long.toString(DateTimeUtils.currentUTCTimeMillis(), DEFAULT_RADIX);
		final String data =
				timestamp + processPadding(Integer.toString(this.counter, DEFAULT_RADIX), 4) + MACHINE_FINGERPRINT;
		byte[] result = SecurityUtils.SHA3_256((data + SALT(length)).getBytes(StandardCharsets.UTF_8));
		int maxLength = Math.min(length, 32);
		char[] buffer = new char[maxLength];
		buffer[0] = firstChar;
		long currentWindow = 0;
		int bitsInWindow = 0, byteIndex = 0, charIndex = 1;
		while (charIndex < maxLength) {
			while (bitsInWindow < 6 && byteIndex < result.length) {
				currentWindow = (currentWindow << 8) | (result[byteIndex++] & 0xFFL);
				bitsInWindow += 8;
			}

			if (bitsInWindow == 0) {
				buffer[charIndex++] = '0';
				continue;
			}

			int extractBits = Math.min(bitsInWindow, 5),
					value = (int) ((currentWindow >>> (bitsInWindow - extractBits)) & ((1 << extractBits) - 1));
			bitsInWindow -= extractBits;
			buffer[charIndex++] = ALPHABET.charAt(value % 36);
		}
		return CUID.fromString(new String(buffer));
	}

	@Override
	public void destroy() {
		this.counter = Integer.MAX_VALUE;
	}

	static String SALT(final int length) {
		char[] buffer = new char[length];
		int primeLength = PRIME_NUMBER_ARRAY.length;
		for (int i = 0; i < length; i++) {
			int random = Globals.random(), primeNumber = PRIME_NUMBER_ARRAY[safeAbs(random) % primeLength];
			buffer[i] = ALPHABET.charAt(safeAbs(primeNumber * random) % 36);
		}
		return new String(buffer);
//		StringBuilder stringBuilder = new StringBuilder(length);
//		while (stringBuilder.length() < length) {
//			primeNumber = PRIME_NUMBER_ARRAY[safeAbs(Globals.random()) % PRIME_NUMBER_ARRAY.length];
//			stringBuilder.append(Integer.toString(primeNumber * Globals.random(), 36));
//		}
//		return stringBuilder.toString();
	}

	private static int safeAbs(final int value) {
		return (value == Integer.MIN_VALUE) ? Globals.INITIALIZE_INT_VALUE : Math.abs(value);
	}
}
