import { useContext, useState } from 'react';
import { UNSAFE_DataRouterContext } from 'react-router-dom';
import { CalendarClock, CheckCircle2, ChevronDown, ChevronsUpDown, Leaf, Plus, Package } from 'lucide-react';
import { useHobbyManagement } from '../../features/activity/hooks/useHobbyManagement';
import { useActivityRecords } from '../../features/activity/hooks/useActivityRecords';
import { managementError, type ManagementPlan } from '../../features/activity/model/managementAdapter';
import { HobbySidebar } from '../../features/activity/ui/HobbySidebar';
import { PlanHeader } from '../../features/activity/ui/PlanHeader';
import { WeekTabs } from '../../features/activity/ui/WeekTabs';
import { ActivityCard } from '../../features/activity/ui/ActivityCard';
import { PlanEditor } from '../../features/activity/ui/PlanEditor';
import { ManagementDialog } from '../../features/activity/ui/ManagementDialog';
import { DraftPrompt, RouterDraftGuard } from '../../features/activity/ui/DraftGuard';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';
import './hobbyManagement.css';

function LoadingPlan() {
  return <div className="hm-loading" role="status" aria-label="계획을 불러오고 있어요"><div /><div /><div /></div>;
}

function PlanContent({ plan, manager }: { plan: ManagementPlan; manager: ReturnType<typeof useHobbyManagement> }) {
  const records = useActivityRecords(plan.id, plan.items);
  const [editOpen, setEditOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null);
  const dataRouter = useContext(UNSAFE_DataRouterContext);
  function guarded(action: () => void) {
    if (records.saving) return;
    if (records.dirty) setPendingAction(() => action);
    else action();
  }
  return <>
    {dataRouter && <RouterDraftGuard dirty={records.dirty} saving={records.saving} saveAll={records.saveAll} discard={records.discard} />}
    <PlanHeader plan={plan} onEdit={() => guarded(() => setEditOpen(true))} onStatus={() => guarded(() => manager.statusMutation.mutate(plan.paused ? 'IN_PROGRESS' : 'PAUSED'))} busy={manager.statusMutation.isPending || records.saving} error={manager.statusMutation.isError ? managementError(manager.statusMutation.error) : undefined} />
    {plan.overdueCount > 0 && <details className="hm-overdue"><summary><CalendarClock size={19} /><span>이전 주차에 남은 활동 <b>{plan.overdueCount}개</b></span><span className="hm-overdue__open">확인하기 <ChevronDown size={15} /></span></summary><div>{plan.overdue.map((group) => <section key={group.weekNo}><h3>{group.weekNo}주차에 남은 활동</h3>{group.items.map((item) => <div className="hm-overdue__item" key={item.itemId}><span>{item.title}</span><Button variant="text" onClick={() => manager.goWeek(group.weekNo)}>해당 주차 보기</Button></div>)}</section>)}</div></details>}
    <WeekTabs plan={plan} onSelect={manager.goWeek} />
    <section className="hm-week" id={`hm-week-${plan.id}`} role="tabpanel" aria-labelledby={`hm-tab-${plan.id}-${plan.selectedWeek}`} tabIndex={0}>
      <header className="hm-week__heading"><div><span className="hm-eyebrow">WEEK {String(plan.selectedWeek).padStart(2, '0')} / {String(plan.totalWeeks).padStart(2, '0')}</span><h2>{plan.selectedWeek}주차 · {plan.weekTitle}</h2></div><p>예상 소요 시간 {plan.duration}<br />{plan.weekProgress.label}</p></header>
      <details className="hm-supplies"><summary><Package size={16} />{plan.selectedWeek === plan.currentWeek ? '이번 주' : `${plan.selectedWeek}주차`} 준비물 <ChevronDown size={15} /></summary><ul>{plan.supplies.length ? plan.supplies.map((supply, index) => <li key={`${index}-${supply}`}>{supply}</li>) : <li>별도의 준비물이 없어요.</li>}</ul></details>
      {plan.restriction && <p className="hm-notice" role="status">{plan.restriction}</p>}
      <div className="hm-activities">{plan.items.map((item) => <ActivityCard key={item.itemId} item={item} canRecord={plan.canRecord && !manager.statusMutation.isPending} records={records} />)}</div>
      {!plan.items.length && <div className="hm-empty-week"><p>이 주차에는 등록된 활동이 없어요.</p>{plan.canEdit && <Button variant="secondary" onClick={() => guarded(() => setEditOpen(true))}><Plus size={16} />활동 추가</Button>}</div>}
      {plan.completion && <div className="hm-completion" role="status"><CheckCircle2 size={21} /><div><b>{plan.completion}</b>{plan.nextWeek && <Button variant="text" onClick={() => manager.goWeek(plan.nextWeek!)}>다음 주차 보기 →</Button>}</div></div>}
      {plan.planCompletion && <p className="hm-notice" role="status">{plan.planCompletion}</p>}
    </section>
    {editOpen && <PlanEditor plan={plan} onClose={() => setEditOpen(false)} />}
    {pendingAction && <DraftPrompt cancel={() => setPendingAction(null)} discard={() => { records.discard(); pendingAction(); setPendingAction(null); }} save={async () => { if (!await records.saveAll()) return false; pendingAction(); setPendingAction(null); return true; }} />}
  </>;
}

