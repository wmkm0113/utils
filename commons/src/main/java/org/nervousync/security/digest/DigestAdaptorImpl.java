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

import org.nervousync.beans.security.CipherConfig;
import org.nervousync.commons.Globals;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.StringUtils;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * <h2 class="en-US">Abstract implement class of the digest adapter</h2>
 * <h2 class="zh-CN">摘要算法适配器的抽象实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:46:55 $
 */
public final class DigestAdaptorImpl extends CryptoAdaptor {

	/**
	 * <span class="en-US">Hash-based Message Authentication Code</span>
	 * <span class="zh-CN">基于密钥的消息认证码算法</span>
	 */
	private final boolean macMode;
	/**
	 * <span class="en-US">Message Authentication Code instance</span>
	 * <span class="zh-CN">消息认证码算法实例对象</span>
	 */
	private Mac hmac;
	/**
	 * <span class="en-US">MessageDigest instance</span>
	 * <span class="zh-CN">消息摘要算法实例对象</span>
	 */
	private final MessageDigest digest;

	/**
	 * <h3 class="en-US">Constructor method for the abstract implement class of the digest adapter</h3>
	 * <h3 class="zh-CN">摘要算法适配器的抽象实现类的构造方法</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when initialize adaptor</span>
	 *                         <span class="zh-CN">当初始化适配器时出现异常</span>
	 */
	public DigestAdaptorImpl(final CipherConfig cipherConfig) {
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

	public boolean isMacMode() {
		return this.macMode;
	}

	@Override
	public void append(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		if (dataBytes.length < (position + length)) {
			throw new CryptoException(0x000000150001L);
		}
		if (this.macMode) {
			this.hmac.update(dataBytes, position, length);
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
				return this.hmac.doFinal();
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
			return Arrays.equals(this.macMode ? this.hmac.doFinal() : this.digest.digest(), signature);
		} catch (Exception e) {
			throw new CryptoException(0x000000150001L, e);
		} finally {
			this.reset();
		}
	}

	@Override
	public void reset() throws CryptoException {
		if (this.macMode) {
			this.hmac.reset();
		} else {
			this.digest.reset();
		}
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
	private MessageDigest initDigest(final String algorithm) throws CryptoException {
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
	public void initHmac(final String algorithm, final byte[] keyBytes) throws CryptoException {
		try {
			SecretKey key = new SecretKeySpec(keyBytes, algorithm);
			this.hmac = Mac.getInstance(algorithm);
			this.hmac.init(key);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new CryptoException(0x00000015000DL, e, algorithm);
		}
	}
}
