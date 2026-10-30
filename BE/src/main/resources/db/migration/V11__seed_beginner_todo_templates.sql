-- Curated starter data for the three beginner hobbies below. Match by name,
-- do not assume production IDs. Never replace an existing authored template.
CREATE TEMP TABLE todo_seed_templates ON COMMIT DROP AS
WITH starter(hobby_name, title) AS (VALUES
  ('클라이밍', '클라이밍 · 입문 3주 계획'),
  ('드로잉', '드로잉 · 입문 3주 계획'),
  ('사진 촬영', '사진 촬영 · 입문 3주 계획')
), inserted AS (
INSERT INTO plan_templates(hobby_id, skill_level, title, total_weeks, version, status, created_at, updated_at)
SELECT h.id, 'BEGINNER', s.title, 3, 1, 'DRAFT', NOW(), NOW()
FROM starter s JOIN hobbies h ON h.name = s.hobby_name AND h.status = 'PUBLISHED'
ON CONFLICT (hobby_id, skill_level) DO NOTHING
RETURNING id
)
SELECT id FROM inserted;

-- Only populate templates actually inserted above; existing drafts are untouched.
WITH starter(hobby_name, week_no, title) AS (VALUES
  ('클라이밍', 1, '환경과 안전 수칙 익히기'), ('클라이밍', 2, '기본 동작 익히기'), ('클라이밍', 3, '기초 동작 복습'),
  ('드로잉', 1, '도구와 선에 익숙해지기'), ('드로잉', 2, '형태와 명암 익히기'), ('드로잉', 3, '일상 사물 그리기'),
  ('사진 촬영', 1, '촬영 도구 익히기'), ('사진 촬영', 2, '구도와 빛 관찰하기'), ('사진 촬영', 3, '주제를 정해 촬영하기')
)
INSERT INTO plan_template_weeks(template_id, week_no, title)
SELECT t.id, s.week_no::SMALLINT, s.title FROM starter s
JOIN hobbies h ON h.name = s.hobby_name
JOIN plan_templates t ON t.hobby_id = h.id AND t.skill_level = 'BEGINNER'
JOIN todo_seed_templates seeded ON seeded.id = t.id
WHERE t.title = h.name || ' · 입문 3주 계획' AND t.version = 1 AND t.status = 'DRAFT'
  AND NOT EXISTS (SELECT 1 FROM plan_template_items i WHERE i.template_id = t.id)
ON CONFLICT (template_id, week_no) DO NOTHING;

