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

package org.nervousync.logger;

import org.nervousync.enumerations.logger.LogLevel;
import org.nervousync.utils.logger.LoggerUtils;

/**
 * <h2 class="en-US">Log configurator interface</h2>
 * <h2 class="zh-CN">日志配置器接口</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Feb 10, 2026 11:51:18 $
 */
public interface LogConfigurator {

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
	void initLogger(final String basePath, final LogLevel rootLevel, final LoggerUtils.PackageLogger... packageLoggers);
}
