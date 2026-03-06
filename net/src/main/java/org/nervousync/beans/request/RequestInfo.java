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
package org.nervousync.beans.request;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.Proxy;
import java.util.*;

import org.nervousync.beans.cert.CertStore;
import org.nervousync.beans.cert.TrustCert;
import org.nervousync.beans.cookie.CookieEntity;
import org.nervousync.beans.header.SimpleHeader;
import org.nervousync.builder.AbstractBuilder;
import org.nervousync.builder.ParentBuilder;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.web.HttpMethodOption;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.ObjectUtils;
import org.nervousync.utils.core.StringUtils;

/**
 * <h2 class="en-US">Request information define</h2>
 * <p class="en-US">Using for parameter of method: org.nervousync.utils.RequestUtils#sendRequest</p>
 * <h2 class="zh-CN">网络请求信息定义</h2>
 * <p class="en-US">用于方法org.nervousync.utils.RequestUtils#sendRequest的参数值</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.4 $ $Date: Sep 25, 2022 11:04:17 $
 */
@SuppressWarnings("unused")
public final class RequestInfo {
	/**
	 * <span class="en-US">Enumeration value of HttpMethodOption</span>
	 * <span class="zh-CN">HttpMethodOption的枚举值</span>
	 *
	 * @see HttpMethodOption
	 */
	private final HttpMethodOption methodOption;
	/**
	 * <span class="en-US">Proxy server config for sending request</span>
	 * <p class="en-US">Default is null for direct connect</p>
	 * <span class="zh-CN">发送请求时使用的代理服务器设置</span>
	 * <p class="zh-CN">默认为null代表不使用代理服务器</p>
	 *
	 * @see ProxyConfig
	 */
	private final ProxyConfig proxyConfig;
	/**
	 * <span class="en-US">Trusted certificate list for sending secure request</span>
	 * <p class="en-US">Default is empty list for using JDK certificate library</p>
	 * <span class="zh-CN">发送加密请求时信任的证书列表</span>
	 * <p class="en-US">默认为空列表，代表使用JDK默认的证书库</p>
	 *
	 * @see CertStore
	 */
	private final List<CertStore> trustCertStores;
	/**
	 * <span class="en-US">Trust certificate list</span>
	 * <span class="zh-CN">信任证书列表</span>
	 */
	private final List<TrustCert> trustCertList;
	/**
	 * <span class="en-US">Pass phrase for system certificate library</span>
	 * <span class="zh-CN">系统信任证书库读取密钥</span>
	 */
	private final String passPhrase;
	/**
	 * <span class="en-US">Using for setting user agent string of request header</span>
	 * <span class="zh-CN">用于设置请求头中的用户代理信息</span>
	 */
	private final String userAgent;
	/**
	 * <span class="en-US">Current request url path</span>
	 * <span class="zh-CN">当前请求地址</span>
	 */
	private final String requestUrl;
	/**
	 * <span class="en-US">Character encoding for http request header "Content-Type" and send request body</span>
	 * <span class="zh-CN">请求头"Content-Type"及发送请求体使用的编码集</span>
	 */
	private final String charset;
	/**
	 * <span class="en-US">String value for http request header "Content-Type"</span>
	 * <span class="zh-CN">请求头"Content-Type"的字符串值</span>
	 */
	private final String contentType;
	/**
	 * <span class="en-US">Connect timeout setting</span>
	 * <span class="zh-CN">连接超时时间</span>
	 */
	private final int connectTimeOut;
	/**
	 * <span class="en-US">Request timeout setting</span>
	 * <span class="zh-CN">请求超时时间</span>
	 */
	private final int requestTimeOut;
	/**
	 * <span class="en-US">Binary data array of current request will post</span>
	 * <span class="zh-CN">当前请求要发送的二进制数据数组</span>
	 */
	private final byte[] postData;
	/**
	 * <span class="en-US">Request header information list</span>
	 * <span class="zh-CN">发送请求的请求头信息列表</span>
	 */
	private final List<SimpleHeader> headers;
	/**
	 * <span class="en-US">Request parameters information mapping</span>
	 * <span class="zh-CN">发送请求的参数信息映射</span>
	 */
	private final Map<String, String[]> parameters;
	/**
	 * <span class="en-US">Upload files of request parameters mapping</span>
	 * <span class="zh-CN">发送请求的上传文件参数信息映射</span>
	 */
	private final Map<String, File> uploadParams;
	/**
	 * <span class="en-US">Request cookies information list</span>
	 * <span class="zh-CN">发送请求的Cookie信息列表</span>
	 */
	private final List<CookieEntity> cookieList;

