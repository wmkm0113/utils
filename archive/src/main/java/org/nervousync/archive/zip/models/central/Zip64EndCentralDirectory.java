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

import jakarta.annotation.Nonnull;
import org.nervousync.archive.commons.ZipGlobals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <h2 class="en-US">Zip64 end of central directory define</h2>
 * <h2 class="zh-CN">Zip64 中央目录结束定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class Zip64EndCentralDirectory {

	/**
	 * <span class="en-US">Zip64 end of central directory record</span>
	 * <span class="zh-CN">Zip64 中央目录结束记录</span>
	 */
	@Nonnull
	private final Record record;
	/**
	 * <span class="en-US">Zip64 end of central directory locator</span>
	 * <span class="zh-CN">Zip64 中央目录结束定位器</span>
	 */
	@Nonnull
	private final Locator locator;
	/**
	 * <span class="en-US">Zip64 end of central directory protection</span>
	 * <span class="zh-CN">Zip64 中央目录保护</span>
	 */
	private Protection protection;

	/**
	 * <h3 class="en-US">Constructor method for the Zip64 end of central directory define</h3>
	 * <h3 class="zh-CN">Zip64 中央目录结束定义的构造方法</h3>
	 */
	public Zip64EndCentralDirectory() {
		this(new Record(), new Locator());
	}

	/**
	 * <h3 class="en-US">Constructor method for the Zip64 end of central directory define</h3>
	 * <h3 class="zh-CN">Zip64 中央目录结束定义的构造方法</h3>
	 *
	 * @param record  <span class="en-US">Zip64 end of central directory record</span>
	 *                <span class="zh-CN">Zip64 中央目录结束记录</span>
	 * @param locator <span class="en-US">Zip64 end of central directory locator</span>
	 *                <span class="zh-CN">Zip64 中央目录结束定位器</span>
	 */
	public Zip64EndCentralDirectory(@Nonnull final Record record, @Nonnull final Locator locator) {
		this.record = record;
		this.locator = locator;
	}

	/**
	 * <h3 class="en-US">Getter method for the Zip64 end of central directory record</h3>
	 * <h3 class="zh-CN">Zip64 中央目录结束记录的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Zip64 end of central directory record</span>
	 * <span class="zh-CN">Zip64 中央目录结束记录</span>
	 */
	@Nonnull
	public Record getRecord() {
		return this.record;
	}

	/**
	 * <h3 class="en-US">Getter method for the Zip64 end of central directory locator</h3>
	 * <h3 class="zh-CN">Zip64 中央目录结束定位器的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Zip64 end of central directory locator</span>
	 * <span class="zh-CN">Zip64 中央目录结束定位器</span>
	 */
	@Nonnull
	public Locator getLocator() {
		return this.locator;
	}

	/**
	 * <h3 class="en-US">Getter method for the Zip64 end of central directory protection</h3>
	 * <h3 class="zh-CN">Zip64 中央目录保护的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Zip64 end of central directory protection</span>
	 * <span class="zh-CN">Zip64 中央目录保护</span>
	 */
	public Protection getProtection() {
		return this.protection;
	}

	/**
	 * <h3 class="en-US">Setter method for the Zip64 end of central directory protection</h3>
	 * <h3 class="zh-CN">Zip64 中央目录保护的 Setter 方法</h3>
	 *
	 * @param protection <span class="en-US">Zip64 end of central directory protection</span>
	 *                   <span class="zh-CN">Zip64 中央目录保护</span>
	 */
	public void setProtection(final Protection protection) {
		this.protection = protection;
	}

	/**
	 * <h2 class="en-US">Zip64 end of central directory record define</h2>
	 * <h2 class="zh-CN">Zip64 中央目录结束记录定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	public static final class Record {

		/**
		 * <span class="en-US">Record size</span>
		 * <span class="zh-CN">记录长度</span>
		 */
		private long recordSize;
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
		 * <span class="en-US">Number of this disk</span>
		 * <span class="zh-CN">所在分卷编号</span>
		 */
		private long diskNumber;
		/**
		 * <span class="en-US">Disk number start of central directory</span>
		 * <span class="zh-CN">中央目录开始分卷编号</span>
		 */
		private long centralDirectoryStartDisk;
		/**
		 * <span class="en-US">Total number of entries in the central directory on this disk</span>
		 * <span class="zh-CN">此分卷上中央目录中的条目总数</span>
		 */
		private long entriesOnThisDisk;
		/**
		 * <span class="en-US">Total number of entries in the central directory</span>
		 * <span class="zh-CN">中央目录中的条目总数</span>
		 */
		private long totalEntries;
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
		 * <span class="en-US">Extensible data sector</span>
		 * <span class="zh-CN">附加数据块</span>
		 */
		private byte[] extensibleDataSector = new byte[0];

		/**
		 * <h3 class="en-US">Constructor method for the Zip64 end of central directory record define</h3>
		 * <h3 class="zh-CN">Zip64 中央目录结束记录定义的构造方法</h3>
		 */
		public Record() {
		}

		/**
		 * <h3 class="en-US">Getter method for the record size</h3>
		 * <h3 class="zh-CN">记录长度的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Record size</span>
		 * <span class="zh-CN">记录长度</span>
		 */
		public long getRecordSize() {
			return this.recordSize;
		}

		/**
		 * <h3 class="en-US">Setter method for the record size</h3>
		 * <h3 class="zh-CN">记录长度的 Setter 方法</h3>
		 *
		 * @param recordSize <span class="en-US">Record size</span>
		 *                   <span class="zh-CN">记录长度</span>
		 */
		public void setRecordSize(final long recordSize) {
			this.recordSize = recordSize;
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
		 * <h3 class="en-US">Getter method for the number of this disk</h3>
		 * <h3 class="zh-CN">所在分卷编号的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Number of this disk</span>
		 * <span class="zh-CN">所在分卷编号</span>
		 */
		public long getDiskNumber() {
			return this.diskNumber;
		}

		/**
		 * <h3 class="en-US">Setter method for the number of this disk</h3>
		 * <h3 class="zh-CN">所在分卷编号的 Setter 方法</h3>
		 *
		 * @param diskNumber <span class="en-US">Number of this disk</span>
		 *                   <span class="zh-CN">所在分卷编号</span>
		 */
		public void setDiskNumber(final long diskNumber) {
			this.diskNumber = diskNumber;
		}

		/**
		 * <h3 class="en-US">Getter method for the disk number start of central directory</h3>
		 * <h3 class="zh-CN">中央目录开始分卷编号的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Disk number start of central directory</span>
		 * <span class="zh-CN">中央目录开始分卷编号</span>
		 */
		public long getCentralDirectoryStartDisk() {
			return this.centralDirectoryStartDisk;
		}

		/**
		 * <h3 class="en-US">Setter method for the disk number start of central directory</h3>
		 * <h3 class="zh-CN">中央目录开始分卷编号的 Setter 方法</h3>
		 *
		 * @param centralDirectoryStartDisk <span class="en-US">Disk number start of central directory</span>
		 *                                  <span class="zh-CN">中央目录开始分卷编号</span>
		 */
		public void setCentralDirectoryStartDisk(final long centralDirectoryStartDisk) {
			this.centralDirectoryStartDisk = centralDirectoryStartDisk;
		}

		/**
		 * <h3 class="en-US">Getter method for the total number of entries in the central directory on this disk</h3>
		 * <h3 class="zh-CN">此分卷上中央目录中的条目总数的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Total number of entries in the central directory on this disk</span>
		 * <span class="zh-CN">此分卷上中央目录中的条目总数</span>
		 */
		public long getEntriesOnThisDisk() {
			return this.entriesOnThisDisk;
		}

		/**
		 * <h3 class="en-US">Setter method for the total number of entries in the central directory on this disk</h3>
		 * <h3 class="zh-CN">此分卷上中央目录中的条目总数的 Setter 方法</h3>
		 *
		 * @param entriesOnThisDisk <span class="en-US">Total number of entries in the central directory on this disk</span>
		 *                          <span class="zh-CN">此分卷上中央目录中的条目总数</span>
		 */
		public void setEntriesOnThisDisk(final long entriesOnThisDisk) {
			this.entriesOnThisDisk = entriesOnThisDisk;
		}

		/**
		 * <h3 class="en-US">Getter method for the total number of entries in the central directory</h3>
		 * <h3 class="zh-CN">中央目录中的条目总数的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Total number of entries in the central directory</span>
		 * <span class="zh-CN">中央目录中的条目总数</span>
		 */
		public long getTotalEntries() {
			return this.totalEntries;
		}

		/**
		 * <h3 class="en-US">Setter method for the total number of entries in the central directory</h3>
		 * <h3 class="zh-CN">中央目录中的条目总数的 Setter 方法</h3>
		 *
		 * @param totalEntries <span class="en-US">Total number of entries in the central directory</span>
		 *                     <span class="zh-CN">中央目录中的条目总数</span>
		 */
		public void setTotalEntries(final long totalEntries) {
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
		 * <h3 class="en-US">Getter method for the extensible data sector</h3>
		 * <h3 class="zh-CN">附加数据块的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Extensible data sector</span>
		 * <span class="zh-CN">附加数据块</span>
		 */
		public byte[] getExtensibleDataSector() {
			return (this.extensibleDataSector == null) ? new byte[0] : this.extensibleDataSector.clone();
		}

		/**
		 * <h3 class="en-US">Setter method for the extensible data sector</h3>
		 * <h3 class="zh-CN">附加数据块的 Setter 方法</h3>
		 *
		 * @param extensibleDataSector <span class="en-US">Extensible data sector</span>
		 *                             <span class="zh-CN">附加数据块</span>
		 */
		public void setExtensibleDataSector(final byte[] extensibleDataSector) {
			this.extensibleDataSector = (extensibleDataSector == null) ? new byte[0] : extensibleDataSector.clone();
		}

		public byte[] toByteArray(final Protection protection) throws IOException {
			if (this.diskNumber < 0L || this.diskNumber > ZipGlobals.ZIP_64_LIMIT
					|| this.centralDirectoryStartDisk < 0L
					|| this.centralDirectoryStartDisk > this.diskNumber
					|| this.entriesOnThisDisk < 0L
					|| this.totalEntries < this.entriesOnThisDisk
					|| this.centralDirectorySize < 0L
					|| this.centralDirectoryOffset < 0L) {
				throw new IOException("Invalid ZIP64 EOCD fields");
			}
			byte[] extraBytes = this.extensibleDataSector == null ? new byte[0] : this.extensibleDataSector;
			byte[] hash = new byte[0];
			int protectionLength = 0;
			int minimumVersion = ZipGlobals.ZIP_64_VERSION;

			if (protection != null) {
				hash = protection.getHashData();
				if (hash == null) {
					hash = new byte[0];
				}
				if (hash.length > ZipGlobals.ZIP_16_LIMIT
						|| protection.getCompressedSize() < 0L
						|| protection.getUncompressedSize() < 0L) {
					throw new IOException("Invalid ZIP64 EOCD protection fields");
				}
				protectionLength = 28 + hash.length;
				minimumVersion = 62;
			}

			long recordSize = 44L + protectionLength + extraBytes.length;
			if (recordSize > Integer.MAX_VALUE - 12L) {
				throw new IOException("ZIP64 EOCD is too large for a byte array");
			}

			ByteBuffer buffer = ByteBuffer.allocate((int) (12L + recordSize)).order(ByteOrder.LITTLE_ENDIAN);

			buffer.putInt((int) ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE);
			buffer.putLong(recordSize);
			buffer.putShort((short) this.madeVersion);
			buffer.putShort((short) Math.max(this.extractNeeded, minimumVersion));
			buffer.putInt((int) this.diskNumber);
			buffer.putInt((int) this.centralDirectoryStartDisk);
			buffer.putLong(this.entriesOnThisDisk);
			buffer.putLong(this.totalEntries);
			buffer.putLong(this.centralDirectorySize);
			buffer.putLong(this.centralDirectoryOffset);

			if (protection != null) {
				buffer.putShort((short) protection.getCompressionMethod());
				buffer.putLong(protection.getCompressedSize());
				buffer.putLong(protection.getUncompressedSize());
				buffer.putShort((short) protection.getAlgorithm());
				buffer.putShort((short) protection.getKeyBitLength());
				buffer.putShort((short) protection.getFlags());
				buffer.putShort((short) protection.getHashAlgorithm());
				buffer.putShort((short) hash.length);
				buffer.put(hash);
			}

			buffer.put(extraBytes);
			return buffer.array();
		}

	}

	/**
	 * <h2 class="en-US">Zip64 end of central directory locator define</h2>
	 * <h2 class="zh-CN">Zip64 中央目录结束定位器定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	public static final class Locator {

		/**
		 * <span class="en-US">The disk number of Zip64 end of central directory record</span>
		 * <span class="zh-CN">Zip64 中央目录终止记录所在分卷</span>
		 */
		private long zip64EndRecordDiskNumber;
		/**
		 * <span class="en-US">The offset of Zip64 end of central directory record</span>
		 * <span class="zh-CN">Zip64 中央目录终止记录偏移值</span>
		 */
		private long zip64EndRecordOffset;
		/**
		 * <span class="en-US">The total number of Zip archive</span>
		 * <span class="zh-CN">整个 Zip 文件包含的分卷数量</span>
		 */
		private long totalNumberOfDisks;

		/**
		 * <h3 class="en-US">Constructor method for the Zip64 end of central directory locator define</h3>
		 * <h3 class="zh-CN">Zip64 中央目录结束定位器定义的构造方法</h3>
		 */
		public Locator() {
		}

		/**
		 * <h3 class="en-US">Getter method for the disk number of Zip64 end of central directory record</h3>
		 * <h3 class="zh-CN">Zip64 中央目录终止记录所在分卷的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">The disk number of Zip64 end of central directory record</span>
		 * <span class="zh-CN">Zip64 中央目录终止记录所在分卷</span>
		 */
		public long getZip64EndRecordDiskNumber() {
			return this.zip64EndRecordDiskNumber;
		}

		/**
		 * <h3 class="en-US">Setter method for the disk number of Zip64 end of central directory record</h3>
		 * <h3 class="zh-CN">Zip64 中央目录终止记录所在分卷的 Setter 方法</h3>
		 *
		 * @param zip64EndRecordDiskNumber <span class="en-US">The disk number of Zip64 end of central directory record</span>
		 *                                 <span class="zh-CN">Zip64 中央目录终止记录所在分卷</span>
		 */
		public void setZip64EndRecordDiskNumber(final long zip64EndRecordDiskNumber) {
			if (zip64EndRecordDiskNumber < 0L || zip64EndRecordDiskNumber > 0xFFFFFFFFL) {
				throw new IllegalArgumentException();
			}
			this.zip64EndRecordDiskNumber = zip64EndRecordDiskNumber;
		}

		/**
		 * <h3 class="en-US">Getter method for the offset of Zip64 end of central directory record</h3>
		 * <h3 class="zh-CN">Zip64 中央目录终止记录偏移值的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">The offset of Zip64 end of central directory record</span>
		 * <span class="zh-CN">Zip64 中央目录终止记录偏移值</span>
		 */
		public long getZip64EndRecordOffset() {
			return this.zip64EndRecordOffset;
		}

		/**
		 * <h3 class="en-US">Setter method for the offset of Zip64 end of central directory record</h3>
		 * <h3 class="zh-CN">Zip64 中央目录终止记录偏移值的 Setter 方法</h3>
		 *
		 * @param zip64EndRecordOffset <span class="en-US">The offset of Zip64 end of central directory record</span>
		 *                             <span class="zh-CN">Zip64 中央目录终止记录偏移值</span>
		 */
		public void setZip64EndRecordOffset(final long zip64EndRecordOffset) {
			if (zip64EndRecordOffset < 0L) {
				throw new IllegalArgumentException("zip64EndRecordOffset must be non-negative");
			}
			this.zip64EndRecordOffset = zip64EndRecordOffset;
		}

		/**
		 * <h3 class="en-US">Getter method for the total number of Zip archive</h3>
		 * <h3 class="zh-CN">整个 Zip 文件包含的分卷数量的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">The total number of Zip archive</span>
		 * <span class="zh-CN">整个 Zip 文件包含的分卷数量</span>
		 */
		public long getTotalNumberOfDisks() {
			return this.totalNumberOfDisks;
		}

		/**
		 * <h3 class="en-US">Setter method for the total number of Zip archive</h3>
		 * <h3 class="zh-CN">整个 Zip 文件包含的分卷数量的 Setter 方法</h3>
		 *
		 * @param totalNumberOfDisks <span class="en-US">The total number of Zip archive</span>
		 *                           <span class="zh-CN">整个 Zip 文件包含的分卷数量</span>
		 */
		public void setTotalNumberOfDisks(final long totalNumberOfDisks) {
			if (totalNumberOfDisks <= 0L || totalNumberOfDisks > 0xFFFFFFFFL) {
				throw new IllegalArgumentException();
			}
			this.totalNumberOfDisks = totalNumberOfDisks;
		}

		public byte[] toByteArray() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);

			buffer.putInt((int) ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE);
			buffer.putInt((int) this.zip64EndRecordDiskNumber);
			buffer.putLong(this.zip64EndRecordOffset);
			buffer.putInt((int) this.totalNumberOfDisks);

			return buffer.array();
		}
	}

	/**
	 * <h2 class="en-US">Zip64 central directory protection define</h2>
	 * <h2 class="zh-CN">Zip64 中央目录保护定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	public static final class Protection {

		/**
		 * <span class="en-US">Compression method code</span>
		 * <span class="zh-CN">压缩方法编号</span>
		 */
		private int compressionMethod;
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
		 * <span class="en-US">Encryption algorithm code</span>
		 * <span class="zh-CN">加密算法代码</span>
		 */
		private int algorithm;
		/**
		 * <span class="en-US">Encryption key bit length</span>
		 * <span class="zh-CN">加密密钥位数</span>
		 */
		private int keyBitLength;
		/**
		 * <span class="en-US">Credentials and procedures required for decryption</span>
		 * <span class="zh-CN">解密所需凭据和处理方式</span>
		 */
		private int flags;
		/**
		 * <span class="en-US">Hash algorithm code</span>
		 * <span class="zh-CN">摘要算法代码</span>
		 */
		private int hashAlgorithm;
		/**
		 * <span class="en-US">Hash result data</span>
		 * <span class="zh-CN">摘要结果</span>
		 */
		private byte[] hashData = new byte[0];

		/**
		 * <h3 class="en-US">Constructor method for the Zip64 central directory protection define</h3>
		 * <h3 class="zh-CN">Zip64 中央目录保护定义的构造方法</h3>
		 */
		public Protection() {
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
		 * <h3 class="en-US">Getter method for the encryption algorithm code</h3>
		 * <h3 class="zh-CN">加密算法代码的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Encryption algorithm code</span>
		 * <span class="zh-CN">加密算法代码</span>
		 */
		public int getAlgorithm() {
			return this.algorithm;
		}

		/**
		 * <h3 class="en-US">Setter method for the encryption algorithm code</h3>
		 * <h3 class="zh-CN">加密算法代码的 Setter 方法</h3>
		 *
		 * @param algorithm <span class="en-US">Encryption algorithm code</span>
		 *                  <span class="zh-CN">加密算法代码</span>
		 */
		public void setAlgorithm(final int algorithm) {
			this.algorithm = algorithm;
		}

		/**
		 * <h3 class="en-US">Getter method for the encryption key bit length</h3>
		 * <h3 class="zh-CN">加密密钥位数的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Encryption key bit length</span>
		 * <span class="zh-CN">加密密钥位数</span>
		 */
		public int getKeyBitLength() {
			return this.keyBitLength;
		}

		/**
		 * <h3 class="en-US">Setter method for the encryption key bit length</h3>
		 * <h3 class="zh-CN">加密密钥位数的 Setter 方法</h3>
		 *
		 * @param keyBitLength <span class="en-US">Encryption key bit length</span>
		 *                     <span class="zh-CN">加密密钥位数</span>
		 */
		public void setKeyBitLength(final int keyBitLength) {
			this.keyBitLength = keyBitLength;
		}

		/**
		 * <h3 class="en-US">Getter method for the credentials and procedures required for decryption</h3>
		 * <h3 class="zh-CN">解密所需凭据和处理方式的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Credentials and procedures required for decryption</span>
		 * <span class="zh-CN">解密所需凭据和处理方式</span>
		 */
		public int getFlags() {
			return this.flags;
		}

		/**
		 * <h3 class="en-US">Setter method for the credentials and procedures required for decryption</h3>
		 * <h3 class="zh-CN">解密所需凭据和处理方式的 Setter 方法</h3>
		 *
		 * @param flags <span class="en-US">Credentials and procedures required for decryption</span>
		 *              <span class="zh-CN">解密所需凭据和处理方式</span>
		 */
		public void setFlags(final int flags) {
			this.flags = flags;
		}

		/**
		 * <h3 class="en-US">Getter method for the hash algorithm code</h3>
		 * <h3 class="zh-CN">摘要算法代码的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Hash algorithm code</span>
		 * <span class="zh-CN">摘要算法代码</span>
		 */
		public int getHashAlgorithm() {
			return this.hashAlgorithm;
		}

		/**
		 * <h3 class="en-US">Setter method for the hash algorithm code</h3>
		 * <h3 class="zh-CN">摘要算法代码的 Setter 方法</h3>
		 *
		 * @param hashAlgorithm <span class="en-US">Hash algorithm code</span>
		 *                      <span class="zh-CN">摘要算法代码</span>
		 */
		public void setHashAlgorithm(final int hashAlgorithm) {
			this.hashAlgorithm = hashAlgorithm;
		}

		/**
		 * <h3 class="en-US">Getter method for the hash result data</h3>
		 * <h3 class="zh-CN">摘要结果的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Hash result data</span>
		 * <span class="zh-CN">摘要结果</span>
		 */
		public byte[] getHashData() {
			return this.hashData == null ? new byte[0] : this.hashData.clone();
		}

		/**
		 * <h3 class="en-US">Setter method for the hash result data</h3>
		 * <h3 class="zh-CN">摘要结果的 Setter 方法</h3>
		 *
		 * @param hashData <span class="en-US">Hash result data</span>
		 *                 <span class="zh-CN">摘要结果</span>
		 */
		public void setHashData(final byte[] hashData) {
			this.hashData = (hashData == null) ? new byte[0] : hashData.clone();
		}

		/**
		 * <h3 class="en-US">Check the encrypted protection flag</h3>
		 * <h3 class="zh-CN">检查是否加密保护</h3>
		 *
		 * @return <span class="en-US">Check result</span>
		 * <span class="zh-CN">检查结果</span>
		 */
		public boolean encrypted() {
			return this.algorithm != ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE;
		}

		/**
		 * <h3 class="en-US">Check the compressed data flag</h3>
		 * <h3 class="zh-CN">检查是否压缩数据</h3>
		 *
		 * @return <span class="en-US">Check result</span>
		 * <span class="zh-CN">检查结果</span>
		 */
		public boolean compressed() {
			return this.compressionMethod != ZipGlobals.COMP_STORE;
		}

		/**
		 * <h3 class="en-US">Check the contains hash validation flag</h3>
		 * <h3 class="zh-CN">检查是否包含哈希验证</h3>
		 *
		 * @return <span class="en-US">Check result</span>
		 * <span class="zh-CN">检查结果</span>
		 */
		public boolean hashed() {
			return this.hashAlgorithm != ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE;
		}
	}
}
