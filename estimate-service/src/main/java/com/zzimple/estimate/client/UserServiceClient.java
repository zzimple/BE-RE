package com.zzimple.estimate.client;

import com.zzimple.estimate.client.feign.UserFeignClient;
import feign.FeignException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** auth-service 사용자 조회 어댑터 (Feign/Eureka) */
@Component
@RequiredArgsConstructor
public class UserServiceClient {

  private final UserFeignClient userFeignClient;

  public Optional<Map<String, Object>> getUser(Long userId) {
    try {
      return Optional.ofNullable(userFeignClient.getUser(userId));
    } catch (FeignException.NotFound e) {
      return Optional.empty();
    }
  }

  public static String asString(Map<String, Object> map, String key) {
    Object v = map.get(key);
    return v == null ? null : v.toString();
  }
}
