import { Bookmark, ChevronDown, Heart, ThumbsDown } from 'lucide-react';
import { Navigate } from 'react-router-dom';
import type { AuthUser } from '../../features/auth/model/authStore';
import type { ExplorationProgress, ExplorationShort } from '../../features/exploration/model/explorationModel';
import { useMemberExploration } from '../../features/exploration/hooks/useMemberExploration';
import { YouTubeShortPlayer } from '../../features/exploration/ui/YouTubeShortPlayer';
import { ApiError } from '../../shared/api/client';
import { AppHeader } from '../../shared/ui/AppHeader';
import { RestartExploreControl } from './RestartExploreControl';

export function MemberExplorePage({ user }: { user: AuthUser }) {
  const exploration = useMemberExploration();
  return <div className="site-shell">
    <AppHeader />
    <main className="page-container explore-page">
      {/* 결과는 주소(/explore/result/:id)로 넘긴다. replace라 결과 화면에서 뒤로가기하면 끝난 쇼츠 화면이 아니라 탐색 전 페이지로 간다 */}
      {exploration.recommendation ? <Navigate replace to={`/explore/result/${exploration.recommendation.recommendationId}`} />
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
              <ReactionButton active={exploration.current.isSaved} disabled={exploration.isSavePending || exploration.isAdvancePending} label={exploration.isSavePending ? '처리 중…' : exploration.current.isSaved ? '보관 해제' : '보관'} onClick={() => exploration.toggleSave()}><Bookmark fill={exploration.current.isSaved ? 'currentColor' : 'none'} /></ReactionButton>
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
    <div><span className="eyebrow">실시간 취향 탐색</span><h1>{user.nickname}님을 위한 숏폼이에요</h1></div>
    <div className="explore-progress" aria-label={`${current}/${max}번째 영상`}><strong>{current}</strong><span>/ {max}</span><div><i style={{ width: `${percent}%` }} /></div></div>
  </header>;
}

function MemberTastePanel({ progress }: { progress?: ExplorationProgress }) {
  return <aside className="taste-panel">
    <h2>나의 탐색 기준</h2>
    <div className="taste-panel__chips">{(progress?.interests.length ? progress.interests : [{ hobbyName: '탐색 중', percent: 0 }]).map((item) => <span key={item.hobbyName}>{item.hobbyName}{item.percent > 0 ? ` ${item.percent}%` : ''}</span>)}</div>
    <dl>{(progress?.interests ?? []).map((item) => <div key={item.hobbyName}><dt>{item.hobbyName}</dt><dd>{item.percent}%</dd></div>)}</dl>
    <div className="taste-score"><div><span>관심 반응</span><strong>{progress?.likeCount ?? 0}</strong></div><div><span>비선호 반응</span><strong>{progress?.dislikeCount ?? 0}</strong></div></div>
  </aside>;
}

function ServerShortPlayer({ short }: { short: ExplorationShort }) {
  return <section className="short-player short-player--video" aria-label={`${short.hobbyName} 숏폼 영상`}>
    <YouTubeShortPlayer videoId={short.youtubeVideoId} title={short.title} />
    <div className="short-player__top"><span>{short.hobbyName}</span><small>{short.durationLabel}</small></div>
    <div className="short-player__copy"><div className="short-player__tags">{short.hobbyCategory && <span>#{short.hobbyCategory}</span>}</div><h2>{short.title}</h2></div>
  </section>;
}

function ReactionButton({ active, disabled, label, onClick, children }: { active: boolean; disabled: boolean; label: string; onClick: () => void; children: React.ReactNode }) {
  return <button type="button" disabled={disabled} className={active ? 'short-action active' : 'short-action'} aria-pressed={active} onClick={onClick}>{children}<span>{label}</span></button>;
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
