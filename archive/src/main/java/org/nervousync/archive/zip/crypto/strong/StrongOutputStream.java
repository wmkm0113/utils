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

import org.nervousync.archive.enumerations.zip.StrongAccessMode;
import org.nervousync.archive.options.ArchiveLimit;
import org.nervousync.archive.security.StrongKey;
import org.nervousync.archive.zip.crypto.CryptoOutputStream;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.archive.zip.models.crypto.StrongRecipientTable;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.RawUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * <h2 class="en-US">Strong encryption output stream</h2>
 * <h2 class="zh-CN">强数据加密输出流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class StrongOutputStream extends CryptoOutputStream<StrongCryptoEngine> {

	/**
	 * <span class="en-US">Strong encryption information</span>
	 * <span class="zh-CN">强加密信息</span>
	 */
	private final StrongEncryptionInfo encryptionInfo;

	/**
	 * <h3 class="en-US">Constructor method for the strong encryption output stream</h3>
	 * <h3 class="zh-CN">AES 强数据加密输出流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source output stream</span>
	 *                     <span class="zh-CN">源输出流</span>
	 * @param algorithm    <span class="en-US">Algorithm code</span>
	 *                     <span class="zh-CN">算法代码</span>
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥比特长度</span>
	 * @throws IOException <span class="en-US">If read data failed or data invalid</span>
	 *                     <span class="zh-CN">读取出错或数据非法</span>
	 */
	public StrongOutputStream(final OutputStream sourceStream, final int algorithm,
	                          final int keyBitLength, final byte[] password) throws IOException {
		this(sourceStream, initialize(algorithm, keyBitLength, StrongAccessMode.PASSWORD, password, List.of()));
	}

	public static StrongOutputStream create(final OutputStream output, final int algorithm, final int keyBitLength,
	                                        final StrongAccessMode mode, final byte[] password,
	                                        final List<X509Certificate> recipients) throws IOException {
		return new StrongOutputStream(output, initialize(algorithm, keyBitLength, mode, password, List.copyOf(recipients)));
	}

	/**
	 * <h3 class="en-US">Constructor method for the strong encryption output stream</h3>
	 * <h3 class="zh-CN">AES 强数据加密输出流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Source output stream</span>
	 *                     <span class="zh-CN">源输出流</span>
	 * @param parameter    <span class="en-US">Encryption parameter information</span>
	 *                     <span class="zh-CN">强加密参数信息</span>
	 * @throws IOException <span class="en-US">If read data failed or data invalid</span>
	 *                     <span class="zh-CN">读取出错或数据非法</span>
	 */
	private StrongOutputStream(final OutputStream sourceStream, final Parameter parameter) throws IOException {
		super(sourceStream, parameter.engine);
		this.encryptionInfo = parameter.encryptionInfo;
		this.writeHeader(parameter.headerBytes);
	}

	/**
	 * <h3 class="en-US">Getter method for the strong encryption information</h3>
	 * <h3 class="zh-CN">强加密信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Strong encryption information</span>
	 * <span class="zh-CN">强加密信息</span>
	 */
	public StrongEncryptionInfo getEncryptionInfo() {
		return this.encryptionInfo;
	}

	/**
	 * <h3 class="en-US">Static method for initialize the strong encryption parameter</h3>
	 * <h3 class="zh-CN">静态方法用于初始化强加密参数</h3>
	 *
	 * @param algorithm    <span class="en-US">Algorithm code</span>
	 *                     <span class="zh-CN">算法代码</span>
	 * @param password     <span class="en-US">Password</span>
	 *                     <span class="zh-CN">密码</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥比特长度</span>
	 * @return <span class="en-US">Strong encryption parameter information</span>
	 * <span class="zh-CN">强加密参数信息</span>
	 * @throws IOException <span class="en-US">If read data failed or data invalid</span>
	 *                     <span class="zh-CN">读取出错或数据非法</span>
	 */
	private static Parameter initialize(final int algorithm, final int keyBitLength, final StrongAccessMode accessMode,
	                                    final byte[] password, final List<X509Certificate> certificates)
			throws IOException {
		Objects.requireNonNull(accessMode, "accessMode");
		StrongKey.validate(algorithm, keyBitLength);
		if (accessMode.usePassword() && (password == null || password.length == 0)) {
			throw new IOException("ZIP password is required");
		}
		if (accessMode.useCertificate() == certificates.isEmpty()) {
			throw new IOException("Recipient list does not match access mode");
		}

		int flags = StrongKey.writeFlags(algorithm, accessMode);

		EncryptionProvider cipher = EncryptionProvider.resolve(algorithm);
		int ivLength = cipher.usesPadding() ? cipher.blockSize() : 16;
		int erdAlgorithm = StrongKey.erdAlgorithm(algorithm, flags);
		int erdBits = StrongKey.erdBits(keyBitLength, flags);
		EncryptionProvider erdCipher = EncryptionProvider.resolve(erdAlgorithm);

		byte[] iv = new byte[ivLength];
		byte[] erdIv = null;
		byte[] randomData = new byte[32];
		byte[] validation = new byte[16];
		byte[] master = null, material = null, sessionKey = null, erd = null, encryptedValidation = null;

		try {
			Globals.randomBytes(iv);
			Globals.randomBytes(randomData);
			Globals.randomBytes(validation);

			cipher.validateHeader(algorithm, keyBitLength, flags, iv);
			erdIv = StrongKey.cipherIV(erdCipher, iv);
			master = accessMode.usePassword() ? erdCipher.deriveKey(password, erdBits)
					: StrongKey.generateMasterKey(erdAlgorithm, erdBits);
			StrongRecipientTable recipients = accessMode.useCertificate()
					? StrongKey.wrap(certificates, erdAlgorithm, erdBits, master) : null;
			erd = erdCipher.encryptRandomData(randomData, master, erdIv);
			material = RawUtils.join(iv, randomData);
			sessionKey = cipher.deriveKey(material, keyBitLength);
			CRC32 crc = new CRC32();
			crc.update(validation, 0, validation.length - 4);
			ByteBuffer.wrap(validation).order(ByteOrder.LITTLE_ENDIAN)
					.putInt(validation.length - 4, (int) crc.getValue());
			CryptoAdaptor adaptor = cipher.encryptor(sessionKey, iv);
			encryptedValidation = adaptor.update(validation);
			if (encryptedValidation.length != validation.length || erd.length > 65535) {
				throw new IOException("Invalid Strong header data length");
			}
			byte[] recipientBytes = recipients == null ? new byte[4]
					: recipients.toDecryptionBytes((int) Math.min(Integer.MAX_VALUE, ArchiveLimit.DEFAULT_LIMIT.getMaxHeaderBytes()));
			int bodySize = Math.addExact(12, Math.addExact(erd.length,
					Math.addExact(recipientBytes.length, encryptedValidation.length)));
			int headerSize = Math.addExact(6 + iv.length, bodySize);
			if (headerSize > ArchiveLimit.DEFAULT_LIMIT.getMaxHeaderBytes()) {
				throw new IOException("Strong header exceeds configured limit");
			}
			byte[] header = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN)
					.putShort((short) iv.length).put(iv).putInt(bodySize)
					.putShort((short) 3).putShort((short) algorithm).putShort((short) keyBitLength).putShort((short) flags)
					.putShort((short) erd.length).put(erd).put(recipientBytes)
					.putShort((short) encryptedValidation.length).put(encryptedValidation).array();
			StrongEncryptionInfo info = new StrongEncryptionInfo(algorithm, keyBitLength, flags,
					recipients == null ? new byte[0] : recipients.toCentralBytes());
			return new Parameter(new StrongCryptoEngine(CryptoMode.ENCRYPT, cipher, adaptor, sessionKey), header, info);
		} finally {
			RawUtils.clear(iv);
			RawUtils.clear(erdIv);
			RawUtils.clear(randomData);
			RawUtils.clear(validation);
			RawUtils.clear(master);
			RawUtils.clear(material);
			RawUtils.clear(sessionKey);
			RawUtils.clear(erd);
			RawUtils.clear(encryptedValidation);
		}
	}

	/**
	 * <h2 class="en-US">Strong encryption parameter information</h2>
	 * <h2 class="zh-CN">强数据加密参数信息</h2>
	 *
	 * @param engine         <span class="en-US">Strong encryption engine</span>
	 *                       <span class="zh-CN">强加密引擎</span>
	 * @param headerBytes    <span class="en-US">Encryption header</span>
	 *                       <span class="zh-CN">加密头</span>
	 * @param encryptionInfo <span class="en-US">Strong encryption information</span>
	 *                       <span class="zh-CN">强加密信息</span>
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
		private record Parameter(StrongCryptoEngine engine, byte[] headerBytes, StrongEncryptionInfo encryptionInfo) {
		/**
		 * <h3 class="en-US">Private constructor method for the strong encryption parameter information</h3>
		 * <h3 class="zh-CN">强数据加密参数信息的私有构造方法</h3>
		 *
		 * @param engine      <span class="en-US">Strong encryption engine</span>
		 *                    <span class="zh-CN">强加密引擎</span>
		 * @param headerBytes <span class="en-US">Encryption header</span>
		 *                    <span class="zh-CN">加密头</span>
		 */
		private Parameter {
		}
		}
}
