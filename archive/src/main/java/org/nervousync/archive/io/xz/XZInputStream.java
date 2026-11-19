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

package org.nervousync.archive.io.xz;

import org.nervousync.archive.enumerations.xz.XZCheck;
import org.nervousync.archive.io.counting.CountingInputStream;
import org.nervousync.archive.io.lzma.LZMA2InputStream;
import org.nervousync.archive.io.lzma.LZMAInputStream;
import org.nervousync.utils.commons.RawUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * <h2 class="en-US">Streaming XZ decoder supporting a single LZMA2 filter.</h2>
 * <h2 class="zh-CN">支持单个 LZMA2 过滤器的流式 XZ 解码器。</h2>
 * <p class="en-US">Supports multiple Blocks, all four standard Checks and optional concatenated Streams.
 * Other filter chains and reserved Check IDs are rejected. Index verification uses an ordered
 * SHA-256 summary, without retaining a list of Blocks. No random access is provided.</p>
 * <p class="zh-CN">支持多个 Block、四种标准校验及可选的拼接 Stream；拒绝其他过滤器链及保留校验标识。
 * 索引使用有序 SHA-256 摘要验证，无需保存 Block 列表，不提供随机访问。</p>
 * <p class="en-US">Construction reads no bytes and transfers source ownership on success. Read through
 * EOF to validate the complete container. close does not drain. Single-Stream mode stops immediately
 * after its Footer, leaving padding and following data untouched. Not thread safe.</p>
 * <p class="zh-CN">构造时不读取字节，成功后负责关闭输入源。读取至 EOF 才能验证整个容器，close 不主动读完。
 * 单 Stream 模式在流尾之后立即停止，保留填充及后续数据。非线程安全。</p>
 * <p class="en-US">The dictionary ceiling is not a total-output limit. Enforce output/ratio limits
 * in the caller. Checks detect corruption but do not authenticate data.</p>
 * <p class="zh-CN">字典上限不是总输出限制，调用方应管理总输出及压缩比限制；校验用于检测损坏，不提供身份认证。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class XZInputStream extends InputStream {
	/**
	 * <span class="en-US">Owned compressed source.</span>
	 * <span class="zh-CN">负责关闭的压缩输入源。</span>
	 */
	private final InputStream input;
	/**
	 * <span class="en-US">Whether standalone concatenated Streams and padding are consumed.</span>
	 * <span class="zh-CN">是否消费独立文件的拼接 Stream 和填充。</span>
	 */
	private final boolean concatenated;
	/**
	 * <span class="en-US">Maximum dictionary bytes accepted before allocation.</span>
	 * <span class="zh-CN">分配前允许的最大字典字节数。</span>
	 */
	private final int maxDictionary;
	/**
	 * <span class="en-US">Current Stream Check selection.</span>
	 * <span class="zh-CN">当前 Stream 的校验类型。</span>
	 */
	private XZCheck checkType;
	/**
	 * <span class="en-US">Actual Block records for the current Stream.</span>
	 * <span class="zh-CN">当前 Stream 的实际 Block 记录摘要。</span>
	 */
	private XZCodec.Index index;
	/**
	 * <span class="en-US">Current Block decoder; null between Blocks.</span>
	 * <span class="zh-CN">当前 Block 解码器，块之间为 null。</span>
	 */
	private LZMA2InputStream block;
	/**
	 * <span class="en-US">Exact compressed bytes consumed by the current Block.</span>
	 * <span class="zh-CN">当前 Block 精确消费的压缩字节数。</span>
	 */
	private CountingInputStream blockInput;
	/**
	 * <span class="en-US">Check of current uncompressed Block bytes.</span>
	 * <span class="zh-CN">当前 Block 未压缩字节的校验状态。</span>
	 */
	private XZCodec.Check check;
	/**
	 * <span class="en-US">Current Block Header byte count.</span>
	 * <span class="zh-CN">当前 Block 头字节数。</span>
	 */
	private int blockHeaderSize;
	/**
	 * <span class="en-US">Optional declared compressed size, or -1.</span>
	 * <span class="zh-CN">可选的声明压缩大小，未提供时为 -1。</span>
	 */
	private long expectedCompressed;
	/**
	 * <span class="en-US">Optional declared uncompressed size, or -1.</span>
	 * <span class="zh-CN">可选的声明未压缩大小，未提供时为 -1。</span>
	 */
	private long expectedUncompressed;
	/**
	 * <span class="en-US">Uncompressed bytes returned from this Block.</span>
	 * <span class="zh-CN">当前 Block 已返回的未压缩字节数。</span>
	 */
	private long blockSize;
	/**
	 * <span class="en-US">A valid Stream Header is active.</span>
	 * <span class="zh-CN">已有生效的合法流头。</span>
	 */
	private boolean streamOpen;
	/**
	 * <span class="en-US">At least one Stream has been fully verified.</span>
	 * <span class="zh-CN">至少已有一个 Stream 完整验证。</span>
	 */
	private boolean hadStream;
	/**
	 * <span class="en-US">End according to the selected concatenation policy.</span>
	 * <span class="zh-CN">已按所选拼接策略结束。</span>
	 */
	private boolean finished;
	/**
	 * <span class="en-US">Closure initiated, including failed source close.</span>
	 * <span class="zh-CN">已开始关闭，包括输入源关闭失败的情况。</span>
	 */
	private boolean closed;
	/**
	 * <span class="en-US">First I/O or format failure.</span>
	 * <span class="zh-CN">首次 I/O 或格式异常。</span>
	 */
	private IOException failure;

	/**
	 * <h4 class="en-US">Create a standalone decoder with concatenation enabled.</h4>
	 * <h4 class="zh-CN">创建启用拼接的独立文件解码器。</h4>
	 *
	 * @param input compressed source / 压缩输入源
	 */
	public XZInputStream(InputStream input) {
		this(input, LZMAInputStream.DEFAULT_MAX_DICTIONARY, true);
	}

	/**
	 * <h4 class="en-US">Select standalone or embedded single-Stream decoding.</h4>
	 * <h4 class="zh-CN">选择独立文件或嵌入式单 Stream 解码。</h4>
	 *
	 * @param input        compressed source / 压缩输入源
	 * @param concatenated true consumes concatenated Streams; false preserves following data / true 消费拼接 Stream；false 保留后续数据
	 */
	public XZInputStream(InputStream input, boolean concatenated) {
		this(input, LZMAInputStream.DEFAULT_MAX_DICTIONARY, concatenated);
	}

	/**
	 * <h4 class="en-US">Create a decoder with an explicit dictionary ceiling.</h4>
	 * <h4 class="zh-CN">指定字典上限创建解码器。</h4>
	 *
	 * @param input         source; not consumed during construction / 输入源，构造时不读取
	 * @param maxDictionary maximum dictionary bytes, at least 4096 / 最大字典字节数，至少 4096
	 * @param concatenated  whether to consume padding and further Streams / 是否消费填充及后续 Stream
	 * @throws IllegalArgumentException dictionary ceiling is below 4096 / 字典上限小于 4096
	 * @throws NullPointerException     source is null / 输入源为 null
	 */
	public XZInputStream(InputStream input, int maxDictionary, boolean concatenated) {
		this.input = Objects.requireNonNull(input, "input");
		if (maxDictionary < 4096) {
			throw new IllegalArgumentException("XZ dictionary ceiling must be at least 4096");
		}
		this.maxDictionary = maxDictionary;
		this.concatenated = concatenated;
	}

	/**
	 * <span class="en-US">Read a Header, or verify standalone trailing padding and physical EOF.</span>
	 * <span class="zh-CN">读取流头，或验证独立文件尾部填充及物理 EOF。</span>
	 */
	private boolean beginStream() throws IOException {
		int first = this.input.read();
		if (this.hadStream) {
			int padding = 0;
			while (first == 0) {
				padding = (padding + 1) & 3;
				first = this.input.read();
			}
			if (padding != 0) {
				throw new IOException("XZ Stream Padding is not a multiple of four");
			}
			if (first == -1) {
				this.finished = true;
				return false;
			}
		}
		if (first != 0xFD) {
			throw new IOException("Missing XZ Stream Header");
		}
		byte[] header = new byte[12];
		header[0] = (byte) first;
		XZCodec.readFully(this.input, header, 1, 11);
		if (header[1] != '7' || header[2] != 'z' || header[3] != 'X'
				|| header[4] != 'Z' || header[5] != 0) {
			throw new IOException("Invalid XZ Header magic");
		}
		XZCodec.verifyCrc(header, 6, 2, 8);
		if (header[6] != 0 || (header[7] & 0xF0) != 0) {
			throw new IOException("Unsupported XZ Stream Flags");
		}
		this.checkType = XZCheck.from(header[7] & 15);
		this.index = new XZCodec.Index();
		this.streamOpen = true;
		return true;
	}

	/**
	 * <span class="en-US">Validate a bounded Block Header before allocating the dictionary.</span>
	 * <span class="zh-CN">分配字典前验证有界的 Block 头。</span>
	 */
	private void beginBlock(int first) throws IOException {
		this.blockHeaderSize = (first + 1) * 4;
		byte[] header = new byte[this.blockHeaderSize];
		header[0] = (byte) first;
		XZCodec.readFully(this.input, header, 1, header.length - 1);
		XZCodec.verifyCrc(header, 0, header.length - 4, header.length - 4);
		int flags = header[1] & 255;
		if ((flags & 0x3C) != 0 || (flags & 3) != 0) {
			throw new IOException("Unsupported XZ Block Flags or filter chain");
		}
		ByteArrayInputStream fields = new ByteArrayInputStream(header, 2, header.length - 6);
		this.expectedCompressed = (flags & 0x40) == 0 ? -1 : XZCodec.readVli(fields);
		this.expectedUncompressed = (flags & 0x80) == 0 ? -1 : XZCodec.readVli(fields);
		if (this.expectedCompressed == 0) {
			throw new IOException("XZ Compressed Size must be nonzero");
		}
		long filter = XZCodec.readVli(fields);
		long properties = XZCodec.readVli(fields);
		if (filter != 0x21 || properties != 1) {
			throw new IOException("Only a single XZ LZMA2 filter is supported");
		}
		int property = XZCodec.readByte(fields);
		if (property > 40) {
			throw new IOException("Invalid XZ LZMA2 dictionary property");
		}
		long dictionary = property == 40 ? 0xFFFFFFFFL : (2L | (property & 1)) << (property / 2 + 11);
		if (dictionary > this.maxDictionary) {
			throw new IOException("XZ dictionary exceeds configured bound");
		}
		while (fields.available() != 0) {
			if (XZCodec.readByte(fields) != 0) {
				throw new IOException("Nonzero XZ Block Header Padding");
			}
		}
		this.blockInput = new CountingInputStream(this.input, this.expectedCompressed);
		this.block = new LZMA2InputStream(this.blockInput, (int) dictionary, this.maxDictionary);
		this.check = new XZCodec.Check(this.checkType);
		this.blockSize = 0;
	}

	/**
	 * <span class="en-US">Verify raw termination, sizes, Block Padding and data Check.</span>
	 * <span class="zh-CN">验证原始流结束、大小、Block 填充及数据校验。</span>
	 */
	private void endBlock() throws IOException {
		if ((this.expectedCompressed >= 0 && this.expectedCompressed != this.blockInput.getCount())
				|| (this.expectedUncompressed >= 0 && this.expectedUncompressed != this.blockSize)) {
			throw new IOException("XZ Block size mismatch");
		}
		int padding = (int) (-this.blockInput.getCount() & 3);
		for (int index = 0; index < padding; index++) {
			if (XZCodec.readByte(this.input) != 0) {
				throw new IOException("Nonzero XZ Block Padding");
			}
		}
		byte[] stored = new byte[this.checkType.getSize()];
		XZCodec.readFully(this.input, stored, 0, stored.length);
		if (!MessageDigest.isEqual(stored, this.check.finish())) {
			throw new IOException("XZ Block Check mismatch");
		}
		long unpadded = RawUtils.add(this.blockHeaderSize, RawUtils.add(this.blockInput.getCount(), stored.length));
		this.index.addRecord(unpadded, this.blockSize);
		this.block.close();
		this.block = null;
		this.blockInput = null;
		this.check = null;
	}

	/**
	 * <span class="en-US">Validate Index records, Index CRC, Backward Size, Footer CRC and flags.</span>
	 * <span class="zh-CN">验证索引记录及 CRC、回溯大小、流尾 CRC 和标志。</span>
	 */
	private void endStream() throws IOException {
		CountingInputStream counted = new CountingInputStream(this.input, XZCodec.MAX_INDEX_SIZE - 5);
		CRC32 crc = new CRC32();
		crc.update(0); // Index Indicator has already been consumed. / 索引指示字节已经消费。
		CheckedInputStream fields = new CheckedInputStream(counted, crc);
		long records = XZCodec.readVli(fields);
		if (records != this.index.records) {
			throw new IOException("XZ Index record count mismatch");
		}
		XZCodec.Index declared = new XZCodec.Index();
		for (long record = 0; record < records; record++) {
			declared.addRecord(XZCodec.readVli(fields), XZCodec.readVli(fields));
		}
		while (((counted.getCount() + 1) & 3) != 0) {
			if (XZCodec.readByte(fields) != 0) {
				throw new IOException("Nonzero XZ Index Padding");
			}
		}
		byte[] stored = new byte[4];
		XZCodec.readFully(this.input, stored, 0, 4);

//		if (crc.getValue() != XZCodec.littleEndian(stored, 0, 4)) {
		if (crc.getValue() != RawUtils.readUnsignedInt(stored, 0, ByteOrder.LITTLE_ENDIAN)) {
			throw new IOException("XZ Index CRC32 mismatch");
		}
		this.index.verify(declared);
		long indexSize = counted.getCount() + 5;
		byte[] footer = new byte[12];
		XZCodec.readFully(this.input, footer, 0, footer.length);
		XZCodec.verifyCrc(footer, 4, 6, 0);
		long backwardSize = (RawUtils.readUnsignedInt(footer, 4, ByteOrder.LITTLE_ENDIAN) + 1L) * 4L;
		if (backwardSize != indexSize || footer[8] != 0 || (footer[9] & 255) != this.checkType.getCode()
				|| footer[10] != 'Y' || footer[11] != 'Z') {
			throw new IOException("XZ Footer does not match Stream");
		}
		RawUtils.check(this.index.blocksSize, RawUtils.add(indexSize, 24));
		this.streamOpen = false;
		this.hadStream = true;
		this.index = null;
		if (!this.concatenated) {
			this.finished = true;
		}
	}

	/**
	 * <span class="en-US">Refuse closed or previously failed decoding.</span>
	 * <span class="zh-CN">拒绝关闭后或先前失败后的解码。</span>
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("XZ input closed");
		}
		if (this.failure != null) {
			throw this.failure;
		}
	}

	/**
	 * <span class="en-US">Read one unsigned output byte, or -1 after complete validation.</span>
	 * <span class="zh-CN">读取一个无符号输出字节，完整验证结束后返回 -1。</span>
	 */
	@Override
	public int read() throws IOException {
		byte[] one = new byte[1];
		return this.read(one, 0, 1) < 0 ? -1 : one[0] & 255;
	}

	/**
	 * <h4 class="en-US">Read uncompressed bytes, progressing across Blocks and Streams.</h4>
	 * <h4 class="zh-CN">读取未压缩字节，并跨越 Block 及 Stream。</h4>
	 *
	 * @param bytes  destination array / 目标数组
	 * @param offset destination start / 目标起始位置
	 * @param length maximum requested bytes / 最多请求的字节数
	 * @return byte count, zero for a zero-length request, or -1 at verified end / 字节数，零长度请求为 0，验证结束后为 -1
	 * @throws IOException format/check failure, delegate failure or closed stream / 格式或校验错误、底层流失败或流已关闭
	 */
	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		this.ensureOpen();
		if (length == 0) {
			return 0;
		}
		try {
			while (!this.finished) {
				if (!this.streamOpen && !this.beginStream()) {
					break;
				}
				if (this.block == null) {
					int first = XZCodec.readByte(this.input);
					if (first == 0) {
						this.endStream();
						continue;
					}
					this.beginBlock(first);
				}
				int count = this.block.read(bytes, offset, length);
				if (count < 0) {
					this.endBlock();
					continue;
				}
				this.blockSize = RawUtils.add(this.blockSize, count);
				if (this.expectedUncompressed >= 0 && this.blockSize > this.expectedUncompressed) {
					throw new IOException("XZ output exceeds declared Block size");
				}
				this.check.update(bytes, offset, count);
				return count;
			}
			return -1;
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <span class="en-US">Release the active dictionary and close once, without reading remaining data.</span>
	 * <span class="zh-CN">释放当前字典并只关闭一次，不读取剩余数据。</span>
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		try {
			if (this.block != null) {
				this.block.close();
			}
		} finally {
			this.block = null;
			this.blockInput = null;
			this.check = null;
			this.index = null;
			this.input.close();
		}
	}
}