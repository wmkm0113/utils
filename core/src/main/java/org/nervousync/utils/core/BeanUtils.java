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

package org.nervousync.utils.core;

import jakarta.annotation.Nonnull;
import org.nervousync.annotations.beans.BeanProperty;
import org.nervousync.annotations.beans.OutputConfig;
import org.nervousync.beans.config.TransferConfig;
import org.nervousync.beans.converter.BeanConverter;
import org.nervousync.beans.converter.impl.DefaultBeanConverterImpl;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.beans.StringType;
import org.nervousync.utils.logger.LoggerUtils;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * <h2 class="en-US">JavaBean Utilities</h2>
 * <span class="en-US">
 * <span>Current utilities implements features:</span>
 *     <ul>Copy object fields value from source object to target object-based field name</ul>
 *     <ul>Copy object fields value from the source object array to target object-based annotation: BeanProperty</ul>
 *     <ul>Copy object fields value from source object to target object arrays based annotation: BeanProperty</ul>
 * </span>
 * <h2 class="zh-CN">JavaBean工具集</h2>
 * <span class="zh-CN">
 *     <span>此工具集实现以下功能:</span>
 *     <ul>根据属性名称从源数据对象复制数据到目标数据对象</ul>
 *     <ul>根据BeanProperty注解从源数据对象数组复制数据到目标数据对象</ul>
 *     <ul>根据BeanProperty注解从源数据对象复制数据到目标数据对象数组</ul>
 * </span>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.2.0 $ $Date: Jun 25, 2015 14:55:15 $
 */
public final class BeanUtils {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(BeanUtils.class);
	/**
	 * <span class="en-US">Registered JavaBean mappings</span>
	 * <span class="zh-CN">已注册的JavaBean映射</span>
	 */
	private static final Map<String, BeanMapping> BEAN_CONFIG_MAP = new HashMap<>();
	/**
	 * <span class="en-US">XML fragment template</span>
	 * <span class="zh-CN">XML声明模板</span>
	 */
	public static final String FRAGMENT_TEMPLATE = "<?xml version=\"1.0\" encoding=\"{}\"?>";
	/**
	 * <span class="en-US">XML Schema file mapping resource path</span>
	 * <span class="zh-CN">XML约束文档的资源映射文件</span>
	 */
	public static final String SCHEMA_MAPPING_RESOURCE_PATH = "META-INF/nervousync.schemas";
	/**
	 * <span class="en-US">Registered schema mapping</span>
	 * <span class="zh-CN">注册的约束文档与资源文件的映射</span>
	 */
	private static final Map<String, String> SCHEMA_MAPPING = new HashMap<>();
	/**
	 * <span class="en-US">JavaBean converter implement class instance object</span>
	 * <span class="zh-CN">JavaBean 转换适配器实现类实例对象</span>
	 */
	private static final BeanConverter BEAN_CONVERTER =
			ServiceLoader.load(BeanConverter.class).findFirst().orElse(new DefaultBeanConverterImpl());

