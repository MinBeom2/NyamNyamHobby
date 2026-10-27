import { create } from 'zustand';

export type AuthUser = {
  id: number;
  email: string;
  nickname: string;
  role: 'USER' | 'ADMIN';
  onboarded: boolean;
};

type AuthState = {
  accessToken: string | null;
  expiresAt: number | null;
  user: AuthUser | null;
  setSession: (session: { accessToken: string; expiresIn: number; user: AuthUser }) => void;
  setAccessToken: (accessToken: string, expiresIn: number) => void;
  clearSession: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  expiresAt: null,
  user: null,
  setSession: ({ accessToken, expiresIn, user }) =>
    set({ accessToken, expiresAt: Date.now() + expiresIn * 1000, user }),
  setAccessToken: (accessToken, expiresIn) =>
    set({ accessToken, expiresAt: Date.now() + expiresIn * 1000 }),
  clearSession: () => set({ accessToken: null, expiresAt: null, user: null }),
}));
