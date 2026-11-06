// @vitest-environment jsdom
import { render, screen, waitFor, within, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { MyHobbiesPage } from './ActivityPages';
import { managementHobbies, managementItems, managementPlan } from '../../mocks/activityManagementFixtures';
import type { PlanItemDto, UpdatePlanItemRequest } from '../../features/activity/api/activityApi';
import { activityQueryKeys } from '../../features/activity/api/activityApi';

const envelope = (data: unknown) => HttpResponse.json({ success: true, data, error: null });
const failure = (code = 'COMMON_INTERNAL_ERROR', message = '테스트 서버 오류', status = 500) => HttpResponse.json({ success: false, data: null, error: { code, message, fields: null, retryAfterSeconds: null } }, { status });
const server = setupServer();
let items: PlanItemDto[];
let hobbyStatus = 'IN_PROGRESS';
let requests: { method: string; path: string; body?: Record<string, unknown> }[];
let failSave = false;
let client: QueryClient;

beforeAll(() => {
  // jsdom's AbortSignal has a different realm from Node's Request. Navigation
  // requests have no loaders here; omit that test-only signal bridge.
  const NodeRequest = globalThis.Request;
  vi.stubGlobal('Request', class extends NodeRequest {
    constructor(input: RequestInfo | URL, init?: RequestInit) { super(input, init ? { ...init, signal: undefined } : init); }
  });
  server.listen({ onUnhandledRequest: 'error' });
  // Browser APIs not implemented by jsdom; actual focus/dialog behavior is also
  // inspected in Chrome. The DOM still exposes open dialogs to role queries.
  HTMLDialogElement.prototype.showModal = function () { this.setAttribute('open', ''); };
  HTMLDialogElement.prototype.close = function () { this.removeAttribute('open'); };
  Element.prototype.scrollIntoView = vi.fn();
  const nativeFetch = globalThis.fetch;
  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => nativeFetch(typeof input === 'string' ? new URL(input, 'http://localhost') : input, init));
});
afterAll(() => { server.close(); vi.unstubAllGlobals(); });
beforeEach(() => {
  items = managementItems(); hobbyStatus = 'IN_PROGRESS'; requests = []; failSave = false;
  server.use(
    http.get('http://localhost/api/v1/users/me/hobbies', () => envelope(managementHobbies().map((hobby) => hobby.hobbyId === 1 ? { ...hobby, status: hobbyStatus, planProgress: managementPlan(2, items).planProgress } : hobby))),
    http.get('http://localhost/api/v1/users/me/todos/summary', () => envelope({ inProgressCount: hobbyStatus === 'IN_PROGRESS' ? 1 : 0, maxInProgressCount: 3, remainingThisWeekCount: 1, overdueCount: 1, statusCounts: {}, plans: [] })),
    http.get('http://localhost/api/v1/plans/88', ({ request }) => {
      const week = Number(new URL(request.url).searchParams.get('week') ?? 2);
      return envelope(managementPlan(week, items, { hobbyStatus, canUpdateItems: hobbyStatus === 'IN_PROGRESS' && week <= 2, canEditPlan: hobbyStatus === 'IN_PROGRESS' }));
    }),
    http.get('http://localhost/api/v1/plans/89', () => envelope(managementPlan(1, [], { planId: 89, hobbyId: 2, hobbyStatus: 'PAUSED', currentWeek: 1, totalWeeks: 8, canEditPlan: false, canUpdateItems: false, title: '드로잉 · 입문 8주 계획', weeks: Array.from({ length: 8 }, (_, index) => ({ ...managementPlan().weeks[0], weekNo: index + 1 })) }))),
    http.patch('http://localhost/api/v1/plans/88/items/:id', async ({ params, request }) => {
      const body = await request.json() as UpdatePlanItemRequest;
      requests.push({ method: 'PATCH', path: request.url, body });
      if (failSave) return failure();
      const item = items.find((item) => item.itemId === Number(params.id))!;
      Object.assign(item, body);
      return envelope({ itemId: item.itemId, status: item.status, memo: item.memo, item, ...managementPlan(item.weekNo, items) });
    }),
    http.patch('http://localhost/api/v1/users/me/hobbies/1/status', async ({ request }) => {
      const body = await request.json() as { status: string }; hobbyStatus = body.status;
      requests.push({ method: 'PATCH', path: request.url, body });
      return envelope({ userHobbyId: 99, planId: 88, status: hobbyStatus, currentWeek: 2, startedAt: managementPlan().startedAt, pausedAt: null });
    }),
    http.post('http://localhost/api/v1/plans/88/items', async ({ request }) => {
      const body = await request.json() as Record<string, unknown>;
      requests.push({ method: 'POST', path: request.url, body });
      const item = { ...items[0], ...body, itemId: 999, seq: 99, status: 'TODO' as const, memo: null, isCustom: true } as PlanItemDto;
      items.push(item); return envelope({ item, ...managementPlan(item.weekNo, items) });
    }),
    http.delete('http://localhost/api/v1/plans/88/items/:id', ({ params, request }) => {
      requests.push({ method: 'DELETE', path: request.url }); items = items.filter((item) => item.itemId !== Number(params.id)); return new HttpResponse(null, { status: 204 });
    }),
    http.delete('http://localhost/api/v1/users/me/hobbies/:id/save', () => failure('COMMON_INTERNAL_ERROR', '관심 해제 실패')),
  );
});
afterEach(() => { cleanup(); client?.clear(); server.resetHandlers(); });

