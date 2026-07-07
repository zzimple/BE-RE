package com.zzimple.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzimple.common.exception.CustomException;
import com.zzimple.staff.client.OwnerClient;
import com.zzimple.staff.client.UserClient;
import com.zzimple.staff.client.dto.OwnerSummaryResponse;
import com.zzimple.staff.client.dto.StoreSummaryResponse;
import com.zzimple.staff.client.dto.UserSummaryResponse;
import com.zzimple.staff.dto.request.StaffsendApprovalRequest;
import com.zzimple.staff.entity.Staff;
import com.zzimple.staff.enums.Status;
import com.zzimple.staff.exception.StaffErrorCode;
import com.zzimple.staff.repository.StaffRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

  @Mock
  private StaffRepository staffRepository;

  @Mock
  private UserClient userClient;

  @Mock
  private OwnerClient ownerClient;

  @InjectMocks
  private StaffService staffService;

  private static final UserSummaryResponse STAFF_USER =
      new UserSummaryResponse(1L, "김직원", "staff01", "010-1111-2222", "s@a.com", "STAFF");
  private static final UserSummaryResponse OWNER_USER =
      new UserSummaryResponse(2L, "박사장", "owner01", "010-0000-0000", "o@a.com", "OWNER");

  private StaffsendApprovalRequest approvalRequest() {
    StaffsendApprovalRequest request = new StaffsendApprovalRequest();
    ReflectionTestUtils.setField(request, "ownerPhoneNumber", "010-0000-0000");
    return request;
  }

  @Test
  @DisplayName("직원 승인 요청: STAFF가 사장 전화번호로 요청하면 PENDING Staff가 저장된다")
  void requestApprovalSuccess() {
    when(userClient.getUser(1L)).thenReturn(STAFF_USER);
    when(userClient.getUserByPhone("010-0000-0000")).thenReturn(OWNER_USER);
    when(ownerClient.getOwnerByUserId(2L)).thenReturn(new OwnerSummaryResponse(10L, 2L));
    when(ownerClient.getStoreByOwnerId(10L))
        .thenReturn(new StoreSummaryResponse(42L, 10L, "찜플이사 강남점"));
    when(staffRepository.existsByUserIdAndOwnerId(1L, 2L)).thenReturn(false);

    var response = staffService.requestApproval(1L, approvalRequest());

    assertThat(response.isSuccess()).isTrue();
    verify(staffRepository).save(any(Staff.class));
  }

  @Test
  @DisplayName("직원 승인 요청: STAFF 권한이 아니면 거부된다")
  void requestApprovalRejectsNonStaff() {
    when(userClient.getUser(2L)).thenReturn(OWNER_USER);

    assertThatThrownBy(() -> staffService.requestApproval(2L, approvalRequest()))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(StaffErrorCode.INVALID_STAFF_ROLE);

    verify(staffRepository, never()).save(any());
  }

  @Test
  @DisplayName("직원 승인: 다른 매장의 직원을 승인하려 하면 거부된다")
  void approveStaffRejectsForeignStore() {
    Staff staff = Staff.builder()
        .userId(1L).ownerId(10L).storeId(42L).status(Status.PENDING).build();
    when(staffRepository.findById(5L)).thenReturn(Optional.of(staff));

    assertThatThrownBy(() -> staffService.approveStaff(5L, Status.APPROVED, 99L))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(StaffErrorCode.STAFF_OWNER_MISMATCH);
  }

  @Test
  @DisplayName("직원 승인: 소속 매장 사장의 승인이면 APPROVED로 변경된다")
  void approveStaffSuccess() {
    Staff staff = Staff.builder()
        .userId(1L).ownerId(10L).storeId(42L).status(Status.PENDING).build();
    when(staffRepository.findById(5L)).thenReturn(Optional.of(staff));

    Status result = staffService.approveStaff(5L, Status.APPROVED, 42L);

    assertThat(result).isEqualTo(Status.APPROVED);
    assertThat(staff.getStatus()).isEqualTo(Status.APPROVED);
  }
}
