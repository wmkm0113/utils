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
import org.nervousync.beans.crypto.CRCConfig;
import org.nervousync.beans.crypto.CipherConfig;
import org.nervousync.beans.crypto.CipherKey;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.enumerations.security.EncodeType;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.security.SecurityAdaptor;
import org.nervousync.security.crc.CRCDigestAdapterImpl;
import org.nervousync.security.impl.DefaultSecurityAdaptorImpl;
import org.nervousync.utils.core.ConvertUtils;
import org.nervousync.utils.core.RawUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.security.*;
import java.util.*;

/**
 * <h2 class="en-US">Security utilities</h2>
 * <h2 class="zh-CN">安全工具集</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
@SuppressWarnings("unused")
public final class SecurityUtils {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(SecurityUtils.class);

	/**
	 * <span class="en-US">Security adaptor instance object</span>
	 * <span class="zh-CN">安全适配器实例对象</span>
	 */
	private static final SecurityAdaptor SECURITY_ADAPTOR;

	/**
	 * <span class="en-US">Registered CRC configure information</span>
	 * <span class="zh-CN">注册的CRC配置值映射表</span>
	 */
	private static final Map<String, CRCConfig> REGISTERED_CRC_CONFIG = new HashMap<>();

	static {
		SECURITY_ADAPTOR = ServiceLoader.load(SecurityAdaptor.class).findFirst().orElse(new DefaultSecurityAdaptorImpl());
		registerConfig("CRC-3/GSM",
				CRCConfig.newInstance(3, 0x3, 0x0, 0x7, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-3/ROHC",
				CRCConfig.newInstance(3, 0x3, 0x7, 0x0, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-4/G-704",
				CRCConfig.newInstance(4, 0x3, 0x0, 0x0, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-4/INTERLAKEN",
				CRCConfig.newInstance(4, 0x3, 0xF, 0xF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-5/EPC-C1G2",
				CRCConfig.newInstance(5, 0x09, 0x09, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-5/G-704",
				CRCConfig.newInstance(5, 0x15, 0x00, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-5/USB",
				CRCConfig.newInstance(5, 0x05, 0x1F, 0x1F, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-6/CDMA2000-A",
				CRCConfig.newInstance(6, 0x27, 0x3F, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-6/CDMA2000-B",
				CRCConfig.newInstance(6, 0x07, 0x3F, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-6/DARC",
				CRCConfig.newInstance(6, 0x19, 0x00, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-6/G-704",
				CRCConfig.newInstance(6, 0x03, 0x00, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-6/GSM",
				CRCConfig.newInstance(6, 0x2F, 0x00, 0x3F, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-7/MMC",
				CRCConfig.newInstance(7, 0x09, 0x00, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-7/ROHC",
				CRCConfig.newInstance(7, 0x4F, 0x7F, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-7/UMTS",
				CRCConfig.newInstance(7, 0x45, 0x00, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/AUTOSAR",
				CRCConfig.newInstance(8, 0x2F, 0xFF, 0xFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/BLUETOOTH",
				CRCConfig.newInstance(8, 0xA7, 0x00, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-8/CDMA2000",
				CRCConfig.newInstance(8, 0x9B, 0xFF, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/DARC",
				CRCConfig.newInstance(8, 0x39, 0x00, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-8/DVB-S2",
				CRCConfig.newInstance(8, 0xD5, 0x00, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/GSM-A",
				CRCConfig.newInstance(8, 0x1D, 0x00, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/GSM-B",
				CRCConfig.newInstance(8, 0x49, 0x00, 0xFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/I-432-1",
				CRCConfig.newInstance(8, 0x07, 0x00, 0x55, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/I-CODE",
				CRCConfig.newInstance(8, 0x1D, 0xFD, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/LTE",
				CRCConfig.newInstance(8, 0x9B, 0x00, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/MAXIM-DOW",
				CRCConfig.newInstance(8, 0x31, 0x00, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-8/MIFARE-MAD",
				CRCConfig.newInstance(8, 0x1D, 0xC7, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/NRSC-5",
				CRCConfig.newInstance(8, 0x31, 0xFF, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/OPENSAFETY",
				CRCConfig.newInstance(8, 0x2F, 0x00, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/ROHC",
				CRCConfig.newInstance(8, 0x07, 0xFF, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-8/SAE-J1850",
				CRCConfig.newInstance(8, 0x1D, 0xFF, 0xFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/SMBUS",
				CRCConfig.newInstance(8, 0x07, 0x00, 0x00, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-8/TECH-3250",
				CRCConfig.newInstance(8, 0x1D, 0xFF, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-8/WCDMA",
				CRCConfig.newInstance(8, 0x9B, 0x00, 0x00, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-10/ATM",
				CRCConfig.newInstance(10, 0x233, 0x000, 0x000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-10/CDMA2000",
				CRCConfig.newInstance(10, 0x3D9, 0x3FF, 0x000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-10/GSM",
				CRCConfig.newInstance(10, 0x175, 0x000, 0x3FF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-11/FLEXRAY",
				CRCConfig.newInstance(11, 0x385, 0x01A, 0x000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-11/UMTS",
				CRCConfig.newInstance(11, 0x307, 0x000, 0x000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-12/CDMA2000",
				CRCConfig.newInstance(12, 0xF13, 0xFFF, 0x000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-12/DECT",
				CRCConfig.newInstance(12, 0x80F, 0x000, 0x000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-12/GSM",
				CRCConfig.newInstance(12, 0xD31, 0x000, 0xFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-12/UMTS",
				CRCConfig.newInstance(12, 0x80F, 0x000, 0x000, Boolean.FALSE, Boolean.TRUE));
		registerConfig("CRC-13/BBC",
				CRCConfig.newInstance(13, 0x1CF5, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-14/DARC",
				CRCConfig.newInstance(14, 0x0805, 0x0000, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-14/GSM",
				CRCConfig.newInstance(14, 0x202D, 0x0000, 0x3FFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-15/CAN",
				CRCConfig.newInstance(15, 0x4599, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-15/MPT1327",
				CRCConfig.newInstance(15, 0x6815, 0x0000, 0x0001, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/ARC",
				CRCConfig.newInstance(16, 0x8005, 0x0000, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/CDMA2000",
				CRCConfig.newInstance(16, 0xC867, 0xFFFF, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/CMS",
				CRCConfig.newInstance(16, 0x8005, 0xFFFF, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/DDS-110",
				CRCConfig.newInstance(16, 0x8005, 0x800D, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/DECT-R",
				CRCConfig.newInstance(16, 0x0589, 0x0000, 0x0001, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/DECT-X",
				CRCConfig.newInstance(16, 0x0589, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/DNP",
				CRCConfig.newInstance(16, 0x3D65, 0x0000, 0xFFFF, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/EN-13757",
				CRCConfig.newInstance(16, 0x3D65, 0x0000, 0xFFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/GENIBUS",
				CRCConfig.newInstance(16, 0x1021, 0xFFFF, 0xFFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/GSM",
				CRCConfig.newInstance(16, 0x1021, 0x0000, 0xFFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/IBM-3740",
				CRCConfig.newInstance(16, 0x1021, 0xFFFF, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/IBM-SDLC",
				CRCConfig.newInstance(16, 0x1021, 0xFFFF, 0xFFFF, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/ISO-IEC-14443-3-A",
				CRCConfig.newInstance(16, 0x1021, 0xC6C6, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/KERMIT",
				CRCConfig.newInstance(16, 0x1021, 0x0000, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/LJ1200",
				CRCConfig.newInstance(16, 0x6F63, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/MAXIM-DOW",
				CRCConfig.newInstance(16, 0x8005, 0x0000, 0xFFFF, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/MCRF4XX",
				CRCConfig.newInstance(16, 0x1021, 0xFFFF, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/MODBUS",
				CRCConfig.newInstance(16, 0x8005, 0xFFFF, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/NRSC-5",
				CRCConfig.newInstance(16, 0x080B, 0xFFFF, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/OPENSAFETY-A",
				CRCConfig.newInstance(16, 0x5935, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/OPENSAFETY-B",
				CRCConfig.newInstance(16, 0x755B, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/PROFIBUS",
				CRCConfig.newInstance(16, 0x1DCF, 0xFFFF, 0xFFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/RIELLO",
				CRCConfig.newInstance(16, 0x1021, 0xB2AA, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/SPI-FUJITSU",
				CRCConfig.newInstance(16, 0x1021, 0x1D0F, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/T10-DIF",
				CRCConfig.newInstance(16, 0x8BB7, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/TELEDISK",
				CRCConfig.newInstance(16, 0xA097, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/TMS37157",
				CRCConfig.newInstance(16, 0x1021, 0x89EC, 0x0000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/UMTS",
				CRCConfig.newInstance(16, 0x8005, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-16/USB",
				CRCConfig.newInstance(16, 0x8005, 0xFFFF, 0xFFFF, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-16/XMODEM",
				CRCConfig.newInstance(16, 0x1021, 0x0000, 0x0000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-17/CAN-FD",
				CRCConfig.newInstance(17, 0x1685B, 0x00000, 0x00000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-21/CAN-FD",
				CRCConfig.newInstance(21, 0x102899, 0x000000, 0x000000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-24/BLE",
				CRCConfig.newInstance(24, 0x00065B, 0x555555, 0x000000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-24/FLEXRAY-A",
				CRCConfig.newInstance(24, 0x5D6DCB, 0xFEDCBA, 0x000000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-24/FLEXRAY-B",
				CRCConfig.newInstance(24, 0x5D6DCB, 0xABCDEF, 0x000000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-24/INTERLAKEN",
				CRCConfig.newInstance(24, 0x328B63, 0xFFFFFF, 0xFFFFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-24/LTE-A",
				CRCConfig.newInstance(24, 0x864CFB, 0x000000, 0x000000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-24/LTE-B",
				CRCConfig.newInstance(24, 0x800063, 0x000000, 0x000000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-24/OPENPGP",
				CRCConfig.newInstance(24, 0x864CFB, 0xB704CE, 0x000000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-24/OS-9",
				CRCConfig.newInstance(24, 0x800063, 0xFFFFFF, 0xFFFFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-30/CDMA",
				CRCConfig.newInstance(30, 0x2030B9C7, 0x3FFFFFFF, 0x3FFFFFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-31/PHILIPS",
				CRCConfig.newInstance(31, 0x04C11DB7, 0x7FFFFFFF, 0x7FFFFFFF, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-32/AIXM",
				CRCConfig.newInstance(32, 0x814141ABL, 0x00000000, 0x00000000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-32/AUTOSAR",
				CRCConfig.newInstance(32, 0xF4ACFB13L, 0xFFFFFFFFL, 0xFFFFFFFFL, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-32/BASE91-D",
				CRCConfig.newInstance(32, 0xA833982BL, 0xFFFFFFFFL, 0xFFFFFFFFL, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-32/BZIP2",
				CRCConfig.newInstance(32, 0x04C11DB7, 0xFFFFFFFFL, 0xFFFFFFFFL, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-32/CD-ROM-EDC",
				CRCConfig.newInstance(32, 0x8001801BL, 0x00000000, 0x00000000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-32/CKSUM",
				CRCConfig.newInstance(32, 0x04C11DB7, 0x00000000, 0xFFFFFFFFL, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-32/ISCSI",
				CRCConfig.newInstance(32, 0x1EDC6F41, 0xFFFFFFFFL, 0xFFFFFFFFL, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-32/ISO-HDLC",
				CRCConfig.newInstance(32, 0x04C11DB7, 0xFFFFFFFFL, 0xFFFFFFFFL, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-32/JAMCRC",
				CRCConfig.newInstance(32, 0x04C11DB7, 0xFFFFFFFFL, 0x00000000, Boolean.TRUE, Boolean.TRUE));
		registerConfig("CRC-32/MPEG-2",
				CRCConfig.newInstance(32, 0x04C11DB7, 0xFFFFFFFFL, 0x00000000, Boolean.FALSE, Boolean.FALSE));
		registerConfig("CRC-32/XFER",
				CRCConfig.newInstance(32, 0x000000AF, 0x00000000, 0x00000000, Boolean.FALSE, Boolean.FALSE));
		LOGGER.info("Registered_CRC_Algorithm",
				String.join(",", new ArrayList<>(REGISTERED_CRC_CONFIG.keySet())));
	}

	/**
	 * <h3 class="en-US">Register CRC configure information</h3>
	 * <h3 class="zh-CN">注册CRC配置信息</h3>
	 *
	 * @param algorithm <span class="en-US">Algorithm name</span>
	 *                  <span class="zh-CN">算法名称</span>
	 * @param crcConfig <span class="en-US">CRC configure information</span>
	 *                  <span class="zh-CN">CRC配置信息</span>
	 */
	public static void registerConfig(final String algorithm, final CRCConfig crcConfig) {
		if (StringUtils.isEmpty(algorithm) || crcConfig == null) {
			LOGGER.error("Parameter_Null_Register_Security_Error");
			return;
		}
		if (crcConfig.getBit() > 32) {
			LOGGER.error("Lager_CRC_Security_Error");
			return;
		}
		if (REGISTERED_CRC_CONFIG.containsKey(algorithm)) {
			LOGGER.warn("Override_Config_Register_Security_Warn", algorithm);
		}
		REGISTERED_CRC_CONFIG.put(algorithm, crcConfig);
	}

	/**
	 * <h3 class="en-US">Registered CRC algorithm name list</h3>
	 * <h3 class="zh-CN">已注册的CRC算法名列表</h3>
	 *
	 * @return <span class="en-US">CRC algorithm name list</span>
	 * <span class="zh-CN">CRC算法名列表</span>
	 */
	public static List<String> registeredCRC() {
		return new ArrayList<>(REGISTERED_CRC_CONFIG.keySet());
	}

	/**
	 * <h3 class="en-US">Initialize CRC secure provider</h3>
	 * <h3 class="zh-CN">初始化CRC安全适配器实例对象</h3>
	 *
	 * @param algorithm <span class="en-US">Algorithm name</span>
	 *                  <span class="zh-CN">算法名称</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If CRC algorithm didn't find</span>
	 *                         <span class="zh-CN">如果CRC算法未找到</span>
	 */
	public static CryptoAdaptor CRC(final String algorithm) throws CryptoException {
		if (REGISTERED_CRC_CONFIG.containsKey(algorithm)) {
			return new CRCDigestAdapterImpl(REGISTERED_CRC_CONFIG.get(algorithm));
		}
		throw new CryptoException(0x00000015000DL, algorithm);
	}

	/**
	 * <h3 class="en-US">Retrieve registered CRC configure information</h3>
	 * <h3 class="zh-CN">查找已注册的CRC配置信息</h3>
	 *
	 * @param algorithm <span class="en-US">Algorithm name</span>
	 *                  <span class="zh-CN">算法名称</span>
	 * @return <span class="en-US">CRC configure information instance or null if not found</span>
	 * <span class="zh-CN">找到的CRC配置信息实例对象，如果未找到返回<code>null</code></span>
	 */
	public static CRCConfig crcConfig(final String algorithm) {
		return REGISTERED_CRC_CONFIG.get(algorithm);
	}

	/**
	 * <h3 class="en-US">Convert crc result from byte arrays to string</h3>
	 * <h3 class="zh-CN">将CRC结果字节数组转换为字符串</h3>
	 *
	 * @param algorithm <span class="en-US">Algorithm name</span>
	 *                  <span class="zh-CN">算法名称</span>
	 * @param result    <span class="en-US">CRC result byte array</span>
	 *                  <span class="zh-CN">CRC结果字节数组</span>
	 * @return <span class="en-US">Converted string result</span>
	 * <span class="zh-CN">转换后的结果字符串</span>
	 * @throws CryptoException <span class="en-US">If CRC algorithm didn't find</span>
	 *                         <span class="zh-CN">如果CRC算法未找到</span>
	 */
	public static String CRCResult(final String algorithm, final byte[] result) throws DataInvalidException, CryptoException {
		long crc = RawUtils.readLong(result, ByteOrder.LITTLE_ENDIAN);
		return Optional.ofNullable(SecurityUtils.crcConfig(algorithm))
				.map(crcConfig -> {
					StringBuilder stringBuilder = new StringBuilder(Long.toString(crc, 16));
					while (stringBuilder.length() < crcConfig.getOutLength()) {
						stringBuilder.insert(0, "0");
					}
					return "0x" + stringBuilder;
				})
				.orElseThrow(() -> new CryptoException(0x00000015000DL, algorithm));
	}

	/**
	 * <h3 class="en-US">Calculate MD5 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的MD5值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String CRC(final String algorithm, final Object source) {
		CryptoAdaptor crcAdaptor = CRC(algorithm);
		SecurityUtils.process(crcAdaptor, source);
		long crc = RawUtils.readLong(crcAdaptor.finish(), ByteOrder.LITTLE_ENDIAN);
		return Optional.ofNullable(SecurityUtils.crcConfig(algorithm))
				.map(crcConfig -> {
					StringBuilder stringBuilder = new StringBuilder(Long.toString(crc, 16));
					while (stringBuilder.length() < crcConfig.getOutLength()) {
						stringBuilder.insert(0, "0");
					}
					return "0x" + stringBuilder;
				})
				.orElseThrow(() -> new CryptoException(0x00000015000DL, algorithm));
	}

	/*
	 * Digest Methods
	 */

	/**
	 * <h3 class="en-US">Initialize MD5 secure provider</h3>
	 * <h3 class="zh-CN">初始化MD5安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor MD5() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("MD5"), null);
	}

	/**
	 * <h3 class="en-US">Calculate MD5 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的MD5值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] MD5(final Object source) {
		return digest("MD5", source);
	}

	/**
	 * <h3 class="en-US">Calculate MD5 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的MD5值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String MD5(final Object source, @Nonnull final EncodeType encodeType) {
		return SecurityUtils.encode(MD5(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacMD5 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacMD5安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacMD5(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacMD5"), new CipherKey("HmacMD5", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacMD5 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacMD5值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacMD5(final byte[] keyBytes, final Object source) {
		return hmac("HmacMD5", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacMD5 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacMD5值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacMD5(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return SecurityUtils.encode(HmacMD5(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA1 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA1安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA1() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA1"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA1 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA1值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA1(final Object source) {
		return digest("SHA1", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA1 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA1值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA1(final Object source, @Nonnull final EncodeType encodeType) {
		return SecurityUtils.encode(SHA1(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA1 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA1安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA1(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA1"), new CipherKey("HmacSHA1", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA1 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA1值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA1(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA1", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA1 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA1值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA1(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return SecurityUtils.encode(HmacSHA1(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA-224 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA-224安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA224() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA-224"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA224值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA224(final Object source) {
		return digest("SHA-224", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA224值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA224(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA224(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA224 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA224安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA224(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA224"), new CipherKey("HmacSHA224", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA224值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA224(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA224", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA224值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA224(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA224(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA256 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA256安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA256() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA-256"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA256值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA256(final Object source) {
		return digest("SHA-256", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA256值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA256(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA256(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA256 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA256安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA256(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA256"), new CipherKey("HmacSHA256", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA256值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA256(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA256", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA256值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA256(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA256(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA384 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA384安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA384() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA-384"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA384 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA384值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA384(final Object source) {
		return digest("SHA-384", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA384 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA384值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA384(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA384(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA384 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA384安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA384(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA384"), new CipherKey("HmacSHA384", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA384 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA384值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA384(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA384", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA384 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA384值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA384(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA384(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA512 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA512安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA512() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA-512"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA512 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA512值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA512(final Object source) {
		return digest("SHA-512", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA512 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA512值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA512(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA512(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA512 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA512安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA512(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA512"), new CipherKey("HmacSHA512", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA512 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA512值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA512(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA512", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA512 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA512值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA512(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA512(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA512/224 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA512/224安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA512_224() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA-512/224"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA512/224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA512/224的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA512_224(final Object source) {
		return digest("SHA-512/224", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA512/224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA512/224的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA512_224(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA512_224(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA512/224 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA512/224安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA512_224(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA512/224"), new CipherKey("HmacSHA512/224", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA512/224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA512/224值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA512_224(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA512/224", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA512/224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA512/224值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA512_224(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA512_224(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA512/256 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA512/256安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA512_256() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA-512/256"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA512/256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA512/256的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA512_256(final Object source) {
		return digest("SHA-512/256", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA512/256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA512/256的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA512_256(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA512_256(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA512/256 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA512/256安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA512_256(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA512/256"), new CipherKey("HmacSHA512/256", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA512/256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA512/256值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA512_256(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA512/256", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA512/256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA512/256值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA512_256(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA512_256(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA3-224 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA3-224安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA3_224() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA3-224"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA3-224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA3-224的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA3_224(final Object source) {
		return digest("SHA3-224", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA3-224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA3-224的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA3_224(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA3_224(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA3-224 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA3-224安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA3_224(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA3-224"), new CipherKey("HmacSHA3-224", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA3-224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA3-224值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA3_224(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA3-224", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA3-224 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA3-224值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA3_224(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA3_224(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA3-256 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA3-256安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA3_256() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA3-256"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA3-256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA3-256的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA3_256(final Object source) {
		return digest("SHA3-256", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA3-256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA3-256的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA3_256(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA3_256(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA3-256 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA3-256安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA3_256(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA3-256"), new CipherKey("HmacSHA3-256", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA3-256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA3-256值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA3_256(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA3-256", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA3-256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA3-256值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA3_256(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA3_256(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA3-384 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA3-384安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA3_384() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA3-384"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA3-384 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA3-384的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA3_384(final Object source) {
		return digest("SHA3-384", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA3-384 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA3-384的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA3_384(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA3_384(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA3-384 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA3-384安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA3_384(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA3-384"), new CipherKey("HmacSHA3-384", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA3-384 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA3-384值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA3_384(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA3-384", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA3-384 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA3-384值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA3_384(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA3_384(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHA3-512 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHA3-512安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHA3_512() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHA3-512"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHA3-512 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA3-512的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHA3_512(final Object source) {
		return digest("SHA3-512", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHA3-512 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHA3-512的值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHA3_512(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHA3_512(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSHA3-512 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSHA3-512安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSHA3_512(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSHA3-512"), new CipherKey("HmacSHA3-512", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA3-512 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA3-512值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSHA3_512(final byte[] keyBytes, final Object source) {
		return hmac("HmacSHA3-512", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSHA3-512 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSHA3-512值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSHA3_512(final byte[] keyBytes, final Object source, @Nonnull final EncodeType encodeType) {
		return encode(HmacSHA3_512(keyBytes, source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHAKE128 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHAKE128安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHAKE128() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHAKE128"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHAKE128 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHAKE128值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHAKE128(final Object source) {
		return digest("SHAKE128", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHAKE128 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHAKE128值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHAKE128(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHAKE128(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SHAKE256 secure provider</h3>
	 * <h3 class="zh-CN">初始化SHAKE256安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SHAKE256() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SHAKE256"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SHAKE256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHAKE256值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SHAKE256(final Object source) {
		return digest("SHAKE256", source);
	}

	/**
	 * <h3 class="en-US">Calculate SHAKE256 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SHAKE256值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SHAKE256(final Object source, @Nonnull final EncodeType encodeType) {
		return encode(SHAKE256(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize SM3 secure provider</h3>
	 * <h3 class="zh-CN">初始化SM3安全适配器实例对象</h3>
	 *
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM3() throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("SM3"), null);
	}

	/**
	 * <h3 class="en-US">Calculate SM3 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SM3值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] SM3(final Object source) {
		return digest("SM3", source);
	}

	/**
	 * <h3 class="en-US">Calculate SM3 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的SM3值</h3>
	 *
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String SM3(final Object source, final EncodeType encodeType) {
		return encode(SM3(source), encodeType);
	}

	/**
	 * <h3 class="en-US">Initialize HmacSM3 secure provider</h3>
	 * <h3 class="zh-CN">初始化HmacSM3安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor HmacSM3(final byte[] keyBytes) throws CryptoException {
		return SECURITY_ADAPTOR.initDigest(new CipherConfig("HmacSM3"), new CipherKey("HmacSM3", keyBytes));
	}

	/**
	 * <h3 class="en-US">Calculate HmacSM3 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSM3值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static byte[] HmacSM3(final byte[] keyBytes, final Object source) {
		return hmac("HmacSM3", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Calculate HmacSM3 value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的HmacSM3值</h3>
	 *
	 * @param keyBytes <span class="en-US">HMAC key bytes</span>
	 *                 <span class="zh-CN">HMAC密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Calculate result or zero-length arrays if processes have error</span>
	 * <span class="zh-CN">计算结果，如果出现错误则返回长度为0的字节数组</span>
	 */
	public static String HmacSM3(final byte[] keyBytes, final Object source, final EncodeType encodeType) {
		return encode(HmacSM3(keyBytes, source), encodeType);
	}

	/*
	 *	Symmetric methods
	 */

	/**
	 * <h3 class="en-US">Initialize Blowfish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化Blowfish加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor BlowfishEncryptor(final byte[] keyBytes) throws CryptoException {
		return BlowfishEncryptor("CBC", "PKCS7Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize Blowfish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化Blowfish加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor BlowfishEncryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("Blowfish", mode, padding),
				new CipherKey("Blowfish", 56, keyBytes, Globals.DEFAULT_VALUE_STRING));
	}

	/**
	 * <h3 class="en-US">Initialize Blowfish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化Blowfish解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor BlowfishDecryptor(final byte[] keyBytes) throws CryptoException {
		return BlowfishDecryptor("CBC", "PKCS7Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize Blowfish encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化Blowfish解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor BlowfishDecryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("Blowfish", mode, padding),
				new CipherKey("Blowfish", 56, keyBytes, Globals.DEFAULT_VALUE_STRING));
	}

	/**
	 * <h3 class="en-US">Generate Blowfish key bytes</h3>
	 * <h3 class="zh-CN">生成Blowfish密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] BlowfishKey() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("Blowfish", 56, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform the Blowfish encryption operation</h3>
	 * <h3 class="zh-CN">执行 Blowfish 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] BlowfishEncrypt(final byte[] keyBytes, final Object source) {
		return BlowfishEncrypt("CBC", "PKCS7Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform the Blowfish encryption operation</h3>
	 * <h3 class="zh-CN">执行 Blowfish 加密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] BlowfishEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                     final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("Blowfish", mode, padding),
				new CipherKey("Blowfish", 56, keyBytes, Globals.DEFAULT_VALUE_STRING), source);
	}

	/**
	 * <h3 class="en-US">Perform the Blowfish decryption operation</h3>
	 * <h3 class="zh-CN">执行 Blowfish 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] BlowfishDecrypt(final byte[] keyBytes, final Object source) {
		return BlowfishDecrypt("CBC", "PKCS7Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform the Blowfish decryption operation</h3>
	 * <h3 class="zh-CN">执行 Blowfish 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] BlowfishDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                     final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("Blowfish", mode, padding),
				new CipherKey("Blowfish", 56, keyBytes, Globals.DEFAULT_VALUE_STRING), source);
	}

	/**
	 * <h3 class="en-US">Initialize DES encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化DES加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor DESEncryptor(final byte[] keyBytes) throws CryptoException {
		return DESEncryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize DES encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化DES加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor DESEncryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("DES", mode, padding),
				new CipherKey("DES", keyBytes));
	}

	/**
	 * <h3 class="en-US">Initialize DES decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化DES解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor DESDecryptor(final byte[] keyBytes) throws CryptoException {
		return DESDecryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize DES decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化DES解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor DESDecryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("DES", mode, padding),
				new CipherKey("DES", keyBytes));
	}

	/**
	 * <h3 class="en-US">Generate DES key bytes</h3>
	 * <h3 class="zh-CN">生成DES密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] DESKey() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("DES", Globals.DEFAULT_VALUE_INT, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform DES encryption operation</h3>
	 * <h3 class="zh-CN">执行 DES 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] DESEncrypt(final byte[] keyBytes, final Object source) {
		return DESEncrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform DES encryption operation</h3>
	 * <h3 class="zh-CN">执行 DES 加密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] DESEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("DES", mode, padding),
				new CipherKey("DES", keyBytes), source);
	}

	/**
	 * <h3 class="en-US">Perform DES decryption operation</h3>
	 * <h3 class="zh-CN">执行 DES 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] DESDecrypt(final byte[] keyBytes, final Object source) {
		return DESDecrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform DES decryption operation</h3>
	 * <h3 class="zh-CN">执行 DES 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] DESDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("DES", mode, padding),
				new CipherKey("DES", keyBytes), source);
	}

	/**
	 * <h3 class="en-US">Initialize TripleDES encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">初始化3DES加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TripleDESEncryptor(final byte[] keyBytes) throws CryptoException {
		return TripleDESEncryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize TripleDES encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">初始化3DES加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TripleDESEncryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("DESede", mode, padding),
				new CipherKey("DESede", keyBytes));
	}

	/**
	 * <h3 class="en-US">Initialize TripleDES decryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">初始化3DES解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TripleDESDecryptor(final byte[] keyBytes) throws CryptoException {
		return TripleDESDecryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize TripleDES decryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">初始化3DES解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor TripleDESDecryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("DESede", mode, padding),
				new CipherKey("DESede", keyBytes));
	}

	/**
	 * <h3 class="en-US">Generate TripleDES key bytes</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">生成3DES密钥字节数组</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] TripleDESKey() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("DESede", Globals.DEFAULT_VALUE_INT, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform the TripleDES encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">执行 TripleDES 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] TripleDESEncrypt(final byte[] keyBytes, final Object source) {
		return TripleDESEncrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform the TripleDES encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">执行 TripleDES 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] TripleDESEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                      final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("DESede", mode, padding),
				new CipherKey("DESede", keyBytes), source);
	}

	/**
	 * <h3 class="en-US">Perform the TripleDES decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">执行 TripleDES 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] TripleDESDecrypt(final byte[] keyBytes, final Object source) {
		return TripleDESDecrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform the TripleDES decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES or SM4 instead</span>
	 * <h3 class="zh-CN">执行 TripleDES 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 或 SM4 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] TripleDESDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                      final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("DESede", mode, padding),
				new CipherKey("DESede", keyBytes), source);
	}

	/**
	 * <h3 class="en-US">Initialize SM4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Encryptor(final byte[] keyBytes) throws CryptoException {
		return SM4Encryptor("CBC", "PKCS5Padding", keyBytes, "SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Initialize SM4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Encryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SM4Encryptor(mode, padding, keyBytes, "SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Initialize SM4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode            <span class="en-US">Cipher Mode</span>
	 *                        <span class="zh-CN">分组密码模式</span>
	 * @param padding         <span class="en-US">Padding Mode</span>
	 *                        <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Encryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final String randomAlgorithm) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("SM4", mode, padding),
				new CipherKey("SM4", 128, keyBytes, randomAlgorithm, "BC"));
	}

	/**
	 * <h3 class="en-US">Initialize SM4 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Decryptor(final byte[] keyBytes) throws CryptoException {
		return SM4Decryptor("CBC", "PKCS5Padding", keyBytes, "SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Initialize SM4 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Decryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SM4Decryptor(mode, padding, keyBytes, "SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Initialize SM4 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM4解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode            <span class="en-US">Cipher Mode</span>
	 *                        <span class="zh-CN">分组密码模式</span>
	 * @param padding         <span class="en-US">Padding Mode</span>
	 *                        <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM4Decryptor(final String mode, final String padding, final byte[] keyBytes,
	                                         final String randomAlgorithm) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("SM4", mode, padding),
				new CipherKey("SM4", 128, keyBytes, randomAlgorithm, "BC"));
	}

	/**
	 * <h3 class="en-US">Generate SM4 key bytes</h3>
	 * <h3 class="zh-CN">生成SM4密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] SM4Key() {
		return SM4Key("SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Generate SM4 key bytes</h3>
	 * <h3 class="zh-CN">生成SM4密钥字节数组</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] SM4Key(final String randomAlgorithm) {
		try {
			return SECURITY_ADAPTOR.symmetricKey("SM4", 128, randomAlgorithm);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform SM4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Encrypt(final byte[] keyBytes, final Object source) {
		return SM4Encrypt(keyBytes, "SHA1PRNG", source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Encrypt(final byte[] keyBytes, final String randomAlgorithm, final Object source) {
		return SM4Encrypt("CBC", "PKCS5Padding", keyBytes, randomAlgorithm, source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 加密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Encrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final String randomAlgorithm, final Object source) {
		return process(CryptoMode.ENCRYPT,
				new CipherConfig("SM4", mode, padding),
				new CipherKey("SM4", 128, keyBytes, randomAlgorithm, "BC"),
				source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Decrypt(final byte[] keyBytes, final Object source) {
		return SM4Decrypt(keyBytes, "SHA1PRNG", source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Decrypt(final byte[] keyBytes, final String randomAlgorithm, final Object source) {
		return SM4Decrypt("CBC", "PKCS5Padding", keyBytes, randomAlgorithm, source);
	}

	/**
	 * <h3 class="en-US">Perform SM4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 SM4 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] SM4Decrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final String randomAlgorithm, final Object source) {
		return process(CryptoMode.DECRYPT,
				new CipherConfig("SM4", mode, padding),
				new CipherKey("SM4", 128, keyBytes, randomAlgorithm, "BC"),
				source);
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Encryptor(final byte[] keyBytes) throws CryptoException {
		return RC2Encryptor("CBC", "PKCS7Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Encryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING));
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Decryptor(final byte[] keyBytes) throws CryptoException {
		return RC2Decryptor("CBC", "PKCS7Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize RC2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC2解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC2Decryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING));
	}

	/**
	 * <h3 class="en-US">Generate RC2 key bytes</h3>
	 * <h3 class="zh-CN">生成RC2密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] RC2Key() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("RC2", 128, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform RC2 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC2 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC2Encrypt(final byte[] keyBytes, final Object source) {
		return RC2Encrypt("CBC", "PKCS7Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform RC2 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC2 加密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC2Encrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING), source);
	}

	/**
	 * <h3 class="en-US">Perform RC2 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC2 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC2Decrypt(final byte[] keyBytes, final Object source) {
		return RC2Decrypt("CBC", "PKCS7Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform RC2 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC2 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC2Decrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("RC2", mode, padding),
				new CipherKey("RC2", 128, keyBytes, Globals.DEFAULT_VALUE_STRING), source);
	}

	/**
	 * <h3 class="en-US">Initialize RC4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC4加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC4Encryptor(final byte[] keyBytes) throws CryptoException {
		return RC4Encryptor(keyBytes, "SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Initialize RC4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC4加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC4Encryptor(final byte[] keyBytes, final String randomAlgorithm)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RC4", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RC4", 128, keyBytes, randomAlgorithm, "BC"));
	}

	/**
	 * <h3 class="en-US">Initialize RC4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC4解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC4Decryptor(final byte[] keyBytes) throws CryptoException {
		return RC4Decryptor(keyBytes, "SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Initialize RC4 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RC4解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC4Decryptor(final byte[] keyBytes, final String randomAlgorithm)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RC4", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RC4", 128, keyBytes, randomAlgorithm, "BC"));
	}

	/**
	 * <h3 class="en-US">Generate RC4 key bytes</h3>
	 * <h3 class="zh-CN">生成RC4密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] RC4Key() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("RC4", 128, "SHA1PRNG");
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform RC4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC4 加密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC4Encrypt(final byte[] keyBytes, final Object source) {
		return RC4Encrypt(keyBytes, "SHA1PRNG", source);
	}

	/**
	 * <h3 class="en-US">Perform RC4 encryption operation</h3>
	 * <h3 class="zh-CN">执行 RC4 加密操作</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param source          <span class="en-US">source object</span>
	 *                        <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC4Encrypt(final byte[] keyBytes, final String randomAlgorithm, final Object source) {
		return process(CryptoMode.ENCRYPT,
				new CipherConfig("RC4", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RC4", 128, keyBytes, randomAlgorithm, "BC"),
				source);
	}

	/**
	 * <h3 class="en-US">Perform RC4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC4 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC4Decrypt(final byte[] keyBytes, final Object source) {
		return RC4Decrypt(keyBytes, "SHA1PRNG", source);
	}

	/**
	 * <h3 class="en-US">Perform RC4 decryption operation</h3>
	 * <h3 class="zh-CN">执行 RC4 解密操作</h3>
	 *
	 * @param keyBytes        <span class="en-US">key bytes</span>
	 *                        <span class="zh-CN">密钥字节数组</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @param source          <span class="en-US">source object</span>
	 *                        <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC4Decrypt(final byte[] keyBytes, final String randomAlgorithm, final Object source) {
		return process(CryptoMode.DECRYPT,
				new CipherConfig("RC4", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RC4", 128, keyBytes, randomAlgorithm, "BC"),
				source);
	}

	/**
	 * <h3 class="en-US">Initialize RC5 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC5加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC5Encryptor(final byte[] keyBytes) throws CryptoException {
		return RC5Encryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize RC5 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC5加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC5Encryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RC5", mode, padding),
				new CipherKey("RC5", 128, keyBytes, Globals.DEFAULT_VALUE_STRING));
	}

	/**
	 * <h3 class="en-US">Initialize RC5 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC5解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC5Decryptor(final byte[] keyBytes) throws CryptoException {
		return RC5Decryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize RC5 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC5解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC5Decryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RC5", mode, padding),
				new CipherKey("RC5", 128, keyBytes, Globals.DEFAULT_VALUE_STRING));
	}

	/**
	 * <h3 class="en-US">Generate RC5 key bytes</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">生成RC5密钥字节数组</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] RC5Key() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("RC5", 128, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform RC5 encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC5 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC5Encrypt(final byte[] keyBytes, final Object source) {
		return RC5Encrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform RC5 encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC5 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC5Encrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("RC5", mode, padding),
				new CipherKey("RC5", 128, keyBytes, Globals.DEFAULT_VALUE_STRING), source);
	}

	/**
	 * <h3 class="en-US">Perform RC5 decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC5 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC5Decrypt(final byte[] keyBytes, final Object source) {
		return RC5Decrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform RC5 decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC5 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC5Decrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("RC5", mode, padding),
				new CipherKey("RC5", 128, keyBytes, Globals.DEFAULT_VALUE_STRING), source);
	}

	/**
	 * <h3 class="en-US">Initialize RC6 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC6加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC6Encryptor(final byte[] keyBytes) throws CryptoException {
		return RC6Encryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize RC6 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC6加密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC6Encryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RC6", mode, padding),
				new CipherKey("RC6", 128, keyBytes, Globals.DEFAULT_VALUE_STRING));
	}

	/**
	 * <h3 class="en-US">Initialize RC6 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC6解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC6Decryptor(final byte[] keyBytes) throws CryptoException {
		return RC6Decryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize RC6 encryptor secure provider</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">初始化RC6解密安全适配器实例对象</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RC6Decryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RC6", mode, padding),
				new CipherKey("RC6", 128, keyBytes, Globals.DEFAULT_VALUE_STRING));
	}

	/**
	 * <h3 class="en-US">Generate RC6 key bytes</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">生成RC6密钥字节数组</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] RC6Key() {
		try {
			return SECURITY_ADAPTOR.symmetricKey("RC6", 128, Globals.DEFAULT_VALUE_STRING);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform RC6 encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC6 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC6Encrypt(final byte[] keyBytes, final Object source) {
		return RC6Encrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform RC6 encryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC6 加密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC6Encrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("RC6", mode, padding),
				new CipherKey("RC6", 128, keyBytes, Globals.DEFAULT_VALUE_STRING), source);
	}

	/**
	 * <h3 class="en-US">Perform RC6 decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC6 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC6Decrypt(final byte[] keyBytes, final Object source) {
		return RC6Decrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform RC6 decryption operation</h3>
	 * <span class="en-US">Since 1.4.0, using AES instead</span>
	 * <h3 class="zh-CN">执行 RC6 解密操作</h3>
	 * <span class="zh-CN">从 1.4.0 版本开始废弃，使用 AES 代替</span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] RC6Decrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("RC6", mode, padding),
				new CipherKey("RC6", 128, keyBytes, Globals.DEFAULT_VALUE_STRING), source);
	}

	/**
	 * <h3 class="en-US">Initialize AES encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化AES加密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor AESEncryptor(final byte[] keyBytes) throws CryptoException {
		return AESEncryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize AES encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化AES加密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor AESEncryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("AES", mode, padding),
				new CipherKey("AES", keyBytes));
	}

	/**
	 * <h3 class="en-US">Initialize AES decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化AES解密安全适配器实例对象</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor AESDecryptor(final byte[] keyBytes) throws CryptoException {
		return AESDecryptor("CBC", "PKCS5Padding", keyBytes);
	}

	/**
	 * <h3 class="en-US">Initialize AES decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化AES解密安全适配器实例对象</h3>
	 * <span>
	 * mode: "ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"
	 * padding: "PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"
	 * </span>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor AESDecryptor(final String mode, final String padding, final byte[] keyBytes)
			throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("AES", mode, padding),
				new CipherKey("AES", keyBytes));
	}

	/**
	 * <h3 class="en-US">Generate AES128 key bytes</h3>
	 * <h3 class="zh-CN">生成AES128密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES128Key() {
		return AES128Key("SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Generate AES128 key bytes</h3>
	 * <h3 class="zh-CN">生成AES128密钥字节数组</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES128Key(final String randomAlgorithm) {
		try {
			return SECURITY_ADAPTOR.symmetricKey("AES", 128, randomAlgorithm);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Generate AES192 key bytes</h3>
	 * <h3 class="zh-CN">生成AES192密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES192Key() {
		return AES192Key("SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Generate AES192 key bytes</h3>
	 * <h3 class="zh-CN">生成AES192密钥字节数组</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES192Key(final String randomAlgorithm) {
		try {
			return SECURITY_ADAPTOR.symmetricKey("AES", 192, randomAlgorithm);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Generate AES256 key bytes</h3>
	 * <h3 class="zh-CN">生成AES256密钥字节数组</h3>
	 *
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES256Key() {
		return AES256Key("SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Generate AES256 key bytes</h3>
	 * <h3 class="zh-CN">生成AES256密钥字节数组</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated key bytes or zero length byte array if process error</span>
	 * <span class="zh-CN">生成的密钥字节数组，如果出现异常则返回长度为0的字节数组</span>
	 */
	public static byte[] AES256Key(final String randomAlgorithm) {
		try {
			return SECURITY_ADAPTOR.symmetricKey("AES", 256, randomAlgorithm);
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESEncrypt(final byte[] keyBytes, final Object source) {
		return AESEncrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESEncrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.ENCRYPT, new CipherConfig("AES", mode, padding),
				new CipherKey("AES", keyBytes), source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESDecrypt(final byte[] keyBytes, final Object source) {
		return AESDecrypt("CBC", "PKCS5Padding", keyBytes, source);
	}

	/**
	 * <h3 class="en-US">Perform AES decryption operation</h3>
	 * <h3 class="zh-CN">执行 AES 解密操作</h3>
	 *
	 * @param mode     <span class="en-US">Cipher Mode</span>
	 *                 <span class="zh-CN">分组密码模式</span>
	 * @param padding  <span class="en-US">Padding Mode</span>
	 *                 <span class="zh-CN">数据填充模式</span>
	 * @param keyBytes <span class="en-US">key bytes</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	public static byte[] AESDecrypt(final String mode, final String padding, final byte[] keyBytes,
	                                final Object source) {
		return process(CryptoMode.DECRYPT, new CipherConfig("AES", mode, padding),
				new CipherKey("AES", keyBytes), source);
	}

	/*
	 * Asymmetric methods
	 */

	/**
	 * <h3 class="en-US">Initialize RSA encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA加密安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSAEncryptor(final Key publicKey) throws CryptoException {
		return RSAEncryptor("PKCS1Padding", publicKey);
	}

	/**
	 * <h3 class="en-US">Initialize RSA encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA加密安全适配器实例对象</h3>
	 * <span>
	 * padding:  "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding",
	 * "OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA-256AndMGF1Padding",
	 * "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
	 * "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding",
	 * "OAEPWithSHA3-384AndMGF1Padding", "OAEPWithSHA3-512AndMGF1Padding"
	 * </span>
	 *
	 * @param padding   <span class="en-US">Padding Mode</span>
	 *                  <span class="zh-CN">数据填充模式</span>
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSAEncryptor(final String padding, final Key publicKey) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("RSA", "None", padding),
				new CipherKey("RSA", publicKey.getEncoded()));
	}

	/**
	 * <h3 class="en-US">Initialize RSA decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA解密安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSADecryptor(final Key privateKey) throws CryptoException {
		return RSADecryptor("PKCS1Padding", privateKey);
	}

	/**
	 * <h3 class="en-US">Initialize RSA decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA解密安全适配器实例对象</h3>
	 * <span>
	 * padding:  "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding",
	 * "OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA-256AndMGF1Padding",
	 * "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
	 * "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding",
	 * "OAEPWithSHA3-384AndMGF1Padding", "OAEPWithSHA3-512AndMGF1Padding"
	 * </span>
	 *
	 * @param padding    <span class="en-US">Padding Mode</span>
	 *                   <span class="zh-CN">数据填充模式</span>
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSADecryptor(final String padding, final Key privateKey) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("RSA", "None", padding),
				new CipherKey("RSA", privateKey.getEncoded()));
	}

	/**
	 * <h3 class="en-US">Initialize RSA signer secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA签名安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSASigner(final PrivateKey privateKey) throws CryptoException {
		return RSASigner("SHA256withRSA", privateKey);
	}

	/**
	 * <h3 class="en-US">Initialize RSA signer secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA签名安全适配器实例对象</h3>
	 *
	 * @param algorithm  <span class="en-US">Signature algorithm name</span>
	 *                   <span class="zh-CN">签名算法名称</span>
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSASigner(final String algorithm, final PrivateKey privateKey) throws CryptoException {
		return SECURITY_ADAPTOR.signer(new CipherConfig(algorithm, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RSA", privateKey.getEncoded()));
	}

	/**
	 * <h3 class="en-US">Initialize RSA signature verifier secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA签名验证安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSAVerifier(final PublicKey publicKey) throws CryptoException {
		return RSAVerifier("SHA256withRSA", publicKey);
	}

	/**
	 * <h3 class="en-US">Initialize RSA signature verifier secure provider</h3>
	 * <h3 class="zh-CN">初始化RSA签名验证安全适配器实例对象</h3>
	 *
	 * @param algorithm <span class="en-US">Signature algorithm name</span>
	 *                  <span class="zh-CN">签名算法名称</span>
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor RSAVerifier(final String algorithm, final PublicKey publicKey) throws CryptoException {
		return SECURITY_ADAPTOR.verifier(new CipherConfig(algorithm, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RSA", publicKey.getEncoded()));
	}

	/**
	 * <h3 class="en-US">Generate RSA KeyPair</h3>
	 * <h3 class="zh-CN">生成RSA密钥对</h3>
	 *
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair RSAKeyPair() {
		return RSAKeyPair(2048);
	}

	/**
	 * <h3 class="en-US">Generate RSA KeyPair</h3>
	 * <h3 class="zh-CN">生成RSA密钥对</h3>
	 *
	 * @param keySize <span class="en-US">Key size</span>
	 *                <span class="zh-CN">密钥长度</span>
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair RSAKeyPair(final int keySize) {
		return RSAKeyPair(keySize, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Generate RSA KeyPair</h3>
	 * <h3 class="zh-CN">生成RSA密钥对</h3>
	 *
	 * @param keySize         <span class="en-US">Key size</span>
	 *                        <span class="zh-CN">密钥长度</span>
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair RSAKeyPair(final int keySize, final String randomAlgorithm) {
		return SECURITY_ADAPTOR.keyPair("RSA", keySize, randomAlgorithm, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Encrypt the data using the given private key.</h3>
	 * <h3 class="zh-CN">使用给定的私钥对数据进行加密操作</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSAEncrypt(final Key publicKey, final Object source)
			throws CryptoException {
		return RSAEncrypt("PKCS1Padding", publicKey, source);
	}

	/**
	 * <h3 class="en-US">Encrypt the data using the given private key.</h3>
	 * <h3 class="zh-CN">使用给定的私钥对数据进行加密操作</h3>
	 * <span>
	 * padding:  "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding",
	 * "OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA-256AndMGF1Padding",
	 * "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
	 * "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding",
	 * "OAEPWithSHA3-384AndMGF1Padding", "OAEPWithSHA3-512AndMGF1Padding"
	 * </span>
	 *
	 * @param padding   <span class="en-US">Padding Mode</span>
	 *                  <span class="zh-CN">数据填充模式</span>
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSAEncrypt(final String padding, final Key publicKey, final Object source)
			throws CryptoException {
		return process(CryptoMode.ENCRYPT,
				new CipherConfig("RSA", "None", padding),
				new CipherKey("RSA", publicKey.getEncoded()),
				source);
	}

	/**
	 * <h3 class="en-US">Decrypt the data using the given private key.</h3>
	 * <h3 class="zh-CN">使用给定的私钥对数据进行解密操作</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSADecrypt(final Key privateKey, final Object source) throws CryptoException {
		return RSADecrypt("PKCS1Padding", privateKey, source);
	}

	/**
	 * <h3 class="en-US">Decrypt the data using the given private key.</h3>
	 * <h3 class="zh-CN">使用给定的私钥对数据进行解密操作</h3>
	 * <span>
	 * padding:  "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding",
	 * "OAEPWithSHA-224AndMGF1Padding", "OAEPWithSHA-256AndMGF1Padding",
	 * "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
	 * "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding",
	 * "OAEPWithSHA3-384AndMGF1Padding", "OAEPWithSHA3-512AndMGF1Padding"
	 * </span>
	 *
	 * @param padding    <span class="en-US">Padding Mode</span>
	 *                   <span class="zh-CN">数据填充模式</span>
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSADecrypt(final String padding, final Key privateKey, final Object source)
			throws CryptoException {
		return process(CryptoMode.DECRYPT,
				new CipherConfig("RSA", "None", padding),
				new CipherKey("RSA", privateKey.getEncoded()),
				source);
	}

	/**
	 * <h3 class="en-US">Sign the data using the given RSA algorithm.</h3>
	 * <h3 class="zh-CN">使用给定 RSA 算法对数据进行签名</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSASign(final PrivateKey privateKey, final Object source) throws CryptoException {
		return RSASign("SHA256withRSA", privateKey, source);
	}

	/**
	 * <h3 class="en-US">Sign the data using the given RSA algorithm.</h3>
	 * <h3 class="zh-CN">使用给定 RSA 算法对数据进行签名</h3>
	 *
	 * @param algorithm  <span class="en-US">Signature algorithm name</span>
	 *                   <span class="zh-CN">签名算法名称</span>
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] RSASign(final String algorithm, final PrivateKey privateKey, final Object source)
			throws CryptoException {
		return process(CryptoMode.SIGNATURE,
				new CipherConfig(algorithm, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RSA", privateKey.getEncoded()),
				source);
	}

	/**
	 * <h3 class="en-US">Verify RSA signature</h3>
	 * <h3 class="zh-CN">验证 RSA 签名</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @param signature <span class="en-US">Signature data</span>
	 *                  <span class="zh-CN">签名数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static boolean RSAVerify(final PublicKey publicKey, final Object source, final byte[] signature)
			throws CryptoException {
		return RSAVerify("SHA256withRSA", publicKey, source, signature);
	}

	/**
	 * <h3 class="en-US">Verify RSA signature</h3>
	 * <h3 class="zh-CN">验证 RSA 签名</h3>
	 *
	 * @param algorithm <span class="en-US">Signature algorithm name</span>
	 *                  <span class="zh-CN">签名算法名称</span>
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @param signature <span class="en-US">Signature data</span>
	 *                  <span class="zh-CN">签名数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static boolean RSAVerify(final String algorithm, final PublicKey publicKey,
	                                final Object source, final byte[] signature) throws CryptoException {
		return verify(
				new CipherConfig(algorithm, Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("RSA", publicKey.getEncoded()),
				source, signature);
	}

	/**
	 * <h3 class="en-US">Initialize SM2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2加密安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM2Encryptor(final PublicKey publicKey) throws CryptoException {
		return SECURITY_ADAPTOR.encryptor(new CipherConfig("SM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", publicKey.getEncoded(), "BC"));
	}

	/**
	 * <h3 class="en-US">Initialize SM2 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2解密安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM2Decryptor(final PrivateKey privateKey) throws CryptoException {
		return SECURITY_ADAPTOR.decryptor(new CipherConfig("SM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", privateKey.getEncoded(), "BC"));
	}

	/**
	 * <h3 class="en-US">Initialize SM2 signer secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2签名安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM2Signer(final PrivateKey privateKey) throws CryptoException {
		return SECURITY_ADAPTOR.signer(new CipherConfig("SM3withSM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", privateKey.getEncoded(), "BC"));
	}

	/**
	 * <h3 class="en-US">Initialize SM2 signature verifier secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2签名验证安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static CryptoAdaptor SM2Verifier(final PublicKey publicKey) throws CryptoException {
		return SECURITY_ADAPTOR.verifier(new CipherConfig("SM3withSM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", publicKey.getEncoded(), "BC"));
	}

	/**
	 * <h3 class="en-US">Generate SM2 KeyPair</h3>
	 * <h3 class="zh-CN">生成SM2密钥对</h3>
	 *
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair SM2KeyPair() {
		return SM2KeyPair("SHA1PRNG");
	}

	/**
	 * <h3 class="en-US">Generate SM2 KeyPair</h3>
	 * <h3 class="zh-CN">生成SM2密钥对</h3>
	 *
	 * @param randomAlgorithm <span class="en-US">Random algorithm</span>
	 *                        <span class="zh-CN">随机数算法</span>
	 * @return <span class="en-US">Generated keypair</span>
	 * <span class="zh-CN">生成的密钥对</span>
	 */
	public static KeyPair SM2KeyPair(final String randomAlgorithm) {
		return SECURITY_ADAPTOR.keyPair("EC", Globals.INITIALIZE_INT_VALUE, randomAlgorithm, "sm2p256v1");
	}

	/**
	 * <h3 class="en-US">Initialize SM2 encryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2加密安全适配器实例对象</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] SM2Encrypt(final PublicKey publicKey, final Object source) throws CryptoException {
		return process(CryptoMode.ENCRYPT,
				new CipherConfig("SM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", publicKey.getEncoded(), "BC"),
				source);
	}

	/**
	 * <h3 class="en-US">Initialize SM2 decryptor secure provider</h3>
	 * <h3 class="zh-CN">初始化SM2解密安全适配器实例对象</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @return <span class="en-US">Initialized secure provider instance</span>
	 * <span class="zh-CN">初始化的安全适配器实例对象</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] SM2Decrypt(final PrivateKey privateKey, final Object source) throws CryptoException {
		return process(CryptoMode.DECRYPT,
				new CipherConfig("SM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", privateKey.getEncoded(), "BC"),
				source);
	}

	/**
	 * <h3 class="en-US">Sign the data using the SM3withSM2 algorithm.</h3>
	 * <h3 class="zh-CN">使用 SM3withSM2 算法对数据进行签名</h3>
	 *
	 * @param privateKey <span class="en-US">Private Key instance</span>
	 *                   <span class="zh-CN">私钥证书实例对象</span>
	 * @param source     <span class="en-US">source object</span>
	 *                   <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static byte[] SM2Sign(final PrivateKey privateKey, final Object source) throws CryptoException {
		return process(CryptoMode.SIGNATURE,
				new CipherConfig("SM3withSM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", privateKey.getEncoded(), "BC"),
				source);
	}

	/**
	 * <h3 class="en-US">Verify SM2 signature</h3>
	 * <h3 class="zh-CN">验证 SM2 签名</h3>
	 *
	 * @param publicKey <span class="en-US">Public Key instance</span>
	 *                  <span class="zh-CN">公钥证书实例对象</span>
	 * @param source    <span class="en-US">source object</span>
	 *                  <span class="zh-CN">原始数据对象</span>
	 * @param signature <span class="en-US">Signature data</span>
	 *                  <span class="zh-CN">签名数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 * @throws CryptoException <span class="en-US">If algorithm didn't find</span>
	 *                         <span class="zh-CN">如果算法未找到</span>
	 */
	public static boolean SM2Verify(final PublicKey publicKey, final Object source, final byte[] signature)
			throws CryptoException {
		return verify(
				new CipherConfig("SM3withSM2", Globals.DEFAULT_VALUE_STRING, Globals.DEFAULT_VALUE_STRING),
				new CipherKey("SM2", publicKey.getEncoded(), "BC"),
				source, signature);
	}

	/**
	 * <h3 class="en-US">Generate PublicKey from key data bytes and given algorithm</h3>
	 * <h3 class="zh-CN">根据给定的算法和二进制数据生成公钥</h3>
	 *
	 * @param algorithm <span class="en-US">Key algorithm</span>
	 *                  <span class="zh-CN">算法</span>
	 * @param keyBytes  <span class="en-US">Key data bytes</span>
	 *                  <span class="zh-CN">二进制数据</span>
	 * @return <span class="en-US">Generated publicKey or null if data bytes invalid</span>
	 * <span class="zh-CN">生成的公钥，如果二进制数据非法则返回null</span>
	 */
	public static PublicKey publicKey(final String algorithm, final byte[] keyBytes) {
		return SECURITY_ADAPTOR.publicKey(algorithm, keyBytes);
	}

	/**
	 * <h3 class="en-US">Generate PrivateKey from key data bytes and given algorithm</h3>
	 * <h3 class="zh-CN">根据给定的算法和二进制数据生成私钥</h3>
	 *
	 * @param algorithm <span class="en-US">Key algorithm</span>
	 *                  <span class="zh-CN">算法</span>
	 * @param keyBytes  <span class="en-US">Key data bytes</span>
	 *                  <span class="zh-CN">二进制数据</span>
	 * @return <span class="en-US">Generated privateKey or null if data bytes invalid</span>
	 * <span class="zh-CN">生成的私钥，如果二进制数据非法则返回null</span>
	 */
	public static PrivateKey privateKey(final String algorithm, final byte[] keyBytes) {
		return SECURITY_ADAPTOR.privateKey(algorithm, keyBytes);
	}

	/**
	 * <h3 class="en-US">Calculate digest value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的摘要值</h3>
	 *
	 * @param name   <span class="en-US">Digest algorithm name</span>
	 *               <span class="zh-CN">摘要算法名</span>
	 * @param source <span class="en-US">source object</span>
	 *               <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	private static byte[] digest(@Nonnull final String name, @Nonnull final Object source) {
		CryptoAdaptor adaptor = SECURITY_ADAPTOR.initDigest(new CipherConfig(name), null);
		process(adaptor, source);
		return adaptor.finish();
	}

	/**
	 * <h3 class="en-US">Calculate Hash-based message authentication code value of the given source object</h3>
	 * <h3 class="zh-CN">计算给定的原始数据对象的密钥散列消息认证码</h3>
	 *
	 * @param name     <span class="en-US">Digest algorithm name</span>
	 *                 <span class="zh-CN">摘要算法名</span>
	 * @param keyBytes <span class="en-US">Byte array of passcode</span>
	 *                 <span class="zh-CN">密钥字节数组</span>
	 * @param source   <span class="en-US">source object</span>
	 *                 <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	private static byte[] hmac(@Nonnull final String name, @Nonnull final byte[] keyBytes, @Nonnull final Object source) {
		CryptoAdaptor adaptor = SECURITY_ADAPTOR.initDigest(new CipherConfig(name), new CipherKey(name, keyBytes));
		process(adaptor, source);
		return adaptor.finish();
	}

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
	private static String encode(@Nonnull final byte[] dataBytes, @Nonnull final EncodeType encodeType) {
		if (dataBytes.length == 0) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		switch (encodeType) {
			case BASE32:
				return StringUtils.base32Encode(dataBytes);
			case BASE64:
				return StringUtils.base64Encode(dataBytes);
			case HEX:
				return ConvertUtils.bytesToHex(dataBytes);
			default:
				return Globals.DEFAULT_VALUE_STRING;
		}
	}

	/**
	 * <h3 class="en-US">Perform the specified operation on the given source object.</h3>
	 * <h3 class="zh-CN">对给定的原始数据对象执行指定的操作</h3>
	 *
	 * @param cryptoMode   <span class="en-US">Enumeration value of data operate</span>
	 *                     <span class="zh-CN">操作类型枚举值</span>
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @param source       <span class="en-US">source object</span>
	 *                     <span class="zh-CN">原始数据对象</span>
	 * @return <span class="en-US">Binary data bytes of calculate result</span>
	 * <span class="zh-CN">计算结果的字节数组</span>
	 */
	private static byte[] process(@Nonnull final CryptoMode cryptoMode, @Nonnull final CipherConfig cipherConfig,
	                              @Nonnull final CipherKey cipherKey, @Nonnull final Object source) {
		CryptoAdaptor adaptor;
		switch (cryptoMode) {
			case ENCRYPT:
				adaptor = SECURITY_ADAPTOR.encryptor(cipherConfig, cipherKey);
				break;
			case DECRYPT:
				adaptor = SECURITY_ADAPTOR.decryptor(cipherConfig, cipherKey);
				break;
			case SIGNATURE:
				adaptor = SECURITY_ADAPTOR.signer(cipherConfig, cipherKey);
				break;
			default:
				throw new CryptoException(0x000000150003L);
		}
		process(adaptor, source);
		return adaptor.finish();
	}

	/**
	 * <h3 class="en-US">Verify the validity of the signature data.</h3>
	 * <h3 class="zh-CN">验证签名数据是否有效</h3>
	 *
	 * @param cipherConfig <span class="en-US">Signature verifier cipher config instance object</span>
	 *                     <span class="zh-CN">签名验证算法配置信息</span>
	 * @param cipherKey    <span class="en-US">Signature verifier cipher key instance object</span>
	 *                     <span class="zh-CN">签名验证密钥实例对象</span>
	 * @param source       <span class="en-US">source object</span>
	 *                     <span class="zh-CN">原始数据对象</span>
	 * @param signature    <span class="en-US">Signature data</span>
	 *                     <span class="zh-CN">签名数据</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	private static boolean verify(@Nonnull final CipherConfig cipherConfig, @Nonnull final CipherKey cipherKey,
	                              @Nonnull final Object source, @Nonnull final byte[] signature) {
		CryptoAdaptor verifierAdaptor = SECURITY_ADAPTOR.verifier(cipherConfig, cipherKey);
		process(verifierAdaptor, source);
		return verifierAdaptor.verify(signature);
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
	private static void process(@Nonnull final CryptoAdaptor cryptoAdaptor, @Nonnull final Object source) {
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
