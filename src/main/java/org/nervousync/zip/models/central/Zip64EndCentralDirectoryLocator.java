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
 * The type Zip64 end central directory locator.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 28, 2017 16:32:41 $
 */
public final class Zip64EndCentralDirectoryLocator {

	private long signature;
	private int indexOfZip64EndOfCentralDirectoryRecord;
	private long offsetZip64EndOfCentralDirectoryRecord;
	private int totalNumberOfDiscs;

	/**
	 * Instantiates a new Zip64 end central directory locator.
	 */
	public Zip64EndCentralDirectoryLocator() {
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
	 * Gets index of zip 64 end of central directory record.
	 *
	 * @return the indexOfZip64EndOfCentralDirectoryRecord
	 */
	public int getIndexOfZip64EndOfCentralDirectoryRecord() {
		return indexOfZip64EndOfCentralDirectoryRecord;
	}

	/**
	 * Sets index of zip 64 end of central directory record.
	 *
	 * @param indexOfZip64EndOfCentralDirectoryRecord the indexOfZip64EndOfCentralDirectoryRecord to set
	 */
	public void setIndexOfZip64EndOfCentralDirectoryRecord(final int indexOfZip64EndOfCentralDirectoryRecord) {
		this.indexOfZip64EndOfCentralDirectoryRecord = indexOfZip64EndOfCentralDirectoryRecord;
	}

	/**
	 * Gets offset zip 64 end of central directory record.
	 *
	 * @return the offsetZip64EndOfCentralDirectoryRecord
	 */
	public long getOffsetZip64EndOfCentralDirectoryRecord() {
		return offsetZip64EndOfCentralDirectoryRecord;
	}

	/**
	 * Sets offset zip 64 end of central directory record.
	 *
	 * @param offsetZip64EndOfCentralDirectoryRecord the offsetZip64EndOfCentralDirectoryRecord to set
	 */
	public void setOffsetZip64EndOfCentralDirectoryRecord(final long offsetZip64EndOfCentralDirectoryRecord) {
		this.offsetZip64EndOfCentralDirectoryRecord = offsetZip64EndOfCentralDirectoryRecord;
	}

	/**
	 * Gets total number of discs.
	 *
	 * @return the totalNumberOfDiscs
	 */
	public int getTotalNumberOfDiscs() {
		return totalNumberOfDiscs;
	}

	/**
	 * Sets total number of discs.
	 *
	 * @param totalNumberOfDiscs the totalNumberOfDiscs to set
	 */
	public void setTotalNumberOfDiscs(final int totalNumberOfDiscs) {
		this.totalNumberOfDiscs = totalNumberOfDiscs;
	}
}
