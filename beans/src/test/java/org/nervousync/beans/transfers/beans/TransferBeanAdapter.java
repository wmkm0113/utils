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
package org.nervousync.beans.transfers.beans;

import org.nervousync.beans.transfer.TransferAdapter;
import org.nervousync.utils.core.BeanUtils;
import org.nervousync.utils.core.ClassUtils;
import org.nervousync.utils.core.StringUtils;

import java.util.Optional;

/**
 * <h2 class="en-US">JavaBean abstract convert adapter</h2>
 * <h2 class="zh-CN">JavaBean数据转换适配器抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.0 $ $Date: Jun 25, 2023 11:27:18 $
 */
public final class TransferBeanAdapter extends TransferAdapter {

	/**
	 * <span class="en-US">Target class</span>
	 * <span class="zh-CN">目标类</span>
	 */
	private final Class<?> beanClass;


	/**
	 * <h3 class="en-US">Constructor method for JavaBean convert adapter</h3>
	 * <h3 class="zh-CN">JavaBean数据转换适配器的构造方法</h3>
	 *
	 * @param className  <span class="en-US">Target class name string</span>
	 *                   <span class="zh-CN">目标类名字符串</span>
	 * @throws IllegalArgumentException <span class="en-US">If target class is not the child class of org.nervousync.beans.core.BeanObject</span>
	 *                                  <span class="zh-CN">如果目标类不是org.nervousync.beans.core.BeanObject的子类</span>
	 */
	public TransferBeanAdapter(final String className)
			throws IllegalArgumentException {
		this.beanClass = ClassUtils.forName(className);
	}

	@Override
	public String marshal(final Object object) {
		return BeanUtils.objectToString(object);
	}

	@Override
	public Object unmarshal(final Object string) {
		return Optional.ofNullable(string)
				.filter(obj -> obj instanceof String)
				.map(obj -> (String) obj)
				.filter(StringUtils::notBlank)
				.map(str -> BeanUtils.stringToObject(str, this.beanClass))
				.orElse(null);
	}
}
