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

package org.nervousync.commons.zip;

public final class ZipGlobals {

	/*
	 * Header signatures
	 */
	/**
	 * <span class="en-US">Local file header signature, read as a little-endian number</span>
	 * <span class="zh-CN">文件头标识值，小端读取</span>
	 */
	public static final long LOCSIG = 0x04034b50L;
	/**
	 * <span class="en-US">Data descriptor signature, read as a little-endian number</span>
	 * <span class="zh-CN">数据描述符标识值，小端读取</span>
	 */
	public static final long EXTSIG = 0x08074b50L;
	/**
	 * <span class="en-US">Central directory file header signature, read as a little-endian number</span>
	 * <span class="zh-CN">中央目录文件头标识值，小端读取</span>
	 */
	public static final long CENSIG = 0x02014b50L;
	/**
	 * <span class="en-US">Central directory end signature, read as a little-endian number</span>
	 * <span class="zh-CN">中央目录结束标识值，小端读取</span>
	 */
	public static final long ENDSIG = 0x06054b50L;
	/**
	 * <span class="en-US">Digital signature, read as a little-endian number</span>
	 * <span class="zh-CN">数字签名标识值，小端读取</span>
	 */
	public static final long DIGSIG = 0x05054b50L;
	/**
	 * <span class="en-US">Archive extra data record signature, read as a little-endian number</span>
	 * <span class="zh-CN">文档额外数据记录标识值，小端读取</span>
	 */
	public static final long ARCEXTDATREC = 0x08064b50L;
	/**
	 * <span class="en-US">ZIP64 end of central directory locator signature, read as a little-endian number</span>
	 * <span class="zh-CN">ZIP64核心目录位置标识值，小端读取</span>
	 */
	public static final long ZIP64ENDCENDIRLOC = 0x07064b50L;
	/**
	 * <span class="en-US">ZIP64 end of central directory signature, read as a little-endian number</span>
	 * <span class="zh-CN">ZIP64核心目录结束标识值，小端读取</span>
	 */
	public static final long ZIP64ENDCENDIRREC = 0x06064b50;
	/**
	 * <span class="en-US">ZIP64 extended information extra field</span>
	 * <span class="zh-CN">ZIP64扩展信息扩展块</span>
	 */
	public static final int EXTRAFIELDZIP64LENGTH = 0x0001;
	/**
	 * <span class="en-US">AES encrypt signature</span>
	 * <span class="zh-CN">AES加密标识</span>
	 */
	public static final int AESSIG = 0x9901;
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
	 * <span class="en-US">ZIP64 version needed to extract</span>
	 * <span class="zh-CN">ZIP64解压需要的最低版本号</span>
	 */
	public static final int ZIP_64_VERSION = 45;
	/**
	 * The constant UFT8_NAMES_FLAG.
	 */
	public static final int UFT8_NAMES_FLAG = 1 << 11;
	/**
	 * Encryption types
	 */
	public static final int ENC_NO_ENCRYPTION = -1;
	/**
	 * The constant ENC_METHOD_STANDARD.
	 */
	public static final int ENC_METHOD_STANDARD = 0;
	/**
	 * The constant ENC_METHOD_STRONG.
	 */
	public static final int ENC_METHOD_STRONG = 1;
	/**
	 * The constant ENC_METHOD_AES.
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
	 * The constant ENDHDR.
	 * END header size
	 */
	public static final int ENDHDR = 22;
	/**
	 * IO buffer size
	 */
	public static final int IO_BUFFER_SIZE = 64 * 1024;

	private ZipGlobals() {
	}
}
