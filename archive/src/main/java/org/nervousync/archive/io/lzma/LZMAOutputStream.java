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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * <h2 class="en-US">Bounded-memory LZMA1 encoder; ZIP method-14 framing by default.</h2>
 * <h2 class="zh-CN">有界内存的 LZMA1 编码器，默认输出 ZIP 方法 14 封装。</h2>
 * <p class="en-US">Uses greedy three-byte hash matching, ordinary matches and literals.
 * It does not implement the reference encoder's optimal parsing or repetition selection.
 * Levels 0..9 select dictionaries 64 KiB..8 MiB and match limits 32..273; level 0 still
 * compresses. Fixed properties: lc=3, lp=0, pb=2. Not thread safe.</p>
 * <p class="zh-CN">采用三字节哈希的贪心匹配，输出普通匹配及字面量，不实现参考编码器的最优解析
 * 与重复距离选择。等级 0..9 对应 64 KiB..8 MiB 字典及 32..273 匹配上限；等级 0 仍压缩。
 * 固定属性 lc=3、lp=0、pb=2。非线程安全。</p>
 * <p class="en-US">finish completes output without closing the parent; flush does not finish.
 * close finishes and closes the parent. After a write failure no more encoding is attempted.</p>
 * <p class="zh-CN">finish 完成输出但不关闭底层流；flush 不结束编码。close 完成并关闭底层流。
 * 写入失败后不再继续编码。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class LZMAOutputStream extends OutputStream {
	/**
	 * <span class="en-US">Original destination, responsible for container bytes after finish and closed by close.</span>
	 * <span class="zh-CN">原始目标流；finish 后容器可继续写入，close 会关闭它。</span>
	 */
	private final OutputStream parent;

	/**
	 * <span class="en-US">Buffered range-data destination; avoids per-byte writes to the original stream.</span>
	 * <span class="zh-CN">区间数据的缓冲输出层，避免逐字节写入原始目标流。</span>
	 */
	private final OutputStream output;

	/**
	 * <span class="en-US">Range encoder for literals, matches and optional EOS.</span>
	 * <span class="zh-CN">编码字面量、匹配及可选 EOS 的区间编码器。</span>
	 */
	private final LZMACodec.Encoder encoder;

	/**
	 * <span class="en-US">Stream-local models for fixed lc=3, lp=0 and pb=2.</span>
	 * <span class="zh-CN">固定 lc=3、lp=0、pb=2 的流独立概率模型。</span>
	 */
	private final LZMACodec.Models models = new LZMACodec.Models(3);

	/**
	 * <span class="en-US">Circular history containing previously consumed input bytes.</span>
	 * <span class="zh-CN">保存已消费输入字节的循环历史字典。</span>
	 */
	private final byte[] dictionary;

	/**
	 * <span class="en-US">Latest absolute input position for each three-byte hash; -1 means no candidate.</span>
	 * <span class="zh-CN">每个三字节哈希的最近绝对输入位置；-1 表示没有候选。</span>
	 */
	private final long[] heads = new long[65536];

	/**
	 * <span class="en-US">Input staging area: 64 KiB of processing space plus up to 273 lookahead bytes.</span>
	 * <span class="zh-CN">输入暂存区：64 KiB 处理空间加最多 273 字节前瞻。</span>
	 */
	private final byte[] buffer = new byte[65536 + 273];

	/**
	 * <span class="en-US">Level-specific maximum match length, from 32 through 273.</span>
	 * <span class="zh-CN">由等级决定的最大匹配长度，范围为 32 至 273。</span>
	 */
	private final int maxMatch;

	/**
	 * <span class="en-US">Emit an EOS match with zero-based distance 0xFFFFFFFF when finishing.</span>
	 * <span class="zh-CN">结束时是否输出零基距离为 0xFFFFFFFF 的 EOS 匹配。</span>
	 */
	private final boolean endMarker;

	/**
	 * <span class="en-US">Number of unprocessed input bytes at the beginning of buffer.</span>
	 * <span class="zh-CN">buffer 起始处尚未处理的输入字节数。</span>
	 */
	private int buffered;

	/**
	 * <span class="en-US">Next write index in the circular history dictionary.</span>
	 * <span class="zh-CN">循环历史字典的下一个写入位置。</span>
	 */
	private int position;

	/**
	 * <span class="en-US">Absolute number of bytes already encoded and entered into history.</span>
	 * <span class="zh-CN">已经编码并写入历史字典的绝对字节数。</span>
	 */
	private long consumed;

	/**
	 * <span class="en-US">Current 12-state LZMA history state.</span>
	 * <span class="zh-CN">当前的 LZMA 十二状态历史状态。</span>
	 */
	private int state;

	/**
	 * <span class="en-US">Most recent zero-based distance, used to condition the next matched literal.</span>
	 * <span class="zh-CN">最近的零基距离，用于下一匹配字面量的上下文。</span>
	 */
	private int rep0;

	/**
	 * <span class="en-US">Most recently consumed input byte as an unsigned value.</span>
	 * <span class="zh-CN">最近消费的输入字节，按无符号值保存。</span>
	 */
	private int previous;

	/**
	 * <span class="en-US">EOS policy, range termination and buffered flushing completed successfully.</span>
	 * <span class="zh-CN">EOS 策略、区间结束和缓冲刷新已成功完成。</span>
	 */
	private boolean finished;

	/**
	 * <span class="en-US">Parent closure has been initiated; further operations must fail.</span>
	 * <span class="zh-CN">已经开始关闭原始流，后续操作失败。</span>
	 */
	private boolean closed;

	/**
	 * <span class="en-US">First output failure; prevents retrying a partially updated coding state.</span>
	 * <span class="zh-CN">首次输出异常，避免重试已部分更新的编码状态。</span>
	 */
	private IOException failure;

	/**
	 * <h4 class="en-US">Write ZIP LZMA properties and an EOS-terminated LZMA1 stream.</h4>
	 * <h4 class="zh-CN">写入 ZIP LZMA 属性及以 EOS 结束的 LZMA1 数据流。</h4>
	 *
	 * <p class="en-US">The container must set the LZMA EOS flag and store its own CRC and sizes. This stream owns the delegate after successful construction; a constructor failure leaves it with the caller.</p>
	 * <p class="zh-CN">容器必须设置 LZMA EOS 标志并保存自身 CRC 和大小。构造成功后本流负责关闭底层流；构造失败时仍由调用方负责。</p>
	 *
	 * @param output <span class="en-US">Original destination stream.</span>
	 *               <span class="zh-CN">原始目标输出流。</span>
	 * @param level  <span class="en-US">Compression level 0..9; controls dictionary and match limit, not an external preset scale.</span>
	 *               <span class="zh-CN">压缩等级 0..9，控制字典及匹配上限，不对应外部编码器的预设等级。</span>
	 * @throws IOException              <span class="en-US">The delegate fails while writing encoded data.</span>
	 *                                  <span class="zh-CN">写入编码数据时底层流失败。</span>
	 * @throws IllegalArgumentException <span class="en-US">The level is outside 0..9.</span>
	 *                                  <span class="zh-CN">等级不在 0..9 范围内。</span>
	 */
	public LZMAOutputStream(OutputStream output, int level) throws IOException {
		this(output, level, 1, true);
	}

	/**
	 * <h4 class="en-US">Configure matching limits and write the selected framing header.</h4>
	 * <h4 class="zh-CN">设置匹配限制并写入所选封装头。</h4>
	 *
	 * <p class="en-US">ZIP writes compatibility version bytes 9/4 and a five-byte property count. Property 93 packs lc=3, lp=0, pb=2; Alone appends eight FF bytes for unknown size. No native SDK is invoked.</p>
	 * <p class="zh-CN">ZIP 写入兼容版本字节 9/4 和五字节属性长度。属性 93 打包 lc=3、lp=0、pb=2；Alone 另写八个 FF 表示未知大小。实现不调用原生 SDK。</p>
	 *
	 * @param parent    <span class="en-US">Original destination stream.</span>
	 *                  <span class="zh-CN">原始目标输出流。</span>
	 * @param level     <span class="en-US">Compression level 0..9; controls dictionary and match limit, not an external preset scale.</span>
	 *                  <span class="zh-CN">压缩等级 0..9，控制字典及匹配上限，不对应外部编码器的预设等级。</span>
	 * @param framing   <span class="en-US">0 for raw, 1 for ZIP properties, 2 for LZMA-Alone.</span>
	 *                  <span class="zh-CN">0 表示原始流，1 表示 ZIP 属性头，2 表示 LZMA-Alone。</span>
	 * @param endMarker <span class="en-US">Whether finish writes a LZMA EOS match.</span>
	 *                  <span class="zh-CN">finish 是否写入 LZMA EOS 匹配。</span>
	 * @throws IOException              <span class="en-US">The delegate fails while writing encoded data.</span>
	 *                                  <span class="zh-CN">写入编码数据时底层流失败。</span>
	 * @throws IllegalArgumentException <span class="en-US">The level is outside 0..9.</span>
	 *                                  <span class="zh-CN">等级不在 0..9 范围内。</span>
	 */
	private LZMAOutputStream(OutputStream parent, int level, int framing, boolean endMarker) throws IOException {
		this(parent, level, framing, endMarker, 1 << (16 + Math.min(level, 7)));
	}

	/**
	 * <h4 class="en-US">Initialize an encoder with an explicit bounded dictionary.</h4>
	 * <h4 class="zh-CN">使用显式的有界字典初始化编码器。</h4>
	 *
	 * <p class="en-US">The ordinary LZMA1 constructor derives dictionary size from level. The LZMA2 chunk factory instead fixes 64 KiB and disables EOS; raw construction writes no framing header.</p>
	 * <p class="zh-CN">普通 LZMA1 构造方法按等级选择字典；LZMA2 分块工厂固定使用 64 KiB 并禁用 EOS；原始流构造不写封装头。</p>
	 * @param parent <span class="en-US">Destination stream.</span>
	 * <span class="zh-CN">目标输出流。</span>
	 * @param level <span class="en-US">Match-search level from 0 through 9.</span>
	 * <span class="zh-CN">范围为 0 至 9 的匹配搜索等级。</span>
	 * @param framing <span class="en-US">0 for raw, 1 for ZIP properties, 2 for LZMA-Alone.</span>
	 * <span class="zh-CN">0 为原始流，1 为 ZIP 属性头，2 为 LZMA-Alone。</span>
	 * @param endMarker <span class="en-US">Whether finish emits LZMA1 EOS.</span>
	 * <span class="zh-CN">finish 是否输出 LZMA1 EOS。</span>
	 * @param dictionarySize <span class="en-US">Positive dictionary allocation selected by the internal caller.</span>
	 * <span class="zh-CN">由内部调用方选定的正数字典分配大小。</span>
	 * @throws IOException <span class="en-US">Writing a framing header fails.</span>
	 * <span class="zh-CN">写入封装头失败。</span>
	 * @throws IllegalArgumentException <span class="en-US">The level is outside 0 through 9.</span>
	 * <span class="zh-CN">等级不在 0 至 9 范围内。</span>
	 */
	private LZMAOutputStream(OutputStream parent, int level, int framing, boolean endMarker,
	                         int dictionarySize) throws IOException {
		this.parent = Objects.requireNonNull(parent, "output");
		if (level < 0 || level > 9) {
			throw new IllegalArgumentException("LZMA level must be 0..9");
		}
		this.dictionary = new byte[dictionarySize];
		this.maxMatch = Math.min(273, 32 + level * 32);
		this.endMarker = endMarker;
		Arrays.fill(this.heads, -1);
		if (framing == 1) {
			this.parent.write(new byte[]{
					9, 4, 5, 0
			});
		}
		if (framing != 0) {
			int size = this.dictionary.length;
			this.parent.write(new byte[]{
					93, (byte) size, (byte) (size >>> 8), (byte) (size >>> 16), (byte) (size >>> 24)
			});
			if (framing == 2) {
				for (int index = 0; index < 8; index++) {
					this.parent.write(255);
				}
			}
		}
		this.output = new BufferedOutputStream(this.parent, 8192);
		this.encoder = new LZMACodec.Encoder(this.output);
	}

	/**
	 * <h4 class="en-US">Create an encoder without any properties or container header.</h4>
	 * <h4 class="zh-CN">创建不写属性及容器头的编码器。</h4>
	 *
	 * <p class="en-US">The caller must save property byte 93 and dictionary size 1 &lt;&lt; (16 + min(level, 7)) for the raw decoder.</p>
	 * <p class="zh-CN">调用方必须为原始解码器另存属性字节 93 与字典大小 1 &lt;&lt; (16 + min(level, 7))。</p>
	 *
	 * @param output    <span class="en-US">Original destination stream.</span>
	 *                  <span class="zh-CN">原始目标输出流。</span>
	 * @param level     <span class="en-US">Compression level 0..9; controls dictionary and match limit, not an external preset scale.</span>
	 *                  <span class="zh-CN">压缩等级 0..9，控制字典及匹配上限，不对应外部编码器的预设等级。</span>
	 * @param endMarker <span class="en-US">Emit EOS; otherwise the caller must supply the uncompressed size separately.</span>
	 *                  <span class="zh-CN">是否输出 EOS；禁用 EOS 时调用方必须另行提供未压缩大小。</span>
	 * @return <span class="en-US">Raw encoder with lc=3, lp=0, pb=2.</span>
	 * <span class="zh-CN">lc=3、lp=0、pb=2 的原始编码器。</span>
	 * @throws IOException              <span class="en-US">The delegate fails while writing encoded data.</span>
	 *                                  <span class="zh-CN">写入编码数据时底层流失败。</span>
	 * @throws IllegalArgumentException <span class="en-US">The level is outside 0..9.</span>
	 *                                  <span class="zh-CN">等级不在 0..9 范围内。</span>
	 */
	public static LZMAOutputStream raw(OutputStream output, int level, boolean endMarker) throws IOException {
		return new LZMAOutputStream(output, level, 0, endMarker);
	}

	/**
	 * <h4 class="en-US">Write an unknown-size 13-byte LZMA-Alone header followed by LZMA1 and EOS.</h4>
	 * <h4 class="zh-CN">写入未知大小的 13 字节 LZMA-Alone 头，随后输出 LZMA1 和 EOS。</h4>
	 *
	 * @param output <span class="en-US">Original destination stream.</span>
	 *               <span class="zh-CN">原始目标输出流。</span>
	 * @param level  <span class="en-US">Compression level 0..9; controls dictionary and match limit, not an external preset scale.</span>
	 *               <span class="zh-CN">压缩等级 0..9，控制字典及匹配上限，不对应外部编码器的预设等级。</span>
	 * @return <span class="en-US">Encoder for an independent .lzma stream.</span>
	 * <span class="zh-CN">独立 .lzma 数据流编码器。</span>
	 * @throws IOException              <span class="en-US">The delegate fails while writing encoded data.</span>
	 *                                  <span class="zh-CN">写入编码数据时底层流失败。</span>
	 * @throws IllegalArgumentException <span class="en-US">The level is outside 0..9.</span>
	 *                                  <span class="zh-CN">等级不在 0..9 范围内。</span>
	 */
	public static LZMAOutputStream alone(OutputStream output, int level) throws IOException {
		return new LZMAOutputStream(output, level, 2, true);
	}

	/**
	 * <h4 class="en-US">Reject writes after finish, closure or an earlier output failure.</h4>
	 * <h4 class="zh-CN">拒绝完成、关闭或首次输出失败后的写入。</h4>
	 *
	 * @throws IOException <span class="en-US">The output is no longer writable.</span>
	 *                     <span class="zh-CN">输出已不可写。</span>
	 */
	private void ensureWritable() throws IOException {
		if (this.closed || this.finished) {
			throw new IOException("LZMA output closed/finished");
		}
		if (this.failure != null) {
			throw this.failure;
		}
	}

	/**
	 * <h4 class="en-US">Write one uncompressed source byte.</h4>
	 * <h4 class="zh-CN">写入一个未压缩源字节。</h4>
	 * @param value <span class="en-US">Only the lowest eight bits are encoded.</span>
	 * <span class="zh-CN">仅编码最低八位。</span>
	 * @throws IOException <span class="en-US">The encoder is no longer writable or output fails.</span>
	 * <span class="zh-CN">编码器不可继续写入，或输出失败。</span>
	 */
	@Override
	public void write(int value) throws IOException {
		byte[] one = {
				(byte) value
		};
		this.write(one, 0, 1);
	}

	/**
	 * <h4 class="en-US">Buffer and encode uncompressed source bytes.</h4>
	 * <h4 class="zh-CN">缓冲并编码未压缩源字节。</h4>
	 *
	 * <p class="en-US">Keep sufficient lookahead for greedy matching. Retain the first IOException so partially updated encoding state cannot be retried.</p>
	 * <p class="zh-CN">为贪心匹配保留足够的前瞻数据；保存首次 IOException，避免重试已经部分变化的编码状态。</p>
	 * @param bytes <span class="en-US">Source array.</span>
	 * <span class="zh-CN">源数组。</span>
	 * @param offset <span class="en-US">First source index.</span>
	 * <span class="zh-CN">源起始下标。</span>
	 * @param length <span class="en-US">Number of source bytes.</span>
	 * <span class="zh-CN">源字节数。</span>
	 * @throws IOException <span class="en-US">The encoder is no longer writable or output fails.</span>
	 * <span class="zh-CN">编码器不可继续写入，或输出失败。</span>
	 * @throws IndexOutOfBoundsException <span class="en-US">The array range is invalid.</span>
	 * <span class="zh-CN">数组范围非法。</span>
	 */
	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		this.ensureWritable();
		try {
			while (length > 0) {
				int count = Math.min(length, this.buffer.length - this.buffered);
				System.arraycopy(bytes, offset, this.buffer, this.buffered, count);
				this.buffered += count;
				offset += count;
				length -= count;
				if (this.buffered == this.buffer.length) {
					this.process(false);
				}
			}
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <h4 class="en-US">Hash the next three staged bytes to a 16-bit candidate slot.</h4>
	 * <h4 class="zh-CN">将接下来的三个暂存字节哈希为 16 位候选槽。</h4>
	 *
	 * @param offset <span class="en-US">Index with at least three available bytes.</span>
	 *               <span class="zh-CN">至少还有三个可用字节的下标。</span>
	 * @return <span class="en-US">Hash index in 0..65535.</span>
	 * <span class="zh-CN">范围为 0..65535 的哈希下标。</span>
	 */
	private int hash(int offset) {
		return (((this.buffer[offset] & 255) * 251
				+ (this.buffer[offset + 1] & 255)) * 251
				+ (this.buffer[offset + 2] & 255)) & 65535;
	}

	/**
	 * <h4 class="en-US">Read one byte at a positive backward offset in the circular dictionary.</h4>
	 * <h4 class="zh-CN">按正向回溯偏移读取循环字典中的一个字节。</h4>
	 *
	 * @param distance <span class="en-US">Actual backward offset, not the zero-based wire distance; validated by matching logic.</span>
	 *                 <span class="zh-CN">实际回溯偏移，并非线格式的零基距离，由匹配逻辑验证。</span>
	 * @return <span class="en-US">Unsigned dictionary byte.</span>
	 * <span class="zh-CN">字典中的无符号字节。</span>
	 */
	private int history(int distance) {
		int index = this.position - distance;
		if (index < 0) {
			index += this.dictionary.length;
		}
		return this.dictionary[index] & 255;
	}

	/**
	 * <h4 class="en-US">Encode a literal using previous-byte and optional match-byte context.</h4>
	 * <h4 class="zh-CN">结合前一字节及可选匹配字节上下文编码字面量。</h4>
	 *
	 * <p class="en-US">After a match, matching high bits use the matched-literal branch. The first differing bit switches all remaining bits back to the ordinary literal tree.</p>
	 * <p class="zh-CN">匹配之后，相同的高位使用匹配字面量分支；遇到首个不同位后，剩余位改用普通字面量树。</p>
	 *
	 * @param value <span class="en-US">Unsigned byte to encode.</span>
	 *              <span class="zh-CN">待编码的无符号字节。</span>
	 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
	 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
	 */
	private void literal(int value) throws IOException {
		int positionState = (int) this.consumed & 3;
		this.encoder.bit(this.models.match, (this.state << 4) + positionState, 0);
		int base = (this.previous >>> 5) * 768, symbol = 1;
		boolean matched = this.state >= 7;
		int matchByte = matched ? this.history(this.rep0 + 1) : 0;
		for (int index = 7; index >= 0; index--) {
			int bit = (value >>> index) & 1;
			int matchBit = (matchByte >>> index) & 1;
			this.encoder.bit(this.models.literal, base + (matched ? ((1 + matchBit) << 8) : 0) + symbol, bit);
			symbol = (symbol << 1) | bit;
			if (bit != matchBit) {
				matched = false;
			}
		}
		this.state = LZMACodec.literalState(this.state);
	}

	/**
	 * <h4 class="en-US">Encode an ordinary match or the reserved EOS match.</h4>
	 * <h4 class="zh-CN">编码普通匹配或保留的 EOS 匹配。</h4>
	 *
	 * <p class="en-US">Distance slots encode the leading bits. Smaller suffixes use reverse probability trees; large suffixes combine direct bits with four modeled alignment bits.</p>
	 * <p class="zh-CN">距离槽表示高位。小距离后缀使用逆序概率树；大距离后缀由直接位与四个建模对齐位组合。</p>
	 *
	 * @param distance <span class="en-US">Zero-based distance; -1 means the unsigned 0xFFFFFFFF EOS distance.</span>
	 *                 <span class="zh-CN">零基距离；-1 表示无符号 0xFFFFFFFF 的 EOS 距离。</span>
	 * @param length   <span class="en-US">Match length 2..273; EOS uses length 2.</span>
	 *                 <span class="zh-CN">匹配长度 2..273；EOS 使用长度 2。</span>
	 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
	 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
	 */
	private void match(int distance, int length) throws IOException {
		int positionState = (int) this.consumed & 3;
		this.encoder.bit(this.models.match, (this.state << 4) + positionState, 1);
		this.encoder.bit(this.models.rep, this.state, 0);
		this.encoder.length(this.models.length, positionState, length);
		this.state = this.state < 7 ? 7 : 10;
		int slot;
		if (distance >= 0 && distance < 4) {
			slot = distance;
		} else {
			int highest = 31 - Integer.numberOfLeadingZeros(distance);
			slot = highest * 2 + ((distance >>> (highest - 1)) & 1);
		}
		this.encoder.tree(this.models.slots, Math.min(length - 2, 3) * 64, 6, slot);
		if (slot >= 4) {
			int bits = (slot >>> 1) - 1;
			int reduced = distance - ((2 | (slot & 1)) << bits);
			if (slot < 14)
			{
				this.encoder.reverse(this.models.distances, ((2 | (slot & 1)) << bits) - slot - 1, bits, reduced);
			} else {
				this.encoder.direct(bits - 4, reduced >>> 4);
				this.encoder.reverse(this.models.align, 0, 4, reduced & 15);
			}
		}
		this.rep0 = distance;
	}

	/**
	 * <h4 class="en-US">Greedily encode staged input and retain any necessary lookahead.</h4>
	 * <h4 class="zh-CN">以贪心策略编码暂存输入，并保留所需前瞻。</h4>
	 *
	 * <p class="en-US">Only the latest hash candidate is tested. A match of at least three bytes is emitted; otherwise one literal is used. Dictionary and hash history advance for every consumed byte.</p>
	 * <p class="zh-CN">只检查最新哈希候选。至少匹配三个字节才输出匹配，否则输出一个字面量。每消费一个字节都推进字典及哈希历史。</p>
	 *
	 * @param all <span class="en-US">Process all remaining input when finishing; otherwise retain lookahead.</span>
	 *            <span class="zh-CN">结束时处理全部剩余输入，否则保留前瞻。</span>
	 * @throws IOException <span class="en-US">The delegate fails or the absolute input position would overflow.</span>
	 *                     <span class="zh-CN">底层输出失败，或绝对输入位置将溢出。</span>
	 */
	private void process(boolean all) throws IOException {
		int offset = 0;
		int limit = all ? this.buffered : this.buffered - 273;
		while (offset < limit) {
			int length = 0, distance = 0;
			int available = Math.min(this.maxMatch, this.buffered - offset);
			if (available >= 3) {
				long candidate = this.heads[this.hash(offset)];
				long back = this.consumed - candidate;
				if (candidate >= 0 && back > 0 && back <= this.dictionary.length) {
					distance = (int) back;
					while (length < available) {
						int expected = length < distance ? this.history(distance - length)
								: this.buffer[offset + length - distance] & 255;
						if ((this.buffer[offset + length] & 255) != expected) {
							break;
						}
						length++;
					}
				}
			}
			if (length >= 3) {
				this.match(distance - 1, length);
			} else {
				length = 1;
				this.literal(this.buffer[offset] & 255);
			}
			for (int index = 0; index < length; index++) {
				if (offset + 2 < this.buffered) {
					this.heads[this.hash(offset)] = this.consumed;
				}
				this.previous = this.buffer[offset++] & 255;
				this.dictionary[this.position++] = (byte) this.previous;
				if (this.position == this.dictionary.length) {
					this.position = 0;
				}
				if (this.consumed == Long.MAX_VALUE) {
					throw new IOException("LZMA input position overflow");
				}
				this.consumed++;
			}
		}
		System.arraycopy(this.buffer, offset, this.buffer, 0, this.buffered - offset);
		this.buffered -= offset;
	}

	/**
	 * <h4 class="en-US">Finish staged input, optional EOS and range data while leaving the parent open.</h4>
	 * <h4 class="zh-CN">完成暂存输入、可选 EOS 及区间数据，保留原始流。</h4>
	 *
	 * <p class="en-US">Repeated successful calls add no output. A failure is retained; callers must not use a partial result as a complete stream.</p>
	 * <p class="zh-CN">成功后重复调用不增加输出。失败会保存，调用方不能将部分结果当作完整数据流。</p>
	 *
	 * @throws IOException <span class="en-US">The delegate fails while writing encoded data.</span>
	 *                     <span class="zh-CN">写入编码数据时底层流失败。</span>
	 */
	public void finish() throws IOException {
		if (this.closed) {
			throw new IOException("LZMA output closed");
		}
		if (this.failure != null) {
			throw this.failure;
		}
		if (this.finished) {
			return;
		}
		try {
			this.process(true);
			if (this.endMarker) {
				this.match(-1, 2);
			}
			this.encoder.finish();
			this.output.flush();
			this.finished = true;
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}
	/**
	 * <h4 class="en-US">Flush encoded output without finishing the LZMA1 stream.</h4>
	 * <h4 class="zh-CN">刷新已编码输出，不结束 LZMA1 数据流。</h4>
	 *
	 * <p class="en-US">Pending match lookahead may remain buffered; unlike LZMA2 flush,
	 * this operation does not complete an independently decodable chunk.</p>
	 * <p class="zh-CN">匹配所需的前瞻输入可能仍保留在缓冲区；本操作与 LZMA2 的 flush 不同，
	 * 不完成可独立解码的分块。</p>
	 * @throws IOException <span class="en-US">The stream is closed, has failed, or flushing fails.</span>
	 * <span class="zh-CN">流已关闭、已有保存的失败，或刷新操作失败。</span>
	 */
	@Override
	public void flush() throws IOException {
		if (this.closed) {
			throw new IOException("LZMA output closed");
		}
		if (this.failure != null) {
			throw this.failure;
		}
		try {
			this.output.flush();
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}
	/**
	 * <h4 class="en-US">Finish when possible and close the original destination once.</h4>
	 * <h4 class="zh-CN">在可行时完成编码，并只关闭一次原始目标流。</h4>
	 *
	 * <p class="en-US">A retained output failure prevents further encoding. When this call
	 * encounters both finish and close failures, suppress the latter on the former.
	 * Mark closed even if finishing or delegate closure fails.</p>
	 * <p class="zh-CN">已有保存的输出失败时不再继续编码；本次调用同时发生结束和关闭异常时，
	 * 将关闭异常加入结束异常的 suppressed。结束或底层关闭失败也标记为已关闭。</p>
	 * @throws IOException <span class="en-US">Finishing output or closing the destination fails.</span>
	 * <span class="zh-CN">完成输出或关闭目标流失败。</span>
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		IOException error = null;
		try {
			if (this.failure == null) {
				this.finish();
			}
		} catch (IOException failure) {
			error = failure;
		} finally {
			this.closed = true;
			try {
				this.parent.close();
			} catch (IOException closeFailure) {
				if (error == null) {
					error = closeFailure;
				} else if (error != closeFailure) {
					error.addSuppressed(closeFailure);
				}
			}
		}
		if (error != null) {
			throw error;
		}
	}

	/**
	 * <h4 class="en-US">Create an independent raw encoder for one LZMA2 chunk.</h4>
	 * <h4 class="zh-CN">创建单个 LZMA2 分块使用的独立原始编码器。</h4>
	 *
	 * <p class="en-US">The caller supplies a bounded trial destination. Do not use the ordinary raw factory here: its dictionary varies with level and its EOS policy differs.</p>
	 * <p class="zh-CN">调用方提供有界试压缩目标；这里使用专用工厂，以固定字典并明确禁用 EOS。</p>
	 * @param output <span class="en-US">Delegate output stream.</span>
	 * <span class="zh-CN">底层输出流。</span>
	 * @param level <span class="en-US">Match-search level from 0 through 9.</span>
	 * <span class="zh-CN">范围为 0 至 9 的匹配搜索等级。</span>
	 * @return <span class="en-US">Encoder with a 64 KiB dictionary and no LZMA1 EOS.</span>
	 * <span class="zh-CN">使用 64 KiB 字典且不输出 LZMA1 EOS 的编码器。</span>
	 * @throws IOException <span class="en-US">Encoder initialization fails.</span>
	 * <span class="zh-CN">编码器初始化失败。</span>
	 */
	static LZMAOutputStream chunk(OutputStream output, int level) throws IOException {
		return new LZMAOutputStream(output, level, 0, false, 65536);
	}
}
