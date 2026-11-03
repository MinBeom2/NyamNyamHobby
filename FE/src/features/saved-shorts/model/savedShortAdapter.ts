import type { SavedShortDto, SavedShortSummaryDto } from '../api/savedShortApi';

export function toSavedShort(short: SavedShortDto) {
  const playable = short.status === 'ACTIVE';
  const savedAt = new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul', month: '2-digit', day: '2-digit',
  }).format(new Date(short.savedAt));
  return {
    id: short.shortId,
    title: short.title,
    hobby: short.hobby?.name ?? '미분류',
    savedAt,
    savedAtIso: short.savedAt,
    playable,
    thumbnailUrl: short.thumbnailUrl ?? `https://i.ytimg.com/vi/${encodeURIComponent(short.youtubeVideoId)}/hqdefault.jpg`,
    videoUrl: playable ? `https://www.youtube.com/shorts/${encodeURIComponent(short.youtubeVideoId)}` : null,
  };
}

export function toSavedShortSummary(summary: SavedShortSummaryDto) {
  return {
    totalCount: summary.totalCount,
    filters: [
      { id: null, label: '전체', count: summary.totalCount },
      ...summary.hobbies.flatMap((hobby) => hobby.id === null ? [] : [{
        id: hobby.id, label: hobby.name ?? '미분류', count: hobby.count,
      }]),
    ],
  };
}
