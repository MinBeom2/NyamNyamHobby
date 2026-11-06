import { useCallback, useEffect, useRef, useState } from 'react';
import { toExplorationShort } from '../../exploration/model/explorationAdapter';
import type { ExplorationShort } from '../../exploration/model/explorationModel';
import { getShortFeed } from '../api/shortformApi';

const PAGE_SIZE = 10;
/** 남은 영상이 이만큼이면 다음 묶음을 미리 받는다 */
const PREFETCH_LEFT = 3;

type Cursor = { seed?: number; cursor: number };

/**
 * 자유 탐색: 공개 숏폼을 섞인 순서로 끝없이 넘긴다. 한 바퀴가 끝나면 새로 섞어 이어 붙인다.
 * 시청·반응을 서버에 남기지 않는다(맞춤 추천에 영향 없음). 보관 상태만 화면에서 바꿔 준다.
 */
export function useShortFeed() {
  const [shorts, setShorts] = useState<ExplorationShort[]>([]);
  const [index, setIndex] = useState(0);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);
  const [empty, setEmpty] = useState(false);
  const nextRef = useRef<Cursor>({ cursor: 0 });
  const loadingRef = useRef(false);

  const loadMore = useCallback(async () => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    try {
      const response = await getShortFeed({ ...nextRef.current, size: PAGE_SIZE });
      // 한 바퀴를 다 봤으면 seed 없이 처음부터 → 서버가 새로 섞는다
      nextRef.current = response.nextCursor === null ? { cursor: 0 } : { seed: response.seed, cursor: response.nextCursor };
      setEmpty(response.total === 0);
      setShorts((current) => [...current, ...response.items.map(toExplorationShort)]);
      setError(null);
    } catch (caught) {
      setError(caught);
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!empty && shorts.length - index <= PREFETCH_LEFT) void loadMore();
  }, [empty, index, loadMore, shorts.length]);

  const next = () => setIndex((value) => Math.min(value + 1, Math.max(0, shorts.length - 1)));
  const previous = () => setIndex((value) => Math.max(0, value - 1));
  const markSaved = (shortId: number, isSaved: boolean) =>
    setShorts((current) => current.map((short) => short.id === shortId ? { ...short, isSaved } : short));

  return {
    current: shorts[index] ?? null,
    position: index + 1,
    hasPrevious: index > 0,
    hasNext: index < shorts.length - 1,
    loading,
    empty,
    error,
    retry: loadMore,
    next,
    previous,
    markSaved,
  };
}
