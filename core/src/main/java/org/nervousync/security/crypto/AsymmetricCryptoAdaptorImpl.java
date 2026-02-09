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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Arrays;

/**
 * <h2 class="en-US">Abstract asymmetric crypto adapter class</h2>
 * <h2 class="zh-CN">非对称加密解密适配器的抽象类</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jan 13, 2012 12:27:33 $
 */
public abstract class AsymmetricCryptoAdaptorImpl extends BaseCryptoAdaptorImpl {

	private final String providerName;
	/**
	 * <span class="en-US">Signature instance</span>
	 * <span class="zh-CN">签名实例对象</span>
	 */
	private Signature signature = null;
	/**
	 * <span class="en-US">Cipher block length</span>
	 * <span class="zh-CN">加密块长度</span>
	 */
	private int blockLength;
	/**
	 * <span class="en-US">Cipher block size</span>
	 * <span class="zh-CN">块数据大小</span>
	 */
	private int blockSize;
	/**
	 * <span class="en-US">Data append buffer</span>
	 * <span class="zh-CN">数据填充缓冲器</span>
	 */
	private byte[] appendBuffer;
	/**
	 * <span class="en-US">Result data bytes</span>
	 * <span class="zh-CN">结果数据二进制数组</span>
	 */
	private byte[] dataBytes;

	protected AsymmetricCryptoAdaptorImpl(@Nonnull final String providerName, @Nonnull final CipherConfig cipherConfig,
	                                      @Nonnull final CryptoMode cryptoMode, @Nonnull final CipherKey cipherKey) {
		super(cipherConfig, cryptoMode, cipherKey);
		this.providerName = providerName;
		this.reset();
		this.appendBuffer = new byte[0];
		this.dataBytes = new byte[0];
	}

	@Override
	public final void append(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		if (dataBytes.length < (position + length)) {
			throw new CryptoException(0x000000150001L);
		}
		switch (this.cryptoMode) {
			case ENCRYPT:
			case DECRYPT:
				this.appendBuffer(dataBytes, position, length);
				this.process();
				break;
			case SIGNATURE:
			case VERIFY:
				try {
					this.signature.update(dataBytes);
				} catch (SignatureException e) {
					throw new CryptoException(0x000000150002L, e);
				}
				break;
			default:
				throw new CryptoException(0x000000150003L);
		}
	}

	@Override
	public final byte[] finish(final byte[] dataBytes, final int position, final int length) throws CryptoException {
		byte[] result;
		switch (this.cryptoMode) {
			case ENCRYPT:
			case DECRYPT:
				this.appendBuffer(dataBytes, position, length);
				this.process();
				if (this.appendBuffer.length > 0) {
					byte[] finalBytes = new byte[this.appendBuffer.length];
					System.arraycopy(this.appendBuffer, Globals.INITIALIZE_INT_VALUE, finalBytes,
							Globals.INITIALIZE_INT_VALUE, this.appendBuffer.length);
					try {
						byte[] encBytes = this.cipher.doFinal(finalBytes);
						result = concat(this.dataBytes, encBytes);
					} catch (IllegalBlockSizeException | BadPaddingException e) {
						throw new CryptoException(0x000000150004L, e);
					} finally {
						this.reset();
						this.appendBuffer = new byte[0];
					}
				} else {
					result = this.dataBytes;
				}
				this.dataBytes = new byte[0];
				break;
			case SIGNATURE:
				try {
					this.signature.update(dataBytes);
					result = this.signature.sign();
				} catch (SignatureException e) {
					throw new CryptoException(0x000000150005L, e);
				} finally {
					this.reset();
				}
				break;
			case VERIFY:
				throw new CryptoException(0x000000150006L);
			default:
				throw new CryptoException(0x000000150003L);
		}
		this.appendBuffer = new byte[0];
		this.dataBytes = new byte[0];
		return result;
	}

	@Override
	public final boolean verify(final byte[] signature) throws CryptoException {
		if (!CryptoMode.VERIFY.equals(this.cryptoMode)) {
			throw new CryptoException(0x000000150007L);
		}
		try {
			return this.signature.verify(signature);
		} catch (SignatureException e) {
			throw new CryptoException(0x000000150008L, e);
		} finally {
			this.reset();
		}
	}

