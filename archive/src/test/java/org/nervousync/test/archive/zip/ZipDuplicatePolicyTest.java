package org.nervousync.test.archive.zip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.DuplicatePolicy;
import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.zip.ZipOptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ZipDuplicatePolicyTest {

	private static final long DUPLICATE_ENTRY = 0x0000001A003FL;

	@TempDir
	Path tempDirectory;

	@Test
	void firstPolicyUsesFirstEntryForReadAndExtract() throws Exception {
		assertSelectedContent(DuplicatePolicy.FIRST, "first");
	}

	@Test
	void lastPolicyUsesLastEntryForReadAndExtract() throws Exception {
		assertSelectedContent(DuplicatePolicy.LAST, "last");
	}

	@Test
	void rejectPolicyFailsWhileOpeningArchive() throws Exception {
		Path archive = duplicateArchive();
		ZipOptions zipOptions = ZipOptions.newBuilder().duplicatePolicy(DuplicatePolicy.REJECT).build();

		ArchiveException exception = assertThrows(ArchiveException.class,
				() -> ArchiveFile.zipFile(new LocalDataFile(archive.toString(), "r"), zipOptions));

		assertEquals(DUPLICATE_ENTRY, exception.getErrorCode());
	}

	private void assertSelectedContent(final DuplicatePolicy policy, final String expectedText) throws Exception {
		Path archive = duplicateArchive();
		byte[] expected = expectedText.getBytes(StandardCharsets.UTF_8);
		ZipOptions zipOptions = ZipOptions.newBuilder().duplicatePolicy(policy).build();

		try (ArchiveFile archiveFile = ArchiveFile.zipFile(new LocalDataFile(archive.toString(), "r"), zipOptions)) {
			ArchiveEntry entry = archiveFile.entry("same.txt").orElseThrow();
			try (ResourceReader reader = archiveFile.openReader(entry).orElseThrow()) {
				assertArrayEquals(expected, reader.openStream().readAllBytes());
			}

			Path output = tempDirectory.resolve("out-" + policy.name());
			archiveFile.extract(output, ExtractOptions.newBuilder()
					.overrideMode(OverrideMode.ALWAYS)
					.build());
			assertArrayEquals(expected, Files.readAllBytes(output.resolve("same.txt")));
		}
	}

	private Path duplicateArchive() throws Exception {
		return ZipTestFixtures.write(tempDirectory.resolve("duplicates.zip"),
				ZipTestFixtures.file("same.txt", "first".getBytes(StandardCharsets.UTF_8)),
				ZipTestFixtures.file("same.txt", "last".getBytes(StandardCharsets.UTF_8)));
	}
}
