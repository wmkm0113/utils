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

package org.nervousync.archive.zip.models.central;

import jakarta.annotation.Nonnull;
import org.nervousync.archive.enumerations.DuplicatePolicy;
import org.nervousync.archive.enumerations.EntryType;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.ZipAdaptor;
import org.nervousync.archive.zip.models.header.GeneralFileHeader;
import org.nervousync.utils.commons.FileUtils;
import org.nervousync.utils.commons.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * <h2 class="en-US">Central directory define</h2>
 * <h2 class="zh-CN">中央仓库定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class CentralDirectory {

	/**
	 * <span class="en-US">General file header define list</span>
	 * <span class="zh-CN">文件头定义列表</span>
	 */
	private final List<GeneralFileHeader> fileHeaders;
	/**
	 * <span class="en-US">Entry path to general file header mapping table</span>
	 * <span class="zh-CN">条目路径和文件头定义映射表</span>
	 */
	private final LinkedHashMap<String, GeneralFileHeader> selectedHeaders = new LinkedHashMap<>();
	/**
	 * <span class="en-US">Duplicate entry process policy</span>
	 * <span class="zh-CN">重复条目处理规则</span>
	 */
	private final DuplicatePolicy duplicatePolicy;
	/**
	 * <span class="en-US">Archive extra data record</span>
	 * <span class="zh-CN">归档扩展数据记录</span>
	 */
	private byte[] archiveExtraData;
	/**
	 * <span class="en-US">Digital signature information</span>
	 * <span class="zh-CN">数字签名信息</span>
	 */
	private byte[] signatureData;

	/**
	 * <h3 class="en-US">Constructor method for the central directory define</h3>
	 * <h3 class="zh-CN">中央仓库定义的构造方法</h3>
	 *
	 * @param duplicatePolicy <span class="en-US">Duplicate entry process policy</span>
	 *                        <span class="zh-CN">重复条目处理规则</span>
	 */
	public CentralDirectory(@Nonnull final DuplicatePolicy duplicatePolicy) {
		this.fileHeaders = new ArrayList<>();
		this.duplicatePolicy = duplicatePolicy;
	}

	/**
	 * <h3 class="en-US">Getter method for the general file header define list</h3>
	 * <h3 class="zh-CN">文件头定义列表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">General file header define list</span>
	 * <span class="zh-CN">文件头定义列表</span>
	 */
	public List<GeneralFileHeader> getFileHeaders() {
		return Collections.unmodifiableList(this.fileHeaders);
	}

	/**
	 * <h3 class="en-US">Setter method for the general file header define list</h3>
	 * <h3 class="zh-CN">文件头定义列表的 Setter 方法</h3>
	 *
	 * @param fileHeaders <span class="en-US">General file header define list</span>
	 *                    <span class="zh-CN">文件头定义列表</span>
	 */
	public void setFileHeaders(final List<GeneralFileHeader> fileHeaders) throws IOException {
		LinkedHashMap<String, GeneralFileHeader> selectedHeaders = new LinkedHashMap<>();
		for (GeneralFileHeader fileHeader : fileHeaders) {
			String entryKey = ZipAdaptor.safePath(fileHeader.getEntryPath());
			if (StringUtils.isEmpty(entryKey)) {
				continue;
			}

			GeneralFileHeader existingHeader = selectedHeaders.get(entryKey);
			if (existingHeader == null) {
				selectedHeaders.put(entryKey, fileHeader);
				continue;
			}

			switch (this.duplicatePolicy) {
				case FIRST:
					continue;
				case LAST:
					selectedHeaders.remove(entryKey);
					selectedHeaders.put(entryKey, fileHeader);
					break;
				case REJECT:
					throw new ArchiveException(0x0000001A003FL, entryKey);
			}
		}
		this.fileHeaders.clear();
		this.fileHeaders.addAll(fileHeaders);
		this.selectedHeaders.clear();
		this.selectedHeaders.putAll(selectedHeaders);
	}

	/**
	 * <h3 class="en-US">Getter method for the archive extra data record</h3>
	 * <h3 class="zh-CN">归档扩展数据记录的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Archive extra data record</span>
	 * <span class="zh-CN">归档扩展数据记录</span>
	 */
	public byte[] getArchiveExtraData() {
		return this.archiveExtraData == null ? new byte[0] : this.archiveExtraData.clone();
	}

	/**
	 * <h3 class="en-US">Setter method for the archive extra data record</h3>
	 * <h3 class="zh-CN">归档扩展数据记录的 Setter 方法</h3>
	 *
	 * @param archiveExtraData <span class="en-US">Archive extra data record</span>
	 *                         <span class="zh-CN">归档扩展数据记录</span>
	 */
	public void setArchiveExtraData(final byte[] archiveExtraData) {
		this.archiveExtraData = archiveExtraData == null ? new byte[0] : archiveExtraData.clone();
	}

	/**
	 * <h3 class="en-US">Getter method for the digital signature information</h3>
	 * <h3 class="zh-CN">数字签名信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Digital signature information</span>
	 * <span class="zh-CN">数字签名信息</span>
	 */
	public byte[] getSignatureData() {
		return this.signatureData == null ? new byte[0] : this.signatureData.clone();
	}

	/**
	 * <h3 class="en-US">Setter method for the digital signature information</h3>
	 * <h3 class="zh-CN">数字签名信息的 Setter 方法</h3>
	 *
	 * @param signatureData <span class="en-US">Digital signature information</span>
	 *                      <span class="zh-CN">数字签名信息</span>
	 */
	public void setSignatureData(final byte[] signatureData) {
		this.signatureData = (signatureData == null) ? new byte[0] : signatureData.clone();
	}

	/**
	 * <h3 class="en-US">Retrieve the general file header</h3>
	 * <h3 class="zh-CN">查找通用文件头信息</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @return <span class="en-US">General file header</span>
	 * <span class="zh-CN">文件头信息</span>
	 */
	public GeneralFileHeader header(final String entryPath) {
		return Optional.of(ZipAdaptor.safePath(entryPath))
				.filter(StringUtils::notBlank)
				.map(this.selectedHeaders::get)
				.orElse(null);
	}

	/**
	 * <h3 class="en-US">Get the general file header list</h3>
	 * <h3 class="zh-CN">获取文件头信息列表</h3>
	 *
	 * @return <span class="en-US">General file header list</span>
	 * <span class="zh-CN">文件头信息列表</span>
	 */
	public List<GeneralFileHeader> entryHeaders() {
		return List.copyOf(this.selectedHeaders.values());
	}

	/**
	 * <h3 class="en-US">Read the file header information for all files in a given directory</h3>
	 * <h3 class="zh-CN">读取给定目录的所有文件头信息</h3>
	 *
	 * @param folderPath <span class="en-US">Folder path</span>
	 *                   <span class="zh-CN">文件夹路径</span>
	 * @return <span class="en-US">General file header list</span>
	 * <span class="zh-CN">文件头信息列表</span>
	 */
	public List<GeneralFileHeader> folderEntries(final String folderPath) {
		GeneralFileHeader folderHeader = this.header(folderPath);
		List<GeneralFileHeader> headerList = new ArrayList<>();
		if (folderHeader != null && folderHeader.entryType() == EntryType.DIRECTORY) {
			String safePath = ZipAdaptor.safePath(folderPath);
			this.selectedHeaders.forEach((entryPath, fileHeader) -> {
				if (FileUtils.matchFolder(entryPath, safePath)) {
					headerList.add(fileHeader);
				}
			});
		}
		return Collections.unmodifiableList(headerList);
	}
}
