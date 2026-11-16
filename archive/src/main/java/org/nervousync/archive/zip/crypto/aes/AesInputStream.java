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

package org.nervousync.archive.zip.crypto.aes;

import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.crypto.CryptoInputStream;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.utils.commons.RawUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * <h2 class="en-US">AES decryption input stream</h2>
 * <h2 class="zh-CN">AES 数据解密输入流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class AesInputStream extends CryptoInputStream<AesCryptoEngine> {

	/**
	 * <span class="en-US">Data authenticated flag</span>
	 * <span class="zh-CN">数据验证标记</span>
	 */
	private boolean authenticated = false;

	/**
	 * <h3 class="en-US">Constructor method for the AES decryption input stream</h3>
	 * <h3 class="zh-CN">AES 数据解密输入流的构造方法</h3>
	 *
	 * @param sourceStream   <span class="en-US">Current zip file input stream</span>
	 *                       <span class="zh-CN">当前压缩文件输入流</span>
	 * @param encryptionInfo <span class="en-US">AES encryption information</span>
	 *                       <span class="zh-CN">AES 加密信息</span>
	 * @param storedSize     <span class="en-US">Compressed size</span>
	 *                       <span class="zh-CN">压缩后大小</span>
	 * @param password       <span class="en-US">Password</span>
	 *                       <span class="zh-CN">密码</span>
	 * @throws IOException <span class="en-US">If an error occurs when read data or data invalid</span>
	 *                     <span class="zh-CN">读取数据出错或数据非法</span>
	 */
	public AesInputStream(final InputStream sourceStream, final AesEncryptionInfo encryptionInfo,
	                      final long storedSize, final byte[] password) throws IOException {
		super(sourceStream, storedSize, AesCryptoEngine.newEngine(sourceStream, encryptionInfo.getStrength(), storedSize, password));
		this.remaining = storedSize - overheadSize(encryptionInfo.getStrength());
	}

	private static int overheadSize(final int strength) throws IOException {
		return switch (strength) {
			case ZipGlobals.AES_STRENGTH_128 -> 8 + ZipGlobals.PASSWORD_VERIFIER_LENGTH + ZipGlobals.AES_AUTH_LENGTH;
			case ZipGlobals.AES_STRENGTH_192 -> 12 + ZipGlobals.PASSWORD_VERIFIER_LENGTH + ZipGlobals.AES_AUTH_LENGTH;
			case ZipGlobals.AES_STRENGTH_256 -> 16 + ZipGlobals.PASSWORD_VERIFIER_LENGTH + ZipGlobals.AES_AUTH_LENGTH;
			default -> throw new ArchiveException(0x0000001A0014L, strength);
		};
	}

	@Override
	protected void afterEngineFinished() throws IOException {
		if (this.authenticated) {
			return;
		}

		byte[] storedAuthentication = null, verifierAuthentication = null;
		try {
			storedAuthentication = AesCryptoEngine.readFully(this.in, ZipGlobals.AES_AUTH_LENGTH);
			verifierAuthentication = this.engine.calculateAuthentication();
			if (!MessageDigest.isEqual(storedAuthentication, verifierAuthentication)) {
				throw new ArchiveException(0x0000001B0019L);
			}
			this.authenticated = true;
		} catch (CryptoException e) {
			throw new ArchiveException(0x0000001B0019L, e);
		} finally {
			RawUtils.clear(storedAuthentication);
			RawUtils.clear(verifierAuthentication);
		}
	}
}
