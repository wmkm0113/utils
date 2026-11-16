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
import org.nervousync.enumerations.crypto.CryptoMode;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * <h2 class="en-US">Zip crypto engine interface</h2>
 * <h2 class="zh-CN">Zip 的加密解密引擎接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public abstract class CryptoEngine implements Closeable {

	protected static final byte[] EMPTY_BYTES = new byte[0];

	/**
	 * <span class="en-US">Crypto mode</span>
	 * <span class="zh-CN">加密解密模式</span>
	 */
	private final CryptoMode mode;
	/**
	 * <span class="en-US">Finished flag</span>
	 * <span class="zh-CN">已完成标记</span>
	 */
	protected boolean finished = false;
	/**
	 * <span class="en-US">Input stream closed flag</span>
	 * <span class="zh-CN">输入流关闭标记</span>
	 */
	protected boolean closed = false;

	/**
	 * <h3 class="en-US">Constructor method for the Zip crypto engine interface</h3>
	 * <h3 class="zh-CN">Zip 的加密解密引擎接口的构造方法</h3>
	 *
	 * @param mode <span class="en-US">Crypto mode</span>
	 *             <span class="zh-CN">加密解密模式</span>
	 */
	protected CryptoEngine(@Nonnull final CryptoMode mode) {
		this.mode = mode;
	}

	/**
	 * <h3 class="en-US">Getter method for the crypto mode</h3>
	 * <h3 class="zh-CN">加密解密模式的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Crypto mode</span>
	 * <span class="zh-CN">加密解密模式</span>
	 */
	public CryptoMode getMode() {
		return this.mode;
	}

	/**
	 * <h3 class="en-US">Process input data</h3>
	 * <h3 class="zh-CN">处理输入数据</h3>
	 *
	 * @param input  <span class="en-US">Input data</span>
	 *               <span class="zh-CN">输入数据</span>
	 * @param offset <span class="en-US">Offset value</span>
	 *               <span class="zh-CN">偏移值</span>
	 * @param length <span class="en-US">Data length</span>
	 *               <span class="zh-CN">数据长度</span>
	 * @return <span class="en-US">Processed data</span>
	 * <span class="zh-CN">处理后的数据</span>
	 * @throws IOException <span class="en-US">Engine closed or already finished</span>
	 *                     <span class="zh-CN">引擎已经关闭或已经处理完成</span>
	 */
	public final byte[] update(@Nonnull final byte[] input, final int offset, final int length) throws IOException {
		Objects.requireNonNull(input, "input");
		Objects.checkFromIndexSize(offset, length, input.length);
		this.ensureActive();

		if (length == 0) {
			return EMPTY_BYTES;
		}

		byte[] result = this.process(input, offset, length);
		return result == null ? EMPTY_BYTES : result;
	}

	/**
	 * <h3 class="en-US">Process finish data</h3>
	 * <h3 class="zh-CN">处理最终数据</h3>
	 *
	 * @return <span class="en-US">Processed data</span>
	 * <span class="zh-CN">处理后的数据</span>
	 * @throws IOException <span class="en-US">Engine closed or already finished</span>
	 *                     <span class="zh-CN">引擎已经关闭或已经处理完成</span>
	 */
	public final byte[] finish() throws IOException {
		this.ensureOpen();

		if (this.finished) {
			return EMPTY_BYTES;
		}

		byte[] result = this.finishEngine();
		this.finished = true;
		return result == null ? EMPTY_BYTES : result;
	}

	/**
	 * <h3 class="en-US">Process data</h3>
	 * <h3 class="zh-CN">处理数据</h3>
	 *
	 * @param buffer <span class="en-US">Buffer data</span>
	 *               <span class="zh-CN">缓冲数据</span>
	 * @param offset <span class="en-US">Offset value</span>
	 *               <span class="zh-CN">偏移值</span>
	 * @param length <span class="en-US">Data length</span>
	 *               <span class="zh-CN">数据长度</span>
	 * @return <span class="en-US">Processed data length</span>
	 * <span class="zh-CN">处理的数据长度</span>
	 */
	protected abstract byte[] process(@Nonnull final byte[] buffer, final int offset, final int length)
			throws IOException;

	/**
	 * <h3 class="en-US">Execute finish operate</h3>
	 * <h3 class="zh-CN">执行最终操作</h3>
	 *
	 * @return <span class="en-US">Processed data length</span>
	 * <span class="zh-CN">处理的数据长度</span>
	 */
	protected abstract byte[] finishEngine() throws IOException;

	/**
	 * <h3 class="en-US">Ensure active status</h3>
	 * <h3 class="zh-CN">确认激活</h3>
	 *
	 * @throws IOException <span class="en-US">Engine closed or already finished</span>
	 *                     <span class="zh-CN">引擎已经关闭或已经处理完成</span>
	 */
	private void ensureActive() throws IOException {
		this.ensureOpen();
		if (this.finished) {
			throw new IOException("Crypto engine already finished");
		}
	}

	/**
	 * <h3 class="en-US">Ensure open status</h3>
	 * <h3 class="zh-CN">确认打开状态</h3>
	 *
	 * @throws IOException <span class="en-US">Engine closed</span>
	 *                     <span class="zh-CN">引擎已经关闭或</span>
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Crypto engine closed");
		}
	}
}
