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

package org.nervousync.beans.converter;

import jakarta.annotation.Nonnull;
import org.nervousync.annotations.beans.OutputConfig;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.beans.StringType;
import org.nervousync.utils.core.ClassUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * <h2 class="en-US">JavaBean converter interface</h2>
 * <h2 class="zh-CN">JavaBean 转换适配器接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.3.0 $ $Date: Jan 18, 2026 14:55:15 $
 */
public interface BeanConverter {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(BeanConverter.class);

	/**
	 * <h3 class="en-US">Convenience method to return a JavaBean object as a string. </h3>
	 * <h3 class="zh-CN">将 JavaBean 实例对象转换为字符串</h3>
	 *
	 * @param object       <span class="en-US">JavaBean object</span>
	 *                     <span class="zh-CN">JavaBean实例对象</span>
	 * @param stringType   <span class="en-US">Target string type</span>
	 *                     <span class="zh-CN">目标字符串类型</span>
	 * @param formatOutput <span class="en-US">format output string</span>
	 *                     <span class="zh-CN">格式化输出字符串</span>
	 * @param encoding     <span class="en-US">String charset encoding</span>
	 *                     <span class="zh-CN">字符串的字符集编码</span>
	 * @return <span class="en-US">the converted string</span>
	 * <span class="zh-CN">转换后的字符串</span>
	 */
	String objectToString(final Object object, final StringType stringType, final boolean formatOutput,
	                      final boolean outputFragment, final String encoding);

