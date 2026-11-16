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

import org.nervousync.archive.enumerations.StreamOwner;
import org.nervousync.archive.security.certificate.CertificateProvider;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.builder.ParentBuilder;
import org.nervousync.exceptions.builder.BuilderException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * <h2 class="en-US">Archive options</h2>
 * <h2 class="zh-CN">归档选项</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class ArchiveOptions {

	/**
	 * <span class="en-US">Charset of the entry name</span>
	 * <span class="zh-CN">条目名使用的字符集</span>
	 */
	private final Charset charset;
	/**
	 * <span class="en-US">Enumeration value of the stream owner</span>
	 * <span class="zh-CN">流所有者枚举值</span>
	 */
	private final StreamOwner streamOwner;
	/**
	 * <span class="en-US">Archive security limit</span>
	 * <span class="zh-CN">归档安全限制</span>
	 */
	private final ArchiveLimit archiveLimit;
	/**
	 * <span class="en-US">Password provider</span>
	 * <span class="zh-CN">密码提供适配器</span>
	 */
	private final PasswordProvider passwordProvider;
	/**
	 * <span class="en-US">Certificate provider</span>
	 * <span class="zh-CN">证书提供适配器</span>
	 */
	private final CertificateProvider certificateProvider;
	/**
	 * <span class="en-US">Verify flag</span>
	 * <span class="zh-CN">验证标记</span>
	 */
	private final boolean verify;
	/**
	 * <span class="en-US">Strict flag</span>
	 * <span class="zh-CN">严格标记</span>
	 */
	private final boolean strict;

	/**
	 * <h3 class="en-US">Constructor method for the abstract class of the archive options</h3>
	 * <h3 class="zh-CN">归档选项抽象类的构造方法</h3>
	 *
	 * @param builder <span class="en-US">Builder instance object</span>
	 *                <span class="zh-CN">构建器实例对象</span>
	 */
	private ArchiveOptions(final Builder builder) {
		this.charset = builder.charset;
		this.streamOwner = builder.streamOwner;
		this.archiveLimit = builder.archiveLimit;
		this.passwordProvider = builder.passwordProvider;
		this.certificateProvider = builder.certificateProvider;
		this.verify = builder.verify;
		this.strict = builder.strict;
	}

	/**
	 * <h3 class="en-US">Static method for initialize the archive options builder</h3>
	 * <h3 class="zh-CN">静态方法用于初始化归档选项构建器</h3>
	 *
	 * @return <span class="en-US">Archive options builder instance object</span>
	 * <span class="zh-CN">归档选项构建器实例对象</span>
	 */
	public static Builder newBuilder() {
		return new Builder(null);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the archive options builder</h3>
	 * <h3 class="zh-CN">静态方法用于初始化归档选项构建器</h3>
	 *
	 * @param options <span class="en-US">Archive options</span>
	 *                <span class="zh-CN">归档选项</span>
	 * @return <span class="en-US">Archive options builder instance object</span>
	 * <span class="zh-CN">归档选项构建器实例对象</span>
	 */
	public static Builder newBuilder(final ArchiveOptions options) {
		return new Builder(options);
	}

	/**
	 * <h3 class="en-US">Getter method for the charset of the entry name</h3>
	 * <h3 class="zh-CN">条目名使用的字符集的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Charset of the entry name</span>
	 * <span class="zh-CN">条目名使用的字符集</span>
	 */
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the stream owner</h3>
	 * <h3 class="zh-CN">流所有者枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the stream owner</span>
	 * <span class="zh-CN">流所有者枚举值</span>
	 */
	public StreamOwner getStreamOwner() {
		return this.streamOwner;
	}

	/**
	 * <h3 class="en-US">Getter method for the archive security limit</h3>
	 * <h3 class="zh-CN">归档安全限制的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Archive security limit</span>
	 * <span class="zh-CN">归档安全限制</span>
	 */
	public ArchiveLimit getArchiveLimit() {
		return this.archiveLimit;
	}

	/**
	 * <h3 class="en-US">Getter method for the password provider</h3>
	 * <h3 class="zh-CN">密码提供适配器的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Password provider</span>
	 * <span class="zh-CN">密码提供适配器</span>
	 */
	public PasswordProvider getPasswordProvider() {
		return this.passwordProvider;
	}

	/**
	 * <h3 class="en-US">Getter method for the certificate provider</h3>
	 * <h3 class="zh-CN">证书提供适配器的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Certificate provider</span>
	 * <span class="zh-CN">证书提供适配器</span>
	 */
	public CertificateProvider getCertificateProvider() {
		return this.certificateProvider;
	}

	/**
	 * <h3 class="en-US">Getter method for the verify flag</h3>
	 * <h3 class="zh-CN">验证标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Verify flag</span>
	 * <span class="zh-CN">验证标记</span>
	 */
	public boolean isVerify() {
		return this.verify;
	}

	/**
	 * <h3 class="en-US">Getter method for the strict flag</h3>
	 * <h3 class="zh-CN">严格标记的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Strict flag</span>
	 * <span class="zh-CN">严格标记</span>
	 */
	public boolean isStrict() {
		return this.strict;
	}

	/**
	 * <h2 class="en-US">Archive options builder</h2>
	 * <h2 class="zh-CN">归档选项构建器</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
	 */
	public static final class Builder extends ParentBuilder implements org.nervousync.builder.Builder<ArchiveOptions> {

		/**
		 * <span class="en-US">Charset of the entry name</span>
		 * <span class="zh-CN">条目名使用的字符集</span>
		 */
		private Charset charset = StandardCharsets.UTF_8;
		/**
		 * <span class="en-US">Enumeration value of the stream owner</span>
		 * <span class="zh-CN">流所有者枚举值</span>
		 */
		private StreamOwner streamOwner = StreamOwner.CALLER;
		/**
		 * <span class="en-US">Archive security limit</span>
		 * <span class="zh-CN">归档安全限制</span>
		 */
		private ArchiveLimit archiveLimit = ArchiveLimit.DEFAULT_LIMIT;
		/**
		 * <span class="en-US">Password provider</span>
		 * <span class="zh-CN">密码提供适配器</span>
		 */
		private PasswordProvider passwordProvider = PasswordProvider.none();
		/**
		 * <span class="en-US">Certificate provider</span>
		 * <span class="zh-CN">证书提供适配器</span>
		 */
		private CertificateProvider certificateProvider = CertificateProvider.none();
		/**
		 * <span class="en-US">Verify flag</span>
		 * <span class="zh-CN">验证标记</span>
		 */
		private boolean verify = Boolean.TRUE;
		/**
		 * <span class="en-US">Strict flag</span>
		 * <span class="zh-CN">严格标记</span>
		 */
		private boolean strict = Boolean.TRUE;

		/**
		 * <h3 class="en-US">Private constructor method for the archive options builder</h3>
		 * <h3 class="zh-CN">归档选项构建器的私有构造方法</h3>
		 *
		 * @param options <span class="en-US">Archive options</span>
		 *                <span class="zh-CN">归档选项</span>
		 */
		private Builder(final ArchiveOptions options) {
			if (options != null) {
				this.charset = options.charset;
				this.streamOwner = options.streamOwner;
				this.archiveLimit = options.archiveLimit;
				this.passwordProvider = options.passwordProvider;
				this.certificateProvider = options.certificateProvider;
				this.verify = options.verify;
				this.strict = options.strict;
			}
		}

		/**
		 * <h3 class="en-US">Setting the charset of the entry name</h3>
		 * <h3 class="zh-CN">设置条目名使用的字符集</h3>
		 *
		 * @param charset <span class="en-US">Charset of the entry name</span>
		 *                <span class="zh-CN">条目名使用的字符集</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder charset(final Charset charset) {
			Objects.requireNonNull(charset, "charset");
			this.charset = charset;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the stream owner</h3>
		 * <h3 class="zh-CN">设置流所有者</h3>
		 *
		 * @param streamOwner <span class="en-US">Enumeration value of the stream owner</span>
		 *                    <span class="zh-CN">流所有者枚举值</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder streamOwner(final StreamOwner streamOwner) {
			Objects.requireNonNull(streamOwner, "streamOwner");
			this.streamOwner = streamOwner;
			return this;
		}

		/**
		 * <h3 class="en-US">Initialize the archive security limit builder</h3>
		 * <h3 class="zh-CN">初始化归档安全限制构建器</h3>
		 *
		 * @return <span class="en-US">The archive security limit builder instance object</span>
		 * <span class="zh-CN">归档安全限制构建器实例对象</span>
		 */
		public ArchiveLimit.Builder<Builder> limitBuilder() {
			return ArchiveLimit.newBuilder(this, this.archiveLimit);
		}

		/**
		 * <h3 class="en-US">Setting the password provider</h3>
		 * <h3 class="zh-CN">设置密码提供器</h3>
		 *
		 * @param passwordProvider <span class="en-US">Password provider</span>
		 *                         <span class="zh-CN">密码提供器</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder passwordProvider(final PasswordProvider passwordProvider) {
			Objects.requireNonNull(passwordProvider, "passwordProvider");
			this.passwordProvider = passwordProvider;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the password provider</h3>
		 * <h3 class="zh-CN">设置密码提供器</h3>
		 *
		 * @param certificateProvider <span class="en-US">Password provider</span>
		 *                            <span class="zh-CN">密码提供器</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder certificateProvider(final CertificateProvider certificateProvider) {
			Objects.requireNonNull(certificateProvider, "certificateProvider");
			this.certificateProvider = certificateProvider;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the verify flag</h3>
		 * <h3 class="zh-CN">设置验证标记</h3>
		 *
		 * @param verify <span class="en-US">Verify flag</span>
		 *               <span class="zh-CN">验证标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder verify(final boolean verify) {
			this.verify = verify;
			return this;
		}

		/**
		 * <h3 class="en-US">Setting the strict flag</h3>
		 * <h3 class="zh-CN">设置严格标记</h3>
		 *
		 * @param strict <span class="en-US">Strict flag</span>
		 *               <span class="zh-CN">严格标记</span>
		 * @return <span class="en-US">Current builder instance object</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder strict(final boolean strict) {
			this.strict = strict;
			return this;
		}

		@Override
		public ArchiveOptions build() throws BuilderException {
			return new ArchiveOptions(this);
		}

		@Override
		protected void confirm(final Object object) throws BuilderException {
			if (object instanceof ArchiveLimit) {
				this.archiveLimit = (ArchiveLimit) object;
			}
		}
	}
}
