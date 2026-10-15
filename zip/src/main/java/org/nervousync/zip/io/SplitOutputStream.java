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
package org.nervousync.zip.io;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.commons.zip.ZipGlobals;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.io.StandardFile;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.core.RawUtils;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.net.EnhanceFileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * The type Split output stream.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 @date: Nov 29, 2017 14:57:01 $
 */
public class SplitOutputStream extends OutputStream {

	private static final long[] HEADER_SIGNATURES = new long[11];

	static {
		HEADER_SIGNATURES[0] = ZipGlobals.LOCSIG;
		HEADER_SIGNATURES[1] = ZipGlobals.EXTSIG;
		HEADER_SIGNATURES[2] = ZipGlobals.CENSIG;
		HEADER_SIGNATURES[3] = ZipGlobals.ENDSIG;
		HEADER_SIGNATURES[4] = ZipGlobals.DIGSIG;
		HEADER_SIGNATURES[5] = ZipGlobals.ARCEXTDATREC;
		HEADER_SIGNATURES[6] = ZipGlobals.EXTSIG;
		HEADER_SIGNATURES[7] = ZipGlobals.ZIP64ENDCENDIRLOC;
		HEADER_SIGNATURES[8] = ZipGlobals.ZIP64ENDCENDIRREC;
		HEADER_SIGNATURES[9] = ZipGlobals.EXTRAFIELDZIP64LENGTH;
		HEADER_SIGNATURES[10] = ZipGlobals.AESSIG;
	}

	/**
	 * One byte buffer
	 */
	private final byte[] oneByteBuffer = new byte[1];

	private StandardFile dataOutput;
	private final String fileName;
	private final String filePath;
	private final String currentFullPath;
	private final long splitLength;
	private int currentSplitFileIndex;
	private long bytesWrittenForThisPart;

	/**
	 * Instantiates a new Split output stream.
	 *
	 * @param savePath    the save path
	 * @param splitLength the split length
	 * @throws FileNotFoundException the file was not found exception
	 */
	private SplitOutputStream(final String savePath, final long splitLength) throws FileNotFoundException {
		int beginIndex;
		if (savePath.startsWith(EnhanceFileUtils.SAMBA_PROTOCOL)) {
			beginIndex = savePath.lastIndexOf(Globals.DEFAULT_URL_SEPARATOR);
		} else {
			beginIndex = savePath.lastIndexOf(Globals.DEFAULT_PAGE_SEPARATOR);
		}
		this.filePath = savePath.substring(0, beginIndex);
		this.fileName = StringUtils.stripFilenameExtension(savePath.substring(beginIndex + 1));
		this.dataOutput = new StandardFile(savePath, Boolean.TRUE);
		this.currentFullPath = savePath;
		this.splitLength = splitLength;
		this.currentSplitFileIndex = 0;
		this.bytesWrittenForThisPart = 0L;
	}

	public static SplitOutputStream newInstance(final String savePath) throws FileNotFoundException, ZipException {
		return new SplitOutputStream(savePath, Globals.DEFAULT_VALUE_LONG);
	}

	public static SplitOutputStream newInstance(final String savePath, final long splitLength)
			throws FileNotFoundException, ZipException {
		if (splitLength >= 0 && splitLength < ZipGlobals.MIN_SPLIT_LENGTH) {
			throw new ZipException(0x0000001B0057L, ZipGlobals.MIN_SPLIT_LENGTH);
		}
		return new SplitOutputStream(savePath, splitLength);
	}

	@Override
	public void write(final int b) throws IOException {
		this.oneByteBuffer[0] =  (byte) b;
		this.write(this.oneByteBuffer, 0, 1);
	}

	@Override
	public void write(@Nonnull final byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}

	@Override
	public void close() throws IOException {
		this.dataOutput.close();
		super.close();
	}

	@Override
	public void write(@Nonnull final byte[] b, final int off, final int len) throws IOException {
		Objects.checkFromIndexSize(off, len, b.length);
		if (len < 0) {
			return;
		}

		if (this.splitLength == Globals.DEFAULT_VALUE_LONG) {
			this.dataOutput.write(b, off, len);
			this.bytesWrittenForThisPart += len;
			return;
		}

		int offset = off, remaining = len;
		boolean headerData = this.isHeaderData(b, off, len);

		while (remaining > 0) {
			long available = this.splitLength - this.bytesWrittenForThisPart;
			if (available == 0L) {
				this.startNextSplitFile();
				available = this.splitLength;
			}

			if (headerData && remaining > available) {
				if (remaining > this.splitLength) {
					throw new IOException("ZIP header exceeds configured split length");
				}
				this.startNextSplitFile();
				continue;
			}

			int writeLength = (int) Math.min(remaining, available);
			this.dataOutput.write(b, offset, writeLength);
			this.bytesWrittenForThisPart += writeLength;
			offset += writeLength;
			remaining -= writeLength;
			headerData = Boolean.FALSE;
		}
	}

