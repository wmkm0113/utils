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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.xz.XZZipTestSupport.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/**
 * ZIP method 95 interoperability, encryption and malformed-entry regression tests.
 * ZIP 方法 95 的互操作、加密和畸形条目回归测试。
 */
final class XZZipTest {

    @TempDir
    Path directory;

    private static Stream<Arguments> configurations() {
        return Stream.of(Layout.values()).flatMap(layout -> Stream.of(Encryption.values())
                .flatMap(encryption -> Stream.of(Zip64Mode.AUTO, Zip64Mode.ALWAYS)
                        .flatMap(mode -> Stream.of(false, true)
                                .map(central -> Arguments.of(layout, encryption, mode, central)))));
    }

    /** Exercise public entry APIs across encryption and volume layouts. / 覆盖加密、分卷和 ZIP64 组合。 */
    @ParameterizedTest(name = "{0}, {1}, {2}, central={3}")
    @MethodSource("configurations")
    void roundTripsEncryptionZip64SplittingAndCentralCompression(final Layout layout,
            final Encryption encryption, final Zip64Mode mode, final boolean central) throws Exception {
        Path path = this.directory.resolve("matrix.zip");
        ZipOptions.Builder builder = ZipOptions.newBuilder(
                options(CompressionAlgorithm.LZMA2, mode, layout, encryption)).compressionLevel(2);
        if (central) {
            builder.enableCentralDirectoryCompress()
                    .centralDirectoryHash(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
        }
        ZipOptions options = builder.build();
        byte[] expected = payload(120017);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "payload.bin", expected);
            writeEntry(archive, "empty.bin", new byte[0]);
            writeEntry(archive, "folder/", new byte[0]);
        }
        assertContent(readablePath(path, layout), options,
                Map.of("payload.bin", expected, "empty.bin", new byte[0]));
    }

    /** An independent ZIP/XZ decoder must accept the produced stream, including an empty entry. */
    /** 独立 ZIP/XZ 解码器必须能够读取生成的数据，包括空文件。 */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 9})
    void writesCompleteXzContainersReadableByIndependentDecoder(final int level) throws Exception {
        Path path = this.directory.resolve("external.zip");
        ZipOptions options = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.LZMA2)
                .compressionLevel(level).build();
        byte[] expected = new byte[140017];
        System.arraycopy(payload(70017), 0, expected, 0, 70017);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "payload.bin", expected);
            writeEntry(archive, "empty.bin", new byte[0]);
            writeEntry(archive, "folder/", new byte[0]);
        }
        try (var external = org.apache.commons.compress.archivers.zip.ZipFile.builder().setPath(path).get()) {
            for (String name : new String[]{"payload.bin", "empty.bin"}) {
                var entry = external.getEntry(name);
                assertNotNull(entry);
                assertEquals(95, entry.getMethod());
                byte[] wanted = name.equals("payload.bin") ? expected : new byte[0];
                try (InputStream input = external.getInputStream(entry)) {
                    assertArrayEquals(wanted, input.readAllBytes());
                }
                try (InputStream input = external.getRawInputStream(entry)) {
                    byte[] raw = input.readAllBytes();
                    assertArrayEquals(new byte[]{(byte) 0xFD, 0x37, 0x7A, 0x58, 0x5A, 0},
                            Arrays.copyOf(raw, 6));
                    assertEquals('Y', raw[raw.length - 2]);
                    assertEquals('Z', raw[raw.length - 1]);
                    assertArrayEquals(wanted, independentlyDecode(raw));
                }
            }
            var folder = external.getEntry("folder/");
            assertTrue(folder.isDirectory());
            assertEquals(0, folder.getMethod());
            assertEquals(0, folder.getCompressedSize());
        }
    }

    /** XZ does not use method 14's EOS flag; AES retains 95 in its extra field. */
    /** XZ 不使用方法 14 的 EOS 标志；AES 扩展字段应保留真实方法 95。 */
    @ParameterizedTest
    @EnumSource(Encryption.class)
    void preservesMethodFlagsAndEncryptionVersion(final Encryption encryption) throws Exception {
        Path path = this.directory.resolve("headers.zip");
        ZipOptions options = options(CompressionAlgorithm.LZMA2, Zip64Mode.AUTO, Layout.SINGLE, encryption);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "payload.bin", payload(31));
        }
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer raw = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int central = raw.getInt(bytes.length - 6);
        assertEquals(0x02014B50, raw.getInt(central));
        int version = encryption == Encryption.AES ? 51 : encryption == Encryption.STRONG ? 63 : 20;
        assertEquals(version, Short.toUnsignedInt(raw.getShort(4)));
        assertEquals(version, Short.toUnsignedInt(raw.getShort(central + 6)));
        assertEquals(0, raw.getShort(6) & 0x0002);
        assertEquals(0, raw.getShort(central + 8) & 0x0002);
        int method = encryption == Encryption.AES ? 99 : 95;
        assertEquals(method, Short.toUnsignedInt(raw.getShort(8)));
        assertEquals(method, Short.toUnsignedInt(raw.getShort(central + 10)));
        if (encryption == Encryption.AES) {
            int localExtra = 30 + Short.toUnsignedInt(raw.getShort(26));
            byte[] aes = extra(bytes, localExtra, Short.toUnsignedInt(raw.getShort(28)), 0x9901);
            assertEquals(95, Short.toUnsignedInt(ByteBuffer.wrap(aes).order(ByteOrder.LITTLE_ENDIAN).getShort(5)));
            int centralExtra = central + 46 + Short.toUnsignedInt(raw.getShort(central + 28));
            aes = extra(bytes, centralExtra, Short.toUnsignedInt(raw.getShort(central + 30)), 0x9901);
            assertEquals(95, Short.toUnsignedInt(ByteBuffer.wrap(aes).order(ByteOrder.LITTLE_ENDIAN).getShort(5)));
        }
    }

    /** Flush must permit more data, and closing one entry must permit another entry. */
    /** flush 后可以继续写入，关闭当前条目后可以继续创建下一条目。 */
    @Test
    void flushAndEntryCompletionKeepArchiveWritable() throws Exception {
        Path path = this.directory.resolve("flush.zip");
        ZipOptions options = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.LZMA2).build();
        byte[] expected = payload(131073);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            try (ResourceWriter writer = archive.openWriter("payload.bin", "").orElseThrow()) {
                try (OutputStream output = writer.openStream(0L)) {
                    output.write(expected, 0, 65536);
                    output.flush();
                    output.write(expected, 65536, expected.length - 65536);
                }
                writer.finish();
            }
            writeEntry(archive, "next.bin", new byte[]{1, 2, 3});
        }
        assertContent(path, options, Map.of("payload.bin", expected, "next.bin", new byte[]{1, 2, 3}));
    }

    private static Stream<Arguments> independentConfigurations() {
        return Stream.of(0, 1, 4, 10).flatMap(check -> Stream.of(false, true)
                .map(descriptor -> Arguments.of(check, descriptor)));
    }

    /** Read independent fixtures with every supported Check and optional data descriptors. */
    /** 读取全部校验类型及可选数据描述符的独立样本。 */
    @ParameterizedTest(name = "check={0}, descriptor={1}")
    @MethodSource("independentConfigurations")
    void readsIndependentMethod95Zip(final int check, final boolean descriptor) throws Exception {
        byte[] expected = payload(120017);
        Path path = this.directory.resolve("independent.zip");
        Files.write(path, entryFixture(expected, compress(expected, check), descriptor, false, 0));
        assertContent(path, ZipOptions.newBuilder().build(), Map.of("payload.bin", expected));
    }

    /** A valid XZ Check cannot substitute for the outer ZIP CRC32. / XZ 校验不能替代 ZIP CRC32。 */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsIncorrectZipCrc(final boolean descriptor) throws Exception {
        byte[] expected = payload(1017);
        Path path = this.directory.resolve("bad-crc.zip");
        Files.write(path, entryFixture(expected, compress(expected, 4), descriptor, true, 0));
        assertThrows(IOException.class, () -> assertContent(path, ZipOptions.newBuilder().build(),
                Map.of("payload.bin", expected)));
    }

    /** A truncated XZ Footer must fail even when all plaintext bytes were produced. */
    /** 即使已经输出全部原始数据，XZ Footer 截断仍然必须报错。 */
    @ParameterizedTest
    @ValueSource(ints = {1, 4, 12, 25})
    void rejectsTruncatedXzContainer(final int removed) throws Exception {
        byte[] expected = payload(1017);
        byte[] compressed = compress(expected, 4);
        Path path = this.directory.resolve("truncated.zip");
        Files.write(path, entryFixture(expected, Arrays.copyOf(compressed, compressed.length - removed),
                false, false, 0));
        assertThrows(IOException.class, () -> assertContent(path, ZipOptions.newBuilder().build(),
                Map.of("payload.bin", expected)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"header", "check", "footer"})
    void rejectsCorruptedXzStructure(final String part) throws Exception {
        byte[] expected = payload(1017);
        byte[] compressed = compress(expected, 4);
        if (part.equals("check")) {
            compressed = corruptCheck(compressed);
        } else {
            compressed[part.equals("header") ? 8 : compressed.length - 1] ^= 1;
        }
        Path path = this.directory.resolve("corrupt.zip");
        Files.write(path, entryFixture(expected, compressed, false, false, 0));
        assertThrows(IOException.class, () -> assertContent(path, ZipOptions.newBuilder().build(),
                Map.of("payload.bin", expected)));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void rejectsIncorrectUncompressedSize(final int delta) throws Exception {
        byte[] expected = payload(1017);
        Path path = this.directory.resolve("size.zip");
        Files.write(path, entryFixture(expected, compress(expected, 4), false, false, delta));
        assertThrows(IOException.class, () -> assertContent(path, ZipOptions.newBuilder().build(),
                Map.of("payload.bin", expected)));
    }

    @ParameterizedTest
    @EnumSource(value = Encryption.class, names = {"STANDARD", "AES", "STRONG"})
    void rejectsIncorrectEntryPassword(final Encryption encryption) throws Exception {
        Path path = this.directory.resolve("password.zip");
        ZipOptions options = options(CompressionAlgorithm.LZMA2, Zip64Mode.AUTO, Layout.SINGLE, encryption);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "payload.bin", payload(1017));
        }
        ArchiveOptions wrong = ArchiveOptions.newBuilder()
                .passwordProvider(PasswordProvider.fixed("incorrect".toCharArray())).build();
        assertThrows(IOException.class, () -> {
            try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"), wrong, options);
                 ResourceReader reader = archive.openReader(archive.entry("payload.bin").orElseThrow()).orElseThrow();
                 InputStream input = reader.openStream()) {
                input.readAllBytes();
            }
        });
    }

    @Test
    void appliesDeclaredEntrySizeLimit() throws Exception {
        byte[] expected = payload(1017);
        Path path = this.directory.resolve("limited.zip");
        Files.write(path, entryFixture(expected, compress(expected, 4), false, false, 0));
        ArchiveOptions limited = ArchiveOptions.newBuilder().limitBuilder().maxEntryBytes(32).confirm().build();
        assertThrows(IOException.class, () -> {
            try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"), limited,
                    ZipOptions.newBuilder().build())) {
                archive.entry("payload.bin");
            }
        });
    }
}
