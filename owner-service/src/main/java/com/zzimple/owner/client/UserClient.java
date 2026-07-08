package com.zzimple.owner.client;

import com.zzimple.owner.client.dto.CreateUserRequest;
import com.zzimple.owner.client.dto.UserSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 사용자 정보 클라이언트.
 * strangler 단계에서는 legacy 모놀리스를, auth-service 추출(Phase 4) 이후에는
 * url 제거로 Eureka(auth-service)를 가리킨다.
 *
 * 회원가입의 User 생성은 동기 Feign이어야 한다:
 * User 없이 Owner만 존재하는 상태(로그인 불가 계정)를 허용할 수 없기 때문.
 */
@FeignClient(name = "auth-service", contextId = "ownerUserClient",
    url = "${clients.auth-service.url:}")
public interface UserClient {

  @GetMapping("/internal/users/{id}")
  UserSummaryResponse getUser(@PathVariable("id") Long id);

  @PostMapping("/internal/users")
  UserSummaryResponse createUser(@RequestBody CreateUserRequest request);
}
