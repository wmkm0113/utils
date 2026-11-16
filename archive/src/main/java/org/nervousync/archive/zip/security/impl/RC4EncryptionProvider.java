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

/**
 * <h2 class="en-US">RC4 archive protection information cipher implement class</h2>
 * <h2 class="zh-CN">归档文件保护信息 RC4 加密解密实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class RC4EncryptionProvider implements EncryptionProvider {

	/**
	 * <span class="en-US">Singleton instance object of the RC4 archive protection information cipher implement class</span>
	 * <span class="zh-CN">归档文件保护信息 RC4 加密解密实现类的单例实例对象</span>
	 */
	public static final RC4EncryptionProvider INSTANCE = new RC4EncryptionProvider();

	@Override
	public int blockSize() {
		return 1;
	}

	@Override
	public boolean usesPadding() {
		return false;
	}

	@Override
	public void validateHeader(final int algorithm, final int keyBitLength, final int flags, final byte[] ivData)
			throws ArchiveException {
		if (algorithm != ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4
				|| !supportedKeySize(keyBitLength)
				|| !EncryptionProvider.validateFlags(flags)
				|| ivData == null) {
			throw new ArchiveException(0x0000001A001FL);
		}
	}

	@Override
	public byte[] deriveKey(final byte[] material, final int keyBitLength) throws CryptoException {
		if (supportedKeySize(keyBitLength)) {
			return EncryptionProvider.deriveKeyBytes(material, keyBitLength / Byte.SIZE);
		}
		throw new CryptoException(0x0000001B0004L);
	}

	@Override
	public byte[] encryptRandomData(final byte[] randomData, final byte[] masterSessionKey, final byte[] ivData)
			throws CryptoException, ArchiveException {
		if (randomData == null || randomData.length == 0) {
			throw new ArchiveException(0x0000001A001FL);
		}
		byte[] encrypted = this.encryptor(masterSessionKey, ivData).finish(randomData);
		if (encrypted.length != randomData.length) {
			RawUtils.clear(encrypted);
			throw new ArchiveException(0x0000001A001FL);
		}
		return encrypted;
	}

	@Override
	public CryptoAdaptor encryptor(final byte[] sessionKey, final byte[] ivData) throws CryptoException {
		validate(sessionKey);
		return CryptoUtils.RC4Encryptor(sessionKey);
	}

	@Override
	public byte[] decryptRandomData(final byte[] encryptedRandomData, final byte[] masterSessionKey, final byte[] ivData)
			throws CryptoException, ArchiveException {
		if (encryptedRandomData == null
				|| encryptedRandomData.length == 0) {
			throw new ArchiveException(0x0000001A001FL);
		}
		byte[] decrypted = this.decryptor(masterSessionKey, ivData).finish(encryptedRandomData);
		if (decrypted.length != encryptedRandomData.length) {
			RawUtils.clear(decrypted);
			throw new ArchiveException(0x0000001A001FL);
		}
		return decrypted;
	}

	@Override
	public CryptoAdaptor decryptor(final byte[] sessionKey, final byte[] ivData) throws CryptoException {
		validate(sessionKey);
		return CryptoUtils.RC4Decryptor(sessionKey);
	}

	private static boolean supportedKeySize(final int keyBitLength) {
		return keyBitLength == 40 || keyBitLength == 64 || keyBitLength == 128;
	}

	private static void validate(final byte[] key) throws CryptoException {
		if (key == null || !supportedKeySize(key.length * Byte.SIZE)) {
			throw new CryptoException(0x0000001B0004L);
		}
	}
}
