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
package org.nervousync.test.archive.xz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.enumerations.xz.XZCheck;
import org.nervousync.archive.io.xz.XZInputStream;
import org.nervousync.archive.io.xz.XZOutputStream;
import org.tukaani.xz.DeltaOptions;
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.LZMA2Options;

import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2 class="en-US">Independent XZ interoperability and container-validation tests.</h2>
 * <h2 class="zh-CN">独立 XZ 互操作与容器验证测试。</h2>
 */
final class XZCodecTest {
    /** Load an independently generated fixture. / 读取独立生成的固定样本。 */
    private static byte[] resource(String name) throws IOException {
        try (InputStream input = XZCodecTest.class.getResourceAsStream("/xz/" + name)) {
            assertNotNull(input, name);
            return input.readAllBytes();
        }
    }

    /** Concatenate small test arrays. / 拼接小型测试数组。 */
    private static byte[] concat(byte[]... arrays) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            bytes.write(array);
        }
        return bytes.toByteArray();
    }

    /** Independent structural CRC32 writer. / 独立的结构 CRC32 写入辅助方法。 */
    private static void crc(byte[] bytes, int offset, int length, int target) {
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, length);
        long value = crc.getValue();
        for (int index = 0; index < 4; index++) {
            bytes[target + index] = (byte) (value >>> (index * 8));
        }
    }

    /** Encode a size field for independently assembled containers. / 为独立拼装的容器编码大小字段。 */
    private static void vli(OutputStream output, long value) throws IOException {
        while (value >= 128) {
            output.write((int) value | 128);
            value >>>= 7;
        }
        output.write((int) value);
    }

    /** Decode known-valid test metadata. / 解码已知合法的测试元数据。 */
    private static long vli(byte[] bytes, int[] offset) {
        long value = 0;
        int shift = 0;
        while (true) {
            int next = bytes[offset[0]++] & 255;
            value |= (long) (next & 127) << shift;
            if ((next & 128) == 0) {
                return value;
            }
            shift += 7;
        }
    }

    /** Locate the Index using the independently parsed Footer. / 独立解析流尾以定位索引。 */
    private static int indexOffset(byte[] bytes) {
        long backward = 0;
        for (int index = 0; index < 4; index++) {
            backward |= (long) (bytes[bytes.length - 8 + index] & 255) << (index * 8);
        }
        return bytes.length - 12 - (int) ((backward + 1) * 4);
    }

    /** Return Header, packed-data, padding, Check and Index positions for one Block. / 返回单 Block 的头、压缩数据、填充、校验及索引位置。 */
    private static int[] layout(byte[] bytes) {
        int index = indexOffset(bytes);
        int[] cursor = {index + 1};
        assertEquals(1, vli(bytes, cursor));
        int unpadded = (int) vli(bytes, cursor);
        int header = ((bytes[12] & 255) + 1) * 4;
        int check = Arrays.stream(XZCheck.values()).filter(c -> c.getCode() == (bytes[7] & 255))
                .findFirst().orElseThrow().getSize();
        int packed = unpadded - header - check;
        int padding = -packed & 3;
        return new int[]{header, packed, padding, 12 + header + packed + padding, index};
    }

    /** Create low-memory reference options. / 创建低内存的独立编码选项。 */
    private static LZMA2Options options() throws IOException {
        LZMA2Options options = new LZMA2Options(1);
        options.setDictSize(65536);
        return options;
    }

    /** Create a reference stream, optionally ending Blocks between pieces. / 创建独立数据流，可在数据片段之间结束 Block。 */
    private static byte[] reference(int check, boolean blocks, byte[]... pieces) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (org.tukaani.xz.XZOutputStream output = new org.tukaani.xz.XZOutputStream(bytes, options(), check)) {
            for (byte[] piece : pieces) {
                output.write(piece);
                if (blocks) {
                    output.endBlock();
                } else {
                    output.flush();
                }
            }
        }
        return bytes.toByteArray();
    }

    /** Require identical output from the independent and project decoders. / 要求独立与本项目解码器输出一致。 */
    private static void assertBoth(byte[] packed, byte[] expected) throws IOException {
        try (InputStream input = new org.tukaani.xz.XZInputStream(new ByteArrayInputStream(packed))) {
            assertArrayEquals(expected, input.readAllBytes(), "independent decoder");
        }
        try (InputStream input = new XZInputStream(new ByteArrayInputStream(packed))) {
            assertArrayEquals(expected, input.readAllBytes(), "project decoder");
        }
    }

    /** Encode through the public API. / 通过公开接口编码。 */
    private static byte[] encode(byte[] source, int level, XZCheck check) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (XZOutputStream output = new XZOutputStream(bytes, level, check)) {
            for (int offset = 0; offset < source.length; offset += 1009) {
                output.write(source, offset, Math.min(1009, source.length - offset));
            }
        }
        return bytes.toByteArray();
    }

    /** Reject a malformed container after fully attempting decoding. / 完整尝试解码后拒绝损坏容器。 */
    private static IOException reject(byte[] bytes) {
        return assertThrows(IOException.class, () -> {
            try (InputStream input = new XZInputStream(new ByteArrayInputStream(bytes))) {
                input.readAllBytes();
            }
        });
    }

    /** Decode all standard data Checks from Python/liblzma. / 解码 Python/liblzma 的全部标准数据校验类型。 */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4, 10})
    void readsIndependentChecks(int check) throws Exception {
        assertBoth(resource("python-check-" + check + ".xz"), resource("payload.bin"));
    }

    /** Verify empty Streams and stored noise. / 验证空 Stream 及未压缩噪声分块。 */
    @Test
    void readsIndependentEmptyAndNoise() throws Exception {
        assertBoth(resource("python-empty.xz"), new byte[0]);
        assertBoth(resource("python-noise.xz"), resource("noise.bin"));
    }

    /** Verify fixed independent concatenation and known CRC64/XZ check value. / 验证独立拼接样本及已知 CRC64/XZ 校验值。 */
    @Test
    void independentConcatenationAndCrc64Vector() throws Exception {
        byte[] payload = resource("payload.bin");
        assertBoth(resource("python-concat.xz"), concat(payload, payload, payload, payload));
        byte[] packed = encode("123456789".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 5, XZCheck.CRC64);
        int position = layout(packed)[3];
        assertArrayEquals(new byte[]{(byte) 0xFA, 0x39, 0x19, (byte) 0xDF,
                (byte) 0xBB, (byte) 0xC9, 0x5D, (byte) 0x99}, Arrays.copyOfRange(packed, position, position + 8));
    }

    /** Exercise every encoder level, Check and chunk boundary. / 覆盖每个编码等级、校验及分块边界。 */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void everyLevelAndCheckInteroperate(int level) throws Exception {
        for (XZCheck check : XZCheck.values()) {
            for (byte[] source : new byte[][]{new byte[0], {42}, new byte[65535],
                    new byte[65536], new byte[65537], resource("payload.bin"), resource("noise.bin")}) {
                byte[] packed = encode(source, level, check);
                assertEquals(check.getCode(), packed[7] & 255);
                assertBoth(packed, source);
            }
        }
    }

    /** Decode a reference multi-Block Stream, including zero-output Blocks. / 解码独立的多 Block Stream，包括零输出 Block。 */
    @Test
    void acceptsMultipleBlocksAndChecks() throws Exception {
        for (int check : new int[]{0, 1, 4, 10}) {
            byte[] first = new byte[3000];
            byte[] second = resource("noise.bin");
            byte[] third = resource("payload.bin");
            assertBoth(reference(check, true, first, second, third), concat(first, second, third));
        }
        byte[] emptyBlock = manual(new byte[0], header(0, new byte[]{0x21, 1, 8}), new byte[]{0});
        assertBoth(emptyBlock, new byte[0]);
    }

    /** Assemble a Block Header independently, recomputing its CRC. / 独立拼装 Block 头并重新计算 CRC。 */
    private static byte[] header(int flags, byte[] fields) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(0);
        bytes.write(flags);
        bytes.write(fields);
        while ((bytes.size() & 3) != 0) {
            bytes.write(0);
        }
        byte[] header = Arrays.copyOf(bytes.toByteArray(), bytes.size() + 4);
        header[0] = (byte) (header.length / 4 - 1);
        crc(header, 0, header.length - 4, header.length - 4);
        return header;
    }

    /** Assemble a one-Block NONE-check Stream without using production helpers. / 不使用生产辅助方法拼装单 Block、无数据校验的 Stream。 */
    private static byte[] manual(byte[] payload, byte[] header, byte[] packed) throws IOException {
        byte[] streamHeader = {(byte) 0xFD, '7', 'z', 'X', 'Z', 0, 0, 0, 0, 0, 0, 0};
        crc(streamHeader, 6, 2, 8);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(streamHeader);
        bytes.write(header);
        bytes.write(packed);
        while ((bytes.size() & 3) != 0) {
            bytes.write(0);
        }
        ByteArrayOutputStream index = new ByteArrayOutputStream();
        index.write(0);
        index.write(1);
        vli(index, header.length + packed.length);
        vli(index, payload.length);
        while ((index.size() & 3) != 0) {
            index.write(0);
        }
        byte[] indexBytes = Arrays.copyOf(index.toByteArray(), index.size() + 4);
        crc(indexBytes, 0, indexBytes.length - 4, indexBytes.length - 4);
        bytes.write(indexBytes);
        byte[] footer = new byte[12];
        long backward = indexBytes.length / 4 - 1;
        for (int part = 0; part < 4; part++) {
            footer[4 + part] = (byte) (backward >>> (part * 8));
        }
        footer[10] = 'Y';
        footer[11] = 'Z';
        crc(footer, 4, 6, 0);
        bytes.write(footer);
        return bytes.toByteArray();
    }

    /** Generate a tiny independent stored LZMA2 payload. / 生成小型独立未压缩 LZMA2 数据。 */
    private static byte[] raw(byte[] payload) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(1);
        bytes.write((payload.length - 1) >>> 8);
        bytes.write((payload.length - 1) & 255);
        bytes.write(payload);
        bytes.write(0);
        return bytes.toByteArray();
    }

    /** Verify all combinations of optional Block sizes and reject mismatches. / 验证可选 Block 大小的全部组合并拒绝不匹配值。 */
    @Test
    void validatesOptionalBlockSizes() throws Exception {
        byte[] payload = {11, 22, 33};
        byte[] packed = raw(payload);
        for (int flags : new int[]{0, 0x40, 0x80, 0xC0}) {
            ByteArrayOutputStream fields = new ByteArrayOutputStream();
            if ((flags & 0x40) != 0) {
                vli(fields, packed.length);
            }
            if ((flags & 0x80) != 0) {
                vli(fields, payload.length);
            }
            fields.write(new byte[]{0x21, 1, 8});
            assertBoth(manual(payload, header(flags, fields.toByteArray()), packed), payload);
        }
        for (int wrong : new int[]{0, packed.length - 1, packed.length + 1}) {
            reject(manual(payload, header(0x40, new byte[]{(byte) wrong, 0x21, 1, 8}), packed));
        }
        for (int wrong : new int[]{0, payload.length - 1, payload.length + 1}) {
            reject(manual(payload, header(0x80, new byte[]{(byte) wrong, 0x21, 1, 8}), packed));
        }
    }

    /** Cover valid VLI boundaries plus overlong and noncanonical forms. / 覆盖合法 VLI 边界及超长、非规范形式。 */
    @Test
    void rejectsMalformedVlisWithoutUnboundedAllocation() throws Exception {
        byte[] payload = {1};
        for (byte[] value : new byte[][]{{(byte) 0x80, 0}, {(byte) 0x81, 0},
                {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0}}) {
            reject(manual(payload, header(0x40, concat(value, new byte[]{0x21, 1, 8})), raw(payload)));
        }
        ByteArrayOutputStream fields = new ByteArrayOutputStream();
        vli(fields, Long.MAX_VALUE);
        fields.write(new byte[]{0x21, 1, 8});
        assertTrue(reject(manual(payload, header(0x40, fields.toByteArray()), raw(payload)))
                .getMessage().contains("Block size mismatch"));
    }

    /** Enforce dictionary bounds before allocation, including 4 GiB property. / 在分配前执行字典限制，包括 4 GiB 属性。 */
    @Test
    void validatesDictionaryPropertiesAndLimits() throws Exception {
        byte[] payload = {7};
        for (int property : new int[]{38, 39, 40, 41, 64, 255}) {
            reject(manual(payload, header(0, new byte[]{0x21, 1, (byte) property}), raw(payload)));
        }
        byte[] packed = manual(payload, header(0, new byte[]{0x21, 1, 8}), raw(payload));
        try (InputStream input = new XZInputStream(new ByteArrayInputStream(packed), 65535, true)) {
            assertThrows(IOException.class, input::readAllBytes);
        }
        try (InputStream input = new XZInputStream(new ByteArrayInputStream(packed), 65536, true)) {
            assertArrayEquals(payload, input.readAllBytes());
        }
        assertThrows(IllegalArgumentException.class,
                () -> new XZInputStream(new ByteArrayInputStream(packed), 4095, true));
        assertThrows(IllegalArgumentException.class, () -> new XZOutputStream(new ByteArrayOutputStream(), 10));
    }

    /** Reject valid-but-unsupported Delta chains and malformed filter descriptors. / 拒绝合法但未支持的 Delta 链及非法过滤器描述。 */
    @Test
    void rejectsUnsupportedFiltersChecksAndReservedFlags() throws Exception {
        ByteArrayOutputStream reference = new ByteArrayOutputStream();
        try (OutputStream output = new org.tukaani.xz.XZOutputStream(reference,
                new FilterOptions[]{new DeltaOptions(1), options()})) {
            output.write(new byte[1000]);
        }
        reject(reference.toByteArray());
        for (byte[] descriptor : new byte[][]{{0x22, 1, 8}, {0x21, 0}, {0x21, 2, 8, 0}}) {
            reject(manual(new byte[]{1}, header(0, descriptor), raw(new byte[]{1})));
        }
        for (int flag : new int[]{1, 4, 0x3C}) {
            reject(manual(new byte[]{1}, header(flag, new byte[]{0x21, 1, 8}), raw(new byte[]{1})));
        }
        byte[] valid = resource("python-empty.xz");
        for (int check : new int[]{2, 3, 15, 0x84}) {
            byte[] invalid = valid.clone();
            invalid[7] = (byte) check;
            crc(invalid, 6, 2, 8);
            reject(invalid);
        }
        valid[6] = 1;
        crc(valid, 6, 2, 8);
        reject(valid);
    }

    /** Verify container checks even when a corrupted Index gets a repaired CRC. / 即使损坏索引的 CRC 被重算，也验证其实际内容。 */
    @Test
    void rejectsRepairedCrcIndexMismatchAndReorderedRecords() throws Exception {
        byte[] valid = reference(4, true, new byte[30], new byte[50]);
        int index = indexOffset(valid);
        int[] cursor = {index + 1};
        assertEquals(2, vli(valid, cursor));
        vli(valid, cursor);
        int first = cursor[0];
        assertEquals(30, vli(valid, cursor));
        vli(valid, cursor);
        int second = cursor[0];
        assertEquals(50, vli(valid, cursor));
        valid[first] = 50;
        valid[second] = 30;
        crc(valid, index, valid.length - 12 - index - 4, valid.length - 16);
        assertTrue(reject(valid).getMessage().contains("Index does not match"));
        valid = resource("python-check-4.xz");
        index = indexOffset(valid);
        valid[index + 1]++;
        reject(valid);
    }

    /** Reject each structural CRC, invalid padding, data corruption and Footer mismatch. / 拒绝各类结构 CRC、填充、数据损坏和流尾不匹配。 */
    @Test
    void validatesEveryContainerLayer() throws Exception {
        byte[] valid = reference(4, false, new byte[]{1, 2});
        int[] fields = layout(valid);
        assertTrue(fields[2] > 0);
        int footer = valid.length - 12;
        for (int offset : new int[]{0, 8, 12 + fields[0] - 1, 12 + fields[0],
                12 + fields[0] + fields[1], fields[3], valid.length - 16, footer, valid.length - 1}) {
            byte[] invalid = valid.clone();
            invalid[offset] ^= 1;
            reject(invalid);
        }
        byte[] invalid = valid.clone();
        invalid[footer + 4]++;
        crc(invalid, footer + 4, 6, footer);
        reject(invalid);
        invalid = valid.clone();
        invalid[footer + 9] = 1;
        crc(invalid, footer + 4, 6, footer);
        reject(invalid);
        invalid = valid.clone();
        invalid[12 + 5] = 1;
        crc(invalid, 12, fields[0] - 4, 12 + fields[0] - 4);
        reject(invalid);
        for (int length = 0; length < valid.length; length++) {
            reject(Arrays.copyOf(valid, length));
        }
    }

    /** Default mode reads concatenation; embedded mode leaves all trailing bytes untouched. / 默认读取拼接，嵌入模式完整保留后续字节。 */
    @Test
    void concatenationPaddingAndSingleStreamBoundaries() throws Exception {
        byte[] first = reference(1, false, new byte[]{1, 2, 3});
        byte[] empty = resource("python-empty.xz");
        byte[] second = reference(10, false, new byte[]{4, 5});
        assertBoth(concat(first, new byte[8], empty, second, new byte[4]), new byte[]{1, 2, 3, 4, 5});
        for (byte[] tail : new byte[][]{{0}, {0, 0}, {0, 0, 0}, {99}, {0, 0, 0, 0, 99}}) {
            reject(concat(first, tail));
        }
        ByteArrayInputStream source = new ByteArrayInputStream(concat(first, new byte[]{0, 0, 0, 0, 91}));
        try (InputStream input = new XZInputStream(source, false)) {
            assertArrayEquals(new byte[]{1, 2, 3}, input.readAllBytes());
            assertEquals(5, source.available());
            assertEquals(0, source.read());
        }
    }

    /** Validate lifecycle, zero-length requests, skip and early close without draining. / 验证生命周期、零长度请求、skip 及不读取剩余数据的提前关闭。 */
    @Test
    void inputOwnershipBoundsAndFailures() throws Exception {
        class Source extends ByteArrayInputStream {
            int closes;
            Source(byte[] bytes) {
                super(bytes);
            }
            @Override
            public void close() {
                this.closes++;
            }
        }
        byte[] packed = resource("python-check-4.xz");
        Source source = new Source(packed);
        XZInputStream input = new XZInputStream(source);
        assertEquals(packed.length, source.available());
        assertEquals(0, input.read(new byte[0]));
        assertThrows(IndexOutOfBoundsException.class, () -> input.read(new byte[1], -1, 1));
        assertEquals(1000, input.skip(1000));
        assertEquals(resource("payload.bin").length - 1000, input.readAllBytes().length);
        input.close();
        input.close();
        assertEquals(1, source.closes);
        assertThrows(IOException.class, input::read);
        Source early = new Source(packed);
        new XZInputStream(early).close();
        assertEquals(packed.length, early.available());
        assertEquals(1, early.closes);
        XZInputStream broken = new XZInputStream(new ByteArrayInputStream(new byte[]{1}));
        IOException error = assertThrows(IOException.class, broken::read);
        assertSame(error, assertThrows(IOException.class, broken::read));
        broken.close();
    }

    /** Complete pending chunks during flush while retaining the Block and parent. / flush 完成待处理分块，同时保留 Block 及底层流。 */
    @Test
    void outputFlushFinishAndCloseOwnership() throws Exception {
        class Destination extends ByteArrayOutputStream {
            int closes;
            @Override
            public void close() {
                this.closes++;
            }
        }
        Destination parent = new Destination();
        XZOutputStream output = new XZOutputStream(parent, 5);
        assertEquals(12, parent.size());
        output.write(new byte[1000]);
        output.flush();
        try (InputStream input = new org.tukaani.xz.XZInputStream(new ByteArrayInputStream(parent.toByteArray()))) {
            assertArrayEquals(new byte[1000], input.readNBytes(1000));
        }
        output.write(99);
        output.finish();
        int size = parent.size();
        output.finish();
        output.flush();
        assertEquals(size, parent.size());
        assertEquals(0, parent.closes);
        assertBoth(parent.toByteArray(), concat(new byte[1000], new byte[]{99}));
        assertThrows(IOException.class, () -> output.write(1));
        output.close();
        output.close();
        assertEquals(1, parent.closes);
    }

    /** Preserve failure identity and suppress secondary closure errors. / 保留失败对象，并保留次要关闭异常。 */
    @Test
    void delegateFailuresAreStickyAndCloseErrorsAreSuppressed() throws Exception {
        class Destination extends OutputStream {
            boolean fail;
            int closes;
            @Override
            public void write(int value) throws IOException {
                if (this.fail) {
                    throw new IOException("write");
                }
            }
            @Override
            public void close() throws IOException {
                this.closes++;
                throw new IOException("close");
            }
        }
        Destination parent = new Destination();
        XZOutputStream output = new XZOutputStream(parent, 5);
        output.write(1);
        parent.fail = true;
        IOException error = assertThrows(IOException.class, output::close);
        assertEquals("write", error.getMessage());
        assertEquals("close", error.getSuppressed()[0].getMessage());
        assertEquals(1, parent.closes);
        output.close();
        Destination another = new Destination();
        XZOutputStream broken = new XZOutputStream(another, 5);
        another.fail = true;
        IOException first = assertThrows(IOException.class, () -> broken.write(1));
        assertSame(first, assertThrows(IOException.class, () -> broken.write(2)));
        assertSame(first, assertThrows(IOException.class, broken::finish));
        assertThrows(IOException.class, broken::close);
    }

    /** Zero-progress delegates must not cause loops or byte loss. / 无进展底层读取不得造成循环或字节丢失。 */
    @Test
    void zeroProgressBulkReadsAndSingleByteAccess() throws Exception {
        byte[] expected = new byte[70000];
        new Random(1234).nextBytes(expected);
        byte[] packed = reference(4, false, expected);
        InputStream source = new ByteArrayInputStream(packed) {
            @Override
            public int read(byte[] bytes, int offset, int length) {
                return 0;
            }
        };
        try (XZInputStream input = new XZInputStream(source)) {
            for (byte value : expected) {
                assertEquals(value & 255, input.read());
            }
            assertEquals(-1, input.read());
        }
    }
}
