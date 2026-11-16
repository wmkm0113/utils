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
package org.nervousync.security.crc;

import org.nervousync.beans.digest.CRCConfig;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.utils.commons.RawUtils;

import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2 class="en-US">CRC checksum adapter implement class</h2>
 * <h2 class="zh-CN">CRC摘要算法适配器的实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 13:50:21 $
 */
public final class CRCDigestAdapterImpl extends CRCAdaptor {

	/**
	 * <span class="en-US">Cached CRC table</span>
	 * <span class="en-US">缓存的 CRC 表</span>
	 */
	private static final ConcurrentHashMap<CacheKey, long[]> CACHED_CRC_TABLE = new ConcurrentHashMap<>();
	/**
	 * <span class="en-US">CRC configure</span>
	 * <span class="zh-CN">CRC设置</span>
	 */
	private final CRCConfig crcConfig;
	/**
	 * <span class="en-US">CRC polynomial</span>
	 * <span class="en-US">CRC多项式编码</span>
	 */
	private final long polynomial;
	/**
	 * <span class="en-US">CRC initialize value</span>
	 * <span class="en-US">CRC初始值</span>
	 */
	private final long init;
	/**
	 * <span class="en-US">CRC check value</span>
	 */
	private final long check;
	/**
	 * <span class="en-US">CRC mask value</span>
	 */
	private final long mask;
	/**
	 * <span class="en-US">CRC result</span>
	 * <span class="en-US">CRC计算结果</span>
	 */
	private long crc;
	/**
	 * <span class="en-US">CRC table</span>
	 * <span class="en-US">CRC 表</span>
	 */
	private final long[] crcTable;

	/**
	 * <h3 class="en-US">Constructor for CRC DigestAdapterImpl</h3>
	 * <h3 class="zh-CN">CRC摘要算法适配器实现类类的构造方法</h3>
	 *
	 * @param crcConfig <span class="en-US">CRC configure</span>
	 *                  <span class="zh-CN">CRC设置</span>
	 */
	public CRCDigestAdapterImpl(final CRCConfig crcConfig) {
		this.crcConfig = crcConfig;
		if (this.crcConfig.isRefIn()) {
			this.polynomial = reverseBit(this.crcConfig.getPolynomial(), this.crcConfig.getBit());
			this.init = reverseBit(this.crcConfig.getInit(), this.crcConfig.getBit());
		} else {
			this.polynomial = (this.crcConfig.getBit() < 8)
					? (this.crcConfig.getPolynomial() << (8 - this.crcConfig.getBit()))
					: this.crcConfig.getPolynomial();
			this.init = (this.crcConfig.getBit() < 8)
					? (this.crcConfig.getInit() << (8 - this.crcConfig.getBit()))
					: this.crcConfig.getInit();
		}
		this.crc = this.init;
		if (this.crcConfig.isRefIn()) {
			this.check = 0x1L;
		} else if (this.crcConfig.getBit() <= 8) {
			this.check = 0x80L;
		} else {
			this.check = 1L << (this.crcConfig.getBit() - 1);
		}
		if (this.crcConfig.getBit() <= 8) {
			this.mask = 0xFFL;
		} else if (this.crcConfig.getBit() == Long.SIZE) {
			this.mask = -1L;
		} else {
			this.mask = (1L << this.crcConfig.getBit()) - 1L;
		}

		if (this.crcConfig.getBit() >= 8) {
			CacheKey cacheKey = new CacheKey(this.crcConfig.getBit(), this.polynomial, this.crcConfig.isRefIn());
			this.crcTable = CACHED_CRC_TABLE.computeIfAbsent(cacheKey, CRCDigestAdapterImpl::generateTable);
		} else {
			this.crcTable = null;
		}
	}

