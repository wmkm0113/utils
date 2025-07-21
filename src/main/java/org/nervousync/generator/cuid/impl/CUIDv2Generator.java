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
import org.nervousync.utils.DateTimeUtils;
import org.nervousync.utils.IDUtils;
import org.nervousync.utils.RawUtils;
import org.nervousync.utils.SecurityUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

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
	private final AtomicInteger counter = new AtomicInteger(Integer.MAX_VALUE);

	@Override
	public CUID generate() {
		byte[] dataBytes = new byte[4];
		RawUtils.writeInt(dataBytes, VALUE_LENGTH);
		return this.generate(dataBytes);
	}

	@Override
	public CUID generate(final byte[] dataBytes) {
		int length = RawUtils.readInt(dataBytes);
		if (length <= Globals.INITIALIZE_INT_VALUE) {
			length = VALUE_LENGTH;
		}
		this.counter.compareAndSet(Integer.MAX_VALUE, safeAbs(Globals.random()));
		final char firstChar = (char) ((safeAbs(Globals.random()) % 26) + 97);
		final String timestamp = Long.toString(DateTimeUtils.currentUTCTimeMillis(), DEFAULT_RADIX);
		final String data = timestamp + SALT(length)
				+ processPadding(Integer.toString(this.counter.incrementAndGet(), DEFAULT_RADIX), 4)
				+ MACHINE_FINGERPRINT;
		String result = new BigInteger(SecurityUtils.SHA3_256((data + SALT(length)).getBytes(StandardCharsets.UTF_8)))
					.toString(DEFAULT_RADIX);
		return CUID.fromString(processPadding(firstChar + result.substring(1, Math.min(length, result.length())), length));
	}

	@Override
	public void destroy() {
		this.counter.set(Integer.MAX_VALUE);
	}

	static String SALT(final int length) {
		int primeNumber;
		StringBuilder stringBuilder = new StringBuilder(length);
		while (stringBuilder.length() < length) {
			primeNumber = PRIME_NUMBER_ARRAY[safeAbs(Globals.random()) % PRIME_NUMBER_ARRAY.length];
			stringBuilder.append(Integer.toString(primeNumber * Globals.random(), 36));
		}
		return stringBuilder.toString();
	}

	private static int safeAbs(final int value) {
		return (value == Integer.MIN_VALUE) ? Globals.INITIALIZE_INT_VALUE : Math.abs(value);
	}
}
