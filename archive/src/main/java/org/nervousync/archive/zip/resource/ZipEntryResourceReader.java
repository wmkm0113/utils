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

package org.nervousync.archive.zip.resource;

import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.enumerations.EntryType;
import org.nervousync.archive.enumerations.zip.PasswordPurpose;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.io.bzip2.BZip2InputStream;
import org.nervousync.archive.io.deflate.DeflateInputStream;
import org.nervousync.archive.io.lzma.LZMAInputStream;
import org.nervousync.archive.io.xz.XZInputStream;
import org.nervousync.archive.options.ArchiveLimit;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.security.password.PasswordCredentials;
import org.nervousync.archive.security.password.PasswordRequest;
import org.nervousync.archive.zip.ZipAdaptor;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.crypto.aes.AesInputStream;
import org.nervousync.archive.zip.crypto.standard.StandardInputStream;
import org.nervousync.archive.zip.crypto.strong.StrongInputStream;
import org.nervousync.archive.zip.io.AbstractFilterInputStream;
import org.nervousync.archive.zip.io.BoundedInputStream;
import org.nervousync.archive.zip.io.ZipArchiveInputStream;
import org.nervousync.archive.zip.models.EntryMetaData;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.archive.zip.models.header.FileHeader;
import org.nervousync.archive.zip.models.header.LocalFileHeader;
import org.nervousync.archive.zip.models.header.StrongDecryptionHeader;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.IOUtils;
import org.nervousync.utils.commons.RawUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * <h2 class="en-US">ZIP entry resource reader implement class</h2>
 * <h2 class="zh-CN">ZIP 条目资源读取器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ZipEntryResourceReader implements ResourceReader {

	/**
	 * <span class="en-US">Current archive file input stream</span>
	 * <span class="zh-CN">当前压缩文件输入流</span>
	 */
	private final ZipArchiveInputStream inputStream;
	/**
	 * <span class="en-US">Zip adaptor</span>
	 * <span class="zh-CN">Zip 适配器</span>
	 */
	private final ZipAdaptor zipAdaptor;
	/**
	 * <span class="en-US">Archive options</span>
	 * <span class="zh-CN">归档选项</span>
	 */
	private final ArchiveOptions options;
	/**
	 * <span class="en-US">Zip options</span>
	 * <span class="zh-CN">Zip 选项</span>
	 */
	private final ZipOptions zipOptions;
	/**
	 * <span class="en-US">Archive entry</span>
	 * <span class="zh-CN">归档条目</span>
	 */
	private final LocalFileHeader fileHeader;
	/**
	 * <span class="en-US">Current entry input stream</span>
	 * <span class="zh-CN">当前条目的输入流</span>
	 */
	private InputStream entryStream = null;
	/**
	 * <span class="en-US">Input stream closed flag</span>
	 * <span class="zh-CN">输入流关闭标记</span>
	 */
	private boolean closed = false;
	/**
	 * <span class="en-US">Full Item Attributes</span>
	 * <span class="zh-CN">完整条目属性</span>
	 */
	private final FileHeader metadataHeader;

	/**
	 * <h3 class="en-US">Constructor method for the ZIP entry resource reader implement class</h3>
	 * <h3 class="zh-CN">ZIP 条目资源读取器实现类的构造方法</h3>
	 *
	 * @param inputStream      <span class="en-US">Zip archive file input stream</span>
	 *                         <span class="zh-CN">Zip 归档文件输入流</span>
	 * @param zipAdaptor       <span class="en-US">Zip adaptor</span>
	 *                         <span class="zh-CN">Zip 适配器</span>
	 * @param options          <span class="en-US">Archive options</span>
	 *                         <span class="zh-CN">归档选项</span>
	 * @param zipOptions       <span class="en-US">Zip options</span>
	 *                         <span class="zh-CN">Zip 选项</span>
	 * @param fileHeader       <span class="en-US">Local file header</span>
	 *                         <span class="zh-CN">本地文件头</span>
	 * @param metadataHeader   <span class="en-US">Full Item Attributes</span>
	 *                         <span class="zh-CN">完整条目属性</span>
	 */
	public ZipEntryResourceReader(final ZipArchiveInputStream inputStream, final ZipAdaptor zipAdaptor,
	                              final ArchiveOptions options, final ZipOptions zipOptions,
	                              final LocalFileHeader fileHeader, final FileHeader metadataHeader) {
		this.inputStream = inputStream;
		this.zipAdaptor = zipAdaptor;
		this.options = options;
		this.zipOptions = zipOptions;
		this.fileHeader = fileHeader;
		this.metadataHeader = Objects.requireNonNull(metadataHeader, "metadataHeader");
	}

	public InputStream sourceStream() throws IOException {
		this.inputStream.seek(this.fileHeader.getDiskNumberStart(), this.fileHeader.getOffsetStartOfData());
		return new BoundedInputStream(this.inputStream, this.fileHeader.getCompressedSize());
	}

	@Override
	public InputStream openStream() throws IOException {
		InputStream inputStream = this.sourceStream();
		if (this.fileHeader.encrypted()) {
			if (this.fileHeader.encryptionMethod() == ZipGlobals.ENC_METHOD_STRONG) {
				StrongDecryptionHeader decryptionHeader =
						ZipCrypto.readStrongDecryptionHeader(new DataInputStream(inputStream),
								this.options.getArchiveLimit().getMaxHeaderBytes());
				inputStream = StrongInputStream.entry(this.inputStream.archivePath(), this.fileHeader.getEntryPath(),
						inputStream, fileHeader.getCompressedSize(), this.options.getPasswordProvider(),
						this.options.getCertificateProvider(),
						(StrongEncryptionInfo) this.fileHeader.getEncryptionInfo(), decryptionHeader);
			} else {
				PasswordRequest request =
						new PasswordRequest(PasswordPurpose.READ_ENTRY, this.inputStream.archivePath(),
								this.fileHeader.getEntryPath(), 1);
				byte[] passwordBytes = null;
				char[] password = null;
				try (PasswordCredentials credentials =
						     this.options.getPasswordProvider().credentials(request)
								     .orElseThrow(() ->
										     new ArchiveException(0x0000001B004CL, this.fileHeader.getEntryPath()))) {
					password = credentials.copyPassword();
					passwordBytes = ZipCrypto.encodePassword(password, credentials.isUtf8Encoding());
					switch (this.fileHeader.encryptionMethod()) {
						case ZipGlobals.ENC_METHOD_AES:
							inputStream =
									new AesInputStream(inputStream, (AesEncryptionInfo)
											this.fileHeader.getEncryptionInfo(), this.fileHeader.getCompressedSize(),
											passwordBytes);
							break;
						case ZipGlobals.ENC_METHOD_STANDARD:
							long storedSize = this.fileHeader.getCompressedSize();
							if (storedSize < ZipGlobals.STD_DEC_HDR_SIZE) {
								throw new ArchiveException(0x0000001A001FL);
							}
							byte[] decryptorHeader = new byte[ZipGlobals.STD_DEC_HDR_SIZE];
							new DataInputStream(inputStream).readFully(decryptorHeader);
							long encryptedSize = this.fileHeader.getCompressedSize() - ZipGlobals.STD_DEC_HDR_SIZE;
							int checkByte = this.fileHeader.hasDataDescriptor()
									? (this.fileHeader.getLastModifiedTime() >>> 8) & 0xFF
									: (int) ((this.fileHeader.getCrc32() >>> 24) & 0xFF);
							inputStream = new StandardInputStream(inputStream, encryptedSize,
									passwordBytes, decryptorHeader, checkByte);
							break;
						case ZipGlobals.ENC_NO_ENCRYPTION:
							break;
						default:
							throw new ArchiveException(0x0000001A0023L);
					}
				} finally {
					RawUtils.clear(password);
					RawUtils.clear(passwordBytes);
				}
			}
		}

		try {
			switch (this.fileHeader.getCompressionMethod()) {
				case ZipGlobals.COMP_STORE ->
						this.entryStream = new EntryVerificationInputStream(inputStream, this.fileHeader.getUncompressedSize(),
								this.fileHeader.getCompressedSize(), this.options.getArchiveLimit(), this.fileHeader.getCrc32(),
								this.options.isVerify() && ZipAdaptor.verifyCRC(this.fileHeader));
				case ZipGlobals.COMP_DEFLATE, ZipGlobals.COMP_DEFLATE64 ->
						this.entryStream = new EntryVerificationInputStream(
								new DeflateInputStream(
										new BufferedInputStream(inputStream, Globals.DEFAULT_BUFFER_SIZE),
										this.fileHeader.getCompressionMethod() == ZipGlobals.COMP_DEFLATE64),
								this.fileHeader.getUncompressedSize(), this.fileHeader.getCompressedSize(),
								this.options.getArchiveLimit(), this.fileHeader.getCrc32(),
								this.options.isVerify() && ZipAdaptor.verifyCRC(this.fileHeader));
				case ZipGlobals.COMP_BZIP2 -> {
					inputStream = new BufferedInputStream(inputStream, Globals.DEFAULT_BUFFER_SIZE);
					this.entryStream = new EntryVerificationInputStream(
							new BZip2InputStream(inputStream), this.fileHeader.getUncompressedSize(),
							this.fileHeader.getCompressedSize(), this.options.getArchiveLimit(), this.fileHeader.getCrc32(),
							this.options.isVerify() && ZipAdaptor.verifyCRC(this.fileHeader));
				}
				case ZipGlobals.COMP_LZMA -> {
					inputStream = new BufferedInputStream(inputStream, Globals.DEFAULT_BUFFER_SIZE);

					this.entryStream = new EntryVerificationInputStream(
							new LZMAInputStream(inputStream, this.fileHeader.getUncompressedSize(),
									(this.fileHeader.getGeneralPurpose() & 0x0002) != 0),
							this.fileHeader.getUncompressedSize(), this.fileHeader.getCompressedSize(),
							this.options.getArchiveLimit(), this.fileHeader.getCrc32(),
							this.options.isVerify() && ZipAdaptor.verifyCRC(this.fileHeader));
				}
				case ZipGlobals.COMP_XZ -> this.entryStream = new EntryVerificationInputStream(
						new XZInputStream(inputStream, false), this.fileHeader.getUncompressedSize(),
						this.fileHeader.getCompressedSize(), this.options.getArchiveLimit(), this.fileHeader.getCrc32(),
						this.options.isVerify() && ZipAdaptor.verifyCRC(this.fileHeader));
				default -> throw new ArchiveException(0x0000001B004AL, fileHeader.getCompressionMethod());
			}
		} catch (IOException | RuntimeException e) {
			IOUtils.closeStream(inputStream);
			throw e;
		}
		return this.entryStream;
	}

	@Override
	public void extract(final Path destination, final ExtractOptions options) throws IOException {
		Objects.requireNonNull(destination, "destination");
		Objects.requireNonNull(options, "options");

		final Path target = destination.toAbsolutePath().normalize();
		final Path parent = target.getParent();

		if (parent == null) {
			throw new ArchiveException(0x0000001A002BL);
		}

		if (this.metadataHeader.entryType() != EntryType.FILE) {
			this.zipAdaptor.extractLinkTo(target, this.metadataHeader, this, options);
			return;
		}

		if (options.isCreateDirectories()) {
			Files.createDirectories(parent);
		} else if (!Files.isDirectory(parent)) {
			throw new ArchiveException(0x0000001A002CL);
		}

		if (ArchiveFile.existsTarget(target, options.getOverrideMode(),
				this.metadataHeader.modifiedInstant(this.zipOptions.getTimeZone(), this.options.isStrict()))) {
			return;
		}

		this.validateDeclaredLimits();

		final Path temporary = Files.createTempFile(parent, ".nervousync-", ".tmp");
		boolean succeed = false;
		try {
			try (InputStream input = this.openStream();
			     OutputStream output =
					     Files.newOutputStream(temporary, StandardOpenOption.WRITE,
							     StandardOpenOption.TRUNCATE_EXISTING)) {
				input.transferTo(output);
			}

			if (ArchiveFile.existsTarget(target, options.getOverrideMode(),
					this.metadataHeader.modifiedInstant(this.zipOptions.getTimeZone(), this.options.isStrict()))) {
				return;
			}

			EntryMetaData.applyBeforePublish(temporary, this.metadataHeader, options, this.options, this.zipOptions);
			ZipAdaptor.temporaryToTarget(temporary, target, options);
			succeed = true;
			EntryMetaData.applyDosAttributes(target, this.metadataHeader, options);
		} finally {
			if (!succeed) {
				Files.deleteIfExists(temporary);
			}
		}
	}

	@Override
	public String path() {
		return this.metadataHeader.getEntryPath();
	}

	@Override
	public synchronized void close() throws Exception {
		if (this.closed) {
			return;
		}

		Exception failure = null;

		try {
			if (this.entryStream != null) {
				this.entryStream.close();
			}
		} catch (Exception e) {
			failure = e;
		}

		try {
			this.inputStream.close();
		} catch (Exception e) {
			if (failure == null) {
				failure = e;
			} else {
				failure.addSuppressed(e);
			}
		} finally {
			this.closed = true;
		}

		if (failure != null) {
			throw failure;
		}
	}

	private void validateDeclaredLimits() throws IOException {
		ArchiveLimit archiveLimit = this.options.getArchiveLimit();

		long compressedSize = this.fileHeader.getCompressedSize();
		long uncompressedSize = this.fileHeader.getUncompressedSize();

		if (compressedSize < 0L || uncompressedSize < 0L) {
			throw new ArchiveException(0x0000001A0011L);
		}
		if (compressedSize > archiveLimit.getMaxCompressedBytes()) {
			throw new ArchiveException(0x0000001A0033L, compressedSize, archiveLimit.getMaxCompressedBytes());
		}
		if (uncompressedSize > archiveLimit.getMaxEntryBytes()) {
			throw new ArchiveException(0x0000001A001BL);
		}

		if (uncompressedSize == 0L) {
			return;
		}

		if (compressedSize == 0L) {
			throw new ArchiveException(0x0000001A001BL);
		}

		double compressionRatio =
				(double) uncompressedSize / (double) compressedSize;

		if (compressionRatio > archiveLimit.getMaxCompressionRatio()) {
			throw new ArchiveException(0x0000001A001BL);
		}
	}

	/**
	 * <h2 class="en-US">Input stream with data validation</h2>
	 * <h2 class="zh-CN">带有数据验证的输入流</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	private static final class EntryVerificationInputStream extends AbstractFilterInputStream {

		/**
		 * <span class="en-US">Expected size</span>
		 * <span class="zh-CN">预期长度</span>
		 */
		private final long expectedSize;
		/**
		 * <span class="en-US">Compressed size</span>
		 * <span class="zh-CN">压缩长度</span>
		 */
		private final long compressedSize;
		/**
		 * <span class="en-US">Archive security limit</span>
		 * <span class="zh-CN">归档安全限制</span>
		 */
		private final ArchiveLimit archiveLimit;
		/**
		 * <span class="en-US">Expected CRC</span>
		 * <span class="zh-CN">预期 CRC</span>
		 */
		private final long expectedCrc;
		/**
		 * <span class="en-US">Verify CRC flag</span>
		 * <span class="zh-CN">验证 CRC 标记</span>
		 */
		private final boolean verifyCrc;

		/**
		 * <span class="en-US">CRC calculator</span>
		 * <span class="zh-CN">CRC 计算器</span>
		 */
		private final CRC32 crc32 = new CRC32();

		/**
		 * <span class="en-US">Read size</span>
		 * <span class="zh-CN">读取长度</span>
		 */
		private long readSize;
		/**
		 * <span class="en-US">Finish flag</span>
		 * <span class="zh-CN">完成标记</span>
		 */
		private boolean finished;

		/**
		 * <h3 class="en-US">Private constructor method for the input stream with data validation</h3>
		 * <h3 class="zh-CN">带有数据验证的输入流的私有构造方法</h3>
		 *
		 * @param inputStream    <span class="en-US">Input stream</span>
		 *                       <span class="zh-CN">输入流</span>
		 * @param expectedSize   <span class="en-US">Expected size</span>
		 *                       <span class="zh-CN">预期长度</span>
		 * @param compressedSize <span class="en-US">Compressed size</span>
		 *                       <span class="zh-CN">压缩长度</span>
		 * @param archiveLimit   <span class="en-US">Archive security limit</span>
		 *                       <span class="zh-CN">归档安全限制</span>
		 * @param expectedCrc    <span class="en-US">Expected CRC</span>
		 *                       <span class="zh-CN">预期 CRC</span>
		 * @param verifyCrc      <span class="en-US">Verify CRC flag</span>
		 *                       <span class="zh-CN">验证 CRC 标记</span>
		 */
		EntryVerificationInputStream(final InputStream inputStream, final long expectedSize, final long compressedSize,
		                             final ArchiveLimit archiveLimit, final long expectedCrc, final boolean verifyCrc) {
			super(Objects.requireNonNull(inputStream));
			this.expectedSize = expectedSize;
			this.compressedSize = compressedSize;
			this.archiveLimit = Objects.requireNonNull(archiveLimit);
			this.expectedCrc = expectedCrc;
			this.verifyCrc = verifyCrc;
		}

		@Override
		public int read() throws IOException {
			if (this.finished) {
				return -1;
			}

			int value = this.in.read();
			if (value < 0) {
				this.finishEntry();
				return -1;
			}

			byte[] data = {(byte) value};
			this.account(data, 0, 1);
			return value;
		}

		@Override
		public int read(final byte[] buffer, final int offset, final int length) throws IOException {
			Objects.checkFromIndexSize(offset, length, buffer.length);
			if (length == 0) {
				return 0;
			}
			if (this.finished) {
				return -1;
			}
			int readLength = this.in.read(buffer, offset, length);
			if (readLength < 0) {
				this.finishEntry();
				return -1;
			}
			if (readLength > 0) {
				this.account(buffer, offset, readLength);
			}
			return readLength;
		}

		@Override
		protected void executeClose() throws IOException {
			this.finishEntry();
		}

		private void account(final byte[] buffer, final int offset, final int length) throws IOException {
			try {
				this.readSize = Math.addExact(this.readSize, length);
			} catch (ArithmeticException e) {
				throw new ArchiveException(0x0000001A002FL, e);
			}

			if (this.readSize > this.expectedSize) {
				throw new ArchiveException(0x0000001A002FL);
			}

			if (this.readSize > this.archiveLimit.getMaxEntryBytes()) {
				throw new ArchiveException(0x0000001A0030L);
			}

			if (compressionRatioExceeded(this.readSize, this.compressedSize, this.archiveLimit.getMaxCompressionRatio())) {
				throw new ArchiveException(0x0000001A0031L);
			}

			this.crc32.update(buffer, offset, length);
		}

		private void finishEntry() throws IOException {
			if (this.finished) {
				return;
			}

			this.finished = true;
			IOException failure = null;

			try {
				this.in.close();
			} catch (IOException e) {
				failure = e;
			}

			if (this.readSize != this.expectedSize) {
				failure = merge(failure, new ArchiveException(0x0000001A002FL));
			}

			if (this.verifyCrc && this.crc32.getValue() != this.expectedCrc) {
				failure = merge(failure, new ArchiveException(0x0000001A0032L));
			}

			if (failure != null) {
				throw failure;
			}
		}

		private static boolean compressionRatioExceeded(final long uncompressedSize, final long compressedSize,
		                                                final double maxCompressionRatio) {
			if (uncompressedSize <= 0L) {
				return false;
			}
			if (compressedSize <= 0L) {
				return true;
			}
			return (double) uncompressedSize / compressedSize > maxCompressionRatio;
		}

		private static IOException merge(final IOException first, final IOException second) {
			if (first == null) {
				return second;
			}
			first.addSuppressed(second);
			return first;
		}
	}
}
