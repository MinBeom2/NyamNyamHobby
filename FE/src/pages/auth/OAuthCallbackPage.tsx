import { useMutation } from '@tanstack/react-query';
import { AlertCircle, CheckCircle, Clock3 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { checkNickname, oauthSignup } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/model/authStore';
import { ApiError } from '../../shared/api/client';
import { AppHeader } from '../../shared/ui/AppHeader';

export function OAuthCallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);

  const status = params.get('status');
  const accessToken = params.get('accessToken');
  const userId = params.get('userId');
  const email = params.get('email') ?? '';
  const nickname = params.get('nickname') ?? '';
  const roleParam = params.get('role');
  const role: 'USER' | 'ADMIN' = roleParam === 'ADMIN' ? 'ADMIN' : 'USER';
  const onboarded = params.get('onboarded') === 'true';

  const signupToken = params.get('signupToken');
  const defaultNickname = params.get('defaultNickname') ?? '';
  const message = params.get('message');

  const processed = useRef(false);

  // 1) 로그인 성공 (기존 소셜 회원)
  useEffect(() => {
    if (status === 'SUCCESS' && accessToken && !processed.current) {
      processed.current = true;
      setSession({
        accessToken,
        expiresIn: 1800,
        user: {
          id: userId ? Number(userId) : 0,
          email,
          nickname,
          role,
          onboarded,
        },
      });


      if (onboarded) {
        navigate('/home', { replace: true });
      } else {
        navigate('/onboarding/interests', { replace: true });
      }
    }
  }, [status, accessToken, userId, email, nickname, role, onboarded, navigate, setSession]);

  // 2) 신규 소셜 회원 닉네임 설정 폼
  if (status === 'SIGNUP_REQUIRED' && signupToken) {
    return (
      <OAuthSignupForm
        signupToken={signupToken}
        email={email}
        defaultNickname={defaultNickname}
      />
    );
  }

  // 3) 동일 이메일 일반 계정 존재 (충돌)
  if (status === 'EMAIL_EXISTS') {
    return (
      <div className="site-shell auth-page">
        <AppHeader />
        <main className="auth-page__main">
          <section className="auth-card" aria-labelledby="oauth-conflict-title">
            <div className="status-icon status-icon--warning" style={{ color: 'var(--color-primary-orange, #ea580c)' }}>
              <AlertCircle size={32} />
            </div>
            <h1 id="oauth-conflict-title">이미 가입된 이메일이에요</h1>
            <p style={{ marginTop: '0.75rem', color: 'var(--color-text-secondary, #666)' }}>
              <strong>{email}</strong> 주소로 이미 가입된 일반 계정이 있어요.
              보안을 위해 기존 이메일과 비밀번호로 로그인해 주세요.
            </p>
            <div style={{ marginTop: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <Link className="auth-form__submit" style={{ textAlign: 'center', textDecoration: 'none' }} to="/login">
                이메일로 로그인하기
              </Link>
              <Link className="status-link" style={{ textAlign: 'center' }} to="/password-reset">
                비밀번호를 잊으셨나요?
              </Link>
            </div>
          </section>
        </main>
      </div>
    );
  }

  // 4) 로그인 실패
  if (status === 'FAILED') {
    return (
      <div className="site-shell auth-page">
        <AppHeader />
        <main className="auth-page__main">
          <section className="auth-card" aria-labelledby="oauth-failed-title">
            <div className="status-icon" style={{ color: 'var(--color-danger, #ef4444)' }}>
              <AlertCircle size={32} />
            </div>
            <h1 id="oauth-failed-title">로그인에 실패했어요</h1>
            <p style={{ marginTop: '0.75rem', color: 'var(--color-text-secondary, #666)' }}>
              {message || 'Google 로그인 처리 중 문제가 발생했어요. 다시 시도해 주세요.'}
            </p>
            <div style={{ marginTop: '1.5rem' }}>
              <Link className="auth-form__submit" style={{ textAlign: 'center', textDecoration: 'none' }} to="/login">
                로그인 화면으로 돌아가기
              </Link>
            </div>
          </section>
        </main>
      </div>
    );
  }

  // 로딩 중 (SUCCESS 처리 대기)
  return (
    <div className="site-shell auth-page">
      <AppHeader />
      <main className="auth-page__main">
        <section className="auth-card">
          <div className="status-icon status-icon--loading">
            <Clock3 size={32} />
          </div>
          <h1>Google 로그인 중이에요</h1>
          <p>잠시만 기다려 주세요. 곧 다음 화면으로 이동해요.</p>
        </section>
      </main>
    </div>
  );
}

function OAuthSignupForm({
  signupToken,
  email,
  defaultNickname,
}: {
  signupToken: string;
  email: string;
  defaultNickname: string;
}) {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);

  const initialNick = defaultNickname.replace(/[^가-힣a-zA-Z0-9]/g, '').slice(0, 12) || '';
  const [nickname, setNickname] = useState(initialNick);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [nicknameMessage, setNicknameMessage] = useState<string | null>(null);

  const nicknameRegex = /^[가-힣a-zA-Z0-9]{2,12}$/;

  const checkMutation = useMutation({
    mutationFn: () => checkNickname(nickname),
    onSuccess: (data) => {
      if (data.available) {
        setNicknameChecked(true);
        setNicknameMessage('사용 가능한 닉네임이에요.');
      } else {
        setNicknameChecked(false);
        setNicknameMessage('이미 사용 중인 닉네임이에요.');
      }
    },
    onError: (err) => {
      setNicknameChecked(false);
      setNicknameMessage(err instanceof ApiError ? err.message : '중복 확인에 실패했어요.');
    },
  });

  const signupMutation = useMutation({
    mutationFn: () => oauthSignup({ signupToken, nickname }),
    onSuccess: (data) => {
      setSession(data);
      navigate('/onboarding/interests', { replace: true });
    },
  });

  const handleNicknameChange = (val: string) => {
    setNickname(val);
    setNicknameChecked(false);
    setNicknameMessage(null);
  };

  const handleCheck = () => {
    if (!nicknameRegex.test(nickname)) {
      setNicknameMessage('2~12자의 한글, 영문, 숫자만 사용할 수 있어요.');
      return;
    }
    checkMutation.mutate();
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nicknameRegex.test(nickname)) {
      setNicknameMessage('2~12자의 한글, 영문, 숫자만 사용할 수 있어요.');
      return;
    }
    signupMutation.mutate();
  };

  return (
    <div className="site-shell auth-page">
      <AppHeader />
      <main className="auth-page__main">
        <section className="auth-card" aria-labelledby="oauth-signup-title">
          <div className="status-icon" style={{ color: 'var(--color-primary, #4f46e5)' }}>
            <CheckCircle size={32} />
          </div>
          <h1 id="oauth-signup-title">환영합니다!</h1>
          <p style={{ marginTop: '0.5rem', color: 'var(--color-text-secondary, #666)' }}>
            Google 계정(<strong>{email}</strong>)으로 냠냠취미를 시작해요.
            서비스에서 사용할 닉네임을 설정해 주세요.
          </p>

          <form className="auth-form" style={{ marginTop: '1.5rem' }} onSubmit={handleSubmit}>
            <div className="form-field">
              <label htmlFor="oauth-nickname">닉네임</label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  id="oauth-nickname"
                  type="text"
                  placeholder="2~12자 한글/영문/숫자"
                  value={nickname}
                  onChange={(e) => handleNicknameChange(e.target.value)}
                  maxLength={12}
                />
                <button
                  type="button"
                  className="status-action"
                  style={{ width: 'auto', padding: '0 1rem', whiteSpace: 'nowrap' }}
                  onClick={handleCheck}
                  disabled={checkMutation.isPending || !nickname}
                >
                  {checkMutation.isPending ? '확인 중' : '중복 확인'}
                </button>
              </div>
              {nicknameMessage && (
                <p
                  className={nicknameChecked ? 'form-field__success' : 'form-field__error'}
                  style={{ color: nicknameChecked ? 'var(--color-success, #16a34a)' : undefined, fontSize: '0.875rem', marginTop: '0.25rem' }}
                >
                  {nicknameMessage}
                </p>
              )}
            </div>

            {signupMutation.isError && (
              <p className="auth-form__server-error" role="alert">
                {signupMutation.error instanceof ApiError
                  ? signupMutation.error.message
                  : '회원가입 처리 중 문제가 발생했어요.'}
              </p>
            )}

            <button
              className="auth-form__submit"
              type="submit"
              disabled={signupMutation.isPending || !nicknameRegex.test(nickname)}
            >
              {signupMutation.isPending ? '가입 완료 중…' : '가입 완료하고 취미 찾기'}
            </button>
          </form>
        </section>
      </main>
    </div>
  );
}
