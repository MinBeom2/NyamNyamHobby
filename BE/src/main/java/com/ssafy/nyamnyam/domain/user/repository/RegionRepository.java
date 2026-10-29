package com.ssafy.nyamnyam.domain.user.repository;

import com.ssafy.nyamnyam.domain.user.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByCode(String code);
}

