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

package org.nervousync.xml.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.nervousync.commons.Globals;
import org.nervousync.utils.DateTimeUtils;

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
public final class DateTimeAdapter extends XmlAdapter<String, Date> {

    /**
     * <span class="en-US">Date time formatter instance object</span>
     * <span class="zh-CN">日期时间格式化转换器实例对象</span>
     */
    private final DateTimeFormatter formatter;

    /**
     * <h3 class="en-US">Constructor method for dateTime convert adapter, using the default datetime pattern (Defined in ISO8601)</h3>
     * <h3 class="zh-CN">日期时间数据转换器的构造方法，使用默认的格式字符串（ISO8601标准定义的格式）</h3>
     */
    public DateTimeAdapter() {
        this.formatter = DateTimeFormatter.ofPattern(DateTimeUtils.DEFAULT_DATETIME_PATTERN_ISO8601);
    }

    @Override
    public Date unmarshal(final String string) {
        return DateTimeUtils.parseDate(string, DateTimeUtils.DEFAULT_DATETIME_PATTERN_ISO8601);
    }

    @Override
    public String marshal(final Date v) {
        return Optional.ofNullable(v)
                .map(date -> DateTimeUtils.formatDate(date, this.formatter))
                .orElse(Globals.DEFAULT_VALUE_STRING);
    }
}
