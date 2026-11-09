-- 신고 Mattermost 정기 알림: 보낸 신고는 notified_at을 채워 다음 시간에 다시 보내지 않는다
-- (V20이 팀원 마이그레이션과 겹쳐 V21로 옮김. 로컬에서 예전 V20으로 이미 적용된 DB도 깨지지 않게 IF NOT EXISTS)
ALTER TABLE reports ADD COLUMN IF NOT EXISTS notified_at TIMESTAMP(6) NULL;
COMMENT ON COLUMN reports.notified_at IS 'Mattermost 신고 알림 전송 시각(NULL이면 아직 안 보냄)';
CREATE INDEX IF NOT EXISTS idx_report_unnotified ON reports (id) WHERE notified_at IS NULL;
