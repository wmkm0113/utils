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

package org.nervousync.beans.converter.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profile.pegdown.Extensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.data.DataHolder;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import org.nervousync.annotations.beans.OutputConfig;
import org.nervousync.annotations.beans.Signature;
import org.nervousync.beans.converter.BeanConverter;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.beans.StringType;
import org.nervousync.enumerations.security.EncodeType;
import org.nervousync.utils.core.*;
import org.nervousync.utils.logger.LoggerUtils;
import org.nervousync.utils.security.SecurityUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * <h2 class="en-US">Enhance implementation class of the JavaBean converter</h2>
 * <h2 class="zh-CN">增强的 JavaBean 转换适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.3.0 $ $Date: Jan 18, 2026 14:55:15 $
 */
public final class EnhanceBeanConverterImpl implements BeanConverter {
	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(BeanUtils.class);
	/**
	 * <span class="en-US">JSON object mapper instance</span>
	 * <span class="zh-CN">JSON数据映射实例对象</span>
	 */
	private static final ObjectMapper JSON_MAPPER =
			JsonMapper.builder().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
					.addModule(new JavaTimeModule())
					.build();
	/**
	 * <span class="en-US">YAML object mapper instance</span>
	 * <span class="zh-CN">YAML数据映射实例对象</span>
	 */
	private static final ObjectMapper YAML_MAPPER =
			JsonMapper.builder(YAMLFactory.builder().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).build())
					.addModule(new JavaTimeModule())
					.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
					.build();

	private static final BeanConverter DEFAULT_BEAN_CONVERTER = new DefaultBeanConverterImpl();

	@Override
	public String objectToString(@Nonnull final Object object) {
		return Optional.ofNullable(object.getClass().getAnnotation(OutputConfig.class))
				.map(outputConfig -> {
					signature(object);
					try {
						switch (outputConfig.type()) {
							case JSON:
								return outputConfig.formatted()
										? JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object)
										: JSON_MAPPER.writeValueAsString(object);
							case YAML:
								return outputConfig.formatted()
										? YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object)
										: YAML_MAPPER.writeValueAsString(object);
							default:
								return DEFAULT_BEAN_CONVERTER.objectToString(object);
						}
					} catch (JsonProcessingException e) {
						LOGGER.error("Convert_String_Error");
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Stack_Message_Error", e);
						}
					}
					return Globals.DEFAULT_VALUE_STRING;
				})
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}

	@Override
	public <T> T streamToObject(@Nonnull final InputStream inputStream, final StringType stringType,
	                            final String encoding, final Class<T> beanClass, final String... schemaPaths) {
		T readObject;
		try {
			switch (stringType) {
				case JSON:
					readObject = JSON_MAPPER.readValue(IOUtils.readContent(inputStream), beanClass);
					break;
				case YAML:
					readObject = YAML_MAPPER.readValue(IOUtils.readContent(inputStream), beanClass);
					break;
				default:
					readObject = DEFAULT_BEAN_CONVERTER.streamToObject(inputStream, stringType, encoding, beanClass, schemaPaths);
					break;
			}
		} catch (IOException ignore) {
			return null;
		}
		return validate(readObject) ? readObject : null;
	}

	@Override
	public <T> List<T> streamToList(@Nonnull final InputStream inputStream, final StringType stringType,
	                                final String encoding, final Class<T> beanClass) {
		try {
			switch (stringType) {
				case JSON:
					return JSON_MAPPER.readValue(inputStream,
							new ObjectMapper().getTypeFactory().constructParametricType(ArrayList.class, beanClass));
				case YAML:
					return YAML_MAPPER.readValue(inputStream,
							new ObjectMapper().getTypeFactory().constructParametricType(ArrayList.class, beanClass));
				default:
					return DEFAULT_BEAN_CONVERTER.streamToList(inputStream, stringType, encoding, beanClass);
			}
		} catch (IOException ignore) {
			return Collections.emptyList();
		}
	}

	@Override
	public Map<String, Object> streamToMap(@Nonnull final InputStream inputStream, final StringType stringType,
	                                       final String encoding) {
		try {
			switch (stringType) {
				case JSON:
					return JSON_MAPPER.readValue(inputStream, new TypeReference<>() {});
				case YAML:
					return YAML_MAPPER.readValue(inputStream, new TypeReference<>() {});
				default:
					return Map.of();
			}
		} catch (IOException ignore) {
			return Map.of();
		}
	}

	@Override
	public String mdToHtml(final String markdown) {
		DataHolder options = PegdownOptionsAdapter.flexmarkOptions(Boolean.TRUE, Extensions.ALL);
		Parser parser = Parser.builder(options).build();
		HtmlRenderer render = HtmlRenderer.builder(options).build();
		return render.render(parser.parse(markdown));
	}

	/**
	 * <h3 class="en-US">Generate the digital signature</h3>
	 * <h3 class="zh-CN">生成数字签名</h3>
	 *
	 * @param object <span class="en-US">JavaBean instance object which need generate signature</span>
	 *               <span class="zh-CN">需要生成数字签名的 JavaBean 实例对象</span>
	 */
	private static void signature(@Nonnull final Object object) {
		Signature signature = object.getClass().getAnnotation(Signature.class);
		if (signature == null || StringUtils.isEmpty(signature.value())) {
			return;
		}
		ReflectionUtils.setField(signature.value(), object, SecurityUtils.SHA256(fieldsMap(object), EncodeType.HEX));
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
	@Override
	public boolean validate(final Object object) {
		if (object == null) {
			return Boolean.FALSE;
		}
		final Signature signAnno = object.getClass().getAnnotation(Signature.class);
		if (signAnno == null || StringUtils.isEmpty(signAnno.value())) {
			//  No signature field defined
			return Boolean.TRUE;
		}
		final String signature = (String) ReflectionUtils.getFieldValue(signAnno.value(), object);
		if (StringUtils.isEmpty(signature)) {
			return Boolean.FALSE;
		}
		return ObjectUtils.nullSafeEquals(signature, SecurityUtils.SHA256(fieldsMap(object), EncodeType.HEX));
	}

	/**
	 * <h3 class="en-US">Retrieving ignored attribute names from JSON data</h3>
	 * <h3 class="zh-CN">获取 JSON 数据中忽略的属性名</h3>
	 *
	 * @param clazz <span class="en-US">JavaBean define class</span>
	 *              <span class="zh-CN">JavaBean 定义类</span>
	 * @return <span class="en-US">Ignored field names array</span>
	 * <span class="zh-CN">忽略的属性名数组</span>
	 */
	private static String[] ignoreFields(@Nonnull final Class<?> clazz) {
		return Optional.ofNullable(clazz.getAnnotation(JsonIgnoreProperties.class))
				.map(JsonIgnoreProperties::value)
				.orElse(new String[0]);
	}

	/**
	 * <h3 class="en-US">Generate the digital signature used data mapping table</h3>
	 * <h3 class="zh-CN">生成数字签名使用的数据映射表</h3>
	 *
	 * @param object <span class="en-US">JavaBean instance object which need generate signature</span>
	 *               <span class="zh-CN">需要生成数字签名的 JavaBean 实例对象</span>
	 * @return <span class="en-US">Data mapping table</span>
	 * <span class="zh-CN">数据映射表</span>
	 */
	private static TreeMap<String, Object> fieldsMap(@Nonnull final Object object) {
		final TreeMap<String, Object> fieldsMap = new TreeMap<>();
		Signature signature = object.getClass().getAnnotation(Signature.class);
		if (signature == null || StringUtils.isEmpty(signature.value())) {
			return fieldsMap;
		}
		Optional.ofNullable(object.getClass().getAnnotation(OutputConfig.class))
				.ifPresent(outputConfig -> {
					Class<?> clazz = object.getClass();
					String[] ignoreFields = StringType.JSON.equals(outputConfig.type())
							? (String[]) CollectionUtils.addObjectToArray(ignoreFields(clazz), signature.value())
							: new String[]{signature.value()};

					ReflectionUtils.getAllDeclaredFields(clazz, Boolean.TRUE, ReflectionUtils.NON_STATIC_FINAL_MEMBERS)
							.stream()
							.filter(field -> {
								if (Arrays.stream(ignoreFields).anyMatch(ignoreField -> field.getName().equalsIgnoreCase(ignoreField))) {
									return Boolean.FALSE;
								}
								switch (outputConfig.type()) {
									case JSON:
									case YAML:
										if (field.isAnnotationPresent(JsonIgnore.class)) {
											return Boolean.FALSE;
										}
										Method getterMethod = ReflectionUtils.getterMethod(field.getName(), clazz);
										if (getterMethod != null && getterMethod.isAnnotationPresent(JsonIgnore.class)) {
											return Boolean.FALSE;
										}
										Method setterMethod = ReflectionUtils.setterMethod(field.getName(), clazz);
										if (setterMethod != null && setterMethod.isAnnotationPresent(JsonIgnore.class)) {
											return Boolean.FALSE;
										}
										return Boolean.TRUE;
									case XML:
										return field.isAnnotationPresent(XmlElement.class)
												|| field.isAnnotationPresent(XmlElementWrapper.class)
												|| field.isAnnotationPresent(XmlAttribute.class);
									default:
										return Boolean.TRUE;
								}
							})
							.forEach(field ->
									Optional.ofNullable(ReflectionUtils.getFieldValue(field, object))
											.map(fieldValue -> {
												if (fieldValue.getClass().isAnnotationPresent(OutputConfig.class)) {
													return fieldsMap(fieldValue);
												} else {
													return StringUtils.base64Encode(ConvertUtils.toByteArray(fieldValue));
												}
											})
											.ifPresent(convertValue -> fieldsMap.put(field.getName(), convertValue)));
				});
		return fieldsMap;
	}
}
