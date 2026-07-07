package com.zzimple.staff.client;

import com.zzimple.staff.client.dto.UserSummaryResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 사용자 정보 조회 클라이언트.
 * strangler 단계에서는 url 프로퍼티로 legacy 모놀리스를 가리키고,
 * auth-service 추출(Phase 4) 이후에는 url을 제거해 Eureka(auth-service)로 전환한다.
 */
@FeignClient(name = "auth-service", contextId = "userClient",
    url = "${clients.auth-service.url:}")
public interface UserClient {

  @GetMapping("/internal/users/{id}")
  UserSummaryResponse getUser(@PathVariable("id") Long id);

  @GetMapping("/internal/users/by-phone")
  UserSummaryResponse getUserByPhone(@RequestParam("phone") String phone);

  @GetMapping("/internal/users/batch")
  List<UserSummaryResponse> getUsersByIds(@RequestParam("ids") List<Long> ids);
}
