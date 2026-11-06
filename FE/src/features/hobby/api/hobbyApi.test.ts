import { afterEach, describe, expect, it, vi } from 'vitest';
import { getHobbyCatalog } from './hobbyApi';

const success = (data: unknown) => new Response(JSON.stringify({ success: true, data, error: null }), {
  status: 200, headers: { 'Content-Type': 'application/json' },
});

afterEach(() => vi.unstubAllGlobals());

describe('complete hobby catalog', () => {
  it('includes hobbies after the 100th record and preserves search filters on every page', async () => {
    const first = Array.from({ length: 100 }, (_, index) => ({ id: index + 1, name: `취미 ${index + 1}` }));
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(success({ content: first, page: { number: 0, hasNext: true } }))
      .mockResolvedValueOnce(success({ content: [{ id: 101, name: '마지막 취미' }], page: { number: 1, hasNext: false } }));
    vi.stubGlobal('fetch', fetchMock);
    const result = await getHobbyCatalog({ keyword: '취미', difficulty: 'EASY', place: 'INDOOR', sort: 'NAME' });
    expect(result).toHaveLength(101);
    expect(result[100].name).toBe('마지막 취미');
    const queries = fetchMock.mock.calls.map(([url]) => new URL(url, 'http://localhost').searchParams);
    expect(queries.map((query) => query.get('page'))).toEqual(['0', '1']);
    for (const query of queries) {
      expect(query.get('size')).toBe('100');
      expect(query.get('keyword')).toBe('취미');
      expect(query.get('difficulty')).toBe('EASY');
      expect(query.get('place')).toBe('INDOOR');
      expect(query.get('sort')).toBe('NAME');
    }
  });

  it('does not request another page for an empty catalog', async () => {
    const fetchMock = vi.fn().mockResolvedValue(success({ content: [], page: { hasNext: false } }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(getHobbyCatalog()).resolves.toEqual([]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('reports a later page failure instead of returning an incomplete catalog', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(success({ content: [{ id: 1, name: '첫 취미' }], page: { hasNext: true } }))
      .mockResolvedValueOnce(new Response('', { status: 503 })));
    await expect(getHobbyCatalog()).rejects.toMatchObject({ status: 503 });
  });
});
