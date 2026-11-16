package org.nervousync.test.archive.zip;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.EntryType;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/** ZIP method 12 integration and independent interoperability checks. */
final class ZipBZip2IntegrationTest {
	@TempDir Path directory;

	@Test
	void writesAndReadsBzip2Entry() throws Exception {
		Path path = directory.resolve("native.zip");
		byte[] bytes = payload(120017);
		ZipOptions options = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.BZIP2).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), options)) {
			writeEntry(archive, "payload.bin", bytes);
		}
		assertContent(path, options, Map.of("payload.bin", bytes));
	}

	private static Stream<Arguments> configurations() {
		return Stream.of(Layout.values()).flatMap(layout ->
				Stream.of(Zip64Mode.AUTO, Zip64Mode.ALWAYS).flatMap(mode ->
						Stream.of(Encryption.values()).map(encryption -> Arguments.of(layout, mode, encryption))));
	}

	@ParameterizedTest(name = "BZIP2 {0}, {1}, {2}")
	@MethodSource("configurations")
	void supportsEncryptionZip64AndSplitLayouts(Layout layout, Zip64Mode mode, Encryption encryption) throws Exception {
		Path path = directory.resolve("matrix.zip");
		ZipOptions options = options(CompressionAlgorithm.BZIP2, mode, layout, encryption);
		byte[] bytes = payload(120017);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "payload.bin", bytes);
			writeEntry(archive, "empty.bin", new byte[0]);
			writeEntry(archive, "folder/", new byte[0]);
		}
		assertContent(readablePath(path, layout), options,
				Map.of("payload.bin", bytes, "empty.bin", new byte[0]));
		try (ArchiveFile archive = ArchiveFile.zipFile(
				new LocalDataFile(readablePath(path, layout).toString(), "r"), archiveOptions(), options)) {
			var folder = archive.entry("folder/").orElseThrow();
			assertEquals(EntryType.DIRECTORY, folder.getEntryType());
			assertTrue(archive.openReader(folder).isEmpty());
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
	void writesCompleteBzhStreamAndIsReadableByCommonsCompress(int level) throws Exception {
		Path path = directory.resolve("reference.zip");
		byte[] bytes = "ZIP BZip2 interoperability\n".repeat(3000).getBytes(StandardCharsets.UTF_8);
		ZipOptions options = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.BZIP2)
				.compressionLevel(level).zip64Mode(Zip64Mode.ALWAYS).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), options)) {
			writeEntry(archive, "payload.bin", bytes);
		}
		byte[] raw = Files.readAllBytes(path);
		ByteBuffer header = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(46, Short.toUnsignedInt(header.getShort(4)));
		assertEquals(12, Short.toUnsignedInt(header.getShort(8)));
		int dataStart = 30 + Short.toUnsignedInt(header.getShort(26)) + Short.toUnsignedInt(header.getShort(28));
		assertArrayEquals(new byte[] {'B', 'Z', 'h', (byte) ('0' + Math.max(1, level))},
				java.util.Arrays.copyOfRange(raw, dataStart, dataStart + 4));
		try (ZipFile archive = ZipFile.builder().setPath(path).get()) {
			var entry = archive.getEntry("payload.bin");
			assertEquals(12, entry.getMethod());
			assertEquals(46, entry.getVersionRequired());
			CRC32 crc = new CRC32(); crc.update(bytes);
			assertEquals(crc.getValue(), entry.getCrc());
			try (InputStream in = archive.getInputStream(entry)) {
				assertArrayEquals(bytes, in.readAllBytes());
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void readsIndependentMethod12FixtureWithOptionalDataDescriptor(boolean descriptor) throws Exception {
		Path path = directory.resolve("external.zip");
		byte[] bytes = payload(240017);
		Files.write(path, fixture(bytes, descriptor, false, false));
		assertContent(path, ZipOptions.newBuilder().build(), Map.of("payload.bin", bytes));
	}

	@Test
	void detectsBzip2CrcAndZipCrcSeparately() throws Exception {
		byte[] bytes = payload(120017);
		for (boolean bzipCorruption : new boolean[] {false, true}) {
			Path path = directory.resolve(bzipCorruption ? "bad-bzip-crc.zip" : "bad-zip-crc.zip");
			Files.write(path, fixture(bytes, false, bzipCorruption, !bzipCorruption));
			assertThrows(IOException.class, () -> {
				try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"), ZipOptions.newBuilder().build());
				     ResourceReader reader = archive.openReader(archive.entry("payload.bin").orElseThrow()).orElseThrow();
				     InputStream input = reader.openStream()) {
					input.readAllBytes();
				}
			});
		}
	}

	@Test
	void preservesAesMethodMetadataAndVerifiesAuthentication() throws Exception {
		Path path = directory.resolve("aes.zip");
		byte[] bytes = payload(120017);
		ZipOptions options = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.BZIP2).aesCrypto(256).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			writeEntry(archive, "payload.bin", bytes);
		}
		assertContent(path, options, Map.of("payload.bin", bytes));
		byte[] raw = Files.readAllBytes(path);
		ByteBuffer buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(51, Short.toUnsignedInt(buffer.getShort(4)));
		assertEquals(99, Short.toUnsignedInt(buffer.getShort(8)));
		int nameLength = Short.toUnsignedInt(buffer.getShort(26));
		int extraLength = Short.toUnsignedInt(buffer.getShort(28));
		boolean found = false;
		for (int extra = 30 + nameLength, end = extra + extraLength; extra < end;) {
			int id = Short.toUnsignedInt(buffer.getShort(extra));
			int length = Short.toUnsignedInt(buffer.getShort(extra + 2));
			if (id == 0x9901) {
				assertEquals(12, Short.toUnsignedInt(buffer.getShort(extra + 9)));
				found = true;
			}
			extra += 4 + length;
		}
		assertTrue(found);
		int central = buffer.getInt(raw.length - 6);
		int compressedSize = buffer.getInt(central + 20);
		int dataStart = 30 + nameLength + extraLength;
		raw[dataStart + compressedSize - 1] ^= (byte) 0x80; // corrupt the AES authentication trailer
		Files.write(path, raw);
		assertThrows(IOException.class, () -> assertContent(path, options, Map.of("payload.bin", bytes)));
	}

	@Test
	void preservesBzip2EntryAcrossUpdateAndDeletion() throws Exception {
		Path path = directory.resolve("edit.zip");
		byte[] kept = payload(12345), replacement = payload(7777);
		ZipOptions options = ZipOptions.newBuilder().compressionAlgorithm(CompressionAlgorithm.BZIP2).build();
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), options)) {
			writeEntry(archive, "keep.bin", kept);
			writeEntry(archive, "change.bin", new byte[] {1, 2});
			writeEntry(archive, "delete.bin", new byte[] {3});
		}
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), options)) {
			writeEntry(archive, "change.bin", replacement);
			archive.remove(archive.entry("delete.bin").orElseThrow());
		}
		assertContent(path, options, Map.of("keep.bin", kept, "change.bin", replacement));
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"), options)) {
			assertTrue(archive.entry("delete.bin").isEmpty());
		}
	}

	@Test
	void enforcesOutputLimitsAndAlwaysChecksInternalBzip2Crc() throws Exception {
		byte[] bytes = payload(1001);
		Path path = directory.resolve("limited.zip");
		Files.write(path, fixture(bytes, false, false, false));
		ArchiveOptions limits = ArchiveOptions.newBuilder().limitBuilder().maxEntryBytes(1000).confirm().build();
		assertThrows(IOException.class, () -> {
			try (ArchiveFile ignored = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"), limits,
					ZipOptions.newBuilder().build())) { }
		});
		Files.write(path, fixture(bytes, false, true, false));
		ArchiveOptions noZipCrc = ArchiveOptions.newBuilder().verify(false).build();
		assertThrows(IOException.class, () -> {
			try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"), noZipCrc,
					ZipOptions.newBuilder().build());
			     ResourceReader reader = archive.openReader(archive.entry("payload.bin").orElseThrow()).orElseThrow();
			     InputStream input = reader.openStream()) { input.readAllBytes(); }
		});
	}

	/** Create a ZIP envelope independently; compressed data comes from Commons Compress. */
	private static byte[] fixture(byte[] original, boolean descriptor, boolean badBzipCrc, boolean badZipCrc) throws Exception {
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		try (OutputStream encoder = new BZip2CompressorOutputStream(compressed, 1)) { encoder.write(original); }
		byte[] payload = compressed.toByteArray();
		if (badBzipCrc) payload[10] ^= 1;
		CRC32 crc32 = new CRC32(); crc32.update(original);
		int crc = (int) crc32.getValue() ^ (badZipCrc ? 1 : 0);
		byte[] name = "payload.bin".getBytes(StandardCharsets.UTF_8);
		int flags = descriptor ? 0x0008 : 0;
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ByteBuffer local = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
		local.putInt(0x04034B50).putShort((short) 46).putShort((short) flags).putShort((short) 12)
				.putShort((short) 0).putShort((short) 0).putInt(descriptor ? 0 : crc)
				.putInt(descriptor ? 0 : payload.length).putInt(descriptor ? 0 : original.length)
				.putShort((short) name.length).putShort((short) 0);
		bytes.write(local.array()); bytes.write(name); bytes.write(payload);
		if (descriptor) {
			bytes.write(ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
					.putInt(0x08074B50).putInt(crc).putInt(payload.length).putInt(original.length).array());
		}
		int centralStart = bytes.size();
		ByteBuffer central = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN);
		central.putInt(0x02014B50).putShort((short) 46).putShort((short) 46).putShort((short) flags)
				.putShort((short) 12).putShort((short) 0).putShort((short) 0).putInt(crc)
				.putInt(payload.length).putInt(original.length).putShort((short) name.length)
				.putShort((short) 0).putShort((short) 0).putShort((short) 0).putShort((short) 0)
				.putInt(0).putInt(0);
		bytes.write(central.array()); bytes.write(name);
		int centralSize = bytes.size() - centralStart;
		bytes.write(ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN).putInt(0x06054B50)
				.putShort((short) 0).putShort((short) 0).putShort((short) 1).putShort((short) 1)
				.putInt(centralSize).putInt(centralStart).putShort((short) 0).array());
		return bytes.toByteArray();
	}
}
