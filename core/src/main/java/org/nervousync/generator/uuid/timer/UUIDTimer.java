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
package org.nervousync.generator.uuid.timer;

/**
 * <h2 class="en-US">Interface of UUID timer, using for time-based UUID generator</h2>
 * <h2 class="zh-CN">时间同步器接口，用于基于时间的UUID生成器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.1 $ $Date: Jul 06, 2022 12:54:12 $
 */
@SuppressWarnings("EmptyMethod")
public interface UUIDTimer {

	/**
	 * <h3 class="en-US">Read timestamp value</h3>
	 * <h3 class="zh-CN">读取时间戳值</h3>
	 *
	 * @return <span class="en-US">Timestamp value</span>
	 * <span class="zh-CN">时间戳值</span>
	 */
	long timestamp();

	/**
	 * <h3 class="en-US">Deactivate current time synchronizer</h3>
	 * <h3 class="zh-CN">反激活当前时间同步器</h3>
	 */
	void destroy();

}
