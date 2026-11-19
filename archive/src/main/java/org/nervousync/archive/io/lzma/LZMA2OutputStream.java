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
import java.io.OutputStream;
import java.util.Objects;

/**
 * <h2 class="en-US">Raw LZMA2 encoder using independent chunks and stored fallback.</h2>
 * <h2 class="zh-CN">采用独立分块及未压缩回退的原始 LZMA2 编码器。</h2>
 * <p class="en-US">Reuses the LZMA1 greedy encoder without an internal LZMA1 EOS.
 * Every chunk resets the dictionary; matches do not span chunks. Fixed dictionary:
 * 64 KiB; levels 0..9 control match limits as in LZMAOutputStream. One thread.</p>
 * <p class="zh-CN">复用 LZMA1 贪心编码器，块内不写 LZMA1 EOS。每块重置字典，匹配不跨块。
 * 固定字典为 64 KiB；等级 0..9 按 LZMAOutputStream 规则控制匹配上限。单线程。</p>
 * <p class="en-US">No XZ header or integrity checksum. Store dictionary size externally.
 * flush completes pending chunks without ending the stream; finish emits one end control.
 * Owns the delegate after construction. Not thread safe.</p>
 * <p class="zh-CN">不包含 XZ 头或完整性校验，字典大小须由外部保存。flush 完成待处理块但不结束流；
 * finish 输出一次结束控制字节。构造成功后负责关闭底层流，非线程安全。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class LZMA2OutputStream extends OutputStream {
	/**
	 * <span class="en-US">Owned raw LZMA2 destination.</span>
	 * <span class="zh-CN">负责关闭的原始 LZMA2 输出目标。</span>
	 */
	private final OutputStream output;

	/**
	 * <span class="en-US">Reused LZMA1 match-search level.</span>
	 * <span class="zh-CN">复用 LZMA1 的匹配搜索等级。</span>
	 */
	private final int level;

	/**
	 * <span class="en-US">One input chunk, never more than 64 KiB.</span>
	 * <span class="zh-CN">一个输入块，最多 64 KiB。</span>
	 */
	private final byte[] buffer = new byte[65536];

	/**
	 * <span class="en-US">Pending input bytes.</span>
	 * <span class="zh-CN">待处理输入字节数。</span>
	 */
	private int buffered;

	/**
	 * <span class="en-US">Successfully emitted end control and flushed.</span>
	 * <span class="zh-CN">已成功输出结束控制字节并刷新。</span>
	 */
	private boolean finished;

	/**
	 * <span class="en-US">Closure initiated.</span>
	 * <span class="zh-CN">已开始关闭。</span>
	 */
	private boolean closed;

	/**
	 * <span class="en-US">Sticky first output failure.</span>
	 * <span class="zh-CN">保存首次输出错误。</span>
	 */
	private IOException failure;

	/**
	 * <h4 class="en-US">Create a raw encoder without writing output.</h4>
	 * <h4 class="zh-CN">创建原始编码器，构造时不写入输出。</h4>
	 *
	 * <p class="en-US">Uses a fixed 64 KiB dictionary and independent chunks. Owns the destination after successful construction; finish leaves it open and close closes it.</p>
	 * <p class="zh-CN">使用固定 64 KiB 字典和独立分块；构造成功后负责管理目标流，finish 保留目标流，close 关闭它。</p>
	 *
	 * @param output <span class="en-US">Delegate output stream.</span>
	 *               <span class="zh-CN">底层输出流。</span>
	 * @param level  <span class="en-US">Match-search level from 0 through 9; zero still attempts compression.</span>
	 *               <span class="zh-CN">范围为 0 至 9 的匹配搜索等级；零等级仍尝试压缩。</span>
	 * @throws IllegalArgumentException <span class="en-US">The level is outside 0 through 9.</span>
	 *                                  <span class="zh-CN">等级不在 0 至 9 范围内。</span>
	 * @throws NullPointerException     <span class="en-US">The output is null.</span>
	 *                                  <span class="zh-CN">输出流为 null。</span>
	 */
	public LZMA2OutputStream(OutputStream output, int level) {
		this.output = Objects.requireNonNull(output, "output");
		if (level < 0 || level > 9) {
			throw new IllegalArgumentException("LZMA2 level must be 0..9");
		}
		this.level = level;
	}

	/**
	 * <h4 class="en-US">Get the dictionary size required for decoding.</h4>
	 * <h4 class="zh-CN">取得解码所需的字典大小。</h4>
	 *
	 * @return <span class="en-US">Fixed 65536-byte dictionary; store this value in the enclosing format.</span>
	 * <span class="zh-CN">固定为 65536 字节；外部格式应保存该值。</span>
	 */
	public int getDictionarySize() {
		return 65536;
	}

	/**
	 * <h4 class="en-US">Write a two-byte big-endian size-minus-one.</h4>
	 * <h4 class="zh-CN">写入两字节大端的大小减一字段。</h4>
	 *
	 * @param size <span class="en-US">Actual size from 1 through 65536, validated by the caller.</span>
	 *             <span class="zh-CN">调用方已验证的实际大小，范围为 1 至 65536。</span>
	 * @throws IOException <span class="en-US">The stream is no longer usable or writing, flushing or closing fails.</span>
	 *                     <span class="zh-CN">流已不可使用，或写入、刷新、关闭操作失败。</span>
	 */
	private void writeSize(int size) throws IOException {
		int value = size - 1;
		this.output.write(value >>> 8);
		this.output.write(value & 255);
	}

	/**
	 * <span class="en-US">Trial compression is capped at the protocol's compressed chunk limit.</span>
	 * <span class="zh-CN">试压缩受协议的压缩块大小上限约束。</span>
	 */
	private static final class ChunkBuffer extends OutputStream {
		/**
		 * <span class="en-US">Bounded compressed storage.</span>
		 * <span class="zh-CN">有界压缩存储。</span>
		 */
		private final byte[] bytes = new byte[65536];

		/**
		 * <span class="en-US">Encoded byte count.</span>
		 * <span class="zh-CN">已编码字节数。</span>
		 */
		private int size;

		/**
		 * <h4 class="en-US">Append one trial-compression byte.</h4>
		 * <h4 class="zh-CN">追加一个试压缩字节。</h4>
		 *
		 * @param value <span class="en-US">Byte value; only the low eight bits are stored.</span>
		 *              <span class="zh-CN">字节值，仅保存最低八位。</span>
		 * @throws ChunkOverflow <span class="en-US">The 64 KiB packed limit is reached.</span>
		 *                       <span class="zh-CN">已达到 64 KiB 压缩数据上限。</span>
		 */
		@Override
		public void write(int value) throws IOException {
			if (this.size == this.bytes.length) {
				throw new ChunkOverflow();
			}
			this.bytes[this.size++] = (byte) value;
		}

		/**
		 * <h4 class="en-US">Append bounded trial-compression bytes.</h4>
		 * <h4 class="zh-CN">追加有界的试压缩字节。</h4>
		 *
		 * @param bytes  <span class="en-US">Source array.</span><span class="zh-CN">源数组。</span>
		 * @param offset <span class="en-US">Source start index.</span><span class="zh-CN">源起始下标。</span>
		 * @param length <span class="en-US">Source byte count.</span><span class="zh-CN">源字节数。</span>
		 * @throws ChunkOverflow             <span class="en-US">The packed bytes would exceed 64 KiB.</span>
		 *                                   <span class="zh-CN">压缩数据将超过 64 KiB。</span>
		 * @throws IndexOutOfBoundsException <span class="en-US">The source range is invalid.</span>
		 *                                   <span class="zh-CN">源数组范围非法。</span>
		 */
		@Override
		public void write(byte[] bytes, int offset, int length) throws IOException {
			Objects.checkFromIndexSize(offset, length, bytes.length);
			if (length > this.bytes.length - this.size) {
				throw new ChunkOverflow();
			}
			System.arraycopy(bytes, offset, this.bytes, this.size, length);
			this.size += length;
		}
	}

	/**
	 * <span class="en-US">Internal signal selecting stored fallback.</span>
	 * <span class="zh-CN">用于选择未压缩回退的内部信号。</span>
	 */
	private static final class ChunkOverflow extends IOException {
		/**
		 * Serialization identifier. / 序列化标识。
		 */
		private static final long serialVersionUID = 1L;
	}

	/**
	 * <h4 class="en-US">Compress or store the pending independent chunk.</h4>
	 * <h4 class="zh-CN">压缩或直接存储待处理的独立分块。</h4>
	 *
	 * <p class="en-US">Trial output is bounded to 64 KiB. Compare payload plus six compressed-header bytes against raw data plus three stored-header bytes. Only internal trial overflow selects fallback; delegate failures propagate.</p>
	 * <p class="zh-CN">试压缩输出限制为 64 KiB；比较压缩数据加六字节头和原始数据加三字节头的总大小。只有内部试压缩溢出触发回退，底层输出错误继续向外抛出。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is no longer usable or writing, flushing or closing fails.</span>
	 *                     <span class="zh-CN">流已不可使用，或写入、刷新、关闭操作失败。</span>
	 */
	private void writeChunk() throws IOException {
		if (this.buffered == 0) {
			return;
		}
		ChunkBuffer packed = new ChunkBuffer();
		boolean overflow = false;
		try (LZMAOutputStream encoder = LZMAOutputStream.chunk(packed, this.level)) {
			encoder.write(this.buffer, 0, this.buffered);
			encoder.finish();
		} catch (ChunkOverflow tooLarge) {
			overflow = true;
		}
		if (!overflow && packed.size + 6 < this.buffered + 3) {
			// Reset dictionary, state and properties for this independent chunk.
			// 为当前独立分块重置字典、状态和属性。
			this.output.write(0xE0);
			this.writeSize(this.buffered);
			this.writeSize(packed.size);
			// Packed properties: lc=3, lp=0, pb=2. / 打包属性：lc=3、lp=0、pb=2。
			this.output.write(93);
			this.output.write(packed.bytes, 0, packed.size);
		} else {
			// Stored chunk with dictionary reset. / 重置字典的未压缩块。
			this.output.write(1);
			this.writeSize(this.buffered);
			this.output.write(this.buffer, 0, this.buffered);
		}
		this.buffered = 0;
	}

	/**
	 * <h4 class="en-US">Check whether more input can be accepted.</h4>
	 * <h4 class="zh-CN">检查是否可以继续接收输入。</h4>
	 *
	 * @throws IOException <span class="en-US">The stream is closed, finished or has a retained write failure.</span>
	 *                     <span class="zh-CN">流已关闭、已结束，或存在保存的写入失败。</span>
	 */
	private void ensureWritable() throws IOException {
		if (this.closed || this.finished) {
			throw new IOException("LZMA2 output closed/finished");
		}
		if (this.failure != null) {
			throw this.failure;
		}
	}

	/**
	 * <h4 class="en-US">Write one uncompressed source byte.</h4>
	 * <h4 class="zh-CN">写入一个未压缩源字节。</h4>
	 *
	 * @param value <span class="en-US">Only the lowest eight bits are encoded.</span>
	 *              <span class="zh-CN">仅编码最低八位。</span>
	 * @throws IOException <span class="en-US">The stream is no longer usable or writing, flushing or closing fails.</span>
	 *                     <span class="zh-CN">流已不可使用，或写入、刷新、关闭操作失败。</span>
	 */
	@Override
	public void write(int value) throws IOException {
		byte[] one = {
				(byte) value
		};
		this.write(one, 0, 1);
	}

	/**
	 * <h4 class="en-US">Buffer input and emit complete chunks.</h4>
	 * <h4 class="zh-CN">缓冲输入并输出完整分块。</h4>
	 *
	 * <p class="en-US">Full 64 KiB chunks are emitted immediately. A final partial chunk remains until flush, finish or close. Retain the first output failure rather than retrying changed coding state.</p>
	 * <p class="zh-CN">完整的 64 KiB 分块立即输出，末尾不足一块的数据保留至 flush、finish 或 close；保存首次输出失败，避免重试已经变化的编码状态。</p>
	 *
	 * @param bytes  <span class="en-US">Source array.</span>
	 *               <span class="zh-CN">源数组。</span>
	 * @param offset <span class="en-US">First source index.</span>
	 *               <span class="zh-CN">源起始下标。</span>
	 * @param length <span class="en-US">Number of source bytes.</span>
	 *               <span class="zh-CN">源字节数。</span>
	 * @throws IOException               <span class="en-US">The stream is no longer usable or writing, flushing or closing fails.</span>
	 *                                   <span class="zh-CN">流已不可使用，或写入、刷新、关闭操作失败。</span>
	 * @throws IndexOutOfBoundsException <span class="en-US">The array range is invalid.</span>
	 *                                   <span class="zh-CN">数组范围非法。</span>
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
					this.writeChunk();
				}
			}
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <h4 class="en-US">Complete the pending chunk without ending the stream.</h4>
	 * <h4 class="zh-CN">完成待处理分块并保留数据流。</h4>
	 *
	 * <p class="en-US">Flushes the delegate, including after finish. Before finish, subsequent writes start another independent chunk. Does not write the zero end control.</p>
	 * <p class="zh-CN">刷新底层输出，finish 后也可刷新；finish 前的后续写入开始新的独立分块，本操作不写零结束控制。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is no longer usable or writing, flushing or closing fails.</span>
	 *                     <span class="zh-CN">流已不可使用，或写入、刷新、关闭操作失败。</span>
	 */
	@Override
	public void flush() throws IOException {
		if (this.closed) {
			throw new IOException("LZMA2 output closed");
		}
		if (this.failure != null) {
			throw this.failure;
		}
		try {
			if (!this.finished) {
				this.writeChunk();
			}
			this.output.flush();
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <h4 class="en-US">Finish raw LZMA2 output while keeping the delegate open.</h4>
	 * <h4 class="zh-CN">完成原始 LZMA2 输出并保留底层流。</h4>
	 *
	 * <p class="en-US">Emit pending data, exactly one zero end control and flush. Repeated successful calls are no-ops. After failure, reject retries; the destination may already contain partial output.</p>
	 * <p class="zh-CN">输出待处理数据、唯一的零结束控制并刷新；成功后重复调用无操作。失败后拒绝重试，目标流可能已有部分输出。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is no longer usable or writing, flushing or closing fails.</span>
	 *                     <span class="zh-CN">流已不可使用，或写入、刷新、关闭操作失败。</span>
	 */
	public void finish() throws IOException {
		if (this.closed) {
			throw new IOException("LZMA2 output closed");
		}
		if (this.failure != null) {
			throw this.failure;
		}
		if (this.finished) {
			return;
		}
		try {
			this.writeChunk();
			this.output.write(0);
			this.output.flush();
			this.finished = true;
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <h4 class="en-US">Finish when possible and close the delegate once.</h4>
	 * <h4 class="zh-CN">在可行时完成编码并只关闭一次底层流。</h4>
	 *
	 * <p class="en-US">Skip further encoding after a retained write failure. If this call encounters both finish and close failures, retain the finish exception and suppress the close exception. Mark closed even when closing fails.</p>
	 * <p class="zh-CN">已有保存的写入失败时跳过后续编码；本次调用同时发生结束和关闭异常时，保留结束异常并将关闭异常加入 suppressed，关闭失败也标记为已关闭。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is no longer usable or writing, flushing or closing fails.</span>
	 *                     <span class="zh-CN">流已不可使用，或写入、刷新、关闭操作失败。</span>
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
		} catch (IOException finishFailure) {
			error = finishFailure;
		} finally {
			this.closed = true;
			try {
				this.output.close();
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
}
