import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useSearchParams } from 'react-router-dom';
import { requestPasswordReset, resetPassword } from '../../features/auth/api/authApi';
import { passwordResetSchema, type PasswordResetFormValues } from '../../features/auth/model/signupSchema';
import { ApiError } from '../../shared/api/client';
import { brandAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';
import { LinkExpiredPage } from './EmailVerificationPages';

export function PasswordResetRequestPage() {
  const [email, setEmail] = useState('');
  const mutation = useMutation({ mutationFn: () => requestPasswordReset(email) });
  return <PasswordCard title="비밀번호 재설정">
    {mutation.isSuccess ? <><p className="password-copy"><strong>{email}</strong>로 재설정 메일을 보냈어요. 메일의 링크는 30분 동안 사용할 수 있어요.</p><aside className="status-note">메일이 보이지 않으면 스팸함도 확인해 주세요.</aside></> : <>
      <p className="password-copy">가입한 이메일을 입력하면 비밀번호를 다시 설정할 수 있는 링크를 보내 드려요.</p>
      <label className="form-field"><span>가입 이메일</span><input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="이메일 주소를 입력하세요" /></label>
      {mutation.isError && <p className="status-error">{mutation.error instanceof ApiError ? mutation.error.message : '메일을 보내지 못했어요.'}</p>}
      <button className="auth-form__submit" type="button" disabled={!/^\S+@\S+\.\S+$/.test(email) || mutation.isPending} onClick={() => mutation.mutate()}>{mutation.isPending ? '전송 중…' : '재설정 메일 보내기'}</button>
      <aside className="status-note">가입 여부와 관계없이 같은 안내가 표시돼요. 메일의 링크는 30분 동안 한 번만 쓸 수 있어요.</aside>
    </>}
    <Link className="status-link" to="/login">로그인으로 돌아가기</Link>
  </PasswordCard>;
}

export function PasswordResetConfirmPage() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const { register, handleSubmit, formState: { errors } } = useForm<PasswordResetFormValues>({ resolver: zodResolver(passwordResetSchema) });
  const mutation = useMutation({ mutationFn: (values: PasswordResetFormValues) => resetPassword(token ?? '', values.newPassword) });
  if (!token) return <LinkExpiredPage />;
  return <PasswordCard title="새 비밀번호 설정">
    {mutation.isSuccess ? <><p className="password-copy">비밀번호가 변경되었어요. 새 비밀번호로 로그인해 주세요.</p><Link className="status-action" to="/login">로그인하기</Link></> : <form className="auth-form" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <div className="form-field"><label htmlFor="new-password">새 비밀번호</label><input id="new-password" type="password" autoComplete="new-password" {...register('newPassword')} />{errors.newPassword && <p className="form-field__error">{errors.newPassword.message}</p>}</div>
      <div className="form-field"><label htmlFor="new-password-confirm">새 비밀번호 확인</label><input id="new-password-confirm" type="password" autoComplete="new-password" {...register('passwordConfirm')} />{errors.passwordConfirm && <p className="form-field__error">{errors.passwordConfirm.message}</p>}</div>
      {mutation.isError && <p className="status-error">{mutation.error instanceof ApiError ? mutation.error.message : '비밀번호를 변경하지 못했어요.'}</p>}
      <button className="auth-form__submit" disabled={mutation.isPending}>{mutation.isPending ? '변경 중…' : '비밀번호 변경'}</button>
    </form>}
  </PasswordCard>;
}

function PasswordCard({ title, children }: { title: string; children: React.ReactNode }) {
  return <div className="site-shell auth-page"><AppHeader /><main className="auth-page__main"><section className="auth-card auth-card--password"><img className="auth-card__logo" src={brandAssets.logo} alt="냠냠취미" /><h1>{title}</h1>{children}</section></main></div>;
}
