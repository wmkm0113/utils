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
import org.nervousync.beans.converter.BeanConverter;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.beans.StringType;
import org.nervousync.utils.core.*;
import org.nervousync.utils.logger.LoggerUtils;

import java.io.*;
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
	public String objectToString(final Object object, final StringType stringType, final boolean formatOutput,
	                             final boolean outputFragment, final String encoding) {
		switch (stringType) {
			case JSON:
				try {
					return formatOutput
							? JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object)
							: JSON_MAPPER.writeValueAsString(object);
				} catch (JsonProcessingException e) {
					LOGGER.error("Convert_String_Error");
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
				}
				break;
			case YAML:
				try {
					return formatOutput
							? YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object)
							: YAML_MAPPER.writeValueAsString(object);
				} catch (JsonProcessingException e) {
					LOGGER.error("Convert_String_Error");
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
				}
				break;
			default:
				return DEFAULT_BEAN_CONVERTER.objectToString(object, stringType, formatOutput, outputFragment, encoding);
		}
		return Globals.DEFAULT_VALUE_STRING;
	}

	@Override
	public <T> T streamToObject(@Nonnull final InputStream inputStream, final StringType stringType,
	                            final String encoding, final Class<T> beanClass, final String... schemaPaths) {
		try {
			switch (stringType) {
				case JSON:
					return JSON_MAPPER.readValue(IOUtils.readContent(inputStream), beanClass);
				case YAML:
					return YAML_MAPPER.readValue(IOUtils.readContent(inputStream), beanClass);
				default:
					return DEFAULT_BEAN_CONVERTER.streamToObject(inputStream, stringType, encoding, beanClass, schemaPaths);
			}
		} catch (IOException ignore) {
			return null;
		}
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
					return Collections.emptyList();
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
					return JSON_MAPPER.readValue(inputStream, new TypeReference<>() {
					});
				case YAML:
					return YAML_MAPPER.readValue(inputStream, new TypeReference<>() {
					});
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
}