	/**
	 * <h3 class="en-US">Parse strings to target JavaBean instance. </h3>
	 * <h3 class="zh-CN">解析字符串为目标 JavaBean 实例对象</h3>
	 *
	 * @param <T>         <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param string      <span class="en-US">The string will parse</span>
	 *                    <span class="zh-CN">要解析的字符串</span>
	 * @param stringType  <span class="en-US">The string type</span>
	 *                    <span class="zh-CN">字符串类型</span>
	 * @param encoding    <span class="en-US">String charset encoding</span>
	 *                    <span class="zh-CN">字符串的字符集编码</span>
	 * @param beanClass   <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param schemaPaths <span class="en-US">XML schema path(Maybe schema uri or local path)</span>
	 *                    <span class="zh-CN">XML描述文件路径（可能为描述文件URI或本地文件路径）</span>
	 * @return <span class="en-US">Converted object instance</span>
	 * <span class="zh-CN">转换后的实例对象</span>
	 */
	default <T> T stringToObject(final String string, final StringType stringType, final String encoding,
	                     final Class<T> beanClass, final String... schemaPaths) {
		if (StringUtils.isEmpty(string)) {
			LOGGER.error("Parse_Empty_String_Error");
			return null;
		}

		String charsetEncoding =
				Optional.ofNullable(beanClass.getAnnotation(OutputConfig.class))
						.map(OutputConfig::encoding)
						.orElse(encoding);
		if (StringUtils.isEmpty(charsetEncoding)) {
			charsetEncoding = Globals.DEFAULT_ENCODING;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Parse_String_Debug", string, charsetEncoding, beanClass.getName());
		}

		if (StringType.SIMPLE.equals(stringType)) {
			return ClassUtils.parseSimpleData(string, beanClass);
		}
		try (InputStream inputStream = new ByteArrayInputStream(string.getBytes(charsetEncoding))) {
			return this.streamToObject(inputStream, stringType, encoding, beanClass, schemaPaths);
		} catch (IOException e) {
			LOGGER.error("Parse_String_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return null;
	}

	/**
	 * <h3 class="en-US">Parse the input stream instance to target JavaBean instance.</h3>
	 * <h3 class="zh-CN">解析输入流对象实例为目标 JavaBean 实例对象</h3>
	 *
	 * @param <T>         <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param inputStream <span class="en-US">Input stream instance</span>
	 *                    <span class="zh-CN">输入流对象实例</span>
	 * @param stringType  <span class="en-US">The string type</span>
	 *                    <span class="zh-CN">字符串类型</span>
	 * @param encoding    <span class="en-US">String charset encoding</span>
	 *                    <span class="zh-CN">字符串的字符集编码</span>
	 * @param beanClass   <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param schemaPaths <span class="en-US">XML schema path(Maybe schema uri or local path)</span>
	 *                    <span class="zh-CN">XML描述文件路径（可能为描述文件URI或本地文件路径）</span>
	 * @return <span class="en-US">Converted object instance</span>
	 * <span class="zh-CN">转换后的实例对象</span>
	 */
	<T> T streamToObject(@Nonnull final InputStream inputStream, final StringType stringType, final String encoding,
	                     final Class<T> beanClass, final String... schemaPaths);

	/**
	 * <h3 class="en-US">Parse strings to target JavaBean instance list. </h3>
	 * <h3 class="zh-CN">解析字符串为目标JavaBean实例对象列表</h3>
	 *
	 * @param <T>        <span class="en-US">target JavaBean class</span>
	 *                   <span class="zh-CN">目标JavaBean类</span>
	 * @param string     <span class="en-US">The string will parse</span>
	 *                   <span class="zh-CN">要解析的字符串</span>
	 * @param stringType <span class="en-US">The string type</span>
	 *                   <span class="zh-CN">字符串类型</span>
	 * @param encoding   <span class="en-US">String charset encoding</span>
	 *                   <span class="zh-CN">字符串的字符集编码</span>
	 * @param beanClass  <span class="en-US">target JavaBean class</span>
	 *                   <span class="zh-CN">目标JavaBean类</span>
	 * @return <span class="en-US">Converted object instance list</span>
	 * <span class="zh-CN">转换后的实例对象列表</span>
	 */
	default <T> List<T> stringToList(final String string, final StringType stringType, final String encoding,
	                         final Class<T> beanClass) {
		if (StringUtils.isEmpty(string)) {
			LOGGER.error("Parse_Empty_String_Error");
			return null;
		}
		try (InputStream inputStream = new ByteArrayInputStream(string.getBytes(StringUtils.isEmpty(encoding) ? Globals.DEFAULT_ENCODING : encoding))) {
			return this.streamToList(inputStream, stringType, encoding, beanClass);
		} catch (Exception e) {
			LOGGER.error("Parse_String_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return new ArrayList<>();
		}
	}

	/**
	 * <h3 class="en-US">Parse the input stream instance to target JavaBean instance list. </h3>
	 * <h3 class="zh-CN">解析输入流对象实例为目标 JavaBean 实例对象列表</h3>
	 *
	 * @param <T>         <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param inputStream <span class="en-US">Input stream instance</span>
	 *                    <span class="zh-CN">输入流对象实例</span>
	 * @param stringType  <span class="en-US">The string type</span>
	 *                    <span class="zh-CN">字符串类型</span>
	 * @param encoding    <span class="en-US">String charset encoding</span>
	 *                    <span class="zh-CN">字符串的字符集编码</span>
	 * @param beanClass   <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @return <span class="en-US">Converted object instance list</span>
	 * <span class="zh-CN">转换后的实例对象列表</span>
	 */
	<T> List<T> streamToList(@Nonnull final InputStream inputStream, final StringType stringType, final String encoding,
	                         final Class<T> beanClass);

	/**
	 * <h3 class="en-US">Parse string to data map.</h3>
	 * <h3 class="zh-CN">解析字符串为数据映射表</h3>
	 *
	 * @param string     <span class="en-US">The string will parse</span>
	 *                   <span class="zh-CN">要解析的字符串</span>
	 * @param stringType <span class="en-US">The string type</span>
	 *                   <span class="zh-CN">字符串类型</span>
	 * @param encoding    <span class="en-US">String charset encoding</span>
	 *                    <span class="zh-CN">字符串的字符集编码</span>
	 * @return <span class="en-US">Converted data map</span>
	 * <span class="zh-CN">转换后的数据映射表</span>
	 */
	default Map<String, Object> stringToMap(final String string, final StringType stringType, final String encoding) {
		if (StringUtils.isEmpty(string)) {
			LOGGER.error("Parse_Empty_String_Error");
			return null;
		}
		try (InputStream inputStream = new ByteArrayInputStream(string.getBytes(StringUtils.isEmpty(encoding) ? Globals.DEFAULT_ENCODING : encoding))) {
			return streamToMap(inputStream, stringType, encoding);
		} catch (IOException e) {
			LOGGER.error("Parse_String_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return new HashMap<>();
		}
	}

	/**
	 * <h3 class="en-US">Parse the input stream instance to data map.</h3>
	 * <h3 class="zh-CN">解析输入流对象实例为数据映射表</h3>
	 *
	 * @param inputStream <span class="en-US">Input stream instance</span>
	 *                    <span class="zh-CN">输入流对象实例</span>
	 * @param stringType  <span class="en-US">The string type</span>
	 *                    <span class="zh-CN">字符串类型</span>
	 * @param encoding    <span class="en-US">String charset encoding</span>
	 *                    <span class="zh-CN">字符串的字符集编码</span>
	 * @return <span class="en-US">Converted data map</span>
	 * <span class="zh-CN">转换后的数据映射表</span>
	 */
	Map<String, Object> streamToMap(@Nonnull final InputStream inputStream, final StringType stringType,
	                                final String encoding);

	/**
	 * <h3 class="en-US">Convert Markdown string to HTML code</h3>
	 * <h3 class="zh-CN">转换 Markdown 字符串为 HTML 代码</h3>
	 *
	 * @param markdown <span class="en-US">Markdown string</span>
	 *                 <span class="zh-CN">Markdown字符串</span>
	 * @return <span class="en-US">Converted HTML code</span>
	 * <span class="zh-CN">转换后的HTML代码</span>
	 */
	String mdToHtml(final String markdown);
}
