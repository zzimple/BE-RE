package com.zzimple.estimate.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.zzimple.common.event.OwnerStoreUpsertedEvent;
import com.zzimple.common.event.UserUpdatedEvent;
import com.zzimple.estimate.view.StoreView;
import com.zzimple.estimate.view.StoreViewRepository;
import com.zzimple.estimate.view.UserView;
import com.zzimple.estimate.view.UserViewRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ViewEventConsumerTest {

  @Mock private UserViewRepository userViewRepository;
  @Mock private StoreViewRepository storeViewRepository;

  @InjectMocks private ViewEventConsumer viewEventConsumer;

  @Test
  @DisplayName("user.updated 소비: user_view가 업서트된다")
  void onUserUpdated() {
    viewEventConsumer.onUserUpdated(UserUpdatedEvent.builder()
        .eventId("e-1").occurredAt(Instant.now())
        .userId(7L).userName("김고객").phoneNumber("010-1111-2222")
        .email("g@a.com").role("GUEST")
        .build());

    ArgumentCaptor<UserView> captor = ArgumentCaptor.forClass(UserView.class);
    verify(userViewRepository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    assertThat(captor.getValue().getUserName()).isEqualTo("김고객");
  }

  @Test
  @DisplayName("owner.store.upserted 소비: store_view가 업서트된다")
  void onOwnerStoreUpserted() {
    viewEventConsumer.onOwnerStoreUpserted(OwnerStoreUpsertedEvent.builder()
        .eventId("e-2").occurredAt(Instant.now())
        .ownerId(10L).ownerUserId(5L).storeId(42L)
        .storeName("찜플이사 강남점").storeAddress("서울 강남구")
        .build());

    ArgumentCaptor<StoreView> captor = ArgumentCaptor.forClass(StoreView.class);
    verify(storeViewRepository).save(captor.capture());
    assertThat(captor.getValue().getStoreId()).isEqualTo(42L);
    assertThat(captor.getValue().getStoreName()).isEqualTo("찜플이사 강남점");
  }
}
