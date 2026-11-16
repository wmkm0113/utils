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

package org.nervousync.archive.zip.crypto.standard;

import jakarta.annotation.Nonnull;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.crypto.CryptoEngine;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.utils.commons.RawUtils;

import java.util.Arrays;

/**
 * <h2 class="en-US">Zip standard crypto engine</h2>
 * <h2 class="zh-CN">Zip 的标准加密解密引擎</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class StandardCryptoEngine extends CryptoEngine {

	/**
	 * <span class="en-US">CRC table</span>
	 * <span class="zh-CN">CRC 表</span>
	 */
	private static final int[] CRC_TABLE = CRC_TABLE();

	/**
	 * <span class="en-US">Keys</span>
	 * <span class="zh-CN">密钥</span>
	 */
	private final int[] keys = new int[3];
	/**
	 * <span class="en-US">Marked keys</span>
	 * <span class="zh-CN">标记的密钥</span>
	 */
	private int[] markedKeys = new int[3];
	/**
	 * <span class="en-US">Encrypt header</span>
	 * <span class="zh-CN">加密头</span>
	 */
	private byte[] headerBytes = null;

	/**
	 * <h3 class="en-US">Constructor method for the Zip standard crypto engine</h3>
	 * <h3 class="zh-CN">Zip 的标准加密解密引擎的构造方法</h3>
	 *
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @param crc      <span class="en-US">CRC value</span>
	 *                 <span class="zh-CN">CRC 值</span>
	 */
	public StandardCryptoEngine(final byte[] password, final int crc) {
		super(CryptoMode.ENCRYPT);
		this.initKey(password);
		this.headerBytes = new byte[ZipGlobals.STD_DEC_HDR_SIZE];
		for (int i = 0; i < ZipGlobals.STD_DEC_HDR_SIZE; i++) {
			this.headerBytes[i] = this.encryptByte((byte) Globals.random(256));
		}
		this.initKey(password);
		this.headerBytes[ZipGlobals.STD_DEC_HDR_SIZE - 1] = (byte) (crc >>> 24);
		this.headerBytes[ZipGlobals.STD_DEC_HDR_SIZE - 2] = (byte) (crc >>> 16);
		this.encrypt(this.headerBytes);
	}

	/**
	 * <h3 class="en-US">Constructor method for the Zip standard crypto engine</h3>
	 * <h3 class="zh-CN">Zip 的标准加密解密引擎的构造方法</h3>
	 *
	 * @param password <span class="en-US">Password</span>
	 *                 <span class="zh-CN">密码</span>
	 * @param header   <span class="en-US">Decrypt header</span>
	 *                 <span class="zh-CN">解密头</span>
	 */
	public StandardCryptoEngine(final byte[] password, final byte[] header, final int checkByte) throws ArchiveException {
		super(CryptoMode.DECRYPT);
		this.initKey(password);

		byte[] encryptedHeader = header.clone();
		try {
			this.decrypt(encryptedHeader);

			if ((encryptedHeader[ZipGlobals.STD_DEC_HDR_SIZE - 1] & 0xFF) != checkByte) {
				throw new ArchiveException(0x0000001A002AL);
			}
		} finally {
			RawUtils.clear(encryptedHeader);
		}
	}

	/**
	 * <h3 class="en-US">Encrypt data</h3>
	 * <h3 class="zh-CN">加密数据</h3>
	 *
	 * @param buffer <span class="en-US">Buffer data</span>
	 *               <span class="zh-CN">缓冲数据</span>
	 */
	private void encrypt(@Nonnull final byte[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = encryptByte(buffer[i]);
		}
	}

	/**
	 * <h3 class="en-US">Getter method for the encrypt header</h3>
	 * <h3 class="zh-CN">加密头的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Encrypt header</span>
	 * <span class="zh-CN">加密头</span>
	 */
	public byte[] getHeaderBytes() {
		return this.headerBytes;
	}

	public void mark() {
		this.markedKeys = this.keys.clone();
	}

	public void reset() {
		int i = 0;
		for (int key : this.markedKeys) {
			this.keys[i] = key;
			i++;
		}
		this.markedKeys = null;
	}

	private byte encryptByte(final byte b) {
		byte temp = (byte) (b ^ this.processByte() & 0xFF);
		this.updateKeys(b);
		return temp;
	}

	/**
	 * <h3 class="en-US">Decrypt data</h3>
	 * <h3 class="zh-CN">解密数据</h3>
	 *
	 * @param buffer <span class="en-US">Buffer data</span>
	 *               <span class="zh-CN">缓冲数据</span>
	 */
	private void decrypt(@Nonnull final byte[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			int value = buffer[i] & 0xFF;
			value = (value ^ this.processByte()) & 0xFF;
			this.updateKeys((byte) value);
			buffer[i] = (byte) value;
		}
	}

	@Override
	public void close() {
		this.keys[0] = 0x12345678;
		this.keys[1] = 0x23456789;
		this.keys[2] = 0x34567890;
		RawUtils.clear(this.headerBytes);
	}

	private void initKey(final byte[] password) {
		this.keys[0] = 0x12345678;
		this.keys[1] = 0x23456789;
		this.keys[2] = 0x34567890;
		for (byte b : password) {
			this.updateKeys(b);
		}
	}

	/**
	 * Decrypt byte.
	 *
	 * @return the byte
	 */
	private byte processByte() {
		int temp = this.keys[2] | 2;
		return (byte) ((temp * (temp ^ 1)) >>> 8);
	}

	/**
	 * Update keys.
	 *
	 * @param b the b
	 */
	private void updateKeys(final byte b) {
		this.keys[0] = crc32(this.keys[0], b);
		this.keys[1] += this.keys[0] & 0xff;
		this.keys[1] = this.keys[1] * 134775813 + 1;
		this.keys[2] = crc32(this.keys[2], (byte) (this.keys[1] >> 24));
	}

	private static int crc32(final int currentCrc, final byte b) {
		return ((currentCrc >>> 8) ^ CRC_TABLE[(currentCrc ^ b) & 0xFF]);
	}

	/**
	 * <h3 class="en-US">Initialize CRC table</h3>
	 * <h3 class="zh-CN">初始化 CRC 表</h3>
	 *
	 * @return <span class="en-US">CRC table</span>
	 * <span class="zh-CN">CRC 表</span>
	 */
	private static int[] CRC_TABLE() {
		int[] crcTable = new int[256];
		for (int i = 0; i < 256; i++) {
			int r = i;
			for (int j = 0; j < 8; j++) {
				if ((r & 1) == 1) {
					r = (r >>> 1) ^ 0xedb88320;
				} else {
					r >>>= 1;
				}
			}
			crcTable[i] = r;
		}
		return crcTable;
	}

	@Override
	protected byte[] process(@Nonnull final byte[] buffer, final int offset, final int length) {
		byte[] output = Arrays.copyOfRange(buffer, offset, offset + length);
		switch (this.getMode()) {
			case ENCRYPT:
				this.encrypt(output);
				break;
			case DECRYPT:
				this.decrypt(output);
				break;
			default:
				break;
		}
		return output;
	}

	@Override
	protected byte[] finishEngine() {
		return CryptoEngine.EMPTY_BYTES.clone();
	}
}
