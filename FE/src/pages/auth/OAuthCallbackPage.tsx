import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { Check, CircleAlert, Clock3 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { checkNickname, oauthSignup } from '../../features/auth/api/authApi';
import { oauthSignupSchema, type OAuthSignupFormValues } from '../../features/auth/model/oauthSignupSchema';
import { useAuthStore, type AuthUser } from '../../features/auth/model/authStore';
import { ApiError } from '../../shared/api/client';
import { brandAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';

type OAuthCallbackState =
  | { status: 'SUCCESS'; accessToken: string; user: AuthUser; expiresIn: number }
  | { status: 'SIGNUP_REQUIRED'; signupToken: string; email: string; defaultNickname: string }
  | { status: 'EMAIL_EXISTS'; email: string }
  | { status: 'FAILED'; message: string };

function readTokenExpiresIn(accessToken: string) {
  try {
    const payload = accessToken.split('.')[1];
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = JSON.parse(atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '='))) as { exp?: number };
    if (decoded.exp) return Math.max(1, decoded.exp - Math.floor(Date.now() / 1000));
  } catch {
    // BE redirect does not currently include expiresIn, so malformed payloads use the documented default.
  }
  return 1800;
}

function parseCallback(): OAuthCallbackState {
  const params = new URLSearchParams(window.location.search);
  const status = params.get('status');

  if (status === 'SUCCESS') {
    const accessToken = params.get('accessToken');
    const id = Number(params.get('userId'));
    const email = params.get('email');
    const nickname = params.get('nickname');
    const role = params.get('role');
    if (accessToken && Number.isFinite(id) && id > 0 && email && nickname && (role === 'USER' || role === 'ADMIN')) {
      return {
        status,
        accessToken,
        expiresIn: readTokenExpiresIn(accessToken),
        user: { id, email, nickname, role, onboarded: params.get('onboarded') === 'true' },
      };
    }
    return { status: 'FAILED', message: '로그인 응답 정보가 올바르지 않아요.' };
  }

  if (status === 'SIGNUP_REQUIRED') {
    const signupToken = params.get('signupToken');
    if (signupToken) return { status, signupToken, email: params.get('email') ?? '', defaultNickname: params.get('defaultNickname') ?? '' };
    return { status: 'FAILED', message: '소셜 가입 토큰이 누락됐어요. Google 로그인을 다시 시도해 주세요.' };
  }

  if (status === 'EMAIL_EXISTS') return { status, email: params.get('email') ?? '' };
  return { status: 'FAILED', message: params.get('message') ?? '소셜 로그인에 실패했어요.' };
}

export function OAuthCallbackPage() {
  const [callback] = useState<OAuthCallbackState>(parseCallback);
  const setSession = useAuthStore((state) => state.setSession);
  const navigate = useNavigate();
  const handled = useRef(false);

  useEffect(() => {
    window.history.replaceState(window.history.state, '', '/oauth/callback');
    if (callback.status !== 'SUCCESS' || handled.current) return;
    handled.current = true;
    setSession({ accessToken: callback.accessToken, expiresIn: callback.expiresIn, user: callback.user });
    navigate(callback.user.onboarded ? '/home' : '/onboarding/interests', { replace: true });
  }, [callback, navigate, setSession]);

  if (callback.status === 'SIGNUP_REQUIRED') return <OAuthSignupCard callback={callback} />;
  if (callback.status === 'EMAIL_EXISTS') return <OAuthStatusCard icon="error" title="이미 가입된 이메일이에요">
    <p><strong>{callback.email || 'Google 계정 이메일'}</strong>은 일반 이메일 계정으로 가입되어 있어요. 이메일과 비밀번호로 로그인해 주세요.</p>
    <Link className="status-action" to="/login">일반 로그인으로 이동</Link>
  </OAuthStatusCard>;
  if (callback.status === 'FAILED') return <OAuthStatusCard icon="error" title="Google 로그인에 실패했어요">
    <p>{callback.message}</p><Link className="status-action status-action--outline" to="/login">로그인으로 돌아가기</Link>
  </OAuthStatusCard>;
  return <OAuthStatusCard icon="loading" title="Google 로그인을 완료하고 있어요"><p>잠시만 기다려 주세요.</p></OAuthStatusCard>;
}

