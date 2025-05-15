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
package org.nervousync.zip.models.central;

/**
 * The type End central directory record.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 28, 2017 16:34:55 $
 */
public final class EndCentralDirectoryRecord {

	private long signature;
	private int indexOfThisDisk;
	private int indexOfThisDiskStartOfCentralDirectory;
	private int totalOfEntriesInCentralDirectoryOnThisDisk;
	private int totalOfEntriesInCentralDirectory;
	private int sizeOfCentralDirectory;
	private long offsetOfStartOfCentralDirectory;
	private int commentLength;
	private byte[] commentBytes;

	/**
	 * Instantiates a new End central directory record.
	 */
	public EndCentralDirectoryRecord() {
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
	 * Gets index of this disk.
	 *
	 * @return the indexOfThisDisk
	 */
	public int getIndexOfThisDisk() {
		return indexOfThisDisk;
	}

	/**
	 * Sets index of this disk.
	 *
	 * @param indexOfThisDisk the indexOfThisDisk to set
	 */
	public void setIndexOfThisDisk(final int indexOfThisDisk) {
		this.indexOfThisDisk = indexOfThisDisk;
	}

	/**
	 * Gets index of this disk start of central directory.
	 *
	 * @return the indexOfThisDiskStartOfCentralDirectory
	 */
	public int getIndexOfThisDiskStartOfCentralDirectory() {
		return indexOfThisDiskStartOfCentralDirectory;
	}

	/**
	 * Sets index of this disk start of central directory.
	 *
	 * @param indexOfThisDiskStartOfCentralDirectory the indexOfThisDiskStartOfCentralDirectory to set
	 */
	public void setIndexOfThisDiskStartOfCentralDirectory(final int indexOfThisDiskStartOfCentralDirectory) {
		this.indexOfThisDiskStartOfCentralDirectory = indexOfThisDiskStartOfCentralDirectory;
	}

	/**
	 * Gets total of entries in the central directory on this disk.
	 *
	 * @return the totalOfEntriesInCentralDirectoryOnThisDisk
	 */
	public int getTotalOfEntriesInCentralDirectoryOnThisDisk() {
		return totalOfEntriesInCentralDirectoryOnThisDisk;
	}

	/**
	 * Sets total of entries in central directory on this disk.
	 *
	 * @param totalOfEntriesInCentralDirectoryOnThisDisk the totalOfEntriesInCentralDirectoryOnThisDisk to set
	 */
	public void setTotalOfEntriesInCentralDirectoryOnThisDisk(final int totalOfEntriesInCentralDirectoryOnThisDisk) {
		this.totalOfEntriesInCentralDirectoryOnThisDisk = totalOfEntriesInCentralDirectoryOnThisDisk;
	}

	/**
	 * Gets total of entries in central directory.
	 *
	 * @return the totalOfEntriesInCentralDirectory
	 */
	public int getTotalOfEntriesInCentralDirectory() {
		return totalOfEntriesInCentralDirectory;
	}

	/**
	 * Sets total of entries in central directory.
	 *
	 * @param totalOfEntriesInCentralDirectory the totalOfEntriesInCentralDirectory to set
	 */
	public void setTotalOfEntriesInCentralDirectory(final int totalOfEntriesInCentralDirectory) {
		this.totalOfEntriesInCentralDirectory = totalOfEntriesInCentralDirectory;
	}

	/**
	 * Gets size of central directory.
	 *
	 * @return the sizeOfCentralDirectory
	 */
	public int getSizeOfCentralDirectory() {
		return sizeOfCentralDirectory;
	}

	/**
	 * Sets size of central directory.
	 *
	 * @param sizeOfCentralDirectory the sizeOfCentralDirectory to set
	 */
	public void setSizeOfCentralDirectory(final int sizeOfCentralDirectory) {
		this.sizeOfCentralDirectory = sizeOfCentralDirectory;
	}

	/**
	 * Gets offset of start of central directory.
	 *
	 * @return the offsetOfStartOfCentralDirectory
	 */
	public long getOffsetOfStartOfCentralDirectory() {
		return offsetOfStartOfCentralDirectory;
	}

	/**
	 * Sets offset of start of central directory.
	 *
	 * @param offsetOfStartOfCentralDirectory the offsetOfStartOfCentralDirectory to set
	 */
	public void setOffsetOfStartOfCentralDirectory(final long offsetOfStartOfCentralDirectory) {
		this.offsetOfStartOfCentralDirectory = offsetOfStartOfCentralDirectory;
	}

	/**
	 * Gets comment length.
	 *
	 * @return the commentLength
	 */
	public int getCommentLength() {
		return commentLength;
	}

	/**
	 * Sets comment length.
	 *
	 * @param commentLength the commentLength to set
	 */
	public void setCommentLength(final int commentLength) {
		this.commentLength = commentLength;
	}

	/**
	 * Get comment bytes byte [ ].
	 *
	 * @return the commentBytes
	 */
	public byte[] getCommentBytes() {
		return commentBytes == null ? new byte[0] : commentBytes.clone();
	}

	/**
	 * Sets comment bytes.
	 *
	 * @param commentBytes the commentBytes to set
	 */
	public void setCommentBytes(final byte[] commentBytes) {
		this.commentBytes = commentBytes == null ? new byte[0] : commentBytes.clone();
	}
}
