-- Additive migration: retain existing plans, items and template versions.
ALTER TABLE user_hobbies ADD COLUMN paused_seconds BIGINT NOT NULL DEFAULT 0;
UPDATE user_hobbies SET paused_seconds = paused_days::BIGINT * 86400;
ALTER TABLE user_hobbies ADD CONSTRAINT ck_user_hobby_paused_seconds CHECK (paused_seconds >= 0);

CREATE TABLE plan_template_weeks (
  template_id BIGINT NOT NULL REFERENCES plan_templates(id),
  week_no SMALLINT NOT NULL CHECK (week_no > 0),
  title VARCHAR(50) NOT NULL,
  PRIMARY KEY (template_id, week_no)
);
CREATE TABLE user_plan_weeks (
  user_plan_id BIGINT NOT NULL REFERENCES user_plans(id),
  week_no SMALLINT NOT NULL CHECK (week_no > 0),
  title VARCHAR(50) NOT NULL,
  PRIMARY KEY (user_plan_id, week_no)
);

-- Existing templates have no authored week title. Use an explicit neutral label,
-- never an activity title. Existing user copies get their own frozen labels.
INSERT INTO plan_template_weeks (template_id, week_no, title)
SELECT p.id, w::SMALLINT, w || '주차'
FROM plan_templates p CROSS JOIN LATERAL generate_series(1, p.total_weeks) w;
INSERT INTO user_plan_weeks (user_plan_id, week_no, title)
SELECT p.id, w::SMALLINT, w || '주차'
FROM user_plans p CROSS JOIN LATERAL generate_series(1, p.total_weeks) w;

-- Fail rather than silently discard inconsistent existing active-plan data.
CREATE UNIQUE INDEX uk_user_plan_active ON user_plans(user_hobby_id) WHERE status = 'ACTIVE';
COMMENT ON COLUMN user_hobbies.status IS 'SAVED|IN_PROGRESS|PAUSED|COMPLETED|STOPPED (자동 완료는 미지원)';
COMMENT ON COLUMN user_hobbies.paused_days IS 'paused_seconds의 일 단위 호환 값; 주차 계산은 paused_seconds 사용';
COMMENT ON COLUMN user_hobbies.paused_seconds IS '누적 일시중지 초; UTC 시각으로 계산';
COMMENT ON TABLE plan_template_weeks IS '원본 계획의 주차 제목';
COMMENT ON TABLE user_plan_weeks IS '시작 시 복사하여 고정한 사용자 계획 주차 제목';
