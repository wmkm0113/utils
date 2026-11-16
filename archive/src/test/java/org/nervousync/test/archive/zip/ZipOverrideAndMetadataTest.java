package org.nervousync.test.archive.zip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.zip.ZipOptions;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ZipOverrideAndMetadataTest {

	@TempDir
	Path tempDirectory;

	@Test
	void neverDoesNotReplaceExistingTarget() throws Exception {
		Path archive = timedArchive("value.txt", "archive", Instant.parse("2020-01-01T00:00:00Z"));
		Path output = Files.createDirectories(tempDirectory.resolve("never"));
		Path target = Files.writeString(output.resolve("value.txt"), "existing");

		extract(archive, output, ExtractOptions.newBuilder().overrideMode(OverrideMode.NEVER).build());

		assertArrayEquals("existing".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
	}

	@Test
	void alwaysReplacesExistingTarget() throws Exception {
		Path archive = timedArchive("value.txt", "archive", Instant.parse("2020-01-01T00:00:00Z"));
		Path output = Files.createDirectories(tempDirectory.resolve("always"));
		Path target = Files.writeString(output.resolve("value.txt"), "existing");

		extract(archive, output, ExtractOptions.newBuilder().overrideMode(OverrideMode.ALWAYS).build());

		assertArrayEquals("archive".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
	}

	@Test
	void newerReplacesOnlyOlderTarget() throws Exception {
		Instant archiveTime = Instant.parse("2020-01-02T00:00:00Z");
		Path archive = timedArchive("value.txt", "archive", archiveTime);
		Path output = Files.createDirectories(tempDirectory.resolve("newer"));
		Path target = Files.writeString(output.resolve("value.txt"), "old");
		Files.setLastModifiedTime(target, FileTime.from(archiveTime.minus(Duration.ofDays(1))));

		extract(archive, output, ExtractOptions.newBuilder().overrideMode(OverrideMode.NEWER).build());

		assertArrayEquals("archive".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));

		Files.writeString(target, "newer-target");
		Files.setLastModifiedTime(target, FileTime.from(archiveTime.plus(Duration.ofDays(1))));
		extract(archive, output, ExtractOptions.newBuilder().overrideMode(OverrideMode.NEWER).build());
		assertArrayEquals("newer-target".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
	}

	@Test
	void appliesDirectoryTimestampAfterExtractingChildren() throws Exception {
		Instant directoryTime = Instant.parse("2001-02-03T04:05:06Z");
		Path archive = tempDirectory.resolve("directory-metadata.zip");
		try (OutputStream output = Files.newOutputStream(archive);
		     ZipOutputStream zip = new ZipOutputStream(output)) {
			ZipEntry directory = new ZipEntry("folder/");
			directory.setTime(directoryTime.toEpochMilli());
			zip.putNextEntry(directory);
			zip.closeEntry();

			ZipEntry child = new ZipEntry("folder/child.txt");
			child.setTime(Instant.parse("2021-01-01T00:00:00Z").toEpochMilli());
			zip.putNextEntry(child);
			zip.write("child".getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		Path output = tempDirectory.resolve("directory-metadata-output");

		extract(archive, output, ExtractOptions.newBuilder().preserveLastModified(true).build());

		Instant actual = Files.getLastModifiedTime(output.resolve("folder")).toInstant();
		assertTrue(Math.abs(Duration.between(directoryTime, actual).toMillis()) <= 2_000L,
				() -> "Expected " + directoryTime + " but was " + actual);
	}

	private Path timedArchive(final String entryName, final String content, final Instant modified) throws Exception {
		Path archive = tempDirectory.resolve(entryName + "-timed.zip");
		try (OutputStream output = Files.newOutputStream(archive);
		     ZipOutputStream zip = new ZipOutputStream(output)) {
			ZipEntry entry = new ZipEntry(entryName);
			entry.setTime(modified.toEpochMilli());
			zip.putNextEntry(entry);
			zip.write(content.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		return archive;
	}

	private static void extract(final Path archive, final Path output, final ExtractOptions options) throws Exception {
		try (ArchiveFile archiveFile = ArchiveFile.zipFile(
				new LocalDataFile(archive.toString(), "r"), ZipOptions.newBuilder().build())) {
			archiveFile.extract(output, options);
		}
	}
}
