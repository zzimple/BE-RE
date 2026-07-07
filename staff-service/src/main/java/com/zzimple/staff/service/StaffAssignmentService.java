package com.zzimple.staff.service;

import com.zzimple.common.exception.CustomException;
import com.zzimple.staff.client.EstimateClient;
import com.zzimple.staff.client.UserClient;
import com.zzimple.staff.client.dto.EstimateConfirmedSummaryResponse;
import com.zzimple.staff.client.dto.EstimateSummaryResponse;
import com.zzimple.staff.client.dto.UserSummaryResponse;
import com.zzimple.staff.dto.response.AssignStaffDateResponse;
import com.zzimple.staff.dto.response.AssignedStaffListResponse;
import com.zzimple.staff.dto.response.AvailableStaffResponse;
import com.zzimple.staff.dto.response.StaffAssignmentResponse;
import com.zzimple.staff.entity.Staff;
import com.zzimple.staff.entity.StaffAssignment;
import com.zzimple.staff.enums.Status;
import com.zzimple.staff.exception.StaffErrorCode;
import com.zzimple.staff.repository.StaffAssignmentRepository;
import com.zzimple.staff.repository.StaffRepository;
import com.zzimple.staff.repository.StaffTimeOffepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
public class StaffAssignmentService {

  private final StaffRepository staffRepository;
  private final StaffAssignmentRepository staffAssignmentRepository;
  private final StaffTimeOffepository staffTimeOffepository;
  private final EstimateClient estimateClient;
  private final UserClient userClient;

  private static final DateTimeFormatter ESTIMATE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private static final String CONFIRMED = "CONFIRMED";

  @Transactional
  public List<AssignStaffDateResponse> assignWithDate(Long estimateNo, List<Long> staffIds) {
    // 0) 직원 ID 목록 검증
    if (staffIds == null || staffIds.isEmpty()) {
      throw new IllegalArgumentException("적어도 한 명의 직원 ID가 필요합니다.");
    }

    // 1) Estimate 상태 확인 - 배정 시점의 '현재' 확정 상태가 필요하므로 동기 Feign 호출
    EstimateSummaryResponse estimate = fetchEstimate(estimateNo);

    if (!CONFIRMED.equals(estimate.getStatus())) {
      throw new IllegalStateException("아직 고객이 견적을 확정하지 않았습니다.");
    }

    // 2) 사장 확정 응답 존재 확인
    fetchConfirmedResponse(estimateNo);

    // 3) 각 직원 배정 처리
    List<AssignStaffDateResponse> results = new ArrayList<>();
    for (Long staffId : staffIds) {
      // 3-1) Staff 조회 및 중복 확인 (로컬)
      Staff staff = staffRepository.findByStaffId(staffId)
          .orElseThrow(() -> new EntityNotFoundException("직원 없음 staffId=" + staffId));
      staffAssignmentRepository.findByEstimateNoAndStaffId(estimateNo, staffId)
          .ifPresent(a -> { throw new IllegalStateException("이미 배정된 직원입니다."); });

      // 3-2) 작업 날짜 파싱
      LocalDate workDate = LocalDate.parse(estimate.getMoveDate(), ESTIMATE_DATE_FORMAT);

      // 3-3) 직원 이름 조회 (auth 도메인 - Feign)
      UserSummaryResponse user;
      try {
        user = userClient.getUser(staff.getUserId());
      } catch (FeignException.NotFound e) {
        throw new EntityNotFoundException("유저 없음 id=" + staff.getUserId());
      }
      String staffName = user.getUserName();

      // 3-4) Assignment 생성 및 저장
      StaffAssignment assignment = StaffAssignment.builder()
          .estimateNo(estimateNo)
          .staffId(staffId)
          .staffName(staffName)
          .workDate(workDate)
          .build();
      staffAssignmentRepository.save(assignment);

      results.add(AssignStaffDateResponse.builder()
          .staffId(staffId)
          .staffName(staffName)
          .workDate(workDate)
          .build());
    }
    return results;
  }

