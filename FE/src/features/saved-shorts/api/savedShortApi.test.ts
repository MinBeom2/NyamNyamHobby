import { afterEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../../auth/model/authStore';
import { deleteSavedShort, saveShort } from '../../shortform/api/shortformApi';
import { getSavedShorts, getSavedShortSummary } from './savedShortApi';

afterEach(() => { vi.unstubAllGlobals(); useAuthStore.getState().clearSession(); });

describe('saved short API', () => {
  it('sends pagination, hobby ID and server sorting with the authenticated request', async () => {
    useAuthStore.getState().setSession({
      user: { id: 1, nickname: '테스터', email: 'test@example.com', role: 'USER', onboarded: true },
      accessToken: 'saved-test-token', expiresIn: 1800,
    });
    const response = { content: [], page: { number: 2, size: 5, totalElements: 11, totalPages: 3, hasNext: false } };
    const fetchMock = vi.fn((_url: string, _init?: RequestInit) => Promise.resolve(new Response(JSON.stringify({ success: true, data: response, error: null }), {
      headers: { 'Content-Type': 'application/json' },
    })));
    vi.stubGlobal('fetch', fetchMock);
    expect(await getSavedShorts({ page: 2, size: 5, hobbyId: 3, sort: 'oldest' })).toEqual(response);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain('/users/me/saved-shorts?page=2&size=5&sort=oldest&hobbyId=3');
    expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer saved-test-token');
  });
  it('reuses the persisted save and unsave endpoints and reads the summary independently', async () => {
    const fetchMock = vi.fn((url: string, init?: RequestInit) => Promise.resolve(new Response(JSON.stringify({
      success: true, error: null, data: url.endsWith('/summary') ? { totalCount: 0, hobbies: [] }
        : { shortId: 41, isSaved: init?.method === 'PUT' },
    }), { headers: { 'Content-Type': 'application/json' } })));
    vi.stubGlobal('fetch', fetchMock);
    expect(await saveShort(41)).toEqual({ shortId: 41, isSaved: true });
    expect(await deleteSavedShort(41)).toEqual({ shortId: 41, isSaved: false });
    expect(await getSavedShortSummary()).toEqual({ totalCount: 0, hobbies: [] });
    expect(fetchMock.mock.calls.map(([url, init]) => [url.split('/api/v1')[1], init?.method ?? 'GET']))
      .toEqual([['/shorts/41/save', 'PUT'], ['/shorts/41/save', 'DELETE'], ['/users/me/saved-shorts/summary', 'GET']]);
  });
});
