import { Check, ChevronDown, Image, Pause, Play, RotateCcw } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { activityQueryKeys, getPlan, updatePlanItem, updateUserHobbyStatus, type PlanItemDto } from '../../features/activity/api/activityApi';
import { getUserHobbies, userQueryKeys, type UserHobbyDto } from '../../features/user/api/userApi';
import { AppHeader } from '../../shared/ui/AppHeader';
import { Button } from '../../shared/ui/Button';

function toPercent(rate?: number | null) {
  if (!rate) return 0;
  return Math.max(0, Math.min(100, Math.round(rate <= 1 ? rate * 100 : rate)));
}

function UserHobbyProgressCard({ hobby }: { hobby: UserHobbyDto }) {
  const queryClient = useQueryClient();
  const statusMutation = useMutation({
    mutationFn: (status: 'PAUSED' | 'IN_PROGRESS' | 'STOPPED') => updateUserHobbyStatus(hobby.hobbyId, status),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: userQueryKeys.all }),
        queryClient.invalidateQueries({ queryKey: activityQueryKeys.all }),
      ]);
    },
  });
  const progress = toPercent(hobby.planProgress?.rate ?? hobby.progressRate);
  const paused = hobby.status === 'PAUSED';
  const planLink = hobby.planId ? `/my-hobbies/${hobby.hobbyId}/plan?planId=${hobby.planId}` : `/hobbies/${hobby.hobbyId}`;
  return (
    <article className="activity-card">
      <div className="activity-card__icon">
        {hobby.thumbnailUrl ? <img src={hobby.thumbnailUrl} alt="" /> : <Image aria-hidden="true" />}
        <span>{hobby.name}</span>
      </div>
      <div className="activity-card__body">
        <div className="activity-card__title">
          <h2>{hobby.name}</h2>
          <span className={paused ? 'badge badge--paused' : 'badge'}>{paused ? '일시중지' : '진행 중'}</span>
        </div>
        <p>{hobby.currentWeek ? `${hobby.currentWeek}주차 · ` : ''}{hobby.weekTitle ?? hobby.title ?? hobby.skillLevel}</p>
        <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>
      </div>
      <div className="activity-card__actions">
        <Link className="button button--primary" to={planLink}>{paused ? '다시 시작' : '이어하기'}</Link>
        <button
          className="button button--secondary"
          type="button"
          disabled={statusMutation.isPending}
          onClick={() => statusMutation.mutate(paused ? 'IN_PROGRESS' : 'PAUSED')}
        >
          {paused ? '재개' : <><Pause /> 일시중지</>}
        </button>
      </div>
    </article>
  );
}

export function MyHobbiesPage() {
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const hobbies = hobbiesQuery.data ?? [];
  const inProgress = hobbies.filter((item) => item.status === 'IN_PROGRESS');
  const paused = hobbies.filter((item) => item.status === 'PAUSED');
  const saved = hobbies.filter((item) => item.status === 'SAVED');
  return (
    <div className="site-shell">
      <AppHeader notificationCount={3} />
      <main className="page-container activity-page">
        <div className="page-title-row">
          <div>
            <h1>내 취미</h1>
            <p>진행 중인 계획과 관심 취미를 한곳에서 확인해요.</p>
          </div>
          <span className="count-pill">진행 중 {inProgress.length} / 3</span>
        </div>
        {hobbiesQuery.isError && <p role="alert">내 취미 정보를 불러오지 못했어요.</p>}
        <section>
          <h2 className="content-title">진행 중</h2>
          <div className="activity-list">
            {inProgress.map((hobby) => <UserHobbyProgressCard hobby={hobby} key={hobby.userHobbyId} />)}
            {!hobbiesQuery.isLoading && inProgress.length === 0 && <p>진행 중인 취미가 없어요.</p>}
          </div>
        </section>
        <section className="section-block">
          <h2 className="content-title">일시중지</h2>
          <div className="activity-list">
            {paused.map((hobby) => <UserHobbyProgressCard hobby={hobby} key={hobby.userHobbyId} />)}
            {!hobbiesQuery.isLoading && paused.length === 0 && <p>일시중지한 취미가 없어요.</p>}
          </div>
        </section>
        <section className="section-block">
          <div className="section-heading">
            <h2>관심 취미</h2>
            <Button to="/mypage/interests" variant="secondary">내 취미 선택</Button>
          </div>
          <div className="interest-mini-grid">
            {saved.map((hobby) => (
              <article key={hobby.userHobbyId}>
                <div>{hobby.thumbnailUrl ? <img src={hobby.thumbnailUrl} alt="" /> : <Image />}</div>
                <h3>{hobby.name}</h3>
                <Button to={`/hobbies/${hobby.hobbyId}`}>상세 보기</Button>
              </article>
            ))}
          </div>
          {!hobbiesQuery.isLoading && saved.length === 0 && <p>저장한 관심 취미가 없어요.</p>}
        </section>
      </main>
    </div>
  );
}

