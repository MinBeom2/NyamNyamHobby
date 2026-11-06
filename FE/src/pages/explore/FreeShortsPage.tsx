import { Bookmark, ChevronDown, ChevronUp } from 'lucide-react';
import { useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/model/authStore';
import type { ExplorationShort } from '../../features/exploration/model/explorationModel';
import { YouTubeShortPlayer } from '../../features/exploration/ui/YouTubeShortPlayer';
import { savedShortQueryKeys } from '../../features/saved-shorts/api/savedShortApi';
import { deleteSavedShort, saveShort } from '../../features/shortform/api/shortformApi';
import { useShortFeed } from '../../features/shortform/model/useShortFeed';
import { userQueryKeys } from '../../features/user/api/userApi';
import { AppHeader } from '../../shared/ui/AppHeader';

/**
 * 상단 '숏폼 탐색'(/shorts). 추천 알고리즘 없이 모든 취미의 공개 숏폼을 섞어서 끝없이 넘겨 본다.
 * 진행 표시·탐색 기준 패널이 없고, 좋아요·별로예요도 받지 않는다(맞춤 추천에 영향 없음). 회원은 보관만 할 수 있다.
 * 맞춤 추천을 다시 받는 숏폼 탐색은 홈의 '숏폼 추천 다시받기'(/explore)다.
 */
export function FreeShortsPage() {
  const user = useAuthStore((state) => state.user);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const feed = useShortFeed();
  const current = feed.current;

  const saveMutation = useMutation({
    mutationFn: (short: ExplorationShort) => short.isSaved ? deleteSavedShort(short.id) : saveShort(short.id),
    onSuccess: (response) => {
      feed.markSaved(response.shortId, response.isSaved);
      void queryClient.invalidateQueries({ queryKey: savedShortQueryKeys.all });
      void queryClient.invalidateQueries({ queryKey: userQueryKeys.summary() });
    },
  });

  // ↓·↑ 키로도 넘긴다(입력 중일 때는 제외)
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) return;
      if (event.key === 'ArrowDown') { event.preventDefault(); feed.next(); }
      if (event.key === 'ArrowUp') { event.preventDefault(); feed.previous(); }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [feed]);

  const toggleSave = () => {
    if (!current) return;
    if (!user) { navigate('/login'); return; }
    saveMutation.mutate(current);
  };

  return <div className="site-shell">
    <AppHeader />
    <main className="page-container explore-page free-shorts-page">
      <header className="explore-heading">
        <div><span className="eyebrow">숏폼 탐색</span><h1>모든 취미의 숏폼을 자유롭게 둘러보세요</h1></div>
      </header>
      {current ? <div className="explore-layout">
        <div aria-hidden="true" />
        <section className="short-player short-player--video" aria-label={`${current.hobbyName} 숏폼 영상`}>
          <YouTubeShortPlayer videoId={current.youtubeVideoId} title={current.title} />
          <div className="short-player__top"><span>{current.hobbyName}</span><small>{current.durationLabel}</small></div>
          <div className="short-player__copy"><div className="short-player__tags">{current.hobbyCategory && <span>#{current.hobbyCategory}</span>}</div><h2>{current.title}</h2></div>
        </section>
        <aside className="short-actions" aria-label="영상 넘기기·보관">
          <button type="button" disabled={saveMutation.isPending} className={current.isSaved ? 'short-action active' : 'short-action'} aria-pressed={current.isSaved} onClick={toggleSave}>
            <Bookmark fill={current.isSaved ? 'currentColor' : 'none'} /><span>{!user ? '로그인 후 보관' : saveMutation.isPending ? '처리 중…' : current.isSaved ? '보관 해제' : '보관'}</span>
          </button>
          <button className="short-actions__next" type="button" disabled={!feed.hasPrevious} onClick={feed.previous}><ChevronUp />이전 영상</button>
          <button className="short-actions__next" type="button" disabled={!feed.hasNext} onClick={feed.next}><ChevronDown />{feed.hasNext ? '다음 영상' : '불러오는 중…'}</button>
        </aside>
      </div>
        : feed.empty ? <section className="explore-status"><h1>아직 볼 수 있는 숏폼이 없어요</h1><p>매주 새 영상이 공개되면 여기에서 볼 수 있어요.</p></section>
        : feed.error ? <section className="explore-status" role="alert"><h1>숏폼을 불러오지 못했어요</h1><button className="button button--primary" type="button" onClick={() => void feed.retry()}>다시 시도</button></section>
        : <section className="explore-status" aria-live="polite"><div className="status-icon status-icon--loading" /><h1>숏폼을 준비하고 있어요</h1></section>}
      {saveMutation.isError && <p className="explore-api-error" role="alert">보관하지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
      {current && <p className="free-shorts-page__hint">여기서 본 영상은 맞춤 추천에 반영되지 않아요. 내 취향으로 추천받으려면 <Link to={user ? '/explore' : '/onboarding/interests?mode=guest'}>숏폼 추천 받기</Link>를 이용해 주세요.</p>}
    </main>
  </div>;
}
