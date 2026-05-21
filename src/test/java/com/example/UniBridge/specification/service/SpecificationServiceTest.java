package com.example.UniBridge.specification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.UniBridge.specification.dto.SpecificationRequest;
import com.example.UniBridge.specification.dto.SpecificationResponse;
import com.example.UniBridge.specification.entity.Specification;
import com.example.UniBridge.specification.repository.SpecificationRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpecificationServiceTest {

    @Mock
    private SpecificationRepository specificationRepository;

    @InjectMocks
    private SpecificationService specificationService;

    @Test
    void saveMySpecification_createsSpecification_whenSpecificationDoesNotExist() {
        SpecificationRequest request = specificationRequest("4.1", "4.5");
        when(specificationRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(specificationRepository.save(any(Specification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SpecificationResponse response = specificationService.saveMySpecification(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getGpa()).isEqualByComparingTo("4.1");
        assertThat(response.getMaxGpa()).isEqualByComparingTo("4.5");
        verify(specificationRepository).save(any(Specification.class));
    }

    @Test
    void saveMySpecification_updatesSpecification_whenSpecificationExists() {
        Specification specification = Specification.builder()
                .userId(1L)
                .gpa(BigDecimal.valueOf(3.0))
                .maxGpa(BigDecimal.valueOf(4.5))
                .build();
        SpecificationRequest request = specificationRequest("3.8", "4.5");
        when(specificationRepository.findByUserId(1L)).thenReturn(Optional.of(specification));
        when(specificationRepository.save(specification)).thenReturn(specification);

        SpecificationResponse response = specificationService.saveMySpecification(request);

        assertThat(response.getGpa()).isEqualByComparingTo("3.8");
        assertThat(response.getMaxGpa()).isEqualByComparingTo("4.5");
        verify(specificationRepository).save(specification);
    }

    @Test
    void getMySpecification_throwsException_whenSpecificationDoesNotExist() {
        when(specificationRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specificationService.getMySpecification())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("등록된 스펙 정보가 없습니다.");
    }

    @Test
    void getMySpecificationEntityForAnalysis_throwsException_whenSpecificationDoesNotExist() {
        when(specificationRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specificationService.getMySpecificationEntityForAnalysis())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("분석을 진행하려면 먼저 학점 정보를 등록해야 합니다.");
    }

    @Test
    void validateGpa_throwsException_whenGpaIsInvalid() {
        assertThatThrownBy(() -> specificationService.validateGpa(null, BigDecimal.valueOf(4.5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학점을 입력해 주세요.");
        assertThatThrownBy(() -> specificationService.validateGpa(BigDecimal.valueOf(3.0), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 학점은 0보다 커야 합니다.");
        assertThatThrownBy(() -> specificationService.validateGpa(BigDecimal.valueOf(-1), BigDecimal.valueOf(4.5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학점은 0 이상이어야 합니다.");
        assertThatThrownBy(() -> specificationService.validateGpa(BigDecimal.valueOf(4.6), BigDecimal.valueOf(4.5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("학점은 최대 학점보다 클 수 없습니다.");
    }

    private SpecificationRequest specificationRequest(String gpa, String maxGpa) {
        SpecificationRequest request = new SpecificationRequest();
        ReflectionTestUtils.setField(request, "gpa", new BigDecimal(gpa));
        ReflectionTestUtils.setField(request, "maxGpa", new BigDecimal(maxGpa));
        return request;
    }
}
