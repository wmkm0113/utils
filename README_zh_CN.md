# Nervousync Utility

企业级 Java 实用程序基础库

[![Maven Central](https://img.shields.io/maven-central/v/org.nervousync/utils-bom?color=green&label=Release)](https://mvnrepository.com/artifact/org.nervousync/utils-bom)
![Maven Snapshot](https://img.shields.io/maven-metadata/v?label=Snapshot&metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Forg%2Fnervousync%2Futils-bom%2Fmaven-metadata.xml)
[![License](https://img.shields.io/github/license/wmkm0113/utils)](https://github.com/wmkm0113/utils/blob/mainline/LICENSE)
![Language](https://img.shields.io/badge/language-Java-green)
[![Twitter:wmkm0113](https://img.shields.io/twitter/follow/wmkm0113?label=Follow)](https://twitter.com/wmkm0113)

[English](README.md)
简体中文
[繁體中文](README_zh_TW.md)

---

## 目录
* [项目概览](#项目概览)
* [JDK版本兼容](#JDK版本兼容)
* [设计理念](#设计理念)
* [模块](#模块)
* [安装引用](#安装引用)
* [快速开始](#快速开始)
  + [JavaBean 与不同数据的相互转换](#JavaBean-与不同数据的相互转换)
  + [随机ID生成](#随机ID生成)
  + [地理信息](#地理信息)
  + [统一管理的配置文件](#统一管理的配置文件)
  + [国际化的支持](#国际化的支持)
  + [OTP生成与验证](#OTP生成与验证)
* [架构概览](#架构概览)
* [同类产品比较](#同类产品比较)
* [适用场景](#适用场景)
* [版本控制](#版本控制)
* [贡献与反馈](#贡献与反馈)
* [许可协议](#许可协议)
* [捐款](#捐款)
* [结语](#结语)

## 项目概览

**Nervousync Utils** 是一个面向企业的 Java 工具库，旨在为后端系统提供可重用的基础设施功能。

与轻量级辅助库不同，该项目专注于**功能层面的抽象**，例如配置管理、国际化、安全工具、HTTP 通信、生命周期管理和结构化数据转换。

它与特定框架无关，可以轻松方便的集成到以下系统中：

- 独立的 Java 应用程序
- 微服务
- 企业平台架构
- 模块化后端系统

---

## JDK版本兼容
**编译：** OpenJDK 11   
**测试：** OpenJDK 11 / 17 / 21  
**运行：** OpenJDK 11+ 或兼容版本  
**Jakarta EE平台：** 10

---

## 设计理念

本项目基于以下工程原则构建：  

- 模块化设计

项目提供了不同侧重的功能模块，方便使用者按需选择，实现外部依赖的最小化，降低整体项目负担和安全风险。

- 提供完善的高级安全特性

项目提供了多种最新的加密算法支持，同时还提供了完整的 OTP（支持 TOTP 和 HOTP） 支持

- 与框架无关

不强制依赖某些重量级框架

---

## 模块

| 模块             | 概述                                                |
|----------------|---------------------------------------------------|
| utils-bom      | BOM模块                                             |
| utils-core     | 核心模块、基础工具                                         |
| utils-beans    | JavaBean 与不同数据格式（XML/JSON/YAML）的转换工具              |
| utils-config   | 统一的配置文件管理、启动项的管理                                  |
| utils-i18n     | 国际化引擎（支持多语言、国际化、本地化）                              |
| utils-log4j    | 使用Log4j2实现的日志配置                                   |
| utils-mail     | 电子邮件工具                                            |
| utils-net      | 网络访问工具（包括网络请求、网络文件、SNMP监控）                        |
| utils-office   | Excel 文件操作支持                                      |
| utils-security | 安全工具（包括加密、解密、签名、验证、密钥管理）、OTP工具、安全工厂（用于配置文件的自动加解密） |
| utils-zip      | Zip压缩文件的访问支持                                      |
| utils-all      | 包含所有模块                                            |

每个模块均可独立使用。

---

## 安装引用
**Maven：**
### 直接引用
```
<dependency>
    <groupId>org.nervousync</groupId>
	<artifactId>${模块名}</artifactId>
    <version>${版本号}</version>
</dependency>
```
### 使用 BOM 管理
```
<!-- 导入 BOM，使用 platform -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.nervousync</groupId>
            <artifactId>utils-bom</artifactId>
            <version>${版本号}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<!-- 依赖项不需要写版本号 -->
<dependencies>
    <dependency>
        <groupId>org.nervousync</groupId>
        <artifactId>${模块名}</artifactId>
        <version>${版本号}</version>
    </dependency>
    ...
</dependencies>
```
**Gradle：**
Gradle 5.0+ 支持将 Maven BOM 导入为 platform 以对齐依赖项版本。
### 直接引用
```
implementation 'org.nervousync:${模块名}:${版本号}'
```
### 使用 BOM 管理
```
// 导入 BOM，使用 platform
implementation platform('org.nervousync:utils-bom:${版本号}')
// 依赖项不需要写版本号
implementation 'org.nervousync:${模块名}'
```
**SBT：**
```
libraryDependencies += "org.nervousync" % "${模块名}" % "${版本号}" % "provided"
```
**Ivy：**
```
<dependency org="org.nervousync" name="${模块名}" rev="${版本号}"/>
```

## 快速开始
### JavaBean 与不同数据的相互转换

`utils-core（使用jsonb）`支持 XML 和 JSON 格式的数据，`utils-beans（使用Jackson）`支持 XML、JSON 和 YAML 格式的数据

首先定义一个简单的 JavaBean，并添加相应的注解：
```java
//  使用 OutputConfig 注解来声明数据的格式类型
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
仅需一行代码，即可将 JavaBean 实例对象转换为对应的字符串：
```java
String string = BeanUtils.objectToString(user);
```

需要将字符串转换为 JavaBean，也仅需要一行代码：
```java
User user = BeanUtils.stringToObject(string, User.class);
```

### 随机ID生成

在日常开发中，经常会有需要生成随机ID的需求，现在您只需要：
```java
UUID uuid = IDUtils.UUIDv4();
ULID ulid = IDUtils.ULID();
CUID cuid = IDUtils.CUID();
long snowflake = IDUtils.snowflake();
String nano = IDUtils.nano();
```

### 地理信息

转换坐标系：
```java
GeoPoint gpsPoint = LocationUtils.anyToGPS(currentPoint);
GeoPoint gcj02Point = LocationUtils.anyToGCJ02(currentPoint);
GeoPoint bd09Point = LocationUtils.anyToBD09(currentPoint);
```

计算两个坐标点的距离（单位：米）：
```java
double distance = LocationUtils.distance(beginPoint, endPoint);
```

### 统一管理的配置文件

此功能需要`utils-config`模块

在日常开发的过程中，经常会遇到各种不同的配置信息，本项目提供了一个统一接口，用于管理这些配置信息。

1. 首先需要定义配置文件的 JavaBean ：
```java
//  添加 Signature 注解后，配置文件管理器会在保存配置信息时生成签名，并在读取信息时验证签名，保证配置文件不会有未经授权的修改
@Signature("signature")
@XmlRootElement(name = "proxy_config", namespace = "https://nervousync.org/schemas/proxy")
@XmlAccessorType(XmlAccessType.NONE)
//  必须标注 OutputConfig，配置好数据类型和编码集
@OutputConfig(type = StringType.XML, encoding = "UTF-8")
public final class ProxyConfig implements Serializable {
	
	@XmlElement(name = "username")
	private String userName;
	//  添加 Password 后，配置文件管理器在保存和读取配置文件时，自动对信息进行加密/解密操作，防止敏感信息泄露
	@Password
	@XmlElement(name = "password")
	private String password;
	@XmlElement
	private String signature;
	// getters and setters
}
```

2. 保存、读取配置信息
```java
boolean result = ConfigureManager.getInstance().saveConfigure(proxyConfig);
ProxyConfig proxyConfig = ConfigureManager.getInstance().readConfigure(ProxyConfig.class);
```

### 国际化的支持

此功能需要`utils-i18n`模块

1. 定义资源文件信息

多语言的资源文件存储路径为 jar 包内的 META-INF/i18n/Resources.json，具体格式如下：
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

2. 引用多语言信息：
```java
//  使用资源文件中定义的 groupId 和 bundle 来获取对应的多语言信息代理
MessageAgent agent = MultilingualUtils.newAgent("org.nervousync", "utils");
String message = agent.getMessage("Load_Schema_Mapping_Error", "/opt/schemas/file.xsd");
```

3. 多语言日志
```java
LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());
logger.info("Load_Schema_Mapping_Error", "/opt/schemas/file.xsd");
```

资源文件中支持使用 Plurals 表达式

### OTP生成与验证

此功能需要`utils-security`模块

生成 OTP 密钥：
```java
String secret = OTPUtils.generateRandomKey();
```

计算 TOTP 用户与服务器的时间偏移
```java
long fixedTime = OTPUtils.calculateFixedTime(secret, authCode);
```

验证 TOTP 数据：
```java
boolean result = OTPUtils.validateTOTPCode(secret, authCode, fixedTime);
```

使用者可以选择不同的算法，默认为 HMAC-SHA1。支持基于时间的标准一次性密码算法（TOTP）和基于计数器的一次性密码算法（HOTP）。

---

## 架构概览

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

---


## 同类产品比较

| Feature  | Nervousync | Apache Commons | Hutool  |
|----------|------------|----------------|---------|
| 统一配置信息管理 | ✔          | ✘              | Partial |
| 内置的国际化引擎 | ✔          | ✘              | ✘       |
| 一次性密码支持  | ✔          | ✘              | ✔       |
| 无重型框架依赖  | ✔          | ✔              | ✔       |

本项目侧重于结构化的企业能力，而不是通用辅助工具。


## 适用场景

- 企业后端系统
- 模块化平台架构
- 需要统一配置管理的集成系统
- 具有国际化需求的系统
- 需要集成更多安全配置的服务

---

## 版本控制

本项目遵循语义化版本控制：

- 主版本号 (MAJOR) – JDK 版本升级变更
- 次版本号 (MINOR) – 内部 API 变更
- 发布版本号 (RELEASE) - 向后兼容的功能新增
- 补丁版本号 (PATCH) – 错误修复和细微改进

---

## 贡献与反馈

欢迎各位朋友将此文档及项目中的提示信息、错误信息等翻译为更多语言，以帮助更多的使用者更好地了解与使用此工具包。   
如果在使用过程中发现问题或需要改进、添加相关功能，请提交issue到本项目或发送电子邮件到[wmkm0113\@gmail.com](mailto:wmkm0113@gmail.com?subject=bugs_and_features)   
为了更好地沟通，请在提交issue或发送电子邮件时，写明如下信息：   
1、目的是：发现Bug/功能改进/添加新功能   
2、请粘贴以下信息（如果存在）：传入数据，预期结果，错误堆栈信息   
3、您认为可能是哪里的代码出现问题（如提供可以帮助我们尽快地找到并解决问题）   
如果您提交的是添加新功能的相关信息，请确保需要添加的功能是一般性的通用需求，即添加的新功能可以帮助到大多数使用者。

同时欢迎各位朋友贡献代码，请遵循以下流程：

1. Fork 此代码仓库
2. 创建特性分支
3. 提交拉取请求

贡献代码的同时请确保：

- 代码遵循项目结构
- 公共 API 已编写文档
- 已考虑向后兼容性

如果您需要添加的是定制化的特殊需求，我将收取一定的定制开发费用，具体费用金额根据定制化的特殊需求的工作量进行评估。   
定制化特殊需求请直接发送电子邮件到[wmkm0113\@gmail.com](mailto:wmkm0113@gmail.com?subject=payment_features)，同时请尽量在邮件中写明您可以负担的开发费用预算金额。

---

## 许可协议

本项目采用 Apache License 2.0 许可协议。
详情请参阅 `LICENSE` 文件

---

## 捐款
为了支持此项目，您可以向以下地址捐款：

* 比特币地址: bc1q3nfj9gafu3x25ea260g7cyhh5s9gnx347tznsf
* 以太坊地址: 0x849D143e943bAA6Dd078d02ebAEc205E2b00a7CA
* Solana 地址: 4Fvujk8DEkVAtYwzim1vrobNm4s72Ra6Xrsu83v2hqE2
* BNB 地址: 0x849D143e943bAA6Dd078d02ebAEc205E2b00a7CA
*
---

## 结语

本项目旨在为企业级 Java 系统提供稳定、可重用的基础架构，如果您希望构建可维护的后端平台，并对配置、安全性、国际化等方面有需求，那么本项目正是为此而设计的。
