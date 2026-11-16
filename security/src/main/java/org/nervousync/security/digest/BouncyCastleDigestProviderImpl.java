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
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.*;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.digest.*;
import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.commons.Globals;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.StringUtils;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * <h2 class="en-US">Digest algorithm adaptor implement class using BouncyCastle library</h2>
 * <h2 class="zh-CN">使用 BC 库的摘要算法适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public final class BouncyCastleDigestProviderImpl implements DigestProvider {

	@Override
	public CryptoAdaptor initDigest(@Nonnull final CipherConfig cipherConfig, final CipherKey cipherKey) {
		DigestAdapterImpl adaptor = new DigestAdapterImpl(cipherConfig);
		if (adaptor.isMacMode()) {
			adaptor.initHmac(cipherConfig.algorithm(), cipherKey.getKeyBytes());
		}
		return adaptor;
	}


	/**
	 * <h2 class="en-US">Digest adapter implement class using BouncyCastle library</h2>
	 * <h2 class="zh-CN">使用BC库的摘要算法适配器实现类</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:46:55 $
	 */
	private static final class DigestAdapterImpl extends CryptoAdaptor {

		/**
		 * <span class="en-US">Hash-based Message Authentication Code</span>
		 * <span class="zh-CN">基于密钥的消息认证码算法</span>
		 */
		private final boolean macMode;
		/**
		 * <span class="en-US">MessageDigest instance</span>
		 * <span class="zh-CN">消息摘要算法实例对象</span>
		 */
		private final MessageDigest digest;

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
		 * @throws CryptoException <span class="en-US">If an error occurs when initialize adaptor</span>
		 *                         <span class="zh-CN">当初始化适配器时出现异常</span>
		 */
		public DigestAdapterImpl(@Nonnull final CipherConfig cipherConfig) throws CryptoException {
			if (StringUtils.isEmpty(cipherConfig.algorithm())) {
				throw new CryptoException(0x00000015000DL);
			}
			this.macMode = cipherConfig.algorithm().toUpperCase().contains("HMAC");
			if (this.macMode) {
				this.digest = null;
			} else {
				this.digest = this.initDigest(cipherConfig.algorithm());
			}
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
		public void append(final byte[] dataBytes, final int position, final int length) throws CryptoException {
			if (dataBytes.length < (position + length)) {
				throw new CryptoException(0x000000150001L);
			}
			if (this.macMode) {
				this.appendHmac(dataBytes, position, length);
			} else {
				this.digest.update(dataBytes, position, length);
			}
		}

		@Override
		public byte[] finish(final byte[] dataBytes, final int position, final int length) throws CryptoException {
			if (dataBytes.length < (position + length)) {
				throw new CryptoException(0x000000150001L);
			}
			try {
				this.append(dataBytes, position, length);
				if (this.macMode) {
					return this.finishHmac();
				} else {
					return this.digest.digest();
				}
			} catch (Exception e) {
				throw new CryptoException(0x000000150001L, e);
			} finally {
				this.reset();
			}
		}

		@Override
		public boolean verify(final byte[] signature) throws CryptoException {
			try {
				return Arrays.equals(this.macMode ? this.finishHmac() : this.digest.digest(), signature);
			} catch (Exception e) {
				throw new CryptoException(0x000000150001L, e);
			} finally {
				this.reset();
			}
		}

		/**
		 * <h3 class="en-US">Getter method for the hash-based message authentication code</h3>
		 * <h3 class="zh-CN">基于密钥的消息认证码算法的 Getter 方法</h3>
		 *
		 * @return
		 * <span class="en-US">Hash-based Message Authentication Code</span>
		 * <span class="zh-CN">基于密钥的消息认证码算法</span>
		 */
		public boolean isMacMode() {
			return this.macMode;
		}

		@Override
		public void reset() throws CryptoException {
			if (this.macMode) {
				this.resetHmac();
			} else {
				this.digest.reset();
			}
		}

		public void appendHmac(final byte[] dataBytes, final int position, final int length) throws CryptoException {
			if (dataBytes.length < (position + length)) {
				throw new CryptoException(0x000000150001L);
			}
			this.hmac.update(dataBytes, position, length);
		}

		private byte[] finishHmac() throws CryptoException {
			byte[] calcResult = new byte[this.hmac.getMacSize()];
			this.hmac.doFinal(calcResult, 0);
			return calcResult;
		}

		private void resetHmac() throws CryptoException {
			this.hmac.reset();
		}

		private MessageDigest initDigest(final String algorithm) throws CryptoException {
			if (StringUtils.isEmpty(algorithm)) {
				throw new CryptoException(0x00000015000DL, algorithm);
			}
			return switch (algorithm.toUpperCase()) {
				case "MD5" -> new MD5.Digest();
				case "SHA1" -> new SHA1.Digest();
				case "SHA-224" -> new SHA224.Digest();
				case "SHA-256" -> new SHA256.Digest();
				case "SHA-384" -> new SHA384.Digest();
				case "SHA-512" -> new SHA512.Digest();
				case "SHA-512/224" -> new SHA512.DigestT224();
				case "SHA-512/256" -> new SHA512.DigestT256();
				case "SHA3-224" -> new SHA3.Digest224();
				case "SHA3-256" -> new SHA3.Digest256();
				case "SHA3-384" -> new SHA3.Digest384();
				case "SHA3-512" -> new SHA3.Digest512();
				case "SHAKE128" -> new SHA3.DigestShake128_256();
				case "SHAKE256" -> new SHA3.DigestShake256_512();
				case "SM3" -> new SM3.Digest();
				default -> throw new CryptoException(0x00000015000DL, algorithm);
			};
		}

		public void initHmac(final String algorithm, final byte[] keyBytes) {
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
			this.hmac.init(new KeyParameter(keyBytes));
		}
	}
}
