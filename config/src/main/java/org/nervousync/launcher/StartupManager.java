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
package org.nervousync.launcher;

import jakarta.annotation.Nonnull;
import org.nervousync.annotations.launcher.Launcher;
import org.nervousync.annotations.provider.Provider;
import org.nervousync.beans.launcher.LauncherConfig;
import org.nervousync.beans.launcher.StartupConfig;
import org.nervousync.commons.Globals;
import org.nervousync.configs.ConfigureManager;
import org.nervousync.enumerations.launcher.StartupType;
import org.nervousync.utils.core.ClassUtils;
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.core.ObjectUtils;
import org.nervousync.utils.core.SystemUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * <h2 class="en-US">Startup Manager</h2>
 * <span class="en-US">Running in singleton mode</span>
 * <h2 class="zh-CN">启动管理器</h2>
 * <span class="en-US">使用单例模式运行</span>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jun 28, 2022 15:25:33 $
 */
@SuppressWarnings("unused")
public final class StartupManager {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(StartupManager.class);

	/**
	 * <span class="en-US">Schedule task execution interval</span>
	 * <span class="en-US">调度任务执行间隔时间</span>
	 */
	private static final long SCHEDULE_PERIOD = 30 * 1000L;
	/**
	 * <span class="en-US">Singleton instance of StartupManager</span>
	 * <span class="en-US">StartupManager的单例对象实例</span>
	 */
	private static StartupManager INSTANCE = null;
	/**
	 * <span class="en-US">Startup manager configure instance</span>
	 * <span class="en-US">启动器配置信息实例对象</span>
	 */
	private final StartupConfig startupConfig;
	/**
	 * <span class="en-US">Registered launcher instance object</span>
	 * <span class="en-US">注册的启动器实例对象</span>
	 */
	private final Map<String, StartupLauncher> registeredLaunchers = new HashMap<>();
	/**
	 * <span class="en-US">Schedule executor for update startup launcher configure</span>
	 * <span class="en-US">启动器配置信息更新调度程序</span>
	 */
	private final ScheduledExecutorService scheduledExecutorService;
	/**
	 * <span class="en-US">Schedule task running status</span>
	 * <span class="en-US">调度任务执行状态</span>
	 */
	private boolean running = Boolean.FALSE;

	/**
	 * <h3 class="en-US">Private constructor method for StartupManager</h3>
	 * <h3 class="zh-CN">启动管理器的私有构造方法</h3>
	 */
	private StartupManager(final StartupConfig startupConfig) {
		this.startupConfig = (startupConfig == null) ? new StartupConfig() : startupConfig;
		this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		this.scheduledExecutorService.scheduleWithFixedDelay(this::scanConfig, Globals.DEFAULT_SCHEDULE_DELAY,
				SCHEDULE_PERIOD, TimeUnit.MILLISECONDS);
		this.startupConfig.getRegisteredLaunchers()
				.stream()
				.filter(launcherConfig -> StartupType.AUTO.equals(launcherConfig.getStartupType()))
				.forEach(this::startLauncher);
	}

	/**
	 * <h3 class="en-US">Static method for initialize StartupManager and execute startup method from all launchers</h3>
	 * <h3 class="zh-CN">静态方法用于初始化启动管理器并执行所有启动器的startup方法</h3>
	 */
	public static void initialize() {
		if (StartupManager.INSTANCE == null) {
			StartupManager.INSTANCE =
					Optional.ofNullable(ConfigureManager.getInstance())
							.map(configureManager -> configureManager.readConfigure(StartupConfig.class))
							.map(StartupManager::new)
							.orElse(null);
			SystemUtils.registerShutdownHook(StartupManager::shutdown);
		}
	}

	/**
	 * <h3 class="en-US">Get instance of startup manager</h3>
	 * <h3 class="zh-CN">获取启动管理器实例对象</h3>
	 *
	 * @return <span class="en-US">Startup manager instance</span>
	 * <span class="en-US">启动管理器实例对象</span>
	 */
	public static StartupManager getInstance() {
		if (StartupManager.INSTANCE == null) {
			initialize();
		}
		return StartupManager.INSTANCE;
	}

	/**
	 * <h3 class="en-US">Get the registered launcher configured information list</h3>
	 * <h3 class="zh-CN">获取已注册的启动器配置信息列表</h3>
	 *
	 * @return <span class="en-US">Launcher configure information list</span>
	 * <span class="en-US">启动器配置信息列表</span>
	 */
	public List<LauncherConfig> registeredLaunchers() {
		return this.startupConfig.getRegisteredLaunchers();
	}

