package com.ssafy.nyamnyam.domain.notification.repository;

import com.ssafy.nyamnyam.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadAtIsNull(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /** 같은 (user_id, dedup_key)가 있으면 아무것도 하지 않는다. 반환값은 실제로 넣은 행 수(0 또는 1) */
    @Modifying
    @Query(value = "INSERT INTO notifications (user_id, type, title, body, link_path, dedup_key, created_at) " +
            "VALUES (:userId, :type, :title, :body, :linkPath, :dedupKey, :createdAt) " +
            "ON CONFLICT (user_id, dedup_key) DO NOTHING", nativeQuery = true)
    int insertIgnoringDuplicate(@Param("userId") Long userId, @Param("type") String type,
                                @Param("title") String title, @Param("body") String body,
                                @Param("linkPath") String linkPath, @Param("dedupKey") String dedupKey,
                                @Param("createdAt") LocalDateTime createdAt);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
    int deleteCreatedBefore(@Param("before") LocalDateTime before);
}
