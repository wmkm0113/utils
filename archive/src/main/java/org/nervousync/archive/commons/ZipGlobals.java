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
package org.nervousync.archive.commons;

public final class ZipGlobals {

	/*
	 * Header signatures
	 */
	/**
	 * <span class="en-US">ZIP64 extended information extra field</span>
	 * <span class="zh-CN">ZIP64扩展信息扩展块</span>
	 */
	public static final int ZIP64_EXTRA_HEADER_ID = 0x0001;
	/**
	 * <span class="en-US">Strong encryption extra field</span>
	 * <span class="zh-CN">强加密扩展块</span>
	 */
	public static final int STRONG_ENC_HEADER_ID = 0x0017;
	/**
	 * <span class="en-US">AES extra field</span>
	 * <span class="zh-CN">AES 扩展块</span>
	 */
	public static final int AES_ENC_HEADER_ID = 0x9901;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：None</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_NONE = 0x0000;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: DES</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：DES</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_DES = 0x6601;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: RC2 old version</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：RC2，旧版本</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_RC2_OLD = 0x6602;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: 3DES-168</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：3DES-168</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_3DES_168 = 0x6603;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: 3DES-112</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：3DES-112</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_3DES_112 = 0x6609;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: AES-128</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：AES-128</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_AES_128 = 0x660E;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: AES-192</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：AES-192</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_AES_192 = 0x660F;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: AES-256</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：AES-256</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_AES_256 = 0x6610;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: RC2 new version</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：RC2，新版本</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW = 0x6702;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: Blowfish</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：Blowfish</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_BLOWFISH = 0x6720;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: Twofish</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：Twofish</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_TWOFISH = 0x6721;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: RC4</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：RC4</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_RC4 = 0x6801;
	/**
	 * <span class="en-US">Zip64 central directory protection algorithm: Unknown</span>
	 * <span class="zh-CN">Zip64 中央目录加密算法：未知</span>
	 */
	public static final int ZIP64_HEADER_ENC_ALGORITHM_UNKNOWN = 0xFFFF;
	/**
	 * <span class="en-US">Zip64 central directory protection decrypt credential: Password</span>
	 * <span class="zh-CN">Zip64 中央目录解密凭据：密码</span>
	 */
	public static final int ZIP64_HEADER_ENC_FLAGS_PASSWORD = 0x0001;
	/**
	 * <span class="en-US">Zip64 central directory protection decrypt credential: Certificate</span>
	 * <span class="zh-CN">Zip64 中央目录解密凭据：证书</span>
	 */
	public static final int ZIP64_HEADER_ENC_FLAGS_CERTIFICATE = 0x0002;
	/**
	 * <span class="en-US">Zip64 central directory protection decrypt credential: Password or Certificate</span>
	 * <span class="zh-CN">Zip64 中央目录解密凭据：密码或证书</span>
	 */
	public static final int ZIP64_HEADER_ENC_FLAGS_BOTH = 0x0003;
	/**
	 * <span class="en-US">Zip64 central directory protection decrypt credential: NON-OAEP</span>
	 * <span class="zh-CN">Zip64 中央目录解密凭据：NON-OAEP</span>
	 */
	public static final int ZIP64_HEADER_ENC_FLAGS_NON_OAEP = 0x0100;
	/**
	 * <span class="en-US">Zip64 central directory protection decrypt credential: Password or Certificate</span>
	 * <span class="zh-CN">Zip64 中央目录解密凭据：密码或证书</span>
	 */
	public static final int ZIP64_HEADER_ENC_FLAGS_ERD_3DES = 0x4000;
	/**
	 * <span class="en-US">Zip64 central directory hash algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录摘要算法：无</span>
	 */
	public static final int ZIP64_HEADER_HASH_ALGORITHM_NONE = 0x0000;
	/**
	 * <span class="en-US">Zip64 central directory hash algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录摘要算法：无</span>
	 */
	public static final int ZIP64_HEADER_HASH_ALGORITHM_CRC32 = 0x0001;
	/**
	 * <span class="en-US">Zip64 central directory hash algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录摘要算法：无</span>
	 */
	public static final int ZIP64_HEADER_HASH_ALGORITHM_MD5 = 0x8003;
	/**
	 * <span class="en-US">Zip64 central directory hash algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录摘要算法：无</span>
	 */
	public static final int ZIP64_HEADER_HASH_ALGORITHM_SHA1 = 0x8004;
	/**
	 * <span class="en-US">Zip64 central directory hash algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录摘要算法：无</span>
	 */
	public static final int ZIP64_HEADER_HASH_ALGORITHM_RIPEMD160 = 0x8007;
	/**
	 * <span class="en-US">Zip64 central directory hash algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录摘要算法：无</span>
	 */
	public static final int ZIP64_HEADER_HASH_ALGORITHM_SHA256 = 0x800C;
	/**
	 * <span class="en-US">Zip64 central directory hash algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录摘要算法：无</span>
	 */
	public static final int ZIP64_HEADER_HASH_ALGORITHM_SHA384 = 0x800D;
	/**
	 * <span class="en-US">Zip64 central directory hash algorithm: None</span>
	 * <span class="zh-CN">Zip64 中央目录摘要算法：无</span>
	 */
	public static final int ZIP64_HEADER_HASH_ALGORITHM_SHA512 = 0x800E;
	/**
	 * <span class="en-US">Extended timestamp header id</span>
	 * <span class="zh-CN">扩展时间戳标记头</span>
	 */
	public static final int EXTENDED_TIMESTAMP_HEADER_ID = 0x5455;
	/**
	 * <span class="en-US">General file header signature, read as a little-endian number</span>
	 * <span class="zh-CN">中央目录文件头标识值，小端读取</span>
	 */
	public static final long GENERAL_FILE_HEADER_SIGNATURE = 0x02014b50L;
	/**
	 * <span class="en-US">Local file header signature, read as a little-endian number</span>
	 * <span class="zh-CN">本地文件头标识值，小端读取</span>
	 */
	public static final long LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50L;
	/**
	 * <span class="en-US">End of central directory signature, read as a little-endian number</span>
	 * <span class="zh-CN">中央目录结束标识值，小端读取</span>
	 */
	public static final long END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50L;
	/**
	 * <span class="en-US">Archive extra data record signature, read as a little-endian number</span>
	 * <span class="zh-CN">文档额外数据记录标识值，小端读取</span>
	 */
	public static final long EXTRA_DATA_RECORD_SIGNATURE = 0x08064b50L;
	/**
	 * <span class="en-US">ZIP64 end of central directory locator signature, read as a little-endian number</span>
	 * <span class="zh-CN">ZIP64核心目录位置标识值，小端读取</span>
	 */
	public static final long ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE = 0x07064b50L;
	/**
	 * <span class="en-US">ZIP64 end of central directory signature, read as a little-endian number</span>
	 * <span class="zh-CN">ZIP64核心目录结束标识值，小端读取</span>
	 */
	public static final long ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06064b50L;
	/**
	 * <span class="en-US">Data descriptor signature, read as a little-endian number</span>
	 * <span class="zh-CN">数据描述符标识值，小端读取</span>
	 */
	public static final long DATA_DESCRIPTOR_SIGNATURE = 0x08074b50L;
	/**
	 * <span class="en-US">Native split archive signature, read as a little-endian number</span>
	 * <span class="zh-CN">原生分卷标识值，小端读取</span>
	 */
	public static final long NATIVE_SPLIT_SIGNATURE = 0x08074b50L;
	/**
	 * <span class="en-US">Digital signature, read as a little-endian number</span>
	 * <span class="zh-CN">数字签名标识值，小端读取</span>
	 */
	public static final long DIGITAL_SIGNATURE = 0x05054b50L;
	/**
	 * <span class="en-US">Temporary split archive signature, read as a little-endian number</span>
	 * <span class="zh-CN">临时分卷标识值，小端读取</span>
	 */
	public static final long TEMP_SPLIT_SIGNATURE = 0x30304b50L;
	/**
	 * <span class="en-US">ZIP version needed to extract</span>
	 * <span class="zh-CN">ZIP解压需要的最低版本号</span>
	 */
	public static final int ZIP_16_VERSION = 20;
	/**
	 * <span class="en-US">ZIP64 version needed to extract</span>
	 * <span class="zh-CN">ZIP64解压需要的最低版本号</span>
	 */
	public static final int ZIP_64_VERSION = 45;
	/**
	 * <span class="en-US">ZIP64 version 2 needed to extract</span>
	 * <span class="zh-CN">ZIP64 版本2解压需要的最低版本号</span>
	 */
	public static final int ZIP_64_VERSION_2 = 62;
	/**
	 * <span class="en-US">Maximum recipients limit</span>
	 * <span class="zh-CN">最大接收者数量</span>
	 */
	public static final int MAX_RECIPIENTS = 1024;
	/**
	 * <span class="en-US">Maximum length of comment data bytes</span>
	 * <span class="zh-CN">注释字节数组的最大长度</span>
	 */
	public static final int MAX_ALLOWED_ZIP_COMMENT_LENGTH = 0xFFFF;
	/**
	 * <span class="en-US">Compression Type: STORE</span>
	 * <span class="zh-CN">压缩类型：STORE</span>
	 */
	public static final int COMP_STORE = 0;
	/**
	 * <span class="en-US">Compression Type: DEFLATE</span>
	 * <span class="zh-CN">压缩类型：DEFLATE</span>
	 */
	public static final int COMP_DEFLATE = 8;
	/**
	 * <span class="en-US">Compression Type: DEFLATE64</span>
	 * <span class="zh-CN">压缩类型：DEFLATE64</span>
	 */
	public static final int COMP_DEFLATE64 = 9;
	/**
	 * <span class="en-US">Compression Type: BZIP2</span>
	 * <span class="zh-CN">压缩类型：BZIP2</span>
	 */
	public static final int COMP_BZIP2 = 12;
	/**
	 * <span class="en-US">Compression Type: LZMA</span>
	 * <span class="zh-CN">压缩类型：LZMA</span>
	 */
	public static final int COMP_LZMA = 14;
	/**
	 * <span class="en-US">Compression Type: Z standard</span>
	 * <span class="zh-CN">压缩类型：Z standard</span>
	 */
	public static final int COMP_Z_STANDARD = 93;
	/**
	 * <span class="en-US">Compression Type: XZ</span>
	 * <span class="zh-CN">压缩类型：XZ</span>
	 */
	public static final int COMP_XZ = 95;
	/**
	 * <span class="en-US">Compression Type: WinZip AES</span>
	 * <span class="zh-CN">压缩类型：WinZip AES</span>
	 */
	public static final int COMP_AES = 99;
	/**
	 * <span class="en-US">AES authentication data length</span>
	 * <span class="zh-CN">AES验证数据长度</span>
	 */
	public static final int AES_AUTH_LENGTH = 10;
	/**
	 * <span class="en-US">AES block length</span>
	 * <span class="zh-CN">AES数据块大小</span>
	 */
	public static final int AES_BLOCK_SIZE = 16;
	/**
	 * <span class="en-US">AES strength length: 128</span>
	 * <span class="zh-CN">AES加密强度：128</span>
	 */
	public static final int AES_STRENGTH_128 = 0x01;
	/**
	 * <span class="en-US">AES strength length: 192</span>
	 * <span class="zh-CN">AES加密强度：192</span>
	 */
	public static final int AES_STRENGTH_192 = 0x02;
	/**
	 * <span class="en-US">AES strength length: 256</span>
	 * <span class="zh-CN">AES加密强度：256</span>
	 */
	public static final int AES_STRENGTH_256 = 0x03;
	/**
	 * <span class="en-US">Minimum length of split zip file</span>
	 * <span class="zh-CN">ZIP文件分割最小长度</span>
	 */
	public static final int MIN_SPLIT_LENGTH = 65536;
	/**
	 * <span class="en-US">Limit size of ZIP16</span>
	 * <span class="zh-CN">ZIP16规定的最大限制</span>
	 */
	public static final int ZIP_16_LIMIT = 0xFFFF;
	/**
	 * <span class="en-US">Limit size of ZIP64</span>
	 * <span class="zh-CN">ZIP64规定的最大限制</span>
	 */
	public static final long ZIP_64_LIMIT = 0xFFFFFFFFL;
	/**
	 * <span class="en-US">No encryption</span>
	 * <span class="zh-CN">无加密</span>
	 */
	public static final int ENC_NO_ENCRYPTION = -1;
	/**
	 * <span class="en-US">Standard encryption</span>
	 * <span class="zh-CN">标准加密</span>
	 */
	public static final int ENC_METHOD_STANDARD = 0;
	/**
	 * <span class="en-US">Strong encryption</span>
	 * <span class="zh-CN">强加密</span>
	 */
	public static final int ENC_METHOD_STRONG = 1;
	/**
	 * <span class="en-US">WinAES encryption</span>
	 * <span class="zh-CN">WinAES 加密</span>
	 */
	public static final int ENC_METHOD_AES = 99;
	/**
	 * Compression level for deflate algorithm
	 */
	public static final int DEFLATE_LEVEL_FASTEST = 1;
	/**
	 * The constant DEFLATE_LEVEL_FAST.
	 */
	public static final int DEFLATE_LEVEL_FAST = 3;
	/**
	 * The constant DEFLATE_LEVEL_NORMAL.
	 */
	public static final int DEFLATE_LEVEL_NORMAL = 5;
	/**
	 * The constant DEFLATE_LEVEL_MAXIMUM.
	 */
	public static final int DEFLATE_LEVEL_MAXIMUM = 7;
	/**
	 * The constant DEFLATE_LEVEL_ULTRA.
	 */
	public static final int DEFLATE_LEVEL_ULTRA = 9;
	/**
	 * The constant PASSWORD_VERIFIER_LENGTH.
	 */
	public static final int PASSWORD_VERIFIER_LENGTH = 2;
	/**
	 * The constant STD_DEC_HDR_SIZE.
	 */
	public static final int STD_DEC_HDR_SIZE = 12;
	/**
	 * The constant DEFAULT_ZIP_PAGE_SEPARATOR.
	 */
	public static final String DEFAULT_ZIP_PAGE_SEPARATOR = "/";

	private ZipGlobals() {
	}
}
