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
package org.nervousync.zip;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.commons.zip.ZipGlobals;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.io.StandardFile;
import org.nervousync.utils.core.*;
import org.nervousync.zip.crypto.Cryptor;
import org.nervousync.zip.crypto.impl.aes.AESDecryptor;
import org.nervousync.zip.crypto.impl.aes.AESEngine;
import org.nervousync.zip.crypto.impl.standard.StandardDecryptor;
import org.nervousync.zip.io.SplitOutputStream;
import org.nervousync.zip.io.ZipOutputStream;
import org.nervousync.zip.io.input.InflaterInputStream;
import org.nervousync.zip.io.input.PartInputStream;
import org.nervousync.zip.io.input.ZipInputStream;
import org.nervousync.zip.models.AESExtraDataRecord;
import org.nervousync.zip.models.ArchiveExtraDataRecord;
import org.nervousync.zip.models.ExtraDataRecord;
import org.nervousync.zip.models.Zip64ExtendInfo;
import org.nervousync.zip.models.central.*;
import org.nervousync.zip.models.header.FileHeader;
import org.nervousync.zip.models.header.GeneralFileHeader;
import org.nervousync.zip.models.header.LocalFileHeader;
import org.nervousync.zip.models.header.utils.HeaderOperator;
import org.nervousync.zip.options.ZipOptions;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;

/**
 * <h2 class="en-US">Zip file wrapper class</h2>
 * <span class="en-US">
 * <span>Implements features:</span>
 *     <ul>Create a new zip file</ul>
 *     <ul>Read the zip file (support the split zip file)</ul>
 *     <ul>Append files to the current zip file</ul>
 *     <ul>Read the zip entry from the current zip file</ul>
 *     <ul>Remove zip entry from the current zip file</ul>
 *     <ul>Extract zip entry to the target path</ul>
 * </span>
 * <h2 class="zh-CN">Zip文件包装类</h2>
 * <span class="zh-CN">
 *     <span>实现以下功能:</span>
 *     <ul>创建ZIP文件</ul>
 *     <ul>读取ZIP文件（支持分卷压缩文件）</ul>
 *     <ul>添加文件到当前ZIP文件</ul>
 *     <ul>读取ZIP文件内的压缩文件</ul>
 *     <ul>删除ZIP文件内的压缩文件</ul>
 *     <ul>解压缩ZIP文件到指定目录</ul>
 * </span>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 28, 2017 17:01:20 $
 */
@SuppressWarnings("unused")
public final class ZipFile implements Cloneable {

	private final byte[] EMPTY_SHORT_BUFFER = {0, 0};
	private final byte[] EMPTY_INT_BUFFER = {0, 0, 0, 0};

	/**
	 * <span class="en-US">Current zip file path</span>
	 * <span class="zh-CN">当前压缩文件路径</span>
	 */
	private final String filePath;
	/**
	 * <span class="en-US">Using charset encoding</span>
	 * <span class="zh-CN">使用的字符集</span>
	 */
	private final String charsetEncoding;
	/**
	 * <span class="en-US">Flag of using number formatted name</span>
	 * <span class="zh-CN">使用数字格式文件名标记</span>
	 */
	private boolean numberFormattedName = Boolean.FALSE;
	/**
	 * <span class="en-US">File header information list</span>
	 * <span class="zh-CN">文件头信息列表</span>
	 */
	private List<LocalFileHeader> localFileHeaderList = null;
	/**
	 * <span class="en-US">Record of archive extra data</span>
	 * <span class="zh-CN">压缩包扩展数据记录</span>
	 *
	 * @see ArchiveExtraDataRecord
	 */
	private ArchiveExtraDataRecord archiveExtraDataRecord = null;
	/**
	 * <span class="en-US">Central directory instance object</span>
	 * <span class="zh-CN">中央目录实例对象</span>
	 *
	 * @see CentralDirectory
	 */
	private CentralDirectory centralDirectory = null;
	/**
	 * <span class="en-US">End central directory record instance object</span>
	 * <span class="zh-CN">中央目录终止记录实例对象</span>
	 *
	 * @see EndCentralDirectoryRecord
	 */
	private EndCentralDirectoryRecord endCentralDirectoryRecord = null;
	/**
	 * <span class="en-US">ZIP 64 end central directory locator instance object</span>
	 * <span class="zh-CN">ZIP64中央目录定位信息实例对象</span>
	 *
	 * @see Zip64EndCentralDirectoryLocator
	 */
	private Zip64EndCentralDirectoryLocator zip64EndCentralDirectoryLocator = null;
	/**
	 * <span class="en-US">ZIP 64 end central directory record instance object</span>
	 * <span class="zh-CN">ZIP64中央目录终止记录实例对象</span>
	 *
	 * @see Zip64EndCentralDirectoryRecord
	 */
	private Zip64EndCentralDirectoryRecord zip64EndCentralDirectoryRecord = null;
	/**
	 * <span class="en-US">Decryptor instance object</span>
	 * <span class="zh-CN">解密器实例对象</span>
	 */
	private Cryptor decryptor = null;
	/**
	 * <span class="en-US">Archive is split file status</span>
	 * <span class="zh-CN">分卷压缩包标记</span>
	 */
	private boolean splitArchive;
	/**
	 * <span class="en-US">Maximum length of split item</span>
	 * <span class="zh-CN">分卷大小</span>
	 */
	private long splitLength;
	/**
	 * <span class="en-US">Current split count of archive</span>
	 * <span class="zh-CN">当前的分卷计数</span>
	 */
	private int splitCount = Globals.INITIALIZE_INT_VALUE;
	/**
	 * <span class="en-US">ZIP64 format archive flag</span>
	 * <span class="zh-CN">压缩包为ZIP64标记</span>
	 */
	private boolean zip64Format = Boolean.FALSE;

	/**
	 * <h3 class="en-US">Private constructor method for ZIP file</h3>
	 * <h3 class="zh-CN">ZIP文件的私有构造方法</h3>
	 *
	 * @param filePath        <span class="en-US">Current zip file path</span>
	 *                        <span class="zh-CN">当前压缩文件路径</span>
	 * @param charsetEncoding <span class="en-US">Using charset encoding</span>
	 *                        <span class="zh-CN">使用的字符集</span>
	 * @param splitArchive    <span class="en-US">Archive is split file status</span>
	 *                        <span class="zh-CN">分卷压缩包标记</span>
	 * @param splitLength     <span class="en-US">Maximum length of split item</span>
	 *                        <span class="zh-CN">分卷大小</span>
	 */
	private ZipFile(final String filePath, final String charsetEncoding, final boolean splitArchive,
	                final long splitLength) throws ZipException {
		this.filePath = filePath;
		this.charsetEncoding = StringUtils.isEmpty(charsetEncoding) ? Globals.DEFAULT_ENCODING : charsetEncoding;
		this.splitArchive = splitArchive;
		this.splitLength = splitLength;
		if (FileUtils.isExists(this.filePath)) {
			if (!FileUtils.canRead(this.filePath)) {
				throw new ZipException(0x0000001B001AL);
			}
			this.readHeaders();
		}
	}

	/**
	 * <h3 class="en-US">Static method for open the ZIP file</h3>
	 * <h3 class="zh-CN">静态方法用于打开ZIP文件</h3>
	 *
	 * @param filePath <span class="en-US">Current zip file path</span>
	 *                 <span class="zh-CN">当前压缩文件路径</span>
	 * @return <span class="en-US">ZIP file instance object</span>
	 * <span class="zh-CN">ZIP文件实例对象</span>
	 * @throws ZipException <span class="en-US">An error occurs when open the ZIP file</span>
	 *                      <span class="zh-CN">读取ZIP文件时出错</span>
	 */
	public static ZipFile openZipFile(final String filePath) throws ZipException {
		return openZipFile(filePath, Globals.DEFAULT_ENCODING);
	}

	/**
	 * <h3 class="en-US">Static method for open the ZIP file</h3>
	 * <h3 class="zh-CN">静态方法用于打开ZIP文件</h3>
	 *
	 * @param filePath        <span class="en-US">Current zip file path</span>
	 *                        <span class="zh-CN">当前压缩文件路径</span>
	 * @param charsetEncoding <span class="en-US">Using charset encoding</span>
	 *                        <span class="zh-CN">使用的字符集</span>
	 * @return <span class="en-US">ZIP file instance object</span>
	 * <span class="zh-CN">ZIP文件实例对象</span>
	 * @throws ZipException <span class="en-US">An error occurs when open the ZIP file</span>
	 *                      <span class="zh-CN">读取ZIP文件时出错</span>
	 */
	public static ZipFile openZipFile(final String filePath, final String charsetEncoding) throws ZipException {
		return new ZipFile(filePath, charsetEncoding, Boolean.FALSE, Globals.DEFAULT_VALUE_LONG);
	}

	/**
	 * <h3 class="en-US">Static method for create the ZIP file</h3>
	 * <h3 class="zh-CN">静态方法用于创建ZIP文件</h3>
	 *
	 * @param filePath   <span class="en-US">Current zip file path</span>
	 *                   <span class="zh-CN">当前压缩文件路径</span>
	 * @param zipOptions <span class="en-US">ZipOption instance object</span>
	 *                   <span class="zh-CN">压缩文件属性</span>
	 * @param addFiles   <span class="en-US">List of files in the zip file</span>
	 *                   <span class="zh-CN">需要添加到压缩文件的文件列表</span>
	 * @return <span class="en-US">ZIP file instance object</span>
	 * <span class="zh-CN">ZIP文件实例对象</span>
	 * @throws ZipException <span class="en-US">An error occurs when create the ZIP file</span>
	 *                      <span class="zh-CN">创建ZIP文件时出错</span>
	 */
	public static ZipFile createZipFile(final String filePath, final ZipOptions zipOptions, final String... addFiles)
			throws ZipException {
		return ZipFile.createZipFile(filePath, zipOptions, Boolean.FALSE, Globals.DEFAULT_VALUE_LONG, addFiles);
	}

	/**
	 * <h3 class="en-US">Static method for create the ZIP file</h3>
	 * <h3 class="zh-CN">静态方法用于创建ZIP文件</h3>
	 *
	 * @param filePath     <span class="en-US">Current zip file path</span>
	 *                     <span class="zh-CN">当前压缩文件路径</span>
	 * @param zipOptions   <span class="en-US">ZipOption instance object</span>
	 *                     <span class="zh-CN">压缩文件属性</span>
	 * @param splitArchive <span class="en-US">Archive is split file status</span>
	 *                     <span class="zh-CN">分卷压缩包标记</span>
	 * @param splitLength  <span class="en-US">Maximum length of split item</span>
	 *                     <span class="zh-CN">分卷大小</span>
	 * @param addFiles     <span class="en-US">List of files in the zip file</span>
	 *                     <span class="zh-CN">需要添加到压缩文件的文件列表</span>
	 * @return <span class="en-US">ZIP file instance object</span>
	 * <span class="zh-CN">ZIP文件实例对象</span>
	 * @throws ZipException <span class="en-US">An error occurs when create the ZIP file</span>
	 *                      <span class="zh-CN">创建ZIP文件时出错</span>
	 */
	public static ZipFile createZipFile(final String filePath, final ZipOptions zipOptions, final boolean splitArchive,
	                                    final long splitLength, final String... addFiles) throws ZipException {
		ZipFile.checkFilePath(filePath);
		if (addFiles == null || addFiles.length == 0) {
			throw new ZipException(0x0000001B001BL);
		}

		ZipFile zipFile = ZipFile.createZipFile(filePath, zipOptions.getCharsetEncoding(), splitArchive, splitLength);
		zipFile.addFiles(Arrays.asList(addFiles), zipOptions);

		return zipFile;
	}

	/**
	 * <h3 class="en-US">Static method for create the ZIP file</h3>
	 * <h3 class="zh-CN">静态方法用于创建ZIP文件</h3>
	 *
	 * @param filePath   <span class="en-US">Current zip file path</span>
	 *                   <span class="zh-CN">当前压缩文件路径</span>
	 * @param zipOptions <span class="en-US">ZipOption instance object</span>
	 *                   <span class="zh-CN">压缩文件属性</span>
	 * @param folderPath <span class="en-US">Folder path will add to the ZIP file</span>
	 *                   <span class="zh-CN">需要添加到压缩文件的文件夹</span>
	 * @return <span class="en-US">ZIP file instance object</span>
	 * <span class="zh-CN">ZIP文件实例对象</span>
	 * @throws ZipException <span class="en-US">An error occurs when create the ZIP file</span>
	 *                      <span class="zh-CN">创建ZIP文件时出错</span>
	 */
	public static ZipFile createZipFileFromFolder(final String filePath, final ZipOptions zipOptions,
	                                              final String folderPath) throws ZipException {
		return ZipFile.createZipFileFromFolder(filePath, zipOptions,
				Boolean.FALSE, Globals.DEFAULT_VALUE_LONG, folderPath);
	}

	/**
	 * <h3 class="en-US">Static method for create the ZIP file</h3>
	 * <h3 class="zh-CN">静态方法用于创建ZIP文件</h3>
	 *
	 * @param filePath     <span class="en-US">Current zip file path</span>
	 *                     <span class="zh-CN">当前压缩文件路径</span>
	 * @param zipOptions   <span class="en-US">ZipOption instance object</span>
	 *                     <span class="zh-CN">压缩文件属性</span>
	 * @param splitArchive <span class="en-US">Archive is split file status</span>
	 *                     <span class="zh-CN">分卷压缩包标记</span>
	 * @param splitLength  <span class="en-US">Maximum length of split item</span>
	 *                     <span class="zh-CN">分卷大小</span>
	 * @param folderPath   <span class="en-US">Folder path will add to the ZIP file</span>
	 *                     <span class="zh-CN">需要添加到压缩文件的文件夹</span>
	 * @return <span class="en-US">ZIP file instance object</span>
	 * <span class="zh-CN">ZIP文件实例对象</span>
	 * @throws ZipException <span class="en-US">An error occurs when create the ZIP file</span>
	 *                      <span class="zh-CN">创建ZIP文件时出错</span>
	 */
	public static ZipFile createZipFileFromFolder(final String filePath, final ZipOptions zipOptions,
	                                              final boolean splitArchive, final long splitLength,
	                                              final String folderPath) throws ZipException {
		ZipFile.checkFilePath(filePath);

		if (StringUtils.isEmpty(folderPath)) {
			throw new ZipException(0x0000001B001BL);
		}

		ZipFile zipFile = ZipFile.createZipFile(filePath, zipOptions.getCharsetEncoding(), splitArchive, splitLength);
		zipFile.addFolder(folderPath, zipOptions, Boolean.FALSE);
		if (zipOptions.getPassword() != null) {
			zipFile.setPassword(zipOptions.getPassword());
		}
		return zipFile;
	}

