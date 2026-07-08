package com.zzimple.staff.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoreSummaryResponse {
  private Long storeId;
  private Long ownerId;
  private String name;
}
