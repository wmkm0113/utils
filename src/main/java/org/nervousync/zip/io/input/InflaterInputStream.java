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
package org.nervousync.zip.io.input;

import java.io.EOFException;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.commons.io.StandardFile;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.zip.crypto.Cryptor;
import org.nervousync.zip.ZipFile;

/**
 * The type Inflater input stream.
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Dec 2, 2017 13:06:01 $
 */
public class InflaterInputStream extends PartInputStream {

	private final Inflater inflater;
	private final byte[] buffer;
	private final byte[] oneByteBuffer = new byte[1];
	private long writeBytes;
	private final long originalSize;

	/**
	 * Instantiates a new Inflater input stream.
	 *
	 * @param zipFile            the zip file
	 * @param currentIndex       the current index
	 * @param input              the seek position
	 * @param length             the length
	 * @param originalSize       the original size
	 * @param decryptor          the decryptor
	 * @param isAESEncryptedFile is aes encrypted file
	 */
	private InflaterInputStream(final ZipFile zipFile, final int currentIndex, final StandardFile input, final long length,
	                            final long originalSize, final Cryptor decryptor, final boolean isAESEncryptedFile) {
		super(zipFile, currentIndex, input, length, decryptor, isAESEncryptedFile);
		this.inflater = new Inflater(Boolean.TRUE);
		this.buffer = new byte[Globals.DEFAULT_BUFFER_SIZE];
		this.writeBytes = 0L;
		this.originalSize = originalSize;
	}

	public static InflaterInputStream newInstance(final ZipFile zipFile, final int currentIndex,
	                                              final long seekPosition, final long length, final long originalSize,
	                                              final Cryptor decryptor, final boolean isAESEncryptedFile)
			throws IOException {
		final StandardFile input = zipFile.openSplitFile(currentIndex);
		input.seek(seekPosition);
		return new InflaterInputStream(zipFile, currentIndex, input, length, originalSize, decryptor, isAESEncryptedFile);
	}

	@Override
	public int read() throws IOException {
		return this.read(this.oneByteBuffer, 0, 1) == Globals.DEFAULT_VALUE_INT ?
				Globals.DEFAULT_VALUE_INT : this.oneByteBuffer[0] & 0xFF;
	}

	@Override
	public int read(@Nonnull final byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int read(@Nonnull final byte[] b, final int off, final int len) throws IOException {
		if (off < 0 || len < 0 || off + len > b.length) {
			throw new IndexOutOfBoundsException();
		} else if (b.length == 0) {
			return 0;
		}

		try {
			if (this.writeBytes >= this.originalSize) {
				this.finishInflating();
				return Globals.DEFAULT_VALUE_INT;
			}

			int readLength;
			while ((readLength = this.inflater.inflate(b, off, len)) == 0) {
				if (this.inflater.finished() || this.inflater.needsDictionary()) {
					this.finishInflating();
					return Globals.DEFAULT_VALUE_INT;
				}

				if (this.inflater.needsInput()) {
					this.fill();
				}
			}

			this.writeBytes += readLength;
			return readLength;
		} catch (DataFormatException | ZipException e) {
			throw new IOException("Invalid data format", e);
		}
	}

	@Override
	public long skip(final long length) throws IOException {
		if (length < 0L) {
			throw new IllegalArgumentException("Negative skip length");
		}

		int limit = (int) Math.min(length, Integer.MAX_VALUE);
		int total = 0;
		byte[] b = new byte[512];
		while (total < limit) {
			int len = limit - total;
			if (len > b.length) {
				len = b.length;
			}
			len = this.read(b, 0, len);
			if (len == Globals.DEFAULT_VALUE_INT) {
				break;
			}
			total += len;
		}
		return total;
	}

	@Override
	public int available() {
		return this.inflater.finished() ? 0 : 1;
	}

	@Override
	public void close() throws IOException {
		this.inflater.end();
		super.close();
	}

	private void finishInflating() throws IOException, ZipException {
		super.seekToEnd();
		this.checkAndReadAESMacBytes();
	}

	private void fill() throws IOException {
		int length = super.read(this.buffer, 0, this.buffer.length);
		if (length == Globals.DEFAULT_VALUE_INT) {
			throw new EOFException("Unexpected end of input stream");
		}
		this.inflater.setInput(this.buffer, 0, length);
	}
}
