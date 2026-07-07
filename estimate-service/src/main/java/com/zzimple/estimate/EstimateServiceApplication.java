package com.zzimple.estimate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// common 모듈의 GlobalExceptionHandler 등을 스캔하기 위해 com.zzimple 전체를 베이스로 잡는다
@SpringBootApplication(scanBasePackages = "com.zzimple")
@EnableFeignClients(basePackages = "com.zzimple.estimate.client")
@EnableJpaAuditing
public class EstimateServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EstimateServiceApplication.class, args);
  }
}
