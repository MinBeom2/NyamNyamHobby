package com.ssafy.nyamnyam.domain.place.service;

import com.ssafy.nyamnyam.domain.place.dto.response.SidoResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.RegionResponse;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    private static final List<SidoResponse> SIDO_LIST = List.of(
            new SidoResponse("서울특별시", true),
            new SidoResponse("부산광역시", false),
            new SidoResponse("대구광역시", false),
            new SidoResponse("인천광역시", false),
            new SidoResponse("광주광역시", false),
            new SidoResponse("대전광역시", false),
            new SidoResponse("울산광역시", false),
            new SidoResponse("세종특별자치시", false),
            new SidoResponse("경기도", false),
            new SidoResponse("강원특별자치도", false),
            new SidoResponse("충청북도", false),
            new SidoResponse("충청남도", false),
            new SidoResponse("전북특별자치도", false),
            new SidoResponse("전라남도", false),
            new SidoResponse("경상북도", false),
            new SidoResponse("경상남도", false),
            new SidoResponse("제주특별자치도", false)
    );

    /** 시·도 목록 조회 (MVP는 서울특별시만 enabled: true) */
    public List<SidoResponse> getSidoList() {
        return SIDO_LIST;
    }

    /** 시·군·구 목록 조회 */
    public List<RegionResponse> getSigunguList(String sido) {
        return regionRepository.findBySidoOrderBySigunguAsc(sido).stream()
                .map(RegionResponse::from)
                .toList();
    }
}

