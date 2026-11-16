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

package org.nervousync.archive.io.bzip2;

import org.nervousync.security.crc.CRCAdaptor;
import org.nervousync.tree.huffman.HuffmanTree;
import org.nervousync.utils.security.DigestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * <h2 class="en-US">Block-based BZip2 compression output stream</h2>
 * <h2 class="zh-CN">按块处理的 BZip2 压缩输出流</h2>
 *
 * <p class="en-US">Writes complete BZh streams independently of ZIP and TAR containers.
 * Encoding stages are RLE1, BWT, MTF, RLE2 and canonical Huffman coding.</p>
 * <p class="zh-CN">独立于 ZIP、TAR 容器，写出包含 BZh 流头的完整 BZip2 流。
 * 编码过程依次为 RLE1、BWT、MTF、RLE2 和规范 Huffman 编码。</p>
 *
 * <p class="en-US">Block sizes 1 through 9 select 100000 through 900000 bytes of
 * RLE1-encoded block capacity. A 19-byte implementation margin is reserved.
 * The original input size of a block may be larger due to RLE1 compression.</p>
 * <p class="zh-CN">块大小参数 1 至 9 对应 100000 至 900000 字节的 RLE1 编码块容量，
 * 实现中保留 19 字节余量。由于 RLE1 压缩，一个块对应的原始输入量可能更大。</p>
 *
 * <p class="en-US">Cyclic prefix-doubling sorting is used for BWT. This encoder writes
 * two identical Huffman tables and selects the first throughout the block.
 * It does not perform legacy block randomization and does not need RANDOM_NUMBERS.
 * Its compression ratio and throughput are not claimed to match libbzip2.</p>
 * <p class="zh-CN">BWT 使用循环前缀倍增排序。当前编码器写出两张相同的 Huffman 表，
 * 整个块均选择第一张表。不执行旧版块随机化，因此无需 RANDOM_NUMBERS 表。
 * 当前实现不承诺达到 libbzip2 的压缩率或吞吐量。</p>
 *
 * <p class="en-US">Not thread safe. finish() writes the member trailer without closing
 * the delegate. flush() drains only already encoded complete bytes; it does not
 * finish a run, a block or the member. close() finishes when possible and closes
 * the delegate. If construction fails, the caller remains responsible for it.</p>
 * <p class="zh-CN">此类不是线程安全的。finish() 写完成员但不关闭底层流；flush() 只刷新
 * 已经编码的完整字节，不结束重复段、数据块或成员；close() 在可行时完成编码并关闭
 * 底层流。构造失败时，底层流仍由调用方负责关闭。</p>
 */
public final class BZip2OutputStream extends OutputStream {

