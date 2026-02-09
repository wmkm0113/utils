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

package org.nervousync.security.impl;

import jakarta.annotation.Nonnull;
import org.nervousync.beans.crypto.CipherConfig;
import org.nervousync.beans.crypto.CipherKey;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.security.core.BaseSecurityAdaptorImpl;
import org.nervousync.security.crypto.AsymmetricCryptoAdaptorImpl;
import org.nervousync.security.crypto.SymmetricCryptoAdaptorImpl;
import org.nervousync.security.digest.BaseDigestAdaptorImpl;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

/**
 * <h2 class="en-US">Default security adaptor implement class</h2>
 * <h2 class="zh-CN">默认安全适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public final class DefaultSecurityAdaptorImpl extends BaseSecurityAdaptorImpl {

	public DefaultSecurityAdaptorImpl() {
		super(Globals.DEFAULT_VALUE_STRING);
	}

	@Override
	public CryptoAdaptor initDigest(@Nonnull final CipherConfig cipherConfig, final CipherKey cipherKey) {
		return new DigestAdaptorImpl(cipherConfig, cipherKey);
	}

	@Override
	public CryptoAdaptor initCipher(@Nonnull final CipherConfig cipherConfig, @Nonnull final CryptoMode cryptoMode,
	                                @Nonnull final CipherKey cipherKey) {
		if (cipherConfig.isAsymmetric()) {
			return new AsymmetricAdaptorImpl(cipherConfig, cryptoMode, cipherKey);
		} else {
			return new SymmetricAdaptorImpl(cipherConfig, cryptoMode, cipherKey);
		}
	}

	/**
	 * <h2 class="en-US">Digest adapter implement class using SunJCE</h2>
	 * <h2 class="zh-CN">使用SunJCE的摘要算法适配器实现类</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:46:55 $
	 */
	private static final class DigestAdaptorImpl extends BaseDigestAdaptorImpl {

		/**
		 * <span class="en-US">Message Authentication Code instance</span>
		 * <span class="zh-CN">消息认证码算法实例对象</span>
		 */
		private Mac hmac;

		/**
		 * <h3 class="en-US">Constructor method for the adapter implement class using SunJCE</h3>
		 * <h3 class="zh-CN">使用SunJCE的摘要算法适配器实现类的构造方法</h3>
		 *
		 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
		 *                     <span class="zh-CN">签名验证算法配置信息</span>
		 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
		 *                     <span class="zh-CN">签名验证密钥实例对象</span>
		 * @throws CryptoException <span class="en-US">If an error occurs when initialize adaptor</span>
		 *                         <span class="zh-CN">当初始化适配器时出现异常</span>
		 */
		DigestAdaptorImpl(final CipherConfig cipherConfig, final CipherKey cipherKey) {
			super(cipherConfig, cipherKey);
		}

		@Override
		public void appendHmac(final byte[] dataBytes, final int position, final int length) throws CryptoException {
			if (dataBytes.length < (position + length)) {
				throw new CryptoException(0x000000150001L);
			}
			this.hmac.update(dataBytes, position, length);
		}

		@Override
		protected byte[] finishHmac() throws CryptoException {
			return this.hmac.doFinal();
		}

		@Override
		protected void resetHmac() throws CryptoException {
			this.hmac.reset();
		}

		/**
		 * <h3 class="en-US">Retrieve current mac size</h3>
		 * <h3 class="zh-CN">读取当前消息认证码的长度</h3>
		 *
		 * @return <span class="en-US">Mac size</span>
		 * <span class="zh-CN">消息认证码的长度</span>
		 */
		@Override
		public int macLength() {
			return this.macMode ? this.hmac.getMacLength() : Globals.DEFAULT_VALUE_INT;
		}

		/**
		 * <h3 class="en-US">Abstract method for initialize MessageDigest instance</h3>
		 * <h3 class="zh-CN">抽象方法用于初始化消息摘要算法适配器实例对象</h3>
		 *
		 * @param algorithm <span class="en-US">Cipher Algorithm</span>
		 *                  <span class="zh-CN">密码算法</span>
		 * @return <span class="en-US">Initialized MessageDigest instance</span>
		 * <span class="zh-CN">初始化的消息摘要算法适配器</span>
		 * @throws CryptoException <span class="en-US">If an error occurs when initialize MessageDigest</span>
		 *                         <span class="zh-CN">当初始化消息摘要算法适配器实例对象时出现异常</span>
		 */
		protected MessageDigest initDigest(final String algorithm) throws CryptoException {
			try {
				return MessageDigest.getInstance(algorithm);
			} catch (NoSuchAlgorithmException e) {
				throw new CryptoException(0x00000015000DL, e, algorithm);
			}
		}

		/**
		 * <h3 class="en-US">Abstract method for initialize Hmac instance</h3>
		 * <h3 class="zh-CN">抽象方法用于初始化消息认证码适配器实例对象</h3>
		 *
		 * @param algorithm <span class="en-US">Cipher Algorithm</span>
		 *                  <span class="zh-CN">密码算法</span>
		 * @param keyBytes  <span class="en-US">Hmac key data bytes</span>
		 *                  <span class="zh-CN">消息认证码算法密钥数据数组</span>
		 * @throws CryptoException <span class="en-US">If an error occurs when initialize Hmac instance</span>
		 *                         <span class="zh-CN">当初始化消息认证码算法适配器实例对象时出现异常</span>
		 */
		protected void initHmac(final String algorithm, final byte[] keyBytes) throws CryptoException {
			try {
				SecretKey key = new SecretKeySpec(keyBytes, algorithm);
				this.hmac = Mac.getInstance(algorithm);
				this.hmac.init(key);
			} catch (NoSuchAlgorithmException | InvalidKeyException e) {
				throw new CryptoException(0x00000015000DL, e, algorithm);
			}
		}
	}

	/**
	 * <h2 class="en-US">Symmetric crypto adapter implement class</h2>
	 * <h2 class="zh-CN">对称加密解密适配器的实现类</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 13:16:24 $
	 */
	private static final class SymmetricAdaptorImpl extends SymmetricCryptoAdaptorImpl {

		SymmetricAdaptorImpl(@Nonnull final CipherConfig cipherConfig, @Nonnull final CryptoMode cryptoMode,
		                     @Nonnull final CipherKey cipherKey) {
			super(Globals.DEFAULT_VALUE_STRING, cipherConfig, cryptoMode, cipherKey);
		}
	}

	/**
	 * <h2 class="en-US">Asymmetric crypto adapter implement class</h2>
	 * <h2 class="zh-CN">非对称加密解密适配器的实现类</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 12:27:33 $
	 */
	private static final class AsymmetricAdaptorImpl extends AsymmetricCryptoAdaptorImpl {

		AsymmetricAdaptorImpl(@Nonnull final CipherConfig cipherConfig, @Nonnull final CryptoMode cryptoMode,
		                      @Nonnull final CipherKey cipherKey) {
			super(Globals.DEFAULT_VALUE_STRING, cipherConfig, cryptoMode, cipherKey);
		}
	}
}
