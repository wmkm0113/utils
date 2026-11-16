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
import org.nervousync.archive.io.zip.AbstractFilterInputStream;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.RawUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * <h2 class="en-US">Decryption input stream</h2>
 * <h2 class="zh-CN">解密输入流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public abstract class CryptoInputStream<T extends CryptoEngine> extends AbstractFilterInputStream {

	/**
	 * <span class="en-US">Crypto engine</span>
	 * <span class="zh-CN">加密解密引擎</span>
	 */
	protected final T engine;
	/**
	 * <span class="en-US">Remaining length</span>
	 * <span class="zh-CN">剩余长度</span>
	 */
	protected long remaining;
	/**
	 * <span class="en-US">Input buffer</span>
	 * <span class="zh-CN">输入缓冲区</span>
	 */
	private final byte[] inputBuffer;
	/**
	 * <span class="en-US">Output buffer</span>
	 * <span class="zh-CN">输出缓冲区</span>
	 */
	private byte[] outputBuffer = CryptoEngine.EMPTY_BYTES.clone();
	/**
	 * <span class="en-US">Output position</span>
	 * <span class="zh-CN">输出偏移值</span>
	 */
	private int outputPosition;
	/**
	 * <span class="en-US">Finished flag</span>
	 * <span class="zh-CN">已完成标记</span>
	 */
	protected boolean finished = false;

	/**
	 * <h3 class="en-US">Constructor method for the decryption input stream</h3>
	 * <h3 class="zh-CN">解密输入流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Current zip file input stream</span>
	 *                     <span class="zh-CN">当前压缩文件输入流</span>
	 * @param remaining    <span class="en-US">Remaining length</span>
	 *                     <span class="zh-CN">剩余长度</span>
	 * @param engine       <span class="en-US">Crypto engine</span>
	 *                     <span class="zh-CN">加密解密引擎</span>
	 */
	protected CryptoInputStream(final InputStream sourceStream, final long remaining, final T engine) {
		super(Objects.requireNonNull(sourceStream, "sourceStream"));
		this.remaining = remaining;
		this.engine = engine;
		this.inputBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
	}

	@Override
	public final int read() throws IOException {
		byte[] singleByte = new byte[1];
		int readLength = this.read(singleByte, 0, 1);
		return readLength < 0 ? -1 : singleByte[0] & 0xFF;
	}

	@Override
	public final int read(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		Objects.requireNonNull(buffer, "buffer");
		Objects.checkFromIndexSize(offset, length, buffer.length);
		this.ensureOpen();
		if (length == 0) {
			return 0;
		}

		int totalRead = 0;

		while (totalRead < length) {
			if (!this.prepareOutput()) {
				break;
			}

			int available = this.outputBuffer.length - this.outputPosition;
			int copyLength = Math.min(length - totalRead, available);

			System.arraycopy(this.outputBuffer, this.outputPosition, buffer, offset + totalRead, copyLength);

			this.outputPosition += copyLength;
			totalRead += copyLength;

			if (this.outputPosition == this.outputBuffer.length) {
				this.releaseOutputBuffer();
			}
		}
		return totalRead == 0 ? -1 : totalRead;
	}

	private boolean prepareOutput() throws IOException {
		while (this.outputPosition >= this.outputBuffer.length) {
			this.releaseOutputBuffer();

			if (this.finished) {
				return false;
			}

			if (this.remaining > 0L) {
				int expectedLength = (int) Math.min(this.inputBuffer.length, this.remaining);

				int readLength = this.in.read(this.inputBuffer, 0, expectedLength);

				if (readLength < 0) {
					throw new EOFException("Unexpected end of encrypted payload");
				}
				if (readLength == 0) {
					continue;
				}

				this.remaining -= readLength;

				this.outputBuffer = this.engine.update(this.inputBuffer, 0, readLength);
			} else {
				this.outputBuffer = this.engine.finish();
				this.finished = true;

				this.afterEngineFinished();
			}

			this.outputPosition = 0;
		}

		return true;
	}

	protected void afterEngineFinished() throws IOException {
	}

	private void releaseOutputBuffer() {
		RawUtils.clear(this.outputBuffer);
		this.outputBuffer = CryptoEngine.EMPTY_BYTES.clone();
		this.outputPosition = 0;
	}

	@Override
	protected void executeClose() throws IOException {
		if (this.closed) {
			return;
		}
		this.engine.close();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public synchronized void mark(final int readLimit) {
		// Not supported
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException("Current stream does not support reset");
	}

	/**
	 * <h3 class="en-US">Confirm that the current stream has not been closed.</h3>
	 * <h3 class="zh-CN">确认当前流未被关闭</h3>
	 *
	 * @throws IOException <span class="en-US">If current stream closed</span>
	 *                     <span class="zh-CN">当前流未被关闭</span>
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
	}
}
