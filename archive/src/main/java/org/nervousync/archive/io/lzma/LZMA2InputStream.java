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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * <h2 class="en-US">Raw LZMA2 chunk decoder with shared LZMA1 symbol logic.</h2>
 * <h2 class="zh-CN">复用 LZMA1 符号逻辑的原始 LZMA2 分块解码器。</h2>
 * <p class="en-US">Dictionary size is supplied externally; no XZ header is read.
 * Compressed chunks may retain state/history or reset either as instructed.
 * Preset dictionaries are not exposed by this API. Not thread safe.</p>
 * <p class="zh-CN">字典大小由外部提供，不读取 XZ 头。压缩块按照控制字节保留状态及历史，
 * 或执行相应重置。本接口不提供预置字典。非线程安全。</p>
 * <p class="en-US">Owns the delegate after successful construction. Read through EOF for
 * complete boundary validation. close does not drain. Enforce total-output/ratio limits
 * in the caller and use a container checksum when integrity is required.</p>
 * <p class="zh-CN">构造成功后负责关闭底层流。完整边界验证需要读取至 EOF，close 不主动读完数据。
 * 总输出量及压缩比由调用方限制，需要完整性保护时应结合容器校验。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class LZMA2InputStream extends InputStream {
	/**
	 * <span class="en-US">Original raw LZMA2 source.</span>
	 * <span class="zh-CN">原始 LZMA2 数据源。</span>
	 */
	private final InputStream input;

	/**
	 * <span class="en-US">Shared dictionary and adaptive decoding state.</span>
	 * <span class="zh-CN">共用字典及自适应解码状态。</span>
	 */
	private final LZMADecoder decoder;

	/**
	 * <span class="en-US">Current compressed payload, bounded to at most 64 KiB.</span>
	 * <span class="zh-CN">当前压缩数据，最多 64 KiB。</span>
	 */
	private ByteArrayInputStream compressedInput;

	/**
	 * <span class="en-US">Fresh range state for the current compressed chunk.</span>
	 * <span class="zh-CN">当前压缩块重新初始化的区间状态。</span>
	 */
	private LZMACodec.Decoder range;

	/**
	 * <span class="en-US">Uncompressed bytes remaining in this chunk.</span>
	 * <span class="zh-CN">当前块剩余的未压缩字节数。</span>
	 */
	private int remaining;

	/**
	 * <span class="en-US">Current chunk is encoded rather than stored.</span>
	 * <span class="zh-CN">当前块是否为压缩块。</span>
	 */
	private boolean compressed;

	/**
	 * <span class="en-US">First nonempty chunk must establish the dictionary.</span>
	 * <span class="zh-CN">首个数据块必须建立字典。</span>
	 */
	private boolean needDictionaryReset = true;

	/**
	 * <span class="en-US">Initial compressed chunk and dictionary resets require properties.</span>
	 * <span class="zh-CN">首个压缩块及字典重置后需要属性。</span>
	 */
	private boolean needProperties = true;

	/**
	 * <span class="en-US">A zero end control has been consumed.</span>
	 * <span class="zh-CN">已消费零值结束控制字节。</span>
	 */
	private boolean finished;

	/**
	 * <span class="en-US">Closure has begun.</span>
	 * <span class="zh-CN">已开始关闭。</span>
	 */
	private boolean closed;

	/**
	 * <span class="en-US">First I/O or format failure, retained for future reads.</span>
	 * <span class="zh-CN">保存首次 I/O 或格式错误。</span>
	 */
	private IOException failure;

	/**
	 * <h4 class="en-US">Create a raw decoder with the default 64 MiB dictionary ceiling.</h4>
	 * <h4 class="zh-CN">使用默认 64 MiB 字典上限创建原始解码器。</h4>
	 *
	 * <p class="en-US">Does not consume input during construction. The stream owns the source after successful construction; the caller retains ownership if construction fails.</p>
	 * <p class="zh-CN">构造时不读取输入；构造成功后负责关闭输入源，构造失败时仍由调用方管理。</p>
	 *
	 * @param input          <span class="en-US">Delegate input stream.</span>
	 *                       <span class="zh-CN">底层输入流。</span>
	 * @param dictionarySize <span class="en-US">Externally supplied dictionary size, at least 4096 bytes.</span>
	 *                       <span class="zh-CN">由外部提供的字典大小，至少为 4096 字节。</span>
	 * @throws IOException              <span class="en-US">The declared dictionary exceeds the default ceiling.</span>
	 *                                  <span class="zh-CN">声明字典超过默认上限。</span>
	 * @throws IllegalArgumentException <span class="en-US">The dictionary is smaller than 4096 bytes.</span>
	 *                                  <span class="zh-CN">字典小于 4096 字节。</span>
	 * @throws NullPointerException     <span class="en-US">The input is null.</span>
	 *                                  <span class="zh-CN">输入流为 null。</span>
	 */
	public LZMA2InputStream(InputStream input, int dictionarySize) throws IOException {
		this(input, dictionarySize, LZMAInputStream.DEFAULT_MAX_DICTIONARY);
	}

	/**
	 * <h4 class="en-US">Create a raw decoder with an explicit dictionary ceiling.</h4>
	 * <h4 class="zh-CN">指定字典上限创建原始解码器。</h4>
	 *
	 * <p class="en-US">Allocate the validated dictionary, but defer probability-model creation until the first compressed chunk supplies properties. Raw LZMA2 stores neither the dictionary size nor an XZ header.</p>
	 * <p class="zh-CN">分配经过验证的字典，概率模型延后至首个压缩块提供属性时创建；原始 LZMA2 不保存字典大小，也不包含 XZ 头。</p>
	 *
	 * @param input          <span class="en-US">Delegate input stream.</span>
	 *                       <span class="zh-CN">底层输入流。</span>
	 * @param dictionarySize <span class="en-US">External dictionary size, at least 4096 bytes.</span>
	 *                       <span class="zh-CN">外部字典大小，至少为 4096 字节。</span>
	 * @param maxDictionary  <span class="en-US">Maximum dictionary allocation, at least 4096 bytes.</span>
	 *                       <span class="zh-CN">字典分配上限，至少为 4096 字节。</span>
	 * @throws IOException              <span class="en-US">The dictionary exceeds the configured ceiling.</span>
	 *                                  <span class="zh-CN">字典超过配置的上限。</span>
	 * @throws IllegalArgumentException <span class="en-US">Either size is smaller than 4096 bytes.</span>
	 *                                  <span class="zh-CN">任一大小小于 4096 字节。</span>
	 * @throws NullPointerException     <span class="en-US">The input is null.</span>
	 *                                  <span class="zh-CN">输入流为 null。</span>
	 */
	public LZMA2InputStream(InputStream input, int dictionarySize, int maxDictionary) throws IOException {
		this.input = Objects.requireNonNull(input, "input");
		if (dictionarySize < 4096 || maxDictionary < 4096) {
			throw new IllegalArgumentException("LZMA2 dictionary must be at least 4096");
		}
		if (dictionarySize > maxDictionary) {
			throw new IOException("LZMA2 dictionary exceeds configured bound");
		}
		this.decoder = new LZMADecoder(-1, dictionarySize, dictionarySize);
	}

	/**
	 * <h4 class="en-US">Read a two-byte big-endian size-minus-one.</h4>
	 * <h4 class="zh-CN">读取两字节大端的大小减一字段。</h4>
	 *
	 * @return <span class="en-US">Decoded size from 1 through 65536.</span>
	 * <span class="zh-CN">范围为 1 至 65536 的实际大小。</span>
	 * @throws IOException <span class="en-US">The delegate fails or encoded data is invalid or truncated.</span>
	 *                     <span class="zh-CN">底层流操作失败，或编码数据非法、截断。</span>
	 */
	private int readSize() throws IOException {
		return ((LZMACodec.readByte(this.input) << 8) | LZMACodec.readByte(this.input)) + 1;
	}

	/**
	 * <h4 class="en-US">Read and apply the next chunk control.</h4>
	 * <h4 class="zh-CN">读取并应用下一个分块控制。</h4>
	 *
	 * <p class="en-US">Requires the previous chunk to be fully consumed and verified. Controls select dictionary, model and property resets independently. Each compressed payload gets a fresh range decoder and a bounded input buffer.</p>
	 * <p class="zh-CN">要求前一块已完整消费并验证；控制字节分别决定字典、模型及属性重置，每个压缩数据块使用新的区间解码器和有界输入缓冲。</p>
	 *
	 * @return <span class="en-US">True for a data chunk; false after consuming the zero end control.</span>
	 * <span class="zh-CN">数据块返回 true；消费零结束控制后返回 false。</span>
	 * @throws IOException <span class="en-US">The delegate fails or encoded data is invalid or truncated.</span>
	 *                     <span class="zh-CN">底层流操作失败，或编码数据非法、截断。</span>
	 */
	private boolean nextChunk() throws IOException {
		int control = LZMACodec.readByte(this.input);
		if (control == 0) {
			this.finished = true;
			return false;
		}
		if (control == 1 || control >= 0xE0) {
			this.decoder.resetDictionary();
			this.needDictionaryReset = false;
			this.needProperties = true;
		} else if (this.needDictionaryReset) {
			throw new IOException("LZMA2 initial dictionary reset is missing");
		}
		if (control < 0x80) {
			if (control != 1 && control != 2) {
				throw new IOException("Invalid LZMA2 control byte");
			}
			this.compressed = false;
			this.remaining = this.readSize();
			return true;
		}
		this.compressed = true;
		this.remaining = ((control & 31) << 16) + this.readSize();
		int compressedSize = this.readSize();
		if (control >= 0xC0) {
			int properties = LZMACodec.readByte(this.input);
			if (properties >= 225 || properties % 9 + (properties / 9) % 5 > 4) {
				throw new IOException("Invalid LZMA2 properties: lc + lp must not exceed 4");
			}
			this.decoder.configure(properties);
			this.needProperties = false;
		} else if (this.needProperties) {
			throw new IOException("LZMA2 properties are missing");
		} else if (control >= 0xA0) {
			this.decoder.resetState();
		}
		if (compressedSize < 5) {
			throw new IOException("LZMA2 range chunk is shorter than initialization");
		}
		byte[] bytes = new byte[compressedSize];
		int offset = 0;
		while (offset < bytes.length) {
			int count = this.input.read(bytes, offset, bytes.length - offset);
			if (count < 0) {
				throw new java.io.EOFException("Truncated LZMA2 compressed chunk");
			}
			if (count == 0) {
				bytes[offset++] = (byte) LZMACodec.readByte(this.input);
			} else {
				offset += count;
			}
		}
		this.compressedInput = new ByteArrayInputStream(bytes);
		this.range = new LZMACodec.Decoder(this.compressedInput);
		return true;
	}

	/**
	 * <h4 class="en-US">Validate exact compressed-chunk termination.</h4>
	 * <h4 class="zh-CN">验证压缩块精确结束。</h4>
	 *
	 * <p class="en-US">Requires zero remaining output. Reject pending matches, a nonzero range code or unread packed bytes before releasing per-chunk state.</p>
	 * <p class="zh-CN">要求剩余输出为零；拒绝残留匹配、非零区间码或未消费的压缩字节，再释放当前块状态。</p>
	 *
	 * @throws IOException <span class="en-US">The delegate fails or encoded data is invalid or truncated.</span>
	 *                     <span class="zh-CN">底层流操作失败，或编码数据非法、截断。</span>
	 */
	private void verifyChunkEnd() throws IOException {
		if (this.decoder.hasPending() || !this.range.finished() || this.compressedInput.available() != 0) {
			throw new IOException("Invalid LZMA2 chunk boundary");
		}
		this.range = null;
		this.compressedInput = null;
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
			throw new IOException("LZMA2 input closed");
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
				if (this.remaining == 0 && !this.nextChunk()) {
					break;
				}
				int take = Math.min(length - count, this.remaining);
				if (this.compressed) {
					for (int index = 0; index < take; index++) {
						bytes[offset + count + index] = (byte) this.decoder.readByte(this.range, false);
					}
				} else {
					take = this.input.read(bytes, offset + count, take);
					if (take < 0) {
						throw new java.io.EOFException("Truncated LZMA2 uncompressed chunk");
					}
					if (take == 0) {
						bytes[offset + count] = (byte) LZMACodec.readByte(this.input);
						take = 1;
					}
					this.decoder.appendUncompressed(bytes, offset + count, take);
				}
				this.remaining -= take;
				count += take;
				if (this.compressed && this.remaining == 0) {
					this.verifyChunkEnd();
				}
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
		this.range = null;
		this.compressedInput = null;
		this.decoder.release();
		this.input.close();
	}
}
