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

package org.nervousync.generator.cuid;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.id.CUID;
import org.nervousync.generator.IGenerator;
import org.nervousync.utils.StringUtils;

import java.lang.management.ManagementFactory;

/**
 * <h2 class="en-US">Abstract CUID generator</h2>
 * <h2 class="zh-CN">CUID生成器抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: May 22, 2025 12:56:32 $
 */
public abstract class CUIDGenerator implements IGenerator<CUID> {

	protected static final int DEFAULT_RADIX = 36;

	protected static final String MACHINE_FINGERPRINT = fingerPrint();

	private static String fingerPrint() {
		final String machineName = ManagementFactory.getRuntimeMXBean().getName();
		final String[] tokenArray = StringUtils.tokenizeToStringArray(machineName, "@");
		final String processId = tokenArray[0];
		final String hostName = tokenArray[1];

		int acc = hostName.length() + DEFAULT_RADIX;
		for (int i = 0; i < hostName.length(); i += 1) {
			acc += hostName.charAt(i);
		}
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(processPadding(processId, 2));
		String nameBlock = Integer.toString(acc);
		stringBuilder.append(processPadding(nameBlock, 2));
		return stringBuilder.toString();
	}

	protected static String processPadding(@Nonnull final String value, final int length) {
		String result = value;
		StringBuilder padding = new StringBuilder();
		while ((result.length() + padding.length()) != length) {
			if (result.length() > length) {
				result = result.substring(1);
			} else {
				padding.append("0");
			}
		}
		padding.append(result);
		return padding.toString();
	}
}
