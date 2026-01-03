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
package org.nervousync.generator.uuid.impl;

import org.nervousync.annotations.provider.Provider;
import org.nervousync.commons.Globals;
import org.nervousync.generator.uuid.UUIDGenerator;
import org.nervousync.utils.IDUtils;
import org.nervousync.utils.RawUtils;
import org.nervousync.utils.SecurityUtils;

import java.util.UUID;

/**
 * <h2 class="en-US">UUID version 3 generator</h2>
 * <h2 class="zh-CN">UUID版本3生成器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 06, 2022 12:55:12 $
 */
@Deprecated(since = "1.2.4")
@SuppressWarnings("DeprecatedIsStillUsed")
@Provider(name = IDUtils.UUIDv3, titleKey = "version3.uuid.id.generator.name")
public final class UUIDv3Generator extends UUIDGenerator {

	/**
	 * <h3 class="en-US">Generate ID value</h3>
	 * <h3 class="zh-CN">生成ID值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的ID值</span>
	 */
	@Override
	public UUID generate() {
		byte[] dataBytes = new byte[16];
		Globals.randomBytes(dataBytes);
		return this.generate(dataBytes);
	}

	/**
	 * <h3 class="en-US">Generate ID value using given parameter</h3>
	 * <h3 class="zh-CN">使用给定的参数生成ID值</h3>
	 *
	 * @param dataBytes <span class="en-US">Given parameter</span>
	 *                  <span class="zh-CN">给定的参数</span>
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的ID值</span>
	 */
	@Override
	public UUID generate(final byte[] dataBytes) {
		byte[] randomBytes = SecurityUtils.MD5(dataBytes);
		/* clear version */
		randomBytes[6] &= 0x0F;
		/* set to version 3 */
		randomBytes[6] |= 0x30;
		/* clear variant */
		randomBytes[8] &= 0x3F;
		/* set to IETF variant */
		randomBytes[8] |= (byte) 0x80;
		return new UUID(RawUtils.readLong(randomBytes), RawUtils.readLong(randomBytes, 8));
	}
}
