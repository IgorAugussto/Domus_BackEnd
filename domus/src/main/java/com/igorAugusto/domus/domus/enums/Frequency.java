package com.igorAugusto.domus.domus.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Frequency {
    MONTHLY("Monthly"),
    ONE_TIME("One-time");

    private final String value;

    Frequency(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Frequency fromValue(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(f -> f.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Frequência inválida: " + value));
    }
}
