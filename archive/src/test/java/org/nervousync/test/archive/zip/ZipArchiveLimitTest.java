package org.nervousync.test.archive.zip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ZipArchiveLimitTest {

	private static final long OUT_OF_LIMIT = 0x0000001A003EL;
	private static final long COMPRESSED_INPUT_OUT_OF_LIMIT = 0x0000001A0033L;

	@TempDir
	Path tempDirectory;

	@Test
	void rejectsTooManyEntries() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("entries.zip"),
				ZipTestFixtures.file("one", new byte[]{1}),
				ZipTestFixtures.file("two", new byte[]{2}));
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.limitBuilder().maxEntries(1).confirm()
				.build();
		assertRejected(archive, options, OUT_OF_LIMIT);
	}

	@Test
	void rejectsEntryLargerThanLimit() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("entry-size.zip"),
				ZipTestFixtures.file("value", new byte[5]));
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.limitBuilder().maxEntryBytes(4).confirm()
				.build();
		assertRejected(archive, options, OUT_OF_LIMIT);
	}

	@Test
	void rejectsTotalSizeLargerThanLimit() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("total-size.zip"),
				ZipTestFixtures.file("one", new byte[3]),
				ZipTestFixtures.file("two", new byte[3]));
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.limitBuilder().maxTotalBytes(5).confirm()
				.build();
		assertRejected(archive, options, OUT_OF_LIMIT);
	}

	@Test
	void rejectsNameLongerThanLimit() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("name.zip"),
				ZipTestFixtures.file("12345", new byte[0]));
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.limitBuilder().maxNameCharacters(4).confirm()
				.build();
		assertRejected(archive, options, OUT_OF_LIMIT);
	}

	@Test
	void rejectsNestingDeeperThanLimit() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("depth.zip"),
				ZipTestFixtures.file("a/b/c.txt", new byte[0]));
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.limitBuilder().maxNestingDepth(1).confirm()
				.build();
		assertRejected(archive, options, OUT_OF_LIMIT);
	}

	@Test
	void rejectsStoredEntryAboveCompressionRatioLimit() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("ratio.zip"),
				ZipTestFixtures.file("value", new byte[10]));
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.limitBuilder().maxCompressionRatio(0.5D).confirm()
				.build();
		assertRejected(archive, options, OUT_OF_LIMIT);
	}

	@Test
	void rejectsArchiveLargerThanCompressedInputLimit() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("compressed.zip"),
				ZipTestFixtures.file("value", new byte[10]));
		ArchiveOptions options = ArchiveOptions.newBuilder()
				.limitBuilder().maxCompressedBytes(Files.size(archive) - 1L).confirm()
				.build();
		assertRejected(archive, options, COMPRESSED_INPUT_OUT_OF_LIMIT);
	}

	private static void assertRejected(final Path archive, final ArchiveOptions options,
	                                   final long expectedErrorCode) {
		ArchiveException exception = assertThrows(ArchiveException.class,
				() -> ArchiveFile.zipFile(new LocalDataFile(archive.toString(), "r"), options,
						ZipOptions.newBuilder().build()));
		assertEquals(expectedErrorCode, exception.getErrorCode());
	}
}
