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
package org.nervousync.zip.io;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.commons.zip.ZipGlobals;
import org.nervousync.enumerations.zip.CompressionType;
import org.nervousync.enumerations.zip.Zip64Mode;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.RawUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.zip.ZipFile;
import org.nervousync.zip.crypto.Cryptor;
import org.nervousync.zip.crypto.impl.aes.AESEncryptor;
import org.nervousync.zip.crypto.impl.aes.AESEngine;
import org.nervousync.zip.crypto.impl.standard.StandardEncryptor;
import org.nervousync.zip.models.AESExtraDataRecord;
import org.nervousync.zip.models.central.CentralDirectory;
import org.nervousync.zip.models.central.EndCentralDirectoryRecord;
import org.nervousync.zip.models.header.GeneralFileHeader;
import org.nervousync.zip.models.header.LocalFileHeader;
import org.nervousync.zip.models.header.utils.HeaderOperator;
import org.nervousync.zip.options.ZipOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.zip.CRC32;

/**
 * The type Cipher output stream.
 *
 * @author Steven Wee <a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 29, 2017 14:39:25 $
 */
public class CipherOutputStream extends OutputStream {

	/**
	 * Target output stream
	 */
	private final OutputStream outputStream;
	/**
	 * One byte buffer
	 */
	private final byte[] oneByteBuffer = new byte[1];
	/**
	 * Source file
	 */
	private File sourceFile;
	private GeneralFileHeader generalFileHeader;
	private LocalFileHeader localFileHeader;
	private Cryptor encryptor;
	/**
	 * The Zip options.
	 */
	ZipOptions zipOptions;
	/**
	 * Target zip file object
	 */
	private final ZipFile zipFile;
	/**
	 * CRC
	 */
	final CRC32 crc;
	private long totalWriteBytes;
	private long totalReadBytes;
	/**
	 * The Bytes written for this file.
	 */
	long bytesWrittenForThisFile;
	private final byte[] pendingBuffer;
	private int pendingBufferLength;

	/**
	 * Instantiates a new Cipher output stream.
	 *
	 * @param outputStream the output stream
	 * @param zipFile      the zip file
	 */
	CipherOutputStream(final OutputStream outputStream, final ZipFile zipFile) {
		this.outputStream = outputStream;
		this.zipFile = zipFile;
		this.initZipFile();
		this.crc = new CRC32();
		this.totalWriteBytes = 0L;
		this.bytesWrittenForThisFile = 0L;
		this.totalReadBytes = 0L;
		this.pendingBuffer = new byte[ZipGlobals.AES_BLOCK_SIZE];
		this.pendingBufferLength = 0;
	}

