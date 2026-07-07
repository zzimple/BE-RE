package com.zzimple.staff.service;

import com.zzimple.common.enums.UserRole;
import com.zzimple.common.exception.CustomException;
import com.zzimple.common.exception.GlobalErrorCode;
import com.zzimple.staff.client.OwnerClient;
import com.zzimple.staff.client.UserClient;
import com.zzimple.staff.client.dto.OwnerSummaryResponse;
import com.zzimple.staff.client.dto.StoreSummaryResponse;
import com.zzimple.staff.client.dto.UserSummaryResponse;
import com.zzimple.staff.dto.request.StaffsendApprovalRequest;
import com.zzimple.staff.dto.response.StaffListResponse;
import com.zzimple.staff.dto.response.StaffProfileResponse;
import com.zzimple.staff.dto.response.StaffsendApprovalResponse;
import com.zzimple.staff.entity.Staff;
import com.zzimple.staff.enums.Status;
import com.zzimple.staff.exception.StaffErrorCode;
import com.zzimple.staff.repository.StaffRepository;
import feign.FeignException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

  private final StaffRepository staffRepository;
  private final UserClient userClient;
  private final OwnerClient ownerClient;

  // 사장님께 승인 요청
  @Transactional
  public StaffsendApprovalResponse requestApproval(Long userId, StaffsendApprovalRequest request) {

    // 실제 유저를 조회하여 STAFF 권한 확인 (auth 도메인 - Feign)
    UserSummaryResponse user = fetchUser(userId);

    if (!UserRole.STAFF.name().equals(user.getRole())) {
      log.warn("[승인 요청 실패] STAFF 권한 아님 - userId: {}", userId);
      throw new CustomException(StaffErrorCode.INVALID_STAFF_ROLE);
    }

    // 사장님 확인 (전화번호로 조회)
    UserSummaryResponse ownerUser;
    try {
      ownerUser = userClient.getUserByPhone(request.getOwnerPhoneNumber());
    } catch (FeignException.NotFound e) {
      log.warn("[승인 요청 실패] 존재하지 않는 사장님 전화번호: {}", request.getOwnerPhoneNumber());
      throw new CustomException(StaffErrorCode.OWNER_NOT_FOUND);
    }

    if (!UserRole.OWNER.name().equals(ownerUser.getRole())) {
      log.warn("[승인 요청 실패] 유저 ID {} - OWNER 권한 아님 (현재: {})",
          ownerUser.getId(), ownerUser.getRole());
      throw new CustomException(StaffErrorCode.INVALID_OWNER_ROLE);
    }

    // 사장/매장 정보 (owner 도메인 - Feign)
    OwnerSummaryResponse owner;
    try {
      owner = ownerClient.getOwnerByUserId(ownerUser.getId());
    } catch (FeignException.NotFound e) {
      log.warn("[승인 요청 실패] 유저 ID {} - Owner 정보 없음", ownerUser.getId());
      throw new CustomException(StaffErrorCode.OWNER_NOT_FOUND);
    }

    StoreSummaryResponse store = fetchStoreByOwnerId(owner.getOwnerId());

    // 이미 요청된 관계
    if (staffRepository.existsByUserIdAndOwnerId(userId, ownerUser.getId())) {
      log.warn("[승인 요청 실패] 이미 요청된 관계 - staffId: {}, ownerId: {}", userId, ownerUser.getId());
      throw new CustomException(StaffErrorCode.APPROVAL_ALREADY_REQUESTED);
    }

    Staff staff = Staff.builder()
        .userId(userId)
        .ownerId(owner.getOwnerId())
        .storeId(store.getStoreId())
        .status(Status.PENDING)
        .build();

    staffRepository.save(staff);

    log.info("[승인 요청 성공] Staff ID: {}, Owner ID: {}, Store ID: {}",
        userId, owner.getOwnerId(), store.getStoreId());

    return new StaffsendApprovalResponse(true);
  }

  // 사장님 승인 메소드
  // storeId는 게이트웨이가 JWT 클레임에서 추출해 전달한 값 (기존: DB에서 재조회)
  @Transactional
  public Status approveStaff(Long staffId, Status status, Long storeId) {

    Staff staff = staffRepository.findById(staffId)
        .orElseThrow(() -> {
          log.warn("[승인 실패] 존재하지 않는 직원 - staffId: {}", staffId);
          return new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
        });

    if (!staff.getStoreId().equals(storeId)) {
      log.warn("[승인 실패] 소속 사장님 불일치 - 요청자 (가게 번호): {}, 직원이 요청한 가게 아이디: {}",
          storeId, staff.getStoreId());
      throw new CustomException(StaffErrorCode.STAFF_OWNER_MISMATCH);
    }

    if (status == Status.REJECTED) {
      log.info("[승인 처리] 요청 거절 - staffId: {}", staffId);
      staffRepository.delete(staff);
      return Status.REJECTED;
    }

    staff.setStatus(Status.APPROVED);
    staffRepository.save(staff);

    log.info("[승인 처리] 요청 승인 완료 - staffId: {}", staffId);
    return Status.APPROVED;
  }

  public List<StaffListResponse> getStaffListByOwner(Long storeId) {
    List<Staff> staffList = staffRepository.findByStoreId(storeId);

    if (staffList.isEmpty()) {
      return List.of();
    }

    // 사용자 표시 정보 배치 조회 (N+1 Feign 호출 방지)
    List<Long> userIds = staffList.stream().map(Staff::getUserId).toList();
    Map<Long, UserSummaryResponse> userMap = userClient.getUsersByIds(userIds).stream()
        .collect(Collectors.toMap(UserSummaryResponse::getId, Function.identity()));

    return staffList.stream()
        .map(staff -> {
          UserSummaryResponse user = userMap.get(staff.getUserId());
          if (user == null) {
            throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
          }
          return StaffListResponse.from(staff, user);
        })
        .toList();
  }

  // 직원프로필 조회
  public StaffProfileResponse getStaffProfile(Long userId) {

    // 1. 직원 유저 정보 (auth 도메인)
    UserSummaryResponse staffUser = fetchUser(userId);

    // 2. 직원 정보 (로컬)
    Staff staff = staffRepository.findByUserId(userId)
        .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    // 3. 사장님 정보 (owner 도메인)
    OwnerSummaryResponse owner;
    try {
      owner = ownerClient.getOwner(staff.getOwnerId());
    } catch (FeignException.NotFound e) {
      throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    // 4. 사장님 유저 정보 (auth 도메인)
    UserSummaryResponse ownerUser = fetchUser(owner.getUserId());

    // 5. 가게 정보 (owner 도메인)
    StoreSummaryResponse store = fetchStoreByOwnerId(owner.getOwnerId());

    return new StaffProfileResponse(
        staffUser.getUserName(),
        staffUser.getLoginId(),
        staffUser.getEmail(),
        ownerUser.getUserName(),
        store.getName(),
        ownerUser.getPhoneNumber()
    );
  }

  private UserSummaryResponse fetchUser(Long userId) {
    try {
      return userClient.getUser(userId);
    } catch (FeignException.NotFound e) {
      throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  private StoreSummaryResponse fetchStoreByOwnerId(Long ownerId) {
    try {
      return ownerClient.getStoreByOwnerId(ownerId);
    } catch (FeignException.NotFound e) {
      throw new CustomException(StaffErrorCode.OWNER_NOT_FOUND);
    }
  }
}
