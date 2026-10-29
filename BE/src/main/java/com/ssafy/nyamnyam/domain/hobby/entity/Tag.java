package com.ssafy.nyamnyam.domain.hobby.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_group", nullable = false, length = 10)
    private TagGroup tagGroup;

    @Builder
    public Tag(String name, TagGroup tagGroup) {
        this.name = name;
        this.tagGroup = tagGroup != null ? tagGroup : TagGroup.SUBJECT;
    }
}
