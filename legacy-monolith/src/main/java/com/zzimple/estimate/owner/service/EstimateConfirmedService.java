package com.zzimple.estimate.owner.service;

import com.zzimple.internal.OwnerServiceClient;
import java.util.Map;
import com.zzimple.estimate.guest.enums.EstimateStatus;
import com.zzimple.estimate.owner.dto.response.EstimateConfirmedResponse;
import com.zzimple.estimate.owner.dto.response.EstimateSummaryResponse;
import com.zzimple.estimate.owner.repository.EstimateRepository;
import com.zzimple.global.exception.CustomException;
import com.zzimple.global.exception.GlobalErrorCode;
import com.zzimple.user.entity.User;
import com.zzimple.user.exception.UserErrorCode;
import com.zzimple.user.repository.UserRepository;
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
  private final UserRepository userRepository;

  // 아예 견적서 확정된 코드
  public Page<EstimateConfirmedResponse> getConfirmedEstimates(Long storeId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return estimateRepository.findByStoreIdAndStatus(storeId, EstimateStatus.CONFIRMED, pageable)
        .map(estimate -> {
          Long userId = estimate.getUserId();
          String guestName = userRepository.findById(userId)
              .map(User::getUserName)
              .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

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