	/**
	 * <span class="en-US">48-bit compressed-block marker</span>
	 * <span class="zh-CN">48 位压缩块开始标记</span>
	 */
	private static final long BLOCK_MAGIC = 0x314159265359L;
	/**
	 * <span class="en-US">48-bit member-end marker, followed by the combined CRC</span>
	 * <span class="zh-CN">48 位成员结束标记，后接该成员的组合 CRC</span>
	 */
	private static final long END_MAGIC = 0x177245385090L;
	/**
	 * <span class="en-US">Delegate receiving the compressed bytes</span>
	 * <span class="zh-CN">接收压缩字节的底层输出流</span>
	 */
	private final OutputStream output;
	/**
	 * <span class="en-US">CRC-32/BZIP2 calculator for original bytes in the current block</span>
	 * <span class="zh-CN">用于计算当前块原始输入数据的 CRC-32/BZIP2</span>
	 */
	private final CRCAdaptor crc;
	/**
	 * <span class="en-US">Current block after RLE1 encoding and before BWT</span>
	 * <span class="zh-CN">经过 RLE1 编码、尚未执行 BWT 的当前块数据</span>
	 */
	private byte[] block;
	/**
	 * <span class="en-US">Scratch buffer for passing the current original run to the CRC calculator</span>
	 * <span class="zh-CN">将当前原始重复段交给 CRC 计算器时使用的临时缓冲区</span>
	 */
	private final byte[] run = new byte[255];
	/**
	 * <span class="en-US">Buffer of complete compressed bytes awaiting a delegate write</span>
	 * <span class="zh-CN">等待批量写入底层流的完整压缩字节缓冲区</span>
	 */
	private final byte[] encoded = new byte[8192];
	/**
	 * <span class="en-US">Number of valid bytes currently buffered in encoded</span>
	 * <span class="zh-CN">字节缓冲区中当前有效的完整字节数量</span>
	 */
	private int encodedLength;
	/**
	 * <span class="en-US">Number of RLE1-encoded bytes in the current block</span>
	 * <span class="zh-CN">当前块已经保存的 RLE1 编码字节数量</span>
	 */
	private int blockLength;
	/**
	 * <span class="en-US">Current repeated byte value from 0 to 255; -1 means not initialized</span>
	 * <span class="zh-CN">当前重复段的字节值，范围为 0 至 255，-1 表示尚未初始化</span>
	 */
	private int runByte = -1;
	/**
	 * <span class="en-US">Number of original bytes in the pending run, capped at 255 by this encoder</span>
	 * <span class="zh-CN">等待编码的重复段所包含的原始字节数，本实现单段最多累计 255 个</span>
	 */
	private int runLength;
	/**
	 * <span class="en-US">Combined CRC of blocks already encoded in the current member</span>
	 * <span class="zh-CN">当前成员中已经编码完成的数据块所组成的组合 CRC</span>
	 */
	private int combinedCrc;
	/**
	 * <span class="en-US">Pending compressed bits, retained in the low bitCount bits</span>
	 * <span class="zh-CN">尚未组成完整输出字节的压缩位，保存在低 bitCount 位中</span>
	 */
	private long bitBuffer;
	/**
	 * <span class="en-US">Number of pending valid bits; fewer than eight after writeBits returns successfully</span>
	 * <span class="zh-CN">暂存的有效位数，writeBits 成功返回后少于八位</span>
	 */
	private int bitCount;
	/**
	 * <span class="en-US">Whether the member trailer and remaining encoded bytes were written successfully</span>
	 * <span class="zh-CN">是否已经成功写出成员尾部及剩余编码字节</span>
	 */
	private boolean finished;
	/**
	 * <span class="en-US">Whether this stream has been closed</span>
	 * <span class="zh-CN">当前输出流是否已经关闭</span>
	 */
	private boolean closed;
	/**
	 * <span class="en-US">Previously reported I/O failure; writing must not resume after it</span>
	 * <span class="zh-CN">此前已经报告的 I/O 异常，记录后不再允许继续写入</span>
	 */
	private IOException failure;

	/**
	 * <h3 class="en-US">Construct a BZip2 encoder with a specified block size</h3>
	 * <h3 class="zh-CN">使用指定块大小构造 BZip2 压缩流</h3>
	 *
	 * <p class="en-US">Allocates the RLE1 block buffer, initializes the block CRC and writes the four-byte BZh header. No data block is written until input has been accumulated.</p>
	 * <p class="zh-CN">分配 RLE1 块缓冲区、初始化块 CRC，并写出四个字节 BZh 流头；累计输入后才会写出数据块。</p>
	 *
	 * @param output    <span class="en-US">Delegate owned by this stream after successful construction</span>
	 *                  <span class="zh-CN">构造成功后由本压缩流负责关闭的底层输出流</span>
	 * @param blockSize <span class="en-US">Block-capacity multiplier from 1 to 9, in units of 100000 bytes</span>
	 *                  <span class="zh-CN">块容量倍数，范围为 1 至 9，每单位为 100000 字节</span>
	 * @throws NullPointerException     <span class="en-US">The delegate is null</span>
	 *                                  <span class="zh-CN">底层输出流为空引用</span>
	 * @throws IllegalArgumentException <span class="en-US">The block size is outside 1 through 9</span>
	 *                                  <span class="zh-CN">块大小参数不在 1 至 9 的范围内</span>
	 * @throws IOException              <span class="en-US">Writing the stream header fails</span>
	 *                                  <span class="zh-CN">写入流头失败</span>
	 */
	public BZip2OutputStream(final OutputStream output, final int blockSize) throws IOException {
		this.output = Objects.requireNonNull(output, "output");
		if (blockSize < 1 || blockSize > 9) {
			throw new IllegalArgumentException("BZip2 blockSize must be between 1 and 9");
		}
		this.crc = DigestUtils.CRCAdaptor("CRC-32/BZIP2");
		this.block = new byte[blockSize * 100000];
		this.output.write(new byte[]{'B', 'Z', 'h', (byte) ('0' + blockSize)});
	}

