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
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.id.IDUtils;

import java.util.UUID;

/**
 * <h2 class="en-US">UUID version 6 generator</h2>
 * <h2 class="zh-CN">UUID版本6生成器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: May 26, 2025 12:28:16 $
 */
@Provider(name = IDUtils.UUIDv7, titleKey = "version7.uuid.id.generator.name")
public final class UUIDv7Generator extends UUIDGenerator {

	@Override
	public synchronized UUID generate() {
		long msb = (DateTimeUtils.currentUTCTimeMillis() & 0xFFFFFFFFFFFFL) << 16;
		msb |= (7L << 12);
		msb |= (Globals.randomLong() >>> 52);
		long lsb = Globals.randomLong();
		lsb &= 0x3FFFFFFFFFFFFFFFL;
		lsb |= 0x8000000000000000L;
		return new UUID(msb, lsb);
	}

	@Override
	public synchronized UUID generate(final byte[] dataBytes) {
		return this.generate();
	}
}
