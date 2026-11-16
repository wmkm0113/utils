package org.nervousync.test.archive.zip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nervousync.archive.core.DataFile;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.central.CentralDirectory;
import org.nervousync.archive.zip.models.central.EndCentralDirectoryRecord;
import org.nervousync.archive.zip.operator.ZipFileOperator;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ZipFileOperatorOptimizationTest {
	@TempDir Path directory;

	@Test
	void rejectsTooManyEntriesBeforeReadingTheDirectory() throws Exception {
		ArchiveOptions options = ArchiveOptions.newBuilder().limitBuilder().maxEntries(2).confirm().build();
		try (Probe probe = open(3, options)) {
			long before = probe.readCalls;
			ArchiveException error = assertThrows(ArchiveException.class,
					() -> probe.operator.readCentralDirectory(probe.end, null));
			assertEquals(0x0000001A003EL, error.getErrorCode());
			assertEquals(before, probe.readCalls, "An over-limit directory must be rejected before reading entries");
		}
	}

	@Test
	void acceptsAnEntryCountExactlyAtTheLimit() throws Exception {
		ArchiveOptions options = ArchiveOptions.newBuilder().limitBuilder().maxEntries(3).confirm().build();
		try (Probe probe = open(3, options)) {
			assertEquals(3, probe.operator.readCentralDirectory(probe.end, null).getFileHeaders().size());
		}
	}

	@Test
	void leavesTheInputAtTheEndOfTheDeclaredDirectory() throws Exception {
		try (Probe probe = open(1, ArchiveOptions.newBuilder().build())) {
			CentralDirectory central = probe.operator.readCentralDirectory(probe.end, null);
			assertEquals("entry-0", central.getFileHeaders().get(0).getEntryPath());
			assertEquals(probe.end.getCentralDirectorySize(), probe.file.getFilePointer(),
					"Buffering must not consume the following EOCD");
			assertEquals(0x50, probe.file.readUnsignedByte());
			assertEquals(0x4B, probe.file.readUnsignedByte());
		}
	}

	@Test
	void readsALargeDirectoryAndReportsUnderlyingReadCalls() throws Exception {
		try (Probe probe = open(10000, ArchiveOptions.newBuilder().build())) {
			long before = probe.readCalls;
			CentralDirectory central = probe.operator.readCentralDirectory(probe.end, null);
			assertEquals(10000, central.getFileHeaders().size());
			assertEquals(probe.end.getCentralDirectorySize(), probe.file.getFilePointer());
			System.out.println("CENTRAL_DIRECTORY entries=10000 bytes=" + probe.end.getCentralDirectorySize()
					+ " underlyingReadCalls=" + (probe.readCalls - before));
		}
	}

	private Probe open(int count, ArchiveOptions options) throws Exception {
		ByteArrayOutputStream central = new ByteArrayOutputStream();
		for (int index = 0; index < count; index++) {
			byte[] name = ("entry-" + index).getBytes(StandardCharsets.UTF_8);
			ByteBuffer header = ByteBuffer.allocate(46 + name.length).order(ByteOrder.LITTLE_ENDIAN);
			header.putInt(0x02014B50).putShort((short)20).putShort((short)20);
			header.position(28);
			header.putShort((short)name.length);
			header.position(46);
			header.put(name);
			central.write(header.array());
		}
		byte[] centralBytes = central.toByteArray();
		ByteBuffer end = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN);
		end.putInt(0x06054B50).putShort((short)0).putShort((short)0)
				.putShort((short)count).putShort((short)count).putInt(centralBytes.length)
				.putInt(0).putShort((short)0);
		central.write(end.array());
		Path path = Files.createTempFile(directory, "central-probe-", ".zip");
		Files.write(path, central.toByteArray());
		return new Probe(path, options);
	}

	private static final class Probe implements AutoCloseable {
		DataFile file;
		final ZipFileOperator operator;
		final EndCentralDirectoryRecord end;
		long readCalls;

		Probe(Path path, ArchiveOptions options) throws Exception {
			file = new LocalDataFile(path.toString(), "r");
			DataFile counted = countReads(file);
			operator = new ZipFileOperator(counted, options, ZipOptions.newBuilder().build());
			end = operator.readEndCentralDirectoryRecord();
			assertNull(operator.readZip64EndCentralDirectory(end));
		}

		private DataFile countReads(DataFile source) {
			return (DataFile) Proxy.newProxyInstance(DataFile.class.getClassLoader(),
					new Class<?>[]{DataFile.class}, (proxy, method, args) -> {
						if (method.getName().equals("read") || method.getName().equals("readFully")) readCalls++;
						try {
							Object result = method.invoke(source, args);
							if (result instanceof DataFile) {
								file = (DataFile) result;
								return countReads(file);
							}
							return result;
						} catch (InvocationTargetException error) {
							throw error.getCause();
						}
					});
		}

		@Override public void close() throws Exception { operator.close(); }
	}
}