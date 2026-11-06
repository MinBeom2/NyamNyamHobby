import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { getMyInterests, getUserHobbies, userQueryKeys } from '../../user/api/userApi';
import { feedQueryKeys, getFeedHighlights, getFeeds, type FeedQuery, type FeedScope } from '../api/feedApi';
import { toFeedItemModel } from './feedAdapter';
import { getRegions, placeQueryKeys } from '../../place/api/placeApi';

const PAGE_SIZE = 20;

export function useFeedList(query: FeedQuery) {
  const list = useInfiniteQuery({
    queryKey: feedQueryKeys.list(query),
    queryFn: ({ pageParam }) => getFeeds(query, pageParam, PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page.hasNext ? last.page.number + 1 : undefined),
  });
  const items = list.data?.pages.flatMap((page) => page.content.map(toFeedItemModel)) ?? [];
  return { ...list, items };
}

export function useFeedHighlights(limit = 5) {
  return useQuery({
    queryKey: feedQueryKeys.highlights(limit),
    queryFn: async () => (await getFeedHighlights(limit)).map(toFeedItemModel),
  });
}

export type ScopeHobby = { hobbyId: number; name: string };

/**
 * 범위별 취미 칩 목록. 서버의 scope 판정과 같은 기준이다.
 * MY = 내 취미 + 진행 중·일시중지, SAVED = 관심 저장, ALL = 칩 없음(취미가 30종이라 전체 탭으로만 본다).
 */
export function useScopeHobbies(scope: FeedScope) {
  const interestsQuery = useQuery({ queryKey: userQueryKeys.interests(), queryFn: getMyInterests });
  const hobbiesQuery = useQuery({ queryKey: userQueryKeys.hobbies(), queryFn: () => getUserHobbies() });
  const userHobbies = hobbiesQuery.data ?? [];

  const my = new Map<number, string>();
  interestsQuery.data?.forEach((item) => my.set(item.hobbyId, item.name));
  userHobbies.filter((item) => item.status === 'IN_PROGRESS' || item.status === 'PAUSED')
    .forEach((item) => my.set(item.hobbyId, item.name));
  const saved = userHobbies.filter((item) => item.status === 'SAVED')
    .map((item) => ({ hobbyId: item.hobbyId, name: item.name }));

  const hobbies: ScopeHobby[] = scope === 'MY'
    ? [...my].map(([hobbyId, name]) => ({ hobbyId, name }))
    : scope === 'SAVED' ? saved : [];
  return {
    hobbies,
    /** 내 취미도 진행 중 취미도 없어 맞춤 피드를 만들 수 없는 회원 */
    hasNoMyHobbies: interestsQuery.isSuccess && hobbiesQuery.isSuccess && my.size === 0,
    isLoading: interestsQuery.isLoading || hobbiesQuery.isLoading,
  };
}

/** 지역 필터 선택지. MVP 정보 피드는 서울만 수집하므로 서울 자치구만 보여 준다 */
export const FEED_SIDO = '서울특별시';

export function useFeedRegions(enabled: boolean) {
  return useQuery({
    queryKey: placeQueryKeys.regions(FEED_SIDO),
    queryFn: () => getRegions(FEED_SIDO),
    enabled,
    staleTime: Infinity,
  });
}
