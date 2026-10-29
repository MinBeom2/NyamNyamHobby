import { Bookmark, Check, ChevronDown, Heart, RotateCcw, ThumbsDown } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { logout } from '../../features/auth/api/authApi';
import { useAuthStore, type AuthUser } from '../../features/auth/model/authStore';
import type { ExplorationProgress, ExplorationRecommendation, ExplorationShort } from '../../features/exploration/model/explorationModel';
import { useMemberExploration } from '../../features/exploration/hooks/useMemberExploration';
import { ApiError } from '../../shared/api/client';
import { characterAssets } from '../../shared/assets/assets';
import { AppHeader } from '../../shared/ui/AppHeader';

export function MemberExplorePage({ user }: { user: AuthUser }) {
  const navigate = useNavigate();
  const clearSession = useAuthStore((state) => state.clearSession);
  const exploration = useMemberExploration();

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession();
      navigate('/', { replace: true });
    },
  });

  return <div className="site-shell">
    <AppHeader authenticated onLogout={() => logoutMutation.mutate()} logoutPending={logoutMutation.isPending} />
    <main className="page-container explore-page">
      {exploration.recommendation ? <RecommendationComplete recommendation={exploration.recommendation} onRestart={exploration.restart} />
        : exploration.isStartError ? <ExploreError error={exploration.startError} onRetry={exploration.restart} />
        : exploration.isStarting || !exploration.current ? <ExploreStatus />
        : <>
          <ExploreHeading user={user} current={exploration.viewedCount} max={exploration.maxCount} />
          <div className="explore-layout">
            <MemberTastePanel progress={exploration.progress} />
            <ServerShortPlayer short={exploration.current} />
            <aside className="short-actions" aria-label="영상 반응">
              <ReactionButton active={exploration.current.reaction === 'LIKE'} disabled={exploration.isReactionPending || exploration.isAdvancePending} label="좋아요" onClick={() => exploration.react('LIKE')}><Heart fill={exploration.current.reaction === 'LIKE' ? 'currentColor' : 'none'} /></ReactionButton>
              <ReactionButton active={exploration.current.reaction === 'DISLIKE'} disabled={exploration.isReactionPending || exploration.isAdvancePending} label="별로예요" onClick={() => exploration.react('DISLIKE')}><ThumbsDown fill={exploration.current.reaction === 'DISLIKE' ? 'currentColor' : 'none'} /></ReactionButton>
              <ReactionButton active={false} disabled label="보관 준비 중" onClick={() => undefined}><Bookmark /></ReactionButton>
              <button className="short-actions__next" type="button" disabled={exploration.isAdvancePending} onClick={() => exploration.advance()}><ChevronDown />{exploration.isAdvancePending ? '처리 중…' : exploration.viewedCount >= exploration.maxCount ? '결과 보기' : '다음 영상'}</button>
            </aside>
          </div>
          {exploration.error && <p className="explore-api-error" role="alert">{getApiErrorMessage(exploration.error)}</p>}
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

function MemberTastePanel({ progress }: { progress?: ExplorationProgress }) {
  return <aside className="taste-panel">
    <h2>서버 탐색 현황</h2><p>실제 시청 및 반응 기록을 기준으로 계산해요.</p>
    <div className="taste-panel__chips">{(progress?.interests.length ? progress.interests : [{ hobbyName: '탐색 중', percent: 0 }]).map((item) => <span key={item.hobbyName}>{item.hobbyName}{item.percent > 0 ? ` ${item.percent}%` : ''}</span>)}</div>
    <dl>{(progress?.interests ?? []).map((item) => <div key={item.hobbyName}><dt>{item.hobbyName}</dt><dd>{item.percent}%</dd></div>)}</dl>
    <div className="taste-score"><div><span>관심 반응</span><strong>{progress?.likeCount ?? 0}</strong></div><div><span>비선호 반응</span><strong>{progress?.dislikeCount ?? 0}</strong></div></div>
  </aside>;
}

function ServerShortPlayer({ short }: { short: ExplorationShort }) {
  return <section className="short-player short-player--video" aria-label={`${short.hobbyName} 숏폼 영상`}>
    <iframe className="short-player__video" src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(short.youtubeVideoId)}?rel=0&playsinline=1`} title={short.title} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen />
    <div className="short-player__top"><span>{short.hobbyName}</span><small>{short.durationLabel}</small></div>
    <div className="short-player__copy"><div className="short-player__tags">{short.hobbyCategory && <span>#{short.hobbyCategory}</span>}</div><h2>{short.title}</h2></div>
  </section>;
}

function ReactionButton({ active, disabled, label, onClick, children }: { active: boolean; disabled: boolean; label: string; onClick: () => void; children: React.ReactNode }) {
  return <button type="button" disabled={disabled} className={active ? 'short-action active' : 'short-action'} aria-pressed={active} onClick={onClick}>{children}<span>{label}</span></button>;
}

function RecommendationComplete({ recommendation, onRestart }: { recommendation: ExplorationRecommendation; onRestart: () => void }) {
  return <section className="explore-complete">
    <div className="explore-complete__icon"><Check /></div><img src={characterAssets.celebration} alt="탐색 완료를 축하하는 냠냠취미 캐릭터" />
    <span className="eyebrow">서버 추천 생성 완료</span><h1>탐색을 마쳤어요!</h1><p>{recommendation.reactionCount}개의 반응을 바탕으로 추천을 생성했어요.</p>
    <div className="explore-complete__chips">{(recommendation.hobbyNames.length ? recommendation.hobbyNames : ['추천 계산 중']).map((hobby) => <span key={hobby}>{hobby}</span>)}</div>
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
