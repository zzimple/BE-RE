package com.zzimple.owner.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사장 회원가입 시 auth 도메인에 기본 User 생성을 요청하는 내부 계약.
 * password는 owner-service에서 이미 BCrypt 인코딩된 값이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
  private String loginId;
  private String encodedPassword;
  private String userName;
  private String phoneNumber;
  private String email;
  private String role;
}