	/**
	 * <h3 class="en-US">Constructor for RequestInfo</h3>
	 * <p class="en-US">Only using for RequestBuilder instance to generate RequestInfo instance</p>
	 * <h3 class="zh-CN">RequestInfo的构造方法</h3>
	 * <p class="zh-CN">仅用于请求构造器生成RequestInfo实例对象使用</p>
	 *
	 * @param methodOption   <span class="en-US">Enumeration value of HttpMethodOption</span>
	 *                       <span class="zh-CN">HttpMethodOption的枚举值</span>
	 * @param requestUrl     <span class="en-US">Current request url path</span>
	 *                       <span class="zh-CN">当前请求地址</span>
	 * @param charset        <span class="en-US">Character encoding for http request header "Content-Type" and send request body</span>
	 *                       <span class="zh-CN">请求头"Content-Type"及发送请求体使用的编码集</span>
	 * @param connectTimeOut <span class="en-US">Connect timeout setting</span>
	 *                       <span class="zh-CN">连接超时时间</span>
	 * @param requestTimeOut <span class="en-US">Request timeout setting</span>
	 *                       <span class="zh-CN">请求超时时间</span>
	 * @param headers        <span class="en-US">Request header information list</span>
	 *                       <span class="zh-CN">发送请求的请求头信息列表</span>
	 * @param parameters     <span class="en-US">Request parameters information mapping</span>
	 *                       <span class="zh-CN">发送请求的参数信息映射</span>
	 * @param uploadParams   <span class="en-US">Upload files of request parameters mapping</span>
	 *                       <span class="zh-CN">发送请求的上传文件参数信息映射</span>
	 * @param cookieList     <span class="en-US">Request cookies information list</span>
	 *                       <span class="zh-CN">发送请求的Cookie信息列表</span>
	 */
	private RequestInfo(final HttpMethodOption methodOption, final ProxyConfig proxyConfig,
	                    final List<CertStore> trustCertStores, final List<TrustCert> trustCertList,
	                    final String passPhrase, final String userAgent,
	                    final String requestUrl, final String charset, final String contentType,
	                    final int connectTimeOut, final int requestTimeOut, final byte[] postData,
	                    final List<SimpleHeader> headers, final Map<String, String[]> parameters,
	                    final Map<String, File> uploadParams, final List<CookieEntity> cookieList) {
		this.methodOption = methodOption;
		this.proxyConfig = proxyConfig;
		this.trustCertStores = trustCertStores;
		this.trustCertList = trustCertList;
		this.passPhrase = passPhrase;
		this.userAgent = userAgent;
		this.requestUrl = requestUrl;
		this.charset = charset;
		this.contentType = contentType;
		this.connectTimeOut = connectTimeOut;
		this.requestTimeOut = requestTimeOut;
		this.postData = postData;
		this.headers = headers;
		this.parameters = parameters;
		this.uploadParams = uploadParams;
		this.cookieList = cookieList;
	}

	/**
	 * Builder request builder.
	 *
	 * @param httpMethodOption the http method option
	 * @return the request builder
	 */
	public static RequestBuilder builder(final HttpMethodOption httpMethodOption) {
		return new RequestBuilder(httpMethodOption);
	}

	/**
	 * <h3 class="en-US">Getter method for method option</h3>
	 * <h3 class="zh-CN">请求类型的Getter方法</h3>
	 */
	public HttpMethodOption getMethodOption() {
		return this.methodOption;
	}

	/**
	 * <h3 class="en-US">Getter method for upload parameters</h3>
	 * <h3 class="zh-CN">上传文件信息映射的Getter方法</h3>
	 */
	public Map<String, File> getUploadParams() {
		return this.uploadParams;
	}

	/**
	 * <h3 class="en-US">Getter method for the cookie list</h3>
	 * <h3 class="zh-CN">请求发送的Cookie信息列表的Getter方法</h3>
	 */
	public List<CookieEntity> getCookieList() {
		return this.cookieList;
	}

	/**
	 * <h3 class="en-US">Getter method for proxy config</h3>
	 * <h3 class="zh-CN">代理服务器设置的Getter方法</h3>
	 */
	public ProxyConfig getProxyConfig() {
		return this.proxyConfig;
	}

	/**
	 * <h3 class="en-US">Getter method for the trusted certificate list</h3>
	 * <h3 class="zh-CN">信任证书列表的Getter方法</h3>
	 */
	public List<CertStore> getTrustCertStores() {
		return this.trustCertStores;
	}

	/**
	 * <h3 class="en-US">Getter method for the trust certificate list</h3>
	 * <h3 class="zh-CN">信任证书列表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Trust certificate list</span>
	 * <span class="zh-CN">信任证书列表</span>
	 */
	public List<TrustCert> getTrustCertList() {
		return this.trustCertList;
	}

	/**
	 * <h3 class="en-US">Getter method for pass phrase of the system certificate list</h3>
	 * <h3 class="zh-CN">系统信任证书库读取密钥的Getter方法</h3>
	 */
	public String getPassPhrase() {
		return this.passPhrase;
	}

