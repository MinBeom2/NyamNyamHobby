import { apiRequest } from '../../../shared/api/client';
import { getHobbies } from '../../hobby/api/hobbyApi';

export type SidoDto = { sido: string; enabled: boolean };

export type RegionDto = {
  id: number;
  code: string;
  sido: string;
  sigungu: string;
};

export type PlaceCategory = 'PARK' | 'SPORTS' | 'STUDIO' | 'CLASS' | 'GYM' | 'ETC';

export type PlaceDto = {
  id: number;
  name: string;
  category: PlaceCategory;
  address: string;
  lat: number | null;
  lng: number | null;
  openingHours: string | null;
  feeText: string | null;
  externalUrl: string | null;
  thumbnailUrl: string | null;
  source: string;
  needsInfoCheck: boolean;
};

export type PlaceHobbyDto = {
  id: number;
  name: string;
  thumbnailUrl?: string | null;
};

export const placeQueryKeys = {
  all: ['places'] as const,
  regions: (sido?: string) => ['regions', sido ?? 'all'] as const,
  hobbies: ['places', 'hobbies'] as const,
  list: (regionId: number | undefined, hobbyId: number | undefined) =>
    [...placeQueryKeys.all, 'list', regionId, hobbyId] as const,
};

export function getSidos() {
  return apiRequest<SidoDto[]>('/regions');
}

export function getRegions(sido: string) {
  return apiRequest<RegionDto[]>(`/regions?sido=${encodeURIComponent(sido)}`);
}

export async function getPlaceHobbies() {
  const content: PlaceHobbyDto[] = [];
  let page = 0;
  while (true) {
    const response = await getHobbies({ page, size: 50, sort: 'NAME' });
    content.push(...response.content);
    if (!response.page.hasNext) return { content };
    page += 1;
  }
}

export function getPlaces(regionId?: number, hobbyId?: number) {
  const query = new URLSearchParams();
  if (regionId !== undefined) query.set('regionId', String(regionId));
  if (hobbyId !== undefined) query.set('hobbyId', String(hobbyId));
  return apiRequest<PlaceDto[]>(`/places?${query.toString()}`);
}
