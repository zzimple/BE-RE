package com.zzimple.estimate.controller;

import com.zzimple.estimate.client.feign.IntegrationFeignClient;
import com.zzimple.estimate.guest.entity.Address;
import com.zzimple.estimate.guest.entity.Estimate;
import com.zzimple.estimate.owner.repository.EstimateRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 견적 경로 조회.
 * 모놀리스의 GET /kakao-navi/route/{estimateNo}를 대체한다:
 * 견적(주소 좌표)은 이 서비스 소유이므로 여기서 좌표를 해석하고,
 * 외부 API 호출은 integration-service에 위임한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/estimates")
public class EstimateRouteController {

  private final EstimateRepository estimateRepository;
  private final IntegrationFeignClient integrationFeignClient;

  @GetMapping("/{estimateNo}/route")
  public ResponseEntity<Map<String, Object>> getRoute(@PathVariable Long estimateNo) {
    Estimate estimate = estimateRepository.findByEstimateNo(estimateNo)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 견적 번호입니다."));

    Address from = estimate.getFromAddress();
    Address to = estimate.getToAddress();

    Map<String, Object> response = integrationFeignClient.getRouteByCoordinates(Map.of(
        "originEntX", Double.parseDouble(from.getEntX()),
        "originEntY", Double.parseDouble(from.getEntY()),
        "destEntX", Double.parseDouble(to.getEntX()),
        "destEntY", Double.parseDouble(to.getEntY())
    ));
    return ResponseEntity.ok(response);
  }
}
