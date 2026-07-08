package com.zzimple.integration.coordinate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoordinateConverterTest {

  @Test
  @DisplayName("EPSG:5179 TM 좌표를 WGS84 위경도로 변환한다 (서울시청 근방)")
  void convertSeoulCityHall() {
    // 서울시청 근방의 EPSG:5179 좌표
    double entX = 953898.0;
    double entY = 1952250.0;

    double[] latLng = CoordinateConverter.convertToWGS84(entX, entY);

    // 반환 형식은 [lat, lng]
    assertThat(latLng[0]).isBetween(37.0, 38.0);  // 위도: 서울 근방
    assertThat(latLng[1]).isBetween(126.5, 127.5); // 경도: 서울 근방
  }

  @Test
  @DisplayName("부산 근방 좌표도 대한민국 위경도 범위로 변환된다")
  void convertBusanCoordinate() {
    // 부산 근방의 EPSG:5179 좌표
    double entX = 1145000.0;
    double entY = 1685000.0;

    double[] latLng = CoordinateConverter.convertToWGS84(entX, entY);

    assertThat(latLng[0]).isBetween(34.5, 35.8);
    assertThat(latLng[1]).isBetween(128.5, 129.5);
  }
}
