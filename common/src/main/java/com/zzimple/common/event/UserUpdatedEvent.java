package com.zzimple.common.event;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토픽: user.updated.v1 (key = userId)
 * 발행: auth-service (회원가입, 이메일 등 프로필 변경 시)
 * 소비: estimate-service 등 (user_view read model 갱신 - 표시용 이름/전화번호)
 *
 * 표시용 데이터는 자주 읽히고 드물게 바뀌므로 이벤트 복제(read model)가 적합하고,
 * 인증처럼 '현재 값'이 필요한 조회는 auth-service 내부에만 존재한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEvent {
  private String eventId;
  private Instant occurredAt;
  private Long userId;
  private String userName;
  private String phoneNumber;
  private String email;
  private String role;
}
