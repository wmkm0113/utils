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

package org.nervousync.archive.zip.security;

import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.security.impl.*;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.security.DigestUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * <h2 class="en-US">Archive protection information cipher interface</h2>
 * <h2 class="zh-CN">归档文件保护信息加密解密接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public interface EncryptionProvider {

	/**
	 * <h3 class="en-US">Get the block size</h3>
	 * <h3 class="zh-CN">获取分块大小</h3>
	 *
	 * @return <span class="en-US">Block size</span>
	 * <span class="zh-CN">分块大小</span>
	 */
	int blockSize();

	/**
	 * <h3 class="en-US">Uses padding flag</h3>
	 * <h3 class="zh-CN">是否使用填充标记</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	boolean usesPadding();

	/**
	 * <h3 class="en-US">Validate header information</h3>
	 * <h3 class="zh-CN">验证信息头数据</h3>
	 *
	 * @param algorithm    <span class="en-US">Algorithm code</span>
	 *                     <span class="zh-CN">算法代码</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥比特长度</span>
	 * @param flags        <span class="en-US">Credentials and procedures required for decryption</span>
	 *                     <span class="zh-CN">解密所需凭据和处理方式</span>
	 * @param ivData       <span class="en-US">IV data</span>
	 *                     <span class="zh-CN">向量数据</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when validation header data</span>
	 *                          <span class="zh-CN">如果数据检查出错</span>
	 */
	void validateHeader(final int algorithm, final int keyBitLength, final int flags, final byte[] ivData)
			throws ArchiveException;

	/**
	 * <h3 class="en-US">Derive key by the given material data and key bit length</h3>
	 * <h3 class="zh-CN">根据给定的原始数据派生密钥</h3>
	 *
	 * @param material     <span class="en-US">Password bytes or IVData || RD</span>
	 *                     <span class="zh-CN">密码信息或 IVData || RD</span>
	 * @param keyBitLength <span class="en-US">Encryption key bit length</span>
	 *                     <span class="zh-CN">加密密钥位数</span>
	 * @return <span class="en-US">Derive key</span>
	 * <span class="zh-CN">派生密钥</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when derive key</span>
	 *                         <span class="zh-CN">解密数据出错</span>
	 */
	byte[] deriveKey(final byte[] material, int keyBitLength) throws CryptoException;

	/**
	 * <h3 class="en-US">Encrypt random data and generate ErdData</h3>
	 * <h3 class="zh-CN">加密安全随机数据生成 ErdData</h3>
	 *
	 * @param randomData       <span class="en-US">Secure random data bytes</span>
	 *                         <span class="zh-CN">安全随机数据</span>
	 * @param masterSessionKey <span class="en-US">Master session key</span>
	 *                         <span class="zh-CN">主会话密钥</span>
	 * @param ivData           <span class="en-US">IV data</span>
	 *                         <span class="zh-CN">向量数据</span>
	 * @return <span class="en-US">Encrypted data bytes</span>
	 * <span class="zh-CN">加密后的数据</span>
	 * @throws CryptoException  <span class="en-US">If an error occurs when encrypt data</span>
	 *                          <span class="zh-CN">解密数据出错</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when validation data</span>
	 *                          <span class="zh-CN">如果数据检查出错</span>
	 */
	default byte[] encryptRandomData(final byte[] randomData, final byte[] masterSessionKey, final byte[] ivData)
			throws CryptoException, ArchiveException {
		if (randomData == null || randomData.length == 0 || randomData.length % this.blockSize() != 0) {
			throw new ArchiveException(0x0000001A001FL);
		}

		byte[] paddedRandomData = null;
		try {
			paddedRandomData = Arrays.copyOf(randomData, randomData.length + this.blockSize());
			Arrays.fill(paddedRandomData, randomData.length, paddedRandomData.length, (byte) this.blockSize());
			CryptoAdaptor encryptor = this.encryptor(masterSessionKey, ivData);
			byte[] encrypted = encryptor.finish(paddedRandomData);
			if (encrypted == null || encrypted.length != paddedRandomData.length) {
				RawUtils.clear(encrypted);
				throw new ArchiveException(0x0000001A001FL);
			}
			return encrypted;
		} finally {
			RawUtils.clear(paddedRandomData);
		}
	}

	/**
	 * <h3 class="en-US">Generate content data encryptor</h3>
	 * <h3 class="zh-CN">创建内容加密器</h3>
	 *
	 * @param sessionKey <span class="en-US">Session key</span>
	 *                   <span class="zh-CN">会话密钥</span>
	 * @param ivData     <span class="en-US">IV data</span>
	 *                   <span class="zh-CN">向量数据</span>
	 * @return <span class="en-US">Crypto adaptor instance object</span>
	 * <span class="zh-CN">解密器实例对象</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when initialize the crypto adaptor</span>
	 *                         <span class="zh-CN">初始化加密器出错</span>
	 */
	CryptoAdaptor encryptor(final byte[] sessionKey, byte[] ivData) throws CryptoException;

	/**
	 * <h3 class="en-US">Decrypt ErdData and remove the padding data</h3>
	 * <h3 class="zh-CN">解密 ErdData 并去除填充</h3>
	 *
	 * @param encryptedRandomData <span class="en-US">Encrypted random data bytes</span>
	 *                            <span class="zh-CN">加密的二进制数据</span>
	 * @param masterSessionKey    <span class="en-US">Master session key</span>
	 *                            <span class="zh-CN">主会话密钥</span>
	 * @param ivData              <span class="en-US">IV data</span>
	 *                            <span class="zh-CN">向量数据</span>
	 * @return <span class="en-US">Decrypted data bytes</span>
	 * <span class="zh-CN">解密后的数据</span>
	 * @throws CryptoException  <span class="en-US">If an error occurs when decrypt data</span>
	 *                          <span class="zh-CN">解密数据出错</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when validation data</span>
	 *                          <span class="zh-CN">如果数据检查出错</span>
	 */
	default byte[] decryptRandomData(final byte[] encryptedRandomData, final byte[] masterSessionKey, final byte[] ivData)
			throws CryptoException, ArchiveException {
		if (encryptedRandomData == null || encryptedRandomData.length == 0
				|| encryptedRandomData.length % this.blockSize() != 0) {
			throw new ArchiveException(0x0000001A001FL);
		}
		byte[] decryptedData = null;
		try {
			CryptoAdaptor decryptor = this.decryptor(masterSessionKey, ivData);

			decryptedData = decryptor.finish(encryptedRandomData);

			if (decryptedData == null || decryptedData.length == 0 || decryptedData.length % this.blockSize() != 0) {
				throw new ArchiveException(0x0000001A001FL);
			}

			int paddingLength = Byte.toUnsignedInt(decryptedData[decryptedData.length - 1]);

			if (paddingLength < 1 || paddingLength > this.blockSize()) {
				throw new ArchiveException(0x0000001B000DL);
			}

			int plainLength = decryptedData.length - paddingLength;
			int paddingDifference = 0;

			for (int i = plainLength; i < decryptedData.length; i++) {
				paddingDifference |= Byte.toUnsignedInt(decryptedData[i]) ^ paddingLength;
			}

			if (paddingDifference != 0) {
				throw new ArchiveException(0x0000001B000DL);
			}

			return Arrays.copyOf(decryptedData, plainLength);
		} finally {
			RawUtils.clear(decryptedData);
		}
	}

	/**
	 * <h3 class="en-US">Generate content data decryptor</h3>
	 * <h3 class="zh-CN">创建内容解密器</h3>
	 *
	 * @param sessionKey <span class="en-US">Session key</span>
	 *                   <span class="zh-CN">会话密钥</span>
	 * @param ivData     <span class="en-US">IV data</span>
	 *                   <span class="zh-CN">向量数据</span>
	 * @return <span class="en-US">Crypto adaptor instance object</span>
	 * <span class="zh-CN">解密器实例对象</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when initialize the crypto adaptor</span>
	 *                         <span class="zh-CN">初始化解密器出错</span>
	 */
	CryptoAdaptor decryptor(final byte[] sessionKey, byte[] ivData) throws CryptoException;

	/**
	 * <h3 class="en-US">Resolve the archive protection information cipher instance object by the given algorithm code</h3>
	 * <h3 class="zh-CN">根据给定的算法代码获取归档文件保护信息加密解密实现类实例对象</h3>
	 *
	 * @param algorithm <span class="en-US">Algorithm code</span>
	 *                  <span class="zh-CN">算法代码</span>
	 * @return <span class="en-US">Archive protection information cipher instance object</span>
	 * <span class="zh-CN">归档文件保护信息加密解密实现类实例对象</span>
	 * @throws ArchiveException <span class="en-US">Algorithm code not supported</span>
	 *                          <span class="zh-CN">算法代码不被支持</span>
	 */
	static EncryptionProvider resolve(final int algorithm) throws ArchiveException {
		return switch (algorithm) {
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES -> DESEncryptionProvider.INSTANCE;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168 ->
					TripleDESEncryptionProvider.INSTANCE;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192,
			     ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 -> AESEncryptionProvider.INSTANCE;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW -> RC2EncryptionProvider.INSTANCE;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4 -> RC4EncryptionProvider.INSTANCE;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH -> BlowfishEncryptionProvider.INSTANCE;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH -> TwofishEncryptionProvider.INSTANCE;
			default -> throw new ArchiveException(0x0000001A001FL);
		};
	}

	/**
	 * <h3 class="en-US">Validate flags access mode</h3>
	 * <h3 class="zh-CN">验证访问模式</h3>
	 *
	 * @param flags <span class="en-US">Flags</span>
	 *              <span class="zh-CN">标记</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	static boolean validateFlags(final int flags) {
		int mode = flags & 3;
		return mode != 0
				&& (flags & ~(3 | ZipGlobals.ZIP64_HEADER_ENC_FLAGS_NON_OAEP | ZipGlobals.ZIP64_HEADER_ENC_FLAGS_ERD_3DES)) == 0
				&& ((flags & ZipGlobals.ZIP64_HEADER_ENC_FLAGS_NON_OAEP) == 0 || (mode & 2) != 0);
	}

	static byte[] deriveKeyBytes(final byte[] material, int keyLengthBytes) throws CryptoException {
		Objects.requireNonNull(material, "material");
		if (keyLengthBytes <= 0 || keyLengthBytes > 40) {
			throw new CryptoException(0x0000001B0004L);
		}

		byte[] digest = null;
		byte[] first = null;
		byte[] second = null;
		byte[] expanded = null;

		byte[] pad36 = new byte[64];
		byte[] pad5C = new byte[64];

		try {
			digest = DigestUtils.SHA1(material);

			Arrays.fill(pad36, (byte) 0x36);
			Arrays.fill(pad5C, (byte) 0x5C);

			for (int i = 0; i < digest.length; i++) {
				pad36[i] ^= digest[i];
				pad5C[i] ^= digest[i];
			}

			first = DigestUtils.SHA1(pad36);
			second = DigestUtils.SHA1(pad5C);

			expanded = new byte[first.length + second.length];
			System.arraycopy(first, 0, expanded, 0, first.length);
			System.arraycopy(second, 0, expanded, first.length, second.length);

			return Arrays.copyOf(expanded, keyLengthBytes);
		} finally {
			RawUtils.clear(digest);
			RawUtils.clear(first);
			RawUtils.clear(second);
			RawUtils.clear(expanded);
			RawUtils.clear(pad36);
			RawUtils.clear(pad5C);
		}
	}

	/**
	 * <h3 class="en-US">Expand DES key</h3>
	 * <h3 class="zh-CN">扩充 DES 密钥</h3>
	 *
	 * @param desKey <span class="en-US">DES key</span>
	 *               <span class="zh-CN">DES 密钥</span>
	 * @return <span class="en-US">Expand DES key</span>
	 * <span class="zh-CN">扩充后的 DES 密钥</span>
	 * @throws CryptoException <span class="en-US">DES key invalid</span>
	 *                         <span class="zh-CN">DES 密钥非法</span>
	 */
	static byte[] expandDESKey(final byte[] desKey, final int offset) throws CryptoException {
		if (desKey == null || offset < 0 || desKey.length - offset < 7) {
			throw new CryptoException(0x0000001B0004L);
		}

		int b0 = desKey[offset] & 0xFF;
		int b1 = desKey[offset + 1] & 0xFF;
		int b2 = desKey[offset + 2] & 0xFF;
		int b3 = desKey[offset + 3] & 0xFF;
		int b4 = desKey[offset + 4] & 0xFF;
		int b5 = desKey[offset + 5] & 0xFF;
		int b6 = desKey[offset + 6] & 0xFF;

		byte[] expandKey = new byte[8];

		expandKey[0] = (byte) (b0 & 0xFE);
		expandKey[1] = (byte) (((b0 << 7) | (b1 >>> 1)) & 0xFE);
		expandKey[2] = (byte) (((b1 << 6) | (b2 >>> 2)) & 0xFE);
		expandKey[3] = (byte) (((b2 << 5) | (b3 >>> 3)) & 0xFE);
		expandKey[4] = (byte) (((b3 << 4) | (b4 >>> 4)) & 0xFE);
		expandKey[5] = (byte) (((b4 << 3) | (b5 >>> 5)) & 0xFE);
		expandKey[6] = (byte) (((b5 << 2) | (b6 >>> 6)) & 0xFE);
		expandKey[7] = (byte) ((b6 << 1) & 0xFE);

		return expandKey;
	}

	/**
	 * <h3 class="en-US">Apply the DES odd parity bit</h3>
	 * <h3 class="zh-CN">设置 DES 奇校验位</h3>
	 *
	 * @param desKey <span class="en-US">DES key</span>
	 *               <span class="zh-CN">DES 密钥</span>
	 * @throws CryptoException <span class="en-US">DES key invalid</span>
	 *                         <span class="zh-CN">DES 密钥非法</span>
	 */
	static void applyOddParity(final byte[] desKey) throws CryptoException {
		if (desKey == null || desKey.length != 8) {
			throw new CryptoException(0x0000001B0004L);
		}

		for (int i = 0; i < desKey.length; i++) {
			int value = desKey[i] & 0xFE;
			int parityBit = (Integer.bitCount(value) & 0x01) == 0 ? 1 : 0;
			desKey[i] = (byte) (value | parityBit);
		}
	}

	/**
	 * <h3 class="en-US">Calculate padding size</h3>
	 * <h3 class="zh-CN">计算填充长度</h3>
	 *
	 * @param plainSize <span class="en-US">Plain size</span>
	 *                  <span class="zh-CN">内容长度</span>
	 * @return <span class="en-US">Padding size</span>
	 * <span class="zh-CN">填充长度</span>
	 */
	default int paddingSize(final long plainSize) {
		if (plainSize < 0) {
			throw new IllegalArgumentException("plainSize must be non-negative");
		}
		if (this.usesPadding()) {
			int blockSize = this.blockSize();
			if (blockSize <= 0) {
				throw new IllegalArgumentException("blockSize must be positive");
			}
			return this.blockSize() - (int) (plainSize % this.blockSize());
		}
		return 0;
	}

	/**
	 * <h3 class="en-US">Calculate encrypted size</h3>
	 * <h3 class="zh-CN">计算加密后的长度</h3>
	 *
	 * @param plainSize <span class="en-US">Plain size</span>
	 *                  <span class="zh-CN">内容长度</span>
	 * @return <span class="en-US">Encrypted size</span>
	 * <span class="zh-CN">加密后的长度</span>
	 */
	default long encryptedSize(final long plainSize) {
		return Math.addExact(plainSize, this.paddingSize(plainSize));
	}
}
