import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import {
  deleteShortReaction,
  explorationQueryKeys,
  finishExploration,
  getExplorationProgress,
  getNextShort,
  recordShortView,
  setShortReaction,
  startExploration,
  type ReactionType,
} from '../api/explorationApi';
import {
  toExplorationProgress,
  toExplorationRecommendation,
  toExplorationShort,
} from '../model/explorationAdapter';
import type {
  ExplorationRecommendation,
  ExplorationShort,
} from '../model/explorationModel';
import { deleteSavedShort, saveShort } from '../../shortform/api/shortformApi';

export function useMemberExploration() {
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [maxCount, setMaxCount] = useState(0);
  const [viewedCount, setViewedCount] = useState(0);
  const [current, setCurrent] = useState<ExplorationShort | null>(null);
  const [recommendation, setRecommendation] = useState<ExplorationRecommendation | null>(null);
  const startedRef = useRef(false);
  const shownAtRef = useRef(Date.now());

  const startMutation = useMutation({
    mutationFn: startExploration,
    onSuccess: (data) => {
      setSessionId(data.sessionId);
      setMaxCount(data.maxCount);
      setViewedCount(data.viewedCount);
      setCurrent(toExplorationShort(data.short));
      setRecommendation(null);
      shownAtRef.current = Date.now();
    },
  });

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;
    startMutation.mutate();
  }, [startMutation]);

  const progressQuery = useQuery({
    queryKey: explorationQueryKeys.progress(sessionId ?? 0),
    queryFn: () => getExplorationProgress(sessionId as number),
    select: toExplorationProgress,
    enabled: sessionId !== null && recommendation === null,
    retry: false,
  });

  const reactionMutation = useMutation({
    mutationFn: async (type: ReactionType) => {
      if (!current || sessionId === null) {
        throw new Error('탐색 세션이 준비되지 않았습니다.');
      }
      if (current.reaction === type) return deleteShortReaction(current.id);
      return setShortReaction(current.id, { type, sessionId });
    },
    onSuccess: (response) => {
      setCurrent((short) => short ? { ...short, reaction: response.type } : short);
      void progressQuery.refetch();
    },
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (!current) throw new Error('보관할 영상이 준비되지 않았습니다.');
      return current.isSaved ? deleteSavedShort(current.id) : saveShort(current.id);
    },
    onSuccess: (response) => {
      setCurrent((short) => short ? { ...short, isSaved: response.isSaved } : short);
    },
  });

  const advanceMutation = useMutation({
    mutationFn: async () => {
      if (!current || sessionId === null) {
        throw new Error('탐색 세션이 준비되지 않았습니다.');
      }
      await recordShortView(sessionId, {
        shortId: current.id,
        watchedSeconds: Math.max(0, Math.round((Date.now() - shownAtRef.current) / 1000)),
        playFailed: false,
      });

      if (viewedCount >= maxCount) {
        return { recommendation: toExplorationRecommendation(await finishExploration(sessionId)), next: null };
      }
      const next = await getNextShort(sessionId);
      if (next.finished || !next.short) {
        return { recommendation: toExplorationRecommendation(await finishExploration(sessionId)), next };
      }
      return { recommendation: null, next };
    },
    onSuccess: ({ recommendation: completedRecommendation, next }) => {
      if (completedRecommendation) {
        setRecommendation(completedRecommendation);
        setCurrent(null);
      } else if (next?.short) {
        setCurrent(toExplorationShort(next.short));
        setViewedCount(next.viewedCount);
        shownAtRef.current = Date.now();
        void progressQuery.refetch();
      }
    },
  });

  const restart = () => {
    setSessionId(null);
    setCurrent(null);
    setRecommendation(null);
    startMutation.mutate();
  };

  return {
    current,
    maxCount,
    viewedCount,
    recommendation,
    progress: progressQuery.data,
    isStarting: startMutation.isPending,
    isStartError: startMutation.isError,
    startError: startMutation.error,
    isReactionPending: reactionMutation.isPending,
    isSavePending: saveMutation.isPending,
    isAdvancePending: advanceMutation.isPending,
    error: startMutation.error ?? reactionMutation.error ?? saveMutation.error ?? advanceMutation.error,
    react: reactionMutation.mutate,
    toggleSave: saveMutation.mutate,
    advance: advanceMutation.mutate,
    restart,
  };
}

