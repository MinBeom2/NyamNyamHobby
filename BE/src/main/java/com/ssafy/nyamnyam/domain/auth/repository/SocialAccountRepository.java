package com.ssafy.nyamnyam.domain.auth.repository;

import com.ssafy.nyamnyam.domain.auth.entity.AuthProvider;
import com.ssafy.nyamnyam.domain.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    boolean existsByUserId(Long userId);

    Optional<SocialAccount> findByUserId(Long userId);
}
