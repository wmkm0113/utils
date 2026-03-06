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

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.mail.smime.SMIMESignedParser;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.pop3.POP3Folder;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.mail.MailProtocol;
import org.nervousync.exceptions.mail.MailException;
import org.nervousync.mail.MailObject;
import org.nervousync.mail.authenticator.DefaultAuthenticator;
import org.nervousync.mail.config.MailConfig;
import org.nervousync.utils.cert.CertificateUtils;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * <h2 class="en-US">E-Mail Client</h2>
 * <h2 class="zh-CN">电子邮件客户端</h2>
 *
 * @author Steven Wee     <a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 31, 2012 21:03:46 $
 */
public final class MailClient implements Closeable {

	/**
	 * <span class="en-US">Logger instance</span>
	 * <span class="zh-CN">日志实例</span>
	 */
	private static final LoggerUtils.Logger LOGGER = LoggerUtils.getLogger(MailClient.class);

	/**
	 * <span class="en-US">Mail account username</span>
	 * <span class="zh-CN">邮件账户用户名</span>
	 */
	private final String userName;
	/**
	 * <span class="en-US">Mail account password</span>
	 * <span class="zh-CN">邮件账户密码</span>
	 */
	private final String passWord;
	/**
	 * <span class="en-US">Mail receive server config</span>
	 * <span class="zh-CN">邮件接收服务器配置信息</span>
	 */
	private final MailConfig.ServerConfig receiveConfig;
	/**
	 * <span class="en-US">Attaches the file storage path</span>
	 * <span class="zh-CN">附件文件的保存地址</span>
	 */
	private final String storagePath;
	/**
	 * <span class="en-US">x509 certificate Using for email signature verify</span>
	 * <span class="zh-CN">x509证书用于电子邮件签名验证</span>
	 */
	private final X509Certificate x509Certificate;
	/**
	 * <span class="en-US">private key Using for email signature</span>
	 * <span class="zh-CN">私有密钥用于电子邮件签名</span>
	 */
	private final PrivateKey privateKey;
	/**
	 * <span class="en-US">Receive protocol</span>
	 * <span class="zh-CN">收件协议</span>
	 */
	private final String receiveProtocol;
	/**
	 * <span class="en-US">Send server session</span>
	 * <span class="zh-CN">发送服务器连接</span>
	 */
	private final Session sendSession;
	/**
	 * <span class="en-US">Receive server session</span>
	 * <span class="zh-CN">接收服务器连接</span>
	 */
	private final Session recvSession;
	private Store store;

	/**
	 * <h3 class="en-US">Private constructor for E-Mail Agent</h3>
	 * <h3 class="zh-CN">电子邮件代理的私有构造方法</h3>
	 *
	 * @param mailConfig <span class="en-US">Mail configure information define</span>
	 *                   <span class="zh-CN">邮件配置信息定义</span>
	 */
	MailClient(final MailConfig mailConfig) throws MailException, MessagingException {
		this.userName = mailConfig.getUserName().toLowerCase();
		this.passWord = mailConfig.getPassword();
		if (mailConfig.getSendConfig() == null
				|| !MailProtocol.SMTP.equals(mailConfig.getSendConfig().getProtocolOption())) {
			throw new MailException(0x0000000E0003L);
		} else {
			Properties properties = MailUtils.mailProperties(mailConfig.getSendConfig(), mailConfig.getProxyConfig());
			if (StringUtils.notBlank(this.userName)) {
				properties.setProperty("mail.smtp.from", this.userName);
			}
			this.sendSession = Session.getDefaultInstance(properties, new DefaultAuthenticator(this.userName, this.passWord));
		}
		if (mailConfig.getReceiveConfig() == null) {
			throw new MailException(0x0000000E0004L);
		} else {
			this.receiveConfig = mailConfig.getReceiveConfig();
			Properties properties = MailUtils.mailProperties(this.receiveConfig, mailConfig.getProxyConfig());
			this.recvSession = Session.getDefaultInstance(properties, new DefaultAuthenticator(this.userName, this.passWord));
			this.receiveProtocol = properties.getProperty(MailUtils.MAIL_STORE_PROTOCOL);
			this.connect();
		}
		this.storagePath = mailConfig.getStoragePath();
		//  Initialize the storage path
		if (StringUtils.notBlank(this.storagePath)) {
			FileUtils.makeDir(this.storagePath);
		}
		this.x509Certificate = StringUtils.notBlank(mailConfig.getCertificate())
				? CertificateUtils.x509(StringUtils.base64Decode(mailConfig.getCertificate()))
				: null;
		this.privateKey = StringUtils.notBlank(mailConfig.getPrivateKey())
				? CertificateUtils.privateKey("RSA", StringUtils.base64Decode(mailConfig.getPrivateKey()))
				: null;
	}