	/**
	 * <h3 class="en-US">Getter method for user agent string</h3>
	 * <h3 class="zh-CN">用户代理字符串的Getter方法</h3>
	 */
	public String getUserAgent() {
		return this.userAgent;
	}

	/**
	 * <h3 class="en-US">Getter method for request url</h3>
	 * <h3 class="zh-CN">请求地址的Getter方法</h3>
	 */
	public String getRequestUrl() {
		return this.requestUrl;
	}

	/**
	 * <h3 class="en-US">Getter method for character encoding</h3>
	 * <h3 class="zh-CN">数据编码集的Getter方法</h3>
	 */
	public String getCharset() {
		return this.charset;
	}

	/**
	 * <h3 class="en-US">Getter method for content type string</h3>
	 * <h3 class="zh-CN">请求头"Content-Type"字符串的Getter方法</h3>
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * <h3 class="en-US">Getter method for the connecting time out</h3>
	 * <h3 class="zh-CN">连接超时时间的Getter方法</h3>
	 */
	public int getConnectTimeOut() {
		return this.connectTimeOut;
	}

	/**
	 * <h3 class="en-US">Getter method for request time out</h3>
	 * <h3 class="zh-CN">请求超时时间的Getter方法</h3>
	 */
	public int getRequestTimeOut() {
		return this.requestTimeOut;
	}

	/**
	 * <h3 class="en-US">Getter method for the post binary data array</h3>
	 * <h3 class="zh-CN">POST发送二进制数据的Getter方法</h3>
	 */
	public byte[] getPostData() {
		return this.postData;
	}

	/**
	 * <h3 class="en-US">Getter method for request header list</h3>
	 * <h3 class="zh-CN">请求头信息列表的Getter方法</h3>
	 */
	public List<SimpleHeader> getHeaders() {
		return this.headers;
	}

	/**
	 * <h3 class="en-US">Getter method for parameters mapping</h3>
	 * <h3 class="zh-CN">请求参数信息映射的Getter方法</h3>
	 */
	public Map<String, String[]> getParameters() {
		return this.parameters;
	}

	/**
	 * <h3 class="en-US">Getter method for upload parameters mapping</h3>
	 * <h3 class="zh-CN">上传文件信息映射的Getter方法</h3>
	 */
	public Map<String, File> getUploadParam() {
		return this.uploadParams;
	}

