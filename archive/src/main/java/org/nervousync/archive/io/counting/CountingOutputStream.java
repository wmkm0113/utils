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
import java.io.OutputStream;
import java.util.Objects;

/**
 * <h2 class="en-US">Counting output stream</h2>
 * <h2 class="zh-CN">计数输出流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class CountingOutputStream extends OutputStream {

	/**
	 * <span class="en-US">Source output stream instance object</span>
	 * <span class="zh-CN">源输出流</span>
	 */
	private final OutputStream sourceStream;
	/**
	 * <span class="en-US">Read counting</span>
	 * <span class="zh-CN">读取计数器</span>
	 */
	private long count;

	/**
	 * <h3 class="en-US">Constructor method for the counting output stream</h3>
	 * <h3 class="zh-CN">计数输出流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source output stream instance object</span>
	 *                     <span class="zh-CN">源输出流</span>
	 */
	public CountingOutputStream(final OutputStream sourceStream) {
		this.sourceStream = sourceStream;
		this.count = 0L;
	}

	@Override
	public void write(final int value) throws IOException {
		long next = RawUtils.add(this.count, 1);
		this.sourceStream.write(value);
		this.count = next;
	}

	@Override
	public void write(@Nonnull byte[] bytes, int offset, int length) throws IOException {
		if (length == 0) {
			return;
		}
		Objects.checkFromIndexSize(offset, length, bytes.length);
		long next = RawUtils.add(this.count, length);
		this.sourceStream.write(bytes, offset, length);
		this.count = next;
	}

	@Override
	public void flush() throws IOException {
		this.sourceStream.flush();
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
