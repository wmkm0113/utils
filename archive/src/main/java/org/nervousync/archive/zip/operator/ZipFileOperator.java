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

package org.nervousync.archive.zip.operator;

import jakarta.annotation.Nonnull;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.DataFile;
import org.nervousync.archive.enumerations.zip.AesVendor;
import org.nervousync.archive.enumerations.zip.StrongAccessMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.io.bzip2.BZip2InputStream;
import org.nervousync.archive.io.deflate.DeflateInputStream;
import org.nervousync.archive.io.lzma.LZMAInputStream;
import org.nervousync.archive.io.xz.XZInputStream;
import org.nervousync.archive.zip.io.BoundedInputStream;
import org.nervousync.archive.zip.io.ZipArchiveInputStream;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.crypto.strong.StrongInputStream;
import org.nervousync.archive.zip.models.ExtraDataRecord;
import org.nervousync.archive.zip.models.central.CentralDirectory;
import org.nervousync.archive.zip.models.central.EndCentralDirectoryRecord;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.archive.zip.models.crypto.StrongRecipientTable;
import org.nervousync.archive.zip.models.header.FileHeader;
import org.nervousync.archive.zip.models.header.GeneralFileHeader;
import org.nervousync.archive.zip.models.header.LocalFileHeader;
import org.nervousync.archive.zip.models.header.StrongDecryptionHeader;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.IOUtils;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * <h2 class="en-US">Zip file operator</h2>
 * <h2 class="zh-CN">Zip 文件操作器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ZipFileOperator implements AutoCloseable {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(ZipFileOperator.class);

	private static final int KNOWN_FLAGS = 0x0003 | 0x0100 | 0x4000;

	public static final int FIXED_SIZE = 22;
	public static final int MAX_COMMENT_LENGTH = 0xFFFF;
	public static final int MAX_SEARCH_LENGTH = MAX_COMMENT_LENGTH + FIXED_SIZE;
	private static final int LOCAL_FILE_HEADER_FIXED_SIZE = 30;
	private static final int CENTRAL_FILE_HEADER_FIXED_SIZE = 46;

	private static final Provider BC_PROVIDER = new BouncyCastleProvider();

	/**
	 * <span class="en-US">Current archive file input stream</span>
	 * <span class="zh-CN">当前归档文件输入流</span>
	 */
	private final ZipArchiveInputStream inputStream;
	/**
	 * <span class="en-US">Archive options</span>
	 * <span class="zh-CN">归档选项</span>
	 */
	private final ArchiveOptions options;
	/**
	 * <span class="en-US">ZIP options</span>
	 * <span class="zh-CN">ZIP 选项</span>
	 */
	private final ZipOptions zipOptions;
	/**
	 * <span class="en-US">Strong decryption header</span>
	 * <span class="zh-CN">解密头信息</span>
	 */
	private StrongDecryptionHeader centralDecryptionHeader = null;

	/**
	 * <h3 class="en-US">Private constructor method for the file reader</h3>
	 * <h3 class="zh-CN">文件读取器的私有构造方法</h3>
	 *
	 * @param dataFile   <span class="en-US">Data file</span>
	 *                   <span class="zh-CN">数据文件</span>
	 * @param options    <span class="en-US">Archive options</span>
	 *                   <span class="zh-CN">归档选项</span>
	 * @param zipOptions <span class="en-US">ZIP options</span>
	 *                   <span class="zh-CN">ZIP 选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when scan the split archive files</span>
	 *                     <span class="zh-CN">扫描分卷归档文件时出错</span>
	 */
	public ZipFileOperator(final DataFile dataFile, @Nonnull final ArchiveOptions options,
	                       @Nonnull final ZipOptions zipOptions) throws IOException {
		this.inputStream = new ZipArchiveInputStream(dataFile, zipOptions);
		this.options = options;
		this.zipOptions = zipOptions;
	}

	/**
	 * <h3 class="en-US">Determine whether the current input stream is a multipart (split) file.</h3>
	 * <h3 class="zh-CN">判断当前输入流是分卷文件</h3>
	 *
	 * @return <span class="en-US">Determine result</span>
	 * <span class="zh-CN">判断结果</span>
	 */
	public boolean splitArchive() {
		return this.inputStream.splitArchive();
	}

	/**
	 * <h3 class="en-US">Getter method for the strong decryption header</h3>
	 * <h3 class="zh-CN">解密头信息的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Strong decryption header</span>
	 * <span class="zh-CN">解密头信息</span>
	 */
	public StrongDecryptionHeader getCentralDecryptionHeader() {
		return this.centralDecryptionHeader;
	}

	/**
	 * <span class="en-US">Get the current archive file input stream</span>
	 * <span class="zh-CN">获取当前归档文件输入流</span>
	 *
	 * @return <span class="en-US">Zip archive file input stream</span>
	 * <span class="zh-CN">Zip 归档文件输入流</span>
	 * @throws IOException <span class="en-US">If an error occurs when open archive files</span>
	 *                     <span class="zh-CN">打开归档文件时出错</span>
	 */
	public synchronized ZipArchiveInputStream inputStream() throws IOException {
		return this.inputStream.newStream();
	}

	/**
	 * <h3 class="en-US">Get current archive split file path list</h3>
	 * <h3 class="zh-CN">获取当前归档分卷文件列表</h3>
	 *
	 * @return <span class="en-US">Archive split file path list</span>
	 * <span class="zh-CN">归档分卷文件列表</span>
	 */
	public List<Path> existVolumes() {
		return this.inputStream.existVolumes();
	}

	/**
	 * <h3 class="en-US">Read the current archive file size</h3>
	 * <h3 class="zh-CN">读取归档文件大小</h3>
	 *
	 * @return <span class="en-US">Archive file size</span>
	 * <span class="zh-CN">归档文件大小</span>
	 */
	public long size() {
		return this.inputStream.length();
	}

	/**
	 * <h3 class="en-US">Read end of central directory record</h3>
	 * <h3 class="zh-CN">读取中央目录终止记录</h3>
	 *
	 * @return <span class="en-US">End of central directory record</span>
	 * <span class="zh-CN">中央目录终止记录</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when read end of central directory record</span>
	 *                          <span class="zh-CN">读取中央目录终止记录时出错</span>
	 */
	public EndCentralDirectoryRecord readEndCentralDirectoryRecord() throws ArchiveException {
		try {
			long currentLength = this.inputStream.length();
			if (currentLength < FIXED_SIZE) {
				throw new ArchiveException(0x000001A000BL);
			}

			int searchLength = (int) Math.min(currentLength, MAX_SEARCH_LENGTH);

			byte[] searchBuffer = this.inputStream.readLastBytes(searchLength);

			for (int offset = searchLength - FIXED_SIZE; offset >= 0; offset--) {
				if (RawUtils.readUnsignedInt(searchBuffer, offset, ByteOrder.LITTLE_ENDIAN)
						!= ZipGlobals.END_OF_CENTRAL_DIRECTORY_SIGNATURE) {
					continue;
				}

				int commentLength = RawUtils.readUnsignedShort(searchBuffer, offset + 20, ByteOrder.LITTLE_ENDIAN);

				if (offset + FIXED_SIZE + commentLength != searchLength) {
					continue;
				}

				EndCentralDirectoryRecord endCentralDirectoryRecord = new EndCentralDirectoryRecord();
				endCentralDirectoryRecord.setDiskNumber(
						RawUtils.readUnsignedShort(searchBuffer, offset + 4, ByteOrder.LITTLE_ENDIAN));
				endCentralDirectoryRecord.setCentralDirectoryStartDisk(
						RawUtils.readUnsignedShort(searchBuffer, offset + 6, ByteOrder.LITTLE_ENDIAN));
				endCentralDirectoryRecord.setEntriesOnThisDisk(
						RawUtils.readUnsignedShort(searchBuffer, offset + 8, ByteOrder.LITTLE_ENDIAN));
				endCentralDirectoryRecord.setTotalEntries(
						RawUtils.readUnsignedShort(searchBuffer, offset + 10, ByteOrder.LITTLE_ENDIAN));
				endCentralDirectoryRecord.setCentralDirectorySize(
						RawUtils.readUnsignedInt(searchBuffer, offset + 12, ByteOrder.LITTLE_ENDIAN));
				endCentralDirectoryRecord.setCentralDirectoryOffset(
						RawUtils.readUnsignedInt(searchBuffer, offset + 16, ByteOrder.LITTLE_ENDIAN));
				endCentralDirectoryRecord.setCommentBytes(
						Arrays.copyOfRange(searchBuffer, offset + FIXED_SIZE,
								offset + FIXED_SIZE + commentLength));
				endCentralDirectoryRecord.setDataLength(searchLength - offset);
				return endCentralDirectoryRecord;
			}
			throw new ArchiveException(0x000001A000BL);
		} catch (IOException e) {
			throw new ArchiveException(0x0000001A000AL, e);
		}
	}

	/**
	 * <h3 class="en-US">Read Zip64 end of central directory information</h3>
	 * <h3 class="zh-CN">读取 Zip64 中央目录结束信息</h3>
	 *
	 * @param endCentralDirectoryRecord <span class="en-US">The end of central directory record</span>
	 *                                  <span class="zh-CN">中央目录终止记录</span>
	 * @return <span class="en-US">Zip64 end of central directory information</span>
	 * <span class="zh-CN">Zip64 中央目录结束信息</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when read Zip64 end of central directory locator</span>
	 *                          <span class="zh-CN">读取 Zip64 中央目录定位器时出错</span>
	 */
	public Zip64EndCentralDirectory readZip64EndCentralDirectory(
			final EndCentralDirectoryRecord endCentralDirectoryRecord) throws ArchiveException {
		try {
			long diskNumber = this.inputStream.lastDiskNumber();
			long position = this.inputStream.length(diskNumber) - endCentralDirectoryRecord.getDataLength() - 20L;
			if (position < 0L) {
				if (endCentralDirectoryRecord.getDiskNumber() == 0xFFFF
						|| endCentralDirectoryRecord.getCentralDirectoryStartDisk() == 0xFFFF
						|| endCentralDirectoryRecord.getEntriesOnThisDisk() == 0xFFFF
						|| endCentralDirectoryRecord.getTotalEntries() == 0xFFFF
						|| endCentralDirectoryRecord.getCentralDirectorySize() == ZipGlobals.ZIP_64_LIMIT
						|| endCentralDirectoryRecord.getCentralDirectoryOffset() == ZipGlobals.ZIP_64_LIMIT) {
					throw new ArchiveException(0x0000001A001FL);
				}
				//  Empty zip archive
				return null;
			}
			this.inputStream.seek(diskNumber, position);
			byte[] locatorBuffer = new byte[20];
			int readLength = this.inputStream.read(locatorBuffer);
			if (readLength != 20) {
				throw new ArchiveException(0x0000001A0011L);
			}

			if (RawUtils.readInt(locatorBuffer, 0, ByteOrder.LITTLE_ENDIAN) != ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE) {
				return null;
			}

			Zip64EndCentralDirectory.Locator locator = new Zip64EndCentralDirectory.Locator();
			locator.setZip64EndRecordDiskNumber(RawUtils.readUnsignedInt(locatorBuffer, 4, ByteOrder.LITTLE_ENDIAN));
			locator.setZip64EndRecordOffset(RawUtils.readLong(locatorBuffer, 8, ByteOrder.LITTLE_ENDIAN));
			locator.setTotalNumberOfDisks(RawUtils.readUnsignedInt(locatorBuffer, 16, ByteOrder.LITTLE_ENDIAN));

			this.inputStream.seek(locator.getZip64EndRecordDiskNumber(), locator.getZip64EndRecordOffset());
			byte[] recordBuffer = new byte[56];
			readLength = this.inputStream.read(recordBuffer);
			if (readLength != 56) {
				throw new ArchiveException(0x0000001A0011L);
			}

			if (RawUtils.readInt(recordBuffer, 0, ByteOrder.LITTLE_ENDIAN) != ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE) {
				throw new ArchiveException(0x0000001A000CL);
			}

			Zip64EndCentralDirectory.Record record = new Zip64EndCentralDirectory.Record();
			long recordSize = RawUtils.readLong(recordBuffer, 4, ByteOrder.LITTLE_ENDIAN);
			if (recordSize < 44L) {
				throw new ArchiveException(0x0000001A000EL, "Record size");
			}

			long totalRecordSize;
			try {
				totalRecordSize = Math.addExact(12L, recordSize);
			} catch (ArithmeticException e) {
				throw new ArchiveException(0x0000001A001FL, "Record size");
			}
			validateHeaderSize(totalRecordSize, this.options.getArchiveLimit().getMaxHeaderBytes());

			long availableRecordBytes =
					this.physicalReadLimit(locator.getZip64EndRecordDiskNumber(), locator.getZip64EndRecordOffset(),
							diskNumber, position);

			if (totalRecordSize > availableRecordBytes) {
				throw new ArchiveException(0x0000001A001FL);
			}

			record.setRecordSize(recordSize);
			record.setMadeVersion(RawUtils.readUnsignedShort(recordBuffer, 12, ByteOrder.LITTLE_ENDIAN));
			record.setExtractNeeded(RawUtils.readUnsignedShort(recordBuffer, 14, ByteOrder.LITTLE_ENDIAN));
			record.setDiskNumber(RawUtils.readUnsignedInt(recordBuffer, 16, ByteOrder.LITTLE_ENDIAN));
			record.setCentralDirectoryStartDisk(RawUtils.readUnsignedInt(recordBuffer, 20, ByteOrder.LITTLE_ENDIAN));
			record.setEntriesOnThisDisk(RawUtils.readLong(recordBuffer, 24, ByteOrder.LITTLE_ENDIAN));
			record.setTotalEntries(RawUtils.readLong(recordBuffer, 32, ByteOrder.LITTLE_ENDIAN));
			record.setCentralDirectorySize(RawUtils.readLong(recordBuffer, 40, ByteOrder.LITTLE_ENDIAN));
			record.setCentralDirectoryOffset(RawUtils.readLong(recordBuffer, 48, ByteOrder.LITTLE_ENDIAN));

			if (record.getDiskNumber() < 0L
					|| record.getDiskNumber() >= locator.getTotalNumberOfDisks()
					|| record.getDiskNumber() != locator.getZip64EndRecordDiskNumber()
					|| record.getCentralDirectoryStartDisk() < 0L
					|| record.getCentralDirectoryStartDisk() >= locator.getTotalNumberOfDisks()
					|| record.getEntriesOnThisDisk() < 0L
					|| record.getTotalEntries() < record.getEntriesOnThisDisk()
					|| record.getCentralDirectorySize() < 0L
					|| record.getCentralDirectoryOffset() < 0L) {
				throw new ArchiveException(0x0000001A000EL);
			}

			long remainLength = record.getRecordSize() - 44L;
			long extensibleDataLength = remainLength;
			Zip64EndCentralDirectory.Protection protection = null;
			if (remainLength >= 28L) {
				byte[] v2Buffer = new byte[28];
				readLength = this.inputStream.read(v2Buffer);
				if (readLength != 28) {
					throw new ArchiveException(0x0000001A0011L);
				}

				int compressionMethod = RawUtils.readUnsignedShort(v2Buffer, 0, ByteOrder.LITTLE_ENDIAN);
				long compressedSize = RawUtils.readLong(v2Buffer, 2, ByteOrder.LITTLE_ENDIAN);
				long uncompressedSize = RawUtils.readLong(v2Buffer, 10, ByteOrder.LITTLE_ENDIAN);
				int algorithm = RawUtils.readUnsignedShort(v2Buffer, 18, ByteOrder.LITTLE_ENDIAN);
				int bitLength = RawUtils.readUnsignedShort(v2Buffer, 20, ByteOrder.LITTLE_ENDIAN);
				int flags = RawUtils.readUnsignedShort(v2Buffer, 22, ByteOrder.LITTLE_ENDIAN);
				int hashAlgorithm = RawUtils.readUnsignedShort(v2Buffer, 24, ByteOrder.LITTLE_ENDIAN);
				int hashLength = RawUtils.readUnsignedShort(v2Buffer, 26, ByteOrder.LITTLE_ENDIAN);

				boolean transformedCentralDirectory =
						compressionMethod != ZipGlobals.COMP_STORE
								|| algorithm != ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE;
				boolean hashedCentralDirectory =
						hashAlgorithm != ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE || hashLength > 0;

				boolean hashLengthWithinRecord = hashLength <= (remainLength - 28L);
				boolean validateStructure =
						hashLengthWithinRecord
								&& compressedSize >= 0L
								&& uncompressedSize >= 0L
								&& recognizedCompressionMethod(compressionMethod)
								&& validateEncryptionParameters(algorithm, bitLength, flags)
								&& hashLength == expectedHashLength(hashAlgorithm);

				if (((record.getExtractNeeded() & 0xFF) >= 62)
						&& validateStructure && (transformedCentralDirectory || hashedCentralDirectory)) {
					boolean directoryMarkers =
							endCentralDirectoryRecord.getTotalEntries() == 0xFFFF
									&& endCentralDirectoryRecord.getEntriesOnThisDisk() == 0
									&& record.getEntriesOnThisDisk() == 0;
					if (transformedCentralDirectory && !directoryMarkers) {
						if (this.options.isStrict()) {
							throw new ArchiveException(0x0000001A001EL);
						} else {
							LOGGER.warn("Zip64_End_Central_Directory_Protection_Structure_Invalid");
						}
					}
					byte[] hashBuffer = new byte[hashLength];
					readLength = this.inputStream.read(hashBuffer);
					if (readLength != hashLength) {
						throw new ArchiveException(0x0000001A0011L);
					}

					//  V2 Protection
					protection = new Zip64EndCentralDirectory.Protection();

					protection.setCompressionMethod(compressionMethod);
					protection.setCompressedSize(compressedSize);
					protection.setUncompressedSize(uncompressedSize);
					protection.setAlgorithm(algorithm);
					protection.setKeyBitLength(bitLength);
					protection.setFlags(flags);
					protection.setHashAlgorithm(hashAlgorithm);
					protection.setHashData(hashBuffer);

					extensibleDataLength = remainLength - 28L - hashLength;
				} else {
					//  Rollback pointer
					this.inputStream.rollback(28L);
				}
			}
			if (extensibleDataLength > Integer.MAX_VALUE) {
				throw new ArchiveException(0x0000001A000DL);
			}
			if (extensibleDataLength > 0L) {
				byte[] extensibleData = new byte[(int) extensibleDataLength];
				readLength = this.inputStream.read(extensibleData);
				if (readLength != (int) extensibleDataLength) {
					throw new ArchiveException(0x0000001A0011L);
				}

				record.setExtensibleDataSector(extensibleData);
			} else {
				record.setExtensibleDataSector(new byte[0]);
			}

			Zip64EndCentralDirectory zip64EndCentralDirectory = new Zip64EndCentralDirectory(record, locator);
			zip64EndCentralDirectory.setProtection(protection);
			return zip64EndCentralDirectory;
		} catch (IOException e) {
			throw (e instanceof ArchiveException) ? (ArchiveException) e : new ArchiveException(0x0000001A000AL, e);
		}
	}

	/**
	 * <h3 class="en-US">Read central directory</h3>
	 * <h3 class="zh-CN">读取中央目录</h3>
	 *
	 * @param endCentralDirectoryRecord <span class="en-US">End of central directory record</span>
	 *                                  <span class="zh-CN">中央目录终止记录</span>
	 * @param zip64EndCentralDirectory  <span class="en-US">Zip64 end of central directory</span>
	 *                                  <span class="zh-CN">Zip64 中央目录</span>
	 * @return <span class="en-US">Central directory</span>
	 * <span class="zh-CN">中央目录</span>
	 * @throws IOException <span class="en-US">If an error occurs when read central directory</span>
	 *                     <span class="zh-CN">读取中央目录时出错</span>
	 */
	public CentralDirectory readCentralDirectory(final EndCentralDirectoryRecord endCentralDirectoryRecord,
	                                             final Zip64EndCentralDirectory zip64EndCentralDirectory)
			throws IOException {
		long diskNumber = endCentralDirectoryRecord.getCentralDirectoryStartDisk();
		long offset = endCentralDirectoryRecord.getCentralDirectoryOffset();
		long totalEntries = endCentralDirectoryRecord.getTotalEntries();
		long centralDirectorySize = endCentralDirectoryRecord.getCentralDirectorySize();
		Zip64EndCentralDirectory.Protection protection = null;
		if (zip64EndCentralDirectory != null) {
			Zip64EndCentralDirectory.Record zip64Record = zip64EndCentralDirectory.getRecord();
			diskNumber = zip64Record.getCentralDirectoryStartDisk();
			offset = zip64Record.getCentralDirectoryOffset();
			totalEntries = zip64Record.getTotalEntries();
			centralDirectorySize = zip64Record.getCentralDirectorySize();
			protection = zip64EndCentralDirectory.getProtection();
		}

		long maxEntries = this.options.getArchiveLimit().getMaxEntries();
		if (totalEntries < 0L) {
			throw new ArchiveException(0x0000001A001FL);
		}
		if (totalEntries > maxEntries) {
			throw new ArchiveException(0x0000001A003EL, maxEntries, totalEntries);
		}

		long headerSize = (protection == null) ? centralDirectorySize : protection.getUncompressedSize();
		validateHeaderSize(headerSize, this.options.getArchiveLimit().getMaxHeaderBytes());

		if (totalEntries > headerSize / CENTRAL_FILE_HEADER_FIXED_SIZE) {
			throw new ArchiveException(0x0000001A001CL);
		}

		if (protection == null) {
			return this.readPlainCentralDirectory(diskNumber, offset, totalEntries, centralDirectorySize);
		}
		this.inputStream.seek(diskNumber, offset);
		Zip64EndCentralDirectory.Locator locator = zip64EndCentralDirectory.getLocator();
		long physicalReadLimit = this.physicalReadLimit(diskNumber, offset,
				locator.getZip64EndRecordDiskNumber(), locator.getZip64EndRecordOffset());
		BoundedInputStream physicalInputStream = new BoundedInputStream(this.inputStream, physicalReadLimit);
		InputStream inputStream;

		if (!supportedCompressionMethod(protection.getCompressionMethod())) {
			throw new ArchiveException(0x0000001B004AL);
		}
		if (expectedHashLength(protection.getHashAlgorithm()) == Globals.DEFAULT_VALUE_INT) {
			throw new ArchiveException(0x0000001A001FL);
		}
		if (protection.encrypted()) {
			if (physicalReadLimit < 0L) {
				throw new IllegalArgumentException("physicalReadLimit must be non-negative");
			}

			InputStream boundedSource = new BoundedInputStream(physicalInputStream, physicalReadLimit);
			DataInputStream dataInputStream = new DataInputStream(boundedSource);

			StrongDecryptionHeader decryptionHeader =
					ZipCrypto.readStrongDecryptionHeader(dataInputStream,
							this.options.getArchiveLimit().getMaxHeaderBytes());
			this.centralDecryptionHeader = decryptionHeader;
			inputStream = StrongInputStream.centralDirectory(this.inputStream.archivePath(), physicalInputStream,
					protection, this.options.getPasswordProvider(), this.options.getCertificateProvider(),
					decryptionHeader);
		} else {
			inputStream = new BoundedInputStream(physicalInputStream, protection.getCompressedSize());
		}
		CentralDirectory centralDirectory = this.readProtectionCentralDirectory(inputStream, totalEntries, protection);

		long remaining = physicalInputStream.remaining();
		if (remaining > 0L) {
			if (remaining < Integer.BYTES + Short.BYTES) {
				throw new ArchiveException(0x0000001A001CL);
			}
			DataInputStream dataInputStream = new DataInputStream(physicalInputStream);
			byte[] signatureBuffer = new byte[Integer.BYTES + Short.BYTES];
			dataInputStream.readFully(signatureBuffer);
			if (RawUtils.readInt(signatureBuffer, 0, ByteOrder.LITTLE_ENDIAN) != ZipGlobals.DIGITAL_SIGNATURE) {
				throw new ArchiveException(0x0000001A000CL);
			}

			int signatureLength = RawUtils.readUnsignedShort(signatureBuffer, Integer.BYTES, ByteOrder.LITTLE_ENDIAN);
			if (signatureLength != remaining - signatureBuffer.length) {
				throw new ArchiveException(0x0000001A001CL);
			}
			byte[] signatureData = new byte[signatureLength];
			dataInputStream.readFully(signatureData);
			centralDirectory.setSignatureData(signatureData);
		}

		return centralDirectory;
	}

	/**
	 * <h3 class="en-US">Read local file header information</h3>
	 * <h3 class="zh-CN">读取本地文件头信息</h3>
	 *
	 * @param generalFileHeader <span class="en-US">General file header</span>
	 *                          <span class="zh-CN">中央目录头</span>
	 * @param charset           <span class="en-US">Charset</span>
	 *                          <span class="zh-CN">字符集</span>
	 * @return <span class="en-US">Local file header information</span>
	 * <span class="zh-CN">本地文件头信息</span>
	 * @throws IOException <span class="en-US">If an error occurs when read data or data invalid</span>
	 *                     <span class="zh-CN">读取错误或数据异常</span>
	 */
	public synchronized LocalFileHeader readLocalFileHeader(@Nonnull final GeneralFileHeader generalFileHeader,
	                                                        final Charset charset, final long maxHeaderBytes) throws IOException {
		Objects.requireNonNull(generalFileHeader, "generalFileHeader");
		Objects.requireNonNull(charset, "charset");

		long diskNumber = generalFileHeader.getDiskNumberStart();
		long localHeaderOffset = generalFileHeader.getOffsetLocalHeader();

		if (diskNumber < 0L || localHeaderOffset < 0L) {
			throw new ArchiveException(0x0000001A001FL, generalFileHeader.getEntryPath());
		}

		long diskLength = this.inputStream.length(diskNumber);
		if (localHeaderOffset > diskLength || (diskLength - localHeaderOffset) < LOCAL_FILE_HEADER_FIXED_SIZE) {
			throw new ArchiveException(0x0000001A001FL, generalFileHeader.getEntryPath());
		}

		this.inputStream.seek(diskNumber, localHeaderOffset);
		DataInputStream dataInputStream = new DataInputStream(this.inputStream);

		byte[] fixedHeader = new byte[LOCAL_FILE_HEADER_FIXED_SIZE];
		dataInputStream.readFully(fixedHeader);

		int signature = RawUtils.readInt(fixedHeader, 0, ByteOrder.LITTLE_ENDIAN);

		if (Integer.toUnsignedLong(signature) != ZipGlobals.LOCAL_FILE_HEADER_SIGNATURE) {
			throw new ArchiveException(0x0000001A000CL, generalFileHeader.getEntryPath());
		}

		LocalFileHeader fileHeader = new LocalFileHeader();

		fileHeader.setSignature(signature);
		fileHeader.setDiskNumberStart(diskNumber);
		fileHeader.setOffsetLocalHeader(localHeaderOffset);

		fileHeader.setExtractNeeded(RawUtils.readUnsignedShort(fixedHeader, 4, ByteOrder.LITTLE_ENDIAN));
		fileHeader.setGeneralPurpose(RawUtils.readUnsignedShort(fixedHeader, 6, ByteOrder.LITTLE_ENDIAN));
		fileHeader.setCompressionMethod(RawUtils.readUnsignedShort(fixedHeader, 8, ByteOrder.LITTLE_ENDIAN));
		fileHeader.setLastModifiedTime(RawUtils.readUnsignedShort(fixedHeader, 10, ByteOrder.LITTLE_ENDIAN));
		fileHeader.setLastModifiedDate(RawUtils.readUnsignedShort(fixedHeader, 12, ByteOrder.LITTLE_ENDIAN));
		fileHeader.setCrc32(RawUtils.readUnsignedInt(fixedHeader, 14, ByteOrder.LITTLE_ENDIAN));
		fileHeader.setCompressedSize(RawUtils.readUnsignedInt(fixedHeader, 18, ByteOrder.LITTLE_ENDIAN));
		fileHeader.setUncompressedSize(RawUtils.readUnsignedInt(fixedHeader, 22, ByteOrder.LITTLE_ENDIAN));

		fileHeader.setExternalFileAttr(generalFileHeader.getExternalFileAttr());
		fileHeader.setMadeVersion(generalFileHeader.getMadeVersion());

		int nameLength = RawUtils.readUnsignedShort(fixedHeader, 26, ByteOrder.LITTLE_ENDIAN);
		int extraFieldLength = RawUtils.readUnsignedShort(fixedHeader, 28, ByteOrder.LITTLE_ENDIAN);

		long localHeaderSize = Math.addExact(LOCAL_FILE_HEADER_FIXED_SIZE, Math.addExact(nameLength, (long) extraFieldLength));
		validateHeaderSize(localHeaderSize, maxHeaderBytes);
		if (localHeaderSize > (diskLength - localHeaderOffset)) {
			throw new ArchiveException(0x0000001A001FL, generalFileHeader.getEntryPath());
		}

		fileHeader.setNameLength(nameLength);
		fileHeader.setExtraFieldLength(extraFieldLength);

		if (nameLength > 0) {
			byte[] nameBytes = new byte[nameLength];
			dataInputStream.readFully(nameBytes);
			String entryPath = new String(nameBytes, fileHeader.utf8Encoded() ? StandardCharsets.UTF_8 : charset);
			if (this.options.isStrict()) {
				validateEntryPath(entryPath);
			}
			fileHeader.setEntryPath(entryPath);
		} else {
			fileHeader.setEntryPath(null);
		}

		byte[] extraFieldBytes = new byte[extraFieldLength];
		if (extraFieldLength > 0) {
			dataInputStream.readFully(extraFieldBytes);
			fileHeader.setExtraDataRecords(readExtraDataRecords(extraFieldBytes));
		} else {
			fileHeader.setExtraDataRecords(Collections.emptyList());
		}

		readLocalZip64ExtendedInformation(fileHeader, generalFileHeader.getCompressedSize(),
				generalFileHeader.getUncompressedSize(), this.options.isStrict());
		readAesEncryptionInformation(fileHeader);
		if (fileHeader.encryptionMethod() == ZipGlobals.ENC_METHOD_STRONG) {
			if (generalFileHeader.getEncryptionInfo() == null) {
				throw new ArchiveException(0x0000001A0027L, generalFileHeader.getEntryPath());
			}
			fileHeader.setEncryptionInfo(generalFileHeader.getEncryptionInfo());
		}

		boolean masked = (fileHeader.getGeneralPurpose() & 0x2000) != 0;

		boolean deferredValues = fileHeader.hasDataDescriptor() || masked;

		if (this.options.isStrict()) {
			boolean zip64VersionUpgrade =
					generalFileHeader.getExtractNeeded() == ZipGlobals.ZIP_64_VERSION
							&& fileHeader.getExtractNeeded() < ZipGlobals.ZIP_64_VERSION
							&& hasZip64ExtraData(generalFileHeader);

			if ((!zip64VersionUpgrade
					&& fileHeader.getExtractNeeded() != generalFileHeader.getExtractNeeded())
					|| fileHeader.getGeneralPurpose() != generalFileHeader.getGeneralPurpose()
					|| fileHeader.getCompressionMethod() != generalFileHeader.getCompressionMethod()) {
				throw new ArchiveException(0x0000001A001FL, generalFileHeader.getEntryPath());
			}

			if (!masked) {
				if (!Objects.equals(fileHeader.getEntryPath(), generalFileHeader.getEntryPath())
						|| fileHeader.getLastModifiedTime() != generalFileHeader.getLastModifiedTime()
						|| fileHeader.getLastModifiedDate() != generalFileHeader.getLastModifiedDate()) {
					throw new ArchiveException(0x0000001A001FL, generalFileHeader.getEntryPath());
				}
			}

			if (!deferredValues) {
				if (fileHeader.getCrc32() != generalFileHeader.getCrc32()
						|| fileHeader.getCompressedSize() != generalFileHeader.getCompressedSize()
						|| fileHeader.getUncompressedSize() != generalFileHeader.getUncompressedSize()) {
					throw new ArchiveException(0x0000001A001FL, generalFileHeader.getEntryPath());
				}
			}

			if (fileHeader.getEncryptionInfo() == null) {
				if (generalFileHeader.getEncryptionInfo() != null) {
					throw new ArchiveException(0x0000001A001FL, generalFileHeader.getEntryPath());
				}
			} else {
				if (!fileHeader.getEncryptionInfo().equals(generalFileHeader.getEncryptionInfo())) {
					throw new ArchiveException(0x0000001A001FL, generalFileHeader.getEntryPath());
				}
			}
		}

		fileHeader.setCrc32(generalFileHeader.getCrc32());
		fileHeader.setCompressedSize(generalFileHeader.getCompressedSize());
		fileHeader.setUncompressedSize(generalFileHeader.getUncompressedSize());

		try {
			fileHeader.setOffsetStartOfData(Math.addExact(localHeaderOffset, localHeaderSize));
		} catch (ArithmeticException e) {
			throw new ArchiveException(0x0000001A001FL, e, generalFileHeader.getEntryPath());
		}

		return fileHeader;
	}

	@Override
	public void close() throws IOException {
		this.inputStream.close();
	}

	/**
	 * <h3 class="en-US">Validate header size</h3>
	 * <h3 class="zh-CN">验证头大小</h3>
	 *
	 * @param headerSize     <span class="en-US">Header size</span>
	 *                       <span class="zh-CN">头大小</span>
	 * @param maxHeaderBytes <span class="en-US">Allowed maximum size</span>
	 *                       <span class="zh-CN">允许的最大值</span>
	 * @throws IOException <span class="en-US">Validation failed</span>
	 *                     <span class="zh-CN">验证失败</span>
	 */
	private static void validateHeaderSize(final long headerSize, final long maxHeaderBytes) throws IOException {
		if (headerSize < 0L) {
			throw new ArchiveException(0x0000001A001FL);
		}
		if (maxHeaderBytes <= 0L) {
			throw new IllegalArgumentException("maxHeaderBytes must be positive");
		}
		if (headerSize > maxHeaderBytes) {
			throw new ArchiveException(0x0000001A003EL, maxHeaderBytes, headerSize);
		}
	}

	/**
	 * <h3 class="en-US">Check weather the file header contains zip64 information</h3>
	 * <h3 class="zh-CN">检查文件头是否包含 Zip64 信息</h3>
	 *
	 * @param fileHeader <span class="en-US">File header</span>
	 *                   <span class="zh-CN">文件头</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	private static boolean hasZip64ExtraData(final FileHeader fileHeader) {
		return Optional.ofNullable(fileHeader.getExtraDataRecords())
				.map(records ->
						records.stream().anyMatch(record ->
								record.headerId() == ZipGlobals.ZIP64_EXTRA_HEADER_ID))
				.orElse(false);
	}

	/**
	 * <h3 class="en-US">Get the physical read limit</h3>
	 * <h3 class="zh-CN">获取读取的物理边界</h3>
	 *
	 * @param startDisk   <span class="en-US">Start disk number</span>
	 *                    <span class="zh-CN">起始磁盘编号</span>
	 * @param startOffset <span class="en-US">Start offset</span>
	 *                    <span class="zh-CN">起始偏移值</span>
	 * @param endDisk     <span class="en-US">End disk number</span>
	 *                    <span class="zh-CN">终止磁盘编号</span>
	 * @param endOffset   <span class="en-US">End offset</span>
	 *                    <span class="zh-CN">终止偏移值</span>
	 * @return <span class="en-US">The physical read limit</span>
	 * <span class="zh-CN">读取的物理边界</span>
	 * @throws IOException <span class="en-US">If the disk number invalid or offset invalid</span>
	 *                     <span class="zh-CN">磁盘编号错误或偏移值错误</span>
	 */
	private long physicalReadLimit(final long startDisk, final long startOffset,
	                               final long endDisk, final long endOffset) throws IOException {
		if (startDisk < 0L || endDisk < startDisk || endDisk > this.inputStream.lastDiskNumber()) {
			throw new ArchiveException(0x0000001A001FL);
		}

		long startDiskLength = this.inputStream.length(startDisk);
		long endDiskLength = this.inputStream.length(endDisk);
		if (startOffset < 0L || startOffset > startDiskLength || endOffset < 0L || endOffset > endDiskLength) {
			throw new ArchiveException(0x0000001A001FL);
		}
		if (startDisk == endDisk) {
			if (endOffset < startOffset) {
				throw new ArchiveException(0x0000001A001FL);
			}
			return endOffset - startOffset;
		}

		try {
			long readLimit = startDiskLength - startOffset;
			for (long diskNumber = startDisk + 1L; diskNumber < endDisk; diskNumber++) {
				readLimit = Math.addExact(readLimit, this.inputStream.length(diskNumber));
			}
			return Math.addExact(readLimit, endOffset);
		} catch (ArithmeticException e) {
			throw new ArchiveException(0x0000001A001FL, e);
		}
	}

	/**
	 * <h3 class="en-US">Static method for read extra data records</h3>
	 * <h3 class="zh-CN">静态方法用于读取扩展数据记录列表</h3>
	 *
	 * @param extraFieldBuffer <span class="en-US">Extra information data bytes</span>
	 *                         <span class="zh-CN">扩展信息数据</span>
	 * @return <span class="en-US">Extra data record list</span>
	 * <span class="zh-CN">扩展数据记录列表</span>
	 */
	private static List<ExtraDataRecord> readExtraDataRecords(final byte[] extraFieldBuffer) throws IOException {
		int position = 0;
		List<ExtraDataRecord> extraDataRecords = new ArrayList<>();
		while (position < extraFieldBuffer.length) {
			if ((extraFieldBuffer.length - position) < 4) {
				throw new ArchiveException(0x0000001A001DL);
			}
			int headerId = RawUtils.readUnsignedShort(extraFieldBuffer, position, ByteOrder.LITTLE_ENDIAN);
			position += Short.BYTES;
			int dataSize = RawUtils.readUnsignedShort(extraFieldBuffer, position, ByteOrder.LITTLE_ENDIAN);
			position += Short.BYTES;
			if ((extraFieldBuffer.length - position) < dataSize) {
				throw new ArchiveException(0x0000001A001DL);
			}
			byte[] dataBytes = new byte[dataSize];
			if (dataSize > 0) {
				System.arraycopy(extraFieldBuffer, position, dataBytes, 0, dataSize);
				position += dataSize;
			}
			extraDataRecords.add(new ExtraDataRecord(headerId, dataBytes));
		}
		return extraDataRecords.isEmpty() ? null : extraDataRecords;
	}

	/**
	 * <h3 class="en-US">Static method for read local Zip64 extended information</h3>
	 * <h3 class="zh-CN">静态方法用于读取本地 Zip64 扩展信息</h3>
	 *
	 * @param fileHeader       <span class="en-US">File header instance object</span>
	 *                         <span class="zh-CN">文件头实例对象</span>
	 * @param compressedSize   <span class="en-US">Compressed data size</span>
	 *                         <span class="zh-CN">压缩后数据大小</span>
	 * @param uncompressedSize <span class="en-US">Uncompressed data size</span>
	 *                         <span class="zh-CN">未压缩数据大小</span>
	 * @param strict           <span class="en-US">Strict format flag</span>
	 *                         <span class="zh-CN">严格格式标记</span>
	 * @throws IOException <span class="en-US">If an error occurs when read Zip64 extended information</span>
	 *                     <span class="zh-CN">读取 Zip64 扩展信息时出错</span>
	 */
	private static void readLocalZip64ExtendedInformation(final LocalFileHeader fileHeader,
	                                                      final long compressedSize, final long uncompressedSize,
	                                                      final boolean strict) throws IOException {
		ExtraDataRecord zip64Record = null;

		List<ExtraDataRecord> records = fileHeader.getExtraDataRecords();

		if (records != null) {
			for (ExtraDataRecord record : records) {
				if (record.headerId() != ZipGlobals.ZIP64_EXTRA_HEADER_ID) {
					continue;
				}
				if (zip64Record != null) {
					// Duplicate zip64 header found
					throw new ArchiveException(0x0000001A0013L);
				}
				zip64Record = record;
			}
		}

		boolean compressedSizePlaceholder = fileHeader.getCompressedSize() == ZipGlobals.ZIP_64_LIMIT;
		boolean uncompressedSizePlaceholder = fileHeader.getUncompressedSize() == ZipGlobals.ZIP_64_LIMIT;

		boolean localUsesZip64 = compressedSizePlaceholder || uncompressedSizePlaceholder;

		boolean deferredValues = fileHeader.hasDataDescriptor() || (fileHeader.getGeneralPurpose() & 0x2000) != 0;

		if (zip64Record == null) {
			if (localUsesZip64) {
				throw new ArchiveException(0x0000001A0012L);
			}
			return;
		}

		if (strict) {
			if (!deferredValues && (!compressedSizePlaceholder || !uncompressedSizePlaceholder)) {
				throw new ArchiveException(0x0000001A001FL);
			}

			if (localUsesZip64 && compressedSizePlaceholder != uncompressedSizePlaceholder) {
				throw new ArchiveException(0x0000001A001FL);
			}
		}
		byte[] dataBytes = zip64Record.dataBytes();

		if (dataBytes.length < Long.BYTES * 2) {
			throw new ArchiveException(0x0000001A001DL);
		}

		if (strict && dataBytes.length != Long.BYTES * 2) {
			throw new ArchiveException(0x0000001A001DL);
		}

		ByteBuffer buffer = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN);

		long zip64UncompressedSize = buffer.getLong();
		long zip64CompressedSize = buffer.getLong();

		if (deferredValues) {
			if (((zip64UncompressedSize != 0L && zip64UncompressedSize != uncompressedSize)
					|| (zip64CompressedSize != 0L && zip64CompressedSize != compressedSize))
					&& strict) {
				throw new ArchiveException(0x0000001A001FL);
			}
			return;
		}

		fileHeader.setUncompressedSize(zip64UncompressedSize);
		fileHeader.setCompressedSize(zip64CompressedSize);
	}

	/**
	 * <h3 class="en-US">Static method for read Zip64 extended information</h3>
	 * <h3 class="zh-CN">静态方法用于读取 Zip64 扩展信息</h3>
	 *
	 * @param fileHeader <span class="en-US">File header instance object</span>
	 *                   <span class="zh-CN">文件头实例对象</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when read Zip64 extended information</span>
	 *                          <span class="zh-CN">读取 Zip64 扩展信息时出错</span>
	 */
	private static void readGeneralZip64ExtendedInformation(final GeneralFileHeader fileHeader)
			throws ArchiveException {
		if (fileHeader == null) {
			throw new ArchiveException(0x0000001A0010L);
		}

		if (fileHeader.getExtraDataRecords() != null && !fileHeader.getExtraDataRecords().isEmpty()) {
			boolean processed = false;
			for (ExtraDataRecord extraDataRecord : fileHeader.getExtraDataRecords()) {
				if (extraDataRecord.headerId() == ZipGlobals.ZIP64_EXTRA_HEADER_ID) {
					if (extraDataRecord.dataBytes() == null || extraDataRecord.dataBytes().length == 0) {
						if (fileHeader.getUncompressedSize() == ZipGlobals.ZIP_64_LIMIT
								|| fileHeader.getCompressedSize() == ZipGlobals.ZIP_64_LIMIT
								|| fileHeader.getOffsetLocalHeader() == ZipGlobals.ZIP_64_LIMIT
								|| fileHeader.getDiskNumberStart() == 0xFFFFL) {
							throw new ArchiveException(0x0000001A0012L);
						}
						processed = true;
						continue;
					}
					if (processed) {
						throw new ArchiveException(0x0000001A0013L);
					}
					byte[] dataBytes = extraDataRecord.dataBytes();
					int position = 0;
					long uncompressedSize = readZip64Value(dataBytes, position, fileHeader.getUncompressedSize());
					if (uncompressedSize != Globals.DEFAULT_VALUE_LONG) {
						position += Long.BYTES;
					}
					long compressedSize = readZip64Value(dataBytes, position, fileHeader.getCompressedSize());
					if (compressedSize != Globals.DEFAULT_VALUE_LONG) {
						position += Long.BYTES;
					}
					long localHeaderOffset = readZip64Value(dataBytes, position, fileHeader.getOffsetLocalHeader());
					if (localHeaderOffset != Globals.DEFAULT_VALUE_LONG) {
						position += Long.BYTES;
					}
					if (uncompressedSize != Globals.DEFAULT_VALUE_LONG) {
						fileHeader.setUncompressedSize(uncompressedSize);
					}
					if (compressedSize != Globals.DEFAULT_VALUE_LONG) {
						fileHeader.setCompressedSize(compressedSize);
					}
					if (localHeaderOffset != Globals.DEFAULT_VALUE_LONG) {
						fileHeader.setOffsetLocalHeader(localHeaderOffset);
					}
					if (fileHeader.getDiskNumberStart() == 0xFFFFL) {
						if ((position + Integer.BYTES) > dataBytes.length) {
							throw new ArchiveException(0x0000001A001DL);
						}
						fileHeader.setDiskNumberStart(
								RawUtils.readUnsignedInt(dataBytes, position, ByteOrder.LITTLE_ENDIAN));
					}
					processed = true;
				}
			}
		}
	}

	/**
	 * <h3 class="en-US">Static method for read data value</h3>
	 * <h3 class="zh-CN">静态方法用于读取数据</h3>
	 *
	 * @param dataBytes    <span class="en-US">Binary data array</span>
	 *                     <span class="zh-CN">二进制数据</span>
	 * @param position     <span class="en-US">Data position</span>
	 *                     <span class="zh-CN">数据所在位置</span>
	 * @param currentValue <span class="en-US">Current data value</span>
	 *                     <span class="zh-CN">当前数据</span>
	 * @return <span class="en-US">Read data value, return -1 if data not existed</span>
	 * <span class="zh-CN">读取的数据，数据不存在返回 -1</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when read data</span>
	 *                          <span class="zh-CN">读取数据时出错</span>
	 */
	private static long readZip64Value(final byte[] dataBytes, final int position, final long currentValue)
			throws ArchiveException {
		if (currentValue == ZipGlobals.ZIP_64_LIMIT) {
			if ((position + Long.BYTES) > dataBytes.length) {
				throw new ArchiveException(0x0000001A001DL);
			}
			long readValue = RawUtils.readLong(dataBytes, position, ByteOrder.LITTLE_ENDIAN);
			if (readValue < 0L) {
				throw new ArchiveException(0x0000001A0011L, readValue);
			}
			return readValue;
		}
		return Globals.DEFAULT_VALUE_LONG;
	}

	private static void check(final boolean processed, final ExtraDataRecord record, final FileHeader fileHeader)
			throws ArchiveException {
		if (processed) {
			throw new ArchiveException(0x0000001A0018L);
		}
		if (record.dataBytes() == null || record.dataBytes().length == 0) {
			throw new ArchiveException(0x0000001A0012L);
		}
		if (!fileHeader.encrypted()) {
			throw new ArchiveException(0x0000001A0019L);
		}
	}

	/**
	 * <h3 class="en-US">Static method for read AES encryption information</h3>
	 * <h3 class="zh-CN">静态方法用于读取 AES 加密信息</h3>
	 *
	 * @param fileHeader <span class="en-US">File header instance object</span>
	 *                   <span class="zh-CN">文件头实例对象</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when read AES encryption information</span>
	 *                          <span class="zh-CN">读取 AES 加密信息时出错</span>
	 */
	private static void readAesEncryptionInformation(final FileHeader fileHeader) throws ArchiveException {
		if (fileHeader == null) {
			throw new ArchiveException(0x0000001A0010L);
		}
		int declaredCompressionMethod = fileHeader.getCompressionMethod();
		boolean processed = false;

		if (fileHeader.getExtraDataRecords() != null && !fileHeader.getExtraDataRecords().isEmpty()) {
			for (ExtraDataRecord record : fileHeader.getExtraDataRecords()) {
				if (record.headerId() == ZipGlobals.AES_ENC_HEADER_ID) {
					check(processed, record, fileHeader);
					if (declaredCompressionMethod != ZipGlobals.COMP_AES) {
						throw new ArchiveException(0x0000001A0021L, declaredCompressionMethod);
					}
					byte[] dataBytes = record.dataBytes();
					if (dataBytes.length != 7) {
						throw new ArchiveException(0x0000001A0015L);
					}
					int version = RawUtils.readUnsignedShort(dataBytes, 0, ByteOrder.LITTLE_ENDIAN);
					AesVendor aesVendor;
					if (version == 1) {
						aesVendor = AesVendor.AE_1;
					} else if (version == 2) {
						aesVendor = AesVendor.AE_2;
					} else {
						throw new ArchiveException(0x0000001A0016L, version);
					}
					if (dataBytes[2] != 'A' || dataBytes[3] != 'E') {
						throw new ArchiveException(0x0000001A0017L, new String(dataBytes, 2, 2));
					}
					int strength = dataBytes[4] & 0xFF;
					int compressionMethod = RawUtils.readUnsignedShort(dataBytes, 5, ByteOrder.LITTLE_ENDIAN);

					if (compressionMethod == ZipGlobals.COMP_AES || !recognizedCompressionMethod(compressionMethod)) {
						throw new ArchiveException(0x0000001A0021L, compressionMethod);
					}

					fileHeader.setEncryptionInfo(new AesEncryptionInfo(aesVendor, strength));
					fileHeader.setCompressionMethod(compressionMethod);

					processed = true;
				}
			}
		}
		if (declaredCompressionMethod == ZipGlobals.COMP_AES && !processed) {
			throw new ArchiveException(0x0000001A0022L);
		}
	}

	/**
	 * <h3 class="en-US">Static method for read strong encryption information</h3>
	 * <h3 class="zh-CN">静态方法用于读取增强加密信息</h3>
	 *
	 * @param fileHeader <span class="en-US">File header instance object</span>
	 *                   <span class="zh-CN">文件头实例对象</span>
	 * @throws ArchiveException <span class="en-US">If an error occurs when read AES encryption information</span>
	 *                          <span class="zh-CN">读取 AES 加密信息时出错</span>
	 */
	private static void readStrongEncryptionInformation(final FileHeader fileHeader) throws ArchiveException {
		if (fileHeader == null) {
			throw new ArchiveException(0x0000001A0010L);
		}
		boolean processed = false;

		if (fileHeader.getExtraDataRecords() != null && !fileHeader.getExtraDataRecords().isEmpty()) {
			for (ExtraDataRecord record : fileHeader.getExtraDataRecords()) {
				if (record.headerId() == ZipGlobals.STRONG_ENC_HEADER_ID) {
					check(processed, record, fileHeader);
					if (fileHeader.encryptionMethod() != ZipGlobals.ENC_METHOD_STRONG) {
						throw new ArchiveException(0x0000001A0025L, fileHeader.encryptionMethod());
					}
					byte[] dataBytes = record.dataBytes();
					if (dataBytes == null || dataBytes.length < 8) {
						throw new ArchiveException(0x0000001A0015L);
					}

					int format = RawUtils.readUnsignedShort(dataBytes, 0, ByteOrder.LITTLE_ENDIAN);
					if (format != 2) {
						throw new ArchiveException(0x0000001A0026L, format);
					}

					int algorithm = RawUtils.readUnsignedShort(dataBytes, 2, ByteOrder.LITTLE_ENDIAN);
					int keyBitLength = RawUtils.readUnsignedShort(dataBytes, 4, ByteOrder.LITTLE_ENDIAN);
					int flags = RawUtils.readUnsignedShort(dataBytes, 6, ByteOrder.LITTLE_ENDIAN);
					if (!validateEncryptionFlags(flags)) {
						throw new ArchiveException(0x0000001A0015L);
					}

					byte[] certificateData = Arrays.copyOfRange(dataBytes, 8, dataBytes.length);

					StrongAccessMode accessMode = StrongAccessMode.from(flags);
					if (accessMode.useCertificate()) {
						try {
							StrongRecipientTable.readCentral(certificateData, ZipGlobals.MAX_RECIPIENTS);
						} catch (IOException e) {
							throw new ArchiveException(0x0000001A0015L, e);
						}
					} else if (certificateData.length != 0) {
						throw new ArchiveException(0x0000001A0015L);
					}

					fileHeader.setEncryptionInfo(new StrongEncryptionInfo(algorithm, keyBitLength, flags, certificateData));

					processed = true;
				}
			}
		}
		if (!processed) {
			throw new ArchiveException(0x0000001A0022L);
		}
	}

	/**
	 * <h3 class="en-US">Static method for check the given compression method was recognized</h3>
	 * <h3 class="zh-CN">静态方法用于检查给定的压缩方式代码是否合法</h3>
	 *
	 * @param compressionMethod <span class="en-US">Compression method</span>
	 *                          <span class="zh-CN">压缩方式代码</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	private static boolean recognizedCompressionMethod(final int compressionMethod) {
		return (compressionMethod >= 0 && compressionMethod <= 10 && compressionMethod != 7)
				|| compressionMethod == 12 || compressionMethod == 14 || compressionMethod == 16
				|| (compressionMethod >= 18 && compressionMethod <= 20)
				|| (compressionMethod >= 93 && compressionMethod <= 99);
	}

	/**
	 * <h3 class="en-US">Static method for check the given compression method was supported</h3>
	 * <h3 class="zh-CN">静态方法用于检查给定的压缩方式代码是否支持</h3>
	 *
	 * @param compressionMethod <span class="en-US">Compression method</span>
	 *                          <span class="zh-CN">压缩方式代码</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	private static boolean supportedCompressionMethod(final int compressionMethod) {
		return switch (compressionMethod) {
			case ZipGlobals.COMP_STORE, ZipGlobals.COMP_DEFLATE, ZipGlobals.COMP_DEFLATE64,
			     ZipGlobals.COMP_BZIP2, ZipGlobals.COMP_LZMA, ZipGlobals.COMP_XZ -> true;
			default -> false;
		};
	}

	/**
	 * <h3 class="en-US">Static method for get the hash length by the given hash algorithm</h3>
	 * <h3 class="zh-CN">静态方法用于获取给定的哈希算法代码结果长度</h3>
	 *
	 * @param hashAlgorithm <span class="en-US">Hash algorithm</span>
	 *                      <span class="zh-CN">哈希算法</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	private static int expectedHashLength(final int hashAlgorithm) {
		return switch (hashAlgorithm) {
			case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE -> 0;
			case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32 -> 4;
			case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_MD5 -> 16;
			case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA1, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_RIPEMD160 -> 20;
			case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256 -> 32;
			case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA384 -> 48;
			case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA512 -> 64;
			default -> Globals.DEFAULT_VALUE_INT;
		};
	}

	private static boolean validateEncryptionFlags(final int flags) {
		int accessMode = (flags & 0x0003);
		if (accessMode != ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD
				&& accessMode != ZipGlobals.ZIP64_HEADER_ENC_FLAGS_CERTIFICATE
				&& accessMode != ZipGlobals.ZIP64_HEADER_ENC_FLAGS_BOTH) {
			return false;
		}
		return (flags & ~KNOWN_FLAGS) == 0;
	}

	/**
	 * <h3 class="en-US">Static method for validate the encryption parameters</h3>
	 * <h3 class="zh-CN">静态方法用于验证加密参数是否合法</h3>
	 *
	 * @param algorithm    <span class="en-US">Encryption algorithm code</span>
	 *                     <span class="zh-CN">加密算法代码</span>
	 * @param keyBitLength <span class="en-US">Encryption key bit length</span>
	 *                     <span class="zh-CN">加密密钥位数</span>
	 * @param flags        <span class="en-US">Credentials and procedures required for decryption</span>
	 *                     <span class="zh-CN">解密所需凭据和处理方式</span>
	 * @return <span class="en-US">Validate result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	private static boolean validateEncryptionParameters(final int algorithm, final int keyBitLength, final int flags) {
		if (algorithm == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE) {
			return keyBitLength == 0 && flags == 0;
		}
		if (!validateEncryptionFlags(flags)) {
			return false;
		}

		return switch (algorithm) {
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_DES -> keyBitLength == 56 || keyBitLength == 64;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_OLD, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW,
			     ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC4, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH ->
					keyBitLength >= 32 && keyBitLength <= 448 && (keyBitLength & 0x07) == 0;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168 -> keyBitLength == 168;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112 -> keyBitLength == 112;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128 -> keyBitLength == 128;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_192 -> keyBitLength == 192;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 -> keyBitLength == 256;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH ->
					keyBitLength == 128 || keyBitLength == 192 || keyBitLength == 256;
			case ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_UNKNOWN -> keyBitLength >= 0 && validateEncryptionFlags(flags);
			default -> false;
		};
	}

	/**
	 * <h3 class="en-US">Read protection central directory</h3>
	 * <h3 class="zh-CN">读取保护的中央仓库信息</h3>
	 *
	 * @param decryptedStream <span class="en-US">Data input stream instance object</span>
	 *                        <span class="zh-CN">数据输入流实例对象</span>
	 * @param totalEntries    <span class="en-US">Total number of entries in the central directory</span>
	 *                        <span class="zh-CN">中央目录中的条目总数</span>
	 * @param protection      <span class="en-US">Size of central directory</span>
	 *                        <span class="zh-CN">中央目录结构占用的字节数</span>
	 * @return <span class="en-US">Central directory</span>
	 * <span class="zh-CN">中央目录</span>
	 * @throws IOException <span class="en-US">If an error occurs when read central directory</span>
	 *                     <span class="zh-CN">读取中央目录时出错</span>
	 */
	private CentralDirectory readProtectionCentralDirectory(final InputStream decryptedStream, final long totalEntries,
	                                                        final Zip64EndCentralDirectory.Protection protection)
			throws IOException {
		if (protection.getCompressedSize() < 0L || protection.getUncompressedSize() < 0L) {
			throw new ArchiveException(0x0000001A001FL);
		}

		InputStream inputStream = decryptedStream;
		BufferedInputStream sourceStream = null;
		CRC32 crc = null;
		MessageDigest digest = null;

		try {
			if (protection.compressed()) {
				switch (protection.getCompressionMethod()) {
					case ZipGlobals.COMP_STORE:
						if (protection.getCompressedSize() != protection.getUncompressedSize()) {
							throw new ArchiveException(0x0000001A001FL);
						}
						break;
					case ZipGlobals.COMP_DEFLATE:
					case ZipGlobals.COMP_DEFLATE64:
						sourceStream = new BufferedInputStream(decryptedStream, Globals.DEFAULT_BUFFER_SIZE);
						inputStream = new DeflateInputStream(sourceStream,
								protection.getCompressionMethod() == ZipGlobals.COMP_DEFLATE64);
						break;
					case ZipGlobals.COMP_BZIP2:
						sourceStream = new BufferedInputStream(decryptedStream, Globals.DEFAULT_BUFFER_SIZE);
						inputStream = new BZip2InputStream(sourceStream);
						break;
					case ZipGlobals.COMP_LZMA:
						sourceStream = new BufferedInputStream(decryptedStream, Globals.DEFAULT_BUFFER_SIZE);
						inputStream = LZMAInputStream.knownSize(sourceStream, protection.getUncompressedSize());
						break;
					case ZipGlobals.COMP_XZ:
						sourceStream = new BufferedInputStream(decryptedStream, Globals.DEFAULT_BUFFER_SIZE);
						inputStream = new XZInputStream(sourceStream, false);
						break;
					default:
						throw new ArchiveException(0x0000001B004AL);
				}
			} else if (protection.getCompressedSize() != protection.getUncompressedSize()) {
				throw new ArchiveException(0x0000001A001FL);
			}

			try {
				switch (protection.getHashAlgorithm()) {
					case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE:
						break;
					case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32:
						crc = new CRC32();
						inputStream = new CheckedInputStream(inputStream, crc);
						break;
					case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_MD5:
						digest = MessageDigest.getInstance("MD5", BC_PROVIDER);
						inputStream = new DigestInputStream(inputStream, digest);
						break;
					case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA1:
						digest = MessageDigest.getInstance("SHA-1", BC_PROVIDER);
						inputStream = new DigestInputStream(inputStream, digest);
						break;
					case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_RIPEMD160:
						digest = MessageDigest.getInstance("RIPEMD160", BC_PROVIDER);
						inputStream = new DigestInputStream(inputStream, digest);
						break;
					case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256:
						digest = MessageDigest.getInstance("SHA-256", BC_PROVIDER);
						inputStream = new DigestInputStream(inputStream, digest);
						break;
					case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA384:
						digest = MessageDigest.getInstance("SHA-384", BC_PROVIDER);
						inputStream = new DigestInputStream(inputStream, digest);
						break;
					case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA512:
						digest = MessageDigest.getInstance("SHA-512", BC_PROVIDER);
						inputStream = new DigestInputStream(inputStream, digest);
						break;
					default:
						throw new ArchiveException(0x0000001A001FL);
				}
			} catch (NoSuchAlgorithmException e) {
				throw new ArchiveException(0x0000001A001FL, e);
			}

			CentralDirectory centralDirectory =
					readCentralDirectory(new DataInputStream(inputStream),
							totalEntries, protection.getUncompressedSize(), this.options, this.zipOptions);

			if (inputStream.read() != -1) {
				throw new ArchiveException(0x0000001A001FL);
			}

			if (sourceStream != null) {
				boolean trailingData = false;
				byte[] drainBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
				int readLength;

				while ((readLength = sourceStream.read(drainBuffer)) != -1) {
					if (readLength > 0) {
						trailingData = true;
					}
				}

				if (trailingData) {
					throw new ArchiveException(0x0000001A001FL);
				}
			}

			if (protection.hashed()) {
				byte[] actualHash;
				if (protection.getHashAlgorithm() == ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32) {
					if (crc == null) {
						throw new ArchiveException(0x0000001A001FL);
					}
					actualHash = ByteBuffer.allocate(Integer.BYTES)
							.order(ByteOrder.LITTLE_ENDIAN)
							.putInt((int) crc.getValue())
							.array();
				} else {
					if (digest == null) {
						throw new ArchiveException(0x0000001A001FL);
					}
					actualHash = digest.digest();
				}

				if (!MessageDigest.isEqual(protection.getHashData(), actualHash)) {
					throw new ArchiveException(0x0000001A001FL);
				}
			}

			return centralDirectory;
		} finally {
			IOUtils.closeStream(inputStream);
		}
	}

	/**
	 * <h3 class="en-US">Read plain central directory</h3>
	 * <h3 class="zh-CN">读取常规中央仓库信息</h3>
	 *
	 * @param diskNumber           <span class="en-US">Split index value</span>
	 *                             <span class="zh-CN">分卷索引号</span>
	 * @param offset               <span class="en-US">Data offset value</span>
	 *                             <span class="zh-CN">数据偏移值</span>
	 * @param totalEntries         <span class="en-US">Total number of entries in the central directory</span>
	 *                             <span class="zh-CN">中央目录中的条目总数</span>
	 * @param centralDirectorySize <span class="en-US">Size of central directory</span>
	 *                             <span class="zh-CN">中央目录结构占用的字节数</span>
	 * @return <span class="en-US">Central directory</span>
	 * <span class="zh-CN">中央目录</span>
	 * @throws IOException <span class="en-US">If an error occurs when read central directory</span>
	 *                     <span class="zh-CN">读取中央目录时出错</span>
	 */
	private CentralDirectory readPlainCentralDirectory(final long diskNumber, final long offset, final long totalEntries,
	                                                   final long centralDirectorySize) throws IOException {
		this.inputStream.seek(diskNumber, offset);
		return readCentralDirectory(
				new DataInputStream(this.inputStream), totalEntries, centralDirectorySize, this.options, this.zipOptions);
	}

	/**
	 * <h3 class="en-US">Read plain central directory</h3>
	 * <h3 class="zh-CN">读取常规中央仓库信息</h3>
	 *
	 * @param sourceStream         <span class="en-US">Data input stream instance object</span>
	 *                             <span class="zh-CN">数据输入流实例对象</span>
	 * @param totalEntries         <span class="en-US">Total number of entries in the central directory</span>
	 *                             <span class="zh-CN">中央目录中的条目总数</span>
	 * @param centralDirectorySize <span class="en-US">Size of central directory</span>
	 *                             <span class="zh-CN">中央目录结构占用的字节数</span>
	 * @param options              <span class="en-US">Archive options</span>
	 *                             <span class="zh-CN">归档选项</span>
	 * @param zipOptions           <span class="en-US">ZIP options</span>
	 *                             <span class="zh-CN">ZIP 选项</span>
	 * @return <span class="en-US">Central directory</span>
	 * <span class="zh-CN">中央目录</span>
	 * @throws IOException <span class="en-US">If an error occurs when read central directory</span>
	 *                     <span class="zh-CN">读取中央目录时出错</span>
	 */
	private static CentralDirectory readCentralDirectory(final InputStream sourceStream, final long totalEntries,
	                                                     final long centralDirectorySize,
	                                                     @Nonnull final ArchiveOptions options,
	                                                     @Nonnull final ZipOptions zipOptions) throws IOException {
		long remaining = centralDirectorySize;
		InputStream directoryStream =
				new BufferedInputStream(new BoundedInputStream(sourceStream, centralDirectorySize),
						Globals.DEFAULT_BUFFER_SIZE);
		CentralDirectory centralDirectory = new CentralDirectory(zipOptions.getDuplicatePolicy());

		byte[] readBuffer;
		DataInputStream inputStream;
		if (remaining >= 8L) {
			PushbackInputStream pushbackInputStream = new PushbackInputStream(directoryStream, 8);
			inputStream = new DataInputStream(pushbackInputStream);
			readBuffer = new byte[8];
			inputStream.readFully(readBuffer);
			if (RawUtils.readUnsignedInt(readBuffer, 0, ByteOrder.LITTLE_ENDIAN) == ZipGlobals.EXTRA_DATA_RECORD_SIGNATURE) {
				long fieldLength = RawUtils.readUnsignedInt(readBuffer, 4, ByteOrder.LITTLE_ENDIAN);
				if (fieldLength > Integer.MAX_VALUE || fieldLength > remaining - 8L) {
					throw new ArchiveException(0x0000001A001BL);
				}
				byte[] extraData = new byte[(int) fieldLength];
				inputStream.readFully(extraData);
				centralDirectory.setArchiveExtraData(extraData);
				remaining -= (8L + fieldLength);
			} else {
				pushbackInputStream.unread(readBuffer);
			}
		} else {
			inputStream = new DataInputStream(directoryStream);
		}

		readBuffer = new byte[CENTRAL_FILE_HEADER_FIXED_SIZE];
		List<GeneralFileHeader> fileHeaders = new ArrayList<>();
		for (long i = 0; i < totalEntries; i++) {
			if (remaining < CENTRAL_FILE_HEADER_FIXED_SIZE) {
				throw new ArchiveException(0x0000001A001CL);
			}
			inputStream.readFully(readBuffer);
			if (RawUtils.readInt(readBuffer, 0, ByteOrder.LITTLE_ENDIAN) != ZipGlobals.GENERAL_FILE_HEADER_SIGNATURE) {
				throw new ArchiveException(0x0000001A000CL);
			}
			remaining -= CENTRAL_FILE_HEADER_FIXED_SIZE;

			GeneralFileHeader fileHeader = new GeneralFileHeader();
			fileHeader.setMadeVersion(RawUtils.readUnsignedShort(readBuffer, 4, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setExtractNeeded(RawUtils.readUnsignedShort(readBuffer, 6, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setGeneralPurpose(RawUtils.readUnsignedShort(readBuffer, 8, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setCompressionMethod(RawUtils.readUnsignedShort(readBuffer, 10, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setLastModifiedTime(RawUtils.readUnsignedShort(readBuffer, 12, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setLastModifiedDate(RawUtils.readUnsignedShort(readBuffer, 14, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setCrc32(RawUtils.readUnsignedInt(readBuffer, 16, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setCompressedSize(RawUtils.readUnsignedInt(readBuffer, 20, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setUncompressedSize(RawUtils.readUnsignedInt(readBuffer, 24, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setNameLength(RawUtils.readUnsignedShort(readBuffer, 28, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setExtraFieldLength(RawUtils.readUnsignedShort(readBuffer, 30, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setCommentLength(RawUtils.readUnsignedShort(readBuffer, 32, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setDiskNumberStart(RawUtils.readUnsignedShort(readBuffer, 34, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setInternalFileAttr(RawUtils.readUnsignedShort(readBuffer, 36, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setExternalFileAttr(RawUtils.readUnsignedInt(readBuffer, 38, ByteOrder.LITTLE_ENDIAN));
			fileHeader.setOffsetLocalHeader(RawUtils.readUnsignedInt(readBuffer, 42, ByteOrder.LITTLE_ENDIAN));

			long totalLength =
					(long) fileHeader.getNameLength() + fileHeader.getExtraFieldLength() + fileHeader.getCommentLength();
			if (totalLength > remaining) {
				throw new ArchiveException(0x0000001A001CL);
			}

			Charset charset = fileHeader.utf8Encoded() ? StandardCharsets.UTF_8 : options.getCharset();
			if (fileHeader.getNameLength() > 0) {
				byte[] nameBuffer = new byte[fileHeader.getNameLength()];
				inputStream.readFully(nameBuffer);
				String entryPath = new String(nameBuffer, charset);
				if (options.isStrict()) {
					validateEntryPath(entryPath);
				}
				fileHeader.setEntryPath(entryPath);
			} else {
				fileHeader.setEntryPath(null);
			}

			//  Read extra field if exists
			if (fileHeader.getExtraFieldLength() > 0) {
				byte[] extraData = new byte[fileHeader.getExtraFieldLength()];
				inputStream.readFully(extraData);
				fileHeader.setExtraDataRecords(readExtraDataRecords(extraData));
			}

			//  Read Zip64 extended information
			readGeneralZip64ExtendedInformation(fileHeader);

			//  Read encryption information
			readAesEncryptionInformation(fileHeader);
			if (fileHeader.encryptionMethod() == ZipGlobals.ENC_METHOD_STRONG) {
				readStrongEncryptionInformation(fileHeader);
			}

			if (fileHeader.getCommentLength() > 0) {
				byte[] commentBuffer = new byte[fileHeader.getCommentLength()];
				inputStream.readFully(commentBuffer);
				fileHeader.setFileComment(
						new String(commentBuffer, fileHeader.utf8Encoded() ? StandardCharsets.UTF_8 : charset));
			}
			fileHeaders.add(fileHeader);

			remaining -= totalLength;
		}

		centralDirectory.setFileHeaders(fileHeaders);

		if (remaining == 0L) {
			centralDirectory.setSignatureData(new byte[0]);
		} else {
			if (remaining < 6L) {
				throw new ArchiveException(0x0000001A001CL);
			}
			byte[] dataBytes = new byte[6];
			inputStream.readFully(dataBytes);
			remaining -= 6L;
			if (RawUtils.readInt(dataBytes, 0, ByteOrder.LITTLE_ENDIAN) != ZipGlobals.DIGITAL_SIGNATURE) {
				throw new ArchiveException(0x0000001A000CL);
			}
			int length = RawUtils.readUnsignedShort(dataBytes, 4, ByteOrder.LITTLE_ENDIAN);
			if (length != remaining) {
				throw new ArchiveException(0x0000001A001CL);
			}
			byte[] signatureData = new byte[length];
			inputStream.readFully(signatureData);
			centralDirectory.setSignatureData(signatureData);
		}

		return centralDirectory;
	}

	/**
	 * <h3 class="en-US">Check weather the given entry path is valid</h3>
	 * <h3 class="zh-CN">检查给定的条目路径是否合法</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @throws ArchiveException <span class="en-US">Entry path invalid</span>
	 *                          <span class="zh-CN">路径非法</span>
	 */
	private static void validateEntryPath(final String entryPath) throws ArchiveException {
		if (entryPath == null || entryPath.isEmpty()) {
			return;
		}

		if (entryPath.indexOf('\0') >= 0) {
			throw new ArchiveException(0x0000001A001FL, entryPath);
		}
		if (entryPath.startsWith("/")) {
			throw new ArchiveException(0x0000001A001FL, entryPath);
		}
		if (entryPath.startsWith("\\")) {
			throw new ArchiveException(0x0000001A001FL, entryPath);
		}
		if (entryPath.length() >= 2) {
			char firstChar = entryPath.charAt(0);
			boolean driveLetter = (firstChar >= 'A' && firstChar <= 'Z') || (firstChar >= 'a' && firstChar <= 'z');
			if (driveLetter && entryPath.charAt(1) == ':') {
				throw new ArchiveException(0x0000001A001FL, entryPath);
			}
		}
		if (entryPath.indexOf('\\') >= 0) {
			throw new ArchiveException(0x0000001A001FL, entryPath);
		}
		for (String pathSegment : entryPath.split("/", -1)) {
			if ("..".equals(pathSegment)) {
				throw new ArchiveException(0x0000001A001FL, entryPath);
			}
		}
	}
}
