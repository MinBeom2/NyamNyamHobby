import { apiRequest } from '../../../shared/api/client';

export type SavedShortSort = 'recent' | 'oldest';
export type SavedShortDto = {
  shortId: number;
  youtubeVideoId: string;
  title: string;
  thumbnailUrl: string | null;
  hobby: { id: number; name: string } | null;
  status: 'CANDIDATE' | 'NEEDS_REVIEW' | 'ACTIVE' | 'UNAVAILABLE' | 'AUTO_HIDDEN' | 'REJECTED';
  savedAt: string;
};
export type SavedShortPageDto = {
  content: SavedShortDto[];
  page: { number: number; size: number; totalElements: number; totalPages: number; hasNext: boolean };
};
export type SavedShortSummaryDto = {
  totalCount: number;
  hobbies: Array<{ id: number | null; name: string | null; count: number }>;
};

export const savedShortQueryKeys = {
  all: ['saved-shorts'] as const,
  list: (userId: number | undefined, hobbyId: number | null, sort: SavedShortSort) =>
    [...savedShortQueryKeys.all, userId, 'list', hobbyId, sort] as const,
  summary: (userId: number | undefined) => [...savedShortQueryKeys.all, userId, 'summary'] as const,
};

export function getSavedShorts({ page = 0, size = 20, hobbyId, sort = 'recent' }: {
  page?: number; size?: number; hobbyId?: number | null; sort?: SavedShortSort;
} = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size), sort });
  if (hobbyId != null) params.set('hobbyId', String(hobbyId));
  return apiRequest<SavedShortPageDto>(`/users/me/saved-shorts?${params}`);
}

export function getSavedShortSummary() {
  return apiRequest<SavedShortSummaryDto>('/users/me/saved-shorts/summary');
}
