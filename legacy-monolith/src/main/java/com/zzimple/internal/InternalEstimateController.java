package com.zzimple.internal;

import com.zzimple.estimate.guest.entity.Estimate;
import com.zzimple.estimate.guest.enums.EstimateStatus;
import com.zzimple.estimate.owner.repository.EstimateOwnerResponseRepository;
import com.zzimple.estimate.owner.repository.EstimateRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [임시 - strangler] 견적 정보 내부 API.
 * estimate-service 추출(Phase 5) 시 estimate-service로 이관되고 이 컨트롤러는 삭제된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/estimates")
public class InternalEstimateController {

  private final EstimateRepository estimateRepository;
  private final EstimateOwnerResponseRepository estimateOwnerResponseRepository;

  @GetMapping("/{estimateNo}")
  public ResponseEntity<Map<String, Object>> getEstimate(@PathVariable Long estimateNo) {
    return estimateRepository.findByEstimateNo(estimateNo)
        .map(e -> ResponseEntity.ok(toSummary(e)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/{estimateNo}/confirmed-response")
  public ResponseEntity<Map<String, Object>> getConfirmedResponse(@PathVariable Long estimateNo) {
    return estimateOwnerResponseRepository
        .findByEstimateNoAndStatus(estimateNo, EstimateStatus.CONFIRMED)
        .map(r -> ResponseEntity.ok(Map.<String, Object>of(
            "estimateNo", estimateNo,
            "storeId", r.getStoreId(),
            "status", r.getStatus().name()
        )))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private Map<String, Object> toSummary(Estimate estimate) {
    return Map.of(
        "estimateNo", estimate.getEstimateNo(),
        "status", estimate.getStatus().name(),
        "moveDate", estimate.getMoveDate(),
        "fromRoadFullAddr", estimate.getFromAddress() == null ? ""
            : estimate.getFromAddress().getRoadFullAddr(),
        "toRoadFullAddr", estimate.getToAddress() == null ? ""
            : estimate.getToAddress().getRoadFullAddr()
    );
  }
}
