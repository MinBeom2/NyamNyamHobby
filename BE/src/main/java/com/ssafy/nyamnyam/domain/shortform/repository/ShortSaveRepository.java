package com.ssafy.nyamnyam.domain.shortform.repository;

import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortResponse;
import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortSummaryResponse.HobbyCount;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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

    public long countByUserId(Long userId) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM short_saves WHERE user_id = ?",
                Long.class, userId);
        return count != null ? count : 0;
    }

    public long count(Long userId, Long hobbyId) {
        if (hobbyId == null) return countByUserId(userId);
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM short_saves sv
                JOIN shorts s ON s.id = sv.short_id
                WHERE sv.user_id = ? AND s.hobby_id = ?
                """, Long.class, userId, hobbyId);
        return count != null ? count : 0;
    }

    public List<SavedShortResponse> findSaved(Long userId, Long hobbyId, int page, int size, boolean oldest) {
        var args = new ArrayList<Object>();
        args.add(userId);
        String filter = "";
        if (hobbyId != null) {
            filter = " AND s.hobby_id = ?";
            args.add(hobbyId);
        }
        args.add(size);
        args.add((long) page * size);
        // 정렬 방향은 검증된 boolean으로 결정하며 요청 문자열을 SQL에 삽입하지 않는다.
        String direction = oldest ? "ASC" : "DESC";
        return jdbc.query("""
                SELECT s.id, s.youtube_video_id, s.title, s.thumbnail_url, s.status,
                       h.id AS hobby_id, h.name AS hobby_name, sv.created_at AS saved_at
                FROM short_saves sv
                JOIN shorts s ON s.id = sv.short_id
                LEFT JOIN hobbies h ON h.id = s.hobby_id
                WHERE sv.user_id = ?
                """ + filter + " ORDER BY sv.created_at " + direction + ", s.id " + direction + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    Long id = rs.getObject("hobby_id", Long.class);
                    return new SavedShortResponse(rs.getLong("id"), rs.getString("youtube_video_id"),
                            rs.getString("title"), rs.getString("thumbnail_url"),
                            id == null ? null : new SavedShortResponse.HobbyInfo(id, rs.getString("hobby_name")),
                            ShortStatus.valueOf(rs.getString("status")),
                            rs.getTimestamp("saved_at").toLocalDateTime().atOffset(ZoneOffset.UTC));
                }, args.toArray());
    }

    public List<HobbyCount> countByHobby(Long userId) {
        return jdbc.query("""
                SELECT h.id, h.name, count(*) AS save_count
                FROM short_saves sv
                JOIN shorts s ON s.id = sv.short_id
                LEFT JOIN hobbies h ON h.id = s.hobby_id
                WHERE sv.user_id = ?
                GROUP BY h.id, h.name
                ORDER BY h.name ASC NULLS LAST, h.id ASC
                """, (rs, rowNum) -> new HobbyCount(rs.getObject("id", Long.class),
                        rs.getString("name"), rs.getLong("save_count")), userId);
    }
}
