-- V3 시드에 들어 있던 개발용 가짜 숏폼 16개 정리
-- 수집 배치(FR-SHORT-07)가 생기기 전 탐색 API 개발용으로 넣은 데이터로, YouTube ID가 실제 영상이 아니라 재생되지 않는다.
-- 시청·반응 기록이 이 영상을 참조할 수 있어 삭제하지 않고 REJECTED(사유 ADMIN)로 바꿔 노출에서만 뺀다.
-- V3는 이미 적용된 마이그레이션이라 수정하지 않는다(Flyway 체크섬).

UPDATE shorts
SET status = 'REJECTED',
    reject_reason = 'ADMIN',
    updated_at = NOW()
WHERE channel_id IS NULL
  AND youtube_video_id IN (
    'aBcD123xYz0', 'bCdE234yZa1', 'cDeF345zAb2', 'dEfG456aBc3',
    'eFgH567bCd4', 'fGhI678cDe5', 'gHiJ789dEf6', 'hIjK890eFg7',
    'iJkL901fGh8', 'jKlM012gHi9', 'kLmN123hIj0', 'lMnO234iJk1',
    'mNoP345jKl2', 'NoPq456kLm3', 'OpQr567lMn4', 'PqRs678mNo5'
  );
