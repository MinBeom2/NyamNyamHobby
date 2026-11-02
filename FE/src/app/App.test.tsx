import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, type InitialEntry } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { useAuthStore } from '../features/auth/model/authStore';

vi.mock('../features/auth/model/authStore', async (importOriginal) => {
  const original = await importOriginal<typeof import('../features/auth/model/authStore')>();
  const store = original.useAuthStore;
  // This suite renders HTML in Node; read the scenario state instead of React's
  // immutable server snapshot. Store actions remain the real implementation.
  return { ...original, useAuthStore: Object.assign(
    (selector: (state: ReturnType<typeof store.getState>) => unknown) => selector(store.getState()), store,
  ) };
});

const user = { id: 1, email: 'test@example.com', nickname: '테스터', role: 'USER', onboarded: true } as const;
const clients: QueryClient[] = [];

function renderPage(entry: InitialEntry) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  clients.push(client);
  return renderToStaticMarkup(<QueryClientProvider client={client}><MemoryRouter initialEntries={[entry]}><App /></MemoryRouter></QueryClientProvider>);
}

beforeEach(() => {
  useAuthStore.getState().clearSession();
});
afterEach(() => { clients.splice(0).forEach((client) => client.clear()); vi.restoreAllMocks(); });

describe('guest access and session-aware pages', () => {
  it.each(['/hobbies', '/hobbies/1', '/hobbies/recommendations', '/places', '/info-feed', '/info-feed/1', '/community', '/community/1', '/mypage', '/my-hobbies'])('does not render member content at %s for a guest', (path) => {
    expect(renderPage(path)).toBe('');
  });

  it('waits for session restoration before deciding access to a protected URL', () => {
    useAuthStore.setState({ sessionInitialized: false });
    const html = renderPage('/places');
    expect(html).toContain('로그인 상태를 확인하고 있어요.');
    expect(html).not.toContain('활동 장소 찾기');
  });

  it('shows the existing signup dialog on the guest home', () => {
    const html = renderPage({ pathname: '/', state: { signupRequired: '취미 찾기', returnTo: '/' } });
    expect(html).toContain('role="dialog"');
    expect(html).toContain('회원가입 후 이용할 수 있어요');
  });

  it('allows guest notices and the guest short exploration', () => {
    expect(renderPage('/notices')).toContain('공지사항');
    expect(renderPage('/explore?guest=true')).toContain('다음 영상');
    expect(useAuthStore.getState().user).toBeNull();
  });

  it('keeps the member header and session across public, member and home pages', () => {
    useAuthStore.getState().setSession({ user, accessToken: 'test-token', expiresIn: 1800 });
    for (const path of ['/notices', '/explore', '/hobbies', '/', '/home']) {
      const html = renderPage(path);
      expect(html).toContain('로그아웃');
      expect(html).toContain('aria-label="냠냠취미 홈"');
      expect(html).toContain('href="/home"');
      expect(html).not.toContain('class="login-link"');
      expect(useAuthStore.getState().accessToken).toBe('test-token');
    }
    expect(renderPage('/')).toContain('테스터');
  });
});
