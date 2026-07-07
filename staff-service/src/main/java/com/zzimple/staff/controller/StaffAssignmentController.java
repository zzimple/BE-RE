package com.zzimple.staff.controller;

import com.zzimple.common.dto.BaseResponse;
import com.zzimple.common.security.GatewayUserPrincipal;
import com.zzimple.staff.dto.response.AssignStaffDateResponse;
import com.zzimple.staff.dto.response.AssignedStaffListResponse;
import com.zzimple.staff.dto.response.AvailableStaffResponse;
import com.zzimple.staff.service.StaffAssignmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 직원 배정 API (모놀리스의 OwnerScheduleController 승계, 프론트 호환을 위해 경로 유지).
 * 사장의 storeId/ownerId는 게이트웨이가 JWT 클레임에서 추출해 전달한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/owner/schedule")
public class StaffAssignmentController {

  private final StaffAssignmentService staffAssignmentService;

  @PostMapping("/{estimateNo}/assign")
  @PreAuthorize("hasRole('OWNER')")
  public ResponseEntity<BaseResponse<List<AssignStaffDateResponse>>> assign(
      @PathVariable Long estimateNo,
      @RequestParam List<Long> staffIds
  ) {
    List<AssignStaffDateResponse> respList =
        staffAssignmentService.assignWithDate(estimateNo, staffIds);

    return ResponseEntity.ok(
        BaseResponse.success("직원 스케줄 배정에 성공했습니다", respList)
    );
  }

  @GetMapping("/{estimateNo}/available-staff")
  @PreAuthorize("hasRole('OWNER')")
  public ResponseEntity<BaseResponse<List<AvailableStaffResponse>>> getAvailableStaff(
      @PathVariable Long estimateNo,
      @AuthenticationPrincipal GatewayUserPrincipal userDetails
  ) {
    List<AvailableStaffResponse> staffList =
        staffAssignmentService.getAvailableStaffList(estimateNo, userDetails.getOwnerId());

    return ResponseEntity.ok(BaseResponse.success("사용 가능한 직원 목록입니다.", staffList));
  }

  @GetMapping("/{estimateNo}/assigned-staff")
  @PreAuthorize("hasRole('OWNER')")
  public ResponseEntity<BaseResponse<AssignedStaffListResponse>> getAssignedStaff(
      @PathVariable Long estimateNo,
      @AuthenticationPrincipal GatewayUserPrincipal userDetails
  ) {
    AssignedStaffListResponse response =
        staffAssignmentService.getAssignedStaffList(estimateNo, userDetails.getStoreId());

    return ResponseEntity.ok(BaseResponse.success("배정된 직원 목록입니다.", response));
  }
}
