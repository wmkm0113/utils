package org.nervousync.test.otp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.commons.Globals;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.otp.OTPUtils;

public final class OTPTest extends BaseTest {

	private static String RANDOM_KEY = Globals.DEFAULT_VALUE_STRING;

	@Test
	@Order(0)
	public void generateRandomKey() {
		String randomKey = OTPUtils.generateRandomKey();
		Assertions.assertNotNull(randomKey);
		this.logger.info("OTP_Random_Key", randomKey);
		//  Test generate random key by given size
		RANDOM_KEY = OTPUtils.generateRandomKey(32);
		Assertions.assertNotNull(RANDOM_KEY);
		this.logger.info("OTP_Random_Key_Length", 32, RANDOM_KEY);
	}

	@Test
	@Order(10)
	public void HOTP() {
		int generateCode = OTPUtils.generateHOTPCode(RANDOM_KEY, 0L);
		Assertions.assertTrue(generateCode != Globals.DEFAULT_VALUE_INT);
		this.logger.info("Generate_Result_HOTP", generateCode, "HmacSHA1");
		Assertions.assertTrue(OTPUtils.validateHOTPCode(generateCode, RANDOM_KEY, 0L));
		//  HmacSHA256
		generateCode = OTPUtils.generateHOTPCode(OTPUtils.CalcType.HmacSHA256, RANDOM_KEY, 0L);
		Assertions.assertTrue(generateCode != Globals.DEFAULT_VALUE_INT);
		this.logger.info("Generate_Result_HOTP", generateCode, "HmacSHA256");
		Assertions.assertTrue(OTPUtils.validateHOTPCode(generateCode, OTPUtils.CalcType.HmacSHA256, RANDOM_KEY, 0L));
		//  HmacSHA256
		generateCode = OTPUtils.generateHOTPCode(OTPUtils.CalcType.HmacSHA512, RANDOM_KEY, 0L);
		Assertions.assertTrue(generateCode != Globals.DEFAULT_VALUE_INT);
		this.logger.info("Generate_Result_HOTP", generateCode, "HmacSHA512");
		Assertions.assertFalse(OTPUtils.validateHOTPCode(generateCode, OTPUtils.CalcType.HmacSHA256, RANDOM_KEY, 0L));
	}

	@Test
	@Order(20)
	public void TOTP() {
		String generateCode = OTPUtils.generateTOTPCode(RANDOM_KEY);
		Assertions.assertTrue(StringUtils.notBlank(generateCode));
		this.logger.info("Generate_Result_TOTP", generateCode, "HmacSHA1");
		long fixTime = OTPUtils.calculateFixedTime(RANDOM_KEY, Integer.parseInt(generateCode));
		this.logger.info("Generate_Result_Fix_Time", fixTime, generateCode);
		Assertions.assertTrue(OTPUtils.validateTOTPCode(Integer.parseInt(generateCode), RANDOM_KEY, fixTime));
		generateCode = OTPUtils.generateTOTPCode(RANDOM_KEY, fixTime);
		this.logger.info("Generate_Result_TOTP_Fix_Time", generateCode, "HmacSHA1", fixTime);
		Assertions.assertTrue(OTPUtils.validateTOTPCode(Integer.parseInt(generateCode), RANDOM_KEY, fixTime, 60));
		//  Fixed time
		long currentFixedTime = -8 * 60 * 60 * 1000L;
		generateCode = OTPUtils.generateTOTPCode(RANDOM_KEY, currentFixedTime, 1);
		Assertions.assertTrue(StringUtils.notBlank(generateCode));
		this.logger.info("Generate_Result_TOTP_Config", generateCode, "HmacSHA1", currentFixedTime, 1);
		long calcFixedTime = OTPUtils.calculateFixedTime(RANDOM_KEY, Integer.parseInt(generateCode), 1);
		this.logger.info("Generate_Result_Fix_Time", calcFixedTime, generateCode);
		Assertions.assertTrue(OTPUtils.validateTOTPCode(Integer.parseInt(generateCode), OTPUtils.CalcType.HmacSHA1, RANDOM_KEY, calcFixedTime, 1, 60));
		//  Generate by sync count
		generateCode = OTPUtils.generateTOTPCode(RANDOM_KEY, currentFixedTime, 60);
		Assertions.assertTrue(StringUtils.notBlank(generateCode));
		this.logger.info("Generate_Result_TOTP_Config", generateCode, "HmacSHA1", currentFixedTime, 60);
		Assertions.assertTrue(OTPUtils.validateTOTPCode(Integer.parseInt(generateCode), OTPUtils.CalcType.HmacSHA1, RANDOM_KEY, currentFixedTime, 60, 1));
		//  HmacSHA256
		generateCode = OTPUtils.generateTOTPCode(OTPUtils.CalcType.HmacSHA256, RANDOM_KEY, fixTime);
		Assertions.assertTrue(StringUtils.notBlank(generateCode));
		this.logger.info("Generate_Result_TOTP_Fix_Time", generateCode, "HmacSHA256", fixTime);
		Assertions.assertTrue(OTPUtils.validateTOTPCode(Integer.parseInt(generateCode), OTPUtils.CalcType.HmacSHA256, RANDOM_KEY, fixTime, Globals.DEFAULT_VALUE_INT, Globals.DEFAULT_VALUE_INT));
		calcFixedTime = OTPUtils.calculateFixedTime(OTPUtils.CalcType.HmacSHA256, RANDOM_KEY, Integer.parseInt(generateCode));
		this.logger.info("Generate_Result_Fix_Time", calcFixedTime, generateCode);
		//  HmacSHA256
		generateCode = OTPUtils.generateTOTPCode(OTPUtils.CalcType.HmacSHA512, RANDOM_KEY, fixTime);
		Assertions.assertTrue(StringUtils.notBlank(generateCode));
		this.logger.info("Generate_Result_TOTP_Fix_Time", generateCode, "HmacSHA512", fixTime);
		Assertions.assertFalse(OTPUtils.validateTOTPCode(Integer.parseInt(generateCode), OTPUtils.CalcType.HmacSHA256, RANDOM_KEY, fixTime, Globals.DEFAULT_VALUE_INT, Globals.DEFAULT_VALUE_INT));
	}
}
