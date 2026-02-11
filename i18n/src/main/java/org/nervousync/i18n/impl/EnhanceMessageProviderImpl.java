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
import org.nervousync.beans.i18n.BundleResource;
import org.nervousync.commons.Globals;
import org.nervousync.commons.i18n.InternationalizationGlobals;
import org.nervousync.i18n.MessageAgent;
import org.nervousync.i18n.MessageProvider;
import org.nervousync.utils.core.ClassUtils;
import org.nervousync.utils.i18n.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;

/**
 * <h2 class="en-US">Advanced internationalization provider implement class</h2>
 * <p class="en-US">Implemented using ICU4j, providing complete plural support.</p>
 * <h2 class="zh-CN">高级国际化适配器实现类</h2>
 * <p class="zh-CN">使用 ICU4j 实现，提供了完整的 Plural 支持</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 19, 2023 16:39:41 $
 */
@Provider(name = "enhanceI18NProvider", titleKey = "enhance.i18n.provider.title", descriptionKey = "enhance.i18n.provider.description")
public final class EnhanceMessageProviderImpl implements MessageProvider {

	/**
	 * <span class="en-US">Logger instance</span>
	 * <span class="zh-CN">日志实例</span>
	 */
	private final static Logger LOGGER = LoggerFactory.getLogger(EnhanceMessageProviderImpl.class);

	/**
	 * <span class="en-US">Registered resources map</span>
	 * <span class="zh-CN">已注册的资源信息映射表</span>
	 */
	private static final String DEFAULT_BUNDLE_KEY = "org.nervousync:utils";

	/**
	 * <span class="en-US">Registered resources map</span>
	 * <span class="zh-CN">已注册的资源信息映射表</span>
	 */
	private final Map<String, MessageResource> registeredResources = new HashMap<>();
	/**
	 * <span class="en-US">Mapping table between resource file addresses and unique resource identifiers.</span>
	 * <span class="zh-CN">资源文件地址和资源唯一识别码的映射表</span>
	 */
	private final Map<String, String> identifyKeyMap = new HashMap<>();

	/**
	 * <h3 class="en-US">Constructor method for the advanced internationalization provider implement class</h3>
	 * <h3 class="zh-CN">高级国际化适配器实现类的构造方法</h3>
	 */
	public EnhanceMessageProviderImpl() {
		try {
			ClassUtils.getDefaultClassLoader().getResources(InternationalizationGlobals.BUNDLE_RESOURCE_PATH)
					.asIterator()
					.forEachRemaining(this::registerBundle);
		} catch (IOException ignore) {
		}
	}

	@Override
	public MessageAgent newAgent(final String groupId, final String bundle) {
		return this.newAgent(this.bundleKey(groupId, bundle));
	}

	@Override
	public MessageAgent newAgent(final Class<?> clazz) {
		return this.newAgent(this.bundleKey(clazz));
	}

