import { Bookmark, Check, ChevronDown, Heart, Play, RotateCcw, ThumbsDown } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { logout } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/model/authStore';
import { useOnboardingStore } from '../../features/onboarding/model/onboardingStore';
import { characterAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';
import { getExploreShorts } from './exploreFixtures';
import { MemberExplorePage } from './MemberExplorePage';

type Reaction = 'LIKE' | 'DISLIKE';

export function ExplorePage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const guestMode = searchParams.get('guest') === 'true';
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const hobbies = useOnboardingStore((state) => state.hobbies);
  const conditions = useOnboardingStore();
  const shorts = useMemo(() => getExploreShorts(hobbies), [hobbies]);
  const [index, setIndex] = useState(0);
  const [reactions, setReactions] = useState<Record<string, Reaction>>({});
  const [savedIds, setSavedIds] = useState<string[]>([]);
  const [completed, setCompleted] = useState(false);
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession();
      navigate('/', { replace: true });
    },
  });

  if (user && !guestMode) return <MemberExplorePage user={user} />;
  if (!user && !guestMode) return <Navigate replace to="/onboarding/interests?mode=guest" />;

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
    setSavedIds([]);
  };

  return (
    <div className="site-shell">
      <AppHeader
        authenticated={Boolean(user)}
        onLogout={user ? () => logoutMutation.mutate() : undefined}
        onBrandClick={guestMode ? conditions.resetOnboarding : undefined}
        logoutPending={logoutMutation.isPending}
      />
      <main className="page-container explore-page">
        {completed ? (
          <ExploreComplete guestMode={guestMode} hobbies={hobbies} reactionCount={Object.keys(reactions).length} onRestart={restart} />
        ) : (
          <>
            <header className="explore-heading">
              <div>
                <span className="eyebrow">취향 탐색</span>
                <h1>{guestMode ? '몇 번의 반응으로 취향을 찾아볼까요?' : `${user?.nickname ?? '회원'}님을 위한 숏폼이에요`}</h1>
                <p>{guestMode ? '선택한 관심 취미와 이용 상황을 먼저 반영했어요.' : '좋아요와 별로예요 반응이 다음 추천에 반영돼요.'}</p>
              </div>
              <div className="explore-progress" aria-label={`${index + 1}/${shorts.length}번째 영상`}><strong>{index + 1}</strong><span>/ {shorts.length}</span><div><i style={{ width: `${((index + 1) / shorts.length) * 100}%` }} /></div></div>
            </header>

            <div className="explore-layout">
              <aside className="taste-panel">
                <h2>나의 탐색 기준</h2>
                <p>관심 취미와 반응을 바탕으로 순서를 조정해요.</p>
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
            {guestMode && <p className="guest-explore-note">비회원 체험은 현재 브라우저에서만 동작하며 서버에 저장되지 않아요.</p>}
          </>
        )}
      </main>
    </div>
  );
}

function ActionButton({ active, label, onClick, children }: { active: boolean; label: string; onClick: () => void; children: React.ReactNode }) {
  return <button type="button" className={active ? 'short-action active' : 'short-action'} aria-pressed={active} onClick={onClick}>{children}<span>{label}</span></button>;
}

function ExploreComplete({ guestMode, hobbies, reactionCount, onRestart }: { guestMode: boolean; hobbies: string[]; reactionCount: number; onRestart: () => void }) {
  return <section className="explore-complete">
    <div className="explore-complete__icon"><Check /></div>
    <img src={characterAssets.celebration} alt="탐색 완료를 축하하는 냠냠취미 캐릭터" />
    <span className="eyebrow">취향 탐색 완료</span>
    <h1>탐색을 마쳤어요!</h1>
    <p>{reactionCount > 0 ? `${reactionCount}개의 반응과 선택한 조건을 바탕으로 취향을 정리했어요.` : '선택한 관심 취미와 이용 상황을 바탕으로 취향을 정리했어요.'}</p>
    <div className="explore-complete__chips">{(hobbies.length ? hobbies.slice(0, 3) : ['새로운 취미', '가볍게 시작']).map((hobby) => <span key={hobby}>{hobby}</span>)}</div>
    <div className="explore-complete__actions">
      <button className="button button--secondary" type="button" onClick={onRestart}><RotateCcw size={16} />다시 탐색하기</button>
      <Link className="button button--primary" to={guestMode ? '/signup' : '/home'}>{guestMode ? '회원가입하기' : '홈으로 가기'}</Link>
    </div>
  </section>;
}
