-- V20: 러닝 취미의 카카오 로컬 검색 키워드 개선
-- 기존의 '러닝트랙', '육상트랙'은 카카오맵 등록 시설이 극히 적어 실제 러닝이 가능한 운동장, 체육공원, 종합운동장, 러닝으로 교체/추가한다.

DELETE FROM hobby_source_mappings
WHERE target = 'KAKAO_KEYWORD'
  AND hobby_id = (SELECT id FROM hobbies WHERE name = '러닝');

INSERT INTO hobby_source_mappings (hobby_id, target, keyword, created_at)
SELECT h.id, 'KAKAO_KEYWORD', v.keyword, NOW()
FROM (VALUES
    ('운동장'),
    ('체육공원'),
    ('종합운동장'),
    ('러닝')
) AS v(keyword)
CROSS JOIN (SELECT id FROM hobbies WHERE name = '러닝') h;

