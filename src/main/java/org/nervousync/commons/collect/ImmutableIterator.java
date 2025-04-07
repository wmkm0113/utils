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

import java.util.Iterator;

/**
 * <h2 class="en-US">Immutable iterator</h2>
 * <h2 class="zh-CN">不可变遍历器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Apr 3, 2025 14:39:52 $
 */
public final class ImmutableIterator<E> implements Iterator<E> {

	/**
	 * <span class="en-US">Original iterator object</span>
	 * <span class="zh-CN">原始遍历器对象</span>
	 */
	private final Iterator<E> delegate;

	/**
	 * <h3 class="en-US">Constructor method for the immutable iterator</h3>
	 * <h3 class="zh-CN">不可变遍历器的私有构造方法</h3>
	 *
	 * @param delegate <span class="en-US">Original iterator object</span>
	 *                 <span class="zh-CN">原始遍历器对象</span>
	 */
	ImmutableIterator(final Iterator<E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean hasNext() {
		return this.delegate.hasNext();
	}

	@Override
	public E next() {
		return this.delegate.next();
	}
}
