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

/**
 * The type Extra data record.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 28, 2017 16:43:23 $
 */
public final class ExtraDataRecord {

	private long header;
	private int dataSize;
	private byte[] dataContent;

	/**
	 * Instantiates a new Extra data record.
	 */
	public ExtraDataRecord() {
	}

	/**
	 * Gets header.
	 *
	 * @return the header
	 */
	public long getHeader() {
		return this.header;
	}

	/**
	 * Sets header.
	 *
	 * @param header the header to set
	 */
	public void setHeader(final long header) {
		this.header = header;
	}

	/**
	 * Gets data size.
	 *
	 * @return the dataSize
	 */
	public int getDataSize() {
		return this.dataSize;
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
	 * Get data content byte [ ].
	 *
	 * @return the dataContent
	 */
	public byte[] getDataContent() {
		return this.dataContent == null ? new byte[0] : this.dataContent.clone();
	}

	/**
	 * Sets data content.
	 *
	 * @param dataContent the dataContent to set
	 */
	public void setDataContent(final byte[] dataContent) {
		this.dataContent = dataContent == null ? new byte[0] : dataContent.clone();
	}
}
