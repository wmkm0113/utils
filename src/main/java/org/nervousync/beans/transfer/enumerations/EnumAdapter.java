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

package org.nervousync.beans.transfer.enumerations;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.nervousync.utils.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

/**
 * <h2 class="en-US">Abstract class of enum data convert adapter</h2>
 * <h2 class="zh-CN">枚举数据转换适配器抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.0 $Date: Jun 15, 2020 14:27:28 $
 */
public abstract class EnumAdapter<T> extends XmlAdapter<String, T> {

	/**
	 * <span class="en-US">Enumeration class valueOf method instance object</span>
	 * <span class="zh-CN">枚举类的valueOf方法实例对象</span>
	 */
	private final Method method;

	/**
	 * <h3 class="en-US">Constructor method for abstract class of enum data convert adapter</h3>
	 * <h3 class="zh-CN">枚举数据转换适配器抽象类的构造方法</h3>
	 */
	protected EnumAdapter() {
		Class<?> clazz = (Class<?>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.method = ReflectionUtils.findMethod(clazz, "valueOf", new Class[]{String.class});
	}

	@Override
	@SuppressWarnings("unchecked")
	public final T unmarshal(final String v) {
		if (this.method == null) {
			throw new IllegalArgumentException("Method not found");
		}
		return (T) ReflectionUtils.invokeMethod(this.method, null, new Object[]{v});
	}

	@Override
	public final String marshal(final T v) {
		return v.toString();
	}
}
