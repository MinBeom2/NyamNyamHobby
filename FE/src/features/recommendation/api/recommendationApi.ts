import { apiRequest } from '../../../shared/api/client';
import type { RecommendationResponse } from '../../exploration/api/explorationApi';

export type TagFeature =
  | 'INDOOR'
  | 'OUTDOOR'
  | 'BOTH_PLACES'
  | 'ALONE'
  | 'TOGETHER'
  | 'BOTH_SOCIAL'
  | 'ACTIVE'
  | 'LIGHT_ACTIVITY'
  | 'STATIC'
  | 'SPORTS'
  | 'NATURE'
  | 'CRAFT'
  | 'ART_PHOTO'
  | 'MUSIC'
  | 'COOKING'
  | 'BOOK_WRITING'
  | 'PLANTS'
  | 'GAME_PUZZLE'
  | 'HEALTH'
  | 'ACHIEVEMENT'
  | 'CREATION'
  | 'RELAXATION'
  | 'FOCUS'
  | 'KNOWLEDGE';

export type RecommendationPreviewResponse = {
  items: Array<{
    hobbyId: number;
    name: string;
    summary: string;
    thumbnailUrl: string | null;
    similarity: number;
    rank: number;
  }>;
};

export const recommendationQueryKeys = {
  all: ['recommendations'] as const,
  detail: (recommendationId: number) => ['recommendations', recommendationId] as const,
  latest: () => ['recommendations', 'latest'] as const,
};

export function createRecommendation(sessionId: number) {
  return apiRequest<RecommendationResponse>('/recommendations', {
    method: 'POST',
    body: JSON.stringify({ basis: 'EXPLORATION_SESSION', sessionId, hobbyIds: [] }),
  });
}

/** 직접 선택 추천: 고른 취미 1~10개로 '시작하기 좋은 순' 3개 + 비슷한 취미 3개를 만든다. 홈의 최근 추천이 이걸로 바뀐다. */
export function createSelectionRecommendation(hobbyIds: number[]) {
  return apiRequest<RecommendationResponse>('/recommendations', {
    method: 'POST',
    body: JSON.stringify({ basis: 'DIRECT_SELECTION', hobbyIds }),
  });
}

export function getRecommendation(recommendationId: number) {
  return apiRequest<RecommendationResponse>(`/recommendations/${recommendationId}`);
}

/** 내 가장 최근 추천(숏폼 탐색 또는 직접 선택). 둘 다 한 적이 없으면 null. */
export function getLatestRecommendation() {
  return apiRequest<RecommendationResponse | null>('/recommendations/latest');
}

export function previewRecommendations(request: {
  preferenceScores: Partial<Record<TagFeature, number>>;
  limit?: number;
  excludedHobbyIds?: number[];
}) {
  return apiRequest<RecommendationPreviewResponse>('/recommendations/preview', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
