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

package org.nervousync.generator.uuid;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.generator.UUIDIdentifier;
import org.nervousync.generator.uuid.timer.UUIDTimer;
import org.nervousync.utils.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <h2 class="en-US">Abstract time-based UUID generator</h2>
 * <h2 class="zh-CN">基于时间的UUID生成器抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 06, 2022 12:48:16 $
 */
public abstract class TimeBasedUUIDGenerator extends UUIDGenerator {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(TimeBasedUUIDGenerator.class);

	/**
	 * <span class="en-US">UUID Epoch Time</span>
	 * <span class="zh-CN">UUID时间戳零点</span>
	 */
	private static final long EPOCH_TIME = 0x01B21DD213814000L;

	protected static final long TICKS_PER_MILLISECOND = 10000L;
	/**
	 * <span class="en-US">The node identification code</span>
	 * <span class="zh-CN">节点识别代码</span>
	 */
	private long nodeIdentifier;
	/**
	 * <span class="en-US">UUID timer instance object</span>
	 * <span class="zh-CN">UUID时钟实例对象</span>
	 */
	protected UUIDTimer uuidTimer;

	protected TimeBasedUUIDGenerator() {
		this.uuidTimer = new UTCTimer();
		this.nodeIdentifier = MAC_IDENTIFIER();
	}

	/**
	 * <h3 class="en-US">Configure current generator</h3>
	 * <h3 class="zh-CN">修改当前生成器的配置</h3>
	 *
	 * @param uuidTimer      <span class="en-US">UUID timer instance</span>
	 *                       <span class="zh-CN">UUID时间生成器实例对象</span>
	 * @param uuidIdentifier <span class="en-US">Generation of UUID node identification code</span>
	 *                       <span class="zh-CN">UUID节点识别代码的生成方式</span>
	 */
	public void config(final UUIDTimer uuidTimer, final UUIDIdentifier uuidIdentifier) {
		if (uuidTimer != null) {
			this.uuidTimer = uuidTimer;
		}
		switch (uuidIdentifier) {
			case MAC:
				this.nodeIdentifier = MAC_IDENTIFIER();
				break;
			case HASH:
				this.nodeIdentifier = HASH_IDENTIFIER();
				break;
			case RANDOM:
				this.nodeIdentifier = RANDOM_IDENTIFIER();
				break;
		}
	}

	/**
	 * <h3 class="en-US">Generate ID value</h3>
	 * <h3 class="zh-CN">生成ID值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的ID值</span>
	 */
	@Override
	public final UUID generate() {
		long timestamp = this.uuidTimer.timestamp();
		return new UUID(this.highBits(timestamp), this.lowBits(timestamp));
	}

	/**
	 * <h3 class="en-US">Generate ID value using given parameter</h3>
	 * <h3 class="zh-CN">使用给定的参数生成ID值</h3>
	 *
	 * @param dataBytes <span class="en-US">Given parameter</span>
	 *                  <span class="zh-CN">给定的参数</span>
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的ID值</span>
	 */
	@Override
	public final UUID generate(byte[] dataBytes) {
		return this.generate();
	}

	/**
	 * <h3 class="en-US">Destroy current generator instance</h3>
	 * <h3 class="zh-CN">销毁当前生成器实例对象</h3>
	 */
	@Override
	public final void destroy() {
		this.uuidTimer.destroy();
	}

	/**
	 * <h3 class="en-US">Getter method for the node identification code</h3>
	 * <h3 class="zh-CN">节点识别代码的Getter方法</h3>
	 *
	 * @return <span class="en-US">The node identification code</span>
	 * <span class="zh-CN">节点识别代码</span>
	 */
	protected final long getNodeIdentifier() {
		return this.nodeIdentifier;
	}

	/**
	 * <h3 class="en-US">Calculate high bits</h3>
	 * <h3 class="zh-CN">计算高位值</h3>
	 *
	 * @return <span class="en-US">High bits value in long</span>
	 * <span class="zh-CN">long型的高位比特值</span>
	 */
	protected abstract long highBits(final long timestamp);

	/**
	 * <h3 class="en-US">Calculate low bits</h3>
	 * <h3 class="zh-CN">计算低位值</h3>
	 *
	 * @return <span class="en-US">Low bits value in long</span>
	 * <span class="zh-CN">long型的低位比特值</span>
	 */
	protected abstract long lowBits(final long timestamp);

	/**
	 * <h3 class="en-US">Get the node identification code through the local network card physical address</h3>
	 * <h3 class="zh-CN">通过本地的网卡物理地址获取节点识别代码</h3>
	 *
	 * @return <span class="en-US">The node identification code</span>
	 * <span class="zh-CN">节点识别代码</span>
	 */
	private static long MAC_IDENTIFIER() {
		byte[] macBytes = SystemUtils.localMac();
		if (macBytes == null || macBytes.length == 0) {
			return Globals.DEFAULT_VALUE_LONG;
		}
		return toLong(macBytes);
	}

