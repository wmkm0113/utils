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

package org.nervousync.beans.launcher;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.nervousync.beans.core.BeanObject;
import org.nervousync.commons.Globals;

import java.util.ArrayList;
import java.util.List;


/**
 * <h2 class="en-US">Startup manager configure information</h2>
 * <h2 class="zh-CN">启动管理器配置信息</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Feb 27, 2024 11:07:46 $
 */
@XmlType(name = "startup_config", namespace = "https://nervousync.org/schemas/startup")
@XmlRootElement(name = "startup_config", namespace = "https://nervousync.org/schemas/startup")
public final class StartupConfig extends BeanObject {

	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = 2193879449739699710L;

	/**
	 * <span class="en-US">Last modify time milliseconds</span>
	 * <span class="zh-CN">最后修改时间毫秒数</span>
	 */
	@XmlElement(name = "last_modify")
	private long lastModify = Globals.DEFAULT_VALUE_LONG;
	/**
	 * <span class="en-US">Registered startup launcher configure information list</span>
	 * <span class="zh-CN">已注册的启动器配置信息列表</span>
	 */
	@XmlElement(name = "launcher_config", namespace = "https://nervousync.org/schemas/startup")
	@XmlElementWrapper(name = "registered_launchers")
	private List<LauncherConfig> registeredLaunchers = new ArrayList<>();

	/**
	 * <h3 class="en-US">Constructor method for Startup manager configure information</h3>
	 * <h3 class="zh-CN">启动管理器配置信息的构造方法</h3>
	 */
	public StartupConfig() {
	}

	/**
	 * <h3 class="en-US">Getter method for last modify time milliseconds</h3>
	 * <h3 class="zh-CN">最后修改时间毫秒数的Getter方法</h3>
	 *
	 * @return <span class="en-US">Last modify time milliseconds</span>
	 * <span class="zh-CN">最后修改时间毫秒数</span>
	 */
	public long getLastModify() {
		return this.lastModify;
	}

	/**
	 * <h3 class="en-US">Setter method for last modify time milliseconds</h3>
	 * <h3 class="zh-CN">最后修改时间毫秒数的Setter方法</h3>
	 *
	 * @param lastModify <span class="en-US">Last modify time milliseconds</span>
	 *                   <span class="zh-CN">最后修改时间毫秒数</span>
	 */
	public void setLastModify(final long lastModify) {
		this.lastModify = lastModify;
	}

	/**
	 * <h3 class="en-US">Getter method for the registered startup launcher configuring information list</h3>
	 * <h3 class="zh-CN">已注册的启动器配置信息列表的Getter方法</h3>
	 *
	 * @return <span class="en-US">Registered startup launcher configure information list</span>
	 * <span class="zh-CN">已注册的启动器配置信息列表</span>
	 */
	public List<LauncherConfig> getRegisteredLaunchers() {
		return this.registeredLaunchers;
	}

	/**
	 * <h3 class="en-US">Setter method for the registered startup launcher configuring information list</h3>
	 * <h3 class="zh-CN">已注册的启动器配置信息列表的Setter方法</h3>
	 *
	 * @param registeredLaunchers <span class="en-US">Registered startup launcher configure information list</span>
	 *                            <span class="zh-CN">已注册的启动器配置信息列表</span>
	 */
	public void setRegisteredLaunchers(final List<LauncherConfig> registeredLaunchers) {
		this.registeredLaunchers = registeredLaunchers;
	}
}
