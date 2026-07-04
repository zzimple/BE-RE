package com.zzimple.integration.kakaonavi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TM(EPSG:5179) 좌표 기반 경로 조회 요청.
 * estimate-service가 견적의 출발/도착 주소 좌표(entX/entY)를 해석해 전달한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RouteByCoordinatesRequest {

  @NotNull
  private Double originEntX;

  @NotNull
  private Double originEntY;

  @NotNull
  private Double destEntX;

  @NotNull
  private Double destEntY;
}
