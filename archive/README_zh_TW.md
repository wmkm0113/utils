# utils-archive

[English](README.md) | [简体中文](README_zh_CN.md) | [繁體中文](README_zh_TW.md)

`utils-archive` 是 Nervousync Java Utilities 的封存模組，目前主要實作
ZIP 的建立、讀取、解壓縮、修改和分卷合併，並提供密碼、憑證及中央目錄保護功能。

## 目錄

- [功能範圍](#功能範圍)
- [依賴與執行環境](#依賴與執行環境)
- [核心介面與預設值](#核心介面與預設值)
- [工作階段提交與回復](#工作階段提交與回復)
- [建立壓縮檔與新增檔案](#建立壓縮檔與新增檔案)
- [串流寫入](#串流寫入)
- [查詢與列舉項目](#查詢與列舉項目)
- [讀取與解壓縮](#讀取與解壓縮)
- [壓縮演算法與壓縮等級](#壓縮演算法與壓縮等級)
- [密碼加密](#密碼加密)
- [pkware-強加密](#pkware-強加密)
- [憑證加密與解密](#憑證加密與解密)
- [中央目錄保護](#中央目錄保護)
- [分卷壓縮檔](#分卷壓縮檔)
- [zip64-與大檔案](#zip64-與大檔案)
- [修改刪除與覆蓋](#修改刪除與覆蓋)
- [檔案屬性目錄與連結](#檔案屬性目錄與連結)
- [資源限制與驗證](#資源限制與驗證)
- [暫存檔案並行與資源生命週期](#暫存檔案並行與資源生命週期)
- [底層介面與格式模型](#底層介面與格式模型)
- [舊介面相容](#舊介面相容)
- [測試與目前限制](#測試與目前限制)

## 功能範圍

| 功能           | 目前實作                                                             |
|----------------|----------------------------------------------------------------------|
| ZIP 建立與讀取 | 支援本地檔案和基於 `DataFile` 的可隨機存取資料來源                     |
| 檔案與目錄寫入 | 支援檔案、明確目錄項目、符號連結及帶中繼資料的串流寫入                 |
| 壓縮演算法 | ZIP 項目讀寫支援 STORE、DEFLATE、DEFLATE64、BZIP2、LZMA（方法 14）和 XZ（方法 95） |
| 查詢與解壓縮     | 按路徑查詢、項目輸入串流、單一項目擷取、整個壓縮檔解壓縮                         |
| 修改           | 在既有壓縮檔中新增、替換、刪除項目，關閉工作階段時重寫並發佈                 |
| 大檔案         | ZIP64 AUTO、ALWAYS、NEVER；大小與位移使用 `long`                     |
| 原生分卷       | `.z01`、`.z02`、最後 `.zip` 的讀寫與合併                             |
| 連續位元組切片   | `.zip.001`、`.zip.002` 的讀寫與合併                                  |
| 傳統密碼加密   | ZipCrypto                                                            |
| WinZip AES     | 128/192/256 位元；可選 AE-1、AE-2                                      |
| PKWARE 強加密  | AES、DES、3DES、RC2 NEW、RC4、Blowfish、Twofish，具體參數見後文      |
| 憑證認證資訊       | X.509 RSA 憑證、憑證與私密金鑰介面、PKCS#12 檔案、多接收者               |
| 認證資訊策略       | 密碼、憑證、密碼或憑證；項目與中央目錄分別配置                       |
| 中央目錄保護   | 壓縮、強加密、雜湊及對應讀取、修改、合併處理                         |
| 檔案屬性       | DOS 時間、NTFS/UT 時間額外欄位、Unix 模式、DOS 屬性                  |
| 連結擷取       | 受控擷取符號連結和已識別的硬連結；包含路徑與依賴檢查                 |
| 結構解析       | Local/Central Header、Data Descriptor、ZIP64、額外欄位和可選目錄記錄 |
| 資源限制       | 項目數、單一項目與總大小、輸入大小、標頭大小、壓縮比、路徑深度與名稱長度 |

`TAR` 進入點與相關壓縮列舉已經存在，但 `TarAdaptor`
的讀寫、解壓縮、提交目前仍為空實作。不能把
TAR、TAR.GZ、TAR.BZ2、TAR.XZ 或 7z 容器當作本模組目前可用功能。

## 依賴與執行環境

原始碼目標版本為 Java 17。儲存庫 CI 配置包含 JDK
17、21、25；具體組合是否通過以對應建置結果為準。

目前分支的模組座標為：

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

版本號應與專案實際使用的發行版本或本地建置保持一致。若目前分支版本尚未發佈，可在儲存庫根目錄執行：

```shell
mvn -pl archive -am install -DskipTests
```

模組執行依賴包含 `utils-commons`、Jakarta Annotation API 和
Bouncy Castle。Commons Compress 在目前模組 POM 中為 **test**
範圍，用於構造部分測試樣本，ZIP 生產實作不透過它完成讀寫。

強加密使用的 `CryptoUtils` 透過 Java SPI 選擇
`CryptoProvider`。需要使用專案提供的 Bouncy Castle
配接器及完整強加密演算法組合時，在應用程式中加入同版本
`utils-security`：

```xml
<dependency>
    <groupId>org.nervousync</groupId>
    <artifactId>utils-security</artifactId>
    <version>${nervousync.version}</version>
    <scope>runtime</scope>
</dependency>
```

僅存在 Bouncy Castle JAR 不等於已經載入專案的
`CryptoProvider` 實作。製作合併 JAR 時，也應保留/合併
`META-INF/services`，使 SPI 能找到相應配接器。

## 核心介面與預設值

| 介面/型別        | 用途                                                       |
|------------------|------------------------------------------------------------|
| `ArchiveFile`    | 封存工作階段進入點，提供新增、查詢、讀取、擷取、刪除、合併、回復 |
| `LocalDataFile`  | 本地可隨機存取檔案實作                                     |
| `DataFile`       | 可擴充的資料存取介面，包含讀寫、定位、長度和分卷列舉       |
| `ArchiveEntry`   | 項目路徑、型別、大小、時間、權限和連結目標的描述           |
| `ResourceWriter` | 寫入一個項目，明確呼叫 `finish()` 註冊完成結果             |
| `ResourceReader` | 讀取或擷取一個項目                                         |
| `ArchiveOptions` | 字元集、驗證、嚴格模式、認證資訊提供器、資源限制               |
| `ZipOptions`     | 壓縮、加密、中央目錄保護、分卷、ZIP64、重複項目策略        |
| `ExtractOptions` | 目標覆蓋、屬性還原、連結、安全路徑和繼續解壓縮策略           |

### 開啟模式

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
程式碼塊為可放入同一個範例類的方法或內部介面，統一使用以上匯入；範例方法透過參數接收來源檔案、目標路徑與認證資訊。

- `new LocalDataFile(path, "r")`：唯讀，檔案需要已經存在。
- `new LocalDataFile(path, "rw")`：檔案不存在時為
  CREATE，已經存在時為 EDIT。
- `"rw"` **不會自動清空既有壓縮檔**。需要建立全新壓縮檔時，應使用尚不存在的目標路徑。
- 建立前應確保目標上層目錄存在。不要同時讓多個寫工作階段修改同一目標或同一組分卷。

### 主要預設值

| 選項                               | 預設值                                      |
|------------------------------------|---------------------------------------------|
| 項目名/註解字元集                  | UTF-8                                       |
| 密碼編碼                           | `PasswordProvider.fixed(password)` 預設為舊式低位元組編碼，傳入 `true` 使用 UTF-8           |
| `ArchiveOptions.verify` / `strict` | `true` / `true`                             |
| 密碼/憑證提供器                    | `none()`                                    |
| 壓縮方法/等級                      | DEFLATE / 5                                 |
| 項目加密、中央目錄壓縮/加密/雜湊   | 預設關閉                                    |
| `aesCrypto(bits)` 的 AES 版本      | AE-2；可用多載明確指定                      |
| ZIP64                              | AUTO                                        |
| 分卷                               | 關閉；型別預設 DETECT，寫入時請明確選擇型別 |
| 重複名稱                           | `DuplicatePolicy.LAST`                      |
| DOS 時間使用的時區                 | `TimeZone.getDefault()`                     |
| 解壓縮覆蓋                           | `OverrideMode.NEWER`                        |
| 解壓縮不安全項目                     | `UnsafePolicy.REJECT`                       |
| 還原時間/權限                      | `true` / `false`                            |
| 允許符號連結/硬連結                | `false` / `false`                           |
| 自動建目錄/遇錯繼續                | `true` / `false`                            |

## 工作階段提交與回復

`ArchiveFile.close()` 對可寫工作階段呼叫提交。
`ResourceWriter.finish()`
只完成並註冊目前項目；封存工作階段關閉時才合併臨時項目、寫出目錄和結束記錄並發佈檔案。

必須注意：Java 的 try-with-resources 在程式碼塊擲出例外時仍呼叫
`close()`。`ArchiveFile`
不會自動得知業務程式碼塊是否成功。如果需要取消整個工作階段，應在例外離開程式碼塊之前呼叫
`rollback()`。

以下輔助方法統一處理業務失敗時的回復。後面的寫入範例複用它：

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

提交自身失敗時，`ArchiveFile.close()` 會嘗試回復，並把回復例外新增為
suppressed exception。呼叫 `rollback()` 後不應繼續複用該寫工作階段。

## 建立壓縮檔與新增檔案

`addFile(entryPath, filePath, comment)`
的第一個參數是封存檔內路徑，第二個參數是來源檔案路徑。封存檔內路徑使用
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
把列舉得到的來源檔案按相對路徑新增到給定封存檔內根路徑；`rootPath`
可用空字元串。該便捷方法目前基於 `FileUtils.listFiles()`
，不能代替對空目錄和連結的完整檔案樹遍歷。需要精確保留這些結構時，應自行遍歷並使用
`ResourceWriter.write(Path)`。

透過 Writer 新增檔案時，`Path`、`File` 和 `String` 都有對應寫入進入點：

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

`ZipEntryResourceWriter.write(Path)` 會讀取來源屬性。明確目錄項目的封存檔內名稱必須以
`/` 結尾。

## 串流寫入

對網路流、生成的資料或其他輸入串流，可以先建立 `ArchiveEntry`
，再寫入項目輸出串流。無需預先計算 CRC 或壓縮後的大小。

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

本例的 `source` 由呼叫端關閉。輸出串流先關閉以完成壓縮和加密，再呼叫
`writer.finish()`。Writer 會按實際資料計算
CRC、原始大小、最終儲存大小，而不是要求呼叫者填寫加密開銷。

寫入鏈路為：原始資料計數/CRC → 可選壓縮 → 可選加密 →
項目暫存檔案。提交時再將項目資料合併到最終 ZIP。

`openStream(long)` 的參數是 **DOS 日期時間打包值**，不是
epoch 毫秒。需要傳遞時間屬性時優先使用
`openStream(ArchiveEntry)`。

## 查詢與列舉項目

`ArchiveFile.entry(path)` 返回 `Optional<ArchiveEntry>`
，適合已知封存檔內路徑的查詢。建立或編輯期間的待提交結果不應透過舊的目錄索引判斷，提交後重新開啟可讀取最終狀態。

目前 `ArchiveFile` 尚未提供列舉所有項目的便捷方法。需要完整目錄時，可透過
`ZipFileOperator` 讀取中央目錄：

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

`getFileHeaders()` 包含原始中央目錄中的項目，包括重複名稱；
`entryHeaders()` 返回應用重複名稱策略後選中的項目。底層
`ZipFileOperator` 不等同於完整的 `ZipAdaptor`
工作階段驗證；上述範例適合目錄檢查，常規讀取和解壓縮仍透過
`ArchiveFile`。

## 讀取與解壓縮

### 讀取項目到輸出串流

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

`destination` 由呼叫端管理。讀取串流關閉時可能繼續讀取剩餘資料並完成驗證，
`close()` 本身也可能擲出例外。應同時處理讀取和關閉階段的失敗。

### 解壓縮整個壓縮檔

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

預設 `NEVER/NEWER/ALWAYS` 的行為分別為：保留既有目標、僅用更新的封存項目替換、允許替換。預設是
`NEWER`，範例明確使用 `ALWAYS`。

### 擷取單個項目到指定檔案

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

`ArchiveFile.extract()` 的目標是解壓縮根目錄；
`ResourceReader.extract()`
的目標是這個項目的最終路徑，二者含義不同。依賴其他項目的硬連結更適合透過整個壓縮檔解壓縮處理。

普通檔案擷取先寫暫存檔案，完成資料驗證與屬性處理後再發佈，降低錯誤密碼、損壞資料造成殘留目標或覆蓋既有檔案的風險。整個壓縮檔解壓縮不是跨所有項目的交易：後續項目失敗不會自動撤銷前面已經擷取成功的檔案。

## 壓縮演算法與壓縮等級

目前 ZIP 項目讀寫和受保護中央目錄支援以下壓縮方法：

| 演算法 | ZIP 方法編號 | 等級行為 |
|--------|--------------|----------|
| `STORE` | 0 | 原樣儲存資料，壓縮等級不生效 |
| `DEFLATE` | 8 | `0..9`，壓縮使用 JDK 原始 DEFLATE 模式 |
| `DEFLATE64` | 9 | `0` 輸出未壓縮區塊；`1..9` 提高匹配長度搜尋上限，使用固定 Huffman 編碼 |
| `BZIP2` | 12 | `1..9` 對應 100000..900000 位元組的區塊容量；`0` 對映為 `1` |
| `LZMA1` | 14 | ZIP LZMA；`0..9` 對應 64 KiB..8 MiB 字典與 32..273 的匹配長度上限；`0` 仍會壓縮 |
| `LZMA2` | 95 | ZIP XZ，內部使用 LZMA2；`0..9` 調整匹配搜尋，字典固定為 64 KiB；`0` 仍會壓縮 |

`ZipOptions.Builder.compressionLevel(...)` 僅接受 `0..9`，預設值為 `5`，不接受 JDK 的特殊等級 `-1`。BZip2 等級 `0` 仍會壓縮資料；需要停用壓縮時請選擇 `STORE`。

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
				archive.addFile("data/payload.bin", source.toString(), "BZip2 資料");
			});
}
```

向同一輔助方法傳入 `CompressionAlgorithm.DEFLATE64`、`CompressionAlgorithm.LZMA1` 或 `CompressionAlgorithm.LZMA2`，即可分別選擇 Deflate64、ZIP LZMA（方法 14）或 ZIP XZ（方法 95）。在 ZIP 中，`LZMA2` 選項使用完整 XZ 封裝，不會直接把原始 LZMA2 資料作為項目寫入。需要按相同演算法和等級壓縮中央目錄時，可用 `ZipOptions.newBuilder(options).enableCentralDirectoryCompress().build()` 複製並調整選項。

```java
ZipOptions lzmaOptions = compressionOptions(CompressionAlgorithm.LZMA1, 5);
ZipOptions xzOptions = compressionOptions(CompressionAlgorithm.LZMA2, 5);
```

需要固定 DOS 時間的時區或在現有設定上調整參數時，可以複製選項：

```java
static ZipOptions utcOptions(ZipOptions source) throws Exception {
	return ZipOptions.newBuilder(source)
			.timezone(TimeZone.getTimeZone("UTC"))
			.build();
}
```

- DEFLATE 與 Deflate64 共用 `DeflateInputStream` 和 `DeflateOutputStream`。解碼支援未壓縮區塊、固定 Huffman 區塊和動態 Huffman 區塊；Deflate64 編碼目前輸出未壓縮區塊或固定 Huffman 區塊，壓縮率不保證與產生動態表的編碼器一致。
- BZip2 使用模組自身的 `BZip2InputStream` 和 `BZip2OutputStream`。ZIP 項目 CRC-32 與 BZip2 的區塊/成員 CRC 是分別執行的完整性檢查。
- 目錄項目按 STORE 寫入，且不進行項目資料加密。
- 壓縮方法與等級透過 `ZipOptions` 設定，不是從 `ArchiveEntry` 讀取。同一個工作階段中逐項選擇不同演算法的專用設定進入點尚未提供。
- `LZMA1` 使用模組自身的 LZMA1 編解碼器及 ZIP 方法 14 封裝。項目寫入包含串流結束標記，並設定對應的 ZIP 旗標。
- `LZMA2` 對應 ZIP 方法 95，透過完整 XZ 容器封裝原始 LZMA2 資料。XZ 解碼支援多個 Block，每個 Block 僅含一個 LZMA2 篩選器，支援 NONE、CRC32、CRC64、SHA256 資料檢查類型；不支援 BCJ、Delta 或篩選器鏈。ZIP XZ 寫入預設採用 CRC64。
- LZMA1 和 LZMA2/XZ 的等級 `0` 仍會壓縮資料。這些等級用於調整本實作的字典或匹配搜尋行為，不等同於外部 LZMA/XZ 工具的預設等級。
- Zstandard 尚未實作。獨立 LZMA2/XZ 串流的支援不代表已經實作 TAR.XZ 或 7z 容器。
- 優先考慮 ZIP 通用相容性時使用 DEFLATE。選擇 Deflate64、BZip2、LZMA、XZ 或受保護中央目錄前，應確認接收工具支援對應擴充。

## 密碼加密

### 提供密碼

密碼透過 `ArchiveOptions.passwordProvider(...)` 提供，而不是寫在
`ZipOptions` 中：

```java
static ArchiveOptions passwordOptions(char[] password) {
	return ArchiveOptions.newBuilder()
			.passwordProvider(PasswordProvider.fixed(password, true))
			.build();
}
```

Provider 每次請求應返回新的 `PasswordCredentials`
。模組會關閉並銷燬使用完的認證資訊，不應反覆返回同一個已被銷燬的認證資訊物件。

需要按密碼用途選擇不同密碼時，可以讀取 `PasswordRequest` 的
`purpose`、封存路徑、項目路徑與嘗試次數：

```java
static ArchiveOptions separatePasswords(char[] entryPassword, char[] directoryPassword) {
	return ArchiveOptions.newBuilder().passwordProvider(request -> {
		boolean directory = request.getPurpose() == PasswordPurpose.READ_HEADER
				|| request.getPurpose() == PasswordPurpose.WRITE_HEADER;
		return Optional.of(PasswordCredentials.of(directory ? directoryPassword : entryPassword, true));
	}).build();
}
```

沒有可用密碼時返回 `Optional.empty()`。目前不能僅憑 `attempt`
欄位推斷模組會自動彈窗或迴圈重試；互動和重試應由業務組織。

`PasswordProvider.fixed(password)` 預設使用舊式低位元組編碼；第二個參數傳入 `true` 可選擇 UTF-8，如上例所示。自訂 Provider 應在 `PasswordCredentials.of(password, utf8Encoding)` 中設定同一旗標。讀取時需符合壓縮檔產生時使用的編碼。檔案名字元集與密碼編碼相互獨立。

### ZipCrypto 與 WinZip AES

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

AES 金鑰長度參數是 128、192、256。`aesCrypto(256)` 預設選擇
AE-2，`aesCrypto(256, AesVendor.AE_1)` 明確選擇 AE-1。

| AES 版本 | CRC 與完整性驗證                                 |
|----------|--------------------------------------------------|
| AE-1     | 儲存實際 CRC，並結合 AES 驗證資料驗證            |
| AE-2     | ZIP 標頭 CRC 為零，使用 AES 驗證資料驗證密文完整性 |

讀取時根據檔案標頭識別項目加密方式。解密普通 AES ZIP 時通常只需要密碼
Provider，不需要把寫入時的 `aesCrypto(...)` 再配置一遍。強加密與
WinZip AES 是不同的 ZIP 加密格式。

## PKWARE 強加密

透過 `strongCrypto(algorithm, keyBitLength)` 配置密碼強加密：

```java
static void createStrongZip(Path target, Path source, char[] password) throws Exception {
	ZipOptions zipOptions = ZipOptions.newBuilder()
			.strongCrypto(ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256, 256)
			.build();
	writeArchive(target, passwordOptions(password), zipOptions,
			archive -> archive.addFile("strong/payload.bin", source.toString(), ""));
}
```

目前 `StrongKey.validate(...)` 與對應加密器接受的主要參數為：

| 演算法     | `ZipGlobals` 常數後綴             | 金鑰位元數                           |
|----------|-----------------------------------|------------------------------------|
| DES      | `DES`                             | 56 或 64；公開 ZIP 選項會規範為 64 |
| 3DES     | `3DES_112` / `3DES_168`           | 112 / 168                          |
| AES      | `AES_128` / `AES_192` / `AES_256` | 128 / 192 / 256                    |
| RC2 NEW  | `RC2_NEW`                         | 40、64、128                        |
| RC4      | `RC4`                             | 40、64、128                        |
| Blowfish | `BLOWFISH`                        | 32..320，且為 8 的倍數             |
| Twofish  | `TWOFISH`                         | 128、192、256                      |

常數前綴統一為 `ZIP64_HEADER_ENC_ALGORITHM_`。RC2 OLD
雖然有格式常數和識別分支，但目前強加密寫入不支援。區塊長度與金鑰位元數是兩個概念，不能用演算法區塊大小推導
`keyBitLength`。

這些能力表示目前程式碼的格式實作與演算法範圍，不代表所有演算法都能被同一個第三方解壓縮工具讀取。內部寫入/讀取測試也不能替代外部格式互操作測試。

## 憑證加密與解密

### 存取模式

| `StrongAccessMode` | 建立時需要              | 解密時需要                 |
|--------------------|-------------------------|----------------------------|
| PASSWORD           | 密碼                    | 密碼                       |
| CERTIFICATE        | 接收者 RSA 公開金鑰憑證清單 | 任一相符接收者憑證及其私密金鑰 |
| BOTH               | 密碼和接收者憑證清單    | 密碼或相符憑證私密金鑰之一     |

**BOTH 是密碼或憑證，不是必須同時輸入兩者。** 建立 BOTH
時需要同時提供兩種解鎖途徑，解密時可選擇其中一種。

目前憑證方案基於 X.509 RSA
公開金鑰封裝工作階段金鑰。內容加密演算法仍可選擇上述支援的強加密演算法，憑證用途並不限於
AES。目前沒有 ECDSA/EC 憑證接收者的對應實作。

### 讀取接收者公開金鑰憑證並建立壓縮檔

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

建立憑證加密壓縮檔只需要接收者公開金鑰憑證，不需要接收者私密金鑰，也不需要在
`ArchiveOptions` 中設定解密用的 `CertificateProvider`
。可以傳入多個接收者，目前接收者數量上限常數為 1024。

### 使用憑證與私密金鑰介面解密

```java
static ArchiveOptions certificateOptions(X509Certificate certificate, PrivateKey privateKey) throws Exception {
	CertificateCredentials credentials = new CertificateCredentials(certificate, privateKey, null);
	return ArchiveOptions.newBuilder()
			.certificateProvider(CertificateProvider.fixed(credentials))
			.build();
}
```

把返回的 `ArchiveOptions` 傳給前面的 `readEntry()`、
`extractOne()` 或 `extractAll()`。`CertificateCredentials`
第三個參數允許傳入實際執行私密金鑰操作的 JCA Provider；不指定時傳
`null`。

自訂 `CertificateProvider`
時，可以根據請求中的公開金鑰雜湊和封存/項目上下文尋找認證資訊。沒有相符認證資訊時返回
`Optional.empty()`；憑證和私密金鑰應成對相符。

### 從 PKCS#12 載入私密金鑰

```java
static ArchiveOptions pkcs12Options(Path keyStore, char[] storePassword,
                                    char[] keyPassword) throws Exception {
	PKCS12CertificateProvider provider = PKCS12CertificateProvider.load(
			keyStore, storePassword, keyPassword);
	return ArchiveOptions.newBuilder().certificateProvider(provider).build();
}
```

儲存密碼和私密金鑰密碼分開傳遞。載入器遍歷 PKCS#12 中的金鑰項，選擇包含
RSA X.509 憑證和私密金鑰的項目；沒有可用項會擲出例外。公開金鑰憑證檔案本身不能用於解密。

### BOTH 與雙 Provider

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

讀取 BOTH 時優先收集憑證候選金鑰，沒有候選時才使用密碼。
`Optional.empty()` 可以允許回退；Provider
擲出例外或既有憑證候選在後續驗證中失敗，不應理解為一定會自動轉用密碼。

## 中央目錄保護

項目資料加密與中央目錄保護可以獨立配置。僅加密項目資料不會自動隱藏中央目錄中的檔案名。

### 只壓縮或只計算雜湊

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

中央目錄壓縮沿用 ZIP 選項的壓縮方法與等級，支援 STORE、DEFLATE、DEFLATE64、BZIP2、LZMA（方法 14）和 XZ（方法 95），分別透過 `CompressionAlgorithm.LZMA1` 和 `CompressionAlgorithm.LZMA2` 選擇 LZMA 與 XZ。與項目壓縮一致，BZIP2 等級 `0` 對映為區塊大小 `1`。編輯和分卷合併可以讀取並重新產生這些方法壓縮的中央目錄。雜湊針對變換前的中央目錄位元組計算；它與 AES 項目驗證碼是不同機制，也不等同於數位簽章。

支援的雜湊常數後綴包括 `NONE`、`CRC32`、`MD5`、`SHA1`、
`RIPEMD160`、`SHA256`、`SHA384`、`SHA512`，前綴為
`ZIP64_HEADER_HASH_ALGORITHM_`。

### 項目與中央目錄均使用密碼強加密

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

呼叫
`writeArchive(target, passwordOptions(password), protectedDirectoryOptions(), action)`
即可使用。寫入中央目錄時按原始目錄位元組計算雜湊，再壓縮、加密，最終寫入
ZIP64 Protection 中繼資料；讀取時執行逆過程並核對大小、尾部與雜湊。

### 項目僅允許憑證，中央目錄僅允許密碼

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

建立時提供中央目錄密碼和接收者公開金鑰憑證；讀取整個壓縮檔時需要同時配置密碼
Provider 與憑證 Provider，因為它們分別負責兩層資料。每一層的存取模式都是獨立的。

`centralDirectoryCrypto(...)` 也有帶 `StrongAccessMode`
和接收者清單的多載，可實作相反組合或
BOTH。開啟受加密中央目錄保護的包時就需要目錄解密認證資訊，不能等到讀取某個項目時才提供。

## 分卷壓縮檔

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

| 型別          | 建立時的配置          | 輸出檔案                                      | 讀取進入點         |
|---------------|-----------------------|-----------------------------------------------|------------------|
| 原生 ZIP 分卷 | `SplitType.NATIVE`    | `backup.z01`、`backup.z02`、最後 `backup.zip` | `backup.zip`     |
| 連續切片      | `SplitType.NUMBERED` | `backup.zip.001`、`backup.zip.002`…           | `backup.zip.001` |

- 原生 ZIP 分卷大小範圍為 `65,536..0xFFFFFFFF` 位元組。
- 原生分卷的單個 Local/Central File Header
  必須能完整放入某一卷；卷容量小於單一標頭長度時會失敗。
- 編號切片可在任意位元組位置切分；項目標頭可以跨切片。
- `splitArchive(true, ...)`
  讀取時也應提供足夠大的分卷長度上限，至少容納實際各卷；通常複用建立時的配置。
- 讀取時 `SplitType.DETECT` 可以根據擴充名識別兩類分卷；建立時應明確使用
  NATIVE 或 NUMBERED。
- 原生模式若只產生一卷，最終檔案名仍為 `backup.zip`；編號模式輸出
  `backup.zip.001`。
- 分卷佈局與項目加密是獨立選項，編號切片不會自動禁用強加密。

例如原生 64 MiB 分卷可使用
`splitOptions(SplitType.NATIVE, 64L * 1024 * 1024)`；編號切片改為
`SplitType.NUMBERED`。

### 合併為單個 ZIP

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

來源必須是分卷壓縮檔。可以在唯讀工作階段呼叫，來源分卷保持保留；目標應使用獨立路徑，避免與任何來源卷重合。該操作重寫卷號、位移及中央目錄結束記錄，不是簡單拼接
`.z01` 檔案。

合併按既有項目的儲存資料複製，並重建最終目錄。受保護中央目錄在重建時需要對應認證資訊，項目
CRC、加密中繼資料與 Local Header
遮蔽語義也需要保持。合併本身不代替逐條讀取來核驗全部項目內容。

## ZIP64 與大檔案

```java
static ZipOptions zip64Options(Zip64Mode mode) throws Exception {
	return ZipOptions.newBuilder().zip64Mode(mode).build();
}
```

| 模式   | 行為                                                      |
|--------|-----------------------------------------------------------|
| AUTO   | 根據實際項目大小、位移、數量及保護結構生成所需 ZIP64 資訊 |
| ALWAYS | 強制生成相應 ZIP64 資訊和 ZIP64 結束結構                  |
| NEVER  | 需要 ZIP64 表示時拒絕操作                                 |

支援超過 4 GiB 的項目或壓縮檔，不代表沒有大小上限。還受
`ArchiveLimit`、Java `long`/集合容量、暫存目錄可用空間和目標檔案系統限制。

預設單一項目上限為 8
GiB。高度可壓縮的大檔案也可能觸發壓縮比限制，應按業務資料設定資源限制。增加上限的範例見“資源限制與驗證”。

## 修改、刪除與覆蓋

使用 `"rw"` 開啟已經存在的 ZIP 即進入
EDIT。用同一路徑寫入新項目可替換原項目，刪除透過
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

現有項目通常透過複製儲存資料保留，新項目使用目前工作階段的 ZIP
選項生成。編輯不是給既有全部項目批次更換密碼或重新壓縮的進入點；需要這種轉換時，應讀取原內容後重新建立目標包。

分卷輸入在提交時也會按配置重新組織輸出。工作階段會先寫工作目錄，再發佈目標並處理舊卷；涉及多檔案發佈時不能把它等同於作業系統級跨檔案原子交易。失敗時可能為還原保留工作目錄。

### 重複名稱

```java
static ZipOptions duplicateOptions(DuplicatePolicy policy) throws Exception {
	return ZipOptions.newBuilder().duplicatePolicy(policy).build();
}
```

- FIRST：同一路徑選擇第一個中央目錄項目。
- LAST：選擇最後一個，預設策略。
- REJECT：檢測到重複時擲出例外。

此策略影響原始目錄中同名項目的選擇，不是允許多個呼叫端同時向同一個
Writer 寫入的配置。目錄模型保留原始標頭清單，便於需要保留重複項目的合併操作。

## 檔案屬性、目錄與連結

### 中繼資料

`ArchiveEntry.fromPath(path, entryPath)`
讀取來源屬性時不跟隨符號連結。支援的主要欄位包括：

| 欄位              | 含義                                            |
|-------------------|-------------------------------------------------|
| `lastModified`    | 最後修改時間；同時生成 DOS 時間與適用的額外欄位 |
| `lastAccess`      | 最後存取時間                                    |
| `createTimestamp` | 建立時間                                        |
| `unixMode`        | Unix 型別與權限位元                               |
| `dosAttributes`   | DOS 檔案屬性位元                                  |
| `linkTarget`      | 連結目標描述                                    |

寫入採用 NTFS 時間額外欄位儲存更高精度時間，在範圍允許時附帶
UT 修改時間；DOS 時間使用 `ZipOptions.timezone(...)`
指定的時區。讀取時結合中央目錄和可用時間額外欄位還原屬性。

手動建立帶中繼資料的項目：

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

把結果傳給 `writer.openStream(entry)`。中繼資料的 `entryPath`
必須與開啟 Writer 時的路徑一致，Unix 檔案型別也必須與
`EntryType` 相符。

解壓縮使用 `preserveLastModified(true)`
控制目前實作中的時間屬性還原，不僅是修改時間；可用的存取/建立時間也會一併傳給檔案系統。
`preservePermissions(true)` 控制權限與 DOS 屬性還原。

目錄屬性在子項目擷取後還原，避免後續寫入改變其時間。實際還原能力受目標檔案系統限制；目前會跳過符號連結自身的屬性還原。沒有完整
ACL、所有者、擴充屬性等任意檔案系統中繼資料的無損保證。

### 明確寫入空目錄

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
				// 目錄項目不寫入資料。
			}
			writer.finish();
		}
	});
}
```

### 符號連結與硬連結

符號連結可以透過 `writer.write(sourceLinkPath)`
儲存，項目資料儲存連結目標文字。也可以用 SYMBOLIC_LINK
中繼資料開啟串流，再把目標文字按封存字元集寫入；僅設定
`linkTarget` 並不會自動替代手動串流寫入。

預設解壓縮不允許建立連結。需要還原時明確啟用：

```java
static ExtractOptions linkExtractionOptions() throws Exception {
	return ExtractOptions.newBuilder()
			.allowSymbolicLinks(true)
			.allowHardLinks(true)
			.unsafePolicy(UnsafePolicy.REJECT)
			.build();
}
```

實作會檢查連結目標和路徑元件，處理已識別硬連結的前向依賴、並拒絕依賴環或越界目標。Windows
建立符號連結可能需要相應權限；硬連結也受作業系統和檔案系統限制。

目前 Writer 接受 FILE、DIRECTORY、SYMBOLIC_LINK；不提供原生
HARD_LINK 中繼資料寫入。讀取/擷取硬連結與寫入硬連結是不同能力。

## 資源限制與驗證

### 預設限制

| 限制                  | 預設值    |
|-----------------------|-----------|
| `maxEntries`          | 1,000,000 |
| `maxEntryBytes`       | 8 GiB     |
| `maxTotalBytes`       | 64 GiB    |
| `maxCompressedBytes`  | 64 GiB    |
| `maxHeaderBytes`      | 256 MiB   |
| `maxCompressionRatio` | 1000      |
| `maxNestingDepth`     | 8         |
| `maxNameCharacters`   | 4096      |

`maxNestingDepth` 目前檢查項目路徑的目錄深度，不是自動遞迴開啟
ZIP 中巢狀 ZIP 的次數。模組不會自動解壓縮內嵌壓縮檔。

### 自訂資源限制與字元集

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

對使用 UTF-8 旗標的項目，讀取時按 UTF-8 解碼；其餘名稱和註解使用配置字元集。非
ASCII 密碼另由 `PasswordCredentials.of(password, utf8Encoding)` 控制。

### 路徑安全與錯誤策略

- 整個壓縮檔擷取檢查目標規範化後的路徑是否仍位於解壓縮根目錄內。
- 檢查既有父路徑元件，處理符號連結引起的目錄逸出問題。
- `UnsafePolicy.REJECT` 拒絕不安全項目；SKIP 在相應擷取檢查中跳過該項目。
- `strict(true)` 同時影響解析階段的結構、路徑和 Local/Central
  Header 一致性檢查。解析階段已經拒絕的非法項目不會等到擷取時再由
  SKIP 處理。
- `continueOnError(true)` 用於允許部分擷取錯誤後繼續，不會把全部結構錯誤或安全拒絕轉成成功。
- `ResourceReader.extract(target, ...)`
  是指定最終路徑的單一項目操作；不可信封存優先使用具有明確根目錄語義的整個壓縮檔擷取。

### 驗證行為

- 普通 ZIP 項目按配置驗證 CRC、大小與資源限制。
- AE-1 保留 CRC，AE-2 依賴 AES 驗證資料；驗證失敗會擲出例外。
- 強加密檢查認證資訊、解密驗證區塊與對應演算法的資料結束條件。
- 中央目錄根據 Protection 驗證結構、大小及配置的雜湊。
- `verify(false)` 不等於關閉所有格式、驗證或資源檢查。
- 串流關閉可能完成剩餘讀取與驗證，應等待關閉成功後再把業務結果認定為成功。

例外可以是 `ArchiveException`、底層 `IOException`
、選項建置或密碼/憑證相關例外。
`ArchiveException.getErrorCode()`
可取得模組錯誤碼；顯示文字依賴專案的國際化資源。避免只依賴例外訊息字元串判斷錯誤。

## 暫存檔案、並行與資源生命週期

### 寫入與發佈

每個項目單獨寫入工作階段暫存檔案，資料經過串流壓縮/加密，不要求把項目完整放入記憶體。最終
ZIP 由提交階段生成，因此這不是直接向 HTTP 響應連續輸出完整壓縮檔的高階介面。

暫存工作目錄位於目標上層目錄，名稱形如 `.nervousync-zip-*`
。必須保證來源、目標及暫存資料的磁碟空間；修改既有壓縮檔時還可能需要輸出與備份空間。成功提交或正常回復會清理工作目錄，發佈失敗時可能保留還原資料。

### 並行約束

獨立項目暫存提供了平行準備的基礎，但目前沒有面向呼叫端的執行緒池或完整執行緒安全工作階段承諾。
`openWriter()` 的查詢/註冊以及工作階段狀態、提交、回復需要協調。

常規用法應循序操作工作階段。需要平行準備時，由控制執行緒建立不同路徑的
Writer，工作執行緒分別寫入獨立項目；等所有任務結束後，再由控制執行緒完成註冊和工作階段關閉。不要同時寫同一個
Writer，也不要讓提交、關閉或回復與工作執行緒重疊。失敗時應先等工作執行緒退出，再處理工作階段暫存目錄。

### 關閉規則

| 物件                          | 呼叫端責任                                         |
|-------------------------------|----------------------------------------------------|
| `ArchiveFile`                 | 使用 try-with-resources；關閉可能提交併擲出例外    |
| `ResourceWriter`              | 關閉項目輸出串流，成功後 `finish()`，最終關閉 Writer |
| `ResourceReader`              | 同時管理輸入串流與 Reader 的關閉                     |
| 外部 InputStream/OutputStream | 範例中由呼叫端建立的外部串流仍由呼叫端管理           |
| `ZipFileOperator`             | 關閉會關閉其持有的封存輸入串流                       |

`writer.abort()` 標記項目取消，不等於已經把寫工作階段還原成可提交狀態。取消整個業務操作時使用
`ArchiveFile.rollback()`。`close()` 也不等於
`ResourceWriter.finish()`，未完成的 Writer 會阻止正常提交。

## 底層介面與格式模型

對需要檢查結構、除錯相容性或擴充封存實作的呼叫端，模組還提供：

| 型別                                              | 用途                                                    |
|---------------------------------------------------|---------------------------------------------------------|
| `ZipFileOperator`                                 | 讀取 EOCD、ZIP64 EOCD/Locator、中央目錄和 Local Header  |
| `ZipArchiveInputStream`                           | 可定位、可跨卷的封存位元組輸入；不是自動逐條解壓縮的 ZIP 流 |
| `ZipArchiveOutputStream`                          | 卷切換、標頭完整性邊界、目錄統計、最終卷發佈對映          |
| `LocalFileHeader` / `GeneralFileHeader`           | 本地頭與中央目錄檔案標頭模型及序列化                      |
| `EndCentralDirectoryRecord`                       | 普通 EOCD                                               |
| `Zip64EndCentralDirectory`                        | ZIP64 Record、Locator、Protection                       |
| `ExtraDataRecord`                                 | ZIP 額外欄位                                            |
| `AesEncryptionInfo` / `StrongEncryptionInfo`      | 兩種加密格式的項目中繼資料                                |
| `StrongDecryptionHeader` / `StrongRecipientTable` | 強加密解密標頭與接收者資訊                                |
| `StandardInputStream` / `StandardOutputStream`    | ZipCrypto 資料串流                                        |
| `AesInputStream` / `AesOutputStream`              | WinZip AES 資料串流                                       |
| `StrongInputStream` / `StrongOutputStream`        | PKWARE 強加密資料串流                                     |
| `DeflateInputStream` / `DeflateOutputStream` | 原始 DEFLATE/Deflate64 編解碼，不包含 ZIP、zlib 或 GZIP 格式封裝 |
| `BZip2InputStream` / `BZip2OutputStream` | 完整 BZh 資料流，可獨立於 ZIP 使用 |
| `LZMAInputStream` / `LZMAOutputStream` | 預設採用 ZIP 方法 14 的 LZMA1 封裝；`raw(...)` 和 `alone(...)` 工廠方法分別支援原始 LZMA1 與 LZMA-Alone（`.lzma`）資料流 |
| `LZMA2InputStream` / `LZMA2OutputStream` | 原始 LZMA2 資料流，不包含 XZ、ZIP 或 7z 容器封裝 |
| `XZInputStream` / `XZOutputStream` | 使用 LZMA2 篩選器的完整 XZ 資料流，可獨立於 ZIP 使用 |

獨立 BZip2 解碼器預設只讀取一個成員，可透過雙參數建構方法啟用串接成員。解碼支援舊版隨機化區塊，編碼器輸出非隨機化區塊。完整驗證 BZip2 區塊/成員 CRC 需要讀取至 EOF；關閉獨立解碼器不會主動讀取剩餘資料。ZIP 項目和中央目錄採用單成員解碼；受保護中央目錄讀取器還會拒絕尾隨資料。

獨立 XZ 解碼器預設讀取串接 Stream；向雙參數建構方法傳入 `false` 可在一個 Stream 結束後停止，ZIP 項目和中央目錄讀取採用此模式。需要讀取至 EOF 才能完整驗證 XZ 資料檢查、Index 和 Footer。預設允許的宣告字典大小上限為 64 MiB，此限制不限制解壓縮後的輸出大小。XZ 編碼器目前對非空輸入寫入一個 LZMA2 Block，固定使用 64 KiB 字典；空輸入產生不含 Block 的 Stream。獨立編碼器可透過 `XZCheck` 選擇檢查類型，預設採用 CRC64。

Deflate、BZip2、LZMA1、LZMA2 和 XZ 編碼輸出流的 `finish()` 都會完成壓縮而保留底層串流，`close()` 還會關閉底層串流。`DeflateOutputStream.release()` 只釋放編碼資源，不會完成輸出。獨立使用編解碼串流時，呼叫端需自行限制輸出大小和壓縮比。

自訂 `DataFile` 時，除了普通讀寫，還需要正確實作 `seek()`、
`getFilePointer()`、`length()`、`newFile()` 和 `splitFiles()`
。分卷程式碼透過 `newFile()` 開啟同配置下的其他卷，透過
`splitFiles()` 尋找卷檔案；獨立 Reader
也會建立新的可定位控制代碼。僅有順序讀取能力的網路流不能直接替代該介面。

普通 ZIP 的主體為項目 Local Header、項目資料及可選 Data
Descriptor，隨後是中央目錄和結束記錄；需要時附帶 ZIP64
結構和目錄保護資訊。目錄模型還能讀取 Archive Extra Data
Record 與 Digital Signature 資料。

保留 Digital Signature
位元組不代表實作了數字簽名驗籤或憑證信任鏈驗證。直接操作底層模型時，需要自行維護位移、卷號、大小、ZIP64
佔位、驗證開銷與串流擁有權。常規應用優先使用 `ArchiveFile`
，避免自行拼裝結構。

## 舊介面相容

模組保留 `org.nervousync.zip.ZipFile` 和
`org.nervousync.zip.options.ZipOptions`
，提供舊式建立、新增、讀取、擷取、刪除、密碼設定、註解及分卷合併等方法。

- 舊的 `ZipFile.openZipFile(...)` 與新的
  `ArchiveFile.zipFile(...)` 是不同進入點。
- 上述壓縮支援適用於 `org.nervousync.archive`；保留的舊 ZIP 呼叫鏈沒有因本次重構而獲得 Deflate64、BZip2、LZMA 或 XZ 支援。
- 新說明文件範例使用的是
  `org.nervousync.archive.zip.ZipOptions`。
- 舊包中的標頭模型和選項不能直接傳入新介面。
- 憑證、雙 Provider 與中央目錄 Protection
  的用法以新介面為準，不應推斷舊介面具有同等能力。

維護舊應用時保留對應舊呼叫鏈；新接入可以從本文的工作階段介面開始。

## 測試與目前限制

在儲存庫根目錄執行模組及其上游模組測試：

```shell
mvn -pl archive -am test
```

只執行一組 ZIP 測試：

```shell
mvn -pl archive -am -Dtest=ZipFileTest -Dsurefire.failIfNoSpecifiedTests=false test
```

需要執行受開關控制的大 ZIP 測試案例時：

```shell
mvn -pl archive -am -DrunLargeZipTest=true test
```

壓縮相關測試包括 `DeflateStreamTest`、`Deflate64ZipTest`、`Deflate64CentralDirectoryTest`、`BZip2StreamTest`、`ZipBZip2IntegrationTest`、`ZipBZip2FailureTest` 和 `ZipBZip2CentralDirectoryTest`。LZMA/LZMA2/XZ 測試包括 `LZMAStreamTest`, `LZMACodecTest`, `LZMAZipTest`, `LZMACentralDirectoryTest`, `LZMA2StreamTest`, `LZMA2CodecTest`, `XZStreamTest`, `XZCodecTest`, `XZZipTest`, `XZCentralDirectoryTest`。這些測試涵蓋獨立解碼器/樣本互通、壓縮等級、非法及截斷資料、加密、分卷、編輯、合併和受保護中央目錄。樣本來源見 [`src/test/resources/deflate/README.md`](src/test/resources/deflate/README.md), [`src/test/resources/bzip2/README.md`](src/test/resources/bzip2/README.md), [`src/test/resources/lzma/README.md`](src/test/resources/lzma/README.md), [`src/test/resources/lzma2/README.md`](src/test/resources/lzma2/README.md), [`src/test/resources/xz/README.md`](src/test/resources/xz/README.md)。

在儲存庫根目錄執行壓縮相關測試：

```shell
mvn -pl archive -am "-Dtest=DeflateStreamTest,Deflate64ZipTest,Deflate64CentralDirectoryTest,BZip2StreamTest,ZipBZip2IntegrationTest,ZipBZip2FailureTest,ZipBZip2CentralDirectoryTest" -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl archive -am "-Dtest=LZMAStreamTest,LZMACodecTest,LZMAZipTest,LZMACentralDirectoryTest,LZMA2StreamTest,LZMA2CodecTest,XZStreamTest,XZCodecTest,XZZipTest,XZCentralDirectoryTest" -Dsurefire.failIfNoSpecifiedTests=false test
```

測試程式碼位於 `src/test/java/org/nervousync/test/archive`
，覆蓋普通讀寫、ZIP64、分卷、修改、合併、路徑與連結、檔案屬性、密碼/憑證認證資訊矩陣、AES/強加密及中央目錄保護。部分連結測試案例可能因本機檔案系統或權限不足而中止，應結合
CI 的實際執行結果判斷覆蓋情況。

目前使用時還需瞭解：

1. 目前可用容器主要為 ZIP；TAR 配接器未完成，未提供 7z 容器實作。
2. ZIP 項目和受保護中央目錄支援 STORE/DEFLATE/DEFLATE64/BZIP2/LZMA/XZ；`LZMA1` 對應方法 14，`LZMA2` 對應使用 XZ 封裝的方法 95。XZ 每個 Block 僅支援單個 LZMA2 篩選器；BCJ、Delta、篩選器鏈及 Zstandard 尚未實作。Deflate64 寫入目前採用未壓縮區塊或固定 Huffman 區塊。
3. `WritableOptions`、部分舊式目錄選擇欄位和 `StreamOwner`
   已存在，但尚未完整接入新 `ArchiveFile` 工作流。
   `streamOwner(CALLER)` 不能作為傳入 `DataFile` 一定不被關閉的保證。
4. `readHiddenFiles`、`includeRootFolder`、`rootFolderInZip`、
   `fileNameInZip` 等欄位沒有在新寫入路徑中形成統一目錄選擇流程；新介面透過
   `addDirectory` 參數或 `openWriter`
   的封存檔內路徑控制名稱，隱藏檔案篩選由呼叫端明確組織。
5. 建立硬連結、完整
   ACL/所有者/任意擴充屬性還原、自動憑證吊銷檢查和憑證信任鏈策略不屬於目前高階實作。
6. 工作階段發佈包含暫存輸出和備份機制，但不提供斷電級或跨多個分卷的原子交易保證。
7. `BOTH` 的憑證優先和密碼回退有前述條件；應用程式若需要其他認證資訊選擇策略，應明確組織
   Provider 和重試流程。
8. 自身往返測試不能替代第三方互操作樣本。啟用憑證、強加密、受保護中央目錄等功能前，應驗證目標使用工具支援對應
   ZIP 擴充。
