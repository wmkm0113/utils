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

package org.nervousync.logger.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.logger.LogLevel;
import org.nervousync.logger.LogConfigurator;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.util.*;

/**
 * <h2 class="en-US">Log4j2 configurator implement class</h2>
 * <h2 class="zh-CN">Log4j2 日志配置器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Feb 10, 2026 12:13:52 $
 */
public final class Log4jConfiguratorImpl implements LogConfigurator {
	@Override
	public void initLogger(final String basePath, final LogLevel rootLevel,
	                       final LoggerUtils.PackageLogger... packageLoggers) {
		Optional.ofNullable(generateConfiguration(generateConfig(basePath, rootLevel, packageLoggers)))
				.ifPresent(configuration -> {
					LoggerContext loggerContext = (LoggerContext) LogManager.getContext(Boolean.FALSE);
					loggerContext.setConfiguration(configuration);
					loggerContext.updateLoggers();
				});
	}

	/**
	 * <h3 class="en-US">Generate Configuration instance by given LogConfig instance</h3>
	 * <h3 class="zh-CN">使用给定的日志配置定义生成Configuration实例对象</h3>
	 *
	 * @param logConfig <span class="en-US">LogConfig instance</span>
	 *                  <span class="zh-CN">日志配置定义实例对象</span>
	 * @return <span class="en-US">Generated Configuration instance</span>
	 * <span class="zh-CN">生成的Configuration实例对象</span>
	 */
	private static Configuration generateConfiguration(final LogConfig logConfig) {
		if (logConfig == null || logConfig.getAppenderConfigures() == null) {
			return null;
		}
		final ConfigurationBuilder<BuiltConfiguration> configurationBuilder =
				ConfigurationBuilderFactory.newConfigurationBuilder();
		configurationBuilder.setStatusLevel(Level.ERROR);
		Optional<LayoutComponentBuilder> layoutComponentBuilder =
				Optional.ofNullable(layoutBuilder(logConfig.getPatternLayoutConfigure()));

		logConfig.getAppenderConfigures()
				.stream()
				.filter(appenderConfigure ->
						StringUtils.notBlank(appenderConfigure.getAppenderName())
								&& StringUtils.notBlank(appenderConfigure.getAppenderPlugin()))
				.forEach(appenderConfigure -> {
					AppenderComponentBuilder appenderComponentBuilder =
							configurationBuilder.newAppender(appenderConfigure.getAppenderName(),
									appenderConfigure.getAppenderPlugin());
					Optional.ofNullable(appenderConfigure.getAppenderAttributes())
							.ifPresent(appenderAttributes ->
									appenderAttributes.forEach(appenderComponentBuilder::addAttribute));
					Optional.ofNullable(appenderConfigure.getAppenderComponents())
							.ifPresent(appenderComponents ->
									appenderComponents.forEach(componentConfigure ->
											initComponentConfig(appenderComponentBuilder, componentConfigure)));
					if (appenderConfigure.getPatternLayoutConfigure() == null) {
						layoutComponentBuilder.ifPresent(appenderComponentBuilder::add);
					} else {
						Optional.ofNullable(layoutBuilder(appenderConfigure.getPatternLayoutConfigure()))
								.ifPresent(appenderComponentBuilder::add);
					}
					configurationBuilder.add(appenderComponentBuilder);
				});

		Optional.ofNullable(logConfig.getLoggerConfigures())
				.ifPresent(loggerConfigures ->
						loggerConfigures.stream()
								.filter(loggerConfigure ->
										StringUtils.notBlank(loggerConfigure.getPackageName())
												&& loggerConfigure.getAppenderNames() != null
												&& !loggerConfigure.getAppenderNames().isEmpty())
								.forEach(loggerConfigure -> {
									LoggerComponentBuilder loggerComponentBuilder =
											configurationBuilder.newLogger(loggerConfigure.getPackageName(),
													loggerConfigure.getLoggerLevel());
									loggerComponentBuilder.addAttribute("additivity", Boolean.FALSE);
									loggerConfigure.getAppenderNames().forEach(appenderName ->
											loggerComponentBuilder.add(configurationBuilder.newAppenderRef(appenderName)));
									configurationBuilder.add(loggerComponentBuilder);
								}));

		LoggerConfigure rootLoggerConfigure = logConfig.getRootLoggerConfigure();
		if (rootLoggerConfigure.getAppenderNames() != null && !rootLoggerConfigure.getAppenderNames().isEmpty()) {
			RootLoggerComponentBuilder rootLoggerComponentBuilder =
					configurationBuilder.newRootLogger(rootLoggerConfigure.getLoggerLevel());
			rootLoggerConfigure.getAppenderNames()
					.forEach(appenderName ->
							rootLoggerComponentBuilder.add(configurationBuilder.newAppenderRef(appenderName)));
			configurationBuilder.add(rootLoggerComponentBuilder);
		}
		return configurationBuilder.build();
	}

