package com.zzimple.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false"
})
class GatewayApplicationTest {

  @Test
  void contextLoads() {
    // 게이트웨이 컨텍스트 기동 스모크 테스트
  }
}
