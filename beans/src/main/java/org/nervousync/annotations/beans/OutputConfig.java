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

package org.nervousync.annotations.beans;

import org.nervousync.commons.Globals;
import org.nervousync.enumerations.beans.StringType;

import java.lang.annotation.*;

/**
 * <h2 class="en-US">Annotation for data output config</h2>
 * <span class="en-US">Configure output data type, formatted output string, and string encoding</span>
 * <h2 class="zh-CN">标注用于数据输出的配置</h2>
 * <span class="en-US">定义输出的数据类型，是否格式化输出的字符串以及输出字符串的编码集</span>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Apr 15, 2023 14:27:15 $
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface OutputConfig {

	/**
	 * <h3 class="en-US">Enumeration of String Type</h3>
	 * <span class="en-US">Default is XML</span>
	 * <h3 class="zh-CN">字符串类型的枚举类</h3>
	 * <span class="zh-CN">默认值为 XML</span>
	 *
	 * @return <span class="en-US">Enumeration of String Type</span>
	 * <span class="zh-CN">字符串类型的枚举类</span>
	 */
	StringType type() default StringType.XML;

	/**
	 * <h3 class="en-US">Output string encoding</h3>
	 * <span class="en-US">Default is UTF-8</span>
	 * <h3 class="zh-CN">输出字符串编码集</h3>
	 * <span class="zh-CN">默认值为UTF-8</span>
	 *
	 * @return <span class="en-US">String encoding</span>
	 * <span class="zh-CN">字符串编码集</span>
	 */
	String encoding() default Globals.DEFAULT_ENCODING;
}
