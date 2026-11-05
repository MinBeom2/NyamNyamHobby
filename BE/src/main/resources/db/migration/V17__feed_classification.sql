-- FEED-B04: 정보 피드 취미 분류(GMS)
-- 1) 30종 어디에도 맞지 않는 피드를 '그 외'로 보여 주려고 취미 없음(hobby_id NULL)을 허용한다.
-- 2) 분류에 쓸 출처 분류명과 분류 진행 상태를 남긴다.
--    classified_at이 NULL이면 아직 GMS 확인 전이다. 검색어로 후보 취미가 붙은 항목은 PUBLISHED로 먼저 보이고,
--    후보가 없는 항목은 PENDING_REVIEW(화면에 안 보임)로 기다리다 분류되면 PUBLISHED가 된다.
--    GMS가 계속 실패하면 classify_attempts가 올라가고, 정해진 횟수가 넘으면 후보(없으면 '그 외')로 확정한다.
ALTER TABLE feed_items ALTER COLUMN hobby_id DROP NOT NULL;

ALTER TABLE feed_items
    ADD COLUMN source_category   VARCHAR(50)  NULL,
    ADD COLUMN classified_at     TIMESTAMP(6) NULL,
    ADD COLUMN classify_attempts SMALLINT     NOT NULL DEFAULT 0;

COMMENT ON COLUMN feed_items.hobby_id IS '연결된 취미. NULL이면 30종 어디에도 맞지 않는 ''그 외'' 피드';
COMMENT ON COLUMN feed_items.source_category IS '출처가 준 분류명(서울 예약 소분류, KOPIS 장르, 문화포털 분야). GMS 취미 분류 단서';
COMMENT ON COLUMN feed_items.classified_at IS 'GMS 취미 분류를 마친 시각. NULL이면 분류 대기';
COMMENT ON COLUMN feed_items.classify_attempts IS 'GMS 분류를 시도했지만 답을 못 받은 횟수';

-- 관리자가 직접 쓴 입문 자료는 분류 대상이 아니다. 수집으로 들어온 기존 피드는 단어 대조로만 붙어 있어 GMS로 다시 확인한다
UPDATE feed_items SET classified_at = NOW() WHERE origin = 'ADMIN';

CREATE INDEX idx_feed_classify ON feed_items (status, id) WHERE classified_at IS NULL;
