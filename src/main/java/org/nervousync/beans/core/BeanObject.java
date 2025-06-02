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
package org.nervousync.beans.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.*;
import org.nervousync.annotations.beans.OutputConfig;
import org.nervousync.annotations.beans.Signature;
import org.nervousync.commons.Globals;
import org.nervousync.utils.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

/**
 * <h2 class="en-US">Abstract class of JavaBean</h2>
 * <span class="en-US">
 * <p>If JavaBean class extends the current abstract class, it can easier convert the object to JSON/XML/YAML string.
 * The default encoding is UTF-8</p>
 * <p>Convert objects to XML must add annotation to class and fields, using JAXB annotation</p>
 * <p>Convert custom fields in objects to JSON/YAML must add annotation to fields, using jackson annotation</p>
 * </span>
 * <h2 class="zh-CN">JavaBean的抽象类</h2>
 * <span class="zh-CN">
 * <p>如果JavaBean类继承此抽象类，将可以简单的转化对象为JSON/XML/YAML字符串。默认编码集为UTF-8</p>
 * <p>转换对象为XML时，必须使用JAXB注解对类和属性进行标注</p>
 * <p>转换对象中的指定属性值为JSON/YAML时，必须使用Jackson注解对属性进行标注</p>
 * </span>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.2.0 $ $Date: Jan 6, 2021 17:10:23 $
 */
@XmlTransient
@XmlAccessorType(XmlAccessType.NONE)
@OutputConfig(formatted = true)
public abstract class BeanObject implements Serializable, Cloneable {
	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = 6900853002518080456L;

	/**
	 * (non-javadoc)
	 *
	 * @see Object#equals(Object)
	 */
	@Override
	public final boolean equals(final Object o) {
		if (o == null || !o.getClass().equals(this.getClass())) {
			return Boolean.FALSE;
		}
		if (this == o) {
			return Boolean.TRUE;
		}
		return Arrays.stream(this.getClass().getDeclaredFields())
				.filter(field -> !ReflectionUtils.staticMember(field))
				.allMatch(field ->
						Objects.equals(ReflectionUtils.getFieldValue(field, this),
								ReflectionUtils.getFieldValue(field, o)));
	}

	/**
	 * (non-javadoc)
	 *
	 * @see Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		int result = Globals.INITIALIZE_INT_VALUE;
		try {
			for (Field field : this.getClass().getDeclaredFields()) {
				Object origValue = ReflectionUtils.getFieldValue(field, this);
				result = Globals.MULTIPLIER * result + (origValue != null ? origValue.hashCode() : 0);
			}
		} catch (Exception e) {
			result = Globals.DEFAULT_VALUE_INT;
		}
		return result;
	}

	/**
	 * (non-javadoc)
	 *
	 * @see Object#toString()
	 */
	@Override
	public final String toString() {
		return Optional.ofNullable(this.getClass().getAnnotation(OutputConfig.class))
				.map(outputConfig ->
						this.toString(outputConfig.defaultType(), outputConfig.formatted(), outputConfig.encoding()))
				.orElse(this.toString(StringUtils.StringType.SERIALIZABLE, Boolean.FALSE, Globals.DEFAULT_ENCODING));
	}

	/**
	 * <h3 class="en-US">Converts to a string of the given type</h3>
	 * <h3 class="zh-CN">转换为给定类型的字符串</h3>
	 *
	 * @param stringType <span class="en-US">Target string type</span>
	 *                   <span class="zh-CN">目标字符串类型</span>
	 * @return <span class="en-US">The converted string, or an empty string if the given type is not supported</span>
	 * <span class="zh-CN">转换后的字符串，如果给定类型不支持，则返回空字符串</span>
	 */
	public final String toString(@Nonnull final StringUtils.StringType stringType) {
		return Optional.ofNullable(this.getClass().getAnnotation(OutputConfig.class))
				.filter(outputConfig ->
						outputConfig.defaultType().equals(stringType)
								|| Arrays.asList(outputConfig.types()).contains(stringType))
				.map(outputConfig ->
						this.toString(stringType, outputConfig.formatted(), outputConfig.encoding()))
				.orElse(super.toString());
	}

