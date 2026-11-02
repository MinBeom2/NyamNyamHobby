import { Image as ImageIcon } from 'lucide-react';
import { useMemo, useState } from 'react';
import { savedShortFixtures, type SavedShortFixture } from '../../features/saved-shorts/model/savedShortFixtures';
import { AppHeader } from '../../shared/ui/AppHeader';

type HobbyFilter = '전체' | SavedShortFixture['hobby'];

const filters: Array<{ label: HobbyFilter; count: number }> = [
  { label: '전체', count: 12 },
  { label: '클라이밍', count: 4 },
  { label: '요가', count: 3 },
  { label: '도예', count: 3 },
  { label: '홈베이킹', count: 2 },
];

export function SavedShortsPage() {
  const [filter, setFilter] = useState<HobbyFilter>('전체');
  const [sort, setSort] = useState<'recent' | 'oldest'>('recent');
  const [visibleCount, setVisibleCount] = useState(5);
  const [removedIds, setRemovedIds] = useState<number[]>([]);
  const shorts = useMemo(() => {
    const filtered = savedShortFixtures.filter((item) => !removedIds.includes(item.id) && (filter === '전체' || item.hobby === filter));
    return sort === 'recent' ? filtered : [...filtered].reverse();
  }, [filter, removedIds, sort]);

  const selectFilter = (nextFilter: HobbyFilter) => {
    setFilter(nextFilter);
    setVisibleCount(5);
  };

  return <div className="site-shell"><AppHeader /><main className="page-container saved-shorts-page">
    <header className="saved-shorts-heading"><h1>숏폼 보관함</h1><label><span>정렬</span><select value={sort} onChange={(event) => setSort(event.target.value as 'recent' | 'oldest')}><option value="recent">최근 보관순</option><option value="oldest">오래된 보관순</option></select></label></header>
    <nav className="saved-shorts-filters" aria-label="취미별 보관 영상 필터">{filters.map((item) => <button key={item.label} className={filter === item.label ? 'active' : ''} type="button" onClick={() => selectFilter(item.label)}>{filter === item.label && '✓ '}{item.label} {item.count}</button>)}</nav>
    <section className="saved-shorts-grid">{shorts.slice(0, visibleCount).map((short) => <article className={!short.playable ? 'saved-short-card unavailable' : 'saved-short-card'} key={short.id}>
      <div className="saved-short-card__thumbnail"><ImageIcon size={20} /><span>{short.playable ? '숏폼 썸네일' : '재생할 수 없는 영상'}</span></div>
      <h2>{short.title}</h2><div><b>{short.playable ? short.hobby : '이용 불가'}</b><time>{short.savedAt} 보관</time></div>
      <button type="button" onClick={() => setRemovedIds((ids) => [...ids, short.id])}>보관 해제</button>
    </article>)}</section>
    {visibleCount < shorts.length && <button className="button button--secondary saved-shorts-more" type="button" onClick={() => setVisibleCount((count) => count + 5)}>더 보기</button>}
  </main></div>;
}

