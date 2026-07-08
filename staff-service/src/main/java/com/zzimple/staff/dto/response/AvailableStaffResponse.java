package com.zzimple.staff.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailableStaffResponse {
  private Long staffId;
  private String staffName;
  private String staffPhoneNum;
  private String status;
}
