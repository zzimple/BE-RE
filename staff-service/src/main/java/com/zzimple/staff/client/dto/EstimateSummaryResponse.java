package com.zzimple.staff.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateSummaryResponse {
  private Long estimateNo;
  private String status;       // EstimateStatus 이름 (PENDING/CONFIRMED/...)
  private String moveDate;     // yyyyMMdd
  private String fromRoadFullAddr;
  private String toRoadFullAddr;
}
