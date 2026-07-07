package com.zzimple.internal;

import com.zzimple.user.entity.User;
import com.zzimple.user.enums.UserRole;
import com.zzimple.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [임시 - strangler] 추출된 서비스(staff-service 등)가 사용자 정보를 조회하는 내부 API.
 * auth-service 추출(Phase 4) 시 auth-service로 이관되고 이 컨트롤러는 삭제된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

  private final UserRepository userRepository;

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id) {
    return userRepository.findById(id)
        .map(u -> ResponseEntity.ok(toSummary(u)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/by-phone")
  public ResponseEntity<Map<String, Object>> getUserByPhone(@RequestParam("phone") String phone) {
    return userRepository.findByPhoneNumber(phone)
        .map(u -> ResponseEntity.ok(toSummary(u)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/batch")
  public ResponseEntity<List<Map<String, Object>>> getUsersByIds(
      @RequestParam("ids") List<Long> ids) {
    return ResponseEntity.ok(
        userRepository.findAllById(ids).stream().map(this::toSummary).toList());
  }

  /**
   * 사장 회원가입 시 owner-service가 호출하는 기본 User 생성 API.
   * password는 owner-service에서 이미 인코딩된 값을 받는다.
   * auth-service 추출(Phase 4) 시 동일 계약으로 auth-service가 제공한다.
   */
  @PostMapping
  public ResponseEntity<Map<String, Object>> createUser(
      @RequestBody Map<String, String> request) {
    User user = User.builder()
        .loginId(request.get("loginId"))
        .password(request.get("encodedPassword"))
        .userName(request.get("userName"))
        .phoneNumber(request.get("phoneNumber"))
        .email(request.get("email"))
        .role(UserRole.valueOf(request.getOrDefault("role", "OWNER")))
        .build();
    userRepository.save(user);
    return ResponseEntity.ok(toSummary(user));
  }

  private Map<String, Object> toSummary(User user) {
    return Map.of(
        "id", user.getId(),
        "userName", user.getUserName(),
        "loginId", user.getLoginId(),
        "phoneNumber", user.getPhoneNumber(),
        "email", user.getEmail() == null ? "" : user.getEmail(),
        "role", user.getRole().name()
    );
  }
}
