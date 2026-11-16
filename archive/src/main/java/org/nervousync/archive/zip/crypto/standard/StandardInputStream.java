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

package org.nervousync.archive.zip.crypto.standard;

import org.nervousync.archive.zip.crypto.CryptoInputStream;
import org.nervousync.commons.Globals;

import java.io.IOException;
import java.io.InputStream;

/**
 * <h2 class="en-US">Standard decryption input stream</h2>
 * <h2 class="zh-CN">标准数据解密输入流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class StandardInputStream extends CryptoInputStream<StandardCryptoEngine> {

	/**
	 * <span class="en-US">Marked remaining length</span>
	 * <span class="zh-CN">标记的剩余长度</span>
	 */
	private long markedRemaining = Globals.DEFAULT_VALUE_INT;

	/**
	 * <h3 class="en-US">Constructor method for the standard decryption input stream</h3>
	 * <h3 class="zh-CN">标准数据解密输入流的构造方法</h3>
	 *
	 * @param sourceStream   <span class="en-US">Current zip file input stream</span>
	 *                       <span class="zh-CN">当前压缩文件输入流</span>
	 * @param storedSize     <span class="en-US">Compressed size</span>
	 *                       <span class="zh-CN">压缩后大小</span>
	 * @param password       <span class="en-US">Password</span>
	 *                       <span class="zh-CN">密码</span>
	 * @param header   <span class="en-US">Decrypt header</span>
	 *                 <span class="zh-CN">解密头</span>
	 */
	public StandardInputStream(final InputStream sourceStream, final long storedSize,
	                           final byte[] password, final byte[] header, final int checkByte) throws IOException {
		super(sourceStream, storedSize, new StandardCryptoEngine(password, header, checkByte));
	}

	@Override
	public synchronized void mark(final int readLimit) {
		this.in.mark(readLimit);
		this.engine.mark();
		this.markedRemaining = this.remaining;
	}

	@Override
	public synchronized void reset() throws IOException {
		this.in.reset();
		this.engine.reset();
		this.remaining = this.markedRemaining;
		this.markedRemaining = Globals.DEFAULT_VALUE_LONG;
	}
}
