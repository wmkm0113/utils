package org.nervousync.test.generator;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.commons.id.CUID;
import org.nervousync.commons.id.SparkID;
import org.nervousync.commons.id.ULID;
import org.nervousync.enumerations.generator.UUIDIdentifier;
import org.nervousync.enumerations.generator.UUIDLocalDomain;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.DateTimeUtils;
import org.nervousync.utils.id.IDUtils;

import java.util.Optional;

public final class GeneratorTest extends BaseTest {

	public GeneratorTest() {
	}

	@Test
	@Order(0)
	public void nano() {
		this.logger.info("Nano_Random", IDUtils.nano());
		IDUtils.nanoConfig("abcdefghijklmnopqrstuvwxyz".toUpperCase(), 16);
		this.logger.info("Nano_Reconfigure_Random", IDUtils.nano());
	}

	@Test
	@Order(10)
	public void snowflake() {
		this.logger.info("Snowflake_Random", IDUtils.snowflake());
		Optional.ofNullable(DateTimeUtils.parseDate("20030421", "yyyyMMdd"))
				.ifPresent(date -> IDUtils.snowflakeConfig(date.getTime(), 2L, 5L));
		this.logger.info("Snowflake_Reconfigure_Random", IDUtils.snowflake());
	}

	@Test
	@Order(20)
	public void UUID() {
		//  Version 1
		for (int i = 0; i < 10; i++) {
			this.logger.info("UUID_Random", 1, IDUtils.UUIDv1());
		}
		IDUtils.UUIDv1Config(null, UUIDIdentifier.RANDOM);
		this.logger.info("UUID_Random", 1, IDUtils.UUIDv1());

		//  Version 2
		this.logger.info("UUID_Random", 2, IDUtils.UUIDv2());
		IDUtils.UUIDv2Config(null, UUIDIdentifier.RANDOM, UUIDLocalDomain.GROUP);
		this.logger.info("UUID_Random", 2, IDUtils.UUIDv2());
		//  Version 3
		this.logger.info("UUID_Random", 3, IDUtils.UUIDv3("TestVersion3".getBytes()));
		//  Version 4
		this.logger.info("UUID_Random", 4, IDUtils.UUIDv4());
		//  Version 5
		this.logger.info("UUID_Random", 5, IDUtils.UUIDv5("TestVersion5".getBytes()));
		//  Version 6
		for (int i = 0; i < 10; i++) {
			this.logger.info("UUID_Random", 6, IDUtils.UUIDv6());
		}
		//  Version 7
		this.logger.info("UUID_Random", 7, IDUtils.UUIDv7());
	}

	@Test
	@Order(30)
	public void ulid() {
		this.logger.info("ULID_Random", IDUtils.ULID());
		for (int i = 0; i < 10; i++) {
			this.logger.info("ULID_Random", IDUtils.ULID());
		}
		Optional.ofNullable(DateTimeUtils.parseDate("20030421", "yyyyMMdd"))
				.ifPresent(date -> IDUtils.ulidConfig(date.getTime(), Boolean.TRUE));
		for (int i = 0; i < 10; i++) {
			ULID ulid = IDUtils.ULID();
			this.logger.info("ULID_Reconfigure_Random", ulid);
		}
	}

	@Test
	@Order(40)
	public void cuid() {
		this.logger.info("CUID_Random", "1", IDUtils.CUIDv1());
		for (int i = 0; i < 10; i++) {
			CUID cuid = IDUtils.CUIDv1();
			this.logger.info("CUID_Random", "1", cuid);
		}

		this.logger.info("CUID_Random", "2", IDUtils.CUIDv2());
		this.logger.info("CUID_Random", "2", IDUtils.CUIDv2(120));
	}

	@Test
	@Order(50)
	public void sparkId() {
		SparkID sparkID = SparkID.fromString("1ocmpHE1bFnygEBAPTzMK");
		this.logger.info("SparkID_Random", sparkID);
		this.logger.info("SparkID_Epoch_Random", IDUtils.sparkID(1767225600000L), 1767225600000L);
		IDUtils.sparkIdConfig(2L);
		this.logger.info("SparkID_Worker_Random", IDUtils.sparkID(), 2L);
	}
}