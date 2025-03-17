package org.nervousync.test.utils;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.MultilingualUtils;

import java.util.Locale;

public final class MultilingualTest extends BaseTest {

    private final MultilingualUtils.Agent multiAgent = MultilingualUtils.newAgent("org.nervousync", "utils");

    @Test
    @Order(10)
    public void message() {
        this.logger.info(this.multiAgent.findMessage("Not_Support_Type_Location_Error"));
        this.logger.info(this.multiAgent.findMessage("Out_Of_Index_Raw_Error", Locale.CHINA, 10, 8, 3));
    }

    @Test
    @Order(20)
    public void destroy() {
        MultilingualUtils.disableLanguage("zh-CN");
        this.logger.info(this.multiAgent.findMessage("Out_Of_Index_Raw_Error", Locale.CHINA, 10, 8, 3));
        MultilingualUtils.removeBundle("org.nervousync", "utils-jdk11");
        this.logger.info(this.multiAgent.findMessage("Not_Support_Type_Location_Error"));
    }
}
