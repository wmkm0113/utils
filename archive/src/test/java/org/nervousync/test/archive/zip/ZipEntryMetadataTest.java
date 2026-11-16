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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.EntryType;
import org.nervousync.archive.enumerations.SplitType;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.EntryTimes;
import org.nervousync.archive.zip.models.ExtraDataRecord;
import org.nervousync.archive.zip.models.header.LocalFileHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** ZIP entry metadata round trips, protection, extraction, editing and split merging. */
public class ZipEntryMetadataTest {
	@TempDir Path directory;
	enum Layout { SINGLE, NATIVE, NUMBERED }
	enum Crypto { NONE, STANDARD, AES, STRONG }
	static final String NAME = "目录/data.bin";
	static final Instant MTIME = Instant.parse("2024-03-05T07:09:11.123456700Z");
	static final Instant ATIME = Instant.parse("2024-02-04T06:08:10.234567800Z");
	static final Instant CTIME = Instant.parse("2023-01-03T05:07:09.345678900Z");
	static final ArchiveOptions PASSWORD = ArchiveOptions.newBuilder()
			.passwordProvider(PasswordProvider.fixed("test-password".toCharArray())).build();

	static Stream<Arguments> cases() {
		return Stream.of(Layout.values()).flatMap(layout -> Stream.of(Crypto.values()).flatMap(crypto ->
				Stream.of(CompressionAlgorithm.STORE, CompressionAlgorithm.DEFLATE)
						.map(algorithm -> Arguments.of(layout, crypto, algorithm))));
	}

	private ZipOptions options(Layout layout, Crypto crypto, CompressionAlgorithm algorithm) throws Exception {
		ZipOptions.Builder builder = ZipOptions.newBuilder().compressionAlgorithm(algorithm).timezone(TimeZone.getTimeZone("UTC"));
		switch (crypto) {
			case STANDARD: builder.standardCrypto(); break;
			case AES: builder.aesCrypto(256); break;
			case STRONG: builder.strongCrypto(0x6610,256).centralDirectoryCrypto(0x6610,256).enableCentralDirectoryCompress(); break;
			default: break;
		}
		if (layout != Layout.SINGLE) builder.splitArchive(true,65536)
				.splitType(layout == Layout.NATIVE ? SplitType.NATIVE : SplitType.NUMBERED);
		return builder.build();
	}

	private static ArchiveEntry metadata(String path, EntryType type) throws Exception {
		return ArchiveEntry.newBuilder().entryPath(path).entryType(type).lastModified(MTIME)
				.lastAccess(ATIME).createTimestamp(CTIME).unixMode(type == EntryType.DIRECTORY ? 0x41E8 : 0x81E9)
				.dosAttributes(0x22).build();
	}