	/**
	 * <h3 class="en-US">Check the acceptance mime-type is application/octet-stream</h3>
	 * <h3 class="zh-CN">检查当前请求是否为字节流</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean octetStreamResponse() {
		return this.headers.stream().anyMatch(simpleHeader ->
				"Accept".equalsIgnoreCase(simpleHeader.getHeaderName())
						&& "application/octet-stream".equalsIgnoreCase(simpleHeader.getHeaderValue()));
	}

	/**
	 * <h3 class="en-US">Check the acceptance mime-type is text/event-stream</h3>
	 * <h3 class="zh-CN">检查当前请求是否为事件流</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean eventStreamResponse() {
		return this.headers.stream().anyMatch(simpleHeader ->
				"Accept".equalsIgnoreCase(simpleHeader.getHeaderName())
						&& "text/event-stream".equalsIgnoreCase(simpleHeader.getHeaderValue()));
	}

	/**
	 * <h2 class="en-US">Request builder</h2>
	 * <h2 class="zh-CN">网络请求构建器</h2>
	 */
	public static final class RequestBuilder extends ParentBuilder {
		/**
		 * <span class="en-US">Enumeration value of HttpMethodOption</span>
		 * <span class="zh-CN">HttpMethodOption的枚举值</span>
		 *
		 * @see HttpMethodOption
		 */
		private final HttpMethodOption methodOption;
		/**
		 * <span class="en-US">Proxy server config for sending request</span>
		 * <p class="en-US">Default is null for direct connect</p>
		 * <span class="zh-CN">发送请求时使用的代理服务器设置</span>
		 * <p class="zh-CN">默认为null代表不使用代理服务器</p>
		 *
		 * @see ProxyConfig
		 */
		private ProxyConfig proxyConfig;
		/**
		 * <span class="en-US">Trusted certificate list for sending secure request</span>
		 * <p class="en-US">Default is empty list for using JDK certificate library</p>
		 * <span class="zh-CN">发送加密请求时信任的证书列表</span>
		 * <p class="en-US">默认为空列表，代表使用JDK默认的证书库</p>
		 *
		 * @see CertStore
		 */
		private final List<CertStore> trustCertStores = new ArrayList<>();
		/**
		 * <span class="en-US">Trust certificate list</span>
		 * <span class="zh-CN">信任证书列表</span>
		 */
		private final List<TrustCert> trustCertList = new ArrayList<>();
		/**
		 * <span class="en-US">Pass phrase for system certificate library</span>
		 * <span class="zh-CN">系统信任证书库读取密钥</span>
		 */
		private String passPhrase;
		/**
		 * <span class="en-US">Using for setting user agent string of request header</span>
		 * <span class="zh-CN">用于设置请求头中的用户代理信息</span>
		 */
		private String userAgent;
		/**
		 * <span class="en-US">Current request url path</span>
		 * <span class="zh-CN">当前请求地址</span>
		 */
		private String requestUrl;
		/**
		 * <span class="en-US">Character encoding for http request header "Content-Type" and send request body</span>
		 * <span class="zh-CN">请求头"Content-Type"及发送请求体使用的编码集</span>
		 */
		private String charset;
		/**
		 * <span class="en-US">String value for http request header "Content-Type"</span>
		 * <span class="zh-CN">请求头"Content-Type"的字符串值</span>
		 */
		private String contentType;
		/**
		 * <span class="en-US">Connect timeout setting</span>
		 * <span class="zh-CN">连接超时时间</span>
		 */
		private int connectTimeOut = Globals.DEFAULT_TIME_OUT;
		/**
		 * <span class="en-US">Request timeout setting</span>
		 * <span class="zh-CN">请求超时时间</span>
		 */
		private int requestTimeOut = Globals.DEFAULT_TIME_OUT;
		/**
		 * <span class="en-US">Binary data array of current request will post</span>
		 * <span class="zh-CN">当前请求要发送的二进制数据数组</span>
		 */
		private byte[] postData;
		/**
		 * <span class="en-US">Request header information list</span>
		 * <span class="zh-CN">发送请求的请求头信息列表</span>
		 */
		private final List<SimpleHeader> headers = new ArrayList<>();
		/**
		 * <span class="en-US">Request parameters information mapping</span>
		 * <span class="zh-CN">发送请求的参数信息映射</span>
		 */
		private final Map<String, String[]> parameters = new HashMap<>();
		/**
		 * <span class="en-US">Upload files of request parameters mapping</span>
		 * <span class="zh-CN">发送请求的上传文件参数信息映射</span>
		 */
		private final Map<String, File> uploadParams = new HashMap<>();
		/**
		 * <span class="en-US">Request cookies information list</span>
		 * <span class="zh-CN">发送请求的Cookie信息列表</span>
		 */
		private final List<CookieEntity> cookieList = new ArrayList<>();

		private RequestBuilder(final HttpMethodOption methodOption) {
			this.methodOption = methodOption;
		}

		/**
		 * <h3 class="en-US">Confirm request info and generate RequestInfo instance</h3>
		 * <h3 class="zh-CN">确认请求配置信息并生成RequestInfo实例对象</h3>
		 *
		 * @return <span class="en-US">RequestInfo instance</span>
		 * <span class="zh-CN">RequestInfo实例对象</span>
		 */
		public RequestInfo build() {
			return new RequestInfo(this.methodOption, this.proxyConfig, this.trustCertStores, this.trustCertList,
					this.passPhrase, this.userAgent, this.requestUrl, this.charset, this.contentType, this.connectTimeOut,
					this.requestTimeOut, this.postData, this.headers, this.parameters, this.uploadParams, this.cookieList);
		}

		/**
		 * <h3 class="en-US">Generate RequestProxyBuilder instance to configure proxy server</h3>
		 * <h3 class="zh-CN">生成RequestProxyBuilder实例对象用于配置代理服务器</h3>
		 *
		 * @return <span class="en-US">RequestProxyBuilder instance</span>
		 * <span class="zh-CN">RequestProxyBuilder实例对象</span>
		 */
		public ProxyConfigBuilder<RequestBuilder> proxyConfig() {
			return new ProxyConfigBuilder<>(this, this.proxyConfig);
		}