	/**
	 * <h3 class="en-US">Send E-Mail</h3>
	 * <h3 class="zh-CN">发送电子邮件</h3>
	 *
	 * @param mailObject <span class="en-US">E-Mail object</span>
	 *                   <span class="zh-CN">电子邮件信息</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	public boolean sendMail(final MailObject mailObject) {
		if (this.sendSession == null) {
			//	Not configs send server
			return Boolean.FALSE;
		}
		if (StringUtils.isEmpty(mailObject.getSendAddress())) {
			mailObject.setSendAddress(this.userName);
		}
		try {
			MimeMessage message = new MimeMessage(this.sendSession);

			message.setSubject(mailObject.getSubject(), mailObject.getCharset());

			MimeMultipart mimeMultipart = new MimeMultipart();

			if (mailObject.getAttachFiles() != null) {
				for (String attachment : mailObject.getAttachFiles()) {
					MimeBodyPart mimeBodyPart = new MimeBodyPart();

					File file;

					try {
						file = FileUtils.getFile(attachment);
					} catch (FileNotFoundException e) {
						throw new MailException(0x0000000E0005L, e);
					}

					DataSource dataSource = new FileDataSource(file);

					mimeBodyPart.setFileName(StringUtils.getFilename(attachment));
					mimeBodyPart.setDataHandler(new DataHandler(dataSource));

					mimeMultipart.addBodyPart(mimeBodyPart, mimeMultipart.getCount());
				}
			}

			if (mailObject.getIncludeFiles() != null) {
				List<String> includeFiles = mailObject.getIncludeFiles();
				for (String filePath : includeFiles) {
					File file;
					MimeBodyPart mimeBodyPart;

					try {
						file = FileUtils.getFile(filePath);
						String fileName = StringUtils.getFilename(filePath);
						mimeBodyPart = new MimeBodyPart();
						DataHandler dataHandler =
								new DataHandler(new ByteArrayDataSource(file.toURI().toURL().openStream(),
										Globals.DEFAULT_CONTENT_TYPE_BINARY));
						mimeBodyPart.setDataHandler(dataHandler);

						mimeBodyPart.setFileName(fileName);
						mimeBodyPart.setHeader("Content-ID", fileName);
					} catch (Exception e) {
						throw new MailException(0x0000000E0006L, e);
					}

					mimeMultipart.addBodyPart(mimeBodyPart, mimeMultipart.getCount());
				}
			}

			if (StringUtils.notBlank(mailObject.getContent())) {
				MimeBodyPart mimeBodyPart = new MimeBodyPart();
				mimeBodyPart.setContent(mailObject.getContent(),
						mailObject.getContentType() + "; charset=" + mailObject.getCharset());
				mimeMultipart.addBodyPart(mimeBodyPart, mimeMultipart.getCount());
			}

			if (x509Certificate != null && privateKey != null) {
				message.setContent(mimeMultipart);
				try {
					//  Generate signature attribute
					ASN1EncodableVector signatureAttribute = new ASN1EncodableVector();
					SMIMECapabilityVector capabilityVector = new SMIMECapabilityVector();
					capabilityVector.addCapability(SMIMECapability.aES256_CBC);
					capabilityVector.addCapability(SMIMECapability.dES_CBC);
					capabilityVector.addCapability(SMIMECapability.rC2_CBC, 128);
					signatureAttribute.add(new SMIMECapabilitiesAttribute(capabilityVector));
					signatureAttribute.add(new SMIMEEncryptionKeyPreferenceAttribute(SMIMEUtil.createIssuerAndSerialNumberFor(x509Certificate)));

					List<X509Certificate> certificateList = Collections.singletonList(x509Certificate);
					JcaCertStore certStore = new JcaCertStore(certificateList);

					SignerInfoGenerator signerInfoGenerator = new JcaSimpleSignerInfoGeneratorBuilder()
							.setProvider("BC")
							.setSignedAttributeGenerator(new AttributeTable(signatureAttribute))
							.build("SHA1withRSA", privateKey, x509Certificate);

					SMIMESignedGenerator generator = new SMIMESignedGenerator();
					generator.addSignerInfoGenerator(signerInfoGenerator);
					generator.addCertificates(certStore);

					MimeMultipart signedMimeMultipart = generator.generate(message);
					message.setContent(signedMimeMultipart, signedMimeMultipart.getContentType());
				} catch (CertificateEncodingException | CertificateParsingException | OperatorCreationException |
				         SMIMEException e) {
					throw new MailException(0x0000000E0007L, e);
				}
			} else {
				message.setContent(mimeMultipart, mimeMultipart.getContentType());
			}

			message.setFrom(new InternetAddress(mailObject.getSendAddress()));

			if (mailObject.getReceiveAddress() == null || mailObject.getReceiveAddress().isEmpty()) {
				throw new MailException(0x0000000E0008L);
			}
			StringBuilder receiveAddress = new StringBuilder();
			mailObject.getReceiveAddress().forEach(address -> receiveAddress.append(",").append(address));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiveAddress.substring(1)));

			if (mailObject.getCcAddress() != null && !mailObject.getCcAddress().isEmpty()) {
				StringBuilder ccAddress = new StringBuilder();
				mailObject.getCcAddress().forEach(address -> ccAddress.append(",").append(address));
				message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccAddress.substring(1)));
			}

			if (mailObject.getBccAddress() != null && !mailObject.getBccAddress().isEmpty()) {
				StringBuilder bccAddress = new StringBuilder();
				mailObject.getBccAddress().forEach(address -> bccAddress.append(",").append(address));
				message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccAddress.substring(1)));
			}

			if (mailObject.getReplyAddress() != null && !mailObject.getReplyAddress().isEmpty()) {
				StringBuilder replyAddress = new StringBuilder();
				mailObject.getReplyAddress().forEach(address -> replyAddress.append(",").append(address));
				message.setReplyTo(InternetAddress.parse(replyAddress.substring(1)));
			} else {
				message.setReplyTo(InternetAddress.parse(mailObject.getSendAddress()));
			}
			message.setSentDate(mailObject.getSendDate() == null ? new Date() : mailObject.getSendDate());
			Transport.send(message);
			return Boolean.TRUE;
		} catch (MailException | MessagingException e) {
			LOGGER.error("Send_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Read the folder name list from the default folder</h3>
	 * <h3 class="zh-CN">从默认文件夹中读取包含的文件夹名称列表</h3>
	 *
	 * @return <span class="en-US">folder name list</span>
	 * <span class="en-US">文件夹名称列表</span>
	 */
	public List<String> folderList() {
		List<String> folderList = new ArrayList<>();
		try {
			Folder defaultFolder = this.store.getDefaultFolder();
			if (defaultFolder != null) {
				for (Folder folder : defaultFolder.list()) {
					folderList.add(folder.getFullName());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Folders_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return folderList;
	}

	/**
	 * <h3 class="en-US">Read mail count from the inbox</h3>
	 * <h3 class="zh-CN">读取收件箱中的邮件数量</h3>
	 *
	 * @return <span class="en-US">Mail count</span>
	 * <span class="zh-CN">邮件数量</span>
	 */
	public int mailCount() {
		return this.mailCount(Globals.DEFAULT_EMAIL_FOLDER_INBOX);
	}

	/**
	 * <h3 class="en-US">Read mail count from the given folder name</h3>
	 * <h3 class="zh-CN">读取给定文件夹中的邮件数量</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @return <span class="en-US">Mail count</span>
	 * <span class="zh-CN">邮件数量</span>
	 */
	public int mailCount(final String folderName) {
		if (this.recvSession == null) {
			//	Not configs receive server
			return Globals.DEFAULT_VALUE_INT;
		}
		try (Folder folder = MailUtils.openFolder(this.store, Boolean.TRUE, folderName)) {
			if (folder.exists() && folder.isOpen()) {
				return folder.getMessageCount();
			}
		} catch (Exception e) {
			LOGGER.error("Receive_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return Globals.DEFAULT_VALUE_INT;
	}

	/**
	 * <h3 class="en-US">Read the mail UID list from the inbox</h3>
	 * <h3 class="zh-CN">读取收件箱中的邮件唯一标识列表</h3>
	 *
	 * @return <span class="en-US">Mail UID list</span>
	 * <span class="zh-CN">邮件唯一标识列表</span>
	 */
	public List<String> mailUids() {
		return this.mailUids(Globals.DEFAULT_EMAIL_FOLDER_INBOX);
	}

	/**
	 * <h3 class="en-US">Read the mail UID list from the given folder name</h3>
	 * <h3 class="zh-CN">读取给定文件夹中的邮件唯一标识列表</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @return <span class="en-US">Mail UID list</span>
	 * <span class="zh-CN">邮件唯一标识列表</span>
	 */
	public List<String> mailUids(final String folderName) {
		return mailUids(folderName, Globals.DEFAULT_VALUE_INT, Globals.DEFAULT_VALUE_INT);
	}

	/**
	 * <h3 class="en-US">Read the mail UID list from the given folder name limit index from beginning to end</h3>
	 * <h3 class="zh-CN">读取给定文件夹中的部分邮件唯一标识列表，从给定的起始索引号到终止索引号</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param begin      <span class="en-US">Begin index</span>
	 *                   <span class="zh-CN">起始索引号</span>
	 * @param end        <span class="en-US">End index</span>
	 *                   <span class="zh-CN">终止索引号</span>
	 * @return <span class="en-US">Mail UID list</span>
	 * <span class="zh-CN">邮件唯一标识列表</span>
	 */
	public List<String> mailUids(final String folderName, final int begin, final int end) {
		if (this.recvSession == null || end < begin) {
			return Collections.emptyList();
		}

		try (Folder folder = MailUtils.openFolder(this.store, Boolean.TRUE, folderName)) {
			if (!folder.exists() || !folder.isOpen()) {
				return Collections.emptyList();
			}

			int totalCount = folder.getMessageCount();
			List<String> mailList = new ArrayList<>();
			int start = Math.max(1, begin);
			int stop = (end < 0) ? totalCount : Math.min(totalCount, end);
			for (Message message : folder.getMessages(start, stop)) {
				mailList.add(MailUtils.readUID(folder, message));
			}
			return mailList;
		} catch (Exception e) {
			LOGGER.error("Receive_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * <h3 class="en-US">Read mail information from the given folder name and UID</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识读取邮件信息</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uid        <span class="en-US">E-Mail UID</span>
	 *                   <span class="zh-CN">邮件唯一标识</span>
	 * @return <span class="en-US">Read MailObject instance</span>
	 * <span class="zh-CN">读取的电子邮件信息实例对象</span>
	 */
	public MailObject readMail(final String folderName, final String uid) {
		return this.readMail(folderName, uid, Boolean.FALSE);
	}

	/**
	 * <h3 class="en-US">Read mail content information from the given folder name and UID</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识读取邮件详细信息</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uid        <span class="en-US">E-Mail UID</span>
	 *                   <span class="zh-CN">邮件唯一标识</span>
	 * @param detail     <span class="en-US">Read detail status</span>
	 *                   <span class="zh-CN">读取全部信息状态</span>
	 * @return <span class="en-US">Read MailObject instance</span>
	 * <span class="zh-CN">读取的电子邮件信息实例对象</span>
	 */
	public MailObject readMail(final String folderName, final String uid, final boolean detail) {
		if (this.recvSession == null) {
			return null;
		}
		try (Folder folder = MailUtils.openFolder(this.store, Boolean.TRUE, folderName)) {
			if (!folder.exists() || !folder.isOpen()) {
				return null;
			}

			Message message = MailUtils.readMessage(folder, uid);
			if (message != null) {
				return receiveMessage((MimeMessage) message, detail);
			}
		} catch (Exception e) {
			LOGGER.error("Receive_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}

		return null;
	}

	/**
	 * <h3 class="en-US">Read the mail content information list from the given folder name and UID array</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识数组读取邮件详细信息</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uidArrays  <span class="en-US">E-Mail UID array</span>
	 *                   <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Read MailObject instance list</span>
	 * <span class="zh-CN">读取的电子邮件信息实例对象列表</span>
	 */
	public List<MailObject> readMailList(final String folderName, final String... uidArrays) {
		List<MailObject> mailList = new ArrayList<>();
		if (this.recvSession == null) {
			return mailList;
		}

		try (Folder folder = MailUtils.openFolder(this.store, Boolean.TRUE, folderName)) {
			if (!folder.exists() || !folder.isOpen()) {
				return mailList;
			}
			MailUtils.readMessages(folder, uidArrays)
					.forEach(message ->
							Optional.ofNullable(receiveMessage((MimeMessage) message, Boolean.FALSE))
									.ifPresent(mailList::add));
		} catch (Exception e) {
			LOGGER.error("Receive_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}

		return mailList;
	}

	/**
	 * <h3 class="en-US">Set mails status as read with the given folder name and UID array</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识数组，将对应的邮件置为已读状态</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uidArrays  <span class="en-US">E-Mail UID array</span>
	 *                   <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	public boolean readMails(final String folderName, final String... uidArrays) {
		return this.flagMailsStatus(Flags.Flag.SEEN, Boolean.TRUE, folderName, uidArrays);
	}

	/**
	 * <h3 class="en-US">Set mails status as unread with the given folder name and UID array</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识数组，将对应的邮件置为未读状态</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uidArrays  <span class="en-US">E-Mail UID array</span>
	 *                   <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	public boolean unreadMails(final String folderName, final String... uidArrays) {
		return this.flagMailsStatus(Flags.Flag.SEEN, Boolean.FALSE, folderName, uidArrays);
	}

	/**
	 * <h3 class="en-US">Set mails status as the answer with the given folder name and UID array</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识数组，将对应的邮件置为已回复状态</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uidArrays  <span class="en-US">E-Mail UID array</span>
	 *                   <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	public boolean answerMails(final String folderName, final String... uidArrays) {
		return this.flagMailsStatus(Flags.Flag.ANSWERED, Boolean.TRUE, folderName, uidArrays);
	}

	/**
	 * <h3 class="en-US">Delete mails by the given folder name and UID array</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识数组，将对应的邮件转移到回收站</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uidArrays  <span class="en-US">E-Mail UID array</span>
	 *                   <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	public boolean deleteMails(final String folderName, final String... uidArrays) {
		return this.flagMailsStatus(Flags.Flag.DELETED, Boolean.TRUE, folderName, uidArrays);
	}

	/**
	 * <h3 class="en-US">Recovery mails by given folder name and UID array</h3>
	 * <h3 class="zh-CN">根据邮件唯一标识数组，将对应的邮件置从回收站转移到给定的文件夹</h3>
	 * Set mails status as read by the uid list
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uidArrays  <span class="en-US">E-Mail UID array</span>
	 *                   <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	public boolean recoverMails(final String folderName, final String... uidArrays) {
		if (this.recvSession == null) {
			return Boolean.FALSE;
		}
		try (Folder folder = MailUtils.openFolder(this.store, Boolean.FALSE, folderName);
		     Folder inbox = MailUtils.openFolder(this.store, Boolean.FALSE, Globals.DEFAULT_EMAIL_FOLDER_INBOX)) {
			if (!folder.exists() || !folder.isOpen()) {
				return Boolean.FALSE;
			}

			folder.copyMessages(MailUtils.readMessages(folder, uidArrays).toArray(new Message[0]), inbox);
			return Boolean.TRUE;
		} catch (Exception e) {
			LOGGER.error("Set_Status_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Set mails status as the flag with the given folder name and UID array</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识数组，将对应的邮件置为标记状态</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uidArrays  <span class="en-US">E-Mail UID array</span>
	 *                   <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	public boolean flagMails(final String folderName, final String... uidArrays) {
		return this.flagMailsStatus(Flags.Flag.FLAGGED, Boolean.TRUE, folderName, uidArrays);
	}

	/**
	 * <h3 class="en-US">Set mails status as unflag with the given folder name and UID array</h3>
	 * <h3 class="zh-CN">根据给定的文件夹名和邮件唯一标识数组，将对应的邮件置为未标记状态</h3>
	 *
	 * @param folderName <span class="en-US">folder name</span>
	 *                   <span class="zh-CN">文件夹名称</span>
	 * @param uidArrays  <span class="en-US">E-Mail UID array</span>
	 *                   <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	public boolean unflagMails(final String folderName, final String... uidArrays) {
		return this.flagMailsStatus(Flags.Flag.FLAGGED, Boolean.FALSE, folderName, uidArrays);
	}

	/**
	 * <h3 class="en-US">Connect to the mail server</h3>
	 * <h3 class="zh-CN">连接到电子邮件服务器</h3>
	 *
	 * @throws MessagingException <span class="en-US">If connect failed</span>
	 *                            <span class="zh-CN">如果连接失败</span>
	 */
	private void connect() throws MessagingException {
		if (this.store == null || !this.store.isConnected()) {
			if (this.store == null) {
				this.store = this.recvSession.getStore(this.receiveProtocol);
			}
			this.store.connect(this.receiveConfig.getHostName(), this.receiveConfig.getHostPort(),
					this.userName, this.passWord);
		}
	}

	/**
	 * <h3 class="en-US">Set mails status with parameters</h3>
	 * <h3 class="zh-CN">根据参数信息设置对应的邮件状态</h3>
	 *
	 * @param flag      <span class="en-US">Flag code</span>
	 *                  <span class="zh-CN">标记类型</span>
	 * @param status    <span class="en-US">Flag status</span>
	 *                  <span class="zh-CN">标记状态</span>
	 * @param uidArrays <span class="en-US">E-Mail UID array</span>
	 *                  <span class="zh-CN">邮件唯一标识数组</span>
	 * @return <span class="en-US">Process result</span>
	 * <span class="zh-CN">操作结果</span>
	 */
	private boolean flagMailsStatus(final Flags.Flag flag, final boolean status,
	                                final String folderName, final String... uidArrays) {
		if (this.recvSession == null) {
			return Boolean.FALSE;
		}
		try (Folder folder = MailUtils.openFolder(this.store, Boolean.FALSE, folderName)) {
			if (!folder.exists() || !folder.isOpen()) {
				return Boolean.FALSE;
			}

			for (Message message : MailUtils.readMessages(folder, uidArrays)) {
				message.setFlag(flag, status);
			}
			return true;
		} catch (Exception e) {
			LOGGER.error("Set_Status_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return Boolean.FALSE;
		}
	}

	/**
	 * <h3 class="en-US">Verify e-mail signature</h3>
	 * <h3 class="zh-CN">验证电子邮件签名</h3>
	 *
	 * @param mimeMessage <span class="en-US">E-Mail MimeMessage instance</span>
	 *                    <span class="zh-CN">电子邮件信息实例对象</span>
	 * @return <span class="en-US">Verify result</span>
	 * <span class="zh-CN">验证结果</span>
	 */
	@SuppressWarnings("unchecked")
	private boolean verifyMessage(final MimeMessage mimeMessage) {
		try {
			MimeMessage signedMessage = new MimeMessage(mimeMessage);
			SMIMESignedParser signedParser;
			if (signedMessage.isMimeType("multipart/signed")) {
				signedParser = new SMIMESignedParser(new JcaDigestCalculatorProviderBuilder().build(),
						(MimeMultipart) signedMessage.getContent());
			} else if (signedMessage.isMimeType("application/pkcs7-mime")) {
				signedParser = new SMIMESignedParser(new JcaDigestCalculatorProviderBuilder().build(), signedMessage);
			} else {
				return Boolean.TRUE;
			}

			org.bouncycastle.util.Store<?> certificates = signedParser.getCertificates();
			for (SignerInformation signerInformation : signedParser.getSignerInfos().getSigners()) {
				Collection<?> certCollection = certificates.getMatches(signerInformation.getSID());
				X509Certificate certificate =
						new JcaX509CertificateConverter().setProvider("BC")
								.getCertificate((X509CertificateHolder) certCollection.iterator().next());

				try {
					SignerInformationVerifier signerInformationVerifier =
							new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(certificate);
					return signerInformation.verify(signerInformationVerifier);
				} catch (Exception e) {
					LOGGER.error("Verify_Signature_Mail_Error");
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Stack_Message_Error", e);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Verify_Signature_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * <h3 class="en-US">Parse MimeMessage instance to MailObject instance</h3>
	 * <h3 class="zh-CN">解析电子邮件MIME信息实例对象并转换为电子邮件信息实例对象</h3>
	 *
	 * @param mimeMessage <span class="en-US">E-Mail MimeMessage instance</span>
	 *                    <span class="zh-CN">电子邮件MIME信息实例对象</span>
	 * @param detail      <span class="en-US">Read detail status</span>
	 *                    <span class="zh-CN">读取全部信息状态</span>
	 * @return <span class="en-US">Read MailObject instance</span>
	 * <span class="zh-CN">读取的电子邮件信息实例对象</span>
	 */
	private MailObject receiveMessage(final MimeMessage mimeMessage, final boolean detail) {
		if (!verifyMessage(mimeMessage)) {
			return null;
		}
		try {
			MailObject mailObject = new MailObject();
			List<String> receiveList = new ArrayList<>();
			Address[] allRecipients = mimeMessage.getAllRecipients();
			if (allRecipients == null) {
				return null;
			}
			Arrays.stream(allRecipients)
					.filter(InternetAddress.class::isInstance)
					.forEach(address -> receiveList.add(((InternetAddress) address).getAddress().toLowerCase()));
			if (!receiveList.contains(this.userName)) {
				throw new MessagingException("Current account not in receive list! ");
			}

			mailObject.setReceiveAddress(receiveList);

			Folder folder = mimeMessage.getFolder();

			if (folder instanceof POP3Folder) {
				mailObject.setUid(((POP3Folder) folder).getUID(mimeMessage));
			} else if (folder instanceof IMAPFolder) {
				mailObject.setUid(Long.valueOf(((IMAPFolder) folder).getUID(mimeMessage)).toString());
			}
			String subject = mimeMessage.getSubject();

			if (StringUtils.notBlank(subject)) {
				mailObject.setSubject(MimeUtility.decodeText(subject));
			} else {
				mailObject.setSubject(Globals.DEFAULT_VALUE_STRING);
			}
			mailObject.setSendDate(mimeMessage.getSentDate());
			mailObject.setSendAddress(MimeUtility.decodeText(InternetAddress.toString(mimeMessage.getFrom())));

			if (detail) {
				//	Read mail cc address
				Optional.ofNullable((InternetAddress[]) mimeMessage.getRecipients(Message.RecipientType.CC))
						.ifPresent(ccAddress -> {
							List<String> ccList = new ArrayList<>();
							Arrays.asList(ccAddress).forEach(address -> ccList.add(address.getAddress()));
							mailObject.setCcAddress(ccList);
						});

				//	Read mail bcc address
				Optional.ofNullable((InternetAddress[]) mimeMessage.getRecipients(Message.RecipientType.BCC))
						.ifPresent(bccAddress -> {
							List<String> bccList = new ArrayList<>();
							Arrays.asList(bccAddress).forEach(address -> bccList.add(address.getAddress()));
							mailObject.setBccAddress(bccList);
						});

				//	Read the mail content message
				StringBuilder contentBuffer = new StringBuilder();
				MailUtils.readMailContent(mimeMessage, contentBuffer);
				mailObject.setContent(contentBuffer.toString());
				mailObject.setContentType(mimeMessage.getContentType());

				mailObject.setAttachFiles(getMailAttachment(mimeMessage));
			}

			return mailObject;
		} catch (MessagingException | IOException e) {
			LOGGER.error("Receive_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
			return null;
		}
	}

	/**
	 * <h3 class="en-US">Read attachment files from the given part of e-mail MIME information</h3>
	 * <h3 class="zh-CN">从给定的电子邮件MIME信息中读取附件文件</h3>
	 *
	 * @param part <span class="en-US">part of e-mail MIME information</span>
	 *             <span class="zh-CN">电子邮件MIME信息</span>
	 * @return <span class="en-US">Read file path list</span>
	 * <span class="zh-CN">读取的附件文件路径列表</span>
	 * @throws MessagingException <span class="en-US">If an error occurs when read attachment information</span>
	 *                            <span class="zh-CN">当读取附件数据时出现异常</span>
	 * @throws IOException        <span class="en-US">If an error occurs when save file to local</span>
	 *                            <span class="zh-CN">当写入数据到本地文件时出现异常</span>
	 */
	private List<String> getMailAttachment(final Part part) throws MessagingException, IOException {
		List<String> saveFiles = new ArrayList<>();
		if (StringUtils.isEmpty(this.storagePath)) {
			throw new IOException("Save attach file path error! ");
		}
		if (part.isMimeType(Globals.DEFAULT_CONTENT_TYPE_MULTIPART)) {
			Multipart multipart = (Multipart) part.getContent();
			int count = multipart.getCount();
			for (int i = 0; i < count; i++) {
				Optional.ofNullable(multipart.getBodyPart(i))
						.ifPresent(bodyPart -> this.readBodyPart(bodyPart, saveFiles));
			}
		}
		return saveFiles;
	}

	/**
	 * <h3 class="en-US">Read attachment files from the given body part of MIME information</h3>
	 * <h3 class="zh-CN">从给定的电子邮件MIME信息体中读取附件文件</h3>
	 *
	 * @param bodyPart  <span class="en-US">body part of MIME information</span>
	 *                  <span class="zh-CN">电子邮件MIME信息体</span>
	 * @param saveFiles <span class="en-US">Saved file path list</span>
	 *                  <span class="zh-CN">已保存的文件路径列表</span>
	 */
	private void readBodyPart(final Part bodyPart, final List<String> saveFiles) {
		try {
			if (bodyPart.getHeader("Content-ID") != null
					&& bodyPart.getHeader("Content-ID").length > 0) {
				return;
			}
			if (StringUtils.notBlank(bodyPart.getFileName())) {
				String disposition = bodyPart.getDisposition();
				if (disposition != null
						&& (disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE))) {
					String savePath = this.storagePath + Globals.DEFAULT_PAGE_SEPARATOR
							+ MimeUtility.decodeText(bodyPart.getFileName());
					if (!savePath.toLowerCase().endsWith("p7s")) {
						boolean saveFile = FileUtils.saveFile(bodyPart.getInputStream(), savePath);
						if (saveFile) {
							saveFiles.add(savePath);
						}
					}
				} else if (bodyPart.isMimeType(Globals.DEFAULT_CONTENT_TYPE_MULTIPART)) {
					saveFiles.addAll(getMailAttachment(bodyPart));
				}
			}
		} catch (MessagingException | IOException e) {
			LOGGER.error("Attachment_Receive_Mail_Error");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Stack_Message_Error", e);
			}
		}
	}

	@Override
	public void close() throws IOException {
		try {
			this.store.close();
		} catch (MessagingException e) {
			throw new IOException(e);
		}
	}
}
