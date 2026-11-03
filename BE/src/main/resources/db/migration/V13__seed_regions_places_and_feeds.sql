-- ========================================================
-- V13__seed_regions_places_and_feeds.sql
-- 서울/수도권 행정구역(regions), 기초 활동 장소(places),
-- 취미-장소 매핑(hobby_places), 취미 정보 피드(feed_items) 시드 데이터
-- ========================================================

-- 1. 행정구역 (regions) : 서울특별시 25개 자치구 + 경기도 주요 시
INSERT INTO regions (id, code, sido, sigungu) VALUES
  (1,  '11110', '서울특별시', '종로구'),
  (2,  '11140', '서울특별시', '중구'),
  (3,  '11170', '서울특별시', '용산구'),
  (4,  '11200', '서울특별시', '성동구'),
  (5,  '11215', '서울특별시', '광진구'),
  (6,  '11230', '서울특별시', '동대문구'),
  (7,  '11260', '서울특별시', '중랑구'),
  (8,  '11290', '서울특별시', '성북구'),
  (9,  '11305', '서울특별시', '강북구'),
  (10, '11320', '서울특별시', '도봉구'),
  (11, '11350', '서울특별시', '노원구'),
  (12, '11380', '서울특별시', '은평구'),
  (13, '11410', '서울특별시', '서대문구'),
  (14, '11440', '서울특별시', '마포구'),
  (15, '11470', '서울특별시', '양천구'),
  (16, '11500', '서울특별시', '강서구'),
  (17, '11530', '서울특별시', '구로구'),
  (18, '11545', '서울특별시', '금천구'),
  (19, '11560', '서울특별시', '영등포구'),
  (20, '11590', '서울특별시', '동작구'),
  (21, '11620', '서울특별시', '관악구'),
  (22, '11650', '서울특별시', '서초구'),
  (23, '11680', '서울특별시', '강남구'),
  (24, '11710', '서울특별시', '송파구'),
  (25, '11740', '서울특별시', '강동구'),
  (26, '41110', '경기도',     '수원시'),
  (27, '41130', '경기도',     '성남시'),
  (28, '41190', '경기도',     '부천시'),
  (29, '41280', '경기도',     '고양시'),
  (30, '41460', '경기도',     '용인시')
ON CONFLICT (code) DO NOTHING;

-- 시퀀스 동기화 (id 직접 지정 삽입 대비)
SELECT setval(pg_get_serial_sequence('regions', 'id'), coalesce(max(id), 1)) FROM regions;


