package com.zzimple.staff.client;

import com.zzimple.staff.client.dto.OwnerSummaryResponse;
import com.zzimple.staff.client.dto.StoreSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 사장/매장 정보 조회 클라이언트.
 * strangler 단계에서는 legacy 모놀리스를, Phase 3 이후에는 owner-service를 가리킨다.
 */
@FeignClient(name = "owner-service", contextId = "ownerClient",
    url = "${clients.owner-service.url:}")
public interface OwnerClient {

  @GetMapping("/internal/owners/{ownerId}")
  OwnerSummaryResponse getOwner(@PathVariable("ownerId") Long ownerId);

  @GetMapping("/internal/owners/by-user/{userId}")
  OwnerSummaryResponse getOwnerByUserId(@PathVariable("userId") Long userId);

  @GetMapping("/internal/stores/by-owner/{ownerId}")
  StoreSummaryResponse getStoreByOwnerId(@PathVariable("ownerId") Long ownerId);
}
