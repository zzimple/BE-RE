package com.zzimple.internal;

import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * [임시 - strangler] legacy가 auth-service의 내부 사용자 API를 호출하는 클라이언트.
 * estimate-service 추출(Phase 5) 시 Feign 클라이언트로 대체된다.
 * 응답 형태: {id, userName, loginId, phoneNumber, email, role}
 */
@Component
@RequiredArgsConstructor
public class UserServiceClient {

  private final RestTemplate restTemplate;

  @Value("${clients.auth-service.url:http://localhost:8084}")
  private String authServiceUrl;

  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> getUser(Long userId) {
    try {
      return Optional.ofNullable(
          restTemplate.getForObject(authServiceUrl + "/internal/users/" + userId, Map.class));
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    }
  }

  public static String asString(Map<String, Object> map, String key) {
    Object v = map.get(key);
    return v == null ? null : v.toString();
  }
}
