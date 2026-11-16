package org.nervousync.test.archive.zip;

import org.junit.jupiter.api.Test;
import org.nervousync.archive.io.zip.BoundedInputStream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;

import static org.junit.jupiter.api.Assertions.*;

final class BoundedInputStreamAdditionalTest {

	@Test
	void rejectsNegativeLength() {
		assertThrows(IllegalArgumentException.class,
				() -> new BoundedInputStream(new ByteArrayInputStream(new byte[0]), -1L));
	}

	@Test
	void singleByteReadStopsExactlyAtBoundary() throws Exception {
		BoundedInputStream input = new BoundedInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}), 2L);

		assertEquals(1, input.read());
		assertEquals(2, input.read());
		assertEquals(-1, input.read());
		assertEquals(0L, input.remaining());
	}

	@Test
	void bulkReadIsClampedToBoundary() throws Exception {
		BoundedInputStream input = new BoundedInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}), 3L);
		byte[] buffer = new byte[8];

		assertEquals(3, input.read(buffer, 0, buffer.length));
		assertArrayEquals(new byte[]{1, 2, 3}, new byte[]{buffer[0], buffer[1], buffer[2]});
		assertEquals(-1, input.read(buffer, 0, buffer.length));
	}

	@Test
	void prematureEndThrowsEof() throws Exception {
		BoundedInputStream input = new BoundedInputStream(new ByteArrayInputStream(new byte[]{1}), 2L);

		assertEquals(1, input.read());
		assertThrows(EOFException.class, input::read);
	}

	@Test
	void closeDoesNotCloseSharedUnderlyingStream() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{1, 2});
		BoundedInputStream input = new BoundedInputStream(source, 1L);

		input.close();

		assertEquals(1, source.read());
	}
}
