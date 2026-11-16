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

package org.nervousync.archive.options;

import jakarta.annotation.Nonnull;
import org.nervousync.builder.AbstractBuilder;
import org.nervousync.builder.ParentBuilder;
import org.nervousync.exceptions.builder.BuilderException;

import java.util.Objects;

/**
 * <h2 class="en-US">Archive security limit</h2>
 * <h2 class="zh-CN">归档安全限制</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ArchiveLimit {

	/**
	 * <span class="en-US">Default value of archive security limit</span>
	 * <span class="zh-CN">归档安全限制默认值</span>
	 */
	public static final ArchiveLimit DEFAULT_LIMIT =
			new ArchiveLimit(1_000_000L, 8L * 1024L * 1024L * 1024L,
					64L * 1024L * 1024L * 1024L, 64L * 1024L * 1024L * 1024L,
					256L * 1024L * 1024L, 1000.0D, 8, 4096);

	/**
	 * <span class="en-US">Maximum entries count</span>
	 * <span class="zh-CN">最大条目数量</span>
	 */
	private final long maxEntries;
	/**
	 * <span class="en-US">Maximum uncompressed size of archive entry</span>
	 * <span class="zh-CN">条目最大字节数</span>
	 */
	private final long maxEntryBytes;
	/**
	 * <span class="en-US">Maximum uncompressed size of total entry</span>
	 * <span class="zh-CN">总计最大字节数</span>
	 */
	private final long maxTotalBytes;
	/**
	 * <span class="en-US">Maximum bytes size of the archive input</span>
	 * <span class="zh-CN">最大输入字节数</span>
	 */
	private final long maxCompressedBytes;
	/**
	 * <span class="en-US">Maximum bytes size of archive header</span>
	 * <span class="zh-CN">最大头字节数</span>
	 */
	private final long maxHeaderBytes;
	/**
	 * <span class="en-US">Maximum compression ratio</span>
	 * <span class="zh-CN">最大压缩比例</span>
	 */
	private final double maxCompressionRatio;
	/**
	 * <span class="en-US">Maximum nesting depth</span>
	 * <span class="zh-CN">最大目录深度</span>
	 */
	private final int maxNestingDepth;
	/**
	 * <span class="en-US">Maximum character size of archive entry name</span>
	 * <span class="zh-CN">最大条目名称字符数</span>
	 */
	private final int maxNameCharacters;

	/**
	 * <h3 class="en-US">Private constructor method for the archive security limit</h3>
	 * <h3 class="zh-CN">归档安全限制的私有构造方法</h3>
	 *
	 * @param maxEntries          <span class="en-US">Maximum entries count</span>
	 *                            <span class="zh-CN">最大条目数量</span>
	 * @param maxEntryBytes       <span class="en-US">Maximum uncompressed size of archive entry</span>
	 *                            <span class="zh-CN">条目最大字节数</span>
	 * @param maxTotalBytes       <span class="en-US">Maximum uncompressed size of total entry</span>
	 *                            <span class="zh-CN">总计最大字节数</span>
	 * @param maxCompressedBytes  <span class="en-US">Maximum bytes size of the archive input</span>
	 *                            <span class="zh-CN">最大输入字节数</span>
	 * @param maxHeaderBytes      <span class="en-US">Maximum bytes size of archive header</span>
	 *                            <span class="zh-CN">最大头字节数</span>
	 * @param maxCompressionRatio <span class="en-US">Maximum compression ratio</span>
	 *                            <span class="zh-CN">最大压缩比例</span>
	 * @param maxNestingDepth     <span class="en-US">Maximum nesting depth</span>
	 *                            <span class="zh-CN">最大目录深度</span>
	 * @param maxNameCharacters   <span class="en-US">Maximum character size of archive entry name</span>
	 *                            <span class="zh-CN">最大条目名称字符数</span>
	 */
	private ArchiveLimit(final long maxEntries, final long maxEntryBytes, final long maxTotalBytes,
	                     final long maxCompressedBytes, final long maxHeaderBytes, final double maxCompressionRatio,
	                     final int maxNestingDepth, final int maxNameCharacters) {
		this.maxEntries = maxEntries;
		this.maxEntryBytes = maxEntryBytes;
		this.maxTotalBytes = maxTotalBytes;
		this.maxCompressedBytes = maxCompressedBytes;
		this.maxHeaderBytes = maxHeaderBytes;
		this.maxCompressionRatio = maxCompressionRatio;
		this.maxNestingDepth = maxNestingDepth;
		this.maxNameCharacters = maxNameCharacters;
	}

	/**
	 * <h3 class="en-US">Merge the current archive security restrictions with the provided archive security restrictions, taking the more restrictive value for each field.</h3>
	 * <h3 class="zh-CN">合并当前归档安全限制和给定的归档安全限制，所有字段都取更严格的值</h3>
	 *
	 * @param archiveLimit <span class="en-US">Archive limit instance object</span>
	 *                     <span class="zh-CN">归档安全限制实例对象</span>
	 * @return <span class="en-US">Merged archive limit instance object</span>
	 * <span class="zh-CN">合并后的归档安全限制实例对象</span>
	 */
	public ArchiveLimit constrainWith(final ArchiveLimit archiveLimit) {
		return newBuilder()
				.maxEntries(Math.min(this.maxEntries, archiveLimit.maxEntries))
				.maxEntryBytes(Math.min(this.maxEntryBytes, archiveLimit.maxEntryBytes))
				.maxTotalBytes(Math.min(this.maxTotalBytes, archiveLimit.maxTotalBytes))
				.maxCompressedBytes(Math.min(this.maxCompressedBytes, archiveLimit.maxCompressedBytes))
				.maxHeaderBytes(Math.min(this.maxHeaderBytes, archiveLimit.maxHeaderBytes))
				.maxCompressionRatio(Math.min(this.maxCompressionRatio, archiveLimit.maxCompressionRatio))
				.maxNestingDepth(Math.min(this.maxNestingDepth, archiveLimit.maxNestingDepth))
				.maxNameCharacters(Math.min(this.maxNameCharacters, archiveLimit.maxNameCharacters))
				.build();
	}

	/**
	 * <h3 class="en-US">Static method for initialize the archive limit builder instance object</h3>
	 * <h3 class="zh-CN">静态方法用于初始化构建器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized archive limit builder instance object</span>
	 * <span class="zh-CN">初始化的构建器实例对象</span>
	 */
	public static Builder<?> newBuilder() {
		return newBuilder(null, DEFAULT_LIMIT);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the archive limit builder instance object</h3>
	 * <h3 class="zh-CN">静态方法用于初始化构建器实例对象</h3>
	 *
	 * @param archiveLimit <span class="en-US">Archive limit instance object</span>
	 *                     <span class="zh-CN">归档安全限制实例对象</span>
	 * @return <span class="en-US">Initialized archive limit builder instance object</span>
	 * <span class="zh-CN">初始化的构建器实例对象</span>
	 */
	public static <P extends ParentBuilder> Builder<P> newBuilder(final P parentBuilder, @Nonnull final ArchiveLimit archiveLimit) {
		return new Builder<>(parentBuilder, archiveLimit);
	}

	/**
	 * <h3 class="en-US">Getter method for the maximum entries count</h3>
	 * <h3 class="zh-CN">最大条目数量的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum entries count</span>
	 * <span class="zh-CN">最大条目数量</span>
	 */
	public long getMaxEntries() {
		return this.maxEntries;
	}

	/**
	 * <h3 class="en-US">Getter method for the maximum uncompressed size of archive entry</h3>
	 * <h3 class="zh-CN">条目最大字节数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum uncompressed size of archive entry</span>
	 * <span class="zh-CN">条目最大字节数</span>
	 */
	public long getMaxEntryBytes() {
		return this.maxEntryBytes;
	}

	/**
	 * <h3 class="en-US">Getter method for the maximum uncompressed size of total entry</h3>
	 * <h3 class="zh-CN">总计最大字节数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum uncompressed size of total entry</span>
	 * <span class="zh-CN">总计最大字节数</span>
	 */
	public long getMaxTotalBytes() {
		return this.maxTotalBytes;
	}

	/**
	 * <h3 class="en-US">Getter method for </h3>
	 * <h3 class="zh-CN">的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum bytes size of the archive input</span>
	 * <span class="zh-CN">最大输入字节数</span>
	 */
	public long getMaxCompressedBytes() {
		return this.maxCompressedBytes;
	}

	/**
	 * <h3 class="en-US">Getter method for the maximum bytes size of archive header</h3>
	 * <h3 class="zh-CN">最大头字节数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum bytes size of archive header</span>
	 * <span class="zh-CN">最大头字节数</span>
	 */
	public long getMaxHeaderBytes() {
		return this.maxHeaderBytes;
	}

	/**
	 * <h3 class="en-US">Getter method for the maximum compression ratio</h3>
	 * <h3 class="zh-CN">最大压缩比例的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum compression ratio</span>
	 * <span class="zh-CN">最大压缩比例</span>
	 */
	public double getMaxCompressionRatio() {
		return this.maxCompressionRatio;
	}

	/**
	 * <h3 class="en-US">Getter method for the maximum nesting depth</h3>
	 * <h3 class="zh-CN">最大目录深度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum nesting depth</span>
	 * <span class="zh-CN">最大目录深度</span>
	 */
	public int getMaxNestingDepth() {
		return this.maxNestingDepth;
	}

	/**
	 * <h3 class="en-US">Getter method for the maximum character size of archive entry name</h3>
	 * <h3 class="zh-CN">最大条目名称字符数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Maximum character size of archive entry name</span>
	 * <span class="zh-CN">最大条目名称字符数</span>
	 */
	public int getMaxNameCharacters() {
		return this.maxNameCharacters;
	}

	/**
	 * <h2 class="en-US">Archive security limit builder</h2>
	 * <h2 class="zh-CN">归档安全限制构建器</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	public static final class Builder<P extends ParentBuilder> extends AbstractBuilder<P, ArchiveLimit> {

		/**
		 * <span class="en-US">Maximum entries count</span>
		 * <span class="zh-CN">最大条目数量</span>
		 */
		private long maxEntries;
		/**
		 * <span class="en-US">Maximum uncompressed size of archive entry</span>
		 * <span class="zh-CN">条目最大字节数</span>
		 */
		private long maxEntryBytes;
		/**
		 * <span class="en-US">Maximum uncompressed size of total entry</span>
		 * <span class="zh-CN">总计最大字节数</span>
		 */
		private long maxTotalBytes;
		/**
		 * <span class="en-US">Maximum bytes size of the archive input</span>
		 * <span class="zh-CN">最大输入字节数</span>
		 */
		private long maxCompressedBytes;
		/**
		 * <span class="en-US">Maximum bytes size of archive header</span>
		 * <span class="zh-CN">最大头字节数</span>
		 */
		private long maxHeaderBytes;
		/**
		 * <span class="en-US">Maximum compression ratio</span>
		 * <span class="zh-CN">最大压缩比例</span>
		 */
		private double maxCompressionRatio;
		/**
		 * <span class="en-US">Maximum nesting depth</span>
		 * <span class="zh-CN">最大目录深度</span>
		 */
		private int maxNestingDepth;
		/**
		 * <span class="en-US">Maximum character size of archive entry name</span>
		 * <span class="zh-CN">最大条目名称字符数</span>
		 */
		private int maxNameCharacters;

		/**
		 * <h3 class="en-US">Private constructor method for the archive security limit builder</h3>
		 * <h3 class="zh-CN">归档安全限制构建器的私有构造方法</h3>
		 *
		 * @param archiveLimit <span class="en-US">Archive limit instance object</span>
		 *                     <span class="zh-CN">归档安全限制实例对象</span>
		 */
		private Builder(final P parentBuilder, @Nonnull final ArchiveLimit archiveLimit) {
			super(parentBuilder);
			Objects.requireNonNull(archiveLimit, "archiveLimit cannot be null");
			this.maxEntries = archiveLimit.maxEntries;
			this.maxEntryBytes = archiveLimit.maxEntryBytes;
			this.maxTotalBytes = archiveLimit.maxTotalBytes;
			this.maxCompressedBytes = archiveLimit.maxCompressedBytes;
			this.maxHeaderBytes = archiveLimit.maxHeaderBytes;
			this.maxCompressionRatio = archiveLimit.maxCompressionRatio;
			this.maxNestingDepth = archiveLimit.maxNestingDepth;
			this.maxNameCharacters = archiveLimit.maxNameCharacters;
		}

		/**
		 * <h3 class="en-US">Setting the maximum entries count</h3>
		 * <h3 class="zh-CN">设置最大条目数量</h3>
		 *
		 * @param value <span class="en-US">Maximum entries count</span>
		 *              <span class="zh-CN">最大条目数量</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<P> maxEntries(final long value) {
			this.maxEntries = requirePositive(value, "maxEntryCount");
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the maximum uncompressed size of archive entry</h3>
		 * <h3 class="zh-CN">设置条目最大字节数</h3>
		 *
		 * @param value <span class="en-US">Maximum uncompressed size of archive entry</span>
		 *              <span class="zh-CN">条目最大字节数</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<P> maxEntryBytes(final long value) {
			this.maxEntryBytes = requirePositive(value, "maxEntryUncompressedBytes");
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the maximum uncompressed size of total entry</h3>
		 * <h3 class="zh-CN">设置总计最大字节数</h3>
		 *
		 * @param value <span class="en-US">Maximum uncompressed size of total entry</span>
		 *              <span class="zh-CN">总计最大字节数</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<P> maxTotalBytes(
				final long value) {
			this.maxTotalBytes = requirePositive(value, "maxTotalUncompressedBytes");
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the maximum bytes size of the archive input</h3>
		 * <h3 class="zh-CN">设置最大输入字节数</h3>
		 *
		 * @param value <span class="en-US">Maximum bytes size of the archive input</span>
		 *              <span class="zh-CN">最大输入字节数</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<P> maxCompressedBytes(
				final long value) {
			this.maxCompressedBytes = requirePositive(value, "maxCompressedInputBytes");
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the maximum bytes size of archive header</h3>
		 * <h3 class="zh-CN">设置最大头字节数</h3>
		 *
		 * @param value <span class="en-US">Maximum bytes size of archive header</span>
		 *              <span class="zh-CN">最大头字节数</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<P> maxHeaderBytes(final long value) {
			this.maxHeaderBytes = requirePositive(value, "maxHeaderBytes");
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the maximum compression ratio</h3>
		 * <h3 class="zh-CN">设置最大压缩比例</h3>
		 *
		 * @param value <span class="en-US">Maximum compression ratio</span>
		 *              <span class="zh-CN">最大压缩比例</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<P> maxCompressionRatio(final double value) {
			if (Double.isNaN(value) || value <= 0.0D) {
				throw new IllegalArgumentException("maxCompressionRatio must be positive");
			}
			this.maxCompressionRatio = value;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the maximum nesting depth</h3>
		 * <h3 class="zh-CN">设置最大目录深度</h3>
		 *
		 * @param value <span class="en-US">Maximum nesting depth</span>
		 *              <span class="zh-CN">最大目录深度</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<P> maxNestingDepth(final int value) {
			if (value < 0) {
				throw new IllegalArgumentException("maxNestingDepth must not be negative");
			}
			this.maxNestingDepth = value;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the maximum character size of archive entry name</h3>
		 * <h3 class="zh-CN">设置最大条目名称字符数</h3>
		 *
		 * @param value <span class="en-US">Maximum character size of archive entry name</span>
		 *              <span class="zh-CN">最大条目名称字符数</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<P> maxNameCharacters(final int value) {
			if (value <= 0) {
				throw new IllegalArgumentException("maxNameCharacters must be positive");
			}
			this.maxNameCharacters = value;
			return this;
		}

		@Override
		public ArchiveLimit build() throws BuilderException {
			return new ArchiveLimit(this.maxEntries, this.maxEntryBytes, this.maxTotalBytes, this.maxCompressedBytes,
					this.maxHeaderBytes, this.maxCompressionRatio, this.maxNestingDepth, this.maxNameCharacters);
		}
	}

	private static long requirePositive(final long value, final String name) {
		if (value <= 0L) {
			throw new IllegalArgumentException(name + " must be positive");
		}
		return value;
	}
}