	/**
	 * <h3 class="en-US">Generate LogConfig instance by given parameters</h3>
	 * <h3 class="zh-CN">使用给定的参数信息生成日志配置定义实例对象</h3>
	 *
	 * @param basePath       <span class="en-US">Log file base path</span>
	 *                       <span class="zh-CN">文件日志的保存目录</span>
	 * @param rootLevel      <span class="en-US">Log level</span>
	 *                       <span class="zh-CN">日志等级</span>
	 * @param packageLoggers <span class="en-US">Package logger configure array</span>
	 *                       <span class="zh-CN">包日志设置数组</span>
	 * @return <span class="en-US">Generated LogConfig instance</span>
	 * <span class="zh-CN">生成的日志配置定义实例对象</span>
	 */
	private static LogConfig generateConfig(final String basePath, final LogLevel rootLevel,
	                                        final LoggerUtils.PackageLogger... packageLoggers) {
		PatternLayoutConfigure patternLayoutConfigure = new PatternLayoutConfigure();
		Map<String, Object> layoutAttributes = new HashMap<>();
		layoutAttributes.put("pattern", "%d{yyyy-MM-dd HH:mm:ss} %p [%t] [%c:%L]: %m%n");
		patternLayoutConfigure.setAttributesMap(layoutAttributes);

		List<AppenderConfigure> appenderConfigures = new ArrayList<>();

		AppenderConfigure appenderConfigure = new AppenderConfigure("Console", "Console");
		Map<String, Object> consoleAppenderAttributes = new HashMap<>();
		consoleAppenderAttributes.put("target", ConsoleAppender.Target.SYSTEM_OUT);
		appenderConfigure.setAppenderAttributes(consoleAppenderAttributes);
		appenderConfigures.add(appenderConfigure);

		LoggerConfigure rootLoggerConfigure = new LoggerConfigure(LoggerUtils.newLogger(Globals.DEFAULT_VALUE_STRING, rootLevel));
		List<String> appenderNames;
		if (StringUtils.notBlank(basePath)) {
			FileUtils.makeDir(basePath);
			AppenderConfigure fileAppenderConfigure = new AppenderConfigure("File", "File");
			Map<String, Object> fileAppenderAttributes = new HashMap<>();
			fileAppenderAttributes.put("fileName", basePath + Globals.DEFAULT_LOG_FILE_PATH);
			fileAppenderConfigure.setAppenderAttributes(fileAppenderAttributes);
			appenderConfigures.add(fileAppenderConfigure);
			appenderNames = List.of("Console", "File");
		} else {
			appenderNames = List.of("Console");
		}
		rootLoggerConfigure.setAppenderNames(appenderNames);

		LogConfig logConfig = new LogConfig(patternLayoutConfigure);
		logConfig.setAppenderConfigures(appenderConfigures);
		logConfig.setRootLoggerConfigure(rootLoggerConfigure);
		if (packageLoggers != null) {
			List<LoggerConfigure> loggerConfigures = new ArrayList<>();
			Arrays.stream(packageLoggers)
					.filter(packageLogger -> StringUtils.notBlank(packageLogger.getPackageName()))
					.forEach(packageLogger -> {
						LoggerConfigure loggerConfigure = new LoggerConfigure(packageLogger);
						loggerConfigure.setAppenderNames(appenderNames);
						loggerConfigures.add(loggerConfigure);
					});
			logConfig.setLoggerConfigures(loggerConfigures);
		}
		return logConfig;
	}

