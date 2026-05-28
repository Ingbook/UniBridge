package com.example.UniBridge.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.UniBridge.certification.dto.CertificationResponse;
import com.example.UniBridge.certification.dto.UserCertificationRequest;
import com.example.UniBridge.certification.dto.UserCertificationResponse;
import com.example.UniBridge.certification.entity.Certification;
import com.example.UniBridge.certification.entity.UserCertification;
import com.example.UniBridge.certification.repository.CertificationRepository;
import com.example.UniBridge.certification.repository.UserCertificationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CertificationServiceTest {

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private UserCertificationRepository userCertificationRepository;

    @InjectMocks
    private CertificationService certificationService;

    @Test
    void getCertifications_returnsOrderedCertificationResponses() {
        Certification certification = certification("SQLD", "DATABASE", 20);
        when(certificationRepository.findAllByOrderByCategoryAscNameAsc()).thenReturn(List.of(certification));

        List<CertificationResponse> responses = certificationService.getCertifications();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("SQLD");
        assertThat(responses.get(0).getCategory()).isEqualTo("DATABASE");
        assertThat(responses.get(0).getScore()).isEqualTo(20);
    }

    @Test
    void getMyCertifications_returnsUserCertificationResponses() {
        Certification certification = certification("정보처리기사", "BACKEND", 30);
        UserCertification userCertification = userCertification(certification, LocalDate.of(2025, 1, 10));
        when(userCertificationRepository.findByUserId(1L)).thenReturn(List.of(userCertification));

        List<UserCertificationResponse> responses = certificationService.getMyCertifications();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("정보처리기사");
        assertThat(responses.get(0).getAcquiredDate()).isEqualTo(LocalDate.of(2025, 1, 10));
    }

    @Test
    void addMyCertification_savesUserCertification() {
        Certification certification = certification("정보처리기사", "BACKEND", 30);
        UserCertificationRequest request = userCertificationRequest(1L, LocalDate.of(2025, 1, 10));
        when(certificationRepository.findById(1L)).thenReturn(Optional.of(certification));
        when(userCertificationRepository.existsByUserIdAndCertificationId(1L, 1L)).thenReturn(false);
        when(userCertificationRepository.save(any(UserCertification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserCertificationResponse response = certificationService.addMyCertification(request);

        assertThat(response.getCertificationId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("정보처리기사");
        verify(userCertificationRepository).save(any(UserCertification.class));
    }

    @Test
    void addMyCertification_throwsException_whenCertificationIdIsNull() {
        UserCertificationRequest request = userCertificationRequest(null, LocalDate.of(2025, 1, 10));

        assertThatThrownBy(() -> certificationService.addMyCertification(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자격증 ID를 입력해 주세요.");
    }

    @Test
    void addMyCertification_throwsException_whenCertificationDoesNotExist() {
        UserCertificationRequest request = userCertificationRequest(999L, LocalDate.of(2025, 1, 10));
        when(certificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificationService.addMyCertification(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 자격증입니다.");
    }

    @Test
    void addMyCertification_throwsException_whenAlreadyRegistered() {
        Certification certification = certification("정보처리기사", "BACKEND", 30);
        UserCertificationRequest request = userCertificationRequest(1L, LocalDate.of(2025, 1, 10));
        when(certificationRepository.findById(1L)).thenReturn(Optional.of(certification));
        when(userCertificationRepository.existsByUserIdAndCertificationId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> certificationService.addMyCertification(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 자격증입니다.");
    }

    @Test
    void deleteMyCertification_deletesByCurrentUserIdAndCertificationId() {
        certificationService.deleteMyCertification(1L);

        verify(userCertificationRepository).deleteByUserIdAndCertificationId(1L, 1L);
    }

    private Certification certification(String name, String category, Integer score) {
        Certification certification = Certification.builder()
                .name(name)
                .category(category)
                .score(score)
                .description("설명")
                .build();
        ReflectionTestUtils.setField(certification, "id", 1L);
        return certification;
    }

    private UserCertification userCertification(Certification certification, LocalDate acquiredDate) {
        UserCertification userCertification = UserCertification.builder()
                .userId(1L)
                .certification(certification)
                .acquiredDate(acquiredDate)
                .build();
        ReflectionTestUtils.setField(userCertification, "id", 1L);
        return userCertification;
    }

    private UserCertificationRequest userCertificationRequest(Long certificationId, LocalDate acquiredDate) {
        UserCertificationRequest request = new UserCertificationRequest();
        ReflectionTestUtils.setField(request, "certificationId", certificationId);
        ReflectionTestUtils.setField(request, "acquiredDate", acquiredDate);
        return request;
    }
}
