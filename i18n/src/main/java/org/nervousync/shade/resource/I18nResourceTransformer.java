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
import org.nervousync.beans.i18n.BundleMessage;
import org.nervousync.beans.i18n.BundleResource;
import org.nervousync.commons.Globals;
import org.nervousync.commons.i18n.InternationalizationGlobals;
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.i18n.LocaleUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * <h2 class="en-US">Transformer implements class which using for the merge internationalization resource file</h2>
 * <h2 class="zh-CN">用于合并国际化资源文件的传送器实现</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 21, 2023 09:05:28 $
 */
public final class I18nResourceTransformer implements ReproducibleResourceTransformer {

	private final BundleResource bundleResource = new BundleResource();

	@Override
	@Deprecated
	public void processResource(final String resource, final InputStream inputStream,
	                            final List<Relocator> relocatorList) throws IOException {
		this.processResource(resource, inputStream, relocatorList, 0L);
	}

	@Override
	public void processResource(final String resource, final InputStream inputStream,
	                            final List<Relocator> relocatorList, final long time) throws IOException {
		if (LocaleUtils.isEmpty(resource)) {
			return;
		}
		if (InternationalizationGlobals.BUNDLE_RESOURCE_PATH.equalsIgnoreCase(resource)) {
			BundleResource readResource = InternationalizationGlobals.readResource(inputStream);
			if (readResource != null) {
				if (readResource.getErrorCodes() != null && !readResource.getErrorCodes().isEmpty()) {
					Map<String, String> errorCodes = this.bundleResource.getErrorCodes();
					errorCodes.putAll(readResource.getErrorCodes());
					this.bundleResource.setErrorCodes(errorCodes);
				}
				if (readResource.getBundleMessages() != null && !readResource.getBundleMessages().isEmpty()) {
					Map<String, Map<String, BundleMessage>> bundleMessages = this.bundleResource.getBundleMessages();
					readResource.getBundleMessages().forEach((languageCode, messages) -> {
						Map<String, BundleMessage> languageMessages = bundleMessages.getOrDefault(languageCode, new HashMap<>());
						languageMessages.putAll(messages);
						bundleMessages.put(languageCode, languageMessages);
					});
					this.bundleResource.setBundleMessages(bundleMessages);
				}
			}
		}
	}

	@Override
	public boolean canTransformResource(final String resource) {
		return InternationalizationGlobals.BUNDLE_RESOURCE_PATH.equalsIgnoreCase(resource);
	}

	@Override
	public boolean hasTransformedResource() {
		return Boolean.TRUE;
	}

	@Override
	public void modifyOutputStream(final JarOutputStream jarOutputStream) throws IOException {
		long currentTime = DateTimeUtils.currentTimeMillis() / 1000 * 1000;
		JarEntry jarEntry = new JarEntry(InternationalizationGlobals.BUNDLE_RESOURCE_PATH);
		jarEntry.setTime(currentTime);
		jarOutputStream.putNextEntry(jarEntry);
		jarOutputStream.write(InternationalizationGlobals.toJson(this.bundleResource).getBytes(Globals.DEFAULT_ENCODING));
		jarOutputStream.write("\r\n".getBytes(Globals.DEFAULT_ENCODING));
		jarOutputStream.flush();
	}

	public void setGroupId(final String groupId) {
		this.bundleResource.setGroupId(groupId);
	}

	public void setBundle(final String bundle) {
		this.bundleResource.setBundle(bundle);
	}
}
