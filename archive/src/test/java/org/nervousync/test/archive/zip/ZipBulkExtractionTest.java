package org.nervousync.test.archive.zip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.zip.ZipOptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ZipBulkExtractionTest {

	private static final long INVALID_ENTRY_PATH = 0x0000001A001FL;

	@TempDir
	Path tempDirectory;

	@Test
	void extractsNestedDirectoryOnlyOnceAndToCorrectPath() throws Exception {
		byte[] content = "nested-content".getBytes(StandardCharsets.UTF_8);
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("nested.zip"),
				ZipTestFixtures.directory("a/b/"),
				ZipTestFixtures.file("a/b/value.txt", content));
		Path output = tempDirectory.resolve("nested-output");

		try (ArchiveFile archiveFile = open(archive)) {
			archiveFile.extract(output, ExtractOptions.newBuilder().build());
		}

		assertTrue(Files.isDirectory(output.resolve("a/b")));
		assertArrayEquals(content, Files.readAllBytes(output.resolve("a/b/value.txt")));
		assertFalse(Files.exists(output.resolve("a/b/a/b/value.txt")));
	}

	@Test
	void parserRejectsTraversalEntryBeforeExtraction() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("unsafe-reject.zip"),
				ZipTestFixtures.file("../outside.txt", new byte[]{1}));

		ArchiveException exception = assertThrows(ArchiveException.class, () -> open(archive));
		assertEquals(INVALID_ENTRY_PATH, exception.getErrorCode());
		assertFalse(Files.exists(tempDirectory.resolve("outside.txt")));
	}

	@Test
	void continueOnErrorDoesNotPublishBrokenEntry() throws Exception {
		byte[] valid = "valid".getBytes(StandardCharsets.UTF_8);
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("continue.zip"),
				ZipTestFixtures.fileWithDeclaredCrc("broken.txt", "broken".getBytes(StandardCharsets.UTF_8), 0L),
				ZipTestFixtures.file("valid.txt", valid));
		Path output = tempDirectory.resolve("continue-output");

		try (ArchiveFile archiveFile = open(archive)) {
			archiveFile.extract(output, ExtractOptions.newBuilder()
					.continueOnError(true)
					.build());
		}

		assertFalse(Files.exists(output.resolve("broken.txt")));
		assertArrayEquals(valid, Files.readAllBytes(output.resolve("valid.txt")));
	}

	@Test
	void resourceReaderExtractsFileToExactDestination() throws Exception {
		byte[] content = "single-entry".getBytes(StandardCharsets.UTF_8);
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("single.zip"),
				ZipTestFixtures.file("folder/value.txt", content));
		Path destination = tempDirectory.resolve("renamed.bin");

		try (ArchiveFile archiveFile = open(archive)) {
			ArchiveEntry entry = archiveFile.entry("folder/value.txt").orElseThrow();
			try (ResourceReader reader = archiveFile.openReader(entry).orElseThrow()) {
				reader.extract(destination, ExtractOptions.newBuilder()
						.overrideMode(OverrideMode.ALWAYS)
						.build());
			}
		}

		assertArrayEquals(content, Files.readAllBytes(destination));
		assertFalse(Files.exists(destination.resolve("folder/value.txt")));
	}

	@Test
	void createDirectoriesFalseDoesNotCreateMissingRoot() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("no-create.zip"),
				ZipTestFixtures.file("value.txt", new byte[]{1}));
		Path output = tempDirectory.resolve("missing-root");

		try (ArchiveFile archiveFile = open(archive)) {
			assertThrows(ArchiveException.class, () -> archiveFile.extract(output,
					ExtractOptions.newBuilder().createDirectories(false).build()));
		}
		assertFalse(Files.exists(output));
	}

	private static ArchiveFile open(final Path archive) throws Exception {
		return ArchiveFile.zipFile(new LocalDataFile(archive.toString(), "r"), ZipOptions.newBuilder().build());
	}
}
