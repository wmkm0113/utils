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

package org.nervousync.test.office;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.commons.Globals;
import org.nervousync.office.excel.ExcelWriter;
import org.nervousync.office.excel.SheetWriter;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.FileUtils;
import org.nervousync.utils.office.OfficeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class OfficeTest extends BaseTest {
    private static final String FILE_PATH = "src/test/resources/test.xlsx";
    private static final String BASE_PATH;

    static {
        String tmpDir = System.getProperty("java.io.tmpdir");
        BASE_PATH = tmpDir.endsWith(Globals.DEFAULT_PAGE_SEPARATOR)
                ? tmpDir.substring(0, tmpDir.length() - 1)
                : tmpDir;
    }

    @AfterAll
    public static void clean() {
        FileUtils.removeFile(BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "test.xls");
        FileUtils.removeFile(BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + "test.xlsx");
    }

    @Test
    @Order(0)
    public void checkExists() {
        this.logger.info("Office_Sheet_Exists", "3",
                OfficeUtils.sheetExists(FILE_PATH, "3"));
        this.logger.info("Office_Sheet_Exists", "3, 6, 9",
                OfficeUtils.sheetExists(FILE_PATH, "3", "6", "9"));
    }

    @Test
    @Order(10)
    public void rowsCount() {
        OfficeUtils.excelRowsCount(FILE_PATH)
                .forEach((sheetName, rowsCount) ->
                        this.logger.info("Office_Excel_Rows_Count", sheetName, rowsCount));
    }

    @Test
    @Order(20)
    public void sheetRowsCount() {
        this.logger.info("Office_Excel_Rows_Count",
                "3", OfficeUtils.excelRowsCount(FILE_PATH, "3"));
    }

    @Test
    @Order(30)
    public void readData() {
        OfficeUtils.readExcel(FILE_PATH)
                .forEach((sheetName, rowsData) ->
                        this.logger.info("Office_Excel_Rows_Count", sheetName, rowsData.size()));
    }

    @Test
    @Order(40)
    public void readPartData() {
        OfficeUtils.readExcel(FILE_PATH, 18, 30)
                .forEach((sheetName, rowsData) ->
                        this.logger.info("Office_Excel_Rows_Count", sheetName, rowsData.size()));
    }

    @Test
    @Order(50)
    public void readSheetData() {
        this.logger.info("Office_Excel_Rows_Count",
                "3", OfficeUtils.readExcel(FILE_PATH, "3").size());
    }

    @Test
    @Order(60)
    public void readSheetPartData() {
        this.logger.info("Office_Excel_Rows_Count",
                "3", OfficeUtils.readExcel(FILE_PATH, "3", 18, 30).size());
    }

    @Test
    @Order(70)
    public void writer() {
        Arrays.asList("test.xls", "test.xlsx")
                .forEach(fileName -> {
                    String filePath = BASE_PATH + Globals.DEFAULT_PAGE_SEPARATOR + fileName;
                    try (ExcelWriter excelWriter = OfficeUtils.newWriter(filePath)) {
                        OfficeUtils.readExcel(FILE_PATH)
                                .forEach((sheetName, rowsData) -> {
                                    SheetWriter sheetWriter = excelWriter.sheetWriter(sheetName);
                                    final AtomicInteger index = new AtomicInteger(Globals.INITIALIZE_INT_VALUE);
                                    rowsData.forEach(rowData ->
                                            sheetWriter.writeData(index.getAndIncrement(), new ArrayList<>(rowData)));
                                });
                        excelWriter.write();
                    } catch (Exception e) {
                        this.logger.error("Stack_Message_Error", e);
                        return;
                    }
                    OfficeUtils.readExcel(filePath)
                            .forEach((sheetName, rowsData) ->
                                    this.logger.info("Office_Excel_Rows_Count", sheetName, rowsData.size()));
                });
    }
}
