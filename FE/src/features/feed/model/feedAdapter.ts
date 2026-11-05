import type { FeedItemDto, FeedType } from '../api/feedApi';

export const feedTypeLabels: Record<FeedType, string> = {
  EVENT: '축제·행사',
  SHOW: '공연·전시',
  CLASS: '강좌·클래스',
  GUIDE: '입문 자료',
};

/** 취미 30종 어디에도 맞지 않는 피드의 이름. 취미 '기타'(악기)와 헷갈리지 않게 따로 둔다 */
export const OTHER_HOBBY_LABEL = '그 외';

export type FeedItemModel = {
  id: number;
  hobbyName: string;
  typeLabel: string;
  title: string;
  summary: string;
  thumbnailUrl: string | null;
  /** 진행 지역 · 기간. 지역이 없는 입문 자료는 작성처 · 게시일 */
  meta: string;
  sourceUrl: string;
  /** 행사가 끝남 → 회색 카드, 링크 닫힘 */
  ended: boolean;
  /** 강좌 접수가 끝남 → 회색 카드, 링크는 열림. 종료된 행사에는 붙이지 않는다 */
  closed: boolean;
};

function formatMonthDay(date: string) {
  const [, month, day] = date.slice(0, 10).split('-');
  return `${month}.${day}`;
}

function formatPeriod(start: string | null, end: string | null) {
  if (!start && !end) return null;
  if (!start || !end || start === end) return formatMonthDay((start ?? end) as string);
  return `${formatMonthDay(start)}~${formatMonthDay(end)}`;
}

export function toFeedItemModel(dto: FeedItemDto): FeedItemModel {
  const period = formatPeriod(dto.eventStart, dto.eventEnd);
  return {
    id: dto.id,
    hobbyName: dto.hobbyName ?? OTHER_HOBBY_LABEL,
    typeLabel: feedTypeLabels[dto.type],
    title: dto.title,
    summary: dto.summary ?? '',
    thumbnailUrl: dto.thumbnailUrl,
    meta: [
      dto.regionName ?? (dto.type === 'GUIDE' ? dto.sourceName : null),
      period ?? `게시 ${formatMonthDay(dto.publishedAt)}`,
    ].filter(Boolean).join(' · '),
    sourceUrl: dto.sourceUrl,
    ended: dto.isEnded,
    closed: !dto.isEnded && dto.isClosed,
  };
}
