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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nervousync.utils.ObjectUtils;

import java.io.Serializable;
import java.util.*;

/**
 * <h2 class="en-US">Immutable mapping</h2>
 * <h2 class="zh-CN">不可变映射表</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Apr 3, 2025 14:27:28 $
 */
@SuppressWarnings("unused")
public final class ImmutableMap<K, V> implements Map<K, V>, Serializable {

	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = 7539339429172583303L;

	/**
	 * <span class="en-US">Data list of key-value pair</span>
	 * <span class="zh-CN">键值对列表</span>
	 */
	private transient final List<Entry<K, V>> entries;
	/**
	 * <span class="en-US">Key data list</span>
	 * <span class="zh-CN">数据键列表</span>
	 */
	private transient final Set<K> keySet;
	/**
	 * <span class="en-US">Value data list</span>
	 * <span class="zh-CN">数据值列表</span>
	 */
	private transient final Collection<V> values;
	/**
	 * <span class="en-US">Length of data list</span>
	 * <span class="zh-CN">列表长度</span>
	 */
	private transient final int size;

	/**
	 * <h3 class="en-US">Private constructor for immutable mapping</h3>
	 * <h3 class="zh-CN">不可变映射表的私有构造方法</h3>
	 *
	 * @param entries <span class="en-US">Data list of key-value pair</span>
	 *                <span class="zh-CN">键值对列表</span>
	 */
	private ImmutableMap(@Nonnull final List<Entry<K, V>> entries) {
		this.entries = entries;
		this.size = this.entries.size();
		this.keySet = new HashSet<>(this.size);
		this.values = new ArrayList<>(this.size);
		this.entries.forEach(entry -> {
			this.keySet.add(entry.getKey());
			this.values.add(entry.getValue());
		});
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public boolean isEmpty() {
		return this.size == 0;
	}

	@Override
	public boolean containsKey(final Object key) {
		return entrySet().stream().anyMatch(entry -> ObjectUtils.nullSafeEquals(entry.getKey(), key));
	}

	@Override
	public boolean containsValue(final Object value) {
		return entrySet().stream().anyMatch(entry -> ObjectUtils.nullSafeEquals(entry.getValue(), value));
	}

	@Override
	public V get(final Object key) {
		return entrySet()
				.stream()
				.filter(entry -> ObjectUtils.nullSafeEquals(entry.getKey(), key))
				.findFirst()
				.map(Entry::getValue)
				.orElse(null);
	}

	@Override
	public @Nullable V put(final K key, final V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(final Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(@NotNull final Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull Set<K> keySet() {
		return ImmutableSet.of(this.keySet);
	}

	@Override
	public @NotNull Collection<V> values() {
		return ImmutableCollection.of(this.values);
	}

	@Override
	public @NotNull Set<Entry<K, V>> entrySet() {
		List<Entry<K, V>> entries = new ArrayList<>(this.size);
		this.entries.forEach(entry -> entries.add(ImmutableEntry.of(entry)));
		return ImmutableSet.of(entries);
	}

	/**
	 * <h3 class="en-US">Static method for initialize the immutable mapping</h3>
	 * <h3 class="zh-CN">静态方法用于初始化不可变数据映射表</h3>
	 *
	 * @return <span class="en-US">Immutable mapping object</span>
	 * <span class="zh-CN">不可变数据映射表</span>
	 */
	public static <K, V> ImmutableMap<K, V> of() {
		return new ImmutableMap<>(Collections.emptyList());
	}

	/**
	 * <h3 class="en-US">Static method for initialize the immutable mapping</h3>
	 * <h3 class="zh-CN">静态方法用于初始化不可变数据映射表</h3>
	 *
	 * @param key   <span class="en-US">Key data</span>
	 *              <span class="zh-CN">数据键</span>
	 * @param value <span class="en-US">Value data</span>
	 *              <span class="zh-CN">数据值</span>
	 * @return <span class="en-US">Immutable mapping object</span>
	 * <span class="zh-CN">不可变数据映射表</span>
	 */
	public static <K, V> ImmutableMap<K, V> of(final K key, final V value) {
		Builder<K, V> builder = builder();
		builder.put(key, value);
		return builder.build();
	}

	/**
	 * <h3 class="en-US">Static method for initialize the immutable mapping</h3>
	 * <h3 class="zh-CN">静态方法用于初始化不可变数据映射表</h3>
	 *
	 * @param map <span class="en-US">Data mapping table</span>
	 *            <span class="zh-CN">数据映射表</span>
	 * @return <span class="en-US">Immutable mapping object</span>
	 * <span class="zh-CN">不可变数据映射表</span>
	 */
	public static <K, V> ImmutableMap<K, V> of(final Map<K, V> map) {
		Builder<K, V> builder = builder();
		return builder.putAll(map).build();
	}

	/**
	 * <h3 class="en-US">Static method for initialize the immutable mapping</h3>
	 * <h3 class="zh-CN">静态方法用于初始化不可变数据映射表</h3>
	 *
	 * @param iterable <span class="en-US">Iterable object of key-value pair</span>
	 *                 <span class="zh-CN">键值对数据迭代对象</span>
	 * @return <span class="en-US">Immutable mapping object</span>
	 * <span class="zh-CN">不可变数据映射表</span>
	 */
	public static <K, V> ImmutableMap<K, V> of(final Iterable<? extends Entry<K, V>> iterable) {
		Builder<K, V> builder = builder();
		return builder.putAll(iterable).build();
	}

	/**
	 * <h3 class="en-US">Static method for initialize the immutable mapping builder instance object</h3>
	 * <h3 class="zh-CN">静态方法用于初始化不可变数据映射表构建器实例对象</h3>
	 *
	 * @return <span class="en-US">Instance of immutable mapping builder</span>
	 * <span class="zh-CN">构建器实例对象</span>
	 */
	public static <K, V> Builder<K, V> builder() {
		return new Builder<>();
	}

	/**
	 * <h2 class="en-US">Immutable key-value pair</h2>
	 * <h2 class="zh-CN">不可变键值对</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Apr 3, 2025 14:35:19 $
	 */
	private static final class ImmutableEntry<K, V> implements Entry<K, V>, Serializable {

		/**
		 * <span class="en-US">Serial version UID</span>
		 * <span class="zh-CN">序列化UID</span>
		 */
		private static final long serialVersionUID = -4428489134356330170L;

		/**
		 * <span class="en-US">Key data</span>
		 * <span class="zh-CN">数据键</span>
		 */
		private final K key;
		/**
		 * <span class="en-US">Value data</span>
		 * <span class="zh-CN">数据值</span>
		 */
		private final V value;

		/**
		 * <h3 class="en-US">Private constructor for the immutable key-value pair</h3>
		 * <h3 class="zh-CN">不可变键值对的私有构造方法</h3>
		 *
		 * @param key   <span class="en-US">Key data</span>
		 *              <span class="zh-CN">数据键</span>
		 * @param value <span class="en-US">Value data</span>
		 *              <span class="zh-CN">数据值</span>
		 */
		private ImmutableEntry(final K key, final V value) {
			this.key = key;
			this.value = value;
		}

		/**
		 * <h3 class="en-US">Static method for initialize the immutable key-value pair</h3>
		 * <h3 class="zh-CN">静态方法用于初始化不可变键值对</h3>
		 *
		 * @param key   <span class="en-US">Key data</span>
		 *              <span class="zh-CN">数据键</span>
		 * @param value <span class="en-US">Value data</span>
		 *              <span class="zh-CN">数据值</span>
		 * @return <span class="en-US">Immutable mapping key-value pair</span>
		 * <span class="zh-CN">不可变数据键值对</span>
		 */
		static <K, V> ImmutableEntry<K, V> of(final K key, final V value) {
			return new ImmutableEntry<>(key, value);
		}

		/**
		 * <h3 class="en-US">Static method for initialize the immutable key-value pair</h3>
		 * <h3 class="zh-CN">静态方法用于初始化不可变键值对</h3>
		 *
		 * @param entry <span class="en-US">Data of key-value pair</span>
		 *              <span class="zh-CN">键值对数据</span>
		 * @return <span class="en-US">Immutable mapping key-value pair</span>
		 * <span class="zh-CN">不可变数据键值对</span>
		 */
		static <K, V> ImmutableEntry<K, V> of(final Entry<K, V> entry) {
			return new ImmutableEntry<>(entry.getKey(), entry.getValue());
		}

		@Override
		public K getKey() {
			return this.key;
		}

		@Override
		public V getValue() {
			return this.value;
		}

		@Override
		public V setValue(final V value) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * <h2 class="en-US">Immutable mapping builder</h2>
	 * <h2 class="zh-CN">不可变映射表构建器</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Apr 3, 2025 14:46:52 $
	 */
	public static final class Builder<K, V> {

		/**
		 * <span class="en-US">Data list of key-value pair</span>
		 * <span class="zh-CN">键值对列表</span>
		 */
		private final List<Entry<K, V>> entries;

		/**
		 * <h3 class="en-US">Private constructor for the immutable mapping builder</h3>
		 * <h3 class="zh-CN">不可变映射表构建器的私有构造方法</h3>
		 */
		private Builder() {
			this.entries = new ArrayList<>();
		}

		/**
		 * <h3 class="en-US">Fill in key-value pairs</h3>
		 * <h3 class="zh-CN">填充键值对</h3>
		 *
		 * @param key   <span class="en-US">Key data</span>
		 *              <span class="zh-CN">数据键</span>
		 * @param value <span class="en-US">Value data</span>
		 *              <span class="zh-CN">数据值</span>
		 * @return <span class="en-US">Current instance of immutable mapping builder</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		@SuppressWarnings("UnusedReturnValue")
		public Builder<K, V> put(final K key, final V value) {
			return this.put(ImmutableEntry.of(key, value));
		}

		/**
		 * <h3 class="en-US">Fill in key-value pairs</h3>
		 * <h3 class="zh-CN">填充键值对</h3>
		 *
		 * @param entry <span class="en-US">Data of key-value pair</span>
		 *              <span class="zh-CN">键值对数据</span>
		 * @return <span class="en-US">Current instance of immutable mapping builder</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<K, V> put(final Entry<K, V> entry) {
			if (this.entries.stream().noneMatch(exist -> ObjectUtils.nullSafeEquals(exist.getKey(), entry.getKey()))) {
				this.entries.add(entry);
			}
			return this;
		}

		/**
		 * <h3 class="en-US">Fill in the data of key-value pairs by the given data mapping table</h3>
		 * <h3 class="zh-CN">填充数据映射表中的所有键值对</h3>
		 *
		 * @param map <span class="en-US">Data mapping table</span>
		 *            <span class="zh-CN">数据映射表</span>
		 * @return <span class="en-US">Current instance of immutable mapping builder</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<K, V> putAll(final Map<? extends K, ? extends V> map) {
			if (map != null) {
				map.forEach(this::put);
			}
			return this;
		}

		/**
		 * <h3 class="en-US">Iterable object and fill in the data of key-value pairs</h3>
		 * <h3 class="zh-CN">填充迭代对象中的所有键值对</h3>
		 *
		 * @param iterable <span class="en-US">Iterable object of key-value pair</span>
		 *                 <span class="zh-CN">键值对数据迭代对象</span>
		 * @return <span class="en-US">Current instance of immutable mapping builder</span>
		 * <span class="zh-CN">当前构建器实例对象</span>
		 */
		public Builder<K, V> putAll(final Iterable<? extends Entry<K, V>> iterable) {
			if (iterable != null) {
				iterable.forEach(this::put);
			}
			return this;
		}

		/**
		 * <h3 class="en-US">Build the immutable mapping object</h3>
		 * <h3 class="zh-CN">构建不可变数据映射表</h3>
		 *
		 * @return <span class="en-US">Immutable mapping object</span>
		 * <span class="zh-CN">不可变数据映射表</span>
		 */
		public ImmutableMap<K, V> build() {
			return new ImmutableMap<>(this.entries);
		}
	}
}
