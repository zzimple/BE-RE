package com.zzimple.estimate.owner.service;

import com.zzimple.estimate.client.OwnerServiceClient;
import java.util.Map;
import com.zzimple.estimate.guest.enums.EstimateStatus;
import com.zzimple.estimate.owner.dto.response.EstimateConfirmedResponse;
import com.zzimple.estimate.owner.dto.response.EstimateSummaryResponse;
import com.zzimple.estimate.owner.repository.EstimateRepository;
import com.zzimple.common.exception.CustomException;
import com.zzimple.common.exception.GlobalErrorCode;
import com.zzimple.estimate.view.UserView;
import com.zzimple.estimate.view.UserViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstimateConfirmedService {

  private final EstimateRepository estimateRepository;
  private final OwnerServiceClient ownerServiceClient;
  private final UserViewRepository userViewRepository;

  // 아예 견적서 확정된 코드
  public Page<EstimateConfirmedResponse> getConfirmedEstimates(Long storeId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return estimateRepository.findByStoreIdAndStatus(storeId, EstimateStatus.CONFIRMED, pageable)
        .map(estimate -> {
          Long userId = estimate.getUserId();
          // 표시용 게스트 이름은 user_view read model에서 조회 (목록 N+1 Feign 제거)
          String guestName = userViewRepository.findById(userId)
              .map(UserView::getUserName)
              .orElse("알 수 없음");

          return EstimateConfirmedResponse.from(estimate, guestName);
        });
  }

  // 확정 / 진행 / 수락
  public EstimateSummaryResponse getEstimateSummary(Long userId) {
    log.info("🟡 [getEstimateSummary] 요청 유저 ID: {}", userId);

    Map<String, Object> store = ownerServiceClient.getStoreByOwnerUserId(userId)
        .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    Long storeId = OwnerServiceClient.asLong(store, "storeId");
    log.info("🟢 매핑된 Store ID: {}", storeId);


    int confirmedCount = estimateRepository.countByStoreIdAndStatus(storeId, EstimateStatus.CONFIRMED);
    int completedCount = estimateRepository.countByStoreIdAndStatus(storeId, EstimateStatus.COMPLETED);
    int inProgressCount = confirmedCount - completedCount; // 진행 중인 견적서

    return new EstimateSummaryResponse(confirmedCount, completedCount, inProgressCount);
  }
}
