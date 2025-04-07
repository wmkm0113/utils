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

import java.lang.annotation.*;

/**
 * <h2 class="en-US">Annotation for the digital signature</h2>
 * <h2 class="zh-CN">标注用于数字签名</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Apr 03, 2025 14:27:15 $
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Signature {

	/**
	 * <span class="en-US">Field name for saving the digital signature value</span>
	 * <span class="zh-CN">用于保存数字签名的属性名</span>
	 *
	 * @return <span class="en-US">Field name</span>
	 * <span class="zh-CN">属性名</span>
	 */
	String value();
}
