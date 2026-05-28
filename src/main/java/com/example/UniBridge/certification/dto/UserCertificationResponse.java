package com.example.UniBridge.certification.dto;

import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCertificationResponse {

    private Long userCertificationId;
    private Long certificationId;
    private String name;
    private String category;
    private Integer score;
    private LocalDate acquiredDate;

    public static UserCertificationResponse from(UserCertification userCertification) {
        Certification certification = userCertification.getCertification();
        return UserCertificationResponse.builder()
                .userCertificationId(userCertification.getId())
                .certificationId(certification.getId())
                .name(certification.getName())
                .category(certification.getCategory())
                .score(certification.getScore())
                .acquiredDate(userCertification.getAcquiredDate())
                .build();
    }
}