function mount(path = '/my-hobbies/1/plan?planId=88&week=2') {
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } } });
  const router = createMemoryRouter([{ path: '/my-hobbies', element: <MyHobbiesPage /> }, { path: '/my-hobbies/:hobbyId/plan', element: <MyHobbiesPage /> }, { path: '/my-hobbies/:hobbyId/todos', element: <MyHobbiesPage /> }, { path: '/hobbies', element: <h1>취미 찾기</h1> }], { initialEntries: [path] });
  render(<QueryClientProvider client={client}><RouterProvider router={router} /></QueryClientProvider>);
  return { router, user: userEvent.setup() };
}
const ready = () => screen.findByRole('heading', { name: '캔들·디퓨저 · 입문 3주 계획' });
const article = (title = '집에서 작은 캔들 만들어보기') => screen.getByRole('article', { name: title });

describe('hobby management API and interaction flows', () => {
  it('selects the first active hobby and restores week in the URL', async () => {
    const { router } = mount('/my-hobbies'); await ready();
    await waitFor(() => expect(router.state.location.search).toBe('?planId=88&week=2'));
    expect((await screen.findByRole('progressbar')).getAttribute('aria-valuenow')).toBe('31');
    expect(screen.getByText('전체 16개 중 5개 완료 · 건너뜀 1개')).toBeTruthy();
    expect(screen.getAllByRole('tab')).toHaveLength(3);
  });

  it('uses eight tabs for an eight week paused plan and preserves read-only records', async () => {
    mount('/my-hobbies/2/plan?planId=89&week=1');
    await screen.findByRole('heading', { name: '드로잉 · 입문 8주 계획' });
    expect(screen.getAllByRole('tab')).toHaveLength(8);
    expect(screen.getByText('일시중지한 계획이에요. 다시 시작하면 활동을 기록할 수 있어요.')).toBeTruthy();
    expect(screen.queryByRole('button', { name: '내 계획 수정' })).toBeNull();
  });

  it('saves a memo only with the explicit button and sends no status field', async () => {
    const { user } = mount(); await ready();
    await user.click(within(article()).getByRole('button', { name: '메모' }));
    await user.type(screen.getByLabelText('집에서 작은 캔들 만들어보기 활동 메모'), '첫 번째 기록');
    await user.tab(); expect(requests).toHaveLength(0);
    await user.click(within(article()).getByRole('button', { name: '저장' }));
    await screen.findByText('메모를 저장했어요.');
    expect(requests[0].body).toEqual({ memo: '첫 번째 기록' });
    expect(within(article()).queryByRole('textbox')).toBeNull();
    expect(within(article()).getByText('첫 번째 기록')).toBeTruthy();
  });

  it('combines a dirty draft and skip in one PATCH and restores skipped items to TODO', async () => {
    const { user } = mount(); await ready();
    await user.click(within(article()).getByRole('button', { name: '메모' }));
    await user.type(screen.getByLabelText('집에서 작은 캔들 만들어보기 활동 메모'), '다음에 도전');
    await user.click(within(article()).getByRole('button', { name: '건너뛰기' }));
    await screen.findByText('다음에 도전');
    expect(requests[0].body).toEqual({ status: 'SKIPPED', memo: '다음에 도전' });
    await user.click(within(article()).getByRole('button', { name: '다시 하기' }));
    await waitFor(() => expect(requests).toHaveLength(2));
    expect(requests[1].body).toEqual({ status: 'TODO' });
    expect(items.find((item) => item.itemId === 108)?.memo).toBe('다음에 도전');
  });

  it('retains a failed memo, blocks navigation until a decision, and retries save before moving', async () => {
    const { user, router } = mount(); await ready();
    await user.click(within(article()).getByRole('button', { name: '메모' }));
    await user.type(screen.getByLabelText('집에서 작은 캔들 만들어보기 활동 메모'), '보존할 초안');
    await user.click(screen.getByRole('tab', { name: '3주차' }));
    failSave = true;
    await user.click(screen.getByRole('button', { name: '저장 후 이동' }));
    await screen.findByText(/저장하지 못했어요. 작성한 내용은 그대로/);
    expect(router.state.location.search).toContain('week=2');
    await user.click(screen.getByRole('button', { name: '계속 작성' }));
    expect((screen.getByLabelText('집에서 작은 캔들 만들어보기 활동 메모') as HTMLTextAreaElement).value).toBe('보존할 초안');
    failSave = false;
    await user.click(screen.getByRole('tab', { name: '3주차' }));
    await user.click(screen.getByRole('button', { name: '저장 후 이동' }));
    await screen.findByText('미리보기 중이에요. 이 주차가 시작되면 활동을 기록할 수 있어요.');
    expect(router.state.location.search).toContain('week=3');
    expect(items.find((item) => item.itemId === 108)?.memo).toBe('보존할 초안');
    expect(screen.queryByRole('button', { name: '건너뛰기' })).toBeNull();
    expect(screen.getByRole('button', { name: '내 계획 수정' })).toBeTruthy();
  });

  it('deletes an existing memo with null and cancels local edits', async () => {
    const { user } = mount(); await ready();
    const card = () => article('원데이 클래스에서 캔들 만들기');
    await user.click(within(card()).getByRole('button', { name: '메모 수정' }));
    await user.clear(screen.getByLabelText('원데이 클래스에서 캔들 만들기 활동 메모'));
    await user.click(within(card()).getByRole('button', { name: '취소' }));
    expect(requests).toHaveLength(0);
    expect(within(card()).getByText(/은은한 우디 향/)).toBeTruthy();
    await user.click(within(card()).getByRole('button', { name: '메모 수정' }));
    await user.clear(screen.getByLabelText('원데이 클래스에서 캔들 만들기 활동 메모'));
    await user.click(within(card()).getByRole('button', { name: '저장' }));
    await screen.findByText('메모를 저장했어요.');
    expect(requests[0].body).toEqual({ memo: null });
    expect(within(card()).queryByText(/은은한 우디 향/)).toBeNull();
  });

  it('pauses and resumes the same plan and handles the three hobby limit', async () => {
    const { user, router } = mount(); await ready();
    await user.click(screen.getByRole('button', { name: '일시중지' }));
    await screen.findByText('일시중지한 계획이에요. 다시 시작하면 활동을 기록할 수 있어요.');
    expect(router.state.location.search).toContain('planId=88');
    server.use(http.patch('http://localhost/api/v1/users/me/hobbies/1/status', () => failure('MY_HOBBY_LIMIT', 'limit', 409)));
    await user.click(screen.getByRole('button', { name: '다시 시작' }));
    await screen.findByText(/동시에 진행할 수 있는 취미는 최대 3개예요/);
    expect(screen.getByRole('button', { name: '다시 시작' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: '건너뛰기' })).toBeNull();
  });

  it('adds, moves, changes time, and deletes using supported fields and 204 refresh', async () => {
    const { user } = mount(); await ready();
    await user.click(screen.getByRole('button', { name: '내 계획 수정' }));
    const panel = screen.getByRole('dialog', { name: '내 계획 수정' });
    await user.click(within(panel).getByRole('button', { name: '활동 추가' }));
    await user.type(within(panel).getByLabelText(/활동 제목/), '추가한 실습');
    await user.click(within(panel).getByRole('button', { name: '활동 추가' }));
    await screen.findByText('활동을 추가했어요.');
    expect(requests[0].method).toBe('POST');
    const newItem = within(panel).getAllByRole('article').find((entry) => entry.textContent?.includes('추가한 실습'))!;
    await user.selectOptions(within(newItem).getByLabelText('이동할 주차'), '3');
    const minutes = within(newItem).getByLabelText('예상 시간 (분)'); await user.clear(minutes); await user.type(minutes, '45');
    await user.click(within(newItem).getByRole('button', { name: '변경 저장' }));
    await screen.findByText('활동을 저장했어요.');
    expect(requests.find((entry) => entry.method === 'PATCH')?.body).toEqual({ weekNo: 3, expectedMinutes: 45 });
    await user.selectOptions(within(panel).getByLabelText('편집할 주차'), '3');
    await waitFor(() => expect(within(panel).getAllByRole('article').some((entry) => entry.textContent?.includes('추가한 실습'))).toBe(true));
    const moved = within(panel).getAllByRole('article').find((entry) => entry.textContent?.includes('추가한 실습'))!;
    await user.click(within(moved).getByRole('button', { name: '삭제' }));
    await user.click(within(screen.getByRole('dialog', { name: '활동을 삭제할까요?' })).getByRole('button', { name: '삭제' }));
    await screen.findByText('활동을 삭제했어요.');
    expect(requests.at(-1)?.method).toBe('DELETE');
    expect(items.some((item) => item.itemId === 999)).toBe(false);
    expect(client.getQueryData(activityQueryKeys.plan(88, 3))).toBeTruthy();
  });

  it('keeps saved hobbies when unsave fails', async () => {
    const { user } = mount(); await ready();
    await user.click(screen.getByRole('button', { name: '도자기 관심 해제' }));
    await screen.findByText(/관심 해제 실패/);
    expect(screen.getByRole('button', { name: '도자기 관심 해제' })).toBeTruthy();
  });

  it('blocks overlength memos and supports the explicit discard choice', async () => {
    const { user, router } = mount(); await ready();
    await user.click(within(article()).getByRole('button', { name: '메모' }));
    await user.click(screen.getByLabelText('집에서 작은 캔들 만들어보기 활동 메모'));
    await user.paste('가'.repeat(501));
    expect(within(article()).getByRole('button', { name: '저장' }).hasAttribute('disabled')).toBe(true);
    expect(requests).toHaveLength(0);
    await user.click(screen.getByRole('tab', { name: '3주차' }));
    await user.click(screen.getByRole('button', { name: '변경 버리기' }));
    await screen.findByText('미리보기 중이에요. 이 주차가 시작되면 활동을 기록할 수 있어요.');
    expect(router.state.location.search).toContain('week=3');
    expect(requests).toHaveLength(0);
  });

  it('moves keyboard focus with arrows and selects only on Enter', async () => {
    const { user, router } = mount(); await ready();
    const current = screen.getByRole('tab', { name: '2주차 이번 주' });
    current.focus(); await user.keyboard('{ArrowRight}');
    expect(document.activeElement).toBe(screen.getByRole('tab', { name: '3주차' }));
    expect(router.state.location.search).toContain('week=2');
    await user.keyboard('{Enter}');
    await screen.findByText('미리보기 중이에요. 이 주차가 시작되면 활동을 기록할 수 있어요.');
    expect(router.state.location.search).toContain('week=3');
  });

  it('protects memo drafts when pausing or entering the plan editor', async () => {
    const { user } = mount(); await ready();
    await user.click(within(article()).getByRole('button', { name: '메모' }));
    await user.type(screen.getByLabelText('집에서 작은 캔들 만들어보기 활동 메모'), '중단 전 초안');
    await user.click(screen.getByRole('button', { name: '일시중지' }));
    expect(screen.getByRole('dialog', { name: '저장하지 않은 메모가 있어요' })).toBeTruthy();
    await user.click(screen.getByRole('button', { name: '계속 작성' }));
    await user.click(screen.getByRole('button', { name: '내 계획 수정' }));
    await user.click(screen.getByRole('button', { name: '저장 후 이동' }));
    await screen.findByRole('dialog', { name: '내 계획 수정' });
    expect(requests[0].body).toEqual({ memo: '중단 전 초안' });
  });

  it('distinguishes empty hobbies from list errors and missing linked plans', async () => {
    server.use(http.get('http://localhost/api/v1/users/me/hobbies', () => envelope([])));
    mount('/my-hobbies'); await screen.findByText('아직 등록한 취미가 없어요');
    expect(screen.queryByText('계획을 불러오지 못했어요')).toBeNull();
  });

  it('shows retryable plan errors rather than treating them as an empty list', async () => {
    server.use(http.get('http://localhost/api/v1/plans/88', () => failure()));
    mount(); await screen.findByRole('heading', { name: '계획을 불러오지 못했어요' });
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeTruthy();
    expect(screen.queryByText('아직 등록한 취미가 없어요')).toBeNull();
  });

  it('opens the legacy todos URL in the same management screen', async () => {
    const { router } = mount('/my-hobbies/1/todos'); await ready();
    await waitFor(() => expect(router.state.location.pathname).toBe('/my-hobbies/1/plan'));
    expect(router.state.location.search).toBe('?planId=88&week=2');
  });

  it('does not display a plan paired with another hobby ID', async () => {
    mount('/my-hobbies/2/plan?planId=88&week=2');
    await screen.findByRole('heading', { name: '계획 연결을 확인해 주세요' });
    expect(screen.queryByRole('heading', { name: '캔들·디퓨저 · 입문 3주 계획' })).toBeNull();
  });
});
