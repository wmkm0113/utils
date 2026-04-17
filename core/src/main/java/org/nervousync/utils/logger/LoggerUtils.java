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

package org.nervousync.utils.logger;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.logger.LogLevel;
import org.nervousync.exceptions.AbstractException;
import org.nervousync.i18n.MessageAgent;
import org.nervousync.logger.LogConfigurator;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.i18n.MultilingualUtils;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <h2 class="en-US">Logger Utilities</h2>
 * <span class="en-US">
 * <span>Current utilities implements features:</span>
 *     <ul>Using programming to initialize Log4j</ul>
 *     <ul>Using programming to initialize Log4j and configure the target package with a custom level</ul>
 *     <ul>Support output internationalize logger information</ul>
 * </span>
 * <h2 class="zh-CN">日志工具集</h2>
 * <span class="zh-CN">
 *     <span>此工具集实现以下功能:</span>
 *     <ul>使用编程方式初始化Log4j</ul>
 *     <ul>使用编程方式初始化Log4j并设置目标包名为不同的日志等级</ul>
 *     <ul>支持国际化的日志输出</ul>
 * </span>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.2.0 $ $Date: Sep 15, 2018 16:54:27 $
 */
@SuppressWarnings("unused")
public final class LoggerUtils {

	/**
	 * <span class="en-US">Log configurator instance object</span>
	 * <span class="zh-CN">日志配置器实例对象</span>
	 */
	private static final LogConfigurator LOG_CONFIGURATOR;

	static {
		//  Using ServiceLoader to load LogConfigurator
		Iterator<LogConfigurator> iterator = ServiceLoader.load(LogConfigurator.class).iterator();
		LogConfigurator logConfigurator = null;
		while (iterator.hasNext()) {
			logConfigurator = iterator.next();
			if (logConfigurator != null) {
				break;
			}
		}
		LOG_CONFIGURATOR = logConfigurator;
	}

	/**
	 * <h3 class="en-US">Private constructor for LoggerUtils</h3>
	 * <h3 class="zh-CN">日志工具集的私有构造方法</h3>
	 */
	private LoggerUtils() {
	}

	/**
	 * <h3 class="en-US">Configure the root logger using the given level</h3>
	 * <h3 class="zh-CN">使用给定的日志等级设置根日志</h3>
	 *
	 * @param rootLevel <span class="en-US">Log level</span>
	 *                  <span class="zh-CN">日志等级</span>
	 */
	public static void initLoggerConfigure(final LogLevel rootLevel) {
		initLoggerConfigure(rootLevel, new PackageLogger[0]);
	}

	/**
	 * <h3 class="en-US">Configure the root logger using the given level and configure the given package name to a custom level</h3>
	 * <h3 class="zh-CN">使用给定的日志等级设置根日志，同时设置给定的包名为对应的日志等级</h3>
	 *
	 * @param rootLevel      <span class="en-US">Log level</span>
	 *                       <span class="zh-CN">日志等级</span>
	 * @param packageLoggers <span class="en-US">Package logger configure array</span>
	 *                       <span class="zh-CN">包日志设置数组</span>
	 */
	public static void initLoggerConfigure(final LogLevel rootLevel, final PackageLogger... packageLoggers) {
		initLoggerConfigure(Globals.DEFAULT_VALUE_STRING, rootLevel, packageLoggers);
	}

	/**
	 * <h3 class="en-US">Configure the root logger using the given level, save the logger to the target file path, and configure the given package name to custom level</h3>
	 * <h3 class="zh-CN">使用给定的日志等级设置根日志，将日志文件写入到指定的目录，同时设置给定的包名为对应的日志等级</h3>
	 *
	 * @param basePath       <span class="en-US">Log file base path</span>
	 *                       <span class="zh-CN">文件日志的保存目录</span>
	 * @param rootLevel      <span class="en-US">Log level</span>
	 *                       <span class="zh-CN">日志等级</span>
	 * @param packageLoggers <span class="en-US">Package logger configure array</span>
	 *                       <span class="zh-CN">包日志设置数组</span>
	 */
	public static void initLoggerConfigure(final String basePath, final LogLevel rootLevel,
	                                       final PackageLogger... packageLoggers) {
		if (LOG_CONFIGURATOR != null) {
			LOG_CONFIGURATOR.initLogger(basePath, rootLevel, packageLoggers);
		}
	}

