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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * <h2 class="en-US">Streaming BZip2 decompression input stream</h2>
 * <h2 class="zh-CN">BZip2 流式解压输入流</h2>
 *
 * <p class="en-US">Independent of ZIP and TAR containers. Decodes complete BZh streams,
 * including legacy randomized blocks and optional concatenated members.</p>
 * <p class="zh-CN">独立于 ZIP、TAR 容器，解码包含 BZh 流头的完整 BZip2 流，
 * 支持旧版随机化块，以及可选的多个串接成员。</p>
 *
 * <p class="en-US">A block is first Huffman-decoded, RLE2-expanded and inverse-MTF-decoded
 * into the BWT last column. Inverse BWT, optional derandomization and RLE1 expansion
 * are then performed as the caller reads the uncompressed bytes.</p>
 * <p class="zh-CN">首先按块执行 Huffman 解码、RLE2 展开和逆 MTF，恢复 BWT 最后一列；
 * 随后在调用方读取时，按需执行逆 BWT、可选的去随机化及 RLE1 展开。</p>
 *
 * <p class="en-US">The header limits the BWT block size, not the total decompressed output.
 * RLE1 expansion is streamed rather than buffered as a complete expanded block.
 * The caller is responsible for limits on total output and compression ratio.</p>
 * <p class="zh-CN">流头限制的是 BWT 编码块大小，而非最终解压输出总量。RLE1 结果按需输出，
 * 不会把整个展开后的块另存为大数组。总输出量和压缩比限制由调用方负责。</p>
 *
 * <p class="en-US">Block CRCs are checked when each block has been consumed. The combined
 * CRC is checked at the member end marker. Read through EOF to validate the complete
 * stream; close() releases resources without draining or validating unread data.</p>
 * <p class="zh-CN">每个块消费完毕时校验块 CRC，读取成员结束标记时校验组合 CRC。
 * 完整校验需要读取至 EOF；close() 只释放资源，不主动读取或校验剩余数据。</p>
 *
 * <p class="en-US">Not thread safe. Single-member mode does not intentionally read beyond
 * that member. After successful construction this stream owns the delegate; if
 * construction fails, closing the delegate remains the caller's responsibility.</p>
 * <p class="zh-CN">此类不是线程安全的。单成员模式不会主动读取该成员之后的字节。
 * 构造成功后本流负责关闭底层流；构造失败时，仍由调用方负责关闭底层流。</p>
 */
