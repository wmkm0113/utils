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
package org.nervousync.utils;

import org.nervousync.annotations.provider.Provider;
import org.nervousync.commons.Globals;
import org.nervousync.commons.id.CUID;
import org.nervousync.commons.id.ULID;
import org.nervousync.enumerations.generator.UUIDIdentifier;
import org.nervousync.enumerations.generator.UUIDLocalDomain;
import org.nervousync.generator.IGenerator;
import org.nervousync.generator.cuid.impl.CUIDv2Generator;
import org.nervousync.generator.nano.NanoGenerator;
import org.nervousync.generator.snowflake.SnowflakeGenerator;
import org.nervousync.generator.ulid.ULIDGenerator;
import org.nervousync.generator.uuid.impl.UUIDv1Generator;
import org.nervousync.generator.uuid.impl.UUIDv2Generator;
import org.nervousync.generator.uuid.timer.UUIDTimer;

import java.util.*;

/**
 * <h2 class="en-US">ID generator utilities</h2>
 * <h2 class="zh-CN">ID生成器工具集</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.2.0 $ $Date: Sep 13, 2017 11:27:28 $
 */
public final class IDUtils {

	/**
	 * <span class="en-US">Static value for provider name of CUIDv1 Generator</span>
	 * <span class="zh-CN">静态值用于CUIDv1生成器的提供名称</span>
	 */
	public static final String CUIDv1 = "CUIDv1";
	/**
	 * <span class="en-US">Static value for provider name of CUIDv2 Generator</span>
	 * <span class="zh-CN">静态值用于CUIDv2生成器的提供名称</span>
	 */
	public static final String CUIDv2 = "CUIDv2";
	/**
	 * <span class="en-US">Static value for provider name of UUIDv1 Generator</span>
	 * <span class="zh-CN">静态值用于UUIDv1生成器的提供名称</span>
	 */
	public static final String UUIDv1 = "UUIDv1";
	/**
	 * <span class="en-US">Static value for provider name of UUIDv2 Generator</span>
	 * <span class="zh-CN">静态值用于UUIDv2生成器的提供名称</span>
	 */
	public static final String UUIDv2 = "UUIDv2";
	/**
	 * <span class="en-US">Static value for provider name of UUIDv3 Generator</span>
	 * <span class="zh-CN">静态值用于UUIDv3生成器的提供名称</span>
	 */
	public static final String UUIDv3 = "UUIDv3";
	/**
	 * <span class="en-US">Static value for provider name of UUIDv4 Generator</span>
	 * <span class="zh-CN">静态值用于UUIDv4生成器的提供名称</span>
	 */
	public static final String UUIDv4 = "UUIDv4";
	/**
	 * <span class="en-US">Static value for provider name of UUIDv5 Generator</span>
	 * <span class="zh-CN">静态值用于UUIDv5生成器的提供名称</span>
	 */
	public static final String UUIDv5 = "UUIDv5";
	/**
	 * <span class="en-US">Static value for provider name of UUIDv6 Generator</span>
	 * <span class="zh-CN">静态值用于UUIDv6生成器的提供名称</span>
	 */
	public static final String UUIDv6 = "UUIDv6";
	/**
	 * <span class="en-US">Static value for provider name of NanoID Generator</span>
	 * <span class="zh-CN">静态值用于NanoID生成器的提供名称</span>
	 */
	public static final String NANO_ID = "NanoID";
	/**
	 * <span class="en-US">Static value for provider name of Snowflake Generator</span>
	 * <span class="zh-CN">静态值用于雪花算法生成器的提供名称</span>
	 */
	public static final String SNOWFLAKE = "Snowflake";
	/**
	 * <span class="en-US">Static value for provider name of Universally Unique Lexicographically Sortable Identifier Generator</span>
	 * <span class="zh-CN">静态值用于通用唯一字典排序标识符生成器的提供名称</span>
	 */
	public static final String ULID = "ULID";
	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(IDUtils.class);
	/**
	 * <span class="en-US">Registered ID generator provider map</span>
	 * <span class="zh-CN">已注册的ID生成器提供名称映射表</span>
	 */
	private static final Map<String, IGenerator<?>> INITIALIZE_MAP = new HashMap<>();

