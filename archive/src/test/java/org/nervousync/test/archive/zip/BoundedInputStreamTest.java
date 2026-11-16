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
import org.nervousync.archive.io.zip.BoundedInputStream;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BoundedInputStreamTest {

	@Test
	void skipWithinBoundaryUpdatesRemaining() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{0x10, 0x11, 0x12, 0x13});

		BoundedInputStream bounded = new BoundedInputStream(source, 3L);

		assertEquals(2L, bounded.skip(2L));
		assertEquals(1L, bounded.remaining());

		assertEquals(0x12, bounded.read());
		assertEquals(0L, bounded.remaining());
		assertEquals(-1, bounded.read());

		// 边界之外的数据不能被 BoundedInputStream 消费。
		assertEquals(0x13, source.read());
	}

	@Test
	void skipPastBoundaryIsClamped() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{0x20, 0x21, 0x22});

		BoundedInputStream bounded = new BoundedInputStream(source, 2L);

		assertEquals(2L, bounded.skip(100L));
		assertEquals(0L, bounded.remaining());
		assertEquals(-1, bounded.read());

		assertEquals(0x22, source.read());
	}
}