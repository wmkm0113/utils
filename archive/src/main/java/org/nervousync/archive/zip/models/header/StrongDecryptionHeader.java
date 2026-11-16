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

package org.nervousync.archive.zip.models.header;

import org.nervousync.archive.zip.models.crypto.StrongRecipientTable;

/**
 * <h2 class="en-US">Decryption header information</h2>
 * <h2 class="zh-CN">解密头信息</h2>
 *
 * @param ivData                  <span class="en-US">IV data</span>
 *                                <span class="zh-CN">向量数据</span>
 * @param algorithm               <span class="en-US">Algorithm code</span>
 *                                <span class="zh-CN">算法代码</span>
 * @param keyBitLength            <span class="en-US">Key bit length</span>
 *                                <span class="zh-CN">密钥比特长度</span>
 * @param flags                   <span class="en-US">Credentials and procedures required for decryption</span>
 *                                <span class="zh-CN">解密所需凭据和处理方式</span>
 * @param encryptedRandomData     <span class="en-US">Encrypted random data bytes</span>
 *                                <span class="zh-CN">加密的二进制数据</span>
 * @param encryptedValidationData <span class="en-US">Validation data</span>
 *                                <span class="zh-CN">验证数据</span>
 * @param serializedSize          <span class="en-US"></span>
 *                                <span class="zh-CN"></span>
 * @param recipients              <span class="en-US">Strong recipient mapping table</span>
 *                                <span class="zh-CN">接收者映射表</span>
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public record StrongDecryptionHeader(byte[] ivData, int algorithm, int keyBitLength, int flags,
                                     byte[] encryptedRandomData, byte[] encryptedValidationData, long serializedSize,
                                     StrongRecipientTable recipients) {

	/**
	 * <h3 class="en-US">Constructor method for the decryption header information</h3>
	 * <h3 class="zh-CN">解密头信息的构造方法</h3>
	 *
	 * @param ivData                  <span class="en-US">IV data</span>
	 *                                <span class="zh-CN">向量数据</span>
	 * @param algorithm               <span class="en-US">Algorithm code</span>
	 *                                <span class="zh-CN">算法代码</span>
	 * @param keyBitLength            <span class="en-US">Key bit length</span>
	 *                                <span class="zh-CN">密钥比特长度</span>
	 * @param flags                   <span class="en-US">Credentials and procedures required for decryption</span>
	 *                                <span class="zh-CN">解密所需凭据和处理方式</span>
	 * @param encryptedRandomData     <span class="en-US">Encrypted random data bytes</span>
	 *                                <span class="zh-CN">加密的二进制数据</span>
	 * @param encryptedValidationData <span class="en-US">Validation data</span>
	 *                                <span class="zh-CN">验证数据</span>
	 * @param serializedSize          <span class="en-US"></span>
	 *                                <span class="zh-CN"></span>
	 */
	public StrongDecryptionHeader {
	}

	/**
	 * <h3 class="en-US">Getter method for the IV data</h3>
	 * <h3 class="zh-CN">向量数据的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">IV data</span>
	 * <span class="zh-CN">向量数据</span>
	 */
	@Override
	public byte[] ivData() {
		return this.ivData;
	}

	/**
	 * <h3 class="en-US">Getter method for the encryption algorithm code</h3>
	 * <h3 class="zh-CN">加密算法代码的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Encryption algorithm code</span>
	 * <span class="zh-CN">加密算法代码</span>
	 */
	@Override
	public int algorithm() {
		return this.algorithm;
	}

	/**
	 * <h3 class="en-US">Getter method for the encryption key bit length</h3>
	 * <h3 class="zh-CN">加密密钥位数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Encryption key bit length</span>
	 * <span class="zh-CN">加密密钥位数</span>
	 */
	@Override
	public int keyBitLength() {
		return this.keyBitLength;
	}

	/**
	 * <h3 class="en-US">Getter method for the credentials and procedures required for decryption</h3>
	 * <h3 class="zh-CN">解密所需凭据和处理方式的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Credentials and procedures required for decryption</span>
	 * <span class="zh-CN">解密所需凭据和处理方式</span>
	 */
	@Override
	public int flags() {
		return this.flags;
	}

	/**
	 * <h3 class="en-US">Getter method for the encrypted random data bytes</h3>
	 * <h3 class="zh-CN">加密的二进制数据的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Encrypted random data bytes</span>
	 * <span class="zh-CN">加密的二进制数据</span>
	 */
	@Override
	public byte[] encryptedRandomData() {
		return this.encryptedRandomData;
	}

	/**
	 * <h3 class="en-US">Getter method for the validation data</h3>
	 * <h3 class="zh-CN">验证数据的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Validation data</span>
	 * <span class="zh-CN">验证数据</span>
	 */
	@Override
	public byte[] encryptedValidationData() {
		return this.encryptedValidationData;
	}

	/**
	 * <h3 class="en-US">Getter method for </h3>
	 * <h3 class="zh-CN">的 Getter 方法</h3>
	 *
	 * @return <span class="en-US"></span>
	 * <span class="zh-CN"></span>
	 */
	@Override
	public long serializedSize() {
		return this.serializedSize;
	}

	/**
	 * <h3 class="en-US">Getter method for the strong recipient mapping table</h3>
	 * <h3 class="zh-CN">接收者映射表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Strong recipient mapping table</span>
	 * <span class="zh-CN">接收者映射表</span>
	 */
	@Override
	public StrongRecipientTable recipients() {
		return this.recipients;
	}
}
