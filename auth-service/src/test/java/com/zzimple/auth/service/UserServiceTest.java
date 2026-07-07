package com.zzimple.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzimple.auth.client.OwnerClient;
import com.zzimple.auth.client.dto.StoreSummaryResponse;
import com.zzimple.auth.dto.request.LoginRequest;
import com.zzimple.auth.dto.request.UserSignUpRequest;
import com.zzimple.auth.entity.User;
import com.zzimple.auth.enums.UserRole;
import com.zzimple.auth.event.UserEventPublisher;
import com.zzimple.auth.jwt.JwtUtil;
import com.zzimple.auth.repository.UserRepository;
import com.zzimple.common.exception.CustomException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtUtil jwtUtil;
  @Mock private OwnerClient ownerClient;
  @Mock private UserEventPublisher userEventPublisher;

  @InjectMocks private UserService userService;

  private User user(UserRole role) {
    User user = User.builder()
        .loginId("login01")
        .password("$2a$10$stored")
        .userName("김테스트")
        .phoneNumber("010-1111-2222")
        .role(role)
        .build();
    ReflectionTestUtils.setField(user, "id", 7L);
    return user;
  }

  @Test
  @DisplayName("회원가입: 저장 후 user.updated 이벤트를 발행한다")
  void registerPublishesEvent() {
    UserSignUpRequest request = new UserSignUpRequest();
    request.setLoginId("login01");
    request.setPassword("pw");
    request.setUserName("김테스트");
    request.setPhoneNumber("010-1111-2222");
    request.setUserRole(UserRole.GUEST);

    when(userRepository.findByLoginId("login01")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("pw")).thenReturn("ENCODED");

    var response = userService.registerUser(request);

    assertThat(response.isSuccess()).isTrue();
    verify(userRepository).save(any(User.class));
    verify(userEventPublisher).publishUserUpdated(any(User.class));
  }

  @Test
  @DisplayName("GUEST 로그인: owner-service 호출 없이 storeId 없는 토큰을 발급한다")
  void guestLoginSkipsOwnerService() {
    LoginRequest request = new LoginRequest();
    request.setLoginId("login01");
    request.setPassword("pw");
    HttpServletResponse response = new MockHttpServletResponse();

    User guest = user(UserRole.GUEST);
    when(userRepository.findByLoginId("login01")).thenReturn(Optional.of(guest));
    when(passwordEncoder.matches("pw", "$2a$10$stored")).thenReturn(true);
    when(jwtUtil.createAccessToken(eq("login01"), eq(7L), anyList())).thenReturn("ACCESS");
    when(jwtUtil.createRefreshToken("login01")).thenReturn("REFRESH");

    var result = userService.login(request, response);

    assertThat(result.getAccessToken()).isEqualTo("ACCESS");
    verify(ownerClient, never()).getStoreByOwnerUserId(anyLong());
  }

  @Test
  @DisplayName("OWNER 로그인: owner-service에서 현재 storeId/ownerId를 받아 토큰 클레임에 넣는다")
  void ownerLoginResolvesStoreClaims() {
    LoginRequest request = new LoginRequest();
    request.setLoginId("login01");
    request.setPassword("pw");
    HttpServletResponse response = new MockHttpServletResponse();

    User owner = user(UserRole.OWNER);
    when(userRepository.findByLoginId("login01")).thenReturn(Optional.of(owner));
    when(passwordEncoder.matches("pw", "$2a$10$stored")).thenReturn(true);
    when(ownerClient.getStoreByOwnerUserId(7L))
        .thenReturn(new StoreSummaryResponse(42L, 10L, "가게"));
    when(jwtUtil.createAccessToken(eq("login01"), eq(7L), anyList(), eq(42L), eq(10L)))
        .thenReturn("OWNER_ACCESS");
    when(jwtUtil.createRefreshToken("login01")).thenReturn("REFRESH");

    var result = userService.login(request, response);

    assertThat(result.getAccessToken()).isEqualTo("OWNER_ACCESS");
    verify(ownerClient).getStoreByOwnerUserId(7L);
  }

  @Test
  @DisplayName("로그인: 비밀번호가 틀리면 거부된다")
  void loginRejectsWrongPassword() {
    LoginRequest request = new LoginRequest();
    request.setLoginId("login01");
    request.setPassword("wrong");

    User guest = user(UserRole.GUEST);
    when(userRepository.findByLoginId("login01")).thenReturn(Optional.of(guest));
    when(passwordEncoder.matches("wrong", "$2a$10$stored")).thenReturn(false);

    assertThatThrownBy(() -> userService.login(request, new MockHttpServletResponse()))
        .isInstanceOf(CustomException.class);
  }
}
