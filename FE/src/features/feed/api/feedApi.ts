import { apiRequest } from '../../../shared/api/client';
import type { PageDto } from '../../community/api/communityApi';

export type FeedType = 'EVENT' | 'SHOW' | 'CLASS' | 'GUIDE';
/** MY = 내 취미 + 진행 중 취미(회원 기본), SAVED = 관심 저장한 취미, ALL = 전체 */
export type FeedScope = 'MY' | 'SAVED' | 'ALL';

export type FeedItemDto = {
  id: number;
  /** null이면 취미 30종 어디에도 맞지 않는 '그 외' 피드 */
  hobbyId: number | null;
  hobbyName: string | null;
  type: FeedType;
  title: string;
  summary: string | null;
  sourceName: string;
  sourceUrl: string;
  thumbnailUrl: string | null;
  /** 진행 지역(예: '서울 송파구'). 입문 자료처럼 지역이 없으면 null */
  regionName: string | null;
  eventStart: string | null;
  eventEnd: string | null;
  publishedAt: string;
  isEnded: boolean;
  /** 강좌 접수가 끝남(접수종료·예약마감 등). 행사 자체는 진행 중일 수 있어 링크는 열려 있다 */
  isClosed: boolean;
};

export type FeedQuery = {
  scope: FeedScope;
  hobbyId?: number;
  type?: FeedType;
  excludeEnded: boolean;
  /** 제목·요약 검색어(2자 이상) */
  keyword?: string;
  /** yyyy-MM-dd. 진행 기간이 [from, to]와 겹치는 항목 */
  from?: string;
  to?: string;
  regionIds?: number[];
  /** true면 '그 외' 피드만(hobbyId보다 우선) */
  uncategorized?: boolean;
};

export const feedQueryKeys = {
  all: ['feeds'] as const,
  list: (query: FeedQuery) => [...feedQueryKeys.all, 'list', query] as const,
};

export function getFeeds(query: FeedQuery, page = 0, size = 20) {
  const params = new URLSearchParams({
    scope: query.scope,
    excludeEnded: String(query.excludeEnded),
    page: String(page),
    size: String(size),
  });
  if (query.hobbyId) params.set('hobbyId', String(query.hobbyId));
  if (query.type) params.set('type', query.type);
  if (query.keyword) params.set('keyword', query.keyword);
  if (query.from) params.set('from', query.from);
  if (query.to) params.set('to', query.to);
  if (query.regionIds?.length) params.set('regionIds', query.regionIds.join(','));
  if (query.uncategorized) params.set('uncategorized', 'true');
  return apiRequest<PageDto<FeedItemDto>>(`/feeds?${params}`);
}
