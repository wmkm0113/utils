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
package org.nervousync.beans.transfer;

/**
 * <h2 class="en-US">Abstract adapter for data transfer</h2>
 * <h2 class="zh-CN">数据转换抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.0 $ $Date: Jun 21, 2023 10:25:22 $
 */
@SuppressWarnings("RedundantThrows")
public abstract class TransferAdapter {

    /**
     * <h3 class="en-US">Convert the given data to the target data</h3>
     * <h3 class="zh-CN">转换给定的数据为目标数据</h3>
     *
     * @param v <span class="en-US">Given data object</span>
     *          <span class="zh-CN">给定数据值</span>
     * @return <span class="en-US">Converted data object</span>
     * <span class="zh-CN">转换后的数据</span>
     */
    public abstract Object unmarshal(final Object v) throws Exception;

    /**
     * <h3 class="en-US">Convert the given data to the target data</h3>
     * <h3 class="zh-CN">转换给定的数据为目标数据</h3>
     *
     * @param v <span class="en-US">Given data object</span>
     *          <span class="zh-CN">给定数据值</span>
     * @return <span class="en-US">Converted data object</span>
     * <span class="zh-CN">转换后的数据</span>
     */
    public abstract Object marshal(final Object v) throws Exception;
}
