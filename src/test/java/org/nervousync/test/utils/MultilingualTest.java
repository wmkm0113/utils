package org.nervousync.test.utils;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.MultilingualUtils;

import java.util.Locale;

public final class MultilingualTest extends BaseTest {

    @Test
    @Order(10)
    public void message() {
        this.logger.info("Not_Support_Type_Location_Error");
        MultilingualUtils.defaultLocale(Locale.CHINA);
        MultilingualUtils.disableLanguage("en-US");
        this.logger.info("Out_Of_Index_Raw_Error", 10, 8, 3);
    }

    @Test
    @Order(20)
    public void destroy() {
        MultilingualUtils.defaultLocale(Locale.US);
        MultilingualUtils.enableLanguages("en-US");
        MultilingualUtils.disableLanguage("zh-CN");
        this.logger.info("Out_Of_Index_Raw_Error", 10, 8, 3);
        MultilingualUtils.removeBundle("org.nervousync", "utils");
        this.logger.info("Not_Support_Type_Location_Error");
    }
}
