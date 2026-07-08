package com.zzimple.owner.event;

import com.zzimple.common.event.EstimateConfirmedEvent;
import com.zzimple.common.event.EstimateStatusChangedEvent;
import com.zzimple.owner.entity.OwnerSalesRecord;
import com.zzimple.owner.repository.OwnerSalesRecordRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * estimate-service가 발행하는 이벤트를 소비해 매출 read model을 적재한다.
 * (매출 대시보드가 estimate-service를 실시간 호출하지 않게 하는 CQRS 소비자)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesEventConsumer {

  private static final DateTimeFormatter MOVE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private final OwnerSalesRecordRepository ownerSalesRecordRepository;

  @Transactional
  @KafkaListener(topics = "estimate.confirmed.v1",
      groupId = "owner-service-estimate-confirmed",
      properties = {
          "spring.json.value.default.type=com.zzimple.common.event.EstimateConfirmedEvent"
      })
  public void onEstimateConfirmed(EstimateConfirmedEvent event) {
    log.info("[이벤트 소비] estimate.confirmed.v1 estimateNo={} storeId={}",
        event.getEstimateNo(), event.getStoreId());

    OwnerSalesRecord record = ownerSalesRecordRepository
        .findById(event.getEstimateNo())
        .orElseGet(() -> OwnerSalesRecord.builder()
            .estimateNo(event.getEstimateNo())
            .storeId(event.getStoreId())
            .status("CONFIRMED")
            .build());

    record.setStoreId(event.getStoreId());
    record.setGuestUserId(event.getGuestUserId());
    record.setGuestName(event.getGuestName());
    record.setFinalTotalPrice(event.getFinalTotalPrice());
    if (event.getMoveDate() != null && !event.getMoveDate().isBlank()) {
      record.setMoveDate(LocalDate.parse(event.getMoveDate(), MOVE_DATE_FORMAT));
    }
    record.setStatus("CONFIRMED");
    ownerSalesRecordRepository.save(record);
  }

  @Transactional
  @KafkaListener(topics = "estimate.status-changed.v1",
      groupId = "owner-service-estimate-status",
      properties = {
          "spring.json.value.default.type=com.zzimple.common.event.EstimateStatusChangedEvent"
      })
  public void onEstimateStatusChanged(EstimateStatusChangedEvent event) {
    log.info("[이벤트 소비] estimate.status-changed.v1 estimateNo={} {} -> {}",
        event.getEstimateNo(), event.getPreviousStatus(), event.getNewStatus());

    ownerSalesRecordRepository.findById(event.getEstimateNo())
        .ifPresent(record -> {
          record.setStatus(event.getNewStatus());
          ownerSalesRecordRepository.save(record);
        });
  }
}
