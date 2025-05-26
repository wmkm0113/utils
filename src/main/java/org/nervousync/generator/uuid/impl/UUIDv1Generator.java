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
import org.nervousync.generator.uuid.TimeBasedClockSequenceGenerator;
import org.nervousync.utils.IDUtils;

/**
 * <h2 class="en-US">UUID version 1 generator</h2>
 * <h2 class="zh-CN">UUID版本1生成器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 06, 2022 12:51:08 $
 */
@Provider(name = IDUtils.UUIDv1, titleKey = "version1.uuid.id.generator.name")
public final class UUIDv1Generator extends TimeBasedClockSequenceGenerator {

	/**
	 * <h3 class="en-US">Calculate high bits</h3>
	 * <h3 class="zh-CN">计算高位值</h3>
	 *
	 * @return <span class="en-US">High bits value in long</span>
	 * <span class="zh-CN">long型的高位比特值</span>
	 */
	@Override
	protected long highBits(final long timestamp) {
		return ((timestamp & 0xFFFFFFFFL) << 32)
				| ((timestamp & 0xFFFF00000000L) >>> 16)
				| ((timestamp & 0xFFF000000000000L) >>> 48)
				| 0x1000L;  // Apply version 1
	}
}
