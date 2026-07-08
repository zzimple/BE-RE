package com.zzimple.owner.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
  private Long id;
  private String userName;
  private String loginId;
  private String phoneNumber;
  private String email;
  private String role;
}
