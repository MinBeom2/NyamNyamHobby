-- GMS 판별 기준 완화(뉴스·광고·무관 영상만 제외)에 맞춰, 이전 기준으로 NOT_HOBBY 제외됐던 영상을 지운다.
-- 지우면 다음 수집 때 같은 영상이 검색되면 새 기준으로 다시 판별된다.
-- REJECTED 영상은 노출된 적이 없어 시청·반응 기록이 없으므로 그대로 삭제해도 된다.
-- 재생 불가(TOO_LONG·NOT_EMBEDDABLE 등)로 제외된 영상은 기준이 바뀌지 않았으므로 남긴다.

DELETE FROM shorts
WHERE status = 'REJECTED'
  AND reject_reason = 'NOT_HOBBY';
