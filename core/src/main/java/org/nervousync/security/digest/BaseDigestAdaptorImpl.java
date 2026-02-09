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

import org.nervousync.beans.crypto.CipherConfig;
import org.nervousync.beans.crypto.CipherKey;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.core.StringUtils;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * <h2 class="en-US">Abstract implement class of the digest adapter</h2>
 * <h2 class="zh-CN">摘要算法适配器的抽象实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:46:55 $
 */
public abstract class BaseDigestAdaptorImpl extends CryptoAdaptor {

	/**
	 * <span class="en-US">Hash-based Message Authentication Code</span>
	 * <span class="zh-CN">基于密钥的消息认证码算法</span>
	 */
	protected final boolean macMode;
	/**
	 * <span class="en-US">MessageDigest instance</span>
	 * <span class="zh-CN">消息摘要算法实例对象</span>
	 */
	protected final MessageDigest digest;

	/**
	 * <h3 class="en-US">Constructor method for the abstract implement class of the digest adapter</h3>
	 * <h3 class="zh-CN">摘要算法适配器的抽象实现类的构造方法</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when initialize adaptor</span>
	 *                         <span class="zh-CN">当初始化适配器时出现异常</span>
	 */
	protected BaseDigestAdaptorImpl(final CipherConfig cipherConfig, final CipherKey cipherKey) {
		if (StringUtils.isEmpty(cipherConfig.getAlgorithm())) {
			throw new CryptoException(0x00000015000DL);
		}
		this.macMode = cipherConfig.getAlgorithm().toUpperCase().contains("HMAC");
		if (this.macMode) {
			this.initHmac(cipherConfig.getAlgorithm(), cipherKey.getKeyBytes());
			this.digest = null;
		} else {
			this.digest = this.initDigest(cipherConfig.getAlgorithm());
		}
	}

	@Override
	public final void append(final byte[] dataBytes, final int position, final int length) throws CryptoException {
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
	public final byte[] finish(final byte[] dataBytes, final int position, final int length) throws CryptoException {
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
	public final boolean verify(final byte[] signature) throws CryptoException {
		try {
			return Arrays.equals(this.macMode ? this.finishHmac() : this.digest.digest(), signature);
		} catch (Exception e) {
			throw new CryptoException(0x000000150001L, e);
		} finally {
			this.reset();
		}
	}

	@Override
	public final void reset() throws CryptoException {
		if (this.macMode) {
			this.resetHmac();
		} else {
			this.digest.reset();
		}
	}

	/**
	 * <h3 class="en-US">Append parts of the given binary data array to the current adapter</h3>
	 * <h3 class="zh-CN">追加给定的二进制字节数组到当前适配器</h3>
	 *
	 * @param dataBytes <span class="en-US">binary data array</span>
	 *                  <span class="zh-CN">二进制字节数组</span>
	 * @param position  <span class="en-US">Data begin position</span>
	 *                  <span class="zh-CN">数据起始坐标</span>
	 * @param length    <span class="en-US">Length of data append</span>
	 *                  <span class="zh-CN">追加的数据长度</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when process data</span>
	 *                         <span class="zh-CN">当处理数据时出现异常</span>
	 */
	protected abstract void appendHmac(final byte[] dataBytes, final int position, final int length) throws CryptoException;

	/**
	 * <h3 class="en-US">Obtain the HMAC calculate result</h3>
	 * <h3 class="zh-CN">获取HMAC计算结果</h3>
	 *
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when process data</span>
	 *                         <span class="zh-CN">当处理数据时出现异常</span>
	 */
	protected abstract byte[] finishHmac() throws CryptoException;

	/**
	 * <h3 class="en-US">Reset current adapter</h3>
	 * <h3 class="zh-CN">重置当前适配器</h3>
	 *
	 * @throws CryptoException <span class="en-US">If an error occurs when process data</span>
	 *                         <span class="zh-CN">当处理数据时出现异常</span>
	 */
	protected abstract void resetHmac() throws CryptoException;

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
	protected abstract MessageDigest initDigest(final String algorithm) throws CryptoException;

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
	protected abstract void initHmac(final String algorithm, final byte[] keyBytes) throws CryptoException;
}
