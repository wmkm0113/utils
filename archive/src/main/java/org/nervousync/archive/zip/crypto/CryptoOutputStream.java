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

package org.nervousync.archive.zip.crypto;

import jakarta.annotation.Nonnull;
import org.nervousync.utils.commons.RawUtils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * <h2 class="en-US">Encryption output stream</h2>
 * <h2 class="zh-CN">加密输出流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public abstract class CryptoOutputStream<T extends CryptoEngine> extends FilterOutputStream {

	/**
	 * <span class="en-US">Crypto engine</span>
	 * <span class="zh-CN">加密解密引擎</span>
	 */
	protected final T engine;
	/**
	 * <span class="en-US">One byte buffer</span>
	 * <span class="zh-CN">单字节缓冲区</span>
	 */
	private final byte[] oneByteBuffer = new byte[1];
	/**
	 * <span class="en-US">Finished flag</span>
	 * <span class="zh-CN">已完成标记</span>
	 */
	protected boolean finished = false;
	/**
	 * <span class="en-US">Closed flag</span>
	 * <span class="zh-CN">关闭标记</span>
	 */
	protected boolean closed = false;
	/**
	 * <span class="en-US">Failed flag</span>
	 * <span class="zh-CN">失败标记</span>
	 */
	private boolean failed;

	/**
	 * <h3 class="en-US">Constructor method for the encryption output stream</h3>
	 * <h3 class="zh-CN">加密输出流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source output stream</span>
	 *                     <span class="zh-CN">源输出流</span>
	 * @param engine       <span class="en-US">Crypto engine</span>
	 *                     <span class="zh-CN">加密解密引擎</span>
	 */
	protected CryptoOutputStream(final OutputStream sourceStream, final T engine) {
		super(sourceStream);
		this.engine = engine;
	}

	@Override
	public void write(final int b) throws IOException {
		this.oneByteBuffer[0] = (byte) b;
		this.write(this.oneByteBuffer, 0, 1);
	}

	@Override
	public void write(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		Objects.requireNonNull(buffer, "buffer");
		Objects.checkFromIndexSize(offset, length, buffer.length);
		this.ensureWritable();

		if (length == 0) {
			return;
		}

		try {
			this.writeResult(this.engine.update(buffer, offset, length));
		} catch (IOException | RuntimeException e) {
			this.failed = true;
			throw e;
		}
	}

	/**
	 * 完成加密，不关闭底层输出流。
	 */
	public final void finish() throws IOException {
		this.ensureOpen();

		if (this.failed) {
			throw new IOException("Crypto output stream has failed");
		}
		if (this.finished) {
			return;
		}

		try {
			this.writeResult(this.engine.finish());
			this.writeTrailer();
			this.engine.close();
			this.finished = true;
		} catch (IOException | RuntimeException e) {
			this.failed = true;
			throw e;
		}
	}

	/**
	 * <h3 class="en-US">Write trailer data</h3>
	 * <h3 class="zh-CN">写入尾部数据</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when write trailer data</span>
	 *                     <span class="zh-CN">写入数据时出错</span>
	 */
	protected void writeTrailer() throws IOException {
	}

	/**
	 * <h3 class="en-US">Write encryption header data</h3>
	 * <h3 class="zh-CN">写入加密头数据</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when write trailer data</span>
	 *                     <span class="zh-CN">写入数据时出错</span>
	 */
	protected final void writeHeader(final byte[] header) throws IOException {
		try {
			this.writeResult(header);
		} catch (IOException | RuntimeException e) {
			this.failed = true;
			try {
				this.engine.close();
			} catch (IOException | RuntimeException closeError) {
				if (closeError != e) {
					e.addSuppressed(closeError);
				}
			}
			throw e;
		}
	}

	/**
	 * <h3 class="en-US">Write result data</h3>
	 * <h3 class="zh-CN">写入结果数据</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when write trailer data</span>
	 *                     <span class="zh-CN">写入数据时出错</span>
	 */
	protected final void writeResult(final byte[] result) throws IOException {
		try {
			if (result.length != 0) {
				this.out.write(result, 0, result.length);
			}
		} finally {
			RawUtils.clear(result);
		}
	}

	@Override
	public final void flush() throws IOException {
		this.ensureOpen();

		if (this.failed) {
			throw new IOException("Crypto output stream has failed");
		}

		try {
			this.out.flush();
		} catch (IOException | RuntimeException e) {
			this.failed = true;
			throw e;
		}
	}

	@Override
	public final void close() throws IOException {
		if (this.closed) {
			return;
		}

		IOException failure = null;

		try {
			if (!this.failed) {
				this.finish();
			}
		} catch (IOException | RuntimeException e) {
			failure = e instanceof IOException ? (IOException) e : new IOException(e);
		}

		this.closed = true;
		RawUtils.clear(this.oneByteBuffer);

		try {
			this.engine.close();
		} catch (IOException | RuntimeException e) {
			failure = merge(failure, e);
		}

		try {
			this.out.close();
		} catch (IOException | RuntimeException e) {
			failure = merge(failure, e);
		}

		if (failure != null) {
			throw failure;
		}
	}

	/**
	 * <h3 class="en-US">Ensure current output stream not closed</h3>
	 * <h3 class="zh-CN">确认当前输出流未关闭</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when process check</span>
	 *                     <span class="zh-CN">执行检查时出错</span>
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Crypto output stream is closed");
		}
	}

	/**
	 * <h3 class="en-US">Ensure current output stream is writable</h3>
	 * <h3 class="zh-CN">确认当前输出流可写数据</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when process check</span>
	 *                     <span class="zh-CN">执行检查时出错</span>
	 */
	private void ensureWritable() throws IOException {
		this.ensureOpen();
		if (this.failed) {
			throw new IOException("Crypto output stream has failed");
		}
		if (this.finished) {
			throw new IOException("Crypto output stream is finished");
		}
	}

	/**
	 * <h3 class="en-US">Merge exception</h3>
	 * <h3 class="zh-CN">合并异常</h3>
	 *
	 * @param previous <span class="en-US">Current exception</span>
	 *                 <span class="zh-CN">已有异常</span>
	 * @param cause    <span class="en-US">Merge exception</span>
	 *                 <span class="zh-CN">要合并的异常</span>
	 * @return <span class="en-US">Merged exception</span>
	 * <span class="zh-CN">合并后的异常</span>
	 */
	private static IOException merge(final IOException previous, final Exception cause) {
		IOException next = cause instanceof IOException ? (IOException) cause : new IOException(cause);
		if (previous == null) {
			return next;
		}
		if (previous != next) {
			previous.addSuppressed(next);
		}
		return previous;
	}
}
