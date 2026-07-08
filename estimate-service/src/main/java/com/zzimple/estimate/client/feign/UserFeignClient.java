package com.zzimple.estimate.client.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", contextId = "estimateUserClient",
    url = "${clients.auth-service.url:}")
public interface UserFeignClient {

  @GetMapping("/internal/users/{id}")
  Map<String, Object> getUser(@PathVariable("id") Long id);
}
