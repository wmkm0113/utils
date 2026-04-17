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

package org.nervousync.utils.i18n;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.utils.core.StringUtils;

import java.util.*;

public final class LocaleUtils {


	/**
	 * <span class="en-US">Default locale instance, usually value is the default locale for this instance of the Java Virtual Machine.</span>
	 * <span class="zh-CN">默认区域设置实例，通常值是 Java 虚拟机实例的默认区域设置。</span>
	 */
	private static String DEFAULT_LANGUAGE = LocaleUtils.languageCode(Globals.DEFAULT_LOCALE);

	/**
	 * <h3 class="en-US">Get default language code</h3>
	 * <h3 class="zh-CN">获取默认语言代码</h3>
	 *
	 * @return <span class="en-US">Default language code</span>
	 * <span class="zh-CN">默认语言代码</span>
	 */
	public static String defaultLanguage() {
		return DEFAULT_LANGUAGE;
	}

	/**
	 * <h3 class="en-US">Configure default locale</h3>
	 * <h3 class="zh-CN">设置默认语言</h3>
	 *
	 * @param locale <span class="en-US">Default locale instance</span>
	 *               <span class="zh-CN">默认区域设置实例</span>
	 */
	public static void defaultLocale(@Nonnull final Locale locale) {
		DEFAULT_LANGUAGE = languageCode(locale);
	}

	/**
	 * <h3 class="en-US">Convert locale instance to string.</h3>
	 * <h3 class="zh-CN">将语言环境实例转换为字符串。</h3>
	 *
	 * @param locale <span class="en-US">locale instance</span>
	 *               <span class="zh-CN">区域设置实例</span>
	 * @return <span class="en-US">Converted string</span>
	 * <span class="zh-CN">转换后的字符串</span>
	 */
	public static String languageCode(@Nonnull final Locale locale) {
		return Optional.of(locale.getCountry())
				.filter(LocaleUtils::notBlank)
				.map(countryCode -> locale.getLanguage() + "-" + countryCode.toUpperCase())
				.orElse(locale.getLanguage());
	}

	/**
	 * <h3 class="en-US">Convert language code string to locale instance.</h3>
	 * <h3 class="zh-CN">将语言代码字符串转换为语言环境实例。</h3>
	 *
	 * @param languageCode <span class="en-US">Language code string</span>
	 *                     <span class="zh-CN">语言代码字符串</span>
	 * @return <span class="en-US">Locale instance</span>
	 * <span class="zh-CN">语言环境实例</span>
	 */
	public static Locale parseLocale(final String languageCode) {
		if (languageCode == null) {
			return null;
		}
		String locale = StringUtils.replace(languageCode,"-", "_");

		String language = Globals.DEFAULT_VALUE_STRING;
		String country = Globals.DEFAULT_VALUE_STRING;
		String variant = Globals.DEFAULT_VALUE_STRING;
		StringTokenizer st = new StringTokenizer(locale, "_");
		int index = 0;
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			switch (index) {
				case 0:
					language = token;
					break;
				case 1:
					country = token;
					break;
				case 2:
					variant = token;
					break;
			}
			index++;
		}
		return (!language.isEmpty() ? new Locale(language, country, variant) : null);
	}

	/**
	 * <h3 class="en-US">Convert error code strings to Long values.</h3>
	 * <h3 class="zh-CN">转换错误代码字符串为Long值</h3>
	 *
	 * @param errorCode <span class="en-US">Error code string</span>
	 *                  <span class="zh-CN">错误代码字符串</span>
	 * @return <span class="en-US">Converted long value</span>
	 * <span class="zh-CN">转换后的Long值</span>
	 */
	public static long parseErrorCode(final String errorCode) {
		if (LocaleUtils.isEmpty(errorCode)) {
			return Globals.DEFAULT_VALUE_LONG;
		}
		if (errorCode.startsWith("0x")) {
			return Long.parseLong(errorCode.substring(2), 16);
		} else if (errorCode.startsWith("0o")) {
			return Long.parseLong(errorCode.substring(2), 8);
		} else if (errorCode.startsWith("0b")) {
			return Long.parseLong(errorCode.substring(2), 2);
		} else {
			return Long.parseLong(errorCode.startsWith("0d") ? errorCode.substring(2) : errorCode);
		}
	}

	/**
	 * <h3 class="en-US">Check that the given CharSequence is <code>null</code> or length 0.</h3>
	 * <span class="en-US">Will return <code>true</code> for a CharSequence that purely consists of blank.</span>
	 * <h3 class="zh-CN">检查给定的 CharSequence 是否为 <code>null</code> 或长度为 0。</h3>
	 * <span class="zh-CN">对于完全由空白组成的 CharSequence 将返回 <code>true</code>。</span>
	 * <pre>
	 * StringUtils.isEmpty(null) = true
	 * StringUtils.isEmpty(Globals.DEFAULT_VALUE_STRING) = true
	 * StringUtils.isEmpty(" ") = false
	 * StringUtils.isEmpty("Hello") = false
	 * </pre>
	 *
	 * @param str <span class="en-US">The CharSequence to check (maybe <code>null</code>)</span>
	 *            <span class="zh-CN">要检查的 CharSequence （可能 <code>null</code>）</span>
	 * @return <span class="en-US"><code>true</code> if the CharSequence is null or length 0.</span>
	 * <span class="zh-CN">如果 CharSequence 为 null 或长度为 0，则 <code>true</code></span>
	 */
	public static boolean isEmpty(final CharSequence str) {
		return ((str == null || str.length() == 0));
	}

	/**
	 * <h3 class="en-US">Check that the given CharSequence is neither <code>null</code> nor only blank character.</h3>
	 * <span class="en-US">Will return <code>true</code> for a CharSequence that purely consists of blank.</span>
	 * <h3 class="zh-CN">检查给定的 CharSequence 既不是 <code>null</code> 也不是空白字符。</h3>
	 * <span class="zh-CN">对于完全由空白组成的 CharSequence 将返回 <code>true</code>。</span>
	 *
	 * @param str <span class="en-US">the String to check (maybe <code>null</code>)</span>
	 *            <span class="zh-CN">要检查的字符串（可能 <code>null</code>）</span>
	 * @return <span class="en-US"><code>true</code> if the CharSequence is not <code>null</code> or blank character and has length.</span>
	 * <span class="zh-CN">如果 CharSequence 不是<code>null</code>或空白字符并且有长度，则<code>true</code></span>
	 */
	public static boolean notBlank(final String str) {
		return (str != null && !str.trim().isEmpty());
	}
}
