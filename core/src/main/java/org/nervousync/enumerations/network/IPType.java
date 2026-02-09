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
package org.nervousync.enumerations.network;

/**
 * <h2 class="en-US">IP address type Enumerations</h2>
 * <h2 class="zh-CN">IP地址类型枚举</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $Date: Sep 21, 2018 18:25:33 $
 */
@SuppressWarnings("unused")
public enum IPType {
	/**
     * <span class="en-US">IPv4 address</span>
     * <span class="zh-CN">IPv4地址</span>
	 */
	IPv4,
	/**
     * <span class="en-US">IPv6 address</span>
     * <span class="zh-CN">IPv6地址</span>
	 */
	IPv6,
	/**
     * <span class="en-US">Unknown type</span>
     * <span class="zh-CN">未知类型</span>
	 */
	Unknown
}
