package com.ssafy.nyamnyam.domain.user.dto.response;

import com.ssafy.nyamnyam.domain.user.entity.Region;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegionResponse {

    private Long id;
    private String code;
    private String sido;
    private String sigungu;

    public RegionResponse(Long id, String code, String sido, String sigungu) {
        this.id = id;
        this.code = code;
        this.sido = sido;
        this.sigungu = sigungu;
    }

    public static RegionResponse from(Region region) {
        if (region == null) {
            return null;
        }
        return new RegionResponse(region.getId(), region.getCode(), region.getSido(), region.getSigungu());
    }
}

