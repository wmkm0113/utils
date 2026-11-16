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
import org.nervousync.beans.digest.CRCConfig;
import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.enumerations.security.EncodeType;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.security.crc.CRCAdaptor;
import org.nervousync.security.crc.CRCDigestAdapterImpl;
import org.nervousync.security.digest.DefaultDigestProviderImpl;
import org.nervousync.security.digest.DigestProvider;
import org.nervousync.utils.commons.RawUtils;
import org.nervousync.utils.commons.StringUtils;

import java.nio.ByteOrder;
import java.util.*;

/**
 * <h2 class="en-US">Digest algorithm utilities</h2>
 * <h2 class="zh-CN">摘要算法工具集</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public final class DigestUtils extends SecurityUtils {

	/**
	 * <span class="en-US">Digest provider instance object</span>
	 * <span class="zh-CN">摘要算法适配器实例对象</span>
	 */
	private static final DigestProvider DIGEST_PROVIDER;

	/**
	 * <span class="en-US">Registered CRC configure information</span>
	 * <span class="zh-CN">注册的CRC配置值映射表</span>
	 */
	private static final Map<String, CRCConfig> REGISTERED_CRC_CONFIG = new HashMap<>();

	static {
		DIGEST_PROVIDER = ServiceLoader.load(DigestProvider.class).findFirst().orElse(new DefaultDigestProviderImpl());
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
		return CRCAdaptor(algorithm);
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
	public static CRCAdaptor CRCAdaptor(final String algorithm) throws CryptoException {
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
		return Optional.ofNullable(DigestUtils.crcConfig(algorithm))
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
		DigestUtils.process(crcAdaptor, source);
		return CRCResult(algorithm, crcAdaptor.finish().clone());
	}

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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("MD5"), null);
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
		return DigestUtils.encode(MD5(source), encodeType);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacMD5"), new CipherKey("HmacMD5", keyBytes));
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
		return DigestUtils.encode(HmacMD5(keyBytes, source), encodeType);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA1"), null);
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
		return DigestUtils.encode(SHA1(source), encodeType);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA1"), new CipherKey("HmacSHA1", keyBytes));
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
		return DigestUtils.encode(HmacSHA1(keyBytes, source), encodeType);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA-224"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA224"), new CipherKey("HmacSHA224", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA-256"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA256"), new CipherKey("HmacSHA256", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA-384"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA384"), new CipherKey("HmacSHA384", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA-512"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA512"), new CipherKey("HmacSHA512", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA-512/224"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA512/224"), new CipherKey("HmacSHA512/224", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA-512/256"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA512/256"), new CipherKey("HmacSHA512/256", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA3-224"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA3-224"), new CipherKey("HmacSHA3-224", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA3-256"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA3-256"), new CipherKey("HmacSHA3-256", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA3-384"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA3-384"), new CipherKey("HmacSHA3-384", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHA3-512"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSHA3-512"), new CipherKey("HmacSHA3-512", keyBytes));
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHAKE128"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SHAKE256"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("SM3"), null);
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
		return DIGEST_PROVIDER.initDigest(new CipherConfig("HmacSM3"), new CipherKey("HmacSM3", keyBytes));
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
		CryptoAdaptor adaptor = DIGEST_PROVIDER.initDigest(new CipherConfig(name), null);
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
		CryptoAdaptor adaptor = DIGEST_PROVIDER.initDigest(new CipherConfig(name), new CipherKey(name, keyBytes));
		process(adaptor, source);
		return adaptor.finish();
	}
}