	/**
	 * <h3 class="en-US">Initialize component configure</h3>
	 * <h3 class="zh-CN">初始化组件配置</h3>
	 *
	 * @param parentBuilder      <span class="en-US">Parent builder instance</span>
	 *                           <span class="zh-CN">上级构建器实例对象</span>
	 * @param componentConfigure <span class="en-US">Component configure</span>
	 *                           <span class="zh-CN">组件配置定义</span>
	 */
	private static void initComponentConfig(final ComponentBuilder<?> parentBuilder,
	                                        final ComponentConfigure componentConfigure) {
		if (parentBuilder == null || componentConfigure == null
				|| StringUtils.isEmpty(componentConfigure.getComponentPlugin())) {
			return;
		}
		final ComponentBuilder<?> componentBuilder =
				ConfigurationBuilderFactory.newConfigurationBuilder()
						.newComponent(componentConfigure.getComponentPlugin());
		if (componentConfigure.getComponentAttributes() != null) {
			componentConfigure.getComponentAttributes().forEach(componentBuilder::addAttribute);
		}
		if (componentConfigure.getChildComponents() != null) {
			componentConfigure.getChildComponents()
					.forEach(childComponentConfig ->
							initComponentConfig(componentBuilder, childComponentConfig));
		}
		parentBuilder.addComponent(componentBuilder);
	}

	/**
	 * <h3 class="en-US">Generate LayoutComponentBuilder by the given pattern layout configured</h3>
	 * <h3 class="zh-CN">根据给定的输出格式配置信息生成输出格式组件构建器</h3>
	 *
	 * @param patternLayoutConfigure <span class="en-US">Pattern layout configure</span>
	 *                               <span class="zh-CN">日志输出格式配置定义</span>
	 */
	private static LayoutComponentBuilder layoutBuilder(final PatternLayoutConfigure patternLayoutConfigure) {
		if (patternLayoutConfigure == null) {
			return null;
		}
		LayoutComponentBuilder layoutComponentBuilder =
				ConfigurationBuilderFactory.newConfigurationBuilder().newLayout("PatternLayout");
		if (patternLayoutConfigure.getAttributesMap() != null) {
			patternLayoutConfigure.getAttributesMap().forEach(layoutComponentBuilder::addAttribute);
		}
		if (patternLayoutConfigure.getLoggerComponents() != null) {
			patternLayoutConfigure.getLoggerComponents()
					.forEach(componentConfigure ->
							initComponentConfig(layoutComponentBuilder, componentConfigure));
		}
		return layoutComponentBuilder;
	}

	/**
	 * <h2 class="en-US">Log configure define</h2>
	 * <h2 class="zh-CN">日志配置定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2018 17:30:18 $
	 */
	private static final class LogConfig {
		/**
		 * <span class="en-US">Log print pattern layout configure</span>
		 * <span class="zh-CN">日志输出的格式设置</span>
		 */
		private final PatternLayoutConfigure patternLayoutConfigure;
		/**
		 * <span class="en-US">Logger appender configure</span>
		 * <span class="zh-CN">日志输出目标设置</span>
		 */
		private List<AppenderConfigure> appenderConfigures;
		/**
		 * <span class="en-US">Custom logger configure</span>
		 * <span class="zh-CN">自定义日志设置</span>
		 */
		private List<LoggerConfigure> loggerConfigures;
		/**
		 * <span class="en-US">Root logger configure</span>
		 * <span class="zh-CN">根日志设置</span>
		 */
		private LoggerConfigure rootLoggerConfigure;

		/**
		 * <h3 class="en-US">Private constructor for LogConfig</h3>
		 * <h3 class="zh-CN">日志配置定义的私有构造方法</h3>
		 *
		 * @param patternLayoutConfigure <span class="en-US">Log print pattern layout configure</span>
		 *                               <span class="zh-CN">日志输出的格式设置</span>
		 */
		private LogConfig(final PatternLayoutConfigure patternLayoutConfigure) {
			this.patternLayoutConfigure = patternLayoutConfigure;
		}

