import type { FeedItemDto, FeedType } from '../api/feedApi';

export const feedTypeLabels: Record<FeedType, string> = {
  EVENT: '축제·행사',
  SHOW: '공연·전시',
  CLASS: '강좌·클래스',
  GUIDE: '입문 자료',
};

export type FeedItemModel = {
  id: number;
  hobbyName: string;
  typeLabel: string;
  title: string;
  summary: string;
  thumbnailUrl: string | null;
  /** 출처 · 기간(행사) 또는 출처 · 게시일(입문 자료 등 기간이 없는 항목) */
  meta: string;
  sourceUrl: string;
  ended: boolean;
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
    hobbyName: dto.hobbyName,
    typeLabel: feedTypeLabels[dto.type],
    title: dto.title,
    summary: dto.summary ?? '',
    thumbnailUrl: dto.thumbnailUrl,
    meta: [dto.sourceName, period ?? `게시 ${formatMonthDay(dto.publishedAt)}`].join(' · '),
    sourceUrl: dto.sourceUrl,
    ended: dto.isEnded,
  };
}
