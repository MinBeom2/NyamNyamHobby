import { Check, ChevronDown, Image, Pause, Play, RotateCcw } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { activeHobbyFixtures, type ActiveHobby, type TodoItem } from '../../features/activity/model/activityFixtures';
import { getUserHobbies, userQueryKeys, type UserHobbyDto } from '../../features/user/api/userApi';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';

function HobbyProgressCard({ hobby }: { hobby: ActiveHobby }) {
  const progress = Math.round((hobby.completed / hobby.totalTodos) * 100);
  return <article className="activity-card"><div className="activity-card__icon"><Image aria-hidden="true" /><span>{hobby.name}</span></div><div className="activity-card__body"><div className="activity-card__title"><h2>{hobby.name}</h2><span className={hobby.status === '진행 중' ? 'badge' : 'badge badge--paused'}>{hobby.status}</span></div><p>{hobby.week}주차 · {hobby.totalWeeks}주 계획 · {hobby.completed}/{hobby.totalTodos} 완료</p><div className="progress-track"><span style={{ width: `${progress}%` }} /></div></div><div className="activity-card__actions"><Link className="button button--primary" to={`/my-hobbies/${hobby.id}/plan`}>{hobby.status === '진행 중' ? '이어하기' : '다시 시작'}</Link><button className="button button--secondary" type="button">{hobby.status === '진행 중' ? <><Pause /> 일시중지</> : '그만두기'}</button></div></article>;
}

function UserHobbyProgressCard({ hobby }: { hobby: UserHobbyDto }) {
  const progress = Math.max(0, Math.min(100, Math.round(hobby.progressRate ?? 0)));
  const status = hobby.status === 'PAUSED' ? '일시중지' : '진행 중';
  return <article className="activity-card"><div className="activity-card__icon">{hobby.thumbnailUrl ? <img src={hobby.thumbnailUrl} alt="" /> : <Image aria-hidden="true" />}<span>{hobby.name}</span></div><div className="activity-card__body"><div className="activity-card__title"><h2>{hobby.name}</h2><span className={hobby.status === 'PAUSED' ? 'badge badge--paused' : 'badge'}>{status}</span></div><p>{hobby.skillLevel || '입문'} · 진행률 {progress}%</p><div className="progress-track"><span style={{ width: `${progress}%` }} /></div></div><div className="activity-card__actions"><Link className="button button--primary" to={`/hobbies/${hobby.hobbyId}`}>취미 정보 보기</Link></div></article>;
}

export function MyHobbiesPage() {
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const hobbies = hobbiesQuery.data ?? [];
  const inProgress = hobbies.filter((item) => item.status === 'IN_PROGRESS');
  const paused = hobbies.filter((item) => item.status === 'PAUSED');
  const saved = hobbies.filter((item) => item.status === 'SAVED');
  return <div className="site-shell"><AppHeader authenticated notificationCount={3} /><main className="page-container activity-page"><div className="page-title-row"><div><h1>내 취미</h1><p>진행 중인 계획과 관심 취미를 한곳에서 확인해요.</p></div><span className="count-pill">진행 중 {inProgress.length} / 3</span></div>
    <section><h2 className="content-title">진행 중</h2><div className="activity-list">{inProgress.map((hobby) => <UserHobbyProgressCard hobby={hobby} key={hobby.userHobbyId} />)}{!hobbiesQuery.isLoading && inProgress.length === 0 && <p>진행 중인 취미가 없어요.</p>}</div></section>
    <section className="section-block"><h2 className="content-title">일시중지</h2><div className="activity-list">{paused.map((hobby) => <UserHobbyProgressCard hobby={hobby} key={hobby.userHobbyId} />)}{!hobbiesQuery.isLoading && paused.length === 0 && <p>일시중지한 취미가 없어요.</p>}</div></section>
    <section className="section-block"><div className="section-heading"><h2>관심 취미</h2><Link to="/hobbies">전체 보기</Link></div><div className="interest-mini-grid">{saved.map((hobby) => <article key={hobby.userHobbyId}><div>{hobby.thumbnailUrl ? <img src={hobby.thumbnailUrl} alt="" /> : <Image />}</div><h3>{hobby.name}</h3><Button to={`/hobbies/${hobby.hobbyId}`}>상세 보기</Button></article>)}</div>{!hobbiesQuery.isLoading && saved.length === 0 && <p>저장한 관심 취미가 없어요.</p>}</section>
  </main></div>;
}

