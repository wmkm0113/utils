package org.nervousync.test.core;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.beans.network.NetworkInfo;
import org.nervousync.exceptions.network.NetworkInfoException;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.SystemUtils;

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
