package com.zzimple.auth.client;

import com.zzimple.auth.client.dto.StoreSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 사장/매장 정보 클라이언트 (owner-service, Eureka).
 * OWNER 로그인/리프레시 시 JWT에 넣을 storeId/ownerId를 해석한다.
 * 갓 발급되는 토큰에 오래된 storeId가 들어가면 안 되므로
 * read model이 아닌 동기 Feign 호출이어야 한다.
 */
@FeignClient(name = "owner-service", contextId = "authOwnerClient",
    url = "${clients.owner-service.url:}")
public interface OwnerClient {

  @GetMapping("/internal/stores/by-owner-user/{userId}")
  StoreSummaryResponse getStoreByOwnerUserId(@PathVariable("userId") Long userId);
}
