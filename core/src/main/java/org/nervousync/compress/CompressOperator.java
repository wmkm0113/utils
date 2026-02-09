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

package org.nervousync.compress;

import org.nervousync.beans.files.TargetPath;

import java.io.IOException;
import java.io.InputStream;

/**
 * <h2 class="en-US">Compress file operator interface</h2>
 * <h2 class="zh-CN">压缩文件操作接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 31, 2023 16:27:08 $
 */
public interface CompressOperator {

	/**
	 * <h3 class="en-US">Open the input stream of the given compress file entry path defines</h3>
	 * <h3 class="zh-CN">打开给定压缩包内文件路径定义的资源路径输入流</h3>
	 *
	 * @param targetPath <span class="en-US">Compress file entry path defines</span>
	 *                   <span class="zh-CN">压缩包内文件路径定义</span>
	 * @return <span class="en-US">input stream instance</span>
	 * <span class="zh-CN">输入流实例对象</span>
	 * @throws IOException <span class="en-US">when opening input stream error</span>
	 *                     <span class="zh-CN">打开输入流时出错</span>
	 */
	InputStream entryInputStream(final TargetPath targetPath) throws IOException;

	/**
	 * <h3 class="en-US">Read the contents of a given path within a compressed file.</h3>
	 * <h3 class="zh-CN">读取给定压缩文件内路径的内容</h3>
	 *
	 * @param targetPath <span class="en-US">Compress file entry path defines</span>
	 *                   <span class="zh-CN">压缩包内文件路径定义</span>
	 * @return <span class="en-US">Read data bytes array</span>
	 * <span class="zh-CN">读取的字节数组</span>
	 * @throws IOException <span class="en-US">when opening input stream error</span>
	 *                     <span class="zh-CN">打开输入流时出错</span>
	 */
	byte[] entryBytes(final TargetPath targetPath) throws IOException;

	/**
	 * <h3 class="en-US">Read the size of a given path within a compressed file.</h3>
	 * <h3 class="zh-CN">读取给定压缩文件内路径的大小</h3>
	 *
	 * @param targetPath <span class="en-US">Compress file entry path defines</span>
	 *                   <span class="zh-CN">压缩包内文件路径定义</span>
	 * @return <span class="en-US">File size</span>
	 * <span class="zh-CN">文件大小</span>
	 * @throws IOException <span class="en-US">when opening input stream error</span>
	 *                     <span class="zh-CN">打开输入流时出错</span>
	 */
	long entryLength(final TargetPath targetPath) throws IOException;

	/**
	 * <h3 class="en-US">Check the existed status of a given path within a compressed file.</h3>
	 * <h3 class="zh-CN">检查给定压缩文件内路径是否存在</h3>
	 *
	 * @param targetPath <span class="en-US">Compress file entry path defines</span>
	 *                   <span class="zh-CN">压缩包内文件路径定义</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 * @throws IOException <span class="en-US">when opening input stream error</span>
	 *                     <span class="zh-CN">打开输入流时出错</span>
	 */
	boolean entryExists(final TargetPath targetPath) throws IOException;
}
