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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.Deflater;

/**
 * <h2 class="en-US">Streaming encoder for raw DEFLATE and Deflate64 data</h2>
 * <h2 class="zh-CN">原始 DEFLATE 和 Deflate64 数据的流式编码器</h2>
 *
 * <p class="en-US">DEFLATE delegates compression to the JDK in raw mode. Deflate64
 * uses bounded LZ77 matching and fixed canonical Huffman tables built by
 * {@link HuffmanTree#canonicalCodes(int[])}. This stream does not write ZIP, zlib,
 * or GZIP headers and does not compute container checksums.</p>
 * <p class="zh-CN">DEFLATE 使用 JDK 的原始压缩模式；Deflate64 使用有界 LZ77
 * 匹配和固定规范 Huffman 表，规范码统一由 HuffmanTree 构建。
 * 本流不写入 ZIP、zlib 或 GZIP 容器头，也不计算容器校验和。</p>
 *
 * <p class="en-US">Deflate64 level 0 emits stored blocks. Levels 1..9 increase
 * the match-length search bound while retaining the fixed Huffman strategy;
 * they do not promise the ratios of dynamic-Huffman encoders.</p>
 * <p class="zh-CN">Deflate64 的级别 0 输出未压缩块；级别 1..9 提高匹配长度搜索上限，
 * 编码策略仍为固定 Huffman，不保证达到动态 Huffman 编码器的压缩率。</p>
 *
 * <p class="en-US">Not thread safe. After successful construction this stream owns
 * the destination. finish() completes and flushes compressed output without closing
 * that destination, allowing an archive writer to append its remaining structures.</p>
 * <p class="zh-CN">本类不是线程安全的。构造成功后由本流负责关闭目标输出流。
 * finish() 完成压缩并排出输出缓冲，但保留目标流，以便归档写入器继续写入后续结构。</p>
 */
public final class DeflateOutputStream extends OutputStream {

	/**
	 * <span class="en-US">Immutable fixed literal/length code book for symbols 0..287.</span>
	 * <span class="zh-CN">符号 0..287 共用的只读固定字面量/长度码表。</span>
	 */
	private static final HuffmanTree.Book FIXED_LITERAL;
	/**
	 * <span class="en-US">Immutable fixed distance code book; all 32 codes have length 5.</span>
	 * <span class="zh-CN">只读固定距离码表，32 个距离符号的码长均为 5 位。</span>
	 */
	private static final HuffmanTree.Book FIXED_DISTANCE;

	static {
		int[] lengths = new int[288];
		Arrays.fill(lengths, 0, 144, 8);
		Arrays.fill(lengths, 144, 256, 9);
		Arrays.fill(lengths, 256, 280, 7);
		Arrays.fill(lengths, 280, 288, 8);
		FIXED_LITERAL = HuffmanTree.canonicalCodes(lengths);

		lengths = new int[32];
		Arrays.fill(lengths, 5);
		FIXED_DISTANCE = HuffmanTree.canonicalCodes(lengths);
	}

