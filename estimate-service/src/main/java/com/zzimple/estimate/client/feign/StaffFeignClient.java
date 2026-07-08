package com.zzimple.estimate.client.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "staff-service", contextId = "estimateStaffClient",
    url = "${clients.staff-service.url:}")
public interface StaffFeignClient {

  @GetMapping("/internal/staff/by-user/{userId}")
  Map<String, Object> getStaffByUserId(@PathVariable("userId") Long userId);

  @GetMapping("/internal/staff/assignments/check")
  Map<String, Object> checkAssignment(@RequestParam("estimateNo") Long estimateNo,
      @RequestParam("staffId") Long staffId);
}
