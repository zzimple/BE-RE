package com.zzimple.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzimple.common.exception.CustomException;
import com.zzimple.staff.client.EstimateClient;
import com.zzimple.staff.client.UserClient;
import com.zzimple.staff.client.dto.EstimateConfirmedSummaryResponse;
import com.zzimple.staff.client.dto.EstimateSummaryResponse;
import com.zzimple.staff.client.dto.UserSummaryResponse;
import com.zzimple.staff.entity.Staff;
import com.zzimple.staff.entity.StaffAssignment;
import com.zzimple.staff.enums.Status;
import com.zzimple.staff.exception.StaffErrorCode;
import com.zzimple.staff.repository.StaffAssignmentRepository;
import com.zzimple.staff.repository.StaffRepository;
import com.zzimple.staff.repository.StaffTimeOffepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffAssignmentServiceTest {

  @Mock
  private StaffRepository staffRepository;

  @Mock
  private StaffAssignmentRepository staffAssignmentRepository;

  @Mock
  private StaffTimeOffepository staffTimeOffepository;

  @Mock
  private EstimateClient estimateClient;

  @Mock
  private UserClient userClient;

  @InjectMocks
  private StaffAssignmentService staffAssignmentService;

  private static final EstimateSummaryResponse CONFIRMED_ESTIMATE =
      new EstimateSummaryResponse(100L, "CONFIRMED", "20260810", "서울 A로 1", "서울 B로 2");

  @Test
  @DisplayName("직원 배정: 확정된 견적이면 배정이 저장되고 근무일이 이사일로 파싱된다")
  void assignWithDateSuccess() {
    when(estimateClient.getEstimate(100L)).thenReturn(CONFIRMED_ESTIMATE);
    when(estimateClient.getConfirmedResponse(100L))
        .thenReturn(new EstimateConfirmedSummaryResponse(100L, 42L, "CONFIRMED"));
    Staff staff = Staff.builder().userId(7L).ownerId(10L).storeId(42L)
        .status(Status.APPROVED).build();
    when(staffRepository.findByStaffId(5L)).thenReturn(Optional.of(staff));
    when(staffAssignmentRepository.findByEstimateNoAndStaffId(100L, 5L))
        .thenReturn(Optional.empty());
    when(userClient.getUser(7L)).thenReturn(
        new UserSummaryResponse(7L, "김직원", "staff01", "010-1111-2222", "s@a.com", "STAFF"));

    var results = staffAssignmentService.assignWithDate(100L, List.of(5L));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getStaffName()).isEqualTo("김직원");
    assertThat(results.get(0).getWorkDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    verify(staffAssignmentRepository).save(any(StaffAssignment.class));
  }

  @Test
  @DisplayName("직원 배정: 고객이 확정하지 않은 견적이면 배정이 거부된다")
  void assignWithDateRejectsUnconfirmed() {
    when(estimateClient.getEstimate(100L)).thenReturn(
        new EstimateSummaryResponse(100L, "PENDING", "20260810", "", ""));

    assertThatThrownBy(() -> staffAssignmentService.assignWithDate(100L, List.of(5L)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("확정");

    verify(staffAssignmentRepository, never()).save(any());
  }

  @Test
  @DisplayName("배정 가능 직원 조회: 휴가자와 이미 배정된 직원은 제외된다")
  void availableStaffExcludesTimeOffAndBusy() {
    when(estimateClient.getEstimate(100L)).thenReturn(CONFIRMED_ESTIMATE);
    LocalDate workDate = LocalDate.of(2026, 8, 10);

    Staff available = Staff.builder().userId(7L).ownerId(10L).storeId(42L)
        .status(Status.APPROVED).build();
    Staff onTimeOff = Staff.builder().userId(8L).ownerId(10L).storeId(42L)
        .status(Status.APPROVED).build();
    Staff busy = Staff.builder().userId(9L).ownerId(10L).storeId(42L)
        .status(Status.APPROVED).build();
    org.springframework.test.util.ReflectionTestUtils.setField(available, "staffId", 1L);
    org.springframework.test.util.ReflectionTestUtils.setField(onTimeOff, "staffId", 2L);
    org.springframework.test.util.ReflectionTestUtils.setField(busy, "staffId", 3L);

    when(staffRepository.findByOwnerIdAndStatus(10L, Status.APPROVED))
        .thenReturn(List.of(available, onTimeOff, busy));
    // staffId=2 는 휴가 중
    when(staffTimeOffepository
        .existsByStaffIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            anyLong(), any(), any(), any()))
        .thenAnswer(inv -> inv.getArgument(0, Long.class).equals(2L));
    // staffId=3 은 이미 배정됨
    StaffAssignment busyAssignment = StaffAssignment.builder()
        .estimateNo(200L).staffId(3L).staffName("바쁨").workDate(workDate).build();
    when(staffAssignmentRepository.findByWorkDate(workDate))
        .thenReturn(List.of(busyAssignment));
    when(userClient.getUsersByIds(List.of(7L))).thenReturn(List.of(
        new UserSummaryResponse(7L, "김가능", "staff01", "010-1111-2222", "s@a.com", "STAFF")));

    var result = staffAssignmentService.getAvailableStaffList(100L, 10L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStaffId()).isEqualTo(1L);
    assertThat(result.get(0).getStaffName()).isEqualTo("김가능");
  }

  @Test
  @DisplayName("배정된 직원 조회: 확정 응답의 storeId와 요청자 storeId가 다르면 거부된다")
  void assignedStaffRejectsForeignStore() {
    when(estimateClient.getConfirmedResponse(100L))
        .thenReturn(new EstimateConfirmedSummaryResponse(100L, 42L, "CONFIRMED"));

    assertThatThrownBy(() -> staffAssignmentService.getAssignedStaffList(100L, 99L))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(StaffErrorCode.STAFF_OWNER_MISMATCH);
  }
}
