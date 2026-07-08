package com.zzimple.staff.dto.response;

import com.zzimple.staff.entity.Staff;
import com.zzimple.staff.enums.Status;
import com.zzimple.staff.client.dto.UserSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StaffListResponse {
  private Long staffId;
  private Long userId;
  private String name;
  private String id;
  private String phone;
  private Status status;

  public static StaffListResponse from(Staff staff, UserSummaryResponse user) {
    return new StaffListResponse(
        staff.getStaffId(),
        staff.getUserId(),
        user.getUserName(),
        user.getLoginId(),
        user.getPhoneNumber(),
        staff.getStatus()
    );
  }

}

