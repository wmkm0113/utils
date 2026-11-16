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

package org.nervousync.archive.io.zip;

import org.nervousync.commons.Globals;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <h2 class="en-US">Abstract class of the filter input stream</h2>
 * <h2 class="zh-CN">数据输入流的抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public abstract class AbstractFilterInputStream extends FilterInputStream {

	/**
	 * <span class="en-US">Input stream closed flag</span>
	 * <span class="zh-CN">输入流关闭标记</span>
	 */
	protected boolean closed = false;

	/**
	 * <h3 class="en-US">Constructor method for the abstract class of the filter input stream</h3>
	 * <h3 class="zh-CN">数据输入流的抽象类的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source input stream</span>
	 *                     <span class="zh-CN">原始输入流</span>
	 */
	protected AbstractFilterInputStream(final InputStream sourceStream) {
		super(sourceStream);
	}

	@Override
	public final long skip(final long length) throws IOException {
		if (length <= 0L) {
			return 0L;
		}
		byte[] buffer = new byte[8192];
		long skipped = 0L;
		while (skipped < length) {
			int readLength = this.read(buffer, 0, (int) Math.min(buffer.length, length - skipped));
			if (readLength < 0) {
				break;
			}
			skipped += readLength;
		}
		return skipped;
	}

	@Override
	public final void close() throws IOException {
		if (this.closed) {
			return;
		}

		try {
			byte[] buffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
			int readLength;
			do {
				readLength = this.read(buffer, 0, buffer.length);
			} while (readLength >= 0);
		} finally {
			this.closed = true;
			super.close();
			this.executeClose();
		}
	}

	/**
	 * <h3 class="en-US">Execute close operate</h3>
	 * <h3 class="zh-CN">执行关闭操作</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when close current input stream</span>
	 *                     <span class="zh-CN">关闭过程中出错</span>
	 */
	protected abstract void executeClose() throws IOException;
}
