import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../../features/auth/api/authApi';
import { loginSchema, type LoginFormValues } from '../../features/auth/model/loginSchema';
import { useAuthStore } from '../../features/auth/model/authStore';
import { ApiError } from '../../shared/api/client';
import { brandAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';

function getLoginErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) return '로그인 중 문제가 발생했어요.';

  switch (error.code) {
    case 'AUTH_INVALID_CREDENTIALS':
      return '이메일 또는 비밀번호가 올바르지 않아요.';
    case 'AUTH_ACCOUNT_LOCKED':
      return error.retryAfterSeconds
        ? `로그인 시도가 잠겼어요. ${error.retryAfterSeconds}초 후 다시 시도해 주세요.`
        : '로그인 실패가 반복되어 계정이 잠겼어요. 잠시 후 다시 시도해 주세요.';
    case 'AUTH_EMAIL_NOT_VERIFIED':
      return '이메일 인증이 완료되지 않았어요. 인증 메일을 확인해 주세요.';
    case 'AUTH_SOCIAL_ACCOUNT':
      return error.message || '소셜 로그인으로 가입된 계정이에요.';
    default:
      return error.message;
  }
}

export function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setSession(data);
      navigate(data.user.onboarded ? '/home' : '/onboarding/interests', { replace: true });
    },
  });

  const onSubmit = handleSubmit((values) => {
    loginMutation.mutate(values);
  });

  return (
    <div className="site-shell auth-page">
      <AppHeader />
      <main className="auth-page__main">
        <section className="auth-card" aria-labelledby="login-title">
          <img className="auth-card__logo" src={brandAssets.logo} alt="냠냠취미" />
          <h1 id="login-title">로그인</h1>

          <form className="auth-form" onSubmit={onSubmit} noValidate>
            <div className="form-field">
              <label htmlFor="login-email">이메일</label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                placeholder="user@example.com"
                aria-invalid={Boolean(errors.email)}
                {...register('email')}
              />
              {errors.email && <p className="form-field__error">{errors.email.message}</p>}
            </div>

            <div className="form-field">
              <label htmlFor="login-password">비밀번호</label>
              <input
                id="login-password"
                type="password"
                autoComplete="current-password"
                placeholder="비밀번호를 입력하세요"
                aria-invalid={Boolean(errors.password)}
                {...register('password')}
              />
              {errors.password && <p className="form-field__error">{errors.password.message}</p>}
            </div>

            {loginMutation.isError && (
              <p className="auth-form__server-error" role="alert">
                {getLoginErrorMessage(loginMutation.error)}
              </p>
            )}

            <button className="auth-form__submit" type="submit" disabled={loginMutation.isPending}>
              {loginMutation.isPending ? '로그인 중…' : '로그인'}
            </button>
          </form>

          <div className="auth-card__links">
            <Link to="/password-reset">비밀번호 재설정</Link>
            <Link to="/signup">회원가입</Link>
          </div>

          <div className="auth-divider"><span>또는</span></div>

          <div className="social-login" aria-label="소셜 로그인">
            <a className="social-login__button social-login__button--google" href="/oauth2/authorization/google">Google로 계속하기</a>
            <button className="social-login__button social-login__button--kakao" type="button" disabled>카카오로 계속하기</button>
            <button className="social-login__button social-login__button--naver" type="button" disabled>네이버로 계속하기</button>
          </div>

        </section>
      </main>
    </div>
  );
}
