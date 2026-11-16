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

import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.enumerations.PasswordPurpose;
import org.nervousync.archive.enumerations.StrongAccessMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.security.StrongKey;
import org.nervousync.archive.security.certificate.CertificateProvider;
import org.nervousync.archive.security.password.PasswordCredentials;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.security.password.PasswordRequest;
import org.nervousync.archive.zip.crypto.CryptoInputStream;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.archive.zip.models.header.StrongDecryptionHeader;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.RawUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.CRC32;

/**
 * <h2 class="en-US">Strong decryption input stream</h2>
 * <h2 class="zh-CN">强解密输入流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class StrongInputStream extends CryptoInputStream<StrongCryptoEngine> {

	/**
	 * <h3 class="en-US">Constructor method for the strong decryption input stream</h3>
	 * <h3 class="zh-CN">强解密输入流的构造方法</h3>
	 *
	 * @param sourceStream <span class="en-US">Current zip file input stream</span>
	 *                     <span class="zh-CN">当前压缩文件输入流</span>
	 * @param remaining    <span class="en-US">Remaining length</span>
	 *                     <span class="zh-CN">剩余长度</span>
	 * @param engine       <span class="en-US">Crypto engine</span>
	 *                     <span class="zh-CN">加密解密引擎</span>
	 */
	private StrongInputStream(final InputStream sourceStream, final long remaining, final StrongCryptoEngine engine) {
		super(sourceStream, remaining, engine);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the strong decryption input stream using to read central directory</h3>
	 * <h3 class="zh-CN">静态方法用于初始化中央仓库强解密输入流</h3>
	 *
	 * @param archivePath         <span class="en-US">Archive file path</span>
	 *                            <span class="zh-CN">归档文件地址</span>
	 * @param sourceStream        <span class="en-US">Current zip file input stream</span>
	 *                            <span class="zh-CN">当前压缩文件输入流</span>
	 * @param protection          <span class="en-US">Zip64 end of central directory protection</span>
	 *                            <span class="zh-CN">Zip64 中央目录保护</span>
	 * @param passwordProvider    <span class="en-US">Password provider</span>
	 *                            <span class="zh-CN">密码提供适配器</span>
	 * @param certificateProvider <span class="en-US">Certificate provider</span>
	 *                            <span class="zh-CN">证书提供适配器</span>
	 * @param decryptionHeader    <span class="en-US">Decryption header information</span>
	 *                            <span class="zh-CN">解密头信息</span>
	 * @return <span class="en-US">Strong input stream instance object</span>
	 * <span class="zh-CN">强解密输入流实例对象</span>
	 * @throws IOException <span class="en-US">If read data failed or data invalid</span>
	 *                     <span class="zh-CN">读取出错或数据非法</span>
	 */
	public static StrongInputStream centralDirectory(final String archivePath, final InputStream sourceStream,
	                                                 final Zip64EndCentralDirectory.Protection protection,
	                                                 final PasswordProvider passwordProvider,
	                                                 final CertificateProvider certificateProvider,
	                                                 final StrongDecryptionHeader decryptionHeader) throws IOException {
		Objects.requireNonNull(archivePath, "archivePath");
		Objects.requireNonNull(sourceStream, "sourceStream");
		Objects.requireNonNull(protection, "protection");
		Objects.requireNonNull(decryptionHeader, "decryptionHeader");

		if (protection.getAlgorithm() != decryptionHeader.algorithm()
				|| protection.getKeyBitLength() != decryptionHeader.keyBitLength()
				|| protection.getFlags() != decryptionHeader.flags()) {
			throw new ArchiveException(0x0000001A001FL);
		}

		EncryptionProvider cipher = EncryptionProvider.resolve(decryptionHeader.algorithm());
		byte[] ivData = decryptionHeader.ivData();
		cipher.validateHeader(decryptionHeader.algorithm(), decryptionHeader.keyBitLength(),
				decryptionHeader.flags(), ivData);

		long plainSize = protection.getCompressedSize();
		if (plainSize < 0L) {
			throw new ArchiveException(0x0000001A001FL);
		}
		long encryptedSize;
		try {
			encryptedSize = cipher.encryptedSize(plainSize);
		} catch (ArithmeticException e) {
			throw new ArchiveException(0x0000001A001FL, e);
		}
		return new StrongInputStream(sourceStream, encryptedSize,
				initEngine(archivePath, Globals.DEFAULT_VALUE_STRING, passwordProvider, certificateProvider,
						PasswordPurpose.READ_HEADER, cipher, ivData, plainSize, decryptionHeader));
	}

	/**
	 * <h3 class="en-US">Static method for initialize the strong decryption input stream using to read archive entry</h3>
	 * <h3 class="zh-CN">静态方法用于初始化归档条目强解密输入流</h3>
	 *
	 * @param archivePath         <span class="en-US">Archive file path</span>
	 *                            <span class="zh-CN">归档文件地址</span>
	 * @param entryPath           <span class="en-US">Archive entry path</span>
	 *                            <span class="zh-CN">条目路径</span>
	 * @param sourceStream        <span class="en-US">Current zip file input stream</span>
	 *                            <span class="zh-CN">当前压缩文件输入流</span>
	 * @param storedSize          <span class="en-US">Stored size</span>
	 *                            <span class="zh-CN">存储大小</span>
	 * @param passwordProvider    <span class="en-US">Password provider</span>
	 *                            <span class="zh-CN">密码提供适配器</span>
	 * @param certificateProvider <span class="en-US">Certificate provider</span>
	 *                            <span class="zh-CN">证书提供适配器</span>
	 * @param encryptionInfo      <span class="en-US">Strong encryption information</span>
	 *                            <span class="zh-CN">强加密信息</span>
	 * @param decryptionHeader    <span class="en-US">Decryption header information</span>
	 *                            <span class="zh-CN">解密头信息</span>
	 * @return <span class="en-US">Strong input stream instance object</span>
	 * <span class="zh-CN">强解密输入流实例对象</span>
	 * @throws IOException <span class="en-US">If read data failed or data invalid</span>
	 *                     <span class="zh-CN">读取出错或数据非法</span>
	 */
	public static StrongInputStream entry(final String archivePath, final String entryPath, final InputStream sourceStream,
	                                      final long storedSize, final PasswordProvider passwordProvider,
	                                      final CertificateProvider certificateProvider,
	                                      final StrongEncryptionInfo encryptionInfo,
	                                      final StrongDecryptionHeader decryptionHeader) throws IOException {
		Objects.requireNonNull(archivePath, "archivePath");
		Objects.requireNonNull(entryPath, "entryPath");
		Objects.requireNonNull(sourceStream, "sourceStream");
		Objects.requireNonNull(encryptionInfo, "encryptionInfo");
		Objects.requireNonNull(decryptionHeader, "decryptionHeader");

		if (encryptionInfo.getAlgorithm() != decryptionHeader.algorithm()
				|| encryptionInfo.getKeyBitLength() != decryptionHeader.keyBitLength()
				|| encryptionInfo.getFlags() != decryptionHeader.flags()) {
			throw new ArchiveException(0x0000001A001FL);
		}

		StrongAccessMode accessMode = StrongAccessMode.from(encryptionInfo.getFlags());
		if (accessMode.useCertificate()) {
			if (decryptionHeader.recipients() == null) {
				throw new IOException("Missing Strong recipient table");
			}
			decryptionHeader.recipients().verifyCentral(encryptionInfo.getCertificateData(), ZipGlobals.MAX_RECIPIENTS);
		} else if (encryptionInfo.getCertificateData().length != 0 || decryptionHeader.recipients() != null) {
			throw new IOException("Unexpected certificate recipients in password-only entry");
		}

		EncryptionProvider cipher = EncryptionProvider.resolve(decryptionHeader.algorithm());
		cipher.validateHeader(decryptionHeader.algorithm(), decryptionHeader.keyBitLength(),
				decryptionHeader.flags(), decryptionHeader.ivData());
		long headerSize = decryptionHeader.serializedSize();

		if (headerSize < 0L || storedSize < headerSize) {
			throw new ArchiveException(0x0000001A001FL);
		}

		long encryptedSize = storedSize - headerSize;
		if (cipher.usesPadding()) {
			if (encryptedSize == 0L || encryptedSize % cipher.blockSize() != 0L) {
				throw new ArchiveException(0x0000001A001FL);
			}
		}
		return new StrongInputStream(sourceStream, encryptedSize,
				initEngine(archivePath, entryPath, passwordProvider, certificateProvider,
						PasswordPurpose.READ_ENTRY, cipher, decryptionHeader.ivData(),
						Globals.DEFAULT_VALUE_LONG, decryptionHeader));
	}

	private static StrongCryptoEngine initEngine(final String archivePath, final String entryPath,
	                                             final PasswordProvider passwordProvider,
	                                             final CertificateProvider certificateProvider,
	                                             final PasswordPurpose purpose,
	                                             final EncryptionProvider cipher, final byte[] ivData, final long expectSize,
	                                             final StrongDecryptionHeader decryptionHeader) throws IOException {
		StrongAccessMode accessMode = StrongAccessMode.from(decryptionHeader.flags());
		int erdAlgorithm = StrongKey.erdAlgorithm(decryptionHeader.algorithm(), decryptionHeader.flags());
		int erdBits = StrongKey.erdBits(decryptionHeader.keyBitLength(), decryptionHeader.flags());
		EncryptionProvider erdCipher = EncryptionProvider.resolve(erdAlgorithm);
		List<byte[]> candidates = new ArrayList<>();
		byte[] erdIv = StrongKey.cipherIV(erdCipher, ivData);
		IOException lastFailure = null;
		try {
			if (accessMode.useCertificate() && certificateProvider != null) {
				if (decryptionHeader.recipients() == null) {
					throw new IOException("Missing Strong recipient table");
				}

				candidates.addAll(StrongKey.unwrapCandidates(archivePath, entryPath,
						purpose == PasswordPurpose.READ_HEADER, erdAlgorithm, erdBits,
						decryptionHeader.flags(), decryptionHeader.recipients(), certificateProvider));

			}
			if (candidates.isEmpty() && accessMode.usePassword() && passwordProvider != null) {
				char[] chars = null;
				byte[] bytes = null;

				Optional<PasswordCredentials> value =
						passwordProvider.credentials(new PasswordRequest(purpose, archivePath, entryPath, 1));

				if (value.isPresent()) {
					try (PasswordCredentials credentials = value.get()) {
						chars = credentials.copyPassword();
						bytes = ZipCrypto.encodePassword(chars, credentials.isUtf8Encoding());
						if (bytes.length != 0) {
							candidates.add(erdCipher.deriveKey(bytes, erdBits));
						}
					} finally {
						RawUtils.clear(chars);
						RawUtils.clear(bytes);
					}
				}
			} else if (candidates.isEmpty()) {
				throw new IOException("No usable credentials for this ZIP stream");
			}
			for (byte[] master : candidates) {
				byte[] randomData = null, material = null, fileKey = null, validation = null;
				try {
					randomData = erdCipher.decryptRandomData(decryptionHeader.encryptedRandomData(), master, erdIv);
					material = RawUtils.join(ivData, randomData);
					fileKey = cipher.deriveKey(material, decryptionHeader.keyBitLength());
					CryptoAdaptor decryptor = cipher.decryptor(fileKey, ivData);
					validation = decryptor.update(decryptionHeader.encryptedValidationData());
					if (validation.length != decryptionHeader.encryptedValidationData().length) {
						throw new IOException("Invalid validation block length");
					}
					verifyValidationData(validation);
					return new StrongCryptoEngine(CryptoMode.DECRYPT, cipher, decryptor, fileKey, expectSize);
				} catch (IOException | org.nervousync.exceptions.crypto.CryptoException failure) {
					lastFailure = failure instanceof IOException ? (IOException) failure : new IOException(failure);
				} finally {
					RawUtils.clear(randomData);
					RawUtils.clear(material);
					RawUtils.clear(fileKey);
					RawUtils.clear(validation);
				}
			}
			throw (lastFailure == null)
					? new IOException("No supplied password or recipient private key could decrypt this ZIP stream")
					: lastFailure;
		} finally {
			candidates.forEach(RawUtils::clear);
			RawUtils.clear(erdIv);
		}
	}

	/**
	 * <h3 class="en-US">Verify the validation data</h3>
	 * <h3 class="zh-CN">验证加密数据的合法性</h3>
	 *
	 * @param validationData <span class="en-US">Validation data</span>
	 *                       <span class="zh-CN">验证数据</span>
	 * @throws ArchiveException <span class="en-US">If the validation data was invalid</span>
	 *                          <span class="zh-CN">数据非法</span>
	 */
	private static void verifyValidationData(final byte[] validationData) throws ArchiveException {
		if (validationData == null || validationData.length <= Integer.BYTES) {
			throw new ArchiveException(0x0000001A001FL);
		}
		int validationDataLength = validationData.length - Integer.BYTES;
		long expectedCRC = RawUtils.readUnsignedInt(validationData, validationDataLength, ByteOrder.LITTLE_ENDIAN);
		CRC32 crc32 = new CRC32();
		crc32.update(validationData, 0, validationDataLength);
		if (crc32.getValue() != expectedCRC) {
			throw new ArchiveException(0x0000001B000DL);
		}
	}
}
