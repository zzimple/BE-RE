package com.zzimple.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

  private static final String SECRET = "local-dev-secret-key-local-dev-secret-key";

  private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", SECRET);
  }

  @Test
  @DisplayName("일반 사용자 토큰: userId/roles 클레임을 담고 검증에 통과한다")
  void createGuestToken() {
    String token = jwtUtil.createAccessToken("guest01", 7L, List.of("GUEST"));

    assertThat(jwtUtil.validateToken(token)).isTrue();
    assertThat(jwtUtil.extractLoginId(token)).isEqualTo("guest01");
    // 게이트웨이가 X-User-Id 헤더를 만들 때 쓰는 userId 클레임
    assertThat(jwtUtil.extractStoreId(token)).isNull();
  }

  @Test
  @DisplayName("사장 토큰: storeId/ownerId 클레임이 포함된다 (게이트웨이 X-Store-Id/X-Owner-Id 출처)")
  void createOwnerToken() {
    String token = jwtUtil.createAccessToken("1234567890", 5L, List.of("OWNER"), 42L, 10L);

    assertThat(jwtUtil.validateToken(token)).isTrue();
    assertThat(jwtUtil.extractStoreId(token)).isEqualTo(42L);
  }

  @Test
  @DisplayName("다른 키로 서명된 토큰은 검증에 실패한다")
  void rejectWrongKey() {
    JwtUtil other = new JwtUtil();
    ReflectionTestUtils.setField(other, "SECRET_KEY",
        "another-secret-key-another-secret-key-!!");
    String token = other.createAccessToken("guest01", 7L, List.of("GUEST"));

    assertThat(jwtUtil.validateToken(token)).isFalse();
  }
}
