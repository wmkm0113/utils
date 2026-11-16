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

package org.nervousync.archive.security.certificate;

import org.nervousync.archive.security.SecurityRequest;

import java.util.Objects;

/**
 * <h2 class="en-US">Certificate request</h2>
 * <h2 class="zh-CN">证书请求</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class CertificateRequest extends SecurityRequest {

	/**
	 * <span class="en-US">Central directory flag</span>
	 * <span class="zh-CN">中央目录标记</span>
	 */
	private final boolean centralDirectory;
	/**
	 * <span class="en-US">Hash algorithm</span>
	 * <span class="zh-CN">哈希算法</span>
	 */
	private final int hashAlgorithm;
	/**
	 * <span class="en-US">Public key hash</span>
	 * <span class="zh-CN">公钥哈希</span>
	 */
	private final byte[] publicKeyHash;

	/**
	 * <h3 class="en-US">Constructor method for the certificate request</h3>
	 * <h3 class="zh-CN">证书请求的构造方法</h3>
	 *
	 * @param archivePath      <span class="en-US">Archive file path</span>
	 *                         <span class="zh-CN">归档文件地址</span>
	 * @param entryPath        <span class="en-US">Entry path</span>
	 *                         <span class="zh-CN">条目地址</span>
	 * @param centralDirectory <span class="en-US">Central directory flag</span>
	 *                         <span class="zh-CN">中央目录标记</span>
	 * @param hashAlgorithm    <span class="en-US">Hash algorithm</span>
	 *                         <span class="zh-CN">哈希算法</span>
	 * @param publicKeyHash    <span class="en-US">Public key hash</span>
	 *                         <span class="zh-CN">公钥哈希</span>
	 */
	public CertificateRequest(final String archivePath, final String entryPath, final boolean centralDirectory,
	                          final int hashAlgorithm, final byte[] publicKeyHash) {
		super(archivePath, entryPath);
		this.centralDirectory = centralDirectory;
		this.hashAlgorithm = hashAlgorithm;
		this.publicKeyHash = Objects.requireNonNull(publicKeyHash, "publicKeyHash").clone();
	}

	/**
	 * <h3 class="en-US">Getter method for the central directory flag</h3>
	 * <h3 class="zh-CN">中央目录标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Central directory flag</span>
	 * <span class="zh-CN">中央目录标记</span>
	 */
	public boolean isCentralDirectory() {
		return this.centralDirectory;
	}

	/**
	 * <h3 class="en-US">Getter method for the hash algorithm</h3>
	 * <h3 class="zh-CN">哈希算法的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Hash algorithm</span>
	 * <span class="zh-CN">哈希算法</span>
	 */
	public int getHashAlgorithm() {
		return this.hashAlgorithm;
	}

	/**
	 * <h3 class="en-US">Getter method for the public key hash</h3>
	 * <h3 class="zh-CN">公钥哈希的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Public key hash</span>
	 * <span class="zh-CN">公钥哈希</span>
	 */
	public byte[] getPublicKeyHash() {
		return this.publicKeyHash;
	}
}
