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

import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.CompressionFraming;

/**
 * <h2 class="en-US">Compression descriptor</h2>
 * <h2 class="zh-CN">压缩描述器</h2>
 *
 * @param algorithm <span class="en-US">Enumeration value of the compression algorithm</span>
 *                  <span class="zh-CN">压缩算法枚举值</span>
 * @param framing   <span class="en-US">Enumeration value of the compression framing</span>
 *                  <span class="zh-CN">压缩架构枚举值</span>
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public record CompressionDescriptor(CompressionAlgorithm algorithm, CompressionFraming framing) {

	/**
	 * <span class="en-US">Compression descriptor for .tar.gz</span>
	 * <span class="zh-CN">.tar.gz 压缩描述器</span>
	 */
	public static final CompressionDescriptor TAR_GZ =
			new CompressionDescriptor(CompressionAlgorithm.DEFLATE, CompressionFraming.GZIP);
	/**
	 * <span class="en-US">Compression descriptor for .tar.bz2</span>
	 * <span class="zh-CN">.tar.bz2 压缩描述器</span>
	 */
	public static final CompressionDescriptor TAR_BZ2 =
			new CompressionDescriptor(CompressionAlgorithm.BZIP2, CompressionFraming.BZIP2_STREAM);
	/**
	 * <span class="en-US">Compression descriptor for .tar.xz</span>
	 * <span class="zh-CN">.tar.xz 压缩描述器</span>
	 */
	public static final CompressionDescriptor TAR_XZ =
			new CompressionDescriptor(CompressionAlgorithm.LZMA2, CompressionFraming.XZ);
	/**
	 * <span class="en-US">Compression descriptor for .bz2</span>
	 * <span class="zh-CN">.bz2 压缩描述器</span>
	 */
	public static final CompressionDescriptor BZ2 =
			new CompressionDescriptor(CompressionAlgorithm.BZIP2, CompressionFraming.BZIP2_STREAM);
	/**
	 * <span class="en-US">Compression descriptor for .lzma</span>
	 * <span class="zh-CN">.lzma 压缩描述器</span>
	 */
	public static final CompressionDescriptor LZMA =
			new CompressionDescriptor(CompressionAlgorithm.LZMA1, CompressionFraming.LZMA_ALONE);

	/**
	 * <h3 class="en-US">Private constructor method for the compression descriptor</h3>
	 * <h3 class="zh-CN">压缩描述器的私有构造方法</h3>
	 *
	 * @param algorithm <span class="en-US">Enumeration value of the compression algorithm</span>
	 *                  <span class="zh-CN">压缩算法枚举值</span>
	 * @param framing   <span class="en-US">Enumeration value of the compression framing</span>
	 *                  <span class="zh-CN">压缩架构枚举值</span>
	 */
	public CompressionDescriptor {
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the compression algorithm</h3>
	 * <h3 class="zh-CN">压缩算法枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the compression algorithm</span>
	 * <span class="zh-CN">压缩算法枚举值</span>
	 */
	@Override
	public CompressionAlgorithm algorithm() {
		return this.algorithm;
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the compression framing</h3>
	 * <h3 class="zh-CN">压缩架构枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the compression framing</span>
	 * <span class="zh-CN">压缩架构枚举值</span>
	 */
	@Override
	public CompressionFraming framing() {
		return this.framing;
	}
}
