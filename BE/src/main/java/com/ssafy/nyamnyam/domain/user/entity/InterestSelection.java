package com.ssafy.nyamnyam.domain.user.entity;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 내 취미 선택(최대 5개). 취미 찾기의 '관심 저장'(user_hobbies SAVED, 즐겨찾기)과는 별개로
 * 사용자가 실제로 수행하겠다고 고른 취미를 담는다. 온보딩과 마이페이지 '내 취미 선택'이 함께 쓴다.
 */
@Entity
@Table(name = "interest_selections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestSelection {

    @EmbeddedId
    private InterestSelectionId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("hobbyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public InterestSelection(User user, Hobby hobby, LocalDateTime createdAt) {
        this.id = new InterestSelectionId(user.getId(), hobby.getId());
        this.user = user;
        this.hobby = hobby;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
