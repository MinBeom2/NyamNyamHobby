import { describe, expect, it } from 'vitest';
import type { RecommendationResponse } from '../../exploration/api/explorationApi';
import { toRecommendationResult, toRecommendedHobbyCards } from './recommendationAdapter';

function item(id: number, type: 'INTEREST' | 'RELATED', rank: number, reason: string | null, reasonStatus = 'COMPLETED') {
  return {
    itemId: id * 10, type, rank,
    hobby: { id, name: `취미${id}`, thumbnailUrl: `/assets/hobbies/${id}.webp`, difficulty: 'EASY',
      sessionMinutes: { min: 30, max: 60 }, monthlyCost: { min: 10000, max: 30000 } },
    reason, reasonStatus, fallbackReason: `취미${id} 기본 이유`, saved: false, conditionWarning: false,
  };
}

const response: RecommendationResponse = {
  recommendationId: 7, basis: 'EXPLORATION_SESSION', basisText: null, isExploratory: false, reactionCount: 12,
  items: [
    item(4, 'RELATED', 4, null, 'PENDING'),
    item(1, 'INTEREST', 1, 'GPT가 만든 이유'),
    item(2, 'INTEREST', 2, null, 'PENDING'),
    item(5, 'RELATED', 5, '유사 이유'),
  ],
};

describe('toRecommendationResult', () => {
  it('splits recommended and related hobbies in rank order', () => {
    const result = toRecommendationResult(response);
    expect(result.interests.map((hobby) => hobby.hobbyId)).toEqual([1, 2]);
    expect(result.related.map((hobby) => hobby.hobbyId)).toEqual([4, 5]);
  });

  it('shows the fallback reason until GPT finishes and reports pending state', () => {
    const result = toRecommendationResult(response);
    expect(result.interests[0].reason).toBe('GPT가 만든 이유');
    expect(result.interests[1].reason).toBe('취미2 기본 이유');
    expect(result.reasonPending).toBe(true);
  });
});

describe('toRecommendedHobbyCards', () => {
  it('puts recommended hobbies before related ones and keeps the kind for badges', () => {
    const cards = toRecommendedHobbyCards(response);
    expect(cards.map((card) => [card.id, card.kind])).toEqual([[1, 'INTEREST'], [2, 'INTEREST'], [4, 'RELATED'], [5, 'RELATED']]);
    expect(cards[0]).toMatchObject({ description: 'GPT가 만든 이유', difficulty: '쉬움', relation: null });
  });
});