	/**
	 * <h3 class="en-US">Accept the low eight bits of one input value</h3>
	 * <h3 class="zh-CN">接收一个输入值的最低八位</h3>
	 *
	 * <p class="en-US">Accumulates RLE1 runs and may encode a block when it becomes full. A write does not necessarily emit compressed bytes immediately.</p>
	 * <p class="zh-CN">累计 RLE1 重复段，块满时可能触发块编码；一次 write 不一定立即产生压缩输出。</p>
	 *
	 * @param value <span class="en-US">Input value; only its low eight bits are used</span>
	 *              <span class="zh-CN">输入值，仅使用其最低八位</span>
	 * @throws IOException <span class="en-US">The stream is closed, finished or failed, or writing compressed bytes fails</span>
	 *                     <span class="zh-CN">流已关闭、已完成或此前失败，或写入压缩数据失败</span>
	 */
	@Override
	public void write(final int value) throws IOException {
		this.ensureWritable();
		try {
			this.accept(value & 0xFF);
		} catch (IOException e) {
			this.failure = e;
			throw e;
		}
	}

	/**
	 * <h3 class="en-US">Accept a range of original input bytes</h3>
	 * <h3 class="zh-CN">接收原始输入字节数组的指定范围</h3>
	 *
	 * <p class="en-US">Uses the same RLE1 accumulation path as single-byte writing. The source array is not retained or modified.</p>
	 * <p class="zh-CN">与单字节写入使用相同的 RLE1 累计路径，不保留或修改调用方的数组。</p>
	 *
	 * @param bytes  <span class="en-US">Original input bytes</span>
	 *               <span class="zh-CN">原始输入字节数组</span>
	 * @param offset <span class="en-US">Start offset</span>
	 *               <span class="zh-CN">起始下标</span>
	 * @param length <span class="en-US">Number of bytes to accept</span>
	 *               <span class="zh-CN">接收字节数</span>
	 * @throws NullPointerException      <span class="en-US">The source array is null</span>
	 *                                   <span class="zh-CN">输入数组为空引用</span>
	 * @throws IndexOutOfBoundsException <span class="en-US">The source range is invalid</span>
	 *                                   <span class="zh-CN">输入数组范围非法</span>
	 * @throws IOException               <span class="en-US">The stream is not writable or writing compressed bytes fails</span>
	 *                                   <span class="zh-CN">流不可写或写入压缩数据失败</span>
	 */
	@Override
	public void write(final byte[] bytes, final int offset, final int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		this.ensureWritable();
		try {
			for (int end = offset + length, index = offset; index < end; index++) {
				this.accept(bytes[index] & 0xFF);
			}
		} catch (IOException e) {
			this.failure = e;
			throw e;
		}
	}

