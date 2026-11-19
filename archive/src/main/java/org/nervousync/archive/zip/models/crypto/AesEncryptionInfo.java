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

package org.nervousync.archive.zip.models.crypto;

import org.nervousync.archive.enumerations.zip.AesVendor;
import org.nervousync.archive.exceptions.ArchiveException;

import java.util.Objects;

/**
 * <h2 class="en-US">AES encryption information</h2>
 * <h2 class="zh-CN">AES 加密信息</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class AesEncryptionInfo extends EncryptionInfo {

	/**
	 * <span class="en-US">Enumeration value of the AES vendor</span>
	 * <span class="zh-CN">AES 版本枚举值</span>
	 */
	private final AesVendor vendor;
	/**
	 * <span class="en-US">AES strength</span>
	 * <span class="zh-CN">AES 强度</span>
	 */
	private final int strength;

	/**
	 * <h3 class="en-US">Constructor method for the AES encryption information</h3>
	 * <h3 class="zh-CN">AES 加密信息的构造方法</h3>
	 *
	 * @param vendor   <span class="en-US">Enumeration value of the AES vendor</span>
	 *                 <span class="zh-CN">AES 版本枚举值</span>
	 * @param strength <span class="en-US">AES strength</span>
	 *                 <span class="zh-CN">AES 强度</span>
	 * @throws ArchiveException     <span class="en-US">AES strength invalid</span>
	 *                              <span class="zh-CN">AES 强度非法</span>
	 * @throws NullPointerException <span class="en-US">Enumeration value of the AES vendor is null</span>
	 *                              <span class="zh-CN">AES 版本枚举值为空</span>
	 */
	public AesEncryptionInfo(final AesVendor vendor, final int strength) throws ArchiveException {
		if (strength <= 0 || strength > 3) {
			throw new ArchiveException(0x0000001A0014L, strength);
		}
		this.vendor = Objects.requireNonNull(vendor, "vendor");
		this.strength = strength;
	}

	/**
	 * <h3 class="en-US">Getter method for the enumeration value of the AES vendor</h3>
	 * <h3 class="zh-CN">AES 版本枚举值的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">Enumeration value of the AES vendor</span>
	 * <span class="zh-CN">AES 版本枚举值</span>
	 */
	public AesVendor getVendor() {
		return this.vendor;
	}

	/**
	 * <h3 class="en-US">Getter method for the AES strength</h3>
	 * <h3 class="zh-CN">AES 强度的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">AES strength</span>
	 * <span class="zh-CN">AES 强度</span>
	 */
	public int getStrength() {
		return this.strength;
	}

	@Override
	public int hashCode() {
		return 31 * Integer.hashCode(this.vendor.getVersion()) + Integer.hashCode(this.strength);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AesEncryptionInfo)) {
			return false;
		}

		return (this.getVendor() == ((AesEncryptionInfo) obj).getVendor())
				&& (this.getStrength() == ((AesEncryptionInfo) obj).getStrength());
	}
}
