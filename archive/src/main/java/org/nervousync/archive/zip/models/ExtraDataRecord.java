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

package org.nervousync.archive.zip.models;

/**
 * <h2 class="en-US">Entry extra data record</h2>
 * <h2 class="zh-CN">条目扩展数据记录</h2>
 *
 * @param headerId  <span class="en-US">Header ID</span>
 *                  <span class="zh-CN">文件头标识码</span>
 * @param dataBytes <span class="en-US">Binary data bytes</span>
 *                  <span class="zh-CN">二进制数据</span>
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public record ExtraDataRecord(int headerId, byte[] dataBytes) {

	/**
	 * <h2 class="en-US">Constructor method for the entry extra data record</h2>
	 * <h2 class="zh-CN">条目扩展数据记录的构造方法</h2>
	 *
	 * @param headerId  <span class="en-US">Header ID</span>
	 *                  <span class="zh-CN">文件头标识码</span>
	 * @param dataBytes <span class="en-US">Binary data bytes</span>
	 *                  <span class="zh-CN">二进制数据</span>
	 */
	public ExtraDataRecord(final int headerId, final byte[] dataBytes) {
		this.headerId = headerId;
		this.dataBytes = dataBytes.clone();
	}

	/**
	 * <h3 class="en-US">Getter method for the header ID</h3>
	 * <h3 class="zh-CN">文件头标识码的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Header ID</span>
	 * <span class="zh-CN">文件头标识码</span>
	 */
	@Override
	public int headerId() {
		return this.headerId;
	}

	/**
	 * <h3 class="en-US">Getter method for the binary data bytes</h3>
	 * <h3 class="zh-CN">二进制数据的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Binary data bytes</span>
	 * <span class="zh-CN">二进制数据</span>
	 */
	@Override
	public byte[] dataBytes() {
		return this.dataBytes;
	}
}
