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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * <h2 class="en-US">Shared LZMA1 probability models and binary range coding.</h2>
 * <h2 class="zh-CN">LZMA1 共用概率模型与二进制区间编解码。</h2>
 *
 * <p class="en-US">Models are updated symmetrically by encoder and decoder. A probability starts at 1024/2048 and approaches each observed bit with an adaptation shift of five.</p>
 * <p class="zh-CN">编码和解码以相同规则更新模型。概率初值为 1024/2048，每次按五位移位的步长向已观察到的位调整。</p>
 *
 * <p class="en-US">Tree operations traverse adaptive binary probability trees, not Huffman code tables. This helper contains no ZIP or LZMA-Alone framing and no dictionary.</p>
 * <p class="zh-CN">树操作遍历自适应二叉概率树，并非 Huffman 编码表。本辅助类不处理 ZIP、LZMA-Alone 封装或字典。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
final class LZMACodec {

	/**
	 * <h4 class="en-US">Prevent construction of the helper namespace.</h4>
	 * <h4 class="zh-CN">禁止创建仅用于组织辅助功能的外层类实例。</h4>
	 */
	private LZMACodec() {
	}

	/**
	 * <h4 class="en-US">Allocate equiprobable binary models.</h4>
	 * <h4 class="zh-CN">分配初始等概率的二进制模型。</h4>
	 *
	 * @param size <span class="en-US">Number of model slots.</span>
	 *             <span class="zh-CN">模型槽数量。</span>
	 * @return <span class="en-US">Initialized probability array.</span>
	 * <span class="zh-CN">已初始化的概率数组。</span>
	 */
	static int[] probabilities(int size) {
		int[] result = new int[size];
		Arrays.fill(result, 1024);
		return result;
	}

	/**
	 * <h4 class="en-US">Apply the LZMA state transition after a literal.</h4>
	 * <h4 class="zh-CN">应用输出字面量后的 LZMA 状态转移。</h4>
	 *
	 * @param state <span class="en-US">Previous state, in 0..11.</span>
	 *              <span class="zh-CN">原状态，范围为 0..11。</span>
	 * @return <span class="en-US">Next state, in 0..6.</span>
	 * <span class="zh-CN">下一状态，范围为 0..6。</span>
	 */
	static int literalState(int state) {
		return state < 4 ? 0 : state < 10 ? state - 3 : state - 6;
	}

	/**
	 * <h2 class="en-US">Adaptive model groups for one independent LZMA1 stream.</h2>
	 * <h2 class="zh-CN">一个独立 LZMA1 数据流所需的自适应模型组。</h2>
	 *
	 * <p class="en-US">Rep selection trees correspond to the four recently used zero-based distances. Length models contain two choices, 16 low trees, 16 middle trees and one high tree.</p>
	 * <p class="zh-CN">重复距离选择树对应最近使用的四个从零开始的距离。长度模型包含两个分支、16 个低段树、16 个中段树和一个高段树。</p>
	 */
	static final class Models {
		// 12 states, up to 16 position states; probabilities start at 1/2 (1024/2048).
		// 12 种状态、最多 16 种位置状态；概率初值为二分之一。
		/**
		 * <span class="en-US">Literal versus match, indexed by state and position state.</span>
		 * <span class="zh-CN">按状态及位置状态索引，区分字面量与匹配。</span>
		 */
		final int[] match = probabilities(12 * 16);
		/**
		 * <span class="en-US">Ordinary match versus cached-distance repetition.</span>
		 * <span class="zh-CN">区分普通匹配与使用缓存距离的重复匹配。</span>
		 */
		final int[] rep = probabilities(12);
		/**
		 * <span class="en-US">Select the most recent distance versus older cached distances.</span>
		 * <span class="zh-CN">选择最近距离或更早的缓存距离。</span>
		 */
		final int[] rep0 = probabilities(12);
		/**
		 * <span class="en-US">Select the second cached distance versus the remaining two.</span>
		 * <span class="zh-CN">选择第二个缓存距离或其余两个距离。</span>
		 */
		final int[] rep1 = probabilities(12);
		/**
		 * <span class="en-US">Select the third versus fourth cached distance.</span>
		 * <span class="zh-CN">选择第三或第四个缓存距离。</span>
		 */
		final int[] rep2 = probabilities(12);
		/**
		 * <span class="en-US">Distinguish a one-byte repetition at the most recent distance.</span>
		 * <span class="zh-CN">区分使用最近距离的单字节重复。</span>
		 */
		final int[] shortRep = probabilities(12 * 16);
		/**
		 * <span class="en-US">Four six-bit distance-slot trees, selected by match length.</span>
		 * <span class="zh-CN">按匹配长度选择的四棵六位距离槽树。</span>
		 */
		final int[] slots = probabilities(4 * 64);
		/**
		 * <span class="en-US">Probability slots for distance suffixes from slots 4..13; offsets account for implicit tree roots.</span>
		 * <span class="zh-CN">距离槽 4..13 的后缀概率槽，偏移计算包含隐式树根的位置。</span>
		 */
		final int[] distances = probabilities(114);
		/**
		 * <span class="en-US">Four-bit alignment tree for the low bits of a large distance.</span>
		 * <span class="zh-CN">保存大距离低四位的对齐树。</span>
		 */
		final int[] align = probabilities(16);
		/**
		 * <span class="en-US">768 probabilities per lc/lp literal context, including matched-literal branches.</span>
		 * <span class="zh-CN">每个 lc/lp 字面量上下文含 768 个概率槽，包括匹配字面量分支。</span>
		 */
		final int[] literal;
		// Choices + low[16][8] + mid[16][8] + high[256]. Lengths cover 2..273.
		// 两个分支概率加低/中/高长度树，覆盖长度 2..273。
		/**
		 * <span class="en-US">Ordinary-match length models: choices at 0/1, low at 2, middle at 130, high at 258.</span>
		 * <span class="zh-CN">普通匹配长度模型：分支位于 0/1，低段从 2、中段从 130、高段从 258 开始。</span>
		 */
		final int[] length = probabilities(2 + 16 * 16 + 256);
		/**
		 * <span class="en-US">Separate length models for cached-distance repetitions.</span>
		 * <span class="zh-CN">使用缓存距离的重复匹配所需的独立长度模型。</span>
		 */
		final int[] repLength = probabilities(2 + 16 * 16 + 256);

