package com.zzimple.integration.kakaonavi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zzimple.integration.kakaonavi.dto.response.KakaoRouteResponse;
import com.zzimple.integration.kakaonavi.service.KakaoNaviService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KakaoNaviController.class)
@AutoConfigureMockMvc(addFilters = false)
class KakaoNaviControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private KakaoNaviService kakaoNaviService;

  @Test
  @DisplayName("POST /kakao-navi/route: 좌표 기반 경로 조회에 성공하면 BaseResponse로 감싸 반환한다")
  void getRouteByCoordinates() throws Exception {
    when(kakaoNaviService.getRouteByCoordinates(any()))
        .thenReturn(new KakaoRouteResponse(5000, 900, List.of(), List.of()));

    mockMvc.perform(post("/kakao-navi/route")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "originEntX": 953898.0,
                  "originEntY": 1952250.0,
                  "destEntX": 958000.0,
                  "destEntY": 1948000.0
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.distance").value(5000))
        .andExpect(jsonPath("$.data.duration").value(900));
  }

  @Test
  @DisplayName("POST /kakao-navi/route: 좌표가 누락되면 400을 반환한다")
  void missingCoordinatesReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/kakao-navi/route")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"originEntX\": 953898.0}"))
        .andExpect(status().isBadRequest());
  }
}
