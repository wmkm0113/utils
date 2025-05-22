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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2 class="en-US">CUID version 1 generator</h2>
 * <h2 class="zh-CN">CUID版本1生成器抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: May 22, 2025 13:05:28 $
 */
@Provider(name = IDUtils.CUIDv1, titleKey = "version1.cuid.id.generator.name")
public final class CUIDv1Generator extends CUIDGenerator {

	/**
	 * <span class="en-US">Valid string value length</span>
	 * <span class="zh-CN">合法值的字符串长度</span>
	 */
	public static final int VALUE_LENGTH = 25;
	/**
	 * <span class="en-US">Start character</span>
	 * <span class="zh-CN">起始字符</span>
	 */
	public static final char START_CHAR = 'c';
	/**
	 * <span class="en-US">Maximum allowed counter-value</span>
	 * <span class="zh-CN">允许的计数器最大值</span>
	 */
	private static final int DISCRETE_VALUE = (int) Math.pow(DEFAULT_RADIX, 4);
	/**
	 * <span class="en-US">Counter</span>
	 * <span class="zh-CN">计数器</span>
	 */
	private final AtomicInteger counter = new AtomicInteger(Globals.INITIALIZE_INT_VALUE);

	@Override
	public CUID generate() throws Exception {
		return this.generate(new byte[0]);
	}

	@Override
	public CUID generate(final byte[] dataBytes) throws Exception {
		this.counter.compareAndSet(DISCRETE_VALUE, Globals.INITIALIZE_INT_VALUE);
		String value = CUIDv1Generator.START_CHAR + Long.toString(DateTimeUtils.currentUTCTimeMillis(), DEFAULT_RADIX)
				+ processPadding(Integer.toString(this.counter.incrementAndGet(), DEFAULT_RADIX), 4)
				+ MACHINE_FINGERPRINT
				+ processPadding(Integer.toString(Globals.random(), DEFAULT_RADIX), 4)
				+ processPadding(Integer.toString(Globals.random(), DEFAULT_RADIX), 4);
		return CUID.fromString(value);
	}

	@Override
	public void destroy() {
		this.counter.set(Globals.INITIALIZE_INT_VALUE);
	}
}
