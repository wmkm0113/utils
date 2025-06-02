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
package org.nervousync.beans.transfer.basic;

import org.nervousync.beans.transfer.AbstractAdapter;
import org.nervousync.commons.Globals;
import org.nervousync.utils.DateTimeUtils;
import org.nervousync.utils.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

/**
 * <h2 class="en-US">DateTime convert adapter</h2>
 * <h2 class="zh-CN">日期时间数据转换器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.0 $Date: 2018-10-15 14:31
 */
public final class DateTimeAdapter extends AbstractAdapter {

	/**
	 * <span class="en-US">Date time format pattern</span>
	 * <span class="zh-CN">日期时间格式字符串</span>
	 */
	private final String pattern;
	private final DateTimeFormatter formatter;

	/**
	 * <h3 class="en-US">Constructor method for dateTime convert adapter, using the default datetime pattern (Defined in ISO8601)</h3>
	 * <h3 class="zh-CN">日期时间数据转换器的构造方法，使用默认的格式字符串（ISO8601标准定义的格式）</h3>
	 */
	public DateTimeAdapter() {
		this(DateTimeUtils.DEFAULT_DATETIME_PATTERN_ISO8601);
	}

	/**
	 * <h3 class="en-US">Constructor method for dateTime convert adapter</h3>
	 * <h3 class="zh-CN">日期时间数据转换器的构造方法</h3>
	 *
	 * @param pattern <span class="en-US">Date time format pattern</span>
	 *                <span class="zh-CN">日期时间格式字符串</span>
	 */
	public DateTimeAdapter(final String pattern) {
		this.pattern = StringUtils.isEmpty(pattern) ? DateTimeUtils.DEFAULT_DATETIME_PATTERN_ISO8601 : pattern;
		this.formatter = DateTimeFormatter.ofPattern(this.pattern);
	}

	@Override
	public Object unmarshal(final String v) {
		return DateTimeUtils.parseDate(v, this.pattern);
	}

	@Override
	public String marshal(final Object v) {
		return Optional.ofNullable(v)
				.filter(date -> date instanceof Date)
				.map(date -> DateTimeUtils.formatDate((Date) v, this.formatter))
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}
}
