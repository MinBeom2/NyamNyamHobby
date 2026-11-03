import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuthStore } from '../../auth/model/authStore';
import { deleteSavedShort } from '../../shortform/api/shortformApi';
import { getSavedShorts, getSavedShortSummary, savedShortQueryKeys, type SavedShortSort } from '../api/savedShortApi';
import { toSavedShort, toSavedShortSummary } from '../model/savedShortAdapter';
import { userQueryKeys } from '../../user/api/userApi';

export function useSavedShortSummary() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: savedShortQueryKeys.summary(userId),
    queryFn: getSavedShortSummary,
    select: toSavedShortSummary,
    enabled: userId !== undefined,
    retry: false,
  });
}

export function useSavedShorts() {
  const userId = useAuthStore((state) => state.user?.id);
  const client = useQueryClient();
  const [hobbyId, setHobbyId] = useState<number | null>(null);
  const [hobbyName, setHobbyName] = useState('선택한 취미');
  const [sort, setSort] = useState<SavedShortSort>('recent');
  const summary = useSavedShortSummary();
  const list = useInfiniteQuery({
    queryKey: savedShortQueryKeys.list(userId, hobbyId, sort),
    queryFn: ({ pageParam }) => getSavedShorts({ hobbyId, sort, page: pageParam, size: 5 }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.page.hasNext ? lastPage.page.number + 1 : undefined,
    enabled: userId !== undefined,
    retry: false,
  });
  const remove = useMutation({
    mutationFn: deleteSavedShort,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: savedShortQueryKeys.all }),
        client.invalidateQueries({ queryKey: userQueryKeys.summary() }),
      ]);
    },
  });
  const filters = [...(summary.data?.filters ?? [])];
  // 마지막 항목을 해제해도 현재 선택한 필터를 0건으로 유지한다.
  if (hobbyId !== null && !filters.some((filter) => filter.id === hobbyId)) {
    filters.push({ id: hobbyId, label: hobbyName, count: 0 });
  }
  return {
    shorts: list.data?.pages.flatMap((page) => page.content.map(toSavedShort)) ?? [],
    filters,
    hobbyId,
    selectFilter: (id: number | null) => {
      setHobbyName(filters.find((filter) => filter.id === id)?.label ?? '선택한 취미');
      setHobbyId(id);
    },
    sort,
    selectSort: (value: string) => { if (value === 'recent' || value === 'oldest') setSort(value); },
    isLoading: list.isPending,
    listError: list.error,
    summaryError: summary.error,
    removeError: remove.error,
    retryList: () => { void list.refetch(); },
    retrySummary: () => { void summary.refetch(); },
    hasNextPage: list.hasNextPage,
    isFetchingNextPage: list.isFetchingNextPage,
    loadMore: () => { void list.fetchNextPage(); },
    remove: remove.mutate,
    isRemoving: remove.isPending,
    removingId: remove.isPending ? remove.variables : null,
  };
}
