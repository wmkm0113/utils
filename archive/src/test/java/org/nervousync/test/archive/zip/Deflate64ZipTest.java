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

package org.nervousync.test.archive.zip;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

final class Deflate64ZipTest {
    @TempDir Path directory;

    @Test void writesDeflate64MinimumVersionToBothHeaders() throws Exception {
        for (Encryption encryption : new Encryption[]{Encryption.NONE,Encryption.STANDARD}) {
            Path zip=this.directory.resolve("version-"+encryption+".zip");
            ZipOptions options=options(CompressionAlgorithm.DEFLATE64, Zip64Mode.AUTO,Layout.SINGLE,encryption);
            try (var archive=ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"rw"),archiveOptions(),options)) {
                writeEntry(archive,"data",new byte[]{1,2,3});
            }
            byte[] bytes=Files.readAllBytes(zip);
            var raw=java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            assertEquals(0x04034B50,raw.getInt(0));
            assertEquals(21,Short.toUnsignedInt(raw.getShort(4)),"local minimum version");
            assertEquals(0x06054B50,raw.getInt(bytes.length-22));
            int central=raw.getInt(bytes.length-6);
            assertEquals(0x02014B50,raw.getInt(central));
            assertEquals(21,Short.toUnsignedInt(raw.getShort(central+6)),"central minimum version");
        }
    }

    static Stream<Arguments> configurations() {
        return Stream.of(Layout.values()).flatMap(layout -> Stream.of(Encryption.values())
                .flatMap(encryption -> Stream.of(false,true).map(central -> Arguments.of(layout,encryption,central))));
    }
    @ParameterizedTest @MethodSource("configurations")
    void writesEntriesAndProtectedCentralDirectory(Layout layout, Encryption encryption, boolean central) throws Exception {
        Path zip = directory.resolve("written.zip");
        ZipOptions.Builder builder = ZipOptions.newBuilder(options(CompressionAlgorithm.DEFLATE64, Zip64Mode.ALWAYS, layout, encryption));
        if (central) builder.enableCentralDirectoryCompress().centralDirectoryHash(1);
        ZipOptions options = builder.build();
        byte[] data = payload(170000);
        try (var archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"), archiveOptions(), options)) {
            writeEntry(archive, "data", data); writeEntry(archive, "empty", new byte[0]);
        }
        assertContent(readablePath(zip, layout), options, Map.of("data", data, "empty", new byte[0]));
        if (layout == Layout.SINGLE && encryption == Encryption.NONE && !central) {
            try (var independent = org.apache.commons.compress.archivers.zip.ZipFile.builder().setPath(zip).get()) {
                var entry = independent.getEntry("data");
                assertEquals(9, entry.getMethod());
                try (var input = independent.getInputStream(entry)) { assertArrayEquals(data, input.readAllBytes()); }
            }
        }
    }

    @Test void readsEncryptedCompressedCentralDirectory() throws Exception {
        for (int encryption : new int[]{0x660E,0x6603}) {
            Path zip = directory.resolve("central-"+encryption+".zip");
            ZipOptions options = ZipOptions.newBuilder(options(CompressionAlgorithm.DEFLATE64,
                    Zip64Mode.AUTO, Layout.SINGLE, Encryption.NONE))
                    .enableCentralDirectoryCompress().centralDirectoryHash(0x800C)
                    .centralDirectoryCrypto(encryption,encryption==0x660E ? 128 : 168).build();
            byte[] data=payload(100001);
            try (var archive=ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"rw"),archiveOptions(),options)) {
                writeEntry(archive,"data",data);
            }
            assertContent(zip,options,Map.of("data",data));
        }
    }

    @Test void readsSevenZipDeflate64() throws Exception {
        Path zip = directory.resolve("deflate64.zip");
        try (var source = getClass().getResourceAsStream("/deflate/deflate64.zip")) {
            Files.copy(source, zip);
        }
        byte[] expected = new byte[170000];
        for (int i = 0; i < 50000; i++) expected[i] = (byte) (i * 37 + i / 7);
        System.arraycopy(expected, 0, expected, 50000, 50000);
        Arrays.fill(expected, 100000, expected.length, (byte) 'A');
        assertDoesNotThrow(() -> {
            try (var archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "r"), ZipOptions.newBuilder().build());
                 var reader = archive.openReader(archive.entry("payload.bin").orElseThrow()).orElseThrow();
                 var input = reader.openStream()) {
                assertArrayEquals(expected, input.readAllBytes());
            }
        });
    }
}
