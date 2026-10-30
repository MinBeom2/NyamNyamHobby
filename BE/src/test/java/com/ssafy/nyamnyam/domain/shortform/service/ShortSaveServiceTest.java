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

    private Short video(ShortStatus status) {
        return Short.builder().youtubeVideoId("abcdefghijk").title("취미 숏폼").status(status).build();
    }
}
