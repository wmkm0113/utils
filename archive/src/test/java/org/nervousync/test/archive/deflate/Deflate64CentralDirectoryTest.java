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

package org.nervousync.test.archive.deflate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.operator.ZipFileOperator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/** Independent method-9 envelopes, stream boundaries and central-directory rewrites. */
final class Deflate64CentralDirectoryTest {
    @TempDir Path directory;

    private static ZipOptions optionsFor(final Layout layout, final boolean encrypted) throws Exception {
        ZipOptions.Builder builder = ZipOptions.newBuilder(options(CompressionAlgorithm.DEFLATE64,
                Zip64Mode.AUTO, layout, Encryption.NONE))
                .enableCentralDirectoryCompress()
                .centralDirectoryHash(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
        if (encrypted) {
            builder.centralDirectoryCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256,256);
        }
        return builder.build();
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void mergePreservesCentralCompressionAndEncryption(final boolean encrypted) throws Exception {
        Path source=this.directory.resolve("split.zip"), merged=this.directory.resolve("merged.zip");
        ZipOptions options=optionsFor(Layout.NATIVE,encrypted);
        byte[] data=payload(120017);
        try (var archive=ArchiveFile.zipFile(new LocalDataFile(source.toString(),"rw"),archiveOptions(),options)) {
            writeEntry(archive,"payload",data);
        }
        ZipOptions reading=ZipOptions.newBuilder().splitArchive(true,65536).build();
        try (var archive=ArchiveFile.zipFile(new LocalDataFile(source.toString(),"r"),archiveOptions(),reading)) {
            archive.merge(merged);
        }
        assertContent(merged,ZipOptions.newBuilder().build(),Map.of("payload",data));
        try (var operator=new ZipFileOperator(new LocalDataFile(merged.toString(),"r"),archiveOptions(),ZipOptions.newBuilder().build())) {
            var end=operator.readEndCentralDirectoryRecord();
            var protection=operator.readZip64EndCentralDirectory(end).getProtection();
            assertEquals(9,protection.getCompressionMethod());
            assertEquals(encrypted ? ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256 : 0,protection.getAlgorithm());
            assertEquals(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256,protection.getHashAlgorithm());
        }
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void readsEmptyCompressedDirectory(final boolean encrypted) throws Exception {
        Path path=this.directory.resolve("empty.zip");
        ZipOptions options=optionsFor(Layout.SINGLE,encrypted);
        try (var ignored=ArchiveFile.zipFile(new LocalDataFile(path.toString(),"rw"),archiveOptions(),options)) { }
        try (var archive=ArchiveFile.zipFile(new LocalDataFile(path.toString(),"r"),archiveOptions(),options)) {
            assertTrue(archive.entry("missing").isEmpty());
        }
        try (var operator=new ZipFileOperator(new LocalDataFile(path.toString(),"r"),archiveOptions(),options)) {
            var protection=operator.readZip64EndCentralDirectory(operator.readEndCentralDirectoryRecord()).getProtection();
            assertEquals(9,protection.getCompressionMethod());
            assertEquals(0,protection.getUncompressedSize());
            assertEquals(2,protection.getCompressedSize());
        }
    }

    @Test void readsIndependentMethodNineDirectory() throws Exception {
        byte[] plain=plainCentral();
        Path path=this.directory.resolve("independent.zip");
        Files.write(path,fixture(stored(plain),plain.length));
        assertNotNull(readFixture(path,ArchiveOptions.newBuilder().build()).header("entry.txt"));
    }

    @ParameterizedTest @ValueSource(ints={1,8193})
    void rejectsBufferedAndUnbufferedTrailingBytes(final int suffix) throws Exception {
        byte[] plain=plainCentral(), compressed=stored(plain);
        byte[] extra=Arrays.copyOf(compressed,compressed.length+suffix);
        extra[extra.length-1]=42;
        Path path=this.directory.resolve("trailing.zip");
        Files.write(path,fixture(extra,plain.length));
        assertThrows(IOException.class,()->readFixture(path,ArchiveOptions.newBuilder().build()));
    }

    @Test void rejectsTruncationWrongSizesAndHeaderLimit() throws Exception {
        byte[] plain=plainCentral(), compressed=stored(plain);
        Path path=this.directory.resolve("invalid.zip");
        Files.write(path,fixture(Arrays.copyOf(compressed,compressed.length-1),plain.length));
        assertThrows(IOException.class,()->readFixture(path,ArchiveOptions.newBuilder().build()));
        Files.write(path,fixture(compressed,plain.length+1));
        assertThrows(IOException.class,()->readFixture(path,ArchiveOptions.newBuilder().build()));
        for (int delta : new int[]{-1,1}) {
            byte[] bytes=fixture(compressed,plain.length);
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(compressed.length+58,compressed.length+delta);
            Files.write(path,bytes);
            assertThrows(IOException.class,()->readFixture(path,ArchiveOptions.newBuilder().build()));
        }
        Files.write(path,fixture(compressed,plain.length));
        var limited=ArchiveOptions.newBuilder().limitBuilder().maxHeaderBytes(plain.length-1).confirm().build();
        assertThrows(IOException.class,()->readFixture(path,limited));
    }

    private static org.nervousync.archive.zip.models.central.CentralDirectory readFixture(
            final Path path, final ArchiveOptions options) throws Exception {
        try (var operator=new ZipFileOperator(new LocalDataFile(path.toString(),"r"),options,ZipOptions.newBuilder().build())) {
            var end=operator.readEndCentralDirectoryRecord();
            return operator.readCentralDirectory(end,operator.readZip64EndCentralDirectory(end));
        }
    }

    /** Stored blocks are common to both formats; construct directly without either encoder. */
    private static byte[] stored(final byte[] plain) {
        return ByteBuffer.allocate(plain.length+5).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte)1).putShort((short)plain.length).putShort((short)(plain.length^65535)).put(plain).array();
    }

    private static byte[] plainCentral() {
        byte[] name="entry.txt".getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(46+name.length+256).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0x02014B50).putShort((short)20).putShort((short)20).putShort((short)0)
                .putShort((short)0).putShort((short)0).putShort((short)0).putInt(0).putInt(0).putInt(0)
                .putShort((short)name.length).putShort((short)0).putShort((short)256).putShort((short)0)
                .putShort((short)0).putInt(0).putInt(0).put(name).put(new byte[256]).array();
    }

    private static byte[] fixture(final byte[] compressed, final int originalLength) {
        return ByteBuffer.allocate(compressed.length+84+20+22).order(ByteOrder.LITTLE_ENDIAN)
                .put(compressed).putInt(0x06064B50).putLong(72).putShort((short)63).putShort((short)62)
                .putInt(0).putInt(0).putLong(0).putLong(1).putLong(compressed.length).putLong(0)
                .putShort((short)9).putLong(compressed.length).putLong(originalLength)
                .putShort((short)0).putShort((short)0).putShort((short)0).putShort((short)0).putShort((short)0)
                .putInt(0x07064B50).putInt(0).putLong(compressed.length).putInt(1)
                .putInt(0x06054B50).putShort((short)0).putShort((short)0).putShort((short)0)
                .putShort((short)0xFFFF).putInt(0xFFFFFFFF).putInt(0xFFFFFFFF).putShort((short)0).array();
    }
}
