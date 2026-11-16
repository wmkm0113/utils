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

import jakarta.annotation.Nonnull;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.utils.commons.DateTimeUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <h2 class="en-US">Resource writer interface</h2>
 * <h2 class="zh-CN">资源写入器接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public interface ResourceWriter extends ArchiveResource, AutoCloseable {

	/**
	 * <h3 class="en-US">Write file to current entry</h3>
	 * <h3 class="zh-CN">将文件写入当前条目</h3>
	 *
	 * @param filePath <span class="en-US">File path</span>
	 *                 <span class="zh-CN">文件地址</span>
	 * @throws IOException <span class="en-US">If an error occurs when write file to current entry</span>
	 *                     <span class="zh-CN">写入过程中出错</span>
	 */
	default void write(@Nonnull final String filePath) throws IOException {
		this.write(Path.of(filePath));
	}

	/**
	 * <h3 class="en-US">Write file to current entry</h3>
	 * <h3 class="zh-CN">将文件写入当前条目</h3>
	 *
	 * @param file <span class="en-US">File instance object</span>
	 *             <span class="zh-CN">文件对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when write file to current entry</span>
	 *                     <span class="zh-CN">写入过程中出错</span>
	 */
	default void write(@Nonnull final File file) throws IOException {
		this.write(file.toPath());
	}

	/**
	 * <h3 class="en-US">Write file to current entry</h3>
	 * <h3 class="zh-CN">将文件写入当前条目</h3>
	 *
	 * @param path <span class="en-US">File path</span>
	 *             <span class="zh-CN">文件路径</span>
	 * @throws IOException <span class="en-US">If an error occurs when write file to current entry</span>
	 *                     <span class="zh-CN">写入过程中出错</span>
	 */
	default void write(final Path path) throws IOException {
		try (OutputStream outputStream =
				     this.openStream(DateTimeUtils.toDosTime(Files.getLastModifiedTime(path).toMillis()))) {
			Files.copy(path, outputStream);
		}
	}

	/**
	 * <h3 class="en-US">Open output stream with entry metadata</h3>
	 * <h3 class="zh-CN">使用条目属性打开输出流</h3>
	 *
	 * @param entry <span class="en-US">Archive entry containing metadata</span>
	 *              <span class="zh-CN">包含文件属性的归档条目</span>
	 * @return <span class="en-US">Output stream instance object</span>
	 * <span class="zh-CN">输出流实例对象</span>
	 * @throws IOException <span class="en-US">If the output stream cannot be opened</span>
	 *                     <span class="zh-CN">无法打开输出流时抛出</span>
	 */
	default OutputStream openStream(@Nonnull final ArchiveEntry entry) throws IOException {
		return this.openStream(DateTimeUtils.toDosTime(entry.getLastModified().toEpochMilli()));
	}

	/**
	 * <h3 class="en-US">Open output stream</h3>
	 * <h3 class="zh-CN">打开输出流</h3>
	 *
	 * @param lastModified <span class="en-US">Last modified timestamp</span>
	 *                     <span class="zh-CN">最后修改时间</span>
	 * @return <span class="en-US">Output stream instance object</span>
	 * <span class="zh-CN">输出流实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open output stream</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	OutputStream openStream(final long lastModified) throws IOException;

	/**
	 * <h3 class="en-US">Abort current operate</h3>
	 * <h3 class="zh-CN">终止当前操作</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when abort operate</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	void abort() throws IOException;

	/**
	 * <h3 class="en-US">Finish current operate</h3>
	 * <h3 class="zh-CN">写入完成</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when abort operate</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	void finish() throws IOException;
}
