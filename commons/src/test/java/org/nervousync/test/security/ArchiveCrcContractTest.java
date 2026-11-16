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

package org.nervousync.test.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.security.crc.CRCAdaptor;
import org.nervousync.utils.security.DigestUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** ZIP 与 BZip2 使用不同 CRC 参数；同时约束流式状态和 finish/reset 行为。 */
final class ArchiveCrcContractTest {

	@ParameterizedTest
	@ValueSource(strings = {"CRC-32/ISO-HDLC", "CRC-32/BZIP2"})
	void standardVectorAndFinishResetContract(String algorithm) throws Exception {
		byte[] vector = "123456789".getBytes(StandardCharsets.US_ASCII);
		long expected = algorithm.equals("CRC-32/BZIP2") ? 0xFC891918L : 0xCBF43926L;
		CRCAdaptor crc = DigestUtils.CRCAdaptor(algorithm);
		assertNotNull(crc);
		assertEquals(0, crc.getValue());
		crc.append(vector, 0, 4);
		long intermediate = crc.getValue();
		assertEquals(intermediate, crc.getValue(), "getValue must not reset state");
		byte[] result = crc.finish(vector, 4, vector.length - 4);
		assertEquals(8, result.length);
		assertEquals(expected, ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).getLong());
		assertEquals(0, crc.getValue(), "finish resets state for reuse");
		crc.append(vector);
		assertEquals(expected, crc.getValue());
		assertEquals(expected, crc.getValue());
		crc.reset();
		assertEquals(0, crc.getValue());
	}

	@ParameterizedTest
	@ValueSource(strings = {"CRC-32/ISO-HDLC", "CRC-32/BZIP2"})
	void fragmentedUpdatesMatchIndependentOracles(String algorithm) throws Exception {
		Random random = new Random(0xC2C32);
		for (int length : new int[] {0, 1, 2, 7, 255, 256, 8191, 8192, 65537}) {
			byte[] bytes = new byte[length];
			random.nextBytes(bytes);
			CRC32 zip = new CRC32();
			zip.update(bytes);
			long expected = algorithm.equals("CRC-32/BZIP2") ? bzipCrc(bytes) : zip.getValue();
			CRCAdaptor crc = DigestUtils.CRCAdaptor(algorithm);
			for (int offset = 0; offset < bytes.length;) {
				int count = Math.min(1 + random.nextInt(257), bytes.length - offset);
				crc.append(bytes, offset, count);
				assertEquals(crc.getValue(), crc.getValue());
				offset += count;
			}
			crc.append(new byte[0]);
			assertEquals(expected, crc.getValue(), "length=" + length);
			assertEquals(expected, ByteBuffer.wrap(crc.finish()).order(ByteOrder.LITTLE_ENDIAN).getLong());
			assertEquals(0, crc.getValue());
		}
	}

	/** 无查表的 MSB-first 参考计算，避免与生产表实现共享错误。 */
	private static long bzipCrc(byte[] bytes) {
		int crc = 0xFFFFFFFF;
		for (byte value : bytes) {
			crc ^= (value & 0xFF) << 24;
			for (int bit = 0; bit < 8; bit++) crc = (crc << 1) ^ (crc < 0 ? 0x04C11DB7 : 0);
		}
		return Integer.toUnsignedLong(~crc);
	}
}
