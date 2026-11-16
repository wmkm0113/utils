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

import org.nervousync.commons.Globals;
import org.nervousync.utils.i18n.LocaleUtils;

import java.util.Locale;

/**
 * <h2 class="en-US">Multilingual Agent</h2>
 * <h2 class="zh-CN">国际化信息代理</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 19, 2023 16:39:41 $
 */
@SuppressWarnings("unused")
public abstract class MessageAgent {


	/**
	 * <h3 class="en-US">Retrieve internationalization information content and formatted by given collections</h3>
	 * <h3 class="zh-CN">读取国际化资源信息详情并使用给定的参数集合格式化资源信息</h3>
	 *
	 * @param errorCode   <span class="en-US">Error code</span>
	 *                    <span class="zh-CN">错误代码</span>
	 * @param collections <span class="en-US">given parameters of information formatter</span>
	 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
	 * @return <span class="en-US">Formatted resource information or joined string by character '/' if not found</span>
	 * <span class="zh-CN">格式化的资源信息，如果未找到则返回使用 '/' 拼接的字符串</span>
	 */
	public String errorMessage(final long errorCode, final Object... collections) {
		return this.errorMessage(errorCode, Globals.DEFAULT_VALUE_STRING, collections);
	}

	/**
	 * <h3 class="en-US">Retrieve internationalization information content and formatted by given collections</h3>
	 * <h3 class="zh-CN">读取国际化资源信息详情并使用给定的参数集合格式化资源信息</h3>
	 *
	 * @param errorCode    <span class="en-US">Error code</span>
	 *                     <span class="zh-CN">错误代码</span>
	 * @param languageCode <span class="en-US">Language code</span>
	 *                     <span class="zh-CN">语言代码</span>
	 * @param collections  <span class="en-US">given parameters of information formatter</span>
	 *                     <span class="zh-CN">用于资源信息格式化的参数</span>
	 * @return <span class="en-US">Formatted resource information or joined string by character '/' if not found</span>
	 * <span class="zh-CN">格式化的资源信息，如果未找到则返回使用 '/' 拼接的字符串</span>
	 */
	public abstract String errorMessage(final long errorCode, final String languageCode, final Object... collections);

	/**
	 * <h3 class="en-US">Retrieve internationalization information content and formatted by given collections</h3>
	 * <h3 class="zh-CN">读取国际化资源信息详情并使用给定的参数集合格式化资源信息</h3>
	 *
	 * @param messageKey   <span class="en-US">Message identify key</span>
	 *                     <span class="zh-CN">信息识别键值</span>
	 * @param collections  <span class="en-US">given parameters of information formatter</span>
	 *                     <span class="zh-CN">用于资源信息格式化的参数</span>
	 * @return <span class="en-US">Formatted resource information or joined string by character '/' if not found</span>
	 * <span class="zh-CN">格式化的资源信息，如果未找到则返回使用 '/' 拼接的字符串</span>
	 */
	public final String findMessage(final String messageKey, final Object... collections) {
		return this.findMessage(messageKey, LocaleUtils.defaultLanguage(), collections);
	}

	/**
	 * <h3 class="en-US">Retrieve internationalization information content and formatted by given collections</h3>
	 * <h3 class="zh-CN">读取国际化资源信息详情并使用给定的参数集合格式化资源信息</h3>
	 *
	 * @param messageKey   <span class="en-US">Message identify key</span>
	 *                     <span class="zh-CN">信息识别键值</span>
	 * @param languageCode <span class="en-US">Language code</span>
	 *                     <span class="zh-CN">语言代码</span>
	 * @param collections  <span class="en-US">given parameters of information formatter</span>
	 *                     <span class="zh-CN">用于资源信息格式化的参数</span>
	 * @return <span class="en-US">Formatted resource information or joined string by character '/' if not found</span>
	 * <span class="zh-CN">格式化的资源信息，如果未找到则返回使用 '/' 拼接的字符串</span>
	 */
	public abstract String findMessage(final String messageKey, final String languageCode, final Object... collections);

	/**
	 * <h3 class="en-US">Retrieve internationalization information content and formatted by given collections</h3>
	 * <h3 class="zh-CN">读取国际化资源信息详情并使用给定的参数集合格式化资源信息</h3>
	 *
	 * @param messageKey  <span class="en-US">Message identify key</span>
	 *                    <span class="zh-CN">信息识别键值</span>
	 * @param locale      <span class="en-US">locale instance</span>
	 *                    <span class="zh-CN">区域设置实例</span>
	 * @param collections <span class="en-US">given parameters of information formatter</span>
	 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
	 * @return <span class="en-US">Formatted resource information or joined string by character '/' if not found</span>
	 * <span class="zh-CN">格式化的资源信息，如果未找到则返回使用 '/' 拼接的字符串</span>
	 */
	public final String findMessage(final String messageKey, final Locale locale, final Object... collections) {
		return this.findMessage(messageKey, LocaleUtils.languageCode(locale), collections);
	}
}
