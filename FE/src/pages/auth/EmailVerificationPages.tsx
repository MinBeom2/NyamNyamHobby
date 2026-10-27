import { useMutation } from '@tanstack/react-query';
import { Check, Clock3 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resendVerificationEmail, verifyEmail } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/model/authStore';
import { ApiError } from '../../shared/api/client';
import { AppHeader } from '../../shared/ui/AppHeader';
import { SignupSteps } from './SignupPage';

export function EmailSentPage() {
  const [params] = useSearchParams();
  const email = params.get('email') ?? '';
  const [seconds, setSeconds] = useState(60);
  const resendMutation = useMutation({ mutationFn: () => resendVerificationEmail(email), onSuccess: () => setSeconds(60) });

  useEffect(() => {
    if (seconds <= 0) return;
    const timer = window.setTimeout(() => setSeconds((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [seconds]);

  return <StatusPage>
    <SignupSteps active={2} />
    <div className="status-icon"><Check size={24} /></div>
    <h1>인증 메일을 보냈어요</h1>
    <p><strong>{email || '가입한 이메일'}</strong>로 보낸 링크를 24시간 안에 눌러 주세요. 인증이 끝나면 자동으로 로그인되고 관심 취미 설정으로 이동해요.</p>
    <button className="status-action" type="button" disabled={seconds > 0 || resendMutation.isPending || !email} onClick={() => resendMutation.mutate()}>
      {resendMutation.isPending ? '보내는 중…' : `인증 메일 다시 보내기${seconds > 0 ? ` (${seconds}초 후 가능)` : ''}`}
    </button>
    <Link className="status-link" to="/signup">이메일 주소 수정</Link>
    {resendMutation.isError && <p className="status-error">{resendMutation.error instanceof ApiError ? resendMutation.error.message : '메일을 다시 보내지 못했어요.'}</p>}
    <aside className="status-note">링크가 만료됐거나 이미 사용했다면 이 화면에서 다시 받을 수 있어요.</aside>
  </StatusPage>;
}

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const started = useRef(false);
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const mutation = useMutation({
    mutationFn: () => verifyEmail(token ?? ''),
    onSuccess: (data) => { setSession(data); navigate('/onboarding/interests', { replace: true }); },
  });
  useEffect(() => { if (!started.current && token) { started.current = true; mutation.mutate(); } }, [token, mutation]);
  if (!token) return <LinkExpiredPage kind="verification" />;
  if (mutation.isError) return <LinkExpiredPage kind="verification" />;
  return <StatusPage><div className="status-icon status-icon--loading"><Clock3 size={24} /></div><h1>이메일을 인증하고 있어요</h1><p>잠시만 기다려 주세요. 인증이 완료되면 다음 단계로 이동해요.</p></StatusPage>;
}

export function LinkExpiredPage({ kind = 'password' }: { kind?: 'password' | 'verification' }) {
  return <StatusPage>
    <div className="expired-bar">링크 만료 상태</div>
    <h1>링크가 만료되었어요</h1>
    <p>{kind === 'password' ? '재설정 링크는 30분 동안 한 번만 쓸 수 있어요. 새 링크를 받아 주세요.' : '인증 링크가 만료됐거나 이미 사용되었어요. 인증 메일을 다시 받아 주세요.'}</p>
    <Link className="status-action status-action--outline" to={kind === 'password' ? '/password-reset' : '/signup'}>{kind === 'password' ? '재설정 메일 다시 받기' : '인증 메일 다시 받기'}</Link>
  </StatusPage>;
}

function StatusPage({ children }: { children: React.ReactNode }) {
  return <div className="site-shell auth-page"><AppHeader /><main className="auth-page__main"><section className="auth-card auth-card--status">{children}</section></main></div>;
}
