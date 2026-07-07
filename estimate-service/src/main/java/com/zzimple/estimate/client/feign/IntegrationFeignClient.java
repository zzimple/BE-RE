package com.zzimple.estimate.client.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "integration-service", contextId = "estimateIntegrationClient",
    url = "${clients.integration-service.url:}")
public interface IntegrationFeignClient {

  /** TM 좌표 기반 경로 조회 - 응답은 BaseResponse<KakaoRouteResponse> 형태 */
  @PostMapping("/kakao-navi/route")
  Map<String, Object> getRouteByCoordinates(@RequestBody Map<String, Double> request);
}