	@Override
	public void registerBundle(final URL url) {
		try (InputStream inputStream = url.openStream()) {
			BundleResource bundleResource = InternationalizationGlobals.readResource(inputStream);
			if (bundleResource == null) {
				throw new IOException("Load bundle resource error! ");
			}
			String bundleKey = this.bundleKey(bundleResource.getGroupId(), bundleResource.getBundle());
			MessageResource messageResource =
					this.registeredResources.getOrDefault(bundleKey, new MessageResource());
			messageResource.updateResource(bundleResource.getErrorCodes(), bundleResource.getBundleMessages());
			this.registeredResources.put(bundleKey, messageResource);
			String basePath = url.getPath().substring(0, url.getPath().length() - InternationalizationGlobals.BUNDLE_RESOURCE_PATH.length());
			if (basePath.startsWith(Globals.FILE_URL_PREFIX)) {
				basePath = basePath.substring(Globals.FILE_URL_PREFIX.length());
			}
			if (basePath.endsWith(Globals.JAR_URL_SEPARATOR)) {
				basePath = basePath.substring(0, basePath.length() - Globals.JAR_URL_SEPARATOR.length());
			}
			this.identifyKeyMap.put(basePath, bundleKey);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Group ID: {}, bundle: {}", bundleResource.getGroupId(), bundleResource.getBundle());
			}
		} catch (IOException e) {
			LOGGER.error("Register resource error! Path: {}", url.getPath());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(Globals.DEFAULT_VALUE_STRING, e);
			}
		}
	}

	@Override
	public void removeBundle(@Nonnull final String groupId, @Nonnull final String bundle) {
		String bundleKey = this.bundleKey(groupId, bundle);
		if (LocaleUtils.notBlank(bundleKey)) {
			this.registeredResources.remove(bundleKey);
		}
	}

	private MessageAgent newAgent(final String bundleKey) {
		if (LocaleUtils.isEmpty(bundleKey) || DEFAULT_BUNDLE_KEY.equalsIgnoreCase(bundleKey)) {
			return new EnhanceMessageAgentImpl(null, this.registeredResources.get(DEFAULT_BUNDLE_KEY));
		}
		return new EnhanceMessageAgentImpl(this.registeredResources.get(bundleKey),
				this.registeredResources.get(DEFAULT_BUNDLE_KEY));
	}

	/**
	 * <h3 class="en-US">Retrieve resource identify key by given class</h3>
	 * <h3 class="zh-CN">根据给定的类查找资源唯一识别码</h3>
	 *
	 * @param groupId <span class="en-US">Resource group id</span>
	 *                <span class="zh-CN">资源的组ID</span>
	 * @param bundle  <span class="en-US">Resource bundle</span>
	 *                <span class="zh-CN">资源的标识</span>
	 */
	private String bundleKey(final String groupId, final String bundle) {
		if (LocaleUtils.isEmpty(groupId) || LocaleUtils.isEmpty(bundle)) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		return groupId + ":" + bundle;
	}

	/**
	 * <h3 class="en-US">Retrieve resource identify key by given class</h3>
	 * <h3 class="zh-CN">根据给定的类查找资源唯一识别码</h3>
	 *
	 * @param clazz <span class="en-US">Class instance</span>
	 *              <span class="zh-CN">类实例对象</span>
	 */
	private String bundleKey(final Class<?> clazz) {
		String jarPath = URLDecoder.decode(clazz.getProtectionDomain().getCodeSource().getLocation().getFile(),
				Charset.defaultCharset());
		return this.identifyKeyMap.getOrDefault(jarPath, Globals.DEFAULT_VALUE_STRING);
	}

	private static final class EnhanceMessageAgentImpl extends MessageAgent {

		/**
		 * <span class="en-US">Resource information instance object</span>
		 * <span class="zh-CN">资源信息实例对象</span>
		 */
		private final MessageResource messageResource;
		/**
		 * <span class="en-US">Default resource information instance object</span>
		 * <span class="zh-CN">默认的资源信息实例对象</span>
		 */
		private final MessageResource defaultResource;

		EnhanceMessageAgentImpl(final MessageResource messageResource, final MessageResource defaultResource) {
			this.messageResource = messageResource;
			this.defaultResource = defaultResource;
		}

		@Override
		public String errorMessage(final long errorCode, final String languageCode, final Object... collections) {
			String messageKey = Optional.of(this.messageResource.errorKey(errorCode))
					.filter(LocaleUtils::notBlank)
					.orElse(this.defaultResource.errorKey(errorCode));
			if (LocaleUtils.isEmpty(messageKey)) {
				return MessageResource.identifyKey(errorCode, languageCode);
			}
			return this.findMessage(messageKey, languageCode, collections);
		}

		@Override
		public String findMessage(final String messageKey, final String languageCode, final Object... collections) {
			String message = Globals.DEFAULT_VALUE_STRING;
			if (this.messageResource != null) {
				message = this.messageResource.findMessage(messageKey, languageCode, LocaleUtils.defaultLanguage(), collections);
			}
			if (LocaleUtils.isEmpty(message) && this.defaultResource != null) {
				message = this.defaultResource.findMessage(messageKey, languageCode, LocaleUtils.defaultLanguage(), collections);
			}
			return LocaleUtils.isEmpty(message) ? MessageResource.identifyKey(messageKey, languageCode) : message;
		}
	}
}
