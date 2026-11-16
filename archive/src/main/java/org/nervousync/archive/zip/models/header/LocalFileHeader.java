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
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.zip.models.EntryTimes;
import org.nervousync.archive.zip.models.ExtraDataRecord;
import org.nervousync.commons.Globals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * <h2 class="en-US">Local file header define</h2>
 * <h2 class="zh-CN">本地文件头定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class LocalFileHeader extends FileHeader {

	/**
	 * <span class="en-US">Volume number where the local file header is located</span>
	 * <span class="zh-CN">本地文件头所在卷编号</span>
	 */
	private long diskNumberStart;
	/**
	 * <span class="en-US">Offset of the local file header of the located volume</span>
	 * <span class="zh-CN">本地文件头所在卷偏移量</span>
	 */
	private long offsetLocalHeader;
	/**
	 * <span class="en-US">Offset of the data start</span>
	 * <span class="zh-CN">数据起始偏移量</span>
	 */
	private long offsetStartOfData;
	/**
	 * <span class="en-US">Comment string</span>
	 * <span class="zh-CN">批注信息</span>
	 */
	private String comment;
	/**
	 * <span class="en-US">Local file path</span>
	 * <span class="zh-CN">本地文件路径</span>
	 */
	private String filePath;
	/**
	 * <span class="en-US">Store timestamp extra fields only in the central directory</span>
	 * <span class="zh-CN">时间扩展字段仅写入中央目录</span>
	 */
	private boolean timestampMetadataCentralOnly = false;

	/**
	 * <h3 class="en-US">Constructor method for the local file header define</h3>
	 * <h3 class="zh-CN">本地文件头定义的构造方法</h3>
	 */
	public LocalFileHeader() {
	}

	/**
	 * <h3 class="en-US">Constructor method for the local file header define</h3>
	 * <h3 class="zh-CN">本地文件头定义的构造方法</h3>
	 */
	private LocalFileHeader(final LocalFileHeader fileHeader) {
		super(fileHeader);
		this.diskNumberStart = fileHeader.diskNumberStart;
		this.offsetLocalHeader = fileHeader.offsetLocalHeader;
		this.offsetStartOfData = fileHeader.offsetStartOfData;
		this.comment = fileHeader.comment;
		this.filePath = fileHeader.filePath;
		this.timestampMetadataCentralOnly = fileHeader.timestampMetadataCentralOnly;
	}

	/**
	 * <h3 class="en-US">Getter method for the volume number where the local file header is located</h3>
	 * <h3 class="zh-CN">本地文件头所在卷编号的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Volume number where the local file header is located</span>
	 * <span class="zh-CN">本地文件头所在卷编号</span>
	 */
	public long getDiskNumberStart() {
		return this.diskNumberStart;
	}

	/**
	 * <h3 class="en-US">Setter method for the volume number where the local file header is located</h3>
	 * <h3 class="zh-CN">本地文件头所在卷编号的 Setter 方法</h3>
	 *
	 * @param diskNumberStart <span class="en-US">Volume number where the local file header is located</span>
	 *                        <span class="zh-CN">本地文件头所在卷编号</span>
	 */
	public void setDiskNumberStart(final long diskNumberStart) {
		this.diskNumberStart = diskNumberStart;
	}

	/**
	 * <h3 class="en-US">Getter method for the offset of the local file header of the located volume</h3>
	 * <h3 class="zh-CN">本地文件头所在卷偏移量的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Offset of the local file header of the located volume</span>
	 * <span class="zh-CN">本地文件头所在卷偏移量</span>
	 */
	public long getOffsetLocalHeader() {
		return this.offsetLocalHeader;
	}

	/**
	 * <h3 class="en-US">Setter method for the offset of the local file header of the located volume</h3>
	 * <h3 class="zh-CN">本地文件头所在卷偏移量的 Setter 方法</h3>
	 *
	 * @param offsetLocalHeader <span class="en-US">Offset of the local file header of the located volume</span>
	 *                          <span class="zh-CN">本地文件头所在卷偏移量</span>
	 */
	public void setOffsetLocalHeader(final long offsetLocalHeader) {
		this.offsetLocalHeader = offsetLocalHeader;
	}

	/**
	 * <h3 class="en-US">Getter method for the offset of the data start</h3>
	 * <h3 class="zh-CN">数据起始偏移量的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Offset of the data start</span>
	 * <span class="zh-CN">数据起始偏移量</span>
	 */
	public long getOffsetStartOfData() {
		return this.offsetStartOfData;
	}

	/**
	 * <h3 class="en-US">Setter method for the offset of the data start</h3>
	 * <h3 class="zh-CN">数据起始偏移量的 Setter 方法</h3>
	 *
	 * @param offsetStartOfData <span class="en-US">Offset of the data start</span>
	 *                          <span class="zh-CN">数据起始偏移量</span>
	 */
	public void setOffsetStartOfData(final long offsetStartOfData) {
		this.offsetStartOfData = offsetStartOfData;
	}

	/**
	 * <h3 class="en-US">Getter method for the comment string</h3>
	 * <h3 class="zh-CN">批注信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Comment string</span>
	 * <span class="zh-CN">批注信息</span>
	 */
	public String getComment() {
		return this.comment;
	}

	/**
	 * <h3 class="en-US">Setter method for the comment string</h3>
	 * <h3 class="zh-CN">批注信息的 Setter 方法</h3>
	 *
	 * @param comment <span class="en-US">Comment string</span>
	 *                <span class="zh-CN">批注信息</span>
	 */
	public void setComment(final String comment) {
		this.comment = comment;
	}

	/**
	 * <h3 class="en-US">Getter method for the local file path</h3>
	 * <h3 class="zh-CN">本地文件路径的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Local file path</span>
	 * <span class="zh-CN">本地文件路径</span>
	 */
	public String getFilePath() {
		return this.filePath;
	}

	/**
	 * <h3 class="en-US">Setter method for the local file path</h3>
	 * <h3 class="zh-CN">本地文件路径的 Setter 方法</h3>
	 *
	 * @param filePath <span class="en-US">Local file path</span>
	 *                 <span class="zh-CN">本地文件路径</span>
	 */
	public void setFilePath(final String filePath) {
		this.filePath = filePath;
	}

	/**
	 * <h3 class="en-US">Set the timestamp extra field serialization policy</h3>
	 * <h3 class="zh-CN">设置时间扩展字段的序列化策略</h3>
	 *
	 * @param timestampMetadataCentralOnly <span class="en-US">Whether timestamp extra fields belong only in the central directory</span>
	 *                                     <span class="zh-CN">是否仅在中央目录中写入时间扩展字段</span>
	 */
	public void setTimestampMetadataCentralOnly(final boolean timestampMetadataCentralOnly) {
		this.timestampMetadataCentralOnly = timestampMetadataCentralOnly;
	}

	@Override
	public byte[] toByteArray(final Charset charset, final Zip64Mode zip64Mode) throws IOException {
		final byte[] nameBytes = this.getEntryPath().getBytes(this.utf8Encoded() ? StandardCharsets.UTF_8 : charset);
		if (nameBytes.length == 0 || nameBytes.length > 0xFFFF) {
			throw new IOException("Invalid ZIP entry name length: " + nameBytes.length);
		}

		final List<ExtraDataRecord> wireExtras = new ArrayList<>();
		final boolean omitTimestamps = this.timestampMetadataCentralOnly || (this.getGeneralPurpose() & 0x2000) != 0;

		if (this.getExtraDataRecords() != null) {
			for (final ExtraDataRecord record : this.getExtraDataRecords()) {
				if (omitTimestamps && EntryTimes.isTimestamp(record)) {
					continue;
				}
				wireExtras.add(record);
			}
		}

		int extraLength = 0;
		boolean zip64 = false;

		for (final ExtraDataRecord record : wireExtras) {
			final int length = record.dataBytes().length;
			if (length > 0xFFFF - extraLength - 4) {
				throw new IOException("ZIP local extra fields exceed 65535 bytes");
			}
			extraLength += 4 + length;
			if (record.headerId() == ZipGlobals.ZIP64_EXTRA_HEADER_ID) {
				if (zip64 || length != 16) {
					throw new IOException("Invalid ZIP64 local extra field");
				}
				zip64 = true;
			}
		}

		final boolean descriptor = this.hasDataDescriptor();
		final long compressedSize = descriptor ? 0L : this.getCompressedSize();
		final long uncompressedSize = descriptor ? 0L : this.getUncompressedSize();

		if (!zip64 && (compressedSize < 0 || uncompressedSize < 0 || compressedSize >= ZipGlobals.ZIP_64_LIMIT
				|| uncompressedSize >= ZipGlobals.ZIP_64_LIMIT)) {
			throw new IOException("Invalid local sizes or missing ZIP64 extra field");
		}

		final int wireMethod =
				(this.encryptionMethod() == ZipGlobals.ENC_METHOD_AES)
						? ZipGlobals.COMP_AES
						: this.getCompressionMethod();

		final ByteBuffer buffer =
				ByteBuffer.allocate(30 + nameBytes.length + extraLength).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt((int) ZipGlobals.LOCAL_FILE_HEADER_SIGNATURE);
		buffer.putShort((short) this.getExtractNeeded());
		buffer.putShort((short) this.getGeneralPurpose());
		buffer.putShort((short) wireMethod);
		buffer.putShort((short) this.getLastModifiedTime());
		buffer.putShort((short) this.getLastModifiedDate());
		buffer.putInt(descriptor ? Globals.INITIALIZE_INT_VALUE : (int) this.getCrc32());
		buffer.putInt(zip64 ? Globals.DEFAULT_VALUE_INT : (int) compressedSize);
		buffer.putInt(zip64 ? Globals.DEFAULT_VALUE_INT : (int) uncompressedSize);
		buffer.putShort((short) nameBytes.length);
		buffer.putShort((short) extraLength);
		buffer.put(nameBytes);

		for (final ExtraDataRecord record : wireExtras) {
			final byte[] data = record.dataBytes();

			buffer.putShort((short) record.headerId());
			buffer.putShort((short) data.length);
			buffer.put(data);
		}

		return buffer.array();
	}

	/**
	 * <h3 class="en-US">Convert to data descriptor data bytes</h3>
	 * <h3 class="zh-CN">生成数据描述字节数组</h3>
	 *
	 * @return <span class="en-US">Converted byte array</span>
	 * <span class="zh-CN">转换后的字节数组</span>
	 * @throws IOException <span class="en-US">If an error occurs when convert data</span>
	 *                     <span class="zh-CN">转换过程中出错</span>
	 */
	public byte[] toDescriptor() throws IOException {
		if (!this.hasDataDescriptor()) {
			return new byte[0];
		}
		long storedSize = this.getCompressedSize();
		long originalSize = this.getUncompressedSize();
		if (storedSize < 0L || originalSize < 0L) {
			throw new IOException("ZIP entry sizes are not finalized");
		}

		boolean zip64 = storedSize >= ZipGlobals.ZIP_64_LIMIT || originalSize >= ZipGlobals.ZIP_64_LIMIT;

		if (this.getExtraDataRecords() != null) {
			for (ExtraDataRecord record : this.getExtraDataRecords()) {
				if (record.headerId() == ZipGlobals.ZIP64_EXTRA_HEADER_ID) {
					if (record.dataBytes().length != 16) {
						throw new IOException("Invalid ZIP64 local extra field");
					}
					zip64 = true;
				}
			}
		}

		boolean masked = (this.getGeneralPurpose() & 0x2000) != 0;
		long crc32 = masked ? 0L : this.getCrc32();
		if (masked) {
			storedSize = 0L;
			originalSize = 0L;
		}

		ByteBuffer buffer = ByteBuffer.allocate(zip64 ? 24 : 16).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt((int) ZipGlobals.DATA_DESCRIPTOR_SIGNATURE);
		buffer.putInt((int) crc32);

		if (zip64) {
			buffer.putLong(storedSize);
			buffer.putLong(originalSize);
		} else {
			buffer.putInt((int) storedSize);
			buffer.putInt((int) originalSize);
		}

		return buffer.array();
	}

	/**
	 * <h3 class="en-US">Duplicate current file header</h3>
	 * <h3 class="zh-CN">复制当前文件头</h3>
	 *
	 * @return <span class="en-US">Duplicate file header</span>
	 * <span class="zh-CN">复制的文件头</span>
	 */
	public LocalFileHeader duplicate() {
		return new LocalFileHeader(this);
	}
}
