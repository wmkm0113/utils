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
import org.nervousync.archive.enumerations.DuplicatePolicy;
import org.nervousync.archive.zip.models.central.CentralDirectory;
import org.nervousync.archive.zip.models.header.GeneralFileHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CentralDirectoryTest {

	@Test
	void optionalDataDefaultsToEmptyArrays() {
		CentralDirectory centralDirectory = new CentralDirectory(DuplicatePolicy.LAST);
		assertArrayEquals(new byte[0], centralDirectory.getArchiveExtraData());
		assertArrayEquals(new byte[0], centralDirectory.getSignatureData());
		assertTrue(centralDirectory.getFileHeaders().isEmpty());
	}

	@Test
	void archiveExtraDataIsDefensivelyCopied() {
		CentralDirectory centralDirectory = new CentralDirectory(DuplicatePolicy.LAST);
		byte[] source = {0x01, 0x02};

		centralDirectory.setArchiveExtraData(source);
		source[0] = 0x7F;

		assertArrayEquals(new byte[]{0x01, 0x02}, centralDirectory.getArchiveExtraData());

		byte[] returned = centralDirectory.getArchiveExtraData();
		returned[1] = 0x7F;

		assertArrayEquals(new byte[]{0x01, 0x02}, centralDirectory.getArchiveExtraData());
	}

	@Test
	void signatureDataIsDefensivelyCopied() {
		CentralDirectory centralDirectory = new CentralDirectory(DuplicatePolicy.LAST);
		byte[] source = {0x11, 0x22};

		centralDirectory.setSignatureData(source);
		source[0] = 0x7F;

		assertArrayEquals(new byte[]{0x11, 0x22}, centralDirectory.getSignatureData());
	}

	@Test
	void fileHeaderListIsDefensivelyCopied() throws IOException {
		CentralDirectory centralDirectory = new CentralDirectory(DuplicatePolicy.LAST);

		GeneralFileHeader first = new GeneralFileHeader();
		first.setEntryPath("first.txt");

		List<GeneralFileHeader> source = new ArrayList<>();
		source.add(first);

		centralDirectory.setFileHeaders(source);

		GeneralFileHeader second = new GeneralFileHeader();
		second.setEntryPath("second.txt");
		source.add(second);

		assertEquals(1, centralDirectory.getFileHeaders().size());
		assertEquals("first.txt", centralDirectory.getFileHeaders().get(0).getEntryPath());

		assertThrows(UnsupportedOperationException.class, () -> centralDirectory.getFileHeaders().clear());
	}
}