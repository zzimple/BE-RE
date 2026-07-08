package com.zzimple.estimate.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 표시용 사용자 read model.
 * auth-service의 user.updated.v1 이벤트로 갱신된다.
 * 목록 렌더링 시 사용자당 Feign 호출(N+1)을 없애기 위한 CQRS 소비측 테이블.
 */
@Entity
@Table(name = "user_view")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserView {

  @Id
  @Column(name = "user_id")
  private Long userId;

  private String userName;
  private String phoneNumber;
  private String email;
  private String role;
}
