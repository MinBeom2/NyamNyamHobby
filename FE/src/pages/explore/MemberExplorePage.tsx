import { Bookmark, Check, ChevronDown, ChevronRight, Heart, Image, ThumbsDown } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { AuthUser } from '../../features/auth/model/authStore';
import type { ExplorationProgress, ExplorationRecommendation, ExplorationShort } from '../../features/exploration/model/explorationModel';
import { useMemberExploration } from '../../features/exploration/hooks/useMemberExploration';
import { useRecommendationResult } from '../../features/recommendation/model/useRecommendationResult';
import { ApiError } from '../../shared/api/client';
import { characterAssets } from '../../shared/assets/assets';
import { useWheelStepNavigation } from '../../shared/hooks/useWheelStepNavigation';
import { AppHeader } from '../../shared/ui/AppHeader';
import { RestartExploreControl } from './RestartExploreControl';

export function MemberExplorePage({ user }: { user: AuthUser }) {
  const exploration = useMemberExploration();

  useWheelStepNavigation({
    enabled: Boolean(exploration.current) && !exploration.recommendation && !exploration.isAdvancePending,
    onNext: () => exploration.advance(),
  });

  return <div className="site-shell">
    <AppHeader />
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
              <ReactionButton active={exploration.current.isSaved} disabled={exploration.isSavePending || exploration.isAdvancePending} label={exploration.isSavePending ? '처리 중…' : exploration.current.isSaved ? '보관 해제' : '보관'} onClick={() => exploration.toggleSave()}><Bookmark fill={exploration.current.isSaved ? 'currentColor' : 'none'} /></ReactionButton>
              <div className="short-actions__scroll-hint" aria-hidden="true"><ChevronDown />아래로 스크롤</div>
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
    <iframe key={short.id} className="short-player__video" src={`https://www.youtube-nocookie.com/embed/${encodeURIComponent(short.youtubeVideoId)}?autoplay=1&mute=1&rel=0&playsinline=1`} title={short.title} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen />
    <div className="short-player__top"><span>{short.hobbyName}</span><small>{short.durationLabel}</small></div>
    <div className="short-player__copy"><div className="short-player__tags">{short.hobbyCategory && <span>#{short.hobbyCategory}</span>}</div><h2>{short.title}</h2></div>
  </section>;
}

function ReactionButton({ active, disabled, label, onClick, children }: { active: boolean; disabled: boolean; label: string; onClick: () => void; children: React.ReactNode }) {
  return <button type="button" disabled={disabled} className={active ? 'short-action active' : 'short-action'} aria-pressed={active} onClick={onClick}>{children}<span>{label}</span></button>;
}

function RecommendationComplete({ recommendation, onRestart }: { recommendation: ExplorationRecommendation; onRestart: () => void }) {
  const resultQuery = useRecommendationResult(recommendation.recommendationId);
  const result = resultQuery.data;
  return <section className="explore-complete">
    <div className="explore-complete__icon"><Check /></div><img src={characterAssets.celebration} alt="탐색 완료를 축하하는 냠냠취미 캐릭터" />
    <h1>탐색을 마쳤어요!</h1>
    {resultQuery.isPending && <p role="status">추천 결과를 불러오고 있어요.</p>}
    {resultQuery.isError && <p role="alert">추천 결과를 불러오지 못했어요.</p>}
    {result && <>
      <ul className="recommend-result-list" aria-label="추천 취미">
        {result.interests.map((hobby) => <li key={hobby.hobbyId}>
          <Link className="recommend-result" to={`/hobbies/${hobby.hobbyId}`} aria-label={`${hobby.name} 취미 정보 보기`}>
            <span className="recommend-result__thumb">{hobby.thumbnailUrl ? <img src={hobby.thumbnailUrl} alt="" /> : <Image aria-hidden="true" />}</span>
            <span className="recommend-result__body"><strong>{hobby.name}</strong><span>{hobby.reason}</span></span>
            <ChevronRight className="recommend-result__arrow" aria-hidden="true" />
          </Link>
        </li>)}
      </ul>
      {result.reasonPending && <p className="recommend-result__pending" role="status">추천 이유를 정리하고 있어요.</p>}
      {result.related.length > 0 && <div className="recommend-related">
        <h2>그 외의 유사한 취미들이에요</h2>
        <div className="explore-complete__chips">{result.related.map((hobby) => <Link key={hobby.hobbyId} to={`/hobbies/${hobby.hobbyId}`}>{hobby.name}</Link>)}</div>
      </div>}
    </>}
    <div className="explore-complete__actions"><RestartExploreControl onConfirm={onRestart} /><Link className="button button--primary" to="/home">홈으로 가기</Link></div>
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
