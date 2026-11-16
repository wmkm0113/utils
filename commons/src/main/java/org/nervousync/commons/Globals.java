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
package org.nervousync.commons;

import java.io.File;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * <h2 class="en-US">Globals constants Value</h2>
 * <h2 class="zh-CN">全局常量值</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.4 $ $Date: Jul 2, 2018 18:44:29 $
 */
@SuppressWarnings("unused")
public final class Globals {

	/**
	 * <span class="en-US">Global unique instance object of secure random</span>
	 * <span class="zh-CN">全局唯一的随机数生成器</span>
	 */
	private static final ThreadLocal<SecureRandom> RANDOM = ThreadLocal.withInitial(SecureRandom::new);
	/**
	 * <span class="en-US">Multiplier value of calculate hash result.</span>
	 * <span class="zh-CN">计算哈希值需要用到的乘数</span>
	 */
	public static final int MULTIPLIER = 31;
	/**
	 * <span class="en-US">Default value of buffer size</span>
	 * <span class="zh-CN">默认缓冲区大小</span>
	 */
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	/**
	 * <span class="en-US">Default value of file reader buffer size</span>
	 * <span class="zh-CN">文件读取的默认缓冲区大小</span>
	 */
	public static final int READ_FILE_BUFFER_SIZE = 32768;
	/**
	 * <span class="en-US">Default value of timeout</span>
	 * <span class="zh-CN">默认超时时间</span>
	 */
	public static final int DEFAULT_TIME_OUT = 0;
	/**
	 * <span class="en-US">Default value of connect timeout (Unit: seconds)</span>
	 * <span class="zh-CN">默认连接超时时间（单位：秒）</span>
	 */
	public static final int DEFAULT_CONNECT_TIME_OUT = 5;
	/**
	 * <span class="en-US">Default value of primitive type int</span>
	 * <span class="zh-CN">int基础类型的默认值</span>
	 */
	public static final int DEFAULT_VALUE_INT = -1;
	/**
	 * <span class="en-US">Default value of primitive type long</span>
	 * <span class="zh-CN">long基础类型的默认值</span>
	 */
	public static final long DEFAULT_VALUE_LONG = -1L;
	/**
	 * <span class="en-US">Default value of primitive type short</span>
	 * <span class="zh-CN">short基础类型的默认值</span>
	 */
	public static final short DEFAULT_VALUE_SHORT = -1;
	/**
	 * <span class="en-US">Default value of primitive type double</span>
	 * <span class="zh-CN">double基础类型的默认值</span>
	 */
	public static final double DEFAULT_VALUE_DOUBLE = -1;
	/**
	 * <span class="en-US">Default value of primitive type float</span>
	 * <span class="zh-CN">float基础类型的默认值</span>
	 */
	public static final float DEFAULT_VALUE_FLOAT = -1;
	/**
	 * <span class="en-US">Default value of type String</span>
	 * <span class="zh-CN">String类型的默认值</span>
	 */
	public static final String DEFAULT_VALUE_STRING = "";
	/**
	 * <span class="en-US">Default value of reference time using it for Snowflake ID generator</span>
	 * <span class="zh-CN">默认起始时间戳值，用于雪花算法ID生成器</span>
	 */
	public static final long DEFAULT_REFERENCE_TIME = 1303315200000L;
	/**
	 * <span class="en-US">Default value of boolean value FALSE to int</span>
	 * <span class="zh-CN">默认的布尔值FALSE，用int表示</span>
	 */
	public static final int DEFAULT_STATUS_FALSE = 0;
	/**
	 * <span class="en-US">Default value of boolean value TRUE to int</span>
	 * <span class="zh-CN">默认的布尔值TRUE，用int表示</span>
	 */
	public static final int DEFAULT_STATUS_TRUE = 1;
	/**
	 * <span class="en-US">Initialize value of primitive type int</span>
	 * <span class="zh-CN">int类型的初始值</span>
	 */
	public static final int INITIALIZE_INT_VALUE = 0;
	/**
	 * <span class="en-US">Default scheduled task delay</span>
	 * <span class="zh-CN">默认的调度任务延时</span>
	 */
	public static final long DEFAULT_SCHEDULE_DELAY = 0L;
	/**
	 * <span class="en-US">Default scheduled task interval(Unit: millisecond)</span>
	 * <span class="zh-CN">默认的调度任务间隔（单位：毫秒）</span>
	 */
	public static final long DEFAULT_SCHEDULE_PERIOD = 5 * 60 * 1000L;
	/**
	 * <span class="en-US">Default value for XML annotations</span>
	 * <span class="zh-CN">XML注解的默认值</span>
	 */
	public static final String DEFAULT_XML_ANNOTATION_VALUE = "##default";
	/**
	 * <span class="en-US">The constant value of HTTP protocol prefix</span>
	 * <span class="zh-CN">HTTP协议的起始前缀值</span>
	 */
	public static final String HTTP_PROTOCOL = "http://";
	/**
	 * <span class="en-US">The constant value of SecureHTTP protocol prefix</span>
	 * <span class="zh-CN">安全HTTP协议的起始前缀值</span>
	 */
	public static final String SECURE_HTTP_PROTOCOL = "https://";
	/**
	 * <span class="en-US">Multilingual information key-value separation characters</span>
	 * <span class="zh-CN">多语言信息键值分割字符</span>
	 */
	public static final String DEFAULT_MULTILINGUAL_KEY_SPLIT_CHARACTER = "_";
	/**
	 * <span class="en-US">The constant value of extension separator</span>
	 * <span class="zh-CN">扩展名分割字符</span>
	 */
	public static final char EXTENSION_SEPARATOR = '.';
	/**
	 * <span class="en-US">The constant value of default package separator</span>
	 * <span class="zh-CN">默认包名分隔符</span>
	 */
	public static final char DEFAULT_PACKAGE_SEPARATOR = '.';
	/**
	 * <span class="en-US">The constant value of email folder: INBOX</span>
	 * <span class="zh-CN">收件箱电子邮件文件夹的默认值</span>
	 */
	public static final String DEFAULT_EMAIL_FOLDER_INBOX = "INBOX";
	/**
	 * <span class="en-US">The constant value of email folder: SPAM</span>
	 * <span class="zh-CN">垃圾邮件电子邮件文件夹的默认值</span>
	 */
	public static final String DEFAULT_EMAIL_FOLDER_SPAM = "Spam";
	/**
	 * <span class="en-US">The constant value of email folder: DRAFTS</span>
	 * <span class="zh-CN">草稿箱电子邮件文件夹的默认值</span>
	 */
	public static final String DEFAULT_EMAIL_FOLDER_DRAFTS = "Drafts";
	/**
	 * <span class="en-US">The constant value of email folder: TRASH</span>
	 * <span class="zh-CN">垃圾箱电子邮件文件夹的默认值</span>
	 */
	public static final String DEFAULT_EMAIL_FOLDER_TRASH = "Trash";
	/**
	 * <span class="en-US">The constant value of email folder: SENT</span>
	 * <span class="zh-CN">发件箱电子邮件文件夹的默认值</span>
	 */
	public static final String DEFAULT_EMAIL_FOLDER_SENT = "Sent";
	/**
	 * <span class="en-US">The constant value of default character encoding</span>
	 * <span class="zh-CN">默认的字符集编码</span>
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";
	/**
	 * <span class="en-US">The constant value of CP850 character encoding</span>
	 * <span class="zh-CN">CP850字符集编码</span>
	 */
	public static final String CHARSET_CP850 = "Cp850";
	/**
	 * <span class="en-US">The constant value of GBK character encoding</span>
	 * <span class="zh-CN">GBK字符集编码</span>
	 */
	public static final String CHARSET_GBK = "GBK";
	/**
	 * <span class="en-US">The constant value of current system default locale</span>
	 * <span class="zh-CN">当前系统的默认语言信息</span>
	 */
	public static final Locale DEFAULT_LOCALE = Locale.getDefault();
	/**
	 * <span class="en-US">The constant value of current system character encoding</span>
	 * <span class="zh-CN">当前系统的默认字符集编码</span>
	 */
	public static final String DEFAULT_SYSTEM_CHARSET = Charset.defaultCharset().displayName();
	/**
	 * <span class="en-US">The constant value of default split separator</span>
	 * <span class="zh-CN">默认的分割字符</span>
	 */
	public static final String DEFAULT_SPLIT_SEPARATOR = ",";
	/**
	 * <span class="en-US">The constant value of current system default page separator</span>
	 * <span class="zh-CN">当前系统的默认名称分隔符</span>
	 */
	public static final String DEFAULT_PAGE_SEPARATOR = File.separator;
	/**
	 * <span class="en-US">The constant value of default url separator</span>
	 * <span class="zh-CN">默认url分隔符</span>
	 */
	public static final String DEFAULT_URL_SEPARATOR = "/";
	/**
	 * <span class="en-US">The constant value of default resource separator</span>
	 * <span class="zh-CN">默认资源路径分隔符</span>
	 */
	public static final char DEFAULT_RESOURCE_SEPARATOR = '/';
	/**
	 * <span class="en-US">The constant value of default jar page separator</span>
	 * <span class="zh-CN">Jar包内默认名称分隔符</span>
	 */
	public static final String DEFAULT_JAR_PAGE_SEPARATOR = "/";
	/**
	 * <span class="en-US">The constant value of Content-Type: TEXT</span>
	 * <span class="zh-CN">文本内容类型的定义字符串</span>
	 */
	public static final String DEFAULT_CONTENT_TYPE_TEXT = "text/plain";
	/**
	 * <span class="en-US">The constant value of Content-Type: HTML</span>
	 * <span class="zh-CN">超文本内容类型的定义字符串</span>
	 */
	public static final String DEFAULT_CONTENT_TYPE_HTML = "text/html";
	/**
	 * <span class="en-US">The constant value of Content-Type: MULTIPART</span>
	 * <span class="zh-CN">多媒体内容类型的定义字符串</span>
	 */
	public static final String DEFAULT_CONTENT_TYPE_MULTIPART = "multipart/*";
	/**
	 * <span class="en-US">The constant value of Content-Type: RFC-822</span>
	 * <span class="zh-CN">RFC-822内容类型的定义字符串</span>
	 */
	public static final String DEFAULT_CONTENT_TYPE_MESSAGE_RFC822 = "message/rfc822";
	/**
	 * <span class="en-US">The constant value of Content-Type: ENCODED</span>
	 * <span class="zh-CN">表单编码类型的定义字符串</span>
	 */
	public static final String DEFAULT_CONTENT_TYPE_ENCODED = "application/x-www-form-urlencoded";
	/**
	 * <span class="en-US">The constant value of Content-Type: FORM_DATA MULTIPART</span>
	 * <span class="zh-CN">表单多媒体类型的定义字符串</span>
	 */
	//	Multipart content type
	public static final String FORM_DATA_CONTENT_TYPE_MULTIPART = "multipart/form-data";
	/**
	 * <span class="en-US">The constant value of Content-Type: MIXED</span>
	 * <span class="zh-CN">混合数据类型的定义字符串</span>
	 */
	public static final String DEFAULT_CONTENT_TYPE_MIXED = "multipart/mixed";
	/**
	 * <span class="en-US">The constant value of Content-Type: BINARY</span>
	 * <span class="zh-CN">二进制数据类型的定义字符串</span>
	 */
	public static final String DEFAULT_CONTENT_TYPE_BINARY = "application/octet-stream";
	/**
	 * <span class="en-US">The constant value of Content-Disposition</span>
	 * <span class="zh-CN">多部份主体的标头</span>
	 */
	public static final String FORM_DATA_CONTENT_DISPOSITION = "form-data";
	/**
	 * <span class="en-US">Default value of log file path</span>
	 * <span class="zh-CN">默认的日志文件路径</span>
	 */
	public static final String DEFAULT_LOG_FILE_PATH = Globals.DEFAULT_PAGE_SEPARATOR + "nervousync-log.log";