	/**
	 * <h3 class="en-US">Static method for generate the entity path</h3>
	 * <h3 class="zh-CN">静态方法用于生成ZIP内路径</h3>
	 *
	 * @param file            <span class="en-US">Which file path will add to the target zip file.</span>
	 *                        <span class="zh-CN">需要添加到ZIP文件的文件路径</span>
	 * @param rootFolderInZip <span class="en-US">Prefix path of the zip file</span>
	 *                        <span class="zh-CN">ZIP文件的路径前缀</span>
	 * @param rootFolderPath  <span class="en-US">Root folder path</span>
	 *                        <span class="zh-CN">根目录路径</span>
	 * @return <span class="en-US">Generated entry path</span>
	 * <span class="zh-CN">生成的路径</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public static String getRelativeFileName(final String file, final String rootFolderInZip,
	                                         final String rootFolderPath) throws ZipException {
		if (StringUtils.isEmpty(file)) {
			throw new ZipException(0x0000001B001EL);
		}

		String fileName;

		if (StringUtils.notBlank(rootFolderPath)) {
			File rootFolderFile = new File(rootFolderPath);

			String rootFolderFileRef = rootFolderFile.getPath();

			if (!rootFolderFileRef.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
				rootFolderFileRef += Globals.DEFAULT_PAGE_SEPARATOR;
			}

			String tmpFileName = file.substring(rootFolderFileRef.length());
			if (tmpFileName.startsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
				tmpFileName = tmpFileName.substring(1);
			}

			File tmpFile = new File(file);

			if (tmpFile.isDirectory()) {
				tmpFileName = StringUtils.replace(tmpFileName, Globals.DEFAULT_PAGE_SEPARATOR,
						Globals.DEFAULT_ZIP_PAGE_SEPARATOR);
				if (!tmpFileName.endsWith(Globals.DEFAULT_ZIP_PAGE_SEPARATOR)) {
					tmpFileName += Globals.DEFAULT_ZIP_PAGE_SEPARATOR;
				}
			} else {
				String bkFileName = tmpFileName.substring(0, tmpFileName.lastIndexOf(tmpFile.getName()));
				bkFileName = StringUtils.replace(bkFileName, Globals.DEFAULT_PAGE_SEPARATOR,
						Globals.DEFAULT_ZIP_PAGE_SEPARATOR);
				tmpFileName = bkFileName + tmpFile.getName();
			}

			fileName = tmpFileName;
		} else {
			File relFile = new File(file);
			if (relFile.isDirectory()) {
				fileName = relFile.getName() + Globals.DEFAULT_ZIP_PAGE_SEPARATOR;
			} else {
				fileName = getFileNameFromFilePath(relFile);
			}
		}

		if (StringUtils.notBlank(rootFolderInZip)) {
			fileName = rootFolderInZip + fileName;
		}

		if (StringUtils.isEmpty(fileName)) {
			throw new ZipException(0x0000001B0020L);
		}

		return fileName;
	}

	/**
	 * <h3 class="en-US">Get the entry path list which path matched by the given regex string</h3>
	 * <h3 class="zh-CN">获取满足给定正则表达式的路径列表</h3>
	 *
	 * @return <span class="en-US">Entry path list</span>
	 * <span class="zh-CN">路径列表</span>
	 */
	public List<String> entryList() {
		return this.entryList(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Get the entry path list which path matched by the given regex string</h3>
	 * <h3 class="zh-CN">获取满足给定正则表达式的路径列表</h3>
	 *
	 * @param regex <span class="en-US">Regex string</span>
	 *              <span class="zh-CN">正则表达式</span>
	 * @return <span class="en-US">Entry path list</span>
	 * <span class="zh-CN">路径列表</span>
	 */
	public List<String> entryList(final String regex) {
		List<String> entryList = new ArrayList<>();
		if (StringUtils.isEmpty(regex)) {
			this.centralDirectory.getFileHeaders()
					.forEach(generalFileHeader -> entryList.add(generalFileHeader.getEntryPath()));
		} else {
			this.centralDirectory.getFileHeaders()
					.stream()
					.filter(generalFileHeader -> StringUtils.matches(generalFileHeader.getEntryPath(), regex))
					.forEach(generalFileHeader -> entryList.add(generalFileHeader.getEntryPath()));
		}
		return entryList;
	}

	/**
	 * <h3 class="en-US">Check the given entry path is existed</h3>
	 * <h3 class="zh-CN">检查当前的ZIP文件中是否包含给定路径</h3>
	 *
	 * @param entryPath <span class="en-US">The path of the file to be read</span>
	 *                  <span class="zh-CN">需要读取的文件路径</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean isEntryExists(final String entryPath) {
		for (GeneralFileHeader generalFileHeader : this.centralDirectory.getFileHeaders()) {
			if (generalFileHeader.getEntryPath().equals(entryPath)) {
				return true;
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * <h3 class="en-US">Read data length by the given entry path from the current ZIP file</h3>
	 * <h3 class="zh-CN">从当前的ZIP文件中读取给定路径的数据大小</h3>
	 *
	 * @param entryPath <span class="en-US">The path of the file to be read</span>
	 *                  <span class="zh-CN">需要读取的文件路径</span>
	 * @return <span class="en-US">Entry data length</span>
	 * <span class="zh-CN">文件大小</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public int readEntryLength(final String entryPath) throws ZipException {
		if (FileUtils.isExists(this.filePath) && this.splitArchive) {
			throw new ZipException(0x0000001B0018L);
		}
		return this.readEntryLength(this.retrieveGeneralFileHeader(entryPath));
	}

	/**
	 * <h3 class="en-US">Read binary data by the given entry path from the current ZIP file</h3>
	 * <h3 class="zh-CN">从当前的ZIP文件中读取给定路径的二进制数据</h3>
	 *
	 * @param entryPath <span class="en-US">The path of the file to be read</span>
	 *                  <span class="zh-CN">需要读取的文件路径</span>
	 * @return <span class="en-US">Read binary data</span>
	 * <span class="zh-CN">读取的二进制数据</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public byte[] readEntry(final String entryPath) throws ZipException {
		return this.readEntry(entryPath, Globals.DEFAULT_VALUE_LONG, Globals.DEFAULT_VALUE_INT);
	}

	/**
	 * <h3 class="en-US">Read binary data by the given entry path from the current ZIP file</h3>
	 * <h3 class="zh-CN">从当前的ZIP文件中读取给定路径的二进制数据</h3>
	 *
	 * @param entryPath  <span class="en-US">The path of the file to be read</span>
	 *                   <span class="zh-CN">需要读取的文件路径</span>
	 * @param position   <span class="en-US">The beginning position</span>
	 *                   <span class="zh-CN">读取的起始位置</span>
	 * @param readLength <span class="en-US">The read length</span>
	 *                   <span class="zh-CN">读取的长度</span>
	 * @return <span class="en-US">Read binary data</span>
	 * <span class="zh-CN">读取的二进制数据</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public byte[] readEntry(final String entryPath, final long position, final int readLength) throws ZipException {
		if (FileUtils.isExists(this.filePath) && this.splitArchive) {
			throw new ZipException(0x0000001B0018L);
		}

		return this.readEntry(this.retrieveGeneralFileHeader(entryPath), position, readLength);
	}

	/**
	 * <h3 class="en-US">Get InputStream instance object by the given entry path from the current ZIP file</h3>
	 * <h3 class="zh-CN">获取给定路径的输入流实例对象</h3>
	 *
	 * @param entryPath <span class="en-US">The path of the file to be read</span>
	 *                  <span class="zh-CN">需要读取的文件路径</span>
	 * @return <span class="en-US">Opened input stream</span>
	 * <span class="zh-CN">打开的输入流实例对象</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public InputStream entryInputStream(final String entryPath) throws ZipException {
		if (FileUtils.isExists(this.filePath) && this.splitArchive) {
			throw new ZipException(0x0000001B0018L);
		}
		return this.openInputStream(this.retrieveGeneralFileHeader(entryPath));
	}

	/**
	 * <h3 class="en-US">Add the file to the current ZIP file</h3>
	 * <h3 class="zh-CN">向当前ZIP文件中添加文件</h3>
	 *
	 * @param file <span class="en-US">File instance object will add to the ZIP file</span>
	 *             <span class="zh-CN">需要添加到压缩文件的文件</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void addFile(final File file) throws ZipException {
		this.addFile(file, ZipOptions.newOptions());
	}

	/**
	 * <h3 class="en-US">Add the file to the current ZIP file with zip options</h3>
	 * <h3 class="zh-CN">使用给定的ZIP压缩属性向当前ZIP文件中添加文件</h3>
	 *
	 * @param file       <span class="en-US">File instance object will add to the ZIP file</span>
	 *                   <span class="zh-CN">需要添加到压缩文件的文件</span>
	 * @param zipOptions <span class="en-US">ZipOption instance object</span>
	 *                   <span class="zh-CN">压缩文件属性</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void addFile(final File file, final ZipOptions zipOptions) throws ZipException {
		this.addFiles(Collections.singletonList(file.getAbsolutePath()), zipOptions);
	}

	/**
	 * <h3 class="en-US">Add the file list to the current ZIP file</h3>
	 * <h3 class="zh-CN">向当前ZIP文件中添加文件</h3>
	 *
	 * @param fileList <span class="en-US">File path list will add to the ZIP file</span>
	 *                 <span class="zh-CN">需要添加到压缩文件的文件列表</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void addFiles(final List<String> fileList) throws ZipException {
		this.addFiles(fileList, ZipOptions.newOptions());
	}

	/**
	 * <h3 class="en-US">Add the file list to the current ZIP file with zip options</h3>
	 * <h3 class="zh-CN">使用给定的ZIP压缩属性向当前ZIP文件中添加文件</h3>
	 *
	 * @param fileList   <span class="en-US">File path list will add to the ZIP file</span>
	 *                   <span class="zh-CN">需要添加到压缩文件的文件列表</span>
	 * @param zipOptions <span class="en-US">ZipOption instance object</span>
	 *                   <span class="zh-CN">压缩文件属性</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void addFiles(final List<String> fileList, final ZipOptions zipOptions) throws ZipException {
		this.appendCheck(zipOptions);
		this.addFilesToZip(fileList, zipOptions);
	}

	/**
	 * <h3 class="en-US">Add InputStream to the current ZIP file</h3>
	 * <h3 class="zh-CN">向当前ZIP文件中添加数据</h3>
	 *
	 * @param inputStream <span class="en-US">InputStream instance object will add to the ZIP file</span>
	 *                    <span class="zh-CN">需要添加到压缩文件的输入流</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void addStream(final InputStream inputStream) throws ZipException {
		this.addStream(inputStream, ZipOptions.newOptions());
	}

	/**
	 * <h3 class="en-US">Add InputStream to the current ZIP file with zip options</h3>
	 * <h3 class="zh-CN">使用给定的ZIP压缩属性向当前ZIP文件中添加数据</h3>
	 *
	 * @param inputStream <span class="en-US">InputStream instance object will add to the ZIP file</span>
	 *                    <span class="zh-CN">需要添加到压缩文件的输入流</span>
	 * @param zipOptions  <span class="en-US">ZipOption instance object</span>
	 *                    <span class="zh-CN">压缩文件属性</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void addStream(final InputStream inputStream, final ZipOptions zipOptions) throws ZipException {
		if (inputStream == null) {
			throw new ZipException(0x0000001B0022L);
		}

		this.appendCheck(zipOptions);
		this.addStreamToZip(inputStream, zipOptions);
	}

	/**
	 * <h3 class="en-US">Add a folder to the current ZIP file</h3>
	 * <h3 class="zh-CN">向当前ZIP文件中添加目录</h3>
	 *
	 * @param folderPath <span class="en-US">Folder path will add to the ZIP file</span>
	 *                   <span class="zh-CN">需要添加到压缩文件的文件夹</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void addFolder(final String folderPath) throws ZipException {
		this.addFolder(folderPath, ZipOptions.newOptions(), true);
	}

	/**
	 * <h3 class="en-US">Add a folder to the current ZIP file with zip options</h3>
	 * <h3 class="zh-CN">使用给定的ZIP压缩属性向当前ZIP文件中添加目录</h3>
	 *
	 * @param folderPath <span class="en-US">Folder path will add to the ZIP file</span>
	 *                   <span class="zh-CN">需要添加到压缩文件的文件夹</span>
	 * @param zipOptions <span class="en-US">ZipOption instance object</span>
	 *                   <span class="zh-CN">压缩文件属性</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void addFolder(final String folderPath, final ZipOptions zipOptions) throws ZipException {
		this.addFolder(folderPath, zipOptions, true);
	}

	/**
	 * <h3 class="en-US">Extract the all files to the target extract file path</h3>
	 * <h3 class="zh-CN">从ZIP文件中解压缩所有文件到目标路径</h3>
	 *
	 * @param destPath <span class="en-US">Target extract file path</span>
	 *                 <span class="zh-CN">解压缩目标路径</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void extractAll(final String destPath) throws ZipException {
		this.extractAll(destPath, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Extract the all files to the target extract file path</h3>
	 * <h3 class="zh-CN">从ZIP文件中解压缩所有文件到目标路径</h3>
	 *
	 * @param destPath       <span class="en-US">Target extract file path</span>
	 *                       <span class="zh-CN">解压缩目标路径</span>
	 * @param ignoreFileAttr <span class="en-US">Status of process file attribute</span>
	 *                       <span class="zh-CN">忽略文件属性标记</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void extractAll(final String destPath, final boolean ignoreFileAttr) throws ZipException {
		if (StringUtils.isEmpty(destPath)) {
			throw new ZipException(0x0000001B0023L);
		}

		if (this.centralDirectory == null || this.centralDirectory.getFileHeaders() == null) {
			throw new ZipException(0x0000001B0024L);
		}

		for (GeneralFileHeader generalFileHeader : this.centralDirectory.getFileHeaders()) {
			this.extractFile(generalFileHeader, destPath, ignoreFileAttr);
		}
	}

	/**
	 * <h3 class="en-US">Extract the entry path file to the target extract file path</h3>
	 * <h3 class="zh-CN">从ZIP文件中解压缩给定路径文件到目标路径</h3>
	 *
	 * @param entryPath <span class="en-US">Which entry paths will be extract</span>
	 *                  <span class="zh-CN">需要解压缩的文件路径</span>
	 * @param destPath  <span class="en-US">Target extract file path</span>
	 *                  <span class="zh-CN">解压缩目标路径</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void extractFile(final String entryPath, final String destPath) throws ZipException {
		this.extractFile(entryPath, destPath, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Extract the entry path file to the target extract file path</h3>
	 * <h3 class="zh-CN">从ZIP文件中解压缩给定路径文件到目标路径</h3>
	 *
	 * @param entryPath      <span class="en-US">Which entry paths will be extract</span>
	 *                       <span class="zh-CN">需要解压缩的文件路径</span>
	 * @param destPath       <span class="en-US">Target extract file path</span>
	 *                       <span class="zh-CN">解压缩目标路径</span>
	 * @param ignoreFileAttr <span class="en-US">Status of process file attribute</span>
	 *                       <span class="zh-CN">忽略文件属性标记</span>
	 * @throws ZipException <span class="en-US">An error occurs when processing</span>
	 *                      <span class="zh-CN">处理操作的过程中出错</span>
	 */
	public void extractFile(final String entryPath, final String destPath, final boolean ignoreFileAttr)
			throws ZipException {
		if (StringUtils.isEmpty(entryPath)) {
			throw new ZipException(0x0000001B0025L);
		}

		if (StringUtils.isEmpty(destPath)) {
			throw new ZipException(0x0000001B0023L);
		}

		this.extractFile(this.retrieveGeneralFileHeader(entryPath), destPath, ignoreFileAttr);
	}

	/**
	 * <h3 class="en-US">Remove the folder path from the ZIP file</h3>
	 * <h3 class="zh-CN">从ZIP文件中删除给定的文件夹路径</h3>
	 *
	 * @param folderPath <span class="en-US">Which folder path will be removed</span>
	 *                   <span class="zh-CN">需要删除的文件夹路径</span>
	 * @throws ZipException <span class="en-US">An error occurs when remove folder path</span>
	 *                      <span class="zh-CN">删除文件夹路径的过程中出错</span>
	 */
	public void removeFolder(final String folderPath) throws ZipException {
		if (this.isDirectory(folderPath)) {
			this.removeFilesIfExists(this.listFolderGeneralFileHeaders(folderPath));
			return;
		}
		throw new ZipException(0x0000001B0025L, folderPath);
	}

	/**
	 * <h3 class="en-US">Remove entry paths from the ZIP file</h3>
	 * <h3 class="zh-CN">从ZIP文件中删除给定的文件路径</h3>
	 *
	 * @param entryPath <span class="en-US">Which entry paths will be removed</span>
	 *                  <span class="zh-CN">需要删除的文件路径</span>
	 * @throws ZipException <span class="en-US">An error occurs when remove entry paths</span>
	 *                      <span class="zh-CN">删除文件路径的过程中出错</span>
	 */
	public void removeExistsEntry(final String entryPath) throws ZipException {
		this.removeExistsEntries(entryPath);
	}

	/**
	 * <h3 class="en-US">Remove entry paths from the ZIP file</h3>
	 * <h3 class="zh-CN">从ZIP文件中删除给定的文件路径</h3>
	 *
	 * @param existsEntries <span class="en-US">Which entry paths will be removed</span>
	 *                      <span class="zh-CN">需要删除的文件路径</span>
	 * @throws ZipException <span class="en-US">An error occurs when remove entry paths</span>
	 *                      <span class="zh-CN">删除文件路径的过程中出错</span>
	 */
	public void removeExistsEntries(final String... existsEntries) throws ZipException {
		if (existsEntries == null) {
			throw new ZipException(0x0000001B0027L);
		}

		if (FileUtils.isExists(this.filePath) && this.splitArchive) {
			throw new ZipException(0x0000001B0018L);
		}

		this.removeFilesIfExists(Arrays.asList(existsEntries));

		if (this.isNoEntry()) {
			FileUtils.removeFile(this.filePath);
		}
	}

	/**
	 * <h3 class="en-US">Setting password</h3>
	 * <h3 class="zh-CN">设置使用的密码</h3>
	 *
	 * @param password <span class="en-US">Password information</span>
	 *                 <span class="zh-CN">密码信息</span>
	 * @throws ZipException <span class="en-US">An error occurs when setting the password</span>
	 *                      <span class="zh-CN">设置密码的过程中出错</span>
	 */
	public void setPassword(final String password) throws ZipException {
		if (StringUtils.isEmpty(password)) {
			throw new ZipException(0x0000001B0006L);
		}
		this.setPassword(password.toCharArray());
	}

	/**
	 * <h3 class="en-US">Setting password</h3>
	 * <h3 class="zh-CN">设置使用的密码</h3>
	 *
	 * @param password <span class="en-US">Password information</span>
	 *                 <span class="zh-CN">密码信息</span>
	 * @throws ZipException <span class="en-US">An error occurs when setting the password</span>
	 *                      <span class="zh-CN">设置密码的过程中出错</span>
	 */
	public void setPassword(final char[] password) throws ZipException {
		if (this.centralDirectory == null || this.centralDirectory.getFileHeaders() == null) {
			throw new ZipException(0x0000001B0028L);
		}

		for (int i = 0; i < this.centralDirectory.getFileHeaders().size(); i++) {
			if (this.centralDirectory.getFileHeaders().get(i) != null
					&& this.centralDirectory.getFileHeaders().get(i).isEncrypted()) {
				this.centralDirectory.getFileHeaders().get(i).setPassword(password);
			}
		}
	}

	/**
	 * <h3 class="en-US">Setting comment information</h3>
	 * <h3 class="zh-CN">设置备注信息</h3>
	 *
	 * @param comment <span class="en-US">Comment information</span>
	 *                <span class="zh-CN">备注信息</span>
	 * @throws ZipException <span class="en-US">An error occurs when setting the comment information</span>
	 *                      <span class="zh-CN">设置备注信息的过程中出错</span>
	 */
	public void setComment(final String comment) throws ZipException {
		if (StringUtils.isEmpty(comment)) {
			this.endCentralDirectoryRecord.setCommentBytes(new byte[0]);
			this.endCentralDirectoryRecord.setCommentLength(Globals.INITIALIZE_INT_VALUE);
			return;
		} else {
			if (!FileUtils.isExists(this.filePath)) {
				throw new ZipException(0x0000001B001BL);
			}

			if (this.endCentralDirectoryRecord == null) {
				throw new ZipException(0x0000001B0029L);
			}

			byte[] commentBytes;
			int commentLength;

			try {
				commentBytes = comment.getBytes(this.charsetEncoding);
			} catch (UnsupportedEncodingException e) {
				throw new ZipException(0x000000FF0002L, e);
			}

			commentLength = commentBytes.length;

			if (commentLength > ZipGlobals.MAX_ALLOWED_ZIP_COMMENT_LENGTH) {
				throw new ZipException(0x0000001B002AL);
			}

			this.endCentralDirectoryRecord.setCommentBytes(commentBytes);
			this.endCentralDirectoryRecord.setCommentLength(commentLength);
		}

		try (SplitOutputStream outputStream = SplitOutputStream.newInstance(this.filePath)) {
			if (this.zip64Format) {
				outputStream.seek(this.zip64EndCentralDirectoryRecord.getOffsetStartCenDirWRTStartDiskNo());
			} else {
				outputStream.seek(this.endCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory());
			}

			this.finalizeZipFileWithoutValidations(outputStream);
		} catch (IOException e) {
			throw new ZipException(0x0000001B002BL, e);
		}
	}

	/**
	 * <h3 class="en-US">Read comment information</h3>
	 * <h3 class="zh-CN">读取备注信息</h3>
	 *
	 * @return <span class="en-US">Read comment content</span>
	 * <span class="zh-CN">读取的备注信息</span>
	 * @throws ZipException <span class="en-US">An error occurs when read the comment information</span>
	 *                      <span class="zh-CN">读取备注信息的过程中出错</span>
	 */
	public String getComment() throws ZipException {
		return this.getComment(this.charsetEncoding);
	}

	/**
	 * <h3 class="en-US">Read comment by given charset encoding</h3>
	 * <h3 class="zh-CN">使用给定的字符集读取备注信息</h3>
	 *
	 * @param charset <span class="en-US">Used charset encoding</span>
	 *                <span class="zh-CN">使用的字符集</span>
	 * @return <span class="en-US">Read comment content</span>
	 * <span class="zh-CN">读取的备注信息</span>
	 * @throws ZipException <span class="en-US">An error occurs when read the comment information</span>
	 *                      <span class="zh-CN">读取备注信息的过程中出错</span>
	 */
	public String getComment(final String charset) throws ZipException {
		String charsetEncoding = StringUtils.isEmpty(charset) ? Globals.DEFAULT_SYSTEM_CHARSET : charset;

		if (!FileUtils.isExists(this.filePath)) {
			throw new ZipException(0x0000001B001BL);
		}

		if (this.endCentralDirectoryRecord == null) {
			throw new ZipException(0x0000001B0029L);
		}

		if (this.endCentralDirectoryRecord.getCommentBytes() == null
				|| this.endCentralDirectoryRecord.getCommentBytes().length == 0) {
			return null;
		}

		try {
			return new String(this.endCentralDirectoryRecord.getCommentBytes(), charsetEncoding);
		} catch (UnsupportedEncodingException e) {
			throw new ZipException(0x000000FF0002L, e);
		}
	}

	/**
	 * <h3 class="en-US">Merge the split files to the target output path</h3>
	 * <h3 class="zh-CN">合并分卷ZIP文件到给定的输出地址</h3>
	 *
	 * @param outputPath <span class="en-US">The given output path</span>
	 *                   <span class="zh-CN">给定的输出地址</span>
	 * @throws ZipException <span class="en-US">An error occurs when merge the split ZIP file</span>
	 *                      <span class="zh-CN">合并分卷ZIP文件的过程中出错</span>
	 */
	public void mergeSplitFile(final String outputPath) throws ZipException {
		if (!this.splitArchive || this.endCentralDirectoryRecord.getIndexOfThisDisk() <= 0) {
			throw new ZipException(0x0000001B002CL);
		}

		StandardFile input = null;
		List<Long> sizeList = new ArrayList<>();
		long totalWriteBytes = 0L;
		boolean removeSplitSig = Boolean.FALSE;

		try (OutputStream outputStream = this.openMergeOutputStream(outputPath)) {
			for (int i = 0; i <= this.endCentralDirectoryRecord.getIndexOfThisDisk(); i++) {
				IOUtils.closeStream(input);
				input = this.openSplitFile(i);
				int start = 0;

				if (i == 0) {
					if (this.centralDirectory != null
							&& this.centralDirectory.getFileHeaders() != null
							&& !this.centralDirectory.getFileHeaders().isEmpty()) {
						byte[] buffer = new byte[4];

						input.seek(0L);
						if (input.read(buffer) > 0
								&& RawUtils.readInt(buffer, 0, ByteOrder.LITTLE_ENDIAN) == ZipGlobals.EXTSIG) {
							start = 4;
							removeSplitSig = true;
						}
					}
				}

				long end = input.length();

				if (i == this.endCentralDirectoryRecord.getIndexOfThisDisk()) {
					end = this.endCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory();
				}

				this.copyFile(input, outputStream, start, end);
				totalWriteBytes += (end - start);

				sizeList.add(end);
			}

			ZipFile newFile = (ZipFile) this.clone();
			newFile.endCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(totalWriteBytes);

			newFile.updateSplitZipEntity(sizeList, removeSplitSig);
			newFile.finalizeZipFileWithoutValidations(outputStream);
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B002DL, e);
			}
		} finally {
			IOUtils.closeStream(input);
		}
	}