	/**
	 * <h3 class="en-US">Destroy instance of startup manager</h3>
	 * <h3 class="zh-CN">销毁当前启动器的实例对象</h3>
	 */
	private static void shutdown() {
		if (StartupManager.INSTANCE != null) {
			StartupManager.INSTANCE.destroy();
			StartupManager.INSTANCE = null;
		}
	}

	/**
	 * <h3 class="en-US">Update startup type by the given class name of the startup launcher</h3>
	 * <h3 class="zh-CN">修改启动器的启动类型</h3>
	 *
	 * @param className   <span class="en-US">Launcher class name</span>
	 *                    <span class="en-US">启动器类名</span>
	 * @param startupType <span class="en-US">Startup type</span>
	 *                    <span class="en-US">启动类型</span>
	 */
	public void update(final String className, final StartupType startupType) {
		LOGGER.debug("Startup_Manager_Update", className, startupType);
		final AtomicBoolean modified = new AtomicBoolean(Boolean.FALSE);
		long startTime = DateTimeUtils.currentUTCTimeMillis();

		while (true) {
			if (!this.running) {
				this.running = Boolean.TRUE;
				break;
			}
			if (1000L < (DateTimeUtils.currentUTCTimeMillis() - startTime)) {
				return;
			}
		}
		List<LauncherConfig> registeredLaunchers = this.startupConfig.getRegisteredLaunchers();
		registeredLaunchers.replaceAll(launcherConfig -> {
			if (ObjectUtils.nullSafeEquals(className, launcherConfig.getLauncherClass())
					&& !ObjectUtils.nullSafeEquals(launcherConfig.getStartupType(), startupType)) {
				launcherConfig.setStartupType(startupType);
				modified.set(Boolean.TRUE);
			}
			return launcherConfig;
		});

		if (modified.get()) {
			this.startupConfig.setRegisteredLaunchers(registeredLaunchers);
			this.saveConfig();
		}

		this.running = Boolean.FALSE;
	}

	/**
	 * <h3 class="en-US">Remove registered startup launcher</h3>
	 * <h3 class="zh-CN">删除注册的启动器</h3>
	 *
	 * @param className   <span class="en-US">Launcher class name</span>
	 *                    <span class="en-US">启动器类名</span>
	 */
	public void remove(final String className) {
		LOGGER.debug("Startup_Manager_Remove", className);
		StartupLauncher startupLauncher = this.registeredLaunchers.get(className);
		if (startupLauncher != null) {
			startupLauncher.stop();
			startupLauncher.destroy();
		}
		this.registeredLaunchers.remove(className);
		List<LauncherConfig> registeredLaunchers = this.startupConfig.getRegisteredLaunchers();
		if (registeredLaunchers.removeIf(launcherConfig ->
				ObjectUtils.nullSafeEquals(className, launcherConfig.getLauncherClass()))) {
			this.startupConfig.setRegisteredLaunchers(registeredLaunchers);
			this.saveConfig();
		}
	}

	/**
	 * <h3 class="en-US">Start registered launcher</h3>
	 * <h3 class="zh-CN">启动注册的启动器</h3>
	 *
	 * @param className <span class="en-US">Launcher class name</span>
	 *                  <span class="en-US">启动器类名</span>
	 */
	public void startup(final String className) {
		LOGGER.debug("Startup_Manager_Start", className);
		this.startupConfig.getRegisteredLaunchers()
				.stream()
				.filter(launcherConfig ->
						ObjectUtils.nullSafeEquals(className, launcherConfig.getLauncherClass()))
				.filter(launcherConfig -> !StartupType.DISABLE.equals(launcherConfig.getStartupType()))
				.forEach(this::startLauncher);
	}

	/**
	 * <h3 class="en-US">Stop registered launcher</h3>
	 * <h3 class="zh-CN">停止注册的启动器</h3>
	 *
	 * @param className <span class="en-US">Launcher class name</span>
	 *                  <span class="en-US">启动器类名</span>
	 */
	public void stop(final String className) {
		LOGGER.debug("Startup_Manager_Stop", className);
		StartupLauncher startupLauncher = this.registeredLaunchers.get(className);
		if (startupLauncher != null && startupLauncher.isRunning()) {
			startupLauncher.stop();
		}
	}

