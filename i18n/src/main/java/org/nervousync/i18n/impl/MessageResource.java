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
package org.nervousync.i18n.impl;

import com.ibm.icu.text.MessageFormat;
import jakarta.annotation.Nonnull;
import org.nervousync.beans.i18n.BundleMessage;
import org.nervousync.commons.Globals;
import org.nervousync.utils.i18n.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <h2 class="en-US">Internationalization Message Resources Define</h2>
 * <h2 class="zh-CN">国际化信息资源定义</h2>
 * .0
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 19, 2023 16:39:41 $
 */
public final class MessageResource {

	/**
	 * <span class="en-US">Logger instance</span>
	 * <span class="zh-CN">日志实例</span>
	 */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	/**
	 * <span class="en-US">Mapping table of error codes and resource keys</span>
	 * <span class="zh-CN">错误代码与资源索引的映射表</span>
	 */
	private final Map<Long, String> codeKeysMap;
	/**
	 * <span class="en-US">Resource information map</span>
	 * <span class="zh-CN">资源信息映射表</span>
	 */
	private final Map<String, BundleMessage> resourcesMap;
	/**
	 * <span class="en-US">Cached resource formatter map</span>
	 * <span class="zh-CN">缓存的资源信息格式化器实例映射表</span>
	 */
	private final Map<String, MessageFormat> cachedFormaterMap;

	/**
	 * <h3 class="en-US">Constructor for Resource</h3>
	 * <h3 class="zh-CN">国际化资源的构造方法</h3>
	 */
	public MessageResource() {
		this.codeKeysMap = new HashMap<>();
		this.resourcesMap = new HashMap<>();
		this.cachedFormaterMap = new HashMap<>();
	}

	/**
	 * <h3 class="en-US">Update resource messages</h3>
	 * <h3 class="zh-CN">更新国际化信息内容</h3>
	 *
	 * @param errorCodes     <span class="en-US">Error codes and message key mapping table</span>
	 *                       <span class="zh-CN">错误代码与信息识别代码的映射表</span>
	 * @param bundleMessages <span class="en-US">Mapping of message language code and message content</span>
	 *                       <span class="zh-CN">信息语言代码与信息内容的定义映射表</span>
	 */
	public void updateResource(@Nonnull final Map<String, String> errorCodes,
	                           @Nonnull final Map<String, Map<String, BundleMessage>> bundleMessages) {
		errorCodes.forEach((key, value) -> {
			long errorCode = LocaleUtils.parseErrorCode(key);
			if (errorCode != Globals.DEFAULT_VALUE_LONG) {
				this.codeKeysMap.put(errorCode, value);
			}
		});
		bundleMessages.forEach((languageCode, messageMap) ->
				messageMap.forEach((key, bundleMessage) -> {
					String identifyKey = identifyKey(key, languageCode);
					if (this.resourcesMap.containsKey(identifyKey)) {
						this.cachedFormaterMap.remove(identifyKey);
						this.logger.warn("Override resource key: {}, language code: {}, original value: {}, new value: {}",
								key, languageCode, this.resourcesMap.get(identifyKey), bundleMessage);
					}
					this.resourcesMap.put(identifyKey, bundleMessage);
				}));
	}

