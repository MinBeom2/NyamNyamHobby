import { describe, expect, it } from 'vitest';
import type { FeedItemDto } from '../api/feedApi';
import { toFeedItemModel } from './feedAdapter';

const base: FeedItemDto = {
  id: 1, hobbyId: 3, hobbyName: '클라이밍', type: 'EVENT', title: '클라이밍 페스티벌', summary: '요약',
  sourceName: '한국관광공사 TourAPI', sourceUrl: 'https://example.com/1', thumbnailUrl: null, regionName: '서울 송파구',
  eventStart: '2026-11-01', eventEnd: '2026-11-03', publishedAt: '2026-10-18T10:00:00+09:00', isEnded: false,
};

describe('toFeedItemModel', () => {
  it('shows where and when the event happens instead of the data source', () => {
    expect(toFeedItemModel(base)).toMatchObject({ typeLabel: '축제·행사', meta: '서울 송파구 · 11.01~11.03', ended: false });
  });

  it('shows a single day once and drops an unknown region', () => {
    expect(toFeedItemModel({ ...base, eventEnd: '2026-11-01' }).meta).toBe('서울 송파구 · 11.01');
    expect(toFeedItemModel({ ...base, regionName: null }).meta).toBe('11.01~11.03');
  });

  it('shows the writer and posted date for guides without a region', () => {
    expect(toFeedItemModel({ ...base, type: 'GUIDE', regionName: null, eventStart: null, eventEnd: null, sourceName: '냠냠취미 편집팀' }).meta)
      .toBe('냠냠취미 편집팀 · 게시 10.18');
  });

  it('keeps an empty summary as empty text', () => {
    expect(toFeedItemModel({ ...base, summary: null }).summary).toBe('');
  });
});
