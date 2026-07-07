package com.zzimple.auth.dto.request;

import lombok.Getter;

@Getter
public class UpdatePasswordRequest {
  private String currentPassword;
  private String newPassword;
}
