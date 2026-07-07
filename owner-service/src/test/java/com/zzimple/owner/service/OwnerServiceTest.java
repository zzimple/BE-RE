package com.zzimple.owner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzimple.common.exception.CustomException;
import com.zzimple.owner.client.UserClient;
import com.zzimple.owner.client.dto.UserSummaryResponse;
import com.zzimple.owner.dto.request.OwnerSignUpRequest;
import com.zzimple.owner.dto.response.MonthlySalesAvgResponse;
import com.zzimple.owner.dto.response.OwnerSignUpResponse;
import com.zzimple.owner.dto.response.WeeklySalesSimpleResponse;
import com.zzimple.owner.entity.Owner;
import com.zzimple.owner.entity.OwnerSalesRecord;
import com.zzimple.owner.event.OwnerEventPublisher;
import com.zzimple.owner.exception.BusinessErrorCode;
import com.zzimple.owner.repository.OwnerRepository;
import com.zzimple.owner.repository.OwnerSalesRecordRepository;
import com.zzimple.owner.repository.redis.BusinessRedisRepository;
import com.zzimple.owner.store.entity.Store;
import com.zzimple.owner.store.repository.StoreRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

  @Mock private OwnerRepository ownerRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private BusinessRedisRepository businessRedisRepository;
  @Mock private StoreRepository storeRepository;
  @Mock private OwnerSalesRecordRepository ownerSalesRecordRepository;
  @Mock private UserClient userClient;
  @Mock private OwnerEventPublisher ownerEventPublisher;

  @InjectMocks private OwnerService ownerService;

  private static OwnerSalesRecord record(long estimateNo, LocalDate moveDate, int price,
      String status) {
    return OwnerSalesRecord.builder()
        .estimateNo(estimateNo)
        .storeId(42L)
        .guestName("김고객")
        .finalTotalPrice(price)
        .moveDate(moveDate)
        .status(status)
        .build();
  }

  @Test
  @DisplayName("월별 평균 매출: read model 레코드를 월 단위로 그룹핑하고 평균을 계산한다")
  void monthlyAverageSales() {
    when(ownerSalesRecordRepository.findByStoreId(42L)).thenReturn(List.of(
        record(1L, LocalDate.of(2026, 6, 5), 100000, "CONFIRMED"),
        record(2L, LocalDate.of(2026, 6, 20), 300000, "COMPLETED"),
        record(3L, LocalDate.of(2026, 7, 1), 500000, "CONFIRMED")
    ));

    List<MonthlySalesAvgResponse> result = ownerService.getMonthlyAverageSales(42L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getMonth()).isEqualTo("2026-06");
    assertThat(result.get(0).getAverage()).isEqualTo(200000.0);
    assertThat(result.get(0).getItems()).hasSize(2);
    assertThat(result.get(1).getMonth()).isEqualTo("2026-07");
    assertThat(result.get(1).getAverage()).isEqualTo(500000.0);
  }

  @Test
  @DisplayName("주간 매출: 월요일 시작 주 단위로 그룹핑해 평균을 계산한다")
  void weeklySales() {
    // 2026-08-10(월), 2026-08-12(수) 같은 주 / 2026-08-17(월) 다른 주
    when(ownerSalesRecordRepository.findByStoreId(42L)).thenReturn(List.of(
        record(1L, LocalDate.of(2026, 8, 10), 100000, "CONFIRMED"),
        record(2L, LocalDate.of(2026, 8, 12), 200000, "CONFIRMED"),
        record(3L, LocalDate.of(2026, 8, 17), 400000, "CONFIRMED")
    ));

    List<WeeklySalesSimpleResponse> result = ownerService.getWeeklySales(42L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getWeekStartDate()).isEqualTo("2026-08-10");
    assertThat(result.get(0).getTotalAmount()).isEqualTo(150000);
    assertThat(result.get(1).getWeekStartDate()).isEqualTo("2026-08-17");
    assertThat(result.get(1).getTotalAmount()).isEqualTo(400000);
  }

  @Test
  @DisplayName("사장 회원가입: User 생성(Feign) -> Owner/Store 저장 -> upserted 이벤트 발행 순서로 처리된다")
  void registerOwnerSuccess() {
    OwnerSignUpRequest request = new OwnerSignUpRequest();
    request.setB_no("1234567890");
    request.setPassword("pw");
    request.setUserName("박사장");
    request.setPhoneNumber("010-0000-0000");
    request.setStoreName("찜플이사 강남점");
    request.setRoadFullAddr("서울 강남구 테헤란로 123");

    when(ownerRepository.findByBusinessNumber("1234567890")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("pw")).thenReturn("ENCODED");
    when(userClient.createUser(any())).thenReturn(
        new UserSummaryResponse(5L, "박사장", "1234567890", "010-0000-0000", "", "OWNER"));

    OwnerSignUpResponse response = ownerService.registerOwner(request);

    assertThat(response.isSuccess()).isTrue();
    verify(ownerRepository).save(any(Owner.class));
    verify(storeRepository).save(any(Store.class));
    verify(ownerEventPublisher).publishOwnerStoreUpserted(any(Owner.class), any(Store.class));
  }

  @Test
  @DisplayName("사장 회원가입: 중복 사업자번호면 User 생성 없이 거부된다")
  void registerOwnerRejectsDuplicate() {
    OwnerSignUpRequest request = new OwnerSignUpRequest();
    request.setB_no("1234567890");

    when(ownerRepository.findByBusinessNumber("1234567890"))
        .thenReturn(Optional.of(new Owner()));

    assertThatThrownBy(() -> ownerService.registerOwner(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(BusinessErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);

    verify(userClient, never()).createUser(any());
    verify(ownerEventPublisher, never()).publishOwnerStoreUpserted(any(), any());
  }
}
