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
package org.nervousync.beans.security;

import org.nervousync.beans.cert.CertStore;
import org.nervousync.beans.cert.TrustCert;
import org.nervousync.exceptions.cert.CertInfoException;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.core.SystemUtils;
import org.nervousync.utils.logger.LoggerUtils;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * <h2 class="en-US">X509 trust manager</h2>
 * <h2 class="zh-CN">X509证书管理器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Dec 18, 2020 20:51：28 $
 */
public final class GeneX509TrustManager implements X509TrustManager {
	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private final LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());
	/**
	 * <span class="en-US">Default password of read certificate from library</span>
	 * <span class="zh-CN">读取证书的默认密码</span>
	 */
	private static final String DEFAULT_PASSPHRASE = "changeit";
	/**
	 * <span class="en-US">Password of read certificate from library</span>
	 * <span class="zh-CN">读取证书的密码</span>
	 */
	private final String passPhrase;
	/**
	 * <span class="en-US">Trust certificate library list</span>
	 * <span class="zh-CN">信任证书库列表</span>
	 */
	private final List<CertStore> certStoreList;
	/**
	 * <span class="en-US">Trust certificate list</span>
	 * <span class="zh-CN">信任证书列表</span>
	 */
	private final List<TrustCert> trustCertList;
	/**
	 * <span class="en-US">Trust manager instance</span>
	 * <span class="zh-CN">信任管理器实例对象</span>
	 */
	private X509TrustManager trustManager = null;

	/**
	 * <h3 class="en-US">Private constructor method for GeneX509TrustManager</h3>
	 * <h3 class="zh-CN">GeneX509TrustManager私有构造方法</h3>
	 *
	 * @param passPhrase    <span class="en-US">Password of read certificate from library</span>
	 *                      <span class="zh-CN">读取证书的密码</span>
	 * @param certStoreList <span class="en-US">Trust certificate library list</span>
	 *                      <span class="zh-CN">信任证书库列表</span>
	 * @throws CertInfoException <span class="en-US">If not found X509TrustManager instance</span>
	 *                           <span class="zh-CN">当没有找到X509TrustManager实例对象时</span>
	 */
	private GeneX509TrustManager(final String passPhrase, final List<CertStore> certStoreList,
	                             final List<TrustCert> trustCertList) throws CertInfoException {
		this.passPhrase = StringUtils.notBlank(passPhrase) ? passPhrase : DEFAULT_PASSPHRASE;
		this.certStoreList = certStoreList;
		this.trustCertList = trustCertList;
		this.initManager();
	}

	/**
	 * <h3 class="en-US">Static method for generate GeneX509TrustManager instance</h3>
	 * <h3 class="zh-CN">静态方法用于生成GeneX509TrustManager实例对象</h3>
	 * Init gene x 509 trust manager.
	 *
	 * @param passPhrase    <span class="en-US">Password of read certificate from library</span>
	 *                      <span class="zh-CN">读取证书的密码</span>
	 * @param certStoreList <span class="en-US">Trust certificate library list</span>
	 *                      <span class="zh-CN">信任证书库列表</span>
	 * @return <span class="en-US">Generated GeneX509TrustManager instance</span>
	 * <span class="zh-CN">生成的GeneX509TrustManager实例对象</span>
	 * @throws CertInfoException <span class="en-US">If not found X509TrustManager instance</span>
	 *                           <span class="zh-CN">当没有找到X509TrustManager实例对象时</span>
	 */
	public static GeneX509TrustManager newInstance(final String passPhrase, final List<CertStore> certStoreList,
	                                               final List<TrustCert> trustCertList) throws CertInfoException {
		return new GeneX509TrustManager(passPhrase, certStoreList, trustCertList);
	}

	/**
	 * <h3 class="en-US">Check the client certificate is trusted</h3>
	 * <h3 class="zh-CN">检查客户端证书信任状态</h3>
	 *
	 * @param x509certificates <span class="en-US">the peer certificate chain</span>
	 *                         <span class="zh-CN">对等证书链</span>
	 * @param authType         <span class="en-US">the authentication type based on the client certificate</span>
	 *                         <span class="zh-CN">基于客户端证书的身份验证类型</span>
	 * @throws CertificateException <span class="en-US">If error occurs when check certificate</span>
	 *                              <span class="zh-CN">当检查证书时出现异常</span>
	 */
	@Override
	public void checkClientTrusted(final X509Certificate[] x509certificates, final String authType)
			throws CertificateException {
		this.trustManager.checkClientTrusted(x509certificates, authType);
	}

	/**
	 * <h3 class="en-US">Check the server certificate is trusted</h3>
	 * <h3 class="zh-CN">检查客户端证书信任状态</h3>
	 *
	 * @param x509certificates <span class="en-US">the peer certificate chain</span>
	 *                         <span class="zh-CN">对等证书链</span>
	 * @param authType         <span class="en-US">the authentication type based on the client certificate</span>
	 *                         <span class="zh-CN">基于客户端证书的身份验证类型</span>
	 * @throws CertificateException <span class="en-US">If error occurs when check certificate</span>
	 *                              <span class="zh-CN">当检查证书时出现异常</span>
	 */
	@Override
	public void checkServerTrusted(final X509Certificate[] x509certificates, final String authType) throws CertificateException {
		this.trustManager.checkServerTrusted(x509certificates, authType);
	}

	/**
	 * <h3 class="en-US">Retrieve the accepted issuers certificate array</h3>
	 * <h3 class="zh-CN">读取信任签发者的证书数组</h3>
	 *
	 * @return <span class="en-US">Return an array of certificate authority certificates which are trusted for authenticating peers.</span>
	 * <span class="zh-CN">返回一组受信任的证书颁发机构证书，可用于对对等方进行身份验证。</span>
	 */
	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return this.trustManager.getAcceptedIssuers();
	}

	/**
	 * <h3 class="en-US">Initialize TrustManager instance</h3>
	 * <h3 class="zh-CN">初始化证书信任管理器实例对象</h3>
	 *
	 * @throws CertInfoException <span class="en-US">If not found X509TrustManager instance</span>
	 *                           <span class="zh-CN">当没有找到X509TrustManager实例对象时</span>
	 */
	private void initManager() throws CertInfoException {
		try {
			KeyStore keyStore = KeyStore.getInstance("JKS");
			if (!FileUtils.isExists(SystemUtils.systemCertPath())) {
				this.logger.warn("System_Certificate_Not_Found_Warn");
				//  Load empty keystore
				keyStore.load(null, new char[0]);
			} else {
				keyStore.load(FileUtils.loadFile(SystemUtils.systemCertPath()), this.passPhrase.toCharArray());
			}
			for (CertStore certStore : this.certStoreList) {
				for (Map.Entry<String, Certificate> entry : this.readCertificates(certStore).entrySet()) {
					keyStore.setCertificateEntry(entry.getKey(), entry.getValue());
				}
			}
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			for (TrustCert trustCert : this.trustCertList) {
				Certificate certificate =
						certificateFactory.generateCertificate(new ByteArrayInputStream(trustCert.getCertContent()));
				if (certificate != null) {
					keyStore.setCertificateEntry(trustCert.getCertAlias(), certificate);
				}
			}
			TrustManagerFactory trustManagerFactory =
					TrustManagerFactory.getInstance("SunX509", "SunJSSE");
			trustManagerFactory.init(keyStore);
			for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
				if (trustManager instanceof X509TrustManager) {
					this.trustManager = (X509TrustManager) trustManager;
					return;
				}
			}
		} catch (Exception e) {
			if (e instanceof CertInfoException) {
				throw (CertInfoException) e;
			}
			throw new CertInfoException(0x000000160001L, e);
		}

		throw new CertInfoException(0x000000160002L);
	}

	/**
	 * <h3 class="en-US">Read all certificate and alias mapping tables in the trusted certificate store.</h3>
	 * <h3 class="zh-CN">读取信任证书库中的所有证书和别名映射表</h3>
	 *
	 * @param certStore <span class="en-US">Certificate store instance object</span>
	 *                  <span class="zh-CN">证书库信息</span>
	 * @return <span class="en-US">Mapping table between certificate aliases and certificate instance objects</span>
	 * <span class="zh-CN">证书别名和证书实例对象的映射表</span>
	 * @throws CertInfoException <span class="en-US">If not found X509TrustManager instance</span>
	 *                           <span class="zh-CN">当没有找到X509TrustManager实例对象时</span>
	 */
	private Hashtable<String, Certificate> readCertificates(final CertStore certStore) throws CertInfoException {
		Hashtable<String, Certificate> certificates = new Hashtable<>();
		try {
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(FileUtils.loadFile(certStore.getStorePath()), this.passPhrase.toCharArray());
			Enumeration<String> enumeration = keyStore.aliases();
			while (enumeration.hasMoreElements()) {
				String alias = enumeration.nextElement();
				Optional.ofNullable(keyStore.getCertificate(alias))
						.ifPresent(certificate -> certificates.put(alias, certificate));
			}
		} catch (Exception e) {
			throw new CertInfoException(0x000000160001L, e);
		}
		return certificates;
	}
}
