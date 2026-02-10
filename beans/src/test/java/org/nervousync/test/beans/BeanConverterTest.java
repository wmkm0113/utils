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

package org.nervousync.test.beans;

import org.junit.jupiter.api.*;
import org.nervousync.beans.transfers.basic.BigDecimalAdapter;
import org.nervousync.beans.transfers.basic.BigIntegerAdapter;
import org.nervousync.beans.transfers.beans.TransferBeanAdapter;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.ClassUtils;

public class BeanConverterTest extends BaseTest {

    @Test
    @Order(0)
    public void testConverter() {
        printTypes(BigDecimalAdapter.class);
        printTypes(BigIntegerAdapter.class);
        printTypes(TransferBeanAdapter.class);
    }
    
    private void printTypes(final Class<?> clazz) {
        StringBuilder stringBuilder = new StringBuilder("Class name: ").append(clazz.getName()).append(" component types: ");
        for (Class<?> type : ClassUtils.componentTypes(clazz)) {
            stringBuilder.append(type.getName()).append(",");
        }
        System.out.println(stringBuilder);
    }
}
