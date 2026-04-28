package com.igorAugusto.domus.domus.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class FrequencyConverter implements AttributeConverter<Frequency, String> {

    @Override
    public String convertToDatabaseColumn(Frequency frequency) {
        return frequency == null ? null : frequency.getValue();
    }

    @Override
    public Frequency convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return Arrays.stream(Frequency.values())
                .filter(f -> f.getValue().equals(dbData))
                .findFirst()
                .orElse(null);
    }
}
