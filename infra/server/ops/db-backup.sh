#!/usr/bin/env bash
# DB·업로드 이미지 매일 백업. ~/backups/에 날짜별로 남기고 14일 지난 것은 지운다. 실패하면 Mattermost로 알린다.
set -euo pipefail

BACKUP_DIR=/home/ubuntu/backups
KEEP_DAYS=${KEEP_DAYS:-14}
STAMP=$(date +%Y%m%d_%H%M)
DB_USER=$(grep -E '^POSTGRES_USER=' /home/ubuntu/infra/.env | cut -d= -f2-)
DB_NAME=$(grep -E '^POSTGRES_DB=' /home/ubuntu/infra/.env | cut -d= -f2-)
WEBHOOK=$(grep -E '^MATTERMOST_WEBHOOK_URL=' /home/ubuntu/ops/.env | cut -d= -f2-)

# 중간에 어떤 명령이든 실패하면 Mattermost로 알린다
notify_failure() {
  curl -sS -X POST -H 'Content-Type: application/json' \
    -d "{\"text\": \"❌ DB 백업 실패 (${STAMP}). 서버에서 ~/ops/logs/backup.log를 확인하세요.\"}" "$WEBHOOK" > /dev/null || true
}
trap notify_failure ERR

mkdir -p "$BACKUP_DIR/db" "$BACKUP_DIR/uploads"
chmod 700 "$BACKUP_DIR"

# 1) DB: 압축 형식(-Fc)으로 덤프. 임시 이름(.part)으로 쓰고, 읽히는지 확인한 뒤 이름을 바꾼다
DB_FILE="$BACKUP_DIR/db/${DB_NAME}_${STAMP}.dump"
docker exec postgres pg_dump -U "$DB_USER" -d "$DB_NAME" -Fc > "$DB_FILE.part"
docker exec -i postgres pg_restore --list < "$DB_FILE.part" > /dev/null
mv "$DB_FILE.part" "$DB_FILE"

# 2) 업로드 이미지 볼륨을 tar.gz로 (읽기 전용으로 연결)
UPLOAD_FILE="$BACKUP_DIR/uploads/uploads_${STAMP}.tar.gz"
docker run --rm -v nyam-app_uploads:/data:ro -v "$BACKUP_DIR/uploads":/backup alpine \
  tar czf "/backup/uploads_${STAMP}.tar.gz" -C /data .

# 3) 보관 기간이 지난 백업과 실패로 남은 임시 파일 정리
find "$BACKUP_DIR/db" -name '*.dump' -mtime +"$KEEP_DAYS" -delete
find "$BACKUP_DIR/uploads" -name '*.tar.gz' -mtime +"$KEEP_DAYS" -delete
find "$BACKUP_DIR/db" -name '*.part' -mmin +60 -delete

echo "$(date '+%F %T') ok db=$(du -h "$DB_FILE" | cut -f1) uploads=$(du -h "$UPLOAD_FILE" | cut -f1)"
