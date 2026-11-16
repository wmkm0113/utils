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
package org.nervousync.test.archive.security;

import org.junit.jupiter.api.Test;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class Zip64ProtectionTest {

	@Test
	void hashDataDefaultsToEmptyArray() {
		Zip64EndCentralDirectory.Protection protection = new Zip64EndCentralDirectory.Protection();
		assertArrayEquals(new byte[0], protection.getHashData());

		protection.setHashData(null);
		assertArrayEquals(new byte[0], protection.getHashData());
	}

	@Test
	void hashDataIsDefensivelyCopied() {
		Zip64EndCentralDirectory.Protection protection = new Zip64EndCentralDirectory.Protection();
		byte[] source = {0x01, 0x02, 0x03};

		protection.setHashData(source);
		source[0] = 0x7F;
		assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, protection.getHashData());

		byte[] returned = protection.getHashData();
		returned[1] = 0x7F;
		assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, protection.getHashData());
	}
}