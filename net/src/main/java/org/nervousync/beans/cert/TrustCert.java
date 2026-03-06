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
import org.nervousync.utils.security.SecurityUtils;

/**
 * <h2 class="en-US">Trust Certificate Define</h2>
 * <h2 class="zh-CN">信任的证书定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $Date: Oct 30, 2018 15:38:36 $
 */
public class TrustCert {
	/**
	 * <span class="en-US">Certificate library data bytes</span>
	 * <span class="zh-CN">证书库二进制字节数组</span>
	 */
	private byte[] certContent;
	/**
	 * <span class="en-US">Certificate alias name</span>
	 * <span class="zh-CN">证书别名</span>
	 */
	private String certAlias;
	/**
	 * <span class="en-US">SHA256 value of certificate library data bytes</span>
	 * <span class="zh-CN">证书库二进制字节数组的SHA256值</span>
	 */
	private final String sha256;

	/**
	 * <h3 class="en-US">Private constructor method for TrustCert</h3>
	 * <h3 class="zh-CN">TrustCert私有构造方法</h3>
	 *
	 * @param certContent <span class="en-US">Certificate library data bytes</span>
	 *                    <span class="zh-CN">证书库二进制字节数组</span>
	 * @param certAlias   <span class="en-US">Certificate alias name</span>
	 *                    <span class="zh-CN">证书别名</span>
	 */
	private TrustCert(final byte[] certContent, final String certAlias) {
		this.certContent = certContent;
		this.certAlias = certAlias;
		this.sha256 = SecurityUtils.SHA256(certContent, EncodeType.HEX);
	}

	/**
	 * <h3 class="en-US">Static method for generate TrustCert instance</h3>
	 * <h3 class="zh-CN">TrustCert私有构造方法</h3>
	 *
	 * @param certContent <span class="en-US">Certificate library data bytes</span>
	 *                    <span class="zh-CN">证书库二进制字节数组</span>
	 * @param certAlias   <span class="en-US">Certificate alias name</span>
	 *                    <span class="zh-CN">证书别名</span>
	 * @return <span class="en-US">Generated TrustCert instance</span>
	 * <span class="zh-CN">生成的TrustCert实例对象</span>
	 */
	public static TrustCert newInstance(final byte[] certContent, final String certAlias) {
		return new TrustCert(certContent, certAlias);
	}

	/**
	 * <h3 class="en-US">Getter method for certificate library data bytes</h3>
	 * <h3 class="zh-CN">证书库二进制字节数组的Getter方法</h3>
	 *
	 * @return <span class="en-US">Certificate library data bytes</span>
	 * <span class="zh-CN">证书库二进制字节数组</span>
	 */
	public byte[] getCertContent() {
		return this.certContent;
	}

	/**
	 * <h3 class="en-US">Setter method for certificate library data bytes</h3>
	 * <h3 class="zh-CN">证书库二进制字节数组的Setter方法</h3>
	 *
	 * @param certContent <span class="en-US">Certificate library data bytes</span>
	 *                    <span class="zh-CN">证书库二进制字节数组</span>
	 */
	public void setCertContent(final byte[] certContent) {
		this.certContent = certContent;
	}

	/**
	 * <h3 class="en-US">Getter method for the certificate alias name</h3>
	 * <h3 class="zh-CN">证书别名的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Certificate alias name</span>
	 * <span class="zh-CN">证书别名</span>
	 */
	public String getCertAlias() {
		return this.certAlias;
	}

	/**
	 * <h3 class="en-US">Setter method for the certificate alias name</h3>
	 * <h3 class="zh-CN">证书别名的 Setter 方法</h3>
	 *
	 * @param certAlias <span class="en-US">Certificate alias name</span>
	 *                  <span class="zh-CN">证书别名</span>
	 */
	public void setCertAlias(final String certAlias) {
		this.certAlias = certAlias;
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
}
