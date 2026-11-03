import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../../features/auth/model/authStore';
import { userQueryKeys } from '../../features/user/api/userApi';
import { savedShortQueryKeys, type SavedShortPageDto } from '../../features/saved-shorts/api/savedShortApi';
import { SavedShortsPage } from './SavedShortsPage';
import { MyPage } from './MyPage';

vi.mock('../../features/auth/model/authStore', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../features/auth/model/authStore')>();
  const store = original.useAuthStore;
  return { ...original, useAuthStore: Object.assign(
    (selector: (state: ReturnType<typeof store.getState>) => unknown) => selector(store.getState()), store,
  ) };
});

let client: QueryClient;
const listKey = savedShortQueryKeys.list(1, null, 'recent');
const summaryKey = savedShortQueryKeys.summary(1);
beforeEach(() => {
  client = new QueryClient({ defaultOptions: { queries: { retry: false, retryOnMount: false } } });
  useAuthStore.getState().setSession({
    user: { id: 1, email: 'test@example.com', nickname: '테스터', role: 'USER', onboarded: true },
    accessToken: 'saved-test-token', expiresIn: 1800,
  });
});
afterEach(() => { client.clear(); useAuthStore.getState().clearSession(); });

function render(page = <SavedShortsPage />) {
  return renderToStaticMarkup(<QueryClientProvider client={client}><MemoryRouter>{page}</MemoryRouter></QueryClientProvider>);
}
function seed(page: SavedShortPageDto) {
  client.setQueryData(listKey, { pages: [page], pageParams: [0] });
}

describe('saved short pages', () => {
  it('shows loading while the real API query is pending', () => {
    const html = render();
    expect(html).toContain('보관한 영상을 불러오고 있어요.');
    expect(html).not.toContain('퇴근 후 1시간, 볼더링 첫 도전기');
  });
  it('shows an empty state instead of fixtures when there are no saved shorts', () => {
    seed({ content: [], page: { number: 0, size: 5, totalElements: 0, totalPages: 0, hasNext: false } });
    client.setQueryData(summaryKey, { totalCount: 0, hobbies: [] });
    const html = render();
    expect(html).toContain('아직 보관한 영상이 없어요.');
    expect(html).toContain('전체 0');
    expect(html).not.toContain('보관 해제');
  });
  it('renders server records, full counts, playback links and unplayable saved records', () => {
    seed({ content: [
      { shortId: 41, youtubeVideoId: 'video000041', title: '실제 보관 영상', thumbnailUrl: 'https://example.com/41.jpg',
        hobby: { id: 3, name: '서버 취미' }, status: 'ACTIVE', savedAt: '2026-10-20T05:20:00Z' },
      { shortId: 42, youtubeVideoId: 'video000042', title: '이용 불가 보관 영상', thumbnailUrl: null,
        hobby: null, status: 'UNAVAILABLE', savedAt: '2026-10-19T05:20:00Z' },
    ], page: { number: 0, size: 5, totalElements: 17, totalPages: 4, hasNext: true } });
    client.setQueryData(summaryKey, { totalCount: 17, hobbies: [{ id: 3, name: '서버 취미', count: 16 }, { id: null, name: null, count: 1 }] });
    const html = render();
    expect(html).toContain('실제 보관 영상');
    expect(html).toContain('전체 17');
    expect(html).toContain('서버 취미 16');
    expect(html).toContain('href="https://www.youtube.com/shorts/video000041"');
    expect(html).not.toContain('href="https://www.youtube.com/shorts/video000042"');
    expect(html).toContain('재생할 수 없는 영상');
    expect(html.match(/보관 해제/g)).toHaveLength(2);
    expect(html).toContain('더 보기');
    client.setQueryData(userQueryKeys.summary(), { savedHobbyCount: 2, inProgressHobbyCount: 1, savedShortCount: 17, myPostCount: 4 });
    expect(render(<MyPage />)).toContain('17</strong><span>보관한 숏폼</span>');
  });
  it('uses the unified MyPage summary for all stats and keeps the hobby and post tabs', () => {
    client.setQueryData(summaryKey, { totalCount: 99, hobbies: [] });
    client.setQueryData(userQueryKeys.summary(), { savedHobbyCount: 2, inProgressHobbyCount: 1, savedShortCount: 17, myPostCount: 4 });
    const html = render(<MyPage />);
    expect(html).toContain('2</strong><span>관심 취미</span>');
    expect(html).toContain('1</strong><span>진행 중</span>');
    expect(html).toContain('17</strong><span>보관한 숏폼</span>');
    expect(html).toContain('4</strong><span>내 글</span>');
    expect(html).toContain('내가 쓴 글');
    expect(html).toContain('href="/mypage/saved-shorts"');
    expect(html).not.toContain('99</strong>');
  });
  it('does not present missing or failed MyPage counts as zero saved shorts', async () => {
    expect(render(<MyPage />)).toContain('…</strong><span>보관한 숏폼</span>');
    await client.fetchQuery({ queryKey: userQueryKeys.summary(),
      queryFn: () => Promise.reject(new Error('요약 조회 실패')), retry: false,
    }).catch(() => undefined);
    expect(render(<MyPage />)).toContain('확인 불가</strong><span>보관한 숏폼</span>');
  });
  it('shows query errors and retry without presenting a failed request as an empty list', async () => {
    await client.fetchInfiniteQuery({ queryKey: listKey, initialPageParam: 0,
      queryFn: () => Promise.reject(new Error('서버 응답 오류')), retry: false,
    }).catch(() => undefined);
    const html = render();
    expect(html).toContain('보관한 영상을 불러오지 못했어요.');
    expect(html).toContain('서버 응답 오류');
    expect(html).toContain('다시 시도');
    expect(html).not.toContain('아직 보관한 영상이 없어요.');
  });
});