	/**
	 * <span class="en-US">Buffered compressed destination, owned by this stream.</span>
	 * <span class="zh-CN">由本流负责关闭的压缩数据输出流，缓冲后再批量写入文件或加密层。</span>
	 */
	private final OutputStream source;
	/**
	 * <span class="en-US">JDK compressor for DEFLATE; null selects the Deflate64 backend.</span>
	 * <span class="zh-CN">普通 DEFLATE 使用的 JDK 压缩器；null 表示使用 Deflate64 后端。</span>
	 */
	private final Deflater deflater;
	/**
	 * <span class="en-US">Raw JDK DEFLATE output wrapper, absent in Deflate64 mode.</span>
	 * <span class="zh-CN">JDK 原始 DEFLATE 输出包装流，Deflate64 模式下为 null。</span>
	 */
	private final java.util.zip.DeflaterOutputStream standard;
	/**
	 * <span class="en-US">Up to 64 KiB of pending Deflate64 input; absent in JDK mode.</span>
	 * <span class="zh-CN">等待编码的 Deflate64 输入块，容量为 64 KiB；JDK 模式不分配。</span>
	 */
	private final byte[] pending;
	/**
	 * <span class="en-US">Trailing input history retained between Deflate64 blocks.</span>
	 * <span class="zh-CN">Deflate64 跨块保留的末尾历史数据，最多 64 KiB。</span>
	 */
	private final byte[] history;
	/**
	 * <span class="en-US">Compression level, validated in the range 0..9.</span>
	 * <span class="zh-CN">经过范围校验的压缩级别，取值为 0..9。</span>
	 */
	private final int level;
	/**
	 * <span class="en-US">Deflate64 block and bit-buffer state:</span>
	 * <span class="zh-CN">Deflate64 块及位缓存状态：</span>
	 * <ul>
	 *   <li>size: valid bytes in pending. 输入缓冲中的有效字节数。</li>
	 *   <li>historySize: valid bytes in history. 历史缓冲中的有效字节数。</li>
	 *   <li>bits: pending output bits, with the next output bit in the lowest position.
	 *       待输出的位，最低位最先写出。</li>
	 *   <li>bitCount: number of valid pending bits. 位缓存中的有效位数。</li>
	 * </ul>
	 */
	private int size, historySize, bits, bitCount;
	/**
	 * <span class="en-US">written counts Deflate64 compressed bytes before destination
	 * buffering; read counts uncompressed bytes supplied through write().</span>
	 * <span class="zh-CN">written 统计已生成并交给输出缓冲的 Deflate64 压缩字节数；
	 * read 统计通过 write() 提交的未压缩字节数。</span>
	 */
	private long written, read;
	/**
	 * <span class="en-US">finished marks completed output; closed marks terminal resource
	 * release. release() can set closed without finishing or closing the destination.</span>
	 * <span class="zh-CN">finished 表示压缩已完成；closed 表示本编码器已终止并释放资源。
	 * release() 可以在尚未完成压缩、也未关闭目标流时设置 closed。</span>
	 */
	private boolean finished, closed;

	/**
	 * <p class="en-US">Create an encoder and take ownership of its destination.</p>
	 * <p class="zh-CN">创建编码器并接管目标输出流。</p>
	 *
	 * @param source <span class="en-US">Destination for raw compressed bytes</span>
	 *               <span class="zh-CN">用于接收原始压缩数据的目标流</span>
	 * @param deflate64 <span class="en-US">true for ZIP method 9, false for method 8</span>
	 *                  <span class="zh-CN">true 使用 ZIP 方法 9；false 使用方法 8</span>
	 * @param level <span class="en-US">Compression level 0..9; Deflate64 level 0 uses stored blocks</span>
	 *              <span class="zh-CN">压缩级别 0..9，Deflate64 的级别 0 使用未压缩块</span>
	 * @throws NullPointerException <span class="en-US">If source is null</span>
	 *                              <span class="zh-CN">目标流为 null 时抛出</span>
	 * @throws IllegalArgumentException <span class="en-US">If level is outside 0..9</span>
	 *                                  <span class="zh-CN">压缩级别超出 0..9 时抛出</span>
	 */
	public DeflateOutputStream(final OutputStream source, final boolean deflate64, final int level) {
		this.source = new BufferedOutputStream(Objects.requireNonNull(source, "source"), 8192);
		if (level < 0 || level > 9) {
			throw new IllegalArgumentException("Compression level: " + level);
		}
		this.level = level;
		this.pending = deflate64 ? new byte[65536] : null;
		this.history = deflate64 ? new byte[65536] : null;
		if (deflate64) {
			this.deflater = null;
			this.standard = null;
		} else {
			this.deflater = new Deflater(this.level, true);
			this.standard = new java.util.zip.DeflaterOutputStream(this.source, this.deflater);
		}
	}

	/**
	 * <p class="en-US">Write the low eight bits of one uncompressed value.</p>
	 * <p class="zh-CN">将给定数值的低 8 位作为一个未压缩字节写入。</p>
	 * @param value <span class="en-US">Byte value</span><span class="zh-CN">待写入字节值</span>
	 * @throws IOException <span class="en-US">If finished, closed, or destination writing fails</span>
	 *                     <span class="zh-CN">压缩已完成、流已终止或目标写入失败时抛出</span>
	 */
	@Override
	public void write(final int value) throws IOException {
		this.write(new byte[]{(byte) value});
	}

