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

/**
 * The type General file header.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 28, 2017 16:44:25 $
 */
public final class GeneralFileHeader extends FileHeader {

	private int madeVersion;
	private int fileCommentLength;
	private int diskNumberStart;
	private byte[] internalFileAttr;
	private byte[] externalFileAttr;
	private long offsetLocalHeader;
	private String fileComment;
	private boolean isDirectory;

	/**
	 * Instantiates a new General file header.
	 */
	public GeneralFileHeader() {
	}

	/**
	 * Gets made versions.
	 *
	 * @return the madeVersion
	 */
	public int getMadeVersion() {
		return madeVersion;
	}

	/**
	 * Sets made version.
	 *
	 * @param madeVersion the madeVersion to set
	 */
	public void setMadeVersion(final int madeVersion) {
		this.madeVersion = madeVersion;
	}

	/**
	 * Gets file comment length.
	 *
	 * @return the fileCommentLength
	 */
	public int getFileCommentLength() {
		return fileCommentLength;
	}

	/**
	 * Sets file comment length.
	 *
	 * @param fileCommentLength the fileCommentLength to set
	 */
	public void setFileCommentLength(final int fileCommentLength) {
		this.fileCommentLength = fileCommentLength;
	}

	/**
	 * Gets disk number start.
	 *
	 * @return the diskNumberStart
	 */
	public int getDiskNumberStart() {
		return diskNumberStart;
	}

	/**
	 * Sets disk number start.
	 *
	 * @param diskNumberStart the diskNumberStart to set
	 */
	public void setDiskNumberStart(final int diskNumberStart) {
		this.diskNumberStart = diskNumberStart;
	}

	/**
	 * Get internal file attr byte [ ].
	 *
	 * @return the internalFileAttr
	 */
	public byte[] getInternalFileAttr() {
		return internalFileAttr == null ? new byte[0] : internalFileAttr.clone();
	}

	/**
	 * Sets internal file attr.
	 *
	 * @param internalFileAttr the internalFileAttr to set
	 */
	public void setInternalFileAttr(final byte[] internalFileAttr) {
		this.internalFileAttr = internalFileAttr == null ? new byte[0] : internalFileAttr.clone();
	}

	/**
	 * Get external file attr byte [ ].
	 *
	 * @return the externalFileAttr
	 */
	public byte[] getExternalFileAttr() {
		return externalFileAttr;
	}

	/**
	 * Sets external file attr.
	 *
	 * @param externalFileAttr the externalFileAttr to set
	 */
	public void setExternalFileAttr(final byte[] externalFileAttr) {
		this.externalFileAttr = externalFileAttr;
	}

	/**
	 * Gets offset local header.
	 *
	 * @return the offsetLocalHeader
	 */
	public long getOffsetLocalHeader() {
		return offsetLocalHeader;
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
	 * Gets file comment.
	 *
	 * @return the fileComment
	 */
	public String getFileComment() {
		return fileComment;
	}

	/**
	 * Sets file comment.
	 *
	 * @param fileComment the fileComment to set
	 */
	public void setFileComment(final String fileComment) {
		this.fileComment = fileComment;
	}

	/**
	 * Is directory boolean.
	 *
	 * @return the isDirectory
	 */
	public boolean isDirectory() {
		return isDirectory;
	}

	/**
	 * Sets directory.
	 *
	 * @param isDirectory the isDirectory to set
	 */
	public void setDirectory(final boolean isDirectory) {
		this.isDirectory = isDirectory;
	}
}