		/**
		 * <h3 class="en-US">Add the trusted certificate library</h3>
		 * <h3 class="zh-CN">添加信任证书库</h3>
		 *
		 * @param certPath     <span class="en-US">Trust certificate path</span>
		 *                     <span class="zh-CN">信任证书地址</span>
		 * @param certPassword <span class="en-US">Password of trust certificate</span>
		 *                     <span class="zh-CN">读取证书的密钥</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder addTrustCertificate(final String certPath, final String certPassword) {
			try {
				return this.addTrustCertificate(FileUtils.readFileBytes(certPath), certPassword);
			} catch (IOException ignore) {
			}
			return this;
		}

		/**
		 * <h3 class="en-US">Add the trusted certificate library</h3>
		 * <h3 class="zh-CN">添加信任证书库</h3>
		 *
		 * @param certContent <span class="en-US">Certificate library data bytes</span>
		 *                    <span class="zh-CN">证书库二进制字节数组</span>
		 * @param certAlias   <span class="en-US">Certificate alias name</span>
		 *                    <span class="zh-CN">证书别名</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder addTrustCertificate(final byte[] certContent, final String certAlias) {
			Optional.of(TrustCert.newInstance(certContent, certAlias))
					.filter(certStore ->
							this.trustCertList.stream().noneMatch(existCert ->
									existCert.getSha256().equals(certStore.getSha256())))
					.ifPresent(this.trustCertList::add);
			return this;
		}

		/**
		 * <h3 class="en-US">Add the trusted certificate library</h3>
		 * <h3 class="zh-CN">添加信任证书库</h3>
		 *
		 * @param storePath     <span class="en-US">Certificate library file path</span>
		 *                      <span class="zh-CN">证书库文件所在位置</span>
		 * @param storePassword <span class="en-US">Certificate library password for read</span>
		 *                      <span class="zh-CN">读取证书的密码</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 * @throws FileNotFoundException <span class="en-US">Certificate library file not found</span>
		 *                               <span class="zh-CN">证书库文件未找到</span>
		 */
		public RequestBuilder addTrustStore(final String storePath, final String storePassword) throws FileNotFoundException {
			Optional.of(CertStore.newInstance(storePath, storePassword))
					.filter(certStore ->
							this.trustCertStores.stream().noneMatch(existCert ->
									existCert.getSha256().equals(certStore.getSha256())))
					.ifPresent(this.trustCertStores::add);
			return this;
		}

		/**
		 * <h3 class="en-US">Configure pass phrase of the system certificate library</h3>
		 * <h3 class="zh-CN">设置系统证书库的读取密码</h3>
		 *
		 * @param passPhrase <span class="en-US">Pass phrase of system certificate library</span>
		 *                   <span class="zh-CN">系统证书库的读取密码</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder passPhrase(final String passPhrase) {
			this.passPhrase = passPhrase;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure user agent string will be used</h3>
		 * <h3 class="zh-CN">设置即将使用的用户代理字符串</h3>
		 *
		 * @param userAgent <span class="en-US">User agent string</span>
		 *                  <span class="zh-CN">用户代理字符串</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder userAgent(final String userAgent) {
			this.userAgent = userAgent;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure request url</h3>
		 * <h3 class="zh-CN">设置请求地址</h3>
		 *
		 * @param requestUrl <span class="en-US">Request url string</span>
		 *                   <span class="zh-CN">请求地址字符串</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder requestUrl(final String requestUrl) {
			this.requestUrl = requestUrl;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure character encoding</h3>
		 * <h3 class="zh-CN">设置请求字符集</h3>
		 *
		 * @param charset <span class="en-US">character encoding</span>
		 *                <span class="zh-CN">字符集</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder charset(final String charset) {
			this.charset = charset;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure content type string</h3>
		 * <h3 class="zh-CN">设置"Content-Type"值</h3>
		 *
		 * @param contentType <span class="en-US">Content type string</span>
		 *                    <span class="zh-CN">需要设置的字符串</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder contentType(final String contentType) {
			this.contentType = contentType;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure HTTP headers: Accept</h3>
		 * <h3 class="zh-CN">设置HTTP头的"Accept"值</h3>
		 *
		 * @param acceptType <span class="en-US">Accept type string</span>
		 *                   <span class="zh-CN">接受的MIME类型字符串</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder acceptType(final String acceptType) {
			if (StringUtils.notBlank(acceptType)) {
				return this.addHeader("Accept", acceptType);
			}
			return this;
		}

		/**
		 * <h3 class="en-US">Configure HTTP headers: Accept-Encoding</h3>
		 * <h3 class="zh-CN">设置HTTP头的“Accept-Encoding”值</h3>
		 *
		 * @param acceptEncoding <span class="en-US">Accept encoding</span>
		 *                       <span class="zh-CN">接受的编码类型</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder acceptEncoding(final String acceptEncoding) {
			if (StringUtils.notBlank(acceptEncoding)) {
				return this.addHeader("Accept-Encoding", acceptEncoding);
			}
			return this;
		}

		/**
		 * <h3 class="en-US">Configure connect timeout</h3>
		 * <h3 class="zh-CN">设置连接超时时间</h3>
		 *
		 * @param timeOut <span class="en-US">Timeout value</span>
		 *                <span class="zh-CN">超时时间</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder connectTimeOut(final int timeOut) {
			this.connectTimeOut = timeOut;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure request timeout</h3>
		 * <h3 class="zh-CN">设置请求超时时间</h3>
		 *
		 * @param timeOut <span class="en-US">Timeout value</span>
		 *                <span class="zh-CN">请求超时时间</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder requestTimeOut(final int timeOut) {
			this.requestTimeOut = timeOut;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure request send data bytes</h3>
		 * <h3 class="zh-CN">设置请求发送的二进制数据</h3>
		 *
		 * @param postData <span class="en-US">Binary data bytes</span>
		 *                 <span class="zh-CN">二进制数据</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder postData(final byte[] postData) {
			this.postData = postData;
			return this;
		}

		/**
		 * <h3 class="en-US">Add request header name and value</h3>
		 * <h3 class="zh-CN">添加请求头的键和值</h3>
		 *
		 * @param headerName  <span class="en-US">Request header name</span>
		 *                    <span class="zh-CN">请求头键名</span>
		 * @param headerValue <span class="en-US">Request header value</span>
		 *                    <span class="zh-CN">请求头键值</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder addHeader(final String headerName, final String headerValue) {
			this.headers.add(new SimpleHeader(headerName, headerValue));
			return this;
		}

		/**
		 * <h3 class="en-US">Add request parameter name and value</h3>
		 * <h3 class="zh-CN">添加请求参数的键和值</h3>
		 *
		 * @param parameterName   <span class="en-US">Request parameter name</span>
		 *                        <span class="zh-CN">请求参数名</span>
		 * @param parameterValues <span class="en-US">Request parameter value</span>
		 *                        <span class="zh-CN">请求参数值</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder addParameter(final String parameterName, final String[] parameterValues) {
			this.parameters.put(parameterName, parameterValues);
			return this;
		}

		/**
		 * <h3 class="en-US">Add request upload parameter name and value</h3>
		 * <h3 class="zh-CN">添加请求上传数据的键和值</h3>
		 *
		 * @param parameterName  <span class="en-US">Request upload parameter name</span>
		 *                       <span class="zh-CN">请求上传参数名</span>
		 * @param parameterValue <span class="en-US">Request upload parameter value</span>
		 *                       <span class="zh-CN">请求上传文件实例对象</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder addUploadParam(final String parameterName, final File parameterValue) {
			this.uploadParams.put(parameterName, parameterValue);
			return this;
		}

		/**
		 * <h3 class="en-US">Add request cookie values</h3>
		 * <h3 class="zh-CN">添加请求Cookie信息</h3>
		 *
		 * @param cookieEntities <span class="en-US">CookieEntity instance array</span>
		 *                       <span class="zh-CN">CookieEntity实例对象数组</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 * @see org.nervousync.beans.cookie.CookieEntity
		 */
		public RequestBuilder addCookies(final CookieEntity... cookieEntities) {
			this.cookieList.addAll(Arrays.asList(cookieEntities));
			return this;
		}