export function MyHobbiesPage() {
  const manager = useHobbyManagement();
  const [pickerOpen, setPickerOpen] = useState(false);
  const { plan, list, hobbiesQuery, planQuery } = manager;
  const sidebar = <>
    {hobbiesQuery.isPending ? <LoadingPlan /> : hobbiesQuery.isError && !hobbiesQuery.data ? <div className="hm-query-error" role="alert"><h2>내 취미 목록을 불러오지 못했어요</h2><p>{managementError(hobbiesQuery.error)}</p><Button variant="secondary" onClick={() => void hobbiesQuery.refetch()}>목록 다시 시도</Button></div>
      : <HobbySidebar list={list} selectedId={manager.hobbyId} count={manager.inProgressCount} max={manager.maxInProgressCount} unsave={(id) => manager.unsaveMutation.mutate(id)} removingId={manager.unsaveMutation.isPending ? manager.unsaveMutation.variables : undefined} removeError={manager.unsaveMutation.isError ? managementError(manager.unsaveMutation.error) : undefined} onSelect={() => setPickerOpen(false)} />}
  </>;
  return <div className="site-shell hobby-management">
    <AppHeader />
    <main className="page-container hm-layout">
      <aside className="hm-desktop-sidebar">{sidebar}</aside>
      <div className="hm-main">
        <div className="hm-mobile-switch"><div><small>내 취미</small><b>{list.plans.find((hobby) => hobby.id === manager.hobbyId)?.name ?? '나의 취미 계획'}</b></div><Button variant="secondary" onClick={() => setPickerOpen(true)}>취미 변경 <ChevronsUpDown size={15} /></Button></div>
        {manager.invalidPlan ? <div className="hm-empty" role="alert"><h1>계획 연결을 확인해 주세요</h1><p>주소의 취미와 계획이 일치하지 않거나 올바르지 않은 주소예요.</p><Button to="/my-hobbies" variant="secondary">내 취미 목록으로</Button></div>
          : planQuery.isError && !plan ? <div className="hm-empty" role="alert"><h1>계획을 불러오지 못했어요</h1><p>{managementError(planQuery.error)}</p><div className="hm-actions"><Button variant="secondary" onClick={() => void planQuery.refetch()}>다시 시도</Button><Button to="/my-hobbies" variant="text">내 취미 목록으로</Button></div></div>
          : (hobbiesQuery.isPending || (manager.planId > 0 && planQuery.isPending) || (!manager.hobbyId && list.plans.length > 0)) ? <LoadingPlan />
          : plan ? <PlanContent key={`${plan.id}-${plan.selectedWeek}`} plan={plan} manager={manager} />
          : <div className="hm-empty"><span className="hm-empty__icon"><Leaf size={32} /></span><span className="hm-eyebrow">A LITTLE TIME FOR YOU</span><h1>{manager.hobbyId ? '연결된 계획을 찾을 수 없어요' : list.saved.length ? '관심 취미를 시작해 보세요' : hobbiesQuery.isError ? '취미 목록을 먼저 확인해 주세요' : '아직 등록한 취미가 없어요'}</h1><p>{manager.hobbyId ? '취미 상세에서 연결된 계획을 확인해 주세요.' : '좋아하는 일을 발견하고, 작은 활동부터 하나씩 이어가요.'}</p><Button to={manager.hobbyId ? `/hobbies/${manager.hobbyId}` : '/hobbies'}>{manager.hobbyId ? '취미 상세 보기' : '취미 찾기'}</Button></div>}
        {planQuery.isError && plan && <p className="hm-error" role="alert">최신 계획을 불러오지 못했어요. 작성 중인 내용은 유지돼요. <button type="button" onClick={() => void planQuery.refetch()}>다시 시도</button></p>}
      </div>
    </main>
    {pickerOpen && <ManagementDialog title="취미 변경" panel onClose={() => setPickerOpen(false)}>{sidebar}</ManagementDialog>}
  </div>;
}

export const HobbyPlanPage = MyHobbiesPage;
export const TodoFocusPage = MyHobbiesPage;
