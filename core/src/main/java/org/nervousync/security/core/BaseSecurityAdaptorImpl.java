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

package org.nervousync.security.core;

import jakarta.annotation.Nonnull;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.SecurityAdaptor;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * <h2 class="en-US">Abstract implement class of security adaptor</h2>
 * <h2 class="zh-CN">安全适配器的抽象实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public abstract class BaseSecurityAdaptorImpl implements SecurityAdaptor {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	protected final LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());

	private final String providerName;

	protected BaseSecurityAdaptorImpl(@Nonnull final String providerName) {
		this.providerName = providerName;
	}

	@Override
	public final PublicKey publicKey(final String algorithm, final byte[] keyBytes) {
		try {
			return StringUtils.isEmpty(this.providerName)
					? KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(keyBytes))
					: KeyFactory.getInstance(algorithm, this.providerName).generatePublic(new X509EncodedKeySpec(keyBytes));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Stack_Message_Error", e);
			}
		}
		return null;
	}

	@Override
	public final PrivateKey privateKey(final String algorithm, final byte[] keyBytes) {
		if (keyBytes == null || keyBytes.length == 0) {
			return null;
		}
		try {
			return StringUtils.isEmpty(this.providerName)
					? KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(keyBytes))
					: KeyFactory.getInstance(algorithm, this.providerName).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Stack_Message_Error", e);
			}
			return null;
		}
	}

	@Override
	public final byte[] symmetricKey(final String algorithm, final int keySize, final String randomAlgorithm)
			throws CryptoException {
		if (StringUtils.isEmpty(algorithm)) {
			throw new CryptoException(0x00000015000DL, algorithm);
		}

		try {
			KeyGenerator keyGenerator = StringUtils.isEmpty(this.providerName)
					? KeyGenerator.getInstance(algorithm)
					: KeyGenerator.getInstance(algorithm, this.providerName);
			switch (algorithm.toUpperCase()) {
				case "AES":
				case "RC4":
					SecureRandom secureRandom =
							StringUtils.isEmpty(randomAlgorithm)
									? new SecureRandom()
									: SecureRandom.getInstance(randomAlgorithm);
					keyGenerator.init(keySize, secureRandom);
					break;
				case "SM4":
				case "RC2":
				case "RC5":
				case "RC6":
					keyGenerator.init(keySize, new SecureRandom());
					break;
				case "DES":
				case "DESEDE":
				case "BLOWFISH":
					break;
				default:
					throw new CryptoException(0x00000015000DL, algorithm);
			}
			SecretKey secretKey = keyGenerator.generateKey();
			return secretKey.getEncoded();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new CryptoException(0x000000150009L, e);
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
			KeyPairGenerator keyPairGenerator = StringUtils.isEmpty(this.providerName)
					? KeyPairGenerator.getInstance(algorithm)
					: KeyPairGenerator.getInstance(algorithm, this.providerName);
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
}
