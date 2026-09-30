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

package org.nervousync.security.password;

/**
 * <h2 class="en-US">Password encoder interface</h2>
 * <h2 class="zh-CN">密码编码器接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2026 11:23:13 $
 */
public interface PasswordEncoder {

	/**
	 * <h3 class="en-US">Encode the given password</h3>
	 * <h3 class="zh-CN">对给定的密码进行编码</h3>
	 *
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @return <span class="en-US">Encoded string</span>
	 * <span class="zh-CN">编码后的字符串</span>
	 */
	String encode(final char[] password);

	/**
	 * <h3 class="en-US">Verify the given password is match the given encoded password</h3>
	 * <h3 class="zh-CN">验证给定的密码是否与给定的编码一致</h3>
	 *
	 * @param password        <span class="en-US">Password</span>
	 *                        <span class="zh-CN">密码</span>
	 * @param encodedPassword <span class="en-US">Encoded string</span>
	 *                        <span class="zh-CN">编码后的字符串</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	boolean verify(final char[] password, final String encodedPassword);

	/**
	 * <h3 class="en-US">Check the given encoded string needs upgrade</h3>
	 * <h3 class="zh-CN">检查给定的编码是否需要升级</h3>
	 *
	 * @param encodedPassword <span class="en-US">Encoded string</span>
	 *                        <span class="zh-CN">编码后的字符串</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	boolean needsUpgrade(final String encodedPassword);
}
