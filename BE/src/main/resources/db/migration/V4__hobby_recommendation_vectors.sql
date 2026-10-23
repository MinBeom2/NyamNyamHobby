CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE tag_vector_dimensions (
    tag_id BIGINT PRIMARY KEY REFERENCES tags(id),
    dimension_index SMALLINT NOT NULL UNIQUE CHECK (dimension_index BETWEEN 0 AND 23)
);

-- 좌표 순서는 TagFeature와 일치한다. 기존 태그는 이름으로 찾아 ID를 재사용한다.
INSERT INTO tags (name, tag_group) VALUES
('실내','PLACE'), ('실외','PLACE'), ('실내·실외','PLACE'),
('혼자','SOCIAL'), ('함께','SOCIAL'), ('혼자·함께','SOCIAL'),
('활동적','ACTIVITY'), ('가볍게 움직임','ACTIVITY'), ('정적','ACTIVITY'),
('스포츠','SUBJECT'), ('자연','SUBJECT'), ('공예','SUBJECT'),
('미술·사진','SUBJECT'), ('음악','SUBJECT'), ('요리','SUBJECT'),
('글·책','SUBJECT'), ('식물','SUBJECT'), ('게임·퍼즐','SUBJECT'),
('체력·건강','GAIN'), ('실력 향상·성취','GAIN'), ('작품·결과물','GAIN'),
('휴식·마음 정리','GAIN'), ('몰입·집중','GAIN'), ('지식·교양','GAIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO tag_vector_dimensions (tag_id, dimension_index)
SELECT t.id, (v.ordinality - 1)::smallint
FROM unnest(ARRAY[
    '실내','실외','실내·실외','혼자','함께','혼자·함께',
    '활동적','가볍게 움직임','정적','스포츠','자연','공예','미술·사진',
    '음악','요리','글·책','식물','게임·퍼즐','체력·건강',
    '실력 향상·성취','작품·결과물','휴식·마음 정리','몰입·집중','지식·교양'
]) WITH ORDINALITY AS v(name, ordinality)
JOIN tags t ON t.name = v.name;

-- 기존 이름이 잘못된 그룹으로 등록된 경우 조용히 다른 벡터를 만들지 않는다.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM tag_vector_dimensions d JOIN tags t ON t.id = d.tag_id
        WHERE t.tag_group <> CASE
            WHEN d.dimension_index < 3 THEN 'PLACE'
            WHEN d.dimension_index < 6 THEN 'SOCIAL'
            WHEN d.dimension_index < 9 THEN 'ACTIVITY'
            WHEN d.dimension_index < 18 THEN 'SUBJECT' ELSE 'GAIN' END
    ) THEN
        RAISE EXCEPTION 'Tag group conflicts with recommendation vector mapping';
    END IF;
END $$;

-- 30개 규모에서는 원본에서 즉시 벡터를 만드는 뷰로 충분하다.
-- 장소·사교는 hobbies 컬럼이 원본이며 별도 hobby_tags 연결이 필요 없다.
-- 사람이 지정하는 ACTIVITY/SUBJECT/GAIN은 정확히 1개씩 필요하다.
CREATE VIEW hobby_recommendation_vectors AS
WITH selected_tags AS (
    SELECT h.id AS hobby_id, d.dimension_index
    FROM hobbies h
    JOIN tags t ON
        (t.tag_group = 'PLACE' AND t.name = CASE h.place_type
            WHEN 'INDOOR' THEN '실내' WHEN 'OUTDOOR' THEN '실외' WHEN 'BOTH' THEN '실내·실외' END)
        OR (t.tag_group = 'SOCIAL' AND t.name = CASE h.social_type
            WHEN 'ALONE' THEN '혼자' WHEN 'TOGETHER' THEN '함께' WHEN 'BOTH' THEN '혼자·함께' END)
    JOIN tag_vector_dimensions d ON d.tag_id = t.id
    UNION ALL
    SELECT ht.hobby_id, d.dimension_index
    FROM hobby_tags ht JOIN tags t ON t.id = ht.tag_id
    LEFT JOIN tag_vector_dimensions d ON d.tag_id = t.id
    WHERE t.tag_group NOT IN ('PLACE', 'SOCIAL')
), valid_hobbies AS (
    SELECT hobby_id FROM selected_tags
    GROUP BY hobby_id
    HAVING count(*) = 5 AND count(dimension_index) = 5
       AND count(*) FILTER (WHERE dimension_index BETWEEN 0 AND 2) = 1
       AND count(*) FILTER (WHERE dimension_index BETWEEN 3 AND 5) = 1
       AND count(*) FILTER (WHERE dimension_index BETWEEN 6 AND 8) = 1
       AND count(*) FILTER (WHERE dimension_index BETWEEN 9 AND 17) = 1
       AND count(*) FILTER (WHERE dimension_index BETWEEN 18 AND 23) = 1
)
SELECT h.id AS hobby_id,
       array_agg(CASE WHEN st.dimension_index IS NULL THEN 0::real
                      WHEN axes.dimension_index < 9 THEN 1::real ELSE 2::real END
                 ORDER BY axes.dimension_index)::vector(24) AS tag_vector
FROM hobbies h
JOIN valid_hobbies vh ON vh.hobby_id = h.id
CROSS JOIN generate_series(0,23) axes(dimension_index)
LEFT JOIN selected_tags st ON st.hobby_id = h.id AND st.dimension_index = axes.dimension_index
WHERE h.status = 'PUBLISHED'
GROUP BY h.id;
