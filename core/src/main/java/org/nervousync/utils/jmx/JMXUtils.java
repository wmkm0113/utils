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

package org.nervousync.utils.jmx;

import jakarta.annotation.Nonnull;
import org.nervousync.annotations.jmx.Monitor;
import org.nervousync.commons.Globals;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Optional;

/**
 * <h2 class="en-US">Java management extensions Utilities</h2>
 * <h2 class="zh-CN">Java管理扩展工具集</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.4 $ $Date: Jan 13, 2010 16:26:58 $
 */
@SuppressWarnings("unused")
public final class JMXUtils {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(JMXUtils.class);

	/**
	 * <span class="en-US">JMX MBean server instance object</span>
	 * <span class="zh-CN">JMX的MBean服务器实例对象</span>
	 */
	private static final MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();

	/**
	 * <h3 class="en-US">Private constructor for Java management extensions Utilities</h3>
	 * <h3 class="zh-CN">Java管理扩展工具集的私有构造方法</h3>
	 */
	private JMXUtils() {
	}

	/**
	 * <h3 class="en-US">According to the annotation {@link org.nervousync.annotations.jmx.Monitor} information, parse the ObjectName of the monitoring object</h3>
	 * <h3 class="zh-CN">根据注解 {@link org.nervousync.annotations.jmx.Monitor} 信息，解析监控对象的ObjectName</h3>
	 *
	 * @param mbeanObject <span class="en-US">Standard MBean object</span>
	 *                    <span class="zh-CN">标准MBean对象</span>
	 * @return <span class="en-US">ObjectName of the monitoring object</span>
	 * <span class="zh-CN">监控对象的ObjectName</span>
	 */
	public static String name(@Nonnull final Object mbeanObject) {
		if (mbeanObject.getClass().isAnnotationPresent(Monitor.class)) {
			return null;
		}
		return Optional.ofNullable(mbeanObject.getClass().getAnnotation(Monitor.class))
				.map(monitor -> {
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append(monitor.domain()).append(":").append("type=").append(monitor.type());
					if (StringUtils.notBlank(monitor.name())) {
						stringBuilder.append(",name=").append(monitor.name());
					}
					return stringBuilder.toString();
				})
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Register the standard MBean object to JMX manager</h3>
	 * <h3 class="zh-CN">注册标准MBean对象到JMX管理器</h3>
	 *
	 * @param mbeanObject <span class="en-US">Standard MBean object</span>
	 *                    <span class="zh-CN">标准MBean对象</span>
	 */
	public static void register(@Nonnull final Object mbeanObject) {
		Optional.ofNullable(JMXUtils.name(mbeanObject))
				.filter(StringUtils::notBlank)
				.ifPresent(objectName -> register(objectName, mbeanObject));
	}

	/**
	 * <h3 class="en-US">Register the standard MBean object to JMX manager</h3>
	 * <h3 class="zh-CN">注册标准MBean对象到JMX管理器</h3>
	 *
	 * @param beanName    <span class="en-US">The object name of the MBean</span>
	 *                    <span class="zh-CN">MBean 的对象名称</span>
	 * @param mbeanObject <span class="en-US">Standard MBean object</span>
	 *                    <span class="zh-CN">标准MBean对象</span>
	 */
	public static void register(@Nonnull final String beanName, @Nonnull final Object mbeanObject) {
		try {
			MBEAN_SERVER.registerMBean(mbeanObject, new ObjectName(beanName));
		} catch (MalformedObjectNameException e) {
			LOGGER.error("MBean_Object_Name_Generate_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			throw new RuntimeException(e);
		} catch (OperationsException | MBeanRegistrationException e) {
			LOGGER.error("MBean_Object_Register_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * <h3 class="en-US">Unregister standard MBean objects from the JMX manager</h3>
	 * <h3 class="zh-CN">从JMX管理器中取消注册标准MBean对象</h3>
	 *
	 * @param beanName <span class="en-US">The object name of the MBean</span>
	 *                 <span class="zh-CN">MBean 的对象名称</span>
	 */
	public static void unregister(@Nonnull final String beanName) {
		try {
			MBEAN_SERVER.unregisterMBean(new ObjectName(beanName));
		} catch (MalformedObjectNameException e) {
			LOGGER.error("MBean_Object_Name_Generate_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			throw new RuntimeException(e);
		} catch (OperationsException | MBeanRegistrationException e) {
			LOGGER.error("MBean_Object_Register_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * <h3 class="en-US">Unregister standard MBean objects from the JMX manager</h3>
	 * <h3 class="zh-CN">从JMX管理器中取消注册标准MBean对象</h3>
	 *
	 * @param mbeanObject <span class="en-US">Standard MBean object</span>
	 *                    <span class="zh-CN">标准MBean对象</span>
	 */
	public static void unregister(final Object mbeanObject) {
		Optional.ofNullable(JMXUtils.name(mbeanObject))
				.filter(StringUtils::notBlank)
				.ifPresent(JMXUtils::unregister);
	}
}
