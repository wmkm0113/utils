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

package org.nervousync.archive.adaptor;

import jakarta.annotation.Nonnull;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.enumerations.OpenMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ExtractOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.Optional;

/**
 * <h2 class="en-US">Abstract class of the archive adaptor</h2>
 * <h2 class="zh-CN">归档适配器抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public abstract class ArchiveAdaptor implements AutoCloseable {

	/**
	 * <span class="en-US">Current zip file path</span>
	 * <span class="zh-CN">当前压缩文件路径</span>
	 */
	protected final String filePath;
	/**
	 * <span class="en-US">Enumeration value of the open mode</span>
	 * <span class="zh-CN">打开模式枚举值</span>
	 */
	protected final OpenMode openMode;
	/**
	 * <span class="en-US">Archive entry mapping table</span>
	 * <span class="zh-CN">归档条目映射表</span>
	 */
	protected final Hashtable<String, ArchiveEntry> entries = new Hashtable<>();

	/**
	 * <h3 class="en-US">Constructor method for the abstract class of the archive adaptor</h3>
	 * <h3 class="zh-CN">归档适配器抽象类的构造方法</h3>
	 *
	 * @param filePath <span class="en-US">Current zip file path</span>
	 *                 <span class="zh-CN">当前压缩文件路径</span>
	 * @param openMode <span class="en-US">Enumeration value of the open mode</span>
	 *                 <span class="zh-CN">打开模式枚举值</span>
	 */
	protected ArchiveAdaptor(final String filePath, final OpenMode openMode) {
		this.filePath = filePath;
		this.openMode = openMode;
	}

	/**
	 * <h3 class="en-US">Getter method for the current zip file path</h3>
	 * <h3 class="zh-CN">当前压缩文件路径的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Current zip file path</span>
	 * <span class="zh-CN">当前压缩文件路径</span>
	 */
	public String getFilePath() {
		return this.filePath;
	}

	/**
	 * <h3 class="en-US">Get the open mode of current adaptor</h3>
	 * <h3 class="zh-CN">获取当前的打开模式</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the open mode</span>
	 * <span class="zh-CN">打开模式枚举值</span>
	 */
	public final OpenMode openMode() {
		return this.openMode;
	}

	/**
	 * <h3 class="en-US">Get the archive entry</h3>
	 * <h3 class="zh-CN">获取归档条目</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @return <span class="en-US">Archive entry</span>
	 * <span class="zh-CN">归档条目</span>
	 */
	public final Optional<ArchiveEntry> entry(final String entryPath) {
		return Optional.ofNullable(this.entries.get(entryPath));
	}

	/**
	 * <h3 class="en-US">Merge the split archive file</h3>
	 * <h3 class="zh-CN">合并分卷文件</h3>
	 *
	 * @param target <span class="en-US">The target path</span>
	 *               <span class="zh-CN">目标地址</span>
	 * @throws IOException <span class="en-US">If an error occurs when open resource reader</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public void merge(@Nonnull final Path target) throws IOException {
		throw new ArchiveException(0x0000001A004BL);
	}

	/**
	 * <h3 class="en-US">Get the resource reader of the given archive entry</h3>
	 * <h3 class="zh-CN">获取指定条目的资源读取器</h3>
	 *
	 * @param archiveEntry <span class="en-US">The given archive entry</span>
	 *                     <span class="zh-CN">指定归档条目</span>
	 * @return <span class="en-US">Resource reader instance object</span>
	 * <span class="zh-CN">资源读取器实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open resource reader</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public abstract Optional<ResourceReader> openReader(final ArchiveEntry archiveEntry) throws IOException;

	/**
	 * <h3 class="en-US">Get the resource writer of the given archive entry</h3>
	 * <h3 class="zh-CN">获取指定条目的资源写入器</h3>
	 *
	 * @param entryPath <span class="en-US">The entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @param comment   <span class="en-US">Comment string</span>
	 *                  <span class="zh-CN">批注信息</span>
	 * @return <span class="en-US">Resource writer instance object</span>
	 * <span class="zh-CN">资源写入器实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open resource reader</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public abstract Optional<ResourceWriter> openWriter(final String entryPath, final String comment) throws IOException;

	/**
	 * <h3 class="en-US">Extract to the target path</h3>
	 * <h3 class="zh-CN">释放到目标地址</h3>
	 *
	 * @param path    <span class="en-US">The target path</span>
	 *                <span class="zh-CN">目标地址</span>
	 * @param options <span class="en-US">Extract options</span>
	 *                <span class="zh-CN">解压选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when extract files</span>
	 *                     <span class="zh-CN">释放文件执行中出错</span>
	 */
	public abstract void extract(final Path path, final ExtractOptions options) throws IOException;

	/**
	 * <h3 class="en-US">Remove the given archive entry</h3>
	 * <h3 class="zh-CN">删除指定条目</h3>
	 *
	 * @param archiveEntry <span class="en-US">The given archive entry</span>
	 *                     <span class="zh-CN">指定归档条目</span>
	 * @throws IOException <span class="en-US">If an error occurs when open resource reader</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public abstract void remove(final ArchiveEntry archiveEntry) throws IOException;

	/**
	 * <h3 class="en-US">Commit the modify</h3>
	 * <h3 class="zh-CN">提交修改</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when commit modify</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public abstract void commit() throws IOException;

	/**
	 * <h3 class="en-US">Rollback the modify</h3>
	 * <h3 class="zh-CN">回滚修改</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when rollback modify</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public abstract void rollback() throws IOException;
}
