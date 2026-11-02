import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/model/authStore';
import { useOnboardingStore, type PlacePreference, type SocialPreference } from '../../features/onboarding/model/onboardingStore';
import {
  getOnboardingOptions,
  saveOnboarding,
  userQueryKeys,
  type OnboardingOptionsDto,
} from '../../features/user/api/userApi';
import { ApiError } from '../../shared/api/client';
import { AppHeader } from '../../shared/ui/AppHeader';
import { SignupSteps } from '../auth/SignupPage';

const hobbyGroups = [
  { label: '운동 · 야외', items: ['클라이밍', '러닝', '등산', '요가', '수영'] },
  { label: '공예 · 만들기', items: ['도예', '뜨개질', '가죽공예'] },
  { label: '음악 · 예술', items: ['드로잉', '우쿨렐레', '사진'] },
  { label: '요리 · 생활', items: ['홈베이킹', '커피', '플랜테리어'] },
];

export function HobbyInterestPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const guestMode = searchParams.get('mode') === 'guest';
  const conditionsPath = guestMode ? '/onboarding/conditions?mode=guest' : '/onboarding/conditions';
  const hobbies = useOnboardingStore((state) => state.hobbies);
  const toggleHobby = useOnboardingStore((state) => state.toggleHobby);
  const resetOnboarding = useOnboardingStore((state) => state.resetOnboarding);
  const optionsQuery = useQuery({
    queryKey: userQueryKeys.onboardingOptions(),
    queryFn: getOnboardingOptions,
    enabled: !guestMode,
  });
  const displayedGroups = guestMode || !optionsQuery.data
    ? hobbyGroups
    : groupHobbies(optionsQuery.data);
  const cancelGuestExperience = () => {
    resetOnboarding();
    navigate('/', { replace: true });
  };
  return <OnboardingLayout guestMode={guestMode}>
    <div className="onboarding-heading"><div><h1>관심 있는 취미를 골라 주세요</h1><p>최대 5개까지 선택할 수 있습니다. 선택하지 않아도 다음 단계로 이동할 수 있습니다.</p></div>{guestMode ? <GuestExperienceSteps active={1} /> : <SignupSteps active={3} />}</div>
    <section className="choice-panel">
      {optionsQuery.isLoading && !guestMode && <p role="status">선택 가능한 취미를 불러오고 있어요.</p>}
      {optionsQuery.isError && !guestMode && <p className="profile-edit-error" role="alert">{getErrorMessage(optionsQuery.error)}</p>}
      {displayedGroups.map((group) => <div className="choice-row" key={group.label}><strong>{group.label}</strong><div>{group.items.map((name) => <button type="button" key={name} className={hobbies.includes(name) ? 'selected' : ''} aria-pressed={hobbies.includes(name)} onClick={() => toggleHobby(name)}>{name}</button>)}</div></div>)}
      <div className="selection-summary"><strong>선택한 취미 {hobbies.length}개</strong><span>{hobbies.length ? hobbies.join(' · ') : '아직 선택한 취미가 없어요'}</span></div>
    </section>
    <div className="onboarding-actions">
      {guestMode ? <button className="button button--secondary onboarding-cancel" type="button" onClick={cancelGuestExperience}>취소</button> : <button className="button button--secondary" type="button" onClick={() => navigate(conditionsPath)}>건너뛰기</button>}
      <div className="onboarding-actions__right">
        {guestMode && <button className="button button--secondary" type="button" onClick={() => navigate(conditionsPath)}>건너뛰기</button>}
        <button className="button button--primary" type="button" onClick={() => navigate(conditionsPath)}>다음</button>
      </div>
    </div>
  </OnboardingLayout>;
}

