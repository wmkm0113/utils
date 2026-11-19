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

package org.nervousync.archive.security;

import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.enumerations.zip.StrongAccessMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.security.certificate.CertificateCredentials;
import org.nervousync.archive.security.certificate.CertificateProvider;
import org.nervousync.archive.security.certificate.CertificateRequest;
import org.nervousync.archive.zip.models.crypto.StrongRecipientTable;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.commons.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.*;

/**
 * <h2 class="en-US">Strong key</h2>
 * <h2 class="zh-CN">强密钥</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class StrongKey {

	/**
	 * <span class="en-US">Microsoft SIMPLEBLOB</span>
	 * <span class="zh-CN">Microsoft SIMPLEBLOB</span>
	 */
	private static final int ALGORITHM_RC2 = 0x00006602;

	/**
	 * <h3 class="en-US">Validate algorithm and key bit length</h3>
	 * <h3 class="zh-CN">验证算法参数</h3>
	 *
	 * @param algorithm    <span class="en-US">Algorithm</span>
	 *                     <span class="zh-CN">算法</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @throws IOException <span class="en-US">Argument invalid</span>
	 *                     <span class="zh-CN">参数非法</span>
	 */
	public static void validate(final int algorithm, final int keyBitLength) throws IOException {
		boolean valid = switch (algorithm) {
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES -> keyBitLength == 56 || keyBitLength == 64;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112 -> keyBitLength == 112;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168 -> keyBitLength == 168;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128 -> keyBitLength == 128;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192 -> keyBitLength == 192;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 -> keyBitLength == 256;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4 ->
					keyBitLength == 40 || keyBitLength == 64 || keyBitLength == 128;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH ->
					keyBitLength >= 32 && keyBitLength <= 320 && (keyBitLength & 7) == 0;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH ->
					keyBitLength == 128 || keyBitLength == 192 || keyBitLength == 256;
			default -> false;
		};
		if (!valid) {
			throw new ArchiveException(0x0000001A0025L);
		}
	}

	/**
	 * <h3 class="en-US">Write flags</h3>
	 * <h3 class="zh-CN">写标记</h3>
	 *
	 * @param algorithm  <span class="en-US">Algorithm</span>
	 *                   <span class="zh-CN">算法</span>
	 * @param accessMode <span class="en-US">Strong encryption access mode</span>
	 *                   <span class="zh-CN">强加密访问模式</span>
	 * @return <span class="en-US">Flags</span>
	 * <span class="zh-CN">标记</span>
	 */
	public static int writeFlags(final int algorithm, final StrongAccessMode accessMode) {
		int flags = accessMode.flags();
		if (accessMode.useCertificate()) {
			flags |= ZipGlobals.ZIP64_HEADER_ENC_FLAGS_NON_OAEP;
			if (algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH
					|| algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH) {
				flags |= ZipGlobals.ZIP64_HEADER_ENC_FLAGS_ERD_3DES;
			}
		}
		return flags;
	}

	/**
	 * <h3 class="en-US">ERD algorithm</h3>
	 * <h3 class="zh-CN">ERD 算法</h3>
	 *
	 * @param algorithm <span class="en-US">Algorithm</span>
	 *                  <span class="zh-CN">算法</span>
	 * @param flags     <span class="en-US">Flags</span>
	 *                  <span class="zh-CN">标记</span>
	 * @return <span class="en-US">ERD algorithm</span>
	 * <span class="zh-CN">ERD 算法</span>
	 */
	public static int erdAlgorithm(final int algorithm, final int flags) {
		return (flags & ZipGlobals.ZIP64_HEADER_ENC_FLAGS_ERD_3DES) != 0
				? ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168
				: algorithm;
	}

	/**
	 * <h3 class="en-US">ERD key bit length</h3>
	 * <h3 class="zh-CN">ERD 密钥长度</h3>
	 *
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @param flags        <span class="en-US">Flags</span>
	 *                     <span class="zh-CN">标记</span>
	 * @return <span class="en-US">Key bit length</span>
	 * <span class="zh-CN">密钥长度</span>
	 */
	public static int erdBits(final int keyBitLength, final int flags) {
		return (flags & ZipGlobals.ZIP64_HEADER_ENC_FLAGS_ERD_3DES) != 0 ? 168 : keyBitLength;
	}

	/**
	 * <h3 class="en-US">Process iv data</h3>
	 * <h3 class="zh-CN">处理向量数据</h3>
	 *
	 * @param cipher <span class="en-US">Encryption provider</span>
	 *               <span class="zh-CN">加密解密接口</span>
	 * @param iv     <span class="en-US">IV data</span>
	 *               <span class="zh-CN">向量数据</span>
	 * @return <span class="en-US">IV data</span>
	 * <span class="zh-CN">向量数据</span>
	 */
	public static byte[] cipherIV(final EncryptionProvider cipher, final byte[] iv) {
		return cipher.usesPadding() ? Arrays.copyOf(iv, cipher.blockSize()) : iv.clone();
	}

	/**
	 * <h3 class="en-US">Generate master key</h3>
	 * <h3 class="zh-CN">生成主密钥</h3>
	 *
	 * @param algorithm    <span class="en-US">Algorithm</span>
	 *                     <span class="zh-CN">算法</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @return <span class="en-US">Master key</span>
	 * <span class="zh-CN">主密钥</span>
	 * @throws IOException <span class="en-US">Argument invalid</span>
	 *                     <span class="zh-CN">参数非法</span>
	 */
	public static byte[] generateMasterKey(final int algorithm, final int keyBitLength) throws IOException {
		validate(algorithm, keyBitLength);
		byte[] seed = new byte[32];
		try {
			Globals.randomBytes(seed);
			return EncryptionProvider.resolve(algorithm).deriveKey(seed, keyBitLength);
		} finally {
			RawUtils.clear(seed);
		}
	}

	/**
	 * <h3 class="en-US">Blob algorithm</h3>
	 * <h3 class="zh-CN">Blob 算法</h3>
	 *
	 * @param algorithm <span class="en-US">Algorithm</span>
	 *                  <span class="zh-CN">算法</span>
	 * @return <span class="en-US">Algorithm</span>
	 * <span class="zh-CN">算法</span>
	 * @throws IOException <span class="en-US">Argument invalid</span>
	 *                     <span class="zh-CN">参数非法</span>
	 */
	public static int blobAlgorithm(final int algorithm) throws IOException {
		return switch (algorithm) {
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112,
			     ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128,
			     ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256,
			     ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4 -> algorithm;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW -> ALGORITHM_RC2;
			default -> throw new ArchiveException(0x0000001A0025L);
		};
	}

	/**
	 * <h3 class="en-US">Encode blob key</h3>
	 * <h3 class="zh-CN">编码 Blob 密钥</h3>
	 *
	 * @param algorithm    <span class="en-US">Algorithm</span>
	 *                     <span class="zh-CN">算法</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @param javaKey      <span class="en-US">Key data</span>
	 *                     <span class="zh-CN">密钥数据</span>
	 * @return <span class="en-US">Encoded data</span>
	 * <span class="zh-CN">编码后的数据</span>
	 * @throws IOException <span class="en-US">Key data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public static byte[] encodeBlobKey(final int algorithm, final int keyBitLength, final byte[] javaKey)
			throws IOException {
		validateJavaKey(algorithm, keyBitLength, javaKey);
		blobAlgorithm(algorithm);
		if (algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112) {
			for (int i = 0; i < 8; i++) {
				if (javaKey[i] != javaKey[i + 16]) {
					throw new ArchiveException(0x0000001A004EL);
				}
			}
			return Arrays.copyOf(javaKey, 16);
		}
		return javaKey.clone();
	}

	/**
	 * <h3 class="en-US">Decode blob key</h3>
	 * <h3 class="zh-CN">解码 Blob 密钥</h3>
	 *
	 * @param algorithm    <span class="en-US">Algorithm</span>
	 *                     <span class="zh-CN">算法</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @param key          <span class="en-US">Data to be decoded</span>
	 *                     <span class="zh-CN">要解码的数据</span>
	 * @return <span class="en-US">Decoded data</span>
	 * <span class="zh-CN">解码后的数据</span>
	 * @throws IOException <span class="en-US">Key data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public static byte[] decodeBlobKey(final int algorithm, final int keyBitLength, final byte[] key)
			throws IOException {
		validate(algorithm, keyBitLength);
		blobAlgorithm(algorithm);

		if (algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112) {
			if (key.length != 16) {
				throw new ArchiveException(0x0000001A004EL);
			}
			byte[] decodeKey = Arrays.copyOf(key, 24);
			System.arraycopy(key, 0, decodeKey, 16, 8);
			return decodeKey;
		}
		validateJavaKey(algorithm, keyBitLength, key);
		return key.clone();
	}

	/**
	 * <h3 class="en-US">Needed version</h3>
	 * <h3 class="zh-CN">需要的版本号</h3>
	 *
	 * @param algorithm    <span class="en-US">Algorithm</span>
	 *                     <span class="zh-CN">算法</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @param flags        <span class="en-US">Flags</span>
	 *                     <span class="zh-CN">标记</span>
	 * @return <span class="en-US">Needed version</span>
	 * <span class="zh-CN">版本号</span>
	 */
	public static int neededVersion(final int algorithm, final int keyBitLength, final int flags) {
		int version = switch (algorithm) {
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192,
			     ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 -> 51;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW -> (keyBitLength == 64) ? 52 : 51;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH -> 63;
			default -> 50;
		};
		return (flags & ZipGlobals.ZIP64_HEADER_ENC_FLAGS_NON_OAEP) != 0 ? Math.max(version, 61) : version;
	}

	/**
	 * <h3 class="en-US">Verify recipients list</h3>
	 * <h3 class="zh-CN">验证接收者列表</h3>
	 *
	 * @param table        <span class="en-US">Strong recipients mapping table</span>
	 *                     <span class="zh-CN">接收者映射表</span>
	 * @param certificates <span class="en-US">Certificates list</span>
	 *                     <span class="zh-CN">证书列表</span>
	 * @throws IOException <span class="en-US">If an error occurs when verify recipients list</span>
	 *                     <span class="zh-CN">验证出错</span>
	 */
	public static void verifyRecipients(final StrongRecipientTable table, final List<X509Certificate> certificates)
			throws IOException {
		Set<String> expected = new HashSet<>();
		for (StrongRecipientTable.Recipient recipient : table.recipients()) {
			expected.add(StringUtils.base64Encode(recipient.publicKeyHash()));
		}
		Set<String> actual = new HashSet<>();
		try {
			for (X509Certificate certificate : certificates) {
				if (!actual.add(Base64.getEncoder().encodeToString(CertificateProvider.calculate(certificate.getPublicKey())))) {
					throw new IOException("Duplicate target recipient");
				}
			}
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
		if (certificates.size() != table.recipients().size() || !expected.equals(actual)) {
			throw new IOException("The complete original central-directory recipient set is required for merge");
		}
	}

	/**
	 * <h3 class="en-US">Wrap strong recipient mapping table</h3>
	 * <h3 class="zh-CN">包装接收者映射表</h3>
	 *
	 * @param certificates <span class="en-US">Certificates list</span>
	 *                     <span class="zh-CN">证书列表</span>
	 * @param erdAlgorithm <span class="en-US">ERD algorithm</span>
	 *                     <span class="zh-CN">ERD 算法</span>
	 * @param erdBits      <span class="en-US">ERC key bit length</span>
	 *                     <span class="zh-CN">ERD 加密密钥长度</span>
	 * @param masterKey    <span class="en-US">Master key</span>
	 *                     <span class="zh-CN">主密钥</span>
	 * @return <span class="en-US">Strong recipients mapping table</span>
	 * <span class="zh-CN">接收者映射表</span>
	 * @throws IOException <span class="en-US">If an error occurs when wrap certificates list</span>
	 *                     <span class="zh-CN">执行过程中出错</span>
	 */
	public static StrongRecipientTable wrap(List<X509Certificate> certificates, int erdAlgorithm,
	                                        int erdBits, byte[] masterKey) throws IOException {
		if (certificates.isEmpty() || certificates.size() > ZipGlobals.MAX_RECIPIENTS) {
			throw new IOException("Invalid certificate recipient count");
		}
		List<StrongRecipientTable.Recipient> recipients = new ArrayList<>();
		Set<String> identifiers = new HashSet<>();
		try {
			for (X509Certificate certificate : certificates) {
				byte[] hash = CertificateProvider.calculate(certificate.getPublicKey());
				if (!identifiers.add(Base64.getEncoder().encodeToString(hash))) {
					throw new IOException("Duplicate recipient public key");
				}
				recipients.add(new StrongRecipientTable.Recipient(hash,
						wrap(certificate, erdAlgorithm, erdBits, masterKey)));
			}
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
		return new StrongRecipientTable(recipients);
	}

	/**
	 * <h3 class="en-US">Unwrap candidates</h3>
	 * <h3 class="zh-CN">拆包接收者映射表</h3>
	 *
	 * @param archivePath  <span class="en-US">Archive file path</span>
	 *                     <span class="zh-CN">归档文件地址</span>
	 * @param entryPath    <span class="en-US">Entry path</span>
	 *                     <span class="zh-CN">条目地址</span>
	 * @param central      <span class="en-US">Central directory flags</span>
	 *                     <span class="zh-CN">中央目录标记</span>
	 * @param erdAlgorithm <span class="en-US">ERD algorithm</span>
	 *                     <span class="zh-CN">ERD 算法</span>
	 * @param erdBits      <span class="en-US">ERC key bit length</span>
	 *                     <span class="zh-CN">ERD 加密密钥长度</span>
	 * @param flags        <span class="en-US">Flags</span>
	 *                     <span class="zh-CN">标记</span>
	 * @param table        <span class="en-US">Strong recipients mapping table</span>
	 *                     <span class="zh-CN">接收者映射表</span>
	 * @param provider     <span class="en-US">Certificate provider</span>
	 *                     <span class="zh-CN">证书提供适配器</span>
	 * @return <span class="en-US">Candidates list</span>
	 * <span class="zh-CN">候选数据列表</span>
	 * @throws IOException <span class="en-US">If an error occurs when unwrap candidates</span>
	 *                     <span class="zh-CN">执行过程中出错</span>
	 */
	public static List<byte[]> unwrapCandidates(final String archivePath, final String entryPath, final boolean central,
	                                            final int erdAlgorithm, final int erdBits, final int flags,
	                                            final StrongRecipientTable table, final CertificateProvider provider)
			throws IOException {
		List<byte[]> candidates = new ArrayList<>();
		boolean successful = false;
		try {
			for (StrongRecipientTable.Recipient recipient : table.recipients()) {
				byte[] hash = recipient.publicKeyHash();
				Optional<CertificateCredentials> value = provider.credentials(
						new CertificateRequest(archivePath, entryPath, central, 0x8004, hash));
				if (value.isEmpty()) {
					continue;
				}
				CertificateCredentials credentials = value.get();
				try {
					if (!MessageDigest.isEqual(hash, CertificateProvider.calculate(credentials.getCertificate().getPublicKey()))) {
						continue;
					}
					candidates.add(unwrap(credentials, erdAlgorithm, erdBits, flags, recipient.keyBlob()));
				} catch (NoSuchAlgorithmException | IOException rejectedKey) {
					// A different matching recipient can still be usable.
				}
			}
			successful = true;
			return candidates;
		} finally {
			if (!successful) {
				candidates.forEach(RawUtils::clear);
			}
		}
	}

	/**
	 * <h3 class="en-US">Wrap Java key</h3>
	 * <h3 class="zh-CN">包装 Java 密钥</h3>
	 *
	 * @param certificate  <span class="en-US">Certificate</span>
	 *                     <span class="zh-CN">证书</span>
	 * @param algorithm    <span class="en-US">Algorithm</span>
	 *                     <span class="zh-CN">算法</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @param javaKey      <span class="en-US">Java key</span>
	 *                     <span class="zh-CN">Java 密钥</span>
	 * @return <span class="en-US">Wrapped data</span>
	 * <span class="zh-CN">包装后的数据</span>
	 * @throws IOException <span class="en-US">Arguments invalid</span>
	 *                     <span class="zh-CN">参数非法</span>
	 */
	public static byte[] wrap(final X509Certificate certificate, final int algorithm, final int keyBitLength,
	                          final byte[] javaKey) throws IOException {
		byte[] key = null, encrypted = null;
		try {
			if (!(certificate.getPublicKey() instanceof RSAPublicKey publicKey)) {
				throw new ArchiveException(0x0000001A004DL);
			}
			if (publicKey.getModulus().bitLength() < 2048) {
				throw new IOException("RSA key is too short for new archives");
			}
			certificate.checkValidity();
			boolean[] usage = certificate.getKeyUsage();
			if (usage != null && (usage.length < 3 || !usage[2])) {
				throw new IOException("Certificate forbids key encipherment");
			}
			key = encodeBlobKey(algorithm, keyBitLength, javaKey);
			Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			rsa.init(Cipher.ENCRYPT_MODE, publicKey, new SecureRandom());
			encrypted = rsa.doFinal(key);
			reverse(encrypted);
			return ByteBuffer.allocate(12 + encrypted.length).order(ByteOrder.LITTLE_ENDIAN)
					.put((byte) 1).put((byte) 2).putShort((short) 0)
					.putInt(blobAlgorithm(algorithm)).putInt(0xA400)
					.put(encrypted).array();
		} catch (Exception e) {
			throw (e instanceof IOException) ? (IOException) e : new IOException(e);
		} finally {
			RawUtils.clear(key);
			RawUtils.clear(encrypted);
		}
	}

	/**
	 * <h3 class="en-US">Unwrap Java key</h3>
	 * <h3 class="zh-CN">解码 Java 密钥</h3>
	 *
	 * @param credentials  <span class="en-US">Certificate credentials</span>
	 *                     <span class="zh-CN">证书凭证</span>
	 * @param algorithm    <span class="en-US">Algorithm</span>
	 *                     <span class="zh-CN">算法</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @param flags        <span class="en-US">Flags</span>
	 *                     <span class="zh-CN">标记</span>
	 * @param blob         <span class="en-US">Wrapped data</span>
	 *                     <span class="zh-CN">编码的数据</span>
	 * @return <span class="en-US">Unwrapped data</span>
	 * <span class="zh-CN">解码的数据</span>
	 * @throws IOException <span class="en-US">Arguments invalid</span>
	 *                     <span class="zh-CN">参数非法</span>
	 */
	public static byte[] unwrap(final CertificateCredentials credentials, final int algorithm, final int keyBitLength,
	                            final int flags, final byte[] blob) throws IOException {
		byte[] key = null, encrypted = null;
		try {
			if (!(credentials.getCertificate().getPublicKey() instanceof RSAPublicKey)) {
				throw new IOException("RSA certificate required");
			}
			int modulusSize = (((RSAPublicKey) credentials.getCertificate().getPublicKey()).getModulus().bitLength() + 7) / 8;
			if (blob.length != 12 + modulusSize) {
				throw new IOException("Invalid SIMPLEBLOB length");
			}
			ByteBuffer buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
			if (buffer.get() != 1 || buffer.get() != 2 || buffer.getShort() != 0
					|| buffer.getInt() != blobAlgorithm(algorithm) || buffer.getInt() != 0xA400) {
				throw new IOException("Invalid SIMPLEBLOB header");
			}
			encrypted = new byte[modulusSize];
			buffer.get(encrypted);
			reverse(encrypted);
			boolean nonOaep = (flags & ZipGlobals.ZIP64_HEADER_ENC_FLAGS_NON_OAEP) != 0;
			String transformation = nonOaep ? "RSA/ECB/PKCS1Padding" : "RSA/ECB/OAEPPadding";
			Provider provider = credentials.getProvider();
			Cipher rsa = provider == null ? Cipher.getInstance(transformation) : Cipher.getInstance(transformation, provider);
			if (nonOaep) {
				rsa.init(Cipher.DECRYPT_MODE, credentials.getPrivateKey());
			} else {
				rsa.init(Cipher.DECRYPT_MODE, credentials.getPrivateKey(),
						new OAEPParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT));
			}
			key = rsa.doFinal(encrypted);
			return decodeBlobKey(algorithm, keyBitLength, key);
		} catch (Exception e) {
			throw (e instanceof IOException) ? (IOException) e : new IOException(e);
		} finally {
			RawUtils.clear(key);
			RawUtils.clear(encrypted);
		}
	}

	/**
	 * <h3 class="en-US">Validate Java key</h3>
	 * <h3 class="zh-CN">验证 Java 密钥</h3>
	 *
	 * @param algorithm    <span class="en-US">Algorithm</span>
	 *                     <span class="zh-CN">算法</span>
	 * @param keyBitLength <span class="en-US">Key bit length</span>
	 *                     <span class="zh-CN">密钥长度</span>
	 * @param key          <span class="en-US">Key data</span>
	 *                     <span class="zh-CN">密钥数据</span>
	 * @throws IOException <span class="en-US">Key data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	private static void validateJavaKey(final int algorithm, final int keyBitLength, final byte[] key)
			throws IOException {
		validate(algorithm, keyBitLength);
		int length;
		if (algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES) {
			length = 8;
		} else if (algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168
				|| algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112) {
			length = 24;
		} else {
			length = keyBitLength / 8;
		}
		if (key == null || key.length != length) {
			throw new ArchiveException(0x0000001A004EL);
		}
	}

	private static void reverse(final byte[] bytes) {
		for (int left = 0, right = bytes.length - 1; left < right; left++, right--) {
			byte value = bytes[left];
			bytes[left] = bytes[right];
			bytes[right] = value;
		}
	}

	private StrongKey() {
	}
}
