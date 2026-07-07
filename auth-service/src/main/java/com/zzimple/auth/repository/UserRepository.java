package com.zzimple.auth.repository;

import com.zzimple.auth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  // 로그인 아이디로 찾기
  Optional<User> findByLoginId(String loginId);
  Optional<User> findByPhoneNumber(String phoneNumber);
  Optional<User> findById(Long userId);
  boolean existsByEmail(String email);
}