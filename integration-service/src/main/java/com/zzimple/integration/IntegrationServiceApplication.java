package com.zzimple.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// common 모듈의 GlobalExceptionHandler 등을 스캔하기 위해 com.zzimple 전체를 베이스로 잡는다
@SpringBootApplication(scanBasePackages = "com.zzimple")
public class IntegrationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(IntegrationServiceApplication.class, args);
  }
}
