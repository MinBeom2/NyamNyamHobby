import { create } from 'zustand';

export type PlacePreference = '' | 'INDOOR' | 'OUTDOOR' | 'ANY';
export type SocialPreference = '' | 'ALONE' | 'TOGETHER' | 'ANY';

type OnboardingState = {
  hobbies: string[];
  weeklyHours: string;
  monthlyBudget: string;
  placePref: PlacePreference;
  socialPref: SocialPreference;
  sido: string;
  sigungu: string;
  toggleHobby: (name: string) => void;
  setCondition: (condition: Partial<Omit<OnboardingState, 'toggleHobby' | 'setCondition' | 'resetOnboarding'>>) => void;
  resetOnboarding: () => void;
};

const initialOnboardingState = {
  hobbies: [],
  weeklyHours: '',
  monthlyBudget: '',
  placePref: '' as PlacePreference,
  socialPref: '' as SocialPreference,
  sido: '',
  sigungu: '',
};

export const useOnboardingStore = create<OnboardingState>((set) => ({
  ...initialOnboardingState,
  toggleHobby: (name) => set((state) => ({ hobbies: state.hobbies.includes(name) ? state.hobbies.filter((item) => item !== name) : state.hobbies.length < 5 ? [...state.hobbies, name] : state.hobbies })),
  setCondition: (condition) => set(condition),
  resetOnboarding: () => set(initialOnboardingState),
}));
