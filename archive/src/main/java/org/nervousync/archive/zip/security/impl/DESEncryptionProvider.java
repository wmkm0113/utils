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
import org.nervousync.utils.security.CryptoUtils;

import java.util.Objects;

/**
 * <h2 class="en-US">DES archive protection information cipher implement class</h2>
 * <h2 class="zh-CN">归档文件保护信息 DES 加密解密实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class DESEncryptionProvider implements EncryptionProvider {

	/**
	 * <span class="en-US">Singleton instance object of the DES archive protection information cipher implement class</span>
	 * <span class="zh-CN">归档文件保护信息 DES 加密解密实现类的单例实例对象</span>
	 */
	public static final DESEncryptionProvider INSTANCE = new DESEncryptionProvider();

	/**
	 * <span class="en-US">Block size</span>
	 * <span class="zh-CN">分块大小</span>
	 */
	private static final int DES_BLOCK_SIZE = 8;
	private static final int DES_EFFECTIVE_KEY_BITS = 56;
	private static final int DES_STORED_KEY_BITS = 64;

	/**
	 * <h3 class="en-US">Private constructor method for the DES archive protection information cipher implement class</h3>
	 * <h3 class="zh-CN">归档文件保护信息 DES 加密解密实现类的私有构造方法</h3>
	 */
	private DESEncryptionProvider() {
	}

	@Override
	public int blockSize() {
		return DES_BLOCK_SIZE;
	}

	@Override
	public boolean usesPadding() {
		return true;
	}

	@Override
	public void validateHeader(final int algorithm, final int keyBitLength, final int flags, final byte[] ivData)
			throws ArchiveException {
		if (algorithm != ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES
				|| (keyBitLength != 56 && keyBitLength != 64)
				|| !EncryptionProvider.validateFlags(flags)
				|| ivData == null
				|| ivData.length != 8) {
			throw new ArchiveException(0x0000001A001FL);
		}
	}

	@Override
	public byte[] deriveKey(final byte[] material, final int keyBitLength) throws CryptoException {
		Objects.requireNonNull(material, "material");
		if (keyBitLength != DES_EFFECTIVE_KEY_BITS && keyBitLength != DES_STORED_KEY_BITS) {
			throw new CryptoException(0x0000001B0004L);
		}

		byte[] desKey = EncryptionProvider.deriveKeyBytes(material, keyBitLength / Byte.SIZE);
		if (keyBitLength == DES_EFFECTIVE_KEY_BITS) {
			desKey = EncryptionProvider.expandDESKey(desKey, 0);
		}
		EncryptionProvider.applyOddParity(desKey);
		return desKey;
	}

	@Override
	public CryptoAdaptor encryptor(final byte[] sessionKey, final byte[] ivData) throws CryptoException {
		validate(sessionKey, ivData);
		return CryptoUtils.DESEncryptor("CBC", "NoPadding", sessionKey, ivData);
	}

	@Override
	public CryptoAdaptor decryptor(final byte[] sessionKey, final byte[] ivData) throws CryptoException {
		validate(sessionKey, ivData);
		return CryptoUtils.DESDecryptor("CBC", "NoPadding", sessionKey, ivData);
	}

	private static void validate(final byte[] key, final byte[] iv) throws CryptoException {
		if (key == null || key.length != 8 || iv == null || iv.length != 8) {
			throw new CryptoException(0x0000001B0004L);
		}
	}
}
