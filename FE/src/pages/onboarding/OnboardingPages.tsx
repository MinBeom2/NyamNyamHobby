import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/model/authStore';
import { useOnboardingStore, type PlacePreference, type SocialPreference } from '../../features/onboarding/model/onboardingStore';
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
  const hobbies = useOnboardingStore((state) => state.hobbies);
  const toggleHobby = useOnboardingStore((state) => state.toggleHobby);
  return <OnboardingLayout>
    <div className="onboarding-heading"><div><h1>관심 있는 취미를 골라 주세요</h1><p>최대 5개까지 선택할 수 있습니다. 선택하지 않아도 다음 단계로 이동할 수 있습니다.</p></div><SignupSteps active={3} /></div>
    <section className="choice-panel">
      {hobbyGroups.map((group) => <div className="choice-row" key={group.label}><strong>{group.label}</strong><div>{group.items.map((name) => <button type="button" key={name} className={hobbies.includes(name) ? 'selected' : ''} aria-pressed={hobbies.includes(name)} onClick={() => toggleHobby(name)}>{name}</button>)}</div></div>)}
      <div className="selection-summary"><strong>선택한 취미 {hobbies.length}개</strong><span>{hobbies.length ? hobbies.join(' · ') : '아직 선택한 취미가 없어요'}</span></div>
    </section>
    <div className="onboarding-actions"><button className="button button--secondary" type="button" onClick={() => navigate('/onboarding/conditions')}>건너뛰기</button><button className="button button--primary" type="button" onClick={() => navigate('/onboarding/conditions')}>다음</button></div>
  </OnboardingLayout>;
}

export function UsageConditionPage() {
  const navigate = useNavigate();
  const state = useOnboardingStore();
  const save = () => navigate('/home', { replace: true });
  return <OnboardingLayout>
    <div className="onboarding-heading"><div><h1>취미를 즐길 상황을 알려 주세요</h1><p>모든 항목은 선택이며, 입력하지 않은 항목은 추천 조건으로 사용하지 않습니다.</p></div><SignupSteps active={4} /></div>
    <section className="condition-panel">
      <label className="condition-field"><strong>활동 가능 시간 (시간/주)</strong><input inputMode="numeric" placeholder="예: 3" value={state.weeklyHours} onChange={(event) => state.setCondition({ weeklyHours: event.target.value.replace(/\D/g, '') })} /></label>
      <label className="condition-field"><strong>예산 (원/월)</strong><input inputMode="numeric" placeholder="예: 150,000" value={state.monthlyBudget} onChange={(event) => state.setCondition({ monthlyBudget: event.target.value.replace(/\D/g, '') })} /></label>
      <ChoiceField<PlacePreference> label="실내 · 실외" value={state.placePref} options={[['INDOOR','실내'],['OUTDOOR','실외'],['ANY','상관없음']]} onChange={(placePref) => state.setCondition({ placePref })} />
      <ChoiceField<SocialPreference> label="혼자 · 함께" value={state.socialPref} options={[['ALONE','혼자'],['TOGETHER','함께'],['ANY','상관없음']]} onChange={(socialPref) => state.setCondition({ socialPref })} />
      <label className="condition-field"><strong>선호 지역 · 시/도</strong><select value={state.sido} onChange={(event) => state.setCondition({ sido: event.target.value })}><option>서울특별시</option></select></label>
      <label className="condition-field"><strong>시 · 군 · 구</strong><select value={state.sigungu} onChange={(event) => state.setCondition({ sigungu: event.target.value })}><option>마포구</option></select></label>
      <aside className="condition-note">선택한 지역을 기준으로 장소를 추천합니다. GPS는 사용하지 않습니다.</aside>
    </section>
    <div className="onboarding-actions"><button className="button button--secondary" type="button" onClick={save}>건너뛰기</button><button className="button button--primary onboarding-save" type="button" onClick={save}>저장하고 숏폼 탐색 시작</button></div>
  </OnboardingLayout>;
}

function ChoiceField<T extends string>({ label, value, options, onChange }: { label: string; value: T; options: [T,string][]; onChange: (value: T) => void }) {
  return <fieldset className="condition-choice"><legend>{label}</legend><div>{options.map(([key, text]) => <button type="button" className={value === key ? 'selected' : ''} key={key} onClick={() => onChange(key)}>{text}</button>)}</div></fieldset>;
}

function OnboardingLayout({ children }: { children: React.ReactNode }) {
  const clearSession = useAuthStore((state) => state.clearSession);
  return <div className="site-shell"><AppHeader authenticated onLogout={clearSession} /><main className="page-container onboarding-page">{children}</main></div>;
}
