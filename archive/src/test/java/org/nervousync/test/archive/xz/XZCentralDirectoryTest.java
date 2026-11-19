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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.resource.ZipEntryResourceReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.xz.XZZipTestSupport.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/**
 * XZ-compressed central directory sizes, encryption, split merges and malformed metadata.
 * XZ 压缩中央目录的长度、加密、分卷合并和畸形元数据测试。
 */
final class XZCentralDirectoryTest {

    @TempDir
    Path directory;

    private static ZipOptions optionsFor(final Layout layout, final Zip64Mode mode,
                                         final int encryption, final int hash) throws Exception {
        ZipOptions.Builder builder = ZipOptions.newBuilder(
                options(CompressionAlgorithm.LZMA2, mode, layout, Encryption.NONE))
                .enableCentralDirectoryCompress();
        if (encryption != 0) {
            builder.centralDirectoryCrypto(encryption,
                    encryption == ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 ? 256 : 168);
        }
        return builder.centralDirectoryHash(hash).build();
    }

    private static Stream<Arguments> configurations() {
        return Stream.of(Layout.values()).flatMap(layout -> Stream.of(Zip64Mode.AUTO, Zip64Mode.ALWAYS)
                .flatMap(mode -> Stream.of(0, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256,
                                ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168)
                        .flatMap(encryption -> Stream.of(0, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32,
                                        ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256)
                                .map(hash -> Arguments.of(layout, mode, encryption, hash)))));
    }

