package org.nervousync.archive.zip.models;

import org.nervousync.archive.ArchiveEntry;
import org.nervousync.archive.exceptions.ArchiveException;
import org.nervousync.archive.zip.models.header.FileHeader;
import org.nervousync.archive.zip.models.header.GeneralFileHeader;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * <h2 class="en-US">ZIP timestamp extra field encoding</h2>
 * <h2 class="zh-CN">ZIP 时间扩展字段编码</h2>
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Oct 26, 2026 09:37:28 $
 */
public final class EntryTimes {

	/**
	 * <span class="en-US">NTFS extended time fields</span>
	 * <span class="zh-CN">NTFS 时间扩展字段</span>
	 *
	 */
	public static final int NTFS_ID = 0x000A;
	/**
	 * <span class="en-US">Extended Timestamp Field</span>
	 * <span class="zh-CN">Extended Timestamp 时间扩展字段</span>
	 *
	 */
	public static final int EXTENDED_ID = 0x5455;
	/**
	 * <span class="en-US">Number of seconds from 1601-01-01 UTC to 1970-01-01 UTC</span>
	 * <span class="zh-CN">1601-01-01 UTC 到 1970-01-01 UTC 的秒数</span>
	 *
	 */
	private static final long WINDOWS_EPOCH_SECONDS = 11644473600L;
	/**
	 * <span class="en-US">The number of 100-nanosecond count units contained in one second</span>
	 * <span class="zh-CN">每秒包含的 100 纳秒计数单位数量</span>
	 *
	 */
	private static final long TICKS_PER_SECOND = 10000000L;

	/**
	 * <span class="en-US"></span>
	 * <span class="zh-CN"></span>
	 */
	private static final BigInteger TICKS = BigInteger.valueOf(TICKS_PER_SECOND);

	/**
	 * <h3 class="en-US">Private constructor method for the ZIP timestamp extra field encoding</h3>
	 * <h3 class="zh-CN">ZIP 时间扩展字段编码的私有构造方法</h3>
	 */
	private EntryTimes() {
	}

	/**
	 * <h3 class="en-US">Determine whether it is a time extension field supported by this class.</h3>
	 * <h3 class="zh-CN">判断是否为本类支持的时间扩展字段</h3>
	 */
	public static boolean isTimestamp(final ExtraDataRecord record) {
		return record.headerId() == NTFS_ID || record.headerId() == EXTENDED_ID;
	}

	/**
	 * <h3 class="en-US">Encoding the temporal attributes of entries</h3>
	 * <h3 class="zh-CN">编码条目的时间属性</h3>
	 *
	 * @param entry <span class="en-US">Archive entry</span>
	 *              <span class="zh-CN">归档条目</span>
	 * @return <span class="en-US">List of extended fields</span>
	 * <span class="zh-CN">扩展字段列表</span>
	 * @throws IOException <span class="en-US">Time exceeds the supported encoding range</span>
	 *                     <span class="zh-CN">时间超出支持的编码范围</span>
	 */
	public static List<ExtraDataRecord> encode(final ArchiveEntry entry) throws IOException {
		Objects.requireNonNull(entry, "entry");

		final Instant modified = Objects.requireNonNull(entry.getLastModified(), "lastModified");
		final List<ExtraDataRecord> records = new ArrayList<>(2);
		final long seconds = modified.getEpochSecond();

		if (seconds >= Integer.MIN_VALUE && seconds <= Integer.MAX_VALUE) {
			final ByteBuffer extended = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
			extended.put((byte) 0x01);
			extended.putInt((int) seconds);
			records.add(new ExtraDataRecord(EXTENDED_ID, extended.array()));
		}

		final ByteBuffer ntfs = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);

		ntfs.putInt(0);
		ntfs.putShort((short) 0x0001);
		ntfs.putShort((short) 24);

		ntfs.putLong(toWindowsTime(modified));
		ntfs.putLong(toWindowsTime(entry.getLastAccessTime()));
		ntfs.putLong(toWindowsTime(entry.getCreationTime()));

		records.add(new ExtraDataRecord(NTFS_ID, ntfs.array()));

