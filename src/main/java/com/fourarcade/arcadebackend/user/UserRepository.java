package com.fourarcade.arcadebackend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    // 로그인 시 이메일로 유저 찾기
    Optional<User> findByEmail(String email);
}