	/**
	 * <h3 class="en-US">Accumulate a byte in the current RLE1 run</h3>
	 * <h3 class="zh-CN">将一个字节累计到当前 RLE1 重复段</h3>
	 *
	 * <p class="en-US">A different byte or the implementation limit of 255 bytes ends the current run. A longer same-byte sequence is represented by multiple runs.</p>
	 * <p class="zh-CN">遇到不同字节或达到本实现的单段 255 字节上限时结束当前段；更长的相同字节序列会拆成多个段。</p>
	 *
	 * @param value <span class="en-US">Unsigned byte value from 0 to 255</span>
	 *              <span class="zh-CN">范围为 0 至 255 的无符号字节值</span>
	 * @throws IOException <span class="en-US">Encoding a full block triggers a delegate write that fails</span>
	 *                     <span class="zh-CN">块满编码过程中写入底层流失败</span>
	 */
	private void accept(final int value) throws IOException {
		if (value == this.runByte && this.runLength < 255) {
			this.runLength++;
		} else {
			this.writeRun();
			this.runByte = value;
			this.runLength = 1;
		}
	}

	/**
	 * <h3 class="en-US">Append the pending run to the RLE1 block</h3>
	 * <h3 class="zh-CN">将等待处理的重复段追加到 RLE1 编码块</h3>
	 *
	 * <p class="en-US">Runs shorter than four bytes remain literals. Longer runs become four copies followed by one byte containing runLength minus four. CRC accumulation uses the original run.</p>
	 * <p class="zh-CN">少于四个字节的段直接保存字面量；较长段保存四个相同字节，再追加值为 runLength 减四的计数字节。CRC 按原始重复段累计。</p>
	 *
	 * <p class="en-US">If the encoded run does not fit with the reserved margin, the preceding block is encoded before this run is added to the next block and its CRC.</p>
	 * <p class="zh-CN">若计入保留余量后容不下当前编码段，先写完前一块，再将本段及其 CRC 归入下一块。</p>
	 *
	 * @throws IOException <span class="en-US">Writing a full block fails</span>
	 *                     <span class="zh-CN">写入已满数据块失败</span>
	 */
	private void writeRun() throws IOException {
		if (this.runLength == 0) {
			return;
		}
		int encodedLength = this.runLength < 4 ? this.runLength : 5;
		if (this.blockLength + encodedLength > this.block.length - 19) {
			this.writeBlock();
		}
		Arrays.fill(this.run, 0, this.runLength, (byte) this.runByte);
		this.crc.append(this.run, 0, this.runLength);
		int copies = Math.min(this.runLength, 4);
		Arrays.fill(this.block, this.blockLength, this.blockLength + copies, (byte) this.runByte);
		this.blockLength += copies;
		if (this.runLength >= 4) {
			this.block[this.blockLength++] = (byte) (this.runLength - 4);
		}
		this.runLength = 0;
	}

