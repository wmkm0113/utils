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

package org.nervousync.archive.zip;

import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.enumerations.*;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.commons.Globals;
import org.nervousync.exceptions.builder.BuilderException;
import org.nervousync.utils.commons.StringUtils;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * <h2 class="en-US">ZIP options</h2>
 * <h2 class="zh-CN">ZIP 选项</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 29, 2017 15:51:12 $
 */
public final class ZipOptions {

	/**
	 * <span class="en-US">Compression algorithm</span>
	 * <span class="zh-CN">压缩算法</span>
	 */
	private final CompressionAlgorithm algorithm;
	/**
	 * <span class="en-US">Compression level</span>
	 * <span class="zh-CN">压缩等级</span>
	 */
	private final int compressionLevel;
	/**
	 * <span class="en-US">Encrypt method code</span>
	 * <span class="zh-CN">文件加密方式代码</span>
	 */
	private final int encryptionMethod;
	/**
	 * <span class="en-US">Encryption algorithm code</span>
	 * <span class="zh-CN">加密算法代码</span>
	 */
	private final int encryptionAlgorithm;
	/**
	 * <span class="en-US">Key bit length</span>
	 * <span class="zh-CN">密钥比特长度</span>
	 */
	private final int keyBitLength;
	/**
	 * <span class="en-US">Strong encryption access mode</span>
	 * <span class="zh-CN">强加密访问模式</span>
	 */
	private final StrongAccessMode accessMode;
	/**
	 * <span class="en-US">Recipients certificate list</span>
	 * <span class="zh-CN">接收者证书列表</span>
	 */
	private final List<X509Certificate> entryRecipients;
	/**
	 * <span class="en-US">Compress central directory flag</span>
	 * <span class="zh-CN">压缩中央目录标记</span>
	 */
	private final boolean compressCentralDirectory;
	/**
	 * <span class="en-US">Central directory encryption algorithm</span>
	 * <span class="zh-CN">中央目录加密算法</span>
	 */
	private final int centralDirectoryEncryptionAlgorithm;
	/**
	 * <span class="en-US">Central directory encryption key bit length</span>
	 * <span class="zh-CN">中央目录加密密钥比特长度</span>
	 */
	private final int centralDirectoryKeyBitLength;
	/**
	 * <span class="en-US">Central directory strong encryption access mode</span>
	 * <span class="zh-CN">中央目录强加密访问模式</span>
	 */
	private final StrongAccessMode centralDirectoryAccessMode;
	/**
	 * <span class="en-US">Central directory recipients certificate list</span>
	 * <span class="zh-CN">中央目录接收者证书列表</span>
	 */
	private final List<X509Certificate> centralDirectoryRecipients;
	/**
	 * <span class="en-US">Central directory hash algorithm</span>
	 * <span class="zh-CN">中央目录哈希算法</span>
	 */
	private final int centralDirectoryHashAlgorithm;
	/**
	 * <span class="en-US">Status of the read hidden file</span>
	 * <span class="zh-CN">读取隐藏文件标记</span>
	 */
	private final boolean readHiddenFiles;
	/**
	 * <span class="en-US">AES key strength</span>
	 * <span class="zh-CN">AES密钥强度</span>
	 */
	private final int aesKeyStrength;
	/**
	 * <span class="en-US">AES vendor</span>
	 * <span class="zh-CN">AES 版本</span>
	 */
	private final AesVendor aesVendor;
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
	 * <span class="en-US">Enumeration value of the Zip64</span>
	 * <span class="zh-CN">Zip64模式枚举值</span>
	 */
	private final Zip64Mode zip64Mode;
	/**
	 * <span class="en-US">File entry path</span>
	 * <span class="zh-CN">文件在压缩包内的路径</span>
	 */
	private final String fileNameInZip;
	/**
	 * <span class="en-US">Archive is split file status</span>
	 * <span class="zh-CN">分卷压缩包标记</span>
	 */
	private final boolean splitArchive;
	/**
	 * <span class="en-US">Enumeration value of the archive split type</span>
	 * <span class="zh-CN">分卷类型枚举值</span>
	 */
	private final SplitType splitType;
	/**
	 * <span class="en-US">Maximum length of split item</span>
	 * <span class="zh-CN">分卷大小</span>
	 */
	private final long splitLength;
	/**
	 * <span class="en-US">Duplicate entry process policy</span>
	 * <span class="zh-CN">重复条目处理规则</span>
	 */
	private final DuplicatePolicy duplicatePolicy;
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
	 * <span class="en-US">Status of source external stream</span>
	 * <span class="zh-CN">源外部流的标记</span>
	 */
	private boolean sourceExternalStream = Boolean.FALSE;

