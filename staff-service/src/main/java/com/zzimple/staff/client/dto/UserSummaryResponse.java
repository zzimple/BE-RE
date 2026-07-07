package com.zzimple.staff.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내부 API로 조회한 사용자 요약 정보 (auth 도메인 소유 데이터의 읽기 전용 사본)
 */
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