    /* Decode the recorded compressed extent independently and verify its plaintext CRC. */
    /* 独立解码记录的压缩区间，并验证原始中央目录的 CRC。 */
    @Test
    void writesCompleteContainerAndAccurateSizes() throws Exception {
        Path path = this.directory.resolve("sizes.zip");
        ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO, 0,
                ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "value", new byte[]{1, 2, 3});
        }
        try (var operator = operator(path, options)) {
            var end = operator.readEndCentralDirectoryRecord();
            var zip64 = operator.readZip64EndCentralDirectory(end);
            if (zip64 != null) {
                var protection = zip64.getProtection();
                assertEquals(95, protection.getCompressionMethod());
                assertTrue(zip64.getRecord().getExtractNeeded() >= 62);
                assertEquals(protection.getCompressedSize(), zip64.getRecord().getCentralDirectorySize());
                byte[] file = Files.readAllBytes(path);
                int offset = Math.toIntExact(zip64.getRecord().getCentralDirectoryOffset());
                byte[] compressed = Arrays.copyOfRange(file, offset,
                        offset + Math.toIntExact(protection.getCompressedSize()));
                byte[] central = independentlyDecode(compressed);
                assertEquals(protection.getUncompressedSize(), central.length);
                assertEquals(0x02014B50, ByteBuffer.wrap(central).order(ByteOrder.LITTLE_ENDIAN).getInt());
                CRC32 crc = new CRC32();
                crc.update(central);
                assertArrayEquals(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt((int) crc.getValue()).array(), protection.getHashData());
                assertNotNull(operator.readCentralDirectory(end, zip64).header("value"));
            }
        }
    }

    @ParameterizedTest(name = "{0} {1}, crypto={2}, hash={3}")
    @MethodSource("configurations")
    void roundTripsProtectionCombinations(final Layout layout, final Zip64Mode mode,
                                          final int encryption, final int hash) throws Exception {
        Path path = this.directory.resolve("protected.zip");
        ZipOptions options = optionsFor(layout, mode, encryption, hash);
        byte[] expected = payload(120017);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "payload", expected);
            writeEntry(archive, "empty", new byte[0]);
        }
        Path readable = readablePath(path, layout);
        assertContent(readable, options, Map.of("payload", expected, "empty", new byte[0]));
        var protection = protection(readable, options);
        assertEquals(95, protection.getCompressionMethod());
        assertEquals(encryption, protection.getAlgorithm());
        assertEquals(hash, protection.getHashAlgorithm());
    }

    /* Ensure the central directory spans both LZMA2 chunks and physical volumes. */
    /* 确保中央目录同时跨越 LZMA2 数据块和物理分卷边界。 */
    @ParameterizedTest
    @ValueSource(ints = {0, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256})
    void supportsLargeCentralDirectoryAcrossNativeVolumes(final int encryption) throws Exception {
        Path path = this.directory.resolve("large.zip");
        ZipOptions options = optionsFor(Layout.NATIVE, Zip64Mode.AUTO, encryption,
                ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
        String firstComment = Base64.getEncoder().encodeToString(payload(48000));
        byte[] other = new byte[48000];
        new Random(43L).nextBytes(other);
        String secondComment = Base64.getEncoder().encodeToString(other);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "first", new byte[]{1}, firstComment);
            writeEntry(archive, "second", new byte[]{2}, secondComment);
        }
        assertTrue(Files.exists(this.directory.resolve("large.z01")));
        assertTrue(protection(path, options).getCompressedSize() > 65536);
        assertContent(path, options, Map.of("first", new byte[]{1}, "second", new byte[]{2}));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256,
            ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_3DES_168})
    void supportsEmptyCentralDirectory(final int encryption) throws Exception {
        Path path = this.directory.resolve("empty.zip");
        ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO, encryption,
                ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_CRC32);
        try (ArchiveFile ignored =
                     ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
            // Closing writes an empty but complete XZ Stream.
            // 关闭时写入完整的空 XZ Stream。
        }
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"),
                archiveOptions(), options)) {
            assertTrue(archive.entry("missing").isEmpty());
        }
        var protection = protection(path, options);
        assertEquals(95, protection.getCompressionMethod());
        assertEquals(0, protection.getUncompressedSize());
        assertEquals(32, protection.getCompressedSize());
    }

    private static Stream<Arguments> mergeConfigurations() {
        return Stream.of(Layout.NATIVE, Layout.NUMBERED).flatMap(layout -> Stream.of(false, true)
                .map(encrypted -> Arguments.of(layout, encrypted)));
    }

    /* Restore the central compression from source metadata, preserving raw entry bytes. */
    /* 从源元数据恢复中央目录压缩算法，同时原样保留条目压缩数据。 */
    @ParameterizedTest(name = "{0}, central encryption={1}")
    @MethodSource("mergeConfigurations")
    void mergePreservesCompressionWithoutSourceWriteOptions(final Layout layout, final boolean encrypted)
            throws Exception {
        ZipOptions options = optionsFor(layout, Zip64Mode.AUTO,
                encrypted ? ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 : 0,
                ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
        Path source = this.directory.resolve("split.zip");
        Path merged = this.directory.resolve("merged.zip");
        byte[] expected = payload(120017);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(source.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "payload", expected);
        }
        ZipOptions reading = options(CompressionAlgorithm.DEFLATE, Zip64Mode.AUTO, layout, Encryption.NONE);
        byte[] originalRaw;
        try (ArchiveFile archive = ArchiveFile.zipFile(
                new LocalDataFile(readablePath(source, layout).toString(), "r"), archiveOptions(), reading)) {
            try (ResourceReader reader = archive.openReader(archive.entry("payload").orElseThrow()).orElseThrow();
                 InputStream raw = ((ZipEntryResourceReader) reader).sourceStream()) {
                originalRaw = raw.readAllBytes();
            }
            archive.merge(merged);
        }
        ZipOptions defaults = ZipOptions.newBuilder().build();
        assertContent(merged, defaults, Map.of("payload", expected));
        var actual = protection(merged, defaults);
        assertEquals(95, actual.getCompressionMethod());
        assertEquals(encrypted ? ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 : 0, actual.getAlgorithm());
        assertEquals(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256, actual.getHashAlgorithm());
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(merged.toString(), "r"),
                archiveOptions(), defaults);
             ResourceReader reader = archive.openReader(archive.entry("payload").orElseThrow()).orElseThrow();
             InputStream raw = ((ZipEntryResourceReader) reader).sourceStream()) {
            assertArrayEquals(originalRaw, raw.readAllBytes());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4, 10})
    void readsIndependentCompressedCentralDirectory(final int check) throws Exception {
        byte[] plain = plainCentral();
        Path path = this.directory.resolve("independent.zip");
        Files.write(path, centralFixture(compress(plain, check), plain.length));
        assertNotNull(readCentral(path, ArchiveOptions.newBuilder().build()).header("entry.txt"));
    }

    @Test
    void rewritesXzDirectoryAfterEditingEntries() throws Exception {
        Path path = this.directory.resolve("edit.zip");
        ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO, 0,
                ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
        byte[] kept = payload(1001);
        byte[] added = payload(2017);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "keep", kept);
            writeEntry(archive, "remove", new byte[]{7});
        }
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            archive.remove(archive.entry("remove").orElseThrow());
            writeEntry(archive, "add", added);
        }
        assertContent(path, options, Map.of("keep", kept, "add", added));
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"),
                archiveOptions(), options)) {
            assertTrue(archive.entry("remove").isEmpty());
        }
        assertEquals(95, protection(path, options).getCompressionMethod());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 8193})
    void rejectsBufferedAndUnbufferedTrailingBytes(final int suffixLength) throws Exception {
        byte[] plain = plainCentral();
        byte[] compressed = compress(plain, 4);
        byte[] extra = Arrays.copyOf(compressed, compressed.length + suffixLength);
        extra[extra.length - 1] = 42;
        Path path = this.directory.resolve("trailing.zip");
        Files.write(path, centralFixture(extra, plain.length));
        assertThrows(IOException.class, () -> readCentral(path, ArchiveOptions.newBuilder().build()));
    }

    @Test
    void rejectsConcatenationCorruptionTruncationAndIncorrectSize() throws Exception {
        byte[] plain = plainCentral();
        byte[] compressed = compress(plain, 4);
        byte[] empty = compress(new byte[0], 4);
        byte[] concatenated = Arrays.copyOf(compressed, compressed.length + empty.length);
        System.arraycopy(empty, 0, concatenated, compressed.length, empty.length);
        byte[] badFooter = compressed.clone();
        badFooter[badFooter.length - 1] ^= 1;
        for (byte[] bad : new byte[][]{concatenated, corruptCheck(compressed), badFooter,
                Arrays.copyOf(compressed, compressed.length - 1)}) {
            Path path = this.directory.resolve("malformed.zip");
            Files.write(path, centralFixture(bad, plain.length));
            assertThrows(IOException.class, () -> readCentral(path, ArchiveOptions.newBuilder().build()));
        }
        Path path = this.directory.resolve("wrong-size.zip");
        Files.write(path, centralFixture(compressed, plain.length + 1));
        assertThrows(IOException.class, () -> readCentral(path, ArchiveOptions.newBuilder().build()));
        Files.write(path, centralFixture(compressed, plain.length));
        ArchiveOptions limited = ArchiveOptions.newBuilder().limitBuilder()
                .maxHeaderBytes(plain.length - 1).confirm().build();
        assertThrows(IOException.class, () -> readCentral(path, limited));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsCorruptedCentralDirectoryHash(final boolean encrypted) throws Exception {
        ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO,
                encrypted ? ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 : 0,
                ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
        Path path = this.directory.resolve("bad-hash.zip");
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "entry", new byte[]{1, 2, 3});
        }
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer raw = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int locator = bytes.length - 22 - 20;
        assertEquals(0x07064B50, raw.getInt(locator));
        int zip64 = Math.toIntExact(raw.getLong(locator + 8));
        assertEquals(0x06064B50, raw.getInt(zip64));
        assertEquals(32, Short.toUnsignedInt(raw.getShort(zip64 + 82)));
        bytes[zip64 + 84] ^= 1;
        Files.write(path, bytes);
        assertThrows(IOException.class, () -> readCentral(path, archiveOptions()));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void rejectsIncorrectCompressedSize(final int delta) throws Exception {
        byte[] plain = plainCentral();
        byte[] compressed = compress(plain, 4);
        byte[] bytes = centralFixture(compressed, plain.length);
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                .putLong(compressed.length + 58, compressed.length + delta);
        Path path = this.directory.resolve("bad-size.zip");
        Files.write(path, bytes);
        assertThrows(IOException.class, () -> readCentral(path, ArchiveOptions.newBuilder().build()));
    }

    @Test
    void rejectsIncorrectCentralDirectoryPassword() throws Exception {
        ZipOptions options = optionsFor(Layout.SINGLE, Zip64Mode.AUTO,
                ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
        Path path = this.directory.resolve("password.zip");
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"),
                archiveOptions(), options)) {
            writeEntry(archive, "entry", new byte[]{1, 2, 3});
        }
        ArchiveOptions wrong = ArchiveOptions.newBuilder()
                .passwordProvider(PasswordProvider.fixed("incorrect".toCharArray())).build();
        assertThrows(IOException.class, () -> readCentral(path, wrong));
    }
}
