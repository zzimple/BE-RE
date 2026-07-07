package com.zzimple.owner.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.zzimple.common.event.OwnerStoreUpsertedEvent;
import com.zzimple.owner.entity.Owner;
import com.zzimple.owner.store.entity.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OwnerEventPublisherTest {

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks private OwnerEventPublisher ownerEventPublisher;

  @Test
  @DisplayName("owner.store.upserted.v1: storeId를 키로 매장 정보 이벤트를 발행한다")
  void publishOwnerStoreUpserted() {
    Owner owner = new Owner();
    ReflectionTestUtils.setField(owner, "id", 10L);
    ReflectionTestUtils.setField(owner, "userId", 5L);
    Store store = new Store();
    ReflectionTestUtils.setField(store, "id", 42L);
    ReflectionTestUtils.setField(store, "ownerId", 10L);
    ReflectionTestUtils.setField(store, "name", "찜플이사 강남점");
    ReflectionTestUtils.setField(store, "address", "서울 강남구 테헤란로 123");

    ownerEventPublisher.publishOwnerStoreUpserted(owner, store);

    ArgumentCaptor<OwnerStoreUpsertedEvent> captor =
        ArgumentCaptor.forClass(OwnerStoreUpsertedEvent.class);
    verify(kafkaTemplate).send(
        org.mockito.ArgumentMatchers.eq(OwnerEventPublisher.TOPIC_OWNER_STORE_UPSERTED),
        org.mockito.ArgumentMatchers.eq("42"),
        captor.capture());

    OwnerStoreUpsertedEvent event = captor.getValue();
    assertThat(event.getOwnerId()).isEqualTo(10L);
    assertThat(event.getOwnerUserId()).isEqualTo(5L);
    assertThat(event.getStoreId()).isEqualTo(42L);
    assertThat(event.getStoreName()).isEqualTo("찜플이사 강남점");
    assertThat(event.getEventId()).isNotBlank();
    assertThat(event.getOccurredAt()).isNotNull();
  }
}
