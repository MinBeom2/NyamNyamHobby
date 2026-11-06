import type { PlanItemDto, PlanProgressDto, PlanViewDto } from '../api/activityApi';
import type { UserHobbyDto } from '../../user/api/userApi';
import { ApiError } from '../../../shared/api/client';
import { HOBBY_THUMBNAIL_MAP } from '../../../shared/assets/hobbyThumbnails';

export function progressView(progress: PlanProgressDto) {
  const percent = progress.total ? Math.round(progress.rate * 100) : 0;
  return {
    ...progress,
    percent: Math.max(0, Math.min(100, percent)),
    doneWidth: progress.total ? progress.done / progress.total * 100 : 0,
    skippedWidth: progress.total ? progress.skipped / progress.total * 100 : 0,
    label: `${progress.total}개 중 ${progress.done}개 완료 · 건너뜀 ${progress.skipped}개`,
  };
}

export function formatMinutes(minutes: number) {
  if (minutes < 60) return `${minutes}분`;
  return `${Math.floor(minutes / 60)}시간${minutes % 60 ? ` ${minutes % 60}분` : ''}`;
}

export function planLink(hobbyId: number, planId: number | null, week?: number) {
  return `/my-hobbies/${hobbyId}/plan${planId ? `?planId=${planId}${week ? `&week=${week}` : ''}` : ''}`;
}

export function hobbyListView(hobbies: UserHobbyDto[]) {
  const map = (hobby: UserHobbyDto) => ({
    id: hobby.hobbyId,
    name: hobby.name,
    thumbnail: HOBBY_THUMBNAIL_MAP[hobby.name] ?? hobby.thumbnailUrl,
    paused: hobby.status === 'PAUSED',
    progress: progressView(hobby.planProgress ?? { done: 0, skipped: 0, total: 0, rate: hobby.progressRate }),
    weekLabel: hobby.currentWeek ? `${hobby.currentWeek}주차${hobby.status === 'PAUSED' ? '에서 멈춤' : ` / ${hobby.totalWeeks ?? '—'}주`}` : '계획 확인하기',
    link: planLink(hobby.hobbyId, hobby.planId),
  });
  const active = hobbies.filter((h) => h.status === 'IN_PROGRESS');
  const paused = hobbies.filter((h) => h.status === 'PAUSED');
  return {
    plans: [...active, ...paused].map(map),
    saved: hobbies.filter((h) => h.status === 'SAVED').map((hobby) => ({ ...map(hobby), link: `/hobbies/${hobby.hobbyId}` })),
    activeCount: active.length,
    pausedCount: paused.length,
  };
}

const itemLabels = { TODO: '진행 전', DONE: '완료', SKIPPED: '건너뜀' } as const;
export function activityView(item: PlanItemDto) {
  return { ...item, statusLabel: itemLabels[item.status], duration: formatMinutes(item.expectedMinutes), suppliesLabel: item.supplies?.trim() || '준비물 없음' };
}

export function planView(plan: PlanViewDto) {
  const selectedWeek = plan.selectedWeek ?? plan.currentWeek;
  const paused = plan.hobbyStatus === 'PAUSED';
  const date = plan.startedAt ? new Date(plan.startedAt) : null;
  const startedLabel = date && !Number.isNaN(date.getTime())
    ? new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit' }).format(date).replaceAll('-', '.')
    : null;
  const progress = progressView(plan.planProgress);
  const weekProgress = progressView(plan.weekProgress ?? { done: 0, skipped: 0, total: 0, rate: 0 });
  const overdue = [...new Set(plan.overdueItems.map((item) => item.weekNo))].sort((a, b) => a - b).map((weekNo) => ({
    weekNo, items: plan.overdueItems.filter((item) => item.weekNo === weekNo).map(activityView),
  }));
  return {
    id: plan.planId, hobbyId: plan.hobbyId, title: plan.title, totalWeeks: plan.totalWeeks,
    currentWeek: plan.currentWeek, selectedWeek, weekTitle: plan.weekTitle ?? `${selectedWeek}주차`,
    paused, startedLabel, statusLabel: paused ? '일시중지' : '진행 중인 취미', progress, weekProgress,
    canEdit: plan.canEditPlan,
    canRecord: plan.canUpdateItems && !paused && selectedWeek <= plan.currentWeek,
    restriction: paused ? '일시중지한 계획이에요. 다시 시작하면 활동을 기록할 수 있어요.'
      : selectedWeek > plan.currentWeek ? '미리보기 중이에요. 이 주차가 시작되면 활동을 기록할 수 있어요.'
      : !plan.canUpdateItems ? '현재 계획 상태에서는 활동을 기록할 수 없어요.' : null,
    duration: formatMinutes(plan.weekExpectedMinutes ?? 0),
    weeks: plan.weeks.map((week) => ({ number: week.weekNo, title: week.title, current: week.weekNo === plan.currentWeek, overdue: week.overdueCount })),
    items: [...plan.items].sort((a, b) => a.seq - b.seq || a.itemId - b.itemId).map(activityView),
    supplies: plan.supplies, overdue, overdueCount: plan.overdueCount,
    completion: plan.weekCompleted ? (weekProgress.skipped
      ? `${selectedWeek}주차 활동을 정리했어요. 완료 ${weekProgress.done}개 · 건너뜀 ${weekProgress.skipped}개`
      : `${selectedWeek}주차 활동을 모두 완료했어요.`) : null,
    planCompletion: plan.planCompleted ? `전체 계획의 활동을 정리했어요. 완료 ${progress.done}개 · 건너뜀 ${progress.skipped}개` : null,
    nextWeek: plan.nextWeek,
  };
}

export type HobbyListView = ReturnType<typeof hobbyListView>;
export type ManagementPlan = ReturnType<typeof planView>;
export type ManagementItem = ReturnType<typeof activityView>;

export function managementError(error: unknown) {
  if (error instanceof ApiError) {
    if (error.code === 'MY_HOBBY_LIMIT') return '동시에 진행할 수 있는 취미는 최대 3개예요';
    if (error.code === 'TODO_FUTURE_WEEK') return '미래 주차에는 아직 기록할 수 없어요. 주차 정보를 새로 확인했어요.';
    if (error.code === 'TODO_PLAN_NOT_ACTIVE') return '계획 상태가 바뀌었어요. 다시 시작한 후 기록해 주세요.';
    if (error.status === 401) return '로그인이 만료되었어요. 다시 로그인해 주세요.';
    if (error.status === 403) return '이 계획을 볼 권한이 없어요.';
    if (error.status === 404) return '계획이나 활동을 찾을 수 없어요.';
    return error.message;
  }
  return '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.';
}

export function positiveInteger(value: string | null) {
  return value !== null && /^\d+$/.test(value) && Number.isSafeInteger(Number(value)) && Number(value) > 0 ? Number(value) : undefined;
}
