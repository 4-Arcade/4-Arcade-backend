package com.fourarcade.arcadebackend.quiz;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class QuizCategoryConverter implements AttributeConverter<QuizCategory, String> {

    @Override
    public String convertToDatabaseColumn(QuizCategory attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public QuizCategory convertToEntityAttribute(String dbData) {
        return dbData == null ? null : QuizCategory.from(dbData);
    }
}