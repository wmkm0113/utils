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
package org.nervousync.archive.io.lzma;

import java.io.IOException;

/**
 * <h2 class="en-US">Shared LZMA symbol decoder and circular dictionary.</h2>
 * <h2 class="zh-CN">LZMA1/LZMA2 共用的符号解码器和循环字典。</h2>
 * <p class="en-US">Owns no stream. Range decoders may change between chunks while
 * dictionary and probability state remain intact. The framing layer validates boundaries.</p>
 * <p class="zh-CN">不持有流的所有权。分块间可更换区间解码器，同时保留字典及概率状态。
 * 块边界由封装层验证。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
final class LZMADecoder {
	/**
	 * <span class="en-US">Probability state; absent until properties are supplied.</span>
	 * <span class="zh-CN">提供属性后才创建的概率状态。</span>
	 */
	private LZMACodec.Models models;

	/**
	 * <span class="en-US">Circular history, released explicitly on close.</span>
	 * <span class="zh-CN">循环历史，关闭时显式释放。</span>
	 */
	private byte[] dictionary;

	/**
	 * <span class="en-US">Declared distance bound, distinct from actual allocation.</span>
	 * <span class="zh-CN">声明距离上限，可不同于实际分配大小。</span>
	 */
	private final long dictionaryLimit;

	/**
	 * <span class="en-US">Literal context bits.</span>
	 * <span class="zh-CN">字面量上下文位数。</span>
	 */
	private int lc;

	/**
	 * <span class="en-US">Low position bits for literals.</span>
	 * <span class="zh-CN">字面量的位置低位掩码。</span>
	 */
	private int literalMask;

	/**
	 * <span class="en-US">Low position bits for match/length models.</span>
	 * <span class="zh-CN">匹配和长度模型的位置低位掩码。</span>
	 */
	private int positionMask;

	/**
	 * <span class="en-US">Next dictionary write index.</span>
	 * <span class="zh-CN">下一个字典写入位置。</span>
	 */
	private int position;

	/**
	 * <span class="en-US">Bytes produced since the last dictionary reset.</span>
	 * <span class="zh-CN">最近一次字典重置后输出的字节数。</span>
	 */
	private long produced;

	/**
	 * <span class="en-US">Current LZMA symbol state.</span>
	 * <span class="zh-CN">当前 LZMA 符号状态。</span>
	 */
	private int state;

	/**
	 * <span class="en-US">Most recent zero-based match distance.</span>
	 * <span class="zh-CN">最近的零基匹配距离。</span>
	 */
	private int rep0;

	/**
	 * <span class="en-US">Second most recent zero-based match distance.</span>
	 * <span class="zh-CN">第二近的零基匹配距离。</span>
	 */
	private int rep1;

	/**
	 * <span class="en-US">Third most recent zero-based match distance.</span>
	 * <span class="zh-CN">第三近的零基匹配距离。</span>
	 */
	private int rep2;

	/**
	 * <span class="en-US">Fourth most recent zero-based match distance.</span>
	 * <span class="zh-CN">第四近的零基匹配距离。</span>
	 */
	private int rep3;

	/**
	 * <span class="en-US">-1 literal, 0 new symbol, positive remaining match length.</span>
	 * <span class="zh-CN">-1 为字面量，0 需新符号，正数为剩余匹配长度。</span>
	 */
	private int pending;

	/**
	 * <span class="en-US">Previous output byte, or pending literal.</span>
	 * <span class="zh-CN">前一输出字节或待输出字面量。</span>
	 */
	private int previous;

	/**
	 * <h4 class="en-US">Allocate shared decoding history.</h4>
	 * <h4 class="zh-CN">分配共用解码历史。</h4>
	 *
	 * <p class="en-US">The framing layer validates memory limits before construction. LZMA2 supplies properties in its first compressed chunk.</p>
	 * <p class="zh-CN">封装层在构造前验证内存上限；LZMA2 在首个压缩块中提供属性。</p>
	 * @param properties <span class="en-US">Packed lc/lp/pb value, or -1 to defer model creation.</span>
	 * <span class="zh-CN">打包的 lc/lp/pb 属性；-1 表示延后创建模型。</span>
	 * @param dictionaryLimit <span class="en-US">Validated declared distance bound in bytes.</span>
	 * <span class="zh-CN">已经验证的声明距离上限，单位为字节。</span>
	 * @param allocation <span class="en-US">Positive history allocation validated by the framing layer.</span>
	 * <span class="zh-CN">封装层已经验证的正数历史缓冲区大小。</span>
	 * @throws IOException <span class="en-US">The supplied property value is invalid.</span>
	 * <span class="zh-CN">提供的属性值非法。</span>
	 */
	LZMADecoder(int properties, long dictionaryLimit, int allocation) throws IOException {
		this.dictionaryLimit = dictionaryLimit;
		this.dictionary = new byte[allocation];
		if (properties >= 0) {
			this.configure(properties);
		}
	}

	/**
	 * <h4 class="en-US">Configure properties and initialize symbol state.</h4>
	 * <h4 class="zh-CN">设置属性并初始化符号状态。</h4>
	 *
	 * <p class="en-US">Rebuild models and reset the state, recent distances and pending output. Preserve history, previous byte and output position. LZMA2 additionally validates lc + lp before calling.</p>
	 * <p class="zh-CN">重新创建概率模型，重置状态机、最近距离及待输出内容；保留历史、前一字节和输出位置。LZMA2 调用前还需验证 lc + lp。</p>
	 * @param properties <span class="en-US">Packed property value from 0 through 224.</span>
	 * <span class="zh-CN">范围为 0 至 224 的打包属性值。</span>
	 * @throws IOException <span class="en-US">The property value is outside 0 through 224.</span>
	 * <span class="zh-CN">属性值不在 0 至 224 范围内。</span>
	 */
	void configure(int properties) throws IOException {
		if (properties < 0 || properties >= 225) {
			throw new IOException("Invalid LZMA properties");
		}
		this.lc = properties % 9;
		int lp = (properties / 9) % 5;
		int pb = properties / 45;
		this.literalMask = (1 << lp) - 1;
		this.positionMask = (1 << pb) - 1;
		this.models = new LZMACodec.Models(this.lc + lp);
		this.state = this.rep0 = this.rep1 = this.rep2 = this.rep3 = this.pending = 0;
	}

	/**
	 * <h4 class="en-US">Reset probability and repetition state.</h4>
	 * <h4 class="zh-CN">重置概率和重复匹配状态。</h4>
	 *
	 * <p class="en-US">Requires configured properties and a verified chunk boundary. Preserve previous byte, dictionary and position context.</p>
	 * <p class="zh-CN">要求已设置属性，且已验证当前块边界。保留前一字节、字典和位置上下文。</p>
	 */
	void resetState() {
		this.models.reset();
		this.state = this.rep0 = this.rep1 = this.rep2 = this.rep3 = this.pending = 0;
	}

	/**
	 * <h4 class="en-US">Invalidate all history references.</h4>
	 * <h4 class="zh-CN">使全部历史引用失效。</h4>
	 *
	 * <p class="en-US">Reset the write position, previous byte, pending output and history count without clearing storage. This operation does not reset probability models; the framing layer applies the remaining control-byte transitions.</p>
	 * <p class="zh-CN">重置写入位置、前一字节、待输出内容和历史计数，无需清空存储；本操作不重置概率模型，其余状态转换由封装层按控制字节处理。</p>
	 */
	void resetDictionary() {
		this.position = this.previous = this.pending = 0;
		this.produced = 0;
	}

	/**
	 * <h4 class="en-US">Check for a partially emitted symbol.</h4>
	 * <h4 class="zh-CN">检查是否有尚未输出完的符号。</h4>
	 * @return <span class="en-US">Whether a literal or match still has pending output.</span>
	 * <span class="zh-CN">是否仍有待输出的字面量或匹配字节。</span>
	 */
	boolean hasPending() {
		return this.pending != 0;
	}

	/**
	 * <h4 class="en-US">Append one byte to circular history.</h4>
	 * <h4 class="zh-CN">将一个字节追加到循环历史。</h4>
	 * @param value <span class="en-US">Unsigned byte from 0 through 255.</span>
	 * <span class="zh-CN">范围为 0 至 255 的无符号字节。</span>
	 * @throws IOException <span class="en-US">The output position would overflow.</span>
	 * <span class="zh-CN">输出位置将发生溢出。</span>
	 */
	private void append(int value) throws IOException {
		if (this.produced == Long.MAX_VALUE) {
			throw new IOException("LZMA output position overflow");
		}
		this.dictionary[this.position++] = (byte) value;
		if (this.position == this.dictionary.length) {
			this.position = 0;
		}
		this.previous = value;
		this.produced++;
	}

	/**
	 * <h4 class="en-US">Add stored bytes to decoding history.</h4>
	 * <h4 class="zh-CN">将未压缩字节加入解码历史。</h4>
	 *
	 * <p class="en-US">Caller validates array bounds and ensures the previous symbol has ended. The following compressed chunk may reference these bytes.</p>
	 * <p class="zh-CN">调用方验证数组边界并确保前一符号已结束，后续压缩块可以引用这些字节。</p>
	 * @param bytes <span class="en-US">Source array.</span>
	 * <span class="zh-CN">源数组。</span>
	 * @param offset <span class="en-US">First source index.</span>
	 * <span class="zh-CN">源起始下标。</span>
	 * @param length <span class="en-US">Number of source bytes.</span>
	 * <span class="zh-CN">源字节数。</span>
	 * @throws IOException <span class="en-US">A match crosses the boundary or the history position overflows.</span>
	 * <span class="zh-CN">匹配越过块边界，或历史位置计数溢出。</span>
	 */
	void appendUncompressed(byte[] bytes, int offset, int length) throws IOException {
		if (this.hasPending()) {
			throw new IOException("LZMA match crosses chunk boundary");
		}
		for (int index = 0; index < length; index++) {
			this.append(bytes[offset + index] & 255);
		}
	}

	/**
	 * <h4 class="en-US">Decode and record one output byte.</h4>
	 * <h4 class="zh-CN">解码并记录一个输出字节。</h4>
	 *
	 * <p class="en-US">Pending matches resume without decoding another symbol. Always pass false for LZMA2; chunk output length is checked by its framing layer.</p>
	 * <p class="zh-CN">待输出匹配直接继续，不重新解码符号。LZMA2 必须传入 false，其块输出长度由封装层验证。</p>
	 * @param range <span class="en-US">Current stream or chunk range decoder.</span>
	 * <span class="zh-CN">当前数据流或分块的区间解码器。</span>
	 * @param allowEndMarker <span class="en-US">Whether the framing permits a LZMA1 EOS match.</span>
	 * <span class="zh-CN">封装是否允许 LZMA1 EOS 匹配。</span>
	 * @return <span class="en-US">Unsigned byte, or -1 for a permitted and validated EOS.</span>
	 * <span class="zh-CN">无符号字节；遇到允许且验证通过的 EOS 时返回 -1。</span>
	 * @throws IOException <span class="en-US">The delegate fails or encoded data is invalid or truncated.</span>
	 * <span class="zh-CN">底层流操作失败，或编码数据非法、截断。</span>
	 */
	int readByte(LZMACodec.Decoder range, boolean allowEndMarker) throws IOException {
		if (this.pending == 0 && !this.symbol(range, allowEndMarker)) {
			return -1;
		}
		int value;
		if (this.pending == -1) {
			value = this.previous;
			this.pending = 0;
		} else {
			value = this.history(this.rep0);
			this.pending--;
		}
		this.append(value);
		return value;
	}

	/**
	 * <h4 class="en-US">Resolve a zero-based backward distance.</h4>
	 * <h4 class="zh-CN">解析零基后向距离。</h4>
	 *
	 * <p class="en-US">The distance must fit produced history, the declared dictionary and allocated storage before an array is accessed.</p>
	 * <p class="zh-CN">访问数组前，距离必须同时满足已产生的历史长度、声明字典上限和实际存储大小。</p>
	 * @param distance <span class="en-US">Unsigned 32-bit distance minus one.</span>
	 * <span class="zh-CN">无符号 32 位的距离减一。</span>
	 * @return <span class="en-US">Unsigned byte at the validated history position.</span>
	 * <span class="zh-CN">经过验证的历史位置中的无符号字节。</span>
	 * @throws IOException <span class="en-US">The distance exceeds valid history or a dictionary bound.</span>
	 * <span class="zh-CN">距离超出有效历史或字典上限。</span>
	 */
	private int history(int distance) throws IOException {
		long offset = Integer.toUnsignedLong(distance) + 1;
		if (offset > this.produced || offset > this.dictionaryLimit || offset > this.dictionary.length) {
			throw new IOException("Invalid LZMA match distance");
		}
		int index = this.position - (int) offset;
		if (index < 0) {
			index += this.dictionary.length;
		}
		return this.dictionary[index] & 255;
	}

	/**
	 * <h4 class="en-US">Decode the next literal, match or EOS.</h4>
	 * <h4 class="zh-CN">解码下一个字面量、匹配或 EOS。</h4>
	 * @param range <span class="en-US">Active range decoder.</span>
	 * <span class="zh-CN">当前区间解码器。</span>
	 * @param allowEndMarker <span class="en-US">Whether LZMA1 EOS is permitted.</span>
	 * <span class="zh-CN">是否允许 LZMA1 EOS。</span>
	 * @return <span class="en-US">True when output is pending; false only for a valid EOS.</span>
	 * <span class="zh-CN">有待输出内容时为 true；仅合法 EOS 返回 false。</span>
	 * @throws IOException <span class="en-US">The delegate fails or encoded data is invalid or truncated.</span>
	 * <span class="zh-CN">底层流操作失败，或编码数据非法、截断。</span>
	 */
	private boolean symbol(LZMACodec.Decoder range, boolean allowEndMarker) throws IOException {
		int positionState = (int) this.produced & this.positionMask;
		if (range.bit(this.models.match, (this.state << 4) + positionState) == 0) {
			int context = (((int) this.produced & this.literalMask) << this.lc) + (this.previous >>> (8 - this.lc));
			int base = context * 768, value = 1;
			if (this.state >= 7) {
				int matchByte = this.history(this.rep0);
				do {
					int matchBit = (matchByte >>> 7) & 1;
					matchByte <<= 1;
					int bit = range.bit(this.models.literal, base + ((1 + matchBit) << 8) + value);
					value = (value << 1) | bit;
					if (bit != matchBit) {
						break;
					}
				}
				while (value < 256);
			}
			while (value < 256) {
				value = (value << 1) | range.bit(this.models.literal, base + value);
			}
			this.state = LZMACodec.literalState(this.state);
			this.previous = value & 255;
			this.pending = -1;
			return true;
		}
		if (range.bit(this.models.rep, this.state) != 0) {
			if (range.bit(this.models.rep0, this.state) == 0) {
				if (range.bit(this.models.shortRep, (this.state << 4) + positionState) == 0) {
					this.state = this.state < 7 ? 9 : 11;
					this.pending = 1;
					this.history(this.rep0);
					return true;
				}
			} else {
				int distance;
				if (range.bit(this.models.rep1, this.state) == 0) {
					distance = this.rep1;
				} else {
					if (range.bit(this.models.rep2, this.state) == 0) {
						distance = this.rep2;
					} else {
						distance = this.rep3;
						this.rep3 = this.rep2;
					}
					this.rep2 = this.rep1;
				}
				this.rep1 = this.rep0;
				this.rep0 = distance;
			}
			this.pending = range.length(this.models.repLength, positionState);
			this.state = this.state < 7 ? 8 : 11;
		} else {
			this.rep3 = this.rep2;
			this.rep2 = this.rep1;
			this.rep1 = this.rep0;
			this.pending = range.length(this.models.length, positionState);
			this.state = this.state < 7 ? 7 : 10;
			int slot = range.tree(this.models.slots, Math.min(this.pending - 2, 3) * 64, 6);
			if (slot < 4) {
				this.rep0 = slot;
			} else {
				int bits = (slot >>> 1) - 1;
				this.rep0 = (2 | (slot & 1)) << bits;
				if (slot < 14) {
					this.rep0 += range.reverse(this.models.distances, this.rep0 - slot - 1, bits);
				} else {
					this.rep0 += range.direct(bits - 4) << 4;
					this.rep0 += range.reverse(this.models.align, 0, 4);
					if (this.rep0 == -1) {
						if (!allowEndMarker || this.pending != 2 || !range.finished()) {
							throw new IOException("Invalid LZMA EOS/size");
						}
						this.pending = 0;
						return false;
					}
				}
			}
		}
		this.history(this.rep0);
		return true;
	}

	/**
	 * <h4 class="en-US">Release decoder-owned memory.</h4>
	 * <h4 class="zh-CN">释放解码器持有的内存。</h4>
	 *
	 * <p class="en-US">Called by the stream wrapper on close. Does not close a stream; the decoder must not be used afterward.</p>
	 * <p class="zh-CN">由流封装层关闭时调用，不关闭任何流；释放后不得继续使用本解码器。</p>
	 */
	void release() {
		this.dictionary = null;
		this.models = null;
	}
}
