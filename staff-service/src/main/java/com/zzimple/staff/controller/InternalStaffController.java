package com.zzimple.staff.controller;

import com.zzimple.staff.entity.Staff;
import com.zzimple.staff.repository.StaffAssignmentRepository;
import com.zzimple.staff.repository.StaffRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 간 내부 API.
 * estimate 도메인(현재 legacy, 추후 estimate-service)이 직원의 견적 접근 권한을
 * 확인할 때 사용한다. 게이트웨이 밖으로 노출되지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/staff")
public class InternalStaffController {

  private final StaffRepository staffRepository;
  private final StaffAssignmentRepository staffAssignmentRepository;

  @GetMapping("/by-user/{userId}")
  public ResponseEntity<Map<String, Object>> getStaffByUserId(@PathVariable Long userId) {
    return staffRepository.findByUserId(userId)
        .map(this::toSummary)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/assignments/check")
  public ResponseEntity<Map<String, Object>> checkAssignment(
      @RequestParam Long estimateNo,
      @RequestParam Long staffId
  ) {
    boolean assigned = staffAssignmentRepository
        .findByEstimateNoAndStaffId(estimateNo, staffId)
        .isPresent();
    return ResponseEntity.ok(Map.of("assigned", assigned));
  }

  private Map<String, Object> toSummary(Staff staff) {
    return Map.of(
        "staffId", staff.getStaffId(),
        "userId", staff.getUserId(),
        "ownerId", staff.getOwnerId(),
        "storeId", staff.getStoreId(),
        "status", staff.getStatus().name()
    );
  }
}
