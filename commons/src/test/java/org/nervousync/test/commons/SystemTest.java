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

package org.nervousync.test.commons;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.beans.network.NetworkInfo;
import org.nervousync.exceptions.network.NetworkInfoException;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.commons.SystemUtils;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class SystemTest extends BaseTest {

	@Test
    @Order(0)
	public void systemInfo() {
		this.logger.info("System_Info",
				SystemUtils.OPERATE_SYSTEM_NAME, SystemUtils.OPERATE_SYSTEM_VERSION, SystemUtils.USER_NAME,
				SystemUtils.USER_HOME, SystemUtils.USER_DIR, SystemUtils.identifiedKey());
		this.logger.info("Operate_System", SystemUtils.isWindows(), SystemUtils.isUnix(), SystemUtils.isLinux(), SystemUtils.isMacOS(),
				SystemUtils.isMacOSX(), SystemUtils.isOS2(), SystemUtils.isSolaris(), SystemUtils.isSunOS(),
				SystemUtils.isMPEiX(), SystemUtils.isHPUX(), SystemUtils.isAIX(), SystemUtils.isOS390(),
				SystemUtils.isFreeBSD(), SystemUtils.isIrix(), SystemUtils.isDigitalUnix(), SystemUtils.isNetware(),
				SystemUtils.isOSF1(), SystemUtils.isOpenVMS());
		this.logger.info("System_Java_Info",
				SystemUtils.JDK_VERSION, SystemUtils.MAJOR_VERSION, SystemUtils.JAVA_HOME, SystemUtils.JAVA_TMP_DIR);
	}

	@Test
    @Order(10)
	public void networkInfo() throws SocketException, NetworkInfoException {
		Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
		do {
			NetworkInfo networkInfo = new NetworkInfo(enumeration.nextElement());
			this.logger.info("System_Adaptor", networkInfo.getDisplayName(), "IPv4");
			networkInfo.getIPv4AddressList().forEach(ipAddressInfo ->
					this.logger.info("System_Adaptor_IP_Info",
							ipAddressInfo.getIpAddress(), ipAddressInfo.isLocal(), ipAddressInfo.isLoop(),
							ipAddressInfo.isLinkLocal()));
			this.logger.info("System_Adaptor", networkInfo.getDisplayName(), "IPv6");
			networkInfo.getIPv6AddressList().forEach(ipAddressInfo ->
					this.logger.info("System_Adaptor_IP_Info",
							ipAddressInfo.getIpAddress(), ipAddressInfo.isLocal(), ipAddressInfo.isLoop(),
							ipAddressInfo.isLinkLocal()));
			this.logger.info("System_Adaptor", networkInfo.getDisplayName(), "IP");
			networkInfo.getIpAddressList().forEach(ipAddressInfo ->
					this.logger.info("System_Adaptor_IP_Info",
							ipAddressInfo.getIpAddress(), ipAddressInfo.isLocal(), ipAddressInfo.isLoop(),
							ipAddressInfo.isLinkLocal()));
		} while (enumeration.hasMoreElements());
	}
}