	/**
	 * Put the next entry.
	 *
	 * @param file       the file
	 * @param zipOptions the zip options
	 * @throws ZipException the zip exception
	 */
	public void putNextEntry(final File file, final ZipOptions zipOptions) throws ZipException {
		if (zipOptions == null) {
			throw new ZipException(0x0000001B0021L);
		}
		if (!zipOptions.isSourceExternalStream() && file == null) {
			throw new ZipException(0x0000001B0013L);
		}

		if (!zipOptions.isSourceExternalStream() && !FileUtils.isExists(file.getAbsolutePath())) {
			throw new ZipException(0x0000001B0013L);
		}

		try {
			this.sourceFile = file;
			this.zipOptions = zipOptions;

			if (this.zipOptions.isSourceExternalStream()) {
				if (StringUtils.notBlank(this.zipOptions.getFileNameInZip())) {
					if (this.zipOptions.getFileNameInZip().endsWith(Globals.DEFAULT_ZIP_PAGE_SEPARATOR)
							|| this.zipOptions.getFileNameInZip().endsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
						this.zipOptions =
								zipOptions.cloneBuilder().compressionType(CompressionType.NONE).noCrypto().build();
						this.zipOptions.setSourceFileCRC(zipOptions.getSourceFileCRC());
						this.zipOptions.setDefaultFolderPath(zipOptions.getDefaultFolderPath());
						this.zipOptions.setSourceExternalStream(zipOptions.isSourceExternalStream());
					}
				}
			} else {
				if (this.sourceFile.isDirectory()) {
					this.zipOptions =
							zipOptions.cloneBuilder().compressionType(CompressionType.NONE).noCrypto().build();
					this.zipOptions.setSourceFileCRC(zipOptions.getSourceFileCRC());
					this.zipOptions.setDefaultFolderPath(zipOptions.getDefaultFolderPath());
					this.zipOptions.setSourceExternalStream(zipOptions.isSourceExternalStream());
				}
			}

			this.createGeneralFileHeaders();
			this.createLocalFileHeaders();

			if (this.zipFile.isSplitArchive()) {
				if (this.zipFile.getCentralDirectory() == null
						|| this.zipFile.getCentralDirectory().getFileHeaders() == null
						|| this.zipFile.getCentralDirectory().getFileHeaders().isEmpty()) {
					byte[] intBuffer = new byte[4];
					RawUtils.writeInt(intBuffer, ByteOrder.LITTLE_ENDIAN, (int) ZipGlobals.EXTSIG);
					this.outputStream.write(intBuffer);
					this.totalWriteBytes += 4L;
				}
			}

			if (this.outputStream instanceof SplitOutputStream) {
				if (this.totalWriteBytes == 4) {
					this.generalFileHeader.setOffsetLocalHeader(4L);
				} else {
					this.generalFileHeader
							.setOffsetLocalHeader(((SplitOutputStream) this.outputStream).getFilePointer());
				}
			} else {
				if (this.totalWriteBytes == 4) {
					this.generalFileHeader.setOffsetLocalHeader(4L);
				} else {
					this.generalFileHeader.setOffsetLocalHeader(this.totalWriteBytes);
				}
			}

			this.totalWriteBytes += this.writeLocalFileHeader(this.localFileHeader, this.outputStream);

			if (this.zipOptions.isEncryptFiles()) {
				this.initEncryptor();
				if (this.encryptor != null) {
					if (this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_STANDARD) {
						byte[] headerBytes = ((StandardEncryptor) this.encryptor).getHeaderBytes();
						this.outputStream.write(headerBytes);
						this.totalWriteBytes += headerBytes.length;
						this.bytesWrittenForThisFile += headerBytes.length;
					} else if (this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
						byte[] saltBytes = ((AESEncryptor) this.encryptor).getSaltBytes();
						byte[] passwordVerifier = ((AESEncryptor) this.encryptor).getDerivedPasswordVerifier();
						this.outputStream.write(saltBytes);
						this.outputStream.write(passwordVerifier);
						this.totalWriteBytes += saltBytes.length + passwordVerifier.length;
						this.bytesWrittenForThisFile += saltBytes.length + passwordVerifier.length;
					}
				}
			}

			this.crc.reset();
		} catch (ZipException e) {
			throw e;
		} catch (Exception e) {
			throw new ZipException(0x0000001B0015L, e);
		}
	}

	@Override
	public void write(final int b) throws IOException {
		this.oneByteBuffer[0] =  (byte) b;
		this.write(this.oneByteBuffer, 0, 1);
	}

	@Override
	public void write(@Nonnull final byte[] b) throws IOException {
		if (b.length == 0) {
			return;
		}
		this.write(b, 0, b.length);
	}

	@Override
	public void write(@Nonnull final byte[] b, final int off, final int len) throws IOException {
		if (len == 0) {
			return;
		}
		int offset = off, length = len;
		if (this.zipOptions.isEncryptFiles() && this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
			if (this.pendingBufferLength != 0) {
				if (len >= (ZipGlobals.AES_BLOCK_SIZE - this.pendingBufferLength)) {
					System.arraycopy(b, off, this.pendingBuffer, this.pendingBufferLength,
							(ZipGlobals.AES_BLOCK_SIZE - this.pendingBufferLength));
					this.encryptAndWrite(this.pendingBuffer, 0, this.pendingBuffer.length);
					offset = (ZipGlobals.AES_BLOCK_SIZE - this.pendingBufferLength);
					length -= offset;
					this.pendingBufferLength = 0;
				} else {
					System.arraycopy(b, offset, this.pendingBuffer, this.pendingBufferLength, length);
					this.pendingBufferLength += length;
					return;
				}
			}

			if (length % 16 != 0) {
				System.arraycopy(b, (length + offset) - (length % 16), this.pendingBuffer, 0, length % 16);
				this.pendingBufferLength = length % 16;
				length -= this.pendingBufferLength;
			}
		}
		if (length != 0) {
			this.encryptAndWrite(b, offset, length);
		}
	}