	/**
	 * <h3 class="en-US">Save the current ZIP file to the given OutputStream instance object</h3>
	 * <h3 class="zh-CN">将ZIP文件保存到给定的输出流中</h3>
	 *
	 * @param outputStream <span class="en-US">The given OutputStream instance object</span>
	 *                     <span class="zh-CN">给定的输出流</span>
	 * @throws ZipException <span class="en-US">An error occurs when save the current ZIP file</span>
	 *                      <span class="zh-CN">保存当前ZIP文件的过程中出错</span>
	 */
	public void finalizeZipFile(final OutputStream outputStream) throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		this.processHeaderData(outputStream);

		long offsetCentralDirectory = this.endCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory();

		long sizeOfCentralDirectory = this.writeCentralDirectory(outputStream);
		this.zip64Format = this.requiresZip64EndRecord(sizeOfCentralDirectory, offsetCentralDirectory);

		int commentLength = Math.max(Globals.INITIALIZE_INT_VALUE, this.endCentralDirectoryRecord.getCommentLength());
		int headerLength = ZipGlobals.ENDHDR + commentLength + (this.zip64Format ? 76 : 0);
		ByteArrayOutputStream headerBytes = new ByteArrayOutputStream(headerLength);

		if (this.zip64Format) {
			this.checkZip64Format();
			this.zip64EndCentralDirectoryLocator
					.setOffsetZip64EndOfCentralDirectoryRecord(offsetCentralDirectory + sizeOfCentralDirectory);

			if (outputStream instanceof SplitOutputStream) {
				this.zip64EndCentralDirectoryLocator.setIndexOfZip64EndOfCentralDirectoryRecord(
						((SplitOutputStream) outputStream).getCurrentSplitFileIndex());
				this.zip64EndCentralDirectoryLocator
						.setTotalNumberOfDiscs(((SplitOutputStream) outputStream).getCurrentSplitFileIndex() + 1);
			} else {
				this.zip64EndCentralDirectoryLocator.setIndexOfZip64EndOfCentralDirectoryRecord(0);
				this.zip64EndCentralDirectoryLocator.setTotalNumberOfDiscs(1);
			}

			this.writeZip64EndOfCentralDirectoryRecord(outputStream, sizeOfCentralDirectory,
					offsetCentralDirectory, headerBytes);
			this.writeZip64EndOfCentralDirectoryLocator(outputStream, headerBytes);
		}

