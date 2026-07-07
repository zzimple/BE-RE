package com.zzimple.staff.config;

import com.zzimple.common.security.GatewayHeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * staff-service 보안 설정.
 * JWT 검증은 게이트웨이에서 수행되고, 이 서비스는 게이트웨이가 주입한
 * X-User-* 헤더(공유 GatewayHeaderAuthenticationFilter)만 신뢰한다.
 * 엔드포인트별 역할 규칙은 @PreAuthorize로 선언한다.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter() {
    return new GatewayHeaderAuthenticationFilter();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            // 내부 서비스 간 API (Docker 내부 네트워크에서만 접근)
            .requestMatchers("/internal/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(gatewayHeaderAuthenticationFilter(),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
