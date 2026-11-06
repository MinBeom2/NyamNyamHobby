import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { hobbyQueryKeys, type HobbyDetailDto } from '../../features/hobby/api/hobbyApi';
import { HobbyDetailPage } from './HobbyPages';

// Render the content/place tab with the existing server-rendering test setup.
vi.mock('react', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react')>();
  return {
    ...actual,
    useState: (initialState: unknown) => actual.useState(initialState === 'hobby-overview' ? 'hobby-content' : initialState),
  };
});

describe('hobby detail nearby place link', () => {
  it.each([[3, '요가'], [12, '도자기']])('passes the displayed hobby %s to place search', (id, name) => {
    const client = new QueryClient();
    const detail: HobbyDetailDto = {
      id, name, category: { id: 1, name: '취미' }, summary: '소개', description: '상세 소개',
      thumbnailUrl: null, difficulty: 'EASY', placeType: 'INDOOR', socialType: 'BOTH',
      sessionMinutes: { min: 60, max: 60 }, monthlyCost: { min: 0, max: 0 },
      initialCost: { min: 0, max: 0 }, saveCount: 0, tags: [], supplies: '',
      environment: '', firstActivity: '', cautions: '', myStatus: 'NONE',
    };
    client.setQueryData(hobbyQueryKeys.detail(id), detail);
    client.setQueryData(hobbyQueryKeys.shorts(id, 3), []);
    try {
      const html = renderToStaticMarkup(
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={[`/hobbies/${id}`]}>
            <Routes><Route path="/hobbies/:hobbyId" element={<HobbyDetailPage />} /></Routes>
          </MemoryRouter>
        </QueryClientProvider>,
      );
      expect(html).toContain(`${name} 상세 가이드`);
      expect(html).toMatch(new RegExp(`href="/places\\?hobbyId=${id}"[^>]*>주변 장소</a>`));
      expect(html).toContain('관련 숏폼');
      expect(html).toMatch(/href="\/explore"[^>]*>전체 숏폼<\/a>/);
    } finally {
      client.clear();
    }
  });
});
