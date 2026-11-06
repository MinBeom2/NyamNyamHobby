import { useCallback, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { readPageNumber, withPageNumber } from './pageSearchParams';

export function usePageSearchParams(name = 'page') {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = readPageNumber(searchParams.get(name));
  const changePage = useCallback((nextPage: number, replace = false) => {
    setSearchParams((current) => withPageNumber(current, nextPage, name), { replace });
  }, [name, setSearchParams]);
  const changeFilters = (values: Record<string, string | null>, replace = false) => {
    setSearchParams((current) => {
      const next = withPageNumber(current, 0, name);
      Object.entries(values).forEach(([key, value]) => {
        if (value === null || value === '') next.delete(key);
        else next.set(key, value);
      });
      return next;
    }, { replace });
  };
  return { page, searchParams, changePage, changeFilters };
}

// 삭제 등으로 마지막 페이지가 사라지거나 범위 밖 URL로 들어오면 남은 마지막 페이지로 이동한다.
export function usePageBounds(page: number, totalPages: number | undefined, changePage: (page: number, replace: boolean) => void) {
  useEffect(() => {
    if (totalPages !== undefined && page > Math.max(0, totalPages - 1)) {
      changePage(Math.max(0, totalPages - 1), true);
    }
  }, [page, totalPages, changePage]);
}
