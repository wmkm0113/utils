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
package org.nervousync.utils.mail;

import jakarta.annotation.Nonnull;
import jakarta.mail.*;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.pop3.POP3Folder;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.mail.SecureProtocol;
import org.nervousync.exceptions.mail.MailException;
import org.nervousync.mail.config.MailConfig;
import org.nervousync.proxy.ProxyConfig;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.io.IOException;
import java.security.Security;
import java.util.*;

/**
 * <h2 class="en-US">E-Mail Utilities</h2>
 * <span class="en-US">
 * <span>Current utilities implements features:</span>
 *     <ul>Send/Receive email</ul>
 *     <ul>Count email in the folder</ul>
 *     <ul>List folder names</ul>
 *     <ul>Download email attachment files automatically</ul>
 *     <ul>Verify email signature</ul>
 *     <ul>Add a signature to email</ul>
 * </span>
 * <h2 class="zh-CN">电子邮件工具集</h2>
 * <span class="zh-CN">
 *     <span>此工具集实现以下功能:</span>
 *     <ul>发送接收电子邮件</ul>
 *     <ul>获取文件夹中的电子邮件数量</ul>
 *     <ul>列出所有文件夹名称</ul>
 *     <ul>自动下载电子邮件中包含的附件</ul>
 *     <ul>验证电子邮件签名</ul>
 *     <ul>添加电子签名到邮件</ul>
 * </span>
 *
 * @author Steven Wee     <a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.1.5 $ $Date: Jul 31, 2012 20:54:04 $
 */
public final class MailUtils {
	/**
	 * <span class="en-US">Logger instance</span>
	 * <span class="zh-CN">日志实例</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(MailUtils.class);

	/**
	 * <span class="en-US">Default SSL socket factory class, using for connecting to ssl mail server</span>
	 * <span class="zh-CN">默认的安全套接字工厂类，用于连接到电子邮件服务器时使用安全连接</span>
	 */
	private static final String SSL_FACTORY_CLASS = "javax.net.ssl.SSLSocketFactory";
	/**
	 * <span class="en-US">Protocol key name of connecting to mail server store</span>
	 * <span class="zh-CN">连接到电子邮件服务器的通讯协议类型键值名</span>
	 */
	public static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
	/**
	 * <span class="en-US">Protocol key name of connecting to mail server transport</span>
	 * <span class="zh-CN">连接到电子邮件服务器的传输协议类型键值名</span>
	 */
	private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";

	/**
	 * <h3 class="en-US">Private constructor for MailUtils</h3>
	 * <h3 class="zh-CN">电子邮件工具集的私有构造方法</h3>
	 */
	private MailUtils() {
	}