	static {
		//  Using Java SPI to loading ID generator implements classes
		ServiceLoader.load(IGenerator.class)
				.forEach(iGenerator ->
						Optional.ofNullable(iGenerator.getClass().getAnnotation(Provider.class))
								.ifPresent(provider -> INITIALIZE_MAP.put(provider.name(), iGenerator)));
		if (LOGGER.isDebugEnabled()) {
			List<String> providerCodes = IDUtils.registeredGenerators();
			LOGGER.info("Names_Generator_Registered_ID_Info",
					String.join(", ", providerCodes.toArray(new String[0])));
			if (LOGGER.isDebugEnabled()) {
				List<String> providerNames = new ArrayList<>();
				INITIALIZE_MAP.values().forEach(provider ->
						providerNames.add(MultilingualUtils.providerName(provider.getClass())));
				LOGGER.info("Names_Generator_Registered_Name_Info",
						String.join(", ", providerNames.toArray(new String[0])));
			}
		}
		SystemUtils.registerShutdownHook(IDUtils::destroy);
	}

	/**
	 * <h3 class="en-US">Private constructor for IDUtils</h3>
	 * <h3 class="zh-CN">ID生成器工具集的私有构造方法</h3>
	 */
	private IDUtils() {
	}

	/**
	 * <h3 class="en-US">Static method for configuring NanoID generator</h3>
	 * <h3 class="zh-CN">静态方法用于设置NanoID生成器</h3>
	 *
	 * @param alphabetConfig <span class="en-US">Alphabet configure string</span>
	 *                       <span class="zh-CN">输出字符设置</span>
	 * @param generateLength <span class="en-US">Generated result length</span>
	 *                       <span class="zh-CN">生成结果的长度</span>
	 */
	public static void nanoConfig(final String alphabetConfig, final int generateLength) {
		if (INITIALIZE_MAP.containsKey(NANO_ID)) {
			synchronized (INITIALIZE_MAP) {
				NanoGenerator generator = (NanoGenerator) INITIALIZE_MAP.get(NANO_ID);
				generator.config(alphabetConfig, generateLength);
				INITIALIZE_MAP.put(NANO_ID, generator);
			}
		}
	}

	/**
	 * <h3 class="en-US">Static method for configure Snowflake generator</h3>
	 * <h3 class="zh-CN">静态方法用于设置雪花算法生成器</h3>
	 *
	 * @param referenceTime <span class="en-US">Reference time, default value: 1303315200000L</span>
	 *                      <span class="zh-CN">起始时间戳，默认值：1303315200000L</span>
	 * @param deviceId      <span class="en-US">Node device ID (between 0 and 63), default value: 1L</span>
	 *                      <span class="zh-CN">节点的机器ID（取值范围：0到63），默认值：1L</span>
	 * @param instanceId    <span class="en-US">Node instance ID (between 0 and 63), default value: 1L</span>
	 *                      <span class="zh-CN">节点的实例ID（取值范围：0到63），默认值：1L</span>
	 */
	public static void snowflakeConfig(final long referenceTime, final long deviceId, final long instanceId) {
		if (INITIALIZE_MAP.containsKey(SNOWFLAKE)) {
			synchronized (INITIALIZE_MAP) {
				SnowflakeGenerator generator = (SnowflakeGenerator) INITIALIZE_MAP.get(SNOWFLAKE);
				generator.config(referenceTime, deviceId, instanceId);
				INITIALIZE_MAP.put(SNOWFLAKE, generator);
			}
		}
	}

	/**
	 * <h3 class="en-US">Static method for configuring Universally Unique Lexicographically Sortable Identifier generator</h3>
	 * <h3 class="zh-CN">静态方法用于设置通用唯一字典排序标识符生成器</h3>
	 *
	 * @param referenceTime <span class="en-US">Reference time, default value: 1303315200000L</span>
	 *                      <span class="zh-CN">起始时间戳，默认值：1303315200000L</span>
	 * @param monotonic     <span class="en-US">Monotonic flag</span>
	 *                      <span class="zh-CN">单调标记</span>
	 */
	public static void ulidConfig(final long referenceTime, final boolean monotonic) {
		if (INITIALIZE_MAP.containsKey(ULID)) {
			synchronized (INITIALIZE_MAP) {
				ULIDGenerator generator = (ULIDGenerator) INITIALIZE_MAP.get(ULID);
				generator.config(referenceTime, monotonic);
				INITIALIZE_MAP.put(ULID, generator);
			}
		}
	}