	/**
	 * <span class="en-US">URL prefixes for loading from the class path: "classpath:"</span>
	 * <span class="zh-CN">用于从类路径加载的 URL 前缀：“classpath:”</span>
	 */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/**
	 * <span class="en-US">URL prefixes for loading from the file system: "file:"</span>
	 * <span class="zh-CN">用于从文件系统加载的 URL 前缀：“file:”</span>
	 */
	public static final String FILE_URL_PREFIX = "file:";

	/**
	 * <span class="en-US">URL protocol for a file in the file system: "file"</span>
	 * <span class="zh-CN">文件系统中文件的 URL 协议：“file”</span>
	 */
	public static final String URL_PROTOCOL_FILE = "file";

	/**
	 * <span class="en-US">URL protocol for an entry from a jar file: "jar"</span>
	 * <span class="zh-CN">jar 文件中条目的 URL 协议：“jar”</span>
	 */
	public static final String URL_PROTOCOL_JAR = "jar";

	/**
	 * <span class="en-US">URL protocol for an entry from a zip file: "zip"</span>
	 * <span class="zh-CN">zip 文件中条目的 URL 协议：“zip”</span>
	 */
	public static final String URL_PROTOCOL_ZIP = "zip";

	/**
	 * <span class="en-US">URL protocol for an entry from a WebSphere jar file: "wsjar"</span>
	 * <span class="zh-CN">WebSphere jar 文件中条目的 URL 协议：“wsjar”</span>
	 */
	public static final String URL_PROTOCOL_WSJAR = "wsjar";

