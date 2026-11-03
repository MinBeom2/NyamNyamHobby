-- V14: 취미 30종 대표 이미지 경로
-- 이미지는 프론트 정적 파일(FE/public/assets/hobbies/*.webp, 960x640)이고 같은 도메인에서 서빙된다.
-- 취미 찾기 카드·추천·상세·내 취미가 같은 이미지를 크기만 바꿔 쓴다.
-- 취미는 이름으로 찾는다(운영 ID를 가정하지 않음). 이미 값이 있는 취미는 덮지 않는다.

UPDATE hobbies h
SET thumbnail_url = '/assets/hobbies/' || v.file || '.webp',
    updated_at = NOW()
FROM (VALUES
    ('클라이밍', 'climbing'),
    ('러닝', 'running'),
    ('요가', 'yoga'),
    ('배드민턴', 'badminton'),
    ('수영', 'swimming'),
    ('등산', 'hiking'),
    ('걷기·산책', 'walking'),
    ('캠핑', 'camping'),
    ('자전거', 'cycling'),
    ('낚시', 'fishing'),
    ('뜨개질', 'knitting'),
    ('도자기', 'pottery'),
    ('목공', 'woodworking'),
    ('프라모델', 'plamodel'),
    ('캔들·디퓨저', 'candle'),
    ('드로잉', 'drawing'),
    ('캘리그라피', 'calligraphy'),
    ('기타', 'guitar'),
    ('K-팝 댄스', 'kpopdance'),
    ('사진 촬영', 'photography'),
    ('베이킹', 'baking'),
    ('홈카페', 'homecafe'),
    ('집밥 요리', 'cooking'),
    ('반려식물', 'plant'),
    ('테라리움', 'terrarium'),
    ('독서', 'reading'),
    ('글쓰기', 'writing'),
    ('보드게임', 'boardgame'),
    ('퍼즐', 'puzzle'),
    ('전시 관람', 'exhibition')
) AS v(hobby_name, file)
WHERE h.name = v.hobby_name
  AND h.thumbnail_url IS NULL;
