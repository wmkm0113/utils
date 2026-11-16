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

package org.nervousync.security.digest;

import jakarta.annotation.Nonnull;
import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.utils.logger.LoggerUtils;

/**
 * <h2 class="en-US">Default digest provider implement class</h2>
 * <h2 class="zh-CN">默认摘要算法适配器实现类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.3 $ $Date: Jan 13, 2010 11:23:13 $
 */
public final class DefaultDigestProviderImpl implements DigestProvider {

	/**
	 * <span class="en-US">Multilingual supported logger instance</span>
	 * <span class="zh-CN">多语言支持的日志对象</span>
	 */
	private final LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());

	@Override
	public CryptoAdaptor initDigest(@Nonnull final CipherConfig cipherConfig, final CipherKey cipherKey) {
		DigestAdaptorImpl adaptor = new DigestAdaptorImpl(cipherConfig);
		if (adaptor.isMacMode()) {
			adaptor.initHmac(cipherConfig.algorithm(), cipherKey.getKeyBytes());
		}
		return adaptor;
	}
}
