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

import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.enumerations.UnsafePolicy;
import org.nervousync.exceptions.builder.BuilderException;

import java.util.Objects;

/**
 * <h2 class="en-US">Extract options</h2>
 * <h2 class="zh-CN">解压选项</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ExtractOptions {

	/**
	 * <span class="en-US">Default options</span>
	 * <span class="zh-CN">默认选项</span>
	 */
	private static final ExtractOptions DEFAULT = newBuilder().build();

	/**
	 * <span class="en-US">Enumeration value of the override mode</span>
	 * <span class="zh-CN">覆盖模式枚举值</span>
	 */
	private final OverrideMode overrideMode;
	/**
	 * <span class="en-US">Enumeration value of the unsafe policy</span>
	 * <span class="zh-CN">不安全规则枚举值</span>
	 */
	private final UnsafePolicy unsafePolicy;
	/**
	 * <span class="en-US">Preserve file timestamp</span>
	 * <span class="zh-CN">处理文件时间戳</span>
	 */
	private final boolean preserveTimestamp;
	/**
	 * <span class="en-US">Preserve permissions</span>
	 * <span class="zh-CN">处理文件权限</span>
	 */
	private final boolean preservePermissions;
	/**
	 * <span class="en-US">Allow symbolic links flag</span>
	 * <span class="zh-CN">允许软连接标记</span>
	 */
	private final boolean allowSymbolicLinks;
	/**
	 * <span class="en-US">Allow hard links flag</span>
	 * <span class="zh-CN">允许硬连接标记</span>
	 */
	private final boolean allowHardLinks;
	/**
	 * <span class="en-US">Create directories flag</span>
	 * <span class="zh-CN">创建目录标记</span>
	 */
	private final boolean createDirectories;
	/**
	 * <span class="en-US">Continue on error flag</span>
	 * <span class="zh-CN">遇到错误仍然继续的标记</span>
	 */
	private final boolean continueOnError;

	/**
	 * <h2 class="en-US">Private constructor method for the extract options</h2>
	 * <h2 class="zh-CN">解压选项的私有构造方法</h2>
	 *
	 * @param builder <span class="en-US">Builder instance object</span>
	 *                <span class="zh-CN">构建器实例对象</span>
	 */
	private ExtractOptions(final Builder builder) {
		this.overrideMode = builder.overrideMode;
		this.unsafePolicy = builder.unsafePolicy;
		this.preserveTimestamp = builder.preserveLastModified;
		this.preservePermissions = builder.preservePermissions;
		this.allowSymbolicLinks = builder.allowSymbolicLinks;
		this.allowHardLinks = builder.allowHardLinks;
		this.createDirectories = builder.createDirectories;
		this.continueOnError = builder.continueOnError;
	}

	public static Builder newBuilder() {
		return newBuilder(DEFAULT);
	}

	public static Builder newBuilder(final ExtractOptions options) {
		return new Builder(options);
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the override mode</h3>
	 * <h3 class="zh-CN">覆盖模式枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the override mode</span>
	 * <span class="zh-CN">覆盖模式枚举值</span>
	 */
	public OverrideMode getOverrideMode() {
		return this.overrideMode;
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the unsafe policy</h3>
	 * <h3 class="zh-CN">不安全规则枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the unsafe policy</span>
	 * <span class="zh-CN">不安全规则枚举值</span>
	 */
	public UnsafePolicy getUnsafePolicy() {
		return this.unsafePolicy;
	}

	/**
	 * <h3 class="en-US">Getter method for the preserve last modified</h3>
	 * <h3 class="zh-CN">处理文件最后修改时间的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Preserve last modified</span>
	 * <span class="zh-CN">处理文件最后修改时间</span>
	 */
	public boolean isPreserveTimestamp() {
		return this.preserveTimestamp;
	}

	/**
	 * <h3 class="en-US">Getter method for the preserve permissions</h3>
	 * <h3 class="zh-CN">处理文件权限的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Preserve permissions</span>
	 * <span class="zh-CN">处理文件权限</span>
	 */
	public boolean isPreservePermissions() {
		return this.preservePermissions;
	}

	/**
	 * <h3 class="en-US">Getter method for the allow symbolic links flag</h3>
	 * <h3 class="zh-CN">允许软连接标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Allow symbolic links flag</span>
	 * <span class="zh-CN">允许软连接标记</span>
	 */
	public boolean isAllowSymbolicLinks() {
		return this.allowSymbolicLinks;
	}

	/**
	 * <h3 class="en-US">Getter method for the allow hard links flag</h3>
	 * <h3 class="zh-CN">允许硬连接标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Allow hard links flag</span>
	 * <span class="zh-CN">允许硬连接标记</span>
	 */
	public boolean isAllowHardLinks() {
		return this.allowHardLinks;
	}

	/**
	 * <h3 class="en-US">Getter method for the creation directories flag</h3>
	 * <h3 class="zh-CN">创建目录标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Create directories flag</span>
	 * <span class="zh-CN">创建目录标记</span>
	 */
	public boolean isCreateDirectories() {
		return this.createDirectories;
	}

	/**
	 * <h3 class="en-US">Getter method for the continue on error flag</h3>
	 * <h3 class="zh-CN">遇到错误仍然继续的标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Continue on error flag</span>
	 * <span class="zh-CN">遇到错误仍然继续的标记</span>
	 */
	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	/**
	 * <h2 class="en-US">Extract options builder</h2>
	 * <h2 class="zh-CN">解压选项构建器</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	public static final class Builder implements org.nervousync.builder.Builder<ExtractOptions> {

		/**
		 * <span class="en-US">Enumeration value of the override mode</span>
		 * <span class="zh-CN">覆盖模式枚举值</span>
		 */
		private OverrideMode overrideMode = OverrideMode.NEWER;
		/**
		 * <span class="en-US">Enumeration value of the unsafe policy</span>
		 * <span class="zh-CN">不安全规则枚举值</span>
		 */
		private UnsafePolicy unsafePolicy = UnsafePolicy.REJECT;
		/**
		 * <span class="en-US">Preserve last modified flag</span>
		 * <span class="zh-CN">处理文件最后修改时间标记</span>
		 */
		private boolean preserveLastModified = Boolean.TRUE;
		/**
		 * <span class="en-US">Preserve permissions flag</span>
		 * <span class="zh-CN">处理文件权限标记</span>
		 */
		private boolean preservePermissions = Boolean.FALSE;
		/**
		 * <span class="en-US">Allow symbolic links flag</span>
		 * <span class="zh-CN">允许软连接标记</span>
		 */
		private boolean allowSymbolicLinks = Boolean.FALSE;
		/**
		 * <span class="en-US">Allow hard links flag</span>
		 * <span class="zh-CN">允许硬连接标记</span>
		 */
		private boolean allowHardLinks = Boolean.FALSE;
		/**
		 * <span class="en-US">Create directories flag</span>
		 * <span class="zh-CN">创建目录标记</span>
		 */
		private boolean createDirectories = Boolean.TRUE;
		/**
		 * <span class="en-US">Continue on error flag</span>
		 * <span class="zh-CN">遇到错误仍然继续的标记</span>
		 */
		private boolean continueOnError = Boolean.FALSE;

		/**
		 * <h3 class="en-US">Private constructor method for the extract options builder</h3>
		 * <h3 class="zh-CN">解压选项构建器的私有构造方法</h3>
		 *
		 * @param options <span class="en-US">Extract options</span>
		 *                <span class="zh-CN">解压选项</span>
		 */
		private Builder(final ExtractOptions options) {
			if (options != null) {
				this.overrideMode = options.overrideMode;
				this.unsafePolicy = options.unsafePolicy;
				this.preserveLastModified = options.preserveTimestamp;
				this.preservePermissions = options.preservePermissions;
				this.allowSymbolicLinks = options.allowSymbolicLinks;
				this.allowHardLinks = options.allowHardLinks;
				this.createDirectories = options.createDirectories;
				this.continueOnError = options.continueOnError;
			}
		}

		/**
		 * <h3 class="en-US">Setting the override mode</h3>
		 * <h3 class="zh-CN">设置覆盖模式</h3>
		 *
		 * @param overrideMode <span class="en-US">Enumeration value of the override mode</span>
		 *                     <span class="zh-CN">覆盖模式枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder overrideMode(final OverrideMode overrideMode) {
			Objects.requireNonNull(overrideMode, "overrideMode cannot be null");
			this.overrideMode = overrideMode;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the unsafe policy</h3>
		 * <h3 class="zh-CN">设置不安全规则</h3>
		 *
		 * @param unsafePolicy <span class="en-US">Enumeration value of the unsafe policy</span>
		 *                     <span class="zh-CN">不安全规则枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder unsafePolicy(final UnsafePolicy unsafePolicy) {
			Objects.requireNonNull(unsafePolicy, "unsafePolicy cannot be null");
			this.unsafePolicy = unsafePolicy;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the preserve last modified flag</h3>
		 * <h3 class="zh-CN">设置处理文件最后修改时间标记</h3>
		 *
		 * @param preserveLastModified <span class="en-US">Preserve last modified</span>
		 *                             <span class="zh-CN">处理文件最后修改时间</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder preserveLastModified(final boolean preserveLastModified) {
			this.preserveLastModified = preserveLastModified;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the preserve permissions flag</h3>
		 * <h3 class="zh-CN">设置处理文件权限标记</h3>
		 *
		 * @param preservePermissions <span class="en-US">Preserve permissions flag</span>
		 *                            <span class="zh-CN">处理文件权限标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder preservePermissions(final boolean preservePermissions) {
			this.preservePermissions = preservePermissions;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the allow symbolic links flag</h3>
		 * <h3 class="zh-CN">设置允许软连接标记</h3>
		 *
		 * @param allowSymbolicLinks <span class="en-US">Allow symbolic links flag</span>
		 *                           <span class="zh-CN">允许软连接标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder allowSymbolicLinks(final boolean allowSymbolicLinks) {
			this.allowSymbolicLinks = allowSymbolicLinks;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the allow hard links flag</h3>
		 * <h3 class="zh-CN">设置允许硬连接标记</h3>
		 *
		 * @param allowHardLinks <span class="en-US">Allow hard links flag</span>
		 *                       <span class="zh-CN">允许硬连接标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder allowHardLinks(final boolean allowHardLinks) {
			this.allowHardLinks = allowHardLinks;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the creation directories flag</h3>
		 * <h3 class="zh-CN">设置创建目录标记</h3>
		 *
		 * @param createDirectories <span class="en-US">Create directories flag</span>
		 *                          <span class="zh-CN">创建目录标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder createDirectories(final boolean createDirectories) {
			this.createDirectories = createDirectories;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the continue on error flag</h3>
		 * <h3 class="zh-CN">设置遇到错误仍然继续的标记</h3>
		 *
		 * @param continueOnError <span class="en-US">Continue on error flag</span>
		 *                        <span class="zh-CN">遇到错误仍然继续的标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder continueOnError(final boolean continueOnError) {
			this.continueOnError = continueOnError;
			return this;
		}

		@Override
		public ExtractOptions build() throws BuilderException {
			return new ExtractOptions(this);
		}
	}
}
