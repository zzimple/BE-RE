package com.zzimple.common.event;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토픽: estimate.status-changed.v1 (key = estimateNo)
 * 발행: estimate-service (스케줄러의 자동 완료 등 CONFIRMED 이후 상태 전이)
 * 소비: owner-service (owner_sales_record.status 갱신 -> 매출 통계의 완료/진행 구분)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateStatusChangedEvent {
  private String eventId;
  private Instant occurredAt;
  private Long estimateNo;
  private Long storeId;
  private String previousStatus;
  private String newStatus;
}
