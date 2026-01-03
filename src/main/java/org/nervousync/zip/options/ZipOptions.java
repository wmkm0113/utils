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
package org.nervousync.zip.options;

import java.util.TimeZone;

import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.commons.Globals;
import org.nervousync.utils.StringUtils;

/**
 * <h2 class="en-US">ZIP options</h2>
 * <h2 class="zh-CN">ZIP压缩属性</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 29, 2017 15:51:12 $
 */
public final class ZipOptions implements Cloneable {

	/**
	 * <span class="en-US">Folder, file name and comment charset encoding</span>
	 * <span class="zh-CN">目录名、文件名、备注信息的字符集编码</span>
	 */
	private String charsetEncoding = Globals.DEFAULT_SYSTEM_CHARSET;
	/**
	 * <span class="en-US">Compress method</span>
	 * <span class="zh-CN">压缩方式</span>
	 */
	private int compressionMethod = Globals.COMP_DEFLATE;
	/**
	 * <span class="en-US">Compress level</span>
	 * <span class="zh-CN">压缩等级</span>
	 */
	private int compressionLevel = Globals.DEFLATE_LEVEL_NORMAL;
	/**
	 * <span class="en-US">Encrypt files status</span>
	 * <span class="zh-CN">文件加密标记</span>
	 */
	private boolean encryptFiles;
	/**
	 * <span class="en-US">Encrypt method code</span>
	 * <span class="zh-CN">文件加密方式代码</span>
	 */
	private int encryptionMethod;
	/**
	 * <span class="en-US">Status of the read hidden file</span>
	 * <span class="zh-CN">读取隐藏文件标记</span>
	 */
	private boolean readHiddenFiles = Boolean.TRUE;
	/**
	 * <span class="en-US">Encrypt/Decrypt password</span>
	 * <span class="zh-CN">加密/解密密码</span>
	 */
	private final char[] password;
	/**
	 * <span class="en-US">AES key strength</span>
	 * <span class="zh-CN">AES密钥强度</span>
	 */
	private final int aesKeyStrength;
	/**
	 * <span class="en-US">Include root folder</span>
	 * <span class="zh-CN">包含根目录标记</span>
	 */
	private boolean includeRootFolder = Boolean.TRUE;
	/**
	 * <span class="en-US">Root folder path</span>
	 * <span class="zh-CN">根目录路径</span>
	 */
	private String rootFolderInZip = Globals.DEFAULT_VALUE_STRING;
	/**
	 * <span class="en-US">Timezone setting</span>
	 * <span class="zh-CN">时区设置</span>
	 *
	 * @see java.util.TimeZone
	 */
	private TimeZone timeZone = TimeZone.getDefault();
	/**
	 * <span class="en-US">Source file CRC result</span>
	 * <span class="zh-CN">源文件CRC值</span>
	 */
	private long sourceFileCRC = Globals.DEFAULT_VALUE_LONG;
	/**
	 * <span class="en-US">Default folder path</span>
	 * <span class="zh-CN">默认目录路径</span>
	 */
	private String defaultFolderPath = Globals.DEFAULT_VALUE_STRING;
	/**
	 * <span class="en-US">File entry path</span>
	 * <span class="zh-CN">文件在压缩包内的路径</span>
	 */
	private String fileNameInZip = null;
	/**
	 * <span class="en-US">Status of source external stream</span>
	 * <span class="zh-CN">源外部流的标记</span>
	 */
	private boolean sourceExternalStream = Boolean.FALSE;

	/**
	 * <h3 class="en-US">Private constructor method for ZIP options</h3>
	 * <h3 class="zh-CN">ZIP压缩属性的私有构造方法</h3>
	 *
	 * @param encryptFiles     <span class="en-US">Encrypt files status</span>
	 *                         <span class="zh-CN">文件加密标记</span>
	 * @param password         <span class="en-US">Encrypt/Decrypt password</span>
	 *                         <span class="zh-CN">加密/解密密码</span>
	 * @param encryptionMethod <span class="en-US">Encrypt method code</span>
	 *                         <span class="zh-CN">文件加密方式代码</span>
	 * @param aesKeyStrength   <span class="en-US">AES key strength</span>
	 *                         <span class="zh-CN">AES密钥强度</span>
	 */
	private ZipOptions(final boolean encryptFiles, final String password, final int encryptionMethod,
	                   final int aesKeyStrength) {
		this.encryptFiles = encryptFiles;
		this.password = password.toCharArray();
		this.encryptionMethod = encryptionMethod;
		this.aesKeyStrength = aesKeyStrength;
	}