		/**
		 * <h4 class="en-US">Allocate literal contexts for lc + lp bits.</h4>
		 * <h4 class="zh-CN">按 lc + lp 位分配字面量上下文。</h4>
		 *
		 * @param contextBits <span class="en-US">Sum of the validated lc and lp properties.</span>
		 *                    <span class="zh-CN">已校验属性 lc 与 lp 之和。</span>
		 */
		Models(int contextBits) {
			this.literal = probabilities(768 << contextBits);
		}

		/**
		 * <h4 class="en-US">Reset all adaptive probability models.</h4>
		 * <h4 class="zh-CN">重置全部自适应概率模型。</h4>
		 *
		 * <p class="en-US">
		 * Restore probabilities to 1024/2048 without reallocating model arrays.
		 * Dictionary history and decoder state are managed separately.
		 * </p>
		 * <p class="zh-CN">
		 * 将概率恢复为 1024/2048，复用现有模型数组。
		 * 历史字典和解码器状态由其他方法分别管理。
		 * </p>
		 */
		void reset() {
			for (int[] values : new int[][]{this.match, this.rep, this.rep0, this.rep1, this.rep2,
					this.shortRep, this.slots, this.distances, this.align, this.literal, this.length, this.repLength}) {
				Arrays.fill(values, 1024);
			}
		}
	}

	/**
	 * <h4 class="en-US">Read one required unsigned byte.</h4>
	 * <h4 class="zh-CN">读取一个必需的无符号字节。</h4>
	 *
	 * @param input <span class="en-US">Delegate stream.</span>
	 *              <span class="zh-CN">底层流。</span>
	 * @return <span class="en-US">Value in 0..255.</span>
	 * <span class="zh-CN">范围为 0..255 的值。</span>
	 * @throws EOFException <span class="en-US">Required encoded data is missing.</span>
	 *                      <span class="zh-CN">缺少必需的编码数据。</span>
	 * @throws IOException  <span class="en-US">The delegate read fails.</span>
	 *                      <span class="zh-CN">底层读取失败。</span>
	 */
	static int readByte(InputStream input) throws IOException {
		int value = input.read();
		if (value < 0) throw new EOFException("Truncated LZMA stream");
		return value;
	}

