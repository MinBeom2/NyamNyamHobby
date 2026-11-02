import { apiRequest } from '../../../shared/api/client';

export type PlacePreference = 'INDOOR' | 'OUTDOOR' | 'ANY';
export type SocialPreference = 'ALONE' | 'TOGETHER' | 'ANY';
export type UserHobbyStatus = 'SAVED' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'STOPPED';

export type RegionDto = {
  id: number;
  code: string;
  sido: string;
  sigungu: string;
};

export type UserConditionsDto = {
  weeklyHours: number | null;
  monthlyBudget: number | null;
  placePref: PlacePreference | null;
  socialPref: SocialPreference | null;
  region: RegionDto | null;
};

export type UserProfileDto = {
  id: number;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  role: 'USER' | 'ADMIN';
  hasPassword: boolean;
  conditions: UserConditionsDto | null;
};

export type OnboardingOptionsDto = {
  hobbies: Array<{
    hobbyId: number;
    name: string;
    categoryId: number;
    categoryName: string;
  }>;
  regions: RegionDto[];
};

export type ConditionsRequest = {
  weeklyHours: number | null;
  monthlyBudget: number | null;
  placePref: PlacePreference | null;
  socialPref: SocialPreference | null;
  regionId: number | null;
};

export type OnboardingRequest = ConditionsRequest & {
  initialHobbies: Array<{ hobbyId: number }>;
};

export type UserHobbyDto = {
  userHobbyId: number;
  hobbyId: number;
  name: string;
  thumbnailUrl: string | null;
  status: UserHobbyStatus;
  skillLevel: string;
  planId: number | null;
  progressRate: number;
  startedAt: string | null;
  title: string | null;
  totalWeeks: number | null;
  currentWeek: number | null;
  weekTitle: string | null;
  planProgress: { done: number; skipped: number; total: number; rate: number } | null;
  weekProgress: { done: number; skipped: number; total: number; rate: number } | null;
  remainingThisWeekCount: number;
  overdueCount: number;
  pausedAt: string | null;
  availableSkillLevels: string[];
  planReady: boolean;
};

export const userQueryKeys = {
  all: ['user'] as const,
  profile: () => [...userQueryKeys.all, 'profile'] as const,
  onboardingOptions: () => [...userQueryKeys.all, 'onboarding-options'] as const,
  hobbies: (status?: UserHobbyStatus) => [...userQueryKeys.all, 'hobbies', status ?? 'ALL'] as const,
};

export function getUserProfile() {
  return apiRequest<UserProfileDto>('/users/me');
}

export function updateUserProfile(request: { nickname?: string; profileImageUrl?: string | null }) {
  return apiRequest<UserProfileDto>('/users/me', {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

export function getOnboardingOptions() {
  return apiRequest<OnboardingOptionsDto>('/users/me/onboarding/options');
}

export function saveOnboarding(request: OnboardingRequest) {
  return apiRequest<{ onboarded: boolean; savedHobbyCount: number }>('/users/me/onboarding', {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export function updateConditions(request: ConditionsRequest) {
  return apiRequest<UserConditionsDto>('/users/me/conditions', {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

export function changePassword(request: { currentPassword: string; newPassword: string }) {
  return apiRequest<{ message: string }>('/users/me/password', {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

export function deleteCurrentUser(password?: string) {
  return apiRequest<void>('/users/me', {
    method: 'DELETE',
    body: JSON.stringify(password ? { password } : {}),
  });
}

export function getUserHobbies(status?: UserHobbyStatus) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return apiRequest<UserHobbyDto[]>(`/users/me/hobbies${query}`);
}

export function saveUserHobby(hobbyId: number) {
  return apiRequest<{ saved: boolean }>(`/users/me/hobbies/${hobbyId}/save`, { method: 'PUT' });
}

export function deleteUserHobby(hobbyId: number) {
  return apiRequest<{ saved: boolean }>(`/users/me/hobbies/${hobbyId}/save`, { method: 'DELETE' });
}
