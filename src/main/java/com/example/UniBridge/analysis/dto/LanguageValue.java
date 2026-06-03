package com.example.UniBridge.analysis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LanguageValue {

    private String type;
    private Integer score;
}
