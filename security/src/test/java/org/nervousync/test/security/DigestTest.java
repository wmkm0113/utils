package org.nervousync.test.security;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.exceptions.utils.DataInvalidException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.ConvertUtils;
import org.nervousync.utils.security.SecurityUtils;

public final class DigestTest extends BaseTest {

    @Test
    @Order(0)
    public void CRC() throws CryptoException, DataInvalidException {
        for (String algorithm : SecurityUtils.registeredCRC()) {
            CryptoAdaptor CryptoAdaptor = SecurityUtils.CRC(algorithm);
            this.logger.info("CRC_Result", algorithm,
                    SecurityUtils.CRCResult(algorithm, CryptoAdaptor.finish("123456")));
        }
    }

    @Test
    @Deprecated
    @Order(10)
    public void MD5() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.MD5();
        this.logger.info("Digits_Result", "MD5", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "MD5", ConvertUtils.bytesToHex(SecurityUtils.MD5("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacMD5", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacMD5("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacMD5", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacMD5("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Deprecated
    @Order(20)
    public void SHA1() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA1();
        this.logger.info("Digits_Result", "SHA1", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA1", ConvertUtils.bytesToHex(SecurityUtils.SHA1("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA1", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA1("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA1 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA1("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(30)
    public void SHA224() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA224();
        this.logger.info("Digits_Result", "SHA-224", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA-224", ConvertUtils.bytesToHex(SecurityUtils.SHA224("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA224", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA224("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA224 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA224("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(31)
    public void SHA256() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA256();
        this.logger.info("Digits_Result", "SHA-256", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA-256", ConvertUtils.bytesToHex(SecurityUtils.SHA256("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA256", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA256("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA256 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA256("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(32)
    public void SHA384() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA384();
        this.logger.info("Digits_Result", "SHA-384", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA-384", ConvertUtils.bytesToHex(SecurityUtils.SHA384("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA384", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA384("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA384 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA384("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(33)
    public void SHA512() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA512();
        this.logger.info("Digits_Result", "SHA-512", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA-512", ConvertUtils.bytesToHex(SecurityUtils.SHA512("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA512", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA512("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA512 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA512("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(34)
    public void SHA512_224() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA512_224();
        this.logger.info("Digits_Result", "SHA-512/224", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA-512/224", ConvertUtils.bytesToHex(SecurityUtils.SHA512_224("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA512/224", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA512_224("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA512/224 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA512_224("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(35)
    public void SHA512_256() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA512_256();
        this.logger.info("Digits_Result", "SHA-512/256", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA-512/256", ConvertUtils.bytesToHex(SecurityUtils.SHA512_256("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA512/256", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA512_256("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA512/256 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA512_256("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(40)
    public void SHA3_224() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA3_224();
        this.logger.info("Digits_Result", "SHA3-224", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA3-224", ConvertUtils.bytesToHex(SecurityUtils.SHA3_224("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA3-224", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA3_224("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA3-224 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA3_224("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(41)
    public void SHA3_256() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA3_256();
        this.logger.info("Digits_Result", "SHA3-256", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA3-256", ConvertUtils.bytesToHex(SecurityUtils.SHA3_256("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA3-256", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA3_256("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA3-256 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA3_256("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(42)
    public void SHA3_384() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA3_384();
        this.logger.info("Digits_Result", "SHA3-384", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA3-384", ConvertUtils.bytesToHex(SecurityUtils.SHA3_384("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA3-384", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA3_384("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA3-384 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA3_384("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(43)
    public void SHA3_512() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHA3_512();
        this.logger.info("Digits_Result", "SHA3-512", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHA3-512", ConvertUtils.bytesToHex(SecurityUtils.SHA3_512("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSHA3-512", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA3_512("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSHA3-512 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSHA3_512("110421".getBytes(), "123456")), "(Static)");
    }

    @Test
    @Order(44)
    public void SHAKE128() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHAKE128();
        this.logger.info("Digits_Result", "SHAKE128", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHAKE128", ConvertUtils.bytesToHex(SecurityUtils.SHAKE128("123456")), "(Static)");
    }

    @Test
    @Order(45)
    public void SHAKE256() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SHAKE256();
        this.logger.info("Digits_Result", "SHAKE256", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SHAKE256", ConvertUtils.bytesToHex(SecurityUtils.SHAKE256("123456")), "(Static)");
    }

    @Test
    @Order(50)
    public void SM3() throws CryptoException {
        CryptoAdaptor CryptoAdaptor = SecurityUtils.SM3();
        this.logger.info("Digits_Result", "SM3", ConvertUtils.bytesToHex(CryptoAdaptor.finish("123456")), "(Provider)");
        this.logger.info("Digits_Result", "SM3", ConvertUtils.bytesToHex(SecurityUtils.SM3("123456")), "(Static)");
        this.logger.info("Hmac_Result", "HmacSM3", "110421",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSM3("110421".getBytes()).finish("123456")), "(Provider)");
        this.logger.info("Hmac_Result", "HmacSM3 key: 110421,",
                ConvertUtils.bytesToHex(SecurityUtils.HmacSM3("110421".getBytes(), "123456")), "(Static)");
    }
}