public final class BZip2InputStream extends InputStream {

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
	 * <span class="en-US">Maximum selector count accepted by this implementation</span>
	 * <span class="zh-CN">当前实现接受的 Huffman 表选择器数量上限</span>
	 */
	private static final int MAX_SELECTORS = 18002;
	/**
	 * <span class="en-US">Fixed 512-entry format table for decoding legacy randomized blocks</span>
	 * <span class="zh-CN">用于解码旧版随机化块的固定 512 项格式表</span>
	 *
	 * <p class="en-US">The table is format data from Apache Commons Compress 1.28.0.
	 * The codec does not delegate decompression to Commons Compress.</p>
	 * <p class="zh-CN">该表为取自 Apache Commons Compress 1.28.0 的固定格式数据，
	 * 本解码器不会委托 Commons Compress 执行解压。</p>
	 */
	private static final int[] RANDOM_NUMBERS = {
			0x026B, 0x02D0, 0x007F, 0x01E1, 0x03A3, 0x0330, 0x032D, 0x00E9, 0x0236, 0x00F7, 0x03D9, 0x02D4, 0x00CD, 0x01C6, 0x035F, 0x01EB,
			0x02E5, 0x00F2, 0x03B5, 0x00D6, 0x02DD, 0x035B, 0x014F, 0x02C4, 0x026D, 0x023E, 0x0049, 0x028E, 0x02DA, 0x01D8, 0x01A3, 0x01B4,
			0x0116, 0x01F0, 0x0363, 0x00D2, 0x018F, 0x02A8, 0x01E0, 0x0033, 0x036E, 0x01D1, 0x032B, 0x00A9, 0x0365, 0x02A3, 0x0263, 0x02B9,
			0x0363, 0x0231, 0x035E, 0x02AF, 0x01FB, 0x011B, 0x01E2, 0x0081, 0x0327, 0x024F, 0x02DD, 0x026F, 0x0096, 0x00EE, 0x003B, 0x017B,
			0x02AC, 0x036D, 0x0271, 0x00A9, 0x0283, 0x0069, 0x00AA, 0x025F, 0x0208, 0x03A4, 0x02D7, 0x01DC, 0x02B5, 0x01A9, 0x00AE, 0x0287,
			0x0049, 0x007A, 0x014F, 0x0212, 0x01BA, 0x0355, 0x02B7, 0x00F9, 0x01BD, 0x0203, 0x038D, 0x0221, 0x02BF, 0x0397, 0x036A, 0x01DA,
			0x0372, 0x01F4, 0x0252, 0x0264, 0x0281, 0x0321, 0x00DC, 0x00A2, 0x0333, 0x03D8, 0x024D, 0x0201, 0x01EF, 0x031F, 0x00A1, 0x025C,
			0x03BE, 0x0215, 0x00DD, 0x0190, 0x0182, 0x0363, 0x0258, 0x030E, 0x017E, 0x0254, 0x019E, 0x00AB, 0x0204, 0x0177, 0x02AA, 0x01E5,
			0x038F, 0x0114, 0x0062, 0x0229, 0x00A3, 0x0162, 0x029A, 0x03A5, 0x01A8, 0x0155, 0x0215, 0x0366, 0x00E3, 0x02DA, 0x01DB, 0x00BA,
			0x0107, 0x0287, 0x0219, 0x02AE, 0x0258, 0x00E0, 0x01D5, 0x0044, 0x0302, 0x0397, 0x00BE, 0x0175, 0x0126, 0x0336, 0x0328, 0x00CE,
			0x00B8, 0x03AF, 0x031B, 0x0180, 0x017F, 0x01CD, 0x0194, 0x02F6, 0x0347, 0x0377, 0x02CB, 0x0043, 0x026A, 0x0114, 0x00CC, 0x0396,
			0x0369, 0x0309, 0x025C, 0x0230, 0x03B7, 0x00A0, 0x0242, 0x02D2, 0x004F, 0x0324, 0x0060, 0x0199, 0x02C9, 0x03AC, 0x028C, 0x03A6,
			0x03CA, 0x01BF, 0x013E, 0x0161, 0x035B, 0x02A0, 0x0070, 0x0311, 0x0285, 0x035F, 0x0323, 0x015E, 0x008B, 0x005D, 0x0162, 0x0063,
			0x0334, 0x038C, 0x0261, 0x0304, 0x009A, 0x0112, 0x0244, 0x00B8, 0x004F, 0x0272, 0x0276, 0x02E6, 0x028D, 0x011A, 0x02FA, 0x026F,
			0x02A8, 0x0051, 0x039F, 0x0272, 0x0315, 0x007D, 0x019B, 0x0209, 0x03AA, 0x012C, 0x0335, 0x004E, 0x0157, 0x00AF, 0x0080, 0x00FA,
			0x00AA, 0x0306, 0x03CC, 0x0113, 0x03E7, 0x027F, 0x01EF, 0x004E, 0x0160, 0x007E, 0x0359, 0x03BC, 0x0166, 0x026B, 0x0244, 0x007C,
			0x02E1, 0x0252, 0x02BD, 0x0264, 0x029D, 0x0070, 0x0086, 0x02B6, 0x016B, 0x03E0, 0x0329, 0x02E7, 0x00A8, 0x03CE, 0x03B0, 0x0177,
			0x02EC, 0x0034, 0x0258, 0x02EB, 0x0282, 0x00B6, 0x035E, 0x0051, 0x0158, 0x0325, 0x03DC, 0x02E3, 0x01FF, 0x028F, 0x032E, 0x014E,
			0x00F9, 0x0203, 0x0381, 0x03BB, 0x0298, 0x03D5, 0x0289, 0x0071, 0x03CE, 0x01CB, 0x037D, 0x00E4, 0x01B1, 0x0345, 0x0229, 0x010C,
			0x039E, 0x00F0, 0x0066, 0x028E, 0x01CB, 0x0033, 0x02AE, 0x02F2, 0x0326, 0x02F8, 0x01ED, 0x0193, 0x019F, 0x018A, 0x02AF, 0x02BC,
			0x03B2, 0x029E, 0x0290, 0x0262, 0x02E2, 0x0188, 0x02F8, 0x031F, 0x0377, 0x028D, 0x03D2, 0x0141, 0x0240, 0x0269, 0x0272, 0x01F6,
			0x037E, 0x02A7, 0x00F3, 0x01B8, 0x02A8, 0x036F, 0x00C2, 0x023C, 0x0280, 0x02D4, 0x039E, 0x0038, 0x00CC, 0x02BC, 0x02C3, 0x0097,
			0x01C9, 0x01C1, 0x031D, 0x00C3, 0x0317, 0x022E, 0x03B1, 0x02A7, 0x0129, 0x003B, 0x0057, 0x0338, 0x02C9, 0x0297, 0x019C, 0x02B5,
			0x0156, 0x025E, 0x0086, 0x006C, 0x023B, 0x016C, 0x0277, 0x00D4, 0x00AE, 0x0283, 0x0130, 0x0149, 0x0157, 0x0061, 0x01AE, 0x02EF,
			0x01F1, 0x013A, 0x03D7, 0x0176, 0x0336, 0x03A0, 0x008C, 0x00CE, 0x0049, 0x0107, 0x03D4, 0x02E0, 0x036C, 0x01DE, 0x01AE, 0x0131,
			0x00AA, 0x0202, 0x016C, 0x02B4, 0x033D, 0x0052, 0x0357, 0x03B9, 0x02A4, 0x00F6, 0x0171, 0x03CA, 0x0126, 0x02EE, 0x0327, 0x033B,
			0x0096, 0x0316, 0x0120, 0x039B, 0x0324, 0x017A, 0x00D7, 0x033C, 0x0250, 0x0119, 0x0235, 0x022B, 0x02C6, 0x0052, 0x0380, 0x033F,
			0x0223, 0x0105, 0x020C, 0x01CE, 0x0125, 0x01D1, 0x01F6, 0x0038, 0x0295, 0x0335, 0x03D0, 0x03DF, 0x0292, 0x0365, 0x0389, 0x02F6,
			0x02E9, 0x00C1, 0x0300, 0x0226, 0x0260, 0x03A5, 0x017A, 0x011E, 0x00D7, 0x03D3, 0x0318, 0x03C1, 0x003D, 0x02B0, 0x0319, 0x0284,
			0x03DA, 0x0193, 0x006A, 0x016E, 0x0389, 0x0284, 0x0174, 0x0237, 0x01D2, 0x01B2, 0x0285, 0x00D2, 0x0185, 0x0226, 0x0397, 0x0087,
			0x030C, 0x0305, 0x027B, 0x0185, 0x02C3, 0x0064, 0x0272, 0x03BE, 0x00A5, 0x01F8, 0x0398, 0x00B0, 0x00C1, 0x02C9, 0x0359, 0x0109,
			0x00CB, 0x0032, 0x029C, 0x006C, 0x0285, 0x03DE, 0x0272, 0x00C5, 0x01FE, 0x0165, 0x0166, 0x0352, 0x035A, 0x016C, 0x03A8, 0x027E
	};

