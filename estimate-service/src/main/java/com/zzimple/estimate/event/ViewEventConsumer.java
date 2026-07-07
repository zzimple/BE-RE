package com.zzimple.estimate.event;

import com.zzimple.common.event.OwnerStoreUpsertedEvent;
import com.zzimple.common.event.UserUpdatedEvent;
import com.zzimple.estimate.view.StoreView;
import com.zzimple.estimate.view.StoreViewRepository;
import com.zzimple.estimate.view.UserView;
import com.zzimple.estimate.view.UserViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 표시용 read model 소비자.
 * 이름/매장명은 자주 읽히고 드물게 바뀌므로 이벤트 복제(eventual consistency)가 적합하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewEventConsumer {

  private final UserViewRepository userViewRepository;
  private final StoreViewRepository storeViewRepository;

  @Transactional
  @KafkaListener(topics = "user.updated.v1",
      groupId = "estimate-service-user-updated",
      properties = {
          "spring.json.value.default.type=com.zzimple.common.event.UserUpdatedEvent"
      })
  public void onUserUpdated(UserUpdatedEvent event) {
    log.info("[이벤트 소비] user.updated.v1 userId={}", event.getUserId());
    userViewRepository.save(UserView.builder()
        .userId(event.getUserId())
        .userName(event.getUserName())
        .phoneNumber(event.getPhoneNumber())
        .email(event.getEmail())
        .role(event.getRole())
        .build());
  }

  @Transactional
  @KafkaListener(topics = "owner.store.upserted.v1",
      groupId = "estimate-service-store-upserted",
      properties = {
          "spring.json.value.default.type=com.zzimple.common.event.OwnerStoreUpsertedEvent"
      })
  public void onOwnerStoreUpserted(OwnerStoreUpsertedEvent event) {
    log.info("[이벤트 소비] owner.store.upserted.v1 storeId={}", event.getStoreId());
    storeViewRepository.save(StoreView.builder()
        .storeId(event.getStoreId())
        .ownerId(event.getOwnerId())
        .ownerUserId(event.getOwnerUserId())
        .storeName(event.getStoreName())
        .storeAddress(event.getStoreAddress())
        .build());
  }
}
