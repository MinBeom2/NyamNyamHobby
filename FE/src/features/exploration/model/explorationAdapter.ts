import type {
  ExplorationProgressResponse,
  RecommendationResponse,
  ShortDetail,
} from '../api/explorationApi';
import type {
  ExplorationProgress,
  ExplorationRecommendation,
  ExplorationShort,
} from './explorationModel';

export function toExplorationShort(short: ShortDetail): ExplorationShort {
  return {
    id: short.shortId,
    youtubeVideoId: short.youtubeVideoId,
    title: short.title,
    durationLabel: formatDuration(short.duration),
    hobbyName: short.hobby?.name ?? '새로운 취미',
    hobbyCategory: short.hobby?.category ?? null,
    reaction: short.myReaction,
    isSaved: short.isSaved,
  };
}

export function toExplorationProgress(
  response: ExplorationProgressResponse,
): ExplorationProgress {
  return {
    interests: response.topInterests,
    likeCount: response.reactedHobbies.filter((item) => item.reaction === 'LIKE').length,
    dislikeCount: response.reactedHobbies.filter((item) => item.reaction === 'DISLIKE').length,
  };
}

export function toExplorationRecommendation(
  response: RecommendationResponse,
): ExplorationRecommendation {
  return {
    recommendationId: response.recommendationId,
    reactionCount: response.reactionCount,
    hobbyNames: response.items.slice(0, 3).map((item) => item.hobby.name),
  };
}

function formatDuration(duration: number | null) {
  if (duration === null) return '--:--';
  const minutes = Math.floor(duration / 60).toString().padStart(2, '0');
  const seconds = (duration % 60).toString().padStart(2, '0');
  return `${minutes}:${seconds}`;
}

