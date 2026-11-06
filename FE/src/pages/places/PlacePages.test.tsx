import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { activityQueryKeys, type PlanViewDto } from '../../features/activity/api/activityApi';
import { placeQueryKeys, type PlaceHobbyDto } from '../../features/place/api/placeApi';
import { userQueryKeys } from '../../features/user/api/userApi';
import { HobbyPlanPage } from '../activities/ActivityPages';
import { PlaceSearchPage } from './PlacePages';

const hobbies: PlaceHobbyDto[] = [
  { id: 1, name: '클라이밍' },
  { id: 3, name: '요가' },
  { id: 12, name: '도자기' },
];

function renderPlaces(path: string, availableHobbies = hobbies) {
  const client = new QueryClient();
  client.setQueryData(placeQueryKeys.hobbies, { content: availableHobbies });
  try {
    return renderToStaticMarkup(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[path]}><PlaceSearchPage /></MemoryRouter>
      </QueryClientProvider>,
    );
  } finally {
    client.clear();
  }
}

describe('place search hobby selection', () => {
  it.each([[1, '클라이밍'], [3, '요가'], [12, '도자기']])('selects hobby %s from the incoming URL on the first render', (hobbyId, name) => {
    const html = renderPlaces(`/places?hobbyId=${hobbyId}`);
    expect(html).toContain(`aria-pressed="true">${name}</button>`);
    expect(html.match(/aria-pressed="true"/g)).toHaveLength(1);
    expect(html).toContain(`· ${name}</h2>`);
  });

  it('asks for a hobby when entering from the general place menu', () => {
    const html = renderPlaces('/places');
    expect(html).not.toContain('aria-pressed="true"');
    expect(html).toContain('장소를 찾을 취미를 선택해 주세요.');
    expect(html).not.toContain('조건에 맞는 장소가 없어요');
  });

  it.each(['invalid', '9999', '', '0', '-1', '1.5'])('does not replace an invalid hobby %s with another hobby', (hobbyId) => {
    const html = renderPlaces(`/places?hobbyId=${hobbyId}`);
    expect(html).not.toContain('aria-pressed="true"');
    expect(html).toContain('선택한 취미를 찾을 수 없어요. 다른 취미를 선택해 주세요.');
  });

  it('does not automatically select the first hobby when climbing is unavailable', () => {
    expect(renderPlaces('/places', hobbies.slice(1))).not.toContain('aria-pressed="true"');
  });

  it('shows an empty hobby list without selecting a different hobby', () => {
    const html = renderPlaces('/places?hobbyId=3', []);
    expect(html).not.toContain('aria-pressed="true"');
    expect(html).toContain('검색할 수 있는 취미가 없어요');
    expect(html).not.toContain('조건에 맞는 장소가 없어요');
  });

  it('waits for the hobby list without showing an invalid hobby or empty search result', () => {
    const client = new QueryClient();
    try {
      const html = renderToStaticMarkup(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={['/places?hobbyId=3']}><PlaceSearchPage /></MemoryRouter>
        </QueryClientProvider>,
      );
      expect(html).toContain('취미 목록을 불러오고 있어요.');
      expect(html).not.toContain('aria-pressed="true"');
      expect(html).not.toContain('선택한 취미를 찾을 수 없어요');
      expect(html).not.toContain('조건에 맞는 장소가 없어요');
    } finally {
      client.clear();
    }
  });
});

describe('my hobby plan place link', () => {
  it.each([3, 12])('passes the current plan hobby %s to place search', (hobbyId) => {
    const client = new QueryClient();
    const progress = { done: 0, skipped: 0, total: 0, rate: 0 };
    const plan: PlanViewDto = {
      planId: 88, userHobbyId: 99, hobbyId, title: '내 취미 계획',
      hobbyStatus: 'IN_PROGRESS', planStatus: 'IN_PROGRESS', skillLevel: 'BEGINNER',
      startedAt: null, totalWeeks: 1, currentWeek: 1, selectedWeek: 1, weekTitle: '첫 주',
      progress, planProgress: progress, weekProgress: progress, weekExpectedMinutes: 0,
      overdueCount: 0, overdueItems: [], skippedItems: [], canEditPlan: true,
      canUpdateItems: true, weekCompleted: false, nextWeek: null, planCompleted: false,
      weeks: [], items: [], supplies: [],
    };
    client.setQueryData(userQueryKeys.hobbies(), []);
    client.setQueryData(activityQueryKeys.plan(88), plan);
    try {
      const html = renderToStaticMarkup(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={[`/my-hobbies/${hobbyId}/plan?planId=88`]}>
            <Routes><Route path="/my-hobbies/:hobbyId/plan" element={<HobbyPlanPage />} /></Routes>
          </MemoryRouter>
        </QueryClientProvider>,
      );
      expect(html).toMatch(new RegExp(`href="/places\\?hobbyId=${hobbyId}"[^>]*>장소 찾기</a>`));
    } finally {
      client.clear();
    }
  });
});
