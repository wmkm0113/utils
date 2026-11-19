package org.nervousync.test.archive.lzma;

import org.junit.jupiter.api.Test;
import org.nervousync.archive.io.lzma.LZMAInputStream;
import org.nervousync.archive.io.lzma.LZMAOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class LZMAStreamTest {
    @Test
    void earlyCloseDoesNotDrainDelegate() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (LZMAOutputStream output = new LZMAOutputStream(bytes, 1)) { output.write(new byte[10000]); }
        class Source extends ByteArrayInputStream {
            int closes;
            Source(byte[] packed) { super(packed); }
            @Override public void close() { this.closes++; }
        }
        Source source = new Source(bytes.toByteArray());
        LZMAInputStream decoder = new LZMAInputStream(source, 10000, true);
        int remaining = source.available();
        decoder.close(); decoder.close();
        assertEquals(remaining, source.available()); assertEquals(1, source.closes);
        assertThrows(IOException.class, decoder::read);
    }

    @Test
    void aReadFailureRemainsSticky() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (LZMAOutputStream output = new LZMAOutputStream(bytes, 1)) { output.write(new byte[1000]); }
        byte[] truncated = Arrays.copyOf(bytes.toByteArray(), bytes.size() - 1);
        try (LZMAInputStream input = new LZMAInputStream(new ByteArrayInputStream(truncated), 1000, true)) {
            IOException failure = assertThrows(IOException.class, input::readAllBytes);
            assertSame(failure, assertThrows(IOException.class, input::read));
        }
    }
}
