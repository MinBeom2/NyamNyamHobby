package com.ssafy.nyamnyam.domain.user.repository;

import com.ssafy.nyamnyam.domain.user.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByCode(String code);

    List<Region> findBySidoOrderBySigunguAsc(String sido);

    Optional<Region> findBySidoAndSigungu(String sido, String sigungu);
}