		/**
		 * <h3 class="en-US">Add request cookie values from response header "Set-Cookie"</h3>
		 * <h3 class="zh-CN">解析响应数据头中的"Set-Cookie"信息，并添加请求Cookie信息</h3>
		 *
		 * @param responseCookieValue <span class="en-US">String value of response header "Set-Cookie"</span>
		 *                            <span class="zh-CN">响应头中的"Set-Cookie"字符串值</span>
		 * @return <span class="en-US">Current RequestBuilder instance</span>
		 * <span class="zh-CN">当前RequestBuilder实例对象</span>
		 */
		public RequestBuilder addCookies(final String responseCookieValue) {
			this.cookieList.add(new CookieEntity(responseCookieValue));
			return this;
		}

		/**
		 * <h3 class="en-US">Confirm proxy configure</h3>
		 * <h3 class="zh-CN">确认代理服务器配置</h3>
		 *
		 * @param proxyConfig <span class="en-US">ProxyConfig instance</span>
		 *                    <span class="zh-CN">ProxyConfig实例对象</span>
		 * @see ProxyConfig
		 */
		@Override
		public void confirm(final Object proxyConfig) {
			if (proxyConfig instanceof ProxyConfig) {
				this.proxyConfig = (ProxyConfig) proxyConfig;
			}
		}
	}

	/**
	 * <h2 class="en-US">Proxy server configure</h2>
	 * <h2 class="zh-CN">代理服务器配置信息</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 4, 2018 16:05:54 $
	 */
	public static final class ProxyConfig implements Serializable {
		/**
		 * <span class="en-US">Serial version UID</span>
		 * <span class="zh-CN">序列化UID</span>
		 */
		private static final long serialVersionUID = -5386443812775715018L;
		/**
		 * <span class="en-US">Enumeration value of proxy type</span>
		 * <span class="zh-CN">代理服务器类型枚举值</span>
		 */
		private Proxy.Type proxyType = Proxy.Type.DIRECT;
		/**
		 * <span class="en-US">Proxy server address</span>
		 * <span class="zh-CN">代理服务器地址</span>
		 */
		private String proxyAddress = Globals.DEFAULT_VALUE_STRING;
		/**
		 * <span class="en-US">Proxy server port</span>
		 * <span class="zh-CN">代理服务器端口号</span>
		 */
		private int proxyPort = Globals.DEFAULT_VALUE_INT;
		/**
		 * <span class="en-US">Authenticate username</span>
		 * <span class="zh-CN">身份认证用户名</span>
		 */
		private String userName = Globals.DEFAULT_VALUE_STRING;
		/**
		 * <span class="en-US">Authenticate password</span>
		 * <span class="zh-CN">身份认证密码</span>
		 */
		private String password = Globals.DEFAULT_VALUE_STRING;

