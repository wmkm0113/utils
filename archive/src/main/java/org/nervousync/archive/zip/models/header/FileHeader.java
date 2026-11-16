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

package org.nervousync.archive.zip.models.header;

import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.enumerations.EntryType;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.models.EntryTimes;
import org.nervousync.archive.zip.models.ExtraDataRecord;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.models.crypto.EncryptionInfo;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * <h2 class="en-US">Abstract class of the file header</h2>
 * <h2 class="zh-CN">文件头抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public abstract class FileHeader {

	/**
	 * <span class="en-US">UNIX system code</span>
	 * <span class="zh-CN">UNIX 主机值</span>
	 */
	private static final int HOST_UNIX = 3;
	/**
	 * <span class="en-US">macOS system code</span>
	 * <span class="zh-CN">macOS 主机值</span>
	 */
	private static final int HOST_MAC_OS_X = 19;
	/**
	 * <span class="en-US">PKWARE UNIX extra field header id</span>
	 * <span class="zh-CN">PKWARE UNIX 扩展属性头 ID</span>
	 */
	private static final int PKWARE_UNIX_EXTRA_FIELD = 0x000D;
	/**
	 * <span class="en-US">PKWARE UNIX extra field length</span>
	 * <span class="zh-CN">PKWARE UNIX 扩展属性长度</span>
	 */
	private static final int PKWARE_UNIX_FIXED_LENGTH = 12;
	/**
	 * <span class="en-US">UNIX file type mask</span>
	 * <span class="zh-CN">UNIX 文件类型掩码</span>
	 */
	private static final int UNIX_FILE_TYPE_MASK = 0xF000;
	/**
	 * <span class="en-US">UNIX regular file</span>
	 * <span class="zh-CN">UNIX 文件</span>
	 */
	private static final int UNIX_REGULAR_FILE = 0x8000;
	/**
	 * <span class="en-US">UNIX Directory</span>
	 * <span class="zh-CN">UNIX 目录</span>
	 */
	private static final int UNIX_DIRECTORY = 0x4000;
	/**
	 * <span class="en-US">UNIX symbolic link</span>
	 * <span class="zh-CN">UNIX 软连接</span>
	 */
	private static final int UNIX_SYMBOLIC_LINK = 0xA000;
	/**
	 * <span class="en-US">Encrypted flag mask</span>
	 * <span class="zh-CN">加密位掩码</span>
	 */
	private static final int FLAG_ENCRYPTED = 0x0001;
	/**
	 * <span class="en-US">Strong encrypted flag mask</span>
	 * <span class="zh-CN">强加密位掩码</span>
	 */
	private static final int FLAG_STRONG_ENCRYPTED = 0x0040;
	/**
	 * <span class="en-US">Data descriptor flag mask</span>
	 * <span class="zh-CN">数据描述器掩码</span>
	 */
	private static final int FLAG_DATA_DESCRIPTOR = 0x0008;
	/**
	 * <span class="en-US">UTF8 flag mask</span>
	 * <span class="zh-CN">UTF8 标记掩码</span>
	 */
	private static final int FLAG_UTF8 = 0x0800;

	/**
	 * <span class="en-US">Record type identifier code</span>
	 * <span class="zh-CN">记录类型标识</span>
	 */
	private int signature;
	/**
	 * <span class="en-US">Made version information</span>
	 * <span class="zh-CN">创建版本信息</span>
	 */
	private int madeVersion;
	/**
	 * <span class="en-US">Minimum ZIP specification version required</span>
	 * <span class="zh-CN">需要的最低 ZIP 规范版本</span>
	 */
	private int extractNeeded;
	/**
	 * <span class="en-US">General purpose flag</span>
	 * <span class="zh-CN">通用用途位标记</span>
	 */
	private int generalPurpose;
	/**
	 * <span class="en-US">Compression method code</span>
	 * <span class="zh-CN">压缩方法编号</span>
	 */
	private int compressionMethod;
	/**
	 * <span class="en-US">External file attribute information</span>
	 * <span class="zh-CN">文件系统属性信息</span>
	 */
	private long externalFileAttr;
	/**
	 * <span class="en-US">Last modified time</span>
	 * <span class="zh-CN">最后修改时间</span>
	 */
	private int lastModifiedTime;
	/**
	 * <span class="en-US">Last modified date</span>
	 * <span class="zh-CN">最后修改日期</span>
	 */
	private int lastModifiedDate;
	/**
	 * <span class="en-US">CRC32 value of the original value</span>
	 * <span class="zh-CN">原始数据的 CRC32 值</span>
	 */
	private long crc32;
	/**
	 * <span class="en-US">Compressed data size</span>
	 * <span class="zh-CN">压缩后数据大小</span>
	 */
	private long compressedSize;
	/**
	 * <span class="en-US">Uncompressed data size</span>
	 * <span class="zh-CN">未压缩数据大小</span>
	 */
	private long uncompressedSize;
	/**
	 * <span class="en-US">Entry path</span>
	 * <span class="zh-CN">条目路径</span>
	 */
	private String entryPath;
	/**
	 * <span class="en-US">The data length of the entry name</span>
	 * <span class="zh-CN">条目名称的字节长度</span>
	 */
	private int nameLength;
	/**
	 * <span class="en-US">Extra field length</span>
	 * <span class="zh-CN">扩展属性长度</span>
	 */
	private int extraFieldLength;
	/**
	 * <span class="en-US">Extra data record list</span>
	 * <span class="zh-CN">扩展数据记录列表</span>
	 */
	private List<ExtraDataRecord> extraDataRecords = null;
	/**
	 * <span class="en-US">Encryption information</span>
	 * <span class="zh-CN">加密信息</span>
	 */
	private EncryptionInfo encryptionInfo;

	/**
	 * <h3 class="en-US">Constructor method for the abstract class of the file header</h3>
	 * <h3 class="zh-CN">文件头抽象类的构造方法</h3>
	 */
	protected FileHeader() {
	}

	/**
	 * <h3 class="en-US">Constructor method for the abstract class of the file header</h3>
	 * <h3 class="zh-CN">文件头抽象类的构造方法</h3>
	 *
	 * @param fileHeader <span class="en-US">File header instance object</span>
	 *                   <span class="zh-CN">文件头实例对象</span>
	 */
	protected FileHeader(final FileHeader fileHeader) {
		this.signature = fileHeader.signature;
		this.madeVersion = fileHeader.madeVersion;
		this.extractNeeded = fileHeader.extractNeeded;
		this.generalPurpose = fileHeader.generalPurpose;
		this.compressionMethod = fileHeader.compressionMethod;
		this.externalFileAttr = fileHeader.externalFileAttr;
		this.lastModifiedTime = fileHeader.lastModifiedTime;
		this.lastModifiedDate = fileHeader.lastModifiedDate;
		this.crc32 = fileHeader.crc32;
		this.compressedSize = fileHeader.compressedSize;
		this.uncompressedSize = fileHeader.uncompressedSize;
		this.entryPath = fileHeader.entryPath;
		this.nameLength = fileHeader.nameLength;
		this.extraFieldLength = fileHeader.extraFieldLength;
		this.extraDataRecords = fileHeader.extraDataRecords;
		this.encryptionInfo = fileHeader.encryptionInfo;
	}

	/**
	 * <h3 class="en-US">Check weather the current header was encrypted</h3>
	 * <h3 class="zh-CN">检查加密条目</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean encrypted() {
		return (this.generalPurpose & FLAG_ENCRYPTED) != 0;
	}

	/**
	 * <h3 class="en-US">Get encryption method</h3>
	 * <h3 class="zh-CN">获取加密方式</h3>
	 *
	 * @return <span class="en-US">Encryption method</span>
	 * <span class="zh-CN">加密方式</span>
	 */
	public int encryptionMethod() {
		if (!this.encrypted()) {
			return ZipGlobals.ENC_NO_ENCRYPTION;
		}
		if (this.compressionMethod == ZipGlobals.COMP_AES || this.encryptionInfo instanceof AesEncryptionInfo) {
			return ZipGlobals.ENC_METHOD_AES;
		}

		if ((this.generalPurpose & FLAG_STRONG_ENCRYPTED) != 0 || this.encryptionInfo instanceof StrongEncryptionInfo) {
			return ZipGlobals.ENC_METHOD_STRONG;
		}

		return ZipGlobals.ENC_METHOD_STANDARD;
	}

	/**
	 * <h3 class="en-US">Get last modified instant</h3>
	 * <h3 class="zh-CN">获取最后修改时间戳</h3>
	 *
	 * @param timeZone <span class="en-US">Timezone</span>
	 *                 <span class="zh-CN">时区</span>
	 * @param strict   <span class="en-US">Strict format flag</span>
	 *                 <span class="zh-CN">严格格式标记</span>
	 * @return <span class="en-US">Instant</span>
	 * <span class="zh-CN">时间戳</span>
	 * @throws ArchiveException <span class="en-US">If last modified date time invalid</span>
	 *                          <span class="zh-CN">时间数据非法</span>
	 */
	public Instant modifiedInstant(final TimeZone timeZone, final boolean strict) throws IOException {
		final Instant timestamp = EntryTimes.read(this, strict)[0];

		if (timestamp != null) {
			return timestamp;
		}

		final int dosDate = this.lastModifiedDate & 0xFFFF;
		final int dosTime = this.lastModifiedTime & 0xFFFF;

		if (dosDate == 0) {
			if (strict) {
				throw new ArchiveException(0x0000001A002DL);
			}
			return Instant.EPOCH;
		}

		final int year = 1980 + ((dosDate >>> 9) & 0x7F);
		final int month = (dosDate >>> 5) & 0x0F;
		final int day = dosDate & 0x1F;

		final int hour = (dosTime >>> 11) & 0x1F;
		final int minute = (dosTime >>> 5) & 0x3F;
		final int second = (dosTime & 0x1F) * 2;

		final ZoneId zoneId = timeZone == null ? ZoneId.systemDefault() : timeZone.toZoneId();

		try {
			return LocalDateTime.of(year, month, day, hour, minute, second).atZone(zoneId).toInstant();
		} catch (final DateTimeException e) {
			if (strict) {
				throw new ArchiveException(0x0000001A002DL, e);
			}
			return Instant.EPOCH;
		}
	}

	/**
	 * <h3 class="en-US">Check weather the current header has data descriptor</h3>
	 * <h3 class="zh-CN">检查包含数据描述器</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean hasDataDescriptor() {
		return (this.generalPurpose & FLAG_DATA_DESCRIPTOR) != 0;
	}

	/**
	 * <h3 class="en-US">Check weather the current header is using UTF8 encoded</h3>
	 * <h3 class="zh-CN">检查当前使用 UTF8 编码</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean utf8Encoded() {
		return (this.generalPurpose & FLAG_UTF8) != 0;
	}

	/**
	 * <h3 class="en-US">Getter method for the record type identifier code</h3>
	 * <h3 class="zh-CN">记录类型标识的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Record type identifier code</span>
	 * <span class="zh-CN">记录类型标识</span>
	 */
	public int getSignature() {
		return this.signature;
	}

	/**
	 * <h3 class="en-US">Setter method for the record type identifier code</h3>
	 * <h3 class="zh-CN">记录类型标识的 Setter 方法</h3>
	 *
	 * @param signature <span class="en-US">Record type identifier code</span>
	 *                  <span class="zh-CN">记录类型标识</span>
	 */
	public void setSignature(final int signature) {
		this.signature = signature;
	}

	/**
	 * <h3 class="en-US">Getter method for the made version information</h3>
	 * <h3 class="zh-CN">创建版本信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Made version information</span>
	 * <span class="zh-CN">创建版本信息</span>
	 */
	public int getMadeVersion() {
		return this.madeVersion;
	}

	/**
	 * <h3 class="en-US">Setter method for the made version information</h3>
	 * <h3 class="zh-CN">创建版本信息的 Setter 方法</h3>
	 *
	 * @param madeVersion <span class="en-US">Made version information</span>
	 *                    <span class="zh-CN">创建版本信息</span>
	 */
	public void setMadeVersion(final int madeVersion) {
		this.madeVersion = madeVersion;
	}

	/**
	 * <h3 class="en-US">Getter method for the minimum ZIP specification version required</h3>
	 * <h3 class="zh-CN">需要的最低 ZIP 规范版本的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Minimum ZIP specification version required</span>
	 * <span class="zh-CN">需要的最低 ZIP 规范版本</span>
	 */
	public int getExtractNeeded() {
		return this.extractNeeded;
	}

	/**
	 * <h3 class="en-US">Setter method for the minimum ZIP specification version required</h3>
	 * <h3 class="zh-CN">需要的最低 ZIP 规范版本的 Setter 方法</h3>
	 *
	 * @param extractNeeded <span class="en-US">Minimum ZIP specification version required</span>
	 *                      <span class="zh-CN">需要的最低 ZIP 规范版本</span>
	 */
	public void setExtractNeeded(final int extractNeeded) {
		this.extractNeeded = extractNeeded;
	}

	/**
	 * <h3 class="en-US">Getter method for the general purpose flag</h3>
	 * <h3 class="zh-CN">通用用途位标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">General purpose flag</span>
	 * <span class="zh-CN">通用用途位标记</span>
	 */
	public int getGeneralPurpose() {
		return this.generalPurpose;
	}

	/**
	 * <h3 class="en-US">Setter method for the general purpose flag</h3>
	 * <h3 class="zh-CN">通用用途位标记的 Setter 方法</h3>
	 *
	 * @param generalPurpose <span class="en-US">General purpose flag</span>
	 *                       <span class="zh-CN">通用用途位标记</span>
	 */
	public void setGeneralPurpose(final int generalPurpose) {
		this.generalPurpose = generalPurpose;
	}

	/**
	 * <h3 class="en-US">Getter method for the compression method code</h3>
	 * <h3 class="zh-CN">压缩方法编号的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Compression method code</span>
	 * <span class="zh-CN">压缩方法编号</span>
	 */
	public int getCompressionMethod() {
		return this.compressionMethod;
	}

	/**
	 * <h3 class="en-US">Setter method for the compression method code</h3>
	 * <h3 class="zh-CN">压缩方法编号的 Setter 方法</h3>
	 *
	 * @param compressionMethod <span class="en-US">Compression method code</span>
	 *                          <span class="zh-CN">压缩方法编号</span>
	 */
	public void setCompressionMethod(final int compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	/**
	 * <h3 class="en-US">Getter method for the external file attribute information</h3>
	 * <h3 class="zh-CN">文件系统属性信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">External file attribute information</span>
	 * <span class="zh-CN">文件系统属性信息</span>
	 */
	public long getExternalFileAttr() {
		return this.externalFileAttr;
	}

	/**
	 * <h3 class="en-US">Setter method for the external file attribute information</h3>
	 * <h3 class="zh-CN">文件系统属性信息的 Setter 方法</h3>
	 *
	 * @param externalFileAttr <span class="en-US">External file attribute information</span>
	 *                         <span class="zh-CN">文件系统属性信息</span>
	 */
	public void setExternalFileAttr(final long externalFileAttr) {
		this.externalFileAttr = externalFileAttr;
	}

	/**
	 * <h3 class="en-US">Getter method for the last modified time</h3>
	 * <h3 class="zh-CN">最后修改时间的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Last modified time</span>
	 * <span class="zh-CN">最后修改时间</span>
	 */
	public int getLastModifiedTime() {
		return this.lastModifiedTime;
	}

	/**
	 * <h3 class="en-US">Setter method for the last modified time</h3>
	 * <h3 class="zh-CN">最后修改时间的 Setter 方法</h3>
	 *
	 * @param lastModifiedTime <span class="en-US">Last modified time</span>
	 *                         <span class="zh-CN">最后修改时间</span>
	 */
	public void setLastModifiedTime(final int lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}

	/**
	 * <h3 class="en-US">Getter method for the last modified date</h3>
	 * <h3 class="zh-CN">最后修改日期的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Last modified date</span>
	 * <span class="zh-CN">最后修改日期</span>
	 */
	public int getLastModifiedDate() {
		return this.lastModifiedDate;
	}

	/**
	 * <h3 class="en-US">Setter method for the last modified date</h3>
	 * <h3 class="zh-CN">最后修改日期的 Setter 方法</h3>
	 *
	 * @param lastModifiedDate <span class="en-US">Last modified date</span>
	 *                         <span class="zh-CN">最后修改日期</span>
	 */
	public void setLastModifiedDate(final int lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	/**
	 * <h3 class="en-US">Getter method for the CRC32 value of the original value</h3>
	 * <h3 class="zh-CN">原始数据的 CRC32 值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">CRC32 value of the original value</span>
	 * <span class="zh-CN">原始数据的 CRC32 值</span>
	 */
	public long getCrc32() {
		return this.crc32;
	}

	/**
	 * <h3 class="en-US">Setter method for the CRC32 value of the original value</h3>
	 * <h3 class="zh-CN">原始数据的 CRC32 值的 Setter 方法</h3>
	 *
	 * @param crc32 <span class="en-US">CRC32 value of the original value</span>
	 *              <span class="zh-CN">原始数据的 CRC32 值</span>
	 */
	public void setCrc32(final long crc32) {
		this.crc32 = crc32;
	}

	/**
	 * <h3 class="en-US">Getter method for the compressed data size</h3>
	 * <h3 class="zh-CN">压缩后数据大小的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Compressed data size</span>
	 * <span class="zh-CN">压缩后数据大小</span>
	 */
	public long getCompressedSize() {
		return this.compressedSize;
	}

	/**
	 * <h3 class="en-US">Setter method for the compressed data size</h3>
	 * <h3 class="zh-CN">压缩后数据大小的 Setter 方法</h3>
	 *
	 * @param compressedSize <span class="en-US">Compressed data size</span>
	 *                       <span class="zh-CN">压缩后数据大小</span>
	 */
	public void setCompressedSize(final long compressedSize) {
		this.compressedSize = compressedSize;
	}

	/**
	 * <h3 class="en-US">Getter method for the uncompressed data size</h3>
	 * <h3 class="zh-CN">未压缩数据大小的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Uncompressed data size</span>
	 * <span class="zh-CN">未压缩数据大小</span>
	 */
	public long getUncompressedSize() {
		return this.uncompressedSize;
	}

	/**
	 * <h3 class="en-US">Setter method for the uncompressed data size</h3>
	 * <h3 class="zh-CN">未压缩数据大小的 Setter 方法</h3>
	 *
	 * @param uncompressedSize <span class="en-US">Uncompressed data size</span>
	 *                         <span class="zh-CN">未压缩数据大小</span>
	 */
	public void setUncompressedSize(final long uncompressedSize) {
		this.uncompressedSize = uncompressedSize;
	}

	/**
	 * <h3 class="en-US">Getter method for the entry path</h3>
	 * <h3 class="zh-CN">条目路径的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Entry path</span>
	 * <span class="zh-CN">条目路径</span>
	 */
	public String getEntryPath() {
		return this.entryPath;
	}

	/**
	 * <h3 class="en-US">Setter method for the entry path</h3>
	 * <h3 class="zh-CN">条目路径的 Setter 方法</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 */
	public void setEntryPath(final String entryPath) {
		this.entryPath = entryPath;
	}

	/**
	 * <h3 class="en-US">Getter method for the data length of the entry name</h3>
	 * <h3 class="zh-CN">条目名称的字节长度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">The data length of the entry name</span>
	 * <span class="zh-CN">条目名称的字节长度</span>
	 */
	public int getNameLength() {
		return this.nameLength;
	}

	/**
	 * <h3 class="en-US">Setter method for the data length of the entry name</h3>
	 * <h3 class="zh-CN">条目名称的字节长度的 Setter 方法</h3>
	 *
	 * @param nameLength <span class="en-US">The data length of the entry name</span>
	 *                   <span class="zh-CN">条目名称的字节长度</span>
	 */
	public void setNameLength(final int nameLength) {
		this.nameLength = nameLength;
	}

	/**
	 * <h3 class="en-US">Getter method for the extra field length</h3>
	 * <h3 class="zh-CN">扩展属性长度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Extra field length</span>
	 * <span class="zh-CN">扩展属性长度</span>
	 */
	public int getExtraFieldLength() {
		return this.extraFieldLength;
	}

	/**
	 * <h3 class="en-US">Setter method for the extra field length</h3>
	 * <h3 class="zh-CN">扩展属性长度的 Setter 方法</h3>
	 *
	 * @param extraFieldLength <span class="en-US">Extra field length</span>
	 *                         <span class="zh-CN">扩展属性长度</span>
	 */
	public void setExtraFieldLength(final int extraFieldLength) {
		this.extraFieldLength = extraFieldLength;
	}

	/**
	 * <h3 class="en-US">Getter method for the extra data record list</h3>
	 * <h3 class="zh-CN">扩展数据记录列表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Extra data record list</span>
	 * <span class="zh-CN">扩展数据记录列表</span>
	 */
	public List<ExtraDataRecord> getExtraDataRecords() {
		return this.extraDataRecords;
	}

	/**
	 * <h3 class="en-US">Setter method for the extra data record list</h3>
	 * <h3 class="zh-CN">扩展数据记录列表的 Setter 方法</h3>
	 *
	 * @param extraDataRecords <span class="en-US">Extra data record list</span>
	 *                         <span class="zh-CN">扩展数据记录列表</span>
	 */
	public void setExtraDataRecords(final List<ExtraDataRecord> extraDataRecords) {
		this.extraDataRecords = Collections.unmodifiableList(extraDataRecords);
	}

	/**
	 * <h3 class="en-US">Getter method for the encryption information</h3>
	 * <h3 class="zh-CN">加密信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">AES encryption information</span>
	 * <span class="zh-CN">AES 加密信息</span>
	 */
	public EncryptionInfo getEncryptionInfo() {
		return this.encryptionInfo;
	}

	/**
	 * <h3 class="en-US">Setter method for the encryption information</h3>
	 * <h3 class="zh-CN">加密信息的 Setter 方法</h3>
	 *
	 * @param encryptionInfo <span class="en-US">AES encryption information</span>
	 *                       <span class="zh-CN">AES 加密信息</span>
	 */
	public void setEncryptionInfo(final EncryptionInfo encryptionInfo) {
		this.encryptionInfo = encryptionInfo;
	}

	/**
	 * <h3 class="en-US">Current archive file created by Unix system</h3>
	 * <h3 class="zh-CN">当前归档文件由 Unix 系统创建</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public boolean isUnix() {
		int host = (this.madeVersion >>> 8) & 0xFF;
		return host == HOST_MAC_OS_X || host == HOST_UNIX;
	}

	/**
	 * <h3 class="en-US">Get Unix mode code</h3>
	 * <h3 class="zh-CN">获取 Unix 模式代码</h3>
	 *
	 * @return <span class="en-US">Unix mode code</span>
	 * <span class="zh-CN">Unix 模式代码</span>
	 */
	public int unixMode() {
		return (int) (((this.externalFileAttr & 0xFFFFFFFFL) >>> 16) & 0xFFFFL);
	}

	/**
	 * <h3 class="en-US">Get dos attribute code</h3>
	 * <h3 class="zh-CN">获取 dos 属性代码</h3>
	 *
	 * @return <span class="en-US">Dos attribute code</span>
	 * <span class="zh-CN">Dos 属性代码</span>
	 */
	public int dosAttributes() {
		return (int) ((this.externalFileAttr & 0xFFFFFFFFL) & 0xFF);
	}

	/**
	 * <h3 class="en-US">Convert to byte array</h3>
	 * <h3 class="zh-CN">转换为字节数组</h3>
	 *
	 * @param charset   <span class="en-US">Charset of the entry name</span>
	 *                  <span class="zh-CN">条目名使用的字符集</span>
	 * @param zip64Mode <span class="en-US">Enumeration value of the Zip64</span>
	 *                  <span class="zh-CN">Zip64模式枚举值</span>
	 * @return <span class="en-US">Converted byte array</span>
	 * <span class="zh-CN">转换后的字节数组</span>
	 * @throws IOException <span class="en-US">If an error occurs when convert data</span>
	 *                     <span class="zh-CN">转换过程中出错</span>
	 */
	public abstract byte[] toByteArray(final Charset charset, final Zip64Mode zip64Mode) throws IOException;

	/**
	 * <h3 class="en-US">Get entry type</h3>
	 * <h3 class="zh-CN">获取条目类型</h3>
	 *
	 * @return <span class="en-US">Entry type</span>
	 * <span class="zh-CN">条目类型</span>
	 */
	public final EntryType entryType() {
		if (StringUtils.isEmpty(this.entryPath)) {
			return EntryType.UNKNOWN;
		}
		if (this.entryPath.endsWith(ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR) || (this.externalFileAttr & 0x10L) != 0L) {
			return EntryType.DIRECTORY;
		}

		if (this.isUnix()) {
			int unixMode = this.unixMode();
			return switch (unixMode & UNIX_FILE_TYPE_MASK) {
				case UNIX_DIRECTORY -> EntryType.DIRECTORY;
				case UNIX_SYMBOLIC_LINK -> EntryType.SYMBOLIC_LINK;
				case UNIX_REGULAR_FILE -> this.hardLinkTarget() ? EntryType.HARD_LINK : EntryType.FILE;
				case 0 -> EntryType.FILE;
				default -> EntryType.UNKNOWN;
			};
		}
		return EntryType.FILE;
	}

	/**
	 * <h3 class="en-US">Read hard link target address</h3>
	 * <h3 class="zh-CN">读取硬连接目标地址</h3>
	 *
	 * @param charset <span class="en-US">Charset</span>
	 *                <span class="zh-CN">字符集</span>
	 * @return <span class="en-US">Hard link target address</span>
	 * <span class="zh-CN">硬连接目标地址</span>
	 */
	public String hardLinkTarget(final Charset charset) {
		if (!this.isUnix() || (this.unixMode() & UNIX_FILE_TYPE_MASK) != UNIX_REGULAR_FILE
				|| this.extraDataRecords == null || this.extraDataRecords.isEmpty()) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		return this.extraDataRecords.stream()
				.filter(record -> record.headerId() == PKWARE_UNIX_EXTRA_FIELD)
				.map(ExtraDataRecord::dataBytes)
				.filter(data -> data != null && data.length > PKWARE_UNIX_FIXED_LENGTH)
				.map(data ->
						new String(data, PKWARE_UNIX_FIXED_LENGTH, data.length - PKWARE_UNIX_FIXED_LENGTH, charset))
				.filter(StringUtils::notBlank)
				.findFirst()
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Check weather the current entry type is hard link</h3>
	 * <h3 class="zh-CN">检查当前条目类型是否为硬连接</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	private boolean hardLinkTarget() {
		if (!this.isUnix()
				|| (this.unixMode() & UNIX_FILE_TYPE_MASK) != UNIX_REGULAR_FILE
				|| this.extraDataRecords == null
				|| this.extraDataRecords.isEmpty()) {
			return false;
		}
		return this.extraDataRecords.stream()
				.filter(record -> record.headerId() == PKWARE_UNIX_EXTRA_FIELD)
				.map(ExtraDataRecord::dataBytes)
				.anyMatch(data -> data != null && data.length > PKWARE_UNIX_FIXED_LENGTH);
	}
}
