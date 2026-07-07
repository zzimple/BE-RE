package com.zzimple.auth.config;

import com.zzimple.common.security.GatewayHeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * auth-service 보안 설정.
 * 이 서비스는 JWT '발급'만 담당한다. 검증은 게이트웨이가 수행하고,
 * 인증이 필요한 엔드포인트(비밀번호/이메일 변경 등)는
 * 게이트웨이가 주입한 X-User-* 헤더(공유 필터)를 신뢰한다.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

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
            // 가입/로그인/중복검사/토큰 재발급은 토큰 없이 접근
            .requestMatchers("/users/register", "/users/login",
                "/users/login-id-duplicate-check", "/users/refresh-token").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(gatewayHeaderAuthenticationFilter(),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
