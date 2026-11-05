-- V19: 장소 대표 썸네일 이미지 컬럼 추가 (Daum 이미지 검색 등 외부 출처 이미지 URL)
ALTER TABLE places ADD COLUMN thumbnail_url VARCHAR(500) NULL;
COMMENT ON COLUMN places.thumbnail_url IS '장소 대표 썸네일 URL';

