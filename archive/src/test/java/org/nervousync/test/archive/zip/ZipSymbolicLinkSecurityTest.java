/*
 * Licensed to the Nervousync Studio (NSYC) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.enumerations.UnsafePolicy;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.utils.commons.RawUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Symbolic-link extraction security tests.
 */
final class ZipSymbolicLinkSecurityTest {

	/**
	 * ZIP local file header signature.
	 */
	private static final int LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50;

	/**
	 * ZIP central directory file header signature.
	 */
	private static final int CENTRAL_DIRECTORY_HEADER_SIGNATURE = 0x02014B50;

	/**
	 * ZIP "version made by" host identifier for Unix.
	 */
	private static final int UNIX_HOST_SYSTEM = 3;

	/**
	 * ZIP version 2.0.
	 */
	private static final int ZIP_VERSION_2_0 = 20;

	/**
	 * Unix symbolic-link mode: 0xA000 和 0x01FF 合并为 0xA1FF
	 */
	private static final int UNIX_SYMBOLIC_LINK_MODE = 0xA1FF;

	/**
	 * Symbolic-link target escaped the extraction root.
	 */
	private static final long UNSAFE_SYMBOLIC_LINK_ERROR_CODE = 0x0000001A0037L;
	/**
	 * Extracted data length does not match the declared uncompressed size.
	 */
	private static final long DATA_LENGTH_MISMATCH_ERROR_CODE = 0x0000001A002FL;

	/**
	 * CRC-32 verification failed.
	 */
	private static final long CRC32_CHECK_FAILED_ERROR_CODE = 0x0000001A0032L;
	/**
	 * CRC-32 field offset in a local file header.
	 */
	private static final int LOCAL_HEADER_CRC_OFFSET = 14;
	/**
	 * CRC-32 field offset in a central-directory file header.
	 */
	private static final int CENTRAL_DIRECTORY_CRC_OFFSET = 16;

	/**
	 * Uncompressed-size field offset in a local file header.
	 */
	private static final int LOCAL_HEADER_UNCOMPRESSED_SIZE_OFFSET = 22;

	/**
	 * Uncompressed-size field offset in a central-directory file header.
	 */
	private static final int CENTRAL_DIRECTORY_UNCOMPRESSED_SIZE_OFFSET = 24;

	@TempDir
	Path tempDirectory;