function TodoRow({ todo, onToggle }: { todo: TodoItem; onToggle: () => void }) {
  return <label className={`plan-todo ${todo.done ? 'done' : ''}`}><button type="button" onClick={onToggle} aria-label={`${todo.title} ${todo.done ? '미완료로 변경' : '완료로 변경'}`}>{todo.done && <Check />}</button><span><b>{todo.title}</b><small>{todo.description}</small></span><textarea aria-label={`${todo.title} 활동 메모`} placeholder="활동하며 느낀 점을 남겨 보세요" /></label>;
}

export function HobbyPlanPage() {
  const { hobbyId } = useParams();
  const hobby = activeHobbyFixtures.find((item) => item.id === Number(hobbyId)) ?? activeHobbyFixtures[0];
  const [week, setWeek] = useState(hobby.week);
  const [completed, setCompleted] = useState<number[]>(hobby.plans.flatMap((plan) => plan.todos.filter((todo) => todo.done).map((todo) => todo.id)));
  const currentPlan = hobby.plans.find((plan) => plan.week === week) ?? hobby.plans[0];
  const doneCount = useMemo(() => currentPlan.todos.filter((todo) => completed.includes(todo.id)).length, [completed, currentPlan.todos]);

  return <div className="site-shell"><AppHeader authenticated notificationCount={3} /><main className="page-container plan-page"><div className="plan-header"><div><Link className="back-link" to="/my-hobbies">내 취미</Link><h1>{hobby.name} · 입문 {hobby.totalWeeks}주 계획</h1><div className="progress-track"><span style={{ width: `${Math.round((hobby.completed / hobby.totalTodos) * 100)}%` }} /></div></div><Button variant="secondary" to={`/hobbies/${hobby.id}`}>취미 정보 보기</Button></div>
    <div className="plan-layout"><section className="plan-main"><div className="plan-week-title"><div><span>{week}주차</span><h2>{currentPlan.title}</h2></div><b>{doneCount}/{currentPlan.todos.length} 완료</b></div><div className="plan-todo-list">{currentPlan.todos.map((todo) => <TodoRow key={todo.id} todo={{ ...todo, done: completed.includes(todo.id) }} onToggle={() => setCompleted((items) => items.includes(todo.id) ? items.filter((id) => id !== todo.id) : [...items, todo.id])} />)}</div><div className="plan-actions"><Button>저장</Button><Button variant="secondary">일시중지</Button></div></section>
      <aside className="plan-sidebar"><h2>이번 주 준비물</h2><ul><li>편한 운동복</li><li>양말과 물</li><li>도전할 마음</li></ul><div className="weekly-cheer"><Image /><b>{week}주차 완료!</b><p>조금씩 꾸준히 해내고 있어요.</p></div><Button to="/places">장소 찾기</Button></aside></div>
    <section className="all-plan"><div className="section-heading"><h2>전체 계획</h2><span>{hobby.totalWeeks}주 과정</span></div>{hobby.plans.map((plan) => <button type="button" className={plan.week === week ? 'active' : ''} onClick={() => setWeek(plan.week)} key={plan.week}><span>{plan.week}주차</span><b>{plan.title}</b><em>{plan.status}</em><ChevronDown /></button>)}</section>
  </main></div>;
}

export function TodoFocusPage() {
  const { hobbyId } = useParams();
  const hobby = activeHobbyFixtures.find((item) => item.id === Number(hobbyId)) ?? activeHobbyFixtures[0];
  return <div className="site-shell"><AppHeader authenticated /><main className="page-container empty-state"><RotateCcw /><h1>{hobby.name} TODO</h1><p>전체 계획 화면에서 이번 주 활동을 이어서 진행할 수 있어요.</p><Button to={`/my-hobbies/${hobby.id}/plan`}><Play /> 계획 이어가기</Button></main></div>;
}
