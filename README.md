# Nervousync Utils

**Enterprise-Grade Java Utility Foundation Library**

[![Maven Central](https://img.shields.io/maven-central/v/org.nervousync/utils-bom?color=green&label=Release)](https://mvnrepository.com/artifact/org.nervousync/utils-bom)
![Maven Snapshot](https://img.shields.io/maven-metadata/v?label=Snapshot&metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Forg%2Fnervousync%2Futils-bom%2Fmaven-metadata.xml)
[![License](https://img.shields.io/github/license/wmkm0113/utils)](https://github.com/wmkm0113/utils/blob/mainline/LICENSE)
![Language](https://img.shields.io/badge/language-Java-green)
[![Twitter:wmkm0113](https://img.shields.io/twitter/follow/wmkm0113?label=Follow)](https://twitter.com/wmkm0113)

English
[简体中文](README_zh_CN.md)
[繁體中文](README_zh_TW.md)

---

## Table of contents
* [Overview](#overview)
* [JDK Version Compatibility](#JDK-Version-Compatibility)
* [Design Philosophy](#Design-Philosophy)
* [Modules](#Modules)
* [Installation](#Installation)
* [Quick Start](#Quick-Start)
    + [JavaBean and the conversion between different data types](#JavaBean-and-the-conversion-between-different-data-types)
    + [Random ID Generation](#Random-ID-Generation)
    + [Geographic Information](#Geographic-Information)
    + [Unified Configuration File Management](#Unified-Configuration-File-Management)
    + [Internationalization Support](#Internationalization-Support)
    + [OTP Generation and Validate](#OTP-Generation-and-Validate)
* [Architecture Overview](#Architecture-Overview)
* [Comparison](#Comparison)
* [Suitable Scenarios](#Suitable-Scenarios)
* [Versioning](#Versioning)
* [Contributions and feedback](#contributions-and-feedback)
* [License](#License)
* [Donations](#donations)
* [Final Note](#Final-Note)

## Overview

**Nervousync Utils** is an enterprise-oriented Java utility foundation designed to provide reusable infrastructure capabilities across backend systems.

Unlike lightweight helper libraries, this project focuses on **capability-level abstractions** such as configuration management, internationalization, security utilities, HTTP communication, lifecycle management, and structured data transformation.

It is framework-agnostic and can be integrated into:

* Standalone Java applications
* Microservices
* Enterprise platform architectures
* Modular backend systems

---

## JDK Version Compatibility
**Compile:** OpenJDK 11   
**Test：** OpenJDK 11/17/21  
**Runtime：** OpenJDK 11+ or compatible version  
**Jakarta EE Platform:** 10

## Design Philosophy
This project is built upon the following engineering principles:

- Modular Design

The project provides functional modules with different focuses, allowing users to choose according to their needs, minimizing external dependencies, and reducing the overall project burden and security risks.

- Comprehensive Advanced Security Features

The project provides support for a variety of the latest encryption algorithms, as well as complete OTP (supporting TOTP and HOTP) support.

- Framework Independence

It does not forcibly depend on any heavyweight frameworks.

## Modules

| Module         | Description                                                                                                                                                                           |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| utils-bom      | BOM module                                                                                                                                                                            |
| utils-core     | Core module, basic tools                                                                                                                                                              |
| utils-beans    | JavaBean conversion tool for different data formats (XML/JSON/YAML)                                                                                                                   |
| utils-config   | Unified configuration file management, startup item management                                                                                                                        |
| utils-i18n     | Internationalization engine (supports multiple languages, internationalization, and localization)                                                                                     |
| utils-log4j    | Log configuration implemented using Log4j2                                                                                                                                            |
| utils-mail     | Email tool                                                                                                                                                                            |
| utils-net      | Network access tool (including network requests, network files, SNMP monitoring)                                                                                                      |
| utils-office   | Excel file operation support                                                                                                                                                          |
| utils-security | Security tools (including encryption, decryption, signing, verification, key management), OTP tool, security factory (for automatic encryption and decryption of configuration files) |
| utils-zip      | Zip compressed file access support                                                                                                                                                    |
| utils-all      | Contains all modules                                                                                                                                                                  |

Each module can be used independently.

## Installation

**Maven：**
### Direct reference
```
<dependency>
    <groupId>org.nervousync</groupId>
	<artifactId>${module_name}</artifactId>
    <version>${version}</version>
</dependency>
```
### Use BOM management
```
<!-- Import BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.nervousync</groupId>
            <artifactId>utils-bom</artifactId>
            <version>${version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<!-- Dependencies do not need to have version numbers specified. -->
<dependencies>
    <dependency>
        <groupId>org.nervousync</groupId>
        <artifactId>${module_name}</artifactId>
        <version>${version}</version>
    </dependency>
    ...
</dependencies>
```
**Gradle：**
Gradle 5.0+ support import Maven BOM as the platform
### Direct reference
```
implementation 'org.nervousync:${module_name}:${version}'
```
### Use BOM management
```
// 导入 BOM，使用 platform
implementation platform('org.nervousync:utils-bom:${version}')
// 依赖项不需要写版本号
implementation 'org.nervousync:${module_name}'
```
**SBT：**
```
libraryDependencies += "org.nervousync" % "${module_name}" % "${version}" % "provided"
```
**Ivy：**
```
<dependency org="org.nervousync" name="${module_name}" rev="${version}"/>
```
---

## Quick Start

### JavaBean and the conversion between different data types

`utils-core` (using jsonb) supports XML and JSON data formats, while `utils-beans` (using Jackson) supports XML, JSON, and YAML data formats.

First, define a simple JavaBean and add the corresponding annotations:
```java
//  Use the `OutputConfig` annotation to declare the data format type
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
With just one line of code, you can convert a JavaBean instance to its corresponding string:
```java
String string = BeanUtils.objectToString(user);
```

To convert a string to a JavaBean, you also only need one line of code:
```java
User user = BeanUtils.stringToObject(string, User.class);
```
### Random ID Generation

In daily development, there's often a need to generate random IDs. Now you only need to:

```java
UUID uuid = IDUtils.UUIDv4();
ULID ulid = IDUtils.ULID();
CUID cuid = IDUtils.CUID();
long snowflake = IDUtils.snowflake();
String nano = IDUtils.nano();

```

### Geographic Information

Convert coordinate systems:

```java
GeoPoint gpsPoint = LocationUtils.anyToGPS(currentPoint);
GeoPoint gcj02Point = LocationUtils.anyToGCJ02(currentPoint);
GeoPoint bd09Point = LocationUtils.anyToBD09(currentPoint);
```

Calculate the distance between two coordinate points (unit: meters):
```java
double distance = LocationUtils.distance(beginPoint, endPoint);
```

### Unified Configuration File Management

This feature requires the `utils-config` module.

In daily development, we often encounter various configuration information. This project provides a unified interface for managing this configuration information.

1. First, we need to define the JavaBean for the configuration file:
```java
/*  After adding the Signature annotation, the configuration file manager 
    will generate a signature when saving configuration information and verify 
    the signature when reading information, ensuring that the configuration file 
    will not be modified without authorization.
 */
@Signature("signature")
@XmlRootElement(name = "proxy_config", namespace = "https://nervousync.org/schemas/proxy")
@XmlAccessorType(XmlAccessType.NONE)
//  OutputConfig must be specified and the data type and encoding set must be configured.
@OutputConfig(type = StringType.XML, encoding = "UTF-8")
public final class ProxyConfig implements Serializable {
	
	@XmlElement(name = "username")
	private String userName;
    /**
    * After adding a password, the configuration file manager automatically encrypts/decrypts the information when saving and reading the configuration file to prevent the leakage of sensitive information.
    */
	@Password
	@XmlElement(name = "password")
	private String password;
	@XmlElement
	private String signature;
	// getters and setters
}
```
2. Save, read, configure information
```java
boolean result = ConfigureManager.getInstance().saveConfigure(proxyConfig);
ProxyConfig proxyConfig = ConfigureManager.getInstance().readConfigure(ProxyConfig.class);
```

### Internationalization Support

This feature requires the `utils-i18n` module.

1. Define Resource File Information
The storage path for multilingual resource files is META-INF/i18n/Resources.json within the JAR file. The specific format is as follows:

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
        "pattern": "加载资源描述文件映射表出错，文件地址：{0}"
      }
    }
  }
}
```
2. Referencing Multilingual Information:

```java
// Use the groupId and bundle defined in the resource file to get the corresponding multilingual information agent
MessageAgent agent = MultilingualUtils.newAgent("org.nervousync", "utils");
String message = agent.getMessage("Load_Schema_Mapping_Error", "/opt/schemas/file.xsd");
```

3. Multilingual Logging

```java
LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());
logger.info("Load_Schema_Mapping_Error", "/opt/schemas/file.xsd");
```

Plurals expressions are supported in the resource file.

### OTP Generation and Validate

This feature requires the `utils-security` module.

Generate OTP key:
```java
String secret = OTPUtils.generateRandomKey();
```

Calculate the time offset between TOTP users and the server:

```java
long fixedTime = OTPUtils.calculateFixedTime(secret, authCode);
```

Validate TOTP data
```java
boolean result = OTPUtils.validateTOTPCode(secret, authCode, fixedTime);
```

Users can choose different algorithms; the default is HMAC-SHA1. It supports both time-based standard one-time password algorithms (TOTP) and counter-based one-time password algorithms (HOTP).

---

## Architecture Overview

```
          +------------------+
          |   utils-core     |
          +------------------+
             /     |     \
            /      |      \
           v       v       v
  utils-config  utils-i18n  utils-security
          \          |           /
           \         |          /
            v        v         v
              utils-net   utils-launcher
```

* `core` provides shared abstractions
* Feature modules build upon core
* Modules are loosely coupled

---

## Comparison

| Feature               | Nervousync | Apache Commons | Hutool  |
|-----------------------|------------|----------------|---------|
| Unified Configuration | ✔          | ✘              | Partial |
| Built-in i18n Engine  | ✔          | ✘              | ✘       |
| OTP Support           | ✔          | ✘              | ✔       |
| Framework Agnostic    | ✔          | ✔              | ✔       |

This project focuses on structured enterprise capabilities rather than general-purpose helpers.

---

## Suitable Scenarios

* Enterprise backend systems
* Modular platform architecture
* Applications requiring secure configuration
* Systems with internationalization requirements
* Services requiring integrated security utilities

Not intended for lightweight scripts or minimal utility needs.

---

## Versioning

This project follows **Semantic Versioning**:

* **MAJOR** – JDK changes
* **MINOR** – Breaking API changes
* **RELEASE** – Backward-compatible feature additions
* **PATCH** – Bug fixes and minor improvements

---

## Contributions and feedback

Friends are welcome to translate the prompt information, error messages, etc. in this document and project into more languages to help more users better understand and use this toolkit.
If you find problems during use or need to improve or add related functions, please submit an issue to this project or send email to [wmkm0113\@gmail.com](mailto:wmkm0113@gmail.com?subject=bugs_and_features)
For better communication, please include the following information when submitting an issue or sending an email: 

1. The purpose is: discovering bugs/function improvements/add new features
2. Please paste the following information (if it exists): incoming data, expected results, error stack information
3. Where do you think there may be a problem with the code (if provided, it can help us find and solve the problem as soon as possible)

If you are submitting information about adding new features, please ensure that the features to be added are general needs, that is, the new features can help most users.

We also welcome code contributions from everyone. Please follow the steps below:

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

Please ensure:

* Code follows the project structure
* Public APIs are documented
* Backward compatibility is considered

If you need to add customized special requirements, I will charge a certain custom development fee. The specific fee amount will be assessed based on the workload of the customized special requirements.
For customized special features, please send an email directly to [wmkm0113\@gmail.com](mailto:wmkm0113@gmail.com?subject=payment_features). At the same time, please try to indicate the budget amount of development cost you can afford in the email.

---

## License

Licensed under the Apache License 2.0.
See the `LICENSE` file for details.

---

## Donations

To support this project, you can make a donation to:

- Bitcoin address: bc1q3nfj9gafu3x25ea260g7cyhh5s9gnx347tznsf
- Ethereum address: 0x849D143e943bAA6Dd078d02ebAEc205E2b00a7CA
- Solana address: 4Fvujk8DEkVAtYwzim1vrobNm4s72Ra6Xrsu83v2hqE2
- BNB address: 0x849D143e943bAA6Dd078d02ebAEc205E2b00a7CA

## Final Note

Nervousync Utils aims to provide a stable and reusable infrastructure foundation for enterprise Java systems.

If your goal is to build maintainable backend platforms with explicit control over configuration, security, and lifecycle management, this project is designed to support that objective.
