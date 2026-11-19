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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.io.lzma.LZMAInputStream;
import org.nervousync.archive.io.lzma.LZMAOutputStream;
import org.tukaani.xz.LZMA2Options;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

final class LZMACodecTest {
    private static byte[] resource(String name) throws IOException {
        try (InputStream input = LZMACodecTest.class.getResourceAsStream("/lzma/" + name)) { return input.readAllBytes(); }
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 6, 9})
    void decodesIndependentLzmaAloneFixtures(int preset) throws Exception {
        try (InputStream input = LZMAInputStream.alone(new ByteArrayInputStream(resource("python-" + preset + ".lzma")))) {
            assertArrayEquals(resource("payload.bin"), input.readAllBytes());
        }
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void encoderInteroperatesWithIndependentDecoderAtEveryLevel(int level) throws Exception {
        byte[] random = new byte[180000]; new Random(5927).nextBytes(random);
        for (byte[] payload : new byte[][]{new byte[0], {0}, random, resource("payload.bin")}) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (LZMAOutputStream encoder = LZMAOutputStream.alone(output, level)) {
                for (int offset = 0; offset < payload.length; offset += 1009)
                    encoder.write(payload, offset, Math.min(1009, payload.length - offset));
            }
            try (InputStream decoder = new org.tukaani.xz.LZMAInputStream(new ByteArrayInputStream(output.toByteArray()))) {
                assertArrayEquals(payload, decoder.readAllBytes());
            }
            try (InputStream decoder = LZMAInputStream.alone(new ByteArrayInputStream(output.toByteArray()))) {
                assertArrayEquals(payload, decoder.readAllBytes());
            }
            if (payload.length == 170000) assertTrue(output.size() < payload.length / 2);
        }
    }

    @Test void decodesNondefaultPropertiesAndBothEndConditions() throws Exception {
        byte[] payload = resource("payload.bin");
        for (int[] context : new int[][]{{0, 0}, {0, 4}, {2, 2}, {4, 0}, {3, 0}}) {
            for (int pb = 0; pb <= 4; pb++) {
                LZMA2Options options = new LZMA2Options(1);
                options.setDictSize(4096); options.setLcLp(context[0], context[1]); options.setPb(pb);
                for (boolean eos : new boolean[]{false, true}) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (org.tukaani.xz.LZMAOutputStream encoder = new org.tukaani.xz.LZMAOutputStream(bytes, options, eos)) { encoder.write(payload); }
                    int properties = (pb * 5 + context[1]) * 9 + context[0];
                    try (InputStream decoder = LZMAInputStream.raw(new ByteArrayInputStream(bytes.toByteArray()),
                            properties, 4096, payload.length, eos, 4096)) { assertArrayEquals(payload, decoder.readAllBytes()); }
                }
            }
        }
    }

    @Test void ownRawOutputSupportsKnownSizeWithoutEos() throws Exception {
        for (byte[] payload : new byte[][]{new byte[0], {1}, resource("payload.bin")}) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (LZMAOutputStream encoder = LZMAOutputStream.raw(bytes, 0, false)) { encoder.write(payload); }
            try (InputStream input = new org.tukaani.xz.LZMAInputStream(new ByteArrayInputStream(bytes.toByteArray()), payload.length, (byte) 93, 65536)) {
                assertArrayEquals(payload, input.readAllBytes());
            }
            try (InputStream input = LZMAInputStream.raw(new ByteArrayInputStream(bytes.toByteArray()), 93, 65536, payload.length, false, 65536)) {
                assertArrayEquals(payload, input.readAllBytes());
            }
        }
    }

    @Test void zipPropertiesAndEosDoNotConsumeFollowingBytes() throws Exception {
        byte[] payload = resource("payload.bin"); ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LZMAOutputStream output = new LZMAOutputStream(bytes, 5); output.write(payload); output.finish();
        assertArrayEquals(new byte[]{9, 4, 5, 0, 93}, Arrays.copyOf(bytes.toByteArray(), 5));
        int size = bytes.size(); output.finish(); assertEquals(size, bytes.size()); bytes.write(123);
        ByteArrayInputStream source = new ByteArrayInputStream(bytes.toByteArray());
        try (InputStream input = new LZMAInputStream(source, payload.length, true)) {
            assertArrayEquals(payload, input.readAllBytes()); assertEquals(123, source.read());
            assertEquals(-1, input.read()); assertEquals(0, input.read(new byte[0]));
        }
    }

    @Test void rejectsEveryTruncationAndWrongDeclaredSize() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (LZMAOutputStream output = new LZMAOutputStream(bytes, 1)) { output.write(new byte[1000]); }
        byte[] packed = bytes.toByteArray();
        for (int size = 0; size < packed.length; size++) {
            byte[] truncated = Arrays.copyOf(packed, size);
            assertThrows(IOException.class, () -> {
                try (InputStream input = new LZMAInputStream(new ByteArrayInputStream(truncated), 1000, true)) { input.readAllBytes(); }
            }, "cutoff " + size);
        }
        for (int size : new int[]{0, 999, 1001}) assertThrows(IOException.class, () -> {
            try (InputStream input = new LZMAInputStream(new ByteArrayInputStream(packed), size, true)) { input.readAllBytes(); }
        });
    }

    @Test void validatesPropertiesAndDictionaryBeforeAllocation() throws Exception {
        byte[] header = {9, 4, 5, 0, 93, 0, 0, 0, 5, 0, 0, 0, 0, 0};
        assertThrows(IOException.class, () -> new LZMAInputStream(new ByteArrayInputStream(header), -1, true));
        header[8] = 0; header[4] = (byte) 225;
        assertThrows(IOException.class, () -> new LZMAInputStream(new ByteArrayInputStream(header), -1, true));
        header[4] = 93; header[2] = 6;
        assertThrows(IOException.class, () -> new LZMAInputStream(new ByteArrayInputStream(header), -1, true));
        assertThrows(IllegalArgumentException.class, () -> LZMAInputStream.raw(new ByteArrayInputStream(new byte[5]), 93, 4096, -1, false, 4096));
        assertThrows(IllegalArgumentException.class, () -> new LZMAOutputStream(new ByteArrayOutputStream(), -1));
    }

    @Test void streamOwnershipSkipAndBoundsAreConsistent() throws Exception {
        class Destination extends ByteArrayOutputStream { int closes; @Override public void close() { this.closes++; } }
        Destination destination = new Destination(); LZMAOutputStream output = LZMAOutputStream.alone(destination, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> output.write(new byte[1], -1, 1));
        byte[] payload = resource("payload.bin"); output.write(payload); output.flush(); output.write(99); output.finish();
        assertEquals(0, destination.closes); output.close(); output.close(); assertEquals(1, destination.closes);
        assertThrows(IOException.class, () -> output.write(0));
        try (LZMAInputStream input = LZMAInputStream.alone(new ByteArrayInputStream(destination.toByteArray()))) {
            assertThrows(IndexOutOfBoundsException.class, () -> input.read(new byte[2], 1, 2));
            assertEquals(90000, input.skip(90000)); byte[] remainder = input.readAllBytes();
            assertEquals(payload.length - 90000 + 1, remainder.length); assertEquals(99, remainder[remainder.length - 1]);
        }
    }

    @Test void finishFailureClosesParentAndSuppressesCloseFailure() throws Exception {
        class Broken extends OutputStream {
            int closes;
            @Override public void write(int value) throws IOException { throw new IOException("write failure"); }
            @Override public void close() throws IOException { this.closes++; throw new IOException("close failure"); }
        }
        Broken broken = new Broken(); LZMAOutputStream output = LZMAOutputStream.raw(broken, 0, true); output.write(new byte[100]);
        IOException failure = assertThrows(IOException.class, output::close);
        assertEquals("write failure", failure.getMessage()); assertEquals(1, failure.getSuppressed().length);
        assertEquals(1, broken.closes); output.close();
    }

    @Test void optionalEosAndKnownSizeAloneHeadersAreSupported() throws Exception {
        for (byte[] payload : new byte[][]{new byte[0], resource("payload.bin")}) {
            for (boolean eos : new boolean[]{false, true}) {
                ByteArrayOutputStream zip = new ByteArrayOutputStream();
                zip.write(new byte[]{9, 4, 5, 0, 93, 0, 0, 1, 0});
                try (LZMAOutputStream output = LZMAOutputStream.raw(zip, 0, eos)) { output.write(payload); }
                try (InputStream input = LZMAInputStream.knownSize(new ByteArrayInputStream(zip.toByteArray()), payload.length)) {
                    assertArrayEquals(payload, input.readAllBytes());
                }
                ByteArrayOutputStream alone = new ByteArrayOutputStream();
                alone.write(new byte[]{93, 0, 0, 1, 0});
                for (int i = 0; i < 8; i++) alone.write((int) ((long) payload.length >>> (8 * i)) & 255);
                alone.write(zip.toByteArray(), 9, zip.size() - 9);
                try (InputStream input = LZMAInputStream.alone(new ByteArrayInputStream(alone.toByteArray()))) {
                    assertArrayEquals(payload, input.readAllBytes());
                }
            }
        }
    }

    @Test void malformedRangeAndDistanceBitsNeverEscapeAsUncheckedErrors() throws Exception {
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        try (LZMAOutputStream output = LZMAOutputStream.raw(packed, 0, true)) { output.write(new byte[1000]); }
        for (int offset = 0; offset < packed.size(); offset++) for (int bit = 0; bit < 8; bit++) {
            byte[] damaged = packed.toByteArray(); damaged[offset] ^= 1 << bit;
            try (InputStream input = LZMAInputStream.raw(new ByteArrayInputStream(damaged), 93, 65536, 1000, true, 65536)) {
                assertEquals(1000, input.readAllBytes().length);
            } catch (IOException expected) { /* A corrupt stream must fail with an I/O error. */ }
        }
    }
}