	/**
	 * <span class="en-US">Delegate containing the compressed bytes</span>
	 * <span class="zh-CN">提供压缩字节的底层输入流</span>
	 */
	private final InputStream input;
	/**
	 * <span class="en-US">Whether to decode consecutive members through source EOF</span>
	 * <span class="zh-CN">是否连续解码多个成员直至底层流 EOF</span>
	 */
	private final boolean concatenated;
	/**
	 * <span class="en-US">CRC-32/BZIP2 calculator for the current block's uncompressed bytes</span>
	 * <span class="zh-CN">计算当前块原始未压缩数据的 CRC-32/BZIP2</span>
	 */
	private final CRCAdaptor crc;
	/**
	 * <span class="en-US">Reusable one-byte buffer for read()</span>
	 * <span class="zh-CN">供 read() 复用的单字节缓冲区</span>
	 */
	private final byte[] singleByte = new byte[1];
	/**
	 * <span class="en-US">BWT last-column bytes after Huffman, RLE2 and inverse MTF decoding</span>
	 * <span class="zh-CN">经过 Huffman、RLE2 和逆 MTF 解码后得到的 BWT 最后一列</span>
	 */
	private byte[] lastColumn;
	/**
	 * <span class="en-US">Successor indexes used to traverse the inverse BWT</span>
	 * <span class="zh-CN">用于遍历 BWT 逆变换的后继索引数组</span>
	 */
	private int[] inverse;
	/**
	 * <span class="en-US">Current member's BWT block-size limit, from 100000 to 900000 bytes</span>
	 * <span class="zh-CN">当前成员的 BWT 块大小上限，范围为 100000 至 900000 字节</span>
	 */
	private int blockCapacity;
	/**
	 * <span class="en-US">Block CRC read from the compressed block header</span>
	 * <span class="zh-CN">从压缩块头读取的预期块 CRC</span>
	 */
	private int expectedBlockCrc;
	/**
	 * <span class="en-US">Combined CRC of validated blocks in the current member</span>
	 * <span class="zh-CN">当前成员中已通过校验的各块所组成的组合 CRC</span>
	 */
	private int combinedCrc;
	/**
	 * <span class="en-US">Pending compressed bits, stored in the low bitCount bits</span>
	 * <span class="zh-CN">尚未消费的压缩位，保存在低 bitCount 位中</span>
	 */
	private long bitBuffer;
	/**
	 * <span class="en-US">Number of pending valid bits in buffer</span>
	 * <span class="zh-CN">缓冲区中尚未消费的有效位数</span>
	 */
	private int bitCount;
	/**
	 * <span class="en-US">Current last-column index during inverse BWT traversal</span>
	 * <span class="zh-CN">执行逆 BWT 时当前访问的最后一列下标</span>
	 */
	private int bwtPosition;
	/**
	 * <span class="en-US">Number of inverse-BWT bytes still to consume, including RLE1 count bytes</span>
	 * <span class="zh-CN">逆 BWT 阶段剩余的编码字节数，包含 RLE1 的重复计数字节</span>
	 */
	private int rleRemaining;
	/**
	 * <span class="en-US">Most recent RLE1 literal byte; -1 means no literal has been seen</span>
	 * <span class="zh-CN">最近的 RLE1 字面量字节，-1 表示尚未读取字面量</span>
	 */
	private int lastByte = -1;
	/**
	 * <span class="en-US">Consecutive equal literals in the current RLE1 literal segment</span>
	 * <span class="zh-CN">当前 RLE1 字面量段内连续相同字节的计数</span>
	 */
	private int sameBytes;
	/**
	 * <span class="en-US">Additional copies of lastByte still to emit from an RLE1 count</span>
	 * <span class="zh-CN">根据 RLE1 计数仍需额外输出的 lastByte 副本数量</span>
	 */
	private int repeats;
	/**
	 * <span class="en-US">Whether the current block uses the legacy randomization transform</span>
	 * <span class="zh-CN">当前块是否使用旧版随机化变换</span>
	 */
	private boolean randomized;
	/**
	 * <span class="en-US">Next entry in the 512-entry legacy randomization table</span>
	 * <span class="zh-CN">下一次使用的旧版随机化表下标，范围为 0 至 511</span>
	 */
	private int randomIndex;
	/**
	 * <span class="en-US">Countdown until the next legacy randomization event</span>
	 * <span class="zh-CN">旧版去随机化过程使用的字节倒计数</span>
	 */
	private int randomRemaining;
	/**
	 * <span class="en-US">Whether a decoded block is available and its output is not yet fully consumed</span>
	 * <span class="zh-CN">是否存在尚未完全消费输出的已解码块</span>
	 */
	private boolean blockActive;
	/**
	 * <span class="en-US">Whether a valid end has been reached for the requested decoding mode</span>
	 * <span class="zh-CN">是否已经到达当前解码模式要求的有效结束位置</span>
	 */
	private boolean finished;
	/**
	 * <span class="en-US">Whether this stream has been closed</span>
	 * <span class="zh-CN">当前输入流是否已经关闭</span>
	 */
	private boolean closed;
	/**
	 * <span class="en-US">Previously reported decoding failure; subsequent reads must fail</span>
	 * <span class="zh-CN">已经报告的解码异常，记录后不再允许继续读取</span>
	 */
	private IOException failure;

