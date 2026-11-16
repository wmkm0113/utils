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

package org.nervousync.archive.zip.crypto.aes;

import org.nervousync.archive.zip.crypto.CryptoOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <h2 class="en-US">AES encryption output stream</h2>
 * <h2 class="zh-CN">AES 数据加密输出流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class AesOutputStream extends CryptoOutputStream<AesCryptoEngine> {

	/**
	 * <h3 class="en-US">Constructor method for the AES encryption output stream</h3>
	 * <h3 class="zh-CN">AES 数据加密输出流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source output stream</span>
	 *                     <span class="zh-CN">源输出流</span>
	 * @param strength     <span class="en-US">AES strength</span>
	 *                     <span class="zh-CN">AES 强度</span>
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public AesOutputStream(final OutputStream sourceStream, final int strength, final byte[] password)
			throws IOException {
		super(sourceStream, AesCryptoEngine.newEngine(strength, password));
		this.writeHeader(this.engine.getHeaderBytes());
	}

	@Override
	protected void writeTrailer() throws IOException {
		this.writeResult(this.engine.calculateAuthentication());
	}
}
