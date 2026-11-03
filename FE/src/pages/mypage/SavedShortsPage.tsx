import { Image as ImageIcon } from 'lucide-react';
import { useSavedShorts } from '../../features/saved-shorts/hooks/useSavedShorts';
import { AppHeader } from '../../shared/ui/AppHeader';

export function SavedShortsPage() {
  const saved = useSavedShorts();
  return <div className="site-shell"><AppHeader /><main className="page-container saved-shorts-page">
    <header className="saved-shorts-heading"><h1>숏폼 보관함</h1><label><span>정렬</span><select value={saved.sort} onChange={(event) => saved.selectSort(event.target.value)}><option value="recent">최근 보관순</option><option value="oldest">오래된 보관순</option></select></label></header>
    <nav className="saved-shorts-filters" aria-label="취미별 보관 영상 필터">{saved.filters.map((item) => <button key={item.id ?? 'all'} className={saved.hobbyId === item.id ? 'active' : ''} type="button" aria-pressed={saved.hobbyId === item.id} onClick={() => saved.selectFilter(item.id)}>{saved.hobbyId === item.id && '✓ '}{item.label} {item.count}</button>)}</nav>
    {saved.summaryError && <p role="alert">보관 수를 불러오지 못했어요. {saved.summaryError.message} <button className="button button--secondary" type="button" onClick={saved.retrySummary}>다시 시도</button></p>}
    {saved.isLoading && <p role="status">보관한 영상을 불러오고 있어요.</p>}
    {saved.listError && <p role="alert">보관한 영상을 불러오지 못했어요. {saved.listError.message} <button className="button button--secondary" type="button" onClick={saved.retryList}>다시 시도</button></p>}
    {!saved.isLoading && !saved.listError && saved.shorts.length === 0 && <p className="mypage-empty">{saved.hobbyId === null ? '아직 보관한 영상이 없어요. 숏츠 탐색에서 마음에 드는 영상을 보관해 보세요.' : '이 취미에 보관한 영상이 없어요.'}</p>}
    {saved.removeError && <p role="alert">보관 해제를 완료하지 못했어요. {saved.removeError.message}</p>}
    <section className="saved-shorts-grid">{saved.shorts.map((short) => <article className={!short.playable ? 'saved-short-card unavailable' : 'saved-short-card'} key={short.id}>
      <div className={`saved-short-card__thumbnail${short.videoUrl ? ' saved-short-card__thumbnail--playable' : ''}`}>
        {short.videoUrl ? <a href={short.videoUrl} target="_blank" rel="noopener noreferrer" aria-label={`${short.title} 재생 (새 탭)`}><img src={short.thumbnailUrl} alt={short.title} loading="lazy" /></a> : <><ImageIcon size={20} /><span>재생할 수 없는 영상</span></>}
      </div>
      <h2>{short.title}</h2><div><b>{short.playable ? short.hobby : '이용 불가'}</b><time dateTime={short.savedAtIso}>{short.savedAt} 보관</time></div>
      <button type="button" disabled={saved.isRemoving} onClick={() => saved.remove(short.id)}>{saved.removingId === short.id ? '처리 중…' : '보관 해제'}</button>
    </article>)}</section>
    {saved.hasNextPage && <button className="button button--secondary saved-shorts-more" type="button" disabled={saved.isFetchingNextPage} onClick={saved.loadMore}>{saved.isFetchingNextPage ? '불러오는 중…' : '더 보기'}</button>}
  </main></div>;
}

