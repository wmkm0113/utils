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
import org.codehaus.plexus.util.StringUtils;
import org.nervousync.archive.enumerations.EntryType;
import org.nervousync.archive.zip.models.EntryTimes;
import org.nervousync.archive.zip.models.header.FileHeader;
import org.nervousync.commons.Globals;
import org.nervousync.exceptions.builder.BuilderException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

/**
 * <h2 class="en-US">Archive entry</h2>
 * <h2 class="zh-CN">归档条目</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ArchiveEntry {

	/**
	 * <span class="en-US">Entry path</span>
	 * <span class="zh-CN">条目路径</span>
	 */
	private final String entryPath;
	/**
	 * <span class="en-US">Enumeration value of the entry type</span>
	 * <span class="zh-CN">条目类型枚举值</span>
	 */
	private final EntryType entryType;
	/**
	 * <span class="en-US">Original size</span>
	 * <span class="zh-CN">原始大小</span>
	 */
	private final long uncompressedSize;
	/**
	 * <span class="en-US">Compressed size</span>
	 * <span class="zh-CN">压缩后大小</span>
	 */
	private final long compressedSize;
	/**
	 * <span class="en-US">Last modified timestamp</span>
	 * <span class="zh-CN">最后修改时间戳</span>
	 */
	private final Instant lastModified;
	/**
	 * <span class="en-US">Last access timestamp</span>
	 * <span class="zh-CN">最后访问时间</span>
	 */
	private final Instant lastAccessTime;
	/**
	 * <span class="en-US">File create timestamp</span>
	 * <span class="zh-CN">文件创建时间</span>
	 */
	private final Instant creationTime;
	/**
	 * <span class="en-US">Unix file type and mode code</span>
	 * <span class="zh-CN">Unix 文件类型及权限位</span>
	 */
	private final int unixMode;
	/**
	 * <span class="en-US">Dos file attributes</span>
	 * <span class="zh-CN">Dos 文件属性</span>
	 */
	private final int dosAttributes;
	/**
	 * <span class="en-US">Link target path</span>
	 * <span class="zh-CN">符号链接指向的路径</span>
	 */
	private final String linkTarget;

	/**
	 * <h3 class="en-US">Archive entry</h3>
	 * <h3 class="zh-CN">归档条目的构造方法</h3>
	 *
	 * @param builder <span class="en-US">Archive entry builder</span>
	 *                <span class="zh-CN">归档条目构建器</span>
	 */
	private ArchiveEntry(final Builder builder) {
		this.entryPath = builder.entryPath;
		this.entryType = builder.entryType;
		this.uncompressedSize = builder.uncompressedSize;
		this.compressedSize = builder.compressedSize;
		this.lastModified = builder.lastModified;
		this.lastAccessTime = builder.lastAccessTime;
		this.creationTime = builder.creationTime;
		this.unixMode = builder.unixMode;
		this.dosAttributes = builder.dosAttributes;
		this.linkTarget = builder.linkTarget;
	}

	/**
	 * <h3 class="en-US">Static method for initialize the builder instance object</h3>
	 * <h3 class="zh-CN">静态方法用于初始化构建器实例对象</h3>
	 *
	 * @return <span class="en-US">Builder instance object</span>
	 * <span class="zh-CN">构建器实例对象</span>
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * <h3 class="en-US">Generate an entry description based on the file header, without reading the entry content.</h3>
	 * <h3 class="zh-CN">根据文件头生成条目描述，不读取条目内容。</h3>
	 *
	 * @param header   <span class="en-US">File header information</span>
	 *                 <span class="zh-CN">文件头信息</span>
	 * @param timeZone <span class="en-US">Timezone</span>
	 *                 <span class="zh-CN">时区</span>
	 * @param strict   <span class="en-US">Strict flag</span>
	 *                 <span class="zh-CN">严格标记</span>
	 */
	public static ArchiveEntry describe(final FileHeader header, final TimeZone timeZone, final boolean strict)
			throws IOException {
		final Instant[] times = EntryTimes.read(header, strict);

		try {
			return ArchiveEntry.newBuilder()
					.entryPath(header.getEntryPath())
					.entryType(header.entryType())
					.uncompressedSize(header.getUncompressedSize())
					.compressedSize(header.getCompressedSize())
					.lastModified(header.modifiedInstant(timeZone, strict))
					.lastAccess(times[1])
					.createTimestamp(times[2])
					.unixMode(header.isUnix() ? header.unixMode() : -1)
					.dosAttributes(header.dosAttributes())
					.build();
		} catch (final BuilderException e) {
			throw new IOException("Invalid ZIP entry metadata", e);
		}
	}

	public static int entryTypeCode(final EntryType entryType) {
		return switch (entryType) {
			case DIRECTORY -> 0x4000;
			case SYMBOLIC_LINK -> 0xA000;
			default -> 0x8000;
		};
	}

	/**
	 * <h3 class="en-US">
	 * Capture source file attributes without following symbolic links
	 * </h3>
	 * <h3 class="zh-CN">读取源文件属性，不跟随符号链接</h3>
	 *
	 * @param path      <span class="en-US">Source file path</span>
	 *                  <span class="zh-CN">源文件路径</span>
	 * @param entryPath <span class="en-US">Path inside the archive</span>
	 *                  <span class="zh-CN">压缩包内的条目路径</span>
	 * @return <span class="en-US">Archive entry containing source attributes</span>
	 * <span class="zh-CN">包含源文件属性的归档条目</span>
	 * @throws IOException <span class="en-US">If the source attributes cannot be read</span>
	 *                     <span class="zh-CN">无法读取源文件属性时抛出</span>
	 */
	public static ArchiveEntry fromPath(final Path path, final String entryPath) throws IOException {
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(entryPath, "entryPath");

		final BasicFileAttributes basic =
				Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

		final EntryType entryType;
		if (basic.isSymbolicLink()) {
			entryType = EntryType.SYMBOLIC_LINK;
		} else if (basic.isDirectory()) {
			entryType = EntryType.DIRECTORY;
		} else if (basic.isRegularFile()) {
			entryType = EntryType.FILE;
		} else {
			throw new IOException("Unsupported source file type: " + path);
		}

		final Builder builder = newBuilder()
				.entryPath(entryPath)
				.entryType(entryType)
				.uncompressedSize(entryType == EntryType.FILE ? basic.size() : 0L)
				.lastModified(basic.lastModifiedTime().toInstant())
				.lastAccess(basic.lastAccessTime().toInstant())
				.createTimestamp(basic.creationTime().toInstant());

		final int fileType = entryTypeCode(entryType);

		final PosixFileAttributeView posixView =
				Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

		if (posixView != null) {
			if (path.getFileSystem().supportedFileAttributeViews().contains("unix")) {
				final Number mode = (Number) Files.getAttribute(
						path, "unix:mode", LinkOption.NOFOLLOW_LINKS);
				builder.unixMode(mode.intValue() & 0xFFFF);
			} else {
				int mode = fileType;
				final Set<PosixFilePermission> permissions = posixView.readAttributes().permissions();
				for (final PosixFilePermission permission : permissions) {
					mode |= 1 << (8 - permission.ordinal());
				}

				builder.unixMode(mode);
			}
		} else if (entryType == EntryType.SYMBOLIC_LINK) {
			// 缺少 POSIX 属性时，仍记录符号链接的文件类型。
			builder.unixMode(fileType | 0x01FF);
		}

		if (entryType == EntryType.SYMBOLIC_LINK) {
			// 保留链接中保存的原始目标，不转为绝对路径，不读取目标内容。
			builder.linkTarget(Files.readSymbolicLink(path).toString());
		} else {
			final DosFileAttributeView dosView =
					Files.getFileAttributeView(path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

			if (dosView != null) {
				final DosFileAttributes attributes = dosView.readAttributes();

				final int dosAttributes =
						(attributes.isReadOnly() ? 0x01 : 0)
								| (attributes.isHidden() ? 0x02 : 0)
								| (attributes.isSystem() ? 0x04 : 0)
								| (attributes.isDirectory() ? 0x10 : 0)
								| (attributes.isArchive() ? 0x20 : 0);

				builder.dosAttributes(dosAttributes);
			}
		}

		try {
			return builder.build();
		} catch (final BuilderException e) {
			throw new IOException("Invalid archive entry for source: " + path, e);
		}
	}

	/**
	 * <h3 class="en-US">Getter method for the entry path</h3>
	 * <h3 class="zh-CN">条目路径的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Entry path</span>
	 * <span class="zh-CN">条目路径</span>
	 */
	public String getEntryPath() {
		return this.entryPath;
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the entry type</h3>
	 * <h3 class="zh-CN">条目类型枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the entry type</span>
	 * <span class="zh-CN">条目类型枚举值</span>
	 */
	public EntryType getEntryType() {
		return this.entryType;
	}

	/**
	 * <h3 class="en-US">Getter method for the original size</h3>
	 * <h3 class="zh-CN">原始大小的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Original size</span>
	 * <span class="zh-CN">原始大小</span>
	 */
	public long getUncompressedSize() {
		return this.uncompressedSize;
	}

	/**
	 * <h3 class="en-US">Getter method for the compressed size</h3>
	 * <h3 class="zh-CN">压缩后大小的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Compressed size</span>
	 * <span class="zh-CN">压缩后大小</span>
	 */
	public long getCompressedSize() {
		return this.compressedSize;
	}

	/**
	 * <h3 class="en-US">Getter method for the last modified timestamp</h3>
	 * <h3 class="zh-CN">最后修改时间戳的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Last modified timestamp</span>
	 * <span class="zh-CN">最后修改时间戳</span>
	 */
	public Instant getLastModified() {
		return this.lastModified;
	}

	/**
	 * <h3 class="en-US">Getter method for the last access timestamp</h3>
	 * <h3 class="zh-CN">最后访问时间的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Last access timestamp</span>
	 * <span class="zh-CN">最后访问时间</span>
	 */
	public Instant getLastAccessTime() {
		return this.lastAccessTime;
	}

	/**
	 * <h3 class="en-US">Getter method for the file create timestamp</h3>
	 * <h3 class="zh-CN">文件创建时间的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">File create timestamp</span>
	 * <span class="zh-CN">文件创建时间</span>
	 */
	public Instant getCreationTime() {
		return this.creationTime;
	}

	/**
	 * <h3 class="en-US">Getter method for the unix file type and mode code</h3>
	 * <h3 class="zh-CN">Unix 文件类型及权限位的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Unix file type and mode code</span>
	 * <span class="zh-CN">Unix 文件类型及权限位</span>
	 */
	public int getUnixMode() {
		return this.unixMode;
	}

	/**
	 * <h3 class="en-US">Getter method for the dos file attributes</h3>
	 * <h3 class="zh-CN">Dos 文件属性的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Dos file attributes</span>
	 * <span class="zh-CN">Dos 文件属性</span>
	 */
	public int getDosAttributes() {
		return this.dosAttributes;
	}

	/**
	 * <h3 class="en-US">Getter method for the link target path</h3>
	 * <h3 class="zh-CN">符号链接指向的路径的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Link target path</span>
	 * <span class="zh-CN">符号链接指向的路径</span>
	 */
	public String getLinkTarget() {
		return this.linkTarget;
	}

	/**
	 * <h2 class="en-US">Archive entry builder</h2>
	 * <h2 class="zh-CN">归档条目构建器</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	public static final class Builder implements org.nervousync.builder.Builder<ArchiveEntry> {

		/**
		 * <span class="en-US">Entry path</span>
		 * <span class="zh-CN">条目路径</span>
		 */
		private String entryPath = Globals.DEFAULT_VALUE_STRING;
		/**
		 * <span class="en-US">Enumeration value of the entry type</span>
		 * <span class="zh-CN">条目类型枚举值</span>
		 */
		private EntryType entryType = EntryType.UNKNOWN;
		/**
		 * <span class="en-US">Original size</span>
		 * <span class="zh-CN">原始大小</span>
		 */
		private long uncompressedSize = Globals.DEFAULT_VALUE_LONG;
		/**
		 * <span class="en-US">Compressed size</span>
		 * <span class="zh-CN">压缩后大小</span>
		 */
		private long compressedSize = Globals.DEFAULT_VALUE_LONG;
		/**
		 * <span class="en-US">Last modified timestamp</span>
		 * <span class="zh-CN">最后修改时间戳</span>
		 */
		private Instant lastModified = Instant.EPOCH;
		/**
		 * <span class="en-US">Last access timestamp</span>
		 * <span class="zh-CN">最后访问时间</span>
		 */
		private Instant lastAccessTime = null;
		/**
		 * <span class="en-US">File create timestamp</span>
		 * <span class="zh-CN">文件创建时间</span>
		 */
		private Instant creationTime = null;
		/**
		 * <span class="en-US">Unix file type and mode code</span>
		 * <span class="zh-CN">Unix 文件类型及权限位</span>
		 */
		private int unixMode = Globals.DEFAULT_VALUE_INT;
		/**
		 * <span class="en-US">Dos file attributes</span>
		 * <span class="zh-CN">Dos 文件属性</span>
		 */
		private int dosAttributes = Globals.DEFAULT_VALUE_INT;
		/**
		 * <span class="en-US">Link target path</span>
		 * <span class="zh-CN">符号链接指向的路径</span>
		 */
		private String linkTarget = null;

		/**
		 * <h3 class="en-US">Private constructor method for the archive entry builder</h3>
		 * <h3 class="zh-CN">归档条目构建器的私有构造方法</h3>
		 */
		private Builder() {
		}

		/**
		 * <h3 class="en-US">Setting the entry path</h3>
		 * <h3 class="zh-CN">设置条目路径</h3>
		 *
		 * @param entryPath <span class="en-US">Entry path</span>
		 *                  <span class="zh-CN">条目路径</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder entryPath(@Nonnull final String entryPath) {
			Objects.requireNonNull(entryPath, "entryPath");
			this.entryPath = entryPath;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the enumeration value of the entry type</h3>
		 * <h3 class="zh-CN">设置条目类型枚举值</h3>
		 *
		 * @param entryType <span class="en-US">Enumeration value of the entry type</span>
		 *                  <span class="zh-CN">条目类型枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder entryType(@Nonnull final EntryType entryType) {
			Objects.requireNonNull(entryType, "entryType");
			this.entryType = entryType;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the original size</h3>
		 * <h3 class="zh-CN">设置原始大小</h3>
		 *
		 * @param uncompressedSize <span class="en-US">Original size</span>
		 *                         <span class="zh-CN">原始大小</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder uncompressedSize(final long uncompressedSize) {
			this.uncompressedSize = uncompressedSize;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the compressed size</h3>
		 * <h3 class="zh-CN">设置压缩后大小</h3>
		 *
		 * @param compressedSize <span class="en-US">Compressed size</span>
		 *                       <span class="zh-CN">压缩后大小</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder compressedSize(final long compressedSize) {
			this.compressedSize = compressedSize;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the last modified timestamp</h3>
		 * <h3 class="zh-CN">设置最后修改时间戳</h3>
		 *
		 * @param lastModified <span class="en-US">Last modified timestamp</span>
		 *                     <span class="zh-CN">最后修改时间戳</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder lastModified(final Instant lastModified) {
			this.lastModified = lastModified;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the last access timestamp</h3>
		 * <h3 class="zh-CN">设置最后访问时间戳</h3>
		 *
		 * @param lastAccessTime <span class="en-US">Last access timestamp</span>
		 *                       <span class="zh-CN">最后访问时间戳</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder lastAccess(final Instant lastAccessTime) {
			this.lastAccessTime = lastAccessTime;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the file creation timestamp</h3>
		 * <h3 class="zh-CN">设置文件创建时间戳</h3>
		 *
		 * @param creationTime <span class="en-US">File creation timestamp</span>
		 *                     <span class="zh-CN">文件创建时间戳</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder createTimestamp(final Instant creationTime) {
			this.creationTime = creationTime;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the unix file type and mode code</h3>
		 * <h3 class="zh-CN">设置 Unix 文件类型及权限位</h3>
		 *
		 * @param unixMode <span class="en-US">Unix file type and mode code</span>
		 *                 <span class="zh-CN">Unix 文件类型及权限位</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder unixMode(final int unixMode) {
			this.unixMode = unixMode;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the dos file attributes</h3>
		 * <h3 class="zh-CN">设置 Dos 文件属性</h3>
		 *
		 * @param dosAttributes <span class="en-US">Dos file attributes</span>
		 *                      <span class="zh-CN">Dos 文件属性</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder dosAttributes(final int dosAttributes) {
			this.dosAttributes = dosAttributes;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the link target path</h3>
		 * <h3 class="zh-CN">设置符号链接指向的路径</h3>
		 *
		 * @param linkTarget <span class="en-US">Link target path</span>
		 *                   <span class="zh-CN">符号链接指向的路径</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder linkTarget(final String linkTarget) {
			this.linkTarget = linkTarget;
			return this;
		}

		@Override
		public ArchiveEntry build() throws BuilderException {
			if (StringUtils.isBlank(this.entryPath)) {
				throw new BuilderException(0x0000001A0001L);
			}
			Objects.requireNonNull(this.lastModified, "lastModified");

			if (this.unixMode < -1 || this.unixMode > 0xFFFF) {
				throw new IllegalArgumentException("unixMode must be -1 or an unsigned 16-bit value");
			}

			if (this.dosAttributes < -1 || this.dosAttributes > 0xFF) {
				throw new IllegalArgumentException("dosAttributes must be -1 or an unsigned 8-bit value");
			}
			return new ArchiveEntry(this);
		}
	}
}
