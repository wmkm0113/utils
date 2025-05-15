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
package org.nervousync.zip.models;

import org.nervousync.commons.Globals;

/**
 * The type Aes extra data record.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 28, 2017 16:50:18 $
 */
public final class AESExtraDataRecord {

	private long signature;
	private int dataSize;
	private int versionNumber;
	private String vendorID;
	private int aesStrength;
	private int compressionMethod;

	/**
	 * Instantiates a new Aes extra data record.
	 */
	public AESExtraDataRecord() {
		this.signature = Globals.DEFAULT_VALUE_LONG;
		this.dataSize = Globals.DEFAULT_VALUE_INT;
		this.versionNumber = Globals.DEFAULT_VALUE_INT;
		this.vendorID = null;
		this.aesStrength = Globals.DEFAULT_VALUE_INT;
		this.compressionMethod = Globals.DEFAULT_VALUE_INT;
	}

	/**
	 * Gets signature.
	 *
	 * @return the signature
	 */
	public long getSignature() {
		return signature;
	}

	/**
	 * Sets signature.
	 *
	 * @param signature the signature to set
	 */
	public void setSignature(final long signature) {
		this.signature = signature;
	}

	/**
	 * Gets data size.
	 *
	 * @return the dataSize
	 */
	public int getDataSize() {
		return dataSize;
	}

	/**
	 * Sets data size.
	 *
	 * @param dataSize the dataSize to set
	 */
	public void setDataSize(final int dataSize) {
		this.dataSize = dataSize;
	}

	/**
	 * Gets version number.
	 *
	 * @return the versionNumber
	 */
	public int getVersionNumber() {
		return versionNumber;
	}

	/**
	 * Sets version number.
	 *
	 * @param versionNumber the versionNumber to set
	 */
	public void setVersionNumber(final int versionNumber) {
		this.versionNumber = versionNumber;
	}

	/**
	 * Gets vendor id.
	 *
	 * @return the vendorID
	 */
	public String getVendorID() {
		return vendorID;
	}

	/**
	 * Sets vendor id.
	 *
	 * @param vendorID the vendorID to set
	 */
	public void setVendorID(final String vendorID) {
		this.vendorID = vendorID;
	}

	/**
	 * Gets aes strength.
	 *
	 * @return the aesStrength
	 */
	public int getAesStrength() {
		return aesStrength;
	}

	/**
	 * Sets aes strength.
	 *
	 * @param aesStrength the aesStrength to set
	 */
	public void setAesStrength(final int aesStrength) {
		this.aesStrength = aesStrength;
	}

	/**
	 * Gets compression method.
	 *
	 * @return the compressionMethod
	 */
	public int getCompressionMethod() {
		return compressionMethod;
	}

	/**
	 * Sets compression method.
	 *
	 * @param compressionMethod the compressionMethod to set
	 */
	public void setCompressionMethod(final int compressionMethod) {
		this.compressionMethod = compressionMethod;
	}
}
