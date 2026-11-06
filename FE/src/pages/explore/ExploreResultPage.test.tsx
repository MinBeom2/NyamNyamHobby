import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { RecommendationResponse } from '../../features/exploration/api/explorationApi';
import { recommendationQueryKeys } from '../../features/recommendation/api/recommendationApi';
import { ExploreResultPage } from './ExploreResultPage';

const startExploration = vi.hoisted(() => vi.fn());
vi.mock('../../features/exploration/api/explorationApi', async (importOriginal) => ({
  ...await importOriginal<typeof import('../../features/exploration/api/explorationApi')>(),
  startExploration,
}));

function item(id: number, name: string, type: 'INTEREST' | 'RELATED', rank: number) {
  return {
    itemId: id * 10, type, rank,
    hobby: { id, name, thumbnailUrl: `/assets/hobbies/${id}.webp`, difficulty: 'EASY',
      sessionMinutes: { min: 30, max: 60 }, monthlyCost: { min: 10000, max: 30000 } },
    reason: `${name} 추천 이유`, reasonStatus: 'COMPLETED', fallbackReason: `${name} 기본 이유`, saved: false, conditionWarning: false,
  };
}

const response: RecommendationResponse = {
  recommendationId: 88, basis: 'EXPLORATION_SESSION', basisText: null, isExploratory: false, reactionCount: 10,
  items: [item(7, '낚시', 'INTEREST', 1), item(9, '자전거', 'INTEREST', 2), item(20, '등산', 'RELATED', 4)],
};

let client: QueryClient;
afterEach(() => client.clear());

function render(path: string) {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  client.setQueryData(recommendationQueryKeys.detail(88), response);
  return renderToStaticMarkup(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/explore/result/:recommendationId" element={<ExploreResultPage />} />
  </Routes></MemoryRouter></QueryClientProvider>);
}

describe('ExploreResultPage', () => {
  it('rebuilds the result screen from the address alone, e.g. after going back from a hobby detail', () => {
    const html = render('/explore/result/88');

    expect(html).toContain('탐색을 마쳤어요!');
    expect(html).toContain('낚시');
    expect(html).toContain('href="/hobbies/7"');
    expect(html).toContain('그 외의 유사한 취미들이에요');
    expect(html).toContain('href="/hobbies/20"');
    expect(startExploration).not.toHaveBeenCalled();
  });

  it('renders nothing of the result for an invalid id (redirects to /explore)', () => {
    expect(render('/explore/result/abc')).not.toContain('탐색을 마쳤어요!');
  });
});
