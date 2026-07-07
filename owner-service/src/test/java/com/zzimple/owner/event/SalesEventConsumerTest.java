package com.zzimple.owner.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzimple.common.event.EstimateConfirmedEvent;
import com.zzimple.common.event.EstimateStatusChangedEvent;
import com.zzimple.owner.entity.OwnerSalesRecord;
import com.zzimple.owner.repository.OwnerSalesRecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesEventConsumerTest {

  @Mock private OwnerSalesRecordRepository ownerSalesRecordRepository;

  @InjectMocks private SalesEventConsumer salesEventConsumer;

  @Test
  @DisplayName("estimate.confirmed 소비: 신규 매출 레코드를 CONFIRMED 상태로 적재한다")
  void onEstimateConfirmedCreatesRecord() {
    when(ownerSalesRecordRepository.findById(100L)).thenReturn(Optional.empty());

    salesEventConsumer.onEstimateConfirmed(EstimateConfirmedEvent.builder()
        .eventId("e-1")
        .occurredAt(Instant.now())
        .estimateNo(100L)
        .storeId(42L)
        .guestUserId(7L)
        .guestName("김고객")
        .moveDate("20260810")
        .finalTotalPrice(550000)
        .build());

    ArgumentCaptor<OwnerSalesRecord> captor = ArgumentCaptor.forClass(OwnerSalesRecord.class);
    verify(ownerSalesRecordRepository).save(captor.capture());
    OwnerSalesRecord saved = captor.getValue();
    assertThat(saved.getEstimateNo()).isEqualTo(100L);
    assertThat(saved.getStoreId()).isEqualTo(42L);
    assertThat(saved.getGuestName()).isEqualTo("김고객");
    assertThat(saved.getFinalTotalPrice()).isEqualTo(550000);
    assertThat(saved.getMoveDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    assertThat(saved.getStatus()).isEqualTo("CONFIRMED");
  }

  @Test
  @DisplayName("estimate.status-changed 소비: 기존 레코드의 상태를 갱신한다")
  void onStatusChangedUpdatesRecord() {
    OwnerSalesRecord existing = OwnerSalesRecord.builder()
        .estimateNo(100L).storeId(42L).status("CONFIRMED").build();
    when(ownerSalesRecordRepository.findById(100L)).thenReturn(Optional.of(existing));

    salesEventConsumer.onEstimateStatusChanged(EstimateStatusChangedEvent.builder()
        .eventId("e-2")
        .occurredAt(Instant.now())
        .estimateNo(100L)
        .storeId(42L)
        .previousStatus("CONFIRMED")
        .newStatus("COMPLETED")
        .build());

    assertThat(existing.getStatus()).isEqualTo("COMPLETED");
    verify(ownerSalesRecordRepository).save(existing);
  }

  @Test
  @DisplayName("estimate.status-changed 소비: 없는 레코드는 무시한다")
  void onStatusChangedIgnoresMissing() {
    when(ownerSalesRecordRepository.findById(999L)).thenReturn(Optional.empty());

    salesEventConsumer.onEstimateStatusChanged(EstimateStatusChangedEvent.builder()
        .estimateNo(999L).newStatus("COMPLETED").build());

    verify(ownerSalesRecordRepository, never()).save(any());
  }
}