	/**
	 * <h3 class="en-US">Clone current object</h3>
	 * <h3 class="zh-CN">复制当前对象</h3>
	 *
	 * @param deepClone <span class="en-US">Deep clone flag</span>
	 *                  <span class="zh-CN">深克隆标记</span>
	 * @return <span class="en-US">Clone object</span>
	 * <span class="zh-CN">克隆的对象</span>
	 */
	public final Object clone(final boolean deepClone) {
		if (deepClone) {
			return ConvertUtils.toObject(ConvertUtils.toByteArray(this));
		} else {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError("Clone object failed! ", e);
			}
		}
	}

	/**
	 * <h3 class="en-US">Verify that the digital signature is legitimate</h3>
	 * <h3 class="zh-CN">验证数字签名是否合法</h3>
	 *
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	public final boolean validate() {
		OutputConfig config = this.getClass().getAnnotation(OutputConfig.class);
		if (config == null) {
			return Boolean.FALSE;
		}

		List<StringUtils.StringType> dataTypes = new ArrayList<>();
		dataTypes.add(config.defaultType());
		for (StringUtils.StringType type : config.types()) {
			if (!dataTypes.contains(type)) {
				dataTypes.add(type);
			}
		}

		return Optional.ofNullable(this.getClass().getAnnotation(Signature.class))
				.map(Signature::value)
				.filter(StringUtils::notBlank)
				.map(fieldName ->
						Optional.ofNullable((String) ReflectionUtils.getFieldValue(fieldName, this))
								.filter(StringUtils::notBlank)
								.map(signature ->
										dataTypes.stream().anyMatch(type ->
												ObjectUtils.nullSafeEquals(signature, this.signature(type, fieldName))))
								.orElse(Boolean.FALSE))
				.orElse(Boolean.TRUE);
	}

	/**
	 * <h3 class="en-US">Converts to a string of the given type</h3>
	 * <h3 class="zh-CN">转换为给定类型的字符串</h3>
	 *
	 * @param stringType   <span class="en-US">Target string type</span>
	 *                     <span class="zh-CN">目标字符串类型</span>
	 * @param formatOutput <span class="en-US">format output string</span>
	 *                     <span class="zh-CN">格式化输出字符串</span>
	 * @param encoding     <span class="en-US">String charset encoding</span>
	 *                     <span class="zh-CN">字符串的字符集编码</span>
	 * @return <span class="en-US">The converted string, or an empty string if the given type is not supported</span>
	 * <span class="zh-CN">转换后的字符串，如果给定类型不支持，则返回空字符串</span>
	 */
	private String toString(final StringUtils.StringType stringType, final boolean formatOutput, final String encoding) {
		Optional.ofNullable(this.getClass().getAnnotation(Signature.class))
				.map(Signature::value)
				.filter(StringUtils::notBlank)
				.ifPresent(fieldName ->
						ReflectionUtils.setField(fieldName, this, this.signature(stringType, fieldName)));
		switch (stringType) {
			case XML:
				return StringUtils.objectToString(this, StringUtils.StringType.XML, formatOutput, Boolean.TRUE, encoding);
			case JSON:
			case YAML:
			case SERIALIZABLE:
				return StringUtils.objectToString(this, stringType, formatOutput);
			default:
				return super.toString();
		}
	}

	/**
	 * <h3 class="en-US">Generate the digital signature</h3>
	 * <h3 class="zh-CN">生成数字签名</h3>
	 *
	 * @param stringType <span class="en-US">Target string type</span>
	 *                   <span class="zh-CN">目标字符串类型</span>
	 * @param fieldName  <span class="en-US">The name of the attribute that stores the digital signature</span>
	 *                   <span class="zh-CN">保存数字签名的属性名</span>
	 * @return <span class="en-US">Generated digital signature</span>
	 * <span class="zh-CN">生成的数字签名</span>
	 */
	private String signature(final StringUtils.StringType stringType, final String fieldName) {
		String[] ignoreFields = Optional.ofNullable(this.getClass().getAnnotation(JsonIgnoreProperties.class))
				.map(JsonIgnoreProperties::value)
				.map(fieldNames -> (String[]) CollectionUtils.addObjectToArray(fieldNames, fieldName))
				.orElse(new String[]{fieldName});
		TreeMap<String, Object> fieldsMap = this.fieldsMap(stringType, ignoreFields);
		if (fieldsMap.isEmpty()) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		return ConvertUtils.bytesToHex(SecurityUtils.SHA256(fieldsMap));
	}

	private TreeMap<String, Object> fieldsMap(final StringUtils.StringType stringType, final String... ignoreFields) {
		TreeMap<String, Object> fieldsMap = new TreeMap<>();
		ReflectionUtils.getAllDeclaredFields(this.getClass(), Boolean.TRUE, ReflectionUtils.NON_STATIC_FINAL_MEMBERS)
				.stream()
				.filter(field -> {
					if (Arrays.stream(ignoreFields).anyMatch(ignoreField -> field.getName().equalsIgnoreCase(ignoreField))) {
						return Boolean.FALSE;
					}
					switch (stringType) {
						case JSON:
						case YAML:
							if (field.isAnnotationPresent(JsonIgnore.class)) {
								return Boolean.FALSE;
							}
							Method getterMethod = ReflectionUtils.getterMethod(field.getName(), this.getClass());
							if (getterMethod != null && getterMethod.isAnnotationPresent(JsonIgnore.class)) {
								return Boolean.FALSE;
							}
							Method setterMethod = ReflectionUtils.setterMethod(field.getName(), this.getClass());
							if (setterMethod != null && setterMethod.isAnnotationPresent(JsonIgnore.class)) {
								return Boolean.FALSE;
							}
							return Boolean.TRUE;
						case XML:
							if (field.isAnnotationPresent(XmlElement.class) || field.isAnnotationPresent(XmlElementWrapper.class)
									|| field.isAnnotationPresent(XmlAttribute.class)) {
								return Boolean.TRUE;
							}
							return Boolean.FALSE;
						default:
							return Boolean.TRUE;
					}
				})
				.forEach(field ->
						Optional.ofNullable(ReflectionUtils.getFieldValue(field, this))
								.ifPresent(fieldValue -> {
									if (fieldValue instanceof BeanObject) {
										fieldsMap.put(field.getName(), ((BeanObject) fieldValue).fieldsMap(stringType));
									} else {
										fieldsMap.put(field.getName(), ConvertUtils.bytesToHex(ConvertUtils.toByteArray(fieldValue)));
									}
								}));
		return fieldsMap;
	}
}