	/**
	 * <h3 class="en-US">Get the multilingual information key corresponding to the error code.</h3>
	 * <h3 class="zh-CN">获取错误代码对应的多语言信息键值</h3>
	 *
	 * @param errorCode <span class="en-US">Error code</span>
	 *                  <span class="zh-CN">错误代码</span>
	 * @return <span class="en-US">The multilingual information key or empty string if not found</span>
	 * <span class="zh-CN">多语言信息键值，如果未找到则返回空字符串</span>
	 */
	public String errorKey(final long errorCode) {
		return this.codeKeysMap.getOrDefault(errorCode, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Retrieve internationalization information content and formatted by given collections</h3>
	 * <h3 class="zh-CN">读取国际化资源信息详情并使用给定的参数集合格式化资源信息</h3>
	 *
	 * @param messageKey      <span class="en-US">Message identify key</span>
	 *                        <span class="zh-CN">信息识别键值</span>
	 * @param languageCode    <span class="en-US">Language code</span>
	 *                        <span class="zh-CN">语言代码</span>
	 * @param defaultLanguage <span class="en-US">Default language code</span>
	 *                        <span class="zh-CN">默认语言代码</span>
	 * @param collections     <span class="en-US">given parameters of information formatter</span>
	 *                        <span class="zh-CN">用于资源信息格式化的参数</span>
	 * @return <span class="en-US">Formatted resource information or empty string if not found</span>
	 * <span class="zh-CN">格式化的资源信息，如果未找到则返回空字符串</span>
	 */
	public String findMessage(final String messageKey, final String languageCode,
	                          final String defaultLanguage, final Object... collections) {
		if (LocaleUtils.isEmpty(languageCode)) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		MessageFormat messageFormat = this.retrieveFormatter(messageKey, languageCode);
		if (messageFormat == null) {
			messageFormat = this.retrieveFormatter(messageKey, defaultLanguage);
		}
		if (messageFormat == null) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		BundleMessage bundleMessage = this.resourcesMap.get(identifyKey(messageKey, languageCode));
		if (bundleMessage == null) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		if (bundleMessage.getArguments() == null || bundleMessage.getArguments().isEmpty()) {
			return messageFormat.format(collections);
		} else {
			Map<String, Object> argumentsMap = new HashMap<>();
			int index = 0;
			for (String name : bundleMessage.getArguments()) {
				argumentsMap.put(name, index < collections.length ? collections[index] : null);
				index++;
			}
			return messageFormat.format(argumentsMap);
		}
	}

	/**
	 * <h3 class="en-US">Retrieve message formatter instance</h3>
	 * <h3 class="zh-CN">读取信息格式化器实例对象</h3>
	 *
	 * @param messageKey   <span class="en-US">Message identify key</span>
	 *                     <span class="zh-CN">信息识别键值</span>
	 * @param languageCode <span class="en-US">Language code</span>
	 *                     <span class="zh-CN">语言代码</span>
	 * @return <span class="en-US">Retrieved message formatter instance or <code>null</code> if argument messageKey is <code>null</code> or empty string</span>
	 * <span class="zh-CN">读取的信息格式化器实例对象，如果参数 messageKey 未找到或为空字符串，则返回 <code>null</code></span>
	 */
	private MessageFormat retrieveFormatter(final String messageKey, final String languageCode) {
		if (LocaleUtils.isEmpty(messageKey)) {
			return null;
		}
		String identifyKey = identifyKey(messageKey, languageCode);
		if (this.resourcesMap.containsKey(identifyKey)) {
			if (!this.cachedFormaterMap.containsKey(identifyKey)) {
				BundleMessage bundleMessage = this.resourcesMap.get(identifyKey);
				this.cachedFormaterMap.put(identifyKey,
						new MessageFormat(bundleMessage.getPattern(), LocaleUtils.parseLocale(languageCode)));
			}
		}
		return this.cachedFormaterMap.get(identifyKey);
	}

	/**
	 * <h3 class="en-US">Retrieve resource identify key by given class</h3>
	 * <h3 class="zh-CN">根据给定的类查找资源唯一识别码</h3>
	 *
	 * @param errorCode    <span class="en-US">Error code</span>
	 *                     <span class="zh-CN">错误代码</span>
	 * @param languageCode <span class="en-US">Language code</span>
	 *                     <span class="zh-CN">语言代码</span>
	 */
	public static String identifyKey(final long errorCode, final String languageCode) {
		return "0x" + Long.toHexString(errorCode) + Globals.DEFAULT_MULTILINGUAL_KEY_SPLIT_CHARACTER + languageCode;
	}

	/**
	 * <h3 class="en-US">Retrieve resource identify key by given class</h3>
	 * <h3 class="zh-CN">根据给定的类查找资源唯一识别码</h3>
	 *
	 * @param messageKey   <span class="en-US">Message identify key</span>
	 *                     <span class="zh-CN">信息识别键值</span>
	 * @param languageCode <span class="en-US">Language code</span>
	 *                     <span class="zh-CN">语言代码</span>
	 */
	public static String identifyKey(final String messageKey, final String languageCode) {
		return messageKey + Globals.DEFAULT_MULTILINGUAL_KEY_SPLIT_CHARACTER + languageCode;
	}
}