	/**
	 * <h3 class="en-US">Restart registered launcher</h3>
	 * <h3 class="zh-CN">重启注册的启动器</h3>
	 *
	 * @param className <span class="en-US">Launcher class name</span>
	 *                  <span class="en-US">启动器类名</span>
	 */
	public void restart(final String className) {
		LOGGER.debug("Startup_Manager_Restart", className);
		this.startupConfig.getRegisteredLaunchers()
				.stream()
				.filter(launcherConfig ->
						ObjectUtils.nullSafeEquals(className, launcherConfig.getLauncherClass()))
				.filter(launcherConfig -> !StartupType.DISABLE.equals(launcherConfig.getStartupType()))
				.forEach(launcherConfig -> {
					StartupLauncher startupLauncher = this.registeredLaunchers.get(launcherConfig.getLauncherClass());
					if (startupLauncher != null) {
						if (startupLauncher.isRunning()) {
							startupLauncher.stop();
						}
						startupLauncher.startup();
					}
				});
	}

	/**
	 * <h3 class="en-US">Start registered launcher</h3>
	 * <h3 class="zh-CN">启动注册的启动器</h3>
	 *
	 * @param launcherConfig <span class="en-US">Launcher configure information instance</span>
	 *                       <span class="en-US">启动器配置信息实例对象</span>
	 */
	private void startLauncher(@Nonnull final LauncherConfig launcherConfig) {
		StartupLauncher startupLauncher = this.registeredLaunchers.get(launcherConfig.getLauncherClass());
		if (startupLauncher != null && !startupLauncher.isRunning()) {
			startupLauncher.startup();
		}
	}

	/**
	 * <h3 class="en-US">Schedule task, using for checking launcher configure modified</h3>
	 * <h3 class="zh-CN">调度任务，用于扫描系统中启动器的修改</h3>
	 */
	private void scanConfig() {
		if (this.running) {
			return;
		}

		this.running = Boolean.TRUE;

		List<String> scannedClasses = new ArrayList<>();
		AtomicBoolean modified = new AtomicBoolean(Boolean.FALSE);
		List<LauncherConfig> registeredLaunchers = this.startupConfig.getRegisteredLaunchers();
		ServiceLoader.load(StartupLauncher.class).forEach(startupLauncher -> {
			Class<?> launcherClass = startupLauncher.getClass();
			if (launcherClass.isAnnotationPresent(Provider.class) && launcherClass.isAnnotationPresent(Launcher.class)) {
				String className = ClassUtils.originalClassName(launcherClass);
				scannedClasses.add(className);
				if (this.registeredLaunchers.containsKey(className)) {
					return;
				}
				if (registeredLaunchers.stream().noneMatch(launcherConfig ->
						ObjectUtils.nullSafeEquals(className, launcherConfig.getLauncherClass()))) {
					Launcher launcher = launcherClass.getAnnotation(Launcher.class);
					LauncherConfig launcherConfig = new LauncherConfig();
					launcherConfig.setLauncherClass(className);
					launcherConfig.setStartupType(launcher.value());
					registeredLaunchers.add(launcherConfig);

					modified.set(Boolean.TRUE);
				}
			}
		});

		List<String> removedLaunchers = this.registeredLaunchers
				.keySet()
				.stream()
				.filter(className -> !scannedClasses.contains(className))
				.collect(Collectors.toList());

		for (String className : removedLaunchers) {
			StartupLauncher startupLauncher = this.registeredLaunchers.get(className);
			startupLauncher.stop();
			startupLauncher.destroy();
			this.registeredLaunchers.remove(className);
			if (registeredLaunchers.removeIf(launcherConfig ->
					ObjectUtils.nullSafeEquals(className, launcherConfig.getLauncherClass()))) {
				modified.set(Boolean.TRUE);
			}
		}

		if (modified.get()) {
			this.startupConfig.setRegisteredLaunchers(registeredLaunchers);
			this.saveConfig();
		}

		this.running = Boolean.FALSE;
	}

	private void saveConfig() {
		this.startupConfig.setLastModify(DateTimeUtils.currentUTCTimeMillis());
		Optional.ofNullable(ConfigureManager.getInstance())
				.ifPresent(configureManager -> {
					if (!configureManager.saveConfigure(this.startupConfig)) {
						LOGGER.warn("Startup_Manager_Save_Config_Error");
					}
				});
	}

	/**
	 * <h3 class="en-US">Destroy all registered launcher instance</h3>
	 * <h3 class="zh-CN">销毁所有已注册的启动器实例</h3>
	 */
	private void destroy() {
		this.scheduledExecutorService.shutdown();
		this.registeredLaunchers.forEach((className, startupLauncher) -> {
			if (startupLauncher.isRunning()) {
				startupLauncher.stop();
			}
			startupLauncher.destroy();
		});
		this.registeredLaunchers.clear();
	}
}
