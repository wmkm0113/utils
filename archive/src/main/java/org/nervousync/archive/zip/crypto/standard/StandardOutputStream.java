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

import org.nervousync.archive.zip.crypto.CryptoOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <h2 class="en-US">Standard encryption output stream</h2>
 * <h2 class="zh-CN">标准数据加密输出流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class StandardOutputStream extends CryptoOutputStream<StandardCryptoEngine> {

	/**
	 * <h3 class="en-US">Constructor method for the standard encryption output stream</h3>
	 * <h3 class="zh-CN">标准数据加密输出流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source output stream</span>
	 *                     <span class="zh-CN">源输出流</span>
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @param crc          <span class="en-US">CRC value</span>
	 *                     <span class="zh-CN">CRC 值</span>
	 * @throws IOException <span class="en-US">If an error occurs when write header information</span>
	 *                     <span class="zh-CN">写入加密头数据时出错</span>
	 */
	public StandardOutputStream(final OutputStream sourceStream, final byte[] password, final int crc)
			throws IOException {
		super(sourceStream, new StandardCryptoEngine(password, crc));
		this.writeHeader(this.engine.getHeaderBytes().clone());
	}
}
