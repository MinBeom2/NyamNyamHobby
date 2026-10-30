package com.ssafy.nyamnyam.domain.plan.repository;

import com.ssafy.nyamnyam.domain.plan.entity.PlanTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PlanTemplateRepository extends JpaRepository<PlanTemplate, Long> {
    @EntityGraph(attributePaths = {"items", "weekTitles"})
    Optional<PlanTemplate> findByHobbyIdAndSkillLevelAndStatus(Long hobbyId, String skillLevel, String status);
    List<PlanTemplate> findByHobbyIdInAndStatus(Collection<Long> hobbyIds, String status);
}
