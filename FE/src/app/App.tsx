import { useEffect } from 'react';
import { scrollToElement, scrollToTop, startSmoothScroll } from '../shared/scroll/smoothScroll';
import { useQuery } from '@tanstack/react-query';
import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { GuestHomePage, MemberHomePage } from '../pages/home/HomePage';
import { LoginPage } from '../pages/auth/LoginPage';
import { LogoutPage } from '../pages/auth/LogoutPage';
import { SignupPage } from '../pages/auth/SignupPage';
import { EmailSentPage, LinkExpiredPage, VerifyEmailPage } from '../pages/auth/EmailVerificationPages';
import { PasswordResetConfirmPage, PasswordResetRequestPage } from '../pages/auth/PasswordResetPages';
import { HobbyInterestPage, UsageConditionPage } from '../pages/onboarding/OnboardingPages';
import { ExplorePage } from '../pages/explore/ExplorePage';
import { ExploreResultPage } from '../pages/explore/ExploreResultPage';
import { InquiryDetailPage, InquiryListPage, InquiryWritePage } from '../pages/support/InquiryPages';
import { NotificationsPage } from '../pages/notifications/NotificationsPage';
import { MyPage } from '../pages/mypage/MyPage';
import { PersonalInfoEditPage } from '../pages/mypage/ProfileEditPages';
import { NoticeDetailPage, NoticeListPage } from '../pages/notices/NoticePages';
import { OAuthCallbackPage } from '../pages/auth/OAuthCallbackPage';
import { SavedShortsPage } from '../pages/mypage/SavedShortsPage';
import { MyInterestsPage } from '../pages/mypage/MyInterestsPage';
import { MyPostsPage } from '../pages/mypage/MyPostsPage';
import { HobbyDetailPage, HobbyInterestSelectPage, HobbyPlanPreviewPage, HobbySearchPage } from '../pages/hobbies/HobbyPages';
import { HobbyPlanPage, MyHobbiesPage, TodoFocusPage } from '../pages/activities/ActivityPages';
import { InfoFeedDetailPage, InfoFeedPage, PlaceSearchPage } from '../pages/places/PlacePages';
import { CommunityDetailPage, CommunityListPage, CommunityWritePage } from '../pages/community/CommunityPages';
import { getCurrentUserProfile } from '../features/auth/api/authApi';
import { useAuthStore } from '../features/auth/model/authStore';
import { getMemberMenu } from '../features/auth/model/accessPolicy';
import { AppHeader } from '../shared/ui/AppHeader';

function AuthSessionBootstrap() {
  const sessionInitialized = useAuthStore((state) => state.sessionInitialized);
  const setUser = useAuthStore((state) => state.setUser);
  const finishSessionCheck = useAuthStore((state) => state.finishSessionCheck);
  const profileQuery = useQuery({
    queryKey: ['auth', 'current-user'],
    queryFn: getCurrentUserProfile,
    enabled: !sessionInitialized,
    retry: false,
  });

  useEffect(() => {
    if (sessionInitialized) return;
    if (profileQuery.isError) {
      finishSessionCheck();
      return;
    }
    if (!profileQuery.isSuccess || profileQuery.fetchStatus !== 'idle' || !profileQuery.data) return;
    setUser({
      id: profileQuery.data.id,
      email: profileQuery.data.email,
      nickname: profileQuery.data.nickname,
      role: profileQuery.data.role,
      onboarded: profileQuery.data.conditions !== null,
    });
  }, [sessionInitialized, profileQuery.data, profileQuery.fetchStatus, profileQuery.isSuccess, profileQuery.isError, setUser, finishSessionCheck]);

  return null;
}

function SessionLoadingPage() {
  return <div className="site-shell"><AppHeader /><main className="page-container" role="status">로그인 상태를 확인하고 있어요.</main></div>;
}

function HomePage() {
  const user = useAuthStore((state) => state.user);
  const initialized = useAuthStore((state) => state.sessionInitialized);
  if (!initialized) return <SessionLoadingPage />;
  return user ? <MemberHomePage /> : <GuestHomePage />;
}