	private void write(Path zip, ZipOptions options, ArchiveEntry entry, byte[] content) throws Exception {
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"rw"),PASSWORD,options);
			 ResourceWriter writer = archive.openWriter(entry.getEntryPath(),"").orElseThrow()) {
			try (OutputStream output = writer.openStream(entry)) { output.write(content); }
			writer.finish();
		}
	}

	@ParameterizedTest(name="{0}, {1}, {2}")
	@MethodSource("cases")
	void manualMetadataSurvivesSplitAndEncryption(Layout layout, Crypto crypto, CompressionAlgorithm algorithm) throws Exception {
		Path zip = directory.resolve("metadata.zip");
		ZipOptions options = options(layout,crypto,algorithm);
		byte[] content = new byte[132071]; new Random(21).nextBytes(content);
		write(zip,options,metadata(NAME,EntryType.FILE),content);
		Path readable = layout == Layout.NUMBERED ? Path.of(zip+".001") : zip;
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(readable.toString(),"r"),PASSWORD,options)) {
			ArchiveEntry entry = archive.entry(NAME).orElseThrow();
			assertAll(() -> assertEquals(MTIME,entry.getLastModified()),
					() -> assertEquals(ATIME,entry.getLastAccessTime()),
					() -> assertEquals(CTIME,entry.getCreationTime()),
					() -> assertEquals(0x81E9,entry.getUnixMode()),
					() -> assertEquals(0x22,entry.getDosAttributes()));
			try (ResourceReader reader = archive.openReader(entry).orElseThrow(); InputStream input = reader.openStream()) {
				assertArrayEquals(content,input.readAllBytes());
			}
		}
	}

	@Test
	void restoresSelectedTimesAndDosPermissions() throws Exception {
		Path zip = directory.resolve("restore.zip");
		ZipOptions options = options(Layout.SINGLE,Crypto.NONE,CompressionAlgorithm.DEFLATE);
		write(zip,options,metadata(NAME,EntryType.FILE),new byte[]{1,2,3});
		Path root = directory.resolve("out");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"r"),PASSWORD,options)) {
			archive.extract(root,ExtractOptions.newBuilder().preserveLastModified(true)
					.preservePermissions(true).build());
		}
		Path target = root.resolve(NAME);
		BasicFileAttributes attrs = Files.readAttributes(target,BasicFileAttributes.class);
		assertEquals(MTIME.getEpochSecond(),attrs.lastModifiedTime().toInstant().getEpochSecond());
		assertEquals(MTIME.getNano()/1000,attrs.lastModifiedTime().toInstant().getNano()/1000);
		assertEquals(ATIME.getEpochSecond(),attrs.lastAccessTime().toInstant().getEpochSecond());
		DosFileAttributeView dos = Files.getFileAttributeView(target,DosFileAttributeView.class);
		if (dos != null) assertTrue(dos.readAttributes().isHidden());
		// Creation-time restoration is filesystem dependent; verify it on Windows/NTFS.
		if (System.getProperty("os.name").startsWith("Windows")) assertEquals(CTIME.getEpochSecond(),attrs.creationTime().toInstant().getEpochSecond());
	}

	@Test
	void preservesSourceDirectoryMetadataWithoutEntryData() throws Exception {
		Path source = Files.createDirectory(directory.resolve("source-dir"));
		Files.setLastModifiedTime(source,FileTime.from(MTIME));
		Path zip = directory.resolve("directory.zip");
		ZipOptions options = options(Layout.SINGLE,Crypto.STANDARD,CompressionAlgorithm.DEFLATE);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"rw"),PASSWORD,options);
			 ResourceWriter writer = archive.openWriter("folder/","").orElseThrow()) {
			writer.write(source); writer.finish();
		}
		try (ZipFile jdk = new ZipFile(zip.toFile())) {
			java.util.zip.ZipEntry entry = jdk.getEntry("folder/");
			assertTrue(entry.isDirectory()); assertEquals(0,entry.getSize()); assertEquals(0,entry.getCompressedSize());
		}
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"r"),PASSWORD,options)) {
			assertEquals(EntryType.DIRECTORY,archive.entry("folder/").orElseThrow().getEntryType());
		}
	}

	@Test
	void restoresSourceReadOnlyHiddenSystemAndArchiveFlags() throws Exception {
		Path source = Files.writeString(directory.resolve("source-flags.bin"), "source flags");
		DosFileAttributeView sourceView = Files.getFileAttributeView(source, DosFileAttributeView.class);
		assumeTrue(sourceView != null, "DOS attributes unavailable");
		Path zip = directory.resolve("source-flags.zip");
		Path output = directory.resolve("flags-out/data");
		try {
			sourceView.setHidden(true); sourceView.setSystem(true); sourceView.setArchive(false); sourceView.setReadOnly(true);
			try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "rw"), ZipOptions.newBuilder().build());
				 ResourceWriter writer = archive.openWriter("data", "").orElseThrow()) {
				writer.write(source); writer.finish();
			}
			try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "r"), ZipOptions.newBuilder().build())) {
				assertEquals(7, archive.entry("data").orElseThrow().getDosAttributes());
				archive.extract(output.getParent(), ExtractOptions.newBuilder().preservePermissions(true).build());
			}
			DosFileAttributes attrs = Files.readAttributes(output, DosFileAttributes.class);
			assertTrue(attrs.isReadOnly()); assertTrue(attrs.isHidden()); assertTrue(attrs.isSystem()); assertFalse(attrs.isArchive());
		} finally {
			for (Path file : List.of(source, output)) {
				if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
					DosFileAttributeView view = Files.getFileAttributeView(file, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
					view.setReadOnly(false); view.setHidden(false); view.setSystem(false);
				}
			}
		}
	}

	@Test
	void capturesSourceSymlinkWithoutReadingTargetContent() throws Exception {
		Path target = Files.writeString(directory.resolve("target.txt"),"file content is not link content");
		Path link = directory.resolve("link");
		try { Files.createSymbolicLink(link,Path.of("target.txt")); }
		catch (UnsupportedOperationException | IOException failure) { assumeTrue(false,"Symbolic links unavailable: "+failure); }
		Path zip = directory.resolve("link.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"rw"),ZipOptions.newBuilder().build());
			 ResourceWriter writer = archive.openWriter("link","").orElseThrow()) {
			writer.write(link); writer.finish();
		}
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"r"),ZipOptions.newBuilder().build())) {
			ArchiveEntry entry = archive.entry("link").orElseThrow();
			assertEquals(EntryType.SYMBOLIC_LINK,entry.getEntryType());
			try (ResourceReader reader = archive.openReader(entry).orElseThrow(); InputStream input = reader.openStream()) {
				assertEquals("target.txt",new String(input.readAllBytes(),StandardCharsets.UTF_8));
			}
		}
		assertEquals("file content is not link content",Files.readString(target));
	}

	@Test
	void copyingOptionsRetainsTimestampAndPermissionFlags() {
		ExtractOptions defaults = ExtractOptions.newBuilder().build();
		assertTrue(defaults.isPreserveTimestamp());
		ExtractOptions options = ExtractOptions.newBuilder().preserveLastModified(false).preservePermissions(true).build();
		ExtractOptions copy = ExtractOptions.newBuilder(options).build();
		assertFalse(copy.isPreserveTimestamp());
		assertTrue(copy.isPreservePermissions());
	}

	@Test
	void rejectsMetadataAfterOpeningAndMismatchedTypes() throws Exception {
		Path zip = directory.resolve("state.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(),"rw"),ZipOptions.newBuilder().build());
			 ResourceWriter writer = archive.openWriter("data","").orElseThrow()) {
			try (OutputStream output = writer.openStream(0)) {
				assertThrows(IOException.class,() -> writer.openStream(metadata("data",EntryType.FILE)));
			}
			writer.abort(); archive.rollback();
		}
		Path zip2 = directory.resolve("type.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip2.toString(),"rw"),ZipOptions.newBuilder().build());
			 ResourceWriter writer = archive.openWriter("data","").orElseThrow()) {
			ArchiveEntry invalid = ArchiveEntry.newBuilder().entryPath("data").entryType(EntryType.FILE).unixMode(0xA1FF).build();
			assertThrows(IOException.class,() -> writer.openStream(invalid));
			writer.abort(); archive.rollback();
		}
	}

	@Test
	void ntfsWinsOverCoarseExtendedTimestamp() throws Exception {
		ArchiveEntry entry = metadata("data",EntryType.FILE);
		LocalFileHeader header = new LocalFileHeader();
		header.setExtraDataRecords(EntryTimes.encode(entry));
		assertArrayEquals(new Instant[]{MTIME,ATIME,CTIME},EntryTimes.read(header,true));
		byte[] malformed = new byte[31]; ByteBuffer.wrap(malformed).order(ByteOrder.LITTLE_ENDIAN).putShort(4,(short)1).putShort(6,(short)24);
		header.setExtraDataRecords(List.of(new ExtraDataRecord(EntryTimes.NTFS_ID,malformed)));
		assertThrows(IOException.class,() -> EntryTimes.read(header,true));
		assertArrayEquals(new Instant[3],EntryTimes.read(header,false));
	}

	@Test
	void maskedLocalHeaderDoesNotExposeTimestampExtras() throws Exception {
		LocalFileHeader header = new LocalFileHeader();
		header.setEntryPath("1"); header.setGeneralPurpose(0x2000); header.setExtractNeeded(20);
		header.setUncompressedSize(0); header.setCompressedSize(0); header.setCompressionMethod(0);
		header.setExtraDataRecords(EntryTimes.encode(metadata("data",EntryType.FILE)));
		byte[] encoded = header.toByteArray(StandardCharsets.UTF_8, Zip64Mode.AUTO);
		assertEquals(0,Short.toUnsignedInt(ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN).getShort(28)));
		assertEquals(2,header.getExtraDataRecords().size(),"Central metadata must remain available");
	}

	@Test
	void protectsHighPrecisionTimesWhenCentralDirectoryIsEncrypted() throws Exception {
		Path zip = directory.resolve("protected-times.zip");
		ZipOptions options = options(Layout.SINGLE, Crypto.STRONG, CompressionAlgorithm.DEFLATE);
		write(zip, options, metadata(NAME, EntryType.FILE), new byte[]{1,2,3});
		byte[] bytes = Files.readAllBytes(zip);
		ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(0x04034b50, data.getInt(0));
		int extra = 30 + Short.toUnsignedInt(data.getShort(26));
		int end = extra + Short.toUnsignedInt(data.getShort(28));
		while (extra < end) {
			int tag = Short.toUnsignedInt(data.getShort(extra));
			assertNotEquals(EntryTimes.NTFS_ID, tag);
			assertNotEquals(EntryTimes.EXTENDED_ID, tag);
			extra += 4 + Short.toUnsignedInt(data.getShort(extra + 2));
		}
		Path output = directory.resolve("protected-out");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(zip.toString(), "r"), PASSWORD, options)) {
			assertEquals(MTIME, archive.entry(NAME).orElseThrow().getLastModified());
			archive.extract(output, ExtractOptions.newBuilder().build());
		}
		assertEquals(MTIME.getEpochSecond(), Files.getLastModifiedTime(output.resolve(NAME)).toInstant().getEpochSecond());
	}

	@Test
	void handlesUnsignedFiletimeAndRejectsUnrepresentableDates() throws Exception {
		Instant future = Instant.parse("+40000-01-02T03:04:05.123456700Z");
		ArchiveEntry entry = ArchiveEntry.newBuilder().entryPath("data").entryType(EntryType.FILE).lastModified(future).build();
		LocalFileHeader header = new LocalFileHeader();
		header.setExtraDataRecords(EntryTimes.encode(entry));
		assertEquals(future, EntryTimes.read(header, true)[0]);
		ArchiveEntry tooOld = ArchiveEntry.newBuilder().entryPath("data").entryType(EntryType.FILE)
				.lastModified(Instant.parse("1500-01-01T00:00:00Z")).build();
		assertThrows(IOException.class, () -> EntryTimes.encode(tooOld));
	}

	@Test
	void distinguishesCentralExtendedTimestampFlagsFromTruncatedLocalData() throws Exception {
		byte[] payload = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN).put((byte)3).putInt(1700000000).array();
		LocalFileHeader local = new LocalFileHeader();
		local.setExtraDataRecords(List.of(new ExtraDataRecord(EntryTimes.EXTENDED_ID, payload)));
		assertThrows(IOException.class, () -> EntryTimes.read(local, true));
		org.nervousync.archive.zip.models.header.GeneralFileHeader central =
				new org.nervousync.archive.zip.models.header.GeneralFileHeader();
		central.setExtraDataRecords(local.getExtraDataRecords());
		assertEquals(Instant.ofEpochSecond(1700000000), EntryTimes.read(central, true)[0]);
	}

	@Test
	void retainsMetadataThroughEditAndMerge() throws Exception {
		Path source = directory.resolve("split.zip");
		ZipOptions split = options(Layout.NATIVE,Crypto.STRONG,CompressionAlgorithm.STORE);
		byte[] bytes = new byte[132071];new Random(24).nextBytes(bytes);
		write(source,split,metadata(NAME,EntryType.FILE),bytes);
		Path merged = directory.resolve("merged.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(source.toString(),"r"),PASSWORD,split)) { archive.merge(merged); }
		ZipOptions single = options(Layout.SINGLE,Crypto.STRONG,CompressionAlgorithm.STORE);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(merged.toString(),"rw"),PASSWORD,single);
			 ResourceWriter writer = archive.openWriter("added","").orElseThrow()) {
			try (OutputStream output = writer.openStream(0)) { output.write(1); } writer.finish();
		}
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(merged.toString(),"r"),PASSWORD,single)) {
			ArchiveEntry entry = archive.entry(NAME).orElseThrow();
			assertEquals(MTIME,entry.getLastModified()); assertEquals(ATIME,entry.getLastAccessTime());
			assertEquals(CTIME,entry.getCreationTime()); assertEquals(0x81E9,entry.getUnixMode());
			try (ResourceReader reader = archive.openReader(entry).orElseThrow(); InputStream input = reader.openStream()) {
				assertArrayEquals(bytes,input.readAllBytes());
			}
		}
	}
}
