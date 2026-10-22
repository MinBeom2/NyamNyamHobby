package com.ssafy.nyamnyam.domain.auth.repository;

import com.ssafy.nyamnyam.domain.auth.entity.AuthToken;
import com.ssafy.nyamnyam.domain.auth.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHashAndType(String tokenHash, TokenType type);

    Optional<AuthToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(Long userId, TokenType type);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM AuthToken t WHERE t.user.id = :userId AND t.type = :type")
    void deleteAllByUserIdAndType(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("type") TokenType type
    );
}