	/**
	 * <h2 class="en-US">Forward-only unsigned 32-bit binary range decoder.</h2>
	 * <h2 class="zh-CN">仅向前读取的无符号 32 位二进制区间解码器。</h2>
	 */
	static final class Decoder {
		/**
		 * <span class="en-US">Delegate providing range-coded bytes.</span>
		 * <span class="zh-CN">提供区间编码字节的底层输入流。</span>
		 */
		private final InputStream input;
		/**
		 * <span class="en-US">Unsigned 32-bit interval width held in a long.</span>
		 * <span class="zh-CN">以 long 保存的无符号 32 位区间宽度。</span>
		 */
		private long range = 0xFFFFFFFFL;
		/**
		 * <span class="en-US">Current unsigned code point relative to the interval lower bound.</span>
		 * <span class="zh-CN">当前相对于区间下界的无符号码值。</span>
		 */
		private long code;

		/**
		 * <h4 class="en-US">Read the zero initialization byte and four-byte code point.</h4>
		 * <h4 class="zh-CN">读取初始化零字节与四字节初始码值。</h4>
		 *
		 * @param input <span class="en-US">Stream positioned at raw LZMA range data.</span>
		 *              <span class="zh-CN">位于原始 LZMA 区间数据起点的流。</span>
		 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
		 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
		 */
		Decoder(InputStream input) throws IOException {
			this.input = input;
			if (readByte(this.input) != 0) throw new IOException("Invalid LZMA range initialization");
			for (int index = 0; index < 4; index++) this.code = (this.code << 8) | readByte(this.input);
			if (this.code == this.range) throw new IOException("Invalid LZMA range code");
		}

		/**
		 * <h4 class="en-US">Refill one byte when the range falls below 2^24.</h4>
		 * <h4 class="zh-CN">区间小于 2^24 时补入一个字节。</h4>
		 *
		 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
		 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
		 */
		private void normalize() throws IOException {
			// Keep at least 24 significant range bits; use long for unsigned 32-bit arithmetic.
			// 保留至少 24 位有效区间，以 long 执行无符号 32 位运算。
			if (this.range < 0x01000000L) {
				this.range <<= 8;
				this.code = ((this.code << 8) | readByte(this.input)) & 0xFFFFFFFFL;
			}
		}

		/**
		 * <h4 class="en-US">Decode one modeled bit and adapt its probability.</h4>
		 * <h4 class="zh-CN">解码一个建模位并更新对应概率。</h4>
		 *
		 * @param models <span class="en-US">Adaptive probability array; each value represents P(bit=0) on a 2048-point scale.</span>
		 *               <span class="zh-CN">自适应概率数组，每项以 2048 为分母表示位为 0 的概率。</span>
		 * @param index  <span class="en-US">Probability slot for this decision.</span>
		 *               <span class="zh-CN">本次判定位使用的概率槽。</span>
		 * @return <span class="en-US">Decoded bit, 0 or 1.</span>
		 * <span class="zh-CN">解码位，值为 0 或 1。</span>
		 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
		 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
		 */
		int bit(int[] models, int index) throws IOException {
			int probability = models[index];
			long bound = (this.range >>> 11) * probability;
			int result;
			if (this.code < bound) {
				this.range = bound;
				models[index] = probability + ((2048 - probability) >>> 5);
				result = 0;
			} else {
				this.range -= bound;
				this.code -= bound;
				models[index] = probability - (probability >>> 5);
				result = 1;
			}
			this.normalize();
			return result;
		}

		/**
		 * <h4 class="en-US">Decode a tree symbol in most-significant-bit order.</h4>
		 * <h4 class="zh-CN">按最高有效位优先顺序解码树符号。</h4>
		 *
		 * @param models <span class="en-US">Adaptive probability array; each value represents P(bit=0) on a 2048-point scale.</span>
		 *               <span class="zh-CN">自适应概率数组，每项以 2048 为分母表示位为 0 的概率。</span>
		 * @param offset <span class="en-US">Base index of the binary probability tree.</span>
		 *               <span class="zh-CN">二叉概率树的起始偏移。</span>
		 * @param bits   <span class="en-US">Number of bits in the encoded symbol.</span>
		 *               <span class="zh-CN">编码符号的位数。</span>
		 * @return <span class="en-US">Unsigned symbol represented by the requested bit count.</span>
		 * <span class="zh-CN">指定位数表示的无符号符号值。</span>
		 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
		 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
		 */
		int tree(int[] models, int offset, int bits) throws IOException {
			int symbol = 1;
			for (int index = 0; index < bits; index++) symbol = (symbol << 1) | this.bit(models, offset + symbol);
			return symbol - (1 << bits);
		}

