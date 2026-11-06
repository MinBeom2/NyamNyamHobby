// Standalone, development-only visual fixture. Vite's production build has a
// single index.html entry and does not ship this page or any fixture data.
import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import '@fontsource/noto-sans-kr/korean-400.css';
import '@fontsource/noto-sans-kr/korean-500.css';
import '@fontsource/noto-sans-kr/korean-600.css';
import '@fontsource/noto-sans-kr/korean-700.css';
import '../src/shared/styles/global.css';
import '../src/pages/activities/hobbyManagement.css';
import { managementHobbies, managementPlan } from '../src/mocks/activityManagementFixtures';
import { hobbyListView, planView } from '../src/features/activity/model/managementAdapter';
import { HobbySidebar } from '../src/features/activity/ui/HobbySidebar';
import { PlanHeader } from '../src/features/activity/ui/PlanHeader';
import { WeekTabs } from '../src/features/activity/ui/WeekTabs';
import { ActivityCard } from '../src/features/activity/ui/ActivityCard';
import { ManagementDialog } from '../src/features/activity/ui/ManagementDialog';
import { useActivityRecords } from '../src/features/activity/hooks/useActivityRecords';
import { AppHeader } from '../src/shared/ui/AppHeader';
import { Button } from '../src/shared/ui/Button';
import { useAuthStore } from '../src/features/auth/model/authStore';

function Preview() {
  const [week, setWeek] = useState(2);
  const [picker, setPicker] = useState(false);
  const plan = planView(managementPlan(week));
  const records = useActivityRecords(plan.id, plan.items);
  const [notice, setNotice] = useState('');
  // Writes are blocked in this visual fixture; no request or fake success.
  const previewRecords = { ...records, save: async () => { setNotice('예시 화면에서는 저장할 수 없어요. 실제 앱에서 로그인 후 이용해 주세요.'); return false; } };
  const sidebar = <HobbySidebar list={hobbyListView(managementHobbies())} selectedId={1} count={1} max={3} unsave={() => setNotice('예시 화면에서는 관심 목록을 변경할 수 없어요.')} />;
  return <div className="site-shell hobby-management">
    <p style={{ margin: 0, padding: '8px 16px', textAlign: 'center', background: '#e1f3f3', fontSize: 11 }}>개발용 디자인 검증 · 예시 데이터 · 저장 불가 · <a href="/my-hobbies" style={{ textDecoration: 'underline' }}>실제 앱 열기</a></p>
    <AppHeader /><main className="page-container hm-layout"><aside className="hm-desktop-sidebar">{sidebar}</aside><div className="hm-main">
      <div className="hm-mobile-switch"><div><small>내 취미</small><b>캔들·디퓨저</b></div><Button variant="secondary" onClick={() => setPicker(true)}>취미 변경</Button></div>
      <PlanHeader plan={plan} onEdit={() => setNotice('계획 편집은 실제 앱에서 로그인 후 확인해 주세요.')} onStatus={() => setNotice('예시 화면에서는 계획 상태를 변경할 수 없어요.')} busy={false} />
      {notice && <p className="hm-notice" role="status">{notice}</p>}
      <WeekTabs plan={plan} onSelect={setWeek} />
      <section className="hm-week" role="tabpanel" id={`hm-week-${plan.id}`} aria-labelledby={`hm-tab-${plan.id}-${week}`}>
        <header className="hm-week__heading"><div><span className="hm-eyebrow">WEEK 0{week} / 03</span><h2>{week}주차 · {plan.weekTitle}</h2></div><p>예상 소요 시간 {plan.duration}<br />{plan.weekProgress.label}</p></header>
        <details className="hm-supplies"><summary>이번 주 준비물</summary><ul>{plan.supplies.map((supply) => <li key={supply}>{supply}</li>)}</ul></details>
        {plan.restriction && <p className="hm-notice">{plan.restriction}</p>}
        <div className="hm-activities">{plan.items.map((item) => <ActivityCard key={item.itemId} item={item} canRecord={plan.canRecord} records={previewRecords} />)}</div>
      </section>
    </div></main>{picker && <ManagementDialog title="취미 변경" panel onClose={() => setPicker(false)}>{sidebar}</ManagementDialog>}
  </div>;
}

if (import.meta.env.DEV) {
  useAuthStore.getState().finishSessionCheck();
  createRoot(document.getElementById('root')!).render(<QueryClientProvider client={new QueryClient()}><MemoryRouter><Preview /></MemoryRouter></QueryClientProvider>);
}
