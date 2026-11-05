package com.ssafy.nyamnyam.domain.place.repository;

import com.ssafy.nyamnyam.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {
    Optional<Place> findBySourceAndSourceRef(String source, String sourceRef);
}
