package com.zzimple.auth.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.zzimple.auth.entity.User;
import com.zzimple.auth.enums.UserRole;
import com.zzimple.common.event.UserUpdatedEvent;
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
class UserEventPublisherTest {

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks private UserEventPublisher userEventPublisher;

  @Test
  @DisplayName("user.updated.v1: userId를 키로 표시용 프로필 이벤트를 발행한다")
  void publishUserUpdated() {
    User user = User.builder()
        .loginId("login01")
        .userName("김테스트")
        .phoneNumber("010-1111-2222")
        .email("t@a.com")
        .role(UserRole.GUEST)
        .build();
    ReflectionTestUtils.setField(user, "id", 7L);

    userEventPublisher.publishUserUpdated(user);

    ArgumentCaptor<UserUpdatedEvent> captor = ArgumentCaptor.forClass(UserUpdatedEvent.class);
    verify(kafkaTemplate).send(eq(UserEventPublisher.TOPIC_USER_UPDATED), eq("7"),
        captor.capture());

    UserUpdatedEvent event = captor.getValue();
    assertThat(event.getUserId()).isEqualTo(7L);
    assertThat(event.getUserName()).isEqualTo("김테스트");
    assertThat(event.getRole()).isEqualTo("GUEST");
    assertThat(event.getEventId()).isNotBlank();
  }
}