	/**
	 * <h3 class="en-US">Private constructor method for the ZIP options</h3>
	 * <h3 class="zh-CN">ZIP 选项的私有构造方法</h3>
	 *
	 * @param builder <span class="en-US">ZIP options builder</span>
	 *                <span class="zh-CN">ZIP 选项构建器</span>
	 */
	private ZipOptions(final Builder builder) {
		this.algorithm = builder.algorithm;
		this.compressionLevel = builder.compressionLevel;
		this.encryptionMethod = builder.encryptionMethod;
		this.encryptionAlgorithm = builder.encryptionAlgorithm;
		this.keyBitLength = builder.keyBitLength;
		this.accessMode = builder.accessMode;
		this.entryRecipients = List.copyOf(builder.entryRecipients);
		this.compressCentralDirectory = builder.compressCentralDirectory;
		this.centralDirectoryEncryptionAlgorithm = builder.centralDirectoryEncryptionAlgorithm;
		this.centralDirectoryKeyBitLength = builder.centralDirectoryKeyBitLength;
		this.centralDirectoryAccessMode = builder.centralDirectoryAccessMode;
		this.centralDirectoryRecipients = List.copyOf(builder.centralDirectoryRecipients);
		this.centralDirectoryHashAlgorithm = builder.centralDirectoryHashAlgorithm;
		this.readHiddenFiles = builder.readHiddenFiles;
		this.aesKeyStrength = builder.aesKeyStrength;
		this.aesVendor = builder.aesVendor;
		this.includeRootFolder = builder.includeRootFolder;
		this.rootFolderInZip = builder.rootFolderInZip;
		this.timeZone = builder.timeZone;
		this.zip64Mode = builder.zip64Mode;
		this.fileNameInZip = builder.fileNameInZip;
		this.splitArchive = builder.splitArchive;
		this.splitType = builder.splitType;
		this.splitLength = builder.splitLength;
		this.duplicatePolicy = builder.duplicatePolicy;
	}