	/**
	 * Close entry.
	 *
	 * @throws IOException  the io exception
	 * @throws ZipException the zip exception
	 */
	public void closeEntry() throws IOException, ZipException {
		if (this.pendingBufferLength != 0) {
			this.encryptAndWrite(this.pendingBuffer, 0, this.pendingBufferLength);
			this.pendingBufferLength = 0;
		}

		if (this.zipOptions.isEncryptFiles() && this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
			if (this.encryptor instanceof AESEncryptor) {
				this.outputStream.write(((AESEncryptor) this.encryptor).getFinalMac());
				this.bytesWrittenForThisFile += 10;
				this.totalWriteBytes += 10;
			} else {
				throw new ZipException(0x0000001B0014L);
			}
		}

		this.generalFileHeader.setCompressedSize(this.bytesWrittenForThisFile);
		this.localFileHeader.setCompressedSize(this.bytesWrittenForThisFile);

		if (this.zipOptions.isSourceExternalStream()) {
			this.generalFileHeader.setOriginalSize(this.totalReadBytes);
			if (this.localFileHeader.getOriginalSize() != this.totalReadBytes) {
				this.localFileHeader.setOriginalSize(this.totalReadBytes);
			}
		}

		long crc32 = this.crc.getValue();

		if (this.generalFileHeader.isEncrypted()
				&& this.generalFileHeader.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
			crc32 = 0L;
		}

		this.generalFileHeader.setCrc32(crc32);
		this.localFileHeader.setCrc32(crc32);

		this.zipFile.getLocalFileHeaderList().add(this.localFileHeader);
		this.zipFile.getCentralDirectory().getFileHeaders().add(this.generalFileHeader);

		try {
			this.totalWriteBytes += HeaderOperator.writeExtendedLocalHeader(this.localFileHeader, this.outputStream);
		} catch (DataInvalidException e) {
			throw new IOException(e);
		}

		this.crc.reset();
		this.bytesWrittenForThisFile = 0L;
		this.encryptor = null;
		this.totalReadBytes = 0L;
	}

	/**
	 * Finish.
	 *
	 * @throws ZipException the zip exception
	 */
	public void finish() throws ZipException {
		this.zipFile.getEndCentralDirectoryRecord().setOffsetOfStartOfCentralDirectory(this.totalWriteBytes);
		this.zipFile.finalizeZipFile(this.outputStream);
	}

	@Override
	public void close() throws IOException {
		if (this.outputStream != null) {
			this.outputStream.close();
		}
	}

	/**
	 * Update total bytes read.
	 *
	 * @param readCount the read count
	 */
	void updateTotalBytesRead(int readCount) {
		if (readCount > 0) {
			this.totalReadBytes += readCount;
		}
	}

	private void encryptAndWrite(final byte[] b, final int off, final int len) throws IOException {
		if (this.encryptor != null) {
			try {
				this.encryptor.process(b, off, len);
			} catch (ZipException e) {
				throw new IOException(e);
			}
		}

		this.outputStream.write(b, off, len);
		this.totalWriteBytes += len;
		this.bytesWrittenForThisFile += len;
	}

	private void initZipFile() {
		if (this.zipFile.getEndCentralDirectoryRecord() == null) {
			this.zipFile.setEndCentralDirectoryRecord(new EndCentralDirectoryRecord());
		}

		if (this.zipFile.getCentralDirectory() == null) {
			this.zipFile.setCentralDirectory(new CentralDirectory());
		}

		if (this.zipFile.getCentralDirectory().getFileHeaders() == null) {
			this.zipFile.getCentralDirectory().setFileHeaders(new ArrayList<>());
		}

		if (this.zipFile.getLocalFileHeaderList() == null) {
			this.zipFile.setLocalFileHeaderList(new ArrayList<>());
		}

		if (this.outputStream instanceof SplitOutputStream) {
			if (((SplitOutputStream) this.outputStream).isSplitZipFile()) {
				this.zipFile.setSplitArchive(true);
				this.zipFile.setSplitLength(((SplitOutputStream) this.outputStream).getSplitLength());
			}
		}

		this.zipFile.getEndCentralDirectoryRecord().setSignature(ZipGlobals.ENDSIG);
	}

