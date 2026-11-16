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

package org.nervousync.archive;

import jakarta.annotation.Nonnull;
import org.nervousync.archive.adaptor.ArchiveAdaptor;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.DataFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.enumerations.CompressionFraming;
import org.nervousync.archive.enumerations.ContainerFormat;
import org.nervousync.archive.enumerations.OpenMode;
import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.tar.TarAdaptor;
import org.nervousync.archive.zip.ZipAdaptor;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.FileUtils;
import org.nervousync.utils.commons.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * <h2 class="en-US">Archive file</h2>
 * <h2 class="zh-CN">归档文件</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ArchiveFile implements AutoCloseable {

	/**
	 * <span class="en-US">Archive adaptor</span>
	 * <span class="zh-CN">归档适配器</span>
	 */
	private final ArchiveAdaptor adaptor;
	/**
	 * <span class="en-US">Archive descriptor</span>
	 * <span class="zh-CN">归档描述器</span>
	 */
	private final ArchiveDescriptor descriptor;
	/**
	 * <span class="en-US">Archive options</span>
	 * <span class="zh-CN">归档选项</span>
	 */
	private final ArchiveOptions options;
	/**
	 * <span class="en-US">Rollback requested flag</span>
	 * <span class="zh-CN">请求回滚标记</span>
	 */
	private boolean rollbackRequested = false;
	/**
	 * <span class="en-US">Closed flag</span>
	 * <span class="zh-CN">关闭标记</span>
	 */
	private boolean closed = false;

	/**
	 * <h3 class="en-US">Private constructor method for the archive file</h3>
	 * <h3 class="zh-CN">归档文件的私有构造方法</h3>
	 *
	 * @param adaptor    <span class="en-US">Archive adaptor</span>
	 *                   <span class="zh-CN">归档适配器</span>
	 * @param descriptor <span class="en-US">Archive descriptor</span>
	 *                   <span class="zh-CN">归档描述器</span>
	 * @param options    <span class="en-US">Archive options</span>
	 *                   <span class="zh-CN">归档选项</span>
	 */
	private ArchiveFile(final ArchiveAdaptor adaptor, final ArchiveDescriptor descriptor, final ArchiveOptions options) {
		this.adaptor = adaptor;
		this.descriptor = descriptor;
		this.options = options;
	}

	/**
	 * <h3 class="en-US">Static method for initialize Zip archive file</h3>
	 * <h3 class="zh-CN">静态方法用于初始化 Zip 文件</h3>
	 *
	 * @param dataFile   <span class="en-US">Data file</span>
	 *                   <span class="zh-CN">数据文件</span>
	 * @param zipOptions <span class="en-US">ZIP options</span>
	 *                   <span class="zh-CN">ZIP 选项</span>
	 * @return <span class="en-US">Archive file instance object</span>
	 * <span class="zh-CN">归档文件实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open archive file</span>
	 *                     <span class="zh-CN">打开归档文件出错</span>
	 */
	public static ArchiveFile zipFile(final DataFile dataFile, final ZipOptions zipOptions)
			throws IOException {
		return zipFile(dataFile, ArchiveOptions.newBuilder().build(), zipOptions);
	}

	/**
	 * <h3 class="en-US">Static method for initialize Zip archive file</h3>
	 * <h3 class="zh-CN">静态方法用于初始化 Zip 文件</h3>
	 *
	 * @param dataFile   <span class="en-US">Data file</span>
	 *                   <span class="zh-CN">数据文件</span>
	 * @param options    <span class="en-US">Archive options</span>
	 *                   <span class="zh-CN">归档选项</span>
	 * @param zipOptions <span class="en-US">ZIP options</span>
	 *                   <span class="zh-CN">ZIP 选项</span>
	 * @return <span class="en-US">Archive file instance object</span>
	 * <span class="zh-CN">归档文件实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open archive file</span>
	 *                     <span class="zh-CN">打开归档文件出错</span>
	 */
	public static ArchiveFile zipFile(final DataFile dataFile, final ArchiveOptions options, final ZipOptions zipOptions)
			throws IOException {
		return new ArchiveFile(new ZipAdaptor(dataFile, options, zipOptions),
				new ArchiveDescriptor(ContainerFormat.ZIP, null), options);
	}

	/**
	 * <h3 class="en-US">Static method for initialize Tar archive file</h3>
	 * <h3 class="zh-CN">静态方法用于初始化 Tar 文件</h3>
	 *
	 * @param filePath <span class="en-US">Current zip file path</span>
	 *                 <span class="zh-CN">当前压缩文件路径</span>
	 * @param openMode <span class="en-US">Enumeration value of the open mode</span>
	 *                 <span class="zh-CN">打开模式枚举值</span>
	 * @param framing  <span class="en-US">Enumeration value of the compression framing</span>
	 *                 <span class="zh-CN">压缩架构枚举值</span>
	 * @param options  <span class="en-US">Archive options</span>
	 *                 <span class="zh-CN">归档选项</span>
	 * @return <span class="en-US">Archive file instance object</span>
	 * <span class="zh-CN">归档文件实例对象</span>
	 */
	public static ArchiveFile tarFile(final String filePath, final OpenMode openMode,
	                                  final CompressionFraming framing, final ArchiveOptions options) {
		ArchiveDescriptor descriptor = switch (framing) {
			case GZIP -> new ArchiveDescriptor(ContainerFormat.TAR, CompressionDescriptor.TAR_GZ);
			case BZIP2_STREAM -> new ArchiveDescriptor(ContainerFormat.TAR, CompressionDescriptor.TAR_BZ2);
			case XZ -> new ArchiveDescriptor(ContainerFormat.TAR, CompressionDescriptor.TAR_XZ);
			default -> throw new IllegalArgumentException("Unknown framing: " + framing);
		};
		return new ArchiveFile(new TarAdaptor(filePath, openMode), descriptor, options);
	}

	/**
	 * <h3 class="en-US">Get the archive entry</h3>
	 * <h3 class="zh-CN">获取归档条目</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @return <span class="en-US">Archive entry</span>
	 * <span class="zh-CN">归档条目</span>
	 */
	public Optional<ArchiveEntry> entry(final String entryPath) {
		return this.adaptor.entry(entryPath);
	}

	/**
	 * <h3 class="en-US">Add file to current archive file</h3>
	 * <h3 class="zh-CN">添加文件到当前压缩包</h3>
	 *
	 * @param entryPath <span class="en-US">Entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @param filePath  <span class="en-US">The target path</span>
	 *                  <span class="zh-CN">目标地址</span>
	 * @param comment   <span class="en-US">Comment string</span>
	 *                  <span class="zh-CN">批注信息</span>
	 * @return <span class="en-US">Current archive file instance object</span>
	 * <span class="zh-CN">当前归档文件实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when add file to archive file</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public ArchiveFile addFile(final String entryPath, final String filePath, final String comment) throws IOException {
		try (ResourceWriter writer = this.openWriter(entryPath, comment).orElseThrow()) {
			writer.write(filePath);
			writer.finish();
		} catch (Exception e) {
			throw (e instanceof IOException) ? (IOException) e : new IOException(e);
		}
		return this;
	}

	/**
	 * <h3 class="en-US">Add folder to current archive file</h3>
	 * <h3 class="zh-CN">添加文件夹到当前压缩包</h3>
	 *
	 * @param rootPath   <span class="en-US">Entry root path</span>
	 *                   <span class="zh-CN">条目根路径</span>
	 * @param folderPath <span class="en-US">Folder path</span>
	 *                   <span class="zh-CN">文件夹路径</span>
	 * @return <span class="en-US">Current archive file instance object</span>
	 * <span class="zh-CN">当前归档文件实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when add folder to archive file</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public ArchiveFile addDirectory(final String rootPath, final String folderPath) throws IOException {
		for (String filePath : FileUtils.listFiles(folderPath)) {
			String itemPath = filePath.substring(folderPath.length());
			if (itemPath.startsWith(Globals.DEFAULT_PAGE_SEPARATOR)) {
				itemPath = itemPath.substring(1);
			}
			String entryPath = StringUtils.isEmpty(rootPath)
					? itemPath
					: rootPath + ZipGlobals.DEFAULT_ZIP_PAGE_SEPARATOR + itemPath;
			this.addFile(entryPath, filePath, Globals.DEFAULT_VALUE_STRING);
		}
		return this;
	}

	/**
	 * <h3 class="en-US">Extract to the target path</h3>
	 * <h3 class="zh-CN">释放到目标地址</h3>
	 *
	 * @param path    <span class="en-US">The target path</span>
	 *                <span class="zh-CN">目标地址</span>
	 * @param options <span class="en-US">Extract options</span>
	 *                <span class="zh-CN">解压选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when extract files</span>
	 *                     <span class="zh-CN">释放文件执行中出错</span>
	 */
	public void extract(final Path path, final ExtractOptions options) throws IOException {
		this.adaptor.extract(path, options);
	}

	/**
	 * <h3 class="en-US">Get the resource reader of the given archive entry</h3>
	 * <h3 class="zh-CN">获取指定条目的资源读取器</h3>
	 *
	 * @param entry <span class="en-US">The given archive entry</span>
	 *              <span class="zh-CN">指定归档条目</span>
	 * @return <span class="en-US">Resource reader instance object</span>
	 * <span class="zh-CN">资源读取器实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open resource reader</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public Optional<ResourceReader> openReader(final ArchiveEntry entry) throws IOException {
		return this.adaptor.openReader(entry);
	}

	/**
	 * <h3 class="en-US">Get the resource writer of the given archive entry</h3>
	 * <h3 class="zh-CN">获取指定条目的资源写入器</h3>
	 *
	 * @param entryPath <span class="en-US">The entry path</span>
	 *                  <span class="zh-CN">条目路径</span>
	 * @param comment   <span class="en-US">Comment string</span>
	 *                  <span class="zh-CN">批注信息</span>
	 * @return <span class="en-US">Resource writer instance object</span>
	 * <span class="zh-CN">资源写入器实例对象</span>
	 * @throws IOException <span class="en-US">If an error occurs when open resource reader</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public Optional<ResourceWriter> openWriter(final String entryPath, final String comment) throws IOException {
		this.ensureWritable();
		return this.adaptor.openWriter(entryPath, comment);
	}

	/**
	 * <h3 class="en-US">Remove the given archive entry</h3>
	 * <h3 class="zh-CN">删除指定条目</h3>
	 *
	 * @param archiveEntry <span class="en-US">The given archive entry</span>
	 *                     <span class="zh-CN">指定归档条目</span>
	 * @throws IOException <span class="en-US">If an error occurs when open resource reader</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public ArchiveFile remove(final ArchiveEntry archiveEntry) throws IOException {
		this.ensureWritable();
		this.adaptor.remove(archiveEntry);
		return this;
	}

	/**
	 * <h3 class="en-US">Merge the split archive file</h3>
	 * <h3 class="zh-CN">合并分卷文件</h3>
	 *
	 * @param target <span class="en-US">The target path</span>
	 *               <span class="zh-CN">目标地址</span>
	 * @throws IOException <span class="en-US">If an error occurs when open resource reader</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public void merge(@Nonnull final Path target) throws IOException {
		this.adaptor.merge(target);
	}

	/**
	 * <h3 class="en-US">Rollback the modify</h3>
	 * <h3 class="zh-CN">回滚修改</h3>
	 *
	 * @throws IOException <span class="en-US">If an error occurs when rollback modify</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public void rollback() throws IOException {
		if (this.closed) {
			throw new ArchiveException(0x0000001A0046L);
		}
		this.rollbackRequested = true;
		this.adaptor.rollback();
	}

	@Override
	public void close() throws Exception {
		if (this.closed) {
			return;
		}

		this.closed = true;

		try (ArchiveAdaptor adaptor = this.adaptor) {
			if (adaptor.openMode() == OpenMode.READ || this.rollbackRequested) {
				return;
			}
			try {
				adaptor.commit();
			} catch (IOException | RuntimeException e) {
				this.rollbackRequested = true;
				try {
					adaptor.rollback();
				} catch (Exception rollbackFailure) {
					if (rollbackFailure != e) {
						e.addSuppressed(rollbackFailure);
					}
				}
				throw e;
			}
		}
	}

	/**
	 * <h3 class="en-US">Check weather the target path was existed</h3>
	 * <h3 class="zh-CN">检查目标路径是否已经存在</h3>
	 *
	 * @param target       <span class="en-US">Target path</span>
	 *                     <span class="zh-CN">目标路径</span>
	 * @param mode         <span class="en-US">Override mode</span>
	 *                     <span class="zh-CN">覆盖模式</span>
	 * @param lastModified <span class="en-US">Last modified</span>
	 *                     <span class="zh-CN">最后修改时间</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 * @throws IOException <span class="en-US">If an error occurs when check target existed</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public static boolean existsTarget(final Path target, final OverrideMode mode, final Instant lastModified)
			throws IOException {
		if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
			return switch (mode) {
				case NEVER -> true;
				case ALWAYS -> false;
				case NEWER ->
						!Files.getLastModifiedTime(target, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(lastModified);
			};
		}
		return false;
	}

	/**
	 * <h3 class="en-US">Validate parent component</h3>
	 * <h3 class="zh-CN">验证父目录</h3>
	 *
	 * @param root   <span class="en-US">Root path</span>
	 *               <span class="zh-CN">根路径</span>
	 * @param target <span class="en-US">Target path</span>
	 *               <span class="zh-CN">目标路径</span>
	 * @throws IOException <span class="en-US">If an error occurs when validate parent component</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public static void validateParentComponents(final Path root, final Path target) throws IOException {
		Path normalizedRoot = root.toAbsolutePath().normalize();
		Path normalizedTarget = target.toAbsolutePath().normalize();

		if (!normalizedTarget.startsWith(normalizedRoot)) {
			throw new ArchiveException(0x0000001A003AL, normalizedRoot);
		}

		if (Files.isSymbolicLink(normalizedRoot) || !Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)) {
			throw new ArchiveException(0x0000001A003BL, normalizedRoot);
		}

		Path parent = normalizedTarget.getParent();
		if (parent == null) {
			throw new ArchiveException(0x0000001A003CL, normalizedTarget);
		}

		Path current = normalizedRoot;
		for (Path component : normalizedRoot.relativize(parent)) {
			current = current.resolve(component);
			if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
				continue;
			}
			if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
				throw new ArchiveException(0x0000001A003DL, current);
			}
		}
	}

	/**
	 * <h3 class="en-US">Ensure current file is writable</h3>
	 * <h3 class="zh-CN">确认文件可写</h3>
	 *
	 * @throws IOException <span class="en-US">File is not writable</span>
	 *                     <span class="zh-CN">文件不可写</span>
	 */
	private void ensureWritable() throws IOException {
		if (this.closed) {
			throw new ArchiveException(0x0000001A0046L);
		}
		if (this.rollbackRequested) {
			throw new ArchiveException(0x0000001A0049L, "ROLLBACK_REQUESTED");
		}
	}
}
