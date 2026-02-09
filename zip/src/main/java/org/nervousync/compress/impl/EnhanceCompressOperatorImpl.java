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

package org.nervousync.compress.impl;

import org.nervousync.beans.files.TargetPath;
import org.nervousync.compress.CompressOperator;
import org.nervousync.exceptions.zip.ZipException;
import org.nervousync.zip.ZipFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * <h2 class="en-US">Enhance compress file operator implementation</h2>
 * <h2 class="zh-CN">增强的压缩文件操作实现</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 31, 2023 16:27:08 $
 */
public final class EnhanceCompressOperatorImpl implements CompressOperator {
	@Override
	public InputStream entryInputStream(final TargetPath targetPath) throws IOException {
		try {
			return ZipFile.openZipFile(targetPath.getFilePath()).entryInputStream(targetPath.getEntryPath());
		} catch (ZipException e) {
			throw new IOException(e);
		}
	}

	@Override
	public byte[] entryBytes(final TargetPath targetPath) throws IOException {
		try {
			return ZipFile.openZipFile(targetPath.getFilePath()).readEntry(targetPath.getEntryPath());
		} catch (ZipException e) {
			throw new IOException(e);
		}
	}

	@Override
	public long entryLength(final TargetPath targetPath) throws IOException {
		try {
			return ZipFile.openZipFile(targetPath.getFilePath()).readEntryLength(targetPath.getEntryPath());
		} catch (ZipException e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean entryExists(final TargetPath targetPath) throws IOException {
		try {
			return ZipFile.openZipFile(targetPath.getFilePath()).isEntryExists(targetPath.getEntryPath());
		} catch (ZipException e) {
			throw new IOException(e);
		}
	}
}
