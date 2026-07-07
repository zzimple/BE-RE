package com.zzimple.estimate.client;

import com.zzimple.estimate.client.feign.OwnerFeignClient;
import feign.FeignException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * owner-service 호출 어댑터.
 * strangler 단계의 RestTemplate 클라이언트와 동일한 시그니처를 유지해
 * 서비스 레이어 코드 변경 없이 Feign(Eureka)으로 전환한다.
 */
@Component
@RequiredArgsConstructor
public class OwnerServiceClient {

  private final OwnerFeignClient ownerFeignClient;

  public Optional<Map<String, Object>> getOwner(Long ownerId) {
    return call(() -> ownerFeignClient.getOwner(ownerId));
  }

  public Optional<Map<String, Object>> getStore(Long storeId) {
    return call(() -> ownerFeignClient.getStore(storeId));
  }

  public Optional<Map<String, Object>> getStoreByOwnerUserId(Long userId) {
    return call(() -> ownerFeignClient.getStoreByOwnerUserId(userId));
  }

  public static Long asLong(Map<String, Object> map, String key) {
    Object v = map.get(key);
    return v == null ? null : ((Number) v).longValue();
  }

  public static String asString(Map<String, Object> map, String key) {
    Object v = map.get(key);
    return v == null ? null : v.toString();
  }

  private Optional<Map<String, Object>> call(java.util.function.Supplier<Map<String, Object>> s) {
    try {
      return Optional.ofNullable(s.get());
    } catch (FeignException.NotFound e) {
      return Optional.empty();
    }
  }
}
