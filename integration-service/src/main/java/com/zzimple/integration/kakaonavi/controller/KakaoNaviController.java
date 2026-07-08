package com.zzimple.integration.kakaonavi.controller;

import com.zzimple.common.dto.BaseResponse;
import com.zzimple.integration.kakaonavi.dto.request.RouteByCoordinatesRequest;
import com.zzimple.integration.kakaonavi.dto.response.KakaoRouteResponse;
import com.zzimple.integration.kakaonavi.service.KakaoNaviService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kakao-navi")
public class KakaoNaviController {

  private final KakaoNaviService kakaoNaviService;

  /**
   * 좌표 기반 경로 조회 (내부 서비스 간 호출용).
   * 기존 GET /kakao-navi/route/{estimateNo}는 estimate-service의
   * GET /estimates/{estimateNo}/route 로 대체된다.
   */
  @PostMapping("/route")
  public ResponseEntity<BaseResponse<KakaoRouteResponse>> getRouteByCoordinates(
      @Valid @RequestBody RouteByCoordinatesRequest request
  ) {
    KakaoRouteResponse response = kakaoNaviService.getRouteByCoordinates(request);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(BaseResponse.success(response));
  }
}
