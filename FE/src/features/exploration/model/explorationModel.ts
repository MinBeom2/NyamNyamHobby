import type { ReactionType } from '../api/explorationApi';

export type ExplorationShort = {
  id: number;
  youtubeVideoId: string;
  title: string;
  durationLabel: string;
  hobbyName: string;
  hobbyCategory: string | null;
  reaction: ReactionType | null;
  isSaved: boolean;
};

export type ExplorationProgress = {
  interests: Array<{
    hobbyName: string;
    percent: number;
  }>;
  likeCount: number;
  dislikeCount: number;
};

export type ExplorationRecommendation = {
  reactionCount: number;
  hobbyNames: string[];
};