	/**
	 * <h3 class="en-US">Static method for configuring time synchronizer of UUIDv2 generator</h3>
	 * <h3 class="zh-CN">静态方法用于设置UUIDv2生成器的时间同步器</h3>
	 *
	 * @param uuidTimer      <span class="en-US">UUID timer instance</span>
	 *                       <span class="zh-CN">UUID时间生成器实例对象</span>
	 * @param uuidIdentifier <span class="en-US">Generation of UUID node identification code</span>
	 *                       <span class="zh-CN">UUID节点识别代码的生成方式</span>
	 */
	public static void UUIDv1Config(final UUIDTimer uuidTimer, final UUIDIdentifier uuidIdentifier) {
		if (INITIALIZE_MAP.containsKey(UUIDv1)) {
			synchronized (INITIALIZE_MAP) {
				UUIDv1Generator generator = (UUIDv1Generator) INITIALIZE_MAP.get(UUIDv1);
				generator.config(uuidTimer, uuidIdentifier);
				INITIALIZE_MAP.put(UUIDv1, generator);
			}
		}
	}

	/**
	 * <h3 class="en-US">Static method for configuring time synchronizer of UUIDv2 generator</h3>
	 * <h3 class="zh-CN">静态方法用于设置UUIDv2生成器的时间同步器</h3>
	 *
	 * @param uuidTimer      <span class="en-US">UUID timer instance</span>
	 *                       <span class="zh-CN">UUID时间生成器实例对象</span>
	 * @param uuidIdentifier <span class="en-US">Generation of UUID node identification code</span>
	 *                       <span class="zh-CN">UUID节点识别代码的生成方式</span>
	 * @param localDomain    <span class="en-US">Local domain of UUID version 2</span>
	 *                       <span class="zh-CN">UUID版本2的本地域</span>
	 */
	public static void UUIDv2Config(final UUIDTimer uuidTimer, final UUIDIdentifier uuidIdentifier,
	                                final UUIDLocalDomain localDomain) {
		if (INITIALIZE_MAP.containsKey(UUIDv2)) {
			synchronized (INITIALIZE_MAP) {
				UUIDv2Generator generator = (UUIDv2Generator) INITIALIZE_MAP.get(UUIDv2);
				generator.config(uuidTimer, uuidIdentifier);
				generator.config(localDomain);
				INITIALIZE_MAP.put(UUIDv2, generator);
			}
		}
	}

