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
import org.nervousync.enumerations.generator.UUIDLocalDomain;
import org.nervousync.generator.uuid.TimeBasedUUIDGenerator;
import org.nervousync.utils.IDUtils;
import org.nervousync.utils.SystemUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h2 class="en-US">UUID version 2 generator</h2>
 * <h2 class="zh-CN">UUID版本2生成器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 06, 2022 12:53:06 $
 */
@Provider(name = IDUtils.UUIDv2, titleKey = "version2.uuid.id.generator.name")
public final class UUIDv2Generator extends TimeBasedUUIDGenerator {

	private long localIdentifier;
	private byte localDomain;
	private final AtomicInteger generateCount = new AtomicInteger(Globals.INITIALIZE_INT_VALUE);

	/**
	 * Instantiates a new Uui dv 2 generator.
	 */
	public UUIDv2Generator() {
		//  Default local domain is PERSON
		this.config(UUIDLocalDomain.PERSON);
	}

	/**
	 * <h3 class="en-US">Configure current local domain</h3>
	 * <h3 class="zh-CN">修改当前本地域的配置</h3>
	 *
	 * @param localDomain <span class="en-US">Local domain of UUID version 2</span>
	 *                    <span class="zh-CN">UUID版本2的本地域</span>
	 */
	public void config(final UUIDLocalDomain localDomain) {
		switch (localDomain) {
			case PERSON:
				this.localDomain = (byte) 0;
				break;
			case GROUP:
				this.localDomain = (byte) 1;
				break;
			case ORG:
				this.localDomain = (byte) 2;
				break;
		}
		this.initialize();
	}

	private void initialize() {
		switch (this.localDomain) {
			case 0:
				this.localIdentifier = SystemUtils.UID();
				break;
			case 1:
				this.localIdentifier = SystemUtils.GID();
				break;
			case 2:
				this.localIdentifier = Globals.randomLong();
				break;
			default:
				this.localIdentifier = Globals.DEFAULT_VALUE_LONG;
				break;
		}
	}

	/**
	 * <h3 class="en-US">Calculate high bits</h3>
	 * <h3 class="zh-CN">计算高位值</h3>
	 *
	 * @return <span class="en-US">High bits value in long</span>
	 * <span class="zh-CN">long型的高位比特值</span>
	 */
	@Override
	protected long highBits(final long timestamp) {
		if (this.localIdentifier == Globals.DEFAULT_VALUE_LONG) {
			return Globals.DEFAULT_VALUE_LONG;
		}
		return ((this.localIdentifier & 0xFFFFFFFFL) << 32)
				| ((timestamp & 0xFFFF00000000L) >>> 16)
				| ((timestamp & 0xFFF000000000000L) >>> 48)
				| 0x2000L;  //  Apply version 2
	}

	/**
	 * <h3 class="en-US">Calculate low bits of given data bytes</h3>
	 * <h3 class="zh-CN">从给定的二进制数组计算低位值</h3>
	 *
	 * @return <span class="en-US">Low bits value in long</span>
	 * <span class="zh-CN">long型的低位比特值</span>
	 */
	@Override
	protected long lowBits(final long timestamp) {
		if (this.localIdentifier == Globals.DEFAULT_VALUE_LONG) {
			return Globals.DEFAULT_VALUE_LONG;
		}
		long nodeIdentifier = super.getNodeIdentifier();
		return (nodeIdentifier & 0xFFFFFFFFFFFFL)
				| ((this.localDomain & 0xFFL) << 48)
				| ((this.generateCount.incrementAndGet() & 0xFFL) << 56);
	}
}
