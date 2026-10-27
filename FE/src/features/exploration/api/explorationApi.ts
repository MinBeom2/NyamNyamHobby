import { apiRequest } from '../../../shared/api/client';

export type ReactionType = 'LIKE' | 'DISLIKE' | 'IRRELEVANT';

export type ShortDetail = {
  shortId: number;
  youtubeVideoId: string;
  title: string;
  duration: number | null;
  hobby: {
    id: number;
    name: string;
    category: string | null;
  } | null;
  myReaction: ReactionType | null;
  isSaved: boolean;
};

export type StartExplorationResponse = {
  sessionId: number;
  maxCount: number;
  viewedCount: number;
  short: ShortDetail;
};

export type NextShortResponse = {
  finished: boolean;
  viewedCount: number;
  short: ShortDetail | null;
};

export type ExplorationProgressResponse = {
  currentStep: number;
  maxCount: number;
  reactedHobbies: Array<{
    id: number;
    name: string;
    reaction: ReactionType;
  }>;
  topInterests: Array<{
    hobbyName: string;
    percent: number;
  }>;
};

export type RecommendationResponse = {
  recommendationId: number;
  basis: string;
  isExploratory: boolean;
  reactionCount: number;
  items: Array<{
    itemId: number;
    hobbyId: number;
    hobbyName: string;
    category: string | null;
    type: string;
    rankNo: number;
    score: number;
    reason: string | null;
    reasonStatus: string;
    difficulty: string | null;
    sessionMinutes: string | null;
    monthlyCost: string | null;
    placeType: string | null;
    socialType: string | null;
    conditionWarning: boolean;
  }>;
};

export const explorationQueryKeys = {
  all: ['exploration'] as const,
  progress: (sessionId: number) => [...explorationQueryKeys.all, sessionId, 'progress'] as const,
};

export function startExploration() {
  return apiRequest<StartExplorationResponse>('/explorations', { method: 'POST' });
}

export function getNextShort(sessionId: number) {
  return apiRequest<NextShortResponse>(`/explorations/${sessionId}/next`);
}

export function recordShortView(
  sessionId: number,
  request: { shortId: number; watchedSeconds: number; playFailed: boolean },
) {
  return apiRequest<void>(`/explorations/${sessionId}/views`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function getExplorationProgress(sessionId: number) {
  return apiRequest<ExplorationProgressResponse>(`/explorations/${sessionId}/progress`);
}

export function setShortReaction(
  shortId: number,
  request: { type: ReactionType; sessionId: number },
) {
  return apiRequest<{ shortId: number; type: ReactionType }>(`/shorts/${shortId}/reaction`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export function deleteShortReaction(shortId: number) {
  return apiRequest<{ shortId: number; type: null }>(`/shorts/${shortId}/reaction`, {
    method: 'DELETE',
  });
}

export function finishExploration(sessionId: number) {
  return apiRequest<RecommendationResponse>(`/explorations/${sessionId}/finish`, {
    method: 'POST',
  });
}
