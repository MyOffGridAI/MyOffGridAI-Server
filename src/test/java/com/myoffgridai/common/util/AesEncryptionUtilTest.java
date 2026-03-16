package com.myoffgridai.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AesEncryptionUtil}.
 */
class AesEncryptionUtilTest {

    private static final String TEST_HEX_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private AesEncryptionUtil util;

    @BeforeEach
    void setUp() {
        util = new AesEncryptionUtil(TEST_HEX_KEY);
    }

    @Test
    void encryptDecrypt_roundTrip() {
        String plaintext = "sk-ant-api03-test-key-1234567890";
        String encrypted = util.encrypt(plaintext);

        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);

        String decrypted = util.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptDecrypt_emptyString() {
        String encrypted = util.encrypt("");
        assertNotNull(encrypted);

        String decrypted = util.decrypt(encrypted);
        assertEquals("", decrypted);
    }

    @Test
    void encrypt_nullReturnsNull() {
        assertNull(util.encrypt(null));
    }

    @Test
    void decrypt_nullReturnsNull() {
        assertNull(util.decrypt(null));
    }

    @Test
    void encryptDecrypt_longContent() {
        String longText = "A".repeat(10000);
        String encrypted = util.encrypt(longText);
        String decrypted = util.decrypt(encrypted);
        assertEquals(longText, decrypted);
    }

    @Test
    void encrypt_producesUniqueOutputsForSamePlaintext() {
        String plaintext = "same-key-different-iv";
        String encrypted1 = util.encrypt(plaintext);
        String encrypted2 = util.encrypt(plaintext);

        // Different IVs should produce different ciphertexts
        assertNotEquals(encrypted1, encrypted2);

        // Both should decrypt to the same value
        assertEquals(plaintext, util.decrypt(encrypted1));
        assertEquals(plaintext, util.decrypt(encrypted2));
    }

    @Test
    void constructor_invalidKeyLength_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new AesEncryptionUtil("tooShort"));
    }

    @Test
    void encryptDecrypt_unicodeContent() {
        String unicode = "日本語テスト 🔐 ñoño café";
        String encrypted = util.encrypt(unicode);
        assertEquals(unicode, util.decrypt(encrypted));
    }
}