	/**
	 * <p class="en-US">Submit uncompressed bytes to the selected compression backend.</p>
	 * <p class="zh-CN">向选定的压缩后端提交未压缩数据。</p>
	 * @param bytes <span class="en-US">Input buffer</span><span class="zh-CN">输入缓冲区</span>
	 * @param offset <span class="en-US">First input index</span><span class="zh-CN">起始读取位置</span>
	 * @param length <span class="en-US">Input byte count</span><span class="zh-CN">输入字节数</span>
	 * @throws NullPointerException <span class="en-US">If bytes is null</span><span class="zh-CN">输入为 null 时抛出</span>
	 * @throws IndexOutOfBoundsException <span class="en-US">If the input range is invalid</span>
	 *                                  <span class="zh-CN">输入范围非法时抛出</span>
	 * @throws IOException <span class="en-US">If finished, closed, or destination writing fails</span>
	 *                     <span class="zh-CN">压缩已完成、流已终止或目标写入失败时抛出</span>
	 */
	@Override
	public void write(final byte[] bytes, int offset, int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		if (this.finished || this.closed) {
			throw new IOException("Stream finished or closed");
		}
		if (this.standard != null) {
			this.standard.write(bytes, offset, length);
			this.read += length;
			return;
		}
		this.read += length;
		while (length > 0) {
			int count = Math.min(length, this.pending.length - this.size);
			System.arraycopy(bytes, offset, this.pending, this.size, count);
			this.size += count;
			offset += count;
			length -= count;
			if (this.size == this.pending.length) {
				this.block(false);
			}
		}
	}

	/**
	 * <p class="en-US">Encode the pending Deflate64 input and retain trailing history.</p>
	 * <p class="zh-CN">编码等待处理的 Deflate64 输入，并保留末尾历史数据。</p>
	 * @param last <span class="en-US">Whether the emitted block sequence ends the stream</span>
	 *             <span class="zh-CN">本次输出的块序列是否结束压缩流</span>
	 * @throws IOException <span class="en-US">If compressed output cannot be written</span>
	 *                     <span class="zh-CN">写入压缩数据失败时抛出</span>
	 */
	private void block(final boolean last) throws IOException {
		if (this.level == 0) {
			int offset = 0;
			do {
				int count = Math.min(65535, this.size - offset);
				this.writeBits(last && offset + count == this.size ? 1 : 0, 3);
				this.writeBits(0, (8 - this.bitCount) & 7);
				this.writeBits(count, 16);
				this.writeBits(count ^ 65535, 16);
				this.source.write(this.pending, offset, count);
				this.written += count;
				offset += count;
			} while (offset < this.size);
			this.size = 0;
			return;
		}

		this.writeBits(last ? 3 : 2, 3);
		byte[] data = new byte[this.historySize + this.size];
		System.arraycopy(this.history, 0, data, 0, this.historySize);
		System.arraycopy(this.pending, 0, data, this.historySize, this.size);

		int[] head = new int[65536];
		Arrays.fill(head, -1);
		for (int i = 0; i < this.historySize && i + 2 < data.length; i++) {
			head[DeflateOutputStream.hash(data, i)] = i;
		}

		int i = this.historySize;
		while (i < data.length) {
			int match = 0;
			int distance = 0;
			if (i + 2 < data.length) {
				int candidate = head[DeflateOutputStream.hash(data, i)];
				if (candidate >= 0 && i - candidate <= 65536) {
					int max = Math.min(Math.min(65538, this.level * 8192), data.length - i);
					while (match < max && data[candidate + match] == data[i + match]) {
						match++;
					}
					distance = i - candidate;
				}
			}

			int consumed = match >= 3 ? match : 1;
			if (match >= 3) {
				if (match >= 258) {
					this.writeSymbol(DeflateOutputStream.FIXED_LITERAL, 285);
					this.writeBits(match - 3, 16);
				} else {
					int code = 0;
					while (code < 27 && DeflateInputStream.LENGTH[code + 1] <= match) {
						code++;
					}
					this.writeSymbol(DeflateOutputStream.FIXED_LITERAL, 257 + code);
					this.writeBits(match - DeflateInputStream.LENGTH[code], DeflateInputStream.LENGTH_BITS[code]);
				}

				int code = 0;
				while (code < 31 && DeflateInputStream.DISTANCE[code + 1] <= distance) {
					code++;
				}
				this.writeSymbol(DeflateOutputStream.FIXED_DISTANCE, code);
				this.writeBits(distance - DeflateInputStream.DISTANCE[code], DeflateInputStream.DISTANCE_BITS[code]);
			} else {
				this.writeSymbol(DeflateOutputStream.FIXED_LITERAL, data[i] & 0xFF);
			}

			// 被匹配跳过的位置同样加入索引，供后续数据引用。
			for (int j = 0; j < consumed && i + j + 2 < data.length; j++) {
				head[DeflateOutputStream.hash(data, i + j)] = i + j;
			}
			i += consumed;
		}

		this.writeSymbol(DeflateOutputStream.FIXED_LITERAL, 256);
		this.historySize = Math.min(65536, data.length);
		System.arraycopy(data, data.length - this.historySize, this.history, 0, this.historySize);
		this.size = 0;
	}

