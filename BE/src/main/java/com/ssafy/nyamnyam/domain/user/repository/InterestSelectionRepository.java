package com.ssafy.nyamnyam.domain.user.repository;

import com.ssafy.nyamnyam.domain.user.entity.InterestSelection;
import com.ssafy.nyamnyam.domain.user.entity.InterestSelectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterestSelectionRepository extends JpaRepository<InterestSelection, InterestSelectionId> {

    @Query("SELECT s FROM InterestSelection s JOIN FETCH s.hobby WHERE s.user.id = :userId ORDER BY s.createdAt ASC, s.hobby.id ASC")
    List<InterestSelection> findAllByUserIdWithHobby(@Param("userId") Long userId);
}
