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

import org.nervousync.commons.Globals;
import org.nervousync.commons.zip.ZipGlobals;
import org.nervousync.enumerations.zip.CompressionLevel;
import org.nervousync.enumerations.zip.CompressionType;
import org.nervousync.enumerations.zip.Zip64Mode;
import org.nervousync.exceptions.builder.BuilderException;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.utils.core.StringUtils;

import java.io.Serializable;
import java.util.TimeZone;

/**
 * <h2 class="en-US">ZIP options</h2>
 * <h2 class="zh-CN">ZIP压缩属性</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 29, 2017 15:51:12 $
 */
public final class ZipOptions implements Serializable {

	/**
	 * <span class="en-US">Folder, file name and comment charset encoding</span>
	 * <span class="zh-CN">目录名、文件名、备注信息的字符集编码</span>
	 */
	private final String charsetEncoding;
	/**
	 * <span class="en-US">Compress method</span>
	 * <span class="zh-CN">压缩方式</span>
	 */
	private final int compressionMethod;
	/**
	 * <span class="en-US">Compress level</span>
	 * <span class="zh-CN">压缩等级</span>
	 */
	private final int compressionLevel;
	/**
	 * <span class="en-US">Encrypt files status</span>
	 * <span class="zh-CN">文件加密标记</span>
	 */
	private final boolean encryptFiles;
	/**
	 * <span class="en-US">Encrypt method code</span>
	 * <span class="zh-CN">文件加密方式代码</span>
	 */
	private final int encryptionMethod;
	/**
	 * <span class="en-US">Status of the read hidden file</span>
	 * <span class="zh-CN">读取隐藏文件标记</span>
	 */
	private final boolean readHiddenFiles;
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
	private final boolean includeRootFolder;
	/**
	 * <span class="en-US">Root folder path</span>
	 * <span class="zh-CN">根目录路径</span>
	 */
	private final String rootFolderInZip;
	/**
	 * <span class="en-US">Timezone setting</span>
	 * <span class="zh-CN">时区设置</span>
	 *
	 * @see TimeZone
	 */
	private final TimeZone timeZone;
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
	private final String fileNameInZip;
	/**
	 * <span class="en-US">Status of source external stream</span>
	 * <span class="zh-CN">源外部流的标记</span>
	 */
	private boolean sourceExternalStream = Boolean.FALSE;
	/**
	 * <span class="en-US">Enumeration value of the Zip64</span>
	 * <span class="zh-CN">Zip64模式枚举值</span>
	 */
	private final Zip64Mode zip64Mode;

	/**
	 * <h3 class="en-US">Private constructor method for ZIP options</h3>
	 * <h3 class="zh-CN">ZIP压缩属性的私有构造方法</h3>
	 *
	 * @param builder <span class="en-US">Builder instance object</span>
	 *                <span class="zh-CN">构建器实例对象</span>
	 */
	private ZipOptions(final Builder builder) {
		this.charsetEncoding = builder.charsetEncoding;
		this.compressionMethod = builder.compressionMethod;
		this.compressionLevel = builder.compressionLevel;
		this.encryptFiles = builder.encryptFiles;
		this.encryptionMethod = builder.encryptionMethod;
		this.readHiddenFiles = builder.readHiddenFiles;
		this.password = builder.password;
		this.aesKeyStrength = builder.aesKeyStrength;
		this.includeRootFolder = builder.includeRootFolder;
		this.rootFolderInZip = builder.rootFolderInZip;
		this.timeZone = builder.timeZone;
		this.fileNameInZip = builder.fileNameInZip;
		this.zip64Mode = builder.zip64Mode;
	}

	/**
	 * <h3 class="en-US">Static method for initialize the builder instance object</h3>
	 * <h3 class="zh-CN">静态方法用于初始化构建器实例对象</h3>
	 *
	 * @return <span class="en-US">ZIP options instance object</span>
	 * <span class="zh-CN">ZIP压缩属性实例对象</span>
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * <h3 class="en-US">Static method for generate the default ZIP options</h3>
	 * <h3 class="zh-CN">静态方法用于生成默认的ZIP压缩属性</h3>
	 *
	 * @return <span class="en-US">ZIP options instance object</span>
	 * <span class="zh-CN">ZIP压缩属性实例对象</span>
	 */
	public static ZipOptions newOptions() {
		return newBuilder().build();
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
		return newBuilder().standardCrypto(password).build();
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
		return newBuilder().aesCrypto(password, aesKeyLength).build();
	}

