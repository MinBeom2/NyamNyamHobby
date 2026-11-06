import type { PlanItemDto, PlanViewDto } from '../features/activity/api/activityApi';
import type { UserHobbyDto } from '../features/user/api/userApi';

export function managementItems(): PlanItemDto[] {
  const titles = ['캔들 재료와 안전 수칙 알아보기', '향과 왁스의 종류 비교하기', '기본 도구 준비하기', '나에게 맞는 향 고르기', '원데이 클래스에서 캔들 만들기', '완성한 캔들의 향과 모양 기록하기', '다른 향 조합 살펴보기', '집에서 작은 캔들 만들어보기'];
  return Array.from({ length: 16 }, (_, index) => ({
    itemId: 101 + index, weekNo: index < 4 ? 1 : index < 8 ? 2 : 3, seq: index % 4 + 1,
    title: titles[index] ?? `나만의 캔들 응용 활동 ${index - 7}`,
    description: index === 4 ? '좋아하는 향을 고르고, 강사와 함께 나만의 첫 캔들을 만들어 보세요.' : '차근차근 익히며 나에게 맞는 취미의 즐거움을 찾아보세요.',
    expectedMinutes: index === 4 ? 90 : 15,
    supplies: index === 4 ? '예약한 클래스, 앞치마' : '필기 도구, 재료 안내서',
    doneCriteria: index === 4 ? '원데이 클래스에 참여하고 캔들 1개 완성하기' : '활동을 한 번 수행하고 알아본 내용 정리하기',
    status: index < 3 || index === 4 || index === 5 ? 'DONE' : index === 6 ? 'SKIPPED' : 'TODO',
    memo: index === 4 ? '은은한 우디 향이 마음에 들었다. 다음에는 심지 길이를 조금 더 짧게 잘라보기.' : null,
    isCustom: false, completedAt: null,
  }));
}

export function managementPlan(week = 2, items = managementItems(), overrides: Partial<PlanViewDto> = {}): PlanViewDto {
  const progress = (entries: PlanItemDto[]) => {
    const done = entries.filter((item) => item.status === 'DONE').length;
    return { done, skipped: entries.filter((item) => item.status === 'SKIPPED').length, total: entries.length, rate: entries.length ? done / entries.length : 0 };
  };
  const weekItems = items.filter((item) => item.weekNo === week);
  const planProgress = progress(items);
  const weekProgress = progress(weekItems);
  const weekTitles = ['재료와 기본 순서 익히기', '나의 첫 캔들 만들기', '취향을 담아 응용하기'];
  return {
    planId: 88, userHobbyId: 99, hobbyId: 1, title: '캔들·디퓨저 · 입문 3주 계획',
    hobbyStatus: 'IN_PROGRESS', planStatus: 'ACTIVE', skillLevel: 'BEGINNER', startedAt: '2026-10-28T15:00:00Z',
    totalWeeks: 3, currentWeek: 2, selectedWeek: week, weekTitle: weekTitles[week - 1],
    progress: planProgress, planProgress, weekProgress, weekExpectedMinutes: weekItems.reduce((sum, item) => sum + item.expectedMinutes, 0),
    overdueCount: items.filter((item) => item.weekNo < 2 && item.status === 'TODO').length,
    overdueItems: items.filter((item) => item.weekNo < 2 && item.status === 'TODO'), skippedItems: items.filter((item) => item.status === 'SKIPPED'),
    canEditPlan: true, canUpdateItems: week <= 2, weekCompleted: weekItems.length > 0 && weekItems.every((item) => item.status !== 'TODO'),
    nextWeek: week < 3 ? week + 1 : null, planCompleted: items.length > 0 && items.every((item) => item.status !== 'TODO'),
    weeks: weekTitles.map((title, index) => {
      const entries = items.filter((item) => item.weekNo === index + 1);
      return { weekNo: index + 1, title, status: index === 0 ? 'PAST' : index === 1 ? 'CURRENT' : 'FUTURE', current: index === 1, progress: progress(entries), expectedMinutes: entries.reduce((sum, item) => sum + item.expectedMinutes, 0), overdueCount: index === 0 ? entries.filter((item) => item.status === 'TODO').length : 0, weekCompleted: entries.length > 0 && entries.every((item) => item.status !== 'TODO'), canEditPlan: true, canUpdateItems: index < 2 };
    }),
    items: weekItems, supplies: ['예약한 클래스', '앞치마', '필기 도구', '재료 안내서'], ...overrides,
  };
}

export function managementHobbies(): UserHobbyDto[] {
  const plan = managementPlan();
  const hobby: UserHobbyDto = {
    userHobbyId: 99, hobbyId: 1, name: '캔들·디퓨저', thumbnailUrl: null, status: 'IN_PROGRESS', skillLevel: 'BEGINNER',
    planId: 88, progressRate: plan.planProgress.rate, startedAt: plan.startedAt, title: plan.title, totalWeeks: 3, currentWeek: 2,
    weekTitle: plan.weekTitle, planProgress: plan.planProgress, weekProgress: plan.weekProgress, remainingThisWeekCount: 1,
    overdueCount: 1, pausedAt: null, availableSkillLevels: ['BEGINNER'], planReady: true,
  };
  return [hobby, { ...hobby, userHobbyId: 100, hobbyId: 2, planId: 89, name: '드로잉', status: 'PAUSED', title: '드로잉 · 입문 8주 계획', totalWeeks: 8, currentWeek: 1 }, { ...hobby, userHobbyId: 101, hobbyId: 3, planId: null, name: '도자기', status: 'SAVED', startedAt: null, planProgress: null, progressRate: 0 }, { ...hobby, userHobbyId: 102, hobbyId: 4, planId: null, name: '사진 촬영', status: 'SAVED', startedAt: null, planProgress: null, progressRate: 0 }];
}
