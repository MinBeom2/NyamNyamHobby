import { apiRequest } from '../../../shared/api/client';
import type { UserHobbyDto } from '../../user/api/userApi';

export type PlanItemStatus = 'TODO' | 'DONE' | 'SKIPPED';

export type PlanProgressDto = {
  done: number;
  skipped: number;
  total: number;
  rate: number;
};

export type PlanWeekDto = {
  weekNo: number;
  title: string;
  status: 'PAST' | 'CURRENT' | 'FUTURE';
  current: boolean;
  progress: PlanProgressDto;
  expectedMinutes: number;
  overdueCount: number;
  weekCompleted: boolean;
  canUpdateItems: boolean;
  canEditPlan: boolean;
};

export type PlanItemDto = {
  itemId: number;
  weekNo: number;
  seq: number;
  title: string;
  description: string | null;
  expectedMinutes: number;
  supplies: string | null;
  doneCriteria: string | null;
  status: PlanItemStatus;
  memo: string | null;
  isCustom: boolean;
  completedAt: string | null;
};

export type PlanViewDto = {
  planId: number;
  userHobbyId: number;
  hobbyId: number;
  title: string;
  hobbyStatus: string;
  planStatus: string;
  skillLevel: string;
  startedAt: string | null;
  totalWeeks: number;
  currentWeek: number;
  selectedWeek: number | null;
  weekTitle: string | null;
  progress: PlanProgressDto;
  planProgress: PlanProgressDto;
  weekProgress: PlanProgressDto | null;
  weekExpectedMinutes: number | null;
  overdueCount: number;
  overdueItems: PlanItemDto[];
  skippedItems: PlanItemDto[];
  canEditPlan: boolean;
  canUpdateItems: boolean;
  weekCompleted: boolean;
  nextWeek: number | null;
  planCompleted: boolean;
  weeks: PlanWeekDto[];
  items: PlanItemDto[];
  supplies: string[];
};

export type UpdatePlanItemRequest = {
  status?: PlanItemStatus;
  memo?: string | null;
  weekNo?: number;
  expectedMinutes?: number;
};

export type CreatePlanItemRequest = {
  weekNo: number;
  title: string;
  description?: string | null;
  expectedMinutes: number;
  supplies?: string | null;
  doneCriteria?: string | null;
};

export type UpdatePlanItemResponse = {
  itemId: number;
  status: PlanItemStatus;
  memo: string | null;
  completedAt: string | null;
  item: PlanItemDto;
  progress: PlanProgressDto;
  planProgress: PlanProgressDto;
  weekProgress: PlanProgressDto | null;
  weekCompleted: boolean;
  nextWeek: number | null;
  planCompleted: boolean;
};

export type UpdateUserHobbyStatusResponse = {
  userHobbyId: number;
  planId: number;
  status: 'PAUSED' | 'IN_PROGRESS' | 'STOPPED';
  currentWeek: number;
  startedAt: string | null;
  pausedAt: string | null;
};

export type TodoSummaryDto = {
  remainingThisWeekCount: number;
  overdueCount: number;
  inProgressCount: number;
  maxInProgressCount: number;
  statusCounts: Record<string, number>;
  plans: UserHobbyDto[];
};

export const activityQueryKeys = {
  all: ['activity'] as const,
  summary: () => [...activityQueryKeys.all, 'summary'] as const,
  plan: (planId: number, week?: number) => [...activityQueryKeys.all, 'plan', planId, week ?? 'current'] as const,
  overview: (planId: number) => [...activityQueryKeys.all, 'overview', planId] as const,
};

export function getTodoSummary() {
  return apiRequest<TodoSummaryDto>('/users/me/todos/summary');
}

export function getPlan(planId: number, week?: number) {
  const query = week ? `?week=${week}` : '';
  return apiRequest<PlanViewDto>(`/plans/${planId}${query}`);
}

export function getPlanOverview(planId: number) {
  return apiRequest<PlanViewDto>(`/plans/${planId}/overview`);
}

export function updatePlanItem(planId: number, itemId: number, request: UpdatePlanItemRequest) {
  return apiRequest<UpdatePlanItemResponse>(`/plans/${planId}/items/${itemId}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

export function createPlanItem(planId: number, request: CreatePlanItemRequest) {
  return apiRequest<UpdatePlanItemResponse>(`/plans/${planId}/items`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function deletePlanItem(planId: number, itemId: number) {
  return apiRequest<void>(`/plans/${planId}/items/${itemId}`, {
    method: 'DELETE',
  });
}

export function updateUserHobbyStatus(hobbyId: number, status: 'PAUSED' | 'IN_PROGRESS' | 'STOPPED') {
  return apiRequest<UpdateUserHobbyStatusResponse>(`/users/me/hobbies/${hobbyId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}
