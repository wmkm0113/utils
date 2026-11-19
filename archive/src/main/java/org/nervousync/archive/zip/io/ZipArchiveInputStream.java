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
import org.nervousync.archive.core.DataFile;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.FileUtils;
import org.nervousync.utils.commons.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h2 class="en-US">Zip archive file input stream</h2>
 * <h2 class="zh-CN">Zip 归档文件输入流</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ZipArchiveInputStream extends InputStream {

	/**
	 * <span class="en-US">.z0X match pattern</span>
	 * <span class="zh-CN">.z0X验证规则</span>
	 */
	private static final Pattern NATIVE_PART_PATTERN =
			Pattern.compile("^(.*)\\.z([0-9]+)$", Pattern.CASE_INSENSITIVE);

	/**
	 * <span class="en-US">.zip.00X match pattern</span>
	 * <span class="zh-CN">.zip.00X验证规则</span>
	 */
	private static final Pattern NUMBER_PART_PATTERN =
			Pattern.compile("^(.*\\.zip)\\.([0-9]+)$", Pattern.CASE_INSENSITIVE);

	/**
	 * <span class="en-US">Archive file path</span>
	 * <span class="zh-CN">归档文件地址</span>
	 */
	private final String archivePath;
	/**
	 * <span class="en-US">Split index to file path mapping table</span>
	 * <span class="zh-CN">分卷索引值和路径的映射表</span>
	 */
	private final Hashtable<Long, SplitFile> splitPathMapping = new Hashtable<>();
	/**
	 * <span class="en-US">Split count of archive</span>
	 * <span class="zh-CN">分卷计数</span>
	 */
	private final long splitCount;
	/**
	 * <span class="en-US">Current index value</span>
	 * <span class="zh-CN">当前索引值</span>
	 */
	private long diskNumber = Globals.DEFAULT_VALUE_LONG;
	/**
	 * <span class="en-US">Total size</span>
	 * <span class="zh-CN">总文件长度</span>
	 */
	private long totalSize = 0L;
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
	 * <h3 class="en-US">Constructor method for the zip archive file input stream</h3>
	 * <h3 class="zh-CN">Zip 归档文件输入流的构造方法</h3>
	 *
	 * @param dataFile   <span class="en-US">Data file</span>
	 *                   <span class="zh-CN">数据文件</span>
	 * @param zipOptions <span class="en-US">Zip options</span>
	 *                   <span class="zh-CN">Zip 选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when scan the split archive files</span>
	 *                     <span class="zh-CN">扫描分卷归档文件时出错</span>
	 */
	public ZipArchiveInputStream(final DataFile dataFile, final ZipOptions zipOptions) throws IOException {
		this.archivePath = dataFile.filePath();
		String filePath = dataFile.filePath();
		this.dataFile = dataFile;
		if (zipOptions.isSplitArchive()) {
			if (zipOptions.getSplitLength() <= 0L) {
				throw new ArchiveException(0x0000001A0008L);
			}
			String fileName = StringUtils.getFilename(filePath);
			String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
			ZipOptions.SplitType splitType = zipOptions.getSplitType();
			if (splitType == ZipOptions.SplitType.DETECT) {
				if (fileName.toLowerCase(Locale.ROOT).endsWith(".zip") || NATIVE_PART_PATTERN.matcher(fileName).matches()) {
					splitType = ZipOptions.SplitType.NATIVE;
				} else if (NUMBER_PART_PATTERN.matcher(fileName).matches()) {
					splitType = ZipOptions.SplitType.NUMBERED;
				}
			}
			Pattern pattern = switch (splitType) {
				case NATIVE -> NATIVE_PART_PATTERN;
				case NUMBERED -> NUMBER_PART_PATTERN;
				default -> throw new ArchiveException(0x0000001A0041L);
			};
			try {
				for (String splitPath : dataFile.splitFiles()) {
					String splitName = StringUtils.getFilename(splitPath);
					Matcher matcher = pattern.matcher(splitName);
					SplitFile splitFile;
					long diskNumber;
					if (matcher.matches() && matcher.group(1).equalsIgnoreCase(baseName)) {
						splitFile = new SplitFile(splitPath, splitType);
						diskNumber = Long.parseLong(matcher.group(2)) - 1L;
					} else if (splitName.equalsIgnoreCase(fileName)) {
						splitFile = new SplitFile(splitPath, ZipOptions.SplitType.FINAL);
						diskNumber = Globals.DEFAULT_VALUE_LONG;
					} else {
						continue;
					}
					if (this.splitPathMapping.containsKey(diskNumber)) {
						throw new ArchiveException(0x0000001A0006L, diskNumber);
					}
					this.splitPathMapping.put(diskNumber, splitFile);
				}

				if (this.splitPathMapping.containsKey(Globals.DEFAULT_VALUE_LONG)) {
					long count = this.splitPathMapping.size() - 1;
					SplitFile finalFile = this.splitPathMapping.remove(Globals.DEFAULT_VALUE_LONG);
					if (finalFile != null) {
						this.splitPathMapping.put(count, finalFile);
					}
				}
				long expectCount = 0L;
				while (expectCount < this.splitPathMapping.size()) {
					if (!this.splitPathMapping.containsKey(expectCount)) {
						throw new ArchiveException(0x0000001A0007L);
					}
					SplitFile splitFile = this.splitPathMapping.get(expectCount);
					if (zipOptions.getSplitLength() < splitFile.getFileSize()) {
						throw new ArchiveException(0x0000001A0008L, splitFile.getFileSize(), zipOptions.getSplitLength());
					}
					this.totalSize += splitFile.getFileSize();
					expectCount++;
				}
				this.splitCount = this.splitPathMapping.size();
			} catch (Exception e) {
				if (e instanceof ArchiveException) {
					throw (ArchiveException) e;
				}
				throw new ArchiveException(0x0000001A0007L, e);
			}
		} else {
			this.totalSize = FileUtils.fileSize(filePath);
			this.splitPathMapping.put(0L, new SplitFile(filePath, ZipOptions.SplitType.FINAL));
			this.splitCount = 1L;
		}
		this.openFile(0L);
	}

	/**
	 * <h3 class="en-US">Constructor method for the zip archive file input stream</h3>
	 * <h3 class="zh-CN">Zip 归档文件输入流的构造方法</h3>
	 *
	 * @param dataFile         <span class="en-US">Data file</span>
	 *                         <span class="zh-CN">数据文件</span>
	 * @param splitPathMapping <span class="en-US">Split index to file path mapping table</span>
	 *                         <span class="zh-CN">分卷索引值和路径的映射表</span>
	 * @param splitCount       <span class="en-US">Split count of archive</span>
	 *                         <span class="zh-CN">分卷计数</span>
	 * @param totalSize        <span class="en-US">Total size</span>
	 *                         <span class="zh-CN">总文件长度</span>
	 * @throws IOException <span class="en-US">If an error occurs when open archive files</span>
	 *                     <span class="zh-CN">打开归档文件时出错</span>
	 */
	private ZipArchiveInputStream(@Nonnull final String archivePath, @Nonnull final DataFile dataFile,
	                              @Nonnull final Hashtable<Long, SplitFile> splitPathMapping,
	                              final long splitCount, final long totalSize) throws IOException {
		this.archivePath = archivePath;
		this.dataFile = dataFile;
		this.splitPathMapping.putAll(splitPathMapping);
		this.splitCount = splitCount;
		this.totalSize = totalSize;
		this.openFile(0L);
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

	/**
	 * <h3 class="en-US">Get current archive split file path list</h3>
	 * <h3 class="zh-CN">获取当前归档分卷文件列表</h3>
	 *
	 * @return <span class="en-US">Archive split file path list</span>
	 * <span class="zh-CN">归档分卷文件列表</span>
	 */
	public List<Path> existVolumes() {
		List<Path> paths = new ArrayList<>();
		for (long disk = 0L; disk < this.splitCount; disk++) {
			paths.add(Path.of(this.splitPathMapping.get(disk).getFilePath()).toAbsolutePath().normalize());
		}
		return List.copyOf(paths);
	}

	/**
	 * <h3 class="en-US">Using current input stream information generate new input stream</h3>
	 * <h3 class="zh-CN">使用当前输入流信息生成新输入流</h3>
	 *
	 * @return <span class="en-US">Zip archive file input stream</span>
	 * <span class="zh-CN">Zip 归档文件输入流</span>
	 * @throws IOException <span class="en-US">If an error occurs when open archive files</span>
	 *                     <span class="zh-CN">打开归档文件时出错</span>
	 */
	public ZipArchiveInputStream newStream() throws IOException {
		return new ZipArchiveInputStream(this.archivePath, this.dataFile.newFile(this.dataFile.filePath()),
				this.splitPathMapping, this.splitCount, this.totalSize);
	}

	@Override
	public int read() throws IOException {
		this.ensureOpen();
		byte[] singleByte = new byte[1];
		int readLength = this.read(singleByte, 0, 1);
		return readLength < 0 ? -1 : singleByte[0] & 0xFF;
	}

	@Override
	public int read(@Nonnull final byte[] buffer, final int offset, final int length) throws IOException {
		this.ensureOpen();
		Objects.requireNonNull(buffer);
		if (offset < 0 || length < 0 || offset > (buffer.length - length)) {
			throw new ArchiveException(0x0000001A000FL);
		}
		if (length == 0) {
			return 0;
		}
		long fileSize = this.dataFile.length();
		byte[] readBuffer = new byte[length];
		if (this.dataFile.getFilePointer() >= (fileSize - length)) {
			int readLength = 0;
			do {
				int readLen = this.dataFile.read(readBuffer, readLength, length - readLength);
				if (readLen == Globals.DEFAULT_VALUE_INT) {
					if (this.hasNext()) {
						this.nextFile();
						continue;
					} else {
						break;
					}
				}
				readLength += readLen;
			} while (readLength < length);
			if (readLength == 0L) {
				return -1;
			}
			System.arraycopy(readBuffer, 0, buffer, offset, readLength);
			return readLength;
		} else {
			this.dataFile.readFully(readBuffer);
			System.arraycopy(readBuffer, 0, buffer, offset, readBuffer.length);
			return readBuffer.length;
		}
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		this.dataFile.close();
		this.splitPathMapping.clear();
		this.diskNumber = Globals.DEFAULT_VALUE_LONG;
		this.totalSize = 0L;
	}

	@Override
	public long skip(final long length) throws IOException {
		this.ensureOpen();
		if (length < 0L) {
			throw new ArchiveException(0x0000001A000FL);
		}
		if (this.splitCount == 1L) {
			if (length > this.dataFile.length() - this.dataFile.getFilePointer()) {
				throw new ArchiveException(0x0000001A000FL);
			}
			this.dataFile.seek(this.dataFile.getFilePointer() + length);
			return length;
		} else {
			if (length > this.dataFile.length() - this.dataFile.getFilePointer()) {
				long diskNumber = this.diskNumber + 1,
						skipLength = this.dataFile.length() - this.dataFile.getFilePointer(),
						remainingLength = length - skipLength;
				while (true) {
					SplitFile splitFile = this.splitPathMapping.get(diskNumber);
					if (splitFile == null) {
						this.openFile(diskNumber);
						this.dataFile.seek(this.dataFile.length());
						return skipLength;
					}
					if (remainingLength < splitFile.getFileSize()) {
						this.openFile(diskNumber);
						this.dataFile.seek(remainingLength);
						skipLength += remainingLength;
						return skipLength;
					}
					skipLength += splitFile.getFileSize();
					remainingLength -= splitFile.getFileSize();
					diskNumber++;
				}
			} else {
				this.dataFile.seek(this.dataFile.getFilePointer() + length);
				return length;
			}
		}
	}

	/**
	 * <h3 class="en-US">Get the total size of the zip archive file</h3>
	 * <h3 class="zh-CN">获取整体归档文件大小</h3>
	 *
	 * @return <span class="en-US">Total size of the zip archive file</span>
	 * <span class="zh-CN">归档文件大小</span>
	 */
	public long length() {
		return this.totalSize;
	}

	/**
	 * <h3 class="en-US">Get the last index number of the archive files</h3>
	 * <h3 class="zh-CN">获取最后一个分卷号</h3>
	 *
	 * @return <span class="en-US">Split index value</span>
	 * <span class="zh-CN">分卷索引号</span>
	 */
	public long lastDiskNumber() {
		return this.numberedArchive() ? 0L : this.splitCount - 1L;
	}

	/**
	 * <h3 class="en-US">Get the file size of the target archive file</h3>
	 * <h3 class="zh-CN">获取指定卷归档文件大小</h3>
	 *
	 * @param diskNumber <span class="en-US">Split index value</span>
	 *                   <span class="zh-CN">分卷索引号</span>
	 * @return <span class="en-US">Total size of the zip archive file</span>
	 * <span class="zh-CN">归档文件大小</span>
	 */
	public long length(final long diskNumber) throws IOException {
		this.ensureOpen();
		if (diskNumber < 0L) {
			throw new ArchiveException(0x0000001A0007L, diskNumber);
		}
		if (this.numberedArchive() && diskNumber == 0L) {
			return this.totalSize;
		}
		if (this.splitPathMapping.containsKey(diskNumber)) {
			return this.splitPathMapping.get(diskNumber).getFileSize();
		}
		throw new ArchiveException(0x0000001A0005L);
	}

	/**
	 * <h3 class="en-US">Setting the file pointer to the target position</h3>
	 * <h3 class="zh-CN">设置文件指针到指定位置</h3>
	 *
	 * @param diskNumber <span class="en-US">Split index value</span>
	 *                   <span class="zh-CN">分卷索引号</span>
	 * @param position   <span class="en-US">Data offset value</span>
	 *                   <span class="zh-CN">数据偏移值</span>
	 * @throws IOException <span class="en-US">If an error occurs when seek data bytes</span>
	 *                     <span class="zh-CN">执行操作时出错</span>
	 */
	public void seek(final long diskNumber, final long position) throws IOException {
		this.ensureOpen();
		if (diskNumber < 0L) {
			throw new ArchiveException(0x0000001A0007L, diskNumber);
		}
		if (position < 0L) {
			throw new ArchiveException(0x0000001A000FL);
		}
		long diskNum = diskNumber, pos = position;
		if (this.numberedArchive()) {
			//  7-zip split archive
			diskNum = 0L;
			while (this.splitPathMapping.containsKey(diskNum)) {
				SplitFile splitFile = this.splitPathMapping.get(diskNum);
				if (splitFile.getFileSize() < pos) {
					pos -= splitFile.getFileSize();
					diskNum++;
				} else {
					break;
				}
			}
		}
		SplitFile splitFile = this.splitPathMapping.get(diskNum);
		if (splitFile == null || splitFile.getFileSize() < pos) {
			throw new ArchiveException(0x0000001A0005L);
		}
		this.openFile(diskNum);
		this.dataFile.seek(pos);
	}

	/**
	 * <h3 class="en-US">Rollback the file pointer by the given offset</h3>
	 * <h3 class="zh-CN">文件指针回滚指定偏移量</h3>
	 *
	 * @param offset <span class="en-US">Data offset value</span>
	 *               <span class="zh-CN">数据偏移值</span>
	 * @throws IOException <span class="en-US">If an error occurs when rollback pointer</span>
	 *                     <span class="zh-CN">执行操作时出错</span>
	 */
	public void rollback(final long offset) throws IOException {
		this.ensureOpen();
		if (offset < 0L) {
			throw new ArchiveException(0x0000001A000FL);
		}
		if (this.dataFile.getFilePointer() < offset) {
			long countOffset = this.dataFile.getFilePointer();
			while (countOffset < offset) {
				this.previousFile();
				long pointer = (offset - countOffset);
				if (pointer < this.dataFile.length()) {
					countOffset += pointer;
					this.dataFile.seek(this.dataFile.length() - pointer);
				} else {
					countOffset += this.dataFile.length();
				}
			}
		} else {
			this.dataFile.seek(this.dataFile.getFilePointer() - offset);
		}

	}

	/**
	 * <h3 class="en-US">Read the last data bytes from the archive file</h3>
	 * <h3 class="zh-CN">读取归档文件的最后二进制数据</h3>
	 *
	 * @param length <span class="en-US">Read data length</span>
	 *               <span class="zh-CN">读取数据长度</span>
	 * @return <span class="en-US">Binary data bytes</span>
	 * <span class="zh-CN">二进制字节数组</span>
	 * @throws IOException <span class="en-US">If an error occurs when read data bytes</span>
	 *                     <span class="zh-CN">执行操作时出错</span>
	 */
	public byte[] readLastBytes(final int length) throws IOException {
		this.ensureOpen();
		if (length <= 0 || (long) length > this.totalSize) {
			throw new ArchiveException(0x0000001A000FL);
		}
		this.openFile(this.splitCount - 1);
		byte[] readBuffer = new byte[length];
		int bufferOffset = length;
		while (bufferOffset > 0) {
			long fileSize = this.dataFile.length();
			int readLength = (int) Math.min(bufferOffset, fileSize);
			bufferOffset -= readLength;
			this.dataFile.seek(fileSize - readLength);
			this.dataFile.readFully(readBuffer, bufferOffset, readLength);
			if (bufferOffset > 0) {
				this.previousFile();
			}
		}
		return readBuffer;
	}

	/**
	 * <h3 class="en-US">Determine whether the current input stream is a multipart (split) file.</h3>
	 * <h3 class="zh-CN">判断当前输入流是分卷文件</h3>
	 *
	 * @return <span class="en-US">Determine result</span>
	 * <span class="zh-CN">判断结果</span>
	 */
	public boolean splitArchive() {
		return this.splitCount > 1L;
	}

	private boolean numberedArchive() {
		return this.splitPathMapping.values().stream()
				.anyMatch(splitFile -> splitFile.getType() == ZipOptions.SplitType.NUMBERED);
	}

	/**
	 * <h3 class="en-US">Confirm that the current stream has not been closed.</h3>
	 * <h3 class="zh-CN">确认当前流未被关闭</h3>
	 *
	 * @throws IOException <span class="en-US">If current stream closed</span>
	 *                     <span class="zh-CN">当前流未被关闭</span>
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
	}

	/**
	 * <h3 class="en-US">Open the archive split file by the given index value</h3>
	 * <h3 class="zh-CN">打开指定分片的分卷文件</h3>
	 *
	 * @param diskNumber <span class="en-US">Split index value</span>
	 *                   <span class="zh-CN">分卷索引值</span>
	 * @throws IOException <span class="en-US">If an error occurs when close current file or open target file</span>
	 *                     <span class="zh-CN">执行操作时出错</span>
	 */
	private synchronized void openFile(final long diskNumber) throws IOException {
		if (!this.splitPathMapping.containsKey(diskNumber)) {
			throw new ArchiveException(0x0000001A0005L);
		}
		if (this.diskNumber == diskNumber) {
			return;
		}
		DataFile newFile = this.dataFile.newFile(this.splitPathMapping.get(diskNumber).getFilePath());
		this.dataFile.close();
		this.dataFile = newFile;
		this.diskNumber = diskNumber;
	}

	/**
	 * <h3 class="en-US">Check the next archive split file was existed</h3>
	 * <h3 class="zh-CN">检查是否还有下一个分卷文件</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	private boolean hasPrevious() {
		return this.splitPathMapping.containsKey(this.diskNumber - 1);
	}

	/**
	 * <h3 class="en-US">Open the next archive split file</h3>
	 * <h3 class="zh-CN">打开下一个分卷文件</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when close current file or open target file</span>
	 *                     <span class="zh-CN">执行操作时出错</span>
	 */
	private void previousFile() throws IOException {
		if (this.hasPrevious()) {
			this.openFile(this.diskNumber - 1);
		} else {
			throw new ArchiveException(0x0000001A000AL);
		}
	}

	/**
	 * <h3 class="en-US">Check the next archive split file was existed</h3>
	 * <h3 class="zh-CN">检查是否还有下一个分卷文件</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	private boolean hasNext() {
		return this.splitPathMapping.containsKey(this.diskNumber + 1);
	}

	/**
	 * <h3 class="en-US">Open the next archive split file</h3>
	 * <h3 class="zh-CN">打开下一个分卷文件</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when close current file or open target file</span>
	 *                     <span class="zh-CN">执行操作时出错</span>
	 */
	private void nextFile() throws IOException {
		if (this.hasNext()) {
			this.openFile(this.diskNumber + 1);
		} else {
			throw new ArchiveException(0x0000001A000AL);
		}
	}

	/**
	 * <h2 class="en-US">Zip split file information</h2>
	 * <h2 class="zh-CN">Zip 分卷文件信息</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	private static final class SplitFile {

		/**
		 * <span class="en-US">Split file path</span>
		 * <span class="zh-CN">分卷文件路径</span>
		 */
		private final String filePath;
		/**
		 * <span class="en-US">File size</span>
		 * <span class="zh-CN">文件长度</span>
		 */
		private final long fileSize;
		/**
		 * <span class="en-US">Part type</span>
		 * <span class="zh-CN">分卷类型</span>
		 */
		private final ZipOptions.SplitType type;

		/**
		 * <h3 class="en-US">Private constructor method for the Zip split file information</h3>
		 * <h3 class="zh-CN">Zip 分卷文件信息的私有构造方法</h3>
		 *
		 * @param filePath <span class="en-US">Split file path</span>
		 *                 <span class="zh-CN">分卷文件路径</span>
		 * @param type     <span class="en-US">Enumeration value of the archive split type</span>
		 *                 <span class="zh-CN">分卷类型枚举值</span>
		 */
		private SplitFile(final String filePath, final ZipOptions.SplitType type) {
			this.filePath = filePath;
			this.fileSize = FileUtils.fileSize(this.filePath);
			this.type = type;
		}

		/**
		 * <h3 class="en-US">Getter method for the split file path</h3>
		 * <h3 class="zh-CN">分卷文件路径的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Split file path</span>
		 * <span class="zh-CN">分卷文件路径</span>
		 */
		public String getFilePath() {
			return this.filePath;
		}

		/**
		 * <h3 class="en-US">Getter method for the file size</h3>
		 * <h3 class="zh-CN">文件长度的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">File size</span>
		 * <span class="zh-CN">文件长度</span>
		 */
		public long getFileSize() {
			return this.fileSize;
		}

		/**
		 * <h3 class="en-US">Getter method for the enumeration value of the archive part type</h3>
		 * <h3 class="zh-CN">分卷类型枚举值的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Enumeration value of the archive split type</span>
		 * <span class="zh-CN">分卷类型枚举值</span>
		 */
		public ZipOptions.SplitType getType() {
			return this.type;
		}
	}
}
