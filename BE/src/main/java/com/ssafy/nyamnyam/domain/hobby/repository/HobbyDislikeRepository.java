package com.ssafy.nyamnyam.domain.hobby.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.HashSet;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class HobbyDislikeRepository {
    private final JdbcTemplate jdbc;
    public void save(Long userId, Long hobbyId) {
        jdbc.update("INSERT INTO hobby_dislikes(user_id,hobby_id,created_at) VALUES (?,?,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING", userId, hobbyId);
    }
    public void delete(Long userId, Long hobbyId) {
        jdbc.update("DELETE FROM hobby_dislikes WHERE user_id = ? AND hobby_id = ?", userId, hobbyId);
    }
    public Set<Long> findIds(Long userId) {
        return new HashSet<>(jdbc.queryForList("SELECT hobby_id FROM hobby_dislikes WHERE user_id = ?", Long.class, userId));
    }
}