	private void createGeneralFileHeaders() throws ZipException {
		boolean zip64Entry = this.zip64Entry();
		this.generalFileHeader = new GeneralFileHeader();

		this.generalFileHeader.setZip64Entry(zip64Entry);

		this.generalFileHeader.setSignature((int) ZipGlobals.CENSIG);
		this.generalFileHeader.setMadeVersion(zip64Entry ? ZipGlobals.ZIP_64_VERSION : 20);
		this.generalFileHeader.setExtractNeeded(zip64Entry ? ZipGlobals.ZIP_64_VERSION : 20);

		if (this.zipOptions.isEncryptFiles() && this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
			this.generalFileHeader.setCompressionMethod(ZipGlobals.ENC_METHOD_AES);
			this.generateAESExtraDataRecord();
		} else {
			this.generalFileHeader.setCompressionMethod(this.zipOptions.getCompressionMethod());
		}

		if (this.zipOptions.isEncryptFiles()) {
			this.generalFileHeader.setEncrypted(Boolean.TRUE);
			this.generalFileHeader.setEncryptionMethod(this.zipOptions.getEncryptionMethod());
		}

		String entryPath;
		if (this.zipOptions.isSourceExternalStream()) {
			this.generalFileHeader.setLastModFileTime((int) DateTimeUtils.toDosTime(System.currentTimeMillis()));
			if (this.zipOptions.getFileNameInZip() == null || this.zipOptions.getFileNameInZip().isEmpty()) {
				throw new ZipException(0x0000001B0016L);
			}
			entryPath = this.zipOptions.getFileNameInZip();
		} else {
			this.generalFileHeader.setLastModFileTime(
					(int) DateTimeUtils.toDosTime(FileUtils.lastModify(this.sourceFile.getAbsolutePath())));
			this.generalFileHeader.setOriginalSize(this.sourceFile.length());
			entryPath = ZipFile.getRelativeFileName(this.sourceFile.getAbsolutePath(),
					this.zipOptions.getRootFolderInZip(), this.zipOptions.getDefaultFolderPath());
		}

		if (entryPath == null || entryPath.isEmpty()) {
			throw new ZipException(0x0000001B0016L);
		}
		this.generalFileHeader.setEntryPath(entryPath);

		if (StringUtils.notBlank(this.zipFile.getCharsetEncoding())) {
			this.generalFileHeader.setFileNameLength(
					StringUtils.encodedStringLength(entryPath, this.zipFile.getCharsetEncoding()));
		} else {
			this.generalFileHeader.setFileNameLength(StringUtils.encodedStringLength(entryPath));
		}

		if (this.outputStream instanceof SplitOutputStream) {
			this.generalFileHeader
					.setDiskNumberStart(((SplitOutputStream) this.outputStream).getCurrentSplitFileIndex());
		} else {
			this.generalFileHeader.setDiskNumberStart(0);
		}

		int fileAttrs = 0;

		if (!this.zipOptions.isSourceExternalStream()) {
			fileAttrs = this.getFileAttributes(this.sourceFile);
		}

		byte[] externalFileAttrs = {(byte) fileAttrs, 0, 0, 0};
		this.generalFileHeader.setExternalFileAttr(externalFileAttrs);

		boolean isDirectory;
		if (this.zipOptions.isSourceExternalStream()) {
			isDirectory = entryPath.endsWith(Globals.DEFAULT_ZIP_PAGE_SEPARATOR)
					|| entryPath.endsWith(Globals.DEFAULT_PAGE_SEPARATOR);
		} else {
			isDirectory = this.sourceFile.isDirectory();
		}
		this.generalFileHeader.setDirectory(isDirectory);

		if (this.generalFileHeader.isDirectory()) {
			this.generalFileHeader.setCompressedSize(0L);
			this.generalFileHeader.setOriginalSize(0L);
		} else {
			if (!this.zipOptions.isSourceExternalStream()) {
				long fileSize = FileUtils.fileSize(this.sourceFile);
				if (this.zipOptions.getCompressionMethod() == ZipGlobals.COMP_STORE) {
					if (this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_STANDARD) {
						this.generalFileHeader.setCompressedSize(fileSize + ZipGlobals.STD_DEC_HDR_SIZE);
					} else if (this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
						int saltLength = switch (this.zipOptions.getAesKeyStrength()) {
							case ZipGlobals.AES_STRENGTH_128 -> 8;
							case ZipGlobals.AES_STRENGTH_256 -> 16;
							default -> throw new ZipException(0x0000001B0005L);
						};
						this.generalFileHeader
								.setCompressedSize(fileSize + saltLength + ZipGlobals.AES_AUTH_LENGTH + 2);
					} else {
						this.generalFileHeader.setCompressedSize(0L);
					}
				} else {
					this.generalFileHeader.setCompressedSize(0L);
				}
				this.generalFileHeader.setOriginalSize(fileSize);
			}
		}

		if (this.zipOptions.isEncryptFiles()
				&& this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_STANDARD) {
			this.generalFileHeader.setCrc32(this.zipOptions.getSourceFileCRC());
		}

		byte[] generalPurposeFlag = new byte[2];
		int[] bitArray = this.generateGeneralPurposeBitArray(this.generalFileHeader.isEncrypted(),
				this.zipOptions.getCompressionMethod());
		generalPurposeFlag[0] = RawUtils.bitArrayToByte(bitArray);

		boolean isFileNameCharset = StringUtils.notBlank(this.zipFile.getCharsetEncoding());

		if ((isFileNameCharset && this.zipFile.getCharsetEncoding().equalsIgnoreCase(Globals.DEFAULT_ENCODING))
				|| (!isFileNameCharset && StringUtils.detectCharset(this.generalFileHeader.getEntryPath())
				.equalsIgnoreCase(Globals.DEFAULT_ENCODING))) {
			generalPurposeFlag[1] = 8;
		}

		this.generalFileHeader.setGeneralPurposeFlag(generalPurposeFlag);
	}

