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

package org.nervousync.enumerations.logger;

/**
 * <h2 class="en-US">Log level enumeration value</h2>
 * <h2 class="zh-CN">日志等级枚举类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Feb 10, 2026 11:52:05 $
 */
public enum LogLevel {

	/**
	 * <span class="en-US">No events will be logged.</span>
	 * <span class="zh-CN">不会记录任何事件。</span>
	 */
	OFF,

	/**
	 * <span class="en-US">A fatal event that will prevent the application from continuing.</span>
	 * <span class="zh-CN">导致申请无法继续进行的致命事件。</span>
	 */
	FATAL,

	/**
	 * <span class="en-US">An error in the application, possibly recoverable.</span>
	 * <span class="zh-CN">应用程序出现错误，可能可以恢复。</span>
	 */
	ERROR,

	/**
	 * <span class="en-US">An event that might possible lead to an error.</span>
	 * <span class="zh-CN">可能导致错误的事件。</span>
	 */
	WARN,

	/**
	 * <span class="en-US">An event for informational purposes.</span>
	 * <span class="zh-CN">仅记录通知事件。</span>
	 */
	INFO,

	/**
	 * <span class="en-US">A general debugging event.</span>
	 * <span class="zh-CN">一个通用调试事件。</span>
	 */
	DEBUG,

	/**
	 * <span class="en-US">A fine-grained debug message, typically capturing the flow through the application.</span>
	 * <span class="zh-CN">一条细粒度的调试消息，通常用于捕获应用程序的流程。</span>
	 */
	TRACE,

	/**
	 * <span class="en-US">All events should be logged.</span>
	 * <span class="zh-CN">记录所有事件。</span>
	 */
	ALL
}