		/**
		 * <h4 class="en-US">Decode a tree symbol in least-significant-bit order.</h4>
		 * <h4 class="zh-CN">按最低有效位优先顺序解码树符号。</h4>
		 *
		 * @param models <span class="en-US">Adaptive probability array; each value represents P(bit=0) on a 2048-point scale.</span>
		 *               <span class="zh-CN">自适应概率数组，每项以 2048 为分母表示位为 0 的概率。</span>
		 * @param offset <span class="en-US">Base index of the binary probability tree.</span>
		 *               <span class="zh-CN">二叉概率树的起始偏移。</span>
		 * @param bits   <span class="en-US">Number of bits in the encoded symbol.</span>
		 *               <span class="zh-CN">编码符号的位数。</span>
		 * @return <span class="en-US">Symbol reconstructed from low to high bits.</span>
		 * <span class="zh-CN">由低位到高位恢复的符号值。</span>
		 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
		 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
		 */
		int reverse(int[] models, int offset, int bits) throws IOException {
			int symbol = 1, result = 0;
			for (int index = 0; index < bits; index++) {
				int bit = this.bit(models, offset + symbol);
				symbol = (symbol << 1) | bit;
				result |= bit << index;
			}
			return result;
		}

		/**
		 * <h4 class="en-US">Decode equiprobable bits without adaptive models.</h4>
		 * <h4 class="zh-CN">不使用自适应模型，解码等概率位。</h4>
		 *
		 * @param bits <span class="en-US">Number of bits in the encoded symbol.</span>
		 *             <span class="zh-CN">编码符号的位数。</span>
		 * @return <span class="en-US">Bits accumulated in most-significant-bit order.</span>
		 * <span class="zh-CN">按最高有效位优先顺序累计的数值。</span>
		 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
		 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
		 */
		int direct(int bits) throws IOException {
			int result = 0;
			for (int index = 0; index < bits; index++) {
				this.range >>>= 1;
				int bit = 0;
				if (this.code >= this.range) {
					this.code -= this.range;
					bit = 1;
				}
				result = (result << 1) | bit;
				this.normalize();
			}
			return result;
		}

		/**
		 * <h4 class="en-US">Decode a match length using low, middle or high trees.</h4>
		 * <h4 class="zh-CN">通过低、中、高段概率树解码匹配长度。</h4>
		 *
		 * @param models        <span class="en-US">Adaptive probability array; each value represents P(bit=0) on a 2048-point scale.</span>
		 *                      <span class="zh-CN">自适应概率数组，每项以 2048 为分母表示位为 0 的概率。</span>
		 * @param positionState <span class="en-US">Low pb bits of the current output position.</span>
		 *                      <span class="zh-CN">当前输出位置的低 pb 位。</span>
		 * @return <span class="en-US">Length in 2..273: low 2..9, middle 10..17, high 18..273.</span>
		 * <span class="zh-CN">范围为 2..273：低段 2..9，中段 10..17，高段 18..273。</span>
		 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
		 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
		 */
		int length(int[] models, int positionState) throws IOException {
			if (this.bit(models, 0) == 0) return 2 + this.tree(models, 2 + positionState * 8, 3);
			if (this.bit(models, 1) == 0) return 10 + this.tree(models, 130 + positionState * 8, 3);
			return 18 + this.tree(models, 258, 8);
		}

		/**
		 * <h4 class="en-US">Inspect the final range-code condition without consuming input.</h4>
		 * <h4 class="zh-CN">不读取输入，检查最终区间码条件。</h4>
		 *
		 * @return <span class="en-US">Whether code is zero; the caller must also validate size and/or EOS.</span>
		 * <span class="zh-CN">码值是否为零；调用方还必须校验大小和/或 EOS。</span>
		 */
		boolean finished() {
			return this.code == 0;
		}
	}

