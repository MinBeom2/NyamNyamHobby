import { useQuery } from '@tanstack/react-query';
import { getLatestRecommendation, getRecommendation, recommendationQueryKeys } from '../api/recommendationApi';
import { toRecommendationResult, toRecommendedHobbyCards } from './recommendationAdapter';

/** GPT 이유는 비동기로 채워지므로 PENDING인 동안만 다시 불러온다. 너무 오래 걸리면 기본 문장으로 둔다. */
const REASON_POLL_MS = 3000;
const REASON_POLL_LIMIT = 10;

export function useRecommendationResult(recommendationId: number) {
  return useQuery({
    queryKey: recommendationQueryKeys.detail(recommendationId),
    queryFn: () => getRecommendation(recommendationId),
    select: toRecommendationResult,
    refetchInterval: (query) => {
      const pending = query.state.data?.items.some((item) => item.reasonStatus === 'PENDING');
      return pending && query.state.dataUpdateCount < REASON_POLL_LIMIT ? REASON_POLL_MS : false;
    },
  });
}

/** 홈 '맞춤 추천 취미': 마지막 탐색에서 받은 추천 취미 + 유사 취미 */
export function useLatestRecommendedHobbies(enabled: boolean) {
  return useQuery({
    queryKey: recommendationQueryKeys.latest(),
    queryFn: getLatestRecommendation,
    select: (response) => (response ? toRecommendedHobbyCards(response) : null),
    enabled,
  });
}
