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

package org.nervousync.http.header;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.utils.StringUtils;

import java.util.Arrays;

/**
 * <h2 class="en-US">Parsed information of Content-Type from http response header</h2>
 * <h2 class="zh-CN">解析的响应头中Content-Type信息</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 4, 2018 12:15:18 $
 */
public final class ContentType {

	/**
	 * <span class="en-US">Charset encoding</span>
	 * <span class="zh-CN">字符集名称</span>
	 */
	@Nonnull
	private final String charsetEncoding;
	/**
	 * <span class="en-US">Data type</span>
	 * <span class="zh-CN">数据类型</span>
	 */
	@Nonnull
	private final StringUtils.StringType stringType;

	/**
	 * <h3 class="en-US">Private constructor method of the parsed information of Content-Type from http response header</h3>
	 * <h3 class="zh-CN">解析的响应头中Content-Type信息的私有构造方法</h3>
	 *
	 * @param charsetEncoding <span class="en-US">Charset encoding</span>
	 *                        <span class="zh-CN">字符集名称</span>
	 * @param stringType      <span class="en-US">Data type</span>
	 *                        <span class="zh-CN">数据类型</span>
	 */
	private ContentType(final String charsetEncoding, final StringUtils.StringType stringType) {
		this.charsetEncoding = StringUtils.isEmpty(charsetEncoding) ? Globals.DEFAULT_ENCODING : charsetEncoding;
		this.stringType = (stringType == null) ? StringUtils.StringType.SERIALIZABLE : stringType;
	}

	/**
	 * <h3 class="en-US">Static method for parse the Content-Type string from http response header</h3>
	 * <h3 class="zh-CN">静态方法用于解析响应头中的Content-Type字符串</h3>
	 *
	 * @param contentType <span class="en-US">Content-Type string</span>
	 *                    <span class="zh-CN">Content-Type字符串</span>
	 * @return <span class="en-US">Parsed information of Content-Type from http response header</span>
	 * <span class="zh-CN">解析的响应头中Content-Type信息</span>
	 */
	public static ContentType parse(final String contentType) {
		if (StringUtils.isEmpty(contentType)) {
			return new ContentType(Globals.DEFAULT_ENCODING, StringUtils.StringType.SERIALIZABLE);
		}
		String[] items = StringUtils.tokenizeToStringArray(contentType, ";");
		StringUtils.StringType stringType = StringUtils.StringType.SERIALIZABLE;
		if (items.length > 0) {
			String mimeType = items[0];
			String[] mimeItems = StringUtils.tokenizeToStringArray(mimeType, "/");
			if (mimeItems.length == 2) {
				if ("json".equalsIgnoreCase(mimeItems[1])) {
					stringType = StringUtils.StringType.JSON;
				} else if ("yaml".equalsIgnoreCase(mimeItems[1]) || "yml".equalsIgnoreCase(mimeItems[1])) {
					stringType = StringUtils.StringType.YAML;
				}
			}
		}
		String charsetEncoding =
				Arrays.stream(StringUtils.tokenizeToStringArray(contentType, ";"))
						.filter(string -> string.trim().toLowerCase().startsWith("charset="))
						.findFirst()
						.map(string -> string.substring("charset=".length()))
						.orElse(Globals.DEFAULT_ENCODING);
		return new ContentType(charsetEncoding, stringType);
	}

	/**
	 * <h3 class="en-US">Getter method for the charset encoding</h3>
	 * <h3 class="zh-CN">字符集名称的Getter方法</h3>
	 *
	 * @return <span class="en-US">Charset encoding</span>
	 * <span class="zh-CN">字符集名称</span>
	 */
	@Nonnull
	public String getCharsetEncoding() {
		return this.charsetEncoding;
	}

	/**
	 * <h3 class="en-US">Getter method for the data type</h3>
	 * <h3 class="zh-CN">数据类型的Getter方法</h3>
	 *
	 * @return <span class="en-US">Data type</span>
	 * <span class="zh-CN">数据类型</span>
	 */
	@Nonnull
	public StringUtils.StringType getStringType() {
		return this.stringType;
	}
}
