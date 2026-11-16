package org.nervousync.test.security;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.nervousync.exceptions.crypto.CryptoException;
import org.nervousync.security.CryptoAdaptor;
import org.nervousync.test.BaseTest;
import org.nervousync.utils.commons.StringUtils;
import org.nervousync.utils.security.CryptoUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

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
    @Deprecated(since = "1.4.0")
    public void DES() throws CryptoException {
        byte[] desKey = CryptoUtils.DESKey();
        this.logger.info("Crypto_Key_Length", "DES", StringUtils.base64Encode(desKey));
        for (String cipherMode : DES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("DES", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.DESEncryptor(cipherMode, padding, desKey, iv);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "DES", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.DESDecryptor(cipherMode, padding, desKey, iv);
                    this.logger.info("Decrypt_Result", "DES", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "DES", "DES/" + cipherMode + "/" + padding);
                }
            }
        }
    }

    @Test
    @Order(0)
    public void AES() {
        byte[] aesKey = CryptoUtils.AES128Key();
        this.logger.info("Crypto_Key_Length", "AES128", StringUtils.base64Encode(aesKey));
        for (String cipherMode : AES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("AES", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.AESEncryptor(cipherMode, padding, aesKey, iv);
                    encryptProvider.append(ORIGINAL_STRING);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish());
                    this.logger.info("Encrypt_Result", "AES", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.AESDecryptor(cipherMode, padding, aesKey, iv);
                    decryptProvider.append(StringUtils.base64Decode(encResult));
                    this.logger.info("Decrypt_Result", "AES", cipherMode, padding,
                            new String(decryptProvider.finish(), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "AES128", "AES/" + cipherMode + "/" + padding);
                }
            }
        }
        aesKey = CryptoUtils.AES192Key();
        this.logger.info("Crypto_Key_Length", "AES192", StringUtils.base64Encode(aesKey));
        for (String cipherMode : AES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("AES", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.AESEncryptor(cipherMode, padding, aesKey, iv);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "AES", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.AESDecryptor(cipherMode, padding, aesKey, iv);
                    this.logger.info("Decrypt_Result", "AES", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "AES192", "AES/" + cipherMode + "/" + padding);
                }
            }
        }
        aesKey = CryptoUtils.AES256Key();
        this.logger.info("Crypto_Key_Length", "AES256", StringUtils.base64Encode(aesKey));
        for (String cipherMode : AES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("AES", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.AESEncryptor(cipherMode, padding, aesKey, iv);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "AES", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.AESDecryptor(cipherMode, padding, aesKey, iv);
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
    @Deprecated(since = "1.4.0")
    public void tripleDES() throws CryptoException {
        byte[] desKey = CryptoUtils.TripleDESKey();
        this.logger.info("Crypto_Key_Length", "TripleDES", StringUtils.base64Encode(desKey));
        for (String cipherMode : TRIPLE_DES_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("DESede", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.TripleDESEncryptor(cipherMode, padding, desKey, iv);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "DESede", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.TripleDESDecryptor(cipherMode, padding, desKey, iv);
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
        KeyPair keyPair = CryptoUtils.RSAKeyPair();
        //  Testing for encrypting and decrypt data using RSA
        for (String padding : RSA_PADDINGS) {
            if (padding.equalsIgnoreCase("OAEPWithSHA-512AndMGF1Padding")) {
                //  Minimum key size was 2048 when padding mode is "OAEPWithSHA-512AndMGF1Padding"
                keyPair = CryptoUtils.RSAKeyPair(2048);
            }
            try {
                CryptoAdaptor encryptProvider = CryptoUtils.RSAEncryptor(padding, keyPair.getPublic());
                String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                this.logger.info("Encrypt_Result", "RSA", "None", padding, encResult);
                CryptoAdaptor decryptProvider = CryptoUtils.RSADecryptor(padding, keyPair.getPrivate());
                decryptProvider.append(StringUtils.base64Decode(encResult));
                this.logger.info("Decrypt_Result", "RSA", "None", padding,
                        new String(decryptProvider.finish(), StandardCharsets.UTF_8));
            } catch (CryptoException e) {
                this.logger.warn("Crypto_Not_Support", "RSA", "RSA/None/" + padding);
            }
        }
        String randomString = StringUtils.randomString(128);
        CryptoAdaptor signProvider = CryptoUtils.RSASigner(keyPair.getPrivate());
        byte[] signBytes = signProvider.finish(randomString);
        this.logger.info("Signature_Result", randomString, "RSA", StringUtils.base64Encode(signBytes));
        CryptoAdaptor verifyProvider = CryptoUtils.RSAVerifier(keyPair.getPublic());
        verifyProvider.append(randomString);
        this.logger.info("Verify_Result", "RSA", verifyProvider.verify(signBytes));
    }

    @Test
    @Order(60)
    @Deprecated(since = "1.4.0")
    public void RC2() throws CryptoException {
        byte[] rc2Key = CryptoUtils.RC2Key();
        this.logger.info("Crypto_Key_Length", "RC2", StringUtils.base64Encode(rc2Key));
        for (String cipherMode : RC_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("RC2", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.RC2Encryptor(cipherMode, padding, rc2Key, iv);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "RC2", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.RC2Decryptor(cipherMode, padding, rc2Key, iv);
                    this.logger.info("Decrypt_Result", "RC2", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "RC2", "RC2/" + cipherMode + "/" + padding);
                }
            }
        }
    }

    @Test
    @Order(80)
    @Deprecated(since = "1.4.0")
    public void RC5() throws CryptoException {
        byte[] rc5Key = CryptoUtils.RC5Key();
        for (String cipherMode : RC_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("RC5", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.RC5Encryptor(cipherMode, padding, rc5Key, iv);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "RC5", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.RC5Decryptor(cipherMode, padding, rc5Key, iv);
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
        byte[] rc6Key = CryptoUtils.RC6Key();
        this.logger.info("Crypto_Key_Length", "RC6", StringUtils.base64Encode(rc6Key));
        for (String cipherMode : RC_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("RC6", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.RC6Encryptor(cipherMode, padding, rc6Key, iv);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "RC6", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.RC6Decryptor(cipherMode, padding, rc6Key, iv);
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
        byte[] blowfishKey = CryptoUtils.BlowfishKey();
        for (String cipherMode : RC_CIPHER_MODES) {
            for (String padding : DEFAULT_PADDINGS) {
                byte[] iv = CryptoUtils.generateIV("Blowfish", cipherMode);
                try {
                    CryptoAdaptor encryptProvider = CryptoUtils.BlowfishEncryptor(cipherMode, padding, blowfishKey, iv);
                    String encResult = StringUtils.base64Encode(encryptProvider.finish(ORIGINAL_STRING));
                    this.logger.info("Encrypt_Result", "Blowfish", cipherMode, padding, encResult);
                    CryptoAdaptor decryptProvider = CryptoUtils.BlowfishDecryptor(cipherMode, padding, blowfishKey, iv);
                    this.logger.info("Decrypt_Result", "Blowfish", cipherMode, padding,
                            new String(decryptProvider.finish(StringUtils.base64Decode(encResult)), StandardCharsets.UTF_8));
                } catch (CryptoException e) {
                    this.logger.warn("Crypto_Not_Support", "Blowfish", "Blowfish/" + cipherMode + "/" + padding);
                }
            }
        }
    }
}
