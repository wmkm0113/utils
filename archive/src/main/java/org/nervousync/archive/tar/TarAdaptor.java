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

package org.nervousync.archive.tar;

import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.adaptor.ArchiveAdaptor;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.enumerations.OpenMode;
import org.nervousync.archive.options.ExtractOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * <h2 class="en-US">TAR adaptor</h2>
 * <h2 class="zh-CN">TAR 适配器</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class TarAdaptor extends ArchiveAdaptor {

	public TarAdaptor(final String filePath, final OpenMode openMode) {
		super(filePath, openMode);
	}

	@Override
	public Optional<ResourceReader> openReader(final ArchiveEntry archiveEntry) throws IOException {
		return Optional.empty();
	}

	@Override
	public Optional<ResourceWriter> openWriter(final String entryPath, final String comment) throws IOException {
		return Optional.empty();
	}

	@Override
	public void extract(final Path path, final ExtractOptions options) throws IOException {

	}

	@Override
	public void remove(final ArchiveEntry archiveEntry) throws IOException {

	}

	@Override
	public void commit() throws IOException {

	}

	@Override
	public void rollback() throws IOException {

	}

	@Override
	public void close() throws Exception {

	}
}
