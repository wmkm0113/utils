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

package org.nervousync.beans.i18n;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.nervousync.commons.Globals;

import java.io.Serializable;
import java.util.List;

/**
 * <h2 class="en-US">Internationalization Information Data</h2>
 * <h2 class="zh-CN">国际化信息数据</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 19, 2023 16:51:46 $
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class BundleMessage implements Serializable {

	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = -3554191239649090032L;

	/**
	 * <span class="en-US">Message content</span>
	 * <span class="zh-CN">信息内容</span>
	 */
	@JsonProperty
	private String pattern = Globals.DEFAULT_VALUE_STRING;
	/**
	 * <span class="en-US">List of parameter names defined by the pluralization rules</span>
	 * <span class="zh-CN">Plural规则定义的参数名列表</span>
	 */
	@JsonProperty
	private List<String> arguments = null;

	/**
	 * <h3 class="en-US">Getter method for the message content</h3>
	 * <h3 class="zh-CN">信息内容的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Message content</span>
	 * <span class="zh-CN">信息内容</span>
	 */
	public String getPattern() {
		return this.pattern;
	}

	/**
	 * <h3 class="en-US">Setter method for the message content</h3>
	 * <h3 class="zh-CN">信息内容的 Setter 方法</h3>
	 *
	 * @param pattern <span class="en-US">Message content</span>
	 *                <span class="zh-CN">信息内容</span>
	 */
	public void setPattern(final String pattern) {
		this.pattern = pattern;
	}

	/**
	 * <h3 class="en-US">Getter method for the list of parameter names defined by the pluralization rules</h3>
	 * <h3 class="zh-CN">Plural规则定义的参数名列表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">List of parameter names defined by the pluralization rules</span>
	 * <span class="zh-CN">Plural规则定义的参数名列表</span>
	 */
	public List<String> getArguments() {
		return this.arguments;
	}

	/**
	 * <h3 class="en-US">Setter method for the list of parameter names defined by the pluralization rules</h3>
	 * <h3 class="zh-CN">Plural规则定义的参数名列表的 Setter 方法</h3>
	 *
	 * @param arguments <span class="en-US">List of parameter names defined by the pluralization rules</span>
	 *                  <span class="zh-CN">Plural规则定义的参数名列表</span>
	 */
	public void setArguments(final List<String> arguments) {
		this.arguments = arguments;
	}
}
