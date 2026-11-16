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

package org.nervousync.archive.zip.crypto.strong;

import jakarta.annotation.Nonnull;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.crypto.CryptoEngine;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.RawUtils;

import java.io.IOException;
import java.util.Arrays;

/**
 * <h2 class="en-US">Zip strong crypto engine</h2>
 * <h2 class="zh-CN">Zip 的强加密解密引擎</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class StrongCryptoEngine extends CryptoEngine {

	/**
	 * <span class="en-US">Cipher instance object</span>
	 * <span class="zh-CN">加密解密器</span>
	 */
	private final EncryptionProvider cipher;
	/**
	 * <span class="en-US">Decryptor instance object</span>
	 * <span class="zh-CN">解密器实例对象</span>
	 */
	private final CryptoAdaptor adaptor;
	/**
	 * <span class="en-US">Session key</span>
	 * <span class="zh-CN">会话密钥</span>
	 */
	private byte[] sessionKey;
	/**
	 * <span class="en-US">Expected length</span>
	 * <span class="zh-CN">预期长度</span>
	 */
	private final long expectedLength;
	/**
	 * <span class="en-US">Input length</span>
	 * <span class="zh-CN">输入长度</span>
	 */
	private long inputLength;
	/**
	 * <span class="en-US">Output length</span>
	 * <span class="zh-CN">输出长度</span>
	 */
	private long outputLength;
	/**
	 * <span class="en-US">Pending block</span>
	 * <span class="zh-CN">等待处理的数据块</span>
	 */
	private byte[] pendingBlock = CryptoEngine.EMPTY_BYTES.clone();

	/**
	 * <h3 class="en-US">Constructor method for the zip strong crypto engine</h3>
	 * <h3 class="zh-CN">Zip 的强加密解密引擎的构造方法</h3>
	 *
	 * @param mode           <span class="en-US">Crypto mode</span>
	 *                       <span class="zh-CN">加密解密模式</span>
	 * @param cipher         <span class="en-US">Cipher instance object</span>
	 *                       <span class="zh-CN">加密解密器</span>
	 * @param adaptor        <span class="en-US">Decryptor instance object</span>
	 *                       <span class="zh-CN">解密器实例对象</span>
	 * @param sessionKey     <span class="en-US">Session key</span>
	 *                       <span class="zh-CN">会话密钥</span>
	 */
	public StrongCryptoEngine(final CryptoMode mode, final EncryptionProvider cipher, final CryptoAdaptor adaptor,
	                          @Nonnull final byte[] sessionKey) {
		this(mode, cipher, adaptor, sessionKey, Globals.DEFAULT_VALUE_LONG);
	}

	/**
	 * <h3 class="en-US">Constructor method for the zip strong crypto engine</h3>
	 * <h3 class="zh-CN">Zip 的强加密解密引擎的构造方法</h3>
	 *
	 * @param mode           <span class="en-US">Crypto mode</span>
	 *                       <span class="zh-CN">加密解密模式</span>
	 * @param cipher         <span class="en-US">Cipher instance object</span>
	 *                       <span class="zh-CN">加密解密器</span>
	 * @param adaptor        <span class="en-US">Decryptor instance object</span>
	 *                       <span class="zh-CN">解密器实例对象</span>
	 * @param sessionKey     <span class="en-US">Session key</span>
	 *                       <span class="zh-CN">会话密钥</span>
	 * @param expectedLength <span class="en-US">Expected length</span>
	 *                       <span class="zh-CN">预期长度</span>
	 */
	public StrongCryptoEngine(final CryptoMode mode, final EncryptionProvider cipher, final CryptoAdaptor adaptor,
	                          @Nonnull final byte[] sessionKey, final long expectedLength) {
		super(mode);
		this.cipher = cipher;
		this.adaptor = adaptor;
		if (cipher.blockSize() <= 0) {
			throw new IllegalArgumentException("Block size must be positive");
		}
		if (cipher.usesPadding() && cipher.blockSize() > 0xFF) {
			throw new IllegalArgumentException("PKCS#7 blockSize must not exceed 255");
		}
		this.sessionKey = sessionKey.clone();
		this.expectedLength = expectedLength;
	}

	@Override
	protected byte[] process(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		return switch (this.getMode()) {
			case ENCRYPT -> this.encrypt(buffer, offset, length);
			case DECRYPT -> this.decrypt(buffer, offset, length);
			default -> throw new ArchiveException(0x0000001A0028L);
		};
	}

	@Override
	protected byte[] finishEngine() throws IOException {
		return switch (this.getMode()) {
			case ENCRYPT -> this.finishEncryption();
			case DECRYPT -> this.finishDecryption();
			default -> throw new ArchiveException(0x0000001A0028L);
		};
	}

	/**
	 * <h3 class="en-US">Encrypt data</h3>
	 * <h3 class="zh-CN">加密数据</h3>
	 *
	 * @param buffer <span class="en-US">Buffer</span>
	 *               <span class="zh-CN">缓冲区</span>
	 * @param offset <span class="en-US">Offset value</span>
	 *               <span class="zh-CN">偏移值</span>
	 * @param length <span class="en-US">Data length</span>
	 *               <span class="zh-CN">数据长度</span>
	 * @return <span class="en-US">Encrypted data</span>
	 * <span class="zh-CN">加密后的数据</span>
	 * @throws IOException <span class="en-US">Data length invalid</span>
	 *                     <span class="zh-CN">数据长度非法</span>
	 */
	private byte[] encrypt(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		long nextLength = Math.addExact(this.inputLength, length);
		if (this.expectedLength != Globals.DEFAULT_VALUE_LONG && nextLength > this.expectedLength) {
			throw this.invalidData();
		}
		byte[] encrypted = this.adaptor.update(buffer, offset, length);
		this.inputLength = nextLength;
		this.outputLength = Math.addExact(this.outputLength, encrypted.length);
		return encrypted;
	}

	/**
	 * <h3 class="en-US">Finish encrypt data</h3>
	 * <h3 class="zh-CN">完成加密数据</h3>
	 *
	 * @return <span class="en-US">Encrypted data</span>
	 * <span class="zh-CN">加密后的数据</span>
	 * @throws IOException <span class="en-US">Data length invalid</span>
	 *                     <span class="zh-CN">数据长度非法</span>
	 */
	private byte[] finishEncryption() throws IOException {
		byte[] padding = CryptoEngine.EMPTY_BYTES.clone();
		byte[] encrypted = null;
		boolean successful = false;

		try {
			if (this.expectedLength != Globals.DEFAULT_VALUE_LONG && this.inputLength != this.expectedLength) {
				throw this.invalidData();
			}

			if (this.cipher.usesPadding()) {
				int paddingLength = this.cipher.paddingSize(this.inputLength);

				if (paddingLength < 1 || paddingLength > this.cipher.blockSize()) {
					throw this.invalidData();
				}

				padding = new byte[paddingLength];
				Arrays.fill(padding, (byte) paddingLength);
			}

			encrypted = this.adaptor.finish(padding);

			long totalLength = Math.addExact(this.outputLength, encrypted.length);
			long expectedLength = this.cipher.encryptedSize(this.inputLength);
			if (totalLength != expectedLength) {
				throw this.invalidData();
			}
			this.outputLength = totalLength;
			this.destroySensitiveState();
			successful = true;
			return encrypted;
		} catch (CryptoException e) {
			throw new ArchiveException(0x0000001A001FL, e);
		} finally {
			this.destroySensitiveState();
			RawUtils.clear(padding);
			if (!successful) {
				RawUtils.clear(encrypted);
			}
		}
	}

	/**
	 * <h3 class="en-US">Decrypt data</h3>
	 * <h3 class="zh-CN">解密数据</h3>
	 *
	 * @param buffer <span class="en-US">Buffer</span>
	 *               <span class="zh-CN">缓冲区</span>
	 * @param offset <span class="en-US">Offset value</span>
	 *               <span class="zh-CN">偏移值</span>
	 * @param length <span class="en-US">Data length</span>
	 *               <span class="zh-CN">数据长度</span>
	 * @return <span class="en-US">Decrypted data</span>
	 * <span class="zh-CN">解密后的数据</span>
	 * @throws IOException <span class="en-US">Data length invalid</span>
	 *                     <span class="zh-CN">数据长度非法</span>
	 */
	private byte[] decrypt(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		long nextLength = Math.addExact(this.inputLength, length);
		byte[] decrypted = this.adaptor.update(buffer, offset, length);

		this.inputLength = nextLength;
		if (this.cipher.usesPadding()) {
			return this.holdLastBlock(decrypted);
		}

		long outputLength = Math.addExact(this.outputLength, decrypted.length);

		if (this.expectedLength != Globals.DEFAULT_VALUE_LONG && outputLength > this.expectedLength) {
			throw this.invalidData();
		}
		this.outputLength = outputLength;
		return decrypted;
	}

	/**
	 * <h3 class="en-US">Finish decrypt data</h3>
	 * <h3 class="zh-CN">完成解密数据</h3>
	 *
	 * @return <span class="en-US">Encrypted data</span>
	 * <span class="zh-CN">加密后的数据</span>
	 * @throws IOException <span class="en-US">Data length invalid</span>
	 *                     <span class="zh-CN">数据长度非法</span>
	 */
	private byte[] finishDecryption() throws IOException {
		byte[] finished = null;
		byte[] complete = null;
		byte[] plaintext = null;
		boolean successful = false;

		try {
			finished = this.adaptor.finish();
			complete = RawUtils.join(this.pendingBlock, finished);

			this.clearPendingBlock();

			if (this.cipher.usesPadding()) {
				if (this.inputLength == 0L || this.inputLength % this.cipher.blockSize() != 0) {
					throw this.invalidData();
				}

				plaintext = this.removeAndValidatePadding(complete);
			} else {
				plaintext = complete.clone();
			}

			long totalPlainLength = Math.addExact(this.outputLength, plaintext.length);

			if (this.expectedLength != Globals.DEFAULT_VALUE_LONG && totalPlainLength != this.expectedLength) {
				throw this.invalidData();
			}

			this.outputLength = totalPlainLength;

			/*
			 * The engine cannot be used again after finish().
			 */
			this.destroySensitiveState();

			successful = true;
			return plaintext;
		} catch (CryptoException e) {
			throw new ArchiveException(0x0000001A001FL, e);
		} finally {
			this.destroySensitiveState();
			RawUtils.clear(finished);
			RawUtils.clear(complete);

			if (!successful) {
				RawUtils.clear(plaintext);
			}
		}
	}

	private byte[] holdLastBlock(final byte[] decrypted) throws IOException {
		byte[] complete = null;

		try {
			complete = RawUtils.join(this.pendingBlock, decrypted);
			if (complete.length == 0) {
				return CryptoEngine.EMPTY_BYTES.clone();
			}

			int blockSize = this.cipher.blockSize();
			/*
			 * Cipher.update() for AES/CBC/NoPadding must only return
			 * complete blocks.
			 */
			if (complete.length % blockSize != 0) {
				throw this.invalidData();
			}
			if (complete.length <= blockSize) {
				this.replacePendingBlock(complete, 0, complete.length);
				return CryptoEngine.EMPTY_BYTES.clone();
			}

			int outputLength = complete.length - blockSize;
			long nextPlainLength = Math.addExact(this.outputLength, outputLength);
			if (this.expectedLength != Globals.DEFAULT_VALUE_LONG && nextPlainLength > this.expectedLength) {
				throw this.invalidData();
			}

			byte[] output = Arrays.copyOf(complete, outputLength);

			this.replacePendingBlock(complete, outputLength, blockSize);

			this.outputLength = nextPlainLength;
			return output;
		} finally {
			RawUtils.clear(decrypted);
			RawUtils.clear(complete);
		}
	}

	private byte[] removeAndValidatePadding(final byte[] complete) throws IOException {
		if (complete == null || complete.length == 0 || complete.length % this.cipher.blockSize() != 0) {
			throw this.invalidData();
		}

		int paddingLength = Byte.toUnsignedInt(complete[complete.length - 1]);
		if (paddingLength < 1 || paddingLength > this.cipher.blockSize() || paddingLength > complete.length) {
			throw this.invalidData();
		}

		int plainLength = complete.length - paddingLength;
		for (int i = plainLength; i < complete.length; i++) {
			if (Byte.toUnsignedInt(complete[i]) != paddingLength) {
				throw this.invalidData();
			}
		}

		return Arrays.copyOf(complete, plainLength);
	}

	private void replacePendingBlock(final byte[] source, final int offset, final int length) {
		this.clearPendingBlock();
		if (length == 0) {
			return;
		}
		this.pendingBlock = Arrays.copyOfRange(source, offset, offset + length);
	}

	private void clearPendingBlock() {
		RawUtils.clear(this.pendingBlock);
		this.pendingBlock = CryptoEngine.EMPTY_BYTES.clone();
	}

	/**
	 * <h3 class="en-US">Abort operate and return the invalid data exception</h3>
	 * <h3 class="zh-CN">终止操作并抛出数据非法异常</h3>
	 *
	 * @return <span class="en-US">The invalid data exception</span>
	 * <span class="zh-CN">数据非法异常</span>
	 */
	private ArchiveException invalidData() {
		return new ArchiveException(0x0000001A001FL);
	}

	private void clearSessionKey() {
		RawUtils.clear(this.sessionKey);
		this.sessionKey = CryptoEngine.EMPTY_BYTES.clone();
	}

	private void destroySensitiveState() {
		this.clearPendingBlock();
		this.clearSessionKey();
	}

	@Override
	public void close() {
		if (this.closed) {
			return;
		}
		this.destroySensitiveState();
		this.closed = true;
	}
}
