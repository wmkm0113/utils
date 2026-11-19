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

package org.nervousync.archive.enumerations.zip;

/**
 * <h2 class="en-US">Enumeration value of the AES vendor</h2>
 * <h2 class="zh-CN">AES 版本枚举值</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public enum AesVendor {

	AE_1(1),
	AE_2(2);

	/**
	 * <span class="en-US">Version number</span>
	 * <span class="zh-CN">版本号</span>
	 */
	private final int version;

	/**
	 * <h3 class="en-US">Constructor method for the enumeration value of the AES vendor</h3>
	 * <h3 class="zh-CN">AES 版本枚举值的构造方法</h3>
	 *
	 * @param version <span class="en-US">Version number</span>
	 *                <span class="zh-CN">版本号</span>
	 */
	AesVendor(final int version) {
		this.version = version;
	}

	/**
	 * <h3 class="en-US">Getter method for the version number</h3>
	 * <h3 class="zh-CN">版本号的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Version number</span>
	 * <span class="zh-CN">版本号</span>
	 */
	public int getVersion() {
		return this.version;
	}
}
