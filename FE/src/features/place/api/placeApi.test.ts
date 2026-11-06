import { afterEach, describe, expect, it, vi } from 'vitest';
import { getPlaceHobbies, getPlaces } from './placeApi';

const success = (data: unknown) => new Response(JSON.stringify({ success: true, data, error: null }), {
  status: 200, headers: { 'Content-Type': 'application/json' },
});

afterEach(() => vi.unstubAllGlobals());

describe('place search API', () => {
  it('includes hobbies beyond the first page so incoming hobby links remain selectable', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(success({
        content: [{ id: 1, name: '클라이밍' }],
        page: { number: 0, size: 50, totalElements: 51, totalPages: 2, hasNext: true },
      }))
      .mockResolvedValueOnce(success({
        content: [{ id: 51, name: '요가' }],
        page: { number: 1, size: 50, totalElements: 51, totalPages: 2, hasNext: false },
      }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(getPlaceHobbies()).resolves.toEqual({ content: [
      { id: 1, name: '클라이밍' }, { id: 51, name: '요가' },
    ] });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const queries = fetchMock.mock.calls.map(([url]) => new URL(url, 'http://localhost').searchParams);
    expect(queries.map((query) => query.get('page'))).toEqual(['0', '1']);
    expect(queries.every((query) => query.get('sort') === 'NAME')).toBe(true);
  });

  it('returns an empty hobby list without requesting another page', async () => {
    const fetchMock = vi.fn().mockResolvedValue(success({
      content: [], page: { number: 0, size: 50, totalElements: 0, totalPages: 0, hasNext: false },
    }));
    vi.stubGlobal('fetch', fetchMock);
    await expect(getPlaceHobbies()).resolves.toEqual({ content: [] });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('reports a later page failure instead of exposing a partial hobby list', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(success({ content: [{ id: 1, name: '클라이밍' }], page: { hasNext: true } }))
      .mockResolvedValueOnce(new Response('', { status: 503 })));
    await expect(getPlaceHobbies()).rejects.toMatchObject({ status: 503 });
  });

  it.each([1, 3, 12])('passes selected hobby %s and region to the search endpoint', async (hobbyId) => {
    const fetchMock = vi.fn().mockResolvedValue(success([]));
    vi.stubGlobal('fetch', fetchMock);
    await getPlaces(7, hobbyId);
    const url = new URL(fetchMock.mock.calls[0][0], 'http://localhost');
    expect(url.pathname).toBe('/api/v1/places');
    expect(url.searchParams.get('hobbyId')).toBe(String(hobbyId));
    expect(url.searchParams.get('regionId')).toBe('7');
  });
});
