package com.zzimple.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseResponseTest {

  @Test
  @DisplayName("성공 응답은 success=true와 데이터를 담는다")
  void successResponse() {
    BaseResponse<String> response = BaseResponse.success("payload");

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getData()).isEqualTo("payload");
    assertThat(response.getMessage()).isNotBlank();
  }

  @Test
  @DisplayName("실패 응답은 success=false와 에러 코드를 담는다")
  void failureResponse() {
    BaseResponse<Object> response = BaseResponse.failure("G001", "유효하지 않은 입력입니다.");

    assertThat(response.isSuccess()).isFalse();
    assertThat(response.getCode()).isEqualTo("G001");
    assertThat(response.getData()).isNull();
  }
}
