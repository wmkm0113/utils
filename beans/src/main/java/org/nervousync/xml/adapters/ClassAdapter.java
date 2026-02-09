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
package org.nervousync.xml.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.nervousync.commons.Globals;
import org.nervousync.utils.core.ClassUtils;
import org.nervousync.utils.core.StringUtils;

import java.util.Optional;

/**
 * <h2 class="en-US">Class DataConverter</h2>
 * <h2 class="zh-CN">Class数据转换器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 29, 2023 17:19:28 $
 */
@SuppressWarnings("unused")
public final class ClassAdapter extends XmlAdapter<String, Class<?>> {

	@Override
	public Class<?> unmarshal(final String className) {
		return StringUtils.isEmpty(className) ? null : ClassUtils.forName(className);
	}

	@Override
	public String marshal(final Class<?> clazz) {
		return Optional.ofNullable(clazz).map(ClassUtils::originalClassName).orElse(Globals.DEFAULT_VALUE_STRING);
	}
}
