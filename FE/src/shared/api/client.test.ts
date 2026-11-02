import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../../features/auth/model/authStore';
import { apiRequest } from './client';

const user = { id: 1, email: 'test@example.com', nickname: '테스터', role: 'USER', onboarded: true } as const;
const json = (data: unknown, status = 200) => new Response(JSON.stringify(data), {
  status, headers: { 'Content-Type': 'application/json' },
});
const success = (data: unknown) => json({ success: true, data, error: null });
const expired = () => json({ success: false, data: null, error: {
  code: 'AUTH_TOKEN_EXPIRED', message: 'expired', fields: null, retryAfterSeconds: null,
} }, 401);

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((complete) => { resolve = complete; });
  return { promise, resolve };
}

beforeEach(() => {
  useAuthStore.getState().setSession({ user, accessToken: 'expired-token', expiresIn: 1 });
});
afterEach(() => { vi.unstubAllGlobals(); });

describe('session refresh', () => {
  it('shares one refresh across concurrent expired requests and retries both with the new token', async () => {
    const refresh = deferred<Response>();
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (url.endsWith('/auth/refresh')) return refresh.promise;
      return Promise.resolve(new Headers(init?.headers).get('Authorization') === 'Bearer renewed-token'
        ? success({ ok: true }) : expired());
    });
    vi.stubGlobal('fetch', fetchMock);
    const requests = Promise.all([apiRequest('/users/me'), apiRequest('/users/me/conditions')]);
    await vi.waitFor(() => expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/auth/refresh'))).toHaveLength(1));
    refresh.resolve(success({ accessToken: 'renewed-token', expiresIn: 1800 }));
    await expect(requests).resolves.toEqual([{ ok: true }, { ok: true }]);
    expect(useAuthStore.getState().user).toEqual(user);
  });

  it('keeps the login when the refresh server has a temporary failure', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string) => Promise.resolve(url.endsWith('/auth/refresh') ? json({}, 503) : expired())));
    await expect(apiRequest('/users/me')).rejects.toMatchObject({ status: 503 });
    expect(useAuthStore.getState().user).toEqual(user);
  });

  it('reuses the refreshed token for an expired response that arrives later', async () => {
    const lateResponse = deferred<Response>();
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (url.endsWith('/auth/refresh')) return Promise.resolve(success({ accessToken: 'renewed-token', expiresIn: 1800 }));
      if (new Headers(init?.headers).get('Authorization') === 'Bearer renewed-token') return Promise.resolve(success({ ok: true }));
      return url.endsWith('/late') ? lateResponse.promise : Promise.resolve(expired());
    });
    vi.stubGlobal('fetch', fetchMock);
    const lateRequest = apiRequest('/late');
    await apiRequest('/users/me');
    lateResponse.resolve(expired());
    await expect(lateRequest).resolves.toEqual({ ok: true });
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/auth/refresh'))).toHaveLength(1);
  });

  it('clears the login when the refresh cookie is rejected', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(expired())));
    await expect(apiRequest('/users/me')).rejects.toMatchObject({ status: 401 });
    expect(useAuthStore.getState()).toMatchObject({ user: null, accessToken: null, sessionInitialized: true });
  });

  it('does not resurrect a session when an old refresh finishes after logout', async () => {
    const refresh = deferred<Response>();
    const fetchMock = vi.fn((url: string) => url.endsWith('/auth/refresh') ? refresh.promise : Promise.resolve(expired()));
    vi.stubGlobal('fetch', fetchMock);
    const request = apiRequest('/users/me');
    const rejected = expect(request).rejects.toMatchObject({ code: 'AUTH_SESSION_CHANGED' });
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    useAuthStore.getState().clearSession();
    refresh.resolve(success({ accessToken: 'old-session-token', expiresIn: 1800 }));
    await rejected;
    expect(useAuthStore.getState()).toMatchObject({ user: null, accessToken: null });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('does not clear a new login when the previous session refresh is rejected late', async () => {
    const refresh = deferred<Response>();
    const fetchMock = vi.fn((url: string) => url.endsWith('/auth/refresh') ? refresh.promise : Promise.resolve(expired()));
    vi.stubGlobal('fetch', fetchMock);
    const request = apiRequest('/users/me');
    const rejected = expect(request).rejects.toMatchObject({ status: 401 });
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    useAuthStore.getState().clearSession();
    useAuthStore.getState().setSession({ user: { ...user, id: 2 }, accessToken: 'new-login-token', expiresIn: 1800 });
    refresh.resolve(expired());
    await rejected;
    expect(useAuthStore.getState().accessToken).toBe('new-login-token');
    expect(useAuthStore.getState().user?.id).toBe(2);
  });
});
