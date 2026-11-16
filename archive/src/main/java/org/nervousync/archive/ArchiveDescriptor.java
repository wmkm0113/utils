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

package org.nervousync.archive;

import org.nervousync.archive.enumerations.ContainerFormat;

/**
 * <h2 class="en-US">Archive descriptor</h2>
 * <h2 class="zh-CN">归档描述器</h2>
 *
 * @param format      <span class="en-US">Enumeration value of the archive container format</span>
 *                    <span class="zh-CN">归档容器格式枚举值</span>
 * @param compression <span class="en-US">Compression descriptor</span>
 *                    <span class="zh-CN">压缩描述器</span>
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public record ArchiveDescriptor(ContainerFormat format, CompressionDescriptor compression) {

	/**
	 * <h3 class="en-US">Constructor method for the archive descriptor</h3>
	 * <h3 class="zh-CN">归档描述器的构造方法</h3>
	 *
	 * @param format      <span class="en-US">Enumeration value of the archive container format</span>
	 *                    <span class="zh-CN">归档容器格式枚举值</span>
	 * @param compression <span class="en-US">Compression descriptor</span>
	 *                    <span class="zh-CN">压缩描述器</span>
	 */
	public ArchiveDescriptor {
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the archive container format</h3>
	 * <h3 class="zh-CN">归档容器格式枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the archive container format</span>
	 * <span class="zh-CN">归档容器格式枚举值</span>
	 */
	@Override
	public ContainerFormat format() {
		return this.format;
	}

	/**
	 * <h3 class="en-US">Getter method for the compression descriptor</h3>
	 * <h3 class="zh-CN">压缩描述器的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Compression descriptor</span>
	 * <span class="zh-CN">压缩描述器</span>
	 */
	@Override
	public CompressionDescriptor compression() {
		return this.compression;
	}
}
