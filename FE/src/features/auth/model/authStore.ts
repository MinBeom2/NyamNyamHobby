import { create } from 'zustand';

export type AuthUser = {
  id: number;
  email: string;
  nickname: string;
  role: 'USER' | 'ADMIN';
  onboarded: boolean;
};

type AuthState = {
  sessionInitialized: boolean;
  sessionRevision: number;
  accessToken: string | null;
  expiresAt: number | null;
  user: AuthUser | null;
  setSession: (session: { accessToken: string; expiresIn: number; user: AuthUser }) => void;
  setAccessToken: (accessToken: string, expiresIn: number) => void;
  setUser: (user: AuthUser) => void;
  clearSession: () => void;
  finishSessionCheck: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  sessionInitialized: false,
  sessionRevision: 0,
  accessToken: null,
  expiresAt: null,
  user: null,
  setSession: ({ accessToken, expiresIn, user }) =>
    set((state) => ({
      accessToken, expiresAt: Date.now() + expiresIn * 1000, user,
      sessionInitialized: true, sessionRevision: state.sessionRevision + 1,
    })),
  setAccessToken: (accessToken, expiresIn) =>
    set({ accessToken, expiresAt: Date.now() + expiresIn * 1000 }),
  setUser: (user) => set({ user, sessionInitialized: true }),
  clearSession: () => set((state) => ({
    accessToken: null, expiresAt: null, user: null,
    sessionInitialized: true, sessionRevision: state.sessionRevision + 1,
  })),
  finishSessionCheck: () => set({ sessionInitialized: true }),
}));
