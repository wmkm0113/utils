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
package org.nervousync.security.crypto;

import jakarta.annotation.Nonnull;
import org.nervousync.beans.crypto.CipherConfig;
import org.nervousync.beans.crypto.CipherKey;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.security.SecurityUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.security.spec.AlgorithmParameterSpec;

/**
 * <h2 class="en-US">Abstract symmetric crypto adapter class</h2>
 * <h2 class="zh-CN">对称加密解密适配器的抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 13:16:24 $
 */
public abstract class SymmetricCryptoAdaptorImpl extends BaseCryptoAdaptorImpl {

	private final String providerName;
	/**
	 * <span class="en-US">Result data bytes output stream</span>
	 * <span class="zh-CN">结果数据二进制数组输出流</span>
	 */
	private ByteArrayOutputStream byteArrayOutputStream;

	protected SymmetricCryptoAdaptorImpl(@Nonnull final String providerName, @Nonnull final CipherConfig cipherConfig,
	                                     @Nonnull final CryptoMode cryptoMode, @Nonnull final CipherKey cipherKey) {
		super(cipherConfig, cryptoMode, cipherKey);
		this.providerName = providerName;
		this.reset();
	}

	@Override
	public final void append(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		if (dataBytes.length < (position + length)) {
			throw new CryptoException(0x000000150001L);
		}
		switch (this.cryptoMode) {
			case ENCRYPT:
			case DECRYPT:
				this.byteArrayOutputStream.write(dataBytes, position, length);
				break;
			default:
				throw new CryptoException(0x000000150003L);
		}
	}

	@Override
	public final byte[] finish(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		switch (this.cryptoMode) {
			case ENCRYPT:
			case DECRYPT:
				try {
					this.byteArrayOutputStream.write(dataBytes, position, length);
					return this.cipher.doFinal(this.byteArrayOutputStream.toByteArray(), Globals.INITIALIZE_INT_VALUE,
							this.byteArrayOutputStream.size());
				} catch (IllegalBlockSizeException | BadPaddingException e) {
					throw new CryptoException(0x000000150004L, e);
				} finally {
					this.reset();
				}
			case SIGNATURE:
			case VERIFY:
				throw new CryptoException(0x00000015000CL);
			default:
				throw new CryptoException(0x000000150003L);
		}
	}

	@Override
	public final boolean verify(final byte[] signature) throws CryptoException {
		throw new CryptoException(0x00000015000CL);
	}

	@Override
	public final void reset() throws CryptoException {
		this.byteArrayOutputStream = new ByteArrayOutputStream();
		AlgorithmParameterSpec parameterSpec = null;
		int ivLength = this.cipherConfig.ivLength();
		if (ivLength > 0) {
			byte[] ivContent = new byte[ivLength];
			System.arraycopy(SecurityUtils.SHA256(this.cipherKey.getKeyBytes()),
					Globals.INITIALIZE_INT_VALUE, ivContent, Globals.INITIALIZE_INT_VALUE, ivContent.length);
			parameterSpec = new IvParameterSpec(ivContent);
		}
		try {
			Cipher cipherInstance = StringUtils.isEmpty(this.providerName)
					? Cipher.getInstance(this.cipherConfig.toString())
					: Cipher.getInstance(this.cipherConfig.toString(), this.providerName);
			switch (this.cryptoMode) {
				case ENCRYPT:
					cipherInstance.init(Cipher.ENCRYPT_MODE, this.cipherKey.generateKey(this.cryptoMode), parameterSpec);
					break;
				case DECRYPT:
					cipherInstance.init(Cipher.DECRYPT_MODE, this.cipherKey.generateKey(this.cryptoMode), parameterSpec);
					break;
				default:
					throw new CryptoException(0x000000150009L);
			}
			this.cipher = cipherInstance;
		} catch (Exception e) {
			if (e instanceof CryptoException) {
				throw (CryptoException) e;
			}
			throw new CryptoException(0x00000015000BL, e);
		}
	}
}
