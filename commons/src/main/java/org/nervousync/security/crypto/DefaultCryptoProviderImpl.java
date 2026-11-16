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
import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.beans.security.CipherParameters;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * <h2 class="en-US">Default crypto provider implement class</h2>
 * <h2 class="zh-CN">默认加密解密算法适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public final class DefaultCryptoProviderImpl implements CryptoProvider {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private final LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());

	@Override
	public CryptoAdaptor initCipher(@Nonnull final CipherConfig cipherConfig, @Nonnull final CryptoMode cryptoMode,
	                                @Nonnull final CipherKey cipherKey, final CipherParameters parameters) {
		return cipherConfig.isAsymmetric()
				? new AsymmetricAdaptorImpl(cipherConfig, cryptoMode, cipherKey)
				: new SymmetricAdaptorImpl(cipherConfig, cryptoMode, cipherKey, parameters);
	}

	@Override
	public PublicKey publicKey(final String algorithm, final byte[] keyBytes) {
		try {
			return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(keyBytes));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
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
			return KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Stack_Message_Error", e);
			}
			return null;
		}
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
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
			if ("EC".equalsIgnoreCase(algorithm)) {
				ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(stdName);
				keyPairGenerator.initialize(ecGenParameterSpec, secureRandom);
			} else {
				keyPairGenerator.initialize(keySize, secureRandom);
			}
			keyPair = keyPairGenerator.generateKeyPair();
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			this.logger.error("Init_Key_Pair_Generator_Error");
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Stack_Message_Error", e);
			}
		}
		return keyPair;
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
		                     @Nonnull final CipherKey cipherKey, final CipherParameters parameters) {
			super(Globals.DEFAULT_VALUE_STRING, cipherConfig, cryptoMode, cipherKey, parameters);
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
