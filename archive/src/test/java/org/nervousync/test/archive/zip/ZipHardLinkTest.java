package org.nervousync.test.archive.zip;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.enumerations.UnsafePolicy;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.zip.ZipOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ZipHardLinkTest {

	private static final long LINK_NOT_ALLOWED = 0x0000001A0038L;
	private static final long LINK_OUTSIDE_ROOT = 0x0000001A0037L;

	@TempDir
	Path tempDirectory;

	@Test
	void extractsForwardHardLinkChainWithMixedCase() throws Exception {
		Path lowercase = Files.writeString(tempDirectory.resolve("case-probe.txt"), "probe");
		Path uppercase = lowercase.resolveSibling("CASE-PROBE.TXT");
		Assumptions.assumeTrue(lowercase.equals(uppercase) && Files.exists(uppercase));
		assumeHardLinksSupported();

		Path archive = ZipTestFixtures.write(tempDirectory.resolve("mixed-case.zip"),
				ZipTestFixtures.hardLink("a.txt", "Z.TXT"),
				ZipTestFixtures.hardLink("z.txt", "source.txt"),
				ZipTestFixtures.file("source.txt", new byte[]{42}));
		Path output = tempDirectory.resolve("mixed-case-out");

		try (ArchiveFile zip = open(archive)) {
			zip.extract(output, ExtractOptions.newBuilder().allowHardLinks(true).build());
		}

		assertTrue(Files.isSameFile(output.resolve("a.txt"), output.resolve("source.txt")));
		assertTrue(Files.isSameFile(output.resolve("z.txt"), output.resolve("source.txt")));
	}

	@Test
	void neverKeepsExistingHardLinkDestinationWhenSourceIsMissing() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("missing.zip"),
				ZipTestFixtures.hardLink("linked.txt", "missing.txt"));
		Path bulk = Files.createDirectory(tempDirectory.resolve("bulk"));
		Path single = Files.createDirectory(tempDirectory.resolve("single"));
		Path bulkTarget = Files.writeString(bulk.resolve("linked.txt"), "bulk existing");
		Path singleTarget = Files.writeString(single.resolve("renamed.txt"), "single existing");
		ExtractOptions options = ExtractOptions.newBuilder()
				.allowHardLinks(true)
				.overrideMode(OverrideMode.NEVER)
				.build();

		try (ArchiveFile zip = open(archive)) {
			zip.extract(bulk, options);
			try (var reader = zip.openReader(zip.entry("linked.txt").orElseThrow()).orElseThrow()) {
				reader.extract(singleTarget, options);
			}
		}

		assertEquals("bulk existing", Files.readString(bulkTarget));
		assertEquals("single existing", Files.readString(singleTarget));
	}

	@Test
	void neverUsesPreservedFileToBreakHardLinkDependencyCycle() throws Exception {
		assumeHardLinksSupported();

		Path archive = ZipTestFixtures.write(tempDirectory.resolve("cycle.zip"),
				ZipTestFixtures.hardLink("a.txt", "b.txt"),
				ZipTestFixtures.hardLink("b.txt", "a.txt"));
		Path output = Files.createDirectory(tempDirectory.resolve("cycle-out"));
		Path existing = Files.writeString(output.resolve("a.txt"), "preserved");

		try (ArchiveFile zip = open(archive)) {
			zip.extract(output, ExtractOptions.newBuilder()
					.allowHardLinks(true)
					.overrideMode(OverrideMode.NEVER)
					.build());
		}

		assertEquals("preserved", Files.readString(existing));
		assertTrue(Files.isSameFile(existing, output.resolve("b.txt")));
	}

	@Test
	void createsHardLinkToEarlierEntry() throws Exception {
		assumeHardLinksSupported();
		byte[] content = "shared-content".getBytes(StandardCharsets.UTF_8);
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("hard-link.zip"),
				ZipTestFixtures.file("source.txt", content),
				ZipTestFixtures.hardLink("linked.txt", "source.txt"));
		Path output = tempDirectory.resolve("hard-link-output");

		try (ArchiveFile archiveFile = open(archive)) {
			archiveFile.extract(output, ExtractOptions.newBuilder()
					.allowHardLinks(true)
					.build());
		}

		assertArrayEquals(content, Files.readAllBytes(output.resolve("linked.txt")));
		assertTrue(Files.isSameFile(output.resolve("source.txt"), output.resolve("linked.txt")));
	}

	@Test
	void rejectsHardLinkWhenDisabled() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("hard-link-disabled.zip"),
				ZipTestFixtures.file("source.txt", new byte[]{1}),
				ZipTestFixtures.hardLink("linked.txt", "source.txt"));
		Path output = tempDirectory.resolve("hard-link-disabled-output");

		try (ArchiveFile archiveFile = open(archive)) {
			ArchiveException exception = assertThrows(ArchiveException.class,
					() -> archiveFile.extract(output, ExtractOptions.newBuilder()
							.allowHardLinks(false)
							.unsafePolicy(UnsafePolicy.REJECT)
							.build()));
			assertEquals(LINK_NOT_ALLOWED, exception.getErrorCode());
		}
	}

	@Test
	void skippedHardLinkDoesNotStopLaterEntries() throws Exception {
		byte[] later = "later".getBytes(StandardCharsets.UTF_8);
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("hard-link-skip.zip"),
				ZipTestFixtures.file("source.txt", new byte[]{1}),
				ZipTestFixtures.hardLink("linked.txt", "source.txt"),
				ZipTestFixtures.file("later.txt", later));
		Path output = tempDirectory.resolve("hard-link-skip-output");

		try (ArchiveFile archiveFile = open(archive)) {
			archiveFile.extract(output, ExtractOptions.newBuilder()
					.allowHardLinks(false)
					.unsafePolicy(UnsafePolicy.SKIP)
					.build());
		}

		assertFalse(Files.exists(output.resolve("linked.txt")));
		assertArrayEquals(later, Files.readAllBytes(output.resolve("later.txt")));
	}

	@Test
	void rejectsHardLinkWhoseTargetEscapesRoot() throws Exception {
		Path archive = ZipTestFixtures.write(tempDirectory.resolve("hard-link-escape.zip"),
				ZipTestFixtures.hardLink("linked.txt", "../outside.txt"));
		Path output = tempDirectory.resolve("hard-link-escape-output");

		try (ArchiveFile archiveFile = open(archive)) {
			ArchiveException exception = assertThrows(ArchiveException.class,
					() -> archiveFile.extract(output, ExtractOptions.newBuilder()
							.allowHardLinks(true)
							.unsafePolicy(UnsafePolicy.REJECT)
							.build()));
			assertEquals(LINK_OUTSIDE_ROOT, exception.getErrorCode());
		}
	}

	@ParameterizedTest(name = "invalid hard-link timestamp, continueOnError={0}")
	@ValueSource(booleans = {true, false})
	void handlesInvalidHardLinkTimestamp(final boolean continueOnError) throws Exception {
		if (continueOnError) {
			assumeHardLinksSupported();
		}

		Path archive = ZipTestFixtures.write(tempDirectory.resolve("invalid-timestamp.zip"),
				ZipTestFixtures.hardLink("bad.txt", "source.txt"),
				ZipTestFixtures.hardLink("good.txt", "source.txt"));
		zeroFirstEntryDosDate(archive);

		Path output = Files.createDirectory(tempDirectory.resolve("timestamp-output"));
		Path source = Files.writeString(output.resolve("source.txt"), "original");
		Path bad = output.resolve("bad.txt");
		Path good = output.resolve("good.txt");

		ExtractOptions options = ExtractOptions.newBuilder()
				.allowHardLinks(true)
				.overrideMode(OverrideMode.NEWER)
				.continueOnError(continueOnError)
				.build();

		try (ArchiveFile zip = open(archive)) {
			if (continueOnError) {
				zip.extract(output, options);
				assertTrue(Files.isSameFile(source, good),
						"The valid hard link after the bad entry must still be created");
			} else {
				ArchiveException exception = assertThrows(ArchiveException.class,
						() -> zip.extract(output, options));
				assertEquals(0x0000001A002DL, exception.getErrorCode());
				assertFalse(Files.exists(good),
						"Extraction must stop before creating the later hard link");
			}
		}

		assertFalse(Files.exists(bad), "The entry with the invalid timestamp must not be created");
	}

	private static void zeroFirstEntryDosDate(final Path archive) throws IOException {
		byte[] bytes = Files.readAllBytes(archive);
		ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

		// ZipTestFixtures writes a standard 22-byte EOCD with no comment.
		int eocd = bytes.length - 22;
		assertEquals(0x06054B50, data.getInt(eocd));

		int centralHeader = data.getInt(eocd + 16);
		assertEquals(0x02014B50, data.getInt(centralHeader));

		int localHeader = data.getInt(centralHeader + 42);
		assertEquals(0x04034B50, data.getInt(localHeader));

		data.putShort(localHeader + 12, (short) 0);
		data.putShort(centralHeader + 14, (short) 0);
		Files.write(archive, bytes);
	}

	private void assumeHardLinksSupported() throws IOException {
		Path source = Files.write(tempDirectory.resolve("hard-link-probe-source"), new byte[]{1});
		Path link = tempDirectory.resolve("hard-link-probe-link");
		try {
			Files.createLink(link, source);
			Assumptions.assumeTrue(Files.isSameFile(source, link));
		} catch (UnsupportedOperationException | IOException e) {
			Assumptions.abort("Hard links are not supported: " + e.getMessage());
		} finally {
			Files.deleteIfExists(link);
		}
	}

	private static ArchiveFile open(final Path archive) throws Exception {
		return ArchiveFile.zipFile(new LocalDataFile(archive.toString(), "r"), ZipOptions.newBuilder().build());
	}
}
