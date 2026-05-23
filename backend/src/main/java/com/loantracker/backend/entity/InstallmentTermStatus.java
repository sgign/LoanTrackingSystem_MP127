package com.loantracker.backend.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InstallmentTermStatus {
    NOT_STARTED("NOT STARTED"),
    UNPAID("UNPAID"),
    PAID("PAID"),
    SKIPPED("SKIPPED"),
    DELINQUENT("DELINQUENT");

    private final String value;

    InstallmentTermStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static InstallmentTermStatus fromString(String text) {
        if (text == null) {
            return null;
        }
        for (InstallmentTermStatus b : InstallmentTermStatus.values()) {
            if (b.value.equalsIgnoreCase(text) || b.name().equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
