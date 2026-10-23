package com.ssafy.nyamnyam.domain.hobby.repository;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HobbyTagRepository extends JpaRepository<HobbyTag, HobbyTag.HobbyTagId> {
    List<HobbyTag> findByHobby(Hobby hobby);
    List<HobbyTag> findByHobbyId(Long hobbyId);
}
