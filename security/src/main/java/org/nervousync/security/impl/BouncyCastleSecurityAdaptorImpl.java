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
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.*;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.jcajce.provider.digest.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.nervousync.beans.crypto.CipherConfig;
import org.nervousync.beans.crypto.CipherKey;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.security.core.BaseSecurityAdaptorImpl;
import org.nervousync.security.digest.BaseDigestAdaptorImpl;
import org.nervousync.utils.core.StringUtils;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.Security;

/**
 * <h2 class="en-US">Security adaptor implement class using BouncyCastle library</h2>
 * <h2 class="zh-CN">使用BC库的安全适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public final class BouncyCastleSecurityAdaptorImpl extends BaseSecurityAdaptorImpl {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * <h3 class="en-US">Constructor method for the security adaptor implement class using BouncyCastle library</h3>
	 * <h3 class="zh-CN">使用BC库的安全适配器实现类的构造方法</h3>
	 */
	public BouncyCastleSecurityAdaptorImpl() {
		super("BC");
	}

	@Override
	public CryptoAdaptor initDigest(@Nonnull final CipherConfig cipherConfig, final CipherKey cipherKey) {
		return new DigestAdapterImpl(cipherConfig, cipherKey);
	}

	@Override
	public CryptoAdaptor initCipher(@Nonnull final CipherConfig cipherConfig, @Nonnull final CryptoMode cryptoMode,
	                                @Nonnull final CipherKey cipherKey) {
		return cipherConfig.isAsymmetric()
				? new AsymmetricCryptoAdaptorImpl(cipherConfig, cryptoMode, cipherKey)
				: new SymmetricCryptoAdaptorImpl(cipherConfig, cryptoMode, cipherKey);
	}

	/**
	 * <h3 class="en-US">Convert data bytes from C1|C2|C3 to C1|C3|C2</h3>
	 * <h3 class="zh-CN">转换字节数组从C1|C2|C3到C1|C3|C2</h3>
	 *
	 * @param dataBytes <span class="en-US">C1|C2|C3 data bytes</span>
	 *                  <span class="zh-CN">C1|C2|C3格式字节数组</span>
	 * @return <span class="en-US">C1|C3|C2 data bytes</span>
	 * <span class="zh-CN">C1|C3|C2格式字节数组</span>
	 */
	public static byte[] C1C2C3toC1C3C2(final byte[] dataBytes) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(dataBytes.length);
		byteArrayOutputStream.write(dataBytes, 0, 65);
		byteArrayOutputStream.write(dataBytes, 97, dataBytes.length - 97);
		byteArrayOutputStream.write(dataBytes, 65, 32);
		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * <h3 class="en-US">Convert data bytes from C1|C3|C2 to C1|C2|C3</h3>
	 * <h3 class="zh-CN">转换字节数组从C1|C2|C3到C1|C3|C2</h3>
	 *
	 * @param dataBytes <span class="en-US">C1|C3|C2 data bytes</span>
	 *                  <span class="zh-CN">C1|C3|C2格式字节数组</span>
	 * @return <span class="en-US">C1|C2|C3 data bytes</span>
	 * <span class="zh-CN">C1|C2|C3格式字节数组</span>
	 */
	public static byte[] C1C3C2toC1C2C3(final byte[] dataBytes) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(dataBytes.length);
		byteArrayOutputStream.write(dataBytes, 0, 65);
		byteArrayOutputStream.write(dataBytes, dataBytes.length - 32, 32);
		byteArrayOutputStream.write(dataBytes, 65, dataBytes.length - 97);
		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * <h2 class="en-US">Digest adapter implement class using BouncyCastle library</h2>
	 * <h2 class="zh-CN">使用BC库的摘要算法适配器实现类</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:46:55 $
	 */
	public static final class DigestAdapterImpl extends BaseDigestAdaptorImpl {

		/**
		 * <span class="en-US">Message Authentication Code instance</span>
		 * <span class="zh-CN">消息认证码算法实例对象</span>
		 */
		private Mac hmac;

		/**
		 * <h3 class="en-US">Constructor method for the digest adapter implement class using BouncyCastle library</h3>
		 * <h3 class="zh-CN">使用BC库的摘要算法适配器实现类的构造方法</h3>
		 *
		 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
		 *                     <span class="zh-CN">签名验证算法配置信息</span>
		 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
		 *                     <span class="zh-CN">签名验证密钥实例对象</span>
		 * @throws CryptoException <span class="en-US">If an error occurs when initialize adaptor</span>
		 *                         <span class="zh-CN">当初始化适配器时出现异常</span>
		 */
		public DigestAdapterImpl(@Nonnull final CipherConfig cipherConfig, final CipherKey cipherKey) throws CryptoException {
			super(cipherConfig, cipherKey);
		}

		/**
		 * <h3 class="en-US">Retrieve current mac size</h3>
		 * <h3 class="zh-CN">读取当前消息认证码的长度</h3>
		 *
		 * @return <span class="en-US">Mac size</span>
		 * <span class="zh-CN">消息认证码的长度</span>
		 */
		public int macLength() {
			return this.macMode ? this.hmac.getMacSize() : Globals.DEFAULT_VALUE_INT;
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
			byte[] calcResult = new byte[this.hmac.getMacSize()];
			this.hmac.doFinal(calcResult, 0);
			return calcResult;
		}

		@Override
		protected void resetHmac() throws CryptoException {
			this.hmac.reset();
		}

		@Override
		protected MessageDigest initDigest(final String algorithm) throws CryptoException {
			if (StringUtils.isEmpty(algorithm)) {
				throw new CryptoException(0x00000015000DL, algorithm);
			}
			switch (algorithm.toUpperCase()) {
				case "MD5":
					return new MD5.Digest();
				case "SHA1":
					return new SHA1.Digest();
				case "SHA-224":
					return new SHA224.Digest();
				case "SHA-256":
					return new SHA256.Digest();
				case "SHA-384":
					return new SHA384.Digest();
				case "SHA-512":
					return new SHA512.Digest();
				case "SHA-512/224":
					return new SHA512.DigestT224();
				case "SHA-512/256":
					return new SHA512.DigestT256();
				case "SHA3-224":
					return new SHA3.Digest224();
				case "SHA3-256":
					return new SHA3.Digest256();
				case "SHA3-384":
					return new SHA3.Digest384();
				case "SHA3-512":
					return new SHA3.Digest512();
				case "SHAKE128":
					return new SHA3.DigestShake128_256();
				case "SHAKE256":
					return new SHA3.DigestShake256_512();
				case "SM3":
					return new SM3.Digest();
				default:
					throw new CryptoException(0x00000015000DL, algorithm);
			}
		}

		@Override
		protected void initHmac(final String algorithm, final byte[] keyBytes) {
			if (algorithm.contains("MD5")) {
				this.hmac = new HMac(new MD5Digest());
			} else if (algorithm.contains("SHA1")) {
				this.hmac = new HMac(new SHA1Digest());
			} else if (algorithm.contains("SHA3-")) {
				switch (algorithm.toUpperCase()) {
					case "HMACSHA3-224":
						this.hmac = new HMac(new SHA3Digest(224));
						break;
					case "HMACSHA3-256":
						this.hmac = new HMac(new SHA3Digest(256));
						break;
					case "HMACSHA3-384":
						this.hmac = new HMac(new SHA3Digest(384));
						break;
					case "HMACSHA3-512":
						this.hmac = new HMac(new SHA3Digest(512));
						break;
					default:
						throw new CryptoException(0x00000015000DL, algorithm);
				}
			} else if (algorithm.contains("SM3")) {
				this.hmac = new HMac(new SM3Digest());
			} else if (algorithm.contains("SHA")) {
				switch (algorithm.toUpperCase()) {
					case "HMACSHA224":
						this.hmac = new HMac(new SHA224Digest());
						break;
					case "HMACSHA256":
						this.hmac = new HMac(new SHA256Digest());
						break;
					case "HMACSHA384":
						this.hmac = new HMac(new SHA384Digest());
						break;
					case "HMACSHA512":
						this.hmac = new HMac(new SHA512Digest());
						break;
					case "HMACSHA512/224":
						this.hmac = new HMac(new SHA512tDigest(224));
						break;
					case "HMACSHA512/256":
						this.hmac = new HMac(new SHA512tDigest(256));
						break;
					default:
						throw new CryptoException(0x00000015000DL, algorithm);
				}
			} else {
				throw new CryptoException(0x00000015000DL, algorithm);
			}
		}
	}

	/**
	 * <h2 class="en-US">Abstract asymmetric crypto adapter class</h2>
	 * <h2 class="zh-CN">非对称加密解密适配器的抽象类</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 12:27:33 $
	 */
	public static final class AsymmetricCryptoAdaptorImpl
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
		public AsymmetricCryptoAdaptorImpl(final CipherConfig cipherConfig, final CryptoMode cryptoMode,
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
	public static final class SymmetricCryptoAdaptorImpl
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
		public SymmetricCryptoAdaptorImpl(final CipherConfig cipherConfig, final CryptoMode cryptoMode,
		                                  final CipherKey cipherKey) throws CryptoException {
			super("BC", cipherConfig, cryptoMode, cipherKey);
		}
	}
}
