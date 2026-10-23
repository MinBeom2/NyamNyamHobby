package com.ssafy.nyamnyam.domain.hobby.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "hobby_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(HobbyTag.HobbyTagId.class)
public class HobbyTag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public HobbyTag(Hobby hobby, Tag tag) {
        this.hobby = hobby;
        this.tag = tag;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class HobbyTagId implements Serializable {
        private Long hobby;
        private Long tag;
    }
}