		/**
		 * <h3 class="en-US">Getter method for Log print pattern layout configure</h3>
		 * <h3 class="zh-CN">日志输出的格式设置的Getter方法</h3>
		 *
		 * @return <span class="en-US">Log print pattern layout configure</span>
		 * <span class="zh-CN">日志输出的格式设置</span>
		 */
		public PatternLayoutConfigure getPatternLayoutConfigure() {
			return patternLayoutConfigure;
		}

		/**
		 * <h3 class="en-US">Getter method for Logger appender configure</h3>
		 * <h3 class="zh-CN">日志输出目标设置的Getter方法</h3>
		 *
		 * @return <span class="en-US">Logger appender configure</span>
		 * <span class="zh-CN">日志输出目标设置</span>
		 */
		public List<AppenderConfigure> getAppenderConfigures() {
			return appenderConfigures;
		}

		/**
		 * <h3 class="en-US">Setter method for Logger appender configure</h3>
		 * <h3 class="zh-CN">日志输出目标设置的Setter方法</h3>
		 *
		 * @param appenderConfigures <span class="en-US">Logger appender configure</span>
		 *                           <span class="zh-CN">日志输出目标设置</span>
		 */
		public void setAppenderConfigures(final List<AppenderConfigure> appenderConfigures) {
			this.appenderConfigures = appenderConfigures;
		}

		/**
		 * <h3 class="en-US">Getter method for Custom logger configure</h3>
		 * <h3 class="zh-CN">自定义日志设置的Getter方法</h3>
		 *
		 * @return <span class="en-US">Custom logger configure</span>
		 * <span class="zh-CN">自定义日志设置</span>
		 */
		public List<LoggerConfigure> getLoggerConfigures() {
			return loggerConfigures;
		}

		/**
		 * <h3 class="en-US">Setter method for Custom logger configure</h3>
		 * <h3 class="zh-CN">自定义日志设置的Setter方法</h3>
		 *
		 * @param loggerConfigures <span class="en-US">Custom logger configure</span>
		 *                         <span class="zh-CN">自定义日志设置</span>
		 */
		public void setLoggerConfigures(final List<LoggerConfigure> loggerConfigures) {
			this.loggerConfigures = loggerConfigures;
		}

		/**
		 * <h3 class="en-US">Getter method for Root logger configure</h3>
		 * <h3 class="zh-CN">根日志设置的Getter方法</h3>
		 *
		 * @return <span class="en-US">Root logger configure</span>
		 * <span class="zh-CN">根日志设置</span>
		 */
		public LoggerConfigure getRootLoggerConfigure() {
			return rootLoggerConfigure;
		}

		/**
		 * <h3 class="en-US">Setter method for Root logger configure</h3>
		 * <h3 class="zh-CN">根日志设置的Setter方法</h3>
		 *
		 * @param rootLoggerConfigure <span class="en-US">Root logger configure</span>
		 *                            <span class="zh-CN">根日志设置</span>
		 */
		public void setRootLoggerConfigure(final LoggerConfigure rootLoggerConfigure) {
			this.rootLoggerConfigure = rootLoggerConfigure;
		}
	}

	/**
	 * <h2 class="en-US">Pattern layout configure define</h2>
	 * <h2 class="zh-CN">日志输出格式配置定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2018 17:31:06 $
	 */
	private static final class PatternLayoutConfigure {
		/**
		 * <span class="en-US">Attribute configure map</span>
		 * <span class="zh-CN">参数设置映射</span>
		 */
		private Map<String, Object> attributesMap;
		/**
		 * <span class="en-US">Logger component list</span>
		 * <span class="zh-CN">日志组件列表</span>
		 */
		private List<ComponentConfigure> loggerComponents;

		/**
		 * <h3 class="en-US">Private constructor for PatternLayoutConfigure</h3>
		 * <h3 class="zh-CN">日志输出格式配置定义的私有构造方法</h3>
		 */
		private PatternLayoutConfigure() {
		}

		/**
		 * <h3 class="en-US">Getter method for the attribute configure map</h3>
		 * <h3 class="zh-CN">参数设置映射的Getter方法</h3>
		 *
		 * @return <span class="en-US">Attribute configure map</span>
		 * <span class="zh-CN">参数设置映射</span>
		 */
		public Map<String, Object> getAttributesMap() {
			return attributesMap;
		}

