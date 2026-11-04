-- FEED-B02: V13에서 화면 확인용으로 넣은 예시 정보 피드 13건을 지운다.
-- 링크·제목이 실제와 다른 예시 데이터라 '자세히 보기'가 열리지 않고, 수집 배치(TourAPI·KOPIS·문화포털)가 실제 데이터를 채운다.
-- 다른 테이블이 feed_items를 참조하지 않아 이 행들만 지우면 된다. 입문 자료(GUIDE)는 실제 내용으로 따로 작성한다.
DELETE FROM feed_items
WHERE source_ref IN (
    'feed-climb-01', 'feed-climb-02', 'feed-climb-03',
    'feed-yoga-01', 'feed-yoga-02',
    'feed-run-01', 'feed-run-02',
    'feed-pottery-01', 'feed-pottery-02',
    'feed-baking-01', 'feed-exhibit-01', 'feed-wood-01',
    'feed-ended-01'
);