	/**
	 * UnsafePolicy.REJECT must not be overridden by continueOnError.
	 */
	@Test
	void rejectsSymbolicLinkThroughExistingExternalLinkEvenWhenContinueOnError() throws Exception {
		Path outsideDirectory = Files.createDirectories(this.tempDirectory.resolve("outside-reject"));
		Path extractionRoot = Files.createDirectories(this.tempDirectory.resolve("output-reject"));

		/*
		 * output-reject/bridge -> outside-reject
		 */
		createSymbolicLinkOrSkip(extractionRoot.resolve("bridge"), outsideDirectory.toAbsolutePath());

		Path archivePath = createSymbolicLinkArchive("unsafe-link.txt", "bridge/secret.txt");
		ExtractOptions extractOptions = ExtractOptions.newBuilder()
				.allowSymbolicLinks(true)
				.unsafePolicy(UnsafePolicy.REJECT)
				.continueOnError(true)
				.build();

		ArchiveException exception;
		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(archivePath.toString(), "r"), ZipOptions.newBuilder().build())) {
			exception = assertThrows(ArchiveException.class, () -> archiveFile.extract(extractionRoot, extractOptions));
		}

		assertEquals(UNSAFE_SYMBOLIC_LINK_ERROR_CODE, exception.getErrorCode());
		assertFalse(Files.exists(extractionRoot.resolve("unsafe-link.txt"), LinkOption.NOFOLLOW_LINKS));
	}

	/**
	 * UnsafePolicy.SKIP must silently skip the unsafe symbolic-link entry.
	 */
	@Test
	void skipsSymbolicLinkThroughExistingExternalLink() throws Exception {
		Path outsideDirectory = Files.createDirectories(this.tempDirectory.resolve("outside-skip"));
		Path extractionRoot = Files.createDirectories(this.tempDirectory.resolve("output-skip"));

		/*
		 * output-skip/bridge -> outside-skip
		 */
		createSymbolicLinkOrSkip(extractionRoot.resolve("bridge"), outsideDirectory.toAbsolutePath());

		Path archivePath = createSymbolicLinkArchive("skipped-link.txt", "bridge/secret.txt");

		ExtractOptions extractOptions = ExtractOptions.newBuilder()
				.allowSymbolicLinks(true)
				.unsafePolicy(UnsafePolicy.SKIP)
				.continueOnError(false)
				.build();

		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(archivePath.toString(), "r"), ZipOptions.newBuilder().build())) {
			assertDoesNotThrow(() -> archiveFile.extract(extractionRoot, extractOptions));
		}

		assertFalse(Files.exists(extractionRoot.resolve("skipped-link.txt"), LinkOption.NOFOLLOW_LINKS));
	}

	@Test
	void keepsSymbolicLinkInsideRootAfterArchiveReplacesIntermediateLink() throws Exception {
		Path root = Files.createDirectory(tempDirectory.resolve("rebind-out"));
		Files.createDirectory(root.resolve("sub"));
		Path inside = Files.writeString(root.resolve("secret.txt"), "inside");
		Files.writeString(tempDirectory.resolve("secret.txt"), "outside");
		createSymbolicLinkOrSkip(root.resolve("bridge"), Path.of("sub"));

		Path archive = ZipTestFixtures.write(tempDirectory.resolve("rebind.zip"),
				ZipTestFixtures.symbolicLink("view", "bridge/../secret.txt"),
				ZipTestFixtures.symbolicLink("bridge", "."));

		try (ArchiveFile zip =
				     ArchiveFile.zipFile(new LocalDataFile(archive.toString(), "r"), ZipOptions.newBuilder().build())) {
			zip.extract(root, ExtractOptions.newBuilder()
					.allowSymbolicLinks(true)
					.overrideMode(OverrideMode.ALWAYS)
					.preserveLastModified(false)
					.build());
		}

		Path view = root.resolve("view");
		assertEquals(root.toRealPath(), root.resolve("bridge").toRealPath());
		assertTrue(Files.isSymbolicLink(view));
		assertEquals(Path.of("secret.txt"), Files.readSymbolicLink(view));
		assertEquals(inside.toRealPath(), view.toRealPath());
		assertEquals("inside", Files.readString(view));
	}

	/**
	 * A pre-existing symbolic-link component is acceptable when its real path
	 * remains inside the extraction root.
	 */
	@Test
	void allowsSymbolicLinkThroughExistingInternalLink() throws Exception {
		byte[] expectedContent = "internal symbolic-link content".getBytes(StandardCharsets.UTF_8);

		Path extractionRoot = Files.createDirectories(this.tempDirectory.resolve("output-internal"));
		Path internalDirectory = Files.createDirectories(extractionRoot.resolve("internal"));

		Files.write(internalDirectory.resolve("secret.txt"), expectedContent);

		/*
		 * output-internal/bridge -> output-internal/internal
		 */
		createSymbolicLinkOrSkip(extractionRoot.resolve("bridge"), internalDirectory.toAbsolutePath());

		Path archivePath = createSymbolicLinkArchive("safe-link.txt", "bridge/secret.txt");

		ExtractOptions extractOptions = ExtractOptions.newBuilder()
				.allowSymbolicLinks(true)
				.unsafePolicy(UnsafePolicy.REJECT)
				.continueOnError(false)
				.build();

		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(archivePath.toString(), "r"), ZipOptions.newBuilder().build())) {
			archiveFile.extract(extractionRoot, extractOptions);
		}

		Path extractedLink = extractionRoot.resolve("safe-link.txt");

		assertTrue(Files.isSymbolicLink(extractedLink));
		assertEquals(internalDirectory.resolve("secret.txt").toRealPath(), extractedLink.toRealPath());
