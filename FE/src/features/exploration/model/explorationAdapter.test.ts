import { describe, expect, it } from 'vitest';
import { toExplorationRecommendation } from './explorationAdapter';
import type { RecommendationResponse } from '../api/explorationApi';

describe('toExplorationRecommendation', () => {
  it('reads hobby names from the documented nested recommendation contract', () => {
    const response: RecommendationResponse = {
      recommendationId: 1, basis: 'EXPLORATION_SESSION', basisText: '추천 기준',
      isExploratory: false, reactionCount: 3,
      items: [{ itemId: 10, type: 'INTEREST', rank: 1,
        hobby: { id: 3, name: '클라이밍', thumbnailUrl: null, difficulty: 'NORMAL',
          sessionMinutes: { min: 60, max: 90 }, monthlyCost: { min: 10000, max: 30000 } },
        reason: null, reasonStatus: 'PENDING', fallbackReason: '클라이밍 영상에 좋아요 3회',
        saved: false, conditionWarning: false }],
    };
    expect(toExplorationRecommendation(response)).toEqual({ recommendationId: 1, reactionCount: 3, hobbyNames: ['클라이밍'] });
  });
});
