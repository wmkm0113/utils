# utils-archive

[English](README.md) | [简体中文](README_zh_CN.md) | [繁體中文](README_zh_TW.md)

`utils-archive` is the archive module of Nervousync Java Utilities. Its current implementation focuses on ZIP creation, reading, extraction, editing, and merging split archives, with support for passwords, certificates, and central directory protection.

## Contents

- [Features](#features)
- [Dependencies and Runtime](#dependencies-and-runtime)
- [Core Interfaces and Defaults](#core-interfaces-and-defaults)
- [Sessions, Commits, and Rollbacks](#sessions-commits-and-rollbacks)
- [Creating Archives and Adding Files](#creating-archives-and-adding-files)
- [Streaming Writes](#streaming-writes)
- [Looking Up and Listing Entries](#looking-up-and-listing-entries)
- [Reading and Extraction](#reading-and-extraction)
- [Compression Methods and Levels](#compression-methods-and-levels)
- [Password Encryption](#password-encryption)
- [PKWARE Strong Encryption](#pkware-strong-encryption)
- [Certificate Encryption and Decryption](#certificate-encryption-and-decryption)
- [Central Directory Protection](#central-directory-protection)
- [Split Archives](#split-archives)
- [ZIP64 and Large Files](#zip64-and-large-files)
- [Editing, Deleting, and Overwriting](#editing-deleting-and-overwriting)
- [File Attributes, Directories, and Links](#file-attributes-directories-and-links)
- [Resource Limits and Verification](#resource-limits-and-verification)
- [Temporary Files, Concurrency, and Resource Lifecycle](#temporary-files-concurrency-and-resource-lifecycle)
- [Low-Level Interfaces and Format Models](#low-level-interfaces-and-format-models)
- [Legacy Interface Compatibility](#legacy-interface-compatibility)
- [Testing and Current Limitations](#testing-and-current-limitations)

## Features

| Feature | Current Implementation |
|----------------|----------------------------------------------------------------------|
| ZIP creation and reading | Local files and random-access data sources based on `DataFile` |
| File and directory writing | Files, explicit directory entries, symbolic links, and streaming writes with metadata |
| Compression methods | STORE, DEFLATE, DEFLATE64, BZIP2, LZMA (method 14), and XZ (method 95) for reading and writing ZIP entries |
| Lookup and extraction | Lookup by path, entry input streams, individual entry extraction, and whole-archive extraction |
| Editing | Add, replace, and remove entries in existing archives; rewrite and publish changes when the session closes |
| Large files | ZIP64 AUTO, ALWAYS, and NEVER; sizes and offsets use `long` |
| Native split ZIP archives | Read, write, and merge `.z01`, `.z02`, and the final `.zip` volume |
| Consecutive byte segments | Read, write, and merge `.zip.001`, `.zip.002`, and subsequent segments |
| Traditional password encryption | ZipCrypto |
| WinZip AES | 128/192/256-bit keys; selectable AE-1 and AE-2 |
| PKWARE Strong Encryption | AES, DES, 3DES, RC2 NEW, RC4, Blowfish, and Twofish; see the parameters below |
| Certificate credentials | X.509 RSA certificates, certificate/private-key interfaces, PKCS#12 files, and multiple recipients |
| Credential policies | Password, certificate, or either; configured separately for entries and the central directory |
| Central directory protection | Compression, strong encryption, hashing, and the corresponding reading, editing, and merging operations |
| File attributes | DOS timestamps, NTFS/UT timestamp extra fields, Unix modes, and DOS attributes |
| Link extraction | Controlled extraction of symbolic links and recognized hard links, with path and dependency checks |
| Structure parsing | Local/Central Headers, Data Descriptors, ZIP64, extra fields, and optional directory records |
| Resource limits | Entry count, individual and total sizes, input size, header size, compression ratio, path depth, and name length |

The `TAR` entry point and related compression enumerations exist, but reading, writing, extraction, and commit operations in `TarAdaptor` are still empty implementations. TAR, TAR.GZ, TAR.BZ2, TAR.XZ, and 7z containers are not currently usable features of this module.

## Dependencies and Runtime

The source targets Java 17. The repository CI configuration includes JDK 17, 21, and 25; consult the corresponding build results to determine which combinations pass.

The module coordinates in the current branch are:

```xml
<properties>
    <nervousync.version>2.0.0-SNAPSHOT</nervousync.version>
</properties>

<dependencies>
<dependency>
    <groupId>org.nervousync</groupId>
    <artifactId>utils-archive</artifactId>
    <version>${nervousync.version}</version>
</dependency>
</dependencies>
```

Use a version matching the release or local build used by your project. If the current branch version has not been published, run the following from the repository root:

```shell
mvn -pl archive -am install -DskipTests
```

Runtime dependencies include `utils-commons`, the Jakarta Annotation API, and Bouncy Castle. In this module's current POM, Commons Compress has **test** scope and is used to construct some test fixtures. The production ZIP implementation does not use it for reading or writing.

`CryptoUtils`, used by strong encryption, selects a `CryptoProvider` through Java SPI. To use the project's Bouncy Castle adapter and the full strong encryption algorithm set, add the same version of `utils-security` to your application:

```xml
<dependency>
    <groupId>org.nervousync</groupId>
    <artifactId>utils-security</artifactId>
    <version>${nervousync.version}</version>
    <scope>runtime</scope>
</dependency>
```

Having the Bouncy Castle JAR on the classpath does not by itself load the project's `CryptoProvider` implementation. When building an aggregate JAR, preserve or merge `META-INF/services` so SPI can discover the adapter.

## Core Interfaces and Defaults

| Interface/Type | Purpose |
|------------------|------------------------------------------------------------|
| `ArchiveFile` | Archive session entry point for adding, looking up, reading, extracting, deleting, merging, and rolling back |
| `LocalDataFile` | Local random-access file implementation |
| `DataFile` | Extensible data access interface for reading, writing, seeking, obtaining lengths, and enumerating volumes |
| `ArchiveEntry` | Describes an entry's path, type, sizes, timestamps, permissions, and link target |
| `ResourceWriter` | Writes one entry; call `finish()` explicitly to register the completed result |
| `ResourceReader` | Reads or extracts one entry |
| `ArchiveOptions` | Character set, verification, strict mode, credential providers, and resource limits |
| `ZipOptions` | Compression, encryption, central directory protection, splitting, ZIP64, and duplicate entry policy |
| `ExtractOptions` | Target replacement, attribute restoration, links, path safety, and error continuation policy |

### Opening Modes

```java
import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.*;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.security.certificate.CertificateCredentials;
import org.nervousync.archive.security.certificate.CertificateProvider;
import org.nervousync.archive.security.certificate.PKCS12CertificateProvider;
import org.nervousync.archive.security.password.PasswordCredentials;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.central.CentralDirectory;
import org.nervousync.archive.zip.models.central.EndCentralDirectoryRecord;
import org.nervousync.archive.zip.models.central.Zip64EndCentralDirectory;
import org.nervousync.archive.zip.models.header.GeneralFileHeader;
import org.nervousync.archive.zip.operator.ZipFileOperator;
import org.nervousync.commons.zip.ZipGlobals;
import org.nervousync.enumerations.zip.Zip64Mode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
```

The Java blocks below contain methods or nested interfaces that can be placed in a single example class using the imports above. The example methods receive source files, destination paths, and credentials as parameters.

- `new LocalDataFile(path, "r")`: read-only; the file must already exist.
- `new LocalDataFile(path, "rw")`: CREATE if the file does not exist, or EDIT if it already exists.
- `"rw"` **does not automatically truncate an existing archive**. To create a new archive, use a destination path that does not yet exist.
- Ensure the destination's parent directory exists before creating an archive. Do not let multiple write sessions modify the same target or volume set simultaneously.

### Main Defaults

| Option | Default |
|------------------------------------|---------------------------------------------|
| Entry name/comment character set | UTF-8 |
| Password encoding | Legacy low-byte encoding for `PasswordProvider.fixed(password)`; pass `true` for UTF-8 |
| `ArchiveOptions.verify` / `strict` | `true` / `true`                             |
| Password/certificate providers | `none()` |
| Compression method/level | DEFLATE / 5 |
| Entry encryption and central directory compression/encryption/hashing | Disabled |
| AES version selected by `aesCrypto(bits)` | AE-2; an overload allows explicit selection |
| ZIP64                              | AUTO                                        |
| Splitting | Disabled; the default type is DETECT. Select a type explicitly when writing |
| Duplicate names | `DuplicatePolicy.LAST` |
| Time zone for DOS timestamps | `TimeZone.getDefault()` |
| Extraction overwrite policy | `OverrideMode.NEWER` |
| Unsafe entries during extraction | `UnsafePolicy.REJECT` |
| Restore timestamps/permissions | `true` / `false` |
| Allow symbolic links/hard links | `false` / `false` |
| Create directories/continue on error | `true` / `false` |

## Sessions, Commits, and Rollbacks

`ArchiveFile.close()` commits writable sessions. `ResourceWriter.finish()` only completes and registers the current entry. Temporary entries are merged, directory and end records are written, and output files are published when the archive session closes.

Java try-with-resources calls `close()` even when the block throws an exception. `ArchiveFile` does not automatically know whether your application code succeeded. To cancel the entire session, call `rollback()` before the exception leaves the block.

The following helper handles rollback when application code fails. The writing examples below reuse it:

```java
@FunctionalInterface
interface ArchiveAction {
	void run(ArchiveFile archive) throws Exception;
}

static void writeArchive(Path target, ArchiveOptions options,
                         ZipOptions zipOptions, ArchiveAction action) throws Exception {
	Files.createDirectories(target.toAbsolutePath().getParent());
	try (ArchiveFile archive = ArchiveFile.zipFile(
			new LocalDataFile(target.toString(), "rw"), options, zipOptions)) {
		try {
			action.run(archive);
		} catch (Exception | Error failure) {
			try {
				archive.rollback();
			} catch (Exception rollbackFailure) {
				if (rollbackFailure != failure) {
					failure.addSuppressed(rollbackFailure);
				}
			}
			throw failure;
		}
	}
}
```

If the commit itself fails, `ArchiveFile.close()` attempts rollback and attaches any rollback failure as a suppressed exception. Do not continue using a write session after calling `rollback()`.

## Creating Archives and Adding Files

The first argument to `addFile(entryPath, filePath, comment)` is the path inside the archive; the second is the source file path. Use `/` as the separator inside an archive.

```java
static void createBasicZip(Path target, Path sourceFile, Path sourceFolder) throws Exception {
	writeArchive(target, ArchiveOptions.newBuilder().build(), ZipOptions.newBuilder().build(), archive -> {
		archive.addFile("documents/readme.txt", sourceFile.toString(), "说明文件");
		archive.addDirectory("assets", sourceFolder.toString());
	});
}
```

`addDirectory(rootPath, folderPath)` adds enumerated source files by their relative paths beneath the specified archive root. `rootPath` may be an empty string. This convenience method currently relies on `FileUtils.listFiles()` and does not replace a complete file-tree traversal that handles empty directories and links. To preserve those structures precisely, traverse the tree yourself and use `ResourceWriter.write(Path)`.

When adding a file through a Writer, writing entry points are available for `Path`, `File`, and `String`:

```java
static void addWithWriter(Path target, Path source) throws Exception {
	writeArchive(target, ArchiveOptions.newBuilder().build(), ZipOptions.newBuilder().build(), archive -> {
		try (ResourceWriter writer = archive.openWriter("data/source.bin", "源文件").orElseThrow()) {
			writer.write(source);
			writer.finish();
		}
	});
}
```

`ZipEntryResourceWriter.write(Path)` reads the source attributes. The archive name of an explicit directory entry must end with `/`.

## Streaming Writes

For network streams, generated data, or other input streams, create an `ArchiveEntry` and then write to the entry output stream. You do not need to calculate the CRC or compressed size in advance.

```java
static void writeStream(Path target, InputStream source) throws Exception {
	ArchiveEntry entry = ArchiveEntry.newBuilder()
			.entryPath("streams/payload.bin")
			.entryType(EntryType.FILE)
			.lastModified(Instant.now())
			.build();

	writeArchive(target, ArchiveOptions.newBuilder().build(), ZipOptions.newBuilder().build(), archive -> {
		try (ResourceWriter writer = archive.openWriter(entry.getEntryPath(), "流式数据").orElseThrow()) {
			try (OutputStream output = writer.openStream(entry)) {
				source.transferTo(output);
			}
			writer.finish();
		}
	});
}
```

The caller closes `source` in this example. Close the output stream first to finish compression and encryption, then call `writer.finish()`. The Writer calculates the CRC, original size, and final stored size from the actual data, so callers do not need to supply encryption overhead.

The write pipeline is: original data counting/CRC → optional compression → optional encryption → entry temporary file. At commit time, the entry data is merged into the final ZIP.

The argument to `openStream(long)` is a **packed DOS date/time value**, not epoch milliseconds. Prefer `openStream(ArchiveEntry)` when supplying timestamp attributes.

## Looking Up and Listing Entries

`ArchiveFile.entry(path)` returns an `Optional<ArchiveEntry>` for lookup by a known archive path. Do not use the old directory index to determine the state of pending changes during creation or editing. Reopen the archive after committing to read its final state.

`ArchiveFile` does not currently provide a convenience method for enumerating all entries. To obtain the complete directory, read the central directory through `ZipFileOperator`:

```java
static List<ArchiveEntry> listEntries(Path source, ArchiveOptions options,
                                      ZipOptions zipOptions) throws Exception {
	try (ZipFileOperator operator = new ZipFileOperator(
			new LocalDataFile(source.toString(), "r"), options, zipOptions)) {
		EndCentralDirectoryRecord end = operator.readEndCentralDirectoryRecord();
		Zip64EndCentralDirectory zip64 = operator.readZip64EndCentralDirectory(end);
		CentralDirectory central = operator.readCentralDirectory(end, zip64);
		List<ArchiveEntry> result = new ArrayList<>();
		for (GeneralFileHeader header : central.getFileHeaders()) {
			result.add(ArchiveEntry.describe(header, zipOptions.getTimeZone(), options.isStrict()));
		}
		return result;
	}
}
```

`getFileHeaders()` includes the entries in the original central directory, including duplicate names. `entryHeaders()` returns the entries selected by the duplicate-name policy. The low-level `ZipFileOperator` does not perform all `ZipAdaptor` session validation. The example above is suitable for directory inspection; use `ArchiveFile` for normal reading and extraction.

## Reading and Extraction

### Reading an Entry into an Output Stream

```java
static void readEntry(Path source, String entryPath, OutputStream destination,
                      ArchiveOptions options, ZipOptions zipOptions) throws Exception {
	try (ArchiveFile archive = ArchiveFile.zipFile(
			new LocalDataFile(source.toString(), "r"), options, zipOptions)) {
		ArchiveEntry entry = archive.entry(entryPath)
				.orElseThrow(() -> new IOException("Entry not found: " + entryPath));
		try (ResourceReader reader = archive.openReader(entry).orElseThrow();
		     InputStream input = reader.openStream()) {
			input.transferTo(destination);
		}
	}
}
```

The caller manages `destination`. Closing the input stream may read the remaining data and complete verification; `close()` itself may also throw. Handle failures during both reading and closing.

### Extracting an Entire Archive

```java
static void extractAll(Path source, Path directory,
                       ArchiveOptions options, ZipOptions zipOptions) throws Exception {
	ExtractOptions extractOptions = ExtractOptions.newBuilder()
			.overrideMode(OverrideMode.ALWAYS)
			.preserveLastModified(true)
			.preservePermissions(true)
			.build();
	try (ArchiveFile archive = ArchiveFile.zipFile(
			new LocalDataFile(source.toString(), "r"), options, zipOptions)) {
		archive.extract(directory, extractOptions);
	}
}
```

The `NEVER/NEWER/ALWAYS` policies respectively preserve an existing target, replace it only with a newer archive entry, or allow replacement. The default is `NEWER`; this example explicitly selects `ALWAYS`.

### Extracting One Entry to a Specific File

```java
static void extractOne(Path source, String entryPath, Path target,
                       ArchiveOptions options, ZipOptions zipOptions) throws Exception {
	try (ArchiveFile archive = ArchiveFile.zipFile(
			new LocalDataFile(source.toString(), "r"), options, zipOptions)) {
		ArchiveEntry entry = archive.entry(entryPath).orElseThrow();
		try (ResourceReader reader = archive.openReader(entry).orElseThrow()) {
			reader.extract(target, ExtractOptions.newBuilder()
					.overrideMode(OverrideMode.ALWAYS).build());
		}
	}
}
```

The destination of `ArchiveFile.extract()` is an extraction root directory, whereas the destination of `ResourceReader.extract()` is the final path of that entry. Hard links that depend on other entries are better handled by whole-archive extraction.

Regular files are extracted to temporary files first. Data verification and attribute handling complete before publication, reducing the risk of leaving a target behind or overwriting an existing file after a wrong password or data corruption. Whole-archive extraction is not a transaction spanning all entries: a later failure does not automatically undo files extracted successfully earlier.

## Compression Methods and Levels

The current ZIP entry reader/writer and protected central directory support the following methods:

| Algorithm | ZIP method ID | Level behavior |
|-----------|---------------|----------------|
| `STORE` | 0 | Stores bytes unchanged; the level has no effect |
| `DEFLATE` | 8 | `0..9`; compression uses the JDK in raw mode |
| `DEFLATE64` | 9 | `0` writes stored blocks; `1..9` increase the match-length search bound using fixed Huffman coding |
| `BZIP2` | 12 | `1..9` select block capacities of 100000..900000 bytes; `0` is mapped to `1` |
| `LZMA1` | 14 | ZIP LZMA; `0..9` select dictionaries of 64 KiB..8 MiB and match-length limits of 32..273; `0` still compresses |
| `LZMA2` | 95 | ZIP XZ, containing LZMA2; `0..9` tune match search with a fixed 64 KiB dictionary; `0` still compresses |

`ZipOptions.Builder.compressionLevel(...)` accepts only `0..9`; the default is `5`. It does not accept the JDK's special level `-1`. BZip2 level `0` still compresses data; select `STORE` to disable compression.

```java
static ZipOptions compressionOptions(CompressionAlgorithm algorithm, int level) throws Exception {
	return ZipOptions.newBuilder()
			.compressionAlgorithm(algorithm)
			.compressionLevel(level)
			.build();
}

static void createBzip2Zip(Path target, Path source) throws Exception {
	writeArchive(target, ArchiveOptions.newBuilder().build(),
			compressionOptions(CompressionAlgorithm.BZIP2, 9), archive -> {
				archive.addFile("data/payload.bin", source.toString(), "BZip2 payload");
			});
}
```

Pass `CompressionAlgorithm.DEFLATE64`, `CompressionAlgorithm.LZMA1`, or `CompressionAlgorithm.LZMA2` to the same helper to select Deflate64, ZIP LZMA (method 14), or ZIP XZ (method 95), respectively. `LZMA2` selects a complete XZ container in ZIP; raw LZMA2 bytes are not written directly as an entry. For central directory compression with the same algorithm and level, copy these options with `ZipOptions.newBuilder(options).enableCentralDirectoryCompress().build()`.

```java
ZipOptions lzmaOptions = compressionOptions(CompressionAlgorithm.LZMA1, 5);
ZipOptions xzOptions = compressionOptions(CompressionAlgorithm.LZMA2, 5);
```

To use a fixed time zone for DOS timestamps or adjust an existing configuration, copy the options:

```java
static ZipOptions utcOptions(ZipOptions source) throws Exception {
	return ZipOptions.newBuilder(source)
			.timezone(TimeZone.getTimeZone("UTC"))
			.build();
}
```

- DEFLATE and Deflate64 share `DeflateInputStream` and `DeflateOutputStream`. Decoding supports stored, fixed-Huffman, and dynamic-Huffman blocks. The Deflate64 encoder currently emits stored or fixed-Huffman blocks; its compression ratio need not match encoders that generate dynamic tables.
- BZip2 uses the module's own `BZip2InputStream` and `BZip2OutputStream`. ZIP entry CRC-32 and the BZip2 block/member CRCs are separate integrity checks.
- Directory entries are written using STORE, without entry data encryption.
- Compression methods and levels are configured through `ZipOptions`, not read from `ArchiveEntry`. A dedicated configuration entry point for selecting a different algorithm for each entry within one session is not yet available.
- `LZMA1` uses the module's own LZMA1 codec with ZIP method-14 framing. Entry writing includes an end-of-stream marker and sets the corresponding ZIP flag.
- `LZMA2` selects ZIP method 95 and wraps the raw LZMA2 codec in a complete XZ container. XZ decoding supports multiple Blocks, each with a single LZMA2 filter, and the NONE, CRC32, CRC64, and SHA256 data Check types. BCJ, Delta, and filter chains are not supported. ZIP XZ writing uses CRC64 by default.
- LZMA1 and LZMA2/XZ level `0` still compress data. Their levels tune this implementation's dictionary or match search and do not represent presets from external LZMA/XZ tools.
- Zstandard remains unimplemented. Standalone LZMA2/XZ streams do not provide TAR.XZ or 7z container support.
- Use DEFLATE when broad ZIP interoperability is the priority. Verify the consuming tool's support before selecting Deflate64, BZip2, LZMA, XZ, or protected central directories.

## Password Encryption

### Providing a Password

Provide passwords through `ArchiveOptions.passwordProvider(...)`, rather than storing them in `ZipOptions`:

```java
static ArchiveOptions passwordOptions(char[] password) {
	return ArchiveOptions.newBuilder()
			.passwordProvider(PasswordProvider.fixed(password, true))
			.build();
}
```

The Provider should return a new `PasswordCredentials` instance for each request. The module closes and destroys credentials after use; do not repeatedly return the same destroyed credentials object.

To select different passwords by purpose, inspect the `purpose`, archive path, entry path, and attempt number in `PasswordRequest`:

```java
static ArchiveOptions separatePasswords(char[] entryPassword, char[] directoryPassword) {
	return ArchiveOptions.newBuilder().passwordProvider(request -> {
		boolean directory = request.getPurpose() == PasswordPurpose.READ_HEADER
				|| request.getPurpose() == PasswordPurpose.WRITE_HEADER;
		return Optional.of(PasswordCredentials.of(directory ? directoryPassword : entryPassword, true));
	}).build();
}
```

Return `Optional.empty()` when no password is available. The `attempt` field alone does not imply automatic prompts or retry loops; the application must organize interaction and retries.

`PasswordProvider.fixed(password)` uses legacy low-byte encoding by default; pass `true` as its second argument to select UTF-8, as in the example above. For a custom Provider, set the same flag in `PasswordCredentials.of(password, utf8Encoding)`. Match the encoding used to create the archive. File name character sets and password encodings are independent.

### ZipCrypto and WinZip AES

```java
static ZipOptions zipCryptoOptions() throws Exception {
	return ZipOptions.newBuilder().standardCrypto().build();
}

static ZipOptions aesOptions(int keyBits, AesVendor vendor) throws Exception {
	return ZipOptions.newBuilder().aesCrypto(keyBits, vendor).build();
}

static void createPasswordZip(Path target, Path source, char[] password) throws Exception {
	writeArchive(target, passwordOptions(password), aesOptions(256, AesVendor.AE_2),
			archive -> archive.addFile("secret/payload.bin", source.toString(), ""));
}
```

AES key length values are 128, 192, and 256. `aesCrypto(256)` defaults to AE-2; `aesCrypto(256, AesVendor.AE_1)` explicitly selects AE-1.

| AES Version | CRC and Integrity Verification |
|----------|--------------------------------------------------|
| AE-1 | Stores the actual CRC and verifies it along with AES authentication data |
| AE-2 | Stores zero in the ZIP header CRC and uses AES authentication data to verify ciphertext integrity |

When reading, the encryption method is detected from the headers. Decrypting a regular AES ZIP usually requires only a password Provider; you do not need to configure `aesCrypto(...)` again. Strong Encryption and WinZip AES are different ZIP encryption formats.

## PKWARE Strong Encryption

Configure password-based strong encryption with `strongCrypto(algorithm, keyBitLength)`:

```java
static void createStrongZip(Path target, Path source, char[] password) throws Exception {
	ZipOptions zipOptions = ZipOptions.newBuilder()
			.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256)
			.build();
	writeArchive(target, passwordOptions(password), zipOptions,
			archive -> archive.addFile("strong/payload.bin", source.toString(), ""));
}
```

The main parameters currently accepted by `StrongKey.validate(...)` and the corresponding encryptors are:

| Algorithm | `ZipGlobals` Constant Suffix | Key Length in Bits |
|----------|-----------------------------------|------------------------------------|
| DES | `DES` | 56 or 64; public ZIP options normalize this to 64 |
| 3DES     | `3DES_112` / `3DES_168`           | 112 / 168                          |
| AES      | `AES_128` / `AES_192` / `AES_256` | 128 / 192 / 256                    |
| RC2 NEW  | `RC2_NEW`                         | 40, 64, 128                        |
| RC4      | `RC4`                             | 40, 64, 128                        |
| Blowfish | `BLOWFISH` | 32..320, in multiples of 8 |
| Twofish  | `TWOFISH`                         | 128, 192, 256                      |

All constants use the prefix `ZIP64_HEADER_ENC_ALGORITHM_`. RC2 OLD has a format constant and recognition branches, but is not supported for strong encryption writing. Block size and key length are separate concepts; an algorithm's block size does not determine `keyBitLength`.

These capabilities describe the current format implementation and algorithm coverage. They do not imply that a single third-party extraction tool can read every algorithm. Internal write/read tests do not replace external format interoperability tests.

## Certificate Encryption and Decryption

### Access Modes

| `StrongAccessMode` | Required for Creation | Required for Decryption |
|--------------------|-------------------------|----------------------------|
| PASSWORD | Password | Password |
| CERTIFICATE | List of recipient RSA public-key certificates | Any matching recipient certificate and its private key |
| BOTH | Password and recipient certificate list | Either a password or a matching certificate/private-key pair |

**BOTH means password or certificate; decryption does not require both.** Creation in BOTH mode requires both unlocking methods; either one can be used for decryption.

The current certificate scheme wraps session keys using X.509 RSA public keys. Content encryption can still use the supported strong encryption algorithms listed above; certificate use is not limited to AES. ECDSA/EC certificate recipients are not currently implemented.

### Reading Recipient Public-Key Certificates and Creating an Archive

```java
static X509Certificate readCertificate(Path path) throws Exception {
	try (InputStream input = Files.newInputStream(path)) {
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
	}
}

static void createCertificateZip(Path target, Path source,
                                 List<X509Certificate> recipients) throws Exception {
	ZipOptions zipOptions = ZipOptions.newBuilder()
			.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256,
					StrongAccessMode.CERTIFICATE, recipients)
			.build();
	writeArchive(target, ArchiveOptions.newBuilder().build(), zipOptions,
			archive -> archive.addFile("secret/payload.bin", source.toString(), ""));
}
```

Creating a certificate-encrypted archive requires only the recipients' public-key certificates, not their private keys. You do not need to configure the decryption `CertificateProvider` in `ArchiveOptions`. Multiple recipients can be supplied; the current recipient-count limit constant is 1024.

### Decrypting through the Certificate and Private-Key Interface

```java
static ArchiveOptions certificateOptions(X509Certificate certificate, PrivateKey privateKey) throws Exception {
	CertificateCredentials credentials = new CertificateCredentials(certificate, privateKey, null);
	return ArchiveOptions.newBuilder()
			.certificateProvider(CertificateProvider.fixed(credentials))
			.build();
}
```

Pass the returned `ArchiveOptions` to `readEntry()`, `extractOne()`, or `extractAll()` above. The third argument of `CertificateCredentials` accepts the JCA Provider that performs private-key operations; pass `null` to leave it unspecified.

A custom `CertificateProvider` can locate credentials using the public-key hash and archive/entry context in the request. Return `Optional.empty()` when no credentials match. The certificate and private key should form a matching pair.

### Loading Private Keys from PKCS#12

```java
static ArchiveOptions pkcs12Options(Path keyStore, char[] storePassword,
                                    char[] keyPassword) throws Exception {
	PKCS12CertificateProvider provider = PKCS12CertificateProvider.load(
			keyStore, storePassword, keyPassword);
	return ArchiveOptions.newBuilder().certificateProvider(provider).build();
}
```

Supply the store password and private-key password separately. The loader iterates through PKCS#12 key entries and selects entries containing an RSA X.509 certificate and a private key. It throws if no usable entries exist. A public-key certificate file alone cannot decrypt an archive.

### BOTH and Dual Providers

```java
static ZipOptions bothEncryptionOptions(List<X509Certificate> recipients) throws Exception {
	return ZipOptions.newBuilder()
			.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256,
					StrongAccessMode.BOTH, recipients)
			.build();
}

static ArchiveOptions dualProviderOptions(char[] password, CertificateProvider certificateProvider) {
	return ArchiveOptions.newBuilder()
			.passwordProvider(PasswordProvider.fixed(password))
			.certificateProvider(certificateProvider)
			.build();
}
```

When reading BOTH mode, certificate candidate keys are collected first; the password is used only if no candidates are found. `Optional.empty()` permits fallback. Do not assume automatic password fallback when a Provider throws or an existing certificate candidate fails subsequent verification.

## Central Directory Protection

Entry data encryption and central directory protection can be configured independently. Encrypting entry data alone does not automatically hide file names in the central directory.

### Compression Only or Hashing Only

```java
static ZipOptions compressedDirectoryOptions() throws Exception {
	return ZipOptions.newBuilder().enableCentralDirectoryCompress().build();
}

static ZipOptions hashedDirectoryOptions() throws Exception {
	return ZipOptions.newBuilder()
			.centralDirectoryHash(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256)
			.build();
}
```

Central directory compression reuses the compression method and level from the ZIP options; STORE, DEFLATE, DEFLATE64, BZIP2, LZMA (method 14), and XZ (method 95) are supported. Select LZMA and XZ with `CompressionAlgorithm.LZMA1` and `CompressionAlgorithm.LZMA2`, respectively. BZIP2 level `0` is mapped to block size `1`, just as for entry compression. Editing and split-archive merging can read and regenerate directories compressed with these methods. The hash is calculated over the central directory bytes before transformation. It is separate from the AES entry authentication code and is not a digital signature.

Supported hash constant suffixes are `NONE`, `CRC32`, `MD5`, `SHA1`, `RIPEMD160`, `SHA256`, `SHA384`, and `SHA512`, with the prefix `ZIP64_HEADER_HASH_ALGORITHM_`.

### Password-Based Strong Encryption for Both Entries and the Central Directory

```java
static ZipOptions protectedDirectoryOptions() throws Exception {
	return ZipOptions.newBuilder()
			.compressionAlgorithm(CompressionAlgorithm.DEFLATE)
			.compressionLevel(5)
			.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256)
			.enableCentralDirectoryCompress()
			.centralDirectoryCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256)
			.centralDirectoryHash(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256)
			.build();
}
```

Use `writeArchive(target, passwordOptions(password), protectedDirectoryOptions(), action)`. When writing the central directory, the implementation hashes the original directory bytes, then compresses and encrypts them, and finally writes the ZIP64 Protection metadata. Reading reverses the process and checks sizes, trailing data, and the hash.

### Certificate-Only Entries with a Password-Only Central Directory

```java
static ZipOptions mixedProtectionOptions(List<X509Certificate> recipients) throws Exception {
	return ZipOptions.newBuilder()
			.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256,
					StrongAccessMode.CERTIFICATE, recipients)
			.enableCentralDirectoryCompress()
			.centralDirectoryCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256)
			.build();
}
```

For creation, provide the central directory password and recipient public-key certificates. Reading the entire archive requires both a password Provider and a certificate Provider because they handle different layers. Each layer has its own independent access mode.

`centralDirectoryCrypto(...)` also has an overload accepting `StrongAccessMode` and a recipient list, allowing the reverse combination or BOTH. Central directory decryption credentials are required when opening an archive with an encrypted central directory; they cannot be deferred until an entry is read.

## Split Archives

### Configuring Splitting

```java
static ZipOptions splitOptions(SplitType type, long splitBytes) throws Exception {
	return ZipOptions.newBuilder()
			.splitArchive(true, splitBytes)
			.splitType(type)
			.build();
}

static void createSplitZip(Path baseTarget, Path source, SplitType type,
                           long splitBytes) throws Exception {
	writeArchive(baseTarget, ArchiveOptions.newBuilder().build(), splitOptions(type, splitBytes),
			archive -> archive.addFile("payload.bin", source.toString(), ""));
}
```

For a `baseTarget` such as `backup.zip`:

| Type | Creation Configuration | Output Files | Reading Entry Point |
|---------------|-----------------------|-----------------------------------------------|------------------|
| Native split ZIP | `SplitType.NATIVE` | `backup.z01`, `backup.z02`, and the final `backup.zip` | `backup.zip` |
| Consecutive segments | `SplitType.NUMBERED` | `backup.zip.001`, `backup.zip.002`, … | `backup.zip.001` |

- Native ZIP volume sizes range from `65,536..0xFFFFFFFF` bytes.
- Each Local/Central File Header in a native split archive must fit entirely within one volume. An operation fails if the volume capacity is smaller than a header.
- Numbered segments can split at any byte position, so entry headers can span segments.
- When reading with `splitArchive(true, ...)`, provide a volume-length upper bound large enough for every actual volume. Usually, you can reuse the creation configuration.
- For reading, `SplitType.DETECT` identifies the two layouts from their extensions. For creation, explicitly select NATIVE or NUMBERED.
- If native mode produces only one volume, the final file is still named `backup.zip`. Numbered mode produces `backup.zip.001`.
- Volume layout and entry encryption are independent options. Numbered segmentation does not automatically disable Strong Encryption.

For native 64 MiB volumes, use `splitOptions(SplitType.NATIVE, 64L * 1024 * 1024)`. For numbered segments, change the type to `SplitType.NUMBERED`.

### Merging into a Single ZIP

```java
static void mergeSplitZip(Path sourceVolume, Path target,
                          ArchiveOptions options, ZipOptions sourceOptions) throws Exception {
	Files.createDirectories(target.toAbsolutePath().getParent());
	try (ArchiveFile archive = ArchiveFile.zipFile(
			new LocalDataFile(sourceVolume.toString(), "r"), options, sourceOptions)) {
		archive.merge(target);
	}
}
```

The source must be a split archive. Merging can be invoked from a read-only session and preserves the source volumes. Use a separate target path that does not overlap any source volume. The operation rewrites disk numbers, offsets, and end-of-central-directory records; it does not merely concatenate `.z01` files.

Merging copies existing entries' stored data and rebuilds the final directory. Rebuilding a protected central directory requires the corresponding credentials. Entry CRCs, encryption metadata, and Local Header masking semantics must also be preserved. Merging does not replace reading every entry to verify all contents.

## ZIP64 and Large Files

```java
static ZipOptions zip64Options(Zip64Mode mode) throws Exception {
	return ZipOptions.newBuilder().zip64Mode(mode).build();
}
```

| Mode | Behavior |
|--------|-----------------------------------------------------------|
| AUTO | Generates the required ZIP64 information based on actual entry sizes, offsets, counts, and protection structures |
| ALWAYS | Forces the corresponding ZIP64 information and ZIP64 end structures |
| NEVER | Rejects operations that require ZIP64 representation |

Support for entries or archives larger than 4 GiB does not mean sizes are unlimited. Limits also come from `ArchiveLimit`, Java `long` and collection capacities, available temporary disk space, and the target file system.

The default per-entry limit is 8 GiB. Large, highly compressible files can also trigger the compression-ratio limit. Set resource limits for your application data; see "Resource Limits and Verification" for an example of increasing them.

## Editing, Deleting, and Overwriting

Opening an existing ZIP with `"rw"` selects EDIT mode. Writing a new entry at the same archive path replaces the old entry. Specify deletions using an `ArchiveEntry`:

```java
static void editZip(Path target, Path replacement,
                    ArchiveOptions options, ZipOptions zipOptions) throws Exception {
	writeArchive(target, options, zipOptions, archive -> {
		Optional<ArchiveEntry> obsolete = archive.entry("obsolete.txt");
		if (obsolete.isPresent()) {
			archive.remove(obsolete.get());
		}
		archive.addFile("documents/current.txt", replacement.toString(), "替换内容");
	});
}
```

Existing entries are generally retained by copying their stored data; new entries use the current session's ZIP options. Editing is not an entry point for changing every existing entry's password or recompressing all data. For those conversions, read the original contents and create a new target archive.

Split input is also reorganized according to the configuration when committed. The session writes to a working directory, then publishes the target and handles old volumes. Publishing multiple files is not an operating-system-level atomic transaction across files. The working directory may be retained for recovery after a failure.

### Duplicate Names

```java
static ZipOptions duplicateOptions(DuplicatePolicy policy) throws Exception {
	return ZipOptions.newBuilder().duplicatePolicy(policy).build();
}
```

- FIRST: selects the first central directory entry at a path.
- LAST: selects the last entry; this is the default.
- REJECT: throws when a duplicate is detected.

This policy selects among entries with the same name in the original directory. It does not permit multiple callers to write concurrently to the same Writer. The directory model retains the original header list for merge operations that need to preserve duplicate entries.

## File Attributes, Directories, and Links

### Metadata

`ArchiveEntry.fromPath(path, entryPath)` reads source attributes without following symbolic links. The main supported fields are:

| Field | Meaning |
|-------------------|-------------------------------------------------|
| `lastModified` | Last modification time; generates DOS timestamps and applicable extra fields |
| `lastAccess` | Last access time |
| `createTimestamp` | Creation time |
| `unixMode` | Unix file type and permission bits |
| `dosAttributes` | DOS file attribute bits |
| `linkTarget` | Link target description |

Writing uses NTFS timestamp extra fields for higher precision and includes a UT modification timestamp when its range permits. DOS timestamps use the time zone specified by `ZipOptions.timezone(...)`. Reading restores attributes using the central directory and available timestamp extra fields.

Create an entry with metadata explicitly:

```java
static ArchiveEntry textEntry(String entryPath) throws Exception {
	Instant now = Instant.now();
	return ArchiveEntry.newBuilder()
			.entryPath(entryPath)
			.entryType(EntryType.FILE)
			.lastModified(now)
			.lastAccess(now)
			.createTimestamp(now)
			.unixMode(0x81A4) // 普通文件类型 0x8000 + 权限 0644 对应的 0x1A4
			.build();
}
```

Pass the result to `writer.openStream(entry)`. The metadata's `entryPath` must match the path used to open the Writer, and the Unix file type must agree with `EntryType`.

For extraction, `preserveLastModified(true)` controls timestamp restoration in the current implementation, including available access and creation times passed to the file system. `preservePermissions(true)` controls permission and DOS attribute restoration.

Directory attributes are restored after child entries are extracted, so subsequent writes do not change their timestamps. Restoration depends on the target file system. Restoration of a symbolic link's own attributes is currently skipped. Lossless preservation of all file system metadata, including complete ACLs, ownership, and extended attributes, is not guaranteed.

### Writing an Explicit Empty Directory

```java
static void addEmptyDirectory(Path target) throws Exception {
	ArchiveEntry directory = ArchiveEntry.newBuilder()
			.entryPath("empty/")
			.entryType(EntryType.DIRECTORY)
			.lastModified(Instant.now())
			.build();
	writeArchive(target, ArchiveOptions.newBuilder().build(), ZipOptions.newBuilder().build(), archive -> {
		try (ResourceWriter writer = archive.openWriter(directory.getEntryPath(), "").orElseThrow()) {
			try (OutputStream output = writer.openStream(directory)) {
				// Directory entries have no payload.
			}
			writer.finish();
		}
	});
}
```

### Symbolic Links and Hard Links

Use `writer.write(sourceLinkPath)` to save a symbolic link; its entry data stores the link target text. Alternatively, open a stream with SYMBOLIC_LINK metadata and write the target text using the archive character set. Setting `linkTarget` alone does not replace writing the stream data manually.

Extraction does not allow link creation by default. Enable it explicitly when required:

```java
static ExtractOptions linkExtractionOptions() throws Exception {
	return ExtractOptions.newBuilder()
			.allowSymbolicLinks(true)
			.allowHardLinks(true)
			.unsafePolicy(UnsafePolicy.REJECT)
			.build();
}
```

The implementation checks link targets and path components, handles forward dependencies of recognized hard links, and rejects dependency cycles or targets outside the permitted directory. Creating symbolic links on Windows may require appropriate privileges. Hard links are also subject to operating-system and file-system limitations.

The current Writer accepts FILE, DIRECTORY, and SYMBOLIC_LINK. It does not provide native HARD_LINK metadata writing. Reading/extracting hard links and writing them are separate capabilities.

## Resource Limits and Verification

### Default Limits

| Limit | Default |
|-----------------------|-----------|
| `maxEntries`          | 1,000,000 |
| `maxEntryBytes`       | 8 GiB     |
| `maxTotalBytes`       | 64 GiB    |
| `maxCompressedBytes`  | 64 GiB    |
| `maxHeaderBytes`      | 256 MiB   |
| `maxCompressionRatio` | 1000      |
| `maxNestingDepth`     | 8         |
| `maxNameCharacters`   | 4096      |

`maxNestingDepth` currently checks directory depth in entry paths, not the number of recursively opened ZIP files nested inside other ZIPs. The module does not automatically extract embedded archives.

### Custom Resource Limits and Character Sets

```java
static ArchiveOptions limitedOptions() throws Exception {
	return ArchiveOptions.newBuilder()
			.charset(StandardCharsets.UTF_8)
			.verify(true)
			.strict(true)
			.limitBuilder()
			.maxEntries(100_000L)
			.maxEntryBytes(16L * 1024 * 1024 * 1024)
			.maxTotalBytes(128L * 1024 * 1024 * 1024)
			.maxCompressedBytes(128L * 1024 * 1024 * 1024)
			.maxHeaderBytes(128L * 1024 * 1024)
			.maxCompressionRatio(2000.0D)
			.maxNestingDepth(16)
			.maxNameCharacters(4096)
			.confirm()
			.build();
}
```

Entries carrying the UTF-8 flag are decoded as UTF-8. Other names and comments use the configured character set. Non-ASCII password encoding is controlled separately by `PasswordCredentials.of(password, utf8Encoding)`.

### Path Safety and Error Policies

- Whole-archive extraction checks that each normalized target path remains inside the extraction root.
- Existing parent path components are checked to handle directory escape through symbolic links.
- `UnsafePolicy.REJECT` rejects unsafe entries. SKIP skips them in the applicable extraction checks.
- `strict(true)` also affects parsing-time structure, path, and Local/Central Header consistency checks. Invalid entries rejected during parsing do not reach extraction-time SKIP handling.
- `continueOnError(true)` allows extraction to continue after certain extraction failures. It does not turn all structural errors or safety rejections into success.
- `ResourceReader.extract(target, ...)` extracts one entry to a specified final path. For untrusted archives, prefer whole-archive extraction with its explicit root-directory semantics.

### Verification Behavior

- Regular ZIP entries are checked against CRCs, sizes, and resource limits according to configuration.
- AE-1 retains the CRC; AE-2 relies on AES authentication data. Authentication failures throw an exception.
- Strong Encryption checks credentials, the decrypted validation block, and the algorithm's end-of-data conditions.
- The central directory is checked against its Protection structure, sizes, and configured hash.
- `verify(false)` does not disable all format, authentication, or resource checks.
- Closing a stream may complete remaining reads and authentication. Wait for closing to succeed before treating the operation as successful.

Failures can be reported as `ArchiveException`, underlying `IOException`, option-building exceptions, or password/certificate-related exceptions. Use `ArchiveException.getErrorCode()` to obtain the module error code. Display text depends on the project's internationalization resources; avoid identifying errors solely by their message strings.

## Temporary Files, Concurrency, and Resource Lifecycle

### Writing and Publication

Each entry is written to a separate session temporary file using streaming compression/encryption, without requiring the complete entry in memory. The final ZIP is generated during commit. The high-level interface therefore does not directly stream a complete archive to an HTTP response.

Temporary working directories are created beneath the target's parent directory with names such as `.nervousync-zip-*`. Allow enough disk space for sources, targets, and staged data; editing an existing archive can also require output and backup space. Successful commits and normal rollbacks clean up the working directory. Publication failures may leave recovery data in place.

### Concurrency Constraints

Independent entry staging provides a basis for parallel preparation, but the current interface does not offer a caller-facing thread pool or a complete thread-safety guarantee for sessions. Writer lookup/registration in `openWriter()`, session state, commits, and rollbacks require coordination.

For normal use, operate on a session sequentially. For parallel preparation, have a control thread create Writers for different paths and worker threads write their own entries. After all tasks finish, the control thread completes registration and closes the session. Do not write concurrently to the same Writer or overlap commit, close, or rollback with worker activity. On failure, wait for workers to exit before handling the session's temporary directory.

### Closing Rules

| Object | Caller Responsibility |
|-------------------------------|----------------------------------------------------|
| `ArchiveFile` | Use try-with-resources; closing may commit and throw an exception |
| `ResourceWriter` | Close the entry output stream, call `finish()` on success, then close the Writer |
| `ResourceReader` | Manage closing both the input stream and the Reader |
| External InputStream/OutputStream | External streams created by callers in the examples remain the callers' responsibility |
| `ZipFileOperator` | Closing also closes its archive input stream |

`writer.abort()` marks an entry as cancelled; it does not necessarily restore the write session to a committable state. Use `ArchiveFile.rollback()` to cancel the whole operation. `close()` is also not equivalent to `ResourceWriter.finish()`; unfinished Writers prevent a normal commit.

## Low-Level Interfaces and Format Models

For callers inspecting structures, diagnosing compatibility issues, or extending archive implementations, the module also provides:

| Type | Purpose |
|---------------------------------------------------|---------------------------------------------------------|
| `ZipFileOperator` | Reads EOCD, ZIP64 EOCD/Locator, central directories, and Local Headers |
| `ZipArchiveInputStream` | Seekable archive byte input across volumes; not a ZIP stream that automatically decompresses entries |
| `ZipArchiveOutputStream` | Volume switching, complete-header boundaries, directory statistics, and final volume publication mappings |
| `LocalFileHeader` / `GeneralFileHeader` | Local and central directory file header models and serialization |
| `EndCentralDirectoryRecord` | Regular EOCD |
| `Zip64EndCentralDirectory`                        | ZIP64 Record, Locator, Protection                       |
| `ExtraDataRecord` | ZIP extra fields |
| `AesEncryptionInfo` / `StrongEncryptionInfo` | Entry metadata for the two encryption formats |
| `StrongDecryptionHeader` / `StrongRecipientTable` | Strong Encryption decryption headers and recipient information |
| `StandardInputStream` / `StandardOutputStream` | ZipCrypto streams |
| `AesInputStream` / `AesOutputStream` | WinZip AES streams |
| `StrongInputStream` / `StrongOutputStream` | PKWARE Strong Encryption streams |
| `DeflateInputStream` / `DeflateOutputStream` | Raw DEFLATE/Deflate64 codecs; no ZIP, zlib, or GZIP framing |
| `BZip2InputStream` / `BZip2OutputStream` | Complete BZh streams, usable independently of ZIP |
| `LZMAInputStream` / `LZMAOutputStream` | ZIP method-14 LZMA1 framing by default; `raw(...)` and `alone(...)` factories support raw LZMA1 and LZMA-Alone (`.lzma`) streams |
| `LZMA2InputStream` / `LZMA2OutputStream` | Raw LZMA2 streams; no XZ, ZIP, or 7z container framing |
| `XZInputStream` / `XZOutputStream` | Complete XZ streams using the LZMA2 filter; usable independently of ZIP |

The standalone BZip2 decoder reads one member by default; its two-argument constructor can enable concatenated members. It supports legacy randomized blocks, while the encoder writes non-randomized blocks. Read to EOF to validate all BZip2 block/member CRCs: closing the standalone decoder does not drain unread data. ZIP entry and central directory readers use single-member decoding; the protected central directory reader also rejects trailing data.

The standalone XZ decoder accepts concatenated Streams by default. Pass `false` to its two-argument constructor to stop after one Stream; ZIP entry and central directory readers use this mode. Read to EOF to validate the XZ data Checks, Index, and Footer. The default maximum declared dictionary size is 64 MiB; this is not a limit on decompressed output size. The XZ encoder currently writes one LZMA2 Block for nonempty input, using a fixed 64 KiB dictionary; empty input produces a Stream without Blocks. The standalone encoder accepts an `XZCheck` selection; its default is CRC64.

For the Deflate, BZip2, LZMA1, LZMA2, and XZ output streams, `finish()` completes compressed output while keeping the delegate open; `close()` also closes the delegate. `DeflateOutputStream.release()` only releases codec resources and does not finish output. Standalone codec users must enforce their own output-size and compression-ratio limits.

A custom `DataFile` must correctly implement `seek()`, `getFilePointer()`, `length()`, `newFile()`, and `splitFiles()` in addition to normal reads and writes. Splitting code opens other volumes with the same configuration through `newFile()` and locates volumes through `splitFiles()`. Independent Readers also create separate seekable handles. A network stream supporting only sequential reads cannot directly replace this interface.

A regular ZIP consists of entry Local Headers, entry data, and optional Data Descriptors, followed by the central directory and end records. ZIP64 structures and directory protection information are included when needed. The directory model can also read Archive Extra Data Record and Digital Signature data.

Preserving Digital Signature bytes does not implement digital signature verification or certificate-chain validation. When manipulating low-level models directly, callers must maintain offsets, disk numbers, sizes, ZIP64 placeholders, authentication overhead, and stream ownership themselves. Normal applications should use `ArchiveFile` rather than assemble structures manually.

## Legacy Interface Compatibility

The module retains `org.nervousync.zip.ZipFile` and `org.nervousync.zip.options.ZipOptions`, providing legacy methods for creation, adding, reading, extraction, deletion, password settings, comments, and merging split archives.

- The legacy `ZipFile.openZipFile(...)` and the new `ArchiveFile.zipFile(...)` are different entry points.
- The compression support described above applies to `org.nervousync.archive`; the retained legacy ZIP pipeline has not acquired Deflate64, BZip2, LZMA, or XZ support through this refactor.
- The examples in this guide use `org.nervousync.archive.zip.ZipOptions`.
- Header models and options from the legacy package cannot be passed directly to the new interfaces.
- Use the new interfaces for the certificate, dual Provider, and central directory Protection workflows described here. Do not assume equivalent capabilities in the legacy interfaces.

Keep the corresponding legacy call chain when maintaining an existing application. New integrations can start with the session interface described in this guide.

## Testing and Current Limitations

Run the module's tests and those of its upstream modules from the repository root:

```shell
mvn -pl archive -am test
```

To run one set of ZIP tests:

```shell
mvn -pl archive -am -Dtest=ZipFileTest -Dsurefire.failIfNoSpecifiedTests=false test
```

To enable the large-ZIP tests controlled by a flag:

```shell
mvn -pl archive -am -DrunLargeZipTest=true test
```

Compression tests include `DeflateStreamTest`, `Deflate64ZipTest`, `Deflate64CentralDirectoryTest`, `BZip2StreamTest`, `ZipBZip2IntegrationTest`, `ZipBZip2FailureTest`, and `ZipBZip2CentralDirectoryTest`. LZMA/LZMA2/XZ coverage is provided by `LZMAStreamTest`, `LZMACodecTest`, `LZMAZipTest`, `LZMACentralDirectoryTest`, `LZMA2StreamTest`, `LZMA2CodecTest`, `XZStreamTest`, `XZCodecTest`, `XZZipTest`, `XZCentralDirectoryTest`. They cover independent decoder/fixture interoperability, compression levels, malformed and truncated data, encryption, splitting, editing, merging, and protected central directories. Fixture provenance is documented in [`src/test/resources/deflate/README.md`](src/test/resources/deflate/README.md), [`src/test/resources/bzip2/README.md`](src/test/resources/bzip2/README.md), [`src/test/resources/lzma/README.md`](src/test/resources/lzma/README.md), [`src/test/resources/lzma2/README.md`](src/test/resources/lzma2/README.md), [`src/test/resources/xz/README.md`](src/test/resources/xz/README.md).

Run the focused compression tests from the repository root:

```shell
mvn -pl archive -am "-Dtest=DeflateStreamTest,Deflate64ZipTest,Deflate64CentralDirectoryTest,BZip2StreamTest,ZipBZip2IntegrationTest,ZipBZip2FailureTest,ZipBZip2CentralDirectoryTest" -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl archive -am "-Dtest=LZMAStreamTest,LZMACodecTest,LZMAZipTest,LZMACentralDirectoryTest,LZMA2StreamTest,LZMA2CodecTest,XZStreamTest,XZCodecTest,XZZipTest,XZCentralDirectoryTest" -Dsurefire.failIfNoSpecifiedTests=false test
```

Tests are located in `src/test/java/org/nervousync/test/archive` and cover regular reads/writes, ZIP64, splitting, editing, merging, paths and links, file attributes, password/certificate credential matrices, AES/Strong Encryption, and central directory protection. Some link tests may be aborted because of local file-system or privilege limitations. Assess coverage alongside actual CI results.

Also note the following current limitations:

1. ZIP is the main usable container. The TAR adapter is unfinished, and no 7z container implementation is provided.
2. ZIP entries and protected central directories support STORE/DEFLATE/DEFLATE64/BZIP2/LZMA/XZ. `LZMA1` selects method 14 and `LZMA2` selects method 95 with XZ framing. XZ supports a single LZMA2 filter per Block; BCJ/Delta/filter chains and Zstandard are not implemented. Deflate64 writing currently uses stored or fixed-Huffman blocks.
3. `WritableOptions`, some legacy directory-selection fields, and `StreamOwner` exist but are not fully integrated into the new `ArchiveFile` workflow. `streamOwner(CALLER)` does not guarantee that a supplied `DataFile` will remain open.
4. Fields such as `readHiddenFiles`, `includeRootFolder`, `rootFolderInZip`, and `fileNameInZip` do not form a unified directory-selection workflow in the new write path. Control entry names through `addDirectory` arguments or the archive path passed to `openWriter`, and organize hidden-file filtering explicitly in the calling application.
5. Hard-link creation, full ACL/ownership/arbitrary extended-attribute restoration, automatic certificate revocation checks, and certificate trust-chain policies are outside the current high-level implementation.
6. Session publication uses temporary output and backup mechanisms but does not guarantee power-loss durability or atomic transactions across multiple volumes.
7. Certificate preference and password fallback in `BOTH` have the conditions described above. Applications needing other credential-selection policies should explicitly organize Providers and retries.
8. Internal round-trip tests do not replace third-party interoperability fixtures. Before enabling certificates, Strong Encryption, or protected central directories, verify that the consuming tool supports the corresponding ZIP extensions.
