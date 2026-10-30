import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { GuestHomePage, MemberHomePage } from '../pages/home/HomePage';
import { LoginPage } from '../pages/auth/LoginPage';
import { SignupPage } from '../pages/auth/SignupPage';
import { EmailSentPage, LinkExpiredPage, VerifyEmailPage } from '../pages/auth/EmailVerificationPages';
import { PasswordResetConfirmPage, PasswordResetRequestPage } from '../pages/auth/PasswordResetPages';
import { HobbyInterestPage, UsageConditionPage } from '../pages/onboarding/OnboardingPages';
import { ExplorePage } from '../pages/explore/ExplorePage';
import { InquiryDetailPage, InquiryListPage, InquiryWritePage } from '../pages/support/InquiryPages';
import { NotificationsPage } from '../pages/notifications/NotificationsPage';
import { MyPage } from '../pages/mypage/MyPage';
import { PersonalInfoEditPage } from '../pages/mypage/ProfileEditPages';
import { NoticeDetailPage, NoticeListPage } from '../pages/notices/NoticePages';
import { OAuthCallbackPage } from '../pages/auth/OAuthCallbackPage';
import { SavedShortsPage } from '../pages/mypage/SavedShortsPage';
import { HobbyDetailPage, HobbyInterestSelectPage, HobbySearchPage } from '../pages/hobbies/HobbyPages';
import { HobbyPlanPage, MyHobbiesPage, TodoFocusPage } from '../pages/activities/ActivityPages';
import { InfoFeedPage, PlaceSearchPage } from '../pages/places/PlacePages';
import { CommunityDetailPage, CommunityListPage, CommunityWritePage } from '../pages/community/CommunityPages';
import { getCurrentUserProfile } from '../features/auth/api/authApi';
import { useAuthStore } from '../features/auth/model/authStore';

function AuthSessionBootstrap() {
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const profileQuery = useQuery({
    queryKey: ['auth', 'current-user'],
    queryFn: getCurrentUserProfile,
    enabled: user === null,
    retry: false,
  });

  useEffect(() => {
    if (!profileQuery.data) return;
    setUser({
      id: profileQuery.data.id,
      email: profileQuery.data.email,
      nickname: profileQuery.data.nickname,
      role: profileQuery.data.role,
      onboarded: profileQuery.data.conditions !== null,
    });
  }, [profileQuery.data, setUser]);

  return null;
}

function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }, [pathname]);

  return null;
}

export function App() {
  return (
    <>
      <AuthSessionBootstrap />
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<GuestHomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/signup/email-sent" element={<EmailSentPage />} />
        <Route path="/auth/email/verify" element={<VerifyEmailPage />} />
        <Route path="/password-reset" element={<PasswordResetRequestPage />} />
        <Route path="/password-reset/new" element={<PasswordResetConfirmPage />} />
        <Route path="/auth/password/reset" element={<PasswordResetConfirmPage />} />
        <Route path="/password-reset/expired" element={<LinkExpiredPage />} />
        <Route path="/onboarding/interests" element={<HobbyInterestPage />} />
        <Route path="/onboarding/conditions" element={<UsageConditionPage />} />
        <Route path="/explore" element={<ExplorePage />} />
        <Route path="/home" element={<MemberHomePage />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/mypage/profile" element={<MyPage profileEditorOpen />} />
        <Route path="/mypage/edit" element={<PersonalInfoEditPage />} />
        <Route path="/mypage/saved-shorts" element={<SavedShortsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/support/inquiries" element={<InquiryListPage />} />
        <Route path="/support/inquiries/new" element={<InquiryWritePage />} />
        <Route path="/support/inquiries/:inquiryId" element={<InquiryDetailPage />} />
        <Route path="/notices" element={<NoticeListPage />} />
        <Route path="/notices/:noticeId" element={<NoticeDetailPage />} />
        <Route path="/hobbies" element={<HobbySearchPage />} />
        <Route path="/hobbies/interests" element={<HobbyInterestSelectPage />} />
        <Route path="/hobbies/recommendations" element={<HobbySearchPage recommended />} />
        <Route path="/hobbies/:hobbyId" element={<HobbyDetailPage />} />
        <Route path="/my-hobbies" element={<MyHobbiesPage />} />
        <Route path="/my-hobbies/:hobbyId/plan" element={<HobbyPlanPage />} />
        <Route path="/my-hobbies/:hobbyId/todos" element={<TodoFocusPage />} />
        <Route path="/places" element={<PlaceSearchPage />} />
        <Route path="/info-feed" element={<InfoFeedPage />} />
        <Route path="/community" element={<CommunityListPage />} />
        <Route path="/community/new" element={<CommunityWritePage />} />
        <Route path="/community/:postId/edit" element={<CommunityWritePage />} />
        <Route path="/community/:postId" element={<CommunityDetailPage />} />
        <Route path="*" element={<Navigate replace to="/" />} />
      </Routes>
    </>
  );
}
