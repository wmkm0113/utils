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

package org.nervousync.utils.security;

import jakarta.annotation.Nonnull;
import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.beans.security.CipherParameters;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.security.crypto.CryptoProvider;
import org.nervousync.security.crypto.DefaultCryptoProviderImpl;
import org.nervousync.utils.logger.LoggerUtils;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ServiceLoader;

/**
 * <h2 class="en-US">Crypto algorithm utilities</h2>
 * <h2 class="zh-CN">加密解密算法工具集</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public final class CryptoUtils extends SecurityUtils {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(CryptoUtils.class);

	/**
	 * <span class="en-US">Crypto adaptor instance object</span>
	 * <span class="zh-CN">加密解密适配器实例对象</span>
	 */
	private static final CryptoProvider SECURITY_ADAPTOR;

	static {
		SECURITY_ADAPTOR = ServiceLoader.load(CryptoProvider.class).findFirst().orElse(new DefaultCryptoProviderImpl());
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
	 * <h3 class="en-US">Initialize Blowfish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化Blowfish加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor BlowfishEncryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return BlowfishEncryptor("CBC", "PKCS7Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize Blowfish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化Blowfish加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor BlowfishEncryptor(final String mode, final String padding, final byte[] keyBytes,
	                                              final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("Blowfish", mode, padding),
				new CipherKey("Blowfish", 56, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize Blowfish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化Blowfish解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor BlowfishDecryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return BlowfishDecryptor("CBC", "PKCS7Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize Blowfish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化Blowfish解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor BlowfishDecryptor(final String mode, final String padding, final byte[] keyBytes,
	                                              final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("Blowfish", mode, padding),
				new CipherKey("Blowfish", 56, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Generate Blowfish key bytes</h3>
	 * <h3 class="zh-CN">生成Blowfish密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] BlowfishKey() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("Blowfish", 56, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform the Blowfish encryption operation</h3>
	 * <h3 class="zh-CN">执行 Blowfish 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] BlowfishEncrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return BlowfishEncrypt("CBC", "PKCS7Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform the Blowfish encryption operation</h3>
	 * <h3 class="zh-CN">执行 Blowfish 加密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] BlowfishEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                     final byte[] iv, final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("Blowfish", mode, padding),
				new CipherKey("Blowfish", 56, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Perform the Blowfish decryption operation</h3>
	 * <h3 class="zh-CN">执行 Blowfish 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] BlowfishDecrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return BlowfishDecrypt("CBC", "PKCS7Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform the Blowfish decryption operation</h3>
	 * <h3 class="zh-CN">执行 Blowfish 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] BlowfishDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                     final byte[] iv, final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("Blowfish", mode, padding),
				new CipherKey("Blowfish", 56, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Initialize Twofish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化 Twofish 加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TwofishEncryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return TwofishEncryptor("CBC", "PKCS7Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize Twofish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化 Twofish 加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TwofishEncryptor(final String mode, final String padding, final byte[] keyBytes,
	                                             final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("Twofish", mode, padding),
				new CipherKey("Twofish", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize Twofish decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化 Twofish 解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TwofishDecryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return TwofishDecryptor("CBC", "PKCS7Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize Twofish decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化 Twofish 解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TwofishDecryptor(final String mode, final String padding, final byte[] keyBytes,
	                                             final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("Twofish", mode, padding),
				new CipherKey("Twofish", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize DES encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化DES加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor DESEncryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return DESEncryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize DES encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化DES加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor DESEncryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("DES", mode, padding),
				new CipherKey("DES", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize DES decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化DES解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor DESDecryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return DESDecryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize DES decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化DES解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor DESDecryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("DES", mode, padding),
				new CipherKey("DES", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Generate DES key bytes</h3>
	 * <h3 class="zh-CN">生成DES密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] DESKey() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("DES", Globals.DEFAULT_VALUE_INT, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform DES encryption operation</h3>
	 * <h3 class="zh-CN">执行 DES 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] DESEncrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return DESEncrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform DES encryption operation</h3>
	 * <h3 class="zh-CN">执行 DES 加密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] DESEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final byte[] iv, final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("DES", mode, padding),
				new CipherKey("DES", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Perform DES decryption operation</h3>
	 * <h3 class="zh-CN">执行 DES 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] DESDecrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return DESDecrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform DES decryption operation</h3>
	 * <h3 class="zh-CN">执行 DES 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] DESDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final byte[] iv, final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("DES", mode, padding),
				new CipherKey("DES", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Initialize TripleDES encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">初始化3DES加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TripleDESEncryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return TripleDESEncryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize TripleDES encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">初始化3DES加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TripleDESEncryptor(final String mode, final String padding, final byte[] keyBytes,
	                                               final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("DESede", mode, padding),
				new CipherKey("DESede", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize TripleDES decryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">初始化3DES解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TripleDESDecryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return TripleDESDecryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize TripleDES decryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">初始化3DES解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TripleDESDecryptor(final String mode, final String padding, final byte[] keyBytes,
	                                               final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("DESede", mode, padding),
				new CipherKey("DESede", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Generate TripleDES key bytes</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">生成3DES密钥字节数组</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] TripleDESKey() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("DESede", Globals.DEFAULT_VALUE_INT, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform the TripleDES encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">执行 TripleDES 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] TripleDESEncrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return TripleDESEncrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform the TripleDES encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">执行 TripleDES 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] TripleDESEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                      final byte[] iv, final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("DESede", mode, padding),
				new CipherKey("DESede", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Perform the TripleDES decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">执行 TripleDES 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] TripleDESDecrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return TripleDESDecrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform the TripleDES decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">执行 TripleDES 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] TripleDESDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                      final byte[] iv, final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("DESede", mode, padding),
				new CipherKey("DESede", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Initialize SM4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Encryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return SM4Encryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize SM4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Encryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SM4Encryptor(mode, padding, keyBytes, "SHA1PRNG", iv);
	}

	/**
	 * <h3 class="en-US">Initialize SM4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode            <span class="en-US">Cipher Mode</span>
	 *                        <span class="zh-CN">分组密码模式</span>
	 * @param padding         <span class="en-US">Padding Mode</span>
	 *                        <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param iv              <span class="en-US">Initialization vector data</span>
	 *                        <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Encryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final String randomAlgorithm, final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("SM4", mode, padding),
				new CipherKey("SM4", 128, keyBytes, randomAlgorithm, "BC"),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize SM4 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Decryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return SM4Decryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize SM4 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Decryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SM4Decryptor(mode, padding, keyBytes, "SHA1PRNG", iv);
	}

	/**
	 * <h3 class="en-US">Initialize SM4 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode            <span class="en-US">Cipher Mode</span>
	 *                        <span class="zh-CN">分组密码模式</span>
	 * @param padding         <span class="en-US">Padding Mode</span>
	 *                        <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param iv              <span class="en-US">Initialization vector data</span>
	 *                        <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Decryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final String randomAlgorithm, final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("SM4", mode, padding),
				new CipherKey("SM4", 128, keyBytes, randomAlgorithm, "BC"),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Generate SM4 key bytes</h3>
	 * <h3 class="zh-CN">生成SM4密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] SM4Key() {
		return SM4Key(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Generate SM4 key bytes</h3>
	 * <h3 class="zh-CN">生成SM4密钥字节数组</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] SM4Key(final String randomAlgorithm) {
		try {
			return SECURITY_ADAPTOR.symmetricKey("SM4", 128, randomAlgorithm);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform SM4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Encrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return SM4Encrypt(keyBytes, Globals.DEFAULT_VALUE_STRING, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 加密操作</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param iv              <span class="en-US">Initialization vector data</span>
	 *                        <span class="zh-CN">初始向量数据</span>
	 * @param source          <span class="en-US">source object</span>
	 *                        <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Encrypt(final byte[] keyBytes, final String randomAlgorithm, final byte[] iv,
	                                final Object source) {
		return SM4Encrypt("CBC", "PKCS5Padding", keyBytes, randomAlgorithm, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 加密操作</h3>
	 *
	 * @param mode            <span class="en-US">Cipher Mode</span>
	 *                        <span class="zh-CN">分组密码模式</span>
	 * @param padding         <span class="en-US">Padding Mode</span>
	 *                        <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param iv              <span class="en-US">Initialization vector data</span>
	 *                        <span class="zh-CN">初始向量数据</span>
	 * @param source          <span class="en-US">source object</span>
	 *                        <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Encrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final String randomAlgorithm, final byte[] iv, final Object source) {
		return process(CryptoMode.ENCRYPT,
				new CipherConfig("SM4", mode, padding),
				new CipherKey("SM4", 128, keyBytes, randomAlgorithm, "BC"),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Decrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return SM4Decrypt(keyBytes, Globals.DEFAULT_VALUE_STRING, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 解密操作</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param iv              <span class="en-US">Initialization vector data</span>
	 *                        <span class="zh-CN">初始向量数据</span>
	 * @param source          <span class="en-US">source object</span>
	 *                        <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Decrypt(final byte[] keyBytes, final String randomAlgorithm, final byte[] iv, final Object source) {
		return SM4Decrypt("CBC", "PKCS5Padding", keyBytes, randomAlgorithm, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 解密操作</h3>
	 *
	 * @param mode            <span class="en-US">Cipher Mode</span>
	 *                        <span class="zh-CN">分组密码模式</span>
	 * @param padding         <span class="en-US">Padding Mode</span>
	 *                        <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param iv              <span class="en-US">Initialization vector data</span>
	 *                        <span class="zh-CN">初始向量数据</span>
	 * @param source          <span class="en-US">source object</span>
	 *                        <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Decrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final String randomAlgorithm, final byte[] iv, final Object source) {
		return process(CryptoMode.DECRYPT,
				new CipherConfig("SM4", mode, padding),
				new CipherKey("SM4", 128, keyBytes, randomAlgorithm, "BC"),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Encryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return RC2Encryptor("CBC", "PKCS7Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes         <span class="en-US">key bytes</span>
	 *                         <span class="zh-CN">密钥字节数组</span>
	 * @param effectiveKeyBits <span class="en-US">Effective key bits</span>
	 *                         <span class="zh-CN">有效密钥位数</span>
	 * @param iv               <span class="en-US">Initialization vector data</span>
	 *                         <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Encryptor(final byte[] keyBytes, final int effectiveKeyBits, final byte[] iv) throws CryptoException {
		return RC2Encryptor("CBC", "PKCS7Padding", keyBytes, effectiveKeyBits, iv);
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Encryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode             <span class="en-US">Cipher Mode</span>
	 *                         <span class="zh-CN">分组密码模式</span>
	 * @param padding          <span class="en-US">Padding Mode</span>
	 *                         <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes         <span class="en-US">key bytes</span>
	 *                         <span class="zh-CN">密钥字节数组</span>
	 * @param effectiveKeyBits <span class="en-US">Effective key bits</span>
	 *                         <span class="zh-CN">有效密钥位数</span>
	 * @param iv               <span class="en-US">Initialization vector data</span>
	 *                         <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Encryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final int effectiveKeyBits, final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv, effectiveKeyBits));
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Decryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return RC2Decryptor("CBC", "PKCS7Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes         <span class="en-US">key bytes</span>
	 *                         <span class="zh-CN">密钥字节数组</span>
	 * @param effectiveKeyBits <span class="en-US">Effective key bits</span>
	 *                         <span class="zh-CN">有效密钥位数</span>
	 * @param iv               <span class="en-US">Initialization vector data</span>
	 *                         <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Decryptor(final byte[] keyBytes, final int effectiveKeyBits, final byte[] iv)
			throws CryptoException {
		return RC2Decryptor("CBC", "PKCS7Padding", keyBytes, effectiveKeyBits, iv);
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Decryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode             <span class="en-US">Cipher Mode</span>
	 *                         <span class="zh-CN">分组密码模式</span>
	 * @param padding          <span class="en-US">Padding Mode</span>
	 *                         <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes         <span class="en-US">key bytes</span>
	 *                         <span class="zh-CN">密钥字节数组</span>
	 * @param effectiveKeyBits <span class="en-US">Effective key bits</span>
	 *                         <span class="zh-CN">有效密钥位数</span>
	 * @param iv               <span class="en-US">Initialization vector data</span>
	 *                         <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Decryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final int effectiveKeyBits, final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv, effectiveKeyBits));
	}

	/**
	 * <h3 class="en-US">Generate RC2 key bytes</h3>
	 * <h3 class="zh-CN">生成RC2密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] RC2Key() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("RC2", 128, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform RC2 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC2 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC2Encrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return RC2Encrypt("CBC", "PKCS7Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform RC2 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC2 加密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC2Encrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final byte[] iv, final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Perform RC2 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC2 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC2Decrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return RC2Decrypt("CBC", "PKCS7Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform RC2 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC2 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC2Decrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final byte[] iv, final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Initialize RC4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC4加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC4Encryptor(final byte[] keyBytes) throws CryptoException {
		return RC4Encryptor(keyBytes, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Initialize RC4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC4加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC4Encryptor(final byte[] keyBytes, final String randomAlgorithm)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(
				new CipherConfig("RC4", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RC4", 128, keyBytes, randomAlgorithm, "BC"),
				CipherParameters.EMPTY);
	}

	/**
	 * <h3 class="en-US">Initialize RC4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC4解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC4Decryptor(final byte[] keyBytes) throws CryptoException {
		return RC4Decryptor(keyBytes, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Initialize RC4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC4解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC4Decryptor(final byte[] keyBytes, final String randomAlgorithm)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(
				new CipherConfig("RC4", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RC4", 128, keyBytes, randomAlgorithm, "BC"),
				CipherParameters.EMPTY);
	}

	/**
	 * <h3 class="en-US">Generate RC4 key bytes</h3>
	 * <h3 class="zh-CN">生成RC4密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] RC4Key() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("RC4", 128, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform RC4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC4 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC4Encrypt(final byte[] keyBytes, final Object source) {
		return RC4Encrypt(keyBytes, Globals.DEFAULT_VALUE_STRING, source);
	}

	/**
	 * <h3 class="en-US">Perform RC4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC4 加密操作</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param source          <span class="en-US">source object</span>
	 *                        <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC4Encrypt(final byte[] keyBytes, final String randomAlgorithm, final Object source) {
		return process(CryptoMode.ENCRYPT,
				new CipherConfig("RC4", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RC4", 128, keyBytes, randomAlgorithm, "BC"),
				CipherParameters.EMPTY, source);
	}

	/**
	 * <h3 class="en-US">Perform RC4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC4 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC4Decrypt(final byte[] keyBytes, final Object source) {
		return RC4Decrypt(keyBytes, Globals.DEFAULT_VALUE_STRING, source);
	}

	/**
	 * <h3 class="en-US">Perform RC4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC4 解密操作</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param source          <span class="en-US">source object</span>
	 *                        <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC4Decrypt(final byte[] keyBytes, final String randomAlgorithm, final Object source) {
		return process(CryptoMode.DECRYPT,
				new CipherConfig("RC4", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RC4", 128, keyBytes, randomAlgorithm, "BC"),
				CipherParameters.EMPTY, source);
	}

	/**
	 * <h3 class="en-US">Initialize RC5 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC5加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC5Encryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return RC5Encryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize RC5 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC5加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC5Encryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RC5", mode, padding),
				new CipherKey("RC5", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize RC5 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC5解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC5Decryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return RC5Decryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize RC5 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC5解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC5Decryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RC5", mode, padding),
				new CipherKey("RC5", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Generate RC5 key bytes</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">生成RC5密钥字节数组</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] RC5Key() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("RC5", 128, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform RC5 encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC5 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC5Encrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return RC5Encrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform RC5 encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC5 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC5Encrypt(final String mode, final String padding, final byte[] keyBytes, final byte[] iv,
	                                final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("RC5", mode, padding),
				new CipherKey("RC5", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Perform RC5 decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC5 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC5Decrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return RC5Decrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform RC5 decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC5 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC5Decrypt(final String mode, final String padding, final byte[] keyBytes, final byte[] iv,
	                                final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("RC5", mode, padding),
				new CipherKey("RC5", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Initialize RC6 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC6加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC6Encryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return RC6Encryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize RC6 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC6加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC6Encryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RC6", mode, padding),
				new CipherKey("RC6", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize RC6 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC6解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC6Decryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return RC6Decryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize RC6 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC6解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC6Decryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RC6", mode, padding),
				new CipherKey("RC6", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Generate RC6 key bytes</h3>
	 * <h3 class="zh-CN">生成RC6密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] RC6Key() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("RC6", 128, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform RC6 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC6 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC6Encrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return RC6Encrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform RC6 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC6 加密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC6Encrypt(final String mode, final String padding, final byte[] keyBytes, final byte[] iv,
	                                final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("RC6", mode, padding),
				new CipherKey("RC6", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Perform RC6 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC6 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC6Decrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return RC6Decrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform RC6 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC6 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC6Decrypt(final String mode, final String padding, final byte[] keyBytes, final byte[] iv,
	                                final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("RC6", mode, padding),
				new CipherKey("RC6", 128, keyBytes, Globals.DEFAULT_VALUE_STRING),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Initialize AES encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化AES加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor AESEncryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return AESEncryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize AES encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化AES加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor AESEncryptor(final String mode, final String padding,
	                                         final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("AES", mode, padding),
				new CipherKey("AES", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Initialize AES decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化AES解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor AESDecryptor(final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return AESDecryptor("CBC", "PKCS5Padding", keyBytes, iv);
	}

	/**
	 * <h3 class="en-US">Initialize AES decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化AES解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor AESDecryptor(final String mode, final String padding,
	                                         final byte[] keyBytes, final byte[] iv) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("AES", mode, padding),
				new CipherKey("AES", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv));
	}

	/**
	 * <h3 class="en-US">Generate AES128 key bytes</h3>
	 * <h3 class="zh-CN">生成AES128密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES128Key() {
		return AES128Key(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Generate AES128 key bytes</h3>
	 * <h3 class="zh-CN">生成AES128密钥字节数组</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES128Key(final String randomAlgorithm) {
		try {
			return SECURITY_ADAPTOR.symmetricKey("AES", 128, randomAlgorithm);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Generate AES192 key bytes</h3>
	 * <h3 class="zh-CN">生成AES192密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES192Key() {
		return AES192Key(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Generate AES192 key bytes</h3>
	 * <h3 class="zh-CN">生成AES192密钥字节数组</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES192Key(final String randomAlgorithm) {
		try {
			return SECURITY_ADAPTOR.symmetricKey("AES", 192, randomAlgorithm);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Generate AES256 key bytes</h3>
	 * <h3 class="zh-CN">生成AES256密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES256Key() {
		return AES256Key(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Generate AES256 key bytes</h3>
	 * <h3 class="zh-CN">生成AES256密钥字节数组</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES256Key(final String randomAlgorithm) {
		try {
			return SECURITY_ADAPTOR.symmetricKey("AES", 256, randomAlgorithm);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESEncrypt(final byte[] keyBytes, final Object source) {
		return AESEncrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESEncrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return AESEncrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return AESEncrypt(mode, padding, keyBytes, null, source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final byte[] iv, final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("AES", mode, padding),
				new CipherKey("AES", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESDecrypt(final byte[] keyBytes, final Object source) {
		return AESDecrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESDecrypt(final byte[] keyBytes, final byte[] iv, final Object source) {
		return AESDecrypt("CBC", "PKCS5Padding", keyBytes, iv, source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return AESDecrypt(mode, padding, keyBytes, null, source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param iv       <span class="en-US">Initialization vector data</span>
	 *                 <span class="zh-CN">初始向量数据</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final byte[] iv, final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("AES", mode, padding),
				new CipherKey("AES", keyBytes),
				"ECB".equalsIgnoreCase(mode) ? CipherParameters.EMPTY : CipherParameters.with(iv), source);
	}

	/*
	 * Asymmetric methods
	 */

	/**
	 * <h3 class="en-US">Initialize RSA encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA加密安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSAEncryptor(final Key publicKey) throws CryptoException {
		return RSAEncryptor("PKCS1Padding", publicKey);
	}

	/**
	 * <h3 class="en-US">Initialize RSA encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA加密安全适配器实例对象</h3>
	 * <span>
	 * padding:  "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding",
	 * "OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA-256AndMGF1Padding",
	 * "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
	 * "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding",
	 * "OAEPWithSHA3-384AndMGF1Padding", "OAEPWithSHA3-512AndMGF1Padding"
	 * </span>
	 *
	 * @param padding   <span class="en-US">Padding Mode</span>
	 *                  <span class="zh-CN">数据填充模式</span>
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSAEncryptor(final String padding, final Key publicKey) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RSA", "None", padding),
				new CipherKey("RSA", publicKey.getEncoded()), CipherParameters.EMPTY);
	}

	/**
	 * <h3 class="en-US">Initialize RSA decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA解密安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSADecryptor(final Key privateKey) throws CryptoException {
		return RSADecryptor("PKCS1Padding", privateKey);
	}

	/**
	 * <h3 class="en-US">Initialize RSA decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA解密安全适配器实例对象</h3>
	 * <span>
	 * padding:  "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding",
	 * "OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA-256AndMGF1Padding",
	 * "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
	 * "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding",
	 * "OAEPWithSHA3-384AndMGF1Padding", "OAEPWithSHA3-512AndMGF1Padding"
	 * </span>
	 *
	 * @param padding    <span class="en-US">Padding Mode</span>
	 *                   <span class="zh-CN">数据填充模式</span>
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSADecryptor(final String padding, final Key privateKey) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RSA", "None", padding),
				new CipherKey("RSA", privateKey.getEncoded()), CipherParameters.EMPTY);
	}

	/**
	 * <h3 class="en-US">Initialize RSA signer secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA签名安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSASigner(final PrivateKey privateKey) throws CryptoException {
		return RSASigner("SHA256withRSA", privateKey);
	}

	/**
	 * <h3 class="en-US">Initialize RSA signer secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA签名安全适配器实例对象</h3>
	 *
	 * @param algorithm  <span class="en-US">Signature algorithm name</span>
	 *                   <span class="zh-CN">签名算法名称</span>
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSASigner(final String algorithm, final PrivateKey privateKey) throws CryptoException {
		return SECURITY_ADAPTOR.signer(
				new CipherConfig(algorithm, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RSA", privateKey.getEncoded()));
	}

	/**
	 * <h3 class="en-US">Initialize RSA signature verifier secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA签名验证安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSAVerifier(final PublicKey publicKey) throws CryptoException {
		return RSAVerifier("SHA256withRSA", publicKey);
	}

	/**
	 * <h3 class="en-US">Initialize RSA signature verifier secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA签名验证安全适配器实例对象</h3>
	 *
	 * @param algorithm <span class="en-US">Signature algorithm name</span>
	 *                  <span class="zh-CN">签名算法名称</span>
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSAVerifier(final String algorithm, final PublicKey publicKey) throws CryptoException {
		return SECURITY_ADAPTOR.verifier(
				new CipherConfig(algorithm, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RSA", publicKey.getEncoded()));
	}

	/**
	 * <h3 class="en-US">Generate RSA KeyPair</h3>
	 * <h3 class="zh-CN">生成RSA密钥对</h3>
	 *
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair RSAKeyPair() {
		return RSAKeyPair(2048);
	}

	/**
	 * <h3 class="en-US">Generate RSA KeyPair</h3>
	 * <h3 class="zh-CN">生成RSA密钥对</h3>
	 *
	 * @param keySize <span class="en-US">Key size</span>
	 *                <span class="zh-CN">密钥长度</span>
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair RSAKeyPair(final int keySize) {
		return RSAKeyPair(keySize, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Generate RSA KeyPair</h3>
	 * <h3 class="zh-CN">生成RSA密钥对</h3>
	 *
	 * @param keySize         <span class="en-US">Key size</span>
	 *                        <span class="zh-CN">密钥长度</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair RSAKeyPair(final int keySize, final String randomAlgorithm) {
		return SECURITY_ADAPTOR.keyPair("RSA", keySize, randomAlgorithm, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Encrypt the data using the given private key.</h3>
	 * <h3 class="zh-CN">使用给定的私钥对数据进行加密操作</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSAEncrypt(final Key publicKey, final Object source)
			throws CryptoException {
		return RSAEncrypt("PKCS1Padding", publicKey, source);
	}

	/**
	 * <h3 class="en-US">Encrypt the data using the given private key.</h3>
	 * <h3 class="zh-CN">使用给定的私钥对数据进行加密操作</h3>
	 * <span>
	 * padding:  "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding",
	 * "OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA-256AndMGF1Padding",
	 * "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
	 * "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding",
	 * "OAEPWithSHA3-384AndMGF1Padding", "OAEPWithSHA3-512AndMGF1Padding"
	 * </span>
	 *
	 * @param padding   <span class="en-US">Padding Mode</span>
	 *                  <span class="zh-CN">数据填充模式</span>
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSAEncrypt(final String padding, final Key publicKey, final Object source)
			throws CryptoException {
		return process(CryptoMode.ENCRYPT, new CipherConfig("RSA", "None", padding),
				new CipherKey("RSA", publicKey.getEncoded()), CipherParameters.EMPTY, source);
	}

	/**
	 * <h3 class="en-US">Decrypt the data using the given private key.</h3>
	 * <h3 class="zh-CN">使用给定的私钥对数据进行解密操作</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSADecrypt(final Key privateKey, final Object source) throws CryptoException {
		return RSADecrypt("PKCS1Padding", privateKey, source);
	}

	/**
	 * <h3 class="en-US">Decrypt the data using the given private key.</h3>
	 * <h3 class="zh-CN">使用给定的私钥对数据进行解密操作</h3>
	 * <span>
	 * padding:  "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding",
	 * "OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA-256AndMGF1Padding",
	 * "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
	 * "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding",
	 * "OAEPWithSHA3-384AndMGF1Padding", "OAEPWithSHA3-512AndMGF1Padding"
	 * </span>
	 *
	 * @param padding    <span class="en-US">Padding Mode</span>
	 *                   <span class="zh-CN">数据填充模式</span>
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSADecrypt(final String padding, final Key privateKey, final Object source)
			throws CryptoException {
		return process(CryptoMode.DECRYPT, new CipherConfig("RSA", "None", padding),
				new CipherKey("RSA", privateKey.getEncoded()), CipherParameters.EMPTY, source);
	}

	/**
	 * <h3 class="en-US">Sign the data using the given RSA algorithm.</h3>
	 * <h3 class="zh-CN">使用给定 RSA 算法对数据进行签名</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSASign(final PrivateKey privateKey, final Object source) throws CryptoException {
		return RSASign("SHA256withRSA", privateKey, source);
	}

	/**
	 * <h3 class="en-US">Sign the data using the given RSA algorithm.</h3>
	 * <h3 class="zh-CN">使用给定 RSA 算法对数据进行签名</h3>
	 *
	 * @param algorithm  <span class="en-US">Signature algorithm name</span>
	 *                   <span class="zh-CN">签名算法名称</span>
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSASign(final String algorithm, final PrivateKey privateKey, final Object source)
			throws CryptoException {
		return process(CryptoMode.SIGNATURE,
				new CipherConfig(algorithm, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RSA", privateKey.getEncoded()), CipherParameters.EMPTY, source);
	}

	/**
	 * <h3 class="en-US">Verify RSA signature</h3>
	 * <h3 class="zh-CN">验证 RSA 签名</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @param signature <span class="en-US">Signature data</span>
	 *                  <span class="zh-CN">签名数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static boolean RSAVerify(final PublicKey publicKey, final Object source, final byte[] signature)
			throws CryptoException {
		return RSAVerify("SHA256withRSA", publicKey, source, signature);
	}

	/**
	 * <h3 class="en-US">Verify RSA signature</h3>
	 * <h3 class="zh-CN">验证 RSA 签名</h3>
	 *
	 * @param algorithm <span class="en-US">Signature algorithm name</span>
	 *                  <span class="zh-CN">签名算法名称</span>
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @param signature <span class="en-US">Signature data</span>
	 *                  <span class="zh-CN">签名数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static boolean RSAVerify(final String algorithm, final PublicKey publicKey,
	                                final Object source, final byte[] signature) throws CryptoException {
		return verify(new CipherConfig(algorithm, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RSA", publicKey.getEncoded()), source, signature);
	}

	/**
	 * <h3 class="en-US">Initialize SM2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2加密安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM2Encryptor(final PublicKey publicKey) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("SM2", "None", "NoPadding"),
				new CipherKey("SM2", publicKey.getEncoded(), "BC"), CipherParameters.EMPTY);
	}

	/**
	 * <h3 class="en-US">Initialize SM2 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2解密安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM2Decryptor(final PrivateKey privateKey) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("SM2", "None", "NoPadding"),
				new CipherKey("SM2", privateKey.getEncoded(), "BC"), CipherParameters.EMPTY);
	}

	/**
	 * <h3 class="en-US">Initialize SM2 signer secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2签名安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM2Signer(final PrivateKey privateKey) throws CryptoException {
		return SECURITY_ADAPTOR.signer(
				new CipherConfig("SM3withSM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", privateKey.getEncoded(), "BC"));
	}

	/**
	 * <h3 class="en-US">Initialize SM2 signature verifier secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2签名验证安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM2Verifier(final PublicKey publicKey) throws CryptoException {
		return SECURITY_ADAPTOR.verifier(new CipherConfig("SM3withSM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", publicKey.getEncoded(), "BC"));
	}

	/**
	 * <h3 class="en-US">Generate SM2 KeyPair</h3>
	 * <h3 class="zh-CN">生成SM2密钥对</h3>
	 *
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair SM2KeyPair() {
		return SM2KeyPair(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Generate SM2 KeyPair</h3>
	 * <h3 class="zh-CN">生成SM2密钥对</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair SM2KeyPair(final String randomAlgorithm) {
		return SECURITY_ADAPTOR.keyPair("EC", Globals.INITIALIZE_INT_VALUE, randomAlgorithm, "sm2p256v1");
	}

	/**
	 * <h3 class="en-US">Initialize SM2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2加密安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] SM2Encrypt(final PublicKey publicKey, final Object source) throws CryptoException {
		return process(CryptoMode.ENCRYPT,
				new CipherConfig("SM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", publicKey.getEncoded(), "BC"),
				CipherParameters.EMPTY, source);
	}

	/**
	 * <h3 class="en-US">Initialize SM2 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2解密安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] SM2Decrypt(final PrivateKey privateKey, final Object source) throws CryptoException {
		return process(CryptoMode.DECRYPT,
				new CipherConfig("SM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", privateKey.getEncoded(), "BC"),
				CipherParameters.EMPTY, source);
	}

	/**
	 * <h3 class="en-US">Sign the data using the SM3withSM2 algorithm.</h3>
	 * <h3 class="zh-CN">使用 SM3withSM2 算法对数据进行签名</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] SM2Sign(final PrivateKey privateKey, final Object source) throws CryptoException {
		return process(CryptoMode.SIGNATURE,
				new CipherConfig("SM3withSM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", privateKey.getEncoded(), "BC"),
				CipherParameters.EMPTY, source);
	}

	/**
	 * <h3 class="en-US">Verify SM2 signature</h3>
	 * <h3 class="zh-CN">验证 SM2 签名</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @param signature <span class="en-US">Signature data</span>
	 *                  <span class="zh-CN">签名数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static boolean SM2Verify(final PublicKey publicKey, final Object source, final byte[] signature)
			throws CryptoException {
		return verify(
				new CipherConfig("SM3withSM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", publicKey.getEncoded(), "BC"),
				source, signature);
	}

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
	public static PublicKey publicKey(final String algorithm, final byte[] keyBytes) {
		return SECURITY_ADAPTOR.publicKey(algorithm, keyBytes);
	}

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
	public static PrivateKey privateKey(final String algorithm, final byte[] keyBytes) {
		return SECURITY_ADAPTOR.privateKey(algorithm, keyBytes);
	}

	public static byte[] generateIV(@Nonnull final String algorithm, @Nonnull final String mode) {
		int ivLength = ivLength(algorithm, mode);
		if (ivLength > 0) {
			byte[] iv = new byte[ivLength];
			Globals.randomBytes(iv);
			return iv;
		}
		return new byte[0];
	}

	public static int ivLength(@Nonnull final String algorithm, @Nonnull final String mode) {
		return switch (algorithm) {
			case "AES", "Twofish", "RC6", "SM4" -> "ECB".equalsIgnoreCase(mode) ? 0 : 16;
			case "Blowfish", "DES", "DESede", "RC2", "RC5" -> "ECB".equalsIgnoreCase(mode) ? 0 : 8;
			default -> Globals.DEFAULT_VALUE_INT;
		};
	}

	/**
	 * <h3 class="en-US">Perform the specified operation on the given source object.</h3>
	 * <h3 class="zh-CN">对给定的原始数据对象执行指定的操作</h3>
	 *
	 * @param cryptoMode   <span class="en-US">Enumeration value of data operate</span>
	 *                     <span class="zh-CN">操作类型枚举值</span>
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @param source       <span class="en-US">source object</span>
	 *                     <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	private static byte[] process(@Nonnull final CryptoMode cryptoMode, @Nonnull final CipherConfig cipherConfig,
	                              @Nonnull final CipherKey cipherKey, final CipherParameters parameters,
	                              @Nonnull final Object source) {
		CryptoAdaptor adaptor = switch (cryptoMode) {
			case ENCRYPT -> SECURITY_ADAPTOR.encryptor(cipherConfig, cipherKey, parameters);
			case DECRYPT -> SECURITY_ADAPTOR.decryptor(cipherConfig, cipherKey, parameters);
			case SIGNATURE -> SECURITY_ADAPTOR.signer(cipherConfig, cipherKey);
			default -> throw new CryptoException(0x000000150003L);
		};
		process(adaptor, source);
		return adaptor.finish();
	}

	/**
	 * <h3 class="en-US">Verify the validity of the signature data.</h3>
	 * <h3 class="zh-CN">验证签名数据是否有效</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @param source       <span class="en-US">source object</span>
	 *                     <span class="zh-CN">原始数据对象</span>
	 * @param signature    <span class="en-US">Signature data</span>
	 *                     <span class="zh-CN">签名数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	private static boolean verify(@Nonnull final CipherConfig cipherConfig, @Nonnull final CipherKey cipherKey,
	                              @Nonnull final Object source, @Nonnull final byte[] signature) {
		CryptoAdaptor verifierAdaptor = SECURITY_ADAPTOR.verifier(cipherConfig, cipherKey);
		process(verifierAdaptor, source);
		return verifierAdaptor.verify(signature);
	}
}