		/**
		 * <h3 class="en-US">Setter method for the attribute configure map</h3>
		 * <h3 class="zh-CN">参数设置映射的Setter方法</h3>
		 *
		 * @param attributesMap <span class="en-US">Attribute configure map</span>
		 *                      <span class="zh-CN">参数设置映射</span>
		 */
		public void setAttributesMap(final Map<String, Object> attributesMap) {
			this.attributesMap = attributesMap;
		}

		/**
		 * <h3 class="en-US">Getter method for Logger component list</h3>
		 * <h3 class="zh-CN">日志组件列表的Getter方法</h3>
		 *
		 * @return <span class="en-US">Logger component list</span>
		 * <span class="zh-CN">日志组件列表</span>
		 */
		public List<ComponentConfigure> getLoggerComponents() {
			return loggerComponents;
		}

		/**
		 * <h3 class="en-US">Setter method for Logger component list</h3>
		 * <h3 class="zh-CN">日志组件列表的Setter方法</h3>
		 *
		 * @param loggerComponents <span class="en-US">Logger component list</span>
		 *                         <span class="zh-CN">日志组件列表</span>
		 */
		public void setLoggerComponents(final List<ComponentConfigure> loggerComponents) {
			this.loggerComponents = loggerComponents;
		}
	}

	/**
	 * <h2 class="en-US">Appender configure define</h2>
	 * <h2 class="zh-CN">日志输出目标配置定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2018 17:37:22 $
	 */
	private static final class AppenderConfigure {
		/**
		 * <span class="en-US">Appender name</span>
		 * <span class="zh-CN">日志输出目标名称</span>
		 */
		private final String appenderName;
		/**
		 * <span class="en-US">Appender plugin name</span>
		 * <span class="zh-CN">日志输出目标插件名</span>
		 */
		private final String appenderPlugin;
		/**
		 * <span class="en-US">Appender attributes map</span>
		 * <span class="zh-CN">日志输出目标属性映射</span>
		 */
		private Map<String, Object> appenderAttributes;
		/**
		 * <span class="en-US">Appender component list</span>
		 * <span class="zh-CN">日志输出目标组件列表</span>
		 */
		private List<ComponentConfigure> appenderComponents;
		/**
		 * <span class="en-US">Appender pattern layout configure</span>
		 * <span class="zh-CN">日志输出目标格式定义</span>
		 */
		private PatternLayoutConfigure patternLayoutConfigure;

		/**
		 * <h3 class="en-US">Private constructor for AppenderConfigure</h3>
		 * <h3 class="zh-CN">日志输出目标配置定义的私有构造方法</h3>
		 *
		 * @param appenderName   <span class="en-US">Appender name</span>
		 *                       <span class="zh-CN">日志输出目标名称</span>
		 * @param appenderPlugin <span class="en-US">Appender plugin name</span>
		 *                       <span class="zh-CN">日志输出目标插件名</span>
		 */
		private AppenderConfigure(final String appenderName, final String appenderPlugin) {
			this.appenderName = appenderName;
			this.appenderPlugin = appenderPlugin;
		}

		/**
		 * <h3 class="en-US">Getter method for appender name</h3>
		 * <h3 class="zh-CN">日志输出目标名称的Getter方法</h3>
		 *
		 * @return <span class="en-US">Appender name</span>
		 * <span class="zh-CN">日志输出目标名称</span>
		 */
		public String getAppenderName() {
			return appenderName;
		}

		/**
		 * <h3 class="en-US">Getter method for appender plugin name</h3>
		 * <h3 class="zh-CN">日志输出目标插件名称的Getter方法</h3>
		 *
		 * @return <span class="en-US">Appender plugin name</span>
		 * <span class="zh-CN">日志输出目标插件名称</span>
		 */
		public String getAppenderPlugin() {
			return appenderPlugin;
		}

		/**
		 * <h3 class="en-US">Getter method for appender attributes map</h3>
		 * <h3 class="zh-CN">日志输出目标属性映射的Getter方法</h3>
		 *
		 * @return <span class="en-US">Appender attributes map</span>
		 * <span class="zh-CN">日志输出目标属性映射</span>
		 */
		public Map<String, Object> getAppenderAttributes() {
			return appenderAttributes;
		}

