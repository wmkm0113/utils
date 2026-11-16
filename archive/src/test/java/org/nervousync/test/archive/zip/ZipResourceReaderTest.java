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

import org.intellij.lang.annotations.MagicConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.zip.ZipOptions;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

final class ZipResourceReaderTest {

	@TempDir
	Path tempDirectory;
	private static final long OUT_OF_LIMIT_ERROR_CODE = 0x0000001A003EL;

	@Test
	void appliesHeaderLimitWhenOpeningEntryReader() throws Exception {
		String entryPath = "entry.txt";

		byte[] localExtraField = ByteBuffer
				.allocate(64)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) 0xCAFE)
				.putShort((short) 60)
				.put(new byte[60])
				.array();

		Path archivePath = createArchiveWithLocalOnlyExtraField(entryPath, localExtraField);
		int centralDirectorySize = 46 + entryPath.getBytes(StandardCharsets.UTF_8).length;

		ArchiveOptions options = ArchiveOptions.newBuilder()
				.limitBuilder()
				.maxHeaderBytes(centralDirectorySize)
				.confirm()
				.build();

		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(archivePath.toString(), "r"), options,
						     ZipOptions.newBuilder().build())) {
			ArchiveEntry archiveEntry = archiveFile.entry(entryPath).orElseThrow();
			ArchiveException exception =
					assertThrows(ArchiveException.class, () -> archiveFile.openReader(archiveEntry));
			assertEquals(OUT_OF_LIMIT_ERROR_CODE, exception.getErrorCode());
		}
	}

	@Test
	void readsStoredEntryThroughResourceReader() throws Exception {
		byte[] content = "stored entry content".getBytes(StandardCharsets.UTF_8);
		Path archivePath = createArchive("stored.txt", content, ZipEntry.STORED);

		assertEntryContent(archivePath, "stored.txt", content);
	}

	@Test
	void readsDeflatedEntryThroughResourceReader() throws Exception {
		byte[] content = "deflated entry content".repeat(64).getBytes(StandardCharsets.UTF_8);
		Path archivePath = createArchive("folder/deflated.txt", content, ZipEntry.DEFLATED);

		assertEntryContent(archivePath, "folder/deflated.txt", content);
	}

	@Test
	void continuesAfterEntryFailureRegardlessOfUnsafePolicy() throws Exception {
		byte[] brokenContent = "broken entry content".getBytes(StandardCharsets.UTF_8);
		byte[] validContent = "valid entry content".getBytes(StandardCharsets.UTF_8);
		Path archivePath = this.tempDirectory.resolve("continue-on-error.zip");
		try (OutputStream outputStream = Files.newOutputStream(archivePath);
		     ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
			writeStoredEntry(zipOutputStream, "broken.txt", brokenContent);
			writeStoredEntry(zipOutputStream, "valid.txt", validContent);
		}
		corruptFirstEntryCrc(archivePath);

		Path destination = this.tempDirectory.resolve("continued-output");
		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(archivePath.toString(), "r"), ZipOptions.newBuilder().build())) {
			archiveFile.extract(destination,
					ExtractOptions.newBuilder().continueOnError(true).build());
		}

		assertArrayEquals(validContent, Files.readAllBytes(destination.resolve("valid.txt")));
	}

	@Test
	void opensDifferentEntriesConcurrently() throws Exception {
		Path archivePath = this.tempDirectory.resolve("concurrent.zip");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archivePath))) {
			writeStoredEntry(output, "first.txt", new byte[] {1});
			writeStoredEntry(output, "second.txt", new byte[] {2});
		}

		try (ArchiveFile archiveFile = ArchiveFile.zipFile(
				new LocalDataFile(archivePath.toString(), "r"), ZipOptions.newBuilder().build())) {
			ArchiveEntry first = archiveFile.entry("first.txt").orElseThrow();
			ArchiveEntry second = archiveFile.entry("second.txt").orElseThrow();
			CyclicBarrier barrier = new CyclicBarrier(2);
			ExecutorService executor = Executors.newFixedThreadPool(2);
			try {
				ExecutorCompletionService<Void> completion = new ExecutorCompletionService<>(executor);
				completion.submit(() -> readRepeatedly(archiveFile, first, (byte) 1, barrier));
				completion.submit(() -> readRepeatedly(archiveFile, second, (byte) 2, barrier));
				for (int index = 0; index < 2; index++) {
					var completed = completion.poll(15, TimeUnit.SECONDS);
					assertNotNull(completed, "Concurrent entry read timed out");
					completed.get();
				}
			} finally {
				executor.shutdownNow();
				executor.awaitTermination(5, TimeUnit.SECONDS);
			}
		}
	}

	private static Void readRepeatedly(final ArchiveFile archiveFile, final ArchiveEntry entry,
	                                   final byte expected, final CyclicBarrier barrier) throws Exception {
		for (int index = 0; index < 100; index++) {
			barrier.await(5, TimeUnit.SECONDS);
			try (ResourceReader reader = archiveFile.openReader(entry).orElseThrow();
			     InputStream input = reader.openStream()) {
				assertArrayEquals(new byte[] {expected}, input.readAllBytes());
			}
		}
		return null;
	}

	private void assertEntryContent(final Path archivePath, final String entryPath, final byte[] expected)
			throws Exception {
		try (ArchiveFile archiveFile = ArchiveFile.zipFile(
				new LocalDataFile(archivePath.toString(), "r"), ZipOptions.newBuilder().build())) {
			ArchiveEntry archiveEntry = archiveFile.entry(entryPath).orElseThrow();
			assertEquals(entryPath, archiveEntry.getEntryPath());
			assertEquals(expected.length, archiveEntry.getUncompressedSize());

			try (ResourceReader resourceReader = archiveFile.openReader(archiveEntry).orElseThrow()) {
				assertEquals(entryPath, resourceReader.path());
				try (InputStream inputStream = resourceReader.openStream()) {
					assertArrayEquals(expected, inputStream.readAllBytes());
				}
			}
		}
	}

	private Path createArchive(final String entryPath, final byte[] content,
	                           @MagicConstant(intValues = {ZipEntry.STORED, ZipEntry.DEFLATED}) final int method)
			throws Exception {
		Path archivePath = this.tempDirectory.resolve(entryPath.replace('/', '-') + ".zip");
		try (OutputStream outputStream = Files.newOutputStream(archivePath);
		     ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
			ZipEntry zipEntry = new ZipEntry(entryPath);
			zipEntry.setMethod(method);
			if (method == ZipEntry.STORED) {
				CRC32 crc32 = new CRC32();
				crc32.update(content);
				zipEntry.setSize(content.length);
				zipEntry.setCompressedSize(content.length);
				zipEntry.setCrc(crc32.getValue());
			}
			zipOutputStream.putNextEntry(zipEntry);
			zipOutputStream.write(content);
			zipOutputStream.closeEntry();
		}
		return archivePath;
	}

	private static void writeStoredEntry(final ZipOutputStream outputStream, final String entryPath,
	                                     final byte[] content) throws Exception {
		CRC32 crc32 = new CRC32();
		crc32.update(content);
		ZipEntry zipEntry = new ZipEntry(entryPath);
		zipEntry.setMethod(ZipEntry.STORED);
		zipEntry.setSize(content.length);
		zipEntry.setCompressedSize(content.length);
		zipEntry.setCrc(crc32.getValue());
		outputStream.putNextEntry(zipEntry);
		outputStream.write(content);
		outputStream.closeEntry();
	}

	private static void corruptFirstEntryCrc(final Path archivePath) throws Exception {
		byte[] archiveData = Files.readAllBytes(archivePath);
		int localHeader = findSignature(archiveData, 0x04034B50);
		int centralHeader = findSignature(archiveData, 0x02014B50);
		if (localHeader < 0 || centralHeader < 0) {
			throw new AssertionError("ZIP headers not found");
		}
		clearInt(archiveData, localHeader + 14);
		clearInt(archiveData, centralHeader + 16);
		Files.write(archivePath, archiveData);
	}

	private static int findSignature(final byte[] data, final int signature) {
		for (int index = 0; index <= data.length - Integer.BYTES; index++) {
			if ((data[index] & 0xFF) == (signature & 0xFF)
					&& (data[index + 1] & 0xFF) == ((signature >>> 8) & 0xFF)
					&& (data[index + 2] & 0xFF) == ((signature >>> 16) & 0xFF)
					&& (data[index + 3] & 0xFF) == ((signature >>> 24) & 0xFF)) {
				return index;
			}
		}
		return -1;
	}

	private static void clearInt(final byte[] data, final int offset) {
		for (int index = 0; index < Integer.BYTES; index++) {
			data[offset + index] = 0;
		}
	}

	private Path createArchiveWithLocalOnlyExtraField(final String entryPath, final byte[] localExtraField)
			throws Exception {

		byte[] nameBytes = entryPath.getBytes(StandardCharsets.UTF_8);

		byte[] localHeader = ByteBuffer
				.allocate(30 + nameBytes.length + localExtraField.length)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putInt((int) ZipGlobals.LOCAL_FILE_HEADER_SIGNATURE)
				.putShort((short) 20)
				.putShort((short) 0x0800)
				.putShort((short) ZipGlobals.COMP_STORE)
				.putShort((short) 0)
				.putShort((short) 0)
				.putInt(0)
				.putInt(0)
				.putInt(0)
				.putShort((short) nameBytes.length)
				.putShort((short) localExtraField.length)
				.put(nameBytes)
				.put(localExtraField)
				.array();

		byte[] centralDirectory = ByteBuffer
				.allocate(46 + nameBytes.length)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putInt((int) ZipGlobals.GENERAL_FILE_HEADER_SIGNATURE)
				.putShort((short) 20)
				.putShort((short) 20)
				.putShort((short) 0x0800)
				.putShort((short) ZipGlobals.COMP_STORE)
				.putShort((short) 0)
				.putShort((short) 0)
				.putInt(0)
				.putInt(0)
				.putInt(0)
				.putShort((short) nameBytes.length)
				.putShort((short) 0)
				.putShort((short) 0)
				.putShort((short) 0)
				.putShort((short) 0)
				.putInt(0)
				.putInt(0)
				.put(nameBytes)
				.array();

		byte[] endRecord = ByteBuffer
				.allocate(22)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putInt((int) 0x06054B50L)
				.putShort((short) 0)
				.putShort((short) 0)
				.putShort((short) 1)
				.putShort((short) 1)
				.putInt(centralDirectory.length)
				.putInt(localHeader.length)
				.putShort((short) 0)
				.array();

		byte[] archiveData = ByteBuffer
				.allocate(localHeader.length + centralDirectory.length + endRecord.length)
				.put(localHeader)
				.put(centralDirectory)
				.put(endRecord)
				.array();

		Path archivePath = this.tempDirectory.resolve("local-header-limit.zip");

		Files.write(archivePath, archiveData);
		return archivePath;
	}
}
