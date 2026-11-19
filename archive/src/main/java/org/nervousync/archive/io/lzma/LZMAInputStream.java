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
import java.io.InputStream;
import java.util.Objects;

/**
 * <h2 class="en-US">Streaming LZMA1 decoder for ZIP, raw and LZMA-Alone framing.</h2>
 * <h2 class="zh-CN">支持 ZIP、原始流及 LZMA-Alone 封装的 LZMA1 流式解码器。</h2>
 * <p class="en-US">Symbol and dictionary decoding is shared with LZMA2 through LZMADecoder.
 * This class owns size/EOS policy and the delegate after successful construction.
 * Not thread safe; read through EOF for complete validation. close does not drain.</p>
 * <p class="zh-CN">通过 LZMADecoder 与 LZMA2 共用符号及字典解码。本类管理大小/EOS 策略，
 * 构造成功后负责关闭底层流。非线程安全；完整验证需读取至 EOF，close 不主动读完数据。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class LZMAInputStream extends InputStream {
	/**
	 * <span class="en-US">Default dictionary ceiling, not an output-size limit.</span>
	 * <span class="zh-CN">默认字典上限，并非输出大小上限。</span>
	 */
	public static final int DEFAULT_MAX_DICTIONARY = 64 * 1024 * 1024;

	/**
	 * <span class="en-US">Owned source after construction.</span>
	 * <span class="zh-CN">构造成功后负责关闭的输入源。</span>
	 */
	private final InputStream input;

	/**
	 * <span class="en-US">Range state for this continuous LZMA1 stream.</span>
	 * <span class="zh-CN">本连续 LZMA1 流的区间状态。</span>
	 */
	private final LZMACodec.Decoder range;

	/**
	 * <span class="en-US">Shared symbol/dictionary engine.</span>
	 * <span class="zh-CN">共用符号和字典引擎。</span>
	 */
	private final LZMADecoder decoder;

	/**
	 * <span class="en-US">Known size, or -1 with required EOS.</span>
	 * <span class="zh-CN">已知大小，或要求 EOS 时的 -1。</span>
	 */
	private final long expectedSize;

	/**
	 * <span class="en-US">EOS permitted/required policy.</span>
	 * <span class="zh-CN">EOS 允许及要求策略。</span>
	 */
	private final boolean endMarker;

	/**
	 * <span class="en-US">Known-size framing may allow either ending form.</span>
	 * <span class="zh-CN">已知大小封装可允许两种结束方式。</span>
	 */
	private final boolean optionalEndMarker;

	/**
	 * <span class="en-US">Number of returned uncompressed bytes.</span>
	 * <span class="zh-CN">已返回的未压缩字节数。</span>
	 */
	private long produced;

	/**
	 * <span class="en-US">Successfully verified end.</span>
	 * <span class="zh-CN">已成功验证结束。</span>
	 */
	private boolean finished;

	/**
	 * <span class="en-US">Closure has begun.</span>
	 * <span class="zh-CN">已开始关闭。</span>
	 */
	private boolean closed;

	/**
	 * <span class="en-US">Sticky first failure.</span>
	 * <span class="zh-CN">保存首次失败。</span>
	 */
	private IOException failure;

	/**
	 * <h4 class="en-US">Read ZIP method-14 properties and LZMA1 data.</h4>
	 * <h4 class="zh-CN">读取 ZIP 方法 14 的属性及 LZMA1 数据。</h4>
	 *
	 * @param input        <span class="en-US">Already decrypted, bounded ZIP data segment.</span>
	 *                     <span class="zh-CN">已经解密且限制范围的 ZIP 数据段。</span>
	 * @param expectedSize <span class="en-US">Uncompressed size; -1 requires EOS.</span>
	 *                     <span class="zh-CN">未压缩大小；-1 要求 EOS。</span>
	 * @param endMarker    <span class="en-US">ZIP general-purpose EOS bit.</span>
	 *                     <span class="zh-CN">ZIP 通用标志中的 EOS 位。</span>
	 * @throws IOException <span class="en-US">Invalid properties or range initialization.</span>
	 *                     <span class="zh-CN">属性或区间初始化非法。</span>
	 */
	public LZMAInputStream(InputStream input, long expectedSize, boolean endMarker) throws IOException {
		this(input, readZipProperties(input), expectedSize, endMarker, DEFAULT_MAX_DICTIONARY, false);
	}

	/**
	 * <h4 class="en-US">Initialize bounded LZMA1 decoding.</h4>
	 * <h4 class="zh-CN">初始化有界的 LZMA1 解码。</h4>
	 *
	 * <p class="en-US">Validate the declared dictionary before reducing the allocation for small known outputs. Create the shared engine, then initialize the continuous range decoder.</p>
	 * <p class="zh-CN">先验证声明字典，再按较小的已知输出缩减分配；随后创建共用核心并初始化连续的区间解码器。</p>
	 *
	 * @param input             <span class="en-US">Delegate input stream.</span>
	 *                          <span class="zh-CN">底层输入流。</span>
	 * @param properties        <span class="en-US">Five property bytes parsed by the framing entry point.</span>
	 *                          <span class="zh-CN">由封装入口读取的五字节属性。</span>
	 * @param expectedSize      <span class="en-US">Declared output size, or -1 when EOS is required.</span>
	 *                          <span class="zh-CN">声明输出大小；要求 EOS 时可以为 -1。</span>
	 * @param endMarker         <span class="en-US">Whether EOS is allowed and normally required.</span>
	 *                          <span class="zh-CN">是否允许且通常要求 EOS。</span>
	 * @param maxDictionary     <span class="en-US">Maximum accepted declared dictionary size.</span>
	 *                          <span class="zh-CN">允许的声明字典大小上限。</span>
	 * @param optionalEndMarker <span class="en-US">Whether known-size input accepts either valid ending.</span>
	 *                          <span class="zh-CN">已知大小输入是否允许两种合法结束形式。</span>
	 * @throws IOException <span class="en-US">The delegate fails or encoded data is invalid or truncated.</span>
	 *                     <span class="zh-CN">底层流操作失败，或编码数据非法、截断。</span>
	 */
	private LZMAInputStream(InputStream input, byte[] properties, long expectedSize,
	                        boolean endMarker, int maxDictionary, boolean optionalEndMarker) throws IOException {
		this.input = Objects.requireNonNull(input, "input");
		if (expectedSize < -1 || (!endMarker && expectedSize < 0)) {
			throw new IllegalArgumentException("LZMA size/EOS");
		}
		if (maxDictionary < 4096) {
			throw new IllegalArgumentException("LZMA dictionary bound");
		}
		int packed = properties[0] & 255;
		if (packed >= 225) {
			throw new IOException("Invalid LZMA properties");
		}
		long declared = 0;
		for (int index = 0; index < 4; index++) {
			declared |= (long) (properties[index + 1] & 255) << (index * 8);
		}
		long dictionaryLimit = Math.max(4096L, declared);
		if (dictionaryLimit > maxDictionary) {
			throw new IOException("LZMA dictionary exceeds configured bound");
		}
		int allocation = (int) dictionaryLimit;
		if (expectedSize >= 0) {
			allocation = (int) Math.min(allocation, Math.max(1, expectedSize));
		}
		this.decoder = new LZMADecoder(packed, dictionaryLimit, allocation);
		this.expectedSize = expectedSize;
		this.endMarker = endMarker;
		this.optionalEndMarker = optionalEndMarker;
		this.range = new LZMACodec.Decoder(this.input);
	}

	/**
	 * <h4 class="en-US">Read version bytes, require five property bytes, and return those properties.</h4>
	 * <h4 class="zh-CN">读取版本字节，要求属性长度为五，并返回属性内容。</h4>
	 *
	 * <p class="en-US">The SDK-version bytes are consumed for framing; decoding is selected by the validated property layout rather than a particular version number.</p>
	 * <p class="zh-CN">SDK 版本字节用于封装解析；是否可解码取决于经过校验的属性布局，不绑定某个版本号。</p>
	 *
	 * @param input <span class="en-US">ZIP LZMA data segment after any encryption header.</span>
	 *              <span class="zh-CN">位于加密头之后的 ZIP LZMA 数据段。</span>
	 * @return <span class="en-US">Five LZMA1 property bytes.</span>
	 * <span class="zh-CN">五字节 LZMA1 属性。</span>
	 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
	 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
	 */
	private static byte[] readZipProperties(InputStream input) throws IOException {
		Objects.requireNonNull(input, "input");
		LZMACodec.readByte(input);
		LZMACodec.readByte(input);
		int size = LZMACodec.readByte(input) | (LZMACodec.readByte(input) << 8);
		if (size != 5) {
			throw new IOException("Unsupported ZIP LZMA properties size: " + size);
		}
		byte[] properties = new byte[5];
		for (int index = 0; index < 5; index++) {
			properties[index] = (byte) LZMACodec.readByte(input);
		}
		return properties;
	}

	/**
	 * <h4 class="en-US">Create a raw LZMA1 decoder with caller-supplied properties and memory bound.</h4>
	 * <h4 class="zh-CN">根据调用方提供的属性与内存上限创建原始 LZMA1 解码器。</h4>
	 *
	 * @param input         <span class="en-US">Raw range-coded data without a container header.</span>
	 *                      <span class="zh-CN">不带容器头的原始区间编码数据。</span>
	 * @param properties    <span class="en-US">Packed property byte: (pb * 5 + lp) * 9 + lc.</span>
	 *                      <span class="zh-CN">打包属性字节：(pb * 5 + lp) * 9 + lc。</span>
	 * @param dictionary    <span class="en-US">Nonnegative declared dictionary size in bytes.</span>
	 *                      <span class="zh-CN">非负的声明字典大小，单位为字节。</span>
	 * @param size          <span class="en-US">Expected uncompressed size; -1 requires EOS.</span>
	 *                      <span class="zh-CN">预期未压缩大小；-1 要求 EOS。</span>
	 * @param endMarker     <span class="en-US">Whether a LZMA EOS marker is required.</span>
	 *                      <span class="zh-CN">是否要求 LZMA EOS 标记。</span>
	 * @param maxDictionary <span class="en-US">Maximum accepted declared dictionary, at least 4096 bytes.</span>
	 *                      <span class="zh-CN">允许的声明字典上限，至少为 4096 字节。</span>
	 * @return <span class="en-US">Initialized decoder owning the delegate.</span>
	 * <span class="zh-CN">初始化后负责关闭底层流的解码器。</span>
	 * @throws IOException              <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
	 *                                  <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
	 * @throws IllegalArgumentException <span class="en-US">Numeric arguments or the size/EOS policy are invalid.</span>
	 *                                  <span class="zh-CN">数值参数或大小/EOS 策略非法。</span>
	 */
	public static LZMAInputStream raw(InputStream input, int properties, int dictionary,
	                                  long size, boolean endMarker, int maxDictionary) throws IOException {
		if (properties < 0 || properties > 255 || dictionary < 0) {
			throw new IllegalArgumentException("LZMA properties");
		}
		byte[] bytes = {
				(byte) properties, (byte) dictionary, (byte) (dictionary >>> 8),
				(byte) (dictionary >>> 16), (byte) (dictionary >>> 24)
		};
		return new LZMAInputStream(input, bytes, size, endMarker, maxDictionary, false);
	}

	/**
	 * <h4 class="en-US">Read a 13-byte LZMA-Alone header and create its decoder.</h4>
	 * <h4 class="zh-CN">读取 13 字节 LZMA-Alone 头并创建解码器。</h4>
	 *
	 * <p class="en-US">An all-ones size field means unknown size and requires EOS. Known-size streams may finish with or without EOS. Other unsigned sizes exceeding Long.MAX_VALUE are rejected.</p>
	 * <p class="zh-CN">全一的大小字段表示未知大小，必须使用 EOS。已知大小允许有或无 EOS。超过 Long.MAX_VALUE 的其他无符号大小会被拒绝。</p>
	 *
	 * @param input <span class="en-US">Stream positioned at the .lzma file header.</span>
	 *              <span class="zh-CN">位于 .lzma 文件头起点的流。</span>
	 * @return <span class="en-US">Decoder with a 64 MiB declared-dictionary ceiling.</span>
	 * <span class="zh-CN">声明字典上限为 64 MiB 的解码器。</span>
	 * @throws IOException <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
	 *                     <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
	 */
	public static LZMAInputStream alone(InputStream input) throws IOException {
		Objects.requireNonNull(input, "input");
		byte[] properties = new byte[5];
		for (int index = 0; index < 5; index++) {
			properties[index] = (byte) LZMACodec.readByte(input);
		}
		long size = 0;
		for (int index = 0; index < 8; index++) {
			size |= (long) LZMACodec.readByte(input) << (index * 8);
		}
		if (size < -1) {
			throw new IOException("Unsupported LZMA-Alone size");
		}
		return new LZMAInputStream(input, properties, size, true, DEFAULT_MAX_DICTIONARY, true);
	}

	/**
	 * <h4 class="en-US">Read known-size ZIP LZMA framing with optional EOS.</h4>
	 * <h4 class="zh-CN">读取已知大小且允许可选 EOS 的 ZIP LZMA 封装。</h4>
	 *
	 * <p class="en-US">Used for protected central directories, which do not have a separate entry EOS flag. Trailing data must still be rejected by the container boundary check.</p>
	 * <p class="zh-CN">用于没有独立条目 EOS 标志的受保护中央目录。尾随数据仍须由容器边界检查拒绝。</p>
	 *
	 * @param input <span class="en-US">Stream positioned at a ZIP LZMA properties header.</span>
	 *              <span class="zh-CN">位于 ZIP LZMA 属性头起点的流。</span>
	 * @param size  <span class="en-US">Known uncompressed size, zero or greater.</span>
	 *              <span class="zh-CN">已知未压缩大小，必须非负。</span>
	 * @return <span class="en-US">Decoder accepting a valid EOS or size-delimited finish.</span>
	 * <span class="zh-CN">接受合法 EOS 或大小结束条件的解码器。</span>
	 * @throws IOException              <span class="en-US">The delegate fails, encoded data is truncated or malformed.</span>
	 *                                  <span class="zh-CN">底层流失败，或编码数据截断、损坏。</span>
	 * @throws IllegalArgumentException <span class="en-US">The supplied size is negative.</span>
	 *                                  <span class="zh-CN">提供的大小为负数。</span>
	 */
	public static LZMAInputStream knownSize(InputStream input, long size) throws IOException {
		if (size < 0) {
			throw new IllegalArgumentException("Known LZMA size required");
		}
		return new LZMAInputStream(input, readZipProperties(input), size, true, DEFAULT_MAX_DICTIONARY, true);
	}

	/**
	 * <h4 class="en-US">Check stream usability.</h4>
	 * <h4 class="zh-CN">检查流是否可以继续读取。</h4>
	 *
	 * @throws IOException <span class="en-US">The stream is closed or an earlier decoding failure is retained.</span>
	 *                     <span class="zh-CN">流已关闭，或存在先前保存的解码异常。</span>
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) {
			throw new IOException("LZMA stream closed");
		}
		if (this.failure != null) {
			throw this.failure;
		}
	}

	/**
	 * <h4 class="en-US">Read one uncompressed byte.</h4>
	 * <h4 class="zh-CN">读取一个未压缩字节。</h4>
	 *
	 * @return <span class="en-US">Unsigned byte from 0 through 255, or -1 at a verified end.</span>
	 * <span class="zh-CN">范围为 0 至 255 的无符号字节；经过验证的结束处返回 -1。</span>
	 * @throws IOException <span class="en-US">The delegate fails or encoded data is invalid or truncated.</span>
	 *                     <span class="zh-CN">底层流操作失败，或编码数据非法、截断。</span>
	 */
	@Override
	public int read() throws IOException {
		byte[] one = new byte[1];
		return this.read(one, 0, 1) < 0 ? -1 : one[0] & 255;
	}

	/**
	 * <h4 class="en-US">Read uncompressed bytes and validate boundaries.</h4>
	 * <h4 class="zh-CN">读取未压缩字节并验证边界。</h4>
	 *
	 * <p class="en-US">Read through EOF to complete end validation. The first decoding IOException is retained and rethrown on later reads; close does not drain unread data.</p>
	 * <p class="zh-CN">读取至 EOF 才能完成全部结束校验；首次解码 IOException 会保存并在后续读取中再次抛出，close 不主动读完剩余数据。</p>
	 *
	 * @param bytes  <span class="en-US">Destination array.</span>
	 *               <span class="zh-CN">目标数组。</span>
	 * @param offset <span class="en-US">First destination index.</span>
	 *               <span class="zh-CN">目标起始下标。</span>
	 * @param length <span class="en-US">Maximum number of bytes to read.</span>
	 *               <span class="zh-CN">最多读取的字节数。</span>
	 * @return <span class="en-US">Number of bytes read; 0 for zero length; -1 at a verified end.</span>
	 * <span class="zh-CN">读取的字节数；零长度请求返回 0，经过验证的结束处返回 -1。</span>
	 * @throws IOException               <span class="en-US">The delegate fails or encoded data is invalid or truncated.</span>
	 *                                   <span class="zh-CN">底层流操作失败，或编码数据非法、截断。</span>
	 * @throws IndexOutOfBoundsException <span class="en-US">The array range is invalid.</span>
	 *                                   <span class="zh-CN">数组范围非法。</span>
	 */
	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		this.ensureOpen();
		if (length == 0) {
			return 0;
		}
		if (this.finished) {
			return -1;
		}
		try {
			int count = 0;
			while (count < length) {
				if (this.expectedSize >= 0 && this.produced == this.expectedSize
						&& (!this.endMarker || (this.optionalEndMarker && this.range.finished()))) {
					if (this.decoder.hasPending() || !this.range.finished()) {
						throw new IOException("Invalid size-delimited LZMA end");
					}
					this.finished = true;
					break;
				}
				int value = this.decoder.readByte(this.range, this.endMarker);
				if (value == -1) {
					if (this.expectedSize >= 0 && this.produced != this.expectedSize) {
						throw new IOException("Invalid LZMA EOS/size");
					}
					this.finished = true;
					break;
				}
				if (this.expectedSize >= 0 && this.produced >= this.expectedSize) {
					throw new IOException("LZMA output exceeds declared size");
				}
				bytes[offset + count++] = (byte) value;
				this.produced++;
			}
			return count == 0 ? -1 : count;
		} catch (IOException error) {
			this.failure = error;
			throw error;
		}
	}

	/**
	 * <h4 class="en-US">Release decoder memory and close the source once.</h4>
	 * <h4 class="zh-CN">释放解码内存并只关闭一次输入源。</h4>
	 *
	 * <p class="en-US">Idempotent after close begins, including when delegate close fails. Unread data is not consumed or validated.</p>
	 * <p class="zh-CN">开始关闭后重复调用无操作，即使底层关闭失败也是如此；不消费或验证未读取的数据。</p>
	 *
	 * @throws IOException <span class="en-US">Closing the delegate fails.</span>
	 *                     <span class="zh-CN">关闭底层输入流失败。</span>
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		this.decoder.release();
		this.input.close();
	}
}