	static {
		try {
			ClassUtils.getDefaultClassLoader().getResources(SCHEMA_MAPPING_RESOURCE_PATH)
					.asIterator()
					.forEachRemaining(BeanUtils::REGISTER_SCHEMA);
		} catch (IOException e) {
			LOGGER.error("Load_Schema_Mapping_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
	}

	/**
	 * <h3 class="en-US">Remove registered JavaBean class</h3>
	 * <h3 class="zh-CN">移除已注册的JavaBean类映射</h3>
	 *
	 * @param classes <span class="en-US">Want removed JavaBean class array</span>
	 *                <span class="zh-CN">需要移除的JavaBean类数组</span>
	 */
	public static void removeBeanConfig(final Class<?>... classes) {
		Arrays.asList(classes).forEach(clazz -> BEAN_CONFIG_MAP.remove(ClassUtils.originalClassName(clazz)));
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Register_Bean_Config_Count_Debug", BEAN_CONFIG_MAP.size());
		}
	}

	/**
	 * <h3 class="en-US">Copy the property values from the source object to the target object, based field name</h3>
	 * <h3 class="zh-CN">从源数据对象复制数据到目标对象，复制依据属性名称</h3>
	 *
	 * @param sourceObject <span class="en-US">Source object instance</span>
	 *                     <span class="zh-CN">源数据对象</span>
	 * @param targetObject <span class="en-US">Target object instance</span>
	 *                     <span class="zh-CN">目标数据对象</span>
	 */
	public static void copyData(final Object sourceObject, final Object targetObject) {
		ReflectionUtils.getAllDeclaredFields(sourceObject.getClass(), Boolean.TRUE)
				.forEach(field ->
						Optional.ofNullable(ReflectionUtils.getFieldValue(field, sourceObject))
								.ifPresent(fieldValue ->
										ReflectionUtils.setField(field.getName(), targetObject, fieldValue)));
	}

	/**
	 * <h3 class="en-US">Copy the map values into the target JavaBean instance</h3>
	 * <p class="en-US">Data mapping to JavaBean field identified by map key</p>
	 * <h3 class="zh-CN">复制Map中的值到目标JavaBean实例</h3>
	 * <p class="zh-CN">数据使用Map的键值映射到JavaBean属性</p>
	 *
	 * @param originalMap  <span class="en-US">Original data map</span>
	 *                     <span class="zh-CN">来源数据Map</span>
	 * @param targetObject <span class="en-US">Target JavaBean instance</span>
	 *                     <span class="zh-CN">目标JavaBean实例</span>
	 */
	public static void copyData(final Map<String, Object> originalMap, final Object targetObject) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Data_Map_Debug", BeanUtils.objectToString(originalMap));
		}
		checkRegister(targetObject.getClass());
		Optional.ofNullable(BEAN_CONFIG_MAP.get(ClassUtils.originalClassName(targetObject.getClass())))
				.ifPresent(beanMapping -> beanMapping.copyData(targetObject, originalMap));
	}

	/**
	 * <h3 class="en-US">Copy the property values of the given source bean arrays into the target bean</h3>
	 * <p class="en-US">Data mapping by the field annotated with org.nervousync.annotations.beans.BeanProperty</p>
	 * <h3 class="zh-CN">从给定的JavaBean对象数组复制映射的属性值到目标对象</h3>
	 * <p class="en-US">数据映射配置于使用org.nervousync.annotations.beans.BeanProperty注解的属性</p>
	 *
	 * @param targetObject    <span class="en-US">Target JavaBean instance</span>
	 *                        <span class="zh-CN">目标JavaBean实例</span>
	 * @param originalObjects <span class="en-US">Original JavaBean instance array</span>
	 *                        <span class="zh-CN">数据源JavaBean实例数组</span>
	 */
	public static void copyFrom(final Object targetObject, final Object... originalObjects) {
		if (targetObject == null || originalObjects.length == 0) {
			return;
		}
		checkRegister(targetObject.getClass());
		Optional.ofNullable(BEAN_CONFIG_MAP.get(ClassUtils.originalClassName(targetObject.getClass())))
				.ifPresent(beanMapping -> beanMapping.unmarshall(targetObject, originalObjects));
	}

	/**
	 * <h3 class="en-US">Copy the property values into the given target JavaBean instance arrays</h3>
	 * <p class="en-US">Data mapping by the field annotated with org.nervousync.annotations.beans.Mappings</p>
	 * <h3 class="zh-CN">从源对象复制属性值到给定的JavaBean对象数组</h3>
	 * <p class="en-US">数据映射配置于使用org.nervousync.annotations.beans.Mappings注解的属性</p>
	 *
	 * @param originalObject <span class="en-US">Original JavaBean instance</span>
	 *                       <span class="zh-CN">数据源JavaBean实例</span>
	 * @param targetObjects  <span class="en-US">Target JavaBean instance array</span>
	 *                       <span class="zh-CN">目标JavaBean实例数组</span>
	 */
	public static void copyTo(final Object originalObject, final Object... targetObjects) {
		if (originalObject == null || targetObjects.length == 0) {
			return;
		}
		checkRegister(originalObject.getClass());
		Optional.ofNullable(BEAN_CONFIG_MAP.get(ClassUtils.originalClassName(originalObject.getClass())))
				.ifPresent(beanMapping -> beanMapping.marshall(originalObject, targetObjects));
	}

	/**
	 * <h3 class="en-US">Get the charset encoding</h3>
	 * <h3 class="zh-CN">获取编码集</h3>
	 *
	 * @param beanClass   <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @return <span class="en-US">Converted object instance</span>
	 * <span class="zh-CN">转换后的实例对象</span>
	 */
	public static String encoding(@Nonnull final Class<?> beanClass) {
		return Optional.ofNullable(beanClass.getAnnotation(OutputConfig.class))
				.map(OutputConfig::encoding)
				.orElse(Globals.DEFAULT_ENCODING);
	}

	/**
	 * <h3 class="en-US">Get the output data type of the given type</h3>
	 * <h3 class="zh-CN">获取给定类型的输出数据类型</h3>
	 *
	 * @param clazz <span class="en-US">Given class instance</span>
	 *              <span class="zh-CN">给定的类对象</span>
	 * @return <span class="en-US">Output data type</span>
	 * <span class="zh-CN">输出数据类型</span>
	 */
	public static StringType type(@Nonnull final Class<?> clazz) {
		return Optional.ofNullable(clazz.getAnnotation(OutputConfig.class))
				.map(OutputConfig::type)
				.orElseGet(() -> {
					if (ClassUtils.simpleDataType(clazz)) {
						return StringType.SIMPLE;
					}
					if (ClassUtils.isAssignable(Map.class, clazz) || ClassUtils.isAssignable(Collection.class, clazz)) {
						return StringType.JSON;
					}
					return StringType.SERIALIZABLE;
				});
	}

	/**
	 * <h3 class="en-US">Convenience method to return a JavaBean object as a string. </h3>
	 * <h3 class="zh-CN">将 JavaBean 实例对象转换为字符串</h3>
	 *
	 * @param object     <span class="en-US">JavaBean object</span>
	 *                   <span class="zh-CN">JavaBean实例对象</span>
	 * @return <span class="en-US">the converted string</span>
	 * <span class="zh-CN">转换后的字符串</span>
	 */
	public static String objectToString(final Object object) {
		return BEAN_CONVERTER.objectToString(object);
	}

	/**
	 * <h3 class="en-US">Parse strings to target JavaBean instance. </h3>
	 * <h3 class="zh-CN">解析字符串为目标 JavaBean 实例对象</h3>
	 *
	 * @param <T>         <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param string      <span class="en-US">The string will parse</span>
	 *                    <span class="zh-CN">要解析的字符串</span>
	 * @param beanClass   <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param schemaPaths <span class="en-US">XML schema path(Maybe schema uri or local path)</span>
	 *                    <span class="zh-CN">XML描述文件路径（可能为描述文件URI或本地文件路径）</span>
	 * @return <span class="en-US">Converted object instance</span>
	 * <span class="zh-CN">转换后的实例对象</span>
	 */
	public static <T> T stringToObject(final String string, final Class<T> beanClass, final String... schemaPaths) {
		if (StringUtils.isEmpty(string)) {
			LOGGER.error("Parse_Empty_String_Error");
			return null;
		}
		String encoding = encoding(beanClass);
		try (InputStream inputStream = new ByteArrayInputStream(string.getBytes(encoding))) {
			return streamToObject(inputStream, type(beanClass), encoding, beanClass, schemaPaths);
		} catch (IOException e) {
			LOGGER.error("Parse_String_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return null;
	}

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
	 * @param encoding  <span class="en-US">String charset encoding</span>
	 *                  <span class="zh-CN">字符串的字符集编码</span>
	 * @param beanClass   <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param schemaPaths <span class="en-US">XML schema path(Maybe schema uri or local path)</span>
	 *                    <span class="zh-CN">XML描述文件路径（可能为描述文件URI或本地文件路径）</span>
	 * @return <span class="en-US">Converted object instance</span>
	 * <span class="zh-CN">转换后的实例对象</span>
	 */
	public static <T> T stringToObject(final String string, final StringType stringType, final String encoding,
	                                   final Class<T> beanClass, final String... schemaPaths) {
		if (StringUtils.isEmpty(string)) {
			LOGGER.error("Parse_Empty_String_Error");
			return null;
		}

		String charsetEncoding = StringUtils.isEmpty(encoding) ? encoding(beanClass) : encoding;
		try (InputStream inputStream = new ByteArrayInputStream(string.getBytes(charsetEncoding))) {
			return streamToObject(inputStream, stringType, charsetEncoding, beanClass, schemaPaths);
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
	 * @param encoding  <span class="en-US">String charset encoding</span>
	 *                  <span class="zh-CN">字符串的字符集编码</span>
	 * @param beanClass   <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @param schemaPaths <span class="en-US">XML schema path(Maybe schema uri or local path)</span>
	 *                    <span class="zh-CN">XML描述文件路径（可能为描述文件URI或本地文件路径）</span>
	 * @return <span class="en-US">Converted object instance</span>
	 * <span class="zh-CN">转换后的实例对象</span>
	 */
	public static <T> T streamToObject(@Nonnull final InputStream inputStream, final StringType stringType,
	                                   final String encoding, final Class<T> beanClass, final String... schemaPaths) {
		return BEAN_CONVERTER.streamToObject(inputStream, stringType, encoding, beanClass, schemaPaths);
	}

	/**
	 * <h3 class="en-US">Parse strings to target JavaBean instance list. </h3>
	 * <h3 class="zh-CN">解析字符串为目标JavaBean实例对象列表</h3>
	 *
	 * @param <T>        <span class="en-US">target JavaBean class</span>
	 *                   <span class="zh-CN">目标JavaBean类</span>
	 * @param string     <span class="en-US">The string will parse</span>
	 *                   <span class="zh-CN">要解析的字符串</span>
	 * @param stringType  <span class="en-US">The string type</span>
	 *                    <span class="zh-CN">字符串类型</span>
	 * @param encoding  <span class="en-US">String charset encoding</span>
	 *                  <span class="zh-CN">字符串的字符集编码</span>
	 * @param beanClass  <span class="en-US">target JavaBean class</span>
	 *                   <span class="zh-CN">目标JavaBean类</span>
	 * @return <span class="en-US">Converted object instance list</span>
	 * <span class="zh-CN">转换后的实例对象列表</span>
	 */
	public static <T> List<T> stringToList(final String string, final StringType stringType,
	                                       final String encoding, final Class<T> beanClass) {
		if (StringUtils.isEmpty(string)) {
			LOGGER.warn("Parse_Empty_String_Error");
			return Collections.emptyList();
		}
		try (InputStream inputStream = new ByteArrayInputStream(string.getBytes(StringUtils.isEmpty(encoding) ? Globals.DEFAULT_ENCODING : encoding))) {
			return streamToList(inputStream, stringType, encoding, beanClass);
		} catch (Exception e) {
			LOGGER.error("Parse_String_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Collections.emptyList();
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
	 * @param beanClass   <span class="en-US">target JavaBean class</span>
	 *                    <span class="zh-CN">目标JavaBean类</span>
	 * @return <span class="en-US">Converted object instance list</span>
	 * <span class="zh-CN">转换后的实例对象列表</span>
	 */
	public static <T> List<T> streamToList(@Nonnull final InputStream inputStream, final StringType stringType,
	                                       final String encoding, final Class<T> beanClass) {
		return BEAN_CONVERTER.streamToList(inputStream, stringType, encoding, beanClass);
	}

	/**
	 * <h3 class="en-US">Parse string to data map.</h3>
	 * <h3 class="zh-CN">解析字符串为数据映射表</h3>
	 *
	 * @param string     <span class="en-US">The string will parse</span>
	 *                   <span class="zh-CN">要解析的字符串</span>
	 * @param stringType <span class="en-US">The string type</span>
	 *                   <span class="zh-CN">字符串类型</span>
	 * @param encoding   <span class="en-US">String charset encoding</span>
	 *                   <span class="zh-CN">字符串的字符集编码</span>
	 * @return <span class="en-US">Converted data map</span>
	 * <span class="zh-CN">转换后的数据映射表</span>
	 */
	public static Map<String, Object> stringToMap(final String string, final StringType stringType, final String encoding) {
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
	public static Map<String, Object> streamToMap(@Nonnull final InputStream inputStream, final StringType stringType,
	                                              final String encoding) {
		return BEAN_CONVERTER.streamToMap(inputStream, stringType, encoding);
	}

	/**
	 * <h3 class="en-US">Verify that the digital signature is legitimate</h3>
	 * <h3 class="zh-CN">验证数字签名是否合法</h3>
	 *
	 * @param object <span class="en-US">JavaBean instance object which need generate signature</span>
	 *               <span class="zh-CN">需要生成数字签名的 JavaBean 实例对象</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	public static boolean validate(final Object object) {
		return BEAN_CONVERTER.validate(object);
	}

	/**
	 * <h3 class="en-US">Convert Markdown string to HTML code</h3>
	 * <h3 class="zh-CN">转换 Markdown 字符串为 HTML 代码</h3>
	 *
	 * @param markdown <span class="en-US">Markdown string</span>
	 *                 <span class="zh-CN">Markdown字符串</span>
	 * @return <span class="en-US">Converted HTML code</span>
	 * <span class="zh-CN">转换后的HTML代码</span>
	 */
	public static String mdToHtml(final String markdown) {
		return BEAN_CONVERTER.mdToHtml(markdown);
	}

	/**
	 * <h3 class="en-US">Generate a Schema instance object according to the given XML description file path.</h3>
	 * <h3 class="zh-CN">根据给定的XML描述文件路径，生成Schema实例对象</h3>
	 *
	 * @param schemaPaths <span class="en-US">XML schema path(Maybe schema uri or local path)</span>
	 *                    <span class="zh-CN">XML描述文件路径（可能为描述文件URI或本地文件路径）</span>
	 * @return <span class="en-US">Generated Schema instance</span>
	 * <span class="zh-CN">生成的Schema实例对象</span>
	 */
	public static Schema newSchema(final String... schemaPaths) {
		if (CollectionUtils.isEmpty(schemaPaths)) {
			return null;
		}
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver(new SchemaResourceResolver());
		try {
			Source[] sources = new Source[schemaPaths.length];
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			docFactory.setNamespaceAware(Boolean.TRUE);
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			for (int i = 0; i < schemaPaths.length; i++) {
				String locationPath = SCHEMA_MAPPING.getOrDefault(schemaPaths[i], schemaPaths[i]);
				InputStream in = FileUtils.loadFile(locationPath);
				Document document = docBuilder.parse(in);
				sources[i] = new DOMSource(document, locationPath);
				IOUtils.closeStream(in);
			}
			return schemaFactory.newSchema(sources);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOGGER.error("Load_Schemas_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return null;
	}

	/**
	 * <h3 class="en-US">Check and register JavaBean mapping configs</h3>
	 * <p class="en-US">
	 * If given JavaBean class instance not registered,
	 * generate BeanMapping instance and register the given JavaBean class mapping configured
	 * </p>
	 * <h3 class="zh-CN">检查并注册JavaBean映射配置</h3>
	 * <p class="en-US">如果给定的JavaBean类没有注册映射配置，则生成映射配置对象，并执行注册</p>
	 *
	 * @param clazz <span class="en-US">Given JavaBean class instance</span>
	 *              <span class="zh-CN">给定的JavaBean类对象</span>
	 */
	private static void checkRegister(final Class<?> clazz) {
		Optional.of(ClassUtils.originalClassName(clazz))
				.filter(StringUtils::notBlank)
				.filter(className -> !BEAN_CONFIG_MAP.containsKey(className))
				.ifPresent(className -> BEAN_CONFIG_MAP.put(className, new BeanMapping(clazz)));
	}

	/**
	 * <h2 class="en-US">JavaBean mapping configure define</h2>
	 * <p class="en-US">Private inner class for define JavaBean mapping configure</p>
	 * <h2 class="zh-CN">JavaBean映射配置定义</h2>
	 * <p class="zh-CN">定义JavaBean映射配置的私有内部类</p>
	 */
	private static final class BeanMapping {
		/**
		 * <span class="en-US">JavaBean field mapping configure list</span>
		 * <span class="zh-CN">JavaBean属性映射配置列表</span>
		 */
		private final List<FieldMapping> fieldMappings;

		/**
		 * <h3 class="en-US">Constructor for parse given JavaBean class instance and generate BeanMapping instance</h3>
		 * <h3 class="zh-CN">构造方法用于解析给定的JavaBean类对象，并生成BeanMapping对象</h3>
		 *
		 * @param beanClass <span class="en-US">Given JavaBean class instance</span>
		 *                  <span class="zh-CN">给定的JavaBean类对象</span>
		 */
		BeanMapping(final Class<?> beanClass) {
			this.fieldMappings = new ArrayList<>();
			ReflectionUtils.getAllDeclaredFields(beanClass, Boolean.TRUE)
					.forEach(field -> this.fieldMappings.add(new FieldMapping(field)));
		}

		/**
		 * <h3 class="en-US">Copy property value from the data map</h3>
		 * <h3 class="zh-CN">从数据Map复制属性数据</h3>
		 *
		 * @param targetObject <span class="en-US">Target JavaBean instance</span>
		 *                     <span class="zh-CN">目标JavaBean实例</span>
		 * @param originalMap  <span class="en-US">Original data map</span>
		 *                     <span class="zh-CN">来源数据Map</span>
		 */
		void copyData(final Object targetObject, final Map<String, Object> originalMap) {
			this.fieldMappings.forEach(fieldMapping -> fieldMapping.copyData(targetObject, originalMap));
		}

		/**
		 * <h3 class="en-US">Copy the property values from JavaBean instance arrays to the target JavaBean instance</h3>
		 * <h3 class="zh-CN">从JavaBean实例对象数组复制数据到目标JavaBean实例对象</h3>
		 *
		 * @param object  <span class="en-US">JavaBean instance</span>
		 *                <span class="zh-CN">JavaBean实例</span>
		 * @param objects <span class="en-US">JavaBean instance array</span>
		 *                <span class="zh-CN">JavaBean实例数组</span>
		 */
		void unmarshall(final Object object, final Object... objects) {
			this.fieldMappings.forEach(fieldMapping -> fieldMapping.unmarshall(object, objects));
		}

		/**
		 * <h3 class="en-US">Copy the property values from the JavaBean instance to JavaBean instance arrays</h3>
		 * <h3 class="zh-CN">在JavaBean实例对象复制数据到JavaBean实例对象数组</h3>
		 *
		 * @param object  <span class="en-US">JavaBean instance</span>
		 *                <span class="zh-CN">JavaBean实例</span>
		 * @param objects <span class="en-US">JavaBean instance array</span>
		 *                <span class="zh-CN">JavaBean实例数组</span>
		 */
		void marshall(final Object object, final Object... objects) {
			this.fieldMappings.forEach(fieldMapping -> fieldMapping.marshall(object, objects));
		}
	}

	/**
	 * <h2 class="en-US">JavaBean field mapping configure define</h2>
	 * <p class="en-US">Private inner class for define JavaBean field mapping configure</p>
	 * <h2 class="zh-CN">JavaBean属性映射配置定义</h2>
	 * <p class="zh-CN">定义JavaBean属性映射配置的私有内部类</p>
	 */
	private static final class FieldMapping {
		/**
		 * <span class="en-US">JavaBean field name</span>
		 * <span class="zh-CN">JavaBean属性名</span>
		 */
		private final String fieldName;
		/**
		 * <span class="en-US">JavaBean field type</span>
		 * <span class="zh-CN">JavaBean属性类型</span>
		 */
		private final Class<?> fieldType;
		/**
		 * <span class="en-US">JavaBean field data mapping configure</span>
		 * <span class="zh-CN">JavaBean属性数据映射配置</span>
		 */
		private final List<PropertyMapping> propertyMappings;

		/**
		 * <h3 class="en-US">Constructor for parse given JavaBean field instance and generate FieldMapping instance</h3>
		 * <h3 class="zh-CN">构造方法用于解析给定的JavaBean属性对象，并生成FieldMapping对象</h3>
		 *
		 * @param field <span class="en-US">JavaBean field instance</span>
		 *              <span class="zh-CN">JavaBean类属性对象</span>
		 */
		FieldMapping(final Field field) {
			this.fieldName = field.getName();
			this.fieldType = field.getType();
			this.propertyMappings = new ArrayList<>();
			Arrays.asList(field.getAnnotationsByType(BeanProperty.class)).forEach(this::registerProperty);
			this.propertyMappings.sort((o1, o2) -> o2.compare(o1));
		}

		/**
		 * <h3 class="en-US">Copy property value from the data map</h3>
		 * <h3 class="zh-CN">从数据Map复制属性数据</h3>
		 *
		 * @param targetObject <span class="en-US">Target JavaBean instance</span>
		 *                     <span class="zh-CN">目标JavaBean实例</span>
		 * @param originalMap  <span class="en-US">Original data map</span>
		 *                     <span class="zh-CN">来源数据Map</span>
		 */
		@SuppressWarnings("unchecked")
		void copyData(final Object targetObject, final Map<String, Object> originalMap) {
			if (originalMap == null || originalMap.isEmpty()) {
				return;
			}
			Object fieldValue = originalMap.get(this.fieldName);
			if (fieldValue instanceof Map
					&& !ObjectUtils.nullSafeEquals(this.fieldType, fieldValue.getClass())) {
				Object targetValue = ReflectionUtils.getFieldValue(this.fieldName, targetObject);
				if (targetValue == null) {
					targetValue = ObjectUtils.newInstance(this.fieldType);
				}
				BeanUtils.copyData((Map<String, Object>) fieldValue, targetValue);
				ReflectionUtils.setField(this.fieldName, targetObject, targetValue);
			} else {
				ReflectionUtils.setField(this.fieldName, targetObject, fieldValue);
			}
		}

		/**
		 * <h3 class="en-US">Copy the property values from the JavaBean instance to JavaBean instance arrays</h3>
		 * <h3 class="zh-CN">在JavaBean实例对象复制数据到JavaBean实例对象数组</h3>
		 *
		 * @param object  <span class="en-US">JavaBean instance</span>
		 *                <span class="zh-CN">JavaBean实例</span>
		 * @param objects <span class="en-US">JavaBean instance array</span>
		 *                <span class="zh-CN">JavaBean实例数组</span>
		 */
		void marshall(final Object object, final Object... objects) {
			if (object == null || objects == null || objects.length == 0) {
				return;
			}

			Object fieldValue = ReflectionUtils.getFieldValue(this.fieldName, object);
			if (fieldValue == null) {
				return;
			}

			for (Object target : objects) {
				this.propertyMappings.stream()
						.filter(propertyMapping -> propertyMapping.match(target))
						.forEach(propertyMapping ->
								ReflectionUtils.setField(propertyMapping.getFieldName(),
										target, propertyMapping.marshal(fieldValue)));
			}
		}

		/**
		 * <h3 class="en-US">Copy the property values from JavaBean instance arrays to the target JavaBean instance</h3>
		 * <h3 class="zh-CN">从JavaBean实例对象数组复制数据到目标JavaBean实例对象</h3>
		 *
		 * @param object  <span class="en-US">JavaBean instance</span>
		 *                <span class="zh-CN">JavaBean实例</span>
		 * @param objects <span class="en-US">JavaBean instance array</span>
		 *                <span class="zh-CN">JavaBean实例数组</span>
		 */
		void unmarshall(final Object object, final Object... objects) {
			if (object == null || objects == null || objects.length == 0) {
				return;
			}
			for (PropertyMapping propertyMapping : this.propertyMappings) {
				for (Object source : objects) {
					if (propertyMapping.match(source)) {
						Object fieldValue = ReflectionUtils.getFieldValue(propertyMapping.getFieldName(), source);
						ReflectionUtils.setField(this.fieldName, object, propertyMapping.unmarshal(fieldValue));
					}
				}
			}
		}

		/**
		 * <h3 class="en-US">Register BeanProperty annotation who was annotated at field</h3>
		 * <h3 class="zh-CN">注册注解在属性上的BeanProperty注解</h3>
		 *
		 * @param beanProperty <span class="en-US">Annotation instance of BeanProperty</span>
		 *                     <span class="zh-CN">BeanProperty注解实例</span>
		 */
		private void registerProperty(final BeanProperty beanProperty) {
			Field field = ReflectionUtils.getFieldIfAvailable(beanProperty.targetBean(), beanProperty.targetField());
			if (field == null) {
				return;
			}
			PropertyMapping propertyMapping = new PropertyMapping(beanProperty);
			if (this.propertyMappings.stream().anyMatch(existMapping -> existMapping.exists(beanProperty))) {
				LOGGER.warn("JavaBean_Property_Mapping_Existed_Warn",
						beanProperty.targetBean(), beanProperty.targetField());
				this.propertyMappings.replaceAll(existMapping -> {
					if (existMapping.exists(beanProperty)) {
						return propertyMapping;
					}
					return existMapping;
				});
			} else {
				this.propertyMappings.add(propertyMapping);
			}
		}
	}

	private static final class PropertyMapping {
		private final int sortCode;
		private final String targetBeanClass;
		private final String fieldName;
		private final TransferConfig transferConfig;

		PropertyMapping(final BeanProperty beanProperty) {
			this.sortCode = beanProperty.sortCode();
			this.targetBeanClass = beanProperty.targetBean().getName();
			this.fieldName = beanProperty.targetField();
			this.transferConfig = new TransferConfig(beanProperty.transfer());
		}

		public int getSortCode() {
			return sortCode;
		}

		public String getFieldName() {
			return fieldName;
		}

		boolean exists(final BeanProperty beanProperty) {
			return ObjectUtils.nullSafeEquals(this.targetBeanClass, beanProperty.targetBean().getName())
					&& ObjectUtils.nullSafeEquals(this.fieldName, beanProperty.targetField());
		}

		boolean match(final Object object) {
			if (object == null) {
				return Boolean.FALSE;
			}
			return ObjectUtils.nullSafeEquals(this.targetBeanClass, ClassUtils.originalClassName(object.getClass()));
		}

		int compare(final PropertyMapping propertyMapping) {
			if (this.sortCode != propertyMapping.getSortCode()) {
				return Integer.compare(propertyMapping.getSortCode(), this.sortCode);
			}
			if (!ObjectUtils.nullSafeEquals(this.targetBeanClass, propertyMapping.targetBeanClass)) {
				return propertyMapping.targetBeanClass.compareTo(this.targetBeanClass);
			}
			return propertyMapping.getFieldName().compareTo(this.fieldName);
		}

		Object marshal(final Object object) {
			return this.transferConfig.marshal(object);
		}

		Object unmarshal(final Object object) {
			return this.transferConfig.unmarshal(object);
		}
	}

	/**
	 * <h3 class="en-US">Register URL instance of the schema mapping file</h3>
	 * <h3 class="zh-CN">从URL实例对象中读取XML约束文档的资源映射文件内容并注册</h3>
	 *
	 * @param url <span class="en-US">URL instance</span>
	 *            <span class="zh-CN">URL实例对象</span>
	 */
	private static void REGISTER_SCHEMA(final URL url) {
		String basePath = url.getPath();
		ConvertUtils.toMap(url, new HashMap<>())
				.forEach((key, value) ->
						SCHEMA_MAPPING.put(key, StringUtils.replace(basePath, SCHEMA_MAPPING_RESOURCE_PATH, value)));
	}

	/**
	 * <h2 class="en-US">Schema resource resolver for support schema mapping</h2>
	 * <h2 class="zh-CN">支持自定义资源描述文件映射的资源文件解析器</h2>
	 */
	private static final class SchemaResourceResolver implements LSResourceResolver {
		/**
		 * (Non-Javadoc)
		 *
		 * @see LSResourceResolver#resolveResource(String, String, String, String, String)
		 */
		@Override
		public LSInput resolveResource(final String type, final String namespaceURI, final String publicId,
		                               final String systemId, final String baseURI) {
			LOGGER.debug("Resolving_Schema_Debug",
					type, namespaceURI, publicId, systemId, baseURI);
			String schemaLocation = baseURI.substring(0, baseURI.lastIndexOf("/") + 1);
			String filePath;
			if (SCHEMA_MAPPING.containsKey(namespaceURI)) {
				filePath = SCHEMA_MAPPING.get(namespaceURI);
			} else {
				if (!systemId.contains(Globals.HTTP_PROTOCOL)) {
					filePath = schemaLocation + systemId;
				} else {
					filePath = systemId;
				}
			}
			try {
				return new LSInputImpl(publicId, namespaceURI, FileUtils.loadFile(filePath));
			} catch (IOException e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Load_Schemas_Error", e);
				}
				return new LSInputImpl();
			}
		}
	}

	/**
	 * <h2 class="en-US">Implement class for LSInput</h2>
	 * <h2 class="zh-CN">LSInput的实现类</h2>
	 */
	private static final class LSInputImpl implements LSInput {
		private String publicId;
		private String systemId;
		private String baseURI;
		private InputStream byteStream;
		private Reader characterStream;
		private String stringData;
		private String encoding;
		private boolean certifiedText;

		/**
		 * <h3 class="en-US">Default constructor for LSInputImpl</h3>
		 * <h3 class="zh-CN">LSInputImpl的私有构造方法</h3>
		 */
		LSInputImpl() {
		}

		/**
		 * <h3 class="en-US">Default constructor for LSInputImpl</h3>
		 * <h3 class="zh-CN">LSInputImpl的私有构造方法</h3>
		 *
		 * @param publicId   <span class="en-US">Public ID</span>
		 *                   <span class="zh-CN">Public ID</span>
		 * @param systemId   <span class="en-US">Namespace URI</span>
		 *                   <span class="zh-CN">命名空间URI</span>
		 * @param byteStream <span class="en-US">Input stream of schema file</span>
		 *                   <span class="zh-CN">描述文件的输入流</span>
		 */
		LSInputImpl(final String publicId, final String systemId, final InputStream byteStream) {
			this.publicId = publicId;
			this.systemId = systemId;
			this.byteStream = byteStream;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#getPublicId()
		 */
		@Override
		public String getPublicId() {
			return publicId;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#setPublicId(String)
		 */
		@Override
		public void setPublicId(String publicId) {
			this.publicId = publicId;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#getSystemId()
		 */
		@Override
		public String getSystemId() {
			return systemId;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#setSystemId(String)
		 */
		@Override
		public void setSystemId(String systemId) {
			this.systemId = systemId;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#getBaseURI()
		 */
		@Override
		public String getBaseURI() {
			return baseURI;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#setBaseURI(String)
		 */
		@Override
		public void setBaseURI(String baseURI) {
			this.baseURI = baseURI;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#getByteStream()
		 */
		@Override
		public InputStream getByteStream() {
			return byteStream;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#setByteStream(InputStream)
		 */
		@Override
		public void setByteStream(InputStream byteStream) {
			this.byteStream = byteStream;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#getCharacterStream()
		 */
		@Override
		public Reader getCharacterStream() {
			return characterStream;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#setCharacterStream(Reader)
		 */
		@Override
		public void setCharacterStream(Reader characterStream) {
			this.characterStream = characterStream;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#getStringData()
		 */
		@Override
		public String getStringData() {
			return stringData;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#setStringData(String)
		 */
		@Override
		public void setStringData(String stringData) {
			this.stringData = stringData;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#getEncoding()
		 */
		@Override
		public String getEncoding() {
			return encoding;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#setEncoding(String)
		 */
		@Override
		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#getCertifiedText()
		 */
		@Override
		public boolean getCertifiedText() {
			return certifiedText;
		}

		/**
		 * (Non-Javadoc)
		 *
		 * @see LSInput#setCertifiedText(boolean)
		 */
		@Override
		public void setCertifiedText(boolean certifiedText) {
			this.certifiedText = certifiedText;
		}
	}
}
