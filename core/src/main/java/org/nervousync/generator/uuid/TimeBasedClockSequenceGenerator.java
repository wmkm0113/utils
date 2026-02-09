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

import org.nervousync.commons.Globals;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <h2 class="en-US">Abstract time-based and clock-sequence UUID generator</h2>
 * <h2 class="zh-CN">基于时间和时钟序列的UUID生成器抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 06, 2022 12:48:16 $
 */
public abstract class TimeBasedClockSequenceGenerator extends TimeBasedUUIDGenerator {

	private final AtomicInteger sequence;
	private final AtomicLong lastTimestamp = new AtomicLong(Globals.DEFAULT_VALUE_LONG);
	private static final ClockSequence CLOCK_SEQUENCE = new ClockSequence();
	private static final SplittableRandom RANDOM = new SplittableRandom();

	public TimeBasedClockSequenceGenerator() {
		this.sequence = new AtomicInteger(CLOCK_SEQUENCE.random());
	}

	@Override
	protected final long lowBits(final long timestamp) {
		long nodeIdentifier = super.getNodeIdentifier();
		long clockSequence;
		if (timestamp > this.lastTimestamp.get()) {
			this.lastTimestamp.set(timestamp);
			clockSequence = this.sequence.get();
		} else {
			this.lastTimestamp.set(timestamp);
			if (this.sequence.incrementAndGet() > ClockSequence.MAX_VALUE) {
				this.sequence.set(ClockSequence.MIN_VALUE);
			}
			clockSequence = this.sequence.updateAndGet(CLOCK_SEQUENCE::take);
		}
		return ((((clockSequence & 0x3FFFL) << 48) | (nodeIdentifier & 0xFFFFFFFFFFFFL)) & 0x3FFFFFFFFFFFFFFFL)
				| 0x8000000000000000L;
	}

	private static final class ClockSequence {

		private static final int MIN_VALUE = 0x0;
		private static final int MAX_VALUE = 0x3FFF;
		private static final int POOL_SIZE = Double.valueOf(Math.pow(2, 14)).intValue();

		private final byte[] pool = new byte[2048];

		synchronized int random() {
			return this.take(Math.abs(RANDOM.nextInt()) % POOL_SIZE);
		}

		synchronized int take(final int val) {
			int value = val;
			for (int i = 0; i < POOL_SIZE; i++) {
				if (setBit(value)) {
					return value;
				}
				value = ++value % POOL_SIZE;
			}
			this.clear();
			this.setBit(value);
			return value;
		}

		synchronized boolean setBit(final int value) {
			if (value < Globals.INITIALIZE_INT_VALUE) {
				return Boolean.FALSE;
			}

			final int byteIndex = value / 8;
			final int bitIndex = value % 8;

			final int mask = (0x1 << bitIndex);
			if ((this.pool[byteIndex] & mask) == 0) {
				this.pool[byteIndex] = (byte) (this.pool[byteIndex] | mask);
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		}

		synchronized void clear() {
			Arrays.fill(this.pool, (byte) 0);
		}
	}
}
