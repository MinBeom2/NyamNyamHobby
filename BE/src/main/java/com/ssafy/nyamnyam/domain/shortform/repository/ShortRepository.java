package com.ssafy.nyamnyam.domain.shortform.repository;

import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShortRepository extends JpaRepository<Short, Long> {

    Optional<Short> findByYoutubeVideoId(String youtubeVideoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Short s WHERE s.id = :id")
    Optional<Short> findByIdForUpdate(@Param("id") Long id);

    /** 선택 후보를 읽은 뒤 숨김 처리된 영상이 노출되지 않도록 잠금 획득 시에도 상태를 검사한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Short s WHERE s.id = :id AND s.status = :status")
    Optional<Short> findByIdAndStatusForUpdate(@Param("id") Long id, @Param("status") ShortStatus status);

    List<Short> findByStatus(ShortStatus status);

    @Query("SELECT s FROM Short s " +
           "LEFT JOIN FETCH s.hobby h " +
           "LEFT JOIN FETCH h.category " +
           "WHERE s.status = :status")
    List<Short> findWithHobbyByStatus(@Param("status") ShortStatus status);

    @Query("SELECT s FROM Short s " +
           "LEFT JOIN FETCH s.hobby h " +
           "LEFT JOIN FETCH h.category " +
           "WHERE s.status = :status AND s.id NOT IN :excludedIds")
    List<Short> findCandidatesExcluding(@Param("status") ShortStatus status,
                                       @Param("excludedIds") Collection<Long> excludedIds);

    // ───────── 숏폼 수집 ─────────

    /** 이미 저장된(상태 무관) 영상 ID. REJECTED도 포함해 같은 영상을 다시 판별하지 않는다. */
    @Query("SELECT s.youtubeVideoId FROM Short s WHERE s.youtubeVideoId IN :videoIds")
    List<String> findExistingVideoIds(@Param("videoIds") Collection<String> videoIds);

    /** 취미별 영상 수. 결과 행은 [hobbyId(Long), count(Long)] */
    @Query("SELECT s.hobby.id, COUNT(s) FROM Short s " +
           "WHERE s.hobby IS NOT NULL AND s.status IN :statuses " +
           "GROUP BY s.hobby.id")
    List<Object[]> countGroupByHobby(@Param("statuses") Collection<ShortStatus> statuses);

    /** 한 취미 안에서 채널별 영상 수. 결과 행은 [youtubeChannelId(String), count(Long)] */
    @Query("SELECT c.youtubeChannelId, COUNT(s) FROM Short s JOIN s.channel c " +
           "WHERE s.hobby.id = :hobbyId AND s.status IN :statuses " +
           "GROUP BY c.youtubeChannelId")
    List<Object[]> countGroupByChannelForHobby(@Param("hobbyId") Long hobbyId,
                                               @Param("statuses") Collection<ShortStatus> statuses);

    /** 주간 반영: from 상태 영상을 한꺼번에 to 상태로 올리고 반영 시각을 남긴다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Short s SET s.status = :to, s.releasedAt = :now, s.updatedAt = :now WHERE s.status = :from")
    int bulkRelease(@Param("from") ShortStatus from,
                    @Param("to") ShortStatus to,
                    @Param("now") LocalDateTime now);
}
