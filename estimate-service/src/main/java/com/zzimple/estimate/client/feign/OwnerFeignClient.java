package com.zzimple.estimate.client.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "owner-service", contextId = "estimateOwnerClient",
    url = "${clients.owner-service.url:}")
public interface OwnerFeignClient {

  @GetMapping("/internal/owners/{ownerId}")
  Map<String, Object> getOwner(@PathVariable("ownerId") Long ownerId);

  @GetMapping("/internal/stores/{storeId}")
  Map<String, Object> getStore(@PathVariable("storeId") Long storeId);

  @GetMapping("/internal/stores/by-owner-user/{userId}")
  Map<String, Object> getStoreByOwnerUserId(@PathVariable("userId") Long userId);
}