		return records;
	}

	/**
	 * <h3 class="en-US">Read time extension field</h3>
	 * <h3 class="zh-CN">读取时间扩展字段</h3>
	 *
	 * @param header <span class="en-US">File header information</span>
	 *               <span class="zh-CN">文件头信息</span>
	 * @param strict <span class="en-US">Strict flag</span>
	 *               <span class="zh-CN">严格标记</span>
	 */
	public static Instant[] read(final FileHeader header, final boolean strict) throws IOException {
		Objects.requireNonNull(header, "header");

		Instant[] ntfs = new Instant[3];
		Instant[] extended = new Instant[3];

		boolean seenNtfs = false;
		boolean seenExtended = false;

		if (header.getExtraDataRecords() == null) {
			return ntfs;
		}

		for (final ExtraDataRecord record : header.getExtraDataRecords()) {
			if (!isTimestamp(record)) {
				continue;
			}

			try {
				final byte[] data = record.dataBytes();
				if (data == null) {
					throw new IOException("Missing ZIP timestamp payload");
				}

				final ByteBuffer bytes = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

				if (record.headerId() == NTFS_ID) {
					if (seenNtfs) {
						throw new IOException("Duplicate NTFS timestamp field");
					}
					seenNtfs = true;

					if (bytes.remaining() < 4) {
						throw new IOException("Truncated NTFS timestamp field");
					}

					// Reserved。
					bytes.getInt();

					boolean seenTimes = false;
					final Instant[] parsed = new Instant[3];

					while (bytes.hasRemaining()) {
						if (bytes.remaining() < 4) {
							throw new IOException("Truncated NTFS attribute header");
						}

						final int tag = Short.toUnsignedInt(bytes.getShort());
						final int size = Short.toUnsignedInt(bytes.getShort());
						if (size > bytes.remaining()) {
							throw new IOException("Truncated NTFS attribute payload");
						}
						final int end = bytes.position() + size;
						if (tag == 0x0001) {
							if (seenTimes || size != 24) {
								throw new IOException("Invalid NTFS timestamp attribute");
							}
							seenTimes = true;

							for (int index = 0; index < 3; index++) {
								parsed[index] = fromWindowsTime(bytes.getLong());
							}
						}
						bytes.position(end);
					}
					ntfs = parsed;
				} else {
					if (seenExtended) {
						throw new IOException("Duplicate extended timestamp field");
					}
					seenExtended = true;
					if (!bytes.hasRemaining()) {
						throw new IOException("Truncated extended timestamp flags");
					}
					final int flags = Byte.toUnsignedInt(bytes.get());
					if ((flags & ~0x07) != 0) {
						throw new IOException("Unknown extended timestamp flags");
					}
					final Instant[] parsed = new Instant[3];
					for (int index = 0; index < 3; index++) {
						if ((flags & (1 << index)) == 0) {
							continue;
						}
						if (index > 0 && !bytes.hasRemaining() && header instanceof GeneralFileHeader) {
							break;
						}
						if (bytes.remaining() < 4) {
							throw new IOException("Truncated extended timestamp value");
						}
						parsed[index] = Instant.ofEpochSecond(bytes.getInt());
					}
					if (bytes.hasRemaining()) {
						throw new IOException("Unexpected extended timestamp data");
					}
					extended = parsed;
				}
			} catch (IOException | RuntimeException e) {
				if (strict) {
					throw new ArchiveException(0x0000001A002DL, e);
				}
			}
		}

		for (int index = 0; index < 3; index++) {
			if (ntfs[index] == null) {
				ntfs[index] = extended[index];
			}
		}

		return ntfs;
	}

	/**
	 * <h3 class="en-US">Convert an Instant to the bit pattern of an unsigned 64-bit FILETIME.</h3>
	 * <h3 class="zh-CN">将 Instant 转为无符号 64 位 FILETIME 的位模式。</h3>
	 *
	 * @param instant <span class="en-US">Instant</span>
	 *                <span class="zh-CN">时间戳</span>
	 * @return <span class="en-US">unsigned 64-bit FILETIME</span>
	 * <span class="zh-CN">无符号 64 位 FILETIME</span>
	 * @throws IOException <span class="en-US">Time exceeds the supported encoding range</span>
	 *                     <span class="zh-CN">时间超出支持的编码范围</span>
	 */
	private static long toWindowsTime(final Instant instant) throws IOException {
		if (instant == null) {
			return 0L;
		}

		final BigInteger value =
				BigInteger.valueOf(instant.getEpochSecond())
						.add(BigInteger.valueOf(WINDOWS_EPOCH_SECONDS))
						.multiply(TICKS)
						.add(BigInteger.valueOf(instant.getNano() / 100));

		if (value.signum() <= 0 || value.bitLength() > 64) {
			throw new IOException("Timestamp is outside the nonzero unsigned FILETIME range: " + instant);
		}
		return value.longValue();
	}

	/**
	 * <h3 class="en-US">Generate a DOS date-time encoding based on the configured time zone.</h3>
	 * <h3 class="zh-CN">按配置时区生成 DOS 日期时间编码。</h3>
	 *
	 * @param instant  <span class="en-US">Instant</span>
	 *                 <span class="zh-CN">时间戳</span>
	 * @param timeZone <span class="en-US">Timezone</span>
	 *                 <span class="zh-CN">时区</span>
	 */
	public static int dosTime(final Instant instant, final TimeZone timeZone) {
		Objects.requireNonNull(instant, "instant");

		final ZoneId zone = timeZone == null ? ZoneId.systemDefault() : timeZone.toZoneId();

		LocalDateTime value = LocalDateTime.ofInstant(instant, zone);
		if (value.getYear() < 1980) {
			value = LocalDateTime.of(1980, 1, 1, 0, 0);
		} else if (value.getYear() > 2107) {
			value = LocalDateTime.of(2107, 12, 31, 23, 59, 58);
		}
		return ((value.getYear() - 1980) << 25)
				| (value.getMonthValue() << 21)
				| (value.getDayOfMonth() << 16)
				| (value.getHour() << 11)
				| (value.getMinute() << 5)
				| (value.getSecond() / 2);
	}

	/**
	 * <h3 class="en-US">Convert an unsigned 64-bit FILETIME to an Instant.</h3>
	 * <h3 class="zh-CN">将无符号 64 位 FILETIME 转为 Instant。</h3>
	 *
	 * @param value <span class="en-US">unsigned 64-bit FILETIME</span>
	 *              <span class="zh-CN">无符号 64 位 FILETIME</span>
	 * @return <span class="en-US">Instant</span>
	 * <span class="zh-CN">时间戳</span>
	 */
	private static Instant fromWindowsTime(final long value) {
		if (value == 0L) {
			return null;
		}

		final long seconds =
				Long.divideUnsigned(value, TICKS_PER_SECOND)
						- WINDOWS_EPOCH_SECONDS;

		final long nanos =
				Long.remainderUnsigned(value, TICKS_PER_SECOND) * 100L;

		return Instant.ofEpochSecond(seconds, nanos);
	}
}