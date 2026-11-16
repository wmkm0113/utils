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
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.security.SecurityCredentials;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * <h2 class="en-US">Certificate credentials</h2>
 * <h2 class="zh-CN">证书凭据</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class CertificateCredentials extends SecurityCredentials {

	/**
	 * <span class="en-US">X.509 certificate</span>
	 * <span class="zh-CN">X.509 证书</span>
	 */
	private final X509Certificate certificate;
	/**
	 * <span class="en-US">Certificate private key</span>
	 * <span class="zh-CN">证书私钥</span>
	 */
	private final PrivateKey privateKey;
	/**
	 * <span class="en-US">Java security provider</span>
	 * <span class="zh-CN">Java 安全适配器</span>
	 */
	private final Provider provider;

	/**
	 * <h3 class="en-US">Constructor method for the certificate credentials</h3>
	 * <h3 class="zh-CN">证书凭据的构造方法</h3>
	 *
	 * @param certificate <span class="en-US">X.509 certificate</span>
	 *                    <span class="zh-CN">X.509 证书</span>
	 * @param privateKey  <span class="en-US">Certificate private key</span>
	 *                    <span class="zh-CN">证书私钥</span>
	 * @param provider    <span class="en-US">Java security provider</span>
	 *                    <span class="zh-CN">Java 安全适配器</span>
	 * @throws IOException <span class="en-US">The required RSA certificate was not used.</span>
	 *                     <span class="zh-CN">未使用规定的 RSA 证书</span>
	 */
	public CertificateCredentials(@Nonnull final X509Certificate certificate, @Nonnull final PrivateKey privateKey,
	                              final Provider provider) throws IOException {
		this.certificate = Objects.requireNonNull(certificate, "certificate");
		this.privateKey = Objects.requireNonNull(privateKey, "privateKey");
		if (!"RSA".equalsIgnoreCase(certificate.getPublicKey().getAlgorithm())
				|| !"RSA".equalsIgnoreCase(privateKey.getAlgorithm())) {
			throw new ArchiveException(0x0000001A004CL);
		}
		this.provider = provider;
	}

	/**
	 * <h3 class="en-US">Getter method for the X.509 certificate</h3>
	 * <h3 class="zh-CN">X.509 证书的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">X.509 certificate</span>
	 * <span class="zh-CN">X.509 证书</span>
	 */
	public X509Certificate getCertificate() {
		return this.certificate;
	}

	/**
	 * <h3 class="en-US">Getter method for the certificate private key</h3>
	 * <h3 class="zh-CN">证书私钥的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Certificate private key</span>
	 * <span class="zh-CN">证书私钥</span>
	 */
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}

	/**
	 * <h3 class="en-US">Getter method for the Java security provider</h3>
	 * <h3 class="zh-CN">Java 安全适配器的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Java security provider</span>
	 * <span class="zh-CN">Java 安全适配器</span>
	 */
	public Provider getProvider() {
		return this.provider;
	}
}
