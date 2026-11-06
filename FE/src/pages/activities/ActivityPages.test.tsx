import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { activityQueryKeys, type PlanItemDto, type PlanViewDto } from '../../features/activity/api/activityApi';
import { userQueryKeys } from '../../features/user/api/userApi';
import { HobbyPlanPage } from './ActivityPages';

const progress = { done: 0, skipped: 0, total: 1, rate: 0 };

const activity: PlanItemDto = {
  itemId: 101,
  weekNo: 1,
  seq: 1,
  title: '기본 동작 연습하기',
  description: '천천히 자세를 확인해요.',
  expectedMinutes: 35,
  supplies: '편한 옷, 물',
  doneCriteria: '기본 동작을 3번 수행하기',
  status: 'TODO',
  memo: null,
  isCustom: false,
  completedAt: null,
};

function createPlan(overrides: Partial<PlanViewDto> = {}): PlanViewDto {
  return {
    planId: 88,
    userHobbyId: 99,
    hobbyId: 1,
    title: '클라이밍 입문 3주 계획',
    hobbyStatus: 'IN_PROGRESS',
    planStatus: 'ACTIVE',
    skillLevel: 'BEGINNER',
    startedAt: '2026-11-01T09:00:00+09:00',
    totalWeeks: 3,
    currentWeek: 1,
    selectedWeek: 1,
    weekTitle: '기본과 친해지기',
    progress,
    planProgress: progress,
    weekProgress: progress,
    weekExpectedMinutes: 35,
    overdueCount: 0,
    overdueItems: [],
    skippedItems: [],
    canEditPlan: true,
    canUpdateItems: true,
    weekCompleted: false,
    nextWeek: 2,
    planCompleted: false,
    weeks: [{
      weekNo: 1,
      title: '기본과 친해지기',
      status: 'CURRENT',
      current: true,
      progress,
      expectedMinutes: 35,
      overdueCount: 0,
      weekCompleted: false,
      canUpdateItems: true,
      canEditPlan: true,
    }],
    items: [activity],
    supplies: ['편한 옷', '물'],
    ...overrides,
  };
}

function renderPlan(plan: PlanViewDto) {
  const client = new QueryClient();
  client.setQueryData(userQueryKeys.hobbies(), []);
  client.setQueryData(activityQueryKeys.plan(plan.planId), plan);
  try {
    return renderToStaticMarkup(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[`/my-hobbies/${plan.hobbyId}/plan?planId=${plan.planId}`]}>
          <Routes><Route path="/my-hobbies/:hobbyId/plan" element={<HobbyPlanPage />} /></Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
  } finally {
    client.clear();
  }
}

describe('hobby plan execution guidance', () => {
  it('shows the next activity with execution details', () => {
    const html = renderPlan(createPlan());

    expect(html).toContain('지금 할 다음 활동');
    expect(html).toContain('예상 35분');
    expect(html).toContain('준비물 · 편한 옷, 물');
    expect(html).toContain('완료 기준 · 기본 동작을 3번 수행하기');
  });

  it('makes a future week read-only and explains why', () => {
    const html = renderPlan(createPlan({
      selectedWeek: 2,
      weekTitle: '다음 단계',
      canUpdateItems: false,
      items: [{ ...activity, itemId: 201, weekNo: 2 }],
    }));

    expect(html).toContain('2주차는 미리보기만 가능해요');
    expect(html).toContain('disabled=""');
  });

  it('surfaces overdue work as the first return path', () => {
    const overdue = { ...activity, itemId: 77, title: '지난주 안전 수칙 확인', weekNo: 1 };
    const html = renderPlan(createPlan({ currentWeek: 2, selectedWeek: 2, overdueCount: 1, overdueItems: [overdue] }));

    expect(html).toContain('1주차에 남은 활동');
    expect(html).toContain('이전에 남은 활동 1개');
    expect(html).toContain('지난주 안전 수칙 확인');
  });
});
