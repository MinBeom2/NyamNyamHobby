package com.ssafy.nyamnyam.domain.shortform;

import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.ZoneOffset;
import java.util.Properties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

/**
 * SAVED_SHORTS_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/testdb 로 실행한다.
 * 필요 시 SAVED_SHORTS_TEST_JDBC_USER/PASSWORD를 설정한다. 임시 테이블만 사용하고 매번 롤백한다.
 */
@EnabledIfEnvironmentVariable(named = "SAVED_SHORTS_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
class ShortSaveRepositoryPostgresTest {
    Connection connection;
    JdbcTemplate jdbc;
    ShortSaveRepository saves;

    @BeforeEach void setup() throws Exception {
        var properties = new Properties();
        String user = System.getenv("SAVED_SHORTS_TEST_JDBC_USER");
        String password = System.getenv("SAVED_SHORTS_TEST_JDBC_PASSWORD");
        if (user != null) properties.setProperty("user", user);
        if (password != null) properties.setProperty("password", password);
        connection = DriverManager.getConnection(System.getenv("SAVED_SHORTS_TEST_JDBC_URL"), properties);
        connection.setAutoCommit(false);
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        // 세션 전용 임시 테이블이 동일 이름의 영구 테이블보다 우선하므로 기존 데이터를 건드리지 않는다.
        jdbc.execute("CREATE TEMP TABLE hobbies(id BIGINT PRIMARY KEY, name VARCHAR(100))");
        jdbc.execute("""
                CREATE TEMP TABLE shorts(id BIGINT PRIMARY KEY, hobby_id BIGINT REFERENCES hobbies(id),
                    youtube_video_id VARCHAR(20), title VARCHAR(200), thumbnail_url VARCHAR(500), status VARCHAR(12))
                """);
        jdbc.execute("""
                CREATE TEMP TABLE short_saves(user_id BIGINT, short_id BIGINT REFERENCES shorts(id),
                    created_at TIMESTAMP(6), PRIMARY KEY(user_id, short_id))
                """);
        jdbc.update("INSERT INTO hobbies VALUES (3, '클라이밍'), (4, '요가')");
        jdbc.update("""
                INSERT INTO shorts VALUES
                    (41, 3, 'video000041', '첫 영상', 'https://example.com/41.jpg', 'ACTIVE'),
                    (42, 3, 'video000042', '동일 시각 영상', null, 'ACTIVE'),
                    (43, null, 'video000043', '미분류 이용 불가', null, 'UNAVAILABLE'),
                    (44, 4, 'video000044', '다른 사용자 영상', null, 'ACTIVE')
                """);
        jdbc.update("""
                INSERT INTO short_saves VALUES
                    (1, 41, '2026-10-20 05:20:00'), (1, 42, '2026-10-20 05:20:00'),
                    (1, 43, '2026-10-21 05:20:00'), (2, 44, '2026-10-22 05:20:00')
                """);
        saves = new ShortSaveRepository(jdbc);
    }

    @AfterEach void cleanup() throws Exception {
        if (connection != null) {
            try { connection.rollback(); } finally { connection.close(); }
        }
    }

    @Test void sortsTiesAndPagesWithoutLeakingOtherUsersAndPreservesNullHobby() {
        assertThat(saves.findSaved(1L, null, 0, 2, false)).extracting(item -> item.shortId()).containsExactly(43L, 42L);
        assertThat(saves.findSaved(1L, null, 1, 2, false)).extracting(item -> item.shortId()).containsExactly(41L);
        assertThat(saves.findSaved(1L, null, 0, 20, true)).extracting(item -> item.shortId()).containsExactly(41L, 42L, 43L);
        assertThat(saves.findSaved(1L, 3L, 0, 20, false)).extracting(item -> item.shortId()).containsExactly(42L, 41L);
        assertThat(saves.findSaved(1L, 4L, 0, 20, false)).isEmpty();
        assertThat(saves.findSaved(1L, null, Integer.MAX_VALUE, 100, false)).isEmpty();
        var unclassified = saves.findSaved(1L, null, 0, 1, false).get(0);
        assertThat(unclassified.hobby()).isNull();
        assertThat(unclassified.status().name()).isEqualTo("UNAVAILABLE");
        assertThat(unclassified.savedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(unclassified.savedAt().toString()).isEqualTo("2026-10-21T05:20Z");
    }

    @Test void countsAllSavesWithUnclassifiedGroupAndFiltersByOwner() {
        assertThat(saves.count(1L, null)).isEqualTo(3);
        assertThat(saves.count(1L, 3L)).isEqualTo(2);
        assertThat(saves.count(1L, 4L)).isZero();
        var groups = saves.countByHobby(1L);
        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).id()).isEqualTo(3L);
        assertThat(groups.get(0).count()).isEqualTo(2);
        assertThat(groups.get(1).id()).isNull();
        assertThat(groups.get(1).count()).isEqualTo(1);
        assertThat(saves.countByHobby(99L)).isEmpty();
    }

    @Test void saveAndDeleteRefreshCountsWithoutChangingFirstSaveTimeOrOtherUsersSaves() {
        var firstTime = saves.findSaved(1L, 3L, 0, 20, true).get(0).savedAt();
        saves.save(1L, 41L);
        assertThat(saves.countByUserId(1L)).isEqualTo(3);
        assertThat(saves.findSaved(1L, 3L, 0, 20, true).get(0).savedAt()).isEqualTo(firstTime);
        saves.save(2L, 41L);
        saves.delete(1L, 41L);
        saves.delete(1L, 43L);
        assertThat(saves.exists(2L, 41L)).isTrue();
        assertThat(saves.countByUserId(1L)).isEqualTo(1);
        assertThat(saves.countByHobby(1L)).hasSize(1);
        saves.delete(1L, 42L);
        assertThat(saves.findSaved(1L, null, 0, 20, false)).isEmpty();
        assertThat(saves.countByHobby(1L)).isEmpty();
    }
}
