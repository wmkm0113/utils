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

package org.nervousync.utils.security;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.security.EncodeType;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.commons.ConvertUtils;
import org.nervousync.utils.commons.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * <h2 class="en-US">Abstract class of the security utilities</h2>
 * <h2 class="zh-CN">安全工具集抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public abstract class SecurityUtils {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	protected static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(SecurityUtils.class);

	/**
	 * <h3 class="en-US">Convert the binary data using the given encoding type.</h3>
	 * <h3 class="zh-CN">使用给定的编码类型对二进制数据进行转换</h3>
	 *
	 * @param dataBytes  <span class="en-US">Binary data array</span>
	 *                   <span class="zh-CN">二进制数据</span>
	 * @param encodeType <span class="en-US">String encoding type</span>
	 *                   <span class="zh-CN">字符串的编码类型</span>
	 * @return <span class="en-US">Converted string</span>
	 * <span class="zh-CN">转换后的字符串</span>
	 */
	protected static String encode(@Nonnull final byte[] dataBytes, @Nonnull final EncodeType encodeType) {
		if (dataBytes.length == 0) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		return switch (encodeType) {
			case BASE32 -> StringUtils.base32Encode(dataBytes);
			case BASE64 -> StringUtils.base64Encode(dataBytes);
			case HEX -> ConvertUtils.bytesToHex(dataBytes);
		};
	}

	/**
	 * <h3 class="en-US">Calculate the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象</h3>
	 *
	 * @param cryptoAdaptor <span class="en-US">Data operator instance object</span>
	 *                      <span class="zh-CN">数据操作器实例对象</span>
	 * @param source        <span class="en-US">source object</span>
	 *                      <span class="zh-CN">原始数据对象</span>
	 */
	protected static void process(@Nonnull final CryptoAdaptor cryptoAdaptor, @Nonnull final Object source) {
		if (source instanceof File) {
			try (InputStream inputStream = new FileInputStream((File) source)) {
				byte[] readBuffer = new byte[Globals.READ_FILE_BUFFER_SIZE];
				int readLength;
				while ((readLength = inputStream.read(readBuffer)) > 0) {
					cryptoAdaptor.append(readBuffer, 0, readLength);
				}
			} catch (Exception e) {
				LOGGER.error("Calculate_Digits_Security_Error", e);
			}
		} else {
			cryptoAdaptor.append(ConvertUtils.toByteArray(source));
		}
	}
}
