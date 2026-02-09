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
package org.nervousync.mail.protocol.impl;

import org.nervousync.commons.Globals;
import org.nervousync.mail.config.MailConfig;
import org.nervousync.proxy.ProxyConfig;
import org.nervousync.mail.operator.SendOperator;
import org.nervousync.mail.protocol.BaseProtocol;

import java.util.Properties;

/**
 * Implement SMTP protocol
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 31, 2012 8:10:30 PM $
 */
public final class SMTPProtocol extends BaseProtocol implements SendOperator {
	/**
	 * <span class="en-US">Serial version UID</span>
	 * <span class="zh-CN">序列化UID</span>
	 */
	private static final long serialVersionUID = -5226459745420272131L;

	/**
	 * <h3 class="en-US">Constructor method for SMTPProtocol</h3>
	 * <h3 class="zh-CN">SMTPProtocol构造方法</h3>
	 *
	 * @param proxyConfig <span class="en-US">Proxy configure information</span>
	 *                    <span class="zh-CN">代理服务器配置信息</span>
	 */
	public SMTPProtocol(final ProxyConfig proxyConfig) {
		super(proxyConfig);
		this.hostParam = "mail.smtp.host";
		this.portParam = "mail.smtp.port";
		this.connectionTimeoutParam = "mail.smtp.connectiontimeout";
		this.timeoutParam = "mail.smtp.timeout";
	}

	@Override
	protected void config(final Properties properties, final MailConfig.ServerConfig serverConfig, final int port) {
		properties.setProperty(MAIL_STORE_PROTOCOL, "smtp");
		properties.setProperty(MAIL_TRANSPORT_PROTOCOL, "smtp");
		if (serverConfig.isAuthLogin()) {
			properties.setProperty("mail.smtp.auth", Boolean.TRUE.toString());
		}
		switch (serverConfig.getSecureProtocol()) {
			case SSL:
				properties.setProperty(MAIL_STORE_PROTOCOL, "smtps");
				properties.setProperty(MAIL_TRANSPORT_PROTOCOL, "smtps");
				properties.setProperty("mail.smtp.ssl.enable", Boolean.TRUE.toString());
				properties.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY_CLASS);
				properties.setProperty("mail.smtp.socketFactory.fallback", Boolean.FALSE.toString());
				if (port != Globals.DEFAULT_VALUE_INT) {
					properties.setProperty("mail.smtp.socketFactory.port", Integer.toString(port));
				}
				break;
			case TLS:
				properties.setProperty("mail.smtp.starttls.enable", Boolean.TRUE.toString());
				break;
		}
	}
}
