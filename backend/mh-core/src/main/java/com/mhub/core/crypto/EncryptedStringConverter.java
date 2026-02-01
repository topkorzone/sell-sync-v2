package com.mhub.core.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptor aesEncryptor;

    public EncryptedStringConverter(AesEncryptor aesEncryptor) {
        this.aesEncryptor = aesEncryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : aesEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : aesEncryptor.decrypt(dbData);
    }
}