		/**
		 * <h3 class="en-US">Setter method for appender attributes map</h3>
		 * <h3 class="zh-CN">日志输出目标属性映射的Setter方法</h3>
		 *
		 * @param appenderAttributes <span class="en-US">Appender attributes map</span>
		 *                           <span class="zh-CN">日志输出目标属性映射</span>
		 */
		public void setAppenderAttributes(final Map<String, Object> appenderAttributes) {
			this.appenderAttributes = appenderAttributes;
		}

		/**
		 * <h3 class="en-US">Getter method for the appender component list</h3>
		 * <h3 class="zh-CN">日志输出目标组件列表的Getter方法</h3>
		 *
		 * @return <span class="en-US">Appender component list</span>
		 * <span class="zh-CN">日志输出目标组件列表</span>
		 */
		public List<ComponentConfigure> getAppenderComponents() {
			return appenderComponents;
		}

		/**
		 * <h3 class="en-US">Setter method for the appender component list</h3>
		 * <h3 class="zh-CN">日志输出目标组件列表的Setter方法</h3>
		 *
		 * @param appenderComponents the appender components
		 *                           <span class="en-US">Appender component list</span>
		 *                           <span class="zh-CN">日志输出目标组件列表</span>
		 */
		public void setAppenderComponents(final List<ComponentConfigure> appenderComponents) {
			this.appenderComponents = appenderComponents;
		}

		/**
		 * <h3 class="en-US">Getter method for appender pattern layout configure</h3>
		 * <h3 class="zh-CN">日志输出目标格式定义的Getter方法</h3>
		 *
		 * @return <span class="en-US">Appender pattern layout configure</span>
		 * <span class="zh-CN">日志输出目标格式定义</span>
		 */
		public PatternLayoutConfigure getPatternLayoutConfigure() {
			return patternLayoutConfigure;
		}

		/**
		 * <h3 class="en-US">Setter method for appender pattern layout configure</h3>
		 * <h3 class="zh-CN">日志输出目标格式定义的Setter方法</h3>
		 *
		 * @param patternLayoutConfigure <span class="en-US">Appender pattern layout configure</span>
		 *                               <span class="zh-CN">日志输出目标格式定义</span>
		 */
		public void setPatternLayoutConfigure(final PatternLayoutConfigure patternLayoutConfigure) {
			this.patternLayoutConfigure = patternLayoutConfigure;
		}
	}

	/**
	 * <h2 class="en-US">Logger configure define</h2>
	 * <h2 class="zh-CN">日志配置定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2018 17:43:57 $
	 */
	private static final class LoggerConfigure {
		/**
		 * <span class="en-US">Package name</span>
		 * <span class="zh-CN">包名</span>
		 */
		private final String packageName;
		/**
		 * <span class="en-US">Log level</span>
		 * <span class="zh-CN">日志等级</span>
		 */
		private final Level loggerLevel;
		/**
		 * <span class="en-US">Log appender name list</span>
		 * <span class="zh-CN">日志输出目标名称列表</span>
		 */
		private List<String> appenderNames;

		/**
		 * <h3 class="en-US">Private constructor for LoggerConfigure</h3>
		 * <h3 class="zh-CN">日志配置定义的私有构造方法</h3>
		 *
		 * @param packageLogger <span class="en-US">Package logger define</span>
		 *                      <span class="zh-CN">包日志定义</span>
		 */
		private LoggerConfigure(final LoggerUtils.PackageLogger packageLogger) {
			this.packageName = packageLogger.getPackageName();
			switch (packageLogger.getLoggerLevel()) {
				case FATAL:
					this.loggerLevel = Level.FATAL;
					break;
				case ERROR:
					this.loggerLevel = Level.ERROR;
					break;
				case WARN:
					this.loggerLevel = Level.WARN;
					break;
				case INFO:
					this.loggerLevel = Level.INFO;
					break;
				case DEBUG:
					this.loggerLevel = Level.DEBUG;
					break;
				case TRACE:
					this.loggerLevel = Level.TRACE;
					break;
				case ALL:
					this.loggerLevel = Level.ALL;
					break;
				default:
					this.loggerLevel = Level.OFF;
					break;
			}
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
		public Level getLoggerLevel() {
			return loggerLevel;
		}

		/**
		 * <h3 class="en-US">Getter method for log appender name list</h3>
		 * <h3 class="zh-CN">日志输出目标名称列表的Getter方法</h3>
		 *
		 * @return <span class="en-US">Log appender name list</span>
		 * <span class="zh-CN">日志输出目标名称列表</span>
		 */
		public List<String> getAppenderNames() {
			return appenderNames;
		}

		/**
		 * <h3 class="en-US">Setter method for log appender name list</h3>
		 * <h3 class="zh-CN">日志输出目标名称列表的Setter方法</h3>
		 *
		 * @param appenderNames <span class="en-US">Log appender name list</span>
		 *                      <span class="zh-CN">日志输出目标名称列表</span>
		 */
		public void setAppenderNames(final List<String> appenderNames) {
			this.appenderNames = appenderNames;
		}
	}

