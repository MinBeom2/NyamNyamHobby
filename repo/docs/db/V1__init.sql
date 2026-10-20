-- 냠냠취미 DB 스키마 v0.3 (MySQL 8.0, InnoDB, utf8mb4)
-- 규칙: 테이블·컬럼 snake_case / PK BIGINT AUTO_INCREMENT / 시간 DATETIME(6) UTC 저장
--       enum은 VARCHAR + JPA @Enumerated(EnumType.STRING) / 삭제는 status 또는 deleted_at (soft delete)

-- ───────── 회원 ─────────
CREATE TABLE users (
  id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  email             VARCHAR(100) NOT NULL,
  password_hash     VARCHAR(100) NULL COMMENT '소셜 전용 계정은 NULL',
  nickname          VARCHAR(12)  NOT NULL,
  profile_image_url VARCHAR(500) NULL,
  role              VARCHAR(10)  NOT NULL DEFAULT 'USER'    COMMENT 'USER|ADMIN',
  status            VARCHAR(10)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|ACTIVE|WITHDRAWN',
  email_verified_at DATETIME(6)  NULL,
  interest_reset_at DATETIME(6)  NULL COMMENT '관심사 초기화 시각. 이후 반응만 점수에 반영',
  login_fail_count  INT          NOT NULL DEFAULT 0,
  locked_until      DATETIME(6)  NULL,
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  withdrawn_at      DATETIME(6)  NULL,
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_nickname (nickname)
);

