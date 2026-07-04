package com.zzimple.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 게이트웨이가 JWT 검증 후 주입한 X-User-* 헤더를 읽어 SecurityContext를 구성한다.
 *
 * 신뢰 경계: 이 필터는 요청이 게이트웨이를 통해서만 들어온다는 전제 위에 동작한다.
 * 게이트웨이는 클라이언트가 보낸 X-User-* 헤더를 항상 제거(strip)한 뒤
 * 검증된 값으로 다시 채워서 전달하므로, 헤더 위조는 게이트웨이에서 차단된다.
 * (모든 도메인 서비스가 동일한 구현을 공유하도록 common에 둔다 —
 * 서비스마다 복사하면 파싱 차이가 곧 권한 상승 버그가 된다)
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

  public static final String HEADER_USER_ID = "X-User-Id";
  public static final String HEADER_LOGIN_ID = "X-User-Login-Id";
  public static final String HEADER_USER_ROLE = "X-User-Role";
  public static final String HEADER_STORE_ID = "X-Store-Id";
  public static final String HEADER_OWNER_ID = "X-Owner-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String userIdHeader = request.getHeader(HEADER_USER_ID);
    String role = request.getHeader(HEADER_USER_ROLE);

    if (userIdHeader != null && role != null
        && SecurityContextHolder.getContext().getAuthentication() == null) {

      Long userId = parseLongOrNull(userIdHeader);
      if (userId != null) {
        GatewayUserPrincipal principal = new GatewayUserPrincipal(
            userId,
            request.getHeader(HEADER_LOGIN_ID),
            role,
            parseLongOrNull(request.getHeader(HEADER_STORE_ID)),
            parseLongOrNull(request.getHeader(HEADER_OWNER_ID))
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private Long parseLongOrNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