		/**
		 * <h3 class="en-US">Constructor method for ProxyConfig</h3>
		 * <h3 class="zh-CN">ProxyConfig构造方法</h3>
		 */
		public ProxyConfig() {
		}

		/**
		 * <h3 class="en-US">Static method for create redirect ProxyConfig instance</h3>
		 * <h3 class="zh-CN">静态方法用于创建无代理的代理服务器配置信息实例对象</h3>
		 *
		 * @return <span class="en-US">Generated ProxyConfig instance</span>
		 * <span class="zh-CN">生成的代理服务器配置信息实例对象</span>
		 */
		public static ProxyConfig redirect() {
			return new ProxyConfig();
		}

		/**
		 * <h3 class="en-US">Getter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Getter方法</h3>
		 *
		 * @return <span class="en-US">Enumeration value of proxy type</span>
		 * <span class="zh-CN">代理服务器类型枚举值</span>
		 */
		public Proxy.Type getProxyType() {
			return this.proxyType;
		}

		/**
		 * <h3 class="en-US">Setter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Setter方法</h3>
		 *
		 * @param proxyType <span class="en-US">Enumeration value of proxy type</span>
		 *                  <span class="zh-CN">代理服务器类型枚举值</span>
		 */
		public void setProxyType(final Proxy.Type proxyType) {
			this.proxyType = proxyType;
		}

		/**
		 * <h3 class="en-US">Getter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Getter方法</h3>
		 *
		 * @return <span class="en-US">Proxy server address</span>
		 * <span class="zh-CN">代理服务器地址</span>
		 */
		public String getProxyAddress() {
			return this.proxyAddress;
		}

		/**
		 * <h3 class="en-US">Setter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Setter方法</h3>
		 *
		 * @param proxyAddress <span class="en-US">Proxy server address</span>
		 *                     <span class="zh-CN">代理服务器地址</span>
		 */
		public void setProxyAddress(final String proxyAddress) {
			this.proxyAddress = proxyAddress;
		}

		/**
		 * <h3 class="en-US">Getter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Getter方法</h3>
		 *
		 * @return <span class="en-US">Proxy server port</span>
		 * <span class="zh-CN">代理服务器端口号</span>
		 */
		public int getProxyPort() {
			return this.proxyPort;
		}

		/**
		 * <h3 class="en-US">Setter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Setter方法</h3>
		 *
		 * @param proxyPort <span class="en-US">Proxy server port</span>
		 *                  <span class="zh-CN">代理服务器端口号</span>
		 */
		public void setProxyPort(final int proxyPort) {
			this.proxyPort = proxyPort;
		}

		/**
		 * <h3 class="en-US">Getter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Getter方法</h3>
		 *
		 * @return <span class="en-US">Authenticate username</span>
		 * <span class="zh-CN">身份认证用户名</span>
		 */
		public String getUserName() {
			return this.userName;
		}

		/**
		 * <h3 class="en-US">Setter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Setter方法</h3>
		 *
		 * @param userName <span class="en-US">Authenticate username</span>
		 *                 <span class="zh-CN">身份认证用户名</span>
		 */
		public void setUserName(final String userName) {
			this.userName = userName;
		}

		/**
		 * <h3 class="en-US">Getter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Getter方法</h3>
		 *
		 * @return <span class="en-US">Authenticate password</span>
		 * <span class="zh-CN">身份认证密码</span>
		 */
		public String getPassword() {
			return this.password;
		}

		/**
		 * <h3 class="en-US">Setter method for proxy type</h3>
		 * <h3 class="zh-CN">代理服务器类型的Setter方法</h3>
		 *
		 * @param password <span class="en-US">Authenticate password</span>
		 *                 <span class="zh-CN">身份认证密码</span>
		 */
		public void setPassword(final String password) {
			this.password = password;
		}
	}

	/**
	 * <h2 class="en-US">Abstract proxy configure builder for Generics Type</h2>
	 * <p class="en-US">
	 * The current abstract class is using to integrate to another builder
	 * which configure contains proxy configure information.
	 * </p>
	 * <h2 class="zh-CN">拥有父构造器的代理服务器配置信息抽象构造器</h2>
	 * <p class="zh-CN">当前抽象构建器用于整合到包含代理服务器配置信息的其他配置构建器</p>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jan 4, 2019 16:22:54 $
	 */
	@SuppressWarnings("unused")
	public static final class ProxyConfigBuilder<P extends ParentBuilder> extends AbstractBuilder<P, ProxyConfig> {

