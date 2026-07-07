package com.zzimple.owner.controller;

import com.zzimple.owner.entity.Owner;
import com.zzimple.owner.repository.OwnerRepository;
import com.zzimple.owner.store.entity.Store;
import com.zzimple.owner.store.repository.StoreRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 간 내부 API.
 * staff-service(직원 소속 검증), legacy/estimate-service(매장 조회),
 * auth-service(로그인 시 storeId/ownerId 클레임 해석)가 사용한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalOwnerController {

  private final OwnerRepository ownerRepository;
  private final StoreRepository storeRepository;

  @GetMapping("/owners/{ownerId}")
  public ResponseEntity<Map<String, Object>> getOwner(@PathVariable Long ownerId) {
    return ownerRepository.findById(ownerId)
        .map(o -> ResponseEntity.ok(toOwnerSummary(o)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/owners/by-user/{userId}")
  public ResponseEntity<Map<String, Object>> getOwnerByUserId(@PathVariable Long userId) {
    return ownerRepository.findByUserId(userId)
        .map(o -> ResponseEntity.ok(toOwnerSummary(o)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/stores/{storeId}")
  public ResponseEntity<Map<String, Object>> getStore(@PathVariable Long storeId) {
    return storeRepository.findById(storeId)
        .map(s -> ResponseEntity.ok(toStoreSummary(s)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/stores/by-owner/{ownerId}")
  public ResponseEntity<Map<String, Object>> getStoreByOwnerId(@PathVariable Long ownerId) {
    return storeRepository.findByOwnerId(ownerId)
        .map(s -> ResponseEntity.ok(toStoreSummary(s)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/stores/by-owner-user/{userId}")
  public ResponseEntity<Map<String, Object>> getStoreByOwnerUserId(@PathVariable Long userId) {
    return storeRepository.findByOwnerUserId(userId)
        .map(s -> ResponseEntity.ok(toStoreSummary(s)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private Map<String, Object> toOwnerSummary(Owner owner) {
    return Map.of(
        "ownerId", owner.getId(),
        "userId", owner.getUserId()
    );
  }

  private Map<String, Object> toStoreSummary(Store store) {
    return Map.of(
        "storeId", store.getId(),
        "ownerId", store.getOwnerId(),
        "name", store.getName() == null ? "" : store.getName()
    );
  }
}
