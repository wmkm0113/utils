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
package org.nervousync.enumerations.beans;

/**
 * <h2 class="en-US">XML Data Type Enumerations</h2>
 * <h2 class="zh-CN">XML数据类型枚举</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Sep 28, 2009 14:32:00 $
 */
public enum DataType {
	/**
     * <span class="en-US">Binary Data</span>
     * <span class="zh-CN">二进制字节数组</span>
	 */
	BINARY,
	/**
     * <span class="en-US">Boolean Data</span>
     * <span class="zh-CN">布尔值</span>
	 */
	BOOLEAN,
	/**
	 * <span class="en-US">Year-month Data</span>
	 * <span class="zh-CN">年度月份数据</span>
	 */
	MONTH,
	/**
     * <span class="en-US">Date Data</span>
     * <span class="zh-CN">日期数据</span>
	 */
	DATE,
	/**
	 * <span class="en-US">Time Data</span>
	 * <span class="zh-CN">时间时间</span>
	 */
	TIME,
	/**
	 * <span class="en-US">DateTime Data</span>
	 * <span class="zh-CN">日期时间</span>
	 */
	DATE_TIME,
	/**
     * <span class="en-US">Enumeration Data</span>
     * <span class="zh-CN">枚举值</span>
	 */
	ENUM,
	/**
     * <span class="en-US">Unknown Data</span>
     * <span class="zh-CN">未知</span>
	 */
	UNKNOWN,
	/**
     * <span class="en-US">Number Data</span>
     * <span class="zh-CN">数字</span>
	 */
	NUMBER,
	/**
     * <span class="en-US">String Data</span>
     * <span class="zh-CN">字符串</span>
	 */
	STRING,
	/**
     * <span class="en-US">Object Data</span>
     * <span class="zh-CN">对象实例</span>
	 */
	OBJECT,
	/**
     * <span class="en-US">Cdata String</span>
     * <span class="zh-CN">Cdata文本</span>
	 */
	CDATA
}