	/**
	 * <h2 class="en-US">Binary range encoder with delayed carry propagation.</h2>
	 * <h2 class="zh-CN">采用延迟进位传播的二进制区间编码器。</h2>
	 */
	static final class Encoder {
		/**
		 * <span class="en-US">Delegate receiving normalized range-coded bytes.</span>
		 * <span class="zh-CN">接收归一化区间编码字节的底层输出流。</span>
		 */
		private final OutputStream output;
		/**
		 * <span class="en-US">Interval lower bound with carry bits retained above bit 31.</span>
		 * <span class="zh-CN">区间下界，保留第 31 位以上的进位信息。</span>
		 */
		private long low;
		/**
		 * <span class="en-US">Unsigned 32-bit interval width held in a long.</span>
		 * <span class="zh-CN">以 long 保存的无符号 32 位区间宽度。</span>
		 */
		private long range = 0xFFFFFFFFL;
		/**
		 * <span class="en-US">One output byte delayed until its carry is known.</span>
		 * <span class="zh-CN">等待进位确定后才能输出的缓存字节。</span>
		 */
		private int cache;
		/**
		 * <span class="en-US">Cached-byte count including the leading cache byte and following deferred FF bytes.</span>
		 * <span class="zh-CN">待输出字节数，包含首个缓存字节及其后的延迟 FF 字节。</span>
		 */
		private long cacheSize = 1;

		/**
		 * <h4 class="en-US">Bind an output delegate without writing framing bytes.</h4>
		 * <h4 class="zh-CN">绑定输出流，不写入格式封装字节。</h4>
		 *
		 * @param output <span class="en-US">Destination for raw range-coded data.</span>
		 *               <span class="zh-CN">原始区间编码数据的目标流。</span>
		 */
		Encoder(OutputStream output) {
			this.output = output;
		}

		/**
		 * <h4 class="en-US">Shift out one lower-bound byte while resolving deferred carry.</h4>
		 * <h4 class="zh-CN">移出下界的一个字节，同时处理延迟进位。</h4>
		 *
		 * <p class="en-US">The low 32 bits remain in the interval; the high part carries into the cached byte. A run of FF bytes is emitted only once its carry can no longer change.</p>
		 * <p class="zh-CN">低 32 位保留区间信息，高位进位传递到缓存字节。连续 FF 字节要等进位不再变化时才能输出。</p>
		 *
		 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
		 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
		 */
		private void shift() throws IOException {
			// Delay FF bytes until carry is known, then emit the cached byte and its followers.
			// 延迟输出 FF 字节，进位确定后再输出缓存字节及其后续字节。
			long low32 = this.low & 0xFFFFFFFFL;
			int carry = (int) (this.low >>> 32);
			if (low32 < 0xFF000000L || carry != 0) {
				int value = this.cache;
				do {
					this.output.write((value + carry) & 255);
					value = 255;
				} while (--this.cacheSize != 0);
				this.cache = (int) (low32 >>> 24);
			}
			this.cacheSize++;
			this.low = (low32 & 0xFFFFFFL) << 8;
		}

		/**
		 * <h4 class="en-US">Encode one modeled bit and adapt its probability.</h4>
		 * <h4 class="zh-CN">编码一个建模位并更新对应概率。</h4>
		 *
		 * @param models <span class="en-US">Adaptive probability array; each value represents P(bit=0) on a 2048-point scale.</span>
		 *               <span class="zh-CN">自适应概率数组，每项以 2048 为分母表示位为 0 的概率。</span>
		 * @param index  <span class="en-US">Probability slot for this decision.</span>
		 *               <span class="zh-CN">本次判定位使用的概率槽。</span>
		 * @param bit    <span class="en-US">Bit to encode, 0 or 1.</span>
		 *               <span class="zh-CN">待编码位，值为 0 或 1。</span>
		 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
		 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
		 */
		void bit(int[] models, int index, int bit) throws IOException {
			int probability = models[index];
			long bound = (this.range >>> 11) * probability;
			if (bit == 0) {
				this.range = bound;
				models[index] = probability + ((2048 - probability) >>> 5);
			} else {
				this.low += bound;
				this.range -= bound;
				models[index] = probability - (probability >>> 5);
			}
			if (this.range < 0x01000000L) {
				this.range <<= 8;
				this.shift();
			}
		}

		/**
		 * <h4 class="en-US">Encode a tree symbol from its highest bit.</h4>
		 * <h4 class="zh-CN">从最高位开始编码树符号。</h4>
		 *
		 * @param models <span class="en-US">Adaptive probability array; each value represents P(bit=0) on a 2048-point scale.</span>
		 *               <span class="zh-CN">自适应概率数组，每项以 2048 为分母表示位为 0 的概率。</span>
		 * @param offset <span class="en-US">Base index of the binary probability tree.</span>
		 *               <span class="zh-CN">二叉概率树的起始偏移。</span>
		 * @param bits   <span class="en-US">Number of bits in the encoded symbol.</span>
		 *               <span class="zh-CN">编码符号的位数。</span>
		 * @param value  <span class="en-US">Unsigned symbol value.</span>
		 *               <span class="zh-CN">无符号符号值。</span>
		 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
		 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
		 */
		void tree(int[] models, int offset, int bits, int value) throws IOException {
			int symbol = 1;
			for (int index = bits - 1; index >= 0; index--) {
				int bit = (value >>> index) & 1;
				this.bit(models, offset + symbol, bit);
				symbol = (symbol << 1) | bit;
			}
		}

