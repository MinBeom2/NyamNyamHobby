import { apiRequest } from '../../../shared/api/client';
import type { ShortDetail } from '../../exploration/api/explorationApi';

export type ShortSaveResponse = { shortId: number; isSaved: boolean };

export function saveShort(shortId: number) {
  return apiRequest<ShortSaveResponse>(`/shorts/${shortId}/save`, { method: 'PUT' });
}

export function deleteSavedShort(shortId: number) {
  return apiRequest<ShortSaveResponse>(`/shorts/${shortId}/save`, { method: 'DELETE' });
}

/** 자유 탐색(상단 '숏폼 탐색') 한 묶음. 공개 숏폼 전체를 seed로 섞은 순서다 */
export type ShortFeedResponse = {
  items: ShortDetail[];
  seed: number;
  /** 다음 요청 cursor. 한 바퀴를 다 봤으면 null → seed 없이 다시 요청해 새로 섞는다 */
  nextCursor: number | null;
  total: number;
};

export function getShortFeed(params: { seed?: number; cursor?: number; size?: number } = {}) {
  const query = new URLSearchParams();
  if (params.seed !== undefined) query.set('seed', String(params.seed));
  query.set('cursor', String(params.cursor ?? 0));
  query.set('size', String(params.size ?? 10));
  return apiRequest<ShortFeedResponse>(`/shorts/feed?${query.toString()}`);
}