	/**
	 * <h3 class="en-US">Transform and encode the current RLE1 block</h3>
	 * <h3 class="zh-CN">变换并编码当前 RLE1 数据块</h3>
	 *
	 * <p class="en-US">Builds the cyclic BWT order, converts its last column through MTF and RLE2, constructs a length-limited canonical code book, then writes metadata and Huffman-coded symbols.</p>
	 * <p class="zh-CN">构建循环 BWT 排序，将最后一列转换为 MTF、RLE2 符号，构造限长规范码表，再写出元数据和 Huffman 编码符号。</p>
	 *
	 * <p class="en-US">Writes two identical Huffman tables, selectors for table zero, and a zero randomization flag. Empty blocks are not emitted. Successful completion updates the combined CRC and resets the block.</p>
	 * <p class="zh-CN">写出两张相同的 Huffman 表、指向第零张表的选择器及零值随机化标志，不写出空块。成功后更新组合 CRC 并重置当前块。</p>
	 *
	 * @throws IOException <span class="en-US">Writing encoded block bytes fails</span>
	 *                     <span class="zh-CN">写入编码后的块数据失败</span>
	 */
	private void writeBlock() throws IOException {
		if (this.blockLength == 0) {
			return;
		}
		int blockCrc = (int) this.crc.getValue();
		int[] rotations = sortedRotations(this.block, this.blockLength);
		int originalPointer = 0;
		boolean[] used = new boolean[256];
		for (int index = 0; index < this.blockLength; index++) {
			used[this.block[index] & 0xFF] = true;
		}
		int[] mtf = new int[256];
		int alphabetSize = 0;
		for (int value = 0; value < 256; value++) {
			if (used[value]) {
				mtf[alphabetSize++] = value;
			}
		}
		int[] symbols = new int[this.blockLength + 1];
		int symbolCount = 0, zeros = 0;
		for (int index = 0; index < this.blockLength; index++) {
			int start = rotations[index];
			if (start == 0) originalPointer = index;
			int value = this.block[start == 0 ? this.blockLength - 1 : start - 1] & 0xFF;
			int position = 0;
			while (mtf[position] != value) {
				position++;
			}
			if (position == 0) {
				zeros++;
			} else {
				symbolCount = emitZeros(symbols, symbolCount, zeros);
				zeros = 0;
				symbols[symbolCount++] = position + 1;
				System.arraycopy(mtf, 0, mtf, 1, position);
				mtf[0] = value;
			}
		}
		symbolCount = emitZeros(symbols, symbolCount, zeros);
		symbols[symbolCount++] = alphabetSize + 1;
		int[] frequencies = new int[alphabetSize + 2];
		Arrays.fill(frequencies, 1);
		for (int index = 0; index < symbolCount; index++) {
			frequencies[symbols[index]]++;
		}
		int[] lengths = HuffmanTree.codeLengths(frequencies, 20);
		HuffmanTree.Book codes = HuffmanTree.canonicalCodes(lengths);

		this.writeBits(48, BLOCK_MAGIC);
		this.writeBits(32, blockCrc & 0xFFFFFFFFL);
		this.writeBits(1, 0);
		this.writeBits(24, originalPointer);
		boolean[] groups = new boolean[16];
		for (int value = 0; value < 256; value++) {
			if (used[value]) {
				groups[value >>> 4] = true;
			}
		}
		for (boolean present : groups) {
			this.writeBits(1, present ? 1 : 0);
		}
		for (int group = 0; group < 16; group++) {
			if (groups[group]) {
				for (int low = 0; low < 16; low++) {
					this.writeBits(1, used[group * 16 + low] ? 1 : 0);
				}
			}
		}
		this.writeBits(3, 2);
		int selectors = (symbolCount + 49) / 50;
		this.writeBits(15, selectors);
		for (int index = 0; index < selectors; index++) {
			this.writeBits(1, 0);
		}
		for (int table = 0; table < 2; table++) {
			int current = lengths[0];
			this.writeBits(5, current);
			for (int length : lengths) {
				while (current < length) {
					this.writeBits(2, 2);
					current++;
				}
				while (current > length) {
					this.writeBits(2, 3);
					current--;
				}
				this.writeBits(1, 0);
			}
		}
		for (int index = 0; index < symbolCount; index++) {
			int symbol = symbols[index];
			this.writeBits(codes.getLength(symbol), codes.getCode(symbol));
		}
		this.combinedCrc = Integer.rotateLeft(this.combinedCrc, 1) ^ blockCrc;
		this.blockLength = 0;
		this.crc.reset();
	}

	/**
	 * <h3 class="en-US">Encode a sequence of zero MTF ranks as RUNA and RUNB</h3>
	 * <h3 class="zh-CN">将连续零 MTF 排名编码为 RUNA 和 RUNB</h3>
	 *
	 * <p class="en-US">The zero is an MTF rank, not necessarily the original byte value zero. RUNA is symbol zero and RUNB is symbol one. The output never exceeds count symbols.</p>
	 * <p class="zh-CN">零指 MTF 排名，不一定对应原始字节值零。RUNA 的符号值为零，RUNB 为一；写出的符号数量不会超过 count。</p>
	 *
	 * @param symbols <span class="en-US">Destination symbol buffer with sufficient remaining capacity</span>
	 *                <span class="zh-CN">剩余容量足够的目标符号缓冲区</span>
	 * @param offset  <span class="en-US">Next output position</span>
	 *                <span class="zh-CN">下一个写入位置</span>
	 * @param count   <span class="en-US">Number of consecutive zero ranks; non-negative</span>
	 *                <span class="zh-CN">连续零排名数量，必须非负</span>
	 * @return <span class="en-US">Next output position after appending the run symbols</span>
	 * <span class="zh-CN">追加重复段符号后的下一个写入位置</span>
	 */
	private static int emitZeros(final int[] symbols, int offset, final int count) {
		if (count == 0) {
			return offset;
		}
		int value = count - 1;
		while (true) {
			symbols[offset++] = value & 1;
			if (value < 2) {
				return offset;
			}
			value = (value - 2) >>> 1;
		}
	}

