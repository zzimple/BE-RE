package com.zzimple.common.enums;

/**
 * 서비스 전반에서 공유되는 사용자 역할.
 * JWT 클레임과 X-User-Role 헤더 값으로 사용된다.
 */
public enum UserRole {
  GUEST,
  OWNER,
  STAFF,
  DEVELOPER
}
