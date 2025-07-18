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
package org.nervousync.zip.crypto.impl.aes;

import org.nervousync.commons.Globals;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.zip.crypto.Cryptor;

/**
 * Encryptor implement of AES
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 30, 2017 15:42:11 $
 */
public final class AESEncryptor extends AESCrypto implements Cryptor {

	private boolean finished = Boolean.FALSE;

	/**
	 * Instantiates a new Aes encryptor.
	 *
	 * @param password    the password
	 * @param aesStrength the aes strength
	 * @throws ZipException the zip exception
	 */
	public AESEncryptor(final char[] password, final int aesStrength) throws ZipException {
		super.preInit(aesStrength);
		this.init(password);
	}

	@Override
	public void process(final byte[] buff, final int start, final int len) throws ZipException {
		if (this.finished) {
			throw new ZipException(0x0000001B0012L);
		}

		if (len % 16 != 0) {
			this.finished = Boolean.TRUE;
		}

		try {
			for (int i = start; i < (start + len); i += Globals.AES_BLOCK_SIZE) {
				this.loopCount =
						(i + Globals.AES_BLOCK_SIZE <= (start + len))
								? Globals.AES_BLOCK_SIZE
								: ((start + len) - i);
				super.processData(buff, i);
				this.macBasedPRF.append(buff, i, this.loopCount);
			}
		} catch (CryptoException | DataInvalidException e) {
			throw new ZipException(0x0000001B000CL, e);
		}
	}

	/**
	 * Get final mac byte [ ].
	 *
	 * @return the byte [ ]
	 */
	public byte[] getFinalMac() {
		try {
			byte[] rawMacBytes = this.macBasedPRF.finish();
			byte[] macBytes = new byte[10];
			System.arraycopy(rawMacBytes, 0, macBytes, 0, 10);
			return macBytes;
		} catch (CryptoException e) {
			return new byte[0];
		}
	}

	/**
	 * Get derived password verifier byte [ ].
	 *
	 * @return the derivedPasswordVerifier
	 */
	public byte[] getDerivedPasswordVerifier() {
		return derivedPasswordVerifier == null ? new byte[0] : derivedPasswordVerifier.clone();
	}
}
