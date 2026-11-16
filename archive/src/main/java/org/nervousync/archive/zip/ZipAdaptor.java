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

package org.nervousync.archive.zip;

import jakarta.annotation.Nonnull;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.adaptor.ArchiveAdaptor;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.DataFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.enumerations.*;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.io.bzip2.BZip2OutputStream;
import org.nervousync.archive.io.deflate.DeflateOutputStream;
import org.nervousync.archive.io.zip.BoundedInputStream;
import org.nervousync.archive.io.zip.ZipArchiveInputStream;
import org.nervousync.archive.io.zip.ZipArchiveOutputStream;
import org.nervousync.archive.options.ArchiveLimit;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.security.StrongKey;
import org.nervousync.archive.security.password.PasswordCredentials;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.security.password.PasswordRequest;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.crypto.strong.StrongOutputStream;
import org.nervousync.archive.zip.models.EntryMetaData;
import org.nervousync.archive.zip.models.ExtraDataRecord;
import org.nervousync.archive.zip.models.central.CentralDirectory;
import org.nervousync.archive.zip.models.central.EndCentralDirectoryRecord;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.models.crypto.EncryptionInfo;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.archive.zip.models.header.FileHeader;
import org.nervousync.archive.zip.models.header.GeneralFileHeader;
import org.nervousync.archive.zip.models.header.LocalFileHeader;
import org.nervousync.archive.zip.models.header.StrongDecryptionHeader;
import org.nervousync.archive.zip.operator.ZipFileOperator;
import org.nervousync.archive.zip.resource.ZipEntryResourceReader;
import org.nervousync.archive.zip.resource.ZipEntryResourceWriter;
import org.nervousync.commons.Globals;
import org.nervousync.exceptions.builder.BuilderException;
import org.nervousync.utils.commons.FileUtils;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.commons.StringUtils;
import org.nervousync.utils.commons.SystemUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.time.Instant;
import java.util.*;
import java.util.zip.CRC32;

