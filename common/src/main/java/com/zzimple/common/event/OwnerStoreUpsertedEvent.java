package com.zzimple.common.event;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토픽: owner.store.upserted.v1 (key = storeId)
 * 발행: owner-service (사장 회원가입/프로필 변경 시)
 * 소비: estimate-service (store_view/owner_view read model 갱신)
 *
 * 이벤트 스키마를 common에 두는 이유: producer/consumer가 와이어 포맷을
 * 컴파일 타임에 합의하게 하기 위함 (실무 대규모 조직이라면 별도 버저닝된
 * event-contracts 아티팩트로 분리하는 것이 정석 - 의도된 단순화).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerStoreUpsertedEvent {
  private String eventId;
  private Instant occurredAt;
  private Long ownerId;
  private Long ownerUserId;
  private Long storeId;
  private String storeName;
  private String storeAddress;
}