	/**
	 * <h2 class="en-US">Component configure define</h2>
	 * <h2 class="zh-CN">组件配置定义</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Sep 15, 2018 17:55:19 $
	 */
	private static final class ComponentConfigure {
		/**
		 * <span class="en-US">Component plugin name</span>
		 * <span class="zh-CN">组件插件名称</span>
		 */
		private final String componentPlugin;
		/**
		 * <span class="en-US">Component attributes map</span>
		 * <span class="zh-CN">组件属性映射</span>
		 */
		private Map<String, Object> componentAttributes;
		/**
		 * <span class="en-US">Child component configure list</span>
		 * <span class="zh-CN">子组件配置信息列表</span>
		 */
		private List<ComponentConfigure> childComponents;

		/**
		 * <h3 class="en-US">Private constructor for ComponentConfigure</h3>
		 * <h3 class="zh-CN">组件配置定义的私有构造方法</h3>
		 *
		 * @param componentPlugin <span class="en-US">Component plugin name</span>
		 *                        <span class="zh-CN">组件插件名称</span>
		 */
		private ComponentConfigure(final String componentPlugin) {
			this.componentPlugin = componentPlugin;
			this.componentAttributes = new HashMap<>();
			this.childComponents = new ArrayList<>();
		}

		/**
		 * <h3 class="en-US">Getter method for component plugin name</h3>
		 * <h3 class="zh-CN">组件插件名称的Getter方法</h3>
		 *
		 * @return <span class="en-US">Component plugin name</span>
		 * <span class="zh-CN">组件插件名称</span>
		 */
		public String getComponentPlugin() {
			return componentPlugin;
		}

		/**
		 * <h3 class="en-US">Getter method for the component attributes map</h3>
		 * <h3 class="zh-CN">组件属性映射的Getter方法</h3>
		 *
		 * @return <span class="en-US">Component attributes map</span>
		 * <span class="zh-CN">组件属性映射</span>
		 */
		public Map<String, Object> getComponentAttributes() {
			return componentAttributes;
		}

		/**
		 * <h3 class="en-US">Setter method for the component attributes map</h3>
		 * <h3 class="zh-CN">组件属性映射的Setter方法</h3>
		 *
		 * @param componentAttributes <span class="en-US">Component attributes map</span>
		 *                            <span class="zh-CN">组件属性映射</span>
		 */
		public void setComponentAttributes(final Map<String, Object> componentAttributes) {
			this.componentAttributes = componentAttributes;
		}

		/**
		 * <h3 class="en-US">Getter method for the child component configure list</h3>
		 * <h3 class="zh-CN">子组件配置信息列表的Getter方法</h3>
		 *
		 * @return <span class="en-US">Child component configure list</span>
		 * <span class="zh-CN">子组件配置信息列表</span>
		 */
		public List<ComponentConfigure> getChildComponents() {
			return childComponents;
		}

		/**
		 * <h3 class="en-US">Setter method for the child component configure list</h3>
		 * <h3 class="zh-CN">子组件配置信息列表的Setter方法</h3>
		 *
		 * @param childComponents <span class="en-US">Child component configure list</span>
		 *                        <span class="zh-CN">子组件配置信息列表</span>
		 */
		public void setChildComponents(final List<ComponentConfigure> childComponents) {
			this.childComponents = childComponents;
		}
	}
}
