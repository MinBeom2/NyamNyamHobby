package com.ssafy.nyamnyam.domain.recommendation.repository;

import com.ssafy.nyamnyam.domain.recommendation.model.HobbyMatch;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class JdbcHobbyRecommendationRepository implements HobbyRecommendationRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcHobbyRecommendationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> findPublishedHobbyVector(long hobbyId) {
        return jdbc.query("""
                SELECT tag_vector::text FROM hobby_recommendation_vectors WHERE hobby_id = :id
                """, new MapSqlParameterSource("id", hobbyId),
                (rs, rowNum) -> rs.getString(1)).stream().findFirst();
    }

    @Override
    public List<HobbyMatch> findNearest(String vector, int limit, Set<Long> excludedHobbyIds) {
        String exclusion = excludedHobbyIds.isEmpty() ? "" : " AND h.id NOT IN (:excluded)";
        var params = new MapSqlParameterSource("vector", vector)
                .addValue("limit", limit).addValue("excluded", excludedHobbyIds);
        return jdbc.query("""
                SELECT h.id, h.name, h.summary, h.thumbnail_url,
                       GREATEST(-1.0, LEAST(1.0,
                           1 - (v.tag_vector <=> CAST(:vector AS vector(24))))) AS similarity
                FROM hobby_recommendation_vectors v JOIN hobbies h ON h.id = v.hobby_id
                WHERE h.status = 'PUBLISHED'
                """ + exclusion + """
                 AND (v.tag_vector <=> CAST(:vector AS vector(24))) < 1.0
                ORDER BY v.tag_vector <=> CAST(:vector AS vector(24)), h.save_count DESC, h.id ASC
                LIMIT :limit
                """, params, (rs, rowNum) -> new HobbyMatch(rs.getLong("id"), rs.getString("name"),
                rs.getString("summary"), rs.getString("thumbnail_url"),
                rs.getDouble("similarity"), rowNum + 1));
    }
}