	@Override
	public final void reset() throws CryptoException {
		int paddingLength = 0;
		AlgorithmParameterSpec parameterSpec = null;
		MGF1ParameterSpec mgf1ParameterSpec = null;
		switch (this.cipherConfig.getPadding()) {
			case "PKCS1Padding":
				paddingLength = 11;
				break;
			case "OAEPWithMD5AndMGF1Padding":
				paddingLength = 34;
				break;
			case "OAEPWithSHA-1AndMGF1Padding":
				mgf1ParameterSpec = MGF1ParameterSpec.SHA1;
				paddingLength = 42;
				break;
			case "OAEPWithSHA-224AndMGF1Padding":
				mgf1ParameterSpec = MGF1ParameterSpec.SHA224;
				paddingLength = 58;
				break;
			case "OAEPWithSHA-256AndMGF1Padding":
				mgf1ParameterSpec = MGF1ParameterSpec.SHA256;
				paddingLength = 66;
				break;
			case "OAEPWithSHA-384AndMGF1Padding":
				mgf1ParameterSpec = MGF1ParameterSpec.SHA384;
				paddingLength = 98;
				break;
			case "OAEPWithSHA-512AndMGF1Padding":
				mgf1ParameterSpec = MGF1ParameterSpec.SHA512;
				paddingLength = 130;
				break;
			case "OAEPWithSHA3-224AndMGF1Padding":
				mgf1ParameterSpec = new MGF1ParameterSpec("SHA3-224");
				paddingLength = 58;
				break;
			case "OAEPWithSHA3-256AndMGF1Padding":
				mgf1ParameterSpec = new MGF1ParameterSpec("SHA3-256");
				paddingLength = 66;
				break;
			case "OAEPWithSHA3-384AndMGF1Padding":
				mgf1ParameterSpec = new MGF1ParameterSpec("SHA3-384");
				paddingLength = 98;
				break;
			case "OAEPWithSHA3-512AndMGF1Padding":
				mgf1ParameterSpec = new MGF1ParameterSpec("SHA3-512");
				paddingLength = 130;
				break;
		}
		if (mgf1ParameterSpec != null) {
			parameterSpec = new OAEPParameterSpec(mgf1ParameterSpec.getDigestAlgorithm(), "MGF1",
					mgf1ParameterSpec, PSource.PSpecified.DEFAULT);
		}

		Key key = this.cipherKey.generateKey(this.cryptoMode);

		this.blockLength = rsaKeySize(key) >> 3;
		if ((CryptoMode.ENCRYPT.equals(cryptoMode) || CryptoMode.DECRYPT.equals(cryptoMode)) && paddingLength > 0) {
			this.blockSize = this.blockLength - paddingLength;
		} else {
			this.blockSize = this.blockLength;
		}
		try {
			switch (this.cryptoMode) {
				case ENCRYPT:
					this.cipher = StringUtils.isEmpty(this.providerName)
							? Cipher.getInstance(this.cipherConfig.toString())
							: Cipher.getInstance(this.cipherConfig.toString(), this.providerName);
					this.cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
					break;
				case DECRYPT:
					this.cipher = StringUtils.isEmpty(this.providerName)
							? Cipher.getInstance(this.cipherConfig.toString())
							: Cipher.getInstance(this.cipherConfig.toString(), this.providerName);
					this.cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
					break;
				case SIGNATURE:
					this.signature = Signature.getInstance(this.cipherConfig.getAlgorithm());
					this.signature.initSign((PrivateKey) key);
					break;
				case VERIFY:
					this.signature = Signature.getInstance(this.cipherConfig.getAlgorithm());
					this.signature.initVerify((PublicKey) key);
					break;
				default:
					throw new CryptoException(0x000000150009L);
			}
		} catch (Exception e) {
			if (e instanceof CryptoException) {
				throw (CryptoException) e;
			}
			throw new CryptoException(0x00000015000BL, e);
		}
	}

