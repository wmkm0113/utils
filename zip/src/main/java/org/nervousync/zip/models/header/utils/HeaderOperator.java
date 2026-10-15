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
package org.nervousync.zip.models.header.utils;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.commons.zip.ZipGlobals;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.zip.models.header.LocalFileHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * The type Header operator.
 *
 * @author Steven Wee   <a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Dec 2, 2017 18:07:27 $
 */
public final class HeaderOperator {

	/**
	 * Retrieve salt length int.
	 *
	 * @param aesStrength the aes strength
	 * @return the int
	 * @throws ZipException the zip exception
	 */
	public static int saltLength(final int aesStrength) throws ZipException {
		return switch (aesStrength) {
			case ZipGlobals.AES_STRENGTH_128 -> 8;
			case ZipGlobals.AES_STRENGTH_192 -> 12;
			case ZipGlobals.AES_STRENGTH_256 -> 16;
			default -> throw new ZipException(0x0000001B0005L);
		};
	}

	/**
	 * Write extended local header int.
	 *
	 * @param localFileHeader the local file header
	 * @param outputStream    the output stream
	 * @return the int
	 * @throws ZipException the zip exception
	 * @throws IOException  the io exception
	 */
	public static int writeExtendedLocalHeader(final LocalFileHeader localFileHeader, final OutputStream outputStream)
			throws ZipException, DataInvalidException, IOException {
		if (localFileHeader == null || outputStream == null) {
			throw new ZipException(0x0000001B002EL);
		}

		ByteArrayOutputStream headerBytes =
				new ByteArrayOutputStream(localFileHeader.isWriteCompressSizeInZip64ExtraRecord() ? 24 : 16);
		writeInt((int) ZipGlobals.EXTSIG, headerBytes);
		writeInt((int) localFileHeader.getCrc32(), headerBytes);

		if (localFileHeader.isWriteCompressSizeInZip64ExtraRecord()) {
			// Compressed size
			writeLong(localFileHeader.getCompressedSize(), headerBytes);
			// Original size
			writeLong(localFileHeader.getOriginalSize(), headerBytes);
		} else {
			if (localFileHeader.getCompressedSize() >= ZipGlobals.ZIP_64_LIMIT
					|| localFileHeader.getOriginalSize() >= ZipGlobals.ZIP_64_LIMIT) {
				throw new ZipException(0x0000001B0048L);
			}

			// Compressed size
			writeInt((int) localFileHeader.getCompressedSize(), headerBytes);
			// Original size
			writeInt((int) localFileHeader.getOriginalSize(), headerBytes);
		}

		int expectSize = localFileHeader.isWriteCompressSizeInZip64ExtraRecord() ? 24 : 16;
		if (headerBytes.size() != expectSize) {
			throw new ZipException(0x0000001B0048L);
		}

		headerBytes.writeTo(outputStream);
		return headerBytes.size();
	}

	public static void writeShort(final short value, @Nonnull final ByteArrayOutputStream outputStream)
			throws ZipException {
		outputStream.write(value & 0xFF);
		outputStream.write((value >>> 8) & 0xFF);
	}

	public static void writeInt(final int value, @Nonnull final ByteArrayOutputStream outputStream)
			throws ZipException {
		outputStream.write(value & 0xFF);
		outputStream.write((value >>> 8) & 0xFF);
		outputStream.write((value >>> 16) & 0xFF);
		outputStream.write((value >>> 24) & 0xFF);
	}

	public static void writeLong(final long value, @Nonnull final ByteArrayOutputStream outputStream)
			throws ZipException {
		outputStream.write((int) value & 0xFF);
		outputStream.write((int) (value >>> 8) & 0xFF);
		outputStream.write((int) (value >>> 16) & 0xFF);
		outputStream.write((int) (value >>> 24) & 0xFF);
		outputStream.write((int) (value >>> 32) & 0xFF);
		outputStream.write((int) (value >>> 40) & 0xFF);
		outputStream.write((int) (value >>> 48) & 0xFF);
		outputStream.write((int) (value >>> 56) & 0xFF);
	}

	public static void writeBytes(@Nonnull final byte[] dataBytes, @Nonnull final ByteArrayOutputStream outputStream)
			throws ZipException {
		outputStream.write(dataBytes, 0, dataBytes.length);
	}

	public static void writeEntryPath(final String entryPath, @Nonnull final ByteArrayOutputStream outputStream)
			throws ZipException {
		writeBytes(convertCharset(entryPath), outputStream);
	}

	/**
	 * Convert charset byte [ ].
	 *
	 * @param string the string
	 * @return the byte [ ]
	 * @throws ZipException the zip exception
	 */
	private static byte[] convertCharset(final String string) throws ZipException {
		try {
			String charSet = StringUtils.detectCharset(string);
			return switch (charSet) {
				case Globals.CHARSET_CP850 -> string.getBytes(Globals.CHARSET_CP850);
				case Globals.CHARSET_GBK -> string.getBytes(Globals.CHARSET_GBK);
				case Globals.DEFAULT_ENCODING -> string.getBytes(Globals.DEFAULT_ENCODING);
				default -> string.getBytes(Globals.DEFAULT_SYSTEM_CHARSET);
			};
		} catch (UnsupportedEncodingException err) {
			return string.getBytes(Charset.defaultCharset());
		} catch (Exception e) {
			throw new ZipException(0x000000FF0002L, e);
		}
	}
}
