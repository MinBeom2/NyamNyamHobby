package com.ssafy.nyamnyam.domain.hobby.repository;

import com.ssafy.nyamnyam.domain.hobby.entity.Tag;
import com.ssafy.nyamnyam.domain.hobby.entity.TagGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    List<Tag> findByTagGroup(TagGroup tagGroup);
}
