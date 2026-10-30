import { apiRequest } from '../../../shared/api/client';

export type ShortSaveResponse = { shortId: number; isSaved: boolean };

export function saveShort(shortId: number) {
  return apiRequest<ShortSaveResponse>(`/shorts/${shortId}/save`, { method: 'PUT' });
}

export function deleteSavedShort(shortId: number) {
  return apiRequest<ShortSaveResponse>(`/shorts/${shortId}/save`, { method: 'DELETE' });
}