	private void generateAESExtraDataRecord() throws ZipException {
		if (this.zipOptions == null) {
			throw new ZipException(0x000000FF0001L);
		}

		AESExtraDataRecord aesExtraDataRecord = new AESExtraDataRecord();
		aesExtraDataRecord.setSignature(ZipGlobals.AESSIG);
		aesExtraDataRecord.setDataSize(7);
		aesExtraDataRecord.setVendorID("AE");

		aesExtraDataRecord.setVersionNumber(2);

		if (this.zipOptions.getAesKeyStrength() == ZipGlobals.AES_STRENGTH_128) {
			aesExtraDataRecord.setAesStrength(ZipGlobals.AES_STRENGTH_128);
		} else if (this.zipOptions.getAesKeyStrength() == ZipGlobals.AES_STRENGTH_256) {
			aesExtraDataRecord.setAesStrength(ZipGlobals.AES_STRENGTH_256);
		} else {
			throw new ZipException(0x0000001B0005L);
		}

		aesExtraDataRecord.setCompressionMethod(this.zipOptions.getCompressionMethod());

		this.generalFileHeader.setAesExtraDataRecord(aesExtraDataRecord);
	}

	private int getFileAttributes(final File file) throws ZipException {
		if (file == null) {
			throw new ZipException(0x000000FF0001L);
		}

		if (!file.exists()) {
			return 0;
		}

		if (file.isDirectory()) {
			return Globals.FOLDER_MODE_NONE;
		} else {
			if (!file.canWrite()) {
				return Globals.FILE_MODE_READ_ONLY;
			} else {
				return Globals.FILE_MODE_NONE;
			}
		}
	}

