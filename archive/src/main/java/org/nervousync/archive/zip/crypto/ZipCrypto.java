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

package org.nervousync.archive.zip.crypto;

import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.models.crypto.StrongRecipientTable;
import org.nervousync.archive.zip.models.header.StrongDecryptionHeader;
import org.nervousync.archive.zip.security.EncryptionProvider;
import org.nervousync.commons.Globals;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.security.DigestUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * <h2 class="en-US">Zip archive crypto</h2>
 * <h2 class="zh-CN">归档文件加密解密</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ZipCrypto {

	/**
	 * <h3 class="en-US">Convert the password from character array to bytes array</h3>
	 * <h3 class="zh-CN">转换密码字符数组为字节数组</h3>
	 *
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @param utf8Encoding <span class="en-US">Using UTF-8 encoding flag</span>
	 *                     <span class="zh-CN">使用 UTF-8 编码标记</span>
	 * @return <span class="en-US">Bytes array</span>
	 * <span class="zh-CN">字节数组</span>
	 */
	public static byte[] encodePassword(final char[] password, final boolean utf8Encoding) throws ArchiveException {
		Objects.requireNonNull(password, "password cannot be null");
		if (password.length == 0) {
			throw new ArchiveException(0x0000001B0006L);
		}
		return utf8Encoding ? encodeUTF8(password) : encodeLegacy(password);
	}

	/**
	 * <h3 class="en-US">Derive key</h3>
	 * <h3 class="zh-CN">派生密钥</h3>
	 *
	 * @param saltBytes <span class="en-US">Salt bytes array</span>
	 *                 <span class="zh-CN">盐字节数组</span>
	 * @param password <span class="en-US">Password bytes array</span>
	 *                 <span class="zh-CN">密码字节数组</span>
	 * @param dkLen <span class="en-US">Derive key length</span>
	 *                 <span class="zh-CN">派生密钥长度</span>
	 * @return <span class="en-US">Derive key</span>
	 *                 <span class="zh-CN">派生密钥</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when process data</span>
	 *                         <span class="zh-CN">当处理数据时出现异常</span>
	 */
	public static byte[] deriveKey(final byte[] saltBytes, final byte[] password, final int dkLen)
			throws CryptoException {
		//	PBKDF2
		if (password == null || password.length == 0) {
			throw new NullPointerException();
		}
		byte[] passwordBytes = null, tempBytes = null;
		try {
			passwordBytes = password.clone();
			CryptoAdaptor digestProvider = DigestUtils.HmacSHA1(passwordBytes);

			int dkLength = (dkLen == 0) ? digestProvider.macLength() : dkLen;
			byte[] salt = (saltBytes == null) ? new byte[0] : saltBytes;

			int length = digestProvider.macLength();
			int m = 0;
			if (dkLength % length > 0) {
				m = 1;
			}
			int l = dkLength / length + m;
			int r = dkLength - (l - 1) * length;
			tempBytes = new byte[l * length];
			int offset = 0;
			for (int i = 1; i <= l; i++) {
				int len = digestProvider.macLength();
				byte[] tmpBytes = new byte[len];
				byte[] intTmpBytes = new byte[salt.length + 4];
				try {
					System.arraycopy(salt, Globals.INITIALIZE_INT_VALUE, intTmpBytes, Globals.INITIALIZE_INT_VALUE, salt.length);
					RawUtils.writeInt(intTmpBytes, salt.length, i);
					int round = 1000;
					while (round > 0) {
						intTmpBytes = digestProvider.finish(intTmpBytes);
						for (int j = 0; j < tmpBytes.length; j++) {
							tmpBytes[j] ^= intTmpBytes[j];
						}
						round--;
					}
					System.arraycopy(tmpBytes, Globals.INITIALIZE_INT_VALUE, tempBytes, offset, length);
				} finally {
					RawUtils.clear(tmpBytes);
					RawUtils.clear(intTmpBytes);
				}
				offset += length;
			}

			if (r < length) {
				byte[] bytes = new byte[dkLength];
				System.arraycopy(tempBytes, 0, bytes, 0, dkLength);
				return bytes;
			}
			return tempBytes.clone();
		} finally {
			RawUtils.clear(passwordBytes);
			RawUtils.clear(tempBytes);
		}
	}

	public static StrongDecryptionHeader readStrongDecryptionHeader(final DataInputStream inputStream,
	                                                                final long maxHeaderBytes) throws IOException {
		int ivLength = Short.toUnsignedInt(Short.reverseBytes(inputStream.readShort()));
		if (ivLength == 0) {
			throw new IOException("IVSize=0 requires entry CRC/size context; this overload does not reconstruct it");
		}
		if (maxHeaderBytes < 6L + ivLength) {
			throw new IOException("Strong header exceeds configured limit");
		}
		byte[] iv = new byte[ivLength];
		inputStream.readFully(iv);
		long size = Integer.toUnsignedLong(Integer.reverseBytes(inputStream.readInt()));
		long serializedSize = 6L + ivLength + size;
		if (size < 16 || size > Integer.MAX_VALUE || serializedSize > maxHeaderBytes) {
			throw new IOException("Invalid Strong header size");
		}
		byte[] bytes = new byte[(int) size];
		inputStream.readFully(bytes);
		ByteBuffer body = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		if (body.getShort() != 3) {
			throw new IOException("Invalid Strong decryption format");
		}
		int algorithm = Short.toUnsignedInt(body.getShort());
		int bits = Short.toUnsignedInt(body.getShort());
		int flags = Short.toUnsignedInt(body.getShort());
		if (!EncryptionProvider.validateFlags(flags)) {
			throw new IOException("Invalid Strong flags");
		}
		int erdSize = Short.toUnsignedInt(body.getShort());
		if (erdSize > body.remaining() - 6) {
			throw new IOException("Invalid ErdSize");
		}
		byte[] erd = new byte[erdSize];
		body.get(erd);
		StrongRecipientTable recipients = StrongRecipientTable.read(body, ZipGlobals.MAX_RECIPIENTS);
		if (((flags & 2) == 0) == (recipients != null)) {
			throw new IOException("Strong recipient mode mismatch");
		}
		if (body.remaining() < 2) {
			throw new IOException("Missing validation length");
		}
		int validationSize = Short.toUnsignedInt(body.getShort());
		if (validationSize <= 4 || validationSize != body.remaining()) {
			throw new IOException("Invalid validation size");
		}
		byte[] validation = new byte[validationSize];
		body.get(validation);
		return new StrongDecryptionHeader(iv, algorithm, bits, flags, erd, validation, serializedSize, recipients);
	}

	/**
	 * <h3 class="en-US">Convert the password from character array to bytes array</h3>
	 * <h3 class="zh-CN">转换密码字符数组为字节数组</h3>
	 *
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @return <span class="en-US">Bytes array</span>
	 * <span class="zh-CN">字节数组</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when convert data</span>
	 *                          <span class="zh-CN">转换编码过程中出错</span>
	 */
	private static byte[] encodeUTF8(final char[] password) throws ArchiveException {
		ByteBuffer encoded = null;
		try {
			encoded = StandardCharsets.UTF_8.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(password));
			byte[] result = new byte[encoded.remaining()];
			encoded.get(result);
			return result;
		} catch (CharacterCodingException e) {
			throw new ArchiveException(0x0000001A0027L, e);
		} finally {
			if (encoded != null && encoded.hasArray()) {
				Arrays.fill(encoded.array(), (byte) 0);
			}
		}
	}

	/**
	 * <h3 class="en-US">Convert the password from character array to bytes array</h3>
	 * <h3 class="zh-CN">转换密码字符数组为字节数组</h3>
	 *
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @return <span class="en-US">Bytes array</span>
	 * <span class="zh-CN">字节数组</span>
	 */
	private static byte[] encodeLegacy(final char[] password) {
		byte[] result = new byte[password.length];
		for (int i = 0; i < password.length; i++) {
			result[i] = (byte) password[i];
		}
		return result;
	}
}
