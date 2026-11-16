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

package org.nervousync.archive.zip.models;

import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.options.ExtractOptions;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.models.header.FileHeader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * <h2 class="en-US">Entry metadata</h2>
 * <h2 class="zh-CN">条目元数据</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class EntryMetaData {

	/**
	 * <span class="en-US">DOS read only attribute mask</span>
	 * <span class="zh-CN">DOS 只读属性掩码</span>
	 */
	private static final int DOS_READ_ONLY = 0x01;
	/**
	 * <span class="en-US">DOS hidden attribute mask</span>
	 * <span class="zh-CN">DOS 隐藏属性掩码</span>
	 */
	private static final int DOS_HIDDEN = 0x02;
	/**
	 * <span class="en-US">DOS system attribute mask</span>
	 * <span class="zh-CN">DOS 系统属性掩码</span>
	 */
	private static final int DOS_SYSTEM = 0x04;
	/**
	 * <span class="en-US">DOS archive attribute mask</span>
	 * <span class="zh-CN">DOS 归档属性掩码</span>
	 */
	private static final int DOS_ARCHIVE = 0x20;

	/**
	 * <h3 class="en-US">Private constructor method for the entry metadata</h3>
	 * <h3 class="zh-CN">条目元数据的私有构造方法</h3>
	 */
	private EntryMetaData() {
	}

	/**
	 * <h3 class="en-US">Apply the entry metadata</h3>
	 * <h3 class="zh-CN">设置条目的元数据</h3>
	 *
	 * @param target         <span class="en-US">Target file</span>
	 *                       <span class="zh-CN">目标文件</span>
	 * @param fileHeader     <span class="en-US">File header</span>
	 *                       <span class="zh-CN">文件头</span>
	 * @param extractOptions <span class="en-US">Extract options</span>
	 *                       <span class="zh-CN">解压选项</span>
	 * @param archiveOptions <span class="en-US">Archive options</span>
	 *                       <span class="zh-CN">归档选项</span>
	 * @param zipOptions     <span class="en-US">Zip options</span>
	 *                       <span class="zh-CN">Zip 选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when open input stream</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public static void applyBeforePublish(final Path target, final FileHeader fileHeader,
	                                      final ExtractOptions extractOptions,
	                                      final ArchiveOptions archiveOptions, final ZipOptions zipOptions) throws IOException {
		if (Files.isSymbolicLink(target)) {
			return;
		}

		if (extractOptions.isPreserveTimestamp()) {
			final Instant[] times = EntryTimes.read(fileHeader, archiveOptions.isStrict());

			final FileTime modified =
					FileTime.from(fileHeader.modifiedInstant(zipOptions.getTimeZone(), archiveOptions.isStrict()));
			final FileTime accessed = (times[1] != null) ? FileTime.from(times[1]) : null;
			final FileTime created = (times[2] != null) ? FileTime.from(times[2]) : null;

			final BasicFileAttributeView attributeView =
					Files.getFileAttributeView(target, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

			if (attributeView != null) {
				attributeView.setTimes(modified, accessed, created);
			}
		}

		if (!extractOptions.isPreservePermissions()) {
			return;
		}

		if (fileHeader.isUnix()) {
			final PosixFileAttributeView attributeView =
					Files.getFileAttributeView(target, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

			if (attributeView != null) {
				final int mode = fileHeader.unixMode();
				final Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);

				addPermission(permissions, mode, 0x100, PosixFilePermission.OWNER_READ);
				addPermission(permissions, mode, 0x080, PosixFilePermission.OWNER_WRITE);
				addPermission(permissions, mode, 0x040, PosixFilePermission.OWNER_EXECUTE);

				addPermission(permissions, mode, 0x020, PosixFilePermission.GROUP_READ);
				addPermission(permissions, mode, 0x010, PosixFilePermission.GROUP_WRITE);
				addPermission(permissions, mode, 0x008, PosixFilePermission.GROUP_EXECUTE);

				addPermission(permissions, mode, 0x004, PosixFilePermission.OTHERS_READ);
				addPermission(permissions, mode, 0x002, PosixFilePermission.OTHERS_WRITE);
				addPermission(permissions, mode, 0x001, PosixFilePermission.OTHERS_EXECUTE);

				attributeView.setPermissions(permissions);
			}
		}
	}

	/**
	 * <h3 class="en-US">Apply the entry metadata</h3>
	 * <h3 class="zh-CN">设置条目的元数据</h3>
	 *
	 * @param target         <span class="en-US">Target file</span>
	 *                       <span class="zh-CN">目标文件</span>
	 * @param fileHeader     <span class="en-US">File header</span>
	 *                       <span class="zh-CN">文件头</span>
	 * @param extractOptions <span class="en-US">Extract options</span>
	 *                       <span class="zh-CN">解压选项</span>
	 * @throws IOException <span class="en-US">If an error occurs when open input stream</span>
	 *                     <span class="zh-CN">执行中出错</span>
	 */
	public static void applyDosAttributes(final Path target, final FileHeader fileHeader,
	                                      final ExtractOptions extractOptions) throws IOException {
		if (!extractOptions.isPreservePermissions() || Files.isSymbolicLink(target)) {
			return;
		}
		DosFileAttributeView attributeView =
				Files.getFileAttributeView(target, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

		if (attributeView != null) {
			int attributes = fileHeader.dosAttributes();

			attributeView.setHidden((attributes & DOS_HIDDEN) != 0);
			attributeView.setSystem((attributes & DOS_SYSTEM) != 0);
			attributeView.setArchive((attributes & DOS_ARCHIVE) != 0);

			attributeView.setReadOnly((attributes & DOS_READ_ONLY) != 0);
		}
	}

	private static void addPermission(final Set<PosixFilePermission> permissions, final int mode,
	                                  final int mask, final PosixFilePermission permission) {
		if ((mode & mask) != 0) {
			permissions.add(permission);
		}
	}
}