/**
 * <h2 class="en-US">ZIP adaptor</h2>
 * <h2 class="zh-CN">ZIP 适配器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ZipAdaptor extends ArchiveAdaptor {

	private static final Provider BC_PROVIDER = new BouncyCastleProvider();

	/**
	 * <span class="en-US">Data file</span>
	 * <span class="zh-CN">数据文件</span>
	 */
	private final DataFile dataFile;
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
	 * <span class="en-US">Central directory</span>
	 * <span class="zh-CN">中央仓库</span>
	 */
	private CentralDirectory centralDirectory;
	/**
	 * <span class="en-US">End of central directory record</span>
	 * <span class="zh-CN">中央目录结束记录</span>
	 */
	private EndCentralDirectoryRecord endOfCentralDirectory;
	/**
	 * <span class="en-US">Zip64 end of central directory</span>
	 * <span class="zh-CN">Zip64 中央目录结束</span>
	 */
	private Zip64EndCentralDirectory zip64EndCentralDirectory = null;
	/**
	 * <span class="en-US">Output temporary directory</span>
	 * <span class="zh-CN">输出临时目录</span>
	 */
	private Path tempDirectory = null;
	/**
	 * <span class="en-US">Zip file operator</span>
	 * <span class="zh-CN">Zip 文件操作器</span>
	 */
	private final ZipFileOperator operator;
	/**
	 * <span class="en-US">Registered entry resource writer</span>
	 * <span class="zh-CN">注册的条目写入器</span>
	 */
	private final Hashtable<String, ZipEntryResourceWriter> registerWriters = new Hashtable<>();
	/**
	 * <span class="en-US">Add or modified entry information mapping table</span>
	 * <span class="zh-CN">新增/修改条目映射表</span>
	 */
	private final Hashtable<String, LocalFileHeader> newEntries = new Hashtable<>();
	/**
	 * <span class="en-US">Drop entries path list</span>
	 * <span class="zh-CN">删除条目路径列表</span>
	 */
	private final List<String> removeEntries = new ArrayList<>();
	/**
	 * <span class="en-US">Session state</span>
	 * <span class="zh-CN">事务状态</span>
	 */
	private SessionState state = SessionState.ACTIVE;
	/**
	 * <span class="en-US">Keep workspace existed when publish failed</span>
	 * <span class="zh-CN">发布失败时保留目录</span>
	 */
	private boolean preserveWorkspace = false;

	/**
	 * <h3 class="en-US">Constructor method for the ZIP adaptor</h3>
	 * <h3 class="zh-CN">ZIP 适配器的构造方法</h3>
	 *
	 * @param dataFile   <span class="en-US">Data file</span>
	 *                   <span class="zh-CN">数据文件</span>
	 * @param options    <span class="en-US">Archive options</span>
	 *                   <span class="zh-CN">归档选项</span>
	 * @param zipOptions <span class="en-US">ZIP options</span>
	 *                   <span class="zh-CN">ZIP 选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when open archive file</span>
	 *                     <span class="zh-CN">打开归档文件出错</span>
	 */
	public ZipAdaptor(final DataFile dataFile, final ArchiveOptions options, final ZipOptions zipOptions)
			throws IOException {
		super(dataFile.filePath(), dataFile.openMode());
		this.dataFile = dataFile;
		if (this.openMode() != OpenMode.CREATE) {
			this.operator = new ZipFileOperator(dataFile, options, zipOptions);
			try {
				ArchiveLimit limit = options.getArchiveLimit();
				if (limit.getMaxCompressedBytes() < this.operator.size()) {
					throw new ArchiveException(0x0000001A0033L, this.operator.size(),
							options.getArchiveLimit().getMaxCompressedBytes());
				}
				this.endOfCentralDirectory = this.operator.readEndCentralDirectoryRecord();
				this.zip64EndCentralDirectory = this.operator.readZip64EndCentralDirectory(this.endOfCentralDirectory);
				this.centralDirectory =
						this.operator.readCentralDirectory(this.endOfCentralDirectory, this.zip64EndCentralDirectory);
				if (this.centralDirectory.getFileHeaders().size() > limit.getMaxEntries()) {
					throw new ArchiveException(0x0000001A003EL, limit.getMaxEntries(),
							this.centralDirectory.getFileHeaders().size());
				}
				long totalSize = 0L;
				for (GeneralFileHeader fileHeader : this.centralDirectory.getFileHeaders()) {
					String entryPath = fileHeader.getEntryPath();
					if (StringUtils.isEmpty(entryPath)) {
						throw new ArchiveException(0x0000001A0001L);
					}

					int nameCharacters = entryPath.codePointCount(0, entryPath.length());
					if (nameCharacters > limit.getMaxNameCharacters()) {
						throw new ArchiveException(0x0000001A003EL, limit.getMaxNameCharacters(), nameCharacters);
					}

					int nestingDepth = nestingDepth(entryPath);
					if (nestingDepth > limit.getMaxNestingDepth()) {
						throw new ArchiveException(0x0000001A003EL, limit.getMaxNestingDepth(), nestingDepth);
					}

					if (fileHeader.getUncompressedSize() > limit.getMaxEntryBytes()) {
						throw new ArchiveException(0x0000001A003EL, limit.getMaxEntryBytes(),
								fileHeader.getUncompressedSize());
					}

					try {
						totalSize = Math.addExact(totalSize, fileHeader.getUncompressedSize());
					} catch (ArithmeticException e) {
						throw new ArchiveException(0x0000001A003EL, e);
					}
					if (totalSize > limit.getMaxTotalBytes()) {
						throw new ArchiveException(0x0000001A003EL, limit.getMaxTotalBytes(), totalSize);
					}

					if (fileHeader.getUncompressedSize() > 0L) {
						if (fileHeader.getCompressedSize() == 0L) {
							throw new ArchiveException(0x0000001A0011L, entryPath);
						}

						double ratio = (double) fileHeader.getUncompressedSize() / (double) fileHeader.getCompressedSize();
						if (ratio > limit.getMaxCompressionRatio()) {
							throw new ArchiveException(0x0000001A003EL, limit.getMaxCompressionRatio(), ratio);
						}
					}
				}
			} catch (IOException | RuntimeException e) {
				try {
					this.operator.close();
				} catch (Exception ex) {
					e.addSuppressed(ex);
				}
				throw e;
			}
		} else {
			this.operator = new ZipFileOperator(dataFile, options, zipOptions);
			this.centralDirectory = new CentralDirectory(zipOptions.getDuplicatePolicy());
			this.endOfCentralDirectory = new EndCentralDirectoryRecord();
			if (Zip64Mode.ALWAYS == zipOptions.getZip64Mode()) {
				this.zip64EndCentralDirectory = new Zip64EndCentralDirectory();
			}
		}
		if (this.openMode() == OpenMode.EDIT || this.openMode() == OpenMode.CREATE) {
			if (this.tempDirectory == null) {
				Path current = Path.of(dataFile.filePath()).toAbsolutePath().normalize();
				this.tempDirectory = Files.createTempDirectory(current.getParent(), ".nervousync-zip-");
			}
		}
		this.options = Objects.requireNonNull(options, "options");
		this.zipOptions = Objects.requireNonNull(zipOptions, "zipOptions");
		this.entries.clear();
		for (GeneralFileHeader fileHeader : this.centralDirectory.getFileHeaders()) {
			try {
				this.registerEntry(ArchiveEntry.describe(fileHeader, this.zipOptions.getTimeZone(), false));
			} catch (BuilderException | NullPointerException e) {
				throw new ArchiveException(0x0000001A001AL, e, this.filePath);
			}
		}
	}

	@Override
	public void merge(@Nonnull final Path target) throws IOException {
		if (this.openMode() == OpenMode.CREATE) {
			throw new ArchiveException(0x0000001A0045L);
		}
		if (!this.operator.splitArchive()) {
			throw new ArchiveException(0x0000001A004BL);
		}

		ZipOptions zipOptions = this.mergeOptions();
		String targetPath = target.toAbsolutePath().toString();
		Path current = target.toAbsolutePath().normalize();
		Path tempDirectory = Files.createTempDirectory(current.getParent(), ".nervousync-zip-");
		Path outputDirectory = Files.createTempDirectory(tempDirectory, "output-");
		boolean preserveRecovery = false, finished = false, dropOnFailed = !FileUtils.isExists(targetPath);

		try {
			Map<Path, Path> publishFiles;
			try (DataFile targetFile = this.dataFile.newFile(targetPath, "rw");
			     ZipArchiveOutputStream outputStream = new ZipArchiveOutputStream(outputDirectory, targetFile, zipOptions)) {
				Queue<GeneralFileHeader> headerQueue = new ArrayDeque<>();
				for (GeneralFileHeader generalFileHeader : this.centralDirectory.getFileHeaders()) {
					LocalFileHeader localFileHeader =
							this.operator.readLocalFileHeader(generalFileHeader, this.options.getCharset(),
									this.options.getArchiveLimit().getMaxHeaderBytes());
					GeneralFileHeader generalHeader = generalFileHeader.duplicate();
					LocalFileHeader localHeader = localFileHeader.duplicate();
					this.copyData(outputStream, generalHeader, localHeader);
					headerQueue.add(generalHeader);
				}
				this.processCentralDirectory(outputStream, headerQueue, zipOptions, false);
				outputStream.finish();
				publishFiles = outputStream.publishFiles();
			}

			List<Path> originalVolumes = new ArrayList<>();
			if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
				originalVolumes.add(target);
			}
			try {
				publishFiles(publishFiles, originalVolumes, outputDirectory.resolve("backup"));
			} catch (IOException | RuntimeException | Error e) {
				preserveRecovery = true;
				throw e;
			}
			finished = true;
		} finally {
			if (!preserveRecovery) {
				this.deleteWorkspace(tempDirectory, current.getParent());
			}
			if (!finished && dropOnFailed) {
				Files.deleteIfExists(target);
			}
		}
	}

	@Override
	public Optional<ResourceReader> openReader(final ArchiveEntry archiveEntry) throws IOException {
		Objects.requireNonNull(archiveEntry, "archiveEntry");
		GeneralFileHeader generalFileHeader = this.centralDirectory.header(archiveEntry.getEntryPath());
		if (generalFileHeader == null) {
			return Optional.empty();
		}
		if (EntryType.DIRECTORY == generalFileHeader.entryType()) {
			return Optional.empty();
		}

		return Optional.of(this.openReader(generalFileHeader));
	}

	@Override
	public Optional<ResourceWriter> openWriter(final String entryPath, final String comment) throws IOException {
		if (OpenMode.READ == this.openMode()) {
			throw new ArchiveException(0x0000001A0002L);
		}
		this.ensureActive();
		String path = this.checkedEntryPath(entryPath);
		ZipEntryResourceWriter writer = this.registerWriters.get(path);
		if (writer == null) {
			writer = new ZipEntryResourceWriter(this, this.tempDirectory, this.options,
					this.zipOptions, path, comment);
			this.registerWriters.put(path, writer);
		}
		return Optional.of(writer);
	}

	/**
	 * <h3 class="en-US">Check the given entry path is valid and return formatted entry path</h3>
	 * <h3 class="zh-CN">检查条目路径并返回符合规则的条目路径</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @return <span class="en-US">Formatted entry path</span>
	 * <span class="zh-CN">符合规则的条目路径</span>
	 * @throws IOException <span class="en-US">Unsafe entry path</span>
	 *                     <span class="zh-CN">不安全的条目路径</span>
	 */
	private String checkedEntryPath(final String entryPath) throws IOException {
		if (entryPath == null || StringUtils.isEmpty(entryPath.trim())) {
			throw new ArchiveException(0x0000001A0001L);
		}
		String path = StringUtils.replace(entryPath.trim(), "\\", ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR);
		if (path.indexOf('\0') >= 0 || path.startsWith("/")) {
			throw new ArchiveException(0x0000001A0034L, entryPath);
		}
		if (path.length() >= 2) {
			char first = path.charAt(0);
			boolean driveLetter = (first >= 'A' && first <= 'Z') || (first >= 'a' && first <= 'z');
			if (driveLetter && path.charAt(1) == ':') {
				throw new ArchiveException(0x0000001A0034L, entryPath);
			}
		}
		for (String segment : path.split(ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR, -1)) {
			if ("..".equals(segment)) {
				throw new ArchiveException(0x0000001A001FL, entryPath);
			}
		}
		return path;
	}

	/**
	 * <h3 class="en-US">Extract to the target path</h3>
	 * <h3 class="zh-CN">释放到目标地址</h3>
	 *
	 * @param adaptor    <span class="en-US">Zip operator</span>
	 *                   <span class="zh-CN">Zip 适配器</span>
	 * @param path       <span class="en-US">The target path</span>
	 *                   <span class="zh-CN">目标地址</span>
	 * @param fileHeader <span class="en-US">File header</span>
	 *                   <span class="zh-CN">文件头</span>
	 * @param options    <span class="en-US">Extract options</span>
	 *                   <span class="zh-CN">解压选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when extract files</span>
	 *                     <span class="zh-CN">释放文件执行中出错</span>
	 */
	public static void extract(@Nonnull final ZipAdaptor adaptor, @Nonnull final Path path,
	                           @Nonnull final FileHeader fileHeader, @Nonnull final ExtractOptions options)
			throws IOException {
		Path root = path.toAbsolutePath().normalize();
		if (options.isCreateDirectories()) {
			Files.createDirectories(root);
		}
		String normalizedName = fileHeader.getEntryPath().replace('\\', '/');
		Path target;

		try {
			target = root.resolve(normalizedName).normalize();
		} catch (InvalidPathException e) {
			if (skipUnsafeEntry(options, 0x0000001A0036L, fileHeader.getEntryPath())) {
				return;
			}
			throw e;
		}

		if (!target.startsWith(root)) {
			if (options.getUnsafePolicy() == UnsafePolicy.SKIP) {
				return;
			}
			throw new ArchiveException(0x0000001A0034L, fileHeader.getEntryPath());
		}

		try {
			switch (fileHeader.entryType()) {
				case DIRECTORY:
					if (adaptor.extractDirectory(target, fileHeader, options)) {
						EntryMetaData.applyBeforePublish(target, fileHeader, options, adaptor.options, adaptor.zipOptions);
						EntryMetaData.applyDosAttributes(target, fileHeader, options);
					}
					break;
				case FILE:
					try (ResourceReader reader = adaptor.openReader(fileHeader)) {
						reader.extract(target, options);
					} catch (Exception e) {
						throw (e instanceof IOException) ? (IOException) e : new IOException(e);
					}
					break;
				case SYMBOLIC_LINK:
					if (options.isAllowSymbolicLinks()) {
						try (ResourceReader reader = adaptor.openReader(fileHeader)) {
							adaptor.createLink(target, root, reader, fileHeader, options);
						} catch (Exception e) {
							throw (e instanceof IOException) ? (IOException) e : new IOException(e);
						}
					} else {
						if (skipUnsafeEntry(options, 0x0000001A0038L, fileHeader.getEntryPath())) {
							return;
						}
					}
					break;
				case HARD_LINK:
					if (options.isAllowHardLinks()) {
						adaptor.createLink(target, root, null, fileHeader, options);
					} else {
						if (skipUnsafeEntry(options, 0x0000001A0038L, fileHeader.getEntryPath())) {
							return;
						}
					}
					break;
				default:
					throw new ArchiveException(0x0000001A0039L, fileHeader.getEntryPath());
			}
		} catch (UnsafeEntryException e) {
			throw e;
		} catch (IOException e) {
			if (!options.isContinueOnError()) {
				throw e;
			}
		}
	}

	@Override
	public void extract(final Path path, final ExtractOptions options) throws IOException {
		if (this.openMode() == OpenMode.CREATE) {
			throw new ArchiveException(0x0000001A0045L);
		}
		Path root = path.toAbsolutePath().normalize();
		if (options.isCreateDirectories()) {
			Files.createDirectories(root);
		}

		TreeMap<Path, GeneralFileHeader> pendingDirectories =
				new TreeMap<>(Comparator.comparingInt(Path::getNameCount).reversed().thenComparing(Path::toString));
		LinkedHashMap<Path, GeneralFileHeader> pendingEntries = new LinkedHashMap<>();
		for (GeneralFileHeader fileHeader : this.centralDirectory.entryHeaders()) {
			String normalizedName = fileHeader.getEntryPath().replace('\\', '/');
			Path target;

			try {
				target = root.resolve(normalizedName).normalize();
			} catch (InvalidPathException e) {
				if (skipUnsafeEntry(options, 0x0000001A0036L, fileHeader.getEntryPath())) {
					continue;
				}
				throw e;
			}

			if (!target.startsWith(root)) {
				if (options.getUnsafePolicy() == UnsafePolicy.SKIP) {
					continue;
				}
				throw new ArchiveException(0x0000001A0034L, fileHeader.getEntryPath());
			}

			try {
				ArchiveFile.validateParentComponents(root, target);
			} catch (IOException e) {
				if (options.getUnsafePolicy() == UnsafePolicy.SKIP) {
					continue;
				}
				throw e;
			}

			try {
				switch (fileHeader.entryType()) {
					case DIRECTORY:
						if (this.extractDirectory(target, fileHeader, options)) {
							pendingDirectories.put(target, fileHeader);
						}
						break;
					case FILE:
						try (ResourceReader reader = this.openReader(fileHeader)) {
							reader.extract(target, options);
						} catch (Exception e) {
							throw (e instanceof IOException) ? (IOException) e : new IOException(e);
						}
						break;
					case SYMBOLIC_LINK:
						if (!options.isAllowSymbolicLinks()) {
							if (skipUnsafeEntry(options, 0x0000001A0038L, fileHeader.getEntryPath())) {
								continue;
							}
						}

						try (ResourceReader reader = this.openReader(fileHeader)) {
							this.createLink(target, root, reader, fileHeader, options);
						} catch (Exception e) {
							throw (e instanceof IOException) ? (IOException) e : new IOException(e);
						}
						break;
					case HARD_LINK:
						if (!options.isAllowHardLinks()) {
							skipUnsafeEntry(options, 0x0000001A0038L, fileHeader.getEntryPath());
							continue;
						}
						pendingEntries.put(target, fileHeader);
						break;
					default:
						throw new ArchiveException(0x0000001A0039L, fileHeader.getEntryPath());
				}
			} catch (UnsafeEntryException e) {
				throw e;
			} catch (IOException e) {
				if (!options.isContinueOnError()) {
					throw e;
				}
			}
		}

		this.extractPendingHardLinks(root, pendingEntries, options);

		for (Map.Entry<Path, GeneralFileHeader> entry : pendingDirectories.entrySet()) {
			try {
				EntryMetaData.applyBeforePublish(entry.getKey(), entry.getValue(), options, this.options, this.zipOptions);
				EntryMetaData.applyDosAttributes(entry.getKey(), entry.getValue(), options);
			} catch (IOException e) {
				if (!options.isContinueOnError()) {
					throw e;
				}
			}
		}
	}

	@Override
	public void remove(final ArchiveEntry archiveEntry) throws IOException {
		if (OpenMode.EDIT != this.openMode()) {
			throw new ArchiveException(0x0000001A0003L);
		}
		this.ensureActive();
		if (!this.removeEntries.contains(archiveEntry.getEntryPath())) {
			this.removeEntries.add(archiveEntry.getEntryPath());
			if (archiveEntry.getEntryType() == EntryType.DIRECTORY) {
				for (String entryPath : this.entries.keySet()) {
					if (entryPath.startsWith(archiveEntry.getEntryPath()) && !this.removeEntries.contains(entryPath)) {
						this.removeEntries.add(entryPath);
					}
				}
			}
		}
	}

	@Override
	public void commit() throws IOException {
		if (this.state == SessionState.COMMITTED) {
			return;
		}

		if (OpenMode.READ == this.openMode()) {
			throw new ArchiveException(0x0000001A0002L);
		}

		if (this.state != SessionState.ACTIVE) {
			throw new ArchiveException(0x0000001A0049L, SessionState.ACTIVE, this.state);
		}

		if (!this.registerWriters.isEmpty()) {
			throw new ArchiveException(0x0000001A004AL);
		}

		if (this.openMode != OpenMode.CREATE && this.newEntries.isEmpty() && this.removeEntries.isEmpty()) {
			this.state = SessionState.COMMITTED;
			return;
		}

		this.state = SessionState.COMMITING;
		try {
			Path outputDirectory = Files.createTempDirectory(this.tempDirectory, "output-");

			Map<Path, Path> publishFiles;
			try (ZipArchiveOutputStream outputStream =
					     new ZipArchiveOutputStream(outputDirectory, this.dataFile, this.zipOptions)) {
				this.processArchive(outputStream);
				outputStream.finish();
				publishFiles = outputStream.publishFiles();
			}

			List<Path> originalVolumes = this.operator.existVolumes();
			this.operator.close();

			this.preserveWorkspace = true;
			publishFiles(publishFiles, originalVolumes, outputDirectory.resolve("backup"));
			this.state = SessionState.COMMITTED;
			this.preserveWorkspace = false;

			this.newEntries.clear();
			this.removeEntries.clear();
		} catch (IOException | RuntimeException e) {
			this.state = SessionState.FAILED;
			throw e;
		}
	}

	@Override
	public void rollback() throws IOException {
		if (OpenMode.READ == this.openMode()) {
			throw new ArchiveException(0x0000001A0002L);
		}
		if (this.state == SessionState.COMMITTED) {
			throw new IOException("Archive has already been committed");
		}

		this.state = SessionState.ROLLED_BACK;
		this.closePendingWriters();
		this.newEntries.clear();
		this.removeEntries.clear();
		this.deleteWorkspace();
	}

	@Override
	public void close() throws IOException {
		if (this.state == SessionState.CLOSED) {
			return;
		}

		IOException failure = null;

		try {
			try {
				this.closePendingWriters();
			} catch (IOException | RuntimeException e) {
				failure = (e instanceof IOException) ? (IOException) e : new IOException(e);
			}

			try {
				this.operator.close();
				this.deleteUncommittedTarget();
			} catch (IOException | RuntimeException e) {
				if (failure == null) {
					failure = (e instanceof IOException) ? (IOException) e : new IOException(e);
				} else {
					failure.addSuppressed(e);
				}
			}

			try {
				this.deleteWorkspace();
			} catch (IOException | RuntimeException e) {
				if (failure == null) {
					failure = (e instanceof IOException) ? (IOException) e : new IOException(e);
				} else {
					failure.addSuppressed(e);
				}
			}
		} finally {
			this.registerWriters.clear();
			this.newEntries.clear();
			this.removeEntries.clear();
			this.entries.clear();

			this.centralDirectory = null;
			this.endOfCentralDirectory = null;
			this.zip64EndCentralDirectory = null;
			this.state = SessionState.CLOSED;
		}

		if (failure != null) {
			throw failure;
		}
	}

	/**
	 * <h3 class="en-US">Register modify or add new entry information</h3>
	 * <h3 class="zh-CN">注册修改/新增条目信息</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 */
	public void modifyEntry(final String entryPath) throws IOException {
		this.ensureActive();
		if (this.registerWriters.containsKey(entryPath)) {
			ZipEntryResourceWriter resourceWriter = this.registerWriters.remove(entryPath);
			this.newEntries.put(entryPath, resourceWriter.fileHeader());
			resourceWriter.close();
		}
	}

	/**
	 * <h3 class="en-US">Check whether the file header requires CRC verification.</h3>
	 * <h3 class="zh-CN">检查文件头是否要求核验 CRC</h3>
	 *
	 * @param fileHeader <span class="en-US">File header</span>
	 *                   <span class="zh-CN">文件头</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 */
	public static boolean verifyCRC(final FileHeader fileHeader) {
		EncryptionInfo encryptionInfo = fileHeader.getEncryptionInfo();

		if (encryptionInfo instanceof AesEncryptionInfo) {
			return ((AesEncryptionInfo) encryptionInfo).getVendor() == AesVendor.AE_1;
		}
		return true;
	}

	/**
	 * <h3 class="en-US">Safe path</h3>
	 * <h3 class="zh-CN">安全路径</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @return <span class="en-US">Safe path</span>
	 * <span class="zh-CN">安全路径</span>
	 */
	public static String safePath(final String entryPath) {
		String entryKey = StringUtils.replace(entryPath, "\\", ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR);
		if (StringUtils.isEmpty(entryKey)) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		return entryKey;
	}

	/**
	 * <h3 class="en-US">Move the temporary file to the target path</h3>
	 * <h3 class="zh-CN">移动临时文件到目标地址</h3>
	 *
	 * @param temporary <span class="en-US">The temporary path</span>
	 *                  <span class="zh-CN">临时文件地址</span>
	 * @param target    <span class="en-US">The target path</span>
	 *                  <span class="zh-CN">目标地址</span>
	 * @param options   <span class="en-US">Extract options</span>
	 *                  <span class="zh-CN">解压选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when move files</span>
	 *                     <span class="zh-CN">释放文件执行中出错</span>
	 */
	public static void temporaryToTarget(final Path temporary, final Path target, final ExtractOptions options)
			throws IOException {
		CopyOption[] copyOptions = (options.getOverrideMode() == OverrideMode.NEVER)
				? new CopyOption[]{StandardCopyOption.ATOMIC_MOVE}
				: new CopyOption[]{StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING};

		try {
			Files.move(temporary, target, copyOptions);
		} catch (AtomicMoveNotSupportedException e) {
			if (options.getOverrideMode() == OverrideMode.NEVER) {
				Files.move(temporary, target);
			} else {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 * <h3 class="en-US">Extract link to the target path</h3>
	 * <h3 class="zh-CN">释放连接到指定位置</h3>
	 *
	 * @param destination <span class="en-US">Destination</span>
	 *                    <span class="zh-CN">精确落点</span>
	 * @param header      <span class="en-US">File header</span>
	 *                    <span class="zh-CN">文件头</span>
	 * @param reader      <span class="en-US">Resource reader</span>
	 *                    <span class="zh-CN">资源读取器</span>
	 * @param options     <span class="en-US">Extract options</span>
	 *                    <span class="zh-CN">解压选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when extract link</span>
	 *                     <span class="zh-CN">释放文件执行中出错</span>
	 */
	public void extractLinkTo(final Path destination, final FileHeader header, final ResourceReader reader,
	                          final ExtractOptions options) throws IOException {
		Path target = destination.toAbsolutePath().normalize();
		Path root = target.getParent();
		if (root == null) {
			throw new ArchiveException(0x0000001A002BL);
		}

		EntryType type = header.entryType();
		if (type != EntryType.SYMBOLIC_LINK && type != EntryType.HARD_LINK) {
			throw new ArchiveException(0x0000001A0039L, header.getEntryPath());
		}
		if ((type == EntryType.SYMBOLIC_LINK && !options.isAllowSymbolicLinks())
				|| (type == EntryType.HARD_LINK && !options.isAllowHardLinks())) {
			skipUnsafeEntry(options, 0x0000001A0038L, header.getEntryPath());
			return;
		}

		try {
			if (!allowLinkLocation(target.getRoot(), target, header, options)) {
				return;
			}
			if (options.isCreateDirectories()) {
				Files.createDirectories(root);
			} else if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
				throw new ArchiveException(0x0000001A002CL);
			}

			this.createLink(target, root, reader, header, options);
		} catch (UnsafeEntryException e) {
			throw e;
		} catch (IOException e) {
			if (!options.isContinueOnError()) {
				throw e;
			}
		}
	}

	/**
	 * <h3 class="en-US">Generate zip options for merge split archive files</h3>
	 * <h3 class="zh-CN">生成合并分卷文件的 Zip 选项</h3>
	 *
	 * @return <span class="en-US">Zip options</span>
	 * <span class="zh-CN">Zip 选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when generate zip options</span>
	 *                     <span class="zh-CN">生成选项时出错</span>
	 */
	private ZipOptions mergeOptions() throws IOException {
		ZipOptions.Builder optionsBuilder =
				ZipOptions.newBuilder().splitArchive(false, Globals.DEFAULT_VALUE_LONG)
						.compressionAlgorithm(this.zipOptions.getAlgorithm())
						.compressionLevel(this.zipOptions.getCompressionLevel())
						.zip64Mode((this.zip64EndCentralDirectory == null)
								? this.zipOptions.getZip64Mode()
								: Zip64Mode.ALWAYS);

		if (this.zip64EndCentralDirectory != null && this.zip64EndCentralDirectory.getProtection() != null) {
			Zip64EndCentralDirectory.Protection protection = this.zip64EndCentralDirectory.getProtection();

			switch (protection.getCompressionMethod()) {
				case ZipGlobals.COMP_STORE:
					break;
				case ZipGlobals.COMP_DEFLATE:
					optionsBuilder.compressionAlgorithm(CompressionAlgorithm.DEFLATE).enableCentralDirectoryCompress();
					break;
				case ZipGlobals.COMP_DEFLATE64:
					optionsBuilder.compressionAlgorithm(CompressionAlgorithm.DEFLATE64).enableCentralDirectoryCompress();
					break;
				case ZipGlobals.COMP_BZIP2:
					optionsBuilder.compressionAlgorithm(CompressionAlgorithm.BZIP2).enableCentralDirectoryCompress();
					break;
				default:
					throw new ArchiveException(0x0000001B004AL);
			}

			if (protection.encrypted()) {
				StrongAccessMode mode = StrongAccessMode.from(protection.getFlags());

				if (mode.useCertificate()) {
					StrongDecryptionHeader sourceHeader = this.operator.getCentralDecryptionHeader();
					if (sourceHeader == null || sourceHeader.recipients() == null) {
						throw new IOException("Missing source recipient metadata");
					}
					StrongKey.verifyRecipients(sourceHeader.recipients(), this.zipOptions.getCentralDirectoryRecipients());
					optionsBuilder.centralDirectoryCrypto(protection.getAlgorithm(), protection.getKeyBitLength(),
							mode, this.zipOptions.getCentralDirectoryRecipients());
				} else {
					optionsBuilder.centralDirectoryCrypto(protection.getAlgorithm(), protection.getKeyBitLength(),
							StrongAccessMode.PASSWORD, List.of());
				}

				if (StrongKey.writeFlags(protection.getAlgorithm(), mode) != protection.getFlags()) {
					throw new IOException("Rewriting this source wrapping profile requires an explicit re-encryption policy");
				}
			}

			optionsBuilder.centralDirectoryHash(protection.getHashAlgorithm());
		}

		return optionsBuilder.build();
	}

	/**
	 * <h3 class="en-US">Delete uncommitted target file</h3>
	 * <h3 class="zh-CN">删除未提交的目标文件</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when delete files</span>
	 *                     <span class="zh-CN">删除文件出错</span>
	 */
	private void deleteUncommittedTarget() throws IOException {
		if (this.openMode() != OpenMode.CREATE || this.state == SessionState.COMMITTED || this.preserveWorkspace) {
			return;
		}

		Path target = Path.of(this.filePath).toAbsolutePath().normalize();
		if (Files.notExists(target, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.size(target) != 0L) {
			throw new IOException("Unexpected ZIP placeholder: " + target);
		}
		Files.deleteIfExists(target);
	}

	/**
	 * <h3 class="en-US">Ensure current adaptor state in ACTIVE</h3>
	 * <h3 class="zh-CN">确认处于 ACTIVE 状态</h3>
	 *
	 * @throws IOException <span class="en-US">State is not ACTIVE</span>
	 *                     <span class="zh-CN">状态错误</span>
	 */
	private void ensureActive() throws IOException {
		if (this.state != SessionState.ACTIVE) {
			throw new ArchiveException(0x0000001A0045L, SessionState.ACTIVE, this.state);
		}
	}

	/**
	 * <h3 class="en-US">Register entry</h3>
	 * <h3 class="zh-CN">注册条目</h3>
	 *
	 * @param archiveEntry <span class="en-US">Archive entry</span>
	 *                     <span class="zh-CN">归档条目</span>
	 * @throws IOException <span class="en-US">If an error occurs when register entry</span>
	 *                     <span class="zh-CN">注册过程中出错</span>
	 */
	private void registerEntry(final ArchiveEntry archiveEntry) throws IOException {
		String entryKey = StringUtils.replace(archiveEntry.getEntryPath(),
				"\\", ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR);
		if (StringUtils.isEmpty(entryKey)) {
			throw new ArchiveException(0x0000001A0001L);
		}
		if (this.entries.containsKey(entryKey)) {
			switch (this.zipOptions.getDuplicatePolicy()) {
				case FIRST:
					return;
				case LAST:
					this.entries.remove(entryKey);
					this.entries.put(entryKey, archiveEntry);
					break;
				case REJECT:
					throw new ArchiveException(0x0000001A003FL, archiveEntry.getEntryPath());
			}
		} else {
			this.entries.put(entryKey, archiveEntry);
		}
	}

	/**
	 * <h3 class="en-US">Calculate entry path nesting depth</h3>
	 * <h3 class="zh-CN">计算条目路径深度</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @return <span class="en-US">Calculate result</span>
	 * <span class="zh-CN">计算结果</span>
	 */
	private static int nestingDepth(final String entryPath) {
		String normalized = entryPath.replace('\\', '/');
		int count = 0;
		for (String component : normalized.split("/")) {
			if (component.isEmpty() || ".".equals(component)) {
				continue;
			}

			if ("..".equals(component)) {
				return Globals.DEFAULT_VALUE_INT;
			}
			count++;
		}
		return count;
	}

	/**
	 * <h3 class="en-US">Get the resource reader instance object by the given general file header</h3>
	 * <h3 class="zh-CN">获得指定通用文件头对应的资源读取器实例对象</h3>
	 *
	 * @param fileHeader <span class="en-US">General file header</span>
	 *                   <span class="zh-CN">通用文件头</span>
	 * @return <span class="en-US">The resource read instance object</span>
	 * <span class="zh-CN">资源读取器实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when initialize the resource read instance object</span>
	 *                     <span class="zh-CN">初始化资源读取器实例对象时出错</span>
	 */
	private ResourceReader openReader(final FileHeader fileHeader) throws IOException {
		LocalFileHeader localFileHeader;
		if (fileHeader instanceof GeneralFileHeader) {
			localFileHeader =
					this.operator.readLocalFileHeader((GeneralFileHeader) fileHeader, this.options.getCharset(),
							this.options.getArchiveLimit().getMaxHeaderBytes());
		} else {
			localFileHeader = (LocalFileHeader) fileHeader;
		}
		return new ZipEntryResourceReader(this.operator.inputStream(), this, this.options,
				this.zipOptions, localFileHeader, fileHeader);
	}

	/**
	 * <h3 class="en-US">Extract target directory</h3>
	 * <h3 class="zh-CN">创建指定目录</h3>
	 *
	 * @param target     <span class="en-US">The target path</span>
	 *                   <span class="zh-CN">目标地址</span>
	 * @param fileHeader <span class="en-US">General file header</span>
	 *                   <span class="zh-CN">通用文件头</span>
	 * @param options    <span class="en-US">Extract options</span>
	 *                   <span class="zh-CN">解压选项</span>
	 * @return <span class="en-US">Execute result</span>
	 * <span class="zh-CN">执行结果</span>
	 * @throws IOException <span class="en-US">If an error occurs when extract files</span>
	 *                     <span class="zh-CN">释放文件执行中出错</span>
	 */
	private boolean extractDirectory(final Path target, final FileHeader fileHeader, final ExtractOptions options)
			throws IOException {
		Instant lastModified = fileHeader.modifiedInstant(this.zipOptions.getTimeZone(), this.options.isStrict());

		if (ArchiveFile.existsTarget(target, options.getOverrideMode(), lastModified)) {
			return false;
		}

		if (!options.isCreateDirectories()) {
			return Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS);
		}

		if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
				&& !Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
			Files.delete(target);
		}

		Files.createDirectories(target);
		return true;
	}

	/**
	 * <h3 class="en-US">Read symbolic link target address</h3>
	 * <h3 class="zh-CN">读取软连接目标地址</h3>
	 *
	 * @param reader     <span class="en-US">Resource reader</span>
	 *                   <span class="zh-CN">资源读取器</span>
	 * @param fileHeader <span class="en-US">General file header</span>
	 *                   <span class="zh-CN">通用文件头</span>
	 * @return <span class="en-US">Symbolic link target address</span>
	 * <span class="zh-CN">软连接目标地址</span>
	 * @throws IOException <span class="en-US">If an error occurs when read symbolic link target address</span>
	 *                     <span class="zh-CN">读取软连接目标地址出错</span>
	 */
	private String targetText(@Nonnull ResourceReader reader, final FileHeader fileHeader) throws IOException {
		if (fileHeader.entryType() != EntryType.SYMBOLIC_LINK) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		long expectedLength = fileHeader.getUncompressedSize();
		CRC32 crc = new CRC32();

		try (InputStream inputStream = reader.openStream()) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] readBuffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
			long readLength = 0L;
			int readLen;
			while ((readLen = inputStream.read(readBuffer)) != -1) {
				if (readLen == 0) {
					continue;
				}
				try {
					readLength = Math.addExact(readLength, readLen);
				} catch (ArithmeticException e) {
					throw new ArchiveException(0x0000001A002FL);
				}
				if (readLength > expectedLength) {
					throw new ArchiveException(0x0000001A002FL);
				}
				crc.update(readBuffer, 0, readLen);
				outputStream.write(readBuffer, 0, readLen);
			}

			if (readLength != expectedLength) {
				throw new ArchiveException(0x0000001A002FL);
			}

			if (this.options.isVerify() && verifyCRC(fileHeader) && crc.getValue() != fileHeader.getCrc32()) {
				throw new ArchiveException(0x0000001A0032L);
			}
			return outputStream.toString(options.getCharset());
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * <h3 class="en-US">Create link</h3>
	 * <h3 class="zh-CN">创建连接</h3>
	 *
	 * @param target         <span class="en-US">The target path</span>
	 *                       <span class="zh-CN">目标地址</span>
	 * @param root           <span class="en-US">The root path</span>
	 *                       <span class="zh-CN">根路径</span>
	 * @param fileHeader     <span class="en-US">General file header</span>
	 *                       <span class="zh-CN">通用文件头</span>
	 * @param extractOptions <span class="en-US">Extract options</span>
	 *                       <span class="zh-CN">解压选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when create link</span>
	 *                     <span class="zh-CN">创建连接出错</span>
	 */
	private void createLink(final Path target, final Path root, final ResourceReader reader,
	                        final FileHeader fileHeader, final ExtractOptions extractOptions) throws IOException {
		if (fileHeader.entryType() == EntryType.HARD_LINK) {
			Path source = hardLinkSource(root, target, fileHeader, extractOptions);
			if (source != null) {
				this.createHardLink(root, target, source, fileHeader, extractOptions);
			}
			return;
		}

		Path linkText = readRelativeLinkPath(this.targetText(reader, fileHeader), fileHeader, extractOptions);
		if (linkText != null && prepareLinkParent(root, target, fileHeader, extractOptions)) {
			Path parent = target.getParent();
			Path resolved = resolveSymbolicLinkTarget(root, parent, linkText);
			if (resolved == null) {
				skipUnsafeEntry(extractOptions, 0x0000001A0037L, fileHeader.getEntryPath());
				return;
			}

			Path relative = parent.toRealPath().relativize(resolved);
			this.publishLink(root, target,
					relative.toString().isEmpty() ? Path.of(".") : relative,
					true, fileHeader, extractOptions);
		}
	}

	/**
	 * <h3 class="en-US">Read relative link path</h3>
	 * <h3 class="zh-CN">读取真实连接路径</h3>
	 *
	 * @param text    <span class="en-US">Target path text</span>
	 *                <span class="zh-CN">目标地址字符串</span>
	 * @param header  <span class="en-US">General file header</span>
	 *                <span class="zh-CN">通用文件头</span>
	 * @param options <span class="en-US">Extract options</span>
	 *                <span class="zh-CN">解压选项</span>
	 * @return <span class="en-US">Relative link path</span>
	 * <span class="zh-CN">真实连接路径</span>
	 * @throws IOException <span class="en-US">If an error occurs when read relative link path</span>
	 *                     <span class="zh-CN">执行过程中出错</span>
	 */
	private static Path readRelativeLinkPath(final String text, final FileHeader header, final ExtractOptions options)
			throws IOException {
		if (text == null || text.isEmpty()) {
			skipUnsafeEntry(options, 0x0000001A0036L, header.getEntryPath());
			return null;
		}

		Path path;
		try {
			path = Paths.get(text);
		} catch (InvalidPathException e) {
			skipUnsafeEntry(options, 0x0000001A0036L, header.getEntryPath());
			return null;
		}
		if (path.getRoot() != null) {
			skipUnsafeEntry(options, 0x0000001A0035L, header.getEntryPath());
			return null;
		}
		return path;
	}

	/**
	 * <h3 class="en-US">Check weather allow link location</h3>
	 * <h3 class="zh-CN">检查是否允许连接地址</h3>
	 *
	 * @param root    <span class="en-US">The root path</span>
	 *                <span class="zh-CN">根路径</span>
	 * @param target  <span class="en-US">The target path</span>
	 *                <span class="zh-CN">目标路径</span>
	 * @param header  <span class="en-US">General file header</span>
	 *                <span class="zh-CN">通用文件头</span>
	 * @param options <span class="en-US">Extract options</span>
	 *                <span class="zh-CN">解压选项</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 * @throws IOException <span class="en-US">If an error occurs when check allow link location</span>
	 *                     <span class="zh-CN">执行过程中出错</span>
	 */
	private static boolean allowLinkLocation(final Path root, final Path target, final FileHeader header,
	                                         final ExtractOptions options) throws IOException {
		if (target.equals(root) || !target.startsWith(root)) {
			return !skipUnsafeEntry(options, 0x0000001A0034L, header.getEntryPath());
		}
		try {
			ArchiveFile.validateParentComponents(root, target);
		} catch (ArchiveException e) {
			return !skipUnsafeEntry(options, e.getErrorCode(), header.getEntryPath());
		}

		Path realRoot = root.toRealPath();
		Path current = root;
		for (Path component : root.relativize(target.getParent())) {
			current = current.resolve(component);
			try {
				if (!current.toRealPath().equals(realRoot.resolve(root.relativize(current)))) {
					return !skipUnsafeEntry(options, 0x0000001A003DL, header.getEntryPath());
				}
			} catch (NoSuchFileException e) {
				break;
			}
		}
		return true;
	}

	/**
	 * <h3 class="en-US">Prepare link parent</h3>
	 * <h3 class="zh-CN">准备链接父目录</h3>
	 *
	 * @param root    <span class="en-US">The root path</span>
	 *                <span class="zh-CN">根路径</span>
	 * @param target  <span class="en-US">The target path</span>
	 *                <span class="zh-CN">目标路径</span>
	 * @param header  <span class="en-US">General file header</span>
	 *                <span class="zh-CN">通用文件头</span>
	 * @param options <span class="en-US">Extract options</span>
	 *                <span class="zh-CN">解压选项</span>
	 * @return <span class="en-US">Prepare result</span>
	 * <span class="zh-CN">准备结果</span>
	 * @throws IOException <span class="en-US">If an error occurs when prepare link parent</span>
	 *                     <span class="zh-CN">执行过程中出错</span>
	 */
	private static boolean prepareLinkParent(final Path root, final Path target, final FileHeader header,
	                                         final ExtractOptions options) throws IOException {
		if (!allowLinkLocation(root, target, header, options)) {
			return false;
		}
		Path parent = target.getParent();
		if (options.isCreateDirectories()) {
			Files.createDirectories(parent);
		} else if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
			throw new ArchiveException(0x0000001A002CL);
		}
		return allowLinkLocation(root, target, header, options);
	}

	/**
	 * <h3 class="en-US">Skip unsafe entry</h3>
	 * <h3 class="zh-CN">跳过不安全条目</h3>
	 *
	 * @param options   <span class="en-US">Extract options</span>
	 *                  <span class="zh-CN">解压选项</span>
	 * @param errorCode <span class="en-US">Error code</span>
	 *                  <span class="zh-CN">错误代码</span>
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @return <span class="en-US">Skip flag</span>
	 * <span class="zh-CN">跳过标记</span>
	 * @throws IOException <span class="en-US">Throws the exception</span>
	 *                     <span class="zh-CN">抛出异常</span>
	 */
	private static boolean skipUnsafeEntry(final ExtractOptions options, final long errorCode, final String entryPath)
			throws IOException {
		if (options.getUnsafePolicy() == UnsafePolicy.SKIP) {
			return true;
		}
		throw new UnsafeEntryException(errorCode, entryPath);
	}

	/**
	 * <h3 class="en-US">Verify that the target addresses of the symbolic links also fall within the actual root directory of the extracted files.</h3>
	 * <h3 class="zh-CN">检查软连接目标地址也在真实解压根目录中</h3>
	 *
	 * @param root       <span class="en-US">The root path</span>
	 *                   <span class="zh-CN">根路径</span>
	 * @param parent     <span class="en-US">The parent path</span>
	 *                   <span class="zh-CN">父路径</span>
	 * @param targetPath <span class="en-US">The target path</span>
	 *                   <span class="zh-CN">目标路径</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 * @throws IOException <span class="en-US">If an error occurs when convert Path to real path</span>
	 *                     <span class="zh-CN">读取真实路径时出错</span>
	 */
	private static Path resolveSymbolicLinkTarget(final Path root, final Path parent, final Path targetPath)
			throws IOException {
		Path realRoot = root.toRealPath();
		Path current = parent.toRealPath();
		if (!current.startsWith(realRoot)) {
			return null;
		}

		boolean missing = false;
		for (int i = 0; i < targetPath.getNameCount(); i++) {
			Path component = targetPath.getName(i);
			String name = component.toString();

			if (".".equals(name)) {
				continue;
			}
			if ("..".equals(name)) {
				if (missing || current.equals(realRoot)) {
					return null;
				}
				current = current.getParent();
				continue;
			}

			current = current.resolve(component);
			if (missing) {
				continue;
			}
			try {
				Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			} catch (NoSuchFileException e) {
				missing = true;
				continue;
			}

			try {
				current = current.toRealPath();
			} catch (NoSuchFileException | FileSystemLoopException e) {
				return null;
			}
			if (!current.startsWith(realRoot)) {
				return null;
			}
			if (i + 1 < targetPath.getNameCount() && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
				return null;
			}
		}
		return current;
	}

	/**
	 * <h3 class="en-US">Hard link source path</h3>
	 * <h3 class="zh-CN">硬连接路径</h3>
	 *
	 * @param root    <span class="en-US">The root path</span>
	 *                <span class="zh-CN">根路径</span>
	 * @param target  <span class="en-US">The target path</span>
	 *                <span class="zh-CN">目标路径</span>
	 * @param header  <span class="en-US">File header</span>
	 *                <span class="zh-CN">文件头</span>
	 * @param options <span class="en-US">Extract options</span>
	 *                <span class="zh-CN">解压选项</span>
	 * @return <span class="en-US">Hard link source path</span>
	 * <span class="zh-CN">硬连接路径</span>
	 * @throws IOException <span class="en-US">If an error occurs when get hard link source path</span>
	 *                     <span class="zh-CN">读取硬连接路径时出错</span>
	 */
	private Path hardLinkSource(final Path root, final Path target, final FileHeader header,
	                            final ExtractOptions options) throws IOException {
		Path relative = readRelativeLinkPath(header.hardLinkTarget(this.options.getCharset()), header, options);
		if (relative == null) {
			return null;
		}

		Path source = target.getParent().resolve(relative).normalize();
		if (source.equals(root) || !source.startsWith(root)) {
			skipUnsafeEntry(options, 0x0000001A0037L, header.getEntryPath());
			return null;
		}
		return source;
	}

	/**
	 * <h3 class="en-US">Create hard link</h3>
	 * <h3 class="zh-CN">创建硬连接</h3>
	 *
	 * @param root    <span class="en-US">The root path</span>
	 *                <span class="zh-CN">根路径</span>
	 * @param target  <span class="en-US">The target path</span>
	 *                <span class="zh-CN">目标路径</span>
	 * @param source  <span class="en-US">Hard link</span>
	 *                <span class="zh-CN">硬连接</span>
	 * @param header  <span class="en-US">File header</span>
	 *                <span class="zh-CN">文件头</span>
	 * @param options <span class="en-US">Extract options</span>
	 *                <span class="zh-CN">解压选项</span>
	 * @return <span class="en-US">Create result</span>
	 * <span class="zh-CN">操作结果</span>
	 * @throws IOException <span class="en-US">If an error occurs when create hard link</span>
	 *                     <span class="zh-CN">创建硬连接时出错</span>
	 */
	private boolean createHardLink(final Path root, final Path target, final Path source, final FileHeader header,
	                               final ExtractOptions options) throws IOException {
		if (!allowLinkLocation(root, source, header, options) || !prepareLinkParent(root, target, header, options)) {
			return false;
		}
		if (ArchiveFile.existsTarget(target, options.getOverrideMode(),
				header.modifiedInstant(this.zipOptions.getTimeZone(), this.options.isStrict()))) {
			return Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS);
		}
		BasicFileAttributes attrs = Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		if (attrs.isSymbolicLink()) {
			return !skipUnsafeEntry(options, 0x0000001A0036L, header.getEntryPath());
		}
		if (!attrs.isRegularFile()) {
			throw new IOException("Hard-link source is not a regular file: " + source);
		}
		return this.publishLink(root, target, source, false, header, options);
	}

	/**
	 * <h3 class="en-US">Extract pending hard links</h3>
	 * <h3 class="zh-CN">释放滞后的硬连接</h3>
	 *
	 * @param root    <span class="en-US">The root path</span>
	 *                <span class="zh-CN">根路径</span>
	 * @param pending <span class="en-US">Pending hard link information</span>
	 *                <span class="zh-CN">滞后释放的硬连接信息</span>
	 * @param options <span class="en-US">Extract options</span>
	 *                <span class="zh-CN">解压选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when create hard link</span>
	 *                     <span class="zh-CN">创建硬连接时出错</span>
	 */
	private void extractPendingHardLinks(final Path root, final Map<Path, GeneralFileHeader> pending,
	                                     final ExtractOptions options) throws IOException {
		Map<Path, Path> unresolved = new HashMap<>();
		Map<Path, Boolean> available = new HashMap<>();

		for (Map.Entry<Path, GeneralFileHeader> entry : pending.entrySet()) {
			Path target = entry.getKey();
			GeneralFileHeader header = entry.getValue();

			try {
				Path source = this.hardLinkSource(root, target, entry.getValue(), options);
				if (source == null
						|| !allowLinkLocation(root, source, header, options)
						|| !allowLinkLocation(root, target, header, options)) {
					available.put(target, false);
					continue;
				}
				if (ArchiveFile.existsTarget(target, options.getOverrideMode(),
						entry.getValue().modifiedInstant(this.zipOptions.getTimeZone(), this.options.isStrict()))) {
					available.put(target, Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS));
				} else {
					unresolved.put(target, source);
				}
			} catch (UnsafeEntryException e) {
				throw e;
			} catch (IOException e) {
				if (!options.isContinueOnError()) {
					throw e;
				}
				available.put(target, false);
			}
		}

		for (Path first : pending.keySet()) {
			Deque<Map.Entry<Path, Path>> chain = new ArrayDeque<>();
			Path current = first;
			Path source;
			while ((source = unresolved.remove(current)) != null) {
				chain.push(Map.entry(current, source));
				current = source;
			}
			if (chain.isEmpty()) {
				continue;
			}

			boolean cycle = pending.containsKey(current) && !available.containsKey(current);
			if (cycle && !options.isContinueOnError()) {
				throw new IOException("Cyclic ZIP hard-link dependency: " + current);
			}

			boolean sourceAvailable = !cycle && available.getOrDefault(current, true);
			while (!chain.isEmpty()) {
				Map.Entry<Path, Path> link = chain.pop();
				Path target = link.getKey();
				if (sourceAvailable) {
					try {
						sourceAvailable = this.createHardLink(
								root, target, link.getValue(), pending.get(target), options);
					} catch (UnsafeEntryException e) {
						throw e;
					} catch (IOException e) {
						if (!options.isContinueOnError()) {
							throw e;
						}
						sourceAvailable = false;
					}
				}
				available.put(target, sourceAvailable);
			}
		}
	}

	/**
	 * <h3 class="en-US">Publish link</h3>
	 * <h3 class="zh-CN">发布连接</h3>
	 *
	 * @param root       <span class="en-US">The root path</span>
	 *                   <span class="zh-CN">根路径</span>
	 * @param target     <span class="en-US">The target path</span>
	 *                   <span class="zh-CN">目标路径</span>
	 * @param linkSource <span class="en-US">Source link</span>
	 *                   <span class="zh-CN">源路径</span>
	 * @param symbolic   <span class="en-US">Symbolic link flag</span>
	 *                   <span class="zh-CN">软连接标记</span>
	 * @param header     <span class="en-US">File header</span>
	 *                   <span class="zh-CN">文件头</span>
	 * @param options    <span class="en-US">Extract options</span>
	 *                   <span class="zh-CN">解压选项</span>
	 * @return <span class="en-US">Create result</span>
	 * <span class="zh-CN">操作结果</span>
	 * @throws IOException <span class="en-US">If an error occurs when publish link</span>
	 *                     <span class="zh-CN">发布连接时出错</span>
	 */
	private boolean publishLink(final Path root, final Path target, final Path linkSource, final boolean symbolic,
	                            final FileHeader header, final ExtractOptions options) throws IOException {
		Instant modified = header.modifiedInstant(this.zipOptions.getTimeZone(),
				this.options.isStrict());

		if (ArchiveFile.existsTarget(target, options.getOverrideMode(), modified)) {
			return symbolic || Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS);
		}

		for (int attempt = 0; attempt < 5; attempt++) {
			Path temporary = target.resolveSibling(".nervousync-link-" + StringUtils.randomString(8));
			try {
				if (symbolic) {
					Files.createSymbolicLink(temporary, linkSource);
				} else {
					Files.createLink(temporary, linkSource);
				}
			} catch (FileAlreadyExistsException e) {
				continue;
			}

			try {
				if (symbolic) {
					EntryMetaData.applyBeforePublish(temporary, header, options, this.options, this.zipOptions);
					EntryMetaData.applyDosAttributes(temporary, header, options);
				}

				if (!allowLinkLocation(root, target, header, options)) {
					return false;
				}
				if (ArchiveFile.existsTarget(target, options.getOverrideMode(), modified)) {
					return symbolic || Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS);
				}

				try {
					temporaryToTarget(temporary, target, options);
				} catch (AccessDeniedException atomicFailure) {
					if (!symbolic || options.getOverrideMode() == OverrideMode.NEVER
							|| !Files.isSymbolicLink(temporary) || !Files.isSymbolicLink(target)) {
						throw atomicFailure;
					}
					try {
						Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException replacementFailure) {
						replacementFailure.addSuppressed(atomicFailure);
						throw replacementFailure;
					}
				}
				return true;
			} finally {
				Files.deleteIfExists(temporary);
			}
		}
		throw new IOException("Unable to stage ZIP link: " + target);
	}

	/**
	 * <h3 class="en-US">Write local file to the archive file</h3>
	 * <h3 class="zh-CN">将本地文件写入归档文件</h3>
	 *
	 * @param outputStream    <span class="en-US">Archive file output stream</span>
	 *                        <span class="zh-CN">归档文件输出流</span>
	 * @param localFileHeader <span class="en-US">Local file header</span>
	 *                        <span class="zh-CN">本地文件头</span>
	 * @param charset         <span class="en-US">Charset encoding</span>
	 *                        <span class="zh-CN">字符集</span>
	 * @param zip64Mode       <span class="en-US">Enumeration value of the Zip64</span>
	 *                        <span class="zh-CN">Zip64模式枚举值</span>
	 * @return <span class="en-US">General file header</span>
	 * <span class="zh-CN">通用文件头</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	private static GeneralFileHeader processFile(final ZipArchiveOutputStream outputStream,
	                                             final LocalFileHeader localFileHeader,
	                                             final Charset charset, final Zip64Mode zip64Mode)
			throws IOException {
		try (InputStream inputStream = FileUtils.loadFile(localFileHeader.getFilePath())) {
			GeneralFileHeader generalFileHeader = toGeneral(localFileHeader, charset);
			processStream(outputStream, inputStream, localFileHeader, generalFileHeader, charset, zip64Mode);
			return generalFileHeader;
		}
	}

	/**
	 * <h3 class="en-US">Write input stream to the archive file</h3>
	 * <h3 class="zh-CN">将输入流中的数据写入归档文件</h3>
	 *
	 * @param outputStream      <span class="en-US">Archive file output stream</span>
	 *                          <span class="zh-CN">归档文件输出流</span>
	 * @param inputStream       <span class="en-US">Input stream</span>
	 *                          <span class="zh-CN">输入流</span>
	 * @param localFileHeader   <span class="en-US">Local file header</span>
	 *                          <span class="zh-CN">本地文件头</span>
	 * @param generalFileHeader <span class="en-US">General file header</span>
	 *                          <span class="zh-CN">通用文件头</span>
	 * @param charset           <span class="en-US">Charset encoding</span>
	 *                          <span class="zh-CN">字符集</span>
	 * @param zip64Mode         <span class="en-US">Enumeration value of the Zip64</span>
	 *                          <span class="zh-CN">Zip64模式枚举值</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	private static void processStream(final ZipArchiveOutputStream outputStream, final InputStream inputStream,
	                                  final LocalFileHeader localFileHeader, final GeneralFileHeader generalFileHeader,
	                                  final Charset charset, final Zip64Mode zip64Mode) throws IOException {
		byte[] headerBytes = localFileHeader.toByteArray(charset, zip64Mode);
		outputStream.ensureFits(headerBytes.length);
		long diskNumber = outputStream.diskNumber(), offset = outputStream.offset();
		outputStream.writeHeader(headerBytes);
		generalFileHeader.setDiskNumberStart(diskNumber);
		generalFileHeader.setOffsetLocalHeader(offset);
		long copiedLength = inputStream.transferTo(outputStream);
		if (copiedLength != localFileHeader.getCompressedSize()) {
			throw new ArchiveException(0x0000001A0043L);
		}
		outputStream.write(localFileHeader.toDescriptor());
	}

	/**
	 * <h3 class="en-US">Convert local file header to general file header</h3>
	 * <h3 class="zh-CN">转换本地文件头为通用文件头</h3>
	 *
	 * @param fileHeader <span class="en-US">Local file header</span>
	 *                   <span class="zh-CN">本地文件头</span>
	 * @param charset    <span class="en-US">Charset encoding</span>
	 *                   <span class="zh-CN">字符集</span>
	 * @return <span class="en-US">General file header</span>
	 * <span class="zh-CN">通用文件头</span>
	 * @throws IOException <span class="en-US">Data invalid</span>
	 *                     <span class="zh-CN">数据错误</span>
	 */
	private static GeneralFileHeader toGeneral(final LocalFileHeader fileHeader, final Charset charset)
			throws IOException {
		Charset encoding = fileHeader.utf8Encoded() ? StandardCharsets.UTF_8 : charset;
		int nameLength = fileHeader.getEntryPath().getBytes(encoding).length;
		if (nameLength == 0 || nameLength > 0xFFFF) {
			throw new IOException("Invalid ZIP entry name length: " + nameLength);
		}

		List<ExtraDataRecord> centralExtras =
				(fileHeader.getExtraDataRecords() == null)
						? new ArrayList<>()
						: new ArrayList<>(fileHeader.getExtraDataRecords());

		if (fileHeader.getEncryptionInfo() instanceof StrongEncryptionInfo encryptionInfo) {
			centralExtras.removeIf(record -> record.headerId() == ZipGlobals.STRONG_ENC_HEADER_ID);
			centralExtras.add(new ExtraDataRecord(ZipGlobals.STRONG_ENC_HEADER_ID, encryptionInfo.toExtraData()));
		}

		int extraLength = 0;
		for (ExtraDataRecord record : centralExtras) {
			int length = record.dataBytes().length;
			if (length > 0xFFFF - extraLength - 4) {
				throw new IOException("ZIP central extra fields exceed 65535 bytes");
			}
			extraLength += 4 + length;
		}

		GeneralFileHeader generalFileHeader = new GeneralFileHeader();
		generalFileHeader.setSignature((int) ZipGlobals.GENERAL_FILE_HEADER_SIGNATURE);

		generalFileHeader.setMadeVersion(fileHeader.getMadeVersion());
		generalFileHeader.setExtractNeeded(fileHeader.getExtractNeeded());
		generalFileHeader.setGeneralPurpose(fileHeader.getGeneralPurpose());
		generalFileHeader.setCompressionMethod(fileHeader.getCompressionMethod());
		generalFileHeader.setLastModifiedTime(fileHeader.getLastModifiedTime());
		generalFileHeader.setLastModifiedDate(fileHeader.getLastModifiedDate());

		generalFileHeader.setCrc32(fileHeader.getCrc32());
		generalFileHeader.setCompressedSize(fileHeader.getCompressedSize());
		generalFileHeader.setUncompressedSize(fileHeader.getUncompressedSize());

		generalFileHeader.setEntryPath(fileHeader.getEntryPath());
		generalFileHeader.setNameLength(nameLength);
		generalFileHeader.setEncryptionInfo(fileHeader.getEncryptionInfo());
		generalFileHeader.setExtraDataRecords(List.copyOf(centralExtras));
		generalFileHeader.setExtraFieldLength(extraLength);

		generalFileHeader.setDiskNumberStart(fileHeader.getDiskNumberStart());
		generalFileHeader.setOffsetLocalHeader(fileHeader.getOffsetLocalHeader());
		generalFileHeader.setExternalFileAttr(fileHeader.getExternalFileAttr());

		if (StringUtils.notBlank(fileHeader.getComment())) {
			generalFileHeader.setFileComment(fileHeader.getComment());
			generalFileHeader.setCommentLength(fileHeader.getComment().getBytes(encoding).length);
		} else {
			generalFileHeader.setFileComment(Globals.DEFAULT_VALUE_STRING);
			generalFileHeader.setCommentLength(0);
		}

		return generalFileHeader;
	}

	/**
	 * <h3 class="en-US">Publish files</h3>
	 * <h3 class="zh-CN">发布文件</h3>
	 *
	 * @param publishFiles    <span class="en-US">File mapping table</span>
	 *                        <span class="zh-CN">文件映射表</span>
	 * @param originalVolumes <span class="en-US">Original volumes path list</span>
	 *                        <span class="zh-CN">原有分卷列表</span>
	 * @param backupDirectory <span class="en-US">Backup directory</span>
	 *                        <span class="zh-CN">备份目录</span>
	 * @throws IOException <span class="en-US">If an error occurs when publish files</span>
	 *                     <span class="zh-CN">发布文件出错</span>
	 */
	private static void publishFiles(final Map<Path, Path> publishFiles, final List<Path> originalVolumes,
	                                 final Path backupDirectory) throws IOException {
		Files.createDirectories(backupDirectory);

		Set<Path> originals = new HashSet<>();
		for (Path original : originalVolumes) {
			originals.add(original.toAbsolutePath().normalize());
		}

		for (Path destination : publishFiles.values()) {
			Path target = destination.toAbsolutePath().normalize();
			if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !originals.contains(target)) {
				throw new FileAlreadyExistsException(target.toString());
			}
		}

		Map<Path, Path> backups = new LinkedHashMap<>();
		List<Path> installed = new ArrayList<>();

		try {
			for (Path original : originals) {
				if (!Files.exists(original, LinkOption.NOFOLLOW_LINKS)) {
					continue;
				}

				Path backup = backupDirectory.resolve(original.getFileName());
				Files.move(original, backup);
				backups.put(original, backup);
			}

			for (Map.Entry<Path, Path> file : publishFiles.entrySet()) {
				Path destination = file.getValue().toAbsolutePath().normalize();
				Files.move(file.getKey(), destination);
				installed.add(destination);
			}
		} catch (IOException | RuntimeException failure) {
			for (int index = installed.size() - 1; index >= 0; index--) {
				try {
					Files.deleteIfExists(installed.get(index));
				} catch (IOException | RuntimeException cleanupFailure) {
					failure.addSuppressed(cleanupFailure);
				}
			}

			for (Map.Entry<Path, Path> backup : backups.entrySet()) {
				try {
					Files.move(backup.getValue(), backup.getKey(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException | RuntimeException restoreFailure) {
					failure.addSuppressed(restoreFailure);
				}
			}

			throw failure;
		}
	}

	/**
	 * <h3 class="en-US">Process archive data</h3>
	 * <h3 class="zh-CN">处理归档数据</h3>
	 *
	 * @param outputStream <span class="en-US">Output stream</span>
	 *                     <span class="zh-CN">输出流</span>
	 * @throws IOException <span class="en-US">If an error occurs when process archive data</span>
	 *                     <span class="zh-CN">处理归档数据出错</span>
	 */
	private void processArchive(final ZipArchiveOutputStream outputStream) throws IOException {
		Map<String, Integer> entryIndexMap = new HashMap<>();
		int index = 0;
		boolean notModified =
				this.openMode != OpenMode.CREATE && this.newEntries.isEmpty() && this.removeEntries.isEmpty();
		List<GeneralFileHeader> headerList = this.centralDirectory.entryHeaders();
		for (GeneralFileHeader fileHeader : headerList) {
			if (this.removeEntries.contains(fileHeader.getEntryPath())) {
				notModified = false;
				index++;
				continue;
			}
			if (this.newEntries.containsKey(fileHeader.getEntryPath())) {
				notModified = false;
				entryIndexMap.put(fileHeader.getEntryPath(), Globals.DEFAULT_VALUE_INT);
			} else {
				if (entryIndexMap.containsKey(fileHeader.getEntryPath())) {
					switch (this.zipOptions.getDuplicatePolicy()) {
						case FIRST:
							continue;
						case LAST:
							notModified = false;
							entryIndexMap.put(fileHeader.getEntryPath(), index);
							break;
						case REJECT:
							throw new ArchiveException(0x0000001A003FL, fileHeader.getEntryPath());
					}
				} else {
					entryIndexMap.put(fileHeader.getEntryPath(), index);
				}
			}
			index++;
		}

		if (notModified) {
			return;
		}

		Queue<GeneralFileHeader> headerQueue = new ArrayDeque<>();
		for (Map.Entry<String, Integer> entry : entryIndexMap.entrySet()) {
			if (entry.getValue() == Globals.DEFAULT_VALUE_INT) {
				LocalFileHeader localFileHeader = this.newEntries.remove(entry.getKey());
				GeneralFileHeader generalFileHeader =
						processFile(outputStream, localFileHeader, this.options.getCharset(),
								this.zipOptions.getZip64Mode());
				headerQueue.add(generalFileHeader);
			} else {
				GeneralFileHeader generalFileHeader = headerList.get(entry.getValue());
				LocalFileHeader localFileHeader =
						this.operator.readLocalFileHeader(generalFileHeader, this.options.getCharset(),
								this.options.getArchiveLimit().getMaxHeaderBytes());
				this.copyData(outputStream, generalFileHeader, localFileHeader);
				headerQueue.add(generalFileHeader);
			}
		}
		for (Map.Entry<String, LocalFileHeader> entry : this.newEntries.entrySet()) {
			LocalFileHeader localFileHeader = entry.getValue();
			GeneralFileHeader generalFileHeader =
					processFile(outputStream, localFileHeader, this.options.getCharset(),
							this.zipOptions.getZip64Mode());
			headerQueue.add(generalFileHeader);
		}

		this.processCentralDirectory(outputStream, headerQueue, this.zipOptions, true);
	}

	/**
	 * <h3 class="en-US">Copy entry data</h3>
	 * <h3 class="zh-CN">复制条目数据</h3>
	 *
	 * @param outputStream      <span class="en-US">Output stream</span>
	 *                          <span class="zh-CN">输出流</span>
	 * @param generalFileHeader <span class="en-US">General file header</span>
	 *                          <span class="zh-CN">通用文件头</span>
	 * @param localFileHeader   <span class="en-US">Local file header</span>
	 *                          <span class="zh-CN">本地文件头</span>
	 * @throws IOException <span class="en-US">If an error occurs when copy entry data</span>
	 *                     <span class="zh-CN">复制数据过程中出错</span>
	 */
	private void copyData(final ZipArchiveOutputStream outputStream, final GeneralFileHeader generalFileHeader,
	                      final LocalFileHeader localFileHeader) throws IOException {
		try (ZipArchiveInputStream inputStream = this.operator.inputStream()) {
			inputStream.seek(localFileHeader.getDiskNumberStart(), localFileHeader.getOffsetLocalHeader());

			int headerLength =
					Math.toIntExact(localFileHeader.getOffsetStartOfData() - localFileHeader.getOffsetLocalHeader());

			byte[] headerBytes = new byte[headerLength];
			new DataInputStream(inputStream).readFully(headerBytes);

			outputStream.ensureFits(headerBytes.length);
			long diskNumber = outputStream.diskNumber();
			long offset = outputStream.offset();

			outputStream.writeHeader(headerBytes);
			generalFileHeader.setDiskNumberStart(diskNumber);
			generalFileHeader.setOffsetLocalHeader(offset);

			long copiedLength =
					new BoundedInputStream(inputStream, localFileHeader.getCompressedSize()).transferTo(outputStream);

			if (copiedLength != localFileHeader.getCompressedSize()) {
				throw new ArchiveException(0x0000001A0043L);
			}

			outputStream.write(localFileHeader.toDescriptor());
		}
	}

	/**
	 * <h3 class="en-US">Open strong encryption output stream</h3>
	 * <h3 class="zh-CN">打开强加密输出流</h3>
	 *
	 * @param targetStream <span class="en-US">Output stream</span>
	 *                     <span class="zh-CN">输出流</span>
	 * @param archivePath  <span class="en-US">Archive file path</span>
	 *                     <span class="zh-CN">归档文件地址</span>
	 * @param zipOptions   <span class="en-US">Zip options</span>
	 *                     <span class="zh-CN">Zip 选项</span>
	 * @return <span class="en-US">Strong encryption output stream</span>
	 * <span class="zh-CN">强加密输出流</span>
	 * @throws IOException <span class="en-US">If an error occurs when open stream</span>
	 *                     <span class="zh-CN">操作过程中出错</span>
	 */
	private StrongOutputStream openCentralDirectoryEncryption(final OutputStream targetStream, final String archivePath,
	                                                          final ZipOptions zipOptions) throws IOException {

		StrongAccessMode mode = Objects.requireNonNull(zipOptions.getCentralDirectoryAccessMode(), "centralDirectoryAccessMode");

		if (!mode.usePassword()) {
			return StrongOutputStream.create(targetStream, zipOptions.getCentralDirectoryEncryptionAlgorithm(),
					zipOptions.getCentralDirectoryKeyBitLength(), mode, null,
					zipOptions.getCentralDirectoryRecipients());
		}

		PasswordProvider passwordProvider = this.options.getPasswordProvider();
		PasswordRequest request =
				new PasswordRequest(PasswordPurpose.WRITE_HEADER, archivePath, Globals.DEFAULT_VALUE_STRING, 1);

		char[] password = null;
		byte[] passwordBytes = null;

		try (PasswordCredentials credentials =
				     passwordProvider.credentials(request).orElseThrow(() -> new ArchiveException(0x0000001B004CL, archivePath))) {
			password = credentials.copyPassword();
			passwordBytes = ZipCrypto.encodePassword(password, credentials.isUtf8Encoding());

			return StrongOutputStream.create(targetStream, zipOptions.getCentralDirectoryEncryptionAlgorithm(),
					zipOptions.getCentralDirectoryKeyBitLength(), mode, passwordBytes,
					zipOptions.getCentralDirectoryRecipients());
		} finally {
			RawUtils.clear(password);
			RawUtils.clear(passwordBytes);
		}
	}

	/**
	 * <h3 class="en-US">Process archive data</h3>
	 * <h3 class="zh-CN">处理归档数据</h3>
	 *
	 * @param outputStream <span class="en-US">Output stream</span>
	 *                     <span class="zh-CN">输出流</span>
	 * @param headerQueue  <span class="en-US">General file header queue</span>
	 *                     <span class="zh-CN">通用文件头队列</span>
	 * @throws IOException <span class="en-US">If an error occurs when process archive data</span>
	 *                     <span class="zh-CN">处理归档数据出错</span>
	 */
	private void processCentralDirectory(final ZipArchiveOutputStream outputStream,
	                                     final Queue<GeneralFileHeader> headerQueue,
	                                     final ZipOptions zipOptions, final boolean override) throws IOException {
		final long entryCount = headerQueue.size(), centralDirectoryStart = outputStream.getTotalSize();
		outputStream.beginCentralDirectory();

		//  Process central directory general file headers
		boolean protectionCentralDirectory =
				zipOptions.isCompressCentralDirectory()
						|| zipOptions.getCentralDirectoryEncryptionAlgorithm() != ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE
						|| zipOptions.getCentralDirectoryHashAlgorithm() != ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE;
		OutputStream centralDirectoryOutputStream = outputStream;
		DeflateOutputStream deflateOutputStream = null;
		BZip2OutputStream bZip2OutputStream = null;
		CentralDirectoryCountingOutputStream countingOutputStream = null;
		StrongOutputStream cryptoOutputStream = null;
		int centralDirectoryCompressionMethod = ZipGlobals.COMP_STORE;
		long centralDirectoryOriginalSize = 0L, compressedSize, uncompressedSize;
		CRC32 crc = null;
		MessageDigest digest = null;
		try {
			if (protectionCentralDirectory) {
				if (zipOptions.getCentralDirectoryEncryptionAlgorithm() != ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE) {
					cryptoOutputStream =
							this.openCentralDirectoryEncryption(centralDirectoryOutputStream,
									outputStream.archivePath(), zipOptions);
					centralDirectoryOutputStream = cryptoOutputStream;
				}
				if (zipOptions.isCompressCentralDirectory()) {
					switch (zipOptions.getAlgorithm()) {
						case STORE:
							break;
						case DEFLATE:
						case DEFLATE64:
							deflateOutputStream = new DeflateOutputStream(centralDirectoryOutputStream,
									zipOptions.getAlgorithm() == CompressionAlgorithm.DEFLATE64,
									zipOptions.getCompressionLevel());
							centralDirectoryOutputStream = deflateOutputStream;
							centralDirectoryCompressionMethod =
									(zipOptions.getAlgorithm() == CompressionAlgorithm.DEFLATE64)
											? ZipGlobals.COMP_DEFLATE64
											: ZipGlobals.COMP_DEFLATE;
							break;
						case BZIP2:
							countingOutputStream = new CentralDirectoryCountingOutputStream(centralDirectoryOutputStream);
							bZip2OutputStream =
									new BZip2OutputStream(countingOutputStream, Math.max(1, zipOptions.getCompressionLevel()));
							centralDirectoryOutputStream = bZip2OutputStream;
							centralDirectoryCompressionMethod = ZipGlobals.COMP_BZIP2;
							break;
						case LZMA1:
						case LZMA2:
						default:
							throw new ArchiveException(0x0000001A0021L, zipOptions.getAlgorithm());

					}
				}
			}

			switch (zipOptions.getCentralDirectoryHashAlgorithm()) {
				case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE:
					break;
				case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32:
					crc = new CRC32();
					break;
				case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_MD5:
					digest = MessageDigest.getInstance("MD5", BC_PROVIDER);
					break;
				case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA1:
					digest = MessageDigest.getInstance("SHA-1", BC_PROVIDER);
					break;
				case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_RIPEMD160:
					digest = MessageDigest.getInstance("RIPEMD160", BC_PROVIDER);
					break;
				case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256:
					digest = MessageDigest.getInstance("SHA-256", BC_PROVIDER);
					break;
				case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA384:
					digest = MessageDigest.getInstance("SHA-384", BC_PROVIDER);
					break;
				case ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA512:
					digest = MessageDigest.getInstance("SHA-512", BC_PROVIDER);
					break;
				default:
					throw new ArchiveException(0x0000001A001FL);
			}
		} catch (NoSuchAlgorithmException e) {
			throw new ArchiveException(0x0000001A001FL, e);
		}

		try {
			GeneralFileHeader generalFileHeader = headerQueue.poll();
			while (generalFileHeader != null) {
				byte[] headerBytes =
						generalFileHeader.toByteArray(this.options.getCharset(), zipOptions.getZip64Mode());
				if (centralDirectoryOutputStream == outputStream) {
					outputStream.writeHeader(headerBytes);
				} else {
					centralDirectoryOutputStream.write(headerBytes);
				}
				if (crc != null) {
					crc.update(headerBytes);
				}
				if (digest != null) {
					digest.update(headerBytes);
				}
				centralDirectoryOriginalSize = Math.addExact(centralDirectoryOriginalSize, headerBytes.length);
				generalFileHeader = headerQueue.poll();
			}

			if (deflateOutputStream != null) {
				deflateOutputStream.finish();

				compressedSize = deflateOutputStream.getBytesWritten();
				uncompressedSize = deflateOutputStream.getBytesRead();
			} else if (bZip2OutputStream != null) {
				bZip2OutputStream.finish();

				compressedSize = countingOutputStream.getCount();
				uncompressedSize = centralDirectoryOriginalSize;
			} else {
				compressedSize = centralDirectoryOriginalSize;
				uncompressedSize = centralDirectoryOriginalSize;
			}

			if (cryptoOutputStream != null) {
				cryptoOutputStream.finish();
			}
		} finally {
			if (deflateOutputStream != null) {
				deflateOutputStream.release();
			}
		}

		outputStream.endCentralDirectory();

		final long centralDirectorySize = outputStream.getTotalSize() - centralDirectoryStart,
				centralDirectoryStartDisk = outputStream.getCentralDirectoryStartDisk(),
				centralDirectoryOffset = outputStream.getCentralDirectoryOffset();

		Zip64EndCentralDirectory.Protection protection = null;
		if (protectionCentralDirectory) {
			protection = new Zip64EndCentralDirectory.Protection();

			boolean encryptedCentralDirectory = cryptoOutputStream != null;
			protection.setCompressionMethod(centralDirectoryCompressionMethod);
			protection.setAlgorithm(encryptedCentralDirectory
					? zipOptions.getCentralDirectoryEncryptionAlgorithm()
					: ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE);
			protection.setKeyBitLength(encryptedCentralDirectory ? zipOptions.getCentralDirectoryKeyBitLength() : 0);
			protection.setFlags(encryptedCentralDirectory ? cryptoOutputStream.getEncryptionInfo().getFlags() : 0);

			protection.setCompressedSize(compressedSize);
			protection.setUncompressedSize(uncompressedSize);

			protection.setHashAlgorithm(zipOptions.getCentralDirectoryHashAlgorithm());
			if (zipOptions.getCentralDirectoryHashAlgorithm() == ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE) {
				protection.setHashData(new byte[0]);
			} else if (zipOptions.getCentralDirectoryHashAlgorithm() == ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32) {
				if (crc == null) {
					throw new ArchiveException(0x0000001A001FL);
				} else {
					protection.setHashData(ByteBuffer.allocate(Integer.BYTES)
							.order(ByteOrder.LITTLE_ENDIAN)
							.putInt((int) crc.getValue())
							.array());
				}
			} else {
				if (digest == null) {
					throw new ArchiveException(0x0000001A001FL);
				}
				protection.setHashData(digest.digest());
			}
		}

		byte[] commentBytes = this.endOfCentralDirectory.getCommentBytes();
		if (commentBytes.length > ZipGlobals.ZIP_16_LIMIT) {
			throw new IOException("ZIP archive comment exceeds 65535 bytes");
		}

		final long endOfCentralDirectoryLength = 22L + commentBytes.length;
		outputStream.ensureFits(endOfCentralDirectoryLength);

		final boolean zip64,
				required = protection != null
						|| outputStream.diskNumber() >= ZipGlobals.ZIP_16_LIMIT
						|| centralDirectoryStartDisk >= ZipGlobals.ZIP_16_LIMIT
						|| outputStream.diskEntries() >= ZipGlobals.ZIP_16_LIMIT
						|| entryCount >= ZipGlobals.ZIP_16_LIMIT
						|| centralDirectorySize >= ZipGlobals.ZIP_64_LIMIT
						|| centralDirectoryOffset >= ZipGlobals.ZIP_64_LIMIT;

		zip64 = switch (zipOptions.getZip64Mode()) {
			case ALWAYS -> true;
			case AUTO -> required;
			case NEVER -> {
				if (required) {
					throw new IOException("ZIP64 is required, but Zip64Mode is NEVER");
				}
				yield false;
			}
		};

		final boolean transformed = (protection != null && (protection.compressed() || protection.encrypted()));
		Zip64EndCentralDirectory zip64EndCentralDirectory = null;
		byte[] locatorBytes = new byte[0];

		if (zip64) {
			final long length = 56L + (protection == null ? 0L : 28L + protection.getHashData().length);
			outputStream.ensureFits(length);

			final long diskNumber = outputStream.diskNumber(), diskOffset = outputStream.offset();

			int extractNeeded = protection == null ? ZipGlobals.ZIP_64_VERSION : ZipGlobals.ZIP_64_VERSION_2;
			if (protection != null
					&& (protection.getAlgorithm() == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH
					|| protection.getAlgorithm() == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_TWOFISH)) {
				extractNeeded = 63;
			}
			int hostSystem;
			if (SystemUtils.isMacOS()) {
				hostSystem = 19;
			} else if (SystemUtils.isUnix()) {
				hostSystem = 3;
			} else {
				hostSystem = 0;
			}

			Zip64EndCentralDirectory.Record record = new Zip64EndCentralDirectory.Record();

			record.setDiskNumber(diskNumber);
			record.setCentralDirectoryStartDisk(centralDirectoryStartDisk);
			record.setCentralDirectoryOffset(centralDirectoryOffset);
			record.setCentralDirectorySize(centralDirectorySize);
			record.setEntriesOnThisDisk(transformed ? 0L : outputStream.diskEntries());
			record.setTotalEntries(entryCount);
			record.setExtractNeeded(extractNeeded);
			record.setMadeVersion((hostSystem << 8) | extractNeeded);
			record.setRecordSize(length - 12L);
			record.setExtensibleDataSector(new byte[0]);

			byte[] recordBytes = record.toByteArray(protection);
			if (recordBytes.length != length) {
				throw new IOException("Unexpected ZIP64 EOCD record length");
			}

			outputStream.write(recordBytes);

			outputStream.ensureFits(20L + endOfCentralDirectoryLength);

			Zip64EndCentralDirectory.Locator locator = new Zip64EndCentralDirectory.Locator();

			locator.setZip64EndRecordDiskNumber(diskNumber);
			locator.setZip64EndRecordOffset(diskOffset);
			locator.setTotalNumberOfDisks(outputStream.diskCount());

			zip64EndCentralDirectory = new Zip64EndCentralDirectory(record, locator);
			zip64EndCentralDirectory.setProtection(protection);

			locatorBytes = locator.toByteArray();
		}

		EndCentralDirectoryRecord endCentralDirectoryRecord = new EndCentralDirectoryRecord();

		endCentralDirectoryRecord.setCommentBytes(commentBytes);
		endCentralDirectoryRecord.setDiskNumber((int) Math.min(outputStream.diskNumber(), ZipGlobals.ZIP_16_LIMIT));
		endCentralDirectoryRecord.setCentralDirectoryStartDisk((int) Math.min(centralDirectoryStartDisk, ZipGlobals.ZIP_16_LIMIT));

		if (transformed) {
			endCentralDirectoryRecord.setEntriesOnThisDisk(0);
			endCentralDirectoryRecord.setTotalEntries(ZipGlobals.ZIP_16_LIMIT);
		} else {
			endCentralDirectoryRecord.setEntriesOnThisDisk((int) Math.min(outputStream.diskEntries(), ZipGlobals.ZIP_16_LIMIT));
			endCentralDirectoryRecord.setTotalEntries((int) Math.min(entryCount, ZipGlobals.ZIP_16_LIMIT));
		}

		endCentralDirectoryRecord.setCentralDirectorySize(Math.min(centralDirectorySize, ZipGlobals.ZIP_64_LIMIT));
		endCentralDirectoryRecord.setCentralDirectoryOffset(Math.min(centralDirectoryOffset, ZipGlobals.ZIP_64_LIMIT));

		outputStream.write(RawUtils.join(locatorBytes, endCentralDirectoryRecord.toByteArray()));

		if (override) {
			this.zip64EndCentralDirectory = zip64EndCentralDirectory;
			this.endOfCentralDirectory = endCentralDirectoryRecord;
		}
	}

	/**
	 * <h3 class="en-US">Close pending writers</h3>
	 * <h3 class="zh-CN">关闭处理中的写入器</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when close writers</span>
	 *                     <span class="zh-CN">关闭写入器时出错</span>
	 */
	private void closePendingWriters() throws IOException {
		IOException failure = null;
		for (ZipEntryResourceWriter writer : List.copyOf(this.registerWriters.values())) {
			try {
				writer.abort();
				writer.close();
			} catch (IOException | RuntimeException e) {
				IOException ex = (e instanceof IOException) ? (IOException) e : new IOException(e);
				if (failure == null) {
					failure = ex;
				} else {
					failure.addSuppressed(ex);
				}
			}
		}

		if (failure != null) {
			throw failure;
		}

		this.registerWriters.clear();
	}

	/**
	 * <h3 class="en-US">Delete temporary workspace</h3>
	 * <h3 class="zh-CN">删除临时工作区</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when delete workspace</span>
	 *                     <span class="zh-CN">删除工作区时出错</span>
	 */
	private void deleteWorkspace() throws IOException {
		Path expectedParent =
				Path.of(this.filePath).toAbsolutePath().normalize().getParent();
		this.deleteWorkspace(this.tempDirectory, expectedParent);
		this.tempDirectory = null;
	}

	/**
	 * <h3 class="en-US">Delete temporary workspace</h3>
	 * <h3 class="zh-CN">删除临时工作区</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when delete workspace</span>
	 *                     <span class="zh-CN">删除工作区时出错</span>
	 */
	private void deleteWorkspace(final Path tempDirectory, final Path expectedParent) throws IOException {
		if (this.preserveWorkspace || tempDirectory == null) {
			return;
		}

		Path root = tempDirectory.toAbsolutePath().normalize();

		if (!Objects.equals(root.getParent(), expectedParent)
				|| !root.getFileName().toString().startsWith(".nervousync-zip-")) {
			throw new IOException("Unexpected ZIP workspace: " + root);
		}

		if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
			Files.walkFileTree(root, new TemporaryFileVisitor());
		}
	}

	private static final class TemporaryFileVisitor extends SimpleFileVisitor<Path> {

		@Override
		@Nonnull
		public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attributes)
				throws IOException {
			Files.delete(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		@Nonnull
		public FileVisitResult postVisitDirectory(@Nonnull Path directory, IOException failure)
				throws IOException {
			if (failure != null) {
				throw failure;
			}

			Files.delete(directory);
			return FileVisitResult.CONTINUE;
		}
	}

	/**
	 * <h2 class="en-US">Unsafe entry exception</h2>
	 * <h2 class="zh-CN">不安全条目异常</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	private static final class UnsafeEntryException extends ArchiveException {

		/**
		 * <span class="en-US">Serial version UID</span>
		 * <span class="zh-CN">序列化UID</span>
		 */
		@Serial
		private static final long serialVersionUID = -8417418182954396703L;

		/**
		 * <h3 class="en-US">Private constructor method for the unsafe entry exception</h3>
		 * <h3 class="zh-CN">不安全条目异常的私有构造方法</h3>
		 *
		 * @param errorCode <span class="en-US">Error identified code</span>
		 *                  <span class="zh-CN">错误识别代码</span>
		 * @param entryPath <span class="en-US">Entry path</span>
		 *                  <span class="zh-CN">条目路径</span>
		 */
		private UnsafeEntryException(long errorCode, String entryPath) {
			super(errorCode, entryPath);
		}
	}

	/**
	 * <h2 class="en-US">Central directory counting output stream</h2>
	 * <h2 class="zh-CN">中央仓库计数输出流</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	private static final class CentralDirectoryCountingOutputStream extends OutputStream {

		/**
		 * <span class="en-US">Source output stream</span>
		 * <span class="zh-CN">源输出流</span>
		 */
		private final OutputStream output;
		/**
		 * <span class="en-US">Output counting</span>
		 * <span class="zh-CN">输出计数</span>
		 */
		private long count;

		/**
		 * <h3 class="en-US">Private constructor method for the central directory counting output stream</h3>
		 * <h3 class="zh-CN">中央仓库计数输出流的私有构造方法</h3>
		 *
		 * @param output <span class="en-US">Source output stream</span>
		 *               <span class="zh-CN">源输出流</span>
		 */
		private CentralDirectoryCountingOutputStream(final OutputStream output) {
			this.output = Objects.requireNonNull(output, "output");
		}

		@Override
		public void write(final int value) throws IOException {
			this.output.write(value);
			this.addCount(1);
		}

		@Override
		public void write(@Nonnull final byte[] bytes, final int offset, final int length) throws IOException {
			this.output.write(bytes, offset, length);
			this.addCount(length);
		}

		@Override
		public void flush() throws IOException {
			this.output.flush();
		}

		private void addCount(final int length) throws IOException {
			try {
				this.count = Math.addExact(this.count, length);
			} catch (ArithmeticException e) {
				throw new IOException("Compressed central directory size overflow", e);
			}
		}

		/**
		 * <h3 class="en-US">Getter method for the output counting</h3>
		 * <h3 class="zh-CN">输出计数的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">Output counting</span>
		 * <span class="zh-CN">输出计数</span>
		 */
		public long getCount() {
			return this.count;
		}
	}

	private enum SessionState {
		ACTIVE, COMMITING, COMMITTED, ROLLED_BACK, FAILED, CLOSED
	}
}
