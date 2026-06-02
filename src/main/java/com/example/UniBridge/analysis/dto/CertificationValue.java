package com.example.UniBridge.analysis.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationValue {

    private List<String> items;
    private Integer count;
}
