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

package org.nervousync.commons.i18n;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.nervousync.beans.i18n.BundleResource;

import java.io.IOException;
import java.io.InputStream;

public final class InternationalizationGlobals {


	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

	/**
	 * <span class="en-US">Storage path for multilingual resource files</span>
	 * <span class="zh-CN">多语言资源文件存储路径</span>
	 */
	public static final String BUNDLE_RESOURCE_PATH = "META-INF/i18n/Resources.json";

	public static BundleResource readResource(final InputStream inputStream) throws IOException {
		return OBJECT_MAPPER.readValue(inputStream, BundleResource.class);
	}

	public static String toJson(final BundleResource bundleResource) throws IOException {
		return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(bundleResource);
	}

	private InternationalizationGlobals() {
	}
}
