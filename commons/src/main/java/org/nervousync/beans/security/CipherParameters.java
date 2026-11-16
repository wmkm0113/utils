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

package org.nervousync.beans.security;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;

import java.util.Objects;

/**
 * <h2 class="en-US">Cipher parameter define</h2>
 * <h2 class="zh-CN">加密解密参数定义</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 11:32:08 $
 */
public final class CipherParameters {

	/**
	 * <span class="en-US">Empty initialization vector data</span>
	 * <span class="zh-CN">无初始向量参数</span>
	 */
	public static final CipherParameters EMPTY = new CipherParameters(null, Globals.DEFAULT_VALUE_INT);

	/**
	 * <span class="en-US">Initialization vector data</span>
	 * <span class="zh-CN">初始向量数据</span>
	 */
	private final byte[] initializationVector;
	/**
	 * <span class="en-US">Effective key bits</span>
	 * <span class="zh-CN">有效密钥位数</span>
	 */
	private final int effectiveKeyBits;

	/**
	 * <h3 class="en-US">Private constructor method for the cipher parameter define</h3>
	 * <h3 class="zh-CN">加密解密参数定义的私有构造方法</h3>
	 *
	 * @param initializationVector <span class="en-US">Initialization vector data</span>
	 *                             <span class="zh-CN">初始向量数据</span>
	 * @param effectiveKeyBits     <span class="en-US">Effective key bits</span>
	 *                             <span class="zh-CN">有效密钥位数</span>
	 */
	private CipherParameters(final byte[] initializationVector, final int effectiveKeyBits) {
		this.initializationVector = (initializationVector == null) ? null : initializationVector.clone();
		this.effectiveKeyBits = effectiveKeyBits;
	}

	/**
	 * <h3 class="en-US">Static method for initialize the cipher parameter define</h3>
	 * <h3 class="zh-CN">静态方法用于初始化加密解密参数</h3>
	 *
	 * @param initializationVector <span class="en-US">Initialization vector data</span>
	 *                             <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Cipher parameter define instance object</span>
	 * <span class="zh-CN">加密解密参数实例对象</span>
	 */
	public static CipherParameters with(@Nonnull final byte[] initializationVector) {
		Objects.requireNonNull(initializationVector, "initializationVector cannot be null");
		if (initializationVector.length == 0) {
			throw new IllegalArgumentException("initializationVector cannot be empty");
		}
		return new CipherParameters(initializationVector, Globals.DEFAULT_VALUE_INT);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the cipher parameter define</h3>
	 * <h3 class="zh-CN">静态方法用于初始化加密解密参数</h3>
	 *
	 * @param initializationVector <span class="en-US">Initialization vector data</span>
	 *                             <span class="zh-CN">初始向量数据</span>
	 * @return <span class="en-US">Cipher parameter define instance object</span>
	 * <span class="zh-CN">加密解密参数实例对象</span>
	 */
	public static CipherParameters with(@Nonnull final byte[] initializationVector, final int effectiveKeyBits) {
		if (effectiveKeyBits <= 0) {
			throw new IllegalArgumentException("effectiveKeyBits must be positive");
		}
		Objects.requireNonNull(initializationVector, "initializationVector cannot be null");
		if (initializationVector.length == 0) {
			throw new IllegalArgumentException("initializationVector cannot be empty");
		}
		return new CipherParameters(initializationVector, effectiveKeyBits);
	}

	/**
	 * <h3 class="en-US">Getter method for the initialization vector data</h3>
	 * <h3 class="zh-CN">初始向量数据的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Initialization vector data</span>
	 * <span class="zh-CN">初始向量数据</span>
	 */
	public byte[] getInitializationVector() {
		return (this.initializationVector == null) ? new byte[0] : this.initializationVector.clone();
	}

	/**
	 * <h3 class="en-US">Getter method for the effective key bits</h3>
	 * <h3 class="zh-CN">有效密钥位数的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Effective key bits</span>
	 * <span class="zh-CN">有效密钥位数</span>
	 */
	public int getEffectiveKeyBits() {
		return this.effectiveKeyBits;
	}

	@Override
	public String toString() {
		return "CipherInitParameters{iv=" + (this.initializationVector == null
				? "absent"
				: "<redacted, length=" + this.initializationVector.length + ">")
				+ '}';
	}
}
