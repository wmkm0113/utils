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
 * <h2 class="en-US">General file header define</h2>
 * <h2 class="zh-CN">中央目录头定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class GeneralFileHeader extends FileHeader {

	/**
	 * <span class="en-US">Comment length</span>
	 * <span class="zh-CN">批注信息长度</span>
	 */
	private int commentLength;
	/**
	 * <span class="en-US">Volume number where the central repository is located</span>
	 * <span class="zh-CN">中央仓库所在卷编号</span>
	 */
	private long diskNumberStart;
	/**
	 * <span class="en-US">Internal file attribute information</span>
	 * <span class="zh-CN">内容属性信息</span>
	 */
	private int internalFileAttr = 0;
	/**
	 * <span class="en-US">Offset value of the local header</span>
	 * <span class="zh-CN">本地文件头偏移值</span>
	 */
	private long offsetLocalHeader;
	/**
	 * <span class="en-US">Comment string</span>
	 * <span class="zh-CN">批注信息</span>
	 */
	private String fileComment;

	/**
	 * <h3 class="en-US">Constructor method for the central directory define</h3>
	 * <h3 class="zh-CN">中央仓库定义的构造方法</h3>
	 */
	public GeneralFileHeader() {
	}

	/**
	 * <h3 class="en-US">Constructor method for the central directory define</h3>
	 * <h3 class="zh-CN">中央仓库定义的构造方法</h3>
	 *
	 * @param fileHeader <span class="en-US">File header instance object</span>
	 *                   <span class="zh-CN">文件头实例对象</span>
	 */
	private GeneralFileHeader(final GeneralFileHeader fileHeader) {
		super(fileHeader);
		this.commentLength = fileHeader.commentLength;
		this.diskNumberStart = fileHeader.diskNumberStart;
		this.internalFileAttr = fileHeader.internalFileAttr;
		this.offsetLocalHeader = fileHeader.offsetLocalHeader;
		this.fileComment = fileHeader.fileComment;
	}

	/**
	 * <h3 class="en-US">Getter method for the comment length</h3>
	 * <h3 class="zh-CN">批注信息长度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Comment length</span>
	 * <span class="zh-CN">批注信息长度</span>
	 */
	public int getCommentLength() {
		return this.commentLength;
	}

	/**
	 * <h3 class="en-US">Setter method for the comment length</h3>
	 * <h3 class="zh-CN">批注信息长度的 Setter 方法</h3>
	 *
	 * @param commentLength <span class="en-US">Comment length</span>
	 *                      <span class="zh-CN">批注信息长度</span>
	 */
	public void setCommentLength(final int commentLength) {
		this.commentLength = commentLength;
	}

	/**
	 * <h3 class="en-US">Getter method for the volume number where the central repository is located</h3>
	 * <h3 class="zh-CN">中央仓库所在卷编号的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Volume number where the central repository is located</span>
	 * <span class="zh-CN">中央仓库所在卷编号</span>
	 */
	public long getDiskNumberStart() {
		return this.diskNumberStart;
	}

	/**
	 * <h3 class="en-US">Setter method for the volume number where the central repository is located</h3>
	 * <h3 class="zh-CN">中央仓库所在卷编号的 Setter 方法</h3>
	 *
	 * @param diskNumberStart <span class="en-US">Volume number where the central repository is located</span>
	 *                        <span class="zh-CN">中央仓库所在卷编号</span>
	 */
	public void setDiskNumberStart(final long diskNumberStart) {
		this.diskNumberStart = diskNumberStart;
	}

	/**
	 * <h3 class="en-US">Getter method for the internal file attribute information</h3>
	 * <h3 class="zh-CN">内容属性信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Internal file attribute information</span>
	 * <span class="zh-CN">内容属性信息</span>
	 */
	public int getInternalFileAttr() {
		return this.internalFileAttr;
	}

	/**
	 * <h3 class="en-US">Setter method for the internal file attribute information</h3>
	 * <h3 class="zh-CN">内容属性信息的 Setter 方法</h3>
	 *
	 * @param internalFileAttr <span class="en-US">Internal file attribute information</span>
	 *                         <span class="zh-CN">内容属性信息</span>
	 */
	public void setInternalFileAttr(final int internalFileAttr) {
		this.internalFileAttr = internalFileAttr;
	}

	/**
	 * <h3 class="en-US">Getter method for the offset value of the local header</h3>
	 * <h3 class="zh-CN">本地文件头偏移值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Offset value of the local header</span>
	 * <span class="zh-CN">本地文件头偏移值</span>
	 */
	public long getOffsetLocalHeader() {
		return this.offsetLocalHeader;
	}

	/**
	 * <h3 class="en-US">Setter method for the offset value of the local header</h3>
	 * <h3 class="zh-CN">本地文件头偏移值的 Setter 方法</h3>
	 *
	 * @param offsetLocalHeader <span class="en-US">Offset value of the local header</span>
	 *                          <span class="zh-CN">本地文件头偏移值</span>
	 */
	public void setOffsetLocalHeader(final long offsetLocalHeader) {
		this.offsetLocalHeader = offsetLocalHeader;
	}

	/**
	 * <h3 class="en-US">Getter method for the comment string</h3>
	 * <h3 class="zh-CN">批注信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Comment string</span>
	 * <span class="zh-CN">批注信息</span>
	 */
	public String getFileComment() {
		return this.fileComment;
	}

	/**
	 * <h3 class="en-US">Setter method for the comment string</h3>
	 * <h3 class="zh-CN">批注信息的 Setter 方法</h3>
	 *
	 * @param fileComment <span class="en-US">Comment string</span>
	 *                    <span class="zh-CN">批注信息</span>
	 */
	public void setFileComment(final String fileComment) {
		this.fileComment = fileComment;
	}

	@Override
	public byte[] toByteArray(final Charset charset, final Zip64Mode zip64Mode) throws IOException {

		Charset encoding = this.utf8Encoded() ? StandardCharsets.UTF_8 : charset;
		byte[] nameBytes = this.getEntryPath().getBytes(encoding);
		String fileComment = this.getFileComment();
		byte[] commentBytes = fileComment == null ? new byte[0] : fileComment.getBytes(encoding);

		if (nameBytes.length == 0 || nameBytes.length > ZipGlobals.ZIP_16_LIMIT
				|| commentBytes.length > ZipGlobals.ZIP_16_LIMIT) {
			throw new IOException("Invalid ZIP central header name or comment length");
		}

		long compressedSize = this.getCompressedSize();
		long uncompressedSize = this.getUncompressedSize();
		long localOffset = this.getOffsetLocalHeader();
		long startDisk = this.getDiskNumberStart();

		if (compressedSize < 0L || uncompressedSize < 0L || localOffset < 0L || startDisk < 0L
				|| startDisk > ZipGlobals.ZIP_64_LIMIT) {
			throw new IOException("Invalid ZIP central header size, offset or disk");
		}

		boolean zip64Sizes = (zip64Mode == Zip64Mode.ALWAYS)
				|| compressedSize >= ZipGlobals.ZIP_64_LIMIT || uncompressedSize >= ZipGlobals.ZIP_64_LIMIT;
		boolean zip64Offset = localOffset >= ZipGlobals.ZIP_64_LIMIT;
		boolean zip64Disk = startDisk >= ZipGlobals.ZIP_16_LIMIT;
		int zip64Length = (zip64Sizes ? 16 : 0) + (zip64Offset ? 8 : 0) + (zip64Disk ? 4 : 0);

		List<ExtraDataRecord> extraRecords = this.getExtraDataRecords() == null
				? new ArrayList<>()
				: new ArrayList<>(this.getExtraDataRecords());
		extraRecords.removeIf(record -> record.headerId() == ZipGlobals.ZIP64_EXTRA_HEADER_ID);

		if (zip64Length > 0) {
			ByteBuffer zip64 = ByteBuffer.allocate(zip64Length).order(ByteOrder.LITTLE_ENDIAN);
			if (zip64Sizes) {
				zip64.putLong(uncompressedSize);
				zip64.putLong(compressedSize);
			}
			if (zip64Offset) {
				zip64.putLong(localOffset);
			}
			if (zip64Disk) {
				zip64.putInt((int) startDisk);
			}
			extraRecords.add(0, new ExtraDataRecord(ZipGlobals.ZIP64_EXTRA_HEADER_ID, zip64.array()));
		}

		int extraLength = 0;
		for (ExtraDataRecord record : extraRecords) {
			int length = record.dataBytes().length;
			if (length > ZipGlobals.ZIP_16_LIMIT - extraLength - 4) {
				throw new IOException("ZIP central extra fields exceed 65535 bytes");
			}
			extraLength += 4 + length;
		}

		int extractNeeded = this.getExtractNeeded();
		if (zip64Length > 0) {
			extractNeeded = Math.max(extractNeeded, ZipGlobals.ZIP_64_VERSION);
		}
		int wireMethod = (this.encryptionMethod() == ZipGlobals.ENC_METHOD_AES)
				? ZipGlobals.COMP_AES
				: this.getCompressionMethod();

		ByteBuffer headerBytes = ByteBuffer.allocate(46 + nameBytes.length + extraLength + commentBytes.length)
				.order(ByteOrder.LITTLE_ENDIAN);
		headerBytes.putInt((int) ZipGlobals.GENERAL_FILE_HEADER_SIGNATURE);
		headerBytes.putShort((short) this.getMadeVersion());
		headerBytes.putShort((short) extractNeeded);
		headerBytes.putShort((short) this.getGeneralPurpose());
		headerBytes.putShort((short) wireMethod);
		headerBytes.putShort((short) this.getLastModifiedTime());
		headerBytes.putShort((short) this.getLastModifiedDate());
		headerBytes.putInt((int) this.getCrc32());
		headerBytes.putInt(zip64Sizes ? Globals.DEFAULT_VALUE_INT : (int) compressedSize);
		headerBytes.putInt(zip64Sizes ? Globals.DEFAULT_VALUE_INT : (int) uncompressedSize);
		headerBytes.putShort((short) nameBytes.length);
		headerBytes.putShort((short) extraLength);
		headerBytes.putShort((short) commentBytes.length);
		headerBytes.putShort((short) (zip64Disk ? ZipGlobals.ZIP_16_LIMIT : startDisk));
		headerBytes.putShort((short) this.getInternalFileAttr());
		headerBytes.putInt((int) this.getExternalFileAttr());
		headerBytes.putInt(zip64Offset ? Globals.DEFAULT_VALUE_INT : (int) localOffset);
		headerBytes.put(nameBytes);

		for (ExtraDataRecord record : extraRecords) {
			byte[] data = record.dataBytes();
			headerBytes.putShort((short) record.headerId());
			headerBytes.putShort((short) data.length);
			headerBytes.put(data);
		}

		headerBytes.put(commentBytes);
		return headerBytes.array();
	}

	/**
	 * <h3 class="en-US">Duplicate current file header</h3>
	 * <h3 class="zh-CN">复制当前文件头</h3>
	 *
	 * @return <span class="en-US">Duplicate file header</span>
	 * <span class="zh-CN">复制的文件头</span>
	 */
	public GeneralFileHeader duplicate() {
		return new GeneralFileHeader(this);
	}
}
