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

package org.nervousync.commons.collect;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * <h2 class="en-US">Immutable collection</h2>
 * <h2 class="zh-CN">不可变列表</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Apr 3, 2025 14:34:18 $
 */
public final class ImmutableCollection<E> extends AbstractCollection<E> implements Collection<E> {

	/**
	 * <h3 class="en-US">Private constructor for the immutable collection</h3>
	 * <h3 class="zh-CN">不可变列表的私有构造方法</h3>
	 *
	 * @param elements <span class="en-US">Data list</span>
	 *                 <span class="zh-CN">数据列表</span>
	 */
	private ImmutableCollection(@Nonnull final Collection<E> elements) {
		super(elements);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the immutable collection</h3>
	 * <h3 class="zh-CN">静态方法用于初始化不可变列表</h3>
	 *
	 * @param elements <span class="en-US">Data list</span>
	 *                 <span class="zh-CN">数据列表</span>
	 */
	public static <E> ImmutableCollection<E> of(@Nonnull final Collection<E> elements) {
		return new ImmutableCollection<>(elements);
	}
}
