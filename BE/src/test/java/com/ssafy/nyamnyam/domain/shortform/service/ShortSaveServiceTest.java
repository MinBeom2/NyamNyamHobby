package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortSummaryResponse.HobbyCount;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortSaveServiceTest {
    @Mock ShortRepository shorts;
    @Mock ShortSaveRepository saves;
    @InjectMocks ShortSaveService service;

    @Test void savesForCurrentUserOnly() {
        given(shorts.findByIdForUpdate(41L)).willReturn(Optional.of(video(ShortStatus.ACTIVE)));
        assertThat(service.save(1L, 41L).isSaved()).isTrue();
        verify(saves).save(1L, 41L);
    }

    @ParameterizedTest
    @EnumSource(value = ShortStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void refusesToSaveInactiveShort(ShortStatus status) {
        given(shorts.findByIdForUpdate(41L)).willReturn(Optional.of(video(status)));
        assertThatThrownBy(() -> service.save(1L, 41L)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SHORT_NOT_ACTIVE));
        verifyNoInteractions(saves);
    }

    @ParameterizedTest
    @EnumSource(ShortStatus.class)
    void canUnsaveEvenIfShortBecomesUnavailable(ShortStatus status) {
        given(shorts.findByIdForUpdate(41L)).willReturn(Optional.of(video(status)));
        assertThat(service.unsave(1L, 41L).isSaved()).isFalse();
        verify(saves).delete(1L, 41L);
    }

    @Test void nonexistentShortIsNotSavedOrDeleted() {
        assertThatThrownBy(() -> service.save(1L, 404L)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.unsave(1L, 404L)).isInstanceOf(CustomException.class);
        verifyNoInteractions(saves);
    }

    @Test void emptyListAndSummaryReturnZeroCounts() {
        given(saves.count(1L, null)).willReturn(0L);
        given(saves.findSaved(1L, null, 0, 20, false)).willReturn(List.of());
        given(saves.countByHobby(1L)).willReturn(List.of());
        var result = service.list(1L, null, 0, 20, "recent");
        assertThat(result.content()).isEmpty();
        assertThat(result.page().totalElements()).isZero();
        assertThat(result.page().totalPages()).isZero();
        assertThat(result.page().hasNext()).isFalse();
        assertThat(service.summary(1L).totalCount()).isZero();
    }

    @Test void pageMetadataUsesFilteredCountAndSupportsLargeOffsets() {
        given(saves.count(1L, 3L)).willReturn(6L);
        given(saves.findSaved(1L, 3L, 0, 5, false)).willReturn(List.of());
        var first = service.list(1L, 3L, 0, 5, "recent");
        assertThat(first.page().totalPages()).isEqualTo(2);
        assertThat(first.page().hasNext()).isTrue();
        given(saves.findSaved(1L, 3L, Integer.MAX_VALUE, 100, true)).willReturn(List.of());
        assertThat(service.list(1L, 3L, Integer.MAX_VALUE, 100, "oldest").page().hasNext()).isFalse();
    }

    @Test void summaryIncludesUnclassifiedSavesAndCountsBeyondIntegerRange() {
        given(saves.countByHobby(2L)).willReturn(List.of(
                new HobbyCount(3L, "클라이밍", Integer.MAX_VALUE), new HobbyCount(null, null, 2)));
        var summary = service.summary(2L);
        assertThat(summary.totalCount()).isEqualTo((long) Integer.MAX_VALUE + 2);
        assertThat(summary.hobbies()).hasSize(2);
        verify(saves).countByHobby(2L);
    }

    private Short video(ShortStatus status) {
        return Short.builder().youtubeVideoId("abcdefghijk").title("취미 숏폼").status(status).build();
    }
}