	private int[] generateGeneralPurposeBitArray(final boolean isEncrypted, final int compressionMethod) {
		int[] generalPurposeFlag = new int[8];

		if (isEncrypted) {
			generalPurposeFlag[0] = 1;
		} else {
			generalPurposeFlag[0] = 0;
		}

		if (compressionMethod != ZipGlobals.COMP_DEFLATE) {
			generalPurposeFlag[1] = 0;
			generalPurposeFlag[2] = 0;
		}

		generalPurposeFlag[3] = 1;

		return generalPurposeFlag;
	}

	private void createLocalFileHeaders() throws ZipException {
		if (this.generalFileHeader == null) {
			throw new ZipException(0x0000001B000FL);
		}

		this.localFileHeader = new LocalFileHeader();

		this.localFileHeader.setSignature((int) ZipGlobals.LOCSIG);
		this.localFileHeader.setExtractNeeded(this.generalFileHeader.getExtractNeeded());
		this.localFileHeader.setCompressionMethod(this.generalFileHeader.getCompressionMethod());
		this.localFileHeader.setLastModFileTime(this.generalFileHeader.getLastModFileTime());
		this.localFileHeader.setOriginalSize(this.generalFileHeader.getOriginalSize());
		this.localFileHeader.setFileNameLength(this.generalFileHeader.getFileNameLength());
		this.localFileHeader.setEntryPath(this.generalFileHeader.getEntryPath());
		this.localFileHeader.setEncrypted(this.generalFileHeader.isEncrypted());
		this.localFileHeader.setEncryptionMethod(this.generalFileHeader.getEncryptionMethod());
		this.localFileHeader.setAesExtraDataRecord(this.generalFileHeader.getAesExtraDataRecord());
		this.localFileHeader.setCrc32(this.generalFileHeader.getCrc32());
		this.localFileHeader.setCompressedSize(this.generalFileHeader.getCompressedSize());
		this.localFileHeader.setZip64Entry(this.generalFileHeader.isZip64Entry());
		this.localFileHeader.setWriteCompressSizeInZip64ExtraRecord(this.generalFileHeader.isZip64Entry());
		this.localFileHeader.setGeneralPurposeFlag(this.generalFileHeader.getGeneralPurposeFlag().clone());
	}

	private void initEncryptor() throws ZipException {
		if (this.zipOptions.isEncryptFiles()) {
			switch (this.zipOptions.getEncryptionMethod()) {
				case ZipGlobals.ENC_METHOD_STANDARD:
					this.encryptor = new StandardEncryptor(this.zipOptions.getPassword(),
							(this.localFileHeader.getLastModFileTime() & 0x0000FFFF) << 16);
					break;
				case ZipGlobals.ENC_METHOD_AES:
					this.encryptor = new AESEncryptor(this.zipOptions.getPassword(), this.zipOptions.getAesKeyStrength());
					break;
				default:
					throw new ZipException(0x0000001B0001L);
			}
		} else {
			this.encryptor = null;
		}
	}

