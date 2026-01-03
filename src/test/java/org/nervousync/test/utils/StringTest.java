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

package org.nervousync.test.utils;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.RawUtils;
import org.nervousync.utils.StringUtils;

public final class StringTest extends BaseTest {

	private static final byte[] BYTE_ARRAY = new byte[8];

	static {
		RawUtils.writeLong(BYTE_ARRAY, 1303315200000L);
	}

	@Test
	@Order(10)
	public void base32() {
		String string = StringUtils.base32Encode(BYTE_ARRAY);
		this.logger.info("String_Encode", "Base32", string);
		byte[] decodeBytes = StringUtils.base32Decode(string);
		this.logger.info("String_Decode", "Base32", RawUtils.readLong(decodeBytes) == 1303315200000L);
	}

	@Test
	@Order(20)
	public void base64() {
		String string = StringUtils.base64Encode(BYTE_ARRAY);
		this.logger.info("String_Encode", "Base64", string);
		byte[] decodeBytes = StringUtils.base64Decode(string);
		this.logger.info("String_Decode", "Base64", RawUtils.readLong(decodeBytes) == 1303315200000L);
	}
}
