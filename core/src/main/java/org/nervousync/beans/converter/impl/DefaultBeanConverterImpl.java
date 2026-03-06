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

import jakarta.annotation.Nonnull;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.nervousync.beans.converter.BeanConverter;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.beans.StringType;
import org.nervousync.utils.core.*;
import org.nervousync.xml.writer.CDataStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.*;

/**
 * <h2 class="en-US">Default implementation class of the JavaBean converter</h2>
 * <h2 class="zh-CN">默认的 JavaBean 转换适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.3.0 $ $Date: Jan 18, 2026 14:55:15 $
 */
public final class DefaultBeanConverterImpl implements BeanConverter {

	@Override
	public String objectToString(@Nonnull final Object object, @Nonnull final StringType stringType,
	                             final String encoding, final boolean formatted) {
		String characterEncoding = StringUtils.isEmpty(encoding) ? Globals.DEFAULT_ENCODING : encoding;
		switch (stringType) {
			case XML:
				if (!object.getClass().isAnnotationPresent(XmlRootElement.class)) {
					return Globals.DEFAULT_VALUE_STRING;
				}
				StringWriter stringWriter = null;
				try {
					stringWriter = new StringWriter();
					XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);
					CDataStreamWriter streamWriter = new CDataStreamWriter(xmlWriter);

					JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
					Marshaller marshaller = jaxbContext.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
					marshaller.setProperty(Marshaller.JAXB_ENCODING, characterEncoding);
					marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

					marshaller.marshal(object, streamWriter);

					streamWriter.flush();
					streamWriter.close();

					if (formatted) {
						Transformer transformer = TransformerFactory.newInstance().newTransformer();
						transformer.setOutputProperty(OutputKeys.ENCODING, characterEncoding);
						transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
						transformer.setOutputProperty(OutputKeys.INDENT, "yes");
						transformer.setOutputProperty("{https://xml.apache.org/xslt}indent-amount", "4");

						String xml = stringWriter.toString();
						stringWriter = new StringWriter();

						transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(stringWriter));
					}

					StringBuilder stringBuilder =
							new StringBuilder(StringUtils.replace(BeanUtils.FRAGMENT_TEMPLATE, "{}", characterEncoding));
					if (formatted) {
						stringBuilder.append(FileUtils.LF);
					}
					stringBuilder.append(stringWriter);
					return stringBuilder.toString();
				} catch (Exception e) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
					return Globals.DEFAULT_VALUE_STRING;
				} finally {
					IOUtils.closeStream(stringWriter);
				}
			case JSON:
				JsonbConfig config = new JsonbConfig().withFormatting(formatted).withEncoding(characterEncoding);
				try (Jsonb jsonb = JsonbBuilder.create(config)) {
					return jsonb.toJson(object);
				} catch (Exception e) {
					LOGGER.error("Convert_String_Error");
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
				}
				break;
			case SIMPLE:
				return object.toString();
			case SERIALIZABLE:
				return StringUtils.base64Encode(ConvertUtils.toByteArray(object));
			default:
				return Globals.DEFAULT_VALUE_STRING;
		}
		return Globals.DEFAULT_VALUE_STRING;
	}

	@Override
	public <T> T streamToObject(@Nonnull final InputStream inputStream, final StringType stringType,
	                            final String encoding, @Nonnull final Class<T> beanClass, final String... schemaPaths) {
		switch (stringType) {
			case XML:
				try {
					Unmarshaller unmarshaller = JAXBContext.newInstance(beanClass).createUnmarshaller();
					Optional.ofNullable(BeanUtils.newSchema(schemaPaths))
							.ifPresent(unmarshaller::setSchema);
					return beanClass.cast(unmarshaller.unmarshal(inputStream));
				} catch (JAXBException e) {
					LOGGER.error("Parse_File_Error");
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
					return null;
				}
			case SIMPLE:
				return ClassUtils.parseSimpleData(IOUtils.readContent(inputStream), beanClass);
			case SERIALIZABLE:
				return Optional.of(IOUtils.readContent(inputStream))
						.map(StringUtils::base64Decode)
						.map(ConvertUtils::toObject)
						.filter(object -> ClassUtils.isAssignable(object.getClass(), beanClass))
						.map(beanClass::cast)
						.orElse(null);
			case JSON:
				JsonbConfig config = new JsonbConfig().withEncoding(StringUtils.isEmpty(encoding) ? Globals.DEFAULT_ENCODING : encoding);
				try (Jsonb jsonb = JsonbBuilder.create(config)) {
					return jsonb.fromJson(inputStream, beanClass);
				} catch (Exception e) {
					LOGGER.error("Parse_File_Error");
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
					return null;
				}
			default:
				return null;
		}
	}

	@Override
	public <T> List<T> streamToList(@Nonnull final InputStream inputStream, final StringType stringType,
	                                final String encoding, final Class<T> beanClass) {
		if (StringType.JSON.equals(stringType)) {
			JsonbConfig config = new JsonbConfig().withEncoding(StringUtils.isEmpty(encoding) ? Globals.DEFAULT_ENCODING : encoding);
			try (Jsonb jsonb = JsonbBuilder.create(config)) {
				return jsonb.fromJson(inputStream, beanClass.getGenericSuperclass());
			} catch (Exception e) {
				LOGGER.error("Parse_File_Error");
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Stack_Message_Error", e);
				}
				return new ArrayList<>();
			}
		}
		return List.of();
	}

	@Override
	public Map<String, Object> streamToMap(@Nonnull final InputStream inputStream, final StringType stringType,
	                                       final String encoding) {
		if (StringType.JSON.equals(stringType)) {
			JsonbConfig config = new JsonbConfig().withEncoding(StringUtils.isEmpty(encoding) ? Globals.DEFAULT_ENCODING : encoding);
			try (Jsonb jsonb = JsonbBuilder.create(config)) {
				return jsonb.fromJson(inputStream, new HashMap<String, Object>() {
					private static final long serialVersionUID = 2929260973754559724L;
				}.getClass().getGenericSuperclass());
			} catch (Exception e) {
				LOGGER.error("Parse_File_Error");
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Stack_Message_Error", e);
				}
			}
		}
		return new HashMap<>();
	}

	@Override
	public String mdToHtml(final String markdown) {
		return Globals.DEFAULT_VALUE_STRING;
	}
}
