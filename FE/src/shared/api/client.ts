import { useAuthStore } from '../../features/auth/model/authStore';
import type { ApiEnvelope, ApiErrorBody } from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fields: ApiErrorBody['fields'];
  readonly retryAfterSeconds: number | null;

  constructor(status: number, error: ApiErrorBody) {
    super(error.message);
    this.name = 'ApiError';
    this.status = status;
    this.code = error.code;
    this.fields = error.fields;
    this.retryAfterSeconds = error.retryAfterSeconds;
  }
}

function buildHeaders(init?: RequestInit, accessToken?: string | null) {
  const headers = new Headers(init?.headers);
  if (init?.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  return headers;
}

async function readEnvelope<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  if (response.status === 204) return null;
  const contentType = response.headers.get('content-type');
  if (!contentType?.includes('application/json')) return null;
  return response.json() as Promise<ApiEnvelope<T>>;
}

function fallbackError(status: number): ApiErrorBody {
  return {
    code: 'COMMON_NETWORK_ERROR',
    message: status >= 500 ? '서버에 문제가 발생했어요. 잠시 후 다시 시도해 주세요.' : '요청을 처리하지 못했어요.',
    fields: null,
    retryAfterSeconds: null,
  };
}

async function refreshAccessToken() {
  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    credentials: 'include',
  });
  const envelope = await readEnvelope<{ accessToken: string; expiresIn: number }>(response);

  if (!response.ok || !envelope?.success) {
    useAuthStore.getState().clearSession();
    throw new ApiError(response.status, envelope?.error ?? fallbackError(response.status));
  }

  useAuthStore.getState().setAccessToken(envelope.data.accessToken, envelope.data.expiresIn);
}

export async function apiRequest<T>(path: string, init: RequestInit = {}, retryOnExpiredToken = true): Promise<T> {
  const accessToken = useAuthStore.getState().accessToken;

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: buildHeaders(init, accessToken),
      credentials: 'include',
    });
  } catch {
    throw new ApiError(0, {
      code: 'COMMON_NETWORK_ERROR',
      message: '서버에 연결할 수 없어요. BE 서버 실행 상태를 확인해 주세요.',
      fields: null,
      retryAfterSeconds: null,
    });
  }

  const envelope = await readEnvelope<T>(response);

  if (response.ok && response.status === 204) {
    return undefined as T;
  }

  if (
    retryOnExpiredToken &&
    response.status === 401 &&
    envelope?.success === false &&
    (envelope.error.code === 'AUTH_TOKEN_EXPIRED' || envelope.error.code === 'COMMON_UNAUTHORIZED')
  ) {
    await refreshAccessToken();
    return apiRequest<T>(path, init, false);
  }

  if (!response.ok || !envelope?.success) {
    throw new ApiError(response.status, envelope?.error ?? fallbackError(response.status));
  }

  return envelope.data;
}
