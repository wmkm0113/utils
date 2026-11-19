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

import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.central.CentralDirectory;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.operator.ZipFileOperator;
import org.tukaani.xz.LZMA2Options;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.archiveOptions;

/**
 * Independent encoders and ZIP envelopes for XZ integration tests.
 * XZ 集成测试使用的独立编码器和 ZIP 封装构造工具。
 */
final class XZZipTestSupport {

    private XZZipTestSupport() {
    }

    /** Encode with Tukaani rather than the implementation under test. / 使用 Tukaani 独立编码。 */
    static byte[] compress(final byte[] plain, final int check) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LZMA2Options options = new LZMA2Options(1);
        options.setDictSize(65536);
        try (OutputStream output = new org.tukaani.xz.XZOutputStream(bytes, options, check)) {
            output.write(plain);
        }
        return bytes.toByteArray();
    }

    /** Build a method-95 ZIP without using the production writer. / 独立构造方法 95 的 ZIP。 */
    static byte[] entryFixture(final byte[] original, final byte[] compressed,
                              final boolean descriptor, final boolean badCrc, final int sizeDelta)
            throws Exception {
        CRC32 crc32 = new CRC32();
        crc32.update(original);
        int crc = (int) crc32.getValue() ^ (badCrc ? 1 : 0);
        byte[] name = "payload.bin".getBytes(StandardCharsets.UTF_8);
        int flags = 0x0800 | (descriptor ? 0x0008 : 0);
        int declaredSize = original.length + sizeDelta;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ByteBuffer local = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
        local.putInt(0x04034B50).putShort((short) 20).putShort((short) flags).putShort((short) 95)
                .putShort((short) 0).putShort((short) 0).putInt(descriptor ? 0 : crc)
                .putInt(descriptor ? 0 : compressed.length).putInt(descriptor ? 0 : declaredSize)
                .putShort((short) name.length).putShort((short) 0);
        bytes.write(local.array());
        bytes.write(name);
        bytes.write(compressed);
        if (descriptor) {
            bytes.write(ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(0x08074B50).putInt(crc).putInt(compressed.length).putInt(declaredSize).array());
        }
        int centralStart = bytes.size();
        ByteBuffer central = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN);
        central.putInt(0x02014B50).putShort((short) 20).putShort((short) 20).putShort((short) flags)
                .putShort((short) 95).putShort((short) 0).putShort((short) 0).putInt(crc)
                .putInt(compressed.length).putInt(declaredSize).putShort((short) name.length)
                .putShort((short) 0).putShort((short) 0).putShort((short) 0).putShort((short) 0)
                .putInt(0).putInt(0);
        bytes.write(central.array());
        bytes.write(name);
        int centralSize = bytes.size() - centralStart;
        bytes.write(ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN).putInt(0x06054B50)
                .putShort((short) 0).putShort((short) 0).putShort((short) 1).putShort((short) 1)
                .putInt(centralSize).putInt(centralStart).putShort((short) 0).array());
        return bytes.toByteArray();
    }

    /** Central directory containing one empty stored entry. / 包含一个空条目的中央目录。 */
    static byte[] plainCentral() {
        byte[] name = "entry.txt".getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(46 + name.length + 256).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0x02014B50).putShort((short) 20).putShort((short) 20).putShort((short) 0)
                .putShort((short) 0).putShort((short) 0).putShort((short) 0).putInt(0).putInt(0).putInt(0)
                .putShort((short) name.length).putShort((short) 0).putShort((short) 256).putShort((short) 0)
                .putShort((short) 0).putInt(0).putInt(0).put(name).put(new byte[256]).array();
    }

    /** Build ZIP64 V2 metadata without the production ZIP writer. / 独立构造 ZIP64 V2 元数据。 */
    static byte[] centralFixture(final byte[] compressed, final int originalLength) {
        ByteBuffer bytes = ByteBuffer.allocate(compressed.length + 84 + 20 + 22)
                .order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(compressed).putInt(0x06064B50).putLong(72).putShort((short) 62).putShort((short) 62)
                .putInt(0).putInt(0).putLong(0).putLong(1).putLong(compressed.length).putLong(0)
                .putShort((short) 95).putLong(compressed.length).putLong(originalLength)
                .putShort((short) 0).putShort((short) 0).putShort((short) 0).putShort((short) 0).putShort((short) 0)
                .putInt(0x07064B50).putInt(0).putLong(compressed.length).putInt(1)
                .putInt(0x06054B50).putShort((short) 0).putShort((short) 0).putShort((short) 0)
                .putShort((short) 0xFFFF).putInt(0xFFFFFFFF).putInt(0xFFFFFFFF).putShort((short) 0);
        return bytes.array();
    }

    static ZipFileOperator operator(final Path path, final ZipOptions options) throws Exception {
        return new ZipFileOperator(new LocalDataFile(path.toString(), "r"), archiveOptions(), options);
    }

    static Zip64EndCentralDirectory.Protection protection(final Path path, final ZipOptions options)
            throws Exception {
        try (ZipFileOperator operator = operator(path, options)) {
            return operator.readZip64EndCentralDirectory(operator.readEndCentralDirectoryRecord()).getProtection();
        }
    }

    static CentralDirectory readCentral(final Path path, final ArchiveOptions options) throws Exception {
        try (ZipFileOperator operator = new ZipFileOperator(new LocalDataFile(path.toString(), "r"),
                options, ZipOptions.newBuilder().build())) {
            var end = operator.readEndCentralDirectoryRecord();
            return operator.readCentralDirectory(end, operator.readZip64EndCentralDirectory(end));
        }
    }

    /** Locate an extra field within an explicitly bounded header area. / 在指定头部范围内查找扩展字段。 */
    static byte[] extra(final byte[] bytes, final int start, final int length, final int identifier) {
        ByteBuffer raw = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int end = start + length;
        for (int offset = start; offset + 4 <= end;) {
            int id = Short.toUnsignedInt(raw.getShort(offset));
            int size = Short.toUnsignedInt(raw.getShort(offset + 2));
            if (offset + 4 + size > end) {
                fail("Extra field exceeds header boundary");
            }
            if (id == identifier) {
                return Arrays.copyOfRange(bytes, offset + 4, offset + 4 + size);
            }
            offset += 4 + size;
        }
        return fail("Missing extra field: " + identifier);
    }

    /** Corrupt the final Block Check, preserving Index and Footer. / 破坏最后一个 Block 的校验值。 */
    static byte[] corruptCheck(final byte[] compressed) {
        byte[] bad = compressed.clone();
        ByteBuffer raw = ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN);
        long indexSize = (Integer.toUnsignedLong(raw.getInt(bad.length - 8)) + 1L) * 4L;
        int indexStart = Math.toIntExact(bad.length - 12L - indexSize);
        bad[indexStart - 1] ^= 1;
        return bad;
    }

    /** Verify the independent decoder consumes a complete XZ container. / 使用独立解码器完整读取容器。 */
    static byte[] independentlyDecode(final byte[] compressed) throws Exception {
        try (InputStream input = new org.tukaani.xz.XZInputStream(new java.io.ByteArrayInputStream(compressed))) {
            byte[] plain = input.readAllBytes();
            assertEquals(-1, input.read());
            return plain;
        }
    }
}