	/**
	 * <h3 class="en-US">Get the node identification code through the hash result</h3>
	 * <h3 class="zh-CN">通过哈希值获取节点识别代码</h3>
	 *
	 * @return <span class="en-US">The node identification code</span>
	 * <span class="zh-CN">节点识别代码</span>
	 */
	private static long HASH_IDENTIFIER() {
		byte[] dataBytes = ConvertUtils.hexToBytes(SystemUtils.identifiedKey());
		if (dataBytes.length == 0) {
			return Globals.DEFAULT_VALUE_LONG;
		}
		return toLong(dataBytes);
	}

	/**
	 * <h3 class="en-US">Get the node identification code through the random numbers</h3>
	 * <h3 class="zh-CN">通过随机数获取节点识别代码</h3>
	 *
	 * @return <span class="en-US">The node identification code</span>
	 * <span class="zh-CN">节点识别代码</span>
	 */
	private static long RANDOM_IDENTIFIER() {
		byte[] dataBytes = new byte[6];
		Globals.randomBytes(dataBytes);
		return toLong(dataBytes);
	}

	private static long toLong(@Nonnull final byte[] dataBytes) {
		final int length = Math.min(dataBytes.length, 6);
		final int srcPos = dataBytes.length >= 6 ? dataBytes.length - 6 : 0;
		final byte[] buffer = new byte[]{(byte) 0x80, 0, 0, 0, 0, 0, 0, 0};
		System.arraycopy(dataBytes, srcPos, buffer, 2, length);
		return RawUtils.readLong(buffer);
	}

	private static final class UTCTimer implements UUIDTimer {

		private long systemTimestamp;
		private long usedTimestamp;
		private long counterOffset = Globals.INITIALIZE_INT_VALUE;
		private final AtomicLong counter = new AtomicLong(Globals.INITIALIZE_INT_VALUE);
		private static final int MAX_WAIT_COUNT = 50;

		UTCTimer() {
			this.initCounters();
			this.usedTimestamp = this.systemTimestamp = DateTimeUtils.currentUTCTimeMillis();
		}

		/**
		 * Gets timestamp.
		 *
		 * @return the timestamp
		 */
		@Override
		public long timestamp() {
			//  Using UTC timestamp
			long currentTimeMillis = DateTimeUtils.currentUTCTimeMillis();
			if (currentTimeMillis < this.systemTimestamp) {
				LOGGER.warn("Go_Back_Time_UUID_Debug", currentTimeMillis, this.systemTimestamp);
				this.systemTimestamp = currentTimeMillis;
			}

			if (currentTimeMillis <= this.usedTimestamp) {
				if ((this.counter.get() - this.counterOffset) < TICKS_PER_MILLISECOND) {
					currentTimeMillis = this.usedTimestamp;
				} else {
					long actDiff = this.usedTimestamp - currentTimeMillis;
					long origTime = currentTimeMillis;
					currentTimeMillis = this.usedTimestamp + 1L;
					LOGGER.warn("Timestamp_Over_Run_Warn");
					this.initCounters();
					if (actDiff >= 100L) {
						slowDown(origTime, actDiff);
					}
				}
			} else {
				this.initCounters();
			}

			this.usedTimestamp = currentTimeMillis;

			long returnValue = (currentTimeMillis * TICKS_PER_MILLISECOND)
					+ (this.counter.incrementAndGet() % TICKS_PER_MILLISECOND)
					+ EPOCH_TIME;
			return returnValue & 0x0FFFFFFFFFFFFFFFL;
		}

		private void initCounters() {
			this.counterOffset = Math.abs(Globals.randomLong());
			this.counter.set(this.counterOffset % TICKS_PER_MILLISECOND);
		}

		private void slowDown(final long startTime, final long actDiff) {
			long ratio = actDiff / 100L;
			long delayMillis;
			if (ratio < 2L) {
				delayMillis = 1L;
			} else if (ratio < 10L) {
				delayMillis = 2L;
			} else if (ratio < 600L) {
				delayMillis = 3L;
			} else {
				delayMillis = 5L;
			}

			LOGGER.warn("Virtual_Clock_Warn", delayMillis);
			long timeOutMillis = startTime + delayMillis;
			int counter = 0;

			while (counter <= MAX_WAIT_COUNT && System.currentTimeMillis() < timeOutMillis) {
				try {
					TimeUnit.MILLISECONDS.sleep(delayMillis);
				} catch (InterruptedException ignored) {
				}
				delayMillis = 1L;
				counter++;
			}
		}

		@Override
		public void destroy() {
			//  Do nothing
		}
	}
}
