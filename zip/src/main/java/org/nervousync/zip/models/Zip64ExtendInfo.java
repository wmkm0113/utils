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
 * The type Zip 64 extend info.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 28, 2017 16:48:01 $
 */
public final class Zip64ExtendInfo {

	private int header = Globals.DEFAULT_VALUE_INT;
	private int size = Globals.DEFAULT_VALUE_INT;
	private long compressedSize = Globals.DEFAULT_VALUE_LONG;
	private long originalSize = Globals.DEFAULT_VALUE_LONG;
	private long offsetLocalHeader = Globals.DEFAULT_VALUE_LONG;
	private int diskNumberStart = Globals.DEFAULT_VALUE_INT;

	/**
	 * Instantiates new Zip64 extend info.
	 */
	public Zip64ExtendInfo() {
	}

	/**
	 * Gets header.
	 *
	 * @return the header
	 */
	public int getHeader() {
		return this.header;
	}

	/**
	 * Sets header.
	 *
	 * @param header the header to set
	 */
	public void setHeader(final int header) {
		this.header = header;
	}

	/**
	 * Gets size.
	 *
	 * @return the size
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * Sets size.
	 *
	 * @param size the size to set
	 */
	public void setSize(final int size) {
		this.size = size;
	}

	/**
	 * Gets compressed size.
	 *
	 * @return the compressedSize
	 */
	public long getCompressedSize() {
		return this.compressedSize;
	}

	/**
	 * Sets compressed size.
	 *
	 * @param compressedSize the compressedSize to set
	 */
	public void setCompressedSize(final long compressedSize) {
		this.compressedSize = compressedSize;
	}

	/**
	 * Gets original size.
	 *
	 * @return the originalSize
	 */
	public long getOriginalSize() {
		return this.originalSize;
	}

	/**
	 * Sets original size.
	 *
	 * @param originalSize the originalSize to set
	 */
	public void setOriginalSize(final long originalSize) {
		this.originalSize = originalSize;
	}

	/**
	 * Gets offset local header.
	 *
	 * @return the offsetLocalHeader
	 */
	public long getOffsetLocalHeader() {
		return this.offsetLocalHeader;
	}

	/**
	 * Sets offset local header.
	 *
	 * @param offsetLocalHeader the offsetLocalHeader to set
	 */
	public void setOffsetLocalHeader(final long offsetLocalHeader) {
		this.offsetLocalHeader = offsetLocalHeader;
	}

	/**
	 * Gets disk number start.
	 *
	 * @return the diskNumberStart
	 */
	public int getDiskNumberStart() {
		return this.diskNumberStart;
	}

	/**
	 * Sets disk number start.
	 *
	 * @param diskNumberStart the diskNumberStart to set
	 */
	public void setDiskNumberStart(final int diskNumberStart) {
		this.diskNumberStart = diskNumberStart;
	}
}
