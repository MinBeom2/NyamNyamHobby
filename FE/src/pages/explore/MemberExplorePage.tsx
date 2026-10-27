import { Bookmark, Check, ChevronDown, Heart, RotateCcw, ThumbsDown } from 'lucide-react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { logout } from '../../features/auth/api/authApi';
import { useAuthStore, type AuthUser } from '../../features/auth/model/authStore';
import {
  deleteShortReaction,
  explorationQueryKeys,
  finishExploration,
  getExplorationProgress,
  getNextShort,
  recordShortView,
  setShortReaction,
  startExploration,
  type ReactionType,
  type RecommendationResponse,
  type ShortDetail,
} from '../../features/exploration/api/explorationApi';
import { ApiError } from '../../shared/api/client';
import { characterAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';

export function MemberExplorePage({ user }: { user: AuthUser }) {
  const navigate = useNavigate();
  const clearSession = useAuthStore((state) => state.clearSession);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [maxCount, setMaxCount] = useState(0);
  const [viewedCount, setViewedCount] = useState(0);
  const [current, setCurrent] = useState<ShortDetail | null>(null);
  const [recommendation, setRecommendation] = useState<RecommendationResponse | null>(null);
  const startedRef = useRef(false);
  const shownAtRef = useRef(Date.now());

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession();
      navigate('/', { replace: true });
    },
  });

  const startMutation = useMutation({
    mutationFn: startExploration,
    onSuccess: (data) => {
      setSessionId(data.sessionId);
      setMaxCount(data.maxCount);
      setViewedCount(data.viewedCount);
      setCurrent(data.short);
      setRecommendation(null);
      shownAtRef.current = Date.now();
    },
  });

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;
    startMutation.mutate();
  }, [startMutation]);

  const progressQuery = useQuery({
    queryKey: explorationQueryKeys.progress(sessionId ?? 0),
    queryFn: () => getExplorationProgress(sessionId as number),
    enabled: sessionId !== null && recommendation === null,
    retry: false,
  });

  const reactionMutation = useMutation({
    mutationFn: async (type: ReactionType) => {
      if (!current || sessionId === null) throw new Error('탐색 세션이 준비되지 않았습니다.');
      if (current.myReaction === type) return deleteShortReaction(current.shortId);
      return setShortReaction(current.shortId, { type, sessionId });
    },
    onSuccess: (response) => {
      setCurrent((short) => short ? { ...short, myReaction: response.type } : short);
      void progressQuery.refetch();
    },
  });

  const advanceMutation = useMutation({
    mutationFn: async () => {
      if (!current || sessionId === null) throw new Error('탐색 세션이 준비되지 않았습니다.');
      await recordShortView(sessionId, {
        shortId: current.shortId,
        watchedSeconds: Math.max(0, Math.round((Date.now() - shownAtRef.current) / 1000)),
        playFailed: false,
      });

      if (viewedCount >= maxCount) {
        return { recommendation: await finishExploration(sessionId), next: null };
      }
      const next = await getNextShort(sessionId);
      if (next.finished || !next.short) {
        return { recommendation: await finishExploration(sessionId), next };
      }
      return { recommendation: null, next };
    },
    onSuccess: ({ recommendation: completedRecommendation, next }) => {
      if (completedRecommendation) {
        setRecommendation(completedRecommendation);
        setCurrent(null);
      } else if (next?.short) {
        setCurrent(next.short);
        setViewedCount(next.viewedCount);
        shownAtRef.current = Date.now();
        void progressQuery.refetch();
      }
    },
  });

  const restart = () => {
    setSessionId(null);
    setCurrent(null);
    setRecommendation(null);
    startMutation.mutate();
  };

  const error = startMutation.error ?? reactionMutation.error ?? advanceMutation.error;

  return <div className="site-shell">
    <AppHeader authenticated onLogout={() => logoutMutation.mutate()} logoutPending={logoutMutation.isPending} />
    <main className="page-container explore-page">
      {recommendation ? <RecommendationComplete recommendation={recommendation} onRestart={restart} />
        : startMutation.isError ? <ExploreError error={startMutation.error} onRetry={restart} />
        : startMutation.isPending || !current ? <ExploreStatus />
        : <>
          <ExploreHeading user={user} current={viewedCount} max={maxCount} />
          <div className="explore-layout">
            <MemberTastePanel progress={progressQuery.data} />
            <ServerShortPlayer short={current} />
            <aside className="short-actions" aria-label="영상 반응">
              <ReactionButton active={current.myReaction === 'LIKE'} disabled={reactionMutation.isPending || advanceMutation.isPending} label="좋아요" onClick={() => reactionMutation.mutate('LIKE')}><Heart fill={current.myReaction === 'LIKE' ? 'currentColor' : 'none'} /></ReactionButton>
              <ReactionButton active={current.myReaction === 'DISLIKE'} disabled={reactionMutation.isPending || advanceMutation.isPending} label="별로예요" onClick={() => reactionMutation.mutate('DISLIKE')}><ThumbsDown fill={current.myReaction === 'DISLIKE' ? 'currentColor' : 'none'} /></ReactionButton>
              <ReactionButton active={false} disabled label="보관 준비 중" onClick={() => undefined}><Bookmark /></ReactionButton>
              <button className="short-actions__next" type="button" disabled={advanceMutation.isPending} onClick={() => advanceMutation.mutate()}><ChevronDown />{advanceMutation.isPending ? '처리 중…' : viewedCount >= maxCount ? '결과 보기' : '다음 영상'}</button>
            </aside>
          </div>
          {error && <p className="explore-api-error" role="alert">{getApiErrorMessage(error)}</p>}
        </>}
    </main>
  </div>;
}

