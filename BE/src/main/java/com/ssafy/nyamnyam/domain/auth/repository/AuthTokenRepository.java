package com.ssafy.nyamnyam.domain.auth.repository;

import com.ssafy.nyamnyam.domain.auth.entity.AuthToken;
import com.ssafy.nyamnyam.domain.auth.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHashAndType(String tokenHash, TokenType type);

    Optional<AuthToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(Long userId, TokenType type);

    @Modifying
    @Query("DELETE FROM AuthToken t WHERE t.user.id = :userId AND t.type = :type")
    void deleteAllByUserIdAndType(@Param("userId") Long userId, @Param("type") TokenType type);

    @Modifying
    @Query("DELETE FROM AuthToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
