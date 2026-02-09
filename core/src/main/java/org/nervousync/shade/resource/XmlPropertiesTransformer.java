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

package org.nervousync.shade.resource;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ReproducibleResourceTransformer;
import org.nervousync.commons.Globals;
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.core.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * <h2 class="en-US">An implementation of a properties file transfer mechanism for merging XML-formatted files.</h2>
 * <h2 class="zh-CN">用于合并 XML 格式的 Properties 文件传送器实现</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 21, 2023 09:05:28 $
 */
public class XmlPropertiesTransformer implements ReproducibleResourceTransformer {

	/**
	 * <span class="en-US">List of file paths that need to be processed</span>
	 * <span class="zh-CN">需要处理的文件地址列表</span>
	 */
	private List<String> filePaths = new ArrayList<>();
	/**
	 * <span class="en-US">Processed resource file mapping table</span>
	 * <span class="zh-CN">处理的资源文件映射表</span>
	 */
	private final Map<String, Properties> propertiesMap = new HashMap<>();

	@Override
	public void processResource(final String resource, final InputStream is,
	                            final List<Relocator> relocators, final long time) throws IOException {
		if (this.filePaths.contains(resource)) {
			Properties properties = new Properties();
			properties.loadFromXML(is);
			Properties existProperties = this.propertiesMap.getOrDefault(resource, new Properties());
			existProperties.putAll(properties);
			this.propertiesMap.put(resource, existProperties);
		}
	}

	@Override
	public boolean canTransformResource(final String resource) {
		return this.filePaths.contains(resource);
	}

	@Override
	@Deprecated
	public void processResource(final String resource, final InputStream inputStream,
	                            final List<Relocator> relocatorList) throws IOException {
		this.processResource(resource, inputStream, relocatorList, 0L);
	}

	@Override
	public boolean hasTransformedResource() {
		return !this.propertiesMap.isEmpty();
	}

	@Override
	public void modifyOutputStream(final JarOutputStream jarOutputStream) throws IOException {
		long currentTime = DateTimeUtils.currentTimeMillis() / 1000 * 1000;
		for (Map.Entry<String, Properties> entry : this.propertiesMap.entrySet()) {
			JarEntry jarEntry = new JarEntry(entry.getKey());
			jarEntry.setTime(currentTime);
			jarOutputStream.putNextEntry(jarEntry);
			entry.getValue().storeToXML(jarOutputStream, null, Globals.DEFAULT_ENCODING);
			jarOutputStream.write(FileUtils.CRLF.getBytes(Globals.DEFAULT_ENCODING));
			jarOutputStream.flush();
		}
	}

	/**
	 * <h3 class="en-US">Getter method for the list of file paths that need to be processed</h3>
	 * <h3 class="zh-CN">需要处理的文件地址列表的 Getter 方法</h3>
	 *
	 * @return <span class="en-US">List of file paths that need to be processed</span>
	 * <span class="zh-CN">需要处理的文件地址列表</span>
	 */
	public List<String> getFilePaths() {
		return this.filePaths;
	}

	/**
	 * <h3 class="en-US">Setter method for the list of file paths that need to be processed</h3>
	 * <h3 class="zh-CN">需要处理的文件地址列表的 Setter 方法</h3>
	 *
	 * @param filePaths <span class="en-US">List of file paths that need to be processed</span>
	 *                  <span class="zh-CN">需要处理的文件地址列表</span>
	 */
	public void setFilePaths(final List<String> filePaths) {
		this.filePaths = filePaths;
	}
}