	/**
	 * <span class="en-US">Hash three consecutive input bytes into a 16-bit bucket index.</span>
	 * <span class="zh-CN">将连续三个输入字节映射为 16 位哈希桶下标。</span>
	 * @param data <span class="en-US">Combined history and pending input</span><span class="zh-CN">历史与当前输入的合并数组</span>
	 * @param i <span class="en-US">First of three valid byte positions</span><span class="zh-CN">三个有效字节的起始位置</span>
	 * @return <span class="en-US">Bucket index 0..65535</span><span class="zh-CN">0..65535 的桶下标</span>
	 */
	private static int hash(final byte[] data, final int i) {
		return (((data[i] & 0xFF) * 251 + (data[i + 1] & 0xFF)) * 251 + (data[i + 2] & 0xFF)) & 65535;
	}

	/**
	 * <p class="en-US">Write a canonical Huffman symbol through the low-bit-first bit writer.</p>
	 * <p class="zh-CN">将规范 Huffman 符号交给低位优先的位写入器输出。</p>
	 *
	 * <p class="en-US">Book returns a low-aligned canonical code whose most significant
	 * meaningful bit must arrive first. Reverse exactly its valid bits before passing
	 * it to writeBits(); extra length and distance values are not reversed.</p>
	 * <p class="zh-CN">Book 返回低位对齐的规范码，码字的有效最高位需要最先输出。
	 * 因而先反转有效码位，再调用 writeBits()；长度、距离的附加数值位不执行反转。</p>
	 * @param book <span class="en-US">Canonical encoding table</span><span class="zh-CN">规范编码表</span>
	 * @param symbol <span class="en-US">Symbol index present in the table</span><span class="zh-CN">码表中存在的符号下标</span>
	 * @throws IOException <span class="en-US">If compressed output cannot be written</span>
	 *                     <span class="zh-CN">写入压缩数据失败时抛出</span>
	 */
	private void writeSymbol(final HuffmanTree.Book book, final int symbol) throws IOException {
		int length = book.getLength(symbol);
		int code = book.getCode(symbol);
		this.writeBits(Integer.reverse(code) >>> (32 - length), length);
	}

	/**
	 * <span class="en-US">Append a low-bit-first unsigned value; internal calls use 0..16 bits.</span>
	 * <span class="zh-CN">按低位优先顺序追加无符号数值，内部调用的位数范围为 0..16。</span>
	 * @param value <span class="en-US">Value fitting within count bits</span><span class="zh-CN">可由 count 位表示的数值</span>
	 * @param count <span class="en-US">Bit count, including zero for no padding</span><span class="zh-CN">写入位数，零表示无需填充</span>
	 * @throws IOException <span class="en-US">If a complete output byte cannot be written</span>
	 *                     <span class="zh-CN">完整输出字节写入失败时抛出</span>
	 */
	private void writeBits(final int value, final int count) throws IOException {
		this.bits |= value << this.bitCount;
		this.bitCount += count;
		while (this.bitCount >= 8) {
			this.source.write(this.bits & 255);
			this.written++;
			this.bits >>>= 8;
			this.bitCount -= 8;
		}
	}