	private int writeLocalFileHeader(final LocalFileHeader localFileHeader, final OutputStream outputStream)
			throws ZipException {
		if (localFileHeader == null) {
			throw new ZipException(0x0000001B000EL);
		}
		try {
			ByteArrayOutputStream headerBytes =
					new ByteArrayOutputStream(30 + localFileHeader.getFileNameLength() + 32);

			HeaderOperator.writeInt(localFileHeader.getSignature(), headerBytes);
			HeaderOperator.writeShort((short) localFileHeader.getExtractNeeded(), headerBytes);
			HeaderOperator.writeBytes(localFileHeader.getGeneralPurposeFlag(), headerBytes);
			HeaderOperator.writeShort((short) localFileHeader.getCompressionMethod(), headerBytes);
			HeaderOperator.writeInt(localFileHeader.getLastModFileTime(), headerBytes);
			HeaderOperator.writeInt((int) localFileHeader.getCrc32(), headerBytes);

			boolean writingZip64Record = localFileHeader.isWriteCompressSizeInZip64ExtraRecord();

			if (writingZip64Record) {
				localFileHeader.setExtractNeeded(ZipGlobals.ZIP_64_VERSION);
				localFileHeader.setWriteCompressSizeInZip64ExtraRecord(Boolean.TRUE);

				HeaderOperator.writeInt((int) ZipGlobals.ZIP_64_LIMIT, headerBytes);
				HeaderOperator.writeInt((int) ZipGlobals.ZIP_64_LIMIT, headerBytes);
			} else {
				HeaderOperator.writeInt((int) localFileHeader.getCompressedSize(), headerBytes);
				HeaderOperator.writeInt((int) localFileHeader.getOriginalSize(), headerBytes);
			}

			HeaderOperator.writeShort((short) localFileHeader.getFileNameLength(), headerBytes);

			int extraFieldLength = 0;
			if (writingZip64Record) {
				extraFieldLength += 20;
			}

			if (localFileHeader.getAesExtraDataRecord() != null) {
				extraFieldLength += 11;
			}

			HeaderOperator.writeShort((short) extraFieldLength, headerBytes);

			if (StringUtils.notBlank(this.zipFile.getCharsetEncoding())) {
				HeaderOperator.writeBytes(localFileHeader.getEntryPath().getBytes(this.zipFile.getCharsetEncoding()), headerBytes);
			} else {
				HeaderOperator.writeEntryPath(localFileHeader.getEntryPath(), headerBytes);
			}

			if (writingZip64Record) {
				HeaderOperator.writeShort((short) ZipGlobals.EXTRAFIELDZIP64LENGTH, headerBytes);
				HeaderOperator.writeShort((short) 16, headerBytes);
				HeaderOperator.writeLong(this.zipOptions.isSourceExternalStream() ? 0L : localFileHeader.getOriginalSize(),
						headerBytes);
				HeaderOperator.writeLong(0L, headerBytes);
			}

			if (localFileHeader.getAesExtraDataRecord() != null) {
				AESEngine.processHeader(localFileHeader.getAesExtraDataRecord(), headerBytes);
			}

			int expectSize = 30 + localFileHeader.getFileNameLength() + extraFieldLength;
			if (headerBytes.size() != expectSize) {
				throw new ZipException(0x0000001B0048L);
			}

			headerBytes.writeTo(outputStream);
			return headerBytes.size();
		} catch (Exception e) {
			throw new ZipException(0x0000001B0017L, e);
		}
	}

	private boolean zip64Entry() {
		if (Zip64Mode.ALWAYS.equals(this.zipOptions.getZip64Mode())) {
			return Boolean.TRUE;
		}
		if (this.zipOptions.isSourceExternalStream()) {
			return this.zipOptions.getZip64Mode() != Zip64Mode.NEVER;
		}

		if (this.sourceFile == null) {
			throw new ZipException(0x0000001B0013L);
		}

		long originalSize = this.sourceFile.length();
		long maximumCompressedSize = this.maximumCompressedSize(originalSize);
		boolean required = originalSize >= ZipGlobals.ZIP_64_LIMIT || maximumCompressedSize >= ZipGlobals.ZIP_64_LIMIT;

		if (required && Zip64Mode.NEVER.equals(this.zipOptions.getZip64Mode())) {
			throw new ZipException(0x0000001B0058L);
		}
		return required;
	}

	private long maximumCompressedSize(final long originalSize) {
		long result = originalSize;
		if (this.zipOptions.getCompressionMethod() == ZipGlobals.COMP_DEFLATE) {
			result = saturatedAdd(result, originalSize >>> 12);
			result = saturatedAdd(result, originalSize >>> 14);
			result = saturatedAdd(result, originalSize >>> 25);
			result = saturatedAdd(result, 13L);
		}

		if (this.zipOptions.isEncryptFiles()) {
			if (this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_STANDARD) {
				result = saturatedAdd(result, ZipGlobals.STD_DEC_HDR_SIZE);
			} else if (this.zipOptions.getEncryptionMethod() == ZipGlobals.ENC_METHOD_AES) {
				try {
					result = saturatedAdd(result,
							HeaderOperator.saltLength(this.zipOptions.getAesKeyStrength()) + ZipGlobals.AES_AUTH_LENGTH + 2L);
				} catch (ZipException ignore) {
					return Long.MAX_VALUE;
				}
			}
		}
		return result;
	}

	private static long saturatedAdd(final long left, final long right) {
		if (right > 0L && left > Long.MAX_VALUE - right) {
			return Long.MAX_VALUE;
		}
		return left + right;
	}
}
