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

import jakarta.annotation.Nonnull;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.beans.security.CipherParameters;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * <h2 class="en-US">Crypto algorithm adaptor implement class using BouncyCastle library</h2>
 * <h2 class="zh-CN">使用 BC 库的加密解密算法适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public final class BouncyCastleCryptoProviderImpl implements CryptoProvider {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private final LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Override
	public CryptoAdaptor initCipher(@Nonnull final CipherConfig cipherConfig, @Nonnull final CryptoMode cryptoMode,
	                                @Nonnull final CipherKey cipherKey, final CipherParameters parameters) {
		return cipherConfig.isAsymmetric()
				? new AsymmetricCryptoAdaptorImpl(cipherConfig, cryptoMode, cipherKey)
				: new SymmetricCryptoAdaptorImpl(cipherConfig, cryptoMode, cipherKey, parameters);
	}

	@Override
	public PublicKey publicKey(final String algorithm, final byte[] keyBytes) {
		try {
			return KeyFactory.getInstance(algorithm, "BC").generatePublic(new X509EncodedKeySpec(keyBytes));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Stack_Message_Error", e);
			}
		}
		return null;
	}

	@Override
	public PrivateKey privateKey(final String algorithm, final byte[] keyBytes) {
		if (keyBytes == null || keyBytes.length == 0) {
			return null;
		}
		try {
			return KeyFactory.getInstance(algorithm, "BC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Stack_Message_Error", e);
			}
			return null;
		}
	}

	@Override
	public String provider() {
		return "BC";
	}

	@Override
	public KeyPair keyPair(final String algorithm, final int keySize,
	                       final String randomAlgorithm, final String stdName) {
		if (keySize % 128 != 0) {
			this.logger.error("Key_Size_Invalid_Error");
			return null;
		}

		KeyPair keyPair = null;
		try {
			SecureRandom secureRandom;
			if (StringUtils.isEmpty(randomAlgorithm)) {
				this.logger.warn("Random_Algorithm_Default_Warn");
				secureRandom = new SecureRandom();
			} else {
				secureRandom = SecureRandom.getInstance(randomAlgorithm);
			}

			//	Initialize keyPair instance
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm, "BC");
			if ("EC".equalsIgnoreCase(algorithm)) {
				ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(stdName);
				keyPairGenerator.initialize(ecGenParameterSpec, secureRandom);
			} else {
				keyPairGenerator.initialize(keySize, secureRandom);
			}
			keyPair = keyPairGenerator.generateKeyPair();
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
			this.logger.error("Init_Key_Pair_Generator_Error");
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Stack_Message_Error", e);
			}
		}
		return keyPair;
	}

	/**
	 * <h2 class="en-US">Abstract asymmetric crypto adapter class</h2>
	 * <h2 class="zh-CN">非对称加密解密适配器的抽象类</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 12:27:33 $
	 */
	private static final class AsymmetricCryptoAdaptorImpl
			extends org.nervousync.security.crypto.AsymmetricCryptoAdaptorImpl {

		/**
		 * <h3 class="en-US">Constructor for AsymmetricCryptoAdapter</h3>
		 * <h3 class="zh-CN">非对称加密解密适配器的抽象类的构造方法</h3>
		 *
		 * @param cipherConfig <span class="en-US">Cipher configure</span>
		 *                     <span class="zh-CN">密码设置</span>
		 * @param cryptoMode   <span class="en-US">Crypto mode</span>
		 *                     <span class="zh-CN">加密解密模式</span>
		 * @param cipherKey    <span class="en-US">Crypto key</span>
		 *                     <span class="zh-CN">加密解密密钥</span>
		 * @throws CryptoException <span class="en-US">If an error occurs when initialize cipher</span>
		 *                         <span class="zh-CN">当初始化加密解密实例对象时出现异常</span>
		 */
		AsymmetricCryptoAdaptorImpl(final CipherConfig cipherConfig, final CryptoMode cryptoMode,
		                            final CipherKey cipherKey) throws CryptoException {
			super("BC", cipherConfig, cryptoMode, cipherKey);
		}
	}

	/**
	 * <h2 class="en-US">Abstract symmetric crypto adapter class</h2>
	 * <h2 class="zh-CN">对称加密解密适配器的抽象类</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 13:16:24 $
	 */
	private static final class SymmetricCryptoAdaptorImpl
			extends org.nervousync.security.crypto.SymmetricCryptoAdaptorImpl {

		/**
		 * <h3 class="en-US">Constructor for SymmetricCryptoAdapter</h3>
		 * <h3 class="zh-CN">对称加密解密适配器的抽象类的构造方法</h3>
		 *
		 * @param cipherConfig <span class="en-US">Cipher configure</span>
		 *                     <span class="zh-CN">密码设置</span>
		 * @param cryptoMode   <span class="en-US">Crypto mode</span>
		 *                     <span class="zh-CN">加密解密模式</span>
		 * @param cipherKey    <span class="en-US">Crypto key</span>
		 *                     <span class="zh-CN">加密解密密钥</span>
		 * @throws CryptoException <span class="en-US">If an error occurs when initialize cipher</span>
		 *                         <span class="zh-CN">当初始化加密解密实例对象时出现异常</span>
		 */
		SymmetricCryptoAdaptorImpl(final CipherConfig cipherConfig, final CryptoMode cryptoMode,
		                           final CipherKey cipherKey, final CipherParameters parameters) throws CryptoException {
			super("BC", cipherConfig, cryptoMode, cipherKey, parameters);
		}
	}
}
