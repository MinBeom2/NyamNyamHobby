package com.ssafy.nyamnyam.domain.place.entity;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceCategory category;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "opening_hours", length = 200)
    private String openingHours;

    @Column(name = "fee_text", length = 100)
    private String feeText;

    @Column(name = "external_url", length = 500)
    private String externalUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlaceStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(name = "hobby_places", joinColumns = @JoinColumn(name = "place_id"), inverseJoinColumns = @JoinColumn(name = "hobby_id"))
    private List<Hobby> hobbies = new ArrayList<>();

    @Builder
    public Place(Region region, String name, PlaceCategory category, String address,
            BigDecimal lat, BigDecimal lng, String openingHours, String feeText,
            String externalUrl, String thumbnailUrl, String source, String sourceRef,
            LocalDateTime sourceUpdatedAt, PlaceStatus status,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.region = region;
        this.name = name;
        this.category = category;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.openingHours = openingHours;
        this.feeText = feeText;
        this.externalUrl = externalUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.source = source;
        this.sourceRef = sourceRef;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.status = status != null ? status : PlaceStatus.PENDING_REVIEW;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    /**
     * 외부 출처(카카오 로컬 등)로부터 장소 최신 정보 갱신
     */
    public void updateFromSource(String name, String address, BigDecimal lat, BigDecimal lng,
                                 String externalUrl, PlaceCategory category, Region region) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (address != null && !address.isBlank()) {
            this.address = address;
        }
        if (lat != null) {
            this.lat = lat;
        }
        if (lng != null) {
            this.lng = lng;
        }
        if (externalUrl != null && !externalUrl.isBlank()) {
            this.externalUrl = externalUrl;
        }
        if (category != null) {
            this.category = category;
        }
        if (region != null) {
            this.region = region;
        }
        this.sourceUpdatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 대표 썸네일 이미지 URL 업데이트
     */
    public void updateThumbnailUrl(String thumbnailUrl) {
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            this.thumbnailUrl = thumbnailUrl;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * N:M 취미 연결 (중복 방지)
     * @return 신규 추가되었으면 true
     */
    public boolean addHobbyIfAbsent(Hobby hobby) {
        if (hobby == null) return false;
        boolean exists = this.hobbies.stream().anyMatch(h -> h.getId().equals(hobby.getId()));
        if (!exists) {
            this.hobbies.add(hobby);
            this.updatedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * '정보 확인 필요' 플래그 계산
     * - 운영 시간이 비어있거나 90일 넘게 갱신 안 됨 (PLACE-B02 스펙)
     */
    public boolean computeNeedsInfoCheck() {
        if (openingHours == null || openingHours.isBlank()) {
            return true;
        }
        LocalDateTime checkTime = sourceUpdatedAt != null ? sourceUpdatedAt : updatedAt;
        if (checkTime == null) {
            return true;
        }
        return checkTime.isBefore(LocalDateTime.now().minusDays(90));
    }
}