	/**
	 * <h3 class="en-US">Getter method for the folder, file name, and comment charset encoding</h3>
	 * <h3 class="zh-CN">目录名、文件名、备注信息的字符集编码的Getter方法</h3>
	 *
	 * @return <span class="en-US">Folder, file name and comment charset encoding</span>
	 * <span class="zh-CN">目录名、文件名、备注信息的字符集编码</span>
	 */
	public String getCharsetEncoding() {
		return this.charsetEncoding;
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

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the Zip64</h3>
	 * <h3 class="zh-CN">Zip64模式枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the Zip64</span>
	 * <span class="zh-CN">Zip64模式枚举值</span>
	 */
	public Zip64Mode getZip64Mode() {
		return this.zip64Mode;
	}

	public Builder cloneBuilder() {
		return new Builder(this);
	}

	public static final class Builder implements org.nervousync.builder.Builder<ZipOptions> {

		/**
		 * <span class="en-US">Folder, file name and comment charset encoding</span>
		 * <span class="zh-CN">目录名、文件名、备注信息的字符集编码</span>
		 */
		private String charsetEncoding = Globals.DEFAULT_SYSTEM_CHARSET;
		/**
		 * <span class="en-US">Compress method</span>
		 * <span class="zh-CN">压缩方式</span>
		 */
		private int compressionMethod = ZipGlobals.COMP_DEFLATE;
		/**
		 * <span class="en-US">Compress level</span>
		 * <span class="zh-CN">压缩等级</span>
		 */
		private int compressionLevel = ZipGlobals.DEFLATE_LEVEL_NORMAL;
		/**
		 * <span class="en-US">Encrypt files status</span>
		 * <span class="zh-CN">文件加密标记</span>
		 */
		private boolean encryptFiles = Boolean.FALSE;
		/**
		 * <span class="en-US">Encrypt method code</span>
		 * <span class="zh-CN">文件加密方式代码</span>
		 */
		private int encryptionMethod = ZipGlobals.ENC_NO_ENCRYPTION;
		/**
		 * <span class="en-US">Encrypt/Decrypt password</span>
		 * <span class="zh-CN">加密/解密密码</span>
		 */
		private char[] password;
		/**
		 * <span class="en-US">AES key strength</span>
		 * <span class="zh-CN">AES密钥强度</span>
		 */
		private int aesKeyStrength = Globals.DEFAULT_VALUE_INT;
		/**
		 * <span class="en-US">Status of the read hidden file</span>
		 * <span class="zh-CN">读取隐藏文件标记</span>
		 */
		private boolean readHiddenFiles = Boolean.TRUE;
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
		 * @see TimeZone
		 */
		private TimeZone timeZone = TimeZone.getDefault();
		/**
		 * <span class="en-US">File entry path</span>
		 * <span class="zh-CN">文件在压缩包内的路径</span>
		 */
		private String fileNameInZip = null;
		/**
		 * <span class="en-US">Enumeration value of the Zip64</span>
		 * <span class="zh-CN">Zip64模式枚举值</span>
		 */
		private Zip64Mode zip64Mode = Zip64Mode.AUTO;

		private Builder() {
		}

		private Builder(final ZipOptions zipOptions) {
			this.charsetEncoding = zipOptions.charsetEncoding;
			this.compressionMethod = zipOptions.compressionMethod;
			this.compressionLevel = zipOptions.compressionLevel;
			this.encryptFiles = zipOptions.encryptFiles;
			this.encryptionMethod = zipOptions.encryptionMethod;
			this.readHiddenFiles = zipOptions.readHiddenFiles;
			this.password = zipOptions.password;
			this.aesKeyStrength = zipOptions.aesKeyStrength;
			this.includeRootFolder = zipOptions.includeRootFolder;
			this.rootFolderInZip = zipOptions.rootFolderInZip;
			this.timeZone = zipOptions.timeZone;
			this.fileNameInZip = zipOptions.fileNameInZip;
			this.zip64Mode = zipOptions.zip64Mode;
		}

		/**
		 * <h3 class="en-US">Configure the character encoding</h3>
		 * <h3 class="zh-CN">设置字符编码集</h3>
		 *
		 * @param charsetEncoding <span class="en-US">The character encoding</span>
		 *                        <span class="zh-CN">字符编码集</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder characterEncoding(final String charsetEncoding) {
			this.charsetEncoding = charsetEncoding;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the compression type</h3>
		 * <h3 class="zh-CN">设置压缩类型</h3>
		 *
		 * @param compressionType <span class="en-US">Enumeration value of the compression type</span>
		 *                        <span class="zh-CN">压缩类型枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder compressionType(final CompressionType compressionType) {
			this.compressionMethod = switch (compressionType) {
				case NONE -> ZipGlobals.COMP_STORE;
				case DEFLATE -> ZipGlobals.COMP_DEFLATE;
				case BZIP2 -> ZipGlobals.COMP_BZIP2;
				case LZMA -> ZipGlobals.COMP_LZMA;
			};
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the compression level</h3>
		 * <h3 class="zh-CN">设置压缩等级</h3>
		 *
		 * @param compressionLevel <span class="en-US">Enumeration value of the compression level</span>
		 *                         <span class="zh-CN">压缩等级枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder compressionLevel(final CompressionLevel compressionLevel) {
			return switch (compressionLevel) {
				case FASTEST -> compressionLevel(ZipGlobals.DEFLATE_LEVEL_FASTEST);
				case FAST -> compressionLevel(ZipGlobals.DEFLATE_LEVEL_FAST);
				case NORMAL -> compressionLevel(ZipGlobals.DEFLATE_LEVEL_NORMAL);
				case MAXIMUM -> compressionLevel(ZipGlobals.DEFLATE_LEVEL_MAXIMUM);
				case ULTRA -> compressionLevel(ZipGlobals.DEFLATE_LEVEL_ULTRA);
			};
		}

		/**
		 * <h3 class="en-US">Configure the compression level</h3>
		 * <h3 class="zh-CN">设置压缩等级</h3>
		 *
		 * @param compressionLevel <span class="en-US">Compression level</span>
		 *                         <span class="zh-CN">压缩等级</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder compressionLevel(final int compressionLevel) throws ZipException {
			if (compressionLevel < ZipGlobals.DEFLATE_LEVEL_FASTEST
					|| compressionLevel > ZipGlobals.DEFLATE_LEVEL_ULTRA) {
				throw new ZipException(0x0000001B004BL);
			}
			this.compressionLevel = compressionLevel;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the no encrypt/decrypt</h3>
		 * <h3 class="zh-CN">设置不加密解密</h3>
		 *
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder noCrypto() {
			this.aesKeyStrength = Globals.DEFAULT_VALUE_INT;
			this.encryptionMethod = ZipGlobals.ENC_NO_ENCRYPTION;
			this.password = new char[0];
			this.encryptFiles = Boolean.FALSE;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the standard encrypt/decrypt</h3>
		 * <h3 class="zh-CN">设置标准加密解密</h3>
		 *
		 * @param password <span class="en-US">Encrypt/Decrypt password</span>
		 *                 <span class="zh-CN">加密/解密密码</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder standardCrypto(final String password) throws ZipException {
			if (StringUtils.isEmpty(password)) {
				throw new ZipException(0x0000001B0006L);
			}
			this.aesKeyStrength = Globals.DEFAULT_VALUE_INT;
			this.encryptionMethod = ZipGlobals.ENC_METHOD_STANDARD;
			this.password = password.toCharArray();
			this.encryptFiles = Boolean.TRUE;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the AES encrypt/decrypt</h3>
		 * <h3 class="zh-CN">设置 AES 加密解密</h3>
		 *
		 * @param password     <span class="en-US">Encrypt/Decrypt password</span>
		 *                     <span class="zh-CN">加密/解密密码</span>
		 * @param aesKeyLength <span class="en-US">AES key length</span>
		 *                     <span class="zh-CN">AES密钥长度</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder aesCrypto(final String password, final int aesKeyLength) throws ZipException {
			if (StringUtils.isEmpty(password)) {
				throw new ZipException(0x0000001B0006L);
			}
			this.aesKeyStrength = switch (aesKeyLength) {
				case 128 -> ZipGlobals.AES_STRENGTH_128;
				case 192 -> ZipGlobals.AES_STRENGTH_192;
				case 256 -> ZipGlobals.AES_STRENGTH_256;
				default -> throw new ZipException(0x0000001B0005L);
			};
			this.encryptionMethod = ZipGlobals.ENC_METHOD_AES;
			this.password = password.toCharArray();
			this.encryptFiles = Boolean.TRUE;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the status of the read hidden file</h3>
		 * <h3 class="zh-CN">设置读取隐藏文件标记</h3>
		 *
		 * @param readHiddenFiles <span class="en-US">Status of the read hidden file</span>
		 *                        <span class="zh-CN">读取隐藏文件标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder readHiddenFiles(final boolean readHiddenFiles) {
			this.readHiddenFiles = readHiddenFiles;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the status of include root folder</h3>
		 * <h3 class="zh-CN">设置包含根目录标记</h3>
		 *
		 * @param includeRootFolder <span class="en-US">Include root folder</span>
		 *                          <span class="zh-CN">包含根目录标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder includeRootFolder(final boolean includeRootFolder) {
			this.includeRootFolder = includeRootFolder;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the root folder path</h3>
		 * <h3 class="zh-CN">设置根目录路径</h3>
		 *
		 * @param rootFolderInZip <span class="en-US">Root folder path</span>
		 *                        <span class="zh-CN">根目录路径</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder rootFolderInZip(final String rootFolderInZip) {
			this.rootFolderInZip = rootFolderInZip;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the timezone</h3>
		 * <h3 class="zh-CN">设置时区</h3>
		 *
		 * @param timeZone <span class="en-US">Timezone</span>
		 *                 <span class="zh-CN">时区</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder timezone(final TimeZone timeZone) {
			this.timeZone = timeZone;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the file entry path</h3>
		 * <h3 class="zh-CN">设置文件在压缩包内的路径</h3>
		 *
		 * @param fileNameInZip <span class="en-US">File entry path</span>
		 *                      <span class="zh-CN">文件在压缩包内的路径</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder fileNameInZip(final String fileNameInZip) {
			this.fileNameInZip = fileNameInZip;
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the ZIP64 mode</h3>
		 * <h3 class="zh-CN">设置 ZIP64 模式</h3>
		 *
		 * @param zip64Mode <span class="en-US">Enumeration value of the ZIP64 mode</span>
		 *                  <span class="zh-CN">ZIP64 模式</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder zip64Mode(final Zip64Mode zip64Mode) {
			this.zip64Mode = zip64Mode;
			return this;
		}

		@Override
		public ZipOptions build() throws BuilderException {
			return new ZipOptions(this);
		}
	}
}
