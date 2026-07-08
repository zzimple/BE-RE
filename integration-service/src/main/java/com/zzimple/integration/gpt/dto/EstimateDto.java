package com.zzimple.integration.gpt.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * GPT 견적 비교용 DTO.
 * MSA 전환으로 estimate 도메인 클래스 의존을 끊고,
 * integration-service가 자체적으로 소유하는 계약으로 재정의했다.
 */
@Getter
@Setter
public class EstimateDto {

  private Integer truckCount;

  private Integer truckTotalPrice;

  private String ownerMessage;

  private List<ExtraChargeItem> itemExtraCharges;

  private List<ExtraChargeItem> extraCharges;

  private Integer totalPrice;
  private Integer holidayCharge;
  private Integer goodDayCharge;
  private Integer weekendCharge;

  @Getter
  @Setter
  public static class ExtraChargeItem {
    private int amount;
    private String reason;
  }
}