		/**
		 * <h4 class="en-US">Encode a tree symbol from its lowest bit.</h4>
		 * <h4 class="zh-CN">从最低位开始编码树符号。</h4>
		 *
		 * @param models <span class="en-US">Adaptive probability array; each value represents P(bit=0) on a 2048-point scale.</span>
		 *               <span class="zh-CN">自适应概率数组，每项以 2048 为分母表示位为 0 的概率。</span>
		 * @param offset <span class="en-US">Base index of the binary probability tree.</span>
		 *               <span class="zh-CN">二叉概率树的起始偏移。</span>
		 * @param bits   <span class="en-US">Number of bits in the encoded symbol.</span>
		 *               <span class="zh-CN">编码符号的位数。</span>
		 * @param value  <span class="en-US">Unsigned symbol value.</span>
		 *               <span class="zh-CN">无符号符号值。</span>
		 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
		 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
		 */
		void reverse(int[] models, int offset, int bits, int value) throws IOException {
			int symbol = 1;
			for (int index = 0; index < bits; index++) {
				int bit = (value >>> index) & 1;
				this.bit(models, offset + symbol, bit);
				symbol = (symbol << 1) | bit;
			}
		}

		/**
		 * <h4 class="en-US">Encode equiprobable bits without adapting models.</h4>
		 * <h4 class="zh-CN">不调整概率模型，编码等概率位。</h4>
		 *
		 * @param bits  <span class="en-US">Number of bits in the encoded symbol.</span>
		 *              <span class="zh-CN">编码符号的位数。</span>
		 * @param value <span class="en-US">The requested low-order bits of this value, emitted highest first.</span>
		 *              <span class="zh-CN">输出本值的低 bits 位，最高位优先。</span>
		 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
		 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
		 */
		void direct(int bits, int value) throws IOException {
			for (int index = bits - 1; index >= 0; index--) {
				this.range >>>= 1;
				if (((value >>> index) & 1) != 0) this.low += this.range;
				if (this.range < 0x01000000L) {
					this.range <<= 8;
					this.shift();
				}
			}
		}

		/**
		 * <h4 class="en-US">Encode a match length through its selected length tree.</h4>
		 * <h4 class="zh-CN">选择对应长度树并编码匹配长度。</h4>
		 *
		 * @param models        <span class="en-US">Adaptive probability array; each value represents P(bit=0) on a 2048-point scale.</span>
		 *                      <span class="zh-CN">自适应概率数组，每项以 2048 为分母表示位为 0 的概率。</span>
		 * @param positionState <span class="en-US">Low pb bits of the current input position.</span>
		 *                      <span class="zh-CN">当前输入位置的低 pb 位。</span>
		 * @param length        <span class="en-US">Match length in 2..273.</span>
		 *                      <span class="zh-CN">范围为 2..273 的匹配长度。</span>
		 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
		 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
		 */
		void length(int[] models, int positionState, int length) throws IOException {
			int value = length - 2;
			if (value < 8) {
				this.bit(models, 0, 0);
				this.tree(models, 2 + positionState * 8, 3, value);
			} else {
				this.bit(models, 0, 1);
				if (value < 16) {
					this.bit(models, 1, 0);
					this.tree(models, 130 + positionState * 8, 3, value - 8);
				} else {
					this.bit(models, 1, 1);
					this.tree(models, 258, 8, value - 16);
				}
			}
		}

		/**
		 * <h4 class="en-US">Perform five final lower-bound shifts to terminate range data.</h4>
		 * <h4 class="zh-CN">执行五次最终下界移位，结束区间数据。</h4>
		 *
		 * <p class="en-US">This method neither emits a LZMA EOS symbol nor flushes or closes the delegate. The stream layer supplies the EOS policy and lifecycle.</p>
		 * <p class="zh-CN">本方法不编码 LZMA EOS 符号，也不刷新或关闭底层流。EOS 策略与生命周期由流层管理。</p>
		 *
		 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
		 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
		 */
		void finish() throws IOException {
			for (int index = 0; index < 5; index++) this.shift();
		}
	}
}
