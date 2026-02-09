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
package org.nervousync.enumerations.snmp.auth;

/**
 * <h2 class="en-US">SNMP Authentication Private Protocol Enumerations</h2>
 * <h2 class="zh-CN">SNMP身份验证私有协议枚举</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 27, 2017 14:17:22 $
 */
public enum SNMPPrivProtocol {
	/**
     * <span class="en-US">DES Private Protocol</span>
     * <span class="zh-CN">DES私有协议</span>
	 */
	PrivDES,
	/**
     * <span class="en-US">TripleDES Private Protocol</span>
     * <span class="zh-CN">3DES私有协议</span>
	 */
	Priv3DES
}
