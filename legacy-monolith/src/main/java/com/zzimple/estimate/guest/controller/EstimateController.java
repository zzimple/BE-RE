package com.zzimple.estimate.guest.controller;

import com.zzimple.estimate.guest.dto.response.EstimateListDetailResponse;
import com.zzimple.estimate.guest.entity.Estimate;
import com.zzimple.estimate.guest.service.GuestEstimateService;
import com.zzimple.estimate.owner.repository.EstimateRepository;
import com.zzimple.global.dto.BaseResponse;
import com.zzimple.global.exception.CustomException;
import com.zzimple.global.jwt.CustomUserDetails;
import com.zzimple.owner.entity.Owner;
import com.zzimple.owner.repository.OwnerRepository;
import com.zzimple.owner.store.entity.Store;
import com.zzimple.owner.store.repository.StoreRepository;
import com.zzimple.internal.StaffServiceClient;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/view")
public class EstimateController {

  private final GuestEstimateService guestEstimateService;
  private final OwnerRepository ownerRepository;
  private final StoreRepository storeRepository;
  private final EstimateRepository estimateRepository;
  private final StaffServiceClient staffServiceClient;

//  @GetMapping("/estimate/{estimateNo}")
//  @Operation(
//      summary = "[고객 | 토큰 O 사장님에게 온 견적서 상세 조회 + 사장 | 토큰 O 견적서 상세 조회]",
//      description = "견적서의 상세 정보를 조회합니다."
//  )
//  @PreAuthorize("hasAnyRole('CUSTOMER','OWNER')")
//  public ResponseEntity<BaseResponse<EstimateListDetailResponse>> getEstimateDetail(
//      @PathVariable Long estimateNo
//  ) {
//    EstimateListDetailResponse response = guestEstimateService.getEstimateDetail(estimateNo);
//    return ResponseEntity.ok(BaseResponse.success(response));
//  }

    @GetMapping("/stores/{storeId}/estimates/{estimateNo}")
//    @PreAuthorize("hasAnyRole('CUSTOMER','OWNER')")
    public ResponseEntity<BaseResponse<EstimateListDetailResponse>> getDetail(
        @PathVariable Long storeId,
        @PathVariable Long estimateNo,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
      boolean isOwner = user.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));

      boolean isStaff = user.getAuthorities().stream()
          .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

      if (isOwner) {
        // 사장님 권한 확인
        Store store = storeRepository.findByOwnerUserId(user.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Store not found"));
        if (!store.getId().equals(storeId)) {
          throw new AccessDeniedException("매장 접근 권한이 없습니다.");
        }

      } else if (isStaff) {
        // 직원 권한 확인 (staff 도메인은 staff-service로 추출됨 - 내부 API 호출)
        Map<String, Object> staff = staffServiceClient.findStaffByUserId(user.getUserId())
            .orElseThrow(() -> new AccessDeniedException("직원 정보가 없습니다."));

        Long staffStoreId = ((Number) staff.get("storeId")).longValue();
        if (!staffStoreId.equals(storeId)) {
          throw new AccessDeniedException("직원: 소속 매장이 아닙니다.");
        }

        // 해당 견적서에 배정된 사람인지 확인
        Long staffId = ((Number) staff.get("staffId")).longValue();
        if (!staffServiceClient.isAssigned(estimateNo, staffId)) {
          throw new AccessDeniedException("이 견적에 배정되지 않았습니다.");
        }

      } else {
        // 고객 권한 확인
        Estimate est = estimateRepository.findByEstimateNo(estimateNo)
            .orElseThrow(() -> new EntityNotFoundException("Estimate not found"));
        if (!est.getUserId().equals(user.getUserId())) {
          throw new AccessDeniedException("이 견적서에 접근 권한이 없습니다.");
        }
      }

      EstimateListDetailResponse dto =
          guestEstimateService.getEstimateDetail(estimateNo, storeId);
      return ResponseEntity.ok(BaseResponse.success(dto));
    }
  }
