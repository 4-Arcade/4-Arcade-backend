package com.fourarcade.arcadebackend.quiz;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QuizCategory {
    K_POP("K-POP"),
    POP("POP"),
    OST("OST"),
    GAME_MUSIC("게임음악"),
    ETC("기타");

    private final String value;

    QuizCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static QuizCategory from(String value) {
        for (QuizCategory category : values()) {
            if (category.value.equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 카테고리입니다.");
    }
}