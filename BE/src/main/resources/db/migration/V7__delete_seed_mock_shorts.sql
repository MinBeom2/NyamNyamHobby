-- V3 시드의 개발용 가짜 숏폼 16개를 완전히 삭제한다(V6에서 REJECTED로 바꿨던 것).
-- 실제 사용자가 없는 시점이라, 이 영상을 참조하는 시청·반응·보관 기록도 한 문장 안에서 함께 지운다.
-- (외래키는 문장 끝에서 검사하므로 자식·부모를 같은 문장에서 지우면 순서 문제 없음)
-- V6은 이미 적용된 마이그레이션이라 수정하지 않는다(Flyway 체크섬).

WITH mock AS (
    SELECT id FROM shorts
    WHERE channel_id IS NULL
      AND youtube_video_id IN (
        'aBcD123xYz0', 'bCdE234yZa1', 'cDeF345zAb2', 'dEfG456aBc3',
        'eFgH567bCd4', 'fGhI678cDe5', 'gHiJ789dEf6', 'hIjK890eFg7',
        'iJkL901fGh8', 'jKlM012gHi9', 'kLmN123hIj0', 'lMnO234iJk1',
        'mNoP345jKl2', 'NoPq456kLm3', 'OpQr567lMn4', 'PqRs678mNo5'
      )
),
deleted_views AS (
    DELETE FROM exploration_views WHERE short_id IN (SELECT id FROM mock)
),
deleted_reactions AS (
    DELETE FROM short_reactions WHERE short_id IN (SELECT id FROM mock)
),
deleted_saves AS (
    DELETE FROM short_saves WHERE short_id IN (SELECT id FROM mock)
)
DELETE FROM shorts WHERE id IN (SELECT id FROM mock);
