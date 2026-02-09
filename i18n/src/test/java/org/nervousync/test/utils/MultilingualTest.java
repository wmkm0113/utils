package org.nervousync.test.utils;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.i18n.LocaleUtils;
import org.nervousync.utils.i18n.MultilingualUtils;
import org.nervousync.utils.logger.LoggerUtils;

import java.util.Locale;

public final class MultilingualTest extends BaseTest {

    @Test
    @Order(10)
    public void message() {
        this.logger.info("Not_Support_Type_Location_Error");
        LocaleUtils.defaultLocale(Locale.CHINA);
        this.logger.info("Out_Of_Index_Raw_Error", 10, 8, 3);
    }

    @Test
    @Order(20)
    public void destroy() {
        LocaleUtils.defaultLocale(Locale.US);
        this.logger.info("Out_Of_Index_Raw_Error", 10, 8, 3);
        MultilingualUtils.removeBundle("org.nervousync", "utils-core");
        LoggerUtils.getLogger(this.getClass()).info("Not_Support_Type_Location_Error");
    }
}