	/**
	 * <h3 class="en-US">Static method for generate the default ZIP options</h3>
	 * <h3 class="zh-CN">静态方法用于生成默认的ZIP压缩属性</h3>
	 *
	 * @return <span class="en-US">ZIP options instance object</span>
	 * <span class="zh-CN">ZIP压缩属性实例对象</span>
	 */
	public static ZipOptions newOptions() {
		return new ZipOptions(Boolean.FALSE, Globals.DEFAULT_VALUE_STRING, Globals.ENC_NO_ENCRYPTION, Globals.AES_STRENGTH_128);
	}

	/**
	 * <h3 class="en-US">Static method for generate the standard encrypt ZIP options</h3>
	 * <h3 class="zh-CN">静态方法用于生成标准加密的ZIP压缩属性</h3>
	 *
	 * @param password <span class="en-US">Encrypt/Decrypt password</span>
	 *                 <span class="zh-CN">加密/解密密码</span>
	 * @return <span class="en-US">ZIP options instance object</span>
	 * <span class="zh-CN">ZIP压缩属性实例对象</span>
	 * @throws ZipException <span class="en-US">If the given password is empty string or <code>null</code></span>
	 *                      <span class="zh-CN">如果给定的密码为空字符串或<code>null</code></span>
	 */
	public static ZipOptions standardEncryptOptions(final String password) throws ZipException {
		if (StringUtils.isEmpty(password)) {
			throw new ZipException(0x0000001B0006L);
		}
		return new ZipOptions(Boolean.TRUE, password, Globals.ENC_METHOD_STANDARD, Globals.AES_STRENGTH_128);
	}

	/**
	 * <h3 class="en-US">Static method for generate the AES encrypt ZIP options</h3>
	 * <h3 class="zh-CN">静态方法用于生成AES加密的ZIP压缩属性</h3>
	 *
	 * @param password <span class="en-US">Encrypt/Decrypt password</span>
	 *                 <span class="zh-CN">加密/解密密码</span>
	 * @return <span class="en-US">ZIP options instance object</span>
	 * <span class="zh-CN">ZIP压缩属性实例对象</span>
	 * @throws ZipException <span class="en-US">If the given password is empty string or <code>null</code></span>
	 *                      <span class="zh-CN">如果给定的密码为空字符串或<code>null</code></span>
	 */
	public static ZipOptions aesEncryptOptions(final String password) throws ZipException {
		return ZipOptions.aesEncryptOptions(password, 128);
	}

	/**
	 * <h3 class="en-US">Static method for generate the AES encrypt ZIP options</h3>
	 * <h3 class="zh-CN">静态方法用于生成AES加密的ZIP压缩属性</h3>
	 *
	 * @param password     <span class="en-US">Encrypt/Decrypt password</span>
	 *                     <span class="zh-CN">加密/解密密码</span>
	 * @param aesKeyLength <span class="en-US">AES key length</span>
	 *                     <span class="zh-CN">AES密钥长度</span>
	 * @return <span class="en-US">ZIP options instance object</span>
	 * <span class="zh-CN">ZIP压缩属性实例对象</span>
	 * @throws ZipException <span class="en-US">If the given password is empty string or <code>null</code></span>
	 *                      <span class="zh-CN">如果给定的密码为空字符串或<code>null</code></span>
	 */
	public static ZipOptions aesEncryptOptions(final String password, final int aesKeyLength) throws ZipException {
		if (StringUtils.isEmpty(password)) {
			throw new ZipException(0x0000001B0006L);
		}

		switch (aesKeyLength) {
			case 128:
				return new ZipOptions(Boolean.TRUE, password, Globals.ENC_METHOD_AES, Globals.AES_STRENGTH_128);
			case 192:
				return new ZipOptions(Boolean.TRUE, password, Globals.ENC_METHOD_AES, Globals.AES_STRENGTH_192);
			case 256:
				return new ZipOptions(Boolean.TRUE, password, Globals.ENC_METHOD_AES, Globals.AES_STRENGTH_256);
			default:
				throw new ZipException(0x0000001B0005L);
		}
	}

