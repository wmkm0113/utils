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

package org.nervousync.security.crc;

import org.nervousync.security.CryptoAdaptor;

/**
 * <h2 class="en-US">Symmetric CRC crypto adapter abstract class</h2>
 * <h2 class="zh-CN">CRC摘要算法适配器的抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 13:50:21 $
 */
public abstract class CRCAdaptor extends CryptoAdaptor {

	/**
	 * <h3 class="en-US">Returns the current CRC value without resetting the internal state.</h3>
	 * <h3 class="zh-CN">返回当前 CRC 值，不重置内部状态</h3>
	 *
	 * @return <span class="en-US">The current CRC value</span>
	 * <span class="zh-CN">当前 CRC 值</span>
	 */
	public abstract long getValue();
}
