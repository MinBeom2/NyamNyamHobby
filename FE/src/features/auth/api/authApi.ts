import { apiRequest } from '../../../shared/api/client';
import type { AuthUser } from '../model/authStore';

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  expiresIn: number;
  user: AuthUser;
};

export type SignupRequest = {
  email: string;
  password: string;
  nickname: string;
};

export type SignupResponse = {
  userId: number;
  email: string;
  nickname: string;
  status: 'PENDING';
  message: string;
};

export type AvailabilityResponse = {
  available: boolean;
};

export type MessageResponse = { message: string };

export type OAuthSignupRequest = {
  signupToken: string;
  nickname: string;
};

export type CurrentUserProfile = {
  id: number;
  email: string;
  nickname: string;
  profileImageUrl: string | null;
  role: 'USER' | 'ADMIN';
  conditions: {
    weeklyHours: number | null;
    monthlyBudget: number | null;
    placePref: 'INDOOR' | 'OUTDOOR' | 'ANY' | null;
    socialPref: 'ALONE' | 'TOGETHER' | 'ANY' | null;
    region: {
      id: number;
      code: string;
      sido: string;
      sigungu: string;
    } | null;
  } | null;
};

export function signup(request: SignupRequest) {
  return apiRequest<SignupResponse>('/auth/signup', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function checkEmail(email: string) {
  return apiRequest<AvailabilityResponse>(`/auth/check-email?email=${encodeURIComponent(email)}`);
}

export function checkNickname(nickname: string) {
  return apiRequest<AvailabilityResponse>(`/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`);
}

export function verifyEmail(token: string) {
  return apiRequest<LoginResponse>('/auth/email/verify', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export function resendVerificationEmail(email: string) {
  return apiRequest<void>('/auth/email/resend', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export function requestPasswordReset(email: string) {
  return apiRequest<MessageResponse>('/auth/password/reset-request', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export function resetPassword(token: string, newPassword: string) {
  return apiRequest<MessageResponse>('/auth/password/reset', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  });
}

export function login(request: LoginRequest) {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function getCurrentUserProfile() {
  return apiRequest<CurrentUserProfile>('/users/me');
}

export function oauthSignup(request: OAuthSignupRequest) {
  return apiRequest<LoginResponse>('/auth/oauth/signup', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function logout() {
  return apiRequest<void>('/auth/logout', { method: 'POST' });
}
