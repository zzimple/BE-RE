package com.zzimple.staff.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.zzimple.staff.client.dto.UserSummaryResponse;
import feign.FeignException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * UserClient(Feign) 계약 테스트.
 * WireMock으로 내부 API 응답을 스텁해 JSON 역직렬화와 404 처리 규약을 검증한다.
 * (strangler 단계에서는 legacy가, Phase 4 이후에는 auth-service가 이 계약을 제공한다)
 */
@SpringBootTest
class UserClientWireMockTest {

  static WireMockServer wireMock;

  @Autowired
  private UserClient userClient;

  @BeforeAll
  static void startWireMock() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();
  }

  @AfterAll
  static void stopWireMock() {
    wireMock.stop();
  }

  @DynamicPropertySource
  static void overrideClientUrl(DynamicPropertyRegistry registry) {
    registry.add("clients.auth-service.url", () -> "http://localhost:" + wireMock.port());
  }

  @Test
  @DisplayName("GET /internal/users/{id}: 사용자 요약 JSON을 역직렬화한다")
  void getUser() {
    wireMock.stubFor(get(urlEqualTo("/internal/users/7"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                  "id": 7,
                  "userName": "김직원",
                  "loginId": "staff01",
                  "phoneNumber": "010-1111-2222",
                  "email": "s@a.com",
                  "role": "STAFF"
                }
                """)));

    UserSummaryResponse user = userClient.getUser(7L);

    assertThat(user.getId()).isEqualTo(7L);
    assertThat(user.getUserName()).isEqualTo("김직원");
    assertThat(user.getRole()).isEqualTo("STAFF");
  }

  @Test
  @DisplayName("GET /internal/users/{id}: 404는 FeignException.NotFound로 매핑된다")
  void getUserNotFound() {
    wireMock.stubFor(get(urlEqualTo("/internal/users/999"))
        .willReturn(aResponse().withStatus(404)));

    assertThatThrownBy(() -> userClient.getUser(999L))
        .isInstanceOf(FeignException.NotFound.class);
  }
}
