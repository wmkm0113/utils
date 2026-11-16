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
import org.nervousync.archive.enumerations.RewriteStrategy;
import org.nervousync.archive.enumerations.VerificationMode;
import org.nervousync.exceptions.builder.BuilderException;

import java.util.Objects;

/**
 * <h2 class="en-US">Writable options</h2>
 * <h2 class="zh-CN">写入选项</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class WritableOptions {

	/**
	 * <span class="en-US">Automatic commit flag</span>
	 * <span class="zh-CN">自动提交标记</span>
	 */
	private final boolean autoCommit;
	/**
	 * <span class="en-US">Rollback on failure flag</span>
	 * <span class="zh-CN">错误时回滚标记</span>
	 */
	private final boolean rollbackFailure;
	/**
	 * <span class="en-US">Atomic replace flag</span>
	 * <span class="zh-CN">原子替换标记</span>
	 */
	private final boolean atomicReplace;
	/**
	 * <span class="en-US">Enumeration value of the rewrite strategy</span>
	 * <span class="zh-CN">重写策略枚举值</span>
	 */
	private final RewriteStrategy rewriteStrategy;
	/**
	 * <span class="en-US">Enumeration value of the verification mode</span>
	 * <span class="zh-CN">验证模式枚举值</span>
	 */
	private final VerificationMode verificationMode;

	/**
	 * <h3 class="en-US">Private constructor method for the writable options</h3>
	 * <h3 class="zh-CN">写入选项的私有构造方法</h3>
	 *
	 * @param builder <span class="en-US">Builder instance object</span>
	 *                <span class="zh-CN">构建器实例对象</span>
	 */
	private WritableOptions(@Nonnull final Builder builder) {
		this.autoCommit = builder.autoCommit;
		this.rollbackFailure = builder.rollbackFailure;
		this.atomicReplace = builder.atomicReplace;
		this.rewriteStrategy = builder.rewriteStrategy;
		this.verificationMode = builder.verificationMode;
	}

	/**
	 * <h2 class="en-US">Writable options builder</h2>
	 * <h2 class="zh-CN">写入选项构建器</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	public static final class Builder implements org.nervousync.builder.Builder<WritableOptions> {

		/**
		 * <span class="en-US">Automatic commit flag</span>
		 * <span class="zh-CN">自动提交标记</span>
		 */
		private boolean autoCommit = Boolean.FALSE;
		/**
		 * <span class="en-US">Rollback on failure flag</span>
		 * <span class="zh-CN">错误时回滚标记</span>
		 */
		private boolean rollbackFailure = Boolean.TRUE;
		/**
		 * <span class="en-US">Atomic replace flag</span>
		 * <span class="zh-CN">原子替换标记</span>
		 */
		private boolean atomicReplace = Boolean.TRUE;
		/**
		 * <span class="en-US">Enumeration value of the rewrite strategy</span>
		 * <span class="zh-CN">重写策略枚举值</span>
		 */
		private RewriteStrategy rewriteStrategy = RewriteStrategy.FULL;
		/**
		 * <span class="en-US">Enumeration value of the verification mode</span>
		 * <span class="zh-CN">验证模式枚举值</span>
		 */
		private VerificationMode verificationMode = VerificationMode.FULL;

		/**
		 * <h3 class="en-US">Private constructor method for the writable options builder</h3>
		 * <h3 class="zh-CN">写入选项构建器的私有构造方法</h3>
		 *
		 * @param options <span class="en-US">Writable options</span>
		 *                <span class="zh-CN">写入选项</span>
		 */
		private Builder(final WritableOptions options) {
			if (options != null) {
				this.autoCommit = options.autoCommit;
				this.rollbackFailure = options.rollbackFailure;
				this.atomicReplace = options.atomicReplace;
				this.rewriteStrategy = options.rewriteStrategy;
				this.verificationMode = options.verificationMode;
			}
		}

		/**
		 * <h3 class="en-US">Setting the automatic commit flag</h3>
		 * <h3 class="zh-CN">设置自动提交标记</h3>
		 *
		 * @param autoCommit <span class="en-US">Automatic commit flag</span>
		 *                   <span class="zh-CN">自动提交标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder autoCommit(final boolean autoCommit) {
			this.autoCommit = autoCommit;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the rollback on failure flag</h3>
		 * <h3 class="zh-CN">设置错误时回滚标记</h3>
		 *
		 * @param rollbackFailure <span class="en-US">Rollback on failure flag</span>
		 *                        <span class="zh-CN">错误时回滚标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder rollbackFailure(final boolean rollbackFailure) {
			this.rollbackFailure = rollbackFailure;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the atomic replacement flag</h3>
		 * <h3 class="zh-CN">设置原子替换标记</h3>
		 *
		 * @param atomicReplace <span class="en-US">Atomic replace flag</span>
		 *                      <span class="zh-CN">原子替换标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder atomicReplace(final boolean atomicReplace) {
			this.atomicReplace = atomicReplace;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the rewrite strategy</h3>
		 * <h3 class="zh-CN">设置重写策略</h3>
		 *
		 * @param rewriteStrategy <span class="en-US">Enumeration value of the rewrite strategy</span>
		 *                        <span class="zh-CN">重写策略枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder rewriteStrategy(final RewriteStrategy rewriteStrategy) {
			Objects.requireNonNull(rewriteStrategy, "rewriteStrategy");
			this.rewriteStrategy = rewriteStrategy;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the verification mode</h3>
		 * <h3 class="zh-CN">设置验证模式</h3>
		 *
		 * @param verificationMode <span class="en-US">Enumeration value of the verification mode</span>
		 *                         <span class="zh-CN">验证模式枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder verificationMode(final VerificationMode verificationMode) {
			Objects.requireNonNull(verificationMode, "verificationMode");
			this.verificationMode = verificationMode;
			return this;
		}

		@Override
		public WritableOptions build() throws BuilderException {
			return new WritableOptions(this);
		}
	}

}
