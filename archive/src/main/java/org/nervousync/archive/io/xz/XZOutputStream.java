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
import org.nervousync.archive.io.counting.CountingOutputStream;
import org.nervousync.archive.io.lzma.LZMA2OutputStream;
import org.nervousync.utils.commons.RawUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * <h2 class="en-US">Bounded-memory XZ encoder with one streaming LZMA2 Block.</h2>
 * <h2 class="zh-CN">使用单个流式 LZMA2 Block 的有界内存 XZ 编码器。</h2>
 * <p class="en-US">Empty input produces an empty Stream without Blocks. Nonempty input uses one Block
 * with a fixed 64 KiB dictionary and independent LZMA2 chunks. Levels 0..9 retain the current LZMA2
 * encoder's matching behavior. No BCJ/Delta filters or parallel encoding are provided.</p>
 * <p class="zh-CN">空输入产生没有 Block 的空 Stream；非空输入使用一个 Block，固定 64 KiB 字典及独立 LZMA2 分块。
 * 等级 0..9 沿用现有 LZMA2 编码器的匹配行为，不提供 BCJ/Delta 过滤器或并行编码。</p>
 * <p class="en-US">Construction writes the Stream Header. Successful construction transfers destination
 * ownership. flush completes pending LZMA2 chunks but leaves the Block open. finish completes the Block,
 * Index and Footer without closing the destination. close finishes when possible and closes once.</p>
 * <p class="zh-CN">构造时写流头，成功后负责管理输出目标。flush 完成待处理 LZMA2 分块但保留当前 Block；
 * finish 完成 Block、索引和流尾，不关闭目标；close 在可行时完成编码并只关闭一次。</p>
 * <p class="en-US">Not thread safe. A failed output must not be retried or treated as a complete file.
 * Checks detect corruption, not malicious modification or sender identity.</p>
 * <p class="zh-CN">非线程安全；输出失败后不得重试，也不能将部分结果当作完整文件。
 * 校验检测数据损坏，不验证恶意修改或发送方身份。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class XZOutputStream extends OutputStream {
	/**
	 * <span class="en-US">Owned compressed destination.</span>
	 * <span class="zh-CN">负责关闭的压缩输出目标。</span>
	 */
	private final OutputStream output;
	/**
	 * <span class="en-US">Validated LZMA2 match-search level.</span>
	 * <span class="zh-CN">已验证的 LZMA2 匹配搜索等级。</span>
	 */
	private final int level;
	/**
	 * <span class="en-US">Per-Block data Check type.</span>
	 * <span class="zh-CN">每个 Block 的数据校验类型。</span>
	 */
	private final XZCheck checkType;
	/**
	 * <span class="en-US">Active Block encoder, allocated on the first nonempty write.</span>
	 * <span class="zh-CN">首次非空写入时分配的当前 Block 编码器。</span>
	 */
	private LZMA2OutputStream block;
	/**
	 * <span class="en-US">Actual compressed Block byte count.</span>
	 * <span class="zh-CN">实际压缩 Block 字节数。</span>
	 */
	private CountingOutputStream blockOutput;
	/**
	 * <span class="en-US">Current uncompressed Check state.</span>
	 * <span class="zh-CN">当前未压缩数据校验状态。</span>
	 */
	private XZCodec.Check check;
	/**
	 * <span class="en-US">Total accepted uncompressed bytes.</span>
	 * <span class="zh-CN">已接受的未压缩字节总数。</span>
	 */
	private long uncompressedSize;
	/**
	 * <span class="en-US">Final Block size excluding padding, or zero for empty input.</span>
	 * <span class="zh-CN">不含填充的最终 Block 大小，空输入时为零。</span>
	 */
	private long unpaddedSize;
	/**
	 * <span class="en-US">All output including Footer has been flushed successfully.</span>
	 * <span class="zh-CN">包括流尾在内的所有输出均已成功刷新。</span>
	 */
	private boolean finished;
	/**
	 * <span class="en-US">Closure initiated.</span>
	 * <span class="zh-CN">已开始关闭。</span>
	 */
	private boolean closed;
	/**
	 * <span class="en-US">First output failure prevents encoding retries.</span>
	 * <span class="zh-CN">首次输出失败阻止后续编码重试。</span>
	 */
	private IOException failure;

	/**
	 * <h4 class="en-US">Create an XZ encoder with CRC64 data Checks.</h4>
	 * <h4 class="zh-CN">创建使用 CRC64 数据校验的 XZ 编码器。</h4>
	 * @param output destination / 输出目标
	 * @param level LZMA2 level 0..9 / LZMA2 等级 0..9
	 * @throws IOException writing the Header fails / 写入流头失败
	 */
	public XZOutputStream(OutputStream output, int level) throws IOException {
		this(output, level, XZCheck.CRC64);
	}

	/**
	 * <h4 class="en-US">Create an XZ encoder with an explicit Check.</h4>
	 * <h4 class="zh-CN">指定校验类型创建 XZ 编码器。</h4>
	 * @param output caller-owned if construction fails / 构造失败时仍由调用方管理的目标
	 * @param level match-search level 0..9; zero still compresses / 匹配搜索等级 0..9，零等级仍压缩
	 * @param checkType NONE, CRC32, CRC64 or SHA256 / NONE、CRC32、CRC64 或 SHA256
	 * @throws IOException writing the Stream Header fails / 写入流头失败
	 * @throws IllegalArgumentException level is outside 0..9 / 等级不在 0..9 范围内
	 * @throws NullPointerException destination or Check is null / 目标或校验类型为 null
	 */
	public XZOutputStream(OutputStream output, int level, XZCheck checkType) throws IOException {
		this.output = Objects.requireNonNull(output, "output");
		this.checkType = Objects.requireNonNull(checkType, "checkType");
		if (level < 0 || level > 9) {
			throw new IllegalArgumentException("XZ level must be 0..9");
		}
		this.level = level;
		byte[] header = {(byte) 0xFD, '7', 'z', 'X', 'Z', 0, 0, (byte) checkType.getCode()};
		this.output.write(header);
		this.output.write(RawUtils.longToBytes(XZCodec.crc32(header, 6, 2), ByteOrder.LITTLE_ENDIAN, 4));
	}

	/**
	 * <span class="en-US">Write the single-filter Header, omitting optional size fields for streaming.</span>
	 * <span class="zh-CN">写单过滤器 Block 头，流式输出省略可选大小字段。</span>
	 */
	private void beginBlock() throws IOException {
		// 12-byte Header; filter 0x21, one property byte, dictionary property 8 = 64 KiB.
		// 12 字节 Block 头；过滤器 0x21，一个属性字节，字典属性 8 表示 64 KiB。
		byte[] header = {2, 0, 0x21, 1, 8, 0, 0, 0};
		this.output.write(header);
		this.output.write(RawUtils.longToBytes(XZCodec.crc32(header, 0, header.length), ByteOrder.LITTLE_ENDIAN, 4));
		this.blockOutput = new CountingOutputStream(this.output);
		this.block = new LZMA2OutputStream(this.blockOutput, this.level);
		this.check = new XZCodec.Check(this.checkType);
	}

	/**
	 * <span class="en-US">Complete raw LZMA2, then write Block Padding before its data Check.</span>
	 * <span class="zh-CN">完成原始 LZMA2，再按顺序写 Block 填充和数据校验。</span>
	 */
	private void endBlock() throws IOException {
		this.block.finish();
		int padding = (int) (-this.blockOutput.getCount() & 3);
		for (int index = 0; index < padding; index++) {
			this.output.write(0);
		}
		this.output.write(this.check.finish());
		this.unpaddedSize = RawUtils.add(12, RawUtils.add(this.blockOutput.getCount(), this.checkType.getSize()));
		this.block.close();
		this.block = null;
		this.blockOutput = null;
		this.check = null;
	}

	/**
	 * <span class="en-US">Write the bounded zero/one-record Index and matching Footer.</span>
	 * <span class="zh-CN">写有界的零或一条记录的索引及匹配流尾。</span>
	 */
	private void writeIndexAndFooter() throws IOException {
		ByteArrayOutputStream index = new ByteArrayOutputStream(24);
		index.write(0);
		XZCodec.writeVli(index, this.unpaddedSize == 0 ? 0 : 1);
		if (this.unpaddedSize != 0) {
			XZCodec.writeVli(index, this.unpaddedSize);
			XZCodec.writeVli(index, this.uncompressedSize);
		}
		while ((index.size() & 3) != 0) {
			index.write(0);
		}
		byte[] body = index.toByteArray();
		long indexSize = body.length + 4;
		long padded = RawUtils.add(this.unpaddedSize, 3L) & ~3L;
		RawUtils.check(padded, RawUtils.add(indexSize, 24));
		this.output.write(body);
		this.output.write(RawUtils.longToBytes(XZCodec.crc32(body, 0, body.length), ByteOrder.LITTLE_ENDIAN, 4));
		ByteArrayOutputStream footer = new ByteArrayOutputStream(6);
		footer.write(RawUtils.longToBytes(indexSize / 4 - 1, ByteOrder.LITTLE_ENDIAN, 4));
		footer.write(0);
		footer.write(this.checkType.getCode());
		byte[] fields = footer.toByteArray();
		this.output.write(RawUtils.longToBytes(XZCodec.crc32(fields, 0, fields.length), ByteOrder.LITTLE_ENDIAN, 4));
		this.output.write(fields);
		this.output.write('Y');
		this.output.write('Z');
	}

	/**
	 * <span class="en-US">Refuse writes after finish, close or the first failure.</span>
	 * <span class="zh-CN">拒绝结束、关闭或首次失败后的写入。</span>
	 */
	private void ensureWritable() throws IOException {
		if (this.closed || this.finished) {
			throw new IOException("XZ output closed/finished");
		}
		if (this.failure != null) {
			throw this.failure;
		}
	}

	/**
	 * <span class="en-US">Write the low eight bits as one uncompressed byte.</span>
	 * <span class="zh-CN">将最低八位作为一个未压缩字节写入。</span>
	 */
	@Override
	public void write(int value) throws IOException {
		byte[] one = {(byte) value};
		this.write(one, 0, 1);
	}

	/**
	 * <h4 class="en-US">Accept uncompressed bytes into the streaming Block.</h4>
	 * <h4 class="zh-CN">将未压缩字节写入流式 Block。</h4>
	 * @param bytes source array / 源数组
	 * @param offset source start / 源起始位置
	 * @param length source byte count / 源字节数
	 * @throws IOException output failure, size overflow or unusable stream / 输出失败、大小溢出或流不可使用
	 */
	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		this.ensureWritable();
		if (length == 0) {
			return;
		}
		try {
			long next = RawUtils.add(this.uncompressedSize, length);
			if (this.block == null) {
				this.beginBlock();
			}
			this.block.write(bytes, offset, length);
			this.check.update(bytes, offset, length);
			this.uncompressedSize = next;
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <span class="en-US">Complete pending LZMA2 chunks without ending the Block or Stream.</span>
	 * <span class="zh-CN">完成待处理 LZMA2 分块，不结束 Block 或 Stream。</span>
	 */
	@Override
	public void flush() throws IOException {
		if (this.closed) {
			throw new IOException("XZ output closed");
		}
		if (this.failure != null) {
			throw this.failure;
		}
		try {
			if (this.block != null) {
				this.block.flush();
			} else {
				this.output.flush();
			}
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <span class="en-US">Finish once and keep the destination open; retain any failure.</span>
	 * <span class="zh-CN">只完成一次并保留目标流，保存任何失败。</span>
	 */
	public void finish() throws IOException {
		if (this.closed) {
			throw new IOException("XZ output closed");
		}
		if (this.failure != null) {
			throw this.failure;
		}
		if (this.finished) {
			return;
		}
		try {
			if (this.block != null) {
				this.endBlock();
			}
			this.writeIndexAndFooter();
			this.output.flush();
			this.finished = true;
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <span class="en-US">Finish when possible, close once, and suppress a secondary close failure.</span>
	 * <span class="zh-CN">在可行时完成编码、只关闭一次，并保留次要关闭异常。</span>
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
			this.block = null;
			this.blockOutput = null;
			this.check = null;
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
