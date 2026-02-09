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

package org.nervousync.generator.ulid;

import org.nervousync.annotations.provider.Provider;
import org.nervousync.commons.Globals;
import org.nervousync.commons.id.ULID;
import org.nervousync.generator.IGenerator;
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.id.IDUtils;
import org.nervousync.utils.core.RawUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <h2 class="en-US">Universally Unique Lexicographically Sortable Identifier generator</h2>
 * <h2 class="zh-CN">通用唯一字典排序标识符生成器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: May 21, 2025 15:28:09 $
 */
@Provider(name = IDUtils.ULID, titleKey = "ulid.id.generator.name")
public final class ULIDGenerator implements IGenerator<ULID> {
	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private final LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());

	/**
	 * <span class="en-US">Reference time, default value: 0L</span>
	 * <span class="zh-CN">起始时间戳，默认值：0L</span>
	 */
	private long referenceTime = 0L;
	/**
	 * <span class="en-US">Sequence index of current time</span>
	 * <span class="zh-CN">当前时间的序列索引</span>
	 */
	private long sequenceIndex = 0L;
	/**
	 * <span class="en-US">Monotonic flag</span>
	 * <span class="zh-CN">单调标记</span>
	 */
	private boolean monotonic = Boolean.FALSE;
	/**
	 * <span class="en-US">Previous generate time</span>
	 * <span class="zh-CN">上次生成ID的时间</span>
	 */
	private final AtomicLong lastTime = new AtomicLong(Globals.DEFAULT_VALUE_LONG);
	/**
	 * <span class="en-US">Previous generate random data bytes</span>
	 * <span class="zh-CN">上次生成的随机数</span>
	 */
	private final AtomicReference<byte[]> lastRandom = new AtomicReference<>(new byte[0]);

	/**
	 * <h3 class="en-US">Configure current generator</h3>
	 * <h3 class="zh-CN">修改当前生成器的配置</h3>
	 *
	 * @param referenceTime <span class="en-US">Reference time, default value: 1303315200000L</span>
	 *                      <span class="zh-CN">起始时间戳，默认值：1303315200000L</span>
	 * @param monotonic     <span class="en-US">Monotonic flag</span>
	 *                      <span class="zh-CN">单调标记</span>
	 */
	public void config(final long referenceTime, final boolean monotonic) {
		this.referenceTime = Math.max(referenceTime, 0L);
		this.sequenceIndex = 0L;
		this.monotonic = monotonic;
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Config_ULID_Error", this.referenceTime);
		}
	}

	@Override
	public ULID generate() {
		long currentTime = DateTimeUtils.currentUTCTimeMillis();
		if (currentTime < this.lastTime.get()) {
			throw new RuntimeException(
					String.format("System clock moved backwards. Refusing to generate id for %d milliseconds",
							this.lastTime.get() - currentTime));
		}

		boolean random = Boolean.TRUE;
		if (this.monotonic) {
			if (currentTime == this.lastTime.get()) {
				this.sequenceIndex += 1;
				random = Boolean.FALSE;
			} else {
				this.sequenceIndex = 0L;
			}
		}
		if (random) {
			byte[] dataBytes = new byte[10];
			Globals.randomBytes(dataBytes);
			this.lastRandom.set(dataBytes);
		}
		this.lastTime.set(currentTime);

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Generate_ULID_Debug", this.lastTime, this.referenceTime, this.sequenceIndex);
		}

		byte[] dataBytes = this.lastRandom.get();
		return new ULID(((currentTime - this.referenceTime) << 16) | (RawUtils.readShort(dataBytes) & 0xFFFFL),
				RawUtils.readLong(dataBytes, 2) + this.sequenceIndex);
	}

	@Override
	public ULID generate(final byte[] dataBytes) {
		return ULID.fromBytes(dataBytes);
	}

	@Override
	public void destroy() {
		this.sequenceIndex = 0L;
		this.lastTime.set(Globals.DEFAULT_VALUE_LONG);
	}
}
