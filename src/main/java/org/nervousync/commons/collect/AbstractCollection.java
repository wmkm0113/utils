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
import org.nervousync.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <h2 class="en-US">Abstract class of immutable collection</h2>
 * <h2 class="zh-CN">不可变列表抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Apr 3, 2025 14:30:38 $
 */
@SuppressWarnings("unused")
public abstract class AbstractCollection<E> implements Collection<E> {

	/**
	 * <span class="en-US">Data list</span>
	 * <span class="zh-CN">数据列表</span>
	 */
	protected final Collection<E> elements;
	/**
	 * <span class="en-US">Data list length</span>
	 * <span class="zh-CN">列表长度</span>
	 */
	protected final int size;

	/**
	 * <h3 class="en-US">Constructor method for the abstract class of immutable collection</h3>
	 * <h3 class="zh-CN">不可变列表抽象类的构造方法</h3>
	 *
	 * @param elements <span class="en-US">Data list</span>
	 *                 <span class="zh-CN">数据列表</span>
	 */
	protected AbstractCollection(@Nonnull final Collection<E> elements) {
		this.elements = List.copyOf(elements);
		this.size = elements.size();
	}

	@Override
	public final int size() {
		return this.size;
	}

	@Override
	public final boolean isEmpty() {
		return this.size == 0;
	}

	@Override
	public final boolean contains(final Object o) {
		return this.elements.contains(o);
	}

	@Override
	public final @Nonnull Object[] toArray() {
		return this.elements.toArray();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final @Nonnull <T> T[] toArray(@Nonnull final T[] a) {
		Class<T> clazz = (Class<T>) a.getClass().getComponentType();
		List<T> collection = new ArrayList<>();
		for (E element : this.elements) {
			try {
				collection.add(clazz.cast(element));
			} catch (ClassCastException ignored) {
			}
		}
		return CollectionUtils.toArray(collection);
	}

	@Override
	public final @Nonnull Iterator<E> iterator() {
		return new ImmutableIterator<>(this.elements.iterator());
	}

	@Override
	public final boolean add(final E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean remove(final Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean containsAll(@Nonnull final Collection<?> c) {
		return this.elements.containsAll(c);
	}

	@Override
	public final boolean addAll(@Nonnull final Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean retainAll(@Nonnull final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean removeAll(@Nonnull final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void clear() {
		throw new UnsupportedOperationException();
	}
}
