#!/usr/bin/env bash
# YouTube 할당량 경고: 오늘(미국 태평양 시간 기준) 사용량이 80%를 넘으면 Mattermost로 하루 한 번 알린다.
set -euo pipefail

# 비밀값은 파일에서 필요한 줄만 읽는다(스크립트에 직접 쓰지 않음)
WEBHOOK=$(grep -E '^MATTERMOST_WEBHOOK_URL=' /home/ubuntu/ops/.env | cut -d= -f2-)
DB_USER=$(grep -E '^POSTGRES_USER=' /home/ubuntu/infra/.env | cut -d= -f2-)
DB_NAME=$(grep -E '^POSTGRES_DB=' /home/ubuntu/infra/.env | cut -d= -f2-)

DAILY_LIMIT=${DAILY_LIMIT:-10000}          # YouTube 기본 하루 한도
THRESHOLD_PERCENT=${THRESHOLD_PERCENT:-80} # 경고 기준(%)
STATE_DIR=/home/ubuntu/ops/state

# batch_runs.started_at은 UTC. 태평양 시간 오늘 0시를 UTC로 바꿔 그 이후 사용량을 더한다
USED=$(docker exec postgres psql -U "$DB_USER" -d "$DB_NAME" -tA -c "
SELECT COALESCE(SUM(quota_used), 0) FROM batch_runs
WHERE job IN ('SHORT_COLLECT', 'SHORT_CHECK', 'CHANNEL_DISCOVER')
  AND started_at >= (date_trunc('day', now() AT TIME ZONE 'America/Los_Angeles')
                     AT TIME ZONE 'America/Los_Angeles') AT TIME ZONE 'UTC';")

PERCENT=$(( USED * 100 / DAILY_LIMIT ))
TODAY=$(TZ=America/Los_Angeles date +%Y%m%d)
echo "$(date '+%F %T') used=${USED} (${PERCENT}%)"

# 기준을 넘었고 오늘 아직 안 보냈으면 한 번만 보낸다
if [ "$PERCENT" -ge "$THRESHOLD_PERCENT" ] && [ ! -f "$STATE_DIR/quota-alert-$TODAY" ]; then
  TEXT="⚠️ YouTube 할당량 경고: 오늘 사용량 ${USED} / ${DAILY_LIMIT} (${PERCENT}%). 할당량은 한국 시간 오후 4~5시에 초기화돼요."
  curl -sS -X POST -H 'Content-Type: application/json' -d "{\"text\": \"${TEXT}\"}" "$WEBHOOK" > /dev/null
  touch "$STATE_DIR/quota-alert-$TODAY"
  echo "alert sent"
fi

# 일주일 지난 기록 파일 정리
find "$STATE_DIR" -name 'quota-alert-*' -mtime +7 -delete
