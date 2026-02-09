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

package org.nervousync.i18n;

import jakarta.annotation.Nonnull;

import java.net.URL;

/**
 * <h2 class="en-US">Multilingual provider interface</h2>
 * <h2 class="zh-CN">国际化适配器接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 19, 2023 16:39:41 $
 */
public interface MessageProvider {

	/**
	 * <h3 class="en-US">Register i18n message resource</h3>
	 * <h3 class="zh-CN">注册国际化信息资源</h3>
	 *
	 * @param url <span class="en-US">Internationalization resource data URL instance</span>
	 *            <span class="zh-CN">资源数据URL对象</span>
	 */
	void registerBundle(final URL url);

	/**
	 * <h3 class="en-US">Remove bundle resources by given argument bundle if registered.</h3>
	 * <h3 class="zh-CN">根据给定的参数 bundle 移除已注册的国际化信息资源</h3>
	 *
	 * @param groupId <span class="en-US">Resource group id</span>
	 *                <span class="zh-CN">资源的组ID</span>
	 * @param bundle  <span class="en-US">Resource bundle</span>
	 *                <span class="zh-CN">资源的标识</span>
	 */
	void removeBundle(@Nonnull final String groupId, @Nonnull final String bundle);

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
	MessageAgent newAgent(final String groupId, final String bundle);

	/**
	 * <h3 class="en-US">Generate multilingual agent instance</h3>
	 * <h3 class="zh-CN">生成国际化代理实例对象</h3>
	 *
	 * @param clazz <span class="en-US">Class instance</span>
	 *              <span class="zh-CN">类实例对象</span>
	 * @return <span class="en-US">Generated instance</span>
	 * <span class="zh-CN">生成的实例对象</span>
	 */
	MessageAgent newAgent(final Class<?> clazz);
}
