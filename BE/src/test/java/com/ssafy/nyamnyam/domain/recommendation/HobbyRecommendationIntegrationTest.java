package com.ssafy.nyamnyam.domain.recommendation;

import com.ssafy.nyamnyam.domain.recommendation.model.TagFeature;
import com.ssafy.nyamnyam.domain.recommendation.service.HobbyRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Jenkins의 임시 PostgreSQL + pgvector에서 실행. 모든 fixture 변경은 롤백된다. */
@SpringBootTest
@Transactional
class HobbyRecommendationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired HobbyRecommendationService service;

    @Test void exactCosineFilteringAndLiveTagUpdates() {
        long category = jdbc.queryForObject("INSERT INTO categories(name) VALUES (?) RETURNING id",
                Long.class, "test-" + UUID.randomUUID().toString().substring(0, 8));
        long base = hobby(category, "base", "INDOOR", "ALONE", "PUBLISHED", 0);
        long identical = hobby(category, "identical", "INDOOR", "ALONE", "PUBLISHED", 2);
        long outdoor = hobby(category, "outdoor", "OUTDOOR", "ALONE", "PUBLISHED", 0);
        long hidden = hobby(category, "hidden", "INDOOR", "ALONE", "HIDDEN", 10);
        long invalid = hobby(category, "invalid", "INDOOR", "ALONE", "PUBLISHED", 20);
        for (long id : new long[]{base, identical, outdoor, hidden}) {
            tags(id, "활동적", "스포츠", "체력·건강");
        }
        tags(invalid, "활동적", "스포츠");
        // 실제 환경의 기존 취미가 테스트 순위에 영향을 주지 않도록 외부 데이터만 제외한다.
        Set<Long> excluded = new HashSet<>(jdbc.queryForList(
                "SELECT id FROM hobbies WHERE category_id <> ?", Long.class, category));
        var result = service.recommendSimilarToHobby(base, 100, excluded);
        assertThat(result).extracting(r -> r.hobbyId()).containsExactly(identical, outdoor);
        assertThat(result.getFirst().similarity()).isCloseTo(1.0, within(1e-6));
        assertThat(result.get(1).similarity()).isCloseTo(10.0 / 11, within(1e-6));
        assertThat(result.get(1).rank()).isEqualTo(2);

        excluded.add(base);
        excluded.add(identical);
        var personal = service.recommendByPreference(Map.of(TagFeature.SPORTS, 1.0), 3, excluded);
        assertThat(personal).extracting(r -> r.hobbyId()).containsExactly(outdoor);
        jdbc.update("DELETE FROM hobby_tags WHERE hobby_id = ? AND tag_id = (SELECT id FROM tags WHERE name = '체력·건강')", outdoor);
        assertThat(service.recommendByPreference(Map.of(TagFeature.SPORTS, 1.0), 3, excluded)).isEmpty();
    }

    private long hobby(long category, String label, String place, String social, String status, int saves) {
        return jdbc.queryForObject("""
                INSERT INTO hobbies(category_id,name,summary,place_type,social_type,difficulty,
                    session_min_minutes,session_max_minutes,initial_cost_min,initial_cost_max,
                    monthly_cost_min,monthly_cost_max,status,save_count,created_at,updated_at)
                VALUES (?, ?, 'test', ?, ?, 'EASY', 30, 60, 0, 0, 0, 0, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, category, label + "-" + UUID.randomUUID().toString().substring(0,8),
                place, social, status, saves);
    }

    private void tags(long id, String... names) {
        for (String name : names) jdbc.update("""
                INSERT INTO hobby_tags(hobby_id,tag_id) SELECT ?,id FROM tags WHERE name = ?
                """, id, name);
    }
}
