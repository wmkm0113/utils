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

package org.nervousync.test.image;

import com.google.zxing.BarcodeFormat;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.beans.image.CodeOptions;
import org.nervousync.beans.image.MarkOptions;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.image.CodeUtils;
import org.nervousync.utils.image.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Optional;

public class CodeTest extends BaseTest {

	@Test
	@Order(10)
	public void qrCode() {
		String text = StringUtils.randomString(64);
		CodeOptions codeOptions = CodeOptions.newBuilder(BarcodeFormat.QR_CODE)
				.markIcon(MarkOptions.MarkLocation.CENTER, "src/test/resources/Logo.png", 1f)
				.codeSize(1500, 1500)
				.build();
		Optional.ofNullable(CodeUtils.generate(text, codeOptions))
				.map(bufferedImage -> ImageUtils.processImage(bufferedImage, 50, 50, null))
				.ifPresent(bufferedImage -> this.printImage(bufferedImage, "  "));
	}

	@Test
	@Order(20)
	public void barCode() {
		String text = StringUtils.randomNumber(13);
		CodeOptions codeOptions = CodeOptions.newBuilder(BarcodeFormat.CODE_93)
				.codeColor(0, 0, 0)
				.codeSize(50, 10)
				.build();
		Optional.ofNullable(CodeUtils.generate(text, codeOptions))
				.ifPresent(bufferedImage -> this.printImage(bufferedImage, " "));
	}

	@Test
	@Order(30)
	public void aztec() {
		String text = StringUtils.randomNumber(12);
		CodeOptions codeOptions = CodeOptions.newBuilder(BarcodeFormat.AZTEC)
				.codeColor(0, 0, 0)
				.codeSize(50, 10)
				.build();
		Optional.ofNullable(CodeUtils.generate(text, codeOptions))
				.ifPresent(bufferedImage -> this.printImage(bufferedImage, "   "));
	}

	private void printImage(final BufferedImage bufferedImage, final String string) {
		for (int y = 0; y < bufferedImage.getHeight(); y++) {
			for (int x = 0; x < bufferedImage.getWidth(); x++) {
				Color color = new Color(bufferedImage.getRGB(x, y));
				System.out.print("\u001B[48;2;" + color.getRed() + ";" + color.getGreen() + ";" + color.getBlue() + "m" + string + "\u001B[0m");
			}
			System.out.println();
		}
	}
}