function OAuthSignupCard({ callback }: { callback: Extract<OAuthCallbackState, { status: 'SIGNUP_REQUIRED' }> }) {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const [nicknameState, setNicknameState] = useState<'idle' | 'checking' | 'available' | 'unavailable'>('idle');
  const { register, handleSubmit, getValues, setError, formState: { errors } } = useForm<OAuthSignupFormValues>({
    resolver: zodResolver(oauthSignupSchema),
    defaultValues: { nickname: /^[가-힣a-zA-Z0-9]{2,12}$/.test(callback.defaultNickname) ? callback.defaultNickname : '' },
  });

  async function validateNickname() {
    const nickname = getValues('nickname');
    if (!/^[가-힣a-zA-Z0-9]{2,12}$/.test(nickname)) return;
    setNicknameState('checking');
    try { setNicknameState((await checkNickname(nickname)).available ? 'available' : 'unavailable'); }
    catch { setNicknameState('idle'); }
  }

  const mutation = useMutation({
    mutationFn: ({ nickname }: OAuthSignupFormValues) => oauthSignup({ signupToken: callback.signupToken, nickname }),
    onSuccess: (data) => {
      setSession(data);
      navigate(data.user.onboarded ? '/home' : '/onboarding/interests', { replace: true });
    },
    onError: (error) => {
      if (!(error instanceof ApiError)) return;
      if (error.code === 'AUTH_NICKNAME_DUPLICATED') {
        setNicknameState('unavailable');
        setError('nickname', { message: '이미 사용 중인 닉네임이에요.' });
      }
    },
  });

  const submit = handleSubmit((values) => {
    if (nicknameState === 'unavailable') {
      setError('nickname', { message: '이미 사용 중인 닉네임이에요.' });
      return;
    }
    mutation.mutate(values);
  });

  return <div className="site-shell auth-page"><AppHeader /><main className="auth-page__main"><section className="auth-card auth-card--oauth" aria-labelledby="oauth-signup-title">
    <img className="auth-card__logo" src={brandAssets.logo} alt="냠냠취미" />
    <div className="status-icon"><Check size={24} /></div><h1 id="oauth-signup-title">Google 계정으로 시작하기</h1>
    <p className="oauth-account">{callback.email}</p>
    <form className="auth-form" onSubmit={submit} noValidate><div className="form-field"><label htmlFor="oauth-nickname">닉네임</label><input id="oauth-nickname" autoComplete="nickname" placeholder="취미찾는사람" {...register('nickname', { onBlur: validateNickname, onChange: () => setNicknameState('idle') })} />
      {errors.nickname ? <p className="form-field__error">{errors.nickname.message}</p> : <p className={`form-field__hint ${nicknameState}`}>{nicknameState === 'available' ? '사용할 수 있는 닉네임이에요' : nicknameState === 'unavailable' ? '이미 사용 중인 닉네임이에요' : nicknameState === 'checking' ? '확인 중…' : '2~12자 · 한글·영문·숫자 · 중복 불가'}</p>}
    </div>{mutation.isError && <p className="auth-form__server-error" role="alert">{mutation.error instanceof ApiError ? mutation.error.message : '소셜 회원가입을 완료하지 못했어요.'}</p>}
      <button className="auth-form__submit" type="submit" disabled={mutation.isPending}>{mutation.isPending ? '가입 중…' : '가입하고 계속하기'}</button>
    </form><Link className="status-link" to="/login">다른 방법으로 로그인</Link>
  </section></main></div>;
}

function OAuthStatusCard({ icon, title, children }: { icon: 'loading' | 'error'; title: string; children: React.ReactNode }) {
  return <div className="site-shell auth-page"><AppHeader /><main className="auth-page__main"><section className="auth-card auth-card--status">
    <div className={icon === 'loading' ? 'status-icon status-icon--loading' : 'status-icon status-icon--error'}>{icon === 'loading' ? <Clock3 size={24} /> : <CircleAlert size={24} />}</div><h1>{title}</h1>{children}
  </section></main></div>;
}