	/**
	 * <h3 class="en-US">Initialize the Mail Agent instance by given mail configure information</h3>
	 * <h3 class="zh-CN">使用给定的电子邮件配置信息生成电子邮件代理实例对象</h3>
	 *
	 * @param mailConfig <span class="en-US">Mail configure information define</span>
	 *                   <span class="zh-CN">邮件配置信息定义</span>
	 * @return <span class="en-US">Generated Mail Agent instance</span>
	 * <span class="zh-CN">生成的电子邮件代理实例对象</span>
	 */
	public static MailClient client(@Nonnull final MailConfig mailConfig) {
		if (StringUtils.isEmpty(mailConfig.getUserName()) || StringUtils.isEmpty(mailConfig.getPassword())) {
			return null;
		}
		try {
			return new MailClient(mailConfig);
		} catch (MailException | MessagingException e) {
			LOGGER.error("Initialize_Client_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return null;
		}
	}

	/**
	 * <h3 class="en-US">Generate the configure information Properties instance object</h3>
	 * <h3 class="zh-CN">生成配置信息 Properties 实例对象</h3>
	 *
	 * @param serverConfig <span class="en-US">Mail server configure information</span>
	 *                     <span class="zh-CN">邮件服务器配置信息</span>
	 * @param proxyConfig  <span class="en-US">Proxy configure information</span>
	 *                     <span class="zh-CN">代理服务器配置信息</span>
	 * @return <span class="en-US">The generated Properties instance object</span>
	 * <span class="zh-CN">生成的 Properties 实例对象</span>
	 * @throws MailException <span class="en-US">Unknown mail protocol</span>
	 *                       <span class="zh-CN">协议类型未知</span>
	 */
	public static Properties mailProperties(final MailConfig.ServerConfig serverConfig,
	                                        final ProxyConfig proxyConfig) throws MailException {
		final String configPrefix, hostParam, portParam, connectionTimeoutParam, timeoutParam;
		switch (serverConfig.getProtocolOption()) {
			case SMTP:
				configPrefix = "mail.smtp";
				hostParam = "mail.smtp.host";
				portParam = "mail.smtp.port";
				connectionTimeoutParam = "mail.smtp.connectiontimeout";
				timeoutParam = "mail.smtp.timeout";
				break;
			case IMAP:
				configPrefix = "mail.imap";
				hostParam = "mail.imap.host";
				portParam = "mail.imap.port";
				connectionTimeoutParam = "mail.imap.connectiontimeout";
				timeoutParam = "mail.imap.timeout";
				break;
			case POP3:
				configPrefix = "mail.pop3";
				hostParam = "mail.pop3.host";
				portParam = "mail.pop3.port";
				connectionTimeoutParam = "mail.pop3.connectiontimeout";
				timeoutParam = "mail.pop3.timeout";
				break;
			default:
				throw new MailException(0x0000000E0002L);
		}
		Properties properties = new Properties();

		properties.setProperty(hostParam, serverConfig.getHostName());
		int port = serverConfig.getHostPort();
		if (port != Globals.DEFAULT_VALUE_INT) {
			properties.setProperty(portParam, Integer.toString(port));
		}

		if (serverConfig.getConnectionTimeout() > 0) {
			properties.setProperty(connectionTimeoutParam,
					Integer.toString(serverConfig.getConnectionTimeout() * 1000));
		}
		if (serverConfig.getProcessTimeout() > 0) {
			properties.setProperty(timeoutParam, Integer.toString(serverConfig.getConnectionTimeout() * 1000));
		}

		if (SecureProtocol.SSL.equals(serverConfig.getSecureProtocol())) {
			Security.addProvider(Security.getProvider("SunJSSE"));
		}

		if (LOGGER.isDebugEnabled()) {
			properties.put("mail.debug", Boolean.TRUE.toString());
		}

		switch (proxyConfig.getProxyType()) {
			case HTTP:
				properties.setProperty(configPrefix + ".proxy.host", proxyConfig.getProxyAddress());
				if (proxyConfig.getProxyPort() != Globals.DEFAULT_VALUE_INT) {
					properties.setProperty(configPrefix + ".proxy.port",
							Integer.toString(proxyConfig.getProxyPort()));
				}
				if (StringUtils.notBlank(proxyConfig.getUserName())
						&& StringUtils.notBlank(proxyConfig.getPassword())) {
					properties.setProperty(configPrefix + ".proxy.user", proxyConfig.getUserName());
					properties.setProperty(configPrefix + ".proxy.password", proxyConfig.getPassword());
				}
				break;
			case SOCKS:
				properties.setProperty(configPrefix + ".socks.host", proxyConfig.getProxyAddress());
				properties.setProperty(configPrefix + ".socks.port", Integer.toString(proxyConfig.getProxyPort()));
				break;
		}

		switch (serverConfig.getProtocolOption()) {
			case SMTP:
				smtpProperties(properties, serverConfig.isAuthLogin(), serverConfig.getSecureProtocol(), port);
				break;
			case IMAP:
				imapProperties(properties, serverConfig.getHostName().toLowerCase().endsWith("gmail.com"),
						serverConfig.isAuthLogin(), serverConfig.getSecureProtocol(), port);
				break;
			case POP3:
				pop3Properties(properties, serverConfig.getSecureProtocol(), port);
				break;
			default:
				throw new MailException(0x0000000E0002L);
		}
		return properties;
	}

	/**
	 * <h3 class="en-US">Setting SMTP configure information</h3>
	 * <h3 class="zh-CN">设置 SMTP 配置信息</h3>
	 *
	 * @param properties     <span class="en-US">Properties instance object</span>
	 *                       <span class="zh-CN">Properties 实例对象</span>
	 * @param authLogin      <span class="en-US">Host server authenticates login</span>
	 *                       <span class="zh-CN">邮件服务器需要身份验证</span>
	 * @param secureProtocol <span class="en-US">Enumeration value of used secure protocol</span>
	 *                       <span class="zh-CN">使用的安全协议枚举值</span>
	 * @param hostPort       <span class="en-US">Mail server port</span>
	 *                       <span class="zh-CN">邮件服务器端口号</span>
	 */
	private static void smtpProperties(@Nonnull final Properties properties, final boolean authLogin,
	                                   final SecureProtocol secureProtocol, final int hostPort) {
		properties.setProperty(MAIL_STORE_PROTOCOL, "smtp");
		properties.setProperty(MAIL_TRANSPORT_PROTOCOL, "smtp");
		if (authLogin) {
			properties.setProperty("mail.smtp.auth", Boolean.TRUE.toString());
		}
		switch (secureProtocol) {
			case SSL:
				properties.setProperty(MAIL_STORE_PROTOCOL, "smtps");
				properties.setProperty(MAIL_TRANSPORT_PROTOCOL, "smtps");
				properties.setProperty("mail.smtp.ssl.enable", Boolean.TRUE.toString());
				properties.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY_CLASS);
				properties.setProperty("mail.smtp.socketFactory.fallback", Boolean.FALSE.toString());
				if (hostPort != Globals.DEFAULT_VALUE_INT) {
					properties.setProperty("mail.smtp.socketFactory.port", Integer.toString(hostPort));
				}
				break;
			case TLS:
				properties.setProperty("mail.smtp.starttls.enable", Boolean.TRUE.toString());
				break;
		}
	}

