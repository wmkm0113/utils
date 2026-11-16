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
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.central.EndCentralDirectoryRecord;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.operator.ZipFileOperator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class Zip64V2DetectionTest {

	@TempDir
	Path tempDirectory;

	@Test
	void fallbackToV1WhenVersion62PayloadIsNotV2() throws Exception {
		byte[] extensibleData = new byte[32];
		Arrays.fill(extensibleData, (byte) 0x5A);

		Zip64EndCentralDirectory result = this.parse(extensibleData, false, true);

		assertNull(result.getProtection());
		assertArrayEquals(extensibleData, result.getRecord().getExtensibleDataSector());
	}

	@Test
	void detectCompressionOnlyV2() throws Exception {
		byte[] extensibleData = {
				0x34, 0x12, 0x00, 0x00, 0x00, 0x00
		};

		byte[] v2Data = v2Data(ZipGlobals.COMP_DEFLATE, 128L, 256L,
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE, 0, 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE, 0, new byte[0], extensibleData);

		Zip64EndCentralDirectory result = this.parse(v2Data, true, true);

		Zip64EndCentralDirectory.Protection protection = result.getProtection();
		assertNotNull(protection);

		assertEquals(ZipGlobals.COMP_DEFLATE, protection.getCompressionMethod());
		assertEquals(128L, protection.getCompressedSize());
		assertEquals(256L, protection.getUncompressedSize());
		assertEquals(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE, protection.getAlgorithm());
		assertArrayEquals(extensibleData, result.getRecord().getExtensibleDataSector());
	}

	@Test
	void detectEncryptionOnlyV2() throws Exception {
		byte[] v2Data = v2Data(ZipGlobals.COMP_STORE, 128L, 128L,
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128, 128,
				ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD, ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE,
				0, new byte[0], new byte[0]);

		Zip64EndCentralDirectory result = this.parse(v2Data, true, true);

		Zip64EndCentralDirectory.Protection protection = result.getProtection();
		assertNotNull(protection);

		assertEquals(ZipGlobals.COMP_STORE, protection.getCompressionMethod());
		assertEquals(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_128, protection.getAlgorithm());
		assertEquals(128, protection.getKeyBitLength());
		assertEquals(ZipGlobals.ZIP64_HEADER_ENC_FLAGS_PASSWORD, protection.getFlags());
		assertEquals(0, result.getRecord().getExtensibleDataSector().length);
	}

	@Test
	void detectHashOnlyV2() throws Exception {
		byte[] hashData = new byte[32];
		for (int i = 0; i < hashData.length; i++) {
			hashData[i] = (byte) i;
		}

		byte[] v2Data = v2Data(ZipGlobals.COMP_STORE, 256L, 256L,
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE, 0, 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256, hashData.length, hashData, new byte[0]);

		Zip64EndCentralDirectory result = this.parse(v2Data, true, true);

		Zip64EndCentralDirectory.Protection protection = result.getProtection();
		assertNotNull(protection);

		assertEquals(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256, protection.getHashAlgorithm());
		assertArrayEquals(hashData, protection.getHashData());
	}

	@Test
	void fallbackToV1WhenV2HasNoFeature() throws Exception {
		byte[] v2LikeData = v2Data(ZipGlobals.COMP_STORE, 128L, 128L,
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE, 0, 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE, 0, new byte[0], new byte[0]);

		Zip64EndCentralDirectory result = this.parse(v2LikeData, false, true);

		assertNull(result.getProtection());
		assertArrayEquals(v2LikeData, result.getRecord().getExtensibleDataSector());
	}

	@Test
	void fallbackToV1WhenHashLengthExceedsRecord() throws Exception {
		byte[] actualHashData = new byte[32];

		byte[] malformedV2Data = v2Data(ZipGlobals.COMP_STORE, 256L, 256L,
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE, 0, 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256, 33, actualHashData, new byte[0]);

		Zip64EndCentralDirectory result = this.parse(malformedV2Data, true, true);

		assertNull(result.getProtection());
		assertArrayEquals(malformedV2Data, result.getRecord().getExtensibleDataSector());
	}

	@Test
	void compatibilityModeAcceptsV2WithMissingDirectoryMarkers()
			throws Exception {
		byte[] v2Data = v2Data(ZipGlobals.COMP_DEFLATE, 128L, 256L,
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE, 0, 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE, 0, new byte[0], new byte[0]);
		Zip64EndCentralDirectory result = this.parse(v2Data, false, false);
		assertNotNull(result.getProtection());
	}

	@Test
	void strictModeRejectsV2WithMissingDirectoryMarkers() {
		byte[] v2Data = v2Data(ZipGlobals.COMP_DEFLATE, 128L, 256L,
				ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_NONE, 0, 0,
				ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_NONE, 0, new byte[0], new byte[0]);

		assertThrows(ArchiveException.class, () -> this.parse(v2Data, false, true));
	}

	private Zip64EndCentralDirectory parse(final byte[] variableData, final boolean validDirectoryMarkers,
	                                       final boolean strict) throws Exception {
		Path archivePath = Files.createTempFile(this.tempDirectory, "zip64-v2-", ".zip");

		Files.write(archivePath, archiveData(variableData, validDirectoryMarkers));

		try (ZipFileOperator operator =
				     new ZipFileOperator(new LocalDataFile(archivePath.toString(), "r"), ArchiveOptions.newBuilder().strict(strict).build(),
						     ZipOptions.newBuilder().build())) {
			EndCentralDirectoryRecord endRecord = operator.readEndCentralDirectoryRecord();
			return operator.readZip64EndCentralDirectory(endRecord);
		}
	}

	private static byte[] archiveData(final byte[] variableData, final boolean validDirectoryMarkers) {
		int zip64RecordLength = 56 + variableData.length;
		int totalLength = zip64RecordLength + 20 + 22;

		long entriesOnThisDisk = validDirectoryMarkers ? 0L : 1L;
		int eocdEntriesOnThisDisk = validDirectoryMarkers ? 0 : 1;
		int eocdTotalEntries = validDirectoryMarkers ? 0xFFFF : 1;

		ByteBuffer buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);

		// ZIP64 End of Central Directory Record
		buffer.putInt((int) ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE);
		buffer.putLong(44L + variableData.length);
		buffer.putShort((short) 63); // version made by 6.3
		buffer.putShort((short) 62); // version needed 6.2
		buffer.putInt(0);            // current disk
		buffer.putInt(0);            // central-directory start disk
		buffer.putLong(entriesOnThisDisk);
		buffer.putLong(1L);          // actual total entries
		buffer.putLong(0L);          // central-directory size
		buffer.putLong(0L);          // central-directory offset
		buffer.put(variableData);

		// ZIP64 End of Central Directory Locator
		buffer.putInt((int) ZipGlobals.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE);
		buffer.putInt(0);            // ZIP64 EOCD disk
		buffer.putLong(0L);          // ZIP64 EOCD offset
		buffer.putInt(1);            // total disks

		// End of Central Directory Record
		buffer.putInt((int) ZipGlobals.END_OF_CENTRAL_DIRECTORY_SIGNATURE);
		buffer.putShort((short) 0);
		buffer.putShort((short) 0);
		buffer.putShort((short) eocdEntriesOnThisDisk);
		buffer.putShort((short) eocdTotalEntries);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putShort((short) 0);

		return buffer.array();
	}

	private static byte[] v2Data(final int compressionMethod, final long compressedSize, final long uncompressedSize,
	                             final int algorithm, final int bitLength, final int flags, final int hashAlgorithm,
	                             final int declaredHashLength, final byte[] hashData, final byte[] extensibleData) {
		ByteBuffer buffer = ByteBuffer
				.allocate(28 + hashData.length + extensibleData.length)
				.order(ByteOrder.LITTLE_ENDIAN);

		buffer.putShort((short) compressionMethod);
		buffer.putLong(compressedSize);
		buffer.putLong(uncompressedSize);
		buffer.putShort((short) algorithm);
		buffer.putShort((short) bitLength);
		buffer.putShort((short) flags);
		buffer.putShort((short) hashAlgorithm);
		buffer.putShort((short) declaredHashLength);
		buffer.put(hashData);
		buffer.put(extensibleData);

		return buffer.array();
	}
}