	/**
	 * <h3 class="en-US">Sort cyclic rotations using prefix doubling and stable counting sorts</h3>
	 * <h3 class="zh-CN">通过前缀倍增和稳定计数排序排列所有循环旋转</h3>
	 *
	 * <p class="en-US">Returns rotation start indexes without allocating a rotation matrix. Time complexity is O(n log n), with O(n) auxiliary storage, including periodic input.</p>
	 * <p class="zh-CN">只返回旋转起点下标，不构建旋转矩阵；包括周期性输入在内，时间复杂度为 O(n log n)，辅助空间为 O(n)。</p>
	 *
	 * @param bytes <span class="en-US">Block data; only the first size bytes participate</span>
	 *              <span class="zh-CN">块数据，仅前 size 个字节参与排序</span>
	 * @param size  <span class="en-US">Positive block length; internal callers guarantee valid bounds</span>
	 *              <span class="zh-CN">大于零的块长度，由内部调用方保证范围有效</span>
	 * @return <span class="en-US">Start indexes of cyclic rotations in lexicographic order</span>
	 * <span class="zh-CN">按字典序排列的循环旋转起点下标数组</span>
	 */
	private static int[] sortedRotations(final byte[] bytes, final int size) {
		int[] order = new int[size], ranks = new int[size], shifted = new int[size], next = new int[size],
				counts = new int[Math.max(size, 256)];
		for (int index = 0; index < size; index++) {
			counts[bytes[index] & 0xFF]++;
		}
		for (int value = 1; value < 256; value++) {
			counts[value] += counts[value - 1];
		}
		for (int index = size - 1; index >= 0; index--) {
			order[--counts[bytes[index] & 0xFF]] = index;
		}
		int classes = 1;
		ranks[order[0]] = 0;
		for (int index = 1; index < size; index++) {
			if (bytes[order[index]] != bytes[order[index - 1]]) classes++;
			ranks[order[index]] = classes - 1;
		}
		for (int width = 1; width < size && classes < size; width <<= 1) {
			Arrays.fill(counts, 0, classes, 0);
			for (int index = 0; index < size; index++) {
				int start = order[index] - width;
				if (start < 0) start += size;
				shifted[index] = start;
				counts[ranks[start]]++;
			}
			for (int value = 1; value < classes; value++) counts[value] += counts[value - 1];
			for (int index = size - 1; index >= 0; index--) {
				int start = shifted[index];
				order[--counts[ranks[start]]] = start;
			}
			int newClasses = 1;
			next[order[0]] = 0;
			for (int index = 1; index < size; index++) {
				int current = order[index], previous = order[index - 1];
				if (ranks[current] != ranks[previous]
						|| ranks[(current + width) % size] != ranks[(previous + width) % size]) newClasses++;
				next[current] = newClasses - 1;
			}
			int[] swap = ranks;
			ranks = next;
			next = swap;
			classes = newClasses;
		}
		return order;
	}

