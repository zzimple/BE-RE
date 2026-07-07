package com.zzimple.staff.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사장이 확정(CONFIRMED)한 견적 응답 요약
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateConfirmedSummaryResponse {
  private Long estimateNo;
  private Long storeId;
  private String status;
}