	/**
	 * <h3 class="en-US">Append parts of the given binary data array to the current adapter</h3>
	 * <h3 class="zh-CN">追加给定的二进制字节数组到当前适配器</h3>
	 *
	 * @param dataBytes <span class="en-US">binary data array</span>
	 *                  <span class="zh-CN">二进制字节数组</span>
	 * @param position  <span class="en-US">Data begin position</span>
	 *                  <span class="zh-CN">数据起始坐标</span>
	 * @param length    <span class="en-US">Length of data append</span>
	 *                  <span class="zh-CN">追加的数据长度</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when process data</span>
	 *                         <span class="zh-CN">当处理数据时出现异常</span>
	 */
	@Override
	public void append(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		if (dataBytes == null || position < 0 || length < 0 || position > dataBytes.length - length) {
			throw new CryptoException(0x000000150001L);
		}
		int endPosition = position + length;
		if (this.crcConfig.getBit() < Byte.SIZE) {
			this.appendBitwise(dataBytes, position, endPosition);
		} else if (this.crcConfig.isRefIn()) {
			this.appendReflected(dataBytes, position, endPosition);
		} else {
			this.appendNormal(dataBytes, position, endPosition);
		}

		this.crc &= this.mask;
	}

	/**
	 * <h3 class="en-US">Append parts of the given binary data array to the current adapter and calculate the final result</h3>
	 * <h3 class="zh-CN">追加给定的二进制字节数组到当前适配器并计算最终结果</h3>
	 *
	 * @param dataBytes <span class="en-US">binary data array</span>
	 *                  <span class="zh-CN">二进制字节数组</span>
	 * @param position  <span class="en-US">Data begin position</span>
	 *                  <span class="zh-CN">数据起始坐标</span>
	 * @param length    <span class="en-US">Length of data append</span>
	 *                  <span class="zh-CN">追加的数据长度</span>
	 * @return <span class="en-US">Calculate result data byte array</span>
	 * <span class="zh-CN">计算的二进制字节数组结果</span>
	 * @throws CryptoException <span class="en-US">If an error occurs when process data</span>
	 *                         <span class="zh-CN">当处理数据时出现异常</span>
	 */
	@Override
	public byte[] finish(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		this.append(dataBytes, position, length);
		long crc = this.getValue();

		byte[] result = new byte[8];
		try {
			RawUtils.writeLong(result, ByteOrder.LITTLE_ENDIAN, crc);
		} catch (DataInvalidException ignore) {
			return new byte[0];
		}
		this.reset();
		return result;
	}

	/**
	 * <h3 class="en-US">Verify given signature data bytes is valid</h3>
	 * <h3 class="zh-CN">验证给定的签名二进制数据是合法的</h3>
	 *
	 * @param signature <span class="en-US">signature data bytes</span>
	 *                  <span class="zh-CN">签名二进制数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	@Override
	public boolean verify(final byte[] signature) {
		if (signature == null || signature.length != Long.BYTES) {
			return Boolean.FALSE;
		}

		try {
			return this.getValue() == RawUtils.readLong(signature, ByteOrder.LITTLE_ENDIAN);
		} catch (DataInvalidException ignore) {
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Reset current adapter</h3>
	 * <h3 class="zh-CN">重置当前适配器</h3>
	 */
	@Override
	public void reset() {
		this.crc = this.init;
	}

	@Override
	public long getValue() {
		long result = this.crc;

		if (this.crcConfig.getBit() < Byte.SIZE && !this.crcConfig.isRefIn()) {
			result >>>= Byte.SIZE - this.crcConfig.getBit();
		}

		if (this.crcConfig.isRefIn() != this.crcConfig.isRefOut()) {
			result = reverseBit(result, this.crcConfig.getBit());
		}

		result ^= this.crcConfig.getXorOut();
		return result & resultMask(this.crcConfig.getBit());
	}