	/**
	 * <h3 class="en-US">Append the low count bits in most-significant-bit-first order</h3>
	 * <h3 class="zh-CN">按最高有效位优先顺序追加低 count 位数据</h3>
	 *
	 * <p class="en-US">Processes at most 24 bits at a time, emits complete bytes into the encoded buffer, and retains fewer than eight remaining bits. Blocks need not start on a byte boundary.</p>
	 * <p class="zh-CN">每次最多处理 24 位，将完整字节加入编码缓冲区，并保留不足八位的剩余内容；块不要求从字节边界开始。</p>
	 *
	 * @param count <span class="en-US">Number of valid bits, from 0 to 48 for current internal callers</span>
	 *              <span class="zh-CN">有效位数，当前内部调用范围为 0 至 48</span>
	 * @param value <span class="en-US">Right-aligned bits; unused high bits are ignored</span>
	 *              <span class="zh-CN">低位对齐的数据，未使用的高位会被忽略</span>
	 * @throws IOException <span class="en-US">Draining a full encoded-byte buffer fails</span>
	 *                     <span class="zh-CN">写出已满的编码字节缓冲区失败</span>
	 */
	private void writeBits(int count, final long value) throws IOException {
		while (count > 0) {
			int take = Math.min(count, 24);
			int shift = count - take;
			this.bitBuffer = (this.bitBuffer << take) | ((value >>> shift) & ((1L << take) - 1));
			this.bitCount += take;
			count -= take;
			while (this.bitCount >= 8) {
				this.bitCount -= 8;
				this.encoded[this.encodedLength++] = (byte) (this.bitBuffer >>> this.bitCount);
				if (this.encodedLength == this.encoded.length) this.drainEncoded();
			}
			this.bitBuffer &= (1L << this.bitCount) - 1;
		}
	}

	/**
	 * <h3 class="en-US">Write buffered complete compressed bytes to the delegate</h3>
	 * <h3 class="zh-CN">将缓冲区内完整的压缩字节写入底层流</h3>
	 *
	 * <p class="en-US">Does not pad or emit pending partial bits, finish a block or flush the delegate. The valid byte count is reset only after the delegate write succeeds.</p>
	 * <p class="zh-CN">不会补齐或输出暂存的不足八位数据，不结束块，也不调用底层 flush。只有底层写入成功后才清空有效字节计数。</p>
	 *
	 * @throws IOException <span class="en-US">The delegate write fails</span>
	 *                     <span class="zh-CN">写入底层流失败</span>
	 */
	private void drainEncoded() throws IOException {
		if (this.encodedLength > 0) {
			this.output.write(this.encoded, 0, this.encodedLength);
			this.encodedLength = 0;
		}
	}

	/**
	 * <h3 class="en-US">Finish the BZip2 member without closing the delegate</h3>
	 * <h3 class="zh-CN">完成 BZip2 成员但不关闭底层输出流</h3>
	 *
	 * <p class="en-US">Encodes the remaining run and block, writes the end marker and combined CRC, pads the last byte with zero bits and drains complete encoded bytes.</p>
	 * <p class="zh-CN">编码剩余重复段和数据块，写出结束标记与组合 CRC，用零位补齐最后一个字节，并写出所有完整编码字节。</p>
	 *
	 * <p class="en-US">May be repeated after successful completion while the stream is open. Does not flush the delegate itself. Further original input is rejected after success; empty input still produces a valid member.</p>
	 * <p class="zh-CN">在流未关闭且已经成功完成时可重复调用；不主动调用底层 flush。成功后不再接收原始输入，空输入也会形成有效成员。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is closed or previously failed, or writing the final data fails</span>
	 *                     <span class="zh-CN">流已关闭、此前失败，或写入最后的数据失败</span>
	 */
	public void finish() throws IOException {
		this.ensureOpen();
		if (this.finished) {
			return;
		}
		try {
			this.writeRun();
			this.writeBlock();
			this.writeBits(48, END_MAGIC);
			this.writeBits(32, this.combinedCrc & 0xFFFFFFFFL);
			if (this.bitCount > 0) {
				this.writeBits(8 - this.bitCount, 0);
			}
			this.drainEncoded();
			this.finished = true;
			this.block = null;
		} catch (IOException e) {
			this.failure = e;
			throw e;
		}
	}