	/**
	 * <h3 class="en-US">Getter method for the folder, file name and comment charset encoding</h3>
	 * <h3 class="zh-CN">目录名、文件名、备注信息的字符集编码的Getter方法</h3>
	 *
	 * @return <span class="en-US">Folder, file name and comment charset encoding</span>
	 * <span class="zh-CN">目录名、文件名、备注信息的字符集编码</span>
	 */
	public String getCharsetEncoding() {
		return this.charsetEncoding;
	}

	/**
	 * <h3 class="en-US">Setter method for the folder, file name and comment charset encoding</h3>
	 * <h3 class="zh-CN">目录名、文件名、备注信息的字符集编码的Setter方法</h3>
	 *
	 * @param charsetEncoding <span class="en-US">Folder, file name and comment charset encoding</span>
	 *                        <span class="zh-CN">目录名、文件名、备注信息的字符集编码</span>
	 */
	public void setCharsetEncoding(final String charsetEncoding) {
		this.charsetEncoding = charsetEncoding;
	}

	/**
	 * <h3 class="en-US">Getter method for the compress method</h3>
	 * <h3 class="zh-CN">压缩方式的Getter方法</h3>
	 *
	 * @return <span class="en-US">Compress method</span>
	 * <span class="zh-CN">压缩方式</span>
	 */
	public int getCompressionMethod() {
		return this.compressionMethod;
	}

