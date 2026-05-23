package com.loantracker.backend.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InstallmentTermStatusConverter implements AttributeConverter<InstallmentTermStatus, String> {

    @Override
    public String convertToDatabaseColumn(InstallmentTermStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public InstallmentTermStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InstallmentTermStatus.fromString(dbData);
    }
}
