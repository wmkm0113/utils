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

import org.nervousync.archive.enumerations.OpenMode;
import org.nervousync.utils.commons.StringUtils;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * <h2 class="en-US">Data file interface</h2>
 * <h2 class="zh-CN">数据文件接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public interface DataFile extends DataInput, DataOutput, Closeable {

	/**
	 * <h3 class="en-US">Get the current file path</h3>
	 * <h3 class="zh-CN">当前文件地址</h3>
	 *
	 * @return <span class="en-US">Current file path</span>
	 * <span class="zh-CN">当前文件地址</span>
	 */
	String filePath();

	/**
	 * <h3 class="en-US">Data file open mode</h3>
	 * <h3 class="zh-CN">文件打开模式</h3>
	 *
	 * @return <span class="en-US">Current file path</span>
	 * <span class="zh-CN">当前文件地址</span>
	 */
	OpenMode openMode();

	/**
	 * <h3 class="en-US">Read the current file total length</h3>
	 * <h3 class="zh-CN">读取当前文件的数据长度</h3>
	 *
	 * @return <span class="en-US">Total length</span>
	 * <span class="zh-CN">数据长度</span>
	 * @throws IOException <span class="en-US">If I/O error occurs when read file length failed</span>
	 *                     <span class="zh-CN">读取当前文件的数据长度时出现I/O错误</span>
	 */
	long length() throws IOException;

	/**
	 * <h3 class="en-US">Get the current file read pointer</h3>
	 * <h3 class="zh-CN">当前文件读取指针位置</h3>
	 *
	 * @return <span class="en-US">Current file read pointer</span>
	 * <span class="zh-CN">当前文件读取指针位置</span>
	 * @throws IOException <span class="en-US">If I/O error occurs when read file pointer</span>
	 *                     <span class="zh-CN">读取当前文件的读取指针时出现I/O错误</span>
	 */
	long getFilePointer() throws IOException;


	/**
	 * <h3 class="en-US">Sets the file-pointer offset</h3>
	 * <span class="en-US">
	 * Measured from the beginning of this file, at which the next read or write occurs.
	 * The offset may be set beyond the end of the file.
	 * Setting the offset beyond the end of the file does not change the file length.
	 * The file length will change only by writing after the offset has been set beyond the end of the file.
	 * </span>
	 * <h3 class="zh-CN">设置当前文件读取指针的位移</h3>
	 * <span class="zh-CN">
	 * 从该文件的开头开始测量，此时发生下一次读取或写入。 偏移量可以设置为超出文件末尾。
	 * 设置超出文件末尾的偏移量不会更改文件长度。 文件长度仅会在偏移量设置为超出文件末尾后写入才会更改。
	 * </span>
	 * Set the file-pointer to position
	 *
	 * @param pos target position
	 * @throws IOException Seek position failed
	 */
	void seek(final long pos) throws IOException;

	/**
	 * <h3 class="en-US">Read data</h3>
	 * <h3 class="zh-CN">读取数据</h3>
	 *
	 * @param buffer <span class="en-US">Read buffer</span>
	 *               <span class="zh-CN">读取缓冲区</span>
	 * @param offset <span class="en-US">Offset value</span>
	 *               <span class="zh-CN">偏移值</span>
	 * @param length <span class="en-US">Read length</span>
	 *               <span class="zh-CN">读取长度</span>
	 * @return <span class="en-US">Read length</span>
	 * <span class="zh-CN">读取长度</span>
	 * @throws IOException <span class="en-US">If an error occurs when read data</span>
	 *                     <span class="zh-CN">读取数据出错</span>
	 */
	int read(byte[] buffer, int offset, int length) throws IOException;

	/**
	 * <h3 class="en-US">Read split archive file list</h3>
	 * <h3 class="zh-CN">读取分卷文件列表</h3>
	 *
	 * @return <span class="en-US">Split archive file list</span>
	 * <span class="zh-CN">分卷文件列表</span>
	 * @throws IOException <span class="en-US">If an error occurs when read data</span>
	 *                     <span class="zh-CN">读取数据出错</span>
	 */
	List<String> splitFiles() throws IOException;

	/**
	 * <h3 class="en-US">Get current file open mode</h3>
	 * <h3 class="zh-CN">获取当前打开模式</h3>
	 *
	 * @return <span class="en-US">Open mode</span>
	 * <span class="zh-CN">打开模式</span>
	 */
	default String currentMode() {
		return (this.openMode() == OpenMode.READ) ? "r" : "rw";
	}

	/**
	 * <h3 class="en-US">Initialize a new data file instance object using current data file configure information</h3>
	 * <h3 class="zh-CN">使用此数据文件的配置信息生成新的数据文件对象</h3>
	 *
	 * @param filePath <span class="en-US">File path</span>
	 *                 <span class="zh-CN">文件路径</span>
	 * @return <span class="en-US">New data file instance object</span>
	 * <span class="zh-CN">新的数据文件对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open new data file</span>
	 *                     <span class="zh-CN">打开新文件出错</span>
	 */
	default DataFile newFile(final String filePath) throws IOException {
		return this.newFile(filePath, this.currentMode());
	}

	/**
	 * <h3 class="en-US">Initialize a new data file instance object using current data file configure information</h3>
	 * <h3 class="zh-CN">使用此数据文件的配置信息生成新的数据文件对象</h3>
	 *
	 * @param filePath <span class="en-US">File path</span>
	 *                 <span class="zh-CN">文件路径</span>
	 * @param mode     <span class="en-US">Data file open mode</span>
	 *                 <span class="zh-CN">文件打开模式</span>
	 * @return <span class="en-US">New data file instance object</span>
	 * <span class="zh-CN">新的数据文件对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open new data file</span>
	 *                     <span class="zh-CN">打开新文件出错</span>
	 */
	DataFile newFile(final String filePath, final String mode) throws IOException;

	/**
	 * <h3 class="en-US">Current data file is a split archive file</h3>
	 * <h3 class="zh-CN">当前文件为分卷归档文件</h3>
	 *
	 * @return <span class="en-US">Split archive file flag</span>
	 * <span class="zh-CN">分卷归档文件标记</span>
	 */
	default boolean splitArchive() {
		String fileName = StringUtils.getFilename(this.filePath());
		String[] items = StringUtils.tokenizeToStringArray(fileName, ".");
		if (items.length >= 3) {
			return !StringUtils.endsWithIgnoreCase(items[items.length - 2], "zip");
		} else {
			return !StringUtils.endsWithIgnoreCase(items[items.length - 1], "zip");
		}
	}
}
