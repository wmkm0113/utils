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

/**
 * <h2 class="en-US">Strong encryption information</h2>
 * <h2 class="zh-CN">强加密信息</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class StrongEncryptionInfo extends EncryptionInfo {

	/**
	 * <span class="en-US">Encryption algorithm code</span>
	 * <span class="zh-CN">加密算法代码</span>
	 */
	private final int algorithm;
	/**
	 * <span class="en-US">Encryption key bit length</span>
	 * <span class="zh-CN">加密密钥位数</span>
	 */
	private final int keyBitLength;
	/**
	 * <span class="en-US">Credentials and procedures required for decryption</span>
	 * <span class="zh-CN">解密所需凭据和处理方式</span>
	 */
	private final int flags;
	/**
	 * <span class="en-US">Certificate data</span>
	 * <span class="zh-CN">证书数据</span>
	 */
	private final byte[] certificateData;

	/**
	 * <h3 class="en-US">Constructor method for the strong encryption information</h3>
	 * <h3 class="zh-CN">强加密信息的构造方法</h3>
	 *
	 * @param algorithm       <span class="en-US">Encryption algorithm code</span>
	 *                        <span class="zh-CN">加密算法代码</span>
	 * @param keyBitLength    <span class="en-US">Encryption key bit length</span>
	 *                        <span class="zh-CN">加密密钥位数</span>
	 * @param flags           <span class="en-US">Credentials and procedures required for decryption</span>
	 *                        <span class="zh-CN">解密所需凭据和处理方式</span>
	 * @param certificateData <span class="en-US">Certificate data</span>
	 *                        <span class="zh-CN">证书数据</span>
	 */
	public StrongEncryptionInfo(final int algorithm, final int keyBitLength, final int flags, final byte[] certificateData) {
		this.algorithm = algorithm;
		this.keyBitLength = keyBitLength;
		this.flags = flags;
		this.certificateData = certificateData.clone();
	}

	/**
	 * <h3 class="en-US">Getter method for the encryption algorithm code</h3>
	 * <h3 class="zh-CN">加密算法代码的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Encryption algorithm code</span>
	 * <span class="zh-CN">加密算法代码</span>
	 */
	public int getAlgorithm() {
		return this.algorithm;
	}

	/**
	 * <h3 class="en-US">Getter method for the encryption key bit length</h3>
	 * <h3 class="zh-CN">加密密钥位数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Encryption key bit length</span>
	 * <span class="zh-CN">加密密钥位数</span>
	 */
	public int getKeyBitLength() {
		return this.keyBitLength;
	}

	/**
	 * <h3 class="en-US">Getter method for the credentials and procedures required for decryption</h3>
	 * <h3 class="zh-CN">解密所需凭据和处理方式的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Credentials and procedures required for decryption</span>
	 * <span class="zh-CN">解密所需凭据和处理方式</span>
	 */
	public int getFlags() {
		return this.flags;
	}

	/**
	 * <h3 class="en-US">Getter method for the certificate data</h3>
	 * <h3 class="zh-CN">证书数据的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Certificate data</span>
	 * <span class="zh-CN">证书数据</span>
	 */
	public byte[] getCertificateData() {
		return this.certificateData;
	}

	public byte[] toExtraData() throws IOException {
		final int fixedSize = 8;

		if (this.certificateData.length > 0xFFFF - fixedSize) {
			throw new IOException(
					"Strong encryption extra field exceeds 65535 bytes");
		}

		return ByteBuffer.allocate(fixedSize + this.certificateData.length)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) 2)
				.putShort((short) this.algorithm)
				.putShort((short) this.keyBitLength)
				.putShort((short) this.flags)
				.put(this.certificateData)
				.array();
	}
}
