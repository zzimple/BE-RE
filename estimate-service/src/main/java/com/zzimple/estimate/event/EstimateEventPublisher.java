package com.zzimple.estimate.event;

import com.zzimple.common.event.EstimateConfirmedEvent;
import com.zzimple.common.event.EstimateStatusChangedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EstimateEventPublisher {

  public static final String TOPIC_ESTIMATE_CONFIRMED = "estimate.confirmed.v1";
  public static final String TOPIC_ESTIMATE_STATUS_CHANGED = "estimate.status-changed.v1";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * 고객이 견적을 확정한 시점에 발행.
   * 소비: owner-service(매출 read model), staff-service(배정 준비).
   */
  public void publishEstimateConfirmed(Long estimateNo, Long storeId, Long guestUserId,
      String guestName, String moveDate, String moveTime, Integer finalTotalPrice) {
    EstimateConfirmedEvent event = EstimateConfirmedEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .occurredAt(Instant.now())
        .estimateNo(estimateNo)
        .storeId(storeId)
        .guestUserId(guestUserId)
        .guestName(guestName)
        .moveDate(moveDate)
        .moveTime(moveTime)
        .finalTotalPrice(finalTotalPrice)
        .build();

    // key = estimateNo : 같은 견적의 이벤트 순서 보장
    kafkaTemplate.send(TOPIC_ESTIMATE_CONFIRMED, String.valueOf(estimateNo), event);
    log.info("[이벤트 발행] {} estimateNo={} storeId={}", TOPIC_ESTIMATE_CONFIRMED, estimateNo, storeId);
  }

  /** CONFIRMED 이후 상태 전이(자동 완료 등) 시 발행. 소비: owner-service. */
  public void publishStatusChanged(Long estimateNo, Long storeId,
      String previousStatus, String newStatus) {
    EstimateStatusChangedEvent event = EstimateStatusChangedEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .occurredAt(Instant.now())
        .estimateNo(estimateNo)
        .storeId(storeId)
        .previousStatus(previousStatus)
        .newStatus(newStatus)
        .build();

    kafkaTemplate.send(TOPIC_ESTIMATE_STATUS_CHANGED, String.valueOf(estimateNo), event);
    log.info("[이벤트 발행] {} estimateNo={} {} -> {}", TOPIC_ESTIMATE_STATUS_CHANGED,
        estimateNo, previousStatus, newStatus);
  }
}
