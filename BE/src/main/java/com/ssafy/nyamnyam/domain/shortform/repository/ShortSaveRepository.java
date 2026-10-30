package com.ssafy.nyamnyam.domain.shortform.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** V1의 (user_id, short_id) 복합 PK를 사용한다. 중복 요청은 기존 보관 시각을 유지한다. */
@Repository
@RequiredArgsConstructor
public class ShortSaveRepository {
    private final JdbcTemplate jdbc;

    public void save(Long userId, Long shortId) {
        jdbc.update("""
                INSERT INTO short_saves (user_id, short_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id, short_id) DO NOTHING
                """, userId, shortId);
    }

    public void delete(Long userId, Long shortId) {
        jdbc.update("DELETE FROM short_saves WHERE user_id = ? AND short_id = ?", userId, shortId);
    }

    public boolean exists(Long userId, Long shortId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM short_saves WHERE user_id = ? AND short_id = ?)",
                Boolean.class, userId, shortId));
    }
}
