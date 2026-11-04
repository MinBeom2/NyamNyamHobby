/** 정보 피드 날짜·지역 필터의 화면 모델. 서버에는 from/to(yyyy-MM-dd), regionIds로 보낸다. */

export type DatePreset = 'THIS_WEEK' | 'THIS_WEEKEND' | 'THIS_MONTH' | 'NEXT_MONTH' | 'NEXT_30_DAYS';

export type DateRange = {
  /** yyyy-MM-dd. 비어 있으면 그쪽은 제한 없음 */
  from?: string;
  to?: string;
  preset?: DatePreset;
};

export const datePresetLabels: Record<DatePreset, string> = {
  THIS_WEEK: '이번 주',
  THIS_WEEKEND: '이번 주말',
  THIS_MONTH: '이번 달',
  NEXT_MONTH: '다음 달',
  NEXT_30_DAYS: '앞으로 30일',
};

export function toIsoDate(date: Date) {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

/** 오늘을 기준으로 미리 정한 기간을 계산한다. 주는 월~일, 주말은 이번 주 토~일(오늘이 일요일이면 오늘만) */
export function presetRange(preset: DatePreset, today = new Date()): DateRange {
  const base = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const weekday = base.getDay(); // 0=일
  const sunday = addDays(base, weekday === 0 ? 0 : 7 - weekday);
  switch (preset) {
    case 'THIS_WEEK':
      return { from: toIsoDate(base), to: toIsoDate(sunday), preset };
    case 'THIS_WEEKEND':
      return { from: toIsoDate(weekday === 0 ? base : addDays(sunday, -1)), to: toIsoDate(sunday), preset };
    case 'THIS_MONTH':
      return { from: toIsoDate(base), to: toIsoDate(new Date(base.getFullYear(), base.getMonth() + 1, 0)), preset };
    case 'NEXT_MONTH':
      return {
        from: toIsoDate(new Date(base.getFullYear(), base.getMonth() + 1, 1)),
        to: toIsoDate(new Date(base.getFullYear(), base.getMonth() + 2, 0)),
        preset,
      };
    case 'NEXT_30_DAYS':
      return { from: toIsoDate(base), to: toIsoDate(addDays(base, 29)), preset };
  }
}

function shortDate(iso: string) {
  const [, month, day] = iso.split('-');
  return `${Number(month)}.${Number(day)}`;
}

/** 필터 버튼에 보일 이름. 고르지 않았으면 null */
export function dateRangeLabel(range: DateRange) {
  if (range.preset) return datePresetLabels[range.preset];
  if (range.from && range.to) return range.from === range.to ? shortDate(range.from) : `${shortDate(range.from)}~${shortDate(range.to)}`;
  if (range.from) return `${shortDate(range.from)}부터`;
  if (range.to) return `${shortDate(range.to)}까지`;
  return null;
}

/** 지역 버튼에 보일 이름: '중구', '중구 외 2곳'. 고르지 않았으면 null */
export function regionLabel(names: string[]) {
  if (names.length === 0) return null;
  return names.length === 1 ? names[0] : `${names[0]} 외 ${names.length - 1}곳`;
}
