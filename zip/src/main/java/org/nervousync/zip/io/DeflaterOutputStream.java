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
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.zip.ZipFile;
import org.nervousync.zip.options.ZipOptions;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * Deflater output stream
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Dec 1, 2017 12:19:07 $
 */
public class DeflaterOutputStream extends CipherOutputStream {

	private final Deflater deflater;
	private final byte[] buffer = new byte[ZipGlobals.IO_BUFFER_SIZE];

	DeflaterOutputStream(final OutputStream outputStream, final ZipFile zipFile) {
		super(outputStream, zipFile);
		this.deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, Boolean.TRUE);
	}

	@Override
	public void write(@Nonnull final byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}

	@Override
	public void write(@Nonnull final byte[] b, final int off, final int len) throws IOException {
		if (this.zipOptions.getCompressionMethod() != ZipGlobals.COMP_DEFLATE) {
			super.write(b, off, len);
		} else {
			this.deflater.setInput(b, off, len);
			while (!this.deflater.needsInput()) {
				this.deflate();
			}
		}
	}

	@Override
	public void putNextEntry(final File file, final ZipOptions zipOptions) throws ZipException {
		super.putNextEntry(file, zipOptions);

		if (zipOptions.getCompressionMethod() == ZipGlobals.COMP_DEFLATE) {
			this.deflater.reset();
			if ((zipOptions.getCompressionLevel() < 0 || zipOptions.getCompressionLevel() > 9)
					&& zipOptions.getCompressionLevel() != Globals.DEFAULT_VALUE_INT) {
				throw new ZipException(0x0000001B004BL);
			}
			this.deflater.setLevel(zipOptions.getCompressionLevel());
		}
	}

	@Override
	public void closeEntry() throws IOException, ZipException {
		if (this.zipOptions.getCompressionMethod() == ZipGlobals.COMP_DEFLATE) {
			if (!this.deflater.finished()) {
				this.deflater.finish();
				while (!this.deflater.finished()) {
					this.deflate();
				}
			}
		}
		super.closeEntry();
	}

	private void deflate() throws IOException {
		int length = this.deflater.deflate(this.buffer, 0, this.buffer.length);
		if (length > 0) {
			super.write(this.buffer, 0, length);
		}
	}
}
