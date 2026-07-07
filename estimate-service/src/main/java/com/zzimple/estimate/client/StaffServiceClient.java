package com.zzimple.estimate.client;

import com.zzimple.estimate.client.feign.StaffFeignClient;
import feign.FeignException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** staff-service 직원/배정 조회 어댑터 (Feign/Eureka) */
@Component
@RequiredArgsConstructor
public class StaffServiceClient {

  private final StaffFeignClient staffFeignClient;

  public Optional<Map<String, Object>> findStaffByUserId(Long userId) {
    try {
      return Optional.ofNullable(staffFeignClient.getStaffByUserId(userId));
    } catch (FeignException.NotFound e) {
      return Optional.empty();
    }
  }

  public boolean isAssigned(Long estimateNo, Long staffId) {
    Map<String, Object> body = staffFeignClient.checkAssignment(estimateNo, staffId);
    return body != null && Boolean.TRUE.equals(body.get("assigned"));
  }
}
