import { Bookmark, Check, ChevronDown, Heart, Play, ThumbsDown } from 'lucide-react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { getCurrentUserProfile, logout } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/model/authStore';
import { useOnboardingStore } from '../../features/onboarding/model/onboardingStore';
import { characterAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';
import { getExploreShorts } from './exploreFixtures';
import { MemberExplorePage } from './MemberExplorePage';
import { RestartExploreControl } from './RestartExploreControl';

type Reaction = 'LIKE' | 'DISLIKE';

export function ExplorePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const guestRequested = searchParams.get('guest') === 'true';
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const clearSession = useAuthStore((state) => state.clearSession);
  const hobbies = useOnboardingStore((state) => state.hobbies);
  const conditions = useOnboardingStore();
  const shorts = useMemo(() => getExploreShorts(hobbies), [hobbies]);
  const [index, setIndex] = useState(0);
  const [reactions, setReactions] = useState<Record<string, Reaction>>({});
  const [savedIds, setSavedIds] = useState<string[]>([]);
  const [completed, setCompleted] = useState(false);
  const sessionQuery = useQuery({
    queryKey: ['auth', 'current-user'],
    queryFn: getCurrentUserProfile,
    enabled: !user,
    retry: false,
  });
  const restoredUser = useMemo(() => sessionQuery.data ? {
    id: sessionQuery.data.id,
    email: sessionQuery.data.email,
    nickname: sessionQuery.data.nickname,
    role: sessionQuery.data.role,
    onboarded: Boolean(sessionQuery.data.conditions),
  } : null, [sessionQuery.data]);
  const activeUser = user ?? restoredUser;
  const guestMode = guestRequested && !activeUser;

  useEffect(() => {
    if (restoredUser) setUser(restoredUser);
  }, [restoredUser, setUser]);
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession();
      navigate('/', { replace: true });
    },
  });

  if (activeUser && !guestMode) return <MemberExplorePage user={activeUser} />;
  if (!activeUser && sessionQuery.isPending) return <ExploreSessionStatus />;
  if (!activeUser && !guestMode) return <Navigate replace to="/onboarding/interests?mode=guest" />;

  const current = shorts[index];
  const isLast = index === shorts.length - 1;
  const react = (reaction: Reaction) => setReactions((state) => ({ ...state, [current.id]: reaction }));
  const next = () => {
    if (isLast) setCompleted(true);
    else setIndex((value) => value + 1);
  };
  const restart = () => {
    setIndex(0);
    setCompleted(false);
    setReactions({});
  };

  return (
    <div className="site-shell">
      <AppHeader
        authenticated={Boolean(activeUser)}
        onLogout={activeUser ? () => logoutMutation.mutate() : undefined}
        onBrandClick={guestMode ? conditions.resetOnboarding : undefined}
        logoutPending={logoutMutation.isPending}
      />
      <main className="page-container explore-page">
        {completed ? (
          <ExploreComplete guestMode={guestMode} hobbies={hobbies} onRestart={restart} />
        ) : (
          <>
            <header className="explore-heading">
              <div>
                <span className="eyebrow">취향 탐색</span>
              </div>
              <div className="explore-progress" aria-label={`${index + 1}/${shorts.length}번째 영상`}><strong>{index + 1}</strong><span>/ {shorts.length}</span><div><i style={{ width: `${((index + 1) / shorts.length) * 100}%` }} /></div></div>
            </header>

            <div className="explore-layout">
              <aside className="taste-panel">
                <h2>나의 탐색 기준</h2>
                <div className="taste-panel__chips">
                  {(hobbies.length ? hobbies : ['새로운 취미']).map((hobby) => <span key={hobby}>{hobby}</span>)}
                </div>
                <dl>
                  <div><dt>활동 시간</dt><dd>{conditions.weeklyHours ? `주 ${conditions.weeklyHours}시간` : '상관없음'}</dd></div>
                  <div><dt>월 예산</dt><dd>{conditions.monthlyBudget ? `${Number(conditions.monthlyBudget).toLocaleString()}원` : '상관없음'}</dd></div>
                  <div><dt>장소</dt><dd>{conditions.placePref === 'INDOOR' ? '실내' : conditions.placePref === 'OUTDOOR' ? '실외' : '상관없음'}</dd></div>
                </dl>
                <div className="taste-score">
                  <div><span>관심 반응</span><strong>{Object.values(reactions).filter((value) => value === 'LIKE').length}</strong></div>
                  <div><span>비선호 반응</span><strong>{Object.values(reactions).filter((value) => value === 'DISLIKE').length}</strong></div>
                </div>
              </aside>

              <section className="short-player" aria-label={`${current.hobby} 숏폼 영상`}>
                <div className="short-player__top"><span>{current.hobby}</span><small>{current.duration}</small></div>
                <button type="button" className="short-player__play" aria-label="영상 재생"><Play fill="currentColor" /></button>
                <div className="short-player__copy"><div className="short-player__tags">{current.tags.map((tag) => <span key={tag}>#{tag}</span>)}</div><h2>{current.title}</h2><p>{current.description}</p></div>
              </section>

              <aside className="short-actions" aria-label="영상 반응">
                <ActionButton active={reactions[current.id] === 'LIKE'} label="좋아요" onClick={() => react('LIKE')}><Heart fill={reactions[current.id] === 'LIKE' ? 'currentColor' : 'none'} /></ActionButton>
                <ActionButton active={reactions[current.id] === 'DISLIKE'} label="별로예요" onClick={() => react('DISLIKE')}><ThumbsDown fill={reactions[current.id] === 'DISLIKE' ? 'currentColor' : 'none'} /></ActionButton>
                <ActionButton active={savedIds.includes(current.id)} label="임시 보관" onClick={() => setSavedIds((ids) => ids.includes(current.id) ? ids.filter((id) => id !== current.id) : [...ids, current.id])}><Bookmark fill={savedIds.includes(current.id) ? 'currentColor' : 'none'} /></ActionButton>
                <button className="short-actions__next" type="button" onClick={next}><ChevronDown />{isLast ? '결과 보기' : '다음 영상'}</button>
              </aside>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

function ExploreSessionStatus() {
  return <div className="site-shell"><AppHeader /><main className="page-container explore-page"><section className="explore-status" aria-live="polite"><div className="status-icon status-icon--loading" /><h1>로그인 상태를 확인하고 있어요</h1></section></main></div>;
}

function ActionButton({ active, label, onClick, children }: { active: boolean; label: string; onClick: () => void; children: React.ReactNode }) {
  return <button type="button" className={active ? 'short-action active' : 'short-action'} aria-pressed={active} onClick={onClick}>{children}<span>{label}</span></button>;
}

function ExploreComplete({ guestMode, hobbies, onRestart }: { guestMode: boolean; hobbies: string[]; onRestart: () => void }) {
  return <section className="explore-complete">
    <div className="explore-complete__icon"><Check /></div>
    <img src={characterAssets.celebration} alt="탐색 완료를 축하하는 냠냠취미 캐릭터" />
    <span className="eyebrow">취향 탐색 완료</span>
    <h1>탐색을 마쳤어요!</h1>
    <div className="explore-complete__chips">{(hobbies.length ? hobbies.slice(0, 3) : ['새로운 취미', '가볍게 시작']).map((hobby) => <span key={hobby}>{hobby}</span>)}</div>
    <div className="explore-complete__actions">
      <RestartExploreControl onConfirm={onRestart} />
      <Link className="button button--primary" to={guestMode ? '/signup' : '/home'}>{guestMode ? '회원가입하기' : '홈으로 가기'}</Link>
    </div>
  </section>;
}
