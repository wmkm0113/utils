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
 * <h2 class="en-US">SNMP Authentication Type Enumerations</h2>
 * <h2 class="zh-CN">SNMP身份验证类型枚举</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 27, 2017 13:57:59 $
 */
public enum SNMPAuthType {
	/**
     * <span class="en-US">No Authentication</span>
     * <span class="zh-CN">无验证</span>
	 */
	NOAUTH_NOPRIV,
	/**
     * <span class="en-US">Authentication with No Private</span>
     * <span class="zh-CN">验证不使用私有验证</span>
	 */
	AUTH_NOPRIV,
	/**
     * <span class="en-US">Authentication with Private</span>
     * <span class="zh-CN">验证并使用私有验证</span>
	 */
	AUTH_PRIV
}
