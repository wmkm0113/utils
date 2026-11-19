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

package org.nervousync.archive.zip.io;

import jakarta.annotation.Nonnull;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.DataFile;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <h2 class="en-US">Zip archive file output stream</h2>
 * <h2 class="zh-CN">Zip 归档文件输出流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ZipArchiveOutputStream extends OutputStream {

	/**
	 * <span class="en-US">One byte buffer</span>
	 * <span class="zh-CN">单字节缓冲区</span>
	 */
	private final byte[] oneByte = new byte[1];
	/**
	 * <span class="en-US">Archive file path</span>
	 * <span class="zh-CN">归档文件地址</span>
	 */
	private final String archivePath;
	/**
	 * <span class="en-US">Archive file template path</span>
	 * <span class="zh-CN">归档文件模板地址</span>
	 */
	private final String templatePath;
	/**
	 * <span class="en-US">Split limit size</span>
	 * <span class="zh-CN">分卷大小</span>
	 */
	private final ZipOptions zipOptions;
	/**
	 * <span class="en-US">Split count of archive</span>
	 * <span class="zh-CN">分卷计数</span>
	 */
	private long splitCount = 1L;
	/**
	 * <span class="en-US">Current index value</span>
	 * <span class="zh-CN">当前索引值</span>
	 */
	private long diskNumber = 0L;
	/**
	 * <span class="en-US">Current disk entries count</span>
	 * <span class="zh-CN">当前分卷条目计数</span>
	 */
	private long diskEntries = 0L;
	/**
	 * <span class="en-US">Total size</span>
	 * <span class="zh-CN">总文件长度</span>
	 */
	private long totalSize = 0L;
	/**
	 * <span class="en-US">Processing central directory flag</span>
	 * <span class="zh-CN">处理中央目录标记</span>
	 */
	private boolean processingCentralDirectory = false;
	/**
	 * <span class="en-US">Disk number of central directory start</span>
	 * <span class="zh-CN">中央目录起始磁盘编号</span>
	 */
	private long centralDirectoryStartDisk = Globals.DEFAULT_VALUE_LONG;
	/**
	 * <span class="en-US">Offset of central directory start</span>
	 * <span class="zh-CN">中央目录起始偏移值</span>
	 */
	private long centralDirectoryOffset = Globals.DEFAULT_VALUE_LONG;
	/**
	 * <span class="en-US">Current standard file instance object</span>
	 * <span class="zh-CN">当前文件实例对象</span>
	 */
	@Nonnull
	private DataFile dataFile;
	/**
	 * <span class="en-US">Closed flag</span>
	 * <span class="zh-CN">关闭标记</span>
	 */
	private boolean closed = false;
	/**
	 * <span class="en-US">Finished flag</span>
	 * <span class="zh-CN">完成标记</span>
	 */
	private boolean finished = false;

	/**
	 * <h3 class="en-US">Constructor method for the zip archive file output stream</h3>
	 * <h3 class="zh-CN">Zip 归档文件输出流的构造方法</h3>
	 *
	 * @param tempDirectory <span class="en-US">Output temporary directory</span>
	 *                      <span class="zh-CN">输出临时目录</span>
	 * @param dataFile      <span class="en-US">Data file</span>
	 *                      <span class="zh-CN">数据文件</span>
	 * @param zipOptions    <span class="en-US">Zip options</span>
	 *                      <span class="zh-CN">Zip 选项</span>
	 */
	public ZipArchiveOutputStream(@Nonnull final Path tempDirectory, final DataFile dataFile,
	                              final ZipOptions zipOptions) throws IOException {
		if (zipOptions.isSplitArchive() && zipOptions.getSplitType() == ZipOptions.SplitType.DETECT) {
			throw new ArchiveException(0x0000001A0041L);
		}
		this.archivePath = dataFile.filePath();
		this.zipOptions = zipOptions;
		String fileName = StringUtils.getFilename(this.archivePath);
		if (this.zipOptions.isSplitArchive()) {
			long splitLength = this.zipOptions.getSplitLength();
			if (splitLength <= 0L) {
				throw new IllegalArgumentException("Split length must be positive");
			}

			if (this.zipOptions.getSplitType() == ZipOptions.SplitType.NATIVE
					&& (splitLength < 0x10000L || splitLength > ZipGlobals.ZIP_64_LIMIT)) {
				throw new IllegalArgumentException("Native ZIP split length must be between 65536 and 0xFFFFFFFF");
			}
			if (this.zipOptions.getSplitType() == ZipOptions.SplitType.NUMBERED) {
				this.templatePath =
						tempDirectory.toAbsolutePath().normalize() + Globals.DEFAULT_PAGE_SEPARATOR + fileName + ".";
			} else {
				this.templatePath =
						tempDirectory.toAbsolutePath().normalize() + Globals.DEFAULT_PAGE_SEPARATOR
								+ fileName.substring(0, fileName.lastIndexOf('.')) + ".z";
			}
		} else {
			this.templatePath = tempDirectory.toAbsolutePath().normalize() + Globals.DEFAULT_PAGE_SEPARATOR + fileName;
		}
		this.dataFile = dataFile.newFile(this.currentPath());
		if (this.zipOptions.isSplitArchive() && this.zipOptions.getSplitType() == ZipOptions.SplitType.NATIVE) {
			//  Write temporary header id
			this.dataFile.write(ByteBuffer.allocate(Integer.BYTES)
					.order(ByteOrder.LITTLE_ENDIAN)
					.putInt((int) ZipGlobals.TEMP_SPLIT_SIGNATURE)
					.array());
			this.totalSize += Integer.BYTES;
		}
	}

	/**
	 * <h3 class="en-US">Begin processing central directory</h3>
	 * <h3 class="zh-CN">开始处理中央目录</h3>
	 */
	public void beginCentralDirectory() {
		if (this.processingCentralDirectory) {
			throw new IllegalStateException("Central directory already processing");
		}

		this.processingCentralDirectory = true;
		this.diskEntries = 0L;
		this.centralDirectoryStartDisk = Globals.DEFAULT_VALUE_LONG;
		this.centralDirectoryOffset = Globals.DEFAULT_VALUE_LONG;
	}

	/**
	 * <h3 class="en-US">End process central directory</h3>
	 * <h3 class="zh-CN">完成处理中央目录</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when read offset value</span>
	 *                     <span class="zh-CN">读取偏移值出错</span>
	 */
	public void endCentralDirectory() throws IOException {
		if (!this.processingCentralDirectory) {
			throw new IllegalStateException("Central directory not processed");
		}

		if (this.centralDirectoryStartDisk < 0L) {
			this.centralDirectoryStartDisk = this.diskNumber();
			this.centralDirectoryOffset = this.offset();
		}

		this.processingCentralDirectory = false;
	}

	/**
	 * <h3 class="en-US">Getter method for the disk number of central directory start</h3>
	 * <h3 class="zh-CN">中央目录起始磁盘编号的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Disk number of central directory start</span>
	 * <span class="zh-CN">中央目录起始磁盘编号</span>
	 */
	public long getCentralDirectoryStartDisk() {
		return this.centralDirectoryStartDisk;
	}

	/**
	 * <h3 class="en-US">Getter method for the offset of central directory start</h3>
	 * <h3 class="zh-CN">中央目录起始偏移值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Offset of central directory start</span>
	 * <span class="zh-CN">中央目录起始偏移值</span>
	 */
	public long getCentralDirectoryOffset() {
		return this.centralDirectoryOffset;
	}

	/**
	 * <h3 class="en-US">Get the current file path</h3>
	 * <h3 class="zh-CN">获取当前文件地址</h3>
	 *
	 * @return <span class="en-US">Current file path</span>
	 * <span class="zh-CN">当前文件地址</span>
	 */
	private String currentPath() {
		String splitPath = this.templatePath;
		if (this.zipOptions.isSplitArchive()) {
			splitPath += this.numberSuffix(this.splitCount);
		}
		return splitPath;
	}

	/**
	 * <h3 class="en-US">Process split number</h3>
	 * <h3 class="zh-CN">处理分卷序号</h3>
	 *
	 * @param splitCount <span class="en-US">Split count of archive</span>
	 *                   <span class="zh-CN">分卷计数</span>
	 * @return <span class="en-US">Split number string</span>
	 * <span class="zh-CN">分卷序号</span>
	 */
	private String numberSuffix(final long splitCount) {
		if (!this.zipOptions.isSplitArchive()) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		StringBuilder number = new StringBuilder(Long.toString(splitCount));
		int minLength = (this.zipOptions.getSplitType() == ZipOptions.SplitType.NUMBERED) ? 3 : 2;
		while (number.length() < minLength) {
			number.insert(0, "0");
		}
		return number.toString();
	}

	/**
	 * <h3 class="en-US">Get current archive file path</h3>
	 * <h3 class="zh-CN">获取当前归档文件路径</h3>
	 *
	 * @return <span class="en-US">Current zip file path</span>
	 * <span class="zh-CN">当前压缩文件路径</span>
	 */
	public String archivePath() {
		return this.archivePath;
	}

	@Override
	public void write(final int b) throws IOException {
		this.oneByte[0] = (byte) b;
		this.write(this.oneByte, 0, 1);
	}

	@Override
	public void write(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		Objects.requireNonNull(buffer, "buffer");
		Objects.checkFromIndexSize(offset, length, buffer.length);
		this.ensureOpen();

		if (length == 0) {
			return;
		}

		if (this.zipOptions.isSplitArchive()) {
			final long splitLength = this.zipOptions.getSplitLength();
			int position = offset;
			int remaining = length;

			while (remaining > 0) {
				long available = splitLength - this.dataFile.getFilePointer();
				if (available < 0L) {
					throw new ArchiveException(0x0000001A001DL);
				}

				if (available == 0L) {
					this.nextSplitFile();
					continue;
				}

				int chunkLength = (int) Math.min(available, remaining);
				this.writeChunk(buffer, position, chunkLength);

				position += chunkLength;
				remaining -= chunkLength;
			}
		} else {
			this.writeChunk(buffer, offset, length);
		}
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		try {
			this.dataFile.close();
		} finally {
			this.closed = true;
		}
	}

	public void finish() throws IOException {
		if (this.finished) {
			return;
		}

		this.ensureOpen();

		if (this.zipOptions.isSplitArchive()
				&& this.zipOptions.getSplitType() == ZipOptions.SplitType.NATIVE
				&& this.splitCount > 1L) {
			try (DataFile dataFile = this.dataFile.newFile(this.templatePath + this.numberSuffix(1L))) {
				dataFile.seek(0L);
				dataFile.write(ByteBuffer.allocate(Integer.BYTES)
						.order(ByteOrder.LITTLE_ENDIAN)
						.putInt((int) ZipGlobals.NATIVE_SPLIT_SIGNATURE)
						.array());
			}
		}

		this.close();
		this.finished = true;
	}

	/**
	 * <h3 class="en-US">Publish files mapping table</h3>
	 * <h3 class="zh-CN">发布文件映射表</h3>
	 *
	 * @return <span class="en-US">Files mapping table</span>
	 * <span class="zh-CN">文件映射表</span>
	 * @throws IOException <span class="en-US">Published</span>
	 *                     <span class="zh-CN">已经发布过</span>
	 */
	public Map<Path, Path> publishFiles() throws IOException {
		if (!this.finished) {
			throw new ArchiveException(0x0000001A0048L);
		}
		Path target = Path.of(this.archivePath).toAbsolutePath().normalize();
		Map<Path, Path> files = new LinkedHashMap<>();

		if (!this.zipOptions.isSplitArchive()) {
			files.put(Path.of(this.templatePath), target);
		} else {
			String name = target.getFileName().toString();
			String stem = name.substring(0, name.lastIndexOf('.'));
			for (long number = 1L; number <= this.splitCount; number++) {
				String numberSuffix = this.numberSuffix(number);
				Path temporary = Path.of(this.templatePath + numberSuffix);
				Path destination;
				if (this.zipOptions.getSplitType() == ZipOptions.SplitType.NATIVE) {
					destination =
							(number == this.splitCount) ? target : target.resolveSibling(stem + ".z" + numberSuffix);
				} else {
					destination = target.resolveSibling(name + "." + numberSuffix);
				}
				files.put(temporary, destination);
			}
		}
		return Collections.unmodifiableMap(files);
	}

	/**
	 * <h3 class="en-US">Write general file header data bytes</h3>
	 * <h3 class="zh-CN">写入通用文件头信息</h3>
	 *
	 * @param headerBytes <span class="en-US">General file header data bytes</span>
	 *                    <span class="zh-CN">通用文件头数据</span>
	 * @throws IOException <span class="en-US">If an error occurs when write data</span>
	 *                     <span class="zh-CN">写入数据出错</span>
	 */
	public void writeHeader(final byte[] headerBytes) throws IOException {
		Objects.requireNonNull(headerBytes, "headerBytes");
		this.ensureFits(headerBytes.length);
		this.write(headerBytes, 0, headerBytes.length);
		if (this.processingCentralDirectory) {
			this.diskEntries++;
		}
	}

	/**
	 * <h3 class="en-US">Ensure available length</h3>
	 * <h3 class="zh-CN">确认剩余长度</h3>
	 *
	 * @param length <span class="en-US">Need available length</span>
	 *               <span class="zh-CN">需要的长度</span>
	 * @throws IOException <span class="en-US">If length invalid or an error occurs when switch split archive file</span>
	 *                     <span class="zh-CN">长度错误或切换分卷文件出错</span>
	 */
	public void ensureFits(final long length) throws IOException {
		this.ensureOpen();

		if (length < 0L) {
			throw new ArchiveException(0x0000001A0011L);
		}

		if (this.zipOptions.isSplitArchive() && this.zipOptions.getSplitType() == ZipOptions.SplitType.NATIVE) {
			final long splitLength = this.zipOptions.getSplitLength();
			if (length > splitLength) {
				throw new ArchiveException(0x0000001A0047L);
			}

			long available = splitLength - this.dataFile.getFilePointer();
			if (available < 0L) {
				throw new ArchiveException(0x0000001A0047L);
			}

			if (available < length) {
				this.nextSplitFile();
			}
		}
	}

	/**
	 * <h3 class="en-US">Getter method for the total size</h3>
	 * <h3 class="zh-CN">总文件长度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Total size</span>
	 * <span class="zh-CN">总文件长度</span>
	 */
	public long getTotalSize() {
		return this.totalSize;
	}

	public long diskEntries() {
		return this.diskEntries;
	}

	public long diskCount() {
		return (this.zipOptions.isSplitArchive() && this.zipOptions.getSplitType() == ZipOptions.SplitType.NATIVE)
				? this.splitCount
				: 1L;
	}

	public long diskNumber() {
		return this.diskNumber;
	}

	public long offset() throws IOException {
		if (this.zipOptions.isSplitArchive() && this.zipOptions.getSplitType() == ZipOptions.SplitType.NUMBERED) {
			return this.totalSize;
		}
		return this.dataFile.getFilePointer();
	}

	/**
	 * <h3 class="en-US">Switch to the next split archive file</h3>
	 * <h3 class="zh-CN">切换到下一分卷文件</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when close current split archive file</span>
	 *                     <span class="zh-CN">关闭现有文件出错</span>
	 */
	private void nextSplitFile() throws IOException {
		this.ensureOpen();

		if (!this.zipOptions.isSplitArchive()) {
			return;
		}

		long nextCount = Math.addExact(this.splitCount, 1L);
		String nextPath = this.templatePath + this.numberSuffix(nextCount);

		DataFile splitFile = this.dataFile.newFile(nextPath);

		try {
			this.dataFile.close();
		} catch (IOException e) {
			try {
				splitFile.close();
			} catch (IOException ex) {
				e.addSuppressed(ex);
			}
			throw e;
		}

		this.dataFile = splitFile;
		this.splitCount = nextCount;

		if (this.zipOptions.getSplitType() == ZipOptions.SplitType.NATIVE) {
			this.diskNumber = nextCount - 1L;
			this.diskEntries = 0L;
		}
	}

	private void writeChunk(final byte[] buffer, final int offset, final int length) throws IOException {
		final long totalSize;
		try {
			totalSize = Math.addExact(this.totalSize, length);
		} catch (ArithmeticException e) {
			throw new ArchiveException(0x0000001A001BL);
		}

		long diskNumber = this.diskNumber();
		long diskOffset = this.offset();

		this.dataFile.write(buffer, offset, length);

		if (this.processingCentralDirectory && this.centralDirectoryStartDisk == Globals.DEFAULT_VALUE_LONG) {
			this.centralDirectoryStartDisk = diskNumber;
			this.centralDirectoryOffset = diskOffset;
		}

		this.totalSize = totalSize;
	}

	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new ArchiveException(0x0000001A0046L);
		}
	}
}