CREATE TABLE social_accounts (
  id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id          BIGINT       NOT NULL,
  provider         VARCHAR(10)  NOT NULL COMMENT 'GOOGLE|KAKAO|NAVER',
  provider_user_id VARCHAR(100) NOT NULL,
  created_at       DATETIME(6)  NOT NULL,
  UNIQUE KEY uk_social (provider, provider_user_id),
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE auth_tokens (
  id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT      NOT NULL,
  type       VARCHAR(20) NOT NULL COMMENT 'EMAIL_VERIFY(24h)|PASSWORD_RESET(30m)|REFRESH(14d)',
  token_hash CHAR(64)    NOT NULL COMMENT 'SHA-256. 원문은 저장하지 않음',
  expires_at DATETIME(6) NOT NULL,
  used_at    DATETIME(6) NULL COMMENT '사용·폐기 시각',
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_token_hash (token_hash),
  KEY idx_token_user (user_id, type),
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE regions (
  id      BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  code    VARCHAR(10) NOT NULL COMMENT '행정구역 코드(시·군·구)',
  sido    VARCHAR(20) NOT NULL,
  sigungu VARCHAR(30) NOT NULL,
  UNIQUE KEY uk_region_code (code),
  KEY idx_region_sido (sido)
);

CREATE TABLE user_conditions (
  user_id        BIGINT      NOT NULL PRIMARY KEY,
  weekly_hours   TINYINT     NULL COMMENT '활동 가능 시간(시간/주) 0~40. NULL=미입력',
  monthly_budget INT         NULL COMMENT '예산(원/월). NULL=미입력',
  place_pref     VARCHAR(10) NULL COMMENT 'INDOOR|OUTDOOR|ANY',
  social_pref    VARCHAR(10) NULL COMMENT 'ALONE|TOGETHER|ANY',
  region_id      BIGINT      NULL COMMENT '선호 지역',
  updated_at     DATETIME(6) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (region_id) REFERENCES regions (id)
);

-- ───────── 취미 콘텐츠 ─────────
CREATE TABLE categories (
  id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(30) NOT NULL,
  sort_order INT         NOT NULL DEFAULT 0,
  UNIQUE KEY uk_category_name (name)
);

CREATE TABLE hobbies (
  id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  category_id         BIGINT       NOT NULL,
  name                VARCHAR(30)  NOT NULL,
  summary             VARCHAR(100) NOT NULL,
  description         TEXT         NULL,
  place_type          VARCHAR(10)  NOT NULL COMMENT 'INDOOR|OUTDOOR|BOTH',
  social_type         VARCHAR(10)  NOT NULL COMMENT 'ALONE|TOGETHER|BOTH',
  difficulty          VARCHAR(10)  NOT NULL COMMENT 'EASY|NORMAL|HARD',
  session_min_minutes SMALLINT     NOT NULL COMMENT '1회 소요 시간(분) 최소',
  session_max_minutes SMALLINT     NOT NULL,
  initial_cost_min    INT          NOT NULL COMMENT '초기 비용(원)',
  initial_cost_max    INT          NOT NULL,
  monthly_cost_min    INT          NOT NULL COMMENT '월 지속 비용(원)',
  monthly_cost_max    INT          NOT NULL,
  supplies            TEXT         NULL COMMENT '준비물',
  environment         TEXT         NULL COMMENT '활동 환경 설명',
  first_activity      TEXT         NULL COMMENT '초보자용 첫 활동',
  cautions            TEXT         NULL COMMENT '유의 사항',
  thumbnail_url       VARCHAR(500) NULL,
  status              VARCHAR(10)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|PUBLISHED|HIDDEN',
  save_count          INT          NOT NULL DEFAULT 0 COMMENT '관심 저장 수(인기순)',
  created_at          DATETIME(6)  NOT NULL,
  updated_at          DATETIME(6)  NOT NULL,
  UNIQUE KEY uk_hobby_name (name),
  KEY idx_hobby_category (category_id, status),
  FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE tags (
  id   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(20) NOT NULL COMMENT '예: 실외, 활동적, 비용낮음',
  UNIQUE KEY uk_tag_name (name)
);

CREATE TABLE hobby_tags (
  hobby_id BIGINT NOT NULL,
  tag_id   BIGINT NOT NULL,
  PRIMARY KEY (hobby_id, tag_id),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
  FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE TABLE hobby_similarity (
  hobby_id         BIGINT       NOT NULL,
  related_hobby_id BIGINT       NOT NULL,
  score            DECIMAL(5,4) NOT NULL COMMENT '0~1 코사인 유사도. Python 배치가 기록',
  computed_at      DATETIME(6)  NOT NULL,
  PRIMARY KEY (hobby_id, related_hobby_id),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
  FOREIGN KEY (related_hobby_id) REFERENCES hobbies (id)
);

CREATE TABLE hobby_source_mappings (
  id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  hobby_id   BIGINT      NOT NULL,
  target     VARCHAR(20) NOT NULL COMMENT 'KAKAO_KEYWORD|PUBLIC_SPORTS|PUBLIC_PARK|EVENT_KEYWORD|SHORT_KEYWORD',
  keyword    VARCHAR(50) NOT NULL COMMENT '예: 클라이밍장, 인공암벽장, 볼더링',
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_mapping (hobby_id, target, keyword),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

-- ───────── 숏폼 탐색 ─────────
CREATE TABLE youtube_channels (
  id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  youtube_channel_id  VARCHAR(30)  NOT NULL COMMENT 'UC로 시작하는 채널 ID',
  title               VARCHAR(100) NOT NULL,
  uploads_playlist_id VARCHAR(40)  NULL COMMENT 'channels.list contentDetails.relatedPlaylists.uploads',
  type                VARCHAR(10)  NOT NULL COMMENT 'SINGLE(한 취미)|MULTI(종합)',
  hobby_id            BIGINT       NULL COMMENT 'SINGLE이면 영상에 상속할 취미',
  status              VARCHAR(10)  NOT NULL DEFAULT 'CANDIDATE' COMMENT 'CANDIDATE|ACTIVE|PAUSED|REJECTED',
  discovered_by       VARCHAR(10)  NOT NULL DEFAULT 'ADMIN' COMMENT 'ADMIN|SEARCH(채널 발굴)',
  hidden_short_count  INT          NOT NULL DEFAULT 0 COMMENT '자동 숨김된 영상 수. 3 이상이면 점검 필요 표시',
  last_collected_at   DATETIME(6)  NULL COMMENT '마지막 수집 성공 시각(이후 업로드만 가져옴)',
  created_at          DATETIME(6)  NOT NULL,
  updated_at          DATETIME(6)  NOT NULL,
  UNIQUE KEY uk_channel (youtube_channel_id),
  KEY idx_channel_status (status),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

CREATE TABLE shorts (
  id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  hobby_id          BIGINT       NULL COMMENT '취미 지정 전·검토 대기면 NULL',
  channel_id        BIGINT       NULL COMMENT '수집한 신뢰 채널(시드 데이터는 NULL)',
  youtube_video_id  VARCHAR(20)  NOT NULL,
  title             VARCHAR(200) NOT NULL,
  description       TEXT         NULL,
  thumbnail_url     VARCHAR(500) NULL,
  duration_seconds  SMALLINT     NOT NULL COMMENT '180 이하만 통과',
  published_at      DATETIME(6)  NULL COMMENT 'YouTube 업로드 시각',
  status            VARCHAR(12)  NOT NULL DEFAULT 'CANDIDATE' COMMENT 'CANDIDATE|NEEDS_REVIEW|ACTIVE|UNAVAILABLE|AUTO_HIDDEN|REJECTED',
  assign_method     VARCHAR(10)  NULL COMMENT 'CHANNEL|KEYWORD|GPT|ADMIN',
  assign_confidence DECIMAL(3,2) NULL COMMENT 'GPT 신뢰도 0~1',
  reject_reason     VARCHAR(20)  NULL COMMENT 'TOO_LONG|NOT_EMBEDDABLE|REGION_BLOCKED|NOT_PUBLIC|NOT_HOBBY|ADMIN',
  view_user_count   INT          NOT NULL DEFAULT 0 COMMENT '고유 시청자 수(관련 없음 비율의 분모)',
  irrelevant_count  INT          NOT NULL DEFAULT 0 COMMENT '관련 없음 고유 사용자 수',
  fail_user_count   INT          NOT NULL DEFAULT 0 COMMENT '재생 실패를 보고한 고유 사용자 수. 3 이상이면 UNAVAILABLE',
  released_at       DATETIME(6)  NULL COMMENT '주간 반영으로 ACTIVE가 된 시각',
  last_checked_at   DATETIME(6)  NULL COMMENT '기존 영상 점검 시각',
  created_at        DATETIME(6)  NOT NULL COMMENT '수집 시각',
  updated_at        DATETIME(6)  NOT NULL,
  UNIQUE KEY uk_short_video (youtube_video_id),
  KEY idx_short_hobby (hobby_id, status),
  KEY idx_short_status (status, created_at),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
  FOREIGN KEY (channel_id) REFERENCES youtube_channels (id)
);

CREATE TABLE exploration_sessions (
  id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT      NULL,
  guest_key    CHAR(36)    NULL COMMENT '비회원 UUID. 이관 후 NULL',
  status       VARCHAR(12) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS|COMPLETED|ABANDONED',
  max_count    TINYINT     NOT NULL DEFAULT 12,
  viewed_count TINYINT     NOT NULL DEFAULT 0,
  started_at   DATETIME(6) NOT NULL,
  ended_at     DATETIME(6) NULL,
  KEY idx_session_user (user_id, started_at),
  KEY idx_session_guest (guest_key),
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE exploration_views (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  session_id  BIGINT      NOT NULL,
  short_id    BIGINT      NOT NULL,
  seq         TINYINT     NOT NULL COMMENT '세션 내 노출 순서 1~12',
  play_failed BOOLEAN     NOT NULL DEFAULT FALSE,
  watched_seconds SMALLINT NOT NULL DEFAULT 0 COMMENT '실제 재생 초(영상 길이 상한)',
  watch_ratio DECIMAL(4,3) NULL COMMENT 'watched_seconds ÷ duration_seconds (0~1)',
  viewed_at   DATETIME(6) NOT NULL,
  UNIQUE KEY uk_view (session_id, short_id),
  FOREIGN KEY (session_id) REFERENCES exploration_sessions (id),
  FOREIGN KEY (short_id) REFERENCES shorts (id)
);

CREATE TABLE short_reactions (
  id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT      NULL,
  guest_key  CHAR(36)    NULL,
  short_id   BIGINT      NOT NULL,
  session_id BIGINT      NULL COMMENT '반응한 탐색 세션(보관함에서 반응 시 NULL)',
  type       VARCHAR(10) NOT NULL COMMENT 'LIKE|DISLIKE|IRRELEVANT',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_reaction_user (user_id, short_id),
  UNIQUE KEY uk_reaction_guest (guest_key, short_id),
  KEY idx_reaction_type (user_id, type, updated_at),
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (short_id) REFERENCES shorts (id)
);

CREATE TABLE short_saves (
  user_id    BIGINT      NOT NULL,
  short_id   BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL COMMENT '보관 시각(보관함 정렬 기준)',
  PRIMARY KEY (user_id, short_id),
  KEY idx_save_user (user_id, created_at),
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (short_id) REFERENCES shorts (id)
);

CREATE TABLE interest_selections (
  user_id    BIGINT      NOT NULL,
  hobby_id   BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id, hobby_id),
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

-- ───────── 추천 ─────────
CREATE TABLE recommendations (
  id                 BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id            BIGINT      NULL,
  guest_key          CHAR(36)    NULL,
  session_id         BIGINT      NULL,
  basis              VARCHAR(12) NOT NULL COMMENT 'REACTION|SELECTION|MIXED',
  is_exploratory     BOOLEAN     NOT NULL DEFAULT FALSE,
  reaction_count     TINYINT     NOT NULL DEFAULT 0,
  condition_snapshot JSON        NULL COMMENT '추천 당시 이용 상황',
  created_at         DATETIME(6) NOT NULL,
  KEY idx_reco_user (user_id, created_at),
  KEY idx_reco_guest (guest_key),
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (session_id) REFERENCES exploration_sessions (id)
);

CREATE TABLE recommendation_items (
  id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  recommendation_id BIGINT       NOT NULL,
  hobby_id          BIGINT       NOT NULL,
  type              VARCHAR(10)  NOT NULL COMMENT 'INTEREST|RELATED',
  rank_no           TINYINT      NOT NULL,
  score             DECIMAL(7,3) NOT NULL,
  reason            VARCHAR(200) NULL,
  reason_status     VARCHAR(10)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|DONE|FALLBACK',
  condition_warning BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '예산·시간 초과',
  UNIQUE KEY uk_reco_item (recommendation_id, hobby_id),
  FOREIGN KEY (recommendation_id) REFERENCES recommendations (id),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

CREATE TABLE hobby_dislikes (
  user_id    BIGINT      NOT NULL,
  hobby_id   BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id, hobby_id),
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

-- ───────── 내 취미·TODO ─────────
CREATE TABLE user_hobbies (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT      NOT NULL,
  hobby_id    BIGINT      NOT NULL,
  status      VARCHAR(12) NOT NULL COMMENT 'SAVED|IN_PROGRESS|PAUSED|COMPLETED|STOPPED',
  skill_level VARCHAR(12) NOT NULL DEFAULT 'BEGINNER' COMMENT 'BEGINNER|NOVICE|INTERMEDIATE',
  saved_at    DATETIME(6) NOT NULL,
  started_at  DATETIME(6) NULL,
  paused_at   DATETIME(6) NULL,
  paused_days INT         NOT NULL DEFAULT 0 COMMENT '누적 일시중지 일수(주차 계산용)',
  ended_at    DATETIME(6) NULL,
  UNIQUE KEY uk_user_hobby (user_id, hobby_id),
  KEY idx_user_hobby_status (user_id, status),
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

CREATE TABLE plan_templates (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  hobby_id    BIGINT      NOT NULL,
  skill_level VARCHAR(12) NOT NULL,
  title       VARCHAR(50) NOT NULL COMMENT '예: 클라이밍 · 입문 8주 계획',
  total_weeks TINYINT     NOT NULL,
  version     INT         NOT NULL DEFAULT 1 COMMENT '저장할 때마다 +1',
  status      VARCHAR(10) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|PUBLISHED|HIDDEN',
  updated_by  BIGINT      NULL,
  created_at  DATETIME(6) NOT NULL,
  updated_at  DATETIME(6) NOT NULL,
  UNIQUE KEY uk_template (hobby_id, skill_level),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

CREATE TABLE plan_template_items (
  id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  template_id      BIGINT       NOT NULL,
  week_no          TINYINT      NOT NULL,
  seq              TINYINT      NOT NULL,
  title            VARCHAR(50)  NOT NULL,
  description      VARCHAR(500) NULL,
  expected_minutes SMALLINT     NOT NULL,
  supplies         VARCHAR(200) NULL,
  done_criteria    VARCHAR(200) NOT NULL COMMENT '완료 기준',
  KEY idx_template_item (template_id, week_no, seq),
  FOREIGN KEY (template_id) REFERENCES plan_templates (id)
);

CREATE TABLE user_plans (
  id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_hobby_id    BIGINT      NOT NULL,
  template_id      BIGINT      NOT NULL,
  template_version INT         NOT NULL COMMENT '복사 당시 원본 버전',
  title            VARCHAR(50) NOT NULL,
  total_weeks      TINYINT     NOT NULL,
  status           VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|ARCHIVED(다시 시작 전 계획)',
  started_at       DATETIME(6) NOT NULL,
  KEY idx_plan_user_hobby (user_hobby_id, status),
  FOREIGN KEY (user_hobby_id) REFERENCES user_hobbies (id),
  FOREIGN KEY (template_id) REFERENCES plan_templates (id)
);

CREATE TABLE user_plan_items (
  id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_plan_id     BIGINT       NOT NULL,
  template_item_id BIGINT       NULL COMMENT '직접 추가한 활동은 NULL',
  week_no          TINYINT      NOT NULL,
  seq              TINYINT      NOT NULL,
  title            VARCHAR(50)  NOT NULL,
  description      VARCHAR(500) NULL,
  expected_minutes SMALLINT     NOT NULL,
  supplies         VARCHAR(200) NULL,
  done_criteria    VARCHAR(200) NULL,
  status           VARCHAR(10)  NOT NULL DEFAULT 'TODO' COMMENT 'TODO|DONE|SKIPPED',
  memo             VARCHAR(500) NULL,
  is_custom        BOOLEAN      NOT NULL DEFAULT FALSE,
  completed_at     DATETIME(6)  NULL,
  deleted_at       DATETIME(6)  NULL,
  updated_at       DATETIME(6)  NOT NULL,
  KEY idx_plan_item (user_plan_id, week_no, seq),
  FOREIGN KEY (user_plan_id) REFERENCES user_plans (id)
);

-- ───────── 장소·정보 피드 ─────────
CREATE TABLE places (
  id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  region_id         BIGINT        NOT NULL,
  name              VARCHAR(100)  NOT NULL,
  category          VARCHAR(20)   NOT NULL COMMENT 'PARK|SPORTS|STUDIO|CLASS|GYM|ETC',
  address           VARCHAR(200)  NOT NULL,
  lat               DECIMAL(10,7) NULL,
  lng               DECIMAL(10,7) NULL,
  opening_hours     VARCHAR(200)  NULL,
  fee_text          VARCHAR(100)  NULL,
  external_url      VARCHAR(500)  NULL,
  source            VARCHAR(50)   NOT NULL COMMENT '대표 출처 PUBLIC_PARK|PUBLIC_SPORTS|KAKAO_LOCAL|ADMIN',
  source_ref        VARCHAR(100)  NULL COMMENT '출처 쪽 ID(중복 수집 방지)',
  source_updated_at DATETIME(6)   NULL,
  status            VARCHAR(16)   NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW|PUBLISHED|HIDDEN',
  created_at        DATETIME(6)   NOT NULL,
  updated_at        DATETIME(6)   NOT NULL,
  UNIQUE KEY uk_place_source (source, source_ref),
  KEY idx_place_region (region_id, status),
  FOREIGN KEY (region_id) REFERENCES regions (id)
);

CREATE TABLE hobby_places (
  hobby_id BIGINT NOT NULL,
  place_id BIGINT NOT NULL,
  PRIMARY KEY (hobby_id, place_id),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
  FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE TABLE place_sources (
  id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  place_id    BIGINT        NOT NULL COMMENT '병합된 대표 장소',
  source      VARCHAR(20)   NOT NULL COMMENT 'PUBLIC_PARK|PUBLIC_SPORTS|KAKAO_LOCAL|ADMIN',
  source_ref  VARCHAR(100)  NOT NULL COMMENT '출처 쪽 ID',
  raw_name    VARCHAR(100)  NOT NULL,
  raw_address VARCHAR(200)  NULL,
  lat         DECIMAL(10,7) NULL,
  lng         DECIMAL(10,7) NULL,
  fetched_at  DATETIME(6)   NOT NULL,
  UNIQUE KEY uk_place_src (source, source_ref),
  KEY idx_place_src_place (place_id),
  FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE TABLE feed_items (
  id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  hobby_id      BIGINT       NOT NULL,
  type          VARCHAR(12)  NOT NULL COMMENT 'EVENT(축제·행사)|SHOW(공연·전시)|CLASS(강좌·클래스)|GUIDE(입문 자료)',
  title         VARCHAR(150) NOT NULL,
  summary       VARCHAR(300) NULL COMMENT 'GMS 요약',
  source_name   VARCHAR(50)  NOT NULL,
  source_url    VARCHAR(500) NOT NULL,
  source_ref    VARCHAR(100) NULL,
  thumbnail_url VARCHAR(500) NULL,
  region_id     BIGINT       NULL,
  published_at  DATETIME(6)  NOT NULL COMMENT '출처 게시일',
  event_start   DATE         NULL,
  event_end     DATE         NULL COMMENT '지나면 종료 배지',
  origin        VARCHAR(20)  NOT NULL COMMENT 'ADMIN|TOUR_API|CULTURE_PORTAL|KOPIS|SEOUL_RESERVATION',
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW|PUBLISHED|HIDDEN',
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL COMMENT '갱신 시점',
  UNIQUE KEY uk_feed_source (origin, source_ref),
  KEY idx_feed_hobby (hobby_id, status, published_at),
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id)
);

-- ───────── 커뮤니티·신고 ─────────
CREATE TABLE posts (
  id            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  hobby_id      BIGINT      NOT NULL,
  user_id       BIGINT      NOT NULL,
  type          VARCHAR(10) NOT NULL COMMENT 'QUESTION|INFO|REVIEW|SHARE',
  title         VARCHAR(50) NOT NULL,
  content       TEXT        NOT NULL,
  view_count    INT         NOT NULL DEFAULT 0,
  like_count    INT         NOT NULL DEFAULT 0,
  comment_count INT         NOT NULL DEFAULT 0,
  status        VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|HIDDEN|DELETED',
  created_at    DATETIME(6) NOT NULL,
  updated_at    DATETIME(6) NOT NULL,
  KEY idx_post_board (hobby_id, status, created_at),
  KEY idx_post_user (user_id, created_at),
  FULLTEXT KEY ft_post (title, content) WITH PARSER ngram,
  FOREIGN KEY (hobby_id) REFERENCES hobbies (id),
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE post_images (
  id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT       NOT NULL,
  url     VARCHAR(500) NOT NULL,
  seq     TINYINT      NOT NULL,
  FOREIGN KEY (post_id) REFERENCES posts (id)
);

CREATE TABLE comments (
  id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  post_id    BIGINT       NOT NULL,
  user_id    BIGINT       NOT NULL,
  parent_id  BIGINT       NULL COMMENT '대댓글이면 부모 댓글 ID(1단계만)',
  content    VARCHAR(500) NOT NULL,
  like_count INT          NOT NULL DEFAULT 0,
  status     VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|HIDDEN|DELETED',
  created_at DATETIME(6)  NOT NULL,
  updated_at DATETIME(6)  NOT NULL,
  KEY idx_comment_post (post_id, parent_id, created_at),
  FOREIGN KEY (post_id) REFERENCES posts (id),
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (parent_id) REFERENCES comments (id)
);

CREATE TABLE likes (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT      NOT NULL,
  target_type VARCHAR(10) NOT NULL COMMENT 'POST|COMMENT',
  target_id   BIGINT      NOT NULL,
  created_at  DATETIME(6) NOT NULL,
  UNIQUE KEY uk_like (user_id, target_type, target_id),
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE reports (
  id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  reporter_id  BIGINT       NOT NULL,
  target_type  VARCHAR(10)  NOT NULL COMMENT 'POST|COMMENT|USER',
  target_id    BIGINT       NOT NULL,
  reason       VARCHAR(10)  NOT NULL COMMENT 'SPAM|ABUSE|ADULT|PRIVACY|ETC',
  detail       VARCHAR(300) NULL,
  status       VARCHAR(10)  NOT NULL DEFAULT 'RECEIVED' COMMENT 'RECEIVED|ACTIONED|DISMISSED',
  action       VARCHAR(10)  NULL COMMENT 'HIDE|KEEP|UNHIDE',
  admin_id     BIGINT       NULL,
  admin_note   VARCHAR(300) NULL,
  processed_at DATETIME(6)  NULL,
  created_at   DATETIME(6)  NOT NULL,
  UNIQUE KEY uk_report (reporter_id, target_type, target_id),
  KEY idx_report_target (target_type, target_id),
  KEY idx_report_status (status, created_at),
  FOREIGN KEY (reporter_id) REFERENCES users (id)
);

-- ───────── 알림·문의·공지·로그 ─────────
CREATE TABLE notifications (
  id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT       NOT NULL,
  type       VARCHAR(20)  NOT NULL COMMENT 'COMMENT|REPLY|INQUIRY_ANSWERED|REPORT_DONE|NOTICE|WEEKLY_TODO|NEW_FEED',
  title      VARCHAR(100) NOT NULL,
  body       VARCHAR(200) NULL,
  link_path  VARCHAR(200) NOT NULL COMMENT '프론트 이동 경로. 예: /posts/12#c-34',
  dedup_key  VARCHAR(100) NOT NULL,
  read_at    DATETIME(6)  NULL,
  created_at DATETIME(6)  NOT NULL,
  UNIQUE KEY uk_noti_dedup (user_id, dedup_key),
  KEY idx_noti_user (user_id, read_at, created_at),
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE inquiries (
  id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT        NOT NULL,
  category    VARCHAR(12)   NOT NULL COMMENT 'ACCOUNT|RECOMMEND|TODO|COMMUNITY|ETC',
  title       VARCHAR(50)   NOT NULL,
  content     VARCHAR(2000) NOT NULL,
  image_url   VARCHAR(500)  NULL,
  status      VARCHAR(10)   NOT NULL DEFAULT 'RECEIVED' COMMENT 'RECEIVED|ANSWERED',
  answer      TEXT          NULL,
  answered_by BIGINT        NULL,
  answered_at DATETIME(6)   NULL,
  created_at  DATETIME(6)   NOT NULL,
  updated_at  DATETIME(6)   NOT NULL,
  KEY idx_inquiry_user (user_id, created_at),
  KEY idx_inquiry_status (status, created_at),
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE notices (
  id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  admin_id     BIGINT       NOT NULL,
  category     VARCHAR(12)  NOT NULL COMMENT 'GUIDE|MAINTENANCE|UPDATE',
  title        VARCHAR(100) NOT NULL,
  content      TEXT         NOT NULL,
  pinned       BOOLEAN      NOT NULL DEFAULT FALSE,
  status       VARCHAR(10)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|PUBLISHED',
  published_at DATETIME(6)  NULL,
  created_at   DATETIME(6)  NOT NULL,
  updated_at   DATETIME(6)  NOT NULL,
  KEY idx_notice_list (status, pinned, published_at),
  FOREIGN KEY (admin_id) REFERENCES users (id)
);

CREATE TABLE feedbacks (
  id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT        NOT NULL,
  category     VARCHAR(12)   NULL COMMENT '사용자가 고른 유형(선택) BUG|RECOMMEND|CONTENT|SUGGESTION|ETC',
  content      VARCHAR(1000) NOT NULL,
  page_path    VARCHAR(200)  NULL COMMENT '제출한 화면 경로',
  client_info  JSON          NULL COMMENT '브라우저·OS·추천 ID 등',
  status       VARCHAR(16)   NOT NULL DEFAULT 'RECEIVED' COMMENT 'RECEIVED|ANALYZING|ANALYZED|ANALYSIS_FAILED',
  priority     VARCHAR(10)   NULL COMMENT 'URGENT|HIGH|NORMAL|LOW (AI 분류)',
  ai_category  VARCHAR(12)   NULL,
  ai_summary   VARCHAR(300)  NULL,
  retry_count  TINYINT       NOT NULL DEFAULT 0,
  notified_at  DATETIME(6)   NULL COMMENT 'Mattermost 전송 시각(즉시 또는 정기 요약)',
  analyzed_at  DATETIME(6)   NULL,
  created_at   DATETIME(6)   NOT NULL,
  KEY idx_feedback_status (status, created_at),
  KEY idx_feedback_priority (priority, created_at),
  FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE batch_runs (
  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  job         VARCHAR(30)  NOT NULL COMMENT 'SHORT_COLLECT|SHORT_CHECK|SHORT_RELEASE|CHANNEL_DISCOVER|PLACE_PUBLIC|PLACE_KAKAO|FEED_COLLECT|CLASS_COLLECT',
  status      VARCHAR(10)  NOT NULL COMMENT 'RUNNING|SUCCESS|PARTIAL|FAILED',
  processed   INT          NOT NULL DEFAULT 0 COMMENT '조회한 건수',
  accepted    INT          NOT NULL DEFAULT 0 COMMENT '저장·반영한 건수',
  rejected    INT          NOT NULL DEFAULT 0 COMMENT '필터로 제외한 건수',
  quota_used  INT          NOT NULL DEFAULT 0 COMMENT '외부 API 사용량(YouTube는 할당량 단위)',
  message     VARCHAR(500) NULL COMMENT '실패 채널·오류 요약',
  started_at  DATETIME(6)  NOT NULL,
  finished_at DATETIME(6)  NULL,
  KEY idx_batch (job, started_at)
);

CREATE TABLE event_logs (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT      NULL,
  guest_key   CHAR(36)    NULL,
  event_type  VARCHAR(30) NOT NULL COMMENT 'RECO_IMPRESSION|RECO_CLICK|HOBBY_SAVE|HOBBY_START|HOBBY_DISLIKE',
  target_type VARCHAR(20) NULL,
  target_id   BIGINT      NULL,
  meta        JSON        NULL,
  created_at  DATETIME(6) NOT NULL,
  KEY idx_event (event_type, created_at)
);
