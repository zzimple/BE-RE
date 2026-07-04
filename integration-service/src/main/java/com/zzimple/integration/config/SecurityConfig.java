package com.zzimple.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * integration-service는 게이트웨이 뒤에서만 접근되는 것을 전제로 한다.
 * 인증/인가는 게이트웨이(JWT 검증)에서 수행되고, 이 서비스의 엔드포인트는
 * 외부 API 프록시 역할만 하므로 자체 인증을 두지 않는다.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }
}
