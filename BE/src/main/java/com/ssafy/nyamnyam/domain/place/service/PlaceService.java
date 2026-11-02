package com.ssafy.nyamnyam.domain.place.service;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.place.dto.response.PlaceResponse;
import com.ssafy.nyamnyam.domain.place.entity.Place;
import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;
import com.ssafy.nyamnyam.domain.place.entity.PlaceStatus;
import com.ssafy.nyamnyam.domain.place.repository.PlaceRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;

    /**
     * 활동 장소 목록 검색 (공개, PUBLISHED 장소만 노출)
     */
    public List<PlaceResponse> searchPlaces(Long regionId, Long hobbyId, PlaceCategory category) {
        Specification<Place> spec = placeSpec(regionId, hobbyId, category);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        List<Place> places = placeRepository.findAll(spec, sort);
        return places.stream()
                .map(PlaceResponse::from)
                .toList();
    }

    private static Specification<Place> placeSpec(Long regionId, Long hobbyId, PlaceCategory category) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), PlaceStatus.PUBLISHED));

            if (regionId != null) {
                predicates.add(cb.equal(root.get("region").get("id"), regionId));
            }

            if (hobbyId != null) {
                Join<Place, Hobby> hobbyJoin = root.join("hobbies", JoinType.INNER);
                predicates.add(cb.equal(hobbyJoin.get("id"), hobbyId));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

