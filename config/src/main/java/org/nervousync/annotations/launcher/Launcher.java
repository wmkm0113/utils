/*
 * Licensed to the Nervousync Studio (NSYC) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
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
package org.nervousync.annotations.launcher;

import org.nervousync.enumerations.launcher.StartupType;

import java.lang.annotation.*;

/**
 * <h3 class="en-US">Annotation class for startup launcher class</h3>
 * <h3 class="zh-CN">启动器类的标注</h3>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jun 28, 2022 15:25:33 $
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Launcher {
	/**
	 * <span class="en-US">Enumeration value of startup type, default value: MANUAL</span>
	 * <span class="zh-CN">启动类型枚举值，默认：手动（MANUAL）</span>
	 *
	 * @return <span class="en-US">Enumeration value of startup type</span>
	 * <span class="zh-CN">启动类型枚举值</span>
	 */
	StartupType value() default StartupType.MANUAL;
}
