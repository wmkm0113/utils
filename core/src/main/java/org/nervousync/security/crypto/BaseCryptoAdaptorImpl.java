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

package org.nervousync.security.crypto;

import org.nervousync.beans.crypto.CipherConfig;
import org.nervousync.beans.crypto.CipherKey;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.security.CryptoAdaptor;

import javax.crypto.Cipher;

/**
 * <h2 class="en-US">Abstract basic crypto adapter class</h2>
 * <h2 class="zh-CN">加密解密适配器的抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:30:24 $
 */
public abstract class BaseCryptoAdaptorImpl extends CryptoAdaptor {

	/**
	 * <span class="en-US">Cipher configure</span>
	 * <span class="zh-CN">密码设置</span>
	 */
	protected final CipherConfig cipherConfig;
	/**
	 * <span class="en-US">Crypto mode</span>
	 * <span class="zh-CN">加密解密模式</span>
	 */
	protected final CryptoMode cryptoMode;
	/**
	 * <span class="en-US">Crypto key</span>
	 * <span class="zh-CN">加密解密密钥</span>
	 */
	protected final CipherKey cipherKey;
	/**
	 * <span class="en-US">Cipher instance</span>
	 * <span class="zh-CN">加密解密实例对象</span>
	 * The Cipher.
	 */
	protected Cipher cipher;


	/**
	 * <h3 class="en-US">Constructor for BaseCryptoAdapter</h3>
	 * <h3 class="zh-CN">加密解密适配器的构造方法</h3>
	 *
	 * @param cipherConfig <span class="en-US">Cipher configure</span>
	 *                     <span class="zh-CN">密码设置</span>
	 * @param cryptoMode   <span class="en-US">Crypto mode</span>
	 *                     <span class="zh-CN">加密解密模式</span>
	 * @param cipherKey    <span class="en-US">Crypto key</span>
	 *                     <span class="zh-CN">加密解密密钥</span>
	 */
	protected BaseCryptoAdaptorImpl(final CipherConfig cipherConfig, final CryptoMode cryptoMode,
	                                final CipherKey cipherKey) {
		this.cipherConfig = cipherConfig;
		this.cryptoMode = cryptoMode;
		this.cipherKey = cipherKey;
	}
}
