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

package org.nervousync.generator.sparkid;

import org.nervousync.annotations.provider.Provider;
import org.nervousync.commons.Globals;
import org.nervousync.commons.id.SparkID;
import org.nervousync.generator.IGenerator;
import org.nervousync.utils.core.RawUtils;
import org.nervousync.utils.id.IDUtils;

import java.time.Instant;

/**
 * <h2 class="en-US">SparkID Sortable Identifier generator</h2>
 * <h2 class="zh-CN">SparkID 标识符生成器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 08, 2026 11:19:28 $
 */
@Provider(name = IDUtils.SPARK_ID, titleKey = "spark.id.generator.name")
public final class SparkIDGenerator implements IGenerator<SparkID> {

	/**
	 * <span class="en-US">Maximum value of the random number</span>
	 * <span class="zh-CN">随机数最大值</span>
	 */
	private static final long MAX_RANDOM = Double.valueOf(Math.pow(58, 5)).longValue();
	/**
	 * <span class="en-US">Maximum value of the random number</span>
	 * <span class="zh-CN">随机数最大值</span>
	 */
	private static final long MAX_SEQUENCE = Double.valueOf(Math.pow(58, 6)).longValue() - 1L;

	/**
	 * <span class="en-US">Last time generate timestamp</span>
	 * <span class="zh-CN">最后一次生成时间戳</span>
	 */
	private long lastTimestamp = Globals.DEFAULT_VALUE_LONG;
	/**
	 * <span class="en-US">Sequence counter</span>
	 * <span class="zh-CN">计数器</span>
	 */
	private long sequence = 0L;
	/**
	 * <span class="en-US">Worker id</span>
	 * <span class="zh-CN">机器码</span>
	 */
	private long workerId = 0L;

	@Override
	public synchronized SparkID generate() {
		return this.generate(Globals.DEFAULT_VALUE_LONG);
	}

	@Override
	public synchronized SparkID generate(final byte[] dataBytes) {
		return this.generate(RawUtils.readLong(dataBytes));
	}

	@Override
	public void destroy() {
		this.lastTimestamp = Globals.DEFAULT_VALUE_LONG;
		this.sequence = 0L;
	}

	/**
	 * <h3 class="en-US">Configure worker id</h3>
	 * <h3 class="zh-CN">设置机器码</h3>
	 *
	 * @param workerId <span class="en-US">Worker id</span>
	 *                 <span class="zh-CN">机器码</span>
	 */
	public void config(final long workerId) {
		this.workerId = Math.max(workerId, 0L);
	}

	/**
	 * <h3 class="en-US">Generate ID value</h3>
	 * <h3 class="zh-CN">生成ID值</h3>
	 *
	 * @param epoch <span class="en-US">Epoch timestamp</span>
	 *              <span class="zh-CN">基准时间戳</span>
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的ID值</span>
	 */
	private synchronized SparkID generate(final long epoch) {
		long epochMillis = epoch > 0L ? epoch : Globals.DEFAULT_REFERENCE_TIME;
		long currentTimestamp = Instant.now().toEpochMilli();
		if (currentTimestamp < this.lastTimestamp) {
			throw new RuntimeException(
					String.format("System clock moved backwards. Refusing to generate id for %d milliseconds",
							this.lastTimestamp - currentTimestamp));
		}
		if (currentTimestamp == this.lastTimestamp) {
			this.sequence++;
			if (this.sequence > MAX_SEQUENCE) {
				while (currentTimestamp <= this.lastTimestamp) {
					currentTimestamp = Instant.now().toEpochMilli();
				}
				this.sequence = 0L;
				this.lastTimestamp = currentTimestamp;
			}
		} else {
			this.sequence = 0L;
			this.lastTimestamp = currentTimestamp;
		}
		return new SparkID(this.lastTimestamp - epochMillis, this.sequence, this.workerId,
				Math.abs(Globals.randomLong()) % MAX_RANDOM);
	}
}