	/**
	 * <h3 class="en-US">Static method for initialize the builder instance object</h3>
	 * <h3 class="zh-CN">静态方法用于初始化构建器实例对象</h3>
	 *
	 * @return <span class="en-US">ZIP options instance object</span>
	 * <span class="zh-CN">ZIP压缩属性实例对象</span>
	 */
	public static Builder newBuilder() {
		return newBuilder(null);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the builder instance object</h3>
	 * <h3 class="zh-CN">静态方法用于初始化构建器实例对象</h3>
	 *
	 * @param zipOptions <span class="en-US">ZIP options</span>
	 *                   <span class="zh-CN">ZIP 选项</span>
	 * @return <span class="en-US">ZIP options instance object</span>
	 * <span class="zh-CN">ZIP压缩属性实例对象</span>
	 */
	public static Builder newBuilder(final ZipOptions zipOptions) {
		return new Builder(zipOptions);
	}

	/**
	 * <h3 class="en-US">Getter method for the compression algorithm</h3>
	 * <h3 class="zh-CN">压缩算法的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Compression algorithm</span>
	 * <span class="zh-CN">压缩算法</span>
	 */
	public CompressionAlgorithm getAlgorithm() {
		return this.algorithm;
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
	 * <h3 class="en-US">Getter method for the encryption algorithm code</h3>
	 * <h3 class="zh-CN">加密算法代码的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Encryption algorithm code</span>
	 * <span class="zh-CN">加密算法代码</span>
	 */
	public int getEncryptionAlgorithm() {
		return this.encryptionAlgorithm;
	}

	/**
	 * <h3 class="en-US">Getter method for the key bit length</h3>
	 * <h3 class="zh-CN">密钥比特长度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Key bit length</span>
	 * <span class="zh-CN">密钥比特长度</span>
	 */
	public int getKeyBitLength() {
		return this.keyBitLength;
	}

	/**
	 * <h3 class="en-US">Getter method for the strong encryption access mode</h3>
	 * <h3 class="zh-CN">强加密访问模式的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Strong encryption access mode</span>
	 * <span class="zh-CN">强加密访问模式</span>
	 */
	public StrongAccessMode getAccessMode() {
		return this.accessMode;
	}

	/**
	 * <h3 class="en-US">Getter method for the recipients certificate list</h3>
	 * <h3 class="zh-CN">接收者证书列表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Recipients certificate list</span>
	 * <span class="zh-CN">接收者证书列表</span>
	 */
	public List<X509Certificate> getEntryRecipients() {
		return this.entryRecipients;
	}

	/**
	 * <h3 class="en-US">Getter method for the compress central directory flag</h3>
	 * <h3 class="zh-CN">压缩中央目录标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Compress central directory flag</span>
	 * <span class="zh-CN">压缩中央目录标记</span>
	 */
	public boolean isCompressCentralDirectory() {
		return this.compressCentralDirectory;
	}

	/**
	 * <h3 class="en-US">Getter method for the central directory encryption algorithm</h3>
	 * <h3 class="zh-CN">中央目录加密算法的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Central directory encryption algorithm</span>
	 * <span class="zh-CN">中央目录加密算法</span>
	 */
	public int getCentralDirectoryEncryptionAlgorithm() {
		return this.centralDirectoryEncryptionAlgorithm;
	}

	/**
	 * <h3 class="en-US">Getter method for the central directory encryption key bit length</h3>
	 * <h3 class="zh-CN">中央目录加密密钥比特长度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Central directory encryption key bit length</span>
	 * <span class="zh-CN">中央目录加密密钥比特长度</span>
	 */
	public int getCentralDirectoryKeyBitLength() {
		return this.centralDirectoryKeyBitLength;
	}

	/**
	 * <h3 class="en-US">Getter method for the central directory strong encryption access mode</h3>
	 * <h3 class="zh-CN">中央目录强加密访问模式的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Central directory strong encryption access mode</span>
	 * <span class="zh-CN">中央目录强加密访问模式</span>
	 */
	public StrongAccessMode getCentralDirectoryAccessMode() {
		return this.centralDirectoryAccessMode;
	}

	/**
	 * <h3 class="en-US">Getter method for the central directory recipients certificate list</h3>
	 * <h3 class="zh-CN">中央目录接收者证书列表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Central directory recipients certificate list</span>
	 * <span class="zh-CN">中央目录接收者证书列表</span>
	 */
	public List<X509Certificate> getCentralDirectoryRecipients() {
		return this.centralDirectoryRecipients;
	}

	/**
	 * <h3 class="en-US">Getter method for the central directory hash algorithm</h3>
	 * <h3 class="zh-CN">中央目录哈希算法的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Central directory hash algorithm</span>
	 * <span class="zh-CN">中央目录哈希算法</span>
	 */
	public int getCentralDirectoryHashAlgorithm() {
		return this.centralDirectoryHashAlgorithm;
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
	 * <h3 class="en-US">Getter method for the AES vendor</h3>
	 * <h3 class="zh-CN">AES 版本的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">AES vendor</span>
	 * <span class="zh-CN">AES 版本</span>
	 */
	public AesVendor getAesVendor() {
		return this.aesVendor;
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
	 * <h3 class="en-US">Getter method for the enumeration value of the Zip64</h3>
	 * <h3 class="zh-CN">Zip64模式枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the Zip64</span>
	 * <span class="zh-CN">Zip64模式枚举值</span>
	 */
	public Zip64Mode getZip64Mode() {
		return this.zip64Mode;
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
	 * <h3 class="en-US">Getter method for the archive is split file status</h3>
	 * <h3 class="zh-CN">分卷压缩包标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Archive is split file status</span>
	 * <span class="zh-CN">分卷压缩包标记</span>
	 */
	public boolean isSplitArchive() {
		return this.splitArchive;
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the archive split type</h3>
	 * <h3 class="zh-CN">分卷类型枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the archive split type</span>
	 * <span class="zh-CN">分卷类型枚举值</span>
	 */
	public SplitType getSplitType() {
		return this.splitType;
	}

	/**
	 * <h3 class="en-US">Getter method for the maximum length of split item</h3>
	 * <h3 class="zh-CN">分卷大小的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum length of split item</span>
	 * <span class="zh-CN">分卷大小</span>
	 */
	public long getSplitLength() {
		return this.splitLength;
	}

	/**
	 * <h3 class="en-US">Getter method for the duplicate entry process policy</h3>
	 * <h3 class="zh-CN">重复条目处理规则的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Duplicate entry process policy</span>
	 * <span class="zh-CN">重复条目处理规则</span>
	 */
	public DuplicatePolicy getDuplicatePolicy() {
		return this.duplicatePolicy;
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
	 * <h2 class="en-US">ZIP options builder</h2>
	 * <h2 class="zh-CN">ZIP 选项构建器</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Nov 29, 2017 15:51:12 $
	 */
	public static final class Builder implements org.nervousync.builder.Builder<ZipOptions> {

		/**
		 * <span class="en-US">Compression algorithm</span>
		 * <span class="zh-CN">压缩算法</span>
		 */
		private CompressionAlgorithm algorithm = CompressionAlgorithm.DEFLATE;
		/**
		 * <span class="en-US">Compress level</span>
		 * <span class="zh-CN">压缩等级</span>
		 */
		private int compressionLevel = ZipGlobals.DEFLATE_LEVEL_NORMAL;
		/**
		 * <span class="en-US">Encrypt method code</span>
		 * <span class="zh-CN">文件加密方式代码</span>
		 */
		private int encryptionMethod = ZipGlobals.ENC_NO_ENCRYPTION;
		/**
		 * <span class="en-US">Encryption algorithm code</span>
		 * <span class="zh-CN">加密算法代码</span>
		 */
		private int encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_UNKNOWN;
		/**
		 * <span class="en-US">Key bit length</span>
		 * <span class="zh-CN">密钥比特长度</span>
		 */
		private int keyBitLength = Globals.DEFAULT_VALUE_INT;
		/**
		 * <span class="en-US">Strong encryption access mode</span>
		 * <span class="zh-CN">强加密访问模式</span>
		 */
		private StrongAccessMode accessMode = StrongAccessMode.PASSWORD;
		/**
		 * <span class="en-US">Recipients certificate list</span>
		 * <span class="zh-CN">接收者证书列表</span>
		 */
		private List<X509Certificate> entryRecipients = List.of();
		/**
		 * <span class="en-US">Compress central directory flag</span>
		 * <span class="zh-CN">压缩中央目录标记</span>
		 */
		private boolean compressCentralDirectory = false;
		/**
		 * <span class="en-US">Central directory encryption algorithm</span>
		 * <span class="zh-CN">中央目录加密算法</span>
		 */
		private int centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE;
		/**
		 * <span class="en-US">Central directory encryption key bit length</span>
		 * <span class="zh-CN">中央目录加密密钥比特长度</span>
		 */
		private int centralDirectoryKeyBitLength = Globals.DEFAULT_VALUE_INT;
		/**
		 * <span class="en-US">Central directory strong encryption access mode</span>
		 * <span class="zh-CN">中央目录强加密访问模式</span>
		 */
		private StrongAccessMode centralDirectoryAccessMode = StrongAccessMode.PASSWORD;
		/**
		 * <span class="en-US">Central directory recipients certificate list</span>
		 * <span class="zh-CN">中央目录接收者证书列表</span>
		 */
		private List<X509Certificate> centralDirectoryRecipients = List.of();
		/**
		 * <span class="en-US">Central directory hash algorithm</span>
		 * <span class="zh-CN">中央目录哈希算法</span>
		 */
		private int centralDirectoryHashAlgorithm = ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE;
		/**
		 * <span class="en-US">Status of the read hidden file</span>
		 * <span class="zh-CN">读取隐藏文件标记</span>
		 */
		private boolean readHiddenFiles = Boolean.TRUE;
		/**
		 * <span class="en-US">AES key strength</span>
		 * <span class="zh-CN">AES密钥强度</span>
		 */
		private int aesKeyStrength = Globals.DEFAULT_VALUE_INT;
		/**
		 * <span class="en-US">AES vendor</span>
		 * <span class="zh-CN">AES 版本</span>
		 */
		private AesVendor aesVendor = AesVendor.AE_1;
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
		 * <span class="en-US">Enumeration value of the Zip64</span>
		 * <span class="zh-CN">Zip64模式枚举值</span>
		 */
		private Zip64Mode zip64Mode = Zip64Mode.AUTO;
		/**
		 * <span class="en-US">File entry path</span>
		 * <span class="zh-CN">文件在压缩包内的路径</span>
		 */
		private String fileNameInZip = Globals.DEFAULT_VALUE_STRING;
		/**
		 * <span class="en-US">Archive is split file status</span>
		 * <span class="zh-CN">分卷压缩包标记</span>
		 */
		private boolean splitArchive = Boolean.FALSE;
		/**
		 * <span class="en-US">Enumeration value of the archive split type</span>
		 * <span class="zh-CN">分卷类型枚举值</span>
		 */
		private SplitType splitType = SplitType.DETECT;
		/**
		 * <span class="en-US">Maximum length of split item</span>
		 * <span class="zh-CN">分卷大小</span>
		 */
		private long splitLength = Globals.DEFAULT_VALUE_LONG;
		/**
		 * <span class="en-US">Duplicate entry process policy</span>
		 * <span class="zh-CN">重复条目处理规则</span>
		 */
		private DuplicatePolicy duplicatePolicy = DuplicatePolicy.LAST;

		/**
		 * <h3 class="en-US">Private constructor method for the ZIP options builder</h3>
		 * <h3 class="zh-CN">ZIP 选项构建器的私有构造方法</h3>
		 *
		 * @param zipOptions <span class="en-US">ZIP options</span>
		 *                   <span class="zh-CN">ZIP 选项</span>
		 */
		private Builder(final ZipOptions zipOptions) {
			if (zipOptions != null) {
				this.algorithm = zipOptions.algorithm;
				this.compressionLevel = zipOptions.compressionLevel;
				this.encryptionMethod = zipOptions.encryptionMethod;
				this.encryptionAlgorithm = zipOptions.encryptionAlgorithm;
				this.keyBitLength = zipOptions.keyBitLength;
				this.accessMode = zipOptions.accessMode;
				this.entryRecipients = List.copyOf(zipOptions.entryRecipients);
				this.compressCentralDirectory = zipOptions.compressCentralDirectory;
				this.centralDirectoryEncryptionAlgorithm = zipOptions.centralDirectoryEncryptionAlgorithm;
				this.centralDirectoryKeyBitLength = zipOptions.centralDirectoryKeyBitLength;
				this.centralDirectoryAccessMode = zipOptions.centralDirectoryAccessMode;
				this.centralDirectoryRecipients = List.copyOf(zipOptions.centralDirectoryRecipients);
				this.centralDirectoryHashAlgorithm = zipOptions.centralDirectoryHashAlgorithm;
				this.readHiddenFiles = zipOptions.readHiddenFiles;
				this.aesKeyStrength = zipOptions.aesKeyStrength;
				this.aesVendor = zipOptions.aesVendor;
				this.includeRootFolder = zipOptions.includeRootFolder;
				this.rootFolderInZip = zipOptions.rootFolderInZip;
				this.timeZone = zipOptions.timeZone;
				this.zip64Mode = zipOptions.zip64Mode;
				this.fileNameInZip = zipOptions.fileNameInZip;
				this.splitArchive = zipOptions.splitArchive;
				this.splitType = zipOptions.splitType;
				this.splitLength = zipOptions.splitLength;
				this.duplicatePolicy = zipOptions.duplicatePolicy;
			}
		}

		/**
		 * <h3 class="en-US">Configure the compression algorithm</h3>
		 * <h3 class="zh-CN">设置压缩算法</h3>
		 *
		 * @param algorithm <span class="en-US">Compression algorithm</span>
		 *                  <span class="zh-CN">压缩算法</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder compressionAlgorithm(final CompressionAlgorithm algorithm) {
			Objects.requireNonNull(algorithm, "algorithm");
			this.algorithm = algorithm;
			return this;
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
		public Builder compressionLevel(final int compressionLevel) {
			if (compressionLevel < 0 || compressionLevel > 9) {
				throw new IllegalArgumentException("compressionLevel must be between 0 and 9");
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
			return this;
		}

		/**
		 * <h3 class="en-US">Use the standard encrypt/decrypt</h3>
		 * <h3 class="zh-CN">使用标准加密解密</h3>
		 *
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder standardCrypto() {
			this.aesKeyStrength = Globals.DEFAULT_VALUE_INT;
			this.encryptionMethod = ZipGlobals.ENC_METHOD_STANDARD;
			return this;
		}

		/**
		 * <h3 class="en-US">Use the AES encrypt/decrypt</h3>
		 * <h3 class="zh-CN">使用 AES 加密解密</h3>
		 *
		 * @param aesKeyLength <span class="en-US">AES key length</span>
		 *                     <span class="zh-CN">AES密钥长度</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder aesCrypto(final int aesKeyLength) throws IOException {
			return this.aesCrypto(aesKeyLength, AesVendor.AE_2);
		}

		/**
		 * <h3 class="en-US">Use the AES encrypt/decrypt</h3>
		 * <h3 class="zh-CN">使用 AES 加密解密</h3>
		 *
		 * @param aesKeyLength <span class="en-US">AES key length</span>
		 *                     <span class="zh-CN">AES密钥长度</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder aesCrypto(final int aesKeyLength, final AesVendor aesVendor) throws IOException {
			Objects.requireNonNull(aesVendor, "aesVendor");
			switch (aesKeyLength) {
				case 128:
					this.aesKeyStrength = ZipGlobals.AES_STRENGTH_128;
					break;
				case 192:
					this.aesKeyStrength = ZipGlobals.AES_STRENGTH_192;
					break;
				case 256:
					this.aesKeyStrength = ZipGlobals.AES_STRENGTH_256;
					break;
				default:
					throw new ArchiveException(0x0000001B0005L);
			}
			this.encryptionMethod = ZipGlobals.ENC_METHOD_AES;
			this.aesVendor = aesVendor;
			return this;
		}

		/**
		 * <h3 class="en-US">Use the strong encrypt/decrypt</h3>
		 * <h3 class="zh-CN">使用强加密解密</h3>
		 *
		 * @param encryptionAlgorithm <span class="en-US">Encryption algorithm code</span>
		 *                            <span class="zh-CN">加密算法代码</span>
		 * @param keyBitLength        <span class="en-US">Key bit length</span>
		 *                            <span class="zh-CN">密钥比特长度</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder strongCrypto(final int encryptionAlgorithm, final int keyBitLength)
				throws IOException {
			return this.strongCrypto(encryptionAlgorithm, keyBitLength, StrongAccessMode.PASSWORD, List.of());
		}

		/**
		 * <h3 class="en-US">Use the strong encrypt/decrypt</h3>
		 * <h3 class="zh-CN">使用强加密解密</h3>
		 *
		 * @param encryptionAlgorithm <span class="en-US">Encryption algorithm code</span>
		 *                            <span class="zh-CN">加密算法代码</span>
		 * @param keyBitLength        <span class="en-US">Key bit length</span>
		 *                            <span class="zh-CN">密钥比特长度</span>
		 * @param accessMode          <span class="en-US">Strong encryption access mode</span>
		 *                            <span class="zh-CN">强加密访问模式</span>
		 * @param recipients          <span class="en-US">Recipients certificate list</span>
		 *                            <span class="zh-CN">接收者证书列表</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder strongCrypto(final int encryptionAlgorithm, final int keyBitLength,
		                            final StrongAccessMode accessMode, final List<X509Certificate> recipients)
				throws IOException {
			switch (encryptionAlgorithm) {
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112;
					this.keyBitLength = 112;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168;
					this.keyBitLength = 168;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128;
					this.keyBitLength = 128;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192;
					this.keyBitLength = 192;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256;
					this.keyBitLength = 256;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH;
					this.keyBitLength = keyBitLength;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES;
					this.keyBitLength = 64;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW;
					this.keyBitLength = keyBitLength;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_OLD:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_OLD;
					this.keyBitLength = keyBitLength;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4;
					this.keyBitLength = keyBitLength;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH:
					this.encryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH;
					this.keyBitLength = keyBitLength;
					break;
				default:
					throw new ArchiveException(0x0000001A0023L);
			}
			this.encryptionMethod = ZipGlobals.ENC_METHOD_STRONG;
			this.accessMode = accessMode;
			this.entryRecipients = List.copyOf(recipients);
			return this;
		}

		/**
		 * <h3 class="en-US">Enable central directory compress</h3>
		 * <h3 class="zh-CN">启用中央目录压缩</h3>
		 *
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder enableCentralDirectoryCompress() {
			this.compressCentralDirectory = true;
			return this;
		}

		/**
		 * <h3 class="en-US">Disable central directory compress</h3>
		 * <h3 class="zh-CN">禁用中央目录压缩</h3>
		 *
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder disableCentralDirectoryCompress() {
			this.compressCentralDirectory = false;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the crypto of the central directory</h3>
		 * <h3 class="zh-CN">设置中央目录加密</h3>
		 *
		 * @param encryptionAlgorithm <span class="en-US">Encryption algorithm code</span>
		 *                            <span class="zh-CN">加密算法代码</span>
		 * @param keyBitLength        <span class="en-US">Key bit length</span>
		 *                            <span class="zh-CN">密钥比特长度</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder centralDirectoryCrypto(final int encryptionAlgorithm, final int keyBitLength)
				throws IOException {
			return this.centralDirectoryCrypto(encryptionAlgorithm, keyBitLength, StrongAccessMode.PASSWORD, List.of());
		}

		/**
		 * <h3 class="en-US">Setting the crypto of the central directory</h3>
		 * <h3 class="zh-CN">设置中央目录加密</h3>
		 *
		 * @param encryptionAlgorithm <span class="en-US">Encryption algorithm code</span>
		 *                            <span class="zh-CN">加密算法代码</span>
		 * @param keyBitLength        <span class="en-US">Key bit length</span>
		 *                            <span class="zh-CN">密钥比特长度</span>
		 * @param accessMode          <span class="en-US">Strong encryption access mode</span>
		 *                            <span class="zh-CN">强加密访问模式</span>
		 * @param recipients          <span class="en-US">Recipients certificate list</span>
		 *                            <span class="zh-CN">接收者证书列表</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder centralDirectoryCrypto(final int encryptionAlgorithm, final int keyBitLength,
		                                      final StrongAccessMode accessMode, final List<X509Certificate> recipients)
				throws IOException {
			switch (encryptionAlgorithm) {
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112;
					this.centralDirectoryKeyBitLength = 112;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168;
					this.centralDirectoryKeyBitLength = 168;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128;
					this.centralDirectoryKeyBitLength = 128;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192;
					this.centralDirectoryKeyBitLength = 192;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256;
					this.centralDirectoryKeyBitLength = 256;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH;
					this.centralDirectoryKeyBitLength = keyBitLength;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES;
					this.centralDirectoryKeyBitLength = 64;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW;
					this.centralDirectoryKeyBitLength = keyBitLength;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_OLD:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_OLD;
					this.centralDirectoryKeyBitLength = keyBitLength;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4;
					this.centralDirectoryKeyBitLength = keyBitLength;
					break;
				case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH:
					this.centralDirectoryEncryptionAlgorithm = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH;
					this.centralDirectoryKeyBitLength = keyBitLength;
					break;
				default:
					throw new ArchiveException(0x0000001A0023L);
			}
			this.centralDirectoryAccessMode = accessMode;
			this.centralDirectoryRecipients = List.copyOf(recipients);
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the hash algorithm of the central directory</h3>
		 * <h3 class="zh-CN">设置中央目录哈希算法</h3>
		 *
		 * @param hashAlgorithm <span class="en-US">Hash algorithm code</span>
		 *                      <span class="zh-CN">哈希算法代码</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder centralDirectoryHash(final int hashAlgorithm) {
			this.centralDirectoryHashAlgorithm = hashAlgorithm;
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
			if (!rootFolderInZip.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
				this.rootFolderInZip = rootFolderInZip + Globals.DEFAULT_PAGE_SEPARATOR;
			}
			this.rootFolderInZip =
					StringUtils.replace(rootFolderInZip, Globals.DEFAULT_PAGE_SEPARATOR, ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR);
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
		 * <h3 class="en-US">Configure the archive is split file flag</h3>
		 * <h3 class="zh-CN">设置分卷压缩包标记</h3>
		 *
		 * @param splitArchive <span class="en-US">Archive is split file status</span>
		 *                     <span class="zh-CN">分卷压缩包标记</span>
		 * @param splitLength  <span class="en-US">Maximum length of split item</span>
		 *                     <span class="zh-CN">分卷大小</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder splitArchive(final boolean splitArchive, final long splitLength) {
			if (splitArchive) {
				this.splitArchive = Boolean.TRUE;
				this.splitLength = splitLength;
			} else {
				this.splitArchive = Boolean.FALSE;
				this.splitLength = Globals.DEFAULT_VALUE_LONG;
			}
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the archive split type</h3>
		 * <h3 class="zh-CN">设置分卷类型</h3>
		 *
		 * @param splitType <span class="en-US">Enumeration value of the archive split type</span>
		 *                  <span class="zh-CN">分卷类型枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder splitType(final SplitType splitType) {
			if (this.splitArchive) {
				this.splitType = splitType;
			} else {
				this.splitType = SplitType.DETECT;
			}
			return this;
		}

		/**
		 * <h3 class="en-US">Configure the duplicate entry process policy</h3>
		 * <h3 class="zh-CN">设置重复条目处理规则</h3>
		 *
		 * @param duplicatePolicy <span class="en-US">Duplicate entry process policy</span>
		 *                        <span class="zh-CN">重复条目处理规则</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder duplicatePolicy(final DuplicatePolicy duplicatePolicy) {
			this.duplicatePolicy = duplicatePolicy;
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
