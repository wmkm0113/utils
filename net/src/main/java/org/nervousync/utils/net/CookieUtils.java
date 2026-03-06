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
package org.nervousync.utils.net;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nervousync.commons.Globals;
import org.nervousync.utils.core.ObjectUtils;
import org.nervousync.utils.core.StringUtils;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * <h2 class="en-US">Cookie utilities</h2>
 * <h2 class="zh-CN">Cookie工具集</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.2.0 $ $Date: Jan 13, 2010 13:49:41 $
 */
@SuppressWarnings("unused")
public final class CookieUtils {

	/**
	 * <h3 class="en-US">Set cookie information</h3>
	 * <h3 class="zh-CN">写入Cookie信息</h3>
	 *
	 * @param cookieName  <span class="en-US">Cookie name</span>
	 *                    <span class="zh-CN">Cookie名称</span>
	 * @param cookieValue <span class="en-US">Cookie value</span>
	 *                    <span class="zh-CN">Cookie值</span>
	 * @param request     <span class="en-US">HttpServletRequest instance</span>
	 *                    <span class="zh-CN">Http请求实例对象</span>
	 * @param response    <span class="en-US">HttpServletResponse instance</span>
	 *                    <span class="zh-CN">Http响应实例对象</span>
	 */
	public static void setCookie(final String cookieName, final String cookieValue,
	                             final HttpServletRequest request, final HttpServletResponse response) {
		setCookie(cookieName, cookieValue, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING, request, response);
	}

	/**
	 * <h3 class="en-US">Set cookie information</h3>
	 * <h3 class="zh-CN">写入Cookie信息</h3>
	 *
	 * @param cookieName  <span class="en-US">Cookie name</span>
	 *                    <span class="zh-CN">Cookie名称</span>
	 * @param cookieValue <span class="en-US">Cookie value</span>
	 *                    <span class="zh-CN">Cookie值</span>
	 * @param domainName  <span class="en-US">Domain name</span>
	 *                    <span class="zh-CN">所在域名</span>
	 * @param request     <span class="en-US">HttpServletRequest instance</span>
	 *                    <span class="zh-CN">Http请求实例对象</span>
	 * @param response    <span class="en-US">HttpServletResponse instance</span>
	 *                    <span class="zh-CN">Http响应实例对象</span>
	 */
	public static void setCookie(final String cookieName, final String cookieValue, final String domainName,
	                             final HttpServletRequest request, final HttpServletResponse response) {
		setCookie(cookieName, cookieValue, domainName, Globals.DEFAULT_VALUE_STRING, null, request, response);
	}

	/**
	 * <h3 class="en-US">Set cookie information</h3>
	 * <h3 class="zh-CN">写入Cookie信息</h3>
	 *
	 * @param cookieName  <span class="en-US">Cookie name</span>
	 *                    <span class="zh-CN">Cookie名称</span>
	 * @param cookieValue <span class="en-US">Cookie value</span>
	 *                    <span class="zh-CN">Cookie值</span>
	 * @param domainName  <span class="en-US">Domain name</span>
	 *                    <span class="zh-CN">所在域名</span>
	 * @param cookiePath  <span class="en-US">Cookie path</span>
	 *                    <span class="zh-CN">Cookie路径</span>
	 * @param request     <span class="en-US">HttpServletRequest instance</span>
	 *                    <span class="zh-CN">Http请求实例对象</span>
	 * @param response    <span class="en-US">HttpServletResponse instance</span>
	 *                    <span class="zh-CN">Http响应实例对象</span>
	 */
	public static void setCookie(final String cookieName, final String cookieValue,
	                             final String domainName, final String cookiePath,
	                             final HttpServletRequest request, final HttpServletResponse response) {
		setCookie(cookieName, cookieValue, domainName, cookiePath, null, request, response);
	}

	/**
	 * <h3 class="en-US">Set cookie information</h3>
	 * <h3 class="zh-CN">写入Cookie信息</h3>
	 *
	 * @param cookieName  <span class="en-US">Cookie name</span>
	 *                    <span class="zh-CN">Cookie名称</span>
	 * @param cookieValue <span class="en-US">Cookie value</span>
	 *                    <span class="zh-CN">Cookie值</span>
	 * @param lifeCycle   <span class="en-US">Life cycle</span>
	 *                    <span class="zh-CN">生存周期</span>
	 * @param request     <span class="en-US">HttpServletRequest instance</span>
	 *                    <span class="zh-CN">Http请求实例对象</span>
	 * @param response    <span class="en-US">HttpServletResponse instance</span>
	 *                    <span class="zh-CN">Http响应实例对象</span>
	 */
	public static void setCookie(final String cookieName, final String cookieValue, final Integer lifeCycle,
	                             final HttpServletRequest request, final HttpServletResponse response) {
		setCookie(cookieName, cookieValue, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING,
				lifeCycle, request, response);
	}

