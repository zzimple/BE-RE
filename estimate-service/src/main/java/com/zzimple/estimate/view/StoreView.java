package com.zzimple.estimate.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 표시용 매장 read model.
 * owner-service의 owner.store.upserted.v1 이벤트로 갱신된다.
 */
@Entity
@Table(name = "store_view")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreView {

  @Id
  @Column(name = "store_id")
  private Long storeId;

  private Long ownerId;
  private Long ownerUserId;
  private String storeName;
  private String storeAddress;
}
