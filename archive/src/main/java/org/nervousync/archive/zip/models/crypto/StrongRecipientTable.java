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

package org.nervousync.archive.zip.models.crypto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <h2 class="en-US">Strong recipient mapping table</h2>
 * <h2 class="zh-CN">接收者映射表</h2>
 *
 * @param recipients <span class="en-US">Recipients list</span>
 *                   <span class="zh-CN">接收者列表</span>
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public record StrongRecipientTable(List<Recipient> recipients) {

	/**
	 * <h3 class="en-US">Constructor method for the strong recipient mapping table</h3>
	 * <h3 class="zh-CN">接收者映射表的构造方法</h3>
	 *
	 * @param recipients <span class="en-US">Recipients list</span>
	 *                   <span class="zh-CN">接收者列表</span>
	 */
	public StrongRecipientTable(final List<Recipient> recipients) {
		this.recipients = List.copyOf(recipients);
		if (this.recipients.isEmpty()) {
			throw new IllegalArgumentException("At least one certificate recipient is required");
		}
	}

	/**
	 * <h3 class="en-US">Static method for read strong recipient mapping table</h3>
	 * <h3 class="zh-CN">静态方法用于读取接收者映射表</h3>
	 *
	 * @param body          <span class="en-US">Data buffer</span>
	 *                      <span class="zh-CN">数据缓冲区</span>
	 * @param maxRecipients <span class="en-US">Maximum limit of recipients</span>
	 *                      <span class="zh-CN">最大接收者数量</span>
	 * @return <span class="en-US">Strong recipient mapping table</span>
	 * <span class="zh-CN">接收者映射表</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public static StrongRecipientTable read(final ByteBuffer body, final int maxRecipients) throws IOException {
		body.order(ByteOrder.LITTLE_ENDIAN);
		if (body.remaining() < 4) {
			throw new IOException("Truncated recipient count");
		}
		long count = Integer.toUnsignedLong(body.getInt());
		if (count == 0) {
			return null; // Password-only: there is no Reserved2 block.
		}
		if (count > maxRecipients || body.remaining() < 4) {
			throw new IOException("Recipient count exceeds limit or header is truncated");
		}
		int hashAlgorithm = Short.toUnsignedInt(body.getShort());
		int hashSize = Short.toUnsignedInt(body.getShort());
		if (hashAlgorithm != 0x8004 || hashSize != 20
				|| count > body.remaining() / (2L + hashSize + 12)) {
			throw new IOException("Unsupported recipient hash or invalid recipient count");
		}
		List<Recipient> recipients = new ArrayList<>();
		for (long index = 0; index < count; index++) {
			if (body.remaining() < 2) {
				throw new IOException("Truncated recipient element length");
			}
			int size = Short.toUnsignedInt(body.getShort());
			if (size < hashSize + 12 || size > body.remaining()) {
				throw new IOException("Invalid recipient element length");
			}
			byte[] hash = new byte[hashSize];
			byte[] blob = new byte[size - hashSize];
			body.get(hash).get(blob);
			recipients.add(new Recipient(hash, blob));
		}
		return new StrongRecipientTable(recipients);
	}

	/**
	 * <h3 class="en-US">Static method for read central directory data</h3>
	 * <h3 class="zh-CN">静态方法用于读取中央目录数据</h3>
	 *
	 * @param data          <span class="en-US">Central directory data</span>
	 *                      <span class="zh-CN">中央目录数据</span>
	 * @param maxRecipients <span class="en-US">Maximum limit of recipients</span>
	 *                      <span class="zh-CN">最大接收者数量</span>
	 * @return <span class="en-US">Data list</span>
	 * <span class="zh-CN">数据列表</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public static List<byte[]> readCentral(final byte[] data, final int maxRecipients) throws IOException {
		if (data == null || data.length < 8) {
			throw new IOException("Truncated central certificate data");
		}
		ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		long count = Integer.toUnsignedLong(buffer.getInt());
		int algorithm = Short.toUnsignedInt(buffer.getShort());
		int hashSize = Short.toUnsignedInt(buffer.getShort());
		if (count == 0 || count > maxRecipients || algorithm != 0x8004 || hashSize != 20
				|| count * hashSize != buffer.remaining()) {
			throw new IOException("Invalid central certificate data");
		}
		List<byte[]> hashes = new ArrayList<>();
		for (long index = 0; index < count; index++) {
			byte[] hash = new byte[hashSize];
			buffer.get(hash);
			hashes.add(hash);
		}
		return List.copyOf(hashes);
	}

	/**
	 * <h3 class="en-US">Getter method for the recipients list</h3>
	 * <h3 class="zh-CN">接收者列表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Recipients list</span>
	 * <span class="zh-CN">接收者列表</span>
	 */
	@Override
	public List<Recipient> recipients() {
		return this.recipients;
	}

	/**
	 * <h3 class="en-US">Convert to decryption data</h3>
	 * <h3 class="zh-CN">转换为解密数据</h3>
	 *
	 * @param maxBytes <span class="en-US">Maximum limit bytes</span>
	 *                 <span class="zh-CN">最大字节数</span>
	 * @return <span class="en-US">Decryption data</span>
	 * <span class="zh-CN">解密数据</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public byte[] toDecryptionBytes(final int maxBytes) throws IOException {
		long size = 8;
		for (Recipient recipient : recipients) {
			size += 2L + 20 + recipient.keyBlob.length;
		}
		if (maxBytes < 8 || size > maxBytes) {
			throw new IOException("Certificate recipient table exceeds header limit");
		}
		ByteBuffer result = ByteBuffer.allocate((int) size).order(ByteOrder.LITTLE_ENDIAN);
		result.putInt(recipients.size()).putShort((short) 0x8004).putShort((short) 20);
		for (Recipient recipient : recipients) {
			result.putShort((short) (20 + recipient.keyBlob.length));
			result.put(recipient.publicKeyHash).put(recipient.keyBlob);
		}
		return result.array();
	}

	/**
	 * <h3 class="en-US">Convert to central directory data</h3>
	 * <h3 class="zh-CN">转换为中央目录数据</h3>
	 *
	 * @return <span class="en-US">Central directory data</span>
	 * <span class="zh-CN">中央目录数据</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public byte[] toCentralBytes() throws IOException {
		long size = 8L + 20L * recipients.size();
		if (size > 65535 - 8) { // Eight bytes are already used by the Strong extra-field fixed part.
			throw new IOException("Strong certificate extra field exceeds 65535 bytes");
		}
		ByteBuffer result = ByteBuffer.allocate((int) size).order(ByteOrder.LITTLE_ENDIAN);
		result.putInt(recipients.size()).putShort((short) 0x8004).putShort((short) 20);
		for (Recipient recipient : recipients) {
			result.put(recipient.publicKeyHash);
		}
		return result.array();
	}

	/**
	 * <h3 class="en-US">Verify central directory data</h3>
	 * <h3 class="zh-CN">验证中央目录数据</h3>
	 *
	 * @param centralData   <span class="en-US">Central directory data</span>
	 *                      <span class="zh-CN">中央目录数据</span>
	 * @param maxRecipients <span class="en-US">Maximum limit of recipients</span>
	 *                      <span class="zh-CN">最大接收者数量</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public void verifyCentral(byte[] centralData, int maxRecipients) throws IOException {
		List<byte[]> hashes = readCentral(centralData, maxRecipients);
		if (hashes.size() != recipients.size()) {
			throw new IOException("Central and decryption recipient counts differ");
		}
		boolean[] matched = new boolean[recipients.size()];
		for (byte[] hash : hashes) {
			boolean found = false;
			for (int index = 0; index < recipients.size(); index++) {
				if (!matched[index] && MessageDigest.isEqual(hash, recipients.get(index).publicKeyHash)) {
					matched[index] = true;
					found = true;
					break;
				}
			}
			if (!found) {
				throw new IOException("Central and decryption recipient hashes differ");
			}
		}
	}

	/**
	 * <h2 class="en-US">Recipient information</h2>
	 * <h2 class="zh-CN">接收者信息</h2>
	 *
	 * @param publicKeyHash <span class="en-US">Public key hash</span>
	 *                      <span class="zh-CN">公钥哈希值</span>
	 * @param keyBlob       <span class="en-US">Key blob</span>
	 *                      <span class="zh-CN">编码后的数据</span>
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
		public record Recipient(byte[] publicKeyHash, byte[] keyBlob) {

			/**
			 * <h3 class="en-US">Constructor method for the recipient information</h3>
			 * <h3 class="zh-CN">接收者信息的构造方法</h3>
			 *
			 * @param publicKeyHash <span class="en-US">Public key hash</span>
			 *                      <span class="zh-CN">公钥哈希值</span>
			 * @param keyBlob       <span class="en-US">Key blob</span>
			 *                      <span class="zh-CN">编码后的数据</span>
			 */
			public Recipient(final byte[] publicKeyHash, final byte[] keyBlob) {
				this.publicKeyHash = Objects.requireNonNull(publicKeyHash, "publicKeyHash").clone();
				this.keyBlob = Objects.requireNonNull(keyBlob, "keyBlob").clone();
				if (this.publicKeyHash.length != 20 || this.keyBlob.length < 12 || this.keyBlob.length > 65535 - 20) {
					throw new IllegalArgumentException("Invalid recipient element");
				}
			}

			/**
			 * <h3 class="en-US">Getter method for the public key hash</h3>
			 * <h3 class="zh-CN">公钥哈希值的 Getter 方法</h3>
			 *
			 * @return <span class="en-US">Public key hash</span>
			 * <span class="zh-CN">公钥哈希值</span>
			 */
			@Override
			public byte[] publicKeyHash() {
				return this.publicKeyHash;
			}

			/**
			 * <h3 class="en-US">Getter method for the key blob</h3>
			 * <h3 class="zh-CN">编码后的数据的 Getter 方法</h3>
			 *
			 * @return <span class="en-US">Key blob</span>
			 * <span class="zh-CN">编码后的数据</span>
			 */
			@Override
			public byte[] keyBlob() {
				return this.keyBlob;
			}
		}
}
