# 냠냠취미 백엔드 작업 (91건)

> 프로젝트 명세서 v0.3 · 디자인 컨셉북 v1.0 · 2026-10-19 · Team SSabby D104  
> 필수 58 · 권장 33 · 진척 관리: `requirements-progress.xlsx`의 '백엔드' 시트 · 전체 안내: [README](README.md)

## 목차

| 대분류 | 필수 | 권장 | 합계 | 짝이 되는 프론트엔드 작업 |
|---|---|---|---|---|
| [공통/시스템](#공통시스템) | 12 | 4 | 16 | SYS-F01~F13 |
| [회원·인증](#회원인증) | 5 | 2 | 7 | AUTH-F01~F07 |
| [프로필·마이페이지](#프로필마이페이지) | 3 | 1 | 4 | USER-F01~F04 |
| [숏폼 탐색](#숏폼-탐색) | 10 | 3 | 13 | SHORT-F01~F09 |
| [취미 검색·상세](#취미-검색상세) | 2 | 0 | 2 | HOBBY-F01~F03 |
| [추천](#추천) | 6 | 1 | 7 | RECO-F01~F03 |
| [내 취미](#내-취미) | 3 | 1 | 4 | MY-F01~F04 |
| [주차별 TODO](#주차별-todo) | 2 | 2 | 4 | TODO-F01~F03 |
| [장소](#장소) | 1 | 4 | 5 | PLACE-F01~F03 |
| [정보 피드](#정보-피드) | 4 | 0 | 4 | FEED-F01 |
| [알림](#알림) | 0 | 3 | 3 | NOTI-F01~F02 |
| [커뮤니티](#커뮤니티) | 4 | 1 | 5 | BOARD-F01~F06 |
| [신고](#신고) | 1 | 0 | 1 | REPORT-F01 |
| [문의·공지](#문의공지) | 0 | 2 | 2 | CS-F01~F03 |
| [관리자](#관리자) | 5 | 5 | 10 | ADM-F01~F08 |
| [서비스 피드백](#서비스-피드백) | 0 | 4 | 4 | FB-F01~F02 |

## 표기
- **ID**: `도메인-파트번호`. 파트 F = 프론트엔드, B = 백엔드, P = 기획·운영. 같은 도메인의 F·B 작업이 짝입니다.
- **필수**: 명세서 P0. 11/04 개발 마감까지. 없으면 서비스 흐름이 성립하지 않음.
- **권장**: 명세서 P1(고도화 포함). 11/06 중간 점검 뒤 개선 기간(11/09~11/12)에 마무리.
- **근거**: 명세서 기능 ID(FR-\*)·비기능 ID(NFR-\*)·장 번호. 근거 항목의 규칙·예외가 곧 완료 기준(QA 체크리스트)입니다.
- **11장 #N**: 명세서 11장 '결정할 것'에 걸린 작업. 결정이 바뀌면 작업 내용도 같이 고칩니다.
- **체크박스**: 진척의 기준은 엑셀의 `진행표기`(완료·부분·미착수·제외)입니다. MD 체크박스는 MR을 올릴 때 같이 체크하는 용도로 씁니다.

## 공통/시스템

필수 12 · 권장 4 · 프론트엔드 짝: SYS-F01~F13

- [ ] **SYS-B01 프로젝트·로컬 환경 구성** · `필수` · 환경
  - Java 17 · Spring Boot 3 프로젝트, local·dev·prod 프로필, Docker Compose(MySQL 8·Redis 7·RabbitMQ), Flyway V1__init.sql(7.5 DDL, 테이블 41개), JPA ddl-auto: validate.
  - API·배치: 7.5 DDL
  - 근거: 9.2 · 9.5
  - 비고: 개발 시작 전 1순위
- [ ] **SYS-B02 공통 응답·예외 처리** · `필수` · API 공통
  - 응답 봉투(success·data·error), 페이지 형식(content·page, size 최대 50), 에러 코드 체계(8.4), Bean Validation → error.fields, X-Request-Id 필터 + MDC 로그, CORS(프론트 도메인·credentials).
  - API·배치: 전체 API
  - 근거: 8.1 · 8.4 · NFR-SEC-05 · NFR-LOG-01
- [ ] **SYS-B03 인증·인가 기반** · `필수` · 보안
  - Spring Security + JWT(Access 30분 응답 본문, Refresh 14일 HttpOnly·Secure·SameSite=Lax 쿠키, DB 해시 저장·재발급 시 교체), 권한 GUEST·USER·ADMIN, /api/v1/admin/** ADMIN 전용, 소유자 검사 공통 처리.
  - API·배치: auth_tokens · users
  - 근거: NFR-SEC-02~04 · 3장
- [ ] **SYS-B04 비회원 식별(guestKey)** · `필수` · 보안
  - 첫 체험 요청 때 guest_key 쿠키(HttpOnly, 7일)를 발급하고, 체험 API에서 회원 ID 또는 guestKey를 꺼내 쓰는 공통 처리를 만든다.
  - API·배치: exploration_sessions · short_reactions · recommendations
  - 근거: 3장 · 8.1
- [ ] **SYS-B05 이미지 업로드** · `필수` · 업로드
  - POST /uploads/images(multipart, purpose): JPG·PNG·WEBP 확장자·MIME 화이트리스트, 크기 제한(프로필 2MB·그 외 5MB), UUID 파일명, EC2 볼륨 저장 + Nginx 정적 서빙.
  - API·배치: /uploads/images
  - 근거: NFR-SEC-06 · 11장 #19
- [ ] **SYS-B06 RabbitMQ 공통** · `필수` · 비동기
  - 큐 4종(reco.reason · notification.create · feedback.analyze · feed.summarize) 선언, 1분 → 5분 → 15분 지연 재시도 + DLQ, 메시지는 ID만 담고 소비자가 DB 최신 상태로 멱등 처리.
  - API·배치: 9.3
  - 근거: 9.3
- [ ] **SYS-B07 GMS(GPT) 클라이언트** · `필수` · 외부 API
  - GPT-4o 호출 공통 클라이언트(타임아웃 5초, 프롬프트 템플릿 관리, 실패 시 호출부 대체 처리), 키는 환경변수.
  - API·배치: —
  - 근거: 9.2 · NFR-OPS-03 · NFR-SEC-07
- [ ] **SYS-B08 배치 공통** · `필수` · 배치
  - @Scheduled(Asia/Seoul), batch_runs 기록(조회·저장·제외 수, 외부 API 사용량, 메시지), RUNNING 행으로 같은 작업 중복 실행 막기.
  - API·배치: batch_runs
  - 근거: 9.4 · NFR-OPS-04
- [ ] **SYS-B09 정리·재집계 배치** · `권장` · 배치
  - 02:00 미종료 세션 ABANDONED(24시간)·비회원 데이터(7일)·알림(90일) 삭제, 02:30 좋아요·댓글·관심 저장·관련 없음 수 재집계.
  - API·배치: 9.4
  - 근거: 9.4 · 7.3
- [ ] **SYS-B10 메일 발송** · `필수` · 메일
  - SMTP(Gmail 앱 비밀번호) 템플릿 3종(가입 인증·비밀번호 재설정·소셜 계정 안내), 실패 시 3회 재시도.
  - API·배치: —
  - 근거: FR-AUTH-01 · FR-AUTH-04 · 11장 #20
  - 비고: 하루 발송 한도 확인
- [ ] **SYS-B11 Redis 요청량 제한** · `권장` · 보안
  - Bucket4j 토큰 버킷 정책 11종(5.1 표), 키 rl:{정책}:u:{userId} / ip:{IP}, 초과 시 429 COMMON_RATE_LIMITED + Retry-After + retryAfterSeconds, 성공 응답 X-RateLimit-Remaining, Redis 장애 시 허용(fail-open).
  - API·배치: 5.1
  - 근거: 5.1 · NFR-SEC-08
  - 비고: 고도화
- [ ] **SYS-B12 배포 파이프라인** · `필수` · 배포
  - GitLab CI → Docker 이미지 → EC2에서 docker compose pull && up -d, Nginx TLS(Let's Encrypt)·HTTP→HTTPS, api.{팀 도메인}, 비밀값은 .env·CI 변수.
  - API·배치: —
  - 근거: 9.5 · NFR-OPS-01 · NFR-SEC-01
- [ ] **SYS-B13 백업·로그** · `권장` · 운영
  - MySQL 일 1회 덤프(7일 보관), 개인정보 마스킹, ERROR 스택 트레이스.
  - API·배치: —
  - 근거: NFR-OPS-02 · NFR-LOG-01
- [ ] **SYS-B14 Swagger·단위 테스트** · `필수` · 품질
  - springdoc으로 요청·응답 예시 포함 문서, 서비스 계층 단위 테스트 라인 커버리지 60% 이상(추천·TODO 진행률 필수).
  - API·배치: Swagger
  - 근거: NFR-QA-01 · NFR-QA-02 · 10.3
- [ ] **SYS-B15 시드 데이터 적재** · `필수` · 데이터
  - 분류·태그·취미·TODO 원본·데이터 매핑표·행정구역(regions)·개발용 ACTIVE 숏폼을 시드 SQL로 넣는다.
  - API·배치: categories · tags · hobbies · plan_templates · hobby_source_mappings · regions · shorts
  - 근거: FR-SHORT-01 · FR-PLACE-03
  - 비고: PREP 작업 결과를 받아 적재
- [ ] **SYS-B16 추천 검증 이벤트 기록** · `권장` · 지표
  - 추천 노출·상세 클릭·관심 저장·시작하기·별로예요 이벤트를 event_logs에 남긴다.
  - API·배치: event_logs
  - 근거: NFR-LOG-02

## 회원·인증

필수 5 · 권장 2 · 프론트엔드 짝: AUTH-F01~F07

- [ ] **AUTH-B01 회원가입·중복 확인** · `필수` · 회원가입
  - POST /auth/signup(PENDING 생성, BCrypt), check-email·check-nickname, 입력 규칙 검증, 409 AUTH_EMAIL_DUPLICATED, 소셜로 가입한 이메일이면 안내 코드.
  - API·배치: /auth/signup · /auth/check-email · /auth/check-nickname · users
  - 근거: FR-AUTH-01
- [ ] **AUTH-B02 이메일 인증** · `필수` · 회원가입
  - 인증 토큰(24시간·1회용·SHA-256 해시), verify → ACTIVE 전환 + 로그인 토큰 발급, 재발송 60초 1회.
  - API·배치: /auth/email/verify · /auth/email/resend · auth_tokens
  - 근거: FR-AUTH-01
- [ ] **AUTH-B03 로그인·토큰 재발급·로그아웃** · `필수` · 로그인
  - login(5회 연속 실패 5분 잠금, 실패 문구 통일, PENDING 안내), refresh(Refresh 교체 발급), logout(Refresh 폐기·쿠키 삭제).
  - API·배치: /auth/login · /auth/refresh · /auth/logout
  - 근거: FR-AUTH-02
- [ ] **AUTH-B04 소셜 로그인** · `필수` · 소셜 로그인
  - OAuth2 Client(Google·Kakao·Naver, Authorization Code), 콜백 → 토큰 발급 → 프론트 /oauth/callback 리다이렉트, (provider, providerUserId) 식별, 최초 가입은 임시 토큰 + /auth/oauth/signup 닉네임 확정, 같은 이메일 자동 연동 안 함.
  - API·배치: /oauth2/** · /auth/oauth/signup · social_accounts
  - 근거: FR-AUTH-03
  - 비고: 11장 #2
- [ ] **AUTH-B05 비밀번호 재설정** · `필수` · 비밀번호
  - reset-request(가입 여부와 관계없이 같은 응답, 30분·1회용 토큰, 소셜 전용 계정에는 안내 메일), reset(규칙 검증, Refresh 전부 폐기).
  - API·배치: /auth/password/reset-request · /auth/password/reset
  - 근거: FR-AUTH-04
- [ ] **AUTH-B06 비회원 기록 이관** · `권장` · 비회원
  - POST /explorations/merge: guestKey의 세션·반응·추천을 회원으로 1회 이관(같은 영상 반응은 회원 기록 유지), 쿠키 삭제.
  - API·배치: /explorations/merge
  - 근거: FR-AUTH-05
- [ ] **AUTH-B07 회원 탈퇴** · `권장` · 탈퇴
  - DELETE /users/me: 비밀번호 확인, 이메일·닉네임 즉시 익명화, 글·댓글은 '탈퇴한 사용자'로 유지, 토큰 폐기, 같은 이메일 재가입 허용.
  - API·배치: /users/me
  - 근거: FR-AUTH-06

## 프로필·마이페이지

필수 3 · 권장 1 · 프론트엔드 짝: USER-F01~F04

- [ ] **USER-B01 온보딩 저장** · `필수` · 온보딩
  - PUT /users/me/onboarding: 관심 취미(최대 5, SAVED로도 저장)·숙련도·이용 상황(시간 0~40, 예산 1만 원 단위)·선호 지역, 미입력은 null.
  - API·배치: /users/me/onboarding · user_conditions · user_hobbies
  - 근거: FR-USER-01
- [ ] **USER-B02 내 정보·수정** · `필수` · 프로필
  - GET·PATCH /users/me(닉네임 중복·프로필 이미지), PUT /users/me/conditions(이용 상황·선호 지역). 저장해도 기존 추천은 그대로.
  - API·배치: /users/me · /users/me/conditions
  - 근거: FR-USER-02 · FR-PLACE-01
- [ ] **USER-B03 비밀번호 변경** · `권장` · 비밀번호
  - 현재 비밀번호 확인, 같은 비밀번호 거부, 다른 기기 Refresh 폐기, 소셜 계정 거부.
  - API·배치: /users/me/password
  - 근거: FR-USER-03
- [ ] **USER-B04 마이페이지 요약·내 글** · `필수` · 마이페이지
  - GET /users/me/summary(관심 취미·진행 중·보관한 숏폼·내 글 수), GET /users/me/posts.
  - API·배치: /users/me/summary · /users/me/posts
  - 근거: FR-USER-04

## 숏폼 탐색

필수 10 · 권장 3 · 프론트엔드 짝: SHORT-F01~F09

- [ ] **SHORT-B01 탐색 세션 API** · `필수` · 탐색 세션
  - POST /explorations(세션 + 첫 영상), progress(진행률·반응한 취미·관심 주제 %), finish(종료 + 추천 생성 호출), 세션 상태 IN_PROGRESS·COMPLETED·ABANDONED.
  - API·배치: /explorations · /explorations/{id}/progress · /explorations/{id}/finish
  - 근거: FR-SHORT-03 · FR-SHORT-04
- [ ] **SHORT-B02 다음 영상 선택** · `필수` · 다음 영상
  - 첫 진입은 분류 분산(1~4번 서로 다른 분류), 이후 선호 70%·탐색 30%(prefer-ratio 설정값, 매번 난수), 별로예요 취미 세션 제외, 본 영상·관련 없음·비ACTIVE 제외, 후보 부족 시 반응 없던 영상 재사용, 12개 또는 후보 없음이면 finished.
  - API·배치: /explorations/{id}/next
  - 근거: FR-SHORT-03
  - 비고: 11장 #3 · #4
- [ ] **SHORT-B03 노출·시청·재생 실패 기록** · `필수` · 시청 기록
  - POST views: watchedSeconds(길이 상한)·watch_ratio 저장, 고유 시청자 view_user_count 증가, playFailed 기록 → 서로 다른 3명 이상 실패 시 UNAVAILABLE.
  - API·배치: /explorations/{id}/views · exploration_views · shorts
  - 근거: FR-SHORT-01 · FR-SHORT-02
- [ ] **SHORT-B04 반응 API** · `필수` · 반응
  - PUT·DELETE /shorts/{id}/reaction: 사용자(또는 guestKey) × 영상 1개, 변경·취소, IRRELEVANT면 irrelevant_count ±1(고유 사용자 기준).
  - API·배치: /shorts/{id}/reaction · short_reactions
  - 근거: FR-SHORT-02 · FR-SHORT-08
- [ ] **SHORT-B05 관심사 직접 선택·초기화** · `필수` · 직접 선택
  - GET /categories/with-hobbies, PUT /users/me/interest-selections(최대 10), POST /users/me/interest-reset(interest_reset_at 갱신, 보관함·관련 없음 기록은 유지).
  - API·배치: interest_selections · users
  - 근거: FR-SHORT-05
- [ ] **SHORT-B06 보관 API** · `권장` · 보관함
  - PUT·DELETE /shorts/{id}/save(회원, 반응과 별개·점수 미반영), GET /users/me/saved-shorts(최근 보관순·취미 필터·이용 불가 표시).
  - API·배치: /shorts/{id}/save · /users/me/saved-shorts · short_saves
  - 근거: FR-SHORT-06
  - 비고: 11장 #31
- [ ] **SHORT-B07 숏폼 영상 수집(매일 04:00)** · `필수` · 수집 배치
  - ACTIVE 채널마다 playlistItems.list(last_collected_at 이후), videos.list 50개 묶음, 기계적 필터(180초 이하·embeddable·public·KR 재생 가능) 불합격은 REJECTED(사유), 실패 채널은 다음 날 이어서, 할당량 사용량 기록.
  - API·배치: YouTube Data API · youtube_channels · shorts · batch_runs
  - 근거: FR-SHORT-07 · NFR-EXT-01
- [ ] **SHORT-B08 영상 취미 지정** · `필수` · 수집 배치
  - 단일 채널은 채널 취미 상속, 종합 채널은 숏폼 키워드 매핑으로, 그래도 못 정하면 GMS(제목·설명·썸네일 → hobbyId·confidence·isHobbyContent). 신뢰도 0.7 미만·실패는 NEEDS_REVIEW.
  - API·배치: shorts.assign_method · assign_confidence
  - 근거: FR-SHORT-07
  - 비고: 11장 #25
- [ ] **SHORT-B09 주간 반영(목 06:00)·지금 반영** · `필수` · 수집 배치
  - 그 주 CANDIDATE와 승인된 NEEDS_REVIEW를 ACTIVE로(released_at), 관리자 수동 실행 지원, 실행 중 409 ADMIN_RELEASE_IN_PROGRESS.
  - API·배치: /admin/shorts/release · shorts
  - 근거: FR-SHORT-07
  - 비고: 11장 #23
- [ ] **SHORT-B10 기존 영상 점검(매일 04:30)** · `필수` · 수집 배치
  - ACTIVE 영상을 videos.list로 50개씩 다시 조회해 삭제·비공개·퍼가기 금지·지역 제한이 생기면 UNAVAILABLE.
  - API·배치: shorts.last_checked_at
  - 근거: FR-SHORT-07
- [ ] **SHORT-B11 신뢰 채널 발굴(매주 월 06:00)** · `권장` · 수집 배치
  - 숏폼 키워드로 search.list(type=video·videoDuration=short·regionCode=KR), 여러 번 나온 채널을 CANDIDATE로 올린다. 하루 100회 한도 안.
  - API·배치: youtube_channels(discovered_by=SEARCH)
  - 근거: FR-SHORT-07 · NFR-EXT-01
  - 비고: 초기엔 수동 등록으로 대체 가능
- [ ] **SHORT-B12 관련 없음 자동 숨김** · `필수` · 걸러 내기
  - 관련 없음 5명 이상 그리고 비율 20% 이상이면 즉시 AUTO_HIDDEN, 채널 자동 숨김 3건 이상이면 '점검 필요', 관리자 복구(카운트 초기화)·영구 제외.
  - API·배치: shorts · youtube_channels.hidden_short_count
  - 근거: FR-SHORT-08
  - 비고: 11장 #26
- [ ] **SHORT-B13 YouTube 할당량 경고** · `권장` · 운영
  - 하루 사용량이 80%를 넘으면 Mattermost 운영 채널에 경고.
  - API·배치: batch_runs.quota_used
  - 근거: NFR-EXT-01

## 취미 검색·상세

필수 2 · 권장 0 · 프론트엔드 짝: HOBBY-F01~F03

- [ ] **HOBBY-B01 취미 검색 API** · `필수` · 검색
  - GET /hobbies: 취미명·태그 부분 일치(2자 이상), 필터 5종 AND, 정렬(추천순: 로그인은 관심사 점수·비회원은 인기순 / 이름순 / 비용 낮은순), 20개. GET /categories, /tags.
  - API·배치: /hobbies · /categories · /tags
  - 근거: FR-HOBBY-01
- [ ] **HOBBY-B02 취미 상세 API** · `필수` · 상세
  - GET /hobbies/{id}(PUBLISHED만, 로그인 시 myStatus), /shorts(ACTIVE 최대 6), /plan-preview(1~2주차), 관련 정보 피드 3개.
  - API·배치: /hobbies/{id} · /hobbies/{id}/shorts · /hobbies/{id}/plan-preview
  - 근거: FR-HOBBY-02

## 추천

필수 6 · 권장 1 · 프론트엔드 짝: RECO-F01~F03

- [ ] **RECO-B01 관심사 점수 계산** · `필수` · 점수 계산
  - 영상 점수 = 반응(LIKE +3, DISLIKE −3) + 시청 가중치(시청 비율 × 2 − 1), 취미 점수 = 영상 점수 합 + 직접 선택 +5, 태그 점수 = 영상 점수 × 0.3. 초기화 이후 반응만, 관련 없음·재생 실패 제외, 가중치는 application.yml.
  - API·배치: short_reactions · exploration_views · interest_selections
  - 근거: FR-RECO-01
  - 비고: 11장 #27
- [ ] **RECO-B02 추천 생성** · `필수` · 추천 생성
  - 관심 취미 상위 3개, 연관 취미 3개(유사도 × 0.7 + 태그 점수 × 0.3, 관심·진행 중·별로예요 제외), 이용 상황은 정렬 가중치(×0.8 + 조건 확인 필요 / ×1.1), 정보 부족 시 탐색용 6개, 스냅샷 저장, 3초 이내.
  - API·배치: /recommendations · recommendations · recommendation_items
  - 근거: FR-RECO-01 · NFR-PERF-02
  - 비고: 11장 #5 · #7
- [ ] **RECO-B03 추천 조회 API** · `필수` · 추천 조회
  - GET /recommendations/latest, /{id}(추천 이유 생성 상태 reason_status, 추천 기준 문구 basisText 포함).
  - API·배치: /recommendations/latest · /recommendations/{id}
  - 근거: FR-RECO-02
- [ ] **RECO-B04 추천 이유 생성(reco.reason)** · `필수` · 추천 이유
  - 추천 생성 직후 큐 발행 → GMS로 60자 이내 1문장, 실패하면 FALLBACK 템플릿('클라이밍 영상에 좋아요 3회').
  - API·배치: reco.reason · recommendation_items.reason
  - 근거: FR-RECO-02
- [ ] **RECO-B05 별로예요 API** · `필수` · 추천 피드백
  - PUT·DELETE /hobbies/{id}/dislike(hobby_dislikes), 이후 추천·연관 후보에서 제외.
  - API·배치: /hobbies/{id}/dislike · hobby_dislikes
  - 근거: FR-RECO-03
  - 비고: 11장 #6
- [ ] **RECO-B06 취미 유사도 배치(Python)** · `필수` · 유사도
  - 태그 벡터 코사인 유사도를 hobby_similarity에 저장. 매일 03:00 + 관리자 수동 실행, Python 컨테이너 cron.
  - API·배치: hobby_similarity
  - 근거: FR-RECO-01 · 9.4
  - 비고: 11장 #8
- [ ] **RECO-B07 추천 가중치 튜닝** · `권장` · 분석
  - 사용자 테스트 후 event_logs·반응 데이터로 가중치를 분석·조정한다(pandas·scikit-learn).
  - API·배치: event_logs
  - 근거: 9.2 · NFR-LOG-02

## 내 취미

필수 3 · 권장 1 · 프론트엔드 짝: MY-F01~F04

- [ ] **MY-B01 내 취미 목록 API** · `필수` · 목록
  - GET /users/me/hobbies?status=(진행률 포함, 상태별 묶음).
  - API·배치: /users/me/hobbies · user_hobbies
  - 근거: FR-MY-01~03
- [ ] **MY-B02 관심 저장·해제** · `필수` · 관심 저장
  - PUT·DELETE /users/me/hobbies/{id}/save: SAVED 생성·삭제(사용자 × 취미 1행), 진행 중·일시중지는 해제 불가, save_count 증감.
  - API·배치: /users/me/hobbies/{id}/save
  - 근거: FR-MY-01
- [ ] **MY-B03 시작하기** · `필수` · 시작하기
  - POST start: 숙련도 원본을 사용자 계획으로 복사(없으면 하위 숙련도, 하나도 없으면 거부), 동시 진행 3개 초과 409 MY_HOBBY_LIMIT, STOPPED·COMPLETED 재시작 시 새 계획 + 이전 ARCHIVED.
  - API·배치: /users/me/hobbies/{id}/start · user_plans · user_plan_items
  - 근거: FR-MY-02
  - 비고: 11장 #10
- [ ] **MY-B04 일시중지·다시 시작·그만두기** · `권장` · 상태 변경
  - PATCH status(PAUSED·IN_PROGRESS·STOPPED, paused_days 누적), 모든 활동이 DONE·SKIPPED면 COMPLETED.
  - API·배치: /users/me/hobbies/{id}/status
  - 근거: FR-MY-03

## 주차별 TODO

필수 2 · 권장 2 · 프론트엔드 짝: TODO-F01~F03

- [ ] **TODO-B01 계획 조회 API** · `필수` · 조회
  - GET /plans/{id}?week=: 현재 주차(시작일부터 주 수 + 1, 일시중지 기간 제외, 최대 N), 밀린 활동 수, 진행률 = DONE ÷ 전체(삭제 제외), 본인만.
  - API·배치: /plans/{id}
  - 근거: FR-TODO-01
- [ ] **TODO-B02 활동 상태·메모** · `필수` · 상태 변경
  - PATCH items: DONE ↔ TODO, SKIPPED, completed_at, 메모 500자, PAUSED·STOPPED는 409 TODO_PLAN_NOT_ACTIVE, 응답에 progress.
  - API·배치: /plans/{id}/items/{itemId}
  - 근거: FR-TODO-02
- [ ] **TODO-B03 내 계획 수정** · `권장` · 계획 수정
  - POST·DELETE items(원본 활동 삭제는 soft delete), PATCH weekNo·expectedMinutes. 원본·다른 사용자에 영향 없음.
  - API·배치: /plans/{id}/items
  - 근거: FR-TODO-03
  - 비고: 11장 #11
- [ ] **TODO-B04 주간 TODO 알림(월 09:00)** · `권장` · 알림
  - 진행 중 계획마다 이번 주 TODO 알림을 notification.create로 발행(dedup WEEKLY_TODO:{planId}:{yyyy-ww}).
  - API·배치: notifications
  - 근거: FR-NOTI-01

## 장소

필수 1 · 권장 4 · 프론트엔드 짝: PLACE-F01~F03

- [ ] **PLACE-B01 지역 API** · `필수` · 지역
  - GET /regions?sido=(시·도 / 시·군·구), 행정구역 코드 시드.
  - API·배치: /regions · regions
  - 근거: FR-PLACE-01
- [ ] **PLACE-B02 장소 목록 API** · `권장` · 장소 목록
  - GET /places?regionId=&hobbyId=: PUBLISHED, 좌표, '정보 확인 필요'(운영 시간·비용 비었거나 90일 넘게 갱신 안 됨) 플래그.
  - API·배치: /places · places · hobby_places
  - 근거: FR-PLACE-02
- [ ] **PLACE-B03 공공데이터 장소 수집(매월 1일 03:30)** · `권장` · 수집 배치
  - 전국도시공원정보표준데이터·공공체육시설 데이터를 서울만 받아 공공데이터 종목명 매핑으로 취미 연결, 카카오 주소 → 좌표 변환(실패 시 목록만), 바로 PUBLISHED.
  - API·배치: places · place_sources · batch_runs
  - 근거: FR-PLACE-03 · NFR-EXT-02
- [ ] **PLACE-B04 카카오 로컬 장소 수집(매주 화 03:30)** · `권장` · 수집 배치
  - 서울 25개 구 × 취미별 카카오 검색어 × 3페이지 키워드 검색, 카테고리로 무관한 결과 제외, 새 장소는 PENDING_REVIEW.
  - API·배치: places · place_sources
  - 근거: FR-PLACE-03 · NFR-EXT-02
  - 비고: 팀 공용 앱 키 1개
- [ ] **PLACE-B05 장소 중복 병합** · `권장` · 병합
  - 좌표 50m 이내 그리고 이름 유사도 0.6 이상(공백·괄호·'점' 제거)이면 병합, 대표 정보는 공공 > 카카오, 출처 원본은 place_sources, (source, source_ref) 멱등.
  - API·배치: places · place_sources
  - 근거: FR-PLACE-03
  - 비고: 11장 #28

## 정보 피드

필수 4 · 권장 0 · 프론트엔드 짝: FEED-F01

- [ ] **FEED-B01 정보 피드 API** · `필수` · 피드
  - GET /feeds: 회원은 관심·진행 중 취미 기준(없으면 인기 취미), 유형·취미 필터, 종료 행사 제외 옵션, 최신순, PUBLISHED만.
  - API·배치: /feeds · feed_items
  - 근거: FR-FEED-01
- [ ] **FEED-B02 행사·공연·전시 수집(매일 05:00)** · `필수` · 수집 배치
  - TourAPI(축제·행사), 문화포털·KOPIS(공연·전시)를 서울만 받아 (origin, source_ref)로 건너뛰기·변경분 갱신, 행사 검색어 매핑으로 취미 연결.
  - API·배치: feed_items · batch_runs
  - 근거: FR-FEED-02 · NFR-EXT-02
  - 비고: 11장 #29
- [ ] **FEED-B03 공공 강좌 수집(매일 05:30)** · `필수` · 수집 배치
  - 서울 열린데이터광장 교육 공공서비스예약 정보를 받아 강좌·클래스로 저장한다.
  - API·배치: feed_items(origin=SEOUL_RESERVATION)
  - 근거: FR-FEED-02
- [ ] **FEED-B04 피드 요약·취미 지정(feed.summarize)** · `필수` · 요약
  - GMS로 요약 2줄과 취미를 붙이고(어디에도 안 붙으면 저장 안 함) PENDING_REVIEW로 둔다. 제목 유사도 0.8 + 같은 시작일이면 중복 제거. 실패하면 요약 없이 검토 대기.
  - API·배치: feed.summarize · feed_items
  - 근거: FR-FEED-02 · 9.3

## 알림

필수 0 · 권장 3 · 프론트엔드 짝: NOTI-F01~F02

- [ ] **NOTI-B01 알림 조회·읽음 API** · `권장` · 알림 API
  - GET /notifications?unreadOnly=(20개씩), /unread-count, PATCH /{id}/read, /read-all.
  - API·배치: /notifications · notifications
  - 근거: FR-NOTI-01
- [ ] **NOTI-B02 알림 생성 컨슈머** · `권장` · 알림 생성
  - notification.create: 댓글·대댓글·문의 답변·신고 처리·공지 알림 저장, dedup_key UNIQUE로 중복 방지.
  - API·배치: notification.create · notifications
  - 근거: FR-NOTI-01 · 9.3
  - 비고: 11장 #16
- [ ] **NOTI-B03 새 피드 알림(매일 09:00)** · `권장` · 알림 생성
  - 관심 취미의 새 정보 피드를 하루 1회 묶음 알림으로 만든다.
  - API·배치: notifications
  - 근거: FR-NOTI-01

## 커뮤니티

필수 4 · 권장 1 · 프론트엔드 짝: BOARD-F01~F06

- [ ] **BOARD-B01 게시판 목록 API** · `필수` · 목록
  - GET /hobbies/{id}/posts: 유형·키워드(FULLTEXT ngram)·정렬(최신·좋아요)·20개, HIDDEN·DELETED 제외.
  - API·배치: /hobbies/{id}/posts · posts
  - 근거: FR-BOARD-01
- [ ] **BOARD-B02 게시글 작성·수정·삭제** · `필수` · 작성
  - POST·PATCH·DELETE /posts: 입력 검증(제목 2~50, 본문 10~5,000, 이미지 5장), HTML 이스케이프 저장, 본인만(403 BOARD_FORBIDDEN), soft delete 후 상세 404.
  - API·배치: /posts · /posts/{id} · post_images
  - 근거: FR-BOARD-02 · NFR-SEC-05
- [ ] **BOARD-B03 게시글 상세·조회수** · `필수` · 상세
  - GET /posts/{id}: 조회수는 회원(비회원은 guestKey) 기준 24시간에 1회.
  - API·배치: /posts/{id}
  - 근거: FR-BOARD-03
- [ ] **BOARD-B04 댓글·대댓글 API** · `필수` · 댓글
  - GET·POST /posts/{id}/comments, PATCH·DELETE /comments/{id}: 1~500자, 대댓글 1단계(답글의 답글도 부모 아래), 대댓글 있으면 '삭제된 댓글입니다', 댓글 알림 발행.
  - API·배치: comments · notification.create
  - 근거: FR-BOARD-03
- [ ] **BOARD-B05 좋아요 API** · `권장` · 좋아요
  - PUT·DELETE /posts/{id}/like, /comments/{id}/like: UNIQUE(user, targetType, targetId), like_count.
  - API·배치: likes
  - 근거: FR-BOARD-04

## 신고

필수 1 · 권장 0 · 프론트엔드 짝: REPORT-F01

- [ ] **REPORT-B01 신고 접수 API** · `필수` · 신고
  - POST /reports: 대상(게시글·댓글·사용자)·사유 5종·기타 상세 필수, 중복 409 REPORT_DUPLICATED, 본인 콘텐츠 400 REPORT_SELF, 접수만으로 숨기지 않음.
  - API·배치: /reports · reports
  - 근거: FR-REPORT-01

## 문의·공지

필수 0 · 권장 2 · 프론트엔드 짝: CS-F01~F03

- [ ] **CS-B01 고객 문의 API** · `권장` · 문의
  - GET·POST /inquiries, GET /inquiries/{id}(본인·관리자만, 그 외 403), 답변 전 PATCH·DELETE(답변 후 409 INQUIRY_ALREADY_ANSWERED).
  - API·배치: /inquiries · inquiries
  - 근거: FR-CS-01
- [ ] **CS-B02 공지 API** · `권장` · 공지
  - GET /notices(상단 고정 → 게시일 최신순), /notices/{id}, 비회원 조회 가능.
  - API·배치: /notices · notices
  - 근거: FR-CS-02

## 관리자

필수 5 · 권장 5 · 프론트엔드 짝: ADM-F01~F08

- [ ] **ADM-B01 취미 관리 API** · `필수` · 취미 콘텐츠
  - GET·POST /admin/hobbies, GET·PUT /{id}, PATCH /{id}/status(필수 항목 검사 → 422 ADMIN_NOT_PUBLISHABLE + fields), 분류·태그 관리, 처리자·시각 기록.
  - API·배치: /admin/hobbies · /admin/categories · /admin/tags
  - 근거: FR-ADM-01
- [ ] **ADM-B02 태그 추천(GMS)** · `권장` · 취미 콘텐츠
  - POST /admin/hobbies/{id}/tag-suggestions: 소개 문구로 태그 후보 생성(admin-ai 요청량 제한).
  - API·배치: /admin/hobbies/{id}/tag-suggestions
  - 근거: FR-ADM-01
  - 비고: 없어도 수동 태그로 공개 가능
- [ ] **ADM-B03 TODO 원본 API** · `필수` · TODO 원본
  - GET·POST /admin/plan-templates, PUT /{id}(활동 전체 저장, 필수 항목, 버전 +1), PATCH /{id}/status. 공개 원본이 있어야 시작하기 가능.
  - API·배치: /admin/plan-templates · plan_templates · plan_template_items
  - 근거: FR-ADM-02
- [ ] **ADM-B04 신고 처리 API** · `필수` · 신고
  - GET /admin/reports(대상별 묶음·고유 신고자 수), 대상 원문 + 신고 목록 + 과거 조치, POST actions(HIDE·KEEP·UNHIDE + note) → 대상 상태·신고 일괄 처리·신고자 알림.
  - API·배치: /admin/reports/** · reports
  - 근거: FR-ADM-03
- [ ] **ADM-B05 문의 답변 API** · `권장` · 문의
  - GET /admin/inquiries?status=, PUT /{id}/answer → ANSWERED + 작성자 알림.
  - API·배치: /admin/inquiries
  - 근거: FR-ADM-04
- [ ] **ADM-B06 공지 관리 API** · `권장` · 공지
  - /admin/notices CRUD, 임시 저장·게시·상단 고정, 게시 시 전체 알림 발행 옵션.
  - API·배치: /admin/notices
  - 근거: FR-ADM-05
- [ ] **ADM-B07 피드·장소 관리 API** · `권장` · 피드·장소
  - /admin/feeds·/admin/places 등록·수정·비공개, PENDING_REVIEW 검토 후 공개.
  - API·배치: /admin/feeds · /admin/places
  - 근거: FR-ADM-06
- [ ] **ADM-B08 데이터 매핑 API** · `권장` · 데이터 매핑
  - GET·PUT /admin/hobbies/{id}/source-mappings(카카오 검색어·공공데이터 종목명·행사 검색어·숏폼 키워드). 바꾼 매핑은 다음 수집부터.
  - API·배치: hobby_source_mappings
  - 근거: FR-ADM-06
- [ ] **ADM-B09 신뢰 채널 관리 API** · `필수` · 숏폼
  - GET·POST /admin/channels(URL·ID → channels.list로 이름·업로드 재생목록 확인), PATCH /{id}(유형·취미·상태, 후보 승인·거절, 수집 중지).
  - API·배치: /admin/channels · youtube_channels
  - 근거: FR-ADM-07
- [ ] **ADM-B10 숏폼 검수 API** · `필수` · 숏폼
  - GET /admin/shorts?status=&hobbyId=, PATCH /{id}(취미 지정·승인·제외·복구), GET·POST /admin/shorts/release, GET /admin/batch-runs?job=.
  - API·배치: /admin/shorts · /admin/shorts/release · /admin/batch-runs
  - 근거: FR-ADM-07 · FR-SHORT-08

## 서비스 피드백

필수 0 · 권장 4 · 프론트엔드 짝: FB-F01~F02

- [ ] **FB-B01 피드백 제출 API** · `권장` · 제출
  - POST /feedbacks: 인증 → 요청량 제한(10분 3회) → 검증 → RECEIVED 저장 → 202, feedback.analyze 발행.
  - API·배치: /feedbacks · feedbacks
  - 근거: FR-FB-01
  - 비고: 고도화
- [ ] **FB-B02 피드백 AI 분석** · `권장` · AI 분석
  - feedback.analyze 소비: GMS로 우선순위(URGENT·HIGH·NORMAL·LOW)·유형·요약 → ANALYZED, 실패 시 재시도 후 ANALYSIS_FAILED, 상태 확인으로 멱등.
  - API·배치: feedback.analyze
  - 근거: FR-FB-02
  - 비고: 고도화
- [ ] **FB-B03 Mattermost 알림** · `권장` · 알림
  - URGENT는 즉시 Incoming Webhook(개인정보 제외), 나머지는 매일 18:00 요약 1건(미분석 원문 포함), notified_at으로 중복 방지.
  - API·배치: Mattermost Webhook
  - 근거: FR-FB-02
  - 비고: 고도화 · 11장 #22
- [ ] **FB-B04 관리자 피드백 API** · `권장` · 관리자
  - GET /admin/feedbacks?priority=&status=, POST /{id}/reanalyze.
  - API·배치: /admin/feedbacks
  - 근거: FR-FB-02
  - 비고: 고도화
