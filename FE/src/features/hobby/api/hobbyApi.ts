import { apiRequest } from '../../../shared/api/client';

export type HobbyShortDto = {
  shortId: number;
  youtubeVideoId: string;
  title: string;
  duration: number;
  thumbnailUrl: string | null;
};

export const hobbyQueryKeys = {
  all: ['hobbies'] as const,
  shorts: (hobbyId: number, limit: number) => ['hobbies', hobbyId, 'shorts', limit] as const,
};

export function getHobbyShorts(hobbyId: number, limit = 6) {
  return apiRequest<HobbyShortDto[]>(`/hobbies/${hobbyId}/shorts?limit=${limit}`);
}

export function dislikeHobby(hobbyId: number) {
  return apiRequest<{ hobbyId: number; disliked: boolean }>(`/hobbies/${hobbyId}/dislike`, { method: 'PUT' });
}

export function cancelHobbyDislike(hobbyId: number) {
  return apiRequest<{ hobbyId: number; disliked: boolean }>(`/hobbies/${hobbyId}/dislike`, { method: 'DELETE' });
}
