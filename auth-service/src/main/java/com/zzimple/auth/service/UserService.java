package com.zzimple.auth.service;

import com.zzimple.auth.client.OwnerClient;
import com.zzimple.auth.client.dto.StoreSummaryResponse;
import com.zzimple.auth.event.UserEventPublisher;
import com.zzimple.common.exception.CustomException;
import com.zzimple.common.exception.GlobalErrorCode;
import feign.FeignException;
import com.zzimple.auth.dto.response.ProfileResponse;
import com.zzimple.auth.enums.UserRole;
import com.zzimple.auth.exception.UserErrorCode;
import com.zzimple.auth.jwt.JwtUtil;
import com.zzimple.auth.dto.request.UserLoginIdCheckRequest;
import com.zzimple.auth.dto.request.LoginRequest;
import com.zzimple.auth.dto.request.UserSignUpRequest;
import com.zzimple.auth.dto.response.LoginIdCheckResponse;
import com.zzimple.auth.dto.response.LoginResponse;
import com.zzimple.auth.dto.response.SignUpResponse;
import com.zzimple.auth.entity.User;
import com.zzimple.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private static final HexFormat HEX_FORMAT = HexFormat.of();

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final OwnerClient ownerClient;
  private final UserEventPublisher userEventPublisher;

  public LoginIdCheckResponse checkLoginIdDuplicate(UserLoginIdCheckRequest request) {
    // 로그인 아이디(이메일) 존재 여부 확인
    boolean isDuplicate = userRepository.findByLoginId(request.getLoginId()).isPresent();

    // 응답 생성
    return LoginIdCheckResponse.builder()
        .isDuplicate(isDuplicate)
        .build();
  }

  @Transactional
  public SignUpResponse registerUser(UserSignUpRequest request) {

    // 1. 중복 회원 검사
    if (userRepository.findByLoginId(request.getLoginId()).isPresent()) {
      log.warn("[회원가입 실패] 이미 존재하는 아이디 - ID: {}", request.getLoginId());
      throw new CustomException(UserErrorCode.DUPLICATE_LOGIN_ID);
    }

    try {
      // 필수 필드 유효성 검증
      if (!StringUtils.hasText(request.getLoginId())) {
        throw new CustomException(UserErrorCode.INVALID_INPUT);
      }
      if (!StringUtils.hasText(request.getPhoneNumber())) {
        throw new CustomException(UserErrorCode.INVALID_INPUT);
      }
      // 2. 비밀번호 암호화
      String encodedPassword = passwordEncoder.encode(request.getPassword());

      String email = StringUtils.hasText(request.getEmail()) ? request.getEmail() : null;

      // 3. 사용자 엔티티 생성
      User user =
          User.builder()
              .userName(request.getUserName())
              .phoneNumber(request.getPhoneNumber())
              .loginId(request.getLoginId())
              .password(encodedPassword)
              .email(email)
              .role(request.getUserRole())
              .build();

      // 4. 데이터베이스에 저장
      userRepository.save(user);

      // 5. user.updated 이벤트 발행 (표시용 read model 갱신)
      userEventPublisher.publishUserUpdated(user);

      log.info("[회원가입 성공] ID: {}, 이름: {}", user.getLoginId(), user.getUserName());

      return SignUpResponse.builder()
          .isSuccess(true)
          .build();
    } catch (Exception e) {
      log.error("[회원가입 오류] ID: {}, 이유: {}", request.getLoginId(), e.getMessage());
      throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  // 로그인
  @Transactional
  public LoginResponse login(LoginRequest request, HttpServletResponse response) {
    try {
      // 1. 로그인 아이디로 사용자 찾기
      User user = userRepository.findByLoginId(request.getLoginId())
          .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

      // 2. 비밀번호 확인
      if (!isPasswordValid(request.getPassword(), user)) {
        throw new CustomException(UserErrorCode.INVALID_PASSWORD);
      }

      List<String> roles = List.of(user.getRole().name());
      String accessToken;

      Long storeId = null;
      Long ownerId = null;

      // 사장님일 경우에만 storeId, ownerId 추출 (1번 조회로 변경) 🔥
      if (user.getRole() == UserRole.OWNER) {
        // 갓 발급되는 토큰의 storeId/ownerId는 '현재' 값이어야 하므로 동기 Feign 호출.
        // owner-service가 죽어있으면 OWNER 로그인은 실패한다 (잘못된 클레임 발급 방지).
        StoreSummaryResponse store = fetchStoreForOwner(user.getId());
        storeId = store.getStoreId();
        ownerId = store.getOwnerId();
      }

      // 3. 토큰 생성
      accessToken = (storeId != null)
          ? jwtUtil.createAccessToken(user.getLoginId(), user.getId(), roles, storeId, ownerId)
          : jwtUtil.createAccessToken(user.getLoginId(), user.getId(), roles); // 오버로드 버전 필요

      String refreshToken = jwtUtil.createRefreshToken(user.getLoginId());

      user.setRefreshToken(refreshToken);
      userRepository.save(user);

      ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
          .httpOnly(false)
          .secure(false) // 배포 시 true
          .sameSite("Lax")
          .path("/")
          .maxAge(Duration.ofHours(1)) // or jwtUtil.getAccessTokenExpirySeconds()
          .build();

      // 7. HttpOnly 쿠키로 리프레시 토큰 내려주기
      long expirySeconds = jwtUtil.getRefreshTokenExpirySeconds();
      // (JwtUtil에 리프레시 토큰 만료 기간을 초 단위로 반환하는 메서드가 있다고 가정)
      ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
          .httpOnly(true)
          .secure(false) // 배포 시 true로 변경 (HTTPS)
          .sameSite("Lax")
          .path("/")
          .maxAge(Duration.ofSeconds(expirySeconds))
          .build();

      response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

      // 5. 로그인 성공 응답 생성
      log.info("[로그인 성공] ID: {}, 이름: {}", user.getLoginId(), user.getUserName());

      return LoginResponse.builder()
          .accessToken(accessToken)
          .build();
    } catch (CustomException e) {
      log.warn("[로그인 실패 - CustomException] ID: {}, 이유: {}", request.getLoginId(), e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("[로그인 실패 - 서버 오류] ID: {}, 에러: {}", request.getLoginId(), e.getMessage(), e);
      throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  // 비밀번호 변경
  @Transactional
  public void updatePassword(Long userId, String currentPassword, String newPassword) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

    if (!isPasswordValid(currentPassword, user)) {
      throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
    }

    String encodedPassword = passwordEncoder.encode(newPassword); // 암호화
    user.updatePassword(encodedPassword);
  }

  // 이메일 변경
  @Transactional
  public void updateEmail(Long userId, String email) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

    if (user.getEmail() != null && user.getEmail().equals(email)) {
      return;
    }

    if (userRepository.existsByEmail(email)) {
      throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
    }

    user.updateEmail(email);

    // 표시용 read model 갱신
    userEventPublisher.publishUserUpdated(user);
  }

  private StoreSummaryResponse fetchStoreForOwner(Long userId) {
    try {
      return ownerClient.getStoreByOwnerUserId(userId);
    } catch (FeignException.NotFound e) {
      throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Transactional(readOnly = true)
  public ProfileResponse getProfile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

    return new ProfileResponse(
        user.getId(),
        user.getUserName(),
        user.getPhoneNumber(),
        user.getEmail(),
        user.getLoginId()
    );
  }

  private boolean isPasswordValid(String rawPassword, User user) {
    String storedPassword = user.getPassword();

    if (!StringUtils.hasText(storedPassword)) {
      return false;
    }

    if (isBcryptHash(storedPassword)) {
      return passwordEncoder.matches(rawPassword, storedPassword);
    }

    if (isSha256Hex(storedPassword) && sha256(rawPassword).equalsIgnoreCase(storedPassword)) {
      user.updatePassword(passwordEncoder.encode(rawPassword));
      userRepository.save(user);
      log.info("[비밀번호 마이그레이션] ID: {} 계정 비밀번호를 SHA-256에서 BCrypt로 전환했습니다.", user.getLoginId());
      return true;
    }

    if (storedPassword.equals(rawPassword)) {
      user.updatePassword(passwordEncoder.encode(rawPassword));
      userRepository.save(user);
      log.warn("[비밀번호 마이그레이션] ID: {} 계정 비밀번호를 평문에서 BCrypt로 전환했습니다.", user.getLoginId());
      return true;
    }

    return false;
  }

  private boolean isBcryptHash(String password) {
    return password.startsWith("$2a$")
        || password.startsWith("$2b$")
        || password.startsWith("$2y$");
  }

  private boolean isSha256Hex(String password) {
    if (password.length() != 64) {
      return false;
    }

    String normalizedPassword = password.toLowerCase(Locale.ROOT);
    for (int i = 0; i < normalizedPassword.length(); i++) {
      char ch = normalizedPassword.charAt(i);
      boolean isHexChar = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f');
      if (!isHexChar) {
        return false;
      }
    }
    return true;
  }

  private String sha256(String rawPassword) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashedBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
      return HEX_FORMAT.formatHex(hashedBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available", e);
    }
  }
}