	/**
	 * <h3 class="en-US">Set cookie information</h3>
	 * <h3 class="zh-CN">写入Cookie信息</h3>
	 *
	 * @param cookieName  <span class="en-US">Cookie name</span>
	 *                    <span class="zh-CN">Cookie名称</span>
	 * @param cookieValue <span class="en-US">Cookie value</span>
	 *                    <span class="zh-CN">Cookie值</span>
	 * @param domainName  <span class="en-US">Domain name</span>
	 *                    <span class="zh-CN">所在域名</span>
	 * @param cookiePath  <span class="en-US">Cookie path</span>
	 *                    <span class="zh-CN">Cookie路径</span>
	 * @param lifeCycle   <span class="en-US">Life cycle</span>
	 *                    <span class="zh-CN">生存周期</span>
	 * @param request     <span class="en-US">HttpServletRequest instance</span>
	 *                    <span class="zh-CN">Http请求实例对象</span>
	 * @param response    <span class="en-US">HttpServletResponse instance</span>
	 *                    <span class="zh-CN">Http响应实例对象</span>
	 */
	public static void setCookie(final String cookieName, final String cookieValue, final String domainName,
	                             final String cookiePath, final Integer lifeCycle,
	                             final HttpServletRequest request, final HttpServletResponse response) {
		response.addCookie(getCookie(cookieName, request)
				.map(cookie -> {
					cookie.setValue(cookieValue);
					return cookie;
				})
				.orElse(newCookie(cookieName, cookieValue, domainName, cookiePath, lifeCycle)));
	}

	/**
	 * <h3 class="en-US">Read cookie instance</h3>
	 * <h3 class="zh-CN">读取Cookie实例对象</h3>
	 *
	 * @param cookieName <span class="en-US">Cookie name</span>
	 *                   <span class="zh-CN">Cookie名称</span>
	 * @param request    <span class="en-US">HttpServletRequest instance</span>
	 *                   <span class="zh-CN">Http请求实例对象</span>
	 * @return <span class="en-US">Cookie instance or null if not exists</span>
	 * <span class="zh-CN">Cookie实例对象，如果不存在则返回null</span>
	 */
	public static Optional<Cookie> getCookie(final String cookieName, @Nonnull final HttpServletRequest request) {
		return Optional.ofNullable(request.getCookies())
				.flatMap(cookies ->
						Stream.of(cookies)
								.filter(cookie -> ObjectUtils.nullSafeEquals(cookie.getName(), cookieName))
								.findFirst());
	}

	/**
	 * <h3 class="en-US">Read cookie information</h3>
	 * <h3 class="zh-CN">读取Cookie信息</h3>
	 *
	 * @param cookieName <span class="en-US">Cookie name</span>
	 *                   <span class="zh-CN">Cookie名称</span>
	 * @param request    <span class="en-US">HttpServletRequest instance</span>
	 *                   <span class="zh-CN">Http请求实例对象</span>
	 * @return <span class="en-US">Cookie value or null if not exists</span>
	 * <span class="zh-CN">Cookie值，如果不存在则返回null</span>
	 */
	public static String getCookieValue(final String cookieName, final HttpServletRequest request) {
		return getCookie(cookieName, request)
				.map(Cookie::getValue)
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Remove cookie information</h3>
	 * <h3 class="zh-CN">移除Cookie信息</h3>
	 *
	 * @param cookieName <span class="en-US">Cookie name</span>
	 *                   <span class="zh-CN">Cookie名称</span>
	 * @param request    <span class="en-US">HttpServletRequest instance</span>
	 *                   <span class="zh-CN">Http请求实例对象</span>
	 * @param response   <span class="en-US">HttpServletResponse instance</span>
	 *                   <span class="zh-CN">Http响应实例对象</span>
	 */
	public static void delCookie(final String cookieName,
	                             final HttpServletRequest request, final HttpServletResponse response) {
		getCookie(cookieName, request).ifPresent(cookie -> delCookie(cookie, response));
	}

	/**
	 * <h3 class="en-US">Remove cookie information</h3>
	 * <h3 class="zh-CN">移除Cookie信息</h3>
	 *
	 * @param cookie   <span class="en-US">Cookie instance</span>
	 *                 <span class="zh-CN">Cookie实例对象</span>
	 * @param response <span class="en-US">HttpServletResponse instance</span>
	 *                 <span class="zh-CN">Http响应实例对象</span>
	 */
	public static void delCookie(final Cookie cookie, final HttpServletResponse response) {
		if (cookie != null) {
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		}
	}

	/**
	 * <h3 class="en-US">Create a cookie instance object</h3>
	 * <h3 class="zh-CN">创建 Cookie 实例对象</h3>
	 *
	 * @param cookieName  <span class="en-US">Cookie name</span>
	 *                    <span class="zh-CN">Cookie名称</span>
	 * @param cookieValue <span class="en-US">Cookie value</span>
	 *                    <span class="zh-CN">Cookie值</span>
	 * @param domainName  <span class="en-US">Domain name</span>
	 *                    <span class="zh-CN">所在域名</span>
	 * @param cookiePath  <span class="en-US">Cookie path</span>
	 *                    <span class="zh-CN">Cookie路径</span>
	 * @param lifeCycle   <span class="en-US">Life cycle</span>
	 *                    <span class="zh-CN">生存周期</span>
	 * @return <span class="en-US">Cookie instance object</span>
	 * <span class="zh-CN">Cookie实例对象</span>
	 */
	private static Cookie newCookie(final String cookieName, final String cookieValue, final String domainName,
	                                final String cookiePath, final Integer lifeCycle) {
		Cookie cookie = new Cookie(cookieName, cookieValue);
		cookie.setPath(StringUtils.isEmpty(cookiePath) ? "/" : cookiePath);
		if (StringUtils.notBlank(domainName)) {
			cookie.setDomain("." + domainName);
		}
		Optional.ofNullable(lifeCycle)
				.filter(cycle -> cycle >= 0)
				.map(cycle -> 60 * 60 * cycle)
				.ifPresent(cookie::setMaxAge);
		cookie.setValue(cookieValue);
		return cookie;
	}

	/**
	 * <h3 class="en-US">Private constructor for CookieUtils</h3>
	 * <h3 class="zh-CN">Cookie 工具集的私有构造方法</h3>
	 */
	private CookieUtils() {
	}
}
