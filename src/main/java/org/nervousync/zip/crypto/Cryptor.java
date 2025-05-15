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

package org.nervousync.zip.crypto;

import jakarta.annotation.Nonnull;
import org.nervousync.exceptions.zip.ZipException;

/**
 * <h2 class="en-US">ZIP file encrypt/decrypt interface</h2>
 * <h2 class="zh-CN">ZIP文件加密/解密接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Dec 2, 2017 10:34:24 AM $
 */
public interface Cryptor {

	/**
	 * <h3 class="en-US">Process data</h3>
	 * <h3 class="zh-CN">处理数据</h3>
	 *
	 * @param buff <span class="en-US">Binary data that needs to be processed</span>
	 *             <span class="zh-CN">需要处理的二进制数据</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing data</span>
	 *                      <span class="zh-CN">处理数据过程中出现异常</span>
	 */
	default void process(@Nonnull final byte[] buff) throws ZipException {
		this.process(buff, 0, buff.length);
	}

	/**
	 * <h3 class="en-US">Process data</h3>
	 * <h3 class="zh-CN">处理数据</h3>
	 *
	 * @param buff  <span class="en-US">Binary data that needs to be processed</span>
	 *              <span class="zh-CN">需要处理的二进制数据</span>
	 * @param start <span class="en-US">Begin index of the processing data</span>
	 *              <span class="zh-CN">处理数据的起始坐标</span>
	 * @param len   <span class="en-US">Data length of the processing data</span>
	 *              <span class="zh-CN">处理数据的长度</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing data</span>
	 *                      <span class="zh-CN">处理数据过程中出现异常</span>
	 */
	void process(final byte[] buff, final int start, final int len) throws ZipException;
}
