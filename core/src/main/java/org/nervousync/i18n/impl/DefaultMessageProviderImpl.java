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

import jakarta.annotation.Nonnull;
import org.nervousync.annotations.provider.Provider;
import org.nervousync.commons.Globals;
import org.nervousync.i18n.MessageAgent;
import org.nervousync.i18n.MessageProvider;
import org.nervousync.utils.i18n.LocaleUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * <h2 class="en-US">Default internationalization provider implement class</h2>
 * <h2 class="zh-CN">默认的国际化适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 19, 2023 16:39:41 $
 */
@Provider(name = "defaultI18NProvider", titleKey = "default.i18n.provider.title", descriptionKey = "default.i18n.provider.description")
public final class DefaultMessageProviderImpl implements MessageProvider {

	/**
	 * <span class="en-US">Cached resource formatter map</span>
	 * <span class="zh-CN">缓存的资源信息格式化器实例映射表</span>
	 */
	private static final Map<String, MessageFormat> CACHED_FORMATER_MAP = new HashMap<>();

	/**
	 * <span class="en-US">Registered error code to message mapping table</span>
	 * <span class="zh-CN">已注册的错误代码和错误信息映射表</span>
	 */
	private static final Map<Long, String> REGISTERED_ERROR_CODE_MAP = new HashMap<>();
	/**
	 * <span class="en-US">Registered message mapping table</span>
	 * <span class="zh-CN">已注册的信息映射表</span>
	 */
	private static final Map<String, String> REGISTERED_MESSAGE_MAP = new HashMap<>();
	/**
	 * <span class="en-US">Error message resource path</span>
	 * <span class="zh-CN">错误信息资源文件路径</span>
	 */
	public static final String ERROR_RESOURCE_PATH = "META-INF/ErrorCode.xml";
	/**
	 * <span class="en-US">Message resource path</span>
	 * <span class="zh-CN">信息资源文件路径</span>
	 */
	public static final String MESSAGE_RESOURCE_PATH = "META-INF/Resources.xml";

	public DefaultMessageProviderImpl() {
		try {
			DefaultMessageAgentImpl.class.getClassLoader().getResources(ERROR_RESOURCE_PATH)
					.asIterator()
					.forEachRemaining(url ->
							loadProperties(url).forEach((key, value) ->
									REGISTER_ERROR_CODE(key.toString(), value.toString())));
			DefaultMessageAgentImpl.class.getClassLoader().getResources(MESSAGE_RESOURCE_PATH)
					.asIterator()
					.forEachRemaining(this::registerBundle);
		} catch (IOException ignore) {
		}
	}

	private static void REGISTER_ERROR_CODE(final String key, final String value) {
		long errorCode = LocaleUtils.parseErrorCode(key);
		if (errorCode != Globals.DEFAULT_VALUE_LONG) {
			REGISTERED_ERROR_CODE_MAP.put(errorCode, value);
		}
	}

	private static Properties loadProperties(final URL url) {
		Properties properties = new Properties();
		try (InputStream inputStream = url.openStream()) {
			properties.loadFromXML(inputStream);
		} catch (Exception ignore) {
		}
		return properties;
	}

	@Override
	public void registerBundle(final URL url) {
		loadProperties(url).forEach((key, value) ->
				REGISTERED_MESSAGE_MAP.put(key.toString(), value.toString()));
	}

	@Override
	public void removeBundle(@Nonnull final String groupId, @Nonnull final String bundle) {
		//  Do nothing
	}

	@Override
	public MessageAgent newAgent(final String groupId, final String bundle) {
		return new DefaultMessageAgentImpl();
	}

	@Override
	public MessageAgent newAgent(final Class<?> clazz) {
		return new DefaultMessageAgentImpl();
	}

	private static final class DefaultMessageAgentImpl extends MessageAgent {

		@Override
		public String errorMessage(final long errorCode, final String languageCode, final Object... collections) {
			return this.findMessage(REGISTERED_ERROR_CODE_MAP.get(errorCode), languageCode, collections);
		}

		@Override
		public String findMessage(final String messageKey, final String languageCode, final Object... collections) {
			return obtainFormatter(messageKey).format(collections);
		}
	}

	private static MessageFormat obtainFormatter(final String messageKey) {
		if (!CACHED_FORMATER_MAP.containsKey(messageKey)) {
			CACHED_FORMATER_MAP.put(messageKey,
					new MessageFormat(encodePattern(REGISTERED_MESSAGE_MAP.getOrDefault(messageKey, messageKey))));
		}
		return CACHED_FORMATER_MAP.get(messageKey);
	}

	private static String encodePattern(final String pattern) {
		if (pattern.contains("'")) {
			StringBuilder stringBuilder = new StringBuilder();
			int position = Globals.INITIALIZE_INT_VALUE;
			int index = pattern.indexOf("'");
			while (index > 0) {
				stringBuilder.append(pattern, position, index);
				stringBuilder.append("''");
				position = index + 1;
				index = pattern.indexOf("'", position);
			}
			return stringBuilder.toString();
		} else {
			return pattern;
		}
	}
}
