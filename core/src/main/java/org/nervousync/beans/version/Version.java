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

package org.nervousync.beans.version;

import org.nervousync.commons.Globals;
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.core.StringUtils;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * <h2 class="en-US">Version information</h2>
 * <h2 class="zh-CN">版本信息</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.0 $ $Date: Jun 21, 2023 10:25:22 $
 */
public final class Version implements Serializable {

	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = -1007568628615392183L;

	/**
	 * <span class="en-US">Major version number</span>
	 * <span class="zh-CN">主版本号</span>
	 */
	private final int major;
	/**
	 * <span class="en-US">Minor version number</span>
	 * <span class="zh-CN">子版本号</span>
	 */
	private final int minor;
	/**
	 * <span class="en-US">Patch version number</span>
	 * <span class="zh-CN">补丁版本号</span>
	 */
	private final int patch;
	/**
	 * <span class="en-US">Building UTC timestamp</span>
	 * <span class="zh-CN">构建时间戳（UTC）</span>
	 */
	private final long timestamp;

	/**
	 * <h3 class="en-US">Constructor method for the version information</h3>
	 * <h3 class="zh-CN">版本信息的构造方法</h3>
	 *
	 * @param major     <span class="en-US">Major version number</span>
	 *                  <span class="zh-CN">主版本号</span>
	 * @param minor     <span class="en-US">Minor version number</span>
	 *                  <span class="zh-CN">子版本号</span>
	 * @param patch     <span class="en-US">Patch version number</span>
	 *                  <span class="zh-CN">补丁版本号</span>
	 * @param timestamp <span class="en-US">Building UTC timestamp</span>
	 *                  <span class="zh-CN">构建时间戳（UTC）</span>
	 */
	public Version(final int major, final int minor, final int patch, final long timestamp) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.timestamp = timestamp;
	}

	/**
	 * <h3 class="en-US">Getter method for </h3>
	 * <h3 class="zh-CN">的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Major version number</span>
	 * <span class="zh-CN">主版本号</span>
	 */
	public int getMajor() {
		return Math.max(Globals.INITIALIZE_INT_VALUE, this.major);
	}

	/**
	 * <h3 class="en-US">Getter method for </h3>
	 * <h3 class="zh-CN">的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Minor version number</span>
	 * <span class="zh-CN">子版本号</span>
	 */
	public int getMinor() {
		return Math.max(Globals.INITIALIZE_INT_VALUE, this.minor);
	}

	/**
	 * <h3 class="en-US">Getter method for </h3>
	 * <h3 class="zh-CN">的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Patch version number</span>
	 * <span class="zh-CN">补丁版本号</span>
	 */
	public int getPatch() {
		return Math.max(Globals.INITIALIZE_INT_VALUE, this.patch);
	}

	/**
	 * <h3 class="en-US">Getter method for </h3>
	 * <h3 class="zh-CN">的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Building UTC timestamp</span>
	 * <span class="zh-CN">构建时间戳（UTC）</span>
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder(Math.max(Globals.INITIALIZE_INT_VALUE, this.major));
		stringBuilder.append(".").append(Math.max(Globals.INITIALIZE_INT_VALUE, this.minor));
		stringBuilder.append(".").append(Math.max(Globals.INITIALIZE_INT_VALUE, this.patch));
		Optional.ofNullable(DateTimeUtils.utcToLocal(this.timestamp))
				.map(localDateTime -> localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")))
				.filter(StringUtils::notBlank)
				.ifPresent(timestamp -> stringBuilder.append("-").append(timestamp));
		return stringBuilder.toString();
	}
}
