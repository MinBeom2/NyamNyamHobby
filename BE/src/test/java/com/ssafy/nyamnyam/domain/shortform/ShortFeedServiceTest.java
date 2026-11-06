package com.ssafy.nyamnyam.domain.shortform;

import com.ssafy.nyamnyam.domain.exploration.dto.response.ShortDetailResponse;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import com.ssafy.nyamnyam.domain.shortform.service.ShortFeedService;
import com.ssafy.nyamnyam.global.error.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShortFeedServiceTest {

    @Mock ShortRepository shorts;
    @Mock ShortSaveRepository saves;
    @InjectMocks ShortFeedService service;

    @BeforeEach void setUp() {
        lenient().when(shorts.findIdsByStatus(ShortStatus.ACTIVE)).thenReturn(LongStream.rangeClosed(1, 25).boxed().toList());
        lenient().when(shorts.findWithHobbyByIdIn(anyCollection())).thenAnswer(call -> {
            Collection<Long> ids = call.getArgument(0);
            return ids.stream().map(ShortFeedServiceTest::active).toList();
        });
    }

    @Test void sameSeedContinuesTheSameShuffledOrderUntilTheEnd() {
        var first = service.feed(null, 42L, 0, 10);
        var second = service.feed(null, 42L, first.nextCursor(), 10);
        var third = service.feed(null, 42L, second.nextCursor(), 10);

        List<Long> seen = new ArrayList<>();
        for (var page : List.of(first, second, third)) page.items().stream().map(ShortDetailResponse::getShortId).forEach(seen::add);
        assertThat(seen).hasSize(25).doesNotHaveDuplicates();
        assertThat(seen).isNotEqualTo(LongStream.rangeClosed(1, 25).boxed().toList()); // 섞였다
        assertThat(first.nextCursor()).isEqualTo(10);
        assertThat(third.nextCursor()).isNull(); // 한 바퀴 끝
        assertThat(first.total()).isEqualTo(25);
        assertThat(service.feed(null, 42L, 0, 10).items()).extracting(ShortDetailResponse::getShortId)
                .containsExactlyElementsOf(seen.subList(0, 10));
    }

    @Test void createsSeedWhenMissingAndMarksSavedOnlyForMembers() {
        given(saves.findSavedShortIds(eq(7L), anyCollection())).willAnswer(call -> {
            Collection<Long> ids = call.getArgument(1);
            Set<Long> saved = new HashSet<>(ids);
            saved.removeIf(id -> id % 2 == 0);
            return saved;
        });

        var member = service.feed(7L, null, 0, 6);
        assertThat(member.seed()).isBetween(1L, (long) Integer.MAX_VALUE);
        assertThat(member.items()).allSatisfy(item -> assertThat(item.getIsSaved()).isEqualTo(item.getShortId() % 2 == 1));

        var guest = service.feed(null, member.seed(), 0, 6);
        assertThat(guest.items()).allSatisfy(item -> assertThat(item.getIsSaved()).isFalse());
        assertThat(guest.items()).extracting(ShortDetailResponse::getShortId)
                .containsExactlyElementsOf(member.items().stream().map(ShortDetailResponse::getShortId).toList());
    }

    @Test void rejectsInvalidPaging() {
        assertThatThrownBy(() -> service.feed(null, 1L, -1, 10)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.feed(null, 1L, 0, 21)).isInstanceOf(CustomException.class);
        verify(shorts, never()).findIdsByStatus(ShortStatus.ACTIVE);
    }

    private static Short active(Long id) {
        Short s = Short.builder().youtubeVideoId("v" + id).title("영상" + id).build();
        ReflectionTestUtils.setField(s, "id", id);
        ReflectionTestUtils.setField(s, "status", ShortStatus.ACTIVE);
        return s;
    }
}
