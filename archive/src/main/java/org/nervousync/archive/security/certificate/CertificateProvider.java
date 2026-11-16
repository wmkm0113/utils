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

import jakarta.annotation.Nonnull;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.exceptions.ArchiveException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Objects;
import java.util.Optional;

/**
 * <h2 class="en-US">Certificate provider</h2>
 * <h2 class="zh-CN">证书提供适配器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
@FunctionalInterface
public interface CertificateProvider {

	/**
	 * <h3 class="en-US">Initialize a certificate credential instance object using a certificate request.</h3>
	 * <h3 class="zh-CN">通过证书请求初始化证书凭证实例对象</h3>
	 *
	 * @param request <span class="en-US">Certificate request</span>
	 *                <span class="zh-CN">证书请求</span>
	 * @return <span class="en-US">Password provider instance object</span>
	 * <span class="zh-CN">密码提供适配器实例对象</span>
	 */
	Optional<CertificateCredentials> credentials(final CertificateRequest request) throws IOException;

	/**
	 * <h3 class="en-US">Static method for initialize the non-certificate provision adapter</h3>
	 * <h3 class="zh-CN">静态方法用于初始化无证书提供适配器</h3>
	 *
	 * @return <span class="en-US">Certificate provider instance object</span>
	 * <span class="zh-CN">证书提供适配器实例对象</span>
	 */
	static CertificateProvider none() {
		return request -> Optional.empty();
	}

	/**
	 * <h3 class="en-US">Static method for initialize the non-interactive certificate adapter</h3>
	 * <h3 class="zh-CN">静态方法用于初始化非交互式的证书提供适配器</h3>
	 *
	 * @param credentials <span class="en-US">Certificate credentials</span>
	 *                    <span class="zh-CN">证书凭证</span>
	 * @return <span class="en-US">Certificate provider instance object</span>
	 * <span class="zh-CN">证书提供适配器实例对象</span>
	 */
	static CertificateProvider fixed(final CertificateCredentials credentials)
			throws IOException, NoSuchAlgorithmException {
		Objects.requireNonNull(credentials, "credentials");
		byte[] identifier = calculate(credentials.getCertificate().getPublicKey());
		return request ->
				request.getHashAlgorithm() == ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA1
						&& MessageDigest.isEqual(identifier, request.getPublicKeyHash())
						? Optional.of(credentials)
						: Optional.empty();
	}

	/**
	 * <h3 class="en-US">Calculate the hash value of the public key</h3>
	 * <h3 class="zh-CN">计算公钥的哈希值</h3>
	 *
	 * @param publicKey <span class="en-US">Public key</span>
	 *                  <span class="zh-CN">公钥</span>
	 * @return <span class="en-US">Hash value</span>
	 * <span class="zh-CN">哈希值</span>
	 * @throws IOException              <span class="en-US">Certificate format invalid</span>
	 *                                  <span class="zh-CN">证书格式错误</span>
	 * @throws NoSuchAlgorithmException <span class="en-US">Not support the hash algorithm</span>
	 *                                  <span class="zh-CN">哈希算法不支持</span>
	 */
	static byte[] calculate(@Nonnull final PublicKey publicKey) throws IOException, NoSuchAlgorithmException {
		Objects.requireNonNull(publicKey, "publicKey");
		byte[] encoded = publicKey.getEncoded();
		if (!"X.509".equalsIgnoreCase(publicKey.getFormat()) || encoded == null) {
			throw new ArchiveException(0x0000001A004DL);
		}
		return MessageDigest.getInstance("SHA-1").digest(encoded);
	}
}