	/**
	 * <h3 class="en-US">Construct a decoder for one BZip2 member</h3>
	 * <h3 class="zh-CN">构造只读取一个 BZip2 成员的解压流</h3>
	 *
	 * <p class="en-US">Equivalent to BZip2InputStream(input, false). Only the initial BZh header is read during construction.</p>
	 * <p class="zh-CN">等价于 BZip2InputStream(input, false)。构造时只读取并校验首个 BZh 流头。</p>
	 *
	 * @param input <span class="en-US">Delegate containing the compressed data</span>
	 *              <span class="zh-CN">提供压缩数据的底层输入流</span>
	 * @throws NullPointerException <span class="en-US">The delegate is null</span>
	 *                              <span class="zh-CN">底层输入流为空引用</span>
	 * @throws IOException          <span class="en-US">The initial header is invalid, truncated, or cannot be read</span>
	 *                              <span class="zh-CN">初始流头非法、截断或底层读取失败</span>
	 */
	public BZip2InputStream(final InputStream input) throws IOException {
		this(input, false);
	}

	/**
	 * <h3 class="en-US">Construct a BZip2 decoder with an explicit member policy</h3>
	 * <h3 class="zh-CN">构造指定成员读取策略的 BZip2 解压流</h3>
	 *
	 * <p class="en-US">When concatenated is true, the next member is expected after each validated member end. Only source EOF ends decoding; trailing non-BZip2 bytes are invalid.</p>
	 * <p class="zh-CN">concatenated 为 true 时，每个成员校验结束后继续查找下一个成员，只有底层 EOF 才结束；尾随非 BZip2 数据会被视为非法。</p>
	 *
	 * @param input        <span class="en-US">Delegate containing the compressed data</span>
	 *                     <span class="zh-CN">提供压缩数据的底层输入流</span>
	 * @param concatenated <span class="en-US">Whether to decode consecutive members through source EOF</span>
	 *                     <span class="zh-CN">是否连续读取多个成员直至底层 EOF</span>
	 * @throws NullPointerException <span class="en-US">The delegate is null</span>
	 *                              <span class="zh-CN">底层输入流为空引用</span>
	 * @throws IOException          <span class="en-US">The initial header is invalid, truncated, or cannot be read</span>
	 *                              <span class="zh-CN">初始流头非法、截断或底层读取失败</span>
	 */
	public BZip2InputStream(final InputStream input, final boolean concatenated) throws IOException {
		this.input = Objects.requireNonNull(input, "input");
		this.concatenated = concatenated;
		this.crc = DigestUtils.CRCAdaptor("CRC-32/BZIP2");
		this.readHeader(this.readSourceByte());
	}

	/**
	 * <h3 class="en-US">Read and validate a member header</h3>
	 * <h3 class="zh-CN">读取并校验一个成员的流头</h3>
	 *
	 * <p class="en-US">The first byte has already been consumed. The fourth header byte selects the BWT block-size limit; the combined CRC restarts for each member.</p>
	 * <p class="zh-CN">第一个字节已由调用方读取。流头第四个字节指定 BWT 块大小上限；每个成员单独重新累计组合 CRC。</p>
	 *
	 * @param first <span class="en-US">Previously read first header byte</span>
	 *              <span class="zh-CN">已经读取的首个流头字节</span>
	 * @throws IOException <span class="en-US">The header is invalid, truncated, or the delegate fails</span>
	 *                     <span class="zh-CN">流头非法、截断或底层读取失败</span>
	 */
	private void readHeader(final int first) throws IOException {
		if (first != 'B' || this.readSourceByte() != 'Z' || this.readSourceByte() != 'h') {
			throw new IOException("Invalid BZip2 stream header");
		}
		// 第四个字节为字符 1～9，指定 RLE1 编码后、BWT 处理的块大小上限。
		int size = this.readSourceByte() - '0';
		if (size < 1 || size > 9) throw new IOException("Invalid BZip2 block size");
		this.blockCapacity = size * 100000;
		this.combinedCrc = 0;
	}

	/**
	 * <h3 class="en-US">Read one uncompressed byte</h3>
	 * <h3 class="zh-CN">读取一个解压后的字节</h3>
	 *
	 * <p class="en-US">Delegates to the bulk-read method so that RLE state and CRC checks follow the same path.</p>
	 * <p class="zh-CN">复用批量读取方法，使 RLE 状态推进和 CRC 校验走同一条处理路径。</p>
	 *
	 * @return <span class="en-US">Unsigned byte value from 0 to 255, or -1 at a valid end</span>
	 * <span class="zh-CN">范围为 0 至 255 的无符号字节值，到达有效结束位置时返回 -1</span>
	 * @throws IOException <span class="en-US">The stream is closed or failed, the data is invalid or truncated, CRC verification fails, or the delegate fails</span>
	 *                     <span class="zh-CN">流已关闭或失败、数据非法或截断、CRC 校验失败，或底层读取失败</span>
	 */
	@Override
	public int read() throws IOException {
		return this.read(this.singleByte, 0, 1) < 0 ? -1 : this.singleByte[0] & 0xFF;
	}

