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

import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;
import org.junit.jupiter.api.Test;
import org.nervousync.archive.io.deflate.DeflateInputStream;
import org.nervousync.archive.io.deflate.DeflateOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static org.junit.jupiter.api.Assertions.*;

final class DeflateStreamTest {
    @Test void decodesJdkStoredFixedAndDynamicStreams() throws Exception {
        byte[][] payloads = {new byte[0], new byte[]{1,2,3}, new byte[150000], random(170000)};
        Arrays.fill(payloads[2], (byte) 'x');
        for (int level : new int[]{0,1,6,9}) for (byte[] payload : payloads) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Deflater compressor = new Deflater(level, true);
            try (var stream = new DeflaterOutputStream(out, compressor)) { stream.write(payload); }
            finally { compressor.end(); }
            try (var stream = new DeflateInputStream(new ByteArrayInputStream(out.toByteArray()), false)) {
                assertArrayEquals(payload, stream.readAllBytes());
            }
        }
    }
    @Test void writerInteroperatesWithIndependentDecoders() throws Exception {
        byte[] repeated = new byte[220000]; Arrays.fill(repeated, (byte) 'A');
        byte[] far = random(50000);
        ByteArrayOutputStream large = new ByteArrayOutputStream(); large.write(far); large.write(far); large.write(repeated);
        for (boolean extended : new boolean[]{false,true}) for (byte[] payload : new byte[][]{new byte[0],random(180000),large.toByteArray()}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (var stream = new DeflateOutputStream(out, extended, 6)) {
                for (int i = 0; i < payload.length; i += 997) stream.write(payload, i, Math.min(997, payload.length-i));
                stream.finish(); stream.finish();
                assertEquals(payload.length, stream.getBytesRead());
                assertEquals(out.size(), stream.getBytesWritten());
                assertThrows(IOException.class, () -> stream.write(1));
            }
            Inflater inflater = new Inflater(true);
            try (InputStream independent = extended ? new Deflate64CompressorInputStream(new ByteArrayInputStream(out.toByteArray()))
                    : new InflaterInputStream(new ByteArrayInputStream(out.toByteArray()), inflater)) {
                assertArrayEquals(payload, independent.readAllBytes());
            } finally { inflater.end(); }
            try (var stream = new DeflateInputStream(new ByteArrayInputStream(out.toByteArray()), extended)) {
                assertArrayEquals(payload, stream.readAllBytes());
            }
        }
    }
    @Test void rejectsTruncationAndInvalidBlocks() throws Exception {
        for (byte[] data : new byte[][]{new byte[0], {7}, {1,1,0,0,0}, {3}}) {
            for (boolean extended : new boolean[]{false,true}) {
                try (var stream = new DeflateInputStream(new ByteArrayInputStream(data), extended)) {
                    assertThrows(IOException.class, stream::readAllBytes);
                }
            }
        }
    }
    @Test void preservesFollowingBytesAndStreamContract() throws Exception {
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{3,0,99});
        var stream = new DeflateInputStream(source, true);
        assertEquals(0, stream.read(new byte[0]));
        assertEquals(-1, stream.read()); assertEquals(-1, stream.read());
        assertEquals(99, source.read());
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[1], 1, 1));
        stream.close(); stream.close();
        assertThrows(IOException.class, stream::read);
    }
    @Test void storedDeflate64WriterAndMaximumMatch() throws Exception {
        for (int level : new int[]{0,1,9}) {
            byte[] payload = new byte[200000]; Arrays.fill(payload, (byte) 65);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (var writer = new DeflateOutputStream(out, true, level)) { writer.write(payload); }
            try (var independent = new Deflate64CompressorInputStream(new ByteArrayInputStream(out.toByteArray()))) {
                assertArrayEquals(payload, independent.readAllBytes());
            }
        }
    }
    @Test void decodesMaximumDistanceAndLengthAcrossBlocks() throws Exception {
        byte[] history = random(65536);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        // Two non-final stored blocks establish exactly 64 KiB of history.
        compressed.write(new byte[]{0,(byte)255,(byte)255,0,0}); compressed.write(history,0,65535);
        compressed.write(new byte[]{0,1,0,(byte)254,(byte)255,history[65535]});
        Bits bits = new Bits(compressed);
        bits.put(3,3); // final fixed Huffman block
        bits.put(Integer.reverse(197) >>> 24,8); // literal/length symbol 285
        bits.put(65535,16); // length 65538
        bits.put(31,5); bits.put(16383,14); // distance 65536
        bits.put(0,7); bits.finish(); // EOB
        byte[] expected = Arrays.copyOf(history,131074);
        System.arraycopy(history,0,expected,65536,65536);
        expected[131072]=history[0]; expected[131073]=history[1];
        try (var input = new DeflateInputStream(new ByteArrayInputStream(compressed.toByteArray()),true)) {
            assertArrayEquals(expected,input.readAllBytes());
        }
        try (var input = new Deflate64CompressorInputStream(new ByteArrayInputStream(compressed.toByteArray()))) {
            assertArrayEquals(expected,input.readAllBytes());
        }
        try (var input = new DeflateInputStream(new ByteArrayInputStream(compressed.toByteArray()),false)) {
            assertThrows(IOException.class,input::readAllBytes);
        }
    }
    @Test void sharedBookPreservesDynamicTreeValidation() throws Exception {
        // Explicit bit-level fixtures: no production codebook is used to encode these streams.
        byte[] literalOnly = dynamicBlock(257, new int[]{65,256}, 1, 0,1);
        byte[] endOnly = dynamicBlock(257, new int[]{256}, 1, 0);
        byte[][] malformed = {
                dynamicBlock(257, new int[]{65,256}, 2), // Incomplete, non-singleton literal tree.
                dynamicBlock(257, new int[]{65}, 1), // Missing end-of-block symbol.
                dynamicBlock(258, new int[]{256,257}, 1, 1) // Match with an empty distance tree.
        };
        for (boolean extended : new boolean[]{false,true}) {
            try (var input = new DeflateInputStream(new ByteArrayInputStream(literalOnly),extended)) {
                assertArrayEquals(new byte[]{65},input.readAllBytes());
            }
            try (var input = new DeflateInputStream(new ByteArrayInputStream(endOnly),extended)) {
                assertArrayEquals(new byte[0],input.readAllBytes());
            }
            for (byte[] bytes : malformed) {
                try (var input = new DeflateInputStream(new ByteArrayInputStream(bytes),extended)) {
                    assertThrows(IOException.class,input::readAllBytes);
                }
            }
        }
    }
    private static byte[] dynamicBlock(int literalCount,int[] used,int codeLength,int... payloadBits) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Bits bits = new Bits(out);
        bits.put(5,3); bits.put(literalCount-257,5); bits.put(0,5); bits.put(14,4);
        int[] order={16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1};
        for (int symbol : order) bits.put(symbol==0 || symbol==codeLength ? 1 : 0,3);
        for (int symbol=0;symbol<literalCount;symbol++) {
            boolean present=false;
            for (int value : used) if (value==symbol) present=true;
            bits.put(present ? 1 : 0,1);
        }
        bits.put(0,1); // Single unused distance symbol, encoded with length zero.
        for (int bit : payloadBits) bits.put(bit,1);
        bits.finish();
        return out.toByteArray();
    }
    private static final class Bits {
        final ByteArrayOutputStream out; int bits,count;
        Bits(ByteArrayOutputStream out) { this.out=out; }
        void put(int value,int size) {
            bits |= value << count; count+=size;
            while(count>=8) { out.write(bits & 255); bits >>>=8; count-=8; }
        }
        void finish() { if(count>0) out.write(bits & 255); }
    }
    private static byte[] random(int size) {
        byte[] bytes = new byte[size]; new Random(1729).nextBytes(bytes); return bytes;
    }
}
