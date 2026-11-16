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

import java.util.Objects;

/**
 * <h2 class="en-US">3DES archive protection information cipher implement class</h2>
 * <h2 class="zh-CN">归档文件保护信息 3DES 加密解密实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class TripleDESEncryptionProvider implements EncryptionProvider {

	/**
	 * <span class="en-US">Singleton instance object of the 3DES archive protection information cipher implement class</span>
	 * <span class="zh-CN">归档文件保护信息 3DES 加密解密实现类的单例实例对象</span>
	 */
	public static final TripleDESEncryptionProvider INSTANCE = new TripleDESEncryptionProvider();

	/**
	 * <span class="en-US">Block size</span>
	 * <span class="zh-CN">分块大小</span>
	 */
	private static final int BLOCK_SIZE = 8;

	private static final int DES_EFFECTIVE_KEY_BYTES = 7;
	private static final int DES_STORED_KEY_BYTES = 8;

	private static final int DESEDE_KEY_BYTES = 24;

	private static final int TRIPLE_DES_112_BITS = 112;
	private static final int TRIPLE_DES_168_BITS = 168;

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
		boolean validAlgorithm =
				(algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112 && keyBitLength == TRIPLE_DES_112_BITS)
						|| (algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168 && keyBitLength == TRIPLE_DES_168_BITS);

		if (!validAlgorithm || !EncryptionProvider.validateFlags(flags) || ivData == null || ivData.length != BLOCK_SIZE) {
			throw new ArchiveException(0x0000001A001FL);
		}
	}

	@Override
	public byte[] deriveKey(final byte[] material, final int keyBitLength) throws CryptoException {
		Objects.requireNonNull(material, "material");
		if (keyBitLength != TRIPLE_DES_112_BITS && keyBitLength != TRIPLE_DES_168_BITS) {
			throw new CryptoException(0x0000001B0004L);
		}

		byte[] derivedMaterial = null;
		byte[] key1 = null;
		byte[] key2 = null;
		byte[] key3 = null;
		byte[] desEdeKey = null;
		boolean success = false;

		try {
			derivedMaterial = EncryptionProvider.deriveKeyBytes(material, keyBitLength / Byte.SIZE);

			key1 = EncryptionProvider.expandDESKey(derivedMaterial, 0);
			EncryptionProvider.applyOddParity(key1);
			key2 = EncryptionProvider.expandDESKey(derivedMaterial, DES_EFFECTIVE_KEY_BYTES);
			EncryptionProvider.applyOddParity(key2);

			if (keyBitLength == TRIPLE_DES_168_BITS) {
				key3 = EncryptionProvider.expandDESKey(derivedMaterial, DES_EFFECTIVE_KEY_BYTES * 2);
				EncryptionProvider.applyOddParity(key3);
			}

			desEdeKey = new byte[DESEDE_KEY_BYTES];

			System.arraycopy(key1, 0, desEdeKey, 0, DES_STORED_KEY_BYTES);
			System.arraycopy(key2, 0, desEdeKey, DES_STORED_KEY_BYTES, DES_STORED_KEY_BYTES);

			System.arraycopy(
					keyBitLength == TRIPLE_DES_112_BITS ? key1 : key3, 0,
					desEdeKey, DES_STORED_KEY_BYTES * 2, DES_STORED_KEY_BYTES);

			success = true;
			return desEdeKey;
		} finally {
			RawUtils.clear(derivedMaterial);
			RawUtils.clear(key1);
			RawUtils.clear(key2);
			RawUtils.clear(key3);

			if (!success) {
				RawUtils.clear(desEdeKey);
			}
		}
	}

	@Override
	public CryptoAdaptor encryptor(final byte[] sessionKey, final byte[] ivData) throws CryptoException {
		validate(sessionKey, ivData);
		return CryptoUtils.TripleDESEncryptor("CBC", "NoPadding", sessionKey, ivData);
	}

	@Override
	public CryptoAdaptor decryptor(final byte[] sessionKey, final byte[] ivData) throws CryptoException {
		validate(sessionKey, ivData);
		return CryptoUtils.TripleDESDecryptor("CBC", "NoPadding", sessionKey, ivData);
	}

	private static void validate(final byte[] key, final byte[] iv) throws CryptoException {

		if (key == null || key.length != DESEDE_KEY_BYTES || iv == null || iv.length != BLOCK_SIZE) {
			throw new CryptoException(0x0000001B0004L);
		}
	}
}
