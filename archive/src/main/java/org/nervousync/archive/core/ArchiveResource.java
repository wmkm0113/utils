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

package org.nervousync.archive.core;

/**
 * <h2 class="en-US">Archive resource interface</h2>
 * <h2 class="zh-CN">归档资源接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public interface ArchiveResource extends AutoCloseable {

	/**
	 * <h3 class="en-US">get the resource name</h3>
	 * <h3 class="zh-CN">获取资源名称</h3>
	 *
	 * @return <span class="en-US">Resource name</span>
	 * <span class="zh-CN">资源名称</span>
	 */
	String path();
}
