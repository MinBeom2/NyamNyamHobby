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
  it.each([[3, '요가'], [12, '도자기']])('selects hobby %s from the incoming URL on the first render', (hobbyId, name) => {
    const html = renderPlaces(`/places?hobbyId=${hobbyId}`);
    expect(html).toContain(`<button class="active" type="button">${name}</button>`);
    expect(html).not.toContain('<button class="active" type="button">클라이밍</button>');
  });

  it.each(['/places', '/places?hobbyId=invalid', '/places?hobbyId=9999'])('keeps the general search default for %s', (path) => {
    expect(renderPlaces(path)).toContain('<button class="active" type="button">클라이밍</button>');
  });

  it('falls back to the first available hobby when climbing is unavailable', () => {
    expect(renderPlaces('/places', hobbies.slice(1))).toContain('<button class="active" type="button">요가</button>');
  });

  it('waits for the hobby list before selecting a hobby', () => {
    expect(renderPlaces('/places?hobbyId=3', [])).not.toContain('<button class="active"');
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
