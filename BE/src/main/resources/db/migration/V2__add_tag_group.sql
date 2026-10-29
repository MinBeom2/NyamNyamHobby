-- V2: 태그 그룹(어느 질문의 답인지) 추가 · 주석 정리
-- 근거: repo/docs/spec/취미태그_유사도기준표.xlsx (결정 T7)
-- 주의: tags가 비어 있을 때 적용해야 NOT NULL 컬럼을 바로 추가할 수 있음 (시드보다 먼저)

ALTER TABLE tags ADD COLUMN tag_group VARCHAR(10) NOT NULL;
COMMENT ON COLUMN tags.tag_group IS 'PLACE(장소)|SOCIAL(사교)|ACTIVITY(활동성)|SUBJECT(소재)|GAIN(얻는 것)';
COMMENT ON COLUMN tags.name IS '화면에 그대로 보이는 이름. 2~8자, 묶을 때는 가운뎃점(예: 실내·실외, 체력·건강)';

-- 유사도 계산 주체 변경 반영 (Python 배치 → Spring 일 배치)
COMMENT ON COLUMN hobby_similarity.score IS '0~1 코사인 유사도(주제 가중치 2, 성향 1). Spring 일 배치가 기록';
