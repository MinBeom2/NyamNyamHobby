package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.exploration.dto.response.ShortDetailResponse;
import com.ssafy.nyamnyam.domain.shortform.dto.response.ShortFeedResponse;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 상단 '숏폼 탐색'(자유 탐색). 추천 알고리즘·탐색 세션 없이 공개(ACTIVE) 숏폼 전체를 섞어서 차례로 보여준다.
 * 같은 seed면 같은 순서라 cursor(앞에서 몇 개 지났는지)로 이어 받을 수 있다. 끝나면 nextCursor가 null이고,
 * 화면은 새 seed로 처음부터 다시 받아 끝없이 이어 본다. 시청·반응을 기록하지 않아 맞춤 추천 점수에 영향이 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShortFeedService {

    public static final int MAX_SIZE = 20;

    private final ShortRepository shortRepository;
    private final ShortSaveRepository saveRepository;

    /**
     * @param userId 비회원이면 null (보관 여부는 모두 false)
     * @param seed   null이면 새로 만든다. 응답에 담아 돌려주므로 다음 요청에 그대로 보낸다
     * @param cursor 0부터. 이미 받은 영상 수
     */
    public ShortFeedResponse feed(Long userId, Long seed, int cursor, int size) {
        if (cursor < 0 || size < 1 || size > MAX_SIZE) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "cursor는 0 이상, size는 1~" + MAX_SIZE + "이어야 합니다.");
        }
        long usedSeed = seed != null ? seed : ThreadLocalRandom.current().nextLong(1, Integer.MAX_VALUE); // JS 숫자로 안전하게 오가는 범위
        List<Long> ids = new ArrayList<>(shortRepository.findIdsByStatus(ShortStatus.ACTIVE));
        Collections.shuffle(ids, new Random(usedSeed));

        int from = Math.min(cursor, ids.size());
        int to = Math.min(from + size, ids.size());
        List<Long> pageIds = ids.subList(from, to);
        Map<Long, Short> byId = pageIds.isEmpty() ? Map.of()
                : shortRepository.findWithHobbyByIdIn(pageIds).stream().collect(Collectors.toMap(Short::getId, Function.identity()));
        Set<Long> saved = userId == null || pageIds.isEmpty() ? Set.of() : saveRepository.findSavedShortIds(userId, pageIds);

        List<ShortDetailResponse> items = pageIds.stream().map(byId::get)
                .filter(s -> s != null && s.getStatus() == ShortStatus.ACTIVE)
                .map(s -> ShortDetailResponse.of(s, null, saved.contains(s.getId())))
                .toList();
        Integer nextCursor = to < ids.size() ? to : null;
        return new ShortFeedResponse(items, usedSeed, nextCursor, ids.size());
    }
}
