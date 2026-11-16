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

package org.nervousync.security.digest;

import jakarta.annotation.Nonnull;
import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.security.CryptoAdaptor;

/**
 * <h2 class="en-US">Digest algorithm adaptor interface</h2>
 * <h2 class="zh-CN">摘要算法适配器接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public interface DigestProvider {

	/**
	 * <h3 class="en-US">Initialize data operator instance object.</h3>
	 * <h3 class="zh-CN">初始化数据操作器实例对象</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @return <span class="en-US">Data operator instance object</span>
	 * <span class="zh-CN">数据操作器实例对象</span>
	 */
	CryptoAdaptor initDigest(@Nonnull final CipherConfig cipherConfig, final CipherKey cipherKey);

}