	/**
	 * <h3 class="en-US">Setter method for the compress method</h3>
	 * <h3 class="zh-CN">压缩方式的Setter方法</h3>
	 *
	 * @param compressionMethod <span class="en-US">Compress method</span>
	 *                          <span class="zh-CN">压缩方式</span>
	 */
	public void setCompressionMethod(final int compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	/**
	 * <h3 class="en-US">Getter method for the compress level</h3>
	 * <h3 class="zh-CN">压缩等级的Getter方法</h3>
	 *
	 * @return <span class="en-US">Compress level</span>
	 * <span class="zh-CN">压缩等级</span>
	 */
	public int getCompressionLevel() {
		return this.compressionLevel;
	}

	/**
	 * <h3 class="en-US">Setter method for the compress level</h3>
	 * <h3 class="zh-CN">压缩等级的Setter方法</h3>
	 *
	 * @param compressionLevel <span class="en-US">Compress level</span>
	 *                         <span class="zh-CN">压缩等级</span>
	 */
	public void setCompressionLevel(final int compressionLevel) {
		this.compressionLevel = compressionLevel;
	}

	/**
	 * <h3 class="en-US">Getter method for encrypt files status</h3>
	 * <h3 class="zh-CN">文件加密标记的Getter方法</h3>
	 *
	 * @return <span class="en-US">Encrypt files status</span>
	 * <span class="zh-CN">文件加密标记</span>
	 */
	public boolean isEncryptFiles() {
		return this.encryptFiles;
	}

	/**
	 * <h3 class="en-US">Setter method for encrypt files status</h3>
	 * <h3 class="zh-CN">文件加密标记的Setter方法</h3>
	 *
	 * @param encryptFiles <span class="en-US">Encrypt files status</span>
	 *                     <span class="zh-CN">文件加密标记</span>
	 */
	public void setEncryptFiles(final boolean encryptFiles) {
		this.encryptFiles = encryptFiles;
	}

	/**
	 * <h3 class="en-US">Getter method for the encrypt method code</h3>
	 * <h3 class="zh-CN">文件加密方式代码的Getter方法</h3>
	 *
	 * @return <span class="en-US">Encrypt method code</span>
	 * <span class="zh-CN">文件加密方式代码</span>
	 */
	public int getEncryptionMethod() {
		return this.encryptionMethod;
	}

	/**
	 * <h3 class="en-US">Setter method for the encrypt method code</h3>
	 * <h3 class="zh-CN">文件加密方式代码的Setter方法</h3>
	 *
	 * @param encryptionMethod <span class="en-US">Encrypt method code</span>
	 *                         <span class="zh-CN">文件加密方式代码</span>
	 */
	public void setEncryptionMethod(final int encryptionMethod) {
		this.encryptionMethod = encryptionMethod;
	}

	/**
	 * <h3 class="en-US">Getter method for the status of the read hidden file</h3>
	 * <h3 class="zh-CN">读取隐藏文件标记的Getter方法</h3>
	 *
	 * @return <span class="en-US">Status of the read hidden file</span>
	 * <span class="zh-CN">读取隐藏文件标记</span>
	 */
	public boolean isReadHiddenFiles() {
		return this.readHiddenFiles;
	}

	/**
	 * <h3 class="en-US">Setter method for the status of the read hidden file</h3>
	 * <h3 class="zh-CN">读取隐藏文件标记的Setter方法</h3>
	 *
	 * @param readHiddenFiles <span class="en-US">Status of the read hidden file</span>
	 *                        <span class="zh-CN">读取隐藏文件标记</span>
	 */
	public void setReadHiddenFiles(final boolean readHiddenFiles) {
		this.readHiddenFiles = readHiddenFiles;
	}

	/**
	 * <h3 class="en-US">Getter method for the encrypt/decrypt password</h3>
	 * <h3 class="zh-CN">加密/解密密码的Getter方法</h3>
	 *
	 * @return <span class="en-US">Encrypt/Decrypt password</span>
	 * <span class="zh-CN">加密/解密密码</span>
	 */
	public char[] getPassword() {
		return this.password;
	}

	/**
	 * <h3 class="en-US">Getter method for the AES key strength</h3>
	 * <h3 class="zh-CN">AES密钥强度的Getter方法</h3>
	 *
	 * @return <span class="en-US">AES key strength</span>
	 * <span class="zh-CN">AES密钥强度</span>
	 */
	public int getAesKeyStrength() {
		return this.aesKeyStrength;
	}

	/**
	 * <h3 class="en-US">Getter method for the include root folder flag</h3>
	 * <h3 class="zh-CN">包含根目录标记的Getter方法</h3>
	 *
	 * @return <span class="en-US">Include root folder flag</span>
	 * <span class="zh-CN">包含根目录标记</span>
	 */
	public boolean isIncludeRootFolder() {
		return this.includeRootFolder;
	}

	/**
	 * <h3 class="en-US">Setter method for the include root folder flag</h3>
	 * <h3 class="zh-CN">包含根目录标记的Setter方法</h3>
	 *
	 * @param includeRootFolder <span class="en-US">Include root folder flag</span>
	 *                          <span class="zh-CN">包含根目录标记</span>
	 */
	public void setIncludeRootFolder(final boolean includeRootFolder) {
		this.includeRootFolder = includeRootFolder;
	}

	/**
	 * <h3 class="en-US">Getter method for the root folder path</h3>
	 * <h3 class="zh-CN">根目录路径的Getter方法</h3>
	 *
	 * @return <span class="en-US">Root folder path</span>
	 * <span class="zh-CN">根目录路径</span>
	 */
	public String getRootFolderInZip() {
		return this.rootFolderInZip;
	}

	/**
	 * <h3 class="en-US">Setter method for the root folder path</h3>
	 * <h3 class="zh-CN">根目录路径的Setter方法</h3>
	 *
	 * @param rootFolderInZip <span class="en-US">Root folder path</span>
	 *                        <span class="zh-CN">根目录路径</span>
	 */
	public void setRootFolderInZip(final String rootFolderInZip) {
		if (!rootFolderInZip.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
			this.rootFolderInZip = rootFolderInZip + Globals.DEFAULT_PAGE_SEPARATOR;
		}
		this.rootFolderInZip =
				StringUtils.replace(rootFolderInZip, Globals.DEFAULT_PAGE_SEPARATOR, Globals.DEFAULT_ZIP_PAGE_SEPARATOR);
	}

	/**
	 * <h3 class="en-US">Getter method for the timezone setting</h3>
	 * <h3 class="zh-CN">时区设置的Getter方法</h3>
	 *
	 * @return <span class="en-US">Timezone setting</span>
	 * <span class="zh-CN">时区设置</span>
	 */
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	/**
	 * <h3 class="en-US">Setter method for the timezone setting</h3>
	 * <h3 class="zh-CN">时区设置的Setter方法</h3>
	 *
	 * @param timeZone <span class="en-US">Timezone setting</span>
	 *                 <span class="zh-CN">时区设置</span>
	 */
	public void setTimeZone(final TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * <h3 class="en-US">Getter method for the source file CRC result</h3>
	 * <h3 class="zh-CN">源文件CRC值的Getter方法</h3>
	 *
	 * @return <span class="en-US">Source file CRC result</span>
	 * <span class="zh-CN">源文件CRC值</span>
	 */
	public long getSourceFileCRC() {
		return this.sourceFileCRC;
	}

	/**
	 * <h3 class="en-US">Setter method for the source file CRC result</h3>
	 * <h3 class="zh-CN">源文件CRC值的Setter方法</h3>
	 *
	 * @param sourceFileCRC <span class="en-US">Source file CRC result</span>
	 *                      <span class="zh-CN">源文件CRC值</span>
	 */
	public void setSourceFileCRC(final long sourceFileCRC) {
		this.sourceFileCRC = sourceFileCRC;
	}

	/**
	 * <h3 class="en-US">Getter method for the default folder path</h3>
	 * <h3 class="zh-CN">默认目录路径的Getter方法</h3>
	 *
	 * @return <span class="en-US">Default folder path</span>
	 * <span class="zh-CN">默认目录路径</span>
	 */
	public String getDefaultFolderPath() {
		return this.defaultFolderPath;
	}

	/**
	 * <h3 class="en-US">Setter method for the default folder path</h3>
	 * <h3 class="zh-CN">默认目录路径的Setter方法</h3>
	 *
	 * @param defaultFolderPath <span class="en-US">Default folder path</span>
	 *                          <span class="zh-CN">默认目录路径</span>
	 */
	public void setDefaultFolderPath(final String defaultFolderPath) {
		this.defaultFolderPath = defaultFolderPath;
	}

	/**
	 * <h3 class="en-US">Getter method for the file entry path</h3>
	 * <h3 class="zh-CN">文件在压缩包内的路径的Getter方法</h3>
	 *
	 * @return <span class="en-US">File entry path</span>
	 * <span class="zh-CN">文件在压缩包内的路径</span>
	 */
	public String getFileNameInZip() {
		return this.fileNameInZip;
	}

	/**
	 * <h3 class="en-US">Setter method for the file entry path</h3>
	 * <h3 class="zh-CN">文件在压缩包内的路径的Setter方法</h3>
	 *
	 * @param fileNameInZip <span class="en-US">File entry path</span>
	 *                      <span class="zh-CN">文件在压缩包内的路径</span>
	 */
	public void setFileNameInZip(final String fileNameInZip) {
		this.fileNameInZip = fileNameInZip;
	}

	/**
	 * <h3 class="en-US">Getter method for the status of source external stream</h3>
	 * <h3 class="zh-CN">源外部流的标记的Getter方法</h3>
	 *
	 * @return <span class="en-US">Status of source external stream</span>
	 * <span class="zh-CN">源外部流的标记</span>
	 */
	public boolean isSourceExternalStream() {
		return this.sourceExternalStream;
	}

	/**
	 * <h3 class="en-US">Setter method for the status of source external stream</h3>
	 * <h3 class="zh-CN">源外部流的标记的Setter方法</h3>
	 *
	 * @param sourceExternalStream <span class="en-US">Status of source external stream</span>
	 *                             <span class="zh-CN">源外部流的标记</span>
	 */
	public void setSourceExternalStream(final boolean sourceExternalStream) {
		this.sourceExternalStream = sourceExternalStream;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