	/**
	 * <p class="en-US">Complete and flush compressed output without closing its destination.</p>
	 * <p class="zh-CN">完成压缩并排出输出缓冲，保留目标流供归档层继续写入。</p>
	 * <p class="en-US">Repeated successful calls have no effect. Deflate64 emits a final
	 * block even for empty input, then zero-pads its last partial byte.</p>
	 * <p class="zh-CN">重复调用已成功完成的 finish() 不产生额外数据。即使输入为空，
	 * Deflate64 也输出最终块，并将最后一个不完整字节的剩余位补零。</p>
	 * @throws IOException <span class="en-US">If closed or output completion fails</span>
	 *                     <span class="zh-CN">编码器已终止或完成输出失败时抛出</span>
	 */
	public void finish() throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
		if (this.finished) {
			return;
		}
		if (this.standard != null) {
			this.standard.finish();
		} else {
			this.block(true);
			if (this.bitCount != 0) {
				this.source.write(this.bits & 255);
				this.written++;
				this.bits = this.bitCount = 0;
			}
		}
		this.source.flush();
		this.finished = true;
	}

	/**
	 * <span class="en-US">Get generated compressed bytes, which may still be buffered before finish().
	 * Query before releasing the JDK compressor.</span>
	 * <span class="zh-CN">获取已生成的压缩字节数，finish() 前数据可能仍在缓冲层中。
	 * JDK 后端应在释放压缩器之前读取此计数。</span>
	 * @return <span class="en-US">Compressed byte count</span><span class="zh-CN">压缩字节数</span>
	 */
	public long getBytesWritten() {
		return this.deflater == null ? this.written : this.deflater.getBytesWritten();
	}

	/**
	 * <span class="en-US">Get uncompressed input bytes submitted through write().</span>
	 * <span class="zh-CN">获取通过 write() 提交的未压缩输入字节数。</span>
	 * @return <span class="en-US">Input byte count</span><span class="zh-CN">未压缩输入字节数</span>
	 */
	public long getBytesRead() {
		return this.read;
	}

	/**
	 * <p class="en-US">Terminate the encoder and release native resources without finishing,
	 * flushing, or closing the destination. Call finish() first when preserving output.</p>
	 * <p class="zh-CN">终止编码器并释放原生资源，不完成压缩、不排出缓冲，也不关闭目标流。
	 * 需要保留有效压缩结果时必须先调用 finish()。此方法适用于归档层自行管理目标流的场景。</p>
	 */
	public void release() {
		if (!this.closed) {
			this.closed = true;
			if (this.deflater != null) {
				this.deflater.end();
			}
		}
	}

	/**
	 * <p class="en-US">Flush already generated bytes without forcing a block boundary.</p>
	 * <p class="zh-CN">排出已生成字节，不强制结束当前压缩块。</p>
	 * <p class="en-US">Pending Deflate64 input and partial output bits remain buffered.
	 * The JDK wrapper uses its default non-sync-flush behavior.</p>
	 * <p class="zh-CN">尚未编码的 Deflate64 输入及不足一个字节的输出位仍保留在缓冲中；
	 * JDK 包装流使用默认的不执行同步刷新行为。</p>
	 * @throws IOException <span class="en-US">If closed or destination flushing fails</span>
	 *                     <span class="zh-CN">编码器已终止或目标刷新失败时抛出</span>
	 */
	@Override
	public void flush() throws IOException {
		if (this.closed) {
			throw new IOException("Stream closed");
		}
		if (this.standard != null) {
			this.standard.flush();
		}
		this.source.flush();
	}

	/**
	 * <p class="en-US">Finish output, release compressor resources, and close the destination.</p>
	 * <p class="zh-CN">完成输出、释放压缩器资源并关闭目标流。</p>
	 * <p class="en-US">If finishing and destination closure both fail with IOExceptions,
	 * preserve the finishing failure and attach the closure failure as suppressed.</p>
	 * <p class="zh-CN">若完成压缩与关闭目标流均抛出 IOException，保留前者，
	 * 并将关闭异常附加为 suppressed 异常。</p>
	 * @throws IOException <span class="en-US">If finishing or destination closure fails</span>
	 *                     <span class="zh-CN">完成压缩或关闭目标流失败时抛出</span>
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		IOException failure = null;
		try {
			this.finish();
		} catch (IOException e) {
			failure = e;
		} finally {
			this.release();
		}
		try {
			this.source.close();
		} catch (IOException e) {
			if (failure == null) {
				failure = e;
			} else {
				failure.addSuppressed(e);
			}
		}
		if (failure != null) {
			throw failure;
		}
	}
}
