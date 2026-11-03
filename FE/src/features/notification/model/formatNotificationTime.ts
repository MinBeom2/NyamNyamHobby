const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

// 알림 시각 표시: "방금 전" · "n분 전" · "n시간 전" · "n일 전", 7일이 넘으면 날짜(화면표 09-1)
export function formatNotificationTime(iso: string, now = Date.now()) {
  const time = new Date(iso).getTime();
  if (Number.isNaN(time)) return '';
  const diff = Math.max(0, now - time);
  if (diff < MINUTE) return '방금 전';
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)}분 전`;
  if (diff < DAY) return `${Math.floor(diff / HOUR)}시간 전`;
  if (diff <= 7 * DAY) return `${Math.floor(diff / DAY)}일 전`;
  const date = new Date(time);
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
}
