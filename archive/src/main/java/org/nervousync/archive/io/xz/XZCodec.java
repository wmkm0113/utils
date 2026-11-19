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
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.security.DigestUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.zip.CRC32;

/**
 * <h2 class="en-US">XZ structural coding, bounded counters and integrity state.</h2>
 * <h2 class="zh-CN">XZ 结构编码、有界计数及完整性状态。</h2>
 * <p class="en-US">No filter decoding is performed here. Structural CRC32 uses the JDK;
 * XZ CRC64 uses a reflected lookup table. Index summaries retain constant memory.</p>
 * <p class="zh-CN">本类不解码过滤器；结构 CRC32 使用 JDK，XZ CRC64 使用反射查找表。
 * 索引摘要保持固定内存用量。</p>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
final class XZCodec {

	/**
	 * <span class="en-US">Maximum Index field size, including its CRC.</span>
	 * <span class="zh-CN">包括 CRC 在内的索引最大长度。</span>
	 */
	static final long MAX_INDEX_SIZE = 1L << 34;

	/**
	 * <h3 class="en-US">Utility class.</h3>
	 * <h3 class="zh-CN">工具类。</h3>
	 */
	private XZCodec() {
	}

	/**
	 * <h3 class="en-US">Read a required unsigned byte.</h3>
	 * <h3 class="zh-CN">读取一个必需的无符号字节。</h3>
	 *
	 * @param inputStream <span class="en-US">Input stream</span>
	 *                    <span class="zh-CN">输入流</span>
	 * @throws IOException <span class="en-US">If an error occurs when read data</span>
	 *                     <span class="zh-CN">读取数据出错</span>
	 */
	static int readByte(final InputStream inputStream) throws IOException {
		int value = inputStream.read();
		if (value < 0) {
			throw new EOFException("Truncated XZ stream");
		}
		return value;
	}

	/**
	 * <h3 class="en-US">Fill a bounded caller buffer, handling zero-progress delegates.</h3>
	 * <h3 class="zh-CN">填满调用方的有界缓冲，处理无进展读取。</h3>
	 *
	 * @param inputStream <span class="en-US">Input stream</span>
	 *                    <span class="zh-CN">输入流</span>
	 * @param buffer      <span class="en-US">Buffer</span>
	 *                    <span class="zh-CN">缓冲区</span>
	 * @param offset      <span class="en-US">Offset value</span>
	 *                    <span class="zh-CN">偏移值</span>
	 * @param length      <span class="en-US">Read length</span>
	 *                    <span class="zh-CN">读取长度</span>
	 * @throws IOException <span class="en-US">If an error occurs when read data</span>
	 *                     <span class="zh-CN">读取数据出错</span>
	 */
	static void readFully(final InputStream inputStream, final byte[] buffer,
	                      int offset, int length) throws IOException {
		while (length > 0) {
			int count = inputStream.read(buffer, offset, length);
			if (count < 0) {
				throw new EOFException("Truncated XZ field");
			}
			if (count == 0) {
				buffer[offset++] = (byte) readByte(inputStream);
				length--;
			} else {
				offset += count;
				length -= count;
			}
		}
	}

	/**
	 * <span class="en-US">Encode a nonnegative 63-bit VLI.</span>
	 * <span class="zh-CN">编码非负的 63 位 VLI。</span>
	 *
	 * @param output <span class="en-US">Output stream</span>
	 *               <span class="zh-CN">输出流</span>
	 * @param value  <span class="en-US">Processed value</span>
	 *               <span class="zh-CN">要处理的数据</span>
	 */
	static void writeVli(final OutputStream output, long value) throws IOException {
		if (value < 0) {
			throw new IOException("Negative XZ VLI");
		}
		while (value >= 128) {
			output.write((int) value | 128);
			value >>>= 7;
		}
		output.write((int) value);
	}

	/**
	 * <h3 class="en-US">Decode a canonical unsigned 63-bit VLI, rejecting overlong forms.</h3>
	 * <h3 class="zh-CN">解码规范的无符号 63 位 VLI，拒绝冗长表示。</h3>
	 *
	 * @param inputStream <span class="en-US">Input stream</span>
	 *                    <span class="zh-CN">输入流</span>
	 * @throws IOException <span class="en-US">If an error occurs when read data</span>
	 *                     <span class="zh-CN">读取数据出错</span>
	 */
	static long readVli(final InputStream inputStream) throws IOException {
		long value = 0;
		for (int index = 0; index < 9; index++) {
			int next = readByte(inputStream);
			value |= (long) (next & 127) << (index * 7);
			if ((next & 128) == 0) {
				if (index != 0 && next == 0) {
					throw new IOException("Non-canonical XZ VLI");
				}
				return value;
			}
		}
		throw new IOException("XZ VLI exceeds 63 bits");
	}

	/**
	 * <h3 class="en-US">Compute structural IEEE CRC32 for a byte range.</h3>
	 * <h3 class="zh-CN">计算字节范围的结构 IEEE CRC32。</h3>
	 *
	 * @param buffer <span class="en-US">Buffer</span>
	 *               <span class="zh-CN">缓冲区</span>
	 * @param offset <span class="en-US">Offset value</span>
	 *               <span class="zh-CN">偏移值</span>
	 * @param length <span class="en-US">Read length</span>
	 *               <span class="zh-CN">读取长度</span>
	 */
	static long crc32(byte[] buffer, int offset, int length) {
		CRC32 crc = new CRC32();
		crc.update(buffer, offset, length);
		return crc.getValue();
	}

	/**
	 * <h3 class="en-US">Validate a little-endian structural CRC32.</h3>
	 * <h3 class="zh-CN">验证小端表示的结构 CRC32。</h3>
	 *
	 * @param buffer    <span class="en-US">Buffer</span>
	 *                  <span class="zh-CN">缓冲区</span>
	 * @param offset    <span class="en-US">Offset value</span>
	 *                  <span class="zh-CN">偏移值</span>
	 * @param length    <span class="en-US">Read length</span>
	 *                  <span class="zh-CN">读取长度</span>
	 * @param crcOffset <span class="en-US">CRC offset value</span>
	 *                  <span class="zh-CN">CRC 偏移值</span>
	 */
	static void verifyCrc(byte[] buffer, int offset, int length, int crcOffset) throws IOException {
		if (crc32(buffer, offset, length) != RawUtils.readUnsignedInt(buffer, crcOffset, ByteOrder.LITTLE_ENDIAN)) {
			throw new IOException("XZ structural CRC32 mismatch");
		}
	}

	/**
	 * <h2 class="en-US">Per-Block uncompressed-data Check state.</h2>
	 * <h2 class="zh-CN">每个 Block 未压缩数据的校验状态。</h2>
	 */
	static final class Check {
		/**
		 * <span class="en-US">Selected wire format.</span>
		 * <span class="zh-CN">所选编码格式。</span>
		 */
		private final XZCheck type;
		/**
		 * <span class="en-US">JDK CRC32 state, allocated only when selected.</span>
		 * <span class="zh-CN">仅选择 CRC32 时分配的 JDK 状态。</span>
		 */
		private final CRC32 crc32;
		/**
		 * <span class="en-US">SHA-256 state, allocated only when selected.</span>
		 * <span class="zh-CN">仅选择 SHA-256 时分配的状态。</span>
		 */
		private final CryptoAdaptor sha256;
		/**
		 * <span class="en-US">Reflected CRC64 register before the final complement.</span>
		 * <span class="zh-CN">最终取反之前的反射 CRC64 寄存器。</span>
		 */
		private final CryptoAdaptor crc64;

		/**
		 * <span class="en-US">Create fresh state for one Block.</span>
		 * <span class="zh-CN">为一个 Block 创建全新状态。</span>
		 */
		Check(XZCheck type) {
			this.type = type;
			this.crc32 = type == XZCheck.CRC32 ? new CRC32() : null;
			this.sha256 = type == XZCheck.SHA256 ? DigestUtils.SHA256() : null;
			this.crc64 = type == XZCheck.CRC64 ? DigestUtils.CRC("CRC-64/XZ") : null;
		}

		/**
		 * <span class="en-US">Include uncompressed bytes in the selected Check.</span>
		 * <span class="zh-CN">将未压缩字节加入所选校验。</span>
		 */
		void update(byte[] bytes, int offset, int length) {
			switch (this.type) {
				case CRC32 -> this.crc32.update(bytes, offset, length);
				case CRC64 -> this.crc64.append(bytes, offset, length);
				case SHA256 -> this.sha256.append(bytes, offset, length);
				default -> {}
			}
		}

		/**
		 * <span class="en-US">Finalize once and return bytes in their XZ wire order.</span>
		 * <span class="zh-CN">完成一次校验并返回 XZ 编码顺序的字节。</span>
		 */
		byte[] finish() {
			return switch (this.type) {
				case CRC32 -> RawUtils.longToBytes(this.crc32.getValue(), ByteOrder.LITTLE_ENDIAN, 4);
				case CRC64 -> this.crc64.finish();
				case SHA256 -> this.sha256.finish();
				default -> new byte[0];
			};
		}
	}

	/**
	 * <h2 class="en-US">Ordered Index summary without a per-Block list.</h2>
	 * <h2 class="zh-CN">不保存逐 Block 列表的有序索引摘要。</h2>
	 */
	static final class Index {
		/**
		 * <span class="en-US">Ordered pairs hashed as two little-endian 64-bit values.</span>
		 * <span class="zh-CN">按两个小端 64 位值散列的有序大小对。</span>
		 */
		private final CryptoAdaptor digest = DigestUtils.SHA256();
		/**
		 * <span class="en-US">Number of Block records.</span>
		 * <span class="zh-CN">Block 记录数量。</span>
		 */
		long records;
		/**
		 * <span class="en-US">Sum of padded Block sizes.</span>
		 * <span class="zh-CN">对齐后的 Block 大小之和。</span>
		 */
		long blocksSize;
		/**
		 * <span class="en-US">Sum of uncompressed Block sizes.</span>
		 * <span class="zh-CN">未压缩 Block 大小之和。</span>
		 */
		long uncompressedSize;

		/**
		 * <span class="en-US">Add validated sizes in their original order.</span>
		 * <span class="zh-CN">按原始顺序加入经过验证的大小。</span>
		 */
		void addRecord(long unpadded, long uncompressed) throws IOException {
			if (unpadded < 5) {
				throw new IOException("Invalid XZ Index Unpadded Size");
			}
			this.records = RawUtils.add(this.records, 1);
			long padded = RawUtils.add(unpadded, 3L) & ~3L;
			this.blocksSize = RawUtils.add(this.blocksSize, padded);
			this.uncompressedSize = RawUtils.add(this.uncompressedSize, uncompressed);
			this.digest.append(RawUtils.longToBytes(unpadded, ByteOrder.LITTLE_ENDIAN));
			this.digest.append(RawUtils.longToBytes(uncompressed, ByteOrder.LITTLE_ENDIAN));
		}

		/**
		 * <span class="en-US">Compare count, totals and ordered SHA-256 values.</span>
		 * <span class="zh-CN">比较数量、总大小及有序 SHA-256 值。</span>
		 */
		void verify(Index other) throws IOException {
			if (this.records != other.records || this.blocksSize != other.blocksSize
					|| this.uncompressedSize != other.uncompressedSize
					|| !MessageDigest.isEqual(this.digest.finish(), other.digest.finish())) {
				throw new IOException("XZ Index does not match decoded Blocks");
			}
		}
	}
}
