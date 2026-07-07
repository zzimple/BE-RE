package com.zzimple.owner.event;

import com.zzimple.common.event.OwnerStoreUpsertedEvent;
import com.zzimple.owner.entity.Owner;
import com.zzimple.owner.store.entity.Store;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerEventPublisher {

  public static final String TOPIC_OWNER_STORE_UPSERTED = "owner.store.upserted.v1";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publishOwnerStoreUpserted(Owner owner, Store store) {
    OwnerStoreUpsertedEvent event = OwnerStoreUpsertedEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .occurredAt(Instant.now())
        .ownerId(owner.getId())
        .ownerUserId(owner.getUserId())
        .storeId(store.getId())
        .storeName(store.getName())
        .storeAddress(store.getAddress())
        .build();

    // key = storeId : 같은 매장의 이벤트 순서 보장
    kafkaTemplate.send(TOPIC_OWNER_STORE_UPSERTED, String.valueOf(store.getId()), event);
    log.info("[이벤트 발행] {} storeId={}", TOPIC_OWNER_STORE_UPSERTED, store.getId());
  }
}
