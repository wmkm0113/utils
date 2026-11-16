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

package org.nervousync.test.archive.bzip2;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.io.bzip2.BZip2InputStream;
import org.nervousync.archive.io.bzip2.BZip2OutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** BZip2 流专项：独立实现互通、边界、损坏输入及资源生命周期。 */
final class BZip2StreamTest {

	private static BZip2InputStream input(InputStream source, boolean concatenated) throws IOException {
		return new BZip2InputStream(source, concatenated);
	}

	private static BZip2OutputStream output(OutputStream target, int size) throws IOException {
		return new BZip2OutputStream(target, size);
	}

	private static byte[] encode(byte[] data, int level, boolean reference) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (OutputStream out = reference
				? new BZip2CompressorOutputStream(bytes, level) : output(bytes, level)) {
			int offset = 0;
			while (offset < data.length) {
				int length = Math.min(7919, data.length - offset);
				out.write(data, offset, length);
				offset += length;
			}
		}
		return bytes.toByteArray();
	}

	private static byte[] decode(byte[] data, boolean reference) throws Exception {
		try (InputStream in = reference
				? new BZip2CompressorInputStream(new ByteArrayInputStream(data))
				: input(new ByteArrayInputStream(data), false)) {
			return in.readAllBytes();
		}
	}

	/** 固定向量由 CPython bz2 生成；运行测试不需要 Python，也不经过本项目编码器。 */
	@Test
	void decodesFixedExternalVectors() throws Exception {
		assertArrayEquals("".getBytes(StandardCharsets.US_ASCII),
				decode(Base64.getDecoder().decode("QlpoMRdyRThQkAAAAAA="), false));
		assertArrayEquals("123456789".getBytes(StandardCharsets.US_ASCII),
				decode(Base64.getDecoder().decode("QlpoMTFBWSZTWfyJGRgAAAAIAD/gIAAiDQwIMmIeDe8pwXckU4UJD8iRkYA="), false));
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9})
	void interoperatesWithReferenceAtEveryBlockSize(int level) throws Exception {
		byte[] random = new byte[104729];
		new Random(42).nextBytes(random);
		byte[] text = "BZip2 编解码：the quick brown fox jumps over the lazy dog\n"
				.repeat(2000).getBytes(StandardCharsets.UTF_8);
		byte[] runs = new byte[280000];
		for (int i = 0; i < runs.length; i++) runs[i] = (byte) (i / 259);
		for (byte[] data : new byte[][] {new byte[0], {0}, {1, 1, 1, 1}, random, text, runs}) {
			byte[] encoded = encode(data, level, false);
			assertArrayEquals(data, decode(encoded, true), "reference decoder, level=" + level);
			assertArrayEquals(data, decode(encoded, false), "native round trip, level=" + level);
			assertArrayEquals(data, decode(encode(data, level, true), false),
					"reference encoder, level=" + level);
		}
	}

	@Test
	void handlesFullSizedBlocksAndHighlyExpandingRuns() throws Exception {
		byte[] random = new byte[1900017];
		new Random(88).nextBytes(random);
		assertArrayEquals(random, decode(encode(random, 9, false), true));
		assertArrayEquals(random, decode(encode(random, 9, true), false));
		byte[] repeated = new byte[20000000];
		Arrays.fill(repeated, (byte) 0xE1);
		assertArrayEquals(repeated, decode(encode(repeated, 1, false), true));
		assertArrayEquals(repeated, decode(encode(repeated, 1, true), false));
	}

	@Test
	void rejectsEveryTruncationAndCorruptedCrc() throws Exception {
		byte[] data = "BZip2 truncation and CRC validation".repeat(10).getBytes(StandardCharsets.UTF_8);
		byte[] compressed = encode(data, 1, true);
		for (int length = 0; length < compressed.length; length++) {
			byte[] truncated = Arrays.copyOf(compressed, length);
			assertThrows(IOException.class, () -> decode(truncated, false), "truncated at " + length);
		}
		byte[] badBlockCrc = compressed.clone();
		badBlockCrc[10] ^= 1;
		assertThrows(IOException.class, () -> decode(badBlockCrc, false));
		byte[] badCombinedCrc = compressed.clone();
		badCombinedCrc[badCombinedCrc.length - 2] ^= 0x10;
		assertThrows(IOException.class, () -> decode(badCombinedCrc, false));
		assertThrows(IOException.class, () -> decode(new byte[] {'B', 'Z', 'h', '0'}, false));
	}

	@Test
	void handlesConcatenationAndLeavesFollowingBytesUntouchedByDefault() throws Exception {
		byte[] a = encode(new byte[] {1, 2, 3}, 1, true);
		byte[] b = encode(new byte[] {4, 5}, 9, true);
		ByteArrayOutputStream combined = new ByteArrayOutputStream();
		combined.write(a); combined.write(b);
		try (InputStream in = input(new ByteArrayInputStream(combined.toByteArray()), true)) {
			assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, in.readAllBytes());
		}
		ByteArrayInputStream source = new ByteArrayInputStream(combined.toByteArray());
		try (InputStream in = input(source, false)) {
			assertArrayEquals(new byte[] {1, 2, 3}, in.readAllBytes());
			assertEquals(b.length, source.available());
			assertEquals('B', source.read());
		}
		combined.write(42);
		assertThrows(IOException.class, () -> {
			try (InputStream in = input(new ByteArrayInputStream(combined.toByteArray()), true)) {
				in.readAllBytes();
			}
		});
	}

	@Test
	void respectsReadSkipAndCloseContracts() throws Exception {
		byte[] data = new byte[20000]; new Random(19).nextBytes(data);
		byte[] compressed = encode(data, 1, true);
		class Source extends ByteArrayInputStream {
			boolean closed;
			Source() { super(compressed); }
			@Override public void close() { closed = true; }
		}
		Source source = new Source();
		InputStream in = input(source, false);
		assertEquals(0, in.read(new byte[0]));
		assertEquals(data[0] & 255, in.read());
		assertEquals(12345, in.skip(12345));
		assertEquals(0, in.skip(-1));
		assertArrayEquals(Arrays.copyOfRange(data, 12346, data.length), in.readAllBytes());
		assertEquals(-1, in.read());
		assertEquals(0, in.read(new byte[0]));
		assertEquals(0, in.skip(10));
		in.close(); in.close();
		assertTrue(source.closed);
		assertThrows(IOException.class, in::read);
		assertThrows(IndexOutOfBoundsException.class, () -> in.read(new byte[1], 1, 1));
	}

	@Test
	void finishIsIdempotentAndFlushDoesNotFinish() throws Exception {
		class Target extends ByteArrayOutputStream {
			boolean closed;
			@Override public void close() { closed = true; }
		}
		Target target = new Target();
		BZip2OutputStream out = output(target, 1);
		out.write(7); out.flush(); out.write(new byte[] {8, 9});
		out.finish();
		byte[] finished = target.toByteArray();
		out.finish();
		assertArrayEquals(finished, target.toByteArray());
		assertFalse(target.closed);
		assertThrows(IOException.class, () -> out.write(10));
		out.close(); out.close();
		assertTrue(target.closed);
		assertArrayEquals(new byte[] {7, 8, 9}, decode(finished, true));
		assertThrows(IllegalArgumentException.class, () -> output(new ByteArrayOutputStream(), 0));
		assertThrows(IllegalArgumentException.class, () -> output(new ByteArrayOutputStream(), 10));
	}

	@Test
	void closeReportsFinishFailureAndClosesDelegateExactlyOnce() throws Exception {
		class Target extends OutputStream {
			int bytes, closes;
			final IOException failure = new IOException("write failure");
			final IOException closeFailure = new IOException("close failure");
			@Override public void write(int value) throws IOException { if (++bytes > 4) throw failure; }
			@Override public void close() throws IOException { closes++; throw closeFailure; }
		}
		Target target = new Target();
		BZip2OutputStream out = output(target, 1);
		out.write(1);
		IOException error = assertThrows(IOException.class, out::close);
		assertSame(target.failure, error);
		assertArrayEquals(new Throwable[] {target.closeFailure}, error.getSuppressed());
		out.close();
		assertEquals(1, target.closes);
	}

	@Test
	void aPreviouslyReportedWriteFailureIsNotRethrownByClose() throws Exception {
		class Target extends OutputStream {
			int bytes, closes;
			@Override public void write(int value) throws IOException {
				if (++bytes > 4) throw new IOException("write failure");
			}
			@Override public void close() { closes++; }
		}
		Target target = new Target();
		BZip2OutputStream out = output(target, 1);
		byte[] data = new byte[150000]; new Random(0).nextBytes(data);
		assertThrows(IOException.class, () -> out.write(data));
		assertThrows(IOException.class, () -> out.write(7));
		assertDoesNotThrow(out::close);
		assertEquals(1, target.closes);
	}

	@Test
	void handlesRunAndBlockBoundariesWithSingleByteWrites() throws Exception {
		for (int length : new int[] {0, 1, 2, 3, 4, 5, 254, 255, 256, 258, 259, 260, 9980, 99980, 99981, 100000}) {
			byte[] data = new byte[length];
			for (int i = 0; i < length; i++) data[i] = (byte) (i / 255);
			ByteArrayOutputStream encoded = new ByteArrayOutputStream();
			try (OutputStream out = output(encoded, 1)) {
				for (byte value : data) out.write(value & 255);
			}
			assertArrayEquals(data, decode(encoded.toByteArray(), true));
			assertArrayEquals(data, decode(encoded.toByteArray(), false));
		}
		for (int length : new int[] {99980, 99981, 99982, 100000, 200000}) {
			byte[] data = new byte[length]; new Random(length).nextBytes(data);
			assertArrayEquals(data, decode(encode(data, 1, false), true));
		}
	}

	/** 历史随机化格式使用固定外部样本，覆盖 512 项随机表循环边界。 */
	@Test
	void decodesLegacyRandomizedBlocksIncludingTableWraparound() throws Exception {
		byte[] original = new byte[400000];
		for (int i = 0; i < original.length; i++) original[i] = (byte) (i % 251);
		byte[] encoded;
		try (InputStream resource = getClass().getResourceAsStream("/bzip2/legacy-randomized.bz2")) {
			assertNotNull(resource, "legacy fixture");
			encoded = resource.readAllBytes();
		}
		assertArrayEquals(original, decode(encoded, true), "reference validates fixture");
		assertArrayEquals(original, decode(encoded, false));
	}

	/** 参数检查发生在读写之前，不得把调用错误变成损坏的压缩数据。 */
	@SuppressWarnings("resource")
	@Test
	void validatesArgumentsWithoutConsumingOrProducingData() throws Exception {
		assertThrows(NullPointerException.class, () -> new BZip2InputStream(null));
		assertThrows(NullPointerException.class, () -> new BZip2OutputStream(null, 1));
		ByteArrayOutputStream target = new ByteArrayOutputStream();
		for (int level : new int[] {Integer.MIN_VALUE, 0, 10, Integer.MAX_VALUE}) {
			assertThrows(IllegalArgumentException.class, () -> new BZip2OutputStream(target, level));
			assertEquals(0, target.size());
		}
		try (BZip2OutputStream out = output(target, 1)) {
			assertThrows(NullPointerException.class, () -> out.write(null, 0, 0));
			for (int[] range : new int[][] {{-1, 1}, {0, -1}, {1, 1}, {1, Integer.MAX_VALUE}}) {
				assertThrows(IndexOutOfBoundsException.class, () -> out.write(new byte[1], range[0], range[1]));
			}
			out.write(new byte[0]);
			out.write(0x1FF); // OutputStream.write(int) 只使用低八位。
		}
		assertArrayEquals(new byte[] {(byte) 0xFF}, decode(target.toByteArray(), true));
		try (BZip2InputStream in = new BZip2InputStream(new ByteArrayInputStream(target.toByteArray()))) {
			assertFalse(in.markSupported());
			assertThrows(IOException.class, in::reset);
			assertThrows(NullPointerException.class, () -> in.read(null, 0, 0));
			for (int[] range : new int[][] {{-1, 1}, {0, -1}, {1, 1}, {1, Integer.MAX_VALUE}}) {
				assertThrows(IndexOutOfBoundsException.class, () -> in.read(new byte[1], range[0], range[1]));
			}
			assertEquals(0xFF, in.read());
			assertEquals(-1, in.read());
		}
	}

	/** 构造失败时，底层流仍由调用方负责关闭。 */
	@Test
	void constructorFailuresLeaveDelegatesOwnedByCaller() throws Exception {
		class Source extends ByteArrayInputStream {
			boolean closed;
			Source() { super(new byte[] {'B', 'Z', 'h', '0'}); }
			@Override public void close() { closed = true; }
		}
		Source source = new Source();
		assertThrows(IOException.class, () -> new BZip2InputStream(source));
		assertFalse(source.closed);
		class Target extends OutputStream {
			boolean closed;
			@Override public void write(int value) throws IOException { throw new IOException("header write"); }
			@Override public void close() { closed = true; }
		}
		Target target = new Target();
		assertThrows(IOException.class, () -> output(target, 1));
		assertFalse(target.closed);
	}

	@Test
	void inputRetainsFailureAndClosesDelegateExactlyOnce() throws Exception {
		byte[] encoded = encode(new byte[] {1, 2, 3}, 1, true);
		encoded[10] ^= 1;
		class Source extends ByteArrayInputStream {
			int closes;
			Source() { super(encoded); }
			@Override public void close() { closes++; }
		}
		Source source = new Source();
		BZip2InputStream in = input(source, false);
		IOException failure = assertThrows(IOException.class, in::readAllBytes);
		assertSame(failure, assertThrows(IOException.class, in::read).getCause());
		in.close(); in.close();
		assertEquals(1, source.closes);
	}

	@Test
	void flushFailureIsStickyAndClosedOperationsAreRejected() throws Exception {
		class Target extends ByteArrayOutputStream {
			final IOException failure = new IOException("flush failure");
			int closes;
			@Override public void flush() throws IOException { throw failure; }
			@Override public void close() { closes++; }
		}
		Target target = new Target();
		BZip2OutputStream out = output(target, 1);
		out.write(1);
		assertSame(target.failure, assertThrows(IOException.class, out::flush));
		assertSame(target.failure, assertThrows(IOException.class, out::finish).getCause());
		assertSame(target.failure, assertThrows(IOException.class, () -> out.write(2)).getCause());
		out.close(); out.close();
		assertEquals(1, target.closes);
		assertThrows(IOException.class, out::flush);
		assertThrows(IOException.class, out::finish);
		assertThrows(IOException.class, () -> out.write(3));
	}

	/** available 为零不代表 EOF；构造和提前关闭不得主动读取整个成员。 */
	@Test
	void constructorIsLazyAndEarlyCloseDoesNotDrainInput() throws Exception {
		byte[] encoded = encode(new byte[] {9, 8, 7}, 1, true);
		encoded[10] ^= 1; // 提前关闭尚未解码的坏块，不触发 CRC 读取。
		class Source extends ByteArrayInputStream {
			int closes;
			Source() { super(encoded); }
			@Override public void close() { closes++; }
		}
		Source source = new Source();
		BZip2InputStream in = new BZip2InputStream(source);
		assertEquals(encoded.length - 4, source.available());
		assertEquals(0, in.available());
		in.close(); in.close();
		assertEquals(encoded.length - 4, source.available());
		assertEquals(1, source.closes);
	}

	@Test
	void skipStillValidatesBlockAndCombinedCrc() throws Exception {
		byte[] encoded = encode(new byte[4096], 1, true);
		for (int index : new int[] {10, encoded.length - 2}) {
			byte[] broken = encoded.clone();
			broken[index] ^= 0x10;
			try (BZip2InputStream in = input(new ByteArrayInputStream(broken), false)) {
				assertThrows(IOException.class, () -> in.skip(Long.MAX_VALUE));
			}
		}
	}

	@Test
	void concatenationSkipsEmptyMembersAndAcceptsDifferentBlockSizes() throws Exception {
		ByteArrayOutputStream combined = new ByteArrayOutputStream();
		combined.write(encode(new byte[0], 1, true));
		combined.write(encode(new byte[] {(byte) 0xFF}, 9, true));
		combined.write(encode(new byte[0], 5, true));
		try (BZip2InputStream in = input(new ByteArrayInputStream(combined.toByteArray()), true)) {
			assertArrayEquals(new byte[] {(byte) 0xFF}, in.readAllBytes());
			assertEquals(-1, in.read());
		}
	}

	private static final class Bits {
		final StringBuilder bits = new StringBuilder();
		Bits add(int count, long value) {
			for (int i = count - 1; i >= 0; i--) bits.append((value >>> i) & 1);
			return this;
		}
		byte[] bytes() {
			bits.append("0".repeat((8 - bits.length() % 8) % 8));
			byte[] result = new byte[bits.length() / 8];
			for (int i = 0; i < result.length; i++) result[i] = (byte) Integer.parseInt(bits.substring(i * 8, i * 8 + 8), 2);
			return result;
		}
	}

	private static Bits blockPrefix(int originalPointer) {
		return new Bits().add(32, 0x425A6831L).add(48, 0x314159265359L).add(32, 0)
				.add(1, 0).add(24, originalPointer).add(16, 0x8000).add(16, 0x8000);
	}

	private static Bits oneByteTables(int originalPointer) {
		Bits bits = blockPrefix(originalPointer).add(3, 2).add(15, 1).add(1, 0);
		for (int i = 0; i < 2; i++) bits.add(5, 1).add(1, 0).add(2, 2).add(1, 0).add(1, 0);
		return bits; // lengths [1,2,2], codes [0,10,11]
	}

	@Test
	void rejectsMalformedHuffmanAndRleStructures() {
		byte[][] invalid = {
				blockPrefix(0).add(3, 1).add(15, 1).bytes(),
				blockPrefix(0).add(3, 7).add(15, 1).bytes(),
				blockPrefix(0).add(3, 2).add(15, 0).bytes(),
				blockPrefix(0).add(3, 2).add(15, 32767).bytes(),
				blockPrefix(0).add(3, 2).add(15, 1).add(2, 3).bytes(),
				blockPrefix(0).add(3, 2).add(15, 1).add(1, 0).add(5, 0).bytes(),
				blockPrefix(0).add(3, 2).add(15, 1).add(1, 0).add(5, 21).bytes(),
				blockPrefix(0).add(3, 2).add(15, 1).add(1, 0).add(5, 1).add(3, 0).bytes(),
				oneByteTables(1).add(1, 0).add(2, 3).bytes(), // origPtr out of range
				oneByteTables(0).add(2, 2).add(1, 0).add(2, 3).bytes(), // missing RLE1 count
				oneByteTables(0).add(18, 0).bytes() // expanding RUNA exceeds 100 KB
		};
		for (byte[] bytes : invalid) assertThrows(IOException.class, () -> decode(bytes, false));
	}

	@Test
	void malformedBitFlipsNeverEscapeAsUncheckedExceptions() throws Exception {
		byte[] original = encode("malformed bzip2 structures and crc".repeat(200).getBytes(StandardCharsets.UTF_8), 1, true);
		Random random = new Random(1901);
		for (int attempt = 0; attempt < 1200; attempt++) {
			byte[] bytes = original.clone();
			bytes[random.nextInt(bytes.length)] ^= (byte) (1 << random.nextInt(8));
			try (InputStream in = input(new ByteArrayInputStream(bytes), false)) {
				byte[] buffer = new byte[4096];
				int count = 0, read;
				while ((read = in.read(buffer)) >= 0) {
					assertTrue(read > 0);
					count += read;
					assertTrue(count < 1000000);
				}
			} catch (IOException expected) {
				// Structural failures and CRC failures must be reported as IOExceptions.
			}
		}
	}
}
