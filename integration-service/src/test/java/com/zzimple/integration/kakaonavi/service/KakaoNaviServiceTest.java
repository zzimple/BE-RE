package com.zzimple.integration.kakaonavi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzimple.integration.kakaonavi.dto.request.RouteByCoordinatesRequest;
import com.zzimple.integration.kakaonavi.dto.response.KakaoRouteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class KakaoNaviServiceTest {

  @Mock
  private RestTemplate restTemplate;

  private KakaoNaviService kakaoNaviService;

  private static final String KAKAO_ROUTE_JSON = """
      {
        "routes": [
          {
            "summary": {
              "distance": 12345,
              "duration": 1800,
              "origin": {"x": 126.97, "y": 37.56},
              "destination": {"x": 127.02, "y": 37.50}
            },
            "sections": [
              {
                "roads": [
                  {"vertexes": [126.97, 37.56, 126.99, 37.53, 127.02, 37.50]}
                ]
              }
            ]
          }
        ]
      }
      """;

  @BeforeEach
  void setUp() {
    kakaoNaviService = new KakaoNaviService(restTemplate, new ObjectMapper());
    ReflectionTestUtils.setField(kakaoNaviService, "kakaoRestApiKey", "test-key");
  }

  @Test
  @DisplayName("TM 좌표 요청을 WGS84로 변환해 카카오 API를 호출하고 경로 요약을 반환한다")
  void getRouteByCoordinates() {
    when(restTemplate.exchange(
        Mockito.anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(KAKAO_ROUTE_JSON));

    RouteByCoordinatesRequest request = new RouteByCoordinatesRequest(
        953898.0, 1952250.0,   // 서울시청 근방 (TM)
        958000.0, 1948000.0);  // 인근 좌표 (TM)

    KakaoRouteResponse response = kakaoNaviService.getRouteByCoordinates(request);

    assertThat(response.getDistance()).isEqualTo(12345);
    assertThat(response.getDuration()).isEqualTo(1800);
    assertThat(response.getMarks()).hasSize(2);
    assertThat(response.getRoutePoints()).hasSize(3);

    // 카카오 API에는 WGS84 "경도,위도" 형식으로 전달되어야 한다
    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(restTemplate).exchange(
        urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    assertThat(urlCaptor.getValue()).contains("origin=126.").contains("destination=127.");
  }
}
