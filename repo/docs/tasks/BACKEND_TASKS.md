# 냠냠취미 백엔드 작업 (91건)

> 프로젝트 명세서 v0.4 · 기능 요구사항 CSV(10/21) 반영 · 디자인 컨셉북 v1.0 · 2026-10-21 · Team SSabby D104  
> 필수 38 · 권장 27 · 후순위 9 · 제외 17 (MY/TODO 2026-10-30 개정) · 진척 관리: `requirements-progress.xlsx`의 '백엔드' 시트 · 전체 안내: [README](README.md)

## 목차

| 대분류 | 필수 | 권장 | 후순위 | 제외 | 합계 | 짝이 되는 프론트엔드 작업 |
|---|---|---|---|---|---|---|
| [공통/시스템](#공통시스템) | 9 | 5 | 1 | 1 | 16 | SYS-F01~F13 |
| [회원·인증](#회원인증) | 5 | 1 | 0 | 1 | 7 | AUTH-F01~F07 |
| [프로필·마이페이지](#프로필마이페이지) | 1 | 3 | 0 | 0 | 4 | USER-F01~F04 |
| [숏폼 탐색](#숏폼-탐색) | 9 | 2 | 0 | 2 | 13 | SHORT-F01~F09 |
| [취미 검색·상세](#취미-검색상세) | 2 | 0 | 0 | 0 | 2 | HOBBY-F01~F03 |
| [추천](#추천) | 5 | 2 | 0 | 0 | 7 | RECO-F01~F03 |
| [내 취미](#내-취미) | 4 | 0 | 0 | 0 | 4 | MY-F01~F04 |
| [주차별 TODO/전체 계획](#주차별-todo--전체-계획) | 3 | 0 | 0 | 1 | 4 | TODO-F01~F04 |
| [장소](#장소) | 0 | 5 | 0 | 0 | 5 | PLACE-F01~F03 |
| [정보 피드](#정보-피드) | 0 | 4 | 0 | 0 | 4 | FEED-F01 |
| [알림](#알림) | 0 | 0 | 0 | 3 | 3 | NOTI-F01~F02 |
| [커뮤니티](#커뮤니티) | 0 | 5 | 0 | 0 | 5 | BOARD-F01~F06 |
| [신고](#신고) | 0 | 0 | 1 | 0 | 1 | REPORT-F01 |
| [문의·공지](#문의공지) | 0 | 0 | 2 | 0 | 2 | CS-F01~F03 |
| [관리자](#관리자) | 0 | 0 | 5 | 5 | 10 | ADM-F01~F08 |
| [서비스 피드백](#서비스-피드백) | 0 | 0 | 0 | 4 | 4 | FB-F01~F02 |
| **합계** | **38** | **27** | **9** | **17** | **91** | |

## 표기
- **ID**: `도메인-파트번호`. 파트 F = 프론트엔드, B = 백엔드, P = 기획·운영. 같은 도메인의 F·B 작업이 짝입니다.
- **필수**: P0(MVP 필수). 11/04 개발 마감까지. 없으면 서비스 흐름이 성립하지 않음.
- **권장**: P1(MVP 권장). 11/06 중간 점검 뒤 개선 기간(11/09~11/12)에 마무리.
- **후순위**: P3. P0·P1을 마친 뒤 시간이 남으면 개발(개선 기간 안에 못 하면 발표 이후).
- **제외**: P2(MVP 제외). 이번 개발 범위에서 빼고 고도화 후보로만 남김. 엑셀 진행표기도 '제외'로 둡니다.
- **개발 순서**: P0 → P1 → P3. P2는 개발하지 않습니다(번호 순서와 다름). 우선순위는 기능 요구사항 CSV(`../spec/feature-requirements.csv`, 10/21) 기준입니다.
- **우선순위가 섞인 작업**: 가장 앞선 등급(필수 > 권장 > 후순위 > 제외)으로 표기하고, 등급이 낮은 부분은 본문에 `[P1]`·`[P2·MVP 제외]`·`[P3]`로 표시하고 비고에 적습니다.
- **근거**: 명세서 기능 ID(FR-\*)·비기능 ID(NFR-\*)·장 번호. 근거 항목의 규칙·예외가 곧 완료 기준(QA 체크리스트)입니다.
- **11장 #N**: 명세서 11장 '결정할 것'에 걸린 작업. 결정이 바뀌면 작업 내용도 같이 고칩니다.
- **체크박스**: 진척의 기준은 엑셀의 `진행표기`(완료·부분·미착수·제외)입니다. MD 체크박스는 MR을 올릴 때 같이 체크하는 용도로 씁니다.

## 공통/시스템

필수 9 · 권장 5 · 후순위 1 · 제외 1 · 프론트엔드 짝: SYS-F01~F13

- [x] **SYS-B01 프로젝트·로컬 환경 구성** · `필수` · 환경
  - Java 17 · Spring Boot 4.1(Gradle 9.8) 프로젝트, local·dev·prod 프로필, Docker Compose(PostgreSQL 17 + pgvector, 이미지 `pgvector/pgvector:pg17`·Redis 7·RabbitMQ), Flyway V1__init.sql(7.5 DDL, 테이블 41개), JPA ddl-auto: validate.
  - DB 이미지 변경(2026-10-23): 로컬·운영 설정 원본을 `pgvector/pgvector:pg17`로 통일. 운영 DB 이미지 교체를 먼저 수행하고, `vector` 확장 활성화와 임베딩 스키마는 신규 Flyway 마이그레이션으로 추가한다. 기존 V1은 유지하며 Jenkins의 백엔드 배포만으로 운영 DB 이미지는 바뀌지 않는다.
  - API·배치: 7.5 DDL
  - 근거: 9.2 · 9.5
  - 비고: 개발 시작 전 1순위
- [x] **SYS-B02 공통 응답·예외 처리** · `필수` · API 공통
  - 응답 봉투(success·data·error), 페이지 형식(content·page, size 최대 50), 에러 코드 체계(8.4), Bean Validation → error.fields, X-Request-Id 필터 + MDC 로그, CORS(프론트 도메인·credentials).
  - API·배치: 전체 API
  - 근거: 8.1 · 8.4 · NFR-SEC-05 · NFR-LOG-01
- [x] **SYS-B03 인증·인가 기반** · `필수` · 보안
  - Spring Security + JWT(Access 30분 응답 본문, Refresh 14일 HttpOnly·Secure·SameSite=Lax 쿠키, DB 해시 저장·재발급 시 교체), 권한 GUEST·USER·ADMIN, /api/v1/admin/** ADMIN 전용(관리자 기능은 P3·P2), 소유자 검사 공통 처리.
  - API·배치: auth_tokens · users
  - 근거: NFR-SEC-02~04 · 3장
- [ ] **SYS-B04 비회원 식별(guestKey)** · `제외` · 보안
  - 첫 체험 요청 때 guest_key 쿠키(HttpOnly, 7일)를 발급하고, 체험 API에서 회원 ID 또는 guestKey를 꺼내 쓰는 공통 처리를 만든다.
  - API·배치: exploration_sessions · short_reactions · recommendations
  - 근거: 3장 · 8.1
  - 비고: MVP 제외(P2): 비회원 체험(FR-AUTH-05). MVP에서 숏폼 탐색·추천은 로그인 후에만(11장 #1)
- [x] **SYS-B05 이미지 업로드** · `권장` · 업로드
  - POST /uploads/images(multipart, purpose): JPG·PNG·WEBP 확장자·MIME 화이트리스트, 크기 제한(프로필 2MB·그 외 5MB), UUID 파일명, EC2 볼륨 저장 + Nginx 정적 서빙.
  - API·배치: /uploads/images
  - 근거: NFR-SEC-06 · 11장 #19
  - 비고: 쓰는 기능이 게시판(P1)·문의(P3)·프로필 이미지(P2)라 권장
- [x] **SYS-B06 RabbitMQ 공통** · `권장` · 비동기
  - 큐 reco.reason · feed.summarize 선언(notification.create · feedback.analyze는 [P2·MVP 제외]), 1분 → 5분 → 15분 지연 재시도 + DLQ, 메시지는 ID만 담고 소비자가 DB 최신 상태로 멱등 처리.
  - API·배치: 9.3
  - 근거: 9.3
  - 비고: 쓰는 기능이 AI 추천 이유(P1)·피드 요약(P1)이라 권장. notification.create·feedback.analyze 큐는 제외(P2)
- [x] **SYS-B07 GMS(GPT) 클라이언트** · `필수` · 외부 API
  - GPT-4o 호출 공통 클라이언트(타임아웃 5초, 프롬프트 템플릿 관리, 실패 시 호출부 대체 처리), 키는 환경변수.
  - API·배치: —
  - 근거: 9.2 · NFR-OPS-03 · NFR-SEC-07
- [x] **SYS-B08 배치 공통** · `필수` · 배치
  - @Scheduled(Asia/Seoul), batch_runs 기록(조회·저장·제외 수, 외부 API 사용량, 메시지), RUNNING 행으로 같은 작업 중복 실행 막기.
  - API·배치: batch_runs
  - 근거: 9.4 · NFR-OPS-04
- [ ] **SYS-B09 정리·재집계 배치** · `권장` · 배치
  - 02:00 미종료 세션 ABANDONED(24시간) 처리(비회원 데이터 7일·알림 90일 삭제는 [P2·MVP 제외]), 02:30 좋아요·댓글·관심 저장·관련 없음 수 재집계.
  - API·배치: 9.4
  - 근거: 9.4 · 7.3
- [x] **SYS-B10 메일 발송** · `필수` · 메일
  - SMTP(Gmail 앱 비밀번호) 템플릿 3종(가입 인증·비밀번호 재설정·소셜(Google) 계정 안내 [P1]), 실패 시 3회 재시도.
  - API·배치: —
  - 근거: FR-AUTH-01 · FR-AUTH-04 · 11장 #20
  - 비고: 하루 발송 한도 확인
- [ ] **SYS-B11 Redis 요청량 제한** · `후순위` · 보안
  - Bucket4j 토큰 버킷 정책 11종(5.1 표), 키 rl:{정책}:u:{userId} / ip:{IP}, 초과 시 429 COMMON_RATE_LIMITED + Retry-After + retryAfterSeconds, 성공 응답 X-RateLimit-Remaining, Redis 장애 시 허용(fail-open).
  - API·배치: 5.1
  - 근거: 5.1 · NFR-SEC-08
  - 비고: 고도화(P3 수준, 여유 시): CSV에 없는 비기능
- [x] **SYS-B12 배포 파이프라인** · `필수` · 배포
  - Jenkins(EC2 Docker 컨테이너, BE/Jenkinsfile): dev 병합 → GitLab 웹훅 → Docker 이미지 빌드(BE/Dockerfile) → EC2에서 backend 컨테이너 교체 → /actuator/health 확인 → Mattermost 알림, Nginx TLS(Let's Encrypt)·HTTP→HTTPS, https://k15d104.p.ssafy.io/api → 127.0.0.1:18080, 비밀값은 .env·Jenkins Credentials.
  - API·배치: —
  - 근거: 9.5 · NFR-OPS-01 · NFR-SEC-01
- [ ] **SYS-B13 백업·로그** · `권장` · 운영
  - PostgreSQL 일 1회 덤프(pg_dump, 7일 보관), 개인정보 마스킹, ERROR 스택 트레이스.
  - API·배치: —
  - 근거: NFR-OPS-02 · NFR-LOG-01
- [ ] **SYS-B14 Swagger·단위 테스트** · `필수` · 품질
  - springdoc으로 요청·응답 예시 포함 문서, 서비스 계층 단위 테스트 라인 커버리지 60% 이상(추천·TODO 진행률 필수).
  - API·배치: Swagger
  - 근거: NFR-QA-01 · NFR-QA-02 · 10.3
- [x] **SYS-B15 시드 데이터 적재** · `필수` · 데이터
  - 관리자 편집 화면이 MVP 제외(P2)라 분류·태그·취미 콘텐츠·TODO 원본·데이터 매핑표·신뢰 채널(youtube_channels)·입문 자료·행정구역(regions)·개발용 ACTIVE 숏폼을 시드 SQL로 넣는다. 운영 중 수정도 시드 SQL 추가 또는 DB 직접 수정으로 한다. 채널 후보 승인·NEEDS_REVIEW 영상 승인·자동 숨김 복구는 숏폼 검수 화면(ADM-F08·ADM-B09·B10, 후순위 P3)이 생기기 전까지 DB 직접 수정.
  - API·배치: categories · tags · hobbies · plan_templates · hobby_source_mappings · youtube_channels · feed_items · regions · shorts
  - 근거: FR-SHORT-01 · FR-PLACE-03
  - 비고: PREP 작업 결과를 받아 적재 · 관리자 화면(FR-ADM-01·02·06은 P2, FR-ADM-07 숏폼 검수는 P3)을 대신하는 MVP 운영 방식
- [ ] **SYS-B16 추천 검증 이벤트 기록** · `권장` · 지표
  - 추천 노출·상세 클릭·관심 저장·시작하기·별로예요 이벤트를 event_logs에 남긴다.
  - API·배치: event_logs
  - 근거: NFR-LOG-02

## 회원·인증

필수 5 · 권장 1 · 후순위 0 · 제외 1 · 프론트엔드 짝: AUTH-F01~F07

- [x] **AUTH-B01 회원가입·중복 확인** · `필수` · 회원가입
  - POST /auth/signup(PENDING 생성, BCrypt), check-email·check-nickname, 입력 규칙 검증, 409 AUTH_EMAIL_DUPLICATED, 소셜(Google)로 가입한 이메일이면 안내 코드 [P1].
  - API·배치: /auth/signup · /auth/check-email · /auth/check-nickname · users
  - 근거: FR-AUTH-01
- [x] **AUTH-B02 이메일 인증** · `필수` · 회원가입
  - 인증 토큰(24시간·1회용·SHA-256 해시), verify → ACTIVE 전환 + 로그인 토큰 발급, 재발송 60초 1회.
  - API·배치: /auth/email/verify · /auth/email/resend · auth_tokens
  - 근거: FR-AUTH-01
- [x] **AUTH-B03 로그인·토큰 재발급·로그아웃** · `필수` · 로그인
  - login(5회 연속 실패 5분 잠금, 실패 문구 통일, PENDING 안내), refresh(Refresh 교체 발급), logout(Refresh 폐기·쿠키 삭제).
  - API·배치: /auth/login · /auth/refresh · /auth/logout
  - 근거: FR-AUTH-02
- [x] **AUTH-B04 소셜 로그인** · `권장` · 소셜 로그인
  - OAuth2 Client(Google만, Authorization Code), 콜백 → 토큰 발급 → 프론트 /oauth/callback 리다이렉트, (provider, providerUserId) 식별, 최초 가입은 임시 토큰 + /auth/oauth/signup 닉네임 확정, 같은 이메일 자동 연동 안 함.
  - API·배치: /oauth2/** · /auth/oauth/signup · social_accounts
  - 근거: FR-AUTH-03
  - 비고: 11장 #2: Google만(P1)으로 변경
- [x] **AUTH-B05 비밀번호 재설정** · `필수` · 비밀번호
  - reset-request(가입 여부와 관계없이 같은 응답, 30분·1회용 토큰, 소셜(Google) 전용 계정에는 'Google 로그인을 이용해 주세요' 안내 메일), reset(규칙 검증, Refresh 전부 폐기).
  - API·배치: /auth/password/reset-request · /auth/password/reset
  - 근거: FR-AUTH-04
- [ ] **AUTH-B06 비회원 기록 이관** · `제외` · 비회원
  - POST /explorations/merge: guestKey의 세션·반응·추천을 회원으로 1회 이관(같은 영상 반응은 회원 기록 유지), 쿠키 삭제.
  - API·배치: /explorations/merge
  - 근거: FR-AUTH-05
  - 비고: MVP 제외(P2): 비회원 체험(11장 #1)
- [x] **AUTH-B07 회원 탈퇴** · `필수` · 탈퇴
  - DELETE /users/me: 비밀번호 확인, 이메일·닉네임 즉시 익명화 [P1], 글·댓글은 '탈퇴한 사용자'로 유지, 토큰 폐기, 같은 이메일 재가입 허용.
  - API·배치: /users/me
  - 근거: FR-AUTH-06
  - 비고: 탈퇴 처리는 필수(P0), 개인정보 익명화는 권장(P1). 탈퇴 시 개인정보 처리 방식 확인 필요

## 프로필·마이페이지

필수 1 · 권장 3 · 후순위 0 · 제외 0 · 프론트엔드 짝: USER-F01~F04

- [x] **USER-B01 온보딩 저장** · `권장` · 온보딩
  - PUT /users/me/onboarding: 관심 취미(최대 5, SAVED로도 저장)·숙련도·이용 상황(시간 0~40, 예산 1만 원 단위)·선호 지역, 미입력은 null.
  - API·배치: /users/me/onboarding · user_conditions · user_hobbies
  - 근거: FR-USER-01
  - 비고: FR-USER-01 P1
- [x] **USER-B02 내 정보·수정** · `권장` · 프로필
  - GET /users/me, PUT /users/me/conditions(관심 취미·이용 상황·선호 지역). PATCH /users/me(닉네임 중복·프로필 이미지)는 [P2·MVP 제외]. 저장해도 기존 추천은 그대로.
  - API·배치: /users/me · /users/me/conditions
  - 근거: FR-USER-02 · FR-PLACE-01
  - 비고: 닉네임·프로필 이미지 수정은 제외(P2)
- [x] **USER-B03 비밀번호 변경** · `권장` · 비밀번호
  - 현재 비밀번호 확인, 같은 비밀번호 거부, 다른 기기 Refresh 폐기, 소셜 계정 거부.
  - API·배치: /users/me/password
  - 근거: FR-USER-03
- [ ] **USER-B04 마이페이지 요약·내 글** · `필수` · 마이페이지
  - GET /users/me/summary(관심 취미·진행 중·보관한 숏폼·내 글 수, 보관한 숏폼은 보관 기능(P2) 도입 전 0 또는 숨김), GET /users/me/posts.
  - API·배치: /users/me/summary · /users/me/posts
  - 근거: FR-USER-04
  - 비고: 보관한 숏폼 카드(P0) ↔ 보관함(P2) 확인 필요

## 숏폼 탐색

필수 9 · 권장 2 · 후순위 0 · 제외 2 · 프론트엔드 짝: SHORT-F01~F09

- [x] **SHORT-B01 탐색 세션 API** · `필수` · 탐색 세션
  - POST /explorations(세션 + 첫 영상), progress(진행률 n / N·반응한 취미·관심 주제 상위 3개 %), finish(종료 + 추천 생성 호출), 세션 상태 IN_PROGRESS·COMPLETED·ABANDONED.
  - API·배치: /explorations · /explorations/{id}/progress · /explorations/{id}/finish
  - 근거: FR-SHORT-03 · FR-SHORT-04
- [x] **SHORT-B02 다음 영상 선택** · `필수` · 다음 영상
  - P0 단계는 ACTIVE 영상 중 본 영상·관련 없음·숨김(비ACTIVE) 제외 후 무작위, 별로예요 취미 세션 제외, 후보 부족 시 반응 없던 영상 재사용, 세션당 노출 수 N(설정값 exploration.session-size, 기본안 12) 또는 후보 없음이면 finished. [P1] 첫 진입 분류 분산(1~4번 서로 다른 분류)과 선호 70%·탐색 30%(prefer-ratio 설정값, 매번 난수)는 개선 기간에 붙인다.
  - API·배치: /explorations/{id}/next
  - 근거: FR-SHORT-03
  - 비고: 11장 #3 · #4 · 첫 진입 분류 분산·선호 70%/탐색 30%는 권장(P1)
- [x] **SHORT-B03 노출·시청·재생 실패 기록** · `필수` · 시청 기록
  - POST views: watchedSeconds(길이 상한)·watch_ratio 저장, 고유 시청자 view_user_count 증가, playFailed 기록 → 서로 다른 3명 이상 실패 시 UNAVAILABLE.
  - API·배치: /explorations/{id}/views · exploration_views · shorts
  - 근거: FR-SHORT-01 · FR-SHORT-02
- [x] **SHORT-B04 반응 API** · `필수` · 반응
  - PUT·DELETE /shorts/{id}/reaction: 사용자 × 영상 1개(비회원 체험 도입 시 guestKey 포함 [P2]), 변경·취소, IRRELEVANT면 irrelevant_count ±1(고유 회원 기준).
  - API·배치: /shorts/{id}/reaction · short_reactions
  - 근거: FR-SHORT-02 · FR-SHORT-08
- [ ] **SHORT-B05 관심사 직접 선택·초기화** · `제외` · 직접 선택
  - GET /categories/with-hobbies, PUT /users/me/interest-selections(최대 10), POST /users/me/interest-reset(interest_reset_at 갱신, 보관함·관련 없음 기록은 유지).
  - API·배치: interest_selections · users
  - 근거: FR-SHORT-05
  - 비고: MVP 제외(P2): 관심사 직접 선택·탐색 기록 초기화(FR-SHORT-05)
- [ ] **SHORT-B06 보관 API** · `제외` · 보관함
  - PUT·DELETE /shorts/{id}/save(회원, 반응과 별개·점수 미반영), GET /users/me/saved-shorts(최근 보관순·취미 필터·이용 불가 표시).
  - API·배치: /shorts/{id}/save · /users/me/saved-shorts · short_saves
  - 근거: FR-SHORT-06
  - 비고: 11장 #31 · MVP 제외(P2): 숏폼 보관함(FR-SHORT-06)
- [x] **SHORT-B07 숏폼 영상 수집(매일 04:00)** · `필수` · 수집 배치
  - ACTIVE 채널마다 playlistItems.list(last_collected_at 이후), videos.list 50개 묶음, 기계적 필터(180초 이하·embeddable·public·KR 재생 가능) 불합격은 REJECTED(사유), 실패 채널은 다음 날 이어서, 할당량 사용량 기록.
  - API·배치: YouTube Data API · youtube_channels · shorts · batch_runs
  - 근거: FR-SHORT-07 · NFR-EXT-01
- [x] **SHORT-B08 영상 취미 지정** · `필수` · 수집 배치
  - 단일 채널은 채널 취미 상속, 종합 채널은 숏폼 키워드 매핑으로, 그래도 못 정하면 GMS(제목·설명·썸네일 → hobbyId·confidence·isHobbyContent). 확실한 영상은 CANDIDATE, 신뢰도 0.7 미만·실패는 NEEDS_REVIEW로 저장한다(이 구분을 P0에서 미리 만들어 두어 관리자 검수(P3)를 언제든 붙일 수 있게 함). NEEDS_REVIEW는 관리자 승인 전까지 노출하지 않음.
  - API·배치: shorts.assign_method · assign_confidence
  - 근거: FR-SHORT-07
  - 비고: 11장 #25
- [x] **SHORT-B09 주간 반영(목 06:00)** · `필수` · 수집 배치
  - 그 주 CANDIDATE만 ACTIVE로(released_at). 관리자가 없어도 확실한 영상은 이 배치로 사용자에게 나간다. NEEDS_REVIEW는 반영하지 않고, 관리자 승인([P3], ADM-B10)으로 CANDIDATE가 된 영상만 다음 반영에 포함. 관리자 '지금 반영' 수동 실행(409 ADMIN_RELEASE_IN_PROGRESS)은 [P2·MVP 제외].
  - API·배치: shorts · batch_runs
  - 근거: FR-SHORT-07
  - 비고: 11장 #23 · 지금 반영 수동 실행 API는 제외(P2)
- [ ] **SHORT-B10 기존 영상 점검(매일 04:30)** · `권장` · 수집 배치
  - ACTIVE 영상을 videos.list로 50개씩 다시 조회해 삭제·비공개·퍼가기 금지·지역 제한이 생기면 UNAVAILABLE.
  - API·배치: shorts.last_checked_at
  - 근거: FR-SHORT-07
  - 비고: 삭제 영상 점검은 FR-SHORT-07 중 P1
- [ ] **SHORT-B11 신뢰 채널 발굴(매주 월 06:00)** · `필수` · 수집 배치
  - 숏폼 키워드로 search.list(type=video·videoDuration=short·regionCode=KR), 여러 번 나온 채널을 youtube_channels에 CANDIDATE로 저장까지 한다(승인은 관리자 화면(ADM-B09, P3)에서, 그 전에는 DB 직접). 하루 100회 한도 안.
  - API·배치: youtube_channels(discovered_by=SEARCH)
  - 근거: FR-SHORT-07 · NFR-EXT-01
  - 비고: CSV P0. 팀 결정(10/21): 발굴은 P0 유지, 후보 승인 화면은 후순위(P3), 그 전에는 DB 직접
- [ ] **SHORT-B12 관련 없음 자동 숨김** · `필수` · 걸러 내기
  - 관련 없음 5명 이상 그리고 비율 20% 이상이면 즉시 AUTO_HIDDEN, 채널 자동 숨김 3건 이상이면 '점검 필요' [P1]. 관리자 복구(카운트 초기화)·영구 제외는 [P3](그 전에는 DB 직접).
  - API·배치: shorts · youtube_channels.hidden_short_count
  - 근거: FR-SHORT-08
  - 비고: 11장 #26 · 채널 점검 필요 표시는 권장(P1), 복구·영구 제외는 후순위(P3, ADM-B10)
- [ ] **SHORT-B13 YouTube 할당량 경고** · `권장` · 운영
  - 하루 사용량이 80%를 넘으면 Mattermost 운영 채널에 경고.
  - API·배치: batch_runs.quota_used
  - 근거: NFR-EXT-01

## 취미 검색·상세

필수 2 · 권장 0 · 후순위 0 · 제외 0 · 프론트엔드 짝: HOBBY-F01~F03

- [x] **HOBBY-B01 취미 검색 API** · `필수` · 검색
  - GET /hobbies: 취미명·태그 부분 일치(2자 이상), 필터 5종 AND, 정렬(추천순: 로그인은 관심사 점수·비회원은 인기순 / 이름순 / 비용 낮은순), 20개. GET /categories, /tags.
  - API·배치: /hobbies · /categories · /tags
  - 근거: FR-HOBBY-01
- [x] **HOBBY-B02 취미 상세 API** · `필수` · 상세
  - GET /hobbies/{id}(PUBLISHED만, 로그인 시 myStatus), /shorts(ACTIVE 최대 6), /plan-preview(1~2주차), 관련 정보 피드 3개.
  - API·배치: /hobbies/{id} · /hobbies/{id}/shorts · /hobbies/{id}/plan-preview
  - 근거: FR-HOBBY-02
  - 비고: 관련 숏폼·TODO 미리보기·관련 정보 피드는 권장(P1)

## 추천

필수 5 · 권장 2 · 후순위 0 · 제외 0 · 프론트엔드 짝: RECO-F01~F03

- [x] **RECO-B01 관심사 점수 계산** · `필수` · 점수 계산
  - 영상 점수 = 반응(LIKE +3, DISLIKE −3) + 시청 가중치(시청 비율 × 2 − 1), 취미 점수 = 영상 점수 합(직접 선택 +5는 [P2]), 태그 점수 = 영상 점수 × 0.3. 관련 없음·재생 실패 제외(초기화 이후 반응만 쓰는 규칙은 [P2]), 가중치는 application.yml.
  - API·배치: short_reactions · exploration_views
  - 근거: FR-RECO-01
  - 비고: 11장 #27
- [x] **RECO-B02 추천 생성** · `필수` · 추천 생성
  - 관심 취미 상위 3개, 연관 취미 3개(유사도 × 0.7 + 태그 점수 × 0.3, 관심·진행 중·별로예요 제외), 이용 상황은 정렬 가중치(×0.8 + 조건 확인 필요 / ×1.1), 정보 부족(LIKE 3개 미만) 시 탐색용 6개, 스냅샷 저장, 3초 이내.
  - API·배치: /recommendations · recommendations · recommendation_items
  - 근거: FR-RECO-01 · NFR-PERF-02
  - 비고: 11장 #5 · #7 · 이용 상황 반영 정렬·정보 부족 시 탐색용 제안은 권장(P1)
- [x] **RECO-B03 추천 조회 API** · `필수` · 추천 조회
  - GET /recommendations/latest, /{id}(추천 이유 생성 상태 reason_status, 추천 기준 문구 basisText 포함).
  - API·배치: /recommendations/latest · /recommendations/{id}
  - 근거: FR-RECO-02
  - 비고: 추천 기준 문구(basisText)·추천 이유 상태는 권장(P1)
- [x] **RECO-B04 추천 이유 생성(reco.reason)** · `권장` · 추천 이유
  - 추천 생성 직후 큐 발행 → GMS로 60자 이내 1문장, 실패하면 FALLBACK 템플릿('클라이밍 영상에 좋아요 3회').
  - API·배치: reco.reason · recommendation_items.reason
  - 근거: FR-RECO-02
  - 비고: AI 추천 이유(60자)는 P1
- [x] **RECO-B05 별로예요 API** · `필수` · 추천 피드백
  - PUT·DELETE /hobbies/{id}/dislike(hobby_dislikes), 이후 추천·연관 후보에서 제외.
  - API·배치: /hobbies/{id}/dislike · hobby_dislikes
  - 근거: FR-RECO-03
  - 비고: 11장 #6
- [ ] **RECO-B06 취미 유사도 배치(Spring)** · `필수` · 유사도
  - 태그 벡터 코사인 유사도를 hobby_similarity에 저장. 매일 03:00(관리자 수동 실행은 [P2]), Spring @Scheduled.
  - API·배치: hobby_similarity
  - 근거: FR-RECO-01 · 9.4
  - 비고: 11장 #8
- [x] **RECO-B07 추천 가중치 튜닝** · `권장` · 분석
  - 사용자 테스트 후 event_logs·반응 데이터로 가중치를 분석·조정한다(pandas·scikit-learn).
  - API·배치: event_logs
  - 근거: 9.2 · NFR-LOG-02

## 내 취미

필수 4 · 권장 0 · 후순위 0 · 제외 0 · 프론트엔드 짝: MY-F01~F04

> 2026-10-30: 화면 기준 동시 진행 3개, 중지·재개·그만두기와 계획 편집 포함. [정식 계약](../spec/api-changes-2026-10-30-todo.md). 아래 완료는 코드·단위/MVC 테스트 기준이며 PostgreSQL 검증 결과는 변경 기록에 별도 기재한다.

- [x] **MY-B01 내 취미 목록 API** · `필수` · 목록
  - GET /users/me/hobbies: 실제 planId·전체/주차 진행·주차 제목·총 주차·원본 숙련도 목록. 기존 배열 응답 유지.
  - GET /users/me/todos/summary: 상태별 개수·maxInProgressCount=3·현재 주차 TODO 남은 수·진행 중 카드, 밀림 별도.
  - 근거: FR-MY-01~03 · 00-2/06-1
- [x] **MY-B02 관심 저장·해제** · `필수` · 관심 저장
  - SAVED만 생성/삭제. IN_PROGRESS·PAUSED 및 종료 상태 보호. 사용자 행 잠금·실제 생성/삭제 시 save_count 증감.
  - 근거: FR-MY-01
- [x] **MY-B03 시작하기** · `필수` · 시작하기
  - 공개 원본의 버전·제목·주차 제목·활동 복사. 숙련도 자동 대체 없음, 미준비 409 MY_PLAN_NOT_READY.
  - IN_PROGRESS 최대 3개(중지 제외), 사용자 행 잠금 후 검사. 중복 시작 거부. 종료 후 새 계획 생성은 제외.
  - V10 주차 제목·정지 초·활성 계획 유일 인덱스, V11 클라이밍/드로잉/사진 촬영 BEGINNER 3주 원본 시드.
  - 근거: FR-MY-02
- [x] **MY-B04 일시중지·재개·그만두기** · `필수` · 상태 변경
  - IN_PROGRESS ↔ PAUSED, IN_PROGRESS/PAUSED → STOPPED. 반복 요청 멱등. 재개 제한 3개 재검사, 동일 계획/시작일/이력 유지.
  - UTC 정지 초 누적·서울 날짜 기준 주차 동결/재개. 자동 COMPLETED 전환·축하 제외.
  - 근거: FR-MY-03

## 주차별 TODO / 전체 계획

필수 3 · 권장 0 · 후순위 0 · 제외 1 · 프론트엔드 짝: TODO-F01~F04

- [x] **TODO-B01 계획 조회 API** · `필수` · 조회
  - GET /plans/{id}?week=, /plans/{id}/overview. currentWeek/selectedWeek 분리, 모든 주차 요약·사용자 사본 미리보기.
  - 전체/주차 진행·예상 시간 합계·밀림/건너뜀 상세·준비물 문자열 배열·권한 제공.
  - 진행률 DONE/삭제 제외 전체, 빈 주차 완료=false. 서버 준비물 체크 저장 없음.
  - 근거: FR-TODO-01 · 06-2/06-3A/B
- [x] **TODO-B02 활동 상태·메모** · `필수` · 상태 변경
  - 상태 PATCH·메모 단독 500자. 생략=유지, memo null=삭제. 완료 재시도 시 completedAt 유지, 취소/건너뜀 시 제거.
  - ACTIVE+IN_PROGRESS만 수정, 미래 주차 상태/메모 거부. 이동 동시 전송도 우회 불가.
  - 주차 완료=DONE+SKIPPED, 전체 처리 완료는 신호만 반환. mutation 응답 집계 갱신.
  - 근거: FR-TODO-02
- [x] **TODO-B03 내 계획 수정** · `필수` · 계획 수정
  - POST·soft DELETE items, PATCH weekNo·expectedMinutes. 원본·사용자 추가·완료 활동 모두 적용.
  - 사용자 사본만 변경. 도착 주차 끝 삽입·출발 주차 순서 정리, 삭제 즉시 집계 제외.
  - 소유자·계획/item 조합·상태·주차·필드 길이·시간 범위 검증. 미래 주차 편집 허용.
  - 근거: FR-TODO-03 · 06-3 진입
- [ ] **TODO-B04 주차 시작 TODO 알림(시작일 기준 7일마다)** · `제외` · 알림
  - 계획 시작 즉시 1주차 알림, 이후 매일 09:00에 진행 중 계획의 현재 주차를 계산해 바뀐 주차 알림 생성(dedup WEEKLY_TODO:{planId}:{주차}).
  - API·배치: notifications
  - 근거: FR-NOTI-01
  - 비고: MVP 제외(P2): 알림 전체

## 장소

필수 0 · 권장 5 · 후순위 0 · 제외 0 · 프론트엔드 짝: PLACE-F01~F03

- [ ] **PLACE-B01 지역 API** · `권장` · 지역
  - GET /regions?sido=(시·도 / 시·군·구), 행정구역 코드 시드.
  - API·배치: /regions · regions
  - 근거: FR-PLACE-01
  - 비고: FR-PLACE-01 P1
- [x] **PLACE-B02 장소 목록 API** · `권장` · 장소 목록
  - GET /places?regionId=&hobbyId=: PUBLISHED, 좌표, '정보 확인 필요'(운영 시간·비용 비었거나 90일 넘게 갱신 안 됨) 플래그.
  - API·배치: /places · places · hobby_places
  - 근거: FR-PLACE-02
- [ ] **PLACE-B03 공공데이터 장소 수집(매월 1일 03:30)** · `권장` · 수집 배치
  - 전국도시공원정보표준데이터·공공체육시설 데이터를 서울만 받아 공공데이터 종목명 매핑으로 취미 연결, 카카오 주소 → 좌표 변환(실패 시 목록만), 바로 PUBLISHED.
  - API·배치: places · place_sources · batch_runs
  - 근거: FR-PLACE-03 · NFR-EXT-02
- [ ] **PLACE-B04 카카오 로컬 장소 수집(매주 화 03:30)** · `권장` · 수집 배치
  - 서울 25개 구 × 취미별 카카오 검색어 × 3페이지 키워드 검색, 카테고리로 무관한 결과 제외, 규칙을 통과한 새 장소는 바로 PUBLISHED(PENDING_REVIEW 검토 단계는 관리자 검토(P2) 도입 시).
  - API·배치: places · place_sources
  - 근거: FR-PLACE-03 · NFR-EXT-02
  - 비고: 팀 공용 앱 키 1개
- [ ] **PLACE-B05 장소 중복 병합** · `권장` · 병합
  - 좌표 50m 이내 그리고 이름 유사도 0.6 이상(공백·괄호·'점' 제거)이면 병합, 대표 정보는 공공 > 카카오, 출처 원본은 place_sources, (source, source_ref) 멱등.
  - API·배치: places · place_sources
  - 근거: FR-PLACE-03
  - 비고: 11장 #28

## 정보 피드

필수 0 · 권장 4 · 후순위 0 · 제외 0 · 프론트엔드 짝: FEED-F01

- [ ] **FEED-B01 정보 피드 API** · `권장` · 피드
  - GET /feeds: 회원은 관심·진행 중 취미 기준(없으면 인기 취미), 유형·취미 필터, 종료 행사 제외 옵션, 최신순, PUBLISHED만.
  - API·배치: /feeds · feed_items
  - 근거: FR-FEED-01
  - 비고: FR-FEED-01 P1
- [ ] **FEED-B02 행사·공연·전시 수집(매일 05:00)** · `권장` · 수집 배치
  - TourAPI(축제·행사), 문화포털·KOPIS(공연·전시)를 서울만 받아 (origin, source_ref)로 건너뛰기·변경분 갱신, 행사 검색어 매핑으로 취미 연결.
  - API·배치: feed_items · batch_runs
  - 근거: FR-FEED-02 · NFR-EXT-02
  - 비고: 11장 #29
- [ ] **FEED-B03 공공 강좌 수집(매일 05:30)** · `권장` · 수집 배치
  - 서울 열린데이터광장 교육 공공서비스예약 정보를 받아 강좌·클래스로 저장한다.
  - API·배치: feed_items(origin=SEOUL_RESERVATION)
  - 근거: FR-FEED-02
  - 비고: FR-FEED-02 P1
- [ ] **FEED-B04 피드 요약·취미 지정(feed.summarize)** · `권장` · 요약
  - GMS로 요약 2줄과 취미를 붙이고(어디에도 안 붙으면 저장 안 함) 바로 PUBLISHED로 둔다(PENDING_REVIEW 검토 단계는 관리자 검토(P2) 도입 시). 제목 유사도 0.8 + 같은 시작일이면 중복 제거. 실패하면 재시도 후 요약 없이 둔다.
  - API·배치: feed.summarize · feed_items
  - 근거: FR-FEED-02 · 9.3
  - 비고: FR-FEED-02 P1. 검토 없이 바로 공개는 확인 필요

## 알림

필수 0 · 권장 0 · 후순위 0 · 제외 3 · 프론트엔드 짝: NOTI-F01~F02

- [ ] **NOTI-B01 알림 조회·읽음 API** · `제외` · 알림 API
  - GET /notifications?unreadOnly=(20개씩), /unread-count, PATCH /{id}/read, /read-all.
  - API·배치: /notifications · notifications
  - 근거: FR-NOTI-01
  - 비고: MVP 제외(P2): 알림 전체
- [ ] **NOTI-B02 알림 생성 컨슈머** · `제외` · 알림 생성
  - notification.create: 댓글·대댓글·문의 답변·신고 처리·공지 알림 저장, dedup_key UNIQUE로 중복 방지.
  - API·배치: notification.create · notifications
  - 근거: FR-NOTI-01 · 9.3
  - 비고: 11장 #16 · MVP 제외(P2): 알림 전체
- [ ] **NOTI-B03 새 피드 알림(매일 09:00)** · `제외` · 알림 생성
  - 관심 취미의 새 정보 피드를 하루 1회 묶음 알림으로 만든다.
  - API·배치: notifications
  - 근거: FR-NOTI-01
  - 비고: MVP 제외(P2): 알림 전체

## 커뮤니티

필수 0 · 권장 5 · 후순위 0 · 제외 0 · 프론트엔드 짝: BOARD-F01~F06

- [x] **BOARD-B01 게시판 목록 API** · `권장` · 목록
  - GET /hobbies/{id}/posts: 유형·키워드(검색 인덱스는 PostgreSQL 방식으로 V2에서 추가, 예: pg_trgm)·정렬(최신·좋아요)·20개, HIDDEN·DELETED 제외.
  - API·배치: /hobbies/{id}/posts · posts
  - 근거: FR-BOARD-01
  - 비고: FR-BOARD P1
- [x] **BOARD-B02 게시글 작성·수정·삭제** · `권장` · 작성
  - POST·PATCH·DELETE /posts: 입력 검증(제목 2~50, 본문 10~5,000, 이미지 5장), HTML 이스케이프 저장, 본인만(403 BOARD_FORBIDDEN), soft delete 후 상세 404.
  - API·배치: /posts · /posts/{id} · post_images
  - 근거: FR-BOARD-02 · NFR-SEC-05
- [x] **BOARD-B03 게시글 상세·조회수** · `권장` · 상세
  - GET /posts/{id}: 조회수는 회원 기준(비회원 체험 도입 시 guestKey 포함) 24시간에 1회.
  - API·배치: /posts/{id}
  - 근거: FR-BOARD-03
- [x] **BOARD-B04 댓글·대댓글 API** · `권장` · 댓글
  - GET·POST /posts/{id}/comments, PATCH·DELETE /comments/{id}: 1~500자, 대댓글 1단계(답글의 답글도 부모 아래), 대댓글 있으면 '삭제된 댓글입니다'. 댓글 알림 발행은 [P2·MVP 제외].
  - API·배치: comments
  - 근거: FR-BOARD-03
- [x] **BOARD-B05 좋아요 API** · `권장` · 좋아요
  - PUT·DELETE /posts/{id}/like, /comments/{id}/like: UNIQUE(user, targetType, targetId), like_count.
  - API·배치: likes
  - 근거: FR-BOARD-04

## 신고

필수 0 · 권장 0 · 후순위 1 · 제외 0 · 프론트엔드 짝: REPORT-F01

- [ ] **REPORT-B01 신고 접수 API** · `후순위` · 신고
  - POST /reports: 대상(게시글·댓글·사용자)·사유 5종·기타 상세 필수, 중복 409 REPORT_DUPLICATED, 본인 콘텐츠 400 REPORT_SELF, 접수만으로 숨기지 않음.
  - API·배치: /reports · reports
  - 근거: FR-REPORT-01
  - 비고: FR-REPORT-01 P3

## 문의·공지

필수 0 · 권장 0 · 후순위 2 · 제외 0 · 프론트엔드 짝: CS-F01~F03

- [ ] **CS-B01 고객 문의 API** · `후순위` · 문의
  - GET·POST /inquiries, GET /inquiries/{id}(본인·관리자만, 그 외 403), 답변 전 PATCH·DELETE(답변 후 409 INQUIRY_ALREADY_ANSWERED).
  - API·배치: /inquiries · inquiries
  - 근거: FR-CS-01
  - 비고: FR-CS-01 P3
- [ ] **CS-B02 공지 API** · `후순위` · 공지
  - GET /notices(상단 고정 → 게시일 최신순), /notices/{id}, 비회원 조회 가능.
  - API·배치: /notices · notices
  - 근거: FR-CS-02
  - 비고: FR-CS-02 P3

## 관리자

필수 0 · 권장 0 · 후순위 5 · 제외 5 · 프론트엔드 짝: ADM-F01~F08

- [ ] **ADM-B01 취미 관리 API** · `제외` · 취미 콘텐츠
  - GET·POST /admin/hobbies, GET·PUT /{id}, PATCH /{id}/status(필수 항목 검사 → 422 ADMIN_NOT_PUBLISHABLE + fields), 분류·태그 관리, 처리자·시각 기록.
  - API·배치: /admin/hobbies · /admin/categories · /admin/tags
  - 근거: FR-ADM-01
  - 비고: MVP 제외(P2): 취미 콘텐츠·분류·태그는 시드 SQL(SYS-B15)
- [ ] **ADM-B02 태그 추천(GMS)** · `제외` · 취미 콘텐츠
  - POST /admin/hobbies/{id}/tag-suggestions: 소개 문구로 태그 후보 생성(admin-ai 요청량 제한).
  - API·배치: /admin/hobbies/{id}/tag-suggestions
  - 근거: FR-ADM-01
  - 비고: MVP 제외(P2): AI 태그 추천
- [ ] **ADM-B03 TODO 원본 API** · `제외` · TODO 원본
  - GET·POST /admin/plan-templates, PUT /{id}(활동 전체 저장, 필수 항목, 버전 +1), PATCH /{id}/status. 공개 원본이 있어야 시작하기 가능.
  - API·배치: /admin/plan-templates · plan_templates · plan_template_items
  - 근거: FR-ADM-02
  - 비고: MVP 제외(P2): TODO 원본은 시드 SQL(SYS-B15)
- [ ] **ADM-B04 신고 처리 API** · `후순위` · 신고
  - GET /admin/reports(대상별 묶음·고유 신고자 수), 대상 원문 + 신고 목록 + 과거 조치, POST actions(HIDE·KEEP·UNHIDE + note) → 대상 상태·신고 일괄 처리(신고자 알림은 [P2·MVP 제외]).
  - API·배치: /admin/reports/** · reports
  - 근거: FR-ADM-03
  - 비고: FR-ADM-03 P3
- [ ] **ADM-B05 문의 답변 API** · `후순위` · 문의
  - GET /admin/inquiries?status=, PUT /{id}/answer → ANSWERED(작성자 알림은 [P2·MVP 제외]).
  - API·배치: /admin/inquiries
  - 근거: FR-ADM-04
  - 비고: FR-ADM-04 P3
- [ ] **ADM-B06 공지 관리 API** · `후순위` · 공지
  - /admin/notices CRUD, 임시 저장·게시·상단 고정, 게시 시 전체 알림 발행 옵션(CSV P3이나 알림이 P2라 확인 필요).
  - API·배치: /admin/notices
  - 근거: FR-ADM-05
  - 비고: FR-ADM-05 P3. 전체 알림 발송 ↔ 알림(P2) 확인 필요
- [ ] **ADM-B07 피드·장소 관리 API** · `제외` · 피드·장소
  - /admin/feeds·/admin/places 등록·수정·비공개, PENDING_REVIEW 검토 후 공개.
  - API·배치: /admin/feeds · /admin/places
  - 근거: FR-ADM-06
  - 비고: MVP 제외(P2): 피드·장소는 검토 없이 바로 PUBLISHED
- [ ] **ADM-B08 데이터 매핑 API** · `제외` · 데이터 매핑
  - GET·PUT /admin/hobbies/{id}/source-mappings(카카오 검색어·공공데이터 종목명·행사 검색어·숏폼 키워드). 바꾼 매핑은 다음 수집부터.
  - API·배치: hobby_source_mappings
  - 근거: FR-ADM-06
  - 비고: MVP 제외(P2): 데이터 매핑표는 시드 SQL(SYS-B15)
- [ ] **ADM-B09 신뢰 채널 관리 API** · `후순위` · 숏폼
  - GET·POST /admin/channels(URL·ID → channels.list로 이름·업로드 재생목록 확인), PATCH /{id}(유형·취미·상태, 후보 승인·거절, 수집 중지).
  - API·배치: /admin/channels · youtube_channels
  - 근거: FR-ADM-07
  - 비고: 팀 결정(10/21): 후순위(P3). 그 전에는 신뢰 채널은 시드 SQL, 발굴 후보 승인은 DB 직접
- [ ] **ADM-B10 숏폼 검수 API** · `후순위` · 숏폼
  - GET /admin/shorts?status=&hobbyId=, PATCH /{id}(취미 지정(변경)·승인(NEEDS_REVIEW → CANDIDATE, 다음 주간 반영 때 ACTIVE)·제외(REJECTED)·복구). [P2·MVP 제외] GET·POST /admin/shorts/release(지금 반영), GET /admin/batch-runs?job=.
  - API·배치: /admin/shorts · /admin/shorts/release · /admin/batch-runs
  - 근거: FR-ADM-07 · FR-SHORT-08
  - 비고: 팀 결정(10/21): 검수(목록·승인·취미 변경·제외·복구)는 후순위(P3), 지금 반영·배치 이력은 제외(P2). 그 전에는 NEEDS_REVIEW 영상은 노출하지 않고, 자동 숨김 복구·영구 제외는 DB 직접

## 서비스 피드백

필수 0 · 권장 0 · 후순위 0 · 제외 4 · 프론트엔드 짝: FB-F01~F02

- [ ] **FB-B01 피드백 제출 API** · `제외` · 제출
  - POST /feedbacks: 인증 → 요청량 제한(10분 3회) → 검증 → RECEIVED 저장 → 202, feedback.analyze 발행.
  - API·배치: /feedbacks · feedbacks
  - 근거: FR-FB-01
  - 비고: MVP 제외(P2): CSV(10/21)에서 빠짐, 고도화 후보
- [ ] **FB-B02 피드백 AI 분석** · `제외` · AI 분석
  - feedback.analyze 소비: GMS로 우선순위(URGENT·HIGH·NORMAL·LOW)·유형·요약 → ANALYZED, 실패 시 재시도 후 ANALYSIS_FAILED, 상태 확인으로 멱등.
  - API·배치: feedback.analyze
  - 근거: FR-FB-02
  - 비고: MVP 제외(P2): CSV(10/21)에서 빠짐, 고도화 후보
- [ ] **FB-B03 Mattermost 알림** · `제외` · 알림
  - URGENT는 즉시 Incoming Webhook(개인정보 제외), 나머지는 매일 18:00 요약 1건(미분석 원문 포함), notified_at으로 중복 방지.
  - API·배치: Mattermost Webhook
  - 근거: FR-FB-02
  - 비고: MVP 제외(P2): CSV(10/21)에서 빠짐, 고도화 후보 · 11장 #22
- [ ] **FB-B04 관리자 피드백 API** · `제외` · 관리자
  - GET /admin/feedbacks?priority=&status=, POST /{id}/reanalyze.
  - API·배치: /admin/feedbacks
  - 근거: FR-FB-02
  - 비고: MVP 제외(P2): CSV(10/21)에서 빠짐, 고도화 후보
