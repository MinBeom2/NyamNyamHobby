import { apiRequest } from '../../../shared/api/client';
import type { PageDto } from '../../community/api/communityApi';

export type FeedType = 'EVENT' | 'SHOW' | 'CLASS' | 'GUIDE';
/** MY = 내 취미 + 진행 중 취미(회원 기본), SAVED = 관심 저장한 취미, ALL = 전체 */
export type FeedScope = 'MY' | 'SAVED' | 'ALL';

export type FeedItemDto = {
  id: number;
  hobbyId: number;
  hobbyName: string;
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
};

export type FeedQuery = {
  scope: FeedScope;
  hobbyId?: number;
  type?: FeedType;
  excludeEnded: boolean;
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
  return apiRequest<PageDto<FeedItemDto>>(`/feeds?${params}`);
}
