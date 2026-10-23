package com.ssafy.nyamnyam.domain.shortform.repository;

import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShortRepository extends JpaRepository<Short, Long> {

    Optional<Short> findByYoutubeVideoId(String youtubeVideoId);

    List<Short> findByStatus(ShortStatus status);

    @Query("SELECT s FROM Short s " +
           "LEFT JOIN FETCH s.hobby h " +
           "LEFT JOIN FETCH h.category " +
           "WHERE s.status = :status")
    List<Short> findWithHobbyByStatus(@Param("status") ShortStatus status);

    @Query("SELECT s FROM Short s " +
           "LEFT JOIN FETCH s.hobby h " +
           "LEFT JOIN FETCH h.category " +
           "WHERE s.status = :status AND s.id NOT IN :excludedIds")
    List<Short> findCandidatesExcluding(@Param("status") ShortStatus status,
                                       @Param("excludedIds") Collection<Long> excludedIds);
}
