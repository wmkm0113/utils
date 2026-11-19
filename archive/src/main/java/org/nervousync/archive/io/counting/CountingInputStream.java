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

package org.nervousync.archive.io.counting;

import jakarta.annotation.Nonnull;
import org.nervousync.utils.commons.RawUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * <h2 class="en-US">Counting input stream</h2>
 * <h2 class="zh-CN">计数输入流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class CountingInputStream extends InputStream {

	/**
	 * <span class="en-US">Source input stream instance object</span>
	 * <span class="zh-CN">源输入流</span>
	 */
	private final InputStream sourceStream;
	/**
	 * <span class="en-US">Maximum limit size</span>
	 * <span class="zh-CN">最大值限制</span>
	 */
	private final long limit;
	/**
	 * <span class="en-US">Read counting</span>
	 * <span class="zh-CN">读取计数器</span>
	 */
	private long count;

	/**
	 * <h3 class="en-US">Constructor method for the counting input stream</h3>
	 * <h3 class="zh-CN">计数输入流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source input stream instance object</span>
	 *                     <span class="zh-CN">源输入流</span>
	 */
	public CountingInputStream(final InputStream sourceStream) {
		this(sourceStream, Long.MAX_VALUE);
	}

	/**
	 * <h3 class="en-US">Constructor method for the counting input stream</h3>
	 * <h3 class="zh-CN">计数输入流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source input stream instance object</span>
	 *                     <span class="zh-CN">源输入流</span>
	 * @param limit        <span class="en-US">Maximum limit size</span>
	 *                     <span class="zh-CN">最大值限制</span>
	 */
	public CountingInputStream(final InputStream sourceStream, final long limit) {
		this.sourceStream = sourceStream;
		this.limit = (limit < 0L) ? Long.MAX_VALUE : limit;
		this.count = 0L;
	}

	@Override
	public int read() throws IOException {
		if (this.count == this.limit) {
			return -1;
		}
		int value = this.sourceStream.read();
		if (value >= 0) {
			this.count = RawUtils.add(this.count, 1);
		}
		return value;
	}

	@Override
	public int read(@Nonnull final byte[] bytes, final int offset, final int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		if (length == 0) {
			return 0;
		}
		if (this.count == this.limit) {
			return -1;
		}
		int count = this.sourceStream.read(bytes, offset, (int) Math.min(length, this.limit - this.count));
		if (count > 0) {
			this.count = RawUtils.add(this.count, count);
		}
		return count;
	}

	@Override
	public void close() {
	}

	/**
	 * <h3 class="en-US">Getter method for the read counting</h3>
	 * <h3 class="zh-CN">读取计数器的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Read counting</span>
	 * <span class="zh-CN">读取计数器</span>
	 */
	public long getCount() {
		return this.count;
	}
}
