package com.zzimple.estimate.owner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzimple.estimate.event.EstimateEventPublisher;
import com.zzimple.estimate.guest.entity.Estimate;
import com.zzimple.estimate.guest.enums.EstimateStatus;
import com.zzimple.estimate.owner.repository.EstimateRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EstimateStatusSchedulerServiceTest {

  @Mock private EstimateRepository estimateRepository;
  @Mock private EstimateEventPublisher estimateEventPublisher;

  @InjectMocks private EstimateStatusSchedulerService schedulerService;

  private Estimate confirmed(Long estimateNo, String moveDate) {
    Estimate estimate = new Estimate();
    ReflectionTestUtils.setField(estimate, "estimateNo", estimateNo);
    ReflectionTestUtils.setField(estimate, "storeId", 42L);
    ReflectionTestUtils.setField(estimate, "moveDate", moveDate);
    ReflectionTestUtils.setField(estimate, "status", EstimateStatus.CONFIRMED);
    return estimate;
  }

  @Test
  @DisplayName("자동 완료: 이사일이 지난 확정 견적은 COMPLETED로 바뀌고 status-changed 이벤트가 발행된다")
  void autoCompletePastEstimates() {
    Estimate past = confirmed(100L, LocalDate.now().minusDays(1).toString());
    Estimate future = confirmed(200L, LocalDate.now().plusDays(3).toString());
    when(estimateRepository.findByStatus(EstimateStatus.CONFIRMED))
        .thenReturn(List.of(past, future));

    schedulerService.autoCompletePastEstimates();

    assertThat(past.getStatus()).isEqualTo(EstimateStatus.COMPLETED);
    assertThat(future.getStatus()).isEqualTo(EstimateStatus.CONFIRMED);
    verify(estimateEventPublisher).publishStatusChanged(
        100L, 42L, "CONFIRMED", "COMPLETED");
    verify(estimateEventPublisher, never()).publishStatusChanged(
        org.mockito.ArgumentMatchers.eq(200L), anyLong(), anyString(), anyString());
  }
}