export function UsageConditionPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const guestMode = searchParams.get('mode') === 'guest';
  const state = useOnboardingStore();
  const authUser = useAuthStore((auth) => auth.user);
  const setUser = useAuthStore((auth) => auth.setUser);
  const optionsQuery = useQuery({
    queryKey: userQueryKeys.onboardingOptions(),
    queryFn: getOnboardingOptions,
    enabled: !guestMode,
  });
  const onboardingMutation = useMutation({
    mutationFn: saveOnboarding,
    onSuccess: () => {
      if (authUser) setUser({ ...authUser, onboarded: true });
      state.resetOnboarding();
      navigate('/home', { replace: true });
    },
  });
  const save = () => {
    if (guestMode) {
      navigate('/explore?guest=true', { replace: true });
      return;
    }
    const options = optionsQuery.data;
    const hobbyByName = new Map(options?.hobbies.map((hobby) => [hobby.name, hobby.hobbyId]) ?? []);
    const selectedRegion = options?.regions.find((region) => region.sido === state.sido && region.sigungu === state.sigungu);
    onboardingMutation.mutate({
      initialHobbies: state.hobbies.flatMap((name) => {
        const hobbyId = hobbyByName.get(name);
        return hobbyId ? [{ hobbyId }] : [];
      }),
      weeklyHours: state.weeklyHours === '' ? null : Number(state.weeklyHours),
      monthlyBudget: state.monthlyBudget === '' ? null : Number(state.monthlyBudget),
      placePref: state.placePref || null,
      socialPref: state.socialPref || null,
      regionId: selectedRegion?.id ?? null,
    });
  };
  const cancelGuestExperience = () => {
    state.resetOnboarding();
    navigate('/', { replace: true });
  };
  return <OnboardingLayout guestMode={guestMode}>
    <div className="onboarding-heading"><div><h1>취미를 즐길 상황을 알려 주세요</h1><p>모든 항목은 선택이며, 입력하지 않은 항목은 추천 조건으로 사용하지 않습니다.</p></div>{guestMode ? <GuestExperienceSteps active={2} /> : <SignupSteps active={4} />}</div>
    <section className="condition-panel">
      <label className="condition-field"><strong>활동 가능 시간 (시간/주)</strong><input inputMode="numeric" placeholder="예: 3" value={state.weeklyHours} onChange={(event) => state.setCondition({ weeklyHours: event.target.value.replace(/\D/g, '') })} /></label>
      <label className="condition-field"><strong>예산 (원/월)</strong><input inputMode="numeric" placeholder="예: 150,000" value={state.monthlyBudget} onChange={(event) => state.setCondition({ monthlyBudget: event.target.value.replace(/\D/g, '') })} /></label>
      <ChoiceField<PlacePreference> label="실내 · 실외" value={state.placePref} options={[['INDOOR','실내'],['OUTDOOR','실외'],['ANY','상관없음']]} onChange={(placePref) => state.setCondition({ placePref })} />
      <ChoiceField<SocialPreference> label="혼자 · 함께" value={state.socialPref} options={[['ALONE','혼자'],['TOGETHER','함께'],['ANY','상관없음']]} onChange={(socialPref) => state.setCondition({ socialPref })} />
      <label className="condition-field"><strong>선호 지역 · 시/도</strong><select value={state.sido} onChange={(event) => state.setCondition({ sido: event.target.value, sigungu: '' })}><option value="">선택 안 함</option>{(guestMode ? ['서울특별시'] : getSidos(optionsQuery.data)).map((sido) => <option value={sido} key={sido}>{sido}</option>)}</select></label>
      <label className="condition-field"><strong>시 · 군 · 구</strong><select value={state.sigungu} disabled={!state.sido} onChange={(event) => state.setCondition({ sigungu: event.target.value })}><option value="">선택 안 함</option>{guestMode ? <option value="마포구">마포구</option> : (optionsQuery.data?.regions ?? []).filter((region) => region.sido === state.sido).map((region) => <option value={region.sigungu} key={region.id}>{region.sigungu}</option>)}</select></label>
      <aside className="condition-note">선택한 지역을 기준으로 장소를 추천합니다. GPS는 사용하지 않습니다.</aside>
    </section>
    {onboardingMutation.isError && <p className="profile-edit-error" role="alert">{getErrorMessage(onboardingMutation.error)}</p>}
    <div className="onboarding-actions">
      {guestMode ? <button className="button button--secondary onboarding-cancel" type="button" onClick={cancelGuestExperience}>취소</button> : <button className="button button--secondary" type="button" onClick={save}>건너뛰기</button>}
      <div className="onboarding-actions__right">
        {guestMode && <button className="button button--secondary" type="button" onClick={save}>건너뛰기</button>}
        <button className="button button--primary onboarding-save" type="button" disabled={onboardingMutation.isPending || (!guestMode && optionsQuery.isLoading)} onClick={save}>{onboardingMutation.isPending ? '저장 중…' : guestMode ? '취향 탐색 시작' : '저장하고 홈으로'}</button>
      </div>
    </div>
  </OnboardingLayout>;
}

function groupHobbies(options: OnboardingOptionsDto) {
  const groups = new Map<string, string[]>();
  options.hobbies.forEach((hobby) => groups.set(hobby.categoryName, [...(groups.get(hobby.categoryName) ?? []), hobby.name]));
  return Array.from(groups, ([label, items]) => ({ label, items }));
}

function getSidos(options?: OnboardingOptionsDto) {
  return Array.from(new Set(options?.regions.map((region) => region.sido) ?? []));
}

function getErrorMessage(error: unknown) {
  if (error instanceof ApiError || error instanceof Error) return error.message;
  return '요청을 처리하지 못했어요.';
}

function GuestExperienceSteps({ active }: { active: 1 | 2 | 3 }) {
  return <ol className="guest-experience-steps" aria-label={`비회원 취향 탐색 ${active}단계`}>
    {['관심 취미', '이용 상황', '숏폼 탐색'].map((label, index) => <li key={label} className={index + 1 === active ? 'active' : index + 1 < active ? 'done' : ''}><span>{index + 1}</span>{label}</li>)}
  </ol>;
}

function ChoiceField<T extends string>({ label, value, options, onChange }: { label: string; value: T; options: [T,string][]; onChange: (value: T) => void }) {
  return <fieldset className="condition-choice"><legend>{label}</legend><div>{options.map(([key, text]) => <button type="button" className={value === key ? 'selected' : ''} key={key} onClick={() => onChange(key)}>{text}</button>)}</div></fieldset>;
}

function OnboardingLayout({ children, guestMode = false }: { children: React.ReactNode; guestMode?: boolean }) {
  const resetOnboarding = useOnboardingStore((state) => state.resetOnboarding);
  return <div className="site-shell"><AppHeader onBrandClick={guestMode ? resetOnboarding : undefined} /><main className="page-container onboarding-page">{children}</main></div>;
}