	/**
	 * <h3 class="en-US">Static method for generate NanoID value</h3>
	 * <h3 class="zh-CN">静态方法用于生成随机NanoID值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static String nano() {
		return Optional.ofNullable(INITIALIZE_MAP.get(NANO_ID))
				.map(generator -> ((NanoGenerator) generator).generate())
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Static method for generate Snowflake value</h3>
	 * <h3 class="zh-CN">静态方法用于生成随机雪花算法值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static Long snowflake() {
		return Optional.ofNullable(generate(SNOWFLAKE, new byte[0]))
				.map(value -> (Long) value)
				.orElse(Globals.DEFAULT_VALUE_LONG);
	}

	/**
	 * <h3 class="en-US">Static method for generate CUID version 1 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成CUID版本1的值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static CUID CUIDv1() {
		return (CUID) generate(CUIDv1, new byte[0]);
	}

	/**
	 * <h3 class="en-US">Static method for generate CUID version 2 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成CUID版本2的值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static CUID CUIDv2() {
		return (CUID) generate(CUIDv2, new byte[0]);
	}

	/**
	 * <h3 class="en-US">Static method for generate CUID version 2 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成CUID版本2的值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static CUID CUIDv2(final int length) {
		byte[] dataBytes = new byte[4];
		if (length <= Globals.INITIALIZE_INT_VALUE) {
			RawUtils.writeInt(dataBytes, CUIDv2Generator.VALUE_LENGTH);
		} else {
			RawUtils.writeInt(dataBytes, length);
		}
		return (CUID) generate(CUIDv2, dataBytes);
	}

	/**
	 * <h3 class="en-US">Static method for generate Universally Unique Lexicographically Sortable Identifier value</h3>
	 * <h3 class="zh-CN">静态方法用于生成通用唯一字典排序标识符值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static ULID ULID() {
		return (ULID) generate(ULID, new byte[0]);
	}

	/**
	 * <h3 class="en-US">Static method for generate UUIDv1 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成随机UUIDv1值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static UUID UUIDv1() {
		return (UUID) generate(UUIDv1, new byte[0]);
	}

	/**
	 * <h3 class="en-US">Static method for generate UUIDv2 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成随机UUIDv2值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static UUID UUIDv2() {
		return (UUID) generate(UUIDv2, new byte[0]);
	}

	/**
	 * <h3 class="en-US">Static method for generate UUIDv3 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成随机UUIDv3值</h3>
	 *
	 * @param dataBytes <span class="en-US">Given parameter</span>
	 *                  <span class="zh-CN">给定的参数</span>
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	@Deprecated(since = "1.2.4")
	public static UUID UUIDv3(final byte[] dataBytes) {
		return (UUID) generate(UUIDv3, dataBytes);
	}

	/**
	 * <h3 class="en-US">Static method for generate UUIDv4 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成随机UUIDv4值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static UUID UUIDv4() {
		return (UUID) generate(UUIDv4, new byte[0]);
	}

	/**
	 * <h3 class="en-US">Static method for generate UUIDv4 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成随机UUIDv4值</h3>
	 *
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static UUID UUIDv6() {
		return (UUID) generate(UUIDv6, new byte[0]);
	}

	/**
	 * <h3 class="en-US">Static method for generate UUIDv5 value</h3>
	 * <h3 class="zh-CN">静态方法用于生成随机UUIDv5值</h3>
	 *
	 * @param dataBytes <span class="en-US">Given parameter</span>
	 *                  <span class="zh-CN">给定的参数</span>
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	@Deprecated(since = "1.2.4")
	public static UUID UUIDv5(final byte[] dataBytes) {
		return (UUID) generate(UUIDv5, dataBytes);
	}

	/**
	 * <h3 class="en-US">Static method for generating value by the given generator name</h3>
	 * <h3 class="zh-CN">静态方法用于生成指定生成器的值</h3>
	 *
	 * @param generatorName <span class="en-US">Given generator name</span>
	 *                      <span class="zh-CN">生成器名称</span>
	 * @param dataBytes     <span class="en-US">Given parameter</span>
	 *                      <span class="zh-CN">给定的参数</span>
	 * @return <span class="en-US">Generated value</span>
	 * <span class="zh-CN">生成的值</span>
	 */
	public static Object generate(final String generatorName, final byte[] dataBytes) {
		if (StringUtils.isEmpty(generatorName)) {
			return Globals.DEFAULT_VALUE_STRING;
		}
		IGenerator<?> generator = INITIALIZE_MAP.get(generatorName);
		if (generator == null) {
			return null;
		}
		try {
			return (dataBytes == null || dataBytes.length == 0) ? generator.generate() : generator.generate(dataBytes);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * <h3 class="en-US">Read the registered generator code list</h3>
	 * <h3 class="zh-CN">读取已注册的生成器代码列表</h3>
	 *
	 * @return <span class="en-US">Registered generator code list</span>
	 * <span class="zh-CN">注册的生成器代码列表</span>
	 */
	public static List<String> registeredGenerators() {
		return new ArrayList<>(INITIALIZE_MAP.keySet());
	}

	/**
	 * <h3 class="en-US">Destroy all registered generator instances and clear map</h3>
	 * <h3 class="zh-CN">销毁所有已注册的生成器实例对象并清空映射表</h3>
	 */
	public static void destroy() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Destroy_Generator_Registered_ID_Debug");
		}
		INITIALIZE_MAP.values().forEach(IGenerator::destroy);
		INITIALIZE_MAP.clear();
	}
}
