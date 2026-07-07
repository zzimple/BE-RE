package com.zzimple.estimate.guest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzimple.estimate.client.OwnerServiceClient;
import com.zzimple.estimate.client.UserServiceClient;
import com.zzimple.estimate.event.EstimateEventPublisher;
import com.zzimple.estimate.guest.entity.Estimate;
import com.zzimple.estimate.guest.enums.EstimateStatus;
import com.zzimple.estimate.guest.repository.MoveItemsRepository;
import com.zzimple.estimate.owner.entity.EstimateCalculation;
import com.zzimple.estimate.owner.entity.EstimateOwnerResponse;
import com.zzimple.estimate.owner.repository.EstimateCalculationRepository;
import com.zzimple.estimate.owner.repository.EstimateExtraChargeRepository;
import com.zzimple.estimate.owner.repository.EstimateOwnerResponseRepository;
import com.zzimple.estimate.owner.repository.EstimateRepository;
import com.zzimple.estimate.owner.repository.EstimateResponseRepository;
import com.zzimple.estimate.owner.repository.MoveItemExtraChargeRepository;
import com.zzimple.estimate.owner.repository.StorePriceSettingRepository;
import com.zzimple.estimate.view.StoreViewRepository;
import com.zzimple.estimate.view.UserView;
import com.zzimple.estimate.view.UserViewRepository;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GuestEstimateRespondTest {

  @Mock private EstimateRepository estimateRepository;
  @Mock private MoveItemsRepository moveItemsRepository;
  @Mock private EstimateExtraChargeRepository estimateExtraChargeRepository;
  @Mock private MoveItemExtraChargeRepository moveItemExtraChargeRepository;
  @Mock private OwnerServiceClient ownerServiceClient;
  @Mock private EstimateEventPublisher estimateEventPublisher;
  @Mock private StoreViewRepository storeViewRepository;
  @Mock private UserViewRepository userViewRepository;
  @Mock private UserServiceClient userServiceClient;
  @Mock private EstimateCalculationRepository estimateCalculationRepository;
  @Mock private StorePriceSettingRepository storePriceSettingRepository;
  @Mock private EstimateResponseRepository estimateResponseRepository;
  @Mock private EstimateOwnerResponseRepository estimateOwnerResponseRepository;

  @InjectMocks private GuestEstimateService guestEstimateService;

  private Estimate estimate(Long estimateNo, Long guestUserId) {
    Estimate estimate = new Estimate();
    ReflectionTestUtils.setField(estimate, "estimateNo", estimateNo);
    ReflectionTestUtils.setField(estimate, "userId", guestUserId);
    ReflectionTestUtils.setField(estimate, "moveDate", "20260810");
    ReflectionTestUtils.setField(estimate, "status", EstimateStatus.WAITING);
    return estimate;
  }

  @Test
  @DisplayName("견적 확정: 선택 매장은 CONFIRMED, 나머지는 REJECTED, estimate.confirmed 이벤트 발행")
  void respondConfirmsSelectedStoreAndPublishes() {
    Estimate est = estimate(100L, 7L);
    when(estimateRepository.findByEstimateNo(100L)).thenReturn(Optional.of(est));

    EstimateOwnerResponse selected = EstimateOwnerResponse.builder()
        .estimateNo(100L).storeId(42L).status(EstimateStatus.WAITING).build();
    EstimateOwnerResponse other = EstimateOwnerResponse.builder()
        .estimateNo(100L).storeId(99L).status(EstimateStatus.WAITING).build();
    when(estimateOwnerResponseRepository.findByEstimateNo(100L))
        .thenReturn(List.of(selected, other));

    EstimateCalculation calc = EstimateCalculation.builder().finalTotalPrice(550000).build();
    when(estimateCalculationRepository.findByEstimateNoAndStoreId(100L, 42L))
        .thenReturn(Optional.of(calc));
    when(userViewRepository.findById(7L)).thenReturn(Optional.of(
        UserView.builder().userId(7L).userName("김고객").build()));

    var result = guestEstimateService.respondToEstimate(100L, 42L, 7L);

    assertThat(result.getConfirmedStoreId()).isEqualTo(42L);
    assertThat(result.getStatus()).isEqualTo(EstimateStatus.CONFIRMED);
    assertThat(selected.getStatus()).isEqualTo(EstimateStatus.CONFIRMED);
    assertThat(other.getStatus()).isEqualTo(EstimateStatus.REJECTED);
    assertThat(est.getStatus()).isEqualTo(EstimateStatus.CONFIRMED);

    verify(estimateEventPublisher).publishEstimateConfirmed(
        eq(100L), eq(42L), eq(7L), eq("김고객"), eq("20260810"), any(), eq(550000));
  }

  @Test
  @DisplayName("견적 확정: 본인 견적이 아니면 거부되고 이벤트도 발행되지 않는다")
  void respondRejectsForeignUser() {
    Estimate est = estimate(100L, 7L);
    when(estimateRepository.findByEstimateNo(100L)).thenReturn(Optional.of(est));

    assertThatThrownBy(() -> guestEstimateService.respondToEstimate(100L, 42L, 999L))
        .isInstanceOf(AccessDeniedException.class);

    verify(estimateEventPublisher, never()).publishEstimateConfirmed(
        anyLong(), anyLong(), anyLong(), anyString(), anyString(), any(), any());
  }
}