  /**
   * 배정 가능 직원 목록.
   * ownerId는 게이트웨이가 JWT 클레임에서 추출해 전달한 값이다.
   * (모놀리스 시절에는 userId로 Owner를 재조회했지만, 토큰에 이미 ownerId가 있다)
   */
  @Transactional(readOnly = true)
  public List<AvailableStaffResponse> getAvailableStaffList(Long estimateNo, Long ownerId) {

    // 1. 견적의 이사 날짜 조회 (estimate 도메인 - Feign)
    EstimateSummaryResponse estimate = fetchEstimate(estimateNo);
    LocalDate workDate = LocalDate.parse(estimate.getMoveDate(), ESTIMATE_DATE_FORMAT);
    log.info("[배정 가능 직원 조회] estimateNo={}, workDate={}", estimateNo, workDate);

    // 2. 승인된 직원 전체 (로컬)
    List<Staff> approvedStaff = staffRepository.findByOwnerIdAndStatus(ownerId, Status.APPROVED);

    // 3. 휴가자 제외
    List<Staff> workingStaff = approvedStaff.stream()
        .filter(s -> !isOnTimeOff(s.getStaffId(), workDate))
        .toList();

    // 4. 이미 배정된 직원 ID
    List<Long> busyStaffIds = staffAssignmentRepository
        .findByWorkDate(workDate)
        .stream()
        .map(StaffAssignment::getStaffId)
        .toList();

    // 5. 최종 배정 가능 직원
    List<Staff> availableStaff = workingStaff.stream()
        .filter(s -> !busyStaffIds.contains(s.getStaffId()))
        .toList();

    if (availableStaff.isEmpty()) {
      return List.of();
    }

    // 6. User 표시 정보 배치 조회 (auth 도메인 - Feign, N+1 방지)
    List<Long> userIds = availableStaff.stream()
        .map(Staff::getUserId)
        .toList();
    Map<Long, UserSummaryResponse> userMap = userClient.getUsersByIds(userIds)
        .stream().collect(Collectors.toMap(UserSummaryResponse::getId, Function.identity()));

    // 7. DTO 변환
    return availableStaff.stream()
        .map(s -> {
          UserSummaryResponse u = userMap.get(s.getUserId());
          return AvailableStaffResponse.builder()
              .staffId(s.getStaffId())
              .staffName(u != null ? u.getUserName() : "이름 없음")
              .staffPhoneNum(u != null ? u.getPhoneNumber() : "번호 없음")
              .status("AVAILABLE")
              .build();
        })
        .toList();
  }

  // staffId 가 workDate에 휴가(승인된 TimeOff) 중인지 체크
  private boolean isOnTimeOff(Long staffId, LocalDate workDate) {
    return staffTimeOffepository.existsByStaffIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        staffId,
        Status.APPROVED,
        workDate,
        workDate
    );
  }

  /**
   * 배정된 직원 목록.
   * storeId는 게이트웨이가 JWT 클레임에서 추출해 전달한 값으로,
   * 확정 응답의 storeId와 비교해 소유권을 검증한다.
   */
  @Transactional(readOnly = true)
  public AssignedStaffListResponse getAssignedStaffList(Long estimateNo, Long storeId) {
    // 1) CONFIRMED된 견적 응답 조회 (estimate 도메인 - Feign)
    EstimateConfirmedSummaryResponse confirmed = fetchConfirmedResponse(estimateNo);

    // 2) 권한 검증: 확정 응답의 storeId와 요청자 storeId 비교
    if (!confirmed.getStoreId().equals(storeId)) {
      throw new CustomException(StaffErrorCode.STAFF_OWNER_MISMATCH);
    }

    // 3) estimateNo로 배정 내역 조회 (로컬)
    List<StaffAssignment> assignments =
        staffAssignmentRepository.findByEstimateNo(estimateNo);

    List<StaffAssignmentResponse> staffList = assignments.stream()
        .map(sa -> StaffAssignmentResponse.builder()
            .staffId(sa.getStaffId())
            .staffName(sa.getStaffName())
            .workDate(sa.getWorkDate())
            .estimateNo(sa.getEstimateNo())
            .build())
        .toList();

    return AssignedStaffListResponse.builder()
        .count(staffList.size())
        .staffList(staffList)
        .build();
  }

  private EstimateSummaryResponse fetchEstimate(Long estimateNo) {
    try {
      return estimateClient.getEstimate(estimateNo);
    } catch (FeignException.NotFound e) {
      throw new EntityNotFoundException("견적 없음 id=" + estimateNo);
    }
  }

  private EstimateConfirmedSummaryResponse fetchConfirmedResponse(Long estimateNo) {
    try {
      return estimateClient.getConfirmedResponse(estimateNo);
    } catch (FeignException.NotFound e) {
      throw new EntityNotFoundException("사장 확정 응답 없음 estimateNo=" + estimateNo);
    }
  }
}
