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
package org.nervousync.zip.models.header;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.nervousync.commons.Globals;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.zip.crypto.impl.aes.AESCrypto;
import org.nervousync.zip.models.header.utils.HeaderOperator;

/**
 * The type Local file header.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 29, 2017 14:29:26 $
 */
public final class LocalFileHeader extends FileHeader {

	private byte[] extraField;
	private long offsetStartOfData;
	private boolean writeCompressSizeInZip64ExtraRecord;

	/**
	 * Instantiates a new Local file header.
	 */
	public LocalFileHeader() {
	}

	/**
	 * Get extra field byte [ ].
	 *
	 * @return the extraField
	 */
	public byte[] getExtraField() {
		return this.extraField == null ? new byte[Globals.INITIALIZE_INT_VALUE] : this.extraField.clone();
	}

	/**
	 * Sets extra field.
	 *
	 * @param extraField the extraField to set
	 */
	public void setExtraField(final byte[] extraField) {
		this.extraField = extraField == null ? new byte[0] : extraField.clone();
	}

	/**
	 * Gets offset start of data.
	 *
	 * @return the offsetStartOfData
	 */
	public long getOffsetStartOfData() {
		return this.offsetStartOfData;
	}

	/**
	 * Sets offset start of data.
	 *
	 * @param offsetStartOfData the offsetStartOfData to set
	 */
	public void setOffsetStartOfData(final long offsetStartOfData) {
		this.offsetStartOfData = offsetStartOfData;
	}

	/**
	 * Is writing compress size in zip 64 extra record boolean.
	 *
	 * @return the writeCompressSizeInZip64ExtraRecord
	 */
	public boolean isWriteCompressSizeInZip64ExtraRecord() {
		return this.writeCompressSizeInZip64ExtraRecord;
	}

	/**
	 * Sets write compress size in zip 64 extra record.
	 *
	 * @param writeCompressSizeInZip64ExtraRecord the writeCompressSizeInZip64ExtraRecord to set
	 */
	public void setWriteCompressSizeInZip64ExtraRecord(final boolean writeCompressSizeInZip64ExtraRecord) {
		this.writeCompressSizeInZip64ExtraRecord = writeCompressSizeInZip64ExtraRecord;
	}

	/**
	 * Verify password boolean.
	 *
	 * @param input the input
	 * @return the boolean
	 */
	public boolean verifyPassword(final DataInput input) {
		if (!this.isEncrypted()) {
			return Boolean.TRUE;
		}

		try {
			if (this.getEncryptionMethod() == Globals.ENC_METHOD_AES) {
				if (this.getAesExtraDataRecord() != null) {
					byte[] salt = new byte[HeaderOperator.saltLength(this.getAesExtraDataRecord().getAesStrength())];
					int readLength = Globals.DEFAULT_VALUE_INT;
					if (input instanceof RandomAccessFile) {
						((RandomAccessFile) input).seek(this.getOffsetStartOfData());
						readLength = ((RandomAccessFile) input).read(salt);
					} else if (input instanceof InputStream) {
						long skipLength = ((InputStream) input).skip(this.getOffsetStartOfData());
						if (skipLength == this.getOffsetStartOfData()) {
							readLength = ((InputStream) input).read(salt);
						}
					}

					if (readLength != salt.length) {
						return Boolean.FALSE;
					}

					byte[] passwordBytes = new byte[2];
					if (input instanceof RandomAccessFile) {
						readLength = ((RandomAccessFile) input).read(passwordBytes);
					} else {
						readLength = ((InputStream) input).read(passwordBytes);
					}

					if (readLength == 2) {
						return AESCrypto.verifyPassword(this.getAesExtraDataRecord().getAesStrength(),
								salt, this.getPassword(), passwordBytes);
					}
				}
				return Boolean.FALSE;
			} else if (this.getEncryptionMethod() == Globals.ENC_METHOD_STANDARD) {
				//	Not supported verify password of standard encrypting
				return Boolean.TRUE;
			} else {
				throw new ZipException(0x0000001B0001L);
			}
		} catch (IOException | ZipException e) {
			return Boolean.FALSE;
		}
	}
}
