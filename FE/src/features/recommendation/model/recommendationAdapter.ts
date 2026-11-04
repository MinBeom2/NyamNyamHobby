import type { RecommendationResponse } from '../../exploration/api/explorationApi';
import { formatCost, formatDuration, type HobbyCardModel } from '../../hobby/model/hobbyAdapter';

/** INTEREST = 숏폼 반응으로 고른 추천 취미, RELATED = 그 취미와 태그가 비슷한 유사 취미 */
export type RecommendationKind = 'INTEREST' | 'RELATED';

export type RecommendedHobby = {
  hobbyId: number;
  name: string;
  thumbnailUrl: string | null;
  kind: RecommendationKind;
  reason: string;
};

export type RecommendationResult = {
  recommendationId: number;
  interests: RecommendedHobby[];
  related: RecommendedHobby[];
  /** GPT 추천 이유가 아직 생성 중인 항목이 있으면 true. 이 동안 fallbackReason을 보여준다. */
  reasonPending: boolean;
};

export type RecommendedHobbyCardModel = HobbyCardModel & { kind: RecommendationKind };

const difficultyLabels: Record<string, string> = { EASY: '쉬움', NORMAL: '보통', HARD: '어려움' };
export const recommendationKindLabels: Record<RecommendationKind, string> = { INTEREST: '추천 취미', RELATED: '유사 취미' };

type RecommendationItemDto = RecommendationResponse['items'][number];

function toKind(type: string): RecommendationKind {
  return type === 'RELATED' ? 'RELATED' : 'INTEREST';
}

function toReason(item: RecommendationItemDto) {
  return item.reason?.trim() || item.fallbackReason;
}

function toRecommendedHobby(item: RecommendationItemDto): RecommendedHobby {
  return {
    hobbyId: item.hobby.id,
    name: item.hobby.name,
    thumbnailUrl: item.hobby.thumbnailUrl,
    kind: toKind(item.type),
    reason: toReason(item),
  };
}

export function toRecommendationResult(response: RecommendationResponse): RecommendationResult {
  const items = [...response.items].sort((a, b) => a.rank - b.rank);
  return {
    recommendationId: response.recommendationId,
    interests: items.filter((item) => toKind(item.type) === 'INTEREST').map(toRecommendedHobby),
    related: items.filter((item) => toKind(item.type) === 'RELATED').map(toRecommendedHobby),
    reasonPending: items.some((item) => item.reasonStatus === 'PENDING'),
  };
}

/** 홈 카드용. 스냅샷에 요약 문구가 없어 추천 이유를 설명 자리에 쓴다. 추천 취미가 먼저 온다. */
export function toRecommendedHobbyCards(response: RecommendationResponse): RecommendedHobbyCardModel[] {
  const items = [...response.items].sort((a, b) =>
    Number(toKind(a.type) === 'RELATED') - Number(toKind(b.type) === 'RELATED') || a.rank - b.rank);
  return items.map((item) => ({
    id: item.hobby.id,
    name: item.hobby.name,
    category: '',
    description: toReason(item),
    thumbnailUrl: item.hobby.thumbnailUrl,
    difficulty: item.hobby.difficulty ? difficultyLabels[item.hobby.difficulty] ?? '' : '',
    place: '',
    social: '',
    duration: formatDuration(item.hobby.sessionMinutes),
    cost: formatCost(item.hobby.monthlyCost),
    tags: [],
    relation: null,
    kind: toKind(item.type),
  }));
}
