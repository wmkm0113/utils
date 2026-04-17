# Nervousync Utilities

企業級 Java 實用程式基礎庫

[![Maven Central](https://img.shields.io/maven-central/v/org.nervousync/utils-bom?color=green&label=Release)](https://mvnrepository.com/artifact/org.nervousync/utils-bom)
![Maven Snapshot](https://img.shields.io/maven-metadata/v?label=Snapshot&metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Forg%2Fnervousync%2Futils-bom%2Fmaven-metadata.xml)
[![License](https://img.shields.io/github/license/wmkm0113/utils-jdk11)](https://github.com/wmkm0113/utils/blob/mainline/LICENSE)
![Language](https://img.shields.io/badge/language-Java-green)
[![Twitter:wmkm0113](https://img.shields.io/twitter/follow/wmkm0113?label=Follow)](https://twitter.com/wmkm0113)

[English](README.md)
[简体中文](README_zh_CN.md)
繁體中文

---

## 目錄
* [專案概覽](#專案概覽)
* [JDK版本相容](#JDK版本相容)
* [設計理念](#設計理念)
* [模組](#模組)
* [安裝引用](#安裝引用)
* [快速開始](#快速開始)
+ [JavaBean 與不同資料的相互轉換](#JavaBean-與不同資料的相互轉換)
+ [隨機ID生成](#隨機ID生成)
+ [地理資訊](#地理資訊)
+ [統一管理的設定文件](#統一管理的設定文件)
+ [國際化的支持](#國際化的支持)
+ [OTP產生與驗證](#OTP產生與驗證)
* [架構概覽](#架構概覽)
* [同類產品比較](#同類產品比較)
* [適用場景](#適用場景)
* [版本控制](#版本控制)
* [貢獻與回饋](#貢獻與回饋)
* [授權協議](#授權協議)
* [捐款](#捐款)
* [結語](#結語)

## 專案概覽

**Nervousync Utils** 是一個以企業為基礎的 Java 工具庫，旨在為後端系統提供可重複使用的基礎架構功能。

與輕量級輔助庫不同，此專案專注於**功能層面的抽象**，例如組態管理、國際化、安全工具、HTTP 通訊、生命週期管理和結構化資料轉換。

它與特定框架無關，可以輕鬆方便的整合到以下系統中：

- 獨立的 Java 應用程式
- 微服務
- 企業平台架構
- 模組化後端系統

---

## JDK版本相容
**編譯：** OpenJDK 11
**測試：** OpenJDK 11 / 17 / 21
**運行：** OpenJDK 11+ 或相容版本
**Jakarta EE平台：** 10

---

## 設計理念

本專案基於以下工程原則建構：

- 模組化設計

專案提供了不同重點的功能模組，方便使用者按需選擇，實現外部依賴的最小化，降低整體專案負擔和安全風險。

- 提供完善的進階安全特性

專案提供了多種最新的加密演算法支持，同時也提供了完整的 OTP（支援 TOTP 和 HOTP） 支持

- 與框架無關

不強制依賴某些重量級框架

---

## 模組

| 模組             | 概述                                               |
|----------------|--------------------------------------------------|
| utils-bom      | BOM模組                                            |
| utils-core     | 核心模組、基礎工具                                        |
| utils-beans    | JavaBean 與不同資料格式（XML/JSON/YAML）的轉換工具             |
| utils-config   | 統一的設定檔管理、啟動項目的管理                                 |
| utils-i18n     | 國際化引擎（支援多語言、國際化、在地化）                             |
| utils-log4j    | 使用Log4j2實現的日誌配置                                  |
| utils-mail     | 電子郵件工具                                           |
| utils-net      | 網路存取工具（包括網路請求、網路檔案、SNMP監控）                       |
| utils-office   | Excel 檔案操作支援                                     |
| utils-security | 安全工具（包括加密、解密、簽署、驗證、金鑰管理）、OTP工具、安全工廠（用於設定檔的自動加解密） |
| utils-zip      | Zip壓縮檔的存取支援                                      |
| utils-all      | 包含所有模組                                           |

每個模組均可獨立使用。

---
## 安裝引用
**Maven：**
### 直接引用
```
<dependency>
 <groupId>org.nervousync</groupId>
 <artifactId>${模組名稱}</artifactId>
 <version>${版本號}</version>
</dependency>
```
### 使用 BOM 管理
```
<!-- 匯入 BOM，使用 platform -->
<dependencyManagement>
 <dependencies>
 <dependency>
 <groupId>org.nervousync</groupId>
 <artifactId>utils-bom</artifactId>
 <version>${版本號}</version>
 <type>pom</type>
 <scope>import</scope>
 </dependency>
 </dependencies>
</dependencyManagement>
<!-- 依賴項不需要寫版本號 -->
<dependencies>
 <dependency>
 <groupId>org.nervousync</groupId>
 <artifactId>${模組名稱}</artifactId>
 <version>${版本號}</version>
 </dependency>
 …
</dependencies>
```
**Gradle：**
Gradle 5.0+ 支援將 Maven BOM 匯入為 platform 以對齊相依性版本。
### 直接引用
```
implementation 'org.nervousync:${模組名}:${版本號}'
```
### 使用 BOM 管理
```
// 匯入 BOM，使用 platform
implementation platform('org.nervousync:utils-bom:${版本號}')
// 依賴項不需要寫版本號
implementation 'org.nervousync:${模組名}'
```
**SBT：**
```
libraryDependencies += "org.nervousync" % "${模組名稱}" % "${版本號碼}" % "provided"
```
**Ivy：**
```
<dependency org="org.nervousync" name="${模組名稱}" rev="${版本號碼}"/>
```

## 快速開始
### JavaBean 與不同資料的相互轉換

`utils-core（使用jsonb）`支援 XML 和 JSON 格式的數據，`utils-beans（使用Jackson）`支援 XML、JSON 和 YAML 格式的數據

首先定義一個簡單的 JavaBean，並加入對應的註解：
```java
// 使用 OutputConfig 註解來宣告資料的格式類型
@OutputConfig(type = StringType.JSON)
@XmlRootElement(name = "user")
public class User {
 @XmlElement
 private String name;
 @XmlElement
 private int age;
 // getters and setters
}
```
只需一行程式碼，即可將 JavaBean 實例物件轉換為對應的字串：
```java
String string = BeanUtils.objectToString(user);
```

需要將字串轉換為 JavaBean，也只需要一行程式碼：
```java
User user = BeanUtils.stringToObject(string, User.class);
```

### 隨機ID生成

在日常開發中，經常會有需要產生隨機ID的需求，現在您只需要：
```java
UUID uuid = IDUtils.UUIDv4();
ULID ulid = IDUtils.ULID();
CUID cuid = IDUtils.CUID();
long snowflake = IDUtils.snowflake();
String nano = IDUtils.nano();
```

### 地理資訊

轉換座標系：
```java
GeoPoint gpsPoint = LocationUtils.anyToGPS(currentPoint);
GeoPoint gcj02Point = LocationUtils.anyToGCJ02(currentPoint);
GeoPoint bd09Point = LocationUtils.anyToBD09(currentPoint);
```

計算兩個座標點的距離（單位：公尺）：
```java
double distance = LocationUtils.distance(beginPoint, endPoint);
```

### 統一管理的設定文件

此功能需要`utils-config`模組

在日常開發的過程中，經常會遇到各種不同的配置信息，本項目提供了一個統一接口，用於管理這些配置信息。

1. 首先需要定義設定檔的 JavaBean ：
    ```java
    // 新增 Signature 註解後，設定檔管理員會在儲存設定資訊時產生簽名，並在讀取資訊時驗證簽名，保證設定檔不會有未經授權的修改
    @Signature("signature")
    @XmlRootElement(name = "proxy_config", namespace = "https://nervousync.org/schemas/proxy")
    @XmlAccessorType(XmlAccessType.NONE)
    // 必須標註 OutputConfig，配置好資料類型和編碼集
    @OutputConfig(type = StringType.XML, encoding = "UTF-8")
    public final class ProxyConfig implements Serializable {
    
     @XmlElement(name = "username")
     private String userName;
     // 新增 Password 後，設定檔管理員在儲存和讀取設定檔時，自動對資訊進行加密/解密操作，防止敏感資訊洩露
     @Password
     @XmlElement(name = "password")
     private String password;
     @XmlElement
     private String signature;
     // getters and setters
    }
    ```

2. 儲存、讀取設定訊息
    ```java
    boolean result = ConfigureManager.getInstance().saveConfigure(proxyConfig);
    ProxyConfig proxyConfig = ConfigureManager.getInstance().readConfigure(ProxyConfig.class);
    ```

### 國際化的支持

此功能需要`utils-i18n`模組

1. 定義資源檔案訊息   
多語言的資源檔案儲存路徑為 jar 套件內的 META-INF/i18n/Resources.json，具體格式如下：
    ```json
    {
     "groupId": "org.nervousync",
     "bundle": "utils",
     "errors": {
     "0x000000150001": "Length_Not_Enough_Crypto_Error",
     },
     "messages": {
     "en-US": {
     "Load_Schema_Mapping_Error": {
     "pattern": "An error occurs when load schema mapping, file path: {0}"
     }
     },
     "zh-CN": {
     "Load_Schema_Mapping_Error": {
     "pattern": "載入資源描述檔對應表出錯，檔案位址：{0}"
     }
     }
     }
    }
    ```

2. 引用多語言資訊：
    ```java
    // 使用資源檔案中定義的 groupId 和 bundle 來取得對應的多語言資訊代理
    MessageAgent agent = MultilingualUtils.newAgent("org.nervousync", "utils");
    String message = agent.getMessage("Load_Schema_Mapping_Error", "/opt/schemas/file.xsd");
    ```

3. 多語言日誌
    ```java
    LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());
    logger.info("Load_Schema_Mapping_Error", "/opt/schemas/file.xsd");
    ```
資源檔案中支援使用 Plurals 表達式

### OTP產生與驗證

此功能需要`utils-security`模組

產生 OTP 金鑰：
```java
String secret = OTPUtils.generateRandomKey();
```

計算 TOTP 使用者與伺服器的時間偏移
```java
long fixedTime = OTPUtils.calculateFixedTime(secret, authCode);
```

驗證 TOTP 資料：
```java
boolean result = OTPUtils.validateTOTPCode(secret, authCode, fixedTime);
```

使用者可以選擇不同的演算法，預設為 HMAC-SHA1。支援基於時間的標準一次性密碼演算法（TOTP）和基於計數器的一次性密碼演算法（HOTP）。

---

## 架構概覽

```
 +------------------+
 | utils-core |
 +------------------+
 / | \
 / | \
 v v v
 utils-config utils-i18n utils-security
 \ | /
 \ | /
 v v v
 utils-net utils-launcher
```

---


## 同類產品比較

| Feature  | Nervousync | Apache Commons | Hutool  |
|----------|------------|----------------|---------|
| 統一配置資訊管理 | ✔          | ✘              | Partial |
| 內建的國際化引擎 | ✔          | ✘              | ✘       |
| 一次性密碼支援  | ✔          | ✘              | ✔       |
| 無重型框架依賴  | ✔          | ✔              | ✔       |

本專案著重於結構化的企業能力，而不是通用輔助工具。


## 適用場景

- 企業後端系統
- 模組化平台架構
- 需要統一配置管理的整合系統
- 具有國際化需求的系統
- 需要整合更多安全配置的服務

---

## 版本控制

本項目遵循語意化版本控制：

- 主版本號 (MAJOR) – JDK 版本升級變更
- 次版本號 (MINOR) – 內部 API 變更
- 發布版本號 (RELEASE) - 向後相容的功能新增
- 補丁版本號 (PATCH) – 錯誤修復和細微改進

---
## 貢獻與回饋

歡迎各位朋友將此文件及項目中的提示訊息、錯誤訊息等翻譯為更多語言，以幫助更多的使用者更了解與使用此工具包。
如果在使用過程中發現問題或需要改進、新增相關功能，請提交issue到本專案或發送電子郵件至[wmkm0113\@gmail.com](mailto:wmkm0113@gmail.com?subject=bugs_and_features)
為了更好地溝通，請在提交issue或發送電子郵件時，寫明如下訊息：
1.目的是：發現Bug/功能改進/增加新功能
2、請貼上以下資訊（如果存在）：傳入數據，預期結果，錯誤堆疊訊息
3、您認為可能是哪裡的程式碼出現問題（如提供可以幫助我們盡快找到並解決問題）
如果您提交的是添加新功能的相關信息，請確保需要添加的功能是一般性的通用需求，即添加的新功能可以幫助到大多數用戶。

同時歡迎各位朋友貢獻程式碼，請遵循以下流程：

1. Fork 此代碼倉庫
2. 建立特性分支
3. 提交拉取請求

貢獻程式碼的同時請確保：

- 程式碼遵循專案結構
- 公共 API 已編寫文檔
- 已考慮向後相容性

如果您需要添加的是客製化的特殊需求，我將收取一定的客製化開發費用，具體​​費用金額根據客製化的特殊需求的工作量進行評估。
客製化特殊需求請直接發送電子郵件到[wmkm0113\@gmail.com](mailto:wmkm0113@gmail.com?subject=payment_features)，同時請盡量在郵件中寫明您可以負擔的開發費用預算金額。

---

## 授權協議

本專案採用 Apache License 2.0 授權協議。
詳情請參閱 `LICENSE` 文件

---

## 捐款
為了支持此項目，您可以向以下地址捐款：

* 比特幣地址: bc1q3nfj9gafu3x25ea260g7cyhh5s9gnx347tznsf
* 以太坊地址: 0x849D143e943bAA6Dd078d02ebAEc205E2b00a7CA
* Solana 地址: 4Fvujk8DEkVAtYwzim1vrobNm4s72Ra6Xrsu83v2hqE2
* BNB 地址: 0x849D143e943bAA6Dd078d02ebAEc205E2b00a7CA
---

## 結語

本專案旨在為企業級 Java 系統提供穩定、可重複使用的基礎架構，如果您希望建立可維護的後端平台，並對配置、安全性、國際化等方面有需求，那麼本專案正是為此而設計的。