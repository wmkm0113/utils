# utils-archive

[English](README.md) | [简体中文](README_zh_CN.md) | [繁體中文](README_zh_TW.md)

`utils-archive` 是 Nervousync Java Utilities 的归档模块，当前主要实现
ZIP 的创建、读取、解压、修改和分卷合并，并提供密码、证书及中央目录保护功能。

## 目录

- [功能范围](#功能范围)
- [依赖与运行环境](#依赖与运行环境)
- [核心接口与默认值](#核心接口与默认值)
- [会话提交与回滚](#会话提交与回滚)
- [创建压缩包与添加文件](#创建压缩包与添加文件)
- [流式写入](#流式写入)
- [查询与枚举条目](#查询与枚举条目)
- [读取与解压](#读取与解压)
- [压缩算法与压缩等级](#压缩算法与压缩等级)
- [密码加密](#密码加密)
- [pkware-强加密](#pkware-强加密)
- [证书加密与解密](#证书加密与解密)
- [中央目录保护](#中央目录保护)
- [分卷压缩包](#分卷压缩包)
- [zip64-与大文件](#zip64-与大文件)
- [修改删除与覆盖](#修改删除与覆盖)
- [文件属性目录与链接](#文件属性目录与链接)
- [资源限制与校验](#资源限制与校验)
- [临时文件并发与资源生命周期](#临时文件并发与资源生命周期)
- [底层接口与格式模型](#底层接口与格式模型)
- [旧接口兼容](#旧接口兼容)
- [测试与当前限制](#测试与当前限制)

## 功能范围

| 功能           | 当前实现                                                             |
|----------------|----------------------------------------------------------------------|
| ZIP 创建与读取 | 支持本地文件和基于 `DataFile` 的可随机访问数据源                     |
| 文件与目录写入 | 支持文件、显式目录条目、符号链接及带元数据的流式写入                 |
| 压缩算法 | ZIP 条目读写支持 STORE、DEFLATE、DEFLATE64、BZIP2、LZMA（方法 14）和 XZ（方法 95） |
| 查询与解压     | 按路径查询、条目输入流、单条目提取、整包解压                         |
| 修改           | 在已有包中新增、替换、删除条目，关闭会话时重写并发布                 |
| 大文件         | ZIP64 AUTO、ALWAYS、NEVER；大小与偏移使用 `long`                     |
| 原生分卷       | `.z01`、`.z02`、最后 `.zip` 的读写与合并                             |
| 连续字节切片   | `.zip.001`、`.zip.002` 的读写与合并                                  |
| 传统密码加密   | ZipCrypto                                                            |
| WinZip AES     | 128/192/256 位；可选 AE-1、AE-2                                      |
| PKWARE 强加密  | AES、DES、3DES、RC2 NEW、RC4、Blowfish、Twofish，具体参数见后文      |
| 证书凭据       | X.509 RSA 证书、证书与私钥接口、PKCS#12 文件、多接收者               |
| 凭据策略       | 密码、证书、密码或证书；条目与中央目录分别配置                       |
| 中央目录保护   | 压缩、强加密、哈希及对应读取、修改、合并处理                         |
| 文件属性       | DOS 时间、NTFS/UT 时间扩展字段、Unix 模式、DOS 属性                  |
| 链接提取       | 受控提取符号链接和已识别的硬链接；包含路径与依赖检查                 |
| 结构解析       | Local/Central Header、Data Descriptor、ZIP64、扩展字段和可选目录记录 |
| 资源限制       | 条目数、单条目与总大小、输入大小、头大小、压缩比、路径深度与名称长度 |

`TAR` 入口与相关压缩枚举已经存在，但 `TarAdaptor`
的读写、解压、提交目前仍为空实现。不能把
TAR、TAR.GZ、TAR.BZ2、TAR.XZ 或 7z 容器当作本模块当前可用功能。

## 依赖与运行环境

源码目标版本为 Java 17。仓库 CI 配置包含 JDK
17、21、25；具体组合是否通过以对应构建结果为准。

当前分支的模块坐标为：

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

版本号应与项目实际使用的发行版本或本地构建保持一致。若当前分支版本尚未发布，可在仓库根目录执行：

```shell
mvn -pl archive -am install -DskipTests
```

模块运行依赖包含 `utils-commons`、Jakarta Annotation API 和
Bouncy Castle。Commons Compress 在当前模块 POM 中为 **test**
范围，用于构造部分测试样本，ZIP 生产实现不通过它完成读写。

强加密使用的 `CryptoUtils` 通过 Java SPI 选择
`CryptoProvider`。需要使用项目提供的 Bouncy Castle
适配器及完整强加密算法组合时，在应用中加入同版本
`utils-security`：

```xml
<dependency>
    <groupId>org.nervousync</groupId>
    <artifactId>utils-security</artifactId>
    <version>${nervousync.version}</version>
    <scope>runtime</scope>
</dependency>
```

仅存在 Bouncy Castle JAR 不等于已经加载项目的
`CryptoProvider` 实现。制作合并 JAR 时，也应保留/合并
`META-INF/services`，使 SPI 能找到相应适配器。

## 核心接口与默认值

| 接口/类型        | 用途                                                       |
|------------------|------------------------------------------------------------|
| `ArchiveFile`    | 归档会话入口，提供添加、查询、读取、提取、删除、合并、回滚 |
| `LocalDataFile`  | 本地可随机访问文件实现                                     |
| `DataFile`       | 可扩展的数据访问接口，包含读写、定位、长度和分卷枚举       |
| `ArchiveEntry`   | 条目路径、类型、大小、时间、权限和链接目标的描述           |
| `ResourceWriter` | 写入一个条目，显式调用 `finish()` 注册完成结果             |
| `ResourceReader` | 读取或提取一个条目                                         |
| `ArchiveOptions` | 字符集、校验、严格模式、凭据提供器、资源限制               |
| `ZipOptions`     | 压缩、加密、中央目录保护、分卷、ZIP64、重复条目策略        |
| `ExtractOptions` | 目标覆盖、属性恢复、链接、安全路径和继续解压策略           |

### 打开模式

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

下文 Java
代码块为可放入同一个示例类的方法或内部接口，统一使用以上导入；示例方法通过参数接收源文件、目标路径与凭据。

- `new LocalDataFile(path, "r")`：只读，文件需要已经存在。
- `new LocalDataFile(path, "rw")`：文件不存在时为
  CREATE，已经存在时为 EDIT。
- `"rw"` **不会自动清空已有压缩包**。需要创建全新包时，应使用尚不存在的目标路径。
- 创建前应确保目标父目录存在。不要同时让多个写会话修改同一目标或同一组分卷。

### 主要默认值

| 选项                               | 默认值                                      |
|------------------------------------|---------------------------------------------|
| 条目名/注释字符集                  | UTF-8                                       |
| 密码编码                           | `PasswordProvider.fixed(password)` 默认为旧式低字节编码，传入 `true` 使用 UTF-8           |
| `ArchiveOptions.verify` / `strict` | `true` / `true`                             |
| 密码/证书提供器                    | `none()`                                    |
| 压缩方法/等级                      | DEFLATE / 5                                 |
| 条目加密、中央目录压缩/加密/哈希   | 默认关闭                                    |
| `aesCrypto(bits)` 的 AES 版本      | AE-2；可用重载显式指定                      |
| ZIP64                              | AUTO                                        |
| 分卷                               | 关闭；类型默认 DETECT，写入时请显式选择类型 |
| 重复名称                           | `DuplicatePolicy.LAST`                      |
| DOS 时间使用的时区                 | `TimeZone.getDefault()`                     |
| 解压覆盖                           | `OverrideMode.NEWER`                        |
| 解压不安全条目                     | `UnsafePolicy.REJECT`                       |
| 恢复时间/权限                      | `true` / `false`                            |
| 允许符号链接/硬链接                | `false` / `false`                           |
| 自动建目录/遇错继续                | `true` / `false`                            |

## 会话提交与回滚

`ArchiveFile.close()` 对可写会话调用提交。
`ResourceWriter.finish()`
只完成并注册当前条目；归档会话关闭时才合并临时条目、写出目录和结束记录并发布文件。

必须注意：Java 的 try-with-resources 在代码块抛出异常时仍调用
`close()`。`ArchiveFile`
不会自动得知业务代码块是否成功。如果需要取消整个会话，应在异常离开代码块之前调用
`rollback()`。

以下辅助方法统一处理业务失败时的回滚。后面的写入示例复用它：

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

提交自身失败时，`ArchiveFile.close()` 会尝试回滚，并把回滚异常添加为
suppressed exception。调用 `rollback()` 后不应继续复用该写会话。

## 创建压缩包与添加文件

`addFile(entryPath, filePath, comment)`
的第一个参数是包内路径，第二个参数是源文件路径。包内路径使用
`/` 分隔。

```java
static void createBasicZip(Path target, Path sourceFile, Path sourceFolder) throws Exception {
	writeArchive(target, ArchiveOptions.newBuilder().build(), ZipOptions.newBuilder().build(), archive -> {
		archive.addFile("documents/readme.txt", sourceFile.toString(), "说明文件");
		archive.addDirectory("assets", sourceFolder.toString());
	});
}
```

`addDirectory(rootPath, folderPath)`
把枚举得到的源文件按相对路径添加到给定包内根路径；`rootPath`
可用空字符串。该便捷方法目前基于 `FileUtils.listFiles()`
，不能代替对空目录和链接的完整文件树遍历。需要精确保留这些结构时，应自行遍历并使用
`ResourceWriter.write(Path)`。

通过 Writer 添加文件时，`Path`、`File` 和 `String` 都有对应写入入口：

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

`ZipEntryResourceWriter.write(Path)` 会读取源属性。显式目录条目的包内名称必须以
`/` 结尾。

## 流式写入

对网络流、生成的数据或其他输入流，可以先创建 `ArchiveEntry`
，再写入条目输出流。无需预先计算 CRC 或压缩后的大小。

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

本例的 `source` 由调用方关闭。输出流先关闭以完成压缩和加密，再调用
`writer.finish()`。Writer 会按实际数据计算
CRC、原始大小、最终存储大小，而不是要求调用者填写加密开销。

写入链路为：原始数据计数/CRC → 可选压缩 → 可选加密 →
条目临时文件。提交时再将条目数据合并到最终 ZIP。

`openStream(long)` 的参数是 **DOS 日期时间打包值**，不是
epoch 毫秒。需要传递时间属性时优先使用
`openStream(ArchiveEntry)`。

## 查询与枚举条目

`ArchiveFile.entry(path)` 返回 `Optional<ArchiveEntry>`
，适合已知包内路径的查询。创建或编辑期间的待提交结果不应通过旧的目录索引判断，提交后重新打开可读取最终状态。

当前 `ArchiveFile` 尚未提供枚举所有条目的便捷方法。需要完整目录时，可通过
`ZipFileOperator` 读取中央目录：

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

`getFileHeaders()` 包含原始中央目录中的条目，包括重复名称；
`entryHeaders()` 返回应用重复名称策略后选中的条目。底层
`ZipFileOperator` 不等同于完整的 `ZipAdaptor`
会话校验；上述示例适合目录检查，常规读取和解压仍通过
`ArchiveFile`。

## 读取与解压

### 读取条目到输出流

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

`destination` 由调用方管理。读取流关闭时可能继续读取剩余数据并完成校验，
`close()` 本身也可能抛出异常。应同时处理读取和关闭阶段的失败。

### 解压整个压缩包

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

默认 `NEVER/NEWER/ALWAYS` 的行为分别为：保留已有目标、仅用更新的归档条目替换、允许替换。默认是
`NEWER`，示例显式使用 `ALWAYS`。

### 提取单个条目到指定文件

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

`ArchiveFile.extract()` 的目标是解压根目录；
`ResourceReader.extract()`
的目标是这个条目的最终路径，二者含义不同。依赖其他条目的硬链接更适合通过整包解压处理。

普通文件提取先写临时文件，完成数据校验与属性处理后再发布，降低错误密码、损坏数据造成残留目标或覆盖已有文件的风险。整包解压不是跨所有条目的事务：后续条目失败不会自动撤销前面已经提取成功的文件。

## 压缩算法与压缩等级

当前 ZIP 条目读写和受保护中央目录支持以下压缩方法：

| 算法 | ZIP 方法编号 | 等级行为 |
|------|--------------|----------|
| `STORE` | 0 | 原样保存数据，压缩等级不生效 |
| `DEFLATE` | 8 | `0..9`，压缩使用 JDK 原始 DEFLATE 模式 |
| `DEFLATE64` | 9 | `0` 输出未压缩块；`1..9` 提高匹配长度搜索上限，使用固定 Huffman 编码 |
| `BZIP2` | 12 | `1..9` 对应 100000..900000 字节的块容量；`0` 映射为 `1` |
| `LZMA1` | 14 | ZIP LZMA；`0..9` 对应 64 KiB..8 MiB 字典与 32..273 的匹配长度上限；`0` 仍会压缩 |
| `LZMA2` | 95 | ZIP XZ，内部使用 LZMA2；`0..9` 调整匹配搜索，字典固定为 64 KiB；`0` 仍会压缩 |

`ZipOptions.Builder.compressionLevel(...)` 仅接受 `0..9`，默认值为 `5`，不接受 JDK 的特殊等级 `-1`。BZip2 等级 `0` 仍会压缩数据；需要禁用压缩时请选择 `STORE`。

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
				archive.addFile("data/payload.bin", source.toString(), "BZip2 数据");
			});
}
```

向同一辅助方法传入 `CompressionAlgorithm.DEFLATE64`、`CompressionAlgorithm.LZMA1` 或 `CompressionAlgorithm.LZMA2`，即可分别选择 Deflate64、ZIP LZMA（方法 14）或 ZIP XZ（方法 95）。在 ZIP 中，`LZMA2` 选项使用完整 XZ 封装，不会直接把原始 LZMA2 数据作为条目写入。需要按相同算法和等级压缩中央目录时，可用 `ZipOptions.newBuilder(options).enableCentralDirectoryCompress().build()` 复制并调整选项。

```java
ZipOptions lzmaOptions = compressionOptions(CompressionAlgorithm.LZMA1, 5);
ZipOptions xzOptions = compressionOptions(CompressionAlgorithm.LZMA2, 5);
```

需要固定 DOS 时间的时区或在现有配置上调整参数时，可以复制选项：

```java
static ZipOptions utcOptions(ZipOptions source) throws Exception {
	return ZipOptions.newBuilder(source)
			.timezone(TimeZone.getTimeZone("UTC"))
			.build();
}
```

- DEFLATE 与 Deflate64 共用 `DeflateInputStream` 和 `DeflateOutputStream`。解码支持未压缩块、固定 Huffman 块和动态 Huffman 块；Deflate64 编码当前输出未压缩块或固定 Huffman 块，压缩率不保证与生成动态表的编码器一致。
- BZip2 使用模块自身的 `BZip2InputStream` 和 `BZip2OutputStream`。ZIP 条目 CRC-32 与 BZip2 的块/成员 CRC 是分别执行的完整性检查。
- 目录条目按 STORE 写入，且不进行条目数据加密。
- 压缩方法与等级通过 `ZipOptions` 配置，不是从 `ArchiveEntry` 读取。同一个会话中逐条选择不同算法的专用配置入口尚未提供。
- `LZMA1` 使用模块自身的 LZMA1 编解码器及 ZIP 方法 14 封装。条目写入包含流结束标记，并设置对应的 ZIP 标志位。
- `LZMA2` 对应 ZIP 方法 95，通过完整 XZ 容器封装原始 LZMA2 数据。XZ 解码支持多个 Block，每个 Block 仅含一个 LZMA2 过滤器，支持 NONE、CRC32、CRC64、SHA256 数据校验类型；不支持 BCJ、Delta 或过滤器链。ZIP XZ 写入默认采用 CRC64。
- LZMA1 和 LZMA2/XZ 的等级 `0` 仍会压缩数据。这些等级用于调整本实现的字典或匹配搜索行为，不等同于外部 LZMA/XZ 工具的预设等级。
- Zstandard 尚未实现。独立 LZMA2/XZ 流的支持不代表已经实现 TAR.XZ 或 7z 容器。
- 优先考虑 ZIP 通用兼容性时使用 DEFLATE。选择 Deflate64、BZip2、LZMA、XZ 或受保护中央目录前，应确认接收工具支持对应扩展。

## 密码加密

### 提供密码

密码通过 `ArchiveOptions.passwordProvider(...)` 提供，而不是写在
`ZipOptions` 中：

```java
static ArchiveOptions passwordOptions(char[] password) {
	return ArchiveOptions.newBuilder()
			.passwordProvider(PasswordProvider.fixed(password, true))
			.build();
}
```

Provider 每次请求应返回新的 `PasswordCredentials`
。模块会关闭并销毁使用完的凭据，不应反复返回同一个已被销毁的凭据对象。

需要按密码用途选择不同密码时，可以读取 `PasswordRequest` 的
`purpose`、归档路径、条目路径与尝试次数：

```java
static ArchiveOptions separatePasswords(char[] entryPassword, char[] directoryPassword) {
	return ArchiveOptions.newBuilder().passwordProvider(request -> {
		boolean directory = request.getPurpose() == PasswordPurpose.READ_HEADER
				|| request.getPurpose() == PasswordPurpose.WRITE_HEADER;
		return Optional.of(PasswordCredentials.of(directory ? directoryPassword : entryPassword, true));
	}).build();
}
```

没有可用密码时返回 `Optional.empty()`。当前不能仅凭 `attempt`
字段推断模块会自动弹窗或循环重试；交互和重试应由业务组织。

`PasswordProvider.fixed(password)` 默认使用旧式低字节编码；第二个参数传入 `true` 可选择 UTF-8，如上例所示。自定义 Provider 应在 `PasswordCredentials.of(password, utf8Encoding)` 中设置同一标记。读取时需匹配压缩包生成时使用的编码。文件名字符集与密码编码相互独立。

### ZipCrypto 与 WinZip AES

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

AES 密钥长度参数是 128、192、256。`aesCrypto(256)` 默认选择
AE-2，`aesCrypto(256, AesVendor.AE_1)` 显式选择 AE-1。

| AES 版本 | CRC 与完整性验证                                 |
|----------|--------------------------------------------------|
| AE-1     | 保存实际 CRC，并结合 AES 认证数据验证            |
| AE-2     | ZIP 头 CRC 为零，使用 AES 认证数据验证密文完整性 |

读取时根据文件头识别条目加密方式。解密普通 AES ZIP 时通常只需要密码
Provider，不需要把写入时的 `aesCrypto(...)` 再配置一遍。强加密与
WinZip AES 是不同的 ZIP 加密格式。

## PKWARE 强加密

通过 `strongCrypto(algorithm, keyBitLength)` 配置密码强加密：

```java
static void createStrongZip(Path target, Path source, char[] password) throws Exception {
	ZipOptions zipOptions = ZipOptions.newBuilder()
			.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256)
			.build();
	writeArchive(target, passwordOptions(password), zipOptions,
			archive -> archive.addFile("strong/payload.bin", source.toString(), ""));
}
```

当前 `StrongKey.validate(...)` 与对应加密器接受的主要参数为：

| 算法     | `ZipGlobals` 常量后缀             | 密钥位数                           |
|----------|-----------------------------------|------------------------------------|
| DES      | `DES`                             | 56 或 64；公开 ZIP 选项会规范为 64 |
| 3DES     | `3DES_112` / `3DES_168`           | 112 / 168                          |
| AES      | `AES_128` / `AES_192` / `AES_256` | 128 / 192 / 256                    |
| RC2 NEW  | `RC2_NEW`                         | 40、64、128                        |
| RC4      | `RC4`                             | 40、64、128                        |
| Blowfish | `BLOWFISH`                        | 32..320，且为 8 的倍数             |
| Twofish  | `TWOFISH`                         | 128、192、256                      |

常量前缀统一为 `ZIP64_HEADER_ENC_ALGORITHM_`。RC2 OLD
虽然有格式常量和识别分支，但当前强加密写入不支持。分组长度与密钥位数是两个概念，不能用算法块大小推导
`keyBitLength`。

这些能力表示当前代码的格式实现与算法范围，不代表所有算法都能被同一个第三方解压工具读取。内部写入/读取测试也不能替代外部格式互操作测试。

## 证书加密与解密

### 访问模式

| `StrongAccessMode` | 创建时需要              | 解密时需要                 |
|--------------------|-------------------------|----------------------------|
| PASSWORD           | 密码                    | 密码                       |
| CERTIFICATE        | 接收者 RSA 公钥证书列表 | 任一匹配接收者证书及其私钥 |
| BOTH               | 密码和接收者证书列表    | 密码或匹配证书私钥之一     |

**BOTH 是密码或证书，不是必须同时输入两者。** 创建 BOTH
时需要同时提供两种解锁途径，解密时可选择其中一种。

当前证书方案基于 X.509 RSA
公钥封装会话密钥。内容加密算法仍可选择上述支持的强加密算法，证书用途并不限于
AES。当前没有 ECDSA/EC 证书接收者的对应实现。

### 读取接收者公钥证书并创建压缩包

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

创建证书包只需要接收者公钥证书，不需要接收者私钥，也不需要在
`ArchiveOptions` 中设置解密用的 `CertificateProvider`
。可以传入多个接收者，当前接收者数量上限常量为 1024。

### 使用证书与私钥接口解密

```java
static ArchiveOptions certificateOptions(X509Certificate certificate, PrivateKey privateKey) throws Exception {
	CertificateCredentials credentials = new CertificateCredentials(certificate, privateKey, null);
	return ArchiveOptions.newBuilder()
			.certificateProvider(CertificateProvider.fixed(credentials))
			.build();
}
```

把返回的 `ArchiveOptions` 传给前面的 `readEntry()`、
`extractOne()` 或 `extractAll()`。`CertificateCredentials`
第三个参数允许传入实际执行私钥操作的 JCA Provider；不指定时传
`null`。

自定义 `CertificateProvider`
时，可以根据请求中的公钥哈希和归档/条目上下文寻找凭据。没有匹配凭据时返回
`Optional.empty()`；证书和私钥应成对匹配。

### 从 PKCS#12 加载私钥

```java
static ArchiveOptions pkcs12Options(Path keyStore, char[] storePassword,
                                    char[] keyPassword) throws Exception {
	PKCS12CertificateProvider provider = PKCS12CertificateProvider.load(
			keyStore, storePassword, keyPassword);
	return ArchiveOptions.newBuilder().certificateProvider(provider).build();
}
```

存储密码和私钥密码分开传递。加载器遍历 PKCS#12 中的密钥项，选择包含
RSA X.509 证书和私钥的条目；没有可用项会抛出异常。公钥证书文件本身不能用于解密。

### BOTH 与双 Provider

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

读取 BOTH 时优先收集证书候选密钥，没有候选时才使用密码。
`Optional.empty()` 可以允许回退；Provider
抛出异常或已有证书候选在后续校验中失败，不应理解为一定会自动转用密码。

## 中央目录保护

条目数据加密与中央目录保护可以独立配置。仅加密条目数据不会自动隐藏中央目录中的文件名。

### 只压缩或只计算哈希

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

中央目录压缩复用 ZIP 选项的压缩方法与等级，支持 STORE、DEFLATE、DEFLATE64、BZIP2、LZMA（方法 14）和 XZ（方法 95），分别通过 `CompressionAlgorithm.LZMA1` 和 `CompressionAlgorithm.LZMA2` 选择 LZMA 与 XZ。与条目压缩一致，BZIP2 等级 `0` 映射为块大小 `1`。编辑和分卷合并可以读取并重新生成这些方法压缩的中央目录。哈希针对变换前的中央目录字节计算；它与 AES 条目认证码是不同机制，也不等同于数字签名。

支持的哈希常量后缀包括 `NONE`、`CRC32`、`MD5`、`SHA1`、
`RIPEMD160`、`SHA256`、`SHA384`、`SHA512`，前缀为
`ZIP64_HEADER_HASH_ALGORITHM_`。

### 条目与中央目录均使用密码强加密

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

调用
`writeArchive(target, passwordOptions(password), protectedDirectoryOptions(), action)`
即可使用。写入中央目录时按原始目录字节计算哈希，再压缩、加密，最终写入
ZIP64 Protection 元数据；读取时执行逆过程并核对大小、尾部与哈希。

### 条目仅允许证书，中央目录仅允许密码

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

创建时提供中央目录密码和接收者公钥证书；读取整个包时需要同时配置密码
Provider 与证书 Provider，因为它们分别负责两层数据。每一层的访问模式都是独立的。

`centralDirectoryCrypto(...)` 也有带 `StrongAccessMode`
和接收者列表的重载，可实现相反组合或
BOTH。打开受加密中央目录保护的包时就需要目录解密凭据，不能等到读取某个条目时才提供。

## 分卷压缩包

### 配置分卷

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

`baseTarget` 例如 `backup.zip`：

| 类型          | 创建时的配置          | 输出文件                                      | 读取入口         |
|---------------|-----------------------|-----------------------------------------------|------------------|
| 原生 ZIP 分卷 | `SplitType.NATIVE`    | `backup.z01`、`backup.z02`、最后 `backup.zip` | `backup.zip`     |
| 连续切片      | `SplitType.NUMBERED` | `backup.zip.001`、`backup.zip.002`…           | `backup.zip.001` |

- 原生 ZIP 分卷大小范围为 `65,536..0xFFFFFFFF` 字节。
- 原生分卷的单个 Local/Central File Header
  必须能完整放入某一卷；卷容量小于单个头长度时会失败。
- 编号切片可在任意字节位置切分；条目头可以跨切片。
- `splitArchive(true, ...)`
  读取时也应提供足够大的分卷长度上限，至少容纳实际各卷；通常复用创建时的配置。
- 读取时 `SplitType.DETECT` 可以根据扩展名识别两类分卷；创建时应明确使用
  NATIVE 或 NUMBERED。
- 原生模式若只产生一卷，最终文件名仍为 `backup.zip`；编号模式输出
  `backup.zip.001`。
- 分卷布局与条目加密是独立选项，编号切片不会自动禁用强加密。

例如原生 64 MiB 分卷可使用
`splitOptions(SplitType.NATIVE, 64L * 1024 * 1024)`；编号切片改为
`SplitType.NUMBERED`。

### 合并为单个 ZIP

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

源必须是分卷包。可以在只读会话调用，源分卷保持保留；目标应使用独立路径，避免与任何源卷重合。该操作重写卷号、偏移及中央目录结束记录，不是简单拼接
`.z01` 文件。

合并按已有条目的存储数据复制，并重建最终目录。受保护中央目录在重建时需要对应凭据，条目
CRC、加密元数据与 Local Header
遮蔽语义也需要保持。合并本身不代替逐条读取来核验全部条目内容。

## ZIP64 与大文件

```java
static ZipOptions zip64Options(Zip64Mode mode) throws Exception {
	return ZipOptions.newBuilder().zip64Mode(mode).build();
}
```

| 模式   | 行为                                                      |
|--------|-----------------------------------------------------------|
| AUTO   | 根据实际条目大小、偏移、数量及保护结构生成所需 ZIP64 信息 |
| ALWAYS | 强制生成相应 ZIP64 信息和 ZIP64 结束结构                  |
| NEVER  | 需要 ZIP64 表示时拒绝操作                                 |

支持超过 4 GiB 的条目或压缩包，不代表没有大小上限。还受
`ArchiveLimit`、Java `long`/集合容量、临时目录可用空间和目标文件系统限制。

默认单条目上限为 8
GiB。高度可压缩的大文件也可能触发压缩比限制，应按业务数据设置资源限制。增加上限的示例见“资源限制与校验”。

## 修改、删除与覆盖

使用 `"rw"` 打开已经存在的 ZIP 即进入
EDIT。用同一路径写入新条目可替换原条目，删除通过
`ArchiveEntry` 指定：

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

现有条目通常通过复制存储数据保留，新条目使用当前会话的 ZIP
选项生成。编辑不是给已有全部条目批量更换密码或重新压缩的入口；需要这种转换时，应读取原内容后重新创建目标包。

分卷输入在提交时也会按配置重新组织输出。会话会先写工作目录，再发布目标并处理旧卷；涉及多文件发布时不能把它等同于操作系统级跨文件原子事务。失败时可能为恢复保留工作目录。

### 重复名称

```java
static ZipOptions duplicateOptions(DuplicatePolicy policy) throws Exception {
	return ZipOptions.newBuilder().duplicatePolicy(policy).build();
}
```

- FIRST：同一路径选择第一个中央目录条目。
- LAST：选择最后一个，默认策略。
- REJECT：检测到重复时抛出异常。

此策略影响原始目录中同名条目的选择，不是允许多个调用方同时向同一个
Writer 写入的配置。目录模型保留原始头列表，便于需要保留重复条目的合并操作。

## 文件属性、目录与链接

### 元数据

`ArchiveEntry.fromPath(path, entryPath)`
读取源属性时不跟随符号链接。支持的主要字段包括：

| 字段              | 含义                                            |
|-------------------|-------------------------------------------------|
| `lastModified`    | 最后修改时间；同时生成 DOS 时间与适用的扩展字段 |
| `lastAccess`      | 最后访问时间                                    |
| `createTimestamp` | 创建时间                                        |
| `unixMode`        | Unix 类型与权限位                               |
| `dosAttributes`   | DOS 文件属性位                                  |
| `linkTarget`      | 链接目标描述                                    |

写入采用 NTFS 时间扩展字段保存更高精度时间，在范围允许时附带
UT 修改时间；DOS 时间使用 `ZipOptions.timezone(...)`
指定的时区。读取时结合中央目录和可用时间扩展字段恢复属性。

手工创建带元数据的条目：

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

把结果传给 `writer.openStream(entry)`。元数据的 `entryPath`
必须与打开 Writer 时的路径一致，Unix 文件类型也必须与
`EntryType` 相符。

解压使用 `preserveLastModified(true)`
控制当前实现中的时间属性恢复，不仅是修改时间；可用的访问/创建时间也会一并传给文件系统。
`preservePermissions(true)` 控制权限与 DOS 属性恢复。

目录属性在子条目提取后恢复，避免后续写入改变其时间。实际恢复能力受目标文件系统限制；当前会跳过符号链接自身的属性恢复。没有完整
ACL、所有者、扩展属性等任意文件系统元数据的无损保证。

### 显式写入空目录

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
				// 目录条目不写数据。
			}
			writer.finish();
		}
	});
}
```

### 符号链接与硬链接

符号链接可以通过 `writer.write(sourceLinkPath)`
保存，条目数据保存链接目标文本。也可以用 SYMBOLIC_LINK
元数据打开流，再把目标文本按归档字符集写入；仅设置
`linkTarget` 并不会自动替代手工流写入。

默认解压不允许创建链接。需要恢复时显式启用：

```java
static ExtractOptions linkExtractionOptions() throws Exception {
	return ExtractOptions.newBuilder()
			.allowSymbolicLinks(true)
			.allowHardLinks(true)
			.unsafePolicy(UnsafePolicy.REJECT)
			.build();
}
```

实现会检查链接目标和路径组件，处理已识别硬链接的前向依赖、并拒绝依赖环或越界目标。Windows
创建符号链接可能需要相应权限；硬链接也受操作系统和文件系统限制。

当前 Writer 接受 FILE、DIRECTORY、SYMBOLIC_LINK；不提供原生
HARD_LINK 元数据写入。读取/提取硬链接与写入硬链接是不同能力。

## 资源限制与校验

### 默认限制

| 限制                  | 默认值    |
|-----------------------|-----------|
| `maxEntries`          | 1,000,000 |
| `maxEntryBytes`       | 8 GiB     |
| `maxTotalBytes`       | 64 GiB    |
| `maxCompressedBytes`  | 64 GiB    |
| `maxHeaderBytes`      | 256 MiB   |
| `maxCompressionRatio` | 1000      |
| `maxNestingDepth`     | 8         |
| `maxNameCharacters`   | 4096      |

`maxNestingDepth` 当前检查条目路径的目录深度，不是自动递归打开
ZIP 中嵌套 ZIP 的次数。模块不会自动解压内嵌压缩包。

### 自定义资源限制与字符集

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

对使用 UTF-8 标志的条目，读取时按 UTF-8 解码；其余名称和注释使用配置字符集。非
ASCII 密码另由 `PasswordCredentials.of(password, utf8Encoding)` 控制。

### 路径安全与错误策略

- 整包提取检查目标规范化后的路径是否仍位于解压根目录内。
- 检查已有父路径组件，处理符号链接引起的目录逃逸问题。
- `UnsafePolicy.REJECT` 拒绝不安全条目；SKIP 在相应提取检查中跳过该条目。
- `strict(true)` 同时影响解析阶段的结构、路径和 Local/Central
  Header 一致性检查。解析阶段已经拒绝的非法条目不会等到提取时再由
  SKIP 处理。
- `continueOnError(true)` 用于允许部分提取错误后继续，不会把全部结构错误或安全拒绝转成成功。
- `ResourceReader.extract(target, ...)`
  是指定最终路径的单条目操作；不可信归档优先使用具有明确根目录语义的整包提取。

### 校验行为

- 普通 ZIP 条目按配置校验 CRC、大小与资源限制。
- AE-1 保留 CRC，AE-2 依赖 AES 认证数据；认证失败会抛出异常。
- 强加密检查凭据、解密验证块与对应算法的数据结束条件。
- 中央目录根据 Protection 校验结构、大小及配置的哈希。
- `verify(false)` 不等于关闭所有格式、认证或资源检查。
- 流关闭可能完成剩余读取与认证，应等待关闭成功后再把业务结果认定为成功。

异常可以是 `ArchiveException`、底层 `IOException`
、选项构建或密码/证书相关异常。
`ArchiveException.getErrorCode()`
可取得模块错误码；显示文本依赖项目的国际化资源。避免只依赖异常消息字符串判断错误。

## 临时文件、并发与资源生命周期

### 写入与发布

每个条目单独写入会话临时文件，数据经过流式压缩/加密，不要求把条目完整放入内存。最终
ZIP 由提交阶段生成，因此这不是直接向 HTTP 响应连续输出完整压缩包的高层接口。

临时工作目录位于目标父目录，名称形如 `.nervousync-zip-*`
。必须保证源、目标及暂存数据的磁盘空间；修改已有包时还可能需要输出与备份空间。成功提交或正常回滚会清理工作目录，发布失败时可能保留恢复数据。

### 并发约束

独立条目暂存提供了并行准备的基础，但当前没有面向调用方的线程池或完整线程安全会话承诺。
`openWriter()` 的查找/注册以及会话状态、提交、回滚需要协调。

常规用法应串行操作会话。需要并行准备时，由控制线程创建不同路径的
Writer，工作线程分别写入独立条目；等所有任务结束后，再由控制线程完成注册和会话关闭。不要同时写同一个
Writer，也不要让提交、关闭或回滚与工作线程重叠。失败时应先等工作线程退出，再处理会话临时目录。

### 关闭规则

| 对象                          | 调用方责任                                         |
|-------------------------------|----------------------------------------------------|
| `ArchiveFile`                 | 使用 try-with-resources；关闭可能提交并抛出异常    |
| `ResourceWriter`              | 关闭条目输出流，成功后 `finish()`，最终关闭 Writer |
| `ResourceReader`              | 同时管理输入流与 Reader 的关闭                     |
| 外部 InputStream/OutputStream | 示例中由调用方创建的外部流仍由调用方管理           |
| `ZipFileOperator`             | 关闭会关闭其持有的归档输入流                       |

`writer.abort()` 标记条目取消，不等于已经把写会话恢复成可提交状态。取消整个业务操作时使用
`ArchiveFile.rollback()`。`close()` 也不等于
`ResourceWriter.finish()`，未完成的 Writer 会阻止正常提交。

## 底层接口与格式模型

对需要检查结构、调试兼容性或扩展归档实现的调用方，模块还提供：

| 类型                                              | 用途                                                    |
|---------------------------------------------------|---------------------------------------------------------|
| `ZipFileOperator`                                 | 读取 EOCD、ZIP64 EOCD/Locator、中央目录和 Local Header  |
| `ZipArchiveInputStream`                           | 可定位、可跨卷的归档字节输入；不是自动逐条解压的 ZIP 流 |
| `ZipArchiveOutputStream`                          | 卷切换、头完整性边界、目录统计、最终卷发布映射          |
| `LocalFileHeader` / `GeneralFileHeader`           | 本地头与中央目录文件头模型及序列化                      |
| `EndCentralDirectoryRecord`                       | 普通 EOCD                                               |
| `Zip64EndCentralDirectory`                        | ZIP64 Record、Locator、Protection                       |
| `ExtraDataRecord`                                 | ZIP 扩展字段                                            |
| `AesEncryptionInfo` / `StrongEncryptionInfo`      | 两种加密格式的条目元数据                                |
| `StrongDecryptionHeader` / `StrongRecipientTable` | 强加密解密头与接收者信息                                |
| `StandardInputStream` / `StandardOutputStream`    | ZipCrypto 数据流                                        |
| `AesInputStream` / `AesOutputStream`              | WinZip AES 数据流                                       |
| `StrongInputStream` / `StrongOutputStream`        | PKWARE 强加密数据流                                     |
| `DeflateInputStream` / `DeflateOutputStream` | 原始 DEFLATE/Deflate64 编解码，不包含 ZIP、zlib 或 GZIP 格式封装 |
| `BZip2InputStream` / `BZip2OutputStream` | 完整 BZh 数据流，可独立于 ZIP 使用 |
| `LZMAInputStream` / `LZMAOutputStream` | 默认采用 ZIP 方法 14 的 LZMA1 封装；`raw(...)` 和 `alone(...)` 工厂方法分别支持原始 LZMA1 与 LZMA-Alone（`.lzma`）数据流 |
| `LZMA2InputStream` / `LZMA2OutputStream` | 原始 LZMA2 数据流，不包含 XZ、ZIP 或 7z 容器封装 |
| `XZInputStream` / `XZOutputStream` | 使用 LZMA2 过滤器的完整 XZ 数据流，可独立于 ZIP 使用 |

独立 BZip2 解码器默认只读取一个成员，可通过双参数构造方法启用拼接成员。解码支持旧版随机化块，编码器输出非随机化块。完整验证 BZip2 块/成员 CRC 需要读取至 EOF；关闭独立解码器不会主动读取剩余数据。ZIP 条目和中央目录采用单成员解码；受保护中央目录读取器还会拒绝尾随数据。

独立 XZ 解码器默认读取拼接 Stream；向双参数构造方法传入 `false` 可在一个 Stream 结束后停止，ZIP 条目和中央目录读取采用此模式。需要读取至 EOF 才能完整验证 XZ 数据校验、Index 和 Footer。默认允许的声明字典大小上限为 64 MiB，此限制不限制解压后的输出大小。XZ 编码器当前对非空输入写入一个 LZMA2 Block，固定使用 64 KiB 字典；空输入产生不含 Block 的 Stream。独立编码器可通过 `XZCheck` 选择校验类型，默认采用 CRC64。

Deflate、BZip2、LZMA1、LZMA2 和 XZ 编码输出流的 `finish()` 都会完成压缩而保留底层流，`close()` 还会关闭底层流。`DeflateOutputStream.release()` 只释放编码资源，不会完成输出。独立使用编解码流时，调用方需自行限制输出大小和压缩比。

自定义 `DataFile` 时，除了普通读写，还需要正确实现 `seek()`、
`getFilePointer()`、`length()`、`newFile()` 和 `splitFiles()`
。分卷代码通过 `newFile()` 打开同配置下的其他卷，通过
`splitFiles()` 寻找卷文件；独立 Reader
也会创建新的可定位句柄。仅有顺序读取能力的网络流不能直接替代该接口。

普通 ZIP 的主体为条目 Local Header、条目数据及可选 Data
Descriptor，随后是中央目录和结束记录；需要时附带 ZIP64
结构和目录保护信息。目录模型还能读取 Archive Extra Data
Record 与 Digital Signature 数据。

保留 Digital Signature
字节不代表实现了数字签名验签或证书信任链校验。直接操作底层模型时，需要自行维护偏移、卷号、大小、ZIP64
占位、认证开销与流所有权。常规应用优先使用 `ArchiveFile`
，避免自行拼装结构。

## 旧接口兼容

模块保留 `org.nervousync.zip.ZipFile` 和
`org.nervousync.zip.options.ZipOptions`
，提供旧式创建、添加、读取、提取、删除、密码设置、注释及分卷合并等方法。

- 旧的 `ZipFile.openZipFile(...)` 与新的
  `ArchiveFile.zipFile(...)` 是不同入口。
- 上述压缩支持适用于 `org.nervousync.archive`；保留的旧 ZIP 调用链没有因本次重构而获得 Deflate64、BZip2、LZMA 或 XZ 支持。
- 新文档示例使用的是
  `org.nervousync.archive.zip.ZipOptions`。
- 旧包中的头模型和选项不能直接传入新接口。
- 证书、双 Provider 与中央目录 Protection
  的用法以新接口为准，不应推断旧接口具有同等能力。

维护旧应用时保留对应旧调用链；新接入可以从本文的会话接口开始。

## 测试与当前限制

在仓库根目录运行模块及其上游模块测试：

```shell
mvn -pl archive -am test
```

只运行一组 ZIP 测试：

```shell
mvn -pl archive -am -Dtest=ZipFileTest -Dsurefire.failIfNoSpecifiedTests=false test
```

需要执行受开关控制的大 ZIP 用例时：

```shell
mvn -pl archive -am -DrunLargeZipTest=true test
```

压缩相关测试包括 `DeflateStreamTest`、`Deflate64ZipTest`、`Deflate64CentralDirectoryTest`、`BZip2StreamTest`、`ZipBZip2IntegrationTest`、`ZipBZip2FailureTest` 和 `ZipBZip2CentralDirectoryTest`。LZMA/LZMA2/XZ 测试包括 `LZMAStreamTest`, `LZMACodecTest`, `LZMAZipTest`, `LZMACentralDirectoryTest`, `LZMA2StreamTest`, `LZMA2CodecTest`, `XZStreamTest`, `XZCodecTest`, `XZZipTest`, `XZCentralDirectoryTest`。这些测试覆盖独立解码器/样本互操作、压缩等级、非法及截断数据、加密、分卷、编辑、合并和受保护中央目录。样本来源见 [`src/test/resources/deflate/README.md`](src/test/resources/deflate/README.md), [`src/test/resources/bzip2/README.md`](src/test/resources/bzip2/README.md), [`src/test/resources/lzma/README.md`](src/test/resources/lzma/README.md), [`src/test/resources/lzma2/README.md`](src/test/resources/lzma2/README.md), [`src/test/resources/xz/README.md`](src/test/resources/xz/README.md)。

在仓库根目录执行压缩相关测试：

```shell
mvn -pl archive -am "-Dtest=DeflateStreamTest,Deflate64ZipTest,Deflate64CentralDirectoryTest,BZip2StreamTest,ZipBZip2IntegrationTest,ZipBZip2FailureTest,ZipBZip2CentralDirectoryTest" -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl archive -am "-Dtest=LZMAStreamTest,LZMACodecTest,LZMAZipTest,LZMACentralDirectoryTest,LZMA2StreamTest,LZMA2CodecTest,XZStreamTest,XZCodecTest,XZZipTest,XZCentralDirectoryTest" -Dsurefire.failIfNoSpecifiedTests=false test
```

测试代码位于 `src/test/java/org/nervousync/test/archive`
，覆盖普通读写、ZIP64、分卷、修改、合并、路径与链接、文件属性、密码/证书凭据矩阵、AES/强加密及中央目录保护。部分链接用例可能因本机文件系统或权限不足而中止，应结合
CI 的实际执行结果判断覆盖情况。

当前使用时还需了解：

1. 当前可用容器主要为 ZIP；TAR 适配器未完成，未提供 7z 容器实现。
2. ZIP 条目和受保护中央目录支持 STORE/DEFLATE/DEFLATE64/BZIP2/LZMA/XZ；`LZMA1` 对应方法 14，`LZMA2` 对应使用 XZ 封装的方法 95。XZ 每个 Block 仅支持单个 LZMA2 过滤器；BCJ、Delta、过滤器链及 Zstandard 尚未实现。Deflate64 写入当前采用未压缩块或固定 Huffman 块。
3. `WritableOptions`、部分旧式目录选择字段和 `StreamOwner`
   已存在，但尚未完整接入新 `ArchiveFile` 工作流。
   `streamOwner(CALLER)` 不能作为传入 `DataFile` 一定不被关闭的保证。
4. `readHiddenFiles`、`includeRootFolder`、`rootFolderInZip`、
   `fileNameInZip` 等字段没有在新写入路径中形成统一目录选择流程；新接口通过
   `addDirectory` 参数或 `openWriter`
   的包内路径控制名称，隐藏文件筛选由调用方明确组织。
5. 创建硬链接、完整
   ACL/所有者/任意扩展属性恢复、自动证书吊销检查和证书信任链策略不属于当前高层实现。
6. 会话发布包含临时输出和备份机制，但不提供断电级或跨多个分卷的原子事务保证。
7. `BOTH` 的证书优先和密码回退有前述条件；应用若需要其他凭据选择策略，应显式组织
   Provider 和重试流程。
8. 自身往返测试不能替代第三方互操作样本。启用证书、强加密、受保护中央目录等功能前，应验证目标消费工具支持对应
   ZIP 扩展。
