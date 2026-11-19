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

package org.nervousync.archive.zip.io;

import jakarta.annotation.Nonnull;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * <h2 class="en-US">Bounded input stream</h2>
 * <h2 class="zh-CN">带有边界保护的输入流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class BoundedInputStream extends FilterInputStream {

	/**
	 * <span class="en-US">Remaining read length</span>
	 * <span class="zh-CN">剩余可读长度</span>
	 */
	private long remaining;

	/**
	 * <h3 class="en-US">Constructor method for the bounded input stream</h3>
	 * <h3 class="zh-CN">带有边界保护输入流的构造方法</h3>
	 *
	 * @param inputStream <span class="en-US">Original input stream</span>
	 *                    <span class="zh-CN">原始输入流</span>
	 * @param length      <span class="en-US">Limit read length</span>
	 *                    <span class="zh-CN">可读长度</span>
	 */
	public BoundedInputStream(final InputStream inputStream, final long length) {
		super(inputStream);
		if (length < 0L) {
			throw new IllegalArgumentException("length must be non-negative");
		}
		this.remaining = length;
	}

	/**
	 * <h3 class="en-US">Get the remaining readable length</h3>
	 * <h3 class="zh-CN">获取剩余可读长度</h3>
	 *
	 * @return <span class="en-US">Remaining readable length</span>
	 * <span class="zh-CN">剩余可读长度</span>
	 */
	public long remaining() {
		return this.remaining;
	}

	@Override
	public int read() throws IOException {
		if (this.remaining == 0L) {
			return -1;
		}
		int value = super.read();
		if (value < 0) {
			throw new EOFException("Unexpected end of protected central directory");
		}
		this.remaining--;
		return value;
	}

	@Override
	public int read(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		Objects.requireNonNull(buffer, "buffer");
		Objects.checkFromIndexSize(offset, length, buffer.length);
		if (length == 0) {
			return 0;
		}
		if (this.remaining == 0L) {
			return -1;
		}
		int readLength = super.read(buffer, offset, (int) Math.min(length, this.remaining));
		if (readLength < 0) {
			throw new EOFException("Unexpected end of protected central directory");
		}
		this.remaining -= readLength;
		return readLength;
	}

	@Override
	public long skip(final long length) throws IOException {
		if (length <= 0L || this.remaining == 0L) {
			return 0L;
		}
		long skipped = super.skip(Math.min(length, this.remaining));
		if (skipped > 0L) {
			this.remaining -= skipped;
		}
		return skipped;
	}

	@Override
	public void close() {
	}
}
