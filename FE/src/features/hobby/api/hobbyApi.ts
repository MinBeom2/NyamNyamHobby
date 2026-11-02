import { apiRequest } from '../../../shared/api/client';

export type HobbyShortDto = {
  shortId: number;
  youtubeVideoId: string;
  title: string;
  duration: number;
  thumbnailUrl: string | null;
};

export type HobbyDifficulty = 'EASY' | 'NORMAL' | 'HARD';
export type HobbyPlaceType = 'INDOOR' | 'OUTDOOR' | 'BOTH';
export type HobbySocialType = 'ALONE' | 'TOGETHER' | 'BOTH';
export type HobbyStatus = 'NONE' | 'SAVED' | 'IN_PROGRESS' | 'PAUSED' | 'STOPPED' | 'COMPLETED';

export type NumberRangeDto = { min: number; max: number };
export type HobbyCategoryDto = { id: number; name: string };

export type HobbyListItemDto = {
  id: number;
  name: string;
  category: HobbyCategoryDto;
  summary: string;
  thumbnailUrl: string | null;
  difficulty: HobbyDifficulty;
  placeType: HobbyPlaceType;
  socialType: HobbySocialType;
  sessionMinutes: NumberRangeDto;
  monthlyCost: NumberRangeDto;
  saveCount: number;
  tags: string[];
};

export type HobbyDetailDto = HobbyListItemDto & {
  description: string;
  initialCost: NumberRangeDto;
  supplies: string;
  environment: string;
  firstActivity: string;
  cautions: string;
  myStatus: HobbyStatus;
};

export type HobbyPlanItemDto = {
  seq: number;
  title: string;
  description: string;
  expectedMinutes: number;
  supplies: string;
  doneCriteria: string;
};

export type HobbyPlanPreviewDto = {
  ready: boolean;
  skillLevel: string;
  title: string;
  totalWeeks: number;
  previewWeeks: number;
  weeks: Array<{ weekNo: number; title: string; items: HobbyPlanItemDto[] }>;
};

export type HobbySearchParams = {
  keyword?: string;
  time?: '1H_LESS' | '1H_TO_2H' | '2H_MORE';
  cost?: '30K_LESS' | '30K_TO_100K' | '100K_MORE';
  difficulty?: HobbyDifficulty;
  place?: 'INDOOR' | 'OUTDOOR' | 'ANY';
  social?: 'ALONE' | 'TOGETHER' | 'ANY';
  sort?: 'RECOMMEND' | 'POPULAR' | 'NAME' | 'COST_ASC';
  page?: number;
  size?: number;
};

export type HobbySearchResponse = {
  content: HobbyListItemDto[];
  page: { number: number; size: number; totalElements: number; totalPages: number; hasNext: boolean };
};

export const hobbyQueryKeys = {
  all: ['hobbies'] as const,
  list: (params: HobbySearchParams) => [...hobbyQueryKeys.all, 'list', params] as const,
  detail: (hobbyId: number) => [...hobbyQueryKeys.all, 'detail', hobbyId] as const,
  planPreview: (hobbyId: number, skillLevel = 'BEGINNER') => [...hobbyQueryKeys.all, hobbyId, 'plan-preview', skillLevel] as const,
  shorts: (hobbyId: number, limit: number) => ['hobbies', hobbyId, 'shorts', limit] as const,
};

function toQuery(params: HobbySearchParams) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') query.set(key, String(value));
  });
  return query.toString();
}

export function getHobbies(params: HobbySearchParams = {}) {
  const query = toQuery(params);
  return apiRequest<HobbySearchResponse>(`/hobbies${query ? `?${query}` : ''}`);
}

export function getHobbyDetail(hobbyId: number) {
  return apiRequest<HobbyDetailDto>(`/hobbies/${hobbyId}`);
}

export function getHobbyPlanPreview(hobbyId: number, skillLevel = 'BEGINNER') {
  const query = new URLSearchParams({ skillLevel });
  return apiRequest<HobbyPlanPreviewDto>(`/hobbies/${hobbyId}/plan-preview?${query.toString()}`);
}

export function startHobby(hobbyId: number, skillLevel = 'BEGINNER') {
  return apiRequest<{ userHobbyId: number; planId: number; status: string; title: string }>(`/users/me/hobbies/${hobbyId}/start`, {
    method: 'POST',
    body: JSON.stringify({ skillLevel }),
  });
}

export function getHobbyShorts(hobbyId: number, limit = 6) {
  return apiRequest<HobbyShortDto[]>(`/hobbies/${hobbyId}/shorts?limit=${limit}`);
}

export function dislikeHobby(hobbyId: number) {
  return apiRequest<{ hobbyId: number; disliked: boolean }>(`/hobbies/${hobbyId}/dislike`, { method: 'PUT' });
}

export function cancelHobbyDislike(hobbyId: number) {
  return apiRequest<{ hobbyId: number; disliked: boolean }>(`/hobbies/${hobbyId}/dislike`, { method: 'DELETE' });
}
