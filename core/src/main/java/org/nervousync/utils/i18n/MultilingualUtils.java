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
import org.nervousync.annotations.provider.Provider;
import org.nervousync.commons.Globals;
import org.nervousync.i18n.MessageAgent;
import org.nervousync.i18n.MessageProvider;
import org.nervousync.i18n.impl.DefaultMessageProviderImpl;

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * <h2 class="en-US">Internationalization Utilities</h2>
 * <h2 class="zh-CN">国际化工具集</h2>
 * .0
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 19, 2023 16:39:41 $
 */
@SuppressWarnings("unused")
public final class MultilingualUtils {

	/**
	 * <span class="en-US">Internationalization provider instance object</span>
	 * <span class="zh-CN">国际化适配器实例对象</span>
	 */
	private static final MessageProvider I18N_PROVIDER;

	static {
		Iterator<MessageProvider> iterator = ServiceLoader.load(MessageProvider.class).iterator();
		MessageProvider messageProvider = null;
		while (iterator.hasNext()) {
			messageProvider = iterator.next();
			if (messageProvider != null) {
				break;
			}
		}
		if (messageProvider == null) {
			messageProvider = new DefaultMessageProviderImpl();
		}
		I18N_PROVIDER = messageProvider;
	}
	/**
	 * <h3 class="en-US">Private constructor for MultilingualUtils</h3>
	 * <h3 class="zh-CN">国际化工具集的私有构造方法</h3>
	 */
	private MultilingualUtils() {
	}

	/**
	 * <h3 class="en-US">Generate multilingual agent instance</h3>
	 * <h3 class="zh-CN">生成国际化代理实例对象</h3>
	 *
	 * @param groupId <span class="en-US">Resource group id</span>
	 *                <span class="zh-CN">资源的组ID</span>
	 * @param bundle  <span class="en-US">Resource bundle</span>
	 *                <span class="zh-CN">资源的标识</span>
	 * @return <span class="en-US">Generated instance</span>
	 * <span class="zh-CN">生成的实例对象</span>
	 */
	public static MessageAgent newAgent(final String groupId, final String bundle) {
		return I18N_PROVIDER.newAgent(groupId, bundle);
	}

	/**
	 * <h3 class="en-US">Generate multilingual agent instance</h3>
	 * <h3 class="zh-CN">生成国际化代理实例对象</h3>
	 *
	 * @param clazz <span class="en-US">Class instance</span>
	 *              <span class="zh-CN">类实例对象</span>
	 * @return <span class="en-US">Generated instance</span>
	 * <span class="zh-CN">生成的实例对象</span>
	 */
	public static MessageAgent newAgent(final Class<?> clazz) {
		return I18N_PROVIDER.newAgent(clazz);
	}

	/**
	 * <h3 class="en-US">Remove bundle resources by given argument bundle if registered.</h3>
	 * <h3 class="zh-CN">根据给定的参数 bundle 移除已注册的国际化信息资源</h3>
	 *
	 * @param groupId <span class="en-US">Resource group id</span>
	 *                <span class="zh-CN">资源的组ID</span>
	 * @param bundle  <span class="en-US">Resource bundle</span>
	 *                <span class="zh-CN">资源的标识</span>
	 */
	public static void removeBundle(@Nonnull final String groupId, @Nonnull final String bundle) {
		I18N_PROVIDER.removeBundle(groupId, bundle);
	}

	/**
	 * <h3 class="en-US">Get the name of the given adapter implementation class</h3>
	 * <h3 class="zh-CN">获取给定适配器实现类的名称</h3>
	 *
	 * @param clazz <span class="en-US">Provider implements class</span>
	 *              <span class="zh-CN">适配器实现类</span>
	 * @return <span class="en-US">The name of provider</span>
	 * <span class="zh-CN">适配器名称</span>
	 */
	public static String providerName(@Nonnull final Class<?> clazz) {
		return providerName(clazz, LocaleUtils.languageCode(Globals.DEFAULT_LOCALE));
	}

	/**
	 * <h3 class="en-US">Get the name of the given adapter implementation class</h3>
	 * <h3 class="zh-CN">获取给定适配器实现类的名称</h3>
	 *
	 * @param clazz        <span class="en-US">Provider implements class</span>
	 *                     <span class="zh-CN">适配器实现类</span>
	 * @param languageCode <span class="en-US">Language code</span>
	 *                     <span class="zh-CN">语言代码</span>
	 * @return <span class="en-US">The name of provider</span>
	 * <span class="zh-CN">适配器名称</span>
	 */
	public static String providerName(@Nonnull final Class<?> clazz, final String languageCode) {
		return Optional.ofNullable(clazz.getAnnotation(Provider.class))
				.filter(provider -> LocaleUtils.notBlank(provider.titleKey()))
				.map(provider -> newAgent(clazz).findMessage(provider.titleKey(), languageCode))
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Get the description of the given adapter implementation class</h3>
	 * <h3 class="zh-CN">获取给定适配器实现类的简介</h3>
	 *
	 * @param clazz <span class="en-US">Provider implements class</span>
	 *              <span class="zh-CN">适配器实现类</span>
	 * @return <span class="en-US">The name of provider</span>
	 * <span class="zh-CN">适配器名称</span>
	 */
	public static String providerDescription(@Nonnull final Class<?> clazz) {
		return providerDescription(clazz, LocaleUtils.languageCode(Globals.DEFAULT_LOCALE));
	}

	/**
	 * <h3 class="en-US">Get the description of the given adapter implementation class</h3>
	 * <h3 class="zh-CN">获取给定适配器实现类的简介</h3>
	 *
	 * @param clazz        <span class="en-US">Provider implements class</span>
	 *                     <span class="zh-CN">适配器实现类</span>
	 * @param languageCode <span class="en-US">Language code</span>
	 *                     <span class="zh-CN">语言代码</span>
	 * @return <span class="en-US">The name of provider</span>
	 * <span class="zh-CN">适配器名称</span>
	 */
	public static String providerDescription(@Nonnull final Class<?> clazz, final String languageCode) {
		return Optional.ofNullable(clazz.getAnnotation(Provider.class))
				.filter(provider -> LocaleUtils.notBlank(provider.descriptionKey()))
				.map(provider -> newAgent(clazz).findMessage(provider.descriptionKey(), languageCode))
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}
}
