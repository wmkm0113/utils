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

import jakarta.annotation.Nonnull;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.enumerations.*;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.io.bzip2.BZip2OutputStream;
import org.nervousync.archive.io.deflate.DeflateOutputStream;
import org.nervousync.archive.io.zip.ZipEntryOutputStream;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.StrongKey;
import org.nervousync.archive.security.password.PasswordCredentials;
import org.nervousync.archive.security.password.PasswordRequest;
import org.nervousync.archive.zip.ZipAdaptor;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.crypto.aes.AesOutputStream;
import org.nervousync.archive.zip.crypto.standard.StandardOutputStream;
import org.nervousync.archive.zip.crypto.strong.StrongOutputStream;
import org.nervousync.archive.zip.models.EntryTimes;
import org.nervousync.archive.zip.models.ExtraDataRecord;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.archive.zip.models.header.LocalFileHeader;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.commons.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <h2 class="en-US">ZIP entry resource writer implement class</h2>
 * <h2 class="zh-CN">ZIP 条目资源写入器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ZipEntryResourceWriter implements ResourceWriter {

	/**
	 * <span class="en-US">Zip adaptor</span>
	 * <span class="zh-CN">Zip 适配器</span>
	 */
	private final ZipAdaptor adaptor;
	/**
	 * <span class="en-US">Entry temporary file</span>
	 * <span class="zh-CN">条目临时文件</span>
	 */
	private final Path temporaryFile;
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
	 * <span class="en-US">Entry path</span>
	 * <span class="zh-CN">条目路径</span>
	 */
	private final String entryPath;
	/**
	 * <span class="en-US">Comment string</span>
	 * <span class="zh-CN">批注信息</span>
	 */
	private final String comment;
	/**
	 * <span class="en-US">Zip entry output stream</span>
	 * <span class="zh-CN">Zip 条目输出流</span>
	 */
	private ZipEntryOutputStream outputStream = null;
	/**
	 * <span class="en-US">Deflater</span>
	 * <span class="zh-CN">压缩器</span>
	 */
	private int dosTime;
	private ArchiveEntry sourceEntry = null;
	private LocalFileHeader completedHeader = null;
	private StrongEncryptionInfo strongEncryptionInfo = null;
	/**
	 * <span class="en-US">Abort flag</span>
	 * <span class="zh-CN">取消标记</span>
	 */
	private boolean abort = false;
	/**
	 * <span class="en-US">Input stream closed flag</span>
	 * <span class="zh-CN">输入流关闭标记</span>
	 */
	private boolean closed = false;

	/**
	 * <h3 class="en-US">Constructor method for the ZIP entry resource writer implement class</h3>
	 * <h3 class="zh-CN">ZIP 条目资源写入器实现类的构造方法</h3>
	 *
	 * @param adaptor       <span class="en-US">Zip adaptor</span>
	 *                      <span class="zh-CN">Zip 适配器</span>
	 * @param tempDirectory <span class="en-US">Temporary directory</span>
	 *                      <span class="zh-CN">临时目录</span>
	 * @param options       <span class="en-US">Archive options</span>
	 *                      <span class="zh-CN">归档选项</span>
	 * @param zipOptions    <span class="en-US">Zip options</span>
	 *                      <span class="zh-CN">Zip 选项</span>
	 * @param entryPath     <span class="en-US">The entry path</span>
	 *                      <span class="zh-CN">条目路径</span>
	 * @param comment       <span class="en-US">Comment string</span>
	 *                      <span class="zh-CN">批注信息</span>
	 * @throws IOException <span class="en-US">Initialize input stream error</span>
	 *                     <span class="zh-CN">初始化输入流出错</span>
	 */
	public ZipEntryResourceWriter(final ZipAdaptor adaptor, final Path tempDirectory, final ArchiveOptions options,
	                              final ZipOptions zipOptions, final String entryPath, final String comment)
			throws IOException {
		this.adaptor = adaptor;
		this.temporaryFile = Files.createTempFile(tempDirectory, "entry-", ".bin");
		this.options = options;
		this.zipOptions = zipOptions;
		this.entryPath = entryPath;
		this.comment = StringUtils.isEmpty(comment) ? Globals.DEFAULT_VALUE_STRING : comment;
	}

	@Override
	public void write(final Path path) throws IOException {
		final ArchiveEntry entry = ArchiveEntry.fromPath(path, this.entryPath);

		try (OutputStream output = this.openStream(entry)) {
			switch (entry.getEntryType()) {
				case FILE:
					Files.copy(path, output);
					break;
				case SYMBOLIC_LINK:
					output.write(entry.getLinkTarget().getBytes(this.options.getCharset()));
					break;
				case DIRECTORY:
					break;
				default:
					throw new IOException("Unsupported source entry type: " + entry.getEntryType());
			}
		} catch (IOException | RuntimeException e) {
			this.abort();
			throw e;
		}
	}

	@Override
	public OutputStream openStream(@Nonnull final ArchiveEntry entry) throws IOException {
		Objects.requireNonNull(entry, "entry");
		if (this.closed || this.abort || this.outputStream != null || this.sourceEntry != null) {
			throw new IOException("Entry metadata must be supplied before opening its stream");
		}
		final int expectedType = this.entryTypeCode(entry);
		final int mode = entry.getUnixMode();

		if (mode >= 0) {
			final int modeType = mode & 0xF000;
			if (modeType != 0 && modeType != expectedType) {
				throw new IOException("Unix mode and entry type disagree");
			}
		}

		EntryTimes.encode(entry);
		final long lastModified =
				Integer.toUnsignedLong(EntryTimes.dosTime(entry.getLastModified(), this.zipOptions.getTimeZone()));
		this.sourceEntry = entry;
		return this.openStream(lastModified);
	}

	@Override
	public OutputStream openStream(final long lastModified) throws IOException {
		if (this.closed || this.abort) {
			throw new ArchiveException(0x0000001A0040L);
		}
		if (this.outputStream != null) {
			return this.outputStream;
		}
		this.dosTime = (int) lastModified;
		OutputStream outputStream = Files.newOutputStream(this.temporaryFile);
		try {
			int encryptionMethod =
					this.isDirectoryEntry() ? ZipGlobals.ENC_NO_ENCRYPTION : this.zipOptions.getEncryptionMethod();
			if (encryptionMethod == ZipGlobals.ENC_METHOD_STRONG
					&& this.zipOptions.getAccessMode() == StrongAccessMode.CERTIFICATE) {
				outputStream = StrongOutputStream.create(outputStream, this.zipOptions.getEncryptionAlgorithm(),
						this.zipOptions.getKeyBitLength(), this.zipOptions.getAccessMode(),
						null, this.zipOptions.getEntryRecipients());
				this.strongEncryptionInfo = ((StrongOutputStream) outputStream).getEncryptionInfo();
			} else if (encryptionMethod != ZipGlobals.ENC_NO_ENCRYPTION) {
				PasswordRequest request =
						new PasswordRequest(PasswordPurpose.WRITE_ENTRY, this.adaptor.getFilePath(), this.entryPath, 1);

				byte[] passwordBytes = null;
				char[] password = null;
				try (PasswordCredentials credentials =
						     this.options.getPasswordProvider().credentials(request)
								     .orElseThrow(() -> new ArchiveException(0x0000001B004CL, this.entryPath))) {
					password = credentials.copyPassword();
					passwordBytes = ZipCrypto.encodePassword(password, credentials.isUtf8Encoding());
					switch (encryptionMethod) {
						case ZipGlobals.ENC_METHOD_AES:
							outputStream =
									new AesOutputStream(outputStream, this.zipOptions.getAesKeyStrength(), passwordBytes);
							break;
						case ZipGlobals.ENC_METHOD_STANDARD:
							outputStream =
									new StandardOutputStream(outputStream, passwordBytes,
											(((int) lastModified) & 0x0000FFFF) << 16);
							break;
						case ZipGlobals.ENC_METHOD_STRONG:
							StrongOutputStream strongOutputStream =
									StrongOutputStream.create(outputStream, this.zipOptions.getEncryptionAlgorithm(),
											this.zipOptions.getKeyBitLength(), this.zipOptions.getAccessMode(),
											passwordBytes, this.zipOptions.getEntryRecipients());
							this.strongEncryptionInfo = strongOutputStream.getEncryptionInfo();
							outputStream = strongOutputStream;
							break;
						default:
							throw new ArchiveException(0x0000001A0023L);
					}
				} finally {
					RawUtils.clear(password);
					RawUtils.clear(passwordBytes);
				}
			}

			switch (this.isDirectoryEntry() ? CompressionAlgorithm.STORE : this.zipOptions.getAlgorithm()) {
				case STORE:
					break;
				case DEFLATE:
				case DEFLATE64:
					outputStream = new DeflateOutputStream(outputStream,
							this.zipOptions.getAlgorithm() == CompressionAlgorithm.DEFLATE64,
							this.zipOptions.getCompressionLevel());
					break;
				case BZIP2:
					outputStream =
							new BZip2OutputStream(outputStream, Math.max(1, this.zipOptions.getCompressionLevel()));
					break;
				case LZMA1:
				case LZMA2:
					//  Not support at this time
					break;
				default:
					throw new ArchiveException(0x0000001A0021L);
			}
			this.outputStream = new ZipEntryOutputStream(outputStream);
			return this.outputStream;
		} catch (IOException | RuntimeException e) {
			this.abort = true;
			try {
				outputStream.close();
			} catch (IOException | RuntimeException ex) {
				e.addSuppressed(ex);
			}
			throw e;
		}
	}

	@Override
	public void abort() {
		if (this.abort) {
			return;
		}
		if (this.outputStream != null) {
			this.outputStream.abort();
		}
		this.abort = true;
	}

	@Override
	public void finish() throws IOException {
		this.adaptor.modifyEntry(this.entryPath);
	}

	@Override
	public String path() {
		return this.entryPath;
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		try {
			if (this.outputStream != null) {
				this.outputStream.close();
			}
		} catch (IOException | RuntimeException e) {
			this.abort = true;
			throw e;
		} finally {
			this.closed = true;
		}
	}

	/**
	 * <h3 class="en-US">Local file header information</h3>
	 * <h3 class="zh-CN">本地文件头信息</h3>
	 *
	 * @return <span class="en-US">Local file header information</span>
	 * <span class="zh-CN">本地文件头信息</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	public LocalFileHeader fileHeader() throws IOException {
		this.close();

		if (this.abort || this.outputStream == null || this.outputStream.isAborted()) {
			throw new IOException("ZIP entry was not completed: " + this.entryPath);
		}
		if (this.completedHeader != null) {
			return this.completedHeader;
		}

		long originalSize = this.outputStream.getUncompressedSize();
		long storedSize = Files.size(this.temporaryFile);

		if (this.isDirectoryEntry() && originalSize != 0L) {
			throw new IOException("Directory entries cannot contain data: " + this.entryPath);
		}

		boolean largeSizes = originalSize >= ZipGlobals.ZIP_64_LIMIT || storedSize >= ZipGlobals.ZIP_64_LIMIT;

		if (largeSizes && this.zipOptions.getZip64Mode() == Zip64Mode.NEVER) {
			throw new IOException("ZIP64 is required for entry: " + this.entryPath);
		}

		boolean zip64 = largeSizes || this.zipOptions.getZip64Mode() == Zip64Mode.ALWAYS;

		int method = switch (this.isDirectoryEntry() ? CompressionAlgorithm.STORE : this.zipOptions.getAlgorithm()) {
			case STORE -> ZipGlobals.COMP_STORE;
			case DEFLATE -> ZipGlobals.COMP_DEFLATE;
			case DEFLATE64 -> ZipGlobals.COMP_DEFLATE64;
			case BZIP2 -> ZipGlobals.COMP_BZIP2;
			default -> throw new IOException(
					"Unsupported ZIP compression: " + this.zipOptions.getAlgorithm());
		};

		LocalFileHeader header = new LocalFileHeader();
		List<ExtraDataRecord> extras = new ArrayList<>();

		int flags = StandardCharsets.UTF_8.equals(this.options.getCharset()) ? 0x0800 : 0;
		int version = method == ZipGlobals.COMP_DEFLATE64 ? 21 : 20;
		if (method == ZipGlobals.COMP_BZIP2) {
			version = 46;
		}
		long crc32 = this.outputStream.getCrc();

		switch (this.isDirectoryEntry() ? ZipGlobals.ENC_NO_ENCRYPTION : this.zipOptions.getEncryptionMethod()) {
			case ZipGlobals.ENC_NO_ENCRYPTION:
				break;
			case ZipGlobals.ENC_METHOD_STANDARD:
				flags |= 0x0001 | 0x0008;
				break;
			case ZipGlobals.ENC_METHOD_AES:
				flags |= 0x0001;
				version = 51;
				AesVendor aesVendor = this.zipOptions.getAesVendor();
				if (aesVendor == AesVendor.AE_2) {
					//  AE-2
					crc32 = 0L;
				}
				int strength = this.zipOptions.getAesKeyStrength();
				header.setEncryptionInfo(new AesEncryptionInfo(aesVendor, strength));

				ByteBuffer aesBuffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);
				aesBuffer.putShort((short) aesVendor.getVersion());
				aesBuffer.put((byte) 'A');
				aesBuffer.put((byte) 'E');
				aesBuffer.put((byte) strength);
				aesBuffer.putShort((short) method);

				extras.add(new ExtraDataRecord(ZipGlobals.AES_ENC_HEADER_ID, aesBuffer.array()));
				method = 99;
				break;
			case ZipGlobals.ENC_METHOD_STRONG:
				flags |= 0x0001 | 0x0040;
				version = 63;
				if (this.zipOptions.getEncryptionAlgorithm() == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_112
						|| this.zipOptions.getEncryptionAlgorithm() == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168) {
					version = 50;
				}
				StrongEncryptionInfo info = Objects.requireNonNull(this.strongEncryptionInfo, "strongEncryptionInfo");
				version = Math.max(version, StrongKey.neededVersion(info.getAlgorithm(), info.getKeyBitLength(), info.getFlags()));
				header.setEncryptionInfo(info);
				break;
			default:
				throw new ArchiveException(0x0000001A0023L);
		}

		if (zip64) {
			ByteBuffer zip64Buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
			zip64Buffer.putLong(originalSize);
			zip64Buffer.putLong(storedSize);

			extras.add(0, new ExtraDataRecord(ZipGlobals.ZIP64_EXTRA_HEADER_ID, zip64Buffer.array()));
			version = Math.max(version, ZipGlobals.ZIP_64_VERSION);
		}

		header.setSignature((int) ZipGlobals.LOCAL_FILE_HEADER_SIGNATURE);
		header.setMadeVersion(version);
		header.setExtractNeeded(version);
		header.setGeneralPurpose(flags);
		header.setCompressionMethod(method);
		header.setLastModifiedTime(this.dosTime & 0xFFFF);
		header.setLastModifiedDate((this.dosTime >>> 16) & 0xFFFF);

		header.setEntryPath(this.entryPath);
		header.setFilePath(this.temporaryFile.toString());
		header.setCrc32(crc32);
		header.setUncompressedSize(originalSize);
		header.setCompressedSize(storedSize);
		if (this.sourceEntry != null) {
			extras.addAll(EntryTimes.encode(this.sourceEntry));
			header.setTimestampMetadataCentralOnly(
					this.zipOptions.getCentralDirectoryEncryptionAlgorithm() != ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE);
		}
		header.setExtraDataRecords(extras);

		header.setNameLength(
				this.entryPath.getBytes(header.utf8Encoded() ? StandardCharsets.UTF_8 : this.options.getCharset()).length);

		header.setComment(this.comment);

		int extraLength = 0;
		for (ExtraDataRecord record : extras) {
			extraLength += 4 + record.dataBytes().length;
		}

		header.setExtraFieldLength(extraLength);

		int dosAttributes =
				this.sourceEntry == null || this.sourceEntry.getDosAttributes() < 0
						? 0
						: this.sourceEntry.getDosAttributes();
		dosAttributes = (dosAttributes & ~0x10) | (this.isDirectoryEntry() ? 0x10 : 0);

		int unixMode = this.sourceEntry == null ? -1 : this.sourceEntry.getUnixMode();
		if (this.sourceEntry != null && this.sourceEntry.getEntryType() == EntryType.SYMBOLIC_LINK && unixMode < 0) {
			unixMode = 0x01FF;
		}
		if (unixMode >= 0) {
			final int fileType;
			if (this.isDirectoryEntry()) {
				fileType = 0x4000;
			} else if (this.sourceEntry.getEntryType() == EntryType.SYMBOLIC_LINK) {
				fileType = 0xA000;
			} else {
				fileType = 0x8000;
			}

			unixMode = (unixMode & 0x0FFF) | fileType;
			header.setMadeVersion(0x0300 | version);
		}

		header.setExternalFileAttr((unixMode < 0 ? 0L : ((long) unixMode << 16)) | dosAttributes);

		this.completedHeader = header;
		return header;
	}

	/**
	 * <h3 class="en-US">Get current entry type code</h3>
	 * <h3 class="zh-CN">获取当前条目类型代码</h3>
	 *
	 * @return <span class="en-US">Type code</span>
	 * <span class="zh-CN">类型代码</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	private int entryTypeCode(@Nonnull final ArchiveEntry entry) throws IOException {
		if (!this.entryPath.equals(entry.getEntryPath())) {
			throw new IOException("Metadata path differs from writer path: " + entry.getEntryPath());
		}

		final EntryType type = entry.getEntryType();
		if (type != EntryType.FILE && type != EntryType.DIRECTORY && type != EntryType.SYMBOLIC_LINK) {
			throw new IOException("Unsupported ZIP entry metadata type: " + type);
		}

		if ((type == EntryType.DIRECTORY) != this.isDirectoryEntry()) {
			throw new IOException("Only directory entry names must end with '/'");
		}

		return ArchiveEntry.entryTypeCode(type);
	}

	/**
	 * <h3 class="en-US">Check weather current entry type is directory</h3>
	 * <h3 class="zh-CN">判断当前条目类型为文件夹</h3>
	 *
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	private boolean isDirectoryEntry() {
		return this.entryPath.endsWith("/");
	}
}
