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
import org.nervousync.commons.Globals;
import org.nervousync.compress.CompressOperator;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <h2 class="en-US">Default compress file operator implementation</h2>
 * <h2 class="zh-CN">默认压缩文件操作实现</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Jul 31, 2023 16:27:08 $
 */
public final class DefaultCompressOperatorImpl implements CompressOperator {

	@Override
	public InputStream entryInputStream(final TargetPath targetPath) throws IOException {
		try (ZipFile zipFile = new ZipFile(targetPath.getFilePath())) {
			ZipEntry zipEntry = zipFile.getEntry(targetPath.getEntryPath());
			return zipEntry == null ? null : zipFile.getInputStream(zipEntry);
		}
	}

	@Override
	public byte[] entryBytes(final TargetPath targetPath) throws IOException {
		try (InputStream inputStream = this.entryInputStream(targetPath)) {
			if (inputStream != null) {
				return inputStream.readAllBytes();
			}
		}
		return new byte[0];
	}

	@Override
	public long entryLength(final TargetPath targetPath) throws IOException {
		try (ZipFile zipFile = new ZipFile(targetPath.getFilePath())) {
			ZipEntry zipEntry = zipFile.getEntry(targetPath.getEntryPath());
			return zipEntry == null ? Globals.DEFAULT_VALUE_LONG : zipEntry.getSize();
		}
	}

	@Override
	public boolean entryExists(final TargetPath targetPath) throws IOException {
		try (ZipFile zipFile = new ZipFile(targetPath.getFilePath())) {
			return zipFile.getEntry(targetPath.getEntryPath()) != null;
		}
	}
}
