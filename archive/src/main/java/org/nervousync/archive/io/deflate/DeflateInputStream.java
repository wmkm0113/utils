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

package org.nervousync.archive.io.deflate;

import org.nervousync.tree.huffman.HuffmanTree;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * <h2 class="en-US">Streaming decoder for raw DEFLATE and Deflate64 data</h2>
 * <h2 class="zh-CN">原始 DEFLATE 和 Deflate64 数据的流式解码器</h2>
 *
 * <p class="en-US">
 * Both formats share stored-block parsing, canonical Huffman decoding,
 * and LZ77 history copying. Deflate64 extends the history window to 64 KiB, enables
 * distance symbols 30 and 31, and gives length symbol 285 sixteen extra bits.
 * The source must contain raw compressed bytes: this class does not
 * parse ZIP, zlib, or GZIP headers and does not check their checksums. The caller
 * must enforce output-size and compression-ratio limits and verify entry metadata.
 * Decoding stops after the final block without intentionally reading
 * subsequent source bytes. Padding bits in the last consumed byte are ignored.
 * If the caller wraps the source in a buffered stream, that wrapper may prefetch
 * subsequent bytes; keep using the same wrapper when inspecting trailing data.
 * Not thread safe. A successful constructor transfers ownership of
 * the source to this stream. Closing releases the source without draining unread data.
 * </p>
 * <p class="zh-CN">
 * 两种格式共用未压缩块解析、规范 Huffman 解码及 LZ77 历史数据复制。Deflate64 将历史窗口扩展为 64 KiB，
 * 启用距离符号 30、31，并为长度符号 285 增加 16 个附加位。输入必须是原始压缩数据。本类不解析 ZIP、zlib 或 GZIP 容器头，
 * 也不校验其校验和。输出大小、压缩比限制及条目元数据校验由调用方负责。解码在最终块结束时停止，不主动读取后续字节；最后一个已读取字节中的
 * 填充位会被忽略。如果调用方使用缓冲输入流，其缓冲层可能预读后续字节，检查尾随数据时应继续使用同一个缓冲流。
 * 本类不是线程安全的。构造成功后由本流负责关闭底层输入流；close() 不会主动解压或验证尚未读取的数据。
 * </p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class DeflateInputStream extends InputStream {
	/**
	 * <span class="en-US">Base lengths for symbols 257..285; index = symbol - 257.
	 * Deflate64 handles symbol 285 separately instead of using the final entry.</span>
	 * <span class="zh-CN">长度符号 257..285 的基础长度，下标为符号值减 257。
	 * Deflate64 单独处理符号 285，不使用表中的最后一项。</span>
	 */
	static final int[] LENGTH = {3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258};
	/**
	 * <span class="en-US">Extra-bit counts paired with the base-length table.</span>
	 * <span class="zh-CN">与基础长度表对应的附加位数量。</span>
	 */
	static final int[] LENGTH_BITS = {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0};
	/**
	 * <span class="en-US">Base distances indexed by distance symbol. Entries 30 and 31
	 * extend the distance range beyond 32 KiB and are valid only for Deflate64.</span>
	 * <span class="zh-CN">按距离符号索引的基础距离。第 30、31 项将回溯范围扩展到
	 * 32 KiB 以上，仅供 Deflate64 使用。</span>
	 */
	static final int[] DISTANCE = {1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577, 32769, 49153};
	/**
	 * <span class="en-US">Extra-bit counts paired with the base-distance table.</span>
	 * <span class="zh-CN">与基础距离表对应的附加位数量。</span>
	 */
	static final int[] DISTANCE_BITS = {0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14};
	/**
	 * <span class="en-US">Wire order of the 19 code-length symbols in a dynamic block.</span>
	 * <span class="zh-CN">动态块中 19 个码长符号的传输顺序，并非符号的自然顺序。</span>
	 */
	private static final int[] ORDER = {16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};
	/**
	 * <span class="en-US">Shared immutable literal/length tree for fixed-code blocks.</span>
	 * <span class="zh-CN">固定编码块共用的只读字面量/长度 Huffman 树。</span>
	 */
	private static final HuffmanTree.Book FIXED_LITERAL;
	/**
	 * <span class="en-US">Shared immutable distance tree for fixed-code blocks.</span>
	 * <span class="zh-CN">固定编码块共用的只读距离 Huffman 树。</span>
	 */
	private static final HuffmanTree.Book FIXED_DISTANCE;

	static {
		try {
			// 固定字面量/长度码按符号范围使用 8、9、7、8 位；固定距离码统一使用 5 位。
			int[] lengths = new int[288];
			Arrays.fill(lengths, 0, 144, 8);
			Arrays.fill(lengths, 144, 256, 9);
			Arrays.fill(lengths, 256, 280, 7);
			Arrays.fill(lengths, 280, 288, 8);
			FIXED_LITERAL = buildBook(lengths, 15, false, false);
			lengths = new int[32];
			Arrays.fill(lengths, 5);
			FIXED_DISTANCE = buildBook(lengths, 15, false, false);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * <span class="en-US">Source of raw compressed bytes, owned by this stream.</span>
	 * <span class="zh-CN">由本流负责关闭的原始压缩数据输入流。</span>
	 */
	private final InputStream source;
	/**
	 * <span class="en-US">Selects Deflate64 window, length, and distance rules.</span>
	 * <span class="zh-CN">是否启用 Deflate64 的窗口、长度和距离规则。</span>
	 */
	private final boolean deflate64;
	/**
	 * <span class="en-US">Circular history buffer. Its power-of-two size allows
	 * wraparound using a bit mask instead of a remainder operation.</span>
	 * <span class="zh-CN">循环历史窗口。长度为 2 的幂，可通过按位与实现索引回绕。</span>
	 */
	private final byte[] window;
	/**
	 * <span class="en-US">Incremental decoder state:</span>
	 * <span class="zh-CN">跨多次 read() 调用保留的解码状态：</span>
	 * <ul>
	 *   <li>bits: buffered source bits; the next bit is the least significant bit.
	 *       位缓存，下一位位于最低位。</li>
	 *   <li>bitCount: number of valid buffered bits. 位缓存中的有效位数。</li>
	 *   <li>position: next history-buffer write index. 历史窗口的下一个写入位置。</li>
	 *   <li>history: valid history size, capped at the window size.
	 *       有效历史数据量，最大为窗口长度。</li>
	 *   <li>stored: unread bytes in the current stored block.
	 *       当前未压缩块中尚未读取的字节数。</li>
	 *   <li>remaining: bytes still to copy for the current LZ77 match.
	 *       当前 LZ77 匹配尚需复制的字节数。</li>
	 *   <li>distance: backward distance for the current match.
	 *       当前匹配的回溯距离。</li>
	 * </ul>
	 */
	private int bits, bitCount, position, history, stored, remaining, distance;
	/**
	 * <span class="en-US">last records BFINAL; block means a Huffman block is active;
	 * finished means the final block has been consumed; closed marks source closure.
	 * Reading BFINAL alone does not mean the final block's data has been consumed.</span>
	 * <span class="zh-CN">last 保存 BFINAL；block 表示正在处理 Huffman 编码块；
	 * finished 表示最终块已消费完毕；closed 表示输入流已关闭。
	 * 读到 BFINAL 标志时，最终块的数据仍可能尚未消费。</span>
	 */
	private boolean last, block, finished, closed;
	/**
	 * <span class="en-US">Literal/length and distance trees for the current Huffman block.</span>
	 * <span class="zh-CN">当前 Huffman 编码块的字面量/长度树及距离树。</span>
	 */
	private HuffmanTree.Book literals, distances;

	/**
	 * <p class="en-US">Create a decoder without consuming source data</p>
	 * <p class="zh-CN">创建解码器，构造时不读取压缩数据</p>
	 *
	 * @param source    <span class="en-US">Raw compressed source, closed by this stream</span>
	 *                  <span class="zh-CN">原始压缩数据源，由本流负责关闭</span>
	 * @param deflate64 <span class="en-US">true for ZIP method 9; false for method 8</span>
	 *                  <span class="zh-CN">true 使用 ZIP 方法 9；false 使用方法 8</span>
	 * @throws NullPointerException <span class="en-US">If source is null</span>
	 *                              <span class="zh-CN">数据源为 null 时抛出</span>
	 */
	public DeflateInputStream(final InputStream source, final boolean deflate64) {
		this.source = Objects.requireNonNull(source, "source");
		this.deflate64 = deflate64;
		this.window = new byte[this.deflate64 ? 65536 : 32768];
	}

	/**
	 * <p class="en-US">Read one decompressed byte</p>
	 * <p class="zh-CN">读取一个解压后的字节</p>
	 *
	 * @return <span class="en-US">An unsigned byte in 0..255, or -1 at stream end</span>
	 * <span class="zh-CN">0..255 的无符号字节，流结束时返回 -1</span>
	 * @throws IOException <span class="en-US">If closed, truncated, malformed, or source reading fails</span>
	 *                     <span class="zh-CN">流已关闭、数据截断、格式错误或底层读取失败时抛出</span>
	 */
	@Override
	public int read() throws IOException {
		byte[] one = new byte[1];
		return this.read(one, 0, 1) < 0 ? -1 : one[0] & 255;
	}

	/**
	 * <p class="en-US">Decode into a caller-provided buffer</p>
	 * <p class="zh-CN">将解压数据写入调用方提供的缓冲区</p>
	 *
	 * <p class="en-US">A match or stored block may span multiple calls. Pending bytes
	 * are emitted before another Huffman symbol or block header is decoded.</p>
	 * <p class="zh-CN">一次匹配或一个未压缩块可跨多次调用。先输出尚未消费的数据，
	 * 再解析下一个 Huffman 符号或块头。</p>
	 *
	 * @param bytes  <span class="en-US">Destination buffer</span><span class="zh-CN">目标缓冲区</span>
	 * @param offset <span class="en-US">First destination index</span><span class="zh-CN">起始写入位置</span>
	 * @param length <span class="en-US">Maximum bytes to decode</span><span class="zh-CN">最多解码字节数</span>
	 * @return <span class="en-US">Bytes decoded, zero for a zero-length request, or -1 at EOF</span>
	 * <span class="zh-CN">解码字节数；请求长度为零时返回 0；流结束时返回 -1</span>
	 * @throws NullPointerException      <span class="en-US">If bytes is null</span><span class="zh-CN">缓冲区为 null 时抛出</span>
	 * @throws IndexOutOfBoundsException <span class="en-US">If the destination range is invalid</span>
	 *                                   <span class="zh-CN">目标缓冲区范围非法时抛出</span>
	 * @throws IOException               <span class="en-US">If closed, truncated, malformed, or source reading fails</span>
	 *                                   <span class="zh-CN">流已关闭、数据截断、格式错误或底层读取失败时抛出</span>
	 */
	@Override
	public int read(final byte[] bytes, final int offset, final int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		if (this.closed) {
			throw new IOException("Stream closed");
		}
		if (length == 0) {
			return 0;
		}
		int count = 0;
		while (count < length) {
			int value;
			if (this.remaining > 0) {
				value = this.window[(this.position - this.distance) & (this.window.length - 1)] & 255;
				this.remaining--;
			} else if (this.stored > 0) {
				value = this.readBits(8);
				this.stored--;
			} else {
				if (!this.block) {
					if (this.finished || this.last) {
						this.finished = true;
						break;
					}

					this.last = this.readBits(1) != 0;
					int type = this.readBits(2);
					if (type == 0) {
						this.bits = this.bitCount = 0;
						this.stored = this.readBits(16);
						if ((this.stored ^ this.readBits(16)) != 65535) {
							throw invalid("Stored block length");
						}
						if (this.stored != 0) {
							this.block = false;
						}
						continue;
					} else if (type == 1) {
						this.literals = FIXED_LITERAL;
						this.distances = FIXED_DISTANCE;
					} else if (type == 2) {
						this.readTrees();
					} else {
						throw invalid("Reserved block type");
					}
					this.block = true;
				}
				int symbol = this.decodeSymbol(this.literals);
				if (symbol == 256) {
					this.block = false;
					continue;
				}
				if (symbol < 256) value = symbol;
				else {
					if (symbol > 285) {
						throw invalid("Reserved length code");
					}
					int index = symbol - 257;
					this.remaining = this.deflate64 && symbol == 285
							? 3 + this.readBits(16)
							: LENGTH[index] + this.readBits(LENGTH_BITS[index]);
					int code = this.decodeSymbol(this.distances);
					if (code >= (this.deflate64 ? 32 : 30)) {
						throw invalid("Reserved distance code");
					}
					this.distance = DISTANCE[code] + this.readBits(DISTANCE_BITS[code]);
					if (this.distance > this.history) {
						throw invalid("Distance exceeds available history");
					}
					continue;
				}
			}
			bytes[offset + count++] = (byte) value;
			this.window[this.position] = (byte) value;
			this.position = (this.position + 1) & (this.window.length - 1);
			if (this.history < this.window.length) {
				this.history++;
			}
		}
		return count == 0 ? -1 : count;
	}

	/**
	 * <p class="en-US">Read the two Huffman trees of a dynamic block</p>
	 * <p class="zh-CN">读取动态块的字面量/长度树和距离树</p>
	 *
	 * <p class="en-US">A temporary code-length tree decodes the combined sequence of
	 * literal/length and distance code lengths. Repeat counts are checked before filling.</p>
	 * <p class="zh-CN">先建立临时码长树，再解码两组目标树的连续码长序列；
	 * 每次展开重复码长之前均检查边界。</p>
	 *
	 * @throws IOException <span class="en-US">If source data or a Huffman tree is invalid</span>
	 *                     <span class="zh-CN">输入截断、读取失败或 Huffman 树非法时抛出</span>
	 */
	private void readTrees() throws IOException {
		int literalCount = 257 + this.readBits(5),
				distanceCount = 1 + this.readBits(5),
				codeCount = 4 + this.readBits(4);
		if (literalCount > 286) {
			throw invalid("Too many literal codes");
		}
		int[] lengths = new int[19];
		for (int i = 0; i < codeCount; i++) {
			lengths[ORDER[i]] = this.readBits(3);
		}
		HuffmanTree.Book codes = buildBook(lengths, 7, false, false);
		lengths = new int[literalCount + distanceCount];
		int index = 0;
		while (index < lengths.length) {
			int code = this.decodeSymbol(codes);
			if (code < 16) lengths[index++] = code;
			else {
				if (code == 16 && index == 0) {
					throw invalid("Missing previous code length");
				}
				int value = code == 16 ? lengths[index - 1] : 0;
				int repeat;
				if (code == 16) {
					repeat = 3 + this.readBits(2);
				} else if (code == 17) {
					repeat = 3 + this.readBits(3);
				} else {
					repeat = 11 + this.readBits(7);
				}
				if (repeat > lengths.length - index) {
					throw invalid("Code length repeat overflow");
				}
				Arrays.fill(lengths, index, index + repeat, value);
				index += repeat;
			}
		}
		if (lengths[256] == 0) throw invalid("Missing end-of-block code");
		this.literals = buildBook(Arrays.copyOf(lengths, literalCount), 15, true, false);
		this.distances = buildBook(Arrays.copyOfRange(lengths, literalCount, lengths.length), 15, true, true);
	}

	/**
	 * <p class="en-US">Consume a low-bit-first unsigned value from the bit cache</p>
	 * <p class="zh-CN">从位缓存中按低位优先读取无符号数值</p>
	 *
	 * @param count <span class="en-US">Requested bit count; internal callers use 0..16</span>
	 *              <span class="zh-CN">读取位数，内部调用范围为 0..16</span>
	 * @return <span class="en-US">Decoded value; zero when count is zero</span>
	 * <span class="zh-CN">读取的数值；位数为零时返回 0</span>
	 * @throws EOFException <span class="en-US">If the source ends before all bits are read</span>
	 *                      <span class="zh-CN">尚未读满所需位数便到达输入末尾时抛出</span>
	 * @throws IOException  <span class="en-US">If source reading fails</span>
	 *                      <span class="zh-CN">底层输入流读取失败时抛出</span>
	 */
	private int readBits(final int count) throws IOException {
		while (this.bitCount < count) {
			int next = this.source.read();
			if (next < 0) {
				throw new EOFException("Truncated DEFLATE stream");
			}
			this.bits |= next << this.bitCount;
			this.bitCount += 8;
		}
		int value = this.bits & ((1 << count) - 1);
		this.bits >>>= count;
		this.bitCount -= count;
		return value;
	}

	/**
	 * <p class="en-US">Close the source without decoding remaining data</p>
	 * <p class="zh-CN">关闭底层输入流，不主动解码剩余数据</p>
	 *
	 * @throws IOException <span class="en-US">If source closure fails</span>
	 *                     <span class="zh-CN">关闭底层输入流失败时抛出</span>
	 */
	@Override
	public void close() throws IOException {
		if (!this.closed) {
			this.closed = true;
			this.source.close();
		}
	}

	/**
	 * <span class="en-US">Build an exception with a consistent malformed-data prefix.</span>
	 * <span class="zh-CN">创建带有统一格式错误前缀的异常。</span>
	 *
	 * @param detail <span class="en-US">Reason for rejection</span><span class="zh-CN">拒绝解码的原因</span>
	 * @return <span class="en-US">Malformed-stream exception</span><span class="zh-CN">压缩流格式异常</span>
	 */
	private static IOException invalid(final String detail) {
		return new IOException("Invalid DEFLATE stream: " + detail);
	}

	/**
	 * <p class="en-US">Validate format-specific code-length rules before delegating
	 * canonical table construction to the shared HuffmanTree component.</p>
	 * <p class="zh-CN">先检查 Deflate 专属的码长规则，再交由共用的 HuffmanTree
	 * 组件建立规范码表；不在归档层重复实现建表算法。</p>
	 *
	 * @param lengths     <span class="en-US">Code lengths indexed by symbol; zero means unused</span>
	 *                    <span class="zh-CN">按符号索引的码长，零表示未使用</span>
	 * @param maxLength   <span class="en-US">Maximum permitted code length: 7 or 15</span>
	 *                    <span class="zh-CN">允许的最大码长：7 或 15</span>
	 * @param allowSingle <span class="en-US">Allow an incomplete one-symbol, one-bit table</span>
	 *                    <span class="zh-CN">是否允许只有一个 1 位符号的不完整表</span>
	 * @param allowEmpty  <span class="en-US">Allow an empty distance table, represented by null</span>
	 *                    <span class="zh-CN">是否允许空距离表，以 null 表示</span>
	 * @return <span class="en-US">Validated canonical table, or null for an allowed empty table</span>
	 * <span class="zh-CN">经过校验的规范码表；允许的空表返回 null</span>
	 * @throws IOException <span class="en-US">If the code lengths or code space are invalid</span>
	 *                     <span class="zh-CN">码长、码位分配或码表结构非法时抛出</span>
	 */
	private static HuffmanTree.Book buildBook(final int[] lengths, final int maxLength,
	                                          final boolean allowSingle, final boolean allowEmpty) throws IOException {
		int[] counts = new int[maxLength + 1];
		int max = 0;

		for (int length : lengths) {
			if (length < 0 || length > maxLength) {
				throw invalid("Invalid code length");
			}
			if (length > 0) {
				counts[length]++;
				max = Math.max(max, length);
			}
		}

		if (max == 0) {
			if (allowEmpty) {
				return null;
			}
			throw invalid("Empty Huffman tree");
		}

		int left = 1;
		for (int length = 1; length <= maxLength; length++) {
			left = (left << 1) - counts[length];
			if (left < 0) {
				throw invalid("Oversubscribed Huffman tree");
			}
		}
		if (left > 0 && !(allowSingle && max == 1)) {
			throw invalid("Incomplete Huffman tree");
		}

		try {
			return HuffmanTree.canonicalCodes(lengths);
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid DEFLATE Huffman tree", e);
		}
	}

	/**
	 * <p class="en-US">Decode a symbol using the shared canonical Huffman table.</p>
	 * <p class="zh-CN">使用共用的规范 Huffman 码表解码一个符号。</p>
	 *
	 * <p class="en-US">Read one bit at a time and accumulate the code in arrival order.
	 * Reading several bits through readBits(n) would instead produce a low-bit-first
	 * integer, which is not the canonical code expected by Book.getSymbol().</p>
	 * <p class="zh-CN">逐位读取，并按到达顺序左移累积规范码。不能直接将 readBits(n)
	 * 返回的低位优先数值传给 Book.getSymbol()，否则会颠倒码字的位序。</p>
	 *
	 * @param book <span class="en-US">Canonical table; null represents an empty distance table</span>
	 *             <span class="zh-CN">规范码表；null 表示空距离表</span>
	 * @return <span class="en-US">Decoded symbol index</span><span class="zh-CN">解码后的符号下标</span>
	 * @throws IOException <span class="en-US">If the table is empty, the code is invalid,
	 *                     or reading the compressed bits fails</span>
	 *                     <span class="zh-CN">码表为空、编码非法或读取压缩位失败时抛出</span>
	 */
	private int decodeSymbol(final HuffmanTree.Book book) throws IOException {
		if (book == null) {
			throw invalid("Missing distance tree");
		}

		int code = 0;
		for (int length = 1; length <= book.getMaxLength(); length++) {
			code = (code << 1) | this.readBits(1);
			int symbol = book.getSymbol(code, length);
			if (symbol >= 0) {
				return symbol;
			}
		}
		throw invalid("Invalid Huffman code");
	}
}
