package com.zzimple.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 게이트웨이가 JWT를 검증한 뒤 X-User-* 헤더로 전달한 사용자 정보.
 * 모놀리스 시절의 CustomUserDetails(DB 조회 기반)를 대체한다.
 */
@Getter
@AllArgsConstructor
public class GatewayUserPrincipal {

  private final Long userId;
  private final String loginId;
  private final String role;
  private final Long storeId;
  private final Long ownerId;
}