	/**
	 * <span class="en-US">URL protocol for an entry from an OC4J jar file: "code-source"</span>
	 * <span class="zh-CN">OC4J jar 文件中条目的 URL 协议：“code-source”</span>
	 */
	public static final String URL_PROTOCOL_CODE_SOURCE = "code-source";

	/**
	 * <span class="en-US">Separator between JAR URL and the path within the JAR</span>
	 * <span class="zh-CN">JAR URL 和 JAR 内路径之间的分隔符</span>
	 */
	public static final String JAR_URL_SEPARATOR = "!/";

	public static final String CONTENT_TYPE_OID_SIGNED_DATA = "1.2.840.113549.1.7.2";
	public static final String CONTENT_TYPE_OID_ENVELOPED_DATA = "1.2.840.113549.1.7.3";

	/**
	 * The constant BUFFER_SIZE.
	 */
	public static final int BUFFER_SIZE = 1024 * 4;
	/**
	 * The constant FILE_MODE_NONE.
	 */
	public static final int FILE_MODE_NONE = 0;
	/**
	 * The constant FILE_MODE_READ_ONLY.
	 */
	public static final int FILE_MODE_READ_ONLY = 1;
	/**
	 * The constant FOLDER_MODE_NONE.
	 */
	public static final int FOLDER_MODE_NONE = 16;
	/**
	 * The constant DEFAULT_ZIP_PAGE_SEPARATOR.
	 */
	public static final String DEFAULT_ZIP_PAGE_SEPARATOR = "/";
	/**
	 * The constant ZIP_ENTRY_SEPARATOR.
	 */
	public static final String DEFAULT_ZIP_ENTRY_SEPARATOR = ":" + Globals.DEFAULT_ZIP_PAGE_SEPARATOR;

	public static int random() {
		return random(Globals.DEFAULT_VALUE_INT);
	}

	public static int random(final int bound) {
		return bound > INITIALIZE_INT_VALUE ? RANDOM.get().nextInt(bound) : RANDOM.get().nextInt();
	}

	public static void randomBytes(final byte[] bytes) {
		RANDOM.get().nextBytes(bytes);
	}

	public static long randomLong() {
		return RANDOM.get().nextLong();
	}

	private Globals() {
	}
}
