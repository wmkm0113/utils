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
package org.nervousync.utils.net;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.Config;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.*;
import org.nervousync.beans.files.SegmentationBlock;
import org.nervousync.beans.files.SegmentationInfo;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.security.EncodeType;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.IOUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;
import org.nervousync.utils.security.SecurityUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.CRC32;

/**
 * <h2 class="en-US">Network file operate utilities</h2>
 * <h2 class="zh-CN">网络文件操作工具集</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.2.0 $ $Date: Jan 13, 2010 11:08:14 $
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class EnhanceFileUtils {
	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(EnhanceFileUtils.class);

	/**
	 * <span class="en-US">The constant value of SAMBA protocol prefix</span>
	 * <span class="zh-CN">Samba协议的起始前缀值</span>
	 */
	public static final String SAMBA_PROTOCOL = "smb://";

	/**
	 * <h3 class="en-US">Private constructor method for the network file operate utilities</h3>
	 * <h3 class="zh-CN">网络文件操作工具集的私有构造函数</h3>
	 */
	private EnhanceFileUtils() {
	}

	static {
		//  Register SMB protocol handler for using java.net.URL class with "smb://"
		Config.registerSmbURLHandler();
	}

	/**
	 * <h3 class="en-US">Read the file last modified time</h3>
	 * <h3 class="zh-CN">读取文件最后修改时间</h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @return <span class="en-US">last modified time with the long type if file exists</span>
	 * <span class="zh-CN">如果文件存在，则最后修改时间为 long 类型</span>
	 */
	public static long lastModify(final String filePath) {
		return lastModify(filePath, new Properties());
	}

	/**
	 * <h3 class="en-US">Read the file last modified time</h3>
	 * <h3 class="zh-CN">读取文件最后修改时间</h3>
	 *
	 * @param resourceLocation <span class="en-US">the location String</span>
	 *                         <span class="zh-CN">位置字符串</span>
	 * @param properties       <span class="en-US">the properties configure of samba</span>
	 *                         <span class="zh-CN">访问samba的配置信息</span>
	 * @return <span class="en-US">last modified time with the long type if file exists</span>
	 * <span class="zh-CN">如果文件存在，则最后修改时间为 long 类型</span>
	 */
	public static long lastModify(final String resourceLocation, final Properties properties) {
		if (resourceLocation == null || resourceLocation.trim().isEmpty()) {
			return Globals.DEFAULT_VALUE_LONG;
		}
		if (resourceLocation.startsWith(SAMBA_PROTOCOL)) {
			try (SmbFile smbFile = new SmbFile(resourceLocation, new BaseContext(new PropertyConfiguration(properties)))) {
				if (smbFile.exists()) {
					return smbFile.getLastModified();
				}
			} catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Last_Modify_Read_File_Error", e);
				}
			}
			return Globals.DEFAULT_VALUE_LONG;
		} else {
			return FileUtils.lastModify(resourceLocation);
		}
	}

	/**
	 * <h3 class="en-US">Read the file last modified time</h3>
	 * <h3 class="zh-CN">读取文件最后修改时间</h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @return <span class="en-US">last modified time with the <code>java.util.Date</code> type if file exists or <code>null</code> for others</span>
	 * <span class="zh-CN">如果文件存在，则最后修改时间为<code>java.util.Date</code>类型，其他情况返回<code>null</code></span>
	 */
	public static Date modifyDate(final String filePath) {
		return modifyDate(filePath, new Properties());
	}

	/**
	 * <h3 class="en-US">Read the file last modified time</h3>
	 * <h3 class="zh-CN">读取文件最后修改时间</h3>
	 *
	 * @param resourceLocation <span class="en-US">the location String</span>
	 *                         <span class="zh-CN">位置字符串</span>
	 * @param properties       <span class="en-US">the properties configure of samba</span>
	 *                         <span class="zh-CN">访问samba的配置信息</span>
	 * @return <span class="en-US">last modified time with the <code>java.util.Date</code> type if file exists or <code>null</code> for others</span>
	 * <span class="zh-CN">如果文件存在，则最后修改时间为<code>java.util.Date</code>类型，其他情况返回<code>null</code></span>
	 */
	public static Date modifyDate(final String resourceLocation, final Properties properties) {
		long lastModify = lastModify(resourceLocation, properties);
		if (lastModify != Globals.DEFAULT_VALUE_LONG) {
			return new Date(lastModify);
		} else {
			return null;
		}
	}

	/**
	 * <h3 class="en-US">Load resource and convert to <code>java.io.InputStream</code> used <code>Globals.DEFAULT_ENCODING</code></h3>
	 * <h3 class="zh-CN">使用 Globals.DEFAULT_ENCODING 加载资源并转换为 <code>java.io.InputStream</code></h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @return <span class="en-US"><code>java.io.InputStream</code> instance</span>
	 * <span class="zh-CN"><code>java.io.InputStream</code>实例对象</span>
	 * @throws IOException <span class="en-US">when opening input stream error</span>
	 *                     <span class="zh-CN">打开输入流时出错</span>
	 */
	public static InputStream loadFile(final String filePath) throws IOException {
		return loadFile(filePath, new Properties());
	}

	/**
	 * <h3 class="en-US">Load resource from samba server and convert to <code>java.io.InputStream</code></h3>
	 * <h3 class="zh-CN">从samba服务器加载资源并转换为 <code>java.io.InputStream</code></h3>
	 *
	 * @param smbLocation <span class="en-US">the samba file location</span>
	 *                    <span class="zh-CN">samba文件位置</span>
	 * @param properties  <span class="en-US">the properties configure of samba</span>
	 *                    <span class="zh-CN">访问samba的配置信息</span>
	 * @return <span class="en-US"><code>java.io.InputStream</code> instance</span>
	 * <span class="zh-CN"><code>java.io.InputStream</code>实例对象</span>
	 * @throws IOException <span class="en-US">when opening input stream error</span>
	 *                     <span class="zh-CN">打开输入流时出错</span>
	 */
	public static InputStream loadFile(final String smbLocation, final Properties properties) throws IOException {
		return loadFile(smbLocation, properties, null);
	}

	/**
	 * <h3 class="en-US">Load resource from samba server and convert to <code>java.io.InputStream</code></h3>
	 * <h3 class="zh-CN">从samba服务器加载资源并转换为 <code>java.io.InputStream</code></h3>
	 *
	 * @param smbLocation   <span class="en-US">the samba file location</span>
	 *                      <span class="zh-CN">samba文件位置</span>
	 * @param authenticator <span class="en-US">Authenticator instance</span>
	 *                      <span class="zh-CN">身份验证器实例</span>
	 * @return <span class="en-US"><code>java.io.InputStream</code> instance</span>
	 * <span class="zh-CN"><code>java.io.InputStream</code>实例对象</span>
	 * @throws IOException <span class="en-US">when opening input stream error</span>
	 *                     <span class="zh-CN">打开输入流时出错</span>
	 */
	public static InputStream loadFile(final String smbLocation, final NtlmPasswordAuthenticator authenticator)
			throws IOException {
		return loadFile(smbLocation, null, authenticator);
	}

	/**
	 * <h3 class="en-US">Load resource from samba server and convert to <code>java.io.InputStream</code></h3>
	 * <h3 class="zh-CN">从samba服务器加载资源并转换为 <code>java.io.InputStream</code></h3>
	 *
	 * @param smbLocation   <span class="en-US">the samba file location</span>
	 *                      <span class="zh-CN">samba文件位置</span>
	 * @param properties    <span class="en-US">the properties configure of samba</span>
	 *                      <span class="zh-CN">访问samba的配置信息</span>
	 * @param authenticator <span class="en-US">Authenticator instance</span>
	 *                      <span class="zh-CN">身份验证器实例</span>
	 * @return <span class="en-US"><code>java.io.InputStream</code> instance</span>
	 * <span class="zh-CN"><code>java.io.InputStream</code>实例对象</span>
	 * @throws IOException <span class="en-US">when opening input stream error</span>
	 *                     <span class="zh-CN">打开输入流时出错</span>
	 */
	public static InputStream loadFile(final String smbLocation, final Properties properties,
	                                   final NtlmPasswordAuthenticator authenticator)
			throws IOException {
		if (StringUtils.isEmpty(smbLocation)) {
			throw new IOException("Location is not a valid smb location! ");
		}
		return smbLocation.startsWith(SAMBA_PROTOCOL)
				? new SmbFileInputStream(smbLocation, generateContext(properties, authenticator))
				: FileUtils.loadFile(smbLocation);
	}

	/**
	 * <h3 class="en-US">Resolve the given resource location to a <code>jcifs.smb.SmbFile</code></h3>
	 * <h3 class="zh-CN">将给定资源位置解析为 <code>jcifs.smb.SmbFile</code></h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @return <span class="en-US">a corresponding <code>jcifs.smb.SmbFile</code> object or <code>null</code> if an error occurs</span>
	 * <span class="zh-CN">对应的<code>jcifs.smb.SmbFile</code>对象，如果出现异常则返回<code>null</code></span>
	 */
	public static Object getFile(final String filePath) {
		return getFile(filePath, null, null);
	}

	/**
	 * <h3 class="en-US">Resolve the given resource location to a <code>jcifs.smb.SmbFile</code></h3>
	 * <h3 class="zh-CN">将给定资源位置解析为 <code>jcifs.smb.SmbFile</code></h3>
	 *
	 * @param smbLocation <span class="en-US">the samba file location</span>
	 *                    <span class="zh-CN">samba文件位置</span>
	 * @param properties  <span class="en-US">the properties configure of samba</span>
	 *                    <span class="zh-CN">访问samba的配置信息</span>
	 * @return <span class="en-US">a corresponding <code>jcifs.smb.SmbFile</code> object or <code>null</code> if an error occurs</span>
	 * <span class="zh-CN">对应的<code>jcifs.smb.SmbFile</code>对象，如果出现异常则返回<code>null</code></span>
	 */
	public static Object getFile(final String smbLocation, final Properties properties) {
		return getFile(smbLocation, properties, null);
	}

	/**
	 * <h3 class="en-US">Resolve the given resource location to a <code>jcifs.smb.SmbFile</code></h3>
	 * <h3 class="zh-CN">将给定资源位置解析为 <code>jcifs.smb.SmbFile</code></h3>
	 *
	 * @param smbLocation   <span class="en-US">the samba file location</span>
	 *                      <span class="zh-CN">samba文件位置</span>
	 * @param authenticator <span class="en-US">Authenticator instance</span>
	 *                      <span class="zh-CN">身份验证器实例</span>
	 * @return <span class="en-US">a corresponding <code>jcifs.smb.SmbFile</code> object or <code>null</code> if an error occurs</span>
	 * <span class="zh-CN">对应的<code>jcifs.smb.SmbFile</code>对象，如果出现异常则返回<code>null</code></span>
	 */
	public static Object getFile(final String smbLocation, final NtlmPasswordAuthenticator authenticator) {
		return getFile(smbLocation, null, authenticator);
	}

	/**
	 * <h3 class="en-US">Resolve the given resource location to a <code>jcifs.smb.SmbFile</code></h3>
	 * <h3 class="zh-CN">将给定资源位置解析为 <code>jcifs.smb.SmbFile</code></h3>
	 *
	 * @param smbLocation   <span class="en-US">the samba file location</span>
	 *                      <span class="zh-CN">samba文件位置</span>
	 * @param properties    <span class="en-US">the properties configure of samba</span>
	 *                      <span class="zh-CN">访问samba的配置信息</span>
	 * @param authenticator <span class="en-US">Authenticator instance</span>
	 *                      <span class="zh-CN">身份验证器实例</span>
	 * @return <span class="en-US">a corresponding <code>jcifs.smb.SmbFile</code> object or <code>null</code> if an error occurs</span>
	 * <span class="zh-CN">对应的<code>jcifs.smb.SmbFile</code>对象，如果出现异常则返回<code>null</code></span>
	 */
	public static Object getFile(final String smbLocation, final Properties properties,
	                              final NtlmPasswordAuthenticator authenticator) {
		if (StringUtils.isEmpty(smbLocation)) {
			return null;
		}
		try {
			return smbLocation.startsWith(SAMBA_PROTOCOL)
					? new SmbFile(smbLocation, generateContext(properties, authenticator))
					: FileUtils.getFile(smbLocation);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * <h3 class="en-US">Retrieve content size of the given file path</h3>
	 * <h3 class="zh-CN">读取给定文件地址的文件大小</h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @return <span class="en-US">File size</span>
	 * <span class="zh-CN">文件大小</span>
	 */
	public static long fileSize(final String filePath) {
		return fileSize(getFile(filePath));
	}

	/**
	 * <h3 class="en-US">Retrieve content size of the given file path</h3>
	 * <h3 class="zh-CN">读取给定文件地址的文件大小</h3>
	 *
	 * @param smbLocation   <span class="en-US">the samba file location</span>
	 *                      <span class="zh-CN">samba文件位置</span>
	 * @param cifsContext      <span class="en-US">the cifs context</span>
	 *                         <span class="zh-CN">CIFS上下文配置信息</span>
	 * @return <span class="en-US">File size</span>
	 * <span class="zh-CN">文件大小</span>
	 */
	public static long fileSize(final String smbLocation, final CIFSContext cifsContext) {
		if (StringUtils.isEmpty(smbLocation)) {
			return Globals.DEFAULT_VALUE_LONG;
		}

		return fileSize(EnhanceFileUtils.getFile(smbLocation, cifsContext));
	}

	/**
	 * <h3 class="en-US">Retrieve content size of given file instance</h3>
	 * <h3 class="zh-CN">读取给定文件实例对象的文件大小</h3>
	 *
	 * @param fileObject <span class="en-US">the file object</span>
	 *                   <span class="zh-CN">文件实例对象</span>
	 * @return <span class="en-US">File size</span>
	 * <span class="zh-CN">文件大小</span>
	 */
	public static long fileSize(final Object fileObject) {
		if (fileObject == null) {
			return Globals.DEFAULT_VALUE_LONG;
		}
		if (fileObject instanceof SmbFile) {
			try {
				long fileSize = 0L;
				if (((SmbFile) fileObject).exists()) {
					if (((SmbFile) fileObject).isDirectory()) {
						SmbFile[] childFiles = ((SmbFile) fileObject).listFiles();
						if (childFiles != null) {
							for (SmbFile childFile : childFiles) {
								fileSize += fileSize(childFile);
							}
						}
					} else if (((SmbFile) fileObject).isFile()) {
						fileSize += ((SmbFile) fileObject).length();
					}
				}
				return fileSize;
			} catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Size_Read_File_Error", e);
				}
				return Globals.DEFAULT_VALUE_LONG;
			}
		} else {
			return FileUtils.fileSize(fileObject);
		}
	}

	/**
	 * <h3 class="en-US">Write data bytes to the target file path</h3>
	 * <h3 class="zh-CN">写入字节数组到目标文件路径</h3>
	 *
	 * @param fileData <span class="en-US">file content data bytes</span>
	 *                 <span class="zh-CN">文件内容字节数组</span>
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean saveFile(final byte[] fileData, final String filePath) {
		return EnhanceFileUtils.saveFile(fileData, filePath, new Properties());
	}

	/**
	 * <h3 class="en-US">Write data bytes to the target file path</h3>
	 * <h3 class="zh-CN">写入字节数组到目标文件路径</h3>
	 *
	 * @param fileData   <span class="en-US">file content data bytes</span>
	 *                   <span class="zh-CN">文件内容字节数组</span>
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @param properties <span class="en-US">the properties configure of samba</span>
	 *                   <span class="zh-CN">访问samba的配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean saveFile(final byte[] fileData, final String filePath, final Properties properties) {
		if (StringUtils.isEmpty(filePath)) {
			return Boolean.FALSE;
		}

		if (filePath.startsWith(SAMBA_PROTOCOL)) {
			try (SmbFile smbFile = new SmbFile(filePath, new BaseContext(new PropertyConfiguration(properties)));
			     OutputStream outputStream = new SmbFileOutputStream(smbFile)) {
				smbFile.mkdirs();
				outputStream.write(fileData);
				outputStream.flush();
				return Boolean.TRUE;
			} catch (IOException e) {
				return Boolean.FALSE;
			}
		} else {
			return FileUtils.saveFile(fileData, filePath);
		}
	}

	/**
	 * <h3 class="en-US">Write data from the input stream to the target file path</h3>
	 * <h3 class="zh-CN">从输入流中读取数据并写入到目标文件路径</h3>
	 *
	 * @param inputStream <span class="en-US">input stream instance</span>
	 *                    <span class="zh-CN">输入流实例对象</span>
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean saveFile(final InputStream inputStream, final String filePath) {
		return EnhanceFileUtils.saveFile(inputStream, filePath, new Properties());
	}

	/**
	 * <h3 class="en-US">Write data from the input stream to the target file path</h3>
	 * <h3 class="zh-CN">从输入流中读取数据并写入到目标文件路径</h3>
	 *
	 * @param inputStream <span class="en-US">input stream instance</span>
	 *                    <span class="zh-CN">输入流实例对象</span>
	 * @param smbLocation <span class="en-US">the samba file location</span>
	 *                    <span class="zh-CN">samba文件位置</span>
	 * @param properties  <span class="en-US">the properties configure of samba</span>
	 *                    <span class="zh-CN">访问samba的配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean saveFile(final InputStream inputStream, final String smbLocation, final Properties properties) {
		if (StringUtils.isEmpty(smbLocation) || !smbLocation.startsWith(SAMBA_PROTOCOL)) {
			return Boolean.FALSE;
		}

		try (SmbFile smbFile = new SmbFile(smbLocation, new BaseContext(new PropertyConfiguration(properties)));
				OutputStream outputStream = new SmbFileOutputStream(smbFile)) {
			smbFile.mkdirs();
			long copiedLength = IOUtils.copyStream(inputStream, outputStream, Boolean.FALSE);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Copy_length_File_Debug", copiedLength);
			}
			return Boolean.TRUE;
		} catch (IOException e) {
			LOGGER.error("Target_Save_File_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * <h3 class="en-US">Write content to the target file path, use default charset: UTF-8</h3>
	 * <h3 class="zh-CN">写入文件内容到目标文件路径，使用UTF-8编码</h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @param content  <span class="en-US">file content string</span>
	 *                 <span class="zh-CN">文件内容字符串</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean saveFile(final String filePath, final String content) {
		return EnhanceFileUtils.saveFile(filePath, new Properties(), content, Globals.DEFAULT_ENCODING);
	}

	/**
	 * <h3 class="en-US">Write content to the target file path, use default charset: UTF-8</h3>
	 * <h3 class="zh-CN">写入文件内容到目标文件路径，使用UTF-8编码</h3>
	 *
	 * @param smbLocation <span class="en-US">the samba file location</span>
	 *                    <span class="zh-CN">samba文件位置</span>
	 * @param content    <span class="en-US">file content string</span>
	 *                   <span class="zh-CN">文件内容字符串</span>
	 * @param properties <span class="en-US">the properties configure of samba</span>
	 *                   <span class="zh-CN">访问samba的配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean saveFile(final String smbLocation, final Properties properties, final String content) {
		return EnhanceFileUtils.saveFile(smbLocation, properties, content, Globals.DEFAULT_ENCODING);
	}

	/**
	 * <h3 class="en-US">Write content to the target file path</h3>
	 * <h3 class="zh-CN">写入文件内容到目标文件路径</h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @param content    <span class="en-US">file content string</span>
	 *                   <span class="zh-CN">文件内容字符串</span>
	 * @param properties <span class="en-US">the properties configure of samba</span>
	 *                   <span class="zh-CN">访问samba的配置信息</span>
	 * @param encoding   <span class="en-US">Charset encoding</span>
	 *                   <span class="zh-CN">字符集编码</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean saveFile(final String filePath, final Properties properties,
	                               final String content, final String encoding) {
		if (filePath.startsWith(SAMBA_PROTOCOL)) {
			try (SmbFile smbFile = new SmbFile(filePath, new BaseContext(new PropertyConfiguration(properties)));
			     OutputStream outputStream = new SmbFileOutputStream(smbFile);
				 OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, encoding);
				 PrintWriter printWriter = new PrintWriter(outputStreamWriter)) {
				printWriter.print(content);
				outputStreamWriter.flush();
				return Boolean.TRUE;
			} catch (Exception e) {
				LOGGER.error("Target_Save_File_Error");
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Stack_Message_Error", e);
				}
			}
			return Boolean.FALSE;
		} else {
			return FileUtils.saveFile(filePath, content, encoding);
		}
	}

	/**
	 * <h3 class="en-US">Read content from the target file path, use the default charset: UTF-8</h3>
	 * <h3 class="zh-CN">从目标文件路径读取文件内容，使用UTF-8编码</h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @return <span class="en-US">File content as string</span>
	 * <span class="zh-CN">文件内容字符串</span>
	 */
	public static String readFile(final String filePath) {
		return EnhanceFileUtils.readFile(filePath, Globals.DEFAULT_ENCODING);
	}

	/**
	 * <h3 class="en-US">Read content from the target file path</h3>
	 * <h3 class="zh-CN">从目标文件路径读取文件内容</h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @param encoding <span class="en-US">Charset encoding</span>
	 *                 <span class="zh-CN">字符集编码</span>
	 * @return <span class="en-US">File content as string</span>
	 * <span class="zh-CN">文件内容字符串</span>
	 */
	public static String readFile(final String filePath, final String encoding) {
		try {
			return IOUtils.readContent(EnhanceFileUtils.loadFile(filePath), encoding);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * <h3 class="en-US">Remove the target file</h3>
	 * <h3 class="zh-CN">删除目标文件</h3>
	 *
	 * @param filePath <span class="en-US">target file path</span>
	 *                 <span class="zh-CN">目标文件路径</span>
	 * @param domain   <span class="en-US">Domain name for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的域名地址</span>
	 * @param userName <span class="en-US">Username for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的用户名</span>
	 * @param passWord <span class="en-US">Password for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的密码</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean removeFile(final String filePath, final String domain,
	                                 final String userName, final String passWord) {
		return removeFile(getFile(filePath, smbAuthenticator(domain, userName, passWord)));
	}

	/**
	 * <h3 class="en-US">Remove the target file</h3>
	 * <h3 class="zh-CN">删除目标文件</h3>
	 *
	 * @param smbFile <span class="en-US">the samba file instance</span>
	 *                <span class="zh-CN">samba文件实例对象</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean removeFile(final Object smbFile) {
		if (smbFile == null) {
			return Boolean.TRUE;
		}

		try {
			if (smbFile instanceof SmbFile) {
				if (((SmbFile) smbFile).exists()) {
					if (((SmbFile) smbFile).isDirectory()) {
						removeDir(((SmbFile) smbFile));
					} else {
						((SmbFile) smbFile).delete();
					}
				}
				return Boolean.TRUE;
			} else {
				return FileUtils.removeFile((File) smbFile);
			}
		} catch (Exception e) {
			LOGGER.error("Remove_Files_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Move a file from the base path to the target path</h3>
	 * <h3 class="zh-CN">从原文件地址移动到目标文件地址</h3>
	 *
	 * @param originalPath <span class="en-US">Original path</span>
	 *                     <span class="zh-CN">原文件地址</span>
	 * @param targetPath   <span class="en-US">Target path</span>
	 *                     <span class="zh-CN">目标文件地址</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveFile(final String originalPath, final String targetPath) {
		return moveFile(originalPath, targetPath, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Move a file from the base path to the target path</h3>
	 * <h3 class="zh-CN">从原文件地址移动到目标文件地址</h3>
	 *
	 * @param originalPath <span class="en-US">Original path</span>
	 *                     <span class="zh-CN">原文件地址</span>
	 * @param targetPath   <span class="en-US">Target path</span>
	 *                     <span class="zh-CN">目标文件地址</span>
	 * @param override     <span class="en-US">Override target if exists</span>
	 *                     <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveFile(final String originalPath, final String targetPath, final boolean override) {
		return moveFile(originalPath, null, targetPath, null, override);
	}

	/**
	 * <h3 class="en-US">Move a file from the samba path to the target path</h3>
	 * <h3 class="zh-CN">从原samba文件地址移动到目标文件地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original path</span>
	 *                        <span class="zh-CN">原文件地址</span>
	 * @param originalContext <span class="en-US">the cifs context</span>
	 *                        <span class="zh-CN">CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveFile(final String originalPath, final CIFSContext originalContext, final String targetPath) {
		return moveFile(originalPath, originalContext, targetPath, null, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Move a file from the samba path to the target path</h3>
	 * <h3 class="zh-CN">从原samba文件地址移动到目标文件地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original path</span>
	 *                        <span class="zh-CN">原文件地址</span>
	 * @param originalContext <span class="en-US">the original cifs context</span>
	 *                        <span class="zh-CN">原文件CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @param override        <span class="en-US">Override target if exists</span>
	 *                        <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveFile(final String originalPath, final CIFSContext originalContext,
	                               final String targetPath, final boolean override) {
		return moveFile(originalPath, originalContext, targetPath, null, override);
	}

	/**
	 * <h3 class="en-US">Move a file from the base path to the target samba path</h3>
	 * <h3 class="zh-CN">从原文件地址移动到目标samba文件地址</h3>
	 *
	 * @param originalPath  <span class="en-US">Original path</span>
	 *                      <span class="zh-CN">原文件地址</span>
	 * @param targetPath    <span class="en-US">Target path</span>
	 *                      <span class="zh-CN">目标文件地址</span>
	 * @param targetContext <span class="en-US">the target cifs context</span>
	 *                      <span class="zh-CN">目标文件CIFS上下文配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveFile(final String originalPath, final String targetPath, final CIFSContext targetContext) {
		return moveFile(originalPath, null, targetPath, targetContext, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Move a file from the base path to the target samba path</h3>
	 * <h3 class="zh-CN">从原文件地址移动到目标samba文件地址</h3>
	 *
	 * @param originalPath  <span class="en-US">Original path</span>
	 *                      <span class="zh-CN">原文件地址</span>
	 * @param targetPath    <span class="en-US">Target path</span>
	 *                      <span class="zh-CN">目标文件地址</span>
	 * @param targetContext <span class="en-US">the target cifs context</span>
	 *                      <span class="zh-CN">目标文件CIFS上下文配置信息</span>
	 * @param override      <span class="en-US">Override target if exists</span>
	 *                      <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveFile(final String originalPath, final String targetPath,
	                               final CIFSContext targetContext, final boolean override) {
		return moveFile(originalPath, null, targetPath, targetContext, override);
	}

	/**
	 * <h3 class="en-US">Move a file from the base samba path to the target samba path</h3>
	 * <h3 class="zh-CN">从原samba文件地址移动到目标samba文件地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original path</span>
	 *                        <span class="zh-CN">原文件地址</span>
	 * @param originalContext <span class="en-US">the original cifs context</span>
	 *                        <span class="zh-CN">原文件CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @param targetContext   <span class="en-US">the target cifs context</span>
	 *                        <span class="zh-CN">目标文件CIFS上下文配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveFile(final String originalPath, final CIFSContext originalContext,
	                               final String targetPath, final CIFSContext targetContext) {
		return moveFile(originalPath, originalContext, targetPath, targetContext, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Move a file from the base samba path to the target samba path</h3>
	 * <h3 class="zh-CN">从原samba文件地址移动到目标samba文件地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original path</span>
	 *                        <span class="zh-CN">原文件地址</span>
	 * @param originalContext <span class="en-US">the original cifs context</span>
	 *                        <span class="zh-CN">原文件CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @param targetContext   <span class="en-US">the target cifs context</span>
	 *                        <span class="zh-CN">目标文件CIFS上下文配置信息</span>
	 * @param override        <span class="en-US">Override target if exists</span>
	 *                        <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveFile(final String originalPath, final CIFSContext originalContext,
	                               final String targetPath, final CIFSContext targetContext,
	                               boolean override) {
		if (StringUtils.isEmpty(originalPath) || StringUtils.isEmpty(targetPath)) {
			return Boolean.FALSE;
		}
		try {
			Object originalFile = originalPath.startsWith(SAMBA_PROTOCOL) ? getFile(originalPath, originalContext) : FileUtils.getFile(originalPath);
			Object targetFile = targetPath.startsWith(SAMBA_PROTOCOL) ? getFile(targetPath, targetContext) : FileUtils.getFile(targetPath);
			return copyFile(originalFile, targetFile, override) && removeFile(originalFile);
		} catch (Exception e) {
			LOGGER.error("Move_Files_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * <h3 class="en-US">Move directory from samba folder to target folder</h3>
	 * <h3 class="zh-CN">从原samba文件夹地址移动到目标文件夹地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original folder path</span>
	 *                        <span class="zh-CN">原文件夹地址</span>
	 * @param originalContext <span class="en-US">the original cifs context</span>
	 *                        <span class="zh-CN">原文件夹CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveDir(final String originalPath, final CIFSContext originalContext,
	                              final String targetPath) {
		return moveDir(originalPath, originalContext, targetPath, null, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Move directory from samba folder to target folder</h3>
	 * <h3 class="zh-CN">从原samba文件夹地址移动到目标文件夹地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original folder path</span>
	 *                        <span class="zh-CN">原文件夹地址</span>
	 * @param originalContext <span class="en-US">the original cifs context</span>
	 *                        <span class="zh-CN">原文件夹CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @param override        <span class="en-US">Override target if exists</span>
	 *                        <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveDir(final String originalPath, final CIFSContext originalContext,
	                              final String targetPath, final boolean override) {
		return moveDir(originalPath, originalContext, targetPath, null, override);
	}

	/**
	 * <h3 class="en-US">Move directory from folder to target samba folder</h3>
	 * <h3 class="zh-CN">从原文件夹地址移动到目标samba文件夹地址</h3>
	 *
	 * @param originalPath  <span class="en-US">Original folder path</span>
	 *                      <span class="zh-CN">原文件夹地址</span>
	 * @param targetPath    <span class="en-US">Target path</span>
	 *                      <span class="zh-CN">目标文件地址</span>
	 * @param targetContext <span class="en-US">the target cifs context</span>
	 *                      <span class="zh-CN">目标文件夹CIFS上下文配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveDir(final String originalPath, final String targetPath, final CIFSContext targetContext) {
		return moveDir(originalPath, null, targetPath, targetContext, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Move directory from folder to target samba folder</h3>
	 * <h3 class="zh-CN">从原文件夹地址移动到目标samba文件夹地址</h3>
	 *
	 * @param originalPath  <span class="en-US">Original folder path</span>
	 *                      <span class="zh-CN">原文件夹地址</span>
	 * @param targetPath    <span class="en-US">Target path</span>
	 *                      <span class="zh-CN">目标文件地址</span>
	 * @param targetContext <span class="en-US">the target cifs context</span>
	 *                      <span class="zh-CN">目标文件夹CIFS上下文配置信息</span>
	 * @param override      <span class="en-US">Override target if exists</span>
	 *                      <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveDir(final String originalPath, final String targetPath,
	                              final CIFSContext targetContext, final boolean override) {
		return moveDir(originalPath, null, targetPath, targetContext, override);
	}

	/**
	 * <h3 class="en-US">Move directory from samba folder to target samba folder</h3>
	 * <h3 class="zh-CN">从原samba文件夹地址移动到目标samba文件夹地址</h3>
	 *
	 * @param originalFolder <span class="en-US">the folder instance</span>
	 *                       <span class="zh-CN">文件夹实例对象</span>
	 * @param originalContext <span class="en-US">the original cifs context</span>
	 *                        <span class="zh-CN">原文件夹CIFS上下文配置信息</span>
	 * @param targetPath     <span class="en-US">Target path</span>
	 *                       <span class="zh-CN">目标文件地址</span>
	 * @param targetContext <span class="en-US">the target cifs context</span>
	 *                      <span class="zh-CN">目标文件夹CIFS上下文配置信息</span>
	 * @param override       <span class="en-US">Override target if exists</span>
	 *                       <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean moveDir(final String originalFolder, final CIFSContext originalContext,
	                              final String targetPath, final CIFSContext targetContext, final boolean override) {
		if (StringUtils.isEmpty(originalFolder) || StringUtils.isEmpty(targetPath)) {
			return Boolean.FALSE;
		}
		Object originalFile = null;
		Object targetFile = null;
		try {
			originalFile = originalFolder.startsWith(SAMBA_PROTOCOL) ? getFile(originalFolder, originalContext) : FileUtils.getFile(originalFolder);
			targetFile = targetPath.startsWith(SAMBA_PROTOCOL) ? getFile(targetPath, targetContext) : FileUtils.getFile(targetPath);
			return copyDirectory(originalFile, targetFile, override) && removeDir(originalFolder);
		} catch (Exception e) {
			LOGGER.error("Move_Directory_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		} finally {
			if (originalFile instanceof SmbFile) {
				((SmbFile) originalFile).close();
			}
			if (targetFile instanceof SmbFile) {
				((SmbFile) targetFile).close();
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * <h3 class="en-US">Create directory</h3>
	 * <h3 class="zh-CN">创建文件夹</h3>
	 *
	 * @param targetPath <span class="en-US">Target path</span>
	 *                   <span class="zh-CN">目标文件夹地址</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean makeDir(final String targetPath) {
		return makeDir(targetPath, new Properties());
	}

	/**
	 * <h3 class="en-US">Makes a directory</h3>
	 * <h3 class="zh-CN">创建文件夹</h3>
	 *
	 * @param targetPath <span class="en-US">Target path</span>
	 *                   <span class="zh-CN">目标文件夹地址</span>
	 * @param properties <span class="en-US">the properties configure of samba</span>
	 *                   <span class="zh-CN">访问samba的配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean makeDir(final String targetPath, final Properties properties) {
		if (FileUtils.isExists(targetPath)) {
			return Boolean.TRUE;
		}

		if (targetPath.startsWith(SAMBA_PROTOCOL)) {
			try (SmbFile smbFile = new SmbFile(targetPath,
					new BaseContext(new PropertyConfiguration(properties == null ? new Properties() : properties)))) {
				smbFile.mkdirs();
				return Boolean.TRUE;
			} catch (Exception e) {
				LOGGER.error("Create_Directory_Error");
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Stack_Message_Error", e);
				}
				return Boolean.FALSE;
			}
		} else {
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Copy a file from base path to the target path</h3>
	 * <h3 class="zh-CN">从原文件地址复制到目标文件地址</h3>
	 *
	 * @param originalPath <span class="en-US">Original path</span>
	 *                     <span class="zh-CN">原文件地址</span>
	 * @param targetPath   <span class="en-US">Target path</span>
	 *                     <span class="zh-CN">目标文件地址</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean copy(final String originalPath, final String targetPath) {
		return copy(originalPath, null, targetPath, null, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Copy a file from base path to the target path</h3>
	 * <h3 class="zh-CN">从原文件地址复制到目标文件地址</h3>
	 *
	 * @param originalPath <span class="en-US">Original path</span>
	 *                     <span class="zh-CN">原文件地址</span>
	 * @param targetPath   <span class="en-US">Target path</span>
	 *                     <span class="zh-CN">目标文件地址</span>
	 * @param override     <span class="en-US">Override target if exists</span>
	 *                     <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean copy(final String originalPath, final String targetPath, final boolean override) {
		return copy(originalPath, null, targetPath, null, override);
	}

	/**
	 * <h3 class="en-US">Copy file from base path to target samba path</h3>
	 * <h3 class="zh-CN">从原文件地址复制到目标samba文件地址</h3>
	 *
	 * @param originalPath  <span class="en-US">Original path</span>
	 *                      <span class="zh-CN">原文件地址</span>
	 * @param targetPath    <span class="en-US">Target path</span>
	 *                      <span class="zh-CN">目标文件地址</span>
	 * @param targetContext <span class="en-US">the target cifs context</span>
	 *                      <span class="zh-CN">目标文件CIFS上下文配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean copy(final String originalPath, final String targetPath, final CIFSContext targetContext) {
		return copy(originalPath, null, targetPath, targetContext, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Copy file from base path to target samba path</h3>
	 * <h3 class="zh-CN">从原文件地址复制到目标samba文件地址</h3>
	 *
	 * @param originalPath  <span class="en-US">Original path</span>
	 *                      <span class="zh-CN">原文件地址</span>
	 * @param targetPath    <span class="en-US">Target path</span>
	 *                      <span class="zh-CN">目标文件地址</span>
	 * @param targetContext <span class="en-US">the target cifs context</span>
	 *                      <span class="zh-CN">目标文件CIFS上下文配置信息</span>
	 * @param override      <span class="en-US">Override target if exists</span>
	 *                      <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean copy(final String originalPath, final String targetPath,
	                           final CIFSContext targetContext, final boolean override) {
		return copy(originalPath, null, targetPath, targetContext, override);
	}

	/**
	 * <h3 class="en-US">Copy files from samba path to the target path</h3>
	 * <h3 class="zh-CN">从原samba文件地址复制到目标文件地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original path</span>
	 *                        <span class="zh-CN">原文件地址</span>
	 * @param originalContext <span class="en-US">the cifs context</span>
	 *                        <span class="zh-CN">CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean copy(final String originalPath, final CIFSContext originalContext, final String targetPath) {
		return copy(originalPath, originalContext, targetPath, null, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Copy files from samba path to the target path</h3>
	 * <h3 class="zh-CN">从原samba文件地址复制到目标文件地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original path</span>
	 *                        <span class="zh-CN">原文件地址</span>
	 * @param originalContext <span class="en-US">the original cifs context</span>
	 *                        <span class="zh-CN">原文件CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @param override        <span class="en-US">Override target if exists</span>
	 *                        <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean copy(final String originalPath, final CIFSContext originalContext,
	                           final String targetPath, final boolean override) {
		return copy(originalPath, originalContext, targetPath, null, override);
	}

	/**
	 * <h3 class="en-US">Copy file from the base samba path to the target samba path</h3>
	 * <h3 class="zh-CN">从原samba文件地址复制到目标samba文件地址</h3>
	 *
	 * @param originalPath    <span class="en-US">Original path</span>
	 *                        <span class="zh-CN">原文件地址</span>
	 * @param originalContext <span class="en-US">the original cifs context</span>
	 *                        <span class="zh-CN">原文件CIFS上下文配置信息</span>
	 * @param targetPath      <span class="en-US">Target path</span>
	 *                        <span class="zh-CN">目标文件地址</span>
	 * @param targetContext   <span class="en-US">the target cifs context</span>
	 *                        <span class="zh-CN">目标文件CIFS上下文配置信息</span>
	 * @param override        <span class="en-US">Override target if exists</span>
	 *                        <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean copy(final String originalPath, final CIFSContext originalContext,
	                           final String targetPath, final CIFSContext targetContext, final boolean override) {
		if (StringUtils.isEmpty(originalPath) || StringUtils.isEmpty(targetPath)) {
			return Boolean.FALSE;
		}

		Object originalFile = null;
		Object targetFile = null;

		try {
			originalFile = getFile(originalPath, originalContext);
			boolean originalIsDir = isDirectory(originalPath, originalContext);
			targetFile = getFile(targetPath, targetContext);
			boolean targetIsDir = isDirectory(targetPath, targetContext);
			if (originalIsDir && targetIsDir) {
				return copyDirectory(originalFile, targetFile, override);
			} else if (!originalIsDir && !targetIsDir) {
				return copyFile(originalFile, targetFile, override);
			}
		} catch (Exception e) {
			LOGGER.error("Copy_Directory_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		} finally {
			if (originalFile instanceof SmbFile) {
				((SmbFile) originalFile).close();
			}
			if (targetFile instanceof SmbFile) {
				((SmbFile) targetFile).close();
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * <h3 class="en-US">Remove target directory</h3>
	 * <h3 class="zh-CN">删除目标文件夹</h3>
	 *
	 * @param directoryPath <span class="en-US">target directory path</span>
	 *                      <span class="zh-CN">目标文件夹路径</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean removeDir(final String directoryPath) {
		return removeDir(directoryPath, null);
	}

	/**
	 * <h3 class="en-US">Remove target directory</h3>
	 * <h3 class="zh-CN">删除目标文件夹</h3>
	 *
	 * @param directoryPath <span class="en-US">target directory path</span>
	 *                      <span class="zh-CN">目标文件夹路径</span>
	 * @param cifsContext   <span class="en-US">the cifs context</span>
	 *                      <span class="zh-CN">文件夹CIFS上下文配置信息</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	public static boolean removeDir(final String directoryPath, final CIFSContext cifsContext) {
		if (directoryPath.startsWith(SAMBA_PROTOCOL)) {
			return removeDir((SmbFile) getFile(directoryPath, cifsContext));
		} else {
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Calculate CRC value of the given file path</h3>
	 * <h3 class="zh-CN">计算目标文件的CRC值</h3>
	 *
	 * @param filePath <span class="en-US">file path</span>
	 *                 <span class="zh-CN">文件地址</span>
	 * @return <span class="en-US">CRC value</span>
	 * <span class="zh-CN">CRC值</span>
	 */
	public static long calcFileCRC(final String filePath) {
		try (InputStream inputStream = loadFile(filePath)) {
			byte[] readBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
			int readLength;
			CRC32 crc = new CRC32();

			while ((readLength = inputStream.read(readBuffer)) != Globals.DEFAULT_VALUE_INT) {
				crc.update(readBuffer, 0, readLength);
			}

			return crc.getValue();
		} catch (Exception e) {
			LOGGER.error("CRC_Calculate_Files_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}

		return Globals.DEFAULT_VALUE_LONG;
	}

	/**
	 * <h3 class="en-US">Check the file path is a directory</h3>
	 * <h3 class="zh-CN">检查文件路径是文件夹</h3>
	 *
	 * @param resourceLocation <span class="en-US">the location String to check</span>
	 *                         <span class="zh-CN">要检查的位置字符串</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean isDirectory(final String resourceLocation) {
		return isDirectory(resourceLocation, null);
	}

	/**
	 * <h3 class="en-US">Check the file path is a directory</h3>
	 * <h3 class="zh-CN">检查文件路径是文件夹</h3>
	 *
	 * @param resourceLocation <span class="en-US">the location String to check</span>
	 *                         <span class="zh-CN">要检查的位置字符串</span>
	 * @param cifsContext      <span class="en-US">the cifs context</span>
	 *                         <span class="zh-CN">文件夹CIFS上下文配置信息</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean isDirectory(final String resourceLocation, final CIFSContext cifsContext) {
		if (StringUtils.isEmpty(resourceLocation)) {
			return Boolean.FALSE;
		}

		if (resourceLocation.startsWith(SAMBA_PROTOCOL)) {
			try (SmbFile smbFile = new SmbFile(resourceLocation, cifsContext)) {
				return smbFile.isDirectory();
			} catch (Exception e) {
				return Boolean.FALSE;
			}
		} else {
			return FileUtils.isDirectory(resourceLocation);
		}
	}

	/**
	 * <h3 class="en-US">Check the current file is existed</h3>
	 * <h3 class="zh-CN">检查当前文件是否存在</h3>
	 *
	 * @param filePath <span class="en-US">Current file path</span>
	 *                 <span class="zh-CN">当前文件地址</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean isExists(final String filePath) {
		return isExists(filePath, null);
	}

	/**
	 * <h3 class="en-US">Check the current file is existed</h3>
	 * <h3 class="zh-CN">检查当前文件是否存在</h3>
	 *
	 * @param filePath      <span class="en-US">Current file path</span>
	 *                      <span class="zh-CN">当前文件地址</span>
	 * @param authenticator <span class="en-US">Client authenticator instance</span>
	 *                      <span class="zh-CN">客户端身份验证器实例对象</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean isExists(final String filePath, final NtlmPasswordAuthenticator authenticator) {
		return isExists(filePath, null, authenticator);
	}

	/**
	 * <h3 class="en-US">Check the current file is existed</h3>
	 * <h3 class="zh-CN">检查当前文件是否存在</h3>
	 *
	 * @param filePath      <span class="en-US">Current file path</span>
	 *                      <span class="zh-CN">当前文件地址</span>
	 * @param properties    <span class="en-US">the properties configure of samba</span>
	 *                      <span class="zh-CN">访问samba的配置信息</span>
	 * @param authenticator <span class="en-US">Client authenticator instance</span>
	 *                      <span class="zh-CN">客户端身份验证器实例对象</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean isExists(final String filePath, final Properties properties,
	                               final NtlmPasswordAuthenticator authenticator) {
		if (StringUtils.isEmpty(filePath) || !filePath.startsWith(SAMBA_PROTOCOL)) {
			return Boolean.FALSE;
		}

		try (SmbFile smbFile = new SmbFile(filePath, generateContext(properties, authenticator))) {
			return smbFile.exists();
		} catch (Exception e) {
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Check the current file can read</h3>
	 * <h3 class="zh-CN">检查当前文件是否可以读取</h3>
	 *
	 * @param filePath <span class="en-US">the file path to check</span>
	 *                 <span class="zh-CN">要检查的路径地址</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean canRead(final String filePath) {
		return canRead(filePath, null, null, null);
	}

	/**
	 * <h3 class="en-US">Check the current file can read</h3>
	 * <h3 class="zh-CN">检查当前文件是否可以读取</h3>
	 *
	 * @param filePath <span class="en-US">the file path to check</span>
	 *                 <span class="zh-CN">要检查的路径地址</span>
	 * @param domain   <span class="en-US">Domain name for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的域名地址</span>
	 * @param userName <span class="en-US">Username for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的用户名</span>
	 * @param passWord <span class="en-US">Password for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的密码</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean canRead(final String filePath, final String domain,
	                              final String userName, final String passWord) {
		if (StringUtils.isEmpty(filePath)) {
			return Boolean.FALSE;
		}

		Object file = getFile(filePath, smbAuthenticator(domain, userName, passWord));
		if (file == null) {
			return Boolean.FALSE;
		}
		if (file instanceof SmbFile) {
			try {
				return ((SmbFile) file).canRead();
			} catch (SmbException e) {
				return Boolean.FALSE;
			} finally {
				((SmbFile) file).close();
			}
		} else {
			return ((File) file).canRead();
		}
	}

	/**
	 * <h3 class="en-US">Check the current file can write</h3>
	 * <h3 class="zh-CN">检查当前文件是否可以写入</h3>
	 *
	 * @param filePath <span class="en-US">the file path to check</span>
	 *                 <span class="zh-CN">要检查的路径地址</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean canWrite(String filePath) {
		return canWrite(filePath, null, null, null);
	}

	/**
	 * <h3 class="en-US">Check the current file can write</h3>
	 * <h3 class="zh-CN">检查当前文件是否可以写入</h3>
	 *
	 * @param filePath <span class="en-US">the file path to check</span>
	 *                 <span class="zh-CN">要检查的路径地址</span>
	 * @param domain   <span class="en-US">Domain name for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的域名地址</span>
	 * @param userName <span class="en-US">Username for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的用户名</span>
	 * @param passWord <span class="en-US">Password for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的密码</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean canWrite(String filePath, String domain, String userName, String passWord) {
		Object file = getFile(filePath, smbAuthenticator(domain, userName, passWord));
		if (file == null) {
			return Boolean.FALSE;
		}
		if (file instanceof SmbFile) {
			try {
				return ((SmbFile) file).canWrite();
			} catch (SmbException e) {
				return Boolean.FALSE;
			} finally {
				((SmbFile) file).close();
			}
		} else {
			return ((File) file).canWrite();
		}
	}

	/**
	 * <h3 class="en-US">Generate CIFSContext instance.</h3>
	 * <h3 class="zh-CN">生成 CIFSContext 实例。</h3>
	 *
	 * @param authenticator <span class="en-US">Client authenticator instance</span>
	 *                      <span class="zh-CN">客户端身份验证器实例对象</span>
	 * @return <span class="en-US">Generated instance</span>
	 * <span class="zh-CN">生成的实例对象</span>
	 * @throws CIFSException <span class="en-US">If CIFS properties has error</span>
	 *                       <span class="zh-CN">如果CIFS属性信息出现错误</span>
	 */
	public static CIFSContext generateContext(final NtlmPasswordAuthenticator authenticator)
			throws CIFSException {
		return generateContext(null, authenticator);
	}

	/**
	 * <h3 class="en-US">Generate CIFSContext instance.</h3>
	 * <h3 class="zh-CN">生成 CIFSContext 实例。</h3>
	 *
	 * @param properties    <span class="en-US">the properties configure of samba</span>
	 *                      <span class="zh-CN">访问samba的配置信息</span>
	 * @param authenticator <span class="en-US">Client authenticator instance</span>
	 *                      <span class="zh-CN">客户端身份验证器实例对象</span>
	 * @return <span class="en-US">Generated instance</span>
	 * <span class="zh-CN">生成的实例对象</span>
	 * @throws CIFSException <span class="en-US">If CIFS properties has error</span>
	 *                       <span class="zh-CN">如果CIFS属性信息出现错误</span>
	 */
	public static CIFSContext generateContext(final Properties properties,
	                                          final NtlmPasswordAuthenticator authenticator)
			throws CIFSException {
		CIFSContext cifsContext =
				new BaseContext(new PropertyConfiguration(properties == null ? new Properties() : properties));
		if (authenticator != null) {
			cifsContext = cifsContext.withCredentials(authenticator);
		}
		return cifsContext;
	}

	/**
	 * <h3 class="en-US">Generate samba authenticator instance</h3>
	 * <h3 class="zh-CN">生成samba身份验证器</h3>
	 *
	 * @param domain   <span class="en-US">Domain name for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的域名地址</span>
	 * @param userName <span class="en-US">Username for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的用户名</span>
	 * @param passWord <span class="en-US">Password for NAS file</span>
	 *                 <span class="zh-CN">NAS文件的密码</span>
	 * @return <span class="en-US">authenticator instance</span>
	 * <span class="zh-CN">身份验证器实例对象</span>
	 */
	public static NtlmPasswordAuthenticator smbAuthenticator(final String domain, final String userName,
	                                                         final String passWord) {
		return new NtlmPasswordAuthenticator(domain, userName, passWord);
	}

	/**
	 * <h3 class="en-US">Write segment block data and save to random access file instance</h3>
	 * <h3 class="zh-CN">将分块数据写入目标文件</h3>
	 *
	 * @param randomAccessFile  <span class="en-US">target file path</span>
	 *                          <span class="zh-CN">目标文件路径</span>
	 * @param segmentationBlock <span class="en-US">Segment Data Block</span>
	 *                          <span class="zh-CN">分块数据文件</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">处理结果</span>
	 */
	private static boolean mergeFile(final RandomAccessFile randomAccessFile,
	                                 final SegmentationBlock segmentationBlock) throws IOException {
		if (segmentationBlock == null || !segmentationBlock.securityCheck()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Invalid_Block_Segment_Error");
			}
			return Boolean.FALSE;
		}

		randomAccessFile.seek(segmentationBlock.getPosition());
		randomAccessFile.write(StringUtils.base64Decode(segmentationBlock.getDataInfo()));
		return Boolean.TRUE;
	}

	/**
	 * <h3 class="en-US">Merge segment file data and save to the target path</h3>
	 * <h3 class="zh-CN">合并分割的文件并保存到目标路径</h3>
	 *
	 * @param savePath         <span class="en-US">target file path</span>
	 *                         <span class="zh-CN">目标文件路径</span>
	 * @param segmentationInfo <span class="en-US">Segment Data Information instance</span>
	 *                         <span class="zh-CN">分割数据信息定义实例对象</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">处理结果</span>
	 */
	public static boolean mergeFile(final String savePath, final SegmentationInfo segmentationInfo) {
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(savePath, "rw")) {
			String extName = StringUtils.getFilenameExtension(savePath);
			if (extName.isEmpty()) {
				extName = Globals.DEFAULT_VALUE_STRING;
			}
			if (!segmentationInfo.getExtName().equalsIgnoreCase(extName)) {
				LOGGER.warn("Not_Match_Ext_Name_Files_Warn");
			}
			long totalSize = 0;
			randomAccessFile.setLength(segmentationInfo.getTotalSize());

			for (SegmentationBlock segmentationBlock : segmentationInfo.getBlockList()) {
				if (segmentationBlock == null) {
					return Boolean.FALSE;
				}

				if (mergeFile(randomAccessFile, segmentationBlock)) {
					totalSize += segmentationBlock.getBlockSize();
				}
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Size_Write_Files_Debug", totalSize);
			}

			if (totalSize != segmentationInfo.getTotalSize()) {
				FileUtils.removeFile(savePath);
				return Boolean.FALSE;
			}
			return Boolean.TRUE;
		} catch (Exception e) {
			LOGGER.error("Merge_Files_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Segment file data by given target path</h3>
	 * <h3 class="zh-CN">将目标路径的文件分割处理</h3>
	 *
	 * @param filePath  <span class="en-US">target file path</span>
	 *                  <span class="zh-CN">目标文件路径</span>
	 * @param blockSize <span class="en-US">Segment block size</span>
	 *                  <span class="zh-CN">分割块大小</span>
	 * @return <span class="en-US">Segment Data Information instance</span>
	 * <span class="zh-CN">分割数据信息定义实例对象</span>
	 */
	public static SegmentationInfo segmentFile(final String filePath, final int blockSize) {
		return segmentFile(filePath, blockSize, null, null, null);
	}

	/**
	 * <h3 class="en-US">Segment file data by given target path</h3>
	 * <h3 class="zh-CN">将目标路径的文件分割处理</h3>
	 *
	 * @param filePath  <span class="en-US">target file path</span>
	 *                  <span class="zh-CN">目标文件路径</span>
	 * @param blockSize <span class="en-US">Segment block size</span>
	 *                  <span class="zh-CN">分割块大小</span>
	 * @param domain    <span class="en-US">Domain name for NAS file</span>
	 *                  <span class="zh-CN">NAS文件的域名地址</span>
	 * @param userName  <span class="en-US">Username for NAS file</span>
	 *                  <span class="zh-CN">NAS文件的用户名</span>
	 * @param passWord  <span class="en-US">Password for NAS file</span>
	 *                  <span class="zh-CN">NAS文件的密码</span>
	 * @return <span class="en-US">Segment Data Information instance</span>
	 * <span class="zh-CN">分割数据信息定义实例对象</span>
	 */
	public static SegmentationInfo segmentFile(final String filePath, final int blockSize,
	                                           final String domain, final String userName, final String passWord) {
		NtlmPasswordAuthenticator authenticator = smbAuthenticator(domain, userName, passWord);
		if (!isExists(filePath, authenticator)) {
			return null;
		}

		List<SegmentationBlock> segmentationBlockList = new ArrayList<>();
		InputStream fileInputStream = null;
		ByteArrayOutputStream byteArrayOutputStream;

		try {
			String extName = StringUtils.getFilenameExtension(filePath);
			if (extName.isEmpty()) {
				extName = Globals.DEFAULT_VALUE_STRING;
			} else {
				extName = extName.toLowerCase();
			}
			Object fileObject;
			if (filePath.startsWith(SAMBA_PROTOCOL)) {
				fileObject = new SmbFile(filePath, generateContext(authenticator));
				fileInputStream = new SmbFileInputStream((SmbFile) fileObject);
			} else {
				fileObject = FileUtils.getFile(filePath);
				fileInputStream = new FileInputStream((File) fileObject);
			}
			long fileSize = FileUtils.fileSize(fileObject);

			byte[] readBuffer = new byte[blockSize];
			int index = 0;
			int readLength;
			while ((readLength = fileInputStream.read(readBuffer)) != -1) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Read_Block_Files_Debug", index, readLength);
				}
				byteArrayOutputStream = new ByteArrayOutputStream(blockSize);
				byteArrayOutputStream.write(readBuffer, 0, readLength);
				SegmentationBlock segmentationBlock =
						new SegmentationBlock((long) index * blockSize, byteArrayOutputStream.toByteArray());
				segmentationBlockList.add(segmentationBlock);
				index++;
			}

			return new SegmentationInfo(extName, fileSize, blockSize,
					SecurityUtils.SHA256(fileObject, EncodeType.HEX), segmentationBlockList);
		} catch (FileNotFoundException e) {
			LOGGER.error("Not_Found_File_Error", filePath);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		} catch (IOException e) {
			LOGGER.error("Read_Files_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		} finally {
			IOUtils.closeStream(fileInputStream);
		}

		return null;
	}

	/**
	 * <h3 class="en-US">Resolve the given resource location to a <code>jcifs.smb.SmbFile</code></h3>
	 * <h3 class="zh-CN">将给定资源位置解析为 <code>jcifs.smb.SmbFile</code></h3>
	 *
	 * @param smbLocation <span class="en-US">the samba file location</span>
	 *                    <span class="zh-CN">samba文件位置</span>
	 * @param cifsContext <span class="en-US">the cifs context</span>
	 *                    <span class="zh-CN">CIFS上下文配置信息</span>
	 * @return <span class="en-US">a corresponding <code>jcifs.smb.SmbFile</code> object or <code>null</code> if an error occurs</span>
	 * <span class="zh-CN">对应的<code>jcifs.smb.SmbFile</code>对象，如果出现异常则返回<code>null</code></span>
	 */
	private static Object getFile(final String smbLocation, final CIFSContext cifsContext) {
		if (StringUtils.isEmpty(smbLocation)) {
			return null;
		}
		try {
			if (smbLocation.startsWith(SAMBA_PROTOCOL)) {
				return new SmbFile(smbLocation, cifsContext);
			} else {
				return FileUtils.getFile(smbLocation);
			}
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * <h3 class="en-US">Remove the target directory</h3>
	 * <h3 class="zh-CN">删除目标文件夹</h3>
	 *
	 * @param directory <span class="en-US">the <code>java.io.File</code> instance</span>
	 *                  <span class="zh-CN"><code>java.io.File</code>实例对象</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	private static boolean removeDir(final SmbFile directory) {
		if (directory == null) {
			return Boolean.FALSE;
		}

		try {
			final boolean smbFile;
			final CIFSContext cifsContext;
			final String basePath;
			String[] fileList;
			fileList = directory.list();
			cifsContext = directory.getContext();
			basePath = directory.getPath();
			if (fileList != null) {
				for (String filePath : fileList) {
					SmbFile childFile = new SmbFile(basePath + "/" + filePath, cifsContext);
					boolean isDirectory = childFile.isDirectory();
					if (isDirectory) {
						if (!removeDir(childFile)) {
							return Boolean.FALSE;
						}
					} else {
						childFile.delete();
					}
				}
			}
			directory.delete();
			return Boolean.TRUE;
		} catch (Exception e) {
			LOGGER.error("Remove_Directory_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Move a file from base samba path to the target samba path</h3>
	 * <h3 class="zh-CN">从原samba文件地址移动到目标samba文件地址</h3>
	 *
	 * @param originalFile <span class="en-US">Original file instance</span>
	 *                     <span class="zh-CN">原文件实例对象</span>
	 * @param targetFile   <span class="en-US">Target file instance</span>
	 *                     <span class="zh-CN">目标文件实例对象</span>
	 * @param override     <span class="en-US">Override target if exists</span>
	 *                     <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	private static boolean copyFile(final Object originalFile, final Object targetFile, final boolean override) {
		if (originalFile == null || targetFile == null) {
			return Boolean.FALSE;
		}

		try {
			if (targetFile instanceof SmbFile) {
				if (!override && ((SmbFile) targetFile).exists()) {
					return Boolean.FALSE;
				}
				try (InputStream inputStream = (originalFile instanceof SmbFile)
						? new SmbFileInputStream((SmbFile) originalFile) : new FileInputStream((File) originalFile);
				     OutputStream outputStream = new SmbFileOutputStream((SmbFile) targetFile)) {
					int readLength;
					byte[] readBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];

					while ((readLength = inputStream.read(readBuffer)) != -1) {
						outputStream.write(readBuffer, Globals.INITIALIZE_INT_VALUE, readLength);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					LOGGER.error("Copy_Files_Error");
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
					return Boolean.FALSE;
				}
			} else {
				if (!override && ((File) targetFile).exists()) {
					return Boolean.FALSE;
				}
				try (InputStream inputStream = (originalFile instanceof SmbFile)
						? new SmbFileInputStream((SmbFile) originalFile) : new FileInputStream((File) originalFile);
				     OutputStream outputStream = new FileOutputStream((File) targetFile)) {
					int readLength;
					byte[] readBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];

					while ((readLength = inputStream.read(readBuffer)) != -1) {
						outputStream.write(readBuffer, Globals.INITIALIZE_INT_VALUE, readLength);
					}
					return Boolean.TRUE;
				} catch (Exception e) {
					LOGGER.error("Copy_Files_Error");
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
					return Boolean.FALSE;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Move_Files_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Move a file from base path to the target path</h3>
	 * <h3 class="zh-CN">从原文件地址移动到目标文件地址</h3>
	 *
	 * @param originalDirectory <span class="en-US">Original folder instance</span>
	 *                          <span class="zh-CN">原文件夹实例对象</span>
	 * @param targetDirectory   <span class="en-US">Target folder instance</span>
	 *                          <span class="zh-CN">目标文件夹实例对象</span>
	 * @param override          <span class="en-US">Override target if exists</span>
	 *                          <span class="zh-CN">覆盖目标文件</span>
	 * @return <span class="en-US"><code>Boolean.TRUE</code> for success and <code>Boolean.FALSE</code> for error</span>
	 * <span class="zh-CN">成功返回<code>Boolean.TRUE</code>，失败返回<code>Boolean.FALSE</code></span>
	 */
	private static boolean copyDirectory(final Object originalDirectory, final Object targetDirectory,
	                                        final boolean override) {
		if (originalDirectory == null || targetDirectory == null) {
			return Boolean.FALSE;
		}

		try {
			String targetBasePath;
			CIFSContext cifsContext = null;
			if (targetDirectory instanceof SmbFile) {
				((SmbFile) targetDirectory).mkdirs();
				targetBasePath = ((SmbFile) targetDirectory).getPath();
				cifsContext = ((SmbFile) targetDirectory).getContext();
			} else {
				if (((File) targetDirectory).exists() || ((File) targetDirectory).mkdirs()) {
					targetBasePath = ((File) targetDirectory).getAbsolutePath();
				} else {
					return Boolean.FALSE;
				}
			}

			boolean processResult = Boolean.TRUE;
			if (originalDirectory instanceof SmbFile) {
				SmbFile[] childFiles = ((SmbFile) originalDirectory).listFiles();
				for (SmbFile tempFile : childFiles) {
					String childPath = targetBasePath + Globals.DEFAULT_PAGE_SEPARATOR + tempFile.getName();
					Object childFile;
					if (targetDirectory instanceof SmbFile) {
						childFile = new SmbFile(childPath, cifsContext);
					} else {
						childFile = FileUtils.getFile(childPath);
					}
					if (tempFile.isDirectory()) {
						processResult &= copyDirectory(tempFile, childFile, override);
					} else if (tempFile.isFile()) {
						processResult &= copyFile(tempFile, childFile, override);
					}
				}
			} else {
				File[] childFiles = ((File) originalDirectory).listFiles();
				if (childFiles != null) {
					for (File tempFile : childFiles) {
						BasicFileAttributes basicFileAttributes =
								Files.readAttributes(tempFile.toPath(), BasicFileAttributes.class);
						String childPath = targetBasePath + Globals.DEFAULT_PAGE_SEPARATOR + tempFile.getName();
						Object childFile;
						if (targetDirectory instanceof SmbFile) {
							childFile = new SmbFile(childPath, cifsContext);
						} else {
							childFile = FileUtils.getFile(childPath);
						}
						if (basicFileAttributes.isDirectory()) {
							processResult &= copyDirectory(tempFile, childFile, override);
						} else if (basicFileAttributes.isRegularFile()) {
							processResult &= copyFile(tempFile, childFile, override);
						}
					}
				}
			}
			return processResult;
		} catch (IOException e) {
			LOGGER.error("Move_Directory_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Boolean.FALSE;
		}
	}
}