	/**
	 * Check buffer size and start next split file boolean.
	 *
	 * @param bufferSize the buffer size
	 * @throws ZipException the zip exception
	 */
	public void checkBufferSizeAndStartNextSplitFile(final int bufferSize) throws ZipException {
		if (bufferSize < 0) {
			throw new ZipException(0x0000001B0056L);
		}

		if (!this.isBufferSizeFitForCurrentSplitFile(bufferSize)) {
			try {
				this.startNextSplitFile();
				this.bytesWrittenForThisPart = 0;
			} catch (IOException e) {
				throw new ZipException(0x0000001B0057L, e);
			}
		}
	}

	private boolean isBufferSizeFitForCurrentSplitFile(final int bufferSize) throws ZipException {
		if (bufferSize < 0) {
			throw new ZipException(0x0000001B0056L);
		}

		if (this.splitLength >= ZipGlobals.MIN_SPLIT_LENGTH) {
			return (this.bytesWrittenForThisPart + bufferSize <= this.splitLength);
		} else {
			return Boolean.TRUE;
		}
	}

	/**
	 * Seek.
	 *
	 * @param pos the pos
	 * @throws IOException the io exception
	 */
	public void seek(final long pos) throws IOException {
		this.dataOutput.seek(pos);
	}

	/**
	 * Gets the file pointer.
	 *
	 * @return the file pointer
	 * @throws IOException the io exception
	 */
	public long getFilePointer() throws IOException {
		return this.dataOutput.getFilePointer();
	}

	@Override
	public void flush() {
	}

	/**
	 * Is split zip file boolean.
	 *
	 * @return the boolean
	 */
	public boolean isSplitZipFile() {
		return this.splitLength != Globals.DEFAULT_VALUE_LONG;
	}

	/**
	 * Gets split length.
	 *
	 * @return the splitLength
	 */
	public long getSplitLength() {
		return splitLength;
	}

	/**
	 * Gets current split file index.
	 *
	 * @return the currentSplitFileIndex
	 */
	public int getCurrentSplitFileIndex() {
		return currentSplitFileIndex;
	}

	private void startNextSplitFile() throws IOException {
		String splitFile = splitPath();

		this.dataOutput.close();

		if (FileUtils.isExists(splitFile)) {
			throw new IOException("split file: " + splitFile
					+ " already exists in the current directory, cannot rename this file");
		}

		if (!FileUtils.moveFile(this.currentFullPath, splitFile)) {
			throw new IOException("Cannot create split file!");
		}

		this.dataOutput = new StandardFile(this.currentFullPath, Boolean.TRUE);
		this.currentSplitFileIndex++;
		this.bytesWrittenForThisPart = 0L;
	}

	private String splitPath() {
		String folderPath;

		if (this.filePath.startsWith(EnhanceFileUtils.SAMBA_PROTOCOL)) {
			folderPath = this.filePath + Globals.DEFAULT_URL_SEPARATOR;
		} else {
			folderPath = this.filePath + Globals.DEFAULT_PAGE_SEPARATOR;
		}

		if (this.currentSplitFileIndex < 9) {
			return folderPath + fileName + ".zip.0" + (this.currentSplitFileIndex + 1);
		} else {
			return folderPath + fileName + ".zip." + (this.currentSplitFileIndex + 1);
		}
	}

	private boolean isHeaderData(final byte[] buffer, final int offset, final int length) throws IOException {
		if (buffer == null || length < Integer.BYTES) {
			return Boolean.FALSE;
		}
		try {
			long signature = RawUtils.readUnsignedInt(buffer, offset, ByteOrder.LITTLE_ENDIAN);

			for (long headerSignature : HEADER_SIGNATURES) {
				if (headerSignature != ZipGlobals.EXTSIG && headerSignature == signature) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		} catch (DataInvalidException e) {
			throw new IOException(e);
		}
	}
}
