package org.nervousync.test.security;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.core.StringUtils;
import org.nervousync.utils.security.SecurityUtils;

import java.nio.charset.StandardCharsets;
import java.security.*;

@SuppressWarnings("unused")
public final class CryptoTest extends BaseTest {

    private static final String[] AES_CIPHER_MODES =
            new String[]{"ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"};
    private static final String[] DES_CIPHER_MODES =
            new String[]{"ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"};
    private static final String[] TRIPLE_DES_CIPHER_MODES =
            new String[]{"ECB", "CBC", "CTR", "CTS", "CFB", "OFB", "CFB8", "OFB8"};
    private static final String[] DEFAULT_PADDINGS = new String[]{"PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "X9.23Padding"};

    private static final String[] RSA_PADDINGS = new String[]{
            "NoPadding", "PKCS1Padding", "OAEPWithSHA-1AndMGF1Padding", "OAEPWithSHA-224AndMGF1Padding",
            "OAEPWithSHA-256AndMGF1Padding", "OAEPWithSHA-384AndMGF1Padding", "OAEPWithSHA-512AndMGF1Padding",
            "OAEPWithSHA3-224AndMGF1Padding", "OAEPWithSHA3-256AndMGF1Padding", "OAEPWithSHA3-384AndMGF1Padding",
            "OAEPWithSHA3-512AndMGF1Padding"
    };
    private static final String ORIGINAL_STRING = "Test测试TestTest测试TestTestTest测试TestTestTestTest测试TestTestTestTestTest测试TestTestTestTestTestTest测试TestTestTestTestTestTestTest测试TestTestTestTestTest测试";
    private static final String ENC_STRING = "gE1XZYLds5uZEFfI80poxN69fkAQQT3NteM7xSqUIyZSULce3b9lMobxR2aWm/4Hpyg64vvRwv7MLdZy9xMOtBC4+enJOywXyU5+plpOqX5J7VsxHw5Ri94X2C70/XR5z9CHc4n3g/CDKAF5MBBZmh0+362VtrRWkB+nN7XEPregccxg0/4ytwGgcxQYb8wfKfU2CvsfY/dfrlv2ynWfS9HaxHEXmIFT5BTQvgZrd+0B+br4C8X5AOEmONEINv9h1rOsPD6AtN42pHd5022D6l4riwXVO3LfLEFJxomg06w3XDEinMk5EjQ4FgoeJDA68meSPmwF1oBoKgDMH/ElrQ==";

    private static final String[] SM4_CIPHER_MODES = new String[]{"ECB", "CBC", "CTR", "CFB", "OFB", "CFB8", "OFB8", "CFB128", "OFB128"};
    private static final String[] RC_CIPHER_MODES = new String[]{"ECB", "CBC", "CTR", "CFB", "OFB", "OFB", "CFB8", "OFB8"};

    @Test
    @Order(10)
    public void DES() throws CryptoException {
        byte[] desKey = SecurityUtils.DESKey();
        this.logger.info("Crypto_Key_Length", "DES", StringUtils.base64Encode(desKey));
        for (String cipherMode : DES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                CryptoAdaptor encryptProvider = SecurityUtils.DESEncryptor(cipherMode, padding, desKey);
                String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                this.logger.info("Encrypt_Result", "DES", cipherMode, padding, encResult);
                CryptoAdaptor decryptProvider = SecurityUtils.DESDecryptor(cipherMode, padding, desKey);
                this.logger.info("Decrypt_Result", "DES", cipherMode, padding,
                        new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    @Order(0)
    public void AES() {
        byte[] aesKey = SecurityUtils.AES128Key();
        this.logger.info("Crypto_Key_Length", "AES128", StringUtils.base64Encode(aesKey));
        for (String cipherMode : AES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                try {
                    CryptoAdaptor encryptProvider = SecurityUtils.AESEncryptor(cipherMode, padding, aesKey);
                    encryptProvider.append(ORIGINAL_STRING);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish());
                    this.logger.info("Encrypt_Result", "AES", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = SecurityUtils.AESDecryptor(cipherMode, padding, aesKey);
                    decryptProvider.append(StringUtils.base64Decode(encResult));
                    this.logger.info("Decrypt_Result", "AES", cipherMode, padding,
                            new String(decryptProvider.finish(), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "AES128", "AES/" + cipherMode + "/" + padding);
                }
            }
        }
        aesKey = SecurityUtils.AES192Key();
        this.logger.info("Crypto_Key_Length", "AES192", StringUtils.base64Encode(aesKey));
        for (String cipherMode : AES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                try {
                    CryptoAdaptor encryptProvider = SecurityUtils.AESEncryptor(cipherMode, padding, aesKey);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "AES", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = SecurityUtils.AESDecryptor(cipherMode, padding, aesKey);
                    this.logger.info("Decrypt_Result", "AES", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "AES192", "AES/" + cipherMode + "/" + padding);
                }
            }
        }
        aesKey = SecurityUtils.AES256Key();
        this.logger.info("Crypto_Key_Length", "AES256", StringUtils.base64Encode(aesKey));
        for (String cipherMode : AES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                try {
                    CryptoAdaptor encryptProvider = SecurityUtils.AESEncryptor(cipherMode, padding, aesKey);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "AES", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = SecurityUtils.AESDecryptor(cipherMode, padding, aesKey);
                    this.logger.info("Decrypt_Result", "AES", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "AES256", "AES/" + cipherMode + "/" + padding);
                }
            }
        }
    }

    @Test
    @Order(20)
    public void tripleDES() throws CryptoException {
        byte[] desKey = SecurityUtils.TripleDESKey();
        this.logger.info("Crypto_Key_Length", "TripleDES", StringUtils.base64Encode(desKey));
        for (String cipherMode : TRIPLE_DES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                try {
                    CryptoAdaptor encryptProvider = SecurityUtils.TripleDESEncryptor(cipherMode, padding, desKey);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "DESede", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = SecurityUtils.TripleDESDecryptor(cipherMode, padding, desKey);
                    this.logger.info("Decrypt_Result", "DESede", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "TripleDES", "DESede/" + cipherMode + "/" + padding);
                }
            }
        }
    }

    @Test
    @Order(40)
    public void RSA() throws CryptoException {
        //  Generate RSA certificate
        KeyPair keyPair = SecurityUtils.RSAKeyPair();
        //  Testing for encrypting and decrypt data using RSA
        for (String padding : RSA_PADDINGS) {
            if (padding.equalsIgnoreCase("OAEPWithSHA-512AndMGF1Padding")) {
                //  Minimum key size was 2048 when padding mode is "OAEPWithSHA-512AndMGF1Padding"
                keyPair = SecurityUtils.RSAKeyPair(2048);
            }
            try {
                CryptoAdaptor encryptProvider = SecurityUtils.RSAEncryptor(padding, keyPair.getPublic());
                String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                this.logger.info("Encrypt_Result", "RSA", "None", padding, encResult);
                CryptoAdaptor decryptProvider = SecurityUtils.RSADecryptor(padding, keyPair.getPrivate());
                decryptProvider.append(StringUtils.base64Decode(encResult));
                this.logger.info("Decrypt_Result", "RSA", "None", padding,
                        new String(decryptProvider.finish(), StandardCharsets.UTF_8));
            } catch (CryptoException e) {
                this.logger.warn("Crypto_Not_Support", "RSA", "RSA/None/" + padding);
            }
        }
        String randomString = StringUtils.randomString(128);
        CryptoAdaptor signProvider = SecurityUtils.RSASigner(keyPair.getPrivate());
        byte[] signBytes = signProvider.finish(randomString);
        this.logger.info("Signature_Result", randomString, "RSA", StringUtils.base64Encode(signBytes));
        CryptoAdaptor verifyProvider = SecurityUtils.RSAVerifier(keyPair.getPublic());
        verifyProvider.append(randomString);
        this.logger.info("Verify_Result", "RSA", verifyProvider.verify(signBytes));
    }

    @Test
    @Order(60)
    public void RC2() throws CryptoException {
        byte[] rc2Key = SecurityUtils.RC2Key();
        this.logger.info("Crypto_Key_Length", "RC2", StringUtils.base64Encode(rc2Key));
        CryptoAdaptor encAdaptor = SecurityUtils.RC2Encryptor(rc2Key);
        String defaultResult = StringUtils.base64Encode(encAdaptor.finish(ORIGINAL_STRING));
        this.logger.info("Encrypt_Result", "RC2", "CBC", "PKCS7Padding", defaultResult);
        CryptoAdaptor decAdaptor = SecurityUtils.RC2Decryptor(rc2Key);
        this.logger.info("Decrypt_Result", "RC2", "CBC", "PKCS7Padding",
                new String(decAdaptor.finish(StringUtils.base64Decode(defaultResult)), StandardCharsets.UTF_8));
        for (String cipherMode : RC_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                CryptoAdaptor encryptProvider = SecurityUtils.RC2Encryptor(cipherMode, padding, rc2Key);
                String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                this.logger.info("Encrypt_Result", "RC2", cipherMode, padding, encResult);
                CryptoAdaptor decryptProvider = SecurityUtils.RC2Decryptor(cipherMode, padding, rc2Key);
                this.logger.info("Decrypt_Result", "RC2", cipherMode, padding,
                        new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    @Order(70)
    public void RC4() throws CryptoException {
        byte[] rc4Key = SecurityUtils.RC4Key();
        this.logger.info("Crypto_Key_Length", "RC4", StringUtils.base64Encode(rc4Key));
        CryptoAdaptor encryptProvider = SecurityUtils.RC4Encryptor(rc4Key);
        String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
        this.logger.info("Encrypt_Result", "RC4", "", "", encResult);
        CryptoAdaptor decryptProvider = SecurityUtils.RC4Decryptor(rc4Key);
        this.logger.info("Decrypt_Result", "RC4", "", "",
                new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
    }

    @Test
    @Order(80)
    public void RC5() throws CryptoException {
        byte[] rc5Key = SecurityUtils.RC5Key();
        for (String cipherMode : RC_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                try {
                    CryptoAdaptor encryptProvider = SecurityUtils.RC5Encryptor(cipherMode, padding, rc5Key);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "RC5", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = SecurityUtils.RC5Decryptor(cipherMode, padding, rc5Key);
                    this.logger.info("Decrypt_Result", "RC5", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "RC5", "RC5/" + cipherMode + "/" + padding);
                }
            }
        }
    }

    @Test
    @Order(90)
    public void RC6() throws CryptoException {
        byte[] rc6Key = SecurityUtils.RC6Key();
        this.logger.info("Crypto_Key_Length", "RC6", StringUtils.base64Encode(rc6Key));
        for (String cipherMode : RC_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                try {
                    CryptoAdaptor encryptProvider = SecurityUtils.RC6Encryptor(cipherMode, padding, rc6Key);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "RC6", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = SecurityUtils.RC6Decryptor(cipherMode, padding, rc6Key);
                    this.logger.info("Decrypt_Result", "RC6", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "RC6", "RC6/" + cipherMode + "/" + padding);
                }
            }
        }
    }

    @Test
    @Order(100)
    public void Blowfish() throws CryptoException {
        byte[] blowfishKey = SecurityUtils.BlowfishKey();
        for (String cipherMode : RC_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                try {
                    CryptoAdaptor encryptProvider = SecurityUtils.BlowfishEncryptor(cipherMode, padding, blowfishKey);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "Blowfish", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = SecurityUtils.BlowfishDecryptor(cipherMode, padding, blowfishKey);
                    this.logger.info("Decrypt_Result", "Blowfish", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "Blowfish", "Blowfish/" + cipherMode + "/" + padding);
                }
            }
        }
    }
}
