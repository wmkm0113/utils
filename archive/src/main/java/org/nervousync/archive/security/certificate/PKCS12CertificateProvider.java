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

import org.nervousync.archive.exceptions.ArchiveException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * <h2 class="en-US">PKCS12 certificate provider</h2>
 * <h2 class="zh-CN">PKCS12 证书提供适配器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class PKCS12CertificateProvider implements CertificateProvider {

	/**
	 * <span class="en-US">Certificate provider list</span>
	 * <span class="zh-CN">证书提供适配器列表</span>
	 */
	private final List<CertificateProvider> providers;

	/**
	 * <h3 class="en-US">Private constructor method for the PKCS12 certificate provider</h3>
	 * <h3 class="zh-CN">PKCS12 证书提供适配器的私有构造方法</h3>
	 *
	 * @param providers <span class="en-US">Certificate provider list</span>
	 *                  <span class="zh-CN">证书提供适配器列表</span>
	 */
	private PKCS12CertificateProvider(final List<CertificateProvider> providers) {
		this.providers = List.copyOf(providers);
	}

	public static PKCS12CertificateProvider load(final Path path, final char[] storePassword, final char[] keyPassword)
			throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		Objects.requireNonNull(path, "path");
		char[] storeSecret = Objects.requireNonNull(storePassword, "storePassword").clone();
		char[] keySecret = Objects.requireNonNull(keyPassword, "keyPassword").clone();
		try {
			KeyStore store = KeyStore.getInstance("PKCS12");
			try (InputStream input = Files.newInputStream(path)) {
				store.load(input, storeSecret);
			}
			List<CertificateProvider> providers = new ArrayList<>();
			Enumeration<String> aliases = store.aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				if (!store.isKeyEntry(alias)) {
					continue;
				}
				Certificate certificate = store.getCertificate(alias);
				if (!(certificate instanceof X509Certificate)
						|| !"RSA".equalsIgnoreCase(certificate.getPublicKey().getAlgorithm())) {
					continue;
				}
				Key key = store.getKey(alias, keySecret);
				if (key instanceof PrivateKey) {
					providers.add(CertificateProvider.fixed(
							new CertificateCredentials((X509Certificate) certificate, (PrivateKey) key, null)));
				}
			}
			if (providers.isEmpty()) {
				throw new ArchiveException(0x0000001A004CL);
			}
			return new PKCS12CertificateProvider(providers);
		} finally {
			Arrays.fill(storeSecret, '\0');
			Arrays.fill(keySecret, '\0');
		}
	}

	@Override
	public Optional<CertificateCredentials> credentials(final CertificateRequest request) throws IOException {
		for (CertificateProvider provider : this.providers) {
			Optional<CertificateCredentials> result = provider.credentials(request);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}
}
