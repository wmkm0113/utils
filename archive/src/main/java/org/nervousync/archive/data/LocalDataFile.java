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

package org.nervousync.archive.data;

import org.nervousync.archive.core.DataFile;
import org.nervousync.archive.enumerations.OpenMode;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.FileUtils;
import org.nervousync.utils.commons.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * <h2 class="en-US">Local data file implement class</h2>
 * <h2 class="zh-CN">本地数据文件实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class LocalDataFile extends RandomAccessFile implements DataFile {

	/**
	 * <span class="en-US">Current file path</span>
	 * <span class="zh-CN">当前文件地址</span>
	 */
	private final String filePath;
	/**
	 * <span class="en-US">Enumeration value of the open mode</span>
	 * <span class="zh-CN">打开模式枚举值</span>
	 */
	private final OpenMode openMode;

	/**
	 * <h3 class="en-US">Constructor method for the local data file implement class</h3>
	 * <h3 class="zh-CN">本地数据文件实现类的构造方法</h3>
	 *
	 * @param filePath <span class="en-US">Local file path</span>
	 *                 <span class="zh-CN">本地文件地址</span>
	 * @param mode     <span class="en-US">Data file open mode</span>
	 *                 <span class="zh-CN">文件打开模式</span>
	 * @throws FileNotFoundException <span class="en-US">Not found the target file</span>
	 *                               <span class="zh-CN">文件未找到</span>
	 */
	public LocalDataFile(final String filePath, final String mode) throws FileNotFoundException {
		this(filePath, mode, openMode(filePath, mode));
	}

	/**
	 * <h3 class="en-US">Constructor method for the local data file implement class</h3>
	 * <h3 class="zh-CN">本地数据文件实现类的构造方法</h3>
	 *
	 * @param filePath <span class="en-US">Local file path</span>
	 *                 <span class="zh-CN">本地文件地址</span>
	 * @param mode     <span class="en-US">Data file open mode</span>
	 *                 <span class="zh-CN">文件打开模式</span>
	 * @param openMode <span class="en-US">Enumeration value of the open mode</span>
	 *                 <span class="zh-CN">打开模式枚举值</span>
	 * @throws FileNotFoundException <span class="en-US">Not found the target file</span>
	 *                               <span class="zh-CN">文件未找到</span>
	 */
	private LocalDataFile(final String filePath, final String mode, final OpenMode openMode)
			throws FileNotFoundException {
		super(filePath, mode);
		this.filePath = filePath;
		this.openMode = openMode;
	}

	@Override
	public String filePath() {
		return this.filePath;
	}

	@Override
	public OpenMode openMode() {
		return this.openMode;
	}

	@Override
	public List<String> splitFiles() throws IOException {
		String fileName = StringUtils.getFilename(this.filePath);
		String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
		int index = this.filePath.lastIndexOf(Globals.DEFAULT_PAGE_SEPARATOR);
		String folderPath = index == Globals.DEFAULT_VALUE_INT ? "." : filePath.substring(0, index);
		return FileUtils.listFiles(FileUtils.getFile(folderPath), (dir, name) -> {
			if (dir != null && dir.isDirectory() && dir.exists() && name != null) {
				if (FileUtils.isDirectory(dir.getAbsolutePath() + Globals.DEFAULT_PAGE_SEPARATOR + name)) {
					return false;
				}
				return name.startsWith(baseName);
			}
			return Boolean.FALSE;
		}, true, false, false);
	}

	@Override
	public DataFile newFile(final String filePath, final String mode) throws IOException {
		return new LocalDataFile(filePath, StringUtils.isEmpty(mode) ? this.currentMode() : mode);
	}

	/**
	 * <h3 class="en-US">Confirm open mode</h3>
	 * <h3 class="zh-CN">判断打开模式</h3>
	 *
	 * @param filePath <span class="en-US">Local file path</span>
	 *                 <span class="zh-CN">本地文件地址</span>
	 * @param mode     <span class="en-US">Data file open mode</span>
	 *                 <span class="zh-CN">文件打开模式</span>
	 */
	private static OpenMode openMode(final String filePath, final String mode) {
		if (mode.equalsIgnoreCase("r")) {
			return OpenMode.READ;
		} else {
			return FileUtils.isExists(filePath) ? OpenMode.EDIT : OpenMode.CREATE;
		}
	}
}