	/**
	 * <h3 class="en-US">Setting POP3 configure information</h3>
	 * <h3 class="zh-CN">设置 POP3 配置信息</h3>
	 *
	 * @param properties     <span class="en-US">Properties instance object</span>
	 *                       <span class="zh-CN">Properties 实例对象</span>
	 * @param secureProtocol <span class="en-US">Enumeration value of used secure protocol</span>
	 *                       <span class="zh-CN">使用的安全协议枚举值</span>
	 * @param hostPort       <span class="en-US">Mail server port</span>
	 *                       <span class="zh-CN">邮件服务器端口号</span>
	 */
	private static void pop3Properties(@Nonnull final Properties properties,
	                                   final SecureProtocol secureProtocol, final int hostPort) {
		properties.setProperty(MAIL_STORE_PROTOCOL, "pop3");
		properties.setProperty(MAIL_TRANSPORT_PROTOCOL, "pop3");

		switch (secureProtocol) {
			case SSL:
				properties.setProperty(MAIL_STORE_PROTOCOL, "pop3s");
				properties.setProperty(MAIL_TRANSPORT_PROTOCOL, "pop3s");
				properties.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY_CLASS);
				if (hostPort != 0) {
					properties.setProperty("mail.pop3.socketFactory.port", Integer.toString(hostPort));
				}
				properties.setProperty("mail.pop3.disabletop", Boolean.TRUE.toString());
				properties.setProperty("mail.pop3.ssl.enable", Boolean.TRUE.toString());
				break;
			case TLS:
				properties.setProperty("mail.pop3.starttls.enable", Boolean.TRUE.toString());
				break;
		}
	}

	/**
	 * <h3 class="en-US">Setting IMAP configure information</h3>
	 * <h3 class="zh-CN">设置 IMAP 配置信息</h3>
	 *
	 * @param properties     <span class="en-US">Properties instance object</span>
	 *                       <span class="zh-CN">Properties 实例对象</span>
	 * @param gmail          <span class="en-US">Google Mail flag</span>
	 *                       <span class="zh-CN">谷歌邮件标记</span>
	 * @param authLogin      <span class="en-US">Host server authenticates login</span>
	 *                       <span class="zh-CN">邮件服务器需要身份验证</span>
	 * @param secureProtocol <span class="en-US">Enumeration value of used secure protocol</span>
	 *                       <span class="zh-CN">使用的安全协议枚举值</span>
	 * @param hostPort       <span class="en-US">Mail server port</span>
	 *                       <span class="zh-CN">邮件服务器端口号</span>
	 */
	private static void imapProperties(@Nonnull final Properties properties, final boolean gmail,
	                                   final boolean authLogin, final SecureProtocol secureProtocol, final int hostPort) {
		properties.setProperty(MAIL_STORE_PROTOCOL, gmail ? "gimap" : "imap");
		if (authLogin) {
			if (gmail) {
				properties.setProperty("mail.gimap.auth.plain.disable", Boolean.TRUE.toString());
				properties.setProperty("mail.gimap.auth.login.disable", Boolean.TRUE.toString());
			} else {
				properties.setProperty("mail.imap.auth.plain.disable", Boolean.TRUE.toString());
				properties.setProperty("mail.imap.auth.login.disable", Boolean.TRUE.toString());
			}
		}

		switch (secureProtocol) {
			case SSL:
				properties.setProperty(MAIL_STORE_PROTOCOL, gmail ? "gimaps" : "imaps");
				if (gmail) {
					properties.setProperty("mail.gimap.socketFactory.class", SSL_FACTORY_CLASS);
					if (hostPort != Globals.DEFAULT_VALUE_INT) {
						properties.setProperty("mail.gimap.socketFactory.port", Integer.toString(hostPort));
					}
				} else {
					properties.setProperty("mail.imap.socketFactory.class", SSL_FACTORY_CLASS);
					if (hostPort != Globals.DEFAULT_VALUE_INT) {
						properties.setProperty("mail.imap.socketFactory.port", Integer.toString(hostPort));
					}
				}
				break;
			case TLS:
				if (gmail) {
					properties.setProperty("mail.gimap.starttls.enable", Boolean.TRUE.toString());
				} else {
					properties.setProperty("mail.imap.starttls.enable", Boolean.TRUE.toString());
				}
				break;
		}
	}

	/**
	 * <h3 class="en-US">Open a folder from the Store instance by the given folder name and mode</h3>
	 * <h3 class="zh-CN">使用给定的模式中打开给定的Store实例对象中的文件夹</h3>
	 *
	 * @param store      <span class="en-US">Store instance</span>
	 *                   <span class="zh-CN">Store实例对象</span>
	 * @param readOnly   <span class="en-US">Read-only status</span>
	 *                   <span class="zh-CN">只读模式状态</span>
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @return <span class="en-US">Opened Folder instance</span>
	 * <span class="zh-CN">打开的文件夹实例对象</span>
	 * @throws MessagingException <span class="en-US">If an error occurs when process open</span>
	 *                            <span class="zh-CN">当读取数据时出现异常</span>
	 */
	static Folder openFolder(final Store store, final boolean readOnly, final String folderName)
			throws MessagingException {
		Folder folder = store.getFolder(folderName);
		folder.open(readOnly ? Folder.READ_ONLY : Folder.READ_WRITE);
		return folder;
	}

	/**
	 * <h3 class="en-US">Read UID string by given folder and message instance</h3>
	 * <h3 class="zh-CN">根据给定的电子邮件目录实例对象和邮件信息实例对象读取唯一识别ID字符串</h3>
	 *
	 * @param folder  <span class="en-US">E-mail folder instance</span>
	 *                <span class="zh-CN">电子邮件目录实例对象</span>
	 * @param message <span class="en-US">E-mail message instance</span>
	 *                <span class="zh-CN">电子邮件信息实例对象</span>
	 * @return <span class="en-US">Read UID string</span>
	 * <span class="zh-CN">读取的唯一识别ID字符串</span>
	 * @throws MessagingException <span class="en-US">If an error occurs when read UID string</span>
	 *                            <span class="zh-CN">当读取唯一识别ID字符串时出现异常</span>
	 */
	static String readUID(final Folder folder, final Message message) throws MessagingException {
		if (folder instanceof POP3Folder) {
			return ((POP3Folder) folder).getUID(message);
		} else if (folder instanceof IMAPFolder) {
			return Long.valueOf(((IMAPFolder) folder).getUID(message)).toString();
		}
		return null;
	}

	/**
	 * <h3 class="en-US">Read the E-mail message by given folder and message UID string</h3>
	 * <h3 class="zh-CN">从给定的电子邮件目录中读取唯一识别ID字符串标识的电子邮件信息</h3>
	 *
	 * @param folder <span class="en-US">E-mail folder instance</span>
	 *               <span class="zh-CN">电子邮件目录实例对象</span>
	 * @param uid    <span class="en-US">UID string</span>
	 *               <span class="zh-CN">唯一标识ID字符串</span>
	 * @return <span class="en-US">Read e-mail message instance</span>
	 * <span class="zh-CN">读取的电子邮件信息实例对象</span>
	 * @throws MessagingException <span class="en-US">If an error occurs when read UID string</span>
	 *                            <span class="zh-CN">当读取唯一识别ID字符串时出现异常</span>
	 */
	static Message readMessage(final Folder folder, final String uid) throws MessagingException {
		if (folder instanceof POP3Folder) {
			for (Message msg : folder.getMessages()) {
				if (((POP3Folder) folder).getUID(msg).equals(uid)) {
					return msg;
				}
			}
		} else if (folder instanceof IMAPFolder) {
			return ((IMAPFolder) folder).getMessageByUID(Long.parseLong(uid));
		}
		return null;
	}

	/**
	 * <h3 class="en-US">Read the E-mail message list by given folder and message UID string array</h3>
	 * <h3 class="zh-CN">从给定的电子邮件目录中读取唯一识别ID字符串数组标识的电子邮件信息列表</h3>
	 *
	 * @param folder    <span class="en-US">E-mail folder instance</span>
	 *                  <span class="zh-CN">电子邮件目录实例对象</span>
	 * @param uidArrays <span class="en-US">UID string</span>
	 *                  <span class="zh-CN">唯一标识ID字符串</span>
	 * @return <span class="en-US">Read e-mail message instance list</span>
	 * <span class="zh-CN">读取的电子邮件信息实例对象列表</span>
	 * @throws MessagingException <span class="en-US">If an error occurs when read UID string</span>
	 *                            <span class="zh-CN">当读取唯一识别ID字符串时出现异常</span>
	 */
	static List<Message> readMessages(final Folder folder, final String... uidArrays) throws MessagingException {
		List<Message> messageList = new ArrayList<>();
		if (folder instanceof POP3Folder) {
			List<String> uidList = Arrays.asList(uidArrays);
			for (Message message : folder.getMessages()) {
				if (uidList.contains(((POP3Folder) folder).getUID(message))) {
					messageList.add(message);
				}
			}
		} else if (folder instanceof IMAPFolder) {
			long[] uidList = new long[uidArrays.length];
			for (int i = 0; i < uidArrays.length; i++) {
				uidList[i] = Long.parseLong(uidArrays[i]);
			}
			Collections.addAll(messageList, ((IMAPFolder) folder).getMessagesByUID(uidList));
		}
		return messageList;
	}

	/**
	 * <h3 class="en-US">Read mail content information</h3>
	 * <h3 class="zh-CN">读取电子邮件详细信息</h3>
	 *
	 * @param part          <span class="en-US">part of e-mail MIME information</span>
	 *                      <span class="zh-CN">电子邮件MIME信息</span>
	 * @param contentBuffer <span class="en-US">Content information buffer</span>
	 *                      <span class="zh-CN">详细信息输出缓冲器</span>
	 * @throws MessagingException <span class="en-US">If an error occurs when processing read</span>
	 *                            <span class="zh-CN">当读取信息时出现异常</span>
	 * @throws IOException        <span class="en-US">If an error occurs when append content information to buffer</span>
	 *                            <span class="zh-CN">当追加详细信息到输出缓冲器时出现异常</span>
	 */
	static void readMailContent(final Part part, final StringBuilder contentBuffer)
			throws MessagingException, IOException {
		String contentType = part.getContentType();
		int nameIndex = contentType.indexOf("name");

		if (contentBuffer == null) {
			throw new IOException();
		}

		if (part.isMimeType(Globals.DEFAULT_CONTENT_TYPE_TEXT) && (nameIndex == -1)) {
			contentBuffer.append(part.getContent().toString());
		} else if (part.isMimeType(Globals.DEFAULT_CONTENT_TYPE_HTML) && (nameIndex == -1)) {
			contentBuffer.append(part.getContent().toString());
		} else if (part.isMimeType(Globals.DEFAULT_CONTENT_TYPE_MULTIPART)) {
			Multipart multipart = (Multipart) part.getContent();
			int count = multipart.getCount();
			for (int i = 0; i < count; i++) {
				readMailContent(multipart.getBodyPart(i), contentBuffer);
			}
		} else if (part.isMimeType(Globals.DEFAULT_CONTENT_TYPE_MESSAGE_RFC822)) {
			readMailContent((Part) part.getContent(), contentBuffer);
		}
	}
}
