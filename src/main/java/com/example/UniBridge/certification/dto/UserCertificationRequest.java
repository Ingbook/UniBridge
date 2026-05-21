package com.example.UniBridge.certification.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserCertificationRequest {

    private Long certificationId;
    private LocalDate acquiredDate;
}
