package com.zzimple.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.config.server.native.search-locations=classpath:/config-repo-test",
    "eureka.client.enabled=false"
})
class ConfigServerApplicationTest {

  @Test
  void contextLoads() {
    // native 프로필 기반 Config Server 컨텍스트 기동 스모크 테스트
  }
}
