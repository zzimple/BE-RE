package com.zzimple.staff.service;

import com.zzimple.common.dto.PagedResponse;
import com.zzimple.common.exception.CustomException;
import com.zzimple.staff.client.EstimateClient;
import com.zzimple.staff.client.UserClient;
import com.zzimple.staff.client.dto.EstimateSummaryResponse;
import com.zzimple.staff.client.dto.UserSummaryResponse;
import com.zzimple.staff.dto.request.StaffTimeOffRequest;
import com.zzimple.staff.dto.response.StaffScheduleCalendarItem;
import com.zzimple.staff.dto.response.StaffTimeOffResponse;
import com.zzimple.staff.entity.Staff;
import com.zzimple.staff.entity.StaffAssignment;
import com.zzimple.staff.entity.StaffTimeOff;
import com.zzimple.staff.enums.Status;
import com.zzimple.staff.exception.StaffErrorCode;
import com.zzimple.staff.repository.StaffAssignmentRepository;
import com.zzimple.staff.repository.StaffRepository;
import com.zzimple.staff.repository.StaffTimeOffepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffScheduleService {

  private final StaffTimeOffepository staffTimeOffepository;
  private final StaffRepository staffRepository;
  private final StaffAssignmentRepository staffAssignmentRepository;
  private final UserClient userClient;
  private final EstimateClient estimateClient;

  // 1) 직원 휴무 신청
  public StaffTimeOffResponse apply(Long userId, StaffTimeOffRequest request) {

    log.info("[휴무 신청] 시작 - userId: {}, startDate: {}, endDate: {}, reason: {}",
        userId, request.getStartDate(), request.getEndDate(), request.getReason());

    // 직원 이름 (auth 도메인 - Feign)
    UserSummaryResponse user;
    try {
      user = userClient.getUser(userId);
    } catch (FeignException.NotFound e) {
      throw new EntityNotFoundException("유효하지 않은 유저입니다.");
    }

    // 직원 존재 확인 (로컬)
    Staff staff = staffRepository.findByUserId(userId)
        .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 직원입니다."));

    String staffName = user.getUserName();
    Long storeId = staff.getStoreId();

    StaffTimeOff staffTimeOff = StaffTimeOff.builder()
        .staffName(staffName)
        .ownerId(staff.getOwnerId())
        .staffId(staff.getStaffId())
        .storeId(storeId)
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .type(request.getType())
        .reason(request.getReason())
        .status(Status.PENDING)
        .build();

    StaffTimeOff saved = staffTimeOffepository.save(staffTimeOff);

    log.info("[휴무 신청] 완료 - requestId: {}, staffId: {}, storeId: {}",
        saved.getStaffTimeOffId(), staffName, storeId);

    return new StaffTimeOffResponse(
        staffTimeOff.getStaffTimeOffId(),
        staffTimeOff.getStaffName(),
        staffTimeOff.getStatus(),
        staffTimeOff.getStartDate(),
        staffTimeOff.getEndDate(),
        staffTimeOff.getType(),
        staffTimeOff.getReason()
    );
  }

  // 엔티티 → DTO 변환
  private StaffTimeOffResponse toResponse(StaffTimeOff e) {
    return StaffTimeOffResponse.builder()
        .staffTimeOffId(e.getStaffTimeOffId())
        .staffName(e.getStaffName())
        .startDate(e.getStartDate())
        .endDate(e.getEndDate())
        .type(e.getType())
        .reason(e.getReason())
        .status(e.getStatus())
        .build();
  }

  // 2) 사장님 승인/거절
  @Transactional
  public StaffTimeOffResponse decide(Long staffTimeOffId, Status status, Long storeId) {

    StaffTimeOff timeOff = staffTimeOffepository.findById(staffTimeOffId)
        .orElseThrow(() -> new CustomException(StaffErrorCode.REQUEST_NOT_FOUND));

    // 소유권 검증
    if (!timeOff.getStoreId().equals(storeId)) {
      throw new CustomException(StaffErrorCode.INVALID_STORE_ASSIGNMENT);
    }

    if (status == Status.APPROVED) {
      timeOff.setStatus(Status.APPROVED);
      log.info("[휴무 승인] requestId={}, staffName={}", staffTimeOffId, timeOff.getStaffName());
    } else {
      timeOff.setStatus(Status.REJECTED);
      log.info("[휴무 반려] requestId={}, staffName={}", staffTimeOffId, timeOff.getStaffName());
    }
    StaffTimeOff updated = staffTimeOffepository.save(timeOff);
    return toResponse(updated);
  }

  // 사장 - 휴무 내역 리스트
  // storeId는 게이트웨이가 JWT 클레임에서 추출해 전달한 값 (기존: DB에서 재조회)
  private List<StaffTimeOffResponse> listByStoreAndStatus(Long storeId, Status status) {
    List<StaffTimeOff> timeOffs = staffTimeOffepository.findAllByStoreIdAndStatus(storeId, status);

    return timeOffs.stream()
        .map(StaffTimeOffResponse::from)
        .collect(Collectors.toList());
  }

  public PagedResponse<StaffTimeOffResponse> listMyRequests(Long userId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

    Staff staff = staffRepository.findByUserId(userId)
        .orElseThrow(() -> new EntityNotFoundException("해당 직원을 찾을 수 없습니다."));

    Page<StaffTimeOff> pageResult =
        staffTimeOffepository.findByStaffId(staff.getStaffId(), pageable);

    List<StaffTimeOffResponse> content = pageResult.getContent().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());

    return PagedResponse.<StaffTimeOffResponse>builder()
        .content(content)
        .page(pageResult.getNumber())
        .size(pageResult.getSize())
        .totalElements(pageResult.getTotalElements())
        .totalPages(pageResult.getTotalPages())
        .last(pageResult.isLast())
        .build();
  }

  @Transactional(readOnly = true)
  public List<StaffTimeOffResponse> listPendingRequests(Long storeId) {
    return listByStoreAndStatus(storeId, Status.PENDING);
  }

  @Transactional(readOnly = true)
  public List<StaffTimeOffResponse> listApprovedRequests(Long storeId) {
    return listByStoreAndStatus(storeId, Status.APPROVED);
  }

  @Transactional(readOnly = true)
  public List<StaffTimeOffResponse> listRejectedRequests(Long storeId) {
    return listByStoreAndStatus(storeId, Status.REJECTED);
  }

  public List<StaffScheduleCalendarItem> getMyMonthlyCalendar(Long userId, String yearMonthStr) {
    Staff staff = staffRepository.findByUserId(userId)
        .orElseThrow(() -> new CustomException(StaffErrorCode.INVALID_STAFF_ROLE));

    Long staffId = staff.getStaffId();
    Long storeId = staff.getStoreId();

    YearMonth yearMonth = YearMonth.parse(yearMonthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
    LocalDate start = yearMonth.atDay(1);
    LocalDate end = yearMonth.atEndOfMonth();

    // 1. 근무 일정 조회 (로컬)
    List<StaffAssignment> assignments = staffAssignmentRepository
        .findAllByStaffIdAndWorkDateBetween(staffId, start, end);

    // 2. 휴무 일정 조회 (로컬)
    List<StaffTimeOff> timeOffs = staffTimeOffepository
        .findByStaffIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
            staffId, Status.APPROVED, start, end
        );

    // 3. 근무 일정 변환 - 견적 주소는 estimate 도메인에서 조회 (Feign)
    List<StaffScheduleCalendarItem> workItems = assignments.stream()
        .map(a -> {
          EstimateSummaryResponse estimate;
          try {
            estimate = estimateClient.getEstimate(a.getEstimateNo());
          } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("Estimate not found");
          }

          return StaffScheduleCalendarItem.builder()
              .date(a.getWorkDate())
              .type("WORK")
              .work(StaffScheduleCalendarItem.WorkInfo.builder()
                  .estimateNo(a.getEstimateNo())
                  .storeId(storeId)
                  .staffName(a.getStaffName())
                  .fromAddress(estimate.getFromRoadFullAddr())
                  .toAddress(estimate.getToRoadFullAddr())
                  .build())
              .build();
        })
        .toList();

    // 4. 휴무 일정 변환 (start ~ end 범위 내 날짜별로 쪼개기)
    List<StaffScheduleCalendarItem> timeOffItems = timeOffs.stream()
        .flatMap(to -> {
          LocalDate from = to.getStartDate().isBefore(start) ? start : to.getStartDate();
          LocalDate toDate = to.getEndDate().isAfter(end) ? end : to.getEndDate();
          return from.datesUntil(toDate.plusDays(1)).map(date ->
              StaffScheduleCalendarItem.builder()
                  .date(date)
                  .type("TIME_OFF")
                  .timeOff(StaffScheduleCalendarItem.TimeOffInfo.builder()
                      .type(to.getType())
                      .reason(to.getReason())
                      .build())
                  .build()
          );
        })
        .toList();

    // 5. 병합 후 날짜 기준 정렬
    List<StaffScheduleCalendarItem> merged = new ArrayList<>(workItems);
    merged.addAll(timeOffItems);
    merged.sort(Comparator.comparing(StaffScheduleCalendarItem::getDate));
    return merged;
  }
}
