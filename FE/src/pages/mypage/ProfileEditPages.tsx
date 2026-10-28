import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { X } from 'lucide-react';
import { useAuthStore } from '../../features/auth/model/authStore';
import { AppHeader } from '../../shared/ui/AppHeader';

type ProfileFormValues = {
  nickname: string;
};

type PersonalInfoFormValues = {
  currentPassword: string;
  newPassword: string;
  passwordConfirm: string;
  weeklyHours: string;
  monthlyBudget: string;
  city: string;
  district: string;
  placeType: 'INDOOR' | 'OUTDOOR' | 'ANY';
  socialType: 'ALONE' | 'TOGETHER' | 'ANY';
};

const apiUnavailableMessage = '현재 BE에 정보 수정 API가 구현되지 않아 아직 저장할 수 없어요.';

export function ProfileEditModal() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const [submitMessage, setSubmitMessage] = useState('');
  const { register, handleSubmit, formState: { errors } } = useForm<ProfileFormValues>({
    defaultValues: { nickname: user?.nickname ?? '취미찾는사람' },
  });

  return <div className="profile-modal-backdrop" role="presentation" onMouseDown={() => navigate('/mypage', { replace: true })}>
    <section className="profile-modal" role="dialog" aria-modal="true" aria-labelledby="profile-modal-title" onMouseDown={(event) => event.stopPropagation()}>
      <Link className="profile-modal__close" to="/mypage" aria-label="닫기"><X size={18} /></Link>
      <h1 id="profile-modal-title">프로필 수정</h1>
      <p>프로필 사진과 닉네임을 변경할 수 있어요.</p>
      <div className="profile-modal__avatar">{(user?.nickname ?? '취').slice(0, 1)}</div>
      <button className="button button--secondary profile-modal__photo" type="button">사진 변경</button>
      <form onSubmit={handleSubmit(() => setSubmitMessage(apiUnavailableMessage))}>
        <label className="form-field" htmlFor="profile-nickname"><span>닉네임</span><input id="profile-nickname" {...register('nickname', {
          required: '닉네임을 입력해 주세요.',
          minLength: { value: 2, message: '닉네임은 2자 이상이어야 해요.' },
          maxLength: { value: 12, message: '닉네임은 12자 이하여야 해요.' },
          pattern: { value: /^[가-힣a-zA-Z0-9]+$/, message: '한글·영문·숫자만 사용할 수 있어요.' },
        })} /></label>
        {errors.nickname && <p className="profile-edit-error" role="alert">{errors.nickname.message}</p>}
        {submitMessage && <p className="profile-edit-error" role="status">{submitMessage}</p>}
        <div className="profile-modal__actions"><Link className="button button--secondary" to="/mypage">취소</Link><button className="button button--primary" type="submit">저장</button></div>
      </form>
    </section>
  </div>;
}

export function PersonalInfoEditPage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const [submitMessage, setSubmitMessage] = useState('');
  const { register, handleSubmit, watch } = useForm<PersonalInfoFormValues>({
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      passwordConfirm: '',
      weeklyHours: '3',
      monthlyBudget: '150000',
      city: '서울특별시',
      district: '마포구',
      placeType: 'INDOOR',
      socialType: 'ANY',
    },
  });
  const placeType = watch('placeType');
  const socialType = watch('socialType');

  return <div className="site-shell"><AppHeader authenticated /><main className="page-container personal-edit-page">
    <header><h1>개인정보 수정</h1><p>가입 정보와 활동 정보를 확인하고 필요한 항목을 변경할 수 있어요.</p></header>
    <form onSubmit={handleSubmit(() => setSubmitMessage(apiUnavailableMessage))}>
      <section className="personal-edit-card"><h2>가입 정보</h2>
        <label className="form-field"><span>이메일</span><div className="personal-edit-email"><input value={user?.email ?? 'user@example.com'} readOnly /><b>인증 완료</b></div></label>
        <hr />
        <h3>비밀번호 변경</h3><p>비밀번호는 보안을 위해 주기적으로 변경해 주세요.</p>
        <label className="form-field"><span>현재 비밀번호</span><input type="password" autoComplete="current-password" {...register('currentPassword')} /></label>
        <label className="form-field"><span>새 비밀번호</span><input type="password" autoComplete="new-password" placeholder="8~20자 · 영문·숫자·특수문자 중 2종 이상" {...register('newPassword')} /></label>
        <label className="form-field"><span>새 비밀번호 확인</span><input type="password" autoComplete="new-password" placeholder="새 비밀번호를 다시 입력" {...register('passwordConfirm')} /></label>
        <button className="button button--primary personal-edit-card__action" type="submit">비밀번호 변경</button>
      </section>
      <section className="personal-edit-card"><h2>활동 정보</h2><p>취미 추천 시 선택한 조건을 참고해요.</p>
        <div className="personal-edit-grid"><label className="form-field"><span>활동 가능 시간</span><input type="number" min="0" {...register('weeklyHours')} /></label><label className="form-field"><span>예산</span><input type="number" min="0" {...register('monthlyBudget')} /></label></div>
        <label className="form-field"><span>선호 지역</span><div className="personal-edit-grid"><select {...register('city')}><option>서울특별시</option></select><select {...register('district')}><option>마포구</option></select></div></label>
        <fieldset><legend>선호 환경</legend><label className={placeType === 'INDOOR' ? 'selected' : ''}><input type="radio" value="INDOOR" {...register('placeType')} />실내</label><label className={placeType === 'OUTDOOR' ? 'selected' : ''}><input type="radio" value="OUTDOOR" {...register('placeType')} />실외</label><label className={placeType === 'ANY' ? 'selected' : ''}><input type="radio" value="ANY" {...register('placeType')} />상관없음</label></fieldset>
        <fieldset><legend>활동 방식</legend><label className={socialType === 'ALONE' ? 'selected' : ''}><input type="radio" value="ALONE" {...register('socialType')} />혼자</label><label className={socialType === 'TOGETHER' ? 'selected' : ''}><input type="radio" value="TOGETHER" {...register('socialType')} />함께</label><label className={socialType === 'ANY' ? 'selected' : ''}><input type="radio" value="ANY" {...register('socialType')} />상관없음</label></fieldset>
        <button className="button button--primary personal-edit-card__action" type="submit">활동 정보 저장</button>
      </section>
      {submitMessage && <p className="profile-edit-error personal-edit-message" role="status">{submitMessage}</p>}
      <div className="personal-edit-actions"><button className="button button--secondary" type="button" onClick={() => navigate('/mypage')}>취소</button><button className="button button--primary" type="submit">변경 저장</button></div>
    </form>
  </main></div>;
}

