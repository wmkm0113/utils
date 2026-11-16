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

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.CompressionAlgorithm;
import org.nervousync.archive.enumerations.OverrideMode;
import org.nervousync.archive.enumerations.Zip64Mode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nervousync.test.archive.zip.ZipWriteTestSupport.*;

/** BZip2 ZIP 失败路径专项：密码、内部 CRC、外层 CRC、认证失败均不得发布半成品。 */
final class ZipBZip2FailureTest {

	@TempDir
	Path directory;

	static Stream<Arguments> wrongPasswords() {
		return Stream.of(Encryption.STANDARD, Encryption.AES, Encryption.STRONG)
				.flatMap(encryption -> Stream.of(false, true).map(existing -> Arguments.of(encryption, existing)));
	}

	@ParameterizedTest(name = "{0}, existingTarget={1}")
	@MethodSource("wrongPasswords")
	void wrongPasswordPreservesExtractionTarget(Encryption encryption, boolean existing) throws Exception {
		ZipOptions options = options(CompressionAlgorithm.BZIP2, Zip64Mode.AUTO, Layout.SINGLE, encryption);
		Path path = create(options);
		ArchiveOptions wrong = ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed("incorrect".toCharArray())).build();
		assertUnpublished(path, options, wrong, existing);
	}

	static Stream<Arguments> damagedEntries() {
		return Stream.of("block-crc", "combined-crc", "zip-crc", "aes-auth")
				.flatMap(damage -> Stream.of(false, true).map(existing -> Arguments.of(damage, existing)));
	}

	@ParameterizedTest(name = "{0}, existingTarget={1}")
	@MethodSource("damagedEntries")
	void lateIntegrityFailurePreservesExtractionTarget(String damage, boolean existing) throws Exception {
		ZipOptions options = options(CompressionAlgorithm.BZIP2, Zip64Mode.AUTO, Layout.SINGLE,
				damage.equals("aes-auth") ? Encryption.AES : Encryption.NONE);
		Path path = create(options);
		byte[] bytes = Files.readAllBytes(path);
		ByteBuffer raw = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		assertEquals(0x04034B50, raw.getInt(0));
		int dataStart = 30 + Short.toUnsignedInt(raw.getShort(26)) + Short.toUnsignedInt(raw.getShort(28));
		int centralStart = raw.getInt(bytes.length - 6);
		assertEquals(0x02014B50, raw.getInt(centralStart));
		int compressedSize = raw.getInt(centralStart + 20);
		switch (damage) {
			case "block-crc":
				bytes[dataStart + 10] ^= 1;
				break;
			case "combined-crc":
				// 倒数第二字节必定处于 32 位 combined CRC 中，避开尾部对齐填充位。
				bytes[dataStart + compressedSize - 2] ^= 0x10;
				break;
			case "zip-crc":
				// 本地头与中央目录同时修改，保证失败来自内容校验，而非头部不一致。
				int wrong = raw.getInt(centralStart + 16) ^ 1;
				raw.putInt(14, wrong);
				raw.putInt(centralStart + 16, wrong);
				break;
			case "aes-auth":
				bytes[dataStart + compressedSize - 1] ^= 1;
				break;
			default: fail("Unknown damage: " + damage);
		}
		Files.write(path, bytes);
		assertUnpublished(path, options, archiveOptions(), existing);
	}

	private Path create(ZipOptions options) throws Exception {
		Path path = directory.resolve("input.zip");
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "rw"), archiveOptions(), options)) {
			// 超过一个 BZip2 block，combined CRC/AES 认证失败前已产生完整的解压输出。
			writeEntry(archive, "data.bin", payload(1000017));
		}
		return path;
	}

	private void assertUnpublished(Path path, ZipOptions options, ArchiveOptions credentials, boolean existing) throws Exception {
		Path parent = Files.createDirectory(directory.resolve("extracted"));
		Path target = parent.resolve("data.bin");
		byte[] original = {5, 4, 3, 2, 1};
		if (existing) Files.write(target, original);
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(path.toString(), "r"), credentials, options);
		     ResourceReader reader = archive.openReader(archive.entry("data.bin").orElseThrow()).orElseThrow()) {
			assertThrows(IOException.class, () -> reader.extract(target,
					ExtractOptions.newBuilder().overrideMode(OverrideMode.ALWAYS).build()));
		}
		if (existing) assertArrayEquals(original, Files.readAllBytes(target));
		else assertFalse(Files.exists(target));
		try (Stream<Path> children = Files.list(parent)) {
			assertEquals(existing ? 1 : 0, children.count(), "No staging files may remain");
		}
	}
}
