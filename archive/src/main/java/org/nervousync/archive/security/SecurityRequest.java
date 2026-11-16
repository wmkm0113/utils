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

package org.nervousync.archive.security;

import java.util.Objects;

/**
 * <h2 class="en-US">Abstract class of security request</h2>
 * <h2 class="zh-CN">安全请求抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public abstract class SecurityRequest {

	/**
	 * <span class="en-US">Archive file path</span>
	 * <span class="zh-CN">归档文件地址</span>
	 */
	private final String archivePath;
	/**
	 * <span class="en-US">Entry path</span>
	 * <span class="zh-CN">条目地址</span>
	 */
	private final String entryPath;

	protected SecurityRequest(final String archivePath, final String entryPath) {
		this.archivePath = Objects.requireNonNull(archivePath, "archivePath");
		this.entryPath = Objects.requireNonNull(entryPath, "entryPath");
	}

	/**
	 * <h3 class="en-US">Getter method for the archive file path</h3>
	 * <h3 class="zh-CN">归档文件地址的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Archive file path</span>
	 * <span class="zh-CN">归档文件地址</span>
	 */
	public final String getArchivePath() {
		return this.archivePath;
	}

	/**
	 * <h3 class="en-US">Getter method for the entry path</h3>
	 * <h3 class="zh-CN">条目地址的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Entry path</span>
	 * <span class="zh-CN">条目地址</span>
	 */
	public final String getEntryPath() {
		return this.entryPath;
	}
}
