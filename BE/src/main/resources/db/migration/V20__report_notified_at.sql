-- 신고 Mattermost 정기 알림: 보낸 신고는 notified_at을 채워 다음 시간에 다시 보내지 않는다
ALTER TABLE reports ADD COLUMN notified_at TIMESTAMP(6) NULL;
COMMENT ON COLUMN reports.notified_at IS 'Mattermost 신고 알림 전송 시각(NULL이면 아직 안 보냄)';
CREATE INDEX idx_report_unnotified ON reports (id) WHERE notified_at IS NULL;
