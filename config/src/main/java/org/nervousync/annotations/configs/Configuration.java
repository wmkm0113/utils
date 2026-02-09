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

package org.nervousync.annotations.configs;

import org.nervousync.commons.Globals;

import java.lang.annotation.*;

/**
 * <h2 class="en-US">Annotation for configuration class define</h2>
 * <h2 class="zh-CN">配置信息数据的注解</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Sep 25, 2022 14:31:08 $
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Configuration {

	/**
	 * @return <span class="en-US">Configure file suffix name</span>
	 * <span class="zh-CN">配置文件后缀名</span>
	 */
	String value() default Globals.DEFAULT_VALUE_STRING;
}