function ExploreHeading({ user, current, max }: { user: AuthUser; current: number; max: number }) {
  const percent = max > 0 ? Math.min(100, (current / max) * 100) : 0;
  return <header className="explore-heading">
    <div><span className="eyebrow">실시간 취향 탐색</span><h1>{user.nickname}님을 위한 숏폼이에요</h1><p>좋아요와 별로예요 반응이 서버에 저장되고 추천 결과에 반영돼요.</p></div>
    <div className="explore-progress" aria-label={`${current}/${max}번째 영상`}><strong>{current}</strong><span>/ {max}</span><div><i style={{ width: `${percent}%` }} /></div></div>
  </header>;
}

function MemberTastePanel({ progress }: { progress?: Awaited<ReturnType<typeof getExplorationProgress>> }) {
  const likes = progress?.reactedHobbies.filter((item) => item.reaction === 'LIKE').length ?? 0;
  const dislikes = progress?.reactedHobbies.filter((item) => item.reaction === 'DISLIKE').length ?? 0;
  return <aside className="taste-panel">
    <h2>서버 탐색 현황</h2><p>실제 시청 및 반응 기록을 기준으로 계산해요.</p>
    <div className="taste-panel__chips">{(progress?.topInterests.length ? progress.topInterests : [{ hobbyName: '탐색 중', percent: 0 }]).map((item) => <span key={item.hobbyName}>{item.hobbyName}{item.percent > 0 ? ` ${item.percent}%` : ''}</span>)}</div>
    <dl>{(progress?.topInterests ?? []).map((item) => <div key={item.hobbyName}><dt>{item.hobbyName}</dt><dd>{item.percent}%</dd></div>)}</dl>
    <div className="taste-score"><div><span>관심 반응</span><strong>{likes}</strong></div><div><span>비선호 반응</span><strong>{dislikes}</strong></div></div>
  </aside>;
}

function ServerShortPlayer({ short }: { short: ShortDetail }) {
  const duration = short.duration === null ? '--:--' : `${Math.floor(short.duration / 60).toString().padStart(2, '0')}:${(short.duration % 60).toString().padStart(2, '0')}`;
  return <section className="short-player short-player--video" aria-label={`${short.hobby?.name ?? '취미'} 숏폼 영상`}>
    <iframe className="short-player__video" src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(short.youtubeVideoId)}?rel=0&playsinline=1`} title={short.title} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen />
    <div className="short-player__top"><span>{short.hobby?.name ?? '새로운 취미'}</span><small>{duration}</small></div>
    <div className="short-player__copy"><div className="short-player__tags">{short.hobby?.category && <span>#{short.hobby.category}</span>}</div><h2>{short.title}</h2></div>
  </section>;
}

function ReactionButton({ active, disabled, label, onClick, children }: { active: boolean; disabled: boolean; label: string; onClick: () => void; children: React.ReactNode }) {
  return <button type="button" disabled={disabled} className={active ? 'short-action active' : 'short-action'} aria-pressed={active} onClick={onClick}>{children}<span>{label}</span></button>;
}

function RecommendationComplete({ recommendation, onRestart }: { recommendation: RecommendationResponse; onRestart: () => void }) {
  return <section className="explore-complete">
    <div className="explore-complete__icon"><Check /></div><img src={characterAssets.celebration} alt="탐색 완료를 축하하는 냠냠취미 캐릭터" />
    <span className="eyebrow">서버 추천 생성 완료</span><h1>탐색을 마쳤어요!</h1><p>{recommendation.reactionCount}개의 반응을 바탕으로 추천을 생성했어요.</p>
    <div className="explore-complete__chips">{(recommendation.items.length ? recommendation.items.slice(0, 3).map((item) => item.hobbyName) : ['추천 계산 중']).map((hobby) => <span key={hobby}>{hobby}</span>)}</div>
    <div className="explore-complete__actions"><button className="button button--secondary" type="button" onClick={onRestart}><RotateCcw size={16} />다시 탐색하기</button><Link className="button button--primary" to="/home">홈으로 가기</Link></div>
  </section>;
}

function ExploreStatus() {
  return <section className="explore-status" aria-live="polite"><div className="status-icon status-icon--loading" /><h1>숏폼을 준비하고 있어요</h1><p>서버에서 탐색 세션과 첫 영상을 불러오는 중이에요.</p></section>;
}

function ExploreError({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  return <section className="explore-status" role="alert"><h1>숏폼을 불러오지 못했어요</h1><p>{getApiErrorMessage(error)}</p><button className="button button--primary" type="button" onClick={onRetry}>다시 시도</button></section>;
}

function getApiErrorMessage(error: unknown) {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return '요청을 처리하는 중 문제가 발생했어요.';
}
