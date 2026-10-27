import { Navigate, Route, Routes } from 'react-router-dom';
import { GuestHomePage, MemberHomePage } from '../pages/home/HomePage';
import { LoginPage } from '../pages/auth/LoginPage';
import { SignupPage } from '../pages/auth/SignupPage';
import { EmailSentPage, LinkExpiredPage, VerifyEmailPage } from '../pages/auth/EmailVerificationPages';
import { PasswordResetConfirmPage, PasswordResetRequestPage } from '../pages/auth/PasswordResetPages';
import { HobbyInterestPage, UsageConditionPage } from '../pages/onboarding/OnboardingPages';
import { ExplorePage } from '../pages/explore/ExplorePage';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<GuestHomePage />} />
      <Route path="/login" element={<LoginPage />} />
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
      <Route path="*" element={<Navigate replace to="/" />} />
    </Routes>
  );
}
