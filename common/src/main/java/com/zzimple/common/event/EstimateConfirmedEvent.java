package com.zzimple.common.event;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토픽: estimate.confirmed.v1 (key = estimateNo)
 * 발행: estimate-service (고객이 견적을 확정한 시점)
 * 소비: owner-service (owner_sales_record 적재), staff-service (배정 준비)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateConfirmedEvent {
  private String eventId;
  private Instant occurredAt;
  private Long estimateNo;
  private Long storeId;
  private Long guestUserId;
  private String guestName;    // 표시용 비정규화 (소비측이 auth 도메인을 재조회하지 않도록)
  private String moveDate;     // yyyyMMdd
  private String moveTime;
  private Integer finalTotalPrice;
}