function MemberRoute() {
  const user = useAuthStore((state) => state.user);
  const initialized = useAuthStore((state) => state.sessionInitialized);
  const location = useLocation();
  if (!initialized) return <SessionLoadingPage />;
  if (!user) return <Navigate replace to="/" state={{ signupRequired: getMemberMenu(location.pathname) ?? '회원 전용 기능', returnTo: '/' }} />;
  return <Outlet />;
}

function PreviewMemberRoute() {
  const user = useAuthStore((state) => state.user);
  const initialized = useAuthStore((state) => state.sessionInitialized);
  const location = useLocation();
  const signupRequired =
    typeof location.state === 'object' &&
    location.state !== null &&
    'signupRequired' in location.state &&
    typeof location.state.signupRequired === 'string';

  if (!initialized) return <SessionLoadingPage />;
  if (user || signupRequired) return <Outlet />;

  return (
    <Navigate
      replace
      to="/"
      state={{
        signupRequired: getMemberMenu(location.pathname) ?? '회원 전용 기능',
        returnTo: '/',
      }}
    />
  );
}

function ScrollToTop() {
  const { pathname, hash } = useLocation();

  useEffect(() => startSmoothScroll(), []);

  useEffect(() => {
    if (hash) {
      const frameId = window.requestAnimationFrame(() => {
        scrollToElement(document.getElementById(hash.slice(1)), { immediate: true });
      });

      return () => window.cancelAnimationFrame(frameId);
    }

    scrollToTop({ immediate: true });
  }, [pathname, hash]);

  return null;
}

export function App() {
  return (
    <>
      <AuthSessionBootstrap />
      <ScrollToTop />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/logout" element={<LogoutPage />} />
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
        <Route path="/notices" element={<NoticeListPage />} />
        <Route path="/notices/:noticeId" element={<NoticeDetailPage />} />
        <Route element={<PreviewMemberRoute />}>
          <Route path="/places" element={<PlaceSearchPage />} />
          <Route path="/info-feed" element={<InfoFeedPage />} />
          <Route path="/info-feed/:feedId" element={<InfoFeedDetailPage />} />
        </Route>
        <Route element={<MemberRoute />}>
          <Route path="/home" element={<MemberHomePage />} />
          <Route path="/explore/result/:recommendationId" element={<ExploreResultPage />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/mypage/profile" element={<MyPage profileEditorOpen />} />
          <Route path="/mypage/edit" element={<PersonalInfoEditPage />} />
          <Route path="/mypage/saved-shorts" element={<SavedShortsPage />} />
          <Route path="/mypage/interests" element={<MyInterestsPage />} />
          <Route path="/mypage/posts" element={<MyPostsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/support/inquiries" element={<InquiryListPage />} />
          <Route path="/support/inquiries/new" element={<InquiryWritePage />} />
          <Route path="/support/inquiries/:inquiryId" element={<InquiryDetailPage />} />
          <Route path="/hobbies" element={<HobbySearchPage />} />
          <Route path="/hobbies/interests" element={<HobbyInterestSelectPage />} />
          <Route path="/hobbies/:hobbyId/plan-preview" element={<HobbyPlanPreviewPage />} />
          <Route path="/hobbies/:hobbyId" element={<HobbyDetailPage />} />
          <Route path="/my-hobbies" element={<MyHobbiesPage />} />
          <Route path="/my-hobbies/:hobbyId/plan" element={<HobbyPlanPage />} />
          <Route path="/my-hobbies/:hobbyId/todos" element={<TodoFocusPage />} />
          <Route path="/community" element={<CommunityListPage />} />
          <Route path="/community/new" element={<CommunityWritePage />} />
          <Route path="/community/:postId/edit" element={<CommunityWritePage />} />
          <Route path="/community/:postId" element={<CommunityDetailPage />} />
        </Route>
        <Route path="*" element={<Navigate replace to="/" />} />
      </Routes>
    </>
  );
}
