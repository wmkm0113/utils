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

package org.nervousync.test.archive.lzma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.operator.ZipFileOperator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

final class LZMAZipTest {
    @TempDir Path directory;

    @Test void readsIndependentPythonZip() throws Exception {
        Path zip = this.directory.resolve("python.zip");
        byte[] expected;
        try (InputStream input = getClass().getResourceAsStream("/lzma/python-lzma.zip")) { Files.copy(input, zip); }
        try (InputStream input = getClass().getResourceAsStream("/lzma/payload.bin")) { expected = input.readAllBytes(); }
        assertContent(zip, ZipOptions.newBuilder().build(), Map.of("payload.bin", expected, "empty.bin", new byte[0]));
    }

    static Stream<Arguments> configurations() {
        return Stream.of(Layout.values()).flatMap(layout -> Stream.of(Encryption.values()).flatMap(encryption ->
                Stream.of(false, true).map(central -> Arguments.of(layout, encryption, central))));
    }

    @ParameterizedTest @MethodSource("configurations")
    void supportsEncryptionZip64SplittingAndDirectoryCompression(Layout layout, Encryption encryption, boolean central) throws Exception {
        Path zip = this.directory.resolve("matrix.zip");
        ZipOptions.Builder builder = ZipOptions.newBuilder(options(CompressionAlgorithm.LZMA1, Zip64Mode.ALWAYS, layout, encryption));
        if (central) builder.enableCentralDirectoryCompress().centralDirectoryHash(0x800C);
        ZipOptions options = builder.build(); byte[] expected = payload(170001);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"), archiveOptions(), options)) {
            writeEntry(archive, "payload.bin", expected); writeEntry(archive, "empty.bin", new byte[0]); writeEntry(archive, "folder/", new byte[0]);
        }
        assertContent(readablePath(zip, layout), options, Map.of("payload.bin", expected, "empty.bin", new byte[0]));
        if (layout == Layout.SINGLE && encryption == Encryption.NONE && !central) {
            try (var external = org.apache.commons.compress.archivers.zip.ZipFile.builder().setPath(zip).get()) {
                var entry = external.getEntry("payload.bin"); assertEquals(14, entry.getMethod());
                // Commons Compress exposes the ZIP envelope; XZ independently decodes its raw LZMA data.
                try (InputStream raw = external.getRawInputStream(entry)) {
                    byte[] properties = raw.readNBytes(9);
                    assertEquals(5, ByteBuffer.wrap(properties).order(ByteOrder.LITTLE_ENDIAN).getShort(2));
                    int dictionary = ByteBuffer.wrap(properties).order(ByteOrder.LITTLE_ENDIAN).getInt(5);
                    try (InputStream input = new org.tukaani.xz.LZMAInputStream(raw, -1, properties[4], dictionary)) {
                        assertArrayEquals(expected, input.readAllBytes());
                    }
                }
            }
        }
    }

    @Test void eosBitAndMinimumVersionSurviveEncryption() throws Exception {
        for (Encryption encryption : Encryption.values()) {
            Path zip = this.directory.resolve(encryption + ".zip");
            ZipOptions options = options(CompressionAlgorithm.LZMA1, Zip64Mode.AUTO, Layout.SINGLE, encryption);
            try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"), archiveOptions(), options)) { writeEntry(archive, "data", payload(31)); }
            byte[] bytes = Files.readAllBytes(zip); ByteBuffer raw = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            int central = raw.getInt(bytes.length - 6);
            assertEquals(63, Short.toUnsignedInt(raw.getShort(4)));
            assertEquals(63, Short.toUnsignedInt(raw.getShort(central + 6)));
            assertEquals(2, raw.getShort(6) & 2); assertEquals(2, raw.getShort(central + 8) & 2);
        }
        Path zip = this.directory.resolve("3des.zip");
        ZipOptions options = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.LZMA1).strongCrypto(0x6603, 168).build();
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"), archiveOptions(), options)) { writeEntry(archive, "data", payload(31)); }
        assertEquals(63, ByteBuffer.wrap(Files.readAllBytes(zip)).order(ByteOrder.LITTLE_ENDIAN).getShort(4));
    }

    @Test void mergePreservesEncryptedLzmaCentralDirectoryAndEditingWorks() throws Exception {
        Path zip = this.directory.resolve("split.zip"), merged = this.directory.resolve("merged.zip");
        ZipOptions options = ZipOptions.newBuilder(options(CompressionAlgorithm.LZMA1, Zip64Mode.AUTO, Layout.NATIVE, Encryption.AES))
                .enableCentralDirectoryCompress().centralDirectoryCrypto(0x6610, 256).centralDirectoryHash(0x800C).build();
        byte[] data = payload(170001);
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"), archiveOptions(), options)) { writeEntry(archive, "keep", data); }
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "r"), archiveOptions(), ZipOptions.newBuilder().splitArchive(true, 65536).build())) { archive.merge(merged); }
        assertContent(merged, ZipOptions.newBuilder().build(), Map.of("keep", data));
        ZipOptions editing = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.LZMA1).enableCentralDirectoryCompress().build();
        try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(merged.toString(), "rw"), archiveOptions(), editing)) { writeEntry(archive, "added", new byte[]{1,2,3}); }
        assertContent(merged, editing, Map.of("keep", data, "added", new byte[]{1,2,3}));
    }

    @Test void emptyAndEncryptedCentralDirectoriesAreValid() throws Exception {
        for (int algorithm : new int[]{0, 0x6603, 0x6610}) {
            Path zip = this.directory.resolve("empty-" + algorithm + ".zip");
            ZipOptions.Builder builder = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.LZMA1)
                    .enableCentralDirectoryCompress().centralDirectoryHash(0x800C);
            if (algorithm != 0) builder.centralDirectoryCrypto(algorithm, algorithm == 0x6603 ? 168 : 256);
            ZipOptions options = builder.build();
            try (ArchiveFile ignored = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"), archiveOptions(), options)) { }
            try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "r"), archiveOptions(), options)) { assertTrue(archive.entry("missing").isEmpty()); }
            try (ZipFileOperator operator = new ZipFileOperator(new LocalDataFile(zip.toString(), "r"), archiveOptions(), options)) {
                var end = operator.readEndCentralDirectoryRecord(); var zip64 = operator.readZip64EndCentralDirectory(end);
                assertEquals(14, zip64.getProtection().getCompressionMethod()); assertEquals(63, zip64.getRecord().getExtractNeeded());
            }
        }
    }
    @Test void readsIndependentSizeDelimitedAndEosZipWithDescriptors() throws Exception {
        byte[] expected = payload(120017);
        for (boolean eos : new boolean[]{false, true}) for (boolean descriptor : new boolean[]{false, true}) {
            Path path = this.directory.resolve("external-" + eos + "-" + descriptor + ".zip");
            Files.write(path, fixture(expected, descriptor, eos, false));
            assertContent(path, ZipOptions.newBuilder().build(), Map.of("payload.bin", expected));
            Files.write(path, fixture(expected, descriptor, eos, true));
            assertThrows(IOException.class, () -> assertContent(path, ZipOptions.newBuilder().build(), Map.of("payload.bin", expected)));
        }
    }

	private static byte[] fixture(byte[] original, boolean descriptor, boolean eos, boolean badZipCrc) throws Exception {
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		org.tukaani.xz.LZMA2Options options = new org.tukaani.xz.LZMA2Options(1);
        options.setDictSize(65536);
        compressed.write(new byte[]{9, 4, 5, 0, 93, 0, 0, 1, 0});
        try (OutputStream encoder = new org.tukaani.xz.LZMAOutputStream(compressed, options, eos)) { encoder.write(original); }
		byte[] payload = compressed.toByteArray();
		java.util.zip.CRC32 crc32 = new java.util.zip.CRC32(); crc32.update(original);
		int crc = (int) crc32.getValue() ^ (badZipCrc ? 1 : 0);
		byte[] name = "payload.bin".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		int flags = (descriptor ? 0x0008 : 0) | (eos ? 2 : 0);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ByteBuffer local = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
		local.putInt(0x04034B50).putShort((short) 63).putShort((short) flags).putShort((short) 14)
				.putShort((short) 0).putShort((short) 0).putInt(descriptor ? 0 : crc)
				.putInt(descriptor ? 0 : payload.length).putInt(descriptor ? 0 : original.length)
				.putShort((short) name.length).putShort((short) 0);
		bytes.write(local.array()); bytes.write(name); bytes.write(payload);
		if (descriptor) {
			bytes.write(ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
					.putInt(0x08074B50).putInt(crc).putInt(payload.length).putInt(original.length).array());
		}
		int centralStart = bytes.size();
		ByteBuffer central = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN);
		central.putInt(0x02014B50).putShort((short) 63).putShort((short) 63).putShort((short) flags)
				.putShort((short) 14).putShort((short) 0).putShort((short) 0).putInt(crc)
				.putInt(payload.length).putInt(original.length).putShort((short) name.length)
				.putShort((short) 0).putShort((short) 0).putShort((short) 0).putShort((short) 0)
				.putInt(0).putInt(0);
		bytes.write(central.array()); bytes.write(name);
		int centralSize = bytes.size() - centralStart;
		bytes.write(ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN).putInt(0x06054B50)
				.putShort((short) 0).putShort((short) 0).putShort((short) 1).putShort((short) 1)
				.putInt(centralSize).putInt(centralStart).putShort((short) 0).array());
		return bytes.toByteArray();
	}
}
