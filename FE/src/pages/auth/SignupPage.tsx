import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { checkEmail, checkNickname, signup } from '../../features/auth/api/authApi';
import { signupSchema, type SignupFormValues } from '../../features/auth/model/signupSchema';
import { ApiError } from '../../shared/api/client';
import { brandAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';

type CheckState = 'idle' | 'checking' | 'available' | 'unavailable';

export function SignupSteps({ active }: { active: 1 | 2 | 3 | 4 }) {
  return (
    <ol className="signup-steps" aria-label={`회원가입 ${active}단계`}>
      {['가입 정보', '이메일 인증', '관심 취미', '이용 상황'].map((label, index) => (
        <li key={label} className={index + 1 === active ? 'active' : index + 1 < active ? 'done' : ''}>
          <span>{index + 1}</span>{label}
        </li>
      ))}
    </ol>
  );
}

export function SignupPage() {
  const navigate = useNavigate();
  const [emailState, setEmailState] = useState<CheckState>('idle');
  const [nicknameState, setNicknameState] = useState<CheckState>('idle');
  const { register, handleSubmit, getValues, setError, formState: { errors } } = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
    mode: 'onChange',
    reValidateMode: 'onChange',
    defaultValues: { email: '', nickname: '', password: '', passwordConfirm: '', terms: false, privacy: false },
  });

  async function validateEmail() {
    const email = getValues('email');
    if (!/^\S+@\S+\.\S+$/.test(email)) return;
    setEmailState('checking');
    try { setEmailState((await checkEmail(email)).available ? 'available' : 'unavailable'); }
    catch { setEmailState('idle'); }
  }

  async function validateNickname() {
    const nickname = getValues('nickname');
    if (!/^[가-힣a-zA-Z0-9]{2,12}$/.test(nickname)) return;
    setNicknameState('checking');
    try { setNicknameState((await checkNickname(nickname)).available ? 'available' : 'unavailable'); }
    catch { setNicknameState('idle'); }
  }

  const signupMutation = useMutation({
    mutationFn: signup,
    onSuccess: (data) => navigate(`/signup/email-sent?email=${encodeURIComponent(data.email)}`),
    onError: (error) => {
      if (!(error instanceof ApiError) || !error.fields) return;
      error.fields.forEach(({ field, reason }) => {
        if (field === 'email' || field === 'nickname' || field === 'password') setError(field, { message: reason });
      });
    },
  });

  const onSubmit = handleSubmit(({ passwordConfirm: _confirm, terms: _terms, privacy: _privacy, ...request }) => {
    if (emailState === 'unavailable') { setError('email', { message: '이미 사용 중인 이메일이에요.' }); return; }
    if (nicknameState === 'unavailable') { setError('nickname', { message: '이미 사용 중인 닉네임이에요.' }); return; }
    signupMutation.mutate(request);
  });

  return (
    <div className="site-shell auth-page"><AppHeader />
      <main className="auth-page__main"><section className="auth-card auth-card--signup" aria-labelledby="signup-title">
        <img className="auth-card__logo" src={brandAssets.logo} alt="냠냠취미" />
        <h1 id="signup-title">회원가입</h1><SignupSteps active={1} />
        <form className="auth-form" onSubmit={onSubmit} noValidate>
          <div className="form-field"><label htmlFor="signup-email">이메일</label><input id="signup-email" type="email" placeholder="user@example.com" autoComplete="email" {...register('email', { onBlur: validateEmail, onChange: () => setEmailState('idle') })} />
            {errors.email ? <p className="form-field__error">{errors.email.message}</p> : <p className={`form-field__hint ${emailState}`}>{emailState === 'available' ? '사용할 수 있는 이메일이에요' : emailState === 'unavailable' ? '이미 사용 중인 이메일이에요' : emailState === 'checking' ? '확인 중…' : '이메일을 입력하면 사용 가능 여부를 확인해요'}</p>}
          </div>
          <div className="form-field"><label htmlFor="signup-nickname">닉네임</label><input id="signup-nickname" placeholder="취미찾는사람" autoComplete="nickname" {...register('nickname', { onBlur: validateNickname, onChange: () => setNicknameState('idle') })} />
            {errors.nickname ? <p className="form-field__error">{errors.nickname.message}</p> : <p className={`form-field__hint ${nicknameState}`}>{nicknameState === 'available' ? '사용할 수 있는 닉네임이에요' : nicknameState === 'unavailable' ? '이미 사용 중인 닉네임이에요' : '2~12자 · 한글·영문·숫자 · 중복 불가'}</p>}
          </div>
          <div className="form-field"><label htmlFor="signup-password">비밀번호</label><input id="signup-password" type="password" autoComplete="new-password" {...register('password')} />{errors.password ? <p className="form-field__error">{errors.password.message}</p> : <p className="form-field__hint">8~20자 · 영문·숫자·특수문자 중 2종 이상</p>}</div>
          <div className="form-field"><label htmlFor="signup-password-confirm">비밀번호 확인</label><input id="signup-password-confirm" type="password" autoComplete="new-password" {...register('passwordConfirm')} />{errors.passwordConfirm ? <p className="form-field__error">{errors.passwordConfirm.message}</p> : <p className="form-field__hint">비밀번호가 일치해야 해요</p>}</div>
          <label className="consent"><input type="checkbox" {...register('terms')} /><span>[필수] 서비스 이용약관 동의</span></label>{errors.terms && <p className="form-field__error">{errors.terms.message}</p>}
          <label className="consent"><input type="checkbox" {...register('privacy')} /><span>[필수] 개인정보 수집·이용 동의</span></label>{errors.privacy && <p className="form-field__error">{errors.privacy.message}</p>}
          {signupMutation.isError && <p className="auth-form__server-error" role="alert">{signupMutation.error instanceof ApiError ? signupMutation.error.message : '회원가입 중 문제가 발생했어요.'}</p>}
          <button className="auth-form__submit" disabled={signupMutation.isPending}>{signupMutation.isPending ? '전송 중…' : '인증 메일 보내기'}</button>
        </form>
        <p className="auth-card__caption">소셜 계정으로 가입하면 이메일 인증을 생략해요</p>
      </section></main>
    </div>
  );
}
