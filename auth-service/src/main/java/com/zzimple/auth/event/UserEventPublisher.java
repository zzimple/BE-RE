package com.zzimple.auth.event;

import com.zzimple.auth.entity.User;
import com.zzimple.common.event.UserUpdatedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

  public static final String TOPIC_USER_UPDATED = "user.updated.v1";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /** 가입/프로필 변경 시 발행. 소비측(estimate 등)은 user_view read model을 갱신한다. */
  public void publishUserUpdated(User user) {
    UserUpdatedEvent event = UserUpdatedEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .occurredAt(Instant.now())
        .userId(user.getId())
        .userName(user.getUserName())
        .phoneNumber(user.getPhoneNumber())
        .email(user.getEmail())
        .role(user.getRole().name())
        .build();

    // key = userId : 같은 사용자의 이벤트 순서 보장
    kafkaTemplate.send(TOPIC_USER_UPDATED, String.valueOf(user.getId()), event);
    log.info("[이벤트 발행] {} userId={}", TOPIC_USER_UPDATED, user.getId());
  }
}
