package com.zzimple.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 게이트웨이 전역 JWT 검증 필터.
 *
 * - accessToken 쿠키 또는 Authorization: Bearer 헤더에서 토큰을 읽는다 (모놀리스 JwtUtil과 동일한 규약)
 * - 서명/만료 검증 후 클레임을 X-User-* 헤더로 변환해 다운스트림에 전달한다
 * - 클라이언트가 직접 보낸 X-User-* 헤더는 항상 제거한다 (위조 방지)
 * - 공개 경로는 검증을 건너뛴다
 */
@Slf4j
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

  @Value("${jwt.secret-key}")
  private String secretKey;

  private final List<String> publicPaths;

  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public JwtAuthenticationGatewayFilter(
      @Value("${gateway.public-paths}") String publicPathsCsv) {
    this.publicPaths = List.of(publicPathsCsv.split("\\s*,\\s*"));
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    // 위조 방지: 클라이언트가 보낸 X-User-* 헤더는 무조건 제거
    ServerHttpRequest sanitized = exchange.getRequest().mutate()
        .headers(h -> {
          h.remove("X-User-Id");
          h.remove("X-User-Login-Id");
          h.remove("X-User-Role");
          h.remove("X-Store-Id");
          h.remove("X-Owner-Id");
        })
        .build();

    boolean isPublic = publicPaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    if (isPublic) {
      return chain.filter(exchange.mutate().request(sanitized).build());
    }

    String token = extractToken(exchange);
    if (token == null) {
      return unauthorized(exchange);
    }

    Claims claims;
    try {
      Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
      claims = Jwts.parserBuilder().setSigningKey(key).build()
          .parseClaimsJws(token).getBody();
    } catch (Exception e) {
      log.debug("JWT 검증 실패: {}", e.getMessage());
      return unauthorized(exchange);
    }

    Number userId = claims.get("userId", Number.class);
    if (userId == null) {
      // userId 클레임이 없는 구버전 토큰은 재로그인 필요
      return unauthorized(exchange);
    }

    @SuppressWarnings("unchecked")
    List<String> roles = claims.get("roles", List.class);
    String role = (roles != null && !roles.isEmpty()) ? roles.get(0) : null;
    if (role == null) {
      return unauthorized(exchange);
    }

    Number storeId = claims.get("storeId", Number.class);
    Number ownerId = claims.get("ownerId", Number.class);

    ServerHttpRequest enriched = sanitized.mutate()
        .header("X-User-Id", String.valueOf(userId.longValue()))
        .header("X-User-Login-Id", claims.getSubject())
        .header("X-User-Role", role)
        .headers(h -> {
          if (storeId != null) {
            h.set("X-Store-Id", String.valueOf(storeId.longValue()));
          }
          if (ownerId != null) {
            h.set("X-Owner-Id", String.valueOf(ownerId.longValue()));
          }
        })
        .build();

    return chain.filter(exchange.mutate().request(enriched).build());
  }

  private String extractToken(ServerWebExchange exchange) {
    // 1) Authorization: Bearer
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    // 2) accessToken 쿠키
    HttpCookie cookie = exchange.getRequest().getCookies().getFirst("accessToken");
    return cookie != null ? cookie.getValue() : null;
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    return -100; // 라우팅 전에 실행
  }
}