		this.writeEndOfCentralDirectoryRecord(sizeOfCentralDirectory, offsetCentralDirectory, headerBytes);
		this.writeZipHeaderBytes(outputStream, headerBytes.toByteArray());
	}

	private boolean requiresZip64EndRecord(final long directorySize, final long directoryOffset) {
		long entryCount = (this.centralDirectory == null || this.centralDirectory.getFileHeaders() == null)
				? 0L
				: this.centralDirectory.getFileHeaders().size();
		return this.zip64Format
				|| entryCount >= ZipGlobals.ZIP_16_LIMIT
				|| directorySize >= ZipGlobals.ZIP_64_LIMIT
				|| directoryOffset >= ZipGlobals.ZIP_64_LIMIT
				|| this.endCentralDirectoryRecord.getIndexOfThisDisk() >= ZipGlobals.ZIP_16_LIMIT
				|| this.endCentralDirectoryRecord.getIndexOfThisDiskStartOfCentralDirectory() >= ZipGlobals.ZIP_16_LIMIT;
	}

	/**
	 * <h3 class="en-US">Getter method for the central directory instance object</h3>
	 * <h3 class="zh-CN">中央目录实例对象的Getter方法</h3>
	 *
	 * @return <span class="en-US">Central directory instance object</span>
	 * <span class="zh-CN">中央目录实例对象</span>
	 */
	public CentralDirectory getCentralDirectory() {
		return this.centralDirectory;
	}

	/**
	 * <h3 class="en-US">Setter method for the central directory instance object</h3>
	 * <h3 class="zh-CN">中央目录实例对象的Setter方法</h3>
	 *
	 * @param centralDirectory <span class="en-US">Central directory instance object</span>
	 *                         <span class="zh-CN">中央目录实例对象</span>
	 */
	public void setCentralDirectory(final CentralDirectory centralDirectory) {
		this.centralDirectory = centralDirectory;
	}

	/**
	 * <h3 class="en-US">Getter method for the using charset encoding</h3>
	 * <h3 class="zh-CN">使用的字符集的Getter方法</h3>
	 *
	 * @return <span class="en-US">Using charset encoding</span>
	 * <span class="zh-CN">使用的字符集</span>
	 */
	public String getCharsetEncoding() {
		return this.charsetEncoding;
	}

	/**
	 * <h3 class="en-US">Getter method for the file header information list</h3>
	 * <h3 class="zh-CN">文件头信息列表的Getter方法</h3>
	 *
	 * @return <span class="en-US">File header information list</span>
	 * <span class="zh-CN">文件头信息列表</span>
	 */
	public List<LocalFileHeader> getLocalFileHeaderList() {
		return this.localFileHeaderList;
	}

	/**
	 * <h3 class="en-US">Setter method for the file header information list</h3>
	 * <h3 class="zh-CN">文件头信息列表的Setter方法</h3>
	 *
	 * @param localFileHeaderList <span class="en-US">File header information list</span>
	 *                            <span class="zh-CN">文件头信息列表</span>
	 */
	public void setLocalFileHeaderList(final List<LocalFileHeader> localFileHeaderList) {
		this.localFileHeaderList = localFileHeaderList;
	}

	/**
	 * <h3 class="en-US">Getter method for the record of archive extra data</h3>
	 * <h3 class="zh-CN">压缩包扩展数据记录的Getter方法</h3>
	 *
	 * @return <span class="en-US">Record of archive extra data</span>
	 * <span class="zh-CN">压缩包扩展数据记录</span>
	 */
	public ArchiveExtraDataRecord getArchiveExtraDataRecord() {
		return this.archiveExtraDataRecord;
	}

	/**
	 * <h3 class="en-US">Setter method for the record of archive extra data</h3>
	 * <h3 class="zh-CN">压缩包扩展数据记录的Setter方法</h3>
	 *
	 * @param archiveExtraDataRecord <span class="en-US">Record of archive extra data</span>
	 *                               <span class="zh-CN">压缩包扩展数据记录</span>
	 */
	public void setArchiveExtraDataRecord(final ArchiveExtraDataRecord archiveExtraDataRecord) {
		this.archiveExtraDataRecord = archiveExtraDataRecord;
	}

	/**
	 * <h3 class="en-US">Getter method for the end central directory record instance object</h3>
	 * <h3 class="zh-CN">中央目录终止记录实例对象的Getter方法</h3>
	 *
	 * @return <span class="en-US">End central directory record instance object</span>
	 * <span class="zh-CN">中央目录终止记录实例对象</span>
	 */
	public EndCentralDirectoryRecord getEndCentralDirectoryRecord() {
		return this.endCentralDirectoryRecord;
	}

	/**
	 * <h3 class="en-US">Setter method for the end central directory record instance object</h3>
	 * <h3 class="zh-CN">中央目录终止记录实例对象的Setter方法</h3>
	 *
	 * @param endCentralDirectoryRecord <span class="en-US">End central directory record instance object</span>
	 *                                  <span class="zh-CN">中央目录终止记录实例对象</span>
	 */
	public void setEndCentralDirectoryRecord(final EndCentralDirectoryRecord endCentralDirectoryRecord) {
		this.endCentralDirectoryRecord = endCentralDirectoryRecord;
	}

	/**
	 * <h3 class="en-US">Getter method for the archive is split file status</h3>
	 * <h3 class="zh-CN">分卷压缩包标记的Getter方法</h3>
	 *
	 * @return <span class="en-US">Archive is split file status</span>
	 * <span class="zh-CN">分卷压缩包标记</span>
	 */
	public boolean isSplitArchive() {
		return this.splitArchive;
	}

	/**
	 * <h3 class="en-US">Setter method for the archive is split file status</h3>
	 * <h3 class="zh-CN">分卷压缩包标记的Setter方法</h3>
	 *
	 * @param splitArchive <span class="en-US">Archive is split file status</span>
	 *                     <span class="zh-CN">分卷压缩包标记</span>
	 */
	public void setSplitArchive(final boolean splitArchive) {
		this.splitArchive = splitArchive;
	}

	/**
	 * <h3 class="en-US">Setter method for the maximum length of a split item</h3>
	 * <h3 class="zh-CN">分卷大小的Setter方法</h3>
	 *
	 * @param splitLength <span class="en-US">Maximum length of split item</span>
	 *                    <span class="zh-CN">分卷大小</span>
	 */
	public void setSplitLength(final long splitLength) {
		this.splitLength = splitLength;
	}

	private static void checkFilePath(final String filePath) throws ZipException {
		if (StringUtils.isEmpty(filePath)) {
			throw new ZipException(0x0000001B001BL);
		}
		if (FileUtils.isExists(filePath)) {
			throw new ZipException(0x0000001B001CL);
		}
	}

	private void appendCheck(final ZipOptions zipOptions) throws ZipException {
		if (zipOptions == null) {
			throw new ZipException(0x0000001B0021L);
		}

		if (FileUtils.isExists(this.filePath) && this.splitArchive) {
			throw new ZipException(0x0000001B0018L);
		}
	}

	/**
	 * <h3 class="en-US">Static method for create the ZIP file</h3>
	 * <h3 class="zh-CN">静态方法用于创建ZIP文件</h3>
	 *
	 * @param filePath        <span class="en-US">Current zip file path</span>
	 *                        <span class="zh-CN">当前压缩文件路径</span>
	 * @param fileNameCharset <span class="en-US">Using charset encoding</span>
	 *                        <span class="zh-CN">使用的字符集</span>
	 * @param splitArchive    <span class="en-US">Archive is split file status</span>
	 *                        <span class="zh-CN">分卷压缩包标记</span>
	 * @param splitLength     <span class="en-US">Maximum length of split item</span>
	 *                        <span class="zh-CN">分卷大小</span>
	 * @return <span class="en-US">ZIP file instance object</span>
	 * <span class="zh-CN">ZIP文件实例对象</span>
	 * @throws ZipException <span class="en-US">An error occurs when create the ZIP file</span>
	 *                      <span class="zh-CN">创建ZIP文件时出错</span>
	 */
	private static ZipFile createZipFile(final String filePath, final String fileNameCharset,
	                                     final boolean splitArchive, final long splitLength) throws ZipException {
		ZipFile.checkFilePath(filePath);
		return new ZipFile(filePath, fileNameCharset, splitArchive, splitLength);
	}

	private void addFolder(final String folderPath, final ZipOptions zipOptions, final boolean checkSplitArchive)
			throws ZipException {
		if (folderPath == null) {
			throw new ZipException(0x0000001B001EL);
		}

		if (zipOptions == null) {
			throw new ZipException(0x0000001B0021L);
		}

		if (checkSplitArchive && this.splitArchive) {
			throw new ZipException(0x0000001B0018L);
		}

		this.addFolderToZip(folderPath, zipOptions);
	}

	private static String getFileNameFromFilePath(final File file) throws ZipException {
		if (file == null) {
			throw new ZipException(0x0000001B001EL);
		}

		if (file.isDirectory()) {
			return null;
		}

		return file.getName();
	}

	private boolean isNoEntry() {
		return this.centralDirectory.getFileHeaders().isEmpty();
	}

	private boolean isDirectory(final String entryPath) throws ZipException {
		return Optional.ofNullable(this.retrieveGeneralFileHeader(entryPath))
				.map(GeneralFileHeader::isDirectory)
				.orElse(Boolean.FALSE);
	}

	private List<String> listFolderGeneralFileHeaders(final String folderPath) throws ZipException {
		if (StringUtils.isEmpty(folderPath)) {
			throw new ZipException(0x0000001B0025L);
		}
		if (this.centralDirectory == null) {
			throw new ZipException(0x0000001B0028L);
		}
		return this.centralDirectory.listFolderGeneralFileHeaders(folderPath);
	}

	private GeneralFileHeader retrieveGeneralFileHeader(final String entryPath) throws ZipException {
		if (StringUtils.isEmpty(entryPath)) {
			throw new ZipException(0x0000001B0025L);
		}
		if (this.centralDirectory == null) {
			throw new ZipException(0x0000001B0028L);
		}
		return this.centralDirectory.retrieveGeneralFileHeader(entryPath);
	}

	private void removeFilesIfExists(final List<String> entryList) throws ZipException {
		if (this.centralDirectory != null
				&& this.centralDirectory.getFileHeaders() != null
				&& !this.centralDirectory.getFileHeaders().isEmpty()) {
			for (String entryPath : entryList) {
				GeneralFileHeader generalFileHeader = this.retrieveGeneralFileHeader(entryPath);
				if (generalFileHeader != null) {
					this.removeExistsFile(generalFileHeader);
				}
			}
		}
	}

	private ZipOutputStream openOutputStream() throws IOException, ZipException {
		SplitOutputStream splitOutputStream = SplitOutputStream.newInstance(this.filePath, this.splitLength);
		ZipOutputStream zipOutputStream = new ZipOutputStream(splitOutputStream, this);
		if (FileUtils.isExists(this.filePath)) {
			if (this.endCentralDirectoryRecord == null) {
				throw new ZipException(0x0000001B0029L);
			}
			splitOutputStream.seek(this.endCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory());
		}
		return zipOutputStream;
	}

	private void addStreamToZip(final InputStream inputStream, final ZipOptions zipOptions) throws ZipException {
		if (zipOptions == null) {
			throw new ZipException(0x0000001B0021L);
		}

		if (inputStream == null) {
			throw new ZipException(0x0000001B0031L);
		}

		try (ZipOutputStream outputStream = this.openOutputStream()) {
			ZipOptions streamOptions = zipOptions.cloneBuilder().build();
			streamOptions.setSourceExternalStream(Boolean.TRUE);
			streamOptions.setSourceFileCRC(zipOptions.getSourceFileCRC());
			streamOptions.setDefaultFolderPath(zipOptions.getDefaultFolderPath());

			this.checkOptions(streamOptions);

			byte[] readBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
			int readLength;

			outputStream.putNextEntry(null, streamOptions);

			if (!streamOptions.getFileNameInZip().endsWith(Globals.DEFAULT_ZIP_PAGE_SEPARATOR)
					&& !streamOptions.getFileNameInZip().endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
				while ((readLength = inputStream.read(readBuffer)) != Globals.DEFAULT_VALUE_INT) {
					outputStream.write(readBuffer, 0, readLength);
				}
			}

			outputStream.closeEntry();
			outputStream.finish();
		} catch (Exception e) {
			throw new ZipException(0x0000001B0015L, e);
		}
	}

	private void addFolderToZip(final String folderPath, final ZipOptions zipOptions) throws ZipException {
		if (StringUtils.isEmpty(folderPath) || !FileUtils.isExists(folderPath)) {
			throw new ZipException(0x0000001B0031L);
		}

		if (!FileUtils.isDirectory(folderPath)) {
			throw new ZipException(0x0000001B0036L);
		}

		if (!FileUtils.canRead(folderPath)) {
			throw new ZipException(0x0000001B001AL);
		}

		if (zipOptions == null) {
			throw new ZipException(0x0000001B0021L);
		}

		String rootFolderPath;

		if (zipOptions.isIncludeRootFolder()) {
			try {
				File file = FileUtils.getFile(folderPath);
				rootFolderPath = file.getAbsoluteFile().getParentFile() != null
						? file.getAbsoluteFile().getParentFile().getAbsolutePath() : Globals.DEFAULT_VALUE_STRING;
			} catch (FileNotFoundException e) {
				throw new ZipException(0x0000001B0037L, folderPath);
			}
		} else {
			rootFolderPath = folderPath;
		}

		zipOptions.setDefaultFolderPath(rootFolderPath);

		List<String> fileList = new ArrayList<>();
		try {
			File folder = FileUtils.getFile(folderPath);
			if (zipOptions.isIncludeRootFolder()) {
				fileList.add(folderPath);
			}
			fileList.addAll(FileUtils.listFiles(folder, zipOptions.isReadHiddenFiles(), zipOptions.isIncludeRootFolder()));
		} catch (Exception e) {
			throw new ZipException(0x0000001B0015L, e);
		}

		this.addFiles(fileList, zipOptions);
	}

	private void checkZip64Format() {
		if (this.zip64EndCentralDirectoryRecord == null) {
			this.zip64EndCentralDirectoryRecord = new Zip64EndCentralDirectoryRecord();
		}

		if (this.zip64EndCentralDirectoryLocator == null) {
			this.zip64EndCentralDirectoryLocator = new Zip64EndCentralDirectoryLocator();
		}
	}

	private void finalizeZipFileWithoutValidations(final OutputStream outputStream) throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		try {
			long offsetCentralDirectory = this.endCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory();

			long sizeOfCentralDirectory = this.writeCentralDirectory(outputStream);
			this.zip64Format = this.requiresZip64EndRecord(sizeOfCentralDirectory, offsetCentralDirectory);

			int commentLength = Math.max(Globals.INITIALIZE_INT_VALUE, this.endCentralDirectoryRecord.getCommentLength());
			int headerLength = ZipGlobals.ENDHDR + commentLength + (this.zip64Format ? 76 : 0);
			ByteArrayOutputStream headerBytes = new ByteArrayOutputStream(headerLength);

			if (this.zip64Format) {
				this.checkZip64Format();

				this.zip64EndCentralDirectoryLocator
						.setOffsetZip64EndOfCentralDirectoryRecord(offsetCentralDirectory + sizeOfCentralDirectory);
				if (outputStream instanceof SplitOutputStream) {
					long fileIndex = ((SplitOutputStream) outputStream).getCurrentSplitFileIndex();
					this.zip64EndCentralDirectoryLocator.setIndexOfZip64EndOfCentralDirectoryRecord(fileIndex);
					this.zip64EndCentralDirectoryLocator.setTotalNumberOfDiscs(fileIndex + 1L);
				} else {
					this.zip64EndCentralDirectoryLocator.setIndexOfZip64EndOfCentralDirectoryRecord(0L);
					this.zip64EndCentralDirectoryLocator.setTotalNumberOfDiscs(1L);
				}
				this.writeZip64EndOfCentralDirectoryRecord(outputStream, sizeOfCentralDirectory,
						offsetCentralDirectory, headerBytes);
				this.writeZip64EndOfCentralDirectoryLocator(outputStream, headerBytes);
			}

			this.writeEndOfCentralDirectoryRecord(sizeOfCentralDirectory, offsetCentralDirectory, headerBytes);
			this.writeZipHeaderBytes(outputStream, headerBytes.toByteArray());
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0015L, e);
			}
		}
	}

	private void extractFile(final GeneralFileHeader generalFileHeader, final String destPath,
	                         final boolean ignoreFileAttr) throws ZipException {
		if (generalFileHeader == null) {
			throw new ZipException(0x0000001B000FL);
		}

		try {
			if (generalFileHeader.isDirectory()) {
				Path rootPath = extractRoot(destPath);
				extractTarget(rootPath, generalFileHeader.getEntryPath(), Boolean.TRUE);
			} else {
				if (!FileUtils.isExists(destPath)) {
					FileUtils.makeDir(destPath);
				}
				if (!FileUtils.isDirectory(destPath)) {
					throw new ZipException(0x0000001B0038L);
				}

				this.extractFileToPath(generalFileHeader, destPath, ignoreFileAttr);
			}
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0043L, e);
			}
		}
	}

	private void addFilesToZip(final List<String> fileList, final ZipOptions zipOptions) throws ZipException {
		if (CollectionUtils.isEmpty(fileList)) {
			throw new ZipException(0x0000001B0031L);
		}

		if (this.endCentralDirectoryRecord == null) {
			this.endCentralDirectoryRecord = new EndCentralDirectoryRecord();
			this.endCentralDirectoryRecord.setSignature(ZipGlobals.ENDSIG);
			this.endCentralDirectoryRecord.setIndexOfThisDisk(0);
			this.endCentralDirectoryRecord.setTotalOfEntriesInCentralDirectory(0);
			this.endCentralDirectoryRecord.setTotalOfEntriesInCentralDirectoryOnThisDisk(0);
			this.endCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(0);
		}

		this.checkOptions(zipOptions);
		List<String> entryList = new ArrayList<>();
		for (String filePath : fileList) {
			entryList.add(ZipFile.getRelativeFileName(filePath,
					zipOptions.getRootFolderInZip(), zipOptions.getDefaultFolderPath()));
		}
		this.removeFilesIfExists(entryList);

		InputStream inputStream = null;
		try (ZipOutputStream outputStream = this.openOutputStream()) {
			byte[] readBuffer = new byte[ZipGlobals.IO_BUFFER_SIZE];
			int readLength;

			for (String filePath : fileList) {
				inputStream = FileUtils.loadFile(filePath);
				ZipOptions fileOptions = zipOptions.cloneBuilder().build();

				if (!FileUtils.isDirectory(filePath)) {
					if (FileUtils.fileSize(filePath) == 0L) {
						fileOptions = zipOptions.cloneBuilder().noCrypto().build();
					}
					if (fileOptions.isEncryptFiles()
							&& fileOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_STANDARD) {
						fileOptions.setSourceFileCRC(FileUtils.calcFileCRC(filePath));
					}
				}

				outputStream.putNextEntry(FileUtils.getFile(filePath), fileOptions);
				if (FileUtils.isDirectory(filePath)) {
					outputStream.closeEntry();
					continue;
				}

				while ((readLength = inputStream.read(readBuffer)) != Globals.DEFAULT_VALUE_INT) {
					outputStream.write(readBuffer, 0, readLength);
				}
				outputStream.closeEntry();
				IOUtils.closeStream(inputStream);
			}

			outputStream.finish();
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0015L, e);
			}
		} finally {
			IOUtils.closeStream(inputStream);
		}
	}

	private void checkOptions(final ZipOptions zipOptions) throws ZipException {
		if (zipOptions == null) {
			throw new ZipException(0x0000001B0021L);
		}

		if (zipOptions.getCompressionMethod() != ZipGlobals.COMP_STORE
				&& zipOptions.getCompressionMethod() != ZipGlobals.COMP_DEFLATE) {
			throw new ZipException(0x0000001B004AL);
		}

		if (zipOptions.getCompressionMethod() == ZipGlobals.COMP_DEFLATE
				&& (zipOptions.getCompressionLevel() < 0 || zipOptions.getCompressionLevel() > 9)) {
			throw new ZipException(0x0000001B004BL);
		}

		if (zipOptions.isEncryptFiles()) {
			if (zipOptions.getEncryptionMethod() != ZipGlobals.ENC_METHOD_STANDARD
					&& zipOptions.getEncryptionMethod() != ZipGlobals.ENC_METHOD_STRONG
					&& zipOptions.getEncryptionMethod() != ZipGlobals.ENC_METHOD_AES) {
				throw new ZipException(0x0000001B0001L);
			}

			if (zipOptions.getPassword() == null || zipOptions.getPassword().length == 0) {
				throw new ZipException(0x0000001B004CL);
			}
		}
	}

	private void removeExistsFile(final GeneralFileHeader generalFileHeader) throws ZipException {
		if (generalFileHeader == null) {
			throw new ZipException(0x0000001B000FL);
		}

		if (this.splitArchive) {
			throw new ZipException(0x0000001B0018L);
		}

		boolean success = Boolean.FALSE;
		String tempFileName = this.filePath + System.currentTimeMillis() % 1000L;

		try (SplitOutputStream outputStream = SplitOutputStream.newInstance(tempFileName);
		     StandardFile input = this.createFileHandler(generalFileHeader)) {
			int indexOfHeader = this.retrieveIndexOfGeneralFileHeader(generalFileHeader);
			if (indexOfHeader < 0) {
				return;
			}

			while (FileUtils.isExists(tempFileName)) {
				tempFileName = this.filePath + System.currentTimeMillis() % 1000L;
			}

			if (!this.readLocalFileHeader(input, generalFileHeader).verifyPassword(input)) {
				throw new ZipException(0x0000001B000DL);
			}

			long offsetLocalFileHeader = generalFileHeader.getOffsetLocalHeader();
			if (generalFileHeader.getZip64ExtendInfo() != null
					&& generalFileHeader.getZip64ExtendInfo().getOffsetLocalHeader() != Globals.DEFAULT_VALUE_LONG) {
				offsetLocalFileHeader = generalFileHeader.getZip64ExtendInfo().getOffsetLocalHeader();
			}

			long offsetStartCentralDirectory = this.endCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory();
			if (this.zip64Format && this.zip64EndCentralDirectoryRecord != null) {
				offsetStartCentralDirectory = this.zip64EndCentralDirectoryRecord.getOffsetStartCenDirWRTStartDiskNo();
			}

			long offsetEndOfCompressedFile = Globals.DEFAULT_VALUE_LONG;

			List<GeneralFileHeader> fileHeaders = this.centralDirectory.getFileHeaders();
			if (indexOfHeader == fileHeaders.size() - 1) {
				offsetEndOfCompressedFile = offsetStartCentralDirectory - 1;
			} else {
				GeneralFileHeader nextFileHeader = fileHeaders.get(indexOfHeader + 1);
				if (nextFileHeader != null) {
					offsetEndOfCompressedFile = nextFileHeader.getOffsetLocalHeader() - 1;
					if (nextFileHeader.getZip64ExtendInfo() != null
							&& nextFileHeader.getZip64ExtendInfo().getOffsetLocalHeader() != Globals.DEFAULT_VALUE_LONG) {
						offsetEndOfCompressedFile = nextFileHeader.getZip64ExtendInfo().getOffsetLocalHeader() - 1;
					}
				}
			}

			if (offsetLocalFileHeader < 0L || offsetEndOfCompressedFile < 0L) {
				throw new ZipException(0x0000001B0044L);
			}

			if (indexOfHeader == 0) {
				if (this.centralDirectory.getFileHeaders().size() > 1) {
					this.copyFile(input, outputStream, offsetEndOfCompressedFile + 1L, offsetStartCentralDirectory);
				}
			} else if (indexOfHeader == (fileHeaders.size() - 1)) {
				this.copyFile(input, outputStream, 0, offsetLocalFileHeader);
			} else {
				this.copyFile(input, outputStream, 0, offsetLocalFileHeader);
				this.copyFile(input, outputStream, offsetEndOfCompressedFile + 1L, offsetStartCentralDirectory);
			}

			this.endCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(outputStream.getFilePointer());
			this.endCentralDirectoryRecord.setTotalOfEntriesInCentralDirectory(this.endCentralDirectoryRecord.getTotalOfEntriesInCentralDirectory() - 1);
			this.endCentralDirectoryRecord.setTotalOfEntriesInCentralDirectoryOnThisDisk(this.endCentralDirectoryRecord.getTotalOfEntriesInCentralDirectoryOnThisDisk() - 1);

			this.centralDirectory.getFileHeaders().remove(indexOfHeader);

			for (int i = indexOfHeader; i < this.centralDirectory.getFileHeaders().size(); i++) {
				long offsetLocalHeader = this.centralDirectory.getFileHeaders().get(i).getOffsetLocalHeader();
				if (this.centralDirectory.getFileHeaders().get(i).getZip64ExtendInfo() != null
						&& this.centralDirectory.getFileHeaders().get(i).getZip64ExtendInfo().getOffsetLocalHeader() != Globals.DEFAULT_VALUE_LONG) {
					offsetLocalHeader = this.centralDirectory.getFileHeaders().get(i).getZip64ExtendInfo().getOffsetLocalHeader();
				}

				this.centralDirectory.getFileHeaders().get(i).setOffsetLocalHeader(offsetLocalHeader - (offsetEndOfCompressedFile - offsetLocalFileHeader) - 1);
			}

			this.finalizeZipFile(outputStream);
			success = true;
		} catch (IOException e) {
			throw new ZipException(0x0000001B0044L, e);
		} finally {
			if (success) {
				FileUtils.copy(tempFileName, this.filePath);
			}
			if (FileUtils.isExists(tempFileName)) {
				FileUtils.removeFile(tempFileName);
			}
		}
	}

	private int retrieveIndexOfGeneralFileHeader(final GeneralFileHeader generalFileHeader) throws ZipException {
		if (generalFileHeader == null) {
			throw new ZipException(0x0000001B000FL);
		}
		if (this.centralDirectory == null) {
			throw new ZipException(0x0000001B0028L);
		}
		return this.centralDirectory.retrieveIndexOfGeneralFileHeader(generalFileHeader);
	}

	private StandardFile createFileHandler(final GeneralFileHeader generalFileHeader)
			throws FileNotFoundException, ZipException {
		if (StringUtils.isEmpty(this.filePath)) {
			throw new ZipException(0x0000001B001BL);
		}

		if (!this.splitArchive) {
			return new StandardFile(this.filePath);
		}
		String splitPath = this.filePath.substring(0, this.filePath.lastIndexOf("."));
		if (this.numberFormattedName) {
			int diskNumberStart = Long.valueOf(generalFileHeader.getOffsetLocalHeader() / this.splitLength).intValue();
			if (diskNumberStart < 9) {
				splitPath += (".00" + (diskNumberStart + 1));
			} else if (diskNumberStart < 99) {
				splitPath += (".0" + (diskNumberStart + 1));
			} else {
				splitPath += ("." + (diskNumberStart + 1));
			}
		} else {
			long diskNumberStart = generalFileHeader.getDiskNumberStart();

			if (this.endCentralDirectoryRecord.getIndexOfThisDiskStartOfCentralDirectory() == diskNumberStart) {
				splitPath = this.filePath;
			} else {
				if (diskNumberStart < 9) {
					splitPath += (".zip.0" + (diskNumberStart + 1));
				} else {
					splitPath += (".zip." + (diskNumberStart + 1));
				}
			}
		}
		return new StandardFile(splitPath);
	}

	private void copyFile(final StandardFile input, final OutputStream outputStream, final long start, final long end)
			throws ZipException {
		if (input == null) {
			throw new ZipException(0x0000001B0031L);
		}

		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		if (start < 0 || end < 0 || start > end) {
			throw new IndexOutOfBoundsException();
		}

		if (start == end) {
			return;
		}

		try {
			input.seek(start);

			int bufferSize = Globals.DEFAULT_BUFFER_SIZE;
			if ((end - start) < Globals.DEFAULT_BUFFER_SIZE) {
				bufferSize = (int) (end - start);
			}

			int readLength;
			byte[] readBuffer = new byte[bufferSize];
			long totalRead = 0L;
			long limitRead = end - start;

			while (true) {
				readLength = input.read(readBuffer);
				if (readLength == Globals.DEFAULT_VALUE_INT) {
					break;
				}
				outputStream.write(readBuffer, 0, readLength);

				totalRead += readLength;
				if (totalRead == limitRead) {
					break;
				}

				if (totalRead + readBuffer.length > limitRead) {
					readBuffer = new byte[(int) (limitRead - totalRead)];
				}
			}
		} catch (Exception e) {
			throw new ZipException(0x0000001B004DL, e);
		}
	}

	private void extractFileToPath(final GeneralFileHeader generalFileHeader, final String destPath,
	                               final boolean ignoreFileAttr) throws ZipException {
		if (generalFileHeader == null) {
			throw new ZipException(0x0000001B000FL);
		}

		try (ZipInputStream inputStream = this.openInputStream(generalFileHeader);
		     OutputStream outputStream = this.openOutputStream(destPath, generalFileHeader.getEntryPath())) {

			byte[] buffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
			int readLength;

			while ((readLength = inputStream.read(buffer)) != Globals.DEFAULT_VALUE_INT) {
				outputStream.write(buffer, 0, readLength);
			}

			if (generalFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
				this.checkMac();
			} else {
				long calculatedCRC = inputStream.crcValue() & 0xFFFFFFFFL;
				if (calculatedCRC != generalFileHeader.getCrc32()) {
					throw new ZipException(0x0000001B0019L);
				}
			}
		} catch (IOException e) {
			throw new ZipException(0x0000001B004EL, e);
		}

		try {
			String filePath = destPath;
			if (!filePath.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
				filePath += Globals.DEFAULT_PAGE_SEPARATOR;
			}
			filePath += generalFileHeader.getEntryPath();
			if (generalFileHeader.getExternalFileAttr() != null && !ignoreFileAttr) {
				if (generalFileHeader.getExternalFileAttr()[0] == Globals.FILE_MODE_READ_ONLY) {
					setFileReadOnly(FileUtils.getFile(filePath));
				}

				setFileLastModify(FileUtils.getFile(filePath),
						DateTimeUtils.dosToJavaTime(generalFileHeader.getLastModFileTime()));
			}
		} catch (FileNotFoundException e) {
			throw new ZipException(0x0000001B0043L, e);
		}
	}

	private void checkMac() throws ZipException {
		if (this.decryptor instanceof AESDecryptor) {
			byte[] tempMacBytes;
			try {
				tempMacBytes = ((AESDecryptor) this.decryptor).calculateAuthenticationBytes();
			} catch (CryptoException e) {
				throw new ZipException(0x0000001B0019L);
			}
			byte[] storedMac = ((AESDecryptor) this.decryptor).getStoredMac();
			byte[] calculateMac = new byte[ZipGlobals.AES_AUTH_LENGTH];

			if (storedMac == null) {
				throw new ZipException(0x0000001B0019L);
			}

			System.arraycopy(tempMacBytes, 0, calculateMac, 0, ZipGlobals.AES_AUTH_LENGTH);

			if (!Arrays.equals(calculateMac, storedMac)) {
				throw new ZipException(0x0000001B0019L);
			}
		}
	}

	private int readEntryLength(final GeneralFileHeader generalFileHeader) throws ZipException {
		if (generalFileHeader == null) {
			throw new ZipException(0x0000001B000FL);
		}

		try (ZipInputStream inputStream = this.openInputStream(generalFileHeader)) {
			return inputStream.available();
		} catch (IOException e) {
			throw new ZipException(0x0000001B0049L, e);
		}
	}

	private byte[] readEntry(final GeneralFileHeader generalFileHeader, final long position, final int dataLength)
			throws ZipException {
		if (generalFileHeader == null) {
			throw new ZipException(0x0000001B000FL);
		}

		try (ZipInputStream inputStream = this.openInputStream(generalFileHeader);
		     ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) generalFileHeader.getOriginalSize())) {
			int totalLength = 0, readLength;

			if (position > 0L) {
				long skipLength = inputStream.skip(position);
				if (skipLength != position) {
					return new byte[0];
				}
			}

			if (dataLength > 0) {
				byte[] readBuffer;
				while (true) {
					int bufferLength = Integer.min(dataLength - totalLength, Globals.DEFAULT_BUFFER_SIZE);
					readBuffer = new byte[bufferLength];

					if ((readLength = inputStream.read(readBuffer)) == Globals.DEFAULT_VALUE_INT) {
						break;
					}
					outputStream.write(readBuffer, 0, readLength);
					totalLength += readLength;

					if (totalLength == dataLength) {
						break;
					}
				}
			} else {
				byte[] readBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
				while ((readLength = inputStream.read(readBuffer)) != Globals.DEFAULT_VALUE_INT) {
					outputStream.write(readBuffer, 0, readLength);
					totalLength += readLength;
				}
			}

			if (generalFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
				this.checkMac();
			} else {
				long calculatedCRC = inputStream.crcValue();
				if (calculatedCRC != generalFileHeader.getCrc32()) {
					throw new ZipException(0x0000001B0019L);
				}
			}

			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new ZipException(0x0000001B0049L, e);
		}
	}

	private ZipInputStream openInputStream(final GeneralFileHeader generalFileHeader) throws ZipException {
		try (StandardFile input = this.createFileHandler(generalFileHeader)) {
			LocalFileHeader localFileHeader = this.readLocalFileHeader(input, generalFileHeader);

			if (localFileHeader.getCompressionMethod() != generalFileHeader.getCompressionMethod()) {
				throw new ZipException(0x0000001B004AL);
			}

			if (localFileHeader.isEncrypted()) {
				if (localFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
					byte[] salt = null;
					if (localFileHeader.getAesExtraDataRecord() != null) {
						salt = new byte[HeaderOperator.saltLength(localFileHeader.getAesExtraDataRecord().getAesStrength())];
						input.seek(localFileHeader.getOffsetStartOfData());
						if (input.read(salt) == Globals.DEFAULT_VALUE_INT) {
							salt = null;
						}
					}

					byte[] passwordBytes = new byte[2];

					if (input.read(passwordBytes) > 0) {
						this.decryptor = new AESDecryptor(localFileHeader, salt, passwordBytes);
					}
				} else if (localFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_STANDARD) {
					byte[] decryptorHeader = new byte[ZipGlobals.STD_DEC_HDR_SIZE];
					input.seek(localFileHeader.getOffsetStartOfData());

					if (input.read(decryptorHeader) > 0) {
						this.decryptor = StandardDecryptor.newInstance(localFileHeader, decryptorHeader);
					}
				} else {
					throw new ZipException(0x0000001B0001L);
				}
			}

			long compressedSize = localFileHeader.getCompressedSize();
			long offsetStartOfData = localFileHeader.getOffsetStartOfData();

			if (localFileHeader.isEncrypted()) {
				if (localFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
					if (this.decryptor instanceof AESDecryptor) {
						compressedSize -= (((AESDecryptor) this.decryptor).getSaltLength() +
								ZipGlobals.PASSWORD_VERIFIER_LENGTH + 10);
						offsetStartOfData += (((AESDecryptor) this.decryptor).getSaltLength() +
								ZipGlobals.PASSWORD_VERIFIER_LENGTH);
					} else {
						throw new ZipException(0x0000001B0050L, localFileHeader.getEntryPath());
					}
				} else if (localFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_STANDARD) {
					compressedSize -= ZipGlobals.STD_DEC_HDR_SIZE;
					offsetStartOfData += ZipGlobals.STD_DEC_HDR_SIZE;
				}
			}

			int compressionMethod = localFileHeader.getCompressionMethod();
			if (generalFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
				if (generalFileHeader.getAesExtraDataRecord() == null) {
					throw new ZipException(0x0000001B0010L);
				}
				compressionMethod = generalFileHeader.getAesExtraDataRecord().getCompressionMethod();
			}
			input.seek(offsetStartOfData);

			boolean isAESEncryptedFile = generalFileHeader.isEncrypted()
					&& generalFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES;

			int currentIndex;
			if (this.numberFormattedName) {
				currentIndex = Long.valueOf(generalFileHeader.getOffsetLocalHeader() / this.splitLength).intValue();
			} else {
				long diskNumberStart = generalFileHeader.getDiskNumberStart();
				if (diskNumberStart < 0L || diskNumberStart > Integer.MAX_VALUE) {
					throw new ZipException(0x0000001B0059L);
				}
				currentIndex = (int) diskNumberStart;
			}
			return switch (compressionMethod) {
				case ZipGlobals.COMP_STORE -> new ZipInputStream(PartInputStream.newInstance(this, currentIndex,
						offsetStartOfData, compressedSize, this.decryptor, isAESEncryptedFile));
				case ZipGlobals.COMP_DEFLATE -> new ZipInputStream(InflaterInputStream.newInstance(this, currentIndex,
						offsetStartOfData, compressedSize, generalFileHeader.getOriginalSize(), this.decryptor,
						isAESEncryptedFile));
				default -> throw new ZipException(0x0000001B004AL);
			};
		} catch (ZipException | IOException e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0043L, e);
			}
		}
	}

	private static Path extractRoot(final String folderPath) throws IOException, ZipException {
		Path rootPath = Path.of(folderPath).toAbsolutePath().normalize();
		Files.createDirectories(rootPath);
		if (Files.isSymbolicLink(rootPath)) {
			throw new ZipException(0x0000001B0038L, rootPath);
		}
		return rootPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
	}

	private static Path extractTarget(final Path rootPath, final String entryName, final boolean directory)
			throws IOException, ZipException {
		String entryPath = StringUtils.replace(entryName, "\\", Globals.DEFAULT_ZIP_PAGE_SEPARATOR);
		entryPath = StringUtils.replace(entryPath, Globals.DEFAULT_ZIP_PAGE_SEPARATOR, Globals.DEFAULT_PAGE_SEPARATOR);
		Path relative = Path.of(entryPath).normalize();
		if (relative.isAbsolute() || relative.getNameCount() == 0 || relative.startsWith("..")) {
			throw new ZipException(0x0000001B0038L, entryName);
		}

		Path target = rootPath.resolve(relative).normalize();
		if (!target.startsWith(rootPath)) {
			throw new ZipException(0x0000001B0038L, entryName);
		}
		Path lastDirectory = directory ? target : target.getParent();
		if (lastDirectory != null) {
			Path cursor = rootPath;
			for (Path part : rootPath.relativize(lastDirectory)) {
				cursor = cursor.resolve(part);
				if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
					if (Files.isSymbolicLink(cursor)
							|| !Files.isDirectory(cursor, LinkOption.NOFOLLOW_LINKS)) {
						throw new ZipException(0x0000001B0038L, entryName);
					}
				} else {
					Files.createDirectory(cursor);
				}
			}
		}

		if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
				&& Files.isSymbolicLink(target)) {
			throw new ZipException(0x0000001B0038L, entryName);
		}
		return target;
	}

	private FileOutputStream openOutputStream(final String folderPath, final String fileName) throws ZipException {
		if (StringUtils.isEmpty(folderPath) || StringUtils.isEmpty(fileName) || !FileUtils.makeDir(folderPath)) {
			throw new ZipException(0x0000001B0043L);
		}

		try {
			Path rootPath = extractRoot(folderPath);
			Path target = extractTarget(rootPath, fileName, false);
			return new FileOutputStream(target.toFile());
		} catch (IOException e) {
			throw new ZipException(0x0000001B0038L, e);
		}
	}

	private OutputStream openMergeOutputStream(final String outputPath) throws ZipException {
		if (StringUtils.isEmpty(outputPath)) {
			throw new ZipException(0x0000001B0038L);
		}

		try {
			return new FileOutputStream(FileUtils.getFile(outputPath));
		} catch (FileNotFoundException e) {
			throw new ZipException(0x0000001B0038L, e);
		}
	}

	/**
	 * Open a split file nervousync random access file.
	 *
	 * @param index the index
	 * @return the nervousync random access file
	 * @throws FileNotFoundException the zip exception
	 */
	public StandardFile openSplitFile(final int index) throws IOException {
		if (this.splitArchive) {
			if (index < 0) {
				throw new FileNotFoundException("invalid index, cannot create split file handler");
			}

			String currentSplitFile = this.currentSplitFileName(index);

			if (!FileUtils.isExists(currentSplitFile)) {
				throw new ZipException(0x0000001B0058L);
			}

			return new StandardFile(currentSplitFile);
		}
		return new StandardFile(this.filePath);
	}

	private String currentSplitFileName(final int index) {
		String currentSplitFile;
		if (index == this.endCentralDirectoryRecord.getIndexOfThisDisk()) {
			currentSplitFile = this.filePath;
		} else {
			currentSplitFile = this.filePath.substring(0, this.filePath.lastIndexOf('.'));
			if (this.numberFormattedName) {
				if (index < 9) {
					currentSplitFile += (".00" + (index + 1));
				} else if (index < 99) {
					currentSplitFile += (".0" + (index + 1));
				} else {
					currentSplitFile += ("." + (index + 1));
				}
			} else {
				if (index < 9) {
					currentSplitFile += (".zip.0" + (index + 1));
				} else {
					currentSplitFile += (".zip." + (index + 1));
				}
			}
		}
		return currentSplitFile;
	}

	private void updateSplitZipEntity(final List<Long> sizeList, final boolean removeSplitSig) throws ZipException {
		this.splitArchive = Boolean.FALSE;
		this.updateSplitZipHeader(sizeList, removeSplitSig);
		this.updateSplitEndCentralDirectory();

		if (this.zip64Format) {
			this.updateSplitZip64EndCentralDirectoryLocator(sizeList);
			this.updateSplitZip64EndCentralDirectoryRecord(sizeList);
		}
	}

	private void updateSplitZipHeader(final List<Long> sizeList, final boolean removeSplitSig) throws ZipException {
		if (this.centralDirectory == null) {
			throw new ZipException(0x0000001B0028L);
		}

		int splitSigOverhead = 0;
		if (removeSplitSig) {
			splitSigOverhead = 4;
		}

		List<GeneralFileHeader> newFileHeaders = new ArrayList<>();

		for (GeneralFileHeader generalFileHeader : this.centralDirectory.getFileHeaders()) {
			long offsetHeaderToAdd = 0L;

			long diskNumberStart = generalFileHeader.getDiskNumberStart();
			if (diskNumberStart < 0L || diskNumberStart > sizeList.size()) {
				throw new ZipException(0x0000001B0059L);
			}

			for (int i = 0; i < (int) diskNumberStart ; i++) {
				offsetHeaderToAdd += sizeList.get(i);
			}

			generalFileHeader.setOffsetLocalHeader(generalFileHeader.getOffsetLocalHeader() + offsetHeaderToAdd - splitSigOverhead);
			generalFileHeader.setDiskNumberStart(0);

			newFileHeaders.add(generalFileHeader);
		}

		this.centralDirectory.setFileHeaders(newFileHeaders);
	}

	private void updateSplitEndCentralDirectory() throws ZipException {
		if (this.centralDirectory == null) {
			throw new ZipException(0x0000001B0028L);
		}

		this.endCentralDirectoryRecord.setIndexOfThisDisk(0);
		this.endCentralDirectoryRecord.setIndexOfThisDiskStartOfCentralDirectory(0);
		this.endCentralDirectoryRecord.setTotalOfEntriesInCentralDirectory(
				this.centralDirectory.getFileHeaders().size());
		this.endCentralDirectoryRecord.setTotalOfEntriesInCentralDirectoryOnThisDisk(
				this.centralDirectory.getFileHeaders().size());
	}

	private void updateSplitZip64EndCentralDirectoryLocator(final List<Long> sizeList) {
		if (this.zip64EndCentralDirectoryLocator == null) {
			return;
		}
		this.zip64EndCentralDirectoryLocator.setIndexOfZip64EndOfCentralDirectoryRecord(0);
		long offsetZip64EndCentralDirRec = 0;

		for (Long recordSize : sizeList) {
			offsetZip64EndCentralDirRec += recordSize;
		}
		this.zip64EndCentralDirectoryLocator.setOffsetZip64EndOfCentralDirectoryRecord(
				this.zip64EndCentralDirectoryLocator.getOffsetZip64EndOfCentralDirectoryRecord() +
						offsetZip64EndCentralDirRec);
		this.zip64EndCentralDirectoryLocator.setTotalNumberOfDiscs(1);
	}

	private void updateSplitZip64EndCentralDirectoryRecord(final List<Long> sizeList) {
		if (this.zip64EndCentralDirectoryRecord == null) {
			return;
		}
		this.zip64EndCentralDirectoryRecord.setIndex(0);
		this.zip64EndCentralDirectoryRecord.setStartOfCentralDirectory(0);
		this.zip64EndCentralDirectoryRecord.setTotalEntriesInCentralDirectoryOnThisDisk(
				this.endCentralDirectoryRecord.getTotalOfEntriesInCentralDirectory());

		long offsetStartCenDirWRTStartDiskNo = 0;

		for (Long recordSize : sizeList) {
			offsetStartCenDirWRTStartDiskNo += recordSize;
		}

		this.zip64EndCentralDirectoryRecord.setOffsetStartCenDirWRTStartDiskNo(
				this.zip64EndCentralDirectoryRecord.getOffsetStartCenDirWRTStartDiskNo() +
						offsetStartCenDirWRTStartDiskNo);
	}

	private void readHeaders() throws ZipException {
		try (StandardFile input = retrieveHeaderFile()) {
			this.readEndOfCentralDirectoryRecord(input);

			// Check and set the zip64 format
			this.readZip64EndCentralDirectoryLocator(input);

			if (this.zip64Format) {
				this.readZip64EndCentralDirectoryRecord(input);
			}

			this.readCentralDirectory(input);
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0049L, e);
			}
		}
	}

	private StandardFile retrieveHeaderFile() throws FileNotFoundException {
		if (this.filePath.endsWith(".001")) {
			String folderPath = this.filePath.substring(0, this.filePath.lastIndexOf(Globals.DEFAULT_PAGE_SEPARATOR));
			final String fileName = Optional.ofNullable(StringUtils.getFilename(this.filePath))
					.map(name -> name.substring(0, name.length() - 4))
					.orElse(Globals.DEFAULT_VALUE_STRING);
			List<String> fileList = FileUtils.listFiles(folderPath, (dir, name) -> name.startsWith(fileName));
			fileList.sort(Comparator.reverseOrder());
			if (fileList.size() > 1) {
				this.splitArchive = Boolean.TRUE;
				this.numberFormattedName = Boolean.TRUE;
				this.splitCount = fileList.size();
				this.splitLength = FileUtils.fileSize(fileList.get(1));
				return new StandardFile(fileList.get(0));
			}
		}
		return new StandardFile(this.filePath);
	}

	private long headerOffset(final GeneralFileHeader generalFileHeader) throws ZipException {
		long localHeaderOffset = generalFileHeader.getOffsetLocalHeader();

		if (generalFileHeader.getZip64ExtendInfo() != null
				&& generalFileHeader.getZip64ExtendInfo().getOffsetLocalHeader() > 0L) {
			localHeaderOffset = generalFileHeader.getZip64ExtendInfo().getOffsetLocalHeader();
		}

		if (localHeaderOffset < 0) {
			throw new ZipException(0x0000001B001DL);
		}

		if (this.numberFormattedName) {
			while (localHeaderOffset > this.splitLength) {
				localHeaderOffset -= this.splitLength;
			}
		}
		return localHeaderOffset;
	}

	private LocalFileHeader readLocalFileHeader(final StandardFile input, final GeneralFileHeader generalFileHeader)
			throws ZipException {
		if (generalFileHeader == null || input == null) {
			throw new ZipException(0x0000001B0030L);
		}
		try {
			long localHeaderOffset = this.headerOffset(generalFileHeader);
			input.seek(localHeaderOffset + 26);

			byte[] tempBuffer = new byte[4];
			if (input.read(tempBuffer) == Globals.DEFAULT_VALUE_INT) {
				throw new ZipException(0x0000001B001DL);
			}

			byte[] shortBuffer = new byte[2];
			System.arraycopy(tempBuffer, 0, shortBuffer, 0, 2);
			int fileNameLength = RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN);
			System.arraycopy(tempBuffer, 2, shortBuffer, 0, 2);
			int extraFieldLength = RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN);

			input.seek(localHeaderOffset);

			int length = 0;
			LocalFileHeader localFileHeader = new LocalFileHeader();

			byte[] readBuffer = new byte[30 + fileNameLength + extraFieldLength];

			if (input.read(readBuffer) == Globals.DEFAULT_VALUE_INT) {
				throw new ZipException(0x0000001B001DL);
			}

			byte[] intBuffer = new byte[4];

			// Signature
			System.arraycopy(readBuffer, 0, intBuffer, 0, 4);
			int signature = RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN);
			if (signature != ZipGlobals.LOCSIG) {
				throw new ZipException(0x0000001B0051L, generalFileHeader.getEntryPath());
			}
			localFileHeader.setSignature(signature);
			length += 4;

			// Extract needed
			System.arraycopy(readBuffer, 4, shortBuffer, 0, 2);
			localFileHeader.setExtractNeeded(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			length += 2;

			// General purpose bit flag
			System.arraycopy(readBuffer, 6, shortBuffer, 0, 2);
			localFileHeader.setFileNameUTF8Encoded(
					(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN) & ZipGlobals.UFT8_NAMES_FLAG) != 0);
			localFileHeader.setGeneralPurposeFlag(shortBuffer.clone());
			length += 2;

			int firstByte = shortBuffer[0];

			// Check if a data descriptor exists for the local file header
			String binaryData = Integer.toBinaryString(firstByte);
			if (binaryData.length() >= 4) {
				localFileHeader.setDataDescriptorExists(binaryData.charAt(3) == '1');
			}

			// Compression method
			System.arraycopy(readBuffer, 8, shortBuffer, 0, 2);
			localFileHeader.setCompressionMethod(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			length += 2;

			// Lase modify time
			System.arraycopy(readBuffer, 10, intBuffer, 0, 4);
			localFileHeader.setLastModFileTime(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			length += 4;

			// CRC
			System.arraycopy(readBuffer, 14, intBuffer, 0, 4);
			localFileHeader.setCrc32(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			localFileHeader.setCrcBuffer(intBuffer.clone());
			length += 4;

			// Compressed size
			System.arraycopy(readBuffer, 18, intBuffer, 0, 4);
			localFileHeader.setCompressedSize(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			length += 4;

			// Original size
			System.arraycopy(readBuffer, 22, intBuffer, 0, 4);
			localFileHeader.setOriginalSize(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			length += 4;

			// File name length
			localFileHeader.setFileNameLength(fileNameLength);
			length += 2;

			// Extra field length
			localFileHeader.setExtraFieldLength(extraFieldLength);
			length += 2;

			// File name
			if (fileNameLength > 0) {
				byte[] fileNameBuffer = new byte[fileNameLength];
				System.arraycopy(readBuffer, 30, fileNameBuffer, 0, fileNameLength);

				String entryPath = new String(fileNameBuffer, this.charsetEncoding);

				if (entryPath.contains(Globals.DEFAULT_ZIP_ENTRY_SEPARATOR)) {
					entryPath = entryPath.substring(entryPath.indexOf(Globals.DEFAULT_ZIP_ENTRY_SEPARATOR)
							+ Globals.DEFAULT_ZIP_ENTRY_SEPARATOR.length());
				}

				localFileHeader.setEntryPath(entryPath);
				length += fileNameLength;
			} else {
				localFileHeader.setEntryPath(null);
			}

			// Extra field
			if (localFileHeader.getExtraFieldLength() > 0) {
				byte[] extraFieldBuffer = new byte[extraFieldLength];
				System.arraycopy(readBuffer, 30 + fileNameLength, extraFieldBuffer, 0, extraFieldLength);
				localFileHeader.setExtraDataRecords(readExtraDataRecords(extraFieldBuffer, extraFieldLength));
			}
			length += extraFieldLength;

			localFileHeader.setOffsetStartOfData(localHeaderOffset + length);

			// Copy password
			localFileHeader.setPassword(generalFileHeader.getPassword());

			readAndSaveZip64ExtendInfo(localFileHeader);
			readAndSaveAESExtraDataRecord(localFileHeader);

			if (localFileHeader.isEncrypted() && localFileHeader.getEncryptionMethod() != ZipGlobals.ENC_METHOD_AES) {
				if ((firstByte & 64) == 64) {
					localFileHeader.setEncryptionMethod(ZipGlobals.ENC_METHOD_STRONG);
				} else {
					localFileHeader.setEncryptionMethod(ZipGlobals.ENC_METHOD_STANDARD);
				}
			}

			if (localFileHeader.getCrc32() <= 0L) {
				localFileHeader.setCrc32(generalFileHeader.getCrc32());
				localFileHeader.setCrcBuffer(generalFileHeader.getCrcBuffer());
			}

			if (localFileHeader.getCompressedSize() <= 0L) {
				localFileHeader.setCompressedSize(generalFileHeader.getCompressedSize());
			}

			if (localFileHeader.getOriginalSize() <= 0L) {
				localFileHeader.setOriginalSize(generalFileHeader.getOriginalSize());
			}

			return localFileHeader;
		} catch (IOException | DataInvalidException e) {
			throw new ZipException(0x0000001B0049L, e);
		}
	}

	private void processHeaderData(final OutputStream outputStream) throws ZipException {
		try {
			int currentSplitFileCount = 0;
			if (outputStream instanceof SplitOutputStream) {
				this.endCentralDirectoryRecord
						.setOffsetOfStartOfCentralDirectory(((SplitOutputStream) outputStream).getFilePointer());
				currentSplitFileCount = ((SplitOutputStream) outputStream).getCurrentSplitFileIndex();
			}

			if (this.zip64Format) {
				this.checkZip64Format();

				this.zip64EndCentralDirectoryLocator
						.setIndexOfZip64EndOfCentralDirectoryRecord(currentSplitFileCount);
				this.zip64EndCentralDirectoryLocator.setTotalNumberOfDiscs(currentSplitFileCount + 1);
			}

			this.endCentralDirectoryRecord.setIndexOfThisDisk(currentSplitFileCount);
			this.endCentralDirectoryRecord.setIndexOfThisDiskStartOfCentralDirectory(currentSplitFileCount);
		} catch (IOException e) {
			throw new ZipException(0x0000001B0046L, e);
		}
	}

	private long writeCentralDirectory(final OutputStream outputStream) throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		if (this.centralDirectory == null || this.centralDirectory.getFileHeaders() == null
				|| this.centralDirectory.getFileHeaders().isEmpty()) {
			return Globals.INITIALIZE_INT_VALUE;
		}

		long sizeOfCentralDirectory = Globals.INITIALIZE_INT_VALUE;

		for (GeneralFileHeader generalFileHeader : this.centralDirectory.getFileHeaders()) {
			ByteArrayOutputStream headerBytes =
					new ByteArrayOutputStream(46 + generalFileHeader.getFileNameLength() + 32);
			int recordSize = writeFileHeader(generalFileHeader, outputStream, headerBytes);
			byte[] dataBytes = headerBytes.toByteArray();
			if (recordSize != dataBytes.length) {
				throw new ZipException(0x0000001B0048L);
			}
			this.writeZipHeaderBytes(outputStream, dataBytes);
			sizeOfCentralDirectory += recordSize;
		}
		return sizeOfCentralDirectory;
	}

	private int writeFileHeader(@Nonnull final GeneralFileHeader generalFileHeader, final OutputStream outputStream,
	                            @Nonnull final ByteArrayOutputStream headerBytes) throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		try {
			int sizeOfFileHeader = 0;

			byte[] shortBuffer = new byte[2];
			byte[] intBuffer = new byte[4];
			byte[] longBuffer = new byte[8];

			HeaderOperator.writeInt(generalFileHeader.getSignature(), headerBytes);
			sizeOfFileHeader += 4;

			HeaderOperator.writeShort((short) generalFileHeader.getMadeVersion(), headerBytes);
			sizeOfFileHeader += 2;

			HeaderOperator.writeShort((short) generalFileHeader.getExtractNeeded(), headerBytes);
			sizeOfFileHeader += 2;

			HeaderOperator.writeBytes(generalFileHeader.getGeneralPurposeFlag(), headerBytes);
			sizeOfFileHeader += 2;

			HeaderOperator.writeShort((short) generalFileHeader.getCompressionMethod(), headerBytes);
			sizeOfFileHeader += 2;

			HeaderOperator.writeInt(generalFileHeader.getLastModFileTime(), headerBytes);
			sizeOfFileHeader += 4;

			HeaderOperator.writeInt((int) (generalFileHeader.getCrc32()), headerBytes);
			sizeOfFileHeader += 4;

			boolean writeZip64FileSize = generalFileHeader.isZip64Entry()
					|| generalFileHeader.getOriginalSize() >= ZipGlobals.ZIP_64_LIMIT
					|| generalFileHeader.getCompressedSize() >= ZipGlobals.ZIP_64_LIMIT;
			boolean writeZip64OffsetLocalHeader = generalFileHeader.getOffsetLocalHeader() >= ZipGlobals.ZIP_64_LIMIT;
			boolean writeZip64DiskStart = generalFileHeader.getDiskNumberStart() >= ZipGlobals.ZIP_16_LIMIT;

			if (writeZip64FileSize || writeZip64OffsetLocalHeader || writeZip64DiskStart) {
				generalFileHeader.setMadeVersion(Math.max(generalFileHeader.getMadeVersion(), ZipGlobals.ZIP_64_VERSION));
				generalFileHeader.setExtractNeeded(Math.max(generalFileHeader.getExtractNeeded(), ZipGlobals.ZIP_64_VERSION));
			}

			if (writeZip64FileSize) {
				HeaderOperator.writeInt((int) ZipGlobals.ZIP_64_LIMIT, headerBytes);
				HeaderOperator.writeInt((int) ZipGlobals.ZIP_64_LIMIT, headerBytes);
			} else {
				HeaderOperator.writeInt((int) generalFileHeader.getCompressedSize(), headerBytes);
				HeaderOperator.writeInt((int) generalFileHeader.getOriginalSize(), headerBytes);
			}
			sizeOfFileHeader += Integer.BYTES * 2;

			HeaderOperator.writeShort((short) generalFileHeader.getFileNameLength(), headerBytes);
			sizeOfFileHeader += 2;

			// Compute offset bytes before an extra field is written for Zip64
			// compatibility
			// NOTE: this data is not written now, but written at a later point
			byte[] offsetLocalHeaderBytes = new byte[4];
			if (writeZip64OffsetLocalHeader) {
				RawUtils.writeLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN, ZipGlobals.ZIP_64_LIMIT);
				System.arraycopy(longBuffer, 0, offsetLocalHeaderBytes, 0, 4);
			} else {
				RawUtils.writeLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN, generalFileHeader.getOffsetLocalHeader());
				System.arraycopy(longBuffer, 0, offsetLocalHeaderBytes, 0, 4);
			}

			// extra field length
			int extraFieldLength = 0;
			if (writeZip64FileSize || writeZip64OffsetLocalHeader || writeZip64DiskStart) {
				extraFieldLength += 4;
				if (writeZip64FileSize) {
					extraFieldLength += 16;
				}
				if (writeZip64OffsetLocalHeader) {
					extraFieldLength += 8;
				}
				if (writeZip64DiskStart) {
					extraFieldLength += 4;
				}
			}
			if (generalFileHeader.getAesExtraDataRecord() != null) {
				extraFieldLength += 11;
			}
			HeaderOperator.writeShort((short) extraFieldLength, headerBytes);
			sizeOfFileHeader += 2;

			// Skip file comment length for now
			HeaderOperator.writeBytes(EMPTY_SHORT_BUFFER, headerBytes);
			sizeOfFileHeader += 2;

			// Skip disk number start for now
			short diskNumberStart =
					writeZip64DiskStart ? (short) ZipGlobals.ZIP_16_LIMIT : (short) generalFileHeader.getDiskNumberStart();
			HeaderOperator.writeShort(diskNumberStart, headerBytes);
			sizeOfFileHeader += 2;

			// Skip internal file attributes for now
			HeaderOperator.writeBytes(EMPTY_SHORT_BUFFER, headerBytes);
			sizeOfFileHeader += 2;

			// External file attributes
			if (generalFileHeader.getExternalFileAttr() != null) {
				HeaderOperator.writeBytes(generalFileHeader.getExternalFileAttr(), headerBytes);
			} else {
				HeaderOperator.writeBytes(EMPTY_INT_BUFFER, headerBytes);
			}
			sizeOfFileHeader += 4;

			// offset local header this data is computed above
			HeaderOperator.writeBytes(offsetLocalHeaderBytes, headerBytes);
			sizeOfFileHeader += 4;

			byte[] fileNameBytes = generalFileHeader.getEntryPath().getBytes(this.charsetEncoding);
			HeaderOperator.writeBytes(fileNameBytes, headerBytes);
			sizeOfFileHeader += fileNameBytes.length;

			if (writeZip64FileSize || writeZip64OffsetLocalHeader || writeZip64DiskStart) {
				this.zip64Format = true;

				// Zip64 header
				HeaderOperator.writeShort((short) ZipGlobals.EXTRAFIELDZIP64LENGTH, headerBytes);
				sizeOfFileHeader += 2;

				// Zip64 extra data record size
				int dataSize = 0;

				if (writeZip64FileSize) {
					dataSize += Long.BYTES * 2;
				}
				if (writeZip64OffsetLocalHeader) {
					dataSize += Long.BYTES;
				}

				if (writeZip64DiskStart) {
					dataSize += Integer.BYTES;
				}

				HeaderOperator.writeShort((short) dataSize, headerBytes);
				sizeOfFileHeader += 2;

				if (writeZip64FileSize) {
					HeaderOperator.writeLong(generalFileHeader.getOriginalSize(), headerBytes);
					sizeOfFileHeader += 8;

					HeaderOperator.writeLong(generalFileHeader.getCompressedSize(), headerBytes);
					sizeOfFileHeader += 8;
				}

				if (writeZip64OffsetLocalHeader) {
					HeaderOperator.writeLong(generalFileHeader.getOffsetLocalHeader(), headerBytes);
					sizeOfFileHeader += 8;
				}

				if (writeZip64DiskStart) {
					if (generalFileHeader.getDiskNumberStart() < 0L
							|| generalFileHeader.getDiskNumberStart() > ZipGlobals.ZIP_64_LIMIT) {
						throw new ZipException(0x0000001B0059L);
					}
					HeaderOperator.writeInt((int) generalFileHeader.getDiskNumberStart(), headerBytes);
					sizeOfFileHeader += 4;
				}
			}

			if (generalFileHeader.getAesExtraDataRecord() != null) {
				AESEngine.processHeader(generalFileHeader.getAesExtraDataRecord(), headerBytes);
				sizeOfFileHeader += 11;
			}

			return sizeOfFileHeader;
		} catch (Exception e) {
			throw new ZipException(0x0000001B0047L, e);
		}
	}

	private void writeZip64EndOfCentralDirectoryRecord(final OutputStream outputStream, final long sizeOfCentralDirectory,
	                                                   final long offsetCentralDirectory, final ByteArrayOutputStream headerBytes)
			throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		try {
			byte[] intBuffer = new byte[4];

			final byte[] EMPTY_SHORT_BUFFER = {0, 0};

			// zip64 end of central dir signature
			HeaderOperator.writeInt((int) ZipGlobals.ZIP64ENDCENDIRREC, headerBytes);

			// size zip64 end of central directory record
			HeaderOperator.writeLong(44L, headerBytes);

			// version made by
			HeaderOperator.writeShort((short) ZipGlobals.ZIP_64_VERSION, headerBytes);
			// version needed to extract
			HeaderOperator.writeShort((short) ZipGlobals.ZIP_64_VERSION, headerBytes);

			// number of these disks
			HeaderOperator.writeInt(this.endCentralDirectoryRecord.getIndexOfThisDisk(), headerBytes);

			// number of the disks with start of central directory
			HeaderOperator.writeInt(this.endCentralDirectoryRecord.getIndexOfThisDiskStartOfCentralDirectory(), headerBytes);

			// total number of entries in the central directory on this disk
			int numEntries;
			int numEntriesOnThisDisk;
			if (this.centralDirectory == null || this.centralDirectory.getFileHeaders() == null) {
				throw new ZipException(0x0000001B0028L);
			} else {
				numEntries = this.centralDirectory.getFileHeaders().size();
				if (this.splitArchive) {
					numEntriesOnThisDisk = countNumberOfFileHeaderEntriesOnDisk(this.centralDirectory.getFileHeaders(),
							this.endCentralDirectoryRecord.getIndexOfThisDisk());
				} else {
					numEntriesOnThisDisk = numEntries;
				}
			}

			HeaderOperator.writeLong(numEntriesOnThisDisk, headerBytes);

			// Total number of entries in the central directory
			HeaderOperator.writeLong(numEntries, headerBytes);

			// Size of the central directory
			HeaderOperator.writeLong(sizeOfCentralDirectory, headerBytes);

			// offset the start of the central directory with respect to the starting disk number
			HeaderOperator.writeLong(offsetCentralDirectory, headerBytes);
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0048L, e);
			}
		}
	}

	private void writeZip64EndOfCentralDirectoryLocator(final OutputStream outputStream,
	                                                    final ByteArrayOutputStream headerBytes) throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		try {
			byte[] intBuffer = new byte[4];
			byte[] longBuffer = new byte[8];

			long zip64EndRecordDisk = this.zip64EndCentralDirectoryLocator.getIndexOfZip64EndOfCentralDirectoryRecord();
			long totalNumberOfDiscs = this.zip64EndCentralDirectoryLocator.getTotalNumberOfDiscs();
			if (zip64EndRecordDisk < 0L || zip64EndRecordDisk > ZipGlobals.ZIP_64_LIMIT
					|| totalNumberOfDiscs <= 0L || totalNumberOfDiscs > ZipGlobals.ZIP_64_LIMIT) {
				throw new ZipException(0x0000001B0059L);
			}

			// zip64 end of central dir locator signature
			HeaderOperator.writeInt((int) ZipGlobals.ZIP64ENDCENDIRLOC, headerBytes);

			// number of the disks with the zip64 end of central directory
			HeaderOperator.writeInt((int) zip64EndRecordDisk, headerBytes);

			// relative offset of the zip64 end of the central directory record
			HeaderOperator.writeLong(this.zip64EndCentralDirectoryLocator.getOffsetZip64EndOfCentralDirectoryRecord(), headerBytes);

			// total number of disks
			HeaderOperator.writeInt((int) totalNumberOfDiscs, headerBytes);
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0045L, e);
			}
		}
	}

	private void writeEndOfCentralDirectoryRecord(final long sizeOfCentralDirectory, final long offsetCentralDirectory,
	                                              final ByteArrayOutputStream headerBytes) throws ZipException {
		try {
			byte[] shortBuffer = new byte[2];
			byte[] intBuffer = new byte[4];
			byte[] longBuffer = new byte[8];

			// End of central directory signature
			HeaderOperator.writeInt((int) this.endCentralDirectoryRecord.getSignature(), headerBytes);

			// number of these disks
			HeaderOperator.writeShort((short) this.endCentralDirectoryRecord.getIndexOfThisDisk(), headerBytes);

			// number of the disks with start of central directory
			HeaderOperator.writeShort((short) this.endCentralDirectoryRecord.getIndexOfThisDiskStartOfCentralDirectory(), headerBytes);

			// Total number of entries in the central directory on this disk
			int numEntries;
			int numEntriesOnThisDisk;
			if (this.centralDirectory == null || this.centralDirectory.getFileHeaders() == null) {
				throw new ZipException(0x0000001B0028L);
			} else {
				numEntries = this.centralDirectory.getFileHeaders().size();
				if (this.splitArchive) {
					numEntriesOnThisDisk = countNumberOfFileHeaderEntriesOnDisk(
							this.centralDirectory.getFileHeaders(),
							this.endCentralDirectoryRecord.getIndexOfThisDisk());
				} else {
					numEntriesOnThisDisk = numEntries;
				}
			}
			HeaderOperator.writeShort((short) Math.min(numEntriesOnThisDisk, ZipGlobals.ZIP_16_LIMIT), headerBytes);

			// Total number of entries in the central directory
			HeaderOperator.writeShort((short) Math.min(numEntries, ZipGlobals.ZIP_16_LIMIT), headerBytes);

			// Size of the central directory
			HeaderOperator.writeInt((int) Math.min(sizeOfCentralDirectory, ZipGlobals.ZIP_64_LIMIT), headerBytes);

			// Offset central directory
			HeaderOperator.writeInt((int) Math.min(offsetCentralDirectory, ZipGlobals.ZIP_64_LIMIT), headerBytes);

			// Zip File comment length
			int commentLength = 0;
			if (this.endCentralDirectoryRecord.getCommentBytes() != null) {
				commentLength = this.endCentralDirectoryRecord.getCommentLength();
			}
			HeaderOperator.writeShort((short) commentLength, headerBytes);

			// Comment
			if (commentLength > 0) {
				HeaderOperator.writeBytes(this.endCentralDirectoryRecord.getCommentBytes(), headerBytes);
			}
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0046L, e);
			}
		}
	}

	private void writeZipHeaderBytes(final OutputStream outputStream, final byte[] buffer) throws ZipException {
		if (buffer == null) {
			throw new ZipException(0x0000001B0047L);
		}

		try {
			if (outputStream instanceof SplitOutputStream) {
				((SplitOutputStream) outputStream).checkBufferSizeAndStartNextSplitFile(buffer.length);
			}
			outputStream.write(buffer);
		} catch (IOException e) {
			throw new ZipException(0x0000001B0047L, e);
		}
	}

	private void readEndOfCentralDirectoryRecord(final StandardFile input) throws ZipException {
		if (input == null) {
			throw new ZipException(0x0000001B0013L);
		}

		try {
			byte[] buffer = new byte[4];
			long position;
			try {
				position = input.length();
			} catch (IOException e) {
				position = Globals.DEFAULT_VALUE_LONG;
			}

			if (position == Globals.DEFAULT_VALUE_LONG) {
				throw new ZipException(0x0000001B0042L);
			}
			position -= ZipGlobals.ENDHDR;

			this.endCentralDirectoryRecord = new EndCentralDirectoryRecord();

			int count = 0;
			do {
				input.seek(position--);
				count++;
			} while ((readIntFromDataInput(input, buffer) != ZipGlobals.ENDSIG)
					&& count <= 3000);

			int endsig = RawUtils.readInt(buffer, 0, ByteOrder.LITTLE_ENDIAN);
			if (endsig != ZipGlobals.ENDSIG) {
				throw new ZipException(0x0000001B0052L);
			}

			byte[] readBuffer = new byte[18];

			if (input.read(readBuffer) == Globals.DEFAULT_VALUE_INT) {
				return;
			}

			byte[] intBuffer = new byte[4];
			byte[] shortBuffer = new byte[2];

			this.endCentralDirectoryRecord.setSignature(ZipGlobals.ENDSIG);

			System.arraycopy(readBuffer, 0, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord.setIndexOfThisDisk(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 2, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord
					.setIndexOfThisDiskStartOfCentralDirectory(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 4, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord.setTotalOfEntriesInCentralDirectoryOnThisDisk(
					RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 6, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord
					.setTotalOfEntriesInCentralDirectory(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 8, intBuffer, 0, 4);
			this.endCentralDirectoryRecord.setSizeOfCentralDirectory(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 12, intBuffer, 0, 4);
			this.endCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(
					RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 16, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord.setCommentLength(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			if (this.endCentralDirectoryRecord.getCommentLength() > 0) {
				byte[] commentBuffer = new byte[this.endCentralDirectoryRecord.getCommentLength()];
				input.read(commentBuffer);
				this.endCentralDirectoryRecord.setCommentBytes(commentBuffer);
			}

			this.splitArchive |= (this.endCentralDirectoryRecord.getIndexOfThisDisk() > 0);
		} catch (IOException | DataInvalidException e) {
			throw new ZipException(0x0000001B0042L, e);
		}
	}

	private void readZip64EndCentralDirectoryLocator(final StandardFile input) throws ZipException {
		try {
			this.zip64EndCentralDirectoryLocator = new Zip64EndCentralDirectoryLocator();
			byte[] buffer = new byte[4];
			long position;
			try {
				position = input.length();
			} catch (Exception e) {
				position = Globals.DEFAULT_VALUE_LONG;
			}

			if (position == Globals.DEFAULT_VALUE_LONG) {
				throw new ZipException(0x0000001B0042L);
			}
			position -= ZipGlobals.ENDHDR;

			do {
				input.seek(position--);
			} while (readIntFromDataInput(input, buffer) != ZipGlobals.ENDSIG);

			// Now the file pointer is at the end of signature of Central Dir
			// Rec
			// Seek back with the following values
			// 4 -> total number of disks
			// 8 -> relative offset of the zip64 end of central directory record
			// 4 -> number of the disk with the start of the zip64 end of
			// central directory
			// 4 -> zip64 end of central dir locator signature
			// Refer to Appose for more information
			input.seek(position - 4 - 8 - 4 - 4);

			byte[] readBuffer = new byte[20];

			if (input.read(readBuffer) == Globals.DEFAULT_VALUE_INT) {
				return;
			}

			byte[] intBuffer = new byte[4];
			byte[] longBuffer = new byte[8];

			int signature = this.readSignature(readBuffer);
			if (signature == ZipGlobals.ZIP64ENDCENDIRLOC) {
				this.zip64Format = true;
				this.zip64EndCentralDirectoryLocator.setSignature(signature);
			} else {
				this.zip64Format = false;
				return;
			}

			System.arraycopy(readBuffer, 4, intBuffer, 0, 4);
			this.zip64EndCentralDirectoryLocator
					.setIndexOfZip64EndOfCentralDirectoryRecord(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 8, longBuffer, 0, 8);
			this.zip64EndCentralDirectoryLocator
					.setOffsetZip64EndOfCentralDirectoryRecord(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 16, intBuffer, 0, 4);
			this.zip64EndCentralDirectoryLocator.setTotalNumberOfDiscs(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
		} catch (Exception e) {
			throw new ZipException(0x0000001B0041L, e);
		}
	}

	private int readSignature(final byte[] dataBytes) {
		byte[] intBuffer = new byte[4];
		System.arraycopy(dataBytes, 0, intBuffer, 0, 4);
		try {
			return RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN);
		} catch (DataInvalidException ignore) {
			return Globals.DEFAULT_VALUE_INT;
		}
	}

	private void readCentralDirectory(final StandardFile input) throws ZipException {
		if (this.endCentralDirectoryRecord == null) {
			throw new ZipException(0x0000001B0029L);
		}

		try {
			List<GeneralFileHeader> fileHeaderList = new ArrayList<>();

			long offsetOfStartOfCentralDirectory = this.endCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory();
			int centralDirectoryEntryCount = this.endCentralDirectoryRecord.getTotalOfEntriesInCentralDirectory();

			if (this.zip64Format) {
				offsetOfStartOfCentralDirectory = this.zip64EndCentralDirectoryRecord.getOffsetStartCenDirWRTStartDiskNo();
				long entryCount = this.zip64EndCentralDirectoryRecord.getTotalEntriesInCentralDirectory();
				if (entryCount < 0L || entryCount > Integer.MAX_VALUE) {
					throw new ZipException(0x0000001B005AL);
				}
				centralDirectoryEntryCount = (int) entryCount;
			}

			if (this.splitArchive && this.numberFormattedName) {
				offsetOfStartOfCentralDirectory -= ((this.splitCount - 1) * this.splitLength);
			}

			long inputLength = input.length();
			if (offsetOfStartOfCentralDirectory < 0L || offsetOfStartOfCentralDirectory > inputLength) {
				throw new ZipException(0x0000001B003EL);
			}

			long bufferSize = inputLength - offsetOfStartOfCentralDirectory;
			if (bufferSize > Integer.MAX_VALUE) {
				throw new ZipException(0x0000001B003EL);
			}

			input.seek(offsetOfStartOfCentralDirectory);

			byte[] readBuffer = new byte[(int) bufferSize];
			input.readFully(readBuffer);

			byte[] shortBuffer = new byte[2];
			byte[] intBuffer = new byte[4];

			int pos = 0;
			for (int i = 0; i < centralDirectoryEntryCount; i++) {
				GeneralFileHeader fileHeader = new GeneralFileHeader();

				System.arraycopy(readBuffer, pos, intBuffer, 0, 4);
				int signature = RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN);
				if (signature != ZipGlobals.CENSIG) {
					throw new ZipException(0x0000001B0040, i);
				}

				fileHeader.setSignature(signature);

				// Made versions
				System.arraycopy(readBuffer, pos + 4, shortBuffer, 0, 2);
				fileHeader.setMadeVersion(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Extract needed
				System.arraycopy(readBuffer, pos + 6, shortBuffer, 0, 2);
				fileHeader.setExtractNeeded(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Purpose bit flag
				System.arraycopy(readBuffer, pos + 8, shortBuffer, 0, 2);
				fileHeader.setFileNameUTF8Encoded(
						(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN) & ZipGlobals.UFT8_NAMES_FLAG) != 0);
				int firstByte = shortBuffer[0];
				fileHeader.setGeneralPurposeFlag(shortBuffer.clone());
				fileHeader.setDataDescriptorExists((firstByte >> 3) == 1);

				// Compression method
				System.arraycopy(readBuffer, pos + 10, shortBuffer, 0, 2);
				fileHeader.setCompressionMethod(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Last modify file time
				System.arraycopy(readBuffer, pos + 12, intBuffer, 0, 4);
				fileHeader.setLastModFileTime(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Crc32
				System.arraycopy(readBuffer, pos + 16, intBuffer, 0, 4);
				fileHeader.setCrc32(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
				fileHeader.setCrcBuffer(intBuffer.clone());

				// Compressed size
				System.arraycopy(readBuffer, pos + 20, intBuffer, 0, 4);
				fileHeader.setCompressedSize(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Original size
				System.arraycopy(readBuffer, pos + 24, intBuffer, 0, 4);
				fileHeader.setOriginalSize(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// File name length
				System.arraycopy(readBuffer, pos + 28, shortBuffer, 0, 2);
				fileHeader.setFileNameLength(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Extra field length
				System.arraycopy(readBuffer, pos + 30, shortBuffer, 0, 2);
				fileHeader.setExtraFieldLength(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Comment length
				System.arraycopy(readBuffer, pos + 32, shortBuffer, 0, 2);
				fileHeader.setFileCommentLength(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Disk number of start
				System.arraycopy(readBuffer, pos + 34, shortBuffer, 0, 2);
				fileHeader.setDiskNumberStart(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Internal file attributes
				System.arraycopy(readBuffer, pos + 36, shortBuffer, 0, 2);
				fileHeader.setInternalFileAttr(shortBuffer.clone());

				// External file attributes
				System.arraycopy(readBuffer, pos + 38, intBuffer, 0, 4);
				fileHeader.setExternalFileAttr(intBuffer.clone());

				// Relative offset of local header
				System.arraycopy(readBuffer, pos + 42, intBuffer, 0, 4);
				fileHeader.setOffsetLocalHeader(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				if (fileHeader.getFileNameLength() > 0) {
					byte[] fileNameBuffer = new byte[fileHeader.getFileNameLength()];
					System.arraycopy(readBuffer, pos + 46, fileNameBuffer, 0, fileHeader.getFileNameLength());

					String entryPath = new String(fileNameBuffer, this.charsetEncoding);

					if (entryPath.contains(Globals.DEFAULT_ZIP_ENTRY_SEPARATOR)) {
						entryPath = entryPath.substring(entryPath.indexOf(Globals.DEFAULT_ZIP_ENTRY_SEPARATOR)
								+ Globals.DEFAULT_ZIP_ENTRY_SEPARATOR.length());
					}

					fileHeader.setEntryPath(entryPath);
					if (entryPath.endsWith(Globals.DEFAULT_ZIP_PAGE_SEPARATOR)
							|| entryPath.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
						fileHeader.setDirectory(true);
					} else {
						fileHeader.setDirectory(Boolean.FALSE);
					}
				} else {
					fileHeader.setEntryPath(null);
				}

				// Extra field
				if (fileHeader.getExtraFieldLength() > 0) {
					byte[] extraFieldBuffer = new byte[fileHeader.getExtraFieldLength()];
					System.arraycopy(readBuffer, pos + 46 + fileHeader.getFileNameLength(), extraFieldBuffer, 0, fileHeader.getExtraFieldLength());
					fileHeader.setExtraDataRecords(readExtraDataRecords(extraFieldBuffer, fileHeader.getExtraFieldLength()));
				}

				// Read zip64 extra data record if exists
				readAndSaveZip64ExtendInfo(fileHeader);

				// Read AES Extra data record if exists
				readAndSaveAESExtraDataRecord(fileHeader);

				if (fileHeader.getFileCommentLength() > 0) {
					byte[] commentBuffer = new byte[fileHeader.getFileCommentLength()];
					System.arraycopy(readBuffer,
							pos + 46 + fileHeader.getFileNameLength() + fileHeader.getExtraFieldLength(),
							commentBuffer, 0, fileHeader.getFileCommentLength());
					fileHeader.setFileComment(new String(commentBuffer, this.charsetEncoding));
				}
				fileHeaderList.add(fileHeader);
				pos += (46 + fileHeader.getFileNameLength() + fileHeader.getExtraFieldLength() + fileHeader.getFileCommentLength());
			}

			this.centralDirectory = new CentralDirectory();

			this.centralDirectory.setFileHeaders(fileHeaderList);

			System.arraycopy(readBuffer, pos, intBuffer, 0, 4);
			int signature = RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN);
			if (signature == ZipGlobals.DIGSIG) {
				DigitalSignature digitalSignature = new DigitalSignature();

				digitalSignature.setSignature(signature);

				System.arraycopy(readBuffer, pos + 4, shortBuffer, 0, 2);
				digitalSignature.setDataSize(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				if (digitalSignature.getDataSize() > 0) {
					byte[] signatureDataBuffer = new byte[digitalSignature.getDataSize()];
					System.arraycopy(readBuffer, pos + 6, signatureDataBuffer,
							0, digitalSignature.getDataSize());
					digitalSignature.setSignatureData(new String(signatureDataBuffer, this.charsetEncoding));
				}

				this.centralDirectory.setDigitalSignature(digitalSignature);
			}
		} catch (IOException | DataInvalidException e) {
			throw new ZipException(0x0000001B003EL, e);
		}
	}

	private void readZip64EndCentralDirectoryRecord(final StandardFile input) throws ZipException {
		if (this.zip64EndCentralDirectoryLocator == null) {
			throw new ZipException(0x0000001B003CL);
		}

		try {
			long offsetZip64EndOfCentralDirectoryRecord =
					this.zip64EndCentralDirectoryLocator.getOffsetZip64EndOfCentralDirectoryRecord();

			if (offsetZip64EndOfCentralDirectoryRecord < 0L) {
				throw new ZipException(0x0000001B003CL);
			}

			input.seek(offsetZip64EndOfCentralDirectoryRecord);

			this.zip64EndCentralDirectoryRecord = new Zip64EndCentralDirectoryRecord();

			byte[] readBuffer = new byte[56];
			if (input.read(readBuffer) == Globals.DEFAULT_VALUE_INT) {
				return;
			}

			byte[] shortBuffer = new byte[2];
			byte[] intBuffer = new byte[4];
			byte[] longBuffer = new byte[8];

			int signature = this.readSignature(readBuffer);
			if (signature != ZipGlobals.ZIP64ENDCENDIRREC) {
				throw new ZipException(0x0000001B003DL);
			}
			this.zip64EndCentralDirectoryRecord.setSignature(signature);

			// Read the size of the zip64 end of the central directory record
			System.arraycopy(readBuffer, 4, longBuffer, 0, 8);
			this.zip64EndCentralDirectoryRecord.setRecordSize(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Made versions
			System.arraycopy(readBuffer, 12, shortBuffer, 0, 2);
			this.zip64EndCentralDirectoryRecord.setMadeVersion(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Extract needed
			System.arraycopy(readBuffer, 14, shortBuffer, 0, 2);
			this.zip64EndCentralDirectoryRecord.setExtractNeeded(RawUtils.readUnsignedShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Number of these disks
			System.arraycopy(readBuffer, 16, intBuffer, 0, 4);
			this.zip64EndCentralDirectoryRecord.setIndex(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Start of the central directory
			System.arraycopy(readBuffer, 20, intBuffer, 0, 4);
			this.zip64EndCentralDirectoryRecord
					.setStartOfCentralDirectory(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Total of entries in the central directory on this disk
			System.arraycopy(readBuffer, 24, longBuffer, 0, 8);
			this.zip64EndCentralDirectoryRecord
					.setTotalEntriesInCentralDirectoryOnThisDisk(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Total of entries in the central directory
			System.arraycopy(readBuffer, 32, longBuffer, 0, 8);
			this.zip64EndCentralDirectoryRecord
					.setTotalEntriesInCentralDirectory(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Size of the central directory
			System.arraycopy(readBuffer, 40, longBuffer, 0, 8);
			this.zip64EndCentralDirectoryRecord
					.setSizeOfCentralDirectory(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Offset start of the central directory with respect to the starting
			// disk number
			System.arraycopy(readBuffer, 48, longBuffer, 0, 8);
			this.zip64EndCentralDirectoryRecord
					.setOffsetStartCenDirWRTStartDiskNo(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Zip64 extensible data sector
			long extDataSize = zip64EndCentralDirectoryRecord.getRecordSize() - 44L;
			if (extDataSize > 0) {
				byte[] extensibleDataSector = new byte[(int) extDataSize];
				if (input.read(extensibleDataSector) > 0) {
					this.zip64EndCentralDirectoryRecord.setExtensibleDataSector(extensibleDataSector);
				}
			}

			this.splitArchive |= (this.zip64EndCentralDirectoryRecord.getIndex() > 0);
		} catch (IOException | DataInvalidException e) {
			throw new ZipException(0x0000001B003CL, e);
		}
	}

	private static void readAndSaveAESExtraDataRecord(final FileHeader fileHeader) throws ZipException {
		if (fileHeader == null) {
			throw new ZipException(0x0000001B0030L);
		}

		if (fileHeader.getExtraDataRecords() != null && !fileHeader.getExtraDataRecords().isEmpty()) {
			for (ExtraDataRecord extraDataRecord : fileHeader.getExtraDataRecords()) {
				if (extraDataRecord != null) {
					if (extraDataRecord.getHeader() == ZipGlobals.AESSIG) {
						if (extraDataRecord.getDataContent() == null) {
							throw new ZipException(0x0000001B0010L);
						}

						AESExtraDataRecord aesExtraDataRecord = new AESExtraDataRecord();

						aesExtraDataRecord.setSignature(ZipGlobals.AESSIG);
						aesExtraDataRecord.setDataSize(extraDataRecord.getDataSize());

						try {
							byte[] aesData = extraDataRecord.getDataContent();
							aesExtraDataRecord.setVersionNumber(RawUtils.readUnsignedShort(aesData, 0, ByteOrder.LITTLE_ENDIAN));

							byte[] vendorIDBuffer = new byte[2];
							System.arraycopy(aesData, 2, vendorIDBuffer, 0, 2);
							aesExtraDataRecord.setVendorID(new String(vendorIDBuffer, StandardCharsets.UTF_8));
							aesExtraDataRecord.setAesStrength((aesData[4] & 0xFF));
							aesExtraDataRecord.setCompressionMethod(RawUtils.readUnsignedShort(aesData, 5, ByteOrder.LITTLE_ENDIAN));
						} catch (DataInvalidException e) {
							throw new ZipException(0x0000001B003BL, e);
						}
						fileHeader.setAesExtraDataRecord(aesExtraDataRecord);
						fileHeader.setEncryptionMethod(ZipGlobals.ENC_METHOD_AES);
						break;
					}
				}
			}
		}
	}

	private static Zip64ExtendInfo readZip64ExtendInfo(final List<ExtraDataRecord> extraDataRecords, final long originalSize,
	                                                   final long compressedSize, final long offsetLocalHeader,
	                                                   final long diskNumberStart) throws ZipException {
		for (ExtraDataRecord extraDataRecord : extraDataRecords) {
			if (extraDataRecord.getHeader() == 0x0001) {
				if (extraDataRecord.getDataSize() <= 0) {
					break;
				}

				byte[] intBuffer = new byte[4];
				byte[] longBuffer = new byte[8];
				int count = 0;
				boolean addValue = Boolean.FALSE;

				Zip64ExtendInfo zip64ExtendInfo = new Zip64ExtendInfo();

				try {
					if (originalSize == ZipGlobals.ZIP_64_LIMIT && count + Long.BYTES <= extraDataRecord.getDataSize()) {
						System.arraycopy(extraDataRecord.getDataContent(), count, longBuffer, 0, 8);
						zip64ExtendInfo.setOriginalSize(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));
						count += Long.BYTES;
						addValue = Boolean.TRUE;
					}

					if ((compressedSize == ZipGlobals.ZIP_64_LIMIT) && count + Long.BYTES <= extraDataRecord.getDataSize()) {
						System.arraycopy(extraDataRecord.getDataContent(), count, longBuffer, 0, 8);
						zip64ExtendInfo.setCompressedSize(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));
						count += Long.BYTES;
						addValue = Boolean.TRUE;
					}

					if ((offsetLocalHeader == ZipGlobals.ZIP_64_LIMIT) && count + Long.BYTES <= extraDataRecord.getDataSize()) {
						System.arraycopy(extraDataRecord.getDataContent(), count, longBuffer, 0, 8);
						zip64ExtendInfo.setOffsetLocalHeader(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));
						count += Long.BYTES;
						addValue = Boolean.TRUE;
					}

					if (diskNumberStart == ZipGlobals.ZIP_16_LIMIT && count + Integer.BYTES <= extraDataRecord.getDataSize()) {
						System.arraycopy(extraDataRecord.getDataContent(), count, intBuffer, 0, 4);
						zip64ExtendInfo.setDiskNumberStart(RawUtils.readUnsignedInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
						addValue = Boolean.TRUE;
					}
				} catch (DataInvalidException e) {
					throw new ZipException(0x0000001B003AL, e);
				}

				if (addValue) {
					return zip64ExtendInfo;
				}

				break;
			}
		}

		return null;
	}

	private static void readAndSaveZip64ExtendInfo(final FileHeader fileHeader) throws ZipException {
		if (fileHeader == null) {
			throw new ZipException(0x0000001B002EL);
		}

		Zip64ExtendInfo zip64ExtendInfo = null;
		if (fileHeader instanceof GeneralFileHeader) {
			if (fileHeader.getExtraDataRecords() != null && !fileHeader.getExtraDataRecords().isEmpty()) {
				zip64ExtendInfo = readZip64ExtendInfo(fileHeader.getExtraDataRecords(),
						fileHeader.getOriginalSize(), fileHeader.getCompressedSize(),
						((GeneralFileHeader) fileHeader).getOffsetLocalHeader(),
						((GeneralFileHeader) fileHeader).getDiskNumberStart());
				if (zip64ExtendInfo != null) {
					if (zip64ExtendInfo.getOffsetLocalHeader() != -1) {
						((GeneralFileHeader) fileHeader).setOffsetLocalHeader(zip64ExtendInfo.getOffsetLocalHeader());
					}

					if (zip64ExtendInfo.getDiskNumberStart() != -1) {
						((GeneralFileHeader) fileHeader).setDiskNumberStart(zip64ExtendInfo.getDiskNumberStart());
					}
				}
			}
		} else if (fileHeader instanceof LocalFileHeader) {
			if (fileHeader.getExtraDataRecords() == null || fileHeader.getExtraDataRecords().isEmpty()) {
				return;
			}

			zip64ExtendInfo = readZip64ExtendInfo(fileHeader.getExtraDataRecords(),
					fileHeader.getOriginalSize(), fileHeader.getCompressedSize(), Globals.DEFAULT_VALUE_LONG,
					Globals.DEFAULT_VALUE_INT);
		} else {
			throw new ZipException(0x0000001B002EL);
		}

		if (zip64ExtendInfo != null) {
			fileHeader.setZip64ExtendInfo(zip64ExtendInfo);
			if (zip64ExtendInfo.getOriginalSize() != -1) {
				fileHeader.setOriginalSize(zip64ExtendInfo.getOriginalSize());
			}

			if (zip64ExtendInfo.getCompressedSize() != -1) {
				fileHeader.setCompressedSize(zip64ExtendInfo.getCompressedSize());
			}

		}
	}

	private static List<ExtraDataRecord> readExtraDataRecords(final byte[] extraFieldBuffer, final int extraFieldLength)
			throws ZipException {
		int count = 0;
		List<ExtraDataRecord> extraDataRecords = new ArrayList<>();

		while (count < extraFieldLength) {
			ExtraDataRecord extraDataRecord = new ExtraDataRecord();
			try {
				extraDataRecord.setHeader(RawUtils.readUnsignedShort(extraFieldBuffer, count, ByteOrder.LITTLE_ENDIAN));

				count += 2;

				int dataSize = RawUtils.readUnsignedShort(extraFieldBuffer, count, ByteOrder.LITTLE_ENDIAN);

				if ((dataSize + 2) > extraFieldLength) {
					dataSize = RawUtils.readUnsignedShort(extraFieldBuffer, count, ByteOrder.BIG_ENDIAN);
					if ((dataSize + 2) > extraFieldLength) {
						break;
					}
				}
				extraDataRecord.setDataSize(dataSize);
				count += 2;

				if (dataSize > 0) {
					byte[] dataContent = new byte[dataSize];
					System.arraycopy(extraFieldBuffer, count, dataContent, 0, dataSize);
					extraDataRecord.setDataContent(dataContent);
				}

				count += dataSize;
			} catch (DataInvalidException e) {
				throw new ZipException(0x0000001B0039L, e);
			}
			extraDataRecords.add(extraDataRecord);
		}

		if (!extraDataRecords.isEmpty()) {
			return extraDataRecords;
		}

		return null;
	}

	private static int countNumberOfFileHeaderEntriesOnDisk(final List<GeneralFileHeader> fileHeaders, final int numOfDisk)
			throws ZipException {
		if (fileHeaders == null) {
			throw new ZipException(0x0000001B002EL);
		}

		int noEntries = 0;
		for (GeneralFileHeader generalFileHeader : fileHeaders) {
			if (generalFileHeader.getDiskNumberStart() == numOfDisk) {
				noEntries++;
			}
		}
		return noEntries;
	}

	private static int readIntFromDataInput(final StandardFile input, final byte[] bytes) throws ZipException {
		try {
			if (input.read(bytes, 0, 4) == 4) {
				return RawUtils.readInt(bytes, 0, ByteOrder.LITTLE_ENDIAN);
			}
		} catch (IOException | DataInvalidException e) {
			throw new ZipException(0x0000001B0035L, e);
		}
		throw new ZipException(0x0000001B0035L);
	}

	private static void setFileReadOnly(final File file) throws ZipException {
		if (file == null) {
			throw new ZipException(0x0000001B0031L);
		}

		if (!file.exists() || !file.setReadOnly()) {
			throw new ZipException(0x0000001B0032L);
		}
	}

	private static void setFileLastModify(final File file, final long lastModify) throws ZipException {
		if (file == null) {
			throw new ZipException(0x0000001B0031L);
		}

		if (lastModify < 0L) {
			throw new ZipException(0x0000001B0034L);
		}

		if (!file.exists() || !file.setLastModified(lastModify)) {
			throw new ZipException(0x0000001B0033L);
		}
	}
}
