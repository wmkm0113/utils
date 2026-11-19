package org.nervousync.test.archive.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

/** Minimal stored-ZIP writer for fixtures that ZipOutputStream refuses to create. */
final class ZipTestFixtures {

	private static final int UTF8_FLAG = 0x0800;
	private static final int STORE = 0;
	private static final int VERSION_20 = 20;
	private static final int DOS_DATE_1980_01_01 = 0x0021;
	private static final int UNIX_VERSION_20 = (3 << 8) | VERSION_20;
	private static final long UNIX_REGULAR_0644 = ((long) 0100644) << 16;

	private ZipTestFixtures() {
	}

	static Entry file(final String name, final byte[] content) {
		return new Entry(name, content, new byte[0], VERSION_20, 0L, null);
	}

	static Entry fileWithDeclaredCrc(final String name, final byte[] content, final long declaredCrc) {
		return new Entry(name, content, new byte[0], VERSION_20, 0L, declaredCrc);
	}

	static Entry directory(final String name) {
		String directoryName = name.endsWith("/") ? name : name + "/";
		return new Entry(directoryName, new byte[0], new byte[0], VERSION_20, 0x10L, null);
	}

	static Entry symbolicLink(final String name, final String target) {
		return new Entry(name, target.getBytes(StandardCharsets.UTF_8),
				new byte[0], UNIX_VERSION_20, ((long) 0120777) << 16, null);
	}

	static Entry hardLink(final String name, final String target) throws IOException {
		byte[] targetBytes = target.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream extra = new ByteArrayOutputStream();
		writeShort(extra, 0x000D);
		writeShort(extra, 12 + targetBytes.length);
		extra.write(new byte[12]);
		extra.write(targetBytes);
		return new Entry(name, new byte[0], extra.toByteArray(), UNIX_VERSION_20,
				UNIX_REGULAR_0644, null);
	}

	static Path write(final Path archive, final Entry... entries) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		List<CentralEntry> centralEntries = new ArrayList<>();

		for (Entry entry : entries) {
			byte[] nameBytes = entry.name.getBytes(StandardCharsets.UTF_8);
			CRC32 crc32 = new CRC32();
			crc32.update(entry.content);
			long crc = entry.declaredCrc == null ? crc32.getValue() : entry.declaredCrc;
			long localOffset = output.size();

			writeInt(output, 0x04034B50L);
			writeShort(output, VERSION_20);
			writeShort(output, UTF8_FLAG);
			writeShort(output, STORE);
			writeShort(output, 0);
			writeShort(output, DOS_DATE_1980_01_01);
			writeInt(output, crc);
			writeInt(output, entry.content.length);
			writeInt(output, entry.content.length);
			writeShort(output, nameBytes.length);
			writeShort(output, entry.extra.length);
			output.write(nameBytes);
			output.write(entry.extra);
			output.write(entry.content);

			centralEntries.add(new CentralEntry(entry, nameBytes, crc, localOffset));
		}

		long centralOffset = output.size();
		for (CentralEntry central : centralEntries) {
			Entry entry = central.entry;
			writeInt(output, 0x02014B50L);
			writeShort(output, entry.madeByVersion);
			writeShort(output, VERSION_20);
			writeShort(output, UTF8_FLAG);
			writeShort(output, STORE);
			writeShort(output, 0);
			writeShort(output, DOS_DATE_1980_01_01);
			writeInt(output, central.crc);
			writeInt(output, entry.content.length);
			writeInt(output, entry.content.length);
			writeShort(output, central.nameBytes.length);
			writeShort(output, entry.extra.length);
			writeShort(output, 0);
			writeShort(output, 0);
			writeShort(output, 0);
			writeInt(output, entry.externalAttributes);
			writeInt(output, central.localOffset);
			output.write(central.nameBytes);
			output.write(entry.extra);
		}

		long centralSize = output.size() - centralOffset;
		writeInt(output, 0x06054B50L);
		writeShort(output, 0);
		writeShort(output, 0);
		writeShort(output, entries.length);
		writeShort(output, entries.length);
		writeInt(output, centralSize);
		writeInt(output, centralOffset);
		writeShort(output, 0);

		Files.write(archive, output.toByteArray());
		return archive;
	}

	private static void writeShort(final ByteArrayOutputStream output, final int value) {
		output.write(value & 0xFF);
		output.write((value >>> 8) & 0xFF);
	}

	private static void writeInt(final ByteArrayOutputStream output, final long value) {
		output.write((int) value & 0xFF);
		output.write((int) (value >>> 8) & 0xFF);
		output.write((int) (value >>> 16) & 0xFF);
		output.write((int) (value >>> 24) & 0xFF);
	}

	static final class Entry {
		private final String name;
		private final byte[] content;
		private final byte[] extra;
		private final int madeByVersion;
		private final long externalAttributes;
		private final Long declaredCrc;

		private Entry(final String name, final byte[] content, final byte[] extra,
		              final int madeByVersion, final long externalAttributes, final Long declaredCrc) {
			this.name = name;
			this.content = Arrays.copyOf(content, content.length);
			this.extra = Arrays.copyOf(extra, extra.length);
			this.madeByVersion = madeByVersion;
			this.externalAttributes = externalAttributes;
			this.declaredCrc = declaredCrc;
		}
	}

	private static final class CentralEntry {
		private final Entry entry;
		private final byte[] nameBytes;
		private final long crc;
		private final long localOffset;

		private CentralEntry(final Entry entry, final byte[] nameBytes, final long crc, final long localOffset) {
			this.entry = entry;
			this.nameBytes = nameBytes;
			this.crc = crc;
			this.localOffset = localOffset;
		}
	}
}
