package com.zzimple.staff.client;

import com.zzimple.staff.client.dto.EstimateConfirmedSummaryResponse;
import com.zzimple.staff.client.dto.EstimateSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 견적 정보 조회 클라이언트.
 * strangler 단계에서는 legacy 모놀리스를, Phase 5 이후에는 estimate-service를 가리킨다.
 * 직원 배정은 "지금 이 순간"의 견적 확정 상태를 봐야 하므로 동기(Feign) 호출이 맞다.
 */
@FeignClient(name = "estimate-service", contextId = "estimateClient",
    url = "${clients.estimate-service.url:}")
public interface EstimateClient {

  @GetMapping("/internal/estimates/{estimateNo}")
  EstimateSummaryResponse getEstimate(@PathVariable("estimateNo") Long estimateNo);

  @GetMapping("/internal/estimates/{estimateNo}/confirmed-response")
  EstimateConfirmedSummaryResponse getConfirmedResponse(@PathVariable("estimateNo") Long estimateNo);
}
