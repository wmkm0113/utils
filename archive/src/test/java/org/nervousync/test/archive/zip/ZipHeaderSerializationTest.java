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
import org.nervousync.archive.enumerations.AesVendor;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.zip.models.ExtraDataRecord;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.models.crypto.AesEncryptionInfo;
import org.nervousync.archive.zip.models.header.FileHeader;
import org.nervousync.archive.zip.models.header.GeneralFileHeader;
import org.nervousync.archive.zip.models.header.LocalFileHeader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ZipHeaderSerializationTest {

	@Test
	void centralHeaderWritesFlagsBeforeCompressionMethod() throws Exception {
		GeneralFileHeader header = new GeneralFileHeader();
		header.setEntryPath("entry.txt");
		header.setMadeVersion(20);
		header.setExtractNeeded(20);
		header.setGeneralPurpose(0x0800);
		header.setCompressionMethod(8);
		ByteBuffer bytes = ByteBuffer.wrap(header.toByteArray(StandardCharsets.UTF_8, Zip64Mode.AUTO))
				.order(ByteOrder.LITTLE_ENDIAN);
		assertAll(() -> assertEquals(0x02014B50, bytes.getInt(0)),
				() -> assertEquals(0x0800, Short.toUnsignedInt(bytes.getShort(8))),
				() -> assertEquals(8, Short.toUnsignedInt(bytes.getShort(10))));
	}

	@Test
	void aesLocalHeaderSerializesMethod99WithoutChangingDecodedMethod() throws Exception {
		LocalFileHeader header = new LocalFileHeader();
		configureDecodedAesHeader(header);
		byte[] bytes = header.toByteArray(StandardCharsets.UTF_8, Zip64Mode.AUTO);
		assertEquals(99, Short.toUnsignedInt(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort(8)));
		assertEquals(8, header.getCompressionMethod());
	}

	@Test
	void aesCentralHeaderSerializesMethod99WithoutChangingDecodedMethod() throws Exception {
		GeneralFileHeader header = new GeneralFileHeader();
		configureDecodedAesHeader(header);
		ByteBuffer bytes = ByteBuffer.wrap(header.toByteArray(StandardCharsets.UTF_8, Zip64Mode.AUTO))
				.order(ByteOrder.LITTLE_ENDIAN);
		assertAll(() -> assertEquals(0x0801, Short.toUnsignedInt(bytes.getShort(8))),
				() -> assertEquals(99, Short.toUnsignedInt(bytes.getShort(10))),
				() -> assertEquals(8, header.getCompressionMethod()));
	}

	@Test
	void zip64EndRecordLengthExcludesOnlyItsLeadingTwelveBytes() throws Exception {
		Zip64EndCentralDirectory.Record record = new Zip64EndCentralDirectory.Record();
		byte[] bytes = record.toByteArray(null);
		assertEquals(56, bytes.length);
		ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(0x06064B50, buffer.getInt());
		assertEquals(44L, buffer.getLong());
	}

	private static void configureDecodedAesHeader(final FileHeader header) throws Exception {
		header.setEntryPath("entry.txt");
		header.setMadeVersion(51);
		header.setExtractNeeded(51);
		header.setGeneralPurpose(0x0801);
		header.setCompressionMethod(8);
		header.setCompressedSize(45L);
		header.setUncompressedSize(17L);
		header.setCrc32(0L);
		header.setEncryptionInfo(new AesEncryptionInfo(AesVendor.AE_2, 3));
		header.setExtraDataRecords(List.of(new ExtraDataRecord(0x9901, new byte[]{2, 0, 'A', 'E', 3, 8, 0})));
	}
}