-- 2. 활동 장소 (places)
INSERT INTO places (id, region_id, name, category, address, lat, lng, opening_hours, fee_text, external_url, source, source_ref, source_updated_at, status, created_at, updated_at) VALUES
  -- 클라이밍 암장 (마포, 영등포, 종로, 강남)
  (1, 14, '더클라임 클라이밍 마포점', 'STUDIO', '서울특별시 마포구 양화로 123', 37.5539201, 126.9204853, '평일 10:00~23:00 / 주말 10:00~20:00', '일일 이용권 25,000원', 'https://place.map.kakao.com/1001', 'KAKAO_LOCAL', 'place-climb-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (2, 19, '서울볼더스 클라이밍 컴퍼니', 'STUDIO', '서울특별시 영등포구 문래로 180', 37.5186214, 126.8987451, '평일 11:00~23:00 / 주말 10:00~20:00', '일일 이용권 20,000원', 'https://place.map.kakao.com/1002', 'KAKAO_LOCAL', 'place-climb-02', NOW(), 'PUBLISHED', NOW(), NOW()),
  (3, 1,  '피커스 클라이밍 종로점', 'STUDIO', '서울특별시 종로구 돈화문로 26', 37.5712154, 126.9912458, '평일 10:00~23:00 / 주말 10:00~20:00', '일일 이용권 22,000원', 'https://place.map.kakao.com/1003', 'KAKAO_LOCAL', 'place-climb-03', NOW(), 'PUBLISHED', NOW(), NOW()),
  (4, 23, '클라이밍파크 강남점', 'STUDIO', '서울특별시 강남구 강남대로 390', 37.4981245, 127.0278912, '매일 10:00~23:00', '일일 이용권 22,000원', 'https://place.map.kakao.com/1004', 'KAKAO_LOCAL', 'place-climb-04', NOW(), 'PUBLISHED', NOW(), NOW()),

  -- 러닝 & 산책 (영등포, 서초, 성동, 중구, 송파)
  (5, 19, '여의도 한강공원 러닝코스', 'PARK', '서울특별시 영등포구 여의동로 330', 37.5284125, 126.9341582, '연중무휴 24시간', '무료', 'https://place.map.kakao.com/1005', 'PUBLIC_PARK', 'place-run-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (6, 22, '반포 한강공원 달빛광장', 'PARK', '서울특별시 서초구 신반포로11길 40', 37.5098412, 126.9952418, '연중무휴 24시간', '무료', 'https://place.map.kakao.com/1006', 'PUBLIC_PARK', 'place-run-02', NOW(), 'PUBLISHED', NOW(), NOW()),
  (7, 4,  '서울숲 공원 산책로', 'PARK', '서울특별시 성동구 뚝섬로 273', 37.5443812, 127.0374158, '연중무휴 24시간', '무료', 'https://place.map.kakao.com/1007', 'PUBLIC_PARK', 'place-run-03', NOW(), 'PUBLISHED', NOW(), NOW()),
  (8, 2,  '남산 둘레길 순환로', 'PARK', '서울특별시 중구 남산공원길 105', 37.5512415, 126.9882145, '연중무휴 24시간', '무료', 'https://place.map.kakao.com/1008', 'PUBLIC_PARK', 'place-run-04', NOW(), 'PUBLISHED', NOW(), NOW()),

  -- 요가 스튜디오
  (9, 14, '숨 요가 스튜디오 연남', 'STUDIO', '서울특별시 마포구 동교로 210', 37.5601245, 126.9245124, '평일 07:00~22:00 / 토 09:00~18:00', '1회 체험권 20,000원', 'https://place.map.kakao.com/1009', 'KAKAO_LOCAL', 'place-yoga-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (10, 3, '자이요가 한남스튜디오', 'STUDIO', '서울특별시 용산구 독서당로 85', 37.5345124, 127.0084512, '평일 08:00~21:30 / 주말 10:00~17:00', '1회 체험권 30,000원', 'https://place.map.kakao.com/1010', 'KAKAO_LOCAL', 'place-yoga-02', NOW(), 'PUBLISHED', NOW(), NOW()),

  -- 배드민턴 전용 구장
  (11, 14, '마포구민체육센터 배드민턴장', 'SPORTS', '서울특별시 마포구 월드컵로25길 190', 37.5621458, 126.8984512, '매일 06:00~22:00', '2시간 3,000원', 'https://place.map.kakao.com/1011', 'PUBLIC_SPORTS', 'place-badminton-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (12, 1,  '종로구민회관 다목적체육관', 'SPORTS', '서울특별시 종로구 지봉로5길 7-5', 37.5754125, 127.0165412, '매일 06:00~21:50', '2시간 3,500원', 'https://place.map.kakao.com/1012', 'PUBLIC_SPORTS', 'place-badminton-02', NOW(), 'PUBLISHED', NOW(), NOW()),

  -- 수영장
  (13, 24, '올림픽공원 실내수영장', 'SPORTS', '서울특별시 송파구 올림픽로 424', 37.5201452, 127.1245125, '평일 06:00~22:00 / 주말 09:00~18:00', '자유수영 일일 6,000원', 'https://place.map.kakao.com/1013', 'PUBLIC_SPORTS', 'place-swim-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (14, 14, '마포아트센터 수영장', 'SPORTS', '서울특별시 마포구 대흥로20길 28', 37.5512458, 126.9465124, '평일 06:00~21:00 / 토 09:00~17:00', '자유수영 일일 5,000원', 'https://place.map.kakao.com/1014', 'PUBLIC_SPORTS', 'place-swim-02', NOW(), 'PUBLISHED', NOW(), NOW()),

  -- 등산 명소
  (15, 9,  '북한산 백운대 탐방지원센터', 'PARK', '서울특별시 강북구 삼양로173길 52', 37.6584125, 127.0094125, '일출 2시간 전 ~ 일몰 시', '무료', 'https://place.map.kakao.com/1015', 'PUBLIC_PARK', 'place-hiking-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (16, 21, '관악산 만남의광장 등산로', 'PARK', '서울특별시 관악구 신림로 80', 37.4641258, 126.9512458, '상시 개방', '무료', 'https://place.map.kakao.com/1016', 'PUBLIC_PARK', 'place-hiking-02', NOW(), 'PUBLISHED', NOW(), NOW()),

  -- 공예 & 클래스 (도자기, 목공)
  (17, 14, '흙놀이 도예공방 연남점', 'CLASS', '서울특별시 마포구 성미산로 150', 37.5641258, 126.9215412, '매일 11:00~20:00 (월요일 휴무)', '원데이 클래스 50,000원', 'https://place.map.kakao.com/1017', 'KAKAO_LOCAL', 'place-pottery-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (18, 4,  '성수 우드워커 목공 스튜디오', 'STUDIO', '서울특별시 성동구 아차산로 100', 37.5451245, 127.0541258, '평일 10:00~21:00 / 주말 10:00~19:00', '기초 목공 과정 80,000원', 'https://place.map.kakao.com/1018', 'KAKAO_LOCAL', 'place-wood-01', NOW(), 'PUBLISHED', NOW(), NOW()),

  -- 예술 & 취미 클래스 (드로잉, 베이킹, 보드게임)
  (19, 14, '성수미술관 연남점 (드로잉카페)', 'CLASS', '서울특별시 마포구 동교로 240', 37.5612458, 126.9234125, '매일 12:00~22:00', '이용권+캔버스 22,000원', 'https://place.map.kakao.com/1019', 'KAKAO_LOCAL', 'place-drawing-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (20, 23, '원데이 베이킹랩 강남본점', 'CLASS', '서울특별시 강남구 테헤란로 120', 37.4998412, 127.0312458, '매일 10:00~21:00', '구움과자 클래스 65,000원', 'https://place.map.kakao.com/1020', 'KAKAO_LOCAL', 'place-baking-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (21, 14, '레드버튼 보드게임카페 홍대점', 'ETC', '서울특별시 마포구 홍익로 15', 37.5541258, 126.9224125, '평일 13:00~24:00 / 주말 12:00~01:00', '1시간 3,000원 (음료 별도)', 'https://place.map.kakao.com/1021', 'KAKAO_LOCAL', 'place-board-01', NOW(), 'PUBLISHED', NOW(), NOW()),

  -- 전시 관람
  (22, 1,  '국립현대미술관 서울관', 'ETC', '서울특별시 종로구 삼청로 30', 37.5784125, 126.9801245, '화~일 10:00~18:00 (수/토 야간개장 ~21:00)', '통합관람권 4,000원', 'https://place.map.kakao.com/1022', 'ADMIN', 'place-museum-01', NOW(), 'PUBLISHED', NOW(), NOW()),
  (23, 22, '예술의전당 한가람미술관', 'ETC', '서울특별시 서초구 남부순환로 2406', 37.4801245, 127.0134125, '화~일 10:00~19:00', '기획전 15,000~20,000원', 'https://place.map.kakao.com/1023', 'ADMIN', 'place-museum-02', NOW(), 'PUBLISHED', NOW(), NOW())
ON CONFLICT (source, source_ref) DO NOTHING;

SELECT setval(pg_get_serial_sequence('places', 'id'), coalesce(max(id), 1)) FROM places;


-- 3. 취미 - 장소 매핑 (hobby_places)
INSERT INTO hobby_places (hobby_id, place_id) VALUES
  -- 클라이밍 (hobby_id = 1)
  (1, 1), (1, 2), (1, 3), (1, 4),
  -- 러닝 (hobby_id = 2) & 걷기·산책 (hobby_id = 7)
  (2, 5), (2, 6), (2, 7), (2, 8),
  (7, 5), (7, 6), (7, 7), (7, 8),
  -- 요가 (hobby_id = 3)
  (3, 9), (3, 10),
  -- 배드민턴 (hobby_id = 4)
  (4, 11), (4, 12),
  -- 수영 (hobby_id = 5)
  (5, 13), (5, 14),
  -- 등산 (hobby_id = 6)
  (6, 15), (6, 16),
  -- 도자기 (hobby_id = 12)
  (12, 17),
  -- 목공 (hobby_id = 13)
  (13, 18),
  -- 드로잉 (hobby_id = 16)
  (16, 19),
  -- 베이킹 (hobby_id = 21)
  (21, 20),
  -- 보드게임 (hobby_id = 28)
  (28, 21),
  -- 전시 관람 (hobby_id = 30)
  (30, 22), (30, 23)
ON CONFLICT DO NOTHING;


-- 4. 취미 정보 피드 (feed_items)
INSERT INTO feed_items (id, hobby_id, type, title, summary, source_name, source_url, source_ref, thumbnail_url, region_id, published_at, event_start, event_end, origin, status, created_at, updated_at) VALUES
  -- 클라이밍 피드
  (1, 1, 'CLASS', '실내 클라이밍 입문 원데이 클래스', '기본 홀드 잡는 법부터 안전한 낙하법, V0 볼더링 문제 완등까지 전문 강사와 함께 배워봅니다.', '서울 공공서비스예약', 'https://yeyak.seoul.go.kr/web/detail/1001', 'feed-climb-01', 'https://images.unsplash.com/photo-1522163182402-834f871fd851?w=600', 14, NOW() - INTERVAL '2 days', CURRENT_DATE + 3, CURRENT_DATE + 14, 'SEOUL_RESERVATION', 'PUBLISHED', NOW(), NOW()),
  (2, 1, 'EVENT', '2026 전국 청소년 스포츠클라이밍 페스티벌', '스포츠클라이밍 꿈나무와 동호인을 위한 축제로 다채로운 체험 부스와 이벤트 볼더링 대회가 열립니다.', '한국관광공사 TourAPI', 'https://korean.visitkorea.or.kr/detail/2001', 'feed-climb-02', 'https://images.unsplash.com/photo-1564769662533-4f00a87b4056?w=600', 1, NOW() - INTERVAL '5 days', CURRENT_DATE + 7, CURRENT_DATE + 9, 'TOUR_API', 'PUBLISHED', NOW(), NOW()),
  (3, 1, 'GUIDE', '처음 암장에 가기 전 알아두면 좋은 볼더링 기초 용어 15', '홀드, 문제, 존, 완등, 그레이드 등 암장 첫 방문 시 꼭 알아야 할 필수 용어를 그림과 함께 정리했습니다.', '냠냠취미 편집팀', 'https://nyamnyam.kr/guides/climbing-terms', 'feed-climb-03', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600', NULL, NOW() - INTERVAL '10 days', NULL, NULL, 'ADMIN', 'PUBLISHED', NOW(), NOW()),

  -- 요가 피드
  (4, 3, 'EVENT', '2026 한강 선셋 요가 페스타', '노을 지는 한강변에서 싱잉볼 사운드와 함께하는 힐링 빈야사 요가 세션입니다.', '서울 공공서비스예약', 'https://yeyak.seoul.go.kr/web/detail/1002', 'feed-yoga-01', 'https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=600', 22, NOW() - INTERVAL '3 days', CURRENT_DATE + 5, CURRENT_DATE + 6, 'SEOUL_RESERVATION', 'PUBLISHED', NOW(), NOW()),
  (5, 3, 'CLASS', '직장인을 위한 척추 교정 & 힐링 요가 4주 코스', '퇴근 후 경직된 목과 어깨를 풀어주는 직장인 맞춤형 저녁 요가 프로그램입니다.', '마포평생학습관', 'https://mapo.seoul.kr/classes/yoga', 'feed-yoga-02', 'https://images.unsplash.com/photo-1545205597-3d9d02c29597?w=600', 14, NOW() - INTERVAL '7 days', CURRENT_DATE + 10, CURRENT_DATE + 38, 'SEOUL_RESERVATION', 'PUBLISHED', NOW(), NOW()),

  -- 러닝 피드
  (6, 2, 'EVENT', '2026 서울 한강 나이트 10K 런', '시원한 강바람을 맞으며 달리는 야간 마라톤 축제! 입문자를 위한 5K 펀런 코스도 함께 운영됩니다.', '한국관광공사 TourAPI', 'https://korean.visitkorea.or.kr/detail/2002', 'feed-run-01', 'https://images.unsplash.com/photo-1452626038306-9aae5e071dd3?w=600', 19, NOW() - INTERVAL '4 days', CURRENT_DATE + 15, CURRENT_DATE + 15, 'TOUR_API', 'PUBLISHED', NOW(), NOW()),
  (7, 2, 'GUIDE', '무릎 통증 없이 달리는 초보 러너 착지법 가이드', '러닝화 고르는 요령부터 케이던스 180 유지법, 미드풋 착지의 기본 원리를 안내합니다.', '냠냠취미 편집팀', 'https://nyamnyam.kr/guides/running-posture', 'feed-run-02', 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=600', NULL, NOW() - INTERVAL '15 days', NULL, NULL, 'ADMIN', 'PUBLISHED', NOW(), NOW()),

  -- 도자기 공예 피드
  (8, 12, 'SHOW', '현대 도자 공예 기획전 「흙의 시간, 손의 기억」', '국내 신진 도예 작가 20인이 선보이는 생활 식기와 조형 오브제 전시회입니다.', '문화포털', 'https://culture.go.kr/detail/3001', 'feed-pottery-01', 'https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?w=600', 22, NOW() - INTERVAL '6 days', CURRENT_DATE - 5, CURRENT_DATE + 20, 'CULTURE_PORTAL', 'PUBLISHED', NOW(), NOW()),
  (9, 12, 'CLASS', '주말 물레 성형 & 나만의 브런치 보울 만들기', '물레를 처음 접하는 초보자를 위한 1:1 밀착 코칭 도예 체험입니다.', '서울 공공서비스예약', 'https://yeyak.seoul.go.kr/web/detail/1003', 'feed-pottery-02', 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600', 14, NOW() - INTERVAL '1 day', CURRENT_DATE + 2, CURRENT_DATE + 16, 'SEOUL_RESERVATION', 'PUBLISHED', NOW(), NOW()),

  -- 베이킹 피드
  (10, 21, 'CLASS', '프랑스 정통 구움과자 피낭시에 & 마들렌 클래스', '태운 버터의 풍미와 촉촉한 식감을 살리는 베이킹 기초 테크닉을 배웁니다.', '냠냠취미 편집팀', 'https://nyamnyam.kr/classes/baking-madeleine', 'feed-baking-01', 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600', 23, NOW() - INTERVAL '8 days', CURRENT_DATE + 4, CURRENT_DATE + 18, 'ADMIN', 'PUBLISHED', NOW(), NOW()),

  -- 전시 관람 피드
  (11, 30, 'SHOW', '국립현대미술관 올해의 작가상 2026', '동시대 한국 현대미술의 지형도를 조망하는 대표 정례전시입니다.', '문화포털', 'https://culture.go.kr/detail/3002', 'feed-exhibit-01', 'https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=600', 1, NOW() - INTERVAL '12 days', CURRENT_DATE - 10, CURRENT_DATE + 40, 'CULTURE_PORTAL', 'PUBLISHED', NOW(), NOW()),

  -- 목공 피드
  (12, 13, 'CLASS', '나만의 원목 도마 & 버터나이프 깎기 원데이 워크숍', '호두나무(월넛) 통원목을 샌딩하고 천연 오일로 마감하는 우드 카빙 수업입니다.', '서울 공공서비스예약', 'https://yeyak.seoul.go.kr/web/detail/1004', 'feed-wood-01', 'https://images.unsplash.com/photo-1538688525198-9b88f6f53126?w=600', 4, NOW() - INTERVAL '3 days', CURRENT_DATE + 1, CURRENT_DATE + 15, 'SEOUL_RESERVATION', 'PUBLISHED', NOW(), NOW()),

  -- 종료된 행사 (종료 행사 숨김 토글 필터 테스트용)
  (13, 1, 'EVENT', '2026 봄맞이 볼더링 페스티벌 (종료)', '다양한 이벤트 루트와 입문자 챌린지가 진행되었던 봄 축제입니다.', '한국관광공사 TourAPI', 'https://korean.visitkorea.or.kr/detail/2003', 'feed-ended-01', 'https://images.unsplash.com/photo-1522163182402-834f871fd851?w=600', 14, NOW() - INTERVAL '40 days', CURRENT_DATE - 35, CURRENT_DATE - 10, 'TOUR_API', 'PUBLISHED', NOW(), NOW())
ON CONFLICT (origin, source_ref) DO NOTHING;

SELECT setval(pg_get_serial_sequence('feed_items', 'id'), coalesce(max(id), 1)) FROM feed_items;