	/**
	 * <h3 class="en-US">Read uncompressed bytes and advance the current block</h3>
	 * <h3 class="zh-CN">读取解压后的字节并推进当前块状态</h3>
	 *
	 * <p class="en-US">May return fewer bytes than requested at a block boundary. A positive-length request returns data or -1; a zero-length request returns zero on an open, non-failed stream.</p>
	 * <p class="zh-CN">到达块边界时可以少于请求长度。非零长度请求返回数据或 -1；流处于可读状态时，零长度请求返回零。</p>
	 *
	 * <p class="en-US">CRC accumulation covers emitted original bytes. The block CRC is checked before returning the final bytes of that block; the combined CRC is checked when a later read consumes the end marker.</p>
	 * <p class="zh-CN">CRC 对本次输出的原始数据累计。本块最后一批数据返回前校验块 CRC；后续读取到流尾标记时再校验组合 CRC。</p>
	 *
	 * @param bytes  <span class="en-US">Destination buffer</span>
	 *               <span class="zh-CN">接收解压数据的缓冲区</span>
	 * @param offset <span class="en-US">Destination start offset</span>
	 *               <span class="zh-CN">接收数据的起始下标</span>
	 * @param length <span class="en-US">Maximum number of bytes to read</span>
	 *               <span class="zh-CN">本次最多读取的字节数</span>
	 * @return <span class="en-US">Number of bytes read, zero for a zero-length request, or -1 at a valid end</span>
	 * <span class="zh-CN">实际读取字节数；零长度请求返回零，到达有效结束位置返回 -1</span>
	 * @throws NullPointerException      <span class="en-US">The destination buffer is null</span>
	 *                                   <span class="zh-CN">接收缓冲区为空引用</span>
	 * @throws IndexOutOfBoundsException <span class="en-US">The destination range is invalid</span>
	 *                                   <span class="zh-CN">接收缓冲区范围非法</span>
	 * @throws IOException               <span class="en-US">The stream is closed or failed, decoding or CRC verification fails, or the delegate fails</span>
	 *                                   <span class="zh-CN">流已关闭或失败、解码或 CRC 校验失败，或底层读取失败</span>
	 */
	@Override
	public int read(final byte[] bytes, final int offset, final int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, bytes.length);
		this.ensureOpen();
		if (length == 0) return 0;
		try {
			if (!this.blockActive && !this.readBlock()) return -1;
			int count = 0;
			while (count < length && (this.rleRemaining > 0 || this.repeats > 0)) {
				if (this.repeats > 0) {
					int take = Math.min(this.repeats, length - count);
					Arrays.fill(bytes, offset + count, offset + count + take, (byte) this.lastByte);
					this.repeats -= take;
					count += take;
				} else {
					int value = this.nextRleByte();
					if (value == this.lastByte) this.sameBytes++;
					else {
						this.lastByte = value;
						this.sameBytes = 1;
					}
					if (this.sameBytes == 4) {
						if (this.rleRemaining == 0) throw new IOException("Missing BZip2 run length");
						this.repeats = this.nextRleByte();
						this.sameBytes = 0;
					}
					bytes[offset + count++] = (byte) value;
				}
			}
			this.crc.append(bytes, offset, count);
			if (this.rleRemaining == 0 && this.repeats == 0) {
				int actual = (int) this.crc.getValue();
				if (actual != this.expectedBlockCrc) throw new IOException("BZip2 block CRC mismatch");
				this.combinedCrc = Integer.rotateLeft(this.combinedCrc, 1) ^ actual;
				this.blockActive = false;
			}
			return count;
		} catch (IOException e) {
			this.failure = e;
			throw e;
		}
	}

	/**
	 * <h3 class="en-US">Decode block metadata and prepare inverse BWT traversal</h3>
	 * <h3 class="zh-CN">解码块元数据并准备逆 BWT 遍历</h3>
	 *
	 * <p class="en-US">Parses the alphabet, selector MTF values and Huffman lengths, restores the BWT last column using Huffman/RLE2/inverse MTF, and constructs inverse-BWT successor indexes.</p>
	 * <p class="zh-CN">解析字母表、选择器的 MTF 值及 Huffman 码长，通过 Huffman、RLE2 和逆 MTF 恢复 BWT 最后一列，并构建逆 BWT 的后继索引。</p>
	 *
	 * <p class="en-US">Does not expand the whole RLE1 output. It may skip empty concatenated members before finding a data block or a valid end.</p>
	 * <p class="zh-CN">不会一次性展开整个 RLE1 输出。串接模式下，可能跳过多个空成员后才找到数据块或有效结束位置。</p>
	 *
	 * @return <span class="en-US">True when a data block is ready, false after a valid end</span>
	 * <span class="zh-CN">准备好数据块时返回 true，到达有效结束位置时返回 false</span>
	 * @throws IOException <span class="en-US">Block metadata, encoded data or a member CRC is invalid, input is truncated, or the delegate fails</span>
	 *                     <span class="zh-CN">块元数据、编码数据或成员 CRC 非法，输入截断，或底层读取失败</span>
	 */
	private boolean readBlock() throws IOException {
		if (this.finished) return false;
		while (true) {
			long magic = ((long) this.readBits(24) << 24) | this.readBits(24);
			if (magic == END_MAGIC) {
				if (this.readBits(32) != this.combinedCrc) throw new IOException("BZip2 stream CRC mismatch");
				this.bitCount = 0;
				this.bitBuffer = 0;
				if (this.concatenated) {
					int first = this.input.read();
					if (first >= 0) {
						this.readHeader(first);
						continue;
					}
				}
				this.finished = true;
				this.lastColumn = null;
				this.inverse = null;
				return false;
			}
			if (magic != BLOCK_MAGIC) throw new IOException("Invalid BZip2 block marker");
			this.expectedBlockCrc = this.readBits(32);
			this.randomized = this.readBits(1) != 0;
			int originalPointer = this.readBits(24);
			// 阶段 3：读取 16 组、每组 16 个字节值的使用位图，构建初始 MTF 字母表。
			int[] mtf = new int[256];
			boolean[] groups = new boolean[16];
			for (int index = 0; index < 16; index++) groups[index] = this.readBits(1) != 0;
			int used = 0;
			for (int group = 0; group < 16; group++) {
				if (groups[group]) {
					for (int low = 0; low < 16; low++) {
						if (this.readBits(1) != 0) mtf[used++] = group * 16 + low;
					}
				}
			}
			if (used == 0) throw new IOException("Empty BZip2 block alphabet");
			int tableCount = this.readBits(3), selectorCount = this.readBits(15);
			if (tableCount < 2 || tableCount > 6 || selectorCount < 1 || selectorCount > MAX_SELECTORS) {
				throw new IOException("Invalid BZip2 Huffman group counts");
			}
			int[] selectors = new int[selectorCount], tableOrder = {0, 1, 2, 3, 4, 5};
			for (int index = 0; index < selectorCount; index++) {
				int position = 0;
				while (this.readBits(1) != 0) {
					if (++position >= tableCount) throw new IOException("Invalid BZip2 table selector");
				}
				int table = tableOrder[position];
				System.arraycopy(tableOrder, 0, tableOrder, 1, position);
				tableOrder[0] = table;
				selectors[index] = table;
			}
			HuffmanTree.Book[] tables = new HuffmanTree.Book[tableCount];
			for (int table = 0; table < tableCount; table++) {
				int[] lengths = new int[used + 2];
				int current = this.readBits(5);
				for (int symbol = 0; symbol < lengths.length; symbol++) {
					if (current < 1 || current > 20) throw new IOException("Invalid BZip2 code length");
					while (this.readBits(1) != 0) {
						current += this.readBits(1) == 0 ? 1 : -1;
						if (current < 1 || current > 20) throw new IOException("Invalid BZip2 code length");
					}
					lengths[symbol] = current;
				}
				try {
					tables[table] = HuffmanTree.canonicalCodes(lengths);
				} catch (IllegalArgumentException e) {
					throw new IOException("Invalid BZip2 Huffman table", e);
				}
			}
			if (this.lastColumn == null || this.lastColumn.length < this.blockCapacity) {
				this.lastColumn = new byte[this.blockCapacity];
				this.inverse = new int[this.blockCapacity];
			}
			SymbolReader reader = new SymbolReader(selectors, tables);
			int[] frequencies = new int[256];
			int count = 0, symbol = reader.next();
			while (symbol != used + 1) {
				if (symbol <= 1) {
					int run = 0, weight = 1;
					do {
						long expanded = (long) run + (long) (symbol + 1) * weight;
						if (expanded > this.blockCapacity - count)
							throw new IOException("BZip2 block exceeds size limit");
						run = (int) expanded;
						weight <<= 1;
						symbol = reader.next();
					} while (symbol <= 1);
					Arrays.fill(this.lastColumn, count, count + run, (byte) mtf[0]);
					frequencies[mtf[0]] += run;
					count += run;
				} else {
					int position = symbol - 1;
					if (position >= used || count >= this.blockCapacity)
						throw new IOException("Invalid BZip2 block data");
					int value = mtf[position];
					System.arraycopy(mtf, 0, mtf, 1, position);
					mtf[0] = value;
					this.lastColumn[count++] = (byte) value;
					frequencies[value]++;
					symbol = reader.next();
				}
			}
			if (count == 0 || originalPointer >= count) throw new IOException("Invalid BZip2 original pointer");
			int[] starts = new int[256];
			for (int value = 1; value < 256; value++) {
				starts[value] = starts[value - 1] + frequencies[value - 1];
			}
			for (int index = 0; index < count; index++) {
				this.inverse[starts[this.lastColumn[index] & 0xFF]++] = index;
			}
			this.bwtPosition = this.inverse[originalPointer];
			this.rleRemaining = count;
			this.lastByte = -1;
			this.sameBytes = 0;
			this.repeats = 0;
			this.randomIndex = 0;
			this.randomRemaining = 0;
			this.crc.reset();
			this.blockActive = true;
			return true;
		}
	}

	/**
	 * <h3 class="en-US">Advance inverse BWT and return one RLE1-encoded byte</h3>
	 * <h3 class="zh-CN">推进逆 BWT 并返回一个 RLE1 编码字节</h3>
	 *
	 * <p class="en-US">The caller must ensure that rleRemaining is positive. Legacy derandomization, when required, is applied before the returned byte is interpreted by RLE1.</p>
	 * <p class="zh-CN">调用方必须保证 rleRemaining 大于零。若当前块使用旧版随机化，先执行去随机化，再将字节交给 RLE1 解释。</p>
	 *
	 * @return <span class="en-US">Next byte value from 0 to 255, which can be an RLE1 literal or a count</span>
	 * <span class="zh-CN">范围为 0 至 255 的下一编码字节，可能是 RLE1 字面量，也可能是计数值</span>
	 */
	private int nextRleByte() {
		int value = this.lastColumn[this.bwtPosition] & 0xFF;
		this.bwtPosition = this.inverse[this.bwtPosition];
		this.rleRemaining--;
		if (this.randomized) {
			if (this.randomRemaining == 0) {
				this.randomRemaining = RANDOM_NUMBERS[this.randomIndex];
				this.randomIndex = (this.randomIndex + 1) & 0x1FF;
			}
			if (--this.randomRemaining == 1) value ^= 1;
		}
		return value;
	}

	/**
	 * <h2 class="en-US">Huffman symbol reader with selector-controlled table switching</h2>
	 * <h2 class="zh-CN">根据选择器切换码表的 Huffman 符号读取器</h2>
	 *
	 * <p class="en-US">Each selector covers 50 Huffman symbols, including RUNA, RUNB and EOB.
	 * This is not a count of compressed bytes or uncompressed output bytes.</p>
	 * <p class="zh-CN">每个选择器负责 50 个 Huffman 符号，包含 RUNA、RUNB 和块结束符，
	 * 该数量不是压缩字节数，也不是最终解压输出字节数。</p>
	 */
	private final class SymbolReader {
		/**
		 * <span class="en-US">Decoded Huffman table indexes in use order</span>
		 * <span class="zh-CN">按使用顺序排列的已解码 Huffman 表下标</span>
		 */
		private final int[] selectors;
		/**
		 * <span class="en-US">Canonical Huffman code books for this block</span>
		 * <span class="zh-CN">当前块的规范 Huffman 码表数组</span>
		 */
		private final HuffmanTree.Book[] tables;
		/**
		 * <span class="en-US">Code book selected for the current group</span>
		 * <span class="zh-CN">当前符号组使用的码表</span>
		 */
		private HuffmanTree.Book current;
		/**
		 * <span class="en-US">Index of the next selector to consume</span>
		 * <span class="zh-CN">下一个待消费的选择器下标</span>
		 */
		private int selector;
		/**
		 * <span class="en-US">Number of symbols still covered by the current selector</span>
		 * <span class="zh-CN">当前选择器仍可覆盖的 Huffman 符号数量</span>
		 */
		private int remaining;

		/**
		 * <h3 class="en-US">Initialize a block-local Huffman symbol reader</h3>
		 * <h3 class="zh-CN">初始化当前块的 Huffman 符号读取器</h3>
		 *
		 * @param selectors <span class="en-US">Validated table selectors in use order</span>
		 *                  <span class="zh-CN">已经校验且按使用顺序排列的码表选择器</span>
		 * @param tables    <span class="en-US">Validated canonical Huffman code books</span>
		 *                  <span class="zh-CN">已经校验的规范 Huffman 码表</span>
		 */
		private SymbolReader(final int[] selectors, final HuffmanTree.Book[] tables) {
			this.selectors = selectors;
			this.tables = tables;
		}

		/**
		 * <h3 class="en-US">Decode the next Huffman symbol</h3>
		 * <h3 class="zh-CN">解码下一个 Huffman 符号</h3>
		 *
		 * <p class="en-US">Selects a new table after every 50 symbols. Starts at the selected table's minimum length, then extends one bit at a time until a symbol matches or the maximum length is exceeded.</p>
		 * <p class="zh-CN">每解码 50 个符号后选择下一张表。从当前表最小码长开始读取，逐位扩展，直到匹配符号或超过最大码长。</p>
		 *
		 * @return <span class="en-US">RUNA (0), RUNB (1), a nonzero MTF rank plus one, or EOB</span>
		 * <span class="zh-CN">RUNA（0）、RUNB（1）、非零 MTF 排名加一，或块结束符</span>
		 * @throws IOException <span class="en-US">A selector is missing, a code is invalid, input is truncated, or the delegate fails</span>
		 *                     <span class="zh-CN">缺少选择器、编码非法、输入截断或底层读取失败</span>
		 */
		private int next() throws IOException {
			if (this.remaining == 0) {
				if (this.selector >= this.selectors.length) throw new IOException("Missing BZip2 table selector");
				this.current = this.tables[this.selectors[this.selector++]];
				this.remaining = 50;
			}
			this.remaining--;
			int length = this.current.getMinLength();
			int code = BZip2InputStream.this.readBits(length);
			while (true) {
				int symbol = this.current.getSymbol(code, length);
				if (symbol >= 0) return symbol;
				if (++length > this.current.getMaxLength()) throw new IOException("Invalid BZip2 Huffman code");
				code = (code << 1) | BZip2InputStream.this.readBits(1);
			}
		}
	}

	/**
	 * <h3 class="en-US">Read compressed bits in most-significant-bit-first order</h3>
	 * <h3 class="zh-CN">按最高有效位优先顺序读取压缩位</h3>
	 *
	 * <p class="en-US">Reads only enough source bytes to satisfy this request. Surplus bits remain in the low bitCount bits for the next request; no whole-member read-ahead buffer is used.</p>
	 * <p class="zh-CN">只从底层读取满足本次请求所需的字节。多余位保留在低 bitCount 位供下次使用，不设置跨成员的主动预读缓冲。</p>
	 *
	 * <p class="en-US">For 32-bit reads, the result preserves the bit pattern in a signed int, which is suitable for direct CRC comparison.</p>
	 * <p class="zh-CN">读取 32 位时，结果保存在有符号 int 中，但原始位模式不变，可直接用于 CRC 比较。</p>
	 *
	 * @param count <span class="en-US">Number of bits, from 0 to 32; internal callers guarantee this range</span>
	 *              <span class="zh-CN">读取位数，范围为 0 至 32，由内部调用方保证</span>
	 * @return <span class="en-US">Requested bits right-aligned in an int</span>
	 * <span class="zh-CN">低位对齐的读取结果</span>
	 * @throws IOException <span class="en-US">Input is truncated or the delegate fails</span>
	 *                     <span class="zh-CN">输入截断或底层读取失败</span>
	 */
	private int readBits(final int count) throws IOException {
		while (this.bitCount < count) {
			this.bitBuffer = (this.bitBuffer << 8) | this.readSourceByte();
			this.bitCount += 8;
		}
		this.bitCount -= count;
		int value = (int) ((this.bitBuffer >>> this.bitCount) & ((1L << count) - 1));
		this.bitBuffer &= (1L << this.bitCount) - 1;
		return value;
	}

	/**
	 * <h3 class="en-US">Read a required to be compressed byte</h3>
	 * <h3 class="zh-CN">读取一个必须存在的压缩字节</h3>
	 *
	 * <p class="en-US">EOF here means truncation, rather than a normal member end. Normal EOF is decided after a validated BZip2 end marker.</p>
	 * <p class="zh-CN">此处遇到 EOF 表示输入截断，而非成员正常结束。正常结束必须由已经校验的 BZip2 流尾标记确定。</p>
	 *
	 * @return <span class="en-US">Unsigned source byte from 0 to 255</span>
	 * <span class="zh-CN">范围为 0 至 255 的无符号压缩字节</span>
	 * @throws EOFException <span class="en-US">A required byte is missing</span>
	 *                      <span class="zh-CN">缺少必须存在的压缩字节</span>
	 * @throws IOException  <span class="en-US">The delegate fails</span>
	 *                      <span class="zh-CN">底层读取失败</span>
	 */
	private int readSourceByte() throws IOException {
		int value = this.input.read();
		if (value < 0) throw new EOFException("Truncated BZip2 stream");
		return value;
	}

	/**
	 * <h3 class="en-US">Return a conservative count of immediately available output bytes</h3>
	 * <h3 class="zh-CN">返回无需继续读取底层流即可输出的保守字节数量</h3>
	 *
	 * <p class="en-US">Reports only pending RLE1 repetitions. Zero does not indicate EOF and does not mean that the current block has no more data.</p>
	 * <p class="zh-CN">只报告等待输出的 RLE1 重复字节数。返回零不表示 EOF，也不表示当前块已无剩余数据。</p>
	 *
	 * @return <span class="en-US">Pending additional RLE1 repetitions</span>
	 * <span class="zh-CN">等待输出的 RLE1 额外重复字节数</span>
	 * @throws IOException <span class="en-US">The stream is closed or previously failed</span>
	 *                     <span class="zh-CN">流已关闭或此前已经失败</span>
	 */
	@Override
	public int available() throws IOException {
		this.ensureOpen();
		return this.repeats;
	}

	/**
	 * <h3 class="en-US">Release block buffers and close the delegate</h3>
	 * <h3 class="zh-CN">释放块缓冲区并关闭底层输入流</h3>
	 *
	 * <p class="en-US">Idempotent. Does not drain unread data or complete outstanding CRC checks. The closed state is set before closing the delegate, even if delegate closing fails.</p>
	 * <p class="zh-CN">可重复调用。不会读取剩余数据，也不会补做未完成的 CRC 校验。关闭底层流之前即标记本流已关闭，底层关闭失败也不会恢复可读状态。</p>
	 *
	 * @throws IOException <span class="en-US">Closing the delegate fails</span>
	 *                     <span class="zh-CN">关闭底层输入流失败</span>
	 */
	@Override
	public void close() throws IOException {
		if (this.closed) return;
		this.closed = true;
		this.lastColumn = null;
		this.inverse = null;
		this.input.close();
	}

	/**
	 * <h3 class="en-US">Check that the stream is open and has not failed</h3>
	 * <h3 class="zh-CN">检查输入流是否仍可读取</h3>
	 *
	 * <p class="en-US">A decoding failure may leave partial state. Subsequent reads fail with the original exception as their cause rather than attempting recovery.</p>
	 * <p class="zh-CN">解码失败可能留下不完整状态，后续读取以原始异常为原因再次失败，不尝试从中间状态恢复。</p>
	 *
	 * @throws IOException <span class="en-US">The stream is closed or previously failed</span>
	 *                     <span class="zh-CN">流已关闭或此前已经失败</span>
	 */
	private void ensureOpen() throws IOException {
		if (this.closed) throw new IOException("BZip2 input stream is closed");
		if (this.failure != null) throw new IOException("BZip2 input stream has failed", this.failure);
	}
}