function TodoRow({ todo, onChange }: { todo: PlanItemDto; onChange: (request: { status?: 'TODO' | 'DONE'; memo?: string }) => void }) {
  const [memo, setMemo] = useState(todo.memo ?? '');
  useEffect(() => setMemo(todo.memo ?? ''), [todo.itemId, todo.memo]);
  const done = todo.status === 'DONE';
  return (
    <label className={`plan-todo ${done ? 'done' : ''}`}>
      <button type="button" onClick={() => onChange({ status: done ? 'TODO' : 'DONE' })} aria-label={`${todo.title} ${done ? '미완료로 변경' : '완료로 변경'}`}>
        {done && <Check />}
      </button>
      <span>
        <b>{todo.title}</b>
        <small>{todo.description}</small>
      </span>
      <textarea
        aria-label={`${todo.title} 활동 메모`}
        placeholder="활동하며 느낀 점을 남겨 보세요"
        value={memo}
        onChange={(event) => setMemo(event.target.value)}
        onBlur={() => { if (memo !== (todo.memo ?? '')) onChange({ memo }); }}
      />
    </label>
  );
}

export function HobbyPlanPage() {
  const { hobbyId } = useParams();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const userHobby = hobbiesQuery.data?.find((item) => item.hobbyId === Number(hobbyId));
  const planId = Number(searchParams.get('planId') ?? userHobby?.planId ?? 0);
  const [week, setWeek] = useState<number | undefined>();
  const planQuery = useQuery({ queryKey: activityQueryKeys.plan(planId, week), queryFn: () => getPlan(planId, week), enabled: planId > 0 });
  const plan = planQuery.data;
  const paused = userHobby?.status === 'PAUSED';

  useEffect(() => { if (week === undefined && plan?.currentWeek) setWeek(plan.currentWeek); }, [plan?.currentWeek, week]);
  const itemMutation = useMutation({
    mutationFn: ({ itemId, request }: { itemId: number; request: { status?: 'TODO' | 'DONE'; memo?: string } }) => updatePlanItem(planId, itemId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: activityQueryKeys.all }),
  });
  const statusMutation = useMutation({
    mutationFn: (nextStatus: 'PAUSED' | 'IN_PROGRESS') => updateUserHobbyStatus(Number(hobbyId), nextStatus),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: userQueryKeys.all }),
        queryClient.invalidateQueries({ queryKey: activityQueryKeys.all }),
      ]);
    },
  });

  if (hobbiesQuery.isLoading || (planId > 0 && planQuery.isLoading)) return <div className="site-shell"><AppHeader /><main className="page-container empty-state"><p>계획을 불러오는 중이에요.</p></main></div>;
  if (!planId || !plan) return <div className="site-shell"><AppHeader /><main className="page-container empty-state"><h1>진행 중인 계획이 없어요</h1><p>취미 상세에서 계획을 시작하면 여기에서 관리할 수 있어요.</p><Button to={`/hobbies/${hobbyId}`}>취미 상세 보기</Button></main></div>;

  const selectedWeek = plan.selectedWeek ?? week ?? plan.currentWeek;
  const doneCount = plan.weekProgress?.done ?? plan.items.filter((item) => item.status === 'DONE').length;
  const supplies = plan.supplies.length ? plan.supplies : ['별도의 준비물이 없어요.'];
  return (
    <div className="site-shell">
      <AppHeader notificationCount={3} />
      <main className="page-container plan-page">
        <div className="plan-header">
          <div>
            <Link className="back-link" to="/my-hobbies">내 취미</Link>
            <h1>
              {plan.title}
              {paused && <span className="badge badge--paused" style={{ marginLeft: 12, verticalAlign: 'middle', fontSize: 13 }}>일시중지</span>}
            </h1>
            <div className="progress-track"><span style={{ width: `${toPercent(plan.planProgress.rate)}%` }} /></div>
          </div>
          <Button variant="secondary" to={`/hobbies/${plan.hobbyId}`}>취미 정보 보기</Button>
        </div>
        <div className="plan-layout">
          <section className="plan-main">
            <div className="plan-week-title">
              <div>
                <span>{selectedWeek}주차</span>
                <h2>{plan.weekTitle}</h2>
              </div>
              <b>{doneCount}/{plan.weekProgress?.total ?? plan.items.length} 완료</b>
            </div>
            <div className="plan-todo-list">
              {plan.items.map((todo) => (
                <TodoRow key={todo.itemId} todo={todo} onChange={(request) => itemMutation.mutate({ itemId: todo.itemId, request })} />
              ))}
            </div>
            <div className="plan-actions">
              <Button
                variant={paused ? 'primary' : 'secondary'}
                disabled={statusMutation.isPending}
                onClick={() => statusMutation.mutate(paused ? 'IN_PROGRESS' : 'PAUSED')}
              >
                {paused ? (
                  <>
                    <Play size={16} /> 다시 시작
                  </>
                ) : (
                  <>
                    <Pause size={16} /> 일시중지
                  </>
                )}
              </Button>
            </div>
          </section>
          <aside className="plan-sidebar">
            <h2>이번 주 준비물</h2>
            <ul>{supplies.map((supply) => <li key={supply}>{supply}</li>)}</ul>
            <div className="weekly-cheer">
              <Image />
              <b>{selectedWeek}주차</b>
              <p>조금씩 꾸준히 해내고 있어요.</p>
            </div>
            <Button to="/places">장소 찾기</Button>
          </aside>
        </div>
        <section className="all-plan">
          <div className="section-heading">
            <h2>전체 계획</h2>
            <span>{plan.totalWeeks}주 과정</span>
          </div>
          {plan.weeks.map((item) => (
            <button type="button" className={item.weekNo === selectedWeek ? 'active' : ''} onClick={() => setWeek(item.weekNo)} key={item.weekNo}>
              <span>{item.weekNo}주차</span>
              <b>{item.title}</b>
              <em>{item.progress.done}/{item.progress.total} 완료</em>
              <ChevronDown />
            </button>
          ))}
        </section>
      </main>
    </div>
  );
}

export function TodoFocusPage() {
  const { hobbyId } = useParams();
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const hobby = hobbiesQuery.data?.find((item) => item.hobbyId === Number(hobbyId));
  const link = hobby?.planId ? `/my-hobbies/${hobbyId}/plan?planId=${hobby.planId}` : `/hobbies/${hobbyId}`;
  return (
    <div className="site-shell">
      <AppHeader />
      <main className="page-container empty-state">
        <RotateCcw />
        <h1>{hobby?.name ?? '내 취미'} TODO</h1>
        <p>전체 계획 화면에서 이번 주 활동을 이어서 진행할 수 있어요.</p>
        <Button to={link}><Play /> 계획 이어가기</Button>
      </main>
    </div>
  );
}
