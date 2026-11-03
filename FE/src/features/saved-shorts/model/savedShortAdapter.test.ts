import { describe, expect, it } from 'vitest';
import { toSavedShort, toSavedShortSummary } from './savedShortAdapter';
import type { SavedShortDto } from '../api/savedShortApi';

const short: SavedShortDto = {
  shortId: 41, youtubeVideoId: 'abcdefghijk', title: '보관 영상', thumbnailUrl: null,
  hobby: null, status: 'ACTIVE', savedAt: '2026-10-20T23:20:00Z',
};

describe('saved short adapter', () => {
  it('uses the database ID, thumbnail fallback and Korean date across UTC midnight', () => {
    const view = toSavedShort(short);
    expect(view.id).toBe(41);
    expect(view.hobby).toBe('미분류');
    expect(view.savedAt).toContain('21');
    expect(view.thumbnailUrl).toContain('/abcdefghijk/hqdefault.jpg');
    expect(view.videoUrl).toBe('https://www.youtube.com/shorts/abcdefghijk');
  });
  it.each(['CANDIDATE', 'NEEDS_REVIEW', 'UNAVAILABLE', 'AUTO_HIDDEN', 'REJECTED'] as const)(
    'does not provide a playback link for %s', (status) => {
      expect(toSavedShort({ ...short, status })).toMatchObject({ id: 41, playable: false, videoUrl: null });
    },
  );
  it('keeps server totals including unclassified items and uses hobby IDs instead of labels', () => {
    expect(toSavedShortSummary({ totalCount: 8, hobbies: [
      { id: 3, name: '클라이밍', count: 6 }, { id: null, name: null, count: 2 },
    ] })).toEqual({ totalCount: 8, filters: [
      { id: null, label: '전체', count: 8 }, { id: 3, label: '클라이밍', count: 6 },
    ] });
  });
});
