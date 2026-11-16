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

import jakarta.annotation.Nonnull;
import org.nervousync.archive.exceptions.ArchiveException;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * <h2 class="en-US">Zip archive entry output stream</h2>
 * <h2 class="zh-CN">Zip 归档条目输出流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ZipEntryOutputStream extends FilterOutputStream {

	/**
	 * <span class="en-US">One byte buffer</span>
	 * <span class="zh-CN">单字节缓冲区</span>
	 */
	private final byte[] oneByte = new byte[1];
	/**
	 * <span class="en-US">CRC calculator</span>
	 * <span class="zh-CN">CRC 计算器</span>
	 */
	private final CRC32 crc = new CRC32();
	/**
	 * <span class="en-US">Uncompressed size</span>
	 * <span class="zh-CN">未压缩大小</span>
	 */
	private long uncompressedSize = 0L;
	/**
	 * <span class="en-US">Abort flag</span>
	 * <span class="zh-CN">取消标记</span>
	 */
	private boolean aborted = false;
	/**
	 * <span class="en-US">Closed flag</span>
	 * <span class="zh-CN">关闭标记</span>
	 */
	private boolean closed = false;

	/**
	 * <h3 class="en-US">Constructor method for the zip archive entry output stream</h3>
	 * <h3 class="zh-CN">Zip 归档条目输出流的构造方法</h3>
	 *
	 * @param out <span class="en-US">Source output stream</span>
	 *            <span class="zh-CN">源输出流</span>
	 */
	public ZipEntryOutputStream(final OutputStream out) {
		super(out);
	}

	@Override
	public void write(final int b) throws IOException {
		this.oneByte[0] = (byte) b;
		this.write(this.oneByte, 0, 1);
	}

	@Override
	public void write(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, buffer.length);
		if (this.closed || this.aborted) {
			throw new ArchiveException(0x0000001A0040L);
		}
		try {
			long newSize = Math.addExact(this.uncompressedSize, length);
			this.out.write(buffer, offset, length);
			this.crc.update(buffer, offset, length);
			this.uncompressedSize = newSize;
		} catch (IOException | RuntimeException e) {
			this.aborted = true;
			throw e;
		}
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		try {
			this.out.close();
		} catch (IOException | RuntimeException e) {
			this.aborted = true;
			throw e;
		}
	}

	/**
	 * <h3 class="en-US">Abort current operate</h3>
	 * <h3 class="zh-CN">终止当前操作</h3>
	 */
	public void abort() {
		this.aborted = true;
	}

	/**
	 * <h3 class="en-US">Getter method for the CRC calculate result</h3>
	 * <h3 class="zh-CN">CRC 计算结果的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">CRC calculate result</span>
	 * <span class="zh-CN">CRC 计算结果</span>
	 */
	public long getCrc() {
		return this.crc.getValue();
	}

	/**
	 * <h3 class="en-US">Getter method for the uncompressed size</h3>
	 * <h3 class="zh-CN">未压缩大小的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Uncompressed size</span>
	 * <span class="zh-CN">未压缩大小</span>
	 */
	public long getUncompressedSize() {
		return this.uncompressedSize;
	}

	/**
	 * <h3 class="en-US">Getter method for the abort flag</h3>
	 * <h3 class="zh-CN">取消标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Abort flag</span>
	 * <span class="zh-CN">取消标记</span>
	 */
	public boolean isAborted() {
		return this.aborted;
	}
}
