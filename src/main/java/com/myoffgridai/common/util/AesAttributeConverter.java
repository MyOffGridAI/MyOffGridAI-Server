package com.myoffgridai.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA attribute converter that transparently encrypts/decrypts string
 * columns using {@link AesEncryptionUtil}.
 *
 * <p>Apply with {@code @Convert(converter = AesAttributeConverter.class)}
 * on entity fields that should be stored encrypted at rest.</p>
 */
@Converter
@Component
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private final AesEncryptionUtil encryptionUtil;

    /**
     * Constructs the converter with the application-wide encryption utility.
     *
     * @param encryptionUtil the AES encryption utility
     */
    public AesAttributeConverter(AesEncryptionUtil encryptionUtil) {
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionUtil.decrypt(dbData);
    }
}