	/**
	 * <h3 class="en-US">Generate a PackageLogger instance using the given package name and log level</h3>
	 * <h3 class="zh-CN">使用给定的包名和日志等级生成PackageLogger实例对象</h3>
	 *
	 * @param packageName <span class="en-US">Package name</span>
	 *                    <span class="zh-CN">包名</span>
	 * @param loggerLevel <span class="en-US">Log level</span>
	 *                    <span class="zh-CN">日志等级</span>
	 * @return <span class="en-US">Generated PackageLogger instance</span>
	 * <span class="zh-CN">生成的PackageLogger实例对象</span>
	 */
	public static PackageLogger newLogger(final String packageName, final LogLevel loggerLevel) {
		return new PackageLogger(packageName, loggerLevel);
	}

	/**
	 * <h3 class="en-US">Retrieve i18n logger instance</h3>
	 * <h3 class="zh-CN">获取国际化支持的日志实例对象</h3>
	 *
	 * @param clazz <span class="en-US">Logger identify class</span>
	 *              <span class="zh-CN">日志识别类</span>
	 * @return <span class="en-US">Generated logger instance</span>
	 * <span class="zh-CN">生成的日志实例对象</span>
	 */
	public static Logger getLogger(final Class<?> clazz) {
		return new Logger(clazz);
	}

	/**
	 * <h2 class="en-US">Package logger define</h2>
	 * <h2 class="zh-CN">包日志定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2018 17:28:14 $
	 */
	public static final class PackageLogger {
		/**
		 * <span class="en-US">Package name</span>
		 * <span class="zh-CN">包名</span>
		 */
		private final String packageName;
		/**
		 * <span class="en-US">Log level</span>
		 * <span class="zh-CN">日志等级</span>
		 */
		private final LogLevel loggerLevel;

		/**
		 * <h3 class="en-US">Private constructor for PackageLogger</h3>
		 * <h3 class="zh-CN">包日志定义的私有构造方法</h3>
		 *
		 * @param packageName <span class="en-US">Package name</span>
		 *                    <span class="zh-CN">包名</span>
		 * @param loggerLevel <span class="en-US">Log level</span>
		 *                    <span class="zh-CN">日志等级</span>
		 */
		private PackageLogger(final String packageName, final LogLevel loggerLevel) {
			this.packageName = packageName;
			this.loggerLevel = loggerLevel;
		}

		/**
		 * <h3 class="en-US">Getter method for package name</h3>
		 * <h3 class="zh-CN">包名的Getter方法</h3>
		 *
		 * @return <span class="en-US">Package name</span>
		 * <span class="zh-CN">包名</span>
		 */
		public String getPackageName() {
			return packageName;
		}

		/**
		 * <h3 class="en-US">Getter method for log level</h3>
		 * <h3 class="zh-CN">日志等级的Getter方法</h3>
		 *
		 * @return <span class="en-US">Log level</span>
		 * <span class="zh-CN">日志等级</span>
		 */
		public LogLevel getLoggerLevel() {
			return loggerLevel;
		}
	}

	/**
	 * <h2 class="en-US">Logger define for support i18n</h2>
	 * <h2 class="zh-CN">有国际化支持的日志定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Jul 21, 2023 12:59:48 $
	 */
	public static final class Logger {
		/**
		 * <span class="en-US">Logger instance</span>
		 * <span class="zh-CN">日志实例</span>
		 */
		private final org.slf4j.Logger logger;
		/**
		 * <span class="en-US">Internationalization message agent instance object</span>
		 * <span class="zh-CN">国际化信息代理实例对象</span>
		 */
		private final MessageAgent multiAgent;

		/**
		 * <h3 class="en-US">Constructor for MultilingualLogger</h3>
		 * <h3 class="zh-CN">有国际化支持的日志的构造方法</h3>
		 *
		 * @param clazz <span class="en-US">Logger identify class</span>
		 *              <span class="zh-CN">日志识别类</span>
		 */
		Logger(final Class<?> clazz) {
			this.logger = LoggerFactory.getLogger(clazz);
			this.multiAgent = MultilingualUtils.newAgent(clazz);
		}

		/**
		 * <h3 class="en-US">Logger level is trace enabled</h3>
		 * <h3 class="zh-CN">日志级别开启Trace</h3>
		 */
		public boolean isTraceEnabled() {
			return this.logger.isTraceEnabled();
		}