//		assertEquals(Paths.get("bridge/secret.txt"), Files.readSymbolicLink(extractedLink));
		assertArrayEquals(expectedContent, Files.readAllBytes(extractedLink));
	}

	/**
	 * Symbolic-link entry data must pass CRC-32 verification before the
	 * symbolic link is created.
	 */
	@Test
	void rejectsSymbolicLinkWithInvalidCrc() throws Exception {
		String entryPath = "invalid-crc-link.txt";

		Path archivePath = createSymbolicLinkArchive(entryPath, "existing-target.txt");
		corruptCrc(archivePath);
		Path extractionRoot = Files.createDirectories(this.tempDirectory.resolve("output-invalid-crc"));

		ExtractOptions extractOptions = ExtractOptions.newBuilder()
				.allowSymbolicLinks(true)
				.unsafePolicy(UnsafePolicy.REJECT)
				.continueOnError(false)
				.build();

		ArchiveException exception;

		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(archivePath.toString(), "r"), ZipOptions.newBuilder().build())) {
			exception = assertThrows(ArchiveException.class,
					() -> archiveFile.extract(extractionRoot, extractOptions));
		}

		assertEquals(CRC32_CHECK_FAILED_ERROR_CODE, exception.getErrorCode(),
				"Invalid symbolic-link data CRC must be rejected");
		assertFalse(Files.exists(extractionRoot.resolve(entryPath), LinkOption.NOFOLLOW_LINKS),
				"A symbolic link must not be created when CRC verification fails");
	}

	/**
	 * Symbolic-link entry data length must exactly match the uncompressed
	 * size declared in the ZIP headers.
	 */
	@Test
	void rejectsSymbolicLinkWithMismatchedUncompressedSize() throws Exception {
		String entryPath = "invalid-size-link.txt";
		Path archivePath = createSymbolicLinkArchive(entryPath, "existing-target.txt");

		increaseDeclaredUncompressedSize(archivePath);

		Path extractionRoot = Files.createDirectories(this.tempDirectory.resolve("output-invalid-size"));

		ExtractOptions extractOptions = ExtractOptions.newBuilder()
				.allowSymbolicLinks(true)
				.unsafePolicy(UnsafePolicy.REJECT)
				.continueOnError(false)
				.build();

		ArchiveException exception;

		try (ArchiveFile archiveFile =
				     ArchiveFile.zipFile(new LocalDataFile(archivePath.toString(), "r"), ZipOptions.newBuilder().build())) {
			exception = assertThrows(ArchiveException.class, () -> archiveFile.extract(extractionRoot, extractOptions));
		}

		assertEquals(DATA_LENGTH_MISMATCH_ERROR_CODE, exception.getErrorCode(),
				"A truncated symbolic-link entry must be rejected");
		assertFalse(Files.exists(extractionRoot.resolve(entryPath), LinkOption.NOFOLLOW_LINKS),
				"A symbolic link must not be created when its data length is invalid");
	}

	/**
	 * Create a ZIP containing one Unix symbolic-link entry.
	 * java.util.zip.ZipEntry does not expose Unix file-mode attributes, so the
	 * generated central-directory header is patched after creation.
	 *
	 * @param entryPath symbolic-link entry path
	 * @param linkTarget symbolic-link target stored as entry data
	 * @return generated ZIP path
	 */
	private Path createSymbolicLinkArchive(final String entryPath, final String linkTarget) throws IOException {
		Path archivePath = this.tempDirectory.resolve(entryPath.replace('/', '-') + ".zip");

		byte[] linkTargetBytes = linkTarget.getBytes(StandardCharsets.UTF_8);

		CRC32 crc32 = new CRC32();
		crc32.update(linkTargetBytes);

		ZipEntry zipEntry = new ZipEntry(entryPath);
		zipEntry.setMethod(ZipEntry.STORED);
		zipEntry.setSize(linkTargetBytes.length);
		zipEntry.setCompressedSize(linkTargetBytes.length);
		zipEntry.setCrc(crc32.getValue());

		try (OutputStream outputStream = Files.newOutputStream(archivePath);
		     ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
			zipOutputStream.putNextEntry(zipEntry);
			zipOutputStream.write(linkTargetBytes);
			zipOutputStream.closeEntry();
		}

		markEntryAsUnixSymbolicLink(archivePath);

		return archivePath;
	}

	/**
	 * Patch the first central-directory entry so that it is recognized as a
	 * Unix symbolic link.
	 * Central-directory header offsets:
	 * - +4: version made by, two bytes
	 * - +38: external file attributes, four bytes
	 *
	 * @param archivePath ZIP path
	 */
	private static void markEntryAsUnixSymbolicLink(final Path archivePath) throws IOException {
		byte[] archiveData = Files.readAllBytes(archivePath);

		int localHeaderOffset = findSignature(archiveData, LOCAL_FILE_HEADER_SIGNATURE);
		int centralHeaderOffset = findSignature(archiveData, CENTRAL_DIRECTORY_HEADER_SIGNATURE);

		if (localHeaderOffset < 0 || centralHeaderOffset < 0) {
			throw new AssertionError("ZIP file headers were not found");
		}

		/*
		 * version made by:
		 *
		 * low byte  = ZIP specification version
		 * high byte = originating host system
		 */
		archiveData[centralHeaderOffset + 4] = (byte) ZIP_VERSION_2_0;
		archiveData[centralHeaderOffset + 5] = (byte) UNIX_HOST_SYSTEM;

		/*
		 * Unix mode is stored in the high 16 bits of external attributes.
		 */
		int externalAttributes = UNIX_SYMBOLIC_LINK_MODE << 16;

		writeIntLittleEndian(archiveData, centralHeaderOffset + 38, externalAttributes);
		Files.write(archivePath, archiveData);
	}

	/**
	 * Create a symbolic link or skip the test when the current platform or
	 * process does not permit symbolic-link creation.
	 *
	 * @param link symbolic-link path
	 * @param target symbolic-link target
	 */
	private static void createSymbolicLinkOrSkip(final Path link, final Path target) {
		try {
			Files.createSymbolicLink(link, target);
		} catch (UnsupportedOperationException | SecurityException | IOException e) {
			Assumptions.assumeTrue(false, "Symbolic-link creation is unavailable: " + e.getMessage());
		}
	}

	/**
	 * Find a little-endian ZIP signature.
	 *
	 * @param data ZIP bytes
	 * @param signature signature value
	 * @return signature offset, or -1 when not found
	 */
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

	/**
	 * Write an integer using little-endian byte order.
	 *
	 * @param data target byte array
	 * @param offset write offset
	 * @param value integer value
	 */
	private static void writeIntLittleEndian(final byte[] data, final int offset, final int value) {
		data[offset] = (byte) value;
		data[offset + 1] = (byte) (value >>> 8);
		data[offset + 2] = (byte) (value >>> 16);
		data[offset + 3] = (byte) (value >>> 24);
	}

	/**
	 * Modify the CRC-32 value stored in the first central-directory header.
	 *
	 * <p>The entry data and the CRC in the local header remain unchanged.
	 * The central-directory CRC is used as the expected CRC during
	 * extraction.</p>
	 *
	 * @param archivePath ZIP archive path
	 * @throws IOException if the archive cannot be read or written
	 */
	private static void corruptCrc(final Path archivePath) throws IOException {
		byte[] archiveData = Files.readAllBytes(archivePath);
		int localHeaderOffset = findSignature(archiveData, LOCAL_FILE_HEADER_SIGNATURE);
		int centralHeaderOffset = findSignature(archiveData, CENTRAL_DIRECTORY_HEADER_SIGNATURE);
		if (localHeaderOffset < 0 || centralHeaderOffset < 0) {
			throw new AssertionError("ZIP local or central-directory header was not found");
		}
		int localCrcOffset = localHeaderOffset + LOCAL_HEADER_CRC_OFFSET;
		int centralCrcOffset = centralHeaderOffset + CENTRAL_DIRECTORY_CRC_OFFSET;

		int localCrc = RawUtils.readInt(archiveData, localCrcOffset, ByteOrder.LITTLE_ENDIAN);
		int centralCrc = RawUtils.readInt(archiveData, centralCrcOffset, ByteOrder.LITTLE_ENDIAN);
		/*
		 * 原始 ZIP 的 Local Header 与 Central Directory CRC 应当相同。
		 */
		if (localCrc != centralCrc) {
			throw new AssertionError("Local and central CRC values are inconsistent before corruption");
		}
		/*
		 * 同时修改两处 CRC，保证 Header 一致性校验通过，
		 * 但计算出的真实数据 CRC 与 Header CRC 不一致。
		 */
		int corruptedCrc = localCrc ^ 0x00000001;

		writeIntLittleEndian(archiveData, localCrcOffset, corruptedCrc);
		writeIntLittleEndian(archiveData, centralCrcOffset, corruptedCrc);

		Files.write(archivePath, archiveData);
	}

	/**
	 * Increase the uncompressed size declared by the local and central
	 * headers without changing the actual entry data.
	 *
	 * @param archivePath ZIP archive path
	 * @throws IOException if the archive cannot be read or written
	 */
	private static void increaseDeclaredUncompressedSize(final Path archivePath) throws IOException {
		byte[] archiveData = Files.readAllBytes(archivePath);

		int localHeaderOffset = findSignature(archiveData, LOCAL_FILE_HEADER_SIGNATURE);
		int centralHeaderOffset = findSignature(archiveData, CENTRAL_DIRECTORY_HEADER_SIGNATURE);

		if (localHeaderOffset < 0 || centralHeaderOffset < 0) {
			throw new AssertionError("ZIP local or central-directory header was not found");
		}

		int localSizeOffset = localHeaderOffset + LOCAL_HEADER_UNCOMPRESSED_SIZE_OFFSET;

		int centralSizeOffset = centralHeaderOffset + CENTRAL_DIRECTORY_UNCOMPRESSED_SIZE_OFFSET;

		long localSize = RawUtils.readUnsignedInt(archiveData, localSizeOffset, ByteOrder.LITTLE_ENDIAN);
		long centralSize = RawUtils.readUnsignedInt(archiveData, centralSizeOffset, ByteOrder.LITTLE_ENDIAN);

		if (localSize == 0xFFFFFFFFL || centralSize == 0xFFFFFFFFL) {
			throw new AssertionError("The test helper does not support ZIP64 size fields");
		}

		if (localSize >= 0xFFFFFFFEL || centralSize >= 0xFFFFFFFEL) {
			throw new AssertionError("The test entry size cannot be safely increased");
		}

		writeIntLittleEndian(archiveData, localSizeOffset, (int) (localSize + 1L));
		writeIntLittleEndian(archiveData, centralSizeOffset, (int) (centralSize + 1L));

		Files.write(archivePath, archiveData);
	}
}