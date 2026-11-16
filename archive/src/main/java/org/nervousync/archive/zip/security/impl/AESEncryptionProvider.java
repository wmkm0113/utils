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

package org.nervousync.archive.zip.security.impl;

import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.security.CryptoUtils;

import java.util.Arrays;

/**
 * <h2 class="en-US">AES archive protection information cipher implement class</h2>
 * <h2 class="zh-CN">归档文件保护信息AES加密解密实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class AESEncryptionProvider implements EncryptionProvider {

	/**
	 * <span class="en-US">Singleton instance object of the AES archive protection information cipher implement class</span>
	 * <span class="zh-CN">归档文件保护信息 AES 加密解密实现类的单例实例对象</span>
	 */
	public static final AESEncryptionProvider INSTANCE = new AESEncryptionProvider();

	/**
	 * <span class="en-US">Block size</span>
	 * <span class="zh-CN">分块大小</span>
	 */
	private static final int BLOCK_SIZE = 16;

	/**
	 * <h3 class="en-US">Private constructor method for the AES archive protection information cipher implement class</h3>
	 * <h3 class="zh-CN">归档文件保护信息AES加密解密实现类的私有构造方法</h3>
	 */
	private AESEncryptionProvider() {
	}

	@Override
	public int blockSize() {
		return BLOCK_SIZE;
	}

	@Override
	public boolean usesPadding() {
		return true;
	}

	@Override
	public void validateHeader(final int algorithm, final int keyBitLength, final int flags, final byte[] ivData)
			throws ArchiveException {
		int expectedBitLength = switch (algorithm) {
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128 -> 128;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192 -> 192;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 -> 256;
			default -> throw new ArchiveException(0x0000001A001FL);
		};

		if (expectedBitLength != keyBitLength) {
			throw new ArchiveException(0x0000001A001FL);
		}

		if (!EncryptionProvider.validateFlags(flags) || ivData == null || ivData.length != BLOCK_SIZE) {
			throw new ArchiveException(0x0000001A001FL);
		}
	}

	@Override
	public byte[] deriveKey(final byte[] material, final int keyBitLength) throws CryptoException {
		return EncryptionProvider.deriveKeyBytes(material, keyBitLength / Byte.SIZE);
	}

	@Override
	public byte[] encryptRandomData(final byte[] randomData, final byte[] masterSessionKey, final byte[] ivData)
			throws CryptoException, ArchiveException {
		if (randomData == null || randomData.length % BLOCK_SIZE != 0) {
			throw new ArchiveException(0x0000001A001FL);
		}

		byte[] paddedRandomData = new byte[randomData.length + BLOCK_SIZE];
		try {
			System.arraycopy(randomData, 0, paddedRandomData, 0, randomData.length);
			Arrays.fill(paddedRandomData, randomData.length, paddedRandomData.length, (byte) BLOCK_SIZE);
			return this.encryptor(masterSessionKey, ivData).finish(paddedRandomData);
		} finally {
			RawUtils.clear(paddedRandomData);
		}
	}

	@Override
	public CryptoAdaptor encryptor(final byte[] sessionKey, final byte[] ivData) throws CryptoException {
		return CryptoUtils.AESEncryptor("CBC", "NoPadding", sessionKey, ivData);
	}

	@Override
	public byte[] decryptRandomData(final byte[] encryptedRandomData, final byte[] masterSessionKey, final byte[] ivData)
			throws CryptoException, ArchiveException {
		if (encryptedRandomData == null || encryptedRandomData.length % BLOCK_SIZE != 0) {
			throw new ArchiveException(0x0000001A001FL);
		}

		byte[] decryptedData = null;
		try {
			CryptoAdaptor decryptor = this.decryptor(masterSessionKey, ivData);
			decryptedData = decryptor.finish(encryptedRandomData);
			if (decryptedData.length % BLOCK_SIZE != 0) {
				throw new ArchiveException(0x0000001A001FL);
			}
			int randomDataLength = decryptedData.length - BLOCK_SIZE;
			for (int i = randomDataLength; i < decryptedData.length; i++) {
				if (Byte.toUnsignedInt(decryptedData[i]) != BLOCK_SIZE) {
					throw new ArchiveException(0x0000001B000DL);
				}
			}
			return Arrays.copyOf(decryptedData, randomDataLength);
		} finally {
			RawUtils.clear(decryptedData);
		}
	}

	@Override
	public CryptoAdaptor decryptor(final byte[] sessionKey, final byte[] ivData) throws CryptoException {
		return CryptoUtils.AESDecryptor("CBC", "NoPadding", sessionKey, ivData);
	}
}