		/**
		 * <h3 class="en-US">Output trace message</h3>
		 * <h3 class="zh-CN">输出Trace信息</h3>
		 *
		 * @param message <span class="en-US">Message identify key</span>
		 *                <span class="zh-CN">信息识别键值</span>
		 */
		public void trace(final String message) {
			this.trace(message, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output trace message</h3>
		 * <h3 class="zh-CN">输出Trace信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void trace(final String message, final Object... collections) {
			this.trace(message, null, collections);
		}

		/**
		 * <h3 class="en-US">Output trace message</h3>
		 * <h3 class="zh-CN">输出Trace信息</h3>
		 *
		 * @param message   <span class="en-US">Message identify key</span>
		 *                  <span class="zh-CN">信息识别键值</span>
		 * @param throwable <span class="en-US">Throwable exception instance</span>
		 *                  <span class="zh-CN">抛出的异常实例对象</span>
		 */
		public void trace(final String message, final Throwable throwable) {
			this.trace(message, throwable, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output trace message</h3>
		 * <h3 class="zh-CN">输出Trace信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param throwable   <span class="en-US">Throwable exception instance</span>
		 *                    <span class="zh-CN">抛出的异常实例对象</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void trace(final String message, final Throwable throwable, final Object... collections) {
			this.logger.trace(this.multiAgent.findMessage(message, collections));
			if (throwable != null) {
				this.logger.trace(this.errorMessage(throwable), throwable);
			}
		}

		/**
		 * <h3 class="en-US">Logger level is debug enabled</h3>
		 * <h3 class="zh-CN">日志级别开启Debug</h3>
		 */
		public boolean isDebugEnabled() {
			return this.logger.isDebugEnabled();
		}

		/**
		 * <h3 class="en-US">Output debug message</h3>
		 * <h3 class="zh-CN">输出Debug信息</h3>
		 *
		 * @param message <span class="en-US">Message identify key</span>
		 *                <span class="zh-CN">信息识别键值</span>
		 */
		public void debug(final String message) {
			this.debug(message, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output debug message</h3>
		 * <h3 class="zh-CN">输出Debug信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void debug(final String message, final Object... collections) {
			this.debug(message, null, collections);
		}

		/**
		 * <h3 class="en-US">Output debug message</h3>
		 * <h3 class="zh-CN">输出Debug信息</h3>
		 *
		 * @param message   <span class="en-US">Message identify key</span>
		 *                  <span class="zh-CN">信息识别键值</span>
		 * @param throwable <span class="en-US">Throwable exception instance</span>
		 *                  <span class="zh-CN">抛出的异常实例对象</span>
		 */
		public void debug(final String message, final Throwable throwable) {
			this.debug(message, throwable, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output debug message</h3>
		 * <h3 class="zh-CN">输出Debug信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param throwable   <span class="en-US">Throwable exception instance</span>
		 *                    <span class="zh-CN">抛出的异常实例对象</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void debug(final String message, final Throwable throwable, final Object... collections) {
			this.logger.debug(this.multiAgent.findMessage(message, collections));
			if (throwable != null) {
				this.logger.debug(this.errorMessage(throwable), throwable);
			}
		}

		/**
		 * <h3 class="en-US">Logger level is info enabled</h3>
		 * <h3 class="zh-CN">日志级别开启Info</h3>
		 */
		public boolean isInfoEnabled() {
			return this.logger.isInfoEnabled();
		}

		/**
		 * <h3 class="en-US">Output info message</h3>
		 * <h3 class="zh-CN">输出Info信息</h3>
		 *
		 * @param message <span class="en-US">Message identify key</span>
		 *                <span class="zh-CN">信息识别键值</span>
		 */
		public void info(final String message) {
			this.info(message, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output info message</h3>
		 * <h3 class="zh-CN">输出Info信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void info(final String message, final Object... collections) {
			this.info(message, null, collections);
		}

		/**
		 * <h3 class="en-US">Output info message</h3>
		 * <h3 class="zh-CN">输出Info信息</h3>
		 *
		 * @param message   <span class="en-US">Message identify key</span>
		 *                  <span class="zh-CN">信息识别键值</span>
		 * @param throwable <span class="en-US">Throwable exception instance</span>
		 *                  <span class="zh-CN">抛出的异常实例对象</span>
		 */
		public void info(final String message, final Throwable throwable) {
			this.info(message, throwable, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output info message</h3>
		 * <h3 class="zh-CN">输出Info信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param throwable   <span class="en-US">Throwable exception instance</span>
		 *                    <span class="zh-CN">抛出的异常实例对象</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void info(final String message, final Throwable throwable, final Object... collections) {
			this.logger.info(this.multiAgent.findMessage(message, collections));
			if (throwable != null) {
				this.logger.info(this.errorMessage(throwable), throwable);
			}
		}

		/**
		 * <h3 class="en-US">Logger level is warn enabled</h3>
		 * <h3 class="zh-CN">日志级别开启Warn</h3>
		 */
		public boolean isWarnEnabled() {
			return this.logger.isWarnEnabled();
		}

		/**
		 * <h3 class="en-US">Output trace message</h3>
		 * <h3 class="zh-CN">输出Warn信息</h3>
		 *
		 * @param message <span class="en-US">Message identify key</span>
		 *                <span class="zh-CN">信息识别键值</span>
		 */
		public void warn(final String message) {
			this.warn(message, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output warns message</h3>
		 * <h3 class="zh-CN">输出Warn信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void warn(final String message, final Object... collections) {
			this.warn(message, null, collections);
		}

		/**
		 * <h3 class="en-US">Output warns message</h3>
		 * <h3 class="zh-CN">输出Warn信息</h3>
		 *
		 * @param message   <span class="en-US">Message identify key</span>
		 *                  <span class="zh-CN">信息识别键值</span>
		 * @param throwable <span class="en-US">Throwable exception instance</span>
		 *                  <span class="zh-CN">抛出的异常实例对象</span>
		 */
		public void warn(final String message, final Throwable throwable) {
			this.warn(message, throwable, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output warns message</h3>
		 * <h3 class="zh-CN">输出Warn信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param throwable   <span class="en-US">Throwable exception instance</span>
		 *                    <span class="zh-CN">抛出的异常实例对象</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void warn(final String message, final Throwable throwable, final Object... collections) {
			this.logger.warn(this.multiAgent.findMessage(message, collections));
			if (throwable != null) {
				this.logger.warn(this.errorMessage(throwable), throwable);
			}
		}

		/**
		 * <h3 class="en-US">Logger level is error enabled</h3>
		 * <h3 class="zh-CN">日志级别开启Error</h3>
		 */
		public boolean isErrorEnabled() {
			return this.logger.isErrorEnabled();
		}

		/**
		 * <h3 class="en-US">Output trace message</h3>
		 * <h3 class="zh-CN">输出Error信息</h3>
		 *
		 * @param message <span class="en-US">Message identify key</span>
		 *                <span class="zh-CN">信息识别键值</span>
		 */
		public void error(final String message) {
			this.error(message, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output error message</h3>
		 * <h3 class="zh-CN">输出Error信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void error(final String message, final Object... collections) {
			this.error(message, null, collections);
		}

		/**
		 * <h3 class="en-US">Output error message</h3>
		 * <h3 class="zh-CN">输出Error信息</h3>
		 *
		 * @param message   <span class="en-US">Message identify key</span>
		 *                  <span class="zh-CN">信息识别键值</span>
		 * @param throwable <span class="en-US">Throwable exception instance</span>
		 *                  <span class="zh-CN">抛出的异常实例对象</span>
		 */
		public void error(final String message, final Throwable throwable) {
			this.error(message, throwable, new Object[0]);
		}

		/**
		 * <h3 class="en-US">Output error message</h3>
		 * <h3 class="zh-CN">输出Error信息</h3>
		 *
		 * @param message     <span class="en-US">Message identify key</span>
		 *                    <span class="zh-CN">信息识别键值</span>
		 * @param throwable   <span class="en-US">Throwable exception instance</span>
		 *                    <span class="zh-CN">抛出的异常实例对象</span>
		 * @param collections <span class="en-US">given parameters of information formatter</span>
		 *                    <span class="zh-CN">用于资源信息格式化的参数</span>
		 */
		public void error(final String message, final Throwable throwable, final Object... collections) {
			this.logger.error(this.multiAgent.findMessage(message, collections));
			if (throwable != null) {
				this.logger.error(this.errorMessage(throwable), throwable);
			}
		}

		/**
		 * <h3 class="en-US">Read the exception error code and get the corresponding multilingual information</h3>
		 * <h3 class="zh-CN">读取异常的错误代码并获取对应的多语言信息</h3>
		 *
		 * @param throwable <span class="en-US">Throwable exception instance</span>
		 *                  <span class="zh-CN">抛出的异常实例对象</span>
		 * @return <span class="en-US">Error message</span>
		 * <span class="zh-CN">错误信息</span>
		 */
		private String errorMessage(@Nonnull final Throwable throwable) {
			StringBuilder stringBuilder = new StringBuilder();
			if (throwable instanceof AbstractException) {
				Optional.ofNullable(this.multiAgent.findMessage("Code_Error",
								"0x" + Long.toHexString(((AbstractException) throwable).getErrorCode())))
						.filter(StringUtils::notBlank)
						.ifPresent(errorMsg -> stringBuilder.append(errorMsg).append(FileUtils.CRLF));
			}
			stringBuilder.append(this.multiAgent.findMessage("Stack_Message_Error"));
			return stringBuilder.toString();
		}
	}
}
