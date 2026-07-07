package com.zzimple.owner.entity;

import com.zzimple.common.base.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 매출 조회용 read model.
 * estimate-service가 발행하는 Kafka 이벤트(estimate.confirmed.v1 /
 * estimate.status-changed.v1)를 소비해 적재한다.
 * 모놀리스 시절 estimate 도메인의 리포지토리를 직접 스캔하던
 * getMonthlyAverageSales/getWeeklySales를 로컬 조회로 대체한다 (CQRS).
 */
@Entity
@Table(name = "owner_sales_record", indexes = {
    @Index(name = "idx_sales_store_id", columnList = "store_id")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OwnerSalesRecord extends BaseTimeEntity {

  @Id
  @Column(name = "estimate_no")
  private Long estimateNo;

  @Column(name = "store_id", nullable = false)
  private Long storeId;

  @Column(name = "guest_user_id")
  private Long guestUserId;

  @Column(name = "guest_name")
  private String guestName;

  @Column(name = "final_total_price")
  private Integer finalTotalPrice;

  @Column(name = "move_date")
  private LocalDate moveDate;

  @Column(name = "status", nullable = false)
  private String status;
}
