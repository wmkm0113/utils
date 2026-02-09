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
import org.nervousync.utils.core.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * <h2 class="en-US">Transformer implements class which using for the merge schema mapping file</h2>
 * <h2 class="zh-CN">用于合并Schema映射的传送器实现</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 21, 2023 09:05:28 $
 */
public final class SchemaMappingResourceTransformer implements ReproducibleResourceTransformer {

	private final StringBuilder stringBuilder = new StringBuilder();

	@Override
	public void processResource(final String resource, final InputStream is,
	                            final List<Relocator> relocators, final long time) {
		if (BeanUtils.SCHEMA_MAPPING_RESOURCE_PATH.equalsIgnoreCase(resource)) {
			String content = IOUtils.readContent(is);
			if (StringUtils.notBlank(content)) {
				stringBuilder.append(content).append(FileUtils.CRLF);
			}
		}
	}

	@Override
	public boolean canTransformResource(final String resource) {
		return BeanUtils.SCHEMA_MAPPING_RESOURCE_PATH.equalsIgnoreCase(resource);
	}

	@Override
	@Deprecated
	public void processResource(final String resource, final InputStream inputStream,
	                            final List<Relocator> relocatorList) {
		this.processResource(resource, inputStream, relocatorList, 0L);
	}

	@Override
	public boolean hasTransformedResource() {
		return this.stringBuilder.length() > 0;
	}

	@Override
	public void modifyOutputStream(final JarOutputStream jarOutputStream) throws IOException {
		long currentTime = DateTimeUtils.currentTimeMillis() / 1000 * 1000;
		JarEntry jarEntry = new JarEntry(BeanUtils.SCHEMA_MAPPING_RESOURCE_PATH);
		jarEntry.setTime(currentTime);
		jarOutputStream.putNextEntry(jarEntry);
		jarOutputStream.write(this.stringBuilder.toString().getBytes(Globals.DEFAULT_ENCODING));
		jarOutputStream.write(FileUtils.CRLF.getBytes(Globals.DEFAULT_ENCODING));
		jarOutputStream.flush();
	}
}
