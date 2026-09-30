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

package org.nervousync.test.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.commons.Globals;
import org.nervousync.security.password.impl.PBKDF2EncoderImpl;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.password.PasswordUtils;

public final class PasswordTest extends BaseTest {

	@AfterEach
	public void destroy() {
		PasswordUtils.destroy();
	}

	@Test
	@Order(0)
	public void argon2id() {
		PasswordUtils.initialize(64, 3, 1);
		String encPwd = PasswordUtils.encode("TestPassword");
		this.logger.info("Password_Encode", "Argon2id", encPwd);
		this.logger.info("Password_Verify", PasswordUtils.verify("TestPassword", encPwd));
		this.logger.info("Password_Upgrade", PasswordUtils.needsUpgrade(encPwd));
		PasswordUtils.destroy();
		PasswordUtils.initialize(128, 3, 1);
		this.logger.info("Password_Upgrade", PasswordUtils.needsUpgrade(encPwd));
	}

	@Test
	@Order(10)
	public void bcrypt() {
		PasswordUtils.initialize(Globals.DEFAULT_VALUE_INT);
		String encPwd = PasswordUtils.encode("TestPassword");
		this.logger.info("Password_Encode", "bcrypt", encPwd);
		this.logger.info("Password_Verify", PasswordUtils.verify("TestPassword", encPwd));
		this.logger.info("Password_Upgrade", PasswordUtils.needsUpgrade(encPwd));
		PasswordUtils.destroy();
		PasswordUtils.initialize(14);
		this.logger.info("Password_Upgrade", PasswordUtils.needsUpgrade(encPwd));
	}

	@Test
	@Order(20)
	public void pbkdf2() {
		PasswordUtils.initialize(PBKDF2EncoderImpl.Algorithm.SHA256, 60000);
		String encPwd = PasswordUtils.encode("TestPassword");
		this.logger.info("Password_Encode", "PBKDF2", encPwd);
		this.logger.info("Password_Verify", PasswordUtils.verify("TestPassword", encPwd));
		this.logger.info("Password_Upgrade", PasswordUtils.needsUpgrade(encPwd));
		PasswordUtils.destroy();
		PasswordUtils.initialize(PBKDF2EncoderImpl.Algorithm.SHA512, 100000);
		this.logger.info("Password_Upgrade", PasswordUtils.needsUpgrade(encPwd));
	}
}
