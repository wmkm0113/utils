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

package org.nervousync.archive.zip.models.central;

import org.nervousync.archive.commons.ZipGlobals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <h2 class="en-US">End of central directory record define</h2>
 * <h2 class="zh-CN">中央目录结束记录定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class EndCentralDirectoryRecord {

	/**
	 * <span class="en-US">Number of this disk</span>
	 * <span class="zh-CN">所在分卷编号</span>
	 */
	private int diskNumber;
	/**
	 * <span class="en-US">Disk number start of central directory</span>
	 * <span class="zh-CN">中央目录开始分卷编号</span>
	 */
	private int centralDirectoryStartDisk;
	/**
	 * <span class="en-US">Total number of entries in the central directory on this disk</span>
	 * <span class="zh-CN">此分卷上中央目录中的条目总数</span>
	 */
	private int entriesOnThisDisk;
	/**
	 * <span class="en-US">Total number of entries in the central directory</span>
	 * <span class="zh-CN">中央目录中的条目总数</span>
	 */
	private int totalEntries;
	/**
	 * <span class="en-US">Size of central directory</span>
	 * <span class="zh-CN">中央目录结构占用的字节数</span>
	 */
	private long centralDirectorySize;
	/**
	 * <span class="en-US">Offset of start of central directory</span>
	 * <span class="zh-CN">中央目录开始位置偏移值</span>
	 */
	private long centralDirectoryOffset;
	/**
	 * <span class="en-US">The data bytes of the file comment</span>
	 * <span class="zh-CN">文件注释的原始字节数组</span>
	 */
	private byte[] commentBytes;
	/**
	 * <span class="en-US">The data length</span>
	 * <span class="zh-CN">数据长度</span>
	 */
	private long dataLength;

	/**
	 * <h3 class="en-US">Constructor method for the end of central directory record define</h3>
	 * <h3 class="zh-CN">中央目录结束记录定义的构造方法</h3>
	 */
	public EndCentralDirectoryRecord() {
	}

	/**
	 * <h3 class="en-US">Getter method for the number of this disk</h3>
	 * <h3 class="zh-CN">所在分卷编号的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Number of this disk</span>
	 * <span class="zh-CN">所在分卷编号</span>
	 */
	public int getDiskNumber() {
		return this.diskNumber;
	}

	/**
	 * <h3 class="en-US">Setter method for the number of this disk</h3>
	 * <h3 class="zh-CN">所在分卷编号的 Setter 方法</h3>
	 *
	 * @param diskNumber <span class="en-US">Number of this disk</span>
	 *                   <span class="zh-CN">所在分卷编号</span>
	 */
	public void setDiskNumber(final int diskNumber) {
		this.diskNumber = diskNumber;
	}

	/**
	 * <h3 class="en-US">Getter method for the disk number start of central directory</h3>
	 * <h3 class="zh-CN">中央目录开始分卷编号的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Disk number start of central directory</span>
	 * <span class="zh-CN">中央目录开始分卷编号</span>
	 */
	public int getCentralDirectoryStartDisk() {
		return this.centralDirectoryStartDisk;
	}

	/**
	 * <h3 class="en-US">Setter method for the disk number start of central directory</h3>
	 * <h3 class="zh-CN">中央目录开始分卷编号的 Setter 方法</h3>
	 *
	 * @param centralDirectoryStartDisk <span class="en-US">Disk number start of central directory</span>
	 *                                  <span class="zh-CN">中央目录开始分卷编号</span>
	 */
	public void setCentralDirectoryStartDisk(final int centralDirectoryStartDisk) {
		this.centralDirectoryStartDisk = centralDirectoryStartDisk;
	}

	/**
	 * <h3 class="en-US">Getter method for the total number of entries in the central directory on this disk</h3>
	 * <h3 class="zh-CN">此分卷上中央目录中的条目总数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Total number of entries in the central directory on this disk</span>
	 * <span class="zh-CN">此分卷上中央目录中的条目总数</span>
	 */
	public int getEntriesOnThisDisk() {
		return this.entriesOnThisDisk;
	}

	/**
	 * <h3 class="en-US">Setter method for the total number of entries in the central directory on this disk</h3>
	 * <h3 class="zh-CN">此分卷上中央目录中的条目总数的 Setter 方法</h3>
	 *
	 * @param entriesOnThisDisk <span class="en-US">Total number of entries in the central directory on this disk</span>
	 *                          <span class="zh-CN">此分卷上中央目录中的条目总数</span>
	 */
	public void setEntriesOnThisDisk(final int entriesOnThisDisk) {
		this.entriesOnThisDisk = entriesOnThisDisk;
	}

	/**
	 * <h3 class="en-US">Getter method for the total number of entries in the central directory</h3>
	 * <h3 class="zh-CN">中央目录中的条目总数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Total number of entries in the central directory</span>
	 * <span class="zh-CN">中央目录中的条目总数</span>
	 */
	public int getTotalEntries() {
		return this.totalEntries;
	}

	/**
	 * <h3 class="en-US">Setter method for the total number of entries in the central directory</h3>
	 * <h3 class="zh-CN">中央目录中的条目总数的 Setter 方法</h3>
	 *
	 * @param totalEntries <span class="en-US">Total number of entries in the central directory</span>
	 *                     <span class="zh-CN">中央目录中的条目总数</span>
	 */
	public void setTotalEntries(final int totalEntries) {
		this.totalEntries = totalEntries;
	}

	/**
	 * <h3 class="en-US">Getter method for the size of central directory</h3>
	 * <h3 class="zh-CN">中央目录结构占用的字节数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Size of central directory</span>
	 * <span class="zh-CN">中央目录结构占用的字节数</span>
	 */
	public long getCentralDirectorySize() {
		return this.centralDirectorySize;
	}

	/**
	 * <h3 class="en-US">Setter method for the size of central directory</h3>
	 * <h3 class="zh-CN">中央目录结构占用的字节数的 Setter 方法</h3>
	 *
	 * @param centralDirectorySize <span class="en-US">Size of central directory</span>
	 *                             <span class="zh-CN">中央目录结构占用的字节数</span>
	 */
	public void setCentralDirectorySize(final long centralDirectorySize) {
		this.centralDirectorySize = centralDirectorySize;
	}

	/**
	 * <h3 class="en-US">Getter method for the offset of start of central directory</h3>
	 * <h3 class="zh-CN">中央目录开始位置偏移值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Offset of start of central directory</span>
	 * <span class="zh-CN">中央目录开始位置偏移值</span>
	 */
	public long getCentralDirectoryOffset() {
		return this.centralDirectoryOffset;
	}

	/**
	 * <h3 class="en-US">Setter method for the offset of start of central directory</h3>
	 * <h3 class="zh-CN">中央目录开始位置偏移值的 Setter 方法</h3>
	 *
	 * @param centralDirectoryOffset <span class="en-US">Offset of start of central directory</span>
	 *                               <span class="zh-CN">中央目录开始位置偏移值</span>
	 */
	public void setCentralDirectoryOffset(final long centralDirectoryOffset) {
		this.centralDirectoryOffset = centralDirectoryOffset;
	}

	/**
	 * <h3 class="en-US">Getter method for the data bytes of the file comment</h3>
	 * <h3 class="zh-CN">文件注释的原始字节数组的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">The data bytes of the file comment</span>
	 * <span class="zh-CN">文件注释的原始字节数组</span>
	 */
	public byte[] getCommentBytes() {
		return this.commentBytes == null ? new byte[0] : this.commentBytes.clone();
	}

	/**
	 * <h3 class="en-US">Setter method for the data bytes of the file comment</h3>
	 * <h3 class="zh-CN">文件注释的原始字节数组的 Setter 方法</h3>
	 *
	 * @param commentBytes <span class="en-US">The data bytes of the file comment</span>
	 *                     <span class="zh-CN">文件注释的原始字节数组</span>
	 */
	public void setCommentBytes(final byte[] commentBytes) {
		this.commentBytes = commentBytes == null ? new byte[0] : commentBytes.clone();
	}

	/**
	 * <h3 class="en-US">Getter method for the data length</h3>
	 * <h3 class="zh-CN">数据长度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">The data length</span>
	 * <span class="zh-CN">数据长度</span>
	 */
	public long getDataLength() {
		return this.dataLength;
	}

	/**
	 * <h3 class="en-US">Setter method for the data length</h3>
	 * <h3 class="zh-CN">数据长度的 Setter 方法</h3>
	 *
	 * @param dataLength <span class="en-US">The data length</span>
	 *                   <span class="zh-CN">数据长度</span>
	 */
	public void setDataLength(final long dataLength) {
		this.dataLength = dataLength;
	}

	public byte[] toByteArray() throws IOException {
		if (this.diskNumber < 0 || this.diskNumber > ZipGlobals.ZIP_16_LIMIT
				|| this.centralDirectoryStartDisk < 0
				|| this.centralDirectoryStartDisk > ZipGlobals.ZIP_16_LIMIT
				|| this.entriesOnThisDisk < 0 || this.entriesOnThisDisk > ZipGlobals.ZIP_16_LIMIT
				|| this.totalEntries < 0 || this.totalEntries > ZipGlobals.ZIP_16_LIMIT) {
			throw new IOException("EOCD disk or entry count exceeds its 16-bit field");
		}

		if (this.centralDirectorySize < 0L || this.centralDirectorySize > ZipGlobals.ZIP_64_LIMIT
				|| this.centralDirectoryOffset < 0L
				|| this.centralDirectoryOffset > ZipGlobals.ZIP_64_LIMIT) {
			throw new IOException("EOCD central directory size or offset exceeds its 32-bit field");
		}

		byte[] comment = this.getCommentBytes();
		if (comment.length > ZipGlobals.ZIP_16_LIMIT) {
			throw new IOException("ZIP archive comment exceeds 65535 bytes");
		}

		ByteBuffer buffer = ByteBuffer.allocate(22 + comment.length).order(ByteOrder.LITTLE_ENDIAN);

		buffer.putInt((int) ZipGlobals.END_OF_CENTRAL_DIRECTORY_SIGNATURE);
		buffer.putShort((short) this.diskNumber);
		buffer.putShort((short) this.centralDirectoryStartDisk);
		buffer.putShort((short) this.entriesOnThisDisk);
		buffer.putShort((short) this.totalEntries);
		buffer.putInt((int) this.centralDirectorySize);
		buffer.putInt((int) this.centralDirectoryOffset);
		buffer.putShort((short) comment.length);
		buffer.put(comment);

		return buffer.array();
	}
}
