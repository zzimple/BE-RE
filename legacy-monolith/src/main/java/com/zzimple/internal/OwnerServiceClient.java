package com.zzimple.internal;

import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * [임시 - strangler] legacy가 추출된 owner-service의 내부 API를 호출하는 클라이언트.
 * auth-service(Phase 4)/estimate-service(Phase 5) 추출 시 각 서비스의
 * Feign 클라이언트로 대체된다.
 * 응답 형태: owner {ownerId, userId} / store {storeId, ownerId, name}
 */
@Component
@RequiredArgsConstructor
public class OwnerServiceClient {

  private final RestTemplate restTemplate;

  @Value("${clients.owner-service.url:http://localhost:8082}")
  private String ownerServiceUrl;

  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> getOwner(Long ownerId) {
    return getForMap("/internal/owners/" + ownerId);
  }

  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> getOwnerByUserId(Long userId) {
    return getForMap("/internal/owners/by-user/" + userId);
  }

  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> getStore(Long storeId) {
    return getForMap("/internal/stores/" + storeId);
  }

  @SuppressWarnings("unchecked")
  public Optional<Map<String, Object>> getStoreByOwnerUserId(Long userId) {
    return getForMap("/internal/stores/by-owner-user/" + userId);
  }

  public static Long asLong(Map<String, Object> map, String key) {
    Object v = map.get(key);
    return v == null ? null : ((Number) v).longValue();
  }

  public static String asString(Map<String, Object> map, String key) {
    Object v = map.get(key);
    return v == null ? null : v.toString();
  }

  @SuppressWarnings("unchecked")
  private Optional<Map<String, Object>> getForMap(String path) {
    try {
      return Optional.ofNullable(
          restTemplate.getForObject(ownerServiceUrl + path, Map.class));
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    }
  }
}
