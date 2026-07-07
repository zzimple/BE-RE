package com.zzimple.staff.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerSummaryResponse {
  private Long ownerId;
  private Long userId;
}
