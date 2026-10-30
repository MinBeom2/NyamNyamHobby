package com.ssafy.nyamnyam.domain.hobby.repository;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HobbyRepository extends JpaRepository<Hobby, Long> {
    @Modifying
    @Query("UPDATE Hobby h SET h.saveCount = CASE WHEN h.saveCount + :delta < 0 THEN 0 ELSE h.saveCount + :delta END WHERE h.id = :id")
    void adjustSaveCount(@Param("id") Long id, @Param("delta") int delta);
    Optional<Hobby> findByName(String name);
    List<Hobby> findByStatus(HobbyStatus status);

    @Query("SELECT DISTINCT h FROM Hobby h " +
           "LEFT JOIN FETCH h.category " +
           "LEFT JOIN FETCH h.hobbyTags ht " +
           "LEFT JOIN FETCH ht.tag " +
           "WHERE h.status = :status")
    List<Hobby> findAllWithTagsByStatus(HobbyStatus status);
}
