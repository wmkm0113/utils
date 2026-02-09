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

package org.nervousync.security;

import jakarta.annotation.Nonnull;
import org.nervousync.beans.crypto.CipherConfig;
import org.nervousync.beans.crypto.CipherKey;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * <h2 class="en-US">Security adaptor interface</h2>
 * <h2 class="zh-CN">安全适配器接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public interface SecurityAdaptor {

	/**
	 * <h3 class="en-US">Calculate digest value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的摘要值</h3>
	 *
	 * @param name   <span class="en-US">Digest algorithm name</span>
	 *               <span class="zh-CN">摘要算法名</span>
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	byte[] digest(@Nonnull final String name, @Nonnull final Object source);

	/**
	 * <h3 class="en-US">Calculate Hash-based message authentication code value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的密钥散列消息认证码</h3>
	 *
	 * @param name     <span class="en-US">Digest algorithm name</span>
	 *                 <span class="zh-CN">摘要算法名</span>
	 * @param keyBytes <span class="en-US">Byte array of passcode</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	byte[] hmac(@Nonnull final String name, @Nonnull final byte[] keyBytes, @Nonnull final Object source);

	/**
	 * <h3 class="en-US">Initialize data operator instance object.</h3>
	 * <h3 class="zh-CN">初始化数据操作器实例对象</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @return <span class="en-US">Data operator instance object</span>
	 * <span class="zh-CN">数据操作器实例对象</span>
	 */
	CryptoAdaptor initDigest(@Nonnull final CipherConfig cipherConfig, final CipherKey cipherKey);

	/**
	 * <h3 class="en-US">Generate a data encryptor instance object.</h3>
	 * <h3 class="zh-CN">生成数据加密器实例对象</h3>
	 *
	 * @param cipherConfig <span class="en-US">Encrypt cipher config instance object</span>
	 *                     <span class="zh-CN">加密算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Encrypt cipher key instance object</span>
	 *                     <span class="zh-CN">加密密钥实例对象</span>
	 * @return <span class="en-US">Data encryptor instance object</span>
	 * <span class="zh-CN">数据加密器实例对象</span>
	 */
	default CryptoAdaptor encryptor(@Nonnull final CipherConfig cipherConfig, @Nonnull final CipherKey cipherKey) {
		return this.initCipher(cipherConfig, CryptoMode.ENCRYPT, cipherKey);
	}

	/**
	 * <h3 class="en-US">Generate a data decryptor instance object.</h3>
	 * <h3 class="zh-CN">生成数据解密器实例对象</h3>
	 *
	 * @param cipherConfig <span class="en-US">Decrypt cipher config instance object</span>
	 *                     <span class="zh-CN">解密算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Decrypt cipher key instance object</span>
	 *                     <span class="zh-CN">解密密钥实例对象</span>
	 * @return <span class="en-US">Data decryptor instance object</span>
	 * <span class="zh-CN">数据解密器实例对象</span>
	 */
	default CryptoAdaptor decryptor(@Nonnull final CipherConfig cipherConfig, @Nonnull final CipherKey cipherKey) {
		return this.initCipher(cipherConfig, CryptoMode.DECRYPT, cipherKey);
	}

	/**
	 * <h3 class="en-US">Generate a data signer instance object.</h3>
	 * <h3 class="zh-CN">生成数据签名器实例对象</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature cipher config instance object</span>
	 *                     <span class="zh-CN">签名算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature cipher key instance object</span>
	 *                     <span class="zh-CN">签名密钥实例对象</span>
	 * @return <span class="en-US">Data signer instance object</span>
	 * <span class="zh-CN">数据签名器实例对象</span>
	 */
	default CryptoAdaptor signer(@Nonnull final CipherConfig cipherConfig, @Nonnull final CipherKey cipherKey) {
		return this.initCipher(cipherConfig, CryptoMode.SIGNATURE, cipherKey);
	}

	/**
	 * <h3 class="en-US">Generate a data signature verifier instance object.</h3>
	 * <h3 class="zh-CN">生成数据签名验证器实例对象</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @return <span class="en-US">Data signature verifier instance object</span>
	 * <span class="zh-CN">数据签名验证器实例对象</span>
	 */
	default CryptoAdaptor verifier(@Nonnull final CipherConfig cipherConfig, @Nonnull final CipherKey cipherKey) {
		return this.initCipher(cipherConfig, CryptoMode.VERIFY, cipherKey);
	}

	/**
	 * <h3 class="en-US">Initialize data operator instance object.</h3>
	 * <h3 class="zh-CN">初始化数据操作器实例对象</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cryptoMode   <span class="en-US">Enumeration value of data operate</span>
	 *                     <span class="zh-CN">操作类型枚举值</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @return <span class="en-US">Data operator instance object</span>
	 * <span class="zh-CN">数据操作器实例对象</span>
	 */
	CryptoAdaptor initCipher(@Nonnull final CipherConfig cipherConfig, @Nonnull final CryptoMode cryptoMode,
	                         @Nonnull final CipherKey cipherKey);

	/**
	 * <h3 class="en-US">Encrypt the given data using the provided encryption algorithm configuration information, and then encode the encrypted result using the specified encoding type.</h3>
	 * <h3 class="zh-CN">使用给定的加密算法配置信息加密给定数据，将加密结果使用给定编码类型进行编码</h3>
	 *
	 * @param cipherConfig <span class="en-US">Encrypt cipher config instance object</span>
	 *                     <span class="zh-CN">加密算法配置信息</span>
	 * @param keyBytes     <span class="en-US">Byte array of passcode</span>
	 *                     <span class="zh-CN">密钥字节数组</span>
	 * @param source       <span class="en-US">source object</span>
	 *                     <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Encoded result value using given encode type</span>
	 * <span class="zh-CN">使用给定编码类型进行编码的结果值</span>
	 */
	byte[] encrypt(@Nonnull final CipherConfig cipherConfig, @Nonnull final byte[] keyBytes, @Nonnull final Object source);

	/**
	 * <h3 class="en-US">Decrypt the given string data using the provided decryption algorithm configuration information</h3>
	 * <h3 class="zh-CN">使用给定的加密算法配置信息解密给定字符串数据</h3>
	 *
	 * @param cipherConfig <span class="en-US">Encrypt cipher config instance object</span>
	 *                     <span class="zh-CN">加密算法配置信息</span>
	 * @param keyBytes     <span class="en-US">Byte array of passcode</span>
	 *                     <span class="zh-CN">密钥字节数组</span>
	 * @param source       <span class="en-US">source object</span>
	 *                     <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">The decrypted binary data</span>
	 * <span class="zh-CN">解密后的二进制数据</span>
	 */
	byte[] decrypt(@Nonnull final CipherConfig cipherConfig, @Nonnull final byte[] keyBytes, @Nonnull final Object source);

	/**
	 * <h3 class="en-US">Generate PublicKey from key data bytes and given algorithm</h3>
	 * <h3 class="zh-CN">根据给定的算法和二进制数据生成公钥</h3>
	 *
	 * @param algorithm <span class="en-US">Key algorithm</span>
	 *                  <span class="zh-CN">算法</span>
	 * @param keyBytes  <span class="en-US">Key data bytes</span>
	 *                  <span class="zh-CN">二进制数据</span>
	 * @return <span class="en-US">Generated publicKey or null if data bytes invalid</span>
	 * <span class="zh-CN">生成的公钥，如果二进制数据非法则返回null</span>
	 */
	PublicKey publicKey(final String algorithm, final byte[] keyBytes);


	/**
	 * <h3 class="en-US">Generate PrivateKey from key data bytes and given algorithm</h3>
	 * <h3 class="zh-CN">根据给定的算法和二进制数据生成私钥</h3>
	 *
	 * @param algorithm <span class="en-US">Key algorithm</span>
	 *                  <span class="zh-CN">算法</span>
	 * @param keyBytes  <span class="en-US">Key data bytes</span>
	 *                  <span class="zh-CN">二进制数据</span>
	 * @return <span class="en-US">Generated privateKey or null if data bytes invalid</span>
	 * <span class="zh-CN">生成的私钥，如果二进制数据非法则返回null</span>
	 */
	PrivateKey privateKey(final String algorithm, final byte[] keyBytes);

	/**
	 * <h3 class="en-US">Generate symmetric key bytes</h3>
	 * <h3 class="zh-CN">生成对称加密密钥字节数组</h3>
	 *
	 * @param algorithm       <span class="en-US">Algorithm name</span>
	 *                        <span class="zh-CN">算法名称</span>
	 * @param keySize         <span class="en-US">Key size</span>
	 *                        <span class="zh-CN">密钥长度</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	byte[] symmetricKey(final String algorithm, final int keySize, final String randomAlgorithm) throws CryptoException;

	/**
	 * <h3 class="en-US">Generate RSA KeyPair</h3>
	 * <h3 class="zh-CN">生成RSA密钥对</h3>
	 *
	 * @param algorithm       <span class="en-US">Algorithm name</span>
	 *                        <span class="zh-CN">算法名称</span>
	 * @param keySize         <span class="en-US">Key size</span>
	 *                        <span class="zh-CN">密钥长度</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param stdName         <span class="en-US">The standard name of the to-be-generated EC domain parameters</span>
	 *                        <span class="zh-CN">待生成EC域参数的标准名称</span>
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	KeyPair keyPair(final String algorithm, final int keySize, final String randomAlgorithm, final String stdName);
}
