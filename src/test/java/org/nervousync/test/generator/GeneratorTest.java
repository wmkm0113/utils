package org.nervousync.test.generator;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.DateTimeUtils;
import org.nervousync.utils.IDUtils;

import java.util.Optional;

public final class GeneratorTest extends BaseTest {

    public GeneratorTest() {
//        super(Locale.CHINA);
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
        this.logger.info("UUID_Random", 1, IDUtils.UUIDv1());
        this.logger.info("UUID_Random", 2, IDUtils.UUIDv2());
        this.logger.info("UUID_Random", 3, IDUtils.UUIDv3("TestVersion3".getBytes()));
        this.logger.info("UUID_Random", 4, IDUtils.UUIDv4());
        this.logger.info("UUID_Random", 5, IDUtils.UUIDv5("TestVersion5".getBytes()));
    }

    @Test
    @Order(30)
    public void ulid() {
        this.logger.info("ULID_Random", IDUtils.ULID());
        Optional.ofNullable(DateTimeUtils.parseDate("20030421", "yyyyMMdd"))
                .ifPresent(date -> IDUtils.ulidConfig(date.getTime()));
        for (int i = 0 ; i < 10 ; i++) {
            this.logger.info("ULID_Reconfigure_Random", IDUtils.ULID());
        }
    }
}
