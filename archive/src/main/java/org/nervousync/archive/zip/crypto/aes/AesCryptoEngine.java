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

import jakarta.annotation.Nonnull;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.crypto.CryptoEngine;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.security.CryptoUtils;
import org.nervousync.utils.security.DigestUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/**
 * <h2 class="en-US">Zip AES crypto engine</h2>
 * <h2 class="zh-CN">Zip 的 AES 加密解密引擎</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class AesCryptoEngine extends CryptoEngine {

	/**
	 * <span class="en-US">Counter encryptor</span>
	 * <span class="zh-CN">计数器加密器</span>
	 */
	private final CryptoAdaptor counterEncryptor;
	/**
	 * <span class="en-US">Hmac calculator</span>
	 * <span class="zh-CN">Hmac 计算器</span>
	 */
	private final CryptoAdaptor macBasedPRF;
	/**
	 * <span class="en-US">Counter block</span>
	 * <span class="zh-CN">计数器数据块</span>
	 */
	private final byte[] counterBlock = new byte[16];
	/**
	 * <span class="en-US">Key stream</span>
	 * <span class="zh-CN">密钥流</span>
	 */
	private final byte[] keyStream = new byte[16];
	/**
	 * <span class="en-US">Authentication data</span>
	 * <span class="zh-CN">验证数据</span>
	 */
	private byte[] authentication = CryptoEngine.EMPTY_BYTES.clone();
	/**
	 * <span class="en-US">Encryption header data</span>
	 * <span class="zh-CN">加密头数据</span>
	 */
	private byte[] encryptionHeader = new byte[0];
	/**
	 * <span class="en-US">Stream offset</span>
	 * <span class="zh-CN">流偏移值</span>
	 */
	private int streamOffset = 16;
	/**
	 * <span class="en-US">Counter nonce</span>
	 * <span class="zh-CN">计数器</span>
	 */
	private long nonce = 1L;

	/**
	 * <h3 class="en-US">Constructor method for the Zip AES crypto engine</h3>
	 * <h3 class="zh-CN">Zip 的 AES 加密解密引擎的构造方法</h3>
	 *
	 * @param mode   <span class="en-US">Crypto mode</span>
	 *               <span class="zh-CN">加密解密模式</span>
	 * @param aesKey <span class="en-US">AES key</span>
	 *               <span class="zh-CN">AES 密钥</span>
	 * @param macKey <span class="en-US">Hmac key</span>
	 *               <span class="zh-CN">Hmac 密钥</span>
	 */
	public AesCryptoEngine(@Nonnull final CryptoMode mode, @Nonnull final byte[] aesKey, @Nonnull final byte[] macKey) {
		super(mode);
		this.counterEncryptor = CryptoUtils.AESEncryptor("ECB", "NoPadding", aesKey, null);
		this.macBasedPRF = DigestUtils.HmacSHA1(macKey);
	}

	@Override
	public byte[] process(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		byte[] output = Arrays.copyOfRange(buffer, offset, offset + length);
		switch (this.getMode()) {
			case ENCRYPT:
				this.processCtr(output);
				this.macBasedPRF.append(output, 0, output.length);
				break;
			case DECRYPT:
				this.macBasedPRF.append(output, 0, output.length);
				this.processCtr(output);
				break;
			default:
				throw new ArchiveException(0x0000001A0025L);
		}
		return output;
	}

	/**
	 * <h3 class="en-US">Calculate authentication data</h3>
	 * <h3 class="zh-CN">计算验证结果</h3>
	 *
	 * @return <span class="en-US">Authentication data</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	public byte[] calculateAuthentication() throws IOException {
		if (!this.finished) {
			throw new IOException("AES crypto engine has not finished");
		}
		if (this.closed) {
			throw new IOException("AES crypto engine closed");
		}
		return this.authentication.clone();
	}

	@Override
	public void close() {
		if (this.closed) {
			return;
		}
		RawUtils.clear(this.counterBlock);
		RawUtils.clear(this.keyStream);
		RawUtils.clear(this.authentication);
		RawUtils.clear(this.encryptionHeader);
		this.encryptionHeader = new byte[0];

		this.authentication = CryptoEngine.EMPTY_BYTES.clone();
		this.streamOffset = this.keyStream.length;
		this.nonce = 1L;
		this.closed = true;
	}

	@Override
	protected byte[] finishEngine() throws IOException {
		byte[] fullMac = this.macBasedPRF.finish();
		try {
			if (fullMac.length < ZipGlobals.AES_AUTH_LENGTH) {
				throw new IOException("Invalid HMAC-SHA1 result length");
			}
			RawUtils.clear(this.authentication);
			this.authentication = Arrays.copyOf(fullMac, ZipGlobals.AES_AUTH_LENGTH);
			return EMPTY_BYTES;
		} finally {
			RawUtils.clear(fullMac);
		}
	}

	/**
	 * <h3 class="en-US">Getter method for the encryption header data</h3>
	 * <h3 class="zh-CN">加密头数据的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Encryption header data</span>
	 * <span class="zh-CN">加密头数据</span>
	 */
	public byte[] getHeaderBytes() {
		return this.encryptionHeader.clone();
	}

	/**
	 * <h3 class="en-US">Initialize the AES crypto engine</h3>
	 * <h3 class="zh-CN">初始化 AES 加密解密引擎</h3>
	 *
	 * @param strength <span class="en-US">AES strength</span>
	 *                 <span class="zh-CN">AES 强度</span>
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @return <span class="en-US">AES crypto engine</span>
	 * <span class="zh-CN">AES 加密解密引擎</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	static AesCryptoEngine newEngine(final int strength, final byte[] password) throws IOException {
		Objects.requireNonNull(password, "password");
		AesConfig config = new AesConfig(strength);
		byte[] salt = new byte[config.saltLength], keyBytes = null;
		DeriveKey deriveKey = null;
		try {
			Globals.randomBytes(salt);

			int verifierLength = ZipGlobals.PASSWORD_VERIFIER_LENGTH;
			int verifierOffset = config.keyLength + config.macLength;
			int derivedLength = verifierOffset + verifierLength;

			keyBytes = ZipCrypto.deriveKey(salt, password, derivedLength);
			deriveKey = new DeriveKey(config, keyBytes);

			AesCryptoEngine engine = new AesCryptoEngine(CryptoMode.ENCRYPT, deriveKey.aesKey, deriveKey.macKey);

			engine.encryptionHeader = Arrays.copyOf(salt, salt.length + verifierLength);
			System.arraycopy(keyBytes, verifierOffset, engine.encryptionHeader, salt.length, verifierLength);
			return engine;
		} finally {
			RawUtils.clear(salt);
			RawUtils.clear(keyBytes);
			if (deriveKey != null) {
				RawUtils.clear(deriveKey.aesKey);
				RawUtils.clear(deriveKey.macKey);
			}
		}
	}

	/**
	 * <h3 class="en-US">Initialize the AES crypto engine</h3>
	 * <h3 class="zh-CN">初始化 AES 加密解密引擎</h3>
	 *
	 * @param sourceStream <span class="en-US">Current zip file input stream</span>
	 *                     <span class="zh-CN">当前压缩文件输入流</span>
	 * @param strength     <span class="en-US">AES strength</span>
	 *                     <span class="zh-CN">AES 强度</span>
	 * @param storedSize   <span class="en-US">Compressed size</span>
	 *                     <span class="zh-CN">压缩后大小</span>
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @return <span class="en-US">AES crypto engine</span>
	 * <span class="zh-CN">AES 加密解密引擎</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	static AesCryptoEngine newEngine(final InputStream sourceStream, final int strength,
	                                 final long storedSize, final byte[] password) throws IOException {
		Objects.requireNonNull(password, "password");
		AesConfig config = new AesConfig(strength);

		long overhead = config.saltLength + ZipGlobals.PASSWORD_VERIFIER_LENGTH + ZipGlobals.AES_AUTH_LENGTH;
		if (storedSize < overhead) {
			throw new EOFException("Invalid WinZip AES entry size");
		}

		byte[] salt = null, storedVerifier = null, keyBytes = null, passwordVerifier = null;
		DeriveKey deriveKey = null;
		try {
			salt = readFully(sourceStream, config.saltLength);
			storedVerifier = readFully(sourceStream, ZipGlobals.PASSWORD_VERIFIER_LENGTH);
			int dkLen = config.keyLength + config.macLength + ZipGlobals.PASSWORD_VERIFIER_LENGTH;
			keyBytes = ZipCrypto.deriveKey(salt, password, dkLen);

			deriveKey = new DeriveKey(config, keyBytes);
			passwordVerifier = new byte[ZipGlobals.PASSWORD_VERIFIER_LENGTH];

			System.arraycopy(keyBytes, (config.keyLength + config.macLength), passwordVerifier, 0, ZipGlobals.PASSWORD_VERIFIER_LENGTH);

			if (!MessageDigest.isEqual(storedVerifier, passwordVerifier)) {
				throw new ArchiveException(0x0000001B000DL);
			}

			return new AesCryptoEngine(CryptoMode.DECRYPT, deriveKey.aesKey, deriveKey.macKey);
		} finally {
			RawUtils.clear(salt);
			RawUtils.clear(storedVerifier);
			RawUtils.clear(keyBytes);
			if (deriveKey != null) {
				RawUtils.clear(deriveKey.aesKey);
				RawUtils.clear(deriveKey.macKey);
			}
			RawUtils.clear(passwordVerifier);
		}
	}

	static byte[] readFully(final InputStream inputStream, final int length) throws IOException {
		byte[] dataBytes = new byte[length];
		int readLength = 0;
		while (readLength < length) {
			int readLen = inputStream.read(dataBytes, readLength, length - readLength);
			if (readLen < 0) {
				throw new EOFException("Unexpected end of WinZip AES data");
			}
			readLength += readLen;
		}
		return dataBytes;
	}

	/**
	 * <h3 class="en-US">Process data</h3>
	 * <h3 class="zh-CN">处理数据</h3>
	 *
	 * @param buffer <span class="en-US">Buffer data</span>
	 *               <span class="zh-CN">缓冲数据</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when process data</span>
	 *                         <span class="zh-CN">处理过程中出错</span>
	 */
	private void processCtr(@Nonnull final byte[] buffer) throws CryptoException {
		for (int i = 0; i < buffer.length; i++) {
			if (this.streamOffset == this.keyStream.length) {
				if (this.nonce > 0xFFFFFFFFL) {
					throw new CryptoException(0x0000001A0029L);
				}
				RawUtils.clear(this.counterBlock);

				long value = this.nonce++;
				this.counterBlock[0] = (byte) value;
				this.counterBlock[1] = (byte) (value >>> 8);
				this.counterBlock[2] = (byte) (value >>> 16);
				this.counterBlock[3] = (byte) (value >>> 24);

				byte[] result = this.counterEncryptor.update(this.counterBlock, 0, this.counterBlock.length);
				if (result.length != this.keyStream.length) {
					throw new CryptoException(0x0000001A0024L);
				}
				System.arraycopy(result, 0, this.keyStream, 0, result.length);
				RawUtils.clear(result);
				this.streamOffset = 0;
			}
			buffer[i] ^= this.keyStream[this.streamOffset++];
		}
	}

	/**
	 * <h2 class="en-US">AES configure information</h2>
	 * <h2 class="zh-CN">AES 配置信息</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	private static final class AesConfig {

		/**
		 * <span class="en-US">Salt length</span>
		 * <span class="zh-CN">盐长度</span>
		 */
		private final int saltLength;
		/**
		 * <span class="en-US">Key length</span>
		 * <span class="zh-CN">密钥长度</span>
		 */
		private final int keyLength;
		/**
		 * <span class="en-US">Mac length</span>
		 * <span class="zh-CN">消息认证码长度</span>
		 */
		private final int macLength;

		/**
		 * <h3 class="en-US">Constructor method for the AES configure information</h3>
		 * <h3 class="zh-CN">AES 配置信息的构造方法</h3>
		 *
		 * @param strength <span class="en-US">AES strength</span>
		 *                 <span class="zh-CN">AES 强度</span>
		 * @throws IOException <span class="en-US">AES strength invalid</span>
		 *                     <span class="zh-CN">AES 强度错误</span>
		 */
		AesConfig(final int strength) throws IOException {
			switch (strength) {
				case ZipGlobals.AES_STRENGTH_128:
					saltLength = 8;
					keyLength = 16;
					macLength = 16;
					break;
				case ZipGlobals.AES_STRENGTH_192:
					saltLength = 12;
					keyLength = 24;
					macLength = 24;
					break;
				case ZipGlobals.AES_STRENGTH_256:
					saltLength = 16;
					keyLength = 32;
					macLength = 32;
					break;
				default:
					throw new ArchiveException(0x0000001A0014L, strength);
			}
		}
	}

	/**
	 * <h2 class="en-US">Derive key information</h2>
	 * <h2 class="zh-CN">派生密钥信息</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	private static final class DeriveKey {

		/**
		 * <span class="en-US">AES Key</span>
		 * <span class="zh-CN">AES 密钥</span>
		 */
		private final byte[] aesKey;
		/**
		 * <span class="en-US">MAC Key</span>
		 * <span class="zh-CN">消息认证密钥</span>
		 */
		private final byte[] macKey;

		/**
		 * <h3 class="en-US">Constructor method for the derive key information</h3>
		 * <h3 class="zh-CN">派生密钥信息的构造方法</h3>
		 *
		 * @param config   <span class="en-US">AES configure information</span>
		 *                 <span class="zh-CN">AES 配置信息</span>
		 * @param keyBytes <span class="en-US">Key bytes</span>
		 *                 <span class="zh-CN">密钥</span>
		 * @throws IOException <span class="en-US">Key bytes invalid</span>
		 *                     <span class="zh-CN">密钥数据错误</span>
		 */
		DeriveKey(final AesConfig config, final byte[] keyBytes) throws IOException {
			int dkLen = config.keyLength + config.macLength + ZipGlobals.PASSWORD_VERIFIER_LENGTH;
			if (keyBytes.length != dkLen) {
				throw new ArchiveException(0x0000001B0008L);
			}
			this.aesKey = new byte[config.keyLength];
			this.macKey = new byte[config.macLength];

			System.arraycopy(keyBytes, 0, aesKey, 0, config.keyLength);
			System.arraycopy(keyBytes, config.keyLength, macKey, 0, config.macLength);
		}
	}
}