	/**
	 * <h3 class="en-US">Flush already encoded complete bytes</h3>
	 * <h3 class="zh-CN">刷新已经编码的完整字节</h3>
	 *
	 * <p class="en-US">Drains the encoded-byte buffer and flushes the delegate. Pending RLE1 input, an unfinished block and partial bits remain pending; this does not make an unfinished member independently decodable.</p>
	 * <p class="zh-CN">写出编码字节缓冲区并刷新底层流。尚未编码的 RLE1 输入、未完成的块及不足八位的数据继续保留，不会因此将未结束成员变成完整可解码的流。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is closed or previously failed, or flushing fails</span>
	 *                     <span class="zh-CN">流已关闭、此前失败，或刷新失败</span>
	 */
	@Override
	public void flush() throws IOException {
		this.ensureOpen();
		try {
			this.drainEncoded();
			this.output.flush();
		} catch (IOException e) {
			this.failure = e;
			throw e;
		}
	}

	/**
	 * <h3 class="en-US">Finish when possible and close the delegate</h3>
	 * <h3 class="zh-CN">在可行时完成编码并关闭底层流</h3>
	 *
	 * <p class="en-US">Idempotent. Attempts finish only if no earlier I/O failure was recorded. The delegate is closed even if finishing fails, and a separate close failure is suppressed on the finishing exception.</p>
	 * <p class="zh-CN">可重复调用。仅在此前未记录 I/O 失败时尝试 finish；finish 失败也会关闭底层流，独立的关闭异常附加到完成异常中。</p>
	 *
	 * <p class="en-US">An earlier write or flush failure is not rethrown solely because close was called. Newly raised finishing or closing errors are still reported.</p>
	 * <p class="zh-CN">不会仅因调用 close 而重新抛出此前已报告的写入或刷新异常；本次新发生的完成或关闭异常仍会报告。</p>
	 *
	 * @throws IOException <span class="en-US">Finishing the member or closing the delegate fails</span>
	 *                     <span class="zh-CN">完成成员或关闭底层流失败</span>
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		IOException error = null;
		try {
			if (this.failure == null) this.finish();
		} catch (IOException e) {
			error = e;
		} finally {
			this.closed = true;
			this.block = null;
			try {
				this.output.close();
			} catch (IOException e) {
				if (error == null) error = e;
				else if (error != e) error.addSuppressed(e);
			}
		}
		if (error != null) {
			throw error;
		}
	}

	/**
	 * <h3 class="en-US">Check that the output is open and has not failed</h3>
	 * <h3 class="zh-CN">检查输出流是否未关闭且未发生 I/O 失败</h3>
	 *
	 * <p class="en-US">Does not reject a successfully finished member, because flushing is still allowed before close. A recorded failure prevents replaying partially written output.</p>
	 * <p class="zh-CN">不会拒绝已经成功完成的成员，因为关闭前仍允许 flush；已经记录的失败会阻止重试部分写出的编码数据。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is closed or previously failed</span>
	 *                     <span class="zh-CN">流已关闭或此前已经失败</span>
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("BZip2 output stream is closed");
		}
		if (this.failure != null) {
			throw new IOException("BZip2 output stream has failed", this.failure);
		}
	}

	/**
	 * <h3 class="en-US">Check that original input can still be accepted</h3>
	 * <h3 class="zh-CN">检查是否仍允许接收原始输入</h3>
	 *
	 * <p class="en-US">Requires an open, non-failed stream whose member has not been finished.</p>
	 * <p class="zh-CN">要求输出流未关闭、未失败，且成员尚未完成。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is closed, failed or already finished</span>
	 *                     <span class="zh-CN">流已关闭、已失败或已经完成</span>
	 */
	private void ensureWritable() throws IOException {
		this.ensureOpen();
		if (this.finished) {
			throw new IOException("BZip2 output stream is finished");
		}
	}
}