	/**
	 * <h3 class="en-US">Reverse result bit</h3>
	 * <h3 class="zh-CN">反转结果比特位</h3>
	 *
	 * @param value <span class="en-US">result value</span>
	 *              <span class="zh-CN">结果值</span>
	 * @param bit   <span class="en-US">Bit value</span>
	 *              <span class="en-US">比特位</span>
	 * @return <span class="en-US">Reverse bit result</span>
	 * <span class="zh-CN">反转比特位的结果值</span>
	 */
	private static long reverseBit(long value, final int bit) {
		long reversed = Long.reverse(value);
		return bit == Long.SIZE ? reversed : reversed >>> (Long.SIZE - bit);
	}

	private void appendBitwise(final byte[] dataBytes, final int position, final int endPosition) {
		for (int i = position; i < endPosition; i++) {
			this.crc ^= Byte.toUnsignedInt(dataBytes[i]);

			for (int j = 0; j < Byte.SIZE; j++) {
				if ((this.crc & this.check) != 0L) {
					this.crc = (this.crcConfig.isRefIn() ? this.crc >>> 1 : this.crc << 1) ^ this.polynomial;
				} else {
					this.crc = this.crcConfig.isRefIn() ? this.crc >>> 1 : this.crc << 1;
				}
			}
		}
	}

	private void appendReflected(final byte[] dataBytes, final int position, final int endPosition) {
		for (int i = position; i < endPosition; i++) {
			int tableIndex = (int) ((this.crc ^ Byte.toUnsignedInt(dataBytes[i])) & 0xFFL);

			this.crc = (this.crc >>> Byte.SIZE) ^ this.crcTable[tableIndex];
		}
	}

	private void appendNormal(final byte[] dataBytes, final int position, final int endPosition) {
		int shift = this.crcConfig.getBit() - Byte.SIZE;

		for (int i = position; i < endPosition; i++) {
			int tableIndex = (int) (((this.crc >>> shift) ^ Byte.toUnsignedInt(dataBytes[i])) & 0xFFL);
			this.crc = ((this.crc << Byte.SIZE) ^ this.crcTable[tableIndex]) & this.mask;
		}
	}

	private static long resultMask(final int bit) {
		return (bit == Long.SIZE) ? -1L : (1L << bit) - 1L;
	}

	private static long[] generateTable(final CacheKey cacheKey) {
		long[] table = new long[256];
		int bit = cacheKey.bit;
		long mask = resultMask(cacheKey.bit);

		long check = cacheKey.refIn ? 0x1L : 1L << (cacheKey.bit - 1);

		for (int i = 0; i < table.length; i++) {
			long value;

			if (cacheKey.refIn) {
				value = i;
				for (int j = 0; j < Byte.SIZE; j++) {
					if ((value & 0x1L) != 0L) {
						value = (value >>> 1) ^ cacheKey.polynomial;
					} else {
						value >>>= 1;
					}
				}
			} else {
				value = ((long) i) << (bit - Byte.SIZE);

				for (int j = 0; j < Byte.SIZE; j++) {
					if ((value & check) != 0L) {
						value = (value << 1) ^ cacheKey.polynomial;
					} else {
						value <<= 1;
					}
				}
			}

			table[i] = value & mask;
		}
		return table;
	}

	/**
	 * @param bit        <span class="en-US">CRC bit</span>
	 *                   <span class="zh-CN">CRC比特位</span>
	 * @param polynomial <span class="en-US">CRC polynomial</span>
	 *                   <span class="en-US">CRC多项式编码</span>
	 * @param refIn      <span class="en-US">CRC reverse input data bytes</span>
	 *                   <span class="en-US">CRC反转输入字节数组</span>
	 */
	private record CacheKey(int bit, long polynomial, boolean refIn) {

		@Override
		public int hashCode() {
			int result = Integer.hashCode(this.bit);
			result = 31 * result + Long.hashCode(this.polynomial);
			result = 31 * result + Boolean.hashCode(this.refIn);
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof CacheKey tableKey)) {
				return false;
			}

			return this.bit == tableKey.bit && this.polynomial == tableKey.polynomial && this.refIn == tableKey.refIn;
		}
	}
}
