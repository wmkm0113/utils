package org.nervousync.test;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.*;
import org.nervousync.commons.Globals;
import org.nervousync.utils.logger.LoggerUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseTest {

    protected transient final LoggerUtils.Logger logger = LoggerUtils.getLogger(this.getClass());

    static {
        LoggerUtils.initLoggerConfigure(Level.DEBUG);
    }

    protected BaseTest() {
    }

    @BeforeEach
    public final void init(final TestInfo testInfo) {
        this.logger.info("Execute_Begin_Test",
                testInfo.getTestClass().map(Class::getName).orElse(Globals.DEFAULT_VALUE_STRING), testInfo.getDisplayName());
    }

    @AfterEach
    public void print(TestInfo testInfo) {
        this.logger.info("Execute_End_Test",
                testInfo.getTestClass().map(Class::getName).orElse(Globals.DEFAULT_VALUE_STRING), testInfo.getDisplayName());
    }
}
