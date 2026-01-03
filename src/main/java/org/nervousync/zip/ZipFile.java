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
import org.nervousync.commons.io.StandardFile;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.utils.*;
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
import java.util.*;

/**
 * <h2 class="en-US">Zip file wrapper class</h2>
 * <span class="en-US">
 * <span>Implements features:</span>
 *     <ul>Create a new zip file</ul>
 *     <ul>Read the zip file (support the split zip file)</ul>
 *     <ul>Append files to the current zip file</ul>
 *     <ul>Read zip entry from the current zip file</ul>
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

		if (StringUtils.isEmpty(rootFolderInZip)) {
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
	 * <h3 class="en-US">Add folder to the current ZIP file</h3>
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
	 * <h3 class="en-US">Add folder to the current ZIP file with zip options</h3>
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

			if (commentLength > Globals.MAX_ALLOWED_ZIP_COMMENT_LENGTH) {
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
								&& RawUtils.readInt(buffer, 0, ByteOrder.LITTLE_ENDIAN) == Globals.EXTSIG) {
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

		List<String> headerBytesList = new ArrayList<>();

		int sizeOfCentralDirectory = this.writeCentralDirectory(outputStream, headerBytesList);

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
					offsetCentralDirectory, headerBytesList);
			this.writeZip64EndOfCentralDirectoryLocator(outputStream, headerBytesList);
		}

		this.writeEndOfCentralDirectoryRecord(sizeOfCentralDirectory, offsetCentralDirectory, headerBytesList);
		this.writeZipHeaderBytes(outputStream, HeaderOperator.convertByteArrayListToByteArray(headerBytesList));
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
	 * <h3 class="en-US">Setter method for the maximum length of split item</h3>
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
			this.checkOptions(zipOptions);

			byte[] readBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
			int readLength;

			outputStream.putNextEntry(null, zipOptions);

			if (!zipOptions.getFileNameInZip().endsWith(Globals.DEFAULT_ZIP_PAGE_SEPARATOR)
					&& !zipOptions.getFileNameInZip().endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
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
			List<String> headerBytesList = new ArrayList<>();

			long offsetCentralDirectory = this.endCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory();

			int sizeOfCentralDirectory = this.writeCentralDirectory(outputStream, headerBytesList);

			if (this.zip64Format) {
				this.checkZip64Format();
				this.zip64EndCentralDirectoryLocator
						.setOffsetZip64EndOfCentralDirectoryRecord(offsetCentralDirectory + sizeOfCentralDirectory);
				this.writeZip64EndOfCentralDirectoryRecord(outputStream, sizeOfCentralDirectory,
						offsetCentralDirectory, headerBytesList);
				this.writeZip64EndOfCentralDirectoryLocator(outputStream, headerBytesList);
			}

			this.writeEndOfCentralDirectoryRecord(sizeOfCentralDirectory, offsetCentralDirectory,
					headerBytesList);
			this.writeZipHeaderBytes(outputStream, HeaderOperator.convertByteArrayListToByteArray(headerBytesList));
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
			String targetPath = destPath;
			if (!targetPath.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
				targetPath += Globals.DEFAULT_PAGE_SEPARATOR;
			}

			if (generalFileHeader.isDirectory()) {
				targetPath += generalFileHeader.getEntryPath();
				targetPath = StringUtils.replace(targetPath, Globals.DEFAULT_ZIP_PAGE_SEPARATOR, Globals.DEFAULT_PAGE_SEPARATOR);
				if (!FileUtils.makeDir(targetPath)) {
					throw new ZipException(0x0000001B0038L);
				}
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
			this.endCentralDirectoryRecord.setSignature(Globals.ENDSIG);
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
			byte[] readBuffer = new byte[Globals.BUFFER_SIZE];
			int readLength;

			for (String filePath : fileList) {
				inputStream = FileUtils.loadFile(filePath);
				ZipOptions fileOptions = (ZipOptions) zipOptions.clone();

				if (!FileUtils.isDirectory(filePath)) {
					if (fileOptions.isEncryptFiles()
							&& fileOptions.getEncryptionMethod() == Globals.ENC_METHOD_STANDARD) {
						fileOptions.setSourceFileCRC(FileUtils.calcFileCRC(filePath));
					}

					if (FileUtils.fileSize(filePath) == 0L) {
						fileOptions.setCompressionMethod(Globals.COMP_STORE);
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

		if (zipOptions.getCompressionMethod() != Globals.COMP_STORE
				&& zipOptions.getCompressionMethod() != Globals.COMP_DEFLATE) {
			throw new ZipException(0x0000001B004AL);
		}

		if (zipOptions.getCompressionMethod() == Globals.COMP_DEFLATE
				&& (zipOptions.getCompressionLevel() < 0 || zipOptions.getCompressionLevel() > 9)) {
			throw new ZipException(0x0000001B004BL);
		}

		if (zipOptions.isEncryptFiles()) {
			if (zipOptions.getEncryptionMethod() != Globals.ENC_METHOD_STANDARD
					&& zipOptions.getEncryptionMethod() != Globals.ENC_METHOD_STRONG
					&& zipOptions.getEncryptionMethod() != Globals.ENC_METHOD_AES) {
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
			int diskNumberStart = generalFileHeader.getDiskNumberStart();

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

			if (generalFileHeader.getEncryptionMethod() == Globals.ENC_METHOD_AES) {
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
			byte[] calculateMac = new byte[Globals.AES_AUTH_LENGTH];

			if (storedMac == null) {
				throw new ZipException(0x0000001B0019L);
			}

			System.arraycopy(tempMacBytes, 0, calculateMac, 0, Globals.AES_AUTH_LENGTH);

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

			if (generalFileHeader.getEncryptionMethod() == Globals.ENC_METHOD_AES) {
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
				if (localFileHeader.getEncryptionMethod() == Globals.ENC_METHOD_AES) {
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
				} else if (localFileHeader.getEncryptionMethod() == Globals.ENC_METHOD_STANDARD) {
					byte[] decryptorHeader = new byte[Globals.STD_DEC_HDR_SIZE];
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
				if (localFileHeader.getEncryptionMethod() == Globals.ENC_METHOD_AES) {
					if (this.decryptor instanceof AESDecryptor) {
						compressedSize -= (((AESDecryptor) this.decryptor).getSaltLength() +
								Globals.PASSWORD_VERIFIER_LENGTH + 10);
						offsetStartOfData += (((AESDecryptor) this.decryptor).getSaltLength() +
								Globals.PASSWORD_VERIFIER_LENGTH);
					} else {
						throw new ZipException(0x0000001B0050L, localFileHeader.getEntryPath());
					}
				} else if (localFileHeader.getEncryptionMethod() == Globals.ENC_METHOD_STANDARD) {
					compressedSize -= Globals.STD_DEC_HDR_SIZE;
					offsetStartOfData += Globals.STD_DEC_HDR_SIZE;
				}
			}

			int compressionMethod = localFileHeader.getCompressionMethod();
			if (generalFileHeader.getEncryptionMethod() == Globals.ENC_METHOD_AES) {
				if (generalFileHeader.getAesExtraDataRecord() == null) {
					throw new ZipException(0x0000001B0010L);
				}
				compressionMethod = generalFileHeader.getAesExtraDataRecord().getCompressionMethod();
			}
			input.seek(offsetStartOfData);

			boolean isAESEncryptedFile = generalFileHeader.isEncrypted()
					&& generalFileHeader.getEncryptionMethod() == Globals.ENC_METHOD_AES;

			int currentIndex;
			if (this.numberFormattedName) {
				currentIndex = Long.valueOf(generalFileHeader.getOffsetLocalHeader() / this.splitLength).intValue();
			} else {
				currentIndex = generalFileHeader.getDiskNumberStart();
			}
			switch (compressionMethod) {
				case Globals.COMP_STORE:
					return new ZipInputStream(PartInputStream.newInstance(this, currentIndex,
							offsetStartOfData, compressedSize, this.decryptor, isAESEncryptedFile));
				case Globals.COMP_DEFLATE:
					return new ZipInputStream(InflaterInputStream.newInstance(this, currentIndex,
							offsetStartOfData, compressedSize, generalFileHeader.getOriginalSize(), this.decryptor,
							isAESEncryptedFile));
				default:
					throw new ZipException(0x0000001B004AL);
			}
		} catch (ZipException | IOException e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0043L, e);
			}
		}
	}

	private FileOutputStream openOutputStream(final String folderPath, final String fileName) throws ZipException {
		if (StringUtils.isEmpty(folderPath) || StringUtils.isEmpty(fileName) || !FileUtils.makeDir(folderPath)) {
			throw new ZipException(0x0000001B0043L);
		}

		try {
			String fullPath = folderPath;
			if (!fullPath.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
				fullPath += Globals.DEFAULT_PAGE_SEPARATOR;
			}

			fullPath += fileName;
			fullPath = StringUtils.replace(fullPath, Globals.DEFAULT_ZIP_PAGE_SEPARATOR, Globals.DEFAULT_PAGE_SEPARATOR);
			FileUtils.makeDir(fullPath.substring(0, fullPath.lastIndexOf(Globals.DEFAULT_PAGE_SEPARATOR)));
			return new FileOutputStream(FileUtils.getFile(fullPath));
		} catch (FileNotFoundException e) {
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
	 * Open split file nervousync random access file.
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

			for (int i = 0; i < generalFileHeader.getDiskNumberStart(); i++) {
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
			int fileNameLength = RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN);
			System.arraycopy(tempBuffer, 2, shortBuffer, 0, 2);
			int extraFieldLength = RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN);

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
			if (signature != Globals.LOCSIG) {
				throw new ZipException(0x0000001B0051L, generalFileHeader.getEntryPath());
			}
			localFileHeader.setSignature(signature);
			length += 4;

			// Extract needed
			System.arraycopy(readBuffer, 4, shortBuffer, 0, 2);
			localFileHeader.setExtractNeeded(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			length += 2;

			// General purpose bit flag
			System.arraycopy(readBuffer, 6, shortBuffer, 0, 2);
			localFileHeader.setFileNameUTF8Encoded(
					(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN) & Globals.UFT8_NAMES_FLAG) != 0);
			localFileHeader.setGeneralPurposeFlag(shortBuffer.clone());
			length += 2;

			int firstByte = shortBuffer[0];

			// Check if data descriptor exists for local file header
			String binaryData = Integer.toBinaryString(firstByte);
			if (binaryData.length() >= 4) {
				localFileHeader.setDataDescriptorExists(binaryData.charAt(3) == '1');
			}

			// Compression method
			System.arraycopy(readBuffer, 8, shortBuffer, 0, 2);
			localFileHeader.setCompressionMethod(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			length += 2;

			// Lase modify time
			System.arraycopy(readBuffer, 10, intBuffer, 0, 4);
			localFileHeader.setLastModFileTime(RawUtils.readShort(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			length += 4;

			// CRC
			System.arraycopy(readBuffer, 14, intBuffer, 0, 4);
			localFileHeader.setCrc32(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
			localFileHeader.setCrcBuffer(intBuffer.clone());
			length += 4;

			// Compressed size
			System.arraycopy(readBuffer, 18, intBuffer, 0, 4);
			localFileHeader.setCompressedSize(RawUtils.readLong(readLongByteFromIntByte(intBuffer), 0, ByteOrder.LITTLE_ENDIAN));
			length += 4;

			// Original size
			System.arraycopy(readBuffer, 22, intBuffer, 0, 4);
			localFileHeader.setOriginalSize(RawUtils.readLong(readLongByteFromIntByte(intBuffer), 0, ByteOrder.LITTLE_ENDIAN));
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

			if (localFileHeader.isEncrypted() && localFileHeader.getEncryptionMethod() != Globals.ENC_METHOD_AES) {
				if ((firstByte & 64) == 64) {
					localFileHeader.setEncryptionMethod(Globals.ENC_METHOD_STRONG);
				} else {
					localFileHeader.setEncryptionMethod(Globals.ENC_METHOD_STANDARD);
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

	private int writeCentralDirectory(final OutputStream outputStream, final List<String> headerBytesList)
			throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		if (this.centralDirectory == null || this.centralDirectory.getFileHeaders() == null
				|| this.centralDirectory.getFileHeaders().isEmpty()) {
			return Globals.INITIALIZE_INT_VALUE;
		}

		int sizeOfCentralDirectory = Globals.INITIALIZE_INT_VALUE;

		for (GeneralFileHeader generalFileHeader : this.centralDirectory.getFileHeaders()) {
			sizeOfCentralDirectory += writeFileHeader(generalFileHeader, outputStream, headerBytesList);
		}
		return sizeOfCentralDirectory;
	}

	private int writeFileHeader(@Nonnull final GeneralFileHeader generalFileHeader, final OutputStream outputStream,
	                            @Nonnull final List<String> headerBytesList) throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		try {
			int sizeOfFileHeader = 0;

			byte[] shortBuffer = new byte[2];
			byte[] intBuffer = new byte[4];
			byte[] longBuffer = new byte[8];

			boolean writeZip64FileSize = false;
			boolean writeZip64OffsetLocalHeader = false;

			HeaderOperator.appendIntToArrayList(generalFileHeader.getSignature(), headerBytesList);
			sizeOfFileHeader += 4;

			HeaderOperator.appendShortToArrayList((short) generalFileHeader.getMadeVersion(), headerBytesList);
			sizeOfFileHeader += 2;

			HeaderOperator.appendShortToArrayList((short) generalFileHeader.getExtractNeeded(), headerBytesList);
			sizeOfFileHeader += 2;

			HeaderOperator.copyByteArrayToList(generalFileHeader.getGeneralPurposeFlag(), headerBytesList);
			sizeOfFileHeader += 2;

			HeaderOperator.appendShortToArrayList((short) generalFileHeader.getCompressionMethod(), headerBytesList);
			sizeOfFileHeader += 2;

			HeaderOperator.appendIntToArrayList(generalFileHeader.getLastModFileTime(), headerBytesList);
			sizeOfFileHeader += 4;

			HeaderOperator.appendIntToArrayList((int) (generalFileHeader.getCrc32()), headerBytesList);
			sizeOfFileHeader += 4;

			if (generalFileHeader.getOriginalSize() + Globals.ZIP64_EXTRA_BUFFER_SIZE >= Globals.ZIP_64_LIMIT
					|| generalFileHeader.getCompressedSize() >= Globals.ZIP_64_LIMIT) {
				RawUtils.writeLong(longBuffer, 0, Globals.ZIP_64_LIMIT);
				System.arraycopy(longBuffer, 0, intBuffer, 0, 4);

				HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);
				sizeOfFileHeader += 4;

				HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);
				sizeOfFileHeader += 4;

				writeZip64FileSize = true;
			} else {
				RawUtils.writeLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN, generalFileHeader.getCompressedSize());
				System.arraycopy(longBuffer, 0, intBuffer, 0, 4);
				HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);
				sizeOfFileHeader += 4;

				RawUtils.writeLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN, generalFileHeader.getOriginalSize());
				System.arraycopy(longBuffer, 0, intBuffer, 0, 4);
				HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);
				sizeOfFileHeader += 4;
			}

			RawUtils.writeShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN, (short) generalFileHeader.getFileNameLength());
			HeaderOperator.copyByteArrayToList(shortBuffer, headerBytesList);
			sizeOfFileHeader += 2;

			// Compute offset bytes before extra field is written for Zip64
			// compatibility
			// NOTE: this data is not written now, but written at a later point
			byte[] offsetLocalHeaderBytes = new byte[4];
			if (generalFileHeader.getOffsetLocalHeader() > Globals.ZIP_64_LIMIT) {
				RawUtils.writeLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN, Globals.ZIP_64_LIMIT);
				System.arraycopy(longBuffer, 0, offsetLocalHeaderBytes, 0, 4);
				writeZip64OffsetLocalHeader = true;
			} else {
				RawUtils.writeLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN, generalFileHeader.getOffsetLocalHeader());
				System.arraycopy(longBuffer, 0, offsetLocalHeaderBytes, 0, 4);
			}

			// extra field length
			int extraFieldLength = 0;
			if (writeZip64FileSize || writeZip64OffsetLocalHeader) {
				extraFieldLength += 4;
				if (writeZip64FileSize) {
					extraFieldLength += 16;
				}
				if (writeZip64OffsetLocalHeader) {
					extraFieldLength += 8;
				}
			}
			if (generalFileHeader.getAesExtraDataRecord() != null) {
				extraFieldLength += 11;
			}
			HeaderOperator.appendShortToArrayList((short) extraFieldLength, headerBytesList);
			sizeOfFileHeader += 2;

			// Skip file comment length for now
			HeaderOperator.copyByteArrayToList(EMPTY_SHORT_BUFFER, headerBytesList);
			sizeOfFileHeader += 2;

			// Skip disk number start for now
			HeaderOperator.appendShortToArrayList((short) generalFileHeader.getDiskNumberStart(), headerBytesList);
			sizeOfFileHeader += 2;

			// Skip internal file attributes for now
			HeaderOperator.copyByteArrayToList(EMPTY_SHORT_BUFFER, headerBytesList);
			sizeOfFileHeader += 2;

			// External file attributes
			if (generalFileHeader.getExternalFileAttr() != null) {
				HeaderOperator.copyByteArrayToList(generalFileHeader.getExternalFileAttr(), headerBytesList);
			} else {
				HeaderOperator.copyByteArrayToList(EMPTY_INT_BUFFER, headerBytesList);
			}
			sizeOfFileHeader += 4;

			// offset local header this data is computed above
			HeaderOperator.copyByteArrayToList(offsetLocalHeaderBytes, headerBytesList);
			sizeOfFileHeader += 4;

			byte[] fileNameBytes = generalFileHeader.getEntryPath().getBytes(this.charsetEncoding);
			HeaderOperator.copyByteArrayToList(fileNameBytes, headerBytesList);
			sizeOfFileHeader += fileNameBytes.length;

			if (writeZip64FileSize || writeZip64OffsetLocalHeader) {
				this.zip64Format = true;

				// Zip64 header
				HeaderOperator.appendShortToArrayList((short) Globals.EXTRAFIELDZIP64LENGTH, headerBytesList);
				sizeOfFileHeader += 2;

				// Zip64 extra data record size
				int dataSize = 0;

				if (writeZip64FileSize) {
					dataSize += 16;
				}
				if (writeZip64OffsetLocalHeader) {
					dataSize += 8;
				}

				HeaderOperator.appendShortToArrayList((short) dataSize, headerBytesList);
				sizeOfFileHeader += 2;

				if (writeZip64FileSize) {
					HeaderOperator.appendLongToArrayList(generalFileHeader.getOriginalSize(), headerBytesList);
					sizeOfFileHeader += 8;

					HeaderOperator.appendLongToArrayList(generalFileHeader.getCompressedSize(), headerBytesList);
					sizeOfFileHeader += 8;
				}

				if (writeZip64OffsetLocalHeader) {
					HeaderOperator.appendLongToArrayList(generalFileHeader.getOffsetLocalHeader(), headerBytesList);
					sizeOfFileHeader += 8;
				}
			}

			if (generalFileHeader.getAesExtraDataRecord() != null) {
				AESEngine.processHeader(generalFileHeader.getAesExtraDataRecord(), headerBytesList);
				sizeOfFileHeader += 11;
			}

			return sizeOfFileHeader;
		} catch (Exception e) {
			throw new ZipException(0x0000001B0047L, e);
		}
	}

	private void writeZip64EndOfCentralDirectoryRecord(final OutputStream outputStream, final int sizeOfCentralDirectory,
	                                                   final long offsetCentralDirectory, final List<String> headerBytesList)
			throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		try {
			byte[] intBuffer = new byte[4];

			final byte[] EMPTY_SHORT_BUFFER = {0, 0};

			// zip64 end of central dir signature
			HeaderOperator.appendIntToArrayList((int) Globals.ZIP64ENDCENDIRREC, headerBytesList);

			// size zip64 end of central directory record
			HeaderOperator.appendLongToArrayList(44L, headerBytesList);

			// version made by
			// version needed to extract
			if (this.centralDirectory != null && this.centralDirectory.getFileHeaders() != null
					&& !this.centralDirectory.getFileHeaders().isEmpty()) {
				HeaderOperator.appendShortToArrayList((short) this.centralDirectory.getFileHeaders().get(0).getMadeVersion(), headerBytesList);
				HeaderOperator.appendShortToArrayList((short) this.centralDirectory.getFileHeaders().get(0).getExtractNeeded(), headerBytesList);
			} else {
				HeaderOperator.copyByteArrayToList(EMPTY_SHORT_BUFFER, headerBytesList);
				HeaderOperator.copyByteArrayToList(EMPTY_SHORT_BUFFER, headerBytesList);
			}

			// number of these disks
			RawUtils.writeInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN,
					this.endCentralDirectoryRecord.getIndexOfThisDisk());
			HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);

			// number of the disks with start of central directory
			RawUtils.writeInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN,
					this.endCentralDirectoryRecord.getIndexOfThisDiskStartOfCentralDirectory());
			HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);

			// total number of entries in the central directory on this disk
			int numEntries;
			int numEntriesOnThisDisk = 0;
			if (this.centralDirectory == null || this.centralDirectory.getFileHeaders() == null) {
				throw new ZipException(0x0000001B0028L);
			} else {
				numEntries = this.centralDirectory.getFileHeaders().size();
				if (this.splitArchive) {
					countNumberOfFileHeaderEntriesOnDisk(this.centralDirectory.getFileHeaders(),
							this.endCentralDirectoryRecord.getIndexOfThisDisk());
				} else {
					numEntriesOnThisDisk = numEntries;
				}
			}

			HeaderOperator.appendLongToArrayList(numEntriesOnThisDisk, headerBytesList);

			// Total number of entries in central directory
			HeaderOperator.appendLongToArrayList(numEntries, headerBytesList);

			// Size of central directory
			HeaderOperator.appendLongToArrayList(sizeOfCentralDirectory, headerBytesList);

			// offset start of central directory with respect to the starting disk number
			HeaderOperator.appendLongToArrayList(offsetCentralDirectory, headerBytesList);
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0048L, e);
			}
		}
	}

	private void writeZip64EndOfCentralDirectoryLocator(final OutputStream outputStream,
	                                                    final List<String> headerBytesList) throws ZipException {
		if (outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		try {
			byte[] intBuffer = new byte[4];
			byte[] longBuffer = new byte[8];

			// zip64 end of central dir locator signature
			RawUtils.writeInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN, (int) Globals.ZIP64ENDCENDIRLOC);
			HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);

			// number of the disks with the zip64 end of central directory
			RawUtils.writeInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN,
					this.zip64EndCentralDirectoryLocator.getIndexOfZip64EndOfCentralDirectoryRecord());
			HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);

			// relative offset of the zip64 end of central directory record
			RawUtils.writeLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN,
					this.zip64EndCentralDirectoryLocator.getOffsetZip64EndOfCentralDirectoryRecord());
			HeaderOperator.copyByteArrayToList(longBuffer, headerBytesList);

			// total number of disks
			RawUtils.writeInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN,
					this.zip64EndCentralDirectoryLocator.getTotalNumberOfDiscs());
			HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);
		} catch (Exception e) {
			if (e instanceof ZipException) {
				throw (ZipException) e;
			} else {
				throw new ZipException(0x0000001B0045L, e);
			}
		}
	}

	private void writeEndOfCentralDirectoryRecord(final int sizeOfCentralDirectory, final long offsetCentralDirectory,
	                                              final List<String> headerBytesList) throws ZipException {
		try {
			byte[] shortBuffer = new byte[2];
			byte[] intBuffer = new byte[4];
			byte[] longBuffer = new byte[8];

			// End of central directory signature
			RawUtils.writeInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN, (int) this.endCentralDirectoryRecord.getSignature());
			HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);

			// number of these disks
			RawUtils.writeShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN,
					(short) (this.endCentralDirectoryRecord.getIndexOfThisDisk()));
			HeaderOperator.copyByteArrayToList(shortBuffer, headerBytesList);

			// number of the disks with start of central directory
			RawUtils.writeShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN,
					(short) (this.endCentralDirectoryRecord.getIndexOfThisDiskStartOfCentralDirectory()));
			HeaderOperator.copyByteArrayToList(shortBuffer, headerBytesList);

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
			RawUtils.writeShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN, (short) numEntriesOnThisDisk);
			HeaderOperator.copyByteArrayToList(shortBuffer, headerBytesList);

			// Total number of entries in central directory
			RawUtils.writeShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN, (short) numEntries);
			HeaderOperator.copyByteArrayToList(shortBuffer, headerBytesList);

			// Size of central directory
			RawUtils.writeInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN, sizeOfCentralDirectory);
			HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);

			// Offset central directory
			RawUtils.writeLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN, Math.min(offsetCentralDirectory, Globals.ZIP_64_LIMIT));
			System.arraycopy(longBuffer, 0, intBuffer, 0, 4);
			HeaderOperator.copyByteArrayToList(intBuffer, headerBytesList);

			// Zip File comment length
			int commentLength = 0;
			if (this.endCentralDirectoryRecord.getCommentBytes() != null) {
				commentLength = this.endCentralDirectoryRecord.getCommentLength();
			}
			RawUtils.writeShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN, (short) commentLength);
			HeaderOperator.copyByteArrayToList(shortBuffer, headerBytesList);

			// Comment
			if (commentLength > 0) {
				HeaderOperator.copyByteArrayToList(this.endCentralDirectoryRecord.getCommentBytes(), headerBytesList);
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
				if (((SplitOutputStream) outputStream).checkBufferSizeAndStartNextSplitFile(buffer.length)) {
					this.finalizeZipFile(outputStream);
					return;
				}
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
			position -= Globals.ENDHDR;

			this.endCentralDirectoryRecord = new EndCentralDirectoryRecord();

			int count = 0;
			do {
				input.seek(position--);
				count++;
			} while ((readIntFromDataInput(input, buffer) != Globals.ENDSIG)
					&& count <= 3000);

			int endsig = RawUtils.readInt(buffer, 0, ByteOrder.LITTLE_ENDIAN);
			if (endsig != Globals.ENDSIG) {
				throw new ZipException(0x0000001B0052L);
			}

			byte[] readBuffer = new byte[18];

			if (input.read(readBuffer) == Globals.DEFAULT_VALUE_INT) {
				return;
			}

			byte[] intBuffer = new byte[4];
			byte[] shortBuffer = new byte[2];

			this.endCentralDirectoryRecord.setSignature(Globals.ENDSIG);

			System.arraycopy(readBuffer, 0, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord.setIndexOfThisDisk(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 2, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord
					.setIndexOfThisDiskStartOfCentralDirectory(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 4, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord.setTotalOfEntriesInCentralDirectoryOnThisDisk(
					RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 6, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord
					.setTotalOfEntriesInCentralDirectory(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 8, intBuffer, 0, 4);
			this.endCentralDirectoryRecord.setSizeOfCentralDirectory(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 12, intBuffer, 0, 4);
			this.endCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(
					RawUtils.readLong(readLongByteFromIntByte(intBuffer), 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 16, shortBuffer, 0, 2);
			this.endCentralDirectoryRecord.setCommentLength(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

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
			position -= Globals.ENDHDR;

			do {
				input.seek(position--);
			} while (readIntFromDataInput(input, buffer) != Globals.ENDSIG);

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
			if (signature == Globals.ZIP64ENDCENDIRLOC) {
				this.zip64Format = true;
				this.zip64EndCentralDirectoryLocator.setSignature(signature);
			} else {
				this.zip64Format = false;
				return;
			}

			System.arraycopy(readBuffer, 4, intBuffer, 0, 4);
			this.zip64EndCentralDirectoryLocator
					.setIndexOfZip64EndOfCentralDirectoryRecord(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 8, longBuffer, 0, 8);
			this.zip64EndCentralDirectoryLocator
					.setOffsetZip64EndOfCentralDirectoryRecord(RawUtils.readInt(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			System.arraycopy(readBuffer, 16, intBuffer, 0, 4);
			this.zip64EndCentralDirectoryLocator.setTotalNumberOfDiscs(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
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
				centralDirectoryEntryCount = (int) this.zip64EndCentralDirectoryRecord.getTotalEntriesInCentralDirectory();
			}

			if (this.splitArchive && this.numberFormattedName) {
				offsetOfStartOfCentralDirectory -= ((this.splitCount - 1) * this.splitLength);
			}

			input.seek(offsetOfStartOfCentralDirectory);

			long bufferSize = input.length() - offsetOfStartOfCentralDirectory;
			byte[] readBuffer = new byte[(int) bufferSize];
			if (input.read(readBuffer) == Globals.DEFAULT_VALUE_INT) {
				return;
			}

			byte[] shortBuffer = new byte[2];
			byte[] intBuffer = new byte[4];

			int pos = 0;
			for (int i = 0; i < centralDirectoryEntryCount; i++) {
				GeneralFileHeader fileHeader = new GeneralFileHeader();

				System.arraycopy(readBuffer, pos, intBuffer, 0, 4);
				int signature = RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN);
				if (signature != Globals.CENSIG) {
					throw new ZipException(0x0000001B0040, i);
				}

				fileHeader.setSignature(signature);

				// Made versions
				System.arraycopy(readBuffer, pos + 4, shortBuffer, 0, 2);
				fileHeader.setMadeVersion(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Extract needed
				System.arraycopy(readBuffer, pos + 6, shortBuffer, 0, 2);
				fileHeader.setExtractNeeded(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Purpose bit flag
				System.arraycopy(readBuffer, pos + 8, shortBuffer, 0, 2);
				fileHeader.setFileNameUTF8Encoded(
						(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN) & Globals.UFT8_NAMES_FLAG) != 0);
				int firstByte = shortBuffer[0];
				fileHeader.setGeneralPurposeFlag(shortBuffer.clone());
				fileHeader.setDataDescriptorExists((firstByte >> 3) == 1);

				// Compression method
				System.arraycopy(readBuffer, pos + 10, shortBuffer, 0, 2);
				fileHeader.setCompressionMethod(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Last modify file time
				System.arraycopy(readBuffer, pos + 12, intBuffer, 0, 4);
				fileHeader.setLastModFileTime(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Crc32
				System.arraycopy(readBuffer, pos + 16, intBuffer, 0, 4);
				fileHeader.setCrc32(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
				fileHeader.setCrcBuffer(intBuffer.clone());

				// Compressed size
				System.arraycopy(readBuffer, pos + 20, intBuffer, 0, 4);
				fileHeader.setCompressedSize(RawUtils.readLong(readLongByteFromIntByte(intBuffer), 0, ByteOrder.LITTLE_ENDIAN));

				// Original size
				System.arraycopy(readBuffer, pos + 24, intBuffer, 0, 4);
				fileHeader.setOriginalSize(RawUtils.readLong(readLongByteFromIntByte(intBuffer), 0, ByteOrder.LITTLE_ENDIAN));

				// File name length
				System.arraycopy(readBuffer, pos + 28, shortBuffer, 0, 2);
				fileHeader.setFileNameLength(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Extra field length
				System.arraycopy(readBuffer, pos + 30, shortBuffer, 0, 2);
				fileHeader.setExtraFieldLength(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Comment length
				System.arraycopy(readBuffer, pos + 32, shortBuffer, 0, 2);
				fileHeader.setFileCommentLength(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Disk number of start
				System.arraycopy(readBuffer, pos + 34, shortBuffer, 0, 2);
				fileHeader.setDiskNumberStart(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

				// Internal file attributes
				System.arraycopy(readBuffer, pos + 36, shortBuffer, 0, 2);
				fileHeader.setInternalFileAttr(shortBuffer.clone());

				// External file attributes
				System.arraycopy(readBuffer, pos + 38, intBuffer, 0, 4);
				fileHeader.setExternalFileAttr(intBuffer.clone());

				// Relative offset of local header
				System.arraycopy(readBuffer, pos + 42, intBuffer, 0, 4);
				fileHeader.setOffsetLocalHeader(
						RawUtils.readLong(readLongByteFromIntByte(intBuffer), 0, ByteOrder.LITTLE_ENDIAN) & 0xFFFFFFFFL);

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
			if (signature == Globals.DIGSIG) {
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
			if (signature != Globals.ZIP64ENDCENDIRREC) {
				throw new ZipException(0x0000001B003DL);
			}
			this.zip64EndCentralDirectoryRecord.setSignature(signature);

			// Read the size of zip64 end of central directory record
			System.arraycopy(readBuffer, 4, longBuffer, 0, 8);
			this.zip64EndCentralDirectoryRecord.setRecordSize(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Made versions
			System.arraycopy(readBuffer, 12, shortBuffer, 0, 2);
			this.zip64EndCentralDirectoryRecord.setMadeVersion(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Extract needed
			System.arraycopy(readBuffer, 14, shortBuffer, 0, 2);
			this.zip64EndCentralDirectoryRecord.setExtractNeeded(RawUtils.readShort(shortBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Number of these disks
			System.arraycopy(readBuffer, 16, intBuffer, 0, 4);
			this.zip64EndCentralDirectoryRecord.setIndex(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));

			// Start of central directory
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

			// Offset start of central directory with respect to the starting
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
					if (extraDataRecord.getHeader() == ((short) Globals.AESSIG)) {
						if (extraDataRecord.getDataContent() == null) {
							throw new ZipException(0x0000001B0010L);
						}

						AESExtraDataRecord aesExtraDataRecord = new AESExtraDataRecord();

						aesExtraDataRecord.setSignature(Globals.AESSIG);
						aesExtraDataRecord.setDataSize(extraDataRecord.getDataSize());

						try {
							byte[] aesData = extraDataRecord.getDataContent();
							aesExtraDataRecord.setVersionNumber(RawUtils.readShort(aesData, 0, ByteOrder.LITTLE_ENDIAN));

							byte[] vendorIDBuffer = new byte[2];
							System.arraycopy(aesData, 2, vendorIDBuffer, 0, 2);
							aesExtraDataRecord.setVendorID(new String(vendorIDBuffer, StandardCharsets.UTF_8));
							aesExtraDataRecord.setAesStrength((aesData[4] & 0xFF));
							aesExtraDataRecord.setCompressionMethod(RawUtils.readShort(aesData, 5, ByteOrder.LITTLE_ENDIAN));
						} catch (DataInvalidException e) {
							throw new ZipException(0x0000001B003BL, e);
						}
						fileHeader.setAesExtraDataRecord(aesExtraDataRecord);
						fileHeader.setEncryptionMethod(Globals.ENC_METHOD_AES);
						break;
					}
				}
			}
		}
	}

	private static Zip64ExtendInfo readZip64ExtendInfo(final List<ExtraDataRecord> extraDataRecords, final long originalSize,
	                                                   final long compressedSize, final long offsetLocalHeader,
	                                                   final int diskNumberStart) throws ZipException {
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
					if ((originalSize & 0xFFFF) == 0xFFFF) {
						System.arraycopy(extraDataRecord.getDataContent(), count, longBuffer, 0, 8);
						zip64ExtendInfo.setOriginalSize(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));
						count += 8;
						addValue = Boolean.TRUE;
					}

					if (((compressedSize & 0xFFFF) == 0xFFFF) && count < extraDataRecord.getDataSize()) {
						System.arraycopy(extraDataRecord.getDataContent(), count, longBuffer, 0, 8);
						zip64ExtendInfo.setCompressedSize(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));
						count += 8;
						addValue = Boolean.TRUE;
					}

					if (((offsetLocalHeader & 0xFFFF) == 0xFFFF) && count < extraDataRecord.getDataSize()) {
						System.arraycopy(extraDataRecord.getDataContent(), count, longBuffer, 0, 8);
						zip64ExtendInfo.setOffsetLocalHeader(RawUtils.readLong(longBuffer, 0, ByteOrder.LITTLE_ENDIAN));
						count += 8;
						addValue = Boolean.TRUE;
					}

					if (((diskNumberStart & 0xFFFF) == 0xFFFF) && count < extraDataRecord.getDataSize()) {
						System.arraycopy(extraDataRecord.getDataContent(), count, intBuffer, 0, 4);
						zip64ExtendInfo.setDiskNumberStart(RawUtils.readInt(intBuffer, 0, ByteOrder.LITTLE_ENDIAN));
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
				extraDataRecord.setHeader(RawUtils.readShort(extraFieldBuffer, count, ByteOrder.LITTLE_ENDIAN));

				count += 2;

				int dataSize = RawUtils.readShort(extraFieldBuffer, count, ByteOrder.LITTLE_ENDIAN);

				if ((dataSize + 2) > extraFieldLength) {
					dataSize = RawUtils.readShort(extraFieldBuffer, count, ByteOrder.BIG_ENDIAN);
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

	private static byte[] readLongByteFromIntByte(final byte[] intByte) throws ZipException {
		if (intByte == null) {
			throw new ZipException(0x0000001B0035L);
		}

		if (intByte.length != 4) {
			throw new ZipException(0x0000001B0035L);
		}

		return new byte[]{intByte[0], intByte[1], intByte[2], intByte[3], 0, 0, 0, 0};
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
