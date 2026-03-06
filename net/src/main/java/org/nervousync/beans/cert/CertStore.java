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
package org.nervousync.beans.cert;

import org.nervousync.enumerations.security.EncodeType;
import org.nervousync.exceptions.cert.CertInfoException;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.ObjectUtils;
import org.nervousync.utils.security.SecurityUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.util.Objects;

/**
 * <h2 class="en-US">Trust Certificate Library Defines</h2>
 * <h2 class="zh-CN">信任的证书库定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $Date: Oct 30, 2018 15:38:36 $
 */
@SuppressWarnings("unused")
public final class CertStore {
	/**
	 * <span class="en-US">Certificate library file path</span>
	 * <span class="zh-CN">证书库文件所在位置</span>
	 */
	private String storePath;
	/**
	 * <span class="en-US">Certificate password for read</span>
	 * <span class="zh-CN">读取证书的密码</span>
	 */
	private String storePassword;
	/**
	 * <span class="en-US">SHA256 value of certificate library data bytes</span>
	 * <span class="zh-CN">证书库二进制字节数组的SHA256值</span>
	 */
	private final String sha256;

	/**
	 * <h3 class="en-US">Private constructor method for TrustCert</h3>
	 * <h3 class="zh-CN">TrustCert私有构造方法</h3>
	 *
	 * @param storePath     <span class="en-US">Certificate library file path</span>
	 *                      <span class="zh-CN">证书库文件所在位置</span>
	 * @param storePassword <span class="en-US">Certificate library password for read</span>
	 *                      <span class="zh-CN">读取证书的密码</span>
	 * @throws FileNotFoundException <span class="en-US">Certificate library file not found</span>
	 *                               <span class="zh-CN">证书库文件未找到</span>
	 */
	private CertStore(final String storePath, final String storePassword) throws FileNotFoundException {
		this.storePath = storePath;
		this.storePassword = storePassword;
		this.sha256 = SecurityUtils.SHA256(FileUtils.readFileBytes(storePath), EncodeType.HEX);
	}

	/**
	 * <h3 class="en-US">Static method for generate TrustCert instance</h3>
	 * <h3 class="zh-CN">TrustCert私有构造方法</h3>
	 *
	 * @param storePath    <span class="en-US">Certificate library file path</span>
	 *                     <span class="zh-CN">证书库文件所在位置</span>
	 * @param storePassword <span class="en-US">Certificate library password for read</span>
	 *                      <span class="zh-CN">读取证书的密码</span>
	 * @return <span class="en-US">Generated CertStore instance</span>
	 * <span class="zh-CN">生成的 CertStore 实例对象</span>
	 * @throws FileNotFoundException <span class="en-US">Certificate library file not found</span>
	 *                               <span class="zh-CN">证书库文件未找到</span>
	 */
	public static CertStore newInstance(final String storePath, final String storePassword) throws FileNotFoundException {
		return new CertStore(storePath, storePassword);
	}

	/**
	 * <h3 class="en-US">Read the certificate library and generate a key manager array</h3>
	 * <h3 class="zh-CN">读取证书库中的证书并生成密钥管理器数组</h3>
	 *
	 * @return the key manager [ ]
	 * @throws CertInfoException the certificate exception
	 */
	public KeyManager[] generateKeyManagers() throws CertInfoException {
		try {
			KeyStore clientStore = KeyStore.getInstance("JKS");
			clientStore.load(FileUtils.loadFile(this.storePath), this.storePassword.toCharArray());
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(clientStore, this.storePassword.toCharArray());
			return keyManagerFactory.getKeyManagers();
		} catch (Exception e) {
			throw new CertInfoException(0x000000010001L, e);
		}
	}

	/**
	 * <h3 class="en-US">Getter method for the certificate library file path</h3>
	 * <h3 class="zh-CN">证书库文件所在位置的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Certificate library file path</span>
	 * <span class="zh-CN">证书库文件所在位置</span>
	 */
	public String getStorePath() {
		return this.storePath;
	}

	/**
	 * <h3 class="en-US">Setter method for the certificate library file path</h3>
	 * <h3 class="zh-CN">证书库文件所在位置的 Setter 方法</h3>
	 *
	 * @param storePath <span class="en-US">Certificate library file path</span>
	 *                  <span class="zh-CN">证书库文件所在位置</span>
	 */
	public void setStorePath(final String storePath) {
		this.storePath = storePath;
	}

	/**
	 * <h3 class="en-US">Getter method for certificate password</h3>
	 * <h3 class="zh-CN">证书读取密码的Getter方法</h3>
	 *
	 * @return <span class="en-US">Certificate password for read</span>
	 * <span class="zh-CN">读取证书的密码</span>
	 */
	public String getStorePassword() {
		return this.storePassword;
	}

	/**
	 * <h3 class="en-US">Setter method for certificate password</h3>
	 * <h3 class="zh-CN">证书读取密码的Setter方法</h3>
	 *
	 * @param storePassword <span class="en-US">Certificate password for read</span>
	 *                      <span class="zh-CN">读取证书的密码</span>
	 */
	public void setStorePassword(final String storePassword) {
		this.storePassword = storePassword;
	}

	/**
	 * <h3 class="en-US">Getter method for SHA256 value</h3>
	 * <h3 class="zh-CN">SHA256验证值的Getter方法</h3>
	 *
	 * @return <span class="en-US">SHA256 value of certificate library data bytes</span>
	 * <span class="zh-CN">证书库二进制字节数组的SHA256值</span>
	 */
	public String getSha256() {
		return this.sha256;
	}

	/**
	 * (non-javadoc)
	 *
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CertStore certStore = (CertStore) o;
		return ObjectUtils.nullSafeEquals(this.storePath, certStore.getStorePath())
				&& ObjectUtils.nullSafeEquals(this.storePassword, certStore.getStorePassword())
				&& ObjectUtils.nullSafeEquals(this.sha256, certStore.getSha256());
	}

	/**
	 * (non-javadoc)
	 *
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.storePath, this.storePassword, this.sha256);
	}
}
