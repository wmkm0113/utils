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

package org.nervousync.beans.crypto;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.security.SecurityUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

/**
 * <h2 class="en-US">Cipher key define</h2>
 * <h2 class="zh-CN">加密解密密钥定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:32:08 $
 */
public final class CipherKey {

	/**
	 * <span class="en-US">Enumeration value of algorithm</span>
	 * <span class="zh-CN">算法枚举值</span>
	 */
	private final String algorithm;
	/**
	 * <span class="en-US">Key size</span>
	 * <span class="zh-CN">密钥长度</span>
	 */
	private final int keySize;
	/**
	 * <span class="en-US">Key data bytes</span>
	 * <span class="zh-CN">密钥字节数组</span>
	 */
	private final byte[] keyBytes;
	/**
	 * <span class="en-US">Random algorithm</span>
	 * <span class="zh-CN">随机数算法</span>
	 */
	private final String randomAlgorithm;
	/**
	 * <span class="en-US">Third party crypto provider name</span>
	 * <span class="zh-CN">第三方加密库提供商名称</span>
	 */
	private final String provider;

	/**
	 * <h3 class="en-US">Constructor for CipherKey</h3>
	 * <h3 class="zh-CN">加密解密密钥定义的构造方法</h3>
	 *
	 * @param algorithm <span class="en-US">Enumeration value of algorithm</span>
	 *                  <span class="zh-CN">算法枚举值</span>
	 * @param keyBytes  <span class="en-US">Key data bytes</span>
	 *                  <span class="zh-CN">密钥字节数组</span>
	 */
	public CipherKey(@Nonnull final String algorithm, @Nonnull final byte[] keyBytes) {
		this(algorithm, Globals.DEFAULT_VALUE_INT, keyBytes, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Constructor for CipherKey</h3>
	 * <h3 class="zh-CN">加密解密密钥定义的构造方法</h3>
	 *
	 * @param algorithm       <span class="en-US">Enumeration value of algorithm</span>
	 *                        <span class="zh-CN">算法枚举值</span>
	 * @param keySize         <span class="en-US">Key size</span>
	 *                        <span class="zh-CN">密钥长度</span>
	 * @param keyBytes        <span class="en-US">Key data bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 */
	public CipherKey(final String algorithm, final int keySize, final byte[] keyBytes, final String randomAlgorithm) {
		this(algorithm, keySize, keyBytes, randomAlgorithm, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Constructor for CipherKey</h3>
	 * <h3 class="zh-CN">加密解密密钥定义的构造方法</h3>
	 *
	 * @param algorithm <span class="en-US">Enumeration value of algorithm</span>
	 *                  <span class="zh-CN">算法枚举值</span>
	 * @param keyBytes  <span class="en-US">Key data bytes</span>
	 *                  <span class="zh-CN">密钥字节数组</span>
	 * @param provider  <span class="en-US">Third party crypto provider name</span>
	 *                  <span class="zh-CN">第三方加密库提供商名称</span>
	 */
	public CipherKey(@Nonnull final String algorithm, @Nonnull final byte[] keyBytes, final String provider) {
		this(algorithm, Globals.DEFAULT_VALUE_INT, keyBytes, Globals.DEFAULT_VALUE_STRING, provider);
	}

	/**
	 * <h3 class="en-US">Constructor for CipherKey</h3>
	 * <h3 class="zh-CN">加密解密密钥定义的构造方法</h3>
	 *
	 * @param algorithm       <span class="en-US">Enumeration value of algorithm</span>
	 *                        <span class="zh-CN">算法枚举值</span>
	 * @param keySize         <span class="en-US">Key size</span>
	 *                        <span class="zh-CN">密钥长度</span>
	 * @param keyBytes        <span class="en-US">Key data bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param provider        <span class="en-US">Third party crypto provider name</span>
	 *                        <span class="zh-CN">第三方加密库提供商名称</span>
	 */
	public CipherKey(final String algorithm, final int keySize, final byte[] keyBytes,
	                 final String randomAlgorithm, final String provider) {
		this.algorithm = algorithm;
		this.keySize = keySize;
		this.keyBytes = keyBytes;
		this.randomAlgorithm = randomAlgorithm;
		this.provider = provider;
	}

	/**
	 * <h3 class="en-US">Getter method for Key data bytes</h3>
	 * <h3 class="zh-CN">密钥字节数组的Getter方法</h3>
	 *
	 * @return <span class="en-US">Key data bytes</span>
	 * <span class="zh-CN">密钥字节数组</span>
	 */
	public byte[] getKeyBytes() {
		return this.keyBytes;
	}

	public Key generateKey(final CryptoMode cryptoMode) throws CryptoException {
		switch (this.algorithm) {
			case "AES":
			case "Blowfish":
			case "RC2":
			case "RC5":
			case "RC6":
				return new SecretKeySpec(this.keyBytes, this.algorithm);
			case "DES":
				try {
					DESKeySpec desKeySpec = new DESKeySpec(this.keyBytes);
					SecretKeyFactory keyFactory = StringUtils.isEmpty(this.provider)
							? SecretKeyFactory.getInstance("DES")
							: SecretKeyFactory.getInstance("DES", this.provider);
					return keyFactory.generateSecret(desKeySpec);
				} catch (Exception e) {
					throw new CryptoException(0x00000015000BL, e);
				}
			case "RSA":
				switch (cryptoMode) {
					case ENCRYPT:
					case VERIFY:
						return SecurityUtils.publicKey("RSA", this.keyBytes);
					default:
						return SecurityUtils.privateKey("RSA", this.keyBytes);
				}
			case "SM2":
				switch (cryptoMode) {
					case ENCRYPT:
					case VERIFY:
						return SecurityUtils.publicKey("EC", this.keyBytes);
					default:
						return SecurityUtils.privateKey("EC", this.keyBytes);
				}
			case "DESede":
				try {
					DESedeKeySpec keySpec = new DESedeKeySpec(this.keyBytes);
					SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
					return keyFactory.generateSecret(keySpec);
				} catch (Exception e) {
					throw new CryptoException(0x00000015000BL, e);
				}
			case "RC4":
			case "SM4":
				try {
					KeyGenerator keyGenerator = KeyGenerator.getInstance(this.algorithm, this.provider);
					SecureRandom secureRandom =
							StringUtils.isEmpty(this.randomAlgorithm)
									? new SecureRandom()
									: SecureRandom.getInstance(this.randomAlgorithm);
					secureRandom.setSeed(this.keyBytes);
					keyGenerator.init(this.keySize, secureRandom);
					return keyGenerator.generateKey();
				} catch (Exception e) {
					throw new CryptoException(0x00000015000BL, e);
				}
			default:
				throw new CryptoException(0x00000015000BL);
		}
	}
}
