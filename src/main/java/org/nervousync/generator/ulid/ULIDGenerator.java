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
import org.nervousync.commons.ULID;
import org.nervousync.generator.IGenerator;
import org.nervousync.utils.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <h2 class="en-US">Universally Unique Lexicographically Sortable Identifier generator</h2>
 * <h2 class="zh-CN">通用唯一字典排序标识符生成器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: May 21, 2025 15:28:09 $
 */
@Provider(name = IDUtils.ULID, titleKey = "ulid.id.generator.name")
public final class ULIDGenerator implements IGenerator<String> {
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
	 * <span class="en-US">Previous generate time</span>
	 * <span class="zh-CN">上次生成ID的时间</span>
	 */
	private final AtomicLong lastTime = new AtomicLong(Globals.DEFAULT_VALUE_LONG);

	/**
	 * <h3 class="en-US">Configure current generator</h3>
	 * <h3 class="zh-CN">修改当前生成器的配置</h3>
	 *
	 * @param referenceTime <span class="en-US">Reference time, default value: 1303315200000L</span>
	 *                      <span class="zh-CN">起始时间戳，默认值：1303315200000L</span>
	 */
	public void config(final long referenceTime) {
		this.referenceTime = Math.max(referenceTime, 0L);
		this.sequenceIndex = 0L;
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Config_ULID_Error", this.referenceTime);
		}
	}

	@Override
	public String generate() {
		return this.random().toString();
	}

	private ULID random() {
		long currentTime = DateTimeUtils.currentUTCTimeMillis();
		if (currentTime < this.lastTime.get()) {
			throw new RuntimeException(
					String.format("System clock moved backwards. Refusing to generate id for %d milliseconds",
							this.lastTime.get() - currentTime));
		}

		if (currentTime == this.lastTime.get()) {
			this.sequenceIndex += 1;
			if (this.sequenceIndex == 0) {
				while (true) {
					if ((currentTime = DateTimeUtils.currentUTCTimeMillis()) > this.lastTime.get()) {
						break;
					}
				}
			}
		} else {
			this.sequenceIndex = 0L;
		}
		this.lastTime.set(currentTime);

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Generate_ULID_Debug", this.lastTime, this.referenceTime, this.sequenceIndex);
		}

		byte[] dataBytes = new byte[8];
		Globals.randomBytes(dataBytes);
		return new ULID(currentTime - this.referenceTime, RawUtils.readLong(dataBytes));
	}

	@Override
	public String generate(final byte[] dataBytes) {
		return ULID.fromBytes(dataBytes).toString();
	}

	@Override
	public void destroy() {
		this.sequenceIndex = 0L;
		this.lastTime.set(Globals.DEFAULT_VALUE_LONG);
	}
}