WITH starter(hobby_name, week_no, seq, title, description, minutes, supplies, criteria) AS (VALUES
  ('클라이밍', 1, 1, '체험 수업 안내 확인하기', '실내 암장 체험 수업의 이용 안내와 안전 교육 시간을 확인합니다.', 15, '편한 운동복', '안전 교육을 포함한 체험 수업 안내 확인'),
  ('클라이밍', 1, 2, '강사와 장비 및 안전 수칙 확인하기', '처음에는 강사의 안내에 따라 장비를 착용하고 안전 수칙을 배웁니다.', 30, '암벽화(대여), 편한 운동복', '강사에게 장비 착용과 안전 수칙 확인받기'),
  ('클라이밍', 2, 1, '강사와 기본 홀드 잡기 연습', '체험 수업에서 본인 수준에 맞는 기초 동작을 지도받습니다.', 40, '암벽화(대여), 초크', '강사의 지도를 받아 기초 동작 연습'),
  ('클라이밍', 2, 2, '안전 교육 내용 기록하기', '수업에서 배운 안전 수칙과 궁금한 점을 기록합니다.', 15, '메모 도구', '안전 수칙과 질문 한 가지 기록'),
  ('클라이밍', 3, 1, '지도받은 기초 동작 복습', '무리한 난이도를 피하고 지도받은 범위에서 복습합니다.', 40, '암벽화(대여), 초크', '지도받은 동작을 복습하고 피드백 받기'),
  ('클라이밍', 3, 2, '체험 소감 정리하기', '재미있었던 점과 다음에 배우고 싶은 내용을 기록합니다.', 15, '메모 도구', '체험 소감과 다음 목표 기록'),
  ('드로잉', 1, 1, '도구 준비하고 선 그리기', '연필과 종이를 준비하고 직선과 곡선을 그립니다.', 20, '연필, 종이, 지우개', '직선과 곡선 한 장 채우기'),
  ('드로잉', 1, 2, '주변 사물 윤곽 관찰하기', '주변 사물 하나를 골라 외곽선을 관찰해 그립니다.', 25, '연필, 종이', '사물 윤곽 스케치 한 장'),
  ('드로잉', 2, 1, '기본 도형 그리기', '원과 상자 형태로 사물을 단순화합니다.', 25, '연필, 종이', '기본 도형 세 가지 그리기'),
  ('드로잉', 2, 2, '명암 단계 연습하기', '연필 힘을 달리해 밝고 어두운 단계를 표현합니다.', 20, '연필, 종이', '명암 다섯 단계 표현'),
  ('드로잉', 3, 1, '일상 사물 완성하기', '작은 사물을 골라 윤곽과 명암을 표현합니다.', 40, '연필, 종이, 지우개', '사물 드로잉 한 장 완성'),
  ('드로잉', 3, 2, '첫 그림과 비교하기', '처음 그린 그림과 비교해 달라진 점을 기록합니다.', 15, '완성한 그림, 메모 도구', '달라진 점 두 가지 기록'),
  ('사진 촬영', 1, 1, '카메라 기본 기능 확인하기', '스마트폰 또는 카메라의 초점과 밝기 조절을 익힙니다.', 20, '스마트폰 또는 카메라', '초점과 밝기를 달리한 사진 촬영'),
  ('사진 촬영', 1, 2, '같은 피사체 다르게 찍기', '안전한 장소에서 같은 사물을 여러 거리로 촬영합니다.', 25, '스마트폰 또는 카메라', '서로 다른 거리의 사진 세 장'),
  ('사진 촬영', 2, 1, '구도 바꿔보기', '피사체의 위치를 바꾸어 구도를 비교합니다.', 30, '스마트폰 또는 카메라', '세 가지 구도로 촬영'),
  ('사진 촬영', 2, 2, '빛 방향 관찰하기', '실내 자연광의 방향에 따른 사진 차이를 관찰합니다.', 25, '스마트폰 또는 카메라', '빛 방향을 바꾼 사진 비교'),
  ('사진 촬영', 3, 1, '주제 사진 촬영하기', '일상에서 주제 하나를 정해 사진을 모읍니다.', 40, '스마트폰 또는 카메라', '같은 주제의 사진 다섯 장'),
  ('사진 촬영', 3, 2, '사진 고르고 소감 남기기', '마음에 드는 사진을 골라 촬영 소감을 기록합니다.', 20, '촬영한 사진, 메모 도구', '사진 세 장과 소감 기록')
)
INSERT INTO plan_template_items(template_id, week_no, seq, title, description, expected_minutes, supplies, done_criteria)
SELECT t.id, s.week_no::SMALLINT, s.seq::SMALLINT, s.title, s.description, s.minutes::SMALLINT, s.supplies, s.criteria
FROM starter s JOIN hobbies h ON h.name = s.hobby_name
JOIN plan_templates t ON t.hobby_id = h.id AND t.skill_level = 'BEGINNER'
JOIN todo_seed_templates seeded ON seeded.id = t.id
WHERE t.title = h.name || ' · 입문 3주 계획' AND t.version = 1 AND t.status = 'DRAFT'
  AND NOT EXISTS (SELECT 1 FROM plan_template_items i WHERE i.template_id = t.id);

UPDATE plan_templates t SET status = 'PUBLISHED', updated_at = NOW()
FROM hobbies h WHERE t.hobby_id = h.id AND h.name IN ('클라이밍', '드로잉', '사진 촬영')
  AND t.id IN (SELECT id FROM todo_seed_templates)
  AND t.skill_level = 'BEGINNER' AND t.title = h.name || ' · 입문 3주 계획'
  AND t.version = 1 AND t.status = 'DRAFT'
  AND (SELECT COUNT(*) FROM plan_template_weeks w WHERE w.template_id = t.id) = 3
  AND (SELECT COUNT(*) FROM plan_template_items i WHERE i.template_id = t.id) = 6;