	/**
	 * <h3 class="en-US">Append parts of the given binary data array to data append buffer</h3>
	 * <h3 class="zh-CN">追加给定的二进制字节数组到当前数据追加缓冲器中</h3>
	 *
	 * @param dataBytes <span class="en-US">binary data array</span>
	 *                  <span class="zh-CN">二进制字节数组</span>
	 * @param position  <span class="en-US">Data begin position</span>
	 *                  <span class="zh-CN">数据起始坐标</span>
	 * @param length    <span class="en-US">Length of data append</span>
	 *                  <span class="zh-CN">追加的数据长度</span>
	 */
	private void appendBuffer(final byte[] dataBytes, final int position, final int length) {
		this.appendBuffer = ByteBuffer.allocate(this.appendBuffer.length + length)
				.put(this.appendBuffer)
				.put(dataBytes, position, length)
				.array();
	}

	/**
	 * <h3 class="en-US">Process append buffer data</h3>
	 * <h3 class="zh-CN">处理追加缓冲区中的数据</h3>
	 *
	 * @throws CryptoException <span class="en-US">If an error occurs when process data</span>
	 *                         <span class="zh-CN">当处理数据时出现异常</span>
	 */
	private void process() throws CryptoException {
		int blockLength = CryptoMode.ENCRYPT.equals(this.cryptoMode) ? this.blockSize : this.blockLength;
		if (blockLength == Globals.DEFAULT_VALUE_INT || this.appendBuffer.length < blockLength) {
			return;
		}
		int position = 0;
		while (position + blockLength < this.appendBuffer.length) {
			byte[] dataBytes = new byte[blockLength];
			System.arraycopy(this.appendBuffer, position, dataBytes, Globals.INITIALIZE_INT_VALUE, blockLength);
			try {
				byte[] encBytes = this.cipher.doFinal(dataBytes);
				this.dataBytes = concat(this.dataBytes, encBytes);
			} catch (IllegalBlockSizeException | BadPaddingException e) {
				throw new CryptoException(0x000000150004L, e);
			} finally {
				this.reset();
			}
			position += blockLength;
		}
		int remainLength = this.appendBuffer.length - position;
		this.appendBuffer = ByteBuffer.allocate(remainLength).put(this.appendBuffer, position, remainLength).array();
	}

	/**
	 * <h3 class="en-US">Concat binary data arrays</h3>
	 * <h3 class="zh-CN">处理追加缓冲区中的数据</h3>
	 *
	 * @param dataBytes   <span class="en-US">Original data bytes</span>
	 *                    <span class="zh-CN">原有字节数组</span>
	 * @param concatBytes <span class="en-US">Concat data bytes</span>
	 *                    <span class="zh-CN">合并连接的字节数组</span>
	 * @return <span class="en-US">Concat data bytes</span>
	 * <span class="zh-CN">合并连接后的字节数组</span>
	 */
	private static byte[] concat(final byte[] dataBytes, final byte[] concatBytes) {
		if (dataBytes == null || dataBytes.length == 0) {
			return concatBytes;
		}

		if (concatBytes == null || concatBytes.length == 0) {
			return dataBytes;
		}
		byte[] newBytes = Arrays.copyOf(dataBytes, dataBytes.length + concatBytes.length);
		System.arraycopy(concatBytes, Globals.INITIALIZE_INT_VALUE, newBytes, dataBytes.length, concatBytes.length);
		return newBytes;
	}

	/**
	 * <h3 class="en-US">Retrieve RSA Key Size</h3>
	 * <h3 class="zh-CN">读取RSA密钥长度</h3>
	 *
	 * @param key <span class="en-US">RSA key instance</span>
	 *            <span class="zh-CN">RSA密钥实例对象</span>
	 * @return <span class="en-US">Retrieve key size</span>
	 * <span class="zh-CN">读取的密钥长度</span>
	 */
	private static int rsaKeySize(final Key key) {
		if (key == null) {
			return Globals.DEFAULT_VALUE_INT;
		}
		try {
			if (key instanceof PrivateKey) {
				return KeyFactory.getInstance("RSA").getKeySpec(key, RSAPrivateKeySpec.class).getModulus().toString(2).length();
			} else if (key instanceof RSAPublicKey) {
				return KeyFactory.getInstance("RSA").getKeySpec(key, RSAPublicKeySpec.class).getModulus().toString(2).length();
			}
			return Globals.DEFAULT_VALUE_INT;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException ignored) {
			return Globals.DEFAULT_VALUE_INT;
		}
	}
}
