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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.nervousync.commons.Globals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <h2 class="en-US">Internationalization Resource Data</h2>
 * <h2 class="zh-CN">国际化资源数据</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 19, 2023 16:46:22 $
 */
public final class BundleResource implements Serializable {

	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = -6512620733960415512L;
	/**
	 * <span class="en-US">Organization identification code</span>
	 * <span class="zh-CN">组织识别代码</span>
	 */
	private String groupId;
	/**
	 * <span class="en-US">Project identification code</span>
	 * <span class="zh-CN">项目识别代码</span>
	 */
	private String bundle;
	/**
	 * <span class="en-US">Error codes and message key mapping table</span>
	 * <span class="zh-CN">错误代码与信息识别代码的映射表</span>
	 */
	@JsonProperty("errors")
	private Map<String, String> errorCodes;
	/**
	 * <span class="en-US">Mapping of message language code and message content</span>
	 * <span class="zh-CN">信息语言代码与信息内容的定义映射表</span>
	 */
	@JsonProperty("messages")
	private Map<String, Map<String, BundleMessage>> bundleMessages;

	/**
	 * <h3 class="en-US">Constructor for internationalization resource data</h3>
	 * <h3 class="zh-CN">国际化资源数据的构造方法</h3>
	 */
	public BundleResource() {
		this.groupId = Globals.DEFAULT_VALUE_STRING;
		this.bundle = Globals.DEFAULT_VALUE_STRING;
		this.errorCodes = new HashMap<>();
		this.bundleMessages = new HashMap<>();
	}

	/**
	 * <h3 class="en-US">Getter method for organization identification code</h3>
	 * <h3 class="zh-CN">组织识别代码的Getter方法</h3>
	 *
	 * @return <span class="en-US">Organization identification code</span>
	 * <span class="zh-CN">组织识别代码</span>
	 */
	public String getGroupId() {
		return this.groupId;
	}

	/**
	 * <h3 class="en-US">Setter method for organization identification code</h3>
	 * <h3 class="zh-CN">组织识别代码的Setter方法</h3>
	 *
	 * @param groupId <span class="en-US">Organization identification code</span>
	 *                <span class="zh-CN">组织识别代码</span>
	 */
	public void setGroupId(final String groupId) {
		this.groupId = groupId;
	}

	/**
	 * <h3 class="en-US">Getter method for project identification code</h3>
	 * <h3 class="zh-CN">项目识别代码的Getter方法</h3>
	 *
	 * @return <span class="en-US">Project identification code</span>
	 * <span class="zh-CN">项目识别代码</span>
	 */
	public String getBundle() {
		return this.bundle;
	}

	/**
	 * <h3 class="en-US">Setter method for project identification code</h3>
	 * <h3 class="zh-CN">项目识别代码的Setter方法</h3>
	 *
	 * @param bundle <span class="en-US">Project identification code</span>
	 *               <span class="zh-CN">项目识别代码</span>
	 */
	public void setBundle(final String bundle) {
		this.bundle = bundle;
	}

	/**
	 * <h3 class="en-US">Getter method for the error codes and message key mapping table</h3>
	 * <h3 class="zh-CN">错误代码与信息识别代码的映射表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Error codes and message key mapping table</span>
	 * <span class="zh-CN">错误代码与信息识别代码的映射表</span>
	 */
	public Map<String, String> getErrorCodes() {
		return this.errorCodes;
	}

	/**
	 * <h3 class="en-US">Setter method for the error codes and message key mapping table</h3>
	 * <h3 class="zh-CN">错误代码与信息识别代码的映射表的 Setter 方法</h3>
	 *
	 * @param errorCodes <span class="en-US">Error codes and message key mapping table</span>
	 *                   <span class="zh-CN">错误代码与信息识别代码的映射表</span>
	 */
	public void setErrorCodes(final Map<String, String> errorCodes) {
		this.errorCodes = errorCodes;
	}

	/**
	 * <h3 class="en-US">Getter method for the mapping of message language code and message content</h3>
	 * <h3 class="zh-CN">信息语言代码与信息内容的定义映射表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Mapping of message language code and message content</span>
	 * <span class="zh-CN">信息语言代码与信息内容的定义映射表</span>
	 */
	public Map<String, Map<String, BundleMessage>> getBundleMessages() {
		return this.bundleMessages;
	}

	/**
	 * <h3 class="en-US">Setter method for the mapping of message language code and message content</h3>
	 * <h3 class="zh-CN">信息语言代码与信息内容的定义映射表的 Setter 方法</h3>
	 *
	 * @param bundleMessages <span class="en-US">Mapping of message language code and message content</span>
	 *                       <span class="zh-CN">信息语言代码与信息内容的定义映射表</span>
	 */
	public void setBundleMessages(final Map<String, Map<String, BundleMessage>> bundleMessages) {
		this.bundleMessages = bundleMessages;
	}
}