		/**
		 * <span class="en-US">Proxy configure information</span>
		 * <span class="zh-CN">代理服务器配置信息</span>
		 */
		private final ProxyConfig proxyConfig;
		/**
		 * <h2 class="en-US">Configure information modified flag</h2>
		 * <h2 class="zh-CN">配置信息修改标记</h2>
		 */
		private boolean modified = Boolean.FALSE;

		/**
		 * <h3 class="en-US">Protected constructor for AbstractProxyConfigBuilder</h3>
		 * <h3 class="zh-CN">AbstractProxyConfigBuilder的构造函数</h3>
		 *
		 * @param proxyConfig <span class="en-US">Proxy configure information</span>
		 *                    <span class="zh-CN">代理服务器配置信息</span>
		 */
		public ProxyConfigBuilder(final ProxyConfig proxyConfig) {
			this(null, proxyConfig);
		}

		/**
		 * <h3 class="en-US">Protected constructor for AbstractProxyConfigBuilder</h3>
		 * <h3 class="zh-CN">AbstractProxyConfigBuilder的构造函数</h3>
		 *
		 * @param parentBuilder <span class="en-US">Parent builder instance object</span>
		 *                      <span class="zh-CN">父构建器实例对象</span>
		 * @param proxyConfig   <span class="en-US">Proxy configure information</span>
		 *                      <span class="zh-CN">代理服务器配置信息</span>
		 */
		public ProxyConfigBuilder(final P parentBuilder, final ProxyConfig proxyConfig) {
			super(parentBuilder);
			this.proxyConfig = proxyConfig;
		}

		/**
		 * <h3 class="en-US">Configure proxy type</h3>
		 * <h3 class="zh-CN">配置代理服务器类型</h3>
		 *
		 * @param proxyType <span class="en-US">Enumeration value of proxy server</span>
		 *                  <span class="zh-CN">代理服务器类型枚举值</span>
		 * @return <span class="en-US">Current builder instance</span>
		 * <span class="zh-CN">当前构造器实例对象</span>
		 */
		public ProxyConfigBuilder<P> proxyType(final Proxy.Type proxyType) {
			if (ObjectUtils.nullSafeEquals(this.proxyConfig.getProxyType(), proxyType)) {
				return this;
			}
			this.proxyConfig.setProxyType(proxyType);
			this.modified = Boolean.TRUE;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure proxy server information</h3>
		 * <h3 class="zh-CN">配置代理服务器信息</h3>
		 *
		 * @param serverAddress <span class="en-US">Proxy server address</span>
		 *                      <span class="zh-CN">代理服务器地址</span>
		 * @param serverPort    <span class="en-US">Proxy server port</span>
		 *                      <span class="zh-CN">代理服务器端口号</span>
		 * @return <span class="en-US">Current builder instance</span>
		 * <span class="zh-CN">当前构造器实例对象</span>
		 */
		public ProxyConfigBuilder<P> serverConfig(final String serverAddress, final int serverPort) {
			if (Proxy.Type.DIRECT.equals(this.proxyConfig.getProxyType())
					|| (ObjectUtils.nullSafeEquals(this.proxyConfig.getProxyAddress(), serverAddress)
					&& this.proxyConfig.getProxyPort() == serverPort)) {
				return this;
			}
			this.proxyConfig.setProxyAddress(serverAddress);
			this.proxyConfig.setProxyPort(serverPort);
			this.modified = Boolean.TRUE;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure proxy servers authenticate information</h3>
		 * <h3 class="zh-CN">配置代理服务器身份验证信息</h3>
		 *
		 * @param userName <span class="en-US">Authenticate username</span>
		 *                 <span class="zh-CN">身份认证用户名</span>
		 * @param passWord <span class="en-US">Authenticate password</span>
		 *                 <span class="zh-CN">身份认证密码</span>
		 * @return <span class="en-US">Current builder instance</span>
		 * <span class="zh-CN">当前构造器实例对象</span>
		 */
		public ProxyConfigBuilder<P> authenticator(final String userName, final String passWord) {
			if (Proxy.Type.DIRECT.equals(this.proxyConfig.getProxyType())
					|| (ObjectUtils.nullSafeEquals(this.proxyConfig.getUserName(), userName)
					&& ObjectUtils.nullSafeEquals(this.proxyConfig.getPassword(), passWord))) {
				return this;
			}
			this.proxyConfig.setUserName(userName);
			this.proxyConfig.setPassword(passWord);
			this.modified = Boolean.TRUE;
			return this;
		}

		@Override
		public ProxyConfig build() {
			return this.proxyConfig;
		}
	}
}
