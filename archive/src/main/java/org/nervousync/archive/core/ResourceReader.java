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

import org.nervousync.archive.options.ExtractOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * <h2 class="en-US">Resource reader interface</h2>
 * <h2 class="zh-CN">资源读取器接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public interface ResourceReader extends ArchiveResource {

	/**
	 * <h3 class="en-US">Open input stream</h3>
	 * <h3 class="zh-CN">打开输入流</h3>
	 *
	 * @return <span class="en-US">Input stream instance object</span>
	 * <span class="zh-CN">输入流实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open input stream</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	InputStream openStream() throws IOException;

	/**
	 * <h3 class="en-US">Extract to the target destination</h3>
	 * <h3 class="zh-CN">释放到目标位置</h3>
	 *
	 * @param destination <span class="en-US">Target path</span>
	 *                    <span class="zh-CN">目标路径</span>
	 * @param options     <span class="en-US">Extract options</span>
	 *                    <span class="zh-CN">释放选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when open input stream</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	void extract(final Path destination, final ExtractOptions options) throws IOException;
}
