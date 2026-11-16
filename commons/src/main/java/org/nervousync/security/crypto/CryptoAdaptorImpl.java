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

import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.beans.security.CipherParameters;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.security.CryptoAdaptor;

import javax.crypto.Cipher;

/**
 * <h2 class="en-US">Abstract class of the crypto adapter</h2>
 * <h2 class="zh-CN">加密解密适配器的抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:30:24 $
 */
public abstract class CryptoAdaptorImpl extends CryptoAdaptor {

	/**
	 * <span class="en-US">Cipher configure</span>
	 * <span class="zh-CN">密码设置</span>
	 */
	protected CipherConfig cipherConfig = null;
	/**
	 * <span class="en-US">Crypto mode</span>
	 * <span class="zh-CN">加密解密模式</span>
	 */
	protected CryptoMode cryptoMode = null;
	/**
	 * <span class="en-US">Crypto key</span>
	 * <span class="zh-CN">加密解密密钥</span>
	 */
	protected CipherKey cipherKey = null;
	/**
	 * <span class="en-US">Cipher parameter</span>
	 * <span class="zh-CN">加密解密参数</span>
	 */
	protected CipherParameters parameters = null;
	/**
	 * <span class="en-US">Cipher instance</span>
	 * <span class="zh-CN">加密解密实例对象</span>
	 * The Cipher.
	 */
	protected Cipher cipher;


	/**
	 * <h3 class="en-US">Configure current crypto adaptor</h3>
	 * <h3 class="zh-CN">配置加密解密适配器</h3>
	 *
	 * @param cipherConfig <span class="en-US">Cipher configure</span>
	 *                     <span class="zh-CN">密码设置</span>
	 * @param cryptoMode   <span class="en-US">Crypto mode</span>
	 *                     <span class="zh-CN">加密解密模式</span>
	 * @param cipherKey    <span class="en-US">Crypto key</span>
	 *                     <span class="zh-CN">加密解密密钥</span>
	 * @param parameters   <span class="en-US">Crypto parameters</span>
	 *                     <span class="zh-CN">加密解密参数</span>
	 */
	public final void config(final CipherConfig cipherConfig, final CryptoMode cryptoMode,
	                         final CipherKey cipherKey, final CipherParameters parameters) {
		this.cipherConfig = cipherConfig;
		this.cryptoMode = cryptoMode;
		this.cipherKey = cipherKey;
		this.parameters = parameters;
		this.reset();
	}
}
