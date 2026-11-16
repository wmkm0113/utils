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
import org.nervousync.beans.security.CipherConfig;
import org.nervousync.beans.security.CipherKey;
import org.nervousync.beans.security.CipherParameters;
import org.nervousync.commons.Globals;
import org.nervousync.enumerations.crypto.CryptoMode;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.utils.commons.StringUtils;
import org.nervousync.utils.security.CryptoUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import java.io.ByteArrayOutputStream;
import java.security.spec.AlgorithmParameterSpec;

/**
 * <h2 class="en-US">Abstract symmetric crypto adapter class</h2>
 * <h2 class="zh-CN">对称加密解密适配器的抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 13:16:24 $
 */
public abstract class SymmetricCryptoAdaptorImpl extends CryptoAdaptorImpl {

	/**
	 * <span class="en-US">Provider name</span>
	 * <span class="zh-CN">适配器名</span>
	 */
	private final String providerName;
	/**
	 * <span class="en-US">Result data bytes output stream</span>
	 * <span class="zh-CN">结果数据二进制数组输出流</span>
	 */
	private ByteArrayOutputStream byteArrayOutputStream;
	/**
	 * <span class="en-US">Data mode</span>
	 * <span class="zh-CN">数据模式</span>
	 */
	private Mode mode = Mode.UNKNOWN;

	/**
	 * <h3 class="en-US">Constructor method for the abstract symmetric crypto adapter class</h3>
	 * <h3 class="zh-CN">对称加密解密适配器的抽象类的构造方法</h3>
	 *
	 * @param providerName <span class="en-US">Provider name</span>
	 *                     <span class="zh-CN">适配器名</span>
	 * @param cipherConfig <span class="en-US">Cipher configure</span>
	 *                     <span class="zh-CN">密码设置</span>
	 * @param cryptoMode   <span class="en-US">Crypto mode</span>
	 *                     <span class="zh-CN">加密解密模式</span>
	 * @param cipherKey    <span class="en-US">Crypto key</span>
	 *                     <span class="zh-CN">加密解密密钥</span>
	 * @param parameters   <span class="en-US">Crypto parameters</span>
	 *                     <span class="zh-CN">加密解密参数</span>
	 */
	protected SymmetricCryptoAdaptorImpl(@Nonnull final String providerName, @Nonnull final CipherConfig cipherConfig,
	                                     @Nonnull final CryptoMode cryptoMode, @Nonnull final CipherKey cipherKey,
	                                     final CipherParameters parameters) {
		this.providerName = providerName;
		this.config(cipherConfig, cryptoMode, cipherKey, parameters);
	}

	@Override
	public final void append(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		if (dataBytes == null || position < 0 || length < 0 || position > dataBytes.length - length) {
			throw new CryptoException(0x000000150001L);
		}
		switch (this.cryptoMode) {
			case ENCRYPT:
			case DECRYPT:
				if (this.checkMode(Mode.BUFFERED)) {
					this.byteArrayOutputStream.write(dataBytes, position, length);
				} else {
					throw new CryptoException(0x00000015000FL);
				}
				break;
			default:
				throw new CryptoException(0x000000150003L);
		}
	}

	@Override
	public final byte[] update(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		if (dataBytes == null || position < 0 || length < 0 || position > dataBytes.length - length) {
			throw new CryptoException(0x000000150001L);
		}
		switch (this.cryptoMode) {
			case ENCRYPT:
			case DECRYPT:
				if (this.checkMode(Mode.STREAM)) {
					byte[] result = this.cipher.update(dataBytes, position, length);
					return result == null ? new byte[0] : result;
				} else {
					throw new CryptoException(0x00000015000FL);
				}
			default:
				throw new CryptoException(0x000000150003L);
		}
	}

	@Override
	public final byte[] finish(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		if (dataBytes == null || position < 0 || length < 0 || position > dataBytes.length - length) {
			throw new CryptoException(0x000000150001L);
		}
		switch (this.cryptoMode) {
			case ENCRYPT:
			case DECRYPT:
				try {
					if (this.checkMode(Mode.BUFFERED)) {
						this.byteArrayOutputStream.write(dataBytes, position, length);
						return this.cipher.doFinal(this.byteArrayOutputStream.toByteArray(), Globals.INITIALIZE_INT_VALUE,
								this.byteArrayOutputStream.size());
					} else if (this.checkMode(Mode.STREAM)) {
						byte[] result = this.cipher.doFinal(dataBytes, position, length);
						return result == null ? new byte[0] : result;
					} else {
						throw new CryptoException(0x00000015000FL);
					}
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
		this.resetMode();
		AlgorithmParameterSpec parameterSpec = null;
		int ivLength = CryptoUtils.ivLength(this.cipherConfig.algorithm(), this.cipherConfig.mode());
		byte[] iv = (this.parameters == null) ? new byte[0] : this.parameters.getInitializationVector();
		if ("RC2".equalsIgnoreCase(this.cipherConfig.algorithm())
				&& this.parameters != null
				&& this.parameters.getEffectiveKeyBits() > 0) {
			parameterSpec = (ivLength > 0)
					? new RC2ParameterSpec(this.parameters.getEffectiveKeyBits(), iv)
					: new RC2ParameterSpec(this.parameters.getEffectiveKeyBits());
		} else if (ivLength > 0) {
			if (this.parameters == null || this.parameters.getInitializationVector().length != ivLength) {
				throw new CryptoException(0x00000015000BL);
			}
			parameterSpec = new IvParameterSpec(this.parameters.getInitializationVector());
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

	/**
	 * <h3 class="en-US">Check the current mode matched with expected mode</h3>
	 * <h3 class="zh-CN">检查使用的模式是否符合预期</h3>
	 *
	 * @param expectMode <span class="en-US">Expected mode</span>
	 *                   <span class="zh-CN">预期模式</span>
	 * @return <span class="en-US">Check result</span>
	 * <span class="zh-CN">检查结果</span>
	 * @throws CryptoException <span class="en-US">If expected mode is UNKNOWN</span>
	 *                         <span class="zh-CN">如果预期模式未知</span>
	 */
	private boolean checkMode(final Mode expectMode) throws CryptoException {
		if (expectMode == Mode.UNKNOWN) {
			throw new CryptoException(0x00000015000FL);
		}
		if (this.mode == Mode.UNKNOWN) {
			this.mode = expectMode;
		}
		return this.mode == expectMode;
	}

	/**
	 * <h3 class="en-US">Reset current mode</h3>
	 * <h3 class="zh-CN">重置当前模式</h3>
	 */
	private void resetMode() {
		this.mode = Mode.UNKNOWN;
	}

	private enum Mode {
		UNKNOWN, BUFFERED, STREAM
	}
}